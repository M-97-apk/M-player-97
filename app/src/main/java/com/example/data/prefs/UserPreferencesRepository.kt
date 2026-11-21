package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.AppThemeStyle
import com.example.model.Audio8DSpeed
import com.example.model.EqualizerPreset
import com.example.model.FloatingWidgetColorMode
import com.example.model.FloatingWidgetGlassMode
import com.example.model.LibraryMediaFilter
import com.example.model.LibrarySortOption
import com.example.model.ListeningTimeUnit
import com.example.model.RepeatMode
import com.example.model.VideoPlaybackMode
import com.example.model.VisualizerMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mplayer97_prefs")

data class UserPreferences(
  val themeStyle: AppThemeStyle = AppThemeStyle.DEEP_BURGUNDY,
  val isDarkMode: Boolean = true,
  val language: AppLanguage = AppLanguage.EN,
  val librarySort: LibrarySortOption = LibrarySortOption.DATE_MODIFIED_DESC,
  val libraryMediaFilter: LibraryMediaFilter = LibraryMediaFilter.ALL,
  val librarySelectedFolder: String? = null,
  val is8DEnabled: Boolean = false,
  val audio8DSpeed: Audio8DSpeed = Audio8DSpeed.NORMAL,
  val audio8DIntensity: Int = 97,
  val isEqEnabled: Boolean = true,
  val eqBandGainsDb: List<Float> = EqualizerPreset.FLAT.gainsDb,
  val repeatMode: RepeatMode = RepeatMode.OFF,
  val shuffleEnabled: Boolean = false,
  val visualizerMode: VisualizerMode = VisualizerMode.SPECTRUM_BARS,
  val usageFileSyncEnabled: Boolean = false,
  val usageFileExternalUri: String? = null,
  val usageLastSyncedIso: String = "",
  val floatingWidgetColorMode: FloatingWidgetColorMode = FloatingWidgetColorMode.MATCH_APP,
  val floatingWidgetGlassMode: FloatingWidgetGlassMode = FloatingWidgetGlassMode.FROSTED_BLUR,
  val floatingWidgetCustomOpacity: Float? = null,
  val listeningTimeUnit: ListeningTimeUnit = ListeningTimeUnit.MINUTES,
  val playlistBackupSyncEnabled: Boolean = false,
  val playlistBackupExternalUri: String? = null,
  val playlistLastSyncedIso: String = "",
  val defaultVideoPlaybackMode: VideoPlaybackMode = VideoPlaybackMode.VIDEO_AND_AUDIO,
  val converterOutputTreeUri: String? = null,
  val appGlassOpacity: Float = 0.55f,
  val videoSeekStepSeconds: Int = 10,
  val volumeBoostPercent: Int = 100,
  val amplifierEnabled: Boolean = false,
  val amplifierLevelPercent: Int = 50,
)

class UserPreferencesRepository(private val context: Context) {
  private object Keys {
    val THEME_STYLE = stringPreferencesKey("theme_style")
    val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    val LANGUAGE = stringPreferencesKey("language")
    val LIBRARY_SORT = stringPreferencesKey("library_sort")
    val LIBRARY_MEDIA_FILTER = stringPreferencesKey("library_media_filter")
    val LIBRARY_SELECTED_FOLDER = stringPreferencesKey("library_selected_folder")
    val IS_8D_ENABLED = booleanPreferencesKey("is_8d_enabled")
    val AUDIO_8D_SPEED = stringPreferencesKey("audio_8d_speed")
    val AUDIO_8D_INTENSITY = intPreferencesKey("audio_8d_intensity")
    val IS_EQ_ENABLED = booleanPreferencesKey("is_eq_enabled")
    val EQ_BAND_GAINS = stringPreferencesKey("eq_band_gains")
    val REPEAT_MODE = stringPreferencesKey("repeat_mode")
    val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
    val VISUALIZER_MODE = stringPreferencesKey("visualizer_mode")
    val USAGE_FILE_SYNC_ENABLED = booleanPreferencesKey("usage_file_sync_enabled")
    val USAGE_FILE_EXTERNAL_URI = stringPreferencesKey("usage_file_external_uri")
    val USAGE_LAST_SYNCED_ISO = stringPreferencesKey("usage_last_synced_iso")
    val FLOATING_WIDGET_COLOR_MODE = stringPreferencesKey("floating_widget_color_mode")
    val FLOATING_WIDGET_GLASS_MODE = stringPreferencesKey("floating_widget_glass_mode")
    val FLOATING_WIDGET_CUSTOM_OPACITY = androidx.datastore.preferences.core.floatPreferencesKey("floating_widget_custom_opacity")
    val LISTENING_TIME_UNIT = stringPreferencesKey("listening_time_unit")
    val PLAYLIST_BACKUP_SYNC_ENABLED = booleanPreferencesKey("playlist_backup_sync_enabled")
    val PLAYLIST_BACKUP_EXTERNAL_URI = stringPreferencesKey("playlist_backup_external_uri")
    val PLAYLIST_LAST_SYNCED_ISO = stringPreferencesKey("playlist_last_synced_iso")
    val DEFAULT_VIDEO_PLAYBACK_MODE = stringPreferencesKey("default_video_playback_mode")
    val CONVERTER_OUTPUT_TREE_URI = stringPreferencesKey("converter_output_tree_uri")
    val APP_GLASS_OPACITY = floatPreferencesKey("app_glass_opacity")
    val VIDEO_SEEK_STEP_SECONDS = intPreferencesKey("video_seek_step_seconds")
    val VOLUME_BOOST_PERCENT = intPreferencesKey("volume_boost_percent")
    val AMPLIFIER_ENABLED = booleanPreferencesKey("amplifier_enabled")
    val AMPLIFIER_LEVEL_PERCENT = intPreferencesKey("amplifier_level_percent")
  }

