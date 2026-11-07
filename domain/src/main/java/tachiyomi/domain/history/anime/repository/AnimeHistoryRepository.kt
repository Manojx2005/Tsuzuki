package tachiyomi.domain.history.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.history.anime.model.AnimeHistory
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import java.util.Date

interface AnimeHistoryRepository {
    suspend fun awaitByEpisodeId(episodeId: Long): AnimeHistory?
    fun subscribeContinueWatching(): Flow<List<AnimeHistoryWithRelations>>
    suspend fun upsert(episodeId: Long, lastSeen: Date?, watchSeconds: Long)
}
