package com.example.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.MPlayerApplication
import com.example.MainActivity
import com.example.R
import com.example.data.prefs.UserPreferences
import com.example.localization.LocalAppStrings
import com.example.model.AppThemeStyle
import com.example.model.Audio8DSpeed
import com.example.model.FloatingWidgetColorMode
import com.example.model.FloatingWidgetGlassMode
import com.example.playback.PlaybackUiState
import com.example.ui.components.Audio8DIntensitySlider
import com.example.ui.components.Audio8DSpeedSelector
import com.example.ui.components.GlassPlaybackControlButton
import com.example.ui.components.SpatialSoundfieldRadar
import com.example.ui.theme.LocalMPlayerPalette
import com.example.ui.theme.MPlayerTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

class FloatingOverlayService : Service() {

  private var windowManager: WindowManager? = null
  private var composeView: ComposeView? = null
  private var lifecycleOwner: OverlayLifecycleOwner? = null
  private var overlayLayoutParams: WindowManager.LayoutParams? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    startOverlayForeground()
    if (Settings.canDrawOverlays(this)) {
      showOverlayWindow()
    } else {
      stopSelf()
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_CLOSE) {
      (application as? MPlayerApplication)?.playerController?.stopPlayback()
      stopSelf()
      return START_NOT_STICKY
    }
    if (!Settings.canDrawOverlays(this)) {
      stopSelf()
      return START_NOT_STICKY
    }
    if (composeView == null) {
      showOverlayWindow()
    }
    return START_STICKY
  }

  private fun showOverlayWindow() {
    val wm = getSystemService(WINDOW_SERVICE) as WindowManager
    windowManager = wm

    val overlayType =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
      }

    val widthPx = (resources.displayMetrics.widthPixels * 0.92f).roundToInt().coerceAtMost(
      (390 * resources.displayMetrics.density).roundToInt()
    )

    val blurFlag =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        WindowManager.LayoutParams.FLAG_BLUR_BEHIND
      } else {
        0
      }

    val params =
      WindowManager.LayoutParams(
        widthPx,
        WindowManager.LayoutParams.WRAP_CONTENT,
        overlayType,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
          WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
          blurFlag,
        PixelFormat.TRANSLUCENT,
      ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = (76 * resources.displayMetrics.density).roundToInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          blurBehindRadius = 36
        }
      }
    overlayLayoutParams = params

    val owner = OverlayLifecycleOwner()
    lifecycleOwner = owner

    val app = application as MPlayerApplication
    val view =
      ComposeView(this).apply {
        val composeViewRef = this
        owner.attachToView(this)
        setContent {
          val prefs by
            app.prefsRepository.preferencesFlow.collectAsStateWithLifecycle(
              initialValue = UserPreferences()
            )
          val playback by app.playerController.state.collectAsStateWithLifecycle()
          val panAngle by app.playerController.panAngleDegrees.collectAsStateWithLifecycle()
          val scope = rememberCoroutineScope()

          LaunchedEffect(
            prefs.floatingWidgetGlassMode,
            prefs.floatingWidgetCustomOpacity,
            prefs.themeStyle,
            prefs.floatingWidgetColorMode,
          ) {
            val spec =
              resolveFloatingWidgetSpec(
                appThemeStyle = prefs.themeStyle,
                colorMode = prefs.floatingWidgetColorMode,
                glassMode = prefs.floatingWidgetGlassMode,
                customOpacity = prefs.floatingWidgetCustomOpacity,
              )
            overlayLayoutParams?.let { lp ->
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val crossWindowBlurReady =
                  runCatching { wm.isCrossWindowBlurEnabled }.getOrDefault(true)
                if (spec.enableWindowBlur && crossWindowBlurReady) {
                  lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                  lp.blurBehindRadius = spec.blurBehindRadiusPx
                } else {
                  lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
                  lp.blurBehindRadius = 0
                }
              }
              runCatching { windowManager?.updateViewLayout(composeViewRef, lp) }
            }
          }

          MPlayerTheme(
            themeStyle = prefs.themeStyle,
            darkTheme = prefs.isDarkMode,
            language = prefs.language,
            glassOpacity = prefs.appGlassOpacity,
          ) {
            FloatingMiniPlayerPanel(
              playback = playback,
              panAngleDegrees = panAngle,
              appThemeStyle = prefs.themeStyle,
              widgetColorMode = prefs.floatingWidgetColorMode,
              widgetGlassMode = prefs.floatingWidgetGlassMode,
              widgetCustomOpacity = prefs.floatingWidgetCustomOpacity,
              onSelectWidgetColorMode = { mode ->
                scope.launch { app.prefsRepository.setFloatingWidgetColorMode(mode) }
              },
              onSelectWidgetGlassMode = { mode ->
                scope.launch { app.prefsRepository.setFloatingWidgetGlassMode(mode) }
              },
              onChangeWidgetCustomOpacity = { opacity ->
                scope.launch { app.prefsRepository.setFloatingWidgetCustomOpacity(opacity) }
              },
              onDrag = { dx, dy ->
                overlayLayoutParams?.let { lp ->
                  lp.x += dx.roundToInt()
                  lp.y += dy.roundToInt()
                  runCatching { windowManager?.updateViewLayout(composeViewRef, lp) }
                }
              },
              onPlayPause = { app.playerController.togglePlayPause() },
              onRewind10s = { app.playerController.seekBy(-10_000L) },
              onForward10s = { app.playerController.seekBy(10_000L) },
              onPrevTrack = { app.playerController.skipPrevious() },
              onNextTrack = { app.playerController.skipNext() },
              onSeek = { pos -> app.playerController.seekTo(pos) },
              onToggle8D = { app.playerController.toggle8DAudio() },
              onSelect8DSpeed = { speed -> app.playerController.set8DSpeed(speed) },
              onChange8DIntensity = { pct -> app.playerController.set8DIntensity(pct) },
              onExpandApp = {
                startActivity(
                  Intent(this@FloatingOverlayService, MainActivity::class.java).apply {
                    flags =
                      Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                  }
                )
                stopSelf()
              },
              onClose = {
                app.playerController.stopPlayback()
                stopSelf()
              },
            )
          }
        }
      }

    owner.onCreate()
    composeView = view
    wm.addView(view, params)
  }

  override fun onDestroy() {
    composeView?.let { view ->
      runCatching { windowManager?.removeView(view) }
    }
    composeView = null
    lifecycleOwner?.onDestroy()
    lifecycleOwner = null
    super.onDestroy()
  }

  private fun startOverlayForeground() {
    val channelId = "mplayer97_overlay_channel"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(NotificationManager::class.java)
      val channel =
        NotificationChannel(
          channelId,
          getString(R.string.notification_channel_overlay),
          NotificationManager.IMPORTANCE_LOW,
        )
      manager.createNotificationChannel(channel)
    }

    val closePendingIntent =
      PendingIntent.getService(
        this,
        101,
        Intent(this, FloatingOverlayService::class.java).apply { action = ACTION_CLOSE },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val notification: Notification =
      NotificationCompat.Builder(this, channelId)
        .setSmallIcon(R.drawable.ic_music_note)
        .setContentTitle(getString(R.string.overlay_notification_title))
        .setContentText(getString(R.string.overlay_notification_text))
        .setOngoing(true)
        .addAction(0, getString(android.R.string.cancel), closePendingIntent)
        .build()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  companion object {
    private const val NOTIFICATION_ID = 9702
    private const val ACTION_CLOSE = "com.example.overlay.ACTION_CLOSE"

    fun start(context: Context) {
      if (!Settings.canDrawOverlays(context)) return
      val intent = Intent(context, FloatingOverlayService::class.java)
      ContextCompat.startForegroundService(context, intent)
    }
  }
}

