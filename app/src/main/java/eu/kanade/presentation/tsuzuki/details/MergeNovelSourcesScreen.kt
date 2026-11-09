package eu.kanade.presentation.tsuzuki.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTokens
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.tsuzuki.novel.MergedNovelChapter
import eu.kanade.tachiyomi.tsuzuki.novel.MergedNovelSources
import eu.kanade.tachiyomi.tsuzuki.novel.NovelEdition
import eu.kanade.tachiyomi.tsuzuki.novel.mergeNovelChapters
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import logcat.LogPriority
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun mergeNovelSourcesAction(book: Manga): AppBar.OverflowAction? {
    if (!book.isNovel || !book.favorite || TsuzukiTheme.tokens.style == TsuzukiStyle.NONE) return null
    val navigator = LocalNavigator.currentOrThrow
    return AppBar.OverflowAction(
        title = stringResource(MR.strings.tsz_merge_sources),
        onClick = { navigator.push(MergeNovelSourcesScreen(book)) },
    )
}

private class MergeNovelSourcesScreen(private val book: Manga) : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val tokens = TsuzukiTheme.tokens
        val service = remember { Injekt.get<MergedNovelSources>() }
        val sourceManager = remember { Injekt.get<SourceManager>() }
        val state by remember(service, book) {
            service.observe(book).map { editions ->
                MergeScreenState(editions, mergeNovelChapters(editions))
            }.flowOn(Dispatchers.Default)
        }.collectAsState(initial = null)
        val scope = rememberCoroutineScope()
        var saving by remember { mutableStateOf(false) }
        var saveFailed by remember { mutableStateOf(false) }
        var adding by rememberSaveable { mutableStateOf(false) }
        var showingChapters by rememberSaveable { mutableStateOf(false) }
        val snapshot = state
        val editions = snapshot?.editions.orEmpty()
        val books = remember(editions) { editions.map(NovelEdition::manga) }
        val hasBook = books.any { it.id == book.id }
        val sources = remember(books) {
            books.associate { it.id to sourceManager.getOrStub(it.source).name }
        }
        var draft by remember(books) { mutableStateOf<List<Manga>?>(null) }

        fun save(members: List<Manga>) {
            if (saving) return
            saving = true
            saveFailed = false
            scope.launch {
                try {
                    service.replace(book, books, members)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    logcat(LogPriority.WARN, error) { "Could not save linked novel sources" }
                    saveFailed = true
                } finally {
                    draft = null
                    saving = false
                }
            }
        }

        fun move(fromId: Long, toId: Long) {
            val ordered = draft ?: books
            val from = ordered.indexOfFirst { it.id == fromId }
            val to = ordered.indexOfFirst { it.id == toId }
            if (!saving && from >= 0 && to >= 0 && from != to) {
                draft = ordered.toMutableList().apply { add(to, removeAt(from)) }
            }
        }

        BackHandler(enabled = showingChapters) { showingChapters = false }
        Column(Modifier.fillMaxSize().tsuzukiBackground(tokens).navigationBarsPadding()) {
            AppBar(
                title = stringResource(if (showingChapters) MR.strings.tsz_chapters else MR.strings.tsz_merge_sources),
                subtitle = book.title,
                backgroundColor = tokens.background,
                navigateUp = { if (showingChapters) showingChapters = false else navigator.pop() },
            )
            when {
                snapshot == null -> MergeStatus(stringResource(MR.strings.loading))
                !hasBook -> MergeStatus(stringResource(MR.strings.tsz_merge_library_required))
                showingChapters -> {
                    val chapters = snapshot.chapters
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (chapters.isEmpty()) item { MergeStatus(stringResource(MR.strings.no_chapters_error)) }
                        items(chapters, key = { it.preferred.id }) { row ->
                            val chapter = row.preferred
                            TsuzukiTile(
                                modifier = Modifier.fillMaxWidth().tsuzukiClickable(role = Role.Button) {
                                    context.startActivity(
                                        ReaderActivity.newIntent(context, chapter.mangaId, chapter.id),
                                    )
                                },
                                padding = 12.dp,
                            ) {
                                Text(
                                    tokens.case(chapter.name),
                                    color = if (row.read) tokens.mutedText else tokens.dotOn,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                MergeStatus(sources[chapter.mangaId].orEmpty())
                            }
                        }
                    }
                }
                else -> {
                    val order = draft ?: books
                    val listState = rememberLazyListState()
                    val reorder = rememberReorderableLazyListState(listState) { from, to ->
                        val fromId = from.key as? Long ?: return@rememberReorderableLazyListState
                        val toId = to.key as? Long ?: return@rememberReorderableLazyListState
                        move(fromId, toId)
                    }
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item("combined") {
                            val chapterCount = snapshot.chapters.size
                            val primary = books.first().id
                            val fallback = snapshot.chapters.count { it.preferred.mangaId != primary }
                            TsuzukiTile(
                                Modifier.fillMaxWidth().tsuzukiClickable(role = Role.Button) { showingChapters = true },
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    TsuzukiSectionLabel(stringResource(MR.strings.tsz_merge_combined))
                                    Text(
                                        chapterCount.toString(),
                                        color = tokens.accent,
                                        style = MaterialTheme.typography.headlineMedium,
                                    )
                                }
                                ChapterCoverage(snapshot.chapters) { row ->
                                    if (row.preferred.mangaId ==
                                        primary
                                    ) {
                                        1
                                    } else {
                                        2
                                    }
                                }
                                MergeStatus(stringResource(MR.strings.tsz_merge_other_count, fallback))
                            }
                        }
                        item("priority") { TsuzukiSectionLabel(stringResource(MR.strings.tsz_merge_priority)) }
                        itemsIndexed(order, key = { _, member -> member.id }) { index, member ->
                            ReorderableItem(reorder, key = member.id) { _ ->
                                val up = stringResource(MR.strings.tsz_query_move_up)
                                val down = stringResource(MR.strings.tsz_query_move_down)
                                val handle = Modifier
                                    .draggableHandle(enabled = !saving, onDragStopped = { draft?.let(::save) })
                                    .semantics {
                                        customActions = listOfNotNull(
                                            order.getOrNull(index - 1)?.let { target ->
                                                CustomAccessibilityAction(up) {
                                                    move(member.id, target.id)
                                                    draft?.let(::save)
                                                    true
                                                }
                                            },
                                            order.getOrNull(index + 1)?.let { target ->
                                                CustomAccessibilityAction(down) {
                                                    move(member.id, target.id)
                                                    draft?.let(::save)
                                                    true
                                                }
                                            },
                                        )
                                    }
                                TsuzukiTile(Modifier.fillMaxWidth(), padding = 12.dp) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.DragIndicator,
                                            null,
                                            handle.padding(8.dp),
                                            tint = tokens.mutedText,
                                        )
                                        Text((index + 1).toString(), style = MaterialTheme.typography.titleLarge)
                                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                            Text(
                                                tokens.case(sources[member.id].orEmpty()),
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                            val chapterCount = editions.first { it.manga.id == member.id }.chapters.size
                                            MergeStatus(stringResource(MR.strings.tsz_download_count, chapterCount))
                                        }
                                        if (member.id != book.id) {
                                            IconButton(onClick = { save(order - member) }, enabled = !saving) {
                                                Icon(Icons.Outlined.Close, stringResource(MR.strings.action_remove))
                                            }
                                        }
                                    }
                                    ChapterCoverage(snapshot.chapters) { row ->
                                        if (row.copies.any { it.mangaId == member.id }) {
                                            (
                                                if (index ==
                                                    0
                                                ) {
                                                    1
                                                } else {
                                                    2
                                                }
                                                )
                                        } else {
                                            0
                                        }
                                    }
                                }
                            }
                        }
                        item("add") {
                            Row(
                                Modifier.fillMaxWidth()
                                    .border(1.dp, tokens.track, RoundedCornerShape(tokens.tileRadius))
                                    .tsuzukiClickable(enabled = !saving, role = Role.Button) { adding = true }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Outlined.Add, null)
                                Text(tokens.case(stringResource(MR.strings.tsz_merge_add)))
                            }
                        }
                        if (saveFailed) item("error") { MergeStatus(stringResource(MR.strings.tsz_merge_save_error)) }
                    }
                }
            }
        }
        if (adding && hasBook) {
            EditionPicker(book, books, service, sourceManager, onDismiss = { adding = false }) { candidate ->
                adding = false
                save(books + candidate)
            }
        }
    }
}

