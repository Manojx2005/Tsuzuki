package eu.kanade.tachiyomi.tsuzuki.tts

import android.os.SystemClock
import eu.kanade.tachiyomi.tsuzuki.ReadingDays
import eu.kanade.tachiyomi.tsuzuki.ReadingDays.ListeningDay
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicLong

/** Accounts only for time between an actual TTS start and its matching end or interruption. */
internal class ListeningSession(
    private val readingDays: () -> ReadingDays,
    private val canRecord: () -> Boolean,
    private val elapsedMs: () -> Long = SystemClock::elapsedRealtime,
    private val localTime: () -> ZonedDateTime = ZonedDateTime::now,
) {
    private var queue: Long? = null
    private var chapterId: Long? = null
    private var playbackRun: Long? = null
    private var recording = false
    private val completedChapters = mutableSetOf<Long>()
    private var activeChunk: Int? = null
    private var lastCheckpoint: Long? = null
    private var readerVisible = false
    private var readingWindowOpen = false
    private var readingMs = 0L
    private val pending = mutableMapOf<LocalDate, ListeningDay>()

    @Synchronized
    fun beginQueue(id: Long, chapterId: Long?) {
        finishSpeech()
        persist()
        if (playbackRun == null) playbackRun = nextRun.incrementAndGet()
        queue = id
        this.chapterId = chapterId
        recording = canRecord()
    }

    @Synchronized
    fun startChunk(id: Long, index: Int) {
        if (queue != id || activeChunk == index) return
        checkpoint()
        activeChunk = index
    }

    /** Returns false for duplicate or interrupted callbacks, including while incognito. */
    @Synchronized
    fun endChunk(id: Long, index: Int, completedChapter: Boolean): Boolean {
        if (queue != id || activeChunk != index) return false
        finishSpeech()
        if (completedChapter) {
            val chapter = chapterId
            val firstCompletion = chapter != null && completedChapters.add(chapter)
            val counted = firstCompletion && recording && canRecord()
            logcat(LogPriority.DEBUG) {
                "TTS chapter finished chapter=$chapter run=$playbackRun queue=$id counted=$counted"
            }
            if (counted) {
                val day = localTime().toLocalDate()
                val previous = pending[day] ?: ListeningDay()
                pending[day] = previous.copy(chapters = previous.chapters + 1)
            }
            queue = null
            chapterId = null
        }
        if (completedChapter || pending.values.sumOf { it.durationMs } >= 60_000L) persist()
        return true
    }

    @Synchronized
    fun endQueue(id: Long) {
        if (queue != id) return
        finishSpeech()
        queue = null
        persist()
    }

    /** Queue changes and pauses preserve a run; Stop, manual navigation and viewer close end it. */
    @Synchronized
    fun endRun() {
        finishSpeech()
        queue = null
        chapterId = null
        persist()
        playbackRun = null
        completedChapters.clear()
        recording = false
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

        if (!recording || !canRecord()) return
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
        if (canRecord()) readingDays().addListening(pending)
        pending.clear()
    }

    private companion object {
        val nextRun = AtomicLong()
    }
}
