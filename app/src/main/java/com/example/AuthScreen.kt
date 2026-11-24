package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.viewmodel.FamilyViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(viewModel: FamilyViewModel) {
    val familyId by viewModel.familyId.collectAsStateWithLifecycle()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
    val familyAction by viewModel.familyAction.collectAsStateWithLifecycle()
    val pendingFamilyName by viewModel.pendingFamilyName.collectAsStateWithLifecycle()
    val managementMode by viewModel.managementMode.collectAsStateWithLifecycle()
    val userAvatarId by viewModel.userAvatarId.collectAsStateWithLifecycle()
    val familyRole by viewModel.familyRole.collectAsStateWithLifecycle()
    val unlockMethod by viewModel.unlockMethod.collectAsStateWithLifecycle()
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val pendingEmailVerification by viewModel.pendingEmailVerification.collectAsStateWithLifecycle()
    val pendingVerificationEmail by viewModel.pendingVerificationEmail.collectAsStateWithLifecycle()
    val needsUnlockMethodChoice by viewModel.needsUnlockMethodChoice.collectAsStateWithLifecycle()
    val startInExistingLogin by viewModel.startInExistingLogin.collectAsStateWithLifecycle()

    val selectedAvatar = remember(userAvatarId) { com.example.ui.AvatarCatalog.getById(userAvatarId) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val paper = FamiTheme.paper

    var currentMode by rememberSaveable {
        mutableStateOf(
            when {
                pendingEmailVerification -> "verify_email"
                needsUnlockMethodChoice -> "choose_unlock"
                familyId != null -> "login"
                startInExistingLogin -> "login_existing"
                hasCompletedOnboarding && familyAction == "join" && managementMode == "familiar" -> "join_family"
                hasCompletedOnboarding -> "create_family"
                else -> "select"
            }
        )
    }
    var forgotPrefillEmail by rememberSaveable { mutableStateOf("") }
    var forgotEmailAlreadySent by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pendingEmailVerification, needsUnlockMethodChoice, familyId) {
        when {
            pendingEmailVerification -> currentMode = "verify_email"
            needsUnlockMethodChoice -> currentMode = "choose_unlock"
            familyId != null && currentMode in setOf("select", "verify_email", "choose_unlock") -> {
                currentMode = "login"
            }
            familyId == null && currentMode == "login" -> {
                currentMode = "select"
            }
        }
    }

    LaunchedEffect(startInExistingLogin) {
        if (startInExistingLogin) {
            currentMode = "login_existing"
            viewModel.consumeStartInExistingLogin()
        }
    }

    var googleJoinCodePending by remember { mutableStateOf("") }
    var googleCustomFamilyNamePending by remember { mutableStateOf("") }
    var googleCustomAliasPending by remember { mutableStateOf("") }
    var googleErrorLocal by remember { mutableStateOf<String?>(null) }
    var isGoogleLaunching by remember { mutableStateOf(false) }
    var pendingGoogleEmailToLink by remember { mutableStateOf<String?>(null) }
    var linkPasswordInput by remember { mutableStateOf("") }
    var showLinkPasswordText by remember { mutableStateOf(false) }

    val googleAccountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isGoogleLaunching = false
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedEmail = result.data
                ?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
                ?.trim()
                .orEmpty()
            if (selectedEmail.isNotBlank()) {
                viewModel.authenticateWithGoogleEmail(
                    email = selectedEmail,
                    joinFamilyCode = googleJoinCodePending,
                    customFamilyName = googleCustomFamilyNamePending.ifBlank { pendingFamilyName },
                    customUserAlias = googleCustomAliasPending,
                    onNeedsExistingPassword = { emailNeedingPass ->
                        linkPasswordInput = ""
                        pendingGoogleEmailToLink = emailNeedingPass
                    }
                )
            } else {
                googleErrorLocal = "No se pudo leer el correo de la cuenta seleccionada."
            }
        }
    }

    fun launchNativeGoogleAccountPicker(joinCode: String) {
        googleJoinCodePending = joinCode
        isGoogleLaunching = true
        try {
            @Suppress("DEPRECATION")
            val intent = android.accounts.AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                false,
                null,
                null,
                null,
                null
            )
            googleAccountPickerLauncher.launch(intent)
        } catch (e: Exception) {
            isGoogleLaunching = false
            googleErrorLocal = "No se encontró selector de cuentas de Google en este dispositivo."
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            val email = account.email?.trim().orEmpty()
            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnSuccessListener { authRes ->
                        isGoogleLaunching = false
                        authRes.user?.let { user ->
                            viewModel.handleGoogleFirebaseUser(
                                firebaseUser = user,
                                joinFamilyCode = googleJoinCodePending,
                                customFamilyName = googleCustomFamilyNamePending.ifBlank { pendingFamilyName },
                                customUserAlias = googleCustomAliasPending
                            )
                        }
                    }
                    .addOnFailureListener {
                        isGoogleLaunching = false
                        if (email.isNotBlank()) {
                            viewModel.authenticateWithGoogleEmail(
                                email = email,
                                joinFamilyCode = googleJoinCodePending,
                                customFamilyName = googleCustomFamilyNamePending.ifBlank { pendingFamilyName },
                                customUserAlias = googleCustomAliasPending,
                                onNeedsExistingPassword = { emailNeedingPass ->
                                    linkPasswordInput = ""
                                    pendingGoogleEmailToLink = emailNeedingPass
                                }
                            )
                        } else {
                            launchNativeGoogleAccountPicker(googleJoinCodePending)
                        }
                    }
            } else if (email.isNotBlank()) {
                isGoogleLaunching = false
                viewModel.authenticateWithGoogleEmail(
                    email = email,
                    joinFamilyCode = googleJoinCodePending,
                    customFamilyName = googleCustomFamilyNamePending.ifBlank { pendingFamilyName },
                    customUserAlias = googleCustomAliasPending,
                    onNeedsExistingPassword = { emailNeedingPass ->
                        linkPasswordInput = ""
                        pendingGoogleEmailToLink = emailNeedingPass
                    }
                )
            } else {
                isGoogleLaunching = false
                launchNativeGoogleAccountPicker(googleJoinCodePending)
            }
        } catch (e: ApiException) {
            isGoogleLaunching = false
            when (e.statusCode) {
                12501 -> {
                    // Usuario canceló voluntariamente
                }
                10, 12500, 7, 8 -> {
                    launchNativeGoogleAccountPicker(googleJoinCodePending)
                }
                else -> {
                    if (result.resultCode == Activity.RESULT_CANCELED && e.statusCode == 16) {
                        // Cancelado
                    } else {
                        launchNativeGoogleAccountPicker(googleJoinCodePending)
                    }
                }
            }
        }
    }

    fun launchGoogleAuth(
        joinCode: String = "",
        customFamilyName: String = "",
        customUserAlias: String = ""
    ) {
        googleJoinCodePending = joinCode
        googleCustomFamilyNamePending = customFamilyName.trim()
        googleCustomAliasPending = customUserAlias.trim()
        googleErrorLocal = null
        viewModel.clearAuthError()
        val webClientResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (webClientResId != 0) {
            val webClientId = context.getString(webClientResId)
            if (webClientId.isNotBlank() && !webClientId.contains("placeholder", ignoreCase = true)) {
                isGoogleLaunching = true
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(context, gso)
                client.signOut().addOnCompleteListener {
                    googleSignInLauncher.launch(client.signInIntent)
                }
                return
            }
        }
        launchNativeGoogleAccountPicker(joinCode)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = paper.sheet,
                    border = BorderStroke(1.8.dp, paper.border),
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_fami_mascot),
                            contentDescription = "Fami Mascota",
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Image(
                            painter = painterResource(
                                id = if (paper.isDark) R.drawable.ic_fami_logo_white else R.drawable.ic_fami_logo_wide
                            ),
                            contentDescription = "Logo Oficial Fami",
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                val canReopenOnboardingFromAuth = familyId == null && !pendingEmailVerification && !needsUnlockMethodChoice
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = paper.stickyYellow,
                    border = BorderStroke(1.3.dp, paper.border),
                    modifier = if (canReopenOnboardingFromAuth) {
                        Modifier.clickable {
                            viewModel.clearAuthError()
                            viewModel.reopenInitialConfig()
                        }
                    } else {
                        Modifier
                    }
                ) {
                    val pendingAliasHeader by viewModel.pendingUserAlias.collectAsStateWithLifecycle()
                    val currentAliasHeader by viewModel.currentUser.collectAsStateWithLifecycle()
                    val displayHeaderAlias = pendingAliasHeader.takeIf { it.isNotBlank() && !it.contains("@") }
                        ?: currentAliasHeader.takeIf { it.isNotBlank() && !it.contains("@") }
                        ?: ""
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.MemberAvatarBadge(
                            memberName = displayHeaderAlias.ifBlank { "Fami" },
                            explicitAvatarId = selectedAvatar.id,
                            size = 28.dp,
                            showAliasBelow = displayHeaderAlias.isNotBlank(),
                            aliasText = displayHeaderAlias,
                            aliasFontSize = 8.5.sp,
                            aliasMaxWidth = 56.dp
                        )
                        Text(
                            text = if (managementMode == "individual") {
                                "👤 Modo Individual"
                            } else {
                                "👨‍👩‍👧‍👦 Familiar • $familyRole"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink
                        )
                        if (canReopenOnboardingFromAuth) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Cambiar modo o configuración inicial",
                                tint = FamiCoral,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val displayedError = authError ?: googleErrorLocal
            if (!displayedError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = paper.alertBg,
                    border = BorderStroke(1.2.dp, FamiAlertRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = FamiAlertRed, modifier = Modifier.size(18.dp))
                        Text(
                            text = displayedError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = paper.ink,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                viewModel.clearAuthError()
                                googleErrorLocal = null
                            },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar error", tint = paper.ink, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = currentMode,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + slideInVertically(animationSpec = tween(220)) { it / 6 })
                        .togetherWith(fadeOut(animationSpec = tween(180)))
                },
                label = "auth_screen_flow"
            ) { mode ->
                when (mode) {
                    "select" -> SelectModeContent(
                        managementMode = managementMode,
                        isLoading = isAuthLoading || isGoogleLaunching,
                        onCreateFamily = {
                            viewModel.clearAuthError()
                            currentMode = "create_family"
                        },
                        onJoinFamily = {
                            viewModel.clearAuthError()
                            viewModel.updateManagementMode("familiar")
                            currentMode = "join_family"
                        },
                        onLoginExisting = {
                            viewModel.clearAuthError()
                            currentMode = "login_existing"
                        },
                        onReopenSetup = {
                            viewModel.clearAuthError()
                            viewModel.reopenInitialConfig()
                        },
                        onContinueWithGoogle = { launchGoogleAuth() }
                    )
                    "login" -> LoginContent(
                        viewModel = viewModel,
                        unlockMethod = unlockMethod,
                        isLoading = isAuthLoading,
                        onReset = {
                            viewModel.clearAuthError()
                            currentMode = "select"
                        },
                        onLogout = {
                            viewModel.clearAuthError()
                            viewModel.logout()
                            currentMode = "select"
                        }
                    )
                    "create_family" -> CreateFamilyContent(
                        viewModel = viewModel,
                        initialFamilyName = pendingFamilyName,
                        managementMode = managementMode,
                        isLoading = isAuthLoading || isGoogleLaunching,
                        onBack = {
                            viewModel.clearAuthError()
                            currentMode = "select"
                        },
                        onSwitchToLogin = {
                            viewModel.clearAuthError()
                            currentMode = "login_existing"
                        },
                        onForgotPassword = { typedEmail ->
                            viewModel.clearAuthError()
                            forgotPrefillEmail = typedEmail
                            forgotEmailAlreadySent = false
                            currentMode = "forgot_password"
                        },
                        onContinueWithGoogle = { customFamName, customAlias ->
                            launchGoogleAuth(customFamilyName = customFamName, customUserAlias = customAlias)
                        }
                    )
                    "join_family" -> JoinFamilyContent(
                        viewModel = viewModel,
                        isLoading = isAuthLoading || isGoogleLaunching,
                        onBack = {
                            viewModel.clearAuthError()
                            currentMode = "select"
                        },
                        onForgotPassword = { typedEmail ->
                            viewModel.clearAuthError()
                            forgotPrefillEmail = typedEmail
                            forgotEmailAlreadySent = false
                            currentMode = "forgot_password"
                        },
                        onContinueWithGoogle = { code, customAlias ->
                            launchGoogleAuth(joinCode = code, customUserAlias = customAlias)
                        }
                    )
                    "login_existing" -> LoginExistingFamilyContent(
                        viewModel = viewModel,
                        isLoading = isAuthLoading || isGoogleLaunching,
                        onBack = {
                            viewModel.clearAuthError()
                            currentMode = "select"
                        },
                        onSwitchToRegister = {
                            viewModel.clearAuthError()
                            currentMode = "create_family"
                        },
                        onForgotPassword = { typedEmail ->
                            viewModel.clearAuthError()
                            forgotPrefillEmail = typedEmail
                            forgotEmailAlreadySent = false
                            currentMode = "forgot_password"
                        },
                        onContinueWithGoogle = { launchGoogleAuth() }
                    )
                    "verify_email" -> VerifyEmailContent(
                        email = pendingVerificationEmail,
                        isLoading = isAuthLoading,
                        onCheckVerified = { viewModel.checkEmailVerified() },
                        onResendEmail = { viewModel.resendVerificationEmail() },
                        onCancel = {
                            viewModel.cancelPendingVerification()
                            currentMode = "select"
                        }
                    )
                    "forgot_password" -> ForgotPasswordContent(
                        viewModel = viewModel,
                        initialEmail = forgotPrefillEmail,
                        initialEmailSent = forgotEmailAlreadySent,
                        isLoading = isAuthLoading,
                        onBackToLogin = {
                            viewModel.clearAuthError()
                            forgotEmailAlreadySent = false
                            currentMode = "login_existing"
                        }
                    )
                    "choose_unlock" -> ChooseUnlockMethodContent(
                        viewModel = viewModel,
                        isLoading = isAuthLoading
                    )
                }
            }
        }

        pendingGoogleEmailToLink?.let { emailToLink ->
            AlertDialog(
                onDismissRequest = { pendingGoogleEmailToLink = null },
                containerColor = paper.sheet,
                title = {
                    Text(
                        "Completar acceso con Google",
                        fontWeight = FontWeight.Black,
                        color = paper.ink
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "El correo $emailToLink ya tenía una contraseña previa en Fami. Ingrésala una sola vez para entrar y vincular tu cuenta de Google:",
                            fontSize = 13.sp,
                            color = paper.inkMuted
                        )
                        OutlinedTextField(
                            value = linkPasswordInput,
                            onValueChange = { linkPasswordInput = it },
                            label = { Text("Contraseña de Fami") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showLinkPasswordText = !showLinkPasswordText }) {
                                    Icon(
                                        if (showLinkPasswordText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showLinkPasswordText) "Ocultar contraseña" else "Mostrar contraseña"
                                    )
                                }
                            },
                            visualTransformation = if (showLinkPasswordText) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(
                            onClick = {
                                val targetEmail = emailToLink
                                pendingGoogleEmailToLink = null
                                viewModel.sendPasswordReset(targetEmail) {
                                    forgotPrefillEmail = targetEmail
                                    forgotEmailAlreadySent = true
                                    currentMode = "forgot_password"
                                }
                            }
                        ) {
                            Text("¿No recuerdas tu clave anterior? Enviar enlace al correo", fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (linkPasswordInput.length < 6) {
                                Toast.makeText(context, "Ingresa tu contraseña (mínimo 6 caracteres)", Toast.LENGTH_SHORT).show()
                            } else {
                                val targetEmail = emailToLink
                                val pass = linkPasswordInput
                                pendingGoogleEmailToLink = null
                                viewModel.authenticateWithGoogleEmail(
                                    email = targetEmail,
                                    joinFamilyCode = googleJoinCodePending,
                                    customFamilyName = googleCustomFamilyNamePending.ifBlank { pendingFamilyName },
                                    customUserAlias = googleCustomAliasPending,
                                    existingPasswordToLink = pass,
                                    onNeedsExistingPassword = {
                                        pendingGoogleEmailToLink = targetEmail
                                    }
                                )
                            }
                        }
                    ) {
                        Text("Vincular y Entrar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingGoogleEmailToLink = null }) {
                        Text("Cancelar", color = paper.inkMuted)
                    }
                }
            )
        }
    }
}

