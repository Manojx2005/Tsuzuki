package eu.kanade.presentation.theme.tsuzuki

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil3.asDrawable
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The two colours the Colour theme takes from a book cover (docs/PLAN.md §3). */
@Immutable
data class BookPalette(
    /** Screen background. Must stay dark enough for white text on top. */
    val background: Color,
    /** Accent for progress, the selected nav pill and buttons. Must stand out from [background]. */
    val accent: Color,
)

/** Extracts a [BookPalette] from a cover bitmap. Runs off the main thread. */
suspend fun extractBookPalette(cover: Bitmap): BookPalette = withContext(Dispatchers.Default) {
    val palette = Palette.from(cover).maximumColorCount(16).generate()
    pickBookPalette(palette)
}

/** Fallback navy + gold, used when a cover gives us nothing usable. */
private val Fallback = BookPalette(background = Color(0xFF1C2B4A), accent = Color(0xFFE9B44C))

/** Darkens [color] until white text on it is readable (luminance ≤ [maxLuminance]). */
internal fun darkenForText(color: Color, maxLuminance: Float = 0.08f): Color {
    var c = color
    repeat(12) {
        if (c.luminance() <= maxLuminance) return c
        c = Color(red = c.red * 0.82f, green = c.green * 0.82f, blue = c.blue * 0.82f, alpha = 1f)
    }
    return c
}

/**
 * Chooses the background and accent from the cover's swatches.
 *
 * Swatches available on [palette] (any may be null):
 *   darkMutedSwatch, mutedSwatch, darkVibrantSwatch, vibrantSwatch, lightVibrantSwatch,
 *   lightMutedSwatch, dominantSwatch. Each has `.rgb` (Int) and `.population`.
 * Convert with `Color(swatch.rgb)`. Use [darkenForText] so the background keeps white text
 * readable, and fall back to [Fallback] when nothing fits.
 */
internal fun pickBookPalette(palette: Palette): BookPalette {
    // Background: the cover's rich dark colour (like the design's navy / burgundy / plum),
    // falling back to muted tones; always darkened so white text stays readable.
    val bgSwatch = palette.darkVibrantSwatch ?: palette.darkMutedSwatch
        ?: palette.mutedSwatch ?: palette.dominantSwatch ?: return Fallback
    val background = liftToDesignDepth(darkenForText(Color(bgSwatch.rgb)))

    // Accent: the brightest swatch that clearly separates from the background.
    val accent = listOfNotNull(
        palette.vibrantSwatch,
        palette.lightVibrantSwatch,
        palette.lightMutedSwatch,
        palette.mutedSwatch,
    )
        .map { Color(it.rgb) }
        .firstOrNull { it.luminance() - background.luminance() >= MIN_ACCENT_CONTRAST && !isGrey(it) }
        // Greyscale covers (common for manga) keep their own dark tone with the default gold.
        ?: Fallback.accent

    return BookPalette(background = background, accent = accent)
}

/**
 * Very dark swatches read as black; lift them toward the design's navy (#1C2B4A, luminance
 * about 0.024) so the cover's hue still shows. Never goes above the text-safe limit.
 */
internal fun liftToDesignDepth(color: Color, minLuminance: Float = 0.024f): Color {
    var c = color
    repeat(12) {
        if (c.luminance() >= minLuminance || maxOf(c.red, c.green, c.blue) >= 1f) return c
        c = Color(
            red = (c.red * 1.15f).coerceAtMost(1f),
            green = (c.green * 1.15f).coerceAtMost(1f),
            blue = (c.blue * 1.15f).coerceAtMost(1f),
            alpha = 1f,
        )
    }
    return c
}

/** Minimum luminance gap between accent and background so progress bars stay visible. */
private const val MIN_ACCENT_CONTRAST = 0.25f

/** True for near-grey colours (low saturation), which make a dull accent. */
private fun isGrey(c: Color): Boolean {
    val max = maxOf(c.red, c.green, c.blue)
    val min = minOf(c.red, c.green, c.blue)
    return max == 0f || (max - min) / max < 0.2f
}

/**
 * Loads [cover] (anything Coil accepts, e.g. a MangaCover) at a small size and extracts its
 * [BookPalette]. Returns null until loaded, or when the cover can't be read.
 */
@androidx.compose.runtime.Composable
fun rememberBookPalette(cover: Any?): BookPalette? {
    val context = androidx.compose.ui.platform.LocalContext.current
    return androidx.compose.runtime.produceState<BookPalette?>(initialValue = null, cover) {
        if (cover == null) {
            value = null
            return@produceState
        }
        value = runCatching {
            val request = coil3.request.ImageRequest.Builder(context)
                .data(cover)
                .size(128)
                // Palette reads pixels, which hardware bitmaps don't allow.
                .allowHardware(false)
                .build()
            val drawable = coil3.SingletonImageLoader.get(context).execute(request).image
                ?.asDrawable(context.resources)
            val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                ?: drawable?.toBitmap(128, 192)
            bitmap?.let { extractBookPalette(it) }
        }.getOrNull()
    }.value
}
