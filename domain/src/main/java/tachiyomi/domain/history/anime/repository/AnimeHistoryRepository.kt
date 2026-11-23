package tachiyomi.domain.history.anime.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

    /** Watch seconds per day from [start] through [end], inclusive. */
    fun subscribeWatchDays(start: LocalDate, end: LocalDate): Flow<Map<LocalDate, Long>> = flowOf(emptyMap())
    suspend fun recordWatchDay(episodeId: Long, day: LocalDate, watchSeconds: Long)
    suspend fun removeHistory(animeId: Long, episodeId: Long? = null)

    /** Clears every watched-episode entry (watch time for the stats is kept), as History's "clear all" does. */
    suspend fun removeAllHistory()
    suspend fun upsert(episodeId: Long, lastSeen: Date?, watchSeconds: Long)
}
