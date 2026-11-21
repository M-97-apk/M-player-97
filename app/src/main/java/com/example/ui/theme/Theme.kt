package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.localization.LocalAppStrings
import com.example.localization.stringsForLanguage
import com.example.model.AppLanguage
import com.example.model.AppThemeStyle

@Immutable
data class MPlayerPalette(
  val themeStyle: AppThemeStyle,
  val isDark: Boolean,
  val isGlassTheme: Boolean,
  val isMinimalPlainBlack: Boolean = false,
  val appGlassOpacity: Float = 0.55f,
  val backgroundTop: Color,
  val backgroundBottom: Color,
  val glowOrbPrimary: Color,
  val glowOrbSecondary: Color,
  val cardSurface: Color,
  val cardSurfaceElevated: Color,
  val borderBrush: Brush,
  val outerRimGlowColor: Color,
  val buttonContainerColor: Color,
  val buttonContentColor: Color,
  val buttonBorderColor: Color,
  val accentPrimary: Color,
  val accentGlow: Color,
  val onAccent: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val helperIconColor: Color,
  val sectionHelperIconColor: Color = accentGlow,
  val sectionHelperTextColor: Color = textPrimary,
  val borderStrokeWidth: Dp,
  val navBarBackground: Color,
  val glassCardFrostBrush: Brush,
  val glassButtonFillBrush: Brush,
  val glassButtonRimBrush: Brush,
  val glassBadgeFillBrush: Brush,
  val glassBadgeActiveFillBrush: Brush,
  val glassBadgeRimBrush: Brush,
  val glassPlayButtonBrush: Brush,
  val activeOptionPillBrush: Brush,
  val activeOptionGlowColor: Color,
  val navPillFillBrush: Brush,
  val navPillRimBrush: Brush,
  val sheetBg: Color,
  val sheetCardFill: Color,
  val sheetCardBorder: Brush,
  val sheetAccentText: Color,
  val sheetActiveAccent: Color,
  val sheetSelectedFill: Color,
  val sheetSelectedBorder: Color,
  val sheetIconBadgeFill: Color,
  val sheetTextPrimary: Color,
  val sheetTextSecondary: Color,
  val vizPrimary: Color,
  val vizBase: Color,
  val vizMid: Color,
  val vizBack: Color,
  val vizLattice: Color,
  val vizOuterRing: Color,
  val vizCenterBadgeFill: Color,
)

private fun scaleGlassColor(color: Color, opacityFactor: Float, minAlpha: Float = 0.02f): Color =
  if (color == Color.Transparent || kotlin.math.abs(opacityFactor - 1f) < 0.001f) {
    color
  } else {
    color.copy(alpha = (color.alpha * opacityFactor).coerceIn(minAlpha, 0.96f))
  }

private fun scaleGlassAlpha(baseAlpha: Float, opacityFactor: Float, minAlpha: Float = 0.02f): Float =
  (baseAlpha * opacityFactor).coerceIn(minAlpha, 0.96f)

