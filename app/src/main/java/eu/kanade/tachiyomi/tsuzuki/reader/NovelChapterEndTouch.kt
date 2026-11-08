package eu.kanade.tachiyomi.tsuzuki.reader

import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import androidx.core.widget.NestedScrollView

/**
 * Ends any overscroll stretch left at the top or bottom of the novel scroll view.
 *
 * NestedScrollView intercepts a new touch while a stretch is still showing, so after a fling into
 * the end of a chapter the chapter-end card's buttons don't receive the tap and the reader menu
 * toggles instead. A scroll fling that is still running is left alone (that intercept is wanted).
 */
internal fun NestedScrollView.settleNovelEdgeEffects() {
    mEdgeGlowTop.finish()
    mEdgeGlowBottom.finish()
}

/** True when [ev] lands on [card] (the chapter-end card), whose buttons handle the tap themselves. */
internal fun isOnChapterEndCard(card: View?, ev: MotionEvent): Boolean {
    if (card == null || !card.isShown) return false
    val bounds = Rect()
    return card.getGlobalVisibleRect(bounds) && bounds.contains(ev.rawX.toInt(), ev.rawY.toInt())
}
