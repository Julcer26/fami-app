package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.analytics.FamiUsageTracker
import com.example.api.GeminiParser
import com.example.database.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FamilyViewModel(
    private val repository: FamilyRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _currentUser = MutableStateFlow("")
    val currentUser: StateFlow<String> = _currentUser.asStateFlow()

    private val _familyId = MutableStateFlow<String?>(null)
    val familyId: StateFlow<String?> = _familyId.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isSessionInitializing = MutableStateFlow(true)
    val isSessionInitializing: StateFlow<Boolean> = _isSessionInitializing.asStateFlow()

    private val _selectedMonth = MutableStateFlow(java.util.Calendar.getInstance())
    val selectedMonth: StateFlow<java.util.Calendar> = _selectedMonth.asStateFlow()

    fun updateSelectedMonth(calendar: java.util.Calendar) {
        _selectedMonth.value = calendar
    }


    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryDetail>> = repository.allCategories
        .map { list -> list.sortedBy { it.categoryName.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chores: StateFlow<List<Chore>> = repository.allChores
        .map { list -> list.sortedBy { it.title.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shoppingItems: StateFlow<List<ShoppingItem>> = repository.allShoppingItems
        .map { list -> list.sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profiles: StateFlow<List<UserProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<List<AppSetting>> = repository.allSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isDarkTheme: StateFlow<Boolean> = dataStoreManager.isDarkThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hasCompletedOnboarding: StateFlow<Boolean> = dataStoreManager.hasCompletedOnboardingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val countryCode: StateFlow<String> = dataStoreManager.countryCodeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "VE")

    val primaryCurrency: StateFlow<String> = dataStoreManager.primaryCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "VES")

    val secondaryCurrency: StateFlow<String> = dataStoreManager.secondaryCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val liveExchangeRate: StateFlow<Double> = dataStoreManager.liveExchangeRateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 904.45)

    val secondaryExchangeRate: StateFlow<Double> = dataStoreManager.secondaryExchangeRateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0)

    val appLanguage: StateFlow<String> = dataStoreManager.appLanguageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.currency.CountryCurrencyConfig.detectSystemLanguageCode())

    val familyRole: StateFlow<String> = dataStoreManager.familyRoleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Administrador")

    val managementMode: StateFlow<String> = dataStoreManager.managementModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "familiar")

    val familyAction: StateFlow<String> = dataStoreManager.familyActionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "create")

    val pendingFamilyName: StateFlow<String> = dataStoreManager.pendingFamilyNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val userAvatarId: StateFlow<String> = dataStoreManager.userAvatarIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "woman_1")

    val monthlyIncomeAmount: StateFlow<Double> = dataStoreManager.monthlyIncomeAmountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyIncomeCurrency: StateFlow<String> = dataStoreManager.monthlyIncomeCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val hasCompletedInAppTour: StateFlow<Boolean> = dataStoreManager.hasCompletedInAppTourFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val wallets: StateFlow<List<Wallet>> = repository.allWallets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _walletRatesToUsd = MutableStateFlow<Map<String, Double>>(emptyMap())
    val walletRatesToUsd: StateFlow<Map<String, Double>> = _walletRatesToUsd.asStateFlow()

    val viewInSecondary: StateFlow<Boolean> = dataStoreManager.viewInSecondaryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val unlockMethod: StateFlow<String> = dataStoreManager.unlockMethodFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "unset")

    val savedEmail: StateFlow<String> = dataStoreManager.savedEmailFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val authProvider: StateFlow<String> = dataStoreManager.authProviderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "email")

    val pendingUserAlias: StateFlow<String> = dataStoreManager.pendingUserAliasFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _pendingEmailVerification = MutableStateFlow(false)
    val pendingEmailVerification: StateFlow<Boolean> = _pendingEmailVerification.asStateFlow()

    private val _pendingVerificationEmail = MutableStateFlow("")
    val pendingVerificationEmail: StateFlow<String> = _pendingVerificationEmail.asStateFlow()

    private val _needsUnlockMethodChoice = MutableStateFlow(false)
    val needsUnlockMethodChoice: StateFlow<Boolean> = _needsUnlockMethodChoice.asStateFlow()

    private val _startInExistingLogin = MutableStateFlow(false)
    val startInExistingLogin: StateFlow<Boolean> = _startInExistingLogin.asStateFlow()

    fun openExistingLoginFromOnboarding() {
        _authError.value = null
        _startInExistingLogin.value = true
        viewModelScope.launch {
            dataStoreManager.setOnboardingCompleted(true)
        }
    }

    fun consumeStartInExistingLogin() {
        _startInExistingLogin.value = false
    }

    suspend fun hasConfiguredPin(): Boolean = dataStoreManager.hasConfiguredUserPin()

    fun isValidAlias(alias: String): Boolean {
        val clean = alias.trim()
        return clean.length >= 2 && !clean.contains("@") && !clean.contains("/")
    }

    fun isValidPin(pin: String): Boolean {
        val clean = pin.trim()
        return clean.length == 5 && clean.all { it.isDigit() } && clean != "00000"
    }

    fun normalizeFamilyCode(raw: String): String {
        return raw.trim().uppercase().filter { it.isLetterOrDigit() || it == '-' }
    }

    val isAdminRole: StateFlow<Boolean> = combine(managementMode, familyRole) { mode, role ->
        mode.equals("individual", ignoreCase = true) || isPrivilegedRole(role)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun isPrivilegedRole(role: String): Boolean {
        val clean = role.trim().lowercase()
        return clean in setOf(
            "admin", "administrador", "administrator",
            "mamá", "mama", "mom", "mãe", "mae",
            "papá", "papa", "dad", "pai"
        )
    }

    fun canEditBudgets(): Boolean =
        managementMode.value.equals("individual", ignoreCase = true) || isPrivilegedRole(familyRole.value)

    fun canManageMembers(): Boolean =
        managementMode.value.equals("individual", ignoreCase = true) || isPrivilegedRole(familyRole.value)

    fun canDeleteTransaction(tx: Transaction): Boolean {
        return managementMode.value.equals("individual", ignoreCase = true) ||
            isPrivilegedRole(familyRole.value) ||
            tx.registeredBy.equals(_currentUser.value, ignoreCase = true)
    }

    fun updateManagementMode(newMode: String) {
        viewModelScope.launch {
            val clean = if (newMode.trim().equals("individual", ignoreCase = true)) "individual" else "familiar"
            dataStoreManager.setManagementMode(clean)
            if (clean == "individual") {
                dataStoreManager.setFamilyRole("Administrador")
            }
            val fid = _familyId.value ?: dataStoreManager.familyIdFlow.firstOrNull()
            if (!fid.isNullOrBlank()) {
                val user = _currentUser.value.ifBlank { "Usuario Fami" }
                runCatching { repository.insertSetting("management_mode", clean, user) }
            }
            val label = if (clean == "individual") "👤 Modo Individual" else "👨‍👩‍👧‍👦 Modo Familiar"
            notifyUser("Modo cambiado a $label ✨")
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    private val _detectedIpCountry = MutableStateFlow<String?>(null)
    val detectedIpCountry: StateFlow<String?> = _detectedIpCountry.asStateFlow()

    fun detectCountryByIpOnStartup() {
        viewModelScope.launch {
            if (_detectedIpCountry.value != null) return@launch
            val detected = com.example.api.BcvExchangeRateService.detectCountryCodeByIp()
                ?: java.util.Locale.getDefault().country.takeIf { it.length == 2 }?.uppercase()
            _detectedIpCountry.value = detected
        }
    }

    fun toggleViewInSecondary() {
        viewModelScope.launch {
            if (secondaryCurrency.value == "NONE") {
                dataStoreManager.toggleViewInSecondary(false)
            } else {
                dataStoreManager.toggleViewInSecondary(!viewInSecondary.value)
            }
        }
    }

    fun applyInitialConfiguration(
        country: com.example.currency.CountryInfo,
        primaryCurr: String,
        secondaryCurr: String,
        languageCode: String,
        role: String,
        managementMode: String = "familiar",
        familyAction: String = "create",
        familyName: String = "",
        avatarId: String = "woman_1",
        monthlyIncome: Double = 0.0,
        monthlyIncomeCurr: String = primaryCurr,
        initialWallets: List<Wallet> = emptyList(),
        userAlias: String = ""
    ) {
        viewModelScope.launch {
            dataStoreManager.setInitialConfiguration(
                countryCode = country.code,
                primaryCurr = primaryCurr,
                secondaryCurr = secondaryCurr,
                languageCode = languageCode,
                familyRole = role.trim(),
                managementMode = managementMode,
                familyAction = familyAction,
                familyName = familyName.trim(),
                avatarId = avatarId,
                monthlyIncomeAmount = monthlyIncome,
                monthlyIncomeCurrency = monthlyIncomeCurr,
                initialWallets = initialWallets,
                userAlias = userAlias.trim()
            )
            val cleanAlias = userAlias.trim().takeIf { it.isNotBlank() && !it.contains("@") }
            if (cleanAlias != null) {
                applyAliasUpdateInternal(cleanAlias)
            }
            val fid = _familyId.value ?: dataStoreManager.familyIdFlow.firstOrNull()
            val activeUser = _currentUser.value.ifBlank { cleanAlias ?: "Usuario Fami" }
            if (!fid.isNullOrBlank()) {
                repository.setFamilyId(fid)
                runCatching { repository.populateDefaultsIfNeeded() }
                runCatching {
                    syncPendingNotebookSetup(
                        userName = activeUser,
                        colorHex = "#D0BCFF",
                        familyNameOverride = familyName.trim()
                    )
                }
            }
            syncExchangeRateFor(country.code, primaryCurr, secondaryCurr)
        }
    }

    fun updateAppLanguage(languageCode: String) {
        viewModelScope.launch {
            val clean = languageCode.trim().lowercase().ifBlank {
                com.example.currency.CountryCurrencyConfig.detectSystemLanguageCode()
            }
            dataStoreManager.setAppLanguage(clean)
            val langOpt = com.example.currency.CountryCurrencyConfig.getLanguageOption(clean)
            notifyUser("Idioma cambiado a ${langOpt.flag} ${langOpt.nativeName}")
        }
    }

    fun updateRegionalSettings(
        country: com.example.currency.CountryInfo,
        primaryCurr: String,
        secondaryCurr: String,
        languageCode: String,
        role: String
    ) {
        viewModelScope.launch {
            dataStoreManager.setCountryAndCurrency(country.code, primaryCurr, secondaryCurr)
            dataStoreManager.setAppLanguage(languageCode)
            dataStoreManager.setFamilyRole(role.trim())
            syncExchangeRateFor(country.code, primaryCurr, secondaryCurr)
            notifyUser("Configuración actualizada: ${country.flag} $primaryCurr")
        }
    }

    fun updateMonthlyIncome(amount: Double, currency: String) {
        viewModelScope.launch {
            val cleanCurr = com.example.currency.CountryCurrencyConfig.normalizeCurrencyCode(currency)
            dataStoreManager.setMonthlyIncome(amount, cleanCurr)
            val user = _currentUser.value.ifBlank { "Usuario" }
            val rate = when {
                cleanCurr == "USD" -> 1.0
                cleanCurr.equals(primaryCurrency.value, ignoreCase = true) && liveExchangeRate.value > 0.0 -> liveExchangeRate.value
                cleanCurr.equals(secondaryCurrency.value, ignoreCase = true) && secondaryExchangeRate.value > 0.0 -> secondaryExchangeRate.value
                else -> com.example.api.BcvExchangeRateService.fetchUsdToCurrencyRate(cleanCurr)
            }
            repository.insertSetting("monthly_income_amount", String.format(java.util.Locale.US, "%.2f", amount), user)
            repository.insertSetting("monthly_income_currency", cleanCurr, user)
            if (rate > 0.0) {
                val amountUsd = if (cleanCurr == "USD") amount else amount / rate
                repository.insertSetting("total_limit_usd", String.format(java.util.Locale.US, "%.2f", amountUsd), user)
                notifyUser("Ingreso mensual actualizado: ${com.example.currency.CountryCurrencyConfig.getCurrencySymbol(cleanCurr)} ${String.format(java.util.Locale.US, "%,.2f", amount)} $cleanCurr")
            } else {
                notifyUser("Ingreso guardado en $cleanCurr, pero no se pudo obtener la tasa para actualizar el límite USD.")
            }
        }
    }

    fun computeTotalWalletsInCurrency(
        targetCurrency: String,
        walletList: List<Wallet>,
        primaryCurr: String,
        primaryRateToUsd: Double,
        secondaryCurr: String,
        secondaryRateToUsd: Double,
        extraRatesToUsd: Map<String, Double>
    ): Double {
        if (walletList.isEmpty()) return 0.0
        val target = com.example.currency.CountryCurrencyConfig.normalizeCurrencyCode(targetCurrency)
        val pCurr = com.example.currency.CountryCurrencyConfig.normalizeCurrencyCode(primaryCurr)
        val sCurr = com.example.currency.CountryCurrencyConfig.normalizeCurrencyCode(secondaryCurr)

        fun ratePerUsd(code: String): Double {
            val c = com.example.currency.CountryCurrencyConfig.normalizeCurrencyCode(code)
            return when {
                c == "USD" -> 1.0
                c == pCurr && primaryRateToUsd > 0.0 -> primaryRateToUsd
                c == sCurr && secondaryRateToUsd > 0.0 -> secondaryRateToUsd
                (extraRatesToUsd[c] ?: 0.0) > 0.0 -> extraRatesToUsd[c]!!
                else -> com.example.api.BcvExchangeRateService.getCachedRate(c) ?: 0.0
            }
        }

        val targetPerUsd = ratePerUsd(target)
        return walletList.sumOf { wallet ->
            val wCurr = com.example.currency.CountryCurrencyConfig.normalizeCurrencyCode(wallet.currency)
            com.example.currency.CountryCurrencyConfig.convertCrossOrNull(
                amount = wallet.balance,
                fromCurrency = wCurr,
                toCurrency = target,
                fromRatePerUsd = ratePerUsd(wCurr),
                toRatePerUsd = targetPerUsd
            ) ?: 0.0
        }
    }

    fun updateUserAvatar(avatarId: String) {
        viewModelScope.launch {
            dataStoreManager.setUserAvatarId(avatarId)
            val user = _currentUser.value
            if (user.isNotBlank()) {
                val existing = profiles.value.find { it.name == user }
                if (existing != null) {
                    repository.insertProfile(existing.copy(avatarId = avatarId), user)
                }
            }
            notifyUser("Avatar actualizado ✨")
        }
    }

    fun completeInAppTour() {
        viewModelScope.launch {
            dataStoreManager.setInAppTourCompleted(true)
        }
    }

    fun reopenInAppTour() {
        viewModelScope.launch {
            dataStoreManager.setInAppTourCompleted(false)
        }
    }

    fun addOrEditWallet(wallet: Wallet) {
        viewModelScope.launch {
            repository.insertWallet(wallet.copy(ownerName = wallet.ownerName.ifBlank { _currentUser.value }), _currentUser.value)
        }
    }

    fun deleteWallet(wallet: Wallet) {
        viewModelScope.launch {
            repository.deleteWallet(wallet, _currentUser.value)
        }
    }

    fun adjustWalletBalance(walletId: String, delta: Double) {
        viewModelScope.launch {
            try {
                repository.adjustWalletBalance(walletId, delta, _currentUser.value)
                notifyUser("Saldo de billetera actualizado")
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (e: Exception) {
                notifyUser("Error al ajustar saldo: ${e.localizedMessage ?: "Intenta de nuevo"}")
            }
        }
    }

    fun transferBetweenWallets(
        fromWallet: Wallet,
        toWallet: Wallet,
        fromAmount: Double,
        toAmount: Double,
        note: String = ""
    ) {
        viewModelScope.launch {
            try {
                repository.transferBetweenWallets(fromWallet, toWallet, fromAmount, toAmount, note, _currentUser.value)
                FamiUsageTracker.track(dataStoreManager.appContext, FamiUsageTracker.FEATURE_WALLET_TRANSFER)
                notifyUser("Transferencia completada: ${fromWallet.name} → ${toWallet.name}")
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (e: Exception) {
                notifyUser("Error en transferencia: ${e.localizedMessage}")
            }
        }
    }

    fun reopenInitialConfig() {
        _authError.value = null
        _startInExistingLogin.value = false
        viewModelScope.launch {
            dataStoreManager.setOnboardingCompleted(false)
        }
    }

    fun setCountry(country: com.example.currency.CountryInfo) {
        viewModelScope.launch {
            val sec = secondaryCurrency.value.ifBlank { "USD" }
            dataStoreManager.setCountryAndCurrency(country.code, country.localCurrency, sec)
            dataStoreManager.setOnboardingCompleted(true)
            syncExchangeRateFor(country.code, country.localCurrency, sec)
        }
    }

    fun syncExchangeRate() {
        viewModelScope.launch {
            syncExchangeRateFor(countryCode.value, primaryCurrency.value, secondaryCurrency.value)
        }
    }

    private suspend fun syncExchangeRateFor(code: String, primaryCurr: String, secondaryCurr: String) {
        try {
            val primaryRate = com.example.api.BcvExchangeRateService.fetchOfficialRateForCountry(code, primaryCurr)
            if (primaryRate > 0.0) {
                dataStoreManager.setLiveExchangeRate(primaryRate)
                repository.updateRateForCountry(code, primaryCurr, "Sistema Fami")
            }
            val secRate = if (secondaryCurr == "NONE" || secondaryCurr.isBlank()) {
                1.0
            } else {
                com.example.api.BcvExchangeRateService.fetchUsdToCurrencyRate(secondaryCurr)
            }
            if (secRate > 0.0) {
                dataStoreManager.setSecondaryExchangeRate(secRate)
            }
        } catch (e: Exception) { }
    }

    fun toggleDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setDarkTheme(isDark)
            if (isDark) {
                FamiUsageTracker.trackOnceDaily(dataStoreManager.appContext, FamiUsageTracker.FEATURE_DARK_MODE_ON)
            }
        }
    }

    val activityLogs: StateFlow<List<ActivityLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<Goal>> = combine(repository.allGoals, _currentUser) { list, user ->
        list.filter { !it.isPrivate || it.createdBy == user }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<Note>> = combine(repository.allNotes, _currentUser) { list, user ->
        list.filter { !it.isPrivate || it.createdBy == user }
            .sortedWith(
                compareByDescending<Note> { it.isPinned && !it.isCompleted }
                    .thenBy { it.isCompleted }
                    .thenByDescending { maxOf(it.updatedDate, it.createdDate) }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringPayments: StateFlow<List<com.example.database.RecurringPayment>> = repository.allRecurringPayments
        .map { list -> list.sortedBy { it.dueDay } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createOrEditRecurringPayment(payment: com.example.database.RecurringPayment) {
        viewModelScope.launch {
            try {
                repository.insertRecurringPayment(payment, _currentUser.value)
                notifyUser("Servicio guardado: '${payment.name}'")
            } catch (e: Exception) {
                notifyUser("Error al guardar servicio: ${e.localizedMessage}")
            }
        }
    }

    fun deleteRecurringPayment(payment: com.example.database.RecurringPayment) {
        viewModelScope.launch {
            try {
                repository.deleteRecurringPayment(payment, _currentUser.value)
                notifyUser("Servicio eliminado: '${payment.name}'")
            } catch (e: Exception) {
                notifyUser("Error al eliminar servicio: ${e.localizedMessage}")
            }
        }
    }

    fun markRecurringPaymentPaid(
        payment: com.example.database.RecurringPayment,
        monthKey: String,
        amount: Double,
        currency: String,
        date: Long,
        walletId: String = ""
    ) {
        viewModelScope.launch {
            try {
                val alert = repository.markRecurringPaymentPaid(payment, monthKey, amount, currency, date, _currentUser.value, walletId)
                FamiUsageTracker.track(dataStoreManager.appContext, FamiUsageTracker.FEATURE_RECURRING_PAID)
                if (alert != null) notifyUser(alert)
                else notifyUser("Pago registrado: '${payment.name}' para $monthKey")
            } catch (e: Exception) {
                notifyUser("Error al registrar pago: ${e.localizedMessage}")
            }
        }
    }

    fun markRecurringPaymentUnpaid(payment: com.example.database.RecurringPayment, monthKey: String) {
        viewModelScope.launch {
            try {
                repository.markRecurringPaymentUnpaid(payment, monthKey, _currentUser.value)
                notifyUser("Pago desmarcado: '${payment.name}' para $monthKey")
            } catch (e: Exception) {
                notifyUser("Error al desmarcar pago: ${e.localizedMessage}")
            }
        }
    }

    private val _isRegisteringNlp = MutableStateFlow(false)
    val isRegisteringNlp: StateFlow<Boolean> = _isRegisteringNlp.asStateFlow()

    private val _nlpError = MutableStateFlow<String?>(null)
    val nlpError: StateFlow<String?> = _nlpError.asStateFlow()

    private val _topNotification = MutableStateFlow<String?>(null)
    val topNotification: StateFlow<String?> = _topNotification.asStateFlow()

    private val _confettiTrigger = MutableStateFlow(0)
    val confettiTrigger: StateFlow<Int> = _confettiTrigger.asStateFlow()
    fun triggerConfetti() { _confettiTrigger.update { it + 1 } }

    private var failedPinAttempts = 0
    private var pinLockoutUntilMs = 0L

    init {
        viewModelScope.launch {
            try {
                // Esperar la primera lectura real de DataStore para evitar parpadeo del Onboarding
                dataStoreManager.hasCompletedOnboardingFlow.firstOrNull()
                val firebaseUser = runCatching {
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                }.getOrNull()
                if (firebaseUser == null) {
                    dataStoreManager.clearAuthData()
                    repository.setFamilyId(null)
                    _familyId.value = null
                    _currentUser.value = ""
                } else {
                    val fid = dataStoreManager.familyIdFlow.firstOrNull()
                    val name = dataStoreManager.userNameFlow.firstOrNull()
                    if (!fid.isNullOrBlank() && !name.isNullOrBlank()) {
                        _familyId.value = fid
                        _currentUser.value = name
                        // Security: keep repository detached until PIN or biometric unlock succeeds
                        repository.setFamilyId(null)
                        val isEmailProvider = firebaseUser.providerData.any { it.providerId == "password" }
                        if (isEmailProvider && !firebaseUser.isEmailVerified) {
                            _pendingVerificationEmail.value = firebaseUser.email ?: ""
                            _pendingEmailVerification.value = true
                        }
                    } else {
                        runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().signOut() }
                        dataStoreManager.clearAuthData()
                        repository.setFamilyId(null)
                    }
                }
            } finally {
                _isSessionInitializing.value = false
            }
        }

        // Observador de monedas de billeteras adicionales para conversión consolidada
        viewModelScope.launch {
            wallets.collect { list ->
                val distinctCurrencies = list.map { com.example.currency.CountryCurrencyConfig.normalizeCurrencyCode(it.currency) }
                    .filter { it.isNotBlank() && it != "USD" }
                    .distinct()
                for (curr in distinctCurrencies) {
                    if ((_walletRatesToUsd.value[curr] ?: 0.0) <= 0.0) {
                        val cached = com.example.api.BcvExchangeRateService.getCachedRate(curr)
                        val resolved = if (cached != null && cached > 0.0) {
                            cached
                        } else {
                            runCatching { com.example.api.BcvExchangeRateService.fetchUsdToCurrencyRate(curr) }.getOrDefault(0.0)
                        }
                        if (resolved > 0.0) {
                            _walletRatesToUsd.update { it + (curr to resolved) }
                        }
                    }
                }
            }
        }

        // Observador en tiempo real de nuevas tareas asignadas al usuario actual o a "Todos"
        viewModelScope.launch {
            combine(isAuthenticated, familyId, currentUser) { auth, fid, user ->
                Triple(auth, fid, user)
            }.flatMapLatest { (auth, fid, user) ->
                if (auth && !fid.isNullOrBlank() && user.isNotBlank()) {
                    repository.allChores.map { list -> Triple(fid, user, list) }
                } else {
                    emptyFlow()
                }
            }.collect { (fid, user, choreList) ->
                val ctx = dataStoreManager.appContext
                if (!com.example.notifications.TaskNotificationHelper.isFamilySeeded(ctx, fid)) {
                    com.example.notifications.TaskNotificationHelper.markFamilySeeded(
                        ctx,
                        fid,
                        choreList.map { it.id }
                    )
                } else {
                    choreList.forEach { chore ->
                        if (com.example.notifications.TaskNotificationHelper.isChoreTargetedToUser(chore, user) &&
                            !com.example.notifications.TaskNotificationHelper.hasNotifiedChore(ctx, chore.id)
                        ) {
                            com.example.notifications.TaskNotificationHelper.showTaskNotification(
                                context = ctx,
                                choreId = chore.id,
                                title = chore.title,
                                assignedTo = chore.assignedTo,
                                createdBy = chore.createdBy,
                                priority = chore.priority
                            )
                            val targetTag = if (com.example.notifications.TaskNotificationHelper.isAssignedToAll(chore.assignedTo)) {
                                "para Todos"
                            } else {
                                "para ti"
                            }
                            notifyUser("📌 Nueva tarea $targetTag: ${chore.title}")
                        }
                    }
                }
            }
        }
    }

    fun isValidEmailFormat(email: String): Boolean {
        val trimmed = email.trim()
        if (trimmed.isBlank()) return false
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) return false
        val domain = trimmed.substringAfter("@", "").lowercase()
        val invalidDomains = setOf("test.com", "example.com", "mailinator.com", "tempmail.com", "fake.com", "yopmail.com")
        return domain.contains(".") && !invalidDomains.contains(domain)
    }

    // --- Authentication ---
    fun loginWithPin(inputPin: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (now < pinLockoutUntilMs) {
                val secsLeft = ((pinLockoutUntilMs - now) / 1000L).coerceAtLeast(1L)
                val lockMsg = "Demasiados intentos fallidos. Espera ${secsLeft}s."
                _authError.value = lockMsg
                notifyUser(lockMsg)
                onResult(false)
                return@launch
            }

            if (!dataStoreManager.hasConfiguredUserPin()) {
                _authError.value = "No hay PIN configurado. Usa biometría o inicia sesión nuevamente."
                notifyUser("Configura un PIN válido o usa tu huella")
                onResult(false)
                return@launch
            }

            val pinMatches = dataStoreManager.verifyUserPin(inputPin)
            if (pinMatches) {
                failedPinAttempts = 0
                pinLockoutUntilMs = 0L
                if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
                    val fid = _familyId.value ?: dataStoreManager.familyIdFlow.firstOrNull()
                    if (fid.isNullOrBlank()) {
                        _authError.value = "No se encontró tu hogar activo. Inicia sesión de nuevo."
                        onResult(false)
                        return@launch
                    }
                    _familyId.value = fid
                    repository.setFamilyId(fid)
                    _authError.value = null
                    _isAuthenticated.value = true
                    onResult(true)
                    checkDefaultsAndSync()
                } else {
                    _authError.value = "Sesión de Firebase expirada. Inicia sesión nuevamente."
                    notifyUser("Sesión expirada. Inicia sesión de nuevo.")
                    dataStoreManager.clearAuthData()
                    repository.setFamilyId(null)
                    _familyId.value = null
                    onResult(false)
                }
            } else {
                failedPinAttempts += 1
                if (failedPinAttempts >= 5) {
                    pinLockoutUntilMs = System.currentTimeMillis() + 30_000L
                    failedPinAttempts = 0
                    val lockMsg = "Demasiados intentos fallidos. Bloqueado por 30 segundos."
                    _authError.value = lockMsg
                    notifyUser(lockMsg)
                } else {
                    _authError.value = "PIN incorrecto. Verifica los 5 dígitos."
                    notifyUser("PIN incorrecto")
                }
                onResult(false)
            }
        }
    }

    fun loginBiometric() {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
            viewModelScope.launch {
                val fid = _familyId.value ?: dataStoreManager.familyIdFlow.firstOrNull()
                if (fid.isNullOrBlank()) {
                    _authError.value = "No se encontró tu hogar activo. Inicia sesión de nuevo."
                    return@launch
                }
                failedPinAttempts = 0
                pinLockoutUntilMs = 0L
                _familyId.value = fid
                repository.setFamilyId(fid)
                _authError.value = null
                _isAuthenticated.value = true
                checkDefaultsAndSync()
            }
        } else {
            _authError.value = "Sesión expirada. Inicia sesión de nuevo."
            notifyUser("Sesión expirada. Inicie sesión de nuevo.")
            viewModelScope.launch {
                dataStoreManager.clearAuthData()
                repository.setFamilyId(null)
                _familyId.value = null
            }
        }
    }

    suspend fun unlockForWidgetIfSignedIn(): Boolean {
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return false
        val isEmailProvider = firebaseUser.providerData.any { it.providerId == "password" }
        if (isEmailProvider && !firebaseUser.isEmailVerified) {
            return false
        }
        val fid = _familyId.value ?: dataStoreManager.familyIdFlow.firstOrNull()
        val name = _currentUser.value.ifBlank { dataStoreManager.userNameFlow.firstOrNull().orEmpty() }
        if (fid.isNullOrBlank() || name.isBlank()) {
            return false
        }
        failedPinAttempts = 0
        pinLockoutUntilMs = 0L
        _familyId.value = fid
        _currentUser.value = name
        repository.setFamilyId(fid)
        _authError.value = null
        _isAuthenticated.value = true
        return true
    }

    fun setUnlockMethodAndEnter(method: String, customPin: String? = null, customAlias: String? = null) {
        viewModelScope.launch {
            val cleanPin = customPin?.trim().orEmpty()
            if (cleanPin.isNotEmpty()) {
                if (cleanPin.length != 5 || !cleanPin.all { it.isDigit() } || cleanPin == "00000") {
                    _authError.value = "El PIN debe tener 5 dígitos distintos de 00000."
                    notifyUser("El PIN debe tener 5 dígitos y no puede ser 00000")
                    return@launch
                }
                dataStoreManager.updatePin(cleanPin)
            } else if (method == "pin" && !dataStoreManager.hasConfiguredUserPin()) {
                _authError.value = "Debes definir un PIN secreto de 5 dígitos para continuar."
                notifyUser("Define un PIN secreto de 5 dígitos")
                return@launch
            }

            val cleanAlias = customAlias?.trim()?.takeIf { it.isNotBlank() && !it.contains("@") }
            if (cleanAlias != null && !cleanAlias.equals(_currentUser.value, ignoreCase = true)) {
                applyAliasUpdateInternal(cleanAlias)
            }
            dataStoreManager.setUnlockMethod(method)
            val fid = _familyId.value ?: dataStoreManager.familyIdFlow.firstOrNull()
            repository.setFamilyId(fid)
            _needsUnlockMethodChoice.value = false
            _isAuthenticated.value = true
            checkDefaultsAndSync()
            val label = if (method == "biometric") "Huella Digital" else "PIN de 5 dígitos"
            notifyUser("Acceso configurado con $label ✅")
        }
    }

    fun updateUserAlias(newAlias: String) {
        val clean = newAlias.trim()
        if (clean.isBlank() || clean.contains("@")) {
            notifyUser("Escribe un alias válido (sin correo electrónico).")
            return
        }
        viewModelScope.launch {
            applyAliasUpdateInternal(clean)
            notifyUser("Alias actualizado a '$clean' ✨")
        }
    }

    private suspend fun applyAliasUpdateInternal(cleanAlias: String) {
        val oldAlias = _currentUser.value
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val fid = _familyId.value ?: dataStoreManager.familyIdFlow.firstOrNull().orEmpty()
        dataStoreManager.setUserName(cleanAlias)
        _currentUser.value = cleanAlias
        if (uid != null && fid.isNotBlank()) {
            runCatching { repository.registerUserMapping(uid, fid, cleanAlias) }
        }
        val existingProfile = profiles.value.firstOrNull { it.name.equals(oldAlias, ignoreCase = true) }
        val avatarId = dataStoreManager.userAvatarIdFlow.firstOrNull() ?: existingProfile?.avatarId ?: "woman_1"
        val role = dataStoreManager.familyRoleFlow.firstOrNull() ?: existingProfile?.role ?: "Administrador"
        val colorHex = existingProfile?.colorHex?.ifBlank { "#D0BCFF" } ?: "#D0BCFF"
        if (existingProfile != null && !existingProfile.name.equals(cleanAlias, ignoreCase = true)) {
            runCatching { repository.deleteProfile(existingProfile, cleanAlias) }
        }
        runCatching {
            repository.insertProfile(UserProfile(name = cleanAlias, colorHex = colorHex, avatarId = avatarId, role = role), cleanAlias)
        }
    }

    fun updateUnlockPreference(method: String) {
        viewModelScope.launch {
            if (method == "pin" && !dataStoreManager.hasConfiguredUserPin()) {
                notifyUser("Configura primero un PIN de 5 dígitos antes de cambiar a desbloqueo por PIN.")
                return@launch
            }
            dataStoreManager.setUnlockMethod(method)
            val label = if (method == "biometric") "Huella Digital" else "PIN de 5 dígitos"
            notifyUser("Método de desbloqueo actualizado: $label")
        }
    }

    fun updateUserPin(newPin: String) {
        val clean = newPin.trim()
        if (clean.length != 5 || !clean.all { it.isDigit() } || clean == "00000") {
            notifyUser("El PIN debe tener 5 dígitos y no puede ser 00000.")
            return
        }
        viewModelScope.launch {
            dataStoreManager.updatePin(clean)
            notifyUser("PIN de 5 dígitos actualizado ✅")
        }
    }

    fun createFamily(email: String, pin: String, password: String, customFamilyName: String = "", customUserName: String = "") {
        viewModelScope.launch {
            val cleanEmail = email.trim()
            val cleanPin = pin.trim()
            if (!isValidEmailFormat(cleanEmail)) {
                _authError.value = "Ingresa un correo electrónico real y válido."
                return@launch
            }
            if (cleanPin.length != 5 || !cleanPin.all { it.isDigit() } || cleanPin == "00000") {
                _authError.value = "El PIN debe tener 5 dígitos y no puede ser 00000."
                return@launch
            }
            _isAuthLoading.value = true
            _authError.value = null
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                var isRecreatedAccount = false
                val authResult = try {
                    auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                } catch (collision: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    isRecreatedAccount = true
                    try {
                        auth.signInWithEmailAndPassword(cleanEmail, password).await()
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        throw Exception("Este correo ya tiene una cuenta registrada. Inicia sesión con tu contraseña o usa '¿Olvidaste tu contraseña?' para recuperarla.")
                    }
                }
                val firebaseUser = authResult.user ?: throw Exception("No se pudo crear el usuario")
                val uid = firebaseUser.uid

                val pendingAlias = dataStoreManager.pendingUserAliasFlow.firstOrNull()?.trim().orEmpty()
                val requestedUserName = customUserName.trim().takeIf { it.isNotBlank() && !it.contains("@") }
                    ?: pendingAlias.takeIf { it.isNotBlank() && !it.contains("@") }
                    ?: "Usuario Fami"

                // Si el correo ya existía en Firebase Auth, verificar si ya tiene un hogar registrado
                // para restaurarlo sin sobrescribir /users/{uid} ni dejar huérfanos sus datos.
                var restoredExistingFamily = false
                val existingSnapshot = if (isRecreatedAccount) {
                    runCatching { repository.getUserMapping(uid) }.getOrNull()
                } else null
                val existingFid = existingSnapshot?.child("familyId")?.getValue(String::class.java)?.trim()
                val existingName = existingSnapshot?.child("userName")?.getValue(String::class.java)?.trim()

                val finalFamilyId: String
                val finalUserName: String

                if (!existingFid.isNullOrBlank()) {
                    restoredExistingFamily = true
                    finalFamilyId = existingFid
                    val cleanExisting = existingName?.takeIf { it.isNotBlank() && !it.contains("@") }
                    finalUserName = cleanExisting ?: requestedUserName
                    if (existingName.isNullOrBlank() || existingName.contains("@")) {
                        runCatching { repository.registerUserMapping(uid, finalFamilyId, finalUserName) }
                    }
                } else {
                    finalFamilyId = UUID.randomUUID().toString().substring(0, 8).uppercase()
                    finalUserName = requestedUserName
                    repository.registerUserMapping(uid, finalFamilyId, finalUserName)
                    repository.registerFamilyMember(finalFamilyId, uid)
                }

                dataStoreManager.saveAuthData(
                    familyId = finalFamilyId,
                    userName = finalUserName,
                    pin = cleanPin,
                    email = cleanEmail,
                    authProvider = "email"
                )
                dataStoreManager.setUserName(finalUserName)
                dataStoreManager.setOnboardingCompleted(true)
                _familyId.value = finalFamilyId
                repository.setFamilyId(finalFamilyId)

                if (restoredExistingFamily) {
                    syncLocalProfileAndModeFromCloud(finalFamilyId, finalUserName)
                } else {
                    repository.populateDefaultsIfNeeded()
                    syncPendingNotebookSetup(
                        userName = finalUserName,
                        colorHex = "#D0BCFF",
                        familyNameOverride = customFamilyName,
                        isJoiningExisting = false
                    )
                }

                _currentUser.value = finalUserName

                if (isRecreatedAccount || firebaseUser.isEmailVerified) {
                    _pendingEmailVerification.value = false
                    _needsUnlockMethodChoice.value = true
                    val successMsg = if (restoredExistingFamily) {
                        "¡Ya tenías un hogar Fami! Restauramos tu cuenta, $finalUserName ✨"
                    } else {
                        "¡Cuenta configurada con éxito, $finalUserName! ✨"
                    }
                    notifyUser(successMsg)
                } else {
                    runCatching { firebaseUser.sendEmailVerification().await() }
                    _pendingVerificationEmail.value = cleanEmail
                    _pendingEmailVerification.value = true
                    notifyUser("Te enviamos un correo de verificación a $cleanEmail")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val msg = mapFirebaseAuthError(e)
                _authError.value = msg
                notifyUser(msg)
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun joinFamily(familyCode: String, name: String, pin: String, email: String, password: String) {
        viewModelScope.launch {
            val code = normalizeFamilyCode(familyCode)
            val cleanEmail = email.trim()
            val cleanPin = pin.trim()
            if (code.length < 4) {
                _authError.value = "Ingresa un código familiar válido."
                return@launch
            }
            if (name.trim().contains("@")) {
                _authError.value = "Escribe tu nombre o alias (sin correo electrónico)."
                return@launch
            }
            if (!isValidEmailFormat(cleanEmail)) {
                _authError.value = "Ingresa un correo electrónico real y válido."
                return@launch
            }
            if (!isValidPin(cleanPin)) {
                _authError.value = "El PIN debe tener 5 dígitos y no puede ser 00000."
                return@launch
            }
            _isAuthLoading.value = true
            _authError.value = null
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                var isNewAccount = false
                val authResult = try {
                    isNewAccount = true
                    auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                } catch (collision: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    isNewAccount = false
                    try {
                        auth.signInWithEmailAndPassword(cleanEmail, password).await()
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        throw Exception("Este correo ya tiene una cuenta registrada. Inicia sesión con tu contraseña o recupérala.")
                    }
                }
                val firebaseUser = authResult.user ?: throw Exception("No se pudo obtener el usuario")
                val uid = firebaseUser.uid

                val pendingAlias = dataStoreManager.pendingUserAliasFlow.firstOrNull()?.trim().orEmpty()
                val cleanName = name.trim().takeIf { it.isNotBlank() && !it.contains("@") }
                    ?: pendingAlias.takeIf { it.isNotBlank() && !it.contains("@") }
                    ?: "Miembro Fami"

                repository.registerFamilyMember(code, uid)
                val familyValid = repository.verifyJoinedFamilyHasExistingData(code, uid)
                if (!familyValid) {
                    throw Exception("El código familiar '$code' no existe. Verifica el código con el administrador del hogar.")
                }
                repository.registerUserMapping(uid, code, cleanName)

                dataStoreManager.saveAuthData(
                    familyId = code,
                    userName = cleanName,
                    pin = cleanPin,
                    email = cleanEmail,
                    authProvider = "email"
                )
                dataStoreManager.setUserName(cleanName)
                dataStoreManager.setManagementMode("familiar")
                dataStoreManager.setOnboardingCompleted(true)
                _familyId.value = code
                repository.setFamilyId(code)

                syncPendingNotebookSetup(userName = cleanName, colorHex = "#EFB8C8", isJoiningExisting = true)
                _currentUser.value = cleanName
                FamiUsageTracker.track(dataStoreManager.appContext, FamiUsageTracker.FEATURE_FAMILY_JOINED_CODE)

                if (isNewAccount && !firebaseUser.isEmailVerified) {
                    runCatching { firebaseUser.sendEmailVerification().await() }
                    _pendingVerificationEmail.value = cleanEmail
                    _pendingEmailVerification.value = true
                    notifyUser("Verifica tu correo ($cleanEmail) para completar el acceso")
                } else {
                    _needsUnlockMethodChoice.value = true
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val msg = mapFirebaseAuthError(e)
                _authError.value = msg
                notifyUser(msg)
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun checkEmailVerified() {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            try {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    _pendingEmailVerification.value = false
                    _authError.value = "Sesión expirada. Inicia sesión nuevamente."
                    return@launch
                }
                user.reload().await()
                if (user.isEmailVerified) {
                    _pendingEmailVerification.value = false
                    _needsUnlockMethodChoice.value = true
                    notifyUser("¡Correo verificado con éxito! ✨")
                } else {
                    _authError.value = "Tu correo aún no aparece verificado. Abre el enlace en tu bandeja (o Spam) y vuelve a intentarlo."
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _authError.value = "Error al verificar: ${e.localizedMessage}"
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            try {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    user.sendEmailVerification().await()
                    notifyUser("Correo de verificación reenviado a ${user.email}")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _authError.value = mapFirebaseAuthError(e)
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun cancelPendingVerification() {
        viewModelScope.launch {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            dataStoreManager.clearAuthData()
            _pendingEmailVerification.value = false
            _pendingVerificationEmail.value = ""
            _familyId.value = null
            _currentUser.value = ""
        }
    }

    fun sendPasswordReset(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val cleanEmail = email.trim()
            if (!isValidEmailFormat(cleanEmail)) {
                _authError.value = "Ingresa un correo electrónico válido para recuperar tu cuenta."
                return@launch
            }
            _isAuthLoading.value = true
            _authError.value = null
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().sendPasswordResetEmail(cleanEmail).await()
                notifyUser("Correo de recuperación enviado a $cleanEmail")
                onSuccess()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val msg = mapFirebaseAuthError(e)
                _authError.value = msg
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun confirmPasswordResetWithCode(oobCodeOrLink: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val cleanCode = extractOobCode(oobCodeOrLink)
            if (cleanCode.isBlank()) {
                _authError.value = "Código de recuperación inválido."
                return@launch
            }
            if (newPassword.length < 6) {
                _authError.value = "La nueva contraseña debe tener al menos 6 caracteres."
                return@launch
            }
            _isAuthLoading.value = true
            _authError.value = null
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance()
                    .confirmPasswordReset(cleanCode, newPassword).await()
                notifyUser("Contraseña actualizada. Ya puedes iniciar sesión.")
                onSuccess()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _authError.value = mapFirebaseAuthError(e)
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    private fun extractOobCode(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.contains("oobCode=")) {
            return trimmed.substringAfter("oobCode=").substringBefore("&").trim()
        }
        return trimmed
    }

    fun authenticateWithGoogleEmail(
        email: String,
        joinFamilyCode: String = "",
        customFamilyName: String = "",
        customUserAlias: String = "",
        existingPasswordToLink: String = "",
        onNeedsExistingPassword: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val cleanEmail = email.trim()
            if (!isValidEmailFormat(cleanEmail)) {
                _authError.value = "No se pudo obtener un correo válido de la cuenta de Google."
                return@launch
            }
            if (existingPasswordToLink.isBlank()) {
                _isAuthLoading.value = false
                onNeedsExistingPassword(cleanEmail)
                return@launch
            }
            _isAuthLoading.value = true
            _authError.value = null
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val authResult = try {
                    auth.signInWithEmailAndPassword(cleanEmail, existingPasswordToLink).await()
                } catch (notFound: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                    auth.createUserWithEmailAndPassword(cleanEmail, existingPasswordToLink).await()
                }

                val firebaseUser = authResult.user ?: throw Exception("No se pudo iniciar sesión con Google.")
                handleGoogleFirebaseUser(
                    firebaseUser = firebaseUser,
                    joinFamilyCode = joinFamilyCode,
                    customFamilyName = customFamilyName,
                    customUserAlias = customUserAlias
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _isAuthLoading.value = false
                val msg = mapFirebaseAuthError(e)
                _authError.value = msg
                notifyUser(msg)
            }
        }
    }

    private fun sanitizeAliasFromEmail(email: String): String {
        val localPart = email.substringBefore("@").trim()
        val cleaned = localPart
            .replace(Regex("[.#\\$\\[\\]/_]+"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
        return cleaned.ifBlank { "Usuario Fami" }
    }

    fun handleGoogleFirebaseUser(
        firebaseUser: com.google.firebase.auth.FirebaseUser,
        joinFamilyCode: String = "",
        customFamilyName: String = "",
        pin: String = "",
        customUserAlias: String = ""
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            try {
                val uid = firebaseUser.uid
                val email = firebaseUser.email.orEmpty()
                val pendingAlias = dataStoreManager.pendingUserAliasFlow.firstOrNull()?.trim().orEmpty()
                val emailFallbackAlias = sanitizeAliasFromEmail(email)
                val preferredAlias = customUserAlias.trim().takeIf { it.isNotBlank() && !it.contains("@") }
                    ?: pendingAlias.takeIf { it.isNotBlank() && !it.contains("@") }
                    ?: firebaseUser.displayName?.trim()?.takeIf { it.isNotBlank() && !it.contains("@") }
                    ?: emailFallbackAlias

                val snapshot = repository.getUserMapping(uid)
                val existingFid = snapshot.child("familyId").getValue(String::class.java)
                val existingName = snapshot.child("userName").getValue(String::class.java)

                val finalFid: String
                val finalUserName: String
                val isNewSetup: Boolean

                if (normalizeFamilyCode(joinFamilyCode).isNotBlank()) {
                    val code = normalizeFamilyCode(joinFamilyCode)
                    repository.registerFamilyMember(code, uid)
                    val familyValid = repository.verifyJoinedFamilyHasExistingData(code, uid)
                    if (!familyValid) {
                        throw Exception("El código familiar '$code' no existe. Verifica el código con el administrador del hogar.")
                    }
                    finalFid = code
                    finalUserName = preferredAlias
                    repository.registerUserMapping(uid, code, finalUserName)
                    dataStoreManager.setManagementMode("familiar")
                    _familyId.value = code
                    repository.setFamilyId(code)
                    syncPendingNotebookSetup(userName = finalUserName, colorHex = "#EFB8C8", isJoiningExisting = true)
                    isNewSetup = true
                } else if (!existingFid.isNullOrBlank() && !existingName.isNullOrBlank()) {
                    val hasInvalidChars = existingName.contains("@") || existingName.contains(".") || existingName.contains("#") || existingName.contains("$") || existingName.contains("[") || existingName.contains("]")
                    finalFid = existingFid
                    finalUserName = if (hasInvalidChars) sanitizeAliasFromEmail(existingName) else existingName
                    if (hasInvalidChars) {
                        runCatching { repository.registerUserMapping(uid, finalFid, finalUserName) }
                    }
                    isNewSetup = false
                } else {
                    finalFid = UUID.randomUUID().toString().substring(0, 8).uppercase()
                    finalUserName = preferredAlias
                    repository.registerUserMapping(uid, finalFid, finalUserName)
                    repository.registerFamilyMember(finalFid, uid)
                    _familyId.value = finalFid
                    repository.setFamilyId(finalFid)
                    repository.populateDefaultsIfNeeded()
                    syncPendingNotebookSetup(
                        userName = finalUserName,
                        colorHex = "#D0BCFF",
                        familyNameOverride = customFamilyName.ifBlank { pendingFamilyName.value },
                        isJoiningExisting = false
                    )
                    isNewSetup = true
                }

                dataStoreManager.saveAuthData(
                    familyId = finalFid,
                    userName = finalUserName,
                    pin = pin,
                    email = email,
                    authProvider = "google"
                )
                dataStoreManager.setUserName(finalUserName)
                dataStoreManager.setOnboardingCompleted(true)
                _familyId.value = finalFid
                repository.setFamilyId(finalFid)
                _currentUser.value = finalUserName
                if (!isNewSetup) {
                    syncLocalProfileAndModeFromCloud(finalFid, finalUserName)
                }

                val savedMethod = dataStoreManager.unlockMethodFlow.firstOrNull() ?: "unset"
                if (isNewSetup || savedMethod == "unset") {
                    _needsUnlockMethodChoice.value = true
                } else {
                    _isAuthenticated.value = true
                    checkDefaultsAndSync()
                    notifyUser("¡Bienvenido, $finalUserName!")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val msg = mapFirebaseAuthError(e)
                _authError.value = msg
                notifyUser(msg)
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    private fun mapFirebaseAuthError(e: Exception): String {
        val msg = e.localizedMessage ?: "Error de autenticación"
        return when {
            msg.contains("password is invalid", ignoreCase = true) || msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
                "Correo o contraseña incorrectos."
            msg.contains("no user record", ignoreCase = true) || msg.contains("USER_NOT_FOUND", ignoreCase = true) ->
                "No existe una cuenta registrada con ese correo."
            msg.contains("already in use", ignoreCase = true) ->
                "Este correo ya tiene una cuenta. Inicia sesión o recupera tu contraseña."
            msg.contains("network", ignoreCase = true) ->
                "Error de conexión. Verifica tu internet e intenta nuevamente."
            else -> msg
        }
    }

    private suspend fun syncPendingNotebookSetup(
        userName: String,
        colorHex: String,
        familyNameOverride: String = "",
        isJoiningExisting: Boolean = false
    ) {
        val avatarId = dataStoreManager.userAvatarIdFlow.firstOrNull() ?: "woman_1"
        val role = dataStoreManager.familyRoleFlow.firstOrNull() ?: if (isJoiningExisting) "Miembro familiar" else "Administrador"
        val mode = if (isJoiningExisting) "familiar" else (dataStoreManager.managementModeFlow.firstOrNull() ?: "familiar")
        val famName = familyNameOverride.trim().ifBlank { dataStoreManager.pendingFamilyNameFlow.firstOrNull()?.trim().orEmpty() }
        val monthlyInc = dataStoreManager.monthlyIncomeAmountFlow.firstOrNull() ?: 0.0
        val monthlyIncCurr = dataStoreManager.monthlyIncomeCurrencyFlow.firstOrNull() ?: "USD"
        val liveRate = dataStoreManager.liveExchangeRateFlow.firstOrNull() ?: 36.85
        val walletsJson = dataStoreManager.pendingWalletsJsonFlow.firstOrNull() ?: "[]"

        repository.insertProfile(
            UserProfile(name = userName, colorHex = colorHex, avatarId = avatarId, role = role),
            userName
        )

        if (!isJoiningExisting) {
            if (famName.isNotBlank()) {
                repository.insertSetting("family_name", famName, userName)
            }
            repository.insertSetting("management_mode", mode, userName)

            if (monthlyInc > 0.0) {
                val normCurr = com.example.currency.CountryCurrencyConfig.normalizeCurrencyCode(monthlyIncCurr)
                val resolvedRate = when {
                    normCurr == "USD" -> 1.0
                    normCurr.equals(primaryCurrency.value, ignoreCase = true) && liveRate > 0.0 -> liveRate
                    else -> com.example.api.BcvExchangeRateService.fetchUsdToCurrencyRate(normCurr)
                }
                repository.insertSetting("monthly_income_amount", String.format(java.util.Locale.US, "%.2f", monthlyInc), userName)
                repository.insertSetting("monthly_income_currency", normCurr, userName)
                if (resolvedRate > 0.0) {
                    val amountUsd = if (normCurr == "USD") monthlyInc else monthlyInc / resolvedRate
                    repository.insertSetting("total_limit_usd", String.format(java.util.Locale.US, "%.2f", amountUsd), userName)
                }
            }
        }

        val initialWallets = dataStoreManager.parseWalletsJson(walletsJson)
        if (initialWallets.isNotEmpty()) {
            initialWallets.forEach { w ->
                repository.insertWallet(w.copy(ownerName = userName), userName)
            }
            dataStoreManager.clearPendingWallets()
        }
    }

    private suspend fun syncLocalProfileAndModeFromCloud(familyId: String, userName: String) {
        if (familyId.isBlank()) return
        try {
            val dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().reference
                .child("families").child(familyId)
            val modeSnap = dbRef.child("app_settings").child("management_mode").child("value").get().await()
            val cloudMode = modeSnap.getValue(String::class.java)?.trim().orEmpty()
            if (cloudMode.equals("individual", ignoreCase = true) || cloudMode.equals("familiar", ignoreCase = true)) {
                dataStoreManager.setManagementMode(cloudMode.lowercase())
            }
            val safeKey = userName.trim().replace(Regex("[.#$\\[\\]/]"), "_")
            if (safeKey.isNotBlank()) {
                val profSnap = dbRef.child("user_profiles").child(safeKey).get().await()
                val profile = profSnap.getValue(UserProfile::class.java)
                if (profile != null) {
                    if (profile.role.isNotBlank()) {
                        dataStoreManager.setFamilyRole(profile.role)
                    }
                    if (profile.avatarId.isNotBlank()) {
                        dataStoreManager.setUserAvatarId(profile.avatarId)
                    }
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
        }
    }

    fun loginExisting(email: String, password: String, pin: String) {
        viewModelScope.launch {
            val cleanEmail = email.trim()
            val cleanPin = pin.trim()
            if (!isValidEmailFormat(cleanEmail)) {
                _authError.value = "Ingresa un correo electrónico válido."
                return@launch
            }
            if (cleanPin.length != 5 || !cleanPin.all { it.isDigit() } || cleanPin == "00000") {
                _authError.value = "El PIN debe tener 5 dígitos y no puede ser 00000."
                return@launch
            }
            _isAuthLoading.value = true
            _authError.value = null
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val authResult = auth.signInWithEmailAndPassword(cleanEmail, password).await()
                val firebaseUser = authResult.user ?: throw Exception("No se pudo obtener el UID")
                val uid = firebaseUser.uid

                val snapshot = repository.getUserMapping(uid)
                val fid = snapshot.child("familyId").getValue(String::class.java)
                val userName = snapshot.child("userName").getValue(String::class.java)

                val finalFid: String
                val finalUserName: String
                val wasRecreated: Boolean
                if (fid.isNullOrBlank() || userName.isNullOrBlank()) {
                    // Si el usuario había eliminado su cuenta antes (borrando /users/{uid}),
                    // reconstruimos su espacio familiar automáticamente al iniciar sesión.
                    finalFid = UUID.randomUUID().toString().substring(0, 8).uppercase()
                    val pendingAlias = dataStoreManager.pendingUserAliasFlow.firstOrNull()?.trim().orEmpty()
                    finalUserName = pendingAlias.takeIf { it.isNotBlank() && !it.contains("@") }
                        ?: firebaseUser.displayName?.trim()?.takeIf { it.isNotBlank() && !it.contains("@") }
                        ?: sanitizeAliasFromEmail(cleanEmail)

                    repository.registerUserMapping(uid, finalFid, finalUserName)
                    repository.registerFamilyMember(finalFid, uid)
                    _familyId.value = finalFid
                    repository.setFamilyId(finalFid)
                    repository.populateDefaultsIfNeeded()
                    syncPendingNotebookSetup(userName = finalUserName, colorHex = "#D0BCFF", isJoiningExisting = false)
                    wasRecreated = true
                } else {
                    val hasInvalidChars = userName.contains("@") || userName.contains(".") || userName.contains("#") || userName.contains("$") || userName.contains("[") || userName.contains("]")
                    finalFid = fid
                    finalUserName = if (hasInvalidChars) sanitizeAliasFromEmail(userName) else userName
                    if (hasInvalidChars) {
                        runCatching { repository.registerUserMapping(uid, finalFid, finalUserName) }
                    }
                    wasRecreated = false
                }

                dataStoreManager.saveAuthData(
                    familyId = finalFid,
                    userName = finalUserName,
                    pin = cleanPin,
                    email = cleanEmail,
                    authProvider = "email"
                )
                dataStoreManager.setUserName(finalUserName)
                dataStoreManager.setOnboardingCompleted(true)
                _familyId.value = finalFid
                repository.setFamilyId(finalFid)
                _currentUser.value = finalUserName
                if (!wasRecreated) {
                    syncLocalProfileAndModeFromCloud(finalFid, finalUserName)
                }

                val savedMethod = dataStoreManager.unlockMethodFlow.firstOrNull() ?: "unset"
                if (savedMethod == "unset") {
                    _needsUnlockMethodChoice.value = true
                } else {
                    _isAuthenticated.value = true
                    checkDefaultsAndSync()
                    notifyUser("¡Bienvenido de vuelta, $finalUserName!")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val msg = mapFirebaseAuthError(e)
                _authError.value = msg
                notifyUser(msg)
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            dataStoreManager.clearAuthData()
            repository.setFamilyId(null)
            _isAuthenticated.value = false
            _pendingEmailVerification.value = false
            _needsUnlockMethodChoice.value = false
            _familyId.value = null
            _currentUser.value = ""
        }
    }

    fun syncFcmTokenAndWorker() {
        val ctx = dataStoreManager.appContext
        com.example.notifications.TaskNotificationHelper.ensureChannel(ctx)
        runCatching { com.example.notifications.ChoreSyncWorker.schedule(ctx) }
        val userName = _currentUser.value
        val fid = _familyId.value
        if (userName.isBlank() || fid.isNullOrBlank()) return
        runCatching {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (!token.isNullOrBlank()) {
                        viewModelScope.launch {
                            repository.registerFcmToken(token, _currentUser.value)
                        }
                    }
                }
        }
    }

    private fun checkDefaultsAndSync() {
        syncFcmTokenAndWorker()
        viewModelScope.launch {
            try {
                com.example.api.GeminiConfig.refreshRemoteConfigIfNeeded()
                val cur = _currentUser.value.trim()
                val fid = _familyId.value.orEmpty()
                if (cur.contains(".") || cur.contains("@") || cur.contains("#") || cur.contains("$") || cur.contains("[") || cur.contains("]")) {
                    val cleaned = if (cur.contains("@")) sanitizeAliasFromEmail(cur) else sanitizeAliasFromEmail("$cur@fami")
                    applyAliasUpdateInternal(cleaned)
                }
                if (fid.isNotBlank() && _currentUser.value.isNotBlank()) {
                    syncLocalProfileAndModeFromCloud(fid, _currentUser.value)
                }
                syncExchangeRateFor(countryCode.value, primaryCurrency.value, secondaryCurrency.value)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }

    fun clearNlpError() {
        _nlpError.value = null
    }

    private fun notifyUser(message: String) {
        _topNotification.value = message
    }

    fun dismissNotification() {
        _topNotification.value = null
    }

    // --- Core functionality copied from previous implementation ---
    suspend fun uploadReceiptImage(bitmap: android.graphics.Bitmap): String? {
        return repository.uploadReceiptImage(bitmap)
    }

    fun addTransaction(
        originalAmount: Double,
        category: String,
        description: String,
        currency: String = "USD",
        isIncome: Boolean = false,
        date: Long = System.currentTimeMillis(),
        receiptUrl: String = "",
        balanceDate: Long = 0L,
        items: List<com.example.database.TransactionItem> = emptyList(),
        walletId: String = "",
        onFinished: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            if (!_isAuthenticated.value || (_familyId.value ?: repository._familyId.value).isNullOrBlank()) {
                val msg = "No hay sesión o familia activa para guardar el movimiento."
                notifyUser(msg)
                onFinished?.invoke(false, msg)
                return@launch
            }
            val user = _currentUser.value
            val resolvedWalletId = walletId.ifBlank {
                wallets.value.firstOrNull { it.currency.equals(currency, ignoreCase = true) }?.id.orEmpty()
            }
            val tx = Transaction(
                amount = originalAmount,
                category = category,
                description = description,
                date = date,
                registeredBy = user,
                currency = currency,
                originalAmount = originalAmount,
                isIncome = isIncome,
                receiptUrl = receiptUrl,
                balanceDate = balanceDate,
                items = items,
                walletId = resolvedWalletId
            )
            try {
                val alertMessage = repository.insertTransaction(tx, user, isNewEntry = true)
                FamiUsageTracker.track(dataStoreManager.appContext, FamiUsageTracker.FEATURE_TX_MANUAL)
                val msg = alertMessage ?: "¡Guardado!: $originalAmount $currency en '$description'"
                notifyUser(msg)
                onFinished?.invoke(true, msg)
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (e: Exception) {
                val errMsg = e.localizedMessage ?: "No se pudo guardar el movimiento."
                notifyUser(errMsg)
                onFinished?.invoke(false, errMsg)
            }
        }
    }
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transaction, _currentUser.value)
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (e: Exception) {
                notifyUser(e.localizedMessage ?: "No se pudo eliminar el movimiento.")
            }
        }
    }
    fun editTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.insertTransaction(transaction, _currentUser.value, isNewEntry = false)
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (e: Exception) {
                notifyUser(e.localizedMessage ?: "No se pudo editar el movimiento.")
            }
        }
    }

    fun createOrEditCategory(oldName: String?, name: String, limit: Double, iconEmoji: String, monthKey: String) {
        viewModelScope.launch {
            val user = _currentUser.value
            val newName = name.trim()
            var oldCat = categories.value.find { it.categoryName == oldName }
            if (oldName != null && oldName != newName) {
                if (oldCat != null) repository.deleteCategory(oldCat, user)
                val txsToUpdate = transactions.value.filter { it.category == oldName }
                for (tx in txsToUpdate) {
                    repository.insertTransaction(tx.copy(category = newName), user, isNewEntry = false)
                }
            } else if (oldCat == null) {
                oldCat = categories.value.find { it.categoryName == newName }
            }
            
            val newLimitsMap = oldCat?.limitsPerMonth?.toMutableMap() ?: mutableMapOf()
            newLimitsMap[monthKey] = limit
            
            repository.insertCategory(CategoryDetail(newName, oldCat?.limitAmount ?: limit, colorHex = oldCat?.colorHex ?: "", iconEmoji = iconEmoji, limitsPerMonth = newLimitsMap), user)
        }
    }
    fun deleteCategory(category: CategoryDetail) { viewModelScope.launch { repository.deleteCategory(category, _currentUser.value) } }

    fun updateMyRole(newRole: String) {
        val clean = newRole.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            dataStoreManager.setFamilyRole(clean)
            val user = _currentUser.value
            if (user.isNotBlank()) {
                val existing = profiles.value.find { it.name.equals(user, ignoreCase = true) }
                val avatarId = dataStoreManager.userAvatarIdFlow.firstOrNull() ?: existing?.avatarId ?: "woman_1"
                val colorHex = existing?.colorHex?.ifBlank { "#D0BCFF" } ?: "#D0BCFF"
                runCatching {
                    repository.insertProfile(
                        UserProfile(name = user, colorHex = colorHex, avatarId = avatarId, role = clean),
                        user
                    )
                }
            }
            notifyUser("Rol actualizado a '$clean' ✨")
        }
    }

    fun updateFamilyName(newName: String) {
        val clean = newName.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val user = _currentUser.value.ifBlank { "Usuario Fami" }
            runCatching { repository.insertSetting("family_name", clean, user) }
            notifyUser("Nombre del espacio actualizado a '$clean' ✨")
        }
    }

    fun switchToExistingFamilyByCode(
        familyCode: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val code = normalizeFamilyCode(familyCode)
            if (code.length < 4) {
                onResult(false, "Ingresa un código familiar válido (mínimo 4 caracteres).")
                return@launch
            }
            val currentFid = _familyId.value ?: dataStoreManager.familyIdFlow.firstOrNull()
            if (code.equals(currentFid, ignoreCase = true)) {
                onResult(false, "Ya perteneces a este hogar ($code).")
                return@launch
            }
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (firebaseUser == null) {
                onResult(false, "Sesión expirada. Inicia sesión nuevamente.")
                return@launch
            }
            val uid = firebaseUser.uid
            val userName = _currentUser.value.ifBlank { dataStoreManager.userNameFlow.firstOrNull().orEmpty() }
                .ifBlank { "Miembro Fami" }
            try {
                repository.registerFamilyMember(code, uid)
                val familyValid = repository.verifyJoinedFamilyHasExistingData(code, uid)
                if (!familyValid) {
                    onResult(false, "El código '$code' no existe. Verifica con el administrador del hogar.")
                    return@launch
                }
                repository.registerUserMapping(uid, code, userName)
                dataStoreManager.saveAuthData(
                    familyId = code,
                    userName = userName,
                    pin = dataStoreManager.userPinFlow.firstOrNull().orEmpty(),
                    email = firebaseUser.email.orEmpty(),
                    authProvider = dataStoreManager.authProviderFlow.firstOrNull() ?: "email"
                )
                dataStoreManager.setManagementMode("familiar")
                val avatarId = dataStoreManager.userAvatarIdFlow.firstOrNull() ?: "woman_1"
                val role = dataStoreManager.familyRoleFlow.firstOrNull()
                    ?.takeIf { it.isNotBlank() && !it.equals("Administrador", ignoreCase = true) }
                    ?: "Miembro familiar"
                dataStoreManager.setFamilyRole(role)
                _familyId.value = code
                repository.setFamilyId(code)
                runCatching {
                    repository.insertProfile(
                        UserProfile(name = userName, colorHex = "#EFB8C8", avatarId = avatarId, role = role),
                        userName
                    )
                }
                syncFcmTokenAndWorker()
                FamiUsageTracker.track(dataStoreManager.appContext, FamiUsageTracker.FEATURE_FAMILY_JOINED_CODE)
                val msg = "¡Te uniste al hogar $code con éxito! 🎉"
                notifyUser(msg)
                onResult(true, msg)
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (e: Exception) {
                val err = mapFirebaseAuthError(e)
                onResult(false, err)
            }
        }
    }

    fun createProfile(name: String, colorHex: String, avatarId: String = "woman_1", role: String = "Miembro familiar") {
        viewModelScope.launch {
            val cleanName = name.trim()
            val cleanRole = role.trim().ifBlank { "Miembro familiar" }
            if (cleanName.isNotEmpty() && !cleanName.contains("@")) {
                repository.insertProfile(UserProfile(cleanName, colorHex, avatarId = avatarId, role = cleanRole), _currentUser.value)
                notifyUser("Miembro '$cleanName' añadido ✨")
            }
        }
    }

    fun editProfile(
        oldProfile: UserProfile,
        newName: String,
        newColorHex: String,
        avatarId: String = oldProfile.avatarId,
        role: String = oldProfile.role
    ) {
        viewModelScope.launch {
            val cleanName = newName.trim()
            val cleanRole = role.trim().ifBlank { oldProfile.role.ifBlank { "Miembro familiar" } }
            if (cleanName.isNotEmpty() && !cleanName.contains("@")) {
                if (!oldProfile.name.equals(cleanName, ignoreCase = true)) {
                    repository.deleteProfile(oldProfile, _currentUser.value)
                }
                repository.insertProfile(
                    UserProfile(cleanName, newColorHex, avatarId = avatarId, role = cleanRole),
                    _currentUser.value
                )
                if (_currentUser.value.equals(oldProfile.name, ignoreCase = true)) {
                    if (avatarId.isNotBlank()) dataStoreManager.setUserAvatarId(avatarId)
                    dataStoreManager.setFamilyRole(cleanRole)
                    if (!oldProfile.name.equals(cleanName, ignoreCase = true)) {
                        applyAliasUpdateInternal(cleanName)
                    }
                    syncFcmTokenAndWorker()
                }
                notifyUser("Perfil '$cleanName' actualizado ✨")
            }
        }
    }
    fun deleteProfile(profile: UserProfile) { viewModelScope.launch { repository.deleteProfile(profile, _currentUser.value) } }

    fun syncLiveBcvRate() { viewModelScope.launch { syncExchangeRateFor(countryCode.value, primaryCurrency.value, secondaryCurrency.value) } }
    fun updateValueSetting(key: String, value: String) { viewModelScope.launch { repository.insertSetting(key, value, _currentUser.value) } }

    fun registerFromNaturalLanguage(
        inputText: String,
        walletId: String = "",
        onFinished: ((Boolean, String) -> Unit)? = null
    ) {
        if (inputText.isBlank()) return
        viewModelScope.launch {
            if (!_isAuthenticated.value || (_familyId.value ?: repository._familyId.value).isNullOrBlank()) {
                val err = "No hay familia activa o sesión desbloqueada."
                _nlpError.value = err
                onFinished?.invoke(false, err)
                return@launch
            }
            _isRegisteringNlp.value = true
            _nlpError.value = null
            try {
                val user = _currentUser.value
                val customKey = settings.value.find { it.key == "gemini_api_key" }?.value
                val categoryNameList = categories.value.map { it.categoryName }.ifEmpty { listOf("Alimentos", "Servicios", "Otros") }
                val (transaction, error, successMsg) = GeminiParser.parseTextToTransaction(inputText, user, categoryNameList, customKey)
                if (error != null) {
                    _nlpError.value = error
                    onFinished?.invoke(false, error)
                } else if (transaction != null) {
                    val resolvedWalletId = walletId.ifBlank {
                        wallets.value.firstOrNull { it.currency.equals(transaction.currency, ignoreCase = true) }?.id.orEmpty()
                    }
                    repository.insertTransaction(transaction.copy(walletId = resolvedWalletId), user, isNewEntry = true)
                    FamiUsageTracker.track(dataStoreManager.appContext, FamiUsageTracker.FEATURE_TX_VOICE_NLP)
                    val finalMsg = successMsg.ifBlank { "IA registró gasto/ingreso exitosamente." }
                    notifyUser(finalMsg)
                    onFinished?.invoke(true, finalMsg)
                } else {
                    val err = "No se pudo interpretar el movimiento."
                    _nlpError.value = err
                    onFinished?.invoke(false, err)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val err = "Error: ${e.message}"
                _nlpError.value = err
                onFinished?.invoke(false, err)
            } finally {
                _isRegisteringNlp.value = false
            }
        }
    }

    fun addChore(title: String, assignedTo: String, description: String, priority: String, category: String = "General", isPrivate: Boolean = false) {
        viewModelScope.launch {
            val chore = Chore(title = title, assignedTo = assignedTo, createdBy = _currentUser.value, description = description, priority = priority, category = category)
            chore.isPrivate = isPrivate
            repository.insertChore(chore, _currentUser.value)
            FamiUsageTracker.track(dataStoreManager.appContext, FamiUsageTracker.FEATURE_CHORE_CREATED)
        }
    }
    fun toggleChoreCompletion(chore: Chore) { if (!chore.isCompleted) triggerConfetti()
        viewModelScope.launch { repository.updateChoreStatus(chore.id, chore.title, !chore.isCompleted, _currentUser.value) } }
    fun deleteChore(chore: Chore) { viewModelScope.launch { repository.deleteChore(chore, _currentUser.value) } }
    fun editChore(chore: Chore) { viewModelScope.launch { repository.insertChore(chore, _currentUser.value) } }

    fun renameTaskList(oldName: String, newName: String) {
        viewModelScope.launch {
            val cleanNew = newName.replace(",", " ").trim()
            if (cleanNew.isBlank()) return@launch
            val choresToRename = chores.value.filter { it.category == oldName }
            choresToRename.forEach { chore ->
                repository.insertChore(chore.copy(category = cleanNew), _currentUser.value)
            }
            val currentSetting = settings.value.find { it.key == "task_lists" }?.value
            if (currentSetting != null) {
                val lists = currentSetting.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val newLists = lists.map { if (it == oldName) cleanNew else it }
                updateValueSetting("task_lists", newLists.joinToString(","))
            }
        }
    }

    fun addShoppingItem(name: String, quantity: String, isPrivate: Boolean = false, listName: String = "General") {
        viewModelScope.launch {
            val item = ShoppingItem(name = name, quantity = quantity, addedBy = _currentUser.value, dateAdded = System.currentTimeMillis(), listName = listName)
            item.isPrivate = isPrivate
            repository.insertShoppingItem(item, _currentUser.value)
            FamiUsageTracker.track(dataStoreManager.appContext, FamiUsageTracker.FEATURE_SHOPPING_ADDED)
        }
    }
    fun toggleShoppingItemBought(item: ShoppingItem) { if (!item.isBought) triggerConfetti()
        viewModelScope.launch { repository.updateShoppingItemStatus(item.id, item.name, !item.isBought, _currentUser.value) } }
    fun deleteShoppingItem(item: ShoppingItem) { viewModelScope.launch { repository.deleteShoppingItem(item, _currentUser.value) } }
    fun editShoppingItem(item: ShoppingItem) { viewModelScope.launch { repository.insertShoppingItem(item, _currentUser.value) } }

    fun renameShoppingList(oldName: String, newName: String) {
        viewModelScope.launch {
            val itemsToRename = shoppingItems.value.filter { it.listName == oldName }
            itemsToRename.forEach { item ->
                repository.insertShoppingItem(item.copy(listName = newName), _currentUser.value)
            }
            val currentSetting = settings.value.find { it.key == "shopping_lists" }?.value
            if (currentSetting != null) {
                val lists = currentSetting.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val newLists = lists.map { if (it == oldName) newName else it }
                updateValueSetting("shopping_lists", newLists.joinToString(","))
            }
        }
    }

    fun deleteShoppingList(listName: String) {
        viewModelScope.launch {
            val itemsToDelete = shoppingItems.value.filter { it.listName == listName }
            itemsToDelete.forEach { item ->
                repository.deleteShoppingItem(item, _currentUser.value)
            }
            val currentSetting = settings.value.find { it.key == "shopping_lists" }?.value
            if (currentSetting != null) {
                val lists = currentSetting.split(",").map { it.trim() }.filter { it.isNotEmpty() && it != listName }
                updateValueSetting("shopping_lists", lists.joinToString(","))
            }
        }
    }

    fun addGoal(
        title: String,
        targetAmount: Double?,
        savedAmount: Double = 0.0,
        currency: String = primaryCurrency.value,
        isPrivate: Boolean = false
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val cleanSaved = savedAmount.coerceAtLeast(0.0)
            val completedNow = targetAmount != null && targetAmount > 0.0 && cleanSaved >= targetAmount
            if (completedNow) triggerConfetti()
            val goal = Goal(
                title = title,
                targetAmount = targetAmount,
                savedAmount = cleanSaved,
                currency = currency.trim().uppercase().ifBlank { primaryCurrency.value },
                createdBy = user,
                createdDate = System.currentTimeMillis(),
                isCompleted = completedNow,
                isPrivate = isPrivate
            )
            repository.insertGoal(goal, user)
            FamiUsageTracker.track(dataStoreManager.appContext, FamiUsageTracker.FEATURE_GOAL_CREATED)
        }
    }

    fun addSavingsToGoal(goal: Goal, depositAmount: Double) {
        if (depositAmount <= 0.0) return
        viewModelScope.launch {
            val user = _currentUser.value
            val newSaved = (goal.savedAmount + depositAmount).coerceAtLeast(0.0)
            val reachedTarget = goal.targetAmount != null && goal.targetAmount > 0.0 && newSaved >= goal.targetAmount
            val updated = goal.copy(
                savedAmount = newSaved,
                isCompleted = goal.isCompleted || reachedTarget,
                currency = goal.currency.ifBlank { primaryCurrency.value }
            )
            repository.insertGoal(updated, user)
            val curr = updated.currency
            val sym = com.example.currency.CountryCurrencyConfig.getCurrencySymbol(curr)
            runCatching {
                repository.insertLog(
                    ActivityLog(
                        tag = "METAS",
                        message = "$user abonó $sym ${String.format(java.util.Locale.US, "%,.2f", depositAmount)} $curr a la meta '${goal.title}'",
                        user = user
                    )
                )
            }
            if (reachedTarget && !goal.isCompleted) {
                triggerConfetti()
                notifyUser("🎉 ¡Meta '${goal.title}' completada!")
            } else {
                notifyUser("💰 Ahorro sumado a '${goal.title}': +$sym ${String.format(java.util.Locale.US, "%,.2f", depositAmount)} $curr")
            }
        }
    }

    fun editGoal(goal: Goal) { if (goal.isCompleted) triggerConfetti()
        viewModelScope.launch { repository.insertGoal(goal, _currentUser.value) } }
    fun deleteGoal(goal: Goal) { viewModelScope.launch { repository.deleteGoal(goal, _currentUser.value) } }

    fun addNote(
        title: String,
        content: String,
        colorKey: String = "yellow",
        category: String = "General",
        isPinned: Boolean = false,
        isPrivate: Boolean = true,
        checklist: List<NoteCheckItem> = emptyList()
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val note = Note(
                title = title,
                content = content,
                createdBy = _currentUser.value,
                createdDate = now,
                isPrivate = isPrivate,
                colorKey = colorKey,
                category = category,
                isPinned = isPinned,
                updatedDate = now,
                checklist = checklist
            )
            repository.insertNote(note, _currentUser.value)
            FamiUsageTracker.track(dataStoreManager.appContext, FamiUsageTracker.FEATURE_NOTE_CREATED)
        }
    }

    fun editNote(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note.copy(updatedDate = System.currentTimeMillis()), _currentUser.value)
        }
    }

    fun toggleNotePin(note: Note) {
        viewModelScope.launch {
            repository.insertNote(
                note.copy(isPinned = !note.isPinned, updatedDate = System.currentTimeMillis()),
                _currentUser.value
            )
        }
    }

    fun toggleNoteChecklistItem(note: Note, index: Int) {
        if (index !in note.checklist.indices) return
        val updatedList = note.checklist.mapIndexed { idx, item ->
            if (idx == index) item.copy(isDone = !item.isDone) else item
        }
        viewModelScope.launch {
            repository.insertNote(
                note.copy(checklist = updatedList, updatedDate = System.currentTimeMillis()),
                _currentUser.value
            )
        }
    }

    fun deleteNote(note: Note) { viewModelScope.launch { repository.deleteNote(note, _currentUser.value) } }

    fun submitSupportTicket(
        type: String,
        section: String,
        subject: String,
        description: String,
        appVersion: String,
        deviceInfo: String,
        onSuccess: (com.example.database.FamilyRepository.SupportTicketResult) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = repository.submitSupportTicket(
                    type = type,
                    section = section,
                    subject = subject,
                    description = description,
                    appVersion = appVersion,
                    deviceInfo = deviceInfo,
                    user = _currentUser.value.ifBlank { "Usuario Fami" }
                )
                val msg = if (result.webhookDelivered) {
                    "¡Gracias! Reporte #${result.ticketId} enviado a Soporte Fami."
                } else {
                    "Reporte #${result.ticketId} guardado. Puedes enviarlo por correo abajo."
                }
                notifyUser(msg)
                onSuccess(result)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "No se pudo enviar el reporte en este momento.")
            }
        }
    }
    fun deleteUserAccount(
        confirmPassword: String = "",
        googleIdToken: String = "",
        onSuccess: () -> Unit,
        onRequiresPassword: (String) -> Unit = {},
        onRequiresGoogleReauth: ((String) -> Unit)? = null,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val savedEmail = dataStoreManager.savedEmailFlow.firstOrNull().orEmpty()
                val fid = _familyId.value ?: dataStoreManager.familyIdFlow.firstOrNull()
                val userAlias = _currentUser.value.ifBlank { dataStoreManager.userNameFlow.firstOrNull().orEmpty() }
                val user = auth.currentUser

                if (user == null) {
                    dataStoreManager.clearAllDataForAccountDeletion()
                    repository.setFamilyId(null)
                    _isAuthenticated.value = false
                    _pendingEmailVerification.value = false
                    _needsUnlockMethodChoice.value = false
                    _familyId.value = null
                    _currentUser.value = ""
                    _authError.value = null
                    onSuccess()
                    return@launch
                }

                val targetEmail = user.email?.takeIf { it.isNotBlank() } ?: savedEmail
                val hasPasswordProvider = user.providerData.any { it.providerId == "password" }
                val hasGoogleProvider = user.providerData.any { it.providerId == "google.com" }

                // 1. Re-autenticar ANTES de borrar datos en RTDB para asegurar que user.delete() no falle después
                when {
                    confirmPassword.isNotBlank() && targetEmail.isNotBlank() -> {
                        val cred = com.google.firebase.auth.EmailAuthProvider.getCredential(targetEmail, confirmPassword)
                        user.reauthenticate(cred).await()
                    }
                    googleIdToken.isNotBlank() -> {
                        val cred = com.google.firebase.auth.GoogleAuthProvider.getCredential(googleIdToken, null)
                        user.reauthenticate(cred).await()
                    }
                    else -> {
                        val lastSignInMs = user.metadata?.lastSignInTimestamp ?: 0L
                        val isRecentSignIn = lastSignInMs > 0L && (System.currentTimeMillis() - lastSignInMs) < 3 * 60_000L
                        if (!isRecentSignIn) {
                            if (hasGoogleProvider && !hasPasswordProvider && onRequiresGoogleReauth != null) {
                                onRequiresGoogleReauth(targetEmail)
                                return@launch
                            } else if (hasPasswordProvider || targetEmail.isNotBlank()) {
                                onRequiresPassword(targetEmail)
                                return@launch
                            }
                        }
                    }
                }

                val uid = user.uid

                // 2. Eliminar datos en Realtime Database MIENTRAS el token auth del usuario sigue siendo válido
                val dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().reference
                if (!fid.isNullOrBlank()) {
                    val membersSnap = runCatching {
                        dbRef.child("families").child(fid).child("members").get().await()
                    }.getOrNull()
                    val otherMembersExist = membersSnap?.children?.any {
                        it.key != uid && it.getValue(Boolean::class.java) == true
                    } == true

                    if (!otherMembersExist) {
                        // Borrar el nodo completo /families/{fid} ANTES de eliminar members/{uid},
                        // ya que las reglas RTDB exigen members/{uid} == true para escribir en /families/{fid}
                        runCatching { dbRef.child("families").child(fid).removeValue().await() }
                    } else {
                        runCatching { dbRef.child("families").child(fid).child("fcm_tokens").child(uid).removeValue().await() }
                        val safeAliasKey = userAlias.trim().replace(Regex("[.#$\\[\\]/]"), "_")
                        if (safeAliasKey.isNotBlank()) {
                            runCatching { dbRef.child("families").child(fid).child("user_profiles").child(safeAliasKey).removeValue().await() }
                        }
                        runCatching { dbRef.child("families").child(fid).child("members").child(uid).removeValue().await() }
                    }
                }
                runCatching { dbRef.child("users").child(uid).removeValue().await() }

                // 3. Eliminar la cuenta de Firebase Auth
                try {
                    user.delete().await()
                } catch (_: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                    if (hasGoogleProvider && !hasPasswordProvider && onRequiresGoogleReauth != null) {
                        onRequiresGoogleReauth(targetEmail)
                    } else {
                        onRequiresPassword(targetEmail)
                    }
                    return@launch
                }

                // 4. Cerrar sesión de Firebase y limpiar DataStore y estado en memoria
                runCatching { auth.signOut() }
                dataStoreManager.clearAllDataForAccountDeletion()
                repository.setFamilyId(null)
                _isAuthenticated.value = false
                _pendingEmailVerification.value = false
                _needsUnlockMethodChoice.value = false
                _familyId.value = null
                _currentUser.value = ""
                _authError.value = null
                onSuccess()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                onError(mapFirebaseAuthError(e))
            }
        }
    }
}

class FamilyViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FamilyViewModel::class.java)) {
            val repository = FamilyRepository()
            val dataStore = DataStoreManager(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return FamilyViewModel(repository, dataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
