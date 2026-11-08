package eu.kanade.tachiyomi.tsuzuki

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class NovelReadingEstimatesTest {

    @Test
    fun `count uses chapter body text and caches the first usable text`() = runTest {
        val store = NovelReadingEstimates(InMemoryPreferenceStore())
        assertNull(store.cacheWords(1L, "<p>...</p>"))
        assertEquals(0L, store.wordCountChanges.value)
        assertEquals(3, store.cacheWords(1L, "<style>one two</style><p>A&nbsp;fox jumps.</p>"))
        assertEquals(3, store.cacheWords(1L, "replacement"))
        assertEquals(1L, store.wordCountChanges.value)
        assertEquals(2, countChapterWords("<p>猫。犬。</p>"))
    }

    @Test
    fun `reading speed requires three samples with words and elapsed time`() = runTest {
        val store = NovelReadingEstimates(InMemoryPreferenceStore())
        store.cacheWords(1L, "word ".repeat(600))
        store.recordSession(20_000, mapOf(1L to 20))
        store.recordSession(60_000, mapOf(1L to 25, 2L to 10))
        store.recordSession(60_000, mapOf(1L to 25))
        store.recordSession(120_000, mapOf(1L to 60))
        assertNull(store.speed())
        store.recordSession(60_000, mapOf(1L to 50))
        assertEquals(180.0, store.speed())
    }

    @Test
    fun `session counts new ground once and can discard a seek`() {
        val session = NovelReadingSession()
        session.start()
        session.note(1, 60, 70)
        session.note(1, 70, 65)
        session.note(1, 65, 80)
        session.note(2, 0, 10)
        assertEquals(mapOf(1L to 20, 2L to 10), session.finish())
        session.note(2, 10, 30)
        assertEquals(emptyMap<Long, Int>(), session.finish())
        session.start()
        session.note(2, 10, 30)
        session.discard()
        session.note(2, 30, 80)
        assertEquals(emptyMap<Long, Int>(), session.finish())
    }

    @Test
    fun `time left uses partial progress and hides estimates with missing counts`() {
        assertEquals(NovelEstimateMinutes(2, 7), NovelEstimateWords(600, 1_000.0, true).minutes(50, 200.0))
        assertEquals(NovelEstimateMinutes(2), NovelEstimateWords(600, null, true).minutes(50, 200.0))
        assertEquals(NovelEstimateMinutes(), NovelEstimateWords(null, 1_000.0, true).minutes(50, 200.0))
        assertEquals(NovelEstimateMinutes(book = 5), NovelEstimateWords(600, 1_000.0, true).minutes(100, 200.0))
        assertEquals(NovelEstimateMinutes(2, 5), NovelEstimateWords(600, 1_000.0, false).minutes(50, 200.0))
        assertEquals(175.0, medianReadingRate(listOf(300, 100, 200, 150)))
    }

    @Test
    fun `book total counts only the unread portions of other chapters`() {
        val otherUnread = remainingNovelWords(400, 25) + remainingNovelWords(500, 80)
        assertEquals(400.0, otherUnread)
        assertEquals(NovelEstimateMinutes(2, 6), NovelEstimateWords(400, otherUnread, true).minutes(50, 100.0))
        assertEquals(0.0, remainingNovelWords(400, 110))
    }
}
