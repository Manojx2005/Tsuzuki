package tachiyomi.domain.entries.anime.interactor

import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.repository.AnimeRepository

class GetAnime(private val repository: AnimeRepository) {
    suspend operator fun invoke(id: Long) = repository.awaitById(id)

    suspend operator fun invoke(source: Long, url: String) = repository.awaitBySourceAndUrl(source, url)
}

class InsertAnime(private val repository: AnimeRepository) {
    suspend operator fun invoke(anime: Anime) = repository.insert(anime)
}

class UpdateAnime(private val repository: AnimeRepository) {
    suspend operator fun invoke(anime: Anime) = repository.update(anime)
}
