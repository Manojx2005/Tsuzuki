package tachiyomi.domain.category.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.category.anime.model.AnimeCategory

interface AnimeCategoryRepository {
    suspend fun awaitAll(): List<AnimeCategory>
    fun subscribeAll(): Flow<List<AnimeCategory>>
    suspend fun awaitByAnimeId(animeId: Long): List<AnimeCategory>
    suspend fun insert(name: String, flags: Long = 0): Long
    suspend fun update(category: AnimeCategory)
    suspend fun delete(id: Long)
    suspend fun reorder(ids: List<Long>)
    suspend fun setForAnime(animeId: Long, categoryIds: Set<Long>)
}
