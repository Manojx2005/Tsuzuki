package eu.kanade.tachiyomi.tsuzuki.tts

import android.app.ActivityManager
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.RemoteException
import eu.kanade.tachiyomi.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.DataOutputStream
import java.io.File
import java.io.IOException

internal fun neuralTtsDebugPreference() = Injekt.get<PreferenceStore>()
    .getBoolean(Preference.appStateKey("tsz_neural_tts_debug"), false)

internal fun isNeuralTtsWorker(context: Context): Boolean {
    if (BuildConfig.BUILD_TYPE != "debug") return false
    val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Application.getProcessName()
    } else {
        context.getSystemService(ActivityManager::class.java)
            ?.runningAppProcesses?.firstOrNull { it.pid == Process.myPid() }?.processName
    }
    return processName == "${context.packageName}:neural_tts"
}

internal object NeuralTtsProtocol {
    const val PLAY = 1
    const val STOP = 2
    const val CHUNK_STARTED = 3
    const val CHAPTER_DONE = 4
    const val FAILED = 5
    const val CHUNK_DONE = 6
    const val UNCOVERED = 7
    const val CHUNKS = "chunks"
    const val SPEED = "speed"
    const val CHUNK_LENGTH = 180
    const val SERVICE = "eu.kanade.tachiyomi.tsuzuki.tts.debug.KokoroTtsService"
}

private const val BIND_TIMEOUT_MS = 30_000L

/**
 * The native runtime lives in a private debug process. If native model loading terminates that
 * process, the reader can still fall back to Android TTS at the interrupted chunk.
 */
