package eu.kanade.presentation.tsuzuki.browse

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BrowseHeaderSectionsTest {
    @Test
    fun `Sources and Extensions show two segments`() {
        browseHeaderSections(BrowseSection.SOURCES) shouldBe listOf(BrowseSection.SOURCES, BrowseSection.EXTENSIONS)
        browseHeaderSections(BrowseSection.EXTENSIONS) shouldBe listOf(BrowseSection.SOURCES, BrowseSection.EXTENSIONS)
    }

    @Test
    fun `Migrate adds itself as the selected third segment`() {
        val sections = browseHeaderSections(BrowseSection.MIGRATION)
        sections shouldBe listOf(BrowseSection.SOURCES, BrowseSection.EXTENSIONS, BrowseSection.MIGRATION)
        sections.indexOf(BrowseSection.MIGRATION) shouldBe 2
    }
}
