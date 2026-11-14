package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.ActivityLog
import com.example.database.CategoryDetail
import com.example.database.Chore
import com.example.database.Transaction
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * CALENDARIO FÍSICO DE PAPEL CON ESPIRALES METÁLICOS Y ANIMACIÓN DE PASO DE HOJA (Fami v3.2)
 */
@Composable
fun PhysicalPaperCalendar(
    selectedMonth: Calendar,
    transactions: List<Transaction>,
    chores: List<Chore>,
    categories: List<CategoryDetail>,
    activityLogs: List<ActivityLog>,
    appLanguage: String = "es",
    isSameDay: (Long, Calendar) -> Boolean,
    toColor: (String) -> Color,
    getMonthName: (Int) -> String
) {
    val paper = FamiTheme.paper
    var calendarMonth by remember(selectedMonth) {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedMonth.get(Calendar.YEAR))
            set(Calendar.MONTH, selectedMonth.get(Calendar.MONTH))
        })
    }
    var selectedDayCal by remember(selectedMonth) {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedMonth.get(Calendar.YEAR))
            set(Calendar.MONTH, selectedMonth.get(Calendar.MONTH))
        })
    }
    // +1 para hoja siguiente, -1 para hoja anterior
    var flipDirection by remember { mutableIntStateOf(1) }

    val goPrevMonth = {
        flipDirection = -1
        calendarMonth = (calendarMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
    }
    val goNextMonth = {
        flipDirection = 1
        calendarMonth = (calendarMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
    }

    val monthKeyVal = calendarMonth.get(Calendar.YEAR) * 12 + calendarMonth.get(Calendar.MONTH)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 6.dp, end = 4.dp)
    ) {
        // Capas de hojas físicas apiladas por debajo del calendario
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 18.dp, start = 6.dp)
                .offset(x = 4.dp, y = 5.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(paper.sheetCream)
                .border(1.5.dp, paper.border, RoundedCornerShape(16.dp))
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 14.dp, start = 3.dp)
                .offset(x = 2.dp, y = 2.5.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (paper.isDark) paper.sheetCream.copy(alpha = 0.9f) else Color(0xFFFDF5E6))
                .border(1.5.dp, paper.border, RoundedCornerShape(16.dp))
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            // Lomo superior físico + Espirales metálicos dobles de calendario de escritorio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
            ) {
                // Barra encuadernadora superior (cabezal de cartón marino / pizarra)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(if (paper.isDark) paper.lipNavy else FamiMidnight)
                )
                // Espirales metálicos dobles que atraviesan la parte superior
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                ) {
                    val spiralCount = 11
                    val step = size.width / (spiralCount + 1)
                    for (i in 1..spiralCount) {
                        val cx = step * i
                        // Perforación ovalada en el papel
                        drawOval(
                            color = if (paper.isDark) Color(0xFF0A0D14) else Color(0xFF1F2752),
                            topLeft = Offset(cx - 4.5.dp.toPx(), size.height - 6.dp.toPx()),
                            size = Size(9.dp.toPx(), 6.dp.toPx())
                        )
                        // Aro trasero metálico
                        drawArc(
                            color = Color(0xFF94A3B8),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(cx - 3.5.dp.toPx(), 2.dp.toPx()),
                            size = Size(7.dp.toPx(), 18.dp.toPx()),
                            style = Stroke(width = 2.6.dp.toPx())
                        )
                        // Aro delantero brillante cobre/acero
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFE2E8F0), Color(0xFF64748B), Color(0xFFCBD5E1))
                            ),
                            topLeft = Offset(cx - 2.3.dp.toPx(), 1.dp.toPx()),
                            size = Size(4.6.dp.toPx(), size.height - 4.dp.toPx()),
                            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                        )
                        drawRoundRect(
                            color = paper.border,
                            topLeft = Offset(cx - 2.3.dp.toPx(), 1.dp.toPx()),
                            size = Size(4.6.dp.toPx(), size.height - 4.dp.toPx()),
                            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
                            style = Stroke(width = 0.9.dp.toPx())
                        )
                    }
                }
            }

            // Hoja física de calendario con animación de cambio de página al deslizar o pulsar flechas
            AnimatedContent(
                targetState = monthKeyVal,
                transitionSpec = {
                    val isNext = flipDirection >= 0
                    val enterAnim = slideInHorizontally(
                        animationSpec = tween(340, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> if (isNext) fullWidth / 2 else -fullWidth / 2 }
                    ) + fadeIn(tween(260))
                    val exitAnim = slideOutHorizontally(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        targetOffsetX = { fullWidth -> if (isNext) -fullWidth / 2 else fullWidth / 2 }
                    ) + fadeOut(tween(220))
                    enterAnim togetherWith exitAnim
                },
                label = "calendarPageTurn"
            ) { _ ->
                var dragAccum by remember { mutableFloatStateOf(0f) }

                val year = calendarMonth.get(Calendar.YEAR)
                val month = calendarMonth.get(Calendar.MONTH)
                val monthLabel = getMonthName(month)

                val firstDayOfMonth = (calendarMonth.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)
                val emptyDaysBefore = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
                val maxDaysInMonth = calendarMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(paper.sheet)
                        .border(
                            1.8.dp,
                            paper.border,
                            RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .pointerInput(monthKeyVal) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (dragAccum < -65f) {
                                        goNextMonth()
                                    } else if (dragAccum > 65f) {
                                        goPrevMonth()
                                    }
                                    dragAccum = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragAccum += dragAmount
                                }
                            )
                        }
                        .drawBehind {
                            // Margen vertical rojo tenue de cuaderno + cuadrícula de papel
                            drawLine(
                                color = paper.marginLine.copy(alpha = 0.3f),
                                start = Offset(14.dp.toPx(), 0f),
                                end = Offset(14.dp.toPx(), size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 14.dp, top = 10.dp, bottom = 14.dp)
                    ) {
                        // Franja roja superior de almanaque clásico
                        Surface(
                            color = FamiCoral,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, paper.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Tactile3DButton(
                                    onClick = goPrevMonth,
                                    faceColor = paper.sheet,
                                    lipColor = paper.lipPaper,
                                    lipHeight = 2.5.dp,
                                    cornerRadius = 8.dp,
                                    contentPadding = PaddingValues(4.dp),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = "Mes anterior",
                                        tint = paper.ink,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${monthLabel.uppercase()} $year",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "DESLIZA O TOCA PARA PASAR HOJA",
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.85f),
                                        letterSpacing = 0.6.sp
                                    )
                                }

                                Tactile3DButton(
                                    onClick = goNextMonth,
                                    faceColor = paper.sheet,
                                    lipColor = paper.lipPaper,
                                    lipHeight = 2.5.dp,
                                    cornerRadius = 8.dp,
                                    contentPadding = PaddingValues(4.dp),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Mes siguiente",
                                        tint = paper.ink,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Días de la semana sobre franja crema
                        val weekHeaders = when (appLanguage.lowercase()) {
                            "en" -> listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                            "pt" -> listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
                            else -> listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do")
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(paper.sheetCream)
                                .border(1.dp, paper.border.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            weekHeaders.forEachIndexed { idx, wDay ->
                                Text(
                                    text = wDay,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (idx >= 5) FamiCoral else paper.ink,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Cuadrícula de días estilo papel impreso
                        val totalCells = emptyDaysBefore + maxDaysInMonth
                        val rows = (0 until totalCells).chunked(7)

                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            rows.forEach { rowIdxs ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    (0 until 7).forEach { col ->
                                        val cellIndex = rowIdxs.getOrNull(col)
                                        if (cellIndex == null || cellIndex < emptyDaysBefore) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .padding(1.5.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(paper.sheetCream.copy(alpha = 0.45f))
                                            )
                                        } else {
                                            val dayNum = cellIndex - emptyDaysBefore + 1
                                            val cellCal = (calendarMonth.clone() as Calendar).apply {
                                                set(Calendar.DAY_OF_MONTH, dayNum)
                                            }
                                            val isToday = isSameDay(System.currentTimeMillis(), cellCal)
                                            val isSelected = isSameDay(selectedDayCal.timeInMillis, cellCal)

                                            val dayTransactions = transactions.filter {
                                                val filterDate = if (it.balanceDate > 0L) it.balanceDate else it.date
                                                isSameDay(filterDate, cellCal)
                                            }
                                            val dayCompletedChores = chores.filter {
                                                it.isCompleted && it.completedDate != null && isSameDay(it.completedDate, cellCal)
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .padding(1.5.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        when {
                                                            isSelected -> FamiCoral
                                                            isToday -> paper.stickyYellow
                                                            col >= 5 -> paper.stickyPeach.copy(alpha = 0.45f)
                                                            else -> paper.sheet
                                                        }
                                                    )
                                                    .clickable { selectedDayCal = cellCal }
                                                    .border(
                                                        width = if (isToday || isSelected) 1.6.dp else 0.8.dp,
                                                        color = if (isSelected || isToday) paper.border else paper.ruleLine,
                                                        shape = RoundedCornerShape(8.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Text(
                                                        text = "$dayNum",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = if (isToday || isSelected) FontWeight.Black else FontWeight.Bold,
                                                        color = if (isSelected) Color.White else paper.ink
                                                    )
                                                    if (dayTransactions.isNotEmpty() || dayCompletedChores.isNotEmpty()) {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(top = 1.dp)
                                                        ) {
                                                            dayTransactions.take(2).forEach { tx ->
                                                                val catColorHex = categories.find {
                                                                    it.categoryName.equals(tx.category, ignoreCase = true)
                                                                }?.colorHex ?: "#EB583B"
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(4.dp)
                                                                        .clip(CircleShape)
                                                                        .background(if (isSelected) Color.White else toColor(catColorHex))
                                                                )
                                                            }
                                                            if (dayCompletedChores.isNotEmpty()) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = if (isSelected) Color.White else FamiEmerald,
                                                                    modifier = Modifier.size(6.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Detalle del día seleccionado escrito sobre líneas de cuaderno
                        val selectedDateTimeLabel = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es")).format(selectedDayCal.time)
                        val dayTx = transactions.filter {
                            val filterDate = if (it.balanceDate > 0L) it.balanceDate else it.date
                            isSameDay(filterDate, selectedDayCal)
                        }
                        val dayLogs = activityLogs.filter { isSameDay(it.timestamp, selectedDayCal) }
                        val dayCompletedChores = chores.filter {
                            it.isCompleted && it.completedDate != null && isSameDay(it.completedDate, selectedDayCal)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = paper.ruleLine, thickness = 1.2.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "📌 ANOTADO EL ${selectedDateTimeLabel.uppercase()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (paper.isDark) FamiCoral else FamiCoralDeep,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        if (dayTx.isEmpty() && dayLogs.isEmpty() && dayCompletedChores.isEmpty()) {
                            RuledNotebookText(
                                text = "Sin movimientos ni tareas registradas este día.",
                                fontSize = 12.sp,
                                color = paper.inkMuted,
                                lineHeight = 24.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                dayCompletedChores.forEach { chore ->
                                    RuledNotebookText(
                                        text = "✅ Tarea lista: ${chore.title} (${chore.assignedTo})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = paper.ink,
                                        lineHeight = 24.sp
                                    )
                                }
                                dayTx.forEach { tx ->
                                    val sign = if (tx.isIncome) "+" else "-"
                                    RuledNotebookText(
                                        text = "${if (tx.isIncome) "💵" else "🧾"} ${tx.description}: $sign${String.format(Locale.US, "%.2f", tx.originalAmount)} ${tx.currency} (${tx.category})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tx.isIncome) FamiEmerald else paper.ink,
                                        lineHeight = 24.sp
                                    )
                                }
                                dayLogs.take(4).forEach { log ->
                                    RuledNotebookText(
                                        text = "📝 [${log.tag}] ${log.message}",
                                        fontSize = 11.5.sp,
                                        color = paper.inkMuted,
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
