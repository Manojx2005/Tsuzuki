package eu.kanade.tachiyomi.tsuzuki.anime.backup

import app.cash.sqldelight.async.coroutines.awaitAsList
import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.yield
import kotlinx.serialization.protobuf.ProtoBuf
import tachiyomi.data.Database
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.anime.AnimeCategoryRepositoryImpl
import tachiyomi.data.anime.AnimeHistoryRepositoryImpl
import tachiyomi.data.anime.AnimeRepositoryImpl
import tachiyomi.data.anime.AnimeSourceRepositoryImpl
import tachiyomi.data.anime.AnimeTrackRepositoryImpl
import tachiyomi.data.anime.EpisodeRepositoryImpl
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.source.anime.model.StubAnimeSource
import java.util.Date
import java.util.Locale

internal class AnimeBackupStore(private val database: Database) {
    private val animes = AnimeRepositoryImpl(database)
    private val episodes = EpisodeRepositoryImpl(database)
    private val history = AnimeHistoryRepositoryImpl(database)
    private val categories = AnimeCategoryRepositoryImpl(database)
    private val tracks = AnimeTrackRepositoryImpl(database)
    private val sources = AnimeSourceRepositoryImpl(database)

    suspend fun restore(
        summary: AnimeBackupSummary,
        parser: ProtoBuf,
        includeEntries: Boolean,
        includeCategories: Boolean,
        read: suspend (suspend (Int, ByteArray) -> Unit) -> Unit,
        onEntry: (String?, Exception?) -> Unit,
    ) {
        val format = summary.format ?: return
        val categoryIds = if (includeCategories) restoreCategories(summary.shelves) else emptyMap()
        if (!includeEntries || summary.count == 0) return
        val sourceNames = summary.sourceNames.associateBy(AnimeBackupSource::id)
        val ids = mutableMapOf<Long, Long>()
        val parents = mutableMapOf<Long, Long>()
        read { field, bytes ->
            if (field == format.entries) {
                var entry: AnimeBackupEntry? = null
                val failure = try {
                    val decoded = parser.decodeFromByteArray(AnimeBackupEntry.serializer(), bytes)
                    entry = decoded
                    val id = restoreEntry(decoded, categoryIds, sourceNames[decoded.source], format)
                    decoded.oldId?.let { ids.putIfAbsent(it, id) }
                    decoded.parentId?.let { parents[id] = it }
                    null
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    error
                }
                onEntry(entry?.title, failure)
            }
        }
        restoreParents(ids, parents)
    }

    suspend fun export(options: BackupOptions, parser: ProtoBuf, write: (Int, ByteArray) -> Unit) {
        val fields = AnimeBackupFormat.TSUZUKI
        if (options.categories) {
            categories.awaitAll().filterNot { it.isSystem }.forEach { category ->
                val bytes = parser.encodeToByteArray(AnimeBackupCategory.serializer(), category.toAnimeBackup())
                write(fields.categories, bytes)
            }
        }
        if (!options.libraryEntries && !options.readEntries) return

        val writtenSources = mutableSetOf<Long>()
        var afterId = 0L
        while (true) {
            val ids = database.animesQueries.getBackupIds(afterId, 200).awaitAsList()
            if (ids.isEmpty()) break
            for (id in ids) {
                val anime = animes.awaitById(id) ?: continue
                if (anime.favorite && !options.libraryEntries) continue
                if (!anime.favorite && !options.readEntries) continue
                val entry = entry(anime, options)
                write(fields.entries, parser.encodeToByteArray(AnimeBackupEntry.serializer(), entry))
                if (writtenSources.add(anime.source)) {
                    val source = sources.awaitById(anime.source)
                    val name = AnimeBackupSource(source?.name.orEmpty(), anime.source, source?.lang.orEmpty())
                    write(fields.sources, parser.encodeToByteArray(AnimeBackupSource.serializer(), name))
                }
            }
            afterId = ids.last()
            yield()
        }
    }

