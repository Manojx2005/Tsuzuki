package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Sheet-only scheme: Material3 uses defaultSpatial for show/settle and fastEffects for hide. */
private class SheetMotionScheme(
    private val base: MotionScheme,
    private val dot: Boolean,
    private val reduced: Boolean,
) : MotionScheme by base {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = when {
        reduced -> snap()
        dot -> tween(TsuzukiMotion.SHEET_MS, easing = TsuzukiMotion.stepped(TsuzukiMotion.Enter))
        else -> tween(TsuzukiMotion.SHEET_MS, easing = TsuzukiMotion.Spring)
    }

    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = when {
        reduced -> snap()
        dot -> tween(200, easing = TsuzukiMotion.stepped(TsuzukiMotion.Exit))
        else -> tween(200, easing = TsuzukiMotion.Exit)
    }
}

/**
 * Keep Material3's anchors, nested scrolling, accessibility and back handling. Button actions also wait
 * for hide to finish. Reduced motion snaps the anchors and fades the whole sheet for 150 real ms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TsuzukiModalBottomSheet(
    onDismissRequest: () -> Unit,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    showDragHandle: Boolean = true,
    maxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    content: @Composable ColumnScope.(close: (() -> Unit) -> Unit) -> Unit,
) {
    val reduced = rememberReduceMotion()
    val currentReduced by rememberUpdatedState(reduced)
    val dot = TsuzukiTheme.tokens.isDot
    val base = MaterialTheme.motionScheme
    val scheme = remember(base, dot, reduced) { SheetMotionScheme(base, dot, reduced) }
    val scope = rememberCoroutineScope()
    val alpha = remember { Animatable(if (reduced) 0f else 1f) }
    var closing by remember { mutableStateOf(false) }
    var allowHidden by remember { mutableStateOf(false) }
    val currentDismiss by rememberUpdatedState(onDismissRequest)
    // Bypass the system's zero duration scale only for the requested accessibility fade.
    val fadeClock = remember {
        object : MotionDurationScale {
            override val scaleFactor = 1f
        }
    }
    val fadeOut: (() -> Unit) -> Unit = { after ->
        if (!closing) {
            closing = true
            scope.launch {
                withContext(fadeClock) { alpha.animateTo(0f, tween(TsuzukiMotion.REDUCED_MS)) }
                allowHidden = true
                after()
            }
        }
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (currentReduced && target == SheetValue.Hidden && !allowHidden) {
                fadeOut { currentDismiss() }
                false
            } else {
                true
            }
        },
    )
    val close: (() -> Unit) -> Unit = { after ->
        if (reduced) {
            fadeOut(after)
        } else if (!closing) {
            closing = true
            scope.launch {
                try {
                    sheetState.hide()
                    if (!sheetState.isVisible) after()
                } finally {
                    closing = false
                }
            }
        }
    }
    LaunchedEffect(reduced) {
        if (reduced) {
            withContext(fadeClock) { alpha.animateTo(1f, tween(TsuzukiMotion.REDUCED_MS)) }
        } else {
            alpha.snapTo(1f)
        }
    }
    MaterialTheme(motionScheme = scheme) {
        ModalBottomSheet(
            onDismissRequest = { if (reduced) fadeOut { currentDismiss() } else currentDismiss() },
            modifier = Modifier.graphicsLayer { this.alpha = alpha.value },
            sheetState = sheetState,
            sheetMaxWidth = maxWidth,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = containerColor,
            scrimColor = scrimColor.copy(alpha = scrimColor.alpha * alpha.value),
            dragHandle = if (showDragHandle) {
                { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant) }
            } else {
                null
            },
        ) {
            // Restore the surrounding scheme for switches and other controls inside the sheet.
            MaterialTheme(motionScheme = base) { content(close) }
        }
    }
}
