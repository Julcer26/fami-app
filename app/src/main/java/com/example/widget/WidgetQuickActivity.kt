package com.example.widget

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.MainActivity
import com.example.api.GeminiParser
import com.example.api.ParsedReceiptItem
import com.example.database.TransactionItem
import com.example.ui.BankWalletSelectorChip
import com.example.ui.ReceiptScanConfirmDialog
import com.example.ui.StickyNoteSheet
import com.example.ui.Tactile3DButton
import com.example.ui.theme.*
import com.example.uriToBitmap
import com.example.viewmodel.FamilyViewModel
import com.example.viewmodel.FamilyViewModelFactory
import kotlinx.coroutines.launch
import java.io.File

class WidgetQuickActivity : FragmentActivity() {

    private val actionState = mutableStateOf(FamiQuickWidgetProvider.ACTION_WRITE)
    private val triggerCounter = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        parseIntent(intent)

        setContent {
            val context = LocalContext.current
            val viewModel: FamilyViewModel = viewModel(
                factory = FamilyViewModelFactory(context.applicationContext)
            )
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                WidgetQuickOverlayScreen(
                    viewModel = viewModel,
                    initialAction = actionState.value,
                    triggerKey = triggerCounter.intValue,
                    onFinishOverlay = {
                        finish()
                        overridePendingTransition(0, 0)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseIntent(intent)
    }

    private fun parseIntent(intent: Intent?) {
        val act = intent?.getStringExtra(FamiQuickWidgetProvider.EXTRA_WIDGET_ACTION)
            ?.trim()
            ?.ifBlank { FamiQuickWidgetProvider.ACTION_WRITE }
            ?: FamiQuickWidgetProvider.ACTION_WRITE
        actionState.value = act
        triggerCounter.intValue += 1
    }
}

@Composable
private fun WidgetQuickOverlayScreen(
    viewModel: FamilyViewModel,
    initialAction: String,
    triggerKey: Int,
    onFinishOverlay: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val paper = FamiTheme.paper

    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val wallets by viewModel.wallets.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val primaryCurrency by viewModel.primaryCurrency.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isRegisteringNlp by viewModel.isRegisteringNlp.collectAsStateWithLifecycle()
    val nlpError by viewModel.nlpError.collectAsStateWithLifecycle()

    var hasCheckedAuthInit by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.unlockForWidgetIfSignedIn()
        hasCheckedAuthInit = true
    }

    var inputText by remember { mutableStateOf("") }
    var selectedWalletId by remember { mutableStateOf("") }
    var isScanningReceipt by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Estado para confirmación de recibo escaneado
    var showConfirmScanDialog by remember { mutableStateOf(false) }
    var scannedAmount by remember { mutableStateOf("") }
    var scannedDesc by remember { mutableStateOf("") }
    var scannedCurrency by remember { mutableStateOf("USD") }
    var scannedCategory by remember { mutableStateOf("") }
    var scannedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var scannedBalanceDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var scannedItems by remember { mutableStateOf<List<ParsedReceiptItem>>(emptyList()) }
    var scannedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var saveReceiptPhoto by remember { mutableStateOf(true) }

    val submitTextMovement: (String) -> Unit = { rawText ->
        val clean = rawText.trim()
        if (clean.isNotEmpty() && !isRegisteringNlp) {
            keyboardController?.hide()
            viewModel.registerFromNaturalLanguage(clean, selectedWalletId) { success, msg ->
                if (success) {
                    Toast.makeText(context, "✅ $msg", Toast.LENGTH_SHORT).show()
                    onFinishOverlay()
                } else if (initialAction != FamiQuickWidgetProvider.ACTION_WRITE) {
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val processReceiptUri: (Uri) -> Unit = { uri ->
        coroutineScope.launch {
            isScanningReceipt = true
            scanError = null
            val bitmap = uriToBitmap(context, uri)
            if (bitmap != null) {
                val availableCats = categories.map { it.categoryName }
                val customKey = settings.find { it.key == "gemini_api_key" }?.value
                val (parsedTx, errorStr, _) = GeminiParser.parseReceiptImage(
                    bitmap,
                    currentUser.ifBlank { "Usuario" },
                    availableCats,
                    customKey
                )
                if (parsedTx != null) {
                    val tx = parsedTx.transaction
                    scannedAmount = tx.originalAmount.toString()
                    scannedDesc = tx.description
                    scannedCurrency = tx.currency
                    scannedCategory = if (tx.category in availableCats) tx.category else (availableCats.firstOrNull() ?: "General")
                    scannedDate = tx.date
                    scannedBalanceDate = tx.date
                    scannedItems = parsedTx.items
                    scannedBitmap = bitmap
                    showConfirmScanDialog = true
                } else {
                    scanError = errorStr ?: "No se pudo leer el recibo."
                }
            } else {
                scanError = "No se pudo procesar la imagen seleccionada."
            }
            isScanningReceipt = false
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()?.trim().orEmpty()
            if (spoken.isNotEmpty()) {
                inputText = spoken
                submitTextMovement(spoken)
            } else if (initialAction == FamiQuickWidgetProvider.ACTION_VOICE) {
                onFinishOverlay()
            }
        } else if (initialAction == FamiQuickWidgetProvider.ACTION_VOICE && inputText.isBlank()) {
            onFinishOverlay()
        }
    }

    val launchVoiceRecognizer = {
        val speechLocale = when (appLanguage.lowercase()) {
            "en" -> "en-US"
            "pt" -> "pt-BR"
            else -> "es-ES"
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLocale)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla para anotar tu gasto o ingreso")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Dictado por voz no disponible en este dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            processReceiptUri(uri)
        } else if (initialAction == FamiQuickWidgetProvider.ACTION_GALLERY && inputText.isBlank()) {
            onFinishOverlay()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = tempPhotoUri
        if (success && uri != null) {
            processReceiptUri(uri)
        } else if (initialAction == FamiQuickWidgetProvider.ACTION_SCAN && inputText.isBlank()) {
            onFinishOverlay()
        }
    }

    val launchCameraCapture = {
        try {
            val file = File.createTempFile("widget_receipt_", ".jpg", context.cacheDir).apply {
                createNewFile()
                deleteOnExit()
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            scanError = "No se pudo abrir la cámara: ${e.localizedMessage}"
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            Toast.makeText(context, "Permiso de cámara requerido para escanear", Toast.LENGTH_SHORT).show()
            if (initialAction == FamiQuickWidgetProvider.ACTION_SCAN) {
                onFinishOverlay()
            }
        }
    }

    val requestAndLaunchCamera = {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            launchCameraCapture()
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Seleccionar por defecto la primera billetera cuando carguen si aún no hay selección
    LaunchedEffect(wallets) {
        if (selectedWalletId.isBlank() && wallets.isNotEmpty()) {
            selectedWalletId = wallets.first().id
        }
    }

    // Disparo automático inmediato según el botón tocado en el widget una vez verificada la sesión
    LaunchedEffect(triggerKey, initialAction, isAuthenticated) {
        if (!isAuthenticated) return@LaunchedEffect
        viewModel.clearNlpError()
        scanError = null
        when (initialAction) {
            FamiQuickWidgetProvider.ACTION_WRITE -> {
                com.example.analytics.FamiUsageTracker.track(context, com.example.analytics.FamiUsageTracker.FEATURE_WIDGET_WRITE)
                withFrameNanos { }
                try {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                } catch (_: Exception) { }
            }
            FamiQuickWidgetProvider.ACTION_VOICE -> {
                com.example.analytics.FamiUsageTracker.track(context, com.example.analytics.FamiUsageTracker.FEATURE_WIDGET_VOICE)
                launchVoiceRecognizer()
            }
            FamiQuickWidgetProvider.ACTION_SCAN -> {
                com.example.analytics.FamiUsageTracker.track(context, com.example.analytics.FamiUsageTracker.FEATURE_WIDGET_SCAN)
                requestAndLaunchCamera()
            }
            FamiQuickWidgetProvider.ACTION_GALLERY -> {
                com.example.analytics.FamiUsageTracker.track(context, com.example.analytics.FamiUsageTracker.FEATURE_WIDGET_GALLERY)
                galleryLauncher.launch("image/*")
            }
        }
    }

    val isWriteMode = initialAction == FamiQuickWidgetProvider.ACTION_WRITE

    // Fondo completamente transparente sobre la pantalla de inicio del teléfono
    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isRegisteringNlp && !isScanningReceipt) {
                    onFinishOverlay()
                }
            }
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isAuthenticated) {
            if (hasCheckedAuthInit) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Absorber toques dentro de la notita */ }
                ) {
                    StickyNoteSheet(
                        modifier = Modifier.fillMaxWidth(),
                        noteColor = paper.stickyYellow,
                        tapeColor = FamiCoralSoft.copy(alpha = 0.72f),
                        borderColor = paper.border,
                        shadowColor = paper.shadow3D
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "👋 INICIA SESIÓN EN FAMI",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                                IconButton(
                                    onClick = { onFinishOverlay() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar",
                                        tint = paper.ink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Abre Fami para iniciar sesión o crear tu hogar antes de usar el acceso rápido.",
                                fontSize = 12.sp,
                                color = paper.inkMuted
                            )
                            Tactile3DButton(
                                onClick = {
                                    val openMain = Intent(context, MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    }
                                    context.startActivity(openMain)
                                    onFinishOverlay()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                faceColor = FamiCoral,
                                lipColor = Lip3DCoral,
                                borderColor = paper.border,
                                cornerRadius = 12.dp,
                                lipHeight = 4.dp,
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text("ABRIR FAMI", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
            }
        } else if (isWriteMode) {
            // Notita flotante SOLO para escritura rápida desde el widget
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Absorber toques dentro de la notita */ }
            ) {
                StickyNoteSheet(
                    modifier = Modifier.fillMaxWidth(),
                    noteColor = paper.stickyYellow,
                    tapeColor = FamiCoralSoft.copy(alpha = 0.72f),
                    borderColor = paper.border,
                    shadowColor = paper.shadow3D
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "NOTA RÁPIDA CON FAMI IA ✏️",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                                Text(
                                    text = "Escribe tu gasto o ingreso sin abrir la app",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = paper.inkMuted
                                )
                            }
                            IconButton(
                                onClick = { onFinishOverlay() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = paper.ink,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (wallets.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(wallets, key = { it.id.ifBlank { "${it.name}_${it.currency}" } }) { w ->
                                    val isSelected = selectedWalletId == w.id
                                    BankWalletSelectorChip(
                                        walletName = w.name,
                                        currency = w.currency,
                                        selected = isSelected,
                                        onClick = {
                                            selectedWalletId = if (isSelected && wallets.size > 1) "" else w.id
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Fila principal: Campo de texto + Micrófono 3D + Enviar 3D
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = {
                                    Text(
                                        text = "Escribe tu gasto...",
                                        fontSize = 12.5.sp,
                                        color = paper.inkMuted
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Send
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = { submitTextMovement(inputText) }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .background(paper.sheet, RoundedCornerShape(14.dp)),
                                shape = RoundedCornerShape(14.dp),
                                trailingIcon = {
                                    if (inputText.isNotEmpty() && !isRegisteringNlp) {
                                        IconButton(
                                            onClick = { inputText = "" },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Limpiar",
                                                tint = paper.ink,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = paper.border,
                                    unfocusedBorderColor = paper.border,
                                    focusedTextColor = paper.ink,
                                    unfocusedTextColor = paper.ink
                                ),
                                maxLines = 2
                            )

                            Tactile3DButton(
                                onClick = { launchVoiceRecognizer() },
                                faceColor = paper.stickyMint,
                                lipColor = Lip3DMint,
                                borderColor = paper.border,
                                cornerRadius = 13.dp,
                                lipHeight = 3.5.dp,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Dictar gasto por voz",
                                    tint = paper.ink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Tactile3DButton(
                                onClick = { submitTextMovement(inputText) },
                                faceColor = FamiCoral,
                                lipColor = Lip3DCoral,
                                borderColor = paper.border,
                                cornerRadius = 13.dp,
                                lipHeight = 3.5.dp,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                            ) {
                                if (isRegisteringNlp) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Enviar gasto",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (!nlpError.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = nlpError ?: "",
                                color = FamiAlertRed,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Fila inferior: ESCANEAR y GALERÍA
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Tactile3DButton(
                                onClick = { requestAndLaunchCamera() },
                                modifier = Modifier.weight(1f),
                                faceColor = FamiCoral,
                                lipColor = Lip3DCoral,
                                borderColor = paper.border,
                                cornerRadius = 12.dp,
                                lipHeight = 4.dp,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Escanear",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ESCANEAR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }

                            Tactile3DButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                faceColor = if (paper.isDark) Color(0xFF233269) else FamiMidnight,
                                lipColor = paper.lipNavy,
                                borderColor = paper.border,
                                cornerRadius = 12.dp,
                                lipHeight = 4.dp,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Galería",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "GALERÍA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Modo directo (Voz / Escanear / Galería):
            // Pantalla completamente limpia; solo muestra un indicador minimalista mientras Fami IA procesa
            if (isRegisteringNlp || isScanningReceipt) {
                Surface(
                    color = paper.stickyYellow,
                    border = BorderStroke(1.8.dp, paper.border),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.4.dp,
                            color = FamiCoral
                        )
                        Text(
                            text = if (isScanningReceipt) "Fami IA leyendo tu recibo..." else "Registrando con Fami IA...",
                            fontSize = 12.sp,
                            color = paper.ink,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            } else if (!scanError.isNullOrBlank() || !nlpError.isNullOrBlank()) {
                Surface(
                    color = paper.alertBg,
                    border = BorderStroke(1.8.dp, paper.border),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = FamiAlertRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = scanError ?: nlpError ?: "No se pudo completar",
                            fontSize = 11.5.sp,
                            color = paper.ink,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        IconButton(
                            onClick = { onFinishOverlay() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = paper.ink,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showConfirmScanDialog) {
        ReceiptScanConfirmDialog(
            scannedDesc = scannedDesc,
            scannedAmount = scannedAmount,
            scannedCurrency = scannedCurrency,
            scannedCategory = scannedCategory,
            scannedDate = scannedDate,
            scannedBalanceDate = scannedBalanceDate,
            scannedItems = scannedItems,
            saveReceiptPhoto = saveReceiptPhoto,
            primaryCurrency = primaryCurrency,
            categories = categories,
            wallets = wallets,
            onSetScannedDesc = { scannedDesc = it },
            onSetScannedAmount = { scannedAmount = it },
            onSetScannedCurrency = { scannedCurrency = it },
            onSetScannedCategory = { scannedCategory = it },
            onSetScannedDate = { scannedDate = it },
            onSetScannedBalanceDate = { scannedBalanceDate = it },
            onSetSaveReceiptPhoto = { saveReceiptPhoto = it },
            onDismiss = {
                showConfirmScanDialog = false
                if (initialAction == FamiQuickWidgetProvider.ACTION_SCAN ||
                    initialAction == FamiQuickWidgetProvider.ACTION_GALLERY
                ) {
                    onFinishOverlay()
                }
            },
            onConfirm = { chosenWalletId ->
                val amt = scannedAmount.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (amt > 0.0 && scannedDesc.isNotBlank()) {
                    coroutineScope.launch {
                        var receiptUrl = ""
                        if (saveReceiptPhoto && scannedBitmap != null) {
                            receiptUrl = viewModel.uploadReceiptImage(scannedBitmap!!) ?: ""
                        }
                        val txItems = scannedItems.map { item ->
                            val catName = if (categories.any { c -> c.categoryName == item.category }) item.category
                            else scannedCategory.ifEmpty { categories.firstOrNull()?.categoryName ?: "General" }
                            TransactionItem(name = item.name, amount = item.amount, category = catName)
                        }
                        viewModel.addTransaction(
                            originalAmount = amt,
                            category = scannedCategory.ifEmpty { categories.firstOrNull()?.categoryName ?: "General" },
                            description = scannedDesc,
                            currency = scannedCurrency,
                            isIncome = false,
                            date = scannedDate,
                            balanceDate = scannedBalanceDate,
                            receiptUrl = receiptUrl,
                            items = txItems,
                            walletId = chosenWalletId
                        ) { success, msg ->
                            val prefix = if (success) "✅ " else "❌ "
                            Toast.makeText(context, "$prefix$msg", Toast.LENGTH_SHORT).show()
                            if (success) {
                                showConfirmScanDialog = false
                                onFinishOverlay()
                            } else {
                                scanError = msg
                            }
                        }
                    }
                } else {
                    scanError = "Por favor ingrese un monto y descripción válidos."
                    showConfirmScanDialog = false
                }
            }
        )
    }
}
