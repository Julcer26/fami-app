package com.example.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.api.BcvExchangeRateService
import com.example.currency.CountryCurrencyConfig
import com.example.currency.CountryInfo
import com.example.currency.CurrencyOption
import com.example.currency.LanguageOption
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

data class OnboardingStep(
    val badge: String,
    val title: String,
    val description: String,
    val imageRes: Int
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initialCountryCode: String = "VE",
    initialPrimaryCurrency: String = "",
    initialSecondaryCurrency: String = "USD",
    initialLanguage: String = CountryCurrencyConfig.detectSystemLanguageCode(),
    initialFamilyRole: String = "",
    onComplete: (
        selectedCountry: CountryInfo,
        primaryCurrency: String,
        secondaryCurrency: String,
        languageCode: String,
        familyRole: String
    ) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

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
    var selectedRole by remember { mutableStateOf(initialFamilyRole.ifBlank { "Mamá" }) }
    var customRoleInput by remember { mutableStateOf("") }

    var showCountryDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPrimaryCurrencyDialog by remember { mutableStateOf(false) }
    var showSecondaryCurrencyDialog by remember { mutableStateOf(false) }

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
                ipDetectionState = "detected"
            } else {
                ipDetectionState = "fallback"
            }
        } else {
            ipDetectionState = if (detectedCode != null) "detected" else "fallback"
        }
    }

    val strings = remember(selectedLanguage) { FamiStrings.get(selectedLanguage) }

    val guideSteps = remember(selectedLanguage) {
        when (selectedLanguage) {
            "en" -> listOf(
                OnboardingStep("STEP 1: MEET FAMI", "Hi! I'm Fami", "Your smart family financial advisor. Built to organize household income, bills, and savings like a clean notebook.", R.drawable.fami_onboarding_1),
                OnboardingStep("STEP 2: CURRENCIES", "Local + Secondary Currency", "Track everything in your local currency with live central bank rates, and optionally view USD, EUR, or hide secondary currency.", R.drawable.fami_onboarding_2),
                OnboardingStep("STEP 3: AI SCANNER", "Scan Receipts with AI", "Snap any supermarket ticket or utility bill. Fami AI extracts items, totals, and taxes in seconds.", R.drawable.fami_onboarding_3),
                OnboardingStep("FAMI HOUSEHOLD!", "Your Home in Sync", "Invite family members with one code. Share budgets, chores, and shopping lists in real time.", R.drawable.fami_onboarding_4)
            )
            "pt" -> listOf(
                OnboardingStep("PASSO 1: CONHEÇA A FAMI", "Olá! Sou a Fami", "Sua consultora financeira familiar. Criada para organizar receitas e gastos como um caderno claro e prático.", R.drawable.fami_onboarding_1),
                OnboardingStep("PASSO 2: MOEDAS", "Moeda Local + Secundária", "Controle tudo na moeda do seu país com cotação oficial e escolha mostrar USD, EUR ou ocultar a moeda secundária.", R.drawable.fami_onboarding_2),
                OnboardingStep("PASSO 3: ESCANEAMENTO IA", "Escaneie Notas com IA", "Tire foto de qualquer recibo ou fatura. A Fami IA organiza produtos, valores e impostos em segundos.", R.drawable.fami_onboarding_3),
                OnboardingStep("COMUNIDADE FAMI!", "Lar em Sincronia", "Convide sua família com um código simples. Compartilhe orçamentos, tarefas e compras em tempo real.", R.drawable.fami_onboarding_4)
            )
            else -> listOf(
                OnboardingStep("PASO 1: CONOCERNOS", "¡Hola! Soy Fami", "Tu asesor financiero inteligente. Creado para que tu familia tome el control total de sus ingresos y gastos con el orden de un cuaderno.", R.drawable.fami_onboarding_1),
                OnboardingStep("PASO 2: TU MONEDA", "Moneda Local + Secundaria", "Monitorea tus gastos en la moneda de tu país con tasa oficial en vivo y elige ver USD, otra divisa o solo tu moneda local.", R.drawable.fami_onboarding_2),
                OnboardingStep("PASO 3: TU CAMINO", "Escanea Facturas con IA", "Toma una foto a cualquier ticket de supermercado o factura de servicios. Fami IA desglosará productos y montos en segundos.", R.drawable.fami_onboarding_3),
                OnboardingStep("¡COMUNIDAD FAMI!", "El Hogar en Sincronía", "Invita a tu pareja o seres queridos con un código simple. Compartan presupuestos, tareas del hogar y listas de compras.", R.drawable.fami_onboarding_4)
            )
        }
    }

    // Página 0 = Panel Inicial de Configuración; Páginas 1..4 = Guía ilustrada de Fami
    val totalPages = 1 + guideSteps.size
    val pagerState = rememberPagerState(pageCount = { totalPages })

    val finalRole = customRoleInput.trim().ifBlank { selectedRole }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperDeskBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Superior con Logo Oficial de Fami (Carpeta Logos fami) + Botón Omitir
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_fami_logo_wide),
                        contentDescription = "Logo Oficial Fami",
                        modifier = Modifier.height(28.dp),
                        contentScale = ContentScale.FillHeight
                    )
                }

                if (pagerState.currentPage < totalPages - 1) {
                    Tactile3DButton(
                        onClick = {
                            onComplete(
                                selectedCountry,
                                selectedPrimaryCurrency,
                                selectedSecondaryCurrency,
                                selectedLanguage,
                                finalRole
                            )
                        },
                        faceColor = PaperSheetWhite,
                        lipColor = Lip3DPaper,
                        lipHeight = 3.dp,
                        cornerRadius = 10.dp,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(strings.skipBtn, color = FamiNavy, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                if (page == 0) {
                    // ==============================================================
                    // PÁGINA 0: PANEL INICIAL DE CONFIGURACIÓN (IP, País, Idioma, Monedas, Rol)
                    // ==============================================================
                    NotebookPaperSheet(
                        modifier = Modifier.fillMaxSize(),
                        paperColor = PaperSheetWhite,
                        showSpiralTop = true,
                        showRuledLines = true,
                        showMarginLine = true,
                        cornerRadius = 18.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(start = 28.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Saludo con Mascota + Título
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PaperSheetCream)
                                        .border(1.8.dp, FamiNavy, RoundedCornerShape(12.dp))
                                        .padding(2.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_fami_mascot),
                                        contentDescription = "Mascota Fami",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.setupTitle,
                                        color = FamiNavy,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = strings.setupSubtitle,
                                        color = FamiNavyMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            // Etiqueta de detección IP en vivo
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (ipDetectionState == "detected") PaperStickyMint else PaperStickyYellow)
                                    .border(1.4.dp, FamiNavy, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = when (ipDetectionState) {
                                        "loading" -> "🌐 ${strings.ipDetecting}"
                                        else -> "📍 ${strings.ipDetectedPrefix} ${selectedCountry.flag} ${selectedCountry.name} (${selectedCountry.localCurrency})"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FamiNavy
                                )
                            }

                            // 1. BARRA DESPLEGABLE DE PAÍS (Todos los países de Google Play)
                            NotebookDropdownField(
                                label = "🌍 ${strings.countryLabel}",
                                value = "${selectedCountry.flag} ${selectedCountry.name} (${selectedCountry.code})",
                                subtitle = "${selectedCountry.rateProvider} • ${CountryCurrencyConfig.SUPPORTED_COUNTRIES.size} países Google Play",
                                onClick = { showCountryDialog = true }
                            )

                            // 2. BARRA DESPLEGABLE DE IDIOMA (Automático del sistema o selector)
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

                            // 3. MONEDA LOCAL / PRINCIPAL + MONEDA SECUNDARIA (USD por defecto u ocultar)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    val primSym = CountryCurrencyConfig.getCurrencySymbol(selectedPrimaryCurrency)
                                    NotebookDropdownField(
                                        label = "💵 ${strings.primaryCurrencyLabel}",
                                        value = "$selectedPrimaryCurrency ($primSym)",
                                        subtitle = "Tocar para cambiar",
                                        onClick = { showPrimaryCurrencyDialog = true }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    val secValue = if (selectedSecondaryCurrency == "NONE") {
                                        "🚫 Oculta"
                                    } else {
                                        "$selectedSecondaryCurrency (${CountryCurrencyConfig.getCurrencySymbol(selectedSecondaryCurrency)})"
                                    }
                                    NotebookDropdownField(
                                        label = "⇄ ${strings.secondaryCurrencyLabel}",
                                        value = secValue,
                                        subtitle = if (selectedSecondaryCurrency == "NONE") "Solo moneda local" else "Cambiar o quitar",
                                        onClick = { showSecondaryCurrencyDialog = true }
                                    )
                                }
                            }

                            // 4. SELECTOR DE ROL FAMILIAR
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "🏡 ${strings.familyRoleLabel}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = FamiNavy
                                )
                                Spacer(modifier = Modifier.height(6.dp))
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
                                            faceColor = if (isChosen) FamiCoral else PaperSheetCream,
                                            lipColor = if (isChosen) Lip3DCoral else Lip3DPaper,
                                            lipHeight = 2.5.dp,
                                            cornerRadius = 10.dp,
                                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "${roleOpt.emoji} $label",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isChosen) Color.White else FamiNavy
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = customRoleInput,
                                    onValueChange = { customRoleInput = it },
                                    placeholder = {
                                        Text(strings.customRoleHint, fontSize = 11.sp, color = FamiNavyMuted)
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = FamiCoral,
                                        unfocusedBorderColor = FamiNavy.copy(alpha = 0.45f),
                                        focusedContainerColor = PaperSheetCream,
                                        unfocusedContainerColor = PaperSheetCream,
                                        focusedTextColor = FamiNavy,
                                        unfocusedTextColor = FamiNavy
                                    )
                                )
                            }
                        }
                    }
                } else {
                    // ==============================================================
                    // PÁGINAS 1..4: GUÍA ILUSTRADA DE LA MASCOTA FAMI
                    // ==============================================================
                    val step = guideSteps[page - 1]
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NotebookPaperSheet(
                            modifier = Modifier.fillMaxWidth(),
                            paperColor = PaperSheetWhite,
                            showSpiralTop = true,
                            showRuledLines = true,
                            showMarginLine = true,
                            cornerRadius = 18.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 28.dp, end = 18.dp, top = 12.dp, bottom = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PaperStickyPeach)
                                        .border(1.5.dp, FamiNavy, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "📓 ${step.badge}",
                                        color = FamiCoralDark,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(225.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(PaperSheetCream)
                                        .border(1.8.dp, FamiNavy, RoundedCornerShape(16.dp))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = step.imageRes),
                                        contentDescription = step.title,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = step.title,
                                    color = FamiNavy,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = step.description,
                                    color = FamiNavyMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 19.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Indicador de Páginas (Dots con borde de tinta)
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
                            .background(if (isSelected) FamiCoral else PaperSheetWhite)
                            .border(1.5.dp, FamiNavy, CircleShape)
                    )
                }
            }

            // Botón Táctil 3D Principal
            Tactile3DButton(
                onClick = {
                    if (pagerState.currentPage < totalPages - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onComplete(
                            selectedCountry,
                            selectedPrimaryCurrency,
                            selectedSecondaryCurrency,
                            selectedLanguage,
                            finalRole
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                faceColor = FamiCoral,
                lipColor = Lip3DCoral,
                lipHeight = 5.dp,
                cornerRadius = 14.dp,
                contentPadding = PaddingValues(vertical = 13.dp, horizontal = 20.dp)
            ) {
                val btnLabel = when {
                    pagerState.currentPage == 0 -> strings.continueToGuide
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
    }
}

@Composable
fun NotebookDropdownField(
    label: String,
    value: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Black,
            color = FamiNavy
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PaperSheetCream)
                .border(1.6.dp, FamiNavy, RoundedCornerShape(12.dp))
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
                        color = FamiNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FamiNavyMuted,
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
            paperColor = PaperSheetWhite,
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
                        color = FamiNavy
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = FamiNavy)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FamiCoral) },
                    placeholder = { Text(searchHint, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FamiCoral,
                        unfocusedBorderColor = FamiNavy,
                        focusedContainerColor = PaperSheetCream,
                        unfocusedContainerColor = PaperSheetCream,
                        focusedTextColor = FamiNavy,
                        unfocusedTextColor = FamiNavy
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
                                .background(if (isChosen) PaperStickyPeach else PaperSheetCream)
                                .border(
                                    width = if (isChosen) 2.dp else 1.2.dp,
                                    color = if (isChosen) FamiCoral else FamiNavy.copy(alpha = 0.5f),
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
                                        color = FamiNavy
                                    )
                                    Text(
                                        text = "${country.localCurrency} (${country.symbol}) • ${country.rateProvider}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = FamiNavyMuted
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
    Dialog(onDismissRequest = onDismiss) {
        NotebookPaperSheet(
            modifier = Modifier.fillMaxWidth(),
            paperColor = PaperSheetWhite,
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
                        color = FamiNavy
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = FamiNavy)
                    }
                }

                CountryCurrencyConfig.SUPPORTED_LANGUAGES.forEach { lang ->
                    val isChosen = lang.code.equals(selectedLanguageCode, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isChosen) PaperStickyPeach else PaperSheetCream)
                            .border(
                                width = if (isChosen) 2.dp else 1.2.dp,
                                color = if (isChosen) FamiCoral else FamiNavy.copy(alpha = 0.5f),
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
                                Text(lang.nativeName, fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = FamiNavy)
                                Text(lang.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FamiNavyMuted)
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
            paperColor = PaperSheetWhite,
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
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = FamiNavy)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = FamiNavy)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FamiCoral) },
                    placeholder = { Text("Buscar moneda (USD, EUR, VES...)", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FamiCoral,
                        unfocusedBorderColor = FamiNavy,
                        focusedContainerColor = PaperSheetCream,
                        unfocusedContainerColor = PaperSheetCream,
                        focusedTextColor = FamiNavy,
                        unfocusedTextColor = FamiNavy
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
                                    .background(if (isChosen) PaperStickyYellow else PaperStickyPeach)
                                    .border(
                                        width = if (isChosen) 2.dp else 1.4.dp,
                                        color = if (isChosen) FamiCoral else FamiNavy,
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
                                    color = FamiNavy,
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
                                .background(if (isChosen) PaperStickyMint else PaperSheetCream)
                                .border(
                                    width = if (isChosen) 2.dp else 1.2.dp,
                                    color = if (isChosen) FamiCoral else FamiNavy.copy(alpha = 0.5f),
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
                                    color = FamiNavy
                                )
                                Text(
                                    text = curr.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FamiNavyMuted
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
