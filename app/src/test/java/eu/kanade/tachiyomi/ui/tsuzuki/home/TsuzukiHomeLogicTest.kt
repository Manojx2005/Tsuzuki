package eu.kanade.tachiyomi.ui.tsuzuki.home

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover
import java.util.concurrent.atomic.AtomicInteger

class TsuzukiHomeLogicTest {

    private fun book(id: Long) = HomeBook(
        mangaId = id,
        chapterId = null,
        title = "Book $id",
        cover = MangaCover(mangaId = id, sourceId = 0, isMangaFavorite = false, url = null, lastModified = 0),
        chapterLabel = null,
        progress = null,
        unread = 0,
        isNovel = true,
    )

    @Test
    fun `chapter number drops the decimal point for whole chapters`() {
        homeChapterNumber(12.0) shouldBe "12"
        homeChapterNumber(0.0) shouldBe "0"
    }

    @Test
    fun `chapter number keeps fractions and unknown numbers`() {
        homeChapterNumber(12.5) shouldBe "12.5"
        homeChapterNumber(-1.0) shouldBe "-1.0"
    }

    @Test
    fun `chapter label is null for an unknown number`() {
        homeChapterLabel(-1.0) shouldBe null
    }

    @Test
    fun `chapter label formats whole and fractional numbers`() {
        homeChapterLabel(336.0) shouldBe "Ch. 336"
        homeChapterLabel(0.0) shouldBe "Ch. 0"
        homeChapterLabel(7.5) shouldBe "Ch. 7.5"
    }

    @Test
    fun `read wins over every other status`() {
        homeChapterStatus(read = true, lastPageRead = 5, dateFetch = 200, lastReadAt = 100) shouldBe ChapterStatus.READ
    }

    @Test
    fun `a started chapter is in progress even when newly fetched`() {
        homeChapterStatus(false, 3, 200, 100) shouldBe ChapterStatus.IN_PROGRESS
    }

    @Test
    fun `an untouched chapter fetched after the last read is new`() {
        homeChapterStatus(false, 0, 200, 100) shouldBe ChapterStatus.NEW
    }

    @Test
    fun `an untouched chapter fetched before or at the last read is unread`() {
        homeChapterStatus(false, 0, 100, 100) shouldBe ChapterStatus.UNREAD
        homeChapterStatus(false, 0, 50, 100) shouldBe ChapterStatus.UNREAD
    }

    @Test
    fun `a book never read has no new chapters`() {
        homeChapterStatus(false, 0, 200, Long.MAX_VALUE) shouldBe ChapterStatus.UNREAD
    }

    @Test
    fun `state is empty only after loading finishes with no books`() {
        HomeState().isEmpty shouldBe false
        HomeState(loading = false).isEmpty shouldBe true
        HomeState(loading = false, books = listOf(book(1))).isEmpty shouldBe false
    }

    @Test
    fun `hero is the first book and the shelf is the rest`() {
        val state = HomeState(loading = false, books = listOf(book(1), book(2), book(3)))
        state.hero?.mangaId shouldBe 1
        state.shelf.map { it.mangaId } shouldBe listOf(2L, 3L)
    }

    @Test
    fun `no books means no hero and an empty shelf`() {
        HomeState().hero shouldBe null
        HomeState().shelf shouldBe emptyList()
    }

    @Test
    fun `badge changes leave the nine book stack loaded`() = runTest {
        val updates = MutableStateFlow(0)
        val history = MutableStateFlow(emptyList<HistoryWithRelations>())
        val library = MutableStateFlow(emptyList<LibraryManga>())
        val stack = (1L..9L).map(::book)
        val loads = AtomicInteger()
        val emitted = Channel<HomeState>()
        backgroundScope.launch {
            homeStates(history, library, updates) { _, _ ->
                loads.incrementAndGet()
                HomeState(loading = false, books = stack)
            }.collect { emitted.send(it) }
        }

        emitted.receive().books shouldBe stack
        repeat(20) { index ->
            updates.value = index + 1
            val state = emitted.receive()
            state.books shouldBe stack
            state.newChapters shouldBe index + 1
        }
        loads.get() shouldBe 1
    }
}
