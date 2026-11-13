package com.example.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.api.CategorySpendBreakdown
import com.example.api.FinanceAiReport
import com.example.ui.theme.*
import java.util.Locale

// --- BANNER DESTACADO DE INTELIGENCIA ARTIFICIAL FINANCIERA FAMI ---
@Composable
fun FinanceAiHeroCard(
    report: FinanceAiReport?,
    isLoading: Boolean,
    onOpenAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    StickyNoteSheet(
        modifier = modifier.fillMaxWidth(),
        noteColor = PaperStickyYellow
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = PaperStickyPeach,
                    border = BorderStroke(1.2.dp, FamiMidnight),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✏️", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "NOTA DEL ASESOR FAMI IA",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = FamiMidnight
                        )
                    }
                }

                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = FamiCoral,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Calculando...",
                            fontSize = 10.sp,
                            color = FamiMidnight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (report != null) {
                    val healthBg = when (report.budgetHealth) {
                        "Saludable" -> PaperStickyMint
                        "Precaución" -> PaperStickyYellow
                        else -> FamiAlertRedBg
                    }
                    Surface(
                        color = healthBg,
                        border = BorderStroke(1.2.dp, FamiMidnight),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "● ${report.budgetHealth.uppercase()}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = FamiMidnight,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fami_sticker_idea),
                    contentDescription = "Fami Mentor IA",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, FamiMidnight, RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (report != null && report.topSpendingCategory != "Sin gastos") {
                        Text(
                            text = "Foco del mes: ${report.topSpendingCategory}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = FamiMidnight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Representa el ${report.topSpendingPercentage}% ($${String.format(Locale.US, "%,.2f", report.topSpendingAmountUsd)} USD).",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FamiMidnight.copy(alpha = 0.78f)
                        )
                    } else {
                        Text(
                            text = "¡Revisemos las cuentas del cuaderno!",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Black,
                            color = FamiMidnight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Fami IA analiza cada hoja de gastos y te sugiere cómo ahorrar en familia.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FamiMidnight.copy(alpha = 0.78f)
                        )
                    }
                }
            }

            if (report != null && report.savingsTips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = PaperSheetWhite,
                    border = BorderStroke(1.2.dp, FamiMidnight),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💡", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = report.savingsTips.first(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FamiMidnight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Tactile3DButton(
                onClick = onOpenAnalysis,
                modifier = Modifier.fillMaxWidth(),
                faceColor = FamiCoral,
                lipColor = Lip3DCoral,
                cornerRadius = 12.dp,
                lipHeight = 3.5.dp,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    text = if (report != null) "ABRIR DIAGNÓSTICO DEL CUADERNO" else "ANALIZAR HOJA CON FAMI IA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

// --- PANEL MODAL MATERIAL 3 DE AUDITORÍA FINANCIERA FAMI ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceAiBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    isLoading: Boolean,
    report: FinanceAiReport?,
    errorMessage: String?,
    onRefresh: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Top Bar with Mascot and Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(FamiCoral, FamiCoralLight)
                                )
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_fami_mascot),
                            contentDescription = "Fami IA",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Diagnóstico Fami IA",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Auditoría inteligente de tus finanzas familiares",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.fami_sticker_idea),
                            contentDescription = null,
                            modifier = Modifier.size(90.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(38.dp),
                            color = FamiCoral,
                            strokeWidth = 3.5.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Fami está auditando tus movimientos...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Calculando categorías dominantes, gastos hormiga y oportunidades de ahorro real.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage,
                            fontSize = 13.sp,
                            color = FamiAlertRed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = FamiCoral),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reintentar Análisis", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (report != null) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. HERO CARD: DÓNDE SE GASTA MÁS
                    item {
                        ElevatedCard(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = FamiMidnight
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(FamiCoralDeep, FamiMidnight)
                                        )
                                    )
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "FOCO PRINCIPAL DE GASTO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White.copy(alpha = 0.85f),
                                        letterSpacing = 1.sp
                                    )

                                    val healthColor = when (report.budgetHealth) {
                                        "Saludable" -> FamiEmerald
                                        "Precaución" -> FamiGold
                                        else -> FamiAlertRed
                                    }
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.30f),
                                        border = BorderStroke(1.dp, healthColor.copy(alpha = 0.7f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "SALUD: ${report.budgetHealth.uppercase()}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = healthColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = report.topSpendingCategory,
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "$${String.format(Locale.US, "%,.2f", report.topSpendingAmountUsd)} USD (${report.topSpendingPercentage}% del gasto mensual)",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                val progressFraction = (report.topSpendingPercentage / 100f).coerceIn(0.05f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progressFraction)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(FamiGold, FamiEmerald)
                                                )
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.28f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("CATEGORÍAS", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                                        Text("${report.categoryBreakdown.size} activas", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("ALERTAS", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                                        Text("${report.moneyLeaks.size} detectadas", fontSize = 12.sp, color = FamiGold, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("ACCIONES", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                                        Text("${report.savingsTips.size} consejos", fontSize = 12.sp, color = FamiEmerald, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }

                    // 2. RESUMEN EJECUTIVO
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📋", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Diagnóstico Global de Fami",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = report.summary,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // 3. DESGLOSE DE CATEGORÍAS
                    if (report.categoryBreakdown.isNotEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📊", fontSize = 15.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Distribución por Categorías",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    report.categoryBreakdown.forEach { item ->
                                        Column(modifier = Modifier.padding(vertical = 5.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                                Text(
                                                    text = "$${String.format(Locale.US, "%,.2f", item.amountUsd)} (${item.percentage}%)",
                                                    fontSize = 11.5.sp,
                                                    color = FamiCoral,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val frac = (item.percentage / 100f).coerceIn(0.02f, 1f)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .fillMaxWidth(frac)
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(
                                                            Brush.horizontalGradient(
                                                                listOf(FamiCoral, FamiGold)
                                                            )
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. FUGAS DE DINERO
                    if (report.moneyLeaks.isNotEmpty()) {
                        item {
                            Surface(
                                color = FamiAlertRed.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, FamiAlertRed.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⚠️", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Fugas de Dinero / Alertas",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FamiAlertRed
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    report.moneyLeaks.forEach { leak ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("🔍", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(leak, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 17.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. CONSEJOS DE AHORRO ACCIONABLES
                    if (report.savingsTips.isNotEmpty()) {
                        item {
                            Surface(
                                color = FamiEmerald.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, FamiEmerald.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("💡", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Plan de Ahorro Familiar Fami",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FamiEmerald
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    report.savingsTips.forEachIndexed { idx, tip ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = FamiEmerald.copy(alpha = 0.2f),
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("${idx + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FamiEmerald)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(tip, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 17.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRefresh,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Actualizar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val shareText = buildString {
                                append("🤖 *Diagnóstico Financiero Fami (somosfami.app)*\n")
                                append("📊 Salud Presupuestaria: *${report.budgetHealth}*\n")
                                append("🏆 Mayor gasto: *${report.topSpendingCategory}* ($${String.format(Locale.US, "%.2f", report.topSpendingAmountUsd)} USD - ${report.topSpendingPercentage}%)\n\n")
                                append("📋 *Resumen:* ${report.summary}\n\n")
                                if (report.savingsTips.isNotEmpty()) {
                                    append("💡 *Consejos de ahorro:*\n")
                                    report.savingsTips.forEach { append("• $it\n") }
                                }
                            }
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Compartir Diagnóstico Fami IA"))
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compartir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FamiCoral),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Listo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
