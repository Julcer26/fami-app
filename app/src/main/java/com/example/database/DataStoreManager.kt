package com.example.database

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class DataStoreManager(private val context: Context) {
    
    companion object {
        val FAMILY_ID = stringPreferencesKey("family_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_PIN = stringPreferencesKey("user_pin")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")

        // Fami Multi-Country & Multi-Currency Preferences
        val COUNTRY_CODE = stringPreferencesKey("country_code")
        val PRIMARY_CURRENCY = stringPreferencesKey("primary_currency")
        val SECONDARY_CURRENCY = stringPreferencesKey("secondary_currency")
        val LIVE_EXCHANGE_RATE = doublePreferencesKey("live_exchange_rate")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val VIEW_IN_SECONDARY = booleanPreferencesKey("view_in_secondary")
    }

    val familyIdFlow: Flow<String?> = context.dataStore.data.map { it[FAMILY_ID] }
    val userNameFlow: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val userPinFlow: Flow<String?> = context.dataStore.data.map { it[USER_PIN] }
    val isDarkThemeFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_DARK_THEME] ?: true }

    val countryCodeFlow: Flow<String> = context.dataStore.data.map { it[COUNTRY_CODE] ?: "VE" }
    val primaryCurrencyFlow: Flow<String> = context.dataStore.data.map { it[PRIMARY_CURRENCY] ?: "VES" }
    val secondaryCurrencyFlow: Flow<String> = context.dataStore.data.map { it[SECONDARY_CURRENCY] ?: "USD" }
    val liveExchangeRateFlow: Flow<Double> = context.dataStore.data.map { it[LIVE_EXCHANGE_RATE] ?: 904.45 }
    val hasCompletedOnboardingFlow: Flow<Boolean> = context.dataStore.data.map { it[HAS_COMPLETED_ONBOARDING] ?: false }
    val viewInSecondaryFlow: Flow<Boolean> = context.dataStore.data.map { it[VIEW_IN_SECONDARY] ?: false }

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

    suspend fun setCountryAndCurrency(countryCode: String, primaryCurr: String, secondaryCurr: String) {
        context.dataStore.edit { prefs ->
            prefs[COUNTRY_CODE] = countryCode
            prefs[PRIMARY_CURRENCY] = primaryCurr
            prefs[SECONDARY_CURRENCY] = secondaryCurr
        }
    }

    suspend fun setLiveExchangeRate(rate: Double) {
        context.dataStore.edit { prefs ->
            prefs[LIVE_EXCHANGE_RATE] = rate
        }
    }

    suspend fun toggleViewInSecondary(showSecondary: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[VIEW_IN_SECONDARY] = showSecondary
        }
    }

    suspend fun saveAuthData(familyId: String, userName: String, pin: String) {
        context.dataStore.edit { prefs ->
            prefs[FAMILY_ID] = familyId
            prefs[USER_NAME] = userName
            prefs[USER_PIN] = pin
        }
    }

    suspend fun clearAuthData() {
        context.dataStore.edit { prefs ->
            prefs.remove(FAMILY_ID)
            prefs.remove(USER_NAME)
            prefs.remove(USER_PIN)
        }
    }
}
