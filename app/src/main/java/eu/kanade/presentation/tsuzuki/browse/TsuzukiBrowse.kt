package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private val TileRadius = 24.dp

/** Board "Browse": the wide search field that searches every source at once. */
@Composable
fun TsuzukiGlobalSearchTile(sourceCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(26.dp)
    val text = stringResource(MR.strings.tsz_search_sources, sourceCount)
    Row(
        modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(t.tile)
            .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        Icon(Icons.Outlined.Search, contentDescription = null, tint = muted, modifier = Modifier.size(18.dp))
        Text(if (t.isDot) text.uppercase() else text, color = muted, fontSize = 14.sp, maxLines = 1)
    }
}

/**
 * One row of a grouped tile: consecutive rows share one rounded tile, so only the first row gets
 * the top corners and only the last gets the bottom corners.
 */
fun tsuzukiGroupedTile(first: Boolean, last: Boolean): Modifier = Modifier.composed {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(
        topStart = if (first) TileRadius else 0.dp,
        topEnd = if (first) TileRadius else 0.dp,
        bottomStart = if (last) TileRadius else 0.dp,
        bottomEnd = if (last) TileRadius else 0.dp,
    )
    Modifier
        .padding(horizontal = 12.dp)
        .clip(shape)
        .background(t.tile)
        .padding(top = if (first) 4.dp else 0.dp, bottom = if (last) 4.dp else 0.dp)
}

/**
 * Board "Browse": pinned sources as big two-up tiles (icon, pin, name, language). Tap opens the
 * catalogue, long press opens the source options, the pin unpins.
 */
@Composable
fun TsuzukiPinnedSources(
    sources: List<tachiyomi.domain.source.model.Source>,
    onClick: (tachiyomi.domain.source.model.Source) -> Unit,
    onLongClick: (tachiyomi.domain.source.model.Source) -> Unit,
    onUnpin: (tachiyomi.domain.source.model.Source) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(TileRadius)
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.foundation.layout.Column(
        modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sources.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { source ->
                    androidx.compose.foundation.layout.Column(
                        Modifier
                            .weight(1f)
                            .height(128.dp)
                            .clip(shape)
                            .background(t.tile)
                            .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
                            .combinedClickable(
                                onClick = { onClick(source) },
                                onLongClick = { onLongClick(source) },
                            )
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            eu.kanade.presentation.browse.components.SourceIcon(source = source)
                            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                            androidx.compose.material3.IconButton(
                                onClick = { onUnpin(source) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    androidx.compose.material.icons.Icons.Filled.PushPin,
                                    contentDescription = stringResource(MR.strings.action_unpin),
                                    tint = t.accent,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        Text(
                            if (t.isDot) source.name.uppercase() else source.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val lang = eu.kanade.tachiyomi.util.system.LocaleHelper.getSourceDisplayName(
                            source.lang,
                            context,
                        )
                        Text(
                            if (t.isDot) lang.uppercase() else lang,
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (pair.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            }
        }
    }
}
