package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.analytics.PlaylistBackupEngine
import com.example.data.analytics.UsageStatsEngine
import com.example.data.media.MediaConverter
import com.example.data.prefs.UserPreferences
import com.example.localization.LocalAppStrings
import com.example.model.AppThemeStyle
import com.example.model.AudioTrack
import com.example.overlay.FloatingOverlayService
import com.example.ui.components.AddToPlaylistBottomSheet
import com.example.ui.components.Audio8DControlSheet
import com.example.ui.components.ConversionProgressDialog
import com.example.ui.components.ConvertFormatDialog
import com.example.ui.components.EqualizerSheet
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.SleepTimerSheet
import com.example.ui.components.ThemedBackground
import com.example.ui.components.VideoTheaterOverlay
import com.example.ui.screens.CreatePlaylistDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.theme.LocalMPlayerPalette
import com.example.ui.theme.MPlayerTheme
import com.example.util.NativePermissionsHelper
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

enum class MainTab {
  LIBRARY,
  PLAYLISTS,
  PLAYER,
  HISTORY,
  MORE,
}

class MainActivity : ComponentActivity() {
  private val viewModel: MainViewModel by viewModels()
  private var openPlayerTabTick by mutableIntStateOf(0)

  companion object {
    const val EXTRA_OPEN_PLAYER_TAB = "extra_open_player_tab"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (intent?.getBooleanExtra(EXTRA_OPEN_PLAYER_TAB, false) == true) {
      openPlayerTabTick++
    }
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
    )

    setContent {
      val prefs by viewModel.userPreferences.collectAsStateWithLifecycle(UserPreferences())

      MPlayerTheme(
        themeStyle = prefs.themeStyle,
        darkTheme = prefs.isDarkMode,
        language = prefs.language,
        glassOpacity = prefs.appGlassOpacity,
      ) {
        MPlayerAppRoot(
          viewModel = viewModel,
          preferences = prefs,
          openPlayerTabTick = openPlayerTabTick,
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.getBooleanExtra(EXTRA_OPEN_PLAYER_TAB, false)) {
      openPlayerTabTick++
    }
  }
}

@Composable
private fun MPlayerAppRoot(
  viewModel: MainViewModel,
  preferences: UserPreferences,
  openPlayerTabTick: Int,
) {
  val context = LocalContext.current
  val strings = LocalAppStrings.current
  val palette = LocalMPlayerPalette.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val requiredMediaPermissions =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
      arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

  fun hasReadAudio(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
        PackageManager.PERMISSION_GRANTED
    }

  fun hasReadVideo(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
        PackageManager.PERMISSION_GRANTED
    }

  fun hasAnyMediaAccess(): Boolean = hasReadAudio() || hasReadVideo()

  fun missingMediaPermissionsOnly(): Array<String> =
    buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          if (!hasReadAudio()) add(Manifest.permission.READ_MEDIA_AUDIO)
          if (!hasReadVideo()) add(Manifest.permission.READ_MEDIA_VIDEO)
        } else if (!hasReadAudio()) {
          add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
      }
      .toTypedArray()

  fun missingPermissionsToRequest(): Array<String> =
    buildList {
        addAll(missingMediaPermissionsOnly())
        if (
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
              PackageManager.PERMISSION_GRANTED
        ) {
          add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
              PackageManager.PERMISSION_GRANTED
        ) {
          add(Manifest.permission.BLUETOOTH_CONNECT)
        }
      }
      .toTypedArray()

  var hasAudioPermission by remember { mutableStateOf(hasAnyMediaAccess()) }
  var hasVideoPermission by remember { mutableStateOf(hasReadVideo()) }
  var canDrawOverlays by remember { mutableStateOf(NativePermissionsHelper.canDrawOverlays(context)) }
  var pendingOverlayLaunchAfterGrant by remember { mutableStateOf(false) }
  var pendingDeleteTrack by remember { mutableStateOf<AudioTrack?>(null) }

