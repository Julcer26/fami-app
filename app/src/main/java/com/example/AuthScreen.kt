package com.example

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
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

private const val PRIVACY_URL = "https://somosfami.app/privacy.html"
private const val TERMS_URL = "https://somosfami.app/terms.html"

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

    var currentMode by remember(familyId, pendingEmailVerification, needsUnlockMethodChoice, startInExistingLogin) {
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

    LaunchedEffect(startInExistingLogin) {
        if (startInExistingLogin) {
            currentMode = "login_existing"
            viewModel.consumeStartInExistingLogin()
        }
    }

    var googleJoinCodePending by remember { mutableStateOf("") }
    var googleErrorLocal by remember { mutableStateOf<String?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnSuccessListener { authRes ->
                            authRes.user?.let { user ->
                                viewModel.handleGoogleFirebaseUser(
                                    firebaseUser = user,
                                    joinFamilyCode = googleJoinCodePending,
                                    customFamilyName = pendingFamilyName
                                )
                            }
                        }
                        .addOnFailureListener { e ->
                            googleErrorLocal = e.localizedMessage ?: "No se pudo iniciar sesión con Google"
                        }
                } else {
                    googleErrorLocal = "No se obtuvo token de Google."
                }
            } catch (e: ApiException) {
                googleErrorLocal = "Error de Google Sign-In (código ${e.statusCode})"
            }
        }
    }

    fun launchGoogleAuth(joinCode: String = "") {
        googleJoinCodePending = joinCode
        googleErrorLocal = null
        viewModel.clearAuthError()
        val activity = context as? FragmentActivity
        val webClientResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (webClientResId != 0) {
            val webClientId = context.getString(webClientResId)
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(context, gso)
            client.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(client.signInIntent)
            }
        } else if (activity != null) {
            val provider = OAuthProvider.newBuilder("google.com").apply {
                addCustomParameter("prompt", "select_account")
            }
            FirebaseAuth.getInstance()
                .startActivityForSignInWithProvider(activity, provider.build())
                .addOnSuccessListener { authRes ->
                    authRes.user?.let { user ->
                        viewModel.handleGoogleFirebaseUser(
                            firebaseUser = user,
                            joinFamilyCode = joinCode,
                            customFamilyName = pendingFamilyName
                        )
                    }
                }
                .addOnFailureListener { e ->
                    googleErrorLocal = e.localizedMessage ?: "No se pudo completar el inicio con Google"
                }
        }
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
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 28.dp)
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
                            painter = painterResource(id = R.drawable.ic_fami_logo_wide),
                            contentDescription = "Logo Oficial Fami",
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = paper.stickyYellow,
                    border = BorderStroke(1.3.dp, paper.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = selectedAvatar.drawableRes),
                            contentDescription = selectedAvatar.labelEs,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                        )
                        Text(
                            text = "${if (managementMode == "individual") "👤 Individual" else "👨‍👩‍👧‍👦 Familiar"} • $familyRole",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink
                        )
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
                        onCreateFamily = {
                            viewModel.clearAuthError()
                            currentMode = "create_family"
                        },
                        onJoinFamily = {
                            viewModel.clearAuthError()
                            currentMode = "join_family"
                        },
                        onLoginExisting = {
                            viewModel.clearAuthError()
                            currentMode = "login_existing"
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
                        }
                    )
                    "create_family" -> CreateFamilyContent(
                        viewModel = viewModel,
                        initialFamilyName = pendingFamilyName,
                        managementMode = managementMode,
                        isLoading = isAuthLoading,
                        onBack = {
                            viewModel.clearAuthError()
                            currentMode = "select"
                        },
                        onSwitchToLogin = {
                            viewModel.clearAuthError()
                            currentMode = "login_existing"
                        },
                        onContinueWithGoogle = { launchGoogleAuth() }
                    )
                    "join_family" -> JoinFamilyContent(
                        viewModel = viewModel,
                        isLoading = isAuthLoading,
                        onBack = {
                            viewModel.clearAuthError()
                            currentMode = "select"
                        },
                        onForgotPassword = {
                            viewModel.clearAuthError()
                            currentMode = "forgot_password"
                        },
                        onContinueWithGoogle = { code -> launchGoogleAuth(joinCode = code) }
                    )
                    "login_existing" -> LoginExistingFamilyContent(
                        viewModel = viewModel,
                        isLoading = isAuthLoading,
                        onBack = {
                            viewModel.clearAuthError()
                            currentMode = "select"
                        },
                        onSwitchToRegister = {
                            viewModel.clearAuthError()
                            currentMode = "create_family"
                        },
                        onForgotPassword = {
                            viewModel.clearAuthError()
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
                        isLoading = isAuthLoading,
                        onBackToLogin = {
                            viewModel.clearAuthError()
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
        Surface(
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "G",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF4285F4)
                )
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
    onCreateFamily: () -> Unit,
    onJoinFamily: () -> Unit,
    onLoginExisting: () -> Unit,
    onContinueWithGoogle: () -> Unit
) {
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Bienvenido a Fami",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "Finanzas inteligentes y colaborativas para el hogar",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
        )

        FeatureHighlightPill(
            icon = Icons.Outlined.AutoAwesome,
            title = "Lector de Facturas IA",
            subtitle = "Escanea tickets y extrae productos y montos al instante con Gemini"
        )
        Spacer(modifier = Modifier.height(10.dp))
        FeatureHighlightPill(
            icon = Icons.Outlined.PeopleAlt,
            title = "Economía Familiar o Personal",
            subtitle = "Sincronización en tiempo real de gastos, billeteras, metas y tareas"
        )
        Spacer(modifier = Modifier.height(10.dp))
        FeatureHighlightPill(
            icon = Icons.Outlined.CurrencyExchange,
            title = "Multi-Moneda Automática",
            subtitle = "Control en moneda local y divisas con tasa oficial actualizada automáticamente"
        )

        Spacer(modifier = Modifier.height(22.dp))

        GoogleSignInButton(
            text = "Continuar con Google",
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
            Text("Crear Cuaderno con Correo", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

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
            Text("Tengo un Código Familiar", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onLoginExisting) {
            Text(
                text = "¿Ya tienes cuenta en Fami? Iniciar Sesión",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "somosfami.app • @hola.fami",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Al continuar aceptas nuestros ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "Términos",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_URL)))
                }
            )
            Text(" y ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "Privacidad",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
                }
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
    onReset: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val context = LocalContext.current as FragmentActivity

    LaunchedEffect(unlockMethod) {
        if (unlockMethod == "biometric") {
            showBiometricPrompt(context, onSuccess = {
                viewModel.loginBiometric()
            })
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
                    imageVector = if (unlockMethod == "biometric") Icons.Default.Fingerprint else Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Desbloquear Fami",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (unlockMethod == "biometric") {
                    "Usa tu huella digital o ingresa tu PIN de respaldo"
                } else {
                    "Ingresa tu PIN de 5 dígitos para abrir tu cuaderno"
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 5 && it.all { ch -> ch.isDigit() }) pin = it },
                label = { Text("PIN de seguridad (5 dígitos)") },
                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = {
                        showBiometricPrompt(context, onSuccess = {
                            viewModel.loginBiometric()
                        })
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = "Huella", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }

                Button(
                    onClick = {
                        if (pin.length == 5) {
                            viewModel.loginWithPin(pin) { ok ->
                                if (!ok) {
                                    Toast.makeText(context, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Introduce los 5 dígitos del PIN", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Entrar", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onReset) {
                Text("Cambiar de cuenta o usar otro método", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// -------------------------------------------------------------
// 3. CREAR NUEVO CUADERNO FAMI (GOOGLE O CORREO VERIFICADO)
// -------------------------------------------------------------
@Composable
private fun CreateFamilyContent(
    viewModel: FamilyViewModel,
    initialFamilyName: String = "",
    managementMode: String = "familiar",
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onSwitchToLogin: () -> Unit,
    onContinueWithGoogle: () -> Unit
) {
    val context = LocalContext.current
    var familyNameInput by remember(initialFamilyName) { mutableStateOf(initialFamilyName) }
    var userNameInput by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (managementMode == "individual") "Crea tu Cuaderno Personal" else "Crea tu Hogar Fami",
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
                onClick = onContinueWithGoogle
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
                label = { Text(if (managementMode == "individual") "Nombre del Cuaderno" else "Nombre de la Familia / Hogar") },
                placeholder = { Text(if (managementMode == "individual") "Mi Cuaderno Fami" else "Ej: Familia Pérez") },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = userNameInput,
                onValueChange = { userNameInput = it },
                label = { Text("Tu Nombre o Alias") },
                placeholder = { Text("Ej: Mamá, Carlos, Sofía...") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico Válido") },
                placeholder = { Text("tucorreo@gmail.com") },
                supportingText = { Text("Te enviaremos un correo para verificar tu cuenta y recuperarla si olvidas tu clave") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña de Acceso") },
                supportingText = { Text("Mínimo 6 caracteres") },
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
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 5 && it.all { ch -> ch.isDigit() }) pin = it },
                label = { Text("PIN de Respaldo (5 dígitos)") },
                supportingText = { Text("Luego elegirás si entrar con Huella o PIN") },
                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (!viewModel.isValidEmailFormat(email)) {
                        Toast.makeText(context, "Ingresa un correo electrónico real y válido", Toast.LENGTH_SHORT).show()
                    } else if (password.length < 6) {
                        Toast.makeText(context, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                    } else if (pin.length != 5) {
                        Toast.makeText(context, "El PIN debe ser exactamente de 5 dígitos", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.createFamily(
                            email = email.trim(),
                            pin = pin,
                            password = password,
                            customFamilyName = familyNameInput.trim(),
                            customUserName = userNameInput.trim()
                        )
                    }
                },
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
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = onSwitchToLogin) {
                    Text("¿Ya tienes cuenta? Iniciar Sesión", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
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
    onForgotPassword: () -> Unit,
    onContinueWithGoogle: (String) -> Unit
) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
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
                onValueChange = { code = it },
                label = { Text("Código Familiar (Ej: A1B2C3D4)") },
                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            GoogleSignInButton(
                text = "Unirme con Google",
                enabled = !isLoading,
                onClick = {
                    if (code.isBlank()) {
                        Toast.makeText(context, "Primero ingresa el código familiar arriba", Toast.LENGTH_SHORT).show()
                    } else {
                        onContinueWithGoogle(code.trim())
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
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Tu Correo Electrónico Válido") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña (mínimo 6 caracteres)") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 5 && it.all { ch -> ch.isDigit() }) pin = it },
                label = { Text("Crea tu PIN de 5 dígitos") },
                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (code.isBlank() || name.isBlank() || !viewModel.isValidEmailFormat(email) || password.length < 6 || pin.length != 5) {
                        Toast.makeText(context, "Completa todos los campos con un correo válido y PIN de 5 dígitos", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.joinFamily(code.trim(), name.trim(), pin, email.trim(), password)
                    }
                },
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
                TextButton(onClick = onForgotPassword) {
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
    onForgotPassword: () -> Unit,
    onContinueWithGoogle: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
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
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 5 && it.all { ch -> ch.isDigit() }) pin = it },
                label = { Text("PIN Rápido para este dispositivo (5 dígitos)") },
                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (viewModel.isValidEmailFormat(email) && password.length >= 6 && pin.length == 5) {
                        viewModel.loginExisting(email.trim(), password, pin)
                    } else {
                        Toast.makeText(context, "Ingresa correo válido, contraseña y PIN de 5 dígitos", Toast.LENGTH_SHORT).show()
                    }
                },
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
                TextButton(onClick = onForgotPassword) {
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
    var resendCooldown by remember { mutableIntStateOf(45) }

    LaunchedEffect(Unit) {
        while (resendCooldown > 0) {
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
                text = "Enviamos un enlace de aprobación oficial a:\n$email\n\nÁbrelo desde tu bandeja de entrada (o Spam) para confirmar que es un correo válido y poder recuperar tu cuenta si olvidas la clave.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

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
    isLoading: Boolean,
    onBackToLogin: () -> Unit
) {
    val paper = FamiTheme.paper
    var email by remember { mutableStateOf("") }
    var emailSent by remember { mutableStateOf(false) }
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        viewModel.sendPasswordReset(email) {
                            emailSent = true
                        }
                    },
                    enabled = !isLoading,
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
                            Icon(if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
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
    val context = LocalContext.current as FragmentActivity
    val biometricSupported = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }
    var selectedOption by remember { mutableStateOf(if (biometricSupported) "biometric" else "pin") }
    var pinConfirm by remember { mutableStateOf("") }

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
            Text(
                text = "🔐 Elige cómo entrar a Fami",
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Tu cuenta ya está lista. Elige tu método preferido para abrir la app rápido la próxima vez:",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Surface(
                onClick = { if (biometricSupported) selectedOption = "biometric" },
                shape = RoundedCornerShape(16.dp),
                color = if (selectedOption == "biometric") paper.stickyMint else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(if (selectedOption == "biometric") 2.dp else 1.dp, if (selectedOption == "biometric") paper.border else Color.Transparent),
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
                            color = if (selectedOption == "biometric") paper.ink else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (biometricSupported) "Acceso instantáneo al abrir Fami" else "No disponible en este dispositivo",
                            fontSize = 11.sp,
                            color = if (selectedOption == "biometric") paper.ink.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
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
                color = if (selectedOption == "pin") paper.stickyYellow else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(if (selectedOption == "pin") 2.dp else 1.dp, if (selectedOption == "pin") paper.border else Color.Transparent),
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
                            color = if (selectedOption == "pin") paper.ink else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ingresa con tu código numérico personal",
                            fontSize = 11.sp,
                            color = if (selectedOption == "pin") paper.ink.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selectedOption == "pin") {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FamiEmerald)
                    }
                }
            }

            OutlinedTextField(
                value = pinConfirm,
                onValueChange = { if (it.length <= 5 && it.all { ch -> ch.isDigit() }) pinConfirm = it },
                label = { Text(if (selectedOption == "pin") "Define tu PIN de 5 dígitos" else "PIN de respaldo (opcional si ya lo creaste)") },
                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val cleanPin = pinConfirm.takeIf { it.length == 5 }
                    if (selectedOption == "biometric" && biometricSupported) {
                        showBiometricPrompt(context, onSuccess = {
                            viewModel.setUnlockMethodAndEnter("biometric", cleanPin)
                        })
                    } else {
                        if (selectedOption == "pin" && pinConfirm.isNotEmpty() && pinConfirm.length != 5) {
                            Toast.makeText(context, "El PIN debe tener 5 dígitos", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.setUnlockMethodAndEnter("pin", cleanPin)
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamiCoral)
            ) {
                Text("Confirmar y Abrir mi Cuaderno", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// -------------------------------------------------------------
// UTILIDAD: PROMPT BIOMÉTRICO (HUELLA / ROSTRO)
// -------------------------------------------------------------
private fun showBiometricPrompt(activity: FragmentActivity, onSuccess: () -> Unit) {
    val biometricManager = BiometricManager.from(activity)
    if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) != BiometricManager.BIOMETRIC_SUCCESS) {
        return
    }

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

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Desbloquear Fami")
        .setSubtitle("Usa tu huella para acceder")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()

    biometricPrompt.authenticate(promptInfo)
}
