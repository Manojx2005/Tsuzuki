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
    range: IntRange = chapters.indices,
): List<ChapterList.Item> {
    val available = chapters.filter {
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
        TsuzukiDownloadOption.CUSTOM -> chapters.filterIndexed { index, _ -> index in range }.filter {
            it.downloadState == Download.State.NOT_DOWNLOADED || it.downloadState == Download.State.ERROR
        }
    }.distinctBy { it.id }
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
