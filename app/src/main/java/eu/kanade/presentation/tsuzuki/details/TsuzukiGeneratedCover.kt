package eu.kanade.presentation.tsuzuki.details

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import kotlin.math.min
import kotlin.math.roundToInt

internal data class CoverPalette(val background: Color, val art: Color, val ink: Color)

private val palettes = listOf(
    CoverPalette(Color(0xFF1C2B4A), Color(0xFFE9B44C), Color(0xFFF4EFE6)),
    CoverPalette(Color(0xFF5B1F2E), Color(0xFFF08A5D), Color(0xFFF8EDE3)),
    CoverPalette(Color(0xFF123B36), Color(0xFF9FD8B5), Color(0xFFEFF7F1)),
    CoverPalette(Color(0xFF2B2240), Color(0xFFC8A2FF), Color(0xFFF2ECFF)),
    CoverPalette(Color(0xFFE6DCCB), Color(0xFFB23A2B), Color(0xFF1E1A16)),
    CoverPalette(Color(0xFF0E2A3A), Color(0xFF5FC7E8), Color(0xFFE8F6FB)),
    CoverPalette(Color(0xFF3A2A1A), Color(0xFFD9A35F), Color(0xFFF6EBDD)),
    CoverPalette(Color(0xFFD4E4DA), Color(0xFF1F4D3A), Color(0xFF14231B)),
    CoverPalette(Color(0xFF18181A), Color(0xFFE8553D), Color(0xFFF4F1EA)),
    CoverPalette(Color(0xFF463A62), Color(0xFFF2C14E), Color(0xFFFBF4E2)),
)

internal data class CoverDesign(val palette: CoverPalette, val motif: Int)

internal fun generatedCoverDesign(title: String): CoverDesign {
    val hash = title.trim().hashCode()
    return CoverDesign(palettes[Math.floorMod(hash, palettes.size)], (hash ushr 8) % 6)
}

@Composable
internal fun rememberTsuzukiGeneratedCover(title: String): Painter {
    val tokens = TsuzukiTheme.tokens
    val measurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.displaySmall
    return remember(title, tokens, measurer, textStyle) {
        val design = generatedCoverDesign(title)
        val palette = if (tokens.isDot) {
            CoverPalette(tokens.tile, tokens.track, tokens.dotOn)
        } else {
            design.palette
        }
        GeneratedCoverPainter(
            title = AnnotatedString(if (tokens.isDot) title.trim().uppercase() else title.trim()),
            design = design.copy(palette = palette),
            marker = if (tokens.isDot) tokens.dotHere else null,
            measurer = measurer,
            textStyle = textStyle,
        )
    }
}

private class GeneratedCoverPainter(
    private val title: AnnotatedString,
    private val design: CoverDesign,
    private val marker: Color?,
    private val measurer: TextMeasurer,
    private val textStyle: TextStyle,
) : Painter() {
    override val intrinsicSize: Size = Size.Unspecified

    // The art already follows the theme's cover colours, including Dot's red marker.
    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean = true

    override fun DrawScope.onDraw() {
        val (background, art, ink) = design.palette
        drawRect(background)

        val width = size.width
        val height = size.height
        val radius = min(width * 0.24f, height * 0.17f)
        val center = Offset(width * 0.5f, height * 0.33f)
        when (design.motif) {
            0 -> {
                drawCircle(art, radius, center)
                val horizon = center.y + radius * 0.35f
                drawRect(background, Offset(0f, horizon), Size(width, radius))
                drawLine(art, Offset(width * 0.17f, horizon), Offset(width * 0.83f, horizon), width * 0.015f)
            }
            1 -> {
                for (stripe in -1..1) {
                    val x = center.x + stripe * radius * 0.7f
                    drawLine(
                        art.copy(alpha = 0.35f),
                        Offset(x - radius, center.y + radius),
                        Offset(x + radius, center.y - radius),
                        width * 0.05f,
                    )
                }
                drawCircle(art, radius * 0.66f, center)
            }
            2 -> {
                drawArc(
                    color = art,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = center - Offset(radius, radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width * 0.035f),
                )
                for (side in listOf(-1, 1)) {
                    val edge = center + Offset(side * radius, 0f)
                    drawLine(art, edge, edge + Offset(0f, radius), width * 0.035f)
                }
            }
            3 -> {
                drawCircle(art, radius, center)
                drawCircle(background, radius * 0.87f, center + Offset(radius * 0.48f, -radius * 0.35f))
                drawCircle(art, radius * 0.11f, center + Offset(radius * 1.15f, -radius * 0.6f))
            }
            4 -> {
                drawRect(
                    art.copy(alpha = 0.35f),
                    Offset(width * 0.25f, height * 0.09f),
                    Size(width * 0.13f, height * 0.48f),
                )
                drawCircle(art, radius * 0.85f, center + Offset(radius * 0.23f, 0f))
            }
            5 -> {
                for (ring in 1..3) {
                    drawCircle(art, radius * ring / 3f, center, style = Stroke(width * 0.018f))
                }
            }
        }
        marker?.let { drawCircle(it, width * 0.022f, Offset(width * 0.87f, height * 0.11f)) }

        val inset = width * 0.12f
        val titleSize = min(width * 0.165f, height * 0.11f).toDp().value.sp
        val layout = measurer.measure(
            text = title,
            style = textStyle.copy(fontSize = titleSize, lineHeight = titleSize * 1.08f, letterSpacing = 0.sp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
            constraints = Constraints(maxWidth = (width - inset * 2).roundToInt().coerceAtLeast(0)),
        )
        drawText(layout, color = ink, topLeft = Offset(inset, height - height * 0.1f - layout.size.height))
    }
}
