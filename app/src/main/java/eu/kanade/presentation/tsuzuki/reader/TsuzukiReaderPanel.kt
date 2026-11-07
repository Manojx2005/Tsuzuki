package eu.kanade.presentation.tsuzuki.reader

import android.app.Activity
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSegments
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.tsuzuki.novel.ContentsBody
import eu.kanade.presentation.tsuzuki.novel.TsuzukiTocEntry
import eu.kanade.presentation.tsuzuki.novel.loadToc
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.quote.Quote
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

/** The docked panel needs a window about as wide as a tablet in landscape. */
internal const val READER_PANEL_MIN_WINDOW_DP = 900

internal fun shouldDockReaderPanel(tabletUi: Boolean, windowWidthDp: Int): Boolean =
    tabletUi && windowWidthDp >= READER_PANEL_MIN_WINDOW_DP

/** About 28% of the window as on the board, kept between 320 and 420 dp. */
internal fun readerPanelWidthDp(windowWidthDp: Int): Int = (windowWidthDp * 0.28f).toInt().coerceIn(320, 420)

/** The book note as shown in the panel, or null when there is nothing to show. */
internal fun panelNoteText(notes: String): String? = notes.trim().ifEmpty { null }

/**
 * The TabletReaderPanel board: a panel docked at the end of the window with Contents, Quotes and Notes for the
 * open book. The reader's own container is padded by the panel width, so the text area shrinks instead of
 * being covered.
 */
@Composable
internal fun TsuzukiReaderPanel(
    activity: Activity,
    visible: Boolean,
    widthDp: Int,
    manga: Manga?,
    currentChapterId: Long?,
    loadQuotes: () -> List<Quote>,
    onDismissRequest: () -> Unit,
    onOpenChapter: (chapterId: Long) -> Unit,
    fold: ReaderFold? = null,
    onInsetsChanged: (end: Dp, bottom: Dp) -> Unit = { _, _ -> },
    listenContent: (@Composable () -> Unit)? = null,
) {
    val tokens = TsuzukiTheme.tokens
    val open = visible && manga != null
    BackHandler(enabled = open, onBack = onDismissRequest)

    if (!open || manga == null) return

    var tab by rememberSaveable { mutableIntStateOf(0) }
    val entries by produceState<List<TsuzukiTocEntry>?>(null, manga.id, listenContent != null) {
        if (listenContent == null) value = withIOContext { loadToc(manga) }
    }
    val shape = RoundedCornerShape(0.dp)
    var windowTop by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    BoxWithConstraints(
        Modifier.fillMaxSize().onGloballyPositioned { windowTop = it.positionInWindow().y.roundToInt() },
        contentAlignment = if (fold == null) Alignment.CenterEnd else Alignment.BottomCenter,
    ) {
        val folded = fold?.let {
            foldedReaderPane(
                ReaderFold(it.top - windowTop, it.bottom - windowTop),
                with(density) { maxHeight.roundToPx() },
            )
        }
        if (fold != null && folded == null) return@BoxWithConstraints
        val end = if (folded == null) widthDp.dp else 0.dp
        val bottom = with(density) { (folded?.readerBottomInset ?: 0).toDp() }
        val endPx = with(density) { end.roundToPx() }
        val bottomPx = folded?.readerBottomInset ?: 0
        DisposableEffect(activity, endPx, bottomPx) {
            val container = activity.findViewById<View>(R.id.reader_container) ?: return@DisposableEffect onDispose {}
            val start = container.paddingStart
            val top = container.paddingTop
            val originalEnd = container.paddingEnd
            val originalBottom = container.paddingBottom
            container.setPaddingRelative(start, top, originalEnd + endPx, originalBottom + bottomPx)
            onInsetsChanged(end, bottom)
            onDispose {
                container.setPaddingRelative(start, top, originalEnd, originalBottom)
                onInsetsChanged(0.dp, 0.dp)
            }
        }
        val size = if (folded == null) {
            Modifier.width(widthDp.dp).fillMaxHeight().statusBarsPadding()
        } else {
            Modifier.fillMaxWidth().height(with(density) { folded.panelHeight.toDp() })
        }
        Column(
            size
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
                .navigationBarsPadding(),
        ) {
            if (listenContent != null) {
                listenContent()
                return@Column
            }
            TsuzukiSegments(
                labels = listOf(
                    stringResource(MR.strings.tsz_reader_contents),
                    stringResource(MR.strings.tsz_reader_panel_quotes),
                    stringResource(MR.strings.tsz_reader_panel_notes),
                ),
                selected = tab,
                onClick = { tab = it },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            )
            when (tab) {
                0 -> ContentsBody(
                    manga = manga,
                    entries = entries,
                    currentChapterId = currentChapterId,
                    onOpenChapter = onOpenChapter,
                    showHeading = false,
                )
                1 -> QuotesList(manga, loadQuotes)
                else -> NotePreview(manga.notes)
            }
        }
    }
}

@Composable
private fun QuotesList(manga: Manga, loadQuotes: () -> List<Quote>) {
    val quotes by produceState<List<Quote>?>(null, manga.id) { value = withIOContext { loadQuotes() } }
    val list = quotes ?: return
    if (list.isEmpty()) {
        EmptyLine(stringResource(MR.strings.tsz_reader_panel_no_quotes))
        return
    }
    val tokens = TsuzukiTheme.tokens
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(list, key = { it.id }, contentType = { "quote" }) { quote ->
            val shape = RoundedCornerShape(16.dp)
            Column(
                Modifier.fillMaxWidth().clip(shape).background(tokens.tile)
                    .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    quote.content,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    tsuzukiCase(quote.chapterName).orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun NotePreview(notes: String) {
    val text = panelNoteText(notes)
    if (text == null) {
        EmptyLine(stringResource(MR.strings.tsz_reader_panel_no_notes))
        return
    }
    Text(
        text,
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun EmptyLine(text: String) {
    Text(
        text,
        modifier = Modifier.padding(20.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
