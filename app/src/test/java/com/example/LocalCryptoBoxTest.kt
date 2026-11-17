package com.example

import com.example.security.LocalCryptoBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalCryptoBoxTest {

    @Test
    fun hashPin_and_verifyPin_validateCorrectPinAndRejectWrongPins() {
        val hashed = LocalCryptoBox.hashPin("24680")
        assertTrue(hashed.isNotBlank())
        assertNotEquals("24680", hashed)
        assertTrue(LocalCryptoBox.verifyPin(hashed, "24680"))
        assertFalse(LocalCryptoBox.verifyPin(hashed, "13579"))
        assertFalse(LocalCryptoBox.verifyPin(hashed, "123"))
        assertTrue(LocalCryptoBox.hasValidPinConfigured(hashed))
    }

    @Test
    fun hasValidPinConfigured_rejectsBlankAndDefaultDummyPins() {
        val defaultDummy = LocalCryptoBox.hashPin("00000")
        assertFalse(LocalCryptoBox.hasValidPinConfigured(defaultDummy))
        assertFalse(LocalCryptoBox.hasValidPinConfigured("00000"))
        assertFalse(LocalCryptoBox.hasValidPinConfigured(""))
        assertFalse(LocalCryptoBox.hasValidPinConfigured(null))
        // Legacy plaintext 5-digit PIN remains recognized before migration
        assertTrue(LocalCryptoBox.hasValidPinConfigured("98765"))
        assertTrue(LocalCryptoBox.verifyPin("98765", "98765"))
        assertEquals("", LocalCryptoBox.encrypt(""))
        assertEquals("", LocalCryptoBox.decrypt(""))
    }
}
