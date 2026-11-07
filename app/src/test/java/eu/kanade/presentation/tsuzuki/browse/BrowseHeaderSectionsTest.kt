package eu.kanade.presentation.tsuzuki.browse

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BrowseHeaderSectionsTest {
    @Test
    fun `the header always shows Sources, Extensions and Migrate`() {
        browseHeaderSections shouldBe listOf(BrowseSection.SOURCES, BrowseSection.EXTENSIONS, BrowseSection.MIGRATION)
    }

    @Test
    fun `swiping stays inside the chosen media`() {
        val all = browsePages(hideManga = false)
        browsePagesFor(all, BrowseMedia.NOVELS) shouldBe
            listOf(BrowsePage.NOVEL_SOURCES, BrowsePage.NOVEL_EXTENSIONS, BrowsePage.NOVEL_MIGRATION)
        browsePagesFor(all, BrowseMedia.MANGA) shouldBe
            listOf(BrowsePage.MANGA_SOURCES, BrowsePage.MANGA_EXTENSIONS, BrowsePage.MANGA_MIGRATION)
    }

    @Test
    fun `each media keeps the same section at the same page`() {
        val all = browsePages(hideManga = false)
        for (section in BrowseSection.entries) {
            browsePagesFor(all, BrowseMedia.NOVELS).pageIndex(BrowseMedia.NOVELS, section) shouldBe
                browsePagesFor(all, BrowseMedia.MANGA).pageIndex(BrowseMedia.MANGA, section)
        }
    }

    @Test
    fun `without manga only the novel pages exist`() {
        browsePagesFor(browsePages(hideManga = true), BrowseMedia.MANGA) shouldBe emptyList()
    }
}