private fun buildLuxuryGlassPalette(
  themeStyle: AppThemeStyle,
  backgroundTop: Color,
  backgroundBottom: Color,
  glowOrbPrimary: Color,
  glowOrbSecondary: Color,
  cardSurface: Color,
  cardSurfaceElevated: Color,
  accentDark: Color,
  accentBright: Color,
  accentHighlight: Color,
  specularRimColor: Color,
  textPrimary: Color,
  textSecondary: Color,
  helperIconColor: Color = textSecondary,
  sectionHelperIconColor: Color = accentHighlight,
  sectionHelperTextColor: Color = textPrimary,
  vizBackColor: Color,
  borderEdgeColors: List<Color>,
  glassOpacity: Float = 0.55f,
): MPlayerPalette {
  val clampedOpacity = glassOpacity.coerceIn(0.05f, 1.0f)
  val opacityFactor = clampedOpacity / 0.55f
  val specularBorder = Brush.linearGradient(borderEdgeColors)
  val sheetBorder =
    Brush.linearGradient(
      listOf(
        specularRimColor.copy(alpha = 0.24f),
        specularRimColor.copy(alpha = 0.08f),
        accentBright.copy(alpha = 0.56f),
      )
    )

  return MPlayerPalette(
    themeStyle = themeStyle,
    isDark = true,
    isGlassTheme = true,
    isMinimalPlainBlack = false,
    appGlassOpacity = clampedOpacity,
    backgroundTop = backgroundTop,
    backgroundBottom = backgroundBottom,
    glowOrbPrimary = glowOrbPrimary,
    glowOrbSecondary = glowOrbSecondary,
    cardSurface = scaleGlassColor(cardSurface, opacityFactor),
    cardSurfaceElevated = scaleGlassColor(cardSurfaceElevated, opacityFactor),
    borderBrush = specularBorder,
    outerRimGlowColor = Color.Transparent,
    buttonContainerColor = accentDark,
    buttonContentColor = textPrimary,
    buttonBorderColor = accentHighlight.copy(alpha = 0.72f),
    accentPrimary = accentDark,
    accentGlow = accentHighlight,
    onAccent = textPrimary,
    textPrimary = textPrimary,
    textSecondary = textSecondary,
    helperIconColor = helperIconColor,
    sectionHelperIconColor = sectionHelperIconColor,
    sectionHelperTextColor = sectionHelperTextColor,
    borderStrokeWidth = 1.dp,
    navBarBackground = backgroundTop.copy(alpha = scaleGlassAlpha(0.48f, opacityFactor, 0.05f)),
    glassCardFrostBrush =
      Brush.linearGradient(
        listOf(
          specularRimColor.copy(alpha = scaleGlassAlpha(0.07f, opacityFactor, 0.01f)),
          cardSurface.copy(alpha = scaleGlassAlpha(0.36f, opacityFactor)),
          backgroundBottom.copy(alpha = scaleGlassAlpha(0.48f, opacityFactor)),
        )
      ),
    glassButtonFillBrush =
      Brush.linearGradient(
        listOf(
          cardSurfaceElevated.copy(alpha = scaleGlassAlpha(0.62f, opacityFactor)),
          accentDark.copy(alpha = scaleGlassAlpha(0.34f, opacityFactor)),
          backgroundBottom.copy(alpha = scaleGlassAlpha(0.78f, opacityFactor)),
        )
      ),
    glassButtonRimBrush = specularBorder,
    glassBadgeFillBrush =
      Brush.linearGradient(
        listOf(
          specularRimColor.copy(alpha = scaleGlassAlpha(0.04f, opacityFactor, 0.01f)),
          cardSurface.copy(alpha = scaleGlassAlpha(0.44f, opacityFactor)),
          backgroundBottom.copy(alpha = scaleGlassAlpha(0.60f, opacityFactor)),
        )
      ),
    glassBadgeActiveFillBrush =
      Brush.linearGradient(
        listOf(
          accentDark.copy(alpha = scaleGlassAlpha(0.46f, opacityFactor)),
          cardSurfaceElevated.copy(alpha = scaleGlassAlpha(0.66f, opacityFactor)),
          backgroundTop.copy(alpha = scaleGlassAlpha(0.80f, opacityFactor)),
        )
      ),
    glassBadgeRimBrush = specularBorder,
    glassPlayButtonBrush =
      Brush.linearGradient(
        listOf(
          cardSurfaceElevated.copy(alpha = scaleGlassAlpha(0.86f, opacityFactor)),
          accentDark.copy(alpha = scaleGlassAlpha(0.46f, opacityFactor)),
          backgroundBottom.copy(alpha = scaleGlassAlpha(0.92f, opacityFactor)),
        )
      ),
    activeOptionPillBrush =
      Brush.linearGradient(
        listOf(
          accentHighlight.copy(alpha = scaleGlassAlpha(0.18f, opacityFactor, 0.06f)),
          accentBright.copy(alpha = scaleGlassAlpha(0.42f, opacityFactor, 0.12f)),
          accentDark.copy(alpha = scaleGlassAlpha(0.58f, opacityFactor, 0.16f)),
        )
      ),
    activeOptionGlowColor = accentHighlight.copy(alpha = (0.28f * minOf(opacityFactor, 1.35f)).coerceIn(0.08f, 0.42f)),
    navPillFillBrush =
      Brush.linearGradient(
        listOf(
          cardSurfaceElevated.copy(alpha = scaleGlassAlpha(0.56f, opacityFactor)),
          accentDark.copy(alpha = scaleGlassAlpha(0.38f, opacityFactor)),
          backgroundBottom.copy(alpha = scaleGlassAlpha(0.78f, opacityFactor)),
        )
      ),
    navPillRimBrush = specularBorder,
    sheetBg = backgroundBottom.copy(alpha = scaleGlassAlpha(0.84f, opacityFactor, 0.20f)),
    sheetCardFill = cardSurfaceElevated.copy(alpha = scaleGlassAlpha(0.54f, opacityFactor)),
    sheetCardBorder = sheetBorder,
    sheetAccentText = accentHighlight,
    sheetActiveAccent = accentBright,
    sheetSelectedFill = accentDark.copy(alpha = scaleGlassAlpha(0.66f, opacityFactor, 0.16f)),
    sheetSelectedBorder = accentHighlight,
    sheetIconBadgeFill = cardSurfaceElevated,
    sheetTextPrimary = textPrimary,
    sheetTextSecondary = textSecondary,
    vizPrimary = accentHighlight,
    vizBase = accentDark,
    vizMid = accentBright,
    vizBack = vizBackColor,
    vizLattice = accentHighlight.copy(alpha = 0.70f),
    vizOuterRing = accentHighlight.copy(alpha = 0.78f),
    vizCenterBadgeFill = backgroundBottom,
  )
}

