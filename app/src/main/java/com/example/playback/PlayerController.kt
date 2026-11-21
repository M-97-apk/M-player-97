package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.TextureView
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.data.media.AudioScanner
import com.example.data.prefs.UserPreferencesRepository
import com.example.data.repository.MusicRepository
import com.example.model.Audio8DSpeed
import com.example.model.AudioOutputRoute
import com.example.model.AudioTrack
import com.example.model.EqualizerPreset
import com.example.model.MediaType
import com.example.model.RepeatMode
import com.example.model.SleepTimerMode
import com.example.model.SleepTimerState
import com.example.model.VideoPlaybackMode
import com.example.overlay.FloatingOverlayService
import com.google.common.util.concurrent.ListenableFuture
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PlaybackUiState(
  val currentTrack: AudioTrack? = null,
  val queue: List<AudioTrack> = emptyList(),
  val currentIndex: Int = -1,
  val isPlaying: Boolean = false,
  val positionMs: Long = 0L,
  val durationMs: Long = 0L,
  val repeatMode: RepeatMode = RepeatMode.OFF,
  val shuffleEnabled: Boolean = false,
  val is8DEnabled: Boolean = false,
  val audio8DSpeed: Audio8DSpeed = Audio8DSpeed.NORMAL,
  val audio8DIntensity: Int = 97,
  val defaultVideoPlaybackMode: VideoPlaybackMode = VideoPlaybackMode.VIDEO_AND_AUDIO,
  val isVideoAudioOnly: Boolean = false,
  val videoAspectRatio: Float = 16f / 9f,
  val volumeBoostPercent: Int = 100,
  val amplifierEnabled: Boolean = false,
  val amplifierLevelPercent: Int = 50,
  val audioOutputRoute: AudioOutputRoute = AudioOutputRoute(),
)

