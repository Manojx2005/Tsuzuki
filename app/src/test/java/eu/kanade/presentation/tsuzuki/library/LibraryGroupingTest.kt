package eu.kanade.presentation.tsuzuki.library

import eu.kanade.tachiyomi.ui.library.LibraryItem
import eu.kanade.tachiyomi.ui.library.LibrarySearchSpec
import eu.kanade.tachiyomi.ui.library.LibraryViewModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga

class LibraryGroupingTest {
    @Test
    fun `five hundred books preserve shelf order membership and filtered counts`() {
        val shelves = (1L..30L).map { Category(it, "Shelf $it", it, flags = 0) }
        val items = (1L..500L).map { id ->
            LibraryItem(
                libraryManga = LibraryManga(
                    manga = Manga.create().copy(id = id, title = "Book $id"),
                    categories = listOf((id - 1) % 30 + 1),
                    totalChapters = 100,
                    readCount = (id * 7) % 100,
                    bookmarkCount = 0,
                    latestUpload = 0,
                    chapterFetchedAt = 0,
                    lastRead = 0,
                ),
            )
        }
        val allGroups = items.groupLibraryItems(shelves, showSystemCategory = true)
        assertEquals(shelves, allGroups.keys.toList())
        assertEquals(500, allGroups.values.sumOf { it.size })

        val query = LibrarySearchSpec.parse("title:Book 1", useRegex = false, searchByUrl = false)
        val filtered = items.filter { it.matches(query) }
        val groups = filtered.groupLibraryItems(shelves, showSystemCategory = true)
        val state = LibraryViewModel.State(
            libraryData = LibraryViewModel.LibraryData(favorites = filtered),
            groupedFavorites = groups,
        )
        assertEquals(111, groups.values.sumOf { it.size })
        for (shelf in shelves) {
            val expected = filtered.filter { shelf.id in it.libraryManga.categories }
            assertEquals(expected, state.getItemsForCategory(shelf))
            assertEquals(expected.size, state.getItemCountForCategory(shelf, showAlways = true))
        }
    }
}
