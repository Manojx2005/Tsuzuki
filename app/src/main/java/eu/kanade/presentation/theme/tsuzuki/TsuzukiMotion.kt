package eu.kanade.presentation.theme.tsuzuki

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlin.math.ceil

/**
 * The MotionSpec board: curves, durations, and how Colour and Dot differ.
 * - Colour is smooth. Dot keeps the same timings but advances in [DOT_STEPS] steps ("dot by dot").
 * - With animations turned off in the system (reduce motion), every transition is a [REDUCED_MS] fade.
 * - Nothing runs longer than [THEME_MS].
 */
object TsuzukiMotion {
    val Enter: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val Exit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** For small pops; overshoots past 1. */
    val Spring: Easing = CubicBezierEasing(0.34f, 1.4f, 0.64f, 1f)

    const val PRESS_MS = 100
    const val TOGGLE_MS = 200
    const val SHEET_MS = 300
    const val STACK_MS = 400
    const val SHARED_MS = 450
    const val THEME_MS = 600
    const val REDUCED_MS = 150

    const val DOT_STEPS = 8

    /** Quantises [easing] into [steps] equal jumps, for Dot's dot-by-dot fills. */
    fun stepped(easing: Easing, steps: Int = DOT_STEPS): Easing = Easing { fraction ->
        if (fraction >= 1f) 1f else ceil(easing.transform(fraction) * steps) / steps
    }
}

/** True when the system's animator scale is 0 (Developer options or Accessibility "Remove animations"). */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    val resolver = context.contentResolver
    fun isReduced() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    var reduced by remember(resolver) { mutableStateOf(isReduced()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = isReduced()
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}

/**
 * A tween following the MotionSpec rules for the current theme: [durationMs] with [easing] in Colour, stepped in
 * Dot, and a short linear fade when motion is reduced.
 */
@Composable
fun <T> tsuzukiTween(durationMs: Int, easing: Easing = TsuzukiMotion.Standard): AnimationSpec<T> {
    val dot = TsuzukiTheme.tokens.isDot
    val reduced = rememberReduceMotion()
    return remember(durationMs, easing, dot, reduced) {
        when {
            reduced -> tween(TsuzukiMotion.REDUCED_MS)
            dot -> tween(durationMs, easing = TsuzukiMotion.stepped(easing))
            else -> tween(durationMs, easing = easing)
        }
    }
}

/**
 * MotionSpec press feedback: scales to 0.97 over 100 ms while [interactionSource] is pressed. Put it first in the
 * modifier chain (so the background scales too) and pass the same source to `clickable`.
 */
@Composable
fun Modifier.tsuzukiPress(interactionSource: androidx.compose.foundation.interaction.InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed && !rememberReduceMotion()) 0.97f else 1f,
        animationSpec = tween(TsuzukiMotion.PRESS_MS, easing = TsuzukiMotion.Standard),
        label = "press",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Prefix the visual scale before the supplied tile modifiers and share its source with click/long-click. */
@Composable
fun Modifier.tsuzukiClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: androidx.compose.ui.semantics.Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    if (TsuzukiTheme.tokens.style == TsuzukiStyle.NONE) {
        return if (onLongClick != null) {
            combinedClickable(
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = role,
                onLongClickLabel = onLongClickLabel,
                onLongClick = onLongClick,
                onClick = onClick,
            )
        } else {
            clickable(enabled = enabled, onClickLabel = onClickLabel, role = role, onClick = onClick)
        }
    }
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val tile = Modifier.tsuzukiPress(source).then(this)
    return if (onLongClick != null) {
        tile.combinedClickable(
            interactionSource = source,
            indication = androidx.compose.material3.ripple(),
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onClick = onClick,
        )
    } else {
        tile.clickable(
            interactionSource = source,
            indication = androidx.compose.material3.ripple(),
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
    }
}
