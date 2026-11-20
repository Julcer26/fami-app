package com.example

import com.example.api.GeminiParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GeminiParserTest {

    @Test
    fun extractResultJsonObject_stripsThoughtPartsAndCodeFences() {
        val raw = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "thought": true, "text": "thinking internally..." },
                      { "text": "```json\n{\"amount\": 45.75, \"title\": \"Supermercado\", \"currency\": \"USD\"}\n```" }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = GeminiParser.extractResultJsonObject(raw)
        assertNotNull(parsed)
        assertEquals(45.75, parsed!!.getDouble("amount"), 0.001)
        assertEquals("Supermercado", parsed.getString("title"))
        assertEquals("USD", parsed.getString("currency"))
    }

    @Test
    fun extractResultJsonObject_unwrapsSingleElementJsonArrayAndHandlesEmpty() {
        val arrayWrapped = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "[{\"category\":\"Comida\",\"amount\":12.5}]" }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = GeminiParser.extractResultJsonObject(arrayWrapped)
        assertNotNull(parsed)
        assertEquals("Comida", parsed!!.getString("category"))
        assertEquals(12.5, parsed.getDouble("amount"), 0.001)

        assertNull(GeminiParser.extractResultJsonObject("""{"candidates":[]}"""))
    }

    @Test
    fun resolveSupportedCurrency_supportsAllLatamAndGlobalCurrencies() {
        assertEquals("VES", GeminiParser.resolveSupportedCurrency("Bs"))
        assertEquals("VES", GeminiParser.resolveSupportedCurrency("VES"))
        assertEquals("COP", GeminiParser.resolveSupportedCurrency("COP"))
        assertEquals("MXN", GeminiParser.resolveSupportedCurrency("MXN"))
        assertEquals("ARS", GeminiParser.resolveSupportedCurrency("ARS"))
        assertEquals("EUR", GeminiParser.resolveSupportedCurrency("€"))
        assertEquals("EUR", GeminiParser.resolveSupportedCurrency("EUR"))
        assertEquals("USD", GeminiParser.resolveSupportedCurrency("$"))
        assertEquals("USD", GeminiParser.resolveSupportedCurrency(""))
    }
}
