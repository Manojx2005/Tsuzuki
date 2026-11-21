package eu.kanade.tachiyomi.tsuzuki.tts

import kotlinx.coroutines.Deferred

/**
 * The one piece of audio the Kokoro worker prepares while the phone voice speaks an uncovered chunk
 * (KOKORO-TRANSFER-F). It is kept only for the matching phone-to-neural continuation: [take] hands it to the
 * request whose first piece has the same key and cancels it for any other text, and [cancel] drops it on an
 * explicit stop or unbind. Main thread only.
 */
internal class NeuralPrefetchSlot<T> {
    private var pending: Pair<String, Deferred<T>>? = null

    val isPending: Boolean get() = pending != null

    /** Replaces (and cancels) anything prepared before. */
    fun prepare(key: String, audio: Deferred<T>) {
        pending?.second?.cancel()
        pending = key to audio
    }

    /** The prepared audio when it was made for [key]; otherwise it is cancelled and null is returned. */
    fun take(key: String): Deferred<T>? {
        val current = pending ?: return null
        pending = null
        if (current.first == key) return current.second
        current.second.cancel()
        return null
    }

    fun cancel() {
        pending?.second?.cancel()
        pending = null
    }
}

/**
 * Which request the reader must name in STOP (KOKORO-TRANSFER-F). After an UNCOVERED hand-off the reader no longer
 * has an active neural request, but the worker may still be preparing the continuation for it, so a stop while the
 * phone voice speaks must still reach that request. A new [started] request replaces it (the worker decides whether
 * that request continues the prepared audio).
 */
internal class NeuralStopTarget {
    private var active: Int? = null
    private var handedOff: Int? = null

    fun started(id: Int) {
        active = id
        handedOff = null
    }

    fun uncovered(id: Int) {
        if (active == id) {
            active = null
            handedOff = id
        }
    }

    fun finished(id: Int) {
        if (active == id) active = null
        if (handedOff == id) handedOff = null
    }

    /** The request to stop, if any, and forget it. */
    fun stop(): Int? = (active ?: handedOff).also {
        active = null
        handedOff = null
    }
}