internal class NeuralTtsClient private constructor(
    context: Context,
    private val scope: CoroutineScope,
) : NeuralPlayer {
    private val context = context.applicationContext
    private var server: Messenger? = null
    private var connecting: CompletableDeferred<Messenger>? = null
    private var bound = false
    private var nextId = 0
    private var sender: Job? = null
    private var watchdog: Job? = null

    /** True until the worker process has answered once: it must start, hash the pack and load the model first. */
    private var cold = true

    private fun watch(playing: Playback, chars: Int) {
        watchdog?.cancel()
        val wait = neuralWatchdogMs(chars, playing.speed, cold)
        watchdog = scope.launch {
            delay(wait)
            fail(playing, playing.index)
        }
    }

    private fun length(playing: Playback, index: Int) = playing.chunks.getOrNull(index)?.length ?: 0

    @Volatile private var active: Playback? = null
    override val isPlaying: Boolean get() = active != null

    private class Playback(
        val id: Int,
        var index: Int,
        val chunks: List<String>,
        val speed: Float,
        val listener: NeuralListener,
    ) {
        val lastIndex: Int get() = chunks.lastIndex
    }

    private val receiver = Messenger(
        Handler(Looper.getMainLooper()) { message ->
            val playing = active
            if (playing != null && message.arg1 == playing.id) {
                when (message.what) {
                    NeuralTtsProtocol.CHUNK_STARTED -> {
                        if (message.arg2 !in playing.index..playing.lastIndex) return@Handler true
                        cold = false
                        playing.index = message.arg2
                        // Playing this chunk while the next one is synthesized.
                        watch(playing, length(playing, playing.index) + length(playing, playing.index + 1))
                        playing.listener.onStart(playing.index)
                    }
                    NeuralTtsProtocol.CHUNK_DONE -> {
                        if (message.arg2 == playing.index) {
                            watch(playing, length(playing, playing.index + 1))
                            playing.listener.onChunkDone(message.arg2)
                        }
                    }
                    NeuralTtsProtocol.CHAPTER_DONE -> {
                        active = null
                        watchdog?.cancel()
                        playing.listener.onDone()
                    }
                    NeuralTtsProtocol.UNCOVERED -> {
                        if (message.arg2 !in playing.index..playing.lastIndex) return@Handler true
                        cold = false
                        active = null
                        watchdog?.cancel()
                        watchdog = null
                        playing.listener.onUncovered(message.arg2)
                    }
                    NeuralTtsProtocol.FAILED -> fail(playing, message.arg2)
                }
            }
            true
        },
    )

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            if (!bound) return
            val messenger = Messenger(binder)
            server = messenger
            connecting?.complete(messenger)
        }

        override fun onServiceDisconnected(name: ComponentName) = disconnected()
        override fun onBindingDied(name: ComponentName) = disconnected()
        override fun onNullBinding(name: ComponentName) = disconnected()
    }

    override fun play(chunks: List<String>, startIndex: Int, speed: Float, listener: NeuralListener) {
        require(startIndex in chunks.indices)
        require(chunks.size - startIndex <= 50_000)
        stop()
        val playing = Playback(++nextId, startIndex, chunks, speed, listener)
        active = playing
        watch(playing, length(playing, startIndex))
        sender = scope.launch {
            try {
                val messenger = connect()
                withContext(Dispatchers.IO) {
                    val text = File.createTempFile("tsuzuki-neural-", ".tts", context.cacheDir)
                    try {
                        DataOutputStream(text.outputStream().buffered()).use { output ->
                            output.writeInt(chunks.size - startIndex)
                            for (index in startIndex until chunks.size) {
                                currentCoroutineContext().ensureActive()
                                output.writeUTF(chunks[index])
                            }
                        }
                        withContext(Dispatchers.Main) send@{
                            if (active !== playing) return@send
                            ParcelFileDescriptor.open(text, ParcelFileDescriptor.MODE_READ_ONLY).use { input ->
                                val message = Message.obtain(null, NeuralTtsProtocol.PLAY, playing.id, startIndex)
                                message.replyTo = receiver
                                message.data = Bundle().apply {
                                    putParcelable(NeuralTtsProtocol.CHUNKS, input)
                                    putFloat(NeuralTtsProtocol.SPEED, speed)
                                }
                                messenger.send(message)
                            }
                        }
                    } finally {
                        text.delete()
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logcat(LogPriority.WARN, error) { "Neural TTS request failed" }
                fail(playing, playing.index)
            }
        }
    }

    override fun stop() {
        val playing = active
        active = null
        sender?.cancel()
        sender = null
        watchdog?.cancel()
        watchdog = null
        if (playing != null) {
            try {
                server?.send(Message.obtain(null, NeuralTtsProtocol.STOP, playing.id, 0).apply { replyTo = receiver })
            } catch (error: RemoteException) {
                logcat(LogPriority.WARN, error) { "Neural TTS service disconnected during stop" }
            }
        }
    }

    override fun close(kill: Boolean) {
        stop()
        unbind()
        if (kill) killWorker()
    }

    /** A hung native call keeps the worker's runtime lock, so a retry must start a new process. */
    private fun killWorker() {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return
        val name = "${context.packageName}:neural_tts"
        manager.runningAppProcesses.orEmpty().filter { it.processName == name }.forEach {
            Process.killProcess(it.pid)
        }
        cold = true
    }

    private suspend fun connect(): Messenger {
        server?.let { return it }
        val pending = connecting ?: CompletableDeferred<Messenger>().also {
            connecting = it
            cold = true
            val intent = Intent().setClassName(context, NeuralTtsProtocol.SERVICE)
            bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            if (!bound) {
                connecting = null
                throw IOException("Neural TTS service is unavailable")
            }
        }
        return withTimeoutOrNull(BIND_TIMEOUT_MS) { pending.await() } ?: run {
            unbind()
            throw IOException("Neural TTS service did not connect")
        }
    }

    private fun disconnected() {
        val playing = active
        unbind()
        if (playing != null) fail(playing, playing.index)
    }

    private fun unbind() {
        if (bound) context.unbindService(connection)
        bound = false
        server = null
        connecting?.completeExceptionally(IOException("Neural TTS service disconnected"))
        connecting = null
    }

    private fun fail(playing: Playback, index: Int) {
        if (active !== playing) return
        stop()
        playing.listener.onFailure(index.coerceIn(0, playing.lastIndex))
    }

    companion object {
        fun createIfEnabled(context: Context, scope: CoroutineScope): NeuralTtsClient? =
            if (BuildConfig.BUILD_TYPE == "debug" && Build.SUPPORTED_ABIS.firstOrNull() == "arm64-v8a" &&
                neuralTtsDebugPreference().get()
            ) {
                NeuralTtsClient(context, scope)
            } else {
                null
            }
    }
}
