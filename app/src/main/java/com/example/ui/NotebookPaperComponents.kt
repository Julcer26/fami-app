package com.example.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

/**
 * TACTILE 3D PUSHABLE BUTTON — Fami v3.0
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
                // Labio 3D inferior sólido
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
 * SPIRAL NOTEBOOK RINGS STRIP — Fami v3.0
 * Dibuja los anillos de encuadernación en cobre/azul marino y las perforaciones de la hoja.
 */
@Composable
fun NotebookSpiralStrip(
    modifier: Modifier = Modifier,
    ringCount: Int = 9
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
    ) {
        val spacing = size.width / (ringCount + 1)
        for (i in 1..ringCount) {
            val cx = spacing * i
            // Sombra del hueco perforado
            drawCircle(
                color = Color(0xFFD5CBB8),
                radius = 4.5.dp.toPx(),
                center = Offset(cx, size.height * 0.68f)
            )
            // Hueco perforado
            drawCircle(
                color = FamiMidnight,
                radius = 3.8.dp.toPx(),
                center = Offset(cx, size.height * 0.68f)
            )
            // Anillo metálico cobre Fami
            drawRoundRect(
                color = FamiCoralDeep,
                topLeft = Offset(cx - 2.2.dp.toPx(), 0f),
                size = Size(4.4.dp.toPx(), size.height * 0.72f),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
            drawRoundRect(
                color = FamiMidnight,
                topLeft = Offset(cx - 2.2.dp.toPx(), 0f),
                size = Size(4.4.dp.toPx(), size.height * 0.72f),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
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
                // Sombra sólida 3D de hojas apiladas
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
                    val lineSpacing = 26.dp.toPx()
                    var y = 34.dp.toPx()
                    while (y < size.height - 10.dp.toPx()) {
                        drawLine(
                            color = PaperRuleLine.copy(alpha = 0.65f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        y += lineSpacing
                    }
                }
                if (showMarginLine) {
                    val marginX = 22.dp.toPx()
                    drawLine(
                        color = PaperMarginRed.copy(alpha = 0.35f),
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
        // Cinta adhesiva (Washi Tape Cobre Fami) en el borde superior central
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
