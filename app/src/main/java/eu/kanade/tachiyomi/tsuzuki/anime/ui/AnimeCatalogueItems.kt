package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.tsuzuki.anime.openAnime
import eu.kanade.presentation.tsuzuki.browse.TsuzukiInLibraryBadge
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Grid cells for each catalogue display mode, as in the book catalogue (AP10). */
internal fun animeCatalogueColumns(mode: LibraryDisplayMode): GridCells = when (mode) {
    LibraryDisplayMode.List -> GridCells.Fixed(1)
    LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> GridCells.Adaptive(104.dp)
    LibraryDisplayMode.ComfortableGrid -> GridCells.Adaptive(112.dp)
}

/** URLs of this source's titles that are in the anime library, for the "in library" badge. */
@Composable
internal fun rememberAnimeLibraryUrls(store: AnimeLibraryStore, sourceId: Long): State<Set<String>> =
    remember(store, sourceId) {
        store.observeLibrary().map { entries ->
            entries.mapNotNullTo(hashSetOf()) { it.anime.url.takeIf { _ -> it.anime.source == sourceId } }
        }
    }.collectAsState(emptySet())

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AnimeCatalogueItem(
    source: AnimeSource,
    anime: SAnime,
    mode: LibraryDisplayMode,
    inLibrary: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val haptic = LocalHapticFeedback.current
    val clickable = Modifier.combinedClickable(
        role = Role.Button,
        onClick = onClick,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongClick()
        },
    )
    // Covers of titles already in the library dim, as in the book catalogue.
    val coverAlpha = if (inLibrary) 0.38f else 1f
    when (mode) {
        LibraryDisplayMode.List -> Row(
            Modifier.fillMaxWidth().height(56.dp).then(clickable).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.padding(vertical = 4.dp).fillMaxHeight().aspectRatio(2f / 3f).clip(RoundedCornerShape(4.dp))) {
                AnimeCover(source, anime, Modifier.fillMaxWidth().graphicsAlpha(coverAlpha))
            }
            Text(
                text = tsuzukiMenuLabel(anime.title),
                modifier = Modifier.weight(1f),
                color = tokens.dotOn,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (inLibrary) TsuzukiInLibraryBadge()
        }
        LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> Box(clickable) {
            AnimeCover(source, anime, Modifier.fillMaxWidth().graphicsAlpha(coverAlpha))
            if (mode == LibraryDisplayMode.CompactGrid) {
                Box(
                    Modifier.matchParentSize().clip(RoundedCornerShape(6.dp)).background(
                        Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.8f)),
                    ),
                )
                Text(
                    text = tsuzukiMenuLabel(anime.title),
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (inLibrary) Box(Modifier.align(Alignment.TopStart).padding(4.dp)) { TsuzukiInLibraryBadge() }
        }
        LibraryDisplayMode.ComfortableGrid -> Column(clickable, horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                AnimeCover(source, anime, Modifier.fillMaxWidth().graphicsAlpha(coverAlpha))
                if (inLibrary) Box(Modifier.align(Alignment.TopStart).padding(4.dp)) { TsuzukiInLibraryBadge() }
            }
            Text(
                text = tsuzukiMenuLabel(anime.title),
                modifier = Modifier.padding(4.dp),
                color = tokens.dotOn,
                style = MaterialTheme.typography.titleSmall,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun Modifier.graphicsAlpha(alpha: Float): Modifier =
    if (alpha >= 1f) this else alpha(alpha)

/** The book catalogue's display menu: comfortable grid, compact grid, list, cover-only grid. */
@Composable
internal fun AnimeCatalogueDisplayMenu(
    expanded: Boolean,
    current: LibraryDisplayMode,
    onDismiss: () -> Unit,
    onSelect: (LibraryDisplayMode) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        listOf(
            LibraryDisplayMode.ComfortableGrid to MR.strings.action_display_comfortable_grid,
            LibraryDisplayMode.CompactGrid to MR.strings.action_display_grid,
            LibraryDisplayMode.List to MR.strings.action_display_list,
            LibraryDisplayMode.CoverOnlyGrid to MR.strings.action_display_cover_only_grid,
        ).forEach { (mode, label) ->
            DropdownMenuItem(
                text = { Text(tsuzukiMenuLabel(stringResource(label))) },
                trailingIcon = if (mode == current) {
                    { Icon(Icons.Outlined.Check, contentDescription = null) }
                } else {
                    null
                },
                onClick = {
                    onDismiss()
                    onSelect(mode)
                },
            )
        }
    }
}

/**
 * Long-press on a catalogue result (AP10): adds it to the library through the duplicate check and the shelf
 * picker, or, when it is already saved, asks before removing it. Same steps as the details screen's button.
 */
internal class AnimeCatalogueLongPress {
    var duplicate by mutableStateOf<Triple<Anime, Anime, SAnime>?>(null)
    var shelves by mutableStateOf<Pair<SAnime, Set<Long>>?>(null)
    var removing by mutableStateOf<SAnime?>(null)
}

@Composable
internal fun rememberAnimeCatalogueLongPress(
    source: AnimeSource,
    store: AnimeLibraryStore,
): Pair<AnimeCatalogueLongPress, (SAnime, Boolean) -> Unit> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { AnimeCatalogueLongPress() }
    val onLongPress: (SAnime, Boolean) -> Unit = { anime, inLibrary ->
        if (inLibrary) {
            state.removing = anime
        } else {
            scope.launch {
                runCatalogueAction(context) {
                    val id = store.cacheSearchResult(source.id, anime)
                    val adding = checkNotNull(store.getAnime(id))
                    val existing = store.findDuplicate(adding)
                    if (existing == null) {
                        state.shelves = anime to store.shelfIds(id)
                    } else {
                        state.duplicate = Triple(existing, adding, anime)
                    }
                }
            }
        }
    }
    return state to onLongPress
}

