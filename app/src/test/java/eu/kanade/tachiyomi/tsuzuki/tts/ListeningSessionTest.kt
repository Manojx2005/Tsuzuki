package eu.kanade.tachiyomi.tsuzuki.tts

import eu.kanade.tachiyomi.tsuzuki.ReadingDays
import eu.kanade.tachiyomi.tsuzuki.ReadingDays.ListeningSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import java.time.ZonedDateTime

class ListeningSessionTest {
    private val preferences = InMemoryPreferenceStore()
    private val days = ReadingDays(preferences)
    private var wall = ZonedDateTime.parse("2026-11-08T14:00:00+09:00[Asia/Tokyo]")
    private var now = 0L
    private var canRecord = true
    private val session = ListeningSession({ days }, { canRecord }, { now }, { wall })

    @Test
    fun pauseThenResumeExcludesSynthesisAndPausedTime() {
        session.beginQueue(1, chapterId = 101)
        advance(5)
        session.startChunk(1, 0)
        advance(30)
        session.endQueue(1)
        assertEquals(ListeningSummary(30_000, 30_000, 0), days.listeningSummary(wall.toLocalDate()))
        advance(120)
        session.beginQueue(2, chapterId = 102)
        session.startChunk(2, 0)
        advance(35)
        session.endChunk(2, 0, completedChapter = true)
        assertEquals(ListeningSummary(65_000, 65_000, 1), days.listeningSummary(wall.toLocalDate()))
    }

    @Test
    fun sleepTimerStopFlushesPartialSpeechWithoutCompletingAChapter() {
        session.beginQueue(1, chapterId = 101)
        session.startChunk(1, 0)
        advance(10 * 60)
        // The minute sleep timer calls the same stop path as a manual stop.
        session.endQueue(1)
        session.endChunk(1, 0, completedChapter = true)
        assertEquals(ListeningSummary(600_000, 600_000, 0), days.listeningSummary(wall.toLocalDate()))
    }

    @Test
    fun chapterHandoffCountsBothFinishesAndIgnoresLateCallbacks() {
        session.beginQueue(1, chapterId = 101)
        session.startChunk(1, 0)
        advance(25)
        session.endChunk(1, 0, completedChapter = true)
        advance(10)
        session.beginQueue(2, chapterId = 102)
        session.startChunk(1, 0)
        session.startChunk(2, 0)
        session.endQueue(1)
        advance(40)
        session.endChunk(2, 0, completedChapter = true)
        assertEquals(ListeningSummary(65_000, 65_000, 2), days.listeningSummary(wall.toLocalDate()))
    }

    @Test
    fun activityPauseFlushSurvivesRestartAndKeepsBackgroundSpeechSeparate() {
        session.setReaderVisible(true)
        session.beginReadingWindow()
        session.beginQueue(1, chapterId = 101)
        advance(10)
        session.startChunk(1, 0)
        advance(40)
        session.setReaderVisible(false)
        val reopened = ReadingDays(preferences)
        assertEquals(40_000L, reopened.days()[wall.toLocalDate()])
        assertEquals(10_000L, session.endReadingWindow())
        advance(30)
        session.endQueue(1)
        session.beginReadingWindow()
        advance(60)
        assertEquals(0L, session.endReadingWindow())
        assertEquals(70_000L, reopened.days()[wall.toLocalDate()])
    }

    @Test
    fun speechSpanningMidnightIsSplitBetweenLocalDays() {
        wall = wall.withHour(23).withMinute(59).withSecond(30)
        val sunday = wall.toLocalDate()
        session.beginQueue(1, chapterId = 101)
        session.startChunk(1, 0)
        advance(70)
        session.endChunk(1, 0, completedChapter = true)
        assertEquals(mapOf(sunday to 30_000L, sunday.plusDays(1) to 40_000L), days.days())
        assertEquals(ListeningSummary(40_000, 40_000, 1), days.listeningSummary(wall.toLocalDate()))
    }

    @Test
    fun incognitoPlaybackDoesNotCreateListeningData() {
        canRecord = false
        session.beginQueue(1, chapterId = 101)
        session.startChunk(1, 0)
        advance(60)
        session.endChunk(1, 0, completedChapter = true)
        assertNull(days.listeningSummary(wall.toLocalDate()))
        assertEquals(emptyMap<java.time.LocalDate, Long>(), days.days())
    }

    @Test
    fun duplicateHandoffsAndNewQueuesForTheSameChapterCountOncePerRun() {
        for ((queue, chapter) in listOf(1L to 101L, 2L to 101L, 3L to 102L)) {
            session.beginQueue(queue, chapter)
            session.startChunk(queue, 0)
            advance(20)
            assertTrue(session.endChunk(queue, 0, completedChapter = true))
            assertFalse(session.endChunk(queue, 0, completedChapter = true))
        }
        assertEquals(ListeningSummary(60_000, 60_000, 2), days.listeningSummary(wall.toLocalDate()))
    }

    @Test
    fun pauseAndSeekKeepTheRunButReplayAfterStopCountsAgain() {
        session.beginQueue(1, 101)
        session.startChunk(1, 0)
        advance(20)
        session.endChunk(1, 0, completedChapter = true)
        session.beginQueue(2, 101)
        session.startChunk(2, 0)
        advance(5)
        session.endQueue(2) // Pause or seek closes only the current queue.
        advance(120)
        session.beginQueue(3, 101)
        session.startChunk(3, 0)
        advance(15)
        session.endChunk(3, 0, completedChapter = true)
        assertEquals(ListeningSummary(40_000, 40_000, 1), days.listeningSummary(wall.toLocalDate()))

        session.endRun() // Stop/manual chapter change/reader close, even after the queue finished.
        session.beginQueue(4, 101)
        session.startChunk(4, 0)
        advance(10)
        session.endChunk(4, 0, completedChapter = true)
        assertEquals(ListeningSummary(50_000, 50_000, 2), days.listeningSummary(wall.toLocalDate()))
    }

    @Test
    fun incognitoCompletionStillAcknowledgesTheHandoffWithoutRecording() {
        canRecord = false
        for (id in 1L..2L) {
            session.beginQueue(id, 100 + id)
            session.startChunk(id, 0)
            advance(20)
            assertTrue(session.endChunk(id, 0, completedChapter = true))
            assertFalse(session.endChunk(id, 0, completedChapter = true))
        }
        assertNull(days.listeningSummary(wall.toLocalDate()))
    }

    @Test
    fun enablingIncognitoDuringAQueueDiscardsItsUnflushedData() {
        session.beginQueue(1, 101)
        session.startChunk(1, 0)
        advance(20)
        session.flush()
        advance(20)
        canRecord = false
        session.endChunk(1, 0, completedChapter = true)
        assertEquals(ListeningSummary(20_000, 20_000, 0), days.listeningSummary(wall.toLocalDate()))
    }

    @Test
    fun speechWithoutAChapterIdCanRecordTimeButCannotCountAChapter() {
        session.beginQueue(1, null)
        session.startChunk(1, 0)
        advance(10)
        assertTrue(session.endChunk(1, 0, completedChapter = true))
        assertEquals(ListeningSummary(10_000, 10_000, 0), days.listeningSummary(wall.toLocalDate()))
    }

    private fun advance(seconds: Long) {
        now += seconds * 1_000
        wall = wall.plusSeconds(seconds)
    }
}