/**
 * Transparent frosted glass floating mini-player capsule matching the active theme
 * (Extra-Deep Burgundy by default) with explicit -10s rewind and +10s forward controls.
 */
@Composable
fun FloatingMiniPlayerPanel(
  playback: PlaybackUiState,
  panAngleDegrees: Float = 0f,
  appThemeStyle: AppThemeStyle = AppThemeStyle.DEEP_BURGUNDY,
  widgetColorMode: FloatingWidgetColorMode = FloatingWidgetColorMode.MATCH_APP,
  widgetGlassMode: FloatingWidgetGlassMode = FloatingWidgetGlassMode.FROSTED_BLUR,
  widgetCustomOpacity: Float? = null,
  onSelectWidgetColorMode: (FloatingWidgetColorMode) -> Unit = {},
  onSelectWidgetGlassMode: (FloatingWidgetGlassMode) -> Unit = {},
  onChangeWidgetCustomOpacity: (Float) -> Unit = {},
  onDrag: ((Float, Float) -> Unit)?,
  onPlayPause: () -> Unit,
  onRewind10s: () -> Unit,
  onForward10s: () -> Unit,
  onPrevTrack: () -> Unit = {},
  onNextTrack: () -> Unit = {},
  onSeek: (Long) -> Unit,
  onToggle8D: () -> Unit,
  onSelect8DSpeed: (Audio8DSpeed) -> Unit = {},
  onChange8DIntensity: (Int) -> Unit = {},
  onExpandApp: () -> Unit,
  onClose: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val track = playback.currentTrack
  val spec =
    remember(appThemeStyle, widgetColorMode, widgetGlassMode, widgetCustomOpacity) {
      resolveFloatingWidgetSpec(
        appThemeStyle = appThemeStyle,
        colorMode = widgetColorMode,
        glassMode = widgetGlassMode,
        customOpacity = widgetCustomOpacity,
      )
    }

  var show8DPanelInCapsule by remember { mutableStateOf(false) }
  var showQuickSettingsInCapsule by remember { mutableStateOf(false) }

  val buttonOpacityScale = spec.buttonOpacityScale

  val dragModifier =
    if (onDrag != null) {
      Modifier.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
          change.consume()
          onDrag(dragAmount.x, dragAmount.y)
        }
      }
    } else {
      Modifier
    }

  val capsuleShape = RoundedCornerShape(34.dp)

  CompositionLocalProvider(LocalMPlayerPalette provides spec.widgetPalette) {
    Surface(
      modifier = Modifier.fillMaxWidth().padding(4.dp).then(dragModifier),
      shape = capsuleShape,
      color = spec.containerColor,
      border = BorderStroke(1.dp, spec.borderBrush),
      shadowElevation = 0.dp,
      tonalElevation = 0.dp,
    ) {
      Box(modifier = Modifier.background(spec.capsuleBackdropBrush)) {
        // Real internal frosted glass diffusion layer (RenderEffect blur on API 31+)
        if (spec.innerRefractionAlpha > 0f) {
          Box(
            modifier =
              Modifier.matchParentSize()
                .then(
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(22.dp)
                  else Modifier
                )
                .background(
                  Brush.linearGradient(
                    listOf(
                      Color.White.copy(alpha = spec.innerRefractionAlpha * 0.55f),
                      Color.Transparent,
                      spec.topSpecularColor.copy(alpha = spec.innerRefractionAlpha * 0.45f),
                      Color.Transparent,
                    )
                  )
                )
          )
        }

        // Tactile frosted micro-noise grain layer (diffuses flat tint into physical mineral frost)
        if (spec.frostGrainAlpha > 0f) {
          FrostedGlassNoiseOverlay(
            grainAlpha = spec.frostGrainAlpha,
            isPitchBlackGold = spec.isPitchBlackGold,
            modifier = Modifier.matchParentSize(),
          )
        }

        // Subtle inner edge rim reflection inside the crisp 1.dp metallic/glass outer border
        if (spec.effectiveOpacity > 0.04f) {
          Box(
            modifier =
              Modifier.matchParentSize()
                .padding(1.dp)
                .border(
                  0.6.dp,
                  Brush.verticalGradient(
                    listOf(
                      Color.White.copy(
                        alpha =
                          if (spec.isPitchBlackGold) 0.10f
                          else spec.topSpecularAlpha * 0.28f
                      ),
                      Color.Transparent,
                      Color.White.copy(alpha = 0.04f),
                    )
                  ),
                  RoundedCornerShape(33.dp),
                )
          )
        }

        // Crisp 1.dp top specular glass highlight
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(1.dp)
              .align(Alignment.TopCenter)
              .background(
                Brush.horizontalGradient(
                  listOf(
                    Color.Transparent,
                    spec.topSpecularColor.copy(alpha = spec.topSpecularAlpha),
                    Color.White.copy(
                      alpha =
                        if (spec.isPitchBlackGold) 0.10f
                        else spec.topSpecularAlpha * 0.46f
                    ),
                    spec.topSpecularColor.copy(alpha = spec.topSpecularAlpha),
                    Color.Transparent,
                  )
                )
              )
        )

        Column(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
          // Top capsule drag indicator + 8D Frosted Pill + Tune Drawer + Quick Settings + Expand / Close
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                  Modifier.clip(RoundedCornerShape(50))
                    .background(
                      if (playback.is8DEnabled) {
                        if (spec.isPitchBlackGold) Color(0xFF0A0A0A).copy(alpha = maxOf(spec.elevatedSurfaceAlpha, 0.25f))
                        else spec.accentTrackColor.copy(alpha = maxOf(spec.elevatedSurfaceAlpha * 0.65f, 0.16f))
                      } else {
                        spec.elevatedPanelColor.copy(alpha = maxOf(spec.elevatedSurfaceAlpha * 0.72f, 0.12f))
                      }
                    )
                    .border(
                      1.dp,
                      if (playback.is8DEnabled) spec.accentIndicatorColor.copy(alpha = spec.borderAlpha)
                      else spec.borderColor.copy(alpha = spec.borderAlpha * 0.48f),
                      RoundedCornerShape(50),
                    )
                    .clickable(onClick = onToggle8D)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("overlay_8d_toggle"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.GraphicEq,
                  contentDescription = strings.audio8DTitle,
                  tint = if (playback.is8DEnabled) spec.accentIndicatorColor else spec.textPrimary,
                  modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                  text = if (playback.is8DEnabled) strings.audio8DOn else strings.audio8DOff,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (playback.is8DEnabled) spec.accentIndicatorColor else spec.textPrimary,
                )
              }

              Spacer(modifier = Modifier.width(6.dp))

              Box(
                modifier =
                  Modifier.size(30.dp)
                    .clip(CircleShape)
                    .background(
                      if (show8DPanelInCapsule) {
                        if (spec.isPitchBlackGold) Color(0xFF0A0A0A)
                        else spec.accentTrackColor.copy(alpha = 0.45f)
                      } else {
                        spec.elevatedPanelColor.copy(alpha = maxOf(spec.elevatedSurfaceAlpha * 0.72f, 0.12f))
                      }
                    )
                    .border(
                      1.dp,
                      if (show8DPanelInCapsule) spec.accentIndicatorColor
                      else spec.borderColor.copy(alpha = spec.borderAlpha * 0.55f),
                      CircleShape,
                    )
                    .clickable {
                      show8DPanelInCapsule = !show8DPanelInCapsule
                      if (show8DPanelInCapsule) showQuickSettingsInCapsule = false
                    }
                    .testTag("overlay_8d_panel_btn"),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Tune,
                  contentDescription = strings.audio8DPanelTitle,
                  tint = if (show8DPanelInCapsule) spec.accentIndicatorColor else spec.textPrimary,
                  modifier = Modifier.size(16.dp),
                )
              }
            }

            // Translucent frosted drag handle pill
            Box(
              modifier =
                Modifier.width(32.dp)
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(spec.accentIndicatorColor.copy(alpha = 0.52f))
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
              // Dedicated Inline Quick Settings Toggle Button
              Box(
                modifier =
                  Modifier.size(30.dp)
                    .clip(CircleShape)
                    .background(
                      if (showQuickSettingsInCapsule) {
                        if (spec.isPitchBlackGold) Color(0xFF0A0A0A)
                        else spec.accentTrackColor.copy(alpha = 0.45f)
                      } else {
                        spec.elevatedPanelColor.copy(alpha = maxOf(spec.elevatedSurfaceAlpha * 0.72f, 0.12f))
                      }
                    )
                    .border(
                      1.dp,
                      if (showQuickSettingsInCapsule) spec.accentIndicatorColor
                      else spec.borderColor.copy(alpha = spec.borderAlpha * 0.55f),
                      CircleShape,
                    )
                    .clickable {
                      showQuickSettingsInCapsule = !showQuickSettingsInCapsule
                      if (showQuickSettingsInCapsule) show8DPanelInCapsule = false
                    }
                    .testTag("overlay_quick_settings_btn"),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Settings,
                  contentDescription = strings.widgetQuickSettingsTitle,
                  tint = if (showQuickSettingsInCapsule) spec.accentIndicatorColor else spec.textPrimary,
                  modifier = Modifier.size(16.dp),
                )
              }

              IconButton(
                onClick = onExpandApp,
                modifier = Modifier.size(34.dp).testTag("overlay_expand_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.OpenInFull,
                  contentDescription = strings.expandFullApp,
                  tint = spec.textPrimary,
                  modifier = Modifier.size(17.dp),
                )
              }
              IconButton(
                onClick = onClose,
                modifier = Modifier.size(34.dp).testTag("overlay_close_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Close,
                  contentDescription = strings.closeOverlay,
                  tint = spec.textSecondary,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Audio file cover art + Song Name + Artist
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier =
                Modifier.size(50.dp)
                  .clip(CircleShape)
                  .background(spec.elevatedPanelColor.copy(alpha = maxOf(spec.elevatedSurfaceAlpha, 0.14f)))
                  .border(1.dp, spec.borderBrush, CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              if (track?.albumArtUri != null) {
                AsyncImage(
                  model = track.albumArtUri,
                  contentDescription = track.title,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                Icon(
                  imageVector = Icons.Rounded.MusicNote,
                  contentDescription = strings.appName,
                  tint = spec.accentIndicatorColor,
                  modifier = Modifier.size(24.dp),
                )
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = track?.title ?: strings.noTrackPlaying,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = spec.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = track?.artist ?: strings.appName,
                style = MaterialTheme.typography.bodySmall,
                color = spec.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          // Progress slider & frosted glass playback controls strictly LTR:
          // Left = Rewind (-10s) / Previous Track, Center = Play/Pause, Right = Forward (+10s) / Next Track
          CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            val duration = playback.durationMs.coerceAtLeast(1L)
            val progress = (playback.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            Slider(
              value = progress,
              onValueChange = { fraction -> onSeek((fraction * duration).toLong()) },
              colors =
                SliderDefaults.colors(
                  thumbColor = spec.accentIndicatorColor,
                  activeTrackColor = spec.accentTrackColor,
                  inactiveTrackColor = spec.textSecondary.copy(alpha = 0.24f),
                ),
              modifier = Modifier.fillMaxWidth().height(26.dp),
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              if (playback.queue.size > 1) {
                GlassPlaybackControlButton(
                  onClick = onPrevTrack,
                  buttonSize = 40.dp,
                  surfaceOpacityScale = buttonOpacityScale,
                  modifier = Modifier.testTag("overlay_prev_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = spec.iconTint,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }

              GlassPlaybackControlButton(
                onClick = onRewind10s,
                buttonSize = 44.dp,
                surfaceOpacityScale = buttonOpacityScale,
                modifier = Modifier.testTag("overlay_rewind_10_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Replay10,
                  contentDescription = "Rewind 10 seconds",
                  tint = spec.iconTint,
                  modifier = Modifier.size(22.dp),
                )
              }

              GlassPlaybackControlButton(
                onClick = onPlayPause,
                buttonSize = 52.dp,
                isPrimary = true,
                surfaceOpacityScale = buttonOpacityScale,
                modifier = Modifier.testTag("overlay_play_pause_button"),
              ) {
                Icon(
                  imageVector = if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                  contentDescription = if (playback.isPlaying) "Pause" else "Play",
                  tint = spec.primaryButtonIconTint,
                  modifier = Modifier.size(28.dp),
                )
              }

              GlassPlaybackControlButton(
                onClick = onForward10s,
                buttonSize = 44.dp,
                surfaceOpacityScale = buttonOpacityScale,
                modifier = Modifier.testTag("overlay_forward_10_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Forward10,
                  contentDescription = "Forward 10 seconds",
                  tint = spec.iconTint,
                  modifier = Modifier.size(22.dp),
                )
              }

              if (playback.queue.size > 1) {
                GlassPlaybackControlButton(
                  onClick = onNextTrack,
                  buttonSize = 40.dp,
                  surfaceOpacityScale = buttonOpacityScale,
                  modifier = Modifier.testTag("overlay_next_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = spec.iconTint,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
            }
          }

          // Expandable Inline Quick Settings Panel (Decoupled Color vs. Glass Opacity & Blur)
          AnimatedVisibility(visible = showQuickSettingsInCapsule) {
            FloatingWidgetQuickSettingsPanel(
              spec = spec,
              appThemeStyle = appThemeStyle,
              selectedColorMode = widgetColorMode,
              selectedGlassMode = widgetGlassMode,
              customOpacity = widgetCustomOpacity,
              onSelectColorMode = onSelectWidgetColorMode,
              onSelectGlassMode = onSelectWidgetGlassMode,
              onChangeCustomOpacity = onChangeWidgetCustomOpacity,
            )
          }

          // Expandable Inline 8D Spatial Control Panel inside Floating Capsule
          AnimatedVisibility(visible = show8DPanelInCapsule) {
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .padding(top = 10.dp)
                  .clip(RoundedCornerShape(22.dp))
                  .background(spec.elevatedSurfaceColor.copy(alpha = maxOf(spec.elevatedSurfaceAlpha, 0.72f)))
                  .border(1.dp, spec.borderBrush, RoundedCornerShape(22.dp))
                  .padding(12.dp)
                  .testTag("overlay_8d_inline_panel")
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = strings.audio8DPanelTitle,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = spec.textPrimary,
                )
                Switch(
                  checked = playback.is8DEnabled,
                  onCheckedChange = { onToggle8D() },
                  colors =
                    SwitchDefaults.colors(
                      checkedThumbColor = spec.accentIndicatorColor,
                      checkedTrackColor =
                        if (spec.isPitchBlackGold) Color(0xFF0A0A0A)
                        else spec.elevatedSurfaceColor,
                      checkedBorderColor = spec.accentIndicatorColor,
                    ),
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                SpatialSoundfieldRadar(
                  isEnabled = playback.is8DEnabled,
                  isPlaying = playback.isPlaying,
                  speed = playback.audio8DSpeed,
                  intensityPercent = playback.audio8DIntensity,
                  panAngleDegrees = panAngleDegrees,
                  radarSize = 112.dp,
                  forceNeutralCapsuleStyle = spec.isNeutral,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = strings.audio8DSpeedTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = spec.textSecondary,
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Audio8DSpeedSelector(
                    selectedSpeed = playback.audio8DSpeed,
                    onSelectSpeed = onSelect8DSpeed,
                    forceNeutralCapsuleStyle = spec.isNeutral,
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = strings.audio8DIntensityTitle,
                      style = MaterialTheme.typography.labelSmall,
                      color = spec.textSecondary,
                    )
                    Text(
                      text = "${playback.audio8DIntensity}%",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = spec.accentIndicatorColor,
                    )
                  }
                  Audio8DIntensitySlider(
                    intensityPercent = playback.audio8DIntensity,
                    onChangeIntensity = onChange8DIntensity,
                    forceNeutralCapsuleStyle = spec.isNeutral,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FrostedGlassNoiseOverlay(
  grainAlpha: Float,
  isPitchBlackGold: Boolean,
  modifier: Modifier = Modifier,
) {
  val normalizedPoints =
    remember {
      val list = ArrayList<Pair<Float, Float>>(240)
      var seed = 0x5EED97u
      for (i in 0 until 240) {
        seed = seed * 1664525u + 1013904223u
        val x = ((seed shr 16) and 0xFFFFu).toFloat() / 65535f
        seed = seed * 1664525u + 1013904223u
        val y = ((seed shr 16) and 0xFFFFu).toFloat() / 65535f
        list.add(x to y)
      }
      list
    }

  Canvas(modifier = modifier) {
    val lightPoints = ArrayList<Offset>(120)
    val darkPoints = ArrayList<Offset>(120)
    normalizedPoints.forEachIndexed { idx, (nx, ny) ->
      val pt = Offset(nx * size.width, ny * size.height)
      if (idx % 2 == 0) lightPoints.add(pt) else darkPoints.add(pt)
    }
    val tint =
      if (isPitchBlackGold) Color.White.copy(alpha = grainAlpha * 0.72f)
      else Color.White.copy(alpha = grainAlpha)
    drawPoints(
      points = lightPoints,
      pointMode = PointMode.Points,
      color = tint,
      strokeWidth = 2f,
    )
    drawPoints(
      points = darkPoints,
      pointMode = PointMode.Points,
      color = Color.Black.copy(alpha = grainAlpha * 0.65f),
      strokeWidth = 2f,
    )
  }
}