  val preferencesFlow: Flow<UserPreferences> =
    context.dataStore.data.map { prefs ->
      val parsedGains =
        prefs[Keys.EQ_BAND_GAINS]
          ?.split(",")
          ?.mapNotNull { it.trim().toFloatOrNull() }
          ?.takeIf { it.size == 5 }
          ?: EqualizerPreset.FLAT.gainsDb

      UserPreferences(
        themeStyle = parseThemeStyle(prefs[Keys.THEME_STYLE]),
        isDarkMode = prefs[Keys.IS_DARK_MODE] ?: true,
        language =
          prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
            ?: AppLanguage.EN,
        librarySort =
          prefs[Keys.LIBRARY_SORT]?.let {
            runCatching { LibrarySortOption.valueOf(it) }.getOrNull()
          } ?: LibrarySortOption.DATE_MODIFIED_DESC,
        libraryMediaFilter =
          prefs[Keys.LIBRARY_MEDIA_FILTER]?.let {
            runCatching { LibraryMediaFilter.valueOf(it) }.getOrNull()
          } ?: LibraryMediaFilter.ALL,
        librarySelectedFolder = prefs[Keys.LIBRARY_SELECTED_FOLDER]?.takeIf { it.isNotBlank() },
        is8DEnabled = prefs[Keys.IS_8D_ENABLED] ?: false,
        audio8DSpeed =
          prefs[Keys.AUDIO_8D_SPEED]?.let { runCatching { Audio8DSpeed.valueOf(it) }.getOrNull() }
            ?: Audio8DSpeed.NORMAL,
        audio8DIntensity = (prefs[Keys.AUDIO_8D_INTENSITY] ?: 97).coerceIn(0, 100),
        isEqEnabled = prefs[Keys.IS_EQ_ENABLED] ?: true,
        eqBandGainsDb = parsedGains,
        repeatMode =
          prefs[Keys.REPEAT_MODE]?.let { runCatching { RepeatMode.valueOf(it) }.getOrNull() }
            ?: RepeatMode.OFF,
        shuffleEnabled = prefs[Keys.SHUFFLE_ENABLED] ?: false,
        visualizerMode =
          prefs[Keys.VISUALIZER_MODE]?.let {
            runCatching { VisualizerMode.valueOf(it) }.getOrNull()
          } ?: VisualizerMode.SPECTRUM_BARS,
        usageFileSyncEnabled = prefs[Keys.USAGE_FILE_SYNC_ENABLED] ?: false,
        usageFileExternalUri = prefs[Keys.USAGE_FILE_EXTERNAL_URI],
        usageLastSyncedIso = prefs[Keys.USAGE_LAST_SYNCED_ISO] ?: "",
        floatingWidgetColorMode =
          prefs[Keys.FLOATING_WIDGET_COLOR_MODE]?.let {
            runCatching { FloatingWidgetColorMode.valueOf(it) }.getOrNull()
          } ?: FloatingWidgetColorMode.MATCH_APP,
        floatingWidgetGlassMode =
          prefs[Keys.FLOATING_WIDGET_GLASS_MODE]?.let {
            runCatching { FloatingWidgetGlassMode.valueOf(it) }.getOrNull()
          } ?: FloatingWidgetGlassMode.FROSTED_BLUR,
        floatingWidgetCustomOpacity =
          prefs[Keys.FLOATING_WIDGET_CUSTOM_OPACITY]?.coerceIn(0f, 1f),
        listeningTimeUnit =
          prefs[Keys.LISTENING_TIME_UNIT]?.let {
            runCatching { ListeningTimeUnit.valueOf(it) }.getOrNull()
          } ?: ListeningTimeUnit.MINUTES,
        playlistBackupSyncEnabled = prefs[Keys.PLAYLIST_BACKUP_SYNC_ENABLED] ?: false,
        playlistBackupExternalUri = prefs[Keys.PLAYLIST_BACKUP_EXTERNAL_URI],
        playlistLastSyncedIso = prefs[Keys.PLAYLIST_LAST_SYNCED_ISO] ?: "",
        defaultVideoPlaybackMode =
          prefs[Keys.DEFAULT_VIDEO_PLAYBACK_MODE]?.let {
            runCatching { VideoPlaybackMode.valueOf(it) }.getOrNull()
          } ?: VideoPlaybackMode.VIDEO_AND_AUDIO,
        converterOutputTreeUri = prefs[Keys.CONVERTER_OUTPUT_TREE_URI],
        appGlassOpacity = (prefs[Keys.APP_GLASS_OPACITY] ?: 0.55f).coerceIn(0.05f, 1f),
        videoSeekStepSeconds = (prefs[Keys.VIDEO_SEEK_STEP_SECONDS] ?: 10).coerceIn(1, 86_400),
        volumeBoostPercent = (prefs[Keys.VOLUME_BOOST_PERCENT] ?: 100).coerceIn(0, 797),
        amplifierEnabled = prefs[Keys.AMPLIFIER_ENABLED] ?: false,
        amplifierLevelPercent = (prefs[Keys.AMPLIFIER_LEVEL_PERCENT] ?: 50).coerceIn(0, 100),
      )
    }

