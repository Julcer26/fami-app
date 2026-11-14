package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class FamiPaperPalette(
    val isDark: Boolean,
    val deskBg: Color,
    val sheet: Color,
    val sheetCream: Color,
    val ink: Color,
    val inkMuted: Color,
    val border: Color,
    val shadow3D: Color,
    val ruleLine: Color,
    val marginLine: Color,
    val stickyYellow: Color,
    val stickyPeach: Color,
    val stickyMint: Color,
    val alertBg: Color,
    val lipPaper: Color,
    val lipNavy: Color
)

private val LightPaperPalette = FamiPaperPalette(
    isDark = false,
    deskBg = PaperDeskBg,
    sheet = PaperSheetWhite,
    sheetCream = PaperSheetCream,
    ink = FamiMidnight,
    inkMuted = LightOnSurfaceVariant,
    border = FamiMidnight,
    shadow3D = FamiMidnight,
    ruleLine = PaperRuleLine,
    marginLine = PaperMarginRed,
    stickyYellow = PaperStickyYellow,
    stickyPeach = PaperStickyPeach,
    stickyMint = PaperStickyMint,
    alertBg = FamiAlertRedBg,
    lipPaper = Lip3DPaper,
    lipNavy = Lip3DNavy
)

private val DarkPaperPalette = FamiPaperPalette(
    isDark = true,
    deskBg = DarkDeskBg,
    sheet = DarkPaperSheet,
    sheetCream = DarkPaperCream,
    ink = DarkInk,
    inkMuted = DarkInkMuted,
    border = DarkPaperBorder,
    shadow3D = Color(0xFF06080D),
    ruleLine = DarkPaperRuler,
    marginLine = FamiCoral.copy(alpha = 0.55f),
    stickyYellow = DarkStickyYellow,
    stickyPeach = DarkStickyPeach,
    stickyMint = DarkStickyMint,
    alertBg = DarkAlertRedBg,
    lipPaper = Lip3DPaperDark,
    lipNavy = Color(0xFF05070B)
)

val LocalFamiPaperPalette = staticCompositionLocalOf { LightPaperPalette }

object FamiTheme {
    val paper: FamiPaperPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalFamiPaperPalette.current
}

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
    background = DarkDeskBg,
    surface = DarkPaperSheet,
    surfaceVariant = DarkPaperCream,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DarkInk,
    onSurface = DarkInk,
    onSurfaceVariant = DarkInkMuted,
    outline = DarkPaperBorder,
    outlineVariant = DarkPaperRuler,
    primaryContainer = DarkStickyPeach,
    onPrimaryContainer = DarkInk
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Default to Fami Flat Paper Notebook light mode!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val paperPalette = if (darkTheme) DarkPaperPalette else LightPaperPalette
    CompositionLocalProvider(LocalFamiPaperPalette provides paperPalette) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
