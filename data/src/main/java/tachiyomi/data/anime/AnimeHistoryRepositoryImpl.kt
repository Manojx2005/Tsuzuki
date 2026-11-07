package tachiyomi.data.anime

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import tachiyomi.data.Database
import tachiyomi.data.subscribeToList
import tachiyomi.domain.history.anime.model.AnimeHistory
import tachiyomi.domain.history.anime.model.AnimeHistoryDisplay
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.domain.history.anime.repository.AnimeHistoryRepository
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

    override suspend fun removeHistory(animeId: Long, episodeId: Long?) = database.transaction {
        queries.clearHistory(animeId, episodeId)
        queries.refreshAnimeLastSeen(animeId)
    }

    override suspend fun upsert(episodeId: Long, lastSeen: Date?, watchSeconds: Long) {
        require(watchSeconds >= 0)
        queries.upsert(episodeId, lastSeen, watchSeconds)
    }
}
