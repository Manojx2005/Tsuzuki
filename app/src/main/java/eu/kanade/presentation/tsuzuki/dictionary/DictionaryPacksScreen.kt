package eu.kanade.presentation.tsuzuki.dictionary

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.tsuzuki.dictionary.DictionaryLanguage
import eu.kanade.tachiyomi.tsuzuki.dictionary.DictionaryPackManifest
import eu.kanade.tachiyomi.tsuzuki.dictionary.DictionaryPacks
import eu.kanade.tachiyomi.tsuzuki.dictionary.InstalledDictionary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class DictionaryPacksScreen : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = {
                AppBar(title = stringResource(MR.strings.tsz_dictionary_packs), navigateUp = navigator::pop)
            },
        ) { padding ->
            DictionaryPackList(Modifier.padding(padding))
        }
    }
}

@Composable
internal fun DictionaryPackList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DictionaryLanguage.entries.forEach { language -> DictionaryPackTile(language) }
    }
}

@Composable
private fun DictionaryPackTile(language: DictionaryLanguage) {
    val context = LocalContext.current
    val packs = remember { Injekt.get<DictionaryPacks>() }
    val scope = rememberCoroutineScope()
    var installed by remember(language) { mutableStateOf<InstalledDictionary?>(null) }
    var available by remember(language) { mutableStateOf<DictionaryPackManifest?>(null) }
    var busy by remember(language) { mutableStateOf(true) }
    var error by remember(language) { mutableStateOf(false) }
    var showLicense by remember(language) { mutableStateOf(false) }
    val name = stringResource(
        when (language) {
            DictionaryLanguage.English -> MR.strings.tsz_dictionary_english
            DictionaryLanguage.Japanese -> MR.strings.tsz_dictionary_japanese
        },
    )

    suspend fun runOperation(action: suspend () -> Unit) {
        busy = true
        error = false
        try {
            action()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            error = true
        } finally {
            busy = false
        }
    }
    suspend fun refresh() = runOperation {
        installed = packs.installed(language)
        available = packs.available(language)
    }
    LaunchedEffect(language) { refresh() }

    TsuzukiTile {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tsuzukiCase(name).orEmpty(), style = MaterialTheme.typography.titleLarge)
            Text(language.attribution, style = MaterialTheme.typography.bodyMedium)
            Text(language.licenseName, style = MaterialTheme.typography.labelMedium)
            val local = installed
            val remote = available
            val size = local?.sizeBytes ?: remote?.sizeBytes
            val version = local?.version ?: remote?.version
            if (size != null && version != null) {
                Text(
                    "$version · ${Formatter.formatFileSize(context, size)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (busy) {
                Text(stringResource(MR.strings.loading), style = MaterialTheme.typography.bodySmall)
            } else if (error) {
                Text(
                    stringResource(MR.strings.tsz_dictionary_pack_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else if (local == null && remote == null) {
                Text(
                    stringResource(MR.strings.tsz_dictionary_pack_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (remote != null && local?.version != remote.version) {
                    TextButton(
                        enabled = !busy,
                        onClick = { scope.launch { runOperation { installed = packs.install(language, remote) } } },
                    ) {
                        Text(
                            stringResource(
                                if (local ==
                                    null
                                ) {
                                    MR.strings.action_download
                                } else {
                                    MR.strings.tsz_dictionary_update
                                },
                            ),
                        )
                    }
                }
                if (local != null) {
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            scope.launch {
                                runOperation {
                                    packs.delete(language)
                                    installed = null
                                }
                            }
                        },
                    ) {
                        Text(stringResource(MR.strings.action_delete))
                    }
                    TextButton(onClick = { showLicense = true }) {
                        Text(stringResource(MR.strings.licenses))
                    }
                }
                if (error) {
                    TextButton(enabled = !busy, onClick = { scope.launch { refresh() } }) {
                        Text(stringResource(MR.strings.action_retry))
                    }
                }
            }
        }
    }

    val license = installed?.license
    if (showLicense && license != null) {
        AlertDialog(
            onDismissRequest = { showLicense = false },
            title = { Text(language.licenseName) },
            text = {
                Text(
                    license,
                    modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = { showLicense = false }) { Text(stringResource(MR.strings.action_close)) }
            },
        )
    }
}
