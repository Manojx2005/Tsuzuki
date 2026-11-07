package eu.kanade.presentation.tsuzuki.browse

import eu.kanade.presentation.browse.SourceUiModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.model.Source
import tachiyomi.source.local.LocalSource

class MangaSourceTilesTest {
    @Test
    fun localMangaJoinsPinnedSourcesWithoutChangingPinState() {
        val local = source(LocalSource.ID, "Local")
        val remote = source(7, "Pinned")
        val pinned = SourceUiModel.Header("pinned")
        val localLanguage = SourceUiModel.Header("other")

        assertEquals(listOf(pinned, local, remote), mangaSourceTiles(listOf(pinned, remote, localLanguage, local)))
    }

    @Test
    fun localMangaGetsAPinnedSectionAfterLastUsed() {
        val local = source(LocalSource.ID, "Local")
        val remote = source(7, "Recent")
        val recent = SourceUiModel.Header("last_used")

        assertEquals(
            listOf(recent, remote, SourceUiModel.Header("pinned"), local),
            mangaSourceTiles(listOf(recent, remote, SourceUiModel.Header("other"), local)),
        )
    }

    @Test
    fun sourceListsWithoutLocalMangaAreReused() {
        val items = listOf(SourceUiModel.Header("en"), source(7, "Remote"))

        assertSame(items, mangaSourceTiles(items))
    }

    private fun source(id: Long, name: String) =
        SourceUiModel.Item(Source(id, "en", name, supportsLatest = false, isStub = false))
}
