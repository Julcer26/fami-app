package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.analytics.FamiUsageTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class FamiUsageTrackerTest {

    @Test
    fun currentMonthKey_formatsYearAndMonthZeroPadded() {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(2026, Calendar.NOVEMBER, 24, 15, 30, 0)
        }
        assertEquals("2026-11", FamiUsageTracker.currentMonthKey(cal.timeInMillis))
    }

    @Test
    fun sanitizeFeatureKey_normalizesAndTruncatesKeys() {
        assertEquals("tx_voice_nlp", FamiUsageTracker.sanitizeFeatureKey("  TX-Voice NLP!! "))
        assertEquals(48, FamiUsageTracker.sanitizeFeatureKey("a".repeat(80)).length)
        assertEquals("", FamiUsageTracker.sanitizeFeatureKey("___---___"))
    }

    @Test
    fun isEnabled_defaultsTrueAndPersistsToggle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FamiUsageTracker.setEnabled(context, true)
        assertTrue(FamiUsageTracker.isEnabled(context))

        FamiUsageTracker.setEnabled(context, false)
        assertFalse(FamiUsageTracker.isEnabled(context))

        FamiUsageTracker.setEnabled(context, true)
        assertTrue(FamiUsageTracker.isEnabled(context))
    }
}