@Composable
private fun OfficialGoogleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.21f
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        val strokeStyle = Stroke(width = stroke, cap = StrokeCap.Butt)

        // Rojo superior (#EA4335)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 212f,
            sweepAngle = 108f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = strokeStyle
        )
        // Amarillo izquierdo (#FBBC05)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 142f,
            sweepAngle = 72f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = strokeStyle
        )
        // Verde inferior (#34A853)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 40f,
            sweepAngle = 104f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = strokeStyle
        )
        // Azul derecho (#4285F4)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -6f,
            sweepAngle = 48f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = strokeStyle
        )
        // Barra horizontal azul de la G (#4285F4)
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.50f, size.height * 0.50f),
            end = Offset(size.width - inset * 0.35f, size.height * 0.50f),
            strokeWidth = stroke * 0.95f,
            cap = StrokeCap.Butt
        )
    }
}

@Composable
private fun GoogleSignInButton(
    text: String = "Continuar con Google",
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        if (!enabled) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF4285F4)
            )
        } else {
            Surface(
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.size(26.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(4.dp)
                ) {
                    OfficialGoogleLogo(modifier = Modifier.fillMaxSize())
                }
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// -------------------------------------------------------------
// 1. PANTALLA PRINCIPAL DE BIENVENIDA / SELECTOR
// -------------------------------------------------------------
@Composable
private fun SelectModeContent(
    managementMode: String = "familiar",
    isLoading: Boolean = false,
    onCreateFamily: () -> Unit,
    onJoinFamily: () -> Unit,
    onLoginExisting: () -> Unit,
    onReopenSetup: () -> Unit = {},
    onContinueWithGoogle: () -> Unit
) {
    val s = com.example.ui.LocalFamiStrings.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = s.authWelcomeTitle,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = s.authWelcomeSubtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
        )

        FeatureHighlightPill(
            icon = Icons.Outlined.AutoAwesome,
            title = s.authFeature1Title,
            subtitle = s.authFeature1Sub
        )
        Spacer(modifier = Modifier.height(10.dp))
        FeatureHighlightPill(
            icon = Icons.Outlined.PeopleAlt,
            title = s.authFeature2Title,
            subtitle = s.authFeature2Sub
        )
        Spacer(modifier = Modifier.height(10.dp))
        FeatureHighlightPill(
            icon = Icons.Outlined.CurrencyExchange,
            title = s.authFeature3Title,
            subtitle = s.authFeature3Sub
        )

        Spacer(modifier = Modifier.height(22.dp))

        GoogleSignInButton(
            text = s.authGoogleBtn,
            enabled = !isLoading,
            onClick = onContinueWithGoogle
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onCreateFamily,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
        ) {
            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(s.authEmailBtn, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        if (managementMode != "individual") {
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onJoinFamily,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(s.authJoinCodeBtn, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onReopenSetup) {
                Text(
                    text = "⚙️ Cambiar modo / país",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onLoginExisting) {
                Text(
                    text = s.authExistingAccountBtn,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "somosfami.app • @hola.fami",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        var legalTab by remember { mutableStateOf<String?>(null) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("${s.settingsPrivacyLegalTitle}: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = s.settingsTermsBtn,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { legalTab = "terms" }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            )
            Text(" • ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = s.settingsPrivacyBtn,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { legalTab = "privacy" }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            )
        }
        legalTab?.let { tab ->
            com.example.ui.LegalDocumentDialog(
                initialTab = tab,
                onDismiss = { legalTab = null }
            )
        }
    }
}

@Composable
private fun FeatureHighlightPill(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
            }
        }
    }
}

// -------------------------------------------------------------
// 2. DESBLOQUEO RÁPIDO (PIN O HUELLA SEGÚN PREFERENCIA)
// -------------------------------------------------------------
@Composable
private fun LoginContent(
    viewModel: FamilyViewModel,
    unlockMethod: String,
    isLoading: Boolean,
    onReset: () -> Unit,
    onLogout: () -> Unit = {}
) {
    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val displayAlias = currentUser.takeIf { it.isNotBlank() && !it.contains("@") }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val activity = remember(context) { context.findFragmentActivity() }
    val biometricAvailable = remember(context) { canAuthenticateBiometric(context) }
    val biometricEnabled = unlockMethod == "biometric" && biometricAvailable
    val s = com.example.ui.LocalFamiStrings.current
    val paper = FamiTheme.paper

    LaunchedEffect(biometricEnabled, activity) {
        if (biometricEnabled && activity != null) {
            showBiometricPrompt(activity, onSuccess = {
                viewModel.loginBiometric()
            })
        }
    }

    val submitPin: (String) -> Unit = { candidate ->
        if (candidate.length == 5 && !isLoading) {
            keyboardController?.hide()
            viewModel.loginWithPin(candidate) { ok ->
                if (!ok) {
                    pin = ""
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            ) {
                Icon(
                    imageVector = if (biometricEnabled) Icons.Default.Fingerprint else Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (displayAlias != null) "¡Hola, $displayAlias!" else s.authUnlockTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (biometricEnabled) {
                    s.authUnlockSubBiometric
                } else {
                    s.authUnlockSubPin
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            // Indicador táctil de 5 puntos del PIN
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                repeat(5) { idx ->
                    val filled = idx < pin.length
                    Box(
                        modifier = Modifier
                            .size(if (filled) 14.dp else 12.dp)
                            .clip(CircleShape)
                            .background(if (filled) FamiCoral else paper.sheetCream)
                            .then(
                                if (!filled) Modifier.background(paper.sheetCream) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = if (filled) FamiCoral else paper.sheetCream,
                            border = BorderStroke(1.5.dp, if (filled) FamiCoral else paper.border.copy(alpha = 0.5f))
                        ) {}
                    }
                }
            }

            OutlinedTextField(
                value = pin,
                onValueChange = { raw ->
                    val digits = raw.filter { ch -> ch.isDigit() }.take(5)
                    pin = digits
                    if (digits.length == 5) {
                        submitPin(digits)
                    }
                },
                label = { Text(s.authPinLabel) },
                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showPin = !showPin }) {
                        Icon(
                            if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPin) "Ocultar PIN" else "Mostrar PIN"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submitPin(pin) }),
                visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (biometricEnabled && activity != null) {
                    IconButton(
                        onClick = {
                            showBiometricPrompt(activity, onSuccess = {
                                viewModel.loginBiometric()
                            })
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = "Biometric", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                }

                Button(
                    onClick = {
                        if (pin.length == 5) {
                            submitPin(pin)
                        } else {
                            Toast.makeText(context, s.authUnlockSubPin, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(s.authEnterBtn, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onReset) {
                    Text(s.authSwitchMethodBtn, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onLogout) {
                    Text(
                        "Cerrar sesión",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (paper.isDark) FamiCoralLight else FamiCoralDeep
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordStrengthBar(password: String) {
    if (password.isEmpty()) return
    val paper = FamiTheme.paper
    val hasLetter = password.any { it.isLetter() }
    val hasDigit = password.any { it.isDigit() }
    val hasSymbol = password.any { !it.isLetterOrDigit() }
    val score = when {
        password.length < 6 -> 1
        password.length >= 10 && hasLetter && hasDigit && hasSymbol -> 3
        password.length >= 8 && hasLetter && hasDigit -> 3
        else -> 2
    }
    val (label, barColor, progress) = when (score) {
        1 -> Triple("Muy corta (mín. 6 caracteres)", FamiAlertRed, 0.33f)
        2 -> Triple("Aceptable (añade números para reforzar)", FamiGold, 0.66f)
        else -> Triple("Contraseña segura ✓", FamiEmerald, 1f)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp)),
            color = barColor,
            trackColor = paper.ruleLine
        )
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (score == 1 && paper.isDark) FamiCoralLight else barColor
        )
    }
}

@Composable
private fun LegalConsentRow(
    accepted: Boolean,
    showError: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val paper = FamiTheme.paper
    var legalTab by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (showError) paper.alertBg else paper.sheetCream,
            border = BorderStroke(
                width = if (showError) 1.5.dp else 1.dp,
                color = if (showError) FamiAlertRed else paper.border.copy(alpha = 0.35f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(!accepted) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = accepted,
                    onCheckedChange = onToggle,
                    colors = CheckboxDefaults.colors(
                        checkedColor = FamiCoral,
                        uncheckedColor = if (showError) FamiAlertRed else paper.inkMuted
                    )
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Acepto los Términos de Uso y la Política de Privacidad de Fami (0 rastreadores publicitarios).",
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = paper.ink
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Ver Términos",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (paper.isDark) FamiCoralLight else FamiCoral,
                            modifier = Modifier
                                .clickable { legalTab = "terms" }
                                .padding(vertical = 2.dp)
                        )
                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            color = paper.inkMuted
                        )
                        Text(
                            text = "Ver Privacidad",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (paper.isDark) FamiCoralLight else FamiCoral,
                            modifier = Modifier
                                .clickable { legalTab = "privacy" }
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
        if (showError) {
            Text(
                text = "Debes aceptar los Términos y la Política de Privacidad para continuar.",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (paper.isDark) FamiCoralLight else FamiAlertRed,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
            )
        }
    }

    legalTab?.let { tab ->
        com.example.ui.LegalDocumentDialog(
            initialTab = tab,
            onDismiss = { legalTab = null }
        )
    }
}

// -------------------------------------------------------------
// 3. CREAR NUEVA CUENTA FAMI (GOOGLE O CORREO VERIFICADO)
// -------------------------------------------------------------
@Composable
private fun CreateFamilyContent(
    viewModel: FamilyViewModel,
    initialFamilyName: String = "",
    managementMode: String = "familiar",
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onSwitchToLogin: () -> Unit,
    onForgotPassword: (String) -> Unit = {},
    onContinueWithGoogle: (String, String) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val pendingAlias by viewModel.pendingUserAlias.collectAsStateWithLifecycle()
    var familyNameInput by remember(initialFamilyName) { mutableStateOf(initialFamilyName) }
    var userNameInput by remember(pendingAlias) {
        mutableStateOf(pendingAlias.takeIf { it.isNotBlank() && !it.contains("@") }.orEmpty())
    }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    var acceptedLegal by rememberSaveable { mutableStateOf(true) }
    var attemptedSubmit by remember { mutableStateOf(false) }
    var attemptedGoogleSubmit by remember { mutableStateOf(false) }

    val aliasError = attemptedSubmit && !viewModel.isValidAlias(userNameInput)
    val emailError = (attemptedSubmit || email.contains("@")) && email.isNotBlank() && !viewModel.isValidEmailFormat(email)
    val emailMissingError = attemptedSubmit && email.isBlank()
    val passwordError = attemptedSubmit && password.length < 6
    val pinError = (attemptedSubmit && !viewModel.isValidPin(pin)) || (pin.length == 5 && pin == "00000")
    val legalError = (attemptedSubmit || attemptedGoogleSubmit) && !acceptedLegal

    val submitCreate: () -> Unit = {
        attemptedSubmit = true
        val cleanAlias = userNameInput.trim()
        if (!acceptedLegal) {
            Toast.makeText(context, "Acepta los Términos y la Política de Privacidad para continuar", Toast.LENGTH_SHORT).show()
        } else if (!viewModel.isValidAlias(cleanAlias)) {
            Toast.makeText(context, "Escribe tu Alias (mínimo 2 letras, sin correo electrónico)", Toast.LENGTH_SHORT).show()
        } else if (!viewModel.isValidEmailFormat(email)) {
            Toast.makeText(context, "Ingresa un correo electrónico real y válido", Toast.LENGTH_SHORT).show()
        } else if (password.length < 6) {
            Toast.makeText(context, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
        } else if (!viewModel.isValidPin(pin)) {
            Toast.makeText(context, "El PIN debe ser de 5 dígitos y distinto de 00000", Toast.LENGTH_SHORT).show()
        } else {
            keyboardController?.hide()
            viewModel.createFamily(
                email = email.trim(),
                pin = pin,
                password = password,
                customFamilyName = familyNameInput.trim(),
                customUserName = cleanAlias
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (managementMode == "individual") "Crea tu Espacio Personal" else "Crea tu Hogar Fami",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (managementMode == "individual") "Tu cuenta privada multi-moneda en la nube" else "Serás el administrador de este hogar",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GoogleSignInButton(
                text = "Crear rápido con Google",
                enabled = !isLoading,
                onClick = {
                    if (!acceptedLegal) {
                        attemptedGoogleSubmit = true
                        Toast.makeText(context, "Acepta los Términos y la Política de Privacidad para continuar", Toast.LENGTH_SHORT).show()
                    } else {
                        onContinueWithGoogle(familyNameInput.trim(), userNameInput.trim())
                    }
                }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "  o con correo válido  ",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            OutlinedTextField(
                value = familyNameInput,
                onValueChange = { familyNameInput = it },
                label = { Text(if (managementMode == "individual") "Nombre de tu Espacio" else "Nombre de la Familia / Hogar") },
                placeholder = { Text(if (managementMode == "individual") "Ej: Mis Finanzas" else "Ej: Familia Pérez") },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = userNameInput,
                onValueChange = { userNameInput = it },
                label = { Text("Tu Nombre o Alias (obligatorio)") },
                placeholder = { Text("Ej: Mamá, Carlos, Sofía...") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                isError = aliasError,
                supportingText = if (aliasError) {
                    { Text("Ingresa un alias de al menos 2 letras (sin correo electrónico)") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico Válido") },
                placeholder = { Text("tucorreo@gmail.com") },
                isError = emailError || emailMissingError,
                supportingText = {
                    Text(
                        when {
                            emailError || emailMissingError -> "Ingresa un correo electrónico real y válido (ej. nombre@gmail.com)"
                            else -> "Te enviaremos un correo para verificar tu cuenta y recuperarla si olvidas tu clave"
                        }
                    )
                },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña de Acceso") },
                isError = passwordError,
                supportingText = {
                    if (password.isEmpty()) {
                        Text("Mínimo 6 caracteres")
                    } else {
                        PasswordStrengthBar(password)
                    }
                },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Mostrar u ocultar contraseña"
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(5) },
                label = { Text("PIN de Respaldo (5 dígitos)") },
                isError = pinError,
                supportingText = {
                    Text(
                        when {
                            pin == "00000" -> "Por seguridad, el PIN no puede ser 00000"
                            pinError -> "Escribe exactamente 5 dígitos numéricos (${pin.length}/5)"
                            else -> "Luego elegirás si entrar con Huella o PIN (${pin.length}/5)"
                        }
                    )
                },
                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showPin = !showPin }) {
                        Icon(
                            if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPin) "Ocultar PIN" else "Mostrar PIN"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submitCreate() }),
                visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            LegalConsentRow(
                accepted = acceptedLegal,
                showError = legalError,
                onToggle = { acceptedLegal = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = submitCreate,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Crear Cuenta y Verificar Correo", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onForgotPassword(email.trim()) }) {
                    Text("¿Olvidaste tu contraseña?", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = onSwitchToLogin) {
                    Text("¿Ya tienes cuenta? Iniciar Sesión", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. UNIRSE A UN HOGAR EXISTENTE MEDIANTE CÓDIGO
// -------------------------------------------------------------
@Composable
private fun JoinFamilyContent(
    viewModel: FamilyViewModel,
    isLoading: Boolean,
    onBack: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onContinueWithGoogle: (String, String) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val pendingAlias by viewModel.pendingUserAlias.collectAsStateWithLifecycle()
    var code by remember { mutableStateOf("") }
    var name by remember(pendingAlias) {
        mutableStateOf(pendingAlias.takeIf { it.isNotBlank() && !it.contains("@") }.orEmpty())
    }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    var acceptedLegal by rememberSaveable { mutableStateOf(true) }
    var attemptedSubmit by remember { mutableStateOf(false) }
    var attemptedGoogleSubmit by remember { mutableStateOf(false) }

    val codeError = attemptedSubmit && code.length < 4
    val nameError = attemptedSubmit && !viewModel.isValidAlias(name)
    val emailError = attemptedSubmit && !viewModel.isValidEmailFormat(email)
    val passError = attemptedSubmit && password.length < 6
    val pinError = (attemptedSubmit && !viewModel.isValidPin(pin)) || (pin.length == 5 && pin == "00000")
    val legalError = (attemptedSubmit || attemptedGoogleSubmit) && !acceptedLegal

    val submitJoin: () -> Unit = {
        attemptedSubmit = true
        val normalizedCode = viewModel.normalizeFamilyCode(code)
        val cleanName = name.trim()
        if (!acceptedLegal) {
            Toast.makeText(context, "Acepta los Términos y la Política de Privacidad para continuar", Toast.LENGTH_SHORT).show()
        } else if (normalizedCode.length < 4) {
            Toast.makeText(context, "Ingresa el código familiar (mínimo 4 caracteres)", Toast.LENGTH_SHORT).show()
        } else if (!viewModel.isValidAlias(cleanName)) {
            Toast.makeText(context, "Escribe tu nombre o alias (sin correo electrónico)", Toast.LENGTH_SHORT).show()
        } else if (!viewModel.isValidEmailFormat(email)) {
            Toast.makeText(context, "Ingresa un correo electrónico válido", Toast.LENGTH_SHORT).show()
        } else if (password.length < 6) {
            Toast.makeText(context, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
        } else if (!viewModel.isValidPin(pin)) {
            Toast.makeText(context, "El PIN debe ser de 5 dígitos y distinto de 00000", Toast.LENGTH_SHORT).show()
        } else {
            keyboardController?.hide()
            viewModel.joinFamily(normalizedCode, cleanName, pin, email.trim(), password)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Unirse a un Hogar Fami",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ingresa el código que te compartió tu familia",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { code = viewModel.normalizeFamilyCode(it).take(16) },
                label = { Text("Código Familiar (Ej: A1B2C3D4)") },
                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                isError = codeError,
                supportingText = if (codeError) {
                    { Text("Pídele el código de 8 caracteres al administrador del hogar") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            GoogleSignInButton(
                text = "Unirme con Google",
                enabled = !isLoading,
                onClick = {
                    val normalizedCode = viewModel.normalizeFamilyCode(code)
                    if (!acceptedLegal) {
                        attemptedGoogleSubmit = true
                        Toast.makeText(context, "Acepta los Términos y la Política de Privacidad para continuar", Toast.LENGTH_SHORT).show()
                    } else if (normalizedCode.length < 4) {
                        attemptedSubmit = true
                        Toast.makeText(context, "Primero ingresa el código familiar arriba", Toast.LENGTH_SHORT).show()
                    } else {
                        onContinueWithGoogle(normalizedCode, name.trim())
                    }
                }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text("  o con correo  ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tu Nombre o Alias") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Escribe tu alias (mín. 2 letras, sin correo electrónico)") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Tu Correo Electrónico Válido") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                isError = emailError,
                supportingText = if (emailError) {
                    { Text("Ingresa un correo electrónico válido") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña (mínimo 6 caracteres)") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                isError = passError,
                supportingText = if (password.isNotEmpty()) {
                    { PasswordStrengthBar(password) }
                } else if (passError) {
                    { Text("Mínimo 6 caracteres") }
                } else null,
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(5) },
                label = { Text("Crea tu PIN de 5 dígitos") },
                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                isError = pinError,
                supportingText = {
                    Text(
                        when {
                            pin == "00000" -> "Por seguridad, el PIN no puede ser 00000"
                            pinError -> "Debe tener exactamente 5 dígitos (${pin.length}/5)"
                            else -> "Para desbloquear Fami rápidamente (${pin.length}/5)"
                        }
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showPin = !showPin }) {
                        Icon(
                            if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPin) "Ocultar PIN" else "Mostrar PIN"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submitJoin() }),
                visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            LegalConsentRow(
                accepted = acceptedLegal,
                showError = legalError,
                onToggle = { acceptedLegal = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = submitJoin,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Unirse a la Familia", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = { onForgotPassword(email.trim()) }) {
                    Text("¿Olvidaste tu contraseña? Recupérala aquí", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. INICIAR SESIÓN EN CUENTA EXISTENTE (GOOGLE O CORREO)
// -------------------------------------------------------------
@Composable
private fun LoginExistingFamilyContent(
    viewModel: FamilyViewModel,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSwitchToRegister: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onContinueWithGoogle: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    var attemptedSubmit by remember { mutableStateOf(false) }

    val emailError = attemptedSubmit && !viewModel.isValidEmailFormat(email)
    val passError = attemptedSubmit && password.length < 6
    val pinError = (attemptedSubmit && !viewModel.isValidPin(pin)) || (pin.length == 5 && pin == "00000")

    val submitLogin: () -> Unit = {
        attemptedSubmit = true
        if (viewModel.isValidEmailFormat(email) && password.length >= 6 && viewModel.isValidPin(pin)) {
            keyboardController?.hide()
            viewModel.loginExisting(email.trim(), password, pin)
        } else {
            Toast.makeText(context, "Ingresa correo válido, contraseña y PIN de 5 dígitos (distinto de 00000)", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Iniciar Sesión",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Accede con tu cuenta de Google o correo",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GoogleSignInButton(
                text = "Iniciar Sesión con Google",
                enabled = !isLoading,
                onClick = onContinueWithGoogle
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text("  o con correo  ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                isError = emailError,
                supportingText = if (emailError) {
                    { Text("Ingresa un correo electrónico válido") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                isError = passError,
                supportingText = if (passError) {
                    { Text("Mínimo 6 caracteres") }
                } else null,
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(5) },
                label = { Text("PIN Rápido para este dispositivo (5 dígitos)") },
                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                isError = pinError,
                supportingText = {
                    Text(
                        when {
                            pin == "00000" -> "Por seguridad, el PIN no puede ser 00000"
                            pinError -> "Escribe 5 dígitos numéricos (${pin.length}/5)"
                            else -> "Se guarda solo en este teléfono para abrir más rápido (luego puedes activar Huella)"
                        }
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showPin = !showPin }) {
                        Icon(
                            if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPin) "Ocultar PIN" else "Mostrar PIN"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submitLogin() }),
                visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = submitLogin,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Entrar a Fami", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onForgotPassword(email.trim()) }) {
                    Text("¿Olvidaste tu contraseña?", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }

                TextButton(onClick = onSwitchToRegister) {
                    Text("Registrarse", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. VERIFICACIÓN DE CORREO ELECTRÓNICO REAL
// -------------------------------------------------------------
@Composable
private fun VerifyEmailContent(
    email: String,
    isLoading: Boolean,
    onCheckVerified: () -> Unit,
    onResendEmail: () -> Unit,
    onCancel: () -> Unit
) {
    val paper = FamiTheme.paper
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var resendCooldown by remember { mutableIntStateOf(45) }
    var hasLeftForMailApp by remember { mutableStateOf(false) }

    // Auto-verificar cuando el usuario vuelve a Fami desde su app de correo
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && hasLeftForMailApp && !isLoading) {
                hasLeftForMailApp = false
                onCheckVerified()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, paper.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .background(paper.stickyYellow, CircleShape)
            ) {
                Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = FamiCoral, modifier = Modifier.size(34.dp))
            }

            Text(
                text = "Verifica tu Correo",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Enviamos un enlace de aprobación oficial a:\n$email\n\nÁbrelo desde tu bandeja de entrada (o Spam). Al volver a Fami verificaremos tu cuenta automáticamente.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                onClick = {
                    val mailIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_APP_EMAIL)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        hasLeftForMailApp = true
                        context.startActivity(mailIntent)
                    } catch (_: Exception) {
                        hasLeftForMailApp = false
                        Toast.makeText(context, "Abre tu app de correo y toca el enlace enviado a $email", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.4.dp, FamiCoral)
            ) {
                Icon(Icons.Default.Email, contentDescription = null, tint = FamiCoral, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Abrir app de correo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FamiCoral)
            }

            Button(
                onClick = onCheckVerified,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamiEmerald)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Ya verifiqué mi correo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            OutlinedButton(
                onClick = {
                    onResendEmail()
                    resendCooldown = 60
                },
                enabled = resendCooldown == 0 && !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (resendCooldown > 0) "Reenviar correo (${resendCooldown}s)" else "Reenviar correo de verificación",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(onClick = onCancel) {
                Text("Cambiar correo o volver", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// -------------------------------------------------------------
// 7. RECUPERACIÓN DE CUENTA / OLVIDO DE CLAVE
// -------------------------------------------------------------
@Composable
private fun ForgotPasswordContent(
    viewModel: FamilyViewModel,
    initialEmail: String = "",
    initialEmailSent: Boolean = false,
    isLoading: Boolean,
    onBackToLogin: () -> Unit
) {
    val paper = FamiTheme.paper
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var emailSent by remember(initialEmailSent) { mutableStateOf(initialEmailSent) }
    var codeOrLinkInput by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var showNewPassword by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToLogin, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Recuperar Cuenta",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Te enviaremos el código/enlace de recuperación a tu correo",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!emailSent) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Tu Correo Registrado") },
                    placeholder = { Text("tucorreo@gmail.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        viewModel.sendPasswordReset(email) {
                            emailSent = true
                        }
                    },
                    enabled = !isLoading && email.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Enviar Código de Recuperación", fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = paper.stickyMint,
                    border = BorderStroke(1.2.dp, paper.border)
                ) {
                    Text(
                        text = "📩 Enviamos un correo de recuperación a $email.\n\nPuedes cambiar tu clave directo desde el enlace del correo o pegar aquí el código (oobCode) del enlace:",
                        fontSize = 12.sp,
                        color = paper.ink,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                OutlinedTextField(
                    value = codeOrLinkInput,
                    onValueChange = { codeOrLinkInput = it },
                    label = { Text("Código o enlace del correo (Opcional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nueva contraseña (mín. 6 caracteres)") },
                    trailingIcon = {
                        IconButton(onClick = { showNewPassword = !showNewPassword }) {
                            Icon(
                                if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showNewPassword) "Ocultar contraseña" else "Mostrar contraseña"
                            )
                        }
                    },
                    visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (codeOrLinkInput.isNotBlank()) {
                    Button(
                        onClick = {
                            viewModel.confirmPasswordResetWithCode(codeOrLinkInput, newPassword) {
                                onBackToLogin()
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Confirmar Nueva Contraseña", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onBackToLogin,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Ya cambié mi clave • Ir a Iniciar Sesión", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 8. ELECCIÓN POST-CREACIÓN: ¿ENTRAR CON HUELLA O PIN?
// -------------------------------------------------------------
@Composable
private fun ChooseUnlockMethodContent(
    viewModel: FamilyViewModel,
    isLoading: Boolean
) {
    val paper = FamiTheme.paper
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val pendingAlias by viewModel.pendingUserAlias.collectAsStateWithLifecycle()
    val initialAlias = remember(currentUser, pendingAlias) {
        pendingAlias.takeIf { it.isNotBlank() && !it.contains("@") }
            ?: currentUser.takeIf { it.isNotBlank() && !it.contains("@") }.orEmpty()
    }
    var aliasInput by remember(initialAlias) { mutableStateOf(initialAlias) }
    var alreadyHasPin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        alreadyHasPin = viewModel.hasConfiguredPin()
    }

    val biometricSupported = remember(context) { canAuthenticateBiometric(context) }
    var selectedOption by remember { mutableStateOf(if (biometricSupported) "biometric" else "pin") }
    var pinConfirm by remember { mutableStateOf("") }
    var showPinConfirm by remember { mutableStateOf(false) }
    var attemptedSubmit by remember { mutableStateOf(false) }

    val pinRequiredNow = selectedOption == "pin" && !alreadyHasPin
    val pinError = (attemptedSubmit && pinRequiredNow && !viewModel.isValidPin(pinConfirm)) ||
        (pinConfirm.isNotEmpty() && pinConfirm.length == 5 && pinConfirm == "00000")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = paper.sheet),
        border = BorderStroke(1.5.dp, paper.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🔐 Elige cómo entrar a Fami",
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                color = paper.ink,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Tu cuenta ya está lista. Confirma tu alias visible y cómo deseas abrir la app:",
                fontSize = 12.5.sp,
                color = paper.inkMuted,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = aliasInput,
                onValueChange = { aliasInput = it },
                label = { Text("Tu Alias en Fami (obligatorio)") },
                placeholder = { Text("Ej: Mamá, Carlos, Sofía...") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                isError = attemptedSubmit && !viewModel.isValidAlias(aliasInput),
                supportingText = if (attemptedSubmit && !viewModel.isValidAlias(aliasInput)) {
                    { Text("Escribe un alias de al menos 2 letras (sin correo)") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Surface(
                onClick = { if (biometricSupported) selectedOption = "biometric" },
                shape = RoundedCornerShape(16.dp),
                color = if (selectedOption == "biometric") paper.stickyMint else paper.sheetCream,
                border = BorderStroke(if (selectedOption == "biometric") 2.dp else 1.2.dp, if (selectedOption == "biometric") paper.border else paper.border.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = FamiCoral, modifier = Modifier.size(30.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Huella Digital / Biometría",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink
                        )
                        Text(
                            text = if (biometricSupported) "Acceso instantáneo al abrir Fami" else "No disponible en este dispositivo",
                            fontSize = 11.sp,
                            color = paper.inkMuted
                        )
                    }
                    if (selectedOption == "biometric") {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FamiEmerald)
                    }
                }
            }

            Surface(
                onClick = { selectedOption = "pin" },
                shape = RoundedCornerShape(16.dp),
                color = if (selectedOption == "pin") paper.stickyYellow else paper.sheetCream,
                border = BorderStroke(if (selectedOption == "pin") 2.dp else 1.2.dp, if (selectedOption == "pin") paper.border else paper.border.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = paper.ink, modifier = Modifier.size(28.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PIN Secreto de 5 dígitos",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink
                        )
                        Text(
                            text = "Ingresa con tu código numérico personal",
                            fontSize = 11.sp,
                            color = paper.inkMuted
                        )
                    }
                    if (selectedOption == "pin") {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FamiEmerald)
                    }
                }
            }

            OutlinedTextField(
                value = pinConfirm,
                onValueChange = { pinConfirm = it.filter { ch -> ch.isDigit() }.take(5) },
                label = {
                    Text(
                        when {
                            pinRequiredNow -> "Define tu PIN de 5 dígitos (obligatorio)"
                            alreadyHasPin -> "Cambiar PIN (opcional, ya configuraste uno)"
                            else -> "PIN de respaldo de 5 dígitos (opcional)"
                        }
                    )
                },
                isError = pinError,
                supportingText = if (pinError) {
                    { Text("Debe ser de 5 dígitos y distinto de 00000 (${pinConfirm.length}/5)") }
                } else null,
                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showPinConfirm = !showPinConfirm }) {
                        Icon(
                            if (showPinConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPinConfirm) "Ocultar PIN" else "Mostrar PIN"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                visualTransformation = if (showPinConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    attemptedSubmit = true
                    val cleanAlias = aliasInput.trim()
                    if (!viewModel.isValidAlias(cleanAlias)) {
                        Toast.makeText(context, "Escribe un alias válido (mín. 2 letras, sin correo)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (pinConfirm.isNotEmpty() && !viewModel.isValidPin(pinConfirm)) {
                        Toast.makeText(context, "El PIN debe tener 5 dígitos y no puede ser 00000", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (pinRequiredNow && !viewModel.isValidPin(pinConfirm)) {
                        Toast.makeText(context, "Define un PIN de 5 dígitos distinto de 00000", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val cleanPin = pinConfirm.takeIf { viewModel.isValidPin(it) }
                    if (selectedOption == "biometric" && biometricSupported && activity != null) {
                        showBiometricPrompt(activity, onSuccess = {
                            viewModel.setUnlockMethodAndEnter("biometric", cleanPin, customAlias = cleanAlias)
                        })
                    } else {
                        viewModel.setUnlockMethodAndEnter("pin", cleanPin, customAlias = cleanAlias)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamiCoral)
            ) {
                Text("Confirmar y Entrar a Fami", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// -------------------------------------------------------------
// UTILIDADES: BIOMETRÍA SEGURA POR NIVEL DE API (24+ Y 30+)
// -------------------------------------------------------------
internal tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

internal fun canAuthenticateBiometric(context: Context): Boolean {
    val bm = BiometricManager.from(context)
    val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else {
        BiometricManager.Authenticators.BIOMETRIC_WEAK
    }
    return bm.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
}

internal fun showBiometricPrompt(activity: FragmentActivity, onSuccess: () -> Unit) {
    if (!canAuthenticateBiometric(activity)) return

    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
        }
    )

    val builder = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Desbloquear Fami")
        .setSubtitle("Usa tu huella para acceder")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        builder.setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
    } else {
        builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("Usar PIN")
    }

    runCatching { biometricPrompt.authenticate(builder.build()) }
}
