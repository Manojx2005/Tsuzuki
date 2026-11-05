package eu.kanade.tachiyomi.tsuzuki

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.LibraryPreferences

class LibraryQueriesTest {

    private val trackers = listOf(1, 2)

    private fun prefs() = LibraryPreferences(InMemoryPreferenceStore())

    @Test
    fun `json round trip keeps every field`() {
        val query = LibraryQuery(
            id = "a",
            name = "Unread, tracked",
            triStates = mapOf("unread" to "ENABLED_IS", "tracker:2" to "ENABLED_NOT"),
            chapterCountThreshold = 40,
            includedTags = setOf("fantasy"),
            excludedTags = setOf("harem"),
            filterExtensions = setOf("7"),
            excludedExtensions = setOf("9"),
            sort = "LAST_READ,DESCENDING",
        )
        val list = listOf(query, LibraryQuery(id = "b", name = "Plain"))
        assertEquals(list, LibraryQueryStore.decode(LibraryQueryStore.encode(list)))
    }

    @Test
    fun `damaged or empty json reads as no queries and unknown keys are ignored`() {
        assertEquals(emptyList<LibraryQuery>(), LibraryQueryStore.decode(""))
        assertEquals(emptyList<LibraryQuery>(), LibraryQueryStore.decode("[{oops"))
        val decoded = LibraryQueryStore.decode("""[{"id":"x","name":"N","future":1}]""")
        assertEquals(listOf(LibraryQuery(id = "x", name = "N")), decoded)
    }

    @Test
    fun `capture stores only filters that are on and the sort`() {
        val p = prefs()
        p.filterUnread.set(TriState.ENABLED_IS)
        p.filterNovel().set(TriState.ENABLED_NOT)
        p.filterTracking(2).set(TriState.ENABLED_IS)
        p.sortingMode.set(LibrarySort(LibrarySort.Type.LastRead, LibrarySort.Direction.Descending))
        p.includedTags.set(setOf("fantasy"))

        val q = LibraryQuerySnapshot.capture(p, trackers, id = "q", name = "Mine")

        assertEquals(
            mapOf("unread" to "ENABLED_IS", "novel" to "ENABLED_NOT", "tracker:2" to "ENABLED_IS"),
            q.triStates,
        )
        assertEquals("LAST_READ,DESCENDING", q.sort)
        assertEquals(setOf("fantasy"), q.includedTags)
        assertEquals(3 + 1, q.activeCount())
    }

    @Test
    fun `apply writes all stored values and turns the rest off`() {
        val p = prefs()
        // Left over from a previous view; the query below doesn't mention them.
        p.filterDownloaded.set(TriState.ENABLED_IS)
        p.filterTracking(1).set(TriState.ENABLED_IS)
        p.excludedTags.set(setOf("old"))
        p.sortingMode.set(LibrarySort(LibrarySort.Type.DateAdded, LibrarySort.Direction.Ascending))

        val query = LibraryQuery(
            id = "q",
            name = "Q",
            triStates = mapOf("started" to "ENABLED_IS", "tracker:2" to "ENABLED_NOT", "tracker:9" to "ENABLED_IS"),
            chapterCountThreshold = 25,
            includedTags = setOf("fantasy"),
            sort = "UNREAD_COUNT,DESCENDING",
        )
        LibraryQuerySnapshot.apply(p, query, trackers)

        assertEquals(TriState.DISABLED, p.filterDownloaded.get())
        assertEquals(TriState.DISABLED, p.filterTracking(1).get())
        assertEquals(TriState.ENABLED_NOT, p.filterTracking(2).get())
        assertEquals(TriState.ENABLED_IS, p.filterTracking(9).get())
        assertEquals(TriState.ENABLED_IS, p.filterStarted.get())
        assertEquals(25, p.filterChapterCountThreshold.get())
        assertEquals(setOf("fantasy"), p.includedTags.get())
        assertEquals(emptySet<String>(), p.excludedTags.get())
        assertEquals(LibrarySort(LibrarySort.Type.UnreadCount, LibrarySort.Direction.Descending), p.sortingMode.get())
    }

    @Test
    fun `capture after apply gives the same content and clear restores defaults`() {
        val p = prefs()
        val query = LibraryQuery(
            id = "q",
            name = "Q",
            triStates = mapOf("bookmarked" to "ENABLED_NOT", "tracker:1" to "ENABLED_IS"),
            excludedExtensions = setOf("5"),
            sort = "TOTAL_CHAPTERS,ASCENDING",
        )
        LibraryQuerySnapshot.apply(p, query, trackers)
        assertEquals(query.content(), LibraryQuerySnapshot.capture(p, trackers))

        LibraryQuerySnapshot.clear(p, trackers)
        assertEquals(LibraryQuery(id = "", name = ""), LibraryQuerySnapshot.capture(p, trackers))
        assertEquals(LibrarySort.default, p.sortingMode.get())
        assertEquals(LibraryQuery.DEFAULT_CHAPTER_COUNT_THRESHOLD, p.filterChapterCountThreshold.get())
    }

    @Test
    fun `store add rename delete and move`() {
        val store = LibraryQueryStore(InMemoryPreferenceStore())
        val content = LibraryQuery(id = "", name = "", triStates = mapOf("unread" to "ENABLED_IS"))
        assertNull(store.add("   ", content))
        store.add(" One ", content, id = "1")
        store.add("Two", content, id = "2")
        store.add("Three", content, id = "3")
        assertEquals(listOf("One", "Two", "Three"), store.all().map { it.name })

        store.move("3", -1)
        assertEquals(listOf("1", "3", "2"), store.all().map { it.id })
        store.move("1", -1)
        store.move("2", 1)
        assertEquals(listOf("1", "3", "2"), store.all().map { it.id })

        store.rename("3", "Renamed")
        store.rename("2", " ")
        assertEquals(listOf("One", "Renamed", "Two"), store.all().map { it.name })

        store.delete("1")
        assertEquals(listOf("3", "2"), store.all().map { it.id })
        assertEquals(content.triStates, store.all().first().triStates)
    }

    private fun LibraryQuery.activeCount() =
        triStates.size + includedTags.size + excludedTags.size + filterExtensions.size + excludedExtensions.size
}
