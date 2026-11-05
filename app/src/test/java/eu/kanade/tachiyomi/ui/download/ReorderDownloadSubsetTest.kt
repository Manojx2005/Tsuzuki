package eu.kanade.tachiyomi.ui.download

import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.CatalogueSource
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test

class ReorderDownloadSubsetTest {
    private val source = mockk<CatalogueSource>()

    @Test
    fun `reordering preserves slots for other media and newly queued chapters`() {
        val first = download(1)
        val second = download(2)
        val novel = download(3)
        val newlyQueued = download(4)
        val current = listOf(first, novel, second, newlyQueued)

        reorderDownloadSubset(current, listOf(second, first)) shouldBe listOf(second, novel, first, newlyQueued)
    }

    @Test
    fun `reordering skips a chapter that completed after the drag began`() {
        val first = download(1)
        val second = download(2)
        val completed = download(3)
        val other = download(4)
        val current = listOf(first, other, second)

        reorderDownloadSubset(current, listOf(second, completed, second, first)) shouldBe listOf(second, other, first)
    }

    private fun download(id: Long) = Download(
        source = source,
        mangaId = 1,
        mangaTitle = "Book",
        chapterId = id,
        chapterName = "Chapter $id",
        chapterUrl = "/$id",
        chapterScanlator = null,
        chapterDateUpload = 0,
        chapterNumber = id.toDouble(),
    )
}