  suspend fun setThemeStyle(style: AppThemeStyle) {
    context.dataStore.edit { it[Keys.THEME_STYLE] = style.name }
  }

  suspend fun setDarkMode(isDark: Boolean) {
    context.dataStore.edit { it[Keys.IS_DARK_MODE] = isDark }
  }

  suspend fun setLanguage(language: AppLanguage) {
    context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
  }

  suspend fun setLibrarySort(sortOption: LibrarySortOption) {
    context.dataStore.edit { it[Keys.LIBRARY_SORT] = sortOption.name }
  }

  suspend fun setLibraryMediaFilter(filter: LibraryMediaFilter) {
    context.dataStore.edit {
      it[Keys.LIBRARY_MEDIA_FILTER] = filter.name
      it.remove(Keys.LIBRARY_SELECTED_FOLDER)
    }
  }

  suspend fun setLibrarySelectedFolder(folder: String?) {
    context.dataStore.edit { prefs ->
      if (folder.isNullOrBlank()) {
        prefs.remove(Keys.LIBRARY_SELECTED_FOLDER)
      } else {
        prefs[Keys.LIBRARY_SELECTED_FOLDER] = folder
      }
    }
  }

  suspend fun set8DEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.IS_8D_ENABLED] = enabled }
  }

  suspend fun set8DSpeed(speed: Audio8DSpeed) {
    context.dataStore.edit { it[Keys.AUDIO_8D_SPEED] = speed.name }
  }

  suspend fun set8DIntensity(intensityPercent: Int) {
    context.dataStore.edit { it[Keys.AUDIO_8D_INTENSITY] = intensityPercent.coerceIn(0, 100) }
  }

  suspend fun setEqEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.IS_EQ_ENABLED] = enabled }
  }

  suspend fun setEqBandGains(gains: List<Float>) {
    val serialized = gains.take(5).joinToString(",") { String.format(java.util.Locale.US, "%.2f", it) }
    context.dataStore.edit { it[Keys.EQ_BAND_GAINS] = serialized }
  }

  suspend fun setRepeatMode(mode: RepeatMode) {
    context.dataStore.edit { it[Keys.REPEAT_MODE] = mode.name }
  }

  suspend fun setShuffleEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.SHUFFLE_ENABLED] = enabled }
  }

  suspend fun setVisualizerMode(mode: VisualizerMode) {
    context.dataStore.edit { it[Keys.VISUALIZER_MODE] = mode.name }
  }

  suspend fun setUsageFileSyncConfig(
    enabled: Boolean,
    externalUri: String? = null,
    clearExternalUri: Boolean = false,
  ) {
    context.dataStore.edit { prefs ->
      prefs[Keys.USAGE_FILE_SYNC_ENABLED] = enabled
      if (clearExternalUri) {
        prefs.remove(Keys.USAGE_FILE_EXTERNAL_URI)
      } else if (externalUri != null) {
        prefs[Keys.USAGE_FILE_EXTERNAL_URI] = externalUri
      }
    }
  }

  suspend fun setUsageLastSyncedIso(isoTimestamp: String) {
    context.dataStore.edit { it[Keys.USAGE_LAST_SYNCED_ISO] = isoTimestamp }
  }

  suspend fun setFloatingWidgetColorMode(mode: FloatingWidgetColorMode) {
    context.dataStore.edit { it[Keys.FLOATING_WIDGET_COLOR_MODE] = mode.name }
  }

  suspend fun setFloatingWidgetGlassMode(mode: FloatingWidgetGlassMode) {
    context.dataStore.edit {
      it[Keys.FLOATING_WIDGET_GLASS_MODE] = mode.name
      it.remove(Keys.FLOATING_WIDGET_CUSTOM_OPACITY)
    }
  }

  suspend fun setFloatingWidgetCustomOpacity(opacity: Float) {
    val clamped = opacity.coerceIn(0f, 1f)
    val snappedMode =
      when {
        clamped <= 0.03f -> FloatingWidgetGlassMode.FULLY_TRANSPARENT
        clamped < 0.44f -> FloatingWidgetGlassMode.SEMI_FROSTED
        clamped < 0.90f -> FloatingWidgetGlassMode.FROSTED_BLUR
        else -> FloatingWidgetGlassMode.OPAQUE
      }
    context.dataStore.edit {
      it[Keys.FLOATING_WIDGET_CUSTOM_OPACITY] = clamped
      it[Keys.FLOATING_WIDGET_GLASS_MODE] = snappedMode.name
    }
  }

  suspend fun setListeningTimeUnit(unit: ListeningTimeUnit) {
    context.dataStore.edit { it[Keys.LISTENING_TIME_UNIT] = unit.name }
  }

  suspend fun setPlaylistBackupConfig(
    enabled: Boolean,
    externalUri: String? = null,
  ) {
    context.dataStore.edit { prefs ->
      prefs[Keys.PLAYLIST_BACKUP_SYNC_ENABLED] = enabled
      if (externalUri != null) {
        prefs[Keys.PLAYLIST_BACKUP_EXTERNAL_URI] = externalUri
      }
    }
  }

  suspend fun setPlaylistLastSyncedIso(isoTimestamp: String) {
    context.dataStore.edit { it[Keys.PLAYLIST_LAST_SYNCED_ISO] = isoTimestamp }
  }

  suspend fun setDefaultVideoPlaybackMode(mode: VideoPlaybackMode) {
    context.dataStore.edit { it[Keys.DEFAULT_VIDEO_PLAYBACK_MODE] = mode.name }
  }

  suspend fun setConverterOutputTreeUri(uriString: String?) {
    context.dataStore.edit { prefs ->
      if (uriString.isNullOrBlank()) {
        prefs.remove(Keys.CONVERTER_OUTPUT_TREE_URI)
      } else {
        prefs[Keys.CONVERTER_OUTPUT_TREE_URI] = uriString
      }
    }
  }

  suspend fun setAppGlassOpacity(opacity: Float) {
    context.dataStore.edit { it[Keys.APP_GLASS_OPACITY] = opacity.coerceIn(0.05f, 1f) }
  }

  suspend fun setVideoSeekStepSeconds(seconds: Int) {
    context.dataStore.edit { it[Keys.VIDEO_SEEK_STEP_SECONDS] = seconds.coerceIn(1, 86_400) }
  }

  suspend fun setVolumeBoostPercent(percent: Int) {
    context.dataStore.edit { it[Keys.VOLUME_BOOST_PERCENT] = percent.coerceIn(0, 797) }
  }

  suspend fun setAmplifierEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.AMPLIFIER_ENABLED] = enabled }
  }

  suspend fun setAmplifierLevelPercent(percent: Int) {
    context.dataStore.edit { it[Keys.AMPLIFIER_LEVEL_PERCENT] = percent.coerceIn(0, 100) }
  }

  private fun parseThemeStyle(raw: String?): AppThemeStyle =
    when (raw) {
      "DEEP_BURGUNDY", "GLASSMORPHISM" -> AppThemeStyle.DEEP_BURGUNDY
      "ROYAL_PURPLE", "DEEP_PURPLE_GLASS" -> AppThemeStyle.ROYAL_PURPLE
      "MIDNIGHT_BLUE" -> AppThemeStyle.MIDNIGHT_BLUE
      "PITCH_BLACK_GOLD", "DARK_GOLD" -> AppThemeStyle.PITCH_BLACK_GOLD
      "LUXURY_DARK_BROWN" -> AppThemeStyle.LUXURY_DARK_BROWN
      "COSMIC_STARFIELD" -> AppThemeStyle.COSMIC_STARFIELD
      "PLAIN_BLACK" -> AppThemeStyle.PLAIN_BLACK
      else -> AppThemeStyle.DEEP_BURGUNDY
    }
}