    private suspend fun entry(anime: Anime, options: BackupOptions): AnimeBackupEntry {
        val storedEpisodes = if (options.chapters || options.history) episodes.awaitByAnimeId(anime.id) else emptyList()
        val episodeHistory = if (options.history) {
            storedEpisodes.mapNotNull { episode -> history.awaitByEpisodeId(episode.id)?.toAnimeBackup(episode.url) }
        } else {
            emptyList()
        }
        val historyUrls = episodeHistory.mapTo(hashSetOf(), AnimeBackupHistory::episodeUrl)
        val episodeBackup = storedEpisodes.mapNotNull { episode ->
            when {
                options.chapters -> episode.toAnimeBackup()
                episode.url in historyUrls -> AnimeBackupEpisode(episode.url, episode.name)
                else -> null
            }
        }
        return AnimeBackupEntry(
            source = anime.source,
            url = anime.url,
            title = anime.title,
            artist = anime.artist,
            author = anime.author,
            description = anime.description,
            genre = anime.genre.orEmpty(),
            status = anime.status,
            thumbnail = anime.thumbnailUrl,
            dateAdded = anime.dateAdded,
            episodes = episodeBackup,
            categories = if (options.categories) categories.awaitByAnimeId(anime.id).map { it.id } else emptyList(),
            tracks = if (options.tracking) tracks.awaitByAnimeId(anime.id).map { it.toAnimeBackup() } else emptyList(),
            favorite = anime.favorite,
            episodeFlags = anime.episodeFlags,
            viewer = anime.viewer,
            history = episodeHistory,
            updateStrategy = anime.updateStrategy,
            lastModifiedAt = anime.lastModifiedAt,
            favoriteModifiedAt = anime.favoriteModifiedAt,
            version = anime.version,
            memo = MemoColumnAdapter.encode(anime.memo),
            background = anime.backgroundUrl,
            parentId = anime.parentId,
            oldId = anime.id,
            seasonFlags = anime.seasonFlags,
            seasonNumber = anime.seasonNumber,
            seasonSourceOrder = anime.seasonSourceOrder,
            fetchType = anime.fetchType,
            notes = anime.notes,
            alternativeTitles = anime.alternativeTitles.orEmpty(),
            initialized = options.chapters && anime.initialized,
        )
    }

    suspend fun restoreCategories(shelves: List<AnimeBackupCategory>) = database.transactionWithResult {
        val local = categories.awaitAll().filterNot { it.isSystem }
            .associate { it.name.lowercase(Locale.ROOT) to it.id }.toMutableMap()
        buildMap {
            put(0L, 0L)
            for (shelf in shelves.sortedBy(AnimeBackupCategory::order)) {
                if (shelf.id <= 0 || shelf.name.isBlank()) continue
                val normalized = shelf.name.lowercase(Locale.ROOT)
                val id = local[normalized] ?: categories.insert(shelf.name, shelf.flags).also { local[normalized] = it }
                put(shelf.id, id)
            }
        }
    }

    suspend fun restoreEntry(
        entry: AnimeBackupEntry,
        categoryIds: Map<Long, Long>,
        source: AnimeBackupSource?,
        format: AnimeBackupFormat = AnimeBackupFormat.TSUZUKI,
    ): Long = database.transactionWithResult {
        require(entry.url.isNotBlank()) { "Invalid anime backup key" }
        val existingSource = sources.awaitById(entry.source)
        val fallbackName = entry.source.toString()
        if (existingSource == null || (existingSource.name == fallbackName && !source?.name.isNullOrBlank())) {
            val name = source?.name?.takeIf(String::isNotBlank) ?: fallbackName
            sources.upsert(StubAnimeSource(entry.source, source?.lang.orEmpty(), name))
        }
        val incoming = entry.toAnime()
        val current = animes.awaitBySourceAndUrl(entry.source, entry.url)
        val id = if (current == null) {
            animes.insert(incoming)
        } else {
            val merged = current.copy(
                title = current.title.ifBlank { incoming.title },
                artist = current.artist ?: incoming.artist,
                author = current.author ?: incoming.author,
                description = current.description ?: incoming.description,
                genre = current.genre ?: incoming.genre,
                status = current.status.takeUnless { it == 0L } ?: incoming.status,
                thumbnailUrl = current.thumbnailUrl ?: incoming.thumbnailUrl,
                backgroundUrl = current.backgroundUrl ?: incoming.backgroundUrl,
                favorite = current.favorite || incoming.favorite,
                initialized = current.initialized || incoming.initialized,
                notes = current.notes.ifBlank { incoming.notes },
                alternativeTitles = current.alternativeTitles ?: incoming.alternativeTitles,
                dateAdded = current.dateAdded.takeIf { it > 0 } ?: incoming.dateAdded,
                seasonFlags = current.seasonFlags.takeUnless { it == 0L } ?: incoming.seasonFlags,
                seasonNumber = current.seasonNumber.takeIf { it >= 0 } ?: incoming.seasonNumber,
                seasonSourceOrder = current.seasonSourceOrder.takeUnless { it == 0L } ?: incoming.seasonSourceOrder,
            )
            if (merged != current) animes.update(merged)
            current.id
        }
        val mapped = entry.categories.mapNotNullTo(mutableSetOf()) { categoryIds[it] }
        if (mapped.isNotEmpty()) {
            val existing = categories.awaitByAnimeId(id).map { it.id }
            categories.setForAnime(id, mapped + existing)
        }
        restoreEpisodes(id, entry, format)
        restoreTracks(id, entry.tracks)
        id
    }

