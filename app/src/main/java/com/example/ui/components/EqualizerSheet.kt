package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.localization.LocalAppStrings
import com.example.model.Audio8DSpeed
import com.example.model.EqualizerPreset
import com.example.playback.AudioAmplifierBus
import com.example.playback.AudioEqualizerBus
import com.example.playback.PlaybackUiState
import com.example.ui.theme.LocalMPlayerPalette
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
  isEnabled: Boolean,
  activePreset: EqualizerPreset?,
  bandGainsDb: List<Float>,
  onToggleEnabled: (Boolean) -> Unit,
  onSelectPreset: (EqualizerPreset) -> Unit,
  onChangeBandGain: (Int, Float) -> Unit,
  onDismiss: () -> Unit,
  playback: PlaybackUiState = PlaybackUiState(),
  panAngleDegrees: Float = 0f,
  onToggle8D: (Boolean) -> Unit = {},
  onSelect8DSpeed: (Audio8DSpeed) -> Unit = {},
  onChange8DIntensity: (Int) -> Unit = {},
  onSetVolumeBoost: (Int) -> Unit = {},
  onToggleAmplifier: (Boolean) -> Unit = {},
  onChangeAmplifierLevel: (Int) -> Unit = {},
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val sheetBg = palette.sheetBg
  val cardFill = palette.sheetCardFill
  val cardBorder = palette.sheetCardBorder
  val accentColor = palette.sheetAccentText
  val curveAccent = palette.sheetActiveAccent
  val textPrimary = palette.sheetTextPrimary
  val textSecondary = palette.sheetTextSecondary

  var hasAcknowledgedHighBoost by rememberSaveable { mutableStateOf(playback.volumeBoostPercent > 397) }
  var showHighBoostWarningDialog by remember { mutableStateOf(false) }
  var pendingHighBoostPercent by remember { mutableStateOf<Int?>(null) }
  var lastActiveBoostPercent by rememberSaveable { mutableIntStateOf(250) }
  var show8DRadar by rememberSaveable { mutableStateOf(false) }

  if (playback.volumeBoostPercent > 100) {
    lastActiveBoostPercent = playback.volumeBoostPercent
  }

  val bandFreqLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = sheetBg,
    contentColor = textPrimary,
    tonalElevation = 0.dp,
    modifier = Modifier.testTag("equalizer_sheet"),
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
            imageVector = Icons.Rounded.Equalizer,
            contentDescription = strings.equalizerTitle,
            tint = palette.sectionHelperIconColor,
            modifier = Modifier.size(26.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = strings.equalizerTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = activePreset?.let { presetLabel(it, strings) } ?: strings.equalizerPresetCustom,
              style = MaterialTheme.typography.bodySmall,
              color = if (isEnabled) accentColor else textSecondary,
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { onSelectPreset(EqualizerPreset.FLAT) },
            modifier = Modifier.size(48.dp).testTag("eq_reset_btn"),
          ) {
            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = strings.equalizerReset, tint = textSecondary)
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp).testTag("eq_close_btn"),
          ) {
            Icon(imageVector = Icons.Rounded.Close, contentDescription = strings.dismiss, tint = textSecondary)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Enable Equalizer Toggle Card
      Surface(
        modifier = Modifier.fillMaxWidth().testTag("eq_enable_card"),
        shape = RoundedCornerShape(18.dp),
        color = cardFill,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        onClick = { onToggleEnabled(!isEnabled) },
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = strings.equalizerEnableLabel,
            style = MaterialTheme.typography.titleSmall,
            color = textPrimary,
          )
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
            modifier = Modifier.testTag("eq_enable_switch"),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Interactive Frequency Response Curve Graph
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardFill,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
          androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
          ) {
            Column(modifier = Modifier.fillMaxWidth()) {
              EqCurvePreview(
                gainsDb = bandGainsDb,
                isEnabled = isEnabled,
                curveAccent = curveAccent,
                highlightColor = accentColor,
                modifier = Modifier.fillMaxWidth().height(84.dp),
              )
              Spacer(modifier = Modifier.height(10.dp))

              // 5 Interactive Vertical Frequency Faders (Bass, Midrange, Treble)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                for (bandIdx in 0 until AudioEqualizerBus.BAND_COUNT) {
                  val gain = bandGainsDb.getOrElse(bandIdx) { 0f }
                  VerticalEqSliderColumn(
                    freqLabel = bandFreqLabels[bandIdx],
                    gainDb = gain,
                    enabled = isEnabled,
                    accentColor = accentColor,
                    trackHighlight = curveAccent,
                    textSecondary = textSecondary,
                    isGlassTheme = palette.isGlassTheme,
                    sliderTrackBg = palette.vizCenterBadgeFill,
                    onChangeGain = { newDb -> onChangeBandGain(bandIdx, newDb) },
                    modifier = Modifier.weight(1f).testTag("eq_band_slider_$bandIdx"),
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = strings.equalizerBassLabel,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = accentColor,
            )
            Text(
              text = strings.equalizerMidLabel,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = textSecondary,
            )
            Text(
              text = strings.equalizerTrebleLabel,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = accentColor,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Genre Presets Carousel
      Text(
        text = strings.equalizerPresetsTitle,
        style = MaterialTheme.typography.labelLarge,
        color = textSecondary,
      )
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        EqualizerPreset.entries.forEach { preset ->
          val selected = isEnabled && preset == activePreset
          val isPitchBlackGold = palette.themeStyle == com.example.model.AppThemeStyle.PITCH_BLACK_GOLD
          val useActiveGlassGlow = selected && !isPitchBlackGold && !palette.isMinimalPlainBlack
          val chipFill =
            if (selected) palette.sheetSelectedFill else cardFill
          val chipBorder =
            if (selected) {
              BorderStroke(1.3.dp, palette.sheetSelectedBorder)
            } else {
              BorderStroke(0.9.dp, textSecondary.copy(alpha = 0.28f))
            }

          Surface(
            shape = RoundedCornerShape(50),
            color = if (useActiveGlassGlow) Color.Transparent else chipFill,
            border = if (useActiveGlassGlow) null else chipBorder,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            onClick = { onSelectPreset(preset) },
            modifier =
              Modifier.then(
                if (useActiveGlassGlow) {
                  Modifier.activeGlassOptionPillDecoration(selected = true, shape = RoundedCornerShape(50))
                } else {
                  Modifier
                }
              ).testTag("eq_preset_${preset.name.lowercase()}"),
          ) {
            Text(
              text = presetLabel(preset, strings),
              style = MaterialTheme.typography.labelLarge,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              color = if (selected) accentColor else textPrimary,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 8D Spatial Audio Control Card (Inside Equalizer Hub)
      Surface(
        modifier = Modifier.fillMaxWidth().testTag("eq_8d_card"),
        shape = RoundedCornerShape(18.dp),
        color = cardFill,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(
                imageVector = Icons.Rounded.Headphones,
                contentDescription = strings.audio8DTitle,
                tint = if (playback.is8DEnabled) accentColor else palette.sectionHelperIconColor,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = strings.audio8DTitle,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = if (playback.is8DEnabled) strings.audio8DOn else strings.audio8DOff,
                  style = MaterialTheme.typography.bodySmall,
                  color = if (playback.is8DEnabled) accentColor else textSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            Switch(
              checked = playback.is8DEnabled,
              onCheckedChange = onToggle8D,
              colors =
                SwitchDefaults.colors(
                  checkedThumbColor = palette.buttonContentColor,
                  checkedTrackColor = palette.buttonContainerColor,
                  checkedBorderColor = palette.buttonBorderColor,
                  uncheckedThumbColor = textSecondary,
                  uncheckedTrackColor = palette.cardSurfaceElevated,
                ),
              modifier = Modifier.testTag("eq_8d_switch"),
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          Audio8DSpeedSelector(
            selectedSpeed = playback.audio8DSpeed,
            onSelectSpeed = onSelect8DSpeed,
            isDarkPanel = true,
          )

          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = strings.audio8DIntensityTitle,
              style = MaterialTheme.typography.labelLarge,
              color = textSecondary,
            )
            Text(
              text = "${playback.audio8DIntensity}%",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = accentColor,
            )
          }
          Audio8DIntensitySlider(
            intensityPercent = playback.audio8DIntensity,
            onChangeIntensity = onChange8DIntensity,
            darkSurface = true,
          )

          Spacer(modifier = Modifier.height(4.dp))
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .background(palette.glassBadgeFillBrush)
                .border(1.dp, palette.borderBrush, RoundedCornerShape(50))
                .clickable { show8DRadar = !show8DRadar }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("eq_toggle_8d_radar_btn"),
          ) {
            Text(
              text = if (show8DRadar) strings.eq8DRadarToggleHide else strings.eq8DRadarToggleShow,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = accentColor,
              maxLines = 1,
              softWrap = false,
            )
          }

          AnimatedVisibility(visible = show8DRadar) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                SpatialSoundfieldRadar(
                  isEnabled = playback.is8DEnabled,
                  isPlaying = playback.isPlaying,
                  speed = playback.audio8DSpeed,
                  intensityPercent = playback.audio8DIntensity,
                  panAngleDegrees = panAngleDegrees,
                  radarSize = 168.dp,
                  modifier = Modifier.testTag("audio_8d_radar"),
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Real Hardware-Synced Volume Boost Card (0% - 797%, inside Equalizer Hub)
      val boostPercent = playback.volumeBoostPercent
      val isBoosted = boostPercent > 100
      val isUltraHighBoost = boostPercent > 397
      Surface(
        modifier = Modifier.fillMaxWidth().testTag("eq_volume_boost_card"),
        shape = RoundedCornerShape(18.dp),
        color = cardFill,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(
                imageVector =
                  if (boostPercent == 0) Icons.AutoMirrored.Rounded.VolumeOff
                  else Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = strings.volumeBoost,
                tint =
                  when {
                    isUltraHighBoost -> MaterialTheme.colorScheme.error
                    isBoosted -> accentColor
                    else -> palette.sectionHelperIconColor
                  },
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = strings.volumeBoost,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color =
                    when {
                      isUltraHighBoost -> MaterialTheme.colorScheme.error
                      isBoosted -> accentColor
                      else -> textPrimary
                    },
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Switch(
              checked = isBoosted,
              onCheckedChange = { enableBoost ->
                if (enableBoost) {
                  val maxSafeTarget = if (hasAcknowledgedHighBoost) 797 else 397
                  val target = lastActiveBoostPercent.coerceIn(150, maxSafeTarget)
                  onSetVolumeBoost(target)
                } else {
                  onSetVolumeBoost(100)
                }
              },
              colors =
                SwitchDefaults.colors(
                  checkedThumbColor = palette.buttonContentColor,
                  checkedTrackColor = palette.buttonContainerColor,
                  checkedBorderColor = palette.buttonBorderColor,
                  uncheckedThumbColor = textSecondary,
                  uncheckedTrackColor = palette.cardSurfaceElevated,
                ),
              modifier = Modifier.testTag("eq_volume_boost_switch"),
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text =
                "$boostPercent% • ${if (boostPercent > 100) strings.volumeBoostUltraLabel else strings.volumeBoostStandardLabel}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color =
                when {
                  isUltraHighBoost -> MaterialTheme.colorScheme.error
                  isBoosted -> accentColor
                  else -> textPrimary
                },
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(50))
                  .background(palette.glassBadgeFillBrush)
                  .border(1.dp, palette.borderBrush, RoundedCornerShape(50))
                  .clickable { onSetVolumeBoost(100) }
                  .padding(horizontal = 10.dp, vertical = 5.dp)
                  .testTag("volume_boost_reset_btn"),
            ) {
              Text(
                text = strings.volumeBoostReset,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                softWrap = false,
              )
            }
          }

          CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Slider(
              value = boostPercent.toFloat(),
              valueRange = 0f..797f,
              onValueChange = { rawValue ->
                val targetPct = rawValue.roundToInt().coerceIn(0, 797)
                if (targetPct > 397 && !hasAcknowledgedHighBoost) {
                  pendingHighBoostPercent = targetPct
                  if (boostPercent != 397) {
                    onSetVolumeBoost(397)
                  }
                  showHighBoostWarningDialog = true
                } else {
                  onSetVolumeBoost(targetPct)
                }
              },
              colors =
                SliderDefaults.colors(
                  thumbColor = if (isUltraHighBoost) MaterialTheme.colorScheme.error else accentColor,
                  activeTrackColor = if (isUltraHighBoost) MaterialTheme.colorScheme.error else curveAccent,
                  inactiveTrackColor = textSecondary.copy(alpha = 0.24f),
                ),
              modifier = Modifier.fillMaxWidth().testTag("volume_boost_slider"),
            )
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text("0%", style = MaterialTheme.typography.labelSmall, color = textSecondary)
              Text("100%", style = MaterialTheme.typography.labelSmall, color = textSecondary)
              Text(
                "397%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
              )
              Text(
                "797%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isUltraHighBoost) MaterialTheme.colorScheme.error else textSecondary,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Separate Real Audio Amplifier Preamp Stage (+0 dB .. +97 dB)
      val ampEnabled = playback.amplifierEnabled
      val ampLevel = playback.amplifierLevelPercent.coerceIn(0, 100)
      val ampDb = AudioAmplifierBus.computeGainDb(enabled = true, level = ampLevel)
      val ampDbLabel = String.format(Locale.US, "+%.1f dB", ampDb)
      Surface(
        modifier = Modifier.fillMaxWidth().testTag("eq_amplifier_card"),
        shape = RoundedCornerShape(18.dp),
        color = cardFill,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = strings.amplifierTitle,
                tint = if (ampEnabled) accentColor else palette.sectionHelperIconColor,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = strings.amplifierTitle,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (ampEnabled) accentColor else textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = strings.amplifierSubtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = textSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Switch(
              checked = ampEnabled,
              onCheckedChange = { enable ->
                if (enable && ampLevel <= 0) {
                  onChangeAmplifierLevel(50)
                }
                onToggleAmplifier(enable)
              },
              colors =
                SwitchDefaults.colors(
                  checkedThumbColor = palette.buttonContentColor,
                  checkedTrackColor = palette.buttonContainerColor,
                  checkedBorderColor = palette.buttonBorderColor,
                  uncheckedThumbColor = textSecondary,
                  uncheckedTrackColor = palette.cardSurfaceElevated,
                ),
              modifier = Modifier.testTag("eq_amplifier_switch"),
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = strings.amplifierGainLabel,
              style = MaterialTheme.typography.labelMedium,
              color = textSecondary,
            )
            Text(
              text = if (ampEnabled) ampDbLabel else "+0.0 dB",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = if (ampEnabled) accentColor else textSecondary,
            )
          }

          CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Slider(
              value = ampDb,
              valueRange = 0f..AudioAmplifierBus.MAX_PREAMP_DB,
              onValueChange = { rawDb ->
                val newLevel =
                  ((rawDb / AudioAmplifierBus.MAX_PREAMP_DB) * 100f)
                    .roundToInt()
                    .coerceIn(0, 100)
                if (!ampEnabled && newLevel > 0) {
                  onToggleAmplifier(true)
                }
                onChangeAmplifierLevel(newLevel)
              },
              colors =
                SliderDefaults.colors(
                  thumbColor = if (ampEnabled) accentColor else textSecondary,
                  activeTrackColor = if (ampEnabled) curveAccent else textSecondary.copy(alpha = 0.5f),
                  inactiveTrackColor = textSecondary.copy(alpha = 0.24f),
                ),
              modifier = Modifier.fillMaxWidth().testTag("eq_amplifier_slider"),
            )
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text("0 dB", style = MaterialTheme.typography.labelSmall, color = textSecondary)
              Text("+48.5 dB", style = MaterialTheme.typography.labelSmall, color = textSecondary)
              Text(
                "+97 dB",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (ampEnabled) accentColor else textSecondary,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(26.dp))
    }
  }

  if (showHighBoostWarningDialog) {
    AlertDialog(
      onDismissRequest = {
        pendingHighBoostPercent = null
        showHighBoostWarningDialog = false
      },
      containerColor = palette.sheetBg,
      titleContentColor = palette.textPrimary,
      textContentColor = palette.textSecondary,
      icon = {
        Icon(
          imageVector = Icons.Rounded.WarningAmber,
          contentDescription = strings.volumeBoostWarningTitle,
          tint = palette.sectionHelperIconColor,
          modifier = Modifier.size(30.dp),
        )
      },
      title = {
        Text(
          text = strings.volumeBoostWarningTitle,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = palette.textPrimary,
        )
      },
      text = {
        Text(
          text = strings.volumeBoostWarningMessage,
          style = MaterialTheme.typography.bodyMedium,
          color = palette.textSecondary,
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            hasAcknowledgedHighBoost = true
            val desired = (pendingHighBoostPercent ?: 450).coerceIn(398, 797)
            pendingHighBoostPercent = null
            showHighBoostWarningDialog = false
            onSetVolumeBoost(desired)
          },
          modifier = Modifier.testTag("volume_boost_ignore_btn"),
        ) {
          Text(
            text = strings.volumeBoostIgnore,
            fontWeight = FontWeight.Bold,
            color = palette.accentGlow,
          )
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            pendingHighBoostPercent = null
            showHighBoostWarningDialog = false
            onSetVolumeBoost(playback.volumeBoostPercent.coerceAtMost(397))
          },
          modifier = Modifier.testTag("volume_boost_cancel_btn"),
        ) {
          Text(
            text = strings.cancel,
            color = palette.textSecondary,
          )
        }
      },
      modifier = Modifier.testTag("volume_boost_warning_dialog"),
    )
  }
}

@Composable
private fun EqCurvePreview(
  gainsDb: List<Float>,
  isEnabled: Boolean,
  curveAccent: Color,
  highlightColor: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val midY = h / 2f

    // 0dB center line & +6/-6dB dashed guides
    drawLine(
      color = Color(0x40FFFFFF),
      start = Offset(0f, midY),
      end = Offset(w, midY),
      strokeWidth = 1.dp.toPx(),
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
    )

    val count = AudioEqualizerBus.BAND_COUNT
    val pts =
      (0 until count).map { idx ->
        val x = w * (0.08f + 0.84f * (idx.toFloat() / (count - 1).toFloat()))
        val g = if (isEnabled) gainsDb.getOrElse(idx) { 0f } else 0f
        val norm = (-g / AudioEqualizerBus.MAX_DB).coerceIn(-1f, 1f)
        val y = midY + norm * (h * 0.38f)
        Offset(x, y)
      }

    val curvePath = Path()
    val fillPath = Path()
    if (pts.isNotEmpty()) {
      curvePath.moveTo(0f, pts.first().y)
      curvePath.lineTo(pts.first().x, pts.first().y)
      fillPath.moveTo(0f, h)
      fillPath.lineTo(0f, pts.first().y)
      fillPath.lineTo(pts.first().x, pts.first().y)

      for (i in 0 until pts.lastIndex) {
        val p0 = pts[i]
        val p1 = pts[i + 1]
        val cx = (p0.x + p1.x) * 0.5f
        curvePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
        fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
      }

      curvePath.lineTo(w, pts.last().y)
      fillPath.lineTo(w, pts.last().y)
      fillPath.lineTo(w, h)
      fillPath.close()

      drawPath(
        path = fillPath,
        brush =
          Brush.verticalGradient(
            listOf(curveAccent.copy(alpha = if (isEnabled) 0.34f else 0.12f), Color.Transparent)
          ),
      )
      drawPath(
        path = curvePath,
        color = if (isEnabled) highlightColor else Color(0x66FFFFFF),
        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
      )

      pts.forEach { p ->
        drawCircle(
          color = curveAccent.copy(alpha = 0.45f),
          radius = 6.dp.toPx(),
          center = p,
        )
        drawCircle(
          color = highlightColor,
          radius = 3.2.dp.toPx(),
          center = p,
        )
      }
    }
  }
}

@Composable
private fun VerticalEqSliderColumn(
  freqLabel: String,
  gainDb: Float,
  enabled: Boolean,
  accentColor: Color,
  trackHighlight: Color,
  textSecondary: Color,
  isGlassTheme: Boolean,
  sliderTrackBg: Color,
  onChangeGain: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val rounded = gainDb.roundToInt()
  val dbText = if (rounded > 0) "+${rounded}dB" else "${rounded}dB"

  Column(
    modifier = modifier.padding(horizontal = 3.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = dbText,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      color = if (enabled) accentColor else textSecondary,
      textAlign = TextAlign.Center,
      maxLines = 1,
    )
    Spacer(modifier = Modifier.height(6.dp))

    BoxWithConstraints(
      modifier =
        Modifier.width(42.dp)
          .height(152.dp)
          .clip(RoundedCornerShape(21.dp))
          .background(if (isGlassTheme) sliderTrackBg.copy(alpha = 0.72f) else sliderTrackBg)
          .border(1.dp, textSecondary.copy(alpha = 0.25f), RoundedCornerShape(21.dp))
          .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures { offset ->
              val frac = 1f - (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
              val db =
                AudioEqualizerBus.MIN_DB +
                  frac * (AudioEqualizerBus.MAX_DB - AudioEqualizerBus.MIN_DB)
              onChangeGain(db)
            }
          }
          .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectVerticalDragGestures { change, _ ->
              change.consume()
              val frac = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
              val db =
                AudioEqualizerBus.MIN_DB +
                  frac * (AudioEqualizerBus.MAX_DB - AudioEqualizerBus.MIN_DB)
              onChangeGain(db)
            }
          },
      contentAlignment = Alignment.BottomCenter,
    ) {
      val fraction =
        ((gainDb - AudioEqualizerBus.MIN_DB) /
            (AudioEqualizerBus.MAX_DB - AudioEqualizerBus.MIN_DB))
          .coerceIn(0f, 1f)
      val trackHeightPx = constraints.maxHeight.toFloat()
      val thumbSizePx = with(androidx.compose.ui.platform.LocalDensity.current) { 20.dp.toPx() }
      val usablePx = (trackHeightPx - thumbSizePx - 8f).coerceAtLeast(1f)
      val offsetY = ((1f - fraction) * usablePx + 4f).roundToInt()

      // Active fill from bottom
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .fillMaxHeight(fraction.coerceAtLeast(0.05f))
            .background(
              Brush.verticalGradient(
                listOf(
                  (if (enabled) trackHighlight else Color.Gray).copy(alpha = 0.85f),
                  (if (enabled) trackHighlight else Color.Gray).copy(alpha = 0.25f),
                )
              )
            )
      )

      // Thumb knob
      Box(
        modifier =
          Modifier.align(Alignment.TopCenter)
            .offset { IntOffset(0, offsetY) }
            .size(20.dp)
            .clip(CircleShape)
            .background(if (enabled) accentColor else textSecondary)
            .border(1.5.dp, Color.Black.copy(alpha = 0.45f), CircleShape)
      )
    }

    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = freqLabel,
      style = MaterialTheme.typography.labelSmall,
      color = textSecondary,
      textAlign = TextAlign.Center,
      maxLines = 1,
    )
  }
}

private fun presetLabel(preset: EqualizerPreset, strings: com.example.localization.AppStrings): String =
  when (preset) {
    EqualizerPreset.FLAT -> strings.equalizerPresetFlat
    EqualizerPreset.BASS_BOOST -> strings.equalizerPresetBassBoost
    EqualizerPreset.ROCK -> strings.equalizerPresetRock
    EqualizerPreset.POP -> strings.equalizerPresetPop
    EqualizerPreset.CLASSICAL -> strings.equalizerPresetClassical
    EqualizerPreset.VOCAL_PODCAST -> strings.equalizerPresetVocal
  }
