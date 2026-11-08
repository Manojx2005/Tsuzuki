package eu.kanade.tachiyomi.tsuzuki

import tachiyomi.core.common.preference.PreferenceStore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Reading and listening time per calendar day, for stats, goals and the reading streak.
 *
 * The history table keeps one row per chapter and overwrites its last-read time, so past days can't be rebuilt
 * from it. This keeps a small day → milliseconds map in preferences instead of a new table, so Tsundoku's
 * database migrations stay mergeable. Only the last [KEEP_DAYS] days are kept. Incognito reading isn't recorded
 * (the caller skips it, as for history).
 */
class ReadingDays(preferenceStore: PreferenceStore) {

    private val pref = preferenceStore.getString("tsz_reading_days", "")

    /** Daily reading goal in minutes (Stats board), 5–240 in steps of 5. */
    val dailyGoalMinutes = preferenceStore.getInt("tsz_daily_goal_minutes", 30)

    /** Adds [durationMs] of reading to [day]. */
    @Synchronized
    fun add(day: LocalDate, durationMs: Long) {
        if (durationMs <= 0) return
        val days = decodeEntries(pref.get()).toMutableMap()
        val previous = days[day] ?: Day()
        days[day] = previous.copy(readMs = previous.readMs + durationMs)
        save(days, day)
    }

    @Synchronized
    internal fun addListening(deltas: Map<LocalDate, ListeningDay>) {
        val newest = deltas.keys.maxOrNull() ?: return
        val days = decodeEntries(pref.get()).toMutableMap()
        deltas.forEach { (date, delta) ->
            val previous = days[date] ?: Day()
            days[date] = previous.copy(
                listening = ListeningDay(
                    durationMs = previous.listening.durationMs + delta.durationMs,
                    chapters = previous.listening.chapters + delta.chapters,
                ),
            )
        }
        save(days, newest)
    }

    private fun save(days: Map<LocalDate, Day>, today: LocalDate) {
        val oldest = today.minusDays(KEEP_DAYS - 1)
        pref.set(encodeEntries(days.filterKeys { !it.isBefore(oldest) }))
    }

    fun days(): Map<LocalDate, Long> = decode(pref.get())

    fun changes() = pref.changes()

    internal data class ListeningDay(val durationMs: Long = 0, val chapters: Int = 0)

    internal data class ListeningSummary(val todayMs: Long, val weekMs: Long, val weekChapters: Int)

    internal fun listeningSummary(today: LocalDate): ListeningSummary? {
        val days = decodeEntries(pref.get())
        if (days.values.none { it.listening != ListeningDay() }) return null
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val week = days.filterKeys { it in weekStart..today }.values
        return ListeningSummary(
            todayMs = days[today]?.listening?.durationMs ?: 0L,
            weekMs = week.sumOf { it.listening.durationMs },
            weekChapters = week.sumOf { it.listening.chapters },
        )
    }

    private data class Day(val readMs: Long = 0, val listening: ListeningDay = ListeningDay())

    companion object {
        const val KEEP_DAYS = 400L

        /** A day counts toward the streak once at least this much was read. */
        const val MIN_STREAK_MS = 60_000L

        internal fun encode(days: Map<LocalDate, Long>): String = encodeEntries(days.mapValues { Day(it.value) })

        internal fun decode(raw: String): Map<LocalDate, Long> =
            decodeEntries(raw).mapValues { (_, entry) -> entry.readMs + entry.listening.durationMs }

        private fun encodeEntries(days: Map<LocalDate, Day>): String =
            days.entries.sortedBy { it.key }.joinToString(";") { (date, day) ->
                val listening = day.listening
                if (listening == ListeningDay()) {
                    "$date=${day.readMs}"
                } else {
                    "$date=${day.readMs},${listening.durationMs},${listening.chapters}"
                }
            }

        private fun decodeEntries(raw: String): Map<LocalDate, Day> = raw.split(';').mapNotNull { entry ->
            val (day, data) = entry.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
            val date = runCatching { LocalDate.parse(day) }.getOrNull() ?: return@mapNotNull null
            val values = data.split(',')
            val readMs = values[0].toLongOrNull() ?: return@mapNotNull null
            val listenMs = values.getOrNull(1)?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            val chapters = values.getOrNull(2)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            date to Day(readMs, ListeningDay(listenMs, chapters))
        }.toMap()

        /**
         * Consecutive days with reading, ending today. If nothing is read yet today the streak still counts up to
         * yesterday, so it doesn't reset in the morning before you've opened a book.
         */
        fun streak(days: Map<LocalDate, Long>, today: LocalDate): Int {
            fun read(day: LocalDate) = (days[day] ?: 0L) >= MIN_STREAK_MS
            var day = if (read(today)) today else today.minusDays(1)
            var count = 0
            while (read(day)) {
                count++
                day = day.minusDays(1)
            }
            return count
        }
    }
}
