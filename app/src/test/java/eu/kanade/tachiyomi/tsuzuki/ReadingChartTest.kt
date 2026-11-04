package eu.kanade.tachiyomi.tsuzuki

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ReadingChartTest {

    // 2026-11-03 is a Tuesday.
    private val today = LocalDate.of(2026, 11, 3)
    private val min = 60_000L

    private fun bars(days: Map<LocalDate, Long>, period: ReadingPeriod) =
        ReadingChart.bars(days, today, period, { it.name.take(1) }, { it.toString() })

    @Test
    fun `week runs Monday to Sunday with today current`() {
        val week =
            bars(
                mapOf(today to 12 * min, today.minusDays(1) to 5 * min, today.minusDays(7) to 99 * min),
                ReadingPeriod.WEEK,
            )
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), week.map { it.label })
        assertEquals(listOf(5L, 12L, 0L, 0L, 0L, 0L, 0L), week.map { it.minutes })
        assertEquals(1, week.indexOfFirst { it.isCurrent })
    }

    @Test
    fun `month is the last 30 days ending today`() {
        val month =
            bars(
                mapOf(today to 3 * min, today.minusDays(29) to 2 * min, today.minusDays(30) to 50 * min),
                ReadingPeriod.MONTH,
            )
        assertEquals(30, month.size)
        assertEquals(2L, month.first().minutes)
        assertEquals(3L, month.last().minutes)
        assertEquals(true, month.last().isCurrent)
    }

    @Test
    fun `year sums each month`() {
        val year = bars(
            mapOf(today to 10 * min, LocalDate.of(2026, 11, 1) to 5 * min, LocalDate.of(2026, 10, 15) to 7 * min),
            ReadingPeriod.YEAR,
        )
        assertEquals(12, year.size)
        assertEquals("11", year.last().label)
        assertEquals(15L, year.last().minutes)
        assertEquals(7L, year[10].minutes)
    }

    @Test
    fun `total formatting`() {
        assertEquals("45 m", ReadingChart.formatTotal(45))
        assertEquals("5 h 22 m", ReadingChart.formatTotal(322))
    }
}
