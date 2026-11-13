package com.example.currency

import java.util.Currency
import java.util.Locale

data class CountryInfo(
    val code: String,
    val name: String,
    val flag: String,
    val localCurrency: String,
    val symbol: String,
    val rateProvider: String,
    val defaultSecondary: String = "USD"
)

data class CurrencyOption(
    val code: String,
    val name: String,
    val symbol: String
)

data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String
)

data class FamilyRoleOption(
    val id: String,
    val labelEs: String,
    val labelEn: String,
    val labelPt: String,
    val emoji: String
)

object CountryCurrencyConfig {

    // Países destacados con proveedor oficial dedicado
    private val FEATURED_COUNTRIES = listOf(
        CountryInfo("VE", "Venezuela", "🇻🇪", "VES", "Bs.", "BCV Oficial", "USD"),
        CountryInfo("CO", "Colombia", "🇨🇴", "COP", "$", "TRM Oficial", "USD"),
        CountryInfo("MX", "México", "🇲🇽", "MXN", "$", "Banxico FIX", "USD"),
        CountryInfo("AR", "Argentina", "🇦🇷", "ARS", "$", "BNA Oficial", "USD"),
        CountryInfo("CL", "Chile", "🇨🇱", "CLP", "$", "Banco Central", "USD"),
        CountryInfo("PE", "Perú", "🇵🇪", "PEN", "S/.", "SBS Oficial", "USD"),
        CountryInfo("EC", "Ecuador", "🇪🇨", "USD", "$", "Tasa Oficial", "EUR"),
        CountryInfo("PA", "Panamá", "🇵🇦", "USD", "$", "Tasa Oficial", "EUR"),
        CountryInfo("DO", "República Dominicana", "🇩🇴", "DOP", "RD$", "Banco Central RD", "USD"),
        CountryInfo("CR", "Costa Rica", "🇨🇷", "CRC", "₡", "BCCR Oficial", "USD"),
        CountryInfo("UY", "Uruguay", "🇺🇾", "UYU", "$U", "BCU Oficial", "USD"),
        CountryInfo("PY", "Paraguay", "🇵🇾", "PYG", "₲", "BCP Oficial", "USD"),
        CountryInfo("BO", "Bolivia", "🇧🇴", "BOB", "Bs", "BCB Oficial", "USD"),
        CountryInfo("GT", "Guatemala", "🇬🇹", "GTQ", "Q", "Banguat Oficial", "USD"),
        CountryInfo("HN", "Honduras", "🇭🇳", "HNL", "L", "BCH Oficial", "USD"),
        CountryInfo("NI", "Nicaragua", "🇳🇮", "NIO", "C$", "BCN Oficial", "USD"),
        CountryInfo("SV", "El Salvador", "🇸🇻", "USD", "$", "Tasa Oficial", "EUR"),
        CountryInfo("ES", "España", "🇪🇸", "EUR", "€", "BCE Oficial", "USD"),
        CountryInfo("US", "Estados Unidos", "🇺🇸", "USD", "$", "Reserva Federal", "EUR"),
        CountryInfo("BR", "Brasil", "🇧🇷", "BRL", "R$", "Banco Central BR", "USD"),
        CountryInfo("CA", "Canadá", "🇨🇦", "CAD", "C$", "Bank of Canada", "USD"),
        CountryInfo("GB", "Reino Unido", "🇬🇧", "GBP", "£", "Bank of England", "USD")
    )

