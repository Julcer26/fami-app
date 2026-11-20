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

// --- TAB 3: TAREAS CON PRIORIDADES ---
@Composable
fun TasksTab(
    viewModel: FamilyViewModel,
    onOpenConfig: () -> Unit,
    pendingChoreId: String? = null,
    onConsumeChoreId: () -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userAvatarIdState by viewModel.userAvatarId.collectAsStateWithLifecycle()
    val chores by viewModel.chores.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val managementMode by viewModel.managementMode.collectAsStateWithLifecycle()
    val isIndividualMode = managementMode == "individual"
    val paper = FamiTheme.paper
    val s = com.example.ui.LocalFamiStrings.current

    var showChoreDialog by remember { mutableStateOf(false) }
    var isEditingChore by remember { mutableStateOf(false) }
    var editingChore by remember { mutableStateOf<Chore?>(null) }

    var taskTitle by remember { mutableStateOf("") }
    var taskCategorySelected by remember { mutableStateOf("General") }
    var taskDesc by remember { mutableStateOf("") }
    var taskAssignee by remember(currentUser, profiles) {
        mutableStateOf(cleanMemberAlias(currentUser, profiles.firstOrNull()?.name ?: "Todos"))
    }
    var taskPrioritySelected by remember { mutableStateOf("Medio") } // Alto, Medio, Bajo
    var taskIsPrivate by remember { mutableStateOf(false) }

    var activeTaskFilter by remember { mutableStateOf("pendientes") } // "pendientes" or "completadas"

    var showListManager by remember { mutableStateOf(false) }
    var listNameToEdit by remember { mutableStateOf<String?>(null) }
    var editListNameInput by remember { mutableStateOf("") }

    val listNames = remember(chores, settings) {
        val dbListsSetting = settings.find { it.key == "task_lists" }?.value
        val dbLists = dbListsSetting?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val itemLists = chores.map { it.category }
        val allButGeneral = (dbLists + itemLists).filter { !it.equals("General", ignoreCase = true) && !it.equals("Todas", ignoreCase = true) }.distinct().sorted()
        listOf("Todas") + allButGeneral
    }
    var selectedListName by remember { mutableStateOf("Todas") }
    var selectedPriorityFilter by remember { mutableStateOf("Todas") }
    var selectedMemberFilter by remember { mutableStateOf("Todos") }

    val userChores = remember(chores, currentUser) {
        chores.filter { !it.isPrivate || it.createdBy == currentUser || it.assignedTo == currentUser }
    }
    val pendingChores = remember(userChores) { userChores.filter { !it.isCompleted } }
    val completedChores = remember(userChores) { userChores.filter { it.isCompleted } }
    val shownChores = remember(pendingChores, completedChores, activeTaskFilter, selectedListName, selectedPriorityFilter, selectedMemberFilter, isIndividualMode, currentUser) {
        val cleanCurUser = cleanMemberAlias(currentUser)
        (if (activeTaskFilter == "pendientes") pendingChores else completedChores).filter {
            val matchesList = selectedListName.equals("Todas", ignoreCase = true) || it.category.equals(selectedListName, ignoreCase = true)
            val matchesPriority = selectedPriorityFilter.equals("Todas", ignoreCase = true) || it.priority.equals(selectedPriorityFilter, ignoreCase = true)
            val matchesMember = isIndividualMode ||
                selectedMemberFilter.equals("Todos", ignoreCase = true) ||
                (selectedMemberFilter == "Mis tareas" && (it.assignedTo.equals(cleanCurUser, ignoreCase = true) || it.assignedTo.equals("Todos", ignoreCase = true))) ||
                it.assignedTo.equals(selectedMemberFilter, ignoreCase = true)
            matchesList && matchesPriority && matchesMember
        }
    }

    val tasksListState = rememberLazyListState()
    var highlightedChoreId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pendingChoreId, chores) {
        val targetId = pendingChoreId?.trim().orEmpty()
        if (targetId.isBlank()) return@LaunchedEffect
        val match = chores.firstOrNull { it.id == targetId } ?: return@LaunchedEffect
        activeTaskFilter = if (match.isCompleted) "completadas" else "pendientes"
        selectedListName = "Todas"
        selectedPriorityFilter = "Todas"
        selectedMemberFilter = "Todos"
        val targetList = if (match.isCompleted) {
            userChores.filter { it.isCompleted }
        } else {
            userChores.filter { !it.isCompleted }
        }
        val idx = targetList.indexOfFirst { it.id == targetId }
        if (idx >= 0) {
            kotlinx.coroutines.delay(80L)
            tasksListState.animateScrollToItem(4 + idx)
            highlightedChoreId = targetId
        }
        onConsumeChoreId()
    }

    LaunchedEffect(highlightedChoreId) {
        if (highlightedChoreId != null) {
            kotlinx.coroutines.delay(2500L)
            highlightedChoreId = null
        }
    }

    LazyColumn(
        state = tasksListState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 150.dp) ){
        // App top header of tasks
        item {
            Spacer(modifier = Modifier.height(10.dp))
            ModernTabHeader(
                title = s.choresTitle,
                subtitle = "${completedChores.size}/${userChores.size} ${s.choresCompletedSuffix}",
                actionText = s.choresNewBtn,
                onActionClick = {
                    isEditingChore = false
                    editingChore = null
                    taskTitle = ""
                    taskDesc = ""
                    taskCategorySelected = if (selectedListName == "Todas") "General" else selectedListName
                    taskAssignee = cleanMemberAlias(currentUser, profiles.firstOrNull()?.name ?: "Todos")
                    taskPrioritySelected = "Medio"
                    taskIsPrivate = false
                    showChoreDialog = true
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Tareas lists selector row
        item {
            var showAddListDialog by remember { mutableStateOf(false) }
            var newListNameInput by remember { mutableStateOf("") }
            
            if (showAddListDialog) {
                AlertDialog(
                    onDismissRequest = { showAddListDialog = false },
                    containerColor = paper.sheet,
                    title = { Text("Nueva Lista de Tareas", fontSize = 16.sp, fontWeight = FontWeight.Black, color = paper.ink) },
                    text = {
                        OutlinedTextField(
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                            value = newListNameInput,
                            onValueChange = { newListNameInput = it },
                            placeholder = { Text("Ej: Limpieza, Jardín...", color = paper.inkMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FamiCoral,
                                unfocusedBorderColor = paper.border.copy(alpha = 0.5f),
                                focusedTextColor = paper.ink,
                                unfocusedTextColor = paper.ink
                            )
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val cleanList = newListNameInput.replace(",", " ").trim()
                                if (cleanList.isNotEmpty() && !cleanList.equals("General", ignoreCase = true) && !cleanList.equals("Todas", ignoreCase = true)) {
                                    val currentSetting = settings.find { it.key == "task_lists" }?.value
                                    val newSettingVal = if (currentSetting.isNullOrBlank()) cleanList else "$currentSetting,$cleanList"
                                    viewModel.updateValueSetting("task_lists", newSettingVal)
                                    selectedListName = cleanList
                                }
                                newListNameInput = ""
                                showAddListDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FamiCoral)
                        ) {
                            Text("Crear", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddListDialog = false }) {
                            Text("Cancelar", color = paper.inkMuted)
                        }
                    }
                )
            }

            if (showListManager) {
                AlertDialog(
                    onDismissRequest = { showListManager = false },
                    containerColor = paper.sheet,
                    title = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Mis Listas de Tareas", fontSize = 18.sp, fontWeight = FontWeight.Black, color = paper.ink)
                            IconButton(onClick = { showListManager = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = paper.inkMuted)
                            }
                        }
                    },
                    text = {
                        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp) ){
                            items(listNames, key = { it }) { listName ->
                                val isEditing = listNameToEdit == listName
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = paper.sheetCream),
                                    border = BorderStroke(1.dp, paper.border.copy(alpha = 0.35f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ){
                                    if (isEditing) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(
                                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                                                value = editListNameInput,
                                                onValueChange = { editListNameInput = it },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = FamiCoral,
                                                    unfocusedBorderColor = paper.border.copy(alpha = 0.5f),
                                                    focusedTextColor = paper.ink,
                                                    unfocusedTextColor = paper.ink
                                                )
                                            )
                                            IconButton(onClick = {
                                                val cleanList = editListNameInput.replace(",", " ").trim()
                                                if (cleanList.isNotEmpty() && cleanList != listName && !cleanList.equals("General", ignoreCase = true) && !cleanList.equals("Todas", ignoreCase = true)) {
                                                    viewModel.renameTaskList(listName, cleanList)
                                                    if (selectedListName == listName) {
                                                        selectedListName = cleanList
                                                    }
                                                }
                                                listNameToEdit = null
                                            }) {
                                                Icon(Icons.Default.Check, contentDescription = "Guardar", tint = FamiCoral)
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                selectedListName = listName
                                                showListManager = false
                                            }.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(listName, fontSize = 16.sp, fontWeight = if (selectedListName == listName) FontWeight.Bold else FontWeight.Normal, color = paper.ink)
                                            if (listName != "General") {
                                                Row {
                                                    IconButton(onClick = {
                                                        listNameToEdit = listName
                                                        editListNameInput = listName
                                                    }, modifier = Modifier.size(32.dp) ){
                                                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = paper.ink, modifier = Modifier.size(20.dp))
                                                    }
                                                    DeleteIconButton(
                                                        modifier = Modifier.size(32.dp),
                                                        iconSize = 20.dp,
                                                        tint = AlertRed,
                                                        onDeleteConfirm = {
                                                            val currentSetting = settings.find { it.key == "task_lists" }?.value
                                                            val dbLists = currentSetting?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
                                                            dbLists.remove(listName)
                                                            viewModel.updateValueSetting("task_lists", dbLists.joinToString(","))
                                                            if (selectedListName == listName) {
                                                                selectedListName = "Todas"
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showAddListDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = FamiCoral)
                        ){
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Crear Nueva Lista", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            ModernListSelectorChips(
                listNames = listNames,
                selectedList = selectedListName,
                onSelectList = { selectedListName = it },
                onAddListClick = { showAddListDialog = true },
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Filter pills (Pendientes vs Completadas)
        item {
            ModernFilterPills(
                options = listOf(
                    "pendientes" to "${s.choresPendingTab} (${pendingChores.size})",
                    "completadas" to "${s.choresCompletedTab} (${completedChores.size})"
                ),
                selectedId = activeTaskFilter,
                onSelect = { activeTaskFilter = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Priority filter selector row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val priorities = listOf("Todas", "Alto", "Medio", "Bajo")
                priorities.forEach { priority ->
                    val isSelected = selectedPriorityFilter == priority
                    val priorityLabel = when (priority) {
                        "Alto" -> s.choresHighPriority
                        "Medio" -> s.choresMedPriority
                        "Bajo" -> s.choresLowPriority
                        else -> s.choresAllFilter
                    }
                    val count = when (priority) {
                        "Todas" -> (if (activeTaskFilter == "pendientes") pendingChores else completedChores).filter {
                            selectedListName.equals("Todas", ignoreCase = true) || it.category.equals(selectedListName, ignoreCase = true)
                        }.size
                        else -> (if (activeTaskFilter == "pendientes") pendingChores else completedChores).filter {
                            (selectedListName.equals("Todas", ignoreCase = true) || it.category.equals(selectedListName, ignoreCase = true)) &&
                            it.priority.equals(priority, ignoreCase = true)
                        }.size
                    }

                    val badgeColor = when (priority) {
                        "Alto" -> Color(0xFFEF5350)
                        "Medio" -> Color(0xFFFFB74D)
                        "Bajo" -> Color(0xFF81C784)
                        else -> FamiCoral
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) badgeColor.copy(alpha = 0.20f)
                                else paper.sheetCream
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) badgeColor else paper.border.copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                            .clickable { selectedPriorityFilter = priority }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp) ){
                            if (priority != "Todas") {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor)
                                )
                            }
                            Text(
                                text = "$priorityLabel ($count)",
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) paper.ink else paper.inkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            if (!isIndividualMode && profiles.size > 1) {
                Spacer(modifier = Modifier.height(6.dp))
                val memberFilterOptions = remember(profiles) {
                    (listOf("Todos", "Mis tareas") + profiles.map { cleanMemberAlias(it.name) }).distinct()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    memberFilterOptions.forEach { memberOpt ->
                        val isMemSelected = selectedMemberFilter == memberOpt
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isMemSelected) FamiCoral else paper.sheetCream)
                                .border(1.dp, if (isMemSelected) FamiCoral else paper.border.copy(alpha = 0.35f), CircleShape)
                                .clickable { selectedMemberFilter = memberOpt }
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (memberOpt == "Mis tareas") "👤 Mis tareas" else memberOpt,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isMemSelected) Color.White else paper.ink
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Homework lists
        if (shownChores.isEmpty()) {
            item {
                ModernEmptyState(
                    icon = if (activeTaskFilter == "pendientes") Icons.Default.CheckCircle else Icons.Default.DoneAll,
                    title = if (activeTaskFilter == "pendientes") s.choresEmptyPendingTitle else s.choresEmptyDoneTitle,
                    subtitle = if (activeTaskFilter == "pendientes") s.choresEmptyPendingSub else s.choresEmptyDoneSub,
                    actionText = if (activeTaskFilter == "pendientes") "+ ${s.choresNewBtn}" else null,
                    onActionClick = {
                        isEditingChore = false
                        editingChore = null
                        taskTitle = ""
                        taskDesc = ""
                        taskCategorySelected = if (selectedListName == "Todas") "General" else selectedListName
                        taskAssignee = currentUser.ifBlank { profiles.firstOrNull()?.name ?: "Todos" }
                        taskPrioritySelected = "Medio"
                        taskIsPrivate = false
                        showChoreDialog = true
                    }
                )
            }
        } else {
            items(shownChores, key = { it.id.ifEmpty { "${it.title}_${it.createdDate}" } }) { chore ->
                    val priorityColor = when (chore.priority.lowercase() ){
                        "alto" -> AlertRed
                        "medio" -> Color(0xFFF57C00)
                        else -> Color(0xFF00796B)
                    }

                    val isHighlighted = chore.id.isNotBlank() && chore.id == highlightedChoreId
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        com.example.ui.NotebookPaperSheet(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isHighlighted) {
                                        Modifier.border(2.dp, FamiCoral, RoundedCornerShape(14.dp))
                                    } else {
                                        Modifier
                                    }
                                ),
                            sheetColor = if (isHighlighted) {
                                paper.stickyYellow
                            } else if (chore.isCompleted) {
                                paper.sheetCream
                            } else {
                                paper.sheet
                            },
                            showRuledLines = true,
                            showMarginLine = true,
                            showSpiralTop = false,
                            onClick = {
                                editingChore = chore
                                isEditingChore = true
                                taskTitle = chore.title
                                taskDesc = chore.description
                                taskCategorySelected = chore.category
                                taskAssignee = chore.assignedTo
                                taskPrioritySelected = chore.priority
                                taskIsPrivate = chore.isPrivate
                                showChoreDialog = true
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 28.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Checkbox rounded toggle with soft ring and check mark
                                val checkboxBg = if (chore.isCompleted) FamiEmerald else Color.Transparent
                                val checkboxBorderColor = if (chore.isCompleted) FamiEmerald else priorityColor

                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(checkboxBg)
                                        .border(2.dp, checkboxBorderColor, CircleShape)
                                        .clickable { viewModel.toggleChoreCompletion(chore) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (chore.isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Desmarcar",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        com.example.ui.RuledNotebookText(
                                            text = chore.title,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (chore.isCompleted) paper.inkMuted else paper.ink,
                                            lineHeight = 24.sp,
                                            textDecoration = if (chore.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Priority badge tag
                                        val priorityBg = when (chore.priority.lowercase()) {
                                            "alto" -> if (paper.isDark) Color(0xFF451C1F) else Color(0xFFFEE2E2)
                                            "medio" -> if (paper.isDark) Color(0xFF432B18) else Color(0xFFFFEDD5)
                                            else -> if (paper.isDark) Color(0xFF153732) else Color(0xFFCCFBF1)
                                        }
                                        val priorityTextCol = when (chore.priority.lowercase()) {
                                            "alto" -> if (paper.isDark) Color(0xFFF87171) else Color(0xFFEF4444)
                                            "medio" -> if (paper.isDark) Color(0xFFFB923C) else Color(0xFFF97316)
                                            else -> if (paper.isDark) FamiEmerald else Color(0xFF0D9488)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (chore.isCompleted) paper.sheetCream else priorityBg)
                                                .border(1.dp, paper.border.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = chore.priority.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (chore.isCompleted) paper.inkMuted else priorityTextCol,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                    if (chore.description.isNotBlank()) {
                                        com.example.ui.RuledNotebookText(
                                            text = chore.description,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = paper.inkMuted,
                                            maxLines = 3,
                                            lineHeight = 24.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val cleanAssignee = cleanMemberAlias(chore.assignedTo, "Todos")
                                        if (!isIndividualMode) {
                                            com.example.ui.MemberAvatarBadge(
                                                memberName = cleanAssignee,
                                                profiles = profiles,
                                                currentUser = currentUser,
                                                currentUserAvatarId = userAvatarIdState,
                                                size = 26.dp,
                                                showAliasBelow = true,
                                                aliasText = cleanAssignee,
                                                aliasFontSize = 9.sp,
                                                aliasColor = paper.inkMuted,
                                                aliasMaxWidth = 58.dp
                                            )
                                        }
                                        Text(
                                            text = if (isIndividualMode) chore.category else " • ${chore.category}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = paper.inkMuted
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                editingChore = chore
                                                isEditingChore = true
                                                taskTitle = chore.title
                                                taskDesc = chore.description
                                                taskCategorySelected = chore.category
                                                taskAssignee = cleanAssignee
                                                taskPrioritySelected = chore.priority
                                                taskIsPrivate = chore.isPrivate
                                                showChoreDialog = true
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(paper.sheetCream, CircleShape)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = paper.ink, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        DeleteIconButton(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(AlertRed.copy(alpha = 0.10f), CircleShape),
                                            iconSize = 16.dp,
                                            tint = AlertRed,
                                            onDeleteConfirm = { viewModel.deleteChore(chore) }
                                        )
                                    }
                                }
                            }
                        }
                    } // End Box
                }
            }
        }

    // Modal Sheet of Chores Dialog Creator
    if (showChoreDialog) {
        AlertDialog(
            onDismissRequest = { showChoreDialog = false },
            title = {
                Text(
                    text = if (isEditingChore) "Editar Tarea" else "Programar Nueva Tarea",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = paper.ink
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp) ){
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Título de la tarea") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FamiCoral,
                            unfocusedBorderColor = paper.border.copy(alpha = 0.5f),
                            focusedTextColor = paper.ink,
                            unfocusedTextColor = paper.ink
                        )
                    )

                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = taskDesc,
                        onValueChange = { taskDesc = it },
                        label = { Text("Descripción (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FamiCoral,
                            unfocusedBorderColor = paper.border.copy(alpha = 0.5f),
                            focusedTextColor = paper.ink,
                            unfocusedTextColor = paper.ink
                        ),
                        maxLines = 3
                    )

                    // Lista dropdown selector
                    var expandedListDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth() ){
                        OutlinedTextField(
                            value = taskCategorySelected,
                            onValueChange = {},
                            label = { Text("Lista") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().clickable { expandedListDropdown = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FamiCoral,
                                unfocusedBorderColor = paper.border.copy(alpha = 0.5f),
                                focusedTextColor = paper.ink,
                                unfocusedTextColor = paper.ink
                            ),
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Seleccionar lista",
                                    tint = paper.inkMuted,
                                    modifier = Modifier.clickable { expandedListDropdown = !expandedListDropdown })
                            }
                        )
                        DropdownMenu(
                            expanded = expandedListDropdown,
                            onDismissRequest = { expandedListDropdown = false }
                        ) {
                            listNames.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        taskCategorySelected = name
                                        expandedListDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Priority segments layout
                    Column {
                        Text("Prioridad de Ejecución:", fontSize = 11.sp, color = paper.inkMuted, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp) ){
                            listOf("Bajo", "Medio", "Alto").forEach { pOption ->
                                val optionColor = when (pOption) {
                                    "Alto" -> if (paper.isDark) Color(0xFFF87171) else AlertRed
                                    "Medio" -> if (paper.isDark) Color(0xFFFB923C) else Color(0xFFF57C00)
                                    else -> if (paper.isDark) FamiEmerald else Color(0xFF00796B)
                                }
                                val isChosen = taskPrioritySelected.equals(pOption, ignoreCase = true)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .background(
                                            if (isChosen) optionColor
                                            else optionColor.copy(alpha = 0.14f)
                                        )
                                        .border(
                                            width = if (isChosen) 2.dp else 1.dp,
                                            color = if (isChosen) optionColor else paper.border.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable { taskPrioritySelected = pOption }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pOption.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChosen) Color.White else optionColor
                                    )
                                }
                            }
                        }
                    }

                    if (!isIndividualMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Switch(
                                checked = taskIsPrivate,
                                onCheckedChange = { taskIsPrivate = it },
                                colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = FamiCoral)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Solo para mí (Privado)", fontSize = 14.sp, color = paper.ink)
                        }

                        // Assignee dropdown picker list (Avatar con alias debajo)
                        Column {
                            Text("Asignar esta tarea a:", fontSize = 11.sp, color = paper.inkMuted, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp) ){
                                val activeProfiles = (profiles.map { cleanMemberAlias(it.name) }.ifEmpty { listOf(cleanMemberAlias(currentUser)) } + "Todos").distinct()

                                activeProfiles.forEach { name ->
                                    val isSelected = taskAssignee == name
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) FamiCoral
                                                else paper.sheetCream
                                            )
                                            .clickable { taskAssignee = name }
                                            .border(
                                                width = if (isSelected) 1.6.dp else 1.dp,
                                                color = if (isSelected) FamiCoral else paper.border.copy(alpha = 0.45f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ){
                                        com.example.ui.MemberAvatarBadge(
                                            memberName = name,
                                            profiles = profiles,
                                            currentUser = currentUser,
                                            currentUserAvatarId = userAvatarIdState,
                                            size = 28.dp,
                                            showAliasBelow = true,
                                            aliasText = name,
                                            aliasFontSize = 10.sp,
                                            aliasColor = if (isSelected) Color.White else paper.ink,
                                            aliasMaxWidth = 64.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank() ){
                            val safeUser = cleanMemberAlias(currentUser)
                            val finalAssignee = if (isIndividualMode || taskIsPrivate) safeUser else taskAssignee
                            if (isEditingChore && editingChore != null) {
                                val updated = editingChore!!.copy(
                                        title = taskTitle.trim(),
                                        description = taskDesc.trim(),
                                        priority = taskPrioritySelected,
                                        category = taskCategorySelected,
                                        assignedTo = finalAssignee,
                                        isPrivate = taskIsPrivate
                                )
                                viewModel.editChore(updated)
                            } else {
                                viewModel.addChore(
                                    title = taskTitle.trim(),
                                    assignedTo = finalAssignee,
                                    description = taskDesc.trim(),
                                    priority = taskPrioritySelected,
                                    category = taskCategorySelected,
                                    isPrivate = taskIsPrivate
                                )
                            }
                            showChoreDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FamiCoral),
                    shape = CircleShape
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChoreDialog = false }) {
                    Text("Cancelar", color = paper.inkMuted)
                }
            },
            containerColor = paper.sheet,
            shape = RoundedCornerShape(28.dp)
        )
    }
}
