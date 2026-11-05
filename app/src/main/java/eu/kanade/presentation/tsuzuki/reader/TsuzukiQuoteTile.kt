package eu.kanade.presentation.tsuzuki.reader

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Reorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import eu.kanade.presentation.theme.tsuzuki.LocalTsuzukiTokens
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.ui.reader.quote.Quote
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TsuzukiQuoteTile(
    quote: Quote,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    isReorderMode: Boolean,
    isBeingDragged: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit,
    onItemHeightMeasured: (Float) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val context = LocalContext.current
    val quoteLabel = stringResource(TDMR.strings.action_quotes)
    val chapter = quoteChapterLabel(quote)
    val date = DateUtils.formatDateTime(context, quote.timestamp, DateUtils.FORMAT_SHOW_DATE)
    val chapterAndDate = if (chapter.isBlank()) date else "$chapter · $date"
    val quoteWithChapter = quoteForSharing(quote)
    val muted = tokens.dotOn.copy(alpha = 0.65f)
    val radius = 22.dp

    var tileHeight by remember { mutableIntStateOf(0) }
    val startDrag by rememberUpdatedState(onDragStart)
    val moveDrag by rememberUpdatedState(onDrag)
    val endDrag by rememberUpdatedState(onDragEnd)
    val measureItem by rememberUpdatedState(onItemHeightMeasured)
    val gesture = if (isReorderMode) {
        Modifier
            .onSizeChanged { tileHeight = it.height }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        measureItem(tileHeight.toFloat())
                        startDrag()
                    },
                    onDragEnd = { endDrag() },
                    onDragCancel = { endDrag() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        moveDrag(dragAmount.y)
                    },
                )
            }
    } else {
        Modifier.tsuzukiClickable(role = Role.Button, onClick = onOpen)
    }
    val selection = if (isBeingDragged) {
        Modifier.zIndex(1f).border(2.dp, tokens.accent, RoundedCornerShape(radius))
    } else {
        Modifier
    }
    CompositionLocalProvider(LocalTsuzukiTokens provides tokens.copy(tileRadius = radius)) {
        TsuzukiTile(Modifier.fillMaxWidth().then(selection).then(gesture), padding = 14.dp) {
            TsuzukiReadingTypography {
                Text(
                    text = quote.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.dotOn,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tsuzukiCase(chapterAndDate).orEmpty(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isReorderMode) {
                    Icon(
                        Icons.Outlined.Reorder,
                        stringResource(MR.strings.tsz_quotes_drag_to_reorder),
                        tint = tokens.accent,
                    )
                } else {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, quoteWithChapter)
                        context.startActivity(Intent.createChooser(intent, null))
                    }) {
                        Icon(Icons.Outlined.Share, stringResource(MR.strings.action_share), tint = muted)
                    }
                    IconButton(onClick = { context.copyToClipboard(quoteLabel, quoteWithChapter) }) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            stringResource(MR.strings.action_copy_to_clipboard),
                            tint = muted,
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, stringResource(MR.strings.action_delete), tint = muted)
                    }
                }
            }
        }
    }
}

internal fun quoteChapterLabel(quote: Quote): String = buildString {
    append(quote.chapterName)
    quote.paragraphIndex?.let { index ->
        if (isNotBlank()) append(" · ")
        append("¶${index + 1}")
    }
}

internal fun quoteForSharing(quote: Quote): String =
    if (quote.chapterName.isBlank()) quote.content else "${quote.content}\n\n${quote.chapterName}"
