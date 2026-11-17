package com.example

import com.example.api.GeminiParser
import com.example.security.LocalCryptoBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiParserAndSecurityTest {

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
    fun localCryptoBox_hashesAndVerifiesPinsWhileRejectingDefaultZeroPin() {
        val hashed = LocalCryptoBox.hashPin("24680")
        assertTrue(hashed.isNotBlank())
        assertNotEquals("24680", hashed)
        assertTrue(LocalCryptoBox.verifyPin(hashed, "24680"))
        assertFalse(LocalCryptoBox.verifyPin(hashed, "13579"))
        assertTrue(LocalCryptoBox.hasValidPinConfigured(hashed))

        val defaultDummy = LocalCryptoBox.hashPin("00000")
        assertFalse(LocalCryptoBox.hasValidPinConfigured(defaultDummy))
        assertFalse(LocalCryptoBox.hasValidPinConfigured("00000"))
        assertFalse(LocalCryptoBox.hasValidPinConfigured(null))
        assertFalse(LocalCryptoBox.verifyPin(hashed, "123"))
    }
}
