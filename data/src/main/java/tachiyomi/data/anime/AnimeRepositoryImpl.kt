package tachiyomi.data.anime

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.flow.map
import tachiyomi.data.Animes
import tachiyomi.data.Database
import tachiyomi.data.subscribeToList
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.repository.AnimeRepository
import tachiyomi.domain.library.anime.model.LibraryAnime

class AnimeRepositoryImpl(private val database: Database) : AnimeRepository {
    private val queries get() = database.animesQueries

    override suspend fun awaitById(id: Long): Anime? =
        queries.getById(id).awaitAsOneOrNull()?.toDomain()

    override suspend fun awaitBySourceAndUrl(source: Long, url: String): Anime? =
        queries.getBySourceAndUrl(source, url).awaitAsOneOrNull()?.toDomain()

    override fun subscribeLibrary() =
        queries.getLibrary().subscribeToList().map { rows -> rows.map { LibraryAnime(it.toDomain()) } }

    override suspend fun insert(anime: Anime): Long = database.transactionWithResult {
        val existing = queries.getBySourceAndUrl(anime.source, anime.url).awaitAsOneOrNull()
        if (existing != null) return@transactionWithResult existing._id

        queries.insert(anime.toRow())
        queries.lastInsertedId().awaitAsOne()
    }

    override suspend fun update(anime: Anime) {
        queries.update(
            id = anime.id,
            url = anime.url,
            title = anime.title,
            artist = anime.artist,
            author = anime.author,
            description = anime.description,
            genre = anime.genre,
            alternativeTitles = anime.alternativeTitles,
            status = anime.status,
            thumbnailUrl = anime.thumbnailUrl,
            backgroundUrl = anime.backgroundUrl,
            favorite = anime.favorite,
            lastUpdate = anime.lastUpdate,
            nextUpdate = anime.nextUpdate,
            initialized = anime.initialized,
            viewer = anime.viewer,
            episodeFlags = anime.episodeFlags,
            coverLastModified = anime.coverLastModified,
            backgroundLastModified = anime.backgroundLastModified,
            dateAdded = anime.dateAdded,
            updateStrategy = anime.updateStrategy,
            calculateInterval = anime.calculateInterval,
            fetchType = anime.fetchType,
            parentId = anime.parentId,
            seasonFlags = anime.seasonFlags,
            seasonNumber = anime.seasonNumber,
            seasonSourceOrder = anime.seasonSourceOrder,
            notes = anime.notes,
            memo = anime.memo,
        )
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean, dateAdded: Long) {
        queries.setFavorite(favorite = favorite, dateAdded = dateAdded, id = id)
    }

    override suspend fun delete(id: Long) {
        queries.delete(id)
    }
}

private fun Animes.toDomain() = Anime(
    id = _id,
    source = source,
    url = url,
    title = title,
    artist = artist,
    author = author,
    description = description,
    genre = genre,
    alternativeTitles = alternative_titles,
    status = status,
    thumbnailUrl = thumbnail_url,
    backgroundUrl = background_url,
    favorite = favorite,
    lastUpdate = last_update,
    nextUpdate = next_update,
    initialized = initialized,
    viewer = viewer,
    episodeFlags = episode_flags,
    coverLastModified = cover_last_modified,
    backgroundLastModified = background_last_modified,
    dateAdded = date_added,
    updateStrategy = update_strategy,
    calculateInterval = calculate_interval,
    fetchType = fetch_type,
    parentId = parent_id,
    seasonFlags = season_flags,
    seasonNumber = season_number,
    seasonSourceOrder = season_source_order,
    notes = notes,
    lastModifiedAt = last_modified_at,
    favoriteModifiedAt = favorite_modified_at,
    version = version,
    isSyncing = is_syncing,
    memo = memo,
    totalCount = total_count,
    seenCount = seen_count,
    bookmarkCount = bookmark_count,
    latestUpload = latest_upload,
    episodeFetchedAt = episode_fetched_at,
    lastSeen = last_seen,
)

private fun Anime.toRow() = Animes(
    _id = id,
    source = source,
    url = url,
    title = title,
    artist = artist,
    author = author,
    description = description,
    genre = genre,
    alternative_titles = alternativeTitles,
    status = status,
    thumbnail_url = thumbnailUrl,
    background_url = backgroundUrl,
    favorite = favorite,
    last_update = lastUpdate,
    next_update = nextUpdate,
    initialized = initialized,
    viewer = viewer,
    episode_flags = episodeFlags,
    cover_last_modified = coverLastModified,
    background_last_modified = backgroundLastModified,
    date_added = dateAdded,
    update_strategy = updateStrategy,
    calculate_interval = calculateInterval,
    fetch_type = fetchType,
    parent_id = parentId,
    season_flags = seasonFlags,
    season_number = seasonNumber,
    season_source_order = seasonSourceOrder,
    notes = notes,
    last_modified_at = lastModifiedAt,
    favorite_modified_at = favoriteModifiedAt,
    version = version,
    is_syncing = isSyncing,
    memo = memo,
    total_count = totalCount,
    seen_count = seenCount,
    bookmark_count = bookmarkCount,
    latest_upload = latestUpload,
    episode_fetched_at = episodeFetchedAt,
    last_seen = lastSeen,
)
