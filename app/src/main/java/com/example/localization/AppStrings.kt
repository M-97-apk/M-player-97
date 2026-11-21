package com.example.localization

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.model.AppLanguage

@Immutable
interface AppStrings {
  val appName: String
  val offlineBadge: String
  val navLibrary: String
  val navPlaylists: String
  val navPlayer: String
  val navHistory: String
  val navMore: String
  val searchPlaceholder: String
  val tracksCountFormat: (Int) -> String
  val allFolders: String
  val shuffleAll: String
  val playAll: String
  val scanLibrary: String
  val emptyLibraryTitle: String
  val emptyLibrarySubtitle: String
  val permissionTitle: String
  val permissionBody: String
  val grantPermission: String
  val openSettings: String
  val sortTitle: String
  val sortAlphaAsc: String
  val sortAlphaDesc: String
  val sortDateModifiedDesc: String
  val sortDateModifiedAsc: String
  val sortArtistAsc: String
  val sortDurationDesc: String
  val sortManual: String
  val sortDateAddedDesc: String
  val sortDateAddedAsc: String
  val playlistsTitle: String
  val createPlaylist: String
  val playlistNameHint: String
  val create: String
  val cancel: String
  val delete: String
  val rename: String
  val save: String
  val changeCover: String
  val removeCover: String
  val addTracks: String
  val removeFromPlaylist: String
  val emptyPlaylistsTitle: String
  val emptyPlaylistsSubtitle: String
  val emptyPlaylistTracksTitle: String
  val emptyPlaylistTracksSubtitle: String
  val manualDragTip: String
  val moveUp: String
  val moveDown: String
  val addToPlaylist: String
  val addedToPlaylist: String
  val nowPlaying: String
  val noTrackPlaying: String
  val pickTrackHint: String
  val audio8DTitle: String
  val audio8DOn: String
  val audio8DOff: String
  val audio8DSubtitle: String
  val queueTitle: String
  val moreTitle: String
  val settingsSection: String
  val themesTitle: String
  val themeRoyalPurpleName: String
  val themeRoyalPurpleDesc: String
  val themeDeepBurgundyName: String
  val themeDeepBurgundyDesc: String
  val themeMidnightBlueName: String
  val themeMidnightBlueDesc: String
  val themePitchBlackGoldName: String
  val themePitchBlackGoldDesc: String
  val themeLuxuryBrownName: String
  val themeLuxuryBrownDesc: String
  val themeCosmicStarfieldName: String
  val themeCosmicStarfieldDesc: String
  val themePlainBlackName: String
  val themePlainBlackDesc: String
  val appGlassOpacityTitle: String
  val appGlassOpacitySubtitle: String
  val appearanceTitle: String
  val darkModeLabel: String
  val lightModeLabel: String
  val languageTitle: String
  val englishLabel: String
  val arabicLabel: String
  val overlaySectionTitle: String
  val overlayGrantedTitle: String
  val overlayNeededTitle: String
  val overlayDescription: String
  val enableOverlayPermission: String
  val launchFloatingPlayer: String
  val weeklySummaryTitle: String
  val weeklySummarySubtitle: String
  val top7Title: String
  val playsLabel: String
  val totalPlaysLabel: String
  val listeningMinutesLabel: String
  val uniqueSongsLabel: String
  val playTop7Mix: String
  val emptyWeeklyTitle: String
  val emptyWeeklySubtitle: String
  val expandFullApp: String
  val closeOverlay: String
  val overlaySetupPrompt: String
  val openSystemOverlaySettings: String
  val dismiss: String
  val unknownArtist: String
  val unknownAlbum: String
  val visualizerAlbumArt: String
  val visualizerEnergyOrb: String
  val visualizerTouchHint: String
  val audio8DPanelTitle: String
  val audio8DToggleLabel: String
  val audio8DRadarTitle: String
  val audio8DSpeedTitle: String
  val audio8DSpeedSlow: String
  val audio8DSpeedNormal: String
  val audio8DSpeedFast: String
  val audio8DIntensityTitle: String
  val audio8DDefaultSettingsTitle: String
  val audio8DOpenPanel: String
  val audio8DTapToTune: String
  val radarFront: String
  val radarBack: String
  val radarLeft: String
  val radarRight: String
  val visualizerSpectrum: String
  val visualizerCircular: String
  val sleepTimerTitle: String
  val sleepTimerOff: String
  val sleepTimer5Min: String
  val sleepTimer10Min: String
  val sleepTimer15Min: String
  val sleepTimer30Min: String
  val sleepTimer45Min: String
  val sleepTimer60Min: String
  val sleepTimerCustom: String
  val sleepTimerHoursLabel: String
  val sleepTimerMinutesLabel: String
  val sleepTimerStartCustom: String
  val sleepTimerEndOfTrack: String
  val sleepTimerCancel: String
  val sleepTimerFadingOut: String
  val sleepTimerRemainingFormat: (String) -> String
  val equalizerTitle: String
  val equalizerEnableLabel: String
  val equalizerPresetsTitle: String
  val equalizerPresetFlat: String
  val equalizerPresetBassBoost: String
  val equalizerPresetRock: String
  val equalizerPresetPop: String
  val equalizerPresetClassical: String
  val equalizerPresetVocal: String
  val equalizerPresetCustom: String
  val equalizerBassLabel: String
  val equalizerMidLabel: String
  val equalizerTrebleLabel: String
  val equalizerReset: String
  val timeframeDaily: String
  val timeframeWeekly: String
  val timeframeMonthly: String
  val timeframeYearly: String
  val insightsTitle: String
  val insightsSubtitle: String
  val usageSyncTitle: String
  val usageSyncActive: String
  val usageSyncInactive: String
  val usageSyncButton: String
  val usageSyncChangeFileButton: String
  val usageImportButton: String
  val usageImportSuccessFormat: (Int) -> String
  val usageImportFailed: String
  val usageSyncLastSyncedPrefix: String
  val listeningUnitSecondsLabel: String
  val listeningUnitMinutesLabel: String
  val listeningUnitSecondsShort: String
  val listeningUnitMinutesShort: String
  val widgetQuickSettingsTitle: String
  val widgetColorSectionTitle: String
  val widgetColorMatchApp: String
  val widgetColorNeutral: String
  val widgetGlassSectionTitle: String
  val widgetGlassFrostedBlur: String
  val widgetGlassSemiFrosted: String
  val widgetGlassOpaque: String
  val widgetGlassTransparent: String
  val widgetTransparencySliderLabel: String
  val playlistBackupSettingsTitle: String
  val playlistBackupSubtitle: String
  val playlistBackupActive: String
  val playlistBackupInactive: String
  val playlistBackupSelectFile: String
  val playlistBackupChangeFile: String
  val playlistImportButton: String
  val playlistImportSuccessFormat: (Int, Int) -> String
  val playlistImportFailed: String
  val favoritesPlaylistName: String
  val filterAllMedia: String
  val filterAudioOnly: String
  val filterVideoOnly: String
  val unknownVideoLabel: String
  val videoBadgeLabel: String
  val audioBadgeLabel: String
  val likeToggleAdd: String
  val likeToggleRemove: String
  val videoOptionsTitle: String
  val playAudioTrack: String
  val playVideoNormal: String
  val playVideoAudioOnly: String
  val convertFormatOption: String
  val deletedMediaFormat: (String) -> String
  val deleteMediaFailed: String
  val videoDisplayModePill: String
  val audioOnlyModePill: String
  val defaultVideoModeTitle: String
  val defaultVideoModeSubtitle: String
  val videoModeNormalLabel: String
  val videoModeAudioOnlyLabel: String
  val convertDialogTitle: String
  val convertChoosePrompt: String
  val convertFullVideo: String
  val convertFullVideoDesc: String
  val convertMp3AudioOnly: String
  val convertMp3AudioOnlyDesc: String
  val converterFolderTitle: String
  val converterFolderAuthorized: String
  val converterFolderNeeded: String
  val converterFolderAuthorizeBtn: String
  val convertingProgressFormat: (Int) -> String
  val conversionSuccessFormat: (String) -> String
  val conversionFailedLabel: String
  val historyTitle: String
  val historyEmptyTitle: String
  val historyEmptySubtitle: String
  val historyClearAll: String
  val historyPausedAtFormat: (String) -> String
  val resumePromptTitle: String
  val resumePromptMessageFormat: (String, String) -> String
  val resumeFromPositionBtnFormat: (String) -> String
  val restartFromBeginningBtn: String
  val externalOpenTitle: String
  val externalOpenSubtitle: String
  val externalPlayNormal: String
  val externalPlayAudioOnly: String
  val videoRotateLandscape: String
  val videoRotatePortrait: String
  val videoBrightness: String
  val videoBrightnessAuto: String
  val videoScreenLock: String
  val videoScreenUnlock: String
  val videoFullscreenExpand: String
  val videoFullscreenExit: String
  val volumeBoost: String
  val volumeBoostWarningTitle: String
  val volumeBoostWarningMessage: String
  val volumeBoostIgnore: String
  val volumeBoostReset: String
  val volumeBoostStandardLabel: String
  val volumeBoostUltraLabel: String
  val routeSpeaker: String
  val routeWiredHeadphones: String
  val routeBluetooth: String
  val scrollToTop: String
  val clearSearch: String
  val videoSeekControlTitle: String
  val videoSeekControlSubtitle: String
  val videoSeekPresetFormat: (Int) -> String
  val videoSeekCustomLabel: String
  val videoSeekCustomDialogTitle: String
  val videoSeekValueHint: String
  val videoSeekUnitSeconds: String
  val videoSeekUnitMinutes: String
  val videoSeekUnitHours: String
  val confirmLabel: String
  val equalizerHubBadge: String
  val volumeBoostToggleLabel: String
  val volumeBoostHardwareSynced: String
  val amplifierTitle: String
  val amplifierSubtitle: String
  val amplifierToggleLabel: String
  val amplifierGainLabel: String
  val eq8DRadarToggleShow: String
  val eq8DRadarToggleHide: String
  val locateCurrentTrack: String
}

