package eu.kanade.tachiyomi.tsuzuki.anime.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AnimeDetailsHeader(
    source: AnimeSource?,
    anime: SAnime,
    coverWidth: Dp = 104.dp,
    onCoverClick: (() -> Unit)? = null,
) {
    val tokens = TsuzukiTheme.tokens
    val status = when (anime.status) {
        SAnime.ONGOING -> stringResource(MR.strings.ongoing)
        SAnime.COMPLETED -> stringResource(MR.strings.completed)
        SAnime.LICENSED -> stringResource(MR.strings.licensed)
        SAnime.PUBLISHING_FINISHED -> stringResource(MR.strings.publishing_finished)
        SAnime.CANCELLED -> stringResource(MR.strings.cancelled)
        SAnime.ON_HIATUS -> stringResource(MR.strings.on_hiatus)
        SAnime.UPCOMING -> androidx.compose.ui.res.stringResource(R.string.tsz_anime_upcoming)
        else -> null
    }
    val metadata = listOfNotNull(anime.author, anime.artist, status, source?.name)
        .map(String::trim).filter(String::isNotEmpty).distinct()
    var expanded by rememberSaveable(anime.url) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AnimeCover(
                source,
                anime,
                Modifier.width(coverWidth)
                    .then(if (onCoverClick != null) Modifier.clickable(onClick = onCoverClick) else Modifier),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    tsuzukiMenuLabel(anime.title),
                    color = tokens.dotOn,
                    style = MaterialTheme.typography.displaySmall,
                )
                if (metadata.isNotEmpty()) {
                    Text(
                        tsuzukiMenuLabel(metadata.joinToString(" · ")),
                        color = tokens.mutedText,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        anime.description?.takeIf(String::isNotBlank)?.let { synopsis ->
            Text(
                synopsis,
                color = tokens.dotOn,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = { expanded = !expanded }) {
                val label = if (expanded) MR.strings.manga_info_collapse else MR.strings.manga_info_expand
                Text(tsuzukiMenuLabel(stringResource(label)))
            }
        }
        val genres = anime.getGenres().orEmpty()
        if (genres.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                genres.forEach { genre ->
                    Surface(shape = RoundedCornerShape(18.dp), color = tokens.tile) {
                        Text(
                            tsuzukiMenuLabel(genre),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = tokens.dotOn,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AnimeLibraryActions(
    favorite: Boolean,
    enabled: Boolean,
    onFavorite: () -> Unit,
    onTrack: (() -> Unit)?,
    onSave: (() -> Unit)?,
    onExternal: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AnimeDetailAction(
            label = stringResource(if (favorite) MR.strings.in_library else MR.strings.add_to_library),
            icon = if (favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            enabled = enabled,
            active = favorite,
            onClick = onFavorite,
        )
        if (onTrack != null) {
            AnimeDetailAction(
                label = stringResource(MR.strings.action_track),
                icon = Icons.Outlined.Sync,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                enabled = enabled,
                onClick = onTrack,
            )
        }
        if (onSave != null) {
            AnimeDetailAction(
                label = stringResource(MR.strings.action_save),
                icon = Icons.Outlined.Download,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                enabled = enabled,
                onClick = onSave,
            )
        }
        if (onExternal != null) {
            AnimeDetailAction(
                label = stringResource(MR.strings.action_open_in_web_view),
                icon = Icons.Outlined.Link,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                enabled = enabled,
                onClick = onExternal,
            )
        }
    }
}

@Composable
private fun AnimeDetailAction(
    label: String,
    icon: ImageVector,
    modifier: Modifier,
    enabled: Boolean,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val color = if (active) tokens.accent else tokens.dotOn
    TsuzukiTile(
        modifier.tsuzukiClickable(enabled = enabled, role = Role.Button, onClick = onClick),
        padding = 8.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Text(
                tsuzukiMenuLabel(label),
                color = color,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
