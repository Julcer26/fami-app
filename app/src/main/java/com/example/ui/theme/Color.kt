package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =================================================================
// PALETA OFICIAL FAMI v3.0 — "CUADERNO DE PAPEL PLANO & BOTONES 3D"
// Extraída directamente de Logos fami svg (ARCHIVO SVG LOGO VERSION A COLOR.svg)
// Tinta Oficial: #101535 | Cobre/Terracota Oficial: #DD5E35 -> #E54629 -> #F57656
// =================================================================

// Tonos Principales de Marca (De Logos fami svg)
val FamiMidnight = Color(0xFF101535)       // Tinta Azul Marino Oficial (.cls-2)
val FamiMidnightDark = Color(0xFF080B1E)   // Sombra 3D de tinta profunda
val FamiCoral = Color(0xFFE54629)          // Coral / Terracota vibrante oficial (stop .48)
val FamiCoralDeep = Color(0xFFDD5E35)      // Cobre / Terracota profundo oficial (stop .06)
val FamiCoralLight = Color(0xFFF57656)     // Coral salmón luminoso oficial (stop .93)
val FamiCoralSoft = Color(0xFFEB9376)      // Cobre pastel oficial (stop .21)
val FamiCoralWarm = Color(0xFFE97D60)      // Terracota medio oficial (stop .39)
val FamiCoralContainer = Color(0xFFFDECE6) // Papel durazno suave

// Superficies de Papel Plano (Cuaderno Fami)
val PaperDeskBg = Color(0xFFF3EDE2)        // Mesa / Pasta de cuaderno crema cálido
val PaperSheetWhite = Color(0xFFFFFDF9)    // Hoja de papel plano marfil puro
val PaperSheetCream = Color(0xFFFAF5E8)    // Hoja secundaria crema
val PaperStickyYellow = Color(0xFFFFF5D6)  // Nota adhesiva amarilla cálida
val PaperStickyPeach = Color(0xFFFDECE6)   // Nota adhesiva cobre/durazno
val PaperStickyMint = Color(0xFFE6F9F3)    // Nota adhesiva menta contable
val PaperRuleLine = Color(0xFFE6DEC8)      // Renglones horizontales del cuaderno
val PaperMarginRed = Color(0xFFF57656)     // Línea de margen vertical del cuaderno
val PaperShadow3D = Color(0xFFD2C7B4)      // Sombra dura 3D de hoja de papel

// Labios de Extrusión para Botones 3D (Tactile 3D Lips)
val Lip3DCoral = Color(0xFF9E2B14)         // Base 3D para botón Coral/Terracota
val Lip3DNavy = Color(0xFF060818)          // Base 3D para botón Azul Tinta
val Lip3DMint = Color(0xFF007A58)          // Base 3D para botón Esmeralda
val Lip3DGold = Color(0xFFB87D00)          // Base 3D para botón Dorado
val Lip3DPaper = Color(0xFFC4B8A3)         // Base 3D para botón de Papel Crema

// Tonos Financieros / Sellos Contables
val FamiEmerald = Color(0xFF00B884)        // Verde sello contable / Ingresos
val FamiEmeraldContainer = Color(0xFFE6F9F3)
val FamiGold = Color(0xFFF59E0B)           // Dorado moneda / Metas
val FamiGoldContainer = Color(0xFFFFF5D6)
val FamiAlertRed = Color(0xFFD93829)       // Rojo sello alerta / Gastos
val FamiAlertRedBg = Color(0xFFFDE8E6)

// Modo Cuaderno Nocturno (Opcional en Ajustes)
val DarkBackground = Color(0xFF101535)
val DarkSurface = Color(0xFF19204A)
val DarkSurfaceElevated = Color(0xFF232C60)
val DarkPrimary = FamiCoral
val DarkSecondary = FamiCoralLight
val DarkOnBackground = Color(0xFFFFFDF9)
val DarkOnSurfaceVariant = Color(0xFFB8C2E6)
val DarkOutline = Color(0xFF344080)

// Modo Cuaderno de Papel Plano (Predeterminado en Fami v3.0)
val LightBackground = PaperDeskBg
val LightSurface = PaperSheetWhite
val LightPrimary = FamiCoral
val LightSecondary = FamiCoralDeep
val LightOnBackground = FamiMidnight
val LightOnSurfaceVariant = Color(0xFF4A5270)
val LightOutline = Color(0xFFD5CCB8)

// Aliases de compatibilidad
val FamiDarkSurface = DarkSurface
val FamiDarkSurfaceElevated = DarkSurfaceElevated
val FamiDarkBorder = DarkOutline

val AlertRed = FamiAlertRed
val AlertRedBg = FamiAlertRedBg
val OutlineGrey = LightOutline

val ColorJulio = Color(0xFF2563EB)
val ColorAna = Color(0xFFDB2777)

val MeshDarkPurple = Color(0xFF101535)
val MeshDarkBlue = Color(0xFF19204A)
val MeshDarkTeal = Color(0xFF1E293B)

val MeshLightPurple = PaperDeskBg
val MeshLightBlue = PaperSheetCream
val MeshLightTeal = PaperStickyYellow

val GlassBorderLight = FamiMidnight.copy(alpha = 0.85f)
val GlassBorderDark = Color(0xFFFFFFFF).copy(alpha = 0.2f)
val GlassSurfaceLight = PaperSheetWhite
val GlassSurfaceDark = DarkSurface

// Gradientes Oficiales del SVG de Fami
val FamiCoralGradient = Brush.horizontalGradient(
    colors = listOf(FamiCoralDeep, FamiCoral, FamiCoralLight)
)
val FamiMidnightGradient = Brush.verticalGradient(
    colors = listOf(FamiMidnight, FamiMidnightDark)
)

val SubtitleText: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant
