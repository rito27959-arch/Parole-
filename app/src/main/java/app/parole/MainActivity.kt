package app.parole

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.animation.ValueAnimator
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.parole.ads.AdController
import app.parole.data.*
import app.parole.ui.*
import kotlinx.coroutines.launch

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        val splash=installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setOnExitAnimationListener{provider->
            if(ValueAnimator.areAnimatorsEnabled())provider.view.animate().alpha(0f).scaleX(1.04f).scaleY(1.04f).setDuration(180).withEndAction{provider.remove()}.start()
            else provider.remove()
        }
        enableEdgeToEdge();val ads=AdController(this)
        setContent{
            val vm:ParoleViewModel=viewModel();val state by vm.state.collectAsStateWithLifecycle();val scope=rememberCoroutineScope()
            var welcome by rememberSaveable{mutableStateOf(!vm.repository.welcomeCompleted)}
            var adsRequested by remember{mutableStateOf(false)}
            LaunchedEffect(welcome){if(!welcome&&!adsRequested){adsRequested=true;ads.initialize()}}
            val dark=(!welcome&&state.screen==Screen.SESSION)||state.appearance==Appearance.DARK||(state.appearance==Appearance.SYSTEM&&isSystemInDarkTheme())
            SideEffect { val bar=if(dark)SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)else SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT);enableEdgeToEdge(statusBarStyle=bar,navigationBarStyle=bar) }
            val pdf=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)vm.importPdf(uri)}
            val microphone=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted)vm.startRecording()else vm.error("Autorisez le microphone dans les réglages Android, ou écrivez votre réponse.")}
            val owner=LocalLifecycleOwner.current
            DisposableEffect(owner){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP)vm.pauseRecording()};owner.lifecycle.addObserver(observer);onDispose{owner.lifecycle.removeObserver(observer)}}
            BackHandler(!welcome&&state.screen !in setOf(Screen.HOME,Screen.PROGRESS,Screen.SETTINGS)){vm.navigate(Screen.HOME)}
            if(welcome)ParoleWelcome(appearance=state.appearance,initialLevel=state.level,initialJury=state.jury){level,jury->
                vm.repository.finishWelcome(level,jury);vm.configureSession(level=level,jury=jury);welcome=false
            }else ParoleUi(state,UiActions(
                navigate=vm::navigate,refresh=vm::refresh,newProject=vm::newProject,importPdf={pdf.launch(arrayOf("application/pdf"))},importText=vm::importText,
                prepare=vm::prepare,configureSession=vm::configureSession,start=vm::start,openSession=vm::openSession,draft=vm::draft,
                record={if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)vm.startRecording()else microphone.launch(Manifest.permission.RECORD_AUDIO)},
                pause=vm::togglePause,stop=vm::stopRecording,send=vm::submitAnswer,repeat=vm::repeatQuestion,play=vm::playAudio,transcribe=vm::transcribe,replay=vm::replay,
                ad={scope.launch{try{val test=BuildConfig.ADMOB_REWARDED_ID.startsWith("ca-app-pub-3940256099942544");val ticket=if(test)null else vm.repository.rewardTicket();ads.show(ticket,vm::rewardComplete,vm::notice)}catch(e:Exception){vm.error(e.message?:"La publicité est indisponible.")}}},
                appearance=vm::appearance,speech=vm::speech,consent=vm::consent,configure=vm::configure,serverCheck=vm::checkServer,login=vm::authenticate,resetPassword=vm::resetPassword,
                signOut=vm::signOut,deleteAccount=vm::deleteAccount,deleteProject=vm::deleteProject,dismiss=vm::clearError,privacy={ads.privacy(vm::notice)},welcome={welcome=true},
                export={session->val text=buildString{append("Mon entraînement avec Parole\n\n");session.answers.forEachIndexed{i,answer->append("Question ${i+1} : ${answer.question.question}\n${answer.text}\nÀ travailler : ${answer.feedback.improvements.joinToString(" ")}\n\n")};append("Bilan pédagogique indicatif généré avec l’IA.")};startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,text)},"Partager mon bilan"))}
            ))
        }
    }
}
