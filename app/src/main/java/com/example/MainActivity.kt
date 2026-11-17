package com.example

import androidx.compose.ui.zIndex

import androidx.compose.ui.graphics.drawscope.rotate

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.graphics.BitmapFactory
import android.speech.RecognizerIntent
import android.widget.Toast
import java.io.ByteArrayOutputStream
import com.google.firebase.storage.FirebaseStorage
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import com.example.ui.*
import com.example.api.FinanceAiAnalyzer
import com.example.api.FinanceAiReport
import com.example.util.FinanceExportHelper
import com.example.api.GeminiParser
import com.example.api.BcvExchangeRateService
import kotlinx.coroutines.launch
import com.example.database.*
import com.example.ui.theme.*
import com.example.viewmodel.FamilyViewModel
import com.example.viewmodel.FamilyViewModelFactory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import androidx.fragment.app.FragmentActivity
import com.example.OtaUpdateManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication

class MainActivity : FragmentActivity() {
    var onImagePicked: ((Uri?) -> Unit)? = null
    var onPhotoTaken: ((Boolean) -> Unit)? = null
    var onPermissionResult: ((Boolean) -> Unit)? = null
    private val pendingOpenTabState = mutableStateOf<String?>(null)
    private val pendingChoreIdState = mutableStateOf<String?>(null)

    val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent() ){ uri ->
        onImagePicked?.invoke(uri)
    }

    val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture() ){ success ->
        onPhotoTaken?.invoke(success)
    }

    val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission() ){ isGranted ->
        onPermissionResult?.invoke(isGranted)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val targetTab = (intent?.getStringExtra("open_tab") ?: intent?.getStringExtra("tab"))?.trim()
        val targetChoreId = (intent?.getStringExtra("chore_id") ?: intent?.getStringExtra("choreId"))?.trim()
        if (!targetChoreId.isNullOrBlank()) {
            pendingChoreIdState.value = targetChoreId
            if (targetTab.isNullOrBlank()) {
                pendingOpenTabState.value = "tareas"
            }
            intent?.removeExtra("chore_id")
            intent?.removeExtra("choreId")
        }
        if (!targetTab.isNullOrBlank()) {
            pendingOpenTabState.value = targetTab
            intent?.removeExtra("open_tab")
            intent?.removeExtra("tab")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onDestroy() {
        OtaUpdateManager.unregisterDownloadReceiver(this)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNotificationIntent(intent)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val view = androidx.compose.ui.platform.LocalView.current
            val viewModel: FamilyViewModel = viewModel(factory = FamilyViewModelFactory(context.applicationContext))
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
            val isDarkThemeSetting by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            val pendingOpenTab by pendingOpenTabState
            val pendingChoreId by pendingChoreIdState

            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as? Activity)?.window ?: return@SideEffect
                    val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !isDarkThemeSetting
                    controller.isAppearanceLightNavigationBars = !isDarkThemeSetting
                }
            }

            MyApplicationTheme(darkTheme = isDarkThemeSetting) {
                MainScreen(
                    viewModel = viewModel,
                    pendingOpenTab = pendingOpenTab,
                    onConsumeOpenTab = { pendingOpenTabState.value = null },
                    pendingChoreId = pendingChoreId,
                    onConsumeChoreId = { pendingChoreIdState.value = null }
                )
            }
        }
    }
}

// Utility extension for hex color support

// --- Modern Design Utilities ---
@Composable
fun Modifier.scaleClickable(
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit
): Modifier {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by actualInteractionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = actualInteractionSource,
        indication = LocalIndication.current,
        onClick = onClick
    )
}

@Composable

fun PremiumCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(18.dp),
    colors: CardColors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    border: androidx.compose.foundation.BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val resolvedBorder = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f))
    if (onClick != null) {
        Card(
            modifier = modifier.scaleClickable(onClick = onClick),
            shape = shape,
            elevation = elevation,
            colors = colors,
            border = resolvedBorder
        ) { content() }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            elevation = elevation,
            colors = colors,
            border = resolvedBorder
        ) { content() }
    }
}


// --------------------------------

fun String.toColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color(0xFF6750A4) // Fallback default
    }
}

// Utility to convert Uri to Bitmap safely
fun uriToBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        android.util.Log.e("MainActivity", "Error converting Uri to Bitmap", e)
        null
    }
}


@OptIn(ExperimentalMaterial3Api::class)



data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val rotation: Float,
    val vRot: Float,
    val swayPhase: Float,
    val swaySpeed: Float,
    val color: Color,
    val size: Float,
    val shapeType: Int
)

