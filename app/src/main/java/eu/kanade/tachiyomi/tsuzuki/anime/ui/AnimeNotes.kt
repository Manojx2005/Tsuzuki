package eu.kanade.tachiyomi.tsuzuki.anime.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** AP7: the title's notes, edited like book notes (stored in the anime's own `notes` column). */
@Composable
internal fun AnimeNotesDialog(
    initial: String,
    saving: Boolean,
    saveError: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(tsuzukiMenuLabel(stringResource(MR.strings.action_edit_notes))) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                enabled = !saving,
                isError = saveError,
                supportingText = if (saveError) {
                    { Text(stringResource(MR.strings.unknown_error)) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
            )
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = { onSave(text.trim()) }) {
                Text(stringResource(MR.strings.action_save))
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}
