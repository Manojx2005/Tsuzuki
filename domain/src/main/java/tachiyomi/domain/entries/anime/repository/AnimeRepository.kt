package tachiyomi.domain.entries.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.library.anime.model.LibraryAnime

interface AnimeRepository {
    suspend fun awaitById(id: Long): Anime?
    suspend fun awaitBySourceAndUrl(source: Long, url: String): Anime?
    fun subscribeLibrary(): Flow<List<LibraryAnime>>
    suspend fun insert(anime: Anime): Long
    suspend fun update(anime: Anime)
    suspend fun setFavorite(id: Long, favorite: Boolean, dateAdded: Long)
    suspend fun delete(id: Long)
}
