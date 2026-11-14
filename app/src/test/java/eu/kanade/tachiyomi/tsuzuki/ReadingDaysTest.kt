package eu.kanade.tachiyomi.tsuzuki

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import java.time.LocalDate

class ReadingDaysTest {

    private val today = LocalDate.of(2026, 11, 3)
    private val min = 60_000L

    private fun days(vararg ago: Pair<Long, Long>) = ago.associate { (d, ms) -> today.minusDays(d) to ms }

    @Test
    fun `consecutive days ending today`() {
        assertEquals(3, ReadingDays.streak(days(0L to 5 * min, 1L to min, 2L to 9 * min, 4L to min), today))
    }

    @Test
    fun `not read yet today keeps yesterday's streak`() {
        assertEquals(2, ReadingDays.streak(days(1L to min, 2L to min), today))
    }

    @Test
    fun `a gap ends the streak and short visits don't count`() {
        assertEquals(0, ReadingDays.streak(days(2L to 10 * min), today))
        assertEquals(1, ReadingDays.streak(days(0L to min, 1L to 30_000L, 2L to min), today))
    }

    @Test
    fun `encoding round-trips and skips junk`() {
        val map = days(0L to 1234L, 3L to 99L)
        assertEquals(map, ReadingDays.decode(ReadingDays.encode(map)))
        assertEquals(mapOf(today to 5L), ReadingDays.decode("bad;2026-11-03=5;2026-13-40=1;x=y"))
        assertEquals(emptyMap<LocalDate, Long>(), ReadingDays.decode(""))
    }

    @Test
    fun `listening preserves older records and counts with reading`() {
        val preferences = InMemoryPreferenceStore()
        preferences.getString("tsz_reading_days").set("$today=$min")
        val store = ReadingDays(preferences)
        store.addListening(mapOf(today to ReadingDays.ListeningDay(2 * min, 1)))
        store.add(today, min)
        assertEquals(mapOf(today to 4 * min), store.days())
        assertEquals(ReadingDays.ListeningSummary(2 * min, 2 * min, 1), store.listeningSummary(today))
    }
}
