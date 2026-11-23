package eu.kanade.presentation.tsuzuki.anime

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AnimeLibraryMarkTest {
    @Test
    fun writesAnimeThenBooksAndReturnsTheChangedEpisodes() = runBlocking {
        val calls = mutableListOf<String>()
        val changed = markLibrarySeen(
            seen = true,
            setAnime = {
                calls += "anime $it"
                listOf(1L, 2L)
            },
            revertAnime = { calls += "revert" },
            setBooks = { calls += "books $it" },
        )
        assertEquals(listOf(1L, 2L), changed)
        assertEquals(listOf("anime true", "books true"), calls)
    }

    @Test
    fun putsAnimeBackWhenTheBookWriteFails() {
        val calls = mutableListOf<String>()
        val failure = IllegalStateException("books")
        val thrown = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                markLibrarySeen(
                    seen = false,
                    setAnime = {
                        calls += "anime"
                        listOf(1L)
                    },
                    revertAnime = { calls += "revert" },
                    setBooks = { throw failure },
                )
            }
        }
        assertSame(failure, thrown.original())
        assertEquals(listOf("anime", "revert"), calls)
    }

    @Test
    fun keepsTheBookFailureWhenTheRollbackAlsoFails() {
        val failure = IllegalStateException("books")
        val rollback = IllegalStateException("revert")
        val thrown = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                markLibrarySeen(
                    seen = true,
                    setAnime = { listOf(1L) },
                    revertAnime = { throw rollback },
                    setBooks = { throw failure },
                )
            }
        }
        assertSame(failure, thrown.original())
        assertEquals(listOf(rollback), failure.suppressed.toList())
    }

    @Test
    fun animeOnlySelectionHasNoBookStep() = runBlocking {
        assertEquals(listOf(3L), markLibrarySeen(true, { listOf(3L) }, { error("no rollback") }, null))
    }
}

// Coroutine debug mode rethrows a copy whose cause is the original exception.
private fun Throwable.original(): Throwable = generateSequence(this) { it.cause }.last()
