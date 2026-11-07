package tachiyomi.data.anime

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import tachiyomi.data.Database
import tachiyomi.data.subscribeToList
import tachiyomi.domain.history.anime.model.AnimeHistory
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.domain.history.anime.repository.AnimeHistoryRepository
import java.util.Date

class AnimeHistoryRepositoryImpl(database: Database) : AnimeHistoryRepository {
    private val queries = database.anime_historyQueries

    override suspend fun awaitByEpisodeId(episodeId: Long): AnimeHistory? =
        queries.getByEpisodeId(episodeId) { _, id, lastSeen, timeWatched ->
            AnimeHistory(id, lastSeen, timeWatched)
        }.awaitAsOneOrNull()

    override fun subscribeContinueWatching() =
        queries.getContinueWatching { animeId, episodeId, lastSeen ->
            AnimeHistoryWithRelations(animeId, episodeId, checkNotNull(lastSeen))
        }.subscribeToList()

    override suspend fun upsert(episodeId: Long, lastSeen: Date?, watchSeconds: Long) {
        require(watchSeconds >= 0)
        queries.upsert(episodeId, lastSeen, watchSeconds)
    }
}
