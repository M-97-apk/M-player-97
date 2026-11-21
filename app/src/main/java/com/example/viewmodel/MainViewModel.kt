package com.example.viewmodel

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MPlayerApplication
import com.example.data.analytics.PlaylistImportResult
import com.example.data.analytics.UsageImportResult
import com.example.data.media.MediaConverter
import com.example.data.prefs.UserPreferences
import com.example.model.AnalyticsTimeframe
import com.example.model.AppLanguage
import com.example.model.AppThemeStyle
import com.example.model.AudioTrack
import com.example.model.ConversionFormat
import com.example.model.ConversionProgressState
import com.example.model.LibraryMediaFilter
import com.example.model.LibrarySortOption
import com.example.model.ListeningTimeUnit
import com.example.model.MediaType
import com.example.model.PlaybackHistoryItem
import com.example.model.PlaylistSortOption
import com.example.model.PlaylistSummary
import com.example.model.PlaylistTrackEntry
import com.example.model.VideoPlaybackMode
import com.example.model.VisualizerMode
import com.example.model.WeeklySummaryReport
import com.example.playback.PlaybackUiState
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PendingDeleteConfirmation(
  val track: AudioTrack,
  val intentSender: IntentSender,
)

data class DeleteOutcome(
  val title: String,
  val success: Boolean,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private val app = application as MPlayerApplication
  private val repository = app.musicRepository
  private val prefsRepository = app.prefsRepository
  val playerController = app.playerController

  val userPreferences: StateFlow<UserPreferences> =
    prefsRepository.preferencesFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = UserPreferences(),
    )

  val playbackState: StateFlow<PlaybackUiState> = playerController.state
  val sleepTimerState: StateFlow<com.example.model.SleepTimerState> = playerController.sleepTimerState
  val panAngleDegrees: StateFlow<Float> = playerController.panAngleDegrees
  val audioAmplitude: StateFlow<Float> = playerController.audioAmplitude
  val bassEnergy: StateFlow<Float> = playerController.bassEnergy
  val waveformSamples: StateFlow<FloatArray> = playerController.waveformSamples
  val spectrumBins: StateFlow<FloatArray> = playerController.spectrumBins
  val eqEnabled: StateFlow<Boolean> = playerController.eqEnabled
  val eqPreset: StateFlow<com.example.model.EqualizerPreset?> = playerController.eqPreset
  val eqBandGainsDb: StateFlow<List<Float>> = playerController.eqBandGainsDb

  private val _rawLibraryTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedFolder = MutableStateFlow<String?>(null)
  val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

  private val _mediaFilter = MutableStateFlow(LibraryMediaFilter.ALL)
  val mediaFilter: StateFlow<LibraryMediaFilter> = _mediaFilter.asStateFlow()

  val availableFolders: StateFlow<List<String>> =
    combine(_rawLibraryTracks, _mediaFilter) { tracks, mediaFilter ->
      tracks
        .asSequence()
        .filter {
          when (mediaFilter) {
            LibraryMediaFilter.ALL -> true
            LibraryMediaFilter.AUDIO -> it.mediaType == MediaType.AUDIO
            LibraryMediaFilter.VIDEO -> it.mediaType == MediaType.VIDEO
          }
        }
        .map { it.folderName }
        .distinct()
        .sorted()
        .toList()
    }
      .flowOn(Dispatchers.Default)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val filteredLibraryTracks: StateFlow<List<AudioTrack>> =
    combine(
      _rawLibraryTracks,
      _searchQuery,
      _selectedFolder,
      _mediaFilter,
      userPreferences.map { it.librarySort },
    ) { tracks, query, folder, mFilter, sort ->
      filterAndSortLibraryTracks(
        tracks = tracks,
        query = query,
        folder = folder,
        mediaFilter = mFilter,
        sortOption = sort,
      )
    }
      .flowOn(Dispatchers.Default)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val playlists: StateFlow<List<PlaylistSummary>> =
    repository
      .observePlaylists()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val favoriteUris: StateFlow<Set<String>> =
    repository
      .observeFavoriteUris()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

  val historyItems: StateFlow<List<PlaybackHistoryItem>> =
    repository
      .observePlaybackHistory()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _conversionState = MutableStateFlow(ConversionProgressState())
  val conversionState: StateFlow<ConversionProgressState> = _conversionState.asStateFlow()

  private val _deleteRequests = MutableSharedFlow<PendingDeleteConfirmation>(extraBufferCapacity = 4)
  val deleteRequests: SharedFlow<PendingDeleteConfirmation> = _deleteRequests.asSharedFlow()

  private val _deleteOutcomes = MutableSharedFlow<DeleteOutcome>(extraBufferCapacity = 8)
  val deleteOutcomes: SharedFlow<DeleteOutcome> = _deleteOutcomes.asSharedFlow()

  private val _selectedTimeframe = MutableStateFlow(AnalyticsTimeframe.DAILY)
  val selectedTimeframe: StateFlow<AnalyticsTimeframe> = _selectedTimeframe.asStateFlow()

  val weeklySummary: StateFlow<WeeklySummaryReport> =
    repository
      .observeUsageSummary(_selectedTimeframe)
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        WeeklySummaryReport(emptyList(), 0, 0L, 0, AnalyticsTimeframe.DAILY, 0L, 0L),
      )

  private data class PendingSyncRequest(
    val unknownArtist: String,
    val unknownAlbum: String,
    val unknownVideo: String,
    val includeAudio: Boolean,
    val includeVideo: Boolean,
    val showProgress: Boolean,
  )

  private var lastUnknownArtist: String = "Unknown Artist"
  private var lastUnknownAlbum: String = "Local Audio"
  private var lastUnknownVideo: String = "Local Video"
  private var lastIncludeAudio: Boolean = false
  private var lastIncludeVideo: Boolean = false
  private var pendingSyncRequest: PendingSyncRequest? = null
  @Volatile private var isSyncWorkerActive: Boolean = false
  @Volatile private var userInteractedWithFilters: Boolean = false

  init {
    viewModelScope.launch {
      val initialPrefs = prefsRepository.preferencesFlow.first()
      if (!userInteractedWithFilters) {
        _mediaFilter.value = initialPrefs.libraryMediaFilter
        _selectedFolder.value = initialPrefs.librarySelectedFolder
      }
      repository.ensureFavoritesPlaylist()
    }
    viewModelScope.launch {
      repository.observeCachedMedia().collect { cached ->
        _rawLibraryTracks.value = cached
      }
    }
    @OptIn(FlowPreview::class)
    viewModelScope.launch {
      repository
        .observeDeviceMediaChanges()
        .debounce(120L)
        .collect {
          if (lastIncludeAudio || lastIncludeVideo) {
            runIncrementalSync(
              unknownArtist = lastUnknownArtist,
              unknownAlbum = lastUnknownAlbum,
              unknownVideo = lastUnknownVideo,
              includeAudio = lastIncludeAudio,
              includeVideo = lastIncludeVideo,
              showProgress = false,
            )
          }
        }
    }
  }

  fun scanLibrary(
    unknownArtist: String,
    unknownAlbum: String,
    unknownVideo: String = unknownAlbum,
    includeAudio: Boolean = true,
    includeVideo: Boolean = true,
    showProgress: Boolean = false,
  ) {
    lastUnknownArtist = unknownArtist
    lastUnknownAlbum = unknownAlbum
    lastUnknownVideo = unknownVideo
    lastIncludeAudio = includeAudio
    lastIncludeVideo = includeVideo
    runIncrementalSync(
      unknownArtist = unknownArtist,
      unknownAlbum = unknownAlbum,
      unknownVideo = unknownVideo,
      includeAudio = includeAudio,
      includeVideo = includeVideo,
      showProgress = showProgress || _rawLibraryTracks.value.isEmpty(),
    )
  }

  private fun runIncrementalSync(
    unknownArtist: String,
    unknownAlbum: String,
    unknownVideo: String,
    includeAudio: Boolean,
    includeVideo: Boolean,
    showProgress: Boolean,
  ) {
    if (isSyncWorkerActive) {
      val existing = pendingSyncRequest
      val nextShow = (existing?.showProgress == true) || showProgress
      if (showProgress) _isScanning.value = true
      pendingSyncRequest =
        PendingSyncRequest(
          unknownArtist = unknownArtist,
          unknownAlbum = unknownAlbum,
          unknownVideo = unknownVideo,
          includeAudio = includeAudio,
          includeVideo = includeVideo,
          showProgress = nextShow,
        )
      return
    }
    isSyncWorkerActive = true
    viewModelScope.launch {
      try {
        var nextReq: PendingSyncRequest? =
          PendingSyncRequest(
            unknownArtist = unknownArtist,
            unknownAlbum = unknownAlbum,
            unknownVideo = unknownVideo,
            includeAudio = includeAudio,
            includeVideo = includeVideo,
            showProgress = showProgress,
          )
        while (nextReq != null) {
          val req = nextReq
          pendingSyncRequest = null
          _isScanning.value = req.showProgress
          runCatching {
            val treeUri = userPreferences.value.converterOutputTreeUri
            val synced =
              repository.syncLibraryIncremental(
                includeAudio = req.includeAudio,
                includeVideo = req.includeVideo,
                unknownArtist = req.unknownArtist,
                unknownAlbum = req.unknownAlbum,
                unknownVideo = req.unknownVideo,
                converterTreeUriString = treeUri,
              )
            if (_rawLibraryTracks.value.isEmpty() && synced.isNotEmpty()) {
              _rawLibraryTracks.value = synced
            }
          }
          nextReq = pendingSyncRequest
        }
      } finally {
        isSyncWorkerActive = false
        _isScanning.value = false
      }
    }
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setSelectedFolder(folder: String?) {
    userInteractedWithFilters = true
    _selectedFolder.value = folder
    viewModelScope.launch { prefsRepository.setLibrarySelectedFolder(folder) }
  }

  fun setMediaFilter(filter: LibraryMediaFilter) {
    userInteractedWithFilters = true
    _mediaFilter.value = filter
    _selectedFolder.value = null
    viewModelScope.launch { prefsRepository.setLibraryMediaFilter(filter) }
  }

  fun setLibrarySort(sortOption: LibrarySortOption) {
    viewModelScope.launch { prefsRepository.setLibrarySort(sortOption) }
  }

  fun setThemeStyle(style: AppThemeStyle) {
    viewModelScope.launch { prefsRepository.setThemeStyle(style) }
  }

  fun setDarkMode(isDark: Boolean) {
    viewModelScope.launch { prefsRepository.setDarkMode(isDark) }
  }

  fun setLanguage(language: AppLanguage) {
    viewModelScope.launch { prefsRepository.setLanguage(language) }
  }

  fun setVisualizerMode(mode: VisualizerMode) {
    viewModelScope.launch { prefsRepository.setVisualizerMode(mode) }
  }

  fun setAnalyticsTimeframe(timeframe: AnalyticsTimeframe) {
    _selectedTimeframe.value = timeframe
  }

  fun setListeningTimeUnit(unit: ListeningTimeUnit) {
    viewModelScope.launch { prefsRepository.setListeningTimeUnit(unit) }
  }

  fun setDefaultVideoPlaybackMode(mode: VideoPlaybackMode) {
    viewModelScope.launch { prefsRepository.setDefaultVideoPlaybackMode(mode) }
  }

  fun setAppGlassOpacity(opacity: Float) {
    viewModelScope.launch { prefsRepository.setAppGlassOpacity(opacity) }
  }

  fun setVideoSeekStepSeconds(seconds: Int) {
    viewModelScope.launch { prefsRepository.setVideoSeekStepSeconds(seconds) }
  }

  fun toggleFavorite(track: AudioTrack, defaultFavoritesName: String = "Liked") {
    viewModelScope.launch {
      val added = repository.toggleFavorite(track, defaultFavoritesName)
      if (added) {
        syncPlaylistsBackupAddOnlyIfEnabled()
      }
    }
  }

  fun playVideoWithMode(track: AudioTrack, audioOnly: Boolean) {
    playerController.playQueue(
      tracks = listOf(track),
      startIndex = 0,
      startPositionMs = 0L,
      forceAudioOnly = audioOnly,
      autoResumeIfLong = true,
    )
  }

  fun playFromHistory(item: PlaybackHistoryItem, resume: Boolean) {
    playerController.playQueue(
      tracks = listOf(item.track),
      startIndex = 0,
      startPositionMs = if (resume) item.lastPositionMs else 0L,
      autoResumeIfLong = false,
      forceRestartFromZero = !resume,
    )
  }

  fun deleteHistoryItem(eventId: Long) {
    viewModelScope.launch { repository.deleteHistoryItem(eventId) }
  }

  fun deleteMediaItem(track: AudioTrack) {
    viewModelScope.launch(Dispatchers.IO) {
      val uri = track.contentUri
      val resolver = app.contentResolver

      if (uri.scheme == "file") {
        val file = uri.path?.let { File(it) }
        val deleted = file == null || !file.exists() || file.delete()
        if (deleted) {
          finalizeDeletedTrack(track)
          _deleteOutcomes.tryEmit(DeleteOutcome(track.title, true))
        } else {
          _deleteOutcomes.tryEmit(DeleteOutcome(track.title, false))
        }
        return@launch
      }

      if (DocumentsContract.isDocumentUri(app, uri)) {
        val deleted = runCatching { DocumentsContract.deleteDocument(resolver, uri) }.getOrDefault(false)
        if (deleted) {
          finalizeDeletedTrack(track)
          _deleteOutcomes.tryEmit(DeleteOutcome(track.title, true))
          return@launch
        }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && uri.authority == MediaStore.AUTHORITY) {
        val sender =
          runCatching {
            MediaStore.createDeleteRequest(resolver, listOf(uri)).intentSender
          }.getOrNull()
        if (sender != null) {
          _deleteRequests.tryEmit(PendingDeleteConfirmation(track, sender))
          return@launch
        }
      }

      try {
        val rows = resolver.delete(uri, null, null)
        if (rows > 0) {
          finalizeDeletedTrack(track)
          _deleteOutcomes.tryEmit(DeleteOutcome(track.title, true))
          return@launch
        }
      } catch (sec: SecurityException) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && sec is RecoverableSecurityException) {
          _deleteRequests.tryEmit(
            PendingDeleteConfirmation(track, sec.userAction.actionIntent.intentSender)
          )
          return@launch
        }
      } catch (_: Exception) {}

      val physicalDeleted =
        runCatching {
          @Suppress("DEPRECATION")
          val dataCol = MediaStore.MediaColumns.DATA
          var path: String? = null
          resolver.query(uri, arrayOf(dataCol), null, null, null)?.use { c ->
            if (c.moveToFirst()) path = c.getString(0)
          }
          val file = path?.takeIf { it.isNotBlank() }?.let { File(it) }
          if (file != null && file.exists()) {
            val ok = file.delete()
            if (ok) runCatching { resolver.delete(uri, null, null) }
            ok
          } else {
            false
          }
        }.getOrDefault(false)

      if (physicalDeleted) {
        finalizeDeletedTrack(track)
        _deleteOutcomes.tryEmit(DeleteOutcome(track.title, true))
      } else {
        _deleteOutcomes.tryEmit(DeleteOutcome(track.title, false))
      }
    }
  }

  fun onSystemDeleteConfirmed(track: AudioTrack) {
    viewModelScope.launch(Dispatchers.IO) {
      finalizeDeletedTrack(track)
      _deleteOutcomes.tryEmit(DeleteOutcome(track.title, true))
    }
  }

  private suspend fun finalizeDeletedTrack(track: AudioTrack) {
    repository.deleteCachedMedia(track.contentUri, track.id)
    if (playerController.state.value.currentTrack?.id == track.id) {
      withContext(Dispatchers.Main) {
        playerController.stopPlayback()
      }
    }
    if (lastIncludeAudio || lastIncludeVideo) {
      runIncrementalSync(
        unknownArtist = lastUnknownArtist,
        unknownAlbum = lastUnknownAlbum,
        unknownVideo = lastUnknownVideo,
        includeAudio = lastIncludeAudio,
        includeVideo = lastIncludeVideo,
        showProgress = false,
      )
    }
  }

  fun clearHistory() {
    viewModelScope.launch { repository.clearHistory() }
  }

  fun onConverterOutputTreeSelected(
    uri: Uri,
    onResult: (Boolean, String) -> Unit = { _, _ -> },
  ) {
    viewModelScope.launch {
      val uriStr = uri.toString()
      prefsRepository.setConverterOutputTreeUri(uriStr)
      val dirInfo = MediaConverter.ensureSafAppDirectory(app, uri)
      onResult(dirInfo != null, dirInfo?.second ?: MediaConverter.APP_FOLDER_NAME)
    }
  }

  fun convertMediaItem(
    track: AudioTrack,
    format: ConversionFormat,
    unknownArtist: String,
    unknownAlbum: String,
    unknownVideo: String,
  ) {
    if (_conversionState.value.isRunning) return
    viewModelScope.launch {
      _conversionState.value =
        ConversionProgressState(
          isRunning = true,
          sourceTitle = track.title,
          format = format,
          progress = 0.02f,
        )
      val safUri = userPreferences.value.converterOutputTreeUri
      val res =
        MediaConverter.convertMedia(
          context = app,
          source = track,
          format = format,
          safTreeUriString = safUri,
          onProgress = { pct ->
            _conversionState.value = _conversionState.value.copy(progress = pct)
          },
        )
      if (res.success) {
        _conversionState.value =
          ConversionProgressState(
            isRunning = false,
            sourceTitle = track.title,
            format = format,
            progress = 1f,
            outputDisplayPath = res.outputDisplayPath,
          )
        scanLibrary(unknownArtist, unknownAlbum, unknownVideo)
      } else {
        _conversionState.value =
          ConversionProgressState(
            isRunning = false,
            sourceTitle = track.title,
            format = format,
            progress = 0f,
            errorMessage = res.errorMessage ?: "Conversion failed",
          )
      }
    }
  }

  fun dismissConversionState() {
    if (!_conversionState.value.isRunning) {
      _conversionState.value = ConversionProgressState()
    }
  }

  fun onExternalUsageFileCreated(uri: Uri, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
    viewModelScope.launch {
      val uriStr = uri.toString()
      prefsRepository.setUsageFileSyncConfig(enabled = true, externalUri = uriStr)
      val res = repository.syncUsageStatsJson(externalDocumentUri = uriStr)
      if (res.success) {
        prefsRepository.setUsageLastSyncedIso(res.syncedAtIso)
      }
      onComplete(res.success && res.externalSynced, res.internalFilePath)
    }
  }

  fun importUsageStats(uri: Uri, onComplete: (UsageImportResult) -> Unit = {}) {
    viewModelScope.launch {
      val result = repository.importUsageStatsFromUri(uri)
      if (result.success) {
        val prefs = prefsRepository.preferencesFlow.first()
        if (prefs.usageFileSyncEnabled) {
          val syncRes = repository.syncUsageStatsJson(externalDocumentUri = prefs.usageFileExternalUri)
          if (syncRes.success) {
            prefsRepository.setUsageLastSyncedIso(syncRes.syncedAtIso)
          }
        }
      }
      onComplete(result)
    }
  }

  fun onPlaylistBackupFileCreated(
    uri: Uri,
    onComplete: (Boolean, String) -> Unit = { _, _ -> },
  ) {
    viewModelScope.launch {
      val uriStr = uri.toString()
      prefsRepository.setPlaylistBackupConfig(enabled = true, externalUri = uriStr)
      val res = repository.syncPlaylistsBackupAddOnly(externalDocumentUri = uriStr)
      if (res.success) {
        prefsRepository.setPlaylistLastSyncedIso(res.syncedAtIso)
      }
      onComplete(res.success && res.externalSynced, res.internalFilePath)
    }
  }

  fun importPlaylists(uri: Uri, onComplete: (PlaylistImportResult) -> Unit = {}) {
    viewModelScope.launch {
      val result = repository.importPlaylistsFromUri(uri)
      if (result.success) {
        syncPlaylistsBackupAddOnlyIfEnabled()
      }
      onComplete(result)
    }
  }

  private suspend fun syncPlaylistsBackupAddOnlyIfEnabled() {
    val prefs = prefsRepository.preferencesFlow.first()
    if (prefs.playlistBackupSyncEnabled) {
      val syncRes = repository.syncPlaylistsBackupAddOnly(externalDocumentUri = prefs.playlistBackupExternalUri)
      if (syncRes.success) {
        prefsRepository.setPlaylistLastSyncedIso(syncRes.syncedAtIso)
      }
    }
  }

  fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) {
    viewModelScope.launch {
      val id = repository.createPlaylist(name)
      onCreated(id)
      runCatching { syncPlaylistsBackupAddOnlyIfEnabled() }
    }
  }

  fun deletePlaylist(playlistId: Long) {
    // Strictly add-only backup rule: deleting a playlist in the app never deletes it from the JSON archive
    viewModelScope.launch { repository.deletePlaylist(playlistId) }
  }

  fun renamePlaylist(playlistId: Long, newName: String) {
    viewModelScope.launch { repository.renamePlaylist(playlistId, newName) }
  }

  fun updatePlaylistCover(playlistId: Long, pickerUri: Uri) {
    viewModelScope.launch { repository.savePlaylistCoverFromUri(playlistId, pickerUri) }
  }

  fun clearPlaylistCover(playlistId: Long) {
    viewModelScope.launch { repository.clearPlaylistCover(playlistId) }
  }

  fun updatePlaylistSort(playlistId: Long, sortOption: PlaylistSortOption) {
    viewModelScope.launch { repository.updatePlaylistSort(playlistId, sortOption) }
  }

  fun addTracksToPlaylist(playlistId: Long, tracks: List<AudioTrack>) {
    viewModelScope.launch {
      val inserted = repository.addTracksToPlaylist(playlistId, tracks)
      if (inserted > 0) {
        syncPlaylistsBackupAddOnlyIfEnabled()
      }
    }
  }

  fun removeTrackFromPlaylist(entryId: Long) {
    // Strictly add-only backup rule: removing tracks does not purge archived tracks
    viewModelScope.launch { repository.removeTrackFromPlaylist(entryId) }
  }

  fun persistManualPlaylistOrder(orderedEntries: List<PlaylistTrackEntry>) {
    viewModelScope.launch {
      repository.persistManualOrder(orderedEntries.map { it.entryId })
    }
  }

  fun observePlaylistDetail(playlistId: Long): Flow<Pair<PlaylistSummary?, List<PlaylistTrackEntry>>> =
    combine(
      playlists.map { list -> list.firstOrNull { it.id == playlistId } },
      repository.observePlaylistTracks(playlistId),
    ) { summary, entries ->
      val sort = summary?.sortOption ?: PlaylistSortOption.MANUAL
      summary to entries.sortPlaylistEntries(sort)
    }

  fun playLibraryTrack(track: AudioTrack) {
    val queue = filteredLibraryTracks.value
    val index = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
    playerController.playQueue(
      tracks = queue.ifEmpty { listOf(track) },
      startIndex = index,
      autoResumeIfLong = true,
    )
  }

  fun playAllLibrary(shuffled: Boolean) {
    val list = filteredLibraryTracks.value
    if (list.isEmpty()) return
    val queue = if (shuffled) list.shuffled() else list
    playerController.playQueue(queue, 0, autoResumeIfLong = false)
  }

  fun playPlaylistEntries(entries: List<PlaylistTrackEntry>, startIndex: Int = 0) {
    val tracks = entries.map { it.track }
    if (tracks.isNotEmpty()) {
      playerController.playQueue(tracks, startIndex, autoResumeIfLong = true)
    }
  }

  fun playWeeklyTop7Mix() {
    val top = weeklySummary.value.topTracks
    if (top.isEmpty()) return
    val tracks =
      top.map { item ->
        AudioTrack(
          id = item.trackId,
          title = item.title,
          artist = item.artist,
          album = item.album,
          albumId = 0L,
          durationMs = item.durationMs,
          contentUri = Uri.parse(item.contentUri),
          albumArtUri = item.albumArtUri?.let { Uri.parse(it) },
          dateAddedMs = item.lastPlayedAtMs,
          dateModifiedMs = item.lastPlayedAtMs,
          sizeBytes = 0L,
          mimeType = "audio/*",
          folderName = "Top 7",
        )
      }
    playerController.playQueue(tracks, 0)
  }
}

