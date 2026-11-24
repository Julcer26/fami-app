package com.example.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.OtaUpdateManager
import com.example.database.ActivityLog
import com.example.database.AppSetting
import com.example.database.UserProfile
import com.example.ui.theme.*
import com.example.viewmodel.FamilyViewModel
import kotlinx.coroutines.launch
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
    val paper = FamiTheme.paper
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(paper.sheetCream)
            .border(1.4.dp, paper.border, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEachIndexed { index, name ->
            val isSelected = selectedCategory == index
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) FamiCoral else Color.Transparent)
                    .border(1.dp, if (isSelected) paper.border else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable { onSelect(index) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) Color.White else paper.ink
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
    val paper = FamiTheme.paper
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(paper.sheetCream)
            .border(1.6.dp, paper.border, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        categories.forEachIndexed { index, name ->
            val isSelected = selectedCategory == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) paper.stickyYellow else Color.Transparent)
                    .border(
                        width = if (isSelected) 1.4.dp else 0.dp,
                        color = if (isSelected) paper.border else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) paper.ink else paper.inkMuted
                )
            }
        }
    }
}

private fun shareFamilyInvitation(context: Context, familyCode: String, familyName: String) {
    if (familyCode.isBlank()) return
    com.example.analytics.FamiUsageTracker.track(context, com.example.analytics.FamiUsageTracker.FEATURE_FAMILY_INVITE_SHARED)
    val homeLabel = familyName.trim().ifBlank { "nuestro hogar Fami" }
    val text = "🏡 ¡Te invito a unirte a $homeLabel en Fami!\n\n" +
        "1. Descarga Fami: https://somosfami.app\n" +
        "2. Toca «Unirme con Código» e ingresa este código:\n" +
        "🔑 $familyCode"
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Invitación a $homeLabel en Fami")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        context.startActivity(Intent.createChooser(sendIntent, "Compartir invitación familiar"))
    } catch (_: Exception) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Invitación Fami", text))
        Toast.makeText(context, "Invitación copiada al portapapeles", Toast.LENGTH_SHORT).show()
    }
}

