package eu.kanade.presentation.tsuzuki.settings

import java.text.NumberFormat
import java.util.Locale

// Pure helpers for the Data and storage breakdown (no Android types, so they are unit-testable).

/** Result of walking a directory tree; [partial] is true when the time budget or cancellation cut it short. */
data class TreeMeasure(val bytes: Long, val files: Int, val partial: Boolean)

/**
 * Sums file sizes under [root] without recursion. [children] returns null for a file (or an unreadable directory).
 * Stops early once [now] passes [deadline] or [keepGoing] returns false, and then reports a partial total.
 * Works for java.io.File and for slow SAF documents alike.
 */
fun <T> measureTree(
    root: T,
    children: (T) -> List<T>?,
    length: (T) -> Long,
    now: () -> Long,
    deadline: Long,
    keepGoing: () -> Boolean = { true },
): TreeMeasure {
    var bytes = 0L
    var files = 0
    val stack = ArrayDeque<T>()
    stack.addLast(root)
    var isRoot = true
    while (stack.isNotEmpty()) {
        if (!keepGoing() || now() > deadline) return TreeMeasure(bytes, files, partial = true)
        val node = stack.removeLast()
        val kids = children(node)
        if (kids == null) {
            if (!isRoot) {
                bytes += length(node).coerceAtLeast(0L)
                files++
            }
        } else {
            kids.forEach(stack::addLast)
        }
        isRoot = false
    }
    return TreeMeasure(bytes, files, partial = false)
}

/** What the screen shows. A null value means "still calculating". */
data class StorageBreakdown(
    val downloads: Long? = null,
    val covers: Long? = null,
    val database: Long? = null,
    val cache: Long? = null,
    val downloadsPartial: Boolean = false,
    val novels: Long? = null,
    val manga: Long? = null,
    val chapters: Long? = null,
) {
    val sizes: List<Long?> get() = listOf(downloads, covers, database, cache)

    /** Sum of the sizes measured so far. */
    val total: Long get() = sizes.sumOf { it ?: 0L }

    val isComplete: Boolean get() = sizes.none { it == null }
}

/** Share of each segment (0..1, summing to 1 unless everything is empty). Unmeasured segments count as 0. */
fun storageFractions(sizes: List<Long?>): List<Float> {
    val clean = sizes.map { (it ?: 0L).coerceAtLeast(0L) }
    val total = clean.sum()
    if (total <= 0L) return clean.map { 0f }
    return clean.map { it.toFloat() / total }
}

/** Decimal (SI) size like the platform formatter: "0 B", "412 KB", "1.8 GB". */
fun formatStorageSize(bytes: Long, locale: Locale = Locale.getDefault()): String {
    val b = bytes.coerceAtLeast(0L)
    if (b < 1000L) return "$b B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = b / 1000.0
    var unit = 0
    while (value >= 1000.0 && unit < units.lastIndex) {
        value /= 1000.0
        unit++
    }
    val fmt = NumberFormat.getNumberInstance(locale)
    fmt.minimumFractionDigits = 0
    fmt.maximumFractionDigits = if (value < 100.0) 1 else 0
    return "${fmt.format(value)} ${units[unit]}"
}

/** Counts like the board: "1,284", "412k", "1.2M". */
fun formatCompactCount(n: Long, locale: Locale = Locale.getDefault()): String {
    val v = n.coerceAtLeast(0L)
    val fmt = NumberFormat.getNumberInstance(locale)
    return when {
        v < 10_000L -> fmt.format(v)
        v < 1_000_000L -> {
            fmt.maximumFractionDigits = 0
            fmt.format(v / 1000.0) + "k"
        }
        else -> {
            fmt.maximumFractionDigits = 1
            fmt.format(v / 1_000_000.0) + "M"
        }
    }
}
