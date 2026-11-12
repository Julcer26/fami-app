package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =================================================================
// PALETA OFICIAL FAMI (somosfami.app)
// Midnight Navy #101535 + Sunset Coral #E54629 + Emerald + Gold
// =================================================================

// Tonos Principales de Marca
val FamiMidnight = Color(0xFF101535)       // Midnight Navy profundo oficial
val FamiMidnightDark = Color(0xFF0B0E24)   // Background ultra-profundo
val FamiCoral = Color(0xFFE54629)          // Coral / Terracota vibrante oficial
val FamiCoralDeep = Color(0xFFDD5E35)      // Coral profundo
val FamiCoralLight = Color(0xFFF57656)     // Coral luminoso
val FamiCoralSoft = Color(0xFFEB9376)      // Coral pastel
val FamiCoralContainer = Color(0x2EE54629) // Fondo translúcido con tinte coral

// Tonos Financieros / Indicadores
val FamiEmerald = Color(0xFF00D09C)        // Ingresos / Crecimiento / Tasa oficial
val FamiEmeraldContainer = Color(0x1F00D09C)
val FamiGold = Color(0xFFFFB800)           // Metas / Ahorros / Moneda USD
val FamiGoldContainer = Color(0x1FFFFB800)
val FamiAlertRed = Color(0xFFEF4444)       // Gastos / Advertencias
val FamiAlertRedBg = Color(0x1FEF4444)

// Dark Theme (Midnight Fintech)
val DarkBackground = FamiMidnightDark
val DarkSurface = Color(0xFF151B42)
val DarkSurfaceElevated = Color(0xFF1D2456)
val DarkPrimary = FamiCoral
val DarkSecondary = FamiCoralLight
val DarkOnBackground = Color(0xFFFFFFFF)
val DarkOnSurfaceVariant = Color(0xFF9DA8D1)
val DarkOutline = Color(0xFF28316B)

// Light Theme
val LightBackground = Color(0xFFF7F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightPrimary = FamiCoral
val LightSecondary = FamiCoralDeep
val LightOnBackground = FamiMidnight
val LightOnSurfaceVariant = Color(0xFF5A6488)
val LightOutline = Color(0xFFE2E6F5)

// Gradientes Oficiales
val FamiCoralGradient = Brush.horizontalGradient(
    colors = listOf(FamiCoral, FamiCoralLight)
)
val FamiMidnightGradient = Brush.verticalGradient(
    colors = listOf(FamiMidnight, FamiMidnightDark)
)

val SubtitleText: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant
