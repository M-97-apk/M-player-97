package com.example.ui.components

import android.os.Build
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.localization.LocalAppStrings
import com.example.model.AppThemeStyle
import com.example.model.RepeatMode
import com.example.playback.PlaybackUiState
import com.example.playback.computeSeekTargetMs
import com.example.ui.theme.LocalMPlayerPalette
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * Purely in-app cinematic brightness overlay scoped exclusively to the video playback frame.
 * Never touches device global system settings.
 */
@Composable
fun VideoSurfaceBrightnessOverlay(
  brightness: Float,
  modifier: Modifier = Modifier,
) {
  if (brightness !in 0.05f..1.0f) return

  val overlayColor =
    if (brightness < 0.65f) {
      val dimAlpha = ((0.65f - brightness) / 0.60f * 0.72f).coerceIn(0f, 0.72f)
      Color.Black.copy(alpha = dimAlpha)
    } else if (brightness > 0.65f) {
      val boostAlpha = ((brightness - 0.65f) / 0.35f * 0.14f).coerceIn(0f, 0.14f)
      Color.White.copy(alpha = boostAlpha)
    } else {
      Color.Transparent
    }

  if (overlayColor.alpha > 0f) {
    Box(modifier = modifier.background(overlayColor))
  }
}

/**
 * Single clean frosted-glass Fullscreen / Expand trigger button displayed in the main video view.
 */
@Composable
fun VideoFullscreenExpandButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  val shape = RoundedCornerShape(50)

  Row(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .clip(shape)
        .background(palette.glassBadgeFillBrush)
        .border(palette.borderStrokeWidth, palette.borderBrush, shape)
        .clickable(onClick = onClick)
        .padding(horizontal = 20.dp, vertical = 11.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = Icons.Rounded.Fullscreen,
      contentDescription = strings.videoFullscreenExpand,
      tint = palette.accentGlow,
      modifier = Modifier.size(21.dp),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = strings.videoFullscreenExpand,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
      color = palette.textPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun FrostedGlassControlPill(
  isLandscape: Boolean,
  onToggleRotateLandscape: () -> Unit,
  onLockScreen: () -> Unit,
  onExitFullscreen: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  val barShape = RoundedCornerShape(20.dp)

  Box(
    modifier =
      modifier
        .clip(barShape)
        .background(palette.sheetBg.copy(alpha = 0.72f))
        .background(palette.glassBadgeFillBrush)
        .border(palette.borderStrokeWidth, palette.borderBrush, barShape)
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      Box(
        modifier =
          Modifier.matchParentSize()
            .blur(18.dp)
            .background(
              if (palette.themeStyle == AppThemeStyle.PITCH_BLACK_GOLD) {
                Brush.horizontalGradient(
                  listOf(Color(0xB8080808), Color(0xCC040404), Color(0xB8080808))
                )
              } else {
                Brush.horizontalGradient(
                  listOf(
                    palette.accentPrimary.copy(alpha = 0.16f),
                    palette.accentGlow.copy(alpha = 0.08f),
                    palette.accentPrimary.copy(alpha = 0.16f),
                  )
                )
              }
            )
      )
    }

    // Crisp 1px top specular highlight edge
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(1.dp)
          .align(Alignment.TopCenter)
          .background(
            Brush.horizontalGradient(
              listOf(
                Color.Transparent,
                palette.accentGlow.copy(alpha = 0.65f),
                Color.White.copy(alpha = 0.16f),
                palette.accentGlow.copy(alpha = 0.65f),
                Color.Transparent,
              )
            )
          )
    )

    Row(
      modifier =
        Modifier.horizontalScroll(rememberScrollState())
          .padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      VideoToolPillButton(
        icon = Icons.Rounded.ScreenRotation,
        label = if (isLandscape) strings.videoRotatePortrait else strings.videoRotateLandscape,
        selected = isLandscape,
        onClick = onToggleRotateLandscape,
        modifier = Modifier.testTag("theater_rotate_btn"),
      )

      VideoToolPillButton(
        icon = Icons.Rounded.LockOpen,
        label = strings.videoScreenLock,
        selected = false,
        onClick = onLockScreen,
        modifier = Modifier.testTag("theater_lock_btn"),
      )

      VideoToolPillButton(
        icon = Icons.Rounded.FullscreenExit,
        label = strings.videoFullscreenExit,
        selected = false,
        onClick = onExitFullscreen,
        modifier = Modifier.testTag("theater_exit_fullscreen_btn"),
      )
    }
  }
}

