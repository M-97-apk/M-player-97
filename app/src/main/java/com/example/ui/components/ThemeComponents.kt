package com.example.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.localization.LocalAppStrings
import com.example.model.AppThemeStyle
import com.example.model.AudioTrack
import com.example.model.PlaylistSummary
import com.example.playback.PlaybackUiState
import com.example.ui.theme.LocalMPlayerPalette
import com.example.ui.theme.MetallicGoldBright
import com.example.ui.theme.MetallicGoldDark
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

fun formatDuration(ms: Long): String {
  val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
  val hours = totalSeconds / 3600L
  val minutes = (totalSeconds % 3600L) / 60L
  val seconds = totalSeconds % 60L
  return if (hours > 0L) {
    String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
  } else {
    String.format(Locale.US, "%d:%02d", minutes, seconds)
  }
}

@Composable
fun ThemedBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val palette = LocalMPlayerPalette.current
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom)))
  ) {
    // Rich, high-saturation blurred ambient glow behind glass panels when theme uses colored ambient orbs
    if (palette.isGlassTheme && !palette.isMinimalPlainBlack && palette.glowOrbPrimary != Color.Transparent) {
      val blurModifier =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          Modifier.fillMaxSize().blur(56.dp)
        } else {
          Modifier.fillMaxSize()
        }
      Canvas(modifier = blurModifier) {
        // Top-left primary depth bloom behind translucent cards
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(palette.glowOrbPrimary.copy(alpha = 0.48f), Color.Transparent),
              center = Offset(size.width * 0.16f, size.height * 0.14f),
              radius = size.width * 0.68f,
            ),
          radius = size.width * 0.68f,
          center = Offset(size.width * 0.16f, size.height * 0.14f),
        )
        // Mid-screen depth aura behind glass cards
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(palette.accentPrimary.copy(alpha = 0.30f), Color.Transparent),
              center = Offset(size.width * 0.76f, size.height * 0.44f),
              radius = size.width * 0.64f,
            ),
          radius = size.width * 0.64f,
          center = Offset(size.width * 0.76f, size.height * 0.44f),
        )
        // Lower-edge luminescence behind bottom navigation & mini player glass
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(palette.glowOrbSecondary.copy(alpha = 0.52f), Color.Transparent),
              center = Offset(size.width * 0.28f, size.height * 0.86f),
              radius = size.width * 0.74f,
            ),
          radius = size.width * 0.74f,
          center = Offset(size.width * 0.28f, size.height * 0.86f),
        )
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(palette.glowOrbSecondary.copy(alpha = 0.44f), Color.Transparent),
              center = Offset(size.width * 0.82f, size.height * 0.92f),
              radius = size.width * 0.68f,
            ),
          radius = size.width * 0.68f,
          center = Offset(size.width * 0.82f, size.height * 0.92f),
        )
      }
    }

    // Clean static background for Cosmic Starfield theme (crisp, dense, bright stars with subtle diamond twinkles)
    if (palette.themeStyle == AppThemeStyle.COSMIC_STARFIELD) {
      val cosmicStars =
        remember {
          val list = ArrayList<CosmicStarSeed>(188)
          var seed = 0x97A57A8u
          for (i in 0 until 188) {
            seed = seed * 1664525u + 1013904223u
            val nx = 0.02f + (((seed shr 16) and 0xFFFFu).toFloat() / 65535f) * 0.96f
            seed = seed * 1664525u + 1013904223u
            val ny = 0.015f + (((seed shr 16) and 0xFFFFu).toFloat() / 65535f) * 0.97f
            seed = seed * 1664525u + 1013904223u
            val rIdx = (seed shr 16) and 0xFu
            val radiusDp =
              when (rIdx.toInt()) {
                0, 1 -> 0.75f
                2, 3 -> 0.9f
                4, 5 -> 1.1f
                6, 7 -> 1.35f
                8, 9 -> 1.6f
                10 -> 1.9f
                11 -> 2.2f
                12 -> 2.5f
                else -> 1.0f
              }
            seed = seed * 1664525u + 1013904223u
            val baseAlpha = 0.44f + (((seed shr 16) and 0xFFFFu).toFloat() / 65535f) * 0.52f
            seed = seed * 1664525u + 1013904223u
            val tintType = ((seed shr 16) and 0x3u).toInt()
            val isDiamond = (i % 11 == 0) && radiusDp >= 1.6f
            seed = seed * 1664525u + 1013904223u
            val speedMul = 0.75f + (((seed shr 16) and 0xFFFFu).toFloat() / 65535f) * 0.55f
            list.add(CosmicStarSeed(nx, ny, radiusDp, baseAlpha, tintType, isDiamond, speedMul))
          }
          list
        }

      val infiniteStarTransition = rememberInfiniteTransition(label = "star_twinkle_transition")
      val starTwinklePhase by
        infiniteStarTransition.animateFloat(
          initialValue = 0f,
          targetValue = (2f * PI).toFloat(),
          animationSpec =
            infiniteRepeatable(
              animation = tween(4200, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "cosmic_stars_twinkle",
        )

      Canvas(modifier = Modifier.fillMaxSize().testTag("cosmic_starfield_bg_canvas")) {
        cosmicStars.forEachIndexed { i, star ->
          val twinkleOffset = (i * 0.53f) % (2f * PI).toFloat()
          val twinkleFactor = 0.82f + 0.18f * sin(starTwinklePhase * star.speedMultiplier + twinkleOffset)
          val alpha = (star.baseAlpha * twinkleFactor).coerceIn(0.35f, 0.98f)
          val color =
            when (star.tintType) {
              1 -> Color(0xFFE8F1FF).copy(alpha = alpha)
              2 -> Color(0xFFCEE2FF).copy(alpha = alpha)
              3 -> Color(0xFFFFF6E5).copy(alpha = alpha)
              else -> Color.White.copy(alpha = alpha)
            }
          val center = Offset(size.width * star.nx, size.height * star.ny)
          val radiusPx = star.radiusDp.dp.toPx()

          // Draw starlight core
          drawCircle(
            color = color,
            radius = radiusPx,
            center = center,
          )

          // Draw subtle 4-point diamond sparkle glint on bright prominent stars
          if (star.isDiamond && alpha > 0.60f) {
            val crossLen = radiusPx * 2.2f
            val crossStroke = 0.8.dp.toPx()
            val glintColor = color.copy(alpha = alpha * 0.72f)
            drawLine(
              color = glintColor,
              start = Offset(center.x - crossLen, center.y),
              end = Offset(center.x + crossLen, center.y),
              strokeWidth = crossStroke,
              cap = StrokeCap.Round,
            )
            drawLine(
              color = glintColor,
              start = Offset(center.x, center.y - crossLen),
              end = Offset(center.x, center.y + crossLen),
              strokeWidth = crossStroke,
              cap = StrokeCap.Round,
            )
          }
        }
      }
    }
    Box(modifier = Modifier.fillMaxSize(), content = content)
  }
}

