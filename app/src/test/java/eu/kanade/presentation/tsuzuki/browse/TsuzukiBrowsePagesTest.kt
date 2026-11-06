package eu.kanade.presentation.tsuzuki.browse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TsuzukiBrowsePagesTest {

    @Test
    fun `two axes preserve the existing full pager order`() {
        val pages = browsePages(hideManga = false)
        assertEquals(BrowsePage.entries, pages)
        assertEquals(0, pages.pageIndex(BrowseMedia.NOVELS, BrowseSection.SOURCES))
        assertEquals(1, pages.pageIndex(BrowseMedia.MANGA, BrowseSection.SOURCES))
        assertEquals(2, pages.pageIndex(BrowseMedia.NOVELS, BrowseSection.EXTENSIONS))
        assertEquals(3, pages.pageIndex(BrowseMedia.MANGA, BrowseSection.EXTENSIONS))
        assertEquals(4, pages.pageIndex(BrowseMedia.NOVELS, BrowseSection.MIGRATION))
        assertEquals(5, pages.pageIndex(BrowseMedia.MANGA, BrowseSection.MIGRATION))
    }

    @Test
    fun `hidden manga leaves the three novel pages and the correct extension shortcut`() {
        val pages = browsePages(hideManga = true)
        assertEquals(listOf(BrowsePage.NOVEL_SOURCES, BrowsePage.NOVEL_EXTENSIONS, BrowsePage.NOVEL_MIGRATION), pages)
        assertEquals(1, pages.pageIndex(BrowseMedia.NOVELS, BrowseSection.EXTENSIONS))
        assertEquals(2, pages.pageIndex(BrowseMedia.NOVELS, BrowseSection.MIGRATION))
        assertEquals(-1, pages.pageIndex(BrowseMedia.MANGA, BrowseSection.EXTENSIONS))
    }
}