class PlayerController(
  private val context: Context,
  private val repository: MusicRepository,
  private val prefsRepository: UserPreferencesRepository,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private val _state = MutableStateFlow(PlaybackUiState())
  val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

  private val _sleepTimerState = MutableStateFlow(SleepTimerState())
  val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState.asStateFlow()

  val panAngleDegrees: StateFlow<Float> = Audio8DEffectBus.panAngleDegrees
  val audioAmplitude: StateFlow<Float> = AudioVisualizerBus.amplitude
  val bassEnergy: StateFlow<Float> = AudioVisualizerBus.bassEnergy
  val waveformSamples: StateFlow<FloatArray> = AudioVisualizerBus.waveformSamples
  val spectrumBins: StateFlow<FloatArray> = AudioVisualizerBus.spectrumBins

  val eqEnabled: StateFlow<Boolean> = AudioEqualizerBus.isEnabledFlow
  val eqPreset: StateFlow<EqualizerPreset?> = AudioEqualizerBus.presetFlow
  val eqBandGainsDb: StateFlow<List<Float>> = AudioEqualizerBus.bandGainsDbFlow

  private var controllerFuture: ListenableFuture<MediaController>? = null
  private var mediaController: MediaController? = null
  private var progressJob: Job? = null
  private var sleepTimerJob: Job? = null
  private var sleepTargetEndElapsedMs: Long = 0L
  private var sleepEndOfTrackMediaId: String? = null

  private class PlaySessionHandle(
    val sessionUri: String,
    val track: AudioTrack,
    val initialPositionMs: Long,
    val resetToZero: Boolean,
    @Volatile var eventId: Long? = null,
  )

  private var activeQueue: List<AudioTrack> = emptyList()
  private val trackingMutex = Mutex()
  @Volatile private var currentSessionHandle: PlaySessionHandle? = null
  private var pendingSessionTrack: AudioTrack? = null
  private var pendingSessionInitialPosMs: Long = 0L
  private var pendingSessionResetToZero: Boolean = false
  @Volatile private var activePlayLogId: Long? = null
  private var lastActivePlayingTickElapsedMs: Long = 0L
  private var pendingListenedMs: Long = 0L
  @Volatile private var lastKnownPositionMs: Long = 0L
  private var lastUsageFileSyncElapsedMs: Long = 0L
  @Volatile private var usageFileSyncEnabled: Boolean = false
  @Volatile private var usageFileExternalUri: String? = null
  private var lastSeekTargetMs: Long = 0L
  private var lastSeekTimestampMs: Long = 0L

  private var attachedTextureViewRef: WeakReference<TextureView>? = null
  private var boundVideoSurfaceRef: WeakReference<TextureView>? = null
  private var perItemAudioOnlyOverride: Boolean? = null

  private var lastHardwareWriteElapsedMs: Long = 0L
  private var lastHardwareStepWritten: Int = -1
  private var lastObservedHardwareStep: Int = -1
  private val hardwareVolumeMonitor: HardwareVolumeMonitor =
    HardwareVolumeMonitor(context) { curStep, maxStep ->
      val nowElapsed = SystemClock.elapsedRealtime()
      if (
        (nowElapsed - lastHardwareWriteElapsedMs < 800L && curStep == lastHardwareStepWritten) ||
          curStep == lastObservedHardwareStep
      ) {
        return@HardwareVolumeMonitor
      }
      if (nowElapsed - lastHardwareWriteElapsedMs < 800L && _state.value.volumeBoostPercent > 100) {
        lastObservedHardwareStep = curStep
        return@HardwareVolumeMonitor
      }
      lastObservedHardwareStep = curStep
      val hwPct = HardwareVolumeMath.hardwareStepToPercent(curStep, maxStep)
      VolumeBoostBus.setBoostPercent(hwPct, fineTrim = 1f)
      _state.update { it.copy(volumeBoostPercent = hwPct) }
      scope.launch { prefsRepository.setVolumeBoostPercent(hwPct) }
    }

  init {
    hardwareVolumeMonitor.start()
    scope.launch {
      val initialPrefs = prefsRepository.preferencesFlow.first()
      Audio8DEffectBus.setEnabled(initialPrefs.is8DEnabled)
      Audio8DEffectBus.setSpeed(initialPrefs.audio8DSpeed)
      Audio8DEffectBus.setIntensityPercent(initialPrefs.audio8DIntensity)
      AudioEqualizerBus.setEnabled(initialPrefs.isEqEnabled)
      AudioEqualizerBus.setCustomGains(initialPrefs.eqBandGainsDb)
      AudioAmplifierBus.setEnabled(initialPrefs.amplifierEnabled)
      AudioAmplifierBus.setLevelPercent(initialPrefs.amplifierLevelPercent)

      val (curStep, maxStep) = hardwareVolumeMonitor.queryCurrentStepAndMax()
      lastObservedHardwareStep = curStep
      val hwPct = HardwareVolumeMath.hardwareStepToPercent(curStep, maxStep)
      val resolvedBoost =
        when {
          initialPrefs.volumeBoostPercent > 100 -> initialPrefs.volumeBoostPercent
          maxStep > 0 -> hwPct
          else -> 100
        }
      VolumeBoostBus.setBoostPercent(resolvedBoost, fineTrim = 1f)

      val initialAudioOnly = initialPrefs.defaultVideoPlaybackMode == VideoPlaybackMode.AUDIO_ONLY
      _state.update {
        it.copy(
          is8DEnabled = initialPrefs.is8DEnabled,
          audio8DSpeed = initialPrefs.audio8DSpeed,
          audio8DIntensity = initialPrefs.audio8DIntensity,
          repeatMode = initialPrefs.repeatMode,
          shuffleEnabled = initialPrefs.shuffleEnabled,
          defaultVideoPlaybackMode = initialPrefs.defaultVideoPlaybackMode,
          isVideoAudioOnly = perItemAudioOnlyOverride ?: initialAudioOnly,
          volumeBoostPercent = resolvedBoost,
          amplifierEnabled = initialPrefs.amplifierEnabled,
          amplifierLevelPercent = initialPrefs.amplifierLevelPercent,
        )
      }
      connectController()
    }
    scope.launch {
      Audio8DEffectBus.isEnabledFlow.collect { enabled ->
        if (_state.value.is8DEnabled != enabled) {
          _state.update { it.copy(is8DEnabled = enabled) }
          prefsRepository.set8DEnabled(enabled)
        }
      }
    }
    scope.launch {
      Audio8DEffectBus.speedFlow.collect { spd ->
        if (_state.value.audio8DSpeed != spd) {
          _state.update { it.copy(audio8DSpeed = spd) }
        }
      }
    }
    scope.launch {
      Audio8DEffectBus.intensityPercentFlow.collect { intensity ->
        if (_state.value.audio8DIntensity != intensity) {
          _state.update { it.copy(audio8DIntensity = intensity) }
        }
      }
    }
    refreshAudioOutputRoute()
    scope.launch {
      VolumeBoostBus.boostPercentFlow.collect { boostPct ->
        if (_state.value.volumeBoostPercent != boostPct) {
          _state.update { it.copy(volumeBoostPercent = boostPct) }
        }
      }
    }
    scope.launch {
      AudioAmplifierBus.isEnabledFlow.collect { ampOn ->
        if (_state.value.amplifierEnabled != ampOn) {
          _state.update { it.copy(amplifierEnabled = ampOn) }
        }
      }
    }
    scope.launch {
      AudioAmplifierBus.levelPercentFlow.collect { ampLvl ->
        if (_state.value.amplifierLevelPercent != ampLvl) {
          _state.update { it.copy(amplifierLevelPercent = ampLvl) }
        }
      }
    }
    scope.launch {
      AudioOutputRouteBus.routeFlow.collect { route ->
        if (_state.value.audioOutputRoute != route) {
          _state.update { it.copy(audioOutputRoute = route) }
        }
      }
    }
    scope.launch {
      prefsRepository.preferencesFlow.collect { prefs ->
        usageFileSyncEnabled = prefs.usageFileSyncEnabled
        usageFileExternalUri = prefs.usageFileExternalUri
        val prevDefault = _state.value.defaultVideoPlaybackMode
        if (prevDefault != prefs.defaultVideoPlaybackMode) {
          val resolvedAudioOnly =
            perItemAudioOnlyOverride ?: (prefs.defaultVideoPlaybackMode == VideoPlaybackMode.AUDIO_ONLY)
          _state.update {
            it.copy(
              defaultVideoPlaybackMode = prefs.defaultVideoPlaybackMode,
              isVideoAudioOnly = resolvedAudioOnly,
            )
          }
          mediaController?.let { applyVideoTrackSelection(it, resolvedAudioOnly) }
        }
      }
    }
  }

  private fun connectController() {
    if (controllerFuture != null) return
    val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    val future = MediaController.Builder(context, token).buildAsync()
    controllerFuture = future
    future.addListener(
      {
        runCatching {
          val controller = future.get()
          mediaController = controller
          attachListener(controller)
          applySavedModesToPlayer(controller)
          applyVideoTrackSelection(controller, _state.value.isVideoAudioOnly)
          syncFromPlayer(controller)
          startProgressTicker()
        }
      },
      ContextCompat.getMainExecutor(context),
    )
  }

  private suspend fun ensureConnected(): MediaController? {
    mediaController?.let { return it }
    connectController()
    repeat(30) {
      mediaController?.let { return it }
      delay(50)
    }
    return mediaController
  }

  private fun applySavedModesToPlayer(controller: MediaController) {
    val current = _state.value
    controller.repeatMode = current.repeatMode.toMedia3Mode()
    controller.shuffleModeEnabled = current.shuffleEnabled
  }

  private fun applyVideoTrackSelection(controller: MediaController, audioOnly: Boolean) {
    val params =
      controller.trackSelectionParameters
        .buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, audioOnly)
        .build()
    controller.trackSelectionParameters = params
    val tv = attachedTextureViewRef?.get()
    if (audioOnly) {
      boundVideoSurfaceRef = null
      if (tv != null) {
        runCatching { controller.clearVideoTextureView(tv) }
      }
    } else if (tv != null && _state.value.currentTrack?.isVideo == true) {
      if (boundVideoSurfaceRef?.get() !== tv) {
        boundVideoSurfaceRef = WeakReference(tv)
        runCatching { controller.setVideoTextureView(tv) }
      }
    }
  }

  fun attachVideoTextureView(view: TextureView) {
    attachedTextureViewRef = WeakReference(view)
    val controller = mediaController ?: return
    if (_state.value.isVideoAudioOnly) {
      boundVideoSurfaceRef = null
      return
    }
    if (boundVideoSurfaceRef?.get() === view) return
    boundVideoSurfaceRef = WeakReference(view)
    runCatching { controller.setVideoTextureView(view) }
  }

  fun detachVideoTextureView(view: TextureView) {
    val existing = attachedTextureViewRef?.get()
    if (existing == null || existing === view) {
      attachedTextureViewRef = null
    }
    if (boundVideoSurfaceRef?.get() === view) {
      boundVideoSurfaceRef = null
    }
    runCatching { mediaController?.clearVideoTextureView(view) }
  }

  fun setVideoAudioOnlyMode(audioOnly: Boolean) {
    perItemAudioOnlyOverride = audioOnly
    _state.update { it.copy(isVideoAudioOnly = audioOnly) }
    scope.launch {
      val controller = ensureConnected() ?: return@launch
      applyVideoTrackSelection(controller, audioOnly)
    }
  }

  private fun attachListener(controller: MediaController) {
    controller.addListener(
      object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
          if (!isPlaying) {
            lastActivePlayingTickElapsedMs = 0L
            lastKnownPositionMs = controller.currentPosition.coerceAtLeast(0L)
            flushPendingListeningTime(forceFileSync = true)
          } else {
            lastActivePlayingTickElapsedMs = SystemClock.elapsedRealtime()
            ensurePlaySessionRecordedForCurrentTrack()
          }
          syncFromPlayer(controller)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
          val finishedNaturally = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
          val previousTrack = _state.value.currentTrack
          flushPendingListeningTime(forceFileSync = false)
          currentSessionHandle = null
          activePlayLogId = null
          if (finishedNaturally && previousTrack != null) {
            scope.launch(Dispatchers.IO) {
              trackingMutex.withLock {
                repository.clearSavedResumePosition(previousTrack.contentUri)
              }
            }
          }
          if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
            perItemAudioOnlyOverride = null
            lastKnownPositionMs = 0L
            pendingSessionInitialPosMs = 0L
            pendingSessionResetToZero =
              finishedNaturally || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
          }
          val resolvedAudioOnly =
            perItemAudioOnlyOverride
              ?: (_state.value.defaultVideoPlaybackMode == VideoPlaybackMode.AUDIO_ONLY)
          _state.update { it.copy(isVideoAudioOnly = resolvedAudioOnly) }
          applyVideoTrackSelection(controller, resolvedAudioOnly)

          if (_sleepTimerState.value.mode == SleepTimerMode.END_OF_TRACK && finishedNaturally) {
            triggerImmediateFadeOutPause()
          }
          syncFromPlayer(controller)
          if (controller.isPlaying) {
            lastActivePlayingTickElapsedMs = SystemClock.elapsedRealtime()
            ensurePlaySessionRecordedForCurrentTrack()
          }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
          if (videoSize.width > 0 && videoSize.height > 0) {
            val ratio = (videoSize.width.toFloat() / videoSize.height.toFloat()).coerceIn(0.45f, 2.5f)
            _state.update { it.copy(videoAspectRatio = ratio) }
          }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
          if (playbackState == Player.STATE_ENDED) {
            lastActivePlayingTickElapsedMs = 0L
            lastKnownPositionMs = 0L
            flushPendingListeningTime(forceFileSync = true)
            val endedHandle = currentSessionHandle
            currentSessionHandle = null
            activePlayLogId = null
            pendingSessionTrack = null
            pendingSessionInitialPosMs = 0L
            pendingSessionResetToZero = true
            val finishedTrack = endedHandle?.track ?: _state.value.currentTrack
            if (finishedTrack != null) {
              scope.launch(Dispatchers.IO) {
                trackingMutex.withLock {
                  repository.clearSavedResumePosition(finishedTrack.contentUri)
                }
              }
            }
            if (_sleepTimerState.value.mode == SleepTimerMode.END_OF_TRACK) {
              completeSleepTimerCleanup(controller)
            }
          }
          syncFromPlayer(controller)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
          val mapped = repeatMode.toAppRepeatMode()
          _state.update { it.copy(repeatMode = mapped) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
          _state.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }
      }
    )
  }

  private fun syncFromPlayer(controller: MediaController) {
    val index = controller.currentMediaItemIndex
    val track =
      activeQueue.getOrNull(index)
        ?: controller.currentMediaItem?.let { item ->
          val meta = item.mediaMetadata
          val itemUri = item.localConfiguration?.uri ?: Uri.EMPTY
          val looksVideo =
            itemUri.toString().contains("/video/", ignoreCase = true) ||
              itemUri.toString().let { u ->
                val lower = u.lowercase()
                lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".mov")
              }
          AudioTrack(
            id = item.mediaId.toLongOrNull() ?: -1L,
            title = meta.title?.toString().orEmpty().ifBlank { "Media Track" },
            artist = meta.artist?.toString().orEmpty().ifBlank { "Local Media" },
            album = meta.albumTitle?.toString().orEmpty().ifBlank { "Local" },
            albumId = 0L,
            durationMs = controller.duration.coerceAtLeast(0L),
            contentUri = itemUri,
            albumArtUri = meta.artworkUri?.takeUnless { it.scheme == "mplayerart" },
            dateAddedMs = 0L,
            dateModifiedMs = 0L,
            sizeBytes = 0L,
            mimeType = if (looksVideo) "video/*" else "audio/*",
            folderName = if (looksVideo) "Videos" else "Music",
            mediaType = if (looksVideo) MediaType.VIDEO else MediaType.AUDIO,
          )
        }

    val rawDuration = controller.duration.coerceAtLeast(0L)
    val duration = if (rawDuration > 0L) rawDuration else (track?.durationMs ?: 0L)
    val vs = controller.videoSize
    val ratio =
      if (vs.width > 0 && vs.height > 0) (vs.width.toFloat() / vs.height.toFloat()).coerceIn(0.45f, 2.5f)
      else _state.value.videoAspectRatio

    val tv = attachedTextureViewRef?.get()
    if (tv != null && track?.isVideo == true && !_state.value.isVideoAudioOnly) {
      runCatching { controller.setVideoTextureView(tv) }
    }

    _state.update {
      it.copy(
        currentTrack = track,
        queue = activeQueue,
        currentIndex = index,
        isPlaying = controller.isPlaying,
        positionMs = controller.currentPosition.coerceAtLeast(0L),
        durationMs = duration,
        repeatMode = controller.repeatMode.toAppRepeatMode(),
        shuffleEnabled = controller.shuffleModeEnabled,
        videoAspectRatio = ratio,
      )
    }
  }

  private fun startProgressTicker() {
    progressJob?.cancel()
    progressJob =
      scope.launch {
        while (isActive) {
          mediaController?.let { controller ->
            val nowElapsed = SystemClock.elapsedRealtime()
            val curPos = controller.currentPosition.coerceAtLeast(0L)
            if (controller.isPlaying || controller.playbackState == Player.STATE_READY) {
              lastKnownPositionMs = curPos
            }
            if (controller.isPlaying && controller.playbackState == Player.STATE_READY) {
              if (lastActivePlayingTickElapsedMs > 0L) {
                val delta = (nowElapsed - lastActivePlayingTickElapsedMs).coerceIn(0L, 600L)
                pendingListenedMs += delta
              }
              lastActivePlayingTickElapsedMs = nowElapsed
              if (pendingListenedMs >= 2_000L) {
                flushPendingListeningTime(forceFileSync = false)
              }
            } else {
              lastActivePlayingTickElapsedMs = 0L
              if (pendingListenedMs > 0L) {
                flushPendingListeningTime(forceFileSync = true)
              }
            }

            if (controller.isPlaying || controller.playbackState == Player.STATE_READY) {
              val rawDuration = controller.duration.coerceAtLeast(0L)
              val fallbackDur = _state.value.currentTrack?.durationMs ?: 0L
              _state.update {
                it.copy(
                  positionMs = curPos,
                  durationMs = if (rawDuration > 0L) rawDuration else fallbackDur,
                  isPlaying = controller.isPlaying,
                )
              }
            }
          }
          delay(250L)
        }
      }
  }

  // ============================================================================
  // Sleep Timer with Smooth Fade-Out
  // ============================================================================

  fun startSleepTimerMinutes(minutes: Int) {
    val durationMs = minutes.coerceAtLeast(1) * 60_000L
    sleepTimerJob?.cancel()
    mediaController?.volume = 1.0f
    sleepEndOfTrackMediaId = null
    sleepTargetEndElapsedMs = android.os.SystemClock.elapsedRealtime() + durationMs
    _sleepTimerState.value =
      SleepTimerState(
        mode = SleepTimerMode.DURATION,
        remainingMs = durationMs,
        totalDurationMs = durationMs,
        isFadingOut = false,
      )

    sleepTimerJob =
      scope.launch {
        val fadeWindowMs = 4_000L
        while (isActive) {
          val remaining = (sleepTargetEndElapsedMs - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0L)
          val controller = mediaController

          if (remaining <= 0L) {
            if (controller != null && controller.isPlaying) {
              var v = controller.volume
              while (v > 0.04f && isActive) {
                v = (v - 0.12f).coerceAtLeast(0f)
                controller.volume = v
                delay(80L)
              }
              controller.pause()
            }
            completeSleepTimerCleanup(controller)
            break
          }

          val fading = remaining <= fadeWindowMs
          if (fading && controller != null && controller.isPlaying) {
            val linear = (remaining.toFloat() / fadeWindowMs.toFloat()).coerceIn(0f, 1f)
            controller.volume = (linear * linear).coerceIn(0.02f, 1f)
          }

          _sleepTimerState.update {
            it.copy(remainingMs = remaining, isFadingOut = fading)
          }
          delay(200L)
        }
      }
  }

  fun startSleepTimerEndOfTrack() {
    sleepTimerJob?.cancel()
    val controller = mediaController
    controller?.volume = 1.0f
    val currentId = controller?.currentMediaItem?.mediaId ?: _state.value.currentTrack?.id?.toString()
    sleepEndOfTrackMediaId = currentId

    val dur = _state.value.durationMs.coerceAtLeast(0L)
    val pos = _state.value.positionMs.coerceAtLeast(0L)
    val initialRem = (dur - pos).coerceAtLeast(0L)

    _sleepTimerState.value =
      SleepTimerState(
        mode = SleepTimerMode.END_OF_TRACK,
        remainingMs = initialRem,
        totalDurationMs = dur,
        isFadingOut = false,
      )

    sleepTimerJob =
      scope.launch {
        val fadeWindowMs = 3_500L
        while (isActive) {
          val activeCtrl = mediaController
          val liveDur = (activeCtrl?.duration ?: 0L).let { if (it > 0L) it else _state.value.durationMs }
          val livePos = activeCtrl?.currentPosition ?: _state.value.positionMs
          val remaining = (liveDur - livePos).coerceAtLeast(0L)

          val activeMediaId = activeCtrl?.currentMediaItem?.mediaId
          if (sleepEndOfTrackMediaId != null && activeMediaId != null && activeMediaId != sleepEndOfTrackMediaId) {
            triggerImmediateFadeOutPause()
            break
          }

          val fading = liveDur > 5_000L && remaining <= fadeWindowMs && (activeCtrl?.isPlaying == true)
          if (fading) {
            val linear = (remaining.toFloat() / fadeWindowMs.toFloat()).coerceIn(0f, 1f)
            activeCtrl?.volume = (linear * linear).coerceIn(0.02f, 1f)
          }

          if (liveDur > 0L && remaining <= 280L && (activeCtrl?.isPlaying == true)) {
            triggerImmediateFadeOutPause()
            break
          }

          _sleepTimerState.update {
            it.copy(
              remainingMs = remaining,
              totalDurationMs = liveDur,
              isFadingOut = fading,
            )
          }
          delay(200L)
        }
      }
  }

  private fun triggerImmediateFadeOutPause() {
    sleepTimerJob?.cancel()
    sleepTimerJob =
      scope.launch {
        val controller = mediaController
        _sleepTimerState.update { it.copy(isFadingOut = true, remainingMs = 0L) }
        if (controller != null && controller.isPlaying) {
          var v = controller.volume
          while (v > 0.04f && isActive) {
            v = (v - 0.14f).coerceAtLeast(0f)
            controller.volume = v
            delay(75L)
          }
        }
        completeSleepTimerCleanup(controller)
      }
  }

  fun cancelSleepTimer() {
    sleepTimerJob?.cancel()
    sleepTimerJob = null
    sleepEndOfTrackMediaId = null
    mediaController?.volume = 1.0f
    _sleepTimerState.value = SleepTimerState()
  }

  internal fun completeSleepTimerCleanup(controller: MediaController? = mediaController) {
    sleepTimerJob?.cancel()
    sleepTimerJob = null
    sleepEndOfTrackMediaId = null
    lastActivePlayingTickElapsedMs = 0L
    if (controller != null) {
      runCatching {
        lastKnownPositionMs = controller.currentPosition.coerceAtLeast(0L)
        controller.playWhenReady = false
        controller.pause()
        controller.stop()
        controller.volume = 1.0f
        controller.sendCustomCommand(
          SessionCommand(PlaybackService.ACTION_SLEEP_TIMER_STOP, Bundle.EMPTY),
          Bundle.EMPTY,
        )
      }
    }
    flushPendingListeningTime(forceFileSync = true)
    AudioVisualizerBus.reset()
    _state.update { it.copy(isPlaying = false) }
    _sleepTimerState.value = SleepTimerState()
    stopActivePlaybackAndOverlayServices()
  }

  private fun stopActivePlaybackAndOverlayServices() {
    runCatching { context.stopService(Intent(context, PlaybackService::class.java)) }
    runCatching { context.stopService(Intent(context, FloatingOverlayService::class.java)) }
  }

  // ============================================================================
  // Audio Equalizer Controls (Real-time DSP + Persistence)
  // ============================================================================

  fun setEqEnabled(enabled: Boolean) {
    AudioEqualizerBus.setEnabled(enabled)
    scope.launch { prefsRepository.setEqEnabled(enabled) }
  }

  fun applyEqPreset(preset: EqualizerPreset) {
    AudioEqualizerBus.applyPreset(preset)
    if (!AudioEqualizerBus.isEnabled) {
      AudioEqualizerBus.setEnabled(true)
      scope.launch { prefsRepository.setEqEnabled(true) }
    }
    scope.launch { prefsRepository.setEqBandGains(preset.gainsDb) }
  }

  fun setEqBandGain(bandIndex: Int, gainDb: Float) {
    AudioEqualizerBus.setBandGain(bandIndex, gainDb)
    if (!AudioEqualizerBus.isEnabled) {
      AudioEqualizerBus.setEnabled(true)
      scope.launch { prefsRepository.setEqEnabled(true) }
    }
    val updated = AudioEqualizerBus.bandGainsDbFlow.value
    scope.launch { prefsRepository.setEqBandGains(updated) }
  }

  // ============================================================================
  // Queue & Transport Controls
  // ============================================================================

  fun playQueue(
    tracks: List<AudioTrack>,
    startIndex: Int = 0,
    startPositionMs: Long = 0L,
    forceAudioOnly: Boolean? = null,
    autoResumeIfLong: Boolean = false,
    forceRestartFromZero: Boolean = false,
  ) {
    if (tracks.isEmpty()) return
    val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
    val safeStartPos = if (forceRestartFromZero) 0L else startPositionMs.coerceAtLeast(0L)
    activeQueue = tracks
    perItemAudioOnlyOverride = forceAudioOnly
    val resolvedAudioOnly =
      forceAudioOnly ?: (_state.value.defaultVideoPlaybackMode == VideoPlaybackMode.AUDIO_ONLY)
    _state.update { it.copy(isVideoAudioOnly = resolvedAudioOnly) }
    lastKnownPositionMs = safeStartPos

    scope.launch {
      if (forceRestartFromZero) {
        repository.clearSavedResumePosition(tracks[safeIndex].contentUri)
      }
      val resolvedStartPos =
        when {
          forceRestartFromZero -> 0L
          safeStartPos > 0L -> safeStartPos
          autoResumeIfLong -> repository.getAutoResumePositionMs(tracks[safeIndex])
          else -> 0L
        }
      lastKnownPositionMs = resolvedStartPos
      val startArtworkBytes = repository.loadBinderSafeArtworkBytes(tracks[safeIndex])
      val controller = ensureConnected() ?: return@launch
      controller.volume = 1.0f
      applyVideoTrackSelection(controller, resolvedAudioOnly)
      flushPendingListeningTime(forceFileSync = false)
      currentSessionHandle = null
      activePlayLogId = null
      lastActivePlayingTickElapsedMs = 0L
      val shouldResetZero = forceRestartFromZero || resolvedStartPos == 0L
      pendingSessionTrack = tracks[safeIndex]
      pendingSessionInitialPosMs = resolvedStartPos
      pendingSessionResetToZero = shouldResetZero

      val mediaItems =
        tracks.mapIndexed { idx, track ->
          track.toMediaItem(
            artworkBytes = if (idx == safeIndex) startArtworkBytes else null
          )
        }
      controller.setMediaItems(mediaItems, safeIndex, resolvedStartPos)
      controller.prepare()
      controller.play()
      syncFromPlayer(controller)
      if (controller.isPlaying) {
        lastActivePlayingTickElapsedMs = SystemClock.elapsedRealtime()
        ensurePlaySessionRecordedForCurrentTrack()
      }
    }
  }

  fun playExternalUri(uri: Uri, audioOnly: Boolean? = null) {
    scope.launch {
      val track = repository.resolveExternalTrack(uri)
      playQueue(listOf(track), 0, forceAudioOnly = audioOnly, autoResumeIfLong = true)
    }
  }

  fun togglePlayPause() {
    scope.launch {
      val controller = ensureConnected() ?: return@launch
      if (controller.isPlaying) {
        lastKnownPositionMs = controller.currentPosition.coerceAtLeast(0L)
        controller.pause()
      } else {
        if (controller.volume < 0.99f && !_sleepTimerState.value.isFadingOut) {
          controller.volume = 1.0f
        }
        if (controller.playbackState == Player.STATE_ENDED) {
          controller.seekTo(0L)
        }
        controller.play()
      }
    }
  }

  /**
   * Completely stops active audio playback, cancels any active sleep timer,
   * clears queued media items, and resets the player UI & visualizer state.
   */
  fun stopPlayback() {
    lastActivePlayingTickElapsedMs = 0L
    lastKnownPositionMs = mediaController?.currentPosition?.coerceAtLeast(0L) ?: lastKnownPositionMs
    flushPendingListeningTime(forceFileSync = true)
    cancelSleepTimer()
    activeQueue = emptyList()
    currentSessionHandle = null
    pendingSessionTrack = null
    pendingSessionInitialPosMs = 0L
    pendingSessionResetToZero = false
    activePlayLogId = null
    lastSeekTargetMs = 0L
    lastSeekTimestampMs = 0L
    lastKnownPositionMs = 0L
    perItemAudioOnlyOverride = null
    AudioVisualizerBus.reset()
    _state.update(::stoppedPlaybackUiState)

    attachedTextureViewRef?.get()?.let { tv ->
      runCatching { mediaController?.clearVideoTextureView(tv) }
    }

    val currentController = mediaController
    if (currentController != null) {
      currentController.playWhenReady = false
      currentController.pause()
      currentController.stop()
      currentController.clearMediaItems()
      currentController.volume = 1.0f
    } else {
      scope.launch {
        ensureConnected()?.let { controller ->
          controller.playWhenReady = false
          controller.pause()
          controller.stop()
          controller.clearMediaItems()
          controller.volume = 1.0f
        }
      }
    }
  }

  fun skipNext() {
    scope.launch {
      val controller = ensureConnected() ?: return@launch
      if (_sleepTimerState.value.mode == SleepTimerMode.END_OF_TRACK) {
        sleepEndOfTrackMediaId = null
      }
      if (controller.hasNextMediaItem()) {
        controller.seekToNextMediaItem()
      } else if (activeQueue.isNotEmpty()) {
        controller.seekTo(0, 0L)
      }
      controller.play()
    }
  }

  fun skipPrevious() {
    scope.launch {
      val controller = ensureConnected() ?: return@launch
      if (_sleepTimerState.value.mode == SleepTimerMode.END_OF_TRACK) {
        sleepEndOfTrackMediaId = null
      }
      if (controller.currentPosition > 3500L) {
        controller.seekTo(0L)
      } else if (controller.hasPreviousMediaItem()) {
        controller.seekToPreviousMediaItem()
      } else {
        controller.seekTo(0L)
      }
      controller.play()
    }
  }

  fun seekTo(positionMs: Long) {
    val target = positionMs.coerceAtLeast(0L)
    lastSeekTargetMs = target
    lastSeekTimestampMs = SystemClock.elapsedRealtime()
    lastKnownPositionMs = target
    _state.update { it.copy(positionMs = target) }
    val eventId = activePlayLogId
    scope.launch {
      val controller = ensureConnected() ?: return@launch
      controller.seekTo(target)
      if (eventId != null) {
        repository.updateLastPlaybackPosition(eventId, target)
      }
    }
  }

  fun seekBy(deltaMs: Long) {
    val controller = mediaController
    val nowElapsed = SystemClock.elapsedRealtime()
    val livePos = controller?.currentPosition?.coerceAtLeast(0L) ?: _state.value.positionMs
    val current =
      if (nowElapsed - lastSeekTimestampMs < 400L) lastSeekTargetMs else livePos
    val controllerDur = controller?.duration?.coerceAtLeast(0L) ?: 0L
    val knownDuration = if (controllerDur > 0L) controllerDur else _state.value.durationMs
    val target = computeSeekTargetMs(currentPosMs = current, durationMs = knownDuration, deltaMs = deltaMs)
    seekTo(target)
  }

  fun cycleRepeatMode() {
    val next =
      when (_state.value.repeatMode) {
        RepeatMode.OFF -> RepeatMode.ALL
        RepeatMode.ALL -> RepeatMode.ONE
        RepeatMode.ONE -> RepeatMode.OFF
      }
    _state.update { it.copy(repeatMode = next) }
    scope.launch {
      prefsRepository.setRepeatMode(next)
      ensureConnected()?.repeatMode = next.toMedia3Mode()
    }
  }

  fun toggleShuffle() {
    val next = !_state.value.shuffleEnabled
    _state.update { it.copy(shuffleEnabled = next) }
    scope.launch {
      prefsRepository.setShuffleEnabled(next)
      ensureConnected()?.shuffleModeEnabled = next
    }
  }

  fun toggle8DAudio() {
    set8DAudio(!_state.value.is8DEnabled)
  }

  fun set8DAudio(enabled: Boolean) {
    Audio8DEffectBus.setEnabled(enabled)
    _state.update { it.copy(is8DEnabled = enabled) }
    scope.launch {
      prefsRepository.set8DEnabled(enabled)
      mediaController?.sendCustomCommand(
        SessionCommand(PlaybackService.ACTION_TOGGLE_8D, Bundle.EMPTY),
        Bundle().apply { putBoolean(PlaybackService.EXTRA_8D_ENABLED, enabled) },
      )
    }
  }

  fun set8DSpeed(speed: Audio8DSpeed) {
    Audio8DEffectBus.setSpeed(speed)
    _state.update { it.copy(audio8DSpeed = speed) }
    scope.launch { prefsRepository.set8DSpeed(speed) }
  }

  fun set8DIntensity(percent: Int) {
    val clamped = percent.coerceIn(0, 100)
    Audio8DEffectBus.setIntensityPercent(clamped)
    _state.update { it.copy(audio8DIntensity = clamped) }
    scope.launch { prefsRepository.set8DIntensity(clamped) }
  }

  fun setVolumeBoostPercent(percent: Int) {
    val clamped = percent.coerceIn(0, 797)
    val (curStep, maxStep) = hardwareVolumeMonitor.queryCurrentStepAndMax()
    val targetStep =
      if (maxStep > 0) HardwareVolumeMath.percentToHardwareStep(clamped, maxStep) else 0
    lastHardwareWriteElapsedMs = SystemClock.elapsedRealtime()
    lastHardwareStepWritten = targetStep
    if (maxStep > 0 && curStep != targetStep) {
      hardwareVolumeMonitor.setHardwareMusicStep(targetStep)
    }
    val (actualStepAfter, _) = hardwareVolumeMonitor.queryCurrentStepAndMax()
    lastObservedHardwareStep = actualStepAfter
    val fineTrim =
      HardwareVolumeMath.computePcmTrimForStandardPercent(clamped, actualStepAfter, maxStep)
    VolumeBoostBus.setBoostPercent(clamped, fineTrim = fineTrim)
    _state.update { it.copy(volumeBoostPercent = clamped) }
    scope.launch { prefsRepository.setVolumeBoostPercent(clamped) }
  }

  fun setAmplifierEnabled(enabled: Boolean) {
    AudioAmplifierBus.setEnabled(enabled)
    _state.update { it.copy(amplifierEnabled = enabled) }
    scope.launch { prefsRepository.setAmplifierEnabled(enabled) }
  }

  fun setAmplifierLevelPercent(percent: Int) {
    val clamped = percent.coerceIn(0, 100)
    AudioAmplifierBus.setLevelPercent(clamped)
    _state.update { it.copy(amplifierLevelPercent = clamped) }
    scope.launch { prefsRepository.setAmplifierLevelPercent(clamped) }
  }

  fun refreshAudioOutputRoute() {
    val route = AudioRouteMonitor.queryActiveOutputRoute(context)
    AudioOutputRouteBus.updateRoute(route)
    val (curStep, maxStep) = hardwareVolumeMonitor.queryCurrentStepAndMax()
    lastObservedHardwareStep = curStep
    val hwPct = HardwareVolumeMath.hardwareStepToPercent(curStep, maxStep)
    val curState = _state.value
    val shouldSyncToHardware = maxStep > 0 && curState.volumeBoostPercent <= 100
    if (shouldSyncToHardware && curState.volumeBoostPercent != hwPct) {
      VolumeBoostBus.setBoostPercent(hwPct, fineTrim = 1f)
      _state.update { it.copy(audioOutputRoute = route, volumeBoostPercent = hwPct) }
    } else {
      _state.update { it.copy(audioOutputRoute = route) }
    }
  }

  fun jumpToQueueIndex(index: Int) {
    if (index !in activeQueue.indices) return
    scope.launch {
      val controller = ensureConnected() ?: return@launch
      controller.seekTo(index, 0L)
      controller.play()
    }
  }

  private fun ensurePlaySessionRecordedForCurrentTrack() {
    val activeTrack = pendingSessionTrack ?: _state.value.currentTrack ?: return
    val uriKey = activeTrack.contentUri.toString()
    val existing = currentSessionHandle
    if (existing != null && existing.sessionUri == uriKey) {
      return
    }
    flushPendingListeningTime(forceFileSync = false)
    val initialPos = pendingSessionInitialPosMs
    val resetZero = pendingSessionResetToZero
    pendingSessionTrack = null
    pendingSessionInitialPosMs = 0L
    pendingSessionResetToZero = false
    val handle =
      PlaySessionHandle(
        sessionUri = uriKey,
        track = activeTrack,
        initialPositionMs = initialPos,
        resetToZero = resetZero,
      )
    currentSessionHandle = handle
    scope.launch(Dispatchers.IO) {
      trackingMutex.withLock {
        val insertedId =
          repository.recordTrackPlay(handle.track, resetPositionToZero = handle.resetToZero)
        handle.eventId = insertedId
        if (currentSessionHandle === handle) {
          activePlayLogId = insertedId
        }
        if (handle.resetToZero) {
          repository.updateLastPlaybackPosition(insertedId, 0L)
        } else if (handle.initialPositionMs > 0L) {
          repository.updateLastPlaybackPosition(insertedId, handle.initialPositionMs)
        }
        if (usageFileSyncEnabled) {
          val res = repository.syncUsageStatsJson(usageFileExternalUri)
          if (res.success) {
            lastUsageFileSyncElapsedMs = SystemClock.elapsedRealtime()
            prefsRepository.setUsageLastSyncedIso(res.syncedAtIso)
          }
        }
      }
    }
  }

  private fun flushPendingListeningTime(forceFileSync: Boolean) {
    val deltaToFlush = pendingListenedMs
    pendingListenedMs = 0L
    val handle = currentSessionHandle
    val posToSave = lastKnownPositionMs
    if ((deltaToFlush <= 0L || handle == null) && !forceFileSync && (handle == null || posToSave <= 0L)) {
      return
    }

    scope.launch(Dispatchers.IO) {
      trackingMutex.withLock {
        val eventId = handle?.eventId ?: activePlayLogId
        if (eventId != null) {
          if (deltaToFlush > 0L) {
            repository.addListenedDuration(eventId, deltaToFlush)
          }
          if (posToSave > 0L) {
            repository.updateLastPlaybackPosition(eventId, posToSave)
          }
        }
        if (usageFileSyncEnabled) {
          val nowElapsed = SystemClock.elapsedRealtime()
          if (forceFileSync || nowElapsed - lastUsageFileSyncElapsedMs >= 6_000L) {
            val res = repository.syncUsageStatsJson(usageFileExternalUri)
            if (res.success) {
              lastUsageFileSyncElapsedMs = nowElapsed
              prefsRepository.setUsageLastSyncedIso(res.syncedAtIso)
            }
          }
        }
      }
    }
  }

  private fun AudioTrack.toMediaItem(artworkBytes: ByteArray? = null): MediaItem =
    buildTrackMediaItem(track = this, artworkBytes = artworkBytes)

  private fun RepeatMode.toMedia3Mode(): Int =
    when (this) {
      RepeatMode.OFF -> Player.REPEAT_MODE_OFF
      RepeatMode.ALL -> Player.REPEAT_MODE_ALL
      RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    }

  private fun Int.toAppRepeatMode(): RepeatMode =
    when (this) {
      Player.REPEAT_MODE_ALL -> RepeatMode.ALL
      Player.REPEAT_MODE_ONE -> RepeatMode.ONE
      else -> RepeatMode.OFF
    }
}