private data class CosmicStarSeed(
  val nx: Float,
  val ny: Float,
  val radiusDp: Float,
  val baseAlpha: Float,
  val tintType: Int = 0,
  val isDiamond: Boolean = false,
  val speedMultiplier: Float = 1f,
)

@Composable
fun FrostedGlassGrainOverlay(
  grainAlpha: Float = 0.032f,
  isPitchBlackGold: Boolean = false,
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
      if (isPitchBlackGold) Color.White.copy(alpha = grainAlpha * 0.65f)
      else Color.White.copy(alpha = grainAlpha)
    drawPoints(
      points = lightPoints,
      pointMode = androidx.compose.ui.graphics.PointMode.Points,
      color = tint,
      strokeWidth = 2f,
    )
    drawPoints(
      points = darkPoints,
      pointMode = androidx.compose.ui.graphics.PointMode.Points,
      color = Color.Black.copy(alpha = grainAlpha * 0.65f),
      strokeWidth = 2f,
    )
  }
}

@Composable
fun ThemedCard(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 20.dp,
  elevated: Boolean = false,
  onClick: (() -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  val palette = LocalMPlayerPalette.current
  val shape = RoundedCornerShape(cornerRadius)
  val isPitchBlackGold = palette.themeStyle == AppThemeStyle.PITCH_BLACK_GOLD
  val isMinimalPlainBlack = palette.isMinimalPlainBlack
  val effectiveOpacity = if (isPitchBlackGold) 0.55f else palette.appGlassOpacity
  val opacityRatio = (effectiveOpacity / 0.55f).coerceIn(0.12f, 1.82f)
  val baseModifier =
    if (onClick != null) {
      modifier.clip(shape).clickable(onClick = onClick)
    } else {
      modifier
    }

  Surface(
    modifier = baseModifier,
    shape = shape,
    color = Color.Transparent,
    border = BorderStroke(palette.borderStrokeWidth, palette.borderBrush),
    shadowElevation = 0.dp,
    tonalElevation = 0.dp,
  ) {
    if (isMinimalPlainBlack) {
      Box(modifier = Modifier.background(Color.Black)) {
        content()
      }
    } else {
      val baseAlpha =
        if (isPitchBlackGold) {
          if (elevated) 0.56f else 0.42f
        } else {
          val targetAlpha = if (elevated) (0.50f * opacityRatio) else (0.38f * opacityRatio)
          targetAlpha.coerceIn(0.05f, 0.94f)
        }
      val frostBrush =
        if (isPitchBlackGold) {
          palette.glassCardFrostBrush
        } else {
          val frostAlphaScale = opacityRatio.coerceIn(0.15f, 1.6f)
          Brush.linearGradient(
            listOf(
              Color.White.copy(alpha = (0.07f * frostAlphaScale).coerceIn(0.008f, 0.16f)),
              palette.cardSurface.copy(alpha = (0.34f * frostAlphaScale).coerceIn(0.03f, 0.88f)),
              palette.backgroundBottom.copy(alpha = (0.46f * frostAlphaScale).coerceIn(0.04f, 0.92f)),
            )
          )
        }
      val sheenScale = minOf(opacityRatio, 1.25f)
      Box(
        modifier =
          Modifier.background(
            if (elevated) palette.cardSurfaceElevated.copy(alpha = baseAlpha)
            else palette.cardSurface.copy(alpha = baseAlpha)
          ).background(frostBrush)
      ) {
        // Internal blurred frosted glass diffusion layer (RenderEffect blur on API 31+)
        Box(
          modifier =
            Modifier.matchParentSize()
              .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(20.dp) else Modifier)
              .background(
                Brush.linearGradient(
                  listOf(
                    Color.White.copy(alpha = (if (isPitchBlackGold) 0.03f else 0.055f) * sheenScale),
                    Color.Transparent,
                    palette.accentGlow.copy(alpha = (if (isPitchBlackGold) 0.03f else 0.055f) * sheenScale),
                    Color.Transparent,
                  )
                )
              )
        )

        // Subtle mineral frosted grain so surfaces look like layered dark frosted glass
        FrostedGlassGrainOverlay(
          grainAlpha = (if (elevated) 0.036f else 0.028f) * sheenScale,
          isPitchBlackGold = isPitchBlackGold,
          modifier = Modifier.matchParentSize(),
        )

        // Subtle inner edge frosted bevel highlight
        Box(
          modifier =
            Modifier.matchParentSize()
              .padding(1.dp)
              .border(
                0.6.dp,
                Brush.verticalGradient(
                  listOf(
                    Color.White.copy(alpha = (if (isPitchBlackGold) 0.08f else 0.16f) * sheenScale),
                    Color.Transparent,
                    Color.White.copy(alpha = 0.03f * sheenScale),
                  )
                ),
                RoundedCornerShape((cornerRadius - 1.dp).coerceAtLeast(4.dp)),
              )
        )

        // Crisp 1.dp top specular frosted edge highlight
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(1.dp)
              .align(Alignment.TopCenter)
              .background(
                Brush.horizontalGradient(
                  listOf(
                    Color.Transparent,
                    palette.accentGlow.copy(alpha = 0.34f * sheenScale),
                    Color.White.copy(alpha = (if (isPitchBlackGold) 0.08f else 0.20f) * sheenScale),
                    palette.accentGlow.copy(alpha = 0.34f * sheenScale),
                    Color.Transparent,
                  )
                )
              )
        )
        content()
      }
    }
  }
}

