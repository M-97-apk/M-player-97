package com.example.overlay

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.model.AppThemeStyle
import com.example.model.FloatingWidgetColorMode
import com.example.model.FloatingWidgetGlassMode
import com.example.ui.theme.LuxuryBrownAccentBright
import com.example.ui.theme.LuxuryBrownBgBottom
import com.example.ui.theme.LuxuryBrownBgTop
import com.example.ui.theme.LuxuryBrownTextPrimary
import com.example.ui.theme.LuxuryBrownTextSecondary
import com.example.ui.theme.MPlayerPalette
import com.example.ui.theme.MetallicGoldBright
import com.example.ui.theme.MetallicGoldDark
import com.example.ui.theme.PitchBlackTextPrimary
import com.example.ui.theme.PitchBlackTextSecondary
import com.example.ui.theme.resolvePalette
import kotlin.math.roundToInt

@Immutable
data class FloatingWidgetVisualSpec(
  val effectiveColorMode: FloatingWidgetColorMode,
  val glassMode: FloatingWidgetGlassMode,
  val effectiveOpacity: Float,
  val isNeutral: Boolean,
  val isPitchBlackGold: Boolean,
  val baseSurfaceColor: Color,
  val elevatedSurfaceColor: Color,
  val topGradientColor: Color,
  val borderColor: Color,
  val topSpecularColor: Color,
  val accentIndicatorColor: Color,
  val accentTrackColor: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val iconTint: Color,
  val primaryButtonIconTint: Color,
  val surfaceAlpha: Float,
  val elevatedSurfaceAlpha: Float,
  val topGradientAlpha: Float,
  val bottomGradientAlpha: Float,
  val borderAlpha: Float,
  val topSpecularAlpha: Float,
  val frostGrainAlpha: Float,
  val innerRefractionAlpha: Float,
  val buttonOpacityScale: Float,
  val enableWindowBlur: Boolean,
  val blurBehindRadiusPx: Int,
  val widgetPalette: MPlayerPalette,
) {
  val containerColor: Color
    get() = baseSurfaceColor.copy(alpha = surfaceAlpha)

  val elevatedPanelColor: Color
    get() = elevatedSurfaceColor.copy(alpha = elevatedSurfaceAlpha)

  val capsuleBackdropBrush: Brush
    get() =
      if (effectiveOpacity <= 0.02f) {
        Brush.verticalGradient(
          listOf(
            Color.Transparent,
            baseSurfaceColor.copy(alpha = bottomGradientAlpha),
          )
        )
      } else {
        Brush.verticalGradient(
          listOf(
            topGradientColor.copy(alpha = topGradientAlpha),
            elevatedSurfaceColor.copy(alpha = elevatedSurfaceAlpha * 0.78f),
            baseSurfaceColor.copy(alpha = bottomGradientAlpha),
          )
        )
      }

  val borderBrush: Brush
    get() =
      Brush.linearGradient(
        when {
          isPitchBlackGold ->
            listOf(
              MetallicGoldBright.copy(alpha = borderAlpha),
              MetallicGoldDark.copy(alpha = (borderAlpha * 0.82f).coerceIn(0f, 1f)),
              MetallicGoldBright.copy(alpha = borderAlpha),
            )
          isNeutral ->
            listOf(
              Color.White.copy(alpha = borderAlpha * 0.78f),
              Color.White.copy(alpha = borderAlpha * 0.36f),
              Color.White.copy(alpha = borderAlpha * 0.72f),
            )
          else ->
            listOf(
              Color.White.copy(alpha = borderAlpha * 0.28f),
              borderColor.copy(alpha = borderAlpha * 0.85f),
              accentTrackColor.copy(alpha = borderAlpha * 0.78f),
            )
        }
      )
}

fun defaultOpacityForGlassMode(glassMode: FloatingWidgetGlassMode): Float =
  when (glassMode) {
    FloatingWidgetGlassMode.FROSTED_BLUR -> 0.58f
    FloatingWidgetGlassMode.SEMI_FROSTED -> 0.30f
    FloatingWidgetGlassMode.OPAQUE -> 1.0f
    FloatingWidgetGlassMode.FULLY_TRANSPARENT -> 0.0f
  }

/**
 * Resolves the floating widget appearance cleanly by keeping the Color axis
 * (Match Active App Theme / Neutral Colorless / Specific Theme) 100% decoupled
 * from the Glass Opacity & Blur axis (Presets + Custom Transparency Slider).
 */
