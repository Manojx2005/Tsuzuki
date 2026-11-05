package eu.kanade.presentation.tsuzuki.details

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSegments
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.tsuzuki.reader.TsuzukiReadingTypography
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.ui.reader.quote.Quote
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** The quotes of one chapter, in the order they should be read. */
internal data class NotebookGroup(
    val chapterName: String,
    /** The chapter the quotes were taken from, when it can be found in the book's chapter list. */
    val chapterId: Long?,
    val quotes: List<Quote>,
)

internal enum class NotebookSort { CHAPTER, NEWEST }

/**
 * Finds the chapter a quote came from. Quotes store only the chapter name, so match it exactly first and then
 * ignoring case and surrounding spaces.
 */
internal fun resolveQuoteChapterId(chapterName: String, chapters: List<Chapter>): Long? {
    val name = chapterName.trim()
    if (name.isEmpty()) return null
    return chapters.firstOrNull { it.name == chapterName }?.id
        ?: chapters.firstOrNull { it.name.trim().equals(name, ignoreCase = true) }?.id
}

/**
 * Groups [quotes] by chapter.
 *
 * [NotebookSort.CHAPTER]: chapters in reading order (oldest first; chapters that can't be found go last), quotes
 * inside a chapter by paragraph, then by the time they were saved.
 * [NotebookSort.NEWEST]: one group per chapter name, the chapter with the most recent quote first, and newest
 * quote first inside it.
 */
internal fun groupNotebookQuotes(
    quotes: List<Quote>,
    chapters: List<Chapter>,
    sort: NotebookSort,
): List<NotebookGroup> {
    // Reading order: highest source order is the oldest chapter.
    val order = chapters.sortedByDescending { it.sourceOrder }.withIndex().associate { it.value.id to it.index }
    val groups = quotes.groupBy { it.chapterName.trim() }.map { (name, list) ->
        val id = resolveQuoteChapterId(name, chapters)
        NotebookGroup(chapterName = name, chapterId = id, quotes = list)
    }
    return when (sort) {
        NotebookSort.CHAPTER ->
            groups
                .map {
                    it.copy(
                        quotes = it.quotes.sortedWith(
                            compareBy<Quote> { q ->
                                q.paragraphIndex ?: Int.MAX_VALUE
                            }.thenBy { q -> q.timestamp },
                        ),
                    )
                }
                .sortedWith(
                    compareBy<NotebookGroup> { it.chapterId?.let(order::get) ?: Int.MAX_VALUE }
                        .thenBy { g -> g.quotes.minOf { it.timestamp } },
                )
        NotebookSort.NEWEST ->
            groups
                .map { it.copy(quotes = it.quotes.sortedByDescending { q -> q.timestamp }) }
                .sortedByDescending { g -> g.quotes.first().timestamp }
    }
}

/** Plain text for sharing every highlight: the book title, then each quote with its chapter. */
internal fun notebookShareText(title: String, groups: List<NotebookGroup>): String = buildString {
    append(title)
    groups.forEach { group ->
        group.quotes.forEach { quote ->
            append("\n\n")
            append(quote.content)
            if (group.chapterName.isNotEmpty()) append("\n— ").append(group.chapterName)
        }
    }
}

/**
 * The Notebook board (`06-reader/Notes`): the book's note on top, then every quote saved for the book. Glossary
 * and bookmarks from the board are left out (not stored / separate list).
 */
