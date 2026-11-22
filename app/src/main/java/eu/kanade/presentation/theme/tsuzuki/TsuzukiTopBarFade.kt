package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Shades content under a pinned title and fades below the bar without changing scroll padding. */
fun Modifier.tsuzukiTopBarFade(color: Color): Modifier = drawWithCache {
    val height = size.height + 18.dp.toPx()
    val brush = Brush.verticalGradient(
        0f to color,
        (size.height - 8.dp.toPx()).coerceAtLeast(0f) / height to color.copy(alpha = 0.97f),
        1f to color.copy(alpha = 0f),
        endY = height,
    )
    onDrawBehind { drawRect(brush, size = Size(size.width, height)) }
}
