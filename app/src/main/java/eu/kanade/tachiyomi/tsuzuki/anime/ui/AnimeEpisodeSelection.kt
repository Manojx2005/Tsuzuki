package eu.kanade.tachiyomi.tsuzuki.anime.ui

import eu.kanade.presentation.tsuzuki.selection.visibleSelectionRange
import tachiyomi.domain.items.episode.model.Episode

/** Use the episode order shown after applying the current filters and sort. */
internal fun toggleAnimeEpisodeSelection(
    visible: List<Episode>,
    selection: LongArray,
    id: Long,
    fromLongPress: Boolean,
): LongArray {
    val index = visible.indexOfFirst { it.id == id }
    if (index == -1) return selection
    if (id in selection) return selection.filterNot { it == id }.toLongArray()
    if (!fromLongPress) return selection + id

    val selectedIds = selection.toMutableSet()
    val range = visibleSelectionRange(visible, index) { it.id in selectedIds }
    range.forEach { selectedIds.add(visible[it].id) }
    return selectedIds.toLongArray()
}
