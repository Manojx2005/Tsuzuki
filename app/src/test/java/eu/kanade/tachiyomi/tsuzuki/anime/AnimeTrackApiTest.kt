package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.tsuzuki.anime.track.AniListAnimeApi
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTrackEdit
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTrackStatus
import eu.kanade.tachiyomi.tsuzuki.anime.track.MyAnimeListAnimeApi
import eu.kanade.tachiyomi.tsuzuki.anime.track.nextAnimeTrackProgress
import eu.kanade.tachiyomi.tsuzuki.anime.track.toTrackDate
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.track.anime.model.AnimeTrack
import java.time.LocalDate

class AnimeTrackApiTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun aniListSearchUsesAnimeAndMapsMalId() = runBlocking {
        val client = respondingTo {
            assertEquals("POST", it.method)
            assertEquals("graphql.anilist.co", it.url.host)
            val request = it.jsonBody()
            assertTrue("type: ANIME" in request.getValue("query").jsonPrimitive.content)
            assertEquals("Title", request.getValue("variables").jsonObject.getValue("search").jsonPrimitive.content)
            """{"data":{"Page":{"media":[{"id":31,"idMal":15,"title":{"romaji":"Title"},"episodes":12}]}}}"""
        }
        val result = AniListAnimeApi(client, json).search("Title").single()
        assertEquals(31L, result.id)
        assertEquals(15L, result.malId)
        assertEquals(12L, result.totalEpisodes)
    }

    @Test
    fun aniListUpdatesOnlyTheRequestedEpisodeProgress() = runBlocking {
        val client = respondingTo {
            val request = it.jsonBody()
            assertTrue("SaveMediaListEntry" in request.getValue("query").jsonPrimitive.content)
            val variables = request.getValue("variables").jsonObject
            assertEquals(setOf("id", "progress"), variables.keys)
            assertEquals("6", variables.getValue("progress").jsonPrimitive.content)
            """
                {"data":{"SaveMediaListEntry":{"id":3,"status":"CURRENT","progress":6,"score":85,
                    "startedAt":{"year":2026,"month":1,"day":3},"media":{
                        "id":31,"idMal":15,"title":{"romaji":"Title"},"episodes":12}}}}
            """.trimIndent()
        }
        val track = AniListAnimeApi(client, json).update(track(2L, 31), AnimeTrackEdit(progress = 6))
        assertEquals(6.0, track.lastEpisodeSeen)
        assertEquals(8.5, track.score)
        assertEquals(15L, track.malId)
        assertEquals(LocalDate.of(2026, 1, 3).toTrackDate(), track.startDate)
    }

    @Test
    fun myAnimeListReadsAnimeListState() = runBlocking {
        val client = respondingTo {
            assertEquals("/v2/anime/15", it.url.encodedPath)
            assertEquals("my_list_status,num_episodes", it.url.queryParameter("fields"))
            """
                {"id":15,"title":"Remote title","num_episodes":12,
                 "my_list_status":{"status":"completed","num_episodes_watched":12,"score":9,"is_rewatching":true}}
            """.trimIndent()
        }
        val track = checkNotNull(MyAnimeListAnimeApi(client, json).entry(track(1L, 15)))
        assertEquals("Remote title", track.title)
        assertEquals(12.0, track.lastEpisodeSeen)
        assertEquals(AnimeTrackStatus.REWATCHING.idFor(1L), track.status)
    }

    @Test
    fun myAnimeListWritesEpisodeCountAndOnlyExplicitEdits() = runBlocking {
        val client = respondingTo {
            assertEquals("/v2/anime/15/my_list_status", it.url.encodedPath)
            assertEquals("PUT", it.method)
            val body = it.body as FormBody
            val fields = (0 until body.size).associate { i -> body.name(i) to body.value(i) }
            assertEquals(
                mapOf(
                    "num_watched_episodes" to "7",
                    "score" to "8",
                    "finish_date" to "2026-02-02",
                ),
                fields,
            )
            assertFalse("status" in fields)
            """{"status":"watching","num_episodes_watched":7,"score":8,"finish_date":"2026-02-02"}"""
        }
        val finish = LocalDate.of(2026, 2, 2).toTrackDate()
        val track = MyAnimeListAnimeApi(client, json).update(
            track(1L, 15),
            AnimeTrackEdit(progress = 7, score = 8.0, finishDate = finish),
        )
        assertEquals(7.0, track.lastEpisodeSeen)
        assertEquals(15L, track.malId)
        assertEquals(finish, track.finishDate)
    }

    @Test
    fun progressOnlyIncreasesAndStaysWithinKnownEpisodeCount() {
        val existing = track(2L, 31).copy(lastEpisodeSeen = 8.0, totalEpisodes = 12)
        assertNull(nextAnimeTrackProgress(existing, 7.0))
        assertNull(nextAnimeTrackProgress(existing, 8.99))
        assertNull(nextAnimeTrackProgress(existing, Double.NaN))
        assertEquals(9, nextAnimeTrackProgress(existing, 9.0))
        assertEquals(12, nextAnimeTrackProgress(existing, 13.0))
        assertNull(nextAnimeTrackProgress(existing.copy(lastEpisodeSeen = 15.0), 25.0))
    }

    private fun Request.jsonBody() = json.parseToJsonElement(
        Buffer().also { checkNotNull(body).writeTo(it) }.readUtf8(),
    ).jsonObject

    private fun track(service: Long, remoteId: Long) = AnimeTrack(
        animeId = 1,
        syncId = service,
        remoteId = remoteId,
        title = "Title",
        remoteUrl = "https://example.com",
    )

    private fun respondingTo(response: (Request) -> String): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(response(request).toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
}
