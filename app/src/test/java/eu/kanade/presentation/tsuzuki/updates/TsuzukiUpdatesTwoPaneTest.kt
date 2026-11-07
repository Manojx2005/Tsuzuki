package eu.kanade.presentation.tsuzuki.updates

import eu.kanade.presentation.updates.UpdatesUiModel
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import eu.kanade.tachiyomi.ui.updates.UpdatesViewModel
import eu.kanade.tachiyomi.util.lang.toLocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.updates.model.UpdatesWithRelations

class TsuzukiUpdatesTwoPaneTest {

    private fun item(manga: Long, chapter: Long, dateFetch: Long = 0) = UpdatesItem(
        update = UpdatesWithRelations(
            mangaId = manga,
            mangaTitle = "m$manga",
            chapterId = chapter,
            chapterName = "c$chapter",
            scanlator = null,
            chapterUrl = "",
            read = false,
            bookmark = false,
            lastPageRead = 0,
            sourceId = 1,
            dateFetch = dateFetch,
            coverData = MangaCover(manga, 1, true, null, 0),
        ),
        downloadStateProvider = { Download.State.NOT_DOWNLOADED },
        downloadProgressProvider = { 0 },
    )

    @Test
    fun selectionFallsBackToFirstRow() {
        val rows = listOf(item(1, 10), item(1, 11))
        assertEquals(10L, resolveUpdatesSelection(rows, null) { it.update.chapterId }?.update?.chapterId)
        assertEquals(10L, resolveUpdatesSelection(rows, 99L) { it.update.chapterId }?.update?.chapterId)
        assertEquals(11L, resolveUpdatesSelection(rows, 11L) { it.update.chapterId }?.update?.chapterId)
        assertNull(resolveUpdatesSelection(emptyList<UpdatesItem>(), 1L) { it.update.chapterId })
    }

    @Test
    fun otherChaptersAreSameBookWithoutSelected() {
        val a = item(1, 10)
        val items = listOf(a, item(2, 20), item(1, 11), item(1, 12))
        assertEquals(listOf(11L, 12L), otherNewChapters(items, a).map { it.update.chapterId })
    }

    @Test
    fun mapsSyntheticLibraryUpdatesInOrderWithOneHeaderPerDate() {
        val rows = (0L until 100L).flatMap { chapter ->
            (1L..500L).map { book ->
                item(book, chapter * 500 + book, dateFetch = (100 - chapter) * 86_400_000L)
            }
        }
        val state = UpdatesViewModel.State(items = rows)
        val models = state.getUiModel()
        assertEquals(rows, models.filterIsInstance<UpdatesUiModel.Item>().map { it.item })
        val expectedDates = rows.chunked(500).map { it.first().update.dateFetch.toLocalDate() }
        assertEquals(expectedDates, models.filterIsInstance<UpdatesUiModel.Header>().map { it.date })
        assertEquals(500, state.getNovelGroups().size)
        assertEquals(50_000, state.getNovelGroups().sumOf { it.chapters.size })
    }
}
