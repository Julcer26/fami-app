package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.DeleteIconButton
import com.example.R
import com.example.cleanMemberAlias
import com.example.database.*
import com.example.ui.theme.*
import com.example.viewmodel.FamilyViewModel
import java.util.*
import kotlin.math.roundToInt

// --- TAB 5: METAS ---
@Composable
fun MetasTab(viewModel: FamilyViewModel, onOpenConfig: () -> Unit) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val appPrimaryCurrency by viewModel.primaryCurrency.collectAsStateWithLifecycle()
    val secondaryCurrency by viewModel.secondaryCurrency.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userAvatarIdState by viewModel.userAvatarId.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val paper = FamiTheme.paper
    val s = com.example.ui.LocalFamiStrings.current

    var activeSubTab by remember { mutableStateOf("deseos") } // "deseos" or "notas"
    var activeGoalFilter by remember { mutableStateOf("pendientes") }

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalTitleInput by remember { mutableStateOf("") }
    var goalAmountInput by remember { mutableStateOf("") }
    var goalSavedInput by remember { mutableStateOf("") }
    var goalCurrencyInput by remember(appPrimaryCurrency) { mutableStateOf(appPrimaryCurrency) }
    var goalIsPrivateInput by remember { mutableStateOf(false) }
    var showGoalCurrencyPicker by remember { mutableStateOf(false) }

    var goalToDeposit by remember { mutableStateOf<Goal?>(null) }
    var depositAmountInput by remember { mutableStateOf("") }

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var noteSearchQuery by remember { mutableStateOf("") }
    var noteFilter by remember { mutableStateOf("Todas") }
    var isNoteGridMode by remember { mutableStateOf(true) }
    var editingGoal by remember { mutableStateOf<Goal?>(null) }

    val openNewGoalDialog = {
        editingGoal = null
        goalTitleInput = ""
        goalAmountInput = ""
        goalSavedInput = ""
        goalCurrencyInput = appPrimaryCurrency
        goalIsPrivateInput = false
        showAddGoalDialog = true
    }

    val pendingGoals = remember(goals) { goals.filter { !it.isCompleted } }
    val completedGoals = remember(goals) { goals.filter { it.isCompleted } }
    val shownGoals = remember(pendingGoals, completedGoals, activeGoalFilter) {
        if (activeGoalFilter == "pendientes") pendingGoals else completedGoals
    }

    val notesPinnedCount = remember(notes) { notes.count { it.isPinned && !it.isCompleted } }
    val notesCompletedCount = remember(notes) { notes.count { it.isCompleted } }
    val activeNotesCount = remember(notes) { notes.count { !it.isCompleted } }
    val filteredNotes = remember(notes, noteSearchQuery, noteFilter) {
        notes.filter { n ->
            val matchesSearch = noteSearchQuery.isBlank() ||
                n.title.contains(noteSearchQuery, ignoreCase = true) ||
                n.content.contains(noteSearchQuery, ignoreCase = true) ||
                n.checklist.any { it.text.contains(noteSearchQuery, ignoreCase = true) }
            val matchesFilter = when (noteFilter) {
                "Todas" -> !n.isCompleted
                "Fijadas" -> n.isPinned && !n.isCompleted
                "Archivadas" -> n.isCompleted
                else -> n.category.equals(noteFilter, ignoreCase = true) && !n.isCompleted
            }
            matchesSearch && matchesFilter
        }
    }
    val noteRows = remember(filteredNotes) { filteredNotes.chunked(2) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 150.dp) ){
        item {
            Spacer(modifier = Modifier.height(10.dp))
            ModernTabHeader(
                title = s.goalsTitle,
                subtitle = s.goalsSubtitle,
                actionText = if (activeSubTab == "deseos") s.goalsNewGoalBtn else s.goalsNewNoteBtn,
                onActionClick = { 
                    if (activeSubTab == "deseos") {
                        openNewGoalDialog()
                    } else {
                        editingNote = null
                        showAddNoteDialog = true 
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Nota Adhesiva de Papel Plano: Alcancía Familiar Fami (agrupación segura por moneda)
            val savedByCurr = goals.mapNotNull { g ->
                val curr = g.currency.ifBlank { appPrimaryCurrency }.uppercase()
                val effSaved = if (g.isCompleted && g.savedAmount <= 0.0 && (g.targetAmount ?: 0.0) > 0.0) {
                    g.targetAmount!!
                } else {
                    g.savedAmount
                }
                if (effSaved > 0.0) curr to effSaved else null
            }.groupBy({ it.first }, { it.second }).mapValues { (_, list) -> list.sum() }

            val pendingByCurr = goals.filter { !it.isCompleted && (it.targetAmount ?: 0.0) > 0.0 }
                .groupBy { it.currency.ifBlank { appPrimaryCurrency }.uppercase() }
                .mapValues { (_, list) -> list.sumOf { it.targetAmount ?: 0.0 } }

            val pricedGoals = goals.filter { (it.targetAmount ?: 0.0) > 0.0 }
            val overallProgress = if (pricedGoals.isEmpty()) {
                if (goals.isNotEmpty()) goals.count { it.isCompleted }.toFloat() / goals.size.toFloat() else 0f
            } else {
                pricedGoals.map { g ->
                    val t = g.targetAmount ?: 1.0
                    val eff = if (g.isCompleted && g.savedAmount <= 0.0) t else g.savedAmount
                    (eff / t).toFloat().coerceIn(0f, 1f)
                }.average().toFloat()
            }
            val animatedOverallProgress by animateFloatAsState(
                targetValue = overallProgress.coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                label = "overallGoalsRing"
            )
            val completedCount = goals.count { it.isCompleted }
            val activeCount = goals.count { !it.isCompleted }

            val piggySummaryText = when {
                savedByCurr.isNotEmpty() -> s.goalsSavedPrefix + savedByCurr.entries.joinToString(" • ") { (curr, total) ->
                    "${com.example.currency.CountryCurrencyConfig.getCurrencySymbol(curr)}${String.format(Locale.US, "%,.0f", total)} $curr"
                }
                pendingByCurr.isNotEmpty() -> s.goalsTargetPrefix + pendingByCurr.entries.joinToString(" • ") { (curr, total) ->
                    "${com.example.currency.CountryCurrencyConfig.getCurrencySymbol(curr)}${String.format(Locale.US, "%,.0f", total)} $curr"
                }
                else -> s.goalsEmptyHint
            }

            StickyNoteSheet(
                modifier = Modifier.fillMaxWidth(),
                noteColor = paper.stickyPeach,
                tapeColor = FamiCoral.copy(alpha = 0.65f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                paper.sheet.copy(alpha = 0.85f),
                                                paper.sheet.copy(alpha = 0.25f),
                                                Color.Transparent
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.fami_sticker_piggy),
                                    contentDescription = s.goalsPiggyHeader,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(2.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = s.goalsPiggyHeader,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = FamiCoralDeep,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = if (goals.isEmpty()) s.goalsPiggyHeader else "$completedCount • $activeCount",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = piggySummaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = paper.inkMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Anillo de progreso circular de metas con monto
                        val overallPct = (overallProgress * 100f).roundToInt().coerceIn(0, 100)
                        val ringColor = if (overallPct >= 100) FamiEmerald else FamiCoral
                        val trackColor = paper.sheet.copy(alpha = 0.85f)
                        Box(
                            modifier = Modifier.size(62.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 7.dp.toPx()
                                drawArc(
                                    color = trackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                if (animatedOverallProgress > 0f) {
                                    drawArc(
                                        color = ringColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f * animatedOverallProgress,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$overallPct%",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                                Text(
                                    text = "avance",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = paper.inkMuted
                                )
                            }
                        }
                    }

                    // Fichas KPI compactas del ahorro familiar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = paper.sheet.copy(alpha = 0.88f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.2.dp, paper.border.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("🎯", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$activeCount en curso",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = paper.stickyMint.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.2.dp, paper.border.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("🏆", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$completedCount cumplidas",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = paper.stickyYellow.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.2.dp, paper.border.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("📝", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${notes.count { !it.isCompleted }} notitas",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            ModernFilterPills(
                options = listOf(
                    "deseos" to "🎯 Metas y Deseos (${goals.size})",
                    "notas" to "📝 Libreta y Notas (${notes.size})"
                ),
                selectedId = activeSubTab,
                onSelect = { activeSubTab = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (activeSubTab == "deseos") {
            item {
                ModernFilterPills(
                    options = listOf(
                        "pendientes" to "En Curso (${pendingGoals.size})",
                        "completadas" to "Cumplidas (${completedGoals.size})"
                    ),
                    selectedId = activeGoalFilter,
                    onSelect = { activeGoalFilter = it }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (shownGoals.isEmpty()) {
                item {
                    ModernEmptyState(
                        icon = Icons.Default.Star,
                        title = if (activeGoalFilter == "pendientes") "No tienes metas pendientes" else "Aún no has cumplido metas",
                        subtitle = if (activeGoalFilter == "pendientes") "Crea metas de ahorro o deseos para tu hogar." else "Las metas alcanzadas se listarán aquí.",
                        actionText = if (activeGoalFilter == "pendientes") "+ Crear Meta" else null,
                        onActionClick = { openNewGoalDialog() }
                    )
                }
            } else {
                items(shownGoals, key = { it.id.ifEmpty { "${it.title}_${it.createdDate}" } }) { goal ->
                    val goalCurr = goal.currency.ifBlank { appPrimaryCurrency }.uppercase()
                    val goalSym = com.example.currency.CountryCurrencyConfig.getCurrencySymbol(goalCurr)
                    val cleanCreator = cleanMemberAlias(goal.createdBy)
                    val target = goal.targetAmount ?: 0.0
                    val hasTarget = target > 0.0
                    val effectiveSaved = if (goal.isCompleted && goal.savedAmount <= 0.0 && hasTarget) target else goal.savedAmount
                    val pct = if (hasTarget) ((effectiveSaved / target) * 100.0).roundToInt().coerceIn(0, 100) else 0
                    val remaining = (target - effectiveSaved).coerceAtLeast(0.0)
                    val animatedGoalFraction by animateFloatAsState(
                        targetValue = (pct / 100f).coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
                        label = "goalBar_${goal.id}"
                    )
                    val lowerTitle = goal.title.lowercase()
                    val (catEmoji, catLabel) = when {
                        listOf("casa", "hogar", "apartamento", "renta", "mueble", "cocina", "reparaci").any { it in lowerTitle } -> "🏠" to "Hogar"
                        listOf("viaje", "vacaci", "vuelo", "playa", "hotel", "pasaje", "maleta").any { it in lowerTitle } -> "✈️" to "Viaje"
                        listOf("emergencia", "salud", "médico", "medico", "seguro", "fondo", "respaldo").any { it in lowerTitle } -> "🛟" to "Respaldo"
                        listOf("carro", "auto", "vehículo", "vehiculo", "moto", "caucho", "repuesto").any { it in lowerTitle } -> "🚗" to "Vehículo"
                        listOf("estudio", "universidad", "colegio", "curso", "libro", "escuela", "laptop").any { it in lowerTitle } -> "🎓" to "Estudio"
                        listOf("regalo", "cumple", "navidad", "fiesta", "boda", "aniversario").any { it in lowerTitle } -> "🎁" to "Especial"
                        listOf("deuda", "pago", "tarjeta", "banco", "crédito", "credito").any { it in lowerTitle } -> "💳" to "Deuda"
                        else -> "🎯" to "Meta"
                    }

                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        com.example.ui.NotebookPaperSheet(
                            modifier = Modifier.fillMaxWidth(),
                            sheetColor = if (goal.isCompleted) paper.stickyMint else paper.sheet,
                            showRuledLines = true,
                            showMarginLine = true,
                            showSpiralTop = false,
                            onClick = {
                                editingGoal = goal
                                goalTitleInput = goal.title
                                goalAmountInput = goal.targetAmount?.let { if (it > 0.0) it.toString() else "" } ?: ""
                                goalSavedInput = if (goal.savedAmount > 0.0) goal.savedAmount.toString() else ""
                                goalCurrencyInput = goalCurr
                                goalIsPrivateInput = goal.isPrivate
                                showAddGoalDialog = true
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 28.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Fila superior: Categoría + Privacidad + Estado/%
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = paper.stickyPeach.copy(alpha = 0.75f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, paper.border.copy(alpha = 0.35f))
                                        ) {
                                            Text(
                                                text = "$catEmoji $catLabel",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = paper.ink,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                            )
                                        }
                                        if (goal.isPrivate) {
                                            Surface(
                                                color = paper.sheetCream,
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, paper.border.copy(alpha = 0.3f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = "Privado",
                                                        tint = paper.inkMuted,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "Solo yo",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = paper.inkMuted
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (hasTarget || goal.isCompleted) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (goal.isCompleted) FamiEmerald.copy(alpha = 0.16f) else paper.stickyYellow)
                                                .border(
                                                    1.dp,
                                                    if (goal.isCompleted) FamiEmerald.copy(alpha = 0.55f) else paper.border.copy(alpha = 0.45f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.5.dp)
                                        ) {
                                            Text(
                                                text = if (goal.isCompleted) "¡Cumplida!" else "$pct% • $goalCurr",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (goal.isCompleted) FamiEmerald else paper.ink
                                            )
                                        }
                                    }
                                }

                                // Título + Check táctil + Autor
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val checkboxBg = if (goal.isCompleted) FamiEmerald else Color.Transparent
                                    val checkboxBorderColor = if (goal.isCompleted) FamiEmerald else FamiCoral

                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(checkboxBg)
                                            .border(2.dp, checkboxBorderColor, CircleShape)
                                            .clickable { viewModel.editGoal(goal.copy(isCompleted = !goal.isCompleted)) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (goal.isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Desmarcar",
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        com.example.ui.RuledNotebookText(
                                            text = goal.title,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            maxLines = 2,
                                            lineHeight = 22.sp,
                                            textDecoration = if (goal.isCompleted) TextDecoration.LineThrough else null,
                                            color = if (goal.isCompleted) paper.inkMuted else paper.ink,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        com.example.ui.MemberAvatarBadge(
                                            memberName = cleanCreator,
                                            profiles = profiles,
                                            currentUser = currentUser,
                                            currentUserAvatarId = userAvatarIdState,
                                            size = 24.dp,
                                            showAliasBelow = true,
                                            aliasText = cleanCreator,
                                            aliasFontSize = 9.sp,
                                            aliasColor = paper.inkMuted,
                                            aliasMaxWidth = 58.dp
                                        )
                                    }
                                }

                                if (hasTarget) {
                                    // Barra de progreso con marcadores de hitos (25%, 50%, 75%)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(11.dp)
                                            .clip(CircleShape)
                                            .background(paper.sheetCream)
                                            .border(1.dp, paper.border.copy(alpha = 0.35f), CircleShape)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction = animatedGoalFraction)
                                                .clip(CircleShape)
                                                .background(if (goal.isCompleted || pct >= 100) FamiEmerald else FamiCoral)
                                        )
                                        // Marcadores visuales de hitos
                                        Row(modifier = Modifier.fillMaxSize()) {
                                            Spacer(modifier = Modifier.weight(1f))
                                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(paper.border.copy(alpha = 0.22f)))
                                            Spacer(modifier = Modifier.weight(1f))
                                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(paper.border.copy(alpha = 0.22f)))
                                            Spacer(modifier = Modifier.weight(1f))
                                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(paper.border.copy(alpha = 0.22f)))
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }

                                    // Ficha numérica limpia: Ahorrado | Falta | Objetivo
                                    Surface(
                                        color = paper.sheetCream.copy(alpha = 0.72f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, paper.border.copy(alpha = 0.28f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("AHORRADO", fontSize = 8.sp, fontWeight = FontWeight.Black, color = paper.inkMuted)
                                                Text(
                                                    text = "$goalSym${String.format(Locale.US, "%,.0f", effectiveSaved)}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = FamiEmerald
                                                )
                                            }
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("FALTA", fontSize = 8.sp, fontWeight = FontWeight.Black, color = paper.inkMuted)
                                                Text(
                                                    text = if (goal.isCompleted || remaining <= 0.0) "${goalSym}0" else "$goalSym${String.format(Locale.US, "%,.0f", remaining)}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (goal.isCompleted || remaining <= 0.0) FamiEmerald else FamiCoralDeep
                                                )
                                            }
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Text("OBJETIVO", fontSize = 8.sp, fontWeight = FontWeight.Black, color = paper.inkMuted)
                                                Text(
                                                    text = "$goalSym${String.format(Locale.US, "%,.0f", target)}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = paper.ink
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when {
                                                goal.isCompleted -> "🎉 ¡Meta lograda!"
                                                pct >= 75 -> "🔥 ¡Casi la alcanzas!"
                                                pct >= 40 -> "💪 Vas por buen camino"
                                                else -> "🌱 Cada abono suma"
                                            },
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = paper.inkMuted
                                        )
                                        if (!goal.isCompleted) {
                                            com.example.ui.Tactile3DButton(
                                                onClick = {
                                                    goalToDeposit = goal
                                                    depositAmountInput = ""
                                                },
                                                faceColor = FamiEmerald,
                                                lipColor = Lip3DMint,
                                                borderColor = paper.border,
                                                cornerRadius = 11.dp,
                                                lipHeight = 3.dp,
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Abonar",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White
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
        } else {
            // Libreta y Notitas Fami
            item {
                com.example.ui.NotesToolbar(
                    searchQuery = noteSearchQuery,
                    onSearchChange = { noteSearchQuery = it },
                    selectedFilter = noteFilter,
                    onSelectFilter = { noteFilter = it },
                    isGridMode = isNoteGridMode,
                    onToggleGridMode = { isNoteGridMode = !isNoteGridMode },
                    totalCount = activeNotesCount,
                    pinnedCount = notesPinnedCount,
                    completedCount = notesCompletedCount
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (notes.isEmpty()) {
                item {
                    ModernEmptyState(
                        icon = Icons.Default.EditNote,
                        title = "Tu libreta está vacía",
                        subtitle = "Crea notitas adhesivas con colores, categorías y listas de verificación.",
                        actionText = "+ Nueva Notita",
                        onActionClick = {
                            editingNote = null
                            showAddNoteDialog = true
                        }
                    )
                }
            } else if (filteredNotes.isEmpty()) {
                item {
                    ModernEmptyState(
                        icon = Icons.Default.Search,
                        title = "Sin notitas en esta vista",
                        subtitle = "Prueba otro filtro o crea una notita nueva.",
                        actionText = "+ Nueva Notita",
                        onActionClick = {
                            editingNote = null
                            showAddNoteDialog = true
                        }
                    )
                }
            } else if (isNoteGridMode) {
                items(
                    noteRows,
                    key = { row -> row.joinToString("_") { it.id.ifEmpty { "${it.title}_${it.createdDate}" } } }
                ) { rowNotes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        rowNotes.forEach { note ->
                            val cleanAuthor = cleanMemberAlias(note.createdBy)
                            com.example.ui.StickyNoteCard(
                                note = note,
                                cleanAuthor = cleanAuthor,
                                profiles = profiles,
                                currentUser = currentUser,
                                currentUserAvatarId = userAvatarIdState,
                                compact = true,
                                onOpenEdit = {
                                    editingNote = note
                                    showAddNoteDialog = true
                                },
                                onTogglePin = { viewModel.toggleNotePin(note) },
                                onToggleComplete = { viewModel.editNote(note.copy(isCompleted = !note.isCompleted)) },
                                onToggleCheckItem = { idx -> viewModel.toggleNoteChecklistItem(note, idx) },
                                onDelete = { viewModel.deleteNote(note) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowNotes.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(filteredNotes, key = { it.id.ifEmpty { "${it.title}_${it.createdDate}" } }) { note ->
                    val cleanAuthor = cleanMemberAlias(note.createdBy)
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        com.example.ui.StickyNoteCard(
                            note = note,
                            cleanAuthor = cleanAuthor,
                            profiles = profiles,
                            currentUser = currentUser,
                            currentUserAvatarId = userAvatarIdState,
                            compact = false,
                            onOpenEdit = {
                                editingNote = note
                                showAddNoteDialog = true
                            },
                            onTogglePin = { viewModel.toggleNotePin(note) },
                            onToggleComplete = { viewModel.editNote(note.copy(isCompleted = !note.isCompleted)) },
                            onToggleCheckItem = { idx -> viewModel.toggleNoteChecklistItem(note, idx) },
                            onDelete = { viewModel.deleteNote(note) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showAddGoalDialog) {
        val quickCurrencies = remember(appPrimaryCurrency, secondaryCurrency) {
            listOf("USD", appPrimaryCurrency, secondaryCurrency, "EUR")
                .map { it.trim().uppercase() }
                .filter { it.isNotBlank() }
                .distinct()
        }
        AlertDialog(
            onDismissRequest = { 
                showAddGoalDialog = false
                editingGoal = null
                goalTitleInput = ""
                goalAmountInput = ""
                goalSavedInput = ""
                goalIsPrivateInput = false
            },
            containerColor = paper.sheet,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (editingGoal != null) "Editar Meta o Deseo" else "Nueva Meta Familiar",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = paper.ink
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = goalTitleInput,
                        onValueChange = { goalTitleInput = it },
                        label = { Text("¿Qué deseas ahorrar o lograr?") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FamiCoral,
                            unfocusedBorderColor = paper.border.copy(alpha = 0.5f),
                            focusedTextColor = paper.ink,
                            unfocusedTextColor = paper.ink
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "🏠 Hogar" to "Fondo para el hogar: ",
                            "✈️ Viaje" to "Viaje familiar: ",
                            "🛟 Emergencia" to "Fondo de emergencia: ",
                            "🚗 Vehículo" to "Vehículo: ",
                            "🎓 Estudio" to "Estudios: ",
                            "🎁 Regalo" to "Regalo especial: "
                        ).forEach { (chipLabel, prefix) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(paper.stickyPeach.copy(alpha = 0.8f))
                                    .border(1.dp, paper.border.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (goalTitleInput.isBlank()) {
                                            goalTitleInput = prefix
                                        } else if (!goalTitleInput.contains(prefix.trim(), ignoreCase = true)) {
                                            goalTitleInput = "$prefix${goalTitleInput.trim()}"
                                        }
                                    }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = chipLabel,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = paper.ink
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = goalAmountInput,
                            onValueChange = { goalAmountInput = it },
                            label = { Text("Monto meta") },
                            placeholder = { Text("Opcional") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FamiCoral,
                                unfocusedBorderColor = paper.border.copy(alpha = 0.5f),
                                focusedTextColor = paper.ink,
                                unfocusedTextColor = paper.ink
                            )
                        )
                        OutlinedTextField(
                            value = goalSavedInput,
                            onValueChange = { goalSavedInput = it },
                            label = { Text("Ahorrado") },
                            placeholder = { Text("0") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FamiCoral,
                                unfocusedBorderColor = paper.border.copy(alpha = 0.5f),
                                focusedTextColor = paper.ink,
                                unfocusedTextColor = paper.ink
                            )
                        )
                    }

                    Text(
                        text = "Moneda de la meta",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = paper.inkMuted
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickCurrencies.forEach { code ->
                            val isSel = goalCurrencyInput.equals(code, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) FamiCoral else paper.sheetCream)
                                    .border(1.dp, if (isSel) FamiCoral else paper.border.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                    .clickable { goalCurrencyInput = code }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${com.example.currency.CountryCurrencyConfig.getCurrencySymbol(code)} $code",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSel) Color.White else paper.ink
                                )
                            }
                        }
                        if (!quickCurrencies.any { it.equals(goalCurrencyInput, ignoreCase = true) } && goalCurrencyInput.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(FamiCoral)
                                    .border(1.dp, FamiCoral, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${com.example.currency.CountryCurrencyConfig.getCurrencySymbol(goalCurrencyInput)} $goalCurrencyInput",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(paper.stickyYellow)
                                .border(1.dp, paper.border.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable { showGoalCurrencyPicker = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "🌍 Otra",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = paper.ink
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clickable { goalIsPrivateInput = !goalIsPrivateInput },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = goalIsPrivateInput,
                            onCheckedChange = { goalIsPrivateInput = it }
                        )
                        Text("Privado (Solo yo puedo verlo)", fontSize = 13.5.sp, color = paper.ink)
                    }
                }
            },
            dismissButton = {
                if (editingGoal != null) {
                    TextButton(onClick = { 
                        viewModel.deleteGoal(editingGoal!!)
                        editingGoal = null
                        showAddGoalDialog = false
                    }) {
                        Text("Eliminar", color = AlertRed, fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = { showAddGoalDialog = false }) {
                        Text("Cancelar", color = paper.inkMuted)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountVal = goalAmountInput.replace(",", ".").toDoubleOrNull()
                        val savedVal = (goalSavedInput.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                        if (goalTitleInput.isNotBlank()) {
                            if (editingGoal != null) {
                                val reached = amountVal != null && amountVal > 0.0 && savedVal >= amountVal
                                val updated = editingGoal!!.copy(
                                    title = goalTitleInput.trim(),
                                    targetAmount = amountVal,
                                    savedAmount = savedVal,
                                    currency = goalCurrencyInput.ifBlank { appPrimaryCurrency },
                                    isPrivate = goalIsPrivateInput,
                                    isCompleted = reached || editingGoal!!.isCompleted
                                )
                                viewModel.editGoal(updated)
                            } else {
                                viewModel.addGoal(
                                    title = goalTitleInput.trim(),
                                    targetAmount = amountVal,
                                    savedAmount = savedVal,
                                    currency = goalCurrencyInput.ifBlank { appPrimaryCurrency },
                                    isPrivate = goalIsPrivateInput
                                )
                            }
                            showAddGoalDialog = false
                            editingGoal = null
                            goalTitleInput = ""
                            goalAmountInput = ""
                            goalSavedInput = ""
                            goalIsPrivateInput = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FamiCoral),
                    shape = CircleShape
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        )
    }

    if (showGoalCurrencyPicker) {
        com.example.ui.CurrencyPickerDialog(
            title = "Moneda para tu meta",
            selectedCurrencyCode = goalCurrencyInput,
            allowNone = false,
            noneLabel = "",
            onDismiss = { showGoalCurrencyPicker = false },
            onSelectCurrency = { code ->
                if (code.isNotBlank()) {
                    goalCurrencyInput = code
                }
                showGoalCurrencyPicker = false
            }
        )
    }

    if (goalToDeposit != null) {
        val targetGoal = goalToDeposit!!
        val depCurr = targetGoal.currency.ifBlank { appPrimaryCurrency }.uppercase()
        val depSym = com.example.currency.CountryCurrencyConfig.getCurrencySymbol(depCurr)
        val goalTarget = targetGoal.targetAmount ?: 0.0
        val remaining = (goalTarget - targetGoal.savedAmount).coerceAtLeast(0.0)

        AlertDialog(
            onDismissRequest = {
                goalToDeposit = null
                depositAmountInput = ""
            },
            containerColor = paper.sheet,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "🐷 Abonar a '${targetGoal.title}'",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = paper.ink
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (goalTarget > 0.0) {
                            "Ahorrado actual: $depSym${String.format(Locale.US, "%,.2f", targetGoal.savedAmount)} de $depSym${String.format(Locale.US, "%,.2f", goalTarget)} $depCurr"
                        } else {
                            "Suma fondos a tu meta en $depCurr"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = paper.inkMuted
                    )

                    OutlinedTextField(
                        value = depositAmountInput,
                        onValueChange = { depositAmountInput = it },
                        label = { Text("Monto a abonar ($depCurr)") },
                        prefix = { Text("$depSym ", fontWeight = FontWeight.Bold, color = paper.ink) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FamiCoral,
                            unfocusedBorderColor = paper.border.copy(alpha = 0.5f),
                            focusedTextColor = paper.ink,
                            unfocusedTextColor = paper.ink
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10.0, 25.0, 50.0, 100.0).forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(paper.stickyYellow)
                                    .border(1.dp, paper.border.copy(alpha = 0.45f), CircleShape)
                                    .clickable { depositAmountInput = preset.toInt().toString() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "+$depSym${preset.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                            }
                        }
                        if (remaining > 0.0) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(paper.stickyMint)
                                    .border(1.dp, FamiEmerald.copy(alpha = 0.6f), CircleShape)
                                    .clickable { depositAmountInput = String.format(Locale.US, "%.2f", remaining) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Completar ($depSym${String.format(Locale.US, "%,.0f", remaining)})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = FamiEmerald
                                )
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    goalToDeposit = null
                    depositAmountInput = ""
                }) {
                    Text("Cancelar", color = paper.inkMuted)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val depVal = depositAmountInput.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (depVal > 0.0) {
                            viewModel.addSavingsToGoal(targetGoal, depVal)
                            goalToDeposit = null
                            depositAmountInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FamiEmerald),
                    shape = CircleShape
                ) {
                    Text("Abonar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        )
    }

    if (showAddNoteDialog) {
        com.example.ui.StickyNoteEditorDialog(
            initialNote = editingNote,
            onDismiss = {
                showAddNoteDialog = false
                editingNote = null
            },
            onSave = { title, content, colorKey, category, isPinned, isPrivate, checklist ->
                if (editingNote != null) {
                    val updated = editingNote!!.copy(
                        title = title,
                        content = content,
                        colorKey = colorKey,
                        category = category,
                        isPinned = isPinned,
                        isPrivate = isPrivate,
                        checklist = checklist
                    )
                    viewModel.editNote(updated)
                } else {
                    viewModel.addNote(
                        title = title,
                        content = content,
                        colorKey = colorKey,
                        category = category,
                        isPinned = isPinned,
                        isPrivate = isPrivate,
                        checklist = checklist
                    )
                }
                showAddNoteDialog = false
                editingNote = null
            },
            onDelete = if (editingNote != null) {
                {
                    viewModel.deleteNote(editingNote!!)
                    showAddNoteDialog = false
                    editingNote = null
                }
            } else null
        )
    }
}
