package eu.kanade.tachiyomi.tsuzuki

import tachiyomi.core.common.preference.PreferenceStore
import java.time.LocalDate

/**
 * Minutes read per calendar day, for the reading streak (History and Home boards).
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
        val days = decode(pref.get()).toMutableMap()
        days[day] = (days[day] ?: 0L) + durationMs
        val oldest = day.minusDays(KEEP_DAYS - 1)
        pref.set(encode(days.filterKeys { !it.isBefore(oldest) }))
    }

    fun days(): Map<LocalDate, Long> = decode(pref.get())

    fun changes() = pref.changes()

    companion object {
        const val KEEP_DAYS = 400L

        /** A day counts toward the streak once at least this much was read. */
        const val MIN_STREAK_MS = 60_000L

        internal fun encode(days: Map<LocalDate, Long>): String =
            days.entries.sortedBy { it.key }.joinToString(";") { "${it.key}=${it.value}" }

        internal fun decode(raw: String): Map<LocalDate, Long> = raw.split(';').mapNotNull { entry ->
            val (day, ms) = entry.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
            val date = runCatching { LocalDate.parse(day) }.getOrNull() ?: return@mapNotNull null
            date to (ms.toLongOrNull() ?: return@mapNotNull null)
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