  val deletePermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
      val target = pendingDeleteTrack
      pendingDeleteTrack = null
      if (result.resultCode == Activity.RESULT_OK && target != null) {
        viewModel.onSystemDeleteConfirmed(target)
      }
    }

  LaunchedEffect(viewModel) {
    viewModel.deleteRequests.collect { req ->
      pendingDeleteTrack = req.track
      deletePermissionLauncher.launch(IntentSenderRequest.Builder(req.intentSender).build())
    }
  }

  LaunchedEffect(viewModel, strings) {
    viewModel.deleteOutcomes.collect { outcome ->
      snackbarHostState.showSnackbar(
        message =
          if (outcome.success) strings.deletedMediaFormat(outcome.title)
          else strings.deleteMediaFailed,
        duration = SnackbarDuration.Short,
      )
    }
  }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
      val audioGranted = hasReadAudio()
      val videoGranted = hasReadVideo()
      val anyGranted = audioGranted || videoGranted || requiredMediaPermissions.any { result[it] == true }
      hasAudioPermission = anyGranted
      hasVideoPermission = videoGranted
      viewModel.playerController.refreshAudioOutputRoute()
      if (anyGranted) {
        viewModel.scanLibrary(
          unknownArtist = strings.unknownArtist,
          unknownAlbum = strings.unknownAlbum,
          unknownVideo = strings.unknownVideoLabel,
          includeAudio = audioGranted,
          includeVideo = videoGranted,
        )
      }
    }

  fun triggerLibraryRescan(showProgress: Boolean) {
    val missingMedia = missingMediaPermissionsOnly()
    if (!hasAnyMediaAccess()) {
      permissionLauncher.launch(
        if (missingMedia.isNotEmpty()) missingMedia else requiredMediaPermissions
      )
      return
    }
    viewModel.scanLibrary(
      unknownArtist = strings.unknownArtist,
      unknownAlbum = strings.unknownAlbum,
      unknownVideo = strings.unknownVideoLabel,
      includeAudio = hasReadAudio(),
      includeVideo = hasReadVideo(),
      showProgress = showProgress,
    )
  }

  val selectConverterFolderLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
      if (uri != null) {
        runCatching {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
          )
        }
        viewModel.onConverterOutputTreeSelected(uri) { ok, folderDisplay ->
          scope.launch {
            snackbarHostState.showSnackbar(
              if (ok) "${strings.converterFolderAuthorized}: $folderDisplay"
              else strings.converterFolderNeeded
            )
          }
        }
      }
    }

  val createUsageJsonLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      if (uri != null) {
        runCatching {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
          )
        }
        viewModel.onExternalUsageFileCreated(uri) { ok, _ ->
          scope.launch {
            snackbarHostState.showSnackbar(
              if (ok) strings.usageSyncActive else strings.usageSyncInactive
            )
          }
        }
      }
    }

  val importUsageJsonLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        runCatching {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        }
        viewModel.importUsageStats(uri) { res ->
          scope.launch {
            snackbarHostState.showSnackbar(
              if (res.success) strings.usageImportSuccessFormat(res.importedCount)
              else strings.usageImportFailed
            )
          }
        }
      }
    }

  val createPlaylistBackupJsonLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      if (uri != null) {
        runCatching {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
          )
        }
        viewModel.onPlaylistBackupFileCreated(uri) { ok, _ ->
          scope.launch {
            snackbarHostState.showSnackbar(
              if (ok) strings.playlistBackupActive else strings.playlistBackupInactive
            )
          }
        }
      }
    }

  val importPlaylistsJsonLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        runCatching {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        }
        viewModel.importPlaylists(uri) { res ->
          scope.launch {
            snackbarHostState.showSnackbar(
              if (res.success) {
                strings.playlistImportSuccessFormat(res.importedPlaylistsCount, res.importedTracksCount)
              } else {
                strings.playlistImportFailed
              }
            )
          }
        }
      }
    }

  // Re-check permissions when returning from system Settings & auto-execute incremental sync on resume
  var skippedInitialResumeSync by remember { mutableStateOf(false) }
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        val audioOk = hasReadAudio()
        val videoOk = hasReadVideo()
        val anyOk = audioOk || videoOk
        val newlyGranted = anyOk && !hasAudioPermission
        hasAudioPermission = anyOk
        hasVideoPermission = videoOk
        if (anyOk && (newlyGranted || skippedInitialResumeSync)) {
          viewModel.scanLibrary(
            unknownArtist = strings.unknownArtist,
            unknownAlbum = strings.unknownAlbum,
            unknownVideo = strings.unknownVideoLabel,
            includeAudio = audioOk,
            includeVideo = videoOk,
            showProgress = false,
          )
        }
        skippedInitialResumeSync = true
        val overlayAllowed = NativePermissionsHelper.canDrawOverlays(context)
        canDrawOverlays = overlayAllowed
        if (overlayAllowed && pendingOverlayLaunchAfterGrant) {
          pendingOverlayLaunchAfterGrant = false
          FloatingOverlayService.start(context)
        }
        viewModel.playerController.refreshAudioOutputRoute()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  var requestedAuxiliaryPermissions by rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(hasAudioPermission) {
    if (hasAudioPermission) {
      viewModel.scanLibrary(
        unknownArtist = strings.unknownArtist,
        unknownAlbum = strings.unknownAlbum,
        unknownVideo = strings.unknownVideoLabel,
        includeAudio = hasReadAudio(),
        includeVideo = hasReadVideo(),
        showProgress = false,
      )
      val auxiliaryMissing = missingPermissionsToRequest()
      if (!requestedAuxiliaryPermissions && auxiliaryMissing.isNotEmpty()) {
        requestedAuxiliaryPermissions = true
        permissionLauncher.launch(auxiliaryMissing)
      }
    } else {
      val toAsk = missingPermissionsToRequest()
      if (toAsk.isNotEmpty()) {
        requestedAuxiliaryPermissions = true
        permissionLauncher.launch(toAsk)
      }
    }
  }

  val libraryTracks by viewModel.filteredLibraryTracks.collectAsStateWithLifecycle()
  val folders by viewModel.availableFolders.collectAsStateWithLifecycle()
  val selectedMediaFilter by viewModel.mediaFilter.collectAsStateWithLifecycle()
  val favoriteUris by viewModel.favoriteUris.collectAsStateWithLifecycle()
  val historyItems by viewModel.historyItems.collectAsStateWithLifecycle()
  val conversionState by viewModel.conversionState.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
  val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
  val playlists by viewModel.playlists.collectAsStateWithLifecycle()
  val playback by viewModel.playbackState.collectAsStateWithLifecycle()
  val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
  val panAngle by viewModel.panAngleDegrees.collectAsStateWithLifecycle()
  val audioAmplitude by viewModel.audioAmplitude.collectAsStateWithLifecycle()
  val bassEnergy by viewModel.bassEnergy.collectAsStateWithLifecycle()
  val waveformSamples by viewModel.waveformSamples.collectAsStateWithLifecycle()
  val spectrumBins by viewModel.spectrumBins.collectAsStateWithLifecycle()
  val eqEnabled by viewModel.eqEnabled.collectAsStateWithLifecycle()
  val eqPreset by viewModel.eqPreset.collectAsStateWithLifecycle()
  val eqBandGainsDb by viewModel.eqBandGainsDb.collectAsStateWithLifecycle()
  val weeklySummary by viewModel.weeklySummary.collectAsStateWithLifecycle()
  val selectedTimeframe by viewModel.selectedTimeframe.collectAsStateWithLifecycle()

  val hasAuthorizedConverterFolder =
    remember(preferences.converterOutputTreeUri) {
      MediaConverter.hasValidTreePermission(context, preferences.converterOutputTreeUri)
    }

  var currentTab by rememberSaveable { mutableStateOf(MainTab.LIBRARY) }
  var openedPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
  var isVideoFullscreen by rememberSaveable { mutableStateOf(false) }
  var isVideoLandscape by rememberSaveable { mutableStateOf(false) }
  var isVideoScreenLocked by rememberSaveable { mutableStateOf(false) }
  var videoBrightness by rememberSaveable { mutableFloatStateOf(-1f) }
  var trackPendingPlaylist by remember { mutableStateOf<AudioTrack?>(null) }
  var trackPendingConvert by remember { mutableStateOf<AudioTrack?>(null) }
  var showCreatePlaylistFromSheet by remember { mutableStateOf(false) }
  var show8DControlSheet by remember { mutableStateOf(false) }
  var showSleepTimerSheet by remember { mutableStateOf(false) }
  var showEqualizerSheet by remember { mutableStateOf(false) }

  LaunchedEffect(openPlayerTabTick) {
    if (openPlayerTabTick > 0) {
      currentTab = MainTab.PLAYER
      openedPlaylistId = null
    }
  }

  val isShowingNormalVideo =
    currentTab == MainTab.PLAYER && playback.currentTrack?.isVideo == true && !playback.isVideoAudioOnly
  val isShowingVideoTheater =
    isShowingNormalVideo && (isVideoFullscreen || isVideoLandscape || isVideoScreenLocked)

  LaunchedEffect(isShowingNormalVideo) {
    if (!isShowingNormalVideo) {
      isVideoFullscreen = false
      isVideoLandscape = false
      isVideoScreenLocked = false
      videoBrightness = -1f
    }
  }

  val hostView = LocalView.current
  val hostActivity = context as? Activity

  // Rotate ONLY during active video landscape mode, leaving phone orientation unlocked elsewhere
  DisposableEffect(isVideoLandscape, isShowingNormalVideo) {
    val forceLandscape = isShowingNormalVideo && isVideoLandscape
    hostActivity?.requestedOrientation =
      if (forceLandscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
      else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    onDispose {
      hostActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
  }

  // Strictly in-app video playback brightness (window layout attributes only; never touches global system settings)
  DisposableEffect(videoBrightness, isShowingNormalVideo) {
    val window = hostActivity?.window
    if (window != null) {
      val lp = window.attributes
      lp.screenBrightness =
        if (isShowingNormalVideo && videoBrightness in 0.05f..1.0f) videoBrightness
        else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
      window.attributes = lp
    }
    onDispose {
      hostActivity?.window?.let { w ->
        val lp = w.attributes
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        w.attributes = lp
      }
    }
  }

  // Keep screen awake automatically while a video is actively playing (not paused/stopped/audio-only)
  val keepScreenAwake =
    shouldKeepScreenAwakeForVideo(
      isShowingVideoSurface = isShowingNormalVideo,
      isPlaying = playback.isPlaying,
      isVideoTrack = playback.currentTrack?.isVideo == true,
      isAudioOnly = playback.isVideoAudioOnly,
    )

  VideoKeepScreenOnEffect(
    keepScreenAwake = keepScreenAwake,
    window = hostActivity?.window,
    hostView = hostView,
  )

  // Hide system status bar / notifications & navigation bar when in locked/fullscreen video theater
  DisposableEffect(isShowingVideoTheater, isVideoScreenLocked) {
    val window = hostActivity?.window
    val controller = if (window != null) WindowCompat.getInsetsController(window, hostView) else null
    if (controller != null) {
      if (isShowingVideoTheater) {
        controller.systemBarsBehavior =
          WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
      } else {
        controller.show(WindowInsetsCompat.Type.systemBars())
      }
    }
    onDispose {
      if (window != null) {
        WindowCompat.getInsetsController(window, hostView).show(WindowInsetsCompat.Type.systemBars())
      }
    }
  }

  ThemedBackground {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavigationRail = maxWidth >= 600.dp

      Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
          if (!useNavigationRail) {
            com.example.ui.components.FloatingGlassBottomNavBar(
              selectedTab = currentTab,
              tabs = MainTab.entries,
              labelForTab = { tab -> tabLabel(tab, strings) },
              iconForTab = { tab, selected ->
                val (filledIcon, outlinedIcon) = tabIcons(tab)
                if (selected) filledIcon else outlinedIcon
              },
              onSelectTab = { tab ->
                currentTab = tab
                if (tab != MainTab.PLAYLISTS) openedPlaylistId = null
              },
              isPlaying = playback.isPlaying,
            )
          }
        },
      ) { innerPadding ->
        val bottomBarHeight = innerPadding.calculateBottomPadding()
        val hasMiniPlayer = currentTab != MainTab.PLAYER && playback.currentTrack != null
        val listBottomPadding = bottomBarHeight + if (hasMiniPlayer) 128.dp else 44.dp
        val fabBottomOffset = bottomBarHeight + if (hasMiniPlayer) 114.dp else 28.dp

        Row(
          modifier =
            Modifier.fillMaxSize()
              .windowInsetsPadding(WindowInsets.statusBars)
        ) {
          if (useNavigationRail) {
            NavigationRail(
              containerColor = palette.navBarBackground,
              modifier = Modifier.fillMaxHeight(),
            ) {
              MainTab.entries.forEach { tab ->
                val selected = currentTab == tab
                val label = tabLabel(tab, strings)
                val (filledIcon, outlinedIcon) = tabIcons(tab)
                NavigationRailItem(
                  selected = selected,
                  onClick = {
                    currentTab = tab
                    if (tab != MainTab.PLAYLISTS) openedPlaylistId = null
                  },
                  icon = {
                    NavTabIcon(
                      icon = if (selected) filledIcon else outlinedIcon,
                      contentDescription = label,
                      selected = selected,
                      isGlassTheme = palette.isGlassTheme,
                    )
                  },
                  label = { Text(label, maxLines = 1, softWrap = false) },
                  colors =
                    NavigationRailItemDefaults.colors(
                      selectedIconColor = palette.accentGlow,
                      selectedTextColor = palette.accentGlow,
                      indicatorColor = Color.Transparent,
                      unselectedIconColor = palette.textSecondary,
                      unselectedTextColor = palette.textSecondary,
                    ),
                  modifier = Modifier.testTag("rail_${tab.name.lowercase()}"),
                )
              }
            }
          }

          Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when (currentTab) {
              MainTab.LIBRARY -> {
                LibraryScreen(
                  tracks = libraryTracks,
                  folders = folders,
                  selectedFolder = selectedFolder,
                  selectedMediaFilter = selectedMediaFilter,
                  favoriteUris = favoriteUris,
                  searchQuery = searchQuery,
                  currentSort = preferences.librarySort,
                  currentTrackId = playback.currentTrack?.id,
                  isPlaying = playback.isPlaying,
                  isScanning = isScanning,
                  hasAudioPermission = hasAudioPermission,
                  onRequestPermission = {
                    val toAsk = missingPermissionsToRequest()
                    permissionLauncher.launch(if (toAsk.isNotEmpty()) toAsk else requiredMediaPermissions)
                  },
                  onOpenAppSettings = {
                    context.startActivity(
                      Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}"),
                      )
                    )
                  },
                  onRescan = { triggerLibraryRescan(showProgress = true) },
                  onSearchQueryChange = viewModel::setSearchQuery,
                  onSelectFolder = viewModel::setSelectedFolder,
                  onSelectMediaFilter = { filter ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                      if (filter == com.example.model.LibraryMediaFilter.VIDEO && !hasVideoPermission) {
                        permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_VIDEO))
                      } else if (filter == com.example.model.LibraryMediaFilter.AUDIO && !hasReadAudio()) {
                        permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO))
                      }
                    } else if (!hasReadAudio()) {
                      permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                    }
                    viewModel.setMediaFilter(filter)
                  },
                  onSelectSort = viewModel::setLibrarySort,
                  onTrackClick = viewModel::playLibraryTrack,
                  onPlayAll = { viewModel.playAllLibrary(shuffled = false) },
                  onShuffleAll = { viewModel.playAllLibrary(shuffled = true) },
                  onAddToPlaylistClick = { trackPendingPlaylist = it },
                  onToggleFavorite = { track ->
                    viewModel.toggleFavorite(track, strings.favoritesPlaylistName)
                  },
                  onPlayVideoNormal = { track ->
                    viewModel.playVideoWithMode(track, audioOnly = false)
                    currentTab = MainTab.PLAYER
                  },
                  onPlayVideoAudioOnly = { track ->
                    viewModel.playVideoWithMode(track, audioOnly = true)
                  },
                  onOpenConvertDialog = { track -> trackPendingConvert = track },
                  onDeleteTrack = { track -> viewModel.deleteMediaItem(track) },
                  bottomOverlayPadding = listBottomPadding,
                  fabBottomOffset = fabBottomOffset,
                )
              }

              MainTab.PLAYLISTS -> {
                val activePlaylistId = openedPlaylistId
                if (activePlaylistId != null) {
                  val detailFlow =
                    remember(activePlaylistId) {
                      viewModel.observePlaylistDetail(activePlaylistId)
                    }
                  val detailState by
                    detailFlow.collectAsStateWithLifecycle(initialValue = null to emptyList())
                  PlaylistDetailScreen(
                    playlist = detailState.first,
                    entries = detailState.second,
                    libraryTracks = libraryTracks,
                    favoriteUris = favoriteUris,
                    currentTrackId = playback.currentTrack?.id,
                    onBack = { openedPlaylistId = null },
                    onRenamePlaylist = { newName ->
                      viewModel.renamePlaylist(activePlaylistId, newName)
                    },
                    onPickCoverUri = { uri ->
                      viewModel.updatePlaylistCover(activePlaylistId, uri)
                    },
                    onRemoveCover = { viewModel.clearPlaylistCover(activePlaylistId) },
                    onSelectSort = { sortOption ->
                      viewModel.updatePlaylistSort(activePlaylistId, sortOption)
                    },
                    onPlayPlaylist = { entries, startIndex ->
                      viewModel.playPlaylistEntries(entries, startIndex)
                    },
                    onAddTracks = { tracks ->
                      viewModel.addTracksToPlaylist(activePlaylistId, tracks)
                    },
                    onRemoveEntry = viewModel::removeTrackFromPlaylist,
                    onPersistManualOrder = viewModel::persistManualPlaylistOrder,
                    onToggleFavorite = { track ->
                      viewModel.toggleFavorite(track, strings.favoritesPlaylistName)
                    },
                    onPlayVideoNormal = { track ->
                      viewModel.playVideoWithMode(track, audioOnly = false)
                      currentTab = MainTab.PLAYER
                    },
                    onPlayVideoAudioOnly = { track ->
                      viewModel.playVideoWithMode(track, audioOnly = true)
                    },
                    onOpenConvertDialog = { track -> trackPendingConvert = track },
                    onDeleteTrack = { track -> viewModel.deleteMediaItem(track) },
                    bottomOverlayPadding = listBottomPadding,
                    fabBottomOffset = fabBottomOffset,
                  )
                } else {
                  PlaylistsScreen(
                    playlists = playlists,
                    preferences = preferences,
                    onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                    onOpenPlaylist = { playlist -> openedPlaylistId = playlist.id },
                    onDeletePlaylist = viewModel::deletePlaylist,
                    onSelectPlaylistBackupFile = {
                      createPlaylistBackupJsonLauncher.launch(PlaylistBackupEngine.PLAYLIST_BACKUP_FILENAME)
                    },
                    onImportPlaylists = {
                      importPlaylistsJsonLauncher.launch(
                        arrayOf("application/json", "text/plain", "*/*")
                      )
                    },
                    isRefreshing = isScanning,
                    onRefresh = { triggerLibraryRescan(showProgress = true) },
                    bottomOverlayPadding = listBottomPadding,
                    fabBottomOffset = fabBottomOffset,
                  )
                }
              }

              MainTab.PLAYER -> {
                if (!isShowingVideoTheater) {
                  val isCurrentFav =
                    playback.currentTrack?.contentUri?.toString()?.let { it in favoriteUris } == true
                  NowPlayingScreen(
                    playback = playback,
                    sleepTimerState = sleepTimerState,
                    isEqEnabled = eqEnabled,
                    isCurrentTrackFavorite = isCurrentFav,
                    panAngleDegrees = panAngle,
                    audioAmplitude = audioAmplitude,
                    bassEnergy = bassEnergy,
                    waveformSamples = waveformSamples,
                    spectrumBins = spectrumBins,
                    visualizerMode = preferences.visualizerMode,
                    onSelectVisualizerMode = viewModel::setVisualizerMode,
                    onPlayPause = viewModel.playerController::togglePlayPause,
                    onNext = viewModel.playerController::skipNext,
                    onPrevious = viewModel.playerController::skipPrevious,
                    onSeek = viewModel.playerController::seekTo,
                    onToggleShuffle = viewModel.playerController::toggleShuffle,
                    onCycleRepeat = viewModel.playerController::cycleRepeatMode,
                    onToggle8D = viewModel.playerController::toggle8DAudio,
                    onOpen8DPanel = { show8DControlSheet = true },
                    onOpenSleepTimer = { showSleepTimerSheet = true },
                    onOpenEqualizer = { showEqualizerSheet = true },
                    onJumpToQueueIndex = viewModel.playerController::jumpToQueueIndex,
                    onAddToPlaylistClick = { trackPendingPlaylist = it },
                    onToggleFavorite = { track ->
                      viewModel.toggleFavorite(track, strings.favoritesPlaylistName)
                    },
                    onSetVideoAudioOnlyMode = viewModel.playerController::setVideoAudioOnlyMode,
                    onAttachVideoTextureView = viewModel.playerController::attachVideoTextureView,
                    onDetachVideoTextureView = viewModel.playerController::detachVideoTextureView,
                    videoBrightness = videoBrightness,
                    onEnterFullscreen = { isVideoFullscreen = true },
                    onSetVolumeBoost = viewModel.playerController::setVolumeBoostPercent,
                    bottomOverlayPadding = if (useNavigationRail) 32.dp else bottomBarHeight + 40.dp,
                  )
                }
              }

              MainTab.HISTORY -> {
                HistoryScreen(
                  historyItems = historyItems,
                  favoriteUris = favoriteUris,
                  onPlayFromHistory = { item, resume ->
                    viewModel.playFromHistory(item, resume)
                    if (item.track.isVideo && preferences.defaultVideoPlaybackMode == com.example.model.VideoPlaybackMode.VIDEO_AND_AUDIO) {
                      currentTab = MainTab.PLAYER
                    }
                  },
                  onToggleFavorite = { track ->
                    viewModel.toggleFavorite(track, strings.favoritesPlaylistName)
                  },
                  onDeleteItem = viewModel::deleteHistoryItem,
                  onClearAll = viewModel::clearHistory,
                  isRefreshing = isScanning,
                  onRefresh = { triggerLibraryRescan(showProgress = true) },
                  bottomOverlayPadding = listBottomPadding,
                  fabBottomOffset = fabBottomOffset,
                )
              }

              MainTab.MORE -> {
                MoreScreen(
                  preferences = preferences,
                  weeklySummary = weeklySummary,
                  selectedTimeframe = selectedTimeframe,
                  canDrawOverlays = canDrawOverlays,
                  onSelectTheme = viewModel::setThemeStyle,
                  onToggleDarkMode = viewModel::setDarkMode,
                  onSelectLanguage = viewModel::setLanguage,
                  onSet8DEnabled = viewModel.playerController::set8DAudio,
                  onSet8DSpeed = viewModel.playerController::set8DSpeed,
                  onSet8DIntensity = viewModel.playerController::set8DIntensity,
                  onOpen8DPanel = { show8DControlSheet = true },
                  onOpenEqualizer = { showEqualizerSheet = true },
                  onOpenSleepTimer = { showSleepTimerSheet = true },
                  onRequestOverlayPermission = {
                    pendingOverlayLaunchAfterGrant = true
                    runCatching {
                      context.startActivity(NativePermissionsHelper.buildOverlaySettingsIntent(context))
                    }
                  },
                  onLaunchFloatingOverlay = {
                    if (NativePermissionsHelper.canDrawOverlays(context)) {
                      FloatingOverlayService.start(context)
                    } else {
                      pendingOverlayLaunchAfterGrant = true
                      runCatching {
                        context.startActivity(NativePermissionsHelper.buildOverlaySettingsIntent(context))
                      }
                    }
                  },
                  onSelectTimeframe = viewModel::setAnalyticsTimeframe,
                  onSetListeningTimeUnit = viewModel::setListeningTimeUnit,
                  onSetAppGlassOpacity = viewModel::setAppGlassOpacity,
                  onSetVideoSeekStepSeconds = viewModel::setVideoSeekStepSeconds,
                  onSelectAutoSyncFile = {
                    createUsageJsonLauncher.launch(UsageStatsEngine.USAGE_STATS_FILENAME)
                  },
                  onImportUsageStats = {
                    importUsageJsonLauncher.launch(
                      arrayOf("application/json", "text/plain", "*/*")
                    )
                  },
                  onPlayTop7Mix = {
                    viewModel.playWeeklyTop7Mix()
                    currentTab = MainTab.PLAYER
                  },
                  hasConverterFolder = hasAuthorizedConverterFolder,
                  onSetDefaultVideoPlaybackMode = viewModel::setDefaultVideoPlaybackMode,
                  onSelectConverterFolder = { selectConverterFolderLauncher.launch(null) },
                  bottomOverlayPadding = listBottomPadding,
                )
              }
            }

            if (hasMiniPlayer) {
              Column(
                modifier =
                  Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .then(
                      if (useNavigationRail) Modifier.navigationBarsPadding()
                      else Modifier.padding(bottom = bottomBarHeight)
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
              ) {
                MiniPlayerBar(
                  playback = playback,
                  onClick = { currentTab = MainTab.PLAYER },
                  onPlayPause = viewModel.playerController::togglePlayPause,
                  onNext = viewModel.playerController::skipNext,
                  onToggle8D = { show8DControlSheet = true },
                )
              }
            }
          }
        }
      }

      if (isShowingVideoTheater) {
        VideoTheaterOverlay(
          playback = playback,
          isLandscape = isVideoLandscape,
          isLocked = isVideoScreenLocked,
          onToggleLandscape = { isVideoLandscape = !isVideoLandscape },
          onSetLocked = { locked -> isVideoScreenLocked = locked },
          onSetVideoAudioOnlyMode = { audioOnly ->
            if (audioOnly) {
              isVideoFullscreen = false
              isVideoScreenLocked = false
              isVideoLandscape = false
            }
            viewModel.playerController.setVideoAudioOnlyMode(audioOnly)
          },
          onPlayPause = viewModel.playerController::togglePlayPause,
          onNext = viewModel.playerController::skipNext,
          onPrevious = viewModel.playerController::skipPrevious,
          onSeek = viewModel.playerController::seekTo,
          onToggleShuffle = viewModel.playerController::toggleShuffle,
          onCycleRepeat = viewModel.playerController::cycleRepeatMode,
          onAttachVideoTextureView = viewModel.playerController::attachVideoTextureView,
          onDetachVideoTextureView = viewModel.playerController::detachVideoTextureView,
          onExitTheater = {
            isVideoFullscreen = false
            isVideoScreenLocked = false
            isVideoLandscape = false
          },
          seekStepMs = preferences.videoSeekStepSeconds * 1000L,
        )
      }
    }
  }

  val pendingTrack = trackPendingPlaylist
  if (pendingTrack != null) {
    AddToPlaylistBottomSheet(
      track = pendingTrack,
      playlists = playlists,
      onSelectPlaylist = { playlist ->
        viewModel.addTracksToPlaylist(playlist.id, listOf(pendingTrack))
        trackPendingPlaylist = null
        val displayPlaylistName =
          if (playlist.isFavorites) strings.favoritesPlaylistName else playlist.name
        scope.launch {
          snackbarHostState.showSnackbar("${strings.addedToPlaylist}: $displayPlaylistName")
        }
      },
      onCreateNewPlaylist = { showCreatePlaylistFromSheet = true },
      onDismiss = { trackPendingPlaylist = null },
    )
  }

  if (showCreatePlaylistFromSheet && pendingTrack != null) {
    CreatePlaylistDialog(
      onConfirm = { name ->
        viewModel.createPlaylist(name) { newId ->
          viewModel.addTracksToPlaylist(newId, listOf(pendingTrack))
        }
        showCreatePlaylistFromSheet = false
        trackPendingPlaylist = null
        scope.launch { snackbarHostState.showSnackbar("${strings.addedToPlaylist}: $name") }
      },
      onDismiss = { showCreatePlaylistFromSheet = false },
    )
  }

  val convertTarget = trackPendingConvert
  if (convertTarget != null) {
    ConvertFormatDialog(
      track = convertTarget,
      hasAuthorizedFolder = hasAuthorizedConverterFolder,
      onAuthorizeFolder = { selectConverterFolderLauncher.launch(null) },
      onSelectFormat = { chosenFormat ->
        trackPendingConvert = null
        viewModel.convertMediaItem(
          track = convertTarget,
          format = chosenFormat,
          unknownArtist = strings.unknownArtist,
          unknownAlbum = strings.unknownAlbum,
          unknownVideo = strings.unknownVideoLabel,
        )
      },
      onDismiss = { trackPendingConvert = null },
    )
  }

  if (conversionState.isRunning || conversionState.outputDisplayPath != null || conversionState.errorMessage != null) {
    ConversionProgressDialog(
      state = conversionState,
      onDismiss = viewModel::dismissConversionState,
    )
  }

  if (show8DControlSheet) {
    Audio8DControlSheet(
      isEnabled = playback.is8DEnabled,
      isPlaying = playback.isPlaying,
      speed = playback.audio8DSpeed,
      intensityPercent = playback.audio8DIntensity,
      panAngleDegrees = panAngle,
      onToggleEnabled = viewModel.playerController::set8DAudio,
      onSelectSpeed = viewModel.playerController::set8DSpeed,
      onChangeIntensity = viewModel.playerController::set8DIntensity,
      onDismiss = { show8DControlSheet = false },
    )
  }

  if (showSleepTimerSheet) {
    SleepTimerSheet(
      sleepTimerState = sleepTimerState,
      onSelectMinutes = viewModel.playerController::startSleepTimerMinutes,
      onSelectEndOfTrack = viewModel.playerController::startSleepTimerEndOfTrack,
      onCancelTimer = viewModel.playerController::cancelSleepTimer,
      onDismiss = { showSleepTimerSheet = false },
    )
  }

  if (showEqualizerSheet) {
    EqualizerSheet(
      isEnabled = eqEnabled,
      activePreset = eqPreset,
      bandGainsDb = eqBandGainsDb,
      playback = playback,
      panAngleDegrees = panAngle,
      onToggleEnabled = viewModel.playerController::setEqEnabled,
      onSelectPreset = viewModel.playerController::applyEqPreset,
      onChangeBandGain = viewModel.playerController::setEqBandGain,
      onToggle8D = viewModel.playerController::set8DAudio,
      onSelect8DSpeed = viewModel.playerController::set8DSpeed,
      onChange8DIntensity = viewModel.playerController::set8DIntensity,
      onSetVolumeBoost = viewModel.playerController::setVolumeBoostPercent,
      onToggleAmplifier = viewModel.playerController::setAmplifierEnabled,
      onChangeAmplifierLevel = viewModel.playerController::setAmplifierLevelPercent,
      onDismiss = { showEqualizerSheet = false },
    )
  }
}

