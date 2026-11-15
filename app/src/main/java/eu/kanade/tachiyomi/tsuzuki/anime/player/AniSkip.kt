package eu.kanade.tachiyomi.tsuzuki.anime.player

import eu.kanade.tachiyomi.network.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.track.anime.model.AnimeTrack
import tachiyomi.domain.track.anime.repository.AnimeTrackRepository
import java.io.IOException
import kotlin.math.roundToLong

internal enum class AniSkipMode { OFF, BUTTON, AUTO }

internal enum class AniSkipKind { OPENING, ENDING, RECAP }

internal data class AniSkipSegment(val kind: AniSkipKind, val start: Double, val end: Double)

internal data class AniSkipKey(val malId: Long, val episode: Int, val length: Long)

internal fun aniSkipKey(bindings: List<AnimeTrack>, episode: Double, duration: Double): AniSkipKey? {
    if (!episode.isFinite() || episode !in 1.0..Int.MAX_VALUE.toDouble() || episode % 1.0 != 0.0 ||
        !duration.isFinite() || duration < 1.0
    ) {
        return null
    }
    val malId = bindings.sortedBy(AnimeTrack::syncId)
        .firstNotNullOfOrNull { it.malId?.takeIf { id -> id > 0 } } ?: return null
    return AniSkipKey(malId, episode.toInt(), duration.roundToLong())
}

internal fun parseAniSkipSegments(body: String, duration: Double): List<AniSkipSegment> {
    val response = Json.decodeFromString<JsonObject>(body)
    val found = (response["found"] as? JsonPrimitive)?.booleanOrNull
        ?: throw IOException("Missing AniSkip result status")
    if (!found) return emptyList()
    val results = response["results"] as? JsonArray ?: throw IOException("Missing AniSkip intervals")
    return results.mapNotNull { value ->
        val result = value as? JsonObject ?: return@mapNotNull null
        val kind = when ((result["skipType"] as? JsonPrimitive)?.contentOrNull) {
            "op", "mixed-op" -> AniSkipKind.OPENING
            "ed", "mixed-ed" -> AniSkipKind.ENDING
            "recap" -> AniSkipKind.RECAP
            else -> return@mapNotNull null
        }
        val interval = result["interval"] as? JsonObject ?: return@mapNotNull null
        val start = (interval["startTime"] as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
        val end = (interval["endTime"] as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
        if (!start.isFinite() || !end.isFinite() || start < 0 || start >= duration || end <= start) {
            return@mapNotNull null
        }
        AniSkipSegment(kind, start, end.coerceAtMost(duration))
    }.distinct().sortedBy(AniSkipSegment::start)
}

internal fun List<AniSkipSegment>.atPosition(position: Double): AniSkipSegment? =
    firstOrNull { position >= it.start && position < it.end }

internal fun AniSkipSegment.canAutoSkip(position: Double, introDelay: Int): Boolean {
    val earliest = start + if (kind == AniSkipKind.OPENING) introDelay else 0
    return position >= earliest && position < end
}

internal class AniSkipLookup(private val client: OkHttpClient, private val tracks: AnimeTrackRepository) {
    private val cache = object : LinkedHashMap<AniSkipKey, List<AniSkipSegment>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<AniSkipKey, List<AniSkipSegment>>): Boolean =
            size > 128
    }

    suspend fun segments(episode: Episode, duration: Double): List<AniSkipSegment> = withContext(Dispatchers.IO) {
        val key = aniSkipKey(tracks.awaitByAnimeId(episode.animeId), episode.episodeNumber, duration)
            ?: return@withContext emptyList()
        synchronized(cache) { cache[key] }?.let { return@withContext it }

        val url = "https://api.aniskip.com/v2/skip-times".toHttpUrl().newBuilder()
            .addPathSegment(key.malId.toString())
            .addPathSegment(key.episode.toString())
            .addQueryParameter("episodeLength", key.length.toString())
            .apply {
                listOf("op", "ed", "mixed-op", "mixed-ed", "recap").forEachIndexed { index, type ->
                    addQueryParameter("types[$index]", type)
                }
            }
            .build()
        val request = Request.Builder().url(url).cacheControl(CacheControl.Builder().noStore().build()).build()
        val results = client.newCall(request).await().use { response ->
            when {
                response.code == 404 -> emptyList()
                response.isSuccessful -> parseAniSkipSegments(response.body.string(), key.length.toDouble())
                else -> throw IOException("AniSkip returned HTTP ${response.code}")
            }
        }
        synchronized(cache) { cache[key] = results }
        results
    }
}
