package eu.kanade.presentation.theme.tsuzuki

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.ceil

/**
 * The MotionSpec board: curves, durations, and how Colour and Dot differ.
 * - Colour and Dot use smooth curves with the same timings.
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

    // Owner 2026-11-14: details must fully open within 300 ms, including preparation.
    // Reserve 100 ms for navigation/composition; the device budget still needs verification.
    const val DETAILS_OPEN_MS = 200
    const val THEME_MS = 600
    const val REDUCED_MS = 150

    /** For discrete fills only; [steps] is the number of dots being lit. */
    fun stepped(easing: Easing, steps: Int): Easing {
        require(steps > 0) { "A discrete fill needs at least one dot" }
        return Easing { fraction ->
            if (fraction >= 1f) 1f else ceil(easing.transform(fraction).coerceIn(0f, 1f) * steps) / steps
        }
    }
}

/** Reduced when the system animator scale is 0 or the app-level "Reduce motion" setting is on. */
internal fun isMotionReduced(systemScaleIsZero: Boolean, appPreference: Boolean): Boolean =
    systemScaleIsZero || appPreference

private fun systemScaleIsZero(resolver: android.content.ContentResolver) =
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

private fun appReduceMotionPref() =
    runCatching { Injekt.get<eu.kanade.domain.ui.UiPreferences>().tsuzukiReduceMotion }.getOrNull()

/** Non-composable check for Activity transitions and the theme reveal. */
fun tsuzukiMotionReduced(context: android.content.Context): Boolean =
    isMotionReduced(systemScaleIsZero(context.contentResolver), appReduceMotionPref()?.get() ?: false)

/** True when motion is reduced: the system's animator scale is 0 (Developer options or Accessibility) or the app setting is on. */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    val resolver = context.contentResolver
    var systemReduced by remember(resolver) { mutableStateOf(systemScaleIsZero(resolver)) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                systemReduced = systemScaleIsZero(resolver)
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    val pref = remember { appReduceMotionPref() }
    val app = if (pref != null) {
        val state by remember(pref) { pref.changes() }.collectAsState(initial = pref.get())
        state
    } else {
        false
    }
    return isMotionReduced(systemReduced, app)
}

/** Smooth in both themes; reduced motion uses the existing short linear fade. */
internal fun <T> motionTween(durationMs: Int, easing: Easing, reduced: Boolean): TweenSpec<T> =
    if (reduced) tween(TsuzukiMotion.REDUCED_MS, easing = LinearEasing) else tween(durationMs, easing = easing)

@Composable
fun <T> tsuzukiTween(durationMs: Int, easing: Easing = TsuzukiMotion.Standard): AnimationSpec<T> {
    val reduced = rememberReduceMotion()
    return remember(durationMs, easing, reduced) { motionTween(durationMs, easing, reduced) }
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