internal fun filterAndSortLibraryTracks(
  tracks: List<AudioTrack>,
  query: String,
  folder: String?,
  mediaFilter: LibraryMediaFilter,
  sortOption: LibrarySortOption,
): List<AudioTrack> {
  val trimmedQuery = query.trim()
  val hasQuery = trimmedQuery.isNotEmpty()
  val filtered =
    if (!hasQuery && folder == null && mediaFilter == LibraryMediaFilter.ALL) {
      tracks
    } else {
      tracks.filter { track ->
        val typeOk =
          when (mediaFilter) {
            LibraryMediaFilter.ALL -> true
            LibraryMediaFilter.AUDIO -> track.mediaType == MediaType.AUDIO
            LibraryMediaFilter.VIDEO -> track.mediaType == MediaType.VIDEO
          }
        if (!typeOk) return@filter false
        if (folder != null && track.folderName != folder) return@filter false
        if (!hasQuery) return@filter true
        track.title.contains(trimmedQuery, ignoreCase = true) ||
          track.artist.contains(trimmedQuery, ignoreCase = true) ||
          track.album.contains(trimmedQuery, ignoreCase = true)
      }
    }
  return filtered.sortLibraryTracks(sortOption)
}

private fun List<AudioTrack>.sortLibraryTracks(option: LibrarySortOption): List<AudioTrack> =
  when (option) {
    LibrarySortOption.TITLE_ASC -> sortedBy { it.title.lowercase() }
    LibrarySortOption.TITLE_DESC -> sortedByDescending { it.title.lowercase() }
    LibrarySortOption.DATE_MODIFIED_DESC -> sortedByDescending { it.dateModifiedMs }
    LibrarySortOption.DATE_MODIFIED_ASC -> sortedBy { it.dateModifiedMs }
    LibrarySortOption.ARTIST_ASC -> sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
    LibrarySortOption.DURATION_DESC -> sortedByDescending { it.durationMs }
  }

private fun List<PlaylistTrackEntry>.sortPlaylistEntries(
  option: PlaylistSortOption
): List<PlaylistTrackEntry> =
  when (option) {
    PlaylistSortOption.MANUAL -> sortedWith(compareBy({ it.manualOrder }, { it.entryId }))
    PlaylistSortOption.ALPHABETICAL_ASC -> sortedBy { it.track.title.lowercase() }
    PlaylistSortOption.ALPHABETICAL_DESC -> sortedByDescending { it.track.title.lowercase() }
    PlaylistSortOption.DATE_ADDED_DESC -> sortedByDescending { it.addedToPlaylistAtMs }
    PlaylistSortOption.DATE_ADDED_ASC -> sortedBy { it.addedToPlaylistAtMs }
    PlaylistSortOption.DATE_MODIFIED_DESC -> sortedByDescending { it.track.dateModifiedMs }
  }