private fun openAppSystemSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Abre Ajustes > Aplicaciones > Fami en tu teléfono", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfigMainContent(
    selectedCategory: Int,
    onSelectCategory: (Int) -> Unit = {},
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
    val s = LocalFamiStrings.current
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteAccountError by remember { mutableStateOf<String?>(null) }
    var deleteRequiresPassword by remember { mutableStateOf(false) }
    var deletePasswordInput by remember { mutableStateOf("") }
    var isDeletingAccount by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var showEditAliasDialog by remember { mutableStateOf(false) }
    var aliasInput by remember { mutableStateOf("") }
    var familyNameEditInput by remember { mutableStateOf("") }
    var roleEditInput by remember { mutableStateOf("") }
    var avatarFilter by remember { mutableStateOf("Todos") }
    var showSwitchFamilyDialog by remember { mutableStateOf(false) }
    var switchFamilyCodeInput by remember { mutableStateOf("") }
    var switchFamilyError by remember { mutableStateOf<String?>(null) }
    var isSwitchingFamily by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<UserProfile?>(null) }
    var legalDialogTab by remember { mutableStateOf<String?>(null) }
    var isCheckingOta by remember { mutableStateOf(false) }
    var manualOtaUpdate by remember { mutableStateOf<OtaUpdateManager.UpdateInfo?>(null) }
    val configScope = rememberCoroutineScope()

    val currentAppLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val currentAvatarId by viewModel.userAvatarId.collectAsStateWithLifecycle()
    val unlockMethod by viewModel.unlockMethod.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentFamilyRole by viewModel.familyRole.collectAsStateWithLifecycle()
    val familyIdState by viewModel.familyId.collectAsStateWithLifecycle()
    val isAdminRole by viewModel.isAdminRole.collectAsStateWithLifecycle()
    val managementMode by viewModel.managementMode.collectAsStateWithLifecycle()
    val displayAlias = currentUser.takeIf { it.isNotBlank() && !it.contains("@") } ?: "Usuario Fami"
    val currentFamilyName = remember(settings) {
        settings.find { it.key == "family_name" }?.value.orEmpty()
    }
    val configContext = LocalContext.current
    val biometricSupported = remember(configContext) {
        com.example.canAuthenticateBiometric(configContext)
    }

    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(configContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(configContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var otaInstallGranted by remember {
        mutableStateOf(OtaUpdateManager.canInstallPackages(configContext))
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
        if (!granted) {
            Toast.makeText(configContext, "Activa Cámara desde Ajustes de la app para escanear recibos", Toast.LENGTH_SHORT).show()
            openAppSystemSettings(configContext)
        }
    }
    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
        if (granted) {
            viewModel.syncFcmTokenAndWorker()
        } else {
            openAppSystemSettings(configContext)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectedCategory == 0) {
            // --- MI ALIAS & MI AVATAR FAMI (12 AVATARES) ---
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                MemberAvatarBadge(
                                    memberName = displayAlias,
                                    profiles = profiles,
                                    currentUser = currentUser,
                                    currentUserAvatarId = currentAvatarId,
                                    size = 40.dp,
                                    showAliasBelow = true,
                                    aliasText = displayAlias,
                                    aliasFontSize = 9.5.sp,
                                    aliasMaxWidth = 68.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = displayAlias,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = paper.ink
                                    )
                                    Text(
                                        text = buildString {
                                            if (currentFamilyName.isNotBlank()) {
                                                append(currentFamilyName)
                                                append(" • ")
                                            }
                                            if (managementMode == "individual") {
                                                append("Titular Individual")
                                            } else {
                                                append(currentFamilyRole.ifBlank { "Miembro" })
                                            }
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (paper.isDark) FamiCoralLight else FamiCoralDeep,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = s.settingsAliasSubtitle,
                                        fontSize = 10.sp,
                                        color = paper.inkMuted
                                    )
                                }
                            }
                            Tactile3DButton(
                                onClick = {
                                    aliasInput = if (currentUser.contains("@")) "" else currentUser
                                    familyNameEditInput = currentFamilyName
                                    roleEditInput = currentFamilyRole.ifBlank { "Administrador" }
                                    showEditAliasDialog = true
                                },
                                faceColor = paper.stickyYellow,
                                lipColor = Lip3DGold,
                                borderColor = paper.border,
                                cornerRadius = 10.dp,
                                lipHeight = 3.dp,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = s.settingsAliasBtn, tint = paper.ink, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(s.settingsAliasBtn, fontSize = 11.sp, fontWeight = FontWeight.Black, color = paper.ink)
                            }
                        }

                        HorizontalDivider(color = paper.border.copy(alpha = 0.18f))

                        Text(
                            text = s.settingsAvatarTitle,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Todos", "Adultos", "Menores", "F", "M").forEach { filter ->
                                val active = avatarFilter == filter
                                val label = when (filter) {
                                    "F" -> "Femenino"
                                    "M" -> "Masculino"
                                    else -> filter
                                }
                                Surface(
                                    onClick = { avatarFilter = filter },
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (active) FamiCoral else paper.sheetCream,
                                    border = BorderStroke(1.dp, if (active) FamiCoral else paper.border.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (active) Color.White else paper.ink,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        val filteredAvatars = remember(avatarFilter) {
                            when (avatarFilter) {
                                "Adultos" -> AvatarCatalog.ALL.filter { it.ageGroup == "Adulto" }
                                "Menores" -> AvatarCatalog.ALL.filter { it.ageGroup == "Menor" }
                                "F" -> AvatarCatalog.ALL.filter { it.gender == "F" }
                                "M" -> AvatarCatalog.ALL.filter { it.gender == "M" }
                                else -> AvatarCatalog.ALL
                            }
                        }
                        filteredAvatars.chunked(4).forEach { rowAvatars ->
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
                                repeat(4 - rowAvatars.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // --- MÉTODO DE DESBLOQUEO (HUELLA DIGITAL vs PIN) ---
            item {
                NotebookPaperSheet(
                    modifier = Modifier.fillMaxWidth(),
                    sheetColor = paper.sheet,
                    borderColor = paper.border,
                    shadowColor = paper.shadow3D,
                    showRuledLines = false,
                    showMarginLine = false
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = FamiCoral
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Método de Acceso",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                                Text(
                                    text = "Elige si deseas entrar a Fami con huella o PIN de 5 dígitos",
                                    fontSize = 11.sp,
                                    color = paper.inkMuted
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
                                onClick = {
                                    if (biometricSupported) {
                                        viewModel.updateUnlockPreference("biometric")
                                    } else {
                                        Toast.makeText(
                                            configContext,
                                            "No hay huella o biometría configurada en este dispositivo",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isBiometric) paper.stickyMint else paper.sheetCream,
                                border = BorderStroke(
                                    if (isBiometric) 2.dp else 1.2.dp,
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
                                    if (isPin) 2.dp else 1.2.dp,
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
                            border = BorderStroke(1.2.dp, paper.border.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = null, tint = paper.ink, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(s.settingsChangePin, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = paper.ink)
                        }
                    }
                }
            }

            // --- GUÍA INTERACTIVA Y AYUDA ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Tactile3DButton(
                        onClick = onOpenGuide,
                        modifier = Modifier.weight(1f),
                        faceColor = paper.stickyMint,
                        lipColor = Lip3DMint,
                        borderColor = paper.border,
                        cornerRadius = 14.dp,
                        lipHeight = 4.dp,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = paper.ink, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = s.settingsGuideBtn,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink
                        )
                    }

                    Tactile3DButton(
                        onClick = { onSelectCategory(2) },
                        modifier = Modifier.weight(1f),
                        faceColor = paper.stickySky,
                        lipColor = paper.lipPaper,
                        borderColor = paper.border,
                        cornerRadius = 14.dp,
                        lipHeight = 4.dp,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = paper.ink, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = s.settingsHelpBtn,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink
                        )
                    }
                }
            }

            // --- MODO DE GESTIÓN (FAMILIAR VS INDIVIDUAL) ---
            item {
                val isIndividual = managementMode == "individual"
                NotebookPaperSheet(
                    modifier = Modifier.fillMaxWidth(),
                    sheetColor = paper.sheet,
                    borderColor = paper.border,
                    shadowColor = paper.shadow3D,
                    showRuledLines = false,
                    showMarginLine = false
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Modo de Organización",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink
                        )
                        Text(
                            text = if (isIndividual) {
                                "👤 Modo Individual activo: interfaz enfocada 100% en tus finanzas, tareas y metas personales."
                            } else {
                                "👨‍👩‍👧‍👦 Modo Familiar activo: comparte gastos, código de hogar, compras y tareas con tu familia."
                            },
                            fontSize = 11.sp,
                            color = paper.inkMuted
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { viewModel.updateManagementMode("familiar") },
                                shape = RoundedCornerShape(12.dp),
                                color = if (!isIndividual) paper.stickyMint else paper.sheetCream,
                                border = BorderStroke(
                                    if (!isIndividual) 2.dp else 1.2.dp,
                                    if (!isIndividual) FamiEmerald else paper.border.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "👨‍👩‍👧‍👦 Familiar",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = paper.ink
                                    )
                                }
                            }
                            Surface(
                                onClick = { viewModel.updateManagementMode("individual") },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isIndividual) paper.stickyYellow else paper.sheetCream,
                                border = BorderStroke(
                                    if (isIndividual) 2.dp else 1.2.dp,
                                    if (isIndividual) FamiCoral else paper.border.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "👤 Individual",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = paper.ink
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- THEME SWAP SECTION ---
            item {
                NotebookPaperSheet(
                    modifier = Modifier.fillMaxWidth(),
                    sheetColor = paper.sheet,
                    borderColor = paper.border,
                    shadowColor = paper.shadow3D,
                    showRuledLines = false,
                    showMarginLine = false
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
                                tint = FamiCoral
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    s.settingsThemeTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                                Text(
                                    s.settingsThemeSubtitle,
                                    fontSize = 11.sp,
                                    color = paper.inkMuted
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

            // --- LANGUAGE SELECTOR (10 MOST COMMON LANGUAGES, DEFAULT = PHONE) ---
            item {
                val activeLangOption = com.example.currency.CountryCurrencyConfig.getLanguageOption(currentAppLanguage)
                val systemDefaultCode = com.example.currency.CountryCurrencyConfig.detectSystemLanguageCode()
                val isUsingSystemLang = currentAppLanguage.equals(systemDefaultCode, ignoreCase = true)

                NotebookPaperSheet(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageDialog = true },
                    sheetColor = paper.sheet,
                    borderColor = paper.border,
                    shadowColor = paper.shadow3D,
                    showRuledLines = false,
                    showMarginLine = false
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
                                imageVector = Icons.Default.Language,
                                contentDescription = s.settingsLangTitle,
                                tint = FamiCoral
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    s.settingsLangTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                                Text(
                                    "${activeLangOption.flag} ${activeLangOption.nativeName}" +
                                        if (isUsingSystemLang) " ${s.settingsLangPhoneTag}" else "",
                                    fontSize = 11.sp,
                                    color = paper.inkMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = paper.stickyYellow,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.2.dp, paper.border)
                        ) {
                            Text(
                                text = s.changeBtn,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = paper.ink,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // --- CENTRO DE PERMISOS DEL DISPOSITIVO ---
            item {
                NotebookPaperSheet(
                    modifier = Modifier.fillMaxWidth(),
                    sheetColor = paper.sheet,
                    borderColor = paper.border,
                    shadowColor = paper.shadow3D,
                    showRuledLines = false,
                    showMarginLine = false
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = FamiCoral)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Permisos del Dispositivo",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                                Text(
                                    text = "Controla los permisos opcionales que usa Fami (solo cuando tú los pides)",
                                    fontSize = 11.sp,
                                    color = paper.inkMuted
                                )
                            }
                        }

                        PermissionStatusRow(
                            title = "📷 Cámara (Escáner IA)",
                            subtitle = "Solo al fotografiar facturas o recibos",
                            granted = cameraGranted,
                            onAction = {
                                if (cameraGranted) openAppSystemSettings(configContext)
                                else cameraPermLauncher.launch(Manifest.permission.CAMERA)
                            }
                        )
                        PermissionStatusRow(
                            title = "🔔 Notificaciones Push",
                            subtitle = "Avisos de tareas asignadas y alertas",
                            granted = notifGranted,
                            onAction = {
                                if (!notifGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    openAppSystemSettings(configContext)
                                }
                            }
                        )
                        PermissionStatusRow(
                            title = "👆 Biometría / Huella",
                            subtitle = if (biometricSupported) "Sensor disponible en el dispositivo" else "Sin biometría configurada en Android",
                            granted = biometricSupported,
                            actionLabel = if (biometricSupported) "Activo" else "Ajustes",
                            onAction = { if (!biometricSupported) openAppSystemSettings(configContext) }
                        )
                        PermissionStatusRow(
                            title = "📦 Instalación OTA directa",
                            subtitle = "Permite instalar APKs firmados verificados por SHA-256",
                            granted = otaInstallGranted,
                            actionLabel = if (otaInstallGranted) "Activo" else "Activar",
                            onAction = {
                                otaInstallGranted = OtaUpdateManager.canInstallPackages(configContext)
                                if (!otaInstallGranted) {
                                    OtaUpdateManager.openInstallPermissionSettings(configContext)
                                }
                            }
                        )
                    }
                }
            }

            // --- VERSIÓN Y ACTUALIZACIÓN OTA ---
            item {
                val installedVerName = remember(configContext) { OtaUpdateManager.getInstalledVersionName(configContext) }
                val installedVerCode = remember(configContext) { OtaUpdateManager.getInstalledVersionCode(configContext) }
                NotebookPaperSheet(
                    modifier = Modifier.fillMaxWidth(),
                    sheetColor = paper.sheet,
                    borderColor = paper.border,
                    shadowColor = paper.shadow3D,
                    showRuledLines = false,
                    showMarginLine = false
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = FamiCoral
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Actualizaciones OTA",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = paper.ink
                                    )
                                    Text(
                                        text = "Instalada: v$installedVerName (Build $installedVerCode) • Verificación SHA-256",
                                        fontSize = 11.sp,
                                        color = paper.inkMuted
                                    )
                                }
                            }
                        }
                        Tactile3DButton(
                            onClick = {
                                if (!isCheckingOta) {
                                    com.example.analytics.FamiUsageTracker.track(configContext, com.example.analytics.FamiUsageTracker.FEATURE_OTA_CHECK)
                                    isCheckingOta = true
                                    configScope.launch {
                                        val info = OtaUpdateManager.checkForUpdates(configContext, ignoreDismissed = true)
                                        isCheckingOta = false
                                        if (info.isUpdateAvailable && info.downloadUrl.isNotEmpty()) {
                                            manualOtaUpdate = info
                                        } else {
                                            Toast.makeText(
                                                configContext,
                                                "Ya tienes la última versión de Fami (v$installedVerName) ✅",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            faceColor = paper.stickyMint,
                            lipColor = Lip3DMint,
                            borderColor = paper.border,
                            cornerRadius = 12.dp,
                            lipHeight = 3.dp,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (isCheckingOta) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = paper.ink
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Buscando versión nueva...", fontSize = 12.sp, fontWeight = FontWeight.Black, color = paper.ink)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = paper.ink, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buscar Actualización OTA", fontSize = 12.sp, fontWeight = FontWeight.Black, color = paper.ink)
                            }
                        }
                    }
                }
            }

            // --- FAMILY CODE, SESSION & LEGAL ---
            item {
                val familyId by viewModel.familyId.collectAsStateWithLifecycle()
                val context = LocalContext.current

                NotebookPaperSheet(
                    modifier = Modifier.fillMaxWidth(),
                    sheetColor = paper.sheet,
                    borderColor = paper.border,
                    shadowColor = paper.shadow3D,
                    showRuledLines = false,
                    showMarginLine = false
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (managementMode != "individual") {
                            Text(
                                s.settingsFamilyCodeTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = paper.ink
                            )
                            Text(
                                "${s.settingsFamilyCodeSubPrefix} '$displayAlias'.",
                                fontSize = 11.sp,
                                color = paper.inkMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    color = paper.stickyYellow,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.4.dp, paper.border)
                                ) {
                                    Text(
                                        text = familyId ?: "...",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = paper.ink,
                                        letterSpacing = 2.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Tactile3DButton(
                                        onClick = {
                                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText(s.settingsFamilyCodeTitle, familyId ?: "")
                                            clipboardManager.setPrimaryClip(clip)
                                            Toast.makeText(context, s.settingsCopiedToast, Toast.LENGTH_SHORT).show()
                                        },
                                        faceColor = paper.stickyMint,
                                        lipColor = Lip3DMint,
                                        borderColor = paper.border,
                                        cornerRadius = 10.dp,
                                        lipHeight = 3.dp,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = s.settingsCopyBtn, tint = paper.ink, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(s.settingsCopyBtn, fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = paper.ink)
                                    }
                                    Tactile3DButton(
                                        onClick = {
                                            shareFamilyInvitation(context, familyId.orEmpty(), currentFamilyName)
                                        },
                                        faceColor = FamiCoral,
                                        lipColor = Lip3DCoral,
                                        borderColor = paper.border,
                                        cornerRadius = 10.dp,
                                        lipHeight = 3.dp,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Invitar", tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Invitar", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Text(
                            s.settingsPrivacyLegalTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { legalDialogTab = "privacy" },
                                border = BorderStroke(1.2.dp, paper.border.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = FamiCoral, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(s.settingsPrivacyBtn, fontSize = 11.sp, color = paper.ink, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { legalDialogTab = "terms" },
                                border = BorderStroke(1.2.dp, paper.border.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = FamiCoral, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(s.settingsTermsBtn, fontSize = 11.sp, color = paper.ink, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { legalDialogTab = "legal" },
                            border = BorderStroke(1.2.dp, paper.border.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = paper.ink, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(s.settingsLegalNoticeBtn, fontSize = 11.sp, color = paper.ink, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Tactile3DButton(
                            onClick = { viewModel.logout() },
                            faceColor = paper.stickyPeach,
                            lipColor = Lip3DCoral,
                            borderColor = paper.border,
                            cornerRadius = 12.dp,
                            lipHeight = 3.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = s.settingsLogoutBtn, tint = paper.ink, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(s.settingsLogoutBtn, fontSize = 12.sp, fontWeight = FontWeight.Black, color = paper.ink)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDeleteAccountDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                            border = BorderStroke(1.2.dp, AlertRed.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = s.settingsDeleteAccountBtn, modifier = Modifier.size(14.dp), tint = AlertRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(s.settingsDeleteAccountBtn, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                        }
                    }
                }
            }
        } else if (selectedCategory == 1) {
            // --- INVITACIÓN RÁPIDA Y AFILIACIÓN FAMILIAR ---
            item {
                NotebookPaperSheet(
                    modifier = Modifier.fillMaxWidth(),
                    sheetColor = paper.sheet,
                    borderColor = paper.border,
                    shadowColor = paper.shadow3D,
                    showRuledLines = false,
                    showMarginLine = false
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🔗 Invitar a mi Familia",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink
                        )
                        Text(
                            text = "Comparte este código para que tu pareja o familiares se afilien a ${currentFamilyName.ifBlank { "este hogar" }} en tiempo real:",
                            fontSize = 11.sp,
                            color = paper.inkMuted
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = paper.stickyYellow,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.4.dp, paper.border)
                            ) {
                                Text(
                                    text = familyIdState ?: "...",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink,
                                    letterSpacing = 2.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Tactile3DButton(
                                    onClick = {
                                        val clipboardManager = configContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboardManager.setPrimaryClip(ClipData.newPlainText(s.settingsFamilyCodeTitle, familyIdState ?: ""))
                                        Toast.makeText(configContext, s.settingsCopiedToast, Toast.LENGTH_SHORT).show()
                                    },
                                    faceColor = paper.stickyMint,
                                    lipColor = Lip3DMint,
                                    borderColor = paper.border,
                                    cornerRadius = 10.dp,
                                    lipHeight = 3.dp,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = s.settingsCopyBtn, tint = paper.ink, modifier = Modifier.size(14.dp))
                                }
                                Tactile3DButton(
                                    onClick = { shareFamilyInvitation(configContext, familyIdState.orEmpty(), currentFamilyName) },
                                    faceColor = FamiCoral,
                                    lipColor = Lip3DCoral,
                                    borderColor = paper.border,
                                    cornerRadius = 10.dp,
                                    lipHeight = 3.dp,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Invitar", tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Invitar", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // --- MIEMBROS DEL HOGAR (Con control RBAC, Roles y Avatares) ---
            item {
                NotebookPaperSheet(
                    modifier = Modifier.fillMaxWidth(),
                    sheetColor = paper.sheet,
                    borderColor = paper.border,
                    shadowColor = paper.shadow3D,
                    showRuledLines = false,
                    showMarginLine = false
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${s.settingsMembersTitle} (${profiles.size})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = paper.ink
                            )
                            if (isAdminRole) {
                                Tactile3DButton(
                                    onClick = onAddProfileClick,
                                    faceColor = FamiCoral,
                                    lipColor = Lip3DCoral,
                                    borderColor = paper.border,
                                    cornerRadius = 10.dp,
                                    lipHeight = 3.dp,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = s.settingsAddMemberBtn, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(s.settingsAddMemberBtn, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAdminRole) {
                                s.settingsMembersSubAdmin
                            } else {
                                s.settingsMembersSubMember
                            },
                            fontSize = 11.sp,
                            color = paper.inkMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (profiles.isEmpty()) {
                            Text("No hay perfiles configurados.", fontSize = 11.sp, color = paper.inkMuted)
                        } else {
                            profiles.forEach { p ->
                                val isSelf = p.name.equals(currentUser, ignoreCase = true)
                                val isMemberAdmin = viewModel.isPrivilegedRole(p.role)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelf) paper.stickyYellow.copy(alpha = if (paper.isDark) 0.72f else 0.55f) else paper.sheetCream,
                                    border = BorderStroke(
                                        if (isSelf) 1.6.dp else 1.2.dp,
                                        if (isSelf) FamiCoral else paper.border.copy(alpha = 0.55f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            MemberAvatarBadge(
                                                memberName = p.name,
                                                profiles = profiles,
                                                currentUser = currentUser,
                                                currentUserAvatarId = currentAvatarId,
                                                explicitAvatarId = p.avatarId,
                                                size = 38.dp,
                                                showAliasBelow = false
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(p.name, fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = paper.ink)
                                                    if (isSelf) {
                                                        Surface(
                                                            color = FamiCoral,
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "TÚ",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = Color.White,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    Surface(
                                                        color = if (isMemberAdmin) paper.stickyMint else paper.sheet,
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = BorderStroke(0.8.dp, paper.border.copy(alpha = 0.4f))
                                                    ) {
                                                        Text(
                                                            text = if (isMemberAdmin) "👑 ${p.role.ifBlank { "Admin" }}" else "👤 ${p.role.ifBlank { "Miembro" }}",
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = paper.ink,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (isAdminRole || isSelf) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { onEditProfileClick(p) },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Edit,
                                                        contentDescription = "Edit profile",
                                                        tint = if (paper.isDark) FamiCoralLight else FamiCoral,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                if (isAdminRole && !isSelf && profiles.size > 1) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    IconButton(
                                                        onClick = { profileToDelete = p },
                                                        modifier = Modifier.size(30.dp)
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
            }

            // --- UNIRME A OTRO HOGAR EXISTENTE POR CÓDIGO ---
            item {
                NotebookPaperSheet(
                    modifier = Modifier.fillMaxWidth(),
                    sheetColor = paper.sheet,
                    borderColor = paper.border,
                    shadowColor = paper.shadow3D,
                    showRuledLines = false,
                    showMarginLine = false
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🔑 Cambiar / Unirme a otro Hogar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink
                        )
                        Text(
                            text = "¿Te invitaron a otro hogar Fami? Ingresa su Código Familiar para afiliarte sin perder tu cuenta.",
                            fontSize = 11.sp,
                            color = paper.inkMuted
                        )
                        OutlinedButton(
                            onClick = {
                                switchFamilyCodeInput = ""
                                switchFamilyError = null
                                showSwitchFamilyDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, FamiCoral),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, tint = if (paper.isDark) FamiCoralLight else FamiCoral, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Ingresar Código Familiar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (paper.isDark) FamiCoralLight else FamiCoral
                            )
                        }
                    }
                }
            }
        } else if (selectedCategory == 2) {
            // --- AYUDA, SOPORTE Y SUGERENCIAS ---
            item {
                HelpSupportContent(
                    viewModel = viewModel,
                    onOpenGuide = onOpenGuide,
                    onOpenLegal = { tab -> legalDialogTab = tab }
                )
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
                        label = { Text("PIN de 5 dígitos (distinto de 00000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = newPinInput.length == 5 && newPinInput != "00000",
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

    if (showEditAliasDialog) {
        AlertDialog(
            onDismissRequest = { showEditAliasDialog = false },
            containerColor = paper.sheet,
            title = { Text("✏️ Editar Mi Perfil", fontWeight = FontWeight.Black, color = paper.ink) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Personaliza cómo te verán en tareas, compras, metas y gastos:",
                        fontSize = 12.sp,
                        color = paper.inkMuted
                    )
                    OutlinedTextField(
                        value = aliasInput,
                        onValueChange = { aliasInput = it },
                        label = { Text("Tu Alias (sin correo)") },
                        placeholder = { Text("Ej: Mamá, Carlos, Sofía...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = familyNameEditInput,
                        onValueChange = { familyNameEditInput = it },
                        label = { Text(if (managementMode == "individual") "Nombre de tu Espacio" else "Nombre del Hogar / Familia") },
                        placeholder = { Text(if (managementMode == "individual") "Ej: Mis Finanzas" else "Ej: Familia Pérez") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (managementMode != "individual") {
                        Text(
                            text = "Tu Rol en el Hogar:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            com.example.currency.CountryCurrencyConfig.FAMILY_ROLES.forEach { roleOpt ->
                                val isSel = roleEditInput.equals(roleOpt.labelEs, ignoreCase = true)
                                Surface(
                                    onClick = { roleEditInput = roleOpt.labelEs },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) FamiCoral else paper.sheetCream,
                                    border = BorderStroke(1.dp, if (isSel) FamiCoral else paper.border.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "${roleOpt.emoji} ${roleOpt.labelEs}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else paper.ink,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = aliasInput.trim().length >= 2 && !aliasInput.contains("@"),
                    onClick = {
                        if (!aliasInput.trim().equals(currentUser.trim(), ignoreCase = true)) {
                            viewModel.updateUserAlias(aliasInput)
                        }
                        if (familyNameEditInput.trim().isNotBlank() && familyNameEditInput.trim() != currentFamilyName) {
                            viewModel.updateFamilyName(familyNameEditInput)
                        }
                        if (managementMode != "individual" && roleEditInput.isNotBlank() && !roleEditInput.equals(currentFamilyRole, ignoreCase = true)) {
                            viewModel.updateMyRole(roleEditInput)
                        }
                        showEditAliasDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FamiCoral)
                ) {
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAliasDialog = false }) {
                    Text("Cancelar", color = paper.inkMuted)
                }
            }
        )
    }

    if (showSwitchFamilyDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSwitchingFamily) showSwitchFamilyDialog = false },
            containerColor = paper.sheet,
            title = { Text("🔑 Unirme a otro Hogar", fontWeight = FontWeight.Black, color = paper.ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Ingresa el Código Familiar que te compartió el administrador. Cambiarás tu espacio activo a ese hogar compartido:",
                        fontSize = 12.sp,
                        color = paper.inkMuted
                    )
                    OutlinedTextField(
                        value = switchFamilyCodeInput,
                        onValueChange = { switchFamilyCodeInput = viewModel.normalizeFamilyCode(it).take(16) },
                        label = { Text("Código Familiar (Ej: A1B2C3D4)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    switchFamilyError?.let { err ->
                        Text(
                            text = err,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (paper.isDark) FamiCoralLight else AlertRed
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSwitchingFamily && switchFamilyCodeInput.length >= 4,
                    onClick = {
                        isSwitchingFamily = true
                        switchFamilyError = null
                        viewModel.switchToExistingFamilyByCode(switchFamilyCodeInput) { ok, msg ->
                            isSwitchingFamily = false
                            if (ok) {
                                showSwitchFamilyDialog = false
                            } else {
                                switchFamilyError = msg
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FamiCoral)
                ) {
                    if (isSwitchingFamily) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Unirme al Hogar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSwitchingFamily,
                    onClick = { showSwitchFamilyDialog = false }
                ) {
                    Text("Cancelar", color = paper.inkMuted)
                }
            }
        )
    }

    val dialogContext = LocalContext.current
    val googleReauthLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val idToken = account.idToken
            if (!idToken.isNullOrBlank()) {
                isDeletingAccount = true
                deleteAccountError = null
                viewModel.deleteUserAccount(
                    googleIdToken = idToken,
                    onSuccess = {
                        isDeletingAccount = false
                        showDeleteAccountDialog = false
                        deleteRequiresPassword = false
                        deletePasswordInput = ""
                        Toast.makeText(
                            dialogContext,
                            "Cuenta y datos eliminados definitivamente. Cerrando Fami...",
                            Toast.LENGTH_LONG
                        ).show()
                        (dialogContext as? android.app.Activity)?.finishAffinity()
                    },
                    onRequiresPassword = { email ->
                        isDeletingAccount = false
                        deleteRequiresPassword = true
                        deleteAccountError = "Ingresa la contraseña de $email para confirmar la eliminación."
                    },
                    onError = { err ->
                        isDeletingAccount = false
                        deleteAccountError = err
                    }
                )
            } else {
                isDeletingAccount = false
                deleteAccountError = "No se pudo confirmar tu cuenta de Google. Cierra sesión, vuelve a iniciar con Google e inténtalo de nuevo."
            }
        } catch (_: Exception) {
            isDeletingAccount = false
            deleteAccountError = "Re-autenticación con Google cancelada o fallida."
        }
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeletingAccount) {
                    showDeleteAccountDialog = false
                    deleteAccountError = null
                    deleteRequiresPassword = false
                    deletePasswordInput = ""
                }
            },
            containerColor = paper.sheet,
            title = { Text("¿Eliminar tu Cuenta?", color = AlertRed, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Esta acción es irreversible y permanente conforme a las políticas de Google Play y leyes de privacidad. Se eliminarán tus credenciales de acceso (correo y contraseña), tu cuenta en Firebase Auth, tus datos en la nube y tus preferencias locales, y la aplicación se cerrará.",
                        color = paper.ink,
                        fontSize = 13.sp
                    )
                    if (deleteRequiresPassword) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = deletePasswordInput,
                            onValueChange = { deletePasswordInput = it },
                            label = { Text("Confirma tu contraseña") },
                            placeholder = { Text("Contraseña de tu cuenta") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    deleteAccountError?.let { err: String ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(err, color = AlertRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isDeletingAccount && (!deleteRequiresPassword || deletePasswordInput.isNotBlank()),
                    onClick = {
                        isDeletingAccount = true
                        deleteAccountError = null
                        viewModel.deleteUserAccount(
                            confirmPassword = if (deleteRequiresPassword) deletePasswordInput else "",
                            onSuccess = {
                                isDeletingAccount = false
                                showDeleteAccountDialog = false
                                deleteRequiresPassword = false
                                deletePasswordInput = ""
                                Toast.makeText(
                                    dialogContext,
                                    "Cuenta y datos eliminados definitivamente. Cerrando Fami...",
                                    Toast.LENGTH_LONG
                                ).show()
                                (dialogContext as? android.app.Activity)?.finishAffinity()
                            },
                            onRequiresPassword = { email ->
                                isDeletingAccount = false
                                deleteRequiresPassword = true
                                deleteAccountError = "Por seguridad legal, ingresa la contraseña de $email para eliminarla definitivamente."
                            },
                            onRequiresGoogleReauth = { email ->
                                val webClientResId = dialogContext.resources.getIdentifier("default_web_client_id", "string", dialogContext.packageName)
                                val webClientId = if (webClientResId != 0) dialogContext.getString(webClientResId) else ""
                                if (webClientId.isNotBlank() && !webClientId.contains("placeholder", ignoreCase = true)) {
                                    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                                    )
                                        .requestIdToken(webClientId)
                                        .requestEmail()
                                        .build()
                                    val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(dialogContext, gso)
                                    googleReauthLauncher.launch(client.signInIntent)
                                } else {
                                    isDeletingAccount = false
                                    deleteAccountError = "Por seguridad de Google ($email), cierra sesión y vuelve a iniciar sesión antes de eliminar la cuenta."
                                }
                            },
                            onError = { err: String ->
                                isDeletingAccount = false
                                deleteAccountError = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    if (isDeletingAccount) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Eliminando...", color = Color.White)
                    } else {
                        Text(
                            if (deleteRequiresPassword) "Confirmar y Eliminar" else "Sí, Eliminar y Cerrar",
                            color = Color.White
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeletingAccount,
                    onClick = {
                        showDeleteAccountDialog = false
                        deleteAccountError = null
                        deleteRequiresPassword = false
                        deletePasswordInput = ""
                    }
                ) {
                    Text("Cancelar", color = paper.inkMuted)
                }
            }
        )
    }

    if (showLanguageDialog) {
        val systemDefaultCode = com.example.currency.CountryCurrencyConfig.detectSystemLanguageCode()
        val systemLangOpt = com.example.currency.CountryCurrencyConfig.getLanguageOption(systemDefaultCode)
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = paper.sheet,
            title = {
                Text(
                    s.settingsLangTitle,
                    fontWeight = FontWeight.Black,
                    color = paper.ink
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        val isSystemSelected = currentAppLanguage.equals(systemDefaultCode, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.updateAppLanguage(systemDefaultCode)
                                    showLanguageDialog = false
                                },
                            color = if (isSystemSelected) paper.stickyYellow else paper.sheetCream,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isSystemSelected) 1.6.dp else 1.dp,
                                color = if (isSystemSelected) paper.border else paper.border.copy(alpha = 0.28f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.settingsLangAutoOption,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = paper.ink
                                    )
                                    Text(
                                        text = "${s.settingsLangAutoDetected} ${systemLangOpt.flag} ${systemLangOpt.nativeName}",
                                        fontSize = 11.sp,
                                        color = paper.inkMuted
                                    )
                                }
                                if (isSystemSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = FamiCoral, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    items(
                        com.example.currency.CountryCurrencyConfig.SUPPORTED_LANGUAGES,
                        key = { it.code }
                    ) { lang ->
                        val isSelected = currentAppLanguage.equals(lang.code, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.updateAppLanguage(lang.code)
                                    showLanguageDialog = false
                                },
                            color = if (isSelected) paper.stickyMint else paper.sheetCream,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isSelected) 1.6.dp else 1.dp,
                                color = if (isSelected) paper.border else paper.border.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(lang.flag, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = lang.nativeName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = paper.ink
                                        )
                                        Text(
                                            text = lang.name,
                                            fontSize = 10.5.sp,
                                            color = paper.inkMuted
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = FamiCoral, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(s.close, fontWeight = FontWeight.Bold, color = paper.ink)
                }
            }
        )
    }

    legalDialogTab?.let { initialTab ->
        LegalDocumentDialog(
            initialTab = initialTab,
            onDismiss = { legalDialogTab = null }
        )
    }

    manualOtaUpdate?.let { info ->
        AlertDialog(
            onDismissRequest = { manualOtaUpdate = null },
            containerColor = paper.sheet,
            title = {
                Text(
                    text = if (info.latestVersionName.isNotBlank()) "Nueva Versión v${info.latestVersionName}" else "Actualización Disponible",
                    fontWeight = FontWeight.Black,
                    color = paper.ink
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Hay una actualización oficial verificada con SHA-256 lista para descargar e instalar.",
                        fontSize = 13.sp,
                        color = paper.inkMuted
                    )
                    if (info.changelog.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = paper.stickyYellow,
                            border = BorderStroke(1.2.dp, paper.border.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = info.changelog,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = paper.ink,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = info
                    manualOtaUpdate = null
                    Toast.makeText(configContext, "Descargando actualización OTA...", Toast.LENGTH_LONG).show()
                    OtaUpdateManager.downloadAndInstallUpdate(
                        configContext,
                        target.downloadUrl,
                        target.latestVersionCode,
                        target.expectedApkSha256
                    )
                }) {
                    Text("Descargar e Instalar", fontWeight = FontWeight.Black, color = FamiCoral)
                }
            },
            dismissButton = {
                TextButton(onClick = { manualOtaUpdate = null }) {
                    Text(s.cancel, color = paper.inkMuted)
                }
            }
        )
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    actionLabel: String = if (granted) "Activo" else "Activar",
    onAction: () -> Unit
) {
    val paper = FamiTheme.paper
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = paper.sheetCream,
        border = BorderStroke(1.dp, paper.border.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
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
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = paper.ink
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = paper.inkMuted
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                onClick = onAction,
                shape = RoundedCornerShape(9.dp),
                color = if (granted) paper.stickyMint else paper.stickyPeach,
                border = BorderStroke(1.dp, if (granted) FamiEmerald else FamiCoral)
            ) {
                Text(
                    text = if (granted) "✓ $actionLabel" else actionLabel,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = if (granted) (if (paper.isDark) FamiEmerald else Lip3DMint) else (if (paper.isDark) FamiCoralLight else FamiCoralDeep),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfigTab(viewModel: FamilyViewModel, onOpenGuide: () -> Unit = {}) {
    val paper = FamiTheme.paper
    val s = LocalFamiStrings.current
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle()

    var showProfileCreatorDialog by remember { mutableStateOf(false) }
    var isEditingProfile by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<UserProfile?>(null) }
    var profileNameInput by remember { mutableStateOf("") }
    var profileColorHexSelected by remember { mutableStateOf("#D0BCFF") }
    var profileAvatarIdSelected by remember { mutableStateOf("woman_1") }
    var profileRoleSelected by remember { mutableStateOf("Miembro familiar") }

    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val managementMode by viewModel.managementMode.collectAsStateWithLifecycle()
    val isIndividual = managementMode == "individual"

    val profileAvatarColors = listOf("#D0BCFF", "#EFB8C8", "#81C784", "#64B5F6", "#FFD54F", "#BA68C8", "#E0E0E0")
    var selectedCategory by remember { mutableStateOf(0) }
    LaunchedEffect(isIndividual) {
        if (isIndividual && selectedCategory == 1) {
            selectedCategory = 0
        }
    }
    val categoriesList = if (isIndividual) {
        listOf(s.settingsCatGeneral, s.settingsCatHelp)
    } else {
        listOf(s.settingsCatGeneral, s.settingsCatMembers, s.settingsCatHelp)
    }
    val visualTabIndex = if (isIndividual && selectedCategory == 2) 1 else selectedCategory
    val onVisualTabSelect: (Int) -> Unit = { idx ->
        selectedCategory = if (isIndividual && idx == 1) 2 else idx
    }

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
                    selectedCategory = visualTabIndex,
                    onSelect = onVisualTabSelect,
                    modifier = Modifier.width(180.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    ConfigMainContent(
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it },
                        profiles = profiles,
                        settings = settings,
                        activityLogs = activityLogs,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { viewModel.toggleDarkTheme(it) },
                        onAddProfileClick = {
                            isEditingProfile = false
                            editingProfile = null
                            profileNameInput = ""
                            profileRoleSelected = "Miembro familiar"
                            profileAvatarIdSelected = AvatarCatalog.ALL.first().id
                            showProfileCreatorDialog = true
                        },
                        onEditProfileClick = { p ->
                            isEditingProfile = true
                            editingProfile = p
                            profileNameInput = p.name
                            profileColorHexSelected = p.colorHex
                            profileRoleSelected = p.role.ifBlank { "Miembro familiar" }
                            profileAvatarIdSelected = p.avatarId.ifBlank { "woman_1" }
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
                    text = s.settingsTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = paper.ink,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                HorizontalCategoryTabs(
                    categories = categoriesList,
                    selectedCategory = visualTabIndex,
                    onSelect = onVisualTabSelect,
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
                        onSelectCategory = { selectedCategory = it },
                        profiles = profiles,
                        settings = settings,
                        activityLogs = activityLogs,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { viewModel.toggleDarkTheme(it) },
                        onAddProfileClick = {
                            isEditingProfile = false
                            editingProfile = null
                            profileNameInput = ""
                            profileRoleSelected = "Miembro familiar"
                            profileAvatarIdSelected = AvatarCatalog.ALL.first().id
                            showProfileCreatorDialog = true
                        },
                        onEditProfileClick = { p ->
                            isEditingProfile = true
                            editingProfile = p
                            profileNameInput = p.name
                            profileColorHexSelected = p.colorHex
                            profileRoleSelected = p.role.ifBlank { "Miembro familiar" }
                            profileAvatarIdSelected = p.avatarId.ifBlank { "woman_1" }
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
            containerColor = paper.sheet,
            title = {
                Text(
                    if (isEditingProfile) "Editar Miembro del Hogar" else "Registrar Miembro del Hogar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = paper.ink
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        value = profileNameInput,
                        onValueChange = { profileNameInput = it },
                        label = { Text("Nombre o Alias del Miembro") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        "Rol y permisos en el hogar:",
                        fontSize = 12.sp,
                        color = paper.ink,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        com.example.currency.CountryCurrencyConfig.FAMILY_ROLES.forEach { roleOpt ->
                            val isSel = profileRoleSelected.equals(roleOpt.labelEs, ignoreCase = true)
                            Surface(
                                onClick = { profileRoleSelected = roleOpt.labelEs },
                                shape = RoundedCornerShape(9.dp),
                                color = if (isSel) FamiCoral else paper.sheetCream,
                                border = BorderStroke(1.dp, if (isSel) FamiCoral else paper.border.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${roleOpt.emoji} ${roleOpt.labelEs}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else paper.ink,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Text(
                        "Elige su avatar:",
                        fontSize = 12.sp,
                        color = paper.ink,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AvatarCatalog.ALL.chunked(4).forEach { rowAvatars ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowAvatars.forEach { av ->
                                    val isChosen = profileAvatarIdSelected == av.id
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isChosen) paper.stickyYellow else paper.sheetCream)
                                            .border(
                                                width = if (isChosen) 2.dp else 1.dp,
                                                color = if (isChosen) FamiCoral else paper.border.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { profileAvatarIdSelected = av.id }
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = av.drawableRes),
                                            contentDescription = av.labelEs,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .border(1.dp, paper.border, CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        "Color de acento:",
                        fontSize = 11.sp,
                        color = paper.inkMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        profileAvatarColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(parseSafeProfileColor(hex))
                                    .clickable { profileColorHexSelected = hex }
                                    .border(
                                        width = if (profileColorHexSelected == hex) 2.dp else 0.dp,
                                        color = if (profileColorHexSelected == hex) paper.ink else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Tactile3DButton(
                    onClick = {
                        if (profileNameInput.isNotBlank() && !profileNameInput.contains("@")) {
                            if (isEditingProfile && editingProfile != null) {
                                viewModel.editProfile(editingProfile!!, profileNameInput, profileColorHexSelected, profileAvatarIdSelected, profileRoleSelected)
                            } else {
                                viewModel.createProfile(profileNameInput, profileColorHexSelected, profileAvatarIdSelected, profileRoleSelected)
                            }
                            profileNameInput = ""
                            showProfileCreatorDialog = false
                            isEditingProfile = false
                            editingProfile = null
                        }
                    },
                    faceColor = FamiCoral,
                    lipColor = Lip3DCoral,
                    borderColor = paper.border,
                    cornerRadius = 10.dp,
                    lipHeight = 3.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        if (isEditingProfile) s.save else s.settingsAddMemberBtn,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 12.sp
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
                    Text(s.cancel, color = paper.inkMuted)
                }
            }
        )
    }
}

@Composable
fun ConfigDialog(
    viewModel: FamilyViewModel,
    onDismiss: () -> Unit,
    onOpenGuide: () -> Unit = {}
) {
    val paper = FamiTheme.paper
    val s = LocalFamiStrings.current
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = paper.sheet),
            border = BorderStroke(1.8.dp, paper.border)
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
                        text = s.settingsTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = paper.ink
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = s.close,
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
