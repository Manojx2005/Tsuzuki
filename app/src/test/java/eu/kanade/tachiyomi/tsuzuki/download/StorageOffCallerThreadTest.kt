package eu.kanade.tachiyomi.tsuzuki.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.download.ChapterContentReader
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.ui.reader.loader.DownloadPageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.StubSource
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import tachiyomi.domain.chapter.model.Chapter as DomainChapter

/**
 * Storage lookups that used to run on whatever thread called them must now leave the caller's thread. Each test
 * calls from a dedicated single thread (standing in for the main thread) and records the thread the mocked storage
 * was hit on.
 */
class StorageOffCallerThreadTest {
    private val callerExecutor = Executors.newSingleThreadExecutor { Thread(it, "fake-main") }
    private val caller = callerExecutor.asCoroutineDispatcher()
    private val storageThread = AtomicReference<Thread>()

    @AfterEach
    fun tearDown() {
        callerExecutor.shutdownNow()
    }

    private fun onCaller(block: suspend (callerThread: Thread) -> Unit) = runBlocking(caller) {
        block(Thread.currentThread())
    }

    private fun assertOffCaller(callerThread: Thread) {
        val thread = storageThread.get()
        (thread == null) shouldBe false
        (thread === callerThread) shouldBe false
    }

    private val domainChapter = DomainChapter.create().copy(name = "Chapter 1", url = "/c/1")
    private val manga = Manga.create().copy(title = "Book")
    private val source = StubSource(1, "en", "Test")

    @Test
    fun `isChapterOnDisk probes storage off the caller thread with the cache skipped`() = onCaller { callerThread ->
        val downloads = mockk<DownloadManager>()
        every { downloads.isChapterDownloaded("Chapter 1", null, "/c/1", "Book", 1L, true) } answers {
            storageThread.set(Thread.currentThread())
            true
        }

        downloads.isChapterOnDisk("Chapter 1", null, "/c/1", "Book", 1L) shouldBe true

        assertOffCaller(callerThread)
    }

    @Test
    fun `reading downloaded content looks up the chapter folder off the caller thread`() = onCaller { callerThread ->
        val provider = mockk<DownloadProvider>()
        every { provider.findChapterDir(any(), any(), any(), any(), any()) } answers {
            storageThread.set(Thread.currentThread())
            null
        }
        every { provider.findMangaDir(any(), any()) } returns null

        ChapterContentReader(mockk<Context>(), provider).readDownloadedContent(manga, domainChapter, source) shouldBe
            null

        assertOffCaller(callerThread)
    }

    @Test
    fun `reading chapter images looks up the chapter folder off the caller thread`() = onCaller { callerThread ->
        val provider = mockk<DownloadProvider>()
        every { provider.findChapterDir(any(), any(), any(), any(), any()) } answers {
            storageThread.set(Thread.currentThread())
            null
        }
        every { provider.findMangaDir(any(), any()) } returns null

        ChapterContentReader(mockk<Context>(), provider).readChapterImages(manga, domainChapter, source) shouldBe
            emptyMap()

        assertOffCaller(callerThread)
    }

    @Test
    fun `download page loader resolves page data off the caller thread`() = onCaller { callerThread ->
        val provider = mockk<DownloadProvider>()
        every { provider.findChapterDir(any(), any(), any(), any(), any()) } answers {
            storageThread.set(Thread.currentThread())
            mockk<UniFile> { every { isDirectory } returns false }
        }
        val loader = DownloadPageLoader(readerChapter(), manga, source, mockk<DownloadManager>(), provider)

        loader.getPageDataStream("image.png") shouldBe null

        assertOffCaller(callerThread)
    }

    @Test
    fun `download page loader finds the chapter folder off the caller thread`() = onCaller { callerThread ->
        val provider = mockk<DownloadProvider>()
        every { provider.findChapterDir(any(), any(), any(), any(), any()) } answers {
            storageThread.set(Thread.currentThread())
            error("stop after the lookup")
        }
        val loader = DownloadPageLoader(readerChapter(), manga, source, mockk<DownloadManager>(), provider)

        runCatching { loader.getPages() }.isFailure shouldBe true

        assertOffCaller(callerThread)
    }

    private fun readerChapter(): ReaderChapter {
        val chapter = mockk<Chapter>(relaxed = true)
        every { chapter.name } returns "Chapter 1"
        every { chapter.url } returns "/c/1"
        every { chapter.scanlator } returns null
        return ReaderChapter(chapter)
    }
}
