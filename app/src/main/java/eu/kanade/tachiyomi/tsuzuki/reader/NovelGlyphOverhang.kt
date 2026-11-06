package eu.kanade.tachiyomi.tsuzuki.reader

import android.graphics.Color
import android.widget.TextView

/**
 * TextView clips its text to the area inside its padding, so glyphs that overhang their advance width (bold or
 * italic "f", "j", closing quotes) lose their tips at the edge of justified lines. TextView widens that clip by
 * the shadow radius, so a fully transparent, zero-offset shadow keeps the overhang visible without drawing
 * anything. The radius only needs to cover the overhang: a quarter of the text size is plenty.
 */
internal fun TextView.keepGlyphOverhangVisible() {
    setShadowLayer(overhangRadius(textSize), 0f, 0f, Color.TRANSPARENT)
}

internal fun overhangRadius(textSizePx: Float): Float = (textSizePx * 0.25f).coerceAtLeast(1f)
