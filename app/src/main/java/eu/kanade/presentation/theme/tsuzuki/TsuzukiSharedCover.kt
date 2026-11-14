package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.util.ScreenTransition
import eu.kanade.tachiyomi.ui.manga.MangaScreen

@OptIn(ExperimentalSharedTransitionApi::class)
private val LocalSharedCoverScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
private val LocalCoverVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }
// Dynamic on purpose: a static local would recompose both screens unskipped when the duration changes.
private val LocalCoverDuration = compositionLocalOf { TsuzukiMotion.SHARED_MS }

internal fun navigatorCoverDuration(isDetails: Boolean, event: StackEvent, reduced: Boolean): Int = when {
    reduced -> TsuzukiMotion.REDUCED_MS
    isDetails && event != StackEvent.Pop -> TsuzukiMotion.DETAILS_OPEN_MS
    else -> TsuzukiMotion.SHARED_MS
}

/** Both outgoing and incoming Voyager screens stay hosted by the same shared transition layout. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TsuzukiNavigatorTransition(navigator: Navigator, modifier: Modifier = Modifier) {
    val reduced = rememberReduceMotion()
    val duration = navigatorCoverDuration(navigator.lastItem is MangaScreen, navigator.lastEvent, reduced)
    // Full-screen fades use the same smooth curve in both themes.
    val easing = TsuzukiMotion.Standard
    val fade = tween<Float>(duration, easing = easing)
    SharedTransitionLayout(modifier) {
        CompositionLocalProvider(LocalSharedCoverScope provides this, LocalCoverDuration provides duration) {
            ScreenTransition(
                navigator = navigator,
                transition = { fadeIn(fade) togetherWith fadeOut(fade) },
            )
        }
    }
}

/** Small slot in the existing AnimatedContent; no navigation graph or saved-state behavior changes. */
@Composable
fun TsuzukiAnimatedScreen(scope: AnimatedVisibilityScope, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCoverVisibilityScope provides scope, content = content)
}

/** Apply only to a source cover and its details cover, never repeated decorative/stack thumbnails. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.tsuzukiSharedCover(mangaId: Long): Modifier {
    if (mangaId <= 0 || TsuzukiTheme.tokens.style == TsuzukiStyle.NONE || rememberReduceMotion()) return this
    val shared = LocalSharedCoverScope.current ?: return this
    val visibility = LocalCoverVisibilityScope.current ?: return this
    val duration = LocalCoverDuration.current
    // Movement stays smooth in Dot too: an 8-step path read as ~18 fps lag (owner, 2026-11-06).
    val easing = TsuzukiMotion.Standard
    return with(shared) {
        this@tsuzukiSharedCover.sharedElement(
            sharedContentState = rememberSharedContentState("tsuzuki-cover-$mangaId"),
            animatedVisibilityScope = visibility,
            boundsTransform = { _, _ -> tween<Rect>(duration, easing = easing) },
        )
    }
}