@Composable
internal fun AnimeCatalogueLongPressDialogs(
    state: AnimeCatalogueLongPress,
    source: AnimeSource,
    store: AnimeLibraryStore,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shelves by remember(store) { store.observeShelves() }.collectAsState(emptyList())
    state.duplicate?.let { (existing, adding, show) ->
        AnimeDuplicateDialog(
            existing = existing,
            adding = adding,
            addingSource = source,
            onDismiss = { state.duplicate = null },
            onAdd = {
                state.duplicate = null
                scope.launch {
                    runCatalogueAction(context) { state.shelves = show to store.shelfIds(adding.id) }
                }
            },
            onOpen = {
                state.duplicate = null
                openAnime(context, existing.id)
            },
            onMigrate = {
                state.duplicate = null
                context.startActivity(AnimeMigrationActivity.fromAnime(context, existing.id, adding.id))
            },
        )
    }
    state.shelves?.let { (anime, initial) ->
        AnimeShelfPicker(shelves, initial, onDismiss = { state.shelves = null }) { selected ->
            state.shelves = null
            scope.launch { runCatalogueAction(context) { store.setFavorite(source.id, anime, true, selected) } }
        }
    }
    state.removing?.let { anime ->
        AlertDialog(
            onDismissRequest = { state.removing = null },
            title = { Text(tsuzukiMenuLabel(anime.title)) },
            text = { Text(stringResource(MR.strings.remove_manga, anime.title)) },
            confirmButton = {
                TextButton(onClick = {
                    state.removing = null
                    scope.launch {
                        runCatalogueAction(context) { store.setFavorite(source.id, anime, false, emptySet()) }
                    }
                }) { Text(stringResource(MR.strings.action_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { state.removing = null }) { Text(stringResource(MR.strings.action_cancel)) }
            },
        )
    }
}

private suspend fun runCatalogueAction(context: Context, block: suspend () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AnimeLog.w("Anime catalogue library action failed", e)
        context.toast(MR.strings.unknown_error)
    }
}
