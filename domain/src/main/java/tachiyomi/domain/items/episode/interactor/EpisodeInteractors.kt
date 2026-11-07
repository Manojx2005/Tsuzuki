package tachiyomi.domain.items.episode.interactor

import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.repository.EpisodeRepository

class GetEpisodesByAnimeId(private val repository: EpisodeRepository) {
    suspend operator fun invoke(animeId: Long) = repository.awaitByAnimeId(animeId)

    fun subscribe(animeId: Long) = repository.subscribeByAnimeId(animeId)
}

class InsertEpisodes(private val repository: EpisodeRepository) {
    suspend operator fun invoke(episodes: List<Episode>) = repository.insertAll(episodes)
}
