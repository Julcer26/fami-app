package com.example.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object BcvExchangeRateService {
    private const val TAG = "FamiExchangeRate"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Obtiene la tasa de cambio oficial según el país seleccionado:
     * - VE (Venezuela): BCV Oficial
     * - CO (Colombia): TRM Oficial
     * - AR (Argentina): BNA Oficial
     * - MX (México), CL (Chile), PE (Perú), ES (Euro), etc: Tasa oficial de mercado interbancario
     */
    suspend fun fetchOfficialRateForCountry(countryCode: String = "VE", localCurrency: String = "VES"): Double = withContext(Dispatchers.IO) {
        val code = countryCode.uppercase()
        when (code) {
            "VE" -> fetchOfficialRate()
            "CO" -> fetchColombiaRate()
            "AR" -> fetchArgentinaRate()
            else -> fetchGlobalInterbankRate(localCurrency)
        }
    }

    private fun fetchColombiaRate(): Double {
        val urls = listOf(
            "https://co.dolarapi.com/v1/dolares/oficial",
            "https://open.er-api.com/v6/latest/USD"
        )
        for (url in urls) {
            try {
                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val json = JSONObject(body)
                        if (url.contains("dolarapi")) {
                            val rate = json.optDouble("promedio", json.optDouble("valor", 0.0))
                            if (rate > 0.0) return rate
                        } else if (url.contains("open.er-api")) {
                            val rates = json.optJSONObject("rates")
                            val rate = rates?.optDouble("COP", 0.0) ?: 0.0
                            if (rate > 0.0) return rate
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching CO rate from $url: ${e.message}")
            }
        }
        return 4200.0 // Tasa representativa fallback
    }

    private fun fetchArgentinaRate(): Double {
        val urls = listOf(
            "https://dolarapi.com/v1/dolares/oficial",
            "https://open.er-api.com/v6/latest/USD"
        )
        for (url in urls) {
            try {
                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val json = JSONObject(body)
                        if (url.contains("dolarapi")) {
                            val rate = json.optDouble("promedio", json.optDouble("venta", 0.0))
                            if (rate > 0.0) return rate
                        } else if (url.contains("open.er-api")) {
                            val rates = json.optJSONObject("rates")
                            val rate = rates?.optDouble("ARS", 0.0) ?: 0.0
                            if (rate > 0.0) return rate
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching AR rate from $url: ${e.message}")
            }
        }
        return 1050.0 // Tasa representativa fallback
    }

    private fun fetchGlobalInterbankRate(currencyCode: String): Double {
        try {
            val url = "https://open.er-api.com/v6/latest/USD"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val json = JSONObject(body)
                    val rates = json.optJSONObject("rates")
                    val rate = rates?.optDouble(currencyCode.uppercase(), 0.0) ?: 0.0
                    if (rate > 0.0) return rate
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching global rate for $currencyCode: ${e.message}")
        }
        return 1.0
    }

    // Attempts to fetch the Venezuelan Bolívar official exchange rate (BCV rate)
    suspend fun fetchOfficialRate(): Double = withContext(Dispatchers.IO) {
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
                        val bodyStr = response.body?.string() ?: ""
                        if (bodyStr.isNotEmpty()) {
                            val json = JSONObject(bodyStr)
                            if (url.contains("dolarapi.com")) {
                                val compra = json.optDouble("compra", 0.0)
                                val venta = json.optDouble("venta", 0.0)
                                val price = json.optDouble("promedio", 0.0)
                                val finalPrice = if (price > 0.0) price else {
                                    if (compra > 0.0 && venta > 0.0) (compra + venta) / 2.0 
                                    else if (compra > 0.0) compra 
                                    else venta
                                }
                                if (finalPrice > 0.0) return@withContext finalPrice
                            } else if (url.contains("pydolarvenezuela")) {
                                val monitors = json.optJSONObject("monitors")
                                if (monitors != null) {
                                    val bcvObj = monitors.optJSONObject("bcv") ?: monitors.optJSONObject("bcv_oficial")
                                    if (bcvObj != null) {
                                        val price = bcvObj.optDouble("price", 0.0)
                                        if (price > 0.0) return@withContext price
                                    }
                                }
                            } else {
                                val price = json.optDouble("price", 0.0)
                                if (price > 0.0) return@withContext price
                            }
                        }
                    }
                }
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
        
        if (targetDateStr == todayStr) {
            val currentRate = fetchOfficialRate()
            if (currentRate > 0.0) return@withContext currentRate
        }
        
        val url = "https://ve.dolarapi.com/v1/historicos/dolares/oficial"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
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
                        if (closestRate > 0.0) return@withContext closestRate
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching historic rate for $targetDateStr: ${e.localizedMessage}")
        }
        return@withContext 0.0
    }
}
