package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OtaUpdateManagerTest {

    @Test
    fun compareVersionNames_comparesSemanticSegmentsCorrectly() {
        assertTrue(OtaUpdateManager.compareVersionNames("3.9.7", "3.9.6") > 0)
        assertTrue(OtaUpdateManager.compareVersionNames("3.10.0", "3.9.9") > 0)
        assertTrue(OtaUpdateManager.compareVersionNames("v4.0", "3.9.99") > 0)
        assertTrue(OtaUpdateManager.compareVersionNames("3.9.5", "3.9.6") < 0)
        assertEquals(0, OtaUpdateManager.compareVersionNames("v3.9.6", "3.9.6"))
        assertEquals(0, OtaUpdateManager.compareVersionNames("3.9.0", "3.9"))
    }
}