fun resolveFloatingWidgetSpec(
  appThemeStyle: AppThemeStyle,
  colorMode: FloatingWidgetColorMode,
  glassMode: FloatingWidgetGlassMode,
  customOpacity: Float? = null,
): FloatingWidgetVisualSpec {
  val effectiveColorMode =
    if (colorMode == FloatingWidgetColorMode.MATCH_APP) {
      when (appThemeStyle) {
        AppThemeStyle.DEEP_BURGUNDY -> FloatingWidgetColorMode.DEEP_BURGUNDY
        AppThemeStyle.ROYAL_PURPLE -> FloatingWidgetColorMode.ROYAL_PURPLE
        AppThemeStyle.MIDNIGHT_BLUE -> FloatingWidgetColorMode.MIDNIGHT_BLUE
        AppThemeStyle.PITCH_BLACK_GOLD -> FloatingWidgetColorMode.PITCH_BLACK_GOLD
        AppThemeStyle.LUXURY_DARK_BROWN -> FloatingWidgetColorMode.LUXURY_DARK_BROWN
        AppThemeStyle.COSMIC_STARFIELD -> FloatingWidgetColorMode.COSMIC_STARFIELD
        AppThemeStyle.PLAIN_BLACK -> FloatingWidgetColorMode.PLAIN_BLACK
      }
    } else {
      colorMode
    }

  val isNeutral = effectiveColorMode == FloatingWidgetColorMode.NEUTRAL
  val isPitchBlackGold = effectiveColorMode == FloatingWidgetColorMode.PITCH_BLACK_GOLD

  // 1. Color Axis (never inspects glassMode or customOpacity)
  val basePalette =
    when (effectiveColorMode) {
      FloatingWidgetColorMode.NEUTRAL -> buildNeutralWidgetPalette()
      FloatingWidgetColorMode.DEEP_BURGUNDY -> resolvePalette(AppThemeStyle.DEEP_BURGUNDY, true)
      FloatingWidgetColorMode.ROYAL_PURPLE -> resolvePalette(AppThemeStyle.ROYAL_PURPLE, true)
      FloatingWidgetColorMode.MIDNIGHT_BLUE -> resolvePalette(AppThemeStyle.MIDNIGHT_BLUE, true)
      FloatingWidgetColorMode.PITCH_BLACK_GOLD -> resolvePalette(AppThemeStyle.PITCH_BLACK_GOLD, true)
      FloatingWidgetColorMode.LUXURY_DARK_BROWN -> resolvePalette(AppThemeStyle.LUXURY_DARK_BROWN, true)
      FloatingWidgetColorMode.COSMIC_STARFIELD -> resolvePalette(AppThemeStyle.COSMIC_STARFIELD, true)
      FloatingWidgetColorMode.PLAIN_BLACK -> resolvePalette(AppThemeStyle.PLAIN_BLACK, true)
      FloatingWidgetColorMode.MATCH_APP -> resolvePalette(appThemeStyle, true)
    }

  val baseSurfaceColor: Color
  val elevatedSurfaceColor: Color
  val topGradientColor: Color
  val borderColor: Color
  val topSpecularColor: Color
  val accentIndicatorColor: Color
  val accentTrackColor: Color
  val textPrimary: Color
  val textSecondary: Color
  val iconTint: Color
  val primaryButtonIconTint: Color

  when {
    isNeutral -> {
      baseSurfaceColor = Color(0xFF0E0E12)
      elevatedSurfaceColor = Color(0xFF1A1A22)
      topGradientColor = Color(0xFF282832)
      borderColor = Color(0xFFEAEAF2)
      topSpecularColor = Color.White
      accentIndicatorColor = Color.White
      accentTrackColor = Color(0xFFD0D0DC)
      textPrimary = Color(0xFFF7F7FA)
      textSecondary = Color(0xFFB8B8C4)
      iconTint = Color(0xFFF7F7FA)
      primaryButtonIconTint = Color.White
    }
    isPitchBlackGold -> {
      // Strictly pure obsidian #000000 / #0A0A0A surfaces — Metallic Gold #D4AF37 ONLY on 1px borders, active indicators & icons
      baseSurfaceColor = Color(0xFF000000)
      elevatedSurfaceColor = Color(0xFF0A0A0A)
      topGradientColor = Color(0xFF0A0A0A)
      borderColor = MetallicGoldBright
      topSpecularColor = MetallicGoldBright
      accentIndicatorColor = MetallicGoldBright
      accentTrackColor = MetallicGoldDark
      textPrimary = PitchBlackTextPrimary
      textSecondary = PitchBlackTextSecondary
      iconTint = PitchBlackTextPrimary
      primaryButtonIconTint = MetallicGoldBright
    }
    effectiveColorMode == FloatingWidgetColorMode.LUXURY_DARK_BROWN -> {
      baseSurfaceColor = LuxuryBrownBgBottom
      elevatedSurfaceColor = Color(0xFF1E0F08)
      topGradientColor = LuxuryBrownBgTop
      borderColor = basePalette.accentGlow
      topSpecularColor = basePalette.accentGlow
      accentIndicatorColor = basePalette.accentGlow
      accentTrackColor = LuxuryBrownAccentBright
      textPrimary = LuxuryBrownTextPrimary
      textSecondary = LuxuryBrownTextSecondary
      iconTint = LuxuryBrownTextPrimary
      primaryButtonIconTint = LuxuryBrownTextPrimary
    }
    else -> {
      baseSurfaceColor = basePalette.backgroundBottom
      elevatedSurfaceColor = basePalette.cardSurfaceElevated.copy(alpha = 1f)
      topGradientColor = basePalette.accentPrimary
      borderColor = basePalette.accentGlow
      topSpecularColor = basePalette.accentGlow
      accentIndicatorColor = basePalette.accentGlow
      accentTrackColor = basePalette.accentPrimary
      textPrimary = basePalette.textPrimary
      textSecondary = basePalette.textSecondary
      iconTint = basePalette.textPrimary
      primaryButtonIconTint = basePalette.buttonContentColor
    }
  }

  // 2. Glass Opacity & Blur Axis (never overrides color choice)
  val effectiveOpacity = (customOpacity ?: defaultOpacityForGlassMode(glassMode)).coerceIn(0f, 1f)

  val surfaceAlpha: Float
  val elevatedSurfaceAlpha: Float
  val topGradientAlpha: Float
  val bottomGradientAlpha: Float
  val borderAlpha: Float
  val topSpecularAlpha: Float
  val frostGrainAlpha: Float
  val innerRefractionAlpha: Float
  val enableWindowBlur: Boolean
  val blurBehindRadiusPx: Int
  val buttonScale: Float

  if (customOpacity == null) {
    when (glassMode) {
      FloatingWidgetGlassMode.FROSTED_BLUR -> {
        surfaceAlpha = if (isPitchBlackGold) 0.54f else 0.44f
        elevatedSurfaceAlpha = if (isPitchBlackGold) 0.58f else 0.38f
        topGradientAlpha = if (isPitchBlackGold || isNeutral) 0.34f else 0.18f
        bottomGradientAlpha = if (isPitchBlackGold) 0.62f else 0.42f
        borderAlpha = if (isPitchBlackGold) 0.94f else 0.76f
        topSpecularAlpha = 0.64f
        frostGrainAlpha = 0.055f
        innerRefractionAlpha = if (isPitchBlackGold) 0.08f else 0.14f
        enableWindowBlur = true
        blurBehindRadiusPx = 42
        buttonScale = 0.95f
      }
      FloatingWidgetGlassMode.SEMI_FROSTED -> {
        surfaceAlpha = 0.24f
        elevatedSurfaceAlpha = 0.22f
        topGradientAlpha = if (isPitchBlackGold || isNeutral) 0.18f else 0.11f
        bottomGradientAlpha = 0.22f
        borderAlpha = if (isPitchBlackGold) 0.88f else 0.66f
        topSpecularAlpha = 0.48f
        frostGrainAlpha = 0.036f
        innerRefractionAlpha = if (isPitchBlackGold) 0.05f else 0.09f
        enableWindowBlur = true
        blurBehindRadiusPx = 18
        buttonScale = 0.56f
      }
      FloatingWidgetGlassMode.OPAQUE -> {
        surfaceAlpha = 0.97f
        elevatedSurfaceAlpha = 0.94f
        topGradientAlpha = if (isPitchBlackGold || isNeutral) 0.94f else 0.22f
        bottomGradientAlpha = 0.99f
        borderAlpha = 0.96f
        topSpecularAlpha = 0.68f
        frostGrainAlpha = 0.0f
        innerRefractionAlpha = 0.0f
        enableWindowBlur = false
        blurBehindRadiusPx = 0
        buttonScale = 1.15f
      }
      FloatingWidgetGlassMode.FULLY_TRANSPARENT -> {
        surfaceAlpha = 0.0f
        elevatedSurfaceAlpha = 0.06f
        topGradientAlpha = 0.0f
        bottomGradientAlpha = 0.02f
        borderAlpha = if (isPitchBlackGold) 0.92f else 0.72f
        topSpecularAlpha = 0.40f
        frostGrainAlpha = 0.0f
        innerRefractionAlpha = 0.0f
        enableWindowBlur = false
        blurBehindRadiusPx = 0
        buttonScale = 0.18f
      }
    }
  } else {
    when {
      effectiveOpacity <= 0.02f -> {
        surfaceAlpha = 0.0f
        elevatedSurfaceAlpha = 0.05f
        topGradientAlpha = 0.0f
        bottomGradientAlpha = 0.01f
        borderAlpha = if (isPitchBlackGold) 0.92f else 0.72f
        topSpecularAlpha = 0.38f
        frostGrainAlpha = 0.0f
        innerRefractionAlpha = 0.0f
        enableWindowBlur = false
        blurBehindRadiusPx = 0
        buttonScale = 0.16f
      }
      effectiveOpacity >= 0.94f -> {
        val tOpaque = ((effectiveOpacity - 0.94f) / 0.06f).coerceIn(0f, 1f)
        surfaceAlpha = 0.92f + tOpaque * 0.07f
        elevatedSurfaceAlpha = 0.90f + tOpaque * 0.06f
        topGradientAlpha = if (isPitchBlackGold || isNeutral) 0.90f else 0.22f
        bottomGradientAlpha = 0.96f + tOpaque * 0.03f
        borderAlpha = 0.94f
        topSpecularAlpha = 0.66f
        frostGrainAlpha = 0.0f
        innerRefractionAlpha = 0.0f
        enableWindowBlur = false
        blurBehindRadiusPx = 0
        buttonScale = 1.15f
      }
      else -> {
        val norm = ((effectiveOpacity - 0.02f) / 0.92f).coerceIn(0f, 1f)
        surfaceAlpha = (0.04f + norm * (if (isPitchBlackGold) 0.84f else 0.78f)).coerceIn(0.03f, 0.88f)
        elevatedSurfaceAlpha = (0.08f + norm * 0.76f).coerceIn(0.06f, 0.86f)
        topGradientAlpha = (0.04f + norm * (if (isPitchBlackGold || isNeutral) 0.56f else 0.24f))
        bottomGradientAlpha = (0.04f + norm * (if (isPitchBlackGold) 0.86f else 0.74f))
        borderAlpha =
          if (isPitchBlackGold) (0.86f + norm * 0.10f)
          else (0.62f + norm * 0.30f)
        topSpecularAlpha = 0.40f + norm * 0.28f
        // Realistic frosted grain & dynamic behind-window blur peak in the translucent glass zone
        val bell = 1f - kotlin.math.abs(norm - 0.58f) / 0.58f
        frostGrainAlpha = (0.024f + bell.coerceAtLeast(0f) * 0.042f)
        innerRefractionAlpha =
          if (isPitchBlackGold) (0.03f + bell.coerceAtLeast(0f) * 0.06f)
          else (0.05f + bell.coerceAtLeast(0f) * 0.10f)
        enableWindowBlur = true
        blurBehindRadiusPx = (10f + (norm * 52f)).roundToInt().coerceIn(8, 60)
        buttonScale = 0.22f + norm * 0.92f
      }
    }
  }

  val scaledPalette =
    basePalette.copy(
      cardSurface = baseSurfaceColor.copy(alpha = (surfaceAlpha * 0.85f).coerceIn(0.02f, 0.96f)),
      cardSurfaceElevated = elevatedSurfaceColor.copy(alpha = elevatedSurfaceAlpha),
      buttonContainerColor =
        basePalette.buttonContainerColor.copy(
          alpha =
            when {
              effectiveOpacity <= 0.02f -> 0.08f
              effectiveOpacity >= 0.94f -> 0.98f
              else -> (0.20f + effectiveOpacity * 0.74f).coerceIn(0.14f, 0.92f)
            }
        ),
      glowOrbPrimary = Color.Transparent,
      glowOrbSecondary = Color.Transparent,
      glassPlayButtonBrush =
        if (isPitchBlackGold || isNeutral) {
          Brush.linearGradient(
            listOf(
              elevatedSurfaceColor.copy(alpha = (0.82f * buttonScale).coerceIn(0.06f, 0.98f)),
              baseSurfaceColor.copy(alpha = (0.90f * buttonScale).coerceIn(0.06f, 1.0f)),
            )
          )
        } else {
          Brush.linearGradient(
            listOf(
              accentIndicatorColor.copy(alpha = (0.28f * buttonScale).coerceIn(0.04f, 0.45f)),
              accentTrackColor.copy(alpha = (0.48f * buttonScale).coerceIn(0.06f, 0.72f)),
              baseSurfaceColor.copy(alpha = (0.58f * buttonScale).coerceIn(0.04f, 0.92f)),
            )
          )
        },
    )

  return FloatingWidgetVisualSpec(
    effectiveColorMode = effectiveColorMode,
    glassMode = glassMode,
    effectiveOpacity = effectiveOpacity,
    isNeutral = isNeutral,
    isPitchBlackGold = isPitchBlackGold,
    baseSurfaceColor = baseSurfaceColor,
    elevatedSurfaceColor = elevatedSurfaceColor,
    topGradientColor = topGradientColor,
    borderColor = borderColor,
    topSpecularColor = topSpecularColor,
    accentIndicatorColor = accentIndicatorColor,
    accentTrackColor = accentTrackColor,
    textPrimary = textPrimary,
    textSecondary = textSecondary,
    iconTint = iconTint,
    primaryButtonIconTint = primaryButtonIconTint,
    surfaceAlpha = surfaceAlpha,
    elevatedSurfaceAlpha = elevatedSurfaceAlpha,
    topGradientAlpha = topGradientAlpha,
    bottomGradientAlpha = bottomGradientAlpha,
    borderAlpha = borderAlpha,
    topSpecularAlpha = topSpecularAlpha,
    frostGrainAlpha = frostGrainAlpha,
    innerRefractionAlpha = innerRefractionAlpha,
    buttonOpacityScale = buttonScale,
    enableWindowBlur = enableWindowBlur,
    blurBehindRadiusPx = blurBehindRadiusPx,
    widgetPalette = scaledPalette,
  )
}

