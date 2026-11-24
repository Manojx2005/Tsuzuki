package eu.kanade.presentation.tsuzuki.anime

import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadEntry
import eu.kanade.tachiyomi.tsuzuki.anime.download.AnimeDownloadStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnimeLibraryRemoveTest {
    private fun run(remove: Boolean, delete: Boolean, undo: Boolean): List<String> = runBlocking {
        val calls = mutableListOf<String>()
        removeLibrarySelection(
            removeFromLibrary = remove,
            deleteDownloads = delete,
            setFavorites = { calls += "favorite $it" },
            deleteSavedDownloads = { calls += "delete downloads" },
            awaitUndo = {
                calls += "offer undo"
                undo
            },
        )
        calls
    }

    @Test
    fun removesFromTheLibraryAndOffersUndo() {
        assertEquals(listOf("favorite false", "offer undo"), run(remove = true, delete = false, undo = false))
    }

    @Test
    fun undoPutsTheEntriesBack() {
        assertEquals(
            listOf("favorite false", "offer undo", "favorite true"),
            run(remove = true, delete = false, undo = true),
        )
    }

    @Test
    fun deletingDownloadsOnlyKeepsTheLibraryAndOffersNoUndo() {
        assertEquals(listOf("delete downloads"), run(remove = false, delete = true, undo = true))
    }

    @Test
    fun bothChoicesDeleteDownloadsBeforeTheUndoWindow() {
        assertEquals(
            listOf("favorite false", "delete downloads", "offer undo", "favorite true"),
            run(remove = true, delete = true, undo = true),
        )
    }

    @Test
    fun deletesOnlyTheSavedDownloadsOfTheSelection() {
        fun entry(anime: Long, episode: Long, status: AnimeDownloadStatus) =
            AnimeDownloadEntry(1L, anime, episode, "src", "anime", "ep", status)
        val entries = listOf(
            entry(1, 10, AnimeDownloadStatus.Saved),
            entry(1, 11, AnimeDownloadStatus.Queued),
            entry(2, 20, AnimeDownloadStatus.Saved),
            entry(3, 30, AnimeDownloadStatus.Saved),
            entry(2, 21, AnimeDownloadStatus.Failed),
        )
        assertEquals(listOf(10L, 20L), animeDownloadsToDelete(entries, setOf(1L, 2L)))
    }
}
