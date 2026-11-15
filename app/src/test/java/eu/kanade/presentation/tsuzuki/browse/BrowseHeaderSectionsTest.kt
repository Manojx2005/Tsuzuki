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

    @Test
    fun `anime pages are absent when the caller does not provide installed sources`() {
        browsePagesFor(browsePages(hideManga = false), BrowseMedia.ANIME) shouldBe emptyList()
    }

    @Test
    fun `anime repos expose sources and extensions even with no sources installed`() {
        val pages = browsePages(hideManga = true, animeEnabled = true, hasAnimeRepos = true)
        browsePagesFor(pages, BrowseMedia.ANIME) shouldBe
            listOf(BrowsePage.ANIME_SOURCES, BrowsePage.ANIME_EXTENSIONS)
    }

    @Test
    fun `installed anime sources remain accessible when there are no repos`() {
        val pages = browsePages(hideManga = true, animeEnabled = true)
        browsePagesFor(pages, BrowseMedia.ANIME) shouldBe listOf(BrowsePage.ANIME_SOURCES)
    }

    @Test
    fun `anime migration requires the beta gate and at least one library anime`() {
        val pages = browsePages(hideManga = true, animeEnabled = true, hasAnimeLibrary = true)
        browsePagesFor(pages, BrowseMedia.ANIME) shouldBe
            listOf(BrowsePage.ANIME_SOURCES, BrowsePage.ANIME_MIGRATION)
        browsePages(hideManga = false, animeEnabled = false, hasAnimeLibrary = true)
            .pageIndex(BrowseMedia.ANIME, BrowseSection.MIGRATION) shouldBe -1
    }
}
