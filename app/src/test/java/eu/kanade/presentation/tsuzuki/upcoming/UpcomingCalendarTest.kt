package eu.kanade.presentation.tsuzuki.upcoming

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import mihon.feature.upcoming.UpcomingUIModel
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import java.time.DayOfWeek

class UpcomingCalendarTest {
    @Test
    fun `leap month keeps its final day and aligns to the locale week start`() {
        val february = YearMonth(2024, 2)

        val mondayWeeks = upcomingMonthWeeks(february, DayOfWeek.MONDAY)
        mondayWeeks.first().indexOf(LocalDate(2024, 2, 1)) shouldBe 3
        mondayWeeks.flatten().filterNotNull().last() shouldBe LocalDate(2024, 2, 29)
        mondayWeeks.flatten().filterNotNull().size shouldBe 29

        val sundayWeeks = upcomingMonthWeeks(february, DayOfWeek.SUNDAY)
        sundayWeeks.first().indexOf(LocalDate(2024, 2, 1)) shouldBe 4
        sundayWeeks.map { it.size }.distinct() shouldBe listOf(7)
    }

    @Test
    fun `calendar uses a sixth week when a month spans it`() {
        val weeks = upcomingMonthWeeks(YearMonth(2026, 11), DayOfWeek.MONDAY)

        weeks.size shouldBe 6
        weeks.last().first() shouldBe LocalDate(2026, 11, 30)
        weeks.last().drop(1) shouldBe List(6) { null }
    }

    @Test
    fun `selection starts on the first forecast in the shown month`() {
        val november = YearMonth(2026, 11)
        val today = LocalDate(2026, 11, 5)
        val forecasts = setOf(LocalDate(2026, 11, 20), LocalDate(2026, 10, 31), LocalDate(2026, 11, 7))

        defaultUpcomingDate(november, today, forecasts) shouldBe LocalDate(2026, 11, 7)
        defaultUpcomingDate(november, today, emptySet()) shouldBe today
        defaultUpcomingDate(YearMonth(2026, 12), today, forecasts) shouldBe LocalDate(2026, 12, 1)
    }

    @Test
    fun `day list keeps stock row order and stops at the next date`() {
        val firstDate = LocalDate(2026, 11, 5)
        val secondDate = LocalDate(2026, 11, 6)
        val first = Manga.create().copy(id = 1)
        val second = Manga.create().copy(id = 2)
        val third = Manga.create().copy(id = 3)
        val items = listOf(
            UpcomingUIModel.Header(firstDate, 2),
            UpcomingUIModel.Item(first),
            UpcomingUIModel.Item(second),
            UpcomingUIModel.Header(secondDate, 1),
            UpcomingUIModel.Item(third),
        )

        upcomingBooksForDay(items, 0) shouldBe listOf(first, second)
        upcomingBooksForDay(items, 3) shouldBe listOf(third)
        upcomingBooksForDay(items, null).isEmpty() shouldBe true
    }
}
