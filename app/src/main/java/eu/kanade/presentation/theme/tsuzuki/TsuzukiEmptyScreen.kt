package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tachiyomi.presentation.core.screens.EmptyScreenStyle

/**
 * V8.2 (board `docs/design/boards/remaining-2026-11/Main.dc.html`): the kaomoji upright at 30 sp in the body face
 * (Space Mono in Dot, Manrope in Colour) instead of the display face, whose italic serif breaks its strokes; the
 * message 15 sp bold. Legacy themes keep upstream's EmptyScreen.
 */
@Composable
internal fun tsuzukiEmptyScreenStyle(tokens: TsuzukiTokens): EmptyScreenStyle? {
    if (tokens.style == TsuzukiStyle.NONE) return null
    val body = MaterialTheme.typography.bodyLarge
    return EmptyScreenStyle(
        face = body.copy(fontSize = 30.sp, lineHeight = 36.sp, fontStyle = FontStyle.Normal),
        message = body.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
    )
}
