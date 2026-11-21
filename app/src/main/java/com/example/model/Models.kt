package com.example.model

import android.net.Uri

enum class AppThemeStyle {
  DEEP_BURGUNDY,
  ROYAL_PURPLE,
  MIDNIGHT_BLUE,
  PITCH_BLACK_GOLD,
  LUXURY_DARK_BROWN,
  COSMIC_STARFIELD,
  PLAIN_BLACK,
}

enum class FloatingWidgetColorMode {
  MATCH_APP,
  NEUTRAL,
  DEEP_BURGUNDY,
  ROYAL_PURPLE,
  MIDNIGHT_BLUE,
  PITCH_BLACK_GOLD,
  LUXURY_DARK_BROWN,
  COSMIC_STARFIELD,
  PLAIN_BLACK,
}

enum class FloatingWidgetGlassMode {
  FROSTED_BLUR,
  SEMI_FROSTED,
  OPAQUE,
  FULLY_TRANSPARENT,
}

enum class AppLanguage(val code: String) {
  EN("en"),
  AR("ar"),
}

enum class RepeatMode {
  OFF,
  ALL,
  ONE,
}

enum class VisualizerMode {
  ALBUM_ART,
  SPECTRUM_BARS,
  PULSING_CIRCLE,
  ENERGY_ORB,
}

enum class Audio8DSpeed(val frequencyHz: Double) {
  SLOW(0.055),
  NORMAL(0.18),
  FAST(0.45),
}

enum class SleepTimerMode {
  OFF,
  DURATION,
  END_OF_TRACK,
}

data class SleepTimerState(
  val mode: SleepTimerMode = SleepTimerMode.OFF,
  val remainingMs: Long = 0L,
  val totalDurationMs: Long = 0L,
  val isFadingOut: Boolean = false,
)

enum class EqualizerPreset(val gainsDb: List<Float>) {
  FLAT(listOf(0f, 0f, 0f, 0f, 0f)),
  BASS_BOOST(listOf(9f, 6f, 0f, 1.5f, 3f)),
  ROCK(listOf(5f, 2f, -1.5f, 4f, 6f)),
  POP(listOf(2f, 3f, 4.5f, 3f, 1.5f)),
  CLASSICAL(listOf(3.5f, 1f, 0f, 2.5f, 5f)),
  VOCAL_PODCAST(listOf(-4f, -1f, 5.5f, 6f, 2.5f)),
}

enum class LibrarySortOption {
  TITLE_ASC,
  TITLE_DESC,
  DATE_MODIFIED_DESC,
  DATE_MODIFIED_ASC,
  ARTIST_ASC,
  DURATION_DESC,
}

enum class PlaylistSortOption {
  MANUAL,
  ALPHABETICAL_ASC,
  ALPHABETICAL_DESC,
  DATE_ADDED_DESC,
  DATE_ADDED_ASC,
  DATE_MODIFIED_DESC,
}

enum class MediaType {
  AUDIO,
  VIDEO,
}

enum class LibraryMediaFilter {
  ALL,
  AUDIO,
  VIDEO,
}

enum class VideoPlaybackMode {
  VIDEO_AND_AUDIO,
  AUDIO_ONLY,
}

enum class VideoSeekUnit {
  SECONDS,
  MINUTES,
  HOURS,
}

enum class ConversionFormat {
  MP3_AUDIO,
  MP4_VIDEO,
}

data class AudioTrack(
  val id: Long,
  val title: String,
  val artist: String,
  val album: String,
  val albumId: Long,
  val durationMs: Long,
  val contentUri: Uri,
  val albumArtUri: Uri?,
  val dateAddedMs: Long,
  val dateModifiedMs: Long,
  val sizeBytes: Long,
  val mimeType: String,
  val folderName: String,
  val mediaType: MediaType = MediaType.AUDIO,
) {
  val isVideo: Boolean
    get() = mediaType == MediaType.VIDEO
}

data class PlaylistSummary(
  val id: Long,
  val name: String,
  val coverPath: String?,
  val sortOption: PlaylistSortOption,
  val createdAtMs: Long,
  val trackCount: Int,
  val totalDurationMs: Long,
  val isFavorites: Boolean = false,
)

data class PlaylistTrackEntry(
  val entryId: Long,
  val playlistId: Long,
  val track: AudioTrack,
  val addedToPlaylistAtMs: Long,
  val manualOrder: Int,
)

data class PlaybackHistoryItem(
  val eventId: Long,
  val track: AudioTrack,
  val playedAtMs: Long,
  val playedAtIso: String,
  val listenedMs: Long,
  val lastPositionMs: Long,
)

data class ConversionProgressState(
  val isRunning: Boolean = false,
  val sourceTitle: String = "",
  val format: ConversionFormat = ConversionFormat.MP3_AUDIO,
  val progress: Float = 0f,
  val statusMessage: String? = null,
  val outputDisplayPath: String? = null,
  val errorMessage: String? = null,
)

enum class AnalyticsTimeframe {
  DAILY,
  WEEKLY,
  MONTHLY,
  YEARLY,
}

enum class ListeningTimeUnit {
  SECONDS,
  MINUTES,
}

data class WeeklyTopTrack(
  val trackId: Long,
  val title: String,
  val artist: String,
  val album: String,
  val albumArtUri: String?,
  val contentUri: String,
  val durationMs: Long,
  val playCount: Int,
  val listenedMs: Long = 0L,
  val lastPlayedAtMs: Long,
  val lastPlayedAtIso: String = "",
)

data class WeeklySummaryReport(
  val topTracks: List<WeeklyTopTrack>,
  val totalPlaysThisWeek: Int,
  val totalListeningMinutes: Long,
  val uniqueTracksPlayed: Int,
  val timeframe: AnalyticsTimeframe = AnalyticsTimeframe.WEEKLY,
  val totalListenedMs: Long = 0L,
  val totalListeningSeconds: Long = 0L,
)

enum class AudioOutputRouteType {
  SPEAKER,
  WIRED_HEADPHONES,
  BLUETOOTH,
}

data class AudioOutputRoute(
  val type: AudioOutputRouteType = AudioOutputRouteType.SPEAKER,
  val deviceName: String = "",
)

