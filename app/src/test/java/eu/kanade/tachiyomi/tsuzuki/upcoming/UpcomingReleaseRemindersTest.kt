package eu.kanade.tachiyomi.tsuzuki.upcoming

import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import kotlin.time.Instant

class UpcomingReleaseRemindersTest {
    private val zone = TimeZone.of("Asia/Tokyo")
    private val forecast = Instant.parse("2026-11-08T15:00:00Z").toEpochMilliseconds()

    @Test
    fun `reminders are off by default and scoped to the selected book`() {
        val preferences = InMemoryPreferenceStore()
        val reminders = UpcomingReminderPreferences(preferences)
        assertTrue(reminders.enabled.get().isEmpty())
        reminders.setEnabled(7, true)
        reminders.setEnabled(42, true)
        reminders.setEnabled(7, false)
        assertFalse("7" in UpcomingReminderPreferences(preferences).enabled.get())
        assertTrue("42" in UpcomingReminderPreferences(preferences).enabled.get())
    }

    @Test
    fun `scheduled work only delivers on the current estimate day`() {
        val before = Instant.parse("2026-11-08T14:59:00Z")
        val laterThatDay = Instant.parse("2026-11-09T11:00:00Z")
        val after = Instant.parse("2026-11-09T15:00:00Z")
        assertTrue(shouldScheduleUpcomingRelease(forecast, 0, before, zone))
        assertFalse(shouldDeliverUpcomingRelease(forecast, forecast, 0, before, zone))
        assertTrue(shouldDeliverUpcomingRelease(forecast, forecast, 0, laterThatDay, zone))
        assertFalse(shouldDeliverUpcomingRelease(forecast, forecast - 1, 0, laterThatDay, zone))
        assertFalse(shouldDeliverUpcomingRelease(forecast, forecast, 0, after, zone))
        assertFalse(shouldScheduleUpcomingRelease(forecast, 0, after, zone))
        assertFalse(shouldScheduleUpcomingRelease(0, 0, before, zone))
    }

    @Test
    fun `a revised time on the same date cannot repeat a delivered reminder`() {
        val today = Instant.parse("2026-11-09T02:00:00Z")
        val revisedToday = forecast + 60_000L
        val tomorrow = forecast + 86_400_000L
        assertFalse(shouldScheduleUpcomingRelease(revisedToday, forecast, today, zone))
        assertTrue(shouldScheduleUpcomingRelease(tomorrow, forecast, today, zone))
    }
}
