package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
        _startInExistingLogin.value = true
        viewModelScope.launch {
            dataStoreManager.setOnboardingCompleted(true)
        }
    }

    fun consumeStartInExistingLogin() {
        _startInExistingLogin.value = false
    }

    val isAdminRole: StateFlow<Boolean> = familyRole
        .map { role -> isPrivilegedRole(role) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun isPrivilegedRole(role: String): Boolean {
        val clean = role.trim().lowercase()
        return clean in setOf(
            "admin", "administrador", "administrator",
            "mamá", "mama", "mom", "mãe", "mae",
            "papá", "papa", "dad", "pai"
        )
    }

    fun canEditBudgets(): Boolean = isPrivilegedRole(familyRole.value)

    fun canManageMembers(): Boolean = isPrivilegedRole(familyRole.value)

    fun canDeleteTransaction(tx: Transaction): Boolean {
        return isPrivilegedRole(familyRole.value) || tx.registeredBy.equals(_currentUser.value, ignoreCase = true)
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
        initialWallets: List<Wallet> = emptyList()
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
                initialWallets = initialWallets
            )
            syncExchangeRateFor(country.code, primaryCurr, secondaryCurr)
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
            val cleanCurr = currency.trim().uppercase().ifBlank { "USD" }
            dataStoreManager.setMonthlyIncome(amount, cleanCurr)
            val user = _currentUser.value.ifBlank { "Usuario" }
            val rate = if (cleanCurr == "USD") 1.0 else {
                val live = liveExchangeRate.value
                if (live > 0.0 && cleanCurr.equals(primaryCurrency.value, ignoreCase = true)) live
                else com.example.api.BcvExchangeRateService.fetchUsdToCurrencyRate(cleanCurr).takeIf { it > 0.0 } ?: 1.0
            }
            val amountUsd = if (cleanCurr == "USD") amount else amount / rate
            repository.insertSetting("monthly_income_amount", String.format(java.util.Locale.US, "%.2f", amount), user)
            repository.insertSetting("monthly_income_currency", cleanCurr, user)
            repository.insertSetting("total_limit_usd", String.format(java.util.Locale.US, "%.2f", amountUsd), user)
            notifyUser("Ingreso mensual actualizado: ${com.example.currency.CountryCurrencyConfig.getCurrencySymbol(cleanCurr)} ${String.format(java.util.Locale.US, "%,.2f", amount)} $cleanCurr")
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
        val target = targetCurrency.trim().uppercase().ifBlank { "USD" }
        val pCurr = primaryCurr.trim().uppercase()
        val sCurr = secondaryCurr.trim().uppercase()

        fun ratePerUsd(code: String): Double {
            val c = code.trim().uppercase()
            return when {
                c == "USD" -> 1.0
                c == pCurr && primaryRateToUsd > 0.0 -> primaryRateToUsd
                c == sCurr && secondaryRateToUsd > 0.0 -> secondaryRateToUsd
                (extraRatesToUsd[c] ?: 0.0) > 0.0 -> extraRatesToUsd[c]!!
                else -> 1.0
            }
        }

        val targetPerUsd = ratePerUsd(target)
        return walletList.sumOf { wallet ->
            val wCurr = wallet.currency.trim().uppercase().ifBlank { "USD" }
            if (wCurr == target) {
                wallet.balance
            } else {
                val usdEquiv = wallet.balance / ratePerUsd(wCurr)
                if (target == "USD") usdEquiv else usdEquiv * targetPerUsd
            }
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
            repository.adjustWalletBalance(walletId, delta, _currentUser.value)
            notifyUser("Saldo de billetera actualizado")
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
                notifyUser("Transferencia completada: ${fromWallet.name} → ${toWallet.name}")
            } catch (e: Exception) {
                notifyUser("Error en transferencia: ${e.localizedMessage}")
            }
        }
    }

    fun reopenInitialConfig() {
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
        }
    }

    val activityLogs: StateFlow<List<ActivityLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<Goal>> = combine(repository.allGoals, _currentUser) { list, user ->
        list.filter { !it.isPrivate || it.createdBy == user }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<Note>> = combine(repository.allNotes, _currentUser) { list, user ->
        list.filter { it.createdBy == user } // Notas totalmente privadas
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
    fun triggerConfetti() { _confettiTrigger.value += 1 }

    init {
        viewModelScope.launch {
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (firebaseUser == null) {
                dataStoreManager.clearAuthData()
                _familyId.value = null
                _currentUser.value = ""
            } else {
                val fid = dataStoreManager.familyIdFlow.firstOrNull()
                val name = dataStoreManager.userNameFlow.firstOrNull()
                if (fid != null && name != null) {
                    _familyId.value = fid
                    _currentUser.value = name
                    repository.setFamilyId(fid)
                    val isEmailProvider = firebaseUser.providerData.any { it.providerId == "password" }
                    if (isEmailProvider && !firebaseUser.isEmailVerified) {
                        _pendingVerificationEmail.value = firebaseUser.email ?: ""
                        _pendingEmailVerification.value = true
                    }
                } else {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    dataStoreManager.clearAuthData()
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
            val savedPin = dataStoreManager.userPinFlow.firstOrNull()
            if (!savedPin.isNullOrEmpty() && savedPin == inputPin) {
                if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
                    _isAuthenticated.value = true
                    onResult(true)
                    checkDefaultsAndSync()
                } else {
                    _authError.value = "Sesión de Firebase expirada. Inicia sesión nuevamente."
                    notifyUser("Sesión expirada. Inicia sesión de nuevo.")
                    dataStoreManager.clearAuthData()
                    _familyId.value = null
                    onResult(false)
                }
            } else {
                _authError.value = "PIN incorrecto. Verifica los 5 dígitos."
                notifyUser("PIN incorrecto")
                onResult(false)
            }
        }
    }

    fun loginBiometric() {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
            _authError.value = null
            _isAuthenticated.value = true
            checkDefaultsAndSync()
        } else {
            _authError.value = "Sesión expirada. Inicia sesión de nuevo."
            notifyUser("Sesión expirada. Inicie sesión de nuevo.")
            viewModelScope.launch {
                dataStoreManager.clearAuthData()
                _familyId.value = null
            }
        }
    }

    fun setUnlockMethodAndEnter(method: String, customPin: String? = null) {
        viewModelScope.launch {
            if (!customPin.isNullOrBlank() && customPin.length == 5) {
                dataStoreManager.updatePin(customPin)
            }
            dataStoreManager.setUnlockMethod(method)
            _needsUnlockMethodChoice.value = false
            _isAuthenticated.value = true
            checkDefaultsAndSync()
            val label = if (method == "biometric") "Huella Digital" else "PIN de 5 dígitos"
            notifyUser("Acceso configurado con $label ✅")
        }
    }

    fun updateUnlockPreference(method: String) {
        viewModelScope.launch {
            dataStoreManager.setUnlockMethod(method)
            val label = if (method == "biometric") "Huella Digital" else "PIN de 5 dígitos"
            notifyUser("Método de desbloqueo actualizado: $label")
        }
    }

    fun updateUserPin(newPin: String) {
        if (newPin.length != 5 || !newPin.all { it.isDigit() }) {
            notifyUser("El PIN debe tener exactamente 5 dígitos.")
            return
        }
        viewModelScope.launch {
            dataStoreManager.updatePin(newPin)
            notifyUser("PIN de 5 dígitos actualizado ✅")
        }
    }

    fun createFamily(email: String, pin: String, password: String, customFamilyName: String = "", customUserName: String = "") {
        viewModelScope.launch {
            val cleanEmail = email.trim()
            if (!isValidEmailFormat(cleanEmail)) {
                _authError.value = "Ingresa un correo electrónico real y válido."
                return@launch
            }
            _isAuthLoading.value = true
            _authError.value = null
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val authResult = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                val firebaseUser = authResult.user ?: throw Exception("No se pudo crear el usuario")
                val uid = firebaseUser.uid

                val newFamilyId = UUID.randomUUID().toString().substring(0, 8).uppercase()
                val userName = customUserName.trim().ifBlank { cleanEmail.substringBefore("@") }

                repository.registerUserMapping(uid, newFamilyId, userName)
                repository.registerFamilyMember(newFamilyId, uid)

                dataStoreManager.saveAuthData(newFamilyId, userName, pin, email = cleanEmail, authProvider = "email")
                _familyId.value = newFamilyId
                repository.setFamilyId(newFamilyId)

                repository.populateDefaultsIfNeeded()
                syncPendingNotebookSetup(userName = userName, colorHex = "#D0BCFF", familyNameOverride = customFamilyName)

                _currentUser.value = userName

                // Enviar verificación real al correo electrónico
                runCatching { firebaseUser.sendEmailVerification().await() }
                _pendingVerificationEmail.value = cleanEmail
                _pendingEmailVerification.value = true
                notifyUser("Te enviamos un correo de verificación a $cleanEmail")
            } catch (e: Exception) {
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
            val code = familyCode.trim().uppercase()
            val cleanEmail = email.trim()
            if (!isValidEmailFormat(cleanEmail)) {
                _authError.value = "Ingresa un correo electrónico real y válido."
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
                    auth.signInWithEmailAndPassword(cleanEmail, password).await()
                }
                val firebaseUser = authResult.user ?: throw Exception("No se pudo obtener el usuario")
                val uid = firebaseUser.uid

                repository.registerFamilyMember(code, uid)
                repository.registerUserMapping(uid, code, name.trim())

                dataStoreManager.saveAuthData(code, name.trim(), pin, email = cleanEmail, authProvider = "email")
                _familyId.value = code
                repository.setFamilyId(code)

                syncPendingNotebookSetup(userName = name.trim(), colorHex = "#EFB8C8")
                _currentUser.value = name.trim()

                if (isNewAccount && !firebaseUser.isEmailVerified) {
                    runCatching { firebaseUser.sendEmailVerification().await() }
                    _pendingVerificationEmail.value = cleanEmail
                    _pendingEmailVerification.value = true
                    notifyUser("Verifica tu correo ($cleanEmail) para completar el acceso")
                } else {
                    _needsUnlockMethodChoice.value = true
                }
            } catch (e: Exception) {
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

    fun handleGoogleFirebaseUser(
        firebaseUser: com.google.firebase.auth.FirebaseUser,
        joinFamilyCode: String = "",
        customFamilyName: String = "",
        pin: String = "00000"
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            try {
                val uid = firebaseUser.uid
                val email = firebaseUser.email.orEmpty()
                val displayName = firebaseUser.displayName?.trim()?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore("@").ifBlank { "Usuario Fami" }

                val snapshot = repository.getUserMapping(uid)
                val existingFid = snapshot.child("familyId").getValue(String::class.java)
                val existingName = snapshot.child("userName").getValue(String::class.java)

                val finalFid: String
                val finalUserName: String
                val isNewSetup: Boolean

                if (existingFid != null && existingName != null) {
                    finalFid = existingFid
                    finalUserName = existingName
                    isNewSetup = false
                } else if (joinFamilyCode.trim().isNotBlank()) {
                    val code = joinFamilyCode.trim().uppercase()
                    finalFid = code
                    finalUserName = displayName
                    repository.registerFamilyMember(code, uid)
                    repository.registerUserMapping(uid, code, finalUserName)
                    _familyId.value = code
                    repository.setFamilyId(code)
                    syncPendingNotebookSetup(userName = finalUserName, colorHex = "#EFB8C8")
                    isNewSetup = true
                } else {
                    finalFid = UUID.randomUUID().toString().substring(0, 8).uppercase()
                    finalUserName = displayName
                    repository.registerUserMapping(uid, finalFid, finalUserName)
                    repository.registerFamilyMember(finalFid, uid)
                    _familyId.value = finalFid
                    repository.setFamilyId(finalFid)
                    repository.populateDefaultsIfNeeded()
                    syncPendingNotebookSetup(
                        userName = finalUserName,
                        colorHex = "#D0BCFF",
                        familyNameOverride = customFamilyName.ifBlank { pendingFamilyName.value }
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
                _familyId.value = finalFid
                repository.setFamilyId(finalFid)
                _currentUser.value = finalUserName

                val savedMethod = dataStoreManager.unlockMethodFlow.firstOrNull() ?: "unset"
                if (isNewSetup || savedMethod == "unset") {
                    _needsUnlockMethodChoice.value = true
                } else {
                    _isAuthenticated.value = true
                    checkDefaultsAndSync()
                    notifyUser("¡Bienvenido, $finalUserName!")
                }
            } catch (e: Exception) {
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
        familyNameOverride: String = ""
    ) {
        val avatarId = dataStoreManager.userAvatarIdFlow.firstOrNull() ?: "woman_1"
        val role = dataStoreManager.familyRoleFlow.firstOrNull() ?: "Administrador"
        val mode = dataStoreManager.managementModeFlow.firstOrNull() ?: "familiar"
        val famName = familyNameOverride.trim().ifBlank { dataStoreManager.pendingFamilyNameFlow.firstOrNull()?.trim().orEmpty() }
        val monthlyInc = dataStoreManager.monthlyIncomeAmountFlow.firstOrNull() ?: 0.0
        val monthlyIncCurr = dataStoreManager.monthlyIncomeCurrencyFlow.firstOrNull() ?: "USD"
        val liveRate = dataStoreManager.liveExchangeRateFlow.firstOrNull() ?: 36.85
        val walletsJson = dataStoreManager.pendingWalletsJsonFlow.firstOrNull() ?: "[]"

        repository.insertProfile(
            UserProfile(name = userName, colorHex = colorHex, avatarId = avatarId, role = role),
            userName
        )
        if (famName.isNotBlank()) {
            repository.insertSetting("family_name", famName, userName)
        }
        repository.insertSetting("management_mode", mode, userName)

        if (monthlyInc > 0.0) {
            val amountUsd = when {
                monthlyIncCurr.equals("USD", ignoreCase = true) -> monthlyInc
                monthlyIncCurr.equals(primaryCurrency.value, ignoreCase = true) && liveRate > 0.0 -> monthlyInc / liveRate
                else -> {
                    val fetched = com.example.api.BcvExchangeRateService.fetchUsdToCurrencyRate(monthlyIncCurr).takeIf { it > 0.0 } ?: 1.0
                    monthlyInc / fetched
                }
            }
            repository.insertSetting("monthly_income_amount", String.format(java.util.Locale.US, "%.2f", monthlyInc), userName)
            repository.insertSetting("monthly_income_currency", monthlyIncCurr, userName)
            repository.insertSetting("total_limit_usd", String.format(java.util.Locale.US, "%.2f", amountUsd), userName)
        }

        val initialWallets = dataStoreManager.parseWalletsJson(walletsJson)
        if (initialWallets.isNotEmpty()) {
            initialWallets.forEach { w ->
                repository.insertWallet(w.copy(ownerName = userName), userName)
            }
            dataStoreManager.clearPendingWallets()
        }
    }

    fun loginExisting(email: String, password: String, pin: String) {
        viewModelScope.launch {
            val cleanEmail = email.trim()
            if (!isValidEmailFormat(cleanEmail)) {
                _authError.value = "Ingresa un correo electrónico válido."
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

                if (fid == null || userName == null) {
                    _authError.value = "Información de usuario no encontrada en el servidor."
                    auth.signOut()
                    return@launch
                }

                dataStoreManager.saveAuthData(fid, userName, pin, email = cleanEmail, authProvider = "email")
                _familyId.value = fid
                repository.setFamilyId(fid)
                _currentUser.value = userName

                val savedMethod = dataStoreManager.unlockMethodFlow.firstOrNull() ?: "unset"
                if (savedMethod == "unset") {
                    _needsUnlockMethodChoice.value = true
                } else {
                    _isAuthenticated.value = true
                    checkDefaultsAndSync()
                    notifyUser("¡Bienvenido de vuelta, $userName!")
                }
            } catch (e: Exception) {
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
            _isAuthenticated.value = false
            _pendingEmailVerification.value = false
            _needsUnlockMethodChoice.value = false
            _familyId.value = null
            _currentUser.value = ""
        }
    }

    private fun checkDefaultsAndSync() {
        viewModelScope.launch {
            try {
                syncExchangeRateFor(countryCode.value, primaryCurrency.value, secondaryCurrency.value)
            } catch (_: Exception) { }
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
        walletId: String = ""
    ) {
        viewModelScope.launch {
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
            val alertMessage = repository.insertTransaction(tx, user, isNewEntry = true)
            if (alertMessage != null) notifyUser(alertMessage)
            else notifyUser("¡Guardado!: $originalAmount $currency en '$description'")
        }
    }
    fun deleteTransaction(transaction: Transaction) { viewModelScope.launch { repository.deleteTransaction(transaction, _currentUser.value) } }
    fun editTransaction(transaction: Transaction) { viewModelScope.launch { repository.insertTransaction(transaction, _currentUser.value, isNewEntry = false) } }

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

    fun createProfile(name: String, colorHex: String) {
        viewModelScope.launch {
            val cleanName = name.trim()
            if (cleanName.isNotEmpty()) {
                repository.insertProfile(UserProfile(cleanName, colorHex), _currentUser.value)
            }
        }
    }

    fun editProfile(oldProfile: UserProfile, newName: String, newColorHex: String) {
        viewModelScope.launch {
            val cleanName = newName.trim()
            if (cleanName.isNotEmpty()) {
                if (oldProfile.name != cleanName) repository.deleteProfile(oldProfile, _currentUser.value)
                repository.insertProfile(UserProfile(cleanName, newColorHex), _currentUser.value)
                if (_currentUser.value == oldProfile.name) {
                    _currentUser.value = cleanName
                    dataStoreManager.saveAuthData(_familyId.value ?: "", cleanName, dataStoreManager.userPinFlow.firstOrNull() ?: "")
                }
            }
        }
    }
    fun deleteProfile(profile: UserProfile) { viewModelScope.launch { repository.deleteProfile(profile, _currentUser.value) } }

    fun syncLiveBcvRate() { viewModelScope.launch { syncExchangeRateFor(countryCode.value, primaryCurrency.value, secondaryCurrency.value) } }
    fun updateValueSetting(key: String, value: String) { viewModelScope.launch { repository.insertSetting(key, value, _currentUser.value) } }

    fun registerFromNaturalLanguage(inputText: String, walletId: String = "") {
        if (inputText.isBlank()) return
        viewModelScope.launch {
            _isRegisteringNlp.value = true
            _nlpError.value = null
            try {
                val user = _currentUser.value
                val customKey = settings.value.find { it.key == "gemini_api_key" }?.value
                val categoryNameList = categories.value.map { it.categoryName }.ifEmpty { listOf("Alimentos", "Servicios", "Otros") }
                val (transaction, error, successMsg) = GeminiParser.parseTextToTransaction(inputText, user, categoryNameList, customKey)
                if (error != null) {
                    _nlpError.value = error
                } else if (transaction != null) {
                    val resolvedWalletId = walletId.ifBlank {
                        wallets.value.firstOrNull { it.currency.equals(transaction.currency, ignoreCase = true) }?.id.orEmpty()
                    }
                    repository.insertTransaction(transaction.copy(walletId = resolvedWalletId), user, isNewEntry = true)
                    notifyUser(successMsg.ifBlank { "IA registró gasto/ingreso exitosamente." })
                }
            } catch (e: Exception) {
                _nlpError.value = "Error: ${e.message}"
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
        }
    }
    fun toggleChoreCompletion(chore: Chore) { if (!chore.isCompleted) triggerConfetti()
        viewModelScope.launch { repository.updateChoreStatus(chore.id, chore.title, !chore.isCompleted, _currentUser.value) } }
    fun deleteChore(chore: Chore) { viewModelScope.launch { repository.deleteChore(chore, _currentUser.value) } }
    fun editChore(chore: Chore) { viewModelScope.launch { repository.insertChore(chore, _currentUser.value) } }

    fun addShoppingItem(name: String, quantity: String, isPrivate: Boolean = false, listName: String = "General") {
        viewModelScope.launch {
            val item = ShoppingItem(name = name, quantity = quantity, addedBy = _currentUser.value, dateAdded = System.currentTimeMillis(), listName = listName)
            item.isPrivate = isPrivate
            repository.insertShoppingItem(item, _currentUser.value)
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

    fun addGoal(title: String, targetAmount: Double?, isPrivate: Boolean = false) {
        viewModelScope.launch {
            val user = _currentUser.value
            val goal = Goal(
                title = title,
                targetAmount = targetAmount,
                createdBy = user,
                createdDate = System.currentTimeMillis(),
                isPrivate = isPrivate
            )
            repository.insertGoal(goal, user)
        }
    }
    fun editGoal(goal: Goal) { if (goal.isCompleted) triggerConfetti()
        viewModelScope.launch { repository.insertGoal(goal, _currentUser.value) } }
    fun deleteGoal(goal: Goal) { viewModelScope.launch { repository.deleteGoal(goal, _currentUser.value) } }

    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            val note = Note(title = title, content = content, createdBy = _currentUser.value, createdDate = System.currentTimeMillis())
            repository.insertNote(note, _currentUser.value)
        }
    }
    fun editNote(note: Note) { viewModelScope.launch { repository.insertNote(note, _currentUser.value) } }
    fun deleteNote(note: Note) { viewModelScope.launch { repository.deleteNote(note, _currentUser.value) } }
    fun deleteUserAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val user = auth.currentUser
                val uid = user?.uid
                if (uid != null) {
                    try {
                        com.google.firebase.database.FirebaseDatabase.getInstance().reference
                            .child("users").child(uid).removeValue().await()
                    } catch (e: Exception) {
                        // Continuar si ya fue removido
                    }
                }
                user?.delete()?.await()
                dataStoreManager.clearAuthData()
                _isAuthenticated.value = false
                _familyId.value = null
                _currentUser.value = ""
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error al eliminar la cuenta. Se requiere reautenticación.")
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
