package eu.kanade.presentation.tsuzuki.track

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TrackerChapterProgressTest {
    @Test
    fun `shows real known chapter total`() {
        trackerChapterProgress(12.0, 26L) shouldBe "12 / 26"
    }

    @Test
    fun `omits unknown chapter total`() {
        trackerChapterProgress(12.0, 0L) shouldBe "12"
    }

    @Test
    fun `keeps stock whole chapter display`() {
        trackerChapterProgress(12.5, 26L) shouldBe "12 / 26"
    }
}
