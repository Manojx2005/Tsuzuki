package eu.kanade.presentation.tsuzuki.category

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.tsuzuki.anime.AnimeShelvesScreen
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * More › Categories edits book shelves; while anime is on, its top bar also links to the anime shelves (AY-INT
 * item 5), the same screen Settings › Library and the anime library open.
 */
@Composable
internal fun TsuzukiAnimeShelvesAction() {
    if (!TsuzukiAnime.enabled) return
    val navigator = LocalNavigator.current ?: return
    AppBarActions(
        listOf(
            AppBar.OverflowAction(
                title = stringResource(MR.strings.tsz_anime_shelves),
                onClick = { navigator.push(AnimeShelvesScreen()) },
            ),
        ),
    )
}
