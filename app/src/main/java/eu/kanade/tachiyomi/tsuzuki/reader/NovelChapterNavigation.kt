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
    private var userLoadInFlight = false
    private var coolDownUntil = 0L

    /**
     * True when a tap must not start a load now. A tap during a load the reader started by itself (infinite
     * scroll, a listening handoff) is kept and replayed once it ends; a tap during a chapter switch the reader
     * asked for is dropped, so an accidental double tap on Next moves one chapter, not two (owner 2026-11-15).
     * A fast chapter (downloaded, local) can finish loading before the second tap of a double tap lands, so taps
     * in the [ADJACENT_TAP_COOL_DOWN_MS] after such a switch are dropped as well.
     */
    fun deferIfLoading(isLoading: Boolean, forward: Boolean, now: Long): Boolean {
        if (userLoadInFlight || now < coolDownUntil) return true
        if (isLoading) this.forward = forward
        return isLoading
    }

    fun beginUserLoad() {
        userLoadInFlight = true
    }

    fun endUserLoad(now: Long) {
        userLoadInFlight = false
        coolDownUntil = now + ADJACENT_TAP_COOL_DOWN_MS
    }

    fun takeWhenIdle(isLoading: Boolean): Boolean? {
        if (isLoading) return null
        return forward.also { forward = null }
    }
}

/** How long after a chapter switch the reader asked for further Next/Previous taps are ignored. */
internal const val ADJACENT_TAP_COOL_DOWN_MS = 600L
