package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1D1B20),
    surface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFF2B2830),
    onPrimary = SkillRushOnPrimaryContainer,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SkillRushPrimary,
    onPrimary = SkillRushOnPrimary,
    primaryContainer = SkillRushPrimaryContainer,
    onPrimaryContainer = SkillRushOnPrimaryContainer,
    secondary = SkillRushSecondary,
    secondaryContainer = SkillRushSecondaryContainer,
    onSecondaryContainer = SkillRushOnSecondaryContainer,
    background = SkillRushBackground,
    surface = SkillRushSurface,
    surfaceVariant = SkillRushSurfaceVariant,
    onBackground = SkillRushOnSurface,
    onSurface = SkillRushOnSurface,
    onSurfaceVariant = SkillRushOnSurfaceVariant,
    outline = SkillRushOutline,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
