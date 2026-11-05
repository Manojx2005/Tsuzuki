package eu.kanade.presentation.tsuzuki.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import eu.kanade.core.preference.asToggleableState
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.theme.tsuzuki.LocalTsuzukiTokens
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.presentation.tsuzuki.settings.tsuzukiDialogColor
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TsuzukiChangeCategoryDialog(
    initialSelection: List<CheckboxState<Category>>,
    onDismissRequest: () -> Unit,
    onEditCategories: () -> Unit,
    onConfirm: (List<Long>, List<Long>) -> Unit,
) {
    var selection by remember { mutableStateOf(initialSelection) }
    val tokens = TsuzukiTheme.tokens

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = tsuzukiDialogColor() ?: tokens.background,
            border = BorderStroke(1.dp, tokens.tileBorder ?: tokens.track),
        ) {
            Column(
                Modifier.padding(start = 18.dp, top = 22.dp, end = 18.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = tsuzukiCase(stringResource(MR.strings.action_move_category)).orEmpty(),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge,
                    color = tokens.dotOn,
                )
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (selection.isEmpty()) {
                        Text(
                            text = tsuzukiCase(stringResource(MR.strings.information_empty_category_dialog)).orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.dotOn,
                        )
                    } else {
                        CompositionLocalProvider(LocalTsuzukiTokens provides tokens.copy(tileRadius = 24.dp)) {
                            TsuzukiTile(Modifier.fillMaxWidth(), padding = 4.dp) {
                                selection.forEachIndexed { index, checkbox ->
                                    CategoryPickerRow(checkbox) {
                                        selection = selection.toMutableList().apply { this[index] = this[index].next() }
                                    }
                                }
                            }
                        }
                    }
                    CompositionLocalProvider(LocalTsuzukiTokens provides tokens.copy(tileRadius = 16.dp)) {
                        TsuzukiTile(
                            Modifier.fillMaxWidth().tsuzukiClickable(role = Role.Button) {
                                onDismissRequest()
                                onEditCategories()
                            },
                            padding = 0.dp,
                        ) {
                            Row(
                                Modifier.fillMaxWidth().heightIn(
                                    min = 50.dp,
                                ).padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, tint = tokens.accent)
                                Text(
                                    text = tsuzukiCase(stringResource(MR.strings.action_edit_categories)).orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.dotOn,
                                )
                            }
                        }
                    }
                }
                if (selection.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onDismissRequest,
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                            shape = CircleShape,
                            border = tokens.tileBorder?.let { BorderStroke(1.dp, it) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tokens.tile,
                                contentColor = tokens.dotOn,
                            ),
                        ) {
                            Text(
                                text = tsuzukiCase(stringResource(MR.strings.action_cancel)).orEmpty(),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        Button(
                            onClick = {
                                onDismissRequest()
                                val (addIds, removeIds) = categorySelectionChanges(initialSelection, selection)
                                onConfirm(addIds, removeIds)
                            },
                            modifier = Modifier.weight(1.4f).heightIn(min = 52.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tokens.accent,
                                contentColor = tokens.onAccent,
                            ),
                        ) {
                            Text(
                                text = tsuzukiCase(stringResource(MR.strings.action_ok)).orEmpty(),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPickerRow(checkbox: CheckboxState<Category>, onClick: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val state = when (checkbox) {
        is CheckboxState.TriState -> checkbox.asToggleableState()
        is CheckboxState.State -> ToggleableState(checkbox.isChecked)
    }
    val active = state != ToggleableState.Off

    Row(
        modifier = Modifier.fillMaxWidth()
            .triStateToggleable(state = state, role = Role.Checkbox, onClick = onClick)
            .heightIn(min = 50.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(26.dp),
            shape = RoundedCornerShape(8.dp),
            color = if (active) tokens.accent else tokens.tile,
            border = if (active) null else BorderStroke(1.dp, tokens.dotOn.copy(alpha = 0.56f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (active) {
                    Icon(
                        imageVector = if (state == ToggleableState.On) Icons.Filled.Check else Icons.Filled.Remove,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = tokens.onAccent,
                    )
                }
            }
        }
        Text(
            text = tsuzukiCase(checkbox.value.visualName).orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            color = tokens.dotOn,
        )
    }
}

/** Keep the same add/remove contract as the stock picker, including mixed selection. */
internal fun categorySelectionChanges(
    initialSelection: List<CheckboxState<Category>>,
    selection: List<CheckboxState<Category>>,
): Pair<List<Long>, List<Long>> {
    val initialById = initialSelection.associateBy { it.value.id }
    val addIds = mutableListOf<Long>()
    val removeIds = mutableListOf<Long>()
    for (current in selection) {
        val id = current.value.id
        val initial = initialById[id]
        val initiallyChecked = initial is CheckboxState.State.Checked || initial is CheckboxState.TriState.Include
        when (current) {
            is CheckboxState.State.Checked, is CheckboxState.TriState.Include -> if (!initiallyChecked) addIds += id
            is CheckboxState.State.None -> if (initiallyChecked) removeIds += id
            is CheckboxState.TriState.Exclude -> removeIds += id
            is CheckboxState.TriState.None -> Unit
        }
    }
    return addIds to removeIds
}