/**
 * Clean, boxless icon button so icons sit directly on top of unified frosted glass cards
 * and headers without square/circle nested box artifacts or drop shadows.
 */
@Composable
fun ThemedIconButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  buttonSize: Dp = 42.dp,
  content: @Composable () -> Unit,
) {
  IconButton(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.size(maxOf(buttonSize, 48.dp)),
  ) {
    content()
  }
}

@Composable
fun ScrollToTopFloatingButton(
  visible: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  buttonTestTag: String = "scroll_to_top_btn",
) {
  val strings = LocalAppStrings.current
  FloatingGlassCircleButton(
    visible = visible,
    icon = Icons.Rounded.KeyboardArrowUp,
    contentDescription = strings.scrollToTop,
    onClick = onClick,
    buttonTestTag = buttonTestTag,
    modifier = modifier,
  )
}

@Composable
fun LocateCurrentTrackFloatingButton(
  visible: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  buttonTestTag: String = "locate_current_track_btn",
) {
  val strings = LocalAppStrings.current
  FloatingGlassCircleButton(
    visible = visible,
    icon = Icons.Rounded.MyLocation,
    contentDescription = strings.locateCurrentTrack,
    onClick = onClick,
    buttonTestTag = buttonTestTag,
    modifier = modifier,
  )
}

