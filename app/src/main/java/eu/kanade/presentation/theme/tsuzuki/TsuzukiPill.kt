package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.DropdownMenu
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Selectable pill (boards "LibraryList", "Updates"): 48 dp, fully rounded; the selected one takes
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
    val shape = RoundedCornerShape(percent = 50)
    val ink = if (selected) t.onAccent else MaterialTheme.colorScheme.onSurface
    val press = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        Modifier.tsuzukiPress(press).then(modifier)
            .heightIn(min = 48.dp)
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
            .selectable(
                selected = selected,
                interactionSource = press,
                indication = androidx.compose.material3.ripple(),
                role = role,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = if (t.isDot) text.uppercase() else text,
            color = ink,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        count?.let {
            Text(text = "$it", color = ink.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Keep the active item visible when a long shelf or filter list needs overflow. */
internal fun visiblePillIndices(size: Int, selected: Int): List<Int> {
    if (size <= 6) return (0 until size).toList()
    return if (selected in 5 until size) (0..3).toList() + selected else (0..4).toList()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TsuzukiPillRow(
    labels: List<String>,
    selected: Int,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    counts: List<Int?> = emptyList(),
) {
    val visible = visiblePillIndices(labels.size, selected)
    var more by remember { mutableStateOf(false) }
    FlowRow(
        modifier = modifier.fillMaxWidth().selectableGroup().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        visible.forEach { index ->
            TsuzukiPill(
                text = labels[index],
                selected = index == selected,
                onClick = { onClick(index) },
                count = counts.getOrNull(index),
            )
        }
        if (visible.size < labels.size) {
            Box {
                TsuzukiPill(stringResource(MR.strings.label_more), false, { more = true }, role = Role.Button)
                DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                    labels.indices.filterNot(visible::contains).forEach { index ->
                        val label = labels[index]
                        val count = counts.getOrNull(index)
                        TsuzukiMenuItem(if (count == null) label else "$label ($count)", onClick = {
                            more = false
                            onClick(index)
                        })
                    }
                }
            }
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
