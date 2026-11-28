package eu.kanade.tachiyomi.tsuzuki.anime.track.simkl

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTrackEdit
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTrackSearch
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTrackStatus
import eu.kanade.tachiyomi.tsuzuki.anime.track.AnimeTrackerApi
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.domain.track.anime.model.AnimeTrack
import kotlin.math.roundToInt

/**
 * A5.4: Simkl as an anime tracker, write-only by design. Simkl's API rules forbid reading the whole library to look
 * one title up (`/sync/all-items` without `/sync/activities` gets the client id suspended), so [entry] returns the
 * locally stored track and Tsuzuki only sends changes: the watchlist status (`/sync/add-to-list`), watched episodes
 * (`/sync/history`, `/sync/history/remove`) and the rating (`/sync/ratings`). Writes are spaced over a second, the
 * API's POST limit.
 */
internal class SimklAnimeApi(private val client: OkHttpClient, private val json: Json) : AnimeTrackerApi {

    override suspend fun search(query: String): List<AnimeTrackSearch> {
        val url = "${SimklAuth.API_URL}/search/anime".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("extended", "full")
            .addQueryParameter("limit", "20")
            .build()
        val body = client.newCall(GET(url)).awaitSuccess().use { it.body.string() }
        return parseSimklSearch(json.parseToJsonElement(body).let { it as? JsonArray } ?: JsonArray(emptyList()))
    }

    /** No remote read (see the class note): a bound track is what Tsuzuki last sent; an unbound one is new. */
    override suspend fun entry(track: AnimeTrack): AnimeTrack? = track.takeIf { it.libraryId != null }

    override suspend fun update(track: AnimeTrack, edit: AnimeTrackEdit): AnimeTrack {
        var current = track.copy(libraryId = track.remoteId)
        var posted = false
        suspend fun post(path: String, payload: JsonObject) {
            if (posted) delay(POST_SPACING_MS)
            posted = true
            val body = json.encodeToString(JsonObject.serializer(), payload).toRequestBody(jsonMime)
            client.newCall(POST("${SimklAuth.API_URL}$path", body = body)).awaitSuccess().close()
        }
        val ids = buildJsonObject {
            put("simkl", track.remoteId)
            track.malId?.let { put("mal", it) }
        }
        edit.status?.let { status ->
            post("/sync/add-to-list", animePayload(ids) { put("to", status.simklStatus) })
            current = current.copy(status = status.idFor(track.syncId))
        }
        edit.progress?.let { progress ->
            val seen = track.lastEpisodeSeen.toInt()
            simklEpisodeChange(seen, progress)?.let { (path, episodes) ->
                post(path, animePayload(ids) { putJsonArray("episodes") { episodes.forEach { add(episode(it)) } } })
            }
            current = current.copy(lastEpisodeSeen = progress.toDouble())
        }
        edit.score?.let { score ->
            val rating = score.roundToInt().coerceIn(0, 10)
            if (rating > 0) {
                post("/sync/ratings", animePayload(ids) { put("rating", rating) })
            } else {
                post("/sync/ratings/remove", animePayload(ids) {})
            }
            current = current.copy(score = rating.toDouble())
        }
        if (!posted) {
            // A first bind with nothing else to send still puts the anime on the user's list.
            post("/sync/add-to-list", animePayload(ids) { put("to", "plantowatch") })
        }
        edit.startDate?.let { current = current.copy(startDate = it) }
        edit.finishDate?.let { current = current.copy(finishDate = it) }
        return current
    }

    private fun animePayload(ids: JsonObject, fields: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit) =
        buildJsonObject {
            putJsonArray("anime") {
                add(
                    buildJsonObject {
                        put("ids", ids)
                        fields()
                    },
                )
            }
        }

    private fun episode(number: Int) = buildJsonObject { put("number", number) }

    companion object {
        private const val POST_SPACING_MS = 1_100L
    }
}

/** Simkl's watchlist names for Tsuzuki's statuses (Simkl has no "rewatching": it is "watching"). */
internal val AnimeTrackStatus.simklStatus: String
    get() = when (this) {
        AnimeTrackStatus.WATCHING, AnimeTrackStatus.REWATCHING -> "watching"
        AnimeTrackStatus.COMPLETED -> "completed"
        AnimeTrackStatus.ON_HOLD -> "hold"
        AnimeTrackStatus.DROPPED -> "dropped"
        AnimeTrackStatus.PLANNING -> "plantowatch"
    }

/**
 * Which episodes to send when progress moves from [seen] to [progress]: added to history going forward, removed
 * going back; at most [MAX_EPISODES_PER_CALL] per call (the rest catch up on the next change). Null when equal.
 */
internal fun simklEpisodeChange(seen: Int, progress: Int): Pair<String, List<Int>>? = when {
    progress > seen -> "/sync/history" to ((seen + 1)..progress).toList().takeLast(MAX_EPISODES_PER_CALL)
    progress < seen -> "/sync/history/remove" to ((progress + 1)..seen).toList().take(MAX_EPISODES_PER_CALL)
    else -> null
}

internal const val MAX_EPISODES_PER_CALL = 500

/** `/search/anime?extended=full` items → search results; the link is the item's own Simkl page (API rule 1). */
internal fun parseSimklSearch(items: JsonArray): List<AnimeTrackSearch> = items.mapNotNull { element ->
    val item = element as? JsonObject ?: return@mapNotNull null
    val ids = item["ids"]?.jsonObject ?: return@mapNotNull null
    val id = (ids["simkl_id"] ?: ids["simkl"])?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
    val slug = ids["slug"]?.jsonPrimitive?.contentOrNull
    val title = listOf("title_en", "title", "title_romaji")
        .firstNotNullOfOrNull { item[it]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank) }
        ?: return@mapNotNull null
    val poster = item["poster"]?.jsonPrimitive?.contentOrNull
    AnimeTrackSearch(
        id = id,
        title = title,
        totalEpisodes = (item["total_episodes"] ?: item["ep_count"])?.jsonPrimitive?.longOrNull ?: 0L,
        url = "https://simkl.com/anime/$id" + (slug?.let { "/$it" } ?: ""),
        cover = poster?.let { "https://wsrv.nl/?url=https://simkl.in/posters/${it}_m.webp&q=90" },
        malId = ids["mal"]?.jsonPrimitive?.contentOrNull?.toLongOrNull(),
    )
}
