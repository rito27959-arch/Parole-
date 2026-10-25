package app.parole

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

/** Real startup, saved onboarding choices, recreation and optional replay. */
class StartupTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(object : ExternalResource() {
        override fun before() {
            InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("parole", 0).edit().clear().commit()
        }
    }).around(compose)

    @Test fun introductionCompletesOnceAndCanBeReplayed() {
        compose.onNodeWithTag("welcome_root").assertIsDisplayed()
        compose.onNodeWithTag("welcome_next").performClick()
        compose.onNodeWithTag("welcome_level_Licence").performScrollTo().performClick()
        compose.onNodeWithTag("welcome_jury_Exigeant").performScrollTo().performClick()
        compose.onNodeWithTag("welcome_next").performClick()
        compose.onNodeWithTag("welcome_next").performClick()
        compose.onNodeWithTag("app_root").assertIsDisplayed()
        compose.onNodeWithTag("new_project").assertIsDisplayed()
        val prefs=compose.activity.getSharedPreferences("parole",0)
        assertTrue(prefs.getBoolean("welcome_completed_v1",false))
        assertEquals("Licence",prefs.getString("preferred_level",null))
        assertEquals("Exigeant",prefs.getString("preferred_jury",null))
        assertFalse(prefs.getBoolean("media_consent",false))
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("welcome_root").assertDoesNotExist()
        compose.onNodeWithContentDescription("Profil").performClick()
        compose.onNodeWithText("Votre espace").assertIsDisplayed()
        compose.onNodeWithText("Revoir la présentation").performScrollTo().performClick()
        compose.onNodeWithTag("welcome_root").assertIsDisplayed()
        compose.onNodeWithTag("welcome_skip").performClick()
        compose.onNodeWithTag("app_root").assertIsDisplayed()
    }
}
