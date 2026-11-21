package com.example.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainTab
import com.example.model.AppThemeStyle
import com.example.ui.theme.LocalMPlayerPalette
import com.example.ui.theme.MetallicGoldBright
import com.example.ui.theme.MetallicGoldDark

@Composable
fun FloatingGlassBottomNavBar(
  selectedTab: MainTab,
  tabs: List<MainTab>,
  labelForTab: (MainTab) -> String,
  iconForTab: (MainTab, Boolean) -> ImageVector,
  onSelectTab: (MainTab) -> Unit,
  isPlaying: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val palette = LocalMPlayerPalette.current
  val isMinimalPlainBlack = palette.isMinimalPlainBlack
  val isPitchBlackGold = palette.themeStyle == AppThemeStyle.PITCH_BLACK_GOLD
  val isWhiteContrastTheme =
    palette.themeStyle == AppThemeStyle.ROYAL_PURPLE ||
      palette.themeStyle == AppThemeStyle.MIDNIGHT_BLUE ||
      isMinimalPlainBlack

  val opacityFactor = (palette.appGlassOpacity / 0.55f).coerceIn(0.08f, 1.82f)
  val pillShape = RoundedCornerShape(36.dp)

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 14.dp)
        .padding(top = 4.dp, bottom = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.widthIn(max = 560.dp)
          .fillMaxWidth()
          .height(72.dp)
          .clip(pillShape)
          .then(
            if (isMinimalPlainBlack) {
              Modifier.background(Color.Black)
            } else {
              Modifier.background(
                Brush.verticalGradient(
                  listOf(
                    palette.navBarBackground.copy(
                      alpha = (palette.navBarBackground.alpha * 1.04f).coerceIn(0.05f, 0.94f)
                    ),
                    palette.cardSurface.copy(
                      alpha = (0.48f * opacityFactor).coerceIn(0.04f, 0.94f)
                    ),
                  )
                )
              )
            }
          )
          .border(width = palette.borderStrokeWidth, brush = palette.borderBrush, shape = pillShape)
          .testTag("floating_glass_nav_pill"),
    ) {
      if (!isMinimalPlainBlack) {
        // Frosted glass refraction layer behind nav icons and labels
        Box(
          modifier =
            Modifier.matchParentSize()
              .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(22.dp) else Modifier)
              .background(
                if (isPitchBlackGold) {
                  Brush.horizontalGradient(
                    listOf(
                      Color(0x990A0A0A),
                      Color(0xB8050505),
                      Color(0x990A0A0A),
                    )
                  )
                } else {
                  val sheenScale = minOf(opacityFactor, 1.35f)
                  Brush.horizontalGradient(
                    listOf(
                      palette.accentPrimary.copy(alpha = (0.20f * sheenScale).coerceIn(0.02f, 0.34f)),
                      palette.accentGlow.copy(alpha = (0.10f * sheenScale).coerceIn(0.01f, 0.22f)),
                      palette.accentPrimary.copy(alpha = (0.20f * sheenScale).coerceIn(0.02f, 0.34f)),
                    )
                  )
                }
              )
        )

        FrostedGlassGrainOverlay(
          grainAlpha = 0.032f * minOf(opacityFactor, 1.25f),
          isPitchBlackGold = isPitchBlackGold,
          modifier = Modifier.matchParentSize(),
        )

        // Specular top capsule highlight edge
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 24.dp)
              .height(1.dp)
              .align(Alignment.TopCenter)
              .background(
                Brush.horizontalGradient(
                  listOf(
                    Color.Transparent,
                    palette.accentGlow.copy(alpha = 0.65f),
                    Color.White.copy(alpha = if (isPitchBlackGold) 0.12f else 0.26f),
                    palette.accentGlow.copy(alpha = 0.65f),
                    Color.Transparent,
                  )
                )
              )
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        tabs.forEach { tab ->
          val isSelected = selectedTab == tab
          val label = labelForTab(tab)
          val icon = iconForTab(tab, isSelected)
          val isCenterPlayer = tab == MainTab.PLAYER

          val activeColor =
            when {
              isMinimalPlainBlack -> Color.White
              isPitchBlackGold -> palette.accentGlow
              isWhiteContrastTheme -> Color.White
              else -> palette.accentGlow
            }
          val inactiveColor =
            when {
              isMinimalPlainBlack -> Color.White.copy(alpha = 0.78f)
              isPitchBlackGold -> palette.textSecondary
              isWhiteContrastTheme -> Color.White.copy(alpha = 0.86f)
              else -> palette.textPrimary.copy(alpha = 0.78f)
            }
          val ringBrush =
            when {
              isMinimalPlainBlack -> Brush.linearGradient(listOf(Color.White, Color.White))
              isPitchBlackGold ->
                Brush.linearGradient(
                  listOf(MetallicGoldBright, MetallicGoldDark, MetallicGoldBright)
                )
              else ->
                Brush.linearGradient(
                  listOf(
                    Color.White.copy(alpha = 0.88f),
                    palette.accentGlow,
                    palette.accentPrimary.copy(alpha = 0.85f),
                  )
                )
            }

          val contentTint by
            animateColorAsState(
              targetValue = if (isSelected) activeColor else inactiveColor,
              animationSpec = tween(durationMillis = 180),
              label = "navTabTint",
            )
          val indicatorScale by
            animateFloatAsState(
              targetValue = if (isSelected) 1f else 0.90f,
              animationSpec = spring(dampingRatio = 0.74f, stiffness = 420f),
              label = "navRingScale",
            )

          val interactionSource = remember(tab) { MutableInteractionSource() }

          Column(
            modifier =
              Modifier.weight(1f)
                .fillMaxHeight()
                .padding(vertical = 4.dp, horizontal = 2.dp)
                .clip(RoundedCornerShape(24.dp))
                .minimumInteractiveComponentSize()
                .semantics { selected = isSelected }
                .clickable(
                  interactionSource = interactionSource,
                  indication = ripple(bounded = true, color = activeColor),
                  role = Role.Tab,
                  onClick = { onSelectTab(tab) },
                )
                .testTag("nav_${tab.name.lowercase()}"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Box(
              modifier = Modifier.height(40.dp),
              contentAlignment = Alignment.Center,
            ) {
              if (isCenterPlayer) {
                // Visually distinct circular medallion Play button in the center of the pill
                val outerHaloModifier =
                  if (isSelected && !isMinimalPlainBlack) {
                    Modifier.border(
                      width = 1.dp,
                      color = activeColor.copy(alpha = 0.42f),
                      shape = CircleShape,
                    )
                  } else {
                    Modifier
                  }
                Box(
                  modifier =
                    Modifier.size(40.dp)
                      .scale(if (isSelected) 1.04f else 0.98f)
                      .then(outerHaloModifier)
                      .padding(if (isSelected && !isMinimalPlainBlack) 1.5.dp else 0.dp)
                      .clip(CircleShape)
                      .background(
                        when {
                          isMinimalPlainBlack && isSelected ->
                            androidx.compose.ui.graphics.SolidColor(Color(0xFF1F1F1F))
                          isMinimalPlainBlack ->
                            androidx.compose.ui.graphics.SolidColor(Color.Black)
                          isSelected -> palette.activeOptionPillBrush
                          else -> palette.glassPlayButtonBrush
                        }
                      )
                      .border(
                        width = if (isSelected) 1.8.dp else 1.15.dp,
                        brush =
                          if (isSelected) ringBrush
                          else Brush.linearGradient(
                            listOf(
                              palette.buttonBorderColor.copy(alpha = 0.72f),
                              palette.accentGlow.copy(alpha = 0.42f),
                            )
                          ),
                        shape = CircleShape,
                      )
                      .testTag("nav_center_player_medallion"),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint =
                      when {
                        isMinimalPlainBlack -> Color.White
                        isPitchBlackGold -> MetallicGoldBright
                        isSelected -> Color.White
                        isPlaying -> palette.accentGlow
                        else -> Color.White.copy(alpha = 0.94f)
                      },
                    modifier = Modifier.size(23.dp),
                  )
                }
              } else {
                // Side tabs with clean circular/pill ring indicator when selected
                val ringFillModifier =
                  if (isSelected) {
                    if (isMinimalPlainBlack) {
                      Modifier.background(Color(0xFF1A1A1A))
                    } else {
                      Modifier.background(palette.navPillFillBrush)
                        .background(palette.accentGlow.copy(alpha = 0.14f))
                    }
                  } else {
                    Modifier
                  }
                val ringBorderModifier =
                  if (isSelected) {
                    Modifier.border(
                      width = 1.5.dp,
                      brush = ringBrush,
                      shape = CircleShape,
                    )
                  } else {
                    Modifier
                  }
                Box(
                  modifier =
                    Modifier.size(width = 46.dp, height = 32.dp)
                      .scale(indicatorScale)
                      .clip(CircleShape)
                      .then(ringFillModifier)
                      .then(ringBorderModifier),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentTint,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
              text = label,
              style = MaterialTheme.typography.labelSmall,
              fontSize = 10.5.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = contentTint,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.defaultMinSize(minHeight = 14.dp),
            )
          }
        }
      }
    }
  }
}
