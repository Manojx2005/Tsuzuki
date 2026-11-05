package eu.kanade.presentation.tsuzuki.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.tsuzuki.settings.tsuzukiDialogColor
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.tsuzuki.LibraryQuery
import eu.kanade.tachiyomi.tsuzuki.LibraryQuerySnapshot
import eu.kanade.tachiyomi.tsuzuki.LibraryQueryStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.presentation.core.util.collectAsState as collectPrefAsState

/** The library filter/sort sources a query reads and writes, resolved once per composition. */
private class QueryEnv {
    val prefs: LibraryPreferences = Injekt.get()
    val store = LibraryQueryStore(Injekt.get<PreferenceStore>())
    val trackerIds: List<Int> = Injekt.get<TrackerManager>().loggedInTrackers().map { it.id.toInt() }

    fun apply(query: LibraryQuery) = LibraryQuerySnapshot.apply(prefs, query, trackerIds)
    fun clear() = LibraryQuerySnapshot.clear(prefs, trackerIds)
    fun current() = LibraryQuerySnapshot.capture(prefs, trackerIds)
}

/**
 * Saved queries as pills under the library header (board 08-tools "LibraryQuery"). Tapping one writes all of its
 * filters and its sort at once; the pill matching the current filters is the accent one; Clear restores the
 * defaults. Nothing shows until a query has been saved.
 */
@Composable
fun TsuzukiLibraryQueryPills(onApplied: () -> Unit = {}) {
    val env = remember { QueryEnv() }
    val queries by env.store.preference.collectPrefAsState()
    val list = remember(queries) { LibraryQueryStore.decode(queries) }
    if (list.isEmpty()) return
    val current by remember { LibraryQuerySnapshot.changes(env.prefs, env.trackerIds) }
        .collectAsState(initial = env.current())
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(list, key = { it.id }, contentType = { "query" }) { query ->
            TsuzukiPill(
                text = query.name,
                selected = query.content() == current,
                onClick = {
                    onApplied()
                    env.apply(query)
                },
                role = Role.Button,
            )
        }
        item(key = "clear") {
            TsuzukiPill(
                text = stringResource(MR.strings.tsz_query_pill_clear),
                selected = false,
                onClick = {
                    onApplied()
                    env.clear()
                },
                role = Role.Button,
            )
        }
    }
}

/** Number of filters a query turns on (each tri-state, each tag, each extension). */
internal fun LibraryQuery.activeFilterCount(): Int =
    triStates.size + includedTags.size + excludedTags.size + filterExtensions.size + excludedExtensions.size

/**
 * Manage screen: save the current view, apply, rename, reorder and delete. Applying a query goes back to the library.
 */
@Composable
fun TsuzukiLibraryQueriesScreen(onBack: () -> Unit) {
    val t = TsuzukiTheme.tokens
    val env = remember { QueryEnv() }
    val raw by env.store.preference.collectPrefAsState()
    val list = remember(raw) { LibraryQueryStore.decode(raw) }
    var naming by remember { mutableStateOf<NameTarget?>(null) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    LazyColumn(
        Modifier
            .fillMaxSize()
            .tsuzukiBackground(t),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            Row(
                Modifier
                    .statusBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(t.tile)
                        .then(t.tileBorder?.let { Modifier.border(1.dp, it, CircleShape) } ?: Modifier)
                        .tsuzukiClickable(role = Role.Button, onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(MR.strings.action_cancel),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        t.case(stringResource(MR.strings.tsz_query_title)),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp, lineHeight = 36.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        t.case(stringResource(MR.strings.tsz_query_subtitle)),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = muted,
                    )
                }
            }
        }
        item(key = "save") {
            val shape = RoundedCornerShape(if (t.isDot) 8.dp else 28.dp)
            Box(
                Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(shape)
                    .background(t.accent)
                    .tsuzukiClickable(role = Role.Button) { naming = NameTarget.Save },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    t.case(stringResource(MR.strings.tsz_query_save_current)),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = t.onAccent,
                )
            }
        }
        if (list.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(MR.strings.tsz_query_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        items(list, key = { it.id }, contentType = { "query" }) { query ->
            val index = list.indexOf(query)
            QueryRow(
                query = query,
                canMoveUp = index > 0,
                canMoveDown = index < list.lastIndex,
                onApply = {
                    env.apply(query)
                    onBack()
                },
                onRename = { naming = NameTarget.Rename(query) },
                onDelete = { env.store.delete(query.id) },
                onMove = { env.store.move(query.id, it) },
            )
        }
    }

    naming?.let { target ->
        NameDialog(
            initial = (target as? NameTarget.Rename)?.query?.name.orEmpty(),
            onDismiss = { naming = null },
            onConfirm = { name ->
                when (target) {
                    NameTarget.Save -> env.store.add(name, env.current())
                    is NameTarget.Rename -> env.store.rename(target.query.id, name)
                }
                naming = null
            },
        )
    }
}

