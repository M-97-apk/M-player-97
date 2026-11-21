package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.analytics.UsageStatsEngine
import com.example.data.prefs.UserPreferences
import com.example.localization.LocalAppStrings
import com.example.model.AnalyticsTimeframe
import com.example.model.AppLanguage
import com.example.model.AppThemeStyle
import com.example.model.Audio8DSpeed
import com.example.model.ListeningTimeUnit
import com.example.model.VideoPlaybackMode
import com.example.model.VideoSeekUnit
import com.example.model.WeeklySummaryReport
import com.example.ui.components.ArtworkThumbnail
import com.example.ui.components.Audio8DIntensitySlider
import com.example.ui.components.Audio8DSpeedSelector
import com.example.ui.components.ThemedCard
import com.example.ui.components.ThemedIconButton
import com.example.ui.components.ThemedPrimaryButton
import com.example.ui.components.activeGlassOptionPillDecoration
import com.example.ui.theme.CosmicStarfieldGlowHighlight
import com.example.ui.theme.DeepBurgundyAccent
import com.example.ui.theme.DeepBurgundyBgBottom
import com.example.ui.theme.DeepBurgundyBgTop
import com.example.ui.theme.DeepBurgundyGlowHighlight
import com.example.ui.theme.LocalMPlayerPalette
import com.example.ui.theme.LuxuryBrownAccent
import com.example.ui.theme.LuxuryBrownBgBottom
import com.example.ui.theme.LuxuryBrownBgTop
import com.example.ui.theme.LuxuryBrownGlowHighlight
import com.example.ui.theme.MetallicGoldBright
import com.example.ui.theme.MidnightBlueAccent
import com.example.ui.theme.MidnightBlueBgBottom
import com.example.ui.theme.MidnightBlueBgTop
import com.example.ui.theme.MidnightBlueGlowHighlight
import com.example.ui.theme.PitchBlackBgBottom
import com.example.ui.theme.PitchBlackBgTop
import com.example.ui.theme.RoyalPurpleAccent
import com.example.ui.theme.RoyalPurpleBgBottom
import com.example.ui.theme.RoyalPurpleBgTop
import com.example.ui.theme.RoyalPurpleGlowHighlight
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoreScreen(
  preferences: UserPreferences,
  weeklySummary: WeeklySummaryReport,
  selectedTimeframe: AnalyticsTimeframe,
  canDrawOverlays: Boolean,
  onSelectTheme: (AppThemeStyle) -> Unit,
  onToggleDarkMode: (Boolean) -> Unit,
  onSelectLanguage: (AppLanguage) -> Unit,
  onSet8DEnabled: (Boolean) -> Unit,
  onSet8DSpeed: (Audio8DSpeed) -> Unit,
  onSet8DIntensity: (Int) -> Unit,
  onOpen8DPanel: () -> Unit,
  onOpenEqualizer: () -> Unit,
  onOpenSleepTimer: () -> Unit,
  onRequestOverlayPermission: () -> Unit,
  onLaunchFloatingOverlay: () -> Unit,
  onSelectTimeframe: (AnalyticsTimeframe) -> Unit,
  onSetListeningTimeUnit: (ListeningTimeUnit) -> Unit,
  onSelectAutoSyncFile: () -> Unit,
  onImportUsageStats: () -> Unit,
  onPlayTop7Mix: () -> Unit,
  hasConverterFolder: Boolean = false,
  onSetDefaultVideoPlaybackMode: (VideoPlaybackMode) -> Unit = {},
  onSelectConverterFolder: () -> Unit = {},
  onSetAppGlassOpacity: (Float) -> Unit = {},
  onSetVideoSeekStepSeconds: (Int) -> Unit = {},
  bottomOverlayPadding: androidx.compose.ui.unit.Dp = 160.dp,
  modifier: Modifier = Modifier,
) {
  var showFullWeeklyReport by remember { mutableStateOf(false) }
  var showCustomSeekDialog by remember { mutableStateOf(false) }

  if (showFullWeeklyReport) {
    WeeklySummaryScreen(
      report = weeklySummary,
      selectedTimeframe = selectedTimeframe,
      preferences = preferences,
      onBack = { showFullWeeklyReport = false },
      onSelectTimeframe = onSelectTimeframe,
      onSetListeningTimeUnit = onSetListeningTimeUnit,
      onPlayTop7Mix = onPlayTop7Mix,
      bottomOverlayPadding = bottomOverlayPadding,
      modifier = modifier,
    )
    return
  }

  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 14.dp)
  ) {
    Text(
      text = strings.moreTitle,
      style = MaterialTheme.typography.headlineMedium,
      color = palette.textPrimary,
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Top Theme Switcher — 7 Theme Circles (Burgundy, Purple, Blue, Gold/Black, Brown, Cosmic Starfield, Plain Black)
    SectionHeader(icon = Icons.Rounded.Palette, title = strings.themesTitle)
    Spacer(modifier = Modifier.height(10.dp))

    ThemedCard(cornerRadius = 22.dp, modifier = Modifier.fillMaxWidth()) {
      FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ThemeSwatchCircle(
          contentDescription = strings.themeDeepBurgundyName,
          selected = preferences.themeStyle == AppThemeStyle.DEEP_BURGUNDY,
          swatchColors = listOf(DeepBurgundyAccent, DeepBurgundyBgTop, DeepBurgundyBgBottom),
          swatchBorderColor = DeepBurgundyGlowHighlight,
          onClick = { onSelectTheme(AppThemeStyle.DEEP_BURGUNDY) },
          modifier = Modifier.testTag("theme_deep_burgundy_card"),
        )
        ThemeSwatchCircle(
          contentDescription = strings.themeRoyalPurpleName,
          selected = preferences.themeStyle == AppThemeStyle.ROYAL_PURPLE,
          swatchColors = listOf(RoyalPurpleAccent, RoyalPurpleBgTop, RoyalPurpleBgBottom),
          swatchBorderColor = RoyalPurpleGlowHighlight,
          onClick = { onSelectTheme(AppThemeStyle.ROYAL_PURPLE) },
          modifier = Modifier.testTag("theme_royal_purple_card"),
        )
        ThemeSwatchCircle(
          contentDescription = strings.themeMidnightBlueName,
          selected = preferences.themeStyle == AppThemeStyle.MIDNIGHT_BLUE,
          swatchColors = listOf(MidnightBlueAccent, MidnightBlueBgTop, MidnightBlueBgBottom),
          swatchBorderColor = MidnightBlueGlowHighlight,
          onClick = { onSelectTheme(AppThemeStyle.MIDNIGHT_BLUE) },
          modifier = Modifier.testTag("theme_midnight_blue_card"),
        )
        ThemeSwatchCircle(
          contentDescription = strings.themePitchBlackGoldName,
          selected = preferences.themeStyle == AppThemeStyle.PITCH_BLACK_GOLD,
          swatchColors = listOf(Color(0xFF0A0A0A), PitchBlackBgTop, PitchBlackBgBottom),
          swatchBorderColor = MetallicGoldBright,
          onClick = { onSelectTheme(AppThemeStyle.PITCH_BLACK_GOLD) },
          modifier = Modifier.testTag("theme_pitch_black_gold_card"),
        )
        ThemeSwatchCircle(
          contentDescription = strings.themeLuxuryBrownName,
          selected = preferences.themeStyle == AppThemeStyle.LUXURY_DARK_BROWN,
          swatchColors = listOf(LuxuryBrownBgTop, LuxuryBrownAccent, LuxuryBrownBgBottom),
          swatchBorderColor = LuxuryBrownGlowHighlight,
          onClick = { onSelectTheme(AppThemeStyle.LUXURY_DARK_BROWN) },
          modifier = Modifier.testTag("theme_luxury_dark_brown_card"),
        )
        CosmicStarfieldSwatchCircle(
          contentDescription = strings.themeCosmicStarfieldName,
          selected = preferences.themeStyle == AppThemeStyle.COSMIC_STARFIELD,
          swatchBorderColor = CosmicStarfieldGlowHighlight,
          onClick = { onSelectTheme(AppThemeStyle.COSMIC_STARFIELD) },
          modifier = Modifier.testTag("theme_cosmic_starfield_card"),
        )
        ThemeSwatchCircle(
          contentDescription = strings.themePlainBlackName,
          selected = preferences.themeStyle == AppThemeStyle.PLAIN_BLACK,
          swatchColors = listOf(Color.Black, Color.Black),
          swatchBorderColor = Color.White,
          onClick = { onSelectTheme(AppThemeStyle.PLAIN_BLACK) },
          modifier = Modifier.testTag("theme_plain_black_card"),
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // App-wide Glass Transparency Slider (controls frosted glass opacity cleanly; Gold/Black default stays 55%)
    ThemedCard(
      cornerRadius = 20.dp,
      modifier = Modifier.fillMaxWidth().testTag("app_glass_opacity_card"),
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = Icons.Rounded.BlurOn,
              contentDescription = strings.appGlassOpacityTitle,
              tint = palette.sectionHelperIconColor,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = strings.appGlassOpacityTitle,
                style = MaterialTheme.typography.titleSmall,
                color = palette.sectionHelperTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = strings.appGlassOpacitySubtitle,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${(preferences.appGlassOpacity * 100f).roundToInt()}%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.accentGlow,
            modifier = Modifier.testTag("app_glass_opacity_value"),
          )
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
          Slider(
            value = preferences.appGlassOpacity.coerceIn(0.10f, 1.0f),
            valueRange = 0.10f..1.0f,
            onValueChange = { value -> onSetAppGlassOpacity(value.coerceIn(0.10f, 1.0f)) },
            colors =
              SliderDefaults.colors(
                thumbColor = palette.accentGlow,
                activeTrackColor = palette.accentGlow,
                inactiveTrackColor = palette.textSecondary.copy(alpha = 0.24f),
              ),
            modifier = Modifier.fillMaxWidth().testTag("app_glass_opacity_slider"),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Listening Insights Card (Today / Weekly / Monthly / Yearly + Seconds/Minutes Toggle + Auto JSON Sync + Import)
    ThemedCard(
      cornerRadius = 24.dp,
      elevated = true,
      modifier = Modifier.fillMaxWidth().testTag("open_weekly_summary_card"),
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable { showFullWeeklyReport = true }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.BarChart,
              contentDescription = strings.insightsTitle,
              tint = palette.sectionHelperIconColor,
              modifier = Modifier.size(26.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = strings.insightsTitle,
                style = MaterialTheme.typography.titleMedium,
                color = palette.sectionHelperTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = strings.insightsSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
              contentDescription = null,
              tint = palette.sectionHelperIconColor,
            )
          }

          ListeningUnitGlassToggle(
            selectedUnit = preferences.listeningTimeUnit,
            onSelectUnit = onSetListeningTimeUnit,
          )
        }

        Spacer(modifier = Modifier.height(14.dp))
        AnalyticsTimeframeFilterRow(
          selectedTimeframe = selectedTimeframe,
          onSelectTimeframe = onSelectTimeframe,
        )

        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          WeeklyStatPill(
            value = "${weeklySummary.totalPlaysThisWeek}",
            label = strings.totalPlaysLabel,
            modifier = Modifier.weight(1f),
          )
          WeeklyStatPill(
            value =
              formatRealListeningStat(
                totalListenedMs = weeklySummary.totalListenedMs,
                unit = preferences.listeningTimeUnit,
                secondsSuffix = strings.listeningUnitSecondsShort,
                minutesSuffix = strings.listeningUnitMinutesShort,
              ),
            label = strings.listeningMinutesLabel,
            modifier = Modifier.weight(1f),
          )
          WeeklyStatPill(
            value = "${weeklySummary.uniqueTracksPlayed}",
            label = strings.uniqueSongsLabel,
            modifier = Modifier.weight(1f),
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        UsageFileSyncBanner(
          preferences = preferences,
          onSelectAutoSyncFileClick = onSelectAutoSyncFile,
          onImportClick = onImportUsageStats,
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Language Selection (English & Arabic)
    SectionHeader(icon = Icons.Rounded.Language, title = strings.languageTitle)
    Spacer(modifier = Modifier.height(10.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      LanguageOptionChip(
        label = strings.englishLabel,
        selected = preferences.language == AppLanguage.EN,
        onClick = { onSelectLanguage(AppLanguage.EN) },
        modifier = Modifier.weight(1f).testTag("lang_en_btn"),
      )
      LanguageOptionChip(
        label = strings.arabicLabel,
        selected = preferences.language == AppLanguage.AR,
        onClick = { onSelectLanguage(AppLanguage.AR) },
        modifier = Modifier.weight(1f).testTag("lang_ar_btn"),
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Default Video Playback & Export Folder ("M player 97")
    SectionHeader(icon = Icons.Rounded.Videocam, title = strings.defaultVideoModeTitle)
    Spacer(modifier = Modifier.height(10.dp))
    ThemedCard(cornerRadius = 22.dp, modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
          text = strings.defaultVideoModeSubtitle,
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          LanguageOptionChip(
            label = strings.videoModeNormalLabel,
            selected = preferences.defaultVideoPlaybackMode == VideoPlaybackMode.VIDEO_AND_AUDIO,
            onClick = { onSetDefaultVideoPlaybackMode(VideoPlaybackMode.VIDEO_AND_AUDIO) },
            modifier = Modifier.weight(1f).testTag("video_mode_normal_btn"),
          )
          LanguageOptionChip(
            label = strings.videoModeAudioOnlyLabel,
            selected = preferences.defaultVideoPlaybackMode == VideoPlaybackMode.AUDIO_ONLY,
            onClick = { onSetDefaultVideoPlaybackMode(VideoPlaybackMode.AUDIO_ONLY) },
            modifier = Modifier.weight(1f).testTag("video_mode_audio_only_btn"),
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(palette.glassBadgeFillBrush)
              .border(0.9.dp, palette.borderBrush, RoundedCornerShape(12.dp))
              .padding(horizontal = 10.dp, vertical = 8.dp)
              .testTag("converter_folder_compact_row"),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = if (hasConverterFolder) Icons.Rounded.CheckCircle else Icons.Rounded.Folder,
            contentDescription = strings.converterFolderTitle,
            tint = palette.sectionHelperIconColor,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = strings.converterFolderTitle,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = palette.sectionHelperTextColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text =
                if (hasConverterFolder) strings.converterFolderAuthorized
                else strings.converterFolderNeeded,
              style = MaterialTheme.typography.labelSmall,
              color = palette.textSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          ThemedPrimaryButton(
            onClick = onSelectConverterFolder,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            modifier = Modifier.height(34.dp).testTag("more_select_converter_folder_btn"),
          ) {
            Text(
              text = strings.converterFolderAuthorizeBtn,
              style = MaterialTheme.typography.labelSmall,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Video Double-Tap Seek Step Control ("التحكم في مقدار التمرير في الفيديو")
    SectionHeader(icon = Icons.Rounded.FastForward, title = strings.videoSeekControlTitle)
    Spacer(modifier = Modifier.height(10.dp))
    ThemedCard(
      cornerRadius = 22.dp,
      modifier = Modifier.fillMaxWidth().testTag("video_seek_step_card"),
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = strings.videoSeekControlSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
            modifier = Modifier.weight(1f),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .background(palette.glassBadgeActiveFillBrush)
                .border(0.9.dp, palette.buttonBorderColor, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("video_seek_active_badge"),
          ) {
            Text(
              text = strings.videoSeekPresetFormat(preferences.videoSeekStepSeconds),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = palette.accentGlow,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
        val presets = listOf(5, 10, 15, 30)
        val isCustomSelected = preferences.videoSeekStepSeconds !in presets
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          presets.forEach { sec ->
            val selected = preferences.videoSeekStepSeconds == sec
            val shape = RoundedCornerShape(12.dp)
            Box(
              modifier =
                Modifier.activeGlassOptionPillDecoration(selected = selected, shape = shape)
                  .then(
                    if (!selected) {
                      Modifier.clip(shape)
                        .background(palette.glassBadgeFillBrush)
                        .border(0.9.dp, palette.borderBrush, shape)
                    } else Modifier
                  )
                  .clickable { onSetVideoSeekStepSeconds(sec) }
                  .padding(horizontal = 14.dp, vertical = 10.dp)
                  .testTag("video_seek_preset_$sec"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = strings.videoSeekPresetFormat(sec),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) palette.accentGlow else palette.textPrimary,
                maxLines = 1,
                softWrap = false,
              )
            }
          }
          val customShape = RoundedCornerShape(12.dp)
          Box(
            modifier =
              Modifier.activeGlassOptionPillDecoration(selected = isCustomSelected, shape = customShape)
                .then(
                  if (!isCustomSelected) {
                    Modifier.clip(customShape)
                      .background(palette.glassBadgeFillBrush)
                      .border(0.9.dp, palette.borderBrush, customShape)
                  } else Modifier
                )
                .clickable { showCustomSeekDialog = true }
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("video_seek_custom_btn"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text =
                if (isCustomSelected) {
                  "${strings.videoSeekCustomLabel} (${strings.videoSeekPresetFormat(preferences.videoSeekStepSeconds)})"
                } else {
                  strings.videoSeekCustomLabel
                },
              style = MaterialTheme.typography.labelLarge,
              fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isCustomSelected) palette.accentGlow else palette.textPrimary,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Default 8D Audio Preferences (Enable, Speed, Intensity, & Open Radar Panel)
    SectionHeader(icon = Icons.Rounded.Headphones, title = strings.audio8DDefaultSettingsTitle)
    Spacer(modifier = Modifier.height(10.dp))
    ThemedCard(cornerRadius = 22.dp, modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = strings.audio8DToggleLabel,
              style = MaterialTheme.typography.titleSmall,
              color = palette.textPrimary,
            )
            Text(
              text = if (preferences.is8DEnabled) strings.audio8DOn else strings.audio8DOff,
              style = MaterialTheme.typography.bodySmall,
              color = if (preferences.is8DEnabled) palette.accentGlow else palette.textSecondary,
            )
          }
          Switch(
            checked = preferences.is8DEnabled,
            onCheckedChange = onSet8DEnabled,
            colors =
              SwitchDefaults.colors(
                checkedThumbColor = palette.buttonContentColor,
                checkedTrackColor = palette.buttonContainerColor,
                checkedBorderColor = palette.buttonBorderColor,
              ),
            modifier = Modifier.testTag("settings_8d_switch"),
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = strings.audio8DSpeedTitle,
          style = MaterialTheme.typography.labelLarge,
          color = palette.textSecondary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Audio8DSpeedSelector(
          selectedSpeed = preferences.audio8DSpeed,
          onSelectSpeed = onSet8DSpeed,
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = strings.audio8DIntensityTitle,
            style = MaterialTheme.typography.labelLarge,
            color = palette.textSecondary,
          )
          Text(
            text = "${preferences.audio8DIntensity}%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.accentGlow,
          )
        }
        Audio8DIntensitySlider(
          intensityPercent = preferences.audio8DIntensity,
          onChangeIntensity = onSet8DIntensity,
        )

        Spacer(modifier = Modifier.height(8.dp))
        ThemedPrimaryButton(
          onClick = onOpen8DPanel,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_open_8d_panel_btn"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Headphones,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(strings.audio8DOpenPanel)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          ThemedPrimaryButton(
            onClick = onOpenEqualizer,
            modifier = Modifier.weight(1f).height(48.dp).testTag("settings_open_eq_btn"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Equalizer,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.equalizerTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }

          ThemedPrimaryButton(
            onClick = onOpenSleepTimer,
            modifier = Modifier.weight(1f).height(48.dp).testTag("settings_open_sleep_btn"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Bedtime,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.sleepTimerTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Floating Overlay / Display Over Other Apps Feature
    SectionHeader(icon = Icons.Rounded.Layers, title = strings.overlaySectionTitle)
    Spacer(modifier = Modifier.height(10.dp))
    ThemedCard(cornerRadius = 22.dp, modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
          text =
            if (canDrawOverlays) strings.overlayGrantedTitle else strings.overlayNeededTitle,
          style = MaterialTheme.typography.titleSmall,
          color = if (canDrawOverlays) palette.accentGlow else palette.textPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = strings.overlayDescription,
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary,
        )
        Spacer(modifier = Modifier.height(14.dp))
        ThemedPrimaryButton(
          onClick = {
            if (canDrawOverlays) onLaunchFloatingOverlay() else onRequestOverlayPermission()
          },
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("overlay_action_btn"),
        ) {
          Text(
            if (canDrawOverlays) strings.launchFloatingPlayer
            else strings.enableOverlayPermission
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(bottomOverlayPadding))
  }

  if (showCustomSeekDialog) {
    VideoSeekCustomDialog(
      currentSeconds = preferences.videoSeekStepSeconds,
      onConfirm = { totalSeconds ->
        onSetVideoSeekStepSeconds(totalSeconds)
        showCustomSeekDialog = false
      },
      onDismiss = { showCustomSeekDialog = false },
    )
  }
}

@Composable
private fun VideoSeekCustomDialog(
  currentSeconds: Int,
  onConfirm: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  val initialUnit =
    when {
      currentSeconds >= 3600 && currentSeconds % 3600 == 0 -> VideoSeekUnit.HOURS
      currentSeconds >= 60 && currentSeconds % 60 == 0 -> VideoSeekUnit.MINUTES
      else -> VideoSeekUnit.SECONDS
    }
  val initialAmount =
    when (initialUnit) {
      VideoSeekUnit.HOURS -> (currentSeconds / 3600).toString()
      VideoSeekUnit.MINUTES -> (currentSeconds / 60).toString()
      VideoSeekUnit.SECONDS -> currentSeconds.toString()
    }

  var selectedUnit by remember { mutableStateOf(initialUnit) }
  var rawInput by remember { mutableStateOf(initialAmount) }
  val parsedAmount = rawInput.toIntOrNull()?.takeIf { it > 0 }

  val shape = RoundedCornerShape(24.dp)
  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .clip(shape)
          .background(palette.sheetBg)
          .background(palette.glassCardFrostBrush)
          .border(palette.borderStrokeWidth, palette.borderBrush, shape)
          .padding(18.dp)
          .testTag("video_seek_custom_dialog")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = strings.videoSeekCustomDialogTitle,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = palette.textPrimary,
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          val unitOptions =
            listOf(
              VideoSeekUnit.SECONDS to strings.videoSeekUnitSeconds,
              VideoSeekUnit.MINUTES to strings.videoSeekUnitMinutes,
              VideoSeekUnit.HOURS to strings.videoSeekUnitHours,
            )
          unitOptions.forEach { (unit, label) ->
            val selected = selectedUnit == unit
            val pillShape = RoundedCornerShape(12.dp)
            Box(
              modifier =
                Modifier.weight(1f)
                  .activeGlassOptionPillDecoration(selected = selected, shape = pillShape)
                  .then(
                    if (!selected) {
                      Modifier.clip(pillShape)
                        .background(palette.glassBadgeFillBrush)
                        .border(0.9.dp, palette.borderBrush, pillShape)
                    } else Modifier
                  )
                  .clickable { selectedUnit = unit }
                  .padding(vertical = 9.dp, horizontal = 6.dp)
                  .testTag("video_seek_unit_${unit.name.lowercase()}"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) palette.accentGlow else palette.textPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = rawInput,
          onValueChange = { text -> rawInput = text.filter { it.isDigit() }.take(4) },
          label = { Text(strings.videoSeekValueHint, color = palette.textSecondary) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedTextColor = palette.textPrimary,
              unfocusedTextColor = palette.textPrimary,
              focusedBorderColor = palette.accentGlow,
              unfocusedBorderColor = palette.borderBrush.let { palette.textSecondary.copy(alpha = 0.45f) },
              focusedContainerColor = palette.cardSurface,
              unfocusedContainerColor = palette.cardSurface,
            ),
          modifier = Modifier.fillMaxWidth().testTag("video_seek_custom_input"),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = onDismiss,
            border = androidx.compose.foundation.BorderStroke(palette.borderStrokeWidth, palette.buttonBorderColor),
            modifier = Modifier.weight(1f).height(46.dp).testTag("video_seek_custom_cancel_btn"),
          ) {
            Text(text = strings.cancel, color = palette.textSecondary)
          }
          ThemedPrimaryButton(
            onClick = {
              val amount = parsedAmount ?: return@ThemedPrimaryButton
              val multiplier =
                when (selectedUnit) {
                  VideoSeekUnit.SECONDS -> 1
                  VideoSeekUnit.MINUTES -> 60
                  VideoSeekUnit.HOURS -> 3600
                }
              val totalSeconds = (amount.toLong() * multiplier.toLong()).coerceIn(1L, 86_400L).toInt()
              onConfirm(totalSeconds)
            },
            enabled = parsedAmount != null,
            modifier = Modifier.weight(1f).height(46.dp).testTag("video_seek_custom_confirm_btn"),
          ) {
            Text(text = strings.confirmLabel, maxLines = 1, softWrap = false)
          }
        }
      }
    }
  }
}

@Composable
fun WeeklySummaryScreen(
  report: WeeklySummaryReport,
  selectedTimeframe: AnalyticsTimeframe,
  preferences: UserPreferences,
  onBack: () -> Unit,
  onSelectTimeframe: (AnalyticsTimeframe) -> Unit,
  onSetListeningTimeUnit: (ListeningTimeUnit) -> Unit,
  onPlayTop7Mix: () -> Unit,
  bottomOverlayPadding: androidx.compose.ui.unit.Dp = 160.dp,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  val maxPlays = (report.topTracks.maxOfOrNull { it.playCount } ?: 1).coerceAtLeast(1)

  Column(modifier = modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ThemedIconButton(
        onClick = onBack,
        modifier = Modifier.testTag("weekly_back_btn"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Back",
          tint = palette.textPrimary,
        )
      }
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = strings.top7Title,
        style = MaterialTheme.typography.titleLarge,
        color = palette.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Spacer(modifier = Modifier.width(8.dp))
      ListeningUnitGlassToggle(
        selectedUnit = preferences.listeningTimeUnit,
        onSelectUnit = onSetListeningTimeUnit,
      )
    }

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
      AnalyticsTimeframeFilterRow(
        selectedTimeframe = selectedTimeframe,
        onSelectTimeframe = onSelectTimeframe,
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (report.topTracks.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = palette.sectionHelperIconColor,
            modifier = Modifier.size(56.dp),
          )
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = strings.emptyWeeklyTitle,
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = strings.emptyWeeklySubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
          )
        }
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottomOverlayPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item {
          ThemedCard(cornerRadius = 22.dp, elevated = true) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                WeeklyStatPill(
                  value = "${report.totalPlaysThisWeek}",
                  label = strings.totalPlaysLabel,
                  modifier = Modifier.weight(1f),
                )
                WeeklyStatPill(
                  value =
                    formatRealListeningStat(
                      totalListenedMs = report.totalListenedMs,
                      unit = preferences.listeningTimeUnit,
                      secondsSuffix = strings.listeningUnitSecondsShort,
                      minutesSuffix = strings.listeningUnitMinutesShort,
                    ),
                  label = strings.listeningMinutesLabel,
                  modifier = Modifier.weight(1f),
                )
                WeeklyStatPill(
                  value = "${report.uniqueTracksPlayed}",
                  label = strings.uniqueSongsLabel,
                  modifier = Modifier.weight(1f),
                )
              }
              Spacer(modifier = Modifier.height(14.dp))
              ThemedPrimaryButton(
                onClick = onPlayTop7Mix,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("play_top7_btn"),
              ) {
                Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.playTop7Mix)
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        itemsIndexed(report.topTracks) { index, item ->
          val barRatio = (item.playCount.toFloat() / maxPlays.toFloat()).coerceIn(0.12f, 1f)
          ThemedCard(
            cornerRadius = 18.dp,
            elevated = index == 0,
            modifier = Modifier.fillMaxWidth().testTag("weekly_rank_${index + 1}"),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier =
                    Modifier.size(34.dp)
                      .clip(CircleShape)
                      .background(
                        if (index == 0) palette.buttonContainerColor
                        else palette.cardSurfaceElevated
                      )
                      .border(
                        palette.borderStrokeWidth,
                        if (index == 0) palette.buttonBorderColor else palette.textSecondary.copy(alpha = 0.25f),
                        CircleShape,
                      ),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = "#${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (index == 0) palette.buttonContentColor else palette.textPrimary,
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                ArtworkThumbnail(
                  model = item.albumArtUri,
                  contentDescription = item.title,
                  size = 44.dp,
                  cornerRadius = 10.dp,
                  showActiveRim = index == 0,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text =
                      if (item.listenedMs > 0L) {
                        val unitText =
                          formatRealListeningStat(
                            totalListenedMs = item.listenedMs,
                            unit = preferences.listeningTimeUnit,
                            secondsSuffix = strings.listeningUnitSecondsShort,
                            minutesSuffix = strings.listeningUnitMinutesShort,
                          )
                        "${item.artist} • $unitText"
                      } else {
                        item.artist
                      },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Text(
                  text = "${item.playCount} ${strings.playsLabel}",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = palette.accentGlow,
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(palette.textSecondary.copy(alpha = 0.14f))
              ) {
                Box(
                  modifier =
                    Modifier.fillMaxWidth(barRatio)
                      .fillMaxHeight()
                      .clip(RoundedCornerShape(3.dp))
                      .background(
                        Brush.horizontalGradient(
                          listOf(palette.accentPrimary, palette.accentGlow)
                        )
                      )
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AnalyticsTimeframeFilterRow(
  selectedTimeframe: AnalyticsTimeframe,
  onSelectTimeframe: (AnalyticsTimeframe) -> Unit,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  val tabs =
    listOf(
      AnalyticsTimeframe.DAILY to strings.timeframeDaily,
      AnalyticsTimeframe.WEEKLY to strings.timeframeWeekly,
      AnalyticsTimeframe.MONTHLY to strings.timeframeMonthly,
      AnalyticsTimeframe.YEARLY to strings.timeframeYearly,
    )

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(palette.glassBadgeFillBrush)
        .border(palette.borderStrokeWidth, palette.borderBrush, RoundedCornerShape(16.dp))
        .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    tabs.forEach { (tf, label) ->
      val selected = tf == selectedTimeframe
      Box(
        modifier =
          Modifier.weight(1f)
            .activeGlassOptionPillDecoration(selected = selected, shape = RoundedCornerShape(12.dp))
            .then(
              if (!selected) Modifier.clip(RoundedCornerShape(12.dp)) else Modifier
            )
            .clickable { onSelectTimeframe(tf) }
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("insights_timeframe_${tf.name.lowercase()}"),
        contentAlignment = Alignment.Center,
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
  }
}

@Composable
private fun ListeningUnitGlassToggle(
  selectedUnit: ListeningTimeUnit,
  onSelectUnit: (ListeningTimeUnit) -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current
  val options =
    listOf(
      ListeningTimeUnit.SECONDS to strings.listeningUnitSecondsLabel,
      ListeningTimeUnit.MINUTES to strings.listeningUnitMinutesLabel,
    )

  Row(
    modifier =
      modifier
        .clip(RoundedCornerShape(14.dp))
        .background(palette.glassBadgeFillBrush)
        .border(palette.borderStrokeWidth, palette.borderBrush, RoundedCornerShape(14.dp))
        .padding(3.dp),
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    options.forEach { (unit, label) ->
      val isSelected = unit == selectedUnit
      Box(
        modifier =
          Modifier.activeGlassOptionPillDecoration(
            selected = isSelected,
            shape = RoundedCornerShape(10.dp),
          )
            .then(if (!isSelected) Modifier.clip(RoundedCornerShape(10.dp)) else Modifier)
            .clickable { onSelectUnit(unit) }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("insights_unit_${unit.name.lowercase()}"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          color = if (isSelected) palette.accentGlow else palette.textSecondary,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun UsageFileSyncBanner(
  preferences: UserPreferences,
  onSelectAutoSyncFileClick: () -> Unit,
  onImportClick: () -> Unit,
) {
  val palette = LocalMPlayerPalette.current
  val strings = LocalAppStrings.current

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(palette.glassBadgeFillBrush)
        .border(1.dp, palette.borderBrush, RoundedCornerShape(16.dp))
        .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector =
          if (preferences.usageFileSyncEnabled) Icons.Rounded.CheckCircle else Icons.Rounded.Folder,
        contentDescription = UsageStatsEngine.USAGE_STATS_FILENAME,
        tint = palette.sectionHelperIconColor,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = strings.usageSyncTitle,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = palette.sectionHelperTextColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text =
            if (preferences.usageFileSyncEnabled) strings.usageSyncActive
            else strings.usageSyncInactive,
          style = MaterialTheme.typography.labelSmall,
          color = palette.textSecondary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (preferences.usageFileSyncEnabled && preferences.usageLastSyncedIso.isNotBlank()) {
          Text(
            text = "${strings.usageSyncLastSyncedPrefix} ${preferences.usageLastSyncedIso.take(19).replace('T', ' ')}",
            style = MaterialTheme.typography.labelSmall,
            color = palette.sectionHelperIconColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ThemedPrimaryButton(
        onClick = onSelectAutoSyncFileClick,
        modifier = Modifier.weight(1f).height(42.dp).testTag("usage_sync_primary_btn"),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
      ) {
        Icon(
          imageVector =
            if (preferences.usageFileSyncEnabled) Icons.Rounded.Folder else Icons.Rounded.Save,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text =
            if (preferences.usageFileSyncEnabled) strings.usageSyncChangeFileButton
            else strings.usageSyncButton,
          style = MaterialTheme.typography.labelMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      ThemedPrimaryButton(
        onClick = onImportClick,
        modifier = Modifier.weight(1f).height(42.dp).testTag("usage_import_btn"),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.FileUpload,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = strings.usageImportButton,
          style = MaterialTheme.typography.labelMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

internal fun formatRealListeningStat(
  totalListenedMs: Long,
  unit: ListeningTimeUnit = ListeningTimeUnit.MINUTES,
  secondsSuffix: String = "s",
  minutesSuffix: String = "m",
): String {
  val safeMs = totalListenedMs.coerceAtLeast(0L)
  val totalSeconds = safeMs / 1000L
  return when (unit) {
    ListeningTimeUnit.SECONDS -> "${totalSeconds}$secondsSuffix"
    ListeningTimeUnit.MINUTES -> {
      val totalMinutes = totalSeconds / 60L
      if (totalSeconds == 0L) {
        "0$minutesSuffix"
      } else if (totalSeconds < 60L) {
        val fractionalMinutes = String.format(java.util.Locale.US, "%.1f", safeMs / 60_000.0)
        "${fractionalMinutes}$minutesSuffix"
      } else {
        "${totalMinutes}$minutesSuffix"
      }
    }
  }
}

@Composable
private fun SectionHeader(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
) {
  val palette = LocalMPlayerPalette.current
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = palette.sectionHelperIconColor,
      modifier = Modifier.size(20.dp),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = palette.sectionHelperTextColor,
    )
  }
}

@Composable
private fun WeeklyStatPill(
  value: String,
  label: String,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  Column(
    modifier = modifier.padding(vertical = 6.dp, horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = palette.accentGlow,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = palette.textSecondary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ThemeSwatchCircle(
  contentDescription: String,
  selected: Boolean,
  swatchColors: List<Color>,
  swatchBorderColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .size(48.dp)
        .clip(CircleShape)
        .semantics {
          this.contentDescription = contentDescription
          this.selected = selected
        }
        .clickable(role = Role.RadioButton, onClick = onClick)
        .then(
          if (selected) {
            Modifier.border(2.dp, swatchBorderColor, CircleShape)
          } else {
            Modifier.border(1.dp, swatchBorderColor.copy(alpha = 0.25f), CircleShape)
          }
        ),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.size(38.dp)
          .clip(CircleShape)
          .background(Brush.linearGradient(swatchColors))
          .border(1.dp, swatchBorderColor, CircleShape)
    )
  }
}

@Composable
private fun CosmicStarfieldSwatchCircle(
  contentDescription: String,
  selected: Boolean,
  swatchBorderColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val staticStarOffsets =
    remember {
      listOf(
        Triple(0.28f, 0.30f, 1.3f),
        Triple(0.68f, 0.24f, 1.6f),
        Triple(0.45f, 0.44f, 1.1f),
        Triple(0.76f, 0.52f, 1.4f),
        Triple(0.24f, 0.62f, 1.5f),
        Triple(0.56f, 0.72f, 1.2f),
        Triple(0.36f, 0.76f, 1.0f),
        Triple(0.64f, 0.38f, 1.1f),
        Triple(0.38f, 0.22f, 1.0f),
        Triple(0.72f, 0.70f, 1.2f),
        Triple(0.20f, 0.44f, 0.9f),
        Triple(0.52f, 0.56f, 1.7f),
      )
    }

  Box(
    modifier =
      modifier
        .size(48.dp)
        .clip(CircleShape)
        .semantics {
          this.contentDescription = contentDescription
          this.selected = selected
        }
        .clickable(role = Role.RadioButton, onClick = onClick)
        .then(
          if (selected) {
            Modifier.border(2.dp, swatchBorderColor, CircleShape)
          } else {
            Modifier.border(1.dp, swatchBorderColor.copy(alpha = 0.35f), CircleShape)
          }
        ),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(
      modifier =
        Modifier.size(38.dp)
          .clip(CircleShape)
          .background(Color.Black)
          .border(1.dp, swatchBorderColor, CircleShape)
    ) {
      val c = Offset(size.width / 2f, size.height / 2f)
      val sphereR = size.minDimension * 0.46f

      // Small celestial sphere shading over deep black void
      drawCircle(
        brush =
          Brush.radialGradient(
            listOf(Color(0xFF1F3368), Color(0xFF070D1D), Color.Black),
            center = Offset(c.x - sphereR * 0.28f, c.y - sphereR * 0.28f),
            radius = sphereR * 1.18f,
          ),
        radius = sphereR,
        center = c,
      )

      // Scattered crisp stars across the miniature celestial sphere
      staticStarOffsets.forEachIndexed { idx, (nx, ny, radiusDp) ->
        drawCircle(
          color = if (idx % 3 == 0) Color(0xFFD6E4FF) else Color.White,
          radius = radiusDp.dp.toPx(),
          center = Offset(size.width * nx, size.height * ny),
        )
      }
      drawCircle(
        color = Color(0xFF8BB0FF).copy(alpha = 0.28f),
        radius = sphereR * 0.5f,
        center = c,
      )
    }
  }
}

@Composable
private fun LanguageOptionChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  ThemedCard(
    onClick = onClick,
    cornerRadius = 18.dp,
    elevated = selected,
    modifier = modifier.height(50.dp),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .activeGlassOptionPillDecoration(selected = selected, shape = RoundedCornerShape(18.dp))
          .padding(horizontal = 10.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) palette.accentGlow else palette.textPrimary,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
