package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeEpisodeUpdate
import eu.kanade.tachiyomi.animesource.model.SAnimeSeasonUpdate
import eu.kanade.tachiyomi.animesource.model.SEpisode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import tachiyomi.data.Database
import tachiyomi.data.subscribeToList
import tachiyomi.data.subscribeToOneOrNull
import tachiyomi.domain.category.anime.model.AnimeCategory
import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.repository.AnimeRepository
import tachiyomi.domain.history.anime.repository.AnimeHistoryRepository
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.repository.EpisodeRepository
import tachiyomi.domain.track.anime.repository.AnimeTrackRepository
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale

internal data class AnimeLibraryEntry(val anime: Anime, val shelfIds: Set<Long>)

internal data class ContinueAnime(val anime: Anime, val episode: Episode)

internal data class AnimeEpisodes(
    val items: List<Episode>,
    val resume: Episode?,
    val groups: List<String> = emptyList(),
)

internal class AnimeLibraryStore(
    private val database: Database,
    private val anime: AnimeRepository,
    private val shelves: AnimeCategoryRepository,
    private val episodes: EpisodeRepository,
    private val history: AnimeHistoryRepository,
) {
    private val refreshLock = Mutex()

    fun observeHasFavorites() = anime.subscribeLibrary().map { it.isNotEmpty() }

    suspend fun getLibrary() = anime.subscribeLibrary().first().map { it.anime }

    fun observeNewEpisodes(since: Long) = database.episodesQueries.getLibraryUpdates(since) {
            animeId,
            title,
            episodeId,
            name,
            previewUrl,
            dateFetch,
            memo,
            scanlator,
            animeMemo,
        ->
        val visible = memo[UPDATE_MEMO_KEY] == JsonPrimitive(true) &&
            !scanlator.isExcludedAnimeGroup(animeMemo.excludedAnimeGroups())
        AnimeUpdateEpisode(animeId, title, episodeId, name, previewUrl, dateFetch) to visible
    }.subscribeToList().map { rows ->
        rows.mapNotNull { (entry, visible) -> entry.takeIf { visible } }
    }

    fun observeAiringEpisodes(start: Long, end: Long) =
        database.episodesQueries.getLibraryAiring(start, end) {
                animeId,
                title,
                episodeId,
                name,
                previewUrl,
                dateUpload,
                scanlator,
                animeMemo,
            ->
            val visible = !scanlator.isExcludedAnimeGroup(animeMemo.excludedAnimeGroups())
            AnimeUpdateEpisode(animeId, title, episodeId, name, previewUrl, dateUpload) to visible
        }.subscribeToList().map { rows ->
            rows.mapNotNull { (entry, visible) -> entry.takeIf { visible } }
        }

    fun observeShelves() = shelves.subscribeAll()

    fun observeSeasons(parentId: Long) = anime.subscribeSeasons(parentId)

    fun observeLibrary() = combine(anime.subscribeLibrary(), shelves.subscribeMemberships()) { items, memberships ->
        items.map { (entry) -> AnimeLibraryEntry(entry, memberships[entry.id].orEmpty().ifEmpty { setOf(0L) }) }
    }

    fun observeContinueWatching() = history.subscribeContinueWatching().map { entries ->
        entries.firstNotNullOfOrNull { entry ->
            val show = anime.awaitById(entry.animeId) ?: return@firstNotNullOfOrNull null
            val last = episodes.awaitById(entry.episodeId) ?: return@firstNotNullOfOrNull null
            val excluded = show.memo.excludedAnimeGroups()
            if (!last.seen && !last.scanlator.isExcludedAnimeGroup(excluded)) {
                return@firstNotNullOfOrNull ContinueAnime(show, last)
            }
            val ordered = episodes.awaitByAnimeId(show.id).sortedWith(animeEpisodeNumberOrder)
            val next = ordered.dropWhile { it.id != last.id }.drop(1)
                .firstOrNull { !it.seen && !it.scanlator.isExcludedAnimeGroup(excluded) }
            next?.let { ContinueAnime(show, it) }
        }
    }

    suspend fun getAnime(id: Long) = anime.awaitById(id)
    suspend fun getAnime(source: Long, url: String) = anime.awaitBySourceAndUrl(source, url)
    suspend fun cacheSearchResult(source: Long, result: SAnime): Long = database.transactionWithResult {
        (anime.awaitBySourceAndUrl(source, result.url) ?: saveAnime(source, result)).id
    }

    suspend fun getEpisode(id: Long) = episodes.awaitById(id)
    suspend fun getEpisodes(animeId: Long) = episodes.awaitByAnimeId(animeId)

    fun observeExcludedGroups(animeId: Long) = database.animesQueries.getById(animeId)
        .subscribeToOneOrNull().map { it?.memo?.excludedAnimeGroups().orEmpty() }.distinctUntilChanged()

    fun observeEpisodes(animeId: Long) =
        combine(episodes.subscribeByAnimeId(animeId), observeExcludedGroups(animeId)) { items, excluded ->
            val visible = items.filterAnimeGroups(excluded)
            val groups = (excluded + items.mapNotNull { it.scanlator?.trim()?.takeIf(String::isNotEmpty) })
                .distinctBy { it.lowercase(Locale.ROOT) }
                .sortedWith(String.CASE_INSENSITIVE_ORDER)
            AnimeEpisodes(visible, chooseAnimeEpisode(visible, history.latestInProgressEpisodeId(animeId)), groups)
        }

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

    suspend fun migrateFavorite(
        originalId: Long,
        targetId: Long,
        mode: AnimeMigrationMode,
        tracks: AnimeTrackRepository,
    ) = database.transaction {
        val original = checkNotNull(anime.awaitById(originalId))
        val target = checkNotNull(anime.awaitById(targetId))
        require(original.favorite && original.source != target.source)
        for ((old, new) in matchAnimeEpisodes(getEpisodes(originalId), getEpisodes(targetId))) {
            val updated = new.withMigratedProgress(old)
            if (updated != new) {
                episodes.updateProgress(updated.id, updated.seen, updated.lastSecondSeen, updated.totalSeconds)
            }
        }
        val existingTrackers = tracks.awaitByAnimeId(targetId).mapTo(mutableSetOf()) { it.syncId }
        tracks.awaitByAnimeId(originalId).filterNot { it.syncId in existingTrackers }.forEach {
            tracks.upsert(it.copy(id = -1, animeId = targetId))
        }
        shelves.setForAnime(targetId, shelfIds(targetId) + shelfIds(originalId))
        val added = when {
            target.favorite -> target.dateAdded
            mode == AnimeMigrationMode.REPLACE -> original.dateAdded
            else -> System.currentTimeMillis()
        }
        anime.update(
            target.copy(
                favorite = true,
                dateAdded = added,
                memo = target.memo.withExcludedAnimeGroups(
                    target.memo.excludedAnimeGroups() + original.memo.excludedAnimeGroups(),
                ),
            ),
        )
        if (mode == AnimeMigrationMode.REPLACE) anime.setFavorite(originalId, false, original.dateAdded)
    }

    suspend fun refresh(
        source: RegisteredAnimeSource,
        show: SAnime,
        fetchDetails: Boolean,
        favoriteOnly: Boolean = false,
    ): Anime? = refreshLock.withLock {
        val cached = getAnime(source.source.id, show.url)
        if (favoriteOnly && cached?.favorite != true) return@withLock null
        val current = cached?.toSourceAnime() ?: show
        // A source can declare the fetch type in its first details response.
        val discoverType = source.libVersion >= 16 && fetchDetails &&
            !current.initialized && current.fetch_type == FetchType.Episodes
        val detailed = if (discoverType) {
            withContext(Dispatchers.IO) {
                source.fetchEpisodeUpdate(current, emptyList(), fetchDetails = true, fetchEpisodes = false).anime
            }
        } else {
            current
        }
        if (source.libVersion >= 16 && detailed.fetch_type == FetchType.Seasons) {
            val existing = cached?.let { observeSeasons(it.id).first() }.orEmpty()
            val update = withContext(Dispatchers.IO) {
                source.fetchSeasonUpdate(detailed, existing.map(Anime::toSourceAnime), fetchDetails && !discoverType)
            }
            return@withLock syncSeasons(source.source.id, update.anime, update.seasons)
        }

        val existing = cached?.let { getEpisodes(it.id) }.orEmpty()
        val update = withContext(Dispatchers.IO) {
            source.fetchEpisodeUpdate(detailed, existing.map(Episode::toSourceEpisode), fetchDetails && !discoverType)
        }
        syncEpisodes(source.source.id, update.anime, update.episodes)
    }

    private suspend fun syncSeasons(source: Long, show: SAnime, available: List<SAnime>): Anime =
        database.transactionWithResult {
            val parent = saveAnime(source, show)
            val ancestors = mutableSetOf(parent.id)
            var ancestor = parent.parentId
            while (ancestor != null && ancestors.add(ancestor)) {
                ancestor = anime.awaitById(ancestor)?.parentId
            }
            available.distinctBy(SAnime::url).forEachIndexed { index, season ->
                if (season.url.isBlank() || season.url == parent.url) return@forEachIndexed
                val entry = saveAnime(source, season)
                if (entry.id in ancestors || (entry.parentId != null && entry.parentId != parent.id)) {
                    return@forEachIndexed
                }
                val linked = entry.copy(parentId = parent.id, seasonSourceOrder = index.toLong())
                if (linked != entry) anime.update(linked)
            }
            val now = System.currentTimeMillis()
            database.animesQueries.recordLibraryRefresh(now, parent.id)
            parent.copy(lastUpdate = now)
        }

    suspend fun syncEpisodes(
        source: Long,
        show: SAnime,
        available: List<SEpisode>,
        downloadedEpisodeIds: Set<Long> = emptySet(),
    ): Anime = database.transactionWithResult {
        val entry = saveAnime(source, show)
        val existing = episodes.awaitByAnimeId(entry.id).associateBy(Episode::url)
        val now = System.currentTimeMillis()
        val hasBaseline = entry.lastUpdate != null || existing.isNotEmpty()
        val incoming = available.distinctBy(SEpisode::url)
        val urls = incoming.mapTo(hashSetOf(), SEpisode::url)
        val additions = ArrayList<Episode>()
        incoming.forEachIndexed { index, episode ->
            val old = existing[episode.url]
            val updated = (old ?: Episode(entry.id, episode.url, episode.name, dateFetch = now)).copy(
                name = episode.name,
                scanlator = episode.scanlator,
                fillermark = episode.fillermark,
                episodeNumber = episode.episode_number.toDouble(),
                sourceOrder = index.toLong(),
                dateUpload = episode.date_upload,
                summary = episode.summary,
                previewUrl = episode.preview_url,
                memo = animeEpisodeMemo(
                    episode.memo,
                    old?.memo,
                    isNewUpdate = old == null && entry.favorite && hasBaseline,
                ),
            )
            if (old == null) {
                additions += updated
            } else if (old != updated) {
                episodes.updateFromSource(updated)
            }
        }
        episodes.insertAll(additions)
        existing.values.filter {
            it.url !in urls && it.id !in downloadedEpisodeIds && !it.seen && !it.bookmark && it.lastSecondSeen == 0L
        }.forEach { episodes.delete(it.id) }
        database.animesQueries.recordLibraryRefresh(now, entry.id)
        entry.copy(lastUpdate = now)
    }

    suspend fun setEpisodeDisplay(animeId: Long, display: AnimeEpisodeDisplay, excludedGroups: Set<String>? = null) =
        database.transaction {
            val show = anime.awaitById(animeId) ?: return@transaction
            anime.update(
                show.copy(
                    episodeFlags = display.toFlags(show.episodeFlags),
                    memo = excludedGroups?.let(show.memo::withExcludedAnimeGroups) ?: show.memo,
                ),
            )
        }

    suspend fun setEpisodesSeen(ids: Set<Long>, seen: Boolean): List<Episode> = database.transactionWithResult {
        val newlySeen = mutableListOf<Episode>()
        ids.forEach { id ->
            val episode = episodes.awaitById(id) ?: return@forEach
            val position = if (seen) episode.lastSecondSeen else 0
            if (episode.seen != seen || episode.lastSecondSeen != position) {
                episodes.updateProgress(id, seen, position, episode.totalSeconds)
            }
            if (seen && !episode.seen) newlySeen += episode
        }
        newlySeen
    }

    suspend fun setEpisodesBookmarked(ids: Set<Long>, bookmarked: Boolean) = database.transaction {
        ids.forEach { episodes.setBookmark(it, bookmarked) }
    }

    suspend fun saveProgress(episodeId: Long, progress: AnimePlaybackCheckpoint): Episode? =
        database.transactionWithResult {
            val episode = episodes.awaitById(episodeId) ?: return@transactionWithResult null
            val duration = progress.duration.takeIf { it > 0.0 } ?: episode.totalSeconds.toDouble()
            val seen = episode.seen || animeEpisodeSeen(progress.position, duration)
            episodes.updateProgress(episode.id, seen, progress.position.toLong(), duration.toLong())
            val now = Instant.now()
            history.upsert(episode.id, Date.from(now), progress.watchSeconds)
            history.recordWatchDay(episode.id, now.atZone(ZoneId.systemDefault()).toLocalDate(), progress.watchSeconds)
            episode.takeIf { seen && !episode.seen }
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
            initialized = show.initialized || stored.initialized,
            updateStrategy = if (show.initialized) show.update_strategy else stored.updateStrategy,
            fetchType = if (show.initialized || (!stored.initialized && show.fetch_type == FetchType.Seasons)) {
                show.fetch_type
            } else {
                stored.fetchType
            },
            seasonNumber = show.season_number.takeIf { it.isFinite() && it >= 0 } ?: stored.seasonNumber,
            favorite = favorite ?: stored.favorite,
            dateAdded = if (favorite == true && !stored.favorite) System.currentTimeMillis() else stored.dateAdded,
            memo = JsonObject(stored.memo + (show.memo - ANIME_EXCLUDED_GROUPS)),
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
    source.memo = JsonObject(memo - ANIME_EXCLUDED_GROUPS)
}

internal fun Episode.toSourceEpisode(): SEpisode = SEpisode.create().also { source ->
    source.url = url
    source.name = name
    source.scanlator = scanlator
    source.fillermark = fillermark
    source.episode_number = episodeNumber.toFloat()
    source.date_upload = dateUpload
    source.summary = summary
    source.preview_url = previewUrl
    source.memo = JsonObject(memo - UPDATE_MEMO_KEY)
}

@Suppress("DEPRECATION")
private suspend fun RegisteredAnimeSource.fetchEpisodeUpdate(
    show: SAnime,
    episodes: List<SEpisode>,
    fetchDetails: Boolean,
    fetchEpisodes: Boolean = true,
): SAnimeEpisodeUpdate = callAnimeSource {
    val update = if (libVersion >= 17) {
        source.getAnimeEpisodeUpdate(show, episodes, fetchDetails = fetchDetails, fetchEpisodes = fetchEpisodes)
    } else {
        SAnimeEpisodeUpdate(
            if (fetchDetails) source.getAnimeDetails(show) else show,
            if (fetchEpisodes) AnimeStreamResolver.episodes(source, libVersion, show) else episodes,
        )
    }
    SAnimeEpisodeUpdate(if (fetchDetails) show.withDetails(update.anime, libVersion >= 17) else show, update.episodes)
}

@Suppress("DEPRECATION")
private suspend fun RegisteredAnimeSource.fetchSeasonUpdate(
    show: SAnime,
    seasons: List<SAnime>,
    fetchDetails: Boolean,
): SAnimeSeasonUpdate = callAnimeSource {
    if (libVersion < 17) {
        val detailed = if (fetchDetails) show.withDetails(source.getAnimeDetails(show), copyMemo = false) else show
        return@callAnimeSource SAnimeSeasonUpdate(detailed, source.getSeasonList(detailed))
    }
    val update = source.getAnimeSeasonUpdate(show, seasons, fetchDetails = fetchDetails, fetchSeasons = true)
    SAnimeSeasonUpdate(if (fetchDetails) show.withDetails(update.anime, copyMemo = true) else show, update.seasons)
}

private fun SAnime.withDetails(remote: SAnime, copyMemo: Boolean): SAnime =
    copy().apply {
        title = runCatching { remote.title }.getOrNull()?.takeIf(String::isNotBlank) ?: title
        artist = remote.artist ?: artist
        author = remote.author ?: author
        status = remote.status.takeUnless { it == SAnime.UNKNOWN } ?: status
        thumbnail_url = remote.thumbnail_url ?: thumbnail_url
        background_url = remote.background_url ?: background_url
        description = remote.description ?: description
        genre = remote.genre ?: genre
        if (!initialized && fetch_type != FetchType.Seasons) fetch_type = remote.fetch_type
        initialized = true
        update_strategy = remote.update_strategy
        if (remote.season_number.isFinite() && remote.season_number >= 0) season_number = remote.season_number
        if (copyMemo) memo = remote.memo
    }

internal data class AnimeUpdateEpisode(
    val animeId: Long,
    val title: String,
    val episodeId: Long,
    val name: String,
    val previewUrl: String?,
    val timestamp: Long,
)

private const val UPDATE_MEMO_KEY = "tsuzuki:library-update"

internal fun animeEpisodeMemo(remote: JsonObject, stored: JsonObject?, isNewUpdate: Boolean): JsonObject {
    val sourceMemo = remote - UPDATE_MEMO_KEY
    val marked = isNewUpdate || stored?.get(UPDATE_MEMO_KEY) == JsonPrimitive(true)
    return JsonObject(if (marked) sourceMemo + (UPDATE_MEMO_KEY to JsonPrimitive(true)) else sourceMemo)
}
