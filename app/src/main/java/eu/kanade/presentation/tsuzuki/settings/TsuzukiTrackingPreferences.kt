package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import tachiyomi.domain.track.interactor.GetTracksPerManga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun tsuzukiTrackingPreferences(stock: List<Preference>): List<Preference> {
    val novelTrackerIds = remember {
        Injekt.get<TrackerManager>().novelOnlyTrackers.mapTo(HashSet()) { it.id }
    }
    val linkedCounts by remember {
        Injekt.get<GetTracksPerManga>().subscribe().map { tracks ->
            tracks.values.asSequence().flatten().groupingBy { it.trackerId }.eachCount()
        }.flowOn(Dispatchers.Default)
    }.collectAsState(initial = null)
    val withCounts = stock.map { preference ->
        if (preference !is Preference.PreferenceGroup) return@map preference
        preference.copy(
            preferenceItems = preference.preferenceItems.map { item ->
                if (item is Preference.PreferenceItem.TrackerPreference) {
                    item.copy(linkedCount = linkedCounts?.let { it[item.tracker.id] ?: 0 })
                } else {
                    item
                }
            },
        )
    }
    val novelGroup = withCounts.filterIsInstance<Preference.PreferenceGroup>().firstOrNull { group ->
        group.preferenceItems.any {
            it is Preference.PreferenceItem.TrackerPreference &&
                it.tracker.id in novelTrackerIds
        }
    } ?: return withCounts
    val (accounts, options) = novelGroup.preferenceItems.partition { it is Preference.PreferenceItem.TrackerPreference }
    return listOf(
        novelGroup.copy(title = stringResource(MR.strings.tsz_tracker_novel_services), preferenceItems = accounts),
        novelGroup.copy(title = stringResource(MR.strings.pref_category_tracking), preferenceItems = options),
    ) + withCounts.filter { it !== novelGroup }
}
