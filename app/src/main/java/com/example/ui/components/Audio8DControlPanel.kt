package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.localization.LocalAppStrings
import com.example.model.Audio8DSpeed
import com.example.ui.theme.LocalMPlayerPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * Sleek dark-themed 8D Spatial Audio Control Panel Bottom Sheet.
 * Contains:
 *  a. Enable 8D Spatial Effect Toggle Switch
 *  b. Live Spatial Soundfield Radar (L, R, F, B)
 *  c. Movement Speed Selector Buttons [Slow, Normal, Fast]
 *  d. Real-Time Effect Intensity Slider (0% - 100%)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Audio8DControlSheet(
  isEnabled: Boolean,
  isPlaying: Boolean,
  speed: Audio8DSpeed,
  intensityPercent: Int,
  panAngleDegrees: Float,
  onToggleEnabled: (Boolean) -> Unit,
  onSelectSpeed: (Audio8DSpeed) -> Unit,
  onChangeIntensity: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  // Sleek dark-themed per active theme (Frosted Burgundy, Solid Black+Gold, or Deep Purple Glass)
  val sheetBg = palette.sheetBg
  val cardFill = palette.sheetCardFill
  val cardBorder = palette.sheetCardBorder
  val accentColor = palette.sheetActiveAccent
  val accentTextColor = palette.sheetAccentText
  val textPrimary = palette.sheetTextPrimary
  val textSecondary = palette.sheetTextSecondary

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = sheetBg,
    contentColor = textPrimary,
    tonalElevation = 0.dp,
    modifier = Modifier.testTag("audio_8d_control_sheet"),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(
            imageVector = Icons.Rounded.Headphones,
            contentDescription = strings.audio8DPanelTitle,
            tint = palette.sectionHelperIconColor,
            modifier = Modifier.size(26.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = strings.audio8DPanelTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = strings.audio8DSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = textSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(48.dp).testTag("audio_8d_panel_close"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = strings.dismiss,
            tint = textSecondary,
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // a. Enable 8D Spatial Effect Toggle Switch
      Surface(
        modifier = Modifier.fillMaxWidth().testTag("audio_8d_enable_card"),
        shape = RoundedCornerShape(18.dp),
        color = cardFill,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        onClick = { onToggleEnabled(!isEnabled) },
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = strings.audio8DToggleLabel,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              color = textPrimary,
            )
            Text(
              text = if (isEnabled) strings.audio8DOn else strings.audio8DOff,
              style = MaterialTheme.typography.bodySmall,
              color = if (isEnabled) accentTextColor else textSecondary,
            )
          }
          Switch(
            checked = isEnabled,
            onCheckedChange = onToggleEnabled,
            colors =
              SwitchDefaults.colors(
                checkedThumbColor = palette.buttonContentColor,
                checkedTrackColor = palette.buttonContainerColor,
                checkedBorderColor = palette.buttonBorderColor,
                uncheckedThumbColor = textSecondary,
                uncheckedTrackColor = palette.cardSurfaceElevated,
              ),
            modifier = Modifier.testTag("audio_8d_panel_switch"),
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // b. Spatial Soundfield Radar (L, R, F, B)
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardFill,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = strings.audio8DRadarTitle,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = textPrimary,
            )
            Text(
              text = if (isEnabled) "$intensityPercent%" else strings.audio8DOff,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = accentTextColor,
            )
          }
          Spacer(modifier = Modifier.height(12.dp))
          androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
          ) {
            SpatialSoundfieldRadar(
              isEnabled = isEnabled,
              isPlaying = isPlaying,
              speed = speed,
              intensityPercent = intensityPercent,
              panAngleDegrees = panAngleDegrees,
              radarSize = 200.dp,
              modifier = Modifier.testTag("audio_8d_radar"),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // c. Movement Speed Selector Buttons [Slow, Normal, Fast]
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cardFill,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.Speed,
              contentDescription = null,
              tint = palette.sectionHelperIconColor,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = strings.audio8DSpeedTitle,
              style = MaterialTheme.typography.titleSmall,
              color = textPrimary,
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Audio8DSpeedSelector(
            selectedSpeed = speed,
            onSelectSpeed = onSelectSpeed,
            isDarkPanel = true,
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // d. Effect Intensity Slider (0% - 100%)
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cardFill,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                tint = palette.sectionHelperIconColor,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = strings.audio8DIntensityTitle,
                style = MaterialTheme.typography.titleSmall,
                color = textPrimary,
              )
            }
            Text(
              text = "$intensityPercent%",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = accentTextColor,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Audio8DIntensitySlider(
            intensityPercent = intensityPercent,
            onChangeIntensity = onChangeIntensity,
            darkSurface = true,
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))
    }
  }
}

/**
 * Spatial Soundfield Radar visualizing 360-degree L / R / F / B spatial positioning
 * driven by live DSP pan angle, movement speed, and intensity depth (0..100%).
 */
