package app.parole

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import app.parole.data.*
import app.parole.ui.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** These are explicitly synthetic UI fixtures, never production data. */
class DesignTest {
    @get:Rule val compose=createComposeRule()
    private val project=Project("fixture-project","Soutenance Nextfleet",42,"2026-10-24")
    private val question=Question("Pourquoi avoir choisi le téléphone pour le suivi GPS ?",18,"Architecture")
    private val answer=Answer("a",question,"Nous avons choisi le téléphone pour limiter les coûts de matériel.",42000,Feedback(listOf("Le choix est clairement expliqué."),listOf("Précisez les limites de la localisation sur téléphone."),4,3,4,"limiter les coûts de matériel"))
    private fun state(screen:Screen,appearance:Appearance)=UiState(screen=screen,appearance=appearance,configured=true,signedIn=true,name="Miguel",projects=listOf(project),selectedProject=project,account=Account(2,0,true),mediaConsent=true,session=Session("fixture-session",project.id,currentQuestion=question,answers=if(screen==Screen.REPORT)listOf(answer)else emptyList(),status=if(screen==Screen.REPORT)"completed"else"active"))
    private fun capture(name:String){compose.waitForIdle();val bitmap=compose.onNodeWithTag("app_root").captureToImage().asAndroidBitmap();val dir=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"screenshots").apply{mkdirs()};File(dir,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}}
    @Test fun homeLight(){compose.setContent{ParoleUi(state(Screen.HOME,Appearance.LIGHT),UiActions())};compose.onNodeWithTag("new_project").assertIsDisplayed();capture("01-home-light")}
    @Test fun homeDark(){compose.setContent{ParoleUi(state(Screen.HOME,Appearance.DARK),UiActions())};compose.onNodeWithText("Les idées sont là.\nFaites-les entendre.").assertIsDisplayed();capture("02-home-dark")}
    @Test fun preparation(){compose.setContent{ParoleUi(state(Screen.PREPARE,Appearance.LIGHT),UiActions())};compose.onNodeWithText("Commencer la séance").performScrollTo().assertIsEnabled();capture("03-prepare-light")}
    @Test fun sessionDark(){compose.setContent{ParoleUi(state(Screen.SESSION,Appearance.DARK),UiActions())};compose.onNodeWithTag("jury_question").assertIsDisplayed();capture("04-session-dark")}
    @Test fun reportLight(){compose.setContent{ParoleUi(state(Screen.REPORT,Appearance.LIGHT),UiActions())};compose.onNodeWithText("Votre bilan").assertIsDisplayed();capture("05-report-light")}
    @Test fun reportDark(){compose.setContent{ParoleUi(state(Screen.REPORT,Appearance.DARK),UiActions())};compose.onNodeWithText("Votre bilan").assertIsDisplayed();capture("06-report-dark")}
    @Test fun largeTextRemainsScrollable(){compose.setContent{CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,1.6f)){ParoleUi(state(Screen.PREPARE,Appearance.DARK),UiActions())}};compose.onNodeWithText("Commencer la séance").performScrollTo().assertIsDisplayed();capture("07-large-text")}
    @Test fun emptyStateDoesNotShowInventedHistory(){compose.setContent{ParoleUi(UiState(),UiActions())};compose.onNodeWithText("Votre premier oral commence ici").assertExists();compose.onNodeWithText("Soutenance Nextfleet").assertDoesNotExist();capture("08-empty-state")}
    @Test fun navigationActuallyChangesScreen(){compose.setContent{var s by remember{mutableStateOf(state(Screen.HOME,Appearance.LIGHT))};ParoleUi(s,UiActions(navigate={s=s.copy(screen=it)}))};compose.onNodeWithContentDescription("Progrès").performClick();compose.onNodeWithText("Vos progrès").assertIsDisplayed()}
    @Test fun typingAnswerEnablesSend(){compose.setContent{var s by remember{mutableStateOf(state(Screen.SESSION,Appearance.DARK))};ParoleUi(s,UiActions(draft={s=s.copy(draft=it)}))};compose.onNodeWithTag("answer_input").performScrollTo().performTextInput("Mon choix repose sur le coût.");compose.onNodeWithText("Envoyer ma réponse").performScrollTo().assertIsEnabled()}
}
