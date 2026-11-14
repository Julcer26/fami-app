package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.api.BcvExchangeRateService
import com.example.currency.CountryCurrencyConfig
import com.example.currency.CountryInfo
import com.example.currency.LanguageOption
import com.example.database.Wallet
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

private data class DraftWallet(
    val id: String = UUID.randomUUID().toString().take(8),
    val name: String,
    val currency: String,
    val balanceText: String,
    val isLocal: Boolean
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initialCountryCode: String = "VE",
    initialPrimaryCurrency: String = "",
    initialSecondaryCurrency: String = "USD",
    initialLanguage: String = CountryCurrencyConfig.detectSystemLanguageCode(),
    initialFamilyRole: String = "",
    initialManagementMode: String = "familiar",
    initialFamilyAction: String = "create",
    initialFamilyName: String = "",
    initialUserAlias: String = "",
    initialAvatarId: String = "woman_1",
    onLoginExisting: () -> Unit = {},
    onComplete: (
        selectedCountry: CountryInfo,
        primaryCurrency: String,
        secondaryCurrency: String,
        languageCode: String,
        familyRole: String,
        managementMode: String,
        familyAction: String,
        familyName: String,
        avatarId: String,
        monthlyIncome: Double,
        monthlyIncomeCurrency: String,
        initialWallets: List<Wallet>,
        userAlias: String
    ) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val paper = FamiTheme.paper

    var selectedCountry by remember {
        mutableStateOf(CountryCurrencyConfig.getCountryByCode(initialCountryCode))
    }
    var userManuallyPickedCountry by remember { mutableStateOf(false) }
    var ipDetectionState by remember { mutableStateOf("loading") } // loading, detected, fallback
    var selectedLanguage by remember { mutableStateOf(initialLanguage) }
    var selectedPrimaryCurrency by remember {
        mutableStateOf(initialPrimaryCurrency.ifBlank { selectedCountry.localCurrency })
    }
    var selectedSecondaryCurrency by remember {
        mutableStateOf(initialSecondaryCurrency.ifBlank { "USD" })
    }

    // Gestión Familiar / Individual + Acción + Roles
    var managementMode by remember { mutableStateOf(initialManagementMode.ifBlank { "familiar" }) }
    var familyAction by remember { mutableStateOf(initialFamilyAction.ifBlank { "create" }) }
    var familyNameInput by remember { mutableStateOf(initialFamilyName) }
    var userAliasInput by remember { mutableStateOf(initialUserAlias) }
    var selectedRole by remember { mutableStateOf(initialFamilyRole.ifBlank { "Administrador" }) }
    var customRoleInput by remember { mutableStateOf("") }

    // Ingreso mensual y múltiples billeteras simultáneas
    var monthlyIncomeInput by remember { mutableStateOf("") }
    var monthlyIncomeCurrency by remember { mutableStateOf(selectedPrimaryCurrency) }
    var draftWallets by remember {
        mutableStateOf(
            buildList {
                add(
                    DraftWallet(
                        name = "Billetera Principal",
                        currency = initialPrimaryCurrency.ifBlank { selectedCountry.localCurrency },
                        balanceText = "",
                        isLocal = true
                    )
                )
                if (!initialSecondaryCurrency.equals("NONE", ignoreCase = true) &&
                    !initialSecondaryCurrency.equals(initialPrimaryCurrency.ifBlank { selectedCountry.localCurrency }, ignoreCase = true)
                ) {
                    add(
                        DraftWallet(
                            name = "Billetera Divisas",
                            currency = initialSecondaryCurrency.ifBlank { "USD" },
                            balanceText = "",
                            isLocal = false
                        )
                    )
                }
            }
        )
    }

    // Selector de Avatar Nano Banana (12 disponibles)
    var selectedAvatarId by remember { mutableStateOf(initialAvatarId.ifBlank { "woman_1" }) }
    var avatarFilter by remember { mutableStateOf("Todos") }

    // Diálogos
    var showCountryDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPrimaryCurrencyDialog by remember { mutableStateOf(false) }
    var showSecondaryCurrencyDialog by remember { mutableStateOf(false) }
    var showIncomeCurrencyDialog by remember { mutableStateOf(false) }
    var editingWalletCurrencyIdx by remember { mutableStateOf<Int?>(null) }

    // Detección automática de país por IP al abrir la app
    LaunchedEffect(Unit) {
        val detectedCode = BcvExchangeRateService.detectCountryCodeByIp()
            ?: Locale.getDefault().country.takeIf { it.length == 2 }?.uppercase()
        if (!userManuallyPickedCountry && !detectedCode.isNullOrBlank()) {
            val found = CountryCurrencyConfig.SUPPORTED_COUNTRIES.find {
                it.code.equals(detectedCode, ignoreCase = true)
            }
            if (found != null) {
                selectedCountry = found
                selectedPrimaryCurrency = found.localCurrency
                monthlyIncomeCurrency = found.localCurrency
                if (draftWallets.isNotEmpty()) {
                    draftWallets = draftWallets.mapIndexed { idx, w ->
                        if (idx == 0 && w.isLocal) w.copy(currency = found.localCurrency) else w
                    }
                }
                ipDetectionState = "detected"
            } else {
                ipDetectionState = "fallback"
            }
        } else {
            ipDetectionState = if (detectedCode != null) "detected" else "fallback"
        }
    }

    val strings = remember(selectedLanguage) { FamiStrings.get(selectedLanguage) }
    val isEn = selectedLanguage.lowercase() == "en"
    val accentCoralText = if (paper.isDark) FamiCoral else FamiCoralDeep
    val totalPages = 5
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val finalRole = customRoleInput.trim().ifBlank { selectedRole }

    val buildFinalWallets: () -> List<Wallet> = {
        draftWallets.mapNotNull { dw ->
            val cleanName = dw.name.trim().ifBlank { "Billetera ${dw.currency}" }
            val bal = dw.balanceText.replace(",", ".").toDoubleOrNull() ?: 0.0
            Wallet(
                id = dw.id,
                name = cleanName,
                currency = dw.currency,
                balance = bal,
                isLocal = dw.isLocal || dw.currency.equals(selectedPrimaryCurrency, ignoreCase = true)
            )
        }
    }

    val finishOnboarding: () -> Unit = {
        val parsedIncome = monthlyIncomeInput.replace(",", ".").toDoubleOrNull() ?: 0.0
        onComplete(
            selectedCountry,
            selectedPrimaryCurrency,
            selectedSecondaryCurrency,
            selectedLanguage,
            finalRole,
            managementMode,
            if (managementMode == "individual") "create" else familyAction,
            familyNameInput.trim(),
            selectedAvatarId,
            parsedIncome,
            monthlyIncomeCurrency.ifBlank { selectedPrimaryCurrency },
            buildFinalWallets(),
            userAliasInput.trim()
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(paper.deskBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera superior con Logo Oficial Fami + Indicador de Hoja + Saltar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_fami_logo_wide),
                        contentDescription = "Logo Oficial Fami",
                        modifier = Modifier.height(26.dp),
                        contentScale = ContentScale.FillHeight
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = paper.stickyYellow,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.2.dp, paper.border)
                    ) {
                        Text(
                            text = if (isEn) "SHEET ${pagerState.currentPage + 1}/$totalPages" else "HOJA ${pagerState.currentPage + 1}/$totalPages",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = paper.ink,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                if (pagerState.currentPage < totalPages - 1) {
                    Tactile3DButton(
                        onClick = finishOnboarding,
                        faceColor = paper.sheet,
                        lipColor = paper.lipPaper,
                        lipHeight = 3.dp,
                        cornerRadius = 10.dp,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(strings.skipBtn, color = paper.ink, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                NotebookPaperSheet(
                    modifier = Modifier.fillMaxSize(),
                    paperColor = paper.sheet,
                    showSpiralTop = true,
                    showRuledLines = false,
                    showMarginLine = true,
                    cornerRadius = 18.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 28.dp, end = 16.dp, top = 8.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (page) {
                            // ==============================================================
                            // HOJA 1: BIENVENIDA CON MASCOTA + ACCESO DIRECTO + PAÍS + IDIOMA
                            // ==============================================================
                            0 -> {
                                Surface(
                                    color = paper.stickyPeach,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.2.dp, paper.border)
                                ) {
                                    Text(
                                        text = if (isEn) "📓 PAGE 1 • MEET YOUR FAMI NOTEBOOK" else "📓 HOJA 1 • BIENVENIDA AL CUADERNO FAMI",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = accentCoralText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(155.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(paper.sheetCream)
                                        .border(1.8.dp, paper.border, RoundedCornerShape(16.dp))
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.fami_onboarding_1),
                                        contentDescription = "Mascota Fami",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                Text(
                                    text = if (isEn) "Hi! I'm Fami 🐾" else "¡Hola! Soy Fami 🐾",
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )

                                RuledNotebookText(
                                    text = if (isEn)
                                        "Let's set up your family or personal ledger sheet by sheet. Everything adapts to your country and currency."
                                    else
                                        "Vamos a preparar tu cuaderno familiar o personal hoja por hoja. Todo se adapta a tu país, idioma y monedas.",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = paper.inkMuted,
                                    lineHeight = 24.sp
                                )

                                // Acceso directo para usuarios que ya tienen cuenta creada
                                Tactile3DButton(
                                    onClick = onLoginExisting,
                                    modifier = Modifier.fillMaxWidth(),
                                    faceColor = paper.stickyYellow,
                                    lipColor = Lip3DGold,
                                    lipHeight = 3.dp,
                                    cornerRadius = 12.dp,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        text = when (selectedLanguage.lowercase()) {
                                            "en" -> "🔑 Already have an account? Sign In"
                                            "pt" -> "🔑 Já tem conta? Entrar"
                                            else -> "🔑 ¿Ya tienes cuenta? Iniciar Sesión"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = paper.ink
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (ipDetectionState == "detected") paper.stickyMint else paper.stickyYellow)
                                        .border(1.4.dp, paper.border, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = when (ipDetectionState) {
                                            "loading" -> "🌐 ${strings.ipDetecting}"
                                            else -> "📍 ${strings.ipDetectedPrefix} ${selectedCountry.flag} ${selectedCountry.name} (${selectedCountry.localCurrency})"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = paper.ink
                                    )
                                }

                                NotebookDropdownField(
                                    label = "🌍 ${strings.countryLabel}",
                                    value = "${selectedCountry.flag} ${selectedCountry.name} (${selectedCountry.code})",
                                    subtitle = "${selectedCountry.rateProvider} • ${CountryCurrencyConfig.SUPPORTED_COUNTRIES.size} países",
                                    onClick = { showCountryDialog = true }
                                )

                                val langOption = remember(selectedLanguage) {
                                    CountryCurrencyConfig.getLanguageOption(selectedLanguage)
                                }
                                NotebookDropdownField(
                                    label = "🗣️ ${strings.languageLabel}",
                                    value = "${langOption.flag} ${langOption.nativeName}",
                                    subtitle = if (selectedLanguage == CountryCurrencyConfig.detectSystemLanguageCode())
                                        "Automático del teléfono (${langOption.name})" else "Idioma seleccionado manualmente",
                                    onClick = { showLanguageDialog = true }
                                )
                            }

                            // ==============================================================
                            // HOJA 2: MONEDAS PREFERIDAS (LOCAL + EXTRA OPCIONAL)
                            // ==============================================================
                            1 -> {
                                Surface(
                                    color = paper.stickyMint,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.2.dp, paper.border)
                                ) {
                                    Text(
                                        text = if (isEn) "💱 PAGE 2 • PREFERRED CURRENCIES" else "💱 HOJA 2 • MONEDAS PREFERIDAS",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = paper.ink,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(paper.sheetCream)
                                            .border(1.6.dp, paper.border, RoundedCornerShape(14.dp))
                                            .padding(4.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.fami_onboarding_2),
                                            contentDescription = "Monedas Fami",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isEn) "Local + Optional Extra" else "Moneda Local + Extra Opcional",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = paper.ink
                                        )
                                        Text(
                                            text = if (isEn) "Track amounts in your local currency and convert live if you want."
                                            else "Anota en tu moneda local y activa una segunda divisa para convertir en vivo.",
                                            fontSize = 11.5.sp,
                                            color = paper.inkMuted,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                val primSym = CountryCurrencyConfig.getCurrencySymbol(selectedPrimaryCurrency)
                                NotebookDropdownField(
                                    label = "💵 ${strings.primaryCurrencyLabel}",
                                    value = "$selectedPrimaryCurrency ($primSym)",
                                    subtitle = "Basada en ${selectedCountry.flag} ${selectedCountry.name} (tocar para cambiar)",
                                    onClick = { showPrimaryCurrencyDialog = true }
                                )

                                val secValue = if (selectedSecondaryCurrency == "NONE") {
                                    "🚫 Oculta (Solo moneda local)"
                                } else {
                                    "$selectedSecondaryCurrency (${CountryCurrencyConfig.getCurrencySymbol(selectedSecondaryCurrency)})"
                                }
                                NotebookDropdownField(
                                    label = "⇄ ${strings.secondaryCurrencyLabel} (Opcional)",
                                    value = secValue,
                                    subtitle = if (selectedSecondaryCurrency == "NONE")
                                        "Sin conversión secundaria en pantalla"
                                    else
                                        "Verás tus montos convertidos entre $selectedPrimaryCurrency y $selectedSecondaryCurrency",
                                    onClick = { showSecondaryCurrencyDialog = true }
                                )

                                Text(
                                    text = "⚡ Atajos rápidos para moneda de conversión:",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("USD" to "🇺🇸 USD ($)", "EUR" to "🇪🇺 EUR (€)", "NONE" to "🚫 Solo Local").forEach { (code, label) ->
                                        val active = selectedSecondaryCurrency.equals(code, ignoreCase = true)
                                        Tactile3DButton(
                                            onClick = {
                                                selectedSecondaryCurrency = code
                                                if (code != "NONE" && draftWallets.none { it.currency.equals(code, ignoreCase = true) }) {
                                                    draftWallets = draftWallets + DraftWallet(
                                                        name = "Billetera $code",
                                                        currency = code,
                                                        balanceText = "",
                                                        isLocal = false
                                                    )
                                                }
                                            },
                                            faceColor = if (active) FamiCoral else paper.sheetCream,
                                            lipColor = if (active) Lip3DCoral else paper.lipPaper,
                                            lipHeight = 2.5.dp,
                                            cornerRadius = 10.dp,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (active) Color.White else paper.ink
                                            )
                                        }
                                    }
                                }

                                StickyNoteSheet(
                                    modifier = Modifier.fillMaxWidth(),
                                    noteColor = paper.stickyYellow
                                ) {
                                    RuledNotebookText(
                                        text = if (selectedSecondaryCurrency == "NONE")
                                            "Modo simple activo: solo se mostrará tu moneda local ($selectedPrimaryCurrency)."
                                        else
                                            "Conversión activa: tus gastos en $selectedPrimaryCurrency mostrarán equivalente en $selectedSecondaryCurrency.",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = paper.ink,
                                        lineHeight = 24.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            // ==============================================================
                            // HOJA 3: MODO FAMILIAR VS INDIVIDUAL + CREAR/UNIRSE + ROLES
                            // ==============================================================
                            2 -> {
                                Surface(
                                    color = paper.stickyYellow,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.2.dp, paper.border)
                                ) {
                                    Text(
                                        text = if (isEn) "🏡 PAGE 3 • HOUSEHOLD MODE & ROLE" else "🏡 HOJA 3 • MODO DE USO, FAMILIA Y ROL",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = paper.ink,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = if (isEn) "How will you use Fami?" else "¿Cómo vas a llevar este cuaderno?",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isFam = managementMode == "familiar"
                                    Tactile3DButton(
                                        onClick = { managementMode = "familiar" },
                                        modifier = Modifier.weight(1f),
                                        faceColor = if (isFam) FamiCoral else paper.sheetCream,
                                        lipColor = if (isFam) Lip3DCoral else paper.lipPaper,
                                        lipHeight = 3.5.dp,
                                        cornerRadius = 12.dp,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("👨‍👩‍👧‍👦 Familiar", fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (isFam) Color.White else paper.ink)
                                            Text("Compartido en casa", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = if (isFam) Color.White.copy(alpha = 0.9f) else paper.inkMuted)
                                        }
                                    }

                                    val isInd = managementMode == "individual"
                                    Tactile3DButton(
                                        onClick = {
                                            managementMode = "individual"
                                            familyAction = "create"
                                            selectedRole = "Administrador"
                                        },
                                        modifier = Modifier.weight(1f),
                                        faceColor = if (isInd) FamiMidnight else paper.sheetCream,
                                        lipColor = if (isInd) paper.lipNavy else paper.lipPaper,
                                        lipHeight = 3.5.dp,
                                        cornerRadius = 12.dp,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("👤 Individual", fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (isInd) Color.White else paper.ink)
                                            Text("Solo mis finanzas", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = if (isInd) Color.White.copy(alpha = 0.9f) else paper.inkMuted)
                                        }
                                    }
                                }

                                if (managementMode == "familiar") {
                                    Text(
                                        text = "¿Qué deseas hacer ahora?",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = paper.ink
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val isCreate = familyAction == "create"
                                        Tactile3DButton(
                                            onClick = {
                                                familyAction = "create"
                                                selectedRole = "Administrador"
                                            },
                                            modifier = Modifier.weight(1f),
                                            faceColor = if (isCreate) paper.stickyMint else paper.sheetCream,
                                            lipColor = if (isCreate) Lip3DMint else paper.lipPaper,
                                            lipHeight = 3.dp,
                                            cornerRadius = 10.dp,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                        ) {
                                            Text("✨ Crear Familia", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = paper.ink)
                                        }

                                        val isJoin = familyAction == "join"
                                        Tactile3DButton(
                                            onClick = {
                                                familyAction = "join"
                                                if (selectedRole == "Administrador") selectedRole = "Miembro familiar"
                                            },
                                            modifier = Modifier.weight(1f),
                                            faceColor = if (isJoin) paper.stickyPeach else paper.sheetCream,
                                            lipColor = if (isJoin) Lip3DCoral else paper.lipPaper,
                                            lipHeight = 3.dp,
                                            cornerRadius = 10.dp,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                        ) {
                                            Text("🔑 Unirme con Código", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = paper.ink)
                                        }
                                    }
                                }

                                if (managementMode == "individual" || familyAction == "create") {
                                    OutlinedTextField(
                                        value = familyNameInput,
                                        onValueChange = { familyNameInput = it },
                                        label = {
                                            Text(
                                                if (managementMode == "individual") "Nombre de tu Cuaderno Personal" else "Nombre de la Familia / Hogar",
                                                fontSize = 11.sp
                                            )
                                        },
                                        placeholder = {
                                            Text(
                                                if (managementMode == "individual") "Ej: Mis Finanzas" else "Ej: Familia Pérez García",
                                                fontSize = 11.sp,
                                                color = paper.inkMuted
                                            )
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = FamiCoral,
                                            unfocusedBorderColor = paper.border.copy(alpha = 0.5f),
                                            focusedContainerColor = paper.sheetCream,
                                            unfocusedContainerColor = paper.sheetCream,
                                            focusedTextColor = paper.ink,
                                            unfocusedTextColor = paper.ink
                                        )
                                    )
                                } else {
                                    StickyNoteSheet(
                                        modifier = Modifier.fillMaxWidth(),
                                        noteColor = paper.stickyMint
                                    ) {
                                        Text(
                                            text = "📲 Al terminar las hojas podrás escribir el Código Familiar que te dio el Administrador para sincronizarte al instante.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = paper.ink,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = userAliasInput,
                                    onValueChange = { userAliasInput = it },
                                    label = { Text(if (isEn) "Your Alias / Nickname (shown in app)" else "Tu Nombre o Alias (como aparecerás en la app)", fontSize = 11.sp) },
                                    placeholder = { Text(if (isEn) "e.g., Mom, Alex, Sofia..." else "Ej: Mamá, Carlos, Sofía...", fontSize = 11.sp, color = paper.inkMuted) },
                                    supportingText = {
                                        Text(
                                            if (isEn) "We will always show this alias instead of your email" else "Usaremos este alias en todas las notas y tareas en lugar de tu correo",
                                            fontSize = 10.sp,
                                            color = paper.inkMuted
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = FamiCoral,
                                        unfocusedBorderColor = paper.border.copy(alpha = 0.5f),
                                        focusedContainerColor = paper.sheetCream,
                                        unfocusedContainerColor = paper.sheetCream,
                                        focusedTextColor = paper.ink,
                                        unfocusedTextColor = paper.ink
                                    )
                                )

                                Text(
                                    text = "👑 ${strings.familyRoleLabel}:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CountryCurrencyConfig.FAMILY_ROLES.forEach { roleOpt ->
                                        val label = when (selectedLanguage) {
                                            "en" -> roleOpt.labelEn
                                            "pt" -> roleOpt.labelPt
                                            else -> roleOpt.labelEs
                                        }
                                        val isChosen = customRoleInput.isBlank() && selectedRole.equals(label, ignoreCase = true)
                                        Tactile3DButton(
                                            onClick = {
                                                selectedRole = label
                                                customRoleInput = ""
                                            },
                                            faceColor = if (isChosen) FamiCoral else paper.sheetCream,
                                            lipColor = if (isChosen) Lip3DCoral else paper.lipPaper,
                                            lipHeight = 2.5.dp,
                                            cornerRadius = 10.dp,
                                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "${roleOpt.emoji} $label",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isChosen) Color.White else paper.ink
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = customRoleInput,
                                    onValueChange = { customRoleInput = it },
                                    placeholder = { Text(strings.customRoleHint, fontSize = 11.sp, color = paper.inkMuted) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = FamiCoral,
                                        unfocusedBorderColor = paper.border.copy(alpha = 0.45f),
                                        focusedContainerColor = paper.sheetCream,
                                        unfocusedContainerColor = paper.sheetCream,
                                        focusedTextColor = paper.ink,
                                        unfocusedTextColor = paper.ink
                                    )
                                )
                            }

                            // ==============================================================
                            // HOJA 4: INGRESO MENSUAL Y MÚLTIPLES BILLETERAS SIMULTÁNEAS
                            // ==============================================================
                            3 -> {
                                Surface(
                                    color = paper.stickyMint,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.2.dp, paper.border)
                                ) {
                                    Text(
                                        text = if (isEn) "💼 PAGE 4 • INCOME & MULTI-WALLETS" else "💼 HOJA 4 • INGRESO MENSUAL Y BILLETERAS",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = paper.ink,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = if (isEn) "Income & Wallets" else "Tu Ingreso y Billeteras",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )

                                Text(
                                    text = if (isEn) "Set an approximate monthly income and manage local & foreign wallets at the same time."
                                    else "Define tu ingreso mensual aproximado y configura billeteras en moneda local y extranjera simultáneamente.",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = paper.inkMuted
                                )

                                // Ingreso mensual + Moneda
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = monthlyIncomeInput,
                                        onValueChange = { monthlyIncomeInput = it },
                                        label = { Text("Ingreso Mensual", fontSize = 11.sp) },
                                        placeholder = { Text("Ej: 1500", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1.4f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = FamiCoral,
                                            unfocusedBorderColor = paper.border.copy(alpha = 0.5f),
                                            focusedContainerColor = paper.sheetCream,
                                            unfocusedContainerColor = paper.sheetCream,
                                            focusedTextColor = paper.ink,
                                            unfocusedTextColor = paper.ink
                                        )
                                    )

                                    Tactile3DButton(
                                        onClick = { showIncomeCurrencyDialog = true },
                                        faceColor = paper.stickyYellow,
                                        lipColor = Lip3DGold,
                                        lipHeight = 3.dp,
                                        cornerRadius = 12.dp,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
                                    ) {
                                        Text(
                                            text = "$monthlyIncomeCurrency ▼",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = paper.ink
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "👛 Billeteras (${draftWallets.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = paper.ink
                                    )
                                    Tactile3DButton(
                                        onClick = {
                                            val defaultExtra = if (selectedSecondaryCurrency != "NONE") selectedSecondaryCurrency else "USD"
                                            draftWallets = draftWallets + DraftWallet(
                                                name = "Billetera ${draftWallets.size + 1}",
                                                currency = defaultExtra,
                                                balanceText = "",
                                                isLocal = false
                                            )
                                        },
                                        faceColor = paper.stickyMint,
                                        lipColor = Lip3DMint,
                                        lipHeight = 2.5.dp,
                                        cornerRadius = 10.dp,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = paper.ink, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Agregar Billetera", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = paper.ink)
                                    }
                                }

                                draftWallets.forEachIndexed { idx, w ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (w.isLocal) paper.stickyPeach.copy(alpha = if (paper.isDark) 0.75f else 0.65f) else paper.sheetCream,
                                        border = BorderStroke(1.4.dp, paper.border)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (w.isLocal) "🏠 MONEDA LOCAL" else "🌐 DIVISA / EXTRA",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = accentCoralText
                                                )
                                                if (draftWallets.size > 1) {
                                                    IconButton(
                                                        onClick = {
                                                            draftWallets = draftWallets.filterIndexed { i, _ -> i != idx }
                                                        },
                                                        modifier = Modifier.size(22.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = FamiAlertRed, modifier = Modifier.size(15.dp))
                                                    }
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = w.name,
                                                    onValueChange = { newVal ->
                                                        draftWallets = draftWallets.mapIndexed { i, old -> if (i == idx) old.copy(name = newVal) else old }
                                                    },
                                                    label = { Text("Nombre", fontSize = 10.sp) },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1.3f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = FamiCoral,
                                                        unfocusedBorderColor = paper.border.copy(alpha = 0.4f),
                                                        focusedContainerColor = paper.sheet,
                                                        unfocusedContainerColor = paper.sheet,
                                                        focusedTextColor = paper.ink,
                                                        unfocusedTextColor = paper.ink
                                                    )
                                                )
                                                OutlinedTextField(
                                                    value = w.balanceText,
                                                    onValueChange = { newVal ->
                                                        draftWallets = draftWallets.mapIndexed { i, old -> if (i == idx) old.copy(balanceText = newVal) else old }
                                                    },
                                                    label = { Text("Saldo", fontSize = 10.sp) },
                                                    placeholder = { Text("0.00", fontSize = 10.sp) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = FamiCoral,
                                                        unfocusedBorderColor = paper.border.copy(alpha = 0.4f),
                                                        focusedContainerColor = paper.sheet,
                                                        unfocusedContainerColor = paper.sheet,
                                                        focusedTextColor = paper.ink,
                                                        unfocusedTextColor = paper.ink
                                                    )
                                                )
                                                Tactile3DButton(
                                                    onClick = { editingWalletCurrencyIdx = idx },
                                                    faceColor = paper.stickyYellow,
                                                    lipColor = Lip3DGold,
                                                    lipHeight = 2.5.dp,
                                                    cornerRadius = 10.dp,
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                                                ) {
                                                    Text(w.currency, fontSize = 11.sp, fontWeight = FontWeight.Black, color = paper.ink)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // ==============================================================
                            // HOJA 5: SELECTOR DE 12 AVATARES NANO BANANA (M/F, ADULTOS/MENORES)
                            // ==============================================================
                            4 -> {
                                Surface(
                                    color = paper.stickyPeach,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.2.dp, paper.border)
                                ) {
                                    Text(
                                        text = if (isEn) "🎨 PAGE 5 • CHOOSE YOUR AVATAR" else "🎨 HOJA 5 • ELIGE TU AVATAR DE CUENTA",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = accentCoralText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = if (isEn) "Pick Your Fami Character" else "¿Cuál te representa mejor?",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )

                                // Filtros por Adultos / Menores / F / M
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Todos", "Adultos", "Menores", "Femenino", "Masculino").forEach { filter ->
                                        val active = avatarFilter == filter
                                        Tactile3DButton(
                                            onClick = { avatarFilter = filter },
                                            faceColor = if (active) FamiCoral else paper.sheetCream,
                                            lipColor = if (active) Lip3DCoral else paper.lipPaper,
                                            lipHeight = 2.dp,
                                            cornerRadius = 9.dp,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = filter,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (active) Color.White else paper.ink
                                            )
                                        }
                                    }
                                }

                                val shownAvatars = remember(avatarFilter) {
                                    when (avatarFilter) {
                                        "Adultos" -> AvatarCatalog.ALL.filter { it.ageGroup == "Adulto" }
                                        "Menores" -> AvatarCatalog.ALL.filter { it.ageGroup == "Menor" }
                                        "Femenino" -> AvatarCatalog.ALL.filter { it.gender == "F" }
                                        "Masculino" -> AvatarCatalog.ALL.filter { it.gender == "M" }
                                        else -> AvatarCatalog.ALL
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    shownAvatars.chunked(3).forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowItems.forEach { av ->
                                                val isSel = selectedAvatarId == av.id
                                                Surface(
                                                    onClick = { selectedAvatarId = av.id },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = if (isSel) paper.stickyYellow else paper.sheetCream,
                                                    border = BorderStroke(if (isSel) 2.2.dp else 1.2.dp, if (isSel) FamiCoral else paper.border)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(vertical = 8.dp, horizontal = 6.dp)
                                                            .fillMaxWidth(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(64.dp)
                                                                .clip(CircleShape)
                                                                .background(paper.sheet)
                                                                .border(
                                                                    width = if (isSel) 2.dp else 1.4.dp,
                                                                    color = if (isSel) FamiCoral else paper.border,
                                                                    shape = CircleShape
                                                                )
                                                        ) {
                                                            Image(
                                                                painter = painterResource(id = av.drawableRes),
                                                                contentDescription = av.labelEs,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            repeat(3 - rowItems.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Indicador de Hojas
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(totalPages) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(10.dp)
                            .width(if (isSelected) 26.dp else 10.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) FamiCoral else paper.sheet)
                            .border(1.5.dp, paper.border, CircleShape)
                    )
                }
            }

            // Botones inferiores 3D: Anterior / Siguiente Hoja o Abrir Mi Cuaderno
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pagerState.currentPage > 0) {
                    Tactile3DButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        faceColor = paper.sheet,
                        lipColor = paper.lipPaper,
                        lipHeight = 5.dp,
                        cornerRadius = 14.dp,
                        contentPadding = PaddingValues(vertical = 13.dp, horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Anterior", tint = paper.ink, modifier = Modifier.size(18.dp))
                    }
                }

                Tactile3DButton(
                    onClick = {
                        if (pagerState.currentPage < totalPages - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            finishOnboarding()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    faceColor = FamiCoral,
                    lipColor = Lip3DCoral,
                    lipHeight = 5.dp,
                    cornerRadius = 14.dp,
                    contentPadding = PaddingValues(vertical = 13.dp, horizontal = 20.dp)
                ) {
                    val btnLabel = when {
                        pagerState.currentPage == totalPages - 1 -> strings.openNotebook
                        else -> strings.nextSheet
                    }
                    Text(
                        text = btnLabel,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (pagerState.currentPage == totalPages - 1) Icons.Default.Check else Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Diálogo Buscable de Todos los Países de Google Play
        if (showCountryDialog) {
            GooglePlayCountryPickerDialog(
                selectedCountryCode = selectedCountry.code,
                searchHint = strings.countrySearchHint,
                onDismiss = { showCountryDialog = false },
                onSelectCountry = { country ->
                    userManuallyPickedCountry = true
                    selectedCountry = country
                    selectedPrimaryCurrency = country.localCurrency
                    monthlyIncomeCurrency = country.localCurrency
                    draftWallets = draftWallets.mapIndexed { idx, w ->
                        if (idx == 0 && w.isLocal) w.copy(currency = country.localCurrency) else w
                    }
                    showCountryDialog = false
                }
            )
        }

        // Diálogo Selector de Idioma
        if (showLanguageDialog) {
            LanguagePickerDialog(
                selectedLanguageCode = selectedLanguage,
                onDismiss = { showLanguageDialog = false },
                onSelectLanguage = { lang ->
                    selectedLanguage = lang.code
                    showLanguageDialog = false
                }
            )
        }

        // Diálogo Selector de Moneda Principal
        if (showPrimaryCurrencyDialog) {
            CurrencyPickerDialog(
                title = "💵 ${strings.primaryCurrencyLabel}",
                selectedCurrencyCode = selectedPrimaryCurrency,
                allowNone = false,
                noneLabel = strings.hideSecondaryOption,
                onDismiss = { showPrimaryCurrencyDialog = false },
                onSelectCurrency = { code ->
                    selectedPrimaryCurrency = code
                    monthlyIncomeCurrency = code
                    draftWallets = draftWallets.mapIndexed { idx, w ->
                        if (idx == 0 && w.isLocal) w.copy(currency = code) else w
                    }
                    showPrimaryCurrencyDialog = false
                }
            )
        }

        // Diálogo Selector de Moneda Secundaria (Incluye opción Quitar/Ocultar)
        if (showSecondaryCurrencyDialog) {
            CurrencyPickerDialog(
                title = "⇄ ${strings.secondaryCurrencyLabel}",
                selectedCurrencyCode = selectedSecondaryCurrency,
                allowNone = true,
                noneLabel = strings.hideSecondaryOption,
                onDismiss = { showSecondaryCurrencyDialog = false },
                onSelectCurrency = { code ->
                    selectedSecondaryCurrency = code
                    showSecondaryCurrencyDialog = false
                }
            )
        }

        // Diálogo de Moneda para Ingreso Mensual
        if (showIncomeCurrencyDialog) {
            CurrencyPickerDialog(
                title = "💵 Moneda del Ingreso Mensual",
                selectedCurrencyCode = monthlyIncomeCurrency,
                allowNone = false,
                noneLabel = "",
                onDismiss = { showIncomeCurrencyDialog = false },
                onSelectCurrency = { code ->
                    monthlyIncomeCurrency = code
                    showIncomeCurrencyDialog = false
                }
            )
        }

        // Diálogo de Moneda para una Billetera específica
        editingWalletCurrencyIdx?.let { wIdx ->
            val currentCurr = draftWallets.getOrNull(wIdx)?.currency ?: selectedPrimaryCurrency
            CurrencyPickerDialog(
                title = "👛 Moneda de Billetera",
                selectedCurrencyCode = currentCurr,
                allowNone = false,
                noneLabel = "",
                onDismiss = { editingWalletCurrencyIdx = null },
                onSelectCurrency = { code ->
                    draftWallets = draftWallets.mapIndexed { i, w ->
                        if (i == wIdx) w.copy(currency = code, isLocal = code.equals(selectedPrimaryCurrency, ignoreCase = true)) else w
                    }
                    editingWalletCurrencyIdx = null
                }
            )
        }
    }
}


@Composable
fun NotebookDropdownField(
    label: String,
    value: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val paper = FamiTheme.paper
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Black,
            color = paper.ink
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(paper.sheetCream)
                .border(1.6.dp, paper.border, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = value,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = paper.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = paper.inkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = FamiCoral,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun GooglePlayCountryPickerDialog(
    selectedCountryCode: String,
    searchHint: String = "Buscar país o moneda...",
    onDismiss: () -> Unit,
    onSelectCountry: (CountryInfo) -> Unit
) {
    val paper = FamiTheme.paper
    var query by remember { mutableStateOf("") }
    val filteredCountries = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) CountryCurrencyConfig.SUPPORTED_COUNTRIES
        else CountryCurrencyConfig.SUPPORTED_COUNTRIES.filter {
            it.name.lowercase().contains(q) ||
                it.code.lowercase().contains(q) ||
                it.localCurrency.lowercase().contains(q)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        NotebookPaperSheet(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f),
            paperColor = paper.sheet,
            showSpiralTop = true,
            showRuledLines = false,
            showMarginLine = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌍 Países de Google Play (${CountryCurrencyConfig.SUPPORTED_COUNTRIES.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = paper.ink
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = paper.ink)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FamiCoral) },
                    placeholder = { Text(searchHint, fontSize = 12.sp, color = paper.inkMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FamiCoral,
                        unfocusedBorderColor = paper.border,
                        focusedContainerColor = paper.sheetCream,
                        unfocusedContainerColor = paper.sheetCream,
                        focusedTextColor = paper.ink,
                        unfocusedTextColor = paper.ink
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredCountries, key = { it.code }) { country ->
                        val isChosen = country.code.equals(selectedCountryCode, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isChosen) paper.stickyPeach else paper.sheetCream)
                                .border(
                                    width = if (isChosen) 2.dp else 1.2.dp,
                                    color = if (isChosen) FamiCoral else paper.border.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectCountry(country) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(country.flag, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${country.name} (${country.code})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = paper.ink
                                    )
                                    Text(
                                        text = "${country.localCurrency} (${country.symbol}) • ${country.rateProvider}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = paper.inkMuted
                                    )
                                }
                            }
                            if (isChosen) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = FamiCoral, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguagePickerDialog(
    selectedLanguageCode: String,
    onDismiss: () -> Unit,
    onSelectLanguage: (LanguageOption) -> Unit
) {
    val paper = FamiTheme.paper
    Dialog(onDismissRequest = onDismiss) {
        NotebookPaperSheet(
            modifier = Modifier.fillMaxWidth(),
            paperColor = paper.sheet,
            showSpiralTop = true,
            showRuledLines = false,
            showMarginLine = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🗣️ Selecciona el Idioma",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = paper.ink
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = paper.ink)
                    }
                }

                CountryCurrencyConfig.SUPPORTED_LANGUAGES.forEach { lang ->
                    val isChosen = lang.code.equals(selectedLanguageCode, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isChosen) paper.stickyPeach else paper.sheetCream)
                            .border(
                                width = if (isChosen) 2.dp else 1.2.dp,
                                color = if (isChosen) FamiCoral else paper.border.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectLanguage(lang) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(lang.flag, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(lang.nativeName, fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = paper.ink)
                                Text(lang.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = paper.inkMuted)
                            }
                        }
                        if (isChosen) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = FamiCoral, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencyPickerDialog(
    title: String,
    selectedCurrencyCode: String,
    allowNone: Boolean,
    noneLabel: String,
    onDismiss: () -> Unit,
    onSelectCurrency: (String) -> Unit
) {
    val paper = FamiTheme.paper
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) CountryCurrencyConfig.AVAILABLE_CURRENCIES
        else CountryCurrencyConfig.AVAILABLE_CURRENCIES.filter {
            it.code.lowercase().contains(q) || it.name.lowercase().contains(q)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        NotebookPaperSheet(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
            paperColor = paper.sheet,
            showSpiralTop = true,
            showRuledLines = false,
            showMarginLine = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = paper.ink)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = paper.ink)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FamiCoral) },
                    placeholder = { Text("Buscar moneda (USD, EUR, VES...)", fontSize = 12.sp, color = paper.inkMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FamiCoral,
                        unfocusedBorderColor = paper.border,
                        focusedContainerColor = paper.sheetCream,
                        unfocusedContainerColor = paper.sheetCream,
                        focusedTextColor = paper.ink,
                        unfocusedTextColor = paper.ink
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (allowNone) {
                        item(key = "NONE") {
                            val isChosen = selectedCurrencyCode == "NONE"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isChosen) paper.stickyYellow else paper.stickyPeach)
                                    .border(
                                        width = if (isChosen) 2.dp else 1.4.dp,
                                        color = if (isChosen) FamiCoral else paper.border,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectCurrency("NONE") }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🚫 $noneLabel",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isChosen) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = FamiCoral, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    items(filtered, key = { it.code }) { curr ->
                        val isChosen = curr.code.equals(selectedCurrencyCode, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isChosen) paper.stickyMint else paper.sheetCream)
                                .border(
                                    width = if (isChosen) 2.dp else 1.2.dp,
                                    color = if (isChosen) FamiCoral else paper.border.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectCurrency(curr.code) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${curr.code} (${curr.symbol})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = paper.ink
                                )
                                Text(
                                    text = curr.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = paper.inkMuted
                                )
                            }
                            if (isChosen) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = FamiCoral, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