@Composable
fun SpatialSoundfieldRadar(
  isEnabled: Boolean,
  isPlaying: Boolean,
  speed: Audio8DSpeed,
  intensityPercent: Int,
  panAngleDegrees: Float,
  modifier: Modifier = Modifier,
  radarSize: Dp = 190.dp,
  forceNeutralCapsuleStyle: Boolean = false,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current

  // Preview oscillator when 8D is enabled while paused so speed & intensity preview visually
  var previewAngle by remember { mutableFloatStateOf(panAngleDegrees) }
  LaunchedEffect(isEnabled, isPlaying, speed) {
    if (isEnabled && !isPlaying) {
      while (true) {
        val degPerFrame = (speed.frequencyHz * 360.0 * 0.032).toFloat()
        previewAngle = (previewAngle + degPerFrame) % 360f
        delay(32L)
      }
    }
  }

  val activeAngle = if (isPlaying) panAngleDegrees else previewAngle
  val animatedIntensity by
    animateFloatAsState(
      targetValue = if (isEnabled) (intensityPercent / 100f).coerceIn(0f, 1f) else 0f,
      animationSpec = tween(180),
      label = "radar_intensity",
    )

  val pulseTransition = rememberInfiniteTransition(label = "radar_pulse")
  val pulseScale by
    pulseTransition.animateFloat(
      initialValue = 0.85f,
      targetValue = 1.25f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(1100, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "radar_dot_pulse",
    )

  val ringColor =
    if (forceNeutralCapsuleStyle) Color(0x66FFFFFF)
    else palette.vizOuterRing.copy(alpha = 0.65f)
  val activeDotColor =
    if (forceNeutralCapsuleStyle) Color.White
    else palette.sheetAccentText
  val haloColor =
    if (forceNeutralCapsuleStyle) Color(0x55FFFFFF)
    else palette.sheetActiveAccent
  val poleLabelColor =
    if (forceNeutralCapsuleStyle) Color(0xE6FFFFFF)
    else palette.sheetAccentText.copy(alpha = 0.90f)

  Box(modifier = modifier.size(radarSize), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize().padding(18.dp)) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val outerR = size.minDimension / 2f

      // Concentric soundfield range rings (33%, 66%, 100%)
      listOf(0.35f, 0.68f, 1.0f).forEachIndexed { idx, factor ->
        drawCircle(
          color = ringColor.copy(alpha = if (idx == 2) 0.55f else 0.25f),
          radius = outerR * factor,
          center = center,
          style =
            Stroke(
              width = if (idx == 2) 1.5.dp.toPx() else 1.dp.toPx(),
              pathEffect =
                if (idx < 2) PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) else null,
            ),
        )
      }

      // Crosshair axes (F-B vertical, L-R horizontal)
      drawLine(
        color = ringColor.copy(alpha = 0.28f),
        start = Offset(center.x, center.y - outerR),
        end = Offset(center.x, center.y + outerR),
        strokeWidth = 1.dp.toPx(),
      )
      drawLine(
        color = ringColor.copy(alpha = 0.28f),
        start = Offset(center.x - outerR, center.y),
        end = Offset(center.x + outerR, center.y),
        strokeWidth = 1.dp.toPx(),
      )

      if (isEnabled) {
        // Depth-scaled distance from center (0% intensity = near center, 100% = wide peripheral 8D field)
        val radialDist = outerR * (0.18f + 0.78f * animatedIntensity)
        val angleRad = activeAngle * (PI / 180.0)
        // 0° = Front (Top), 90° = Right, 180° = Back (Bottom), 270° = Left
        val soundX = center.x + (radialDist * sin(angleRad)).toFloat()
        val soundY = center.y - (radialDist * cos(angleRad)).toFloat()
        val soundPos = Offset(soundX, soundY)

        // Spatial intensity coverage circle
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(haloColor.copy(alpha = 0.26f), Color.Transparent),
              center = center,
              radius = radialDist.coerceAtLeast(1f),
            ),
          radius = radialDist,
          center = center,
        )

        // Directional radar beam line
        drawLine(
          brush = Brush.linearGradient(listOf(haloColor.copy(alpha = 0.2f), activeDotColor), center, soundPos),
          start = center,
          end = soundPos,
          strokeWidth = 2.2.dp.toPx(),
          cap = StrokeCap.Round,
        )

        // Pulsing sound source emitter halo
        drawCircle(
          color = haloColor.copy(alpha = 0.42f),
          radius = 12.dp.toPx() * pulseScale,
          center = soundPos,
        )
        drawCircle(
          color = activeDotColor,
          radius = 6.dp.toPx(),
          center = soundPos,
        )
      }
    }

    // Center listener head / headphones icon sitting cleanly on radar
    Icon(
      imageVector = Icons.Rounded.Headphones,
      contentDescription = null,
      tint = activeDotColor,
      modifier = Modifier.size(18.dp),
    )

    // F, B, L, R Pole Labels
    PoleTag(text = strings.radarFront, color = poleLabelColor, modifier = Modifier.align(Alignment.TopCenter))
    PoleTag(text = strings.radarBack, color = poleLabelColor, modifier = Modifier.align(Alignment.BottomCenter))
    PoleTag(text = strings.radarLeft, color = poleLabelColor, modifier = Modifier.align(Alignment.CenterStart))
    PoleTag(text = strings.radarRight, color = poleLabelColor, modifier = Modifier.align(Alignment.CenterEnd))
  }
}

