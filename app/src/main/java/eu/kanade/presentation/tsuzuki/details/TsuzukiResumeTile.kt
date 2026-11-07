package eu.kanade.presentation.tsuzuki.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.tsuzuki.reader.TsuzukiReadingEstimateLabel
import eu.kanade.tachiyomi.ui.manga.MangaViewModel
import eu.kanade.tachiyomi.util.chapter.getNextUnread
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TsuzukiDetailsResumeTile(
    state: MangaViewModel.State.Success,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val next = remember(state.chapters, state.manga) { state.chapters.getNextUnread(state.manga) }
    TsuzukiResumeTile(
        readCount = state.chapters.count { it.chapter.read },
        total = state.chapters.size,
        nextChapter = next?.name,
        onClick = onClick,
        modifier = modifier,
        estimates = {
            if (state.isNovel && next != null) {
                val chapters = remember(state.chapters) { state.chapters.map { it.chapter } }
                TsuzukiReadingEstimateLabel(state.manga, next.id, next.lastPageRead.toInt(), chapters)
            }
        },
    )
}

/**
 * Resume tile on book details (board "NovelDetails"): play disc, what comes next, and progress
 * through the book. The play disc uses the theme accent.
 */
@Composable
fun TsuzukiResumeTile(
    readCount: Int,
    total: Int,
    nextChapter: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    estimates: @Composable () -> Unit = {},
) {
    val t = TsuzukiTheme.tokens
    val ink = t.dotOn
    val shape = RoundedCornerShape(24.dp)
    val started = readCount > 0
    val label =
        (if (started) stringResource(MR.strings.tsz_resume) else stringResource(MR.strings.tsz_start)) +
            " · $readCount / $total"
    Row(
        modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clip(shape)
            .background(t.tile)
            .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(t.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = t.onAccent,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (t.isDot) label.uppercase() else label,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                color = ink,
                maxLines = 1,
            )
            nextChapter?.let {
                Text(
                    text = if (t.isDot) it.uppercase() else it,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = ink.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            estimates()
            if (total > 0) {
                TsuzukiProgressBar(
                    progress = readCount.toFloat() / total,
                    dots = 18,
                    thickness = 5.dp,
                )
            }
        }
    }
}
