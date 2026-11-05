package eu.kanade.presentation.reader.appbars

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.tsuzuki.manga.TsuzukiMangaReaderTopBar
import eu.kanade.presentation.tsuzuki.reader.TsuzukiReaderTitle
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReaderTopBar(
    mangaTitle: String?,
    chapterTitle: String?,
    navigateUp: () -> Unit,
    bookmarked: Boolean,
    onToggleBookmarked: () -> Unit,
    onOpenInWebView: (() -> Unit)?,
    onOpenInBrowser: (() -> Unit)?,
    onShare: (() -> Unit)?,
    modifier: Modifier = Modifier,
    readingMode: ReadingMode? = null,
) {
    val actions: @Composable RowScope.() -> Unit = {
        AppBarActions(
            actions = buildList {
                add(
                    AppBar.Action(
                        title = stringResource(
                            if (bookmarked) {
                                MR.strings.action_remove_bookmark
                            } else {
                                MR.strings.action_bookmark
                            },
                        ),
                        icon = if (bookmarked) {
                            Icons.Outlined.Bookmark
                        } else {
                            Icons.Outlined.BookmarkBorder
                        },
                        onClick = onToggleBookmarked,
                    ),
                )
                onOpenInWebView?.let {
                    add(
                        AppBar.OverflowAction(
                            title = stringResource(MR.strings.action_open_in_web_view),
                            onClick = it,
                            icon = eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuIcons.webView,
                        ),
                    )
                }
                onOpenInBrowser?.let {
                    add(
                        AppBar.OverflowAction(
                            title = stringResource(MR.strings.action_open_in_browser),
                            onClick = it,
                            icon = eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuIcons.browser,
                        ),
                    )
                }
                onShare?.let {
                    add(
                        AppBar.OverflowAction(
                            title = stringResource(MR.strings.action_share),
                            onClick = it,
                            icon = eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuIcons.share,
                        ),
                    )
                }
            },
        )
    }
    if (TsuzukiTheme.tokens.style != TsuzukiStyle.NONE) {
        TsuzukiMangaReaderTopBar(
            mangaTitle = mangaTitle,
            chapterTitle = chapterTitle,
            readingMode = readingMode,
            navigateUp = navigateUp,
            modifier = modifier,
            actions = actions,
        )
        return
    }
    AppBar(
        modifier = modifier,
        backgroundColor = Color.Transparent,
        titleContent = { TsuzukiReaderTitle(title = mangaTitle, subtitle = chapterTitle) },
        navigateUp = navigateUp,
        actions = actions,
    )
}
