package eu.kanade.presentation.tsuzuki.updates

import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeUpdateEpisode
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadStatus
import eu.kanade.tachiyomi.ui.updates.UpdatesFilter
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.updates.model.UpdatesWithRelations

class TsuzukiMixedUpdatesSelectionTest {

    private fun chapter(
        id: Long,
        read: Boolean = false,
        bookmark: Boolean = false,
        lastPage: Long = 0,
        state: Download.State = Download.State.NOT_DOWNLOADED,
    ) = UpdatesItem(
        update = UpdatesWithRelations(
            mangaId = id, mangaTitle = "Book $id", chapterId = id, chapterName = "c$id", scanlator = null,
            chapterUrl = "", read = read, bookmark = bookmark, lastPageRead = lastPage, sourceId = 1, dateFetch = id,
            coverData = MangaCover(id, 1, true, null, 0),
        ),
        downloadStateProvider = { state },
        downloadProgressProvider = { 0 },
        selected = true,
    )

    private fun episode(id: Long, seen: Boolean = false, bookmark: Boolean = false) = AnimeUpdateEpisode(
        animeId = 1,
        title = "Anime",
        episodeId = id,
        name = "Episode $id",
        previewUrl = null,
        timestamp = id,
        seen = seen,
        bookmark = bookmark,
    )

    @Test
    fun `books only match the upstream bar rules`() {
        val books = listOf(chapter(1, read = true, bookmark = true, state = Download.State.DOWNLOADED))
        val actions = mixedUpdateSelectionActions(books, emptyList(), emptyMap())
        assertEquals(bookUpdateSelectionActions(books), actions)
        assertFalse(actions.bookmark)
        assertTrue(actions.removeBookmark)
        assertFalse(actions.markSeen)
        assertTrue(actions.markUnseen)
        assertFalse(actions.download)
        assertTrue(actions.delete)

        val queued =
            mixedUpdateSelectionActions(listOf(chapter(2, state = Download.State.QUEUE)), emptyList(), emptyMap())
        assertTrue(queued.download)
        assertFalse(queued.delete)
        assertTrue(mixedUpdateSelectionActions(listOf(chapter(3, lastPage = 4)), emptyList(), emptyMap()).markUnseen)
    }

    @Test
    fun `episodes only match the anime bar rules`() {
        val anime = listOf(episode(10, seen = true, bookmark = true))
        val status = mapOf(10L to AnimeDownloadStatus.Saved)
        assertEquals(
            animeUpdateSelectionActions(anime, status),
            mixedUpdateSelectionActions(emptyList(), anime, status),
        )
    }

    @Test
    fun `a mixed selection offers an action when either kind needs it`() {
        val books = listOf(chapter(1, read = true, bookmark = true, state = Download.State.DOWNLOADED))
        val anime = listOf(episode(10))
        val actions = mixedUpdateSelectionActions(books, anime, emptyMap())
        assertTrue(actions.bookmark) // the episode isn't bookmarked
        assertFalse(actions.removeBookmark) // not everything is bookmarked
        assertTrue(actions.markSeen) // the episode is new
        assertTrue(actions.markUnseen) // the chapter is read
        assertTrue(actions.download) // the episode isn't saved
        assertTrue(actions.delete) // the chapter is downloaded
    }

    @Test
    fun `remove bookmark needs every row of both kinds bookmarked`() {
        val books = listOf(chapter(1, bookmark = true))
        assertTrue(mixedUpdateSelectionActions(books, listOf(episode(10, bookmark = true)), emptyMap()).removeBookmark)
        assertFalse(mixedUpdateSelectionActions(books, listOf(episode(10)), emptyMap()).removeBookmark)
        assertFalse(
            mixedUpdateSelectionActions(listOf(chapter(2)), listOf(episode(10, bookmark = true)), emptyMap())
                .removeBookmark,
        )
    }

    @Test
    fun `an empty selection offers nothing`() {
        val none = mixedUpdateSelectionActions(emptyList(), emptyList(), emptyMap())
        assertEquals(AnimeUpdateSelectionActions(false, false, false, false, false, false), none)
    }

    @Test
    fun `episodes are selectable only in the per-chapter list of All`() {
        val episodes = listOf(episode(1), episode(2))
        assertEquals(episodes, selectableAnimeInAll(UpdatesFilter.ALL, groupByBook = false, episodes))
        assertTrue(selectableAnimeInAll(UpdatesFilter.ALL, groupByBook = true, episodes).isEmpty())
        assertTrue(selectableAnimeInAll(UpdatesFilter.NOVELS, groupByBook = false, episodes).isEmpty())
        assertTrue(selectableAnimeInAll(UpdatesFilter.MANGA, groupByBook = false, episodes).isEmpty())
    }

    @Test
    fun `selection keeps only listed episodes and toggles ids`() {
        val listed = listOf(episode(1), episode(2), episode(3))
        assertEquals(listOf(1L, 3L), selectedAnimeInAll(listed, setOf(1L, 3L, 99L)).map { it.episodeId })
        assertTrue(selectedAnimeInAll(emptyList(), setOf(1L)).isEmpty())
        assertEquals(setOf(1L, 2L), toggledAnimeSelection(setOf(1L), 2L))
        assertEquals(setOf(1L), toggledAnimeSelection(setOf(1L, 2L), 2L))
    }

    @Test
    fun `select all and invert cover the listed episodes`() {
        val listed = listOf(episode(1), episode(2), episode(3))
        assertEquals(setOf(1L, 2L, 3L), animeUpdatesSelectAll(listed))
        assertEquals(setOf(2L), animeUpdatesInvert(listed, setOf(1L, 3L)))
        assertEquals(setOf(1L, 2L, 3L), animeUpdatesInvert(listed, emptySet()))
    }

    @Test
    fun `the All selection toggles, selects all, inverts and freezes while applying`() {
        val listed = listOf(episode(1), episode(2), episode(3))
        val state = AnimeAllSelectionState()
        fun view(books: Boolean = false) = AnimeAllSelection(state, listed, booksSelecting = books)

        assertFalse(view().selecting)
        assertTrue(view(books = true).selecting) // a book selection makes taps toggle episode rows too
        view().toggle(listed[1])
        assertEquals(setOf(2L), state.ids)
        assertTrue(view().selecting)
        view().invert()
        assertEquals(setOf(1L, 3L), state.ids)
        view().selectAll()
        assertEquals(setOf(1L, 2L, 3L), state.ids)

        state.applying = true
        view().clear()
        assertEquals(3, view().selected.size) // frozen while an action runs
        state.applying = false
        view().clear()
        assertTrue(state.ids.isEmpty())
    }

    @Test
    fun `selection mode and counter include both kinds`() {
        assertFalse(updatesSelecting(false, null))
        assertTrue(updatesSelecting(true, null))
        assertEquals(4, updatesSelectedCount(4, null))
        val slots = AnimeUpdatesSlots(null, null, null)
        assertFalse(updatesSelecting(false, slots))
        assertEquals(2, updatesSelectedCount(2, slots))
    }
}
