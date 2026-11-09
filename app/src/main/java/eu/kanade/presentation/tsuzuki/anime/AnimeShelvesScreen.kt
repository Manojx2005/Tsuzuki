package eu.kanade.presentation.tsuzuki.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.components.CategoryDeleteDialog
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.category.anime.model.AnimeCategory
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class AnimeShelvesScreen : Screen() {
    @Composable
    override fun Content() {
        if (!TsuzukiAnime.enabled) return
        val navigator = LocalNavigator.currentOrThrow
        val store = remember { Injekt.get<AnimeLibraryStore>() }
        val shelves by remember { store.observeShelves() }.collectAsState(emptyList())
        val library by remember { store.observeLibrary() }.collectAsState(emptyList())
        val scope = rememberCoroutineScope()
        val snackbars = remember { SnackbarHostState() }
        val error = stringResource(MR.strings.unknown_error)
        val tokens = TsuzukiTheme.tokens
        var editing by remember { mutableStateOf(false) }
        var rename by remember { mutableStateOf<AnimeCategory?>(null) }
        var delete by remember { mutableStateOf<AnimeCategory?>(null) }
        val displayed = remember { mutableStateListOf<AnimeCategory>() }
        val listState = rememberLazyListState()
        val order = rememberReorderableLazyListState(listState) { from, to ->
            val source = displayed.indexOfFirst { it.id == from.key }
            val target = displayed.indexOfFirst { it.id == to.key }
            if (source <= 0 || target <= 0) return@rememberReorderableLazyListState
            displayed.add(target, displayed.removeAt(source))
            store.reorderShelves(displayed.map(AnimeCategory::id))
        }
        LaunchedEffect(shelves, order.isAnyItemDragging) {
            if (!order.isAnyItemDragging) {
                displayed.clear()
                displayed.addAll(shelves)
            }
        }
        val edit: (suspend () -> Unit) -> Unit = { action ->
            scope.launch {
                try {
                    action()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    snackbars.showSnackbar(error)
                }
            }
        }
        Scaffold(
            modifier = Modifier.tsuzukiBackground(tokens),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbars) },
            topBar = { AppBar(stringResource(MR.strings.tsz_anime_shelves), navigateUp = navigator::pop) },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                Text(
                    tsuzukiMenuLabel(stringResource(MR.strings.tsz_anime_shelves_hint)),
                    modifier = Modifier.padding(bottom = 12.dp),
                    color = tokens.mutedText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                ) {
                    itemsIndexed(displayed, key = { _, shelf -> shelf.id }) { index, shelf ->
                        ReorderableItem(order, shelf.id) {
                            var menu by remember { mutableStateOf(false) }
                            val top = if (index == 0) tokens.tileRadius else 0.dp
                            val bottom = if (index == displayed.lastIndex) tokens.tileRadius else 0.dp
                            val shape = RoundedCornerShape(top, top, bottom, bottom)
                            Column(Modifier.fillMaxWidth().clip(shape).background(tokens.tile)) {
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (!shelf.isSystem) {
                                        Icon(
                                            Icons.Outlined.DragIndicator,
                                            null,
                                            tint = tokens.mutedText,
                                            modifier = Modifier.draggableHandle().padding(end = 8.dp),
                                        )
                                    } else {
                                        Spacer(Modifier.width(32.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                tsuzukiMenuLabel(shelf.displayName()),
                                                color = tokens.dotOn,
                                                style = MaterialTheme.typography.titleSmall,
                                            )
                                            if (shelf.isSystem) {
                                                Text(
                                                    tsuzukiMenuLabel(stringResource(MR.strings.theme_system)),
                                                    color = tokens.accent,
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                            }
                                        }
                                        Text(
                                            tsuzukiMenuLabel(
                                                stringResource(
                                                    MR.strings.tsz_anime_shelf_count,
                                                    library.count { shelf.id in it.shelfIds },
                                                ),
                                            ),
                                            color = tokens.mutedText,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                    if (!shelf.isSystem) {
                                        IconButton(onClick = { menu = true }) {
                                            Icon(Icons.Outlined.MoreVert, stringResource(MR.strings.action_menu))
                                        }
                                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                            TsuzukiMenuItem(
                                                stringResource(MR.strings.action_rename_category),
                                                onClick = {
                                                    menu = false
                                                    rename = shelf
                                                    editing = true
                                                },
                                            )
                                            TsuzukiMenuItem(stringResource(MR.strings.action_delete), onClick = {
                                                menu = false
                                                delete = shelf
                                            }, danger = true)
                                        }
                                    }
                                }
                                if (index < displayed.lastIndex) {
                                    HorizontalDivider(color = tokens.tileBorder ?: tokens.track)
                                }
                            }
                        }
                    }
                }
                Button(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.accent,
                        contentColor = tokens.onAccent,
                    ),
                    onClick = {
                        rename = null
                        editing = true
                    },
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(tsuzukiMenuLabel(stringResource(MR.strings.tsz_anime_new_shelf)))
                }
            }
        }
        if (editing) {
            val target = rename
            var name by remember(target) { mutableStateOf(target?.name.orEmpty()) }
            val available = name.isNotBlank() &&
                shelves.none { it.id != target?.id && it.name.equals(name.trim(), ignoreCase = true) }
            AlertDialog(
                onDismissRequest = { editing = false },
                title = {
                    val title = if (target == null) {
                        MR.strings.tsz_anime_new_shelf
                    } else {
                        MR.strings.action_rename_category
                    }
                    Text(stringResource(title))
                },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(MR.strings.name)) },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(enabled = available, onClick = {
                        editing = false
                        edit { store.saveShelf(target, name) }
                    }) { Text(stringResource(MR.strings.action_save)) }
                },
                dismissButton = {
                    TextButton(onClick = { editing = false }) { Text(stringResource(MR.strings.action_cancel)) }
                },
            )
        }
        delete?.let { target ->
            CategoryDeleteDialog(
                onDismissRequest = { delete = null },
                onDelete = { edit { store.deleteShelf(target.id) } },
                category = target.name,
            )
        }
    }
}
