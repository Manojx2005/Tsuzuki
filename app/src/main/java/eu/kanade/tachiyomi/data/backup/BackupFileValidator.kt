package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupExtensionStore
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.isBackupSourceName
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.tsuzuki.backup.isBackupField
import kotlinx.serialization.protobuf.ProtoBuf
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException

class BackupFileValidator(
    private val context: Context,

    private val sourceManager: SourceManager = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
    private val parser: ProtoBuf = Injekt.get(),
) {

    /**
     * Checks for critical backup file data.
     *
     * @return File contents and missing sources or trackers from the same streaming pass.
     */
    suspend fun validate(uri: Uri): Results {
        val backupSources = mutableListOf<BackupSource>()
        val trackerIds = mutableSetOf<Long>()
        var books = 0
        var shelves = 0
        var historyEntries = 0L
        val reader = BackupProtoReader(context)
        var recognised = false
        reader.read(uri) { fieldNumber, data ->
            if (isBackupField(fieldNumber)) recognised = true
            when (fieldNumber) {
                1 -> {
                    val migrated = BackupProtoMigration.migrateManga(data)
                    val manga = parser.decodeFromByteArray(BackupManga.serializer(), migrated)
                    manga.tracking.forEach { trackerIds.add(it.syncId.toLong()) }
                    books++
                    historyEntries += manga.history.size
                }
                2 -> {
                    parser.decodeFromByteArray(BackupCategory.serializer(), data)
                    shelves++
                }
                101 -> {
                    val source = parser.decodeFromByteArray(BackupSource.serializer(), data)
                    backupSources.add(source)
                }
                104 -> parser.decodeFromByteArray(BackupPreference.serializer(), data)
                105 -> parser.decodeFromByteArray(BackupSourcePreferences.serializer(), data)
                106 -> parser.decodeFromByteArray(BackupExtensionStore.serializer(), data)
            }
        }
        if (!recognised) throw IOException(context.stringResource(MR.strings.invalid_backup_file_unknown))

        val sources = backupSources.associate { it.sourceId to it.name }
        val missingSources = sources
            .filter { sourceManager.get(it.key) == null }
            .map { (id, name) ->
                if (name.isBackupSourceName()) name else sourceManager.getOrStub(id).toString()
            }
            .distinct()
            .sorted()

        val missingTrackers = trackerIds
            .mapNotNull { trackerManager.get(it) }
            .filter { !it.isLoggedIn }
            .map { it.name }
            .sorted()

        return Results(missingSources, missingTrackers, Contents(books, shelves, historyEntries))
    }

    data class Results(
        val missingSources: List<String>,
        val missingTrackers: List<String>,
        val contents: Contents,
    )

    data class Contents(val books: Int, val shelves: Int, val historyEntries: Long)
}
