package eu.kanade.presentation.tsuzuki.updates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.tsuzuki.selection.TsuzukiUndoSnackbarHost
import eu.kanade.presentation.tsuzuki.settings.isTsuzukiStyle
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryUpdateJob
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeUpdateEpisode
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.ui.updates.UpdatesFilter
import eu.kanade.tachiyomi.ui.updates.UpdatesViewModel
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class AnimeUpdatesSlots(
    val mediaPills: (@Composable (twoPane: Boolean) -> Unit)?,
    val airingStrip: (@Composable () -> Unit)?,
    val screen: (@Composable (SnackbarHostState) -> Unit)?,
)

@Composable
internal fun rememberAnimeUpdatesSlots(
    state: UpdatesViewModel.State,
    hideManga: Boolean,
    onFilterSelected: (UpdatesFilter) -> Unit,
): AnimeUpdatesSlots? {
    if (!TsuzukiAnime.enabled || !isTsuzukiStyle()) return null
    val store = remember { Injekt.get<AnimeLibraryStore>() }
    val hasAnime by remember(store) { store.observeHasFavorites() }.collectAsState(null)
    var animeFilter by rememberSaveable { mutableStateOf(false) }
    val animeSelected = hasAnime == true && animeFilter
    BackHandler(enabled = animeSelected) { animeFilter = false }
    LaunchedEffect(hasAnime) {
        if (hasAnime == false) animeFilter = false
    }
    if (hasAnime != true) return null

    val airing = rememberAnimeAiringToday(store)
    val pills: @Composable (Boolean) -> Unit = { twoPane ->
        AnimeUpdatesPills(
            filter = state.filter,
            hideManga = hideManga,
            animeSelected = animeSelected,
            twoPane = twoPane,
            onBookFilterSelected = {
                animeFilter = false
                onFilterSelected(it)
            },
            onAnimeSelected = { animeFilter = true },
        )
    }
    return AnimeUpdatesSlots(
        mediaPills = pills.takeUnless { state.selectionMode },
        airingStrip = airing?.let { entry -> { AnimeAiringStrip(entry, Modifier.padding(horizontal = 16.dp)) } },
        screen = if (animeSelected) {
            { snackbar -> AnimeUpdatesScreen(store, snackbar, { pills(false) }, airing) }
        } else {
            null
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnimeUpdatesPills(
    filter: UpdatesFilter,
    hideManga: Boolean,
    animeSelected: Boolean,
    twoPane: Boolean,
    onBookFilterSelected: (UpdatesFilter) -> Unit,
    onAnimeSelected: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = if (twoPane) 4.dp else 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!hideManga) {
            TsuzukiPill(
                text = stringResource(MR.strings.all),
                selected = !animeSelected && filter == UpdatesFilter.ALL,
                onClick = { onBookFilterSelected(UpdatesFilter.ALL) },
            )
            TsuzukiPill(
                text = stringResource(TDMR.strings.label_manga),
                selected = !animeSelected && filter == UpdatesFilter.MANGA,
                onClick = { onBookFilterSelected(UpdatesFilter.MANGA) },
            )
        }
        TsuzukiPill(
            text = stringResource(TDMR.strings.label_novels),
            selected = !animeSelected && filter == UpdatesFilter.NOVELS,
            onClick = { onBookFilterSelected(UpdatesFilter.NOVELS) },
        )
        TsuzukiPill(
            text = stringResource(MR.strings.tsz_anime_media),
            selected = animeSelected,
            onClick = onAnimeSelected,
        )
    }
}

@Composable
private fun AnimeUpdatesScreen(
    store: AnimeLibraryStore,
    snackbar: SnackbarHostState,
    pills: @Composable () -> Unit,
    airing: AnimeUpdateEpisode?,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.label_recent_updates),
                scrollBehavior = scrollBehavior,
                actions = {
                    AppBarActions(
                        listOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_update_library),
                                icon = Icons.Outlined.Refresh,
                                onClick = { AnimeLibraryUpdateJob.start(context) },
                            ),
                        ),
                    )
                },
            )
        },
        snackbarHost = { TsuzukiUndoSnackbarHost(snackbar) },
    ) { padding ->
        TsuzukiAnimeUpdates(store, padding, pills, airing)
    }
}
