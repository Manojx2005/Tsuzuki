package tachiyomi.domain.track.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.track.anime.model.AnimeTrack

interface AnimeTrackRepository {
    suspend fun awaitByAnimeId(animeId: Long): List<AnimeTrack>
    fun subscribeByAnimeId(animeId: Long): Flow<List<AnimeTrack>>
    suspend fun upsert(track: AnimeTrack)
    suspend fun delete(animeId: Long, syncId: Long)
}
