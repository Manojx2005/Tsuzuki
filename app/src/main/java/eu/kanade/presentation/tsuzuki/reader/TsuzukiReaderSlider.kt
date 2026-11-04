package eu.kanade.presentation.tsuzuki.reader

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import kotlin.math.roundToInt

/** Keep Material's seeking and accessibility, with the reader boards' themed progress track. */
@Composable
fun TsuzukiReaderSliderTrack(state: SliderState) {
    if (TsuzukiTheme.tokens.style == TsuzukiStyle.NONE) {
        SliderDefaults.Track(sliderState = state)
        return
    }
    val range = state.valueRange
    val progress = if (range.endInclusive > range.start) {
        (state.value - range.start) / (range.endInclusive - range.start)
    } else {
        0f
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    BoxWithConstraints(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
        TsuzukiProgressBar(
            progress = progress,
            dots = (maxWidth / 9.dp).roundToInt().coerceIn(2, 80),
            thickness = 6.dp,
            modifier = Modifier.graphicsLayer { scaleX = if (rtl) -1f else 1f },
        )
    }
}

@Composable
fun TsuzukiReaderSliderThumb(interactionSource: MutableInteractionSource) {
    if (TsuzukiTheme.tokens.style == TsuzukiStyle.NONE) {
        SliderDefaults.Thumb(interactionSource = interactionSource)
    } else {
        // The current Dot marker / Colour fill already identifies the seek position.
        Spacer(Modifier.size(width = 0.dp, height = 48.dp))
    }
}
