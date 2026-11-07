package tachiyomi.data.anime

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.flow.map
import tachiyomi.data.Database
import tachiyomi.data.Episodes
import tachiyomi.data.subscribeToList
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.repository.EpisodeRepository

class EpisodeRepositoryImpl(private val database: Database) : EpisodeRepository {
    private val queries get() = database.episodesQueries

    override suspend fun awaitById(id: Long): Episode? =
        queries.getById(id).awaitAsOneOrNull()?.toDomain()

    override suspend fun awaitByAnimeId(animeId: Long): List<Episode> =
        queries.getByAnimeId(animeId).awaitAsList().map(Episodes::toDomain)

    override fun subscribeByAnimeId(animeId: Long) =
        queries.getByAnimeId(animeId).subscribeToList().map { rows -> rows.map(Episodes::toDomain) }

    override suspend fun insertAll(episodes: List<Episode>): List<Long> = database.transactionWithResult {
        episodes.map { episode ->
            queries.insert(episode.toRow())
            queries.lastInsertedId().awaitAsOne()
        }
    }

    override suspend fun updateProgress(id: Long, seen: Boolean, lastSecondSeen: Long, totalSeconds: Long) {
        require(lastSecondSeen >= 0 && totalSeconds >= 0)
        queries.updateProgress(seen = seen, lastSecondSeen = lastSecondSeen, totalSeconds = totalSeconds, id = id)
    }

    override suspend fun delete(id: Long) {
        queries.delete(id)
    }
}

private fun Episodes.toDomain() = Episode(
    id = _id,
    animeId = anime_id,
    url = url,
    name = name,
    scanlator = scanlator,
    seen = seen,
    bookmark = bookmark,
    fillermark = fillermark,
    lastSecondSeen = last_second_seen,
    totalSeconds = total_seconds,
    episodeNumber = episode_number,
    sourceOrder = source_order,
    dateFetch = date_fetch,
    dateUpload = date_upload,
    summary = summary,
    previewUrl = preview_url,
    lastModifiedAt = last_modified_at,
    version = version,
    isSyncing = is_syncing,
    memo = memo,
)

private fun Episode.toRow() = Episodes(
    _id = id,
    anime_id = animeId,
    url = url,
    name = name,
    scanlator = scanlator,
    seen = seen,
    bookmark = bookmark,
    fillermark = fillermark,
    last_second_seen = lastSecondSeen,
    total_seconds = totalSeconds,
    episode_number = episodeNumber,
    source_order = sourceOrder,
    date_fetch = dateFetch,
    date_upload = dateUpload,
    summary = summary,
    preview_url = previewUrl,
    last_modified_at = lastModifiedAt,
    version = version,
    is_syncing = isSyncing,
    memo = memo,
)
