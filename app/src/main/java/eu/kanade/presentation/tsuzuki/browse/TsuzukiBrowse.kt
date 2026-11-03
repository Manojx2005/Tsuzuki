package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

/** Board "Browse": section label above a group of sources. */
@Composable
fun TsuzukiSourceHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
