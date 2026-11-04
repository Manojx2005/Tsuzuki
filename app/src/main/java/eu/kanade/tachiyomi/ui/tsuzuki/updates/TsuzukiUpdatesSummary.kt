package eu.kanade.tachiyomi.ui.tsuzuki.updates

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.updates.UpdatesItem

/** Totals for the complete filtered result, independent of the list's loaded page. */
@Immutable
data class TsuzukiUpdatesSummary(val newCount: Int, val toSave: Int)

fun List<UpdatesItem>.unreadUpdatesToSave(): List<UpdatesItem> = filter {
    !it.update.read && it.downloadStateProvider() in setOf(Download.State.NOT_DOWNLOADED, Download.State.ERROR)
}

fun List<UpdatesItem>.toTsuzukiUpdatesSummary() = TsuzukiUpdatesSummary(
    newCount = count { !it.update.read },
    toSave = unreadUpdatesToSave().size,
)