@Immutable
object EnglishStrings : AppStrings {
  override val appName = "M player 97"
  override val offlineBadge = "100% Offline"
  override val navLibrary = "Library"
  override val navPlaylists = "Playlists"
  override val navPlayer = "Player"
  override val navHistory = "History"
  override val navMore = "More"
  override val searchPlaceholder = "Search songs, videos, artists..."
  override val tracksCountFormat: (Int) -> String = { count -> "$count items" }
  override val allFolders = "All Folders"
  override val shuffleAll = "Shuffle"
  override val playAll = "Play All"
  override val scanLibrary = "Rescan"
  override val emptyLibraryTitle = "No local media found"
  override val emptyLibrarySubtitle = "Add MP3, WAV, FLAC, M4A, or MP4 video files to your device and tap Rescan."
  override val permissionTitle = "Access Your Media Library"
  override val permissionBody = "M player 97 works 100% offline. Allow local audio & video access to scan your songs and videos."
  override val grantPermission = "Allow Media Access"
  override val openSettings = "Open App Settings"
  override val sortTitle = "Sort Order"
  override val sortAlphaAsc = "Alphabetical (A–Z)"
  override val sortAlphaDesc = "Alphabetical (Z–A)"
  override val sortDateModifiedDesc = "Date Downloaded / Modified (Newest)"
  override val sortDateModifiedAsc = "Date Downloaded / Modified (Oldest)"
  override val sortArtistAsc = "Artist (A–Z)"
  override val sortDurationDesc = "Longest Duration"
  override val sortManual = "Manual Order (Drag & Drop)"
  override val sortDateAddedDesc = "Date Added to Playlist (Newest)"
  override val sortDateAddedAsc = "Date Added to Playlist (Oldest)"
  override val playlistsTitle = "Playlists"
  override val createPlaylist = "New Playlist"
  override val playlistNameHint = "Playlist name"
  override val create = "Create"
  override val cancel = "Cancel"
  override val delete = "Delete"
  override val rename = "Rename"
  override val save = "Save"
  override val changeCover = "Custom Cover"
  override val removeCover = "Reset Cover"
  override val addTracks = "Add Songs"
  override val removeFromPlaylist = "Remove from playlist"
  override val emptyPlaylistsTitle = "No playlists yet"
  override val emptyPlaylistsSubtitle = "Create custom playlists with your own gallery covers and drag-and-drop track order."
  override val emptyPlaylistTracksTitle = "Playlist is empty"
  override val emptyPlaylistTracksSubtitle = "Tap Add Songs to populate this playlist from your local library."
  override val manualDragTip = "Long-press & drag tracks (or use arrows) to reorder manually"
  override val moveUp = "Move Up"
  override val moveDown = "Move Down"
  override val addToPlaylist = "Add to Playlist"
  override val addedToPlaylist = "Added to playlist"
  override val nowPlaying = "Now Playing"
  override val noTrackPlaying = "Nothing playing yet"
  override val pickTrackHint = "Pick any local song from Library or open an audio file from your file manager."
  override val audio8DTitle = "8D Audio"
  override val audio8DOn = "8D Active"
  override val audio8DOff = "8D Off"
  override val audio8DSubtitle = "360° spatial panning + binaural room reverb"
  override val queueTitle = "Up Next"
  override val moreTitle = "More & Insights"
  override val settingsSection = "Appearance & Language"
  override val themesTitle = "App Theme"
  override val themeRoyalPurpleName = "Royal Purple"
  override val themeRoyalPurpleDesc = "Ultra-dark #0B0316 night purple frosted glass with deep #3E1478 violet accents"
  override val themeDeepBurgundyName = "Extra-Deep Burgundy"
  override val themeDeepBurgundyDesc = "Ultra-dark #140307 royal wine frosted glass with #5A0D17 burgundy & crisp silver icons"
  override val themeMidnightBlueName = "Deep Midnight Blue"
  override val themeMidnightBlueDesc = "Ultra-dark #030914 abyssal sapphire frosted glass with deep #102C6B blue accents"
  override val themePitchBlackGoldName = "Pitch-Black & Gold"
  override val themePitchBlackGoldDesc = "Pure #000000 obsidian smoked glass with metallic #D4AF37 champagne gold borders"
  override val themeLuxuryBrownName = "Luxury Dark Brown"
  override val themeLuxuryBrownDesc = "Ultra-dark espresso #140A05 to chocolate #26130A frosted glass with warm bronze accents"
  override val themeCosmicStarfieldName = "Cosmic Starfield"
  override val themeCosmicStarfieldDesc = "Pure #000000 night sky with animated 7-second celestial starfield orbital cycles"
  override val themePlainBlackName = "Plain Black"
  override val themePlainBlackDesc = "Pure #000000 black background with crisp #FFFFFF icons & text and zero extra effects"
  override val appGlassOpacityTitle = "Transparency"
  override val appGlassOpacitySubtitle = "Adjust frosted glass card & bar opacity across the app"
  override val appearanceTitle = "Brightness Mode"
  override val darkModeLabel = "Dark Mode"
  override val lightModeLabel = "Light Mode"
  override val languageTitle = "Language"
  override val englishLabel = "English"
  override val arabicLabel = "العربية"
  override val overlaySectionTitle = "Floating Overlay Mini-Player"
  override val overlayGrantedTitle = "Display Over Other Apps: Enabled"
  override val overlayNeededTitle = "Display Over Other Apps: Permission Needed"
  override val overlayDescription = "When opening audio files from an external file manager, M player 97 launches a floating mini-player over other apps instead of interrupting your workflow."
  override val enableOverlayPermission = "Grant Overlay Permission"
  override val launchFloatingPlayer = "Launch Floating Mini-Player"
  override val weeklySummaryTitle = "Listening Insights"
  override val weeklySummarySubtitle = "Playback counts & listening duration"
  override val top7Title = "Listening Analytics & Top Played"
  override val playsLabel = "plays"
  override val totalPlaysLabel = "Plays"
  override val listeningMinutesLabel = "Listening"
  override val uniqueSongsLabel = "Unique Tracks"
  override val playTop7Mix = "Play Top Mix"
  override val emptyWeeklyTitle = "No listening activity in this period"
  override val emptyWeeklySubtitle = "Play your local music to record listening duration and top played tracks."
  override val expandFullApp = "Open Full App"
  override val closeOverlay = "Close Overlay"
  override val overlaySetupPrompt = "Enable 'Display over other apps' so future audio files open in a floating mini-player."
  override val openSystemOverlaySettings = "Enable Overlay"
  override val dismiss = "Dismiss"
  override val unknownArtist = "Unknown Artist"
  override val unknownAlbum = "Local Audio"
  override val visualizerAlbumArt = "Cover"
  override val visualizerEnergyOrb = "3D Sphere"
  override val visualizerTouchHint = "Touch or drag to rotate & pulse 3D sphere"
  override val audio8DPanelTitle = "8D Spatial Control Panel"
  override val audio8DToggleLabel = "Enable 8D Spatial Effect"
  override val audio8DRadarTitle = "Spatial Soundfield Radar"
  override val audio8DSpeedTitle = "Movement Speed"
  override val audio8DSpeedSlow = "Slow"
  override val audio8DSpeedNormal = "Normal"
  override val audio8DSpeedFast = "Fast"
  override val audio8DIntensityTitle = "Effect Intensity"
  override val audio8DDefaultSettingsTitle = "Default 8D Preferences"
  override val audio8DOpenPanel = "Open 8D Radar & Panel"
  override val audio8DTapToTune = "Tap to open Radar, Speed & Intensity panel"
  override val radarFront = "F"
  override val radarBack = "B"
  override val radarLeft = "L"
  override val radarRight = "R"
  override val visualizerSpectrum = "Spectrum"
  override val visualizerCircular = "Bass Pulse"
  override val sleepTimerTitle = "Sleep Timer"
  override val sleepTimerOff = "Off"
  override val sleepTimer5Min = "5 min"
  override val sleepTimer10Min = "10 min"
  override val sleepTimer15Min = "15 min"
  override val sleepTimer30Min = "30 min"
  override val sleepTimer45Min = "45 min"
  override val sleepTimer60Min = "1 hour"
  override val sleepTimerCustom = "Custom Time"
  override val sleepTimerHoursLabel = "Hours"
  override val sleepTimerMinutesLabel = "Minutes"
  override val sleepTimerStartCustom = "Start Timer"
  override val sleepTimerEndOfTrack = "End of current track"
  override val sleepTimerCancel = "Turn Off Timer"
  override val sleepTimerFadingOut = "Fading out audio..."
  override val sleepTimerRemainingFormat: (String) -> String = { time -> "Pausing in $time" }
  override val equalizerTitle = "Audio Equalizer"
  override val equalizerEnableLabel = "Enable Graphic Equalizer"
  override val equalizerPresetsTitle = "Sound Presets"
  override val equalizerPresetFlat = "Flat"
  override val equalizerPresetBassBoost = "Bass Boost"
  override val equalizerPresetRock = "Rock"
  override val equalizerPresetPop = "Pop"
  override val equalizerPresetClassical = "Classical"
  override val equalizerPresetVocal = "Vocal / Podcast"
  override val equalizerPresetCustom = "Custom"
  override val equalizerBassLabel = "Bass"
  override val equalizerMidLabel = "Midrange"
  override val equalizerTrebleLabel = "Treble"
  override val equalizerReset = "Reset"
  override val timeframeDaily = "Today"
  override val timeframeWeekly = "Weekly"
  override val timeframeMonthly = "Monthly"
  override val timeframeYearly = "Yearly"
  override val insightsTitle = "Listening Insights"
  override val insightsSubtitle = "Playback counts, listening duration & unique tracks"
  override val usageSyncTitle = "Local Stats File (mplayer97_usage_stats.json)"
  override val usageSyncActive = "Auto-syncing playback logs in background to mplayer97_usage_stats.json"
  override val usageSyncInactive = "Select or create mplayer97_usage_stats.json to enable automatic background sync"
  override val usageSyncButton = "Select Sync File"
  override val usageSyncChangeFileButton = "Change Sync File"
  override val usageImportButton = "Import Stats"
  override val usageImportSuccessFormat: (Int) -> String = { count -> "Imported $count listening entries" }
  override val usageImportFailed = "Could not import selected JSON stats file"
  override val usageSyncLastSyncedPrefix = "Last synced:"
  override val listeningUnitSecondsLabel = "Seconds"
  override val listeningUnitMinutesLabel = "Minutes"
  override val listeningUnitSecondsShort = "s"
  override val listeningUnitMinutesShort = "m"
  override val widgetQuickSettingsTitle = "Widget Quick Settings"
  override val widgetColorSectionTitle = "Widget Theme Color"
  override val widgetColorMatchApp = "Match App"
  override val widgetColorNeutral = "Neutral / Colorless"
  override val widgetGlassSectionTitle = "Glass Opacity & Blur"
  override val widgetGlassFrostedBlur = "Frosted Blur"
  override val widgetGlassSemiFrosted = "Semi-Frosted"
  override val widgetGlassOpaque = "Opaque"
  override val widgetGlassTransparent = "Transparent"
  override val widgetTransparencySliderLabel = "Glass Density & Transparency"
  override val playlistBackupSettingsTitle = "Playlist Backup & Sync"
  override val playlistBackupSubtitle = "Add-only archive (deletions inside the app stay preserved in mplayer97_playlists.json)"
  override val playlistBackupActive = "Add-only auto-sync active (mplayer97_playlists.json)"
  override val playlistBackupInactive = "Select or create mplayer97_playlists.json for add-only auto backup"
  override val playlistBackupSelectFile = "Create Backup JSON"
  override val playlistBackupChangeFile = "Change Backup File"
  override val playlistImportButton = "Import Playlists"
  override val playlistImportSuccessFormat: (Int, Int) -> String = { playlists, tracks -> "Restored $playlists playlists ($tracks items)" }
  override val playlistImportFailed = "Could not import selected playlists JSON file"
  override val favoritesPlaylistName = "Liked"
  override val filterAllMedia = "All"
  override val filterAudioOnly = "Audio"
  override val filterVideoOnly = "Video"
  override val unknownVideoLabel = "Local Video"
  override val videoBadgeLabel = "VIDEO"
  override val audioBadgeLabel = "AUDIO"
  override val likeToggleAdd = "Add to Liked"
  override val likeToggleRemove = "Remove from Liked"
  override val videoOptionsTitle = "Options"
  override val playAudioTrack = "Play"
  override val playVideoNormal = "Play Normally (Audio + Video)"
  override val playVideoAudioOnly = "Play Audio Only (Background)"
  override val convertFormatOption = "Convert to MP3"
  override val deletedMediaFormat: (String) -> String = { name -> "Deleted \"$name\"" }
  override val deleteMediaFailed = "Could not delete file"
  override val videoDisplayModePill = "Normal Video"
  override val audioOnlyModePill = "Audio Only"
  override val defaultVideoModeTitle = "Default Video Playback"
  override val defaultVideoModeSubtitle = "Choose whether videos open with picture or audio-only in background"
  override val videoModeNormalLabel = "Normal Video"
  override val videoModeAudioOnlyLabel = "Audio Only"
  override val convertDialogTitle = "Convert Media Format"
  override val convertChoosePrompt = "Choose export format (saved directly into folder \"M player 97\"):"
  override val convertFullVideo = "Convert Full Video (.mp4)"
  override val convertFullVideoDesc = "Exports complete video + audio stream to M player 97"
  override val convertMp3AudioOnly = "Create MP3 Audio Copy Only (.mp3)"
  override val convertMp3AudioOnlyDesc = "Extracts and encodes audio into standalone MP3"
  override val converterFolderTitle = "Export Folder (M player 97)"
  override val converterFolderAuthorized = "Folder \"M player 97\" authorized for direct export"
  override val converterFolderNeeded = "Grant folder access to save converted files in \"M player 97\""
  override val converterFolderAuthorizeBtn = "Choose Folder"
  override val convertingProgressFormat: (Int) -> String = { pct -> "Converting media... $pct%" }
  override val conversionSuccessFormat: (String) -> String = { path -> "Saved to $path" }
  override val conversionFailedLabel = "Conversion failed"
  override val historyTitle = "History"
  override val historyEmptyTitle = "No playback history yet"
  override val historyEmptySubtitle = "Played songs and videos appear here with your saved resume timestamp."
  override val historyClearAll = "Clear All"
  override val historyPausedAtFormat: (String) -> String = { pos -> "Saved at $pos" }
  override val resumePromptTitle = "Resume Playback?"
  override val resumePromptMessageFormat: (String, String) -> String = { title, pos -> "\"$title\" has a saved position at $pos." }
  override val resumeFromPositionBtnFormat: (String) -> String = { pos -> "Resume from $pos" }
  override val restartFromBeginningBtn = "Restart from beginning"
  override val externalOpenTitle = "Open Media"
  override val externalOpenSubtitle = "Choose how to play this file:"
  override val externalPlayNormal = "Normal Video"
  override val externalPlayAudioOnly = "Audio Only"
  override val videoRotateLandscape = "Rotate Landscape"
  override val videoRotatePortrait = "Exit Landscape"
  override val videoBrightness = "Brightness"
  override val videoBrightnessAuto = "Auto"
  override val videoScreenLock = "Lock Screen"
  override val videoScreenUnlock = "Unlock Screen"
  override val videoFullscreenExpand = "Fullscreen / Expand"
  override val videoFullscreenExit = "Exit Fullscreen"
  override val volumeBoost = "Volume Boost"
  override val volumeBoostWarningTitle = "Warning: Ultra-High Volume (> 397%)"
  override val volumeBoostWarningMessage = "Boosting volume past 397% exceeds standard speaker limits and may cause speaker damage or hearing fatigue. Proceed carefully."
  override val volumeBoostIgnore = "Ignore"
  override val volumeBoostReset = "Reset 100%"
  override val volumeBoostStandardLabel = "Standard Media"
  override val volumeBoostUltraLabel = "Ultra Boost + Limiter"
  override val routeSpeaker = "Phone Speaker"
  override val routeWiredHeadphones = "Wired Headphones"
  override val routeBluetooth = "Bluetooth Audio"
  override val scrollToTop = "Scroll to Top"
  override val clearSearch = "Clear search"
  override val videoSeekControlTitle = "Video Seek Step"
  override val videoSeekControlSubtitle = "Double-tap left or right in fullscreen video to skip"
  override val videoSeekPresetFormat: (Int) -> String = { sec ->
    when {
      sec >= 3600 && sec % 3600 == 0 -> "${sec / 3600}h"
      sec >= 60 && sec % 60 == 0 -> "${sec / 60}m"
      else -> "${sec}s"
    }
  }
  override val videoSeekCustomLabel = "Custom"
  override val videoSeekCustomDialogTitle = "Custom Seek Step"
  override val videoSeekValueHint = "Amount"
  override val videoSeekUnitSeconds = "Seconds"
  override val videoSeekUnitMinutes = "Minutes"
  override val videoSeekUnitHours = "Hours"
  override val confirmLabel = "Confirm"
  override val equalizerHubBadge = "EQ • 8D • Amplifier"
  override val volumeBoostToggleLabel = "Enable Volume Boost"
  override val volumeBoostHardwareSynced = "Synced with phone volume buttons"
  override val amplifierTitle = "Audio Amplifier"
  override val amplifierSubtitle = "Dedicated preamp gain stage (+0 to +97 dB)"
  override val amplifierToggleLabel = "Enable Amplifier"
  override val amplifierGainLabel = "Amplification Level"
  override val eq8DRadarToggleShow = "Show 8D Radar"
  override val eq8DRadarToggleHide = "Hide 8D Radar"
  override val locateCurrentTrack = "Jump to current track"
}

