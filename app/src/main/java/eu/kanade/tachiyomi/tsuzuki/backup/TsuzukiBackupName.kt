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
 * Matches automatic backups by the new name and by the old package-id name, so older automatic backups are still
 * rotated out (only the newest few are kept).
 */
internal fun backupFileRegex(applicationId: String): Regex {
    val prefixes = listOf(Regex.escape(backupFilePrefix(applicationId)), Regex.escape(applicationId))
    return Regex("""(?:${prefixes.joinToString("|")})_\d{4}-\d{2}-\d{2}_\d{2}-\d{2}\.tachibk""")
}
