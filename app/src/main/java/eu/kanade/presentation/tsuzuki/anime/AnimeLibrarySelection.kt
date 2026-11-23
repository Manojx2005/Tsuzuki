package eu.kanade.presentation.tsuzuki.anime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.ui.library.libraryRangeSelection

internal class AnimeLibrarySelection {
    var ids by mutableStateOf(emptySet<Long>())
        private set
    private var anchor: Long? = null

    fun toggle(id: Long) {
        ids = if (id in ids) ids - id else ids + id
        anchor = id.takeIf { it in ids }
    }

    fun range(id: Long, visible: List<Long>) {
        ids = ids + libraryRangeSelection(visible, anchor, id)
        anchor = id
    }

    fun selectAll(visible: List<Long>) {
        ids = ids + visible
        anchor = null
    }

    fun invert(visible: List<Long>) {
        val visibleIds = visible.toSet()
        ids = (ids - visibleIds) + (visibleIds - ids)
        anchor = null
    }

    fun clear() {
        ids = emptySet()
        anchor = null
    }
}
