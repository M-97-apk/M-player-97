package com.example.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import com.example.data.media.AudioScanner
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

  private var mediaSession: MediaSession? = null
  private val audio8DProcessor = Audio8DEffectProcessor()
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var visualizerTickerJob: Job? = null
  private var loudnessEnhancer: LoudnessEnhancer? = null
  private var attachedAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET
  private var audioRouteRegistration: AudioRouteMonitor.Registration? = null

  override fun onCreate() {
    super.onCreate()
    ensureNotificationChannel()

    val renderersFactory =
      object : DefaultRenderersFactory(this) {
        override fun buildAudioSink(
          context: Context,
          enableFloatOutput: Boolean,
          enableAudioTrackPlaybackParams: Boolean,
        ): AudioSink {
          return DefaultAudioSink.Builder(context)
            .setAudioProcessors(arrayOf(audio8DProcessor))
            .build()
        }
      }

    val audioAttributes =
      AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    val player =
      ExoPlayer.Builder(this, renderersFactory)
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .build()

    val openAppIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val sessionCallback =
      object : MediaSession.Callback {
        override fun onConnect(
          session: MediaSession,
          controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
          val sessionCommands =
            MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
              .add(SessionCommand(ACTION_TOGGLE_8D, Bundle.EMPTY))
              .add(SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY))
              .add(SessionCommand(ACTION_SLEEP_TIMER_STOP, Bundle.EMPTY))
              .build()
          return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(sessionCommands)
            .setMediaButtonPreferences(
              buildOneUiMediaButtonPreferences(
                is8DEnabled = Audio8DEffectBus.isEnabled,
                isShuffleEnabled = player.shuffleModeEnabled,
              )
            )
            .build()
        }

        override fun onCustomCommand(
          session: MediaSession,
          controller: MediaSession.ControllerInfo,
          customCommand: SessionCommand,
          args: Bundle,
        ): ListenableFuture<SessionResult> {
          when (customCommand.customAction) {
            ACTION_TOGGLE_8D -> {
              val desired =
                if (args.containsKey(EXTRA_8D_ENABLED)) {
                  args.getBoolean(EXTRA_8D_ENABLED)
                } else {
                  !Audio8DEffectBus.isEnabled
                }
              Audio8DEffectBus.setEnabled(desired)
              refreshMediaButtonPreferences(session)
              return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            ACTION_TOGGLE_SHUFFLE -> {
              player.shuffleModeEnabled = !player.shuffleModeEnabled
              refreshMediaButtonPreferences(session)
              return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            ACTION_SLEEP_TIMER_STOP -> {
              player.playWhenReady = false
              player.pause()
              player.stop()
              player.volume = 1.0f
              AudioVisualizerBus.reset()
              stopForeground(STOP_FOREGROUND_REMOVE)
              stopSelf()
              return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
          }
          return super.onCustomCommand(session, controller, customCommand, args)
        }
      }

    val bitmapLoader = CacheBitmapLoader(MPlayerBitmapLoader(this))

    val session =
      MediaSession.Builder(this, player)
        .setSessionActivity(openAppIntent)
        .setCallback(sessionCallback)
        .setBitmapLoader(bitmapLoader)
        .setMediaButtonPreferences(
          buildOneUiMediaButtonPreferences(
            is8DEnabled = Audio8DEffectBus.isEnabled,
            isShuffleEnabled = player.shuffleModeEnabled,
          )
        )
        .build()

    mediaSession = session

    player.addAnalyticsListener(
      object : AnalyticsListener {
        override fun onAudioPositionAdvancing(
          eventTime: AnalyticsListener.EventTime,
          playoutStartSystemTimeMs: Long,
        ) {
          AudioVisualizerBus.onAudioPlayoutStarted()
        }
      }
    )

    player.addListener(
      object : Player.Listener {
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
          refreshMediaButtonPreferences(session)
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
          attachOrUpdateLoudnessEnhancer(
            sessionId = audioSessionId,
            boostPercent = VolumeBoostBus.boostPercent,
            amplifierEnabled = AudioAmplifierBus.isEnabled,
            amplifierLevel = AudioAmplifierBus.levelPercent,
          )
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
          updateVisualizerLoop(player, isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
          if (playbackState == Player.STATE_READY) {
            attachOrUpdateLoudnessEnhancer(
              sessionId = player.audioSessionId,
              boostPercent = VolumeBoostBus.boostPercent,
              amplifierEnabled = AudioAmplifierBus.isEnabled,
              amplifierLevel = AudioAmplifierBus.levelPercent,
            )
          }
          if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
            updateVisualizerLoop(player, player.isPlaying)
          }
        }
      }
    )

    attachOrUpdateLoudnessEnhancer(
      sessionId = player.audioSessionId,
      boostPercent = VolumeBoostBus.boostPercent,
      amplifierEnabled = AudioAmplifierBus.isEnabled,
      amplifierLevel = AudioAmplifierBus.levelPercent,
    )

    audioRouteRegistration =
      AudioRouteMonitor.register(
        context = this,
        onBecomingNoisy = {
          if (player.isPlaying) {
            player.pause()
          }
        },
        onRouteChanged = { /* AudioOutputRouteBus is kept synchronized automatically */ },
      )

    serviceScope.launch {
      Audio8DEffectBus.isEnabledFlow.collectLatest {
        refreshMediaButtonPreferences(session)
      }
    }

    serviceScope.launch {
      combine(
        VolumeBoostBus.boostPercentFlow,
        AudioAmplifierBus.isEnabledFlow,
        AudioAmplifierBus.levelPercentFlow,
      ) { boostPct, ampEnabled, ampLevel ->
        Triple(boostPct, ampEnabled, ampLevel)
      }.collectLatest { (boostPct, ampEnabled, ampLevel) ->
        attachOrUpdateLoudnessEnhancer(
          sessionId = player.audioSessionId,
          boostPercent = boostPct,
          amplifierEnabled = ampEnabled,
          amplifierLevel = ampLevel,
        )
      }
    }

    setMediaNotificationProvider(OneUiMediaNotificationProvider(this))
  }

  private fun attachOrUpdateLoudnessEnhancer(
    sessionId: Int,
    boostPercent: Int,
    amplifierEnabled: Boolean,
    amplifierLevel: Int,
  ) {
    if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0) return
    if (loudnessEnhancer == null || attachedAudioSessionId != sessionId) {
      runCatching { loudnessEnhancer?.release() }
      loudnessEnhancer =
        runCatching { LoudnessEnhancer(sessionId) }.getOrNull()?.also {
          attachedAudioSessionId = sessionId
        }
    }
    val enhancer = loudnessEnhancer ?: return
    runCatching {
      val boostMb =
        if (boostPercent > 100) {
          (20.0 * kotlin.math.log10(boostPercent.toDouble() / 100.0) * 100.0)
            .toInt()
            .coerceAtLeast(0)
        } else {
          0
        }
      val ampDb = AudioAmplifierBus.computeGainDb(amplifierEnabled, amplifierLevel)
      val ampMb = (ampDb * 100f).toInt().coerceAtLeast(0)
      val totalMb = (boostMb + ampMb).coerceIn(0, 9700)
      if (totalMb > 0 && boostPercent > 0) {
        val applied =
          runCatching {
            enhancer.setTargetGain(totalMb)
            enhancer.enabled = true
            true
          }.getOrDefault(false)
        if (!applied) {
          for (fallbackCap in intArrayOf(4000, 2500, 1500)) {
            val ok =
              runCatching {
                enhancer.setTargetGain(totalMb.coerceAtMost(fallbackCap))
                enhancer.enabled = true
                true
              }.getOrDefault(false)
            if (ok) break
          }
        }
      } else {
        enhancer.setTargetGain(0)
        enhancer.enabled = false
      }
    }
  }

  private fun updateVisualizerLoop(player: ExoPlayer, isPlaying: Boolean) {
    visualizerTickerJob?.cancel()
    visualizerTickerJob =
      if (isPlaying) {
        serviceScope.launch {
          var lastTickMs = SystemClock.elapsedRealtime()
          while (isActive && player.isPlaying) {
            val nowMs = SystemClock.elapsedRealtime()
            val dtMs = (nowMs - lastTickMs).coerceIn(1L, 64L).toDouble()
            lastTickMs = nowMs
            AudioVisualizerBus.advancePlayheadAndPublish(
              deltaWallMs = dtMs,
              playbackSpeed = player.playbackParameters.speed,
            )
            delay(16L)
          }
        }
      } else {
        serviceScope.launch {
          var steps = 0
          while (isActive && !player.isPlaying && steps < 25) {
            if (!AudioVisualizerBus.decayToRest()) break
            steps++
            delay(16L)
          }
        }
      }
  }

  private fun refreshMediaButtonPreferences(session: MediaSession) {
    session.setMediaButtonPreferences(
      buildOneUiMediaButtonPreferences(
        is8DEnabled = Audio8DEffectBus.isEnabled,
        isShuffleEnabled = session.player.shuffleModeEnabled,
      )
    )
  }

  private fun buildOneUiMediaButtonPreferences(
    is8DEnabled: Boolean,
    isShuffleEnabled: Boolean,
  ): List<CommandButton> {
    val toggle8DButton =
      CommandButton.Builder(CommandButton.ICON_UNDEFINED)
        .setCustomIconResId(
          if (is8DEnabled) R.drawable.ic_oneui_8d_on else R.drawable.ic_oneui_8d_off
        )
        .setDisplayName(if (is8DEnabled) "8D On" else "8D Off")
        .setSessionCommand(SessionCommand(ACTION_TOGGLE_8D, Bundle.EMPTY))
        .setSlots(CommandButton.SLOT_OVERFLOW)
        .setEnabled(true)
        .build()

    val shuffleButton =
      CommandButton.Builder(
          if (isShuffleEnabled) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF
        )
        .setCustomIconResId(R.drawable.ic_oneui_shuffle)
        .setDisplayName(if (isShuffleEnabled) "Shuffle On" else "Shuffle Off")
        .setSessionCommand(SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY))
        .setSlots(CommandButton.SLOT_OVERFLOW)
        .setEnabled(true)
        .build()

    return listOf(toggle8DButton, shuffleButton)
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
    mediaSession

  override fun onTaskRemoved(rootIntent: Intent?) {
    val player = mediaSession?.player
    if (player == null || !player.playWhenReady || player.mediaItemCount == 0 ||
      player.playbackState == Player.STATE_ENDED
    ) {
      stopSelf()
    }
  }

  override fun onDestroy() {
    audioRouteRegistration?.unregister()
    audioRouteRegistration = null
    runCatching { loudnessEnhancer?.release() }
    loudnessEnhancer = null
    serviceScope.cancel()
    mediaSession?.run {
      player.release()
      release()
      mediaSession = null
    }
    super.onDestroy()
  }

  private fun ensureNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(NotificationManager::class.java)
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          getString(R.string.notification_channel_playback),
          NotificationManager.IMPORTANCE_LOW,
        ).apply {
          setShowBadge(false)
          lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
      manager.createNotificationChannel(channel)
    }
  }

  companion object {
    const val CHANNEL_ID = "mplayer97_playback_channel"
    const val ACTION_TOGGLE_8D = "com.example.playback.TOGGLE_8D"
    const val ACTION_TOGGLE_SHUFFLE = "com.example.playback.TOGGLE_SHUFFLE"
    const val ACTION_SLEEP_TIMER_STOP = "com.example.playback.SLEEP_TIMER_STOP"
    const val EXTRA_8D_ENABLED = "extra_8d_enabled"
  }
}

