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
val PaperStickySky = Color(0xFFE6F1FF)     // Nota adhesiva azul cielo suave
val PaperStickyLavender = Color(0xFFF1E8FF) // Nota adhesiva lavanda pastel
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

// Modo Cuaderno Nocturno Oficial — Papel Grafito Cálido (Warm Graphite Sketchbook)
val DarkDeskBg = Color(0xFF111113)         // Pasta / Escritorio grafito carbón profundo
val DarkPaperSheet = Color(0xFF1C1C20)     // Hoja principal de papel grafito mate
val DarkPaperCream = Color(0xFF25252B)     // Hoja secundaria / tarjeta grafito elevada
val DarkPaperRuler = Color(0xFF32323A)     // Renglones horizontales grafito sutiles
val DarkPaperBorder = Color(0xFF42424C)    // Borde entintado grafito refinado
val DarkInk = Color(0xFFF3F0E8)            // Tinta marfil/tiza cálida (contraste AAA)
val DarkInkMuted = Color(0xFFA9A59D)       // Tinta secundaria arena grafito (contraste > 6.5:1)
val DarkStickyYellow = Color(0xFF2F2B20)   // Notita adhesiva ámbar arena grafito
val DarkStickyPeach = Color(0xFF332321)    // Notita adhesiva terracota suave grafito
val DarkStickyMint = Color(0xFF1B2E29)     // Notita adhesiva salvia/menta grafito
val DarkStickySky = Color(0xFF1E2833)      // Notita adhesiva pizarra cielo grafito
val DarkStickyLavender = Color(0xFF292333) // Notita adhesiva lavanda humo grafito
val DarkAlertRedBg = Color(0xFF3A1F23)     // Fondo de alerta carmesí humo
val Lip3DPaperDark = Color(0xFF0D0D10)     // Labio 3D profundo grafito

val DarkBackground = DarkDeskBg
val DarkSurface = DarkPaperSheet
val DarkSurfaceElevated = DarkPaperCream
val DarkPrimary = FamiCoral
val DarkSecondary = FamiCoralLight
val DarkOnBackground = DarkInk
val DarkOnSurfaceVariant = DarkInkMuted
val DarkOutline = DarkPaperBorder

// Modo Cuaderno de Papel Plano (Predeterminado en Fami v3.0)
val LightBackground = PaperDeskBg
val LightSurface = PaperSheetWhite
val LightPrimary = FamiCoral
val LightSecondary = FamiCoralDeep
val LightOnBackground = FamiMidnight
val LightOnSurfaceVariant = Color(0xFF4A5270)
val LightOutline = Color(0xFFD5CCB8)

// Aliases de compatibilidad
val FamiNavy = FamiMidnight
val FamiNavyMuted = LightOnSurfaceVariant
val FamiCoralDark = FamiCoralDeep
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
