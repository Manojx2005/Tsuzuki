package eu.kanade.presentation.theme

import android.content.Context
import android.content.res.Resources
import android.graphics.fonts.FontStyle.FONT_SLANT_ITALIC
import android.graphics.fonts.FontStyle.FONT_SLANT_UPRIGHT
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.R
import android.graphics.Typeface as AndroidTypeface
import android.graphics.fonts.Font as AndroidFont
import android.graphics.fonts.FontFamily as AndroidFontFamily
import androidx.compose.ui.text.font.AndroidFont as ComposeAndroidFont
import androidx.compose.ui.text.font.Typeface as ComposeTypeface

/*
 * Typography for the two Tsuzuki themes (docs/PLAN.md §3).
 *
 * Colour: Instrument Serif (italic) for display/headline, Manrope for everything else.
 * Dot:    Doto (dot-matrix) for display/headline, Space Mono for everything else.
 *
 * None of those have Japanese glyphs, so each is paired with a Japanese font in the same spirit,
 * used only for characters the Latin font lacks (Android 10+, via Typeface.CustomFallbackBuilder):
 *   Doto → DotGothic16 · Space Mono → M PLUS 1 Code · Instrument Serif → Noto Serif JP ·
 *   Manrope → Zen Kaku Gothic New. Rare kanji outside JIS X 0208 fall back to the system font.
 *
 * All fonts are SIL OFL 1.1 (docs/licenses/) and bundled in res/font. The Japanese ones are
 * subset to kana, CJK punctuation, full-width forms and JIS X 0208 kanji.
 * Reading text is NOT affected: the novel reader keeps its own font setting.
 */

// ---------------------------------------------------------------- Compose-only families (Android 8–9)

