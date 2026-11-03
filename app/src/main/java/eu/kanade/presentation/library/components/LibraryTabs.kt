package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.category.visualName
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
    val t = TsuzukiTheme.tokens
    val listState = rememberLazyListState()
    // Keep the selected pill in view when the pager is swiped.
    LaunchedEffect(selected) {
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.none { it.index == selected && it.offset >= 0 }) listState.animateScrollToItem(selected)
    }
    val shape = RoundedCornerShape(19.dp)
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(categories, key = { _, c -> c.id }) { index, category ->
            val on = index == selected
            val ink = if (on) t.onAccent else MaterialTheme.colorScheme.onSurface
            val name = category.visualName
            Row(
                Modifier
                    .height(38.dp)
                    .clip(shape)
                    .then(
                        if (on) {
                            Modifier.background(t.accent)
                        } else {
                            Modifier
                                .background(t.tile)
                                .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
                        },
                    )
                    .clickable(role = Role.Tab) { onClick(index) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = if (t.isDot) name.uppercase() else name,
                    color = ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
                getItemCountForCategory(category)?.let {
                    Text(
                        text = "$it",
                        color = ink.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}
