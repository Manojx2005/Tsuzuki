package eu.kanade.presentation.tsuzuki.dictionary

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.view.Menu
import android.view.MenuItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import eu.kanade.presentation.theme.tsuzuki.TsuzukiAdaptiveSheet
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.tsuzuki.dictionary.DictionaryEntry
import eu.kanade.tachiyomi.tsuzuki.dictionary.DictionaryPacks
import eu.kanade.tachiyomi.tsuzuki.dictionary.DictionaryTerm
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.core.common.i18n.stringResource as contextStringResource

internal const val DICTIONARY_SELECTION_MENU_ID = 0xD107

internal fun Menu.addDictionaryAction(context: Context) {
    add(
        Menu.NONE,
        DICTIONARY_SELECTION_MENU_ID,
        Menu.NONE,
        context.contextStringResource(MR.strings.tsz_dictionary_lookup),
    )
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
}

internal data class ReaderDictionarySelection(
    val term: DictionaryTerm,
    val chapterName: String,
    val paragraphIndex: Int?,
)

@Composable
internal fun ReaderDictionarySheet(
    selection: ReaderDictionarySelection,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val term = selection.term
    val packs = remember { Injekt.get<DictionaryPacks>() }
    var showPacks by remember(term) { mutableStateOf(false) }
    val result by produceState<LookupResult?>(null, term, showPacks) {
        if (!showPacks) {
            value = LookupResult(packs.installed(term.language) != null, packs.lookup(term))
        }
    }

    TsuzukiAdaptiveSheet(onDismissRequest = onDismiss) { close ->
        if (showPacks) {
            Column {
                TextButton(onClick = { showPacks = false }) {
                    Text(tsuzukiCase(stringResource(MR.strings.tsz_dictionary_lookup)).orEmpty())
                }
                DictionaryPackList()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val first = result?.entries?.firstOrNull()
                Text(
                    tsuzukiCase(first?.headword ?: term.value).orEmpty(),
                    style = MaterialTheme.typography.displaySmall,
                )
                if (first?.reading?.isNotBlank() == true) {
                    Text(
                        first.reading,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TsuzukiTheme.tokens.mutedText,
                    )
                }
                val current = result
                when {
                    current == null -> Text(stringResource(MR.strings.loading))
                    !current.hasPack -> TextButton(onClick = { showPacks = true }) {
                        Text(tsuzukiCase(stringResource(MR.strings.tsz_dictionary_get_pack)).orEmpty())
                    }
                    current.entries.isEmpty() -> Text(stringResource(MR.strings.tsz_dictionary_no_match))
                    else -> current.entries.forEach { entry -> DefinitionTile(entry, first) }
                }
                DictionaryFallbacks(term.value)
                Button(onClick = { close(onSave) }, modifier = Modifier.fillMaxWidth()) {
                    Text(tsuzukiCase(stringResource(MR.strings.tsz_dictionary_save)).orEmpty())
                }
            }
        }
    }
}

@Composable
private fun DefinitionTile(entry: DictionaryEntry, first: DictionaryEntry?) {
    val tokens = TsuzukiTheme.tokens
    TsuzukiTile {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (entry.headword != first?.headword || entry.reading != first?.reading) {
                Text(
                    listOf(entry.headword, entry.reading).filter(String::isNotBlank).distinct().joinToString(" · "),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            var lastPart: String? = null
            entry.senses.forEachIndexed { index, sense ->
                if (sense.partOfSpeech.isNotBlank() && sense.partOfSpeech != lastPart) {
                    Text(sense.partOfSpeech, style = MaterialTheme.typography.labelMedium, color = tokens.mutedText)
                    lastPart = sense.partOfSpeech
                }
                Text("${index + 1}. ${sense.gloss}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DictionaryFallbacks(word: String) {
    val context = LocalContext.current
    val error = stringResource(MR.strings.tsz_dictionary_no_handler)
    val handlers = remember(context) {
        context.packageManager.queryIntentActivities(processTextIntent(""), 0)
            .filter { it.activityInfo.exported && it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName to it.activityInfo.name }
            .take(8)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        handlers.forEach { handler ->
            TextButton(onClick = { openProcessText(context, handler, word, error) }) {
                Text(handler.loadLabel(context.packageManager).toString())
            }
        }
        TextButton(onClick = { openWebSearch(context, word, error) }) {
            Text(tsuzukiCase(stringResource(MR.strings.tsz_dictionary_web_search)).orEmpty())
        }
    }
}

private fun processTextIntent(word: String) = Intent(Intent.ACTION_PROCESS_TEXT)
    .setType("text/plain")
    .putExtra(Intent.EXTRA_PROCESS_TEXT, word)
    .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)

private fun openProcessText(context: Context, handler: ResolveInfo, word: String, error: String) {
    try {
        context.startActivity(
            processTextIntent(
                word,
            ).setComponent(ComponentName(handler.activityInfo.packageName, handler.activityInfo.name)),
        )
    } catch (_: ActivityNotFoundException) {
        context.toast(error)
    } catch (_: SecurityException) {
        context.toast(error)
    }
}

private fun openWebSearch(context: Context, word: String, error: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, word))
    } catch (_: ActivityNotFoundException) {
        val url = "https://www.google.com/search".toUri().buildUpon().appendQueryParameter("q", word).build()
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url))
        } catch (_: ActivityNotFoundException) {
            context.toast(error)
        }
    }
}

private data class LookupResult(val hasPack: Boolean, val entries: List<DictionaryEntry>)