@Composable
private fun FloatingGlassCircleButton(
  visible: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  buttonTestTag: String,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  val isPitchBlackGold = palette.themeStyle == AppThemeStyle.PITCH_BLACK_GOLD

  AnimatedVisibility(
    visible = visible,
    enter = fadeIn() + scaleIn(initialScale = 0.85f),
    exit = fadeOut() + scaleOut(targetScale = 0.85f),
    modifier = modifier,
  ) {
    Box(
      modifier =
        Modifier.size(48.dp)
          .clip(CircleShape)
          .background(palette.glassButtonFillBrush)
          .border(1.2.dp, palette.buttonBorderColor, CircleShape)
          .clickable(onClick = onClick)
          .testTag(buttonTestTag),
      contentAlignment = Alignment.Center,
    ) {
      if (!palette.isMinimalPlainBlack) {
        FrostedGlassGrainOverlay(
          grainAlpha = 0.03f,
          isPitchBlackGold = isPitchBlackGold,
          modifier = Modifier.matchParentSize(),
        )
      }
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = palette.buttonContentColor,
        modifier = Modifier.size(24.dp),
      )
    }
  }
}

@Composable
fun Modifier.activeGlassOptionPillDecoration(
  selected: Boolean,
  shape: Shape = RoundedCornerShape(14.dp),
): Modifier {
  val palette = LocalMPlayerPalette.current
  if (!selected) return this
  val glowColor =
    if (palette.themeStyle == AppThemeStyle.PITCH_BLACK_GOLD || palette.isMinimalPlainBlack) {
      Color.Transparent
    } else {
      palette.activeOptionGlowColor
    }
  return this
    .then(
      if (glowColor != Color.Transparent) {
        Modifier.drawBehind {
          drawRoundRect(
            color = glowColor,
            topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
            size = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
          )
        }
      } else {
        Modifier
      }
    )
    .clip(shape)
    .background(palette.activeOptionPillBrush)
    .border(1.15.dp, palette.buttonBorderColor, shape)
}

/**
 * Translucent frosted glass playback control button with subtle glowing specular rim,
 * blurred theme-tinted internal refraction, and crisp ripple touch target (>= 48.dp).
 */