private data class MergeScreenState(val editions: List<NovelEdition>, val chapters: List<MergedNovelChapter>)

private fun TsuzukiTokens.case(text: String) = if (isDot) text.uppercase() else text

@Composable
private fun MergeStatus(text: String) {
    val tokens = TsuzukiTheme.tokens
    Text(tokens.case(text), color = tokens.mutedText, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ChapterCoverage(chapters: List<MergedNovelChapter>, coverage: (MergedNovelChapter) -> Int) {
    val tokens = TsuzukiTheme.tokens
    val count = minOf(chapters.size, 40)
    if (count == 0) return
    val colours = List(count) { index ->
        when (coverage(chapters[index * chapters.size / count])) {
            1 -> if (tokens.isDot) tokens.dotOn else tokens.accent
            2 -> if (tokens.isDot) tokens.accent else tokens.dotOn
            else -> tokens.track
        }
    }
    Canvas(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(12.dp)) {
        val pitch = size.width / colours.size
        colours.forEachIndexed { index, colour ->
            drawRoundRect(colour, Offset(index * pitch, 0f), Size(pitch * .76f, size.height), CornerRadius(2.dp.toPx()))
        }
    }
}

@Composable
private fun EditionPicker(
    book: Manga,
    books: List<Manga>,
    service: MergedNovelSources,
    sourceManager: SourceManager,
    onDismiss: () -> Unit,
    onSelect: (Manga) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    var query by rememberSaveable { mutableStateOf("") }
    val choices by produceState<List<Manga>?>(null, book, books) {
        value = try {
            service.candidates(book, books).sortedWith(
                compareByDescending<Manga> {
                    it.title.equals(book.title, ignoreCase = true)
                }.thenBy { it.title.lowercase() },
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logcat(LogPriority.WARN, error) { "Could not find other saved novel editions" }
            emptyList()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tokens.case(stringResource(MR.strings.tsz_merge_add))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tokens.case(stringResource(MR.strings.tsz_search))) },
                    singleLine = true,
                )
                val candidates = choices
                when {
                    candidates == null -> MergeStatus(stringResource(MR.strings.loading))
                    candidates.isEmpty() -> MergeStatus(stringResource(MR.strings.tsz_merge_library_required))
                    else -> LazyColumn(Modifier.heightIn(max = 280.dp)) {
                        items(
                            candidates.filter {
                                it.title.contains(query.trim(), ignoreCase = true)
                            },
                            key = Manga::id,
                        ) { choice ->
                            Column(
                                Modifier.fillMaxWidth().tsuzukiClickable(role = Role.Button) {
                                    onSelect(choice)
                                }.padding(8.dp),
                            ) {
                                Text(tokens.case(choice.title), style = MaterialTheme.typography.titleSmall)
                                MergeStatus(sourceManager.getOrStub(choice.source).name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tokens.case(stringResource(MR.strings.action_cancel))) }
        },
    )
}
