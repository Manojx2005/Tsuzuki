package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Index of the "you are here" dot for [progress] across [count] dots, or -1 when finished.
 * Matches the design's Bar: on = round(p × n); dots before it are read.
 */
internal fun hereDotIndex(progress: Float, count: Int): Int {
    val p = progress.coerceIn(0f, 1f)
    return if (p >= 1f) -1 else (p * count).roundToInt()
}

/**
 * Progress bar. Colour: solid accent bar on a translucent track.
 * Dot: a row of [dots] dots; read = white, unread = grey, current = red.
 */
@Composable
fun TsuzukiProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    dots: Int = 20,
    thickness: Dp = 6.dp,
    fill: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    track: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
) {
    val t = TsuzukiTheme.tokens
    val p = progress.coerceIn(0f, 1f)
    val fillColor = if (fill.isSpecifiedColor()) fill else t.accent
    val trackColor = if (track.isSpecifiedColor()) track else t.track
    Canvas(
        modifier
            .fillMaxWidth()
            .height(thickness)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(p, 0f..1f) },
    ) {
        val h = size.height
        if (!t.isDot) {
            val r = CornerRadius(h / 2, h / 2)
            drawRoundRect(trackColor, cornerRadius = r)
            if (p > 0f) drawRoundRect(fillColor, size = Size(size.width * p, h), cornerRadius = r)
        } else {
            val on = (p * dots).roundToInt()
            val here = hereDotIndex(p, dots)
            // Spread across the width, but never further apart than the design's dot pitch
            // (gap = half a dot), so wide rows keep a compact, left-aligned bar.
            val step = if (dots > 1) minOf((size.width - h) / (dots - 1), h * 2.2f) else 0f
            for (k in 0 until dots) {
                val color = when {
                    k == here -> t.dotHere
                    k < on -> t.dotOn
                    else -> t.dotOff
                }
                drawCircle(color, radius = h / 2, center = Offset(h / 2 + k * step, h / 2))
            }
        }
    }
}

/**
 * Progress ring. Colour: solid arc. Dot: a ring of [dots] dots, red at the current position.
 * Put a label in [content] (it is centred).
 */
@Composable
fun TsuzukiProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 72.dp,
    thickness: Dp = 6.dp,
    dots: Int = 24,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val t = TsuzukiTheme.tokens
    val p = progress.coerceIn(0f, 1f)
    Box(
        modifier
            .size(diameter)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(p, 0f..1f) },
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Canvas(Modifier.size(diameter)) {
            val stroke = thickness.toPx()
            val radius = (size.minDimension - stroke) / 2
            if (!t.isDot) {
                val topLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2, radius * 2)
                drawArc(t.track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                drawArc(
                    t.accent,
                    -90f,
                    360f * p,
                    false,
                    topLeft,
                    arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            } else {
                val on = (p * dots).roundToInt()
                val here = hereDotIndex(p, dots)
                for (k in 0 until dots) {
                    val angle = -PI / 2 + 2 * PI * k / dots
                    val color = when {
                        k == here -> t.dotHere
                        k < on -> t.dotOn
                        else -> t.dotOff
                    }
                    drawCircle(
                        color,
                        radius = stroke / 2,
                        center = Offset(
                            center.x + radius * cos(angle).toFloat(),
                            center.y + radius * sin(angle).toFloat(),
                        ),
                    )
                }
            }
        }
        content()
    }
}

/** Fills with the theme background; in Dot, also draws the 12 dp dot grid. */
fun Modifier.tsuzukiBackground(tokens: TsuzukiTokens): Modifier {
    val base = if (tokens.background.isSpecifiedColor()) background(tokens.background) else this
    val grid = tokens.gridDot ?: return base
    return base.drawBehind {
        val step = tokens.gridSpacing.toPx()
        val r = 1.dp.toPx()
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                drawCircle(grid, r, Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

private fun androidx.compose.ui.graphics.Color.isSpecifiedColor() =
    this != androidx.compose.ui.graphics.Color.Unspecified

/** Bento tile: translucent and borderless in Colour, near-black with a 1 dp border in Dot. */
@Composable
fun TsuzukiTile(
    modifier: Modifier = Modifier,
    padding: Dp = 20.dp,
    content: @Composable () -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(t.tileRadius)
    Column(
        modifier
            .clip(shape)
            .background(t.tile)
            .then(if (t.tileBorder != null) Modifier.border(1.dp, t.tileBorder, shape) else Modifier)
            .padding(padding),
    ) { content() }
}

/** Dot theme covers: greyscale plus a little contrast, matching the design's `grayscale(1) contrast(1.2)`. */
val DotCoverFilter: androidx.compose.ui.graphics.ColorFilter = run {
    val grey = androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }
    val c = 1.2f
    val offset = 128f * (1f - c)
    grey.timesAssign(
        androidx.compose.ui.graphics.ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, offset,
                0f, c, 0f, 0f, offset,
                0f, 0f, c, 0f, offset,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
    androidx.compose.ui.graphics.ColorFilter.colorMatrix(grey)
}

/** Dot: true when the owner keeps cover colours (Appearance → "Colour covers"). Provided by the app theme. */
val LocalDotColourCovers = androidx.compose.runtime.compositionLocalOf { false }

/** Reads the "Colour covers" preference for [LocalDotColourCovers]; false where Injekt isn't set up (previews). */
@Composable
fun rememberDotColourCovers(): Boolean {
    val pref = androidx.compose.runtime.remember {
        runCatching { Injekt.get<eu.kanade.domain.ui.UiPreferences>().tsuzukiDotColourCovers }
            .getOrNull()
    } ?: return false
    val keep by androidx.compose.runtime.remember(pref) { pref.changes() }.collectAsState(initial = pref.get())
    return keep
}

/** The filter every cover uses: Dot draws covers in greyscale unless the owner keeps their colours. */
@Composable
fun tsuzukiCoverFilter(): androidx.compose.ui.graphics.ColorFilter? =
    if (TsuzukiTheme.tokens.isDot && !LocalDotColourCovers.current) DotCoverFilter else null