internal fun computeSeekTargetMs(
  currentPosMs: Long,
  durationMs: Long,
  deltaMs: Long,
): Long {
  val safeCurrent = currentPosMs.coerceAtLeast(0L)
  val rawTarget = (safeCurrent + deltaMs).coerceAtLeast(0L)
  return if (durationMs > 0L) {
    rawTarget.coerceAtMost(durationMs)
  } else {
    rawTarget
  }
}

internal fun stoppedPlaybackUiState(current: PlaybackUiState): PlaybackUiState =
  current.copy(
    currentTrack = null,
    queue = emptyList(),
    currentIndex = -1,
    isPlaying = false,
    positionMs = 0L,
    durationMs = 0L,
  )

internal fun buildTrackMediaItem(
  track: AudioTrack,
  artworkBytes: ByteArray? = null,
): MediaItem {
  val metaBuilder =
    MediaMetadata.Builder()
      .setTitle(track.title)
      .setArtist(track.artist)
      .setAlbumTitle(track.album)
      .setArtworkUri(AudioScanner.buildMediaSessionArtworkUri(track))
  if (artworkBytes != null && artworkBytes.isNotEmpty()) {
    metaBuilder.setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
  }
  return MediaItem.Builder()
    .setMediaId(track.id.toString())
    .setUri(track.contentUri)
    .setMediaMetadata(metaBuilder.build())
    .build()
}