private sealed interface NameTarget {
    data object Save : NameTarget
    data class Rename(val query: LibraryQuery) : NameTarget
}

@Composable
private fun QueryRow(
    query: LibraryQuery,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onApply: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(if (t.isDot) 8.dp else 24.dp)
    val count = query.activeFilterCount()
    val summary = (
        if (count == 0) {
            stringResource(MR.strings.tsz_query_no_filters)
        } else {
            stringResource(MR.strings.tsz_query_filters_count, count)
        }
        ) + " · " + sortLabel(query)
    Column(
        Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(t.tile)
            .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .tsuzukiClickable(role = Role.Button, onClick = onApply)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                t.case(query.name),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                t.case(summary),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), horizontalArrangement = Arrangement.End) {
            RowIcon(Icons.Outlined.ArrowUpward, stringResource(MR.strings.tsz_query_move_up), canMoveUp) { onMove(-1) }
            RowIcon(
                Icons.Outlined.ArrowDownward,
                stringResource(MR.strings.tsz_query_move_down),
                canMoveDown,
            ) { onMove(1) }
            RowIcon(Icons.Outlined.Edit, stringResource(MR.strings.tsz_query_rename), true, onClick = onRename)
            RowIcon(
                Icons.Outlined.Delete,
                stringResource(MR.strings.tsz_query_delete),
                true,
                danger = true,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun RowIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (danger) TsuzukiTheme.tokens.accent else MaterialTheme.colorScheme.onSurface
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .tsuzukiClickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint.copy(alpha = if (enabled) 1f else 0.38f))
    }
}

/** The sort as its short label; reuses the stock sort strings. */
@Composable
private fun sortLabel(query: LibraryQuery): String {
    val sort = LibrarySort.deserialize(query.sort)
    val res = when (sort.type) {
        LibrarySort.Type.Alphabetical -> MR.strings.action_sort_alpha
        LibrarySort.Type.LastRead -> MR.strings.action_sort_last_read
        LibrarySort.Type.LastUpdate -> MR.strings.action_sort_last_manga_update
        LibrarySort.Type.UnreadCount -> MR.strings.action_sort_unread_count
        LibrarySort.Type.TotalChapters -> MR.strings.action_sort_total
        LibrarySort.Type.LatestChapter -> MR.strings.action_sort_latest_chapter
        LibrarySort.Type.ChapterFetchDate -> MR.strings.action_sort_chapter_fetch_date
        LibrarySort.Type.DateAdded -> MR.strings.action_sort_date_added
        LibrarySort.Type.TrackerMean -> MR.strings.action_sort_tracker_score
        LibrarySort.Type.DownloadedChapters -> MR.strings.downloaded_chapters
        LibrarySort.Type.SourceName -> TDMR.strings.action_sort_source_name
        LibrarySort.Type.Random -> MR.strings.action_sort_random
    }
    return stringResource(res) + if (sort.isAscending) " ↑" else " ↓"
}

@Composable
private fun NameDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tsuzukiDialogColor() ?: MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(stringResource(MR.strings.tsz_query_name_label)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text(stringResource(MR.strings.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}

private fun eu.kanade.presentation.theme.tsuzuki.TsuzukiTokens.case(s: String) = if (isDot) s.uppercase() else s