private fun buildNeutralWidgetPalette(): MPlayerPalette {
  val base = resolvePalette(AppThemeStyle.DEEP_BURGUNDY, true)
  val neutralSurface = Color(0xCC141418)
  val neutralElevated = Color(0xE61E1E24)
  val neutralBorder =
    Brush.linearGradient(
      listOf(
        Color(0x99FFFFFF),
        Color(0x44FFFFFF),
        Color(0x88FFFFFF),
      )
    )
  return base.copy(
    backgroundTop = Color(0xFF0C0C10),
    backgroundBottom = Color(0xFF08080B),
    glowOrbPrimary = Color.Transparent,
    glowOrbSecondary = Color.Transparent,
    cardSurface = neutralSurface,
    cardSurfaceElevated = neutralElevated,
    borderBrush = neutralBorder,
    buttonContainerColor = Color(0x55FFFFFF),
    buttonContentColor = Color.White,
    buttonBorderColor = Color(0xCCFFFFFF),
    accentPrimary = Color(0xFFCBD5E1),
    accentGlow = Color.White,
    onAccent = Color(0xFF0B0B0E),
    textPrimary = Color(0xFFF7F7FA),
    textSecondary = Color(0xFFB8B8C4),
    sheetAccentText = Color.White,
    sheetActiveAccent = Color(0xFFE2E8F0),
    sheetSelectedFill = Color(0x4AFFFFFF),
    sheetSelectedBorder = Color(0xCCFFFFFF),
    sheetTextPrimary = Color(0xFFF7F7FA),
    sheetTextSecondary = Color(0xFFB8B8C4),
    vizPrimary = Color.White,
    vizBase = Color(0xFFCBD5E1),
    vizMid = Color(0xFFE2E8F0),
    vizBack = Color(0xFF64748B),
    vizLattice = Color(0xCCFFFFFF),
    vizOuterRing = Color(0xCCFFFFFF),
    vizCenterBadgeFill = Color(0xFF141418),
  )
}
