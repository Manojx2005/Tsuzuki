package eu.kanade.tachiyomi.tsuzuki.download

import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.CatalogueSource
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class DownloadQueueSchedulingTest {
    private val mangaA = source(1)
    private val mangaB = source(2)
    private val mangaC = source(3)
    private val novel = source(4, novel = true)

    @Test
    fun `pausing a manga source releases its slot without affecting other media or queue order`() {
        val queue = listOf(
            download(novel, 10),
            download(mangaA, 11),
            download(mangaB, 12),
            download(mangaA, 13),
            download(mangaC, 14),
        )

        activeMangaDownloads(queue, 2, emptySet()).map { it.chapterId } shouldBe listOf(11L, 12L)
        activeMangaDownloads(queue, 2, setOf(1)).map { it.chapterId } shouldBe listOf(12L, 14L)
        activeMangaDownloads(queue, 2, emptySet()).map { it.chapterId } shouldBe listOf(11L, 12L)
    }

    @Test
    fun `targeted worker start leaves other errors in the queue`() {
        val failed = download(mangaA, 11).apply { status = Download.State.ERROR }
        val retry = download(mangaB, 12)
        val finished = download(mangaC, 13).apply { status = Download.State.DOWNLOADED }
        val queue = listOf(failed, retry, finished)

        pendingDownloadsForStart(queue, retryErrors = false) shouldBe listOf(retry)
        pendingDownloadsForStart(queue, retryErrors = true) shouldBe listOf(failed, retry)
        activeMangaDownloads(queue, 3, emptySet()) shouldBe listOf(retry)
    }

    @Test
    fun `retry wakes the existing queue and leaves other failures and paused sources alone`() {
        val paused = download(mangaA, 11).apply { status = Download.State.ERROR }
        val other = download(mangaB, 12).apply { status = Download.State.ERROR }
        val queue = MutableStateFlow(listOf(paused, other))
        var starts = 0
        val controls = DownloadQueueControls(queue, MutableStateFlow(emptySet())) { starts++ }

        controls.setMangaSourcePaused(mangaA.id, true)
        controls.retryFailedDownloads(setOf(paused.chapterId))

        paused.status shouldBe Download.State.QUEUE
        other.status shouldBe Download.State.ERROR
        controls.changes.value.revision shouldBe 1L
        starts shouldBe 0

        controls.retryFailedDownloads(setOf(other.chapterId))
        other.status shouldBe Download.State.QUEUE
        controls.changes.value.revision shouldBe 2L
        starts shouldBe 1
    }

    @Test
    fun `resume wakes a waiting source after its cancelled chapter releases the queue entry`() = runTest {
        val chapter = download(mangaA, 11)
        val queue = MutableStateFlow(listOf(chapter))
        val controls = DownloadQueueControls(queue, MutableStateFlow(emptySet())) {}
        var attemptStillRunning = true
        controls.setMangaSourcePaused(mangaA.id, true)
        val ready = async(start = CoroutineStart.UNDISPATCHED) {
            controls.mangaQueue(mangaA.id).first { it.isNotEmpty() && !attemptStillRunning }
        }

        controls.setMangaSourcePaused(mangaA.id, false)
        runCurrent()
        ready.isCompleted shouldBe false

        attemptStillRunning = false
        controls.attemptFinished(chapter)
        ready.await() shouldBe listOf(chapter)
    }

    private fun source(sourceId: Long, novel: Boolean = false) = mockk<CatalogueSource> {
        every { id } returns sourceId
        every { isNovelSource } returns novel
    }

    private fun download(source: CatalogueSource, chapterId: Long) = Download(
        source = source,
        mangaId = 1,
        mangaTitle = "Book",
        chapterId = chapterId,
        chapterName = "Chapter $chapterId",
        chapterUrl = "/$chapterId",
        chapterScanlator = null,
        chapterDateUpload = 0,
        chapterNumber = chapterId.toDouble(),
    ).apply { status = Download.State.QUEUE }
}