private fun variable(resId: Int, weight: Int) = Font(
    resId = resId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val Manrope = FontFamily(
    variable(R.font.lnr_manrope, 400),
    variable(R.font.lnr_manrope, 500),
    variable(R.font.lnr_manrope, 600),
    variable(R.font.lnr_manrope, 700),
    variable(R.font.lnr_manrope, 800),
)

private val InstrumentSerif = FontFamily(
    Font(R.font.lnr_instrument_serif, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.lnr_instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

private val SpaceMono = FontFamily(
    Font(R.font.lnr_space_mono_regular, FontWeight.Normal),
    Font(R.font.lnr_space_mono_bold, FontWeight.Bold),
)

private val Doto = FontFamily(
    variable(R.font.lnr_doto, 700),
    variable(R.font.lnr_doto, 900),
)

// ---------------------------------------------------------------- Latin + Japanese (Android 10+)

@RequiresApi(Build.VERSION_CODES.Q)
private class FontLoader(private val res: Resources) {
    fun font(id: Int, weight: Int, italic: Boolean = false, variable: Boolean = false): AndroidFont =
        AndroidFont.Builder(res, id)
            .setWeight(weight)
            .setSlant(if (italic) FONT_SLANT_ITALIC else FONT_SLANT_UPRIGHT)
            .apply { if (variable) setFontVariationSettings("'wght' $weight") }
            .build()

    fun family(vararg fonts: AndroidFont): AndroidFontFamily =
        AndroidFontFamily.Builder(fonts.first()).apply { fonts.drop(1).forEach(::addFont) }.build()

    /** A Compose family: Latin first, the Japanese partner next, then the system [system] family. */
    fun paired(latin: AndroidFontFamily, japanese: AndroidFontFamily, system: String): FontFamily =
        FontFamily(
            ComposeTypeface(
                AndroidTypeface.CustomFallbackBuilder(latin)
                    .addCustomFallback(japanese)
                    .setSystemFallback(system)
                    .build(),
            ),
        )

    /**
     * Like [paired], but one fallback typeface per weight in [weights], so bold text is bold. Compose's
     * `FontFamily(Typeface)` always draws its single typeface and ignores the requested weight.
     */
    fun pairedWeights(
        latin: AndroidFontFamily,
        japanese: AndroidFontFamily,
        system: String,
        weights: List<Int>,
    ): FontFamily = FontFamily(
        weights.map { weight ->
            TypefaceFont(
                FontWeight(weight),
                AndroidTypeface.CustomFallbackBuilder(latin)
                    .addCustomFallback(japanese)
                    .setSystemFallback(system)
                    .setStyle(android.graphics.fonts.FontStyle(weight, FONT_SLANT_UPRIGHT))
                    .build(),
            )
        },
    )
}

/** A ready-made Android typeface as one weight of a Compose [FontFamily]. */
@OptIn(ExperimentalTextApi::class)
private class TypefaceFont(
    override val weight: FontWeight,
    val typeface: AndroidTypeface,
) : ComposeAndroidFont(FontLoadingStrategy.Blocking, Loader, FontVariation.Settings()) {
    override val style: FontStyle = FontStyle.Normal

    private object Loader : TypefaceLoader {
        override fun loadBlocking(context: Context, font: ComposeAndroidFont): AndroidTypeface =
            (font as TypefaceFont).typeface

        override suspend fun awaitLoad(context: Context, font: ComposeAndroidFont): AndroidTypeface =
            (font as TypefaceFont).typeface
    }
}

/** Dot: Doto + DotGothic16 for display, Space Mono + M PLUS 1 Code for body. */
@RequiresApi(Build.VERSION_CODES.Q)
private fun buildDotFamilies(res: Resources): Pair<FontFamily, FontFamily> = with(FontLoader(res)) {
    val display = paired(
        family(font(R.font.lnr_doto, 700, variable = true), font(R.font.lnr_doto, 900, variable = true)),
        family(font(R.font.lnr_jp_dotgothic16, 400)),
        "monospace",
    )
    val body = pairedWeights(
        family(font(R.font.lnr_space_mono_regular, 400), font(R.font.lnr_space_mono_bold, 700)),
        family(
            font(R.font.lnr_jp_mplus1code, 400, variable = true),
            font(R.font.lnr_jp_mplus1code, 700, variable = true),
        ),
        "monospace",
        weights = listOf(400, 700),
    )
    display to body
}

/** Colour: Instrument Serif + Noto Serif JP for display, Manrope + Zen Kaku Gothic New for body. */
@RequiresApi(Build.VERSION_CODES.Q)
private fun buildColourFamilies(res: Resources): Pair<FontFamily, FontFamily> = with(FontLoader(res)) {
    val display = paired(
        family(
            font(R.font.lnr_instrument_serif, 400),
            font(R.font.lnr_instrument_serif_italic, 400, italic = true),
        ),
        family(font(R.font.lnr_jp_notoserif, 600)),
        "serif",
    )
    val body = pairedWeights(
        family(
            font(R.font.lnr_manrope, 400, variable = true),
            font(R.font.lnr_manrope, 500, variable = true),
            font(R.font.lnr_manrope, 600, variable = true),
            font(R.font.lnr_manrope, 700, variable = true),
            font(R.font.lnr_manrope, 800, variable = true),
        ),
        family(font(R.font.lnr_jp_zenkaku_regular, 400), font(R.font.lnr_jp_zenkaku_bold, 700)),
        "sans-serif",
        weights = listOf(400, 500, 600, 700, 800),
    )
    display to body
}

// ---------------------------------------------------------------- Typography

private fun Typography.withFonts(
    display: TextStyle.() -> TextStyle,
    body: TextStyle.() -> TextStyle,
) = copy(
    displayLarge = displayLarge.display(),
    displayMedium = displayMedium.display(),
    displaySmall = displaySmall.display(),
    headlineLarge = headlineLarge.display(),
    headlineMedium = headlineMedium.display(),
    headlineSmall = headlineSmall.display(),
    titleLarge = titleLarge.body(),
    titleMedium = titleMedium.body(),
    titleSmall = titleSmall.body(),
    bodyLarge = bodyLarge.body(),
    bodyMedium = bodyMedium.body(),
    bodySmall = bodySmall.body(),
    labelLarge = labelLarge.body(),
    labelMedium = labelMedium.body(),
    labelSmall = labelSmall.body(),
)

private fun colourTypography(serif: FontFamily, sans: FontFamily) = Typography().withFonts(
    display = { copy(fontFamily = serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal) },
    body = { copy(fontFamily = sans) },
)

private fun dotTypography(display: FontFamily, mono: FontFamily) = Typography().withFonts(
    display = { copy(fontFamily = display, fontWeight = FontWeight.Black) },
    body = { copy(fontFamily = mono) },
)

/** Built once per process and theme; falls back to the Latin-only fonts if loading fails. */
private object TsuzukiTypographies {
    @Volatile private var colour: Typography? = null

    @Volatile private var dot: Typography? = null

    fun colour(res: Resources): Typography = colour ?: synchronized(this) {
        colour ?: run {
            val paired = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { buildColourFamilies(res) }.getOrNull()
            } else {
                null
            }
            val (display, body) = paired ?: (InstrumentSerif to Manrope)
            colourTypography(display, body).also { colour = it }
        }
    }

    fun dot(res: Resources): Typography = dot ?: synchronized(this) {
        dot ?: run {
            val paired = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { buildDotFamilies(res) }.getOrNull()
            } else {
                null
            }
            val (display, body) = paired ?: (Doto to SpaceMono)
            dotTypography(display, body).also { dot = it }
        }
    }
}

/** Returns the typography for [appTheme], or null to keep Material's default fonts. */
internal fun lnrTypography(appTheme: AppTheme, res: Resources): Typography? = when (appTheme) {
    AppTheme.COLOUR -> TsuzukiTypographies.colour(res)
    AppTheme.DOT -> TsuzukiTypographies.dot(res)
    else -> null
}
