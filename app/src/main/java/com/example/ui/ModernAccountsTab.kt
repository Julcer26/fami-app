package com.example.ui

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.BcvExchangeRateService
import com.example.api.FinanceAiAnalyzer
import com.example.api.FinanceAiReport
import com.example.currency.CountryCurrencyConfig
import com.example.database.CategoryDetail
import com.example.database.RecurringPayment
import com.example.database.Transaction
import com.example.database.TransactionItem
import com.example.database.Wallet
import com.example.ui.theme.*
import com.example.util.FinanceExportHelper
import com.example.viewmodel.FamilyViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Paleta Fintech Oficial Fami v2.0
private val EmeraldGreen = Color(0xFF00D09C)
private val WarningAmber = Color(0xFFFFB800)
private val CoralRed = Color(0xFFEF4444)
private val IndigoAccent = Color(0xFFE54629)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModernAccountsTab(
    viewModel: FamilyViewModel,
    onOpenConfig: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val recurringPayments by viewModel.recurringPayments.collectAsStateWithLifecycle()
    val wallets by viewModel.wallets.collectAsStateWithLifecycle()
    val isAdminRole by viewModel.isAdminRole.collectAsStateWithLifecycle()
    val monthlyIncomeAmount by viewModel.monthlyIncomeAmount.collectAsStateWithLifecycle()
    val monthlyIncomeCurrency by viewModel.monthlyIncomeCurrency.collectAsStateWithLifecycle()
    val primaryCurrency by viewModel.primaryCurrency.collectAsStateWithLifecycle()
    val countryCode by viewModel.countryCode.collectAsStateWithLifecycle()
    val liveExchangeRate by viewModel.liveExchangeRate.collectAsStateWithLifecycle()

    val localCurrency = remember(primaryCurrency, countryCode) {
        primaryCurrency.ifBlank { CountryCurrencyConfig.getCountryByCode(countryCode).localCurrency }
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val monthKey = remember(selectedMonth.timeInMillis) {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(selectedMonth.time)
    }

    // Estados de Navegación y Filtros Rápidos
    var activeSection by remember { mutableStateOf("gastos") } // "gastos" | "categorias"
    var selectedCategoryFilter by remember { mutableStateOf("Todas") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }

    // Estados de IA Financiera
    var aiReport by remember { mutableStateOf<FinanceAiReport?>(null) }
    var isAnalyzingAi by remember { mutableStateOf(false) }
    var aiErrorMessage by remember { mutableStateOf<String?>(null) }
    var showAiBottomSheet by remember { mutableStateOf(false) }

    // Estados de BottomSheets y Diálogos
    var showTxSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var txToDelete by remember { mutableStateOf<Transaction?>(null) }

    var showCategorySheet by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryDetail?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryDetail?>(null) }
    var photoUrlToShow by remember { mutableStateOf<String?>(null) }

    // Estados de Ingreso Mensual y Transferencia entre Billeteras
    var showEditIncomeDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    // Estados de Pagos Recurrentes
    var showAddRecurringDialog by remember { mutableStateOf(false) }
    var showMarkPaidDialog by remember { mutableStateOf(false) }
    var showPaidDetailsDialog by remember { mutableStateOf(false) }
    var selectedRecurringPayment by remember { mutableStateOf<RecurringPayment?>(null) }
    var recNameInput by remember { mutableStateOf("") }
    var recAmountInput by remember { mutableStateOf("") }
    var recCurrencySelected by remember { mutableStateOf("USD") }
    var recDueDayInput by remember { mutableStateOf("5") }
    var recEmojiInput by remember { mutableStateOf("⚡") }

    // Configuración Global (Tasa interna y Presupuesto Total)
    val bcvRate = remember(settings, liveExchangeRate) {
        settings.find { it.key == "bcv_rate" }?.value?.toDoubleOrNull()
            ?: liveExchangeRate.takeIf { it > 0.0 }
            ?: 36.85
    }
    val totalMonthlyBudgetUsd = remember(settings) {
        settings.find { it.key == "total_limit_usd" }?.value?.toDoubleOrNull() ?: 1800.0
    }

    // Transacciones del mes seleccionado
    val monthTransactions = remember(transactions, selectedMonth.timeInMillis) {
        transactions.filter { tx ->
            val effectiveDate = if (tx.balanceDate > 0L) tx.balanceDate else tx.date
            val txCal = Calendar.getInstance().apply { timeInMillis = effectiveDate }
            txCal.get(Calendar.YEAR) == selectedMonth.get(Calendar.YEAR) &&
                txCal.get(Calendar.MONTH) == selectedMonth.get(Calendar.MONTH)
        }.sortedByDescending { if (it.balanceDate > 0L) it.balanceDate else it.date }
    }

    // Filtrado en tiempo real por Categoría + Búsqueda
    val filteredTransactions = remember(monthTransactions, selectedCategoryFilter, searchQuery) {
        monthTransactions.filter { tx ->
            val matchesCat = selectedCategoryFilter == "Todas" ||
                tx.category.equals(selectedCategoryFilter, ignoreCase = true) ||
                tx.items.any { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
            val matchesQuery = searchQuery.isBlank() ||
                tx.description.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.registeredBy.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    // Agrupación cronológica por día
    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { tx ->
            val effectiveDate = if (tx.balanceDate > 0L) tx.balanceDate else tx.date
            formatDayHeader(effectiveDate)
        }
    }

    // Métricas Financieras (Excluyendo transferencias internas entre billeteras)
    val totalSpentUsd = remember(monthTransactions) {
        monthTransactions.filter { !it.isIncome && !it.isTransfer }.sumOf { it.amount }
    }
    val totalIncomeUsd = remember(monthTransactions) {
        monthTransactions.filter { it.isIncome && !it.isTransfer }.sumOf { it.amount }
    }
    val filteredSpentUsd = remember(filteredTransactions) {
        filteredTransactions.filter { !it.isIncome && !it.isTransfer }.sumOf { it.amount }
    }

    // Análisis con IA
    fun runAiAnalysis(openModal: Boolean = false) {
        if (openModal) showAiBottomSheet = true
        isAnalyzingAi = true
        aiErrorMessage = null
        coroutineScope.launch {
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
            val monthLabel = monthFormat.format(selectedMonth.time).replaceFirstChar { it.uppercase() }
            val customKey = settings.find { it.key == "gemini_api_key" }?.value
            val result = FinanceAiAnalyzer.analyze(
                transactions = monthTransactions.filter { !it.isTransfer },
                categories = categories,
                monthName = monthLabel,
                customUserKey = customKey
            )
            result.onSuccess { report ->
                aiReport = report
                isAnalyzingAi = false
            }.onFailure { err ->
                aiErrorMessage = err.localizedMessage ?: "Error al analizar las finanzas"
                isAnalyzingAi = false
            }
        }
    }

    LaunchedEffect(selectedMonth.timeInMillis, monthTransactions.size) {
        if (monthTransactions.isNotEmpty()) {
            runAiAnalysis(openModal = false)
        } else {
            aiReport = null
        }
    }

    // Modal de IA BottomSheet
    FinanceAiBottomSheet(
        isOpen = showAiBottomSheet,
        onDismiss = { showAiBottomSheet = false },
        isLoading = isAnalyzingAi,
        report = aiReport,
        errorMessage = aiErrorMessage,
        onRefresh = { runAiAnalysis(openModal = true) }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Selector de Mes Moderno
            item {
                ModernMonthSelector(
                    selectedMonth = selectedMonth,
                    onMonthChange = { viewModel.updateSelectedMonth(it) }
                )
            }

            // 1B. Tarjeta de Ingreso Mensual Base (Configurable en Cuentas)
            item {
                MonthlyIncomeAccountCard(
                    monthlyIncomeAmount = monthlyIncomeAmount,
                    monthlyIncomeCurrency = monthlyIncomeCurrency,
                    totalMonthlyBudgetUsd = totalMonthlyBudgetUsd,
                    localCurrency = localCurrency,
                    bcvRate = bcvRate,
                    canEdit = isAdminRole,
                    onEditClick = { showEditIncomeDialog = true }
                )
            }

            // 1C. Resumen de Billeteras Multi-Moneda y Transferencias
            if (wallets.isNotEmpty()) {
                item {
                    WalletsAccountsStripCard(
                        wallets = wallets,
                        canTransfer = wallets.size >= 2,
                        onTransferClick = { showTransferDialog = true }
                    )
                }
            }

            // 2. Tarjeta Hero de Asistente IA
            item {
                FinanceAiHeroCard(
                    report = aiReport,
                    isLoading = isAnalyzingAi,
                    onOpenAnalysis = {
                        if (aiReport == null) runAiAnalysis(openModal = true)
                        else showAiBottomSheet = true
                    }
                )
            }

            // 3. Master Hero Financiero Bimoneda + Salud Presupuestaria
            item {
                MasterFinancialHeroCard(
                    spentUsd = totalSpentUsd,
                    incomeUsd = totalIncomeUsd,
                    monthlyBudgetUsd = totalMonthlyBudgetUsd,
                    localCurrency = localCurrency,
                    bcvRate = bcvRate,
                    onExportCsv = {
                        FinanceExportHelper.exportToGoogleSheetsCsv(
                            context = context,
                            transactions = monthTransactions,
                            monthCalendar = selectedMonth
                        )
                    },
                    onQuickAdd = {
                        editingTransaction = null
                        showTxSheet = true
                    }
                )
            }

            // 4. Control Segmentado + Botón de Búsqueda
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                            .padding(4.dp)
                    ) {
                        SegmentedTabButton(
                            title = "Movimientos (${monthTransactions.size})",
                            isSelected = activeSection == "gastos",
                            modifier = Modifier.weight(1f),
                            onClick = { activeSection = "gastos" }
                        )
                        SegmentedTabButton(
                            title = "Presupuestos (${categories.size})",
                            isSelected = activeSection == "categorias",
                            modifier = Modifier.weight(1f),
                            onClick = { activeSection = "categorias" }
                        )
                    }

                    if (activeSection == "gastos") {
                        FilledTonalIconButton(
                            onClick = {
                                showSearchBar = !showSearchBar
                                if (!showSearchBar) searchQuery = ""
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Buscar"
                            )
                        }
                    }
                }
            }

            // 5. SECCIÓN A: HISTORIAL DE MOVIMIENTOS
            if (activeSection == "gastos") {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Buscador Expandible
                        AnimatedVisibility(visible = showSearchBar) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Buscar por concepto, categoría o miembro...", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Carrusel horizontal de Chips de Categoría
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter == "Todas",
                                    onClick = { selectedCategoryFilter = "Todas" },
                                    label = { Text("Todas", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                            items(categories) { cat ->
                                val isSelected = selectedCategoryFilter.equals(cat.categoryName, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCategoryFilter = if (isSelected) "Todas" else cat.categoryName
                                    },
                                    leadingIcon = if (cat.iconEmoji.isNotBlank()) {
                                        { Text(cat.iconEmoji, fontSize = 14.sp) }
                                    } else null,
                                    label = { Text(cat.categoryName, fontWeight = FontWeight.Medium, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        }

                        // Banner de filtro activo
                        if (selectedCategoryFilter != "Todas" || searchQuery.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Filtro: ${filteredTransactions.size} reg. • Total: $${String.format(Locale.US, "%.2f", filteredSpentUsd)} USD",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Limpiar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        selectedCategoryFilter = "Todas"
                                        searchQuery = ""
                                    }
                                )
                            }
                        }

                        // Carrusel de Servicios y Pagos Recurrentes
                        ModernRecurringPaymentsSection(
                            recurringPayments = recurringPayments,
                            monthKey = monthKey,
                            onPaymentClick = { rec ->
                                selectedRecurringPayment = rec
                                val isPaid = rec.paidMonths[monthKey] == true
                                if (isPaid) {
                                    showPaidDetailsDialog = true
                                } else {
                                    recAmountInput = if (rec.defaultAmount > 0.0) rec.defaultAmount.toString() else ""
                                    recCurrencySelected = rec.currency
                                    showMarkPaidDialog = true
                                }
                            },
                            onAddPaymentClick = {
                                selectedRecurringPayment = null
                                recNameInput = ""
                                recAmountInput = ""
                                recCurrencySelected = "USD"
                                recDueDayInput = "5"
                                recEmojiInput = "⚡"
                                showAddRecurringDialog = true
                            },
                            onSeedDefaultPayments = {
                                viewModel.createOrEditRecurringPayment(RecurringPayment(name = "Condominio", defaultAmount = 0.0, category = "Servicios", iconEmoji = "🏢", dueDay = 5))
                                viewModel.createOrEditRecurringPayment(RecurringPayment(name = "Seguro Médico / Familiar", defaultAmount = 0.0, category = "Servicios", iconEmoji = "🛡️", dueDay = 10))
                                viewModel.createOrEditRecurringPayment(RecurringPayment(name = "Servicio Eléctrico (Luz)", defaultAmount = 0.0, category = "Servicios", iconEmoji = "⚡", dueDay = 15))
                                viewModel.createOrEditRecurringPayment(RecurringPayment(name = "Internet / WiFi", defaultAmount = 0.0, category = "Servicios", iconEmoji = "🌐", dueDay = 20))
                                viewModel.createOrEditRecurringPayment(RecurringPayment(name = "Seguro Vehicular", defaultAmount = 0.0, category = "Servicios", iconEmoji = "🚗", dueDay = 25))
                            }
                        )
                    }
                }

                // Listado de Transacciones Agrupadas por Día
                if (filteredTransactions.isEmpty()) {
                    item {
                        EmptyStateCard(
                            emoji = "🧾",
                            title = "Sin movimientos en esta vista",
                            subtitle = "Registra un gasto o ingreso tocando el botón '+ Nuevo Registro' o escanea una factura."
                        )
                    }
                } else {
                    groupedTransactions.forEach { (dayLabel, dayTxList) ->
                        val dayNetSpent = dayTxList.filter { !it.isIncome && !it.isTransfer }.sumOf { it.amount }
                        item(key = "header_$dayLabel") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dayLabel.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.8.sp
                                )
                                if (dayNetSpent > 0) {
                                    Text(
                                        text = "-$${String.format(Locale.US, "%.2f", dayNetSpent)} USD",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(dayTxList, key = { it.id.ifEmpty { "${it.date}_${it.description}_${it.originalAmount}" } }) { tx ->
                            val matchedCategory = categories.find {
                                it.categoryName.equals(tx.category, ignoreCase = true)
                            }
                            val walletName = wallets.find { it.id == tx.walletId }?.name
                            val canDeleteThisTx = viewModel.canDeleteTransaction(tx)
                            ModernTransactionCardItem(
                                tx = tx,
                                category = matchedCategory,
                                walletName = walletName,
                                onClick = {
                                    if (!tx.isTransfer) {
                                        editingTransaction = tx
                                        showTxSheet = true
                                    }
                                },
                                onViewReceipt = {
                                    if (tx.receiptUrl.isNotEmpty()) {
                                        photoUrlToShow = tx.receiptUrl
                                    }
                                },
                                onDeleteRequest = {
                                    if (canDeleteThisTx) txToDelete = tx
                                }
                            )
                        }
                    }
                }
            } else {
                // 6. SECCIÓN B: PRESUPUESTOS POR CATEGORÍA
                if (categories.isEmpty()) {
                    item {
                        EmptyStateCard(
                            emoji = "🎯",
                            title = "Configura tus presupuestos",
                            subtitle = "Crea categorías con límites mensuales en dólares para recibir alertas antes de excederte."
                        )
                    }
                } else {
                    items(categories, key = { it.categoryName }) { category ->
                        val catSpent = monthTransactions
                            .filter { !it.isIncome && !it.isTransfer && (it.category.equals(category.categoryName, ignoreCase = true) || it.items.any { item -> item.category.equals(category.categoryName, ignoreCase = true) }) }
                            .sumOf { tx ->
                                if (tx.items.isNotEmpty()) {
                                    tx.items.filter { it.category.equals(category.categoryName, ignoreCase = true) }.sumOf { it.amount }
                                } else {
                                    tx.amount
                                }
                            }
                        val currentLimit = category.limitsPerMonth[monthKey] ?: category.limitAmount

                        ModernCategoryBudgetCardItem(
                            category = category,
                            spentUsd = catSpent,
                            limitUsd = currentLimit,
                            bcvRate = bcvRate,
                            canEdit = isAdminRole,
                            onEdit = {
                                if (isAdminRole) {
                                    editingCategory = category
                                    showCategorySheet = true
                                }
                            },
                            onFilterClick = {
                                selectedCategoryFilter = category.categoryName
                                activeSection = "gastos"
                            }
                        )
                    }
                }
            }
        }

        // Botón flotante posicionado por encima de la barra de navegación inferior sin solaparse
        if (activeSection == "gastos" || isAdminRole) {
            ExtendedFloatingActionButton(
                onClick = {
                    if (activeSection == "gastos") {
                        editingTransaction = null
                        showTxSheet = true
                    } else if (isAdminRole) {
                        editingCategory = null
                        showCategorySheet = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 18.dp, bottom = 88.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (activeSection == "gastos") "Nuevo Registro" else "Nueva Categoría",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }

    // --- BOTTOM SHEET: CREAR / EDITAR TRANSACCIÓN ---
    if (showTxSheet) {
        val canDeleteEditingTx = editingTransaction?.let { viewModel.canDeleteTransaction(it) } ?: false
        TransactionFormBottomSheet(
            initialTransaction = editingTransaction,
            categories = categories,
            wallets = wallets,
            primaryCurrency = localCurrency,
            defaultBcvRate = bcvRate,
            defaultDateMillis = selectedMonth.timeInMillis,
            canDelete = canDeleteEditingTx,
            onDismiss = {
                showTxSheet = false
                editingTransaction = null
            },
            onSaveNew = { amount, category, desc, currency, isIncome, date, balanceDate, walletId ->
                viewModel.addTransaction(
                    originalAmount = amount,
                    category = category,
                    description = desc,
                    currency = currency,
                    isIncome = isIncome,
                    date = date,
                    balanceDate = balanceDate,
                    walletId = walletId
                )
                showTxSheet = false
            },
            onSaveEdit = { updatedTx ->
                viewModel.editTransaction(updatedTx)
                showTxSheet = false
                editingTransaction = null
            },
            onDelete = { tx ->
                showTxSheet = false
                editingTransaction = null
                if (viewModel.canDeleteTransaction(tx)) {
                    txToDelete = tx
                }
            },
            onViewReceipt = { receiptUrl ->
                photoUrlToShow = receiptUrl
            }
        )
    }

    // --- BOTTOM SHEET: CREAR / EDITAR CATEGORÍA ---
    if (showCategorySheet && isAdminRole) {
        CategoryFormBottomSheet(
            initialCategory = editingCategory,
            monthKey = monthKey,
            onDismiss = {
                showCategorySheet = false
                editingCategory = null
            },
            onSave = { oldName, newName, limitUsd, emoji ->
                viewModel.createOrEditCategory(oldName, newName, limitUsd, emoji, monthKey)
                showCategorySheet = false
                editingCategory = null
            },
            onDelete = { category ->
                showCategorySheet = false
                editingCategory = null
                categoryToDelete = category
            }
        )
    }

    // --- DIÁLOGO: EDITAR INGRESO MENSUAL BASE ---
    if (showEditIncomeDialog) {
        MonthlyIncomeEditDialog(
            initialAmount = monthlyIncomeAmount,
            initialCurrency = monthlyIncomeCurrency.ifBlank { localCurrency },
            localCurrency = localCurrency,
            onDismiss = { showEditIncomeDialog = false },
            onSave = { amt, curr ->
                viewModel.updateMonthlyIncome(amt, curr)
                showEditIncomeDialog = false
            }
        )
    }

    // --- DIÁLOGO: TRANSFERIR ENTRE BILLETERAS ---
    if (showTransferDialog && wallets.size >= 2) {
        WalletTransferDialog(
            wallets = wallets,
            bcvRate = bcvRate,
            localCurrency = localCurrency,
            onDismiss = { showTransferDialog = false },
            onConfirmTransfer = { fromW, toW, fromAmt, toAmt, note ->
                viewModel.transferBetweenWallets(fromW, toW, fromAmt, toAmt, note)
                showTransferDialog = false
            }
        )
    }

    // --- DIÁLOGOS DE PAGOS RECURRENTES ---
    ModernRecurringPaymentDialogs(
        monthKey = monthKey,
        selectedMonth = selectedMonth,
        selectedRecurringPayment = selectedRecurringPayment,
        showMarkPaidDialog = showMarkPaidDialog,
        showPaidDetailsDialog = showPaidDetailsDialog,
        showAddRecurringDialog = showAddRecurringDialog,
        recNameInput = recNameInput,
        recAmountInput = recAmountInput,
        recCurrencySelected = recCurrencySelected,
        recDueDayInput = recDueDayInput,
        recEmojiInput = recEmojiInput,
        onNameChange = { recNameInput = it },
        onAmountChange = { recAmountInput = it },
        onCurrencyChange = { recCurrencySelected = it },
        onDueDayChange = { recDueDayInput = it },
        onEmojiChange = { recEmojiInput = it },
        onDismissMarkPaid = { showMarkPaidDialog = false },
        onDismissPaidDetails = { showPaidDetailsDialog = false },
        onDismissAddRecurring = { showAddRecurringDialog = false },
        onSwitchToEdit = { rec ->
            selectedRecurringPayment = rec
            recNameInput = rec.name
            recAmountInput = if (rec.defaultAmount > 0.0) rec.defaultAmount.toString() else ""
            recCurrencySelected = rec.currency
            recDueDayInput = rec.dueDay.toString()
            recEmojiInput = rec.iconEmoji.ifBlank { "⚡" }
            showAddRecurringDialog = true
        },
        onConfirmMarkPaid = { rec, amt, cur, dateMillis ->
            val matchedWalletId = wallets.firstOrNull { it.currency.equals(cur, ignoreCase = true) }?.id.orEmpty()
            viewModel.markRecurringPaymentPaid(rec, monthKey, amt, cur, dateMillis, matchedWalletId)
            showMarkPaidDialog = false
        },
        onConfirmMarkUnpaid = { rec ->
            viewModel.markRecurringPaymentUnpaid(rec, monthKey)
            showPaidDetailsDialog = false
        },
        onSaveRecurring = { item ->
            viewModel.createOrEditRecurringPayment(item)
            showAddRecurringDialog = false
        },
        onDeleteRecurring = { rec ->
            viewModel.deleteRecurringPayment(rec)
            showAddRecurringDialog = false
        },
        localCurrency = localCurrency
    )

    // --- DIÁLOGOS DE CONFIRMACIÓN DE BORRADO SEGURO ---
    if (txToDelete != null) {
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CoralRed) },
            title = { Text("¿Eliminar registro?", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Text(
                    "Se eliminará \"${txToDelete?.description}\" por ${txToDelete?.originalAmount} ${txToDelete?.currency}. Esta acción no se puede deshacer.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        txToDelete?.let { viewModel.deleteTransaction(it) }
                        txToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CoralRed) },
            title = { Text("¿Eliminar categoría?", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Text(
                    "Se eliminará la categoría \"${categoryToDelete?.categoryName}\". Los gastos asociados conservarán su nombre.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        categoryToDelete?.let { viewModel.deleteCategory(it) }
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- VISOR DE RECIBO FULLSCREEN CON ZOOM ---
    if (photoUrlToShow != null) {
        ReceiptViewerDialog(
            photoUrl = photoUrlToShow!!,
            onDismiss = { photoUrlToShow = null }
        )
    }
}

// ============================================================================
// COMPONENTES VISUALES FINTECH
// ============================================================================

@Composable
private fun MonthlyIncomeAccountCard(
    monthlyIncomeAmount: Double,
    monthlyIncomeCurrency: String,
    totalMonthlyBudgetUsd: Double,
    localCurrency: String,
    bcvRate: Double,
    canEdit: Boolean,
    onEditClick: () -> Unit
) {
    val paper = FamiTheme.paper
    val effectiveCurr = monthlyIncomeCurrency.ifBlank { localCurrency }
    val symbol = CountryCurrencyConfig.getCurrencySymbol(effectiveCurr)
    val approxUsd = when {
        monthlyIncomeAmount <= 0.0 -> totalMonthlyBudgetUsd
        effectiveCurr.equals("USD", ignoreCase = true) -> monthlyIncomeAmount
        bcvRate > 0.0 && effectiveCurr.equals(localCurrency, ignoreCase = true) -> monthlyIncomeAmount / bcvRate
        else -> monthlyIncomeAmount
    }

    NotebookPaperSheet(
        modifier = Modifier.fillMaxWidth(),
        sheetColor = paper.stickyMint,
        borderColor = paper.border,
        shadowColor = paper.shadow3D,
        ruleLineColor = paper.ruleLine,
        marginLineColor = paper.marginLine,
        showRuledLines = false,
        showMarginLine = false,
        showSpiralTop = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "💵 INGRESO MENSUAL BASE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = FamiEmerald,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                val formattedAmount = if (monthlyIncomeAmount > 0.0) {
                    "$symbol ${String.format(Locale.US, "%,.2f", monthlyIncomeAmount)} $effectiveCurr"
                } else {
                    "$${String.format(Locale.US, "%,.2f", totalMonthlyBudgetUsd)} USD"
                }
                Text(
                    text = formattedAmount,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = paper.ink
                )
                if (monthlyIncomeAmount > 0.0 && !effectiveCurr.equals("USD", ignoreCase = true)) {
                    Text(
                        text = "≈ $${String.format(Locale.US, "%,.2f", approxUsd)} USD presupuesto mensual",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = paper.inkMuted
                    )
                } else {
                    Text(
                        text = "Presupuesto base del hogar para este mes",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = paper.inkMuted
                    )
                }
            }

            if (canEdit) {
                Tactile3DButton(
                    onClick = onEditClick,
                    faceColor = paper.sheet,
                    lipColor = paper.lipPaper,
                    borderColor = paper.border,
                    cornerRadius = 12.dp,
                    lipHeight = 3.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar Ingreso",
                        modifier = Modifier.size(14.dp),
                        tint = paper.ink
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Editar",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = paper.ink
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletsAccountsStripCard(
    wallets: List<Wallet>,
    canTransfer: Boolean,
    onTransferClick: () -> Unit
) {
    val paper = FamiTheme.paper
    NotebookPaperSheet(
        modifier = Modifier.fillMaxWidth(),
        sheetColor = paper.sheet,
        borderColor = paper.border,
        shadowColor = paper.shadow3D,
        ruleLineColor = paper.ruleLine,
        marginLineColor = paper.marginLine,
        showRuledLines = false,
        showMarginLine = false,
        showSpiralTop = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏦 MIS BILLETERAS (${wallets.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = paper.ink,
                    letterSpacing = 0.5.sp
                )
                if (canTransfer) {
                    Tactile3DButton(
                        onClick = onTransferClick,
                        faceColor = paper.stickyYellow,
                        lipColor = Lip3DGold,
                        borderColor = paper.border,
                        cornerRadius = 10.dp,
                        lipHeight = 2.5.dp,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "Transferir",
                            modifier = Modifier.size(14.dp),
                            tint = paper.ink
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Transferir",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink
                        )
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(wallets, key = { it.id.ifBlank { it.name } }) { wallet ->
                    val sym = CountryCurrencyConfig.getCurrencySymbol(wallet.currency)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = paper.sheetCream,
                        border = BorderStroke(1.2.dp, paper.border.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = wallet.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = paper.inkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$sym ${String.format(Locale.US, "%,.2f", wallet.balance)} ${wallet.currency}",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (wallet.balance >= 0) paper.ink else CoralRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MasterFinancialHeroCard(
    spentUsd: Double,
    incomeUsd: Double,
    monthlyBudgetUsd: Double,
    localCurrency: String,
    bcvRate: Double,
    onExportCsv: () -> Unit,
    onQuickAdd: () -> Unit
) {
    val paper = FamiTheme.paper
    val netBalance = incomeUsd - spentUsd
    val budgetRatio = if (monthlyBudgetUsd > 0) (spentUsd / monthlyBudgetUsd).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = budgetRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "budgetProgress"
    )
    val statusColor by animateColorAsState(
        targetValue = when {
            budgetRatio >= 1.0f -> CoralRed
            budgetRatio >= 0.8f -> WarningAmber
            else -> EmeraldGreen
        },
        label = "statusColor"
    )
    val localSymbol = CountryCurrencyConfig.getCurrencySymbol(localCurrency)

    NotebookPaperSheet(
        modifier = Modifier.fillMaxWidth(),
        sheetColor = paper.sheet,
        borderColor = paper.border,
        shadowColor = paper.shadow3D,
        ruleLineColor = paper.ruleLine,
        marginLineColor = paper.marginLine,
        showRuledLines = true,
        showMarginLine = true,
        showSpiralTop = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
        ) {
            // Fila Superior: Sello de Libro Mayor + Referencia de moneda interna (no editable)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = paper.stickyPeach,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.2.dp, paper.border)
                ) {
                    Text(
                        text = "🧮 LIBRO MAYOR FAMI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = paper.ink,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (!localCurrency.equals("USD", ignoreCase = true) && bcvRate > 0.0) {
                    Surface(
                        color = paper.stickyYellow,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, paper.border.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "1 USD ≈ ${String.format(Locale.US, "%.2f", bcvRate)} $localCurrency",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = paper.ink,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Monto Principal en Tinta + Sello Contable
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TOTAL EJECUTADO EN HOJA",
                        fontSize = 10.sp,
                        color = FamiCoralDeep,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", spentUsd)} USD",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Black,
                        color = paper.ink,
                        letterSpacing = (-0.5).sp
                    )
                    if (!localCurrency.equals("USD", ignoreCase = true)) {
                        Text(
                            text = "≈ $localSymbol ${String.format(Locale.US, "%,.2f", spentUsd * bcvRate)} $localCurrency",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = paper.inkMuted
                        )
                    }
                }

                val isPositive = netBalance >= 0
                Surface(
                    color = if (isPositive) paper.stickyMint else paper.alertBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, paper.border)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isPositive) "SUPERÁVIT" else "DÉFICIT",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isPositive) FamiEmerald else CoralRed
                        )
                        val sign = if (netBalance > 0) "+" else ""
                        Text(
                            text = "$sign$${String.format(Locale.US, "%,.2f", netBalance)}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (monthlyBudgetUsd > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val remaining = (monthlyBudgetUsd - spentUsd).coerceAtLeast(0.0)
                    Text(
                        text = if (spentUsd <= monthlyBudgetUsd)
                            "Disponible en hoja: $${String.format(Locale.US, "%,.0f", remaining)} de $${monthlyBudgetUsd.roundToInt()}"
                        else
                            "Excedido por $${String.format(Locale.US, "%,.2f", spentUsd - monthlyBudgetUsd)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = paper.ink
                    )
                    Text(
                        text = "${(budgetRatio * 100).roundToInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = paper.ink
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(paper.sheetCream)
                        .border(1.2.dp, paper.border, RoundedCornerShape(5.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .background(statusColor)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Fila Inferior: Sello de Ingresos + Botones 3D (Exportar Hoja & Anotar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = paper.stickyMint,
                    border = BorderStroke(1.5.dp, paper.border),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📈", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(5.dp))
                        Column {
                            Text("Ingresos", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = paper.inkMuted)
                            Text(
                                text = "+$${String.format(Locale.US, "%,.2f", incomeUsd)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = paper.ink
                            )
                        }
                    }
                }

                Tactile3DButton(
                    onClick = onExportCsv,
                    faceColor = paper.sheetCream,
                    lipColor = paper.lipPaper,
                    borderColor = paper.border,
                    cornerRadius = 12.dp,
                    lipHeight = 3.5.dp,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    Text("📊", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("HOJA CSV", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = paper.ink)
                }

                Tactile3DButton(
                    onClick = onQuickAdd,
                    faceColor = FamiCoral,
                    lipColor = Lip3DCoral,
                    borderColor = paper.border,
                    cornerRadius = 12.dp,
                    lipHeight = 4.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ANOTAR", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ModernTransactionCardItem(
    tx: Transaction,
    category: CategoryDetail?,
    walletName: String?,
    onClick: () -> Unit,
    onViewReceipt: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val displayTime = timeFormat.format(Date(if (tx.balanceDate > 0) tx.balanceDate else tx.date))
    val iconEmoji = when {
        tx.isTransfer -> "🔄"
        tx.isIncome -> "💵"
        !category?.iconEmoji.isNullOrBlank() -> category!!.iconEmoji
        else -> "💳"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Izquierda: Avatar con Emoji + Descripción y Metadatos
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            when {
                                tx.isTransfer -> FamiTheme.paper.stickyYellow
                                tx.isIncome -> EmeraldGreen.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = iconEmoji, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.description.ifBlank { if (tx.isTransfer) "Transferencia entre billeteras" else "Gasto sin concepto" },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = if (tx.isTransfer) "Transferencia" else tx.category.ifBlank { "General" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!walletName.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "🏦 $walletName",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (tx.items.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "🧾 ${tx.items.size}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text("•", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                        if (tx.registeredBy.isNotBlank()) {
                            Text(
                                text = tx.registeredBy,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("•", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Text(
                            text = displayTime,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Chip para ver factura si tiene foto adjunta
                    if (tx.receiptUrl.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            onClick = onViewReceipt,
                            shape = RoundedCornerShape(6.dp),
                            color = IndigoAccent.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = "Ver Recibo",
                                    tint = IndigoAccent,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ver factura",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoAccent
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Derecha: Monto en USD protagonista + Moneda original debajo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    val sign = when {
                        tx.isTransfer -> "⇄ "
                        tx.isIncome -> "+"
                        else -> "-"
                    }
                    val amountColor = when {
                        tx.isTransfer -> MaterialTheme.colorScheme.primary
                        tx.isIncome -> EmeraldGreen
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Text(
                        text = "$sign$${String.format(Locale.US, "%.2f", tx.amount)}",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = amountColor
                    )

                    if (!tx.currency.equals("USD", ignoreCase = true)) {
                        val origSym = CountryCurrencyConfig.getCurrencySymbol(tx.currency)
                        Text(
                            text = "${String.format(Locale.US, "%,.2f", tx.originalAmount)} $origSym (${tx.currency})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "USD",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                if (tx.isTransfer) {
                    IconButton(
                        onClick = onDeleteRequest,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar transferencia",
                            tint = CoralRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernCategoryBudgetCardItem(
    category: CategoryDetail,
    spentUsd: Double,
    limitUsd: Double,
    bcvRate: Double,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onFilterClick: () -> Unit
) {
    val ratio = if (limitUsd > 0) (spentUsd / limitUsd).toFloat() else 0f
    val animatedRatio by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "catProgress"
    )
    val barColor = when {
        ratio > 1.0f -> CoralRed
        ratio >= 0.8f -> WarningAmber
        else -> EmeraldGreen
    }
    val remainingUsd = limitUsd - spentUsd

    Surface(
        onClick = onFilterClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            if (ratio > 1f) CoralRed.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.iconEmoji.ifBlank { "📁" },
                            fontSize = 17.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = category.categoryName,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (remainingUsd >= 0)
                                "Quedan $${String.format(Locale.US, "%.2f", remainingUsd)} USD"
                            else
                                "Excedido por $${String.format(Locale.US, "%.2f", -remainingUsd)} USD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = barColor
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$${spentUsd.roundToInt()} / $${limitUsd.roundToInt()} USD",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (ratio > 1f) CoralRed else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(ratio * 100).roundToInt()}%",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = barColor
                        )
                    }
                    if (canEdit) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Editar Presupuesto",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

// ============================================================================
// MODAL BOTTOM SHEET: REGISTRO Y EDICIÓN DE TRANSACCIÓN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TransactionFormBottomSheet(
    initialTransaction: Transaction?,
    categories: List<CategoryDetail>,
    wallets: List<Wallet>,
    primaryCurrency: String,
    defaultBcvRate: Double,
    defaultDateMillis: Long,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSaveNew: (Double, String, String, String, Boolean, Long, Long, String) -> Unit,
    onSaveEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    onViewReceipt: (String) -> Unit
) {
    val isEditing = initialTransaction != null
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isIncome by remember { mutableStateOf(initialTransaction?.isIncome ?: false) }
    var currency by remember { mutableStateOf(initialTransaction?.currency ?: "USD") }
    var amountInput by remember { mutableStateOf(initialTransaction?.originalAmount?.toString() ?: "") }
    var descriptionInput by remember { mutableStateOf(initialTransaction?.description ?: "") }
    var selectedCategory by remember {
        mutableStateOf(
            initialTransaction?.category
                ?: categories.firstOrNull()?.categoryName
                ?: "General"
        )
    }
    var selectedWalletId by remember {
        mutableStateOf(
            initialTransaction?.walletId?.takeIf { it.isNotBlank() }
                ?: wallets.firstOrNull { it.currency.equals(initialTransaction?.currency ?: "USD", ignoreCase = true) }?.id
                ?: wallets.firstOrNull()?.id.orEmpty()
        )
    }
    var txDate by remember { mutableStateOf(initialTransaction?.date ?: System.currentTimeMillis()) }
    var balanceDate by remember {
        mutableStateOf(
            initialTransaction?.balanceDate?.takeIf { it > 0L } ?: txDate
        )
    }
    var activeBcvRate by remember {
        mutableStateOf(initialTransaction?.bcvRateUsed?.takeIf { it > 0.0 } ?: defaultBcvRate)
    }

    val currencyOptions = remember(primaryCurrency) {
        listOf("USD", primaryCurrency.uppercase(), "EUR")
            .filter { it.isNotBlank() && it != "NONE" }
            .distinct()
            .take(3)
    }

    LaunchedEffect(txDate, currency) {
        if (currency.equals("VES", ignoreCase = true)) {
            val fetched = BcvExchangeRateService.fetchRateForDate(txDate)
            if (fetched > 0.0) activeBcvRate = fetched
        } else if (!currency.equals("USD", ignoreCase = true)) {
            val fetched = BcvExchangeRateService.fetchUsdToCurrencyRate(currency)
            if (fetched > 0.0) activeBcvRate = fetched
        } else {
            activeBcvRate = 1.0
        }
    }

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cabecera + Botón Eliminar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Editar Movimiento" else "Nuevo Movimiento",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                if (isEditing && canDelete) {
                    IconButton(onClick = { onDelete(initialTransaction!!) }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar",
                            tint = CoralRed
                        )
                    }
                }
            }

            // Tipo: Gasto / Ingreso
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(3.dp)
            ) {
                listOf(false to "Gasto (-)", true to "Ingreso (+)").forEach { (inc, label) ->
                    val selected = isIncome == inc
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) (if (inc) EmeraldGreen else MaterialTheme.colorScheme.primary)
                                else Color.Transparent
                            )
                            .clickable { isIncome = inc }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Moneda Multi-divisa
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(3.dp)
            ) {
                currencyOptions.forEach { cur ->
                    val selected = currency.equals(cur, ignoreCase = true)
                    val sym = CountryCurrencyConfig.getCurrencySymbol(cur)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) IndigoAccent else Color.Transparent)
                            .clickable {
                                currency = cur
                                if (!isEditing) {
                                    wallets.firstOrNull { it.currency.equals(cur, ignoreCase = true) }
                                        ?.let { selectedWalletId = it.id }
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$sym $cur",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Input de Monto Protagonista + Conversión Interna en Vivo
            val currSym = CountryCurrencyConfig.getCurrencySymbol(currency)
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = { Text("Monto en $currency ($currSym)") },
                prefix = {
                    Text(
                        text = "$currSym ",
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (!currency.equals("USD", ignoreCase = true)) {
                val parsed = amountInput.toDoubleOrNull() ?: 0.0
                val usdEquiv = if (activeBcvRate > 0) parsed / activeBcvRate else 0.0
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IndigoAccent.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Equivalente en Libro (${String.format(Locale.US, "%.2f", activeBcvRate)} $currency/$):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "≈ $${String.format(Locale.US, "%.2f", usdEquiv)} USD",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = IndigoAccent
                        )
                    }
                }
            }

            // Concepto / Descripción
            OutlinedTextField(
                value = descriptionInput,
                onValueChange = { descriptionInput = it },
                label = { Text("Concepto o comercio (ej. Automercado, Farmacia)") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Selector de Billetera (Conecta el movimiento con el saldo real de la billetera)
            if (wallets.isNotEmpty()) {
                Text(
                    text = "Billetera / Cuenta asociada",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(wallets, key = { it.id.ifBlank { it.name } }) { w ->
                        val isSelected = selectedWalletId == w.id
                        val sym = CountryCurrencyConfig.getCurrencySymbol(w.currency)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedWalletId = w.id },
                            label = {
                                Text(
                                    text = "🏦 ${w.name} ($sym${String.format(Locale.US, "%.0f", w.balance)})",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Selectores de Fecha (Registro y Balance)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = txDate }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply { set(y, m, d) }
                                txDate = newCal.timeInMillis
                                if (!isEditing) balanceDate = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Fecha factura", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(sdf.format(Date(txDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = balanceDate }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply { set(y, m, d) }
                                balanceDate = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Mes contable", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(sdf.format(Date(balanceDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Selección de Categoría con Emojis
            if (!isIncome && categories.isNotEmpty()) {
                Text(
                    text = "Categoría",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory.equals(cat.categoryName, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat.categoryName },
                            leadingIcon = if (cat.iconEmoji.isNotBlank()) {
                                { Text(cat.iconEmoji, fontSize = 14.sp) }
                            } else null,
                            label = {
                                Text(
                                    text = cat.categoryName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Desglose de Productos Escaneados por IA (si existen)
            if (!initialTransaction?.items.isNullOrEmpty()) {
                val itemsList = initialTransaction!!.items
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🧾 Desglose del Recibo (${itemsList.size} ítems)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Extraído con IA",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        itemsList.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.category.isNotBlank()) {
                                        Text(
                                            text = item.category,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", item.amount)} USD",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }
                        }
                    }
                }
            }

            // Botón para ver factura adjunta si existe
            if (!initialTransaction?.receiptUrl.isNullOrEmpty()) {
                FilledTonalButton(
                    onClick = { onViewReceipt(initialTransaction!!.receiptUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver Foto del Recibo Original", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Botón Guardar Principal
            Button(
                onClick = {
                    val amountVal = amountInput.toDoubleOrNull()
                    if (amountVal != null && amountVal > 0.0 && descriptionInput.isNotBlank()) {
                        val finalCat = if (isIncome) "Ingreso" else selectedCategory
                        if (isEditing) {
                            val effectiveRate = if (!currency.equals("USD", ignoreCase = true)) {
                                if (activeBcvRate > 0.0) activeBcvRate else defaultBcvRate.coerceAtLeast(1.0)
                            } else 1.0
                            val updatedUsdAmount = if (currency.equals("USD", ignoreCase = true)) amountVal else amountVal / effectiveRate
                            onSaveEdit(
                                initialTransaction!!.copy(
                                    amount = updatedUsdAmount,
                                    originalAmount = amountVal,
                                    bcvRateUsed = effectiveRate,
                                    category = finalCat,
                                    description = descriptionInput.trim(),
                                    currency = currency,
                                    isIncome = isIncome,
                                    date = txDate,
                                    balanceDate = balanceDate,
                                    walletId = selectedWalletId
                                )
                            )
                        } else {
                            onSaveNew(
                                amountVal,
                                finalCat,
                                descriptionInput.trim(),
                                currency,
                                isIncome,
                                txDate,
                                balanceDate,
                                selectedWalletId
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isEditing) "Guardar Cambios" else "Confirmar Registro",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============================================================================
// MODAL BOTTOM SHEET: CREAR / EDITAR CATEGORÍA DE PRESUPUESTO
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFormBottomSheet(
    initialCategory: CategoryDetail?,
    monthKey: String,
    onDismiss: () -> Unit,
    onSave: (String?, String, Double, String) -> Unit,
    onDelete: (CategoryDetail) -> Unit
) {
    val isEditing = initialCategory != null
    val currentLimit = initialCategory?.limitsPerMonth?.get(monthKey) ?: initialCategory?.limitAmount ?: 200.0

    var nameInput by remember { mutableStateOf(initialCategory?.categoryName ?: "") }
    var limitInput by remember { mutableStateOf(if (isEditing) currentLimit.roundToInt().toString() else "") }
    var emojiInput by remember { mutableStateOf(initialCategory?.iconEmoji ?: "🛒") }

    val quickEmojis = listOf("🛒", "🏠", "🚗", "💊", "🍽️", "⚡", "🐾", "🎉", "📱", "✈️")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Editar Presupuesto" else "Nueva Categoría",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                if (isEditing) {
                    IconButton(onClick = { onDelete(initialCategory!!) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = CoralRed)
                    }
                }
            }

            // Selector rápido de Emojis
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickEmojis) { emoji ->
                    val selected = emojiInput == emoji
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { emojiInput = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 20.sp)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = emojiInput,
                    onValueChange = { emojiInput = it },
                    label = { Text("Icono") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.width(85.dp)
                )
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nombre de categoría") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = limitInput,
                onValueChange = { limitInput = it },
                label = { Text("Límite mensual en Dólares ($ USD)") },
                prefix = { Text("$ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val limitVal = limitInput.toDoubleOrNull() ?: 200.0
                    if (nameInput.isNotBlank()) {
                        onSave(initialCategory?.categoryName, nameInput.trim(), limitVal, emojiInput.trim())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isEditing) "Guardar Presupuesto" else "Crear Categoría",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ============================================================================
// COMPONENTES AUXILIARES
// ============================================================================

@Composable
private fun SegmentedTabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyStateCard(emoji: String, title: String, subtitle: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 38.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReceiptViewerDialog(photoUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        val bmp = remember(photoUrl) {
            try {
                val base64Str = if (photoUrl.contains("base64,")) {
                    photoUrl.substringAfter("base64,")
                } else {
                    photoUrl
                }
                val imageBytes = Base64.decode(base64Str, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) {
                null
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.96f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Barra superior limpia sin solaparse con la imagen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Comprobante / Recibo",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Pellizca para hacer zoom o arrastra",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (scale > 1.05f) {
                            FilledTonalButton(
                                onClick = {
                                    scale = 1f
                                    offset = Offset.Zero
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Restablecer 1x", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        FilledTonalIconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Recibo",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        val maxX = (size.width * (scale - 1f)) / 2f
                                        val maxY = (size.height * (scale - 1f)) / 2f
                                        offset = Offset(
                                            x = (offset.x + pan.x * scale).coerceIn(-maxX, maxX),
                                            y = (offset.y + pan.y * scale).coerceIn(-maxY, maxY)
                                        )
                                    }
                                }
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                        )
                    } else {
                        Text(
                            text = "No se pudo decodificar la imagen del recibo.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatDayHeader(timestampMillis: Long): String {
    val targetCal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
    val todayCal = Calendar.getInstance()
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        targetCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            targetCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) -> "Hoy"
        targetCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
            targetCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR) -> "Ayer"
        else -> SimpleDateFormat("EEEE, d MMMM", Locale("es", "ES"))
            .format(Date(timestampMillis))
            .replaceFirstChar { it.uppercase() }
    }
}

@Composable
private fun MonthlyIncomeEditDialog(
    initialAmount: Double,
    initialCurrency: String,
    localCurrency: String,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    val paper = FamiTheme.paper
    var amountInput by remember { mutableStateOf(if (initialAmount > 0.0) initialAmount.toString() else "") }
    var selectedCurrency by remember { mutableStateOf(initialCurrency.ifBlank { "USD" }) }
    val currencyOptions = remember(localCurrency) {
        listOf("USD", localCurrency.uppercase(), "EUR")
            .filter { it.isNotBlank() && it != "NONE" }
            .distinct()
            .take(3)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = paper.sheet,
        title = {
            Text(
                text = "💵 Ingreso Mensual Base",
                fontWeight = FontWeight.Black,
                color = paper.ink
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Define tu ingreso mensual para calcular automáticamente el presupuesto disponible de tu hoja.",
                    fontSize = 12.sp,
                    color = paper.inkMuted
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(paper.sheetCream)
                        .padding(3.dp)
                ) {
                    currencyOptions.forEach { cur ->
                        val selected = selectedCurrency.equals(cur, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) FamiCoral else Color.Transparent)
                                .clickable { selectedCurrency = cur }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cur,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else paper.inkMuted
                            )
                        }
                    }
                }

                val sym = CountryCurrencyConfig.getCurrencySymbol(selectedCurrency)
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Monto mensual ($selectedCurrency)") },
                    prefix = { Text("$sym ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountInput.toDoubleOrNull()
                    if (amt != null && amt > 0.0) {
                        onSave(amt, selectedCurrency)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FamiCoral)
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = paper.inkMuted)
            }
        }
    )
}

@Composable
fun WalletTransferDialog(
    wallets: List<Wallet>,
    bcvRate: Double,
    localCurrency: String,
    onDismiss: () -> Unit,
    onConfirmTransfer: (Wallet, Wallet, Double, Double, String) -> Unit
) {
    val paper = FamiTheme.paper
    var fromWallet by remember(wallets) { mutableStateOf(wallets.firstOrNull()) }
    var toWallet by remember(wallets) {
        mutableStateOf(wallets.firstOrNull { it.id != wallets.firstOrNull()?.id } ?: wallets.lastOrNull())
    }
    var fromAmountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    val fromCurr = fromWallet?.currency ?: "USD"
    val toCurr = toWallet?.currency ?: "USD"
    val fromAmountVal = fromAmountText.toDoubleOrNull() ?: 0.0

    var liveConvertedAmount by remember { mutableStateOf(0.0) }
    LaunchedEffect(fromAmountVal, fromCurr, toCurr, bcvRate) {
        if (fromAmountVal <= 0.0) {
            liveConvertedAmount = 0.0
        } else if (fromCurr.equals(toCurr, ignoreCase = true)) {
            liveConvertedAmount = fromAmountVal
        } else {
            val fromRateToUsd = when {
                fromCurr.equals("USD", ignoreCase = true) -> 1.0
                fromCurr.equals(localCurrency, ignoreCase = true) && bcvRate > 0.0 -> bcvRate
                else -> BcvExchangeRateService.fetchUsdToCurrencyRate(fromCurr).takeIf { it > 0.0 } ?: bcvRate.coerceAtLeast(1.0)
            }
            val toRateToUsd = when {
                toCurr.equals("USD", ignoreCase = true) -> 1.0
                toCurr.equals(localCurrency, ignoreCase = true) && bcvRate > 0.0 -> bcvRate
                else -> BcvExchangeRateService.fetchUsdToCurrencyRate(toCurr).takeIf { it > 0.0 } ?: bcvRate.coerceAtLeast(1.0)
            }
            val usdEquiv = if (fromCurr.equals("USD", ignoreCase = true)) fromAmountVal else fromAmountVal / fromRateToUsd
            liveConvertedAmount = if (toCurr.equals("USD", ignoreCase = true)) usdEquiv else usdEquiv * toRateToUsd
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = paper.sheet,
        title = {
            Text(
                text = "🔄 Transferir entre Billeteras",
                fontWeight = FontWeight.Black,
                color = paper.ink
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Desde:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = paper.inkMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(wallets, key = { "from_${it.id.ifBlank { it.name }}" }) { w ->
                        val isSelected = fromWallet?.id == w.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                fromWallet = w
                                if (toWallet?.id == w.id) {
                                    toWallet = wallets.firstOrNull { it.id != w.id }
                                }
                            },
                            label = { Text("${w.name} (${w.currency})", fontSize = 11.5.sp) }
                        )
                    }
                }

                Text("Hacia:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = paper.inkMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(wallets.filter { it.id != fromWallet?.id }, key = { "to_${it.id.ifBlank { it.name }}" }) { w ->
                        val isSelected = toWallet?.id == w.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { toWallet = w },
                            label = { Text("${w.name} (${w.currency})", fontSize = 11.5.sp) }
                        )
                    }
                }

                val fromSym = CountryCurrencyConfig.getCurrencySymbol(fromCurr)
                OutlinedTextField(
                    value = fromAmountText,
                    onValueChange = { fromAmountText = it },
                    label = { Text("Monto a transferir ($fromCurr)") },
                    prefix = { Text("$fromSym ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (fromAmountVal > 0.0 && toWallet != null && !fromCurr.equals(toCurr, ignoreCase = true)) {
                    val toSym = CountryCurrencyConfig.getCurrencySymbol(toCurr)
                    Surface(
                        color = paper.stickyMint,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, paper.border.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Recibe aprox.: $toSym ${String.format(Locale.US, "%,.2f", liveConvertedAmount)} $toCurr",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FamiEmerald,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Nota opcional (ej. Cambio de divisas)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = fromWallet != null && toWallet != null && fromWallet?.id != toWallet?.id && fromAmountVal > 0.0,
                onClick = {
                    val fw = fromWallet ?: return@Button
                    val tw = toWallet ?: return@Button
                    val finalTo = if (liveConvertedAmount > 0.0) liveConvertedAmount else fromAmountVal
                    onConfirmTransfer(fw, tw, fromAmountVal, finalTo, noteText.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = FamiCoral)
            ) {
                Text("Transferir", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = paper.inkMuted)
            }
        }
    )
}

