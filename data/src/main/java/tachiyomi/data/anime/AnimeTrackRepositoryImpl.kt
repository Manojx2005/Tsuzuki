package tachiyomi.data.anime

import app.cash.sqldelight.async.coroutines.awaitAsList
import tachiyomi.data.Anime_sync
import tachiyomi.data.Database
import tachiyomi.domain.track.anime.model.AnimeTrack
import tachiyomi.domain.track.anime.repository.AnimeTrackRepository

class AnimeTrackRepositoryImpl(database: Database) : AnimeTrackRepository {
    private val queries = database.anime_syncQueries

    override suspend fun awaitByAnimeId(animeId: Long): List<AnimeTrack> =
        queries.getByAnimeId(animeId).awaitAsList().map(Anime_sync::toDomain)

    override suspend fun upsert(track: AnimeTrack) {
        queries.upsert(track.toRow())
    }

    override suspend fun delete(animeId: Long, syncId: Long) {
        queries.delete(animeId, syncId)
    }
}

private fun Anime_sync.toDomain() = AnimeTrack(
    id = _id,
    animeId = anime_id,
    syncId = sync_id,
    remoteId = remote_id,
    libraryId = library_id,
    title = title,
    lastEpisodeSeen = last_episode_seen,
    totalEpisodes = total_episodes,
    status = status,
    score = score,
    remoteUrl = remote_url,
    startDate = start_date,
    finishDate = finish_date,
    isPrivate = private_,
)

private fun AnimeTrack.toRow() = Anime_sync(
    _id = id,
    anime_id = animeId,
    sync_id = syncId,
    remote_id = remoteId,
    library_id = libraryId,
    title = title,
    last_episode_seen = lastEpisodeSeen,
    total_episodes = totalEpisodes,
    status = status,
    score = score,
    remote_url = remoteUrl,
    start_date = startDate,
    finish_date = finishDate,
    private_ = isPrivate,
)
