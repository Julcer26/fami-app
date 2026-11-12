package com.example.currency

data class CountryInfo(
    val code: String,
    val name: String,
    val flag: String,
    val localCurrency: String,
    val symbol: String,
    val rateProvider: String,
    val defaultSecondary: String = "USD"
)

object CountryCurrencyConfig {

    val SUPPORTED_COUNTRIES = listOf(
        CountryInfo(
            code = "VE",
            name = "Venezuela",
            flag = "🇻🇪",
            localCurrency = "VES",
            symbol = "Bs.",
            rateProvider = "BCV Oficial",
            defaultSecondary = "USD"
        ),
        CountryInfo(
            code = "CO",
            name = "Colombia",
            flag = "🇨🇴",
            localCurrency = "COP",
            symbol = "$",
            rateProvider = "TRM Oficial",
            defaultSecondary = "USD"
        ),
        CountryInfo(
            code = "MX",
            name = "México",
            flag = "🇲🇽",
            localCurrency = "MXN",
            symbol = "$",
            rateProvider = "Banxico FIX",
            defaultSecondary = "USD"
        ),
        CountryInfo(
            code = "AR",
            name = "Argentina",
            flag = "🇦🇷",
            localCurrency = "ARS",
            symbol = "$",
            rateProvider = "BNA Oficial",
            defaultSecondary = "USD"
        ),
        CountryInfo(
            code = "CL",
            name = "Chile",
            flag = "🇨🇱",
            localCurrency = "CLP",
            symbol = "$",
            rateProvider = "Banco Central",
            defaultSecondary = "USD"
        ),
        CountryInfo(
            code = "PE",
            name = "Perú",
            flag = "🇵🇪",
            localCurrency = "PEN",
            symbol = "S/.",
            rateProvider = "SBS Oficial",
            defaultSecondary = "USD"
        ),
        CountryInfo(
            code = "ES",
            name = "España (Eurozona)",
            flag = "🇪🇸",
            localCurrency = "EUR",
            symbol = "€",
            rateProvider = "BCE Oficial",
            defaultSecondary = "USD"
        ),
        CountryInfo(
            code = "US",
            name = "Estados Unidos",
            flag = "🇺🇸",
            localCurrency = "USD",
            symbol = "$",
            rateProvider = "Mercado Oficial",
            defaultSecondary = "EUR"
        ),
        CountryInfo(
            code = "GL",
            name = "Internacional / Global",
            flag = "🌐",
            localCurrency = "USD",
            symbol = "$",
            rateProvider = "Tasa Oficial Global",
            defaultSecondary = "EUR"
        )
    )

    fun getCountryByCode(code: String): CountryInfo {
        return SUPPORTED_COUNTRIES.find { it.code.equals(code, ignoreCase = true) }
            ?: SUPPORTED_COUNTRIES.first()
    }

    fun convert(amount: Double, fromCurrency: String, toCurrency: String, rate: Double): Double {
        if (fromCurrency.equals(toCurrency, ignoreCase = true) || rate <= 0.0) return amount
        // If converting from Local to Secondary (e.g. VES/COP to USD): amount / rate
        // If converting from Secondary to Local (e.g. USD to VES/COP): amount * rate
        return if (fromCurrency == "USD" || fromCurrency == "EUR") {
            amount * rate
        } else {
            amount / rate
        }
    }
}