    // Lista completa de países disponibles en Google Play (176+ territorios ISO 3166-1)
    private val GOOGLE_PLAY_ISO_CODES = listOf(
        "AF", "AL", "DZ", "AO", "AG", "AR", "AM", "AU", "AT", "AZ",
        "BS", "BH", "BD", "BB", "BY", "BE", "BZ", "BJ", "BT", "BO",
        "BA", "BW", "BR", "BN", "BG", "BF", "BI", "KH", "CM", "CA",
        "CV", "CF", "TD", "CL", "CN", "CO", "KM", "CG", "CD", "CR",
        "CI", "HR", "CY", "CZ", "DK", "DJ", "DM", "DO", "EC", "EG",
        "SV", "GQ", "ER", "EE", "SZ", "ET", "FJ", "FI", "FR", "GA",
        "GM", "GE", "DE", "GH", "GR", "GD", "GT", "GN", "GW", "GY",
        "HT", "HN", "HK", "HU", "IS", "IN", "ID", "IQ", "IE", "IL",
        "IT", "JM", "JP", "JO", "KZ", "KE", "KI", "KW", "KG", "LA",
        "LV", "LB", "LS", "LR", "LY", "LI", "LT", "LU", "MO", "MG",
        "MW", "MY", "MV", "ML", "MT", "MR", "MU", "MX", "FM", "MD",
        "MC", "MN", "ME", "MA", "MZ", "MM", "NA", "NP", "NL", "NZ",
        "NI", "NE", "NG", "MK", "NO", "OM", "PK", "PW", "PS", "PA",
        "PG", "PY", "PE", "PH", "PL", "PT", "PR", "QA", "RO", "RU",
        "RW", "KN", "LC", "VC", "WS", "SM", "ST", "SA", "SN", "RS",
        "SC", "SL", "SG", "SK", "SI", "SB", "SO", "ZA", "KR", "ES",
        "LK", "SR", "SE", "CH", "TW", "TJ", "TZ", "TH", "TL", "TG",
        "TO", "TT", "TN", "TR", "TM", "UG", "UA", "AE", "GB", "US",
        "UY", "UZ", "VU", "VE", "VN", "YE", "ZM", "ZW"
    )

