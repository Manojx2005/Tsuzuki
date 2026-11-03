package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import tachiyomi.domain.category.model.Category
import tachiyomi.presentation.core.components.material.TabText

@Composable
internal fun LibraryTabs(
    categories: List<Category>,
    pagerState: PagerState,
    getItemCountForCategory: (Category) -> Int?,
    onTabItemClick: (Int) -> Unit,
) {
    val currentPageIndex = pagerState.currentPage.coerceAtMost(categories.lastIndex)
    if (TsuzukiTheme.tokens.style != TsuzukiStyle.NONE) {
        TsuzukiCategoryPills(categories, currentPageIndex, getItemCountForCategory, onTabItemClick)
        return
    }
    PrimaryScrollableTabRow(
        selectedTabIndex = currentPageIndex,
        edgePadding = 0.dp,
    ) {
        categories.forEachIndexed { index, category ->
            Tab(
                selected = currentPageIndex == index,
                onClick = { onTabItemClick(index) },
                text = {
                    TabText(
                        text = category.visualName,
                        badgeCount = getItemCountForCategory(category),
                    )
                },
                unselectedContentColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Board "LibraryList": categories as pills; the current one takes the accent. */
@Composable
private fun TsuzukiCategoryPills(
    categories: List<Category>,
    selected: Int,
    getItemCountForCategory: (Category) -> Int?,
    onClick: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    // Keep the selected pill in view when the pager is swiped.
    LaunchedEffect(selected) {
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.none { it.index == selected && it.offset >= 0 }) listState.animateScrollToItem(selected)
    }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(categories, key = { _, c -> c.id }) { index, category ->
            TsuzukiPill(
                text = category.visualName,
                selected = index == selected,
                onClick = { onClick(index) },
                count = getItemCountForCategory(category),
            )
        }
    }
}