@Composable
fun GlassPlaybackControlButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  buttonSize: Dp = 52.dp,
  isPrimary: Boolean = false,
  isActive: Boolean = false,
  surfaceOpacityScale: Float = 1f,
  content: @Composable BoxScope.() -> Unit,
) {
  val palette = LocalMPlayerPalette.current
  val visualSize = buttonSize
  val touchSize = maxOf(buttonSize, 48.dp)
  val isPitchBlackGold = palette.themeStyle == AppThemeStyle.PITCH_BLACK_GOLD
  val isMinimalPlainBlack = palette.isMinimalPlainBlack
  val paletteOpacityScale = (palette.appGlassOpacity / 0.55f).coerceIn(0.15f, 1.82f)
  val alphaScale = (surfaceOpacityScale * paletteOpacityScale).coerceIn(0.05f, 1.5f)

  val glassFill =
    when {
      isMinimalPlainBlack && (isPrimary || isActive) -> palette.activeOptionPillBrush
      isMinimalPlainBlack -> palette.glassButtonFillBrush
      isPrimary -> palette.glassPlayButtonBrush
      isActive && !isPitchBlackGold -> palette.activeOptionPillBrush
      else ->
        Brush.linearGradient(
          listOf(
            palette.cardSurfaceElevated.copy(alpha = (0.38f * alphaScale).coerceIn(0.02f, 0.95f)),
            palette.cardSurface.copy(alpha = (0.28f * alphaScale).coerceIn(0.02f, 0.92f)),
            palette.backgroundBottom.copy(alpha = (0.48f * alphaScale).coerceIn(0.02f, 0.96f)),
          )
        )
    }

  val rimBrush =
    Brush.linearGradient(
      when {
        isMinimalPlainBlack -> listOf(Color.White, Color.White)
        isPitchBlackGold ->
          listOf(
            MetallicGoldBright.copy(alpha = if (isPrimary || isActive) 0.96f else 0.72f),
            MetallicGoldDark.copy(alpha = if (isPrimary || isActive) 0.80f else 0.52f),
            MetallicGoldBright.copy(alpha = if (isPrimary || isActive) 0.92f else 0.68f),
          )
        isPrimary || isActive ->
          listOf(
            palette.accentGlow.copy(alpha = 0.85f),
            palette.buttonBorderColor.copy(alpha = 0.72f),
            palette.accentGlow.copy(alpha = 0.65f),
          )
        else ->
          listOf(
            Color.White.copy(alpha = 0.16f),
            palette.accentGlow.copy(alpha = 0.34f),
            palette.accentPrimary.copy(alpha = 0.24f),
          )
      }
    )

  Box(
    modifier = modifier.size(touchSize).clip(CircleShape).clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.size(visualSize)
          .then(
            if (isActive && !isPrimary && !isPitchBlackGold && !isMinimalPlainBlack && palette.activeOptionGlowColor != Color.Transparent) {
              Modifier.drawBehind {
                drawCircle(
                  color = palette.activeOptionGlowColor,
                  radius = size.minDimension * 0.56f,
                )
              }
            } else {
              Modifier
            }
          )
          .clip(CircleShape)
          .background(glassFill)
          .border(
            width = if (isMinimalPlainBlack) 1.dp else if (isPrimary) 1.25.dp else if (isActive) 1.3.dp else 1.dp,
            brush = rimBrush,
            shape = CircleShape,
          ),
      contentAlignment = Alignment.Center,
    ) {
      if (!isMinimalPlainBlack) {
        if (palette.glowOrbPrimary != Color.Transparent) {
          Box(
            modifier =
              Modifier.matchParentSize()
                .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(14.dp) else Modifier)
                .background(
                  Brush.radialGradient(
                    listOf(
                      palette.accentGlow.copy(
                        alpha =
                          when {
                            isPrimary -> 0.14f
                            isActive -> 0.18f
                            else -> 0.08f
                          }
                      ),
                      Color.Transparent,
                    )
                  )
                )
          )
        }
        FrostedGlassGrainOverlay(
          grainAlpha = if (isPrimary) 0.030f else 0.024f,
          isPitchBlackGold = isPitchBlackGold,
          modifier = Modifier.matchParentSize(),
        )
        Box(
          modifier =
            Modifier.matchParentSize()
              .background(
                Brush.verticalGradient(
                  listOf(
                    Color.White.copy(
                      alpha = (if (isPitchBlackGold) 0.04f else if (isPrimary) 0.06f else 0.05f) *
                        minOf(alphaScale, 1f)
                    ),
                    Color.Transparent,
                    Color.Transparent,
                  )
                )
              )
        )
      }
      content()
      if (isActive && !isPrimary && !isMinimalPlainBlack) {
        Box(
          modifier =
            Modifier.align(Alignment.BottomCenter)
              .padding(bottom = 4.dp)
              .size(4.5.dp)
              .clip(CircleShape)
              .background(palette.accentGlow)
        )
      }
    }
  }
}

