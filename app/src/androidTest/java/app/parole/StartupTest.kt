package app.parole

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

/** Launches the real Activity, Application and ViewModel, not a UI-only fixture. */
class StartupTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun appStartsAndProfileOpensWithoutCloudCredentials() {
        compose.onNodeWithTag("app_root").assertIsDisplayed()
        compose.onNodeWithTag("new_project").assertIsDisplayed()
        compose.onNodeWithContentDescription("Profil").performClick()
        compose.onNodeWithText("Votre espace").assertIsDisplayed()
    }
}
