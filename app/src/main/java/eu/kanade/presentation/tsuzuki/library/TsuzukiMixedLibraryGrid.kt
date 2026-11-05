package eu.kanade.presentation.tsuzuki.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.library.components.DownloadsBadge
import eu.kanade.presentation.library.components.LanguageBadge
import eu.kanade.presentation.library.components.UnreadBadge
import eu.kanade.presentation.library.components.globalSearchItem
import eu.kanade.presentation.library.components.loadMoreSentinel
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiSharedCover
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.components.FastScrollLazyVerticalGrid
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import tachiyomi.domain.manga.model.MangaCover as MangaCoverModel

/** Board LibraryMixed, with the stock click, range-selection, resume and paging callbacks. */
@Composable
fun TsuzukiMixedLibraryGrid(
    items: List<LibraryItem>,
    columns: Int,
    showTitle: Boolean,
    contentPadding: PaddingValues,
    selection: Set<Long>,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
    onContinueReading: ((LibraryManga) -> Unit)?,
    searchQuery: String?,
    onGlobalSearch: () -> Unit,
    onLoadMore: (() -> Unit)?,
    loadMoreKey: Long,
    titleMaxLines: Int,
) {
    FastScrollLazyVerticalGrid(
        columns = if (columns == 0) GridCells.Adaptive(100.dp) else GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding + PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        globalSearchItem(searchQuery, onGlobalSearch)
        items(items = items, contentType = { "tsuzuki_mixed_book" }) { item ->
            val book = item.libraryManga
            val manga = book.manga
            val tokens = TsuzukiTheme.tokens
            val shape = RoundedCornerShape(14.dp)
            Column(
                modifier = Modifier
                    .clip(shape)
                    .then(
                        if (manga.id in
                            selection
                        ) {
                            Modifier.background(tokens.accent.copy(alpha = 0.25f))
                        } else {
                            Modifier
                        },
                    )
                    .tsuzukiClickable(
                        onClick = { onClick(book) },
                        onLongClick = { onLongClick(book) },
                    ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(160.dp)) {
                    MangaCover.Book(
                        data = MangaCoverModel(
                            mangaId = manga.id,
                            sourceId = manga.source,
                            isMangaFavorite = manga.favorite,
                            url = manga.thumbnailUrl,
                            lastModified = manga.coverLastModified,
                            title = manga.title,
                        ),
                        modifier = Modifier.fillMaxSize().tsuzukiSharedCover(manga.id).alpha(
                            if (manga.id in
                                selection
                            ) {
                                0.76f
                            } else {
                                1f
                            },
                        ),
                        shape = shape,
                    )
                    Text(
                        text = if (manga.isNovel) "LN" else stringResource(TDMR.strings.label_manga).uppercase(),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (manga.isNovel) tokens.accent else tokens.background.copy(alpha = 0.9f))
                            .then(
                                if (manga.isNovel) {
                                    Modifier
                                } else {
                                    Modifier.border(
                                        1.dp,
                                        tokens.dotOn.copy(alpha = 0.4f),
                                        RoundedCornerShape(5.dp),
                                    )
                                },
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        color = if (manga.isNovel) tokens.onAccent else tokens.dotOn,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                    Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                        UnreadBadge(item.badges.unreadCount)
                    }
                    Row(
                        modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        DownloadsBadge(item.badges.downloadCount)
                        LanguageBadge(item.badges.isLocal, item.badges.sourceLanguage)
                    }
                    if (onContinueReading != null && item.unreadCount > 0) {
                        FilledIconButton(
                            onClick = { onContinueReading(book) },
                            modifier = Modifier.align(Alignment.BottomEnd),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = tokens.accent,
                                contentColor = tokens.onAccent,
                            ),
                        ) {
                            Icon(Icons.Filled.PlayArrow, stringResource(MR.strings.action_resume))
                        }
                    }
                }
                if (book.totalChapters > 0) {
                    TsuzukiProgressBar(
                        progress = book.readCount.toFloat() / book.totalChapters,
                        thickness = 4.dp,
                        dots = 12,
                    )
                }
                if (showTitle) {
                    Text(
                        text = if (tokens.isDot) manga.title.uppercase() else manga.title,
                        modifier = Modifier.padding(horizontal = 2.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 12.sp,
                        maxLines = titleMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        loadMoreSentinel(loadMoreKey, onLoadMore)
    }
}
