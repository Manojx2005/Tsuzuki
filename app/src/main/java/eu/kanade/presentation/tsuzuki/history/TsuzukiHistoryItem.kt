package eu.kanade.presentation.tsuzuki.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.util.lang.toTimestampString
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.DateTimeFormatter

/**
 * History row in the Tsuzuki themes (design board "History"): a tile with the cover,
 * title, "chapter · time", a progress bar for the chapter, and a remove button.
 */
@Composable
fun TsuzukiHistoryItem(
    history: HistoryWithRelations,
    onClickCover: () -> Unit,
    onClickResume: () -> Unit,
    onClickDelete: () -> Unit,
    onClickFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    timeFormatter: DateTimeFormatter,
) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(18.dp)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    fun up(s: String) = if (t.isDot) s.uppercase() else s

    // Novels store scroll progress as a percent; manga store a page index we can't turn into a
    // fraction here, so they only show a full bar once read.
    val progress: Float? = when {
        history.chapterRead -> 1f
        history.isNovel && history.lastPageRead > 0 -> (history.lastPageRead / 100f).coerceIn(0f, 1f)
        else -> null
    }
    val readAt = remember(history.readAt, timeFormatter) {
        history.readAt?.toTimestampString(timeFormatter).orEmpty()
    }
    val chapter = if (history.chapterNumber > -1) "Ch. ${formatChapterNumber(history.chapterNumber)}" else null

    Row(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(t.tile)
            .then(if (t.tileBorder != null) Modifier.border(1.dp, t.tileBorder, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClickResume)
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MangaCover.Book(
            data = history.coverData,
            modifier = Modifier.width(44.dp),
            shape = RoundedCornerShape(6.dp),
            onClick = onClickCover,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = up(history.title),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = up(listOfNotNull(chapter, readAt.takeIf { it.isNotEmpty() }).joinToString(" · ")),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            progress?.let { TsuzukiProgressBar(it, dots = 16, thickness = 4.dp) }
        }
        if (!history.coverData.isMangaFavorite) {
            IconButton(onClick = onClickFavorite) {
                Icon(
                    Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(MR.strings.add_to_library),
                    tint = muted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        IconButton(onClick = onClickDelete) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(MR.strings.action_delete),
                tint = muted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
