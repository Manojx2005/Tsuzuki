package eu.kanade.tachiyomi.tsuzuki

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** The Stats board's Week / Month / Year periods. */
enum class ReadingPeriod { WEEK, MONTH, YEAR }

/** One bar of the minutes chart: its label and minutes read. */
data class ReadingBar(val label: String, val minutes: Long, val isCurrent: Boolean)

object ReadingChart {

    /**
     * Bars for [period] ending at [today]:
     * - Week: Monday–Sunday of this week (labels are the weekday initials from [weekdayLabel]).
     * - Month: the last 30 days, one bar per day (labels are day numbers).
     * - Year: the last 12 months, one bar per month (labels from [monthLabel]).
     * The bar holding today is [ReadingBar.isCurrent] (drawn in the accent).
     */
    fun bars(
        days: Map<LocalDate, Long>,
        today: LocalDate,
        period: ReadingPeriod,
        weekdayLabel: (DayOfWeek) -> String,
        monthLabel: (Int) -> String,
    ): List<ReadingBar> {
        fun minutes(ms: Long) = ms / 60_000L
        return when (period) {
            ReadingPeriod.WEEK -> {
                val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                (0L..6L).map { offset ->
                    val day = monday.plusDays(offset)
                    ReadingBar(weekdayLabel(day.dayOfWeek), minutes(days[day] ?: 0L), day == today)
                }
            }
            ReadingPeriod.MONTH -> (29L downTo 0L).map { ago ->
                val day = today.minusDays(ago)
                ReadingBar(day.dayOfMonth.toString(), minutes(days[day] ?: 0L), ago == 0L)
            }
            ReadingPeriod.YEAR -> (11L downTo 0L).map { ago ->
                val month = today.withDayOfMonth(1).minusMonths(ago)
                val total = days.filterKeys { it.year == month.year && it.month == month.month }.values.sum()
                ReadingBar(monthLabel(month.monthValue), minutes(total), ago == 0L)
            }
        }
    }

    /** "5 h 22 m" style total for the chart header. */
    fun formatTotal(minutes: Long): String = if (minutes >= 60) "${minutes / 60} h ${minutes % 60} m" else "$minutes m"
}
