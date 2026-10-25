package app.parole

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import app.parole.data.Appearance
import app.parole.ui.ParoleWelcome
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WelcomeTest {
    @get:Rule val compose=createComposeRule()
    private fun capture(name:String) {
        val bitmap=compose.onNodeWithTag("welcome_root").captureToImage().asAndroidBitmap()
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val values=android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,"$name.png")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,"Pictures/ParoleTests")
        }
        val uri=checkNotNull(context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values))
        checkNotNull(context.contentResolver.openOutputStream(uri)).use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    private fun journey(theme:Appearance,suffix:String) {
        compose.setContent{ParoleWelcome(appearance=theme,motionEnabled=false){_,_->}}
        compose.onNodeWithTag("welcome_next").assertIsDisplayed();capture("09-welcome-voice-$suffix")
        compose.onNodeWithTag("welcome_next").performClick();capture("10-welcome-jury-$suffix")
        compose.onNodeWithTag("welcome_next").performClick();capture("11-welcome-start-$suffix")
    }
    @Test fun lightJourney(){journey(Appearance.LIGHT,"light")}
    @Test fun darkJourney(){journey(Appearance.DARK,"dark")}
    @Test fun choicesSurviveBackAndReachCompletion(){
        var result:Pair<String,String>?=null
        compose.setContent{ParoleWelcome(motionEnabled=false){level,jury->result=level to jury}}
        compose.onNodeWithTag("welcome_next").performClick()
        compose.onNodeWithTag("welcome_level_Master").performScrollTo().performClick()
        compose.onNodeWithTag("welcome_jury_Exigeant").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Étape précédente").performClick()
        compose.onNodeWithTag("welcome_next").performClick()
        compose.onNodeWithTag("welcome_level_Master").assertIsSelected()
        compose.onNodeWithTag("welcome_jury_Exigeant").assertIsSelected()
        compose.onNodeWithTag("welcome_next").performClick()
        compose.onNodeWithTag("welcome_next").performClick()
        compose.runOnIdle{assertEquals("Master" to "Exigeant",result)}
    }
    @Test fun skipWorksWithoutNetworkOrConsent(){
        var result:Pair<String,String>?=null
        compose.setContent{ParoleWelcome(initialLevel="Licence",motionEnabled=false){level,jury->result=level to jury}}
        compose.onNodeWithTag("welcome_skip").performClick()
        compose.runOnIdle{assertEquals("Licence" to "Bienveillant",result)}
    }
    @Test fun largeTextKeepsControlsAndContentReachable(){
        compose.setContent{CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,2f)){
            ParoleWelcome(appearance=Appearance.DARK,initialPage=2,motionEnabled=false){_,_->}
        }}
        compose.onNodeWithTag("welcome_next").assertIsDisplayed()
        compose.onNodeWithText("Repartez avec une piste").performScrollTo().assertIsDisplayed()
        capture("12-welcome-large-text")
    }
}
