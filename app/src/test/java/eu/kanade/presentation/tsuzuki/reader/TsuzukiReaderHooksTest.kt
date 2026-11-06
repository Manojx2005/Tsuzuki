package eu.kanade.presentation.tsuzuki.reader

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class TsuzukiReaderHooksTest {
    @Test
    fun `legacy reader keeps distinct open and close animations`() {
        readerTransitionAnimations(opening = true, themedAnimations = null) shouldBe
            (R.anim.shared_axis_x_push_enter to R.anim.shared_axis_x_push_exit)
        readerTransitionAnimations(opening = false, themedAnimations = null) shouldBe
            (R.anim.shared_axis_x_pop_enter to R.anim.shared_axis_x_pop_exit)
    }

    @Test
    fun `theme animation pair is used for either direction`() {
        val themed = R.anim.tsuzuki_reader_fade_in to R.anim.tsuzuki_reader_fade_out
        readerTransitionAnimations(opening = true, themedAnimations = themed) shouldBe themed
        readerTransitionAnimations(opening = false, themedAnimations = themed) shouldBe themed
    }

    @Test
    fun `contents follows the visible novel chapter and falls back to the current viewer chapter`() {
        val current = ReaderChapter(Chapter.create().copy(id = 10))
        val visible = ReaderChapter(Chapter.create().copy(id = 11)).chapter
        val state = ReaderViewModel.State(viewerChapters = ViewerChapters(current, null, null))

        state.copy(novelVisibleChapter = visible).contentsChapterId() shouldBe 11L
        state.contentsChapterId() shouldBe 10L
        ReaderViewModel.State().contentsChapterId() shouldBe null
    }

    @Test
    fun `adjacency does not read the ordered list before the viewer chapter is ready`() {
        ReaderViewModel.State().resolveTsuzukiAdjacentChapter(
            forward = true,
            ordered = { error("Chapter list should be lazy") },
            infiniteScrollEnabled = { error("Preference should be lazy") },
        ) shouldBe null
    }

    @Test
    fun `chapter end marks the loaded first page complete`() {
        val chapter = ReaderChapter(Chapter.create())
        val saved = mutableListOf<Pair<ReaderPage, Int>>()
        val saveProgress: (ReaderPage, Int) -> Unit = { page, progress -> saved.add(page to progress) }
        markTsuzukiChapterRead(chapter, saveProgress)
        saved shouldBe emptyList()

        val first = ReaderPage(0)
        chapter.state = ReaderChapter.State.Loaded(listOf(first, ReaderPage(1)))
        markTsuzukiChapterRead(chapter, saveProgress)
        saved shouldBe listOf(first to 100)
    }
}