/**
 * Clean icon wrapper with no nested square box artifacts behind icons.
 */
@Composable
fun ThemedIconBadge(
  modifier: Modifier = Modifier,
  size: Dp = 44.dp,
  cornerRadius: Dp = 14.dp,
  active: Boolean = false,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
    content = content,
  )
}

@Composable
fun ThemedPrimaryButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
  content: @Composable RowScope.() -> Unit,
) {
  val palette = LocalMPlayerPalette.current
  val shape = RoundedCornerShape(16.dp)
  val isPitchBlackGold = palette.themeStyle == AppThemeStyle.PITCH_BLACK_GOLD
  val isMinimalPlainBlack = palette.isMinimalPlainBlack
  val containerBrush =
    when {
      isMinimalPlainBlack -> SolidBlackButtonBrush
      enabled -> palette.glassButtonFillBrush
      else ->
        Brush.linearGradient(
          listOf(
            palette.cardSurface.copy(alpha = 0.36f),
            palette.backgroundBottom.copy(alpha = 0.45f),
          )
        )
    }
  val contentColor =
    if (enabled) palette.buttonContentColor else palette.textSecondary.copy(alpha = 0.6f)

  Surface(
    onClick = onClick,
    enabled = enabled,
    shape = shape,
    color = Color.Transparent,
    contentColor = contentColor,
    border = BorderStroke(palette.borderStrokeWidth, palette.borderBrush),
    shadowElevation = 0.dp,
    tonalElevation = 0.dp,
    modifier =
      modifier
        .defaultMinSize(minWidth = ButtonDefaults.MinWidth, minHeight = ButtonDefaults.MinHeight)
        .clip(shape)
        .background(containerBrush),
  ) {
    Box(
      modifier =
        Modifier.defaultMinSize(
          minWidth = ButtonDefaults.MinWidth,
          minHeight = ButtonDefaults.MinHeight,
        ),
      contentAlignment = Alignment.Center,
    ) {
      if (!isMinimalPlainBlack) {
        FrostedGlassGrainOverlay(
          grainAlpha = 0.028f,
          isPitchBlackGold = isPitchBlackGold,
          modifier = Modifier.matchParentSize(),
        )
        Box(
          modifier =
            Modifier.matchParentSize(),
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(
                  Brush.horizontalGradient(
                    listOf(
                      Color.Transparent,
                      palette.accentGlow.copy(alpha = 0.35f),
                      Color.White.copy(alpha = if (isPitchBlackGold) 0.06f else 0.14f),
                      palette.accentGlow.copy(alpha = 0.35f),
                      Color.Transparent,
                    )
                  )
                )
          )
        }
      }
      ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
        Row(
          modifier = Modifier.padding(contentPadding),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          content = content,
        )
      }
    }
  }
}

private val SolidBlackButtonBrush = Brush.linearGradient(listOf(Color.Black, Color.Black))

@Composable
fun ArtworkThumbnail(
  model: Any?,
  contentDescription: String,
  size: Dp = 52.dp,
  cornerRadius: Dp = 14.dp,
  showActiveRim: Boolean = false,
  isVideo: Boolean = false,
) {
  val palette = LocalMPlayerPalette.current
  val shape = RoundedCornerShape(cornerRadius)
  Box(
    modifier =
      Modifier.size(size)
        .clip(shape)
        .background(palette.cardSurfaceElevated)
        .border(
          width = if (showActiveRim) 1.4.dp else palette.borderStrokeWidth,
          brush =
            if (showActiveRim && palette.isGlassTheme) {
              Brush.linearGradient(listOf(palette.accentGlow, palette.buttonContainerColor))
            } else {
              palette.borderBrush
            },
          shape = shape,
        ),
    contentAlignment = Alignment.Center,
  ) {
    if (model != null) {
      AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Icon(
        imageVector = if (isVideo) Icons.Rounded.Videocam else Icons.Rounded.MusicNote,
        contentDescription = contentDescription,
        tint = palette.accentGlow,
        modifier = Modifier.size(size * 0.46f),
      )
    }

    if (isVideo) {
      Box(
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .padding(3.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.70f))
            .border(0.75.dp, palette.accentGlow.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
            .padding(2.dp),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.Videocam,
          contentDescription = null,
          tint = palette.sectionHelperIconColor,
          modifier = Modifier.size(11.dp),
        )
      }
    }
  }
}

