package tachiyomi.domain.track.anime.repository

import tachiyomi.domain.track.anime.model.AnimeTrack

interface AnimeTrackRepository {
    suspend fun awaitByAnimeId(animeId: Long): List<AnimeTrack>
    suspend fun upsert(track: AnimeTrack)
    suspend fun delete(animeId: Long, syncId: Long)
}
