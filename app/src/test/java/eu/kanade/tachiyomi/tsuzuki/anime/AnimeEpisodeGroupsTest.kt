package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.tsuzuki.anime.player.adjacentAnimeEpisodes
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.items.episode.model.Episode

class AnimeEpisodeGroupsTest {
    @Test
    fun groupMatchingIgnoresCaseAndOuterWhitespaceAndKeepsUnlabeledEpisodes() {
        val episodes =
            listOf(episode(1, null), episode(2, " Release Team "), episode(3, "Another group"), episode(4, " "))
        assertEquals(listOf(1L, 3L, 4L), episodes.filterAnimeGroups(setOf("release team")).map(Episode::id))
    }

    @Test
    fun nextEpisodeSkipsExcludedReleasesEvenWhenCurrentWasOpenedFromHistory() {
        val first = episode(1, null)
        val current = episode(2, "Hidden")
        val skipped = episode(3, "Hidden")
        val next = episode(4, "Visible")
        val queue = listOf(first, current, skipped, next).filterAnimeGroups(setOf("Hidden"), current.id)

        assertEquals(first to next, adjacentAnimeEpisodes(queue, current.id))
        assertEquals(null to next, adjacentAnimeEpisodes(queue.filterNot { it.id == first.id }, current.id))
    }

    @Test
    fun exclusionsPreserveOtherMetadataAndIgnoreMalformedEntries() {
        val metadata = JsonObject(mapOf("source" to JsonPrimitive("fixture")))
        val withFilter = metadata.withExcludedAnimeGroups(setOf("Hide"))
        assertEquals(setOf("Hide"), withFilter.excludedAnimeGroups())
        assertEquals(metadata, withFilter.withExcludedAnimeGroups(emptySet()))
        val invalid =
            JsonObject(mapOf(ANIME_EXCLUDED_GROUPS to JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(" ")))))
        assertEquals(emptySet<String>(), invalid.excludedAnimeGroups())
    }

    private fun episode(id: Long, group: String?) = Episode(
        animeId = 12,
        url = "/$id",
        name = "Episode $id",
        id = id,
        scanlator = group,
        episodeNumber = id.toDouble(),
    )
}
