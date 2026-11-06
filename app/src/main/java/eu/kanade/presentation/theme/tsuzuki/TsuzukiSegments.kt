package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** The label under the moving pill, changing only when the pill crosses a segment midpoint. */
internal fun highlightedSegment(position: Float, count: Int): Int =
    if (count <= 0) -1 else position.roundToInt().coerceIn(0, count - 1)

/** Segmented control from the sheet boards: a tile track with the selected segment filled white. */
@Composable
fun TsuzukiSegments(
    labels: List<String>,
    selected: Int,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier.padding(horizontal = 16.dp),
) {
    if (LocalDensity.current.fontScale >= 1.5f) {
        TsuzukiPillRow(labels, selected, onClick, modifier)
        return
    }
    val tokens = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(22.dp)
    // Continuous motion stays in the layer; label contrast changes only at segment midpoints.
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val position = androidx.compose.animation.core.animateFloatAsState(
        targetValue = selected.toFloat(),
        animationSpec = tsuzukiTween(TsuzukiMotion.TOGGLE_MS),
        label = "segment",
    )
    val highlightedIndex by remember(position, labels.size) {
        derivedStateOf { if (position.value < -0.5f) -1 else highlightedSegment(position.value, labels.size) }
    }
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier.fillMaxWidth().clip(shape)
            .background(tokens.tile)
            .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .padding(4.dp),
    ) {
        val segmentWidth = maxWidth / labels.size.coerceAtLeast(1)
        var rowHeightPx by remember { mutableIntStateOf(0) }
        val rowHeight = with(LocalDensity.current) { rowHeightPx.toDp().coerceAtLeast(48.dp) }
        Box(
            Modifier
                .graphicsLayer { translationX = direction * segmentWidth.toPx() * position.value }
                .width(segmentWidth)
                .height(rowHeight)
                .clip(RoundedCornerShape(18.dp))
                .background(tokens.dotOn),
        )
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup().onSizeChanged { rowHeightPx = it.height },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            labels.forEachIndexed { index, label ->
                val active = selected == index
                Box(
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(18.dp))
                        .selectable(selected = active, role = Role.Tab, onClick = { onClick(index) })
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (tokens.isDot) label.uppercase() else label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (highlightedIndex == index) {
                            tokens.background
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}