@Composable
internal fun TsuzukiNotebook(
    title: String,
    notes: String,
    quotes: List<Quote>,
    chapters: List<Chapter>,
    navigateUp: () -> Unit,
    onEditNote: () -> Unit,
    onOpenChapter: (chapterId: Long) -> Unit,
    onDeleteQuote: (Quote) -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val context = LocalContext.current
    var sortIndex by rememberSaveable { mutableIntStateOf(0) }
    val sort = NotebookSort.entries[sortIndex]
    val groups = remember(quotes, chapters, sort) { groupNotebookQuotes(quotes, chapters, sort) }
    val muted = t.dotOn.copy(alpha = 0.65f)
    val note = notes.trim()
    val shareLabel = stringResource(MR.strings.action_share)

    Column(
        Modifier
            .fillMaxSize()
            .tsuzukiBackground(t)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleButton(
                Icons.AutoMirrored.Outlined.ArrowBack,
                stringResource(MR.strings.action_bar_up_description),
                navigateUp,
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    tsuzukiCase(stringResource(MR.strings.tsz_notebook_title)).orEmpty(),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp, lineHeight = 40.sp),
                    color = t.dotOn,
                )
                Text(
                    tsuzukiCase(title).orEmpty(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (groups.isNotEmpty()) {
                CircleButton(Icons.Outlined.Share, shareLabel) {
                    val intent = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, notebookShareText(title, groups))
                    context.startActivity(Intent.createChooser(intent, null))
                }
            }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "note") {
                TsuzukiTile(Modifier.fillMaxWidth(), padding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            tsuzukiCase(stringResource(MR.strings.tsz_notebook_note)).orEmpty(),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = t.accent,
                        )
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .tsuzukiClickable(role = Role.Button, onClick = onEditNote)
                                .height(48.dp)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Edit, null, tint = t.accent, modifier = Modifier.size(18.dp))
                            Text(
                                tsuzukiCase(stringResource(MR.strings.tsz_notebook_edit)).orEmpty(),
                                modifier = Modifier.padding(start = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = t.accent,
                            )
                        }
                    }
                    Text(
                        if (note.isEmpty()) stringResource(MR.strings.tsz_notebook_no_note) else note,
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (note.isEmpty()) muted else t.dotOn,
                        maxLines = 12,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item(key = "sort") {
                TsuzukiSegments(
                    labels = listOf(
                        "${stringResource(MR.strings.tsz_notebook_highlights)} ${quotes.size}",
                        stringResource(MR.strings.tsz_notebook_newest),
                    ),
                    selected = sortIndex,
                    onClick = { sortIndex = it },
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            if (groups.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(MR.strings.tsz_notebook_no_quotes),
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted,
                    )
                }
            }
            groups.forEach { group ->
                items(group.quotes, key = { it.id }, contentType = { "quote" }) { quote ->
                    NotebookQuoteTile(
                        quote = quote,
                        chapterName = group.chapterName,
                        onOpen = group.chapterId?.let { id -> { onOpenChapter(id) } },
                        onDelete = { onDeleteQuote(quote) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotebookQuoteTile(
    quote: Quote,
    chapterName: String,
    onOpen: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val context = LocalContext.current
    val muted = t.dotOn.copy(alpha = 0.65f)
    val date = DateUtils.formatDateTime(context, quote.timestamp, DateUtils.FORMAT_SHOW_DATE)
    val header = listOf(chapterName, date).filter { it.isNotBlank() }.joinToString(" · ")
    val shareText = if (chapterName.isBlank()) quote.content else "${quote.content}\n\n$chapterName"
    val quoteLabel = stringResource(MR.strings.tsz_notebook_title)
    val open = if (onOpen != null) Modifier.tsuzukiClickable(role = Role.Button, onClick = onOpen) else Modifier
    TsuzukiTile(Modifier.fillMaxWidth().then(open), padding = 16.dp) {
        Text(
            tsuzukiCase(header).orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(Modifier.padding(top = 8.dp).height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(t.accent))
            TsuzukiReadingTypography {
                Text(
                    quote.content,
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = t.dotOn,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = {
                val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                context.startActivity(Intent.createChooser(intent, null))
            }) {
                Icon(Icons.Outlined.Share, stringResource(MR.strings.action_share), tint = muted)
            }
            IconButton(onClick = { context.copyToClipboard(quoteLabel, shareText) }) {
                Icon(Icons.Outlined.ContentCopy, stringResource(MR.strings.action_copy_to_clipboard), tint = muted)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, stringResource(MR.strings.action_delete), tint = muted)
            }
        }
    }
}

@Composable
private fun CircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val t = TsuzukiTheme.tokens
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(t.tile)
            .then(t.tileBorder?.let { Modifier.border(1.dp, it, CircleShape) } ?: Modifier)
            .tsuzukiClickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = t.dotOn)
    }
}
