package app.parole.data

import android.content.Context
import android.net.Uri
import app.parole.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(val status:Int,val apiCode:String,message:String):IOException(message)
class ParoleRepository(private val context:Context) {
    val json=Json {ignoreUnknownKeys=true;encodeDefaults=true}
    private val prefs=context.getSharedPreferences("parole",Context.MODE_PRIVATE)
    val welcomeCompleted get()=prefs.getBoolean("welcome_completed_v1",false)
    val preferredLevel get()=prefs.getString("preferred_level","BTS")?:"BTS"
    val preferredJury get()=prefs.getString("preferred_jury","Bienveillant")?:"Bienveillant"
    fun finishWelcome(level:String,jury:String){prefs.edit().putBoolean("welcome_completed_v1",true).putString("preferred_level",level).putString("preferred_jury",jury).apply()}
    private val client=OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(90,TimeUnit.SECONDS).callTimeout(110,TimeUnit.SECONDS).build()
    val auth:FirebaseAuth? get()=if(FirebaseApp.getApps(context).isNotEmpty()) FirebaseAuth.getInstance() else null
    var apiUrl:String
        get()=prefs.getString("api_url",BuildConfig.API_BASE_URL).orEmpty().trimEnd('/')
        set(value){prefs.edit().putString("api_url",value.trim().trimEnd('/')).apply()}
    var appearance:Appearance
        get()=runCatching{Appearance.valueOf(prefs.getString("appearance","SYSTEM")!!)}.getOrDefault(Appearance.SYSTEM)
        set(value){prefs.edit().putString("appearance",value.name).apply()}
    var speechEnabled:Boolean
        get()=prefs.getBoolean("speech_enabled",true)
        set(value){prefs.edit().putBoolean("speech_enabled",value).apply()}
    var mediaConsent:Boolean
        get()=prefs.getBoolean("media_consent",false)
        set(value){prefs.edit().putBoolean("media_consent",value).apply()}
    val configured get()=auth!=null&&apiUrl.startsWith("https://")
    private fun cacheFile()=File(context.filesDir,"cache-${auth?.currentUser?.uid?:"guest"}.json")
    suspend fun readCache():Cache=withContext(Dispatchers.IO){runCatching{json.decodeFromString<Cache>(cacheFile().readText())}.getOrDefault(Cache())}
    suspend fun cache(projects:List<Project>,sessions:List<Session>)=withContext(Dispatchers.IO){val target=cacheFile();val temp=File(target.path+".tmp");temp.writeText(json.encodeToString(Cache(projects,sessions)));check(temp.renameTo(target))}
    fun saveDraft(id:String,text:String){prefs.edit().putString("draft_$id",text).apply()}
    fun draft(id:String)=prefs.getString("draft_$id","").orEmpty()
    fun clearDraft(id:String){prefs.edit().remove("draft_$id").apply()}
    private suspend fun body(path:String,method:String="GET",data:RequestBody?=null,authenticated:Boolean=true):String {
        if(!apiUrl.startsWith("https://"))throw ApiException(503,"NOT_CONFIGURED","Ajoutez l’adresse HTTPS du serveur dans Réglages.")
        val token=if(authenticated)auth?.currentUser?.getIdToken(false)?.await()?.token?:throw ApiException(401,"UNAUTHENTICATED","Connectez-vous pour continuer.") else null
        return withContext(Dispatchers.IO){
            val builder=Request.Builder().url(apiUrl+path).header("Accept","application/json")
            if(token!=null)builder.header("Authorization","Bearer $token")
            builder.method(method,if(method in listOf("POST","PUT"))data?:ByteArray(0).toRequestBody() else data)
            client.newCall(builder.build()).execute().use{response->
                val text=response.body?.string().orEmpty()
                if(!response.isSuccessful){val e=runCatching{json.decodeFromString<ApiFailure>(text)}.getOrNull();throw ApiException(response.code,e?.code?:"HTTP_ERROR",e?.error?:"Le serveur est indisponible (${response.code}).")}
                text
            }
        }
    }
    private fun jsonBody(text:String)=text.toRequestBody("application/json; charset=utf-8".toMediaType())
    suspend fun signIn(email:String,password:String){requireAuth().signInWithEmailAndPassword(email.trim(),password).await()}
    suspend fun signUp(name:String,email:String,password:String){val user=requireAuth().createUserWithEmailAndPassword(email.trim(),password).await().user?:error("Compte non créé");user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()}
    suspend fun resetPassword(email:String){requireAuth().sendPasswordResetEmail(email.trim()).await()}
    private fun requireAuth()=auth?:throw ApiException(503,"NOT_CONFIGURED","Firebase n’est pas encore configuré dans cette version.")
    suspend fun signOut(){withContext(Dispatchers.IO){cacheFile().delete();File(context.filesDir,"recordings").deleteRecursively()};prefs.edit().apply{prefs.all.keys.filter{it.startsWith("draft_")}.forEach{remove(it)}}.apply();auth?.signOut()}
    suspend fun deleteAccount(password:String){
        val user=requireAuth().currentUser?:throw ApiException(401,"UNAUTHENTICATED","Reconnectez-vous pour continuer.")
        val email=user.email?:throw ApiException(401,"UNAUTHENTICATED","Reconnectez-vous pour continuer.")
        user.reauthenticate(EmailAuthProvider.getCredential(email,password)).await()
        user.getIdToken(true).await()
        body("/v1/account","DELETE")
        signOut()
    }
    suspend fun initializeCloud() {
        if(auth!=null||!apiUrl.startsWith("https://"))return
        val config=json.decodeFromString<ServerConfig>(body("/config",authenticated=false)).firebase?:return
        FirebaseApp.initializeApp(context,FirebaseOptions.Builder().setApiKey(config.apiKey).setApplicationId(config.applicationId).setProjectId(config.projectId).build())
    }
    suspend fun projects()=json.decodeFromString<ProjectList>(body("/v1/projects")).projects
    suspend fun sessions()=json.decodeFromString<SessionList>(body("/v1/sessions")).sessions
    suspend fun session(id:String)=json.decodeFromString<Session>(body("/v1/sessions/$id"))
    suspend fun account()=json.decodeFromString<Account>(body("/v1/account"))
    suspend fun health()=body("/ready",authenticated=false)
    suspend fun importPdf(uri:Uri):Project {
        val bytes=withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)?.use{input->
            val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var total=0
            while(true){val count=input.read(buffer);if(count<0)break;total+=count;if(total>10*1024*1024)throw ApiException(413,"FILE_TOO_LARGE","Le fichier dépasse 10 Mo.");output.write(buffer,0,count)};output.toByteArray()
        }?:throw IOException("Impossible de lire ce fichier.")}
        val name=context.contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())it.getString(0)else null}?:"Rapport.pdf"
        val multipart=MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file",name,bytes.toRequestBody("application/pdf".toMediaType())).build()
        return json.decodeFromString(body("/v1/projects","POST",multipart))
    }
    suspend fun importText(title:String,text:String):Project=json.decodeFromString(body("/v1/projects","POST",jsonBody(json.encodeToString(TextProject(title,text)))))
    suspend fun deleteProject(id:String){
        val sessions=readCache().sessions.filter{it.projectId==id}
        body("/v1/projects/$id","DELETE")
        withContext(Dispatchers.IO){File(context.filesDir,"recordings").listFiles()?.filter{file->sessions.any{file.name.startsWith(it.id+"_")}}?.forEach{it.delete()}}
        sessions.forEach{clearDraft(it.id)}
    }
    suspend fun start(request:StartRequest):Session=json.decodeFromString(body("/v1/sessions","POST",jsonBody(json.encodeToString(request))))
    suspend fun answer(id:String,request:AnswerRequest):Session=json.decodeFromString(body("/v1/sessions/$id/answers","POST",jsonBody(json.encodeToString(request))))
    suspend fun transcribe(file:File):Transcript {
        val multipart=MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file","answer.m4a",file.asRequestBody("audio/mp4".toMediaType())).build()
        return json.decodeFromString(body("/v1/transcriptions","POST",multipart))
    }
    suspend fun rewardTicket():RewardTicket=json.decodeFromString(body("/v1/rewards/ticket","POST"))
}
