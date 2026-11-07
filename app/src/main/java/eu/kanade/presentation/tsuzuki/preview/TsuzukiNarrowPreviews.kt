package eu.kanade.presentation.tsuzuki.preview

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.presentation.manga.components.ChapterHeader
import eu.kanade.presentation.manga.components.MangaActionRow
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiNavBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiNavItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.tsuzuki.browse.TsuzukiGlobalSearchTile
import eu.kanade.presentation.tsuzuki.browse.TsuzukiListingSegments
import eu.kanade.presentation.tsuzuki.browse.TsuzukiSourceRow
import eu.kanade.presentation.tsuzuki.details.TsuzukiDetailsDescription
import eu.kanade.presentation.tsuzuki.history.TsuzukiHistoryItem
import eu.kanade.presentation.tsuzuki.home.HomeActions
import eu.kanade.presentation.tsuzuki.home.TsuzukiHomeScreen
import eu.kanade.presentation.tsuzuki.library.TsuzukiShelfPills
import eu.kanade.presentation.tsuzuki.manga.TsuzukiMangaPageIndicator
import eu.kanade.presentation.tsuzuki.manga.TsuzukiMangaReaderTopBar
import eu.kanade.presentation.tsuzuki.more.TsuzukiMoreScreen
import eu.kanade.presentation.tsuzuki.novel.ReaderPageBasics
import eu.kanade.presentation.tsuzuki.novel.ReaderTextMetrics
import eu.kanade.presentation.tsuzuki.novel.TsuzukiPageColourPicker
import eu.kanade.presentation.tsuzuki.reader.NovelChromeAction
import eu.kanade.presentation.tsuzuki.reader.TsuzukiNovelActions
import eu.kanade.presentation.tsuzuki.reader.TsuzukiNovelNavigation
import eu.kanade.presentation.tsuzuki.reader.TsuzukiNovelProgress
import eu.kanade.presentation.tsuzuki.reader.TsuzukiReaderTitle
import eu.kanade.presentation.tsuzuki.settings.TsuzukiSettingsSectionRow
import eu.kanade.presentation.tsuzuki.settings.TsuzukiSettingsTitle
import eu.kanade.presentation.tsuzuki.updates.TsuzukiUpdatesTiles
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.tsuzuki.home.HomeBook
import eu.kanade.tachiyomi.ui.tsuzuki.home.HomeState
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@Preview(name = "EN 100%", group = "360 dp", widthDp = 360, heightDp = 800, locale = "en", fontScale = 1f)
@Preview(name = "EN 130%", group = "360 dp", widthDp = 360, heightDp = 800, locale = "en", fontScale = 1.3f)
@Preview(name = "EN 200%", group = "360 dp", widthDp = 360, heightDp = 800, locale = "en", fontScale = 2f)
@Preview(name = "JA 100%", group = "360 dp", widthDp = 360, heightDp = 800, locale = "ja", fontScale = 1f)
@Preview(name = "JA 130%", group = "360 dp", widthDp = 360, heightDp = 800, locale = "ja", fontScale = 1.3f)
@Preview(name = "JA 200%", group = "360 dp", widthDp = 360, heightDp = 800, locale = "ja", fontScale = 2f)
private annotation class NarrowPreviews

internal class TsuzukiNarrowPreviews : PreviewParameterProvider<AppTheme> {
    override val values = sequenceOf(AppTheme.DOT, AppTheme.COLOUR)
}

@Composable
private fun PreviewPage(theme: AppTheme, content: @Composable () -> Unit) {
    TachiyomiPreviewTheme(appTheme = theme) {
        Column(Modifier.fillMaxSize().tsuzukiBackground(TsuzukiTheme.tokens).verticalScroll(rememberScrollState())) {
            content()
        }
    }
}

@Composable
private fun longTitle(): String = if (LocalConfiguration.current.locales[0].language == "ja") {
    "古い時計塔の秘密を追いかける旅の記録"
} else {
    "The Extraordinary Journey to the Old Clock Tower"
}

@Composable
private fun chapterTitle(): String = if (LocalConfiguration.current.locales[0].language == "ja") {
    "第131話 時計塔の中で交わした約束"
} else {
    "Chapter 131 · A promise made inside the old clock tower"
}

