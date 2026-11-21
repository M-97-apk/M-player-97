package com.example.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.LocalAppStrings
import com.example.model.AppThemeStyle
import com.example.model.FloatingWidgetColorMode
import com.example.model.FloatingWidgetGlassMode
import com.example.ui.theme.CosmicStarfieldAccent
import com.example.ui.theme.CosmicStarfieldGlowHighlight
import com.example.ui.theme.DeepBurgundyAccent
import com.example.ui.theme.DeepBurgundyGlowHighlight
import com.example.ui.theme.LuxuryBrownAccent
import com.example.ui.theme.LuxuryBrownGlowHighlight
import com.example.ui.theme.MetallicGoldBright
import com.example.ui.theme.MidnightBlueAccent
import com.example.ui.theme.MidnightBlueGlowHighlight
import com.example.ui.theme.PlainBlackGlowHighlight
import com.example.ui.theme.RoyalPurpleAccent
import com.example.ui.theme.RoyalPurpleGlowHighlight

/**
 * Compact, inline frosted glass Quick Settings popover displayed directly inside
 * the Floating Mini Player Widget. Strictly decouples Widget Theme Color from
 * Glass Opacity & Blur Mode so either can be tuned independently on the fly.
 */
@Composable
fun FloatingWidgetQuickSettingsPanel(
  spec: FloatingWidgetVisualSpec,
  appThemeStyle: AppThemeStyle,
  selectedColorMode: FloatingWidgetColorMode,
  selectedGlassMode: FloatingWidgetGlassMode,
  customOpacity: Float? = null,
  onSelectColorMode: (FloatingWidgetColorMode) -> Unit,
  onSelectGlassMode: (FloatingWidgetGlassMode) -> Unit,
  onChangeCustomOpacity: (Float) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current

  val popoverBg =
    spec.elevatedSurfaceColor.copy(alpha = maxOf(spec.elevatedSurfaceAlpha, 0.72f))

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(top = 10.dp)
        .clip(RoundedCornerShape(22.dp))
        .background(popoverBg)
        .border(1.dp, spec.borderBrush, RoundedCornerShape(22.dp))
        .padding(12.dp)
        .testTag("overlay_quick_settings_panel")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Palette,
        contentDescription = strings.widgetQuickSettingsTitle,
        tint = spec.accentIndicatorColor,
        modifier = Modifier.size(16.dp),
      )
      Text(
        text = strings.widgetQuickSettingsTitle,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = spec.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 1. Independent Control: Widget Theme Color (Match App / Neutral / Direct Theme Color)
    Text(
      text = strings.widgetColorSectionTitle,
      style = MaterialTheme.typography.labelSmall,
      color = spec.textSecondary,
    )
    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      val appDotColor =
        when (appThemeStyle) {
          AppThemeStyle.DEEP_BURGUNDY -> DeepBurgundyGlowHighlight
          AppThemeStyle.ROYAL_PURPLE -> RoyalPurpleGlowHighlight
          AppThemeStyle.MIDNIGHT_BLUE -> MidnightBlueGlowHighlight
          AppThemeStyle.PITCH_BLACK_GOLD -> MetallicGoldBright
          AppThemeStyle.LUXURY_DARK_BROWN -> LuxuryBrownGlowHighlight
          AppThemeStyle.COSMIC_STARFIELD -> CosmicStarfieldGlowHighlight
          AppThemeStyle.PLAIN_BLACK -> PlainBlackGlowHighlight
        }
      WidgetColorModeChip(
        label = strings.widgetColorMatchApp,
        selected = selectedColorMode == FloatingWidgetColorMode.MATCH_APP,
        swatchFill =
          when (appThemeStyle) {
            AppThemeStyle.PITCH_BLACK_GOLD,
            AppThemeStyle.COSMIC_STARFIELD,
            AppThemeStyle.PLAIN_BLACK -> Color.Black
            AppThemeStyle.LUXURY_DARK_BROWN -> LuxuryBrownAccent
            else -> appDotColor
          },
        swatchBorder = appDotColor,
        spec = spec,
        onClick = { onSelectColorMode(FloatingWidgetColorMode.MATCH_APP) },
        modifier = Modifier.weight(1f).testTag("widget_color_match_app"),
      )
      WidgetColorModeChip(
        label = strings.widgetColorNeutral,
        selected = selectedColorMode == FloatingWidgetColorMode.NEUTRAL,
        swatchFill = Color(0xFF282830),
        swatchBorder = Color.White.copy(alpha = 0.85f),
        spec = spec,
        onClick = { onSelectColorMode(FloatingWidgetColorMode.NEUTRAL) },
        modifier = Modifier.weight(1f).testTag("widget_color_neutral"),
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Direct theme color swatches so user can pick any of the 7 themes directly from the widget
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      val themeSwatches =
        listOf(
          Triple(FloatingWidgetColorMode.DEEP_BURGUNDY, DeepBurgundyAccent, DeepBurgundyGlowHighlight),
          Triple(FloatingWidgetColorMode.ROYAL_PURPLE, RoyalPurpleAccent, RoyalPurpleGlowHighlight),
          Triple(FloatingWidgetColorMode.MIDNIGHT_BLUE, MidnightBlueAccent, MidnightBlueGlowHighlight),
          Triple(FloatingWidgetColorMode.PITCH_BLACK_GOLD, Color(0xFF000000), MetallicGoldBright),
          Triple(FloatingWidgetColorMode.LUXURY_DARK_BROWN, LuxuryBrownAccent, LuxuryBrownGlowHighlight),
          Triple(FloatingWidgetColorMode.COSMIC_STARFIELD, CosmicStarfieldAccent, CosmicStarfieldGlowHighlight),
          Triple(FloatingWidgetColorMode.PLAIN_BLACK, Color(0xFF000000), Color.White),
        )
      themeSwatches.forEach { (mode, fill, rim) ->
        val isSelected = selectedColorMode == mode
        Box(
          modifier =
            Modifier.weight(1f)
              .height(32.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(
                if (isSelected) {
                  if (spec.isPitchBlackGold) Color(0xFF0A0A0A)
                  else spec.elevatedSurfaceColor.copy(alpha = 0.72f)
                } else {
                  spec.baseSurfaceColor.copy(alpha = 0.34f)
                }
              )
              .border(
                width = if (isSelected) 1.2.dp else 0.8.dp,
                color = if (isSelected) rim else rim.copy(alpha = 0.42f),
                shape = RoundedCornerShape(10.dp),
              )
              .clickable { onSelectColorMode(mode) }
              .testTag("widget_color_${mode.name.lowercase()}"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.size(16.dp)
                .clip(CircleShape)
                .background(fill)
                .border(1.2.dp, rim, CircleShape)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2. Independent Control: Glass Opacity & Blur Mode (Custom Transparency Slider + Presets)
    val sliderOpacity = (customOpacity ?: spec.effectiveOpacity).coerceIn(0f, 1f)
    val opacityPercent = (sliderOpacity * 100f).roundToInt().coerceIn(0, 100)

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Opacity,
          contentDescription = strings.widgetGlassSectionTitle,
          tint = spec.accentIndicatorColor,
          modifier = Modifier.size(14.dp),
        )
        Text(
          text = strings.widgetGlassSectionTitle,
          style = MaterialTheme.typography.labelSmall,
          color = spec.textSecondary,
        )
      }
      Text(
        text = "$opacityPercent%",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = spec.accentIndicatorColor,
        modifier = Modifier.testTag("widget_transparency_pct"),
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Standalone Transparency Slider Bar placed directly above the 4 preset buttons
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(spec.baseSurfaceColor.copy(alpha = 0.36f))
          .border(1.dp, spec.borderColor.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
          .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
      Slider(
        value = sliderOpacity,
        onValueChange = { next -> onChangeCustomOpacity(next.coerceIn(0f, 1f)) },
        valueRange = 0f..1f,
        colors =
          SliderDefaults.colors(
            thumbColor = spec.accentIndicatorColor,
            activeTrackColor = spec.accentTrackColor,
            inactiveTrackColor = spec.textSecondary.copy(alpha = 0.28f),
          ),
        modifier = Modifier.fillMaxWidth().height(32.dp).testTag("widget_transparency_slider"),
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    val glassOptions =
      listOf(
        Triple(FloatingWidgetGlassMode.FROSTED_BLUR, strings.widgetGlassFrostedBlur, Icons.Rounded.BlurOn),
        Triple(FloatingWidgetGlassMode.SEMI_FROSTED, strings.widgetGlassSemiFrosted, Icons.Rounded.AutoAwesome),
        Triple(FloatingWidgetGlassMode.OPAQUE, strings.widgetGlassOpaque, Icons.Rounded.Contrast),
        Triple(FloatingWidgetGlassMode.FULLY_TRANSPARENT, strings.widgetGlassTransparent, Icons.Rounded.Visibility),
      )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      glassOptions.chunked(2).forEach { pair ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          pair.forEach { (mode, label, icon) ->
            WidgetGlassModeChip(
              label = label,
              icon = icon,
              selected = customOpacity == null && selectedGlassMode == mode ||
                (customOpacity != null && selectedGlassMode == mode),
              spec = spec,
              onClick = { onSelectGlassMode(mode) },
              modifier = Modifier.weight(1f).testTag("widget_glass_${mode.name.lowercase()}"),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun WidgetColorModeChip(
  label: String,
  selected: Boolean,
  swatchFill: Color,
  swatchBorder: Color,
  spec: FloatingWidgetVisualSpec,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val chipFill =
    when {
      selected && spec.isPitchBlackGold -> Color(0xFF0A0A0A)
      selected -> spec.accentTrackColor.copy(alpha = 0.28f)
      else -> spec.baseSurfaceColor.copy(alpha = 0.34f)
    }
  val chipBorder =
    if (selected) spec.accentIndicatorColor else spec.borderColor.copy(alpha = 0.28f)

  Row(
    modifier =
      modifier
        .height(36.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(chipFill)
        .border(if (selected) 1.1.dp else 0.8.dp, chipBorder, RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Box(
      modifier =
        Modifier.size(11.dp)
          .clip(CircleShape)
          .background(swatchFill)
          .border(1.dp, swatchBorder, CircleShape)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      color = if (selected) spec.accentIndicatorColor else spec.textPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun WidgetGlassModeChip(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  spec: FloatingWidgetVisualSpec,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val chipFill =
    when {
      selected && spec.isPitchBlackGold -> Color(0xFF0A0A0A)
      selected -> spec.accentTrackColor.copy(alpha = 0.28f)
      else -> spec.baseSurfaceColor.copy(alpha = 0.34f)
    }
  val chipBorder =
    if (selected) spec.accentIndicatorColor else spec.borderColor.copy(alpha = 0.28f)

  Row(
    modifier =
      modifier
        .height(36.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(chipFill)
        .border(if (selected) 1.1.dp else 0.8.dp, chipBorder, RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (selected) spec.accentIndicatorColor else spec.textSecondary,
      modifier = Modifier.size(14.dp),
    )
    Spacer(modifier = Modifier.width(5.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      color = if (selected) spec.accentIndicatorColor else spec.textPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
