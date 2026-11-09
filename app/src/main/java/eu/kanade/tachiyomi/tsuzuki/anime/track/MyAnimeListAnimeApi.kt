package eu.kanade.tachiyomi.tsuzuki.anime.track

import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import tachiyomi.domain.track.anime.model.AnimeTrack
import java.time.LocalDate
import kotlin.math.roundToInt

internal class MyAnimeListAnimeApi(private val client: OkHttpClient, private val json: Json) : AnimeTrackerApi {
    override suspend fun search(query: String): List<AnimeTrackSearch> {
        val url = BASE.toHttpUrl().newBuilder()
            .addQueryParameter("q", query.take(64))
            .addQueryParameter("limit", "20")
            .addQueryParameter("fields", "num_episodes,main_picture")
            .build()
        return request<SearchPage>(Request.Builder().url(url).build()).data.map { it.node.toResult() }
    }

    override suspend fun entry(track: AnimeTrack): AnimeTrack? {
        val url = BASE.toHttpUrl().newBuilder()
            .addPathSegment(track.remoteId.toString())
            .addQueryParameter("fields", "my_list_status,num_episodes")
            .build()
        val detailRequest = Request.Builder().url(url).cacheControl(CacheControl.FORCE_NETWORK).build()
        val anime = request<Media>(detailRequest)
        return anime.listStatus?.toTrack(track.copy(title = anime.title, totalEpisodes = anime.episodes))
    }

    override suspend fun update(track: AnimeTrack, edit: AnimeTrackEdit): AnimeTrack {
        val url = BASE.toHttpUrl().newBuilder()
            .addPathSegment(track.remoteId.toString())
            .addPathSegment("my_list_status")
            .build()
        val body = FormBody.Builder().apply {
            edit.progress?.let { add("num_watched_episodes", it.toString()) }
            edit.status?.let {
                add("status", it.myAnimeListStatus)
                add("is_rewatching", (it == AnimeTrackStatus.REWATCHING).toString())
            }
            edit.score?.let { add("score", it.roundToInt().coerceIn(0, 10).toString()) }
            edit.startDate?.let { add("start_date", it.toTrackLocalDate()?.toString().orEmpty()) }
            edit.finishDate?.let { add("finish_date", it.toTrackLocalDate()?.toString().orEmpty()) }
        }.build()
        return request<Entry>(Request.Builder().url(url).put(body).build()).toTrack(track)
    }

    private suspend inline fun <reified T> request(request: Request): T = with(json) {
        client.newCall(request).awaitSuccess().parseAs<T>()
    }

    @Serializable
    private data class SearchPage(val data: List<Node>)

    @Serializable
    private data class Node(val node: Media)

    @Serializable
    private data class Media(
        val id: Long,
        val title: String,
        @SerialName("num_episodes") val episodes: Long = 0,
        @SerialName("main_picture") val cover: Cover? = null,
        @SerialName("my_list_status") val listStatus: Entry? = null,
    ) {
        fun toResult() = AnimeTrackSearch(
            id,
            title,
            episodes,
            "https://myanimelist.net/anime/$id",
            cover?.large ?: cover?.medium,
            id,
        )
    }

    @Serializable
    private data class Cover(val large: String? = null, val medium: String? = null)

    @Serializable
    private data class Entry(
        val status: String,
        val score: Double = 0.0,
        @SerialName("num_episodes_watched") val episodesWatched: Int? = null,
        @SerialName("num_watched_episodes") val alternateEpisodesWatched: Int? = null,
        @SerialName("is_rewatching") val isRewatching: Boolean = false,
        @SerialName("start_date") val startDate: String? = null,
        @SerialName("finish_date") val finishDate: String? = null,
    ) {
        fun toTrack(base: AnimeTrack): AnimeTrack {
            val mappedStatus = if (isRewatching) {
                AnimeTrackStatus.REWATCHING
            } else {
                AnimeTrackStatus.entries.firstOrNull { it.myAnimeListStatus == status }
            }
            return base.copy(
                status = mappedStatus?.idFor(base.syncId) ?: 0,
                score = score,
                lastEpisodeSeen = checkNotNull(episodesWatched ?: alternateEpisodesWatched).toDouble(),
                startDate = startDate?.timestamp() ?: 0,
                finishDate = finishDate?.timestamp() ?: 0,
                malId = base.remoteId,
            )
        }

        private fun String.timestamp(): Long? = runCatching { LocalDate.parse(this).toTrackDate() }.getOrNull()
    }

    private companion object {
        const val BASE = "https://api.myanimelist.net/v2/anime"
    }
}