internal fun resolvePalette(
  themeStyle: AppThemeStyle,
  isDark: Boolean = true,
  glassOpacity: Float = 0.55f,
): MPlayerPalette {
  val clampedOpacity = glassOpacity.coerceIn(0.05f, 1.0f)
  val opacityFactor = clampedOpacity / 0.55f

  return when (themeStyle) {
    // 1. Deep Royal Burgundy: pure ultra-dark wine frosted glass with crisp white/silver highlights (zero pink, zero gold)
    AppThemeStyle.DEEP_BURGUNDY ->
      buildLuxuryGlassPalette(
        themeStyle = themeStyle,
        backgroundTop = DeepBurgundyBgTop,
        backgroundBottom = DeepBurgundyBgBottom,
        glowOrbPrimary = DeepBurgundyOrbPrimary,
        glowOrbSecondary = DeepBurgundyOrbSecondary,
        cardSurface = DeepBurgundySurface,
        cardSurfaceElevated = DeepBurgundySurfaceElevated,
        accentDark = DeepBurgundyAccent,
        accentBright = DeepBurgundyAccentBright,
        accentHighlight = DeepBurgundyGlowHighlight,
        specularRimColor = DeepBurgundyGlowHighlight,
        textPrimary = DeepBurgundyTextPrimary,
        textSecondary = DeepBurgundyTextSecondary,
        helperIconColor = DeepBurgundyTextSecondary,
        vizBackColor = Color(0xFF290509),
        borderEdgeColors =
          listOf(
            Color(0x52F6F2F3),
            DeepBurgundyAccentBright.copy(alpha = 0.78f),
            DeepBurgundyGlowHighlight.copy(alpha = 0.40f),
            DeepBurgundyAccentBright.copy(alpha = 0.56f),
          ),
        glassOpacity = clampedOpacity,
      )

    // 2. Deep Night Purple: pure ultra-dark amethyst frosted glass with crisp white helper icons & labels
    AppThemeStyle.ROYAL_PURPLE ->
      buildLuxuryGlassPalette(
        themeStyle = themeStyle,
        backgroundTop = RoyalPurpleBgTop,
        backgroundBottom = RoyalPurpleBgBottom,
        glowOrbPrimary = RoyalPurpleOrbPrimary,
        glowOrbSecondary = RoyalPurpleOrbSecondary,
        cardSurface = RoyalPurpleSurface,
        cardSurfaceElevated = RoyalPurpleSurfaceElevated,
        accentDark = RoyalPurpleAccent,
        accentBright = RoyalPurpleAccentBright,
        accentHighlight = RoyalPurpleGlowHighlight,
        specularRimColor = RoyalPurpleTextPrimary,
        textPrimary = RoyalPurpleTextPrimary,
        textSecondary = RoyalPurpleTextSecondary,
        helperIconColor = Color.White,
        sectionHelperIconColor = Color.White,
        sectionHelperTextColor = Color.White,
        vizBackColor = Color(0xFF220742),
        borderEdgeColors =
          listOf(
            Color(0x4AF2EEFA),
            RoyalPurpleAccentBright.copy(alpha = 0.74f),
            RoyalPurpleGlowHighlight.copy(alpha = 0.46f),
            RoyalPurpleAccentBright.copy(alpha = 0.54f),
          ),
        glassOpacity = clampedOpacity,
      )

    // 3. Deep Midnight Blue: pure ultra-dark sapphire frosted glass with crisp white helper icons & labels
    AppThemeStyle.MIDNIGHT_BLUE ->
      buildLuxuryGlassPalette(
        themeStyle = themeStyle,
        backgroundTop = MidnightBlueBgTop,
        backgroundBottom = MidnightBlueBgBottom,
        glowOrbPrimary = MidnightBlueOrbPrimary,
        glowOrbSecondary = MidnightBlueOrbSecondary,
        cardSurface = MidnightBlueSurface,
        cardSurfaceElevated = MidnightBlueSurfaceElevated,
        accentDark = MidnightBlueAccent,
        accentBright = MidnightBlueAccentBright,
        accentHighlight = MidnightBlueGlowHighlight,
        specularRimColor = MidnightBlueTextPrimary,
        textPrimary = MidnightBlueTextPrimary,
        textSecondary = MidnightBlueTextSecondary,
        helperIconColor = Color.White,
        sectionHelperIconColor = Color.White,
        sectionHelperTextColor = Color.White,
        vizBackColor = Color(0xFF07163B),
        borderEdgeColors =
          listOf(
            Color(0x4AEEF3FA),
            MidnightBlueAccentBright.copy(alpha = 0.74f),
            MidnightBlueGlowHighlight.copy(alpha = 0.46f),
            MidnightBlueAccentBright.copy(alpha = 0.54f),
          ),
        glassOpacity = clampedOpacity,
      )

    // 4. Pitch-Black & Metallic Gold (Untouched gold-accent theme): smoked obsidian glass + #D4AF37 rims
    AppThemeStyle.PITCH_BLACK_GOLD ->
      buildLuxuryGlassPalette(
        themeStyle = themeStyle,
        backgroundTop = PitchBlackBgTop,
        backgroundBottom = PitchBlackBgBottom,
        glowOrbPrimary = PitchBlackOrbPrimary,
        glowOrbSecondary = PitchBlackOrbSecondary,
        cardSurface = PitchBlackSurface,
        cardSurfaceElevated = PitchBlackSurfaceElevated,
        accentDark = MetallicGoldDark,
        accentBright = MetallicGoldBright,
        accentHighlight = MetallicGoldHighlight,
        specularRimColor = MetallicGoldBright,
        textPrimary = PitchBlackTextPrimary,
        textSecondary = PitchBlackTextSecondary,
        helperIconColor = PitchBlackTextSecondary,
        vizBackColor = MetallicGoldBevelShadow,
        borderEdgeColors =
          listOf(
            MetallicGoldBright.copy(alpha = 0.88f),
            MetallicGoldDark.copy(alpha = 0.68f),
            MetallicGoldBright.copy(alpha = 0.92f),
            MetallicGoldDark.copy(alpha = 0.72f),
          ),
        glassOpacity = 0.55f,
      ).copy(
        appGlassOpacity = 0.55f,
        navBarBackground = Color(0xFF0A0A0A).copy(alpha = 0.47f),
        buttonContainerColor = Color(0xFF0A0A0A),
        buttonContentColor = MetallicGoldBright,
        buttonBorderColor = MetallicGoldBright,
        onAccent = Color(0xFF000000),
        glassCardFrostBrush =
          Brush.linearGradient(
            listOf(
              Color(0xFFFFFFFF).copy(alpha = 0.07f),
              Color(0xFF0A0A0A).copy(alpha = 0.36f),
              Color(0xFF020202).copy(alpha = 0.50f),
            )
          ),
        glassButtonFillBrush =
          Brush.linearGradient(
            listOf(
              Color(0xFF161616).copy(alpha = 0.60f),
              Color(0xFF0A0A0A).copy(alpha = 0.70f),
              Color(0xFF020202).copy(alpha = 0.80f),
            )
          ),
        glassBadgeFillBrush =
          Brush.linearGradient(
            listOf(
              Color(0xFFFFFFFF).copy(alpha = 0.12f),
              Color(0xFF0C0C0C).copy(alpha = 0.40f),
              Color(0xFF030303).copy(alpha = 0.53f),
            )
          ),
        glassBadgeActiveFillBrush =
          Brush.linearGradient(
            listOf(
              Color(0xFF1A1A1A).copy(alpha = 0.67f),
              Color(0xFF0C0C0C).copy(alpha = 0.80f),
              Color(0xFF030303).copy(alpha = 0.88f),
            )
          ),
        activeOptionPillBrush =
          Brush.linearGradient(
            listOf(
              Color(0xFF1A1A1A).copy(alpha = 0.67f),
              Color(0xFF0C0C0C).copy(alpha = 0.80f),
              Color(0xFF030303).copy(alpha = 0.88f),
            )
          ),
        activeOptionGlowColor = Color.Transparent,
        glassPlayButtonBrush =
          Brush.linearGradient(
            listOf(
              Color(0xFF181818).copy(alpha = 0.72f),
              Color(0xFF0C0C0C).copy(alpha = 0.84f),
              Color(0xFF030303).copy(alpha = 0.92f),
            )
          ),
        navPillFillBrush =
          Brush.linearGradient(
            listOf(
              Color(0xFF141414).copy(alpha = 0.60f),
              Color(0xFF0A0A0A).copy(alpha = 0.74f),
              Color(0xFF020202).copy(alpha = 0.84f),
            )
          ),
        sheetBg = Color(0xFF050505).copy(alpha = 0.85f),
        sheetCardFill = Color(0xFF0E0E0E).copy(alpha = 0.55f),
        sheetCardBorder =
          Brush.linearGradient(
            listOf(
              MetallicGoldBright.copy(alpha = 0.78f),
              MetallicGoldDark.copy(alpha = 0.56f),
              MetallicGoldBright.copy(alpha = 0.78f),
            )
          ),
        sheetSelectedFill = Color(0xFF181818).copy(alpha = 0.60f),
        sheetSelectedBorder = MetallicGoldBright,
        sheetIconBadgeFill = Color(0xFF0A0A0A),
      )

    // 5. Luxury Dark Brown (البني الفاخر)
    AppThemeStyle.LUXURY_DARK_BROWN ->
      buildLuxuryGlassPalette(
        themeStyle = themeStyle,
        backgroundTop = LuxuryBrownBgTop,
        backgroundBottom = LuxuryBrownBgBottom,
        glowOrbPrimary = LuxuryBrownOrbPrimary,
        glowOrbSecondary = LuxuryBrownOrbSecondary,
        cardSurface = LuxuryBrownSurface,
        cardSurfaceElevated = LuxuryBrownSurfaceElevated,
        accentDark = LuxuryBrownAccent,
        accentBright = LuxuryBrownAccentBright,
        accentHighlight = LuxuryBrownGlowHighlight,
        specularRimColor = LuxuryBrownGlowHighlight,
        textPrimary = LuxuryBrownTextPrimary,
        textSecondary = LuxuryBrownTextSecondary,
        helperIconColor = LuxuryBrownTextSecondary,
        vizBackColor = Color(0xFF26130A),
        borderEdgeColors =
          listOf(
            Color(0x4AF4EDE7),
            LuxuryBrownAccentBright.copy(alpha = 0.76f),
            LuxuryBrownGlowHighlight.copy(alpha = 0.44f),
            LuxuryBrownAccentBright.copy(alpha = 0.56f),
          ),
        glassOpacity = clampedOpacity,
      ).copy(
        navBarBackground = Color(0xFF140A05).copy(alpha = scaleGlassAlpha(0.50f, opacityFactor, 0.05f)),
        buttonContainerColor = LuxuryBrownAccent,
        buttonContentColor = LuxuryBrownTextPrimary,
        buttonBorderColor = LuxuryBrownGlowHighlight.copy(alpha = 0.74f),
        glassCardFrostBrush =
          Brush.linearGradient(
            listOf(
              Color(0xFFF4EDE7).copy(alpha = scaleGlassAlpha(0.086f, opacityFactor, 0.01f)),
              Color(0xFF1D0F08).copy(alpha = scaleGlassAlpha(0.36f, opacityFactor)),
              Color(0xFF26130A).copy(alpha = scaleGlassAlpha(0.47f, opacityFactor)),
            )
          ),
        glassButtonFillBrush =
          Brush.linearGradient(
            listOf(
              Color(0xFF241209).copy(alpha = scaleGlassAlpha(0.65f, opacityFactor)),
              LuxuryBrownAccent.copy(alpha = scaleGlassAlpha(0.44f, opacityFactor)),
              Color(0xFF140A05).copy(alpha = scaleGlassAlpha(0.83f, opacityFactor)),
            )
          ),
        glassBadgeFillBrush =
          Brush.linearGradient(
            listOf(
              Color(0xFFF4EDE7).copy(alpha = scaleGlassAlpha(0.06f, opacityFactor, 0.01f)),
              Color(0xFF1E0F08).copy(alpha = scaleGlassAlpha(0.38f, opacityFactor)),
              Color(0xFF160B06).copy(alpha = scaleGlassAlpha(0.49f, opacityFactor)),
            )
          ),
        glassBadgeActiveFillBrush =
          Brush.linearGradient(
            listOf(
              LuxuryBrownAccent.copy(alpha = scaleGlassAlpha(0.48f, opacityFactor)),
              Color(0xFF2A160C).copy(alpha = scaleGlassAlpha(0.72f, opacityFactor)),
              Color(0xFF140A05).copy(alpha = scaleGlassAlpha(0.85f, opacityFactor)),
            )
          ),
        glassPlayButtonBrush =
          Brush.linearGradient(
            listOf(
              Color(0xFF24120A).copy(alpha = scaleGlassAlpha(0.85f, opacityFactor)),
              LuxuryBrownAccent.copy(alpha = scaleGlassAlpha(0.48f, opacityFactor)),
              Color(0xFF140A05).copy(alpha = scaleGlassAlpha(0.93f, opacityFactor)),
            )
          ),
        sheetBg = Color(0xFF160B06).copy(alpha = scaleGlassAlpha(0.85f, opacityFactor, 0.25f)),
        sheetCardFill = Color(0xFF211109).copy(alpha = scaleGlassAlpha(0.55f, opacityFactor)),
        sheetCardBorder =
          Brush.linearGradient(
            listOf(
              Color(0x45F4EDE7),
              LuxuryBrownAccentBright.copy(alpha = 0.66f),
              LuxuryBrownGlowHighlight.copy(alpha = 0.42f),
            )
          ),
        sheetSelectedFill = Color(0xFF361B0E).copy(alpha = scaleGlassAlpha(0.65f, opacityFactor)),
        sheetSelectedBorder = LuxuryBrownGlowHighlight,
        sheetIconBadgeFill = Color(0xFF1F1008),
      )

    // 6. Cosmic Starfield: Pure black night sky (#000000) with stars & starlight-tinted dark glass
    AppThemeStyle.COSMIC_STARFIELD ->
      buildLuxuryGlassPalette(
        themeStyle = themeStyle,
        backgroundTop = CosmicStarfieldBgTop,
        backgroundBottom = CosmicStarfieldBgBottom,
        glowOrbPrimary = CosmicStarfieldOrbPrimary,
        glowOrbSecondary = CosmicStarfieldOrbSecondary,
        cardSurface = CosmicStarfieldSurface,
        cardSurfaceElevated = CosmicStarfieldSurfaceElevated,
        accentDark = CosmicStarfieldAccent,
        accentBright = CosmicStarfieldAccentBright,
        accentHighlight = CosmicStarfieldGlowHighlight,
        specularRimColor = CosmicStarfieldGlowHighlight,
        textPrimary = CosmicStarfieldTextPrimary,
        textSecondary = CosmicStarfieldTextSecondary,
        helperIconColor = Color.White,
        vizBackColor = Color(0xFF081024),
        borderEdgeColors =
          listOf(
            Color(0x70FFFFFF),
            CosmicStarfieldAccentBright.copy(alpha = 0.72f),
            CosmicStarfieldGlowHighlight.copy(alpha = 0.56f),
            CosmicStarfieldAccentBright.copy(alpha = 0.52f),
          ),
        glassOpacity = clampedOpacity,
      ).copy(
        navBarBackground = Color(0xFF040711).copy(alpha = scaleGlassAlpha(0.54f, opacityFactor, 0.08f)),
        buttonBorderColor = CosmicStarfieldGlowHighlight.copy(alpha = 0.85f),
      )

    // 7. Plain Black: pure #000000 black with crisp #FFFFFF white icons & text and ZERO effects/ornamentation
    AppThemeStyle.PLAIN_BLACK -> {
      val crispWhiteBorder = Brush.linearGradient(listOf(Color.White, Color.White))
      val solidBlackBrush = Brush.linearGradient(listOf(Color.Black, Color.Black))
      val activeMinimalBrush = Brush.linearGradient(listOf(Color(0xFF1A1A1A), Color(0xFF121212)))
      MPlayerPalette(
        themeStyle = themeStyle,
        isDark = true,
        isGlassTheme = false,
        isMinimalPlainBlack = true,
        appGlassOpacity = clampedOpacity,
        backgroundTop = PlainBlackBgTop,
        backgroundBottom = PlainBlackBgBottom,
        glowOrbPrimary = Color.Transparent,
        glowOrbSecondary = Color.Transparent,
        cardSurface = PlainBlackSurface,
        cardSurfaceElevated = PlainBlackSurfaceElevated,
        borderBrush = crispWhiteBorder,
        outerRimGlowColor = Color.Transparent,
        buttonContainerColor = Color.Black,
        buttonContentColor = Color.White,
        buttonBorderColor = Color.White,
        accentPrimary = PlainBlackAccentBright,
        accentGlow = Color.White,
        onAccent = Color.Black,
        textPrimary = Color.White,
        textSecondary = Color.White,
        helperIconColor = Color.White,
        sectionHelperIconColor = Color.White,
        sectionHelperTextColor = Color.White,
        borderStrokeWidth = 1.dp,
        navBarBackground = Color.Black,
        glassCardFrostBrush = solidBlackBrush,
        glassButtonFillBrush = solidBlackBrush,
        glassButtonRimBrush = crispWhiteBorder,
        glassBadgeFillBrush = solidBlackBrush,
        glassBadgeActiveFillBrush = activeMinimalBrush,
        glassBadgeRimBrush = crispWhiteBorder,
        glassPlayButtonBrush = solidBlackBrush,
        activeOptionPillBrush = activeMinimalBrush,
        activeOptionGlowColor = Color.Transparent,
        navPillFillBrush = activeMinimalBrush,
        navPillRimBrush = crispWhiteBorder,
        sheetBg = Color.Black,
        sheetCardFill = Color(0xFF0A0A0A),
        sheetCardBorder = crispWhiteBorder,
        sheetAccentText = Color.White,
        sheetActiveAccent = Color.White,
        sheetSelectedFill = Color(0xFF1A1A1A),
        sheetSelectedBorder = Color.White,
        sheetIconBadgeFill = Color.Black,
        sheetTextPrimary = Color.White,
        sheetTextSecondary = Color.White,
        vizPrimary = Color.White,
        vizBase = Color(0xFF262626),
        vizMid = Color(0xFF888888),
        vizBack = Color(0xFF121212),
        vizLattice = Color.White.copy(alpha = 0.72f),
        vizOuterRing = Color.White,
        vizCenterBadgeFill = Color.Black,
      )
    }
  }
}

