package eu.kanade.tachiyomi.tsuzuki.anime.track

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.domain.track.anime.model.AnimeTrack
import java.time.Instant
import kotlin.math.roundToInt

internal class KitsuAnimeApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val base: HttpUrl,
    private val userId: () -> String,
) : AnimeTrackerApi {
    override suspend fun search(query: String): List<AnimeTrackSearch> {
        if (query.isBlank()) return emptyList()
        val url = base.newBuilder()
            .addPathSegment("anime")
            .addQueryParameter("filter[text]", query.trim())
            .addQueryParameter("page[limit]", "20")
            .build()
        return request<AnimePage>(requestTo(url).build()).data.mapNotNull(Media::toSearch)
    }

    override suspend fun entry(track: AnimeTrack): AnimeTrack? {
        val url = base.newBuilder()
            .addPathSegment("library-entries")
            .addQueryParameter("filter[anime_id]", track.remoteId.toString())
            .addQueryParameter("filter[user_id]", userId())
            .addQueryParameter("include", "anime")
            .build()
        val entries = request<EntryPage>(requestTo(url).cacheControl(CacheControl.FORCE_NETWORK).build())
        val media = entries.included.firstOrNull { it.id == track.remoteId }?.toSearch()
        val local = media?.let { track.copy(title = it.title, totalEpisodes = it.totalEpisodes, remoteUrl = it.url) }
            ?: track
        return entries.data.firstOrNull()?.toTrack(local)
    }

    override suspend fun update(track: AnimeTrack, edit: AnimeTrackEdit): AnimeTrack {
        val libraryId = track.libraryId
        val status = edit.status ?: AnimeTrackStatus.PLANNING.takeIf { libraryId == null }
        val attributes = buildJsonObject {
            edit.progress?.let { nextAnimeTrackProgress(track, it.toDouble()) }?.let { put("progress", it) }
            status?.let {
                put("status", it.kitsuStatus())
                put("reconsuming", it == AnimeTrackStatus.REWATCHING)
            }
            edit.score?.takeIf(Double::isFinite)?.let {
                put(
                    "ratingTwenty",
                    it.takeIf { score -> score > 0 }?.let { score ->
                        (score.coerceAtMost(10.0) * 2).roundToInt().coerceAtLeast(2)
                    },
                )
            }
            edit.startDate?.let { put("startedAt", it.toKitsuDate()) }
            edit.finishDate?.let { put("finishedAt", it.toKitsuDate()) }
        }
        if (attributes.isEmpty()) return track

        val body = buildJsonObject {
            putJsonObject("data") {
                put("type", "libraryEntries")
                put("attributes", attributes)
                if (libraryId == null) {
                    putJsonObject("relationships") {
                        putJsonObject("user") {
                            putJsonObject("data") {
                                put("type", "users")
                                put("id", userId())
                            }
                        }
                        putJsonObject("media") {
                            putJsonObject("data") {
                                put("type", "anime")
                                put("id", track.remoteId.toString())
                            }
                        }
                    }
                } else {
                    put("id", libraryId.toString())
                }
            }
        }.toString().toRequestBody(JSON_API.toMediaType())
        val url = base.newBuilder().addPathSegment("library-entries")
        val call = if (libraryId == null) {
            requestTo(url.build()).post(body)
        } else {
            requestTo(url.addPathSegment(libraryId.toString()).build()).patch(body)
        }
        return request<EntryResponse>(call.build()).data.toTrack(track)
    }

    private fun requestTo(url: HttpUrl): Request.Builder = Request.Builder().url(url).header("Accept", JSON_API)

    private suspend inline fun <reified T> request(request: Request): T = with(json) {
        client.newCall(request).awaitSuccess().parseAs<T>()
    }

    @Serializable
    private data class AnimePage(val data: List<Media>)

    @Serializable
    private data class Media(val id: Long, val attributes: AnimeAttributes) {
        fun toSearch(): AnimeTrackSearch? {
            val title = attributes.canonicalTitle?.takeIf(String::isNotBlank)
                ?: attributes.titles?.values?.firstOrNull { !it.isNullOrBlank() }
                ?: return null
            return AnimeTrackSearch(
                id = id,
                title = title,
                totalEpisodes = attributes.episodeCount ?: 0,
                url = "https://kitsu.app/anime/".toHttpUrl().newBuilder()
                    .addPathSegment(attributes.slug?.takeIf(String::isNotBlank) ?: id.toString())
                    .build().toString(),
                cover = attributes.posterImage?.let { it["small"] ?: it["medium"] ?: it["original"] },
            )
        }
    }

    @Serializable
    private data class AnimeAttributes(
        val canonicalTitle: String? = null,
        val titles: Map<String, String?>? = null,
        val slug: String? = null,
        val episodeCount: Long? = null,
        val posterImage: Map<String, String?>? = null,
    )

    @Serializable
    private data class EntryPage(val data: List<Entry>, val included: List<Media> = emptyList())

    @Serializable
    private data class EntryResponse(val data: Entry)

    @Serializable
    private data class Entry(val id: Long, val attributes: EntryAttributes) {
        fun toTrack(track: AnimeTrack) = track.copy(
            libraryId = id,
            lastEpisodeSeen = attributes.progress.toDouble(),
            status = attributes.animeStatus()?.idFor(TrackerManager.KITSU) ?: track.status,
            score = attributes.ratingTwenty?.div(2.0) ?: 0.0,
            startDate = attributes.startedAt.fromKitsuDate(),
            finishDate = attributes.finishedAt.fromKitsuDate(),
            isPrivate = attributes.privateEntry,
        )
    }

    @Serializable
    private data class EntryAttributes(
        val status: String,
        val progress: Int = 0,
        val ratingTwenty: Int? = null,
        val reconsuming: Boolean = false,
        val startedAt: String? = null,
        val finishedAt: String? = null,
        @SerialName("private") val privateEntry: Boolean = false,
    ) {
        fun animeStatus() = if (reconsuming) {
            AnimeTrackStatus.REWATCHING
        } else {
            when (status) {
                "current" -> AnimeTrackStatus.WATCHING
                "completed" -> AnimeTrackStatus.COMPLETED
                "on_hold", "onHold" -> AnimeTrackStatus.ON_HOLD
                "dropped" -> AnimeTrackStatus.DROPPED
                "planned" -> AnimeTrackStatus.PLANNING
                else -> null
            }
        }
    }

    private companion object {
        const val JSON_API = "application/vnd.api+json"
    }
}

private fun AnimeTrackStatus.kitsuStatus() = when (this) {
    AnimeTrackStatus.WATCHING, AnimeTrackStatus.REWATCHING -> "current"
    AnimeTrackStatus.COMPLETED -> "completed"
    AnimeTrackStatus.ON_HOLD -> "on_hold"
    AnimeTrackStatus.DROPPED -> "dropped"
    AnimeTrackStatus.PLANNING -> "planned"
}

private fun Long.toKitsuDate() = takeIf { it != 0L }?.let { Instant.ofEpochMilli(it).toString() }

private fun String?.fromKitsuDate(): Long =
    this?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: 0L
