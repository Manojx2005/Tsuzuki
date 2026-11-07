package tachiyomi.domain.category.anime.interactor

import tachiyomi.domain.category.anime.model.AnimeCategory
import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository

class AnimeCategories(private val repository: AnimeCategoryRepository) {
    fun subscribe() = repository.subscribeAll()

    suspend fun awaitForAnime(animeId: Long) = repository.awaitByAnimeId(animeId)

    suspend fun insert(name: String, flags: Long = 0) = repository.insert(name.trim(), flags)

    suspend fun update(category: AnimeCategory) = repository.update(category.copy(name = category.name.trim()))

    suspend fun delete(id: Long) = repository.delete(id)

    suspend fun reorder(ids: List<Long>) = repository.reorder(ids)
}

class SetAnimeCategories(private val repository: AnimeCategoryRepository) {
    suspend operator fun invoke(animeId: Long, ids: Set<Long>) = repository.setForAnime(animeId, ids)
}
