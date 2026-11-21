package com.example.data.repository

import android.content.Context
import android.database.ContentObserver
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.example.data.analytics.BackupPlaylist
import com.example.data.analytics.BackupPlaylistTrack
import com.example.data.analytics.PlaylistBackupEngine
import com.example.data.analytics.PlaylistBackupSyncResult
import com.example.data.analytics.PlaylistImportResult
import com.example.data.analytics.UsageFileSyncResult
import com.example.data.analytics.UsageImportResult
import com.example.data.analytics.UsageStatsEngine
import com.example.data.db.CachedMediaDao
import com.example.data.db.CachedMediaEntity
import com.example.data.db.PlayHistoryDao
import com.example.data.db.PlayHistoryEntity
import com.example.data.db.PlaylistDao
import com.example.data.db.PlaylistEntity
import com.example.data.db.PlaylistTrackEntity
import com.example.data.media.AudioScanner
import com.example.data.media.CachedMediaRecord
import com.example.model.AnalyticsTimeframe
import com.example.model.AudioTrack
import com.example.model.MediaType
import com.example.model.PlaybackHistoryItem
import com.example.model.PlaylistSortOption
import com.example.model.PlaylistSummary
import com.example.model.PlaylistTrackEntry
import com.example.model.WeeklySummaryReport
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MusicRepository(
  private val context: Context,
  private val audioScanner: AudioScanner,
  private val playlistDao: PlaylistDao,
  private val playHistoryDao: PlayHistoryDao,
  private val cachedMediaDao: CachedMediaDao? = null,
) {
  private val syncMutex = Mutex()
  private val historyMutex = Mutex()
  private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  @Volatile private var hydrationJob: Job? = null

  fun observeCachedMedia(): Flow<List<AudioTrack>> =
    cachedMediaDao?.observeAllCachedMedia()?.map { entities ->
      entities.map { it.toAudioTrack() }
    } ?: flowOf(emptyList())

  suspend fun deleteCachedMedia(contentUri: Uri, trackId: Long) {
    cachedMediaDao?.deleteByUriOrId(contentUri.toString(), trackId)
  }

  suspend fun syncLibraryIncremental(
    includeAudio: Boolean = true,
    includeVideo: Boolean = true,
    unknownArtist: String,
    unknownAlbum: String,
    unknownVideo: String = unknownAlbum,
    converterTreeUriString: String? = null,
  ): List<AudioTrack> =
    syncMutex.withLock {
      withContext(Dispatchers.IO) {
        val existingRecords =
          cachedMediaDao?.getAllCachedMediaSnapshot()?.map { it.toCachedRecord() }.orEmpty()
        val diff =
          audioScanner.scanLocalMediaIncremental(
            existingRecords = existingRecords,
            includeAudio = includeAudio,
            includeVideo = includeVideo,
            unknownArtistFallback = unknownArtist,
            unknownAlbumFallback = unknownAlbum,
            unknownVideoFallback = unknownVideo,
            converterTreeUriString = converterTreeUriString,
            deferHeavyMetadataExtraction = true,
          )
        if (cachedMediaDao != null && (diff.upserts.isNotEmpty() || diff.deletedUris.isNotEmpty())) {
          cachedMediaDao.applySyncDiff(
            upserts = diff.upserts.map { it.toEntity() },
            deletedUris = diff.deletedUris,
          )
        }
        val toHydrate = diff.pendingHydration
        if (cachedMediaDao != null && toHydrate.isNotEmpty()) {
          hydrationJob?.cancel()
          hydrationJob =
            repositoryScope.launch {
              toHydrate.chunked(25).forEach { chunk ->
                val enriched =
                  audioScanner.hydrateTracksBatch(
                    records = chunk,
                    unknownArtistFallback = unknownArtist,
                    unknownAlbumFallback = unknownAlbum,
                    unknownVideoFallback = unknownVideo,
                  )
                if (enriched.isNotEmpty()) {
                  cachedMediaDao.upsertAll(enriched.map { it.toEntity() })
                }
              }
            }
        }
        diff.resolvedTracks
      }
    }

  suspend fun scanDeviceAudio(
    unknownArtist: String,
    unknownAlbum: String,
    unknownVideo: String = unknownAlbum,
    converterTreeUriString: String? = null,
  ): List<AudioTrack> =
    syncLibraryIncremental(
      includeAudio = true,
      includeVideo = true,
      unknownArtist = unknownArtist,
      unknownAlbum = unknownAlbum,
      unknownVideo = unknownVideo,
      converterTreeUriString = converterTreeUriString,
    )

  fun observeDeviceMediaChanges(): Flow<Unit> = callbackFlow {
    val handler = Handler(Looper.getMainLooper())
    val observer =
      object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
          trySend(Unit)
        }
      }
    runCatching {
      context.contentResolver.registerContentObserver(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        true,
        observer,
      )
      context.contentResolver.registerContentObserver(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        true,
        observer,
      )
      context.contentResolver.registerContentObserver(
        MediaStore.Files.getContentUri("external"),
        true,
        observer,
      )
    }

    val fileObservers = mutableListOf<FileObserver>()
    val watchMask =
      FileObserver.CLOSE_WRITE or
        FileObserver.MOVED_TO or
        FileObserver.CREATE or
        FileObserver.DELETE or
        FileObserver.MOVED_FROM
    val watchRoots = audioScanner.defaultMediaDirectories()
    val expandedWatchDirs =
      buildList {
        val seen = HashSet<String>()
        watchRoots.forEach { root ->
          if (seen.add(root.absolutePath)) add(root)
          root.listFiles()?.forEach { child ->
            if (size < 64 && child.isDirectory && !child.name.startsWith(".") && child.canRead()) {
              if (seen.add(child.absolutePath)) add(child)
            }
          }
        }
      }
    fun handleObservedFileEvent(dir: File?, event: Int, relativePath: String?) {
      if (relativePath != null && !audioScanner.isSupportedMediaFilename(relativePath)) return
      if (relativePath != null && dir != null &&
        (event and (FileObserver.CLOSE_WRITE or FileObserver.MOVED_TO or FileObserver.CREATE)) != 0
      ) {
        val fullFile = File(dir, relativePath)
        if (fullFile.isFile) {
          runCatching {
            MediaScannerConnection.scanFile(
              context,
              arrayOf(fullFile.absolutePath),
              null,
              null,
            )
          }
        }
      }
      trySend(Unit)
    }
    if (expandedWatchDirs.isNotEmpty()) {
      expandedWatchDirs.forEach { dir ->
        runCatching {
          val dirObserver =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              object : FileObserver(dir, watchMask) {
                override fun onEvent(event: Int, path: String?) {
                  handleObservedFileEvent(dir, event, path)
                }
              }
            } else {
              @Suppress("DEPRECATION")
              object : FileObserver(dir.absolutePath, watchMask) {
                override fun onEvent(event: Int, path: String?) {
                  handleObservedFileEvent(dir, event, path)
                }
              }
            }
          dirObserver.startWatching()
          fileObservers.add(dirObserver)
        }
      }
    }

    awaitClose {
      runCatching { context.contentResolver.unregisterContentObserver(observer) }
      fileObservers.forEach { obs -> runCatching { obs.stopWatching() } }
    }
  }

  suspend fun loadBinderSafeArtworkBytes(track: AudioTrack): ByteArray? =
    withContext(Dispatchers.IO) {
      audioScanner.loadBinderSafeArtworkBytes(track)
    }

  suspend fun resolveExternalTrack(uri: Uri): AudioTrack =
    audioScanner.extractTrackFromExternalUri(uri)

  fun observePlaylists(): Flow<List<PlaylistSummary>> =
    playlistDao.observePlaylistsWithStats().map { rows ->
      rows.map { row ->
        PlaylistSummary(
          id = row.id,
          name = row.name,
          coverPath = row.coverPath,
          sortOption =
            runCatching { PlaylistSortOption.valueOf(row.sortOption) }
              .getOrDefault(PlaylistSortOption.MANUAL),
          createdAtMs = row.createdAtMs,
          trackCount = row.trackCount,
          totalDurationMs = row.totalDurationMs,
          isFavorites = row.isFavorites,
        )
      }
    }

  fun observeFavoriteUris(): Flow<Set<String>> =
    playlistDao.observeFavoriteContentUris().map { it.toSet() }

  suspend fun ensureFavoritesPlaylist(defaultName: String = "Liked"): Long {
    val existing = playlistDao.getFavoritesPlaylist()
    if (existing != null) return existing.id
    return playlistDao.insertPlaylist(
      PlaylistEntity(
        name = defaultName,
        sortOption = PlaylistSortOption.DATE_ADDED_DESC.name,
        createdAtMs = System.currentTimeMillis(),
        isFavorites = true,
      )
    )
  }

  suspend fun toggleFavorite(track: AudioTrack, defaultFavoritesName: String = "Liked"): Boolean {
    val favId = ensureFavoritesPlaylist(defaultFavoritesName)
    val uriStr = track.contentUri.toString()
    val exists = playlistDao.countTrackInPlaylistByUriOrId(favId, uriStr, track.id) > 0
    return if (exists) {
      playlistDao.removeTrackByPlaylistAndUriOrId(favId, uriStr, track.id)
      false
    } else {
      addTracksToPlaylist(favId, listOf(track))
      true
    }
  }

  fun observePlaylistSummary(playlistId: Long): Flow<PlaylistEntity?> =
    playlistDao.observePlaylist(playlistId)

  fun observePlaylistTracks(playlistId: Long): Flow<List<PlaylistTrackEntry>> =
    playlistDao.observePlaylistTracks(playlistId).map { entities ->
      entities.map { entity ->
        val resolvedMediaType =
          runCatching { MediaType.valueOf(entity.mediaType) }
            .getOrElse {
              if (entity.mimeType.startsWith("video/", ignoreCase = true)) MediaType.VIDEO
              else MediaType.AUDIO
            }
        PlaylistTrackEntry(
          entryId = entity.id,
          playlistId = entity.playlistId,
          track =
            AudioTrack(
              id = entity.trackId,
              title = entity.title,
              artist = entity.artist,
              album = entity.album,
              albumId = entity.albumId,
              durationMs = entity.durationMs,
              contentUri = Uri.parse(entity.contentUri),
              albumArtUri = entity.albumArtUri?.let { Uri.parse(it) },
              dateAddedMs = entity.dateAddedDeviceMs,
              dateModifiedMs = entity.dateModifiedDeviceMs,
              sizeBytes = entity.sizeBytes,
              mimeType = entity.mimeType,
              folderName = entity.folderName,
              mediaType = resolvedMediaType,
            ),
          addedToPlaylistAtMs = entity.addedToPlaylistAtMs,
          manualOrder = entity.manualOrder,
        )
      }
    }

  suspend fun createPlaylist(name: String): Long {
    val cleanName = name.trim().ifBlank { "Playlist" }
    return playlistDao.insertPlaylist(PlaylistEntity(name = cleanName))
  }

  suspend fun deletePlaylist(playlistId: Long) {
    val existing = playlistDao.getPlaylistById(playlistId) ?: return
    if (existing.isFavorites) return // Default Favorites playlist cannot be deleted
    existing.coverPath?.let { path -> runCatching { File(path).delete() } }
    playlistDao.deletePlaylistById(playlistId)
  }

  suspend fun renamePlaylist(playlistId: Long, newName: String) {
    val cleanName = newName.trim()
    if (cleanName.isNotBlank()) {
      playlistDao.renamePlaylist(playlistId, cleanName)
    }
  }

  suspend fun updatePlaylistSort(playlistId: Long, sortOption: PlaylistSortOption) {
    playlistDao.updatePlaylistSortOption(playlistId, sortOption.name)
  }

  suspend fun savePlaylistCoverFromUri(playlistId: Long, sourceUri: Uri): String? =
    withContext(Dispatchers.IO) {
      val coversDir = File(context.filesDir, "playlist_covers").apply { mkdirs() }
      val targetFile = File(coversDir, "cover_${playlistId}_${UUID.randomUUID()}.jpg")

      val copied =
        runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
              FileOutputStream(targetFile).use { output -> input.copyTo(output) }
            }
            targetFile.exists() && targetFile.length() > 0L
          }
          .getOrDefault(false)

      if (!copied) {
        runCatching { targetFile.delete() }
        return@withContext null
      }

      val previous = playlistDao.getPlaylistById(playlistId)
      previous?.coverPath?.let { oldPath ->
        if (oldPath != targetFile.absolutePath) {
          runCatching { File(oldPath).delete() }
        }
      }

      playlistDao.updatePlaylistCover(playlistId, targetFile.absolutePath)
      targetFile.absolutePath
    }

  suspend fun clearPlaylistCover(playlistId: Long) {
    val previous = playlistDao.getPlaylistById(playlistId)
    previous?.coverPath?.let { oldPath -> runCatching { File(oldPath).delete() } }
    playlistDao.updatePlaylistCover(playlistId, null)
  }

  suspend fun addTracksToPlaylist(playlistId: Long, tracks: List<AudioTrack>): Int {
    if (tracks.isEmpty()) return 0
    var nextOrder = playlistDao.getMaxManualOrder(playlistId) + 1
    val now = System.currentTimeMillis()
    val toInsert = mutableListOf<PlaylistTrackEntity>()

    for (track in tracks) {
      val uriStr = track.contentUri.toString()
      val exists = playlistDao.countTrackInPlaylistByUriOrId(playlistId, uriStr, track.id) > 0
      if (!exists) {
        toInsert.add(
          PlaylistTrackEntity(
            playlistId = playlistId,
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            albumId = track.albumId,
            durationMs = track.durationMs,
            contentUri = uriStr,
            albumArtUri = track.albumArtUri?.toString(),
            dateAddedDeviceMs = track.dateAddedMs,
            dateModifiedDeviceMs = track.dateModifiedMs,
            sizeBytes = track.sizeBytes,
            mimeType = track.mimeType,
            folderName = track.folderName,
            addedToPlaylistAtMs = now,
            manualOrder = nextOrder++,
            mediaType = track.mediaType.name,
          )
        )
      }
    }

    if (toInsert.isNotEmpty()) {
      playlistDao.insertPlaylistTracks(toInsert)
    }
    return toInsert.size
  }

  suspend fun removeTrackFromPlaylist(entryId: Long) {
    playlistDao.removeTrackByEntryId(entryId)
  }

  suspend fun persistManualOrder(orderedEntryIds: List<Long>) {
    playlistDao.reorderPlaylistTracks(orderedEntryIds)
  }

  suspend fun getBackupPlaylistsSnapshot(): List<BackupPlaylist> =
    withContext(Dispatchers.IO) {
      val entities = playlistDao.getAllPlaylistsSnapshot()
      entities.map { entity ->
        val tracks = playlistDao.getPlaylistTracksSnapshot(entity.id)
        BackupPlaylist(
          name = entity.name,
          sortOption = entity.sortOption,
          createdAtMs = entity.createdAtMs,
          isFavorites = entity.isFavorites,
          tracks =
            tracks.map { t ->
              BackupPlaylistTrack(
                trackId = t.trackId,
                title = t.title,
                artist = t.artist,
                album = t.album,
                albumId = t.albumId,
                durationMs = t.durationMs,
                contentUri = t.contentUri,
                albumArtUri = t.albumArtUri,
                dateAddedDeviceMs = t.dateAddedDeviceMs,
                dateModifiedDeviceMs = t.dateModifiedDeviceMs,
                sizeBytes = t.sizeBytes,
                mimeType = t.mimeType,
                folderName = t.folderName,
                addedToPlaylistAtMs = t.addedToPlaylistAtMs,
                manualOrder = t.manualOrder,
                mediaType = t.mediaType,
              )
            },
        )
      }
    }

  suspend fun syncPlaylistsBackupAddOnly(externalDocumentUri: String?): PlaylistBackupSyncResult =
    withContext(Dispatchers.IO) {
      val snapshot = getBackupPlaylistsSnapshot()
      PlaylistBackupEngine.syncPlaylistsAddOnlyToFiles(
        context = context,
        currentPlaylists = snapshot,
        externalDocumentUri = externalDocumentUri,
      )
    }

  suspend fun importPlaylistsFromUri(uri: Uri): PlaylistImportResult =
    withContext(Dispatchers.IO) {
      val jsonText = UsageStatsEngine.readTextFromUri(context, uri)
      if (jsonText.isNullOrBlank()) {
        return@withContext PlaylistImportResult(
          success = false,
          errorMessage = "Unable to read selected playlists JSON file",
        )
      }
      importPlaylistsFromJson(jsonText)
    }

  suspend fun importPlaylistsFromJson(jsonText: String): PlaylistImportResult =
    withContext(Dispatchers.IO) {
      val incomingPlaylists =
        runCatching { PlaylistBackupEngine.parsePlaylistsJson(jsonText) }
          .getOrElse { err ->
            return@withContext PlaylistImportResult(success = false, errorMessage = err.message)
          }

      if (incomingPlaylists.isEmpty()) {
        return@withContext PlaylistImportResult(
          success = false,
          errorMessage = "No valid playlists found in JSON",
        )
      }

      val existingPlaylists = playlistDao.getAllPlaylistsSnapshot().toMutableList()
      var restoredPlaylistsCount = 0
      var restoredTracksCount = 0

      for (incoming in incomingPlaylists) {
        val matchingPlaylist =
          if (incoming.isFavorites) {
            existingPlaylists.firstOrNull { it.isFavorites }
          } else {
            existingPlaylists.firstOrNull {
              !it.isFavorites && it.name.trim().equals(incoming.name.trim(), ignoreCase = true)
            }
          } ?: existingPlaylists.firstOrNull {
            it.name.trim().equals(incoming.name.trim(), ignoreCase = true)
          }

        val targetPlaylistId =
          if (matchingPlaylist != null) {
            matchingPlaylist.id
          } else {
            val insertedId =
              playlistDao.insertPlaylist(
                PlaylistEntity(
                  name = incoming.name.trim(),
                  sortOption = incoming.sortOption,
                  createdAtMs = incoming.createdAtMs,
                  isFavorites = incoming.isFavorites,
                )
              )
            existingPlaylists.add(
              PlaylistEntity(
                id = insertedId,
                name = incoming.name.trim(),
                sortOption = incoming.sortOption,
                createdAtMs = incoming.createdAtMs,
                isFavorites = incoming.isFavorites,
              )
            )
            restoredPlaylistsCount++
            insertedId
          }

        val existingTracks = playlistDao.getPlaylistTracksSnapshot(targetPlaylistId)
        val existingTrackKeys =
          existingTracks
            .map { track -> track.contentUri.ifBlank { "id:${track.trackId}" } }
            .toHashSet()
        var nextOrder = (existingTracks.maxOfOrNull { it.manualOrder } ?: -1) + 1
        val toInsert = mutableListOf<PlaylistTrackEntity>()

        for (t in incoming.tracks) {
          val trackKey = t.contentUri.ifBlank { "id:${t.trackId}" }
          if (existingTrackKeys.add(trackKey)) {
            toInsert.add(
              PlaylistTrackEntity(
                playlistId = targetPlaylistId,
                trackId = t.trackId,
                title = t.title,
                artist = t.artist,
                album = t.album,
                albumId = t.albumId,
                durationMs = t.durationMs,
                contentUri = t.contentUri,
                albumArtUri = t.albumArtUri,
                dateAddedDeviceMs = t.dateAddedDeviceMs,
                dateModifiedDeviceMs = t.dateModifiedDeviceMs,
                sizeBytes = t.sizeBytes,
                mimeType = t.mimeType,
                folderName = t.folderName,
                addedToPlaylistAtMs = t.addedToPlaylistAtMs,
                manualOrder = nextOrder++,
                mediaType = t.mediaType,
              )
            )
          }
        }

        if (toInsert.isNotEmpty()) {
          playlistDao.insertPlaylistTracks(toInsert)
          restoredTracksCount += toInsert.size
          if (matchingPlaylist != null) {
            restoredPlaylistsCount++
          }
        }
      }

      PlaylistImportResult(
        success = true,
        importedPlaylistsCount = restoredPlaylistsCount,
        importedTracksCount = restoredTracksCount,
      )
    }

  suspend fun recordTrackPlay(
    track: AudioTrack,
    resetPositionToZero: Boolean = false,
  ): Long =
    historyMutex.withLock {
      withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val uriString = track.contentUri.toString()
        val existing = playHistoryDao.getLatestHistoryForUri(uriString)
        val preservedLastPosition =
          if (resetPositionToZero) {
            playHistoryDao.resetLastPositionForUri(uriString)
            0L
          } else {
            existing?.lastPositionMs ?: 0L
          }
        // Insert an immutable timestamped session so replaying an older track today
        // never shifts past plays or historical duration into Today's analytics timeframe.
        val eventId =
          playHistoryDao.insertPlayEvent(
            PlayHistoryEntity(
              trackId = track.id,
              title = track.title,
              artist = track.artist,
              album = track.album,
              albumArtUri = track.albumArtUri?.toString(),
              contentUri = uriString,
              durationMs = if (track.durationMs > 0L) track.durationMs else (existing?.durationMs ?: 0L),
              playedAtMs = now,
              listenedMs = 0L,
              playedAtIso = UsageStatsEngine.formatIsoTimestamp(now),
              lastPositionMs = preservedLastPosition,
              mediaType = track.mediaType.name,
              playCount = 1,
            )
          )
        // Retain up to 400 days locally so Daily / Weekly / Monthly / Yearly views have full real data
        val fourHundredDaysAgo = now - 400L * 24L * 60L * 60L * 1000L
        playHistoryDao.pruneHistoryOlderThan(fourHundredDaysAgo)
        eventId
      }
    }

  suspend fun addListenedDuration(eventId: Long, deltaMs: Long) {
    if (eventId <= 0L || deltaMs <= 0L) return
    playHistoryDao.incrementListenedMs(eventId, deltaMs)
  }

  suspend fun updateLastPlaybackPosition(eventId: Long, positionMs: Long) {
    if (eventId <= 0L) return
    playHistoryDao.updateLastPosition(eventId, positionMs.coerceAtLeast(0L))
  }

  suspend fun getAutoResumePositionMs(track: AudioTrack): Long =
    withContext(Dispatchers.IO) {
      val latest = playHistoryDao.getLatestHistoryForUri(track.contentUri.toString()) ?: return@withContext 0L
      val effectiveDuration = if (track.durationMs > 0L) track.durationMs else latest.durationMs
      computeAutoResumePositionMs(
        durationMs = effectiveDuration,
        isVideo = track.isVideo,
        savedPositionMs = latest.lastPositionMs,
      )
    }

  suspend fun clearSavedResumePosition(contentUri: Uri) {
    withContext(Dispatchers.IO) {
      playHistoryDao.resetLastPositionForUri(contentUri.toString())
    }
  }

  fun observePlaybackHistory(): Flow<List<PlaybackHistoryItem>> =
    playHistoryDao.observeAllHistory().map { events ->
      events.map { ev ->
        val resolvedType =
          runCatching { MediaType.valueOf(ev.mediaType) }.getOrDefault(MediaType.AUDIO)
        PlaybackHistoryItem(
          eventId = ev.id,
          track =
            AudioTrack(
              id = ev.trackId,
              title = ev.title,
              artist = ev.artist,
              album = ev.album,
              albumId = 0L,
              durationMs = ev.durationMs,
              contentUri = Uri.parse(ev.contentUri),
              albumArtUri = ev.albumArtUri?.let { Uri.parse(it) },
              dateAddedMs = ev.playedAtMs,
              dateModifiedMs = ev.playedAtMs,
              sizeBytes = 0L,
              mimeType = if (resolvedType == MediaType.VIDEO) "video/*" else "audio/*",
              folderName = ev.album,
              mediaType = resolvedType,
            ),
          playedAtMs = ev.playedAtMs,
          playedAtIso = ev.playedAtIso,
          listenedMs = ev.listenedMs,
          lastPositionMs = ev.lastPositionMs,
        )
      }.distinctBy { item -> item.track.contentUri.toString().ifBlank { "id:${item.eventId}" } }
    }

  suspend fun deleteHistoryItem(eventId: Long) {
    playHistoryDao.deleteHistoryEvent(eventId)
  }

  suspend fun clearHistory() {
    playHistoryDao.clearAllHistory()
  }

  fun observeUsageSummary(timeframeFlow: Flow<AnalyticsTimeframe>): Flow<WeeklySummaryReport> =
    combine(playHistoryDao.observeAllHistory(), timeframeFlow) { events, timeframe ->
      UsageStatsEngine.aggregateUsageReport(events = events, timeframe = timeframe)
    }

  fun observeWeeklySummary(): Flow<WeeklySummaryReport> =
    observeUsageSummary(flowOf(AnalyticsTimeframe.WEEKLY))

  suspend fun syncUsageStatsJson(externalDocumentUri: String?): UsageFileSyncResult =
    withContext(Dispatchers.IO) {
      val snapshot = playHistoryDao.getAllHistorySnapshot()
      UsageStatsEngine.syncStatsToLocalFiles(
        context = context,
        events = snapshot,
        externalDocumentUri = externalDocumentUri,
      )
    }

  suspend fun importUsageStatsFromUri(uri: Uri): UsageImportResult =
    withContext(Dispatchers.IO) {
      val jsonText = UsageStatsEngine.readTextFromUri(context, uri)
      if (jsonText.isNullOrBlank()) {
        return@withContext UsageImportResult(
          success = false,
          errorMessage = "Unable to read selected JSON file",
        )
      }
      importUsageStatsFromJson(jsonText)
    }

  suspend fun importUsageStatsFromJson(jsonText: String): UsageImportResult =
    withContext(Dispatchers.IO) {
      val parsedEvents =
        runCatching { UsageStatsEngine.parseUsageStatsJson(jsonText) }
          .getOrElse { err ->
            return@withContext UsageImportResult(success = false, errorMessage = err.message)
          }

      if (parsedEvents.isEmpty()) {
        return@withContext UsageImportResult(
          success = false,
          errorMessage = "No valid listening events found in JSON",
        )
      }

      val existing = playHistoryDao.getAllHistorySnapshot()
      val existingBySession =
        existing
          .associateBy {
            val uriKey = it.contentUri.ifBlank { "id:${it.trackId}|${it.title.lowercase()}" }
            "$uriKey|${it.playedAtMs}"
          }
          .toMutableMap()

      val toInsert = mutableListOf<PlayHistoryEntity>()
      var updatedCount = 0
      var skippedCount = 0

      for (incoming in parsedEvents) {
        val uriKey = incoming.contentUri.ifBlank { "id:${incoming.trackId}|${incoming.title.lowercase()}" }
        val sessionKey = "$uriKey|${incoming.playedAtMs}"
        val matched = existingBySession[sessionKey]
        if (matched == null) {
          val normalized = incoming.copy(id = 0L)
          toInsert.add(normalized)
          existingBySession[sessionKey] = normalized
        } else {
          val mergedPlayCount = maxOf(matched.playCount.coerceAtLeast(1), incoming.playCount.coerceAtLeast(1))
          val mergedListened = maxOf(matched.listenedMs, incoming.listenedMs)
          val mergedLastPos =
            if (incoming.playedAtMs >= matched.playedAtMs) incoming.lastPositionMs else matched.lastPositionMs
          val mergedPlayedAt = maxOf(matched.playedAtMs, incoming.playedAtMs)
          val mergedIso =
            if (incoming.playedAtMs >= matched.playedAtMs && incoming.playedAtIso.isNotBlank()) {
              incoming.playedAtIso
            } else {
              matched.playedAtIso
            }
          if (mergedPlayCount > matched.playCount ||
            mergedListened > matched.listenedMs ||
            mergedPlayedAt > matched.playedAtMs ||
            mergedLastPos != matched.lastPositionMs
          ) {
            val updated =
              matched.copy(
                playCount = mergedPlayCount,
                listenedMs = mergedListened,
                lastPositionMs = mergedLastPos,
                playedAtMs = mergedPlayedAt,
                playedAtIso = mergedIso,
              )
            if (matched.id > 0L) {
              playHistoryDao.updatePlayEvent(updated)
              updatedCount++
            }
            existingBySession[sessionKey] = updated
          } else {
            skippedCount++
          }
        }
      }

      if (toInsert.isNotEmpty()) {
        playHistoryDao.insertPlayEvents(toInsert)
      }

      UsageImportResult(
        success = true,
        importedCount = toInsert.size + updatedCount,
        skippedDuplicates = skippedCount,
      )
    }

  private fun CachedMediaEntity.toAudioTrack(): AudioTrack {
    val resolvedMediaType =
      runCatching { MediaType.valueOf(mediaType) }
        .getOrElse {
          if (mimeType.startsWith("video/", ignoreCase = true)) MediaType.VIDEO
          else MediaType.AUDIO
        }
    return AudioTrack(
      id = trackId,
      title = title,
      artist = artist,
      album = album,
      albumId = albumId,
      durationMs = durationMs,
      contentUri = Uri.parse(contentUri),
      albumArtUri = albumArtUri?.let { Uri.parse(it) },
      dateAddedMs = dateAddedMs,
      dateModifiedMs = dateModifiedMs,
      sizeBytes = sizeBytes,
      mimeType = mimeType,
      folderName = folderName,
      mediaType = resolvedMediaType,
    )
  }

  private fun CachedMediaEntity.toCachedRecord(): CachedMediaRecord =
    CachedMediaRecord(
      track = toAudioTrack(),
      sourceBucket = sourceBucket,
    )

  private fun CachedMediaRecord.toEntity(): CachedMediaEntity =
    CachedMediaEntity(
      contentUri = track.contentUri.toString(),
      trackId = track.id,
      title = track.title,
      artist = track.artist,
      album = track.album,
      albumId = track.albumId,
      durationMs = track.durationMs,
      albumArtUri = track.albumArtUri?.toString(),
      dateAddedMs = track.dateAddedMs,
      dateModifiedMs = track.dateModifiedMs,
      sizeBytes = track.sizeBytes,
      mimeType = track.mimeType,
      folderName = track.folderName,
      mediaType = track.mediaType.name,
      sourceBucket = sourceBucket,
    )
}

internal fun computeAutoResumePositionMs(
  durationMs: Long,
  isVideo: Boolean,
  savedPositionMs: Long,
): Long {
  if (savedPositionMs < 5_000L) return 0L
  val minLongDurationMs = if (isVideo) 60_000L else 300_000L
  if (durationMs > 0L) {
    if (durationMs < minLongDurationMs) return 0L
    if (savedPositionMs >= durationMs - 8_000L) return 0L
  } else if (!isVideo) {
    return 0L
  }
  return savedPositionMs
}

