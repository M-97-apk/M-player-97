package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.localization.LocalAppStrings
import com.example.model.AudioTrack
import com.example.model.PlaybackHistoryItem
import com.example.ui.components.ArtworkThumbnail
import com.example.ui.components.FavoriteHeartButton
import com.example.ui.components.ScrollToTopFloatingButton
import com.example.ui.components.ThemedCard
import com.example.ui.components.ThemedIconButton
import com.example.ui.components.ThemedPrimaryButton
import com.example.ui.components.formatDuration
import com.example.ui.theme.LocalMPlayerPalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
  historyItems: List<PlaybackHistoryItem>,
  favoriteUris: Set<String>,
  onPlayFromHistory: (PlaybackHistoryItem, Boolean) -> Unit,
  onToggleFavorite: (AudioTrack) -> Unit,
  onDeleteItem: (Long) -> Unit,
  onClearAll: () -> Unit,
  isRefreshing: Boolean = false,
  onRefresh: () -> Unit = {},
  bottomOverlayPadding: Dp = 148.dp,
  fabBottomOffset: Dp = 96.dp,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  var pendingResumeItem by remember { mutableStateOf<PlaybackHistoryItem?>(null) }
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val pullRefreshState = rememberPullToRefreshState()
  val showScrollToTop by
    remember {
      derivedStateOf {
        listState.firstVisibleItemIndex >= 2 ||
          (listState.firstVisibleItemIndex >= 1 && listState.firstVisibleItemScrollOffset > 120)
      }
    }

  Column(modifier = modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = strings.historyTitle,
        style = MaterialTheme.typography.headlineMedium,
        color = palette.textPrimary,
      )
      if (historyItems.isNotEmpty()) {
        TextButton(
          onClick = onClearAll,
          modifier = Modifier.testTag("history_clear_all_btn"),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteSweep,
            contentDescription = strings.historyClearAll,
            tint = palette.sectionHelperIconColor,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = strings.historyClearAll,
            style = MaterialTheme.typography.labelMedium,
            color = palette.sectionHelperTextColor,
          )
        }
      }
    }

    PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = onRefresh,
      state = pullRefreshState,
      indicator = {
        PullToRefreshDefaults.Indicator(
          state = pullRefreshState,
          isRefreshing = isRefreshing,
          containerColor = palette.sheetBg,
          color = palette.accentGlow,
          modifier = Modifier.align(Alignment.TopCenter),
        )
      },
      modifier = Modifier.weight(1f).fillMaxWidth().testTag("history_pull_to_refresh"),
    ) {
      if (historyItems.isEmpty()) {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = bottomOverlayPadding + 32.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Rounded.History,
              contentDescription = null,
              tint = palette.sectionHelperIconColor,
              modifier = Modifier.size(64.dp),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = strings.historyEmptyTitle,
              style = MaterialTheme.typography.titleLarge,
              color = palette.textPrimary,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = strings.historyEmptySubtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = palette.textSecondary,
              textAlign = TextAlign.Center,
            )
          }
        }
      } else {
        LazyColumn(
          state = listState,
          contentPadding =
            PaddingValues(start = 20.dp, end = 20.dp, bottom = bottomOverlayPadding + 72.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize().testTag("history_tracks_list"),
        ) {
          items(historyItems, key = { it.eventId }) { item ->
          val isFav = item.track.contentUri.toString() in favoriteUris
          val savedPosFormatted = formatDuration(item.lastPositionMs)
          val playedTimeStr = item.playedAtIso.take(16).replace('T', ' ')

          ThemedCard(
            onClick = {
              if (item.lastPositionMs > 0L) {
                pendingResumeItem = item
              } else {
                onPlayFromHistory(item, false)
              }
            },
            cornerRadius = 18.dp,
            modifier = Modifier.fillMaxWidth().testTag("history_item_${item.eventId}"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              ArtworkThumbnail(
                model = item.track.albumArtUri,
                contentDescription = item.track.title,
                size = 50.dp,
                cornerRadius = 12.dp,
                isVideo = item.track.isVideo,
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.track.title,
                  style = MaterialTheme.typography.titleSmall,
                  color = palette.textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier =
                      Modifier.clip(RoundedCornerShape(6.dp))
                        .background(palette.glassBadgeFillBrush)
                        .border(0.8.dp, palette.borderBrush, RoundedCornerShape(6.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                  ) {
                    Text(
                      text = if (item.track.isVideo) strings.videoBadgeLabel else strings.audioBadgeLabel,
                      style = MaterialTheme.typography.labelSmall,
                      color = palette.accentGlow,
                    )
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text =
                      if (item.lastPositionMs > 0L) {
                        "${strings.historyPausedAtFormat(savedPosFormatted)} • $playedTimeStr"
                      } else {
                        "${formatDuration(item.track.durationMs)} • $playedTimeStr"
                      },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              FavoriteHeartButton(
                isFavorite = isFav,
                onToggle = { onToggleFavorite(item.track) },
                buttonSize = 36.dp,
                iconSize = 18.dp,
                modifier = Modifier.testTag("history_fav_${item.eventId}"),
              )

              ThemedIconButton(
                onClick = { onDeleteItem(item.eventId) },
                buttonSize = 36.dp,
                modifier = Modifier.testTag("history_delete_${item.eventId}"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Close,
                  contentDescription = strings.delete,
                  tint = palette.textSecondary,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
      }
      }

      ScrollToTopFloatingButton(
        visible = showScrollToTop,
        onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
        buttonTestTag = "history_scroll_to_top_btn",
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = fabBottomOffset),
      )
    }
  }

  val target = pendingResumeItem
  if (target != null) {
    ResumeOrRestartDialog(
      item = target,
      onResume = {
        pendingResumeItem = null
        onPlayFromHistory(target, true)
      },
      onRestart = {
        pendingResumeItem = null
        onPlayFromHistory(target, false)
      },
      onDismiss = { pendingResumeItem = null },
    )
  }
}

@Composable
private fun ResumeOrRestartDialog(
  item: PlaybackHistoryItem,
  onResume: () -> Unit,
  onRestart: () -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  val posText = formatDuration(item.lastPositionMs)
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
          .testTag("resume_restart_dialog")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = Icons.Rounded.History,
              contentDescription = null,
              tint = palette.sectionHelperIconColor,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = strings.resumePromptTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = palette.textPrimary,
            )
          }
          ThemedIconButton(onClick = onDismiss, buttonSize = 34.dp) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = strings.cancel,
              tint = palette.textSecondary,
              modifier = Modifier.size(18.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = strings.resumePromptMessageFormat(item.track.title, posText),
          style = MaterialTheme.typography.bodyMedium,
          color = palette.textSecondary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        ThemedPrimaryButton(
          onClick = onResume,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("history_resume_btn"),
        ) {
          Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = strings.resumeFromPositionBtnFormat(posText),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = onRestart,
          border = BorderStroke(palette.borderStrokeWidth, palette.buttonBorderColor),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("history_restart_btn"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Replay,
            contentDescription = null,
            tint = palette.sectionHelperIconColor,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = strings.restartFromBeginningBtn,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
