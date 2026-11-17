package com.example.api

import android.util.Base64
import com.example.BuildConfig
import com.example.security.LocalCryptoBox
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Secure Gemini API Configuration & Cloud Function Proxy Resolver.
 *
 * Security Layered Architecture:
 * 1. Cloud Proxy First (Zero API Key on Device): If a Firebase Cloud Function proxy URL is configured
 *    (or resolved from `/secure_config/gemini_proxy_url`), requests are sent authenticated with the
 *    user's Firebase ID Token (`Authorization: Bearer <idToken>`) so the Gemini API key stays on the server.
 * 2. Authenticated RTDB Vault (`/secure_config/gemini_api_key`): Readable ONLY by authenticated
 *    Firebase users (`auth != null` in `database.rules.json`), cached in memory with TTL and never
 *    hardcoded in the APK source code.
 * 3. Custom User/Family Key & BuildConfig `.env` fallback: Supports personal user-configured keys
 *    (decrypted via `LocalCryptoBox`) or local developer `.env` `GEMINI_API_KEY` via `BuildConfig`.
 */
object GeminiConfig {
    private const val DEFAULT_CLOUD_PROXY_URL =
        "https://us-central1-hogar-finanzas-2026.cloudfunctions.net/geminiGenerate"

    private const val CACHE_TTL_MS = 15 * 60 * 1000L

    @Volatile
    private var cachedRemoteKey: String? = null

    @Volatile
    private var cachedProxyUrl: String? = null

    @Volatile
    private var lastRemoteFetchMs: Long = 0L

    private val proxyHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(28, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun decodeIfEncoded(raw: String): String {
        val trimmed = LocalCryptoBox.decrypt(raw.trim()).trim()
        if (trimmed.isBlank() || trimmed.equals("MY_GEMINI_API_KEY", ignoreCase = true) || trimmed.equals("null", ignoreCase = true)) {
            return ""
        }
        if (trimmed.startsWith("AIza") || trimmed.startsWith("AQ.")) {
            return trimmed
        }
        return try {
            val decoded = String(Base64.decode(trimmed, Base64.DEFAULT), Charsets.UTF_8).trim()
            if (decoded.startsWith("AIza") || decoded.startsWith("AQ.")) decoded else trimmed
        } catch (_: Exception) {
            trimmed
        }
    }

    private fun getBuildConfigKey(): String {
        return try {
            val field = BuildConfig::class.java.getDeclaredField("GEMINI_API_KEY")
            field.isAccessible = true
            val raw = (field.get(null) as? String).orEmpty()
            decodeIfEncoded(raw)
        } catch (_: Exception) {
            ""
        }
    }

    fun hasUsableCustomKey(customUserKey: String?): Boolean {
        if (customUserKey.isNullOrBlank()) return false
        return decodeIfEncoded(customUserKey).isNotBlank()
    }

    /**
     * Synchronously returns currently available local/cached keys without blocking.
     * Prefer [resolveApiKeysSuspend] inside suspend functions so authenticated remote config is loaded.
     */
    fun getApiKeys(customUserKey: String? = null): List<String> {
        val keys = LinkedHashSet<String>()
        if (!customUserKey.isNullOrBlank()) {
            val cleanCustom = decodeIfEncoded(customUserKey)
            if (cleanCustom.isNotBlank()) keys.add(cleanCustom)
        }
        cachedRemoteKey?.takeIf { it.isNotBlank() }?.let { keys.add(it) }
        getBuildConfigKey().takeIf { it.isNotBlank() }?.let { keys.add(it) }
        return keys.toList()
    }

    /**
     * Resolves API keys from:
     * 1) User/Family custom key (decrypted if encrypted)
     * 2) Authenticated Firebase RTDB `/secure_config/gemini_api_key` (or `/config/gemini_api_key`)
     * 3) Local developer `.env` `BuildConfig.GEMINI_API_KEY`
     */
    suspend fun resolveApiKeysSuspend(customUserKey: String? = null): List<String> {
        refreshRemoteConfigIfNeeded()
        return getApiKeys(customUserKey)
    }

    suspend fun refreshRemoteConfigIfNeeded(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && cachedRemoteKey != null && (now - lastRemoteFetchMs) < CACHE_TTL_MS) {
            return
        }
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        if (currentUser.uid.isBlank()) return

        val dbRef = FirebaseDatabase.getInstance().reference
        runCatching {
            val snap = dbRef.child("secure_config").get().await()
            val remoteKeyRaw = snap.child("gemini_api_key").getValue(String::class.java).orEmpty()
            val proxyUrlRaw = snap.child("gemini_proxy_url").getValue(String::class.java).orEmpty()
            val decodedKey = decodeIfEncoded(remoteKeyRaw)
            if (decodedKey.isNotBlank()) {
                cachedRemoteKey = decodedKey
            }
            if (proxyUrlRaw.startsWith("https://")) {
                cachedProxyUrl = proxyUrlRaw.trim()
            }
            lastRemoteFetchMs = now
        }

        if (cachedRemoteKey.isNullOrBlank()) {
            runCatching {
                val legacyKeyRaw = dbRef.child("config").child("gemini_api_key")
                    .get().await().getValue(String::class.java).orEmpty()
                val decodedLegacy = decodeIfEncoded(legacyKeyRaw)
                if (decodedLegacy.isNotBlank()) {
                    cachedRemoteKey = decodedLegacy
                    lastRemoteFetchMs = now
                }
            }
        }
    }

    /**
     * Calls the authenticated Firebase Cloud Function `geminiGenerate` with the user's Firebase ID token
     * so the Gemini API key never needs to leave the backend.
     * Returns null if the proxy is unreachable or not yet deployed, allowing seamless fallback.
     */
    suspend fun executeViaAuthenticatedCloudProxy(
        model: String,
        jsonPayload: JSONObject
    ): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        val idToken = runCatching { user.getIdToken(false).await()?.token }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: return null

        val proxyUrl = cachedProxyUrl?.takeIf { it.startsWith("https://") } ?: DEFAULT_CLOUD_PROXY_URL
        val wrapper = JSONObject().apply {
            put("model", model)
            put("payload", jsonPayload)
        }
        val body = wrapper.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(proxyUrl)
            .addHeader("Authorization", "Bearer $idToken")
            .post(body)
            .build()

        return try {
            proxyHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    val API_KEY: String
        get() = getApiKeys().firstOrNull() ?: ""
}
