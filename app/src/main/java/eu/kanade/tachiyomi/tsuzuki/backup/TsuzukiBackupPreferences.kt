package eu.kanade.tachiyomi.tsuzuki.backup

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import kotlin.time.Clock

internal val backupRetentionChoices = listOf(2, 4, 8, 12)
private const val DEFAULT_BACKUP_RETENTION = 4

internal class TsuzukiBackupPreferences(preferenceStore: PreferenceStore) {
    val retention = preferenceStore.getInt("tsz_backup_retention", DEFAULT_BACKUP_RETENTION)
    val lastExport = preferenceStore.getString(Preference.appStateKey("tsz_last_backup_export"), "")

    fun retentionCount(): Int = retention.get().takeIf { it in backupRetentionChoices } ?: DEFAULT_BACKUP_RETENTION

    fun recordExport(sizeBytes: Long) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        lastExport.set("$timestamp:$sizeBytes")
    }
}

internal data class BackupExport(val timestamp: Long, val sizeBytes: Long?) {
    companion object {
        fun fromPreference(value: String, legacyAutoTimestamp: Long): BackupExport? {
            val timestamp = value.substringBefore(':').toLongOrNull()?.takeIf { it > 0 }
            val size = value.substringAfter(':', "").toLongOrNull()?.takeIf { it > 0 }
            return if (timestamp != null && timestamp >= legacyAutoTimestamp) {
                BackupExport(timestamp, size)
            } else {
                legacyAutoTimestamp.takeIf { it > 0 }?.let { BackupExport(it, null) }
            }
        }
    }
}
