package eu.kanade.tachiyomi.tsuzuki.reader

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovelNextChapterWarmupTest {

    @Test
    fun `warms up from three quarters of the chapter`() {
        shouldWarmNextChapter(0) shouldBe false
        shouldWarmNextChapter(74) shouldBe false
        shouldWarmNextChapter(75) shouldBe true
        shouldWarmNextChapter(100) shouldBe true
    }
}
