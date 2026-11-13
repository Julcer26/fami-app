package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = FamiEmerald,
    background = PaperDeskBg,
    surface = PaperSheetWhite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = FamiMidnight,
    onSurface = FamiMidnight,
    surfaceVariant = PaperSheetCream,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = FamiMidnight,
    outlineVariant = PaperRuleLine,
    primaryContainer = PaperStickyPeach,
    onPrimaryContainer = FamiMidnight
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = FamiEmerald,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceElevated,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DarkOnBackground,
    onSurface = DarkOnBackground,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = Color(0xFF28316B),
    primaryContainer = FamiCoralContainer,
    onPrimaryContainer = FamiCoralLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Default to Fami Flat Paper Notebook light mode!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
