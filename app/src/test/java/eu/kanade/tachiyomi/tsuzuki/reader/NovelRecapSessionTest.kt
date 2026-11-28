package eu.kanade.tachiyomi.tsuzuki.reader

import android.content.Context
import android.util.AtomicFile
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.download.ChapterContentReader
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.domain.history.model.History
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.LocalNovelSource
import java.io.File
import java.io.FileNotFoundException
import java.util.Date

class NovelRecapSessionTest {
    @TempDir lateinit var cache: File

    @Test
    fun coldLocalRecapLoadsTextFromUrlOnlyPage() = runTest {
        withColdCache {
            val source = mockk<LocalNovelSource>()
            val chapter = chapter()
            val page = Page(0, url = "localnovels/book/chapter-1.txt")
            val first = "The archivist lowered the ledger onto the table."
            val second = "She heard the bells ring from the locked tower."
            coEvery { source.getPageList(chapter.chapter) } returns listOf(page)
            coEvery { source.fetchPageText(page) } returns "$first\n\n$second"
            val session = session()

            session.prepare(Manga.create().copy(id = 1), 3, chapter, source)

            assertEquals(listOf(first, second), session.forChapter(3)?.paragraphs)
            coVerify(exactly = 1) { source.fetchPageText(page) }
            session.dismiss(3)
            assertEquals(null, session.forChapter(3))
        }
    }

    @Test
    fun onlineNovelWithNoOfflineTextFetchesPreviousChapterFromSource() = kotlinx.coroutines.runBlocking {
        // Real time: the fetch runs on Dispatchers.IO, which runTest's virtual clock would time out.
        withColdCache {
            val source = mockk<eu.kanade.tachiyomi.source.Source>()
            val chapter = chapter()
            val page = Page(0, url = "/novel/chapter-1")
            val first = "The archivist lowered the ledger onto the table."
            val second = "She heard the bells ring from the locked tower."
            every { source.isNovelSource } returns true
            coEvery { source.getPageList(chapter.chapter) } returns listOf(page)
            coEvery { source.fetchPageText(page) } returns "<p>$first</p><p>$second</p>"
            val session = session()

            session.prepare(Manga.create().copy(id = 1), 3, chapter, source)

            assertEquals(listOf(first, second), session.forChapter(3)?.paragraphs)
        }
    }

    @Test
    fun onlineFetchFailureStillOpensWithoutRecap() = runTest {
        withColdCache {
            val source = mockk<eu.kanade.tachiyomi.source.Source>()
            every { source.isNovelSource } returns true
            coEvery { source.getPageList(any()) } throws java.io.IOException("offline")
            val session = session()

            session.prepare(Manga.create().copy(id = 1), 3, chapter(), source)

            assertEquals(null, session.forChapter(3))
        }
    }

    @Test
    fun blockingSourceDoesNotHoldTheChapterOpenPastTheTimeout() {
        kotlinx.coroutines.runBlocking {
            withColdCache {
                val source = mockk<eu.kanade.tachiyomi.source.Source>()
                every { source.isNovelSource } returns true
                coEvery { source.getPageList(any()) } answers {
                    Thread.sleep(10_000)
                    emptyList()
                }
                val session = session()
                val started = System.nanoTime()

                session.prepare(Manga.create().copy(id = 1), 3, chapter(), source)

                assertEquals(null, session.forChapter(3))
                assert((System.nanoTime() - started) / 1_000_000 < 6_000)
            }
        }
    }

    @Test
    fun previousChapterIgnoresReaderFilters() {
        val all = listOf(1L, 2L, 3L, 4L)
        assertEquals(3L, previousChapterForRecap(all, 4L) { it })
        assertEquals(null, previousChapterForRecap(all, 1L) { it })
        assertEquals(null, previousChapterForRecap(all, 99L) { it })
    }

    @Test
    fun resumedChapterGetsAnOverlayFromTheTextBeforeThePosition() = runTest {
        withColdCache {
            val source = mockk<LocalNovelSource>()
            val chapter = chapter()
            val page = Page(0, url = "local")
            val previousFirst = "The archivist lowered the ledger onto the table."
            val previousSecond = "She heard the bells ring from the locked tower."
            coEvery { source.getPageList(chapter.chapter) } returns listOf(page)
            coEvery { source.fetchPageText(page) } returns "$previousFirst\n\n$previousSecond"
            val session = session()
            val current = listOf(
                "A light came on in the window above the gate.",
                "The stranger stepped out into the rain without a word.",
                "Morning came grey and quiet over the sleeping harbour.",
                "Nobody on the quay remembered the stranger's face.",
            )

            session.prepare(Manga.create().copy(id = 1), 3, chapter, source, entryProgress = 0.8f)

            // Nothing is injected above the text; the overlay waits for the chapter's own text.
            assertEquals(null, session.forChapter(3))
            assertEquals(null, session.overlay.value)
            session.onResumeViewport(3, current.take(3))
            assertEquals(current.take(3), session.overlay.value?.paragraphs)
            assertEquals(80, session.overlay.value?.resumePercent)
            session.dismiss(3)
            assertEquals(null, session.overlay.value)
        }
    }