@NarrowPreviews
@Composable
private fun HomePreview(@PreviewParameter(TsuzukiNarrowPreviews::class) theme: AppTheme) {
    val title = longTitle()
    val chapter = chapterTitle()
    TachiyomiPreviewTheme(appTheme = theme) {
        TsuzukiHomeScreen(
            state = HomeState(
                loading = false,
                books = (1L..3L).map { id ->
                    HomeBook(id, id, title, MangaCover(id, 0L, false, null, 0L), chapter, 0.63f, 4, true)
                },
                fromHistory = true,
                libraryCount = 312,
                newChapters = 128,
                todayMinutes = 75,
            ),
            actions = HomeActions({}, {}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }),
            bottomBarOverlay = false,
        )
    }
}

@NarrowPreviews
@Composable
private fun LibraryPreview(@PreviewParameter(TsuzukiNarrowPreviews::class) theme: AppTheme) = PreviewPage(theme) {
    val title = longTitle()
    val names = listOf("", title, "Reference books", "On hold", "Completed", "Favourites", "Long-running serials")
    TsuzukiShelfPills(
        categories = names.mapIndexed { index, name -> Category(index.toLong(), name, index.toLong(), 0L) },
        selected = 6,
        getItemCountForCategory = { if (it.id == 0L) 312 else 12 },
        onClick = {},
    )
    val navigation = listOf(
        stringResource(MR.strings.tsz_home),
        stringResource(MR.strings.label_library),
        stringResource(MR.strings.label_recent_updates),
        stringResource(MR.strings.browse),
        stringResource(MR.strings.label_more),
    )
    TsuzukiNavBar(
        navigation.mapIndexed { index, label ->
            TsuzukiNavItem(
                "$index",
                label,
                label.uppercase(),
                Icons.Outlined.Home,
                index == 1,
            ) {}
        },
    )
}

@NarrowPreviews
@Composable
private fun BrowsePreview(@PreviewParameter(TsuzukiNarrowPreviews::class) theme: AppTheme) = PreviewPage(theme) {
    TsuzukiPillRow(
        listOf(stringResource(TDMR.strings.label_novels), stringResource(TDMR.strings.label_manga)),
        selected = 0,
        onClick = {},
    )
    TsuzukiGlobalSearchTile(12, {}, Modifier.padding(horizontal = 12.dp))
    TsuzukiListingSegments(true, popular = true, latest = false, onPopular = {}, onLatest = {})
    TsuzukiSourceRow(
        source = Source(1, "ja", longTitle(), supportsLatest = true, isStub = false, isNovelSource = true),
        showLanguage = true,
        onOpen = {},
        onOptions = {},
        onPreset = {},
        onLatest = {},
        pin = { IconButton(onClick = {}) { Icon(Icons.Outlined.PushPin, null) } },
        sourceIcon = { Icon(Icons.Outlined.Book, null) },
    )
}

@NarrowPreviews
@Composable
private fun DetailsPreview(@PreviewParameter(TsuzukiNarrowPreviews::class) theme: AppTheme) = PreviewPage(theme) {
    val title = longTitle()
    MangaActionRow(
        favorite = true,
        trackingCount = 0,
        nextUpdate = Clock.System.now() + 2.days,
        isUserIntervalMode = false,
        onAddToLibraryClicked = {},
        onWebViewClicked = {},
        onWebViewLongClicked = null,
        onSearchChaptersClicked = null,
        onTrackingClicked = {},
        onEditIntervalClicked = {},
        onEditCategory = null,
    )
    TsuzukiDetailsDescription(
        defaultExpanded = false,
        hasSummary = true,
        tags = listOf(title, "Action", "Adventure", "Fantasy", "Historical mystery", "Drama", "Comedy"),
        onTagSearch = {},
        onCopyTag = {},
        summary = { Text(title) },
    )
    ChapterHeader(enabled = true, chapterCount = 1871, missingChapterCount = 0, onClick = {})
}

