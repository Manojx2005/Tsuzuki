package eu.kanade.presentation.tsuzuki.dictionary

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.presentation.tsuzuki.settings.tsuzukiSettingsBackground
import eu.kanade.presentation.tsuzuki.settings.tsuzukiSettingsContainer
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
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
import androidx.compose.ui.res.stringResource as androidStringResource

internal class DictionaryPacksScreen : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            modifier = tsuzukiSettingsBackground(),
            containerColor = tsuzukiSettingsContainer() ?: MaterialTheme.colorScheme.background,
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
    val tokens = TsuzukiTheme.tokens
    val packs = remember { Injekt.get<DictionaryPacks>() }
    val scope = rememberCoroutineScope()
    var importRevision by remember { mutableIntStateOf(0) }
    var importing by remember { mutableStateOf(false) }
    var importFailed by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { files ->
        if (files.isEmpty() || importing) return@rememberLauncherForActivityResult
        scope.launch {
            importing = true
            importFailed = false
            try {
                packs.importFiles(files)
                importRevision++
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                importFailed = true
            } finally {
                importing = false
            }
        }
    }
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TsuzukiTile(Modifier.fillMaxWidth(), padding = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = androidStringResource(R.string.tsz_dictionary_import_matching_files),
                    color = tokens.mutedText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = { launcher.launch(arrayOf("*/*")) },
                    enabled = !importing,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.accent,
                        contentColor = tokens.onAccent,
                    ),
                ) {
                    Text(
                        tsuzukiMenuLabel(
                            if (importing) {
                                stringResource(MR.strings.loading)
                            } else {
                                androidStringResource(R.string.tsz_dictionary_import_file)
                            },
                        ),
                    )
                }
                if (importFailed) {
                    Text(
                        androidStringResource(R.string.tsz_dictionary_import_invalid),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        DictionaryLanguage.entries.forEach { language -> DictionaryPackTile(language, importRevision) }
    }
}

@Composable
private fun DictionaryPackTile(language: DictionaryLanguage, importRevision: Int) {
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
        try {
            available = packs.available(language)
        } catch (e: java.io.IOException) {
            if (installed == null) throw e
        }
    }
    LaunchedEffect(language, importRevision) { refresh() }

    TsuzukiTile(Modifier.fillMaxWidth()) {
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
            if (local?.isBundled == true) {
                Text(
                    tsuzukiMenuLabel(androidStringResource(R.string.tsz_dictionary_bundled)),
                    style = MaterialTheme.typography.labelMedium,
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
                if (local != null && !local.isBundled) {
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            scope.launch {
                                runOperation {
                                    packs.delete(language)
                                    installed = packs.installed(language)
                                }
                            }
                        },
                    ) {
                        Text(stringResource(MR.strings.action_delete))
                    }
                }
                if (local != null) {
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
