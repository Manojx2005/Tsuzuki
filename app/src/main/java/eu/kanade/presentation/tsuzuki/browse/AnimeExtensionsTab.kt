package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.ExtensionTrustDialog
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeSourceManager
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtension
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtensionCatalogue
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtensionInstaller
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtensionRepo
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtensionRepos
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeLoadResult
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeStoreAction
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeStoreRow
import eu.kanade.tachiyomi.tsuzuki.anime.extension.animeStoreRows
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimeSourcePreferencesActivity
import eu.kanade.tachiyomi.tsuzuki.anime.ui.hasAnimeSourceSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.ui.res.stringResource as androidStringResource

internal fun animeExtensionsTab(repos: List<AnimeExtensionRepo>) =
    TabContent(MR.strings.label_extensions) { padding, _ ->
        val context = LocalContext.current.applicationContext
        val navigator = LocalNavigator.currentOrThrow
        val installer = remember(context) { AnimeExtensionInstaller(context) }
        val store = remember(installer) { AnimeExtensionRepos(Injekt.get(), installer) }
        val scope = rememberCoroutineScope()
        val tokens = TsuzukiTheme.tokens
        val owner = LocalLifecycleOwner.current
        var reload by remember { mutableIntStateOf(0) }
        var section by rememberSaveable { mutableIntStateOf(0) }
        var busy by remember { mutableStateOf<String?>(null) }
        var operationFailed by remember { mutableStateOf(false) }
        var trust by remember { mutableStateOf<AnimeExtension.Untrusted?>(null) }
        var remove by remember { mutableStateOf<AnimeStoreRow?>(null) }
        val catalogue by produceState<AnimeExtensionCatalogue?>(null, store, repos, reload) {
            value = null
            value = store.catalogue(repos)
        }
        val installed by produceState(emptyMap(), installer, owner, reload) {
            owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                AnimeSourceManager.extensionChanges.collect {
                    value = installer.installed()
                }
            }
        }
        // AY2: the loaded sources with settings of their own, by extension package.
        val configurable by produceState(emptyMap<String, List<AnimeSource>>(), installed) {
            value = try {
                Injekt.get<AnimeSourceManager>().ensureLoaded()
                    .filter { hasAnimeSourceSettings(it.source) }
                    .groupBy({ it.extensionPackage }, { it.source })
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyMap()
            }
        }
        var pickSettings by remember { mutableStateOf<List<AnimeSource>?>(null) }
        val activityContext = LocalContext.current
        val openSettings = { sources: List<AnimeSource> ->
            if (sources.size == 1) {
                activityContext.startActivity(AnimeSourcePreferencesActivity.intent(activityContext, sources[0].id))
            } else {
                pickSettings = sources
            }
        }
        val rows by produceState(emptyList<AnimeStoreRow>(), catalogue, installed) {
            value = withContext(Dispatchers.IO) {
                animeStoreRows(catalogue?.entries.orEmpty(), installed, installer::isPrivate)
            }
        }
        val groups = remember(rows) {
            listOf(
                rows.filter { it.action == AnimeStoreAction.INSTALL },
                rows.filter { it.action != AnimeStoreAction.INSTALL },
                rows.filter { it.action == AnimeStoreAction.UPDATE },
            )
        }

        fun mutate(pkg: String, operation: suspend () -> Unit) {
            if (busy != null) return
            busy = pkg
            operationFailed = false
            scope.launch {
                try {
                    operation()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    operationFailed = true
                } finally {
                    busy = null
                }
            }
        }

        LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TsuzukiPill(
                        text = stringResource(MR.strings.extensionStores),
                        selected = false,
                        onClick = { navigator.push(AnimeExtensionReposScreen) },
                        role = Role.Button,
                    )
                    TsuzukiPill(
                        text = stringResource(MR.strings.action_webview_refresh),
                        selected = false,
                        onClick = { reload++ },
                        role = Role.Button,
                    )
                }
            }
            item {
                TsuzukiPillRow(
                    labels = listOf(
                        androidStringResource(R.string.tsz_anime_extensions_available),
                        stringResource(MR.strings.ext_installed),
                        stringResource(MR.strings.ext_updates_pending),
                    ),
                    selected = section,
                    onClick = { section = it },
                    counts = groups.map(List<AnimeStoreRow>::size),
                )
            }
            if (catalogue == null) {
                item { CircularProgressIndicator(Modifier.padding(16.dp), color = tokens.accent) }
            }
            if (catalogue?.failed == true || operationFailed) {
                item {
                    val error = if (operationFailed) MR.strings.unknown_error else MR.strings.extension_api_error
                    Text(
                        stringResource(error),
                        color = tokens.dotOn,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            val shown = groups.getOrElse(section) { emptyList() }
            if (shown.isEmpty() && catalogue != null) {
                item {
                    Text(
                        stringResource(MR.strings.no_results_found),
                        color = tokens.mutedText,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(shown, key = AnimeStoreRow::pkgName) { row ->
                AnimeStoreExtensionRow(
                    row = row,
                    busy = busy == row.pkgName,
                    enabled = busy == null,
                    onAction = {
                        when (row.action) {
                            AnimeStoreAction.INSTALL, AnimeStoreAction.UPDATE -> {
                                val listed = checkNotNull(row.listed)
                                mutate(row.pkgName) {
                                    val result = installer.install(listed.index, listed.entry)
                                    check(result != AnimeLoadResult.Error)
                                    if (result is AnimeLoadResult.Untrusted) trust = result.extension
                                }
                            }
                            AnimeStoreAction.TRUST -> trust = row.extension as? AnimeExtension.Untrusted
                            AnimeStoreAction.INSTALLED, AnimeStoreAction.BROKEN -> Unit
                        }
                    },
                    onRemove = { remove = row },
                    onSettings = configurable[row.pkgName]?.let { sources -> { openSettings(sources) } },
                )
            }
        }
        pickSettings?.let { sources ->
            AlertDialog(
                title = { Text(stringResource(MR.strings.source_settings)) },
                text = {
                    Column {
                        sources.forEach { source ->
                            TextButton(onClick = {
                                pickSettings = null
                                activityContext.startActivity(
                                    AnimeSourcePreferencesActivity.intent(activityContext, source.id),
                                )
                            }) { Text(tsuzukiMenuLabel("${source.name} · ${source.lang}")) }
                        }
                    }
                },
                onDismissRequest = { pickSettings = null },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { pickSettings = null }) { Text(stringResource(MR.strings.action_cancel)) }
                },
            )
        }
        trust?.let { extension ->
            ExtensionTrustDialog(
                details = "${extension.name} · ${extension.versionName}\n${extension.signatureHash}",
                dismissLabel = stringResource(MR.strings.action_cancel),
                onClickConfirm = {
                    trust = null
                    mutate(extension.pkgName) { check(installer.trust(extension) != AnimeLoadResult.Error) }
                },
                onClickDismiss = { trust = null },
                onDismissRequest = { trust = null },
            )
        }
        remove?.let { row ->
            AlertDialog(
                title = { Text(stringResource(MR.strings.ext_confirm_remove)) },
                text = { Text(row.name) },
                onDismissRequest = { remove = null },
                confirmButton = {
                    TextButton(onClick = {
                        remove = null
                        mutate(row.pkgName) { installer.remove(row.pkgName) }
                    }) { Text(stringResource(MR.strings.ext_uninstall)) }
                },
                dismissButton = {
                    TextButton(onClick = { remove = null }) { Text(stringResource(MR.strings.action_cancel)) }
                },
            )
        }
    }

@Composable
private fun AnimeStoreExtensionRow(
    row: AnimeStoreRow,
    busy: Boolean,
    enabled: Boolean,
    onAction: () -> Unit,
    onRemove: () -> Unit,
    onSettings: (() -> Unit)? = null,
) {
    val tokens = TsuzukiTheme.tokens
    val action = when (row.action) {
        AnimeStoreAction.INSTALL -> MR.strings.ext_install
        AnimeStoreAction.UPDATE -> MR.strings.ext_update
        AnimeStoreAction.TRUST -> MR.strings.ext_trust
        AnimeStoreAction.INSTALLED, AnimeStoreAction.BROKEN -> null
    }
    TsuzukiTile(Modifier.fillMaxWidth().padding(horizontal = 12.dp), padding = 12.dp) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tsuzukiMenuLabel(row.name), color = tokens.dotOn, style = MaterialTheme.typography.titleSmall)
                val language = row.extension?.lang ?: row.listed?.entry?.lang.orEmpty()
                val version = if (row.action == AnimeStoreAction.UPDATE) {
                    "${row.version} → ${row.listed?.entry?.versionName.orEmpty()}"
                } else {
                    row.version
                }
                val meta = listOf(version, language).filter(String::isNotBlank).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(tsuzukiMenuLabel(meta), color = tokens.mutedText, style = MaterialTheme.typography.labelSmall)
                }
                if (row.action == AnimeStoreAction.BROKEN) {
                    Text(
                        stringResource(MR.strings.unknown_error),
                        color = tokens.mutedText,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            if (busy) {
                CircularProgressIndicator(color = tokens.accent)
            } else {
                if (action != null) {
                    TextButton(onClick = onAction, enabled = enabled) {
                        Text(tsuzukiMenuLabel(stringResource(action)))
                    }
                }
                if (onSettings != null) {
                    IconButton(onClick = onSettings, enabled = enabled) {
                        Icon(Icons.Outlined.Settings, stringResource(MR.strings.source_settings))
                    }
                }
                if (row.removable) {
                    IconButton(onClick = onRemove, enabled = enabled) {
                        Icon(Icons.Outlined.Delete, stringResource(MR.strings.ext_uninstall))
                    }
                }
            }
        }
    }
}
