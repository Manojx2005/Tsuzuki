package eu.kanade.tachiyomi.tsuzuki.reader

import android.view.View
import androidx.core.view.children
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import java.util.WeakHashMap

internal class MangaSpreadController(
    viewer: PagerViewer,
    private val themed: Boolean,
    private val isIdle: () -> Boolean,
    private val setChapters: (ViewerChapters) -> Unit,
    private val recreateHolders: () -> Unit,
) {
    private val pager = viewer.pager
    private val config = viewer.config
    private val rightToLeft = viewer is R2LPagerViewer
    private val portraitPages = WeakHashMap<ReaderPage, Boolean>()
    private var layoutDirty = false

    var chapters: ViewerChapters? = null
    var active = false
        private set

    private val updateLayout = Runnable {
        if (!isIdle()) return@Runnable
        val enabled = config.tabletSpreadEnabled && pager.width > pager.height &&
            pager.width / pager.resources.displayMetrics.density >= MANGA_SPREAD_MIN_WIDTH_DP
        val modeChanged = enabled != active
        active = enabled
        if (modeChanged || (active && layoutDirty)) {
            layoutDirty = false
            chapters?.let(setChapters)
            // Existing holders may have already applied the other wide-page rendering policy.
            if (modeChanged) recreateHolders()
        }
    }

    private val onLayout = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> scheduleLayout() }

    fun attach() {
        if (!themed) return
        config.tabletSpreadChangedListener = ::scheduleLayout
        pager.addOnLayoutChangeListener(onLayout)
    }

    fun detach() {
        config.tabletSpreadChangedListener = null
        pager.removeOnLayoutChangeListener(onLayout)
        pager.removeCallbacks(updateLayout)
    }

    fun scheduleLayout() {
        if (!themed || (!config.tabletSpreadEnabled && !active)) return
        pager.removeCallbacks(updateLayout)
        pager.post(updateLayout)
    }

    fun recordShape(page: ReaderPage, portrait: Boolean) {
        if (active && page.index > 0 && portraitPages.put(page, portrait) != portrait) {
            layoutDirty = true
            scheduleLayout()
        }
    }

    fun group(items: MutableList<Any>): MutableList<Any> {
        if (!active) return items
        return groupMangaPages(items, rightToLeft) { portraitPages[it] == true }.toMutableList()
    }

    fun positionOf(items: List<Any>, item: Any?): Int {
        val page = endPage(item)
        if (!active || page == null) return items.indexOf(page ?: item)
        return items.indexOfFirst { it == page || (it is MangaSpread && (it.first == page || it.second == page)) }
    }

    fun endPage(item: Any?): ReaderPage? = (item as? MangaSpread)?.second ?: item as? ReaderPage

    fun startIndex(item: Any?): Int? = (item as? MangaSpread)?.first?.number

    fun pageAtTap(item: Any?, x: Float): Any? {
        if (item !is MangaSpread) return item
        return if (x < pager.width / 2f) item.left else item.right
    }

    fun holder(item: Any?): MangaSpreadHolder? {
        if (item !is MangaSpread) return null
        return pager.children.filterIsInstance<MangaSpreadHolder>().firstOrNull { it.item == item }
    }

    fun notifyHolder(item: Any?, forward: Boolean): Boolean {
        if (item !is MangaSpread) return false
        holder(item)?.onPageSelected(forward)
        return true
    }

    fun selection(previous: Any?, item: Any?): SpreadSelection? {
        if (previous == item || (previous !is MangaSpread && item !is MangaSpread)) return null
        val page = endPage(item) ?: return null
        val previousPage = endPage(previous)
        val forward = when {
            previousPage != null && page.number == previousPage.number -> page is InsertPage
            previousPage != null -> page.number > previousPage.number
            else -> previous !is ChapterTransition.Prev
        }
        return SpreadSelection(page, forward)
    }

    data class SpreadSelection(val page: ReaderPage, val forward: Boolean)
}
