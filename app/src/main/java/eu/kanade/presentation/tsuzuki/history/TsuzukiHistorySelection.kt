package eu.kanade.presentation.tsuzuki.history

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import tachiyomi.domain.history.model.HistoryWithRelations

@Stable
internal class TsuzukiHistorySelection {
    var ids by mutableStateOf<Set<Long>>(emptySet())
        private set
    private var anchor: Long? = null

    fun toggle(id: Long) {
        ids = if (id in ids) ids - id else ids + id
        if (ids.isEmpty()) anchor = null
    }

    fun extendTo(id: Long, rows: List<HistoryWithRelations>) {
        ids = ids + historySelectionRange(rows.map { it.id }, anchor, id)
        anchor = id
    }

    fun selectAll(rows: List<HistoryWithRelations>) {
        ids = rows.mapTo(mutableSetOf()) { it.id }
        anchor = null
    }

    fun clear() {
        ids = emptySet()
        anchor = null
    }
}

internal fun historySelectionRange(order: List<Long>, anchor: Long?, end: Long): List<Long> {
    val endIndex = order.indexOf(end)
    if (endIndex == -1) return emptyList()
    val anchorIndex = anchor?.let(order::indexOf) ?: -1
    if (anchorIndex == -1) return listOf(end)
    return order.subList(minOf(anchorIndex, endIndex), maxOf(anchorIndex, endIndex) + 1)
}
