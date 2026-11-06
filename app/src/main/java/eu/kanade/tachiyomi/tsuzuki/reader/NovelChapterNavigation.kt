package eu.kanade.tachiyomi.tsuzuki.reader

import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters

internal fun adjacentNovelChapter(
    active: ViewerChapters,
    ordered: List<ReaderChapter>,
    visibleId: Long?,
    forward: Boolean,
    infiniteScroll: Boolean,
    paged: Boolean,
): ReaderChapter? {
    if (infiniteScroll && !paged && visibleId != null && visibleId != active.currChapter.chapter.id) {
        val visibleIndex = ordered.indexOfFirst { it.chapter.id == visibleId }
        if (visibleIndex >= 0) {
            return ordered.getOrNull(visibleIndex + if (forward) 1 else -1)
        }
    }
    return if (forward) active.nextChapter else active.prevChapter
}

/** Replays the last requested direction once an adjacent chapter load completes. */
internal class PendingAdjacentNavigation {
    private var forward: Boolean? = null

    fun deferIfLoading(isLoading: Boolean, forward: Boolean): Boolean {
        if (isLoading) this.forward = forward
        return isLoading
    }

    fun takeWhenIdle(isLoading: Boolean): Boolean? {
        if (isLoading) return null
        return forward.also { forward = null }
    }
}
