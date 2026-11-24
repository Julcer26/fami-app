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

    @Test
    fun onboardingScreen_individualDarkMode_composesWithoutCrashing() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                OnboardingScreen(
                    initialCountryCode = "US",
                    initialPrimaryCurrency = "USD",
                    initialSecondaryCurrency = "USD",
                    initialManagementMode = "individual",
                    onComplete = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun familyViewModel_authValidationHelpers_validateAliasPinAndFamilyCode() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = com.example.viewmodel.FamilyViewModelFactory(context)
            .create(com.example.viewmodel.FamilyViewModel::class.java)

        org.junit.Assert.assertTrue(vm.isValidAlias("Carlos"))
        org.junit.Assert.assertTrue(vm.isValidAlias("Mamá"))
        org.junit.Assert.assertFalse(vm.isValidAlias("A"))
        org.junit.Assert.assertFalse(vm.isValidAlias("carlos@gmail.com"))
        org.junit.Assert.assertFalse(vm.isValidAlias("   "))

        org.junit.Assert.assertTrue(vm.isValidPin("12345"))
        org.junit.Assert.assertFalse(vm.isValidPin("00000"))
        org.junit.Assert.assertFalse(vm.isValidPin("1234"))
        org.junit.Assert.assertFalse(vm.isValidPin("12a45"))

        org.junit.Assert.assertEquals("A1B2C3D4", vm.normalizeFamilyCode("  a1b2 c3d4! "))
        org.junit.Assert.assertEquals("FAM-2026", vm.normalizeFamilyCode("fam-2026 "))
    }

    @Test
    fun warmGraphiteDarkTheme_composesAuthScreenWithHighContrastTokens() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = com.example.viewmodel.FamilyViewModelFactory(context)
            .create(com.example.viewmodel.FamilyViewModel::class.java)

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                AuthScreen(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().assertExists()

        // Verify Warm Graphite Paper dark surfaces are neutral-warm charcoal (not saturated electric blue)
        val sheet = com.example.ui.theme.DarkPaperSheet
        val ink = com.example.ui.theme.DarkInk
        org.junit.Assert.assertTrue(
            "DarkPaperSheet should be neutral graphite, not saturated blue",
            (sheet.blue - sheet.red) < 0.05f
        )
        org.junit.Assert.assertTrue(
            "DarkInk should be high-luminance warm ivory",
            ink.red > 0.9f && ink.green > 0.9f
        )
    }

    @Test
    fun widgetQuickActivity_launchesWithoutCrashing() {
        val controller = Robolectric.buildActivity(com.example.widget.WidgetQuickActivity::class.java).setup()
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
}
