package eu.kanade.tachiyomi.ui.download

import eu.kanade.tachiyomi.data.download.model.Download
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

/** The novel queue row status is a translated resource for every queue state. */
class NovelDownloadItemStatusTest {

    private fun download(state: Download.State): Download = mockk {
        every { status } returns state
    }

    private fun item(vararg states: Download.State, initialTotal: Int = states.size) = NovelDownloadItem(
        mangaId = 1L,
        mangaTitle = "Book",
        sourceName = "Source",
        subItems = states.map(::download),
        initialTotal = initialTotal,
    )

    @Test
    fun `an error wins over everything else`() {
        assertEquals(MR.strings.chapter_error, item(Download.State.DOWNLOADING, Download.State.ERROR).status)
    }

    @Test
    fun `a chapter in progress reads as downloading`() {
        assertEquals(MR.strings.ext_downloading, item(Download.State.DOWNLOADING, Download.State.QUEUE).status)
    }

    @Test
    fun `only queued chapters read as queued`() {
        assertEquals(MR.strings.tsz_downloads_queued, item(Download.State.QUEUE, Download.State.QUEUE).status)
    }

    @Test
    fun `nothing left of the batch reads as done`() {
        assertEquals(MR.strings.tsz_download_done, item(initialTotal = 0).status)
    }

    @Test
    fun `anything else reads as pending`() {
        // One chapter already finished and left the queue; the remaining one is not queued yet.
        assertEquals(MR.strings.tsz_download_pending, item(Download.State.NOT_DOWNLOADED, initialTotal = 2).status)
    }
}
