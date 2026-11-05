package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import java.util.Locale

class GlobalSearchStatsTest {
    private fun manga(id: Long) = Manga.create().copy(id = id, url = "/$id")

    @Test
    fun `counts only successful results`() {
        val results = listOf(
            SearchItemResult.Success(listOf(manga(1), manga(2))),
            SearchItemResult.Success(emptyList()),
            SearchItemResult.Loading,
            SearchItemResult.Error(RuntimeException()),
            SearchItemResult.Success(listOf(manga(3))),
        )
        GlobalSearchStats.resultCount(results) shouldBe 3
        GlobalSearchStats.resultCount(emptyList()) shouldBe 0
    }

    @Test
    fun `elapsed is converted to milliseconds and never negative`() {
        GlobalSearchStats.elapsedMillis(1_000_000_000L, 1_600_000_000L) shouldBe 600L
        GlobalSearchStats.elapsedMillis(5L, 1L) shouldBe 0L
    }

    @Test
    fun `seconds use one decimal and the locale separator`() {
        GlobalSearchStats.formatSeconds(600, Locale.US) shouldBe "0.6"
        GlobalSearchStats.formatSeconds(12_340, Locale.US) shouldBe "12.3"
        GlobalSearchStats.formatSeconds(600, Locale.GERMANY) shouldBe "0,6"
    }
}
