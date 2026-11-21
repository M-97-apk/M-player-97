package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.LocalAppStrings
import com.example.model.SleepTimerMode
import com.example.model.SleepTimerState
import com.example.ui.theme.LocalMPlayerPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
  sleepTimerState: SleepTimerState,
  onSelectMinutes: (Int) -> Unit,
  onSelectEndOfTrack: () -> Unit,
  onCancelTimer: () -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val sheetBg = palette.sheetBg
  val cardFill = palette.sheetCardFill
  val cardBorder = palette.sheetCardBorder
  val accentColor = palette.sheetAccentText
  val progressColor = palette.sheetActiveAccent
  val textPrimary = palette.sheetTextPrimary
  val textSecondary = palette.sheetTextSecondary

  val isActive = sleepTimerState.mode != SleepTimerMode.OFF
  var showCustomPicker by remember { mutableStateOf(false) }
  var customHours by remember { mutableIntStateOf(0) }
  var customMinutes by remember { mutableIntStateOf(20) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = sheetBg,
    contentColor = textPrimary,
    tonalElevation = 0.dp,
    modifier = Modifier.testTag("sleep_timer_sheet"),
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
            imageVector = Icons.Rounded.Bedtime,
            contentDescription = strings.sleepTimerTitle,
            tint = palette.sectionHelperIconColor,
            modifier = Modifier.size(26.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = strings.sleepTimerTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text =
                if (isActive) {
                  if (sleepTimerState.isFadingOut) strings.sleepTimerFadingOut
                  else strings.sleepTimerRemainingFormat(formatDuration(sleepTimerState.remainingMs))
                } else {
                  strings.sleepTimerOff
                },
              style = MaterialTheme.typography.bodySmall,
              color = if (isActive) accentColor else textSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(48.dp).testTag("sleep_timer_close"),
        ) {
          Icon(imageVector = Icons.Rounded.Close, contentDescription = strings.dismiss, tint = textSecondary)
        }
      }

      if (isActive) {
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = cardFill,
          border = BorderStroke(1.dp, cardBorder),
          shadowElevation = 0.dp,
          tonalElevation = 0.dp,
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text =
                  if (sleepTimerState.mode == SleepTimerMode.END_OF_TRACK) {
                    strings.sleepTimerEndOfTrack
                  } else {
                    strings.sleepTimerTitle
                  },
                style = MaterialTheme.typography.labelLarge,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = formatDuration(sleepTimerState.remainingMs),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            val total = sleepTimerState.totalDurationMs.coerceAtLeast(1L)
            val fraction = (sleepTimerState.remainingMs.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
              progress = { fraction },
              modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
              color = progressColor,
              trackColor = Color(0x33FFFFFF),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      val durationOptions =
        listOf(
          5 to strings.sleepTimer5Min,
          10 to strings.sleepTimer10Min,
          15 to strings.sleepTimer15Min,
          30 to strings.sleepTimer30Min,
          45 to strings.sleepTimer45Min,
          60 to strings.sleepTimer60Min,
        )

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        durationOptions.chunked(2).forEach { pair ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            pair.forEach { (minutes, label) ->
              val isSelected =
                sleepTimerState.mode == SleepTimerMode.DURATION &&
                  sleepTimerState.totalDurationMs == minutes * 60_000L
              SleepTimerOptionCard(
                label = label,
                selected = isSelected,
                cardFill = cardFill,
                selectedFill = palette.sheetSelectedFill,
                cardBorder = cardBorder,
                accentColor = accentColor,
                textPrimary = textPrimary,
                onClick = {
                  onSelectMinutes(minutes)
                  onDismiss()
                },
                modifier = Modifier.weight(1f).testTag("sleep_timer_${minutes}m"),
              )
            }
          }
        }

        // Manual Custom Time Picker Option Card
        val isCustomSelected =
          sleepTimerState.mode == SleepTimerMode.DURATION &&
            sleepTimerState.totalDurationMs > 0L &&
            durationOptions.none { it.first * 60_000L == sleepTimerState.totalDurationMs }

        SleepTimerOptionCard(
          label = strings.sleepTimerCustom,
          selected = isCustomSelected || showCustomPicker,
          cardFill = cardFill,
          selectedFill = palette.sheetSelectedFill,
          cardBorder = cardBorder,
          accentColor = accentColor,
          textPrimary = textPrimary,
          onClick = { showCustomPicker = !showCustomPicker },
          modifier = Modifier.fillMaxWidth().testTag("sleep_timer_custom_btn"),
        )

        if (showCustomPicker) {
          Surface(
            modifier = Modifier.fillMaxWidth().testTag("sleep_timer_custom_picker"),
            shape = RoundedCornerShape(18.dp),
            color = palette.sheetSelectedFill.copy(alpha = 0.55f),
            border = BorderStroke(1.2.dp, cardBorder),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                // Hours Stepper
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = strings.sleepTimerHoursLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = textSecondary,
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                      onClick = { customHours = (customHours - 1).coerceAtLeast(0) },
                      modifier = Modifier.size(34.dp).testTag("sleep_timer_custom_hour_minus"),
                    ) {
                      Icon(imageVector = Icons.Rounded.Remove, contentDescription = null, tint = accentColor)
                    }
                    Text(
                      text = String.format(java.util.Locale.US, "%02d", customHours),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = textPrimary,
                      modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    IconButton(
                      onClick = { customHours = (customHours + 1).coerceIn(0, 12) },
                      modifier = Modifier.size(34.dp).testTag("sleep_timer_custom_hour_plus"),
                    ) {
                      Icon(imageVector = Icons.Rounded.Add, contentDescription = null, tint = accentColor)
                    }
                  }
                }

                Text(
                  text = ":",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = accentColor,
                )

                // Minutes Stepper
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = strings.sleepTimerMinutesLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = textSecondary,
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                      onClick = { customMinutes = (customMinutes - 5).coerceAtLeast(1) },
                      modifier = Modifier.size(34.dp).testTag("sleep_timer_custom_min_minus"),
                    ) {
                      Icon(imageVector = Icons.Rounded.Remove, contentDescription = null, tint = accentColor)
                    }
                    Text(
                      text = String.format(java.util.Locale.US, "%02d", customMinutes),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = textPrimary,
                      modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    IconButton(
                      onClick = { customMinutes = (customMinutes + 5).coerceIn(1, 180) },
                      modifier = Modifier.size(34.dp).testTag("sleep_timer_custom_min_plus"),
                    ) {
                      Icon(imageVector = Icons.Rounded.Add, contentDescription = null, tint = accentColor)
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))
              ThemedPrimaryButton(
                onClick = {
                  val totalMin = customHours * 60 + customMinutes
                  if (totalMin > 0) {
                    onSelectMinutes(totalMin)
                    onDismiss()
                  }
                },
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("sleep_timer_custom_confirm"),
              ) {
                Text(strings.sleepTimerStartCustom)
              }
            }
          }
        }

        // End of current track option
        val isEndSelected = sleepTimerState.mode == SleepTimerMode.END_OF_TRACK
        Surface(
          modifier = Modifier.fillMaxWidth().testTag("sleep_timer_end_of_track"),
          shape = RoundedCornerShape(16.dp),
          color = if (isEndSelected) palette.sheetSelectedFill else cardFill,
          border = BorderStroke(if (isEndSelected) 1.5.dp else 1.dp, cardBorder),
          shadowElevation = 0.dp,
          tonalElevation = 0.dp,
          onClick = {
            onSelectEndOfTrack()
            onDismiss()
          },
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(
                imageVector = Icons.Rounded.MusicOff,
                contentDescription = null,
                tint = if (isEndSelected) accentColor else textSecondary,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = strings.sleepTimerEndOfTrack,
                style = MaterialTheme.typography.titleSmall,
                color = if (isEndSelected) accentColor else textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            if (isEndSelected) {
              Icon(imageVector = Icons.Rounded.Check, contentDescription = null, tint = accentColor)
            }
          }
        }

        if (isActive) {
          Surface(
            modifier = Modifier.fillMaxWidth().testTag("sleep_timer_cancel_btn"),
            shape = RoundedCornerShape(16.dp),
            color = palette.sheetSelectedFill.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            onClick = {
              onCancelTimer()
              onDismiss()
            },
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.TimerOff,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = strings.sleepTimerCancel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))
    }
  }
}

@Composable
private fun SleepTimerOptionCard(
  label: String,
  selected: Boolean,
  cardFill: Color,
  selectedFill: Color,
  cardBorder: Brush,
  accentColor: Color,
  textPrimary: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  val useActiveGlassGlow =
    selected &&
      palette.themeStyle != com.example.model.AppThemeStyle.PITCH_BLACK_GOLD &&
      !palette.isMinimalPlainBlack
  Surface(
    modifier =
      modifier.height(54.dp).then(
        if (useActiveGlassGlow) {
          Modifier.activeGlassOptionPillDecoration(selected = true, shape = RoundedCornerShape(16.dp))
        } else {
          Modifier
        }
      ),
    shape = RoundedCornerShape(16.dp),
    color = if (useActiveGlassGlow) Color.Transparent else if (selected) selectedFill else cardFill,
    border = if (useActiveGlassGlow) null else BorderStroke(if (selected) 1.5.dp else 1.dp, cardBorder),
    shadowElevation = 0.dp,
    tonalElevation = 0.dp,
    onClick = onClick,
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) accentColor else textPrimary,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
