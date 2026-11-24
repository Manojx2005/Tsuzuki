package eu.kanade.presentation.tsuzuki.selection

/** Extend selection to the nearest visible boundary, or add just the pressed row inside the boundaries. */
internal inline fun <T> visibleSelectionRange(
    items: List<T>,
    index: Int,
    isSelected: (T) -> Boolean,
): IntRange {
    val first = items.indexOfFirst(isSelected)
    val last = items.indexOfLast(isSelected)
    return when {
        first == -1 -> index..index
        index < first -> index..first
        index > last -> last..index
        else -> index..index
    }
}
