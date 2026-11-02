package eu.kanade.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.R

/*
 * Typography for the two Tsuzuki themes (docs/PLAN.md §3).
 *
 * Colour: Instrument Serif (italic) for display/headline, Manrope for everything else.
 * Dot:    Doto (dot-matrix) for display/headline, Space Mono for everything else.
 *
 * All fonts are SIL OFL 1.1 (docs/licenses/OFL-1.1.txt) and bundled in res/font.
 * Reading text is NOT affected: the novel reader keeps its own font setting.
 */

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

private val ColourTypography = Typography().withFonts(
    display = { copy(fontFamily = InstrumentSerif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal) },
    body = { copy(fontFamily = Manrope) },
)

private val DotTypography = Typography().withFonts(
    display = { copy(fontFamily = Doto, fontWeight = FontWeight.Black) },
    body = { copy(fontFamily = SpaceMono) },
)

/** Returns the typography for [appTheme], or null to keep Material's default fonts. */
internal fun lnrTypography(appTheme: AppTheme): Typography? = when (appTheme) {
    AppTheme.COLOUR -> ColourTypography
    AppTheme.DOT -> DotTypography
    else -> null
}
