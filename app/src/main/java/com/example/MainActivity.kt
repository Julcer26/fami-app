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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.composed
import androidx.compose.foundation.LocalIndication

class MainActivity : FragmentActivity() {
    var onImagePicked: ((Uri?) -> Unit)? = null
    var onPhotoTaken: ((Boolean) -> Unit)? = null
    var onPermissionResult: ((Boolean) -> Unit)? = null

    val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent() ){ uri ->
        onImagePicked?.invoke(uri)
    }

    val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture() ){ success ->
        onPhotoTaken?.invoke(success)
    }

    val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission() ){ isGranted ->
        onPermissionResult?.invoke(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val viewModel: FamilyViewModel = viewModel(factory = FamilyViewModelFactory(context.applicationContext))
            val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
            val isDarkThemeSetting by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkThemeSetting) {
                MainScreen(viewModel)
            }
        }
    }
}

// Utility extension for hex color support

// --- Modern Design Utilities ---
fun Modifier.scaleClickable(
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit
): Modifier = composed {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by actualInteractionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    this.graphicsLayer {
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
fun MainScreen(viewModel: FamilyViewModel) {
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
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        val info = OtaUpdateManager.checkForUpdates(context)
        if (info.isUpdateAvailable && info.downloadUrl.isNotEmpty()) {
            updateUrl = info.downloadUrl
            latestVersionCode = info.latestVersionCode
            latestVersionName = info.latestVersionName
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

    LaunchedEffect(appLanguageState) {
        if (appLanguageState.isNotBlank()) {
            java.util.Locale.setDefault(java.util.Locale(appLanguageState))
        }
    }

    if (!hasCompletedOnboarding) {
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
    } else if (!isAuthenticated) {
        Box(modifier = Modifier.fillMaxSize() ){
            AuthScreen(viewModel = viewModel)
            
            // Top banner notification system on login
            topNotification?.let { msg ->
                TopNotificationBanner(
                    message = msg,
                    onDismiss = { viewModel.dismissNotification() }
                )
            }
        }
    } else {
        val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
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
                        "tareas" -> TasksTab(viewModel = viewModel, onOpenConfig = { showConfigDialog = true })
                        "compras" -> ComprasTab(viewModel = viewModel, onOpenConfig = { showConfigDialog = true })
                        "metas" -> MetasTab(viewModel = viewModel, onOpenConfig = { showConfigDialog = true })
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
                                        OtaUpdateManager.downloadAndInstallUpdate(context, updateUrl, latestVersionCode)
                                    } else {
                                        OtaUpdateManager.downloadAndInstallUpdate(context, updateUrl, latestVersionCode)
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
                    onDismiss = { viewModel.completeInAppTour() }
                )
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
        Triple("inicio", strings.tabNotebook, if (currentTab == "inicio") Icons.Filled.MenuBook else Icons.Outlined.MenuBook),
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
                        modifier = Modifier.weight(1f),
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
            .background(AlertRedBg)
            .border(1.dp, AlertRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable { onDismiss() }
            .padding(16.dp) ){
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alerta",
                tint = AlertRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = AlertRed,
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
                    tint = AlertRed,
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
    val monthFormat = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("es", "ES"))
    val monthStr = monthFormat.format(selectedMonth.time).replaceFirstChar { it.uppercase() }
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
    val userTransactions = transactions.sortedByDescending { if (it.balanceDate > 0) it.balanceDate else it.date }
    val curMonth = selectedMonth.get(java.util.Calendar.MONTH)
    val curYear = selectedMonth.get(java.util.Calendar.YEAR)
    
    val currentMonthTransactions = userTransactions.filter {
        val tCal = java.util.Calendar.getInstance()
        val filterDate = if (it.balanceDate > 0L) it.balanceDate else it.date
        tCal.timeInMillis = filterDate
        tCal.get(java.util.Calendar.MONTH) == curMonth && tCal.get(java.util.Calendar.YEAR) == curYear
    }

    val totalSpentUsd = currentMonthTransactions.filter { !it.isIncome && !it.isTransfer }.sumOf { it.amount }
    val totalIncomeUsd = currentMonthTransactions.filter { it.isIncome && !it.isTransfer }.sumOf { it.amount }
    val baseLimitUsd = totalLimit + totalIncomeUsd // Ingreso base + ingresos extra del mes
    val monthKey = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(selectedMonth.time)
    val presupuestadoUsd = categories.sumOf { it.limitsPerMonth[monthKey] ?: it.limitAmount } // Suma de categorias
    val remainingBudgetUsd = presupuestadoUsd + totalIncomeUsd - totalSpentUsd
    val libreBudgetUsd = baseLimitUsd - presupuestadoUsd
    val totalAvailableForPercentage = presupuestadoUsd + totalIncomeUsd
    val spentPercentage = if (baseLimitUsd > 0) (totalSpentUsd / baseLimitUsd * 100).roundToInt() else 0

    LazyColumn(
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

            // 1. PORTADA SUPERIOR DEL CUADERNO FAMI (Anillos + Logo Oficial + Botones 3D)
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
                    val avatarOpt = remember(userAvatarId) { com.example.ui.AvatarCatalog.getById(userAvatarId) }
                    val customFamilyName = settings.find { it.key == "family_name" }?.value.orEmpty()
                    val cleanHeaderUser = cleanMemberAlias(currentUser, "")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = avatarOpt.drawableRes),
                            contentDescription = avatarOpt.labelEs,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(paper.sheetCream)
                                .border(1.8.dp, paper.border, CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_fami_logo_wide),
                                contentDescription = "Logo Oficial Fami",
                                modifier = Modifier.height(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when {
                                    customFamilyName.isNotBlank() -> customFamilyName
                                    cleanHeaderUser.isNotBlank() -> "Cuaderno de $cleanHeaderUser"
                                    else -> "Cuaderno Familiar Fami"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = paper.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${if (managementMode == "individual") "👤 Personal" else "🏡 Familiar"} • $familyRole",
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
                                contentDescription = "Cambiar país",
                                tint = paper.ink,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Botón 3D de Ajustes
                        com.example.ui.Tactile3DButton(
                            onClick = onOpenConfig,
                            faceColor = paper.sheetCream,
                            lipColor = paper.lipPaper,
                            borderColor = paper.border,
                            cornerRadius = 12.dp,
                            lipHeight = 3.5.dp,
                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ajustes",
                                tint = paper.ink,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. HOJA MAYOR DE CONTABILIDAD FAMILIAR (Saldo Consolidado de Billeteras en Moneda de Preferencia)
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
                showRuledLines = false,
                showMarginLine = true,
                showSpiralTop = false
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)
                ) {
                    // Cabecera de Hoja, Cambio Rápido de Moneda y Tasa de Referencia
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
                                    text = "📓 HOJA DEL MES",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (showBalance) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Ocultar",
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
                                            contentDescription = "Cambiar moneda",
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

                    // Bloque Principal: Suma Consolidada de Billeteras + Ilustración Nano Banana
                    val displaySymbol = com.example.currency.CountryCurrencyConfig.getCurrencySymbol(displayCurrency)
                    val primaryDisplayText = if (!showBalance) {
                        "••••••"
                    } else {
                        "$displaySymbol ${String.format(Locale.US, "%,.2f", totalWalletsDisplay)} $displayCurrency"
                    }

                    val secondaryDisplayText = when {
                        wallets.isEmpty() -> "Agrega tus billeteras en Cuentas para reflejar tu saldo"
                        otherCurrency != null && totalWalletsOther != null -> {
                            val otherSymbol = com.example.currency.CountryCurrencyConfig.getCurrencySymbol(otherCurrency)
                            "Suma de ${wallets.size} billetera(s) • ≈ $otherSymbol ${String.format(Locale.US, "%,.2f", totalWalletsOther)} $otherCurrency"
                        }
                        else -> "Suma total de ${wallets.size} billetera(s)"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SALDO DISPONIBLE ($displayCurrency)",
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

                        Spacer(modifier = Modifier.width(8.dp))
                        // Sticker Polaroid de la Mascota Fami con su Cuaderno y Alcancía (Nano Banana)
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(paper.sheetCream)
                                .border(1.8.dp, paper.border, RoundedCornerShape(16.dp))
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.fami_notebook_hero),
                                contentDescription = "Fami Guía Financiera",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(13.dp))
                            )
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
                                Text("INGRESOS", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = paper.inkMuted)
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
                                Text("PRESUPUESTO", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = paper.inkMuted)
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
                                Text("GASTADO", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = paper.inkMuted)
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
                            spentPercentage > 100 -> "⚠️ Excediste la hoja mensual por $${String.format(Locale.US, "%,.2f", totalSpentUsd - baseLimitUsd)} USD"
                            spentPercentage >= 85 -> "📌 Atención Fami: Has ejecutado el $spentPercentage% de tu hoja mensual"
                            else -> "✅ Cuaderno al día: $spentPercentage% del presupuesto utilizado"
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
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.fami_sticker_idea),
                                contentDescription = "Plan Fami IA",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.5.dp, paper.border, RoundedCornerShape(14.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "NOTA RÁPIDA CON FAMI IA ✏️",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                                Text(
                                    text = "Escribe o dicta tu gasto y Fami lo clasifica solo",
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(wallets, key = { it.id.ifBlank { it.name } }) { w ->
                                val isSelected = selectedNlpWalletId == w.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedNlpWalletId = w.id },
                                    label = { Text("👛 ${w.name} (${w.currency})", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                            placeholder = { Text("Escribe o dicta tu gasto...", fontSize = 12.5.sp, color = paper.inkMuted) },
                            modifier = Modifier
                                .weight(1f)
                                .background(paper.sheet, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            trailingIcon = {
                                if (nlpInputText.isNotEmpty() && !isRegisteringNlp) {
                                    IconButton(onClick = { nlpInputText = "" }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = paper.ink, modifier = Modifier.size(16.dp))
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
                                contentDescription = "Dictar gasto por voz",
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
                                    contentDescription = "Enviar gasto",
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
                            Icon(Icons.Default.CameraAlt, contentDescription = "Escanear", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ESCANEAR", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
                        }

                        com.example.ui.Tactile3DButton(
                            onClick = { onLaunchGallery() },
                            modifier = Modifier.weight(1f),
                            faceColor = FamiMidnight,
                            lipColor = paper.lipNavy,
                            borderColor = paper.border,
                            cornerRadius = 12.dp,
                            lipHeight = 4.dp,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Galería", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GALERÍA", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
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
                                    text = "Fami IA leyendo el recibo y anotando en el cuaderno...",
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
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = paper.ink, modifier = Modifier.size(14.dp))
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
                                Icon(Icons.Default.CheckCircle, "Éxito", tint = FamiEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = successMessageScan!!, fontSize = 11.sp, color = paper.ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onSetSuccessMessageScan(null) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = paper.ink, modifier = Modifier.size(14.dp))
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
                onSeedDefaultPayments = {
                    viewModel.createOrEditRecurringPayment(com.example.database.RecurringPayment(name = "Condominio", defaultAmount = 0.0, category = "Servicios", iconEmoji = "🏢", dueDay = 5))
                    viewModel.createOrEditRecurringPayment(com.example.database.RecurringPayment(name = "Seguro Médico / Familiar", defaultAmount = 0.0, category = "Servicios", iconEmoji = "🛡️", dueDay = 10))
                    viewModel.createOrEditRecurringPayment(com.example.database.RecurringPayment(name = "Servicio Eléctrico (Luz)", defaultAmount = 0.0, category = "Servicios", iconEmoji = "⚡", dueDay = 15))
                    viewModel.createOrEditRecurringPayment(com.example.database.RecurringPayment(name = "Internet / WiFi", defaultAmount = 0.0, category = "Servicios", iconEmoji = "🌐", dueDay = 20))
                    viewModel.createOrEditRecurringPayment(com.example.database.RecurringPayment(name = "Seguro Vehicular", defaultAmount = 0.0, category = "Servicios", iconEmoji = "🚗", dueDay = 25))
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // CALENDARIO FÍSICO ESTILO PAPEL CON ESPIRALES Y ANIMACIÓN DE PASO DE HOJA
        item {
            com.example.ui.PhysicalPaperCalendar(
                selectedMonth = selectedMonth,
                transactions = userTransactions,
                chores = chores,
                categories = categories,
                activityLogs = activityLogs,
                appLanguage = appLanguage,
                isSameDay = { ts, cal -> isSameDay(ts, cal) },
                toColor = { hex -> hex.toColor() },
                getMonthName = { m -> getMonthNameSpanish(m) }
            )
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

// --- TAB 3: TAREAS CON PRIORIDADES ---
@Composable
fun TasksTab(viewModel: FamilyViewModel, onOpenConfig: () -> Unit) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userAvatarIdState by viewModel.userAvatarId.collectAsStateWithLifecycle()
    val chores by viewModel.chores.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val paper = FamiTheme.paper

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

    val userChores = chores.filter { !it.isPrivate || it.createdBy == currentUser || it.assignedTo == currentUser }
    val pendingChores = userChores.filter { !it.isCompleted }
    val completedChores = userChores.filter { it.isCompleted }
    val shownChores = (if (activeTaskFilter == "pendientes") pendingChores else completedChores).filter {
        (selectedListName.equals("Todas", ignoreCase = true) || it.category.equals(selectedListName, ignoreCase = true)) &&
        (selectedPriorityFilter.equals("Todas", ignoreCase = true) || it.priority.equals(selectedPriorityFilter, ignoreCase = true))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 150.dp) ){
        // App top header of tasks
        item {
            Spacer(modifier = Modifier.height(10.dp))
            ModernTabHeader(
                title = "Tareas del Hogar",
                subtitle = "Progreso: ${completedChores.size}/${userChores.size} completadas",
                actionText = "Nueva",
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
                                val cleanList = newListNameInput.trim()
                                if (cleanList.isNotEmpty() && cleanList != "General") {
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
                            items(listNames) { listName ->
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
                                                    val currentSetting = settings.find { it.key == "task_lists" }?.value
                                                    val dbLists = currentSetting?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
                                                    if (dbLists.contains(listName) ){
                                                        dbLists[dbLists.indexOf(listName)] = cleanList
                                                        viewModel.updateValueSetting("task_lists", dbLists.joinToString(","))
                                                    }
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
                    "pendientes" to "Pendientes (${pendingChores.size})",
                    "completadas" to "Completadas (${completedChores.size})"
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
                                text = "$priority ($count)",
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
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Homework lists
        if (shownChores.isEmpty()) {
            item {
                ModernEmptyState(
                    icon = if (activeTaskFilter == "pendientes") Icons.Default.CheckCircle else Icons.Default.DoneAll,
                    title = if (activeTaskFilter == "pendientes") "¡Todo listo por ahora!" else "No hay tareas completadas",
                    subtitle = if (activeTaskFilter == "pendientes") "No tienes labores pendientes en esta lista. ¡Excelente trabajo!" else "Completa tareas para ver su registro aquí.",
                    actionText = if (activeTaskFilter == "pendientes") "+ Nueva Tarea" else null,
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

                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        com.example.ui.NotebookPaperSheet(
                            modifier = Modifier.fillMaxWidth(),
                            sheetColor = if (chore.isCompleted) paper.sheetCream else paper.sheet,
                            showRuledLines = false,
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
                                            "alto" -> Color(0xFFEF4444)
                                            "medio" -> Color(0xFFF97316)
                                            else -> Color(0xFF0D9488)
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
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val cleanAssignee = cleanMemberAlias(chore.assignedTo, "Todos")
                                        com.example.ui.MemberAvatarBadge(
                                            memberName = cleanAssignee,
                                            profiles = profiles,
                                            currentUser = currentUser,
                                            currentUserAvatarId = userAvatarIdState,
                                            size = 22.dp
                                        )
                                        Text(
                                            text = "Asignado a $cleanAssignee",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = paper.inkMuted
                                        )
                                        Text(
                                            text = " • ${chore.category}",
                                            fontSize = 11.sp,
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
                                    "Alto" -> AlertRed
                                    "Medio" -> Color(0xFFF57C00)
                                    else -> Color(0xFF00796B)
                                }
                                val isChosen = taskPrioritySelected.equals(pOption, ignoreCase = true)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .background(
                                            if (isChosen) optionColor
                                            else optionColor.copy(alpha = 0.1f)
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Switch(
                            checked = taskIsPrivate,
                            onCheckedChange = { taskIsPrivate = it },
                            colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = FamiCoral)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Solo para mí (Privado)", fontSize = 14.sp, color = paper.ink)
                    }

                    // Assignee dropdown picker list (Represented by profiles capsules)
                    Column {
                        Text("Asignar esta tarea a:", fontSize = 11.sp, color = paper.inkMuted, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp) ){
                            val activeProfiles = (profiles.map { cleanMemberAlias(it.name) }.ifEmpty { listOf(cleanMemberAlias(currentUser)) } + "Todos").distinct()

                            activeProfiles.forEach { name ->
                                val isSelected = taskAssignee == name
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) FamiCoral
                                            else paper.sheetCream
                                        )
                                        .clickable { taskAssignee = name }
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) FamiCoral else paper.border.copy(alpha = 0.45f),
                                            shape = CircleShape
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ){
                                    com.example.ui.MemberAvatarBadge(
                                        memberName = name,
                                        profiles = profiles,
                                        currentUser = currentUser,
                                        currentUserAvatarId = userAvatarIdState,
                                        size = 18.dp
                                    )
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else paper.ink
                                    )
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
                            if (isEditingChore && editingChore != null) {
                                val updated = editingChore!!.copy(
                                        title = taskTitle.trim(),
                                        description = taskDesc.trim(),
                                        priority = taskPrioritySelected,
                                        category = taskCategorySelected,
                                        assignedTo = if (taskIsPrivate) safeUser else taskAssignee,
                                        isPrivate = taskIsPrivate
                                )
                                viewModel.editChore(updated)
                            } else {
                                viewModel.addChore(
                                    title = taskTitle.trim(),
                                    assignedTo = if (taskIsPrivate) safeUser else taskAssignee,
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


// --- TAB 3.5: LISTA DE COMPRAS SEPARADA ---
@Composable
fun ComprasTab(viewModel: FamilyViewModel, onOpenConfig: () -> Unit) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userAvatarIdState by viewModel.userAvatarId.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val paper = FamiTheme.paper
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

    val userShoppingItems = shoppingItems.filter { (!it.isPrivate || it.addedBy == currentUser) && (selectedListName == "General" || it.listName == selectedListName) }
    val pendingShopping = userShoppingItems.filter { !it.isBought }
    val boughtShopping = userShoppingItems.filter { it.isBought }
    val shownShopping = if (activeShoppingFilter == "pendientes") pendingShopping else boughtShopping

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 120.dp) ){
        // Top header block
        item {
            Spacer(modifier = Modifier.height(10.dp))
            ModernTabHeader(
                title = "Lista de Compras",
                subtitle = "${boughtShopping.size}/${userShoppingItems.size} artículos listos",
                actionText = "Nuevo",
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
                            items(listNames) { listName ->
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
                    "pendientes" to "Por Comprar (${pendingShopping.size})",
                    "comprados" to "Comprados (${boughtShopping.size})"
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
                    title = if (activeShoppingFilter == "pendientes") "¡Despensa al día!" else "No hay compras registradas",
                    subtitle = if (activeShoppingFilter == "pendientes") "No hay artículos pendientes por comprar en esta lista." else "Los artículos marcados como comprados aparecerán aquí.",
                    actionText = if (activeShoppingFilter == "pendientes") "+ Agregar Artículo" else null,
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
                        showRuledLines = false,
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
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val cleanAdder = cleanMemberAlias(item.addedBy)
                                    com.example.ui.MemberAvatarBadge(
                                        memberName = cleanAdder,
                                        profiles = profiles,
                                        currentUser = currentUser,
                                        currentUserAvatarId = userAvatarIdState,
                                        size = 22.dp
                                    )
                                    Text(
                                        text = "Por $cleanAdder",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = paper.inkMuted
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
                    Text("Lista de destino:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = paper.ink)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp) ){
                        items(listNames) { list ->
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


// --- BRAND IDENTITY LOGO COMPOSE CANVAS ---
@Composable
fun FamiLogo(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_fami_logo_wide),
        contentDescription = "Fami Logo Oficial",
        modifier = modifier
    )
}


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
    var noteTitleInput by remember { mutableStateOf("") }
    var noteContentInput by remember { mutableStateOf("") }
    
    var editingNote by remember { mutableStateOf<Note?>(null) }
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 150.dp) ){
        item {
            Spacer(modifier = Modifier.height(10.dp))
            ModernTabHeader(
                title = "Alcancía y Metas Fami",
                subtitle = "Ahorros compartidos, proyectos y libreta familiar",
                actionText = if (activeSubTab == "deseos") "Nueva Meta" else "Nueva Nota",
                onActionClick = { 
                    if (activeSubTab == "deseos") {
                        openNewGoalDialog()
                    } else {
                        editingNote = null
                        noteTitleInput = ""
                        noteContentInput = ""
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

            val piggySummaryText = when {
                savedByCurr.isNotEmpty() -> "Ahorrado: " + savedByCurr.entries.joinToString(" • ") { (curr, total) ->
                    "${com.example.currency.CountryCurrencyConfig.getCurrencySymbol(curr)}${String.format(Locale.US, "%,.0f", total)} $curr"
                }
                pendingByCurr.isNotEmpty() -> "Por alcanzar: " + pendingByCurr.entries.joinToString(" • ") { (curr, total) ->
                    "${com.example.currency.CountryCurrencyConfig.getCurrencySymbol(curr)}${String.format(Locale.US, "%,.0f", total)} $curr"
                }
                else -> "Crea una meta con monto y abona a tu alcancía"
            }

            StickyNoteSheet(
                modifier = Modifier.fillMaxWidth(),
                noteColor = paper.stickyPeach,
                tapeColor = FamiCoral.copy(alpha = 0.65f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(paper.sheet)
                                .border(1.8.dp, paper.border, RoundedCornerShape(14.dp))
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.fami_sticker_piggy),
                                contentDescription = "Alcancía Fami",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "🐷 ALCANCÍA FAMILIAR FAMI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = FamiCoral,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${goals.count { it.isCompleted }} cumplidas • ${goals.count { !it.isCompleted }} en curso",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = paper.ink
                            )
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
            val pendingGoals = goals.filter { !it.isCompleted }
            val completedGoals = goals.filter { it.isCompleted }
            
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

            val shownGoals = if (activeGoalFilter == "pendientes") pendingGoals else completedGoals

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

                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        com.example.ui.NotebookPaperSheet(
                            modifier = Modifier.fillMaxWidth(),
                            sheetColor = if (goal.isCompleted) paper.stickyMint else paper.sheet,
                            showRuledLines = false,
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
                                    .padding(start = 28.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val checkboxBg = if (goal.isCompleted) FamiEmerald else Color.Transparent
                                    val checkboxBorderColor = if (goal.isCompleted) FamiEmerald else FamiCoral

                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
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
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            com.example.ui.RuledNotebookText(
                                                text = goal.title,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.5.sp,
                                                maxLines = 2,
                                                lineHeight = 22.sp,
                                                textDecoration = if (goal.isCompleted) TextDecoration.LineThrough else null,
                                                color = if (goal.isCompleted) paper.inkMuted else paper.ink,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (goal.isPrivate) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Privado",
                                                    tint = paper.inkMuted,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            if (hasTarget) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (goal.isCompleted) FamiEmerald.copy(alpha = 0.16f) else paper.stickyYellow)
                                                        .border(1.dp, paper.border.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "$pct% • $goalCurr",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (goal.isCompleted) FamiEmerald else paper.ink
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            com.example.ui.MemberAvatarBadge(
                                                memberName = cleanCreator,
                                                profiles = profiles,
                                                currentUser = currentUser,
                                                currentUserAvatarId = userAvatarIdState,
                                                size = 18.dp
                                            )
                                            Text(
                                                text = "Por $cleanCreator",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = paper.inkMuted
                                            )
                                        }
                                    }
                                }

                                if (hasTarget) {
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Barra de progreso estilo papel
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(9.dp)
                                            .clip(CircleShape)
                                            .background(paper.sheetCream)
                                            .border(1.dp, paper.border.copy(alpha = 0.35f), CircleShape)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction = (pct / 100f).coerceIn(0f, 1f))
                                                .clip(CircleShape)
                                                .background(if (goal.isCompleted || pct >= 100) FamiEmerald else FamiCoral)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Ahorrado: $goalSym${String.format(Locale.US, "%,.2f", effectiveSaved)} / $goalSym${String.format(Locale.US, "%,.2f", target)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = paper.ink
                                            )
                                            if (!goal.isCompleted && remaining > 0.0) {
                                                Text(
                                                    text = "Faltan $goalSym${String.format(Locale.US, "%,.2f", remaining)} $goalCurr",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = paper.inkMuted
                                                )
                                            }
                                        }
                                        if (!goal.isCompleted) {
                                            com.example.ui.Tactile3DButton(
                                                onClick = {
                                                    goalToDeposit = goal
                                                    depositAmountInput = ""
                                                },
                                                faceColor = paper.stickyYellow,
                                                lipColor = Lip3DGold,
                                                borderColor = paper.border,
                                                cornerRadius = 10.dp,
                                                lipHeight = 2.5.dp,
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = "+ Ahorrar",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = paper.ink
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
            // Notas
            if (notes.isEmpty()) {
                item {
                    ModernEmptyState(
                        icon = Icons.Default.EditNote,
                        title = "Tu libreta está vacía",
                        subtitle = "Anota recordatorios, ideas o apuntes compartidos del hogar.",
                        actionText = "+ Nueva Nota",
                        onActionClick = {
                            editingNote = null
                            noteTitleInput = ""
                            noteContentInput = ""
                            showAddNoteDialog = true
                        }
                    )
                }
            } else {
                items(notes) { note ->
                    val cleanAuthor = cleanMemberAlias(note.createdBy)
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        com.example.ui.NotebookPaperSheet(
                            modifier = Modifier.fillMaxWidth(),
                            sheetColor = if (note.isCompleted) paper.sheetCream else paper.stickyYellow.copy(alpha = if (paper.isDark) 0.55f else 0.35f),
                            showRuledLines = false,
                            showMarginLine = true,
                            showSpiralTop = false,
                            onClick = {
                                editingNote = note
                                noteTitleInput = note.title
                                noteContentInput = note.content
                                showAddNoteDialog = true
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 28.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                val checkboxBg = if (note.isCompleted) FamiEmerald else Color.Transparent
                                val checkboxBorderColor = if (note.isCompleted) FamiEmerald else FamiCoral

                                Box(
                                    modifier = Modifier
                                        .padding(top = 3.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(checkboxBg)
                                        .border(2.dp, checkboxBorderColor, CircleShape)
                                        .clickable { viewModel.editNote(note.copy(isCompleted = !note.isCompleted)) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (note.isCompleted) {
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
                                        text = note.title,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.5.sp,
                                        maxLines = 2,
                                        lineHeight = 24.sp,
                                        textDecoration = if (note.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                        color = if (note.isCompleted) paper.inkMuted.copy(alpha = 0.65f) else paper.ink
                                    )
                                    if (note.content.isNotBlank()) {
                                        com.example.ui.RuledNotebookText(
                                            text = note.content,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 24.sp,
                                            minRuledLines = 2,
                                            color = if (note.isCompleted) paper.inkMuted.copy(alpha = 0.55f) else paper.ink
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        com.example.ui.MemberAvatarBadge(
                                            memberName = cleanAuthor,
                                            profiles = profiles,
                                            currentUser = currentUser,
                                            currentUserAvatarId = userAvatarIdState,
                                            size = 18.dp
                                        )
                                        Text(
                                            text = "Por $cleanAuthor",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = paper.inkMuted
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
        AlertDialog(
            onDismissRequest = { 
                showAddNoteDialog = false
                editingNote = null
                noteTitleInput = ""
                noteContentInput = ""
            },
            containerColor = paper.sheet,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (editingNote != null) "Editar Nota" else "Nueva Nota",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = paper.ink
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp) ){
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), 
                        value = noteTitleInput,
                        onValueChange = { noteTitleInput = it },
                        label = { Text("Título") },
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
                        value = noteContentInput,
                        onValueChange = { noteContentInput = it },
                        label = { Text("Contenido") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FamiCoral,
                            unfocusedBorderColor = paper.border.copy(alpha = 0.5f),
                            focusedTextColor = paper.ink,
                            unfocusedTextColor = paper.ink
                        )
                    )
                }
            },
            dismissButton = {
                if (editingNote != null) {
                    TextButton(onClick = { 
                        viewModel.deleteNote(editingNote!!)
                        editingNote = null
                        showAddNoteDialog = false
                    }) {
                        Text("Eliminar", color = AlertRed, fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = { showAddNoteDialog = false }) {
                        Text("Cancelar", color = paper.inkMuted)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteTitleInput.isNotBlank() ){
                            if (editingNote != null) {
                                val updated = editingNote!!.copy(title = noteTitleInput.trim(), content = noteContentInput.trim())
                                viewModel.editNote(updated)
                            } else {
                                viewModel.addNote(noteTitleInput.trim(), noteContentInput.trim())
                            }
                            showAddNoteDialog = false
                            editingNote = null
                            noteTitleInput = ""
                            noteContentInput = ""
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

private fun cleanMemberAlias(raw: String, fallback: String = "Miembro"): String {
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
