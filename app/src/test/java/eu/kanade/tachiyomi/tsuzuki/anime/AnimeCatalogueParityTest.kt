package eu.kanade.tachiyomi.tsuzuki.anime

import androidx.compose.foundation.lazy.grid.GridCells
import eu.kanade.presentation.tsuzuki.browse.AnimeGlobalSearch
import eu.kanade.tachiyomi.tsuzuki.anime.ui.animeCatalogueColumns
import eu.kanade.tachiyomi.tsuzuki.anime.ui.catalogueMigrationId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryDisplayMode

class AnimeCatalogueParityTest {
    @Test
    fun animeFirstGlobalSearchIsUsedOnce() {
        AnimeGlobalSearch.consumeAnimeFirst()
        assertEquals(null, AnimeGlobalSearch.consumeAnimeFirst())
        AnimeGlobalSearch.openAnimeFirst()
        assertEquals("", AnimeGlobalSearch.consumeAnimeFirst())
        assertEquals(null, AnimeGlobalSearch.consumeAnimeFirst())
    }

    @Test
    fun listModeUsesOneColumn() {
        assertEquals(GridCells.Fixed(1), animeCatalogueColumns(LibraryDisplayMode.List))
        assertTrue(animeCatalogueColumns(LibraryDisplayMode.ComfortableGrid) is GridCells.Adaptive)
        assertTrue(animeCatalogueColumns(LibraryDisplayMode.CompactGrid) is GridCells.Adaptive)
    }

    @Test
    fun catalogueMigrationRequiresASavedResultAndAnotherSource() {
        assertEquals(null, catalogueMigrationId(savedId = null, sourceId = 1, availableSourceIds = setOf(1, 2)))
        assertEquals(null, catalogueMigrationId(savedId = 9, sourceId = 1, availableSourceIds = setOf(1)))
        assertEquals(9L, catalogueMigrationId(savedId = 9, sourceId = 1, availableSourceIds = setOf(1, 2)))
    }
}
