package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.tsuzuki.anime.track.AniListAnimeApi
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTrackEdit
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTrackStatus
import eu.kanade.tachiyomi.tsuzuki.anime.track.KitsuAnimeApi
import eu.kanade.tachiyomi.tsuzuki.anime.track.MyAnimeListAnimeApi
import eu.kanade.tachiyomi.tsuzuki.anime.track.nextAnimeTrackProgress
import eu.kanade.tachiyomi.tsuzuki.anime.track.toTrackDate
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
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
    fun kitsuSearchUsesAnimeAndParsesJsonApiMedia() = runBlocking {
        val client = respondingTo {
            assertEquals("/api/edge/anime", it.url.encodedPath)
            assertEquals("Title & subtitle", it.url.queryParameter("filter[text]"))
            assertEquals("application/vnd.api+json", it.header("Accept"))
            """
                {"data":[{"id":"31","type":"anime","attributes":{
                  "canonicalTitle":"Title","slug":"title","episodeCount":12,
                  "posterImage":{"small":"https://example.com/small.png"}}},
                  {"id":"32","type":"anime","attributes":{"titles":{"en":"Fallback"},"episodeCount":null}},
                  {"id":"33","type":"anime","attributes":{}}]}
            """.trimIndent()
        }
        val results = kitsu(client).search("Title & subtitle")
        assertEquals(listOf("Title", "Fallback"), results.map { it.title })
        assertEquals(31L, results[0].id)
        assertEquals(12L, results[0].totalEpisodes)
        assertEquals("https://kitsu.app/anime/title", results[0].url)
        assertEquals("https://example.com/small.png", results[0].cover)
    }

    @Test
    fun kitsuFindsAnimeEntryForTheLoggedInUser() = runBlocking {
        val client = respondingTo {
            assertEquals("/api/edge/library-entries", it.url.encodedPath)
            assertEquals("31", it.url.queryParameter("filter[anime_id]"))
            assertEquals("42", it.url.queryParameter("filter[user_id]"))
            assertEquals("anime", it.url.queryParameter("include"))
            assertTrue(it.cacheControl.noCache)
            """
                {"data":[{"id":"9","type":"libraryEntries","attributes":{"status":"on_hold","progress":7,
                  "ratingTwenty":17,"private":true,"startedAt":"2026-01-03T00:00:00.000Z"}}],
                  "included":[{"id":"31","type":"anime","attributes":{"canonicalTitle":"New title","episodeCount":12}}]}
            """.trimIndent()
        }
        val result = checkNotNull(kitsu(client).entry(track(3L, 31)))
        assertEquals(9L, result.libraryId)
        assertEquals("New title", result.title)
        assertEquals(12L, result.totalEpisodes)
        assertEquals(7.0, result.lastEpisodeSeen)
        assertEquals(8.5, result.score)
        assertEquals(AnimeTrackStatus.ON_HOLD.idFor(3L), result.status)
        assertEquals(LocalDate.of(2026, 1, 3).toTrackDate(), result.startDate)
        assertTrue(result.isPrivate)
    }

    @Test
    fun kitsuCreatesAnAnimeRelationshipForTheCurrentAccount() = runBlocking {
        val client = respondingTo {
            assertEquals("POST", it.method)
            assertEquals("/api/edge/library-entries", it.url.encodedPath)
            val data = it.jsonBody().getValue("data").jsonObject
            assertEquals("libraryEntries", data.getValue("type").jsonPrimitive.content)
            val relationships = data.getValue("relationships").jsonObject
            val media = relationships.getValue("media").jsonObject.getValue("data").jsonObject
            val user = relationships.getValue("user").jsonObject.getValue("data").jsonObject
            assertEquals("anime", media.getValue("type").jsonPrimitive.content)
            assertEquals("31", media.getValue("id").jsonPrimitive.content)
            assertEquals("42", user.getValue("id").jsonPrimitive.content)
            assertEquals("planned", data.getValue("attributes").jsonObject.getValue("status").jsonPrimitive.content)
            """{"data":{"id":"9","attributes":{"status":"planned"}}}"""
        }
        val result = kitsu(client).update(track(3L, 31), AnimeTrackEdit(status = AnimeTrackStatus.PLANNING))
        assertEquals(9L, result.libraryId)
        assertEquals(AnimeTrackStatus.PLANNING.idFor(3L), result.status)
    }

    @Test
    fun kitsuUpdatesOnlyExplicitFieldsAndNeverSendsLowerProgress() = runBlocking {
        val client = respondingTo {
            assertEquals("PATCH", it.method)
            assertEquals("/api/edge/library-entries/9", it.url.encodedPath)
            val attributes = it.jsonBody().getValue("data").jsonObject.getValue("attributes").jsonObject
            assertEquals(setOf("status", "reconsuming", "ratingTwenty"), attributes.keys)
            assertEquals("current", attributes.getValue("status").jsonPrimitive.content)
            assertEquals("true", attributes.getValue("reconsuming").jsonPrimitive.content)
            assertEquals("17", attributes.getValue("ratingTwenty").jsonPrimitive.content)
            """{"data":{"id":"9","attributes":{"status":"current","reconsuming":true,"progress":7,"ratingTwenty":17}}}"""
        }
        val previous = track(3L, 31).copy(libraryId = 9, lastEpisodeSeen = 7.0, totalEpisodes = 12)
        val api = kitsu(client)
        assertEquals(previous, api.update(previous, AnimeTrackEdit(progress = 5)))
        val result = api.update(
            previous,
            AnimeTrackEdit(progress = 5, status = AnimeTrackStatus.REWATCHING, score = 8.5),
        )
        assertEquals(7.0, result.lastEpisodeSeen)
        assertEquals(8.5, result.score)
        assertEquals(AnimeTrackStatus.REWATCHING.idFor(3L), result.status)
    }

    @Test
    fun kitsuSendsOnlyBoundedEpisodeProgressDuringAutoSync() = runBlocking {
        val client = respondingTo {
            val attributes = it.jsonBody().getValue("data").jsonObject.getValue("attributes").jsonObject
            assertEquals(setOf("progress"), attributes.keys)
            assertEquals("12", attributes.getValue("progress").jsonPrimitive.content)
            """{"data":{"id":"9","attributes":{"status":"current","progress":12}}}"""
        }
        val previous = track(3L, 31).copy(libraryId = 9, lastEpisodeSeen = 7.0, totalEpisodes = 12)
        assertEquals(12.0, kitsu(client).update(previous, AnimeTrackEdit(progress = 13)).lastEpisodeSeen)
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

    private fun kitsu(client: OkHttpClient) = KitsuAnimeApi(client, json, "https://kitsu.app/api/edge/".toHttpUrl()) {
        "42"
    }

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
