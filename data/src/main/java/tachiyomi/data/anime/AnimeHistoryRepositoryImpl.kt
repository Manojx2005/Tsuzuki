package tachiyomi.data.anime

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import eu.kanade.tachiyomi.animesource.model.SAnime
import kotlinx.coroutines.flow.map
import tachiyomi.data.Database
import tachiyomi.data.subscribeToList
import tachiyomi.data.subscribeToOne
import tachiyomi.domain.history.anime.model.AnimeHistory
import tachiyomi.domain.history.anime.model.AnimeHistoryDisplay
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.domain.history.anime.model.AnimeWatchStats
import tachiyomi.domain.history.anime.repository.AnimeHistoryRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Date

class AnimeHistoryRepositoryImpl(private val database: Database) : AnimeHistoryRepository {
    private val queries = database.anime_historyQueries

    override suspend fun awaitByEpisodeId(episodeId: Long): AnimeHistory? =
        queries.getByEpisodeId(episodeId) { _, id, lastSeen, timeWatched ->
            AnimeHistory(id, lastSeen, timeWatched)
        }.awaitAsOneOrNull()

    override fun subscribeContinueWatching() =
        queries.getContinueWatching { animeId, episodeId, lastSeen ->
            AnimeHistoryWithRelations(animeId, episodeId, checkNotNull(lastSeen))
        }.subscribeToList()

    override suspend fun latestInProgressEpisodeId(animeId: Long): Long? =
        queries.getLatestInProgress(animeId).awaitAsOneOrNull()

    override fun subscribeHistory() = queries.getHistory {
            animeId,
            title,
            thumbnailUrl,
            episodeId,
            name,
            number,
            previewUrl,
            seen,
            position,
            duration,
            lastSeen,
        ->
        AnimeHistoryDisplay(
            animeId,
            title,
            thumbnailUrl,
            episodeId,
            name,
            number,
            previewUrl,
            seen,
            position,
            duration,
            checkNotNull(lastSeen),
        )
    }.subscribeToList()

    override fun subscribeWatchStats(today: LocalDate) = database.anime_watch_daysQueries.getWatchStats(
        weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay(),
        today = today.toEpochDay(),
        monthStart = today.withDayOfMonth(1).toEpochDay(),
        completedStatus = SAnime.COMPLETED.toLong(),
        mapper = ::AnimeWatchStats,
    ).subscribeToOne()

    override fun subscribeWatchDays(start: LocalDate, end: LocalDate) =
        database.anime_watch_daysQueries.getWatchDays(start.toEpochDay(), end.toEpochDay()) { day, seconds ->
            day to (seconds ?: 0L)
        }
            .subscribeToList()
            .map { rows -> rows.associate { (day, seconds) -> LocalDate.ofEpochDay(day) to seconds } }

    override suspend fun recordWatchDay(episodeId: Long, day: LocalDate, watchSeconds: Long) {
        if (watchSeconds > 0) {
            database.anime_watch_daysQueries.recordWatchDay(episodeId, day.toEpochDay(), watchSeconds)
        }
    }

    override suspend fun removeHistory(animeId: Long, episodeId: Long?) = database.transaction {
        queries.clearHistory(animeId, episodeId)
        queries.refreshAnimeLastSeen(animeId)
    }

    override suspend fun removeAllHistory() = database.transaction {
        queries.clearAllHistory()
        queries.refreshAllAnimeLastSeen()
    }

    override suspend fun upsert(episodeId: Long, lastSeen: Date?, watchSeconds: Long) {
        require(watchSeconds >= 0)
        queries.upsert(episodeId, lastSeen, watchSeconds)
    }
}