@Composable
fun ConfettiEffect(
    trigger: Int,
    modifier: Modifier = Modifier
) {
    if (trigger <= 0) return

    val particles = remember(trigger) {
        val colors = listOf(
            Color(0xFF10B981),
            Color(0xFF6366F1),
            Color(0xFFF59E0B),
            Color(0xFFEC4899),
            Color(0xFF3B82F6),
            Color(0xFF8B5CF6),
            Color(0xFF14B8A6),
            Color(0xFFF43F5E),
            Color(0xFFFFD700)
        )
        List(140) {
            ConfettiParticle(
                x = 0.5f + (kotlin.random.Random.nextFloat() - 0.5f) * 0.7f,
                y = 0.3f + (kotlin.random.Random.nextFloat() - 0.5f) * 0.2f,
                vx = (kotlin.random.Random.nextFloat() - 0.5f) * 0.015f,
                vy = -0.012f - kotlin.random.Random.nextFloat() * 0.014f,
                rotation = kotlin.random.Random.nextFloat() * 360f,
                vRot = (kotlin.random.Random.nextFloat() - 0.5f) * 6f,
                swayPhase = kotlin.random.Random.nextFloat() * 6.28f,
                swaySpeed = 2f + kotlin.random.Random.nextFloat() * 3f,
                color = colors.random(),
                size = 10f + kotlin.random.Random.nextFloat() * 14f,
                shapeType = kotlin.random.Random.nextInt(3)
            )
        }
    }

    var frameTick by remember(trigger) { mutableLongStateOf(0L) }

    LaunchedEffect(trigger) {
        val startTime = withFrameNanos { it }
        val durationNanos = 3_800_000_000L
        while (true) {
            val now = withFrameNanos { it }
            val elapsed = now - startTime
            frameTick = elapsed
            if (elapsed >= durationNanos) break
        }
    }

    val progress = (frameTick.toFloat() / 3_800_000_000L).coerceIn(0f, 1f)
    if (frameTick > 0L && progress < 1f) {
        val alpha = if (progress > 0.75f) {
            ((1f - progress) / 0.25f).coerceIn(0f, 1f)
        } else {
            1f
        }

        androidx.compose.foundation.Canvas(
            modifier = modifier
                .fillMaxSize()
                .zIndex(9999f)
                .graphicsLayer { this.alpha = alpha }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val elapsedSec = frameTick / 1_000_000_000f
            val frames = elapsedSec * 60f
            val gravity = 0.00035f

            particles.forEach { p ->
                val sway = kotlin.math.sin(elapsedSec * p.swaySpeed + p.swayPhase) * 0.0012f * frames
                val curX = p.x + (p.vx * frames) + sway
                val curY = p.y + (p.vy * frames) + (0.5f * gravity * frames * frames)
                val curRot = p.rotation + (p.vRot * frames)

                val px = curX * canvasWidth
                val py = curY * canvasHeight

                rotate(degrees = curRot, pivot = Offset(px, py)) {
                    when (p.shapeType) {
                        1 -> drawCircle(
                            color = p.color,
                            center = Offset(px, py),
                            radius = p.size / 2.2f
                        )
                        2 -> drawRect(
                            color = p.color,
                            topLeft = Offset(px - p.size / 3f, py - p.size * 0.75f),
                            size = androidx.compose.ui.geometry.Size(p.size * 0.6f, p.size * 1.5f)
                        )
                        else -> drawRect(
                            color = p.color,
                            topLeft = Offset(px - p.size / 2f, py - p.size / 3f),
                            size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.65f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: FamilyViewModel,
    pendingOpenTab: String? = null,
    onConsumeOpenTab: () -> Unit = {},
    pendingWidgetAction: String? = null,
    onConsumeWidgetAction: () -> Unit = {},
    pendingChoreId: String? = null,
    onConsumeChoreId: () -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val topNotification by viewModel.topNotification.collectAsStateWithLifecycle()
    val confettiTrigger by viewModel.confettiTrigger.collectAsStateWithLifecycle()
    
    // Bottom tab routing selection
    var currentTab by remember { mutableStateOf("inicio") }
    
    var showConfigDialog by remember { mutableStateOf(false) }
    
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateUrl by remember { mutableStateOf("") }
    var latestVersionCode by remember { mutableIntStateOf(0) }
    var latestVersionName by remember { mutableStateOf("") }
    var updateApkSha256 by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        val info = OtaUpdateManager.checkForUpdates(context)
        if (info.isUpdateAvailable && info.downloadUrl.isNotEmpty()) {
            updateUrl = info.downloadUrl
            latestVersionCode = info.latestVersionCode
            latestVersionName = info.latestVersionName
            updateApkSha256 = info.expectedApkSha256
            showUpdateDialog = true
        }
    }

    // Hoisted camera/gallery launcher state (must be outside AnimatedContent to avoid requestCode overflow)
    val coroutineScopeMain = rememberCoroutineScope()
    var isScanningDirect by remember { mutableStateOf(false) }
    var errorMessageScan by remember { mutableStateOf<String?>(null) }
    var successMessageScan by remember { mutableStateOf<String?>(null) }
    var showConfirmScanDialog by remember { mutableStateOf(false) }
    var scannedAmount by remember { mutableStateOf("") }
    var scannedDesc by remember { mutableStateOf("") }
    var scannedCurrency by remember { mutableStateOf("USD") }
    var scannedCategory by remember { mutableStateOf("") }
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val mainSettings by viewModel.settings.collectAsStateWithLifecycle()

    var scannedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var tempPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var scannedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var saveReceiptPhoto by remember { mutableStateOf(true) }
    var scannedItems by remember { mutableStateOf<List<com.example.api.ParsedReceiptItem>>(emptyList()) }
    var scannedBalanceDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val imagePickerLauncherDirect = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent() ){ uri ->
        if (uri != null) {
            coroutineScopeMain.launch {
                isScanningDirect = true
                errorMessageScan = null
                successMessageScan = null
                val bitmap = uriToBitmap(context, uri)
                if (bitmap != null) {
                    val availableCats = categories.map { it.categoryName }
                    val customKey = mainSettings.find { it.key == "gemini_api_key" }?.value
                    val (parsedTx, errorStr, successMsg) = GeminiParser.parseReceiptImage(bitmap, currentUser, availableCats, customKey)
                    if (parsedTx != null) {
                        val tx = parsedTx.transaction
                        scannedAmount = tx.originalAmount.toString()
                        scannedDesc = tx.description
                        scannedCurrency = tx.currency
                        scannedCategory = if (tx.category in availableCats) tx.category else (availableCats.firstOrNull() ?: "")
                        scannedDate = tx.date
                        scannedBalanceDate = tx.date
                        scannedItems = parsedTx.items
                        scannedBitmap = bitmap
                        showConfirmScanDialog = true
                    } else if (errorStr != null) {
                        errorMessageScan = errorStr
                    }
                } else {
                    errorMessageScan = "No se pudo procesar la imagen seleccionada."
                }
                isScanningDirect = false
            }
        }
    }

    val cameraLauncherDirect = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture() ){ success ->
        val uri = tempPhotoUri
        if (success && uri != null) {
            coroutineScopeMain.launch {
                isScanningDirect = true
                errorMessageScan = null
                successMessageScan = null
                val bitmap = uriToBitmap(context, uri)
                if (bitmap != null) {
                    val availableCats = categories.map { it.categoryName }
                    val customKey = mainSettings.find { it.key == "gemini_api_key" }?.value
                    val (parsedTx, errorStr, successMsg) = GeminiParser.parseReceiptImage(bitmap, currentUser, availableCats, customKey)
                    if (parsedTx != null) {
                        val tx = parsedTx.transaction
                        scannedAmount = tx.originalAmount.toString()
                        scannedDesc = tx.description
                        scannedCurrency = tx.currency
                        scannedCategory = if (tx.category in availableCats) tx.category else (availableCats.firstOrNull() ?: "")
                        scannedDate = tx.date
                        scannedBalanceDate = tx.date
                        scannedItems = parsedTx.items
                        scannedBitmap = bitmap
                        showConfirmScanDialog = true
                    } else if (errorStr != null) {
                        errorMessageScan = errorStr
                    }
                } else {
                    errorMessageScan = "No se pudo cargar la foto tomada."
                }
                isScanningDirect = false
            }
        } else {
            errorMessageScan = "No se tomó ninguna foto o se canceló."
        }
    }

    val launchHighResCamera = {
        try {
            val directory = context.cacheDir
            val file = java.io.File.createTempFile("receipt_photo_", ".jpg", directory).apply {
                createNewFile()
                deleteOnExit()
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                file
            )
            tempPhotoUri = uri
            cameraLauncherDirect.launch(uri)
        } catch (e: Exception) {
            errorMessageScan = "No se pudo preparar la foto: ${e.localizedMessage}"
        }
    }

    val permissionLauncherDirect = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission() ){ isGranted ->
        if (isGranted) {
            launchHighResCamera()
        } else {
            errorMessageScan = "Permiso de cámara denegado."
        }
    }

    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val countryCodeState by viewModel.countryCode.collectAsStateWithLifecycle()
    val primaryCurrencyState by viewModel.primaryCurrency.collectAsStateWithLifecycle()
    val secondaryCurrencyState by viewModel.secondaryCurrency.collectAsStateWithLifecycle()
    val appLanguageState by viewModel.appLanguage.collectAsStateWithLifecycle()
    val familyRoleState by viewModel.familyRole.collectAsStateWithLifecycle()
    val managementModeState by viewModel.managementMode.collectAsStateWithLifecycle()
    val familyActionState by viewModel.familyAction.collectAsStateWithLifecycle()
    val pendingFamilyNameState by viewModel.pendingFamilyName.collectAsStateWithLifecycle()
    val userAvatarIdState by viewModel.userAvatarId.collectAsStateWithLifecycle()
    val hasCompletedInAppTour by viewModel.hasCompletedInAppTour.collectAsStateWithLifecycle()
    val pendingUserAlias by viewModel.pendingUserAlias.collectAsStateWithLifecycle()

    val famiStrings = remember(appLanguageState) {
        java.util.Locale.setDefault(com.example.ui.FamiStrings.localeFor(appLanguageState))
        com.example.ui.FamiStrings.get(appLanguageState)
    }

    LaunchedEffect(appLanguageState) {
        java.util.Locale.setDefault(com.example.ui.FamiStrings.localeFor(appLanguageState))
    }

    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
            showConfigDialog = false
        }
    }

    CompositionLocalProvider(
        com.example.ui.LocalFamiStrings provides famiStrings
    ) {
    if (!isAuthenticated) {
        Box(modifier = Modifier.fillMaxSize()) {
            AuthScreen(viewModel = viewModel)

            // Top banner notification system on login
            topNotification?.let { msg ->
                TopNotificationBanner(
                    message = msg,
                    onDismiss = { viewModel.dismissNotification() }
                )
            }
        }
    } else if (!hasCompletedOnboarding) {
        com.example.ui.OnboardingScreen(
            initialCountryCode = countryCodeState,
            initialPrimaryCurrency = primaryCurrencyState,
            initialSecondaryCurrency = secondaryCurrencyState,
            initialLanguage = appLanguageState,
            initialFamilyRole = familyRoleState,
            initialManagementMode = managementModeState,
            initialFamilyAction = familyActionState,
            initialFamilyName = pendingFamilyNameState,
            initialUserAlias = pendingUserAlias,
            initialAvatarId = userAvatarIdState,
            onLoginExisting = { viewModel.openExistingLoginFromOnboarding() },
            onComplete = { selectedCountry, primaryCurr, secondaryCurr, langCode, role, mgmtMode, famAction, famName, avatarId, incomeAmt, incomeCurr, initWallets, userAlias ->
                viewModel.applyInitialConfiguration(
                    country = selectedCountry,
                    primaryCurr = primaryCurr,
                    secondaryCurr = secondaryCurr,
                    languageCode = langCode,
                    role = role,
                    managementMode = mgmtMode,
                    familyAction = famAction,
                    familyName = famName,
                    avatarId = avatarId,
                    monthlyIncome = incomeAmt,
                    monthlyIncomeCurr = incomeCurr,
                    initialWallets = initWallets,
                    userAlias = userAlias
                )
            }
        )
    } else {
        val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
        val tourRegistry = com.example.ui.rememberTourTargetRegistry()
        val dashboardListState = rememberLazyListState()
        val notifPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                viewModel.syncFcmTokenAndWorker()
            }
        }

        LaunchedEffect(pendingOpenTab, pendingChoreId) {
            if (!pendingOpenTab.isNullOrBlank()) {
                currentTab = pendingOpenTab
                onConsumeOpenTab()
            } else if (!pendingChoreId.isNullOrBlank()) {
                currentTab = "tareas"
            }
        }

        var showWidgetQuickWriteDialog by remember { mutableStateOf(false) }
        val widgetSpeechLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val spokenText = matches?.firstOrNull()?.trim().orEmpty()
                if (spokenText.isNotEmpty()) {
                    viewModel.registerFromNaturalLanguage(spokenText, "")
                    Toast.makeText(context, spokenText, Toast.LENGTH_SHORT).show()
                }
            }
        }

        LaunchedEffect(pendingWidgetAction, isAuthenticated, currentUser) {
            if (!pendingWidgetAction.isNullOrBlank() && isAuthenticated && currentUser.isNotBlank()) {
                currentTab = "inicio"
                when (pendingWidgetAction) {
                    com.example.widget.FamiQuickWidgetProvider.ACTION_WRITE -> {
                        showWidgetQuickWriteDialog = true
                    }
                    com.example.widget.FamiQuickWidgetProvider.ACTION_VOICE -> {
                        val speechLocale = com.example.ui.FamiStrings.speechLocaleFor(appLanguageState)
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLocale)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, famiStrings.voicePrompt)
                        }
                        try {
                            widgetSpeechLauncher.launch(intent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, "Voice dictation unavailable", Toast.LENGTH_SHORT).show()
                        }
                    }
                    com.example.widget.FamiQuickWidgetProvider.ACTION_SCAN -> {
                        val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.CAMERA
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        try {
                            if (hasCameraPermission) {
                                launchHighResCamera()
                            } else {
                                permissionLauncherDirect.launch(android.Manifest.permission.CAMERA)
                            }
                        } catch (e: Exception) {
                            errorMessageScan = "Camera Error: ${e.message}"
                        }
                    }
                    com.example.widget.FamiQuickWidgetProvider.ACTION_GALLERY -> {
                        try {
                            imagePickerLauncherDirect.launch("image/*")
                        } catch (e: Exception) {
                            errorMessageScan = "Gallery Error: ${e.message}"
                        }
                    }
                }
                onConsumeWidgetAction()
            }
        }

        LaunchedEffect(isAuthenticated, currentUser, hasCompletedInAppTour) {
            if (isAuthenticated && currentUser.isNotBlank()) {
                viewModel.syncFcmTokenAndWorker()
                if (hasCompletedInAppTour && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasNotifPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (!hasNotifPerm) {
                        notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }

        CompositionLocalProvider(
            com.example.ui.LocalTourRegistry provides (if (!hasCompletedInAppTour) tourRegistry else null)
        ) {
        Box(modifier = Modifier.fillMaxSize() ){
                        
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    FamilyBottomNavigation(
                        currentTab = currentTab,
                        isDarkTheme = isDarkTheme,
                        appLanguage = appLanguageState
                    ) { currentTab = it }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            bottom = 0.dp
                        ) ){
                val tabOrder = listOf("inicio", "cuentas", "tareas", "compras", "metas")
                // Main content based on routing
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        val initialIndex = tabOrder.indexOf(initialState)
                        val targetIndex = tabOrder.indexOf(targetState)
                        val isForward = targetIndex > initialIndex
                        
                        val enterAnim = if (isForward) {
                            slideInHorizontally(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow) ){ it / 2 } + fadeIn(animationSpec = tween(220))
                        } else {
                            slideInHorizontally(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow) ){ -it / 2 } + fadeIn(animationSpec = tween(220))
                        }
                        
                        val exitAnim = if (isForward) {
                            slideOutHorizontally(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow) ){ -it / 2 } + fadeOut(animationSpec = tween(220))
                        } else {
                            slideOutHorizontally(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow) ){ it / 2 } + fadeOut(animationSpec = tween(220))
                        }
                        
                        enterAnim togetherWith exitAnim
                    },
                    label = "TabTransition"
                ) { tab ->
                    when (tab) {
                        "inicio" -> DashboardTab(
                            viewModel = viewModel,
                            listState = dashboardListState,
                            onOpenConfig = { showConfigDialog = true },
                            isScanningDirect = isScanningDirect,
                            errorMessageScan = errorMessageScan,
                            successMessageScan = successMessageScan,
                            showConfirmScanDialog = showConfirmScanDialog,
                            scannedAmount = scannedAmount,
                            scannedDesc = scannedDesc,
                            scannedCurrency = scannedCurrency,
                            scannedCategory = scannedCategory,
                            scannedDate = scannedDate,
                            onSetIsScanningDirect = { isScanningDirect = it },
                            onSetErrorMessageScan = { errorMessageScan = it },
                            onSetSuccessMessageScan = { successMessageScan = it },
                            onSetShowConfirmScanDialog = { showConfirmScanDialog = it },
                            onSetScannedAmount = { scannedAmount = it },
                            onSetScannedDesc = { scannedDesc = it },
                            onSetScannedCurrency = { scannedCurrency = it },
                            onSetScannedCategory = { scannedCategory = it },
                            onSetScannedDate = { scannedDate = it },
                            scannedBitmap = scannedBitmap,
                            scannedItems = scannedItems,
                            onSetScannedItems = { scannedItems = it },
                            scannedBalanceDate = scannedBalanceDate,
                            onSetScannedBalanceDate = { scannedBalanceDate = it },
                            saveReceiptPhoto = saveReceiptPhoto,
                            onSetSaveReceiptPhoto = { saveReceiptPhoto = it },
                            onLaunchCamera = {
                                val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                try {
                                    if (hasCameraPermission) {
                                        launchHighResCamera()
                                    } else {
                                        permissionLauncherDirect.launch(android.Manifest.permission.CAMERA)
                                    }
                                } catch (e: Exception) {
                                    errorMessageScan = "Camera Error: ${e.message}"
                                }
                            },
                            onLaunchGallery = {
                                try {
                                    imagePickerLauncherDirect.launch("image/*")
                                } catch (e: Exception) {
                                    errorMessageScan = "Gallery Error: ${e.message}"
                                }
                            }
                        )
                        "cuentas" -> AccountsTab(viewModel = viewModel, onOpenConfig = { showConfigDialog = true })
                        "tareas" -> TasksTab(
                            viewModel = viewModel,
                            onOpenConfig = { showConfigDialog = true },
                            pendingChoreId = pendingChoreId,
                            onConsumeChoreId = onConsumeChoreId
                        )
                        "compras" -> ComprasTab(viewModel = viewModel, onOpenConfig = { showConfigDialog = true })
                        "metas" -> MetasTab(viewModel = viewModel, onOpenConfig = { showConfigDialog = true })
                    }
                }
                if (showWidgetQuickWriteDialog) {
                    var quickText by remember { mutableStateOf("") }
                    val walletsList by viewModel.wallets.collectAsStateWithLifecycle()
                    val isRegistering by viewModel.isRegisteringNlp.collectAsStateWithLifecycle()
                    var selectedWalletId by remember { mutableStateOf("") }
                    val paperDialog = FamiTheme.paper
                    androidx.compose.ui.window.Dialog(onDismissRequest = { showWidgetQuickWriteDialog = false }) {
                        com.example.ui.StickyNoteSheet(
                            noteColor = paperDialog.stickyYellow,
                            tapeColor = FamiCoralSoft.copy(alpha = 0.65f),
                            borderColor = paperDialog.border,
                            shadowColor = paperDialog.shadow3D
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
                                        text = famiStrings.quickNoteTitle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = paperDialog.inkMuted
                                    )
                                    IconButton(
                                        onClick = { showWidgetQuickWriteDialog = false },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = famiStrings.close,
                                            tint = paperDialog.ink,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = famiStrings.financesActionMovement,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paperDialog.ink
                                )
                                OutlinedTextField(
                                    value = quickText,
                                    onValueChange = { quickText = it },
                                    placeholder = {
                                        Text(
                                            famiStrings.quickNotePlaceholder,
                                            fontSize = 13.sp,
                                            color = paperDialog.inkMuted
                                        )
                                    },
                                    singleLine = false,
                                    maxLines = 3,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = paperDialog.border,
                                        unfocusedBorderColor = paperDialog.border,
                                        focusedContainerColor = paperDialog.sheet,
                                        unfocusedContainerColor = paperDialog.sheet,
                                        focusedTextColor = paperDialog.ink,
                                        unfocusedTextColor = paperDialog.ink
                                    )
                                )
                                if (walletsList.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        walletsList.forEach { w ->
                                            val isSelected = selectedWalletId == w.id
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    selectedWalletId = if (isSelected) "" else w.id
                                                },
                                                label = {
                                                    Text(
                                                        text = w.name,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                                com.example.ui.Tactile3DButton(
                                    onClick = {
                                        if (quickText.isNotBlank() && !isRegistering) {
                                            viewModel.registerFromNaturalLanguage(quickText.trim(), selectedWalletId)
                                            showWidgetQuickWriteDialog = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    faceColor = FamiCoral,
                                    lipColor = Lip3DCoral,
                                    borderColor = paperDialog.border,
                                    cornerRadius = 12.dp,
                                    lipHeight = 4.dp,
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = famiStrings.save.uppercase(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Global Config Dialog as popup at the top
                if (showConfigDialog) {
                    ConfigDialog(
                        viewModel = viewModel,
                        onDismiss = { showConfigDialog = false },
                        onOpenGuide = {
                            showConfigDialog = false
                            viewModel.reopenInAppTour()
                        }
                    )
                }
                
                if (showUpdateDialog) {
                    val paper = FamiTheme.paper
                    AlertDialog(
                        onDismissRequest = {
                            showUpdateDialog = false
                        },
                        containerColor = paper.sheet,
                        title = {
                            Text(
                                text = if (latestVersionName.isNotBlank()) "Actualización Disponible (v$latestVersionName)"
                                else "Actualización Disponible",
                                fontWeight = FontWeight.Black,
                                color = paper.ink
                            )
                        },
                        text = {
                            Text(
                                "Hay una nueva versión de Fami disponible. ¿Deseas descargarla e instalarla ahora?",
                                color = paper.inkMuted
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (updateUrl.isNotEmpty() && !updateUrl.contains("play.google.com")) {
                                    if (OtaUpdateManager.canInstallPackages(context)) {
                                        showUpdateDialog = false
                                        android.widget.Toast.makeText(context, "Descargando actualización OTA...", android.widget.Toast.LENGTH_LONG).show()
                                        OtaUpdateManager.downloadAndInstallUpdate(context, updateUrl, latestVersionCode, updateApkSha256)
                                    } else {
                                        OtaUpdateManager.downloadAndInstallUpdate(context, updateUrl, latestVersionCode, updateApkSha256)
                                    }
                                } else {
                                    showUpdateDialog = false
                                    try {
                                        val playIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=${context.packageName}"))
                                        context.startActivity(playIntent)
                                    } catch (e: Exception) {
                                        val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                                        context.startActivity(webIntent)
                                    }
                                }
                            }) {
                                Text("Actualizar", fontWeight = FontWeight.Bold, color = FamiCoral)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                if (latestVersionCode > 0) {
                                    OtaUpdateManager.dismissUpdateForVersion(context, latestVersionCode)
                                }
                                showUpdateDialog = false
                            }) {
                                Text("Más tarde", color = paper.inkMuted)
                            }
                        }
                    )
                }

                // Top banner notification system
                topNotification?.let { msg ->
                    TopNotificationBanner(
                        message = msg,
                        onDismiss = { viewModel.dismissNotification() }
                    )
                }
                
                ConfettiEffect(trigger = confettiTrigger, modifier = Modifier.fillMaxSize().zIndex(9999f))
                }
            }

            if (!hasCompletedInAppTour) {
                com.example.ui.InAppGuideOverlay(
                    appLanguage = appLanguageState,
                    onSwitchTab = { target -> currentTab = target },
                    onEnsureDashboardScroll = { idx -> dashboardListState.scrollToItem(idx) },
                    onDismiss = { viewModel.completeInAppTour() }
                )
            }
        }
        }
    }
    }
}

