package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DisabledByDefault
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSegments
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.source.model.Filter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Source filters change in place. The revision invalidates the row and any nested rows after an edit. */
@Composable
internal fun TsuzukiCatalogueFilterItem(filter: Filter<*>, revision: Int, onUpdate: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    when (filter) {
        is Filter.Header -> CatalogueFilterLabel(filter.name)
        is Filter.Separator -> HorizontalDivider(color = tokens.track)
        is Filter.CheckBox -> FilterToggleTile(
            name = filter.name,
            state = ToggleableState(filter.state),
        ) {
            filter.state = !filter.state
            onUpdate()
        }
        is Filter.TriState -> {
            val state = when (filter.state) {
                Filter.TriState.STATE_INCLUDE -> ToggleableState.On
                Filter.TriState.STATE_EXCLUDE -> ToggleableState.Indeterminate
                else -> ToggleableState.Off
            }
            val description = when (state) {
                ToggleableState.On -> stringResource(MR.strings.include, filter.name)
                ToggleableState.Indeterminate -> stringResource(MR.strings.exclude, filter.name)
                ToggleableState.Off -> null
            }
            FilterToggleTile(filter.name, state, description) {
                filter.state = nextCatalogueTriState(filter.state)
                onUpdate()
            }
        }
        is Filter.Text -> TsuzukiTile(modifier = Modifier.fillMaxWidth(), padding = 10.dp) {
            OutlinedTextField(
                value = filter.state,
                onValueChange = {
                    filter.state = it
                    onUpdate()
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                label = { Text(tsuzukiCase(filter.name).orEmpty()) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = tokens.dotOn,
                    unfocusedTextColor = tokens.dotOn,
                    focusedBorderColor = tokens.accent,
                    unfocusedBorderColor = tokens.track,
                    focusedLabelColor = tokens.accent,
                    unfocusedLabelColor = tokens.dotOn,
                    cursorColor = tokens.accent,
                ),
            )
        }
        is Filter.Select<*> -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CatalogueFilterLabel(filter.name)
            val labels = filter.values.map { it.toString() }
            val onSelect: (Int) -> Unit = {
                filter.state = it
                onUpdate()
            }
            when {
                labels.size in 1..3 && filter.state in labels.indices -> {
                    TsuzukiSegments(labels, filter.state, onSelect, Modifier.fillMaxWidth())
                }
                labels.isNotEmpty() -> TsuzukiPillRow(labels, filter.state.coerceIn(labels.indices), onSelect)
            }
        }
        is Filter.Sort -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CatalogueFilterLabel(filter.name)
            TsuzukiTile(modifier = Modifier.fillMaxWidth(), padding = 4.dp) {
                filter.values.forEachIndexed { index, label ->
                    val sort = filter.state?.takeIf { it.index == index }
                    val arrow = sort?.let { if (it.ascending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward }
                    CatalogueFilterRow(
                        label = label,
                        icon = arrow,
                        active = sort != null,
                        modifier = Modifier.semantics { selected = sort != null },
                        role = Role.RadioButton,
                    ) {
                        filter.state = nextCatalogueSort(filter.state, index)
                        onUpdate()
                    }
                }
            }
        }
        is Filter.Group<*> -> {
            var expanded by remember { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TsuzukiTile(modifier = Modifier.fillMaxWidth(), padding = 4.dp) {
                    val action = stringResource(if (expanded) MR.strings.action_collapse else MR.strings.action_expand)
                    CatalogueFilterRow(
                        label = filter.name,
                        icon = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        active = expanded,
                        role = Role.Button,
                        onClickLabel = action,
                    ) { expanded = !expanded }
                }
                if (expanded) {
                    filter.state.filterIsInstance<Filter<*>>().forEach { child ->
                        TsuzukiCatalogueFilterItem(child, revision, onUpdate)
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogueFilterLabel(name: String) {
    TsuzukiSectionLabel(name, contentPadding = PaddingValues(vertical = 2.dp))
}

@Composable
private fun FilterToggleTile(name: String, state: ToggleableState, description: String? = null, onClick: () -> Unit) {
    TsuzukiTile(modifier = Modifier.fillMaxWidth(), padding = 4.dp) {
        CatalogueFilterRow(
            label = name,
            icon = when (state) {
                ToggleableState.Off -> Icons.Rounded.CheckBoxOutlineBlank
                ToggleableState.On -> Icons.Rounded.CheckBox
                ToggleableState.Indeterminate -> Icons.Rounded.DisabledByDefault
            },
            active = state != ToggleableState.Off,
            modifier = Modifier.semantics {
                toggleableState = state
                if (description != null) stateDescription = description
            },
            role = Role.Checkbox,
            onClick = onClick,
        )
    }
}

@Composable
private fun CatalogueFilterRow(
    label: String,
    icon: ImageVector?,
    active: Boolean,
    role: Role,
    modifier: Modifier = Modifier,
    onClickLabel: String? = null,
    onClick: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val ink = if (active) tokens.accent else tokens.dotOn
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)
            .tsuzukiClickable(role = role, onClickLabel = onClickLabel, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = tsuzukiCase(label).orEmpty(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = ink,
        )
        if (icon != null) Icon(icon, contentDescription = null, tint = ink)
    }
}

internal fun nextCatalogueTriState(state: Int): Int = when (state) {
    Filter.TriState.STATE_INCLUDE -> Filter.TriState.STATE_EXCLUDE
    Filter.TriState.STATE_EXCLUDE -> Filter.TriState.STATE_IGNORE
    else -> Filter.TriState.STATE_INCLUDE
}

internal fun nextCatalogueSort(selection: Filter.Sort.Selection?, index: Int): Filter.Sort.Selection =
    Filter.Sort.Selection(
        index = index,
        ascending = if (selection?.index == index) !selection.ascending else selection?.ascending ?: true,
    )
