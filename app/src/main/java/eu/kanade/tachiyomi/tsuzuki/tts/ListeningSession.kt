package eu.kanade.tachiyomi.tsuzuki.tts

import android.os.SystemClock
import eu.kanade.tachiyomi.tsuzuki.ReadingDays
import eu.kanade.tachiyomi.tsuzuki.ReadingDays.ListeningDay
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

/** Accounts only for time between an actual TTS start and its matching end or interruption. */
internal class ListeningSession(
    private val readingDays: () -> ReadingDays,
    private val canRecord: () -> Boolean,
    private val elapsedMs: () -> Long = SystemClock::elapsedRealtime,
    private val localTime: () -> ZonedDateTime = ZonedDateTime::now,
) {
    private var queue: Long? = null
    private var activeChunk: Int? = null
    private var lastCheckpoint: Long? = null
    private var readerVisible = false
    private var readingWindowOpen = false
    private var readingMs = 0L
    private val pending = mutableMapOf<LocalDate, ListeningDay>()

    @Synchronized
    fun beginQueue(id: Long) {
        finishSpeech()
        persist()
        queue = id.takeIf { canRecord() }
    }

    @Synchronized
    fun startChunk(id: Long, index: Int) {
        if (queue != id || activeChunk == index) return
        checkpoint()
        activeChunk = index
    }

    @Synchronized
    fun endChunk(id: Long, index: Int, completedChapter: Boolean) {
        if (queue != id || activeChunk != index) return
        finishSpeech()
        if (completedChapter) {
            val day = localTime().toLocalDate()
            val previous = pending[day] ?: ListeningDay()
            pending[day] = previous.copy(chapters = previous.chapters + 1)
            queue = null
        }
        if (completedChapter || pending.values.sumOf { it.durationMs } >= 60_000L) persist()
    }

    @Synchronized
    fun endQueue(id: Long) {
        if (queue != id) return
        finishSpeech()
        queue = null
        persist()
    }

    @Synchronized
    fun flush() {
        checkpoint()
        persist()
    }

    @Synchronized
    fun setReaderVisible(visible: Boolean) {
        checkpoint()
        readerVisible = visible
        if (!visible) persist()
    }

    @Synchronized
    fun beginReadingWindow() {
        checkpoint()
        readingWindowOpen = true
        readingMs = 0L
    }

    @Synchronized
    fun endReadingWindow(): Long {
        checkpoint()
        readingWindowOpen = false
        return readingMs.also { readingMs = 0L }
    }

    private fun finishSpeech() {
        checkpoint()
        activeChunk = null
    }

    private fun checkpoint() {
        val now = elapsedMs()
        val before = lastCheckpoint
        lastCheckpoint = now
        val duration = now - (before ?: return)
        if (duration <= 0L) return
        if (activeChunk == null) {
            if (readerVisible && readingWindowOpen) readingMs += duration
            return
        }

        val end = localTime()
        var start = end.minus(Duration.ofMillis(duration))
        while (start.isBefore(end)) {
            val day = start.toLocalDate()
            val midnight = day.plusDays(1).atStartOfDay(start.zone)
            val until = minOf(end, midnight)
            val portion = Duration.between(start, until).toMillis()
            val previous = pending[day] ?: ListeningDay()
            pending[day] = previous.copy(durationMs = previous.durationMs + portion)
            start = until
        }
    }

    private fun persist() {
        if (pending.isEmpty()) return
        readingDays().addListening(pending)
        pending.clear()
    }
}
