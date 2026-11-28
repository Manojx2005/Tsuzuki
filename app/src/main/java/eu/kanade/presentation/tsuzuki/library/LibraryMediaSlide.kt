package eu.kanade.presentation.tsuzuki.library

/**
 * Owner 2026-11-28: Novels -> Manga slides the segment highlight, but Manga -> Anime swapped the whole tab.
 * The books and anime libraries are separate tabs with their own segment row; the one being left records its
 * segment here and the one that appears slides its highlight from it.
 */
internal object LibraryMediaSlide {
    private var from: Int? = null

    fun leave(segment: Int) {
        from = segment
    }

    fun take(): Float? = from?.toFloat().also { from = null }
}
