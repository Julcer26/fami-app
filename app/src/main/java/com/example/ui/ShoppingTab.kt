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

// --- TAB 3.5: LISTA DE COMPRAS SEPARADA ---
@Composable
fun ComprasTab(viewModel: FamilyViewModel, onOpenConfig: () -> Unit) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userAvatarIdState by viewModel.userAvatarId.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val managementMode by viewModel.managementMode.collectAsStateWithLifecycle()
    val isIndividualMode = managementMode == "individual"
    val paper = FamiTheme.paper
    val s = com.example.ui.LocalFamiStrings.current
    val shoppingItems by viewModel.shoppingItems.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var editName by remember { mutableStateOf("") }
    var editQty by remember { mutableStateOf("") }

    var itemNameInput by remember { mutableStateOf("") }
    var itemQtyInput by remember { mutableStateOf("") }
    var itemIsPrivate by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    var activeShoppingFilter by remember { mutableStateOf("pendientes") } // "pendientes" or "comprados"
    var selectedListName by remember { mutableStateOf("General") }
    var showListManager by remember { mutableStateOf(false) }
    var listNameToEdit by remember { mutableStateOf<String?>(null) }
    var editListNameInput by remember { mutableStateOf("") }
    val listNames = remember(shoppingItems, settings) {
        val dbListsSetting = settings.find { it.key == "shopping_lists" }?.value
        val dbLists = dbListsSetting?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val itemLists = shoppingItems.map { it.listName }
        (listOf("General") + dbLists + itemLists).distinct()
    }
    var editSelectedList by remember { mutableStateOf("General") }
    var itemSelectedList by remember(selectedListName) { mutableStateOf(selectedListName) }

    val userShoppingItems = remember(shoppingItems, currentUser, selectedListName) {
        shoppingItems.filter { (!it.isPrivate || it.addedBy == currentUser) && (selectedListName == "General" || it.listName == selectedListName) }
    }
    val pendingShopping = remember(userShoppingItems) { userShoppingItems.filter { !it.isBought } }
    val boughtShopping = remember(userShoppingItems) { userShoppingItems.filter { it.isBought } }
    val shownShopping = remember(pendingShopping, boughtShopping, activeShoppingFilter) {
        if (activeShoppingFilter == "pendientes") pendingShopping else boughtShopping
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 120.dp) ){
        // Top header block
        item {
            Spacer(modifier = Modifier.height(10.dp))
            ModernTabHeader(
                title = s.marketTitle,
                subtitle = "${boughtShopping.size}/${userShoppingItems.size} ${s.marketReadySuffix}",
                actionText = s.marketNewBtn,
                onActionClick = {
                    itemNameInput = ""
                    itemQtyInput = ""
                    itemIsPrivate = false
                    showAddItemDialog = true
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Shopping lists selector row
        item {
            var showAddListDialog by remember { mutableStateOf(false) }
            var newListNameInput by remember { mutableStateOf("") }
            
            if (showAddListDialog) {
                AlertDialog(
                    onDismissRequest = { showAddListDialog = false },
                    containerColor = paper.sheet,
                    title = { Text("Nueva Lista de Compras", fontSize = 16.sp, fontWeight = FontWeight.Black, color = paper.ink) },
                    text = {
                        OutlinedTextField(
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                            value = newListNameInput,
                            onValueChange = { newListNameInput = it },
                            placeholder = { Text("Ej: Supermercado, Farmacia...", color = paper.inkMuted) },
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
                                val cleanList = newListNameInput.trim()
                                if (cleanList.isNotEmpty() && cleanList != "General") {
                                    val currentSetting = settings.find { it.key == "shopping_lists" }?.value
                                    val newSettingVal = if (currentSetting.isNullOrBlank()) cleanList else "$currentSetting,$cleanList"
                                    viewModel.updateValueSetting("shopping_lists", newSettingVal)
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
                            Text("Mis Listas de Compras", fontSize = 18.sp, fontWeight = FontWeight.Black, color = paper.ink)
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
                                                val cleanList = editListNameInput.trim()
                                                if (cleanList.isNotEmpty() && cleanList != listName && cleanList != "General") {
                                                    viewModel.renameShoppingList(listName, cleanList)
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
                                                            viewModel.deleteShoppingList(listName)
                                                            if (selectedListName == listName) {
                                                                selectedListName = "General"
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

        // Filter pills (Por Comprar vs Comprados)
        item {
            ModernFilterPills(
                options = listOf(
                    "pendientes" to "${s.marketPendingTab} (${pendingShopping.size})",
                    "comprados" to "${s.marketBoughtTab} (${boughtShopping.size})"
                ),
                selectedId = activeShoppingFilter,
                onSelect = { activeShoppingFilter = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Shopping List tracker items
        if (shownShopping.isEmpty()) {
            item {
                ModernEmptyState(
                    icon = Icons.Default.ShoppingCart,
                    title = if (activeShoppingFilter == "pendientes") s.marketEmptyPendingTitle else s.marketEmptyDoneTitle,
                    subtitle = if (activeShoppingFilter == "pendientes") s.marketEmptyPendingSub else s.marketEmptyDoneSub,
                    actionText = if (activeShoppingFilter == "pendientes") s.marketAddItemBtn else null,
                    onActionClick = {
                        itemNameInput = ""
                        itemQtyInput = ""
                        itemIsPrivate = false
                        showAddItemDialog = true
                    }
                )
            }
        } else {
            items(shownShopping, key = { it.id.ifEmpty { "${it.name}_${it.dateAdded}" } }) { item ->
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    com.example.ui.NotebookPaperSheet(
                        modifier = Modifier.fillMaxWidth(),
                        sheetColor = if (item.isBought) paper.sheetCream else paper.sheet,
                        showRuledLines = true,
                        showMarginLine = true,
                        showSpiralTop = false,
                        onClick = {
                            editingItem = item
                            editName = item.name
                            editQty = item.quantity
                            editSelectedList = item.listName
                            itemIsPrivate = item.isPrivate
                            showEditDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 28.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Toggle Status Bought circle Checkmark
                            val checkboxBg = if (item.isBought) FamiEmerald else Color.Transparent
                            val checkboxBorderColor = if (item.isBought) FamiEmerald else FamiCoral

                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(checkboxBg)
                                    .border(2.dp, checkboxBorderColor, CircleShape)
                                    .clickable { viewModel.toggleShoppingItemBought(item) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.isBought) {
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
                                com.example.ui.RuledNotebookText(
                                    text = item.name,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (item.isBought) paper.inkMuted else paper.ink,
                                    maxLines = 2,
                                    lineHeight = 24.sp,
                                    textDecoration = if (item.isBought) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                )
                                if (!isIndividualMode) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val cleanAdder = cleanMemberAlias(item.addedBy)
                                    com.example.ui.MemberAvatarBadge(
                                        memberName = cleanAdder,
                                        profiles = profiles,
                                        currentUser = currentUser,
                                        currentUserAvatarId = userAvatarIdState,
                                        size = 24.dp,
                                        showAliasBelow = true,
                                        aliasText = cleanAdder,
                                        aliasFontSize = 9.sp,
                                        aliasColor = paper.inkMuted,
                                        aliasMaxWidth = 58.dp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Controls container Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Unified Quantity Pill
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(paper.stickyYellow)
                                        .border(1.2.dp, paper.border, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 2.dp, vertical = 1.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val currentQtyNum = item.quantity.toIntOrNull() ?: 1
                                            val nextQtyNum = if (currentQtyNum > 1) currentQtyNum - 1 else 1
                                            viewModel.editShoppingItem(item.copy(quantity = nextQtyNum.toString()))
                                        },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Disminuir",
                                            tint = paper.ink,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }

                                    Text(
                                        text = item.quantity,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = paper.ink,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )

                                    IconButton(
                                        onClick = {
                                            val currentQtyNum = item.quantity.toIntOrNull() ?: 1
                                            val nextQtyNum = currentQtyNum + 1
                                            viewModel.editShoppingItem(item.copy(quantity = nextQtyNum.toString()))
                                        },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Aumentar",
                                            tint = paper.ink,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        editingItem = item
                                        editName = item.name
                                        editQty = item.quantity
                                        editSelectedList = item.listName
                                        itemIsPrivate = item.isPrivate
                                        showEditDialog = true
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(paper.sheetCream, CircleShape)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = paper.ink, modifier = Modifier.size(15.dp))
                                }
                                DeleteIconButton(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(AlertRed.copy(alpha = 0.10f), CircleShape),
                                    iconSize = 15.dp,
                                    tint = AlertRed,
                                    onDeleteConfirm = { viewModel.deleteShoppingItem(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue Sheet editing shopping item
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar Artículo", fontWeight = FontWeight.Black, fontSize = 16.sp, color = paper.ink) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp) ){
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nombre del artículo") },
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
                        value = editQty,
                        onValueChange = { editQty = it },
                        label = { Text("Cantidad") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FamiCoral,
                            unfocusedBorderColor = paper.border.copy(alpha = 0.5f),
                            focusedTextColor = paper.ink,
                            unfocusedTextColor = paper.ink
                        )
                    )

                    if (!isIndividualMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Switch(
                                checked = itemIsPrivate,
                                onCheckedChange = { itemIsPrivate = it },
                                colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = FamiCoral)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Solo para mí (Privado)", fontSize = 14.sp, color = paper.ink)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    // Lista dropdown selector
                    var expandedShopListDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth() ){
                        OutlinedTextField(
                            value = editSelectedList,
                            onValueChange = {},
                            label = { Text("Lista") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().clickable { expandedShopListDropdown = true },
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
                                    modifier = Modifier.clickable { expandedShopListDropdown = !expandedShopListDropdown })
                            }
                        )
                        DropdownMenu(
                            expanded = expandedShopListDropdown,
                            onDismissRequest = { expandedShopListDropdown = false }
                        ) {
                            listNames.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        editSelectedList = name
                                        expandedShopListDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank() && editingItem != null) {
                            val targetQty = editQty.trim().ifBlank { "1" }
                            val updated = editingItem!!.copy(
                                name = editName.trim(),
                                quantity = targetQty,
                                listName = editSelectedList
                            )
                            updated.isPrivate = itemIsPrivate
                            viewModel.editShoppingItem(updated)
                            showEditDialog = false
                            editingItem = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FamiCoral),
                    shape = CircleShape
                ) {
                    Text("Actualizar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditDialog = false
                    editingItem = null
                }) {
                    Text("Cancelar", color = paper.inkMuted)
                }
            },
            containerColor = paper.sheet,
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showAddItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Nuevo Artículo", fontWeight = FontWeight.Black, fontSize = 16.sp, color = paper.ink) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp) ){
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = itemNameInput,
                        onValueChange = { itemNameInput = it },
                        label = { Text("Nombre del artículo") },
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
                        value = itemQtyInput,
                        onValueChange = { itemQtyInput = it },
                        label = { Text("Cantidad") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FamiCoral,
                            unfocusedBorderColor = paper.border.copy(alpha = 0.5f),
                            focusedTextColor = paper.ink,
                            unfocusedTextColor = paper.ink
                        )
                    )
                    if (!isIndividualMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Switch(
                                checked = itemIsPrivate,
                                onCheckedChange = { itemIsPrivate = it },
                                colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = FamiCoral)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Solo para mí (Privado)", fontSize = 14.sp, color = paper.ink)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text("Lista de destino:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = paper.ink)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp) ){
                        items(listNames, key = { it }) { list ->
                            val isSelected = itemSelectedList == list
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) FamiCoral else paper.sheetCream)
                                    .border(1.dp, if (isSelected) FamiCoral else paper.border.copy(alpha = 0.4f), CircleShape)
                                    .clickable { itemSelectedList = list }
                                    .padding(horizontal = 12.dp, vertical = 6.dp) ){
                                Text(
                                    text = list,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else paper.ink
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (itemNameInput.isNotBlank() ){
                            viewModel.addShoppingItem(
                                itemNameInput.trim(),
                                itemQtyInput.trim().ifBlank { "1" },
                                isPrivate = itemIsPrivate,
                                listName = itemSelectedList
                            )
                            itemNameInput = ""
                            itemQtyInput = ""
                            itemIsPrivate = false
                            showAddItemDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FamiCoral),
                    shape = CircleShape
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddItemDialog = false
                    itemNameInput = ""
                    itemQtyInput = ""
                }) {
                    Text("Cancelar", color = paper.inkMuted)
                }
            },
            containerColor = paper.sheet,
            shape = RoundedCornerShape(28.dp)
        )
    }
}

