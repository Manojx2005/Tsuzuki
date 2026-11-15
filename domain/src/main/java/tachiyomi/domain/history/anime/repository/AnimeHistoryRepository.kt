package tachiyomi.domain.history.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.history.anime.model.AnimeHistory
import tachiyomi.domain.history.anime.model.AnimeHistoryDisplay
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.domain.history.anime.model.AnimeWatchStats
import java.time.LocalDate
import java.util.Date

interface AnimeHistoryRepository {
    suspend fun awaitByEpisodeId(episodeId: Long): AnimeHistory?
    suspend fun latestInProgressEpisodeId(animeId: Long): Long?
    fun subscribeContinueWatching(): Flow<List<AnimeHistoryWithRelations>>
    fun subscribeHistory(): Flow<List<AnimeHistoryDisplay>>
    fun subscribeWatchStats(today: LocalDate): Flow<AnimeWatchStats>
    suspend fun recordWatchDay(episodeId: Long, day: LocalDate, watchSeconds: Long)
    suspend fun removeHistory(animeId: Long, episodeId: Long? = null)
    suspend fun upsert(episodeId: Long, lastSeen: Date?, watchSeconds: Long)
}
