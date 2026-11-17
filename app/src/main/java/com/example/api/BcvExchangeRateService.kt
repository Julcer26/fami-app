package com.example.api

import android.util.Log
import com.example.currency.CountryCurrencyConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object BcvExchangeRateService {
    private const val TAG = "FamiExchangeRate"
    private const val CURRENT_RATE_TTL_MS = 15 * 60 * 1000L
    private const val HISTORIC_RATE_TTL_MS = 6 * 60 * 60 * 1000L

    private data class CachedRate(val value: Double, val timestampMs: Long)

    private val rateCache = ConcurrentHashMap<String, CachedRate>()
    private val historicRateCache = ConcurrentHashMap<String, CachedRate>()
    private val globalRatesCache = ConcurrentHashMap<String, Double>()

    @Volatile
    private var globalRatesTimestampMs: Long = 0L

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun getCachedRate(currencyCode: String): Double? {
        val norm = CountryCurrencyConfig.normalizeCurrencyCode(currencyCode)
        if (norm == "USD") return 1.0
        val now = System.currentTimeMillis()
        val entry = rateCache[norm]
        if (entry != null && entry.value > 0.0 && (now - entry.timestampMs) < CURRENT_RATE_TTL_MS) {
            return entry.value
        }
        if ((now - globalRatesTimestampMs) < CURRENT_RATE_TTL_MS) {
            return globalRatesCache[norm]?.takeIf { it > 0.0 }
        }
        return null
    }

    private fun getFreshCurrentRate(key: String): Double? {
        val entry = rateCache[key] ?: return null
        val age = System.currentTimeMillis() - entry.timestampMs
        return if (entry.value > 0.0 && age < CURRENT_RATE_TTL_MS) entry.value else null
    }

    private fun putCurrentRate(key: String, value: Double) {
        if (value > 0.0 && !value.isNaN() && !value.isInfinite()) {
            rateCache[key] = CachedRate(value, System.currentTimeMillis())
        }
    }

    /**
     * Detecta automáticamente el código de país (ISO-2) del usuario mediante su IP pública.
     * Si no hay red o falla, retorna null para usar el locale del dispositivo.
     */
    suspend fun detectCountryCodeByIp(): String? = withContext(Dispatchers.IO) {
        val ipEndpoints = listOf(
            "https://ipwho.is/",
            "https://ipapi.co/json/",
            "https://api.country.is/"
        )
        val fastClient = client.newBuilder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .build()

        for (url in ipEndpoints) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FamiApp/3.1")
                    .build()
                fastClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()?.trim().orEmpty()
                        if (body.isNotEmpty()) {
                            val json = JSONObject(body)
                            val code = json.optString("country_code")
                                .ifBlank { json.optString("country") }
                                .ifBlank { json.optString("countryCode") }
                                .trim()
                                .uppercase()
                            if (code.length == 2 && code.all { it.isLetter() }) {
                                return@withContext code
                            }
                        }
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.w(TAG, "GeoIP lookup failed on $url: ${e.message}")
            }
        }
        return@withContext null
    }

    /**
     * Obtiene la tasa de cambio oficial según el país seleccionado:
     * - VE (Venezuela): BCV Oficial
     * - CO (Colombia): TRM Oficial
     * - AR (Argentina): BNA Oficial
     * - MX (México), CL (Chile), PE (Perú), ES (Euro), etc: Tasa oficial de mercado interbancario
     * Retorna 0.0 si no se pudo obtener ninguna tasa válida (evita retornar 1.0 silenciosamente).
     */
    suspend fun fetchOfficialRateForCountry(
        countryCode: String = "VE",
        localCurrency: String = "VES"
    ): Double = withContext(Dispatchers.IO) {
        val curr = CountryCurrencyConfig.normalizeCurrencyCode(localCurrency)
        if (curr == "USD") return@withContext 1.0
        if (curr == "NONE") return@withContext 0.0
        val code = countryCode.trim().uppercase()
        when {
            code == "VE" && curr == "VES" -> fetchOfficialRate()
            code == "CO" && curr == "COP" -> fetchColombiaRate()
            code == "AR" && curr == "ARS" -> fetchArgentinaRate()
            curr == "VES" -> fetchOfficialRate()
            else -> fetchGlobalInterbankRate(curr)
        }
    }

    suspend fun fetchUsdToCurrencyRate(currencyCode: String): Double = withContext(Dispatchers.IO) {
        val rawUpper = currencyCode.trim().uppercase()
        if (rawUpper == "NONE" || rawUpper.isBlank()) return@withContext 0.0
        val curr = CountryCurrencyConfig.normalizeCurrencyCode(rawUpper)
        if (curr == "USD") return@withContext 1.0
        when (curr) {
            "VES" -> fetchOfficialRate()
            "COP" -> fetchColombiaRate()
            "ARS" -> fetchArgentinaRate()
            else -> fetchGlobalInterbankRate(curr)
        }
    }

    private fun fetchColombiaRate(): Double {
        getFreshCurrentRate("COP")?.let { return it }
        val urls = listOf(
            "https://co.dolarapi.com/v1/dolares/oficial",
            "https://open.er-api.com/v6/latest/USD"
        )
        for (url in urls) {
            try {
                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()?.trim().orEmpty()
                        if (body.isNotEmpty()) {
                            val json = JSONObject(body)
                            val rate = if (url.contains("dolarapi")) {
                                json.optDouble("promedio", json.optDouble("valor", 0.0))
                            } else {
                                json.optJSONObject("rates")?.optDouble("COP", 0.0) ?: 0.0
                            }
                            if (rate > 0.0) {
                                putCurrentRate("COP", rate)
                                return rate
                            }
                        }
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching CO rate from $url: ${e.message}")
            }
        }
        return 0.0
    }

    private fun fetchArgentinaRate(): Double {
        getFreshCurrentRate("ARS")?.let { return it }
        val urls = listOf(
            "https://dolarapi.com/v1/dolares/oficial",
            "https://open.er-api.com/v6/latest/USD"
        )
        for (url in urls) {
            try {
                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()?.trim().orEmpty()
                        if (body.isNotEmpty()) {
                            val json = JSONObject(body)
                            val rate = if (url.contains("dolarapi")) {
                                json.optDouble("promedio", json.optDouble("venta", 0.0))
                            } else {
                                json.optJSONObject("rates")?.optDouble("ARS", 0.0) ?: 0.0
                            }
                            if (rate > 0.0) {
                                putCurrentRate("ARS", rate)
                                return rate
                            }
                        }
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching AR rate from $url: ${e.message}")
            }
        }
        return 0.0
    }

    private fun fetchGlobalInterbankRate(currencyCode: String): Double {
        val norm = CountryCurrencyConfig.normalizeCurrencyCode(currencyCode)
        if (norm == "USD") return 1.0
        if (norm == "NONE") return 0.0

        getFreshCurrentRate(norm)?.let { return it }
        val now = System.currentTimeMillis()
        if ((now - globalRatesTimestampMs) < CURRENT_RATE_TTL_MS) {
            globalRatesCache[norm]?.takeIf { it > 0.0 }?.let { return it }
        }

        try {
            val url = "https://open.er-api.com/v6/latest/USD"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()?.trim().orEmpty()
                    if (body.isNotEmpty()) {
                        val json = JSONObject(body)
                        val rates = json.optJSONObject("rates")
                        if (rates != null) {
                            val keys = rates.keys()
                            val fetchedAt = System.currentTimeMillis()
                            while (keys.hasNext()) {
                                val k = keys.next().uppercase()
                                val v = rates.optDouble(k, 0.0)
                                if (v > 0.0 && !v.isNaN() && !v.isInfinite()) {
                                    globalRatesCache[k] = v
                                }
                            }
                            globalRatesTimestampMs = fetchedAt
                            val targetRate = rates.optDouble(norm, 0.0)
                            if (targetRate > 0.0) {
                                putCurrentRate(norm, targetRate)
                                return targetRate
                            }
                        }
                    }
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching global rate for $norm: ${e.message}")
        }
        return 0.0
    }

    // Attempts to fetch the Venezuelan Bolívar official exchange rate (BCV rate)
    suspend fun fetchOfficialRate(): Double = withContext(Dispatchers.IO) {
        getFreshCurrentRate("VES")?.let { return@withContext it }

        val urls = listOf(
            "https://ve.dolarapi.com/v1/dolares/oficial",
            "https://pydolarvenezuela-api.vercel.app/api/v1/dollar?page=bcv",
            "https://ve.bcvapi.com/v1/exchange",
            "https://bcv-api.vercel.app/api/v1/dollar"
        )

        for (url in urls) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string()?.trim().orEmpty()
                        if (bodyStr.isNotEmpty()) {
                            val json = JSONObject(bodyStr)
                            val foundPrice = if (url.contains("dolarapi.com")) {
                                val compra = json.optDouble("compra", 0.0)
                                val venta = json.optDouble("venta", 0.0)
                                val price = json.optDouble("promedio", 0.0)
                                if (price > 0.0) price
                                else if (compra > 0.0 && venta > 0.0) (compra + venta) / 2.0
                                else if (compra > 0.0) compra
                                else venta
                            } else if (url.contains("pydolarvenezuela")) {
                                val monitors = json.optJSONObject("monitors")
                                val bcvObj = monitors?.optJSONObject("bcv") ?: monitors?.optJSONObject("bcv_oficial")
                                bcvObj?.optDouble("price", 0.0) ?: 0.0
                            } else {
                                json.optDouble("price", 0.0)
                            }
                            if (foundPrice > 0.0) {
                                putCurrentRate("VES", foundPrice)
                                return@withContext foundPrice
                            }
                        }
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching from $url: ${e.localizedMessage}")
            }
        }
        return@withContext 0.0
    }

    suspend fun fetchRateForDate(timestamp: Long): Double = withContext(Dispatchers.IO) {
        val caracasZone = java.util.TimeZone.getTimeZone("America/Caracas")
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            timeZone = caracasZone
        }
        val todayStr = sdf.format(java.util.Date())
        val targetDateStr = sdf.format(java.util.Date(timestamp))

        val ttl = if (targetDateStr == todayStr) CURRENT_RATE_TTL_MS else HISTORIC_RATE_TTL_MS
        val cachedHistoric = historicRateCache[targetDateStr]
        if (cachedHistoric != null && cachedHistoric.value > 0.0 &&
            (System.currentTimeMillis() - cachedHistoric.timestampMs) < ttl
        ) {
            return@withContext cachedHistoric.value
        }

        if (targetDateStr == todayStr) {
            val currentRate = fetchOfficialRate()
            if (currentRate > 0.0) {
                historicRateCache[targetDateStr] = CachedRate(currentRate, System.currentTimeMillis())
                return@withContext currentRate
            }
        }

        val url = "https://ve.dolarapi.com/v1/historicos/dolares/oficial"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()?.trim().orEmpty()
                    if (bodyStr.isNotEmpty()) {
                        val arr = org.json.JSONArray(bodyStr)
                        var closestRate = 0.0
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val fecha = obj.optString("fecha", "")
                            if (fecha.isNotEmpty() && fecha <= targetDateStr) {
                                val rate = obj.optDouble("promedio", 0.0)
                                if (rate > 0.0) closestRate = rate
                            }
                        }
                        if (closestRate > 0.0) {
                            historicRateCache[targetDateStr] = CachedRate(closestRate, System.currentTimeMillis())
                            return@withContext closestRate
                        }
                    }
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching historic rate for $targetDateStr: ${e.localizedMessage}")
        }
        return@withContext 0.0
    }
}
