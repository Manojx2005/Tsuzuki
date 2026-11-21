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
import android.os.SystemClock
import androidx.core.os.BundleCompat
import com.k2fsa.sherpa.onnx.GeneratedAudio
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import eu.kanade.tachiyomi.tsuzuki.tts.playNeuralChunks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
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
    private var vocabulary: Set<String> = emptySet()
    private var playback: Job? = null
    private var client: Messenger? = null
    private var request = 0

    /**
     * Audio being prepared for the next request's first piece while the phone voice speaks a chunk Kokoro can't
     * say (keyed by speed and text). Removes the ~2.6 s silence before every Kokoro chunk after a phone-voice
     * chunk on the Tab S11 (2026-11-21). Main thread only.
     */
    private var prefetch: Pair<String, Deferred<GeneratedAudio?>>? = null
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
        val requestedAt = SystemClock.elapsedRealtime()
        var firstAudioLogged = false
        playback = scope.launch {
            try {
                val chunks = ParcelFileDescriptor.AutoCloseInputStream(input).use { stream ->
                    withContext(Dispatchers.IO) {
                        val data = DataInputStream(stream.buffered())
                        val count = data.readInt()
                        require(count in 1..50_000 && first >= 0)
                        List(count) { data.readUTF().also { require(it.length in 1..Protocol.CHUNK_LENGTH) } }
                    }
                }
                playNeuralChunks(
                    chunks = chunks,
                    firstIndex = first,
                    synthesize = { synthesizeOrPrefetched(it, speed) },
                    covered = ::isCovered,
                    play = ::playAudio,
                    onStart = {
                        if (!firstAudioLogged) {
                            firstAudioLogged = true
                            logcat(LogPriority.INFO) {
                                "KokoroTiming firstAudio chunk=$it afterRequestMs=${SystemClock.elapsedRealtime() - requestedAt}"
                            }
                        }
                        notify(receiver, Protocol.CHUNK_STARTED, id, it)
                    },
                    onDone = { notify(receiver, Protocol.CHAPTER_DONE, id, 0) },
                    onChunkDone = { notify(receiver, Protocol.CHUNK_DONE, id, it) },
                    onUncovered = {
                        logcat(LogPriority.INFO) { "Kokoro: chunk $it is not covered; the phone voice speaks it" }
                        notify(receiver, Protocol.UNCOVERED, id, it)
                        prepareAfterUncovered(chunks, it - first, speed)
                    },
                    onFailure = { index, error ->
                        logcat(LogPriority.WARN, error) { "Kokoro listening test failed at chunk $index" }
                        notify(receiver, Protocol.FAILED, id, index)
                    },
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: LinkageError) {
                logcat(LogPriority.WARN, error) { "Kokoro runtime is unavailable" }
                notify(receiver, Protocol.FAILED, id, first)
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

    /** Load the pack and model on first use. Call with [runtimeLock] held. */
    private suspend fun engine(): OfflineTts = tts ?: createKokoro().also { tts = it }

    private suspend fun isCovered(text: String): Boolean = withContext(Dispatchers.Default) {
        runtimeLock.withLock {
            engine()
            eu.kanade.tachiyomi.tsuzuki.tts.isKokoroTextCovered(text, vocabulary)
        }
    }

    private suspend fun synthesize(text: String, speed: Float): GeneratedAudio = withContext(Dispatchers.Default) {
        val synthesis = currentCoroutineContext()
        runtimeLock.withLock {
            val engine = engine()
            check(eu.kanade.tachiyomi.tsuzuki.tts.isKokoroTextCovered(text, vocabulary)) {
                "Phone voice required for uncovered text"
            }
            val started = SystemClock.elapsedRealtime()
            val result = engine.generateWithCallback(
                text = eu.kanade.tachiyomi.tsuzuki.tts.kokoroModelText(text),
                sid = 0,
                speed = speed,
                callback = KokoroProgressCallback { synthesis.isActive },
            )
            check(result.sampleRate > 0 && result.samples.isNotEmpty()) { "Kokoro returned no audio" }
            val synthMs = SystemClock.elapsedRealtime() - started
            val audioMs = result.samples.size * 1_000L / result.sampleRate
            logcat(LogPriority.INFO) {
                "KokoroTiming synth chars=${text.length} speed=$speed synthMs=$synthMs audioMs=$audioMs " +
                    "rtf=${"%.2f".format(synthMs.toDouble() / audioMs.coerceAtLeast(1))}"
            }
            result
        }
    }

    private suspend fun synthesizeOrPrefetched(text: String, speed: Float): GeneratedAudio {
        val key = prefetchKey(text, speed)
        val pending = prefetch
        prefetch = null
        if (pending != null) {
            if (pending.first == key) {
                val audio = try {
                    pending.second.await()
                } catch (error: CancellationException) {
                    currentCoroutineContext().ensureActive()
                    null
                } catch (error: Exception) {
                    logcat(LogPriority.WARN, error) { "Kokoro prefetch failed; synthesizing again" }
                    null
                }
                if (audio != null) {
                    logcat(LogPriority.INFO) { "KokoroTiming prefetch hit chars=${text.length}" }
                    return audio
                }
            } else {
                pending.second.cancel()
            }
        }
        return synthesize(text, speed)
    }

    private fun prepareAfterUncovered(chunks: List<String>, uncoveredOffset: Int, speed: Float) {
        val (chunk, text) = eu.kanade.tachiyomi.tsuzuki.tts.nextNeuralPrefetch(chunks, uncoveredOffset) ?: return
        prefetch?.second?.cancel()
        prefetch = prefetchKey(text, speed) to scope.async {
            if (isCovered(chunk)) synthesize(text, speed) else null
        }
    }

    private fun prefetchKey(text: String, speed: Float) = "$speed|$text"

    private suspend fun createKokoro(): OfflineTts {
        val loadStarted = SystemClock.elapsedRealtime()
        val voicePack = eu.kanade.tachiyomi.tsuzuki.tts.KokoroPack(this)
        vocabulary = voicePack.verify()
        val verified = SystemClock.elapsedRealtime()
        val pack = voicePack.directory
        require(!File(pack, "espeak-ng-data").exists()) { "Only the lexicon-only pack is supported" }
        fun packFile(name: String): String {
            val file = File(pack, name)
            require(file.isFile && file.length() > 0L && file.canonicalFile.parentFile == pack.canonicalFile) {
                "Missing or invalid Kokoro file: $name"
            }
            return file.absolutePath
        }

        val loaded = OfflineTts(
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
        val ready = SystemClock.elapsedRealtime()
        logcat(LogPriority.INFO) {
            "KokoroTiming load verifyMs=${verified - loadStarted} modelMs=${ready - verified} " +
                "processUptimeMs=${SystemClock.elapsedRealtime() - android.os.Process.getStartElapsedRealtime()}"
        }
        return loaded
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

/**
 * The sherpa-onnx JNI calls `invoke([F)Ljava/lang/Integer;` on the callback object. A Kotlin lambda compiles to a
 * synthetic class with only the erased `invoke(Object)`, so the native lookup fails and ART aborts the process
 * (seen on the Tab S11, 2026-11-15). A named class keeps the typed method. Return 1 to continue, 0 to stop.
 */
private class KokoroProgressCallback(private val active: () -> Boolean) : (FloatArray) -> Int {
    override fun invoke(samples: FloatArray): Int = if (active()) 1 else 0
}
