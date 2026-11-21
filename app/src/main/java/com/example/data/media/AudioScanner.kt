package com.example.data.media

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Size
import com.example.model.AudioTrack
import com.example.model.MediaType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class CachedMediaRecord(
  val track: AudioTrack,
  val sourceBucket: String,
)

data class IncrementalSyncDiff(
  val upserts: List<CachedMediaRecord>,
  val deletedUris: List<String>,
  val resolvedTracks: List<AudioTrack>,
  val pendingHydration: List<CachedMediaRecord> = emptyList(),
)

class AudioScanner(private val context: Context) {

  suspend fun scanLocalAudio(
    unknownArtistFallback: String = "Unknown Artist",
    unknownAlbumFallback: String = "Local Audio",
    unknownVideoFallback: String = "Local Video",
    converterTreeUriString: String? = null,
  ): List<AudioTrack> =
    scanLocalMediaIncremental(
      existingRecords = emptyList(),
      includeAudio = true,
      includeVideo = true,
      unknownArtistFallback = unknownArtistFallback,
      unknownAlbumFallback = unknownAlbumFallback,
      unknownVideoFallback = unknownVideoFallback,
      converterTreeUriString = converterTreeUriString,
    ).resolvedTracks

  private data class ScanChunkResult(
    val queriedOk: Boolean,
    val bucket: String,
    val tracks: List<AudioTrack>,
    val upserts: List<CachedMediaRecord>,
    val hydration: List<CachedMediaRecord>,
    val uris: List<String>,
    val signatures: List<String>,
    val discoveredPaths: List<String> = emptyList(),
  )

  suspend fun scanLocalMediaIncremental(
    existingRecords: List<CachedMediaRecord>,
    includeAudio: Boolean = true,
    includeVideo: Boolean = true,
    unknownArtistFallback: String = "Unknown Artist",
    unknownAlbumFallback: String = "Local Audio",
    unknownVideoFallback: String = "Local Video",
    converterTreeUriString: String? = null,
    deferHeavyMetadataExtraction: Boolean = true,
  ): IncrementalSyncDiff =
    withContext(Dispatchers.IO) {
      val existingByUri = existingRecords.associateBy { it.track.contentUri.toString() }
      val artCacheDir = File(context.cacheDir, "track_art").apply { mkdirs() }

      val chunks =
        coroutineScope {
          val audioDeferred =
            async(Dispatchers.IO) {
              val chunkTracks = mutableListOf<AudioTrack>()
              val chunkUpserts = mutableListOf<CachedMediaRecord>()
              val chunkUris = HashSet<String>()
              val chunkSigs = HashSet<String>()
              val chunkHydration = mutableListOf<CachedMediaRecord>()
              val ok =
                if (includeAudio) {
                  scanAudioMediaStore(
                    artCacheDir = artCacheDir,
                    unknownArtistFallback = unknownArtistFallback,
                    unknownAlbumFallback = unknownAlbumFallback,
                    out = chunkTracks,
                    seenUris = chunkUris,
                    seenFileSignatures = chunkSigs,
                    existingByUri = existingByUri,
                    upserts = chunkUpserts,
                    deferHeavyMetadataExtraction = deferHeavyMetadataExtraction,
                    pendingHydration = chunkHydration,
                  )
                } else {
                  existingRecords
                    .filter { it.sourceBucket == BUCKET_AUDIO }
                    .forEach { rec ->
                      if (chunkUris.add(rec.track.contentUri.toString())) {
                        chunkSigs.add(mediaSignature(rec.track.mediaType, rec.track.title, rec.track.sizeBytes))
                        chunkTracks.add(rec.track)
                      }
                    }
                  false
                }
              ScanChunkResult(
                queriedOk = ok,
                bucket = BUCKET_AUDIO,
                tracks = chunkTracks,
                upserts = chunkUpserts,
                hydration = chunkHydration,
                uris = chunkUris.toList(),
                signatures = chunkSigs.toList(),
              )
            }

          val videoDeferred =
            async(Dispatchers.IO) {
              val chunkTracks = mutableListOf<AudioTrack>()
              val chunkUpserts = mutableListOf<CachedMediaRecord>()
              val chunkUris = HashSet<String>()
              val chunkSigs = HashSet<String>()
              val chunkHydration = mutableListOf<CachedMediaRecord>()
              val ok =
                if (includeVideo) {
                  scanVideoMediaStore(
                    artCacheDir = artCacheDir,
                    unknownArtistFallback = unknownArtistFallback,
                    unknownVideoFallback = unknownVideoFallback,
                    out = chunkTracks,
                    seenUris = chunkUris,
                    seenFileSignatures = chunkSigs,
                    existingByUri = existingByUri,
                    upserts = chunkUpserts,
                    deferHeavyMetadataExtraction = deferHeavyMetadataExtraction,
                    pendingHydration = chunkHydration,
                  )
                } else {
                  existingRecords
                    .filter { it.sourceBucket == BUCKET_VIDEO }
                    .forEach { rec ->
                      if (chunkUris.add(rec.track.contentUri.toString())) {
                        chunkSigs.add(mediaSignature(rec.track.mediaType, rec.track.title, rec.track.sizeBytes))
                        chunkTracks.add(rec.track)
                      }
                    }
                  false
                }
              ScanChunkResult(
                queriedOk = ok,
                bucket = BUCKET_VIDEO,
                tracks = chunkTracks,
                upserts = chunkUpserts,
                hydration = chunkHydration,
                uris = chunkUris.toList(),
                signatures = chunkSigs.toList(),
              )
            }

          val safDeferred =
            async(Dispatchers.IO) {
              val chunkTracks = mutableListOf<AudioTrack>()
              val chunkUpserts = mutableListOf<CachedMediaRecord>()
              val chunkUris = HashSet<String>()
              val chunkSigs = HashSet<String>()
              val chunkHydration = mutableListOf<CachedMediaRecord>()
              val safReconciled =
                scanSafConvertedFolder(
                  treeUriString = converterTreeUriString,
                  artCacheDir = artCacheDir,
                  unknownArtistFallback = unknownArtistFallback,
                  unknownAlbumFallback = unknownAlbumFallback,
                  out = chunkTracks,
                  seenUris = chunkUris,
                  seenFileSignatures = chunkSigs,
                  existingByUri = existingByUri,
                  upserts = chunkUpserts,
                  deferHeavyMetadataExtraction = deferHeavyMetadataExtraction,
                  pendingHydration = chunkHydration,
                )
              ScanChunkResult(
                queriedOk = safReconciled || converterTreeUriString.isNullOrBlank(),
                bucket = BUCKET_SAF,
                tracks = chunkTracks,
                upserts = chunkUpserts,
                hydration = chunkHydration,
                uris = chunkUris.toList(),
                signatures = chunkSigs.toList(),
              )
            }

          val appDirDeferred =
            async(Dispatchers.IO) {
              val chunkTracks = mutableListOf<AudioTrack>()
              val chunkUpserts = mutableListOf<CachedMediaRecord>()
              val chunkUris = HashSet<String>()
              val chunkSigs = HashSet<String>()
              val chunkHydration = mutableListOf<CachedMediaRecord>()
              scanAppScopedConvertedFolder(
                artCacheDir = artCacheDir,
                unknownArtistFallback = unknownArtistFallback,
                unknownAlbumFallback = unknownAlbumFallback,
                out = chunkTracks,
                seenUris = chunkUris,
                seenFileSignatures = chunkSigs,
                existingByUri = existingByUri,
                upserts = chunkUpserts,
                deferHeavyMetadataExtraction = deferHeavyMetadataExtraction,
                pendingHydration = chunkHydration,
              )
              ScanChunkResult(
                queriedOk = true,
                bucket = BUCKET_APP_DIR,
                tracks = chunkTracks,
                upserts = chunkUpserts,
                hydration = chunkHydration,
                uris = chunkUris.toList(),
                signatures = chunkSigs.toList(),
              )
            }

          val directDirDeferred =
            async(Dispatchers.IO) {
              scanDirectMediaDirectoriesConcurrent(
                artCacheDir = artCacheDir,
                includeAudio = includeAudio,
                includeVideo = includeVideo,
                unknownArtistFallback = unknownArtistFallback,
                unknownAlbumFallback = unknownAlbumFallback,
                unknownVideoFallback = unknownVideoFallback,
                existingByUri = existingByUri,
                deferHeavyMetadataExtraction = deferHeavyMetadataExtraction,
              )
            }

          listOf(
            audioDeferred.await(),
            videoDeferred.await(),
            safDeferred.await(),
            appDirDeferred.await(),
            directDirDeferred.await(),
          )
        }

      val tracks = mutableListOf<AudioTrack>()
      val upserts = mutableListOf<CachedMediaRecord>()
      val pendingHydration = mutableListOf<CachedMediaRecord>()
      val seenUris = HashSet<String>()
      val seenFileSignatures = HashSet<String>()
      val reconciledBuckets = HashSet<String>()
      val newlyDiscoveredPaths = mutableListOf<String>()

      for (chunk in chunks) {
        if (chunk.queriedOk) reconciledBuckets.add(chunk.bucket)
        val isDirectChunk = chunk.bucket == BUCKET_DIRECT_DIR
        val upsertsByUri = chunk.upserts.associateBy { it.track.contentUri.toString() }
        val hydrationByUri = chunk.hydration.associateBy { it.track.contentUri.toString() }

        for (track in chunk.tracks) {
          val uriStr = track.contentUri.toString()
          val sig = mediaSignature(track.mediaType, track.title, track.sizeBytes)
          if (isDirectChunk && (uriStr in seenUris || sig in seenFileSignatures)) {
            continue
          }
          if (!seenUris.add(uriStr)) continue
          seenFileSignatures.add(sig)
          tracks.add(track)
          upsertsByUri[uriStr]?.let { upserts.add(it) }
          hydrationByUri[uriStr]?.let { pendingHydration.add(it) }
        }
        if (isDirectChunk && chunk.discoveredPaths.isNotEmpty()) {
          newlyDiscoveredPaths.addAll(chunk.discoveredPaths)
        }
      }

      if (newlyDiscoveredPaths.isNotEmpty()) {
        runCatching {
          MediaScannerConnection.scanFile(
            context,
            newlyDiscoveredPaths.toTypedArray(),
            null,
            null,
          )
        }
      }

      val deletedUris =
        existingRecords
          .asSequence()
          .filter { it.sourceBucket in reconciledBuckets }
          .map { it.track.contentUri.toString() }
          .filter { it !in seenUris }
          .toList()

      IncrementalSyncDiff(
        upserts = upserts,
        deletedUris = deletedUris,
        resolvedTracks = tracks.sortedByDescending { it.dateModifiedMs },
        pendingHydration = pendingHydration,
      )
    }