@Composable
private fun PoleTag(text: String, color: Color, modifier: Modifier = Modifier) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelMedium,
    fontWeight = FontWeight.Bold,
    color = color,
    modifier = modifier.padding(2.dp),
  )
}

@Composable
fun Audio8DSpeedSelector(
  selectedSpeed: Audio8DSpeed,
  onSelectSpeed: (Audio8DSpeed) -> Unit,
  modifier: Modifier = Modifier,
  isDarkPanel: Boolean = false,
  forceNeutralCapsuleStyle: Boolean = false,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current

  val items =
    listOf(
      Audio8DSpeed.SLOW to strings.audio8DSpeedSlow,
      Audio8DSpeed.NORMAL to strings.audio8DSpeedNormal,
      Audio8DSpeed.FAST to strings.audio8DSpeedFast,
    )

  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items.forEach { (speed, label) ->
      val selected = speed == selectedSpeed
      val fillColor =
        when {
          forceNeutralCapsuleStyle && selected -> Color(0x59FFFFFF)
          forceNeutralCapsuleStyle -> Color(0x1EFFFFFF)
          selected && isDarkPanel -> palette.sheetSelectedFill
          selected -> palette.buttonContainerColor
          isDarkPanel -> palette.sheetTextSecondary.copy(alpha = 0.14f)
          else -> palette.cardSurface
        }
      val borderColor =
        when {
          forceNeutralCapsuleStyle && selected -> Color(0xCCFFFFFF)
          forceNeutralCapsuleStyle -> Color(0x45FFFFFF)
          selected && isDarkPanel -> palette.sheetSelectedBorder
          selected -> palette.buttonBorderColor
          isDarkPanel -> palette.sheetTextSecondary.copy(alpha = 0.25f)
          else -> palette.textSecondary.copy(alpha = 0.25f)
        }
      val textColor =
        when {
          forceNeutralCapsuleStyle -> Color.White
          selected && isDarkPanel -> palette.sheetAccentText
          selected -> palette.buttonContentColor
          isDarkPanel -> palette.sheetTextSecondary
          else -> palette.textPrimary
        }

      val useActiveGlassGlow =
        selected && !forceNeutralCapsuleStyle &&
          palette.themeStyle != com.example.model.AppThemeStyle.PITCH_BLACK_GOLD &&
          !palette.isMinimalPlainBlack
      val chipModifier =
        if (useActiveGlassGlow) {
          Modifier.weight(1f)
            .height(42.dp)
            .activeGlassOptionPillDecoration(selected = true, shape = RoundedCornerShape(12.dp))
        } else {
          Modifier.weight(1f)
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fillColor)
            .border(1.1.dp, borderColor, RoundedCornerShape(12.dp))
        }

      Box(
        modifier =
          chipModifier
            .clickable { onSelectSpeed(speed) }
            .testTag("8d_speed_${speed.name.lowercase()}"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
          color = textColor,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun Audio8DIntensitySlider(
  intensityPercent: Int,
  onChangeIntensity: (Int) -> Unit,
  modifier: Modifier = Modifier,
  darkSurface: Boolean = false,
  forceNeutralCapsuleStyle: Boolean = false,
) {
  val palette = LocalMPlayerPalette.current
  val thumbColor =
    when {
      forceNeutralCapsuleStyle -> Color.White
      darkSurface -> palette.sheetAccentText
      else -> palette.accentGlow
    }
  val activeTrack =
    when {
      forceNeutralCapsuleStyle -> Color(0xE6FFFFFF)
      darkSurface -> palette.sheetActiveAccent
      else -> palette.accentPrimary
    }
  val inactiveTrack =
    when {
      forceNeutralCapsuleStyle -> Color(0x44FFFFFF)
      darkSurface -> palette.sheetTextSecondary.copy(alpha = 0.24f)
      else -> palette.textSecondary.copy(alpha = 0.24f)
    }

  Slider(
    value = intensityPercent.toFloat(),
    onValueChange = { value -> onChangeIntensity(value.roundToInt().coerceIn(0, 100)) },
    valueRange = 0f..100f,
    colors =
      SliderDefaults.colors(
        thumbColor = thumbColor,
        activeTrackColor = activeTrack,
        inactiveTrackColor = inactiveTrack,
      ),
    modifier = modifier.fillMaxWidth().testTag("8d_intensity_slider"),
  )
}