@Composable
private fun VideoToolPillButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  val pillShape = RoundedCornerShape(13.dp)
  Row(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .activeGlassOptionPillDecoration(selected = selected, shape = pillShape)
        .then(if (!selected) Modifier.clip(pillShape) else Modifier)
        .clickable(onClick = onClick)
        .padding(horizontal = 11.dp, vertical = 9.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (selected) palette.accentGlow else palette.textPrimary,
      modifier = Modifier.size(18.dp),
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      color = if (selected) palette.accentGlow else palette.textPrimary,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/**
 * Fullscreen video theater overlay launched when the user taps Fullscreen/Expand, rotates to Landscape,
 * or activates Smart Screen Lock.
 *
 * - Displays an auto-hiding frosted-glass control bar containing Landscape Rotation,
 *   Screen Lock, Exit Fullscreen, and Audio-Only playback switch alongside transport controls.
 * - Automatically fades/slides out after inactivity; tapping anywhere on screen brings the glass bar
 *   back temporarily.
 * - When [isLocked] is true, all toolbars stay hidden and full-screen touches are intercepted,
 *   showing only a 3-second auto-hiding Padlock button on tap.
 */
@Composable
fun VideoTheaterOverlay(
  playback: PlaybackUiState,
  isLandscape: Boolean,
  isLocked: Boolean,
  onToggleLandscape: () -> Unit,
  onSetLocked: (Boolean) -> Unit,
  onSetVideoAudioOnlyMode: (Boolean) -> Unit,
  onPlayPause: () -> Unit,
  onNext: () -> Unit,
  onPrevious: () -> Unit,
  onSeek: (Long) -> Unit,
  onToggleShuffle: () -> Unit,
  onCycleRepeat: () -> Unit,
  onAttachVideoTextureView: (TextureView) -> Unit,
  onDetachVideoTextureView: (TextureView) -> Unit,
  onExitTheater: () -> Unit,
  seekStepMs: Long = 10_000L,
  autoHideDelayMs: Long = 3_500L,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  val track = playback.currentTrack

  var showControls by remember(isLocked) { mutableStateOf(!isLocked) }
  var hudInteractionTick by remember { mutableIntStateOf(0) }

  // Double-tap seek visual indicator state
  var doubleTapFeedbackTick by remember { mutableIntStateOf(0) }
  var doubleTapFeedbackForward by remember { mutableStateOf(true) }
  var doubleTapFeedbackStepMs by remember { mutableLongStateOf(seekStepMs) }
  var showDoubleTapFeedback by remember { mutableStateOf(false) }

  // Long-press + horizontal drag continuous scrub state
  var isLongPressScrubbing by remember { mutableStateOf(false) }
  var scrubStartPosMs by remember { mutableLongStateOf(0L) }
  var scrubTargetPosMs by remember { mutableLongStateOf(0L) }
  var scrubDurationMs by remember { mutableLongStateOf(0L) }
  var surfaceWidthPx by remember { mutableIntStateOf(1) }

  val latestPlayback by rememberUpdatedState(playback)
  val latestSeekStepMs by rememberUpdatedState(seekStepMs.coerceAtLeast(1_000L))
  val latestOnSeek by rememberUpdatedState(onSeek)
  val latestShowControls by rememberUpdatedState(showControls)

  // Padlock auto-hide timer state when Screen Lock is ON
  var isPadlockVisible by remember { mutableStateOf(false) }
  var lockRevealTick by remember { mutableIntStateOf(0) }

  LaunchedEffect(doubleTapFeedbackTick) {
    if (doubleTapFeedbackTick > 0) {
      showDoubleTapFeedback = true
      delay(850L)
      showDoubleTapFeedback = false
    }
  }

  LaunchedEffect(isLocked) {
    if (isLocked) {
      showControls = false
      isPadlockVisible = true
      lockRevealTick++
    } else {
      isPadlockVisible = false
      showControls = true
      hudInteractionTick++
    }
  }

  // Auto-hide the frosted-glass control bar after inactivity for uninterrupted viewing
  LaunchedEffect(showControls, isLocked, hudInteractionTick) {
    if (!isLocked && showControls) {
      delay(autoHideDelayMs)
      showControls = false
    }
  }

  // Re-hide padlock automatically 3 seconds after lock activation or last tap
  LaunchedEffect(isLocked, lockRevealTick) {
    if (isLocked && isPadlockVisible) {
      delay(3_000L)
      isPadlockVisible = false
    }
  }

  // Intercept system Back while locked: reveal padlock for 3s without interrupting playback
  BackHandler(enabled = isLocked) {
    isPadlockVisible = true
    lockRevealTick++
  }

  BackHandler(enabled = !isLocked) {
    onExitTheater()
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color.Black)
        .testTag("video_theater_overlay")
  ) {
    // 1. Hardware TextureView fitted cleanly preserving aspect ratio + strictly in-app brightness scrim
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      val targetAspect = playback.videoAspectRatio.coerceIn(0.4f, 2.5f)
      val boxAspect = if (maxHeight > 0.dp) maxWidth / maxHeight else 16f / 9f
      val surfaceMod =
        if (boxAspect > targetAspect) {
          Modifier.fillMaxHeight().aspectRatio(targetAspect)
        } else {
          Modifier.fillMaxWidth().aspectRatio(targetAspect)
        }

      Box(modifier = surfaceMod) {
        AndroidView(
          factory = { ctx ->
            TextureView(ctx).also { tv -> onAttachVideoTextureView(tv) }
          },
          update = { tv -> onAttachVideoTextureView(tv) },
          onRelease = { tv -> onDetachVideoTextureView(tv) },
          modifier = Modifier.fillMaxSize().testTag("theater_video_surface"),
        )
      }
    }

    // 2. Unified Gesture Surface (Single Tap -> Controls | Double Tap Right/Left -> Step Seek | Long-Press + Drag -> Continuous Scrub)
    if (!isLocked) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .onSizeChanged { surfaceWidthPx = it.width.coerceAtLeast(1) }
            .pointerInput(isLocked) {
              awaitEachGesture {
                val firstDown = awaitFirstDown(requireUnconsumed = false)
                val touchSlop = viewConfiguration.touchSlop
                val longPressMs = viewConfiguration.longPressTimeoutMillis
                val doubleTapMs = viewConfiguration.doubleTapTimeoutMillis
                val doubleTapSlop = 48.dp.toPx()

                var firstUpPos: androidx.compose.ui.geometry.Offset? = null
                var movedBeforeLongPress = false

                val waitBeforeLongPress =
                  withTimeoutOrNull(longPressMs) {
                    while (true) {
                      val event = awaitPointerEvent()
                      val active = event.changes.firstOrNull { it.id == firstDown.id }
                      if (active == null || !active.pressed) {
                        firstUpPos = active?.position ?: firstDown.position
                        return@withTimeoutOrNull true
                      }
                      if (hypot(active.position.x - firstDown.position.x, active.position.y - firstDown.position.y) > touchSlop) {
                        movedBeforeLongPress = true
                        return@withTimeoutOrNull false
                      }
                    }
                  }

                if (waitBeforeLongPress == null && !movedBeforeLongPress) {
                  // Long-press activated -> Continuous Horizontal Scrubbing independent of double-tap step
                  val anchorPos = latestPlayback.positionMs.coerceAtLeast(0L)
                  val totalDur = latestPlayback.durationMs.coerceAtLeast(0L)
                  val maxSpanMs =
                    if (totalDur > 0L) totalDur.coerceIn(30_000L, 240_000L) else 90_000L
                  var currentTarget = anchorPos
                  var lastDispatchedSeek = anchorPos

                  scrubStartPosMs = anchorPos
                  scrubTargetPosMs = anchorPos
                  scrubDurationMs = totalDur
                  isLongPressScrubbing = true

                  while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == firstDown.id }
                    if (change == null || !change.pressed) {
                      break
                    }
                    change.consume()
                    val dx = change.position.x - firstDown.position.x
                    val fraction = dx / surfaceWidthPx.toFloat().coerceAtLeast(300f)
                    val deltaMs = (fraction * maxSpanMs).toLong()
                    currentTarget = computeSeekTargetMs(anchorPos, totalDur, deltaMs)
                    scrubTargetPosMs = currentTarget
                    if (abs(currentTarget - lastDispatchedSeek) >= 400L) {
                      lastDispatchedSeek = currentTarget
                      latestOnSeek(currentTarget)
                    }
                  }

                  latestOnSeek(currentTarget)
                  isLongPressScrubbing = false
                  return@awaitEachGesture
                }

                if (waitBeforeLongPress == true && firstUpPos != null) {
                  val prevShowControls = latestShowControls
                  if (prevShowControls) {
                    showControls = false
                  } else {
                    showControls = true
                    hudInteractionTick++
                  }

                  // Check briefly for a second tap to upgrade to Double-Tap Seek
                  var secondTapUpPos: androidx.compose.ui.geometry.Offset? = null
                  withTimeoutOrNull(doubleTapMs) {
                    val secondDown = awaitFirstDown(requireUnconsumed = false)
                    val dist = hypot(
                      secondDown.position.x - firstUpPos!!.x,
                      secondDown.position.y - firstUpPos!!.y,
                    )
                    if (dist <= doubleTapSlop) {
                      while (true) {
                        val ev2 = awaitPointerEvent()
                        val ch2 = ev2.changes.firstOrNull { it.id == secondDown.id }
                        if (ch2 == null || !ch2.pressed) {
                          secondTapUpPos = ch2?.position ?: secondDown.position
                          break
                        }
                      }
                    }
                  }

                  if (secondTapUpPos != null) {
                    showControls = prevShowControls
                    if (prevShowControls) hudInteractionTick++
                    val isRightHalf = secondTapUpPos!!.x >= (surfaceWidthPx / 2f)
                    val stepMs = latestSeekStepMs
                    val deltaMs = if (isRightHalf) stepMs else -stepMs
                    val target =
                      computeSeekTargetMs(
                        currentPosMs = latestPlayback.positionMs,
                        durationMs = latestPlayback.durationMs,
                        deltaMs = deltaMs,
                      )
                    latestOnSeek(target)
                    doubleTapFeedbackForward = isRightHalf
                    doubleTapFeedbackStepMs = stepMs
                    doubleTapFeedbackTick++
                  }
                } else if (movedBeforeLongPress) {
                  // Drain remaining pointer movement cleanly until release
                  while (true) {
                    val ev = awaitPointerEvent()
                    if (ev.changes.none { it.pressed }) break
                  }
                }
              }
            }
            .testTag("theater_tap_surface")
      )
    }

    // 2b. Double-Tap Seek Feedback Badge (Left / Right visual indicator)
    AnimatedVisibility(
      visible = !isLocked && showDoubleTapFeedback,
      enter = fadeIn(tween(120)),
      exit = fadeOut(tween(240)),
      modifier =
        Modifier.align(if (doubleTapFeedbackForward) Alignment.CenterEnd else Alignment.CenterStart)
          .padding(horizontal = 36.dp),
    ) {
      val stepLabel = formatSeekStepCompact(doubleTapFeedbackStepMs, strings)
      val prefix = if (doubleTapFeedbackForward) "+" else "-"
      Row(
        modifier =
          Modifier.clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.72f))
            .border(1.dp, palette.buttonBorderColor, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("theater_double_tap_seek_badge"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector =
            if (doubleTapFeedbackForward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
          contentDescription = null,
          tint = palette.accentGlow,
          modifier = Modifier.size(22.dp),
        )
        Text(
          text = "$prefix$stepLabel",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White,
        )
      }
    }

    // 2c. Long-Press Continuous Scrub Time Indicator Overlay
    AnimatedVisibility(
      visible = !isLocked && isLongPressScrubbing,
      enter = fadeIn(tween(100)),
      exit = fadeOut(tween(150)),
      modifier = Modifier.align(Alignment.Center),
    ) {
      val deltaMs = scrubTargetPosMs - scrubStartPosMs
      val deltaAbsFormatted = formatDuration(abs(deltaMs))
      val deltaSign = if (deltaMs >= 0L) "+" else "-"
      val scrubShape = RoundedCornerShape(20.dp)
      Column(
        modifier =
          Modifier.clip(scrubShape)
            .background(Color.Black.copy(alpha = 0.78f))
            .border(1.dp, palette.buttonBorderColor, scrubShape)
            .padding(horizontal = 22.dp, vertical = 12.dp)
            .testTag("theater_scrub_time_indicator"),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text =
            if (scrubDurationMs > 0L) {
              "${formatDuration(scrubTargetPosMs)} / ${formatDuration(scrubDurationMs)}"
            } else {
              formatDuration(scrubTargetPosMs)
            },
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "$deltaSign$deltaAbsFormatted",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = palette.accentGlow,
        )
      }
    }

    // 3. Auto-Hiding Frosted-Glass Control Bars (Header + Floating Glass Tools Bar + Transport)
    AnimatedVisibility(
      visible = !isLocked && showControls,
      enter = fadeIn(tween(240)),
      exit = fadeOut(tween(260)),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(
                  Color.Black.copy(alpha = 0.62f),
                  Color.Transparent,
                  Color.Black.copy(alpha = 0.70f),
                )
              )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        // Top Frosted Header (Exit Fullscreen / Title / Audio-Only Pill)
        AnimatedVisibility(
          visible = !isLocked && showControls,
          enter = fadeIn(tween(240)) + slideInVertically(tween(240)) { -it / 2 },
          exit = fadeOut(tween(240)) + slideOutVertically(tween(240)) { -it / 2 },
          modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
        ) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.sheetBg.copy(alpha = 0.68f))
                .background(palette.glassBadgeFillBrush)
                .border(palette.borderStrokeWidth, palette.borderBrush, RoundedCornerShape(18.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              ThemedIconButton(
                onClick = onExitTheater,
                buttonSize = 44.dp,
                modifier = Modifier.testTag("theater_back_btn"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = strings.videoFullscreenExit,
                  tint = palette.textPrimary,
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = track?.title.orEmpty(),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = palette.textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = track?.artist.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            Spacer(modifier = Modifier.width(8.dp))

            VideoToolPillButton(
              icon = Icons.Rounded.Headphones,
              label = strings.audioOnlyModePill,
              selected = playback.isVideoAudioOnly,
              onClick = {
                hudInteractionTick++
                onSetVideoAudioOnlyMode(true)
              },
              modifier = Modifier.testTag("theater_audio_only_btn"),
            )
          }
        }

        // Bottom Frosted-Glass Stack (Auto-Hiding Glass Control Bar + Transport)
        AnimatedVisibility(
          visible = !isLocked && showControls,
          enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 2 },
          exit = fadeOut(tween(260)) + slideOutVertically(tween(260)) { it / 2 },
          modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            FrostedGlassControlPill(
              isLandscape = isLandscape,
              onToggleRotateLandscape = {
                hudInteractionTick++
                onToggleLandscape()
              },
              onLockScreen = {
                onSetLocked(true)
              },
              onExitFullscreen = onExitTheater,
              modifier = Modifier.widthIn(max = 620.dp).testTag("fullscreen_glass_control_bar"),
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Transport & Seek Bar in matching Frosted Glass
            val duration = playback.durationMs.coerceAtLeast(1L)
            val progress = (playback.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
              Column(
                modifier =
                  Modifier.fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(palette.sheetBg.copy(alpha = 0.72f))
                    .background(palette.glassBadgeFillBrush)
                    .border(palette.borderStrokeWidth, palette.borderBrush, RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = formatDuration(playback.positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textPrimary,
                  )
                  Slider(
                    value = progress,
                    onValueChange = { fraction ->
                      hudInteractionTick++
                      onSeek((fraction * duration).toLong())
                    },
                    colors =
                      SliderDefaults.colors(
                        thumbColor = palette.accentGlow,
                        activeTrackColor = palette.accentPrimary,
                        inactiveTrackColor = palette.textSecondary.copy(alpha = 0.28f),
                      ),
                    modifier =
                      Modifier.weight(1f).padding(horizontal = 10.dp).testTag("theater_seek_slider"),
                  )
                  Text(
                    text = formatDuration(playback.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textPrimary,
                  )
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceEvenly,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  GlassPlaybackControlButton(
                    onClick = {
                      hudInteractionTick++
                      onToggleShuffle()
                    },
                    buttonSize = 44.dp,
                    isActive = playback.shuffleEnabled,
                    modifier = Modifier.testTag("theater_shuffle_btn"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Shuffle,
                      contentDescription = strings.shuffleAll,
                      tint = if (playback.shuffleEnabled) palette.accentGlow else palette.textPrimary,
                      modifier = Modifier.size(20.dp),
                    )
                  }

                  GlassPlaybackControlButton(
                    onClick = {
                      hudInteractionTick++
                      onPrevious()
                    },
                    buttonSize = 48.dp,
                    modifier = Modifier.testTag("theater_prev_btn"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.SkipPrevious,
                      contentDescription = "Previous",
                      tint = palette.textPrimary,
                      modifier = Modifier.size(26.dp),
                    )
                  }

                  GlassPlaybackControlButton(
                    onClick = {
                      hudInteractionTick++
                      onPlayPause()
                    },
                    buttonSize = 58.dp,
                    isPrimary = true,
                    modifier = Modifier.testTag("theater_play_pause_btn"),
                  ) {
                    Icon(
                      imageVector =
                        if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                      contentDescription = if (playback.isPlaying) "Pause" else "Play",
                      tint = palette.buttonContentColor,
                      modifier = Modifier.size(30.dp),
                    )
                  }

                  GlassPlaybackControlButton(
                    onClick = {
                      hudInteractionTick++
                      onNext()
                    },
                    buttonSize = 48.dp,
                    modifier = Modifier.testTag("theater_next_btn"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.SkipNext,
                      contentDescription = "Next",
                      tint = palette.textPrimary,
                      modifier = Modifier.size(26.dp),
                    )
                  }

                  GlassPlaybackControlButton(
                    onClick = {
                      hudInteractionTick++
                      onCycleRepeat()
                    },
                    buttonSize = 44.dp,
                    isActive = playback.repeatMode != RepeatMode.OFF,
                    modifier = Modifier.testTag("theater_repeat_btn"),
                  ) {
                    Icon(
                      imageVector =
                        if (playback.repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne
                        else Icons.Rounded.Repeat,
                      contentDescription = "Repeat",
                      tint =
                        if (playback.repeatMode != RepeatMode.OFF) palette.accentGlow
                        else palette.textPrimary,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // 4. Full-Screen Touch Blocker + 3-Second Self-Hiding Padlock Button when Locked
    if (isLocked) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .pointerInput(Unit) {
              detectTapGestures(
                onPress = {
                  tryAwaitRelease()
                  isPadlockVisible = true
                  lockRevealTick++
                }
              )
            }
            .testTag("video_locked_touch_blocker")
      ) {
        AnimatedVisibility(
          visible = isPadlockVisible,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier.align(Alignment.TopStart).padding(top = 28.dp, start = 20.dp),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(52.dp)
                .clip(CircleShape)
                .background(palette.sheetBg.copy(alpha = 0.88f))
                .background(palette.glassBadgeActiveFillBrush)
                .border(palette.borderStrokeWidth, palette.buttonBorderColor, CircleShape)
                .clickable { onSetLocked(false) }
                .testTag("video_unlock_padlock_btn"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Lock,
              contentDescription = strings.videoScreenUnlock,
              tint = palette.accentGlow,
              modifier = Modifier.size(24.dp),
            )
          }
        }
      }
    }
  }
}

private fun formatSeekStepCompact(stepMs: Long, strings: com.example.localization.AppStrings): String {
  val totalSec = (stepMs / 1000L).coerceAtLeast(1L).toInt()
  return strings.videoSeekPresetFormat(totalSec)
}
