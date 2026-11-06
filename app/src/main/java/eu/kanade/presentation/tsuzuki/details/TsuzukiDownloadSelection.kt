package eu.kanade.presentation.tsuzuki.details

import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.ChapterList

internal enum class TsuzukiDownloadOption {
    NEXT_1,
    NEXT_5,
    NEXT_25,
    UNREAD,
    ALL,
    CUSTOM,
    NEXT_10,
    BOOKMARKED,
}

/** Receives chapters in reading order. Saved and active downloads never enter a new batch. */
internal fun chaptersToSave(
    chapters: List<ChapterList.Item>,
    option: TsuzukiDownloadOption,
    range: ClosedFloatingPointRange<Double>? = null,
): List<ChapterList.Item> {
    val candidates = if (option == TsuzukiDownloadOption.CUSTOM && range != null) {
        chaptersInNumberRange(chapters, range.start, range.endInclusive)
    } else {
        chapters
    }
    val available = candidates.filter {
        it.downloadState == Download.State.NOT_DOWNLOADED || it.downloadState == Download.State.ERROR
    }
    return when (option) {
        TsuzukiDownloadOption.NEXT_1 -> available.filterNot { it.chapter.read }.take(1)
        TsuzukiDownloadOption.NEXT_5 -> available.filterNot { it.chapter.read }.take(5)
        TsuzukiDownloadOption.NEXT_10 -> available.filterNot { it.chapter.read }.take(10)
        TsuzukiDownloadOption.NEXT_25 -> available.filterNot { it.chapter.read }.take(25)
        TsuzukiDownloadOption.UNREAD -> available.filterNot { it.chapter.read }
        TsuzukiDownloadOption.ALL -> available
        TsuzukiDownloadOption.BOOKMARKED -> available.filter { it.chapter.bookmark }
        TsuzukiDownloadOption.CUSTOM -> available.takeIf { range != null }.orEmpty()
    }.distinctBy { it.id }
}

/** Use the source number when known, otherwise use the 1-based reading-order position. */
internal fun downloadChapterNumber(item: ChapterList.Item, index: Int): Double =
    item.chapter.chapterNumber.takeIf { it.isFinite() && item.chapter.isRecognizedNumber } ?: (index + 1).toDouble()

internal fun chaptersInNumberRange(
    chapters: List<ChapterList.Item>,
    from: Double,
    to: Double,
): List<ChapterList.Item> =
    if (from.isFinite() && to.isFinite() && from <= to) {
        chapters.filterIndexed { index, item -> downloadChapterNumber(item, index) in from..to }
    } else {
        emptyList()
    }

internal fun parseDownloadChapterNumber(text: String): Double? {
    val normalized = text.trim().map { char ->
        when (char) {
            ',', '，', '．', '٫' -> '.'
            else -> char.digitToIntOrNull()?.digitToChar() ?: char
        }
    }.joinToString("")
    if (normalized.any { it !in '0'..'9' && it != '.' }) return null
    return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
}

/** Select beyond the nearest visible boundary. Hidden rows never take part in a long-press range. */
internal fun chapterSelectionRange(chapters: List<ChapterList.Item>, index: Int): IntRange {
    val first = chapters.indexOfFirst { it.selected }
    val last = chapters.indexOfLast { it.selected }
    return when {
        first == -1 -> index..index
        index < first -> index..first
        index > last -> last..index
        else -> index..index
    }
}

/**
 * "Mark previous read" in the chapter filter sheet: the unread chapters before the furthest chapter the reader has
 * reached (read, or opened with progress). Covers starting a book mid-way, e.g. after reading earlier chapters
 * elsewhere. Takes chapters in reading order.
 */
internal fun chaptersBeforeFurthestProgress(chapters: List<tachiyomi.domain.chapter.model.Chapter>) =
    chapters.indexOfLast { it.read || it.lastPageRead > 0 }
        .let { furthest -> if (furthest <= 0) emptyList() else chapters.take(furthest).filterNot { it.read } }

/** [chaptersBeforeFurthestProgress] over a details screen's chapters, in reading order. */
internal fun markPreviousCandidates(state: eu.kanade.tachiyomi.ui.manga.MangaViewModel.State.Success) =
    chaptersBeforeFurthestProgress(
        state.chapters.map { it.chapter }
            .sortedWith(tachiyomi.domain.chapter.service.getChapterSort(state.manga, sortDescending = false)),
    )
