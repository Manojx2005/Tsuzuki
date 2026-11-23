package eu.kanade.presentation.tsuzuki.anime

import android.app.Activity
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMotion
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.rememberReduceMotion
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMotionReduced
import eu.kanade.presentation.theme.tsuzuki.tsuzukiReaderTransition
import eu.kanade.tachiyomi.R

/**
 * Owner 2026-11-23: anime screens opened "differently" from books. Book details and the other book pages are Compose
 * screens that crossfade in the main navigator, while anime details, catalogues, migration and source settings are
 * their own Activities and got the system window animation. Give those windows the same crossfade (or the reader's
 * short fade with reduced motion), and the old shared-axis slide in the legacy themes, as WebView does.
 */
internal fun Activity.applyAnimeScreenTransition(opening: Boolean) {
    val (enter, exit) = animeScreenAnimations(this, opening)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        overrideActivityTransition(
            if (opening) Activity.OVERRIDE_TRANSITION_OPEN else Activity.OVERRIDE_TRANSITION_CLOSE,
            enter,
            exit,
        )
    } else {
        @Suppress("DEPRECATION")
        overridePendingTransition(enter, exit)
    }
}

private fun animeScreenAnimations(activity: Activity, opening: Boolean): Pair<Int, Int> = when {
    // tsuzukiReaderTransition is null outside Colour and Dot.
    tsuzukiReaderTransition(activity, opening) == null -> if (opening) {
        R.anim.shared_axis_x_push_enter to R.anim.shared_axis_x_push_exit
    } else {
        R.anim.shared_axis_x_pop_enter to R.anim.shared_axis_x_pop_exit
    }
    tsuzukiMotionReduced(activity) -> R.anim.tsuzuki_reader_fade_in to R.anim.tsuzuki_reader_fade_out
    else -> R.anim.tsuzuki_screen_fade_in to R.anim.tsuzuki_screen_fade_out
}

/**
 * Anime details shown over its catalogue inside one Activity: fade in and out on the book screens' timing
 * (TsuzukiNavigatorTransition), instantly in the legacy themes. Starts shown without animating when [visible] is
 * already true. The layer takes every touch so nothing reaches the catalogue underneath.
 */
@Composable
internal fun AnimeScreenOverlay(visible: Boolean, content: @Composable () -> Unit) {
    val tsuzuki = TsuzukiTheme.tokens.style != TsuzukiStyle.NONE
    val duration = if (rememberReduceMotion()) TsuzukiMotion.REDUCED_MS else TsuzukiMotion.SHARED_MS
    val fade = tween<Float>(duration, easing = TsuzukiMotion.Standard)
    AnimatedVisibility(
        visible = visible,
        enter = if (tsuzuki) fadeIn(fade) else EnterTransition.None,
        exit = if (tsuzuki) fadeOut(fade) else ExitTransition.None,
    ) {
        Box(
            // Opaque in every theme: the legacy themes' details background is transparent.
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
        ) { content() }
    }
}
