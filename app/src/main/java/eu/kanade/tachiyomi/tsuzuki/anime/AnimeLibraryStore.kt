package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import tachiyomi.data.Database
import tachiyomi.domain.category.anime.model.AnimeCategory
import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.repository.AnimeRepository
import tachiyomi.domain.history.anime.repository.AnimeHistoryRepository
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.repository.EpisodeRepository
import java.util.Date

internal data class AnimeLibraryEntry(val anime: Anime, val shelfIds: Set<Long>)

internal data class ContinueAnime(val anime: Anime, val episode: Episode)

internal class AnimeLibraryStore(
    private val database: Database,
    private val anime: AnimeRepository,
    private val shelves: AnimeCategoryRepository,
    private val episodes: EpisodeRepository,
    private val history: AnimeHistoryRepository,
) {
    fun observeShelves() = shelves.subscribeAll()

    fun observeLibrary() = combine(anime.subscribeLibrary(), shelves.subscribeMemberships()) { items, memberships ->
        items.map { (entry) -> AnimeLibraryEntry(entry, memberships[entry.id].orEmpty().ifEmpty { setOf(0L) }) }
    }

    fun observeContinueWatching() = history.subscribeContinueWatching().map { entries ->
        entries.firstNotNullOfOrNull { entry ->
            val show = anime.awaitById(entry.animeId) ?: return@firstNotNullOfOrNull null
            val last = episodes.awaitById(entry.episodeId) ?: return@firstNotNullOfOrNull null
            if (!last.seen) return@firstNotNullOfOrNull ContinueAnime(show, last)
            val ordered = episodes.awaitByAnimeId(show.id).sortedWith(
                compareBy<Episode> { it.episodeNumber.takeIf { number -> number >= 0 } ?: Double.POSITIVE_INFINITY }
                    .thenBy(Episode::sourceOrder),
            )
            val next = ordered.dropWhile { it.id != last.id }.drop(1).firstOrNull { !it.seen }
            next?.let { ContinueAnime(show, it) }
        }
    }

    suspend fun getAnime(id: Long) = anime.awaitById(id)
    suspend fun getAnime(source: Long, url: String) = anime.awaitBySourceAndUrl(source, url)
    suspend fun getEpisode(id: Long) = episodes.awaitById(id)
    suspend fun getShelves() = shelves.awaitAll()
    suspend fun shelfIds(animeId: Long) = shelves.awaitByAnimeId(animeId).mapTo(mutableSetOf(), AnimeCategory::id)

    suspend fun saveShelf(category: AnimeCategory?, name: String) = database.transaction {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        require(shelves.awaitAll().none { it.id != category?.id && it.name.equals(trimmed, ignoreCase = true) })
        if (category == null) shelves.insert(trimmed) else shelves.update(category.copy(name = trimmed))
    }

    suspend fun deleteShelf(id: Long) = shelves.delete(id)
    suspend fun reorderShelves(ids: List<Long>) = shelves.reorder(ids)

    suspend fun setFavorite(source: Long, show: SAnime, favorite: Boolean, shelfIds: Set<Long>): Long =
        database.transactionWithResult {
            val entry = saveAnime(source, show, favorite)
            if (favorite) shelves.setForAnime(entry.id, shelfIds)
            entry.id
        }

    suspend fun saveEpisodes(source: Long, show: SAnime, available: List<SEpisode>): Map<String, Episode> =
        database.transactionWithResult {
            val entry = saveAnime(source, show)
            val existing = episodes.awaitByAnimeId(entry.id).associateBy(Episode::url)
            val additions = available.distinctBy(SEpisode::url).mapIndexedNotNull { index, episode ->
                if (episode.url in existing) return@mapIndexedNotNull null
                Episode(
                    animeId = entry.id,
                    url = episode.url,
                    name = episode.name,
                    scanlator = episode.scanlator,
                    fillermark = episode.fillermark,
                    episodeNumber = episode.episode_number.toDouble(),
                    sourceOrder = index.toLong(),
                    dateFetch = System.currentTimeMillis(),
                    dateUpload = episode.date_upload,
                    summary = episode.summary,
                    previewUrl = episode.preview_url,
                    memo = episode.memo,
                )
            }
            val ids = episodes.insertAll(additions)
            existing + additions.zip(ids).associate { (episode, id) -> episode.url to episode.copy(id = id) }
        }

    suspend fun saveProgress(episodeId: Long, progress: AnimePlaybackCheckpoint) = database.transaction {
        val episode = episodes.awaitById(episodeId) ?: return@transaction
        val duration = progress.duration.takeIf { it > 0.0 } ?: episode.totalSeconds.toDouble()
        episodes.updateProgress(
            episode.id,
            episode.seen || animeEpisodeSeen(progress.position, duration),
            progress.position.toLong(),
            duration.toLong(),
        )
        history.upsert(episode.id, Date(), progress.watchSeconds)
    }

    private suspend fun saveAnime(source: Long, show: SAnime, favorite: Boolean? = null): Anime {
        val stored = anime.awaitBySourceAndUrl(source, show.url)
            ?: Anime(source = source, url = show.url, title = show.title)
        val updated = stored.copy(
            title = show.title,
            artist = show.artist ?: stored.artist,
            author = show.author ?: stored.author,
            description = show.description ?: stored.description,
            genre = show.getGenres() ?: stored.genre,
            status = show.status.toLong().takeUnless { it == 0L } ?: stored.status,
            thumbnailUrl = show.thumbnail_url ?: stored.thumbnailUrl,
            backgroundUrl = show.background_url ?: stored.backgroundUrl,
            favorite = favorite ?: stored.favorite,
            dateAdded = if (favorite == true && !stored.favorite) System.currentTimeMillis() else stored.dateAdded,
            memo = JsonObject(stored.memo + show.memo),
        )
        val id = if (stored.id == -1L) anime.insert(updated) else stored.id
        val entry = updated.copy(id = id)
        if (entry != stored) anime.update(entry)
        return entry
    }
}

internal fun Anime.toSourceAnime(): SAnime = SAnime.create().also { source ->
    source.url = url
    source.title = title
    source.artist = artist
    source.author = author
    source.description = description
    source.genre = genre?.joinToString(", ")
    source.status = status.toInt()
    source.thumbnail_url = thumbnailUrl
    source.background_url = backgroundUrl
    source.update_strategy = updateStrategy
    source.fetch_type = fetchType
    source.season_number = seasonNumber
    source.initialized = initialized
    source.memo = memo
}
