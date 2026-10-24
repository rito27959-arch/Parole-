package app.parole.voice

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import java.io.File
import java.util.Locale

class VoiceEngine(private val context:Context) {
    private var recorder:MediaRecorder?=null
    private var player:MediaPlayer?=null
    private var tts:TextToSpeech?=null
    private var ttsReady=false
    private var startedAt=0L
    private var accumulated=0L
    private var paused=false
    var currentFile:File?=null;private set
    init {tts=TextToSpeech(context){status->ttsReady=status==TextToSpeech.SUCCESS;if(ttsReady)tts?.language=Locale.FRENCH}}
    fun speak(text:String){if(ttsReady){player?.release();player=null;tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"parole-question")}}
    fun silence(){tts?.stop();player?.release();player=null}
    @Suppress("DEPRECATION")
    fun start(sessionId:String,index:Int) {
        silence();cancel()
        val dir=File(context.filesDir,"recordings").apply{mkdirs()}
        currentFile=File(dir,"${sessionId}_${index}_${System.currentTimeMillis()}.m4a")
        val device=if(Build.VERSION.SDK_INT>=31)MediaRecorder(context)else MediaRecorder()
        try {
            device.setAudioSource(MediaRecorder.AudioSource.MIC);device.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            device.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);device.setAudioSamplingRate(16000);device.setAudioEncodingBitRate(64000)
            device.setOutputFile(currentFile!!.absolutePath);device.prepare();device.start();recorder=device
            startedAt=SystemClock.elapsedRealtime();accumulated=0;paused=false
        }catch(e:Exception){device.release();currentFile?.delete();throw e}
    }
    fun amplitude():Float=if(paused)0f else runCatching{(recorder?.maxAmplitude?:0)/32767f}.getOrDefault(0f)
    fun elapsed():Long=accumulated+if(recorder!=null&&!paused)SystemClock.elapsedRealtime()-startedAt else 0
    fun pause(){if(recorder!=null&&!paused){recorder?.pause();accumulated+=SystemClock.elapsedRealtime()-startedAt;paused=true}}
    fun resume(){if(recorder!=null&&paused){recorder?.resume();startedAt=SystemClock.elapsedRealtime();paused=false}}
    fun stop():File? {val device=recorder?:return currentFile;accumulated=elapsed();recorder=null
        try{device.stop()}catch(e:RuntimeException){currentFile?.delete();currentFile=null;throw e}finally{device.release()};return currentFile}
    fun play(file:File,onDone:()->Unit={}){silence();player=MediaPlayer().apply{setDataSource(file.absolutePath);setOnCompletionListener{it.release();player=null;onDone()};prepare();start()}}
    fun cancel(){runCatching{recorder?.stop()};recorder?.release();recorder=null}
    fun close(){cancel();silence();tts?.shutdown();tts=null}
}
