package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainActivityLaunchTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun ensureFirebaseInitialized() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
    }

    @Test
    fun mainActivity_launchesWithoutCrashing() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get()
            assertNotNull(activity)
            assertFalse(activity.isFinishing)
            assertFalse(activity.isDestroyed)
            assertNotNull(activity.window)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun onboardingScreen_composesWithoutCrashing() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                OnboardingScreen(
                    onComplete = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().assertExists()
    }
}
