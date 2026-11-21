package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.localization.LocalAppStrings
import com.example.model.AudioTrack
import com.example.model.LibraryMediaFilter
import com.example.model.LibrarySortOption
import com.example.ui.components.ArtworkThumbnail
import com.example.ui.components.FavoriteHeartButton
import com.example.ui.components.LocateCurrentTrackFloatingButton
import com.example.ui.components.MediaOptionsMenuButton
import com.example.ui.components.ScrollToTopFloatingButton
import com.example.ui.components.ThemedCard
import com.example.ui.components.ThemedIconButton
import com.example.ui.components.ThemedPrimaryButton
import com.example.ui.components.activeGlassOptionPillDecoration
import com.example.ui.components.formatDuration
import com.example.ui.theme.LocalMPlayerPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
  tracks: List<AudioTrack>,
  folders: List<String>,
  selectedFolder: String?,
  selectedMediaFilter: LibraryMediaFilter = LibraryMediaFilter.ALL,
  favoriteUris: Set<String> = emptySet(),
  searchQuery: String,
  currentSort: LibrarySortOption,
  currentTrackId: Long?,
  isPlaying: Boolean,
  isScanning: Boolean,
  hasAudioPermission: Boolean,
  onRequestPermission: () -> Unit,
  onOpenAppSettings: () -> Unit,
  onRescan: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onSelectFolder: (String?) -> Unit,
  onSelectMediaFilter: (LibraryMediaFilter) -> Unit = {},
  onSelectSort: (LibrarySortOption) -> Unit,
  onTrackClick: (AudioTrack) -> Unit,
  onPlayAll: () -> Unit,
  onShuffleAll: () -> Unit,
  onAddToPlaylistClick: (AudioTrack) -> Unit,
  onToggleFavorite: (AudioTrack) -> Unit = {},
  onPlayVideoNormal: (AudioTrack) -> Unit = onTrackClick,
  onPlayVideoAudioOnly: (AudioTrack) -> Unit = onTrackClick,
  onOpenConvertDialog: (AudioTrack) -> Unit = {},
  onDeleteTrack: (AudioTrack) -> Unit = {},
  bottomOverlayPadding: Dp = 140.dp,
  fabBottomOffset: Dp = 96.dp,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  var showSortMenu by remember { mutableStateOf(false) }
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
  val currentTrackIndex =
    remember(tracks, currentTrackId) {
      if (currentTrackId == null) -1 else tracks.indexOfFirst { it.id == currentTrackId }
    }
  var jumpHighlightedTrackId by remember { mutableStateOf<Long?>(null) }
  var jumpHighlightSeq by remember { mutableIntStateOf(0) }
  LaunchedEffect(jumpHighlightSeq) {
    if (jumpHighlightSeq > 0) {
      delay(1800L)
      jumpHighlightedTrackId = null
    }
  }

  Column(modifier = modifier.fillMaxSize()) {
    // Top Brand Header
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
          painter = painterResource(id = R.drawable.mplayer_icon_art),
          contentDescription = strings.appName,
          contentScale = ContentScale.Crop,
          modifier =
            Modifier.size(42.dp)
              .clip(RoundedCornerShape(12.dp))
              .border(1.dp, palette.borderBrush, RoundedCornerShape(12.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = strings.appName,
            style = MaterialTheme.typography.headlineSmall,
            color = palette.textPrimary,
          )
          Text(
            text = strings.tracksCountFormat(tracks.size),
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
            modifier = Modifier.testTag("library_item_count"),
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        ThemedIconButton(
          onClick = onRescan,
          modifier = Modifier.testTag("library_rescan_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = strings.scanLibrary,
            tint = palette.textPrimary,
          )
        }
        Box {
          ThemedIconButton(
            onClick = { showSortMenu = true },
            modifier = Modifier.testTag("library_sort_button"),
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
                LibrarySortOption.TITLE_ASC to strings.sortAlphaAsc,
                LibrarySortOption.TITLE_DESC to strings.sortAlphaDesc,
                LibrarySortOption.DATE_MODIFIED_DESC to strings.sortDateModifiedDesc,
                LibrarySortOption.DATE_MODIFIED_ASC to strings.sortDateModifiedAsc,
                LibrarySortOption.ARTIST_ASC to strings.sortArtistAsc,
                LibrarySortOption.DURATION_DESC to strings.sortDurationDesc,
              )
            options.forEach { (option, label) ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = label,
                    color = if (currentSort == option) palette.accentGlow else palette.textPrimary,
                  )
                },
                trailingIcon = {
                  if (currentSort == option) {
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

    AnimatedVisibility(visible = isScanning) {
      LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        color = palette.accentGlow,
      )
    }

    if (!hasAudioPermission) {
      Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        ThemedCard(cornerRadius = 26.dp, elevated = true) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              imageVector = Icons.Rounded.LibraryMusic,
              contentDescription = null,
              tint = palette.sectionHelperIconColor,
              modifier = Modifier.size(44.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = strings.permissionTitle,
              style = MaterialTheme.typography.titleLarge,
              color = palette.textPrimary,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = strings.permissionBody,
              style = MaterialTheme.typography.bodyMedium,
              color = palette.textSecondary,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            ThemedPrimaryButton(
              onClick = onRequestPermission,
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("grant_audio_permission_btn"),
            ) {
              Text(strings.grantPermission)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
              onClick = onOpenAppSettings,
              border = androidx.compose.foundation.BorderStroke(palette.borderStrokeWidth, palette.buttonBorderColor),
              modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
              Text(strings.openSettings, color = palette.textPrimary)
            }
          }
        }
      }
      return
    }

    // Clean Smart Search Bar (empty with only lens icon when idle; subtle clear X when text is typed)
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchQueryChange,
      leadingIcon = {
        Icon(
          imageVector = Icons.Rounded.Search,
          contentDescription = null,
          tint = palette.sectionHelperIconColor,
        )
      },
      trailingIcon = {
        AnimatedVisibility(
          visible = searchQuery.isNotEmpty(),
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          IconButton(
            onClick = { onSearchQueryChange("") },
            modifier = Modifier.testTag("library_search_clear_btn"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = strings.clearSearch,
              tint = palette.helperIconColor,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(18.dp),
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedTextColor = palette.textPrimary,
          unfocusedTextColor = palette.textPrimary,
          focusedBorderColor = palette.accentGlow,
          unfocusedBorderColor = palette.textSecondary.copy(alpha = 0.25f),
          focusedContainerColor = palette.cardSurface,
          unfocusedContainerColor = palette.cardSurface,
        ),
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 20.dp)
          .testTag("library_search_input"),
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Unified Media Filter Row: All / Audio / Video + Folder filter chips
    LazyRow(
      contentPadding = PaddingValues(horizontal = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      val typeFilters =
        listOf(
          LibraryMediaFilter.ALL to strings.filterAllMedia,
          LibraryMediaFilter.AUDIO to strings.filterAudioOnly,
          LibraryMediaFilter.VIDEO to strings.filterVideoOnly,
        )
      items(typeFilters, key = { it.first.name }) { (filter, label) ->
        FolderFilterChip(
          label = label,
          selected = selectedMediaFilter == filter,
          onClick = { onSelectMediaFilter(filter) },
          modifier = Modifier.testTag("library_media_filter_${filter.name.lowercase()}"),
        )
      }
      if (folders.isNotEmpty()) {
        item {
          FolderFilterChip(
            label = strings.allFolders,
            selected = selectedFolder == null,
            onClick = { onSelectFolder(null) },
          )
        }
        items(folders) { folder ->
          FolderFilterChip(
            label = folder,
            selected = selectedFolder == folder,
            onClick = { onSelectFolder(if (selectedFolder == folder) null else folder) },
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(10.dp))

    // Play All & Shuffle Action Row
    if (tracks.isNotEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ThemedPrimaryButton(
          onClick = onPlayAll,
          modifier = Modifier.weight(1f).height(48.dp).testTag("library_play_all_btn"),
        ) {
          Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text(strings.playAll, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        ThemedCard(
          onClick = onShuffleAll,
          cornerRadius = 16.dp,
          modifier = Modifier.weight(1f).height(48.dp).testTag("library_shuffle_all_btn"),
        ) {
          Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Shuffle,
              contentDescription = null,
              tint = palette.sectionHelperIconColor,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = strings.shuffleAll,
              style = MaterialTheme.typography.labelLarge,
              color = palette.textPrimary,
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
    }

    PullToRefreshBox(
      isRefreshing = isScanning,
      onRefresh = onRescan,
      state = pullRefreshState,
      indicator = {
        PullToRefreshDefaults.Indicator(
          state = pullRefreshState,
          isRefreshing = isScanning,
          containerColor = palette.sheetBg,
          color = palette.accentGlow,
          modifier = Modifier.align(Alignment.TopCenter),
        )
      },
      modifier = Modifier.weight(1f).fillMaxWidth().testTag("library_pull_to_refresh"),
    ) {
      if (tracks.isEmpty()) {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(start = 28.dp, end = 28.dp, top = 28.dp, bottom = bottomOverlayPadding + 40.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Rounded.FolderOpen,
              contentDescription = null,
              tint = palette.sectionHelperIconColor,
              modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = strings.emptyLibraryTitle,
              style = MaterialTheme.typography.titleLarge,
              color = palette.textPrimary,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = strings.emptyLibrarySubtitle,
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
            PaddingValues(
              start = 20.dp,
              end = 20.dp,
              bottom = bottomOverlayPadding + 84.dp,
            ),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxSize().testTag("library_tracks_list"),
        ) {
          items(tracks, key = { it.id }) { track ->
            val isCurrent = track.id == currentTrackId
            val isFavorite = track.contentUri.toString() in favoriteUris
            TrackRowCard(
              track = track,
              isCurrent = isCurrent,
              isPlaying = isCurrent && isPlaying,
              isFavorite = isFavorite,
              isJumpHighlighted = isCurrent && (track.id == jumpHighlightedTrackId),
              onClick = { onTrackClick(track) },
              onToggleFavorite = { onToggleFavorite(track) },
              onAddToPlaylist = { onAddToPlaylistClick(track) },
              onPlayVideoNormal = { onPlayVideoNormal(track) },
              onPlayVideoAudioOnly = { onPlayVideoAudioOnly(track) },
              onConvertFormat = { onOpenConvertDialog(track) },
              onDelete = { onDeleteTrack(track) },
            )
          }
        }
      }

      Row(
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = fabBottomOffset),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LocateCurrentTrackFloatingButton(
          visible = currentTrackIndex >= 0,
          onClick = {
            if (currentTrackIndex >= 0) {
              jumpHighlightedTrackId = tracks[currentTrackIndex].id
              jumpHighlightSeq += 1
              coroutineScope.launch {
                val targetIndex = (currentTrackIndex - 1).coerceAtLeast(0)
                listState.animateScrollToItem(index = targetIndex)
                val targetVisible =
                  listState.layoutInfo.visibleItemsInfo.any { it.index == currentTrackIndex }
                if (!targetVisible) {
                  listState.animateScrollToItem(index = currentTrackIndex)
                }
              }
            }
          },
          buttonTestTag = "library_locate_current_track_btn",
        )
        ScrollToTopFloatingButton(
          visible = showScrollToTop,
          onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
          buttonTestTag = "library_scroll_to_top_btn",
        )
      }
    }
  }
}

@Composable
private fun FolderFilterChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  val isPitchBlackGold = palette.themeStyle == com.example.model.AppThemeStyle.PITCH_BLACK_GOLD
  val chipMod =
    when {
      !selected ->
        modifier
          .clip(RoundedCornerShape(50))
          .background(palette.cardSurface)
          .border(1.dp, palette.textSecondary.copy(alpha = 0.22f), RoundedCornerShape(50))
      isPitchBlackGold ->
        modifier
          .clip(RoundedCornerShape(50))
          .background(palette.accentPrimary.copy(alpha = 0.24f))
          .border(1.dp, palette.accentGlow, RoundedCornerShape(50))
      else ->
        modifier.activeGlassOptionPillDecoration(selected = true, shape = RoundedCornerShape(50))
    }
  Box(
    modifier =
      chipMod
        .clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      color = if (selected) palette.accentGlow else palette.textSecondary,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun TrackRowCard(
  track: AudioTrack,
  isCurrent: Boolean,
  isPlaying: Boolean,
  isFavorite: Boolean = false,
  isJumpHighlighted: Boolean = false,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit = {},
  onAddToPlaylist: () -> Unit,
  onPlayVideoNormal: () -> Unit = onClick,
  onPlayVideoAudioOnly: () -> Unit = onClick,
  onConvertFormat: () -> Unit = {},
  onDelete: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  val highlightAlpha by
    animateFloatAsState(
      targetValue = if (isJumpHighlighted) 1f else 0f,
      animationSpec = tween(durationMillis = 260),
      label = "track_jump_highlight_alpha",
    )
  val highlightColor =
    if (palette.isMinimalPlainBlack) Color.White else palette.accentGlow
  val highlightCardModifier =
    if (highlightAlpha > 0.02f) {
      modifier
        .fillMaxWidth()
        .border(
          width = 2.2.dp,
          color = highlightColor.copy(alpha = highlightAlpha),
          shape = RoundedCornerShape(18.dp),
        )
    } else {
      modifier.fillMaxWidth()
    }

  ThemedCard(
    onClick = onClick,
    cornerRadius = 18.dp,
    elevated = isCurrent || isJumpHighlighted,
    modifier = highlightCardModifier.testTag("track_card_${track.id}"),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .then(
            if (highlightAlpha > 0.02f) {
              Modifier.background(highlightColor.copy(alpha = 0.14f * highlightAlpha))
            } else {
              Modifier
            }
          )
          .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ArtworkThumbnail(
        model = track.albumArtUri,
        contentDescription = track.title,
        size = 50.dp,
        cornerRadius = 12.dp,
        showActiveRim = isCurrent,
        isVideo = track.isVideo,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = track.title,
          style = MaterialTheme.typography.titleSmall,
          color = if (isCurrent) palette.accentGlow else palette.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${track.artist} • ${formatDuration(track.durationMs)}",
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (isCurrent) {
        Icon(
          imageVector = Icons.Rounded.GraphicEq,
          contentDescription = if (isPlaying) "Playing" else "Paused",
          tint = palette.accentGlow,
          modifier = Modifier.padding(horizontal = 4.dp).size(18.dp),
        )
      }
      FavoriteHeartButton(
        isFavorite = isFavorite,
        onToggle = onToggleFavorite,
        buttonSize = 36.dp,
        iconSize = 18.dp,
        modifier = Modifier.testTag("fav_track_${track.id}"),
      )
      ThemedIconButton(
        onClick = onAddToPlaylist,
        buttonSize = 36.dp,
        modifier = Modifier.testTag("add_to_playlist_${track.id}"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
          contentDescription = strings.addToPlaylist,
          tint = palette.textSecondary,
          modifier = Modifier.size(19.dp),
        )
      }
      MediaOptionsMenuButton(
        track = track,
        onPlay = onClick,
        onAddToPlaylist = onAddToPlaylist,
        onPlayVideoNormal = onPlayVideoNormal,
        onPlayVideoAudioOnly = onPlayVideoAudioOnly,
        onConvertFormat = onConvertFormat,
        onDelete = onDelete,
        buttonSize = 36.dp,
        modifier = Modifier.testTag("media_options_${track.id}"),
      )
    }
  }
}