val LocalMPlayerPalette = staticCompositionLocalOf {
  resolvePalette(AppThemeStyle.DEEP_BURGUNDY, true)
}

private val AppShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(20.dp),
  large = RoundedCornerShape(28.dp),
)

@Composable
fun MPlayerTheme(
  themeStyle: AppThemeStyle = AppThemeStyle.DEEP_BURGUNDY,
  darkTheme: Boolean = true,
  language: AppLanguage = AppLanguage.EN,
  glassOpacity: Float = 0.55f,
  content: @Composable () -> Unit,
) {
  val palette = resolvePalette(themeStyle, darkTheme, glassOpacity)
  val strings = stringsForLanguage(language)
  val layoutDirection = if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

  val colorScheme =
    darkColorScheme(
      primary = palette.buttonContainerColor,
      onPrimary = palette.buttonContentColor,
      primaryContainer = palette.cardSurfaceElevated,
      onPrimaryContainer = palette.textPrimary,
      secondary = palette.accentGlow,
      onSecondary = palette.onAccent,
      background = palette.backgroundTop,
      onBackground = palette.textPrimary,
      surface = palette.cardSurface,
      onSurface = palette.textPrimary,
      surfaceVariant = palette.cardSurfaceElevated,
      onSurfaceVariant = palette.textSecondary,
      outline = palette.buttonBorderColor,
    )

  CompositionLocalProvider(
    LocalMPlayerPalette provides palette,
    LocalAppStrings provides strings,
    LocalLayoutDirection provides layoutDirection,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = buildAppTypography(language),
      shapes = AppShapes,
      content = content,
    )
  }
}
