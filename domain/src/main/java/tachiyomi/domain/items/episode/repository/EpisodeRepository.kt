package tachiyomi.domain.items.episode.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.items.episode.model.Episode

interface EpisodeRepository {
    suspend fun awaitById(id: Long): Episode?
    suspend fun awaitByAnimeId(animeId: Long): List<Episode>
    fun subscribeByAnimeId(animeId: Long): Flow<List<Episode>>
    suspend fun insertAll(episodes: List<Episode>): List<Long>
    suspend fun updateProgress(id: Long, seen: Boolean, lastSecondSeen: Long, totalSeconds: Long)
    suspend fun delete(id: Long)
}
