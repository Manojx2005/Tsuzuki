package eu.kanade.tachiyomi.data.updater

/**
 * True when [releaseVersion] is the exact version the user chose to skip. Only that version is hidden: any newer (or
 * different) release is still announced, and an empty [skippedVersion] means nothing was skipped.
 */
fun isAppUpdateSkipped(releaseVersion: String, skippedVersion: String): Boolean {
    return skippedVersion.isNotEmpty() && releaseVersion == skippedVersion
}