    @Test
    fun resumeInTheOpeningParagraphsFallsBackToThePreviousChapter() = runTest {
        withColdCache {
            val source = mockk<LocalNovelSource>()
            val chapter = chapter()
            val page = Page(0, url = "local")
            val first = "The archivist lowered the ledger onto the table."
            val second = "She heard the bells ring from the locked tower."
            coEvery { source.getPageList(chapter.chapter) } returns listOf(page)
            coEvery { source.fetchPageText(page) } returns "$first\n\n$second"
            val session = session()

            session.prepare(Manga.create().copy(id = 1), 3, chapter, source, entryProgress = 0.05f)
            session.onResumeViewport(3, listOf("Only one completed opening paragraph is long enough here."))

            assertEquals(listOf(first, second), session.overlay.value?.paragraphs)
        }
    }

    @Test
    fun resumedFirstChapterWithNoPreviousStillGetsAnOverlay() = runTest {
        withColdCache {
            val session = session()
            val text = listOf(
                "A light came on in the window above the gate.",
                "The stranger stepped out into the rain without a word.",
                "Morning came grey and quiet over the sleeping harbour.",
                "Nobody on the quay remembered the stranger's face.",
            )

            session.prepare(Manga.create().copy(id = 1), 3, null, mockk(), entryProgress = 0.6f)
            session.onResumeViewport(3, text.take(2))

            assertEquals(text.take(2), session.overlay.value?.paragraphs)
        }
    }

    @Test
    fun actualViewportWinsOverSavedScrollRatioAndDismissedReportsStayDismissed() = runTest {
        withColdCache {
            val session = session()
            val completed = listOf(
                "The first completed paragraph is entirely above the restored viewport.",
                "The second completed paragraph is also entirely above the restored viewport.",
            )
            session.prepare(Manga.create().copy(id = 1), 3, null, mockk(), entryProgress = 0.5f)
            session.onResumeViewport(4, completed)
            assertEquals(null, session.overlay.value)
            session.onResumeViewport(3, completed)
            assertEquals(completed, session.overlay.value?.paragraphs)
            assertEquals(50, session.overlay.value?.resumePercent)
            session.dismiss(3)
            session.onResumeViewport(3, completed)
            assertEquals(null, session.overlay.value)
            session.prepare(Manga.create().copy(id = 1), 3, null, mockk(), entryProgress = 0.5f)
            session.dismiss(3)
            session.onResumeViewport(3, completed)
            assertEquals(null, session.overlay.value)
        }
    }

    @Test
    fun chapterSwitchWhileViewportSelectionRunsCannotPublishTheOldOverlay() = runTest {
        withColdCache {
            val session = session()
            session.prepare(Manga.create().copy(id = 1), 3, null, mockk(), entryProgress = 0.5f)
            val started = java.util.concurrent.CountDownLatch(1)
            val release = java.util.concurrent.CountDownLatch(1)
            val paragraphs = object : AbstractList<String>() {
                override val size = 2
                override fun get(index: Int): String {
                    if (index == 0) {
                        started.countDown()
                        check(release.await(3, java.util.concurrent.TimeUnit.SECONDS))
                    }
                    return "Completed paragraph $index is entirely before the restored viewport."
                }
            }
            val workerFailure = java.util.concurrent.atomic.AtomicReference<Throwable?>()
            val worker = kotlin.concurrent.thread {
                try {
                    session.onResumeViewport(3, paragraphs)
                } catch (error: Throwable) {
                    workerFailure.set(error)
                }
            }
            try {
                check(started.await(3, java.util.concurrent.TimeUnit.SECONDS))
                session.onChapterChanged(4)
            } finally {
                release.countDown()
                worker.join(3000)
            }
            check(!worker.isAlive)
            workerFailure.get()?.let { throw it }
            assertEquals(null, session.overlay.value)
            assertEquals(false, session.needsViewport(3))
        }
    }

    @Test
    fun localFetchCancellationPropagates() {
        assertThrows(CancellationException::class.java) {
            runTest {
                withColdCache {
                    val source = mockk<LocalNovelSource>()
                    coEvery { source.getPageList(any()) } returns listOf(Page(0, url = "local"))
                    coEvery { source.fetchPageText(any()) } throws CancellationException("reader closed")
                    session().prepare(Manga.create().copy(id = 1), 3, chapter(), source)
                }
            }
        }
    }

    private fun session(): NovelRecapSession {
        val context = mockk<Context> { every { cacheDir } returns cache }
        return NovelRecapSession(context, mockk()) {
            listOf(History(1, 2, Date(1_000), 0))
        }
    }

    private fun chapter() = ReaderChapter(
        mockk<Chapter>(relaxed = true) {
            every { id } returns 2
            every { manga_id } returns 1
        },
    )

    private suspend fun withColdCache(block: suspend () -> Unit) {
        mockkConstructor(AtomicFile::class, ChapterContentReader::class)
        try {
            every { anyConstructed<AtomicFile>().readFully() } throws FileNotFoundException()
            coEvery { anyConstructed<ChapterContentReader>().readDownloadedContent(any(), any(), any()) } returns null
            block()
        } finally {
            unmockkConstructor(AtomicFile::class, ChapterContentReader::class)
        }
    }
}
