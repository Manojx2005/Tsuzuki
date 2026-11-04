package eu.kanade.presentation.tsuzuki.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Mass import link check as the board's three counts (board 08-tools "MassImport"): valid links in the accent, then
 * links already in the library and links no installed source can open. The counts come from the dialog's existing
 * `MassImport.analyzeUrls`; zero counts after the first are left out.
 */
@Composable
fun TsuzukiMassImportCounts(valid: Int, inLibrary: Int, unsupported: Int, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CountPill(stringResource(MR.strings.tsz_mass_import_valid, valid), accent = true)
        if (inLibrary > 0) CountPill(stringResource(MR.strings.tsz_mass_import_in_library, inLibrary), accent = false)
        if (unsupported > 0) {
            CountPill(stringResource(MR.strings.tsz_mass_import_unsupported, unsupported), accent = false)
        }
    }
}

@Composable
private fun CountPill(text: String, accent: Boolean) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(if (t.isDot) 6.dp else 19.dp)
    Box(
        Modifier
            .height(36.dp)
            .clip(shape)
            .then(
                if (accent) {
                    Modifier.background(t.accent)
                } else {
                    Modifier
                        .background(t.tile)
                        .border(1.dp, t.tileBorder ?: Color.White.copy(alpha = 0.12f), shape)
                },
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (t.isDot) text.uppercase() else text,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (accent) t.onAccent else Color.White,
        )
    }
}

/**
 * Marks [unsupported] links in the accent inside the URL box (board "MassImport"); offsets are unchanged, so editing
 * works as before. No transformation in stock themes or when every link is supported.
 */
@Composable
fun tsuzukiUnsupportedLinks(unsupported: List<String>): androidx.compose.ui.text.input.VisualTransformation {
    val t = TsuzukiTheme.tokens
    if (t.style == eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle.NONE || unsupported.isEmpty()) {
        return androidx.compose.ui.text.input.VisualTransformation.None
    }
    val accent = t.accent
    return androidx.compose.ui.text.input.VisualTransformation { text ->
        val styled = androidx.compose.ui.text.buildAnnotatedString {
            append(text)
            unsupported.distinct().forEach { link ->
                var start = text.text.indexOf(link)
                while (start >= 0) {
                    addStyle(androidx.compose.ui.text.SpanStyle(color = accent), start, start + link.length)
                    start = text.text.indexOf(link, start + link.length)
                }
            }
        }
        androidx.compose.ui.text.input.TransformedText(styled, androidx.compose.ui.text.input.OffsetMapping.Identity)
    }
}
