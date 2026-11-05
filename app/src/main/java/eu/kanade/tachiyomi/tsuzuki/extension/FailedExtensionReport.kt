package eu.kanade.tachiyomi.tsuzuki.extension

import eu.kanade.tachiyomi.extension.model.Extension

/**
 * Plain text copied to the clipboard for an extension that failed to load (Mihon #3953), so it can be
 * pasted into a bug report. Deliberately not localised: it is meant for developers.
 */
fun Extension.Failed.toReport(): String = buildString {
    appendLine("Extension: $name ($pkgName)")
    appendLine("Version: $versionName (code $versionCode), lib $libVersion")
    appendLine("Installed: ${if (isShared) "system" else "private"}${if (isNovel) ", novel" else ""}")
    append("Reason: ").append(reason.name)
    if (!detail.isNullOrBlank()) append(" (").append(detail).append(')')
    appendLine()
    val trace = stackTrace
    if (!trace.isNullOrBlank()) {
        appendLine()
        append(trace.trimEnd())
        appendLine()
    }
}
