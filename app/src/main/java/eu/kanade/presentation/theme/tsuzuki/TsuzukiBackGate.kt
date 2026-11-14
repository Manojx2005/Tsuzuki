package eu.kanade.presentation.theme.tsuzuki

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator

/**
 * While a screen is being pushed, the screen underneath stays composed until the transition ends. A
 * `BackHandler` that was registered after the navigator's own one (any screen composed again after a
 * pop) then wins the system Back, so it must only act while its screen is the navigator's top screen.
 */
internal fun backHandlerActive(ownEnabled: Boolean, topKey: ScreenKey?, ownerKey: ScreenKey): Boolean =
    ownEnabled && (topKey == null || topKey == ownerKey)

/** `enabled` value for a `BackHandler` that belongs to [owner] (a tab host counts as its screen). */
@Composable
fun tsuzukiBackEnabled(ownEnabled: Boolean, owner: Screen): Boolean =
    backHandlerActive(ownEnabled, LocalNavigator.current?.lastItem?.key, owner.key)
