package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.api.ParsedReceiptItem
import com.example.currency.CountryCurrencyConfig
import com.example.database.CategoryDetail
import com.example.database.Wallet
import com.example.ui.theme.FamiCoral
import com.example.ui.theme.FamiEmerald
import com.example.ui.theme.FamiTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptScanConfirmDialog(
    scannedDesc: String,
    scannedAmount: String,
    scannedCurrency: String,
    scannedCategory: String,
    scannedDate: Long,
    scannedBalanceDate: Long,
    scannedItems: List<ParsedReceiptItem>,
    saveReceiptPhoto: Boolean,
    primaryCurrency: String,
    categories: List<CategoryDetail>,
    wallets: List<Wallet>,
    onSetScannedDesc: (String) -> Unit,
    onSetScannedAmount: (String) -> Unit,
    onSetScannedCurrency: (String) -> Unit,
    onSetScannedCategory: (String) -> Unit,
    onSetScannedDate: (Long) -> Unit,
    onSetScannedBalanceDate: (Long) -> Unit,
    onSetSaveReceiptPhoto: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (selectedWalletId: String) -> Unit
) {
    val context = LocalContext.current
    val paper = FamiTheme.paper
    val currencyOptions = remember(primaryCurrency) {
        listOf("USD", primaryCurrency.uppercase(), "EUR")
            .filter { it.isNotBlank() && it != "NONE" }
            .distinct()
            .take(3)
    }

    var selectedWalletId by remember(wallets, scannedCurrency) {
        mutableStateOf(
            wallets.firstOrNull { it.currency.equals(scannedCurrency, ignoreCase = true) }?.id
                ?: wallets.firstOrNull()?.id.orEmpty()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = paper.sheet,
            tonalElevation = 8.dp,
            border = BorderStroke(1.6.dp, paper.border)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxSize()
            ) {
                // --- FIXED HEADER ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(FamiCoral, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Confirmar IA",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Confirmar Recibo ✨",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = paper.ink
                            )
                            Text(
                                text = "Extracción Inteligente por IA",
                                fontSize = 10.sp,
                                color = paper.inkMuted
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = paper.inkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = paper.ruleLine,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // --- SCROLLABLE BODY ---
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        value = scannedDesc,
                        onValueChange = onSetScannedDesc,
                        label = { Text("Comercio / Detalle", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = scannedAmount,
                            onValueChange = onSetScannedAmount,
                            label = { Text("Monto Total", fontSize = 11.sp) },
                            modifier = Modifier.weight(1.2f),
                            textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FamiEmerald),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Moneda:", fontSize = 10.sp, color = paper.inkMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                currencyOptions.forEach { m ->
                                    val isSelected = scannedCurrency.equals(m, ignoreCase = true)
                                    val sym = CountryCurrencyConfig.getCurrencySymbol(m)
                                    Surface(
                                        onClick = {
                                            onSetScannedCurrency(m)
                                            wallets.firstOrNull { it.currency.equals(m, ignoreCase = true) }
                                                ?.let { selectedWalletId = it.id }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) paper.stickyYellow else paper.sheetCream,
                                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) paper.border else paper.border.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = sym.ifBlank { m },
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                            color = paper.ink,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (wallets.isNotEmpty()) {
                        Text(
                            "Billetera a descontar:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FamiCoral
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(wallets, key = { it.id.ifBlank { it.name } }) { w ->
                                val isSelected = selectedWalletId == w.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedWalletId = w.id },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = paper.sheetCream,
                                        selectedContainerColor = paper.stickyYellow,
                                        labelColor = paper.ink,
                                        selectedLabelColor = paper.ink
                                    ),
                                    label = {
                                        Text("🏦 ${w.name} (${w.currency})", fontSize = 10.5.sp)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    Text("Fechas del Registro:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FamiCoral)
                    val sdfDate = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val cal = Calendar.getInstance().apply { timeInMillis = scannedDate }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val newCal = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, year)
                                                set(Calendar.MONTH, month)
                                                set(Calendar.DAY_OF_MONTH, day)
                                            }
                                            onSetScannedDate(newCal.timeInMillis)
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                            color = paper.sheetCream,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, paper.border.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("📄 Fecha Factura", fontSize = 10.sp, color = paper.inkMuted, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(sdfDate.format(Date(scannedDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = paper.ink)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val cal = Calendar.getInstance().apply { timeInMillis = scannedBalanceDate }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val newCal = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, year)
                                                set(Calendar.MONTH, month)
                                                set(Calendar.DAY_OF_MONTH, day)
                                            }
                                            onSetScannedBalanceDate(newCal.timeInMillis)
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                            color = paper.stickyYellow,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, paper.border.copy(alpha = 0.7f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("⚖️ Fecha Balance", fontSize = 10.sp, color = FamiCoral, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(sdfDate.format(Date(scannedBalanceDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = paper.ink)
                            }
                        }
                    }

                    Text("Categoría Principal:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = paper.inkMuted)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = scannedCategory == cat.categoryName
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSetScannedCategory(cat.categoryName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = paper.sheetCream,
                                    selectedContainerColor = paper.stickyYellow,
                                    labelColor = paper.ink,
                                    selectedLabelColor = paper.ink
                                ),
                                label = { Text(cat.categoryName, fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    if (scannedItems.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "🛒 Desglose de Productos (${scannedItems.size} ítems)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FamiCoral
                                )
                                Text(
                                    text = "IVA Incluido",
                                    fontSize = 9.sp,
                                    color = paper.inkMuted,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            Surface(
                                color = paper.sheetCream,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, paper.border.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    scannedItems.forEach { item ->
                                        Surface(
                                            color = paper.sheet,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, paper.border.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.name,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = paper.ink
                                                    )
                                                    if (item.category.isNotBlank()) {
                                                        Surface(
                                                            color = paper.stickyYellow,
                                                            shape = RoundedCornerShape(4.dp),
                                                            modifier = Modifier.padding(top = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = item.category,
                                                                fontSize = 9.sp,
                                                                color = paper.ink,
                                                                fontWeight = FontWeight.SemiBold,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "${item.amount} $scannedCurrency",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = FamiEmerald
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Switch(
                            checked = saveReceiptPhoto,
                            onCheckedChange = onSetSaveReceiptPhoto,
                            colors = SwitchDefaults.colors(checkedTrackColor = FamiCoral)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar foto como respaldo", fontSize = 12.sp, color = paper.ink)
                    }
                }

                HorizontalDivider(
                    color = paper.ruleLine,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Descartar", color = paper.inkMuted, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { onConfirm(selectedWalletId) },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = FamiCoral),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Registrar Gasto", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
