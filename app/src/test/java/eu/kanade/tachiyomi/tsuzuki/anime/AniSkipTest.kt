package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.tsuzuki.anime.player.AniSkipKey
import eu.kanade.tachiyomi.tsuzuki.anime.player.AniSkipKind
import eu.kanade.tachiyomi.tsuzuki.anime.player.AniSkipLookup
import eu.kanade.tachiyomi.tsuzuki.anime.player.AniSkipMode
import eu.kanade.tachiyomi.tsuzuki.anime.player.AniSkipSegment
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerPreferences
import eu.kanade.tachiyomi.tsuzuki.anime.player.aniSkipKey
import eu.kanade.tachiyomi.tsuzuki.anime.player.atPosition
import eu.kanade.tachiyomi.tsuzuki.anime.player.canAutoSkip
import eu.kanade.tachiyomi.tsuzuki.anime.player.parseAniSkipSegments
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.track.anime.model.AnimeTrack
import tachiyomi.domain.track.anime.repository.AnimeTrackRepository

class AniSkipTest {
    @Test
    fun `parses opening ending recap and rejects invalid intervals`() {
        assertEquals(
            listOf(
                AniSkipSegment(AniSkipKind.RECAP, 0.0, 12.0),
                AniSkipSegment(AniSkipKind.OPENING, 12.0, 99.5),
                AniSkipSegment(AniSkipKind.ENDING, 1_300.0, 1_400.0),
            ),
            parseAniSkipSegments(RESPONSE, 1_400.0),
        )
        assertTrue(parseAniSkipSegments("""{"found":false,"results":[]}""", 1_400.0).isEmpty())
    }

    @Test
    fun `active intervals include their start and apply the delay only to openings`() {
        val intro = AniSkipSegment(AniSkipKind.OPENING, 30.0, 100.0)
        val recap = AniSkipSegment(AniSkipKind.RECAP, 100.0, 110.0)
        val segments = listOf(intro, recap)
        assertNull(segments.atPosition(29.99))
        assertEquals(intro, segments.atPosition(30.0))
        assertEquals(recap, segments.atPosition(100.0))
        assertNull(segments.atPosition(110.0))
        assertFalse(intro.canAutoSkip(32.99, 3))
        assertTrue(intro.canAutoSkip(33.0, 3))
        assertFalse(intro.canAutoSkip(100.0, 3))
        assertTrue(recap.canAutoSkip(100.0, 3))
    }

    @Test
    fun `uses a bound MAL id and skips specials without an integer episode`() {
        val aniList = binding(2, 5114)
        assertEquals(AniSkipKey(5114, 2, 1_400), aniSkipKey(listOf(aniList), 2.0, 1_400.2))
        assertEquals(AniSkipKey(15, 2, 1_400), aniSkipKey(listOf(aniList, binding(1, 15)), 2.0, 1_400.0))
        assertNull(aniSkipKey(listOf(aniList), 2.5, 1_400.0))
        assertNull(aniSkipKey(listOf(binding(2, null)), 2.0, 1_400.0))
    }

    @Test
    fun `lookup with no id makes no HTTP call and a real lookup is cached`() = runBlocking {
        var requests = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests++
            val request = chain.request()
            assertEquals("/v2/skip-times/5114/2", request.url.encodedPath)
            assertEquals("1400", request.url.queryParameter("episodeLength"))
            assertEquals(
                listOf("op", "ed", "mixed-op", "mixed-ed", "recap"),
                List(5) { request.url.queryParameter("types[$it]") },
            )
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(RESPONSE.toResponseBody()).build()
        }.build()
        val episode = Episode(7, "/2", "Episode 2", episodeNumber = 2.0)
        val withoutId = AniSkipLookup(client, tracks(binding(2, null)))
        assertTrue(withoutId.segments(episode, 1_400.0).isEmpty())
        assertEquals(0, requests)

        val withId = AniSkipLookup(client, tracks(binding(2, 5114)))
        val segments = withId.segments(episode, 1_400.0)
        assertEquals(3, segments.size)
        assertEquals(segments, withId.segments(episode, 1_400.0))
        assertEquals(1, requests)
    }

    @Test
    fun `preferences default off and bound the opening delay`() {
        val preferences = AnimePlayerPreferences(InMemoryPreferenceStore())
        assertEquals(AniSkipMode.OFF, preferences.get().aniSkipMode)
        preferences.set(preferences.get().copy(aniSkipMode = AniSkipMode.AUTO, aniSkipIntroDelay = 75))
        assertEquals(AniSkipMode.AUTO, preferences.get().aniSkipMode)
        assertEquals(30, preferences.get().aniSkipIntroDelay)
    }

    private fun binding(service: Long, malId: Long?) =
        AnimeTrack(7, service, 1, "Example", "", malId = malId)

    private fun tracks(vararg bindings: AnimeTrack) = object : AnimeTrackRepository {
        override suspend fun awaitByAnimeId(animeId: Long) = bindings.toList()
        override fun subscribeByAnimeId(animeId: Long) = flowOf(bindings.toList())
        override suspend fun upsert(track: AnimeTrack) = error("Unexpected update")
        override suspend fun delete(animeId: Long, syncId: Long) = error("Unexpected delete")
    }

    private companion object {
        const val RESPONSE = """
            {"found":true,"results":[
                {"skipType":"mixed-ed","interval":{"startTime":1300,"endTime":1402},"skipId":"ed"},
                {"skipType":"op","interval":{"startTime":12,"endTime":99.5},"skipId":"op"},
                {"skipType":"recap","interval":{"startTime":0,"endTime":12},"skipId":"recap"},
                {"skipType":"op","interval":{"startTime":-1,"endTime":3}},
                {"skipType":"ed","interval":{"startTime":1400,"endTime":1405}},
                {"skipType":"recap","interval":{"startTime":5,"endTime":1}},
                {"skipType":"unknown","interval":{"startTime":100,"endTime":120}},
                {"skipType":"op","interval":{}}
            ]}
        """
    }
}
