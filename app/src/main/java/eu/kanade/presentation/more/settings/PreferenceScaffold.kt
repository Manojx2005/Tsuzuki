package eu.kanade.presentation.more.settings

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun PreferenceScaffold(
    titleRes: StringResource,
    actions: @Composable RowScope.() -> Unit = {},
    onBackPressed: (() -> Unit)? = null,
    itemsProvider: @Composable () -> List<Preference>,
) {
    val themedContainer = eu.kanade.presentation.tsuzuki.settings.tsuzukiSettingsContainer()
    Scaffold(
        // Tsuzuki: theme background (dot grid in Dot) behind every settings screen
        modifier = eu.kanade.presentation.tsuzuki.settings.tsuzukiSettingsBackground(),
        containerColor = themedContainer
            ?: androidx.compose.material3.MaterialTheme.colorScheme.background,
        topBar = {
            AppBar(
                title = stringResource(titleRes),
                navigateUp = onBackPressed,
                actions = actions,
                scrollBehavior = it,
                backgroundFadeColor = themedContainer?.let { TsuzukiTheme.tokens.background },
            )
        },
        content = { contentPadding ->
            PreferenceScreen(
                items = itemsProvider(),
                contentPadding = contentPadding,
                settingsTitle = titleRes,
            )
        },
    )
}
