package eu.kanade.presentation.more.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.widget.PreferenceGroupHeader
import eu.kanade.presentation.more.settings.widget.highlightBackground
import eu.kanade.presentation.tsuzuki.settings.advancedPreferenceToggle
import eu.kanade.presentation.tsuzuki.settings.tsuzukiAdvancedPreferences
import kotlinx.coroutines.delay
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import kotlin.time.Duration.Companion.seconds

/**
 * Preference Screen composable which contains a list of [Preference] items
 * @param items [Preference] items which should be displayed on the preference screen. An item can be a single [PreferenceItem] or a group ([Preference.PreferenceGroup])
 * @param modifier [Modifier] to be applied to the preferenceScreen layout
 */
@Composable
fun PreferenceScreen(
    items: List<Preference>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    settingsTitle: StringResource? = null,
) {
    val state = rememberLazyListState()
    val highlightKey = remember { SearchableSettings.highlightKey }
    val groups = tsuzukiAdvancedPreferences(items, settingsTitle)
    var showAdvanced by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val expanded = showAdvanced ?: groups.containsHighlight(highlightKey)
    val visibleItems = if (groups.advanced.isEmpty()) {
        items
    } else {
        groups.primary + advancedPreferenceToggle(expanded) { showAdvanced = !expanded } +
            if (expanded) groups.advanced else emptyList()
    }
    if (highlightKey != null) {
        LaunchedEffect(Unit) {
            val i = visibleItems.findHighlightedIndex(highlightKey)
            if (i >= 0) {
                delay(0.5.seconds)
                state.animateScrollToItem(i)
            }
            SearchableSettings.highlightKey = null
        }
    }

    // Tsuzuki themes: each preference group is one tile under a small label (board "Settings").
    val tsuzuki = eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme.tokens.style !=
        eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle.NONE
    ScrollbarLazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
    ) {
        visibleItems.fastForEachIndexed { i, preference ->
            when (preference) {
                // Create Preference Group
                is Preference.PreferenceGroup -> {
                    if (!preference.enabled) return@fastForEachIndexed

                    item {
                        Column(
                            Modifier.fillMaxWidth().highlightBackground(preference.title == highlightKey),
                        ) {
                            if (tsuzuki) {
                                eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel(text = preference.title)
                            } else {
                                PreferenceGroupHeader(title = preference.title)
                            }
                        }
                    }
                    val groupItems = preference.preferenceItems
                    itemsIndexed(groupItems) { index, item ->
                        if (tsuzuki) {
                            androidx.compose.foundation.layout.Box(
                                eu.kanade.presentation.tsuzuki.browse.tsuzukiGroupedTile(
                                    first = index == 0,
                                    last = index == groupItems.lastIndex,
                                ),
                            ) {
                                PreferenceItem(item = item, highlightKey = highlightKey)
                            }
                        } else {
                            PreferenceItem(
                                item = item,
                                highlightKey = highlightKey,
                            )
                        }
                    }
                    item {
                        if (i < visibleItems.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // Create Preference Item
                is Preference.PreferenceItem<*, *> -> item {
                    PreferenceItem(
                        item = preference,
                        highlightKey = highlightKey,
                    )
                }
            }
        }
    }
}

private fun List<Preference>.findHighlightedIndex(highlightKey: String): Int {
    return flatMap {
        if (it is Preference.PreferenceGroup) {
            if (!it.enabled) return@flatMap emptyList()
            buildList<String?> {
                add(it.title) // Header
                addAll(it.preferenceItems.map { groupItem -> groupItem.title })
                add(null) // Spacer
            }
        } else {
            listOf(it.title)
        }
    }.indexOfFirst { it == highlightKey }
}