@Composable
fun PlaylistCoverArt(
  coverPath: String?,
  contentDescription: String,
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 18.dp,
) {
  val palette = LocalMPlayerPalette.current
  val shape = RoundedCornerShape(cornerRadius)
  Box(
    modifier =
      modifier
        .clip(shape)
        .background(palette.cardSurfaceElevated)
        .border(palette.borderStrokeWidth, palette.borderBrush, shape),
    contentAlignment = Alignment.Center,
  ) {
    val file = coverPath?.let { File(it) }?.takeIf { it.exists() }
    if (file != null) {
      AsyncImage(
        model = file,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Image(
        painter = painterResource(id = R.drawable.mplayer_icon_art),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        alpha = if (palette.isGlassTheme) 0.90f else 1.0f,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@Composable
fun MiniPlayerBar(
  playback: PlaybackUiState,
  onClick: () -> Unit,
  onPlayPause: () -> Unit,
  onNext: () -> Unit,
  onToggle8D: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val track = playback.currentTrack ?: return
  val palette = LocalMPlayerPalette.current
  val duration = playback.durationMs.coerceAtLeast(1L)
  val progress = (playback.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

  ThemedCard(
    modifier = modifier.fillMaxWidth().testTag("mini_player_bar"),
    cornerRadius = 20.dp,
    elevated = true,
    onClick = onClick,
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ArtworkThumbnail(
          model = track.albumArtUri,
          contentDescription = track.title,
          size = 44.dp,
          cornerRadius = 12.dp,
          showActiveRim = playback.is8DEnabled,
          isVideo = track.isVideo,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = track.title,
            style = MaterialTheme.typography.titleSmall,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        // Quick 8D frosted glass badge pill with active indicator dot
        Row(
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .background(
                if (playback.is8DEnabled) palette.glassBadgeActiveFillBrush
                else palette.glassBadgeFillBrush
              )
              .border(
                width = if (playback.is8DEnabled) 1.3.dp else 0.9.dp,
                color =
                  if (playback.is8DEnabled) palette.buttonBorderColor
                  else palette.textSecondary.copy(alpha = 0.28f),
                shape = RoundedCornerShape(50),
              )
              .clickable(onClick = onToggle8D)
              .padding(horizontal = 9.dp, vertical = 5.dp)
              .testTag("mini_8d_toggle"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          if (playback.is8DEnabled) {
            Box(
              modifier = Modifier.size(6.dp).clip(CircleShape).background(palette.accentGlow)
            )
          }
          Text(
            text = if (playback.is8DEnabled) "8D ${playback.audio8DIntensity}%" else "8D",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (playback.is8DEnabled) palette.accentGlow else palette.textSecondary,
            maxLines = 1,
          )
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            GlassPlaybackControlButton(
              onClick = onPlayPause,
              buttonSize = 40.dp,
              isPrimary = true,
              modifier = Modifier.testTag("mini_play_pause_button"),
            ) {
              Icon(
                imageVector = if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playback.isPlaying) "Pause" else "Play",
                tint = palette.buttonContentColor,
                modifier = Modifier.size(22.dp),
              )
            }

            GlassPlaybackControlButton(
              onClick = onNext,
              buttonSize = 40.dp,
              modifier = Modifier.testTag("mini_next_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = palette.textPrimary,
                modifier = Modifier.size(22.dp),
              )
            }
          }
        }
      }

      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier.fillMaxWidth().height(2.5.dp),
          color = palette.accentGlow,
          trackColor = palette.textSecondary.copy(alpha = 0.16f),
        )
      }
    }
  }
}

@Composable
fun Orbit8DVisualizer(
  is8DEnabled: Boolean,
  isPlaying: Boolean,
  panAngleDegrees: Float,
  albumArtUri: Any?,
  title: String,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  val infiniteTransition = rememberInfiniteTransition(label = "8d_pulse")
  val pulse by
    infiniteTransition.animateFloat(
      initialValue = 0.94f,
      targetValue = 1.04f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(1600, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "pulse",
    )

  Box(modifier = modifier.size(260.dp), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val outerRadius = size.minDimension * 0.46f

      if (is8DEnabled) {
        if (palette.isGlassTheme && palette.glowOrbPrimary != Color.Transparent) {
          val activeRadius = outerRadius * (if (isPlaying) pulse else 1f)
          drawCircle(
            brush =
              Brush.radialGradient(
                colors = listOf(palette.accentGlow.copy(alpha = 0.28f), Color.Transparent),
                center = center,
                radius = activeRadius * 1.15f,
              ),
            radius = activeRadius * 1.15f,
            center = center,
          )
        }
        drawCircle(
          color = palette.buttonBorderColor.copy(alpha = 0.7f),
          radius = outerRadius,
          center = center,
          style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
        )

        val rad = panAngleDegrees * (PI / 180.0)
        val orbX = center.x + (outerRadius * cos(rad)).toFloat()
        val orbY = center.y + (outerRadius * sin(rad)).toFloat()
        drawCircle(
          color = palette.buttonBorderColor,
          radius = 8.dp.toPx(),
          center = Offset(orbX, orbY),
        )
        drawCircle(
          color = Color.White,
          radius = 3.5.dp.toPx(),
          center = Offset(orbX, orbY),
        )
      } else {
        drawCircle(
          color = palette.textSecondary.copy(alpha = 0.20f),
          radius = outerRadius,
          center = center,
          style = Stroke(width = 1.2.dp.toPx()),
        )
      }
    }

    Box(
      modifier =
        Modifier.size(198.dp)
          .clip(CircleShape)
          .background(palette.cardSurfaceElevated)
          .border(1.5.dp, palette.borderBrush, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      if (albumArtUri != null) {
        AsyncImage(
          model = albumArtUri,
          contentDescription = title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Image(
          painter = painterResource(id = R.drawable.mplayer_icon_art),
          contentDescription = title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }

      if (is8DEnabled) {
        Box(
          modifier =
            Modifier.align(Alignment.BottomCenter)
              .padding(bottom = 12.dp)
              .clip(RoundedCornerShape(50))
              .background(palette.buttonContainerColor.copy(alpha = 0.92f))
              .border(1.dp, palette.buttonBorderColor, RoundedCornerShape(50))
              .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.Headphones,
              contentDescription = null,
              tint = palette.accentGlow,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "8D 360°",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = palette.buttonContentColor,
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistBottomSheet(
  track: AudioTrack,
  playlists: List<PlaylistSummary>,
  onSelectPlaylist: (PlaylistSummary) -> Unit,
  onCreateNewPlaylist: () -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = palette.sheetBg,
    contentColor = palette.sheetTextPrimary,
    tonalElevation = 0.dp,
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
      Text(
        text = strings.addToPlaylist,
        style = MaterialTheme.typography.titleLarge,
        color = palette.textPrimary,
      )
      Text(
        text = track.title,
        style = MaterialTheme.typography.bodyMedium,
        color = palette.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(14.dp))

      ThemedCard(
        onClick = onCreateNewPlaylist,
        cornerRadius = 16.dp,
        modifier = Modifier.fillMaxWidth().testTag("sheet_create_playlist_btn"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = strings.createPlaylist,
            tint = palette.sectionHelperIconColor,
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = strings.createPlaylist,
            style = MaterialTheme.typography.titleSmall,
            color = palette.sectionHelperTextColor,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(playlists, key = { it.id }) { playlist ->
          ThemedCard(
            onClick = { onSelectPlaylist(playlist) },
            cornerRadius = 16.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              PlaylistCoverArt(
                coverPath = playlist.coverPath,
                contentDescription = playlist.name,
                modifier = Modifier.size(44.dp),
                cornerRadius = 10.dp,
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = playlist.name,
                  style = MaterialTheme.typography.titleSmall,
                  color = palette.textPrimary,
                )
                Text(
                  text = strings.tracksCountFormat(playlist.trackCount),
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textSecondary,
                )
              }
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                contentDescription = null,
                tint = palette.sectionHelperIconColor,
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
