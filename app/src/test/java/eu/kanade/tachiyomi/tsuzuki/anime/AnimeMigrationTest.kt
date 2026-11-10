package eu.kanade.tachiyomi.tsuzuki.anime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.items.episode.model.Episode

class AnimeMigrationTest {
    @Test
    fun matchesSourceAndBackupDecimalNumbersAndSkipsUnknownNumbers() {
        val original = listOf(
            episode(1, 1.2, seconds = 30),
            episode(2, -1.0, seconds = 45),
            episode(3, Double.NaN, seconds = 50),
            episode(4, 2.0, seconds = 10),
            episode(5, 2.0, seconds = 90),
        )
        val target = listOf(
            episode(11, 1.2f.toDouble()),
            episode(12, -1.0),
            episode(13, Double.NaN),
            episode(14, 2.0),
            episode(15, 1.25),
        )
        val matches = matchAnimeEpisodes(original, target).map { it.first.id to it.second.id }
        assertEquals(listOf(1L to 11L, 5L to 14L), matches)
    }

    @Test
    fun mergeKeepsTargetProgressAndBoundsImportedPositionToTheTargetDuration() {
        val target = episode(11, 1.0, seconds = 80).copy(totalSeconds = 90)
        val migrated = target.withMigratedProgress(episode(1, 1.0, seconds = 110).copy(seen = true, totalSeconds = 120))
        assertTrue(migrated.seen)
        assertEquals(90L, migrated.lastSecondSeen)
        assertEquals(90L, migrated.totalSeconds)
        assertEquals(target, target.withMigratedProgress(episode(1, 1.0, seconds = 40)))
    }

    private fun episode(id: Long, number: Double, seconds: Long = 0) = Episode(
        animeId = 1,
        url = "/$id",
        name = "Episode $id",
        id = id,
        episodeNumber = number,
        lastSecondSeen = seconds,
    )
}
