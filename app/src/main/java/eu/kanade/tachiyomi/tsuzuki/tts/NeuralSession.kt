package eu.kanade.tachiyomi.tsuzuki.tts

/** Events from a neural playback, always delivered on the main thread. */
internal interface NeuralListener {
    fun onStart(index: Int)
    fun onChunkDone(index: Int)
    fun onDone()

    /** The voice pack cannot speak this chunk. Playback has ended; the chunk is not a failure. */
    fun onUncovered(index: Int)

    /** The worker failed or stalled at [index]. */
    fun onFailure(index: Int)
}

internal interface NeuralPlayer {
    val isPlaying: Boolean
    fun play(chunks: List<String>, startIndex: Int, speed: Float, listener: NeuralListener)
    fun stop()

    /** Stop and release the worker; [kill] also ends its process so a hung native call cannot block the next request. */
    fun close(kill: Boolean = false)
}

/** Allowance for the worker process to start, hash the pack and load the model before its first answer. */
internal const val NEURAL_COLD_START_MS = 120_000L
private const val NEURAL_EVENT_BASE_MS = 20_000L
private const val NEURAL_MS_PER_CHAR_AT_1X = 200L

/**
 * How long to wait for the next worker event: a fixed margin plus time proportional to the text to be played or
 * synthesized (slower speeds play longer), plus [NEURAL_COLD_START_MS] while the worker process is still cold.
 */
internal fun neuralWatchdogMs(chars: Int, speed: Float, cold: Boolean): Long {
    val scaled = (chars * NEURAL_MS_PER_CHAR_AT_1X / speed.coerceIn(0.5f, 2f)).toLong()
    return NEURAL_EVENT_BASE_MS + scaled + if (cold) NEURAL_COLD_START_MS else 0L
}

/**
 * Keeps the Kokoro worker and the phone voice in step for one reader. Uncovered chunks are spoken by the phone voice
 * (one chunk, same queue and utterance ids) and neural playback resumes at the next chunk; a real failure is retried
 * once per chapter on a fresh worker, then the rest of the chapter uses the phone voice.
 */
internal class NeuralSession(
    private val newPlayer: () -> NeuralPlayer?,
    /** Speak chunks [from]..[through] with the phone voice under [queue]'s utterance ids. */
    private val speakPhone: (queue: Long, from: Int, through: Int) -> Unit,
    private val onStart: (queue: Long, index: Int) -> Unit,
    private val onChunkDone: (queue: Long, index: Int) -> Unit,
) {
    private class Resume(val queue: Long, val index: Int, val chunks: List<String>, val speed: Float)

    private var player: NeuralPlayer? = null
    private var retried = false
    private var phoneOnly = false
    private var resume: Resume? = null

    val isPlaying: Boolean get() = player?.isPlaying == true

    /** A new chapter gets a fresh retry and a fresh chance at the neural voice. */
    fun beginChapter() {
        retried = false
        phoneOnly = false
    }

    /** True when neural playback was started; false means the caller must use the phone voice. */
    fun start(queue: Long, chunks: List<String>, startIndex: Int, speed: Float): Boolean {
        stop()
        if (phoneOnly || startIndex !in chunks.indices) return false
        if (chunks.drop(startIndex).any { it.length > NeuralTtsProtocol.CHUNK_LENGTH }) return false
        val active = player ?: newPlayer()?.also { player = it } ?: return false
        begin(active, queue, chunks, startIndex, speed)
        return true
    }

    private fun begin(active: NeuralPlayer, queue: Long, chunks: List<String>, startIndex: Int, speed: Float) {
        active.play(
            chunks,
            startIndex,
            speed,
            object : NeuralListener {
                override fun onStart(index: Int) = this@NeuralSession.onStart(queue, index)
                override fun onChunkDone(index: Int) = this@NeuralSession.onChunkDone(queue, index)
                override fun onDone() = Unit
                override fun onUncovered(index: Int) {
                    if (index < chunks.lastIndex) resume = Resume(queue, index + 1, chunks, speed)
                    speakPhone(queue, index, index)
                }
                override fun onFailure(index: Int) {
                    player?.close(kill = true)
                    player = null
                    val fresh = if (!retried) newPlayer() else null
                    if (fresh != null) {
                        retried = true
                        player = fresh
                        begin(fresh, queue, chunks, index, speed)
                    } else {
                        phoneOnly = true
                        speakPhone(queue, index, chunks.lastIndex)
                    }
                }
            },
        )
    }

    /** The phone voice finished [index] of [queue]: continue neural playback after an uncovered chunk. */
    fun phoneChunkDone(queue: Long, index: Int) {
        val pending = resume ?: return
        if (pending.queue != queue || pending.index != index + 1) return
        resume = null
        if (!start(queue, pending.chunks, pending.index, pending.speed)) {
            speakPhone(queue, pending.index, pending.chunks.lastIndex)
        }
    }

    fun stop() {
        resume = null
        player?.stop()
    }

    fun close() {
        stop()
        player?.close()
        player = null
    }
}
