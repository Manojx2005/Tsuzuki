package eu.kanade.tachiyomi.tsuzuki.anime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun AnimeFilterDialog(
    filters: AnimeFilterList,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tsuzukiMenuLabel(stringResource(MR.strings.action_filter))) },
        text = {
            if (filters.isEmpty()) {
                Text(tsuzukiMenuLabel(stringResource(MR.strings.no_results_found)))
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filters.list) { filter -> AnimeFilterField(filter) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onApply) { Text(tsuzukiMenuLabel(stringResource(MR.strings.action_apply))) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) { Text(tsuzukiMenuLabel(stringResource(MR.strings.action_reset))) }
                TextButton(onClick = onDismiss) { Text(tsuzukiMenuLabel(stringResource(MR.strings.action_cancel))) }
            }
        },
    )
}

@Composable
private fun AnimeFilterField(filter: AnimeFilter<*>) {
    when (filter) {
        is AnimeFilter.Header -> Text(tsuzukiMenuLabel(filter.name), style = MaterialTheme.typography.titleSmall)
        is AnimeFilter.Separator -> HorizontalDivider()
        is AnimeFilter.Group<*> -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(tsuzukiMenuLabel(filter.name), style = MaterialTheme.typography.titleSmall)
            filter.state.filterIsInstance<AnimeFilter<*>>().forEach { AnimeFilterField(it) }
        }
        is AnimeFilter.Text -> {
            var value by remember(filter) { mutableStateOf(filter.state) }
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                    filter.state = it
                },
                label = { Text(tsuzukiMenuLabel(filter.name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        is AnimeFilter.CheckBox -> {
            var checked by remember(filter) { mutableStateOf(filter.state) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = {
                    checked = it
                    filter.state = it
                })
                Text(tsuzukiMenuLabel(filter.name))
            }
        }
        is AnimeFilter.TriState -> {
            var value by remember(filter) { mutableStateOf(filter.state) }
            val state = when (value) {
                AnimeFilter.TriState.STATE_INCLUDE -> ToggleableState.On
                AnimeFilter.TriState.STATE_EXCLUDE -> ToggleableState.Off
                else -> ToggleableState.Indeterminate
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TriStateCheckbox(
                    state = state,
                    onClick = {
                        value = (value + 1) % 3
                        filter.state = value
                    },
                )
                Text(tsuzukiMenuLabel(filter.name))
            }
        }
        is AnimeFilter.Select<*> -> {
            var selected by remember(filter) { mutableStateOf(filter.state) }
            AnimeFilterChoice(filter.name, filter.values.map { it.toString() }, selected) {
                selected = it
                filter.state = it
            }
        }
        is AnimeFilter.Sort -> {
            var selected by remember(filter) { mutableStateOf(filter.state) }
            AnimeFilterChoice(filter.name, filter.values.toList(), selected?.index) {
                val next = AnimeFilter.Sort.Selection(it, selected?.ascending ?: true)
                selected = next
                filter.state = next
            }
            selected?.let { selection ->
                TextButton(
                    onClick = {
                        val next = selection.copy(ascending = !selection.ascending)
                        selected = next
                        filter.state = next
                    },
                ) {
                    val order = if (selection.ascending) MR.strings.action_asc else MR.strings.action_desc
                    Text(tsuzukiMenuLabel(stringResource(order)))
                }
            }
        }
    }
}

@Composable
private fun AnimeFilterChoice(name: String, options: List<String>, selected: Int?, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, enabled = options.isNotEmpty()) {
            val selection = selected?.let(options::getOrNull)
            Text(tsuzukiMenuLabel(listOfNotNull(name, selection).joinToString(" · ")))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(tsuzukiMenuLabel(option)) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Copies the values of [previous] onto [draft], the fresh filters of a source. Drafts come from the source and are
 * independent of the filters an active request is using.
 *
 * AY2-F: [previous] may come from an older instance of the source, loaded by another class loader after its
 * settings changed, so filters are matched by class name and title, never by class identity, and only values of
 * the shared source-api types are copied. Select and sort choices follow their displayed label across
 * reorderings. A missing or ambiguous choice keeps the source's default.
 */
internal fun restoreAnimeFilterStates(previous: List<AnimeFilter<*>>, draft: List<AnimeFilter<*>>) {
    fun identity(filter: AnimeFilter<*>) = filter.javaClass.name to filter.name
    val oldByIdentity = previous.groupBy(::identity)
    val newByIdentity = draft.groupBy(::identity)
    newByIdentity.forEach { (key, matches) ->
        val new = matches.singleOrNull() ?: return@forEach
        val old = oldByIdentity[key]?.singleOrNull() ?: return@forEach
        when {
            old is AnimeFilter.Group<*> && new is AnimeFilter.Group<*> -> restoreAnimeFilterStates(
                old.state.filterIsInstance<AnimeFilter<*>>(),
                new.state.filterIsInstance<AnimeFilter<*>>(),
            )
            old is AnimeFilter.Select<*> && new is AnimeFilter.Select<*> ->
                matchingChoice(old.values, old.state, new.values)?.let { new.state = it }
            old is AnimeFilter.Sort && new is AnimeFilter.Sort -> {
                val selection = old.state
                if (selection == null) {
                    new.state = null
                } else {
                    matchingChoice(old.values, selection.index, new.values)?.let {
                        new.state = selection.copy(index = it)
                    }
                }
            }
            old is AnimeFilter.Text && new is AnimeFilter.Text -> new.state = old.state
            old is AnimeFilter.CheckBox && new is AnimeFilter.CheckBox -> new.state = old.state
            old is AnimeFilter.TriState && new is AnimeFilter.TriState -> new.state = old.state
        }
    }
}

private fun matchingChoice(old: Array<out Any?>, index: Int, new: Array<out Any?>): Int? {
    if (index !in old.indices) return null
    val label = old[index].toString()
    if (old.count { it.toString() == label } != 1) return null
    return new.indices.singleOrNull { new[it].toString() == label }
}

/**
 * The filters to use with [freshFilters] (the new source instance's list) after the source was reloaded (AY2-F):
 * no filters stay no filters, otherwise the new instance's own filter objects with the compatible values of
 * [applied]. Extension-owned filter objects from the old instance are never handed to the new one.
 */
internal fun carryAnimeFilters(applied: AnimeFilterList, freshFilters: () -> AnimeFilterList): AnimeFilterList {
    if (applied.isEmpty()) return applied
    return freshFilters().also { restoreAnimeFilterStates(applied, it) }
}
