package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.isBackupSourceName
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.backup.AnimeBackupEntry
import eu.kanade.tachiyomi.tsuzuki.anime.backup.AnimeBackupFormat
import eu.kanade.tachiyomi.tsuzuki.anime.backup.AnimeBackupSummary
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtensionLoader
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeLoadResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.protobuf.ProtoBuf
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BackupFileValidator(
    private val context: Context,

    private val sourceManager: SourceManager = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
    private val parser: ProtoBuf = Injekt.get(),
) {

    /**
     * Checks for critical backup file data.
     *
     * @return List of missing sources or missing trackers.
     */
    suspend fun validate(uri: Uri): Results {
        val backupSources = mutableListOf<BackupSource>()
        val trackerIds = mutableSetOf<Long>()
        val anime = AnimeBackupSummary()
        val animeTrackers = mutableMapOf<AnimeBackupFormat, MutableSet<Long>>()
        val reader = BackupProtoReader(context)
        try {
            reader.read(uri) { fieldNumber, data ->
                when (fieldNumber) {
                    1 -> {
                        val migrated = BackupProtoMigration.migrateManga(data)
                        val manga = parser.decodeFromByteArray(BackupManga.serializer(), migrated)
                        manga.tracking.forEach { trackerIds.add(it.syncId.toLong()) }
                    }
                    101 -> {
                        val source = parser.decodeFromByteArray(BackupSource.serializer(), data)
                        backupSources.add(source)
                    }
                    else -> if (TsuzukiAnime.enabled) {
                        anime.record(fieldNumber, data, parser)
                        AnimeBackupFormat.entries.firstOrNull { it.entries == fieldNumber }?.let { format ->
                            val entry = parser.decodeFromByteArray(AnimeBackupEntry.serializer(), data)
                            entry.tracks.mapTo(animeTrackers.getOrPut(format, ::mutableSetOf), { it.service })
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }

        val sources = backupSources.associate { it.sourceId to it.name }
        val missingBookSources = sources
            .filter { sourceManager.get(it.key) == null }
            .map { (id, name) ->
                if (name.isBackupSourceName()) name else sourceManager.getOrStub(id).toString()
            }
        val missingAnimeSources = if (anime.sourceNames.isEmpty()) {
            emptyList()
        } else {
            val installedIds = withContext(Dispatchers.IO) {
                AnimeExtensionLoader.loadExtensions(context).filterIsInstance<AnimeLoadResult.Success>()
                    .flatMap { it.extension.sources }.mapTo(hashSetOf()) { it.id }
            }
            anime.sourceNames.filter { it.id !in installedIds }.map { it.name.ifBlank { it.id.toString() } }
        }

        trackerIds +=
            animeTrackers[anime.format].orEmpty().filter {
                it == trackerManager.myAnimeList.id ||
                    it == trackerManager.aniList.id
            }
        val missingTrackers = trackerIds
            .mapNotNull { trackerManager.get(it) }
            .filter { !it.isLoggedIn }
            .map { it.name }
            .sorted()

        return Results((missingBookSources + missingAnimeSources).distinct().sorted(), missingTrackers)
    }

    data class Results(
        val missingSources: List<String>,
        val missingTrackers: List<String>,
    )
}
