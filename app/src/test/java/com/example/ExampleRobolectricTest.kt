package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.MPlayerDatabase
import com.example.data.media.AudioScanner
import com.example.data.repository.MusicRepository
import com.example.localization.stringsForLanguage
import com.example.model.AppLanguage
import com.example.model.AudioTrack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.doubleClick
import com.example.ui.components.VideoTheaterOverlay
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.theme.MPlayerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var db: MPlayerDatabase
  private lateinit var repository: MusicRepository

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db =
      Room.inMemoryDatabaseBuilder(context, MPlayerDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    repository =
      MusicRepository(
        context = context,
        audioScanner = AudioScanner(context),
        playlistDao = db.playlistDao(),
        playHistoryDao = db.playHistoryDao(),
        cachedMediaDao = db.cachedMediaDao(),
      )
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `app name matches M player 97`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("M player 97", appName)
  }

  @Test
  fun `bilingual strings provide English and Arabic`() {
    val en = stringsForLanguage(AppLanguage.EN)
    val ar = stringsForLanguage(AppLanguage.AR)
    assertEquals("8D Audio", en.audio8DTitle)
    assertEquals("صوت 8D", ar.audio8DTitle)
  }

  @Test
  fun `playlist creation and manual reordering persist accurately`() = runTest {
    val playlistId = repository.createPlaylist("Night Drive")
    val tracks =
      listOf(
        sampleTrack(1L, "Alpha"),
        sampleTrack(2L, "Beta"),
        sampleTrack(3L, "Gamma"),
      )
    val inserted = repository.addTracksToPlaylist(playlistId, tracks)
    assertEquals(3, inserted)

    val initial = repository.observePlaylistTracks(playlistId).first()
    assertEquals(listOf("Alpha", "Beta", "Gamma"), initial.map { it.track.title })

    repository.persistManualOrder(listOf(initial[2].entryId, initial[0].entryId, initial[1].entryId))
    val reordered = repository.observePlaylistTracks(playlistId).first()
    assertEquals(listOf("Gamma", "Alpha", "Beta"), reordered.map { it.track.title })
  }

  @Test
  fun `weekly summary limits to top 7 ranked by play count and uses real listenedMs`() = runTest {
    for (i in 1..9) {
      val track = sampleTrack(i.toLong(), "Song $i")
      repeat(i) {
        val id = repository.recordTrackPlay(track)
        repository.addListenedDuration(id, 15_000L)
      }
    }
    val report = repository.observeWeeklySummary().first()
    assertEquals(7, report.topTracks.size)
    assertEquals("Song 9", report.topTracks.first().title)
    assertEquals(9, report.topTracks.first().playCount)
    assertEquals(9 * 15_000L, report.topTracks.first().listenedMs)
    assertTrue(report.totalPlaysThisWeek == 45)
    assertEquals(45 * 15_000L, report.totalListenedMs)
    assertEquals(11L, report.totalListeningMinutes)
  }

  @Test
  fun `timeframe filter queries Daily Weekly Monthly Yearly accurately and syncs JSON file`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val nowMs = 1_794_823_200_000L // Fixed reference point
    val oneDayMs = 24L * 60L * 60L * 1000L
    val events =
      listOf(
        com.example.data.db.PlayHistoryEntity(
          id = 1L,
          trackId = 10L,
          title = "Today Song",
          artist = "Artist A",
          album = "Album A",
          albumArtUri = null,
          contentUri = "content://audio/10",
          durationMs = 200_000L,
          playedAtMs = nowMs - 60_000L,
          listenedMs = 90_000L,
          playedAtIso = com.example.data.analytics.UsageStatsEngine.formatIsoTimestamp(nowMs - 60_000L),
        ),
        com.example.data.db.PlayHistoryEntity(
          id = 2L,
          trackId = 11L,
          title = "Last Week Song",
          artist = "Artist B",
          album = "Album B",
          albumArtUri = null,
          contentUri = "content://audio/11",
          durationMs = 180_000L,
          playedAtMs = nowMs - 4L * oneDayMs,
          listenedMs = 120_000L,
          playedAtIso = com.example.data.analytics.UsageStatsEngine.formatIsoTimestamp(nowMs - 4L * oneDayMs),
        ),
        com.example.data.db.PlayHistoryEntity(
          id = 3L,
          trackId = 12L,
          title = "Last Month Song",
          artist = "Artist C",
          album = "Album C",
          albumArtUri = null,
          contentUri = "content://audio/12",
          durationMs = 240_000L,
          playedAtMs = nowMs - 20L * oneDayMs,
          listenedMs = 180_000L,
          playedAtIso = com.example.data.analytics.UsageStatsEngine.formatIsoTimestamp(nowMs - 20L * oneDayMs),
        ),
        com.example.data.db.PlayHistoryEntity(
          id = 4L,
          trackId = 13L,
          title = "This Year Song",
          artist = "Artist D",
          album = "Album D",
          albumArtUri = null,
          contentUri = "content://audio/13",
          durationMs = 300_000L,
          playedAtMs = nowMs - 120L * oneDayMs,
          listenedMs = 240_000L,
          playedAtIso = com.example.data.analytics.UsageStatsEngine.formatIsoTimestamp(nowMs - 120L * oneDayMs),
        ),
      )

    val daily = com.example.data.analytics.UsageStatsEngine.aggregateUsageReport(events, com.example.model.AnalyticsTimeframe.DAILY, nowMs)
    val weekly = com.example.data.analytics.UsageStatsEngine.aggregateUsageReport(events, com.example.model.AnalyticsTimeframe.WEEKLY, nowMs)
    val monthly = com.example.data.analytics.UsageStatsEngine.aggregateUsageReport(events, com.example.model.AnalyticsTimeframe.MONTHLY, nowMs)
    val yearly = com.example.data.analytics.UsageStatsEngine.aggregateUsageReport(events, com.example.model.AnalyticsTimeframe.YEARLY, nowMs)

    assertEquals(1, daily.totalPlaysThisWeek)
    assertEquals(90_000L, daily.totalListenedMs)
    assertEquals(2, weekly.totalPlaysThisWeek)
    assertEquals(210_000L, weekly.totalListenedMs)
    assertEquals(3, monthly.totalPlaysThisWeek)
    assertEquals(390_000L, monthly.totalListenedMs)
    assertEquals(4, yearly.totalPlaysThisWeek)
    assertEquals(630_000L, yearly.totalListenedMs)
    assertEquals(4, yearly.uniqueTracksPlayed)

    // Verify local JSON file sync in app files directory
    val trackId = repository.recordTrackPlay(sampleTrack(77L, "Synced Track"))
    repository.addListenedDuration(trackId, 35_000L)
    val syncResult = repository.syncUsageStatsJson(externalDocumentUri = null)
    assertTrue(syncResult.success)
    val jsonFile = java.io.File(syncResult.internalFilePath)
    assertTrue(jsonFile.exists())
    val jsonText = jsonFile.readText()
    assertTrue(jsonText.contains("Synced Track"))
    assertTrue(jsonText.contains("35000"))
    assertTrue(jsonText.contains("playedAtIso"))
  }

  @Test
  fun `stoppedPlaybackUiState resets playback and queue while keeping user modes`() {
    val activeTrack = sampleTrack(42L, "Night Drive")
    val playing =
      com.example.playback.PlaybackUiState(
        currentTrack = activeTrack,
        queue = listOf(activeTrack),
        currentIndex = 0,
        isPlaying = true,
        positionMs = 64_000L,
        durationMs = 180_000L,
        repeatMode = com.example.model.RepeatMode.ALL,
        shuffleEnabled = true,
        is8DEnabled = true,
        audio8DSpeed = com.example.model.Audio8DSpeed.FAST,
        audio8DIntensity = 90,
      )

    val stopped = com.example.playback.stoppedPlaybackUiState(playing)
    assertEquals(null, stopped.currentTrack)
    assertTrue(stopped.queue.isEmpty())
    assertEquals(-1, stopped.currentIndex)
    assertEquals(false, stopped.isPlaying)
    assertEquals(0L, stopped.positionMs)
    assertEquals(0L, stopped.durationMs)
    assertEquals(com.example.model.RepeatMode.ALL, stopped.repeatMode)
    assertTrue(stopped.shuffleEnabled)
    assertTrue(stopped.is8DEnabled)
    assertEquals(com.example.model.Audio8DSpeed.FAST, stopped.audio8DSpeed)
    assertEquals(90, stopped.audio8DIntensity)
  }

  @Test
  fun `floating widget color mode and glass opacity mode persist independently in DataStore`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = com.example.data.prefs.UserPreferencesRepository(context)

    prefsRepo.setFloatingWidgetColorMode(com.example.model.FloatingWidgetColorMode.NEUTRAL)
    val afterColorOnly = prefsRepo.preferencesFlow.first()
    assertEquals(com.example.model.FloatingWidgetColorMode.NEUTRAL, afterColorOnly.floatingWidgetColorMode)
    assertEquals(com.example.model.FloatingWidgetGlassMode.FROSTED_BLUR, afterColorOnly.floatingWidgetGlassMode)

    prefsRepo.setFloatingWidgetGlassMode(com.example.model.FloatingWidgetGlassMode.FULLY_TRANSPARENT)
    val afterGlassChange = prefsRepo.preferencesFlow.first()
    assertEquals(com.example.model.FloatingWidgetColorMode.NEUTRAL, afterGlassChange.floatingWidgetColorMode)
    assertEquals(com.example.model.FloatingWidgetGlassMode.FULLY_TRANSPARENT, afterGlassChange.floatingWidgetGlassMode)

    prefsRepo.setFloatingWidgetColorMode(com.example.model.FloatingWidgetColorMode.PITCH_BLACK_GOLD)
    val afterSecondColorChange = prefsRepo.preferencesFlow.first()
    assertEquals(com.example.model.FloatingWidgetColorMode.PITCH_BLACK_GOLD, afterSecondColorChange.floatingWidgetColorMode)
    assertEquals(com.example.model.FloatingWidgetGlassMode.FULLY_TRANSPARENT, afterSecondColorChange.floatingWidgetGlassMode)

    // Restore defaults
    prefsRepo.setFloatingWidgetColorMode(com.example.model.FloatingWidgetColorMode.MATCH_APP)
    prefsRepo.setFloatingWidgetGlassMode(com.example.model.FloatingWidgetGlassMode.FROSTED_BLUR)
  }

  @Test
  fun `importUsageStatsFromJson parses exports restores history and deduplicates re-imports`() = runTest {
    val playedMs = 1_794_823_200_000L
    val exportedJson =
      com.example.data.analytics.UsageStatsEngine.buildUsageStatsJson(
        events =
          listOf(
            com.example.data.db.PlayHistoryEntity(
              id = 1L,
              trackId = 101L,
              title = "Imported Anthem",
              artist = "Studio Artist",
              album = "Midnight",
              albumArtUri = null,
              contentUri = "content://media/external/audio/media/101",
              durationMs = 210_000L,
              playedAtMs = playedMs,
              listenedMs = 64_000L,
              playedAtIso = com.example.data.analytics.UsageStatsEngine.formatIsoTimestamp(playedMs),
            )
          ),
        nowMs = playedMs + 1000L,
      )

    val parsed = com.example.data.analytics.UsageStatsEngine.parseUsageStatsJson(exportedJson)
    assertEquals(1, parsed.size)
    assertEquals("Imported Anthem", parsed.first().title)
    assertEquals(64_000L, parsed.first().listenedMs)

    val firstImport = repository.importUsageStatsFromJson(exportedJson)
    assertTrue(firstImport.success)
    assertEquals(1, firstImport.importedCount)

    // Re-importing exact same JSON skips duplicates cleanly
    val duplicateImport = repository.importUsageStatsFromJson(exportedJson)
    assertTrue(duplicateImport.success)
    assertEquals(0, duplicateImport.importedCount)
    assertEquals(1, duplicateImport.skippedDuplicates)

    // Re-importing with increased listenedMs updates the existing record instead of duplicating it
    val updatedJson = exportedJson.replace("64000", "128000")
    val upgradeImport = repository.importUsageStatsFromJson(updatedJson)
    assertTrue(upgradeImport.success)
    assertEquals(1, upgradeImport.importedCount)

    val allEvents = db.playHistoryDao().getAllHistorySnapshot()
    assertEquals(1, allEvents.size)
    assertEquals(128_000L, allEvents.first().listenedMs)
  }

  @Test
  fun `listeningTimeUnit preference persists Seconds and Minutes in DataStore`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = com.example.data.prefs.UserPreferencesRepository(context)

    prefsRepo.setListeningTimeUnit(com.example.model.ListeningTimeUnit.SECONDS)
    assertEquals(
      com.example.model.ListeningTimeUnit.SECONDS,
      prefsRepo.preferencesFlow.first().listeningTimeUnit,
    )

    prefsRepo.setListeningTimeUnit(com.example.model.ListeningTimeUnit.MINUTES)
    assertEquals(
      com.example.model.ListeningTimeUnit.MINUTES,
      prefsRepo.preferencesFlow.first().listeningTimeUnit,
    )
  }

  @Test
  fun `playlist add-only backup preserves deleted playlists and restores them on import`() = runTest {
    val p1 = repository.createPlaylist("Late Night")
    repository.addTracksToPlaylist(p1, listOf(sampleTrack(11L, "Nocturne")))
    val p2 = repository.createPlaylist("Road Trip")
    repository.addTracksToPlaylist(p2, listOf(sampleTrack(12L, "Horizon")))

    // 1. Initial sync creates mplayer97_playlists.json with both playlists
    val firstSync = repository.syncPlaylistsBackupAddOnly(externalDocumentUri = null)
    assertTrue(firstSync.success)
    assertEquals(2, firstSync.archivedPlaylistCount)

    // 2. Delete "Late Night" inside app, then add a new playlist "Workout" and sync add-only again
    repository.deletePlaylist(p1)
    val p3 = repository.createPlaylist("Workout")
    repository.addTracksToPlaylist(p3, listOf(sampleTrack(13L, "Pulse")))

    val secondSync = repository.syncPlaylistsBackupAddOnly(externalDocumentUri = null)
    assertTrue(secondSync.success)
    assertEquals(3, secondSync.archivedPlaylistCount)

    val backupJson = java.io.File(secondSync.internalFilePath).readText(Charsets.UTF_8)
    val archivedPlaylists = com.example.data.analytics.PlaylistBackupEngine.parsePlaylistsJson(backupJson)
    assertEquals(3, archivedPlaylists.size)
    assertTrue(archivedPlaylists.any { it.name == "Late Night" && it.tracks.first().title == "Nocturne" })

    // 3. Import backup JSON restores deleted "Late Night" playlist without duplicating existing ones
    val importResult = repository.importPlaylistsFromJson(backupJson)
    assertTrue(importResult.success)
    assertEquals(1, importResult.importedPlaylistsCount)
    assertEquals(1, importResult.importedTracksCount)

    val currentPlaylists = repository.observePlaylists().first()
    assertEquals(3, currentPlaylists.size)
    assertTrue(currentPlaylists.any { it.name == "Late Night" && it.trackCount == 1 })
  }

  @Test
  fun `favorites playlist protects deletion and toggles mixed audio and video items`() = runTest {
    val favId = repository.ensureFavoritesPlaylist("Liked")
    val playlists = repository.observePlaylists().first()
    val favSummary = playlists.first { it.id == favId }
    assertTrue(favSummary.isFavorites)
    assertEquals("Liked", favSummary.name)

    val audioItem = sampleTrack(201L, "Midnight Echo")
    val videoItem =
      sampleTrack(202L, "Concert Clip").copy(
        contentUri = Uri.parse("content://media/external/video/media/202"),
        mimeType = "video/mp4",
        mediaType = com.example.model.MediaType.VIDEO,
      )

    assertTrue(repository.toggleFavorite(audioItem, "Liked"))
    assertTrue(repository.toggleFavorite(videoItem, "Liked"))

    val favUris = repository.observeFavoriteUris().first()
    assertTrue(favUris.contains(audioItem.contentUri.toString()))
    assertTrue(favUris.contains(videoItem.contentUri.toString()))

    val favEntries = repository.observePlaylistTracks(favId).first()
    assertEquals(2, favEntries.size)
    assertTrue(favEntries.any { it.track.mediaType == com.example.model.MediaType.VIDEO })

    // Protected Favorites playlist cannot be deleted
    repository.deletePlaylist(favId)
    val afterDeleteAttempt = repository.observePlaylists().first()
    assertTrue(afterDeleteAttempt.any { it.id == favId && it.isFavorites })

    // Second toggle removes item cleanly
    val stillLiked = repository.toggleFavorite(videoItem, "Liked")
    assertEquals(false, stillLiked)
    val favUrisAfter = repository.observeFavoriteUris().first()
    assertEquals(false, favUrisAfter.contains(videoItem.contentUri.toString()))
    assertTrue(favUrisAfter.contains(audioItem.contentUri.toString()))
  }

  @Test
  fun `playback history records mixed media types saved position and delete or clear`() = runTest {
    val audioTrack = sampleTrack(301L, "Lofi Beat")
    val videoTrack =
      sampleTrack(302L, "Music Video").copy(
        contentUri = Uri.parse("content://media/external/video/media/302"),
        mimeType = "video/mp4",
        mediaType = com.example.model.MediaType.VIDEO,
      )

    val ev1 = repository.recordTrackPlay(audioTrack)
    repository.updateLastPlaybackPosition(ev1, 45_000L)
    val ev2 = repository.recordTrackPlay(videoTrack)
    repository.updateLastPlaybackPosition(ev2, 92_500L)

    val history = repository.observePlaybackHistory().first()
    assertEquals(2, history.size)
    val videoEntry = history.first { it.eventId == ev2 }
    assertEquals(com.example.model.MediaType.VIDEO, videoEntry.track.mediaType)
    assertEquals(92_500L, videoEntry.lastPositionMs)

    val audioEntry = history.first { it.eventId == ev1 }
    assertEquals(com.example.model.MediaType.AUDIO, audioEntry.track.mediaType)
    assertEquals(45_000L, audioEntry.lastPositionMs)

    repository.deleteHistoryItem(ev1)
    assertEquals(1, repository.observePlaybackHistory().first().size)

    repository.clearHistory()
    assertTrue(repository.observePlaybackHistory().first().isEmpty())
  }

  @Test
  fun `video playback mode and converter folder URI persist in DataStore`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = com.example.data.prefs.UserPreferencesRepository(context)

    prefsRepo.setDefaultVideoPlaybackMode(com.example.model.VideoPlaybackMode.AUDIO_ONLY)
    prefsRepo.setConverterOutputTreeUri("content://com.android.externalstorage.documents/tree/primary%3AM%20player%2097")

    val prefs = prefsRepo.preferencesFlow.first()
    assertEquals(com.example.model.VideoPlaybackMode.AUDIO_ONLY, prefs.defaultVideoPlaybackMode)
    assertEquals(
      "content://com.android.externalstorage.documents/tree/primary%3AM%20player%2097",
      prefs.converterOutputTreeUri,
    )

    prefsRepo.setDefaultVideoPlaybackMode(com.example.model.VideoPlaybackMode.VIDEO_AND_AUDIO)
    prefsRepo.setConverterOutputTreeUri(null)
  }

  @Test
  fun `floatingWidgetCustomOpacity persists independently of color mode and clears on preset tap`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = com.example.data.prefs.UserPreferencesRepository(context)

    prefsRepo.setFloatingWidgetColorMode(com.example.model.FloatingWidgetColorMode.ROYAL_PURPLE)
    prefsRepo.setFloatingWidgetCustomOpacity(0.42f)

    val withSlider = prefsRepo.preferencesFlow.first()
    assertEquals(com.example.model.FloatingWidgetColorMode.ROYAL_PURPLE, withSlider.floatingWidgetColorMode)
    assertEquals(0.42f, withSlider.floatingWidgetCustomOpacity ?: -1f, 0.001f)
    assertEquals(com.example.model.FloatingWidgetGlassMode.SEMI_FROSTED, withSlider.floatingWidgetGlassMode)

    // Tapping a preset clears customOpacity while leaving widget theme color untouched
    prefsRepo.setFloatingWidgetGlassMode(com.example.model.FloatingWidgetGlassMode.FROSTED_BLUR)
    val afterPreset = prefsRepo.preferencesFlow.first()
    assertEquals(com.example.model.FloatingWidgetColorMode.ROYAL_PURPLE, afterPreset.floatingWidgetColorMode)
    assertEquals(null, afterPreset.floatingWidgetCustomOpacity)
    assertEquals(com.example.model.FloatingWidgetGlassMode.FROSTED_BLUR, afterPreset.floatingWidgetGlassMode)

    // Cleanup
    prefsRepo.setFloatingWidgetColorMode(com.example.model.FloatingWidgetColorMode.MATCH_APP)
  }

  @Test
  fun `cachedMediaDao provides instant startup cache and incremental upsert and deletion per bucket`() = runTest {
    val cachedDao = db.cachedMediaDao()
    val audioEntity =
      com.example.data.db.CachedMediaEntity(
        contentUri = "content://media/external/audio/media/501",
        trackId = 501L,
        title = "Instant Audio",
        artist = "Cache Artist",
        album = "Cache Album",
        albumId = 10L,
        durationMs = 195_000L,
        albumArtUri = null,
        dateAddedMs = 10_000L,
        dateModifiedMs = 20_000L,
        sizeBytes = 5_000_000L,
        mimeType = "audio/mpeg",
        folderName = "Music",
        mediaType = "AUDIO",
        sourceBucket = AudioScanner.BUCKET_AUDIO,
      )
    val videoEntity =
      com.example.data.db.CachedMediaEntity(
        contentUri = "content://media/external/video/media/601",
        trackId = 0x4000_0000_0000_0000L or 601L,
        title = "Instant Video",
        artist = "Video Creator",
        album = "Local Videos",
        albumId = 0L,
        durationMs = 310_000L,
        albumArtUri = null,
        dateAddedMs = 15_000L,
        dateModifiedMs = 30_000L,
        sizeBytes = 25_000_000L,
        mimeType = "video/mp4",
        folderName = "Movies",
        mediaType = "VIDEO",
        sourceBucket = AudioScanner.BUCKET_VIDEO,
      )

    cachedDao.applySyncDiff(upserts = listOf(audioEntity, videoEntity), deletedUris = emptyList())

    val instant = repository.observeCachedMedia().first()
    assertEquals(2, instant.size)
    assertEquals("Instant Video", instant[0].title)
    assertEquals(com.example.model.MediaType.VIDEO, instant[0].mediaType)
    assertEquals("Instant Audio", instant[1].title)
    assertEquals(com.example.model.MediaType.AUDIO, instant[1].mediaType)

    // Incremental diff deletes removed audio while updating/adding another without touching video
    val newAudioEntity =
      audioEntity.copy(
        contentUri = "content://media/external/audio/media/502",
        trackId = 502L,
        title = "Newly Downloaded Song",
        dateModifiedMs = 40_000L,
      )
    cachedDao.applySyncDiff(
      upserts = listOf(newAudioEntity),
      deletedUris = listOf(audioEntity.contentUri),
    )

    val updated = repository.observeCachedMedia().first()
    assertEquals(2, updated.size)
    assertEquals("Newly Downloaded Song", updated.first().title)
    assertTrue(updated.any { it.contentUri.toString() == videoEntity.contentUri })
    assertEquals(false, updated.any { it.contentUri.toString() == audioEntity.contentUri })

    // Native delete cleanup removes item immediately by contentUri or trackId
    repository.deleteCachedMedia(Uri.parse(newAudioEntity.contentUri), newAudioEntity.trackId)
    val afterDelete = repository.observeCachedMedia().first()
    assertEquals(1, afterDelete.size)
    assertEquals(videoEntity.contentUri, afterDelete.first().contentUri.toString())
  }

  @Test
  fun `lock screen artwork compression enforces binder size cap and populates MediaMetadata artworkData`() {
    val largeBitmap = android.graphics.Bitmap.createBitmap(1200, 1200, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(largeBitmap)
    canvas.drawColor(android.graphics.Color.rgb(120, 20, 45))

    val jpegBytes = AudioScanner.compressBitmapToBinderSafeJpeg(largeBitmap, maxDimension = 512)
    assertTrue(jpegBytes.isNotEmpty())
    assertTrue(jpegBytes.size <= AudioScanner.MAX_BINDER_ART_BYTES)

    val audioTrack = sampleTrack(801L, "Cover Track")
    val audioMediaItem =
      com.example.playback.buildTrackMediaItem(track = audioTrack, artworkBytes = jpegBytes)
    assertEquals("Cover Track", audioMediaItem.mediaMetadata.title?.toString())
    assertTrue(audioMediaItem.mediaMetadata.artworkData?.isNotEmpty() == true)
    assertEquals(
      androidx.media3.common.MediaMetadata.PICTURE_TYPE_FRONT_COVER.toInt(),
      audioMediaItem.mediaMetadata.artworkDataType,
    )
    assertTrue(audioMediaItem.mediaMetadata.artworkUri != null)

    val videoTrack =
      sampleTrack(802L, "Video Audio Stream").copy(
        contentUri = Uri.parse("content://media/external/video/media/802"),
        mimeType = "video/mp4",
        mediaType = com.example.model.MediaType.VIDEO,
      )
    val videoMediaItem =
      com.example.playback.buildTrackMediaItem(track = videoTrack, artworkBytes = jpegBytes)
    assertTrue(videoMediaItem.mediaMetadata.artworkData?.contentEquals(jpegBytes) == true)
    assertEquals("mplayerart", videoMediaItem.mediaMetadata.artworkUri?.scheme)
  }

  @Test
  fun `video player shows single fullscreen button and auto-hiding frosted glass bar inside fullscreen`() {
    val videoTrack =
      sampleTrack(901L, "Cinema Demo Video").copy(
        contentUri = Uri.parse("content://media/external/video/media/901"),
        mimeType = "video/mp4",
        mediaType = com.example.model.MediaType.VIDEO,
      )
    val playback =
      com.example.playback.PlaybackUiState(
        currentTrack = videoTrack,
        queue = listOf(videoTrack),
        currentIndex = 0,
        isPlaying = true,
        durationMs = 120_000L,
        positionMs = 15_000L,
        isVideoAudioOnly = false,
      )

    var enteredFullscreen = false
    composeTestRule.setContent {
      MPlayerTheme(themeStyle = com.example.model.AppThemeStyle.DEEP_BURGUNDY) {
        NowPlayingScreen(
          playback = playback,
          sleepTimerState = com.example.model.SleepTimerState(),
          isEqEnabled = false,
          panAngleDegrees = 0f,
          audioAmplitude = 0f,
          bassEnergy = 0f,
          waveformSamples = FloatArray(32),
          spectrumBins = FloatArray(16),
          visualizerMode = com.example.model.VisualizerMode.ALBUM_ART,
          onSelectVisualizerMode = {},
          onPlayPause = {},
          onNext = {},
          onPrevious = {},
          onSeek = {},
          onToggleShuffle = {},
          onCycleRepeat = {},
          onToggle8D = {},
          onOpen8DPanel = {},
          onOpenSleepTimer = {},
          onOpenEqualizer = {},
          onJumpToQueueIndex = {},
          onAddToPlaylistClick = {},
          onEnterFullscreen = { enteredFullscreen = true },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("video_fullscreen_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("portrait_video_tools_bar").assertDoesNotExist()
    composeTestRule.onNodeWithTag("video_rotate_btn").assertDoesNotExist()
    composeTestRule.onNodeWithTag("video_lock_btn").assertDoesNotExist()

    composeTestRule.onNodeWithTag("video_fullscreen_btn").performClick()
    assertTrue(enteredFullscreen)
  }

  @Test
  fun `fullscreen theater overlay displays frosted glass bar with rotate lock and auto-hides on inactivity`() {
    val videoTrack =
      sampleTrack(902L, "Theater Clip").copy(
        contentUri = Uri.parse("content://media/external/video/media/902"),
        mimeType = "video/mp4",
        mediaType = com.example.model.MediaType.VIDEO,
      )
    val playback =
      com.example.playback.PlaybackUiState(
        currentTrack = videoTrack,
        queue = listOf(videoTrack),
        currentIndex = 0,
        isPlaying = true,
        durationMs = 180_000L,
        positionMs = 20_000L,
        isVideoAudioOnly = false,
      )

    composeTestRule.setContent {
      MPlayerTheme(themeStyle = com.example.model.AppThemeStyle.PITCH_BLACK_GOLD) {
        VideoTheaterOverlay(
          playback = playback,
          isLandscape = false,
          isLocked = false,
          onToggleLandscape = {},
          onSetLocked = {},
          onSetVideoAudioOnlyMode = {},
          onPlayPause = {},
          onNext = {},
          onPrevious = {},
          onSeek = {},
          onToggleShuffle = {},
          onCycleRepeat = {},
          onAttachVideoTextureView = {},
          onDetachVideoTextureView = {},
          onExitTheater = {},
          autoHideDelayMs = 500L,
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("fullscreen_glass_control_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("theater_rotate_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("theater_lock_btn").assertIsDisplayed()

    // Inactivity auto-hides the glass control bar cleanly
    composeTestRule.waitUntil(timeoutMillis = 4_000L) {
      composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag("fullscreen_glass_control_bar")).fetchSemanticsNodes().isEmpty()
    }
    composeTestRule.onNodeWithTag("fullscreen_glass_control_bar").assertDoesNotExist()

    // Tapping anywhere on screen brings the frosted glass control bar back temporarily
    composeTestRule.onNodeWithTag("theater_tap_surface").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("fullscreen_glass_control_bar").assertIsDisplayed()
  }

  @Test
  fun `more screen displays all five ultra dark themes including luxury dark brown`() {
    var selectedTheme: com.example.model.AppThemeStyle? = null
    val summary =
      com.example.model.WeeklySummaryReport(
        topTracks = emptyList(),
        totalPlaysThisWeek = 0,
        totalListeningMinutes = 0L,
        uniqueTracksPlayed = 0,
      )
    composeTestRule.setContent {
      MPlayerTheme(
        themeStyle = com.example.model.AppThemeStyle.LUXURY_DARK_BROWN,
        language = com.example.model.AppLanguage.AR,
      ) {
        com.example.ui.screens.MoreScreen(
          preferences = com.example.data.prefs.UserPreferences(themeStyle = com.example.model.AppThemeStyle.LUXURY_DARK_BROWN),
          weeklySummary = summary,
          selectedTimeframe = com.example.model.AnalyticsTimeframe.WEEKLY,
          canDrawOverlays = true,
          onSelectTheme = { selectedTheme = it },
          onToggleDarkMode = {},
          onSelectLanguage = {},
          onSet8DEnabled = {},
          onSet8DSpeed = {},
          onSet8DIntensity = {},
          onOpen8DPanel = {},
          onOpenEqualizer = {},
          onOpenSleepTimer = {},
          onRequestOverlayPermission = {},
          onLaunchFloatingOverlay = {},
          onSelectTimeframe = {},
          onSetListeningTimeUnit = {},
          onSelectAutoSyncFile = {},
          onImportUsageStats = {},
          onPlayTop7Mix = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("theme_deep_burgundy_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("theme_royal_purple_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("theme_midnight_blue_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("theme_pitch_black_gold_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("theme_luxury_dark_brown_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("theme_cosmic_starfield_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("theme_plain_black_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("app_glass_opacity_slider").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("video_mode_normal_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("فيديو عادي").assertIsDisplayed()
    composeTestRule.onNodeWithText("صوت فقط").assertIsDisplayed()
    composeTestRule.onNodeWithTag("converter_folder_compact_row").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("theme_cosmic_starfield_card").performScrollTo().performClick()
    assertEquals(com.example.model.AppThemeStyle.COSMIC_STARFIELD, selectedTheme)
    composeTestRule.onNodeWithTag("theme_plain_black_card").performScrollTo().performClick()
    assertEquals(com.example.model.AppThemeStyle.PLAIN_BLACK, selectedTheme)
  }

  @Test
  fun `now playing screen frees space with compact equalizer hub pill and shows bluetooth route`() {
    val track = sampleTrack(950L, "Bass Test")
    var openedEqHub = false
    composeTestRule.setContent {
      val playback =
        com.example.playback.PlaybackUiState(
          currentTrack = track,
          queue = listOf(track),
          currentIndex = 0,
          isPlaying = true,
          durationMs = 180_000L,
          positionMs = 10_000L,
          is8DEnabled = true,
          volumeBoostPercent = 180,
          amplifierEnabled = true,
          amplifierLevelPercent = 75,
          audioOutputRoute =
            com.example.model.AudioOutputRoute(
              type = com.example.model.AudioOutputRouteType.BLUETOOTH,
              deviceName = "WH-1000XM5",
            ),
        )
      MPlayerTheme(
        themeStyle = com.example.model.AppThemeStyle.DEEP_BURGUNDY,
        language = com.example.model.AppLanguage.AR,
      ) {
        NowPlayingScreen(
          playback = playback,
          sleepTimerState = com.example.model.SleepTimerState(),
          isEqEnabled = true,
          panAngleDegrees = 0f,
          audioAmplitude = 0f,
          bassEnergy = 0f,
          waveformSamples = FloatArray(32),
          spectrumBins = FloatArray(20),
          visualizerMode = com.example.model.VisualizerMode.ALBUM_ART,
          onSelectVisualizerMode = {},
          onPlayPause = {},
          onNext = {},
          onPrevious = {},
          onSeek = {},
          onToggleShuffle = {},
          onCycleRepeat = {},
          onOpenSleepTimer = {},
          onOpenEqualizer = { openedEqHub = true },
          onJumpToQueueIndex = {},
          onAddToPlaylistClick = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("audio_output_route_badge").assertIsDisplayed()
    composeTestRule.onNodeWithText("سماعة بلوتوث: WH-1000XM5").assertIsDisplayed()
    composeTestRule.onNodeWithTag("player_8d_toggle_card").assertDoesNotExist()
    composeTestRule.onNodeWithTag("player_volume_boost_btn").assertDoesNotExist()
    composeTestRule.onNodeWithTag("player_equalizer_btn").performScrollTo().assertIsDisplayed().performClick()
    assertTrue(openedEqHub)
  }

  @Test
  fun `equalizer sheet hosts 8D controls, 0 to 797 volume boost with warning, and separate amplifier`() {
    composeTestRule.setContent {
      val boostState = remember { mutableStateOf(100) }
      val is8DState = remember { mutableStateOf(false) }
      val speedState = remember { mutableStateOf(com.example.model.Audio8DSpeed.NORMAL) }
      val intensityState = remember { mutableStateOf(80) }
      val ampEnabledState = remember { mutableStateOf(false) }
      val ampLevelState = remember { mutableStateOf(50) }
      val playback =
        com.example.playback.PlaybackUiState(
          currentTrack = sampleTrack(951L, "Equalizer Hub Track"),
          isPlaying = true,
          is8DEnabled = is8DState.value,
          audio8DSpeed = speedState.value,
          audio8DIntensity = intensityState.value,
          volumeBoostPercent = boostState.value,
          amplifierEnabled = ampEnabledState.value,
          amplifierLevelPercent = ampLevelState.value,
        )
      MPlayerTheme(
        themeStyle = com.example.model.AppThemeStyle.DEEP_BURGUNDY,
        language = com.example.model.AppLanguage.AR,
      ) {
        com.example.ui.components.EqualizerSheet(
          isEnabled = true,
          activePreset = com.example.model.EqualizerPreset.FLAT,
          bandGainsDb = listOf(0f, 0f, 0f, 0f, 0f),
          onToggleEnabled = {},
          onSelectPreset = {},
          onChangeBandGain = { _, _ -> },
          onDismiss = {},
          playback = playback,
          panAngleDegrees = 45f,
          onToggle8D = { is8DState.value = it },
          onSelect8DSpeed = { speedState.value = it },
          onChange8DIntensity = { intensityState.value = it },
          onSetVolumeBoost = { boostState.value = it },
          onToggleAmplifier = { ampEnabledState.value = it },
          onChangeAmplifierLevel = { ampLevelState.value = it },
        )
      }
    }

    composeTestRule.waitForIdle()
    // 1. 8D Audio section + expandable radar inside Equalizer
    composeTestRule.onNodeWithTag("eq_8d_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("eq_8d_switch").performClick()
    composeTestRule.onNodeWithTag("eq_toggle_8d_radar_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("audio_8d_radar").performScrollTo().assertIsDisplayed()

    // 2. Volume Boost 0..797 with safety warning dialog past 397% ("تجاهل")
    composeTestRule.onNodeWithTag("volume_boost_slider").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {
      it(550f)
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("volume_boost_warning_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("volume_boost_ignore_btn").assertIsDisplayed()
    composeTestRule.onNodeWithText("تجاهل").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("volume_boost_warning_dialog").assertDoesNotExist()
    composeTestRule.onNodeWithTag("volume_boost_slider").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {
      it(797f)
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("797% • تضخيم نقي + محدد ديناميكي").performScrollTo().assertIsDisplayed()

    // 3. Separate Real Amplifier card with switch & dedicated slider up to +97.0 dB
    composeTestRule.onNodeWithTag("eq_amplifier_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("eq_amplifier_switch").performClick()
    composeTestRule.onNodeWithTag("eq_amplifier_slider").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {
      it(97f)
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("+97 dB").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("+97.0 dB").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `library media filter and selected folder persist cleanly in DataStore`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = com.example.data.prefs.UserPreferencesRepository(context)

    prefsRepo.setLibrarySort(com.example.model.LibrarySortOption.DURATION_DESC)
    prefsRepo.setLibraryMediaFilter(com.example.model.LibraryMediaFilter.VIDEO)
    prefsRepo.setLibrarySelectedFolder("Movies")

    val loaded = prefsRepo.preferencesFlow.first()
    assertEquals(com.example.model.LibrarySortOption.DURATION_DESC, loaded.librarySort)
    assertEquals(com.example.model.LibraryMediaFilter.VIDEO, loaded.libraryMediaFilter)
    assertEquals("Movies", loaded.librarySelectedFolder)

    // Restore defaults
    prefsRepo.setLibrarySort(com.example.model.LibrarySortOption.DATE_MODIFIED_DESC)
    prefsRepo.setLibraryMediaFilter(com.example.model.LibraryMediaFilter.ALL)
    prefsRepo.setLibrarySelectedFolder(null)
    val cleared = prefsRepo.preferencesFlow.first()
    assertEquals(com.example.model.LibraryMediaFilter.ALL, cleared.libraryMediaFilter)
    assertEquals(null, cleared.librarySelectedFolder)
  }

  @Test
  fun `music repository auto-resume resolves long media and resets after completion`() = runTest {
    val podcast =
      sampleTrack(1101L, "Long Podcast").copy(
        durationMs = 600_000L, // 10 min (>= 5 min)
      )
    val shortSong =
      sampleTrack(1102L, "Short Song").copy(
        durationMs = 180_000L, // 3 min (< 5 min)
      )
    val videoClip =
      sampleTrack(1103L, "Long Video").copy(
        contentUri = Uri.parse("content://media/external/video/media/1103"),
        mimeType = "video/mp4",
        mediaType = com.example.model.MediaType.VIDEO,
        durationMs = 150_000L, // 2.5 min (>= 60s)
      )

    val podEv = repository.recordTrackPlay(podcast)
    repository.updateLastPlaybackPosition(podEv, 145_000L)
    val songEv = repository.recordTrackPlay(shortSong)
    repository.updateLastPlaybackPosition(songEv, 75_000L)
    val vidEv = repository.recordTrackPlay(videoClip)
    repository.updateLastPlaybackPosition(vidEv, 52_000L)

    assertEquals(145_000L, repository.getAutoResumePositionMs(podcast))
    assertEquals(0L, repository.getAutoResumePositionMs(shortSong))
    assertEquals(52_000L, repository.getAutoResumePositionMs(videoClip))

    // Finishing playback clears saved resume position cleanly
    repository.clearSavedResumePosition(videoClip.contentUri)
    assertEquals(0L, repository.getAutoResumePositionMs(videoClip))
  }

  @Test
  fun `audio scanner direct directory walk discovers newly added files and deduplicates`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val scanner = AudioScanner(context)
    val tempRoot = kotlin.io.path.createTempDirectory("mplayer_scan_test").toFile()
    try {
      val subDir = java.io.File(tempRoot, "Downloads").apply { mkdirs() }
      val mp3 = java.io.File(subDir, "FreshTrack.mp3").apply { writeBytes(ByteArray(1024) { 1 }) }
      val mp4 = java.io.File(subDir, "FreshClip.mp4").apply { writeBytes(ByteArray(2048) { 2 }) }
      java.io.File(subDir, "Notes.txt").apply { writeText("ignore me") }

      val out = mutableListOf<AudioTrack>()
      val seenUris = HashSet<String>()
      val seenSigs = HashSet<String>()

      val discoveredPaths =
        scanner.scanDirectoriesInternal(
          roots = listOf(tempRoot),
          includeAudio = true,
          includeVideo = true,
          out = out,
          seenUris = seenUris,
          seenFileSignatures = seenSigs,
        )

      assertEquals(2, out.size)
      assertEquals(2, discoveredPaths.size)
      assertTrue(out.any { it.title == "FreshTrack" && !it.isVideo && it.folderName == "Downloads" })
      assertTrue(out.any { it.title == "FreshClip" && it.isVideo && it.folderName == "Downloads" })
      assertTrue(discoveredPaths.contains(mp3.absolutePath))
      assertTrue(discoveredPaths.contains(mp4.absolutePath))
    } finally {
      tempRoot.deleteRecursively()
    }
  }

  @Test
  fun `filterAndSortLibraryTracks trims whitespace and combines folder media filter and sort`() {
    val t1 = sampleTrack(1L, "Midnight Jazz").copy(artist = "Miles", folderName = "Music", durationMs = 200_000L)
    val t2 = sampleTrack(2L, "Zebra Rock").copy(artist = "Ace", folderName = "Downloads", durationMs = 150_000L)
    val v1 =
      sampleTrack(3L, "Midnight Video").copy(
        contentUri = Uri.parse("content://media/external/video/media/3"),
        mimeType = "video/mp4",
        mediaType = com.example.model.MediaType.VIDEO,
        folderName = "Movies",
        durationMs = 300_000L,
      )

    val queried =
      com.example.viewmodel.filterAndSortLibraryTracks(
        tracks = listOf(t1, t2, v1),
        query = "  midnight ",
        folder = null,
        mediaFilter = com.example.model.LibraryMediaFilter.ALL,
        sortOption = com.example.model.LibrarySortOption.TITLE_ASC,
      )
    assertEquals(listOf("Midnight Jazz", "Midnight Video"), queried.map { it.title })

    val videoOnly =
      com.example.viewmodel.filterAndSortLibraryTracks(
        tracks = listOf(t1, t2, v1),
        query = "",
        folder = "Movies",
        mediaFilter = com.example.model.LibraryMediaFilter.VIDEO,
        sortOption = com.example.model.LibrarySortOption.DURATION_DESC,
      )
    assertEquals(listOf("Midnight Video"), videoOnly.map { it.title })
  }

  @Test
  fun `library screen removes offline badge and search placeholder, toggles clear button, and shows scroll-to-top`() {
    val sampleList = (1..24).map { idx -> sampleTrack(idx.toLong(), "Track $idx") }
    composeTestRule.setContent {
      val queryState = remember { mutableStateOf("") }
      MPlayerTheme(
        themeStyle = com.example.model.AppThemeStyle.DEEP_BURGUNDY,
        language = AppLanguage.AR,
      ) {
        LibraryScreen(
          tracks = sampleList,
          folders = listOf("Music"),
          selectedFolder = null,
          searchQuery = queryState.value,
          currentSort = com.example.model.LibrarySortOption.TITLE_ASC,
          currentTrackId = null,
          isPlaying = false,
          isScanning = false,
          hasAudioPermission = true,
          onRequestPermission = {},
          onOpenAppSettings = {},
          onRescan = {},
          onSearchQueryChange = { queryState.value = it },
          onSelectFolder = {},
          onSelectSort = {},
          onTrackClick = {},
          onPlayAll = {},
          onShuffleAll = {},
          onAddToPlaylistClick = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    // 1. Clean counter shows item count only (no "بدون إنترنت 100%" / "100% Offline")
    composeTestRule.onNodeWithTag("library_item_count").assertIsDisplayed()
    composeTestRule.onNodeWithText("24 عنصر").assertIsDisplayed()
    assertTrue(composeTestRule.onAllNodesWithText("بدون إنترنت 100%", substring = true).fetchSemanticsNodes().isEmpty())

    // 2. Search bar has no idle placeholder and hides "X" clear button when empty
    assertTrue(composeTestRule.onAllNodesWithText("ابحث", substring = true).fetchSemanticsNodes().isEmpty())
    composeTestRule.onNodeWithTag("library_search_clear_btn").assertDoesNotExist()

    // Typing shows "X" clear button and clicking it clears query back to idle
    composeTestRule.onNodeWithTag("library_search_input").performSemanticsAction(SemanticsActions.SetText) {
      it(androidx.compose.ui.text.AnnotatedString("Track 5"))
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("library_search_clear_btn").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("library_search_clear_btn").assertDoesNotExist()

    // 3. Scroll-to-top button hidden at top, appears after scrolling down, and jumps back to top
    composeTestRule.onNodeWithTag("library_scroll_to_top_btn").assertDoesNotExist()
    composeTestRule.onNodeWithTag("library_tracks_list").performScrollToIndex(14)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("library_scroll_to_top_btn").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(timeoutMillis = 3_000L) {
      composeTestRule.onAllNodes(hasTestTag("library_scroll_to_top_btn")).fetchSemanticsNodes().isEmpty()
    }
  }

  @Test
  fun `music repository logs distinct play sessions for stats while deduplicating history and respecting restart vs dismiss`() = runTest {
    val podcast = sampleTrack(1201L, "Deduplicated Episode").copy(durationMs = 500_000L)
    val other = sampleTrack(1202L, "Other Track").copy(durationMs = 200_000L)

    // Backdated historical session from 3 days ago (must NOT leak into Daily stats when replayed today)
    val nowMs = System.currentTimeMillis()
    val threeDaysAgoMs = nowMs - 3L * 24L * 60L * 60L * 1000L
    db.playHistoryDao().insertPlayEvent(
      com.example.data.db.PlayHistoryEntity(
        trackId = podcast.id,
        title = podcast.title,
        artist = podcast.artist,
        album = podcast.album,
        albumArtUri = null,
        contentUri = podcast.contentUri.toString(),
        durationMs = podcast.durationMs,
        playedAtMs = threeDaysAgoMs,
        playedAtIso = com.example.data.analytics.UsageStatsEngine.formatIsoTimestamp(threeDaysAgoMs),
        playCount = 1,
        listenedMs = 120_000L,
        lastPositionMs = 0L,
      )
    )

    // Play same file again today -> creates immutable new session rows for accurate stats
    val firstId = repository.recordTrackPlay(podcast)
    repository.addListenedDuration(firstId, 30_000L)
    repository.updateLastPlaybackPosition(firstId, 64_000L)
    repository.recordTrackPlay(other)
    val thirdId = repository.recordTrackPlay(podcast)
    repository.addListenedDuration(thirdId, 20_000L)

    assertTrue(firstId != thirdId)

    val history = repository.observePlaybackHistory().first()
    val matching = history.filter { it.track.contentUri == podcast.contentUri }
    assertEquals(1, matching.size)
    // Dismissing dialog / not resetting preserves lastPositionMs cleanly across new session
    assertEquals(64_000L, matching.first().lastPositionMs)
    // Podcast is most recent -> sits at top (index 0)
    assertEquals(podcast.contentUri, history.first().track.contentUri)

    // Daily stats strictly include only today's 3 plays (firstId + other + thirdId) and 50_000ms, NOT the 3-day-old session
    val reportNowMs = System.currentTimeMillis()
    val allEvents = db.playHistoryDao().getAllHistorySnapshot()
    val dailyReport = com.example.data.analytics.UsageStatsEngine.aggregateUsageReport(allEvents, com.example.model.AnalyticsTimeframe.DAILY, reportNowMs)
    val weeklyReport = com.example.data.analytics.UsageStatsEngine.aggregateUsageReport(allEvents, com.example.model.AnalyticsTimeframe.WEEKLY, reportNowMs)
    assertEquals(3, dailyReport.totalPlaysThisWeek)
    assertEquals(50_000L, dailyReport.totalListenedMs)
    assertEquals(4, weeklyReport.totalPlaysThisWeek)
    assertEquals(170_000L, weeklyReport.totalListenedMs)

    // Restart from zero clears lastPositionMs to 0L on all matching rows for this file
    val restartId = repository.recordTrackPlay(podcast, resetPositionToZero = true)
    assertTrue(restartId > thirdId)
    val afterRestart = repository.observePlaybackHistory().first()
    assertEquals(2, afterRestart.size)
    assertEquals(0L, afterRestart.first { it.track.contentUri == podcast.contentUri }.lastPositionMs)
  }

  @Test
  fun `cosmic starfield animation renders exclusively for COSMIC_STARFIELD and energy orb renders interactively`() {
    val track = sampleTrack(1301L, "Cosmic Soundscape")
    composeTestRule.setContent {
      val themeState = remember { mutableStateOf(com.example.model.AppThemeStyle.COSMIC_STARFIELD) }
      MPlayerTheme(themeStyle = themeState.value, language = AppLanguage.AR) {
        com.example.ui.components.ThemedBackground {
          com.example.ui.components.DynamicPlayerVisualizer(
            mode = com.example.model.VisualizerMode.ENERGY_ORB,
            isPlaying = false,
            is8DEnabled = true,
            panAngleDegrees = 45f,
            amplitude = 0.65f,
            bassEnergy = 0.72f,
            waveformSamples = FloatArray(32) { 0.3f },
            spectrumBins = FloatArray(20) { 0.45f },
            albumArtUri = null,
            title = track.title,
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("cosmic_starfield_bg_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("visualizer_energy_orb_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("visualizer_3d_sphere_hint").assertDoesNotExist()
  }

  @Test
  fun `glass opacity slider value persists cleanly in DataStore`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = com.example.data.prefs.UserPreferencesRepository(context)

    prefsRepo.setAppGlassOpacity(0.25f)
    assertEquals(0.25f, prefsRepo.preferencesFlow.first().appGlassOpacity, 0.001f)

    prefsRepo.setAppGlassOpacity(0.55f)
    assertEquals(0.55f, prefsRepo.preferencesFlow.first().appGlassOpacity, 0.001f)
  }

  @Test
  fun `video seek step preference persists presets and clamps custom range in DataStore`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = com.example.data.prefs.UserPreferencesRepository(context)

    prefsRepo.setVideoSeekStepSeconds(5)
    assertEquals(5, prefsRepo.preferencesFlow.first().videoSeekStepSeconds)

    prefsRepo.setVideoSeekStepSeconds(120)
    assertEquals(120, prefsRepo.preferencesFlow.first().videoSeekStepSeconds)

    prefsRepo.setVideoSeekStepSeconds(0)
    assertEquals(1, prefsRepo.preferencesFlow.first().videoSeekStepSeconds)

    prefsRepo.setVideoSeekStepSeconds(100_000)
    assertEquals(86_400, prefsRepo.preferencesFlow.first().videoSeekStepSeconds)

    // Restore default
    prefsRepo.setVideoSeekStepSeconds(10)
    assertEquals(10, prefsRepo.preferencesFlow.first().videoSeekStepSeconds)
  }

  @Test
  fun `more screen video seek control shows presets and custom unit dialog updates step`() {
    var updatedSeekSeconds = 10
    val summary =
      com.example.model.WeeklySummaryReport(
        topTracks = emptyList(),
        totalPlaysThisWeek = 0,
        totalListeningMinutes = 0L,
        uniqueTracksPlayed = 0,
      )
    composeTestRule.setContent {
      val seekStepState = remember { mutableStateOf(10) }
      MPlayerTheme(
        themeStyle = com.example.model.AppThemeStyle.DEEP_BURGUNDY,
        language = AppLanguage.AR,
      ) {
        com.example.ui.screens.MoreScreen(
          preferences = com.example.data.prefs.UserPreferences(videoSeekStepSeconds = seekStepState.value),
          weeklySummary = summary,
          selectedTimeframe = com.example.model.AnalyticsTimeframe.WEEKLY,
          canDrawOverlays = true,
          onSelectTheme = {},
          onToggleDarkMode = {},
          onSelectLanguage = {},
          onSet8DEnabled = {},
          onSet8DSpeed = {},
          onSet8DIntensity = {},
          onOpen8DPanel = {},
          onOpenEqualizer = {},
          onOpenSleepTimer = {},
          onRequestOverlayPermission = {},
          onLaunchFloatingOverlay = {},
          onSelectTimeframe = {},
          onSetListeningTimeUnit = {},
          onSelectAutoSyncFile = {},
          onImportUsageStats = {},
          onPlayTop7Mix = {},
          onSetVideoSeekStepSeconds = { sec ->
            seekStepState.value = sec
            updatedSeekSeconds = sec
          },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("video_seek_step_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("التحكم في مقدار التمرير في الفيديو").assertIsDisplayed()
    composeTestRule.onNodeWithTag("video_seek_preset_5").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("video_seek_preset_10").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("video_seek_preset_15").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("video_seek_preset_30").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(30, updatedSeekSeconds)

    composeTestRule.onNodeWithTag("video_seek_custom_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("video_seek_custom_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("video_seek_unit_minutes").performClick()
    composeTestRule.onNodeWithTag("video_seek_custom_input").performTextReplacement("2")
    composeTestRule.onNodeWithTag("video_seek_custom_confirm_btn").performClick()
    composeTestRule.waitForIdle()

    assertEquals(120, updatedSeekSeconds)
    composeTestRule.onNodeWithTag("video_seek_custom_dialog").assertDoesNotExist()
  }

  @Test
  fun `fullscreen theater overlay supports double tap step seek and independent long press scrub`() {
    val videoTrack =
      sampleTrack(999L, "Gesture Cinema Clip").copy(
        contentUri = Uri.parse("content://media/external/video/media/999"),
        mimeType = "video/mp4",
        mediaType = com.example.model.MediaType.VIDEO,
      )
    val seekTargets = mutableListOf<Long>()
    composeTestRule.setContent {
      val positionState = remember { mutableStateOf(60_000L) }
      val playback =
        com.example.playback.PlaybackUiState(
          currentTrack = videoTrack,
          queue = listOf(videoTrack),
          currentIndex = 0,
          isPlaying = true,
          durationMs = 300_000L,
          positionMs = positionState.value,
          isVideoAudioOnly = false,
        )
      MPlayerTheme(themeStyle = com.example.model.AppThemeStyle.DEEP_BURGUNDY, language = AppLanguage.AR) {
        VideoTheaterOverlay(
          playback = playback,
          isLandscape = false,
          isLocked = false,
          seekStepMs = 15_000L,
          onToggleLandscape = {},
          onSetLocked = {},
          onSetVideoAudioOnlyMode = {},
          onPlayPause = {},
          onNext = {},
          onPrevious = {},
          onSeek = { target ->
            positionState.value = target
            seekTargets.add(target)
          },
          onToggleShuffle = {},
          onCycleRepeat = {},
          onAttachVideoTextureView = {},
          onDetachVideoTextureView = {},
          onExitTheater = {},
          autoHideDelayMs = 10_000L,
        )
      }
    }

    composeTestRule.waitForIdle()
    // 1. Double-tap right half -> +15s (60_000 -> 75_000)
    composeTestRule.onNodeWithTag("theater_tap_surface").performTouchInput {
      doubleClick(androidx.compose.ui.geometry.Offset(visibleSize.width * 0.8f, visibleSize.height * 0.5f))
    }
    composeTestRule.waitForIdle()
    assertEquals(75_000L, seekTargets.last())
    composeTestRule.onNodeWithTag("theater_double_tap_seek_badge").assertIsDisplayed()

    // Advance past doubleTap timeout cleanly before left double-tap
    composeTestRule.mainClock.advanceTimeBy(450L)
    composeTestRule.waitForIdle()

    // 2. Double-tap left half -> -15s (75_000 -> 60_000)
    composeTestRule.onNodeWithTag("theater_tap_surface").performTouchInput {
      doubleClick(androidx.compose.ui.geometry.Offset(visibleSize.width * 0.2f, visibleSize.height * 0.5f))
    }
    composeTestRule.waitForIdle()
    assertEquals(60_000L, seekTargets.last())

    // 3. Long-press + horizontal drag right -> shows continuous scrub overlay & advances independently
    composeTestRule.mainClock.advanceTimeBy(450L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("theater_tap_surface").performTouchInput {
      val start = androidx.compose.ui.geometry.Offset(visibleSize.width * 0.5f, visibleSize.height * 0.5f)
      down(start)
      advanceEventTime(750L)
      moveBy(androidx.compose.ui.geometry.Offset(180f, 0f))
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("theater_scrub_time_indicator").assertIsDisplayed()
    composeTestRule.onNodeWithTag("theater_tap_surface").performTouchInput { up() }
    composeTestRule.waitForIdle()
    assertTrue("Expected long-press scrub seek > 60_000ms, got ${seekTargets.last()}", seekTargets.last() > 60_000L)
  }

  @Test
  fun `history screen prompts resume only when lastPositionMs gt 0 and restarts cleanly from 0`() {
    val resumedTrack = sampleTrack(1401L, "Podcast Saved")
    val freshTrack = sampleTrack(1402L, "Song From Start")
    val savedItem =
      com.example.model.PlaybackHistoryItem(
        eventId = 10L,
        track = resumedTrack,
        playedAtMs = 1_700_000_000_000L,
        playedAtIso = "2026-11-20T01:00:00Z",
        listenedMs = 45_000L,
        lastPositionMs = 45_000L,
      )
    val zeroItem =
      com.example.model.PlaybackHistoryItem(
        eventId = 11L,
        track = freshTrack,
        playedAtMs = 1_700_000_060_000L,
        playedAtIso = "2026-11-20T01:05:00Z",
        listenedMs = 0L,
        lastPositionMs = 0L,
      )

    val playedCalls = mutableListOf<Pair<Long, Boolean>>()
    composeTestRule.setContent {
      MPlayerTheme(themeStyle = com.example.model.AppThemeStyle.DEEP_BURGUNDY, language = AppLanguage.AR) {
        com.example.ui.screens.HistoryScreen(
          historyItems = listOf(savedItem, zeroItem),
          favoriteUris = emptySet(),
          onPlayFromHistory = { item, resume -> playedCalls.add(item.eventId to resume) },
          onToggleFavorite = {},
          onDeleteItem = {},
          onClearAll = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    // 1. Clicking item with lastPositionMs == 0L plays directly without prompt
    composeTestRule.onNodeWithTag("history_item_11").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("resume_restart_dialog").assertDoesNotExist()
    assertEquals(listOf(11L to false), playedCalls)

    // 2. Clicking item with lastPositionMs > 0L shows resume/restart prompt
    composeTestRule.onNodeWithTag("history_item_10").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("resume_restart_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("history_restart_btn").performClick()
    composeTestRule.waitForIdle()
    assertEquals(listOf(11L to false, 10L to false), playedCalls)
  }

  @Test
  fun `cosmic 3d sphere cycle finishes uninterrupted after finger lift and requires fresh long press to repeat`() {
    val stateHolder = com.example.ui.components.CosmicStarfallState()
    composeTestRule.mainClock.autoAdvance = false
    try {
      composeTestRule.setContent {
        MPlayerTheme(themeStyle = com.example.model.AppThemeStyle.COSMIC_STARFIELD, language = AppLanguage.AR) {
          com.example.ui.components.DynamicPlayerVisualizer(
            mode = com.example.model.VisualizerMode.ENERGY_ORB,
            isPlaying = false,
            is8DEnabled = false,
            panAngleDegrees = 0f,
            amplitude = 0.3f,
            bassEnergy = 0.4f,
            waveformSamples = FloatArray(64) { 0f },
            spectrumBins = FloatArray(20) { 0.3f },
            albumArtUri = null,
            title = "Cosmic Test",
            cosmicStarfallState = stateHolder,
          )
        }
      }

      fun stepFrames(ms: Long) {
        val ticks = (ms / 100L).toInt().coerceAtLeast(1)
        repeat(ticks) {
          composeTestRule.mainClock.advanceTimeBy(100L)
        }
      }

      stepFrames(200L)
      assertEquals(com.example.ui.components.CosmicSpherePhase.IDLE, stateHolder.phase)

      // 1. Touch down and hold steadily for 7.2s without lifting or dragging -> enters SWELLING then BURST/STARFALL
      composeTestRule.onNodeWithTag("visualizer_energy_orb_canvas").performTouchInput {
        down(center)
      }
      stepFrames(1000L)
      assertEquals(com.example.ui.components.CosmicSpherePhase.SWELLING, stateHolder.phase)

      stepFrames(6300L)
      assertTrue(
        "Expected BURST or STARFALL after 7.3s steady press, was ${stateHolder.phase}",
        stateHolder.phase == com.example.ui.components.CosmicSpherePhase.BURST ||
          stateHolder.phase == com.example.ui.components.CosmicSpherePhase.STARFALL,
      )

      // 2. Lift finger mid-cycle! Animation MUST continue uninterrupted through STARFALL -> REMERGE -> IDLE
      composeTestRule.onNodeWithTag("visualizer_energy_orb_canvas").performTouchInput {
        up()
      }
      stepFrames(1500L)
      assertEquals(
        "STARFALL must continue running after finger release",
        com.example.ui.components.CosmicSpherePhase.STARFALL,
        stateHolder.phase,
      )

      // Advance through remainder of STARFALL into REMERGE
      stepFrames(6000L)
      assertEquals(
        "STARFALL should transition cleanly into REMERGE",
        com.example.ui.components.CosmicSpherePhase.REMERGE,
        stateHolder.phase,
      )

      // Advance through REMERGE to IDLE
      stepFrames(3200L)
      assertEquals(com.example.ui.components.CosmicSpherePhase.IDLE, stateHolder.phase)
    } finally {
      composeTestRule.mainClock.autoAdvance = true
    }
  }

  @Test
  fun `convert format dialog renders both mp4 and mp3 options clearly`() {
    val videoTrack =
      sampleTrack(1501L, "Sample Video Clip").copy(
        contentUri = Uri.parse("content://media/external/video/media/1501"),
        mimeType = "video/mp4",
        mediaType = com.example.model.MediaType.VIDEO,
      )
    var chosen: com.example.model.ConversionFormat? = null
    composeTestRule.setContent {
      MPlayerTheme(themeStyle = com.example.model.AppThemeStyle.ROYAL_PURPLE, language = AppLanguage.AR) {
        com.example.ui.components.ConvertFormatDialog(
          track = videoTrack,
          hasAuthorizedFolder = true,
          onAuthorizeFolder = {},
          onSelectFormat = { chosen = it },
          onDismiss = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("convert_format_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("convert_mp4_option_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("convert_mp3_option_btn").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(com.example.model.ConversionFormat.MP3_AUDIO, chosen)
  }

  @Test
  fun `video keep screen on activates while playing video and clears when paused or stopped`() {
    assertTrue(
      shouldKeepScreenAwakeForVideo(
        isShowingVideoSurface = true,
        isPlaying = true,
        isVideoTrack = true,
        isAudioOnly = false,
      )
    )
    assertTrue(
      !shouldKeepScreenAwakeForVideo(
        isShowingVideoSurface = true,
        isPlaying = false,
        isVideoTrack = true,
        isAudioOnly = false,
      )
    )
    assertTrue(
      !shouldKeepScreenAwakeForVideo(
        isShowingVideoSurface = true,
        isPlaying = true,
        isVideoTrack = true,
        isAudioOnly = true,
      )
    )

    val context = ApplicationProvider.getApplicationContext<Context>()
    val testView = android.view.View(context)
    val activity = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).get()
    val window = activity.window
    val isPlayingState = mutableStateOf(true)
    val isShowingVideoState = mutableStateOf(true)

    composeTestRule.setContent {
      VideoKeepScreenOnEffect(
        keepScreenAwake =
          shouldKeepScreenAwakeForVideo(
            isShowingVideoSurface = isShowingVideoState.value,
            isPlaying = isPlayingState.value,
            isVideoTrack = true,
            isAudioOnly = false,
          ),
        window = window,
        hostView = testView,
      )
    }

    composeTestRule.waitForIdle()
    assertTrue(testView.keepScreenOn)
    assertTrue(
      (window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
    )

    // Paused -> immediately releases screen wake lock
    isPlayingState.value = false
    composeTestRule.waitForIdle()
    assertTrue(!testView.keepScreenOn)
    assertTrue(
      (window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) == 0
    )

    // Resumed -> re-enables, then video stopped/closed -> releases again
    isPlayingState.value = true
    composeTestRule.waitForIdle()
    assertTrue(testView.keepScreenOn)
    assertTrue(
      (window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
    )

    isShowingVideoState.value = false
    composeTestRule.waitForIdle()
    assertTrue(!testView.keepScreenOn)
    assertTrue(
      (window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) == 0
    )
  }

  @Test
  fun `amplifier and volume boost settings persist and clamp cleanly in DataStore`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = com.example.data.prefs.UserPreferencesRepository(context)

    prefsRepo.setVolumeBoostPercent(285)
    prefsRepo.setAmplifierEnabled(true)
    prefsRepo.setAmplifierLevelPercent(82)

    val loaded = prefsRepo.preferencesFlow.first()
    assertEquals(285, loaded.volumeBoostPercent)
    assertTrue(loaded.amplifierEnabled)
    assertEquals(82, loaded.amplifierLevelPercent)

    prefsRepo.setVolumeBoostPercent(1200)
    prefsRepo.setAmplifierLevelPercent(-15)
    val clamped = prefsRepo.preferencesFlow.first()
    assertEquals(797, clamped.volumeBoostPercent)
    assertEquals(0, clamped.amplifierLevelPercent)

    // Restore defaults
    prefsRepo.setVolumeBoostPercent(100)
    prefsRepo.setAmplifierEnabled(false)
    prefsRepo.setAmplifierLevelPercent(50)
  }

  @Test
  fun `playlists screen create playlist dialog saves new playlist and updates list immediately`() = runTest {
    val createdNames = mutableListOf<String>()
    val playlistsState = mutableStateOf<List<com.example.model.PlaylistSummary>>(emptyList())

    composeTestRule.setContent {
      MPlayerTheme(themeStyle = com.example.model.AppThemeStyle.DEEP_BURGUNDY, language = AppLanguage.AR) {
        com.example.ui.screens.PlaylistsScreen(
          playlists = playlistsState.value,
          onCreatePlaylist = { name ->
            createdNames.add(name)
            val newId = (playlistsState.value.size + 1).toLong()
            playlistsState.value =
              listOf(
                com.example.model.PlaylistSummary(
                  id = newId,
                  name = name,
                  coverPath = null,
                  sortOption = com.example.model.PlaylistSortOption.MANUAL,
                  createdAtMs = System.currentTimeMillis(),
                  trackCount = 0,
                  totalDurationMs = 0L,
                  isFavorites = false,
                )
              ) + playlistsState.value
          },
          onOpenPlaylist = {},
          onDeletePlaylist = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("create_playlist_fab").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("playlist_name_input").performTextReplacement("  Gym Mix  ")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("confirm_create_playlist_btn").performClick()
    composeTestRule.waitForIdle()

    assertEquals(listOf("Gym Mix"), createdNames)
    composeTestRule.onNodeWithTag("playlists_list").assertIsDisplayed()
    composeTestRule.onNodeWithText("Gym Mix").assertIsDisplayed()

    // Also verify real Room repository persistence path
    val savedId = repository.createPlaylist("Gym Mix")
    val savedPlaylists = repository.observePlaylists().first()
    assertTrue(savedPlaylists.any { it.id == savedId && it.name == "Gym Mix" })
  }

  @Test
  fun `audio scanner extension matching and incremental sync handle new files quickly`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val scanner = AudioScanner(context)
    val tempRoot = kotlin.io.path.createTempDirectory("mplayer_fast_inc_test").toFile()
    try {
      val subDir = java.io.File(tempRoot, "Music").apply { mkdirs() }
      java.io.File(subDir, "UpperCaseSong.MP3").apply { writeBytes(ByteArray(512) { 3 }) }
      java.io.File(subDir, "ClipOne.MKV").apply { writeBytes(ByteArray(512) { 4 }) }

      val out = mutableListOf<AudioTrack>()
      val seenUris = HashSet<String>()
      val seenSigs = HashSet<String>()
      scanner.scanDirectoriesInternal(
        roots = listOf(tempRoot),
        includeAudio = true,
        includeVideo = true,
        out = out,
        seenUris = seenUris,
        seenFileSignatures = seenSigs,
      )
      assertEquals(2, out.size)

      // Add a new file and confirm incremental scan picks it up cleanly
      java.io.File(subDir, "BrandNew.flac").apply { writeBytes(ByteArray(512) { 5 }) }
      val out2 = mutableListOf<AudioTrack>()
      scanner.scanDirectoriesInternal(
        roots = listOf(tempRoot),
        includeAudio = true,
        includeVideo = true,
        out = out2,
        seenUris = HashSet(),
        seenFileSignatures = HashSet(),
      )
      assertEquals(3, out2.size)
      assertTrue(out2.any { it.title == "BrandNew" })
    } finally {
      tempRoot.deleteRecursively()
    }
  }

  @Test
  fun `floating glass bottom nav bar displays 5 tabs, distinct center player medallion, and updates selected tab across themes`() {
    val selectedTab = mutableStateOf(MainTab.LIBRARY)
    val themeState = mutableStateOf(com.example.model.AppThemeStyle.ROYAL_PURPLE)
    val opacityState = mutableStateOf(0.30f)
    composeTestRule.setContent {
      MPlayerTheme(
        themeStyle = themeState.value,
        language = AppLanguage.AR,
        glassOpacity = opacityState.value,
      ) {
        val strings = com.example.localization.LocalAppStrings.current
        com.example.ui.components.FloatingGlassBottomNavBar(
          selectedTab = selectedTab.value,
          tabs = MainTab.entries,
          labelForTab = { tab ->
            when (tab) {
              MainTab.LIBRARY -> strings.navLibrary
              MainTab.PLAYLISTS -> strings.navPlaylists
              MainTab.PLAYER -> strings.navPlayer
              MainTab.HISTORY -> strings.navHistory
              MainTab.MORE -> strings.navMore
            }
          },
          iconForTab = { _, selected ->
            if (selected) Icons.Rounded.LibraryMusic else Icons.Outlined.LibraryMusic
          },
          onSelectTab = { selectedTab.value = it },
          isPlaying = true,
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("floating_glass_nav_pill").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_center_player_medallion", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_library").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_playlists").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_player").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_history").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_more").assertIsDisplayed()

    composeTestRule.onNodeWithText("المكتبة").assertIsDisplayed()
    composeTestRule.onNodeWithText("القوائم").assertIsDisplayed()
    composeTestRule.onNodeWithText("المشغل").assertIsDisplayed()
    composeTestRule.onNodeWithText("السجل").assertIsDisplayed()
    composeTestRule.onNodeWithText("المزيد").assertIsDisplayed()

    // Switch to center Player tab and History tab
    composeTestRule.onNodeWithTag("nav_player").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(
      hasTestTag("nav_player").and(androidx.compose.ui.test.isSelected())
    ).assertIsDisplayed()

    // Switch across PITCH_BLACK_GOLD and PLAIN_BLACK with altered opacity and keep interaction intact
    composeTestRule.runOnIdle {
      opacityState.value = 0.85f
      themeState.value = com.example.model.AppThemeStyle.PITCH_BLACK_GOLD
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("floating_glass_nav_pill").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_center_player_medallion", useUnmergedTree = true).assertIsDisplayed()

    composeTestRule.runOnIdle {
      themeState.value = com.example.model.AppThemeStyle.PLAIN_BLACK
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_history").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNode(
      hasTestTag("nav_history").and(androidx.compose.ui.test.isSelected())
    ).assertIsDisplayed()
  }

  @Test
  fun `library locate current track button appears only when track active and jumps smoothly to playing card`() {
    val tracks = (1L..30L).map { sampleTrack(it, "Library Track $it") }
    val activeTrackIdState = mutableStateOf<Long?>(null)

    composeTestRule.setContent {
      MPlayerTheme(
        themeStyle = com.example.model.AppThemeStyle.DEEP_BURGUNDY,
        language = com.example.model.AppLanguage.AR,
      ) {
        LibraryScreen(
          tracks = tracks,
          folders = emptyList(),
          selectedFolder = null,
          searchQuery = "",
          currentSort = com.example.model.LibrarySortOption.TITLE_ASC,
          currentTrackId = activeTrackIdState.value,
          isPlaying = true,
          isScanning = false,
          hasAudioPermission = true,
          onRequestPermission = {},
          onOpenAppSettings = {},
          onRescan = {},
          onSearchQueryChange = {},
          onSelectFolder = {},
          onSelectSort = {},
          onTrackClick = {},
          onPlayAll = {},
          onShuffleAll = {},
          onAddToPlaylistClick = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    // Hidden when no track is active
    composeTestRule.onNodeWithTag("library_locate_current_track_btn").assertDoesNotExist()
    composeTestRule.onNodeWithTag("track_card_24").assertDoesNotExist()

    // Activate track #24 far down the list -> button appears and jumping brings track_card_24 into view
    composeTestRule.runOnIdle { activeTrackIdState.value = 24L }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("library_locate_current_track_btn").assertIsDisplayed().performClick()
    composeTestRule.waitUntil(timeoutMillis = 3_000L) {
      composeTestRule.onAllNodes(hasTestTag("track_card_24")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("track_card_24").assertIsDisplayed()
    // Scroll-to-top button is also visible alongside locate button once scrolled down
    composeTestRule.onNodeWithTag("library_scroll_to_top_btn").assertIsDisplayed()
  }

  @Test
  fun `opening player queue auto-centers list on current track with previous and upcoming context`() {
    val queueTracks = (0 until 25).map { idx -> sampleTrack(idx.toLong() + 1L, "Queue Song $idx") }
    val playback =
      com.example.playback.PlaybackUiState(
        queue = queueTracks,
        currentTrack = queueTracks[12],
        currentIndex = 12,
        isPlaying = true,
        durationMs = 180_000L,
        positionMs = 42_000L,
      )

    composeTestRule.setContent {
      MPlayerTheme(
        themeStyle = com.example.model.AppThemeStyle.PITCH_BLACK_GOLD,
        language = com.example.model.AppLanguage.AR,
      ) {
        NowPlayingScreen(
          playback = playback,
          sleepTimerState = com.example.model.SleepTimerState(),
          isEqEnabled = false,
          panAngleDegrees = 0f,
          audioAmplitude = 0f,
          bassEnergy = 0f,
          waveformSamples = FloatArray(32),
          spectrumBins = FloatArray(16),
          visualizerMode = com.example.model.VisualizerMode.ALBUM_ART,
          onSelectVisualizerMode = {},
          onPlayPause = {},
          onNext = {},
          onPrevious = {},
          onSeek = {},
          onToggleShuffle = {},
          onCycleRepeat = {},
          onOpenSleepTimer = {},
          onOpenEqualizer = {},
          onJumpToQueueIndex = {},
          onAddToPlaylistClick = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("player_queue_btn").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("player_queue_list").assertIsDisplayed()
    composeTestRule.waitUntil(timeoutMillis = 3_000L) {
      composeTestRule.onAllNodes(hasTestTag("queue_item_12")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("queue_item_11").assertIsDisplayed()
    composeTestRule.onNodeWithTag("queue_item_12").assertIsDisplayed()
    composeTestRule.onNodeWithTag("queue_item_13").assertIsDisplayed()
  }

  private fun sampleTrack(id: Long, title: String) =
    AudioTrack(
      id = id,
      title = title,
      artist = "Artist $id",
      album = "Album $id",
      albumId = id,
      durationMs = 180_000L,
      contentUri = Uri.parse("content://media/external/audio/media/$id"),
      albumArtUri = null,
      dateAddedMs = 1000L * id,
      dateModifiedMs = 1000L * id,
      sizeBytes = 4_000_000L,
      mimeType = "audio/mpeg",
      folderName = "Music",
    )
}
