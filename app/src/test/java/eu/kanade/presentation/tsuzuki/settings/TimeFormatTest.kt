package eu.kanade.presentation.tsuzuki.settings

import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.TimeFormat
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

class TimeFormatTest {
    @Test
    fun `only follow system uses the device hour cycle`() {
        TimeFormat.FOLLOW_SYSTEM.uses24HourClock(true) shouldBe true
        TimeFormat.FOLLOW_SYSTEM.uses24HourClock(false) shouldBe false
        TimeFormat.HOURS_24.uses24HourClock(false) shouldBe true
        TimeFormat.HOURS_12.uses24HourClock(true) shouldBe false
        TimeFormat.FOLLOW_LANGUAGE.uses24HourClock(true) shouldBe null
    }

    @Test
    fun `follow language formats the same time using the app locale`() {
        val time = LocalTime.of(15, 5)
        UiPreferences.timeFormat(TimeFormat.FOLLOW_LANGUAGE, Locale.GERMANY, false).format(time) shouldBe "15:05"
        val us = UiPreferences.timeFormat(TimeFormat.FOLLOW_LANGUAGE, Locale.US, true).format(time)
        us.startsWith("3:05") shouldBe true
        us.endsWith("PM") shouldBe true
    }

    @Test
    fun `combined timestamp retains the configured date pattern`() {
        val formatter = UiPreferences.dateTimeFormat("yyyy-MM-dd", TimeFormat.FOLLOW_LANGUAGE, Locale.GERMANY, false)
        formatter.format(LocalDateTime.of(2026, 11, 6, 15, 5)) shouldBe "2026-11-06 15:05"
    }
}
