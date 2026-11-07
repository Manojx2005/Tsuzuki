package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Browse → Migrate in the Tsuzuki themes: the same grouped source rows as Sources, with each source's library count
 * where Sources shows its pin, and the sort controls beside the section label. Behaviour matches the stock list
 * (tap opens the source's books to migrate, long press copies the source id).
 */
@Composable
internal fun TsuzukiMigrateSourceList(
    list: List<Pair<Source, Long>>,
    contentPadding: PaddingValues,
    onClickItem: (Source) -> Unit,
    onLongClickItem: (Source) -> Unit,
    sortingMode: SetMigrateSorting.Mode,
    onToggleSortingMode: () -> Unit,
    sortingDirection: SetMigrateSorting.Direction,
    onToggleSortingDirection: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    LazyColumn(contentPadding = contentPadding) {
        item(key = "migrate-header") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                TsuzukiSectionLabel(
                    text = stringResource(MR.strings.migration_selection_prompt),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onToggleSortingMode) {
                    when (sortingMode) {
                        SetMigrateSorting.Mode.ALPHABETICAL -> Icon(
                            Icons.Outlined.SortByAlpha,
                            contentDescription = stringResource(MR.strings.action_sort_alpha),
                            tint = tokens.dotOn,
                        )
                        SetMigrateSorting.Mode.TOTAL -> Icon(
                            Icons.Outlined.Numbers,
                            contentDescription = stringResource(MR.strings.action_sort_count),
                            tint = tokens.dotOn,
                        )
                    }
                }
                IconButton(onClick = onToggleSortingDirection) {
                    when (sortingDirection) {
                        SetMigrateSorting.Direction.ASCENDING -> Icon(
                            Icons.Outlined.ArrowUpward,
                            contentDescription = stringResource(MR.strings.action_asc),
                            tint = tokens.dotOn,
                        )
                        SetMigrateSorting.Direction.DESCENDING -> Icon(
                            Icons.Outlined.ArrowDownward,
                            contentDescription = stringResource(MR.strings.action_desc),
                            tint = tokens.dotOn,
                        )
                    }
                }
            }
        }
        itemsIndexed(items = list, key = { _, (source, _) -> "migrate-${source.id}" }) { index, (source, count) ->
            TsuzukiSourceRow(
                source = source,
                showLanguage = source.lang != "",
                onOpen = { onClickItem(source) },
                onOptions = { onLongClickItem(source) },
                onPreset = null,
                onLatest = null,
                pin = {
                    Text(
                        text = count.toString(),
                        color = tokens.accent,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                },
                modifier = Modifier
                    .animateItem()
                    .then(tsuzukiGroupedTile(first = index == 0, last = index == list.lastIndex)),
            )
        }
    }
}
