package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiAdaptiveSheet
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.source.model.FilterList
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TsuzukiCatalogueFilters(
    onDismissRequest: () -> Unit,
    filters: FilterList,
    onReset: () -> Unit,
    onFilter: () -> Unit,
    onUpdate: (FilterList) -> Unit,
    onOpenPresets: () -> Unit,
    onSavePreset: (String, Boolean) -> Unit,
    savePresetDialog: @Composable (onDismiss: () -> Unit, onSave: (String, Boolean) -> Unit) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    // Source filters are mutable models. Recompose rows even when the filter list retains its identity.
    var revision by remember { mutableIntStateOf(0) }
    var showSaveDialog by remember { mutableStateOf(false) }
    val update = {
        revision++
        onUpdate(filters)
    }

    TsuzukiAdaptiveSheet(onDismissRequest) { close ->
        Column(
            Modifier.fillMaxWidth().heightIn(max = 700.dp).padding(top = 10.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp)
                    .background(tokens.tileBorder ?: tokens.track, CircleShape),
            )
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    tsuzukiCase(stringResource(MR.strings.tsz_catalogue_filters)).orEmpty(),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineLarge,
                )
                IconButton(onClick = { showSaveDialog = true }) {
                    Icon(Icons.Filled.Save, stringResource(MR.strings.action_save), tint = tokens.accent)
                }
                IconButton(onClick = onOpenPresets) {
                    Icon(
                        Icons.Outlined.BookmarkBorder,
                        stringResource(MR.strings.tsz_catalogue_filter_presets),
                        tint = tokens.accent,
                    )
                }
                TextButton(onClick = onReset) {
                    Text(
                        tsuzukiCase(stringResource(MR.strings.action_reset)).orEmpty(),
                        color = tokens.accent,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            LazyColumn(
                Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filters) { filter -> TsuzukiCatalogueFilterItem(filter, revision, update) }
            }
            Button(
                onClick = {
                    close {
                        onFilter()
                        onDismissRequest()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = tokens.accent, contentColor = tokens.onAccent),
            ) {
                Text(
                    tsuzukiCase(stringResource(MR.strings.action_filter)).orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.onAccent,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (showSaveDialog) {
        savePresetDialog({ showSaveDialog = false }) { name, setAsDefault ->
            onSavePreset(name, setAsDefault)
            showSaveDialog = false
        }
    }
}
