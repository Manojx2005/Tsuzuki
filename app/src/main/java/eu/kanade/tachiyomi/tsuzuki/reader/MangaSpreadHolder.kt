package eu.kanade.tachiyomi.tsuzuki.reader

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerPageHolder
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.widget.ViewPagerAdapter

@SuppressLint("ViewConstructor")
internal class MangaSpreadHolder(
    context: Context,
    viewer: PagerViewer,
    override val item: MangaSpread,
) : LinearLayout(context), ViewPagerAdapter.PositionableView {
    val left = PagerPageHolder(context, viewer, item.left)
    val right = PagerPageHolder(context, viewer, item.right)

    init {
        orientation = HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        addView(left, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        addView(right, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
    }

    fun onPageSelected(forward: Boolean) {
        left.onPageSelected(forward)
        right.onPageSelected(forward)
    }
}
