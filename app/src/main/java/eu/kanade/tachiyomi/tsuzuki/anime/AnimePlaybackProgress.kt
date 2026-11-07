package eu.kanade.tachiyomi.tsuzuki.anime

internal data class AnimePlaybackCheckpoint(val position: Double, val duration: Double, val watchSeconds: Long)

internal fun animeEpisodeSeen(position: Double, duration: Double): Boolean =
    position.isFinite() && duration.isFinite() && duration > 0 && position >= duration * 0.85

/** Counts playback increments while excluding jumps from seeking or restoring a position. */
internal class AnimePlaybackProgress {
    private var position: Double? = null
    private var duration = 0.0
    private var watchSeconds = 0.0
    private var changed = false
    private var skipIncrement = true

    @Synchronized
    fun onPosition(seconds: Double, playbackRate: Float = 1f) {
        if (!seconds.isFinite() || seconds < 0.0) return
        val last = position
        if (last != null && !skipIncrement) {
            val elapsed = (seconds - last) / playbackRate
            if (elapsed in 0.0..2.0) watchSeconds += elapsed
        }
        skipIncrement = false
        if (last != null && last != seconds) changed = true
        position = seconds
    }

    @Synchronized
    fun onDuration(seconds: Double) {
        if (seconds.isFinite() && seconds > 0.0) duration = seconds
    }

    @Synchronized
    fun skipNextIncrement() {
        skipIncrement = true
    }

    @Synchronized
    fun resumePosition(): Double? = position

    @Synchronized
    fun checkpoint(): AnimePlaybackCheckpoint? {
        val seconds = position ?: return null
        if (!changed) return null
        changed = false
        val wholeSeconds = watchSeconds.toLong()
        watchSeconds -= wholeSeconds
        return AnimePlaybackCheckpoint(seconds, duration, wholeSeconds)
    }
}
