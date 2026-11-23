package eu.kanade.presentation.tsuzuki.anime

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.items.episode.model.Episode

class AnimeLibrarySelectionTest {
    @Test
    fun rangeAndInvertFollowTheVisibleShelfOrder() {
        val visible = listOf(105L, 3L, 88L, 12L, 9L)
        val selection = AnimeLibrarySelection()

        selection.range(12L, visible)
        selection.range(3L, visible)
        assertEquals(setOf(3L, 88L, 12L), selection.ids)

        selection.invert(visible)
        assertEquals(setOf(105L, 9L), selection.ids)
        selection.selectAll(visible)
        assertEquals(visible.toSet(), selection.ids)
        selection.clear()
        assertEquals(emptySet<Long>(), selection.ids)
    }

    @Test
    fun downloadNextSkipsWatchedAndExistingDownloadsBeforeTakingLimit() {
        val episodes = listOf(
            episode(4),
            episode(3),
            episode(2),
            episode(1).copy(seen = true),
        )
        assertEquals(listOf(3L), episodesToDownload(episodes, AnimeLibraryDownload.NextOne, setOf(2)).map(Episode::id))
        assertEquals(
            listOf(2L, 4L),
            episodesToDownload(episodes, AnimeLibraryDownload.Unwatched, setOf(3)).map(Episode::id),
        )
        assertEquals(
            listOf(1L, 2L, 4L),
            episodesToDownload(episodes, AnimeLibraryDownload.All, setOf(3)).map(Episode::id),
        )
    }

    @Test
    fun mixedRemoveFinishesBothWritesAfterParentCancellation() = runTest {
        val booksWritten = CompletableDeferred<Unit>()
        val writeAnime = CompletableDeferred<Unit>()
        val changes = mutableListOf<Pair<String, Boolean>>()
        val removal = launch {
            changeLibraryFavorites(
                favorite = false,
                setBooks = { favorite ->
                    changes += "books" to favorite
                    booksWritten.complete(Unit)
                    writeAnime.await()
                    true
                },
                setAnime = { favorite -> changes += "anime" to favorite },
            )
        }
        booksWritten.await()
        removal.cancel()
        writeAnime.complete(Unit)
        removal.join()

        assertEquals(listOf("books" to false, "anime" to false), changes)
    }

    @Test
    fun failedAnimeUndoRollsBookFavoritesBack() = runTest {
        val failure = IllegalStateException("Anime write failed")
        val bookChanges = mutableListOf<Boolean>()
        val result = runCatching {
            changeLibraryFavorites(
                favorite = true,
                setBooks = { favorite ->
                    bookChanges += favorite
                    true
                },
                setAnime = { throw failure },
            )
        }

        assertEquals(failure.message, result.exceptionOrNull()?.message)
        assertEquals(listOf(true, false), bookChanges)
    }

    private fun episode(number: Long) =
        Episode(
            animeId = 1,
            url = number.toString(),
            name = number.toString(),
            id = number,
            episodeNumber = number.toDouble(),
        )
}
