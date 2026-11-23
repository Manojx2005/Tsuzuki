package tachiyomi.domain.track.anime.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import tachiyomi.domain.track.anime.model.AnimeTrack

interface AnimeTrackRepository {
    suspend fun awaitByAnimeId(animeId: Long): List<AnimeTrack>
    fun subscribeByAnimeId(animeId: Long): Flow<List<AnimeTrack>>
    suspend fun upsert(track: AnimeTrack)
    suspend fun delete(animeId: Long, syncId: Long)

    /** Anime with at least one tracker (AP8, the library's Tracked filter). */
    fun subscribeTrackedAnimeIds(): Flow<Set<Long>> = flowOf(emptySet())
}
