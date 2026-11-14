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
    val spotlightTarget: String,
    val spotlightIsTop: Boolean,
    val imageRes: Int
)

/**
 * GUÍA INTERACTIVA DENTRO DE LA APP — Fami v3.3
 * Explica paso a paso para qué sirve cada sección/botón al entrar, con soporte Dark Mode y foco visual.
 */
@Composable
fun InAppGuideOverlay(
    appLanguage: String = "es",
    onSwitchTab: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val paper = FamiTheme.paper
    val lang = appLanguage.lowercase()

    val steps = remember(lang) {
        when (lang) {
            "en" -> listOf(
                InAppTourStep("1 OF 6 • NOTEBOOK", "Month Sheet & Balance", "View your consolidated available balance in your preferred currency, summing all wallets.", "Tap the currency pill inside the sheet to switch between primary and secondary currency.", "inicio", "Top bar & Sheet 1: Notebook", true, R.drawable.fami_notebook_hero),
                InAppTourStep("2 OF 6 • AI & QUICK NOTE", "Write, Speak or Scan", "Type or tap the Mic button to dictate your expense and press Send, or use Scan / Gallery below.", "Pick the target wallet directly in the yellow sticky note.", "inicio", "Quick Note: Mic, Send, Scan & Gallery", false, R.drawable.fami_sticker_idea),
                InAppTourStep("3 OF 6 • CALENDAR", "Physical Spiral Calendar", "Swipe left/right or tap arrows to turn pages and inspect transactions & completed chores per day.", "Tap any day to see all notes recorded on that date.", "inicio", "Calendar section inside Notebook", false, R.drawable.fami_onboarding_2),
                InAppTourStep("4 OF 6 • ACCOUNTS", "Wallets, Income & Budgets", "Create & edit your multi-currency wallets, transfer between them, manage monthly income, and export CSV.", "Your wallets sum automatically into the available balance at Home.", "cuentas", "Bottom Tab 2: Accounts", false, R.drawable.fami_onboarding_3),
                InAppTourStep("5 OF 6 • CHORES, MARKET & GOALS", "Household Lists & Notes", "Assign home chores, share shopping lists, save toward goals, and write ruled notebook notes.", "Switch tabs at the bottom 3D bar anytime.", "metas", "Bottom Tabs 3–5: Chores • Market • Piggy Bank", false, R.drawable.fami_sticker_piggy),
                InAppTourStep("6 OF 6 • SETTINGS", "Security, Avatar & Family Code", "Open Settings (gear icon) to switch Huella/PIN unlock, Dark Mode, language, or invite family.", "You can replay this guide anytime from Settings.", "inicio", "Top-right Gear Icon (⚙️ Settings)", true, R.drawable.ic_fami_mascot)
            )
            "pt" -> listOf(
                InAppTourStep("1 DE 6 • CADERNO", "Folha do Mês e Saldo", "Veja seu saldo disponível consolidado na sua moeda preferida, somando todas as suas carteiras.", "Toque no botão de moeda na folha para alternar entre moeda principal e secundária.", "inicio", "Topo e Aba 1: Caderno", true, R.drawable.fami_notebook_hero),
                InAppTourStep("2 DE 6 • FAMI IA", "Escreva, Fale ou Escaneie", "Digite ou toque no Microfone para ditar seu gasto e envie, ou use Escanear / Galeria logo abaixo.", "Escolha a carteira na própria nota amarela.", "inicio", "Nota Rápida: Microfone, Enviar, Escanear e Galeria", false, R.drawable.fami_sticker_idea),
                InAppTourStep("3 DE 6 • CALENDÁRIO", "Calendário Físico em Espiral", "Deslize ou toque nas setas para virar a página e ver gastos e tarefas concluídas por dia.", "Toque em qualquer dia para ver detalhes anotados.", "inicio", "Seção Calendário no Caderno", false, R.drawable.fami_onboarding_2),
                InAppTourStep("4 DE 6 • CONTAS", "Carteiras, Renda e Orçamentos", "Crie e edite suas carteiras multi-moeda, transfira saldos, gerencie renda mensal e exporte CSV.", "A soma das suas carteiras aparece como saldo disponível no Início.", "cuentas", "Barra Inferior Aba 2: Contas", false, R.drawable.fami_onboarding_3),
                InAppTourStep("5 DE 6 • TAREFAS, MERCADO E METAS", "Casa, Compras e Caderneta", "Organize tarefas da casa, lista de mercado, metas de economia e notas em folhas pautadas.", "Alterne pelas abas 3D inferiores quando quiser.", "metas", "Abas 3–5: Tarefas • Mercado • Cofrinho", false, R.drawable.fami_sticker_piggy),
                InAppTourStep("6 DE 6 • AJUSTES", "Segurança, Avatar e Código", "Toque na engrenagem (⚙️) para escolher Digital/PIN, Modo Escuro, idioma ou copiar o Código Familiar.", "Reabra este guia em Ajustes a qualquer momento.", "inicio", "Ícone de Engrenagem no Topo (⚙️)", true, R.drawable.ic_fami_mascot)
            )
            else -> listOf(
                InAppTourStep("1 DE 6 • INICIO", "Hoja del Mes y Saldo Total", "Aquí ves tu saldo disponible consolidado en tu moneda de preferencia, sumando todas tus billeteras.", "Usa el botón de cambio de moneda dentro de la hoja si deseas ver la otra moneda.", "inicio", "Hoja del Mes en Cuaderno", true, R.drawable.fami_notebook_hero),
                InAppTourStep("2 DE 6 • FAMI IA", "Escribe, Habla o Escanea", "Escribe o toca el Micrófono para dictar tu gasto y pulsa Enviar, o usa Escanear / Galería debajo.", "Elige la billetera donde se registrará tu movimiento.", "inicio", "Nota Rápida: Voz, Enviar, Escanear y Galería", false, R.drawable.fami_sticker_idea),
                InAppTourStep("3 DE 6 • CALENDARIO", "Calendario Físico con Espirales", "Desliza la hoja o usa las flechas para pasar de mes y toca cualquier día para ver tus gastos y tareas de esa fecha.", "Las marcas de colores indican movimientos y tareas listas.", "inicio", "Sección Calendario dentro de Cuaderno", false, R.drawable.fami_onboarding_2),
                InAppTourStep("4 DE 6 • CUENTAS", "Mis Billeteras, Ingresos y Presupuesto", "Crea y edita tus billeteras multi-moneda, transfiere entre ellas, edita tu ingreso mensual y exporta a CSV.", "La suma de tus billeteras refleja tu saldo disponible al inicio.", "cuentas", "Barra Inferior • Pestaña 2: Cuentas", false, R.drawable.fami_onboarding_3),
                InAppTourStep("5 DE 6 • TAREAS, MERCADO Y METAS", "Hogar, Compras y Libreta de Notas", "En la barra inferior 3D navega entre Tareas del hogar, Lista de Compras y Alcancía / Libreta con notas sobre renglones.", "Marca tareas listas para celebrar logros en familia.", "metas", "Barra Inferior • Pestañas 3–5: Tareas • Mercado • Alcancía", false, R.drawable.fami_sticker_piggy),
                InAppTourStep("6 DE 6 • AJUSTES", "Huella/PIN, Avatar y Código Familiar", "Toca el ícono de engranaje (⚙️) para elegir ingreso con Huella o PIN, cambiar tema oscuro, avatar o copiar tu Código Familiar.", "Puedes volver a ver esta guía desde Ajustes cuando gustes.", "inicio", "Ícono de Engrenaje arriba a la derecha (⚙️)", true, R.drawable.ic_fami_mascot)
            )
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentIndex]

    LaunchedEffect(currentIndex) {
        onSwitchTab(step.targetTab)
    }

    val skipLabel = when (lang) {
        "en" -> "Skip guide"
        "pt" -> "Pular guia"
        else -> "Saltar guía"
    }
    val nextLabel = when (lang) {
        "en" -> "Next"
        "pt" -> "Próximo"
        else -> "Siguiente"
    }
    val finishLabel = when (lang) {
        "en" -> "Got it!"
        "pt" -> "Entendi!"
        else -> "¡Entendido!"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(900f)
            .background(Color(0xFF0A0D18).copy(alpha = 0.72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        // Indicador visual de foco (Spotlight Target) arriba o abajo según el paso
        Surface(
            color = FamiCoral,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.6.dp, paper.border),
            modifier = Modifier
                .align(if (step.spotlightIsTop) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = if (step.spotlightIsTop) "⬆️ ${step.spotlightTarget}" else "⬇️ ${step.spotlightTarget}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        NotebookPaperSheet(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            paperColor = paper.sheet,
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
                        color = paper.stickyYellow,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.3.dp, paper.border)
                    ) {
                        Text(
                            text = "🧭 ${step.badge}",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Tactile3DButton(
                        onClick = onDismiss,
                        faceColor = paper.stickyPeach,
                        lipColor = paper.lipPaper,
                        lipHeight = 2.5.dp,
                        cornerRadius = 10.dp,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = paper.ink, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(skipLabel, fontSize = 11.sp, fontWeight = FontWeight.Black, color = paper.ink)
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
                            .background(paper.sheetCream)
                            .border(1.8.dp, paper.border, RoundedCornerShape(16.dp))
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
                            color = paper.ink
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        RuledNotebookText(
                            text = step.description,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = paper.inkMuted,
                            lineHeight = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = paper.stickyMint,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.3.dp, paper.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 ${step.tip}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = paper.ink,
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
                                .background(if (active) FamiCoral else paper.sheetCream)
                                .border(1.2.dp, paper.border, CircleShape)
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
                            faceColor = paper.sheetCream,
                            lipColor = paper.lipPaper,
                            lipHeight = 3.5.dp,
                            cornerRadius = 12.dp,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Anterior", tint = paper.ink, modifier = Modifier.size(16.dp))
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
