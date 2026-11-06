package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Drives a custom pager header (Browse: media pills × section segment).
 *
 * The header follows the page the pager is heading to, not the one it is passing through: going from
 * Novel sources to Novel extensions crosses Manga sources, and following `currentPage` made the
 * NOVELS / MANGA pills flick to MANGA and back. A tap that skips pages jumps instead of sliding through them.
 */
@Composable
internal fun TsuzukiPagerHeader(
    state: PagerState,
    pageCount: Int,
    scope: CoroutineScope,
    header: @Composable (currentPage: Int, onSelectPage: (Int) -> Unit) -> Unit,
) {
    if (pageCount == 0) return
    header(state.targetPage.coerceIn(0, pageCount - 1)) { index ->
        if (index in 0 until pageCount) {
            scope.launch {
                if (pagerJumps(state.currentPage, index)) state.scrollToPage(index) else state.animateScrollToPage(index)
            }
        }
    }
}

/** True when moving from [from] to [to] would slide through another page on the way. */
internal fun pagerJumps(from: Int, to: Int): Boolean = abs(to - from) > 1
