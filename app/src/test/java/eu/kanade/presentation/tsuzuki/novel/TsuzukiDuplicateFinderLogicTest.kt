package eu.kanade.presentation.tsuzuki.novel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.interactor.DuplicateMatchMode

class TsuzukiDuplicateFinderLogicTest {

    @Test
    fun `segments round trip every match mode`() {
        DuplicateMatchMode.entries.forEach {
            assertEquals(it, duplicateSegmentMode(duplicateSegmentIndex(it)))
        }
        assertEquals(DuplicateMatchMode.URL, duplicateSegmentMode(99))
        assertEquals(DuplicateMatchMode.EXACT, duplicateSegmentMode(-3))
    }

    @Test
    fun `zero counts are left out`() {
        assertEquals(emptyList<DuplicateStat>(), duplicateStats(0, 0))
        assertEquals(listOf(DuplicateStat.DOWNLOADS), duplicateStats(4, 0))
        assertEquals(listOf(DuplicateStat.READ), duplicateStats(0, 2))
        assertEquals(listOf(DuplicateStat.DOWNLOADS, DuplicateStat.READ), duplicateStats(1, 1))
    }

    @Test
    fun `totals count groups and books`() {
        assertEquals(0 to 0, duplicateTotals(emptyList()))
        assertEquals(2 to 5, duplicateTotals(listOf(3, 2)))
    }
}
