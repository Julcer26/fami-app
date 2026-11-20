package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private data class CoachMarkStep(
    val targetId: String,
    val targetTab: String,
    val scrollIndex: Int?,
    val badge: String,
    val title: String,
    val description: String
)

/**
 * GUÍA INTERACTIVA TIPO SPOTLIGHT COACH-MARK — Fami v3.8.1
 * Recorta dinámicamente la zona real del botón o sección explicada sin taparla,
 * mantiene transiciones de 60fps sin saltos al cambiar de pestaña o posición, y
 * presenta a la mascota oficial Fami (robot cobre con cuaderno) señalando cada función.
 */
@Composable
fun InAppGuideOverlay(
    appLanguage: String = "es",
    onSwitchTab: (String) -> Unit,
    onEnsureDashboardScroll: suspend (Int) -> Unit = {},
    onDismiss: () -> Unit
) {
    val paper = FamiTheme.paper
    val density = LocalDensity.current
    val registry = LocalTourRegistry.current
    val lang = appLanguage.lowercase()

    val steps = remember(lang) {
        when (lang) {
            "en" -> listOf(
                CoachMarkStep("balance_sheet", "inicio", 1, "1 / 7", "Available Balance & Currency", "See the total sum of your wallets here. Tap the currency pill to switch between currencies."),
                CoachMarkStep("quick_note_actions", "inicio", 2, "2 / 7", "Type, Speak or Scan", "Write an expense, tap Mic to dictate, Send to save, or Scan / Gallery to read receipts with AI."),
                CoachMarkStep("calendar_section", "inicio", 4, "3 / 7", "Physical Spiral Calendar", "Swipe or tap the arrows to flip months and tap any day to inspect expenses and tasks."),
                CoachMarkStep("tab_cuentas", "cuentas", null, "4 / 7", "Finances & Wallets", "Manage your multi-currency wallets, transfer funds, update monthly income, and export CSV."),
                CoachMarkStep("tab_tareas", "tareas", null, "5 / 7", "Household Tasks", "Create chores for a specific person or for everyone—Fami sends a push alert to the assignee!"),
                CoachMarkStep("tab_compras", "compras", null, "6 / 7", "Market & Piggy Bank", "Keep shared grocery lists here, and use Piggy Bank (next tab) for savings goals and ruled notes."),
                CoachMarkStep("btn_settings", "inicio", 0, "7 / 7", "Settings & Family Code", "Tap this gear icon to change Face/Fingerprint/PIN lock, Dark Mode, avatar, or replay this guide.")
            )
            "pt" -> listOf(
                CoachMarkStep("balance_sheet", "inicio", 1, "1 / 7", "Saldo Disponível e Moeda", "Veja aqui a soma das suas carteiras. Toque no botão de moeda para alternar rapidamente."),
                CoachMarkStep("quick_note_actions", "inicio", 2, "2 / 7", "Escreva, Fale ou Escaneie", "Digite um gasto, use o Microfone para ditar, Enviar para salvar ou Escanear / Galeria para recibos."),
                CoachMarkStep("calendar_section", "inicio", 4, "3 / 7", "Calendário em Espiral", "Deslize ou toque nas setas para mudar de mês e toque em qualquer dia para ver detalhes."),
                CoachMarkStep("tab_cuentas", "cuentas", null, "4 / 7", "Finanças e Carteiras", "Gerencie suas carteiras, faça transferências, edite sua renda mensal e exporte CSV."),
                CoachMarkStep("tab_tareas", "tareas", null, "5 / 7", "Tarefas da Casa", "Atribua tarefas a alguém específico ou para Todos — o app envia uma notificação push!"),
                CoachMarkStep("tab_compras", "compras", null, "6 / 7", "Mercado e Cofrinho", "Organize listas de compras aqui e use o Cofrinho ao lado para metas e notas pautadas."),
                CoachMarkStep("btn_settings", "inicio", 0, "7 / 7", "Ajustes e Código Familiar", "Toque nesta engrenagem para configurar Digital/PIN, Modo Escuro, avatar ou rever este guia.")
            )
            else -> listOf(
                CoachMarkStep("balance_sheet", "inicio", 1, "1 / 7", "Saldo Disponible y Moneda", "Aquí ves la suma de tus billeteras en tu moneda preferida. Toca el botón de moneda para alternarla."),
                CoachMarkStep("quick_note_actions", "inicio", 2, "2 / 7", "Escribe, Habla o Escanea", "Escribe tu gasto, usa el Micrófono para dictar, Enviar para registrar o Escanear / Galería para recibos con IA."),
                CoachMarkStep("calendar_section", "inicio", 4, "3 / 7", "Calendario Físico", "Desliza o usa las flechas para cambiar de mes y toca cualquier día para ver tus gastos y tareas."),
                CoachMarkStep("tab_cuentas", "cuentas", null, "4 / 7", "Pestaña Finanzas", "Crea y edita tus billeteras, transfiere saldos entre ellas, ajusta tu ingreso mensual y exporta a CSV."),
                CoachMarkStep("tab_tareas", "tareas", null, "5 / 7", "Pestaña Tareas", "Asigna tareas del hogar a una persona o a Todos: Fami enviará una notificación push al destinatario."),
                CoachMarkStep("tab_compras", "compras", null, "6 / 7", "Mercado y Alcancía", "Lleva tus listas de compras aquí, y en Alcancía (botón derecho) guarda tus metas de ahorro y notas."),
                CoachMarkStep("btn_settings", "inicio", 0, "7 / 7", "Botón de Ajustes", "Toca este engranaje para cambiar Huella o PIN, Modo Oscuro, tu avatar, Código Familiar o repetir la guía.")
            )
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentIndex]

    var containerSize by remember { mutableStateOf(Size.Zero) }
    var bubbleHeightPx by remember { mutableIntStateOf(with(density) { 196.dp.roundToPx() }) }

    val fallbackRect = remember(containerSize, step.targetTab, step.targetId) {
        val w = containerSize.width.takeIf { it > 0f } ?: 1080f
        val h = containerSize.height.takeIf { it > 0f } ?: 2200f
        when (step.targetId) {
            "btn_settings" -> Rect(w - 190f, 110f, w - 40f, 230f)
            "tab_cuentas", "tab_tareas", "tab_compras", "tab_metas" -> Rect(w * 0.2f, h - 220f, w * 0.8f, h - 60f)
            else -> Rect(w * 0.06f, h * 0.22f, w * 0.94f, h * 0.44f)
        }
    }

    // Mantiene el último Rect válido mientras la lista o pestaña termina de posicionarse,
    // evitando saltos bruscos hacia el centro de la pantalla entre pasos.
    val rawTarget = registry?.get(step.targetId)
    var settledTargetRect by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(rawTarget) {
        if (rawTarget != null) {
            settledTargetRect = rawTarget
        }
    }

    LaunchedEffect(currentIndex) {
        onSwitchTab(step.targetTab)
        if (step.targetTab == "inicio" && step.scrollIndex != null) {
            // Posicionamiento rápido sin bloquear el hilo de animación
            delay(35)
            onEnsureDashboardScroll(step.scrollIndex)
            delay(45)
        } else {
            delay(45)
        }
        val measured = registry?.get(step.targetId)
        if (measured != null) {
            settledTargetRect = measured
        } else if (settledTargetRect == null) {
            settledTargetRect = fallbackRect
        }
    }

    val skipLabel = when (lang) {
        "en" -> "Skip"
        "pt" -> "Pular"
        else -> "Saltar"
    }
    val nextLabel = when (lang) {
        "en" -> "Next"
        "pt" -> "Próximo"
        else -> "Siguiente"
    }
    val finishLabel = when (lang) {
        "en" -> "Done!"
        "pt" -> "Pronto!"
        else -> "¡Listo!"
    }

    val pulseTransition = rememberInfiniteTransition(label = "SpotlightPulse")
    val pulseProgress by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val desiredRect = rawTarget ?: settledTargetRect ?: fallbackRect
    val animatedTargetRect by animateRectAsState(
        targetValue = desiredRect,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "SpotlightRect"
    )

    val padPx = with(density) { 8.dp.toPx() }
    val spotlightRect = Rect(
        left = (animatedTargetRect.left - padPx).coerceAtLeast(4f),
        top = (animatedTargetRect.top - padPx).coerceAtLeast(4f),
        right = (animatedTargetRect.right + padPx).coerceAtMost((containerSize.width - 4f).coerceAtLeast(40f)),
        bottom = (animatedTargetRect.bottom + padPx).coerceAtMost((containerSize.height - 4f).coerceAtLeast(40f))
    )

    val statusTopPx = WindowInsets.statusBars.getTop(density).toFloat()
    val navBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()
    val marginPx = with(density) { 14.dp.toPx() }
    val gapPx = with(density) { 16.dp.toPx() }
    val arrowHeightPx = with(density) { 12.dp.toPx() }

    val screenW = containerSize.width.takeIf { it > 0f } ?: 1080f
    val screenH = containerSize.height.takeIf { it > 0f } ?: 2200f
    val safeTop = statusTopPx + marginPx
    val safeBottom = (screenH - navBottomPx - marginPx).coerceAtLeast(safeTop + 200f)

    val targetTopForBubble = desiredRect.top - padPx
    val targetBottomForBubble = desiredRect.bottom + padPx
    val spaceAbove = (targetTopForBubble - gapPx) - safeTop
    val spaceBelow = safeBottom - (targetBottomForBubble + gapPx)
    val placeBelow = if (spaceBelow >= bubbleHeightPx + arrowHeightPx) {
        true
    } else if (spaceAbove >= bubbleHeightPx + arrowHeightPx) {
        false
    } else {
        spaceBelow >= spaceAbove
    }

    val rawBubbleTopPx = if (placeBelow) {
        (targetBottomForBubble + gapPx).coerceAtMost((safeBottom - bubbleHeightPx).coerceAtLeast(safeTop))
    } else {
        (targetTopForBubble - gapPx - bubbleHeightPx).coerceAtLeast(safeTop)
    }

    val animatedBubbleTopPx by animateFloatAsState(
        targetValue = rawBubbleTopPx,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "BubbleTopPx"
    )

    val bubbleHorizontalPadPx = with(density) { 14.dp.toPx() }
    val bubbleWidthPx = (screenW - bubbleHorizontalPadPx * 2f).coerceAtLeast(200f)
    val arrowCenterX = spotlightRect.center.x.coerceIn(
        bubbleHorizontalPadPx + with(density) { 28.dp.toPx() },
        screenW - bubbleHorizontalPadPx - with(density) { 28.dp.toPx() }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(900f)
            .onSizeChanged { containerSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(currentIndex, spotlightRect) {
                detectTapGestures { offset ->
                    if (spotlightRect.contains(offset)) {
                        if (currentIndex < steps.lastIndex) {
                            currentIndex++
                        } else {
                            onSwitchTab("inicio")
                            onDismiss()
                        }
                    }
                }
            }
    ) {
        // 1. Scrim oscuro con recorte transparente real (BlendMode.Clear) + aro pulsante
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(color = Color(0xFF060A18).copy(alpha = 0.78f))

            val spotSize = Size(
                (spotlightRect.width).coerceAtLeast(16f),
                (spotlightRect.height).coerceAtLeast(16f)
            )
            val corner = CornerRadius(18.dp.toPx(), 18.dp.toPx())

            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(spotlightRect.left, spotlightRect.top),
                size = spotSize,
                cornerRadius = corner,
                blendMode = BlendMode.Clear
            )

            // Halo exterior pulsante
            val expand = (3.dp + (5.dp * pulseProgress)).toPx()
            drawRoundRect(
                color = FamiCoral.copy(alpha = 0.48f * (1f - pulseProgress * 0.55f)),
                topLeft = Offset(spotlightRect.left - expand, spotlightRect.top - expand),
                size = Size(spotSize.width + expand * 2f, spotSize.height + expand * 2f),
                cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )

            // Borde principal nítido
            drawRoundRect(
                color = FamiGold,
                topLeft = Offset(spotlightRect.left, spotlightRect.top),
                size = spotSize,
                cornerRadius = corner,
                style = Stroke(width = 2.6.dp.toPx())
            )
        }

        // 2. Flecha triangular apuntando desde el globo Coach-Mark hacia el elemento real
        Canvas(modifier = Modifier.fillMaxSize()) {
            val halfW = 11.dp.toPx()
            val arrowH = arrowHeightPx
            val tipY = if (placeBelow) animatedBubbleTopPx - arrowH + 2f else animatedBubbleTopPx + bubbleHeightPx + arrowH - 2f
            val baseY = if (placeBelow) animatedBubbleTopPx + 2f else animatedBubbleTopPx + bubbleHeightPx - 2f

            val path = Path().apply {
                moveTo(arrowCenterX, tipY)
                lineTo(arrowCenterX - halfW, baseY)
                lineTo(arrowCenterX + halfW, baseY)
                close()
            }
            drawPath(path = path, color = paper.sheet)

            val borderPath = Path().apply {
                moveTo(arrowCenterX - halfW, baseY)
                lineTo(arrowCenterX, tipY)
                lineTo(arrowCenterX + halfW, baseY)
            }
            drawPath(
                path = borderPath,
                color = paper.border,
                style = Stroke(width = 2.2.dp.toPx())
            )
        }

        // 3. Globo Coach-Mark tipo hoja de cuaderno con la mascota oficial Fami Robot
        Surface(
            modifier = Modifier
                .offset { IntOffset(bubbleHorizontalPadPx.roundToInt(), animatedBubbleTopPx.roundToInt()) }
                .width(with(density) { bubbleWidthPx.toDp() })
                .onSizeChanged { bubbleHeightPx = it.height }
                .shadow(16.dp, RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            color = paper.sheet,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.2.dp, paper.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = paper.stickyYellow,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.2.dp, paper.border)
                    ) {
                        Text(
                            text = "🤖 GUÍA FAMI • ${step.badge}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Tactile3DButton(
                        onClick = {
                            onSwitchTab("inicio")
                            onDismiss()
                        },
                        faceColor = paper.stickyPeach,
                        lipColor = paper.lipPaper,
                        lipHeight = 2.dp,
                        cornerRadius = 9.dp,
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = paper.ink, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(skipLabel, fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = paper.ink)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mascota oficial Fami Robot (ajustada con Nano Banana sosteniendo cuaderno y señalando)
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .graphicsLayer {
                                translationY = (pulseProgress - 0.5f) * 5f
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(paper.sheetCream)
                            .border(1.5.dp, paper.border, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.fami_guide_pointer),
                            contentDescription = "Mascota Oficial Fami Guía",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            fadeIn(tween(150)) togetherWith fadeOut(tween(110))
                        },
                        modifier = Modifier.weight(1f),
                        label = "GuideStepText"
                    ) { activeStep ->
                        Column {
                            Text(
                                text = activeStep.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = paper.ink
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = activeStep.description,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = paper.inkMuted,
                                lineHeight = 16.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        steps.indices.forEach { idx ->
                            val active = idx == currentIndex
                            Box(
                                modifier = Modifier
                                    .height(7.dp)
                                    .width(if (active) 18.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(if (active) FamiCoral else paper.sheetCream)
                                    .border(1.dp, paper.border, CircleShape)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (currentIndex > 0) {
                            Tactile3DButton(
                                onClick = { currentIndex-- },
                                faceColor = paper.sheetCream,
                                lipColor = paper.lipPaper,
                                lipHeight = 3.dp,
                                cornerRadius = 10.dp,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Anterior", tint = paper.ink, modifier = Modifier.size(15.dp))
                            }
                        }

                        val isLast = currentIndex == steps.lastIndex
                        Tactile3DButton(
                            onClick = {
                                if (!isLast) {
                                    currentIndex++
                                } else {
                                    onSwitchTab("inicio")
                                    onDismiss()
                                }
                            },
                            faceColor = FamiCoral,
                            lipColor = Lip3DCoral,
                            lipHeight = 3.dp,
                            cornerRadius = 10.dp,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = if (isLast) finishLabel.uppercase() else nextLabel.uppercase(),
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isLast) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
