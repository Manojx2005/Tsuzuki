package eu.kanade.presentation.tsuzuki.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiPress
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.updates.UpdatesNovelGroup
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Board "Updates": one book's new chapters as a tile. Cover, title, "N new · latest chapter", and a status pill:
 * NEW (accent) while any are unread, SAVED (outline) once every chapter is downloaded.
 */
@Composable
fun TsuzukiUpdatesGroupTile(group: UpdatesNovelGroup, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t = TsuzukiTheme.tokens
    val up: (String) -> String = { if (t.isDot) it.uppercase() else it }
    val shape = RoundedCornerShape(22.dp)
    val press = remember { MutableInteractionSource() }
    val latest = group.chapters.maxByOrNull { it.update.dateFetch }?.update?.chapterName
    val saved = group.chapters.isNotEmpty() &&
        group.chapters.all { it.downloadStateProvider() == Download.State.DOWNLOADED }
    Row(
        modifier
            .tsuzukiPress(press)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(t.tile)
            .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .clickable(interactionSource = press, indication = ripple(), role = Role.Button, onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MangaCover.Book(
            data = group.coverUrl,
            modifier = Modifier.width(44.dp).height(62.dp),
        )
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                up(group.mangaTitle),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(stringResource(MR.strings.tsz_updates_new_count, group.chapterCount), latest)
                    .joinToString(" · ")
                    .let(up),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        when {
            group.hasUnreadChapters -> StatusPill(up(stringResource(MR.strings.tsz_updates_new)), filled = true)
            saved -> StatusPill(up(stringResource(MR.strings.tsz_updates_saved)), filled = false)
        }
    }
}

@Composable
private fun StatusPill(text: String, filled: Boolean) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.clip(shape)
            .then(
                if (filled) {
                    Modifier.background(t.accent)
                } else {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface, shape)
                },
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (filled) t.onAccent else MaterialTheme.colorScheme.onSurface,
        )
    }
}