/**
 * Custom Media3 Notification Provider styled to match Samsung One UI 9's sleek rounded
 * geometric iconography and public lock screen media controls, with 8D Audio toggle support.
 */
@OptIn(UnstableApi::class)
private class OneUiMediaNotificationProvider(
  private val context: Context,
) : MediaNotification.Provider {

  private val delegate =
    DefaultMediaNotificationProvider.Builder(context)
      .setChannelId(PlaybackService.CHANNEL_ID)
      .setChannelName(R.string.notification_channel_playback)
      .build()
      .apply { setSmallIcon(R.drawable.ic_music_note) }

  override fun createNotification(
    mediaSession: MediaSession,
    customLayout: ImmutableList<CommandButton>,
    actionFactory: MediaNotification.ActionFactory,
    onNotificationChangedCallback: MediaNotification.Provider.Callback,
  ): MediaNotification {
    val wrappedCallback =
      MediaNotification.Provider.Callback { updatedNotification ->
        onNotificationChangedCallback.onNotificationChanged(
          applyOneUiStyling(updatedNotification, mediaSession)
        )
      }
    val initial =
      delegate.createNotification(mediaSession, customLayout, actionFactory, wrappedCallback)
    return applyOneUiStyling(initial, mediaSession)
  }

  override fun handleCustomCommand(
    session: MediaSession,
    action: String,
    extras: Bundle,
  ): Boolean {
    return delegate.handleCustomCommand(session, action, extras)
  }

  private fun applyOneUiStyling(
    mediaNotification: MediaNotification,
    mediaSession: MediaSession,
  ): MediaNotification {
    val notification = mediaNotification.notification
    notification.visibility = Notification.VISIBILITY_PUBLIC

    val existingActions = notification.actions ?: return mediaNotification
    val isPlaying = mediaSession.player.isPlaying

    val styledActions =
      existingActions.map { action ->
        val titleStr = action.title?.toString()?.lowercase().orEmpty()
        val oneUiIconRes =
          when {
            titleStr.contains("8d on") -> R.drawable.ic_oneui_8d_on
            titleStr.contains("8d") -> R.drawable.ic_oneui_8d_off
            titleStr.contains("shuffle") -> R.drawable.ic_oneui_shuffle
            titleStr.contains("prev") -> R.drawable.ic_oneui_prev
            titleStr.contains("next") -> R.drawable.ic_oneui_next
            titleStr.contains("pause") -> R.drawable.ic_oneui_pause
            titleStr.contains("play") -> R.drawable.ic_oneui_play
            titleStr.contains("repeat") -> R.drawable.ic_oneui_repeat
            else -> null
          }

        if (oneUiIconRes != null) {
          Notification.Action.Builder(
              Icon.createWithResource(context, oneUiIconRes),
              action.title,
              action.actionIntent,
            )
            .build()
        } else {
          action
        }
      }

    val builder =
      Notification.Builder.recoverBuilder(context, notification)
        .setSmallIcon(R.drawable.ic_music_note)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .setActions(*styledActions.toTypedArray())
        .setSubText(
          if (Audio8DEffectBus.isEnabled) {
            "8D ${Audio8DEffectBus.intensityPercent}% • M player 97"
          } else {
            "M player 97"
          }
        )

    if (notification.getLargeIcon() == null) {
      val artBytes = mediaSession.player.currentMediaItem?.mediaMetadata?.artworkData
      val fallbackBitmap =
        if (artBytes != null && artBytes.isNotEmpty()) {
          AudioScanner.decodeSampledBitmapFromBytes(artBytes, maxDimension = 384)
        } else {
          mediaSession.player.currentMediaItem?.mediaMetadata?.artworkUri?.let { artUri ->
            AudioScanner.loadBitmapForUri(context, artUri, maxDimension = 384)
          }
        }
      if (fallbackBitmap != null) {
        builder.setLargeIcon(fallbackBitmap)
      }
    }

    val rebuilt = builder.build()

    // Preserve playing/pause icon fallback if needed for compact action slots
    if (isPlaying) {
      rebuilt.visibility = Notification.VISIBILITY_PUBLIC
    }

    return MediaNotification(mediaNotification.notificationId, rebuilt)
  }
}

@OptIn(UnstableApi::class)
private class MPlayerBitmapLoader(
  private val context: Context,
) : BitmapLoader {
  private val executor: ListeningExecutorService =
    MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
  private val delegate = DataSourceBitmapLoader(executor, DefaultDataSource.Factory(context))

  override fun supportsMimeType(mimeType: String): Boolean =
    mimeType.startsWith("image/", ignoreCase = true) || delegate.supportsMimeType(mimeType)

  override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
    executor.submit<Bitmap> {
      AudioScanner.decodeSampledBitmapFromBytes(data, maxDimension = 512)
        ?: throw IllegalArgumentException("Unable to decode artwork byte array")
    }

  override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
    if (uri.scheme == "mplayerart" || uri.scheme == "file" || uri.scheme == "content") {
      return executor.submit<Bitmap> {
        AudioScanner.loadBitmapForUri(context, uri, maxDimension = 512)
          ?: throw IllegalStateException("No artwork available for $uri")
      }
    }
    return delegate.loadBitmap(uri)
  }
}
