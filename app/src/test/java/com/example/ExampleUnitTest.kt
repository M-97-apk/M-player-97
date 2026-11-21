package com.example

import com.example.playback.Audio8DEffectBus
import com.example.ui.components.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun formatDuration_formatsMinutesAndHours() {
    assertEquals("0:00", formatDuration(0L))
    assertEquals("3:05", formatDuration(185_000L))
    assertEquals("1:02:03", formatDuration(3_723_000L))
  }

  @Test
  fun audio8DEffectBus_togglesStateAndUpdatesSpeedAndIntensity() {
    Audio8DEffectBus.setEnabled(false)
    assertFalse(Audio8DEffectBus.isEnabled)
    Audio8DEffectBus.setEnabled(true)
    assertTrue(Audio8DEffectBus.isEnabled)

    Audio8DEffectBus.setSpeed(com.example.model.Audio8DSpeed.SLOW)
    assertEquals(com.example.model.Audio8DSpeed.SLOW, Audio8DEffectBus.speed)
    Audio8DEffectBus.setSpeed(com.example.model.Audio8DSpeed.FAST)
    assertEquals(com.example.model.Audio8DSpeed.FAST, Audio8DEffectBus.speed)
    Audio8DEffectBus.setSpeed(com.example.model.Audio8DSpeed.NORMAL)

    Audio8DEffectBus.setIntensityPercent(150)
    assertEquals(100, Audio8DEffectBus.intensityPercent)
    Audio8DEffectBus.setIntensityPercent(-20)
    assertEquals(0, Audio8DEffectBus.intensityPercent)
    Audio8DEffectBus.setIntensityPercent(80)
    assertEquals(80, Audio8DEffectBus.intensityPercent)

    Audio8DEffectBus.setEnabled(false)
  }

  @Test
  fun audioEqualizerBus_presetsAndClampingWorkProperly() {
    com.example.playback.AudioEqualizerBus.applyPreset(com.example.model.EqualizerPreset.BASS_BOOST)
    assertEquals(com.example.model.EqualizerPreset.BASS_BOOST, com.example.playback.AudioEqualizerBus.presetFlow.value)
    assertEquals(9f, com.example.playback.AudioEqualizerBus.bandGainsDb[0], 0.01f)

    com.example.playback.AudioEqualizerBus.setBandGain(0, 25f)
    assertEquals(12f, com.example.playback.AudioEqualizerBus.bandGainsDb[0], 0.01f)
    assertEquals(null, com.example.playback.AudioEqualizerBus.presetFlow.value)

    com.example.playback.AudioEqualizerBus.setBandGain(2, -30f)
    assertEquals(-12f, com.example.playback.AudioEqualizerBus.bandGainsDb[2], 0.01f)

    com.example.playback.AudioEqualizerBus.applyPreset(com.example.model.EqualizerPreset.FLAT)
    assertEquals(com.example.model.EqualizerPreset.FLAT, com.example.playback.AudioEqualizerBus.presetFlow.value)
  }

  @Test
  fun unifiedSevenThemes_resolvePalettesWhiteContrastAndGlassOpacity() {
    val entries = com.example.model.AppThemeStyle.entries
    assertEquals(7, entries.size)
    assertEquals(
      listOf(
        com.example.model.AppThemeStyle.DEEP_BURGUNDY,
        com.example.model.AppThemeStyle.ROYAL_PURPLE,
        com.example.model.AppThemeStyle.MIDNIGHT_BLUE,
        com.example.model.AppThemeStyle.PITCH_BLACK_GOLD,
        com.example.model.AppThemeStyle.LUXURY_DARK_BROWN,
        com.example.model.AppThemeStyle.COSMIC_STARFIELD,
        com.example.model.AppThemeStyle.PLAIN_BLACK,
      ),
      entries,
    )
    assertEquals(
      com.example.model.AppThemeStyle.DEEP_BURGUNDY,
      com.example.data.prefs.UserPreferences().themeStyle,
    )
    assertEquals(0.55f, com.example.data.prefs.UserPreferences().appGlassOpacity, 0.001f)

    val burgundy = com.example.ui.theme.resolvePalette(com.example.model.AppThemeStyle.DEEP_BURGUNDY, true)
    assertTrue(burgundy.isGlassTheme)
    assertFalse(burgundy.isMinimalPlainBlack)
    assertTrue(burgundy.navBarBackground.alpha < 0.75f)
    assertTrue(burgundy.cardSurface.alpha < 0.65f)
    assertEquals(com.example.ui.theme.DeepBurgundyBgTop, burgundy.backgroundTop)
    assertEquals(com.example.ui.theme.DeepBurgundyAccent, burgundy.accentPrimary)
    assertEquals(com.example.ui.theme.DeepBurgundyGlowHighlight, burgundy.accentGlow)
    // Zero gold/yellow bleed & zero pink bleed in Burgundy
    assertTrue(burgundy.accentGlow != com.example.ui.theme.MetallicGoldBright)
    assertTrue(burgundy.buttonContentColor != com.example.ui.theme.MetallicGoldBright)
    assertTrue(burgundy.backgroundTop.red < 0.10f && burgundy.backgroundTop.green < 0.03f && burgundy.backgroundTop.blue < 0.04f)
    assertTrue(burgundy.accentPrimary.green < 0.08f && burgundy.accentPrimary.blue < 0.12f)

    // Royal Purple & Midnight Blue helper labels and icons must be crisp white
    val royalPurple = com.example.ui.theme.resolvePalette(com.example.model.AppThemeStyle.ROYAL_PURPLE, true)
    assertTrue(royalPurple.isGlassTheme)
    assertEquals(androidx.compose.ui.graphics.Color.White, royalPurple.textSecondary)
    assertEquals(androidx.compose.ui.graphics.Color.White, royalPurple.helperIconColor)
    assertEquals(com.example.ui.theme.RoyalPurpleBgTop, royalPurple.backgroundTop)
    assertEquals(com.example.ui.theme.RoyalPurpleBgBottom, royalPurple.backgroundBottom)
    assertEquals(com.example.ui.theme.RoyalPurpleAccent, royalPurple.accentPrimary)

    val midnightBlue = com.example.ui.theme.resolvePalette(com.example.model.AppThemeStyle.MIDNIGHT_BLUE, true)
    assertTrue(midnightBlue.isGlassTheme)
    assertEquals(androidx.compose.ui.graphics.Color.White, midnightBlue.textSecondary)
    assertEquals(androidx.compose.ui.graphics.Color.White, midnightBlue.helperIconColor)
    assertEquals(com.example.ui.theme.MidnightBlueBgTop, midnightBlue.backgroundTop)
    assertEquals(com.example.ui.theme.MidnightBlueAccent, midnightBlue.accentPrimary)

    // Gold/Black must remain strictly untouched
    val blackGold = com.example.ui.theme.resolvePalette(com.example.model.AppThemeStyle.PITCH_BLACK_GOLD, true)
    assertTrue(blackGold.isGlassTheme)
    assertFalse(blackGold.isMinimalPlainBlack)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), blackGold.backgroundTop)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), blackGold.backgroundBottom)
    assertEquals(androidx.compose.ui.graphics.Color.Transparent, blackGold.glowOrbPrimary)
    assertEquals(androidx.compose.ui.graphics.Color.Transparent, blackGold.glowOrbSecondary)
    assertEquals(androidx.compose.ui.graphics.Color.Transparent, blackGold.activeOptionGlowColor)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFB8962E), blackGold.accentPrimary)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFD4AF37), blackGold.accentGlow)

    val luxuryBrown = com.example.ui.theme.resolvePalette(com.example.model.AppThemeStyle.LUXURY_DARK_BROWN, true)
    assertTrue(luxuryBrown.isGlassTheme)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF140A05), luxuryBrown.backgroundTop)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF26130A), luxuryBrown.backgroundBottom)
    assertEquals(com.example.ui.theme.LuxuryBrownAccent, luxuryBrown.accentPrimary)
    assertEquals(com.example.ui.theme.LuxuryBrownGlowHighlight, luxuryBrown.accentGlow)
    assertTrue(luxuryBrown.accentGlow != com.example.ui.theme.MetallicGoldBright)

    // 6th Cosmic Starfield theme: pure black night sky + crisp starlight
    val cosmic = com.example.ui.theme.resolvePalette(com.example.model.AppThemeStyle.COSMIC_STARFIELD, true)
    assertTrue(cosmic.isGlassTheme)
    assertFalse(cosmic.isMinimalPlainBlack)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), cosmic.backgroundTop)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), cosmic.backgroundBottom)
    assertEquals(androidx.compose.ui.graphics.Color.White, cosmic.helperIconColor)

    // 7th Plain Black theme: pure #000000 with crisp white icons & text and zero extra ornamentation
    val plainBlack = com.example.ui.theme.resolvePalette(com.example.model.AppThemeStyle.PLAIN_BLACK, true)
    assertFalse(plainBlack.isGlassTheme)
    assertTrue(plainBlack.isMinimalPlainBlack)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), plainBlack.backgroundTop)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), plainBlack.backgroundBottom)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), plainBlack.cardSurface)
    assertEquals(androidx.compose.ui.graphics.Color.White, plainBlack.textPrimary)
    assertEquals(androidx.compose.ui.graphics.Color.White, plainBlack.textSecondary)
    assertEquals(androidx.compose.ui.graphics.Color.White, plainBlack.helperIconColor)
    assertEquals(androidx.compose.ui.graphics.Color.White, plainBlack.buttonBorderColor)
    assertEquals(androidx.compose.ui.graphics.Color.Transparent, plainBlack.glowOrbPrimary)
    assertEquals(androidx.compose.ui.graphics.Color.Transparent, plainBlack.activeOptionGlowColor)

    // App-wide glass opacity slider scales card surface alpha smoothly
    val lowGlass = com.example.ui.theme.resolvePalette(com.example.model.AppThemeStyle.MIDNIGHT_BLUE, true, glassOpacity = 0.20f)
    val highGlass = com.example.ui.theme.resolvePalette(com.example.model.AppThemeStyle.MIDNIGHT_BLUE, true, glassOpacity = 0.90f)
    assertTrue("Low opacity alpha (${lowGlass.cardSurface.alpha}) should be less than high (${highGlass.cardSurface.alpha})", lowGlass.cardSurface.alpha < highGlass.cardSurface.alpha)

    val brownWidgetSpec =
      com.example.overlay.resolveFloatingWidgetSpec(
        appThemeStyle = com.example.model.AppThemeStyle.LUXURY_DARK_BROWN,
        colorMode = com.example.model.FloatingWidgetColorMode.MATCH_APP,
        glassMode = com.example.model.FloatingWidgetGlassMode.FROSTED_BLUR,
      )
    assertEquals(com.example.model.FloatingWidgetColorMode.LUXURY_DARK_BROWN, brownWidgetSpec.effectiveColorMode)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF140A05), brownWidgetSpec.topGradientColor)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF26130A), brownWidgetSpec.baseSurfaceColor)

    val en = com.example.localization.stringsForLanguage(com.example.model.AppLanguage.EN)
    val ar = com.example.localization.stringsForLanguage(com.example.model.AppLanguage.AR)
    assertEquals("Extra-Deep Burgundy", en.themeDeepBurgundyName)
    assertEquals("نبيتي غامق جداً", ar.themeDeepBurgundyName)
    assertEquals("Royal Purple", en.themeRoyalPurpleName)
    assertEquals("بنفسجي ملكي", ar.themeRoyalPurpleName)
    assertEquals("Deep Midnight Blue", en.themeMidnightBlueName)
    assertEquals("أزرق ليلي داكن", ar.themeMidnightBlueName)
    assertEquals("Pitch-Black & Gold", en.themePitchBlackGoldName)
    assertEquals("أسود داكن وذهبي معدني", ar.themePitchBlackGoldName)
    assertEquals("Luxury Dark Brown", en.themeLuxuryBrownName)
    assertEquals("البني الفاخر", ar.themeLuxuryBrownName)
    assertEquals("Cosmic Starfield", en.themeCosmicStarfieldName)
    assertEquals("سماء النجوم الكونية", ar.themeCosmicStarfieldName)
    assertEquals("Plain Black", en.themePlainBlackName)
    assertEquals("أسود سادة", ar.themePlainBlackName)
    assertEquals("Transparency", en.appGlassOpacityTitle)
    assertEquals("الشفافية", ar.appGlassOpacityTitle)
  }

  @Test
  fun realtimeFftAnalyzer_distinguishesBassVsTrebleAndGatesSilence() {
    val sr = 44100
    val analyzer = com.example.playback.RealtimeFftSpectrumAnalyzer(sr)

    // 1. Silence produces zero energy across all metrics
    var lastFrame: com.example.playback.VisualizerFrame? = null
    for (n in 0 until 2048) {
      val f = analyzer.pushSample(0f, n * 1000.0 / sr)
      if (f != null) lastFrame = f
    }
    val silentFrame = lastFrame!!
    assertEquals(0f, silentFrame.rms, 0.001f)
    assertEquals(0f, silentFrame.bass, 0.001f)
    assertTrue(silentFrame.spectrum.all { it == 0f })

    // 2. Pure 85 Hz sub-bass tone excites low bars (0..4) much more than high bars (14..19)
    analyzer.reset()
    var bassFrame: com.example.playback.VisualizerFrame? = null
    for (n in 0 until 4096) {
      val sample = 0.65f * kotlin.math.sin(2.0 * kotlin.math.PI * 85.0 * n / sr).toFloat()
      val f = analyzer.pushSample(sample, n * 1000.0 / sr)
      if (f != null) bassFrame = f
    }
    val bassOut = bassFrame!!
    val lowEnergy85Hz = bassOut.spectrum.slice(0..4).max()
    val highEnergy85Hz = bassOut.spectrum.slice(14..19).max()
    assertTrue("85Hz low energy=$lowEnergy85Hz should exceed high=$highEnergy85Hz", lowEnergy85Hz > 0.35f && lowEnergy85Hz > highEnergy85Hz * 3f)
    assertTrue("85Hz bass pulse should be strong: ${bassOut.bass}", bassOut.bass > 0.25f)

    // 3. Pure 5000 Hz treble tone excites upper bars (13..18) much more than bass bars (0..4)
    analyzer.reset()
    var trebleFrame: com.example.playback.VisualizerFrame? = null
    for (n in 0 until 4096) {
      val sample = 0.65f * kotlin.math.sin(2.0 * kotlin.math.PI * 5000.0 * n / sr).toFloat()
      val f = analyzer.pushSample(sample, n * 1000.0 / sr)
      if (f != null) trebleFrame = f
    }
    val trebleOut = trebleFrame!!
    val lowEnergy5kHz = trebleOut.spectrum.slice(0..4).max()
    val highEnergy5kHz = trebleOut.spectrum.slice(13..18).max()
    assertTrue("5kHz high energy=$highEnergy5kHz should exceed low=$lowEnergy5kHz", highEnergy5kHz > 0.35f && highEnergy5kHz > lowEnergy5kHz * 3f)
  }

  @Test
  fun audioVisualizerBus_synchronizesQueuedFramesToPlayhead() {
    val bus = com.example.playback.AudioVisualizerBus
    bus.reset()
    assertEquals(0f, bus.amplitude.value, 0.001f)

    val frame1 =
      com.example.playback.VisualizerFrame(
        audioTimeMs = 20.0,
        rms = 0.42f,
        bass = 0.61f,
        waveform = FloatArray(com.example.playback.AudioVisualizerBus.WAVEFORM_POINTS) { 0.25f },
        spectrum = FloatArray(com.example.playback.AudioVisualizerBus.SPECTRUM_BINS) { 0.5f },
      )
    bus.enqueueFrame(frame1, 20.0)
    bus.onAudioPlayoutStarted()
    bus.advancePlayheadAndPublish(deltaWallMs = 25.0, playbackSpeed = 1.0f)

    assertEquals(0.42f, bus.amplitude.value, 0.001f)
    assertEquals(0.61f, bus.bassEnergy.value, 0.001f)
    assertEquals(0.5f, bus.spectrumBins.value[0], 0.001f)

    bus.reset()
    assertEquals(0f, bus.amplitude.value, 0.001f)
    assertEquals(0f, bus.bassEnergy.value, 0.001f)
  }

  @Test
  fun computeSeekTargetMs_handlesForwardRewindAndUnknownDuration() {
    // +10s advances forward
    assertEquals(25_000L, com.example.playback.computeSeekTargetMs(15_000L, 180_000L, 10_000L))
    // +10s clamps to track duration
    assertEquals(180_000L, com.example.playback.computeSeekTargetMs(175_000L, 180_000L, 10_000L))
    // -10s rewinds backward
    assertEquals(5_000L, com.example.playback.computeSeekTargetMs(15_000L, 180_000L, -10_000L))
    // -10s clamps to 0ms at start of track
    assertEquals(0L, com.example.playback.computeSeekTargetMs(4_000L, 180_000L, -10_000L))
    // Unknown duration (0L) still allows +10s forward instead of clamping back
    assertEquals(15_000L, com.example.playback.computeSeekTargetMs(5_000L, 0L, 10_000L))
    assertEquals(0L, com.example.playback.computeSeekTargetMs(5_000L, 0L, -10_000L))
  }

  @Test
  fun formatRealListeningStat_displaysAccurateSecondsAndMinutesModes() {
    val secUnit = com.example.model.ListeningTimeUnit.SECONDS
    val minUnit = com.example.model.ListeningTimeUnit.MINUTES

    assertEquals("0s", com.example.ui.screens.formatRealListeningStat(0L, secUnit, "s", "m"))
    assertEquals("42s", com.example.ui.screens.formatRealListeningStat(42_000L, secUnit, "s", "m"))
    assertEquals("300s", com.example.ui.screens.formatRealListeningStat(300_000L, secUnit, "s", "m"))
    assertEquals("4500s", com.example.ui.screens.formatRealListeningStat(4_500_000L, secUnit, "s", "m"))

    assertEquals("0m", com.example.ui.screens.formatRealListeningStat(0L, minUnit, "s", "m"))
    assertEquals("0.7m", com.example.ui.screens.formatRealListeningStat(42_000L, minUnit, "s", "m"))
    assertEquals("5m", com.example.ui.screens.formatRealListeningStat(300_000L, minUnit, "s", "m"))
    assertEquals("75m", com.example.ui.screens.formatRealListeningStat(4_500_000L, minUnit, "s", "m"))

    assertEquals("42 ث", com.example.ui.screens.formatRealListeningStat(42_000L, secUnit, " ث", " د"))
    assertEquals("5 د", com.example.ui.screens.formatRealListeningStat(300_000L, minUnit, " ث", " د"))
  }

  @Test
  fun insightsStrings_omitFillerWordsAndIncludeUnitToggleAndImportLabels() {
    val en = com.example.localization.stringsForLanguage(com.example.model.AppLanguage.EN)
    val ar = com.example.localization.stringsForLanguage(com.example.model.AppLanguage.AR)

    assertFalse(ar.weeklySummarySubtitle.contains("حقيقي"))
    assertFalse(ar.emptyWeeklySubtitle.contains("حقيقي"))
    assertFalse(ar.insightsSubtitle.contains("حقيقي"))
    assertFalse(ar.usageSyncActive.contains("حقيقي"))

    assertFalse(en.weeklySummarySubtitle.contains("Real-time", ignoreCase = true))
    assertFalse(en.insightsSubtitle.contains("Real-time", ignoreCase = true))

    assertEquals("Seconds", en.listeningUnitSecondsLabel)
    assertEquals("Minutes", en.listeningUnitMinutesLabel)
    assertEquals("ثواني", ar.listeningUnitSecondsLabel)
    assertEquals("دقائق", ar.listeningUnitMinutesLabel)
    assertEquals("Import Stats", en.usageImportButton)
    assertEquals("استيراد السجل", ar.usageImportButton)
  }

  @Test
  fun floatingWidgetSpec_decouplesThemeColorFromGlassOpacityAndBlur() {
    val glassModes = com.example.model.FloatingWidgetGlassMode.entries
    assertEquals(4, glassModes.size)

    // 1. Changing Glass Opacity & Blur never changes resolved Theme Color
    val baseColors =
      glassModes.map { glass ->
        com.example.overlay.resolveFloatingWidgetSpec(
          appThemeStyle = com.example.model.AppThemeStyle.DEEP_BURGUNDY,
          colorMode = com.example.model.FloatingWidgetColorMode.MATCH_APP,
          glassMode = glass,
        )
      }
    assertTrue(baseColors.all { it.effectiveColorMode == com.example.model.FloatingWidgetColorMode.DEEP_BURGUNDY })
    assertTrue(baseColors.all { it.borderColor == baseColors.first().borderColor })
    assertTrue(baseColors.all { it.accentIndicatorColor == baseColors.first().accentIndicatorColor })
    assertTrue(baseColors.all { !it.isNeutral })

    // Verify expected opacity & blur progression
    assertEquals(true, baseColors[0].enableWindowBlur) // FROSTED_BLUR
    assertEquals(42, baseColors[0].blurBehindRadiusPx)
    assertEquals(true, baseColors[1].enableWindowBlur) // SEMI_FROSTED
    assertEquals(18, baseColors[1].blurBehindRadiusPx)
    assertTrue(baseColors[1].surfaceAlpha < baseColors[0].surfaceAlpha)
    assertEquals(false, baseColors[2].enableWindowBlur) // OPAQUE
    assertEquals(0, baseColors[2].blurBehindRadiusPx)
    assertTrue(baseColors[2].surfaceAlpha > 0.90f)
    assertEquals(false, baseColors[3].enableWindowBlur) // FULLY_TRANSPARENT
    assertEquals(0, baseColors[3].blurBehindRadiusPx)
    assertEquals(0.0f, baseColors[3].surfaceAlpha, 0.001f)

    // 2. Changing Widget Color Mode (Match App vs Neutral vs Explicit) never alters Glass Mode or Transparency
    val transparentSpecs =
      com.example.model.FloatingWidgetColorMode.entries.map { colorMode ->
        com.example.overlay.resolveFloatingWidgetSpec(
          appThemeStyle = com.example.model.AppThemeStyle.ROYAL_PURPLE,
          colorMode = colorMode,
          glassMode = com.example.model.FloatingWidgetGlassMode.FULLY_TRANSPARENT,
        )
      }
    assertTrue(transparentSpecs.all { it.glassMode == com.example.model.FloatingWidgetGlassMode.FULLY_TRANSPARENT })
    assertTrue(transparentSpecs.all { !it.enableWindowBlur && it.blurBehindRadiusPx == 0 })
    assertTrue(transparentSpecs.all { it.surfaceAlpha == 0.0f })

    val neutral =
      com.example.overlay.resolveFloatingWidgetSpec(
        appThemeStyle = com.example.model.AppThemeStyle.DEEP_BURGUNDY,
        colorMode = com.example.model.FloatingWidgetColorMode.NEUTRAL,
        glassMode = com.example.model.FloatingWidgetGlassMode.OPAQUE,
      )
    assertTrue(neutral.isNeutral)
    assertEquals(false, neutral.isPitchBlackGold)
    assertEquals(androidx.compose.ui.graphics.Color.White, neutral.accentIndicatorColor)
    assertEquals(false, neutral.enableWindowBlur)
    assertTrue(neutral.surfaceAlpha > 0.90f)
  }

  @Test
  fun floatingWidgetAndPalette_enforceStrictBorderOnlyMetallicGoldOnPitchBlack() {
    val palette =
      com.example.ui.theme.resolvePalette(com.example.model.AppThemeStyle.PITCH_BLACK_GOLD, true)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF0A0A0A), palette.buttonContainerColor)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFD4AF37), palette.buttonBorderColor)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFD4AF37), palette.buttonContentColor)

    val widgetSpec =
      com.example.overlay.resolveFloatingWidgetSpec(
        appThemeStyle = com.example.model.AppThemeStyle.PITCH_BLACK_GOLD,
        colorMode = com.example.model.FloatingWidgetColorMode.MATCH_APP,
        glassMode = com.example.model.FloatingWidgetGlassMode.FROSTED_BLUR,
      )
    assertTrue(widgetSpec.isPitchBlackGold)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), widgetSpec.baseSurfaceColor)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF0A0A0A), widgetSpec.elevatedSurfaceColor)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF0A0A0A), widgetSpec.topGradientColor)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFD4AF37), widgetSpec.borderColor)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFD4AF37), widgetSpec.accentIndicatorColor)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFD4AF37), widgetSpec.primaryButtonIconTint)
  }

  @Test
  fun playlistBackupEngine_addOnlyMerge_preservesDeletedPlaylistsAndAppendsNewTracks() {
    val track1 =
      com.example.data.analytics.BackupPlaylistTrack(
        trackId = 1L,
        title = "Track One",
        artist = "Artist A",
        album = "Album A",
        albumId = 1L,
        durationMs = 180_000L,
        contentUri = "content://audio/1",
        albumArtUri = null,
        dateAddedDeviceMs = 100L,
        dateModifiedDeviceMs = 100L,
        sizeBytes = 1024L,
        mimeType = "audio/mpeg",
        folderName = "Music",
        addedToPlaylistAtMs = 1000L,
        manualOrder = 0,
      )
    val track2 = track1.copy(trackId = 2L, title = "Track Two", contentUri = "content://audio/2", manualOrder = 1)

    val archived =
      listOf(
        com.example.data.analytics.BackupPlaylist(
          name = "Deleted In App",
          createdAtMs = 5000L,
          tracks = listOf(track1),
        ),
        com.example.data.analytics.BackupPlaylist(
          name = "Chill Mix",
          createdAtMs = 6000L,
          tracks = listOf(track1),
        ),
      )

    // App now only has "Chill Mix" (with track2 added) and a brand new "Gym" playlist.
    // "Deleted In App" is absent from current in-app playlists.
    val incoming =
      listOf(
        com.example.data.analytics.BackupPlaylist(
          name = "Chill Mix",
          createdAtMs = 6000L,
          tracks = listOf(track2),
        ),
        com.example.data.analytics.BackupPlaylist(
          name = "Gym",
          createdAtMs = 7000L,
          tracks = listOf(track2),
        ),
      )

    val merged = com.example.data.analytics.PlaylistBackupEngine.mergePlaylistsAddOnly(archived, incoming)
    assertEquals(3, merged.size)
    assertTrue(merged.any { it.name == "Deleted In App" && it.tracks.size == 1 })
    val chill = merged.first { it.name == "Chill Mix" }
    assertEquals(listOf("Track One", "Track Two"), chill.tracks.map { it.title })
    assertTrue(merged.any { it.name == "Gym" && it.tracks.size == 1 })
  }

  @Test
  fun pureMp3Encoder_producesValidId3AndMpeg1Layer3Frames() {
    val sampleRate = 44100
    val channels = 2
    val totalSamples = sampleRate / 5 // 200ms
    val pcmBytes = ByteArray(totalSamples * channels * 2)
    val buffer =
      java.nio.ByteBuffer.wrap(pcmBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    for (n in 0 until totalSamples) {
      val s =
        (kotlin.math.sin(2.0 * kotlin.math.PI * 440.0 * n / sampleRate) * 16000.0)
          .toInt()
          .toShort()
      buffer.putShort(s)
      buffer.putShort(s)
    }

    val out = java.io.ByteArrayOutputStream()
    val encoder =
      com.example.data.media.PureMp3Encoder(
        inputSampleRate = sampleRate,
        inputChannels = channels,
        output = out,
        title = "Test Tone",
        artist = "M player 97",
        album = "Converted Audio",
      )
    encoder.feedPcm16(pcmBytes, pcmBytes.size)
    encoder.finish()

    val mp3 = out.toByteArray()
    assertTrue("Expected multiple MP3 frames written, got ${encoder.framesWritten}", encoder.framesWritten >= 5)
    assertTrue(mp3.size > 4000)
    assertEquals('I'.code.toByte(), mp3[0])
    assertEquals('D'.code.toByte(), mp3[1])
    assertEquals('3'.code.toByte(), mp3[2])

    // Find MPEG-1 Layer III frame sync (0xFF 0xFB / 0xFA) after ID3v2 header
    var syncCount = 0
    for (i in 10 until mp3.size - 1) {
      if ((mp3[i].toInt() and 0xFF) == 0xFF && (mp3[i + 1].toInt() and 0xF6) == 0xF2) {
        syncCount++
      }
    }
    assertTrue("Expected multiple valid MPEG-1 Layer III frame syncs, found $syncCount", syncCount >= 3)
  }

  @Test
  fun pureMp3Encoder_feedPcmFloat_resamplesAndProducesCleanMp3Frames() {
    val sampleRate = 48000
    val channels = 2
    val totalSamples = sampleRate / 4 // 250ms at 48kHz
    val floatBytes = ByteArray(totalSamples * channels * 4)
    val buffer =
      java.nio.ByteBuffer.wrap(floatBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    for (n in 0 until totalSamples) {
      val s = kotlin.math.sin(2.0 * kotlin.math.PI * 523.25 * n / sampleRate).toFloat() * 0.7f
      buffer.putFloat(s)
      buffer.putFloat(s)
    }

    val out = java.io.ByteArrayOutputStream()
    val encoder =
      com.example.data.media.PureMp3Encoder(
        inputSampleRate = sampleRate,
        inputChannels = channels,
        output = out,
        title = "Float Resampled Tone",
        artist = "M player 97",
        album = "Converted Audio",
      )
    encoder.feedPcmFloat(floatBytes, floatBytes.size)
    encoder.finish()

    val mp3 = out.toByteArray()
    assertTrue("Expected multiple MP3 frames from float PCM, got ${encoder.framesWritten}", encoder.framesWritten >= 6)
    assertTrue(mp3.size > 5000)
    assertEquals('I'.code.toByte(), mp3[0])
    assertEquals('D'.code.toByte(), mp3[1])
    assertEquals('3'.code.toByte(), mp3[2])

    var syncCount = 0
    for (i in 10 until mp3.size - 1) {
      if ((mp3[i].toInt() and 0xFF) == 0xFF && (mp3[i + 1].toInt() and 0xF6) == 0xF2) {
        syncCount++
      }
    }
    assertTrue("Expected MPEG-1 Layer III frame syncs in float-encoded MP3, found $syncCount", syncCount >= 3)
  }

  @Test
  fun multimediaUpgradeStrings_and_enumsAreConsistent() {
    assertEquals(2, com.example.model.VideoPlaybackMode.entries.size)
    assertEquals(2, com.example.model.ConversionFormat.entries.size)
    assertEquals(3, com.example.model.LibraryMediaFilter.entries.size)

    val en = com.example.localization.stringsForLanguage(com.example.model.AppLanguage.EN)
    val ar = com.example.localization.stringsForLanguage(com.example.model.AppLanguage.AR)

    assertEquals("History", en.navHistory)
    assertEquals("السجل", ar.navHistory)
    assertEquals("Liked", en.favoritesPlaylistName)
    assertEquals("المفضلة", ar.favoritesPlaylistName)
    assertTrue(en.convertMp3AudioOnly.contains(".mp3"))
    assertTrue(en.convertFullVideo.contains(".mp4"))
    assertTrue(ar.convertMp3AudioOnly.contains("MP3", ignoreCase = true))
    assertTrue(ar.convertFullVideo.contains("MP4", ignoreCase = true))
    assertFalse(ar.historyTitle.contains("حقيقي"))
    assertFalse(ar.defaultVideoModeSubtitle.contains("حقيقي"))
    assertFalse(ar.converterFolderAuthorized.contains("حقيقي"))
  }

  @Test
  fun visualizerModes_excludeWaveform_and_externalAndVideoControlsStringsExist() {
    assertEquals(
      listOf(
        com.example.model.VisualizerMode.ALBUM_ART,
        com.example.model.VisualizerMode.SPECTRUM_BARS,
        com.example.model.VisualizerMode.PULSING_CIRCLE,
        com.example.model.VisualizerMode.ENERGY_ORB,
      ),
      com.example.model.VisualizerMode.entries,
    )

    val en = com.example.localization.stringsForLanguage(com.example.model.AppLanguage.EN)
    val ar = com.example.localization.stringsForLanguage(com.example.model.AppLanguage.AR)

    assertEquals("Normal Video", en.externalPlayNormal)
    assertEquals("Audio Only", en.externalPlayAudioOnly)
    assertEquals("فيديو عادي", ar.externalPlayNormal)
    assertEquals("صوت فقط", ar.externalPlayAudioOnly)
    assertEquals("فيديو عادي", ar.videoModeNormalLabel)
    assertEquals("صوت فقط", ar.videoModeAudioOnlyLabel)
    assertEquals("فيديو عادي", ar.videoDisplayModePill)
    assertEquals("صوت فقط", ar.audioOnlyModePill)
    assertTrue(en.videoRotateLandscape.isNotBlank() && ar.videoRotateLandscape.isNotBlank())
    assertTrue(en.videoBrightness.isNotBlank() && ar.videoBrightness.isNotBlank())
    assertTrue(en.videoScreenLock.isNotBlank() && ar.videoScreenLock.isNotBlank())
    assertEquals("Fullscreen / Expand", en.videoFullscreenExpand)
    assertEquals("تكبير / ملء الشاشة", ar.videoFullscreenExpand)
    assertTrue(en.videoFullscreenExit.isNotBlank() && ar.videoFullscreenExit.isNotBlank())
    assertEquals(97, com.example.data.prefs.UserPreferences().audio8DIntensity)
  }

  @Test
  fun floatingWidgetDualTransparencyControls_and_arabicSemiTransparentLabelAreAccurate() {
    val ar = com.example.localization.stringsForLanguage(com.example.model.AppLanguage.AR)
    val en = com.example.localization.stringsForLanguage(com.example.model.AppLanguage.EN)
    assertEquals("شبه شفاف", ar.widgetGlassSemiFrosted)
    assertTrue(en.widgetTransparencySliderLabel.isNotBlank())
    assertTrue(ar.widgetTransparencySliderLabel.isNotBlank())

    // Continuous slider values scale opacity, frost grain, and window blur smoothly while preserving color decoupling
    val clear =
      com.example.overlay.resolveFloatingWidgetSpec(
        appThemeStyle = com.example.model.AppThemeStyle.DEEP_BURGUNDY,
        colorMode = com.example.model.FloatingWidgetColorMode.PITCH_BLACK_GOLD,
        glassMode = com.example.model.FloatingWidgetGlassMode.FROSTED_BLUR,
        customOpacity = 0.0f,
      )
    val lightFrost =
      com.example.overlay.resolveFloatingWidgetSpec(
        appThemeStyle = com.example.model.AppThemeStyle.DEEP_BURGUNDY,
        colorMode = com.example.model.FloatingWidgetColorMode.PITCH_BLACK_GOLD,
        glassMode = com.example.model.FloatingWidgetGlassMode.FROSTED_BLUR,
        customOpacity = 0.30f,
      )
    val richFrost =
      com.example.overlay.resolveFloatingWidgetSpec(
        appThemeStyle = com.example.model.AppThemeStyle.DEEP_BURGUNDY,
        colorMode = com.example.model.FloatingWidgetColorMode.PITCH_BLACK_GOLD,
        glassMode = com.example.model.FloatingWidgetGlassMode.FROSTED_BLUR,
        customOpacity = 0.65f,
      )
    val solid =
      com.example.overlay.resolveFloatingWidgetSpec(
        appThemeStyle = com.example.model.AppThemeStyle.DEEP_BURGUNDY,
        colorMode = com.example.model.FloatingWidgetColorMode.PITCH_BLACK_GOLD,
        glassMode = com.example.model.FloatingWidgetGlassMode.FROSTED_BLUR,
        customOpacity = 1.0f,
      )

    assertEquals(0.0f, clear.surfaceAlpha, 0.001f)
    assertFalse(clear.enableWindowBlur)
    assertEquals(0, clear.blurBehindRadiusPx)

    assertTrue(lightFrost.enableWindowBlur)
    assertTrue(lightFrost.frostGrainAlpha > 0f)
    assertTrue(lightFrost.surfaceAlpha < richFrost.surfaceAlpha)
    assertTrue(lightFrost.blurBehindRadiusPx < richFrost.blurBehindRadiusPx)

    assertTrue(solid.surfaceAlpha > 0.95f)
    assertFalse(solid.enableWindowBlur)
    assertEquals(0, solid.blurBehindRadiusPx)

    // Strict Pitch-Black & Metallic Gold preserved across all slider positions
    listOf(clear, lightFrost, richFrost, solid).forEach { spec ->
      assertTrue(spec.isPitchBlackGold)
      assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), spec.baseSurfaceColor)
      assertEquals(androidx.compose.ui.graphics.Color(0xFFD4AF37), spec.borderColor)
    }
  }

  @Test
  fun volumeBoostBus_clamps0To797_and_softLimiterAmplifiesQuietPcmCleanly() {
    com.example.playback.VolumeBoostBus.setBoostPercent(100)
    assertEquals(100, com.example.playback.VolumeBoostBus.boostPercent)

    com.example.playback.VolumeBoostBus.setBoostPercent(999)
    assertEquals(797, com.example.playback.VolumeBoostBus.boostPercent)

    com.example.playback.VolumeBoostBus.setBoostPercent(-50)
    assertEquals(0, com.example.playback.VolumeBoostBus.boostPercent)

    val processor = com.example.playback.Audio8DEffectProcessor()
    val format =
      androidx.media3.common.audio.AudioProcessor.AudioFormat(
        44100,
        2,
        androidx.media3.common.C.ENCODING_PCM_16BIT,
      )
    processor.configure(format)
    processor.flush()

    // Soft limiter keeps high gain strictly bounded inside [-1f, 1f] cleanly
    assertTrue(processor.softLimitPcm(0.5f) == 0.5f)
    val limitedPeak = processor.softLimitPcm(6.5f)
    assertTrue("Expected soft-limited peak in (0.82, 0.99], got $limitedPeak", limitedPeak > 0.82f && limitedPeak <= 0.985f)
    assertEquals(-limitedPeak, processor.softLimitPcm(-6.5f), 0.0001f)

    // Feed quiet PCM wave at 100% vs 600%: boosted RMS must be noticeably louder without overflow clipping
    fun processSineRms(boostPct: Int, amp: Double): Pair<Double, Int> {
      com.example.playback.VolumeBoostBus.setBoostPercent(boostPct)
      processor.flush()
      val frames = 1024
      val inBuf =
        java.nio.ByteBuffer.allocateDirect(frames * 4).order(java.nio.ByteOrder.nativeOrder())
      for (i in 0 until frames) {
        val sample = (kotlin.math.sin(2.0 * kotlin.math.PI * 440.0 * i / 44100.0) * amp * 32767.0).toInt().toShort()
        inBuf.putShort(sample)
        inBuf.putShort(sample)
      }
      inBuf.flip()
      processor.queueInput(inBuf)
      val outBuf = processor.output.order(java.nio.ByteOrder.nativeOrder())
      var sumSq = 0.0
      var maxAbs = 0
      var count = 0
      while (outBuf.remaining() >= 2) {
        val s = outBuf.short.toInt()
        val absVal = kotlin.math.abs(s)
        if (absVal > maxAbs) maxAbs = absVal
        sumSq += s.toDouble() * s.toDouble()
        count++
      }
      return kotlin.math.sqrt(sumSq / count.coerceAtLeast(1)) to maxAbs
    }

    val (rmsNormal, _) = processSineRms(100, 0.12)
    val (rmsBoosted, maxAbsBoosted) = processSineRms(600, 0.12)
    assertTrue("600% boost RMS ($rmsBoosted) should exceed 100% RMS ($rmsNormal)", rmsBoosted > rmsNormal * 2.5)
    assertTrue("Soft limiter should keep peak <= 32500 without wrap/clip, got $maxAbsBoosted", maxAbsBoosted <= 32500)

    com.example.playback.VolumeBoostBus.setBoostPercent(100)
  }

  @Test
  fun audioOutputRouteBus_and_bilingualBoostStrings_areAccurate() {
    val en = com.example.localization.stringsForLanguage(com.example.model.AppLanguage.EN)
    val ar = com.example.localization.stringsForLanguage(com.example.model.AppLanguage.AR)

    assertEquals("Volume Boost", en.volumeBoost)
    assertEquals("رفع الصوت المعزز", ar.volumeBoost)
    assertEquals("Ignore", en.volumeBoostIgnore)
    assertEquals("تجاهل", ar.volumeBoostIgnore)
    assertTrue(en.volumeBoostWarningTitle.contains("397"))
    assertTrue(ar.volumeBoostWarningTitle.contains("397"))

    val btRoute = com.example.model.AudioOutputRoute(com.example.model.AudioOutputRouteType.BLUETOOTH, "Galaxy Buds")
    com.example.playback.AudioOutputRouteBus.updateRoute(btRoute)
    assertEquals(btRoute, com.example.playback.AudioOutputRouteBus.currentRoute)
    com.example.playback.AudioOutputRouteBus.updateRoute(com.example.model.AudioOutputRoute())
  }

  @Test
  fun computeAutoResumePositionMs_enforcesThresholdsAndEndGuard() {
    // Short audio (< 5 minutes) always starts at 0:00
    assertEquals(
      0L,
      com.example.data.repository.computeAutoResumePositionMs(
        durationMs = 210_000L,
        isVideo = false,
        savedPositionMs = 45_000L,
      ),
    )

    // Long audio (>= 5 minutes) resumes when >= 5s and before last 8s
    assertEquals(
      125_000L,
      com.example.data.repository.computeAutoResumePositionMs(
        durationMs = 600_000L,
        isVideo = false,
        savedPositionMs = 125_000L,
      ),
    )

    // Video (>= 60 seconds) resumes accurately
    assertEquals(
      42_000L,
      com.example.data.repository.computeAutoResumePositionMs(
        durationMs = 120_000L,
        isVideo = true,
        savedPositionMs = 42_000L,
      ),
    )

    // Positions < 5s or near end (>= duration - 8s) reset cleanly to 0
    assertEquals(
      0L,
      com.example.data.repository.computeAutoResumePositionMs(
        durationMs = 120_000L,
        isVideo = true,
        savedPositionMs = 3_500L,
      ),
    )
    assertEquals(
      0L,
      com.example.data.repository.computeAutoResumePositionMs(
        durationMs = 120_000L,
        isVideo = true,
        savedPositionMs = 115_000L,
      ),
    )
  }

  @Test
  fun resolvePalette_enforcesPureWhiteHelpersInPurpleAndBlueOnly_andPreservesGoldBurgundyBrown() {
    val purple = com.example.ui.theme.resolvePalette(com.example.model.AppThemeStyle.ROYAL_PURPLE)
    val blue = com.example.ui.theme.resolvePalette(com.example.model.AppThemeStyle.MIDNIGHT_BLUE)
    val gold = com.example.ui.theme.resolvePalette(com.example.model.AppThemeStyle.PITCH_BLACK_GOLD)
    val burgundy = com.example.ui.theme.resolvePalette(com.example.model.AppThemeStyle.DEEP_BURGUNDY)
    val brown = com.example.ui.theme.resolvePalette(com.example.model.AppThemeStyle.LUXURY_DARK_BROWN)

    // Purple & Blue must have pure white (#FFFFFF) helper icons, helper text, and secondary captions
    listOf(purple, blue).forEach { palette ->
      assertEquals(androidx.compose.ui.graphics.Color.White, palette.helperIconColor)
      assertEquals(androidx.compose.ui.graphics.Color.White, palette.sectionHelperIconColor)
      assertEquals(androidx.compose.ui.graphics.Color.White, palette.sectionHelperTextColor)
      assertEquals(androidx.compose.ui.graphics.Color.White, palette.textSecondary)
    }

    // Gold/Black, Burgundy, and Brown remain untouched
    assertEquals(androidx.compose.ui.graphics.Color(0xFFD4AF37), gold.accentGlow)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFD4AF37), gold.sectionHelperIconColor)
    assertEquals(0.55f, gold.appGlassOpacity, 0.001f)
    assertEquals(com.example.ui.theme.DeepBurgundyGlowHighlight, burgundy.sectionHelperIconColor)
    assertEquals(com.example.ui.theme.LuxuryBrownGlowHighlight, brown.sectionHelperIconColor)
  }

  @Test
  fun dspBoostReleaseAcross100Percent_remainsBoundedWithoutDiscontinuity() {
    val processor = com.example.playback.Audio8DEffectProcessor()
    val format =
      androidx.media3.common.audio.AudioProcessor.AudioFormat(
        44100,
        2,
        androidx.media3.common.C.ENCODING_PCM_16BIT,
      )
    processor.configure(format)
    processor.flush()

    // First run at 600% boost to charge compGainSmoothed > 1.0
    com.example.playback.VolumeBoostBus.setBoostPercent(600)
    val frames = 256
    fun feedAndReadMax(): Int {
      val buf =
        java.nio.ByteBuffer.allocateDirect(frames * 4).order(java.nio.ByteOrder.nativeOrder())
      for (i in 0 until frames) {
        val sample = (kotlin.math.sin(2.0 * kotlin.math.PI * 440.0 * i / 44100.0) * 0.05 * 32767.0).toInt().toShort()
        buf.putShort(sample)
        buf.putShort(sample)
      }
      buf.flip()
      processor.queueInput(buf)
      val out = processor.output.order(java.nio.ByteOrder.nativeOrder())
      var maxAbs = 0
      while (out.remaining() >= 2) {
        maxAbs = maxOf(maxAbs, kotlin.math.abs(out.short.toInt()))
      }
      return maxAbs
    }

    val peakBoosted = feedAndReadMax()
    assertTrue("Expected noticeable amplification at 600%, got $peakBoosted", peakBoosted > 3000)

    // Now drop back to 100%: smoothing tracks down cleanly without integer overflow
    com.example.playback.VolumeBoostBus.setBoostPercent(100)
    val peakAfterDrop1 = feedAndReadMax()
    val peakAfterDrop2 = feedAndReadMax()
    assertTrue("Expected smooth decay toward unity gain ($peakAfterDrop1 -> $peakAfterDrop2)", peakAfterDrop2 <= peakAfterDrop1)

    com.example.playback.VolumeBoostBus.setBoostPercent(100)
  }

  @Test
  fun hardwareVolumeMath_mapsStepsAndPreventsDoubleAttenuation() {
    assertEquals(0, com.example.playback.HardwareVolumeMath.hardwareStepToPercent(0, 15))
    assertEquals(100, com.example.playback.HardwareVolumeMath.hardwareStepToPercent(15, 15))
    assertEquals(53, com.example.playback.HardwareVolumeMath.hardwareStepToPercent(8, 15))

    assertEquals(0, com.example.playback.HardwareVolumeMath.percentToHardwareStep(0, 15))
    assertEquals(1, com.example.playback.HardwareVolumeMath.percentToHardwareStep(2, 15))
    assertEquals(15, com.example.playback.HardwareVolumeMath.percentToHardwareStep(100, 15))
    assertEquals(15, com.example.playback.HardwareVolumeMath.percentToHardwareStep(450, 15))

    // Fractional vernier trim compensates integer hardware steps without double attenuation
    val stepFor50 = com.example.playback.HardwareVolumeMath.percentToHardwareStep(50, 15) // 8 (53.3%)
    val trim = com.example.playback.HardwareVolumeMath.computePcmTrimForStandardPercent(50, stepFor50, 15)
    val effectiveRatio = (stepFor50.toFloat() / 15f) * trim
    assertEquals(0.50f, effectiveRatio, 0.01f)
    assertEquals(1f, com.example.playback.HardwareVolumeMath.computePcmTrimForStandardPercent(100, 15, 15), 0.001f)
    assertEquals(1f, com.example.playback.HardwareVolumeMath.computePcmTrimForStandardPercent(300, 15, 15), 0.001f)
  }

  @Test
  fun audioAmplifierBus_amplifiesPcmIndependentlyOfVolumeBoost() {
    com.example.playback.VolumeBoostBus.setBoostPercent(100)
    com.example.playback.AudioAmplifierBus.setEnabled(false)
    com.example.playback.AudioAmplifierBus.setLevelPercent(100)
    assertEquals(0f, com.example.playback.AudioAmplifierBus.computeGainDb(), 0.001f)
    assertEquals(1f, com.example.playback.AudioAmplifierBus.computeGainLinear(), 0.001f)

    com.example.playback.AudioAmplifierBus.setEnabled(true)
    assertEquals(97f, com.example.playback.AudioAmplifierBus.computeGainDb(), 0.01f)
    assertTrue(com.example.playback.AudioAmplifierBus.computeGainLinear() > 70_000f)
    assertEquals(0f, com.example.playback.AudioAmplifierBus.computeCombinedDspGain(0, 1f, true, 100), 0.001f)

    // Volume Boost 250% combined with Amplifier produces higher DSP gain than standard 100%
    val baseAmpOnly = com.example.playback.AudioAmplifierBus.computeCombinedDspGain(100, 1f, true, 50)
    val boostedPlusAmp = com.example.playback.AudioAmplifierBus.computeCombinedDspGain(250, 1f, true, 50)
    assertTrue("Combined Volume Boost + Amplifier ($boostedPlusAmp) should exceed Amplifier-only ($baseAmpOnly)", boostedPlusAmp > baseAmpOnly)

    val processor = com.example.playback.Audio8DEffectProcessor()
    val format =
      androidx.media3.common.audio.AudioProcessor.AudioFormat(
        44100,
        2,
        androidx.media3.common.C.ENCODING_PCM_16BIT,
      )
    processor.configure(format)

    fun runSampleRms(ampEnabled: Boolean): Pair<Double, Int> {
      com.example.playback.AudioAmplifierBus.setEnabled(ampEnabled)
      com.example.playback.AudioAmplifierBus.setLevelPercent(100)
      processor.flush()
      val frames = 512
      val inBuf =
        java.nio.ByteBuffer.allocateDirect(frames * 4).order(java.nio.ByteOrder.nativeOrder())
      for (i in 0 until frames) {
        val sample = (kotlin.math.sin(2.0 * kotlin.math.PI * 440.0 * i / 44100.0) * 0.08 * 32767.0).toInt().toShort()
        inBuf.putShort(sample)
        inBuf.putShort(sample)
      }
      inBuf.flip()
      processor.queueInput(inBuf)
      val outBuf = processor.output.order(java.nio.ByteOrder.nativeOrder())
      var sumSq = 0.0
      var maxAbs = 0
      var count = 0
      while (outBuf.remaining() >= 2) {
        val s = kotlin.math.abs(outBuf.short.toInt())
        if (s > maxAbs) maxAbs = s
        sumSq += s.toDouble() * s.toDouble()
        count++
      }
      return kotlin.math.sqrt(sumSq / count.coerceAtLeast(1)) to maxAbs
    }

    val (rmsOff, _) = runSampleRms(false)
    val (rmsOn, maxAbsOn) = runSampleRms(true)
    assertTrue("Amplifier ON RMS ($rmsOn) should substantially exceed OFF ($rmsOff)", rmsOn > rmsOff * 2.5)
    assertTrue("Amplifier output peak ($maxAbsOn) must stay soft-limited <= 32500", maxAbsOn <= 32500)

    com.example.playback.AudioAmplifierBus.setEnabled(false)
    com.example.playback.AudioAmplifierBus.setLevelPercent(50)
    com.example.playback.VolumeBoostBus.setBoostPercent(100)
  }
}