    private suspend fun restoreEpisodes(animeId: Long, entry: AnimeBackupEntry, format: AnimeBackupFormat) {
        val local = episodes.awaitByAnimeId(animeId).associateBy(Episode::url).toMutableMap()
        for (row in entry.episodes) {
            if (row.url.isBlank()) continue
            val incoming = row.toEpisode(animeId, format)
            val existing = local[row.url]
            if (existing == null) {
                val id = episodes.insertAll(listOf(incoming)).single()
                local[row.url] = incoming.copy(id = id)
            } else {
                val seen = existing.seen || incoming.seen
                val duration = maxOf(existing.totalSeconds, incoming.totalSeconds)
                val resume = maxOf(existing.lastSecondSeen, incoming.lastSecondSeen)
                if (seen != existing.seen || resume != existing.lastSecondSeen || duration != existing.totalSeconds) {
                    episodes.updateProgress(existing.id, seen, resume, duration)
                }
                if (incoming.bookmark && !existing.bookmark) episodes.setBookmark(existing.id, true)
                local[row.url] = existing.copy(
                    seen = seen,
                    lastSecondSeen = resume,
                    totalSeconds = duration,
                    bookmark = existing.bookmark || incoming.bookmark,
                )
            }
        }
        for (row in entry.history) {
            val episode = local[row.episodeUrl] ?: continue
            val existing = history.awaitByEpisodeId(episode.id)
            val newTime = row.watchSeconds.coerceAtLeast(0)
            val delta = (newTime - (existing?.timeWatched ?: 0).coerceAtLeast(0)).coerceAtLeast(0)
            val lastSeen = row.lastSeen.takeIf { it > (existing?.lastSeen?.time ?: 0) }?.let(::Date)
            if (delta > 0 || lastSeen != null) history.upsert(episode.id, lastSeen, delta)
        }
    }

    private suspend fun restoreTracks(animeId: Long, rows: List<AnimeBackupTrack>) {
        val existing = tracks.awaitByAnimeId(animeId).associateBy { it.syncId }.toMutableMap()
        for (row in rows) {
            val incoming = row.toTrack(animeId) ?: continue
            val current = existing[incoming.syncId]
            if (current != null && current.remoteId != incoming.remoteId) continue
            val merged = current?.copy(
                lastEpisodeSeen = maxOf(current.lastEpisodeSeen, incoming.lastEpisodeSeen),
                malId = current.malId ?: incoming.malId,
            ) ?: incoming
            if (current != merged) tracks.upsert(merged)
            existing[incoming.syncId] = merged
        }
    }

    suspend fun restoreParents(oldToNew: Map<Long, Long>, parentLinks: Map<Long, Long>) = database.transaction {
        for ((childId, oldParent) in parentLinks) {
            val parentId = oldToNew[oldParent]?.takeUnless { it == childId } ?: continue
            val child = animes.awaitById(childId) ?: continue
            var ancestor: Long? = parentId
            val visited = hashSetOf(childId)
            while (ancestor != null && visited.add(ancestor)) {
                ancestor = animes.awaitById(ancestor)?.parentId
            }
            if (ancestor == null && child.parentId == null) animes.update(child.copy(parentId = parentId))
        }
    }
}
