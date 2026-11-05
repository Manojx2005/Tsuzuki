package eu.kanade.presentation.tsuzuki.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme

@Composable
internal fun TsuzukiNovelStatusBar(
    progressPercent: Int,
    pageIndex: Int,
    pageCount: Int,
    showTrack: Boolean,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier,
    onHeightChanged: (Int) -> Unit,
    status: @Composable () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val paged = pageCount > 0
    val progress = if (paged) {
        novelPageNumber(pageIndex, pageCount).toFloat() / pageCount
    } else {
        progressPercent / 100f
    }
    Column(
        modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .onSizeChanged { onHeightChanged(it.height) },
    ) {
        if (showTrack) {
            TsuzukiProgressBar(
                progress = progress,
                modifier = Modifier.padding(horizontal = 26.dp).padding(top = 8.dp),
                dots = if (paged) pageCount.coerceAtMost(34) else 34,
                thickness = if (paged) 4.dp else 3.dp,
                fill = if (paged) textColor else tokens.accent,
                track = textColor.copy(alpha = 0.18f),
                spreadDots = true,
            )
        }
        status()
    }
}

internal fun novelPageNumber(pageIndex: Int, pageCount: Int): Int = pageIndex.coerceIn(0, pageCount - 1) + 1
