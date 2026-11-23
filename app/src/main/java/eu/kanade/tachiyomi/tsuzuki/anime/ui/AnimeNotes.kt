package eu.kanade.tachiyomi.tsuzuki.anime.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** AP7: the title's notes, edited like book notes (stored in the anime's own `notes` column). */
@Composable
internal fun AnimeNotesDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tsuzukiMenuLabel(stringResource(MR.strings.action_edit_notes))) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim()) }) { Text(stringResource(MR.strings.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}

/** The saved notes under the details header, as book details show theirs. */
@Composable
internal fun AnimeNotesTile(notes: String, onClick: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    TsuzukiTile(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).tsuzukiClickable(role = Role.Button, onClick = onClick),
        padding = 14.dp,
    ) {
        Column {
            Text(
                tsuzukiMenuLabel(stringResource(MR.strings.action_edit_notes)),
                color = tokens.mutedText,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(notes, color = tokens.dotOn, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
