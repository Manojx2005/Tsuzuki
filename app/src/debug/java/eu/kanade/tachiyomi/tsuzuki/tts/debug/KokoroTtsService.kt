package eu.kanade.tachiyomi.tsuzuki.tts.debug

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import androidx.core.os.BundleCompat
import com.k2fsa.sherpa.onnx.GeneratedAudio
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import eu.kanade.tachiyomi.tsuzuki.tts.playNeuralChunks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.DataInputStream
import java.io.File
import eu.kanade.tachiyomi.tsuzuki.tts.NeuralTtsProtocol as Protocol

class KokoroTtsService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val runtimeLock = Mutex()
    private var tts: OfflineTts? = null
    private var playback: Job? = null
    private var client: Messenger? = null
    private var request = 0
    private val incoming = Messenger(
        Handler(Looper.getMainLooper()) { message ->
            when (message.what) {
                Protocol.PLAY -> start(message)
                Protocol.STOP -> if (message.replyTo == client && message.arg1 == request) stop()
            }
            true
        },
    )

    override fun onBind(intent: Intent): IBinder = incoming.binder

    override fun onUnbind(intent: Intent): Boolean {
        stop()
        return false
    }

    override fun onDestroy() {
        stop()
        scope.cancel()
        CoroutineScope(Dispatchers.Default).launch {
            runtimeLock.withLock {
                tts?.release()
                tts = null
            }
        }
        super.onDestroy()
    }

    private fun start(message: Message) {
        val receiver = message.replyTo ?: return
        val id = message.arg1
        val first = message.arg2
        val speed = message.data.getFloat(Protocol.SPEED, 1f)
        val input = BundleCompat.getParcelable(message.data, Protocol.CHUNKS, ParcelFileDescriptor::class.java)
        if (input == null) {
            notify(receiver, Protocol.FAILED, id, first)
            return
        }
        stop()
        request = id
        client = receiver
        playback = scope.launch {
            try {
                val chunks = ParcelFileDescriptor.AutoCloseInputStream(input).use { stream ->
                    withContext(Dispatchers.IO) {
                        val data = DataInputStream(stream.buffered())
                        val count = data.readInt()
                        require(count in 1..1_000_000)
                        List(count) { data.readUTF() }
                    }
                }
                playNeuralChunks(
                    chunks = chunks,
                    firstIndex = first,
                    synthesize = { synthesize(it, speed) },
                    play = ::playAudio,
                    onStart = { notify(receiver, Protocol.CHUNK_STARTED, id, it) },
                    onDone = { notify(receiver, Protocol.CHAPTER_DONE, id, 0) },
                    onFailure = { index, error ->
                        logcat(LogPriority.WARN, error) { "Kokoro listening test failed at chunk $index" }
                        notify(receiver, Protocol.FAILED, id, index)
                    },
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logcat(LogPriority.WARN, error) { "Could not read the chapter for Kokoro" }
                notify(receiver, Protocol.FAILED, id, first)
            }
        }
    }

    private fun stop() {
        playback?.cancel()
        playback = null
        client = null
    }

    private fun notify(receiver: Messenger, event: Int, id: Int, index: Int) {
        try {
            receiver.send(Message.obtain(null, event, id, index))
        } catch (error: RemoteException) {
            logcat(LogPriority.WARN, error) { "Neural TTS client disconnected" }
            stop()
        }
    }

    private suspend fun synthesize(text: String, speed: Float): GeneratedAudio = withContext(Dispatchers.Default) {
        val synthesis = currentCoroutineContext()
        runtimeLock.withLock {
            val engine = tts ?: createKokoro().also { tts = it }
            val result = engine.generateWithCallback(text = text, sid = 0, speed = speed) {
                if (synthesis.isActive) 1 else 0
            }
            check(result.sampleRate > 0 && result.samples.isNotEmpty()) { "Kokoro returned no audio" }
            result
        }
    }

    private fun createKokoro(): OfflineTts {
        val pack = File(filesDir, "tts/kokoro-int8-multi-lang-v1_1")
        require(!File(pack, "espeak-ng-data").exists()) { "Only the lexicon-only pack is supported" }
        fun packFile(name: String): String {
            val file = File(pack, name)
            require(file.isFile && file.length() > 0L && file.canonicalFile.parentFile == pack.canonicalFile) {
                "Missing or invalid Kokoro file: $name"
            }
            return file.absolutePath
        }

        return OfflineTts(
            config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    kokoro = OfflineTtsKokoroModelConfig(
                        model = packFile("model.int8.onnx"),
                        voices = packFile("voices.bin"),
                        tokens = packFile("tokens.txt"),
                        lexicon = packFile("lexicon-us-en.txt"),
                    ),
                    numThreads = 2,
                ),
            ),
        )
    }
}

private suspend fun playAudio(audio: GeneratedAudio) = withContext(Dispatchers.IO) {
    val samples = audio.samples
    val track = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setSampleRate(audio.sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
        )
        .setTransferMode(AudioTrack.MODE_STATIC)
        .setBufferSizeInBytes(samples.size * Float.SIZE_BYTES)
        .build()
    try {
        check(track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING) == samples.size)
        track.play()
        val duration = samples.size * 1_000L / audio.sampleRate
        val finished = withTimeoutOrNull(duration + 5_000L) {
            while (track.playbackHeadPosition < samples.size) delay(25)
            true
        }
        check(finished == true) { "Kokoro audio output stalled" }
    } finally {
        track.release()
    }
}