private fun tabLabel(tab: MainTab, strings: com.example.localization.AppStrings): String =
  when (tab) {
    MainTab.LIBRARY -> strings.navLibrary
    MainTab.PLAYLISTS -> strings.navPlaylists
    MainTab.PLAYER -> strings.navPlayer
    MainTab.HISTORY -> strings.navHistory
    MainTab.MORE -> strings.navMore
  }

private fun tabIcons(tab: MainTab) =
  when (tab) {
    MainTab.LIBRARY -> Icons.Rounded.LibraryMusic to Icons.Outlined.LibraryMusic
    MainTab.PLAYLISTS -> Icons.AutoMirrored.Rounded.QueueMusic to Icons.AutoMirrored.Outlined.QueueMusic
    MainTab.PLAYER -> Icons.Rounded.PlayCircle to Icons.Outlined.PlayCircle
    MainTab.HISTORY -> Icons.Rounded.History to Icons.Outlined.History
    MainTab.MORE -> Icons.Rounded.MoreHoriz to Icons.Outlined.MoreHoriz
  }

@Composable
internal fun VideoKeepScreenOnEffect(
  keepScreenAwake: Boolean,
  window: android.view.Window?,
  hostView: android.view.View,
) {
  DisposableEffect(keepScreenAwake, window, hostView) {
    hostView.keepScreenOn = keepScreenAwake
    if (window != null) {
      if (keepScreenAwake) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
    }
    onDispose {
      hostView.keepScreenOn = false
      window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }
}

internal fun shouldKeepScreenAwakeForVideo(
  isShowingVideoSurface: Boolean,
  isPlaying: Boolean,
  isVideoTrack: Boolean,
  isAudioOnly: Boolean,
): Boolean = isShowingVideoSurface && isPlaying && isVideoTrack && !isAudioOnly

@Composable
private fun NavTabIcon(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  selected: Boolean,
  isGlassTheme: Boolean,
) {
  Icon(imageVector = icon, contentDescription = contentDescription)
}
