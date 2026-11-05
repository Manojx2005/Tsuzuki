package eu.kanade.presentation.tsuzuki.updates

import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.updates.model.UpdatesWithRelations

class TsuzukiUpdatesTwoPaneTest {

    private fun item(manga: Long, chapter: Long) = UpdatesItem(
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
            dateFetch = 0,
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
}
