package eu.kanade.presentation.tsuzuki.novel

/**
 * "n/m" text for the find bar. [activeIndex] is the 0-based ordinal WebView reports; with no match it reads "0/0".
 */
internal fun findCounterText(activeIndex: Int, total: Int): String =
    if (total <= 0) "0/0" else "${activeIndex.coerceIn(0, total - 1) + 1}/$total"
