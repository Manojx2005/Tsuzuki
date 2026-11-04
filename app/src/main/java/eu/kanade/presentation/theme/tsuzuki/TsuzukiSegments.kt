package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Segmented control from the sheet boards: a tile track with the selected segment filled white. */
@Composable
fun TsuzukiSegments(
    labels: List<String>,
    selected: Int,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier.padding(horizontal = 16.dp),
) {
    val tokens = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(22.dp)
    // MotionSpec: the selected pill slides between segments (200 ms; dot-by-dot in Dot).
    val position by androidx.compose.animation.core.animateFloatAsState(
        targetValue = selected.toFloat(),
        animationSpec = tsuzukiTween(TsuzukiMotion.TOGGLE_MS),
        label = "segment",
    )
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier.fillMaxWidth().clip(shape)
            .background(tokens.tile)
            .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .padding(4.dp),
    ) {
        val segmentWidth = maxWidth / labels.size.coerceAtLeast(1)
        Box(
            Modifier
                .offset(x = segmentWidth * position)
                .width(segmentWidth)
                .height(44.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(tokens.dotOn),
        )
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            labels.forEachIndexed { index, label ->
                val active = selected == index
                Box(
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(18.dp))
                        .selectable(selected = active, role = Role.Tab, onClick = { onClick(index) })
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (tokens.isDot) label.uppercase() else label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        // Text flips colour when the pill is mostly under it.
                        color = if (kotlin.math.abs(position - index) < 0.5f) {
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
