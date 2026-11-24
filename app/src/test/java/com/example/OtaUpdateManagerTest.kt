package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OtaUpdateManagerTest {

    @Test
    fun compareVersionNames_comparesSemanticSegmentsCorrectly() {
        assertTrue(OtaUpdateManager.compareVersionNames("3.9.7", "3.9.6") > 0)
        assertTrue(OtaUpdateManager.compareVersionNames("3.10.0", "3.9.9") > 0)
        assertTrue(OtaUpdateManager.compareVersionNames("3.11.0", "3.10.0") > 0)
        assertTrue(OtaUpdateManager.compareVersionNames("v4.0", "3.9.99") > 0)
        assertTrue(OtaUpdateManager.compareVersionNames("3.9.5", "3.9.6") < 0)
        assertEquals(0, OtaUpdateManager.compareVersionNames("v3.9.6", "3.9.6"))
        assertEquals(0, OtaUpdateManager.compareVersionNames("3.9.0", "3.9"))
    }

    @Test
    fun updateInfo_preservesChangelogAndSha256Metadata() {
        val info = OtaUpdateManager.UpdateInfo(
            isUpdateAvailable = true,
            latestVersionCode = 27,
            latestVersionName = "3.11.0",
            downloadUrl = "https://github.com/Julcer26/fami-app/releases/download/v3.11.0/Fami_v3.11.0.apk",
            expectedApkSha256 = "ABCDEF1234",
            changelog = "Mejoras de inicio, registro y OTA manual"
        )
        assertTrue(info.isUpdateAvailable)
        assertEquals(27, info.latestVersionCode)
        assertEquals("Mejoras de inicio, registro y OTA manual", info.changelog)
        assertEquals("ABCDEF1234", info.expectedApkSha256)
    }
}
