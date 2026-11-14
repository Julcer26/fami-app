package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.ui.theme.*

private data class InAppTourStep(
    val badge: String,
    val title: String,
    val description: String,
    val tip: String,
    val targetTab: String,
    val imageRes: Int
)

/**
 * GUÍA INTERACTIVA DENTRO DE LA APP — Fami v3.2
 * Explica paso a paso para qué sirve cada sección/botón al entrar, con botón visible para Saltar.
 */
@Composable
fun InAppGuideOverlay(
    appLanguage: String = "es",
    onSwitchTab: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val steps = remember(appLanguage) {
        when (appLanguage.lowercase()) {
            "en" -> listOf(
                InAppTourStep("1 OF 6 • NOTEBOOK", "Month Sheet & Wallets", "View your available balance, live official rate, and your local & foreign wallets side by side.", "Tap the top country pill anytime to switch currency.", "inicio", R.drawable.fami_notebook_hero),
                InAppTourStep("2 OF 6 • AI & QUICK NOTE", "3D Quick Actions", "Use SCAN or RECEIPT to read tickets with AI, or type a sentence in the yellow sticky note.", "Example: 'Spent $25 on lunch' and Fami logs it automatically.", "inicio", R.drawable.fami_sticker_idea),
                InAppTourStep("3 OF 6 • CALENDAR", "Physical Spiral Calendar", "Swipe left/right or tap arrows to turn pages and inspect transactions & completed chores per day.", "Tap any day to see all notes recorded on that date.", "inicio", R.drawable.fami_onboarding_2),
                InAppTourStep("4 OF 6 • ACCOUNTS", "Budgets, Wallets & AI Report", "Track monthly spending limits by category, manage wallets, and export to CSV.", "Tap any movement to edit, inspect photo, or categorize.", "cuentas", R.drawable.fami_onboarding_3),
                InAppTourStep("5 OF 6 • CHORES, MARKET & GOALS", "Household Lists & Notes", "Assign home chores, share shopping lists, save toward goals, and write ruled notebook notes.", "Switch tabs at the bottom 3D bar anytime.", "metas", R.drawable.fami_sticker_piggy),
                InAppTourStep("6 OF 6 • SETTINGS", "Profile, Avatar & Family Code", "Open Settings (gear icon) to change theme, language, avatar, or share your family invite code.", "You can replay this guide anytime from Settings.", "inicio", R.drawable.ic_fami_mascot)
            )
            else -> listOf(
                InAppTourStep("1 DE 6 • INICIO", "Hoja del Mes y Billeteras", "Aquí ves tu saldo disponible, tasa oficial en vivo y tus billeteras en moneda local y extranjera al mismo tiempo.", "Toca el botón de país arriba para cambiar país o moneda cuando quieras.", "inicio", R.drawable.fami_notebook_hero),
                InAppTourStep("2 DE 6 • FAMI IA", "Botones 3D y Nota Rápida", "Usa ESCANEAR o FACTURA para leer recibos con cámara/galería, o escribe en la nota adhesiva para que Fami IA lo anote solo.", "Ej: 'Gasté 25$ en almuerzo' y Fami lo clasifica automáticamente.", "inicio", R.drawable.fami_sticker_idea),
                InAppTourStep("3 DE 6 • CALENDARIO", "Calendario Físico con Espirales", "Desliza la hoja o usa las flechas para pasar de mes y tocar cualquier día para ver tus gastos y tareas de esa fecha.", "Las marcas de colores indican movimientos y tareas listas.", "inicio", R.drawable.fami_onboarding_2),
                InAppTourStep("4 DE 6 • CUENTAS", "Cuentas, Billeteras y Presupuesto", "Administra tus gastos del mes, categorías con límite, billeteras en varias monedas y exporta a CSV.", "También verás el resumen inteligente de Fami IA.", "cuentas", R.drawable.fami_onboarding_3),
                InAppTourStep("5 DE 6 • TAREAS, MERCADO Y METAS", "Hogar, Compras y Libreta de Notas", "En la barra inferior 3D navega entre Tareas del hogar, Lista de Compras y Alcancía / Libreta con notas sobre renglones.", "Marca tareas listas para celebrar logros en familia.", "metas", R.drawable.fami_sticker_piggy),
                InAppTourStep("6 DE 6 • AJUSTES", "Avatar, Tema y Código Familiar", "Toca el ícono de engranaje (⚙️) para cambiar tu avatar, idioma, tema claro/oscuro o copiar tu Código Familiar.", "Puedes volver a ver esta guía desde Ajustes cuando gustes.", "inicio", R.drawable.ic_fami_mascot)
            )
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentIndex]

    LaunchedEffect(currentIndex) {
        onSwitchTab(step.targetTab)
    }

    val skipLabel = if (appLanguage.lowercase() == "en") "Skip guide" else "Saltar guía"
    val nextLabel = if (appLanguage.lowercase() == "en") "Next" else "Siguiente"
    val finishLabel = if (appLanguage.lowercase() == "en") "Got it!" else "¡Entendido!"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(900f)
            .background(FamiMidnight.copy(alpha = 0.68f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        NotebookPaperSheet(
            modifier = Modifier.fillMaxWidth(),
            paperColor = PaperSheetWhite,
            showSpiralTop = true,
            showRuledLines = true,
            showMarginLine = true,
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Barra superior de guía con indicador de paso + Botón Saltar bien visible
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = PaperStickyYellow,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.3.dp, FamiMidnight)
                    ) {
                        Text(
                            text = "🧭 ${step.badge}",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = FamiMidnight,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Tactile3DButton(
                        onClick = onDismiss,
                        faceColor = PaperStickyPeach,
                        lipColor = Lip3DPaper,
                        lipHeight = 2.5.dp,
                        cornerRadius = 10.dp,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = FamiMidnight, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(skipLabel, fontSize = 11.sp, fontWeight = FontWeight.Black, color = FamiMidnight)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Ilustración de Fami explicando la sección
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PaperSheetCream)
                            .border(1.8.dp, FamiMidnight, RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = step.imageRes),
                            contentDescription = step.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = FamiMidnight
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        RuledNotebookText(
                            text = step.description,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FamiNavyMuted,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = PaperStickyMint,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.3.dp, FamiMidnight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 ${step.tip}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FamiMidnight,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Indicador de puntos
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    steps.indices.forEach { idx ->
                        val active = idx == currentIndex
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(8.dp)
                                .width(if (active) 22.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (active) FamiCoral else PaperSheetCream)
                                .border(1.2.dp, FamiMidnight, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botones Anterior / Siguiente o Entendido
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentIndex > 0) {
                        Tactile3DButton(
                            onClick = { currentIndex-- },
                            faceColor = PaperSheetCream,
                            lipColor = Lip3DPaper,
                            lipHeight = 3.5.dp,
                            cornerRadius = 12.dp,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Anterior", tint = FamiMidnight, modifier = Modifier.size(16.dp))
                        }
                    }

                    Tactile3DButton(
                        onClick = {
                            if (currentIndex < steps.lastIndex) {
                                currentIndex++
                            } else {
                                onSwitchTab("inicio")
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        faceColor = FamiCoral,
                        lipColor = Lip3DCoral,
                        lipHeight = 4.dp,
                        cornerRadius = 12.dp,
                        contentPadding = PaddingValues(vertical = 11.dp, horizontal = 16.dp)
                    ) {
                        val isLast = currentIndex == steps.lastIndex
                        Text(
                            text = if (isLast) finishLabel.uppercase() else nextLabel.uppercase(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isLast) Icons.Default.Check else Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
