package eu.kanade.presentation.tsuzuki.anime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.LibrarySort.Direction
import tachiyomi.domain.library.model.LibrarySort.Type

class TsuzukiMixedLibraryTest {

    private data class Item(val name: String, val keys: MixedLibrarySortKeys)

    private fun item(name: String, lastRead: Long = 0, unread: Long = 0) =
        Item(name, MixedLibrarySortKeys(title = name, lastRead = lastRead, unread = unread))

    private fun merge(books: List<Item>, anime: List<Item>, type: Type, direction: Direction) =
        mergeMixedLibrary(books, { it.keys }, anime, { it.keys }, LibrarySort(type, direction)).map {
            when (it) {
                is MixedLibraryEntry.Book -> "b:" + it.item.name
                is MixedLibraryEntry.Anime -> "a:" + it.item.name
            }
        }

    @Test
    fun `alphabetical ascending interleaves by title ignoring case`() {
        val books = listOf(item("apple"), item("Cherry"), item("egg"))
        val anime = listOf(item("Dragon"), item("banana"))
        assertEquals(
            listOf("b:apple", "a:banana", "b:Cherry", "a:Dragon", "b:egg"),
            merge(books, anime, Type.Alphabetical, Direction.Ascending),
        )
    }

    @Test
    fun `last read descending puts the most recent first`() {
        val books = listOf(item("b30", lastRead = 30), item("b10", lastRead = 10))
        val anime = listOf(item("a5", lastRead = 5), item("a20", lastRead = 20), item("a40", lastRead = 40))
        assertEquals(
            listOf("a:a40", "b:b30", "a:a20", "b:b10", "a:a5"),
            merge(books, anime, Type.LastRead, Direction.Descending),
        )
    }

    @Test
    fun `books never move and win ties`() {
        // The library's own tie-break put z before a; the merge must not undo it.
        val books = listOf(item("z", unread = 3), item("a", unread = 3))
        val anime = listOf(item("m", unread = 3))
        assertEquals(listOf("b:z", "b:a", "a:m"), merge(books, anime, Type.UnreadCount, Direction.Ascending))
    }

    @Test
    fun `no anime returns the books unchanged`() {
        val books = listOf(item("c"), item("a"), item("b"))
        assertEquals(listOf("b:c", "b:a", "b:b"), merge(books, emptyList(), Type.Alphabetical, Direction.Ascending))
    }

    @Test
    fun `sorts anime cannot follow put anime after the books, by title`() {
        val books = listOf(item("b2"), item("b1"))
        val anime = listOf(item("Zeta"), item("alpha"))
        for (type in listOf(Type.Random, Type.TrackerMean, Type.DownloadedChapters, Type.SourceName)) {
            assertEquals(
                listOf("b:b2", "b:b1", "a:alpha", "a:Zeta"),
                merge(books, anime, type, Direction.Ascending),
                type.toString(),
            )
        }
    }

    @Test
    fun `only anime are sorted on their own`() {
        val anime = listOf(item("b", unread = 1), item("a", unread = 9))
        assertEquals(listOf("a:a", "a:b"), merge(emptyList(), anime, Type.UnreadCount, Direction.Descending))
    }
}