  private fun isCacheEntryValid(
    cached: CachedMediaRecord?,
    expectedBucket: String,
    dateModifiedMs: Long,
    sizeBytes: Long,
  ): Boolean {
    if (cached == null || cached.sourceBucket != expectedBucket) return false
    if (cached.track.dateModifiedMs != dateModifiedMs || cached.track.sizeBytes != sizeBytes) {
      return false
    }
    if (cached.track.title.isBlank()) return false
    val artUri = cached.track.albumArtUri
    if (artUri != null && artUri.scheme == "file") {
      val path = artUri.path
      if (path.isNullOrBlank()) return false
      val file = File(path)
      if (!file.exists() || file.length() <= 0L) return false
    }
    return true
  }

  private fun mediaSignature(mediaType: MediaType, nameOrTitle: String, sizeBytes: Long): String {
    val cleanStem = nameOrTitle.substringBeforeLast('.').trim().lowercase()
    return "${mediaType.name}|$cleanStem|$sizeBytes"
  }

  private fun scanAudioMediaStore(
    artCacheDir: File,
    unknownArtistFallback: String,
    unknownAlbumFallback: String,
    out: MutableList<AudioTrack>,
    seenUris: HashSet<String>,
    seenFileSignatures: HashSet<String>,
    existingByUri: Map<String, CachedMediaRecord> = emptyMap(),
    upserts: MutableList<CachedMediaRecord>? = null,
    deferHeavyMetadataExtraction: Boolean = false,
    pendingHydration: MutableList<CachedMediaRecord>? = null,
  ): Boolean {
    val collection =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
      } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
      }

    val projection = buildList {
      add(MediaStore.Audio.Media._ID)
      add(MediaStore.Audio.Media.TITLE)
      add(MediaStore.Audio.Media.DISPLAY_NAME)
      add(MediaStore.Audio.Media.ARTIST)
      add(MediaStore.Audio.Media.ALBUM)
      add(MediaStore.Audio.Media.ALBUM_ID)
      add(MediaStore.Audio.Media.DURATION)
      add(MediaStore.Audio.Media.DATE_ADDED)
      add(MediaStore.Audio.Media.DATE_MODIFIED)
      add(MediaStore.Audio.Media.SIZE)
      add(MediaStore.Audio.Media.MIME_TYPE)
      add(MediaStore.Audio.Media.IS_RINGTONE)
      add(MediaStore.Audio.Media.IS_NOTIFICATION)
      add(MediaStore.Audio.Media.IS_ALARM)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        add(MediaStore.Audio.Media.RELATIVE_PATH)
      } else {
        @Suppress("DEPRECATION")
        add(MediaStore.Audio.Media.DATA)
      }
    }.toTypedArray()

    val selection =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "${MediaStore.Audio.Media.SIZE} > 0 AND ${MediaStore.Audio.Media.IS_PENDING} = 0"
      } else {
        "${MediaStore.Audio.Media.SIZE} > 0"
      }
    val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"

    var queriedSuccessfully = false
    runCatching {
      context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
        queriedSuccessfully = true
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
        val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val ringtoneCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_RINGTONE)
        val notifCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_NOTIFICATION)
        val alarmCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_ALARM)
        val relPathCol =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
          } else {
            @Suppress("DEPRECATION")
            cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
          }

        while (cursor.moveToNext()) {
          val isSystemSound =
            cursor.getInt(ringtoneCol) == 1 ||
              cursor.getInt(notifCol) == 1 ||
              cursor.getInt(alarmCol) == 1
          val mediaStoreDurationMs = cursor.getLong(durationCol).coerceAtLeast(0L)
          if (isSystemSound && mediaStoreDurationMs < 15_000L) continue

          val mime = cursor.getString(mimeCol).orEmpty()
          val displayName = cursor.getString(displayNameCol).orEmpty()
          val isSupportedAudio =
            mime.startsWith("audio/") ||
              mime == "application/ogg" ||
              extensionOf(displayName) in AUDIO_EXT_SET
          if (!isSupportedAudio) continue

          val id = cursor.getLong(idCol)
          val dateAddedMs = cursor.getLong(dateAddedCol) * 1000L
          val dateModifiedMs = cursor.getLong(dateModCol) * 1000L
          val sizeBytes = cursor.getLong(sizeCol)
          val contentUri =
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
          val uriStr = contentUri.toString()
          if (!seenUris.add(uriStr)) continue

          val fileNameStem = displayName.substringBeforeLast('.').trim()
          if (displayName.isNotBlank()) {
            seenFileSignatures.add(mediaSignature(MediaType.AUDIO, displayName, sizeBytes))
          }

          // Fast incremental cache hit: skip heavy MediaMetadataRetriever when unchanged
          val cached = existingByUri[uriStr]
          if (isCacheEntryValid(cached, BUCKET_AUDIO, dateModifiedMs, sizeBytes)) {
            seenFileSignatures.add(
              mediaSignature(MediaType.AUDIO, cached!!.track.title, sizeBytes)
            )
            out.add(cached.track)
            continue
          }
          val rawTitle = cursor.getString(titleCol).orEmpty().trim()
          val rawArtist = cursor.getString(artistCol).orEmpty().trim()
          val rawAlbum = if (albumCol >= 0) cursor.getString(albumCol).orEmpty().trim() else ""
          val albumId = cursor.getLong(albumIdCol)
          val needsTitle = rawTitle.isBlank() || rawTitle == fileNameStem
          val needsArtist = rawArtist.isBlank() || rawArtist.equals("<unknown>", ignoreCase = true)
          val needsAlbum = rawAlbum.isBlank() || rawAlbum.equals("<unknown>", ignoreCase = true)
          val needsDuration = mediaStoreDurationMs <= 0L
          val artCacheKey = "aud_${id}_${dateModifiedMs}"
          val cachedArtFile = File(artCacheDir, "art_$artCacheKey.jpg")
          val noArtMarker = File(artCacheDir, "art_$artCacheKey.none")
          val hasDiskArt = cachedArtFile.exists() && cachedArtFile.length() > 0L

          val id3Parsed =
            if (deferHeavyMetadataExtraction ||
              (!needsTitle && !needsArtist && !needsAlbum && !needsDuration && (albumId > 0L || hasDiskArt || noArtMarker.exists()))
            ) {
              ParsedMediaMetadata(
                embeddedArtUri = if (hasDiskArt) Uri.fromFile(cachedArtFile) else null
              )
            } else {
              extractMediaMetadata(
                contentUri = contentUri,
                cacheKey = artCacheKey,
                artCacheDir = artCacheDir,
                isVideo = false,
                needsTitle = needsTitle,
                needsArtist = needsArtist,
                needsAlbum = needsAlbum,
                needsDuration = needsDuration,
              )
            }

          val cleanTitle =
            id3Parsed.title ?: rawTitle.ifBlank { fileNameStem.ifBlank { "Track $id" } }
          val cleanArtist =
            id3Parsed.artist
              ?: if (rawArtist.isBlank() || rawArtist.equals("<unknown>", ignoreCase = true)) {
                unknownArtistFallback
              } else {
                rawArtist
              }
          val cleanAlbum =
            id3Parsed.album
              ?: if (rawAlbum.isBlank() || rawAlbum.equals("<unknown>", ignoreCase = true)) {
                unknownAlbumFallback
              } else {
                rawAlbum
              }
          val finalDurationMs =
            id3Parsed.durationMs?.takeIf { it > 0L } ?: mediaStoreDurationMs

          val albumArtUri =
            id3Parsed.embeddedArtUri
              ?: if (albumId > 0L) {
                ContentUris.withAppendedId(
                  Uri.parse("content://media/external/audio/albumart"),
                  albumId,
                )
              } else {
                null
              }

          val rawFolder = if (relPathCol >= 0) cursor.getString(relPathCol).orEmpty() else ""
          val folderName =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              rawFolder.trimEnd('/').substringAfterLast('/', "Music").ifBlank { "Music" }
            } else {
              File(rawFolder).parentFile?.name?.ifBlank { "Music" } ?: "Music"
            }

          val track =
            AudioTrack(
              id = id,
              title = cleanTitle,
              artist = cleanArtist,
              album = cleanAlbum,
              albumId = albumId,
              durationMs = finalDurationMs,
              contentUri = contentUri,
              albumArtUri = albumArtUri,
              dateAddedMs = dateAddedMs,
              dateModifiedMs = dateModifiedMs,
              sizeBytes = sizeBytes,
              mimeType = mime.ifBlank { "audio/*" },
              folderName = folderName,
              mediaType = MediaType.AUDIO,
            )
          val record = CachedMediaRecord(track = track, sourceBucket = BUCKET_AUDIO)
          out.add(track)
          upserts?.add(record)
          if (deferHeavyMetadataExtraction &&
            (needsDuration || needsArtist || (albumArtUri == null && !noArtMarker.exists()))
          ) {
            pendingHydration?.add(record)
          }
        }
      }
    }
    return queriedSuccessfully
  }

  private fun scanVideoMediaStore(
    artCacheDir: File,
    unknownArtistFallback: String,
    unknownVideoFallback: String,
    out: MutableList<AudioTrack>,
    seenUris: HashSet<String>,
    seenFileSignatures: HashSet<String>,
    existingByUri: Map<String, CachedMediaRecord> = emptyMap(),
    upserts: MutableList<CachedMediaRecord>? = null,
    deferHeavyMetadataExtraction: Boolean = false,
    pendingHydration: MutableList<CachedMediaRecord>? = null,
  ): Boolean {
    val collection =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
      } else {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
      }

    val projection = buildList {
      add(MediaStore.Video.Media._ID)
      add(MediaStore.Video.Media.TITLE)
      add(MediaStore.Video.Media.DISPLAY_NAME)
      add(MediaStore.Video.Media.ARTIST)
      add(MediaStore.Video.Media.ALBUM)
      add(MediaStore.Video.Media.DURATION)
      add(MediaStore.Video.Media.DATE_ADDED)
      add(MediaStore.Video.Media.DATE_MODIFIED)
      add(MediaStore.Video.Media.SIZE)
      add(MediaStore.Video.Media.MIME_TYPE)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        add(MediaStore.Video.Media.RELATIVE_PATH)
      } else {
        @Suppress("DEPRECATION")
        add(MediaStore.Video.Media.DATA)
      }
    }.toTypedArray()

    val selection =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "${MediaStore.Video.Media.SIZE} > 0 AND ${MediaStore.Video.Media.IS_PENDING} = 0"
      } else {
        "${MediaStore.Video.Media.SIZE} > 0"
      }
    val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

    var queriedSuccessfully = false
    runCatching {
      context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
        queriedSuccessfully = true
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
        val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val artistCol = cursor.getColumnIndex(MediaStore.Video.Media.ARTIST)
        val albumCol = cursor.getColumnIndex(MediaStore.Video.Media.ALBUM)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
        val relPathCol =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
          } else {
            @Suppress("DEPRECATION")
            cursor.getColumnIndex(MediaStore.Video.Media.DATA)
          }

        while (cursor.moveToNext()) {
          val mime = cursor.getString(mimeCol).orEmpty()
          val displayName = cursor.getString(displayNameCol).orEmpty()
          val isSupportedVideo =
            mime.startsWith("video/") || extensionOf(displayName) in VIDEO_EXT_SET
          if (!isSupportedVideo) continue

          val rawId = cursor.getLong(idCol)
          // Offset video IDs into high bit range to guarantee zero ID collision with Audio MediaStore rows
          val uniqueId = rawId or VIDEO_ID_NAMESPACE_BIT
          val dateAddedMs = cursor.getLong(dateAddedCol) * 1000L
          val dateModifiedMs = cursor.getLong(dateModCol) * 1000L
          val sizeBytes = cursor.getLong(sizeCol)
          val mediaStoreDurationMs = cursor.getLong(durationCol).coerceAtLeast(0L)
          val contentUri =
            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, rawId)
          val uriStr = contentUri.toString()
          if (!seenUris.add(uriStr)) continue

          val fileNameStem = displayName.substringBeforeLast('.').trim()
          if (displayName.isNotBlank()) {
            seenFileSignatures.add(mediaSignature(MediaType.VIDEO, displayName, sizeBytes))
          }

          // Fast incremental cache hit: skip frame/metadata extraction when unchanged
          val cached = existingByUri[uriStr]
          if (isCacheEntryValid(cached, BUCKET_VIDEO, dateModifiedMs, sizeBytes)) {
            seenFileSignatures.add(
              mediaSignature(MediaType.VIDEO, cached!!.track.title, sizeBytes)
            )
            out.add(cached.track)
            continue
          }
          val rawTitle = cursor.getString(titleCol).orEmpty().trim()
          val rawArtist = if (artistCol >= 0) cursor.getString(artistCol).orEmpty().trim() else ""
          val rawAlbum = if (albumCol >= 0) cursor.getString(albumCol).orEmpty().trim() else ""
          val cacheKey = "vid_${rawId}_${dateModifiedMs}"
          val cachedArtFile = File(artCacheDir, "art_$cacheKey.jpg")
          val noArtMarker = File(artCacheDir, "art_$cacheKey.none")
          val hasDiskArt = cachedArtFile.exists() && cachedArtFile.length() > 0L

          val parsed =
            if (deferHeavyMetadataExtraction) {
              ParsedMediaMetadata(
                embeddedArtUri = if (hasDiskArt) Uri.fromFile(cachedArtFile) else null
              )
            } else {
              extractMediaMetadata(
                contentUri = contentUri,
                cacheKey = cacheKey,
                artCacheDir = artCacheDir,
                isVideo = true,
                needsTitle = rawTitle.isBlank(),
                needsArtist = rawArtist.isBlank() || rawArtist.equals("<unknown>", ignoreCase = true),
                needsAlbum = rawAlbum.isBlank() || rawAlbum.equals("<unknown>", ignoreCase = true),
                needsDuration = mediaStoreDurationMs <= 0L,
              )
            }

          val cleanTitle =
            fileNameStem.ifBlank { parsed.title ?: rawTitle.ifBlank { "Video $rawId" } }
          val cleanArtist =
            parsed.artist
              ?: if (rawArtist.isBlank() || rawArtist.equals("<unknown>", ignoreCase = true)) {
                unknownVideoFallback
              } else {
                rawArtist
              }
          val cleanAlbum =
            parsed.album
              ?: if (rawAlbum.isBlank() || rawAlbum.equals("<unknown>", ignoreCase = true)) {
                unknownVideoFallback
              } else {
                rawAlbum
              }
          val finalDurationMs = parsed.durationMs?.takeIf { it > 0L } ?: mediaStoreDurationMs

          val rawFolder = if (relPathCol >= 0) cursor.getString(relPathCol).orEmpty() else ""
          val folderName =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              rawFolder.trimEnd('/').substringAfterLast('/', "Videos").ifBlank { "Videos" }
            } else {
              File(rawFolder).parentFile?.name?.ifBlank { "Videos" } ?: "Videos"
            }

          val track =
            AudioTrack(
              id = uniqueId,
              title = cleanTitle,
              artist = cleanArtist.ifBlank { unknownArtistFallback },
              album = cleanAlbum,
              albumId = 0L,
              durationMs = finalDurationMs,
              contentUri = contentUri,
              albumArtUri = parsed.embeddedArtUri,
              dateAddedMs = dateAddedMs,
              dateModifiedMs = dateModifiedMs,
              sizeBytes = sizeBytes,
              mimeType = mime.ifBlank { "video/*" },
              folderName = folderName,
              mediaType = MediaType.VIDEO,
            )
          val record = CachedMediaRecord(track = track, sourceBucket = BUCKET_VIDEO)
          out.add(track)
          upserts?.add(record)
          if (deferHeavyMetadataExtraction &&
            (mediaStoreDurationMs <= 0L || (parsed.embeddedArtUri == null && !noArtMarker.exists()))
          ) {
            pendingHydration?.add(record)
          }
        }
      }
    }
    return queriedSuccessfully
  }

  private fun scanSafConvertedFolder(
    treeUriString: String?,
    artCacheDir: File,
    unknownArtistFallback: String,
    unknownAlbumFallback: String,
    out: MutableList<AudioTrack>,
    seenUris: HashSet<String>,
    seenFileSignatures: HashSet<String>,
    existingByUri: Map<String, CachedMediaRecord> = emptyMap(),
    upserts: MutableList<CachedMediaRecord>? = null,
    deferHeavyMetadataExtraction: Boolean = false,
    pendingHydration: MutableList<CachedMediaRecord>? = null,
  ): Boolean {
    if (!MediaConverter.hasValidTreePermission(context, treeUriString)) return false
    val treeUri = runCatching { Uri.parse(treeUriString) }.getOrNull() ?: return false
    val dirInfo = MediaConverter.ensureSafAppDirectory(context, treeUri) ?: return false
    val (dirUri, _) = dirInfo
    val dirDocId = runCatching { DocumentsContract.getDocumentId(dirUri) }.getOrNull() ?: return false
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, dirDocId)

    var queriedSuccessfully = false
    runCatching {
      context.contentResolver.query(
        childrenUri,
        arrayOf(
          DocumentsContract.Document.COLUMN_DOCUMENT_ID,
          DocumentsContract.Document.COLUMN_DISPLAY_NAME,
          DocumentsContract.Document.COLUMN_MIME_TYPE,
          DocumentsContract.Document.COLUMN_LAST_MODIFIED,
          DocumentsContract.Document.COLUMN_SIZE,
        ),
        null,
        null,
        null,
      )?.use { cursor ->
        queriedSuccessfully = true
        while (cursor.moveToNext()) {
          val docId = cursor.getString(0).orEmpty()
          val name = cursor.getString(1).orEmpty()
          val mime = cursor.getString(2).orEmpty()
          val lastMod = cursor.getLong(3)
          val size = cursor.getLong(4)
          if (size <= 0L) continue
          val ext = extensionOf(name)
          val isVideo = mime.startsWith("video/") || ext in VIDEO_EXT_SET
          val isAudio = mime.startsWith("audio/") || ext in AUDIO_EXT_SET
          if (!isVideo && !isAudio) continue

          val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
          val uriStr = docUri.toString()
          val type = if (isVideo) MediaType.VIDEO else MediaType.AUDIO
          val signature = mediaSignature(type, name, size)
          if (!seenFileSignatures.add(signature) || !seenUris.add(uriStr)) continue

          val cached = existingByUri[uriStr]
          if (isCacheEntryValid(cached, BUCKET_SAF, lastMod, size)) {
            out.add(cached!!.track)
            continue
          }

          val syntheticId =
            (uriStr.hashCode().toLong() and 0x3FFFFFFFL) or
              if (isVideo) VIDEO_ID_NAMESPACE_BIT else SAF_AUDIO_NAMESPACE_BIT

          val cacheKey = "saf_${syntheticId}_$lastMod"
          val cachedArtFile = File(artCacheDir, "art_$cacheKey.jpg")
          val noArtMarker = File(artCacheDir, "art_$cacheKey.none")
          val hasDiskArt = cachedArtFile.exists() && cachedArtFile.length() > 0L

          val parsed =
            if (deferHeavyMetadataExtraction) {
              ParsedMediaMetadata(
                embeddedArtUri = if (hasDiskArt) Uri.fromFile(cachedArtFile) else null
              )
            } else {
              extractMediaMetadata(
                contentUri = docUri,
                cacheKey = cacheKey,
                artCacheDir = artCacheDir,
                isVideo = isVideo,
                needsTitle = true,
                needsArtist = true,
                needsAlbum = true,
                needsDuration = true,
              )
            }

          val stem = name.substringBeforeLast('.').ifBlank { name }
          val track =
            AudioTrack(
              id = syntheticId,
              title = parsed.title ?: stem,
              artist = parsed.artist ?: unknownArtistFallback,
              album = parsed.album ?: unknownAlbumFallback,
              albumId = 0L,
              durationMs = parsed.durationMs ?: 0L,
              contentUri = docUri,
              albumArtUri = parsed.embeddedArtUri,
              dateAddedMs = lastMod,
              dateModifiedMs = lastMod,
              sizeBytes = size,
              mimeType = mime.ifBlank { if (isVideo) "video/*" else "audio/*" },
              folderName = MediaConverter.APP_FOLDER_NAME,
              mediaType = if (isVideo) MediaType.VIDEO else MediaType.AUDIO,
            )
          val record = CachedMediaRecord(track = track, sourceBucket = BUCKET_SAF)
          out.add(track)
          upserts?.add(record)
          if (deferHeavyMetadataExtraction && !hasDiskArt && !noArtMarker.exists()) {
            pendingHydration?.add(record)
          }
        }
      }
    }
    return queriedSuccessfully
  }

  private fun scanAppScopedConvertedFolder(
    artCacheDir: File,
    unknownArtistFallback: String,
    unknownAlbumFallback: String,
    out: MutableList<AudioTrack>,
    seenUris: HashSet<String>,
    seenFileSignatures: HashSet<String>,
    existingByUri: Map<String, CachedMediaRecord> = emptyMap(),
    upserts: MutableList<CachedMediaRecord>? = null,
    deferHeavyMetadataExtraction: Boolean = false,
    pendingHydration: MutableList<CachedMediaRecord>? = null,
  ) {
    val dirs =
      listOfNotNull(
        context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.let { File(it, MediaConverter.APP_FOLDER_NAME) },
        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.let { File(it, MediaConverter.APP_FOLDER_NAME) },
      )
    for (dir in dirs) {
      if (!dir.exists() || !dir.isDirectory) continue
      dir.listFiles()?.forEach { file ->
        val ext = extensionOf(file.name)
        val isVideo = ext in VIDEO_EXT_SET
        val isAudio = !isVideo && ext in AUDIO_EXT_SET
        if ((!isVideo && !isAudio) || !file.isFile || file.length() <= 0L) return@forEach

        val lastMod = file.lastModified()
        val sizeBytes = file.length()
        val fileUri = Uri.fromFile(file)
        val uriStr = fileUri.toString()
        val type = if (isVideo) MediaType.VIDEO else MediaType.AUDIO
        val signature = mediaSignature(type, file.name, sizeBytes)
        if (!seenFileSignatures.add(signature) || !seenUris.add(uriStr)) return@forEach

        val cached = existingByUri[uriStr]
        if (isCacheEntryValid(cached, BUCKET_APP_DIR, lastMod, sizeBytes)) {
          out.add(cached!!.track)
          return@forEach
        }

        val syntheticId =
          (uriStr.hashCode().toLong() and 0x3FFFFFFFL) or
            if (isVideo) VIDEO_ID_NAMESPACE_BIT else SAF_AUDIO_NAMESPACE_BIT
        val cacheKey = "appdir_${syntheticId}_$lastMod"
        val cachedArtFile = File(artCacheDir, "art_$cacheKey.jpg")
        val noArtMarker = File(artCacheDir, "art_$cacheKey.none")
        val hasDiskArt = cachedArtFile.exists() && cachedArtFile.length() > 0L

        val parsed =
          if (deferHeavyMetadataExtraction) {
            ParsedMediaMetadata(
              embeddedArtUri = if (hasDiskArt) Uri.fromFile(cachedArtFile) else null
            )
          } else {
            extractMediaMetadata(
              contentUri = fileUri,
              cacheKey = cacheKey,
              artCacheDir = artCacheDir,
              isVideo = isVideo,
              needsTitle = true,
              needsArtist = true,
              needsAlbum = true,
              needsDuration = true,
            )
          }
        val track =
          AudioTrack(
            id = syntheticId,
            title = parsed.title ?: file.nameWithoutExtension,
            artist = parsed.artist ?: unknownArtistFallback,
            album = parsed.album ?: unknownAlbumFallback,
            albumId = 0L,
            durationMs = parsed.durationMs ?: 0L,
            contentUri = fileUri,
            albumArtUri = parsed.embeddedArtUri,
            dateAddedMs = lastMod,
            dateModifiedMs = lastMod,
            sizeBytes = sizeBytes,
            mimeType = if (isVideo) "video/mp4" else "audio/mpeg",
            folderName = MediaConverter.APP_FOLDER_NAME,
            mediaType = if (isVideo) MediaType.VIDEO else MediaType.AUDIO,
          )
        val record = CachedMediaRecord(track = track, sourceBucket = BUCKET_APP_DIR)
        out.add(track)
        upserts?.add(record)
        if (deferHeavyMetadataExtraction && !hasDiskArt && !noArtMarker.exists()) {
          pendingHydration?.add(record)
        }
      }
    }
  }

  private suspend fun scanDirectMediaDirectoriesConcurrent(
    artCacheDir: File,
    includeAudio: Boolean,
    includeVideo: Boolean,
    unknownArtistFallback: String,
    unknownAlbumFallback: String,
    unknownVideoFallback: String,
    existingByUri: Map<String, CachedMediaRecord>,
    deferHeavyMetadataExtraction: Boolean,
  ): ScanChunkResult {
    if (!includeAudio && !includeVideo) {
      return ScanChunkResult(
        queriedOk = false,
        bucket = BUCKET_DIRECT_DIR,
        tracks = emptyList(),
        upserts = emptyList(),
        hydration = emptyList(),
        uris = emptyList(),
        signatures = emptyList(),
      )
    }
    val rootDirs = buildDefaultScanDirectories()
    val gate = Semaphore(4)
    val rootResults =
      coroutineScope {
        rootDirs.map { root ->
          async(Dispatchers.IO) {
            gate.withPermit {
              val chunkTracks = mutableListOf<AudioTrack>()
              val chunkUpserts = mutableListOf<CachedMediaRecord>()
              val chunkUris = HashSet<String>()
              val chunkSigs = HashSet<String>()
              val chunkHydration = mutableListOf<CachedMediaRecord>()
              val discovered =
                scanDirectoriesInternal(
                  roots = listOf(root),
                  artCacheDir = artCacheDir,
                  includeAudio = includeAudio,
                  includeVideo = includeVideo,
                  unknownArtistFallback = unknownArtistFallback,
                  unknownAlbumFallback = unknownAlbumFallback,
                  unknownVideoFallback = unknownVideoFallback,
                  out = chunkTracks,
                  seenUris = chunkUris,
                  seenFileSignatures = chunkSigs,
                  existingByUri = existingByUri,
                  upserts = chunkUpserts,
                  deferHeavyMetadataExtraction = deferHeavyMetadataExtraction,
                  pendingHydration = chunkHydration,
                )
              ScanChunkResult(
                queriedOk = true,
                bucket = BUCKET_DIRECT_DIR,
                tracks = chunkTracks,
                upserts = chunkUpserts,
                hydration = chunkHydration,
                uris = chunkUris.toList(),
                signatures = chunkSigs.toList(),
                discoveredPaths = discovered,
              )
            }
          }
        }.awaitAll()
      }

    val combinedTracks = mutableListOf<AudioTrack>()
    val combinedUpserts = mutableListOf<CachedMediaRecord>()
    val combinedHydration = mutableListOf<CachedMediaRecord>()
    val combinedUris = HashSet<String>()
    val combinedSigs = HashSet<String>()
    val combinedDiscovered = mutableListOf<String>()
    for (r in rootResults) {
      combinedTracks.addAll(r.tracks)
      combinedUpserts.addAll(r.upserts)
      combinedHydration.addAll(r.hydration)
      combinedUris.addAll(r.uris)
      combinedSigs.addAll(r.signatures)
      combinedDiscovered.addAll(r.discoveredPaths)
    }
    return ScanChunkResult(
      queriedOk = includeAudio && includeVideo,
      bucket = BUCKET_DIRECT_DIR,
      tracks = combinedTracks,
      upserts = combinedUpserts,
      hydration = combinedHydration,
      uris = combinedUris.toList(),
      signatures = combinedSigs.toList(),
      discoveredPaths = combinedDiscovered,
    )
  }

  internal fun defaultMediaDirectories(): List<File> = buildDefaultScanDirectories()

  private fun buildDefaultScanDirectories(): List<File> {
    val candidates = linkedSetOf<File>()
    @Suppress("DEPRECATION")
    runCatching {
      val extRoot = Environment.getExternalStorageDirectory()
      if (extRoot != null && extRoot.exists()) {
        listOf(
          Environment.DIRECTORY_MUSIC,
          Environment.DIRECTORY_DOWNLOADS,
          "Download",
          Environment.DIRECTORY_MOVIES,
          Environment.DIRECTORY_DCIM,
          Environment.DIRECTORY_PODCASTS,
          Environment.DIRECTORY_AUDIOBOOKS,
          "Recordings",
          "Audio",
          "Video",
          "Videos",
          MediaConverter.APP_FOLDER_NAME,
        ).forEach { folderName ->
          candidates.add(File(extRoot, folderName))
        }
      }
    }
    return candidates.filter { it.exists() && it.isDirectory && it.canRead() }
  }

  internal fun scanDirectoriesInternal(
    roots: List<File>,
    artCacheDir: File = File(context.cacheDir, "track_art").apply { mkdirs() },
    includeAudio: Boolean = true,
    includeVideo: Boolean = true,
    unknownArtistFallback: String = "Unknown Artist",
    unknownAlbumFallback: String = "Local Audio",
    unknownVideoFallback: String = "Local Video",
    out: MutableList<AudioTrack>,
    seenUris: HashSet<String>,
    seenFileSignatures: HashSet<String>,
    existingByUri: Map<String, CachedMediaRecord> = emptyMap(),
    upserts: MutableList<CachedMediaRecord>? = null,
    maxDepth: Int = 3,
    deferHeavyMetadataExtraction: Boolean = false,
    pendingHydration: MutableList<CachedMediaRecord>? = null,
  ): List<String> {
    val newlyDiscoveredPaths = mutableListOf<String>()
    val visitedDirs = HashSet<String>()

    fun walkDir(dir: File, depth: Int) {
      if (depth > maxDepth || !dir.exists() || !dir.isDirectory || !dir.canRead()) return
      val dirNameLower = dir.name.lowercase()
      if (dirNameLower.startsWith(".") || dirNameLower in SKIPPED_DIR_NAMES) return
      val canonical = runCatching { dir.canonicalPath }.getOrDefault(dir.absolutePath)
      if (!visitedDirs.add(canonical)) return

      val children = dir.listFiles() ?: return
      for (entry in children) {
        val entryName = entry.name
        if (entryName.startsWith(".")) continue
        val ext = extensionOf(entryName)
        if (ext.isEmpty()) {
          if (entry.isDirectory) {
            walkDir(entry, depth + 1)
          }
          continue
        }
        val isVideo = ext in VIDEO_EXT_SET
        val isAudio = !isVideo && ext in AUDIO_EXT_SET
        if (!isVideo && !isAudio) {
          if (entry.isDirectory) {
            walkDir(entry, depth + 1)
          }
          continue
        }
        if ((isVideo && !includeVideo) || (isAudio && !includeAudio)) continue
        if (!entry.isFile || !entry.canRead()) continue
        val sizeBytes = entry.length()
        if (sizeBytes <= 0L) continue
        val lower = entryName.lowercase()

        val fileUri = Uri.fromFile(entry)
        val uriStr = fileUri.toString()
        val type = if (isVideo) MediaType.VIDEO else MediaType.AUDIO
        val signature = mediaSignature(type, entry.name, sizeBytes)
        if (!seenFileSignatures.add(signature) || !seenUris.add(uriStr)) continue

        val lastMod = entry.lastModified()
        val cached = existingByUri[uriStr]
        if (isCacheEntryValid(cached, BUCKET_DIRECT_DIR, lastMod, sizeBytes)) {
          out.add(cached!!.track)
          continue
        }

        val syntheticId =
          (uriStr.hashCode().toLong() and 0x3FFFFFFFL) or
            if (isVideo) VIDEO_ID_NAMESPACE_BIT else DIRECT_AUDIO_NAMESPACE_BIT
        val cacheKey = "dir_${syntheticId}_$lastMod"
        val cachedArtFile = File(artCacheDir, "art_$cacheKey.jpg")
        val noArtMarker = File(artCacheDir, "art_$cacheKey.none")
        val hasDiskArt = cachedArtFile.exists() && cachedArtFile.length() > 0L
        val parsed =
          if (deferHeavyMetadataExtraction) {
            ParsedMediaMetadata(
              embeddedArtUri = if (hasDiskArt) Uri.fromFile(cachedArtFile) else null
            )
          } else {
            extractMediaMetadata(
              contentUri = fileUri,
              cacheKey = cacheKey,
              artCacheDir = artCacheDir,
              isVideo = isVideo,
              needsTitle = true,
              needsArtist = !isVideo,
              needsAlbum = !isVideo,
              needsDuration = true,
            )
          }
        val folderName = entry.parentFile?.name?.ifBlank { null } ?: if (isVideo) "Videos" else "Music"
        val track =
          AudioTrack(
            id = syntheticId,
            title = parsed.title ?: entry.nameWithoutExtension.ifBlank { entry.name },
            artist = parsed.artist ?: if (isVideo) unknownVideoFallback else unknownArtistFallback,
            album = parsed.album ?: if (isVideo) unknownVideoFallback else unknownAlbumFallback,
            albumId = 0L,
            durationMs = parsed.durationMs ?: 0L,
            contentUri = fileUri,
            albumArtUri = parsed.embeddedArtUri,
            dateAddedMs = lastMod,
            dateModifiedMs = lastMod,
            sizeBytes = sizeBytes,
            mimeType = guessMimeType(lower, isVideo),
            folderName = folderName,
            mediaType = type,
          )
        val record = CachedMediaRecord(track = track, sourceBucket = BUCKET_DIRECT_DIR)
        out.add(track)
        upserts?.add(record)
        newlyDiscoveredPaths.add(entry.absolutePath)
        if (deferHeavyMetadataExtraction && !hasDiskArt && !noArtMarker.exists()) {
          pendingHydration?.add(record)
        }
      }
    }

    roots.forEach { walkDir(it, 0) }
    return newlyDiscoveredPaths
  }

  suspend fun hydrateTracksBatch(
    records: List<CachedMediaRecord>,
    unknownArtistFallback: String = "Unknown Artist",
    unknownAlbumFallback: String = "Local Audio",
    unknownVideoFallback: String = "Local Video",
  ): List<CachedMediaRecord> =
    withContext(Dispatchers.IO) {
      if (records.isEmpty()) return@withContext emptyList()
      val artCacheDir = File(context.cacheDir, "track_art").apply { mkdirs() }
      val concurrencyGate = Semaphore(4)
      coroutineScope {
        records.map { record ->
          async {
            concurrencyGate.withPermit {
              val t = record.track
              val isVideo = t.isVideo
              val cacheKey =
                when (record.sourceBucket) {
                  BUCKET_VIDEO -> {
                    val rawId = t.id and VIDEO_ID_NAMESPACE_BIT.inv()
                    "vid_${rawId}_${t.dateModifiedMs}"
                  }
                  BUCKET_SAF -> "saf_${t.id}_${t.dateModifiedMs}"
                  BUCKET_APP_DIR -> "appdir_${t.id}_${t.dateModifiedMs}"
                  BUCKET_DIRECT_DIR -> "dir_${t.id}_${t.dateModifiedMs}"
                  else -> "aud_${t.id}_${t.dateModifiedMs}"
                }
              val needsArtist =
                t.artist.isBlank() ||
                  t.artist == unknownArtistFallback ||
                  t.artist == unknownVideoFallback
              val needsAlbum =
                t.album.isBlank() ||
                  t.album == unknownAlbumFallback ||
                  t.album == unknownVideoFallback
              val parsed =
                extractMediaMetadata(
                  contentUri = t.contentUri,
                  cacheKey = cacheKey,
                  artCacheDir = artCacheDir,
                  isVideo = isVideo,
                  needsTitle = t.title.isBlank(),
                  needsArtist = needsArtist,
                  needsAlbum = needsAlbum,
                  needsDuration = t.durationMs <= 0L,
                )
              val enriched =
                t.copy(
                  title = parsed.title ?: t.title,
                  artist = parsed.artist ?: t.artist,
                  album = parsed.album ?: t.album,
                  durationMs = parsed.durationMs?.takeIf { it > 0L } ?: t.durationMs,
                  albumArtUri = parsed.embeddedArtUri ?: t.albumArtUri,
                )
              if (enriched != t) record.copy(track = enriched) else null
            }
          }
        }.awaitAll().filterNotNull()
      }
    }

  internal fun isSupportedMediaFilename(fileName: String): Boolean {
    val ext = extensionOf(fileName)
    return ext in AUDIO_EXT_SET || ext in VIDEO_EXT_SET
  }

  private fun guessMimeType(lowerName: String, isVideo: Boolean): String =
    when {
      lowerName.endsWith(".mp3") -> "audio/mpeg"
      lowerName.endsWith(".m4a") -> "audio/mp4"
      lowerName.endsWith(".wav") -> "audio/wav"
      lowerName.endsWith(".flac") -> "audio/flac"
      lowerName.endsWith(".ogg") || lowerName.endsWith(".opus") -> "audio/ogg"
      lowerName.endsWith(".aac") -> "audio/aac"
      lowerName.endsWith(".mkv") -> "video/x-matroska"
      lowerName.endsWith(".webm") -> "video/webm"
      isVideo -> "video/mp4"
      else -> "audio/*"
    }

  private data class ParsedMediaMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
    val embeddedArtUri: Uri? = null,
  )

  private fun extractMediaMetadata(
    contentUri: Uri,
    cacheKey: String,
    artCacheDir: File,
    isVideo: Boolean,
    needsTitle: Boolean,
    needsArtist: Boolean,
    needsAlbum: Boolean,
    needsDuration: Boolean,
  ): ParsedMediaMetadata {
    val cachedArtFile = File(artCacheDir, "art_$cacheKey.jpg")
    val noArtMarker = File(artCacheDir, "art_$cacheKey.none")
    val hasArtCached = cachedArtFile.exists() && cachedArtFile.length() > 0L
    val artKnownMissing = noArtMarker.exists()

    val needArtCheck = !hasArtCached && !artKnownMissing
    if (!needArtCheck && !needsTitle && !needsArtist && !needsAlbum && !needsDuration) {
      return ParsedMediaMetadata(
        embeddedArtUri = if (hasArtCached) Uri.fromFile(cachedArtFile) else null
      )
    }

    var id3Title: String? = null
    var id3Artist: String? = null
    var id3Album: String? = null
    var id3Duration: Long? = null
    var artUri: Uri? = if (hasArtCached) Uri.fromFile(cachedArtFile) else null

    val retriever = MediaMetadataRetriever()
    try {
      retriever.setDataSource(context, contentUri)
      if (needsTitle) {
        id3Title =
          retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
      }
      if (needsArtist) {
        id3Artist =
          retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
      }
      if (needsAlbum) {
        id3Album =
          retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
      }
      if (needsDuration) {
        id3Duration =
          retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
            ?.takeIf { it > 0L }
      }
      if (needArtCheck) {
        val artBytes = retriever.embeddedPicture
        if (artBytes != null && artBytes.isNotEmpty()) {
          cachedArtFile.writeBytes(artBytes)
          if (cachedArtFile.exists() && cachedArtFile.length() > 0L) {
            artUri = Uri.fromFile(cachedArtFile)
          }
        } else if (isVideo) {
          val frameBitmap =
            runCatching {
              retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L)
            }.getOrNull()
          if (frameBitmap != null) {
            FileOutputStream(cachedArtFile).use { fos ->
              frameBitmap.compress(Bitmap.CompressFormat.JPEG, 82, fos)
            }
            frameBitmap.recycle()
            if (cachedArtFile.exists() && cachedArtFile.length() > 0L) {
              artUri = Uri.fromFile(cachedArtFile)
            }
          } else {
            runCatching { noArtMarker.createNewFile() }
          }
        } else {
          runCatching { noArtMarker.createNewFile() }
        }
      }
    } catch (_: Exception) {
      // Skip files whose container cannot be probed by MediaMetadataRetriever
    } finally {
      runCatching { retriever.release() }
    }

    return ParsedMediaMetadata(
      title = id3Title,
      artist = id3Artist,
      album = id3Album,
      durationMs = id3Duration,
      embeddedArtUri = artUri,
    )
  }

  suspend fun extractTrackFromExternalUri(
    uri: Uri,
    unknownArtistFallback: String = "External File",
    unknownAlbumFallback: String = "File Manager",
  ): AudioTrack =
    withContext(Dispatchers.IO) {
      var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "Media Track"
      var sizeBytes = 0L

      runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
          if (cursor.moveToFirst()) {
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIdx >= 0) {
              cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { displayName = it }
            }
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIdx >= 0) {
              sizeBytes = cursor.getLong(sizeIdx)
            }
          }
        }
      }

      val resolvedMime = context.contentResolver.getType(uri).orEmpty()
      val isVideo =
        resolvedMime.startsWith("video/") ||
          extensionOf(displayName) in VIDEO_EXT_SET

      var title = displayName.substringBeforeLast('.').ifBlank { "Media Track" }
      var artist = unknownArtistFallback
      var album = unknownAlbumFallback
      var durationMs = 0L
      var extractedArtUri: Uri? = null

      val retriever = MediaMetadataRetriever()
      try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
          ?.trim()
          ?.takeIf { it.isNotBlank() }
          ?.let { title = it }
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
          ?.trim()
          ?.takeIf { it.isNotBlank() }
          ?.let { artist = it }
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
          ?.trim()
          ?.takeIf { it.isNotBlank() }
          ?.let { album = it }
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
          ?.toLongOrNull()
          ?.let { durationMs = it.coerceAtLeast(0L) }
        val artBytes = retriever.embeddedPicture
        val artFile = File(context.cacheDir, "external_art_${uri.toString().hashCode()}.jpg")
        if (artBytes != null && artBytes.isNotEmpty()) {
          artFile.writeBytes(artBytes)
          if (artFile.exists() && artFile.length() > 0L) {
            extractedArtUri = Uri.fromFile(artFile)
          }
        } else if (isVideo) {
          val bmp =
            runCatching {
              retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L)
            }.getOrNull()
          if (bmp != null) {
            FileOutputStream(artFile).use { fos -> bmp.compress(Bitmap.CompressFormat.JPEG, 82, fos) }
            bmp.recycle()
            if (artFile.exists() && artFile.length() > 0L) {
              extractedArtUri = Uri.fromFile(artFile)
            }
          }
        }
      } catch (_: Exception) {
        // Fallback to display name metadata when MediaMetadataRetriever is unsupported for URI
      } finally {
        runCatching { retriever.release() }
      }

      val now = System.currentTimeMillis()
      val syntheticId = -(uri.toString().hashCode().toLong() and 0x7FFFFFFFL).coerceAtLeast(1L)
      AudioTrack(
        id = syntheticId,
        title = title,
        artist = artist,
        album = album,
        albumId = 0L,
        durationMs = durationMs,
        contentUri = uri,
        albumArtUri = extractedArtUri,
        dateAddedMs = now,
        dateModifiedMs = now,
        sizeBytes = sizeBytes,
        mimeType = resolvedMime.ifBlank { if (isVideo) "video/*" else "audio/*" },
        folderName = unknownAlbumFallback,
        mediaType = if (isVideo) MediaType.VIDEO else MediaType.AUDIO,
      )
    }

  fun loadBinderSafeArtworkBytes(track: AudioTrack, maxDimension: Int = 512): ByteArray? {
    val cachedBinderFile =
      File(
        File(context.cacheDir, "track_art").apply { mkdirs() },
        "binder_${track.contentUri.toString().hashCode()}.jpg",
      )
    if (cachedBinderFile.exists() && cachedBinderFile.length() in 1L..MAX_BINDER_ART_BYTES) {
      return runCatching { cachedBinderFile.readBytes() }.getOrNull()
    }

    val artUri = track.albumArtUri
    val bitmapFromArtUri =
      if (artUri != null) {
        loadBitmapForUri(context = context, uri = artUri, maxDimension = maxDimension)
      } else {
        null
      }

    val resolvedBitmap =
      bitmapFromArtUri
        ?: extractArtworkBitmapFromMedia(
          context = context,
          contentUri = track.contentUri,
          isVideo = track.isVideo,
          maxDimension = maxDimension,
        )
        ?: return null

    val bytes = compressBitmapToBinderSafeJpeg(resolvedBitmap, maxDimension = maxDimension)
    resolvedBitmap.recycle()
    if (bytes.isNotEmpty() && bytes.size <= MAX_BINDER_ART_BYTES) {
      runCatching { cachedBinderFile.writeBytes(bytes) }
    }
    return bytes.takeIf { it.isNotEmpty() }
  }

  companion object {
    const val BUCKET_AUDIO = "AUDIO"
    const val BUCKET_VIDEO = "VIDEO"
    const val BUCKET_SAF = "SAF"
    const val BUCKET_APP_DIR = "APP_DIR"
    const val BUCKET_DIRECT_DIR = "DIRECT_DIR"
    const val MAX_BINDER_ART_BYTES = 220 * 1024

    private const val VIDEO_ID_NAMESPACE_BIT = 0x4000_0000_0000_0000L
    private const val SAF_AUDIO_NAMESPACE_BIT = 0x2000_0000_0000_0000L
    private const val DIRECT_AUDIO_NAMESPACE_BIT = 0x1000_0000_0000_0000L

    private val AUDIO_EXT_SET =
      hashSetOf("mp3", "wav", "flac", "m4a", "aac", "ogg", "opus", "wma", "amr", "3gp")

    private val VIDEO_EXT_SET =
      hashSetOf("mp4", "mkv", "mov", "webm", "3gp", "avi", "m4v")

    private val SKIPPED_DIR_NAMES =
      hashSetOf("android", "thumbnails", "cache", "tmp", "temp", "lost.dir")

    internal fun extensionOf(fileName: String): String {
      val dot = fileName.lastIndexOf('.')
      if (dot < 0 || dot >= fileName.length - 1) return ""
      return fileName.substring(dot + 1).lowercase()
    }

    fun buildMediaSessionArtworkUri(track: AudioTrack): Uri =
      track.albumArtUri
        ?: Uri.Builder()
          .scheme("mplayerart")
          .authority("track")
          .appendQueryParameter("uri", track.contentUri.toString())
          .appendQueryParameter("video", track.isVideo.toString())
          .build()

    fun loadBitmapForUri(
      context: Context,
      uri: Uri,
      maxDimension: Int = 512,
    ): Bitmap? {
      if (uri.scheme == "mplayerart") {
        val rawMediaUri = uri.getQueryParameter("uri")?.let { Uri.parse(it) } ?: return null
        val isVideo = uri.getQueryParameter("video")?.toBooleanStrictOrNull() ?: false
        return extractArtworkBitmapFromMedia(context, rawMediaUri, isVideo, maxDimension)
      }

      if (uri.scheme == "file") {
        val path = uri.path ?: return null
        val file = File(path)
        if (!file.exists() || file.length() <= 0L) return null
        return decodeSampledBitmapFromBytes(file.readBytes(), maxDimension)
      }

      if (uri.scheme == "content") {
        // 1. Try direct ContentResolver stream (works for albumart & SAF docs)
        runCatching {
          context.contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readBytes()
            if (bytes.isNotEmpty()) {
              decodeSampledBitmapFromBytes(bytes, maxDimension)?.let { return it }
            }
          }
        }
        // 2. Try Android Q+ loadThumbnail (for video/audio MediaStore URIs)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          runCatching {
            val thumb =
              context.contentResolver.loadThumbnail(uri, Size(maxDimension, maxDimension), null)
            return scaleDownBitmap(thumb, maxDimension)
          }
        }
      }

      return null
    }

    fun extractArtworkBitmapFromMedia(
      context: Context,
      contentUri: Uri,
      isVideo: Boolean,
      maxDimension: Int = 512,
    ): Bitmap? {
      val retriever = MediaMetadataRetriever()
      return try {
        retriever.setDataSource(context, contentUri)
        val embedded = retriever.embeddedPicture
        if (embedded != null && embedded.isNotEmpty()) {
          decodeSampledBitmapFromBytes(embedded, maxDimension)
        } else if (isVideo) {
          val frame =
            runCatching {
              retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L)
            }.getOrNull()
          frame?.let { scaleDownBitmap(it, maxDimension) }
        } else {
          null
        }
      } catch (_: Exception) {
        null
      } finally {
        runCatching { retriever.release() }
      }
    }

    fun decodeSampledBitmapFromBytes(bytes: ByteArray, maxDimension: Int = 512): Bitmap? {
      if (bytes.isEmpty()) return null
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
      var sampleSize = 1
      val maxOrig = maxOf(bounds.outWidth, bounds.outHeight)
      while (maxOrig / sampleSize > maxDimension * 2) {
        sampleSize *= 2
      }
      val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null
      return scaleDownBitmap(decoded, maxDimension)
    }

    fun scaleDownBitmap(bitmap: Bitmap, maxDimension: Int = 512): Bitmap {
      val w = bitmap.width
      val h = bitmap.height
      if (w <= 0 || h <= 0 || (w <= maxDimension && h <= maxDimension)) return bitmap
      val ratio = minOf(maxDimension.toFloat() / w.toFloat(), maxDimension.toFloat() / h.toFloat())
      val targetW = (w * ratio).toInt().coerceAtLeast(1)
      val targetH = (h * ratio).toInt().coerceAtLeast(1)
      val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
      if (scaled !== bitmap) {
        bitmap.recycle()
      }
      return scaled
    }

    fun compressBitmapToBinderSafeJpeg(
      bitmap: Bitmap,
      maxDimension: Int = 512,
      initialQuality: Int = 85,
    ): ByteArray {
      val scaled = scaleDownBitmap(bitmap, maxDimension)
      var quality = initialQuality
      val baos = ByteArrayOutputStream()
      while (quality >= 45) {
        baos.reset()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        if (baos.size() <= MAX_BINDER_ART_BYTES) break
        quality -= 15
      }
      return baos.toByteArray()
    }
  }
}
