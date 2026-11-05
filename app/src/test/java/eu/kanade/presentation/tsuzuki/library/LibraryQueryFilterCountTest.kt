package eu.kanade.presentation.tsuzuki.library

import eu.kanade.tachiyomi.tsuzuki.LibraryQuery
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LibraryQueryFilterCountTest {

    @Test
    fun `an empty query has no active filters`() {
        LibraryQuery(id = "a", name = "Empty").activeFilterCount() shouldBe 0
    }

    @Test
    fun `tri states tags and extensions each add to the count`() {
        val query = LibraryQuery(
            id = "a",
            name = "Busy",
            triStates = mapOf("unread" to "ENABLED_IS", "downloaded" to "ENABLED_NOT"),
            includedTags = setOf("fantasy"),
            excludedTags = setOf("romance", "horror"),
            filterExtensions = setOf("ext1"),
            excludedExtensions = setOf("ext2"),
        )
        query.activeFilterCount() shouldBe 7
    }

    @Test
    fun `sort and threshold do not count as filters`() {
        LibraryQuery(id = "a", name = "Sorted", sort = "ALPHABETICAL,ASCENDING", chapterCountThreshold = 99)
            .activeFilterCount() shouldBe 0
    }
}
