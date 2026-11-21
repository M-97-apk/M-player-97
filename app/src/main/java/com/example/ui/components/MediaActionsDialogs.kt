package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.localization.LocalAppStrings
import com.example.model.AudioTrack
import com.example.model.ConversionFormat
import com.example.model.ConversionProgressState
import com.example.ui.theme.LocalMPlayerPalette

@Composable
fun FavoriteHeartButton(
  isFavorite: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
  buttonSize: Dp = 38.dp,
  iconSize: Dp = 20.dp,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  ThemedIconButton(
    onClick = onToggle,
    buttonSize = buttonSize,
    modifier = modifier,
  ) {
    Icon(
      imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
      contentDescription = if (isFavorite) strings.likeToggleRemove else strings.likeToggleAdd,
      tint = if (isFavorite) palette.accentGlow else palette.textSecondary,
      modifier = Modifier.size(iconSize),
    )
  }
}

@Composable
fun MediaOptionsMenuButton(
  track: AudioTrack,
  onPlay: () -> Unit,
  onAddToPlaylist: (() -> Unit)?,
  onPlayVideoNormal: () -> Unit,
  onPlayVideoAudioOnly: () -> Unit,
  onConvertFormat: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
  buttonSize: Dp = 36.dp,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = modifier) {
    ThemedIconButton(
      onClick = { expanded = true },
      buttonSize = buttonSize,
    ) {
      Icon(
        imageVector = Icons.Rounded.MoreVert,
        contentDescription = strings.videoOptionsTitle,
        tint = palette.textSecondary,
        modifier = Modifier.size(18.dp),
      )
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      containerColor = palette.sheetBg,
      shadowElevation = 0.dp,
      tonalElevation = 0.dp,
    ) {
      if (track.isVideo) {
        DropdownMenuItem(
          text = { Text(strings.playVideoNormal, color = palette.textPrimary) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.PlayArrow,
              contentDescription = null,
              tint = palette.sectionHelperIconColor,
            )
          },
          onClick = {
            expanded = false
            onPlayVideoNormal()
          },
        )
        DropdownMenuItem(
          text = { Text(strings.playVideoAudioOnly, color = palette.textPrimary) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.Headphones,
              contentDescription = null,
              tint = palette.sectionHelperIconColor,
            )
          },
          onClick = {
            expanded = false
            onPlayVideoAudioOnly()
          },
        )
        DropdownMenuItem(
          text = {
            Text(
              strings.convertFormatOption,
              color = palette.sectionHelperTextColor,
              fontWeight = FontWeight.SemiBold,
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.AudioFile,
              contentDescription = null,
              tint = palette.sectionHelperIconColor,
            )
          },
          onClick = {
            expanded = false
            onConvertFormat()
          },
        )
      } else {
        DropdownMenuItem(
          text = { Text(strings.playAudioTrack, color = palette.textPrimary) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.PlayArrow,
              contentDescription = null,
              tint = palette.sectionHelperIconColor,
            )
          },
          onClick = {
            expanded = false
            onPlay()
          },
        )
      }

      if (onAddToPlaylist != null) {
        DropdownMenuItem(
          text = { Text(strings.addToPlaylist, color = palette.textPrimary) },
          leadingIcon = {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
              contentDescription = null,
              tint = palette.sectionHelperIconColor,
            )
          },
          onClick = {
            expanded = false
            onAddToPlaylist()
          },
        )
      }

      DropdownMenuItem(
        text = { Text(strings.delete, color = Color(0xFFFF6B6B), fontWeight = FontWeight.SemiBold) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Rounded.Delete,
            contentDescription = null,
            tint = Color(0xFFFF6B6B),
          )
        },
        modifier = Modifier.testTag("delete_media_option_${track.id}"),
        onClick = {
          expanded = false
          onDelete()
        },
      )
    }
  }
}

