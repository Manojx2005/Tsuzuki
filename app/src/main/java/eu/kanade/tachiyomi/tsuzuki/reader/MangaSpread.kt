package eu.kanade.tachiyomi.tsuzuki.reader

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

internal const val MANGA_SPREAD_MIN_WIDTH_DP = 900

/** Pages are stored in reading order. RTL only changes where each page is drawn. */
internal data class MangaSpread(val first: ReaderPage, val second: ReaderPage, val rightToLeft: Boolean) {
    val left: ReaderPage get() = if (rightToLeft) second else first
    val right: ReaderPage get() = if (rightToLeft) first else second
}

internal fun groupMangaPages(
    items: List<Any>,
    rightToLeft: Boolean,
    isPortrait: (ReaderPage) -> Boolean,
): List<Any> = buildList {
    var index = 0
    while (index < items.size) {
        val first = items[index++]
        val second = items.getOrNull(index)
        if (first is ReaderPage && second is ReaderPage && first.chapter === second.chapter &&
            first.index > 0 && second.index == first.index + 1 && isPortrait(first) && isPortrait(second)
        ) {
            add(MangaSpread(first, second, rightToLeft))
            index++
        } else {
            add(first)
        }
    }
}
