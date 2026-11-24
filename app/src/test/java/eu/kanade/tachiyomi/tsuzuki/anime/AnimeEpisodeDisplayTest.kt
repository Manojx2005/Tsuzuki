package eu.kanade.tachiyomi.tsuzuki.anime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.items.episode.model.Episode

class AnimeEpisodeDisplayTest {
    @Test
    fun sourceSortFollowsProviderOrderAndCanBeReversed() {
        val episodes = listOf(
            Episode(1, "/1", "One", id = 1, episodeNumber = 1.0, sourceOrder = 1, dateUpload = 30),
            Episode(1, "/2", "Two", id = 2, episodeNumber = 2.0, sourceOrder = 2, dateUpload = 10),
            Episode(1, "/3", "Three", id = 3, episodeNumber = 3.0, sourceOrder = 0, dateUpload = 20),
        )
        val display = AnimeEpisodeDisplay(sort = AnimeEpisodeSort.SOURCE)
        assertEquals(listOf(3L, 1L, 2L), display.apply(episodes).map(Episode::id))
        assertEquals(listOf(2L, 1L, 3L), display.copy(ascending = true).apply(episodes).map(Episode::id))
        assertEquals(listOf(3L, 2L, 1L), display.copy(sort = AnimeEpisodeSort.NUMBER).apply(episodes).map(Episode::id))
        assertEquals(
            listOf(1L, 3L, 2L),
            display.copy(sort = AnimeEpisodeSort.UPLOAD_DATE).apply(episodes).map(Episode::id),
        )
    }

    @Test
    fun newOptionsCoexistWithLegacyFiltersAndUnrelatedBackupBits() {
        val unrelated = 0x123L or (1L shl 50)
        val legacyFlags = unrelated or (1L shl 32) or (1L shl 33) or (1L shl 34) or (1L shl 37) or (1L shl 38)
        val legacy = AnimeEpisodeDisplay.fromFlags(legacyFlags)
        assertEquals(
            AnimeEpisodeDisplay(
                sort = AnimeEpisodeSort.UPLOAD_DATE,
                ascending = true,
                unseen = TriState.ENABLED_IS,
                bookmarked = TriState.ENABLED_NOT,
                downloaded = TriState.ENABLED_IS,
            ),
            legacy,
        )
        assertEquals(legacyFlags, legacy.toFlags(legacyFlags))
        assertEquals(AnimeEpisodeSort.NUMBER, AnimeEpisodeDisplay.fromFlags(0L).sort)
        assertFalse(AnimeEpisodeDisplay.fromFlags(0L).showNumberOnly)

        val updated = legacy.copy(sort = AnimeEpisodeSort.SOURCE, showNumberOnly = true)
        val flags = updated.toFlags(legacyFlags)
        assertEquals(unrelated, flags and unrelated)
        assertEquals(0L, flags and (1L shl 32))
        assertEquals(updated, AnimeEpisodeDisplay.fromFlags(flags))
        assertEquals(legacyFlags, legacy.toFlags(flags))
    }
}