// Barra Inferior de Botones 3D Táctiles sobre Base de Papel Plano (Fami v3.0)
@Composable
fun FamilyBottomNavigation(
    currentTab: String,
    isDarkTheme: Boolean,
    appLanguage: String = "es",
    onTabSelected: (String) -> Unit
) {
    val paper = FamiTheme.paper
    val strings = remember(appLanguage) { com.example.ui.FamiStrings.get(appLanguage) }
    val tabs = listOf(
        Triple("inicio", strings.tabNotebook, if (currentTab == "inicio") Icons.Filled.Home else Icons.Outlined.Home),
        Triple("cuentas", strings.tabAccounts, if (currentTab == "cuentas") Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet),
        Triple("tareas", strings.tabChores, if (currentTab == "tareas") Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank),
        Triple("compras", strings.tabMarket, if (currentTab == "compras") Icons.Filled.ShoppingBag else Icons.Outlined.ShoppingBag),
        Triple("metas", strings.tabGoals, if (currentTab == "metas") Icons.Filled.Savings else Icons.Outlined.Savings)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .padding(bottom = 4.dp, end = 3.dp)
                .drawBehind {
                    drawRoundRect(
                        color = paper.shadow3D,
                        topLeft = Offset(3.dp.toPx(), 4.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx(), 22.dp.toPx())
                    )
                }
                .clip(RoundedCornerShape(22.dp))
                .background(paper.sheet)
                .border(2.dp, paper.border, RoundedCornerShape(22.dp))
                .padding(horizontal = 8.dp, vertical = 7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { (tabId, label, icon) ->
                    val selected = currentTab == tabId
                    com.example.ui.Tactile3DButton(
                        onClick = { onTabSelected(tabId) },
                        modifier = Modifier
                            .weight(1f)
                            .tourTarget("tab_$tabId"),
                        faceColor = if (selected) FamiCoral else paper.sheetCream,
                        lipColor = if (selected) Lip3DCoral else paper.lipPaper,
                        borderColor = paper.border,
                        cornerRadius = 14.dp,
                        lipHeight = if (selected) 4.dp else 3.dp,
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (selected) Color.White else paper.ink,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selected) Color.White else paper.ink,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// Floating notification bar
@Composable
fun BoxScope.TopNotificationBanner(message: String, onDismiss: () -> Unit) {
    val paper = FamiTheme.paper
    // Auto dismiss after 3.5 seconds
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(3500)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(paper.alertBg)
            .border(1.5.dp, AlertRed.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
            .clickable { onDismiss() }
            .padding(16.dp) ){
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alerta",
                tint = if (paper.isDark) FamiCoralLight else AlertRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = if (paper.isDark) paper.ink else AlertRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1.0f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp) ){
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Cerrar",
                    tint = if (paper.isDark) FamiCoralLight else AlertRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


// --- TAB 1: INICIO (DASHBOARD) ---
@Composable
fun MonthSelector(
    selectedMonth: java.util.Calendar,
    onMonthChange: (java.util.Calendar) -> Unit
) {
    val activeLocale = java.util.Locale.getDefault()
    val monthFormat = remember(activeLocale) { java.text.SimpleDateFormat("MMMM yyyy", activeLocale) }
    val monthStr = remember(selectedMonth.timeInMillis, activeLocale) {
        monthFormat.format(selectedMonth.time).replaceFirstChar { it.titlecase(activeLocale) }
    }
    val context = androidx.compose.ui.platform.LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            val prev = selectedMonth.clone() as java.util.Calendar
            prev.add(java.util.Calendar.MONTH, -1)
            onMonthChange(prev)
        }) {
            Icon(androidx.compose.material.icons.Icons.Filled.ArrowBack, contentDescription = "Mes Anterior", tint = MaterialTheme.colorScheme.primary)
        }
        Row(
            modifier = Modifier.clickable {
                android.app.DatePickerDialog(
                    context,
                    { _, year, month, _ ->
                        val newCal = selectedMonth.clone() as java.util.Calendar
                        newCal.set(java.util.Calendar.YEAR, year)
                        newCal.set(java.util.Calendar.MONTH, month)
                        newCal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                        onMonthChange(newCal)
                    },
                    selectedMonth.get(java.util.Calendar.YEAR),
                    selectedMonth.get(java.util.Calendar.MONTH),
                    1
                ).show()
            }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(androidx.compose.material.icons.Icons.Default.DateRange, contentDescription = "Seleccionar Mes", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = monthStr,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = MaterialTheme.colorScheme.onBackground)
        }

        IconButton(onClick = {
            val next = selectedMonth.clone() as java.util.Calendar
            next.add(java.util.Calendar.MONTH, 1)
            onMonthChange(next)
        }) {
            Icon(androidx.compose.material.icons.Icons.Filled.ArrowForward, contentDescription = "Mes Siguiente", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun DashboardTab(
    viewModel: FamilyViewModel,
    listState: LazyListState = rememberLazyListState(),
    onOpenConfig: () -> Unit,
    isScanningDirect: Boolean,
    errorMessageScan: String?,
    successMessageScan: String?,
    showConfirmScanDialog: Boolean,
    scannedAmount: String,
    scannedDesc: String,
    scannedCurrency: String,
    scannedCategory: String,
    scannedDate: Long,
    onSetIsScanningDirect: (Boolean) -> Unit,
    onSetErrorMessageScan: (String?) -> Unit,
    onSetSuccessMessageScan: (String?) -> Unit,
    onSetShowConfirmScanDialog: (Boolean) -> Unit,
    onSetScannedAmount: (String) -> Unit,
    onSetScannedDesc: (String) -> Unit,
    onSetScannedCurrency: (String) -> Unit,
    onSetScannedCategory: (String) -> Unit,
    onSetScannedDate: (Long) -> Unit,
    scannedBitmap: android.graphics.Bitmap?,
    scannedItems: List<com.example.api.ParsedReceiptItem> = emptyList(),
    onSetScannedItems: (List<com.example.api.ParsedReceiptItem>) -> Unit = {},
    scannedBalanceDate: Long = System.currentTimeMillis(),
    onSetScannedBalanceDate: (Long) -> Unit = {},
    saveReceiptPhoto: Boolean,
    onSetSaveReceiptPhoto: (Boolean) -> Unit,
    onLaunchCamera: () -> Unit,
    onLaunchGallery: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val chores by viewModel.chores.collectAsStateWithLifecycle()
    val shoppingItems by viewModel.shoppingItems.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val wallets by viewModel.wallets.collectAsStateWithLifecycle()
    val userAvatarId by viewModel.userAvatarId.collectAsStateWithLifecycle()
    val familyRole by viewModel.familyRole.collectAsStateWithLifecycle()
    val managementMode by viewModel.managementMode.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle()
    val isRegisteringNlp by viewModel.isRegisteringNlp.collectAsStateWithLifecycle()
    val nlpError by viewModel.nlpError.collectAsStateWithLifecycle()

    val primaryCurrencyState by viewModel.primaryCurrency.collectAsStateWithLifecycle()
    val secondaryCurrencyState by viewModel.secondaryCurrency.collectAsStateWithLifecycle()
    val liveExchangeRate by viewModel.liveExchangeRate.collectAsStateWithLifecycle()
    val secondaryExchangeRate by viewModel.secondaryExchangeRate.collectAsStateWithLifecycle()
    val walletRatesToUsd by viewModel.walletRatesToUsd.collectAsStateWithLifecycle()
    val viewInSecondary by viewModel.viewInSecondary.collectAsStateWithLifecycle()
    val selectedCountryCode by viewModel.countryCode.collectAsStateWithLifecycle()
    val savedEmailState by viewModel.savedEmail.collectAsStateWithLifecycle()
    val paper = FamiTheme.paper
    val s = com.example.ui.LocalFamiStrings.current

    var nlpInputText by remember { mutableStateOf("") }
    var selectedNlpWalletId by remember(wallets) { mutableStateOf(wallets.firstOrNull()?.id.orEmpty()) }

    // --- AI & Sincronización State variables ---
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val matches = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()?.trim().orEmpty()
            if (spoken.isNotBlank()) {
                nlpInputText = if (nlpInputText.isBlank()) spoken else "${nlpInputText.trim()} $spoken"
            }
        }
    }
    
    var showEmailSyncDialog by remember { mutableStateOf(false) }
    val recurringPayments by viewModel.recurringPayments.collectAsStateWithLifecycle()
    var showAddRecurringDialog by remember { mutableStateOf(false) }
    var showMarkPaidDialog by remember { mutableStateOf(false) }
    var showPaidDetailsDialog by remember { mutableStateOf(false) }
    var selectedRecurringPayment by remember { mutableStateOf<com.example.database.RecurringPayment?>(null) }
    var recNameInput by remember { mutableStateOf("") }
    var recAmountInput by remember { mutableStateOf("") }
    var recCurrencySelected by remember { mutableStateOf("USD") }
    var recDueDayInput by remember { mutableStateOf("5") }
    var recEmojiInput by remember { mutableStateOf("⚡") }
    // showConfirmScanDialog, isScanningDirect, errorMessageScan, successMessageScan,
    // scannedAmount, scannedDesc, scannedCurrency, scannedCategory are now hoisted to MainScreen
    
    // Email sync flows & simulation
    var isLinkingEmail by remember { mutableStateOf(false) }
    var isEmailLinked by remember { mutableStateOf(false) }
    var isFetchingEmails by remember { mutableStateOf(false) }
    var selectedEmailsToSync by remember { mutableStateOf(setOf<Int>()) }
    val hideBalanceSetting = settings.find { it.key == "hide_balance" }?.value
    var showBalance by remember(hideBalanceSetting) { mutableStateOf(hideBalanceSetting != "true") }

    // imagePickerLauncherDirect, cameraLauncherDirect, launchHighResCamera, permissionLauncher
    // are now hoisted to MainScreen and passed via onLaunchCamera / onLaunchGallery lambdas
    
    // Read live rates & parameters
    val bcvRateStr = settings.find { it.key == "bcv_rate" }?.value ?: ""
    val bcvRate = bcvRateStr.toDoubleOrNull()?.takeIf { it > 0.0 }
        ?: liveExchangeRate.takeIf { it > 0.0 }
        ?: 36.85
    val totalLimitStr = settings.find { it.key == "total_limit_usd" }?.value ?: "1800.0"
    val totalLimit = totalLimitStr.toDoubleOrNull() ?: 1800.0

    // FAMILIA COMPARTIDA: Todos los miembros ven todas las transacciones
    val userTransactions = remember(transactions) {
        transactions.sortedByDescending { if (it.balanceDate > 0) it.balanceDate else it.date }
    }
    val curMonth = selectedMonth.get(java.util.Calendar.MONTH)
    val curYear = selectedMonth.get(java.util.Calendar.YEAR)

    val currentMonthTransactions = remember(userTransactions, curMonth, curYear) {
        val tCal = java.util.Calendar.getInstance()
        userTransactions.filter {
            val filterDate = if (it.balanceDate > 0L) it.balanceDate else it.date
            tCal.timeInMillis = filterDate
            tCal.get(java.util.Calendar.MONTH) == curMonth && tCal.get(java.util.Calendar.YEAR) == curYear
        }
    }

    val totalSpentUsd = remember(currentMonthTransactions) {
        currentMonthTransactions.filter { !it.isIncome && !it.isTransfer }.sumOf { it.amount }
    }
    val totalIncomeUsd = remember(currentMonthTransactions) {
        currentMonthTransactions.filter { it.isIncome && !it.isTransfer }.sumOf { it.amount }
    }
    val baseLimitUsd = totalLimit + totalIncomeUsd // Ingreso base + ingresos extra del mes
    val monthKey = remember(selectedMonth.timeInMillis) {
        java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(selectedMonth.time)
    }
    val presupuestadoUsd = remember(categories, monthKey) {
        categories.sumOf { it.limitsPerMonth[monthKey] ?: it.limitAmount }
    }
    val remainingBudgetUsd = presupuestadoUsd + totalIncomeUsd - totalSpentUsd
    val libreBudgetUsd = baseLimitUsd - presupuestadoUsd
    val totalAvailableForPercentage = presupuestadoUsd + totalIncomeUsd
    val spentPercentage = if (baseLimitUsd > 0) (totalSpentUsd / baseLimitUsd * 100).roundToInt() else 0

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 150.dp) ){
        // App top header (Fami v2.0 Bento Header + Live Country Picker)
        item {
            val selectedCountryCode by viewModel.countryCode.collectAsStateWithLifecycle()
            val viewInSecondary by viewModel.viewInSecondary.collectAsStateWithLifecycle()
            val countryInfo = remember(selectedCountryCode) {
                com.example.currency.CountryCurrencyConfig.getCountryByCode(selectedCountryCode)
            }
            var showCountryPickerModal by remember { mutableStateOf(false) }

            if (showCountryPickerModal) {
                AlertDialog(
                    onDismissRequest = { showCountryPickerModal = false },
                    containerColor = paper.sheet,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌎", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("País y Moneda Oficial", fontSize = 17.sp, fontWeight = FontWeight.Black, color = paper.ink)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Selecciona tu país para ajustar tu moneda local y sincronizar la tasa oficial en tu Bóveda Fami:",
                                fontSize = 11.5.sp,
                                color = paper.inkMuted
                            )
                            com.example.currency.CountryCurrencyConfig.SUPPORTED_COUNTRIES.forEach { c ->
                                val isSel = c.code == countryInfo.code
                                Surface(
                                    onClick = {
                                        viewModel.setCountry(c)
                                        showCountryPickerModal = false
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSel) FamiCoral.copy(alpha = 0.16f) else paper.sheetCream,
                                    border = BorderStroke(1.dp, if (isSel) FamiCoral else paper.border.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(c.flag, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(c.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = paper.ink)
                                                Text("${c.localCurrency} (${c.symbol}) • ${c.rateProvider}", fontSize = 10.5.sp, color = paper.inkMuted)
                                            }
                                        }
                                        if (isSel) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FamiCoral, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCountryPickerModal = false }) {
                            Text("Cerrar", fontWeight = FontWeight.Bold, color = FamiCoral)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. CABECERA PRINCIPAL FAMI (Avatar + Alias abajo + Logo Oficial + Botones 3D)
            com.example.ui.NotebookPaperSheet(
                modifier = Modifier.fillMaxWidth(),
                sheetColor = paper.sheet,
                showRuledLines = false,
                showMarginLine = false,
                showSpiralTop = true
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val customFamilyName = settings.find { it.key == "family_name" }?.value.orEmpty()
                    val cleanHeaderUser = cleanMemberAlias(currentUser, "Miembro")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        com.example.ui.MemberAvatarBadge(
                            memberName = cleanHeaderUser,
                            profiles = profiles,
                            currentUser = currentUser,
                            currentUserAvatarId = userAvatarId,
                            explicitAvatarId = userAvatarId,
                            size = 44.dp,
                            borderWidth = 1.8.dp,
                            showAliasBelow = true,
                            aliasText = cleanHeaderUser,
                            aliasFontSize = 10.sp,
                            aliasMaxWidth = 68.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(
                                    id = if (paper.isDark) R.drawable.ic_fami_logo_white else R.drawable.ic_fami_logo_wide
                                ),
                                contentDescription = "Logo Oficial Fami",
                                modifier = Modifier.height(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when {
                                    customFamilyName.isNotBlank() -> customFamilyName
                                    managementMode == "individual" -> s.homePersonalSpace
                                    else -> s.homeFamilySpace
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = paper.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${if (managementMode == "individual") s.homePersonalBadge else s.homeFamilyBadge} • $familyRole",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = FamiCoralDeep,
                                maxLines = 1
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Botón 3D Selector de País y Moneda
                        com.example.ui.Tactile3DButton(
                            onClick = { showCountryPickerModal = true },
                            faceColor = paper.stickyYellow,
                            lipColor = Lip3DGold,
                            borderColor = paper.border,
                            cornerRadius = 12.dp,
                            lipHeight = 3.5.dp,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(countryInfo.flag, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = countryInfo.localCurrency,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = paper.ink
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Country",
                                tint = paper.ink,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Botón 3D de Ajustes
                        com.example.ui.Tactile3DButton(
                            onClick = onOpenConfig,
                            modifier = Modifier.tourTarget("btn_settings"),
                            faceColor = paper.sheetCream,
                            lipColor = paper.lipPaper,
                            borderColor = paper.border,
                            cornerRadius = 12.dp,
                            lipHeight = 3.5.dp,
                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = s.settingsTitle,
                                tint = paper.ink,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. RESUMEN MENSUAL (Saldo Consolidado de Billeteras en Moneda de Preferencia)
        item {
            val countryInfo = remember(selectedCountryCode) {
                com.example.currency.CountryCurrencyConfig.getCountryByCode(selectedCountryCode)
            }
            val preferredPrimary = primaryCurrencyState.ifBlank { countryInfo.localCurrency }
            val preferredSecondary = secondaryCurrencyState.trim().uppercase()
            val hasSecondary = preferredSecondary.isNotBlank() &&
                preferredSecondary != "NONE" &&
                !preferredSecondary.equals(preferredPrimary, ignoreCase = true)
            val displayCurrency = if (viewInSecondary && hasSecondary) preferredSecondary else preferredPrimary
            val otherCurrency = if (hasSecondary) {
                if (displayCurrency.equals(preferredPrimary, ignoreCase = true)) preferredSecondary else preferredPrimary
            } else null

            val totalWalletsDisplay = viewModel.computeTotalWalletsInCurrency(
                targetCurrency = displayCurrency,
                walletList = wallets,
                primaryCurr = preferredPrimary,
                primaryRateToUsd = bcvRate,
                secondaryCurr = preferredSecondary,
                secondaryRateToUsd = secondaryExchangeRate,
                extraRatesToUsd = walletRatesToUsd
            )
            val totalWalletsOther = otherCurrency?.let { otherCurr ->
                viewModel.computeTotalWalletsInCurrency(
                    targetCurrency = otherCurr,
                    walletList = wallets,
                    primaryCurr = preferredPrimary,
                    primaryRateToUsd = bcvRate,
                    secondaryCurr = preferredSecondary,
                    secondaryRateToUsd = secondaryExchangeRate,
                    extraRatesToUsd = walletRatesToUsd
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            com.example.ui.NotebookPaperSheet(
                modifier = Modifier.fillMaxWidth(),
                sheetColor = paper.sheet,
                showRuledLines = true,
                showMarginLine = true,
                showSpiralTop = false
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tourTarget("balance_sheet")
                    ) {
                    // Cabecera de Resumen, Cambio Rápido de Moneda y Tasa de Referencia
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
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = s.monthSheet,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (showBalance) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle balance",
                                    tint = FamiCoral,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            showBalance = !showBalance
                                            viewModel.updateValueSetting("hide_balance", if (showBalance) "false" else "true")
                                        }
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hasSecondary) {
                                Surface(
                                    onClick = { viewModel.toggleViewInSecondary() },
                                    color = paper.stickyMint,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.2.dp, paper.border)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = "Switch currency",
                                            tint = paper.ink,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (viewInSecondary) preferredPrimary else preferredSecondary,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = paper.ink
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = paper.stickyYellow,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.2.dp, paper.border)
                            ) {
                                Text(
                                    text = "1$ = ${countryInfo.symbol} ${String.format(Locale.US, "%.2f", bcvRate)}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    ModernMonthSelector(selectedMonth = selectedMonth, onMonthChange = { viewModel.updateSelectedMonth(it) })
                    Spacer(modifier = Modifier.height(8.dp))

                    // Bloque Principal: Suma Consolidada de Billeteras
                    val displaySymbol = com.example.currency.CountryCurrencyConfig.getCurrencySymbol(displayCurrency)
                    val primaryDisplayText = if (!showBalance) {
                        "••••••"
                    } else {
                        "$displaySymbol ${String.format(Locale.US, "%,.2f", totalWalletsDisplay)} $displayCurrency"
                    }

                    val secondaryDisplayText = when {
                        wallets.isEmpty() -> s.walletsEmptyHint
                        otherCurrency != null && totalWalletsOther != null -> {
                            val otherSymbol = com.example.currency.CountryCurrencyConfig.getCurrencySymbol(otherCurrency)
                            "${wallets.size} ${s.walletsCountSuffix} • ≈ $otherSymbol ${String.format(Locale.US, "%,.2f", totalWalletsOther)} $otherCurrency"
                        }
                        else -> "${wallets.size} ${s.walletsCountSuffix}"
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${s.availableBalance} ($displayCurrency)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = FamiCoralDeep,
                            letterSpacing = 0.7.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = primaryDisplayText,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink,
                            letterSpacing = (-0.6).sp
                        )
                        if (showBalance) {
                            Text(
                                text = secondaryDisplayText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = paper.inkMuted
                            )
                        }
                    }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3 Fichas de Papel Contable con Borde Entintado
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = paper.stickyMint,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, paper.border)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                Text(s.income, fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = paper.inkMuted)
                                Text(
                                    text = if (showBalance) "$${String.format(Locale.US, "%,.0f", baseLimitUsd)}" else "••••",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = paper.stickyYellow,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, paper.border)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                Text(s.budget, fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = paper.inkMuted)
                                Text(
                                    text = if (showBalance) "$${String.format(Locale.US, "%,.0f", presupuestadoUsd)}" else "••••",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = paper.stickyPeach,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, paper.border)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                Text(s.spent, fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = paper.inkMuted)
                                Text(
                                    text = if (showBalance) "$${String.format(Locale.US, "%,.2f", totalSpentUsd)}" else "••••",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = FamiCoral
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val healthBarColor = when {
                        spentPercentage >= 95 -> FamiAlertRed
                        spentPercentage >= 75 -> FamiGold
                        else -> FamiEmerald
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(paper.sheetCream)
                            .border(1.2.dp, paper.border, RoundedCornerShape(6.dp))
                    ) {
                        val frac = (if (baseLimitUsd > 0) (totalSpentUsd / baseLimitUsd) else 0.0).toFloat().coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(frac)
                                .background(healthBarColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    com.example.ui.RuledNotebookText(
                        text = when {
                            spentPercentage > 100 -> "${s.budgetExceededPrefix} $${String.format(Locale.US, "%,.2f", totalSpentUsd - baseLimitUsd)} USD"
                            spentPercentage >= 85 -> "${s.budgetWarnPrefix} $spentPercentage%"
                            else -> "${s.budgetOkPrefix} $spentPercentage%"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = paper.ink,
                        lineHeight = 24.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 3. NOTA ADHESIVA DEL ASESOR FAMI IA (Escritura, Voz, Envío y Escáner/Galería abajo)
        item {
            com.example.ui.StickyNoteSheet(
                modifier = Modifier.fillMaxWidth(),
                noteColor = paper.stickyYellow
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(paper.sheet.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.fami_sticker_idea),
                                    contentDescription = "Plan Fami IA",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                 Text(
                                     text = s.quickNoteTitle,
                                     fontSize = 12.sp,
                                     fontWeight = FontWeight.Black,
                                     color = paper.ink
                                 )
                                 Text(
                                     text = s.quickNoteSubtitle,
                                     fontSize = 11.sp,
                                     fontWeight = FontWeight.SemiBold,
                                     color = paper.inkMuted
                                 )
                            }
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
                                val isSelected = selectedNlpWalletId == w.id
                                BankWalletSelectorChip(
                                    walletName = w.name,
                                    currency = w.currency,
                                    selected = isSelected,
                                    onClick = { selectedNlpWalletId = w.id }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tourTarget("quick_note_actions")
                    ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (nlpInputText.isNotBlank() && !isRegisteringNlp) {
                                        viewModel.registerFromNaturalLanguage(nlpInputText, selectedNlpWalletId)
                                        nlpInputText = ""
                                    }
                                }
                            ),
                            value = nlpInputText,
                            onValueChange = { nlpInputText = it },
                            placeholder = { Text(s.quickNotePlaceholder, fontSize = 12.5.sp, color = paper.inkMuted) },
                            modifier = Modifier
                                .weight(1f)
                                .background(paper.sheet, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            trailingIcon = {
                                if (nlpInputText.isNotEmpty() && !isRegisteringNlp) {
                                    IconButton(onClick = { nlpInputText = "" }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Clear, contentDescription = s.clear, tint = paper.ink, modifier = Modifier.size(16.dp))
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

                        com.example.ui.Tactile3DButton(
                            onClick = {
                                val speechLocale = com.example.ui.FamiStrings.speechLocaleFor(appLanguage)
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLocale)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, s.voicePrompt)
                                }
                                try {
                                    speechLauncher.launch(intent)
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(context, "Voice dictation unavailable", Toast.LENGTH_SHORT).show()
                                }
                            },
                            faceColor = paper.stickyMint,
                            lipColor = Lip3DMint,
                            borderColor = paper.border,
                            cornerRadius = 13.dp,
                            lipHeight = 3.5.dp,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = s.voicePrompt,
                                tint = paper.ink,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        com.example.ui.Tactile3DButton(
                            onClick = {
                                if (nlpInputText.isNotBlank() && !isRegisteringNlp) {
                                    viewModel.registerFromNaturalLanguage(nlpInputText, selectedNlpWalletId)
                                    nlpInputText = ""
                                }
                            },
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
                                    imageVector = Icons.Default.Send,
                                    contentDescription = s.save,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (nlpError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = nlpError ?: "",
                            color = FamiAlertRed,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.Tactile3DButton(
                            onClick = { onLaunchCamera() },
                            modifier = Modifier.weight(1f),
                            faceColor = FamiCoral,
                            lipColor = Lip3DCoral,
                            borderColor = paper.border,
                            cornerRadius = 12.dp,
                            lipHeight = 4.dp,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = s.scanBtn, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(s.scanBtn, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
                        }

                        com.example.ui.Tactile3DButton(
                            onClick = { onLaunchGallery() },
                            modifier = Modifier.weight(1f),
                            faceColor = if (paper.isDark) Color(0xFF233269) else FamiMidnight,
                            lipColor = paper.lipNavy,
                            borderColor = paper.border,
                            cornerRadius = 12.dp,
                            lipHeight = 4.dp,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = s.galleryBtn, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(s.galleryBtn, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
                        }
                    }
                    }

                    if (isScanningDirect) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = paper.sheet,
                            border = BorderStroke(1.5.dp, paper.border),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = FamiCoral)
                                Text(
                                    text = s.scanningReceipt,
                                    fontSize = 11.sp,
                                    color = paper.ink,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (errorMessageScan != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = paper.alertBg,
                            border = BorderStroke(1.5.dp, paper.border),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, "Error", tint = FamiAlertRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = errorMessageScan!!, fontSize = 11.sp, color = paper.ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onSetErrorMessageScan(null) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = s.close, tint = paper.ink, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    if (successMessageScan != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = paper.stickyMint,
                            border = BorderStroke(1.5.dp, paper.border),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, "OK", tint = FamiEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = successMessageScan!!, fontSize = 11.sp, color = paper.ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onSetSuccessMessageScan(null) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = s.close, tint = paper.ink, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 5. RECIBOS FIJOS CON CLIP EN EL CUADERNO
        item {
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
                onEditPaymentClick = { rec ->
                    selectedRecurringPayment = rec
                    recNameInput = rec.name
                    recAmountInput = if (rec.defaultAmount > 0.0) rec.defaultAmount.toString() else ""
                    recCurrencySelected = rec.currency
                    recDueDayInput = rec.dueDay.toString()
                    recEmojiInput = rec.iconEmoji.ifBlank { "⚡" }
                    showAddRecurringDialog = true
                },
                onDeletePaymentClick = { rec ->
                    viewModel.deleteRecurringPayment(rec)
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // CALENDARIO FÍSICO ESTILO PAPEL CON ESPIRALES Y ANIMACIÓN DE PASO DE HOJA
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .tourTarget("calendar_section")
            ) {
                com.example.ui.PhysicalPaperCalendar(
                    selectedMonth = selectedMonth,
                    transactions = userTransactions,
                    chores = chores,
                    categories = categories,
                    activityLogs = activityLogs,
                    appLanguage = appLanguage,
                    isSameDay = { ts, cal -> isSameDay(ts, cal) },
                    toColor = { hex -> hex.toColor() },
                    getMonthName = { m -> com.example.ui.FamiStrings.monthName(m, appLanguage) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }


    }

    // --- DIÁLOGOS DE PAGOS RECURRENTES Y ESCÁNER IA (FUERA DE LAZYCOLUMN PARA EVITAR DESCOMPOSICIÓN AL HACER SCROLL) ---
    com.example.ui.ModernRecurringPaymentDialogs(
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
        localCurrency = primaryCurrencyState.ifBlank { "VES" }
    )

    if (showConfirmScanDialog) {
        com.example.ui.ReceiptScanConfirmDialog(
            scannedDesc = scannedDesc,
            scannedAmount = scannedAmount,
            scannedCurrency = scannedCurrency,
            scannedCategory = scannedCategory,
            scannedDate = scannedDate,
            scannedBalanceDate = scannedBalanceDate,
            scannedItems = scannedItems,
            saveReceiptPhoto = saveReceiptPhoto,
            primaryCurrency = primaryCurrencyState,
            categories = categories,
            wallets = wallets,
            onSetScannedDesc = onSetScannedDesc,
            onSetScannedAmount = onSetScannedAmount,
            onSetScannedCurrency = onSetScannedCurrency,
            onSetScannedCategory = onSetScannedCategory,
            onSetScannedDate = onSetScannedDate,
            onSetScannedBalanceDate = onSetScannedBalanceDate,
            onSetSaveReceiptPhoto = onSetSaveReceiptPhoto,
            onDismiss = { onSetShowConfirmScanDialog(false) },
            onConfirm = { chosenWalletId ->
                val amt = scannedAmount.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (amt > 0.0 && scannedDesc.isNotBlank()) {
                    coroutineScope.launch {
                        var receiptUrl = ""
                        if (saveReceiptPhoto && scannedBitmap != null) {
                            receiptUrl = viewModel.uploadReceiptImage(scannedBitmap) ?: ""
                        }
                        val txItems = scannedItems.map { item ->
                            val catName = if (categories.any { c -> c.categoryName == item.category }) item.category
                            else scannedCategory.ifEmpty { categories.firstOrNull()?.categoryName ?: "General" }
                            com.example.database.TransactionItem(name = item.name, amount = item.amount, category = catName)
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
                        )
                        val msg = if (txItems.isNotEmpty()) {
                            "Recibo registrado ($amt $scannedCurrency) con ${txItems.size} ítems en $scannedDesc"
                        } else {
                            "Gasto registrado: $amt $scannedCurrency en $scannedDesc"
                        }
                        onSetSuccessMessageScan(msg)
                        onSetShowConfirmScanDialog(false)
                    }
                } else {
                    onSetErrorMessageScan("Por favor ingrese un monto y descripción válidos.")
                    onSetShowConfirmScanDialog(false)
                }
            }
        )
    }

    if (showEmailSyncDialog) {
        var simMailInput by remember { mutableStateOf(savedEmailState) }
        val detectedEmailsList = listOf(
            Triple("PedidosYa - Delivery Cena 🍔", 14.50, "USD"),
            Triple("Netflix - Suscripción Familiar 📺", 7.99, "USD"),
            Triple("Farmatodo Sabana Grande 💊", 850.00, "VES"),
            Triple("MercadoLibre Venezuela 📦", 1200.00, "VES")
        )

        AlertDialog(
            onDismissRequest = { showEmailSyncDialog = false },
            containerColor = paper.sheet,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email Sync",
                        tint = FamiCoral,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Vincular Correo Electrónico ✉️",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = paper.ink
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState() )){

                    if (!isEmailLinked) {
                        Text(
                            "Vincule su correo para que la IA escanee su bandeja en búsqueda de recibos digitales de compras y suscripciones de manera automática.",
                            fontSize = 11.5.sp,
                            color = paper.inkMuted,
                            lineHeight = 15.sp
                        )

                        OutlinedTextField(
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            value = simMailInput,
                            onValueChange = { simMailInput = it },
                            label = { Text("Correo Electrónico", fontSize = 11.sp) },
                            placeholder = { Text("tu.correo@gmail.com", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = paper.ink),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FamiCoral,
                                unfocusedBorderColor = paper.border.copy(alpha = 0.5f),
                                focusedTextColor = paper.ink,
                                unfocusedTextColor = paper.ink
                            )
                        )

                        if (isLinkingEmail) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = FamiCoral)
                                Text("Solicitando autorización de lectura segura OAUTH2...", fontSize = 10.sp, color = paper.inkMuted)
                            }
                        } else {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isLinkingEmail = true
                                        kotlinx.coroutines.delay(1200)
                                        isEmailLinked = true
                                        isLinkingEmail = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FamiCoral)
                            ) {
                                Text("Otorgar Permiso de Correo e IA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(paper.stickyMint)
                                .border(1.dp, FamiEmerald.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, "Linked", tint = FamiEmerald, modifier = Modifier.size(16.dp))
                            Text("Bandeja vinculada: ${simMailInput.ifBlank { "Correo conectado" }}", fontSize = 10.5.sp, color = paper.ink, fontWeight = FontWeight.SemiBold)
                        }

                        if (isFetchingEmails) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = FamiCoral)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Buscando facturas con IA de Gemini...", fontSize = 11.sp, color = paper.inkMuted)
                            }
                        } else if (selectedEmailsToSync.isEmpty() && !isFetchingEmails) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isFetchingEmails = true
                                        kotlinx.coroutines.delay(1000)
                                        selectedEmailsToSync = setOf(0, 1)
                                        isFetchingEmails = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = paper.ink)
                            ) {
                                Text("Buscar nuevos recibos en bandeja", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = paper.sheet)
                            }
                        }

                        if (selectedEmailsToSync.isNotEmpty() ){
                            Text("Recibos detectados por IA en tus correos:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FamiCoral)
                            
                            detectedEmailsList.forEachIndexed { index, item ->
                                val isChecked = selectedEmailsToSync.contains(index)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .scaleClickable { selectedEmailsToSync = if (isChecked) {
                                                selectedEmailsToSync - index
                                            } else {
                                                selectedEmailsToSync + index
                                            }
                                        }
                                        .background(if (isChecked) FamiCoral.copy(alpha = 0.08f) else Color.Transparent)
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedEmailsToSync = if (checked == true) {
                                                selectedEmailsToSync + index
                                            } else {
                                                selectedEmailsToSync - index
                                            }
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f) ){
                                        Text(item.first, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = paper.ink)
                                        Text("Monto detectado: ${item.second} ${item.third}", fontSize = 9.5.sp, color = paper.inkMuted)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    var count = 0
                                    selectedEmailsToSync.forEach { idx ->
                                        val entry = detectedEmailsList[idx]
                                        viewModel.addTransaction(
                                            originalAmount = entry.second,
                                            category = "Servicios",
                                            description = entry.first,
                                            currency = entry.third,
                                            isIncome = false
                                        )
                                        count++
                                    }
                                    onSetSuccessMessageScan("Sincronizados con éxito: $count gastos nuevos registrados desde Correo.")
                                    selectedEmailsToSync = emptySet()
                                    showEmailSyncDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FamiCoral)
                            ) {
                                Text("Sincronizar e importar marcados (${selectedEmailsToSync.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        TextButton(
                            onClick = {
                                isEmailLinked = false
                                selectedEmailsToSync = emptySet()
                            },
                            modifier = Modifier.fillMaxWidth() ){
                            Text("Desvincular Correo", color = AlertRed, fontSize = 10.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showEmailSyncDialog = false }) {
                    Text("Cerrar", color = paper.inkMuted, fontSize = 11.sp)
                }
            }
        )
    }
}

// Helpers for calendar tracking
fun getTodayMidnight(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = 
    androidx.compose.foundation.BorderStroke(width, color)

fun isSameDay(timestampMillis: Long, targetCal: Calendar): Boolean {
    val txCal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
    return txCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
           txCal.get(Calendar.MONTH) == targetCal.get(Calendar.MONTH) &&
           txCal.get(Calendar.DAY_OF_MONTH) == targetCal.get(Calendar.DAY_OF_MONTH)
}

fun getMonthNameSpanish(monthIndex: Int): String {
    return when (monthIndex) {
        Calendar.JANUARY -> "Enero"
        Calendar.FEBRUARY -> "Febrero"
        Calendar.MARCH -> "Marzo"
        Calendar.APRIL -> "Abril"
        Calendar.MAY -> "Mayo"
        Calendar.JUNE -> "Junio"
        Calendar.JULY -> "Julio"
        Calendar.AUGUST -> "Agosto"
        Calendar.SEPTEMBER -> "Septiembre"
        Calendar.OCTOBER -> "Octubre"
        Calendar.NOVEMBER -> "Noviembre"
        Calendar.DECEMBER -> "Diciembre"
        else -> ""
    }
}



// --- DIALOGO DE ANALISTA FINANCIERO CON IA ---
@Composable
fun FinanceAiAnalysisDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    isLoading: Boolean,
    report: FinanceAiReport?,
    errorMessage: String?,
    onRefresh: () -> Unit
) {
    if (!isOpen) return

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6366F1).copy(alpha = 0.6f),
                            Color(0xFF06B6D4).copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF4F46E5), Color(0xFF06B6D4))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Analista Financiero IA",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF6366F1).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "INTELIGENTE",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6366F1),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Diagnóstico en tiempo real de tus gastos",
                                fontSize = 11.sp,
                                color = SubtitleText
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(46.dp),
                                color = Color(0xFF6366F1),
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Analizando consumos y facturas...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Identificando en qué gastas más y oportunidades de ahorro",
                                fontSize = 11.sp,
                                color = SubtitleText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚠️", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            fontSize = 12.sp,
                            color = AlertRed,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reintentar Análisis")
                        }
                    }
                } else if (report != null) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // HERO CARD: EN QUÉ GASTAS MÁS
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF3730A3),
                                                Color(0xFF4F46E5),
                                                Color(0xFF1E1B4B)
                                            )
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "DONDE SE VA MÁS DINERO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White.copy(alpha = 0.8f),
                                            letterSpacing = 1.sp
                                        )
                                        val healthColor = when (report.budgetHealth) {
                                            "Saludable" -> Color(0xFF4ADE80)
                                            "Precaución" -> Color(0xFFFBBF24)
                                            else -> Color(0xFFF87171)
                                        }
                                        Surface(
                                            color = healthColor.copy(alpha = 0.20f),
                                            border = BorderStroke(1.dp, healthColor.copy(alpha = 0.6f)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = report.budgetHealth.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = healthColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = report.topSpendingCategory,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${String.format("%.2f", report.topSpendingAmountUsd)} USD consumidos (${report.topSpendingPercentage}% del gasto total)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        // RESUMEN EJECUTIVO
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📝", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Diagnóstico del Mes",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = report.summary,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }

                        // PUNTOS CLAVE DE CONSUMO
                        if (report.topExpensesSummary.isNotEmpty()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("🎯", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Concentración de Gastos",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        report.topExpensesSummary.forEach { point ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 3.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text("•", color = Color(0xFF6366F1), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(point, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // FUGAS DE DINERO Y GASTOS HORMIGA
                        if (report.moneyLeaks.isNotEmpty()) {
                            item {
                                Surface(
                                    color = Color(0xFFEF4444).copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("⚠️", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Fugas de Dinero / Alertas",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFDC2626)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        report.moneyLeaks.forEach { leak ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 3.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text("🔍", fontSize = 11.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(leak, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // CONSEJOS DE AHORRO ACCIONABLES
                        if (report.savingsTips.isNotEmpty()) {
                            item {
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("💡", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Consejos de Ahorro para tu Hogar",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF059669)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        report.savingsTips.forEach { tip ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 3.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text("✓", color = Color(0xFF059669), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(tip, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRefresh,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Actualizar", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Text("Entendido", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 2: CUENTAS, CATEGORÍAS & GASTOS (ARQUITECTURA FINTECH MODULAR) ---
@Composable
fun AccountsTab(viewModel: FamilyViewModel, onOpenConfig: () -> Unit) {
    com.example.ui.ModernAccountsTab(viewModel = viewModel, onOpenConfig = onOpenConfig)
}

// --- BRAND IDENTITY LOGO COMPOSE CANVAS ---
@Composable
fun FamiLogo(modifier: Modifier = Modifier) {
    val paper = FamiTheme.paper
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(
            id = if (paper.isDark) R.drawable.ic_fami_logo_white else R.drawable.ic_fami_logo_wide
        ),
        contentDescription = "Fami Logo Oficial",
        modifier = modifier
    )
}



@Composable
fun DeleteIconButton(
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 16.dp,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.error,
    onDeleteConfirm: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val paper = FamiTheme.paper
    
    IconButton(onClick = { showDialog = true }, modifier = modifier) {
        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = tint, modifier = Modifier.size(iconSize))
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = paper.sheet,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Confirmar eliminación", fontWeight = FontWeight.Black, color = paper.ink) },
            text = { Text("¿Estás seguro de que deseas eliminar este elemento?", color = paper.inkMuted) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteConfirm()
                    showDialog = false
                }) {
                    Text("Eliminar", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar", color = paper.inkMuted)
                }
            }
        )
    }
}

fun cleanMemberAlias(raw: String, fallback: String = "Miembro"): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return fallback
    if (trimmed.contains("@")) {
        val local = trimmed.substringBefore("@")
            .replace(".", " ")
            .replace("_", " ")
            .replace("-", " ")
            .trim()
        return local.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString() }
            }
            .ifBlank { fallback }
    }
    return trimmed
}
