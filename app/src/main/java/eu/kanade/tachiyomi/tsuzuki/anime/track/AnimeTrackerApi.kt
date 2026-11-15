package eu.kanade.tachiyomi.tsuzuki.anime.track

import eu.kanade.tachiyomi.data.track.TrackerManager
import tachiyomi.domain.track.anime.model.AnimeTrack
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.floor

internal interface AnimeTrackerApi {
    suspend fun search(query: String): List<AnimeTrackSearch>
    suspend fun entry(track: AnimeTrack): AnimeTrack?
    suspend fun update(track: AnimeTrack, edit: AnimeTrackEdit): AnimeTrack
}

internal data class AnimeTrackSearch(
    val id: Long,
    val title: String,
    val totalEpisodes: Long,
    val url: String,
    val cover: String? = null,
    val malId: Long? = null,
) {
    fun track(animeId: Long, trackerId: Long) = AnimeTrack(
        animeId = animeId,
        syncId = trackerId,
        remoteId = id,
        title = title,
        remoteUrl = url,
        totalEpisodes = totalEpisodes,
        malId = malId,
    )
}

internal data class AnimeTrackEdit(
    val progress: Int? = null,
    val status: AnimeTrackStatus? = null,
    val score: Double? = null,
    val startDate: Long? = null,
    val finishDate: Long? = null,
)

internal enum class AnimeTrackStatus(
    private val aniListId: Long,
    private val myAnimeListId: Long,
    val aniListStatus: String,
    val myAnimeListStatus: String,
) {
    WATCHING(1, 1, "CURRENT", "watching"),
    COMPLETED(2, 2, "COMPLETED", "completed"),
    ON_HOLD(3, 3, "PAUSED", "on_hold"),
    DROPPED(4, 4, "DROPPED", "dropped"),
    PLANNING(5, 6, "PLANNING", "plan_to_watch"),
    REWATCHING(6, 7, "REPEATING", "watching"),
    ;

    fun idFor(trackerId: Long) = when (trackerId) {
        TrackerManager.ANILIST, TrackerManager.KITSU -> aniListId
        else -> myAnimeListId
    }
}

internal fun nextAnimeTrackProgress(remote: AnimeTrack, seenNumber: Double): Int? {
    if (!seenNumber.isFinite() || seenNumber < 1.0) return null
    val maximum = remote.totalEpisodes.takeIf { it > 0 } ?: Int.MAX_VALUE.toLong()
    val candidate = floor(seenNumber).toLong().coerceAtMost(maximum).coerceAtMost(Int.MAX_VALUE.toLong())
    return candidate.toInt().takeIf { it > remote.lastEpisodeSeen }
}

internal fun LocalDate.toTrackDate(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun Long.toTrackLocalDate(): LocalDate? =
    takeIf { it != 0L }?.let { Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC).toLocalDate() }
