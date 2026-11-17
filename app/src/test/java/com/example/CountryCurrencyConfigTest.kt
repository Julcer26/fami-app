package com.example

import com.example.currency.CountryCurrencyConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountryCurrencyConfigTest {

    @Test
    fun normalizeCurrencyCode_mapsBolivarAliasesAndDefaultsBlankToUsd() {
        assertEquals("VES", CountryCurrencyConfig.normalizeCurrencyCode("Bs."))
        assertEquals("VES", CountryCurrencyConfig.normalizeCurrencyCode(" bs.s "))
        assertEquals("VES", CountryCurrencyConfig.normalizeCurrencyCode("Bs.D"))
        assertEquals("USD", CountryCurrencyConfig.normalizeCurrencyCode("  "))
        assertEquals("EUR", CountryCurrencyConfig.normalizeCurrencyCode("eur"))
    }

    @Test
    fun convert_handlesUsdAndEurWithoutTreatingEurAsUsd() {
        // 1 USD = 0.92 EUR -> 100 EUR = 100 / 0.92 USD
        val eurToUsd = CountryCurrencyConfig.convert(100.0, "EUR", "USD", 0.92)
        assertEquals(108.6956, eurToUsd, 0.01)

        // 1 USD = 50 VES -> 10 USD = 500 VES
        val usdToVes = CountryCurrencyConfig.convert(10.0, "USD", "VES", 50.0)
        assertEquals(500.0, usdToVes, 0.001)

        // Same currency returns unchanged amount
        assertEquals(42.5, CountryCurrencyConfig.convert(42.5, "Bs.", "VES", 50.0), 0.001)
    }

    @Test
    fun convertCrossOrNull_pivotsViaUsdAndRejectsZeroRates() {
        // 92 EUR -> USD (0.92 EUR per USD) -> VES (50 VES per USD) = 100 USD * 50 = 5000 VES
        val ves = CountryCurrencyConfig.convertCrossOrNull(
            amount = 92.0,
            fromCurrency = "EUR",
            toCurrency = "VES",
            fromRatePerUsd = 0.92,
            toRatePerUsd = 50.0
        )
        assertEquals(5000.0, ves!!, 0.01)

        // Missing or zero rate returns null cleanly instead of 1:1 fallback
        assertNull(CountryCurrencyConfig.convertCrossOrNull(100.0, "EUR", "VES", 0.0, 50.0))
        assertNull(CountryCurrencyConfig.convertCrossOrNull(100.0, "USD", "VES", 1.0, 0.0))
    }
}
