package eu.kanade.tachiyomi.tsuzuki.backup

/**
 * Backup files are named after the app ("Tsuzuki_2026-11-07_22-00.tachibk") instead of the package id. Test builds get
 * their own prefix so their backups are easy to tell apart from the real app's.
 */
internal fun backupFilePrefix(applicationId: String): String = when {
    applicationId.endsWith(".dev") -> "Tsuzuki-dev"
    applicationId.endsWith(".debug") -> "Tsuzuki-preview"
    applicationId.endsWith(".nightly") -> "Tsuzuki-nightly"
    else -> "Tsuzuki"
}

/**
 * Matches only the scheduler's own files (`…_yyyy-MM-dd_HH-mm-ss-SSS.tachibk`), so rotation never deletes a manual
 * export. Manual exports use the shorter `…_yyyy-MM-dd_HH-mm.tachibk` name, which older automatic backups shared:
 * those can't be told apart, so they are kept rather than rotated.
 */
internal fun backupFileRegex(applicationId: String): Regex {
    val prefixes = listOf(Regex.escape(backupFilePrefix(applicationId)), Regex.escape(applicationId))
    return Regex("""(?:${prefixes.joinToString("|")})_(\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}-\d{3})\.tachibk""")
}

/** Top-level fields of a Tsuzuki / Mihon backup (`models/Backup.kt`). A file with none of them is not a backup. */
internal fun isBackupField(fieldNumber: Int): Boolean = fieldNumber in setOf(1, 2, 101, 104, 105, 106)

internal fun <T> backupsToPrune(
    backups: Iterable<T>,
    regex: Regex,
    keep: Int,
    newestName: String,
    nameOf: (T) -> String?,
): List<T> = backups.mapNotNull { file ->
    val name = nameOf(file)?.takeUnless { it == newestName } ?: return@mapNotNull null
    val date = regex.matchEntire(name)?.groupValues?.get(1) ?: return@mapNotNull null
    date to file
}.sortedByDescending { it.first }
    .drop(keep - 1)
    .map { it.second }
