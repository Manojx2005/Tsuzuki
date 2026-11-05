package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Selectable pill (boards "LibraryList", "Updates"): 38 dp, fully rounded; the selected one takes
 * the accent, the rest sit on a tile (with the tile border in Dot). Optional count after the label.
 */
@Composable
fun TsuzukiPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    role: Role = Role.Tab,
) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(19.dp)
    val ink = if (selected) t.onAccent else MaterialTheme.colorScheme.onSurface
    val press = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        Modifier.tsuzukiPress(press).then(modifier)
            .height(38.dp)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(t.accent)
                } else {
                    Modifier
                        .background(t.tile)
                        .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
                },
            )
            .clickable(
                interactionSource = press,
                indication = androidx.compose.material3.ripple(),
                role = role,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = if (t.isDot) text.uppercase() else text,
            color = ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
        count?.let {
            Text(text = "$it", color = ink.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/**
 * Horizontally scrolling row of [TsuzukiPill]s that keeps the selected one in view (category tabs,
 * Browse/Updates tab rows). [counts] may hold null for "no count".
 */
@Composable
fun TsuzukiPillRow(
    labels: List<String>,
    selected: Int,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    counts: List<Int?> = emptyList(),
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    androidx.compose.runtime.LaunchedEffect(selected) {
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.none { it.index == selected && it.offset >= 0 }) listState.animateScrollToItem(selected)
    }
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier,
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(labels.size, contentType = { "pill" }) { index ->
            TsuzukiPill(
                text = labels[index],
                selected = index == selected,
                onClick = { onClick(index) },
                count = counts.getOrNull(index),
            )
        }
    }
}

/** Small uppercase label above a group of tiles (Browse, Settings, History, Updates boards). */
@Composable
fun TsuzukiSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(contentPadding),
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
