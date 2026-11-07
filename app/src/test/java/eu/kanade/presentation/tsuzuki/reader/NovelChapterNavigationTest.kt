package eu.kanade.presentation.tsuzuki.reader

import eu.kanade.tachiyomi.tsuzuki.reader.PendingAdjacentNavigation
import eu.kanade.tachiyomi.tsuzuki.reader.adjacentNovelChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class NovelChapterNavigationTest {
    private val chapters = (1L..4L).map { ReaderChapter(Chapter.create().copy(id = it)) }
    private val active = ViewerChapters(chapters[0], null, chapters[1])

    @Test
    fun `previous entry is used once even if a renderer receives the same chapter snapshot again`() {
        val previous = active.copy(previousEntry = ViewerChapters.PreviousEntry())
        previous.takePreviousEntry() shouldBe true
        previous.takePreviousEntry() shouldBe false
        active.takePreviousEntry() shouldBe false
    }

    @Test
    fun `a second previous tap targeting the same chapter still emits a new entry`() {
        val first = active.copy(previousEntry = ViewerChapters.PreviousEntry())
        val second = active.copy(previousEntry = ViewerChapters.PreviousEntry())
        first shouldNotBe second
        second.takePreviousEntry() shouldBe true
    }

    @Test
    fun `infinite scroll follows the visible chapter including when it has no next chapter`() {
        adjacentNovelChapter(active, chapters, 3, forward = true, infiniteScroll = true, paged = false) shouldBe
            chapters[3]
        adjacentNovelChapter(active, chapters, 3, forward = false, infiniteScroll = true, paged = false) shouldBe
            chapters[1]
        adjacentNovelChapter(active, chapters, 4, forward = true, infiniteScroll = true, paged = false) shouldBe null
    }

    @Test
    fun `paged WebView navigates from the current chapter even with stale scroll metadata`() {
        adjacentNovelChapter(active, chapters, 3, forward = true, infiniteScroll = true, paged = true) shouldBe
            chapters[1]
        adjacentNovelChapter(active, chapters, 3, forward = false, infiniteScroll = true, paged = true) shouldBe null
    }

    @Test
    fun `the last tap during a load replays exactly once against the new active chapter`() {
        val pending = PendingAdjacentNavigation()
        pending.deferIfLoading(isLoading = true, forward = false) shouldBe true
        pending.deferIfLoading(isLoading = true, forward = true) shouldBe true
        pending.takeWhenIdle(isLoading = true) shouldBe null
        val forward = requireNotNull(pending.takeWhenIdle(isLoading = false))
        val newlyActive = ViewerChapters(chapters[1], chapters[0], chapters[2])
        adjacentNovelChapter(newlyActive, chapters, null, forward, infiniteScroll = true, paged = false) shouldBe
            chapters[2]
        pending.takeWhenIdle(isLoading = false) shouldBe null
        pending.deferIfLoading(isLoading = false, forward = true) shouldBe false
    }
}
