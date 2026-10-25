package app.parole

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.parole.data.*
import app.parole.voice.VoiceEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.UUID

class ParoleViewModel(application:Application):AndroidViewModel(application) {
    val repository=(application as ParoleApplication).repository
    private val voice=VoiceEngine(application)
    private val mutable=MutableStateFlow(UiState(appearance=repository.appearance,apiUrl=repository.apiUrl,configured=repository.configured,speechEnabled=repository.speechEnabled,mediaConsent=repository.mediaConsent,level=repository.preferredLevel,jury=repository.preferredJury))
    val state=mutable.asStateFlow()
    private var ticker:Job?=null
    private var answerRequestId=UUID.randomUUID().toString()
    private var startRequest:StartRequest?=null
    init{refresh()}
    fun navigate(screen:Screen){if(state.value.recording)pauseRecording();voice.silence();mutable.update{it.copy(screen=screen,error=null)}}
    fun clearError(){mutable.update{it.copy(error=null,notice=null)}}
    fun error(message:String){mutable.update{it.copy(error=message)}}
    fun notice(message:String){mutable.update{it.copy(notice=message)}}
    fun appearance(value:Appearance){repository.appearance=value;mutable.update{it.copy(appearance=value)}}
    fun speech(value:Boolean){repository.speechEnabled=value;mutable.update{it.copy(speechEnabled=value)};if(!value)voice.silence()}
    fun consent(value:Boolean){if(!value&&state.value.recording){ticker?.cancel();voice.cancel();mutable.update{it.copy(recording=false,paused=false,amplitude=0f)}};repository.mediaConsent=value;mutable.update{it.copy(mediaConsent=value)}}
    fun configure(url:String){val trimmed=url.trim().trimEnd('/');if(trimmed.isNotBlank()&&(!trimmed.startsWith("https://")||runCatching{java.net.URI(trimmed).host}.getOrNull().isNullOrBlank())){error("Utilisez une adresse HTTPS valide.");return};repository.apiUrl=trimmed;mutable.update{it.copy(apiUrl=trimmed,configured=repository.configured,notice="Adresse du serveur enregistrée.")}}
    private fun task(block:suspend()->Unit){if(state.value.busy)return;mutable.update{it.copy(busy=true,error=null)};viewModelScope.launch{try{block()}catch(e:CancellationException){throw e}catch(e:Exception){mutable.update{it.copy(error=friendly(e))}}finally{mutable.update{it.copy(busy=false)}}}}
    private fun friendly(e:Exception):String=when(e){is ApiException->e.message?:"Service indisponible";is java.net.UnknownHostException->"Connexion Internet indisponible. Vos données locales sont conservées.";is java.net.SocketTimeoutException->"Le serveur met trop de temps à répondre. Réessayez.";is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException->"Vérifiez l’adresse e-mail et le mot de passe.";is com.google.firebase.auth.FirebaseAuthUserCollisionException->"Cette adresse possède déjà un compte.";is com.google.firebase.auth.FirebaseAuthWeakPasswordException->"Choisissez un mot de passe d’au moins 8 caractères.";is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException->"Reconnectez-vous avant de supprimer votre compte.";else->e.message?.takeIf{it.length<180}?:"Une erreur est survenue. Réessayez."}
    fun refresh(){task{repository.initializeCloud();val user=repository.auth?.currentUser;mutable.update{it.copy(signedIn=user!=null,email=user?.email.orEmpty(),name=user?.displayName.orEmpty(),configured=repository.configured)};val cache=repository.readCache();mutable.update{it.copy(projects=cache.projects,sessions=cache.sessions)};if(user!=null&&repository.configured)loadRemote()}}
    private suspend fun loadRemote(){try{val projects=repository.projects();val sessions=repository.sessions();val account=repository.account();repository.cache(projects,sessions);mutable.update{it.copy(projects=projects,sessions=sessions,account=account,offline=false)}}catch(e:Exception){mutable.update{it.copy(offline=true)};throw e}}
    fun authenticate(name:String,email:String,password:String,register:Boolean){task{if(register)repository.signUp(name,email,password)else repository.signIn(email,password);val user=repository.auth?.currentUser;mutable.update{it.copy(name=user?.displayName.orEmpty(),email=user?.email.orEmpty(),signedIn=true,screen=Screen.HOME)};if(repository.configured)loadRemote()}}
    fun resetPassword(email:String){task{repository.resetPassword(email);notice("Si un compte existe, vous recevrez un lien de réinitialisation.")}}
    fun signOut(){task{ticker?.cancel();voice.cancel();voice.silence();repository.signOut();mutable.update{UiState(appearance=it.appearance,apiUrl=it.apiUrl,configured=it.configured,speechEnabled=it.speechEnabled,mediaConsent=it.mediaConsent)}}}
    fun deleteAccount(password:String){task{repository.deleteAccount(password);ticker?.cancel();voice.cancel();voice.silence();mutable.update{UiState(appearance=it.appearance,apiUrl=it.apiUrl,configured=it.configured,notice="Votre compte a été supprimé.")}}}
    fun checkServer(){task{repository.health();repository.initializeCloud();mutable.update{it.copy(configured=repository.configured,serverStatus="Connecté et configuré",notice="Le serveur répond correctement.")}}}
    fun newProject(){navigate(if(!state.value.signedIn)Screen.AUTH else Screen.IMPORT)}
    private fun requireConsent(){if(!state.value.mediaConsent)throw ApiException(403,"CONSENT_REQUIRED","Autorisez le traitement par l’IA avant d’envoyer votre contenu.")}
    fun importPdf(uri:Uri){task{requireConsent();val project=repository.importPdf(uri);loadRemote();prepare(project)}}
    fun importText(title:String,text:String){task{requireConsent();val project=repository.importText(title,text);loadRemote();prepare(project)}}
    fun prepare(project:Project,rewarded:Boolean=false){startRequest=null;mutable.update{it.copy(selectedProject=project,screen=Screen.PREPARE,rewarded=rewarded,error=null)}}
    fun configureSession(level:String=state.value.level,minutes:Int=state.value.minutes,jury:String=state.value.jury){startRequest=null;mutable.update{it.copy(level=level,minutes=minutes,jury=jury)}}
    fun start(){task{requireConsent();val s=state.value;val project=s.selectedProject?:return@task
        val req=startRequest?:StartRequest(project.id,UUID.randomUUID().toString(),s.level,s.minutes,s.jury,if(s.minutes==5)4 else 6,s.rewarded).also{startRequest=it}
        var session=repository.start(req)
        repeat(8){if(session.status=="preparing"){delay(1500);session=repository.session(session.id)}}
        if(session.status=="preparing")throw ApiException(409,"PREPARING","Le jury prépare encore la séance. Réessayez dans un instant.")
        enterSession(session);loadRemote()
    }}
    private fun enterSession(session:Session){
        val previous=state.value
        if(previous.recording&&previous.session?.id==session.id&&previous.session.answers.size==session.answers.size){mutable.update{it.copy(session=session,screen=Screen.SESSION)};return}
        ticker?.cancel();voice.cancel()
        answerRequestId=UUID.randomUUID().toString();mutable.update{it.copy(session=session,screen=if(session.status=="completed")Screen.REPORT else Screen.SESSION,draft=repository.draft(session.id),elapsedMs=0,lastAudio=null,recording=false,paused=false,replayAnswer=null)};if(state.value.speechEnabled)session.currentQuestion?.let{voice.speak(it.question)}}
    fun openSession(session:Session){if(session.status=="completed"){mutable.update{it.copy(session=session,screen=Screen.REPORT)};return};task{enterSession(repository.session(session.id))}}
    fun draft(text:String){val value=text.take(8000);mutable.update{it.copy(draft=value)};state.value.session?.let{repository.saveDraft(it.id,value)}}
    fun startRecording(){val s=state.value;val session=s.session?:return
        if(s.busy||!s.mediaConsent)return
        try{voice.start(session.id,session.answers.size);mutable.update{it.copy(recording=true,paused=false,elapsedMs=0,lastAudio=null)};ticker?.cancel();ticker=viewModelScope.launch{while(isActive){mutable.update{it.copy(elapsedMs=voice.elapsed(),amplitude=voice.amplitude())};if(voice.elapsed()>=120000){stopRecording();break};delay(80)}}}catch(e:Exception){error("Impossible d’ouvrir le microphone. Vérifiez son autorisation.")}}
    fun pauseRecording(){if(!state.value.recording||state.value.paused)return;voice.pause();mutable.update{it.copy(paused=true,amplitude=0f)}}
    fun togglePause(){if(state.value.paused){runCatching{voice.resume()}.onSuccess{mutable.update{it.copy(paused=false)}}.onFailure{error("Impossible de reprendre. Terminez cet enregistrement.")}}else pauseRecording()}
    fun stopRecording(){ticker?.cancel();val duration=voice.elapsed();val file=runCatching{voice.stop()}.getOrElse{error("Enregistrement trop court. Réessayez.");null};mutable.update{it.copy(recording=false,paused=false,elapsedMs=duration,amplitude=0f,lastAudio=file?.path)};if(file!=null)transcribe()}
    fun transcribe(){val file=state.value.lastAudio?.let{File(it)}?:return;task{requireConsent();val result=repository.transcribe(file);if(result.text.isBlank())throw ApiException(422,"NO_SPEECH","Aucune parole reconnue. Réécoutez l’audio ou écrivez votre réponse.");draft(result.text)}}
    fun playAudio(){state.value.lastAudio?.let{runCatching{voice.play(File(it))}.onFailure{error("Impossible de relire cet audio.")}}}
    fun repeatQuestion(){state.value.session?.currentQuestion?.let{voice.speak(it.question)}}
    fun submitAnswer(){task{requireConsent();val s=state.value;val session=s.session?:return@task;val updated=repository.answer(session.id,AnswerRequest(answerRequestId,session.answers.size,s.draft.trim(),s.elapsedMs));repository.clearDraft(session.id);val audio=s.lastAudio
        if(audio!=null){val saved=File(getApplication<Application>().filesDir,"recordings/${session.id}_${session.answers.size}.m4a");File(audio).renameTo(saved)}
        enterSession(updated);loadRemote()
    }}
    fun replay(index:Int){val s=state.value.session?:return;val file=File(getApplication<Application>().filesDir,"recordings/${s.id}_${index}.m4a");if(file.exists())runCatching{voice.play(file)}.onFailure{error("Lecture indisponible.")}else notice("Cet enregistrement n’est pas disponible sur cet appareil.");mutable.update{it.copy(replayAnswer=index)}}
    fun deleteProject(project:Project){task{repository.deleteProject(project.id);loadRemote();notice("Projet supprimé.")}}
    fun rewardComplete(){viewModelScope.launch{repeat(6){delay(2000);runCatching{repository.account()}.onSuccess{a->mutable.update{s->s.copy(account=a)};if(a.credits>=2){notice("Vos 2 questions supplémentaires sont disponibles.");return@launch}}};notice("Récompense en cours de validation. Elle apparaîtra après confirmation du serveur.")}}
    override fun onCleared(){ticker?.cancel();voice.close();super.onCleared()}
}
