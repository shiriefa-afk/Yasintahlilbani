package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
  primary = DeepEmerald,
  onPrimary = Color.White,
  primaryContainer = EmeraldContainer,
  onPrimaryContainer = OnEmeraldContainer,
  secondary = AntiqueGold,
  onSecondary = Color.White,
  secondaryContainer = GoldContainer,
  onSecondaryContainer = OnGoldContainer,
  tertiary = SoftSage,
  onTertiary = Color.White,
  tertiaryContainer = SageContainer,
  onTertiaryContainer = DeepCharcoalInk,
  background = ParchmentCream,
  onBackground = DeepCharcoalInk,
  surface = SoftIvory,
  onSurface = DeepCharcoalInk,
  surfaceVariant = WarmSand,
  onSurfaceVariant = MutedSlateInk,
  surfaceContainerLowest = SoftIvory,
  surfaceContainerLow = ParchmentCream,
  surfaceContainer = WarmSand,
  surfaceContainerHigh = WarmSandHigh,
  outline = OutlineSand,
  outlineVariant = WarmSandHigh,
  error = ErrorCrimson,
  onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
  primary = SageEmeraldLight,
  onPrimary = EmeraldDark,
  primaryContainer = NightEmeraldContainer,
  onPrimaryContainer = NightOnEmeraldContainer,
  secondary = WarmGoldLight,
  onSecondary = Color(0xFF2B2004),
  secondaryContainer = NightGoldContainer,
  onSecondaryContainer = NightOnGoldContainer,
  tertiary = SageEmeraldLight,
  onTertiary = EmeraldDark,
  background = ForestNight,
  onBackground = WarmIvoryText,
  surface = NightEmeraldCard,
  onSurface = WarmIvoryText,
  surfaceVariant = NightSurfaceVariant,
  onSurfaceVariant = MutedSageText,
  surfaceContainerLowest = ForestNight,
  surfaceContainerLow = NightEmeraldCard,
  surfaceContainer = NightSurfaceVariant,
  surfaceContainerHigh = NightSurfaceHigh,
  outline = NightOutline,
  outlineVariant = NightSurfaceHigh,
  error = NightError,
  onError = Color(0xFF601410),
)

private val AppShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun YasinTahlilTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    shapes = AppShapes,
    content = content,
  )
}
