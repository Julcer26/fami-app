package com.example.database

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.currency.CountryCurrencyConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        val FAMILY_ID = stringPreferencesKey("family_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_PIN = stringPreferencesKey("user_pin")
        val IS_DARK_THEME = booleanPreferencesKey("is_notebook_dark_mode")

        // Fami Multi-Country, Multi-Currency, Language, Family Mode, Avatar, Income & Wallets
        val COUNTRY_CODE = stringPreferencesKey("country_code")
        val PRIMARY_CURRENCY = stringPreferencesKey("primary_currency")
        val SECONDARY_CURRENCY = stringPreferencesKey("secondary_currency")
        val LIVE_EXCHANGE_RATE = doublePreferencesKey("live_exchange_rate")
        val SECONDARY_EXCHANGE_RATE = doublePreferencesKey("secondary_exchange_rate")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val FAMILY_ROLE = stringPreferencesKey("family_role")
        val MANAGEMENT_MODE = stringPreferencesKey("management_mode") // "familiar" | "individual"
        val FAMILY_ACTION = stringPreferencesKey("family_action")     // "create" | "join" | "individual"
        val PENDING_FAMILY_NAME = stringPreferencesKey("pending_family_name")
        val USER_AVATAR_ID = stringPreferencesKey("user_avatar_id")
        val MONTHLY_INCOME_AMOUNT = doublePreferencesKey("monthly_income_amount")
        val MONTHLY_INCOME_CURRENCY = stringPreferencesKey("monthly_income_currency")
        val PENDING_WALLETS_JSON = stringPreferencesKey("pending_wallets_json")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val HAS_COMPLETED_IN_APP_TOUR = booleanPreferencesKey("has_completed_in_app_tour")
        val VIEW_IN_SECONDARY = booleanPreferencesKey("view_in_secondary")
        val UNLOCK_METHOD = stringPreferencesKey("unlock_method") // "unset" | "biometric" | "pin"
        val SAVED_EMAIL = stringPreferencesKey("saved_email")
        val AUTH_PROVIDER = stringPreferencesKey("auth_provider") // "email" | "google"
    }

    val familyIdFlow: Flow<String?> = context.dataStore.data.map { it[FAMILY_ID] }
    val userNameFlow: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val userPinFlow: Flow<String?> = context.dataStore.data.map { it[USER_PIN] }
    val isDarkThemeFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_DARK_THEME] ?: false }

    val countryCodeFlow: Flow<String> = context.dataStore.data.map { it[COUNTRY_CODE] ?: "VE" }
    val primaryCurrencyFlow: Flow<String> = context.dataStore.data.map { it[PRIMARY_CURRENCY] ?: "VES" }
    val secondaryCurrencyFlow: Flow<String> = context.dataStore.data.map { it[SECONDARY_CURRENCY] ?: "USD" }
    val liveExchangeRateFlow: Flow<Double> = context.dataStore.data.map { it[LIVE_EXCHANGE_RATE] ?: 904.45 }
    val secondaryExchangeRateFlow: Flow<Double> = context.dataStore.data.map { it[SECONDARY_EXCHANGE_RATE] ?: 1.0 }
    val appLanguageFlow: Flow<String> = context.dataStore.data.map {
        it[APP_LANGUAGE] ?: CountryCurrencyConfig.detectSystemLanguageCode()
    }
    val familyRoleFlow: Flow<String> = context.dataStore.data.map { it[FAMILY_ROLE] ?: "Administrador" }
    val managementModeFlow: Flow<String> = context.dataStore.data.map { it[MANAGEMENT_MODE] ?: "familiar" }
    val familyActionFlow: Flow<String> = context.dataStore.data.map { it[FAMILY_ACTION] ?: "create" }
    val pendingFamilyNameFlow: Flow<String> = context.dataStore.data.map { it[PENDING_FAMILY_NAME] ?: "" }
    val userAvatarIdFlow: Flow<String> = context.dataStore.data.map { it[USER_AVATAR_ID] ?: "woman_1" }
    val monthlyIncomeAmountFlow: Flow<Double> = context.dataStore.data.map { it[MONTHLY_INCOME_AMOUNT] ?: 0.0 }
    val monthlyIncomeCurrencyFlow: Flow<String> = context.dataStore.data.map { it[MONTHLY_INCOME_CURRENCY] ?: "USD" }
    val pendingWalletsJsonFlow: Flow<String> = context.dataStore.data.map { it[PENDING_WALLETS_JSON] ?: "[]" }
    val hasCompletedOnboardingFlow: Flow<Boolean> = context.dataStore.data.map { it[HAS_COMPLETED_ONBOARDING] ?: false }
    val hasCompletedInAppTourFlow: Flow<Boolean> = context.dataStore.data.map { it[HAS_COMPLETED_IN_APP_TOUR] ?: false }
    val viewInSecondaryFlow: Flow<Boolean> = context.dataStore.data.map { it[VIEW_IN_SECONDARY] ?: false }
    val unlockMethodFlow: Flow<String> = context.dataStore.data.map { it[UNLOCK_METHOD] ?: "unset" }
    val savedEmailFlow: Flow<String> = context.dataStore.data.map { it[SAVED_EMAIL] ?: "" }
    val authProviderFlow: Flow<String> = context.dataStore.data.map { it[AUTH_PROVIDER] ?: "email" }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_DARK_THEME] = isDark
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    suspend fun setInAppTourCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAS_COMPLETED_IN_APP_TOUR] = completed
        }
    }

    suspend fun setCountryAndCurrency(countryCode: String, primaryCurr: String, secondaryCurr: String) {
        context.dataStore.edit { prefs ->
            prefs[COUNTRY_CODE] = countryCode
            prefs[PRIMARY_CURRENCY] = primaryCurr
            prefs[SECONDARY_CURRENCY] = secondaryCurr
            if (secondaryCurr == "NONE") {
                prefs[VIEW_IN_SECONDARY] = false
            }
        }
    }

    suspend fun setInitialConfiguration(
        countryCode: String,
        primaryCurr: String,
        secondaryCurr: String,
        languageCode: String,
        familyRole: String,
        managementMode: String = "familiar",
        familyAction: String = "create",
        familyName: String = "",
        avatarId: String = "woman_1",
        monthlyIncomeAmount: Double = 0.0,
        monthlyIncomeCurrency: String = primaryCurr,
        initialWallets: List<Wallet> = emptyList()
    ) {
        val walletsArr = JSONArray()
        initialWallets.forEach { w ->
            val obj = JSONObject()
            obj.put("id", w.id)
            obj.put("name", w.name)
            obj.put("currency", w.currency)
            obj.put("balance", w.balance)
            obj.put("isLocal", w.isLocal)
            obj.put("ownerName", w.ownerName)
            walletsArr.put(obj)
        }

        context.dataStore.edit { prefs ->
            prefs[COUNTRY_CODE] = countryCode
            prefs[PRIMARY_CURRENCY] = primaryCurr
            prefs[SECONDARY_CURRENCY] = secondaryCurr
            prefs[APP_LANGUAGE] = languageCode
            prefs[FAMILY_ROLE] = familyRole
            prefs[MANAGEMENT_MODE] = managementMode
            prefs[FAMILY_ACTION] = familyAction
            prefs[PENDING_FAMILY_NAME] = familyName
            prefs[USER_AVATAR_ID] = avatarId
            prefs[MONTHLY_INCOME_AMOUNT] = monthlyIncomeAmount
            prefs[MONTHLY_INCOME_CURRENCY] = monthlyIncomeCurrency
            prefs[PENDING_WALLETS_JSON] = walletsArr.toString()
            if (secondaryCurr == "NONE") {
                prefs[VIEW_IN_SECONDARY] = false
            }
            prefs[HAS_COMPLETED_ONBOARDING] = true
        }
    }

    suspend fun clearPendingWallets() {
        context.dataStore.edit { prefs ->
            prefs[PENDING_WALLETS_JSON] = "[]"
        }
    }

    suspend fun setAppLanguage(languageCode: String) {
        context.dataStore.edit { prefs ->
            prefs[APP_LANGUAGE] = languageCode
        }
    }

    suspend fun setFamilyRole(role: String) {
        context.dataStore.edit { prefs ->
            prefs[FAMILY_ROLE] = role
        }
    }

    suspend fun setUserAvatarId(avatarId: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_AVATAR_ID] = avatarId
        }
    }

    suspend fun setMonthlyIncome(amount: Double, currency: String) {
        context.dataStore.edit { prefs ->
            prefs[MONTHLY_INCOME_AMOUNT] = amount
            prefs[MONTHLY_INCOME_CURRENCY] = currency
        }
    }

    suspend fun setLiveExchangeRate(rate: Double) {
        context.dataStore.edit { prefs ->
            prefs[LIVE_EXCHANGE_RATE] = rate
        }
    }

    suspend fun setSecondaryExchangeRate(rate: Double) {
        context.dataStore.edit { prefs ->
            prefs[SECONDARY_EXCHANGE_RATE] = rate
        }
    }

    suspend fun toggleViewInSecondary(showSecondary: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[VIEW_IN_SECONDARY] = showSecondary
        }
    }

    suspend fun setUnlockMethod(method: String) {
        context.dataStore.edit { prefs ->
            prefs[UNLOCK_METHOD] = method
        }
    }

    suspend fun updatePin(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_PIN] = pin
        }
    }

    suspend fun saveAuthData(
        familyId: String,
        userName: String,
        pin: String,
        email: String = "",
        authProvider: String = "email"
    ) {
        context.dataStore.edit { prefs ->
            prefs[FAMILY_ID] = familyId
            prefs[USER_NAME] = userName
            prefs[USER_PIN] = pin
            if (email.isNotBlank()) prefs[SAVED_EMAIL] = email
            prefs[AUTH_PROVIDER] = authProvider
        }
    }

    suspend fun clearAuthData() {
        context.dataStore.edit { prefs ->
            prefs.remove(FAMILY_ID)
            prefs.remove(USER_NAME)
            prefs.remove(USER_PIN)
            prefs.remove(UNLOCK_METHOD)
            prefs.remove(SAVED_EMAIL)
            prefs.remove(AUTH_PROVIDER)
        }
    }

    fun parseWalletsJson(jsonStr: String): List<Wallet> {
        val result = mutableListOf<Wallet>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    Wallet(
                        id = obj.optString("id", ""),
                        name = obj.optString("name", ""),
                        currency = obj.optString("currency", "USD"),
                        balance = obj.optDouble("balance", 0.0),
                        isLocal = obj.optBoolean("isLocal", true),
                        ownerName = obj.optString("ownerName", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }
}
