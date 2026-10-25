@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package app.parole.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import app.parole.data.*

class UiActions(
    val navigate:(Screen)->Unit={},val refresh:()->Unit={},val newProject:()->Unit={},val importPdf:()->Unit={},val importText:(String,String)->Unit={_,_->},
    val prepare:(Project,Boolean)->Unit={_,_->},val configureSession:(String,Int,String)->Unit={_,_,_->},val start:()->Unit={},val openSession:(Session)->Unit={},
    val draft:(String)->Unit={},val record:()->Unit={},val pause:()->Unit={},val stop:()->Unit={},val send:()->Unit={},val repeat:()->Unit={},val play:()->Unit={},val transcribe:()->Unit={},
    val replay:(Int)->Unit={},val ad:()->Unit={},val appearance:(Appearance)->Unit={},val speech:(Boolean)->Unit={},val consent:(Boolean)->Unit={},
    val configure:(String)->Unit={},val serverCheck:()->Unit={},val login:(String,String,String,Boolean)->Unit={_,_,_,_->},val resetPassword:(String)->Unit={},
    val signOut:()->Unit={},val deleteAccount:(String)->Unit={},val deleteProject:(Project)->Unit={},val dismiss:()->Unit={},val privacy:()->Unit={},val export:(Session)->Unit={}
)
@Composable fun ParoleUi(state:UiState,actions:UiActions){
    val rootScreens=setOf(Screen.HOME,Screen.PROGRESS,Screen.SETTINGS)
    val immersive=state.screen==Screen.SESSION
    val snackbar=remember{SnackbarHostState()}
    LaunchedEffect(state.error,state.notice){val message=state.error?:state.notice;if(message!=null){snackbar.showSnackbar(message,actionLabel="Fermer",duration=SnackbarDuration.Long);actions.dismiss()}}
    ParoleTheme(if(immersive)Appearance.DARK else state.appearance){
        Scaffold(modifier=Modifier.testTag("app_root"),containerColor=MaterialTheme.colorScheme.background,
            snackbarHost={SnackbarHost(snackbar)},bottomBar={if(state.screen in rootScreens)ParoleNavigation(state.screen,actions.navigate)}){padding->
            Box(Modifier.fillMaxSize().padding(padding).imePadding()){
                Crossfade(targetState=state.screen,label="screen"){screen->
                    when(screen){
                        Screen.HOME->HomeScreen(state,actions)
                        Screen.IMPORT->ImportScreen(state,actions)
                        Screen.PREPARE->PreparationScreen(state,actions)
                        Screen.SESSION->SessionScreen(state,actions)
                        Screen.REPORT->ReportScreen(state,actions)
                        Screen.PROGRESS->ProgressScreen(state,actions)
                        Screen.SETTINGS->SettingsScreen(state,actions)
                        Screen.AUTH->AuthScreen(state,actions)
                    }
                }
                if(state.busy){LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter).testTag("loading"),color=MaterialTheme.colorScheme.primary)}
            }
        }
    }
}
@Composable private fun ParoleNavigation(selected:Screen,navigate:(Screen)->Unit){
    val tabs=listOf(Triple(Screen.HOME,Icons.Rounded.Home,"Accueil"),Triple(Screen.PROGRESS,Icons.Rounded.BarChart,"Progrès"),Triple(Screen.SETTINGS,Icons.Rounded.PersonOutline,"Profil"))
    Surface(color=MaterialTheme.colorScheme.surface.copy(alpha=.96f),shape=RoundedCornerShape(topStart=26.dp,topEnd=26.dp),shadowElevation=8.dp){Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=14.dp,vertical=7.dp)){
        tabs.forEach{(screen,icon,label)->val active=selected==screen
            Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(if(active)MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable{navigate(screen)}.heightIn(min=60.dp).padding(9.dp).semantics{contentDescription=label;role=Role.Tab;this.selected=active},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)){
                Icon(icon,null,Modifier.size(23.dp),tint=if(active)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant);Text(label,style=MaterialTheme.typography.labelMedium,color=if(active)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }}
}
@Composable private fun Page(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=18.dp),verticalArrangement=Arrangement.spacedBy(22.dp),content=content)}
@Composable fun HomeScreen(state:UiState,a:UiActions){Page{
    Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Parole",style=MaterialTheme.typography.displaySmall);Text("Votre prochaine prise de parole",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)};IconButton(onClick={a.navigate(Screen.SETTINGS)},modifier=Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)){if(state.name.isNotBlank())Text(state.name.take(1).uppercase(),fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)else Icon(Icons.Rounded.PersonOutline,"Votre profil",tint=MaterialTheme.colorScheme.primary)}}
    Spacer(Modifier.height(4.dp))
    Text("Les idées sont là.\nFaites-les entendre.",style=MaterialTheme.typography.headlineLarge)
    PrimaryButton("Nouvelle préparation",a.newProject,enabled=!state.busy,icon=Icons.Rounded.Add,modifier=Modifier.testTag("new_project"))
    if(state.offline)Pill("Hors connexion · historique conservé",Icons.Rounded.WifiOff)
    if(!state.configured)SoftCard(Modifier.fillMaxWidth()){
        Text("Votre espace se prépare",style=MaterialTheme.typography.titleMedium);Spacer(Modifier.height(6.dp));Text("Le service cloud doit être activé pour discuter avec le jury.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);TextButton(onClick={a.navigate(Screen.SETTINGS)}){Text("Vérifier la connexion")}
    }
    if(state.projects.isEmpty())EmptyState("Votre premier oral commence ici","Importez un rapport PDF ou collez votre texte. Préparez une présentation qui vous ressemble.",Icons.Rounded.Description)
    else {
        SectionLabel("Reprendre","Actualiser",a.refresh)
        state.projects.take(4).forEach{project->
            val session=state.sessions.firstOrNull{it.projectId==project.id&&it.status=="active"}
            SoftCard(Modifier.fillMaxWidth()){
                Row(verticalAlignment=Alignment.CenterVertically){IconTile(Icons.Rounded.Description);Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(project.title,style=MaterialTheme.typography.titleMedium);Text("Rapport · ${project.pageCount} pages",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
                Spacer(Modifier.height(18.dp));TinyDivider();Spacer(Modifier.height(8.dp))
                Row(verticalAlignment=Alignment.CenterVertically){Text(if(session!=null)"${session.answers.size} réponse(s) sur ${session.questionCount}" else "Prêt pour votre première séance",style=MaterialTheme.typography.bodySmall,modifier=Modifier.weight(1f),color=MaterialTheme.colorScheme.onSurfaceVariant);IconButton(onClick={if(session!=null)a.openSession(session)else a.prepare(project,false)},modifier=Modifier.background(ParoleBlue,CircleShape)){Icon(Icons.Rounded.PlayArrow,"Commencer ${project.title}",tint=Color.White)}}
            }
        }
    }
    SoftCard(Modifier.fillMaxWidth()){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.GraphicEq,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(14.dp));Column{Text("Un peu mieux, chaque jour",style=MaterialTheme.typography.titleMedium);Text("5 minutes pour progresser",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
    if(state.signedIn)Text("${state.account.freeRemaining} séance(s) disponible(s) aujourd’hui",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
}}
@Composable fun ImportScreen(state:UiState,a:UiActions){var title by remember{mutableStateOf("")};var text by remember{mutableStateOf("")};Page{
    BackTitle("Votre sujet",{a.navigate(Screen.HOME)},"Un rapport. Une idée. Votre prochaine réussite.")
    SoftCard(Modifier.fillMaxWidth()){IconTile(Icons.Rounded.UploadFile);Spacer(Modifier.height(16.dp));Text("Importer votre rapport",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(8.dp));Text("PDF avec texte sélectionnable · 80 pages · 10 Mo maximum",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(18.dp));PrimaryButton("Choisir un PDF",a.importPdf,enabled=!state.busy&&state.mediaConsent)}
    MediaConsent(state,a)
    SectionLabel("Ou coller votre texte")
    OutlinedTextField(title,{title=it.take(100)},label={Text("Nom du projet")},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(18.dp))
    OutlinedTextField(text,{text=it.take(100000)},label={Text("Contenu du rapport")},supportingText={Text("Au moins 80 caractères · ${text.length} saisis")},modifier=Modifier.fillMaxWidth().heightIn(min=190.dp),minLines=6,shape=RoundedCornerShape(18.dp))
    PrimaryButton("Créer ma préparation",{a.importText(title,text)},enabled=!state.busy&&title.trim().length>=2&&text.trim().length>=80&&state.mediaConsent)
}}
@Composable private fun MediaConsent(state:UiState,a:UiActions){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top){Checkbox(state.mediaConsent,a.consent);Text("J’accepte l’envoi de mon rapport et de mes réponses à Parole et Groq pour préparer mon oral. Je peux supprimer mes données depuis mon profil.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.weight(1f).padding(top=10.dp))}}
@Composable fun PreparationScreen(state:UiState,a:UiActions){Page{
    BackTitle("Votre soutenance",{a.navigate(Screen.HOME)})
    SoftCard(Modifier.fillMaxWidth()){Row(verticalAlignment=Alignment.CenterVertically){IconTile(Icons.Rounded.Description);Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(state.selectedProject?.title?:"Votre rapport",style=MaterialTheme.typography.titleMedium);Text("${state.selectedProject?.pageCount?:0} pages · Document prêt",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Rounded.CheckCircle,null,tint=MaterialTheme.colorScheme.primary)}}
    SectionLabel("Votre séance")
    SoftCard(Modifier.fillMaxWidth()){
        ChoiceRow("Niveau",listOf("BTS","Licence","Master","Autre"),state.level){a.configureSession(it,state.minutes,state.jury)}
        Spacer(Modifier.height(15.dp));TinyDivider();Spacer(Modifier.height(15.dp))
        ChoiceRow("Durée",listOf("5 min","10 min","15 min"),"${state.minutes} min"){a.configureSession(state.level,it.substringBefore(' ').toInt(),state.jury)}
    }
    ChoiceRow("Votre jury",listOf("Bienveillant","Exigeant"),state.jury){a.configureSession(state.level,state.minutes,it)}
    Text("Des questions adaptées à votre rapport. Vous pourrez relire la transcription avant d’envoyer chaque réponse.",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
    if(!state.mediaConsent)MediaConsent(state,a)
    PrimaryButton(if(state.busy)"Préparation du jury…" else if(state.rewarded)"Utiliser mes 2 questions"else"Commencer la séance",a.start,enabled=!state.busy&&state.mediaConsent,icon=Icons.Rounded.PlayArrow)
    if(state.account.credits>=2&&!state.rewarded)TextButton(onClick={state.selectedProject?.let{a.prepare(it,true)}}){Text("Utiliser mes questions bonus (${state.account.credits})")}
}}
@Composable fun SessionScreen(state:UiState,a:UiActions){val session=state.session;Page{
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={a.navigate(Screen.HOME)},modifier=Modifier.background(MaterialTheme.colorScheme.surface,CircleShape)){Icon(Icons.Rounded.Close,"Revenir à l’accueil")};Text("Soutenance",textAlign=TextAlign.Center,modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleMedium);Pill(SessionMetrics.clock(state.elapsedMs))}
    Spacer(Modifier.height(6.dp))
    Text("QUESTION ${(session?.answers?.size?:0)+1} SUR ${session?.questionCount?:6}",style=MaterialTheme.typography.labelMedium,letterSpacing=2.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.align(Alignment.CenterHorizontally))
    Text(session?.currentQuestion?.question?:"Votre question arrive…",style=MaterialTheme.typography.headlineMedium,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().testTag("jury_question"))
    session?.currentQuestion?.sourcePage?.let{Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){Pill("Votre rapport · p. $it",Icons.Rounded.ContentCopy)}}
    Waveform(state.amplitude,state.recording&&!state.paused)
    Row(Modifier.align(Alignment.CenterHorizontally),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(8.dp).clip(CircleShape).background(if(state.recording&&!state.paused)Color(0xFF87ABFF)else MaterialTheme.colorScheme.onSurfaceVariant));Spacer(Modifier.width(9.dp));Text(when{state.busy->"Le jury prépare la suite…";state.paused->"Enregistrement en pause";state.recording->"Le jury vous écoute";state.draft.isNotBlank()->"Relisez votre réponse";else->"À vous de prendre la parole"},style=MaterialTheme.typography.bodyMedium)}
    if(state.recording){
        PrimaryButton("Terminer l’enregistrement",a.stop,icon=Icons.Rounded.Check)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){TextButton(onClick=a.pause){Icon(if(state.paused)Icons.Rounded.PlayArrow else Icons.Rounded.Pause,null);Spacer(Modifier.width(7.dp));Text(if(state.paused)"Reprendre" else "Pause")}}
        Text("2 minutes maximum par réponse",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.align(Alignment.CenterHorizontally))
    } else {
        if(state.draft.isBlank())PrimaryButton("Répondre à voix haute",a.record,enabled=!state.busy&&state.mediaConsent,icon=Icons.Rounded.Mic)
        else PrimaryButton("Envoyer ma réponse",a.send,enabled=!state.busy&&state.draft.trim().length>=3,icon=Icons.Rounded.Check)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){TextButton(onClick=a.repeat,enabled=!state.busy){Icon(Icons.AutoMirrored.Rounded.VolumeUp,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("Réécouter")};if(state.draft.isNotBlank())TextButton(onClick=a.record,enabled=!state.busy){Icon(Icons.Rounded.Mic,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("Réenregistrer")}}
    }
    OutlinedTextField(state.draft,a.draft,label={Text(if(state.lastAudio!=null)"Votre transcription"else"Ou écrire votre réponse")},modifier=Modifier.fillMaxWidth().heightIn(min=130.dp).testTag("answer_input"),minLines=3,enabled=!state.recording&&!state.busy,shape=RoundedCornerShape(20.dp))
    if(state.lastAudio!=null&&!state.recording)Row{TextButton(onClick=a.play){Text("Écouter mon audio")};TextButton(onClick=a.transcribe,enabled=!state.busy){Text("Retranscrire")}}
    if(!state.mediaConsent)MediaConsent(state,a)
}}
@Composable fun ReportScreen(state:UiState,a:UiActions){val session=state.session?:return;val project=state.projects.firstOrNull{it.id==session.projectId};Page{
    BackTitle("Votre bilan",{a.navigate(Screen.PROGRESS)},project?.title?:"Votre soutenance")
    Text("Une séance de plus.\nUn pas en avant.",style=MaterialTheme.typography.headlineLarge)
    FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Pill(SessionMetrics.clock(SessionMetrics.duration(session)),Icons.Rounded.Schedule);Pill("${session.answers.size} réponses",Icons.AutoMirrored.Rounded.Chat);SessionMetrics.wordsPerMinute(session)?.let{Pill("$it mots/min")}}
    SoftCard(Modifier.fillMaxWidth()){
        val metrics=listOf("Clarté" to SessionMetrics.mean(session){it.clarity},"Structure" to SessionMetrics.mean(session){it.structure},"Précision" to SessionMetrics.mean(session){it.precision})
        metrics.forEachIndexed{index,(title,value)->Row(Modifier.fillMaxWidth().padding(vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(8.dp).background(if((value?:0.0)>=4)Color(0xFF32B98E)else Color(0xFFD9A440),CircleShape));Spacer(Modifier.width(12.dp));Text(title,modifier=Modifier.weight(1f));Text(SessionMetrics.label(value),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)};if(index<2)TinyDivider()}
    }
    Text("Évaluation indicative de vos réponses par l’IA. Elle ne prédit pas la note de votre jury.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    SectionLabel("Vos prochains pas")
    session.answers.forEachIndexed{index,answer->SoftCard(Modifier.fillMaxWidth()){
        Text("Question ${index+1}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(7.dp));Text(answer.question.question,style=MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp));answer.feedback.strengths.forEach{Text("✓ $it",style=MaterialTheme.typography.bodyMedium);Spacer(Modifier.height(5.dp))}
        answer.feedback.improvements.forEach{Text(it,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(5.dp))}
        if(answer.feedback.evidence.isNotBlank()){Spacer(Modifier.height(8.dp));Text("« ${answer.feedback.evidence} »",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.primary)}
        TextButton(onClick={a.replay(index)}){Icon(Icons.Rounded.PlayCircleOutline,null);Spacer(Modifier.width(7.dp));Text("Réécouter ma réponse")}
        if(state.replayAnswer==index){TinyDivider();Spacer(Modifier.height(10.dp));Text(answer.text,style=MaterialTheme.typography.bodyMedium)}
    }}
    if(project!=null)PrimaryButton("M’entraîner à nouveau",{a.prepare(project,false)},icon=Icons.Rounded.Replay)
    OutlinedButton(onClick={a.export(session)},modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),shape=RoundedCornerShape(18.dp)){Text("Partager mon bilan")}
    if(state.account.rewardsEnabled)TextButton(onClick=a.ad,enabled=!state.busy,modifier=Modifier.fillMaxWidth()){Icon(Icons.Rounded.PlayCircleOutline,null);Spacer(Modifier.width(8.dp));Text("Publicité facultative · +2 questions")}
}}
@Composable fun ProgressScreen(state:UiState,a:UiActions){Page{
    Text("Vos progrès",style=MaterialTheme.typography.displaySmall);Text("Chaque prise de parole compte.",color=MaterialTheme.colorScheme.onSurfaceVariant)
    val completed=state.sessions.filter{it.status=="completed"}
    if(completed.isEmpty())EmptyState("Votre progression commence ici","Vos séances terminées et leurs conseils apparaîtront dans cet espace.",Icons.Rounded.BarChart,"Préparer mon oral",a.newProject)
    else {
        SoftCard(Modifier.fillMaxWidth()){Text("${completed.size}",style=MaterialTheme.typography.displaySmall,color=MaterialTheme.colorScheme.primary);Text("séance(s) terminée(s)",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp));Text("${completed.sumOf{it.answers.size}} réponses travaillées",style=MaterialTheme.typography.titleMedium)}
        SectionLabel("Votre historique","Actualiser",a.refresh)
        completed.forEach{session->SoftCard(Modifier.fillMaxWidth().clickable{a.openSession(session)}){
            Row(verticalAlignment=Alignment.CenterVertically){IconTile(Icons.Rounded.CheckCircleOutline);Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(state.projects.firstOrNull{it.id==session.projectId}?.title?:"Soutenance",style=MaterialTheme.typography.titleMedium);Text("${session.createdAt.take(10)} · ${session.answers.size} réponses",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight,null)}
        }}
    }
}}
@Composable fun SettingsScreen(state:UiState,a:UiActions){var url by remember(state.apiUrl){mutableStateOf(state.apiUrl)};var delete by remember{mutableStateOf(false)};var deletePassword by remember{mutableStateOf("")};var deleteProject by remember{mutableStateOf<Project?>(null)};Page{
    Text("Votre espace",style=MaterialTheme.typography.displaySmall)
    SoftCard(Modifier.fillMaxWidth()){Text(state.name.ifBlank{if(state.signedIn)"Votre compte"else"Bienvenue dans Parole"},style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(5.dp));Text(state.email.ifBlank{"Préparez votre prochain oral à votre rythme."},color=MaterialTheme.colorScheme.onSurfaceVariant);if(!state.signedIn){Spacer(Modifier.height(16.dp));PrimaryButton("Se connecter",{a.navigate(Screen.AUTH)})}}
    SectionLabel("À votre image")
    SoftCard(Modifier.fillMaxWidth()){
        ChoiceRow("Apparence",listOf("Système","Clair","Sombre"),when(state.appearance){Appearance.SYSTEM->"Système";Appearance.LIGHT->"Clair";Appearance.DARK->"Sombre"}){a.appearance(when(it){"Clair"->Appearance.LIGHT;"Sombre"->Appearance.DARK;else->Appearance.SYSTEM})}
        TinyDivider();SettingRow("Lire les questions","Voix française de votre téléphone",Icons.AutoMirrored.Rounded.VolumeUp){Switch(state.speechEnabled,a.speech)}
    }
    SectionLabel("Connexion")
    SoftCard(Modifier.fillMaxWidth()){
        Text("Service Parole",style=MaterialTheme.typography.titleMedium);Spacer(Modifier.height(10.dp));OutlinedTextField(url,{url=it},label={Text("Adresse du serveur HTTPS")},modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Uri),shape=RoundedCornerShape(14.dp))
        Row{TextButton(onClick={a.configure(url)}){Text("Enregistrer")};TextButton(onClick=a.serverCheck,enabled=!state.busy){Text("Tester")}}
        Text(state.serverStatus,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(!state.configured)Text("L’activation de Firebase et du serveur est nécessaire pour les séances IA.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    SectionLabel("Confidentialité")
    SoftCard(Modifier.fillMaxWidth()){
        SettingRow("Choix publicitaires",icon=Icons.Rounded.PrivacyTip,onClick=a.privacy){Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight,null)}
        TinyDivider();SettingRow("Traitement par l’IA","Autoriser l’envoi de vos documents et réponses."){Switch(state.mediaConsent,a.consent)}
        Spacer(Modifier.height(8.dp));Text("Vos fichiers audio restent privés sur ce téléphone. Ils sont envoyés temporairement au serveur et à Groq pour transcription. Le texte et les bilans sont conservés dans votre compte Firebase. Aucun contenu de rapport n’est envoyé à AdMob.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if(state.projects.isNotEmpty()){
        SectionLabel("Mes documents")
        SoftCard(Modifier.fillMaxWidth()){state.projects.forEach{p->SettingRow(p.title,"${p.pageCount} pages"){IconButton(onClick={deleteProject=p}){Icon(Icons.Rounded.DeleteOutline,"Supprimer ${p.title}",tint=MaterialTheme.colorScheme.error)}}}}
    }
    if(state.signedIn){OutlinedButton(onClick=a.signOut,enabled=!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp)){Text("Se déconnecter")};TextButton(onClick={deletePassword="";delete=true},modifier=Modifier.fillMaxWidth()){Text("Supprimer mon compte et mes données",color=MaterialTheme.colorScheme.error)}}
    Text("Parole · 1.0.0\nUn espace pour trouver votre voix.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
}
    if(delete)AlertDialog(onDismissRequest={delete=false},title={Text("Supprimer votre compte ?")},text={Column{Text("Vos projets et bilans seront définitivement supprimés. Confirmez avec votre mot de passe.");Spacer(Modifier.height(16.dp));OutlinedTextField(deletePassword,{deletePassword=it},label={Text("Mot de passe")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password))}},confirmButton={TextButton(enabled=deletePassword.isNotBlank(),onClick={delete=false;a.deleteAccount(deletePassword);deletePassword=""}){Text("Supprimer")}},dismissButton={TextButton(onClick={delete=false}){Text("Annuler")}})
    deleteProject?.let{p->AlertDialog(onDismissRequest={deleteProject=null},title={Text("Supprimer ce projet ?")},text={Text("Le rapport et les séances associées seront supprimés.")},confirmButton={TextButton(onClick={deleteProject=null;a.deleteProject(p)}){Text("Supprimer")}},dismissButton={TextButton(onClick={deleteProject=null}){Text("Annuler")}})}
}
@Composable fun AuthScreen(state:UiState,a:UiActions){var register by remember{mutableStateOf(false)};var name by remember{mutableStateOf("")};var email by remember{mutableStateOf("")};var password by remember{mutableStateOf("")};var visible by remember{mutableStateOf(false)};Page{
    BackTitle(if(register)"Votre nouvelle voix"else"Heureux de vous revoir",{a.navigate(Screen.HOME)})
    Text("Un espace personnel pour vos rapports, vos entraînements et vos progrès.",color=MaterialTheme.colorScheme.onSurfaceVariant)
    if(register)OutlinedTextField(name,{name=it.take(60)},label={Text("Prénom")},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(18.dp))
    OutlinedTextField(email,{email=it.take(254)},label={Text("Adresse e-mail")},modifier=Modifier.fillMaxWidth().testTag("email"),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),shape=RoundedCornerShape(18.dp))
    OutlinedTextField(password,{password=it},label={Text("Mot de passe")},modifier=Modifier.fillMaxWidth().testTag("password"),singleLine=true,visualTransformation=if(visible)VisualTransformation.None else PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),shape=RoundedCornerShape(18.dp),trailingIcon={IconButton(onClick={visible=!visible}){Icon(if(visible)Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,if(visible)"Masquer le mot de passe"else"Afficher le mot de passe")}})
    PrimaryButton(if(register)"Créer mon compte"else"Me connecter",{a.login(name,email,password,register)},enabled=!state.busy&&email.contains('@')&&password.length>=(if(register)8 else 1)&&(!register||name.isNotBlank()))
    TextButton(onClick={register=!register},modifier=Modifier.fillMaxWidth()){Text(if(register)"J’ai déjà un compte"else"Créer un compte")}
    if(!register)TextButton(onClick={a.resetPassword(email)},enabled=email.contains('@'),modifier=Modifier.fillMaxWidth()){Text("Mot de passe oublié ?")}
    if(!state.configured)Text("Le service cloud de cette version attend encore son activation. Vos identifiants ne seront pas envoyés tant que Firebase n’est pas configuré.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
}}
