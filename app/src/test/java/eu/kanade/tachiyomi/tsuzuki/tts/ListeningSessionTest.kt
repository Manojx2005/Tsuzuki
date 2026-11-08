package eu.kanade.tachiyomi.tsuzuki.tts

import eu.kanade.tachiyomi.tsuzuki.ReadingDays
import eu.kanade.tachiyomi.tsuzuki.ReadingDays.ListeningSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
        session.beginQueue(1)
        advance(5)
        session.startChunk(1, 0)
        advance(30)
        session.endQueue(1)
        assertEquals(ListeningSummary(30_000, 30_000, 0), days.listeningSummary(wall.toLocalDate()))
        advance(120)
        session.beginQueue(2)
        session.startChunk(2, 0)
        advance(35)
        session.endChunk(2, 0, completedChapter = true)
        assertEquals(ListeningSummary(65_000, 65_000, 1), days.listeningSummary(wall.toLocalDate()))
    }

    @Test
    fun sleepTimerStopFlushesPartialSpeechWithoutCompletingAChapter() {
        session.beginQueue(1)
        session.startChunk(1, 0)
        advance(10 * 60)
        // The minute sleep timer calls the same stop path as a manual stop.
        session.endQueue(1)
        session.endChunk(1, 0, completedChapter = true)
        assertEquals(ListeningSummary(600_000, 600_000, 0), days.listeningSummary(wall.toLocalDate()))
    }

    @Test
    fun chapterHandoffCountsBothFinishesAndIgnoresLateCallbacks() {
        session.beginQueue(1)
        session.startChunk(1, 0)
        advance(25)
        session.endChunk(1, 0, completedChapter = true)
        advance(10)
        session.beginQueue(2)
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
        session.beginQueue(1)
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
        session.beginQueue(1)
        session.startChunk(1, 0)
        advance(70)
        session.endChunk(1, 0, completedChapter = true)
        assertEquals(mapOf(sunday to 30_000L, sunday.plusDays(1) to 40_000L), days.days())
        assertEquals(ListeningSummary(40_000, 40_000, 1), days.listeningSummary(wall.toLocalDate()))
    }

    @Test
    fun incognitoPlaybackDoesNotCreateListeningData() {
        canRecord = false
        session.beginQueue(1)
        session.startChunk(1, 0)
        advance(60)
        session.endChunk(1, 0, completedChapter = true)
        assertNull(days.listeningSummary(wall.toLocalDate()))
        assertEquals(emptyMap<java.time.LocalDate, Long>(), days.days())
    }

    private fun advance(seconds: Long) {
        now += seconds * 1_000
        wall = wall.plusSeconds(seconds)
    }
}
