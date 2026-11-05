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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import kotlin.math.roundToInt

/** Keep Material's seeking and accessibility, with the reader boards' themed progress track. */
@Composable
fun TsuzukiReaderSliderTrack(state: SliderState, vertical: Boolean = false) {
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
    val rtl = !vertical && LocalLayoutDirection.current == LayoutDirection.Rtl
    val orientation = if (vertical) Modifier.verticalReaderTrack() else Modifier
    BoxWithConstraints(orientation.fillMaxWidth().clearAndSetSemantics {}) {
        TsuzukiProgressBar(
            progress = progress,
            dots = (maxWidth / 9.dp).roundToInt().coerceIn(2, 80),
            thickness = 6.dp,
            modifier = Modifier.graphicsLayer { scaleX = if (rtl) -1f else 1f },
        )
    }
}

@Composable
fun TsuzukiReaderSliderThumb(interactionSource: MutableInteractionSource, vertical: Boolean = false) {
    if (TsuzukiTheme.tokens.style == TsuzukiStyle.NONE) {
        SliderDefaults.Thumb(interactionSource = interactionSource)
    } else {
        // The current Dot marker / Colour fill already identifies the seek position.
        Spacer(
            if (vertical) {
                Modifier.size(width = 48.dp, height = 0.dp)
            } else {
                Modifier.size(width = 0.dp, height = 48.dp)
            },
        )
    }
}

// Swap the measured axes as well as rotating the drawing. VerticalSlider still owns the
// full-height track bounds used for seeking, with the minimum at the top.
private fun Modifier.verticalReaderTrack(): Modifier = layout { measurable, constraints ->
    val track = measurable.measure(
        Constraints(
            minWidth = constraints.minHeight,
            maxWidth = constraints.maxHeight,
            minHeight = constraints.minWidth,
            maxHeight = constraints.maxWidth,
        ),
    )
    layout(width = track.height, height = track.width) {
        track.placeWithLayer(
            x = (track.height - track.width) / 2,
            y = (track.width - track.height) / 2,
        ) {
            rotationZ = 90f
        }
    }
}
