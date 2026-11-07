package eu.kanade.tachiyomi.tsuzuki.reader

internal fun novelEntryProgress(
    savedPercent: Int,
    isRead: Boolean,
    restoreReadPosition: Boolean,
    enteredViaPrevious: Boolean,
): Float {
    if (savedPercent !in 1..100 || (isRead && !restoreReadPosition)) return 0f
    // Previous should leave a completed chapter with room to scroll forward.
    if (enteredViaPrevious && savedPercent == 100) return 0f
    return savedPercent / 100f
}
