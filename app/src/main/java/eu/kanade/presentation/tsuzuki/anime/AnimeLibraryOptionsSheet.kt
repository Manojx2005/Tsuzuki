package eu.kanade.presentation.tsuzuki.anime

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.TabTitle
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryDisplay
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryOptionPreferences
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibrarySort
import tachiyomi.core.common.preference.TriState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.RadioItem
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.i18n.stringResource as mokoString

/**
 * AP8: the anime library's Filter / Sort / Display sheet, in the book library sheet's design (TabbedDialog with the
 * shared settings rows). Shelves are edited from the sheet's overflow menu, as books' categories are.
 */
@Composable
internal fun AnimeLibraryOptionsSheet(
    preferences: AnimeLibraryOptionPreferences,
    onDismiss: () -> Unit,
    onEditShelves: () -> Unit,
) {
    TabbedDialog(
        onDismissRequest = onDismiss,
        tabTitles = listOf(
            TabTitle.Text(mokoString(MR.strings.action_filter)),
            TabTitle.Text(mokoString(MR.strings.action_sort)),
            TabTitle.Text(mokoString(MR.strings.action_display)),
        ),
        tabOverflowMenuContent = { close ->
            DropdownMenuItem(
                text = { Text(mokoString(MR.strings.tsz_anime_shelves)) },
                onClick = {
                    close()
                    onEditShelves()
                },
            )
        },
    ) { page ->
        Column(Modifier.padding(vertical = 8.dp).verticalScroll(rememberScrollState())) {
            when (page) {
                0 -> FilterPage(preferences)
                1 -> SortPage(preferences)
                else -> DisplayPage(preferences)
            }
        }
    }
}

@Composable
private fun FilterPage(preferences: AnimeLibraryOptionPreferences) {
    listOf(
        mokoString(MR.strings.label_downloaded) to preferences.downloaded,
        stringResource(R.string.tsz_anime_unwatched) to preferences.unwatched,
        mokoString(MR.strings.label_started) to preferences.started,
        mokoString(MR.strings.completed) to preferences.completed,
    ).forEach { (label, preference) ->
        val state by preference.collectAsState()
        TriStateItem(label = label, state = state, onClick = { preference.set(state.next()) })
    }
}

@Composable
private fun SortPage(preferences: AnimeLibraryOptionPreferences) {
    val sort by preferences.sort.collectAsState()
    val descending by preferences.descending.collectAsState()
    listOf(
        AnimeLibrarySort.TITLE to mokoString(MR.strings.action_sort_alpha),
        AnimeLibrarySort.LAST_WATCHED to stringResource(R.string.tsz_anime_sort_last_watched),
        AnimeLibrarySort.LATEST_EPISODE to stringResource(R.string.tsz_anime_sort_latest_episode),
        AnimeLibrarySort.UNWATCHED to stringResource(R.string.tsz_anime_sort_unwatched_count),
        AnimeLibrarySort.DATE_ADDED to mokoString(MR.strings.action_sort_date_added),
        AnimeLibrarySort.TOTAL_EPISODES to stringResource(R.string.tsz_anime_sort_total_episodes),
    ).forEach { (field, label) ->
        SortItem(label = label, sortDescending = descending.takeIf { sort == field }) {
            if (sort == field) {
                preferences.descending.set(!descending)
            } else {
                preferences.sort.set(field)
                preferences.descending.set(false)
            }
        }
    }
}

@Composable
private fun DisplayPage(preferences: AnimeLibraryOptionPreferences) {
    val display by preferences.display.collectAsState()
    listOf(
        AnimeLibraryDisplay.GRID to stringResource(R.string.tsz_anime_display_grid),
        AnimeLibraryDisplay.LIST to stringResource(R.string.tsz_anime_display_list),
    ).forEach { (mode, label) ->
        RadioItem(label = label, selected = display == mode) { preferences.display.set(mode) }
    }
}

/** True while any filter is set, for tinting the library's filter icon. */
@Composable
internal fun AnimeLibraryOptionPreferences.anyFilter(): Boolean {
    val values = listOf(downloaded, unwatched, started, completed).map { it.collectAsState().value }
    return values.any { it != TriState.DISABLED }
}
