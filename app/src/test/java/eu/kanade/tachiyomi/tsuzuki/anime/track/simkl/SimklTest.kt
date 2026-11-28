package eu.kanade.tachiyomi.tsuzuki.anime.track.simkl

import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTrackStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SimklTest {
    @Test
    fun progressForwardAddsTheNewEpisodesAndBackwardRemovesThem() {
        assertEquals("/sync/history" to listOf(4, 5, 6), simklEpisodeChange(seen = 3, progress = 6))
        assertEquals("/sync/history/remove" to listOf(3, 4), simklEpisodeChange(seen = 4, progress = 2))
        assertNull(simklEpisodeChange(seen = 5, progress = 5))
        assertEquals(MAX_EPISODES_PER_CALL, simklEpisodeChange(0, 2_000)!!.second.size)
    }

    @Test
    fun statusesUseSimklWatchlistNames() {
        assertEquals("watching", AnimeTrackStatus.REWATCHING.simklStatus)
        assertEquals("plantowatch", AnimeTrackStatus.PLANNING.simklStatus)
        assertEquals("hold", AnimeTrackStatus.ON_HOLD.simklStatus)
    }

    @Test
    fun searchResultsLinkToTheItemsOwnSimklPage() {
        val items = Json.parseToJsonElement(
            """
            [
              {"title":"Shingeki no Kyojin","title_en":"Attack on Titan","poster":"74/7480","total_episodes":25,
               "ids":{"simkl_id":39687,"slug":"attack-on-titan","mal":"16498"}},
              {"title":"No id"},
              {"title":"Bare","ids":{"simkl_id":7}}
            ]
            """.trimIndent(),
        ) as JsonArray
        val results = parseSimklSearch(items)
        assertEquals(2, results.size)
        val first = results.first()
        assertEquals("Attack on Titan", first.title)
        assertEquals("https://simkl.com/anime/39687/attack-on-titan", first.url)
        assertEquals(25L, first.totalEpisodes)
        assertEquals(16498L, first.malId)
        assertEquals("https://simkl.com/anime/7", results[1].url)
    }

    @Test
    fun tokensRefreshAMinuteEarlyAndKeepTheirRefreshToken() {
        val token = SimklOAuth("a", null, expiresIn = 604_800).stamped(previousRefresh = "r", now = 0)
        assertEquals("r", token.refreshToken)
        assertFalse(token.isExpired(now = 0))
        assertTrue(token.isExpired(now = 604_800_000 - 30_000))
        assertFalse(SimklOAuth("a").isExpired(now = Long.MAX_VALUE / 2)) // no expiry known: never "expired"
    }
}
