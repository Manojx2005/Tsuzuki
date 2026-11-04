package eu.kanade.presentation.tsuzuki.history

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.model.MangaCover
import java.util.Date

class HistoryStatsTest {

    private val today = 1_000_000_000L

    private fun row(chapterId: Long, readAt: Long?, minutes: Long) = HistoryWithRelations(
        id = chapterId,
        chapterId = chapterId,
        mangaId = 1,
        title = "",
        chapterNumber = 1.0,
        readAt = readAt?.let(::Date),
        readDuration = minutes * 60_000L,
        coverData = MangaCover(mangaId = 1, sourceId = 0, isMangaFavorite = false, url = null, lastModified = 0),
        chapterRead = false,
        lastPageRead = 0,
        isNovel = true,
    )

    @Test
    fun `only chapters last read today count`() {
        val stats = historyStats(
            listOf(row(1, today + 10, 5), row(2, today + 20, 3), row(3, today - 1, 40), row(4, null, 9)),
            today,
        )
        assertEquals(HistoryStats(todayMinutes = 8, chaptersToday = 2), stats)
    }

    @Test
    fun `no history is zero, not missing`() {
        assertEquals(HistoryStats(0, 0), historyStats(emptyList(), today))
    }
}
