package eu.kanade.tachiyomi.ui.tsuzuki.updates

import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.updates.model.UpdatesWithRelations

class TsuzukiUpdatesSummaryTest {

    private fun item(id: Long, read: Boolean, state: Download.State) = UpdatesItem(
        update = UpdatesWithRelations(
            mangaId = 1,
            mangaTitle = "Book",
            chapterId = id,
            chapterName = "Ch $id",
            scanlator = null,
            chapterUrl = "/$id",
            read = read,
            bookmark = false,
            lastPageRead = 0,
            sourceId = 0,
            dateFetch = 0,
            coverData = MangaCover(mangaId = 1, sourceId = 0, isMangaFavorite = true, url = null, lastModified = 0),
        ),
        downloadStateProvider = { state },
        downloadProgressProvider = { 0 },
    )

    @Test
    fun `empty list gives zero totals`() {
        emptyList<UpdatesItem>().toTsuzukiUpdatesSummary() shouldBe TsuzukiUpdatesSummary(0, 0)
    }

    @Test
    fun `new counts unread chapters only`() {
        val items = listOf(
            item(1, read = false, state = Download.State.DOWNLOADED),
            item(2, read = true, state = Download.State.NOT_DOWNLOADED),
            item(3, read = false, state = Download.State.NOT_DOWNLOADED),
        )
        items.toTsuzukiUpdatesSummary().newCount shouldBe 2
    }

    @Test
    fun `to save counts unread chapters that are not downloaded or failed`() {
        val items = listOf(
            item(1, read = false, state = Download.State.NOT_DOWNLOADED),
            item(2, read = false, state = Download.State.ERROR),
            item(3, read = false, state = Download.State.DOWNLOADED),
            item(4, read = false, state = Download.State.QUEUE),
            item(5, read = true, state = Download.State.NOT_DOWNLOADED),
        )
        items.toTsuzukiUpdatesSummary() shouldBe TsuzukiUpdatesSummary(newCount = 4, toSave = 2)
        items.unreadUpdatesToSave().map { it.update.chapterId } shouldBe listOf(1L, 2L)
    }
}
