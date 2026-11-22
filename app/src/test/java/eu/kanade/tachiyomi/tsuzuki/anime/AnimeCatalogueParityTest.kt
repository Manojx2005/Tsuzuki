package eu.kanade.tachiyomi.tsuzuki.anime

import androidx.compose.foundation.lazy.grid.GridCells
import eu.kanade.presentation.tsuzuki.browse.AnimeGlobalSearch
import eu.kanade.tachiyomi.tsuzuki.anime.ui.animeCatalogueColumns
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryDisplayMode

class AnimeCatalogueParityTest {
    @Test
    fun animeFirstGlobalSearchIsUsedOnce() {
        assertFalse(AnimeGlobalSearch.consumeAnimeFirst())
        AnimeGlobalSearch.openAnimeFirst()
        assertTrue(AnimeGlobalSearch.consumeAnimeFirst())
        assertFalse(AnimeGlobalSearch.consumeAnimeFirst())
    }

    @Test
    fun listModeUsesOneColumn() {
        assertEquals(GridCells.Fixed(1), animeCatalogueColumns(LibraryDisplayMode.List))
        assertTrue(animeCatalogueColumns(LibraryDisplayMode.ComfortableGrid) is GridCells.Adaptive)
        assertTrue(animeCatalogueColumns(LibraryDisplayMode.CompactGrid) is GridCells.Adaptive)
    }
}