@NarrowPreviews
@Composable
private fun MorePreview(@PreviewParameter(TsuzukiNarrowPreviews::class) theme: AppTheme) {
    TachiyomiPreviewTheme(appTheme = theme) {
        TsuzukiMoreScreen(
            downloadQueueStateProvider = { DownloadQueueState.Stopped },
            downloadedOnly = false,
            onDownloadedOnlyChange = {},
            incognitoMode = true,
            onIncognitoModeChange = {},
            onClickHistory = {},
            onClickDownloadQueue = {},
            onClickCategories = {},
            onClickStats = {},
            onClickDataAndStorage = {},
            onClickSettings = {},
            onClickAbout = {},
            onClickHelp = {},
        )
    }
}

@NarrowPreviews
@Composable
private fun NovelReaderPreview(@PreviewParameter(TsuzukiNarrowPreviews::class) theme: AppTheme) = PreviewPage(theme) {
    TsuzukiReaderTitle(longTitle(), chapterTitle(), novel = true)
    val interactions = remember { MutableInteractionSource() }
    TsuzukiNovelNavigation(true, {}, true, {}) {
        TsuzukiNovelProgress(19, "3 / 16", {}, interactions)
    }
    TsuzukiNovelActions(
        listOf(
            NovelChromeAction(
                Icons.Outlined.VerticalAlignTop,
                stringResource(TDMR.strings.action_scroll_to_top),
                {},
                shortLabel = stringResource(MR.strings.tsz_q8_reader_top),
            ),
            NovelChromeAction(Icons.Outlined.Translate, stringResource(TDMR.strings.action_translate), {}),
            NovelChromeAction(Icons.Outlined.VolumeUp, stringResource(MR.strings.tsz_reader_listen), {}),
            NovelChromeAction(Icons.Outlined.Settings, stringResource(MR.strings.action_settings), {}),
            NovelChromeAction(Icons.Outlined.Menu, stringResource(MR.strings.chapters), {}),
        ),
    )
}

@NarrowPreviews
@Composable
private fun MangaReaderPreview(@PreviewParameter(TsuzukiNarrowPreviews::class) theme: AppTheme) = PreviewPage(theme) {
    TsuzukiMangaReaderTopBar(longTitle(), chapterTitle(), null, {}, Modifier) {
        IconButton(onClick = {}) { Icon(Icons.Outlined.Settings, stringResource(MR.strings.action_settings)) }
    }
    TsuzukiMangaPageIndicator(12, 39, Modifier)
}

@NarrowPreviews
@Composable
private fun ReaderSettingsPreview(
    @PreviewParameter(TsuzukiNarrowPreviews::class) theme: AppTheme,
) = PreviewPage(theme) {
    val preferences = remember { ReaderPreferences(InMemoryPreferenceStore()) }
    TsuzukiPageColourPicker(preferences, columns = 6, includeCustom = false)
    ReaderTextMetrics(preferences, quick = true)
    ReaderPageBasics(preferences)
}

@NarrowPreviews
@Composable
private fun SettingsPreview(@PreviewParameter(TsuzukiNarrowPreviews::class) theme: AppTheme) = PreviewPage(theme) {
    TsuzukiSettingsTitle(stringResource(MR.strings.label_settings), onBack = {})
    TsuzukiSettingsSectionRow(
        title = stringResource(MR.strings.label_data_storage),
        subtitle = longTitle(),
        icon = Icons.Outlined.Download,
        twoPane = false,
        selected = false,
        onClick = {},
    )
}

@NarrowPreviews
@Composable
private fun UpdatesPreview(@PreviewParameter(TsuzukiNarrowPreviews::class) theme: AppTheme) = PreviewPage(theme) {
    TsuzukiUpdatesTiles(newCount = 1871, toSave = 925, onSaveAll = {})
}

@NarrowPreviews
@Composable
private fun HistoryPreview(@PreviewParameter(TsuzukiNarrowPreviews::class) theme: AppTheme) = PreviewPage(theme) {
    TsuzukiHistoryItem(
        history = HistoryWithRelations(
            id = 1L,
            chapterId = 1L,
            mangaId = 1L,
            title = longTitle(),
            chapterNumber = 131.0,
            readAt = Date(0L),
            readDuration = 60000,
            coverData = MangaCover(1L, 0L, true, null, 0L),
            chapterRead = false,
            lastPageRead = 47L,
            isNovel = true,
        ),
        onClickCover = {},
        onClickResume = {},
        onClickDelete = {},
        onClickFavorite = {},
        timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT),
    )
}