@Composable
fun ConvertFormatDialog(
  track: AudioTrack,
  hasAuthorizedFolder: Boolean,
  onAuthorizeFolder: () -> Unit,
  onSelectFormat: (ConversionFormat) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  val shape = RoundedCornerShape(24.dp)

  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .clip(shape)
          .background(palette.sheetBg)
          .background(palette.glassCardFrostBrush)
          .border(palette.borderStrokeWidth, palette.borderBrush, shape)
          .padding(18.dp)
          .testTag("convert_format_dialog")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = Icons.Rounded.Transform,
              contentDescription = null,
              tint = palette.sectionHelperIconColor,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = strings.convertDialogTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = palette.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          ThemedIconButton(onClick = onDismiss, buttonSize = 36.dp) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = strings.cancel,
              tint = palette.textSecondary,
              modifier = Modifier.size(18.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = track.title,
          style = MaterialTheme.typography.titleSmall,
          color = palette.accentGlow,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = strings.convertChoosePrompt,
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Output folder status pill ("M player 97")
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(palette.glassBadgeFillBrush)
              .border(1.dp, palette.borderBrush, RoundedCornerShape(14.dp))
              .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = if (hasAuthorizedFolder) Icons.Rounded.CheckCircle else Icons.Rounded.Folder,
              contentDescription = null,
              tint = palette.sectionHelperIconColor,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (hasAuthorizedFolder) strings.converterFolderAuthorized else strings.converterFolderNeeded,
              style = MaterialTheme.typography.labelSmall,
              color = palette.textPrimary,
              modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
              onClick = onAuthorizeFolder,
              border = BorderStroke(palette.borderStrokeWidth, palette.buttonBorderColor),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.height(34.dp).testTag("convert_choose_folder_btn"),
            ) {
              Text(
                text = strings.converterFolderAuthorizeBtn,
                style = MaterialTheme.typography.labelSmall,
                color = palette.sectionHelperTextColor,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Option 1: Convert Full Video (.mp4)
        ConverterOptionCard(
          icon = Icons.Rounded.VideoFile,
          title = strings.convertFullVideo,
          subtitle = strings.convertFullVideoDesc,
          onClick = { onSelectFormat(ConversionFormat.MP4_VIDEO) },
          modifier = Modifier.testTag("convert_mp4_option_btn"),
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Option 2: Create MP3 Audio Copy Only (.mp3)
        ConverterOptionCard(
          icon = Icons.Rounded.AudioFile,
          title = strings.convertMp3AudioOnly,
          subtitle = strings.convertMp3AudioOnlyDesc,
          onClick = { onSelectFormat(ConversionFormat.MP3_AUDIO) },
          modifier = Modifier.testTag("convert_mp3_option_btn"),
        )
      }
    }
  }
}

@Composable
private fun ConverterOptionCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  val shape = RoundedCornerShape(16.dp)
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(shape)
        .background(palette.glassBadgeActiveFillBrush)
        .border(palette.borderStrokeWidth, palette.buttonBorderColor, shape)
        .clickable(onClick = onClick)
        .padding(14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = palette.sectionHelperIconColor,
      modifier = Modifier.size(28.dp),
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = palette.textPrimary,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = palette.textSecondary,
      )
    }
  }
}

@Composable
fun ConversionProgressDialog(
  state: ConversionProgressState,
  onDismiss: () -> Unit,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  val shape = RoundedCornerShape(24.dp)

  Dialog(onDismissRequest = { if (!state.isRunning) onDismiss() }) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .clip(shape)
          .background(palette.sheetBg)
          .background(palette.glassCardFrostBrush)
          .border(palette.borderStrokeWidth, palette.borderBrush, shape)
          .padding(18.dp)
          .testTag("conversion_progress_dialog")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = strings.convertDialogTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
          )
          if (!state.isRunning) {
            ThemedIconButton(onClick = onDismiss, buttonSize = 34.dp) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = strings.dismiss,
                tint = palette.textSecondary,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = state.sourceTitle,
          style = MaterialTheme.typography.titleSmall,
          color = palette.accentGlow,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(12.dp))
        when {
          state.isRunning -> {
            val pct = (state.progress * 100f).toInt().coerceIn(0, 100)
            LinearProgressIndicator(
              progress = { state.progress.coerceIn(0f, 1f) },
              modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
              color = palette.accentGlow,
              trackColor = palette.textSecondary.copy(alpha = 0.20f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = strings.convertingProgressFormat(pct),
              style = MaterialTheme.typography.bodySmall,
              color = palette.textSecondary,
            )
          }
          state.outputDisplayPath != null -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = palette.sectionHelperIconColor,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = strings.conversionSuccessFormat(state.outputDisplayPath),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textPrimary,
              )
            }
            Spacer(modifier = Modifier.height(14.dp))
            ThemedPrimaryButton(
              onClick = onDismiss,
              modifier = Modifier.fillMaxWidth().height(46.dp),
            ) {
              Text(strings.dismiss)
            }
          }
          state.errorMessage != null -> {
            Text(
              text = "${strings.conversionFailedLabel}: ${state.errorMessage}",
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFFFF6B6B),
            )
            Spacer(modifier = Modifier.height(14.dp))
            ThemedPrimaryButton(
              onClick = onDismiss,
              modifier = Modifier.fillMaxWidth().height(46.dp),
            ) {
              Text(strings.dismiss)
            }
          }
        }
      }
    }
  }
}
