package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun SensitiveEditTextPreference(
    title: String,
    isConfigured: Boolean,
    onConfirm: suspend (String) -> Boolean,
) {
    var editing by remember { mutableStateOf(false) }
    TextPreferenceWidget(
        title = title,
        subtitle = if (isConfigured) "••••••••" else stringResource(TDMR.strings.not_set),
        onPreferenceClick = { editing = true },
    )
    if (!editing) return

    // Keep only the draft replacement in memory and never prefill the stored credential.
    val draft = remember { TextFieldState() }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { editing = false },
        title = { Text(title) },
        text = {
            OutlinedSecureTextField(
                state = draft,
                modifier = Modifier.fillMaxWidth(),
                textObfuscationMode = TextObfuscationMode.Hidden,
            )
        },
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
        confirmButton = {
            TextButton(
                enabled = draft.text.isNotBlank(),
                onClick = {
                    scope.launch {
                        if (onConfirm(draft.text.toString())) editing = false
                    }
                },
            ) { Text(stringResource(MR.strings.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = { editing = false }) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}
