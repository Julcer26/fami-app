package com.example.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * TACTILE 3D PUSHABLE BUTTON — Fami
 * Botón físico 3D con cara superior plana, borde de tinta #101535 y labio inferior 3D extruido
 * que se hunde físicamente al presionarlo.
 */
@Composable
fun Tactile3DButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    faceColor: Color = FamiCoral,
    lipColor: Color = Lip3DCoral,
    borderColor: Color = FamiMidnight,
    contentColor: Color = Color.Unspecified,
    cornerRadius: Dp = 14.dp,
    lipHeight: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) (lipHeight - 1.dp).coerceAtLeast(0.dp) else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "3dPressOffset"
    )

    Box(
        modifier = modifier
            .padding(bottom = lipHeight)
            .drawBehind {
                val radiusPx = cornerRadius.toPx()
                val lipPx = lipHeight.toPx()
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(0f, lipPx),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
                drawRoundRect(
                    color = lipColor,
                    topLeft = Offset(1.5.dp.toPx(), lipPx - 1.dp.toPx()),
                    size = Size(size.width - 3.dp.toPx(), size.height),
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
            }
            .offset(y = pressOffset)
            .clip(RoundedCornerShape(cornerRadius))
            .background(faceColor)
            .border(1.8.dp, borderColor, RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

/**
 * SPIRAL NOTEBOOK RINGS STRIP — Fami
 * Dibuja los anillos metálicos de encuadernación y las perforaciones reales de la hoja.
 */
@Composable
fun NotebookSpiralStrip(
    modifier: Modifier = Modifier,
    ringCount: Int = 9,
    ringColor: Color = FamiCoralDeep
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val spacing = size.width / (ringCount + 1)
        for (i in 1..ringCount) {
            val cx = spacing * i
            // Sombra del hueco perforado
            drawCircle(
                color = Color(0xFFD5CBB8),
                radius = 4.6.dp.toPx(),
                center = Offset(cx, size.height * 0.68f)
            )
            // Hueco perforado
            drawCircle(
                color = FamiMidnight,
                radius = 3.8.dp.toPx(),
                center = Offset(cx, size.height * 0.68f)
            )
            // Anillo metálico doble aro
            drawRoundRect(
                color = ringColor,
                topLeft = Offset(cx - 2.3.dp.toPx(), 0f),
                size = Size(4.6.dp.toPx(), size.height * 0.72f),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
            drawRoundRect(
                color = FamiMidnight,
                topLeft = Offset(cx - 2.3.dp.toPx(), 0f),
                size = Size(4.6.dp.toPx(), size.height * 0.72f),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

/**
 * RULED NOTEBOOK TEXT — Texto exactamente sentado SOBRE las líneas del cuaderno
 * Calcula las líneas usando la línea base tipográfica real (`getLineBaseline(i)`) para que
 * cada renglón de texto repose sobre su línea azul/sepia del cuaderno, nunca entre líneas.
 */
@Composable
fun RuledNotebookText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = FamiMidnight,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    lineHeight: TextUnit = 26.sp,
    maxLines: Int = Int.MAX_VALUE,
    minRuledLines: Int = 1,
    textDecoration: TextDecoration? = null,
    ruleColor: Color = PaperRuleLine.copy(alpha = 0.75f)
) {
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        lineHeight = lineHeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(textDecoration = textDecoration),
        onTextLayout = { textLayout = it },
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val layout = textLayout
                val strokePx = 1.dp.toPx()
                val baselinePad = 3.dp.toPx()
                val stepPx = lineHeight.toPx().coerceAtLeast(18.dp.toPx())

                if (layout != null && layout.lineCount > 0) {
                    var lastY = 0f
                    for (i in 0 until layout.lineCount) {
                        val y = (layout.getLineBaseline(i) + baselinePad).coerceAtMost(size.height)
                        drawLine(
                            color = ruleColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokePx
                        )
                        lastY = y
                    }
                    var extraLines = layout.lineCount
                    while (extraLines < minRuledLines && lastY + stepPx <= size.height) {
                        lastY += stepPx
                        drawLine(
                            color = ruleColor,
                            start = Offset(0f, lastY),
                            end = Offset(size.width, lastY),
                            strokeWidth = strokePx
                        )
                        extraLines++
                    }
                } else {
                    var y = stepPx - baselinePad
                    while (y < size.height) {
                        drawLine(
                            color = ruleColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokePx
                        )
                        y += stepPx
                    }
                }
            }
            .padding(bottom = 4.dp)
    )
}

/**
 * NOTEBOOK PAPER SHEET — Hoja de Papel Plano con Renglones, Margen Coral y Sombra 3D
 */
@Composable
fun NotebookPaperSheet(
    modifier: Modifier = Modifier,
    sheetColor: Color = PaperSheetWhite,
    paperColor: Color = sheetColor,
    borderColor: Color = FamiMidnight,
    shadowColor: Color = FamiMidnight,
    cornerRadius: Dp = 18.dp,
    showRuledLines: Boolean = true,
    showMarginLine: Boolean = true,
    showSpiralTop: Boolean = false,
    lineSpacing: Dp = 28.dp,
    firstLineOffset: Dp = 44.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffset by animateDpAsState(
        targetValue = if (onClick != null && isPressed) 2.5.dp else 0.dp,
        label = "sheetPress"
    )

    val clickMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .padding(bottom = 4.dp, end = 3.dp)
            .drawBehind {
                val r = cornerRadius.toPx()
                drawRoundRect(
                    color = shadowColor,
                    topLeft = Offset(3.dp.toPx(), 4.dp.toPx()),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(r, r)
                )
            }
            .offset(x = pressOffset * 0.6f, y = pressOffset)
            .clip(RoundedCornerShape(cornerRadius))
            .background(paperColor)
            .border(1.8.dp, borderColor, RoundedCornerShape(cornerRadius))
            .then(clickMod)
            .drawBehind {
                if (showRuledLines) {
                    val step = lineSpacing.toPx()
                    var y = firstLineOffset.toPx()
                    while (y < size.height - 10.dp.toPx()) {
                        drawLine(
                            color = PaperRuleLine.copy(alpha = 0.55f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        y += step
                    }
                }
                if (showMarginLine) {
                    val marginX = 22.dp.toPx()
                    drawLine(
                        color = PaperMarginRed.copy(alpha = 0.38f),
                        start = Offset(marginX, 0f),
                        end = Offset(marginX, size.height),
                        strokeWidth = 1.4.dp.toPx()
                    )
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (showSpiralTop) {
                NotebookSpiralStrip(ringCount = 10)
            }
            content()
        }
    }
}

/**
 * STICKY NOTE SHEET — Nota Adhesiva Plana con Cinta Adhesiva Superior y Relieve 3D
 */
@Composable
fun StickyNoteSheet(
    modifier: Modifier = Modifier,
    noteColor: Color = PaperStickyYellow,
    tapeColor: Color = FamiCoralSoft.copy(alpha = 0.65f),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .padding(top = 6.dp, bottom = 4.dp, end = 3.dp)
            .drawBehind {
                val r = 16.dp.toPx()
                drawRoundRect(
                    color = FamiMidnight,
                    topLeft = Offset(3.dp.toPx(), 4.dp.toPx()),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(r, r)
                )
            }
            .clip(RoundedCornerShape(16.dp))
            .background(noteColor)
            .border(1.8.dp, FamiMidnight, RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(76.dp)
                .height(12.dp)
                .background(tapeColor)
                .border(0.8.dp, FamiMidnight.copy(alpha = 0.35f))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            content = content
        )
    }
}
