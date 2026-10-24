package app.parole.data

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable data class FirebasePublicConfig(val apiKey:String,val applicationId:String,val projectId:String)
@Serializable data class ServerConfig(val firebase:FirebasePublicConfig?=null)
@Serializable data class Project(val id:String,val title:String,val pageCount:Int=1,val createdAt:String="")
@Serializable data class Question(val question:String,val sourcePage:Int?=null,val focus:String="")
@Serializable data class Feedback(val strengths:List<String> = emptyList(),val improvements:List<String> = emptyList(),val clarity:Int=0,val structure:Int=0,val precision:Int=0,val evidence:String="")
@Serializable data class Answer(val requestId:String,val question:Question,val text:String,val durationMs:Long=0,val feedback:Feedback=Feedback(),val answeredAt:String="")
@Serializable data class Session(val id:String,val projectId:String,val level:String="BTS",val minutes:Int=10,val style:String="Bienveillant",val questionCount:Int=6,val rewarded:Boolean=false,val status:String="active",val answers:List<Answer> = emptyList(),val currentQuestion:Question?=null,val createdAt:String="",val completedAt:String?=null)
@Serializable data class Account(val freeRemaining:Int=0,val credits:Int=0,val rewardsEnabled:Boolean=false)
@Serializable data class ProjectList(val projects:List<Project> = emptyList())
@Serializable data class SessionList(val sessions:List<Session> = emptyList())
@Serializable data class Transcript(val text:String,val duration:Double?=null)
@Serializable data class RewardTicket(val id:String,val userId:String)
@Serializable data class ApiFailure(val error:String="Erreur inconnue",val code:String="")
@Serializable data class Cache(val projects:List<Project> = emptyList(),val sessions:List<Session> = emptyList())
@Serializable data class StartRequest(val projectId:String,val requestId:String,val level:String,val minutes:Int,val style:String,val questionCount:Int=6,val rewarded:Boolean=false)
@Serializable data class AnswerRequest(val requestId:String,val questionIndex:Int,val text:String,val durationMs:Long)
@Serializable data class TextProject(val title:String,val text:String)

enum class Appearance { SYSTEM, LIGHT, DARK }
enum class Screen { HOME, IMPORT, PREPARE, SESSION, REPORT, PROGRESS, SETTINGS, AUTH }
data class UiState(
    val screen:Screen=Screen.HOME,val appearance:Appearance=Appearance.SYSTEM,
    val name:String="",val email:String="",val signedIn:Boolean=false,val configured:Boolean=false,
    val projects:List<Project> = emptyList(),val sessions:List<Session> = emptyList(),val account:Account=Account(),
    val selectedProject:Project?=null,val session:Session?=null,val busy:Boolean=false,val error:String?=null,val notice:String?=null,
    val level:String="BTS",val minutes:Int=10,val jury:String="Bienveillant",val rewarded:Boolean=false,
    val draft:String="",val recording:Boolean=false,val paused:Boolean=false,val elapsedMs:Long=0,val amplitude:Float=0f,
    val apiUrl:String="",val serverStatus:String="Non vérifié",val speechEnabled:Boolean=true,val mediaConsent:Boolean=false,
    val lastAudio:String?=null,val replayAnswer:Int?=null,val offline:Boolean=false
)
object SessionMetrics {
    fun duration(session:Session)=session.answers.sumOf { it.durationMs }
    fun words(text:String)=text.trim().split(Regex("\\s+")).count {it.isNotBlank()}
    fun wordsPerMinute(session:Session):Int? {val ms=duration(session);return if(ms<1000)null else (session.answers.filter{it.durationMs>0}.sumOf{words(it.text)}*60000.0/ms).roundToInt()}
    fun mean(session:Session,metric:(Feedback)->Int):Double?=session.answers.map{metric(it.feedback)}.filter{it in 1..5}.takeIf{it.isNotEmpty()}?.average()
    fun label(score:Double?)=when {score==null->"—";score>=4->"Bonne";score>=3->"En progrès";else->"À renforcer"}
    fun clock(ms:Long):String {val seconds=(ms.coerceAtLeast(0)/1000);return "%02d:%02d".format(seconds/60,seconds%60)}
}
