package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import eu.kanade.tachiyomi.ui.home.HomeScreen

/**
 * While a screen is being pushed, the screen underneath stays composed until the transition ends. A
 * `BackHandler` that was registered after the navigator's own one (any screen composed again after a
 * pop) then wins the system Back, so it must only act while its screen is the navigator's top screen.
 */
internal fun backHandlerActive(
    ownEnabled: Boolean,
    topKey: ScreenKey?,
    ownerKey: ScreenKey,
    restoredOwnerKey: ScreenKey? = null,
): Boolean = ownEnabled && (topKey == null || topKey == (restoredOwnerKey ?: ownerKey))

/** `enabled` value for a `BackHandler` that belongs to [owner] (a tab host counts as its screen). */
@Composable
fun tsuzukiBackEnabled(ownEnabled: Boolean, owner: Screen): Boolean {
    val navigator = LocalNavigator.current
    // A restored HomeScreen can keep its saved key while the process singleton has a new one.
    // Resolve only this singleton host; distinct BrowseSourceScreen instances keep their own keys.
    val restoredOwnerKey = if (owner is HomeScreen) {
        navigator?.items?.firstOrNull { it is HomeScreen }?.key
    } else {
        null
    }
    return backHandlerActive(ownEnabled, navigator?.lastItem?.key, owner.key, restoredOwnerKey)
}
