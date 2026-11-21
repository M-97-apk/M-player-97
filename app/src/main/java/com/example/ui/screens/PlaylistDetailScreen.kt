package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.localization.LocalAppStrings
import com.example.model.AudioTrack
import com.example.model.PlaylistSortOption
import com.example.model.PlaylistSummary
import com.example.model.PlaylistTrackEntry
import com.example.ui.components.ArtworkThumbnail
import com.example.ui.components.FavoriteHeartButton
import com.example.ui.components.MediaOptionsMenuButton
import com.example.ui.components.PlaylistCoverArt
import com.example.ui.components.ScrollToTopFloatingButton
import com.example.ui.components.ThemedCard
import com.example.ui.components.ThemedIconButton
import com.example.ui.components.ThemedPrimaryButton
import com.example.ui.components.formatDuration
import com.example.ui.theme.LocalMPlayerPalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
  playlist: PlaylistSummary?,
  entries: List<PlaylistTrackEntry>,
  libraryTracks: List<AudioTrack>,
  favoriteUris: Set<String> = emptySet(),
  currentTrackId: Long?,
  onBack: () -> Unit,
  onRenamePlaylist: (String) -> Unit,
  onPickCoverUri: (Uri) -> Unit,
  onRemoveCover: () -> Unit,
  onSelectSort: (PlaylistSortOption) -> Unit,
  onPlayPlaylist: (List<PlaylistTrackEntry>, Int) -> Unit,
  onAddTracks: (List<AudioTrack>) -> Unit,
  onRemoveEntry: (Long) -> Unit,
  onPersistManualOrder: (List<PlaylistTrackEntry>) -> Unit,
  onToggleFavorite: (AudioTrack) -> Unit = {},
  onPlayVideoNormal: (AudioTrack) -> Unit = {},
  onPlayVideoAudioOnly: (AudioTrack) -> Unit = {},
  onOpenConvertDialog: (AudioTrack) -> Unit = {},
  onDeleteTrack: (AudioTrack) -> Unit = {},
  bottomOverlayPadding: androidx.compose.ui.unit.Dp = 140.dp,
  fabBottomOffset: androidx.compose.ui.unit.Dp = 96.dp,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current

  var showSortMenu by remember { mutableStateOf(false) }
  var showRenameDialog by remember { mutableStateOf(false) }
  var showAddTracksSheet by remember { mutableStateOf(false) }
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val showScrollToTop by
    remember {
      derivedStateOf {
        listState.firstVisibleItemIndex >= 2 ||
          (listState.firstVisibleItemIndex >= 1 && listState.firstVisibleItemScrollOffset > 120)
      }
    }

  // Zero-permission Android Photo Picker for custom gallery covers
  val coverPickerLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
      uri?.let(onPickCoverUri)
    }

  val localOrderedEntries = remember { mutableStateListOf<PlaylistTrackEntry>() }
  LaunchedEffect(entries) {
    localOrderedEntries.clear()
    localOrderedEntries.addAll(entries)
  }

  var draggedEntryId by remember { mutableStateOf<Long?>(null) }
  var dragDeltaY by remember { mutableFloatStateOf(0f) }

  val isManualMode = (playlist?.sortOption ?: PlaylistSortOption.MANUAL) == PlaylistSortOption.MANUAL

  Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
    // Top Navigation Bar
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      ThemedIconButton(
        onClick = onBack,
        modifier = Modifier.testTag("playlist_back_btn"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Back",
          tint = palette.textPrimary,
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        if (playlist?.isFavorites != true) {
          ThemedIconButton(
            onClick = { showRenameDialog = true },
            modifier = Modifier.testTag("playlist_rename_btn"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Edit,
              contentDescription = strings.rename,
              tint = palette.textPrimary,
            )
          }
        }
        Box {
          ThemedIconButton(
            onClick = { showSortMenu = true },
            modifier = Modifier.testTag("playlist_sort_btn"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.Sort,
              contentDescription = strings.sortTitle,
              tint = palette.sectionHelperIconColor,
            )
          }
          DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { showSortMenu = false },
            containerColor = palette.sheetBg,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
          ) {
            val options =
              listOf(
                PlaylistSortOption.MANUAL to strings.sortManual,
                PlaylistSortOption.ALPHABETICAL_ASC to strings.sortAlphaAsc,
                PlaylistSortOption.ALPHABETICAL_DESC to strings.sortAlphaDesc,
                PlaylistSortOption.DATE_ADDED_DESC to strings.sortDateAddedDesc,
                PlaylistSortOption.DATE_ADDED_ASC to strings.sortDateAddedAsc,
                PlaylistSortOption.DATE_MODIFIED_DESC to strings.sortDateModifiedDesc,
              )
            options.forEach { (option, label) ->
              val selected = playlist?.sortOption == option
              DropdownMenuItem(
                text = {
                  Text(
                    text = label,
                    color = if (selected) palette.accentGlow else palette.textPrimary,
                  )
                },
                trailingIcon = {
                  if (selected) {
                    Icon(
                      imageVector = Icons.Rounded.Check,
                      contentDescription = null,
                      tint = palette.accentGlow,
                    )
                  }
                },
                onClick = {
                  onSelectSort(option)
                  showSortMenu = false
                },
              )
            }
          }
        }
      }
    }

    // Playlist Hero Card with Custom Cover Picker
    ThemedCard(
      cornerRadius = 24.dp,
      elevated = true,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PlaylistCoverArt(
          coverPath = playlist?.coverPath,
          contentDescription = playlist?.name ?: strings.playlistsTitle,
          modifier =
            Modifier.size(96.dp)
              .clickable {
                coverPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              }
              .testTag("playlist_cover_picker"),
          cornerRadius = 18.dp,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text =
              if (playlist?.isFavorites == true) strings.favoritesPlaylistName
              else playlist?.name.orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
            color = palette.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = strings.tracksCountFormat(localOrderedEntries.size),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                coverPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              border = BorderStroke(palette.borderStrokeWidth, palette.buttonBorderColor),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.height(36.dp).testTag("change_cover_btn"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                tint = palette.sectionHelperIconColor,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = strings.changeCover,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textPrimary,
              )
            }
            if (playlist?.coverPath != null) {
              TextButton(
                onClick = onRemoveCover,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp),
              ) {
                Text(
                  text = strings.removeCover,
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.textSecondary,
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Action Row: Play + Add Songs
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      ThemedPrimaryButton(
        onClick = {
          if (localOrderedEntries.isNotEmpty()) {
            onPlayPlaylist(localOrderedEntries.toList(), 0)
          }
        },
        enabled = localOrderedEntries.isNotEmpty(),
        modifier = Modifier.weight(1f).height(48.dp).testTag("playlist_play_btn"),
      ) {
        Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text(strings.playAll)
      }

      ThemedCard(
        onClick = { showAddTracksSheet = true },
        cornerRadius = 16.dp,
        modifier = Modifier.weight(1f).height(48.dp).testTag("playlist_add_songs_btn"),
      ) {
        Row(
          modifier = Modifier.fillMaxSize(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Icon(imageVector = Icons.Rounded.Add, contentDescription = null, tint = palette.sectionHelperIconColor)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = strings.addTracks,
            style = MaterialTheme.typography.labelLarge,
            color = palette.textPrimary,
          )
        }
      }
    }

    if (isManualMode && localOrderedEntries.size > 1) {
      Text(
        text = strings.manualDragTip,
        style = MaterialTheme.typography.labelSmall,
        color = palette.sectionHelperTextColor,
        modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
      )
    } else {
      Spacer(modifier = Modifier.height(10.dp))
    }

    if (localOrderedEntries.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = strings.emptyPlaylistTracksTitle,
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = strings.emptyPlaylistTracksSubtitle,
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        itemsIndexed(localOrderedEntries, key = { _, item -> item.entryId }) { index, entry ->
          val isDragging = draggedEntryId == entry.entryId
          val isCurrent = entry.track.id == currentTrackId

          ThemedCard(
            onClick = { onPlayPlaylist(localOrderedEntries.toList(), index) },
            cornerRadius = 18.dp,
            elevated = isCurrent || isDragging,
            modifier =
              Modifier.fillMaxWidth()
                .zIndex(if (isDragging) 10f else 0f)
                .graphicsLayer {
                  if (isDragging) {
                    translationY = dragDeltaY
                    scaleX = 1.02f
                    scaleY = 1.02f
                  }
                }
                .then(
                  if (isManualMode) {
                    Modifier.pointerInput(entry.entryId, localOrderedEntries.size) {
                      detectDragGesturesAfterLongPress(
                        onDragStart = {
                          draggedEntryId = entry.entryId
                          dragDeltaY = 0f
                        },
                        onDragEnd = {
                          draggedEntryId = null
                          dragDeltaY = 0f
                          onPersistManualOrder(localOrderedEntries.toList())
                        },
                        onDragCancel = {
                          draggedEntryId = null
                          dragDeltaY = 0f
                        },
                        onDrag = { change, dragAmount ->
                          change.consume()
                          dragDeltaY += dragAmount.y
                          val rowHeightPx = 76.dp.toPx()
                          val currentIdx =
                            localOrderedEntries.indexOfFirst { it.entryId == entry.entryId }
                          if (currentIdx >= 0) {
                            if (dragDeltaY > rowHeightPx * 0.65f &&
                              currentIdx < localOrderedEntries.lastIndex
                            ) {
                              val moved = localOrderedEntries.removeAt(currentIdx)
                              localOrderedEntries.add(currentIdx + 1, moved)
                              dragDeltaY -= rowHeightPx
                            } else if (dragDeltaY < -rowHeightPx * 0.65f && currentIdx > 0) {
                              val moved = localOrderedEntries.removeAt(currentIdx)
                              localOrderedEntries.add(currentIdx - 1, moved)
                              dragDeltaY += rowHeightPx
                            }
                          }
                        },
                      )
                    }
                  } else {
                    Modifier
                  }
                )
                .testTag("playlist_track_${entry.entryId}"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              if (isManualMode) {
                Icon(
                  imageVector = Icons.Rounded.DragHandle,
                  contentDescription = null,
                  tint = palette.sectionHelperIconColor,
                  modifier = Modifier.padding(end = 8.dp).size(22.dp),
                )
              }

              ArtworkThumbnail(
                model = entry.track.albumArtUri,
                contentDescription = entry.track.title,
                size = 46.dp,
                cornerRadius = 12.dp,
                showActiveRim = isCurrent,
                isVideo = entry.track.isVideo,
              )

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = entry.track.title,
                  style = MaterialTheme.typography.titleSmall,
                  color = if (isCurrent) palette.accentGlow else palette.textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${entry.track.artist} • ${formatDuration(entry.track.durationMs)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              if (isManualMode && localOrderedEntries.size > 1) {
                IconButton(
                  onClick = {
                    if (index > 0) {
                      val item = localOrderedEntries.removeAt(index)
                      localOrderedEntries.add(index - 1, item)
                      onPersistManualOrder(localOrderedEntries.toList())
                    }
                  },
                  enabled = index > 0,
                  modifier = Modifier.size(36.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.ArrowUpward,
                    contentDescription = strings.moveUp,
                    tint =
                      if (index > 0) palette.textSecondary else palette.textSecondary.copy(alpha = 0.25f),
                    modifier = Modifier.size(18.dp),
                  )
                }
                IconButton(
                  onClick = {
                    if (index < localOrderedEntries.lastIndex) {
                      val item = localOrderedEntries.removeAt(index)
                      localOrderedEntries.add(index + 1, item)
                      onPersistManualOrder(localOrderedEntries.toList())
                    }
                  },
                  enabled = index < localOrderedEntries.lastIndex,
                  modifier = Modifier.size(36.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.ArrowDownward,
                    contentDescription = strings.moveDown,
                    tint =
                      if (index < localOrderedEntries.lastIndex) palette.textSecondary
                      else palette.textSecondary.copy(alpha = 0.25f),
                    modifier = Modifier.size(18.dp),
                  )
                }
              }

              FavoriteHeartButton(
                isFavorite = entry.track.contentUri.toString() in favoriteUris,
                onToggle = { onToggleFavorite(entry.track) },
                buttonSize = 34.dp,
                iconSize = 18.dp,
                modifier = Modifier.testTag("playlist_fav_${entry.entryId}"),
              )

              MediaOptionsMenuButton(
                track = entry.track,
                onPlay = { onPlayPlaylist(localOrderedEntries.toList(), index) },
                onAddToPlaylist = null,
                onPlayVideoNormal = { onPlayVideoNormal(entry.track) },
                onPlayVideoAudioOnly = { onPlayVideoAudioOnly(entry.track) },
                onConvertFormat = { onOpenConvertDialog(entry.track) },
                onDelete = { onDeleteTrack(entry.track) },
                buttonSize = 34.dp,
                modifier = Modifier.testTag("playlist_media_options_${entry.entryId}"),
              )

              IconButton(
                onClick = { onRemoveEntry(entry.entryId) },
                modifier = Modifier.size(36.dp).testTag("remove_entry_${entry.entryId}"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Close,
                  contentDescription = strings.removeFromPlaylist,
                  tint = palette.textSecondary,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
      }
    }
    }

    ScrollToTopFloatingButton(
      visible = showScrollToTop,
      onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
      buttonTestTag = "playlist_detail_scroll_to_top_btn",
      modifier =
        Modifier.align(Alignment.BottomEnd)
          .padding(end = 20.dp, bottom = fabBottomOffset),
    )
  }

  if (showRenameDialog && playlist != null) {
    var updatedName by remember { mutableStateOf(playlist.name) }
    AlertDialog(
      onDismissRequest = { showRenameDialog = false },
      containerColor = palette.cardSurfaceElevated,
      title = { Text(strings.rename, color = palette.textPrimary) },
      text = {
        OutlinedTextField(
          value = updatedName,
          onValueChange = { updatedName = it },
          singleLine = true,
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedTextColor = palette.textPrimary,
              unfocusedTextColor = palette.textPrimary,
              cursorColor = palette.accentGlow,
              focusedBorderColor = palette.accentGlow,
              unfocusedBorderColor = palette.buttonBorderColor,
            ),
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        ThemedPrimaryButton(
          onClick = {
            if (updatedName.isNotBlank()) {
              onRenamePlaylist(updatedName.trim())
              showRenameDialog = false
            }
          },
          enabled = updatedName.isNotBlank(),
        ) {
          Text(strings.save)
        }
      },
      dismissButton = {
        TextButton(onClick = { showRenameDialog = false }) {
          Text(strings.cancel, color = palette.textSecondary)
        }
      },
    )
  }

  if (showAddTracksSheet) {
    val existingIds = remember(entries) { entries.map { it.track.id }.toSet() }
    val selectedTracks = remember { mutableStateListOf<AudioTrack>() }

    ModalBottomSheet(
      onDismissRequest = { showAddTracksSheet = false },
      containerColor = palette.sheetBg,
      contentColor = palette.sheetTextPrimary,
      tonalElevation = 0.dp,
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = strings.addTracks,
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
          )
          ThemedPrimaryButton(
            onClick = {
              onAddTracks(selectedTracks.toList())
              showAddTracksSheet = false
            },
            enabled = selectedTracks.isNotEmpty(),
          ) {
            Text("${strings.addTracks} (${selectedTracks.size})")
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
          modifier = Modifier.height(360.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          itemsIndexed(libraryTracks) { _, track ->
            val alreadyInPlaylist = track.id in existingIds
            val checked = alreadyInPlaylist || selectedTracks.any { it.id == track.id }
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable(enabled = !alreadyInPlaylist) {
                    if (selectedTracks.any { it.id == track.id }) {
                      selectedTracks.removeAll { it.id == track.id }
                    } else {
                      selectedTracks.add(track)
                    }
                  }
                  .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Checkbox(
                checked = checked,
                enabled = !alreadyInPlaylist,
                onCheckedChange = { isChecked ->
                  if (!alreadyInPlaylist) {
                    if (isChecked) selectedTracks.add(track)
                    else selectedTracks.removeAll { it.id == track.id }
                  }
                },
              )
              Spacer(modifier = Modifier.width(8.dp))
              ArtworkThumbnail(
                model = track.albumArtUri,
                contentDescription = track.title,
                size = 38.dp,
                cornerRadius = 8.dp,
                isVideo = track.isVideo,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = track.title,
                  style = MaterialTheme.typography.titleSmall,
                  color = if (alreadyInPlaylist) palette.textSecondary else palette.textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text =
                    if (track.isVideo) "${strings.videoBadgeLabel} • ${formatDuration(track.durationMs)}"
                    else track.artist,
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textSecondary,
                  maxLines = 1,
                )
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }
}
