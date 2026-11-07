package tachiyomi.domain.track.anime.interactor

import tachiyomi.domain.track.anime.model.AnimeTrack
import tachiyomi.domain.track.anime.repository.AnimeTrackRepository

class AnimeTracks(private val repository: AnimeTrackRepository) {
    suspend fun awaitForAnime(animeId: Long) = repository.awaitByAnimeId(animeId)

    suspend fun upsert(track: AnimeTrack) = repository.upsert(track)

    suspend fun delete(animeId: Long, syncId: Long) = repository.delete(animeId, syncId)
}
