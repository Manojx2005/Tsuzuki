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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.util.ScreenTransition

@OptIn(ExperimentalSharedTransitionApi::class)
private val LocalSharedCoverScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
private val LocalCoverVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Both outgoing and incoming Voyager screens stay hosted by the same shared transition layout. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TsuzukiNavigatorTransition(navigator: Navigator, modifier: Modifier = Modifier) {
    val reduced = rememberReduceMotion()
    // Full-screen fades stay smooth in Dot; stepping is kept for small UI (pills, toggles).
    val easing = TsuzukiMotion.Standard
    val fade = tween<Float>(if (reduced) TsuzukiMotion.REDUCED_MS else TsuzukiMotion.SHARED_MS, easing = easing)
    SharedTransitionLayout(modifier) {
        CompositionLocalProvider(LocalSharedCoverScope provides this) {
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
    // Movement stays smooth in Dot too: an 8-step path read as ~18 fps lag (owner, 2026-11-06).
    val easing = TsuzukiMotion.Standard
    return with(shared) {
        this@tsuzukiSharedCover.sharedElement(
            sharedContentState = rememberSharedContentState("tsuzuki-cover-$mangaId"),
            animatedVisibilityScope = visibility,
            boundsTransform = { _, _ -> tween<Rect>(TsuzukiMotion.SHARED_MS, easing = easing) },
        )
    }
}
