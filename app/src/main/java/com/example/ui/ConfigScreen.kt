package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.ActivityLog
import com.example.database.AppSetting
import com.example.database.UserProfile
import com.example.ui.theme.*
import com.example.viewmodel.FamilyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun parseSafeProfileColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF6750A4)
    }
}

@Composable
fun NavigationSidebar(
    categories: List<String>,
    selectedCategory: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEachIndexed { index, name ->
            val isSelected = selectedCategory == index
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HorizontalCategoryTabs(
    categories: List<String>,
    selectedCategory: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        categories.forEachIndexed { index, name ->
            val isSelected = selectedCategory == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surface
                        else Color.Transparent
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ConfigMainContent(
    selectedCategory: Int,
    profiles: List<UserProfile>,
    settings: List<AppSetting>,
    activityLogs: List<ActivityLog>,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onAddProfileClick: () -> Unit,
    onEditProfileClick: (UserProfile) -> Unit,
    onDeleteProfileClick: (UserProfile) -> Unit,
    viewModel: FamilyViewModel,
    modifier: Modifier = Modifier,
    onOpenGuide: () -> Unit = {}
) {
    val paper = FamiTheme.paper
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteAccountError by remember { mutableStateOf<String?>(null) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var profileToDelete by remember { mutableStateOf<UserProfile?>(null) }

    val currentAvatarId by viewModel.userAvatarId.collectAsStateWithLifecycle()
    val unlockMethod by viewModel.unlockMethod.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAdminRole by viewModel.isAdminRole.collectAsStateWithLifecycle()
    val savedEmail by viewModel.savedEmail.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectedCategory == 0) {
            // --- MI AVATAR FAMI (12 AVATARES) ---
            item {
                NotebookPaperSheet(
                    modifier = Modifier.fillMaxWidth(),
                    sheetColor = paper.sheet,
                    borderColor = paper.border,
                    shadowColor = paper.shadow3D,
                    ruleColor = paper.ruleLine.copy(alpha = 0.58f),
                    marginColor = paper.marginLine.copy(alpha = 0.42f),
                    showRuledLines = false,
                    showMarginLine = false
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🧑 MI AVATAR DE CUENTA FAMI",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink
                        )
                        Text(
                            text = "Elige tu personaje (mujer, hombre, niño o niña):",
                            fontSize = 11.sp,
                            color = paper.inkMuted
                        )
                        AvatarCatalog.ALL.chunked(4).forEach { rowAvatars ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowAvatars.forEach { av ->
                                    val isChosen = av.id == currentAvatarId
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isChosen) paper.stickyYellow else paper.sheetCream)
                                            .border(
                                                width = if (isChosen) 2.2.dp else 1.2.dp,
                                                color = if (isChosen) FamiCoral else paper.border.copy(alpha = 0.45f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.updateUserAvatar(av.id) }
                                            .padding(vertical = 8.dp, horizontal = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = av.drawableRes),
                                            contentDescription = av.labelEs,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .border(1.2.dp, paper.border, CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- MÉTODO DE DESBLOQUEO (HUELLA DIGITAL vs PIN) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Método de Acceso",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Elige si deseas entrar a Fami con huella o PIN de 5 dígitos",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isBiometric = unlockMethod == "biometric"
                            val isPin = unlockMethod == "pin" || unlockMethod == "unset"

                            Surface(
                                onClick = { viewModel.updateUnlockPreference("biometric") },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isBiometric) paper.stickyMint else paper.sheetCream,
                                border = BorderStroke(
                                    if (isBiometric) 2.dp else 1.dp,
                                    if (isBiometric) FamiEmerald else paper.border.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = if (isBiometric) FamiEmerald else paper.inkMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Huella",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = paper.ink
                                    )
                                }
                            }

                            Surface(
                                onClick = { viewModel.updateUnlockPreference("pin") },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPin) paper.stickyYellow else paper.sheetCream,
                                border = BorderStroke(
                                    if (isPin) 2.dp else 1.dp,
                                    if (isPin) FamiCoral else paper.border.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Pin,
                                        contentDescription = null,
                                        tint = if (isPin) FamiCoral else paper.inkMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PIN (5 díg.)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = paper.ink
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                newPinInput = ""
                                showChangePinDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cambiar PIN de 5 dígitos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- GUÍA INTERACTIVA DE OPCIONES ---
            item {
                Tactile3DButton(
                    onClick = onOpenGuide,
                    modifier = Modifier.fillMaxWidth(),
                    faceColor = paper.stickyMint,
                    lipColor = Lip3DMint,
                    borderColor = paper.border,
                    cornerRadius = 14.dp,
                    lipHeight = 4.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = paper.ink, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ver Guía Interactiva de Opciones",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = paper.ink
                    )
                }
            }

            // --- THEME SWAP SECTION ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Theme state",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Aspecto Visual del Sistema",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Alterna entre cuaderno claro y papel grafito nocturno",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleTheme(it) }
                        )
                    }
                }
            }

            // --- FAMILY CODE, SESSION & LEGAL ---
            item {
                val familyId by viewModel.familyId.collectAsStateWithLifecycle()
                val context = LocalContext.current

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Código Familiar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Comparte este código para que otros miembros se unan al hogar Fami.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (savedEmail.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Cuenta conectada: $savedEmail",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = familyId ?: "CARGANDO...",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            Button(
                                onClick = {
                                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Código Familiar", familyId ?: "")
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://somosfami.app/privacy.html"))
                                    context.startActivity(browserIntent)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Privacidad", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://somosfami.app/terms.html"))
                                    context.startActivity(browserIntent)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Términos", fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = "Cerrar Sesión", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cerrar Sesión", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDeleteAccountDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Eliminar Cuenta", modifier = Modifier.size(14.dp), tint = AlertRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Eliminar mi Cuenta y Datos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                        }
                    }
                }
            }
        } else if (selectedCategory == 1) {
            // --- MIEMBROS DEL HOGAR (Con control RBAC) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Miembros del Hogar",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isAdminRole) {
                                Button(
                                    onClick = onAddProfileClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Nuevo perfil", modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Añadir", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = if (isAdminRole) {
                                "Perfiles registrados en el hogar para la asignación de responsabilidades."
                            } else {
                                "Solo los administradores del hogar pueden añadir o modificar perfiles."
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (profiles.isEmpty()) {
                            Text("No hay perfiles configurados.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            profiles.forEach { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(parseSafeProfileColor(p.colorHex)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(p.name.take(1).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            if (p.role.isNotBlank()) {
                                                Text(p.role, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }

                                    if (isAdminRole) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onEditProfileClick(p) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit profile",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            if (!p.name.equals(currentUser, ignoreCase = true) && profiles.size > 1) {
                                                IconButton(
                                                    onClick = { profileToDelete = p },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Eliminar miembro",
                                                        tint = AlertRed.copy(alpha = 0.85f),
                                                        modifier = Modifier.size(16.dp)
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
        } else {
            // --- HISTORY LOGS ---
            item {
                Text(
                    "Historial de Actividades Recientes",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (activityLogs.isEmpty()) {
                item {
                    Text("No hay logs de actividad registrados.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(activityLogs.take(30)) { log ->
                    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val dateStr = format.format(Date(log.timestamp))

                    val tagColor = when (log.tag.uppercase()) {
                        "FINANZAS" -> MaterialTheme.colorScheme.primary
                        "TAREAS" -> Color(0xFFE65100)
                        "COMPRAS" -> Color(0xFF0D9488)
                        "ALERTA" -> AlertRed
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(tagColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = log.tag,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tagColor
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = log.message,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dateStr,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (profileToDelete != null) {
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            containerColor = paper.sheet,
            title = { Text("¿Eliminar miembro?", fontWeight = FontWeight.Bold, color = paper.ink) },
            text = {
                Text(
                    "Se eliminará a ${profileToDelete?.name} de la lista del hogar.",
                    fontSize = 13.sp,
                    color = paper.inkMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        profileToDelete?.let(onDeleteProfileClick)
                        profileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("Cancelar", color = paper.inkMuted)
                }
            }
        )
    }

    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            containerColor = paper.sheet,
            title = { Text("🔐 Nuevo PIN de Acceso", fontWeight = FontWeight.Black, color = paper.ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ingresa un nuevo PIN numérico de 5 dígitos para desbloquear Fami:",
                        fontSize = 12.sp,
                        color = paper.inkMuted
                    )
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 5 && it.all { ch -> ch.isDigit() }) newPinInput = it },
                        label = { Text("PIN de 5 dígitos") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = newPinInput.length == 5,
                    onClick = {
                        viewModel.updateUserPin(newPinInput)
                        showChangePinDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FamiCoral)
                ) {
                    Text("Guardar PIN", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancelar", color = paper.inkMuted)
                }
            }
        )
    }

    val dialogContext = LocalContext.current
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteAccountDialog = false
                deleteAccountError = null
            },
            containerColor = paper.sheet,
            title = { Text("¿Eliminar tu Cuenta?", color = AlertRed, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Esta acción es irreversible y permanente conforme a las políticas de Google Play. Se eliminarán tus credenciales de acceso, tus comprobantes en la nube y tus preferencias.",
                        color = paper.ink
                    )
                    deleteAccountError?.let { err: String ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(err, color = AlertRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUserAccount(
                            onSuccess = {
                                showDeleteAccountDialog = false
                                Toast.makeText(dialogContext, "Cuenta eliminada correctamente", Toast.LENGTH_LONG).show()
                            },
                            onError = { err: String ->
                                deleteAccountError = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Sí, Eliminar Cuenta")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteAccountDialog = false
                    deleteAccountError = null
                }) {
                    Text("Cancelar", color = paper.inkMuted)
                }
            }
        )
    }
}

@Composable
fun ConfigTab(viewModel: FamilyViewModel, onOpenGuide: () -> Unit = {}) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle()

    var showProfileCreatorDialog by remember { mutableStateOf(false) }
    var isEditingProfile by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<UserProfile?>(null) }
    var profileNameInput by remember { mutableStateOf("") }
    var profileColorHexSelected by remember { mutableStateOf("#D0BCFF") }

    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    val profileAvatarColors = listOf("#D0BCFF", "#EFB8C8", "#81C784", "#64B5F6", "#FFD54F", "#BA68C8", "#E0E0E0")
    var selectedCategory by remember { mutableStateOf(0) }
    val categoriesList = listOf("General", "Miembros")

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 600.dp

        if (isWideScreen) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NavigationSidebar(
                    categories = categoriesList,
                    selectedCategory = selectedCategory,
                    onSelect = { selectedCategory = it },
                    modifier = Modifier.width(180.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    ConfigMainContent(
                        selectedCategory = selectedCategory,
                        profiles = profiles,
                        settings = settings,
                        activityLogs = activityLogs,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { viewModel.toggleDarkTheme(it) },
                        onAddProfileClick = { showProfileCreatorDialog = true },
                        onEditProfileClick = { p ->
                            isEditingProfile = true
                            editingProfile = p
                            profileNameInput = p.name
                            profileColorHexSelected = p.colorHex
                            showProfileCreatorDialog = true
                        },
                        onDeleteProfileClick = { p -> viewModel.deleteProfile(p) },
                        viewModel = viewModel,
                        onOpenGuide = onOpenGuide
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Ajustes del Hogar",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                HorizontalCategoryTabs(
                    categories = categoriesList,
                    selectedCategory = selectedCategory,
                    onSelect = { selectedCategory = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    ConfigMainContent(
                        selectedCategory = selectedCategory,
                        profiles = profiles,
                        settings = settings,
                        activityLogs = activityLogs,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { viewModel.toggleDarkTheme(it) },
                        onAddProfileClick = { showProfileCreatorDialog = true },
                        onEditProfileClick = { p ->
                            isEditingProfile = true
                            editingProfile = p
                            profileNameInput = p.name
                            profileColorHexSelected = p.colorHex
                            showProfileCreatorDialog = true
                        },
                        onDeleteProfileClick = { p -> viewModel.deleteProfile(p) },
                        viewModel = viewModel,
                        onOpenGuide = onOpenGuide
                    )
                }
            }
        }
    }

    if (showProfileCreatorDialog) {
        AlertDialog(
            onDismissRequest = {
                showProfileCreatorDialog = false
                isEditingProfile = false
                editingProfile = null
                profileNameInput = ""
                profileColorHexSelected = "#D0BCFF"
            },
            title = {
                Text(
                    if (isEditingProfile) "Editar Miembro del Hogar" else "Registrar Miembro del Hogar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        value = profileNameInput,
                        onValueChange = { profileNameInput = it },
                        label = { Text("Nombre del Perfil") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(
                        "Color de avatar representativo:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        profileAvatarColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(parseSafeProfileColor(hex))
                                    .clickable { profileColorHexSelected = hex }
                                    .border(
                                        width = if (profileColorHexSelected == hex) 2.dp else 0.dp,
                                        color = if (profileColorHexSelected == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (profileNameInput.isNotBlank()) {
                        if (isEditingProfile && editingProfile != null) {
                            viewModel.editProfile(editingProfile!!, profileNameInput, profileColorHexSelected)
                        } else {
                            viewModel.createProfile(profileNameInput, profileColorHexSelected)
                        }
                        profileNameInput = ""
                        showProfileCreatorDialog = false
                        isEditingProfile = false
                        editingProfile = null
                    }
                }) {
                    Text(
                        if (isEditingProfile) "Guardar" else "Registrar",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showProfileCreatorDialog = false
                    isEditingProfile = false
                    editingProfile = null
                    profileNameInput = ""
                    profileColorHexSelected = "#D0BCFF"
                }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun ConfigDialog(
    viewModel: FamilyViewModel,
    onDismiss: () -> Unit,
    onOpenGuide: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ajustes de la Aplicación",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), thickness = 1.dp)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    ConfigTab(viewModel = viewModel, onOpenGuide = onOpenGuide)
                }
            }
        }
    }
}
