package com.example.analytics

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Telemetría de producto first-party y anónima de Fami.
 * - Cero rastreadores publicitarios de terceros (0 ad SDKs).
 * - Cero PII, cero montos, cero descripciones ni correos.
 * - Solo incrementa contadores agregados por mes (YYYY-MM) en Firebase RTDB
 *   para medir qué funciones son más útiles y cuáles mejorar o simplificar.
 * - Se activa automáticamente tras instalar y aceptar Términos y Política de Privacidad.
 */
object FamiUsageTracker {

    private const val PREFS_NAME = "fami_usage_analytics_prefs"
    private const val KEY_ENABLED = "analytics_enabled"
    private const val KEY_LAST_DAILY_PREFIX = "daily_"

    const val FEATURE_APP_OPEN = "app_open"
    const val FEATURE_TAB_INICIO = "tab_inicio"
    const val FEATURE_TAB_CUENTAS = "tab_cuentas"
    const val FEATURE_TAB_TAREAS = "tab_tareas"
    const val FEATURE_TAB_COMPRAS = "tab_compras"
    const val FEATURE_TAB_METAS = "tab_metas"
    const val FEATURE_TX_MANUAL = "tx_manual"
    const val FEATURE_TX_VOICE_NLP = "tx_voice_nlp"
    const val FEATURE_TX_RECEIPT_SCAN = "tx_receipt_scan"
    const val FEATURE_WALLET_TRANSFER = "wallet_transfer"
    const val FEATURE_WIDGET_WRITE = "widget_write"
    const val FEATURE_WIDGET_VOICE = "widget_voice"
    const val FEATURE_WIDGET_SCAN = "widget_scan"
    const val FEATURE_WIDGET_GALLERY = "widget_gallery"
    const val FEATURE_CHORE_CREATED = "chore_created"
    const val FEATURE_SHOPPING_ADDED = "shopping_added"
    const val FEATURE_GOAL_CREATED = "goal_created"
    const val FEATURE_NOTE_CREATED = "note_created"
    const val FEATURE_RECURRING_PAID = "recurring_paid"
    const val FEATURE_AI_DIAGNOSIS = "ai_diagnosis"
    const val FEATURE_CSV_EXPORT = "csv_export"
    const val FEATURE_OTA_CHECK = "ota_check"
    const val FEATURE_DARK_MODE_ON = "theme_dark_enabled"
    const val FEATURE_MODE_INDIVIDUAL = "mode_individual_active"
    const val FEATURE_MODE_FAMILIAR = "mode_familiar_active"
    const val FEATURE_FAMILY_INVITE_SHARED = "family_invite_shared"
    const val FEATURE_FAMILY_JOINED_CODE = "family_joined_by_code"

    internal fun currentMonthKey(timestampMs: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("yyyy-MM", Locale.US).format(Date(timestampMs))
    }

    internal fun currentDayKey(timestampMs: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestampMs))
    }

    internal fun sanitizeFeatureKey(raw: String): String {
        return raw.trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9_]+"), "_")
            .trim('_')
            .take(48)
    }

    fun isEnabled(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    /**
     * Registra un incremento anónimo de uso para una función en el mes actual.
     * Nunca lanza excepciones ni bloquea la UI.
     */
    fun track(context: Context, featureKey: String) {
        if (!isEnabled(context)) return
        val cleanKey = sanitizeFeatureKey(featureKey)
        if (cleanKey.isBlank()) return
        runCatching {
            if (FirebaseAuth.getInstance().currentUser == null) return@runCatching
            val monthKey = currentMonthKey()
            FirebaseDatabase.getInstance().reference
                .child("analytics_monthly")
                .child(monthKey)
                .child("features")
                .child(cleanKey)
                .setValue(ServerValue.increment(1L))
        }
    }

    /**
     * Registra un evento a lo sumo una vez al día por dispositivo (ideal para medir
     * sesiones activas, modo Familiar vs Individual o adopción de Modo Oscuro sin inflar conteos).
     */
    fun trackOnceDaily(context: Context, featureKey: String) {
        if (!isEnabled(context)) return
        val cleanKey = sanitizeFeatureKey(featureKey)
        if (cleanKey.isBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = currentDayKey()
        val prefKey = "$KEY_LAST_DAILY_PREFIX$cleanKey"
        if (prefs.getString(prefKey, null) == today) return
        prefs.edit().putString(prefKey, today).apply()
        track(context, cleanKey)
    }
}