@Immutable
object ArabicStrings : AppStrings {
  override val appName = "M player 97"
  override val offlineBadge = "بدون إنترنت 100%"
  override val navLibrary = "المكتبة"
  override val navPlaylists = "القوائم"
  override val navPlayer = "المشغل"
  override val navHistory = "السجل"
  override val navMore = "المزيد"
  override val searchPlaceholder = "ابحث عن أغنية أو فيديو أو فنان..."
  override val tracksCountFormat: (Int) -> String = { count -> "$count عنصر" }
  override val allFolders = "كل المجلدات"
  override val shuffleAll = "عشوائي"
  override val playAll = "تشغيل الكل"
  override val scanLibrary = "تحديث"
  override val emptyLibraryTitle = "لم يتم العثور على وسائط محلية"
  override val emptyLibrarySubtitle = "أضف ملفات صوت MP3/WAV/FLAC أو فيديو MP4 إلى جهازك ثم اضغط تحديث."
  override val permissionTitle = "الوصول إلى مكتبة الصوت والفيديو"
  override val permissionBody = "يعمل M player 97 بدون إنترنت 100%. اسمح بالوصول إلى الصوتيات والفيديوهات لفحص مكتبتك المحلية."
  override val grantPermission = "السماح بالوصول للوسائط"
  override val openSettings = "فتح إعدادات التطبيق"
  override val sortTitle = "ترتيب العرض"
  override val sortAlphaAsc = "أبجدي (أ - ي / A-Z)"
  override val sortAlphaDesc = "أبجدي تنازلي (ي - أ / Z-A)"
  override val sortDateModifiedDesc = "تاريخ التحميل / التعديل (الأحدث)"
  override val sortDateModifiedAsc = "تاريخ التحميل / التعديل (الأقدم)"
  override val sortArtistAsc = "حسب الفنان"
  override val sortDurationDesc = "الأطول مدة"
  override val sortManual = "ترتيب يدوي (سحب وإفلات)"
  override val sortDateAddedDesc = "تاريخ الإضافة للقائمة (الأحدث)"
  override val sortDateAddedAsc = "تاريخ الإضافة للقائمة (الأقدم)"
  override val playlistsTitle = "قوائم التشغيل"
  override val createPlaylist = "قائمة جديدة"
  override val playlistNameHint = "اسم القائمة"
  override val create = "إنشاء"
  override val cancel = "إلغاء"
  override val delete = "حذف"
  override val rename = "إعادة تسمية"
  override val save = "حفظ"
  override val changeCover = "صورة غلاف مخصصة"
  override val removeCover = "إزالة الغلاف"
  override val addTracks = "إضافة مقاطع"
  override val removeFromPlaylist = "إزالة من القائمة"
  override val emptyPlaylistsTitle = "لا توجد قوائم تشغيل"
  override val emptyPlaylistsSubtitle = "أنشئ قوائم تشغيل بصور غلاف من معرض الصور مع ترتيب يدوي بالسحب والإفلات."
  override val emptyPlaylistTracksTitle = "القائمة فارغة"
  override val emptyPlaylistTracksSubtitle = "اضغط إضافة مقاطع لاختيار أغاني من مكتبتك المحلية."
  override val manualDragTip = "اضغط مطولاً واسحب المقاطع (أو استخدم الأسهم) للترتيب اليدوي"
  override val moveUp = "تحريك للأعلى"
  override val moveDown = "تحريك للأسفل"
  override val addToPlaylist = "إضافة إلى قائمة"
  override val addedToPlaylist = "تمت الإضافة للقائمة"
  override val nowPlaying = "قيد التشغيل الآن"
  override val noTrackPlaying = "لا يوجد مقطع قيد التشغيل"
  override val pickTrackHint = "اختر أي مقطع من المكتبة أو افتح ملفاً صوتياً من مدير الملفات."
  override val audio8DTitle = "صوت 8D"
  override val audio8DOn = "8D مفعل"
  override val audio8DOff = "8D متوقف"
  override val audio8DSubtitle = "توزيع صوتي محيطي 360° مع صدى مكاني"
  override val queueTitle = "قائمة الانتظار"
  override val moreTitle = "المزيد والتقارير"
  override val settingsSection = "المظهر واللغة"
  override val themesTitle = "ثيم التطبيق"
  override val themeRoyalPurpleName = "بنفسجي ملكي"
  override val themeRoyalPurpleDesc = "زجاج مصنفر بنفسجي ليلي شديد العمق (#0B0316) مع درجات داكنة نقية"
  override val themeDeepBurgundyName = "نبيتي غامق جداً"
  override val themeDeepBurgundyDesc = "زجاج نبيتي ملكي مصنفر (#140307 / #5A0D17) مع أيقونات فضية نقية دون أي أصفر أو وردي"
  override val themeMidnightBlueName = "أزرق ليلي داكن"
  override val themeMidnightBlueDesc = "زجاج أزرق ليلي سحيق مصنفر (#030914 / #102C6B) شديد التعتيم"
  override val themePitchBlackGoldName = "أسود داكن وذهبي معدني"
  override val themePitchBlackGoldDesc = "زجاج أوبسيديان أسود مدخن (#000000 / #0A0A0A) مع حواف ذهبية معدنية (#D4AF37)"
  override val themeLuxuryBrownName = "البني الفاخر"
  override val themeLuxuryBrownDesc = "زجاج مصنفر شديد التعتيم من الإسبرسو (#140A05) إلى الشوكولاتة الداكنة (#26130A) مع برونزي دافئ"
  override val themeCosmicStarfieldName = "سماء النجوم الكونية"
  override val themeCosmicStarfieldDesc = "سماء سوداء (#000000) مرصعة بنجوم متحركة في دورة مدتها 7 ثوانٍ"
  override val themePlainBlackName = "أسود سادة"
  override val themePlainBlackDesc = "أسود نقي (#000000) مع أيقونات ونصوص بيضاء (#FFFFFF) دون أي تأثيرات إضافية"
  override val appGlassOpacityTitle = "الشفافية"
  override val appGlassOpacitySubtitle = "تحكم في درجة شفافية الواجهات والبطاقات الزجاجية في التطبيق"
  override val appearanceTitle = "وضع الإضاءة"
  override val darkModeLabel = "الوضع الداكن"
  override val lightModeLabel = "الوضع الفاتح"
  override val languageTitle = "اللغة"
  override val englishLabel = "English"
  override val arabicLabel = "العربية"
  override val overlaySectionTitle = "المشغل العائم فوق التطبيقات"
  override val overlayGrantedTitle = "الظهور فوق التطبيقات الأخرى: مفعل"
  override val overlayNeededTitle = "الظهور فوق التطبيقات الأخرى: يحتاج إذن"
  override val overlayDescription = "عند فتح ملف صوتي من مدير الملفات، يظهر مشغل مصغر عائم فوق التطبيقات فوراً دون إجبارك على فتح التطبيق بالكامل."
  override val enableOverlayPermission = "منح إذن الظهور فوق التطبيقات"
  override val launchFloatingPlayer = "تشغيل المشغل العائم الآن"
  override val weeklySummaryTitle = "إحصائيات الاستماع"
  override val weeklySummarySubtitle = "إحصائيات وقت التشغيل وعدد المرات"
  override val top7Title = "تحليلات الاستماع والأكثر تشغيلاً"
  override val playsLabel = "مرات"
  override val totalPlaysLabel = "التشغيلات"
  override val listeningMinutesLabel = "وقت الاستماع"
  override val uniqueSongsLabel = "مقاطع مختلفة"
  override val playTop7Mix = "تشغيل الأكثر استماعاً"
  override val emptyWeeklyTitle = "لا يوجد نشاط استماع في هذه الفترة"
  override val emptyWeeklySubtitle = "شغّل موسيقاك المحلية لتسجيل وقت الاستماع والأغاني الأكثر تشغيلاً."
  override val expandFullApp = "فتح التطبيق الكامل"
  override val closeOverlay = "إغلاق العائم"
  override val overlaySetupPrompt = "فعّل إذن الظهور فوق التطبيقات لفتح الملفات الصوتية في مشغل عائم مباشرة."
  override val openSystemOverlaySettings = "تفعيل العائم"
  override val dismiss = "إخفاء"
  override val unknownArtist = "فنان غير معروف"
  override val unknownAlbum = "ملف محلي"
  override val visualizerAlbumArt = "صورة الأغنية"
  override val visualizerEnergyOrb = "مجسم 3D"
  override val visualizerTouchHint = "المس أو اسحب لتدوير المجسم الكروي 3D"
  override val audio8DPanelTitle = "لوحة تحكم صوت 8D المحيطي"
  override val audio8DToggleLabel = "تفعيل تأثير الصوت المحيطي 8D"
  override val audio8DRadarTitle = "رادار المجال الصوتي (L / R / F / B)"
  override val audio8DSpeedTitle = "سرعة الحركة الدورانية"
  override val audio8DSpeedSlow = "بطيء"
  override val audio8DSpeedNormal = "عادي"
  override val audio8DSpeedFast = "سريع"
  override val audio8DIntensityTitle = "قوة وعمق التأثير"
  override val audio8DDefaultSettingsTitle = "تفضيلات صوت 8D الافتراضية"
  override val audio8DOpenPanel = "فتح رادار ولوحة 8D"
  override val audio8DTapToTune = "اضغط لفتح الرادار والسرعة ودرجة التأثير"
  override val radarFront = "F"
  override val radarBack = "B"
  override val radarLeft = "L"
  override val radarRight = "R"
  override val visualizerSpectrum = "أعمدة الطيف"
  override val visualizerCircular = "نبض دائري"
  override val sleepTimerTitle = "مؤقت النوم"
  override val sleepTimerOff = "متوقف"
  override val sleepTimer5Min = "5 دقائق"
  override val sleepTimer10Min = "10 دقائق"
  override val sleepTimer15Min = "15 دقيقة"
  override val sleepTimer30Min = "30 دقيقة"
  override val sleepTimer45Min = "45 دقيقة"
  override val sleepTimer60Min = "ساعة واحدة"
  override val sleepTimerCustom = "ضبط يدوي"
  override val sleepTimerHoursLabel = "ساعات"
  override val sleepTimerMinutesLabel = "دقائق"
  override val sleepTimerStartCustom = "تفعيل المؤقت"
  override val sleepTimerEndOfTrack = "نهاية المقطع الحالي"
  override val sleepTimerCancel = "إلغاء المؤقت"
  override val sleepTimerFadingOut = "جاري خفض الصوت تدريجياً..."
  override val sleepTimerRemainingFormat: (String) -> String = { time -> "الإيقاف خلال $time" }
  override val equalizerTitle = "موزع الصوت (Equalizer)"
  override val equalizerEnableLabel = "تفعيل موزع الصوت"
  override val equalizerPresetsTitle = "الأوضاع الجاهزة"
  override val equalizerPresetFlat = "قياسي (Flat)"
  override val equalizerPresetBassBoost = "تضخيم البيس"
  override val equalizerPresetRock = "روك"
  override val equalizerPresetPop = "بوب"
  override val equalizerPresetClassical = "كلاسيك"
  override val equalizerPresetVocal = "بودكاست / صوت"
  override val equalizerPresetCustom = "مخصص"
  override val equalizerBassLabel = "البيس (Bass)"
  override val equalizerMidLabel = "الوسط (Mid)"
  override val equalizerTrebleLabel = "الحدة (Treble)"
  override val equalizerReset = "إعادة ضبط"
  override val timeframeDaily = "يومي"
  override val timeframeWeekly = "أسبوعي"
  override val timeframeMonthly = "شهري"
  override val timeframeYearly = "سنوي"
  override val insightsTitle = "إحصائيات الاستماع"
  override val insightsSubtitle = "سجل التشغيلات ووقت الاستماع والمقاطع المختلفة"
  override val usageSyncTitle = "ملف السجل المحلي (mplayer97_usage_stats.json)"
  override val usageSyncActive = "مزامنة تلقائية مستمرة في الخلفية مع ملف mplayer97_usage_stats.json"
  override val usageSyncInactive = "اختر أو أنشئ ملف mplayer97_usage_stats.json لتفعيل المزامنة التلقائية في الخلفية"
  override val usageSyncButton = "اختيار ملف المزامنة"
  override val usageSyncChangeFileButton = "تغيير ملف المزامنة"
  override val usageImportButton = "استيراد السجل"
  override val usageImportSuccessFormat: (Int) -> String = { count -> "تم استيراد $count سجل استماع بنجاح" }
  override val usageImportFailed = "تعذر قراءة ملف الإحصائيات المحدد"
  override val usageSyncLastSyncedPrefix = "آخر مزامنة:"
  override val listeningUnitSecondsLabel = "ثواني"
  override val listeningUnitMinutesLabel = "دقائق"
  override val listeningUnitSecondsShort = " ث"
  override val listeningUnitMinutesShort = " د"
  override val widgetQuickSettingsTitle = "إعدادات المشغل العائم السريعة"
  override val widgetColorSectionTitle = "لون ثيم المشغل العائم"
  override val widgetColorMatchApp = "مطابق للتطبيق"
  override val widgetColorNeutral = "محايد / بلا لون"
  override val widgetGlassSectionTitle = "مستوى الزجاج والشفافية"
  override val widgetGlassFrostedBlur = "زجاج ضبابي"
  override val widgetGlassSemiFrosted = "شبه شفاف"
  override val widgetGlassOpaque = "معتم صلب"
  override val widgetGlassTransparent = "شفاف بالكامل"
  override val widgetTransparencySliderLabel = "درجة الزجاج والشفافية"
  override val playlistBackupSettingsTitle = "نسخ ومزامنة القوائم"
  override val playlistBackupSubtitle = "حفظ تراكمي للإضافات فقط (حذف قائمة داخل التطبيق لا يحذفها من ملف mplayer97_playlists.json)"
  override val playlistBackupActive = "المزامنة التلقائية للإضافات مفعلة (mplayer97_playlists.json)"
  override val playlistBackupInactive = "اختر أو أنشئ ملف mplayer97_playlists.json للنسخ التلقائي عند الإضافة"
  override val playlistBackupSelectFile = "إنشاء ملف النسخ (JSON)"
  override val playlistBackupChangeFile = "تغيير ملف النسخ"
  override val playlistImportButton = "استيراد القوائم"
  override val playlistImportSuccessFormat: (Int, Int) -> String = { playlists, tracks -> "تم استيراد $playlists قائمة ($tracks عنصر)" }
  override val playlistImportFailed = "تعذر قراءة ملف قوائم التشغيل المحدد"
  override val favoritesPlaylistName = "المفضلة"
  override val filterAllMedia = "الكل"
  override val filterAudioOnly = "صوت"
  override val filterVideoOnly = "فيديو"
  override val unknownVideoLabel = "فيديو محلي"
  override val videoBadgeLabel = "فيديو"
  override val audioBadgeLabel = "صوت"
  override val likeToggleAdd = "إضافة إلى المفضلة"
  override val likeToggleRemove = "إزالة من المفضلة"
  override val videoOptionsTitle = "خيارات"
  override val playAudioTrack = "تشغيل"
  override val playVideoNormal = "فيديو عادي"
  override val playVideoAudioOnly = "صوت فقط"
  override val convertFormatOption = "تحويل إلى MP3"
  override val deletedMediaFormat: (String) -> String = { name -> "تم حذف \"$name\"" }
  override val deleteMediaFailed = "تعذر حذف الملف"
  override val videoDisplayModePill = "فيديو عادي"
  override val audioOnlyModePill = "صوت فقط"
  override val defaultVideoModeTitle = "وضع تشغيل الفيديو الافتراضي"
  override val defaultVideoModeSubtitle = "اختر تشغيل الفيديوهات بالصورة أم كصوت فقط في الخلفية"
  override val videoModeNormalLabel = "فيديو عادي"
  override val videoModeAudioOnlyLabel = "صوت فقط"
  override val convertDialogTitle = "تحويل صيغة الملف"
  override val convertChoosePrompt = "اختر صيغة التصدير (يتم الحفظ تلقائياً في مجلد M player 97):"
  override val convertFullVideo = "تحويل الفيديو بالكامل (MP4)"
  override val convertFullVideoDesc = "حفظ الفيديو مع الصوت بجودة كاملة في مجلد M player 97"
  override val convertMp3AudioOnly = "إنشاء نسخة صوتية فقط (MP3)"
  override val convertMp3AudioOnlyDesc = "استخراج الصوت وحفظه كملف MP3 مستقل"
  override val converterFolderTitle = "مجلد حفظ المحولات (M player 97)"
  override val converterFolderAuthorized = "تم تفعيل مجلد M player 97 للحفظ المباشر"
  override val converterFolderNeeded = "حدد المجلد للسماح بإنشاء مجلد M player 97 وتصدير الملفات"
  override val converterFolderAuthorizeBtn = "اختيار مجلد الحفظ"
  override val convertingProgressFormat: (Int) -> String = { pct -> "جاري التحويل... $pct%" }
  override val conversionSuccessFormat: (String) -> String = { path -> "تم الحفظ في $path" }
  override val conversionFailedLabel = "فشل التحويل"
  override val historyTitle = "السجل"
  override val historyEmptyTitle = "لا يوجد سجل تشغيل بعد"
  override val historyEmptySubtitle = "تظهر هنا الأغاني والفيديوهات المشغّلة مؤخراً مع نقطة التوقف لمتابعتها بسهولة."
  override val historyClearAll = "مسح السجل"
  override val historyPausedAtFormat: (String) -> String = { pos -> "توقف عند $pos" }
  override val resumePromptTitle = "متابعة التشغيل؟"
  override val resumePromptMessageFormat: (String, String) -> String = { title, pos -> "تم إيقاف \"$title\" مسبقاً عند الدقيقة $pos." }
  override val resumeFromPositionBtnFormat: (String) -> String = { pos -> "متابعة من $pos" }
  override val restartFromBeginningBtn = "إعادة من البداية"
  override val externalOpenTitle = "فتح ملف وسائط"
  override val externalOpenSubtitle = "اختر طريقة تشغيل هذا الملف:"
  override val externalPlayNormal = "فيديو عادي"
  override val externalPlayAudioOnly = "صوت فقط"
  override val videoRotateLandscape = "تدوير بالعرض"
  override val videoRotatePortrait = "إلغاء التدوير"
  override val videoBrightness = "السطوع"
  override val videoBrightnessAuto = "تلقائي"
  override val videoScreenLock = "قفل الشاشة"
  override val videoScreenUnlock = "إلغاء القفل"
  override val videoFullscreenExpand = "تكبير / ملء الشاشة"
  override val videoFullscreenExit = "تصغير الشاشة"
  override val volumeBoost = "رفع الصوت المعزز"
  override val volumeBoostWarningTitle = "تحذير: مستوى صوت مرتفع جداً (+397%)"
  override val volumeBoostWarningMessage = "تجاوز 397% يتخطى حدود الأمان القياسية للسماعات وقد يعرض سماعات الجهاز للتلف أو الإجهاد السمعي."
  override val volumeBoostIgnore = "تجاهل"
  override val volumeBoostReset = "إعادة 100%"
  override val volumeBoostStandardLabel = "صوت قياسي"
  override val volumeBoostUltraLabel = "تضخيم نقي + محدد ديناميكي"
  override val routeSpeaker = "مكبر صوت الجهاز"
  override val routeWiredHeadphones = "سماعة سلكية"
  override val routeBluetooth = "سماعة بلوتوث"
  override val scrollToTop = "العودة للأعلى"
  override val clearSearch = "مسح البحث"
  override val videoSeekControlTitle = "التحكم في مقدار التمرير في الفيديو"
  override val videoSeekControlSubtitle = "انقر مرتين يميناً أو يساراً في الفيديو بملء الشاشة للتقديم أو التأخير"
  override val videoSeekPresetFormat: (Int) -> String = { sec ->
    when {
      sec >= 3600 && sec % 3600 == 0 -> "${sec / 3600} س"
      sec >= 60 && sec % 60 == 0 -> "${sec / 60} د"
      else -> "$sec ث"
    }
  }
  override val videoSeekCustomLabel = "مخصص"
  override val videoSeekCustomDialogTitle = "تحديد مقدار التمرير المخصص"
  override val videoSeekValueHint = "أدخل القيمة"
  override val videoSeekUnitSeconds = "ثواني"
  override val videoSeekUnitMinutes = "دقائق"
  override val videoSeekUnitHours = "ساعات"
  override val confirmLabel = "تأكيد"
  override val equalizerHubBadge = "الموزع • 8D • المضخم"
  override val volumeBoostToggleLabel = "تفعيل تعزيز الصوت"
  override val volumeBoostHardwareSynced = "متزامن مع أزرار صوت الهاتف"
  override val amplifierTitle = "مضخم الصوت"
  override val amplifierSubtitle = "مرحلة تضخيم مخصصة (+0 إلى +97 dB)"
  override val amplifierToggleLabel = "تفعيل المضخم"
  override val amplifierGainLabel = "قوة التضخيم"
  override val eq8DRadarToggleShow = "عرض رادار 8D"
  override val eq8DRadarToggleHide = "إخفاء رادار 8D"
  override val locateCurrentTrack = "الانتقال للمقطع الحالي"
}

fun stringsForLanguage(language: AppLanguage): AppStrings =
  when (language) {
    AppLanguage.EN -> EnglishStrings
    AppLanguage.AR -> ArabicStrings
  }

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { EnglishStrings }
