package eu.kanade.tachiyomi.tsuzuki.reader

/**
 * Index in the loaded-chapter list that the next-chapter hand-off must start from: the chapter
 * that is actually being read aloud (matched by id, so queue trimming cannot skew it). The
 * viewport chapter is only a fallback for when nothing is playing, because during autoplay the
 * viewport can lag behind the speech and would re-select the chapter that just finished.
 */
internal fun ttsHandoffAnchorIndex(
    loadedChapterIds: List<Long?>,
    playbackChapterId: Long?,
    fallbackIndex: Int,
): Int {
    if (playbackChapterId == null) return fallbackIndex
    val index = loadedChapterIds.indexOf(playbackChapterId)
    return if (index >= 0) index else fallbackIndex
}

/** Chapter title for the Listen dock: the playing chapter while speech is active, else the viewport's. */
internal fun listenDockChapterTitle(
    playbackChapterId: Long?,
    playbackChapterTitle: String?,
    viewportChapterTitle: String?,
): String? = if (playbackChapterId != null) playbackChapterTitle ?: viewportChapterTitle else viewportChapterTitle