    fun countryCodeToFlag(countryCode: String): String {
        val clean = countryCode.trim().uppercase()
        if (clean.length != 2 || !clean[0].isLetter() || !clean[1].isLetter()) return "🌐"
        val first = Character.codePointAt(clean, 0) - 0x41 + 0x1F1E6
        val second = Character.codePointAt(clean, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }

    val SUPPORTED_COUNTRIES: List<CountryInfo> by lazy {
        val featuredMap = FEATURED_COUNTRIES.associateBy { it.code }
        val spanishLocale = Locale("es")
        val allCodes = (FEATURED_COUNTRIES.map { it.code } + GOOGLE_PLAY_ISO_CODES).distinct()
        val generatedRest = allCodes
            .filter { !featuredMap.containsKey(it) }
            .mapNotNull { code ->
                try {
                    val loc = Locale("", code)
                    val displayName = loc.getDisplayCountry(spanishLocale).takeIf { it.isNotBlank() && it != code } ?: code
                    val currency = runCatching { Currency.getInstance(loc) }.getOrNull()
                    val currCode = currency?.currencyCode ?: "USD"
                    val symbol = currency?.getSymbol(Locale.US)?.takeIf { it.length <= 4 } ?: currCode
                    CountryInfo(
                        code = code,
                        name = displayName,
                        flag = countryCodeToFlag(code),
                        localCurrency = currCode,
                        symbol = symbol,
                        rateProvider = "Tasa Oficial",
                        defaultSecondary = "USD"
                    )
                } catch (_: Exception) {
                    null
                }
            }
            .sortedBy { it.name }

        FEATURED_COUNTRIES + generatedRest
    }

    val AVAILABLE_CURRENCIES: List<CurrencyOption> by lazy {
        val curated = listOf(
            CurrencyOption("USD", "Dólar Estadounidense", "$"),
            CurrencyOption("EUR", "Euro", "€"),
            CurrencyOption("VES", "Bolívar Venezolano", "Bs."),
            CurrencyOption("COP", "Peso Colombiano", "$"),
            CurrencyOption("MXN", "Peso Mexicano", "$"),
            CurrencyOption("ARS", "Peso Argentino", "$"),
            CurrencyOption("CLP", "Peso Chileno", "$"),
            CurrencyOption("PEN", "Sol Peruano", "S/."),
            CurrencyOption("BRL", "Real Brasileño", "R$"),
            CurrencyOption("GBP", "Libra Esterlina", "£"),
            CurrencyOption("DOP", "Peso Dominicano", "RD$"),
            CurrencyOption("CRC", "Colón Costarricense", "₡"),
            CurrencyOption("UYU", "Peso Uruguayo", "\$U"),
            CurrencyOption("PYG", "Guaraní Paraguayo", "₲"),
            CurrencyOption("BOB", "Boliviano", "Bs"),
            CurrencyOption("GTQ", "Quetzal Guatemalteco", "Q"),
            CurrencyOption("HNL", "Lempira Hondureño", "L"),
            CurrencyOption("NIO", "Córdoba Nicaragüense", "C$"),
            CurrencyOption("CAD", "Dólar Canadiense", "C$"),
            CurrencyOption("CHF", "Franco Suizo", "CHF"),
            CurrencyOption("JPY", "Yen Japonés", "¥")
        )
        val existing = curated.map { it.code }.toMutableSet()
        val extra = SUPPORTED_COUNTRIES
            .filter { existing.add(it.localCurrency) }
            .map { CurrencyOption(it.localCurrency, "${it.localCurrency} (${it.name})", it.symbol) }
            .sortedBy { it.code }
        curated + extra
    }

    val SUPPORTED_LANGUAGES = listOf(
        LanguageOption("es", "Español", "Español", "🇪🇸"),
        LanguageOption("en", "Inglés", "English", "🇺🇸"),
        LanguageOption("pt", "Portugués", "Português", "🇧🇷"),
        LanguageOption("fr", "Francés", "Français", "🇫🇷"),
        LanguageOption("it", "Italiano", "Italiano", "🇮🇹"),
        LanguageOption("de", "Alemán", "Deutsch", "🇩🇪")
    )

    val FAMILY_ROLES = listOf(
        FamilyRoleOption("mama", "Mamá", "Mom", "Mãe", "👩"),
        FamilyRoleOption("papa", "Papá", "Dad", "Pai", "👨"),
        FamilyRoleOption("pareja", "Esposo/a / Pareja", "Spouse / Partner", "Cônjuge / Parceiro(a)", "💑"),
        FamilyRoleOption("hijo", "Hijo / Hija", "Son / Daughter", "Filho / Filha", "🧑"),
        FamilyRoleOption("abuelo", "Abuelo / Abuela", "Grandparent", "Avô / Avó", "👵"),
        FamilyRoleOption("admin", "Admin del Hogar", "Home Admin", "Admin da Casa", "🏠")
    )

    fun detectSystemLanguageCode(): String {
        val sysLang = Locale.getDefault().language.lowercase()
        return SUPPORTED_LANGUAGES.find { it.code == sysLang }?.code ?: "es"
    }

    fun getLanguageOption(code: String): LanguageOption {
        return SUPPORTED_LANGUAGES.find { it.code.equals(code, ignoreCase = true) }
            ?: SUPPORTED_LANGUAGES.first()
    }

    fun getCurrencySymbol(currencyCode: String): String {
        if (currencyCode == "NONE" || currencyCode.isBlank()) return ""
        return AVAILABLE_CURRENCIES.find { it.code.equals(currencyCode, ignoreCase = true) }?.symbol
            ?: runCatching { Currency.getInstance(currencyCode.uppercase()).getSymbol(Locale.US) }.getOrNull()
            ?: currencyCode
    }

    fun getCountryByCode(code: String): CountryInfo {
        return SUPPORTED_COUNTRIES.find { it.code.equals(code, ignoreCase = true) }
            ?: FEATURED_COUNTRIES.first()
    }

    fun convert(amount: Double, fromCurrency: String, toCurrency: String, rate: Double): Double {
        if (fromCurrency.equals(toCurrency, ignoreCase = true) || rate <= 0.0) return amount
        return if (fromCurrency == "USD" || fromCurrency == "EUR") {
            amount * rate
        } else {
            amount / rate
        }
    }
}
