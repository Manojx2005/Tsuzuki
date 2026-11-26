package eu.kanade.tachiyomi.tsuzuki.anime.debug

import eu.kanade.tachiyomi.animesource.model.ChapterType
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.TimeStamp
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.Headers.Companion.toHeaders

/**
 * Maps the fixture server's JSON (scripts/anime-fixture/serve.py) onto the extension models.
 * Pure, so the tests run on canned JSON without a network.
 */
internal object FixtureJson {

    /** Marks a hoster whose `getVideoList(hoster)` must throw (case 03). */
    const val THROWS = "fixture:throws"

    /** The case id ("01"…"16") from an anime or episode url such as `/anime/03` or `/episode/03/2`. */
    fun caseOf(url: String): String = url.trim('/').split('/')[1]

    /** The case list; [noHosters] picks the lib16 case(s) (true) or all the others (false). */
    fun animeList(json: String, noHosters: Boolean): List<SAnime> =
        Json.parseToJsonElement(json).jsonArray.map { it.jsonObject }
            .filter { (it.bool("noHosters") ?: false) == noHosters }
            .map { item ->
                SAnime.create().apply {
                    url = item.string("url")!!
                    title = item.string("title")!!
                    thumbnail_url = item.string("thumbnail")
                    description = "Tsuzuki fixture case ${item.string("case")}"
                    status = SAnime.COMPLETED
                    initialized = true
                }
            }

    fun episodes(json: String): List<SEpisode> =
        Json.parseToJsonElement(json).jsonArray.map { it.jsonObject }.map { item ->
            SEpisode.create().apply {
                url = item.string("url")!!
                name = item.string("name")!!
                episode_number = item.getValue("number").jsonPrimitive.double.toFloat()
                date_upload = item.getValue("dateUpload").jsonPrimitive.long
                preview_url = item.string("previewUrl")
            }
        }

    /**
     * The hosters of one case. A lazy or throwing hoster carries no videos, so the app has to call
     * `getVideoList(hoster)`; the others come with their videos, as most extensions do.
     */
    fun hosters(json: String): List<Hoster> =
        root(json).getValue("hosters").jsonArray.mapIndexed { index, element ->
            val item = element.jsonObject
            val throws = item.bool("throws") ?: false
            val lazy = item.bool("lazy") ?: false
            val slow = isSlowList(item)
            Hoster(
                hosterUrl = "#$index",
                hosterName = item.string("name")!!,
                videoList = if (throws || lazy || slow) null else videos(item),
                internalData = if (throws) THROWS else item.toString(),
                lazy = lazy,
            )
        }

    /** Case 17: a hoster whose `getVideoList(hoster)` first waits on the server's slow route. */
    fun isSlowList(hoster: JsonObject): Boolean = hoster.bool("slowList") ?: false

    /** All videos of the first hoster; what the lib16 source returns from `getVideoList(episode)`. */
    fun firstHosterVideos(json: String): List<Video> =
        videos(root(json).getValue("hosters").jsonArray.first().jsonObject)

    /** The videos of one hoster's JSON object (also used for [Hoster.internalData]). */
    fun videos(hoster: JsonObject): List<Video> =
        (hoster["videos"] as? JsonArray).orEmpty().map { it.jsonObject }.map { video ->
            val quality = video.string("quality") ?: ""
            Video(
                videoUrl = video.string("url")!!,
                videoTitle = quality,
                resolution = quality.takeWhile { it.isDigit() }.toIntOrNull(),
                headers = (video["headers"] as? JsonObject)
                    ?.mapValues { (_, value) -> value.jsonPrimitive.content }
                    ?.toHeaders(),
                preferred = video.bool("preferred") ?: false,
                subtitleTracks = (video["subtitleTracks"] as? JsonArray).orEmpty().map {
                    Track(
                        it.jsonObject.string("url")!!,
                        it.jsonObject.string("title") ?: it.jsonObject.string("lang")!!,
                    )
                },
                timestamps = (video["timestamps"] as? JsonArray).orEmpty().map {
                    val stamp = it.jsonObject
                    TimeStamp(
                        start = stamp.getValue("start").jsonPrimitive.double,
                        end = stamp.getValue("end").jsonPrimitive.double,
                        name = stamp.string("name") ?: "",
                        type = stamp.string("type")?.let { type -> ChapterType.valueOf(type) } ?: ChapterType.Other,
                    )
                },
                mpvArgs = (video["mpvArgs"] as? JsonArray).orEmpty().map {
                    val pair = it.jsonArray
                    pair[0].jsonPrimitive.content to pair[1].jsonPrimitive.content
                },
            )
        }

    /** Moves the videos whose title starts with [quality] (e.g. "720") to the front, keeping order otherwise. */
    fun preferQuality(videos: List<Video>, quality: String): List<Video> =
        videos.sortedByDescending { it.videoTitle.startsWith(quality) }

    private fun root(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonElement)
        ?.takeUnless { it is JsonArray || it is JsonObject }?.jsonPrimitive?.contentOrNull

    private fun JsonObject.bool(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull
}
