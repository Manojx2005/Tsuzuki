package eu.kanade.presentation.tsuzuki.novel

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSegments
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.tachiyomi.data.download.DownloadManager
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.getChapterSort
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** One row of the contents drawer. [position] is the 1-based place in reading order. */
internal data class TsuzukiTocEntry(val chapter: Chapter, val position: Int, val downloaded: Boolean)

/** Filters by title, or by reading-order position when the query is a number. */
internal fun List<TsuzukiTocEntry>.filterToc(query: String, marksOnly: Boolean): List<TsuzukiTocEntry> {
    val trimmed = query.trim()
    return filter { entry ->
        (!marksOnly || entry.chapter.bookmark) &&
            (
                trimmed.isEmpty() ||
                    entry.chapter.name.contains(trimmed, ignoreCase = true) ||
                    trimmed.toIntOrNull() == entry.position
                )
    }
}

/**
 * The ReaderTOC board: a left drawer over the page with every chapter of the book.
 * Picking a chapter calls [onOpenChapter]; the caller reopens the reader on it.
 */
@Composable
fun TsuzukiReaderContents(
    visible: Boolean,
    manga: Manga?,
    currentChapterId: Long?,
    onDismissRequest: () -> Unit,
    onOpenChapter: (chapterId: Long) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    if (tokens.style == TsuzukiStyle.NONE || manga == null) return

    BackHandler(enabled = visible, onBack = onDismissRequest)

    // Reloaded each time the drawer opens, so read/downloaded marks are current.
    val entries by produceState<List<TsuzukiTocEntry>?>(null, manga.id, visible) {
        if (!visible) return@produceState
        value = withIOContext { loadToc(manga) }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    ),
            )
        }
        AnimatedVisibility(
            visible,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it },
        ) {
            BoxWithConstraints {
                val shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
                Column(
                    Modifier
                        .width(minOf(336.dp, maxWidth * 0.88f))
                        .fillMaxHeight()
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
                        // Swallow taps so they don't reach the scrim.
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                ) {
                    ContentsBody(
                        manga = manga,
                        entries = entries,
                        currentChapterId = currentChapterId,
                        onOpenChapter = onOpenChapter,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ContentsBody(
    manga: Manga,
    entries: List<TsuzukiTocEntry>?,
    currentChapterId: Long?,
    onOpenChapter: (Long) -> Unit,
    showHeading: Boolean = true,
) {
    val tokens = TsuzukiTheme.tokens
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(entries, tab, query) { entries.orEmpty().filterToc(query, marksOnly = tab == 1) }
    val listState = rememberLazyListState()

    // Open on the chapter being read, with a few rows of context above it.
    LaunchedEffect(entries) {
        val index = entries?.indexOfFirst { it.chapter.id == currentChapterId } ?: -1
        if (index > 0) listState.scrollToItem((index - 3).coerceAtLeast(0))
    }

    if (showHeading) {
        TsuzukiSectionLabel(
            manga.title,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp),
        )
        Text(
            stringResource(MR.strings.tsz_reader_contents).let { if (tokens.isDot) it.uppercase() else it },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp).semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 34.sp,
        )
    }
    TsuzukiSegments(
        labels = listOf(stringResource(MR.strings.tsz_reader_chapters), stringResource(MR.strings.tsz_reader_marks)),
        selected = tab,
        onClick = { tab = it },
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
    )
    JumpField(query, onQueryChange = { query = it })
    Spacer(Modifier.height(8.dp))
    when {
        entries == null -> Unit
        shown.isEmpty() -> Text(
            stringResource(if (tab == 1) MR.strings.tsz_reader_no_marks else MR.strings.no_results_found),
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(shown, key = { it.chapter.id }, contentType = { "chapter_row" }) { entry ->
                ChapterRow(entry, current = entry.chapter.id == currentChapterId) { onOpenChapter(entry.chapter.id) }
            }
        }
    }
}

@Composable
private fun JumpField(query: String, onQueryChange: (String) -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(22.dp)
    val ink = MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.padding(horizontal = 14.dp).fillMaxWidth().height(44.dp).clip(shape)
            .background(tokens.tile)
            .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.padding(start = 10.dp).weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    stringResource(MR.strings.tsz_reader_jump),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = ink),
                cursorBrush = SolidColor(tokens.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ChapterRow(entry: TsuzukiTocEntry, current: Boolean, onClick: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val read = entry.chapter.read && !current
    val ink = if (current) tokens.onAccent else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(14.dp))
            .then(if (current) Modifier.background(tokens.accent) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .alpha(if (read) 0.5f else 1f)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            entry.position.toString(),
            modifier = Modifier.width(48.dp),
            style = if (tokens.isDot) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleSmall,
            fontSize = 14.sp,
            color = ink,
            maxLines = 1,
        )
        Text(
            entry.chapter.name.let { if (tokens.isDot) it.uppercase() else it },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val mark = when {
            current -> "●"
            entry.downloaded -> "↓"
            entry.chapter.read -> "✓"
            else -> null
        }
        if (mark != null) {
            Text(
                mark,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    current -> tokens.onAccent
                    entry.downloaded -> tokens.accent
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

/** Every chapter in the book's own order (as on its details page), numbered by reading order. */
internal suspend fun loadToc(manga: Manga): List<TsuzukiTocEntry> {
    val chapters = Injekt.get<GetChaptersByMangaId>().await(manga.id, applyScanlatorFilter = true)
    val downloads = Injekt.get<DownloadManager>()
    val readingOrder = chapters.sortedWith(getChapterSort(manga, sortDescending = false))
    val positions = readingOrder.withIndex().associate { (index, chapter) -> chapter.id to index + 1 }
    return chapters.sortedWith(getChapterSort(manga)).map { chapter ->
        TsuzukiTocEntry(
            chapter = chapter,
            position = positions.getValue(chapter.id),
            downloaded = downloads.isChapterDownloaded(
                chapter.name,
                chapter.scanlator,
                chapter.url,
                manga.title,
                manga.source,
            ),
        )
    }
}
