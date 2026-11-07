package eu.kanade.presentation.tsuzuki.reader

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.ui.reader.quote.Quote
import eu.kanade.tachiyomi.util.epub.EpubExportNaming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.io.IOException

@Composable
internal fun TsuzukiQuotesExport(novelTitle: String, quotes: List<Quote>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var writing by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        writing = true
        failed = false
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openOutputStream(uri) ?: throw IOException()
                    stream.bufferedWriter(Charsets.UTF_8).use { writeQuoteExport(it, novelTitle, quotes) }
                }
            } catch (_: IOException) {
                failed = true
            } catch (_: SecurityException) {
                failed = true
            } finally {
                writing = false
            }
        }
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            tsuzukiMenuLabel(stringResource(MR.strings.tsz_quotes_saved_count, quotes.size)),
            style = MaterialTheme.typography.labelLarge,
            color = TsuzukiTheme.tokens.accent,
        )
        TextButton(
            enabled = quotes.isNotEmpty() && !writing,
            onClick = {
                val title = EpubExportNaming.sanitizeFilename(novelTitle).ifBlank { "quotes" }
                launcher.launch("$title.txt")
            },
        ) {
            Text(tsuzukiMenuLabel(stringResource(MR.strings.tsz_quotes_export)))
        }
    }
    if (failed) {
        Text(stringResource(MR.strings.unknown_error), color = MaterialTheme.colorScheme.error)
    }
}

internal fun writeQuoteExport(output: Appendable, novelTitle: String, quotes: List<Quote>) {
    output.appendLine(novelTitle)
    for (quote in quotes) {
        output.appendLine()
        output.appendLine(quoteForSharing(quote))
    }
}
