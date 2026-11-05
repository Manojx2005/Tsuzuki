package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.tsuzuki.extension.toReport
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Explains why an installed extension could not be loaded, and lets the user copy the report (reason and
 * stack trace) or uninstall it.
 */
@Composable
fun ExtensionFailedDialog(
    extension: Extension.Failed,
    onClickUninstall: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = extension.name) },
        text = {
            Column {
                Text(
                    text = stringResource(MR.strings.tsz_ext_failed_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = failedReasonText(extension),
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.error,
                )
                if (!extension.stackTrace.isNullOrBlank()) {
                    Text(
                        text = stringResource(MR.strings.tsz_ext_failed_trace),
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    SelectionContainer {
                        Text(
                            text = extension.stackTrace!!,
                            modifier = Modifier
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState()),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    context.copyToClipboard(extension.name, extension.toReport())
                },
            ) {
                Text(text = stringResource(MR.strings.action_copy_to_clipboard))
            }
        },
        dismissButton = {
            TextButton(onClick = onClickUninstall) {
                Text(text = stringResource(MR.strings.ext_uninstall))
            }
        },
    )
}

private fun Extension.Failed.Reason.stringRes(): StringResource = when (this) {
    Extension.Failed.Reason.MISSING_VERSION -> MR.strings.tsz_ext_failed_missing_version
    Extension.Failed.Reason.UNSUPPORTED_LIB_VERSION -> MR.strings.tsz_ext_failed_unsupported_lib
    Extension.Failed.Reason.NOT_SIGNED -> MR.strings.tsz_ext_failed_not_signed
    Extension.Failed.Reason.CLASS_LOADER -> MR.strings.tsz_ext_failed_class_loader
    Extension.Failed.Reason.MISSING_SOURCE_CLASS -> MR.strings.tsz_ext_failed_missing_class
    Extension.Failed.Reason.SOURCE_CLASS_NOT_FOUND -> MR.strings.tsz_ext_failed_class_not_found
    Extension.Failed.Reason.SOURCE_LOAD_ERROR -> MR.strings.tsz_ext_failed_load_error
}

@Composable
fun failedReasonText(extension: Extension.Failed): String {
    val base = stringResource(extension.reason.stringRes())
    val detail = extension.detail
    return if (detail.isNullOrBlank()) base else "$base: $detail"
}
