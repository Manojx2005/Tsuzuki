package eu.kanade.tachiyomi.tsuzuki.reader

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovelTtsPlaybackTest {

    @Test
    fun `hand-off anchors on the playing chapter, not the viewport`() {
        // Chapters 1 and 2 loaded; chapter 2 (index 1) just finished while the viewport sits on index 0.
        val anchor = ttsHandoffAnchorIndex(listOf(1L, 2L), playbackChapterId = 2L, fallbackIndex = 0)
        anchor shouldBe 1
        // Nothing loaded after it, so the caller must stop instead of replaying index 1.
        (anchor + 1 in listOf(1L, 2L).indices) shouldBe false
    }

    @Test
    fun `hand-off survives queue trimming because it matches by id`() {
        ttsHandoffAnchorIndex(listOf(5L, 6L, 7L), playbackChapterId = 6L, fallbackIndex = 2) shouldBe 1
    }

    @Test
    fun `hand-off falls back to the viewport when nothing is playing or the id is gone`() {
        ttsHandoffAnchorIndex(listOf(1L, 2L), playbackChapterId = null, fallbackIndex = 1) shouldBe 1
        ttsHandoffAnchorIndex(listOf(1L, 2L), playbackChapterId = 9L, fallbackIndex = 0) shouldBe 0
    }

    @Test
    fun `dock shows the playing chapter while speaking`() {
        listenDockChapterTitle(2L, "Chapter 2", "Chapter 1") shouldBe "Chapter 2"
    }

    @Test
    fun `dock falls back to the viewport chapter when nothing is playing or the title is unknown`() {
        listenDockChapterTitle(null, null, "Chapter 1") shouldBe "Chapter 1"
        listenDockChapterTitle(null, "Chapter 2", "Chapter 1") shouldBe "Chapter 1"
        listenDockChapterTitle(2L, null, "Chapter 1") shouldBe "Chapter 1"
    }
}
