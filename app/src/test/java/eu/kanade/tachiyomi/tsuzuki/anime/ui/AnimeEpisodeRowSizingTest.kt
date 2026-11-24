package eu.kanade.tachiyomi.tsuzuki.anime.ui

import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnimeEpisodeRowSizingTest {
    @Test
    fun phoneRowShrinksThePreview() {
        // S22: 384 dp screen, 12 dp row margins and 10 dp tile padding leave 340 dp.
        assertEquals(88.4f, animeEpisodeThumbnailWidth(340f), 0.01f)
    }

    @Test
    fun narrowTabletPaneUsesTheMinimum() {
        // Tab S11 portrait right pane: 753 - 450 dp, minus list and row padding.
        assertEquals(THUMBNAIL_MIN_WIDTH, animeEpisodeThumbnailWidth(243f))
        assertEquals(THUMBNAIL_MIN_WIDTH, animeEpisodeThumbnailWidth(0f))
    }

    @Test
    fun wideRowKeepsTheOldPreviewSize() {
        assertEquals(THUMBNAIL_MAX_WIDTH, animeEpisodeThumbnailWidth(400f))
        assertEquals(THUMBNAIL_MAX_WIDTH, animeEpisodeThumbnailWidth(1200f))
    }

    @Test
    fun sheetHeightOnlyGrows() {
        assertEquals(420.dp, stableSheetHeight(0.dp, 420.dp))
        assertEquals(420.dp, stableSheetHeight(420.dp, 180.dp))
        assertEquals(500.dp, stableSheetHeight(420.dp, 500.dp))
    }
}
