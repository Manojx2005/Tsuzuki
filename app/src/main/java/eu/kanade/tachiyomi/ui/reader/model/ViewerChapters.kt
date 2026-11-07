package eu.kanade.tachiyomi.ui.reader.model

data class ViewerChapters(
    val currChapter: ReaderChapter,
    val prevChapter: ReaderChapter?,
    val nextChapter: ReaderChapter?,
    private val previousEntry: PreviousEntry? = null,
) {
    // Each Previous tap gets its own entry even when the chapter snapshot compares otherwise equal.
    class PreviousEntry internal constructor() {
        private var pending = true

        internal fun take(): Boolean {
            val wasPending = pending
            pending = false
            return wasPending
        }
    }

    // Renderer changes and reloads can dispatch this snapshot again.
    internal fun takePreviousEntry(): Boolean = previousEntry?.take() == true

    fun ref() {
        currChapter.ref()
        prevChapter?.ref()
        nextChapter?.ref()
    }

    fun unref() {
        currChapter.unref()
        prevChapter?.unref()
        nextChapter?.unref()
    }
}
