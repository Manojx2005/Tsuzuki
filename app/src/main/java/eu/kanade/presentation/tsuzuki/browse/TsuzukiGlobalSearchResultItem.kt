package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchStats
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * One source's results in global search, in the Tsuzuki themes (board 04-browse "GlobalSearch"): a tile with the
 * source name, its language line and an arrow, and the result covers inside. The board's results-and-time line is
 * [TsuzukiGlobalSearchSummary], shown above the tiles.
 */
@Composable
fun TsuzukiGlobalSearchResultItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val up: (String) -> String = { if (t.isDot) it.uppercase() else it }
    val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(t.tile)
            .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .padding(bottom = 12.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    up(title),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    up(subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = muted,
                    maxLines = 1,
                )
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Color.White)
        }
        content()
    }
}

/** results-and-time line of the board ("N results, 0.6 s"); shown once the search has finished timing. */
@Composable
fun TsuzukiGlobalSearchSummary(resultCount: Int, elapsedMillis: Long?) {
    if (elapsedMillis == null) return
    val t = TsuzukiTheme.tokens
    val text = stringResource(
        MR.strings.tsz_search_results_time,
        resultCount,
        GlobalSearchStats.formatSeconds(elapsedMillis),
    )
    Text(
        if (t.isDot) text.uppercase() else text,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
        color = if (t.isDot) Color(0xFF8A8A8A) else t.accent,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
    )
}
