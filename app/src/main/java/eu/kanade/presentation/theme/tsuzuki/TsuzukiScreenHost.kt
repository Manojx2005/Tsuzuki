package eu.kanade.presentation.theme.tsuzuki

import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.CancellationException

/*
 * Batch SM5 (motion system approved by the owner 2026-11-28, docs/design/boards/motion-2026-11/README.md):
 * - push / pop: shared axis X — the new screen slides 30 dp and fades in after the old one has faded out (90 of
 *   300 ms); Back mirrors it;
 * - predictive Back: while the finger is down the page shrinks to 0.90, rounds to 28 dp and moves up to 24 dp toward
 *   the finger's side, the screen behind comes in from −30 dp at 55 % brightness; release past the commit point
 *   leaves sideways in 250 ms (Exit), an early release settles back in 250 ms (Enter);
 * - reduced motion: a plain crossfade.
 * Replaces ScreenTransition for the Tsuzuki themes only; the legacy themes keep Navigator.kt's shared axis.
 */

/** The gesture moves through the first 35 % of the seekable transition; the rest is the commit. */
private const val BACK_GESTURE_SPAN = 0.35f

/** Nominal length of the seekable Back transition; only its fractions matter (commit and cancel use 250 ms). */
private const val BACK_SPEC_MS = 1_000
private const val BACK_GESTURE_MS = (BACK_SPEC_MS * BACK_GESTURE_SPAN).toInt()
private const val BACK_SETTLE_MS = 250

@Composable
internal fun TsuzukiScreenHost(
    navigator: Navigator,
    durationMs: Int,
    reduced: Boolean,
    content: @Composable (Screen, androidx.compose.animation.AnimatedVisibilityScope) -> Unit,
) {
    val density = LocalDensity.current
    val slide = with(density) { 30.dp.roundToPx() }
    val drift = with(density) { 24.dp.roundToPx() }
    val behind = with(density) { 30.dp.roundToPx() }
    val behindAtCommit = with(density) { 18.dp.roundToPx() }
    val cornerPx = with(density) { 28.dp.toPx() }

    val state = remember { SeekableTransitionState(navigator.lastItem) }
    val transition = rememberTransition(state, label = "tsuzukiScreens")
    var gesture by remember { mutableStateOf(false) }
    var fromRightEdge by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    // Ordinary navigation (taps, the Back button and key): follow the navigator.
    LaunchedEffect(navigator.lastItem) {
        val target = navigator.lastItem
        if (!gesture && (state.currentState != target || state.targetState != target)) state.animateTo(target)
    }

    transition.AnimatedContent(
        contentKey = { it.key },
        transitionSpec = {
            when {
                reduced -> fadeIn(tween(TsuzukiMotion.REDUCED_MS)) togetherWith fadeOut(tween(TsuzukiMotion.REDUCED_MS))
                gesture -> predictiveBack(fromRightEdge, drift, behind, behindAtCommit)
                else -> sharedAxisX(
                    forward = navigator.lastEvent != StackEvent.Pop,
                    durationMs = durationMs,
                    slide = slide,
                )
            }
        },
    ) { screen ->
        val leaving = gesture && screen.key == state.currentState.key
        val shape = Modifier.graphicsLayer {
            if (leaving) {
                this.shape = RoundedCornerShape(cornerPx * progress.coerceIn(0f, 1f))
                clip = true
            }
        }
        androidx.compose.foundation.layout.Box(shape) { content(screen, this@AnimatedContent) }
    }

    // Reduced motion has no gesture animation: Back pops at once (as ScreenTransition's BackHandler did).
    BackHandler(enabled = navigator.canPop && reduced, onBack = navigator::pop)
    PredictiveBackHandler(enabled = navigator.canPop && !reduced) { events ->
        val current = navigator.lastItem
        val previous = navigator.items.getOrNull(navigator.items.size - 2) ?: return@PredictiveBackHandler
        // A Back key or button arrives with no progress events: that is an ordinary pop (300 ms shared axis X),
        // so the gesture look starts only with the first progress event.
        var seeking = false
        try {
            events.collect { event ->
                if (!seeking) {
                    seeking = true
                    gesture = true
                }
                fromRightEdge = event.swipeEdge == BackEventCompat.EDGE_RIGHT
                progress = event.progress
                state.seekTo(event.progress * BACK_GESTURE_SPAN, targetState = previous)
            }
            // Released past the commit point (or flicked): the page leaves, then the navigator follows.
            if (seeking) state.animateTo(previous, tween(BACK_SETTLE_MS, easing = TsuzukiMotion.Exit))
            navigator.pop()
        } catch (e: CancellationException) {
            if (seeking) state.animateTo(current, tween(BACK_SETTLE_MS, easing = TsuzukiMotion.Enter))
            throw e
        } finally {
            gesture = false
            progress = 0f
        }
    }
}

private fun sharedAxisX(forward: Boolean, durationMs: Int, slide: Int): ContentTransform {
    // The old screen is gone in the first 30 % (90 of 300 ms); the new one fades in over the rest.
    val outMs = durationMs * 3 / 10
    val enter =
        slideInHorizontally(tween(durationMs, easing = TsuzukiMotion.Enter)) { if (forward) slide else -slide } +
            fadeIn(tween(durationMs - outMs, delayMillis = outMs, easing = LinearEasing))
    val exit =
        slideOutHorizontally(tween(durationMs, easing = TsuzukiMotion.Enter)) { if (forward) -slide else slide } +
            fadeOut(tween(outMs, easing = LinearEasing))
    return enter togetherWith exit
}

private fun predictiveBack(fromRightEdge: Boolean, drift: Int, behind: Int, behindAtCommit: Int): ContentTransform {
    val toward = if (fromRightEdge) -1 else 1
    // The page behind: from −30 dp at 55 % to −18 dp at 75 % while the finger is down, then settles.
    val enter: EnterTransition =
        slideInHorizontally(
            keyframes<IntOffset> {
                durationMillis = BACK_SPEC_MS
                IntOffset(-behind * toward, 0) at 0 using LinearEasing
                IntOffset(-behindAtCommit * toward, 0) at BACK_GESTURE_MS using TsuzukiMotion.Exit
            },
        ) { -behind * toward } +
            fadeIn(
                keyframes<Float> {
                    durationMillis = BACK_SPEC_MS
                    0.55f at 0 using LinearEasing
                    0.75f at BACK_GESTURE_MS using TsuzukiMotion.Exit
                },
                initialAlpha = 0.55f,
            )
    // The page leaving: shrinks to 0.90 and drifts 24 dp with the finger, then slides out on commit.
    val exit: ExitTransition =
        scaleOut(
            keyframes<Float> {
                durationMillis = BACK_SPEC_MS
                1f at 0 using LinearEasing
                0.9f at BACK_GESTURE_MS
            },
            targetScale = 0.9f,
        ) +
            slideOutHorizontally(
                keyframes<IntOffset> {
                    durationMillis = BACK_SPEC_MS
                    IntOffset(0, 0) at 0 using LinearEasing
                    IntOffset(drift * toward, 0) at BACK_GESTURE_MS using TsuzukiMotion.Exit
                },
            ) { width -> width * toward }
    // The leaving page draws above the page behind (AnimatedContent puts the target on top by default).
    return (enter togetherWith exit).apply { targetContentZIndex = -1f }
}
