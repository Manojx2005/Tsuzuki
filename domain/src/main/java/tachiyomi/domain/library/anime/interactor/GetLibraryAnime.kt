package tachiyomi.domain.library.anime.interactor

import tachiyomi.domain.entries.anime.repository.AnimeRepository

class GetLibraryAnime(private val repository: AnimeRepository) {
    fun subscribe() = repository.subscribeLibrary()
}
