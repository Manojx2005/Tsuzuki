package eu.kanade.presentation.tsuzuki.novel

import tachiyomi.domain.manga.interactor.DuplicateMatchMode

/** The match modes shown as the board's three segments, in order. */
internal val DuplicateSegmentModes = listOf(
    DuplicateMatchMode.EXACT,
    DuplicateMatchMode.CONTAINS,
    DuplicateMatchMode.URL,
)

internal fun duplicateSegmentIndex(mode: DuplicateMatchMode): Int = DuplicateSegmentModes.indexOf(mode).coerceAtLeast(0)

internal fun duplicateSegmentMode(index: Int): DuplicateMatchMode =
    DuplicateSegmentModes[index.coerceIn(0, DuplicateSegmentModes.lastIndex)]

/** Counts shown next to a copy's chapter count; a count of zero is left out, as in the stock row. */
internal enum class DuplicateStat { DOWNLOADS, READ }

internal fun duplicateStats(downloaded: Int, read: Long): List<DuplicateStat> = buildList {
    if (downloaded > 0) add(DuplicateStat.DOWNLOADS)
    if (read > 0) add(DuplicateStat.READ)
}

/** (groups, books) for the header subtitle. */
internal fun duplicateTotals(groupSizes: Collection<Int>): Pair<Int, Int> = groupSizes.size to groupSizes.sum()
