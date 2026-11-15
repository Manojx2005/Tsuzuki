package eu.kanade.tachiyomi.tsuzuki.anime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tachiyomi.domain.entries.anime.model.Anime

class AnimeDuplicatesTest {
    @Test
    fun normalizesCasePunctuationWidthAndEquivalentSeasonSuffixes() {
        assertEquals(animeTitleKey("The Show! (Season 02)"), animeTitleKey("ＴＨＥ ＳＨＯＷ – S2"))
        assertEquals(animeTitleKey("The Show S2"), animeTitleKey("the show: 2nd season"))
        assertEquals(animeTitleKey("作品 第2期"), animeTitleKey("作品第２期"))
        assertEquals(animeTitleKey("The Show"), animeTitleKey("The Show Season 1"))
        assertNotEquals(animeTitleKey("The Show S2"), animeTitleKey("The Show S3"))
        assertNull(animeTitleKey("! — ?"))
    }

    @Test
    fun choosesTheTrackerMatchBeforeATitleMatchAndIgnoresTheSameSource() {
        val candidate = title(1, 10, "The Show")
        val sameSource = title(2, 10, "The Show")
        val titleMatch = title(3, 11, "THE SHOW!")
        val trackerMatch = title(4, 12, "Different title")
        val library = listOf(sameSource, titleMatch, trackerMatch)
        assertEquals(trackerMatch, findAnimeDuplicate(candidate, library, trackedMatchId = trackerMatch.id))
        assertEquals(titleMatch, findAnimeDuplicate(candidate, library, trackedMatchId = sameSource.id))
        assertNull(findAnimeDuplicate(candidate, listOf(sameSource, titleMatch.copy(favorite = false)), null))
    }

    @Test
    fun knownSeasonMetadataPreventsMatchingAnUnrelatedSeasonWithTheSameTitle() {
        val candidate = title(1, 10, "The Show").copy(seasonNumber = 2.0)
        val first = title(2, 11, "The Show").copy(seasonNumber = 1.0)
        val second = title(3, 12, "The Show S2")
        assertEquals(second, findAnimeDuplicate(candidate, listOf(first, second), null))
    }

    private fun title(id: Long, source: Long, name: String) =
        Anime(id = id, source = source, url = "/$id", title = name, favorite = true)
}
