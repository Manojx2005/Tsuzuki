package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen

object SettingsMainScreen : Screen() {

    @Composable
    override fun Content() {
        Content(twoPane = false)
    }

    @Composable
    private fun getPalerSurface(): Color {
        val surface = MaterialTheme.colorScheme.surface
        val dark = isSystemInDarkTheme()
        return remember(surface, dark) {
            val arr = FloatArray(3)
            ColorUtils.colorToHSL(surface.toArgb(), arr)
            arr[2] = if (dark) {
                arr[2] - 0.05f
            } else {
                arr[2] + 0.02f
            }.coerceIn(0f, 1f)
            Color.hsl(arr[0], arr[1], arr[2])
        }
    }

    @Composable
    fun Content(twoPane: Boolean) {
        val navigator = LocalNavigator.currentOrThrow
        val backPress = LocalBackPress.currentOrThrow
        val basePreferences = remember { Injekt.get<BasePreferences>() }
        val hideMangaUi by basePreferences.hideMangaUi.collectAsState()
        val containerColor = if (twoPane) getPalerSurface() else MaterialTheme.colorScheme.surface
        val topBarState = rememberTopAppBarState()
        val visibleItems = remember(hideMangaUi) {
            items.filterNot { hideMangaUi && it.isMangaOnly }
        }

        Scaffold(
            topBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState),
            topBar = topBar@{ scrollBehavior ->
                if (eu.kanade.presentation.tsuzuki.settings.isTsuzukiStyle()) {
                    eu.kanade.presentation.tsuzuki.settings.TsuzukiSettingsTitle(
                        title = stringResource(MR.strings.label_settings),
                        onBack = if (twoPane) null else backPress::invoke,
                    )
                    return@topBar
                }
                AppBar(
                    title = stringResource(MR.strings.label_settings),
                    navigateUp = backPress::invoke,
                    actions = {
                        AppBarActions(
                            listOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_search),
                                    icon = Icons.Outlined.Search,
                                    onClick = { navigator.navigate(SettingsSearchScreen(), twoPane) },
                                ),
                            ),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            containerColor = containerColor,
            content = { contentPadding ->
                val state = rememberLazyListState()
                val indexSelected = if (twoPane) {
                    visibleItems.indexOfFirst { it.screen::class == navigator.items.first()::class }
                        .also {
                            LaunchedEffect(Unit) {
                                if (it >= 0) {
                                    state.animateScrollToItem(it)
                                }
                                if (it > 0) {
                                    // Lift scroll
                                    topBarState.contentOffset = topBarState.heightOffsetLimit
                                }
                            }
                        }
                } else {
                    null
                }

                val tsuzuki = !twoPane &&
                    eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme.tokens.style !=
                    eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle.NONE
                LazyColumn(
                    state = state,
                    contentPadding = contentPadding,
                ) {
                    // Tsuzuki themes: search + theme cards, then every section in one tile (board "Settings").
                    if (tsuzuki) {
                        item(key = "tsuzuki_settings_header") {
                            val uiPreferences = remember { Injekt.get<eu.kanade.domain.ui.UiPreferences>() }
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val appTheme by uiPreferences.appTheme.collectAsState()
                            eu.kanade.presentation.tsuzuki.settings.TsuzukiSettingsHeader(
                                appTheme = appTheme,
                                onSearch = { navigator.navigate(SettingsSearchScreen(), twoPane) },
                                onPickTheme = {
                                    if (it != appTheme) {
                                        uiPreferences.appTheme.set(it)
                                        // Same as the Appearance picker: rebuild the activity in the new theme.
                                        (context as? android.app.Activity)?.let { a ->
                                            androidx.core.app.ActivityCompat.recreate(a)
                                        }
                                    }
                                },
                            )
                        }
                    }
                    itemsIndexed(
                        items = visibleItems,
                        key = { _, item -> item.hashCode() },
                    ) { index, item ->
                        val selected = indexSelected == index
                        if (eu.kanade.presentation.tsuzuki.settings.isTsuzukiStyle()) {
                            eu.kanade.presentation.tsuzuki.settings.TsuzukiSettingsSectionRow(
                                title = stringResource(item.titleRes),
                                subtitle = item.formatSubtitle(),
                                icon = item.icon,
                                twoPane = twoPane,
                                selected = selected,
                                onClick = { navigator.navigate(item.screen, twoPane) },
                                modifier = if (twoPane) {
                                    Modifier
                                } else {
                                    eu.kanade.presentation.tsuzuki.browse.tsuzukiGroupedTile(
                                        first = index == 0,
                                        last = index == visibleItems.lastIndex,
                                    )
                                },
                            )
                            return@itemsIndexed
                        }
                        var modifier: Modifier = if (tsuzuki) {
                            eu.kanade.presentation.tsuzuki.browse.tsuzukiGroupedTile(
                                first = index == 0,
                                last = index == visibleItems.lastIndex,
                            )
                        } else {
                            Modifier
                        }
                        var contentColor = LocalContentColor.current
                        val tsuzukiPane = eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme.tokens.style !=
                            eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle.NONE
                        if (twoPane) {
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .then(eu.kanade.presentation.tsuzuki.settings.settingsPaneSelection(selected))
                            if (selected && !tsuzukiPane) {
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        }
                        CompositionLocalProvider(LocalContentColor provides contentColor) {
                            TextPreferenceWidget(
                                modifier = modifier,
                                title = eu.kanade.presentation.tsuzuki.settings.tsuzukiCase(
                                    stringResource(item.titleRes),
                                ),
                                subtitle = eu.kanade.presentation.tsuzuki.settings.tsuzukiCase(item.formatSubtitle()),
                                icon = item.icon,
                                iconTint = eu.kanade.presentation.tsuzuki.settings.tsuzukiSectionIconTint()
                                    ?: MaterialTheme.colorScheme.primary,
                                onPreferenceClick = { navigator.navigate(item.screen, twoPane) },
                            )
                        }
                    }
                }
            },
        )
    }

    private fun Navigator.navigate(screen: VoyagerScreen, twoPane: Boolean) {
        if (twoPane) replaceAll(screen) else push(screen)
    }

    private data class Item(
        val titleRes: StringResource,
        val subtitleRes: StringResource? = null,
        val formatSubtitle: @Composable () -> String? = { subtitleRes?.let { stringResource(it) } },
        val icon: ImageVector,
        val screen: VoyagerScreen,
        val isMangaOnly: Boolean = false,
    )

    private val items = listOf(
        Item(
            titleRes = MR.strings.pref_category_appearance,
            subtitleRes = MR.strings.pref_appearance_summary,
            icon = Icons.Outlined.Palette,
            screen = SettingsAppearanceScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_library,
            subtitleRes = MR.strings.pref_library_summary,
            icon = Icons.Outlined.CollectionsBookmark,
            screen = SettingsLibraryScreen,
        ),
        Item(
            titleRes = TDMR.strings.pref_category_manga_reader,
            subtitleRes = TDMR.strings.pref_manga_reader_summary,
            icon = Icons.AutoMirrored.Outlined.ChromeReaderMode,
            screen = SettingsReaderScreen,
            isMangaOnly = true,
        ),
        Item(
            titleRes = TDMR.strings.pref_category_novel_reader,
            subtitleRes = TDMR.strings.pref_novel_reader_summary,
            icon = Icons.AutoMirrored.Outlined.ChromeReaderMode,
            screen = SettingsNovelReaderScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_downloads,
            subtitleRes = MR.strings.pref_downloads_summary,
            icon = Icons.Outlined.GetApp,
            screen = SettingsDownloadScreen,
        ),
        Item(
            titleRes = TDMR.strings.pref_category_novel_downloads,
            subtitleRes = TDMR.strings.pref_novel_request_throttling_summary,
            icon = Icons.Outlined.GetApp,
            screen = SettingsNovelDownloadScreen,
        ),
        Item(
            titleRes = TDMR.strings.pref_category_translation,
            subtitleRes = TDMR.strings.pref_translation_enabled_summary,
            icon = Icons.Outlined.Translate,
            screen = SettingsTranslationScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_tracking,
            subtitleRes = MR.strings.pref_tracking_summary,
            icon = Icons.Outlined.Sync,
            screen = SettingsTrackingScreen,
        ),
        Item(
            titleRes = MR.strings.browse,
            subtitleRes = MR.strings.pref_browse_summary,
            icon = Icons.Outlined.Explore,
            screen = SettingsBrowseScreen,
        ),
        Item(
            titleRes = MR.strings.label_data_storage,
            subtitleRes = MR.strings.pref_backup_summary,
            icon = Icons.Outlined.Storage,
            screen = SettingsDataScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_security,
            subtitleRes = MR.strings.pref_security_summary,
            icon = Icons.Outlined.Security,
            screen = SettingsSecurityScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_advanced,
            subtitleRes = MR.strings.pref_advanced_summary,
            icon = Icons.Outlined.Code,
            screen = SettingsAdvancedScreen,
        ),
        Item(
            titleRes = MR.strings.pref_category_about,
            formatSubtitle = {
                "${stringResource(MR.strings.app_name)} ${AboutScreen.getVersionName(withBuildDate = false)}"
            },
            icon = Icons.Outlined.Info,
            screen = AboutScreen,
        ),
    )
}
