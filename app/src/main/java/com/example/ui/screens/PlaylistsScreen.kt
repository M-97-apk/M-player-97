package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.example.ui.components.FrostedGlassGrainOverlay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.analytics.PlaylistBackupEngine
import com.example.data.prefs.UserPreferences
import com.example.localization.LocalAppStrings
import com.example.model.PlaylistSummary
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
fun PlaylistsScreen(
  playlists: List<PlaylistSummary>,
  preferences: UserPreferences = UserPreferences(),
  onCreatePlaylist: (String) -> Unit,
  onOpenPlaylist: (PlaylistSummary) -> Unit,
  onDeletePlaylist: (Long) -> Unit,
  onSelectPlaylistBackupFile: () -> Unit = {},
  onImportPlaylists: () -> Unit = {},
  isRefreshing: Boolean = false,
  onRefresh: () -> Unit = {},
  bottomOverlayPadding: androidx.compose.ui.unit.Dp = 148.dp,
  fabBottomOffset: androidx.compose.ui.unit.Dp = 96.dp,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  var showCreateDialog by remember { mutableStateOf(false) }
  var showBackupPopover by remember { mutableStateOf(false) }
  var pendingScrollToTopAfterCreate by remember { mutableStateOf(false) }
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(playlists.size, pendingScrollToTopAfterCreate) {
    if (pendingScrollToTopAfterCreate && playlists.isNotEmpty()) {
      listState.animateScrollToItem(0)
      pendingScrollToTopAfterCreate = false
    }
  }
  val pullRefreshState = rememberPullToRefreshState()
  val showScrollToTop by
    remember {
      derivedStateOf {
        listState.firstVisibleItemIndex >= 2 ||
          (listState.firstVisibleItemIndex >= 1 && listState.firstVisibleItemScrollOffset > 120)
      }
    }

  Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = strings.playlistsTitle,
          style = MaterialTheme.typography.headlineMedium,
          color = palette.textPrimary,
        )
        ThemedIconButton(
          onClick = { showBackupPopover = true },
          buttonSize = 44.dp,
          modifier =
            Modifier.clip(androidx.compose.foundation.shape.CircleShape)
              .background(
                if (preferences.playlistBackupSyncEnabled) palette.glassBadgeActiveFillBrush
                else palette.glassBadgeFillBrush
              )
              .border(
                width = palette.borderStrokeWidth,
                brush = palette.borderBrush,
                shape = androidx.compose.foundation.shape.CircleShape,
              )
              .testTag("playlists_backup_settings_btn"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = strings.playlistBackupSettingsTitle,
            tint = if (preferences.playlistBackupSyncEnabled) palette.accentGlow else palette.textPrimary,
            modifier = Modifier.size(21.dp),
          )
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
        modifier = Modifier.weight(1f).fillMaxWidth().testTag("playlists_pull_to_refresh"),
      ) {
        if (playlists.isEmpty()) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = bottomOverlayPadding + 32.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = null,
                tint = palette.sectionHelperIconColor,
                modifier = Modifier.size(64.dp),
              )
              Spacer(modifier = Modifier.height(14.dp))
              Text(
                text = strings.emptyPlaylistsTitle,
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
                textAlign = TextAlign.Center,
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = strings.emptyPlaylistsSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
              )
              Spacer(modifier = Modifier.height(20.dp))
              ThemedPrimaryButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.height(48.dp).testTag("empty_create_playlist_btn"),
              ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.createPlaylist)
              }
            }
          }
        } else {
          LazyColumn(
            state = listState,
            contentPadding =
              PaddingValues(start = 20.dp, end = 20.dp, bottom = bottomOverlayPadding + 72.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().testTag("playlists_list"),
          ) {
            items(playlists, key = { it.id }) { playlist ->
            val displayName =
              if (playlist.isFavorites) strings.favoritesPlaylistName else playlist.name
            ThemedCard(
              onClick = { onOpenPlaylist(playlist) },
              cornerRadius = 22.dp,
              modifier = Modifier.fillMaxWidth().testTag("playlist_card_${playlist.id}"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(modifier = Modifier.size(68.dp)) {
                  PlaylistCoverArt(
                    coverPath = playlist.coverPath,
                    contentDescription = displayName,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 16.dp,
                  )
                  if (playlist.isFavorites && playlist.coverPath == null) {
                    Box(
                      modifier =
                        Modifier.fillMaxSize()
                          .clip(RoundedCornerShape(16.dp))
                          .background(palette.glassBadgeActiveFillBrush),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = displayName,
                        tint = palette.sectionHelperIconColor,
                        modifier = Modifier.size(28.dp),
                      )
                    }
                  }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text =
                      "${strings.tracksCountFormat(playlist.trackCount)} • ${
                        formatDuration(playlist.totalDurationMs)
                      }",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                  )
                }
                if (!playlist.isFavorites) {
                  ThemedIconButton(
                    onClick = { onDeletePlaylist(playlist.id) },
                    buttonSize = 38.dp,
                    modifier = Modifier.testTag("delete_playlist_${playlist.id}"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.DeleteOutline,
                      contentDescription = strings.delete,
                      tint = palette.textSecondary,
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
    }

    ScrollToTopFloatingButton(
      visible = showScrollToTop,
      onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
      buttonTestTag = "playlists_scroll_to_top_btn",
      modifier =
        Modifier.align(Alignment.BottomEnd)
          .padding(end = 24.dp, bottom = fabBottomOffset + 68.dp),
    )

    Box(
      modifier =
        Modifier.align(Alignment.BottomEnd)
          .padding(end = 20.dp, bottom = fabBottomOffset)
          .size(56.dp)
          .clip(CircleShape)
          .background(palette.glassButtonFillBrush)
          .border(1.2.dp, palette.buttonBorderColor, CircleShape)
          .clickable(onClick = { showCreateDialog = true })
          .testTag("create_playlist_fab"),
      contentAlignment = Alignment.Center,
    ) {
      FrostedGlassGrainOverlay(grainAlpha = 0.03f, modifier = Modifier.matchParentSize())
      Icon(
        imageVector = Icons.Rounded.Add,
        contentDescription = strings.createPlaylist,
        tint = palette.buttonContentColor,
        modifier = Modifier.size(24.dp),
      )
    }
  }

  if (showCreateDialog) {
    CreatePlaylistDialog(
      onConfirm = { name ->
        pendingScrollToTopAfterCreate = true
        onCreatePlaylist(name)
        showCreateDialog = false
      },
      onDismiss = { showCreateDialog = false },
    )
  }

  if (showBackupPopover) {
    PlaylistBackupSettingsPopover(
      preferences = preferences,
      onSelectBackupFile = {
        showBackupPopover = false
        onSelectPlaylistBackupFile()
      },
      onImportPlaylists = {
        showBackupPopover = false
        onImportPlaylists()
      },
      onDismiss = { showBackupPopover = false },
    )
  }
}

@Composable
private fun PlaylistBackupSettingsPopover(
  preferences: UserPreferences,
  onSelectBackupFile: () -> Unit,
  onImportPlaylists: () -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  val modalShape = RoundedCornerShape(24.dp)

  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .clip(modalShape)
          .background(palette.sheetBg)
          .background(palette.glassCardFrostBrush)
          .border(palette.borderStrokeWidth, palette.borderBrush, modalShape)
          .padding(18.dp)
          .testTag("playlists_backup_popover")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector =
                if (preferences.playlistBackupSyncEnabled) Icons.Rounded.CheckCircle
                else Icons.Rounded.Folder,
              contentDescription = PlaylistBackupEngine.PLAYLIST_BACKUP_FILENAME,
              tint = palette.sectionHelperIconColor,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = strings.playlistBackupSettingsTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = palette.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          ThemedIconButton(
            onClick = onDismiss,
            buttonSize = 36.dp,
            modifier = Modifier.testTag("playlists_backup_close_btn"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = strings.dismiss,
              tint = palette.textSecondary,
              modifier = Modifier.size(18.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(palette.glassBadgeFillBrush)
              .border(1.dp, palette.borderBrush, RoundedCornerShape(16.dp))
              .padding(12.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text =
                if (preferences.playlistBackupSyncEnabled) strings.playlistBackupActive
                else strings.playlistBackupInactive,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = palette.textPrimary,
            )
            Text(
              text = strings.playlistBackupSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = palette.textSecondary,
            )
            if (preferences.playlistBackupSyncEnabled && preferences.playlistLastSyncedIso.isNotBlank()) {
              Text(
                text =
                  "${strings.usageSyncLastSyncedPrefix} ${
                    preferences.playlistLastSyncedIso.take(19).replace('T', ' ')
                  }",
                style = MaterialTheme.typography.labelSmall,
                color = palette.sectionHelperTextColor,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        ThemedPrimaryButton(
          onClick = onSelectBackupFile,
          modifier =
            Modifier.fillMaxWidth()
              .height(48.dp)
              .testTag("playlists_select_backup_file_btn"),
        ) {
          Icon(
            imageVector =
              if (preferences.playlistBackupSyncEnabled) Icons.Rounded.Folder
              else Icons.Rounded.Save,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text =
              if (preferences.playlistBackupSyncEnabled) strings.playlistBackupChangeFile
              else strings.playlistBackupSelectFile,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        ThemedPrimaryButton(
          onClick = onImportPlaylists,
          modifier =
            Modifier.fillMaxWidth()
              .height(48.dp)
              .testTag("playlists_import_btn"),
        ) {
          Icon(
            imageVector = Icons.Rounded.FileUpload,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = strings.playlistImportButton,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
fun CreatePlaylistDialog(
  onConfirm: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  var name by remember { mutableStateOf("") }
  val canConfirm = name.isNotBlank()

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = palette.cardSurfaceElevated,
    title = { Text(strings.createPlaylist, color = palette.textPrimary) },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(strings.playlistNameHint) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions =
          KeyboardActions(
            onDone = {
              val trimmed = name.trim()
              if (trimmed.isNotEmpty()) onConfirm(trimmed)
            }
          ),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary,
            cursorColor = palette.accentGlow,
            focusedBorderColor = palette.accentGlow,
            unfocusedBorderColor = palette.buttonBorderColor,
            focusedLabelColor = palette.accentGlow,
            unfocusedLabelColor = palette.textSecondary,
          ),
        modifier = Modifier.fillMaxWidth().testTag("playlist_name_input"),
      )
    },
    confirmButton = {
      ThemedPrimaryButton(
        onClick = {
          val trimmed = name.trim()
          if (trimmed.isNotEmpty()) onConfirm(trimmed)
        },
        enabled = canConfirm,
        modifier = Modifier.testTag("confirm_create_playlist_btn"),
      ) {
        Text(strings.create)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.cancel, color = palette.textSecondary)
      }
    },
  )
}
