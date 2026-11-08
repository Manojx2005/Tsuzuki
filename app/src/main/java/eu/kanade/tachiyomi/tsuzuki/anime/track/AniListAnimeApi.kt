package eu.kanade.tachiyomi.tsuzuki.anime.track

import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.domain.track.anime.model.AnimeTrack
import java.time.LocalDate
import kotlin.math.roundToInt

internal class AniListAnimeApi(private val client: OkHttpClient, private val json: Json) : AnimeTrackerApi {
    override suspend fun search(query: String): List<AnimeTrackSearch> {
        val data = request(SEARCH, buildJsonObject { put("search", query) })
        return json.decodeFromJsonElement<SearchPage>(data.getValue("Page")).media.map(Media::toResult)
    }

    override suspend fun entry(track: AnimeTrack): AnimeTrack? {
        val data = request(ENTRY, buildJsonObject { put("id", track.remoteId) })
        val media = json.decodeFromJsonElement<Media>(data.getValue("Media"))
        return media.mediaListEntry?.toTrack(media, track)
    }

    override suspend fun update(track: AnimeTrack, edit: AnimeTrackEdit): AnimeTrack {
        val variables = buildJsonObject {
            put("id", track.remoteId)
            edit.progress?.let { put("progress", it) }
            edit.status?.let { put("status", it.aniListStatus) }
            edit.score?.let { put("score", (it * 10).roundToInt().coerceIn(0, 100)) }
            edit.startDate?.let { putDate("start", it) }
            edit.finishDate?.let { putDate("finish", it) }
        }
        val entry = json.decodeFromJsonElement<Entry>(request(SAVE, variables).getValue("SaveMediaListEntry"))
        return entry.toTrack(checkNotNull(entry.media), track)
    }

    private suspend fun request(query: String, variables: JsonObject): JsonObject {
        val body = buildJsonObject {
            put("query", query)
            put("variables", variables)
        }
        val response = with(json) {
            client.newCall(POST(GRAPHQL, body = body.toString().toRequestBody(jsonMime)))
                .awaitSuccess()
                .parseAs<JsonObject>()
        }
        check("errors" !in response) { "AniList rejected the anime tracker request" }
        return response.getValue("data").jsonObject
    }

    private fun JsonObjectBuilder.putDate(key: String, timestamp: Long) {
        val date = timestamp.toTrackLocalDate()
        putJsonObject(key) {
            put("year", date?.year)
            put("month", date?.monthValue)
            put("day", date?.dayOfMonth)
        }
    }

    @Serializable
    private data class SearchPage(val media: List<Media>)

    @Serializable
    private data class Media(
        val id: Long,
        val idMal: Long? = null,
        val title: Titles,
        val episodes: Long? = null,
        val coverImage: Cover? = null,
        val mediaListEntry: Entry? = null,
    ) {
        fun toResult() = AnimeTrackSearch(
            id = id,
            title = title.english ?: title.romaji ?: title.native.orEmpty(),
            totalEpisodes = episodes ?: 0L,
            url = "https://anilist.co/anime/$id",
            cover = coverImage?.large,
            malId = idMal,
        )
    }

    @Serializable
    private data class Titles(val english: String? = null, val romaji: String? = null, val native: String? = null)

    @Serializable
    private data class Cover(val large: String? = null)

    @Serializable
    private data class Entry(
        val id: Long,
        val status: String,
        val progress: Int? = null,
        val score: Double? = null,
        val private: Boolean = false,
        val startedAt: Date? = null,
        val completedAt: Date? = null,
        val media: Media? = null,
    ) {
        fun toTrack(media: Media, base: AnimeTrack): AnimeTrack {
            val result = media.toResult()
            return base.copy(
                libraryId = id,
                title = result.title,
                totalEpisodes = result.totalEpisodes,
                status = AnimeTrackStatus.entries.firstOrNull { it.aniListStatus == status }?.idFor(base.syncId) ?: 0,
                lastEpisodeSeen = progress?.toDouble() ?: 0.0,
                score = (score ?: 0.0) / 10,
                isPrivate = private,
                startDate = startedAt?.timestamp() ?: 0,
                finishDate = completedAt?.timestamp() ?: 0,
                malId = result.malId ?: base.malId,
            )
        }
    }

    @Serializable
    private data class Date(val year: Int? = null, val month: Int? = null, val day: Int? = null) {
        fun timestamp(): Long? {
            val y = year ?: return null
            val m = month ?: return null
            val d = day ?: return null
            return runCatching { LocalDate.of(y, m, d).toTrackDate() }.getOrNull()
        }
    }

    private companion object {
        const val GRAPHQL = "https://graphql.anilist.co/"
        const val MEDIA = "id idMal title { english romaji native } episodes coverImage { large }"
        const val LIST = "id status progress score(format: POINT_100) private startedAt { year month day } " +
            "completedAt { year month day }"

        val SEARCH = $$"""
            query AnimeSearch($search: String) {
                Page(page: 1, perPage: 20) {
                    media(search: $search, type: ANIME) { $${MEDIA} }
                }
            }
        """.trimIndent()

        val ENTRY = $$"""
            query AnimeEntry($id: Int) {
                Media(id: $id, type: ANIME) { $${MEDIA} mediaListEntry { $${LIST} } }
            }
        """.trimIndent()

        val SAVE = $$"""
            mutation AnimeSave($id: Int, $progress: Int, $status: MediaListStatus, $score: Int,
                $start: FuzzyDateInput, $finish: FuzzyDateInput) {
                SaveMediaListEntry(mediaId: $id, progress: $progress, status: $status, scoreRaw: $score,
                    startedAt: $start, completedAt: $finish) { $${LIST} media { $${MEDIA} } }
            }
        """.trimIndent()
    }
}
