package com.example

import com.example.currency.CountryCurrencyConfig
import com.example.ui.FamiStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FamiStringsLocalizationTest {

    @Test
    fun supportedLanguages_containsExpectedTenCoreLanguages() {
        val codes = CountryCurrencyConfig.SUPPORTED_LANGUAGES.map { it.code }.toSet()
        assertEquals(10, codes.size)
        assertEquals(
            setOf("es", "en", "pt", "fr", "it", "de", "zh", "ja", "ko", "ar"),
            codes
        )
    }

    @Test
    fun famiStrings_providesCompleteNonBlankTranslationsForAllTenLanguages() {
        val spanish = FamiStrings.get("es")
        for (lang in CountryCurrencyConfig.SUPPORTED_LANGUAGES) {
            val s = FamiStrings.get(lang.code)
            assertTrue("Missing tabNotebook for ${lang.code}", s.tabNotebook.isNotBlank())
            assertTrue("Missing tabAccounts for ${lang.code}", s.tabAccounts.isNotBlank())
            assertTrue("Missing tabChores for ${lang.code}", s.tabChores.isNotBlank())
            assertTrue("Missing tabMarket for ${lang.code}", s.tabMarket.isNotBlank())
            assertTrue("Missing tabGoals for ${lang.code}", s.tabGoals.isNotBlank())
            assertTrue("Missing settingsTitle for ${lang.code}", s.settingsTitle.isNotBlank())
            assertTrue("Missing settingsLangTitle for ${lang.code}", s.settingsLangTitle.isNotBlank())
            assertTrue("Missing settingsDeleteAccountBtn for ${lang.code}", s.settingsDeleteAccountBtn.isNotBlank())
            assertTrue("Missing recurringCreateBtn for ${lang.code}", s.recurringCreateBtn.isNotBlank())
            assertTrue("Missing save/cancel for ${lang.code}", s.save.isNotBlank() && s.cancel.isNotBlank())
            assertTrue("Missing delete/edit for ${lang.code}", s.delete.isNotBlank() && s.edit.isNotBlank())

            if (lang.code != "es") {
                assertNotEquals(
                    "Expected distinct settingsTitle translation for ${lang.code}",
                    spanish.settingsTitle,
                    s.settingsTitle
                )
            }

            val weekDays = FamiStrings.weekDayHeaders(lang.code)
            assertEquals("Expected 7 weekday headers for ${lang.code}", 7, weekDays.size)
            assertTrue(weekDays.all { it.isNotBlank() })

            val speechLocale = FamiStrings.speechLocaleFor(lang.code)
            assertTrue(
                "Invalid BCP-47 speech locale '$speechLocale' for ${lang.code}",
                speechLocale.matches(Regex("^[a-z]{2}-[A-Z]{2}$"))
            )
        }
    }

    @Test
    fun detectSystemLanguageCode_fallsBackToSpanishForUnsupportedLocale() {
        val prev = Locale.getDefault()
        try {
            Locale.setDefault(Locale.FRANCE)
            assertEquals("fr", CountryCurrencyConfig.detectSystemLanguageCode())

            Locale.setDefault(Locale("ru", "RU"))
            assertEquals("es", CountryCurrencyConfig.detectSystemLanguageCode())
        } finally {
            Locale.setDefault(prev)
        }
    }
}
