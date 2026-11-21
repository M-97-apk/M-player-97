package com.example.ui.screens

import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.localization.LocalAppStrings
import com.example.model.AppThemeStyle
import com.example.model.AudioOutputRouteType
import com.example.model.AudioTrack
import com.example.model.RepeatMode
import com.example.model.VisualizerMode
import com.example.playback.PlaybackUiState
import com.example.ui.components.ArtworkThumbnail
import com.example.ui.components.CosmicPlayerScreenStarfallOverlay
import com.example.ui.components.CosmicStarfallState
import com.example.ui.components.DynamicPlayerVisualizer
import com.example.ui.components.FavoriteHeartButton
import com.example.ui.components.GlassPlaybackControlButton
import com.example.ui.components.ThemedCard
import com.example.ui.components.ThemedIconButton
import com.example.ui.components.VideoFullscreenExpandButton
import com.example.ui.components.VideoSurfaceBrightnessOverlay
import com.example.ui.components.VisualizerModeSelector
import com.example.ui.components.activeGlassOptionPillDecoration
import com.example.ui.components.formatDuration
import com.example.ui.theme.LocalMPlayerPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
  playback: PlaybackUiState,
  sleepTimerState: com.example.model.SleepTimerState,
  isEqEnabled: Boolean,
  isCurrentTrackFavorite: Boolean = false,
  panAngleDegrees: Float,
  audioAmplitude: Float,
  bassEnergy: Float,
  waveformSamples: FloatArray,
  spectrumBins: FloatArray,
  visualizerMode: VisualizerMode,
  onSelectVisualizerMode: (VisualizerMode) -> Unit,
  onPlayPause: () -> Unit,
  onNext: () -> Unit,
  onPrevious: () -> Unit,
  onSeek: (Long) -> Unit,
  onToggleShuffle: () -> Unit,
  onCycleRepeat: () -> Unit,
  onToggle8D: () -> Unit = {},
  onOpen8DPanel: () -> Unit = {},
  onOpenSleepTimer: () -> Unit,
  onOpenEqualizer: () -> Unit,
  onJumpToQueueIndex: (Int) -> Unit,
  onAddToPlaylistClick: (AudioTrack) -> Unit,
  onToggleFavorite: (AudioTrack) -> Unit = {},
  onSetVideoAudioOnlyMode: (Boolean) -> Unit = {},
  onAttachVideoTextureView: (TextureView) -> Unit = {},
  onDetachVideoTextureView: (TextureView) -> Unit = {},
  videoBrightness: Float = -1f,
  onEnterFullscreen: () -> Unit = {},
  onSetVolumeBoost: (Int) -> Unit = {},
  bottomOverlayPadding: androidx.compose.ui.unit.Dp = 128.dp,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  val track = playback.currentTrack
  var showQueueSheet by remember { mutableStateOf(false) }
  val isSleepTimerActive = sleepTimerState.mode != com.example.model.SleepTimerMode.OFF
  val anyAudioDspActive =
    isEqEnabled ||
      playback.is8DEnabled ||
      playback.volumeBoostPercent > 100 ||
      playback.amplifierEnabled
  val cosmicStarfallState = remember { CosmicStarfallState() }
  var screenRootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

  Box(
    modifier = modifier.fillMaxSize().onGloballyPositioned { screenRootCoordinates = it }
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = strings.nowPlaying,
          style = MaterialTheme.typography.titleLarge,
          color = palette.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (isSleepTimerActive) {
          Text(
            text =
              if (sleepTimerState.isFadingOut) strings.sleepTimerFadingOut
              else strings.sleepTimerRemainingFormat(formatDuration(sleepTimerState.remainingMs)),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = palette.accentGlow,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        ThemedIconButton(
          onClick = onOpenSleepTimer,
          buttonSize = 38.dp,
          modifier = Modifier.testTag("player_sleep_timer_btn"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Bedtime,
            contentDescription = strings.sleepTimerTitle,
            tint = if (isSleepTimerActive) palette.accentGlow else palette.textPrimary,
          )
        }
        ThemedIconButton(
          onClick = onOpenEqualizer,
          buttonSize = 38.dp,
          modifier = Modifier.testTag("player_equalizer_btn"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Equalizer,
            contentDescription = strings.equalizerTitle,
            tint = if (anyAudioDspActive) palette.accentGlow else palette.textPrimary,
          )
        }
        if (track != null) {
          FavoriteHeartButton(
            isFavorite = isCurrentTrackFavorite,
            onToggle = { onToggleFavorite(track) },
            buttonSize = 38.dp,
            iconSize = 20.dp,
            modifier = Modifier.testTag("player_favorite_btn"),
          )
          ThemedIconButton(
            onClick = { onAddToPlaylistClick(track) },
            buttonSize = 38.dp,
            modifier = Modifier.testTag("player_add_to_playlist_btn"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
              contentDescription = strings.addToPlaylist,
              tint = palette.textPrimary,
            )
          }
        }
        ThemedIconButton(
          onClick = { showQueueSheet = true },
          buttonSize = 38.dp,
          modifier = Modifier.testTag("player_queue_btn"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
            contentDescription = strings.queueTitle,
            tint = palette.sectionHelperIconColor,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Live Hardware Output Route Indicator (Bluetooth / Wired Headphones / Speaker)
    val route = playback.audioOutputRoute
    val routeIcon =
      when (route.type) {
        AudioOutputRouteType.BLUETOOTH -> Icons.Rounded.BluetoothAudio
        AudioOutputRouteType.WIRED_HEADPHONES -> Icons.Rounded.Headphones
        AudioOutputRouteType.SPEAKER -> Icons.Rounded.Speaker
      }
    val routeLabel =
      when (route.type) {
        AudioOutputRouteType.BLUETOOTH ->
          if (route.deviceName.isNotBlank()) "${strings.routeBluetooth}: ${route.deviceName}"
          else strings.routeBluetooth
        AudioOutputRouteType.WIRED_HEADPHONES ->
          if (route.deviceName.isNotBlank()) "${strings.routeWiredHeadphones}: ${route.deviceName}"
          else strings.routeWiredHeadphones
        AudioOutputRouteType.SPEAKER -> strings.routeSpeaker
      }
    val isExternalHeadphones = route.type != AudioOutputRouteType.SPEAKER
    Row(
      modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
      horizontalArrangement = Arrangement.Start,
    ) {
      Row(
        modifier =
          Modifier.clip(RoundedCornerShape(50))
            .background(
              if (isExternalHeadphones) palette.glassBadgeActiveFillBrush
              else palette.glassBadgeFillBrush
            )
            .then(
              if (isExternalHeadphones) {
                Modifier.border(palette.borderStrokeWidth, palette.buttonBorderColor, RoundedCornerShape(50))
              } else {
                Modifier.border(palette.borderStrokeWidth, palette.borderBrush, RoundedCornerShape(50))
              }
            )
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .testTag("audio_output_route_badge"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = routeIcon,
          contentDescription = routeLabel,
          tint = if (isExternalHeadphones) palette.accentGlow else palette.textSecondary,
          modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = routeLabel,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = if (isExternalHeadphones) FontWeight.Bold else FontWeight.Medium,
          color = if (isExternalHeadphones) palette.accentGlow else palette.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (track?.isVideo == true) {
      Row(
        modifier =
          Modifier.clip(RoundedCornerShape(50))
            .background(palette.glassBadgeFillBrush)
            .border(palette.borderStrokeWidth, palette.borderBrush, RoundedCornerShape(50))
            .padding(4.dp)
            .testTag("video_playback_mode_switcher"),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        VideoModeTogglePill(
          icon = Icons.Rounded.Videocam,
          label = strings.videoDisplayModePill,
          selected = !playback.isVideoAudioOnly,
          onClick = { onSetVideoAudioOnlyMode(false) },
          modifier = Modifier.testTag("mode_pill_video"),
        )
        VideoModeTogglePill(
          icon = Icons.Rounded.Headphones,
          label = strings.audioOnlyModePill,
          selected = playback.isVideoAudioOnly,
          onClick = { onSetVideoAudioOnlyMode(true) },
          modifier = Modifier.testTag("mode_pill_audio_only"),
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
    }

    val renderVideoSurface = track?.isVideo == true && !playback.isVideoAudioOnly
    if (!renderVideoSurface) {
      VisualizerModeSelector(
        selectedMode = visualizerMode,
        onSelectMode = onSelectVisualizerMode,
        modifier = Modifier.testTag("visualizer_mode_selector"),
      )

      Spacer(modifier = Modifier.height(10.dp))

      Box(
        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        DynamicPlayerVisualizer(
          mode = visualizerMode,
          isPlaying = playback.isPlaying,
          is8DEnabled = playback.is8DEnabled,
          panAngleDegrees = panAngleDegrees,
          amplitude = audioAmplitude,
          bassEnergy = bassEnergy,
          waveformSamples = waveformSamples,
          spectrumBins = spectrumBins,
          albumArtUri = track?.albumArtUri,
          title = track?.title ?: strings.appName,
          cosmicStarfallState = cosmicStarfallState,
          screenRootCoordinates = screenRootCoordinates,
        )
      }
    } else {
      val videoShape = RoundedCornerShape(24.dp)
      Box(
        modifier =
          Modifier.widthIn(max = 520.dp)
            .fillMaxWidth()
            .aspectRatio(playback.videoAspectRatio.coerceIn(0.6f, 2.2f))
            .clip(videoShape)
            .background(Color.Black)
            .border(palette.borderStrokeWidth, palette.borderBrush, videoShape)
            .testTag("video_surface_stage"),
        contentAlignment = Alignment.Center,
      ) {
        AndroidView(
          factory = { ctx ->
            TextureView(ctx).also { tv ->
              onAttachVideoTextureView(tv)
            }
          },
          update = { tv -> onAttachVideoTextureView(tv) },
          onRelease = { tv -> onDetachVideoTextureView(tv) },
          modifier = Modifier.fillMaxSize(),
        )
        VideoSurfaceBrightnessOverlay(
          brightness = videoBrightness,
          modifier = Modifier.matchParentSize(),
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      VideoFullscreenExpandButton(
        onClick = onEnterFullscreen,
        modifier = Modifier.testTag("video_fullscreen_btn"),
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (track == null) {
      Text(
        text = strings.noTrackPlaying,
        style = MaterialTheme.typography.headlineSmall,
        color = palette.textPrimary,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = strings.pickTrackHint,
        style = MaterialTheme.typography.bodyMedium,
        color = palette.textSecondary,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(18.dp))
    } else {
      Text(
        text = track.title,
        style = MaterialTheme.typography.headlineSmall,
        color = palette.textPrimary,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "${track.artist} • ${track.album}",
        style = MaterialTheme.typography.bodyMedium,
        color = palette.textSecondary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(10.dp))
    }


    // Seek Bar & Time Labels + Transport Controls locked to LTR so Previous/Rewind is always Left and Next/Forward is always Right
    val duration = playback.durationMs.coerceAtLeast(1L)
    val progress = (playback.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
      Column(modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
        Slider(
          value = progress,
          onValueChange = { fraction -> onSeek((fraction * duration).toLong()) },
          colors =
            SliderDefaults.colors(
              thumbColor = palette.accentGlow,
              activeTrackColor = palette.accentPrimary,
              inactiveTrackColor = palette.textSecondary.copy(alpha = 0.22f),
            ),
          modifier = Modifier.fillMaxWidth().testTag("player_seek_slider"),
        )
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = formatDuration(playback.positionMs),
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
          )
          Text(
            text = formatDuration(playback.durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Frosted Glass Transport Controls
      Row(
        modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        GlassPlaybackControlButton(
          onClick = onToggleShuffle,
          buttonSize = 46.dp,
          isActive = playback.shuffleEnabled,
          modifier = Modifier.testTag("player_shuffle_btn"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Shuffle,
            contentDescription = strings.shuffleAll,
            tint =
              if (playback.shuffleEnabled) palette.accentGlow
              else if (palette.helperIconColor == Color.White) Color.White
              else palette.textPrimary.copy(alpha = 0.84f),
            modifier = Modifier.size(21.dp),
          )
        }

        GlassPlaybackControlButton(
          onClick = onPrevious,
          buttonSize = 54.dp,
          modifier = Modifier.testTag("player_prev_btn"),
        ) {
          Icon(
            imageVector = Icons.Rounded.SkipPrevious,
            contentDescription = "Previous",
            tint = palette.textPrimary,
            modifier = Modifier.size(29.dp),
          )
        }

        GlassPlaybackControlButton(
          onClick = onPlayPause,
          buttonSize = 72.dp,
          isPrimary = true,
          modifier = Modifier.testTag("player_play_pause_btn"),
        ) {
          Icon(
            imageVector = if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (playback.isPlaying) "Pause" else "Play",
            tint = palette.buttonContentColor,
            modifier = Modifier.size(36.dp),
          )
        }

        GlassPlaybackControlButton(
          onClick = onNext,
          buttonSize = 54.dp,
          modifier = Modifier.testTag("player_next_btn"),
        ) {
          Icon(
            imageVector = Icons.Rounded.SkipNext,
            contentDescription = "Next",
            tint = palette.textPrimary,
            modifier = Modifier.size(29.dp),
          )
        }

        GlassPlaybackControlButton(
          onClick = onCycleRepeat,
          buttonSize = 46.dp,
          isActive = playback.repeatMode != RepeatMode.OFF,
          modifier = Modifier.testTag("player_repeat_btn"),
        ) {
          Icon(
            imageVector =
              if (playback.repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne
              else Icons.Rounded.Repeat,
            contentDescription = "Repeat",
            tint =
              if (playback.repeatMode != RepeatMode.OFF) palette.accentGlow
              else if (palette.helperIconColor == Color.White) Color.White
              else palette.textPrimary.copy(alpha = 0.84f),
            modifier = Modifier.size(21.dp),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(bottomOverlayPadding + 16.dp))
    }

    if (palette.themeStyle == AppThemeStyle.COSMIC_STARFIELD && cosmicStarfallState.isStarfallActive) {
      CosmicPlayerScreenStarfallOverlay(
        state = cosmicStarfallState,
        modifier = Modifier.matchParentSize(),
      )
    }
  }

  if (showQueueSheet) {
    val activeQueueIndex =
      playback.currentIndex.coerceIn(0, (playback.queue.size - 1).coerceAtLeast(0))
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val queueListState =
      rememberLazyListState(
        initialFirstVisibleItemIndex = (activeQueueIndex - 2).coerceAtLeast(0)
      )

    LaunchedEffect(showQueueSheet, activeQueueIndex, playback.queue.size) {
      if (showQueueSheet && playback.queue.isNotEmpty()) {
        yield()
        withFrameNanos { }
        if (queueListState.layoutInfo.viewportSize.height == 0 ||
          queueListState.layoutInfo.visibleItemsInfo.isEmpty()
        ) {
          delay(16L)
          withFrameNanos { }
        }
        val layoutInfo = queueListState.layoutInfo
        val viewportHeight = layoutInfo.viewportSize.height
        val measuredItem = layoutInfo.visibleItemsInfo.firstOrNull()
        val itemHeight = measuredItem?.size ?: 0
        val spacing = layoutInfo.mainAxisItemSpacing
        val stride = (itemHeight + spacing).coerceAtLeast(1)
        if (viewportHeight > 0 && itemHeight > 0) {
          val targetTopY = ((viewportHeight - itemHeight) / 2).coerceAtLeast(0)
          val itemsBefore = targetTopY / stride
          if (activeQueueIndex <= itemsBefore) {
            queueListState.scrollToItem(0, 0)
          } else {
            val anchorIndex = (activeQueueIndex - itemsBefore - 1).coerceAtLeast(0)
            val scrollOffset = ((itemsBefore + 1) * stride - targetTopY).coerceAtLeast(0)
            queueListState.scrollToItem(anchorIndex, scrollOffset)
          }
        } else {
          queueListState.scrollToItem((activeQueueIndex - 2).coerceAtLeast(0), 0)
        }
      }
    }

    ModalBottomSheet(
      onDismissRequest = { showQueueSheet = false },
      sheetState = queueSheetState,
      containerColor = palette.sheetBg,
      contentColor = palette.sheetTextPrimary,
      tonalElevation = 0.dp,
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .testTag("player_queue_sheet")
      ) {
        Text(
          text = "${strings.queueTitle} (${playback.queue.size})",
          style = MaterialTheme.typography.titleLarge,
          color = palette.textPrimary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
          state = queueListState,
          modifier = Modifier.fillMaxWidth().height(360.dp).testTag("player_queue_list"),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          itemsIndexed(
            items = playback.queue,
            key = { idx, item -> "${idx}_${item.id}" },
          ) { idx, item ->
            val active = idx == playback.currentIndex
            ThemedCard(
              onClick = {
                onJumpToQueueIndex(idx)
                showQueueSheet = false
              },
              cornerRadius = 16.dp,
              elevated = active,
              modifier = Modifier.fillMaxWidth().testTag("queue_item_$idx"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                ArtworkThumbnail(
                  model = item.albumArtUri,
                  contentDescription = item.title,
                  size = 42.dp,
                  cornerRadius = 10.dp,
                  showActiveRim = active,
                  isVideo = item.isVideo,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) palette.accentGlow else palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = item.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                if (active) {
                  Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = palette.accentGlow,
                  )
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }
}

@Composable
private fun VideoModeTogglePill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  val pillShape = RoundedCornerShape(50)
  Row(
    modifier =
      modifier
        .activeGlassOptionPillDecoration(selected = selected, shape = pillShape)
        .then(if (!selected) Modifier.clip(pillShape) else Modifier)
        .clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (selected) palette.accentGlow else palette.textSecondary,
      modifier = Modifier.size(16.dp),
    )
    Spacer(modifier = Modifier.width(6.dp))
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
