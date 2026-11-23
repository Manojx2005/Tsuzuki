package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import eu.kanade.presentation.browse.ExtensionTrustDialog
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
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
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.ui.res.stringResource as androidStringResource

/**
 * Anime extensions (board "Extensions", 04-browse; redesign owner 2026-11-22): updates-ready and Update all tiles,
 * language pills, then Updates / Installed / Available as grouped tiles with each extension's own icon. Names drop
 * the "Aniyomi: " prefix every repo puts on them. [query] comes from the Browse search field.
 */
@Composable
internal fun animeExtensionsTab(repos: List<AnimeExtensionRepo>, query: String?): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    var reload by remember { mutableIntStateOf(0) }
    return TabContent(
        titleRes = MR.strings.label_extensions,
        searchEnabled = true,
        actions = listOf(
            AppBar.Action(
                title = stringResource(MR.strings.extensionStores),
                icon = Icons.Outlined.Layers,
                onClick = { navigator.push(AnimeExtensionReposScreen) },
            ),
            AppBar.Action(
                title = stringResource(MR.strings.action_webview_refresh),
                icon = Icons.Outlined.Refresh,
                onClick = { reload++ },
            ),
        ),
    ) { padding, _ ->
        val context = LocalContext.current.applicationContext
        val installer = remember(context) { AnimeExtensionInstaller(context) }
        val store = remember(installer) { AnimeExtensionRepos(Injekt.get(), installer) }
        val scope = rememberCoroutineScope()
        val tokens = TsuzukiTheme.tokens
        val owner = LocalLifecycleOwner.current
        var language by rememberSaveable { mutableStateOf<String?>(null) }
        var busy by remember { mutableStateOf<String?>(null) }
        var operationFailed by remember { mutableStateOf(false) }
        var trust by remember { mutableStateOf<AnimeExtension.Untrusted?>(null) }
        var remove by remember { mutableStateOf<AnimeStoreRow?>(null) }
        var details by remember { mutableStateOf<AnimeStoreRow?>(null) }
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
        val languages = remember(rows) { animeExtensionLanguages(rows) }
        if (language != null && language !in languages) language = null
        val shown = remember(rows, language, query) { filterAnimeStoreRows(rows, language, query) }
        val updates = shown.filter { it.action == AnimeStoreAction.UPDATE }
        val installedRows = shown.filter {
            it.action == AnimeStoreAction.INSTALLED || it.action == AnimeStoreAction.TRUST ||
                it.action == AnimeStoreAction.BROKEN
        }
        val available = shown.filter { it.action == AnimeStoreAction.INSTALL }

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

        fun install(row: AnimeStoreRow) {
            val listed = row.listed ?: return
            mutate(row.pkgName) {
                val result = installer.install(listed.index, listed.entry)
                check(result != AnimeLoadResult.Error)
                if (result is AnimeLoadResult.Untrusted) trust = result.extension
            }
        }

        val onAction: (AnimeStoreRow) -> Unit = { row ->
            when (row.action) {
                AnimeStoreAction.INSTALL, AnimeStoreAction.UPDATE -> install(row)
                AnimeStoreAction.TRUST -> trust = row.extension as? AnimeExtension.Untrusted
                AnimeStoreAction.INSTALLED, AnimeStoreAction.BROKEN -> Unit
            }
        }
        val labelUpdate = stringResource(MR.strings.ext_update)
        val labelInstalled = stringResource(MR.strings.ext_installed)
        val labelAvailable = androidStringResource(R.string.tsz_anime_extensions_available)

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            if (updates.isNotEmpty()) {
                item(key = "updates-summary") {
                    AnimeUpdatesSummary(
                        count = updates.size,
                        enabled = busy == null,
                        onUpdateAll = {
                            mutate(updates.first().pkgName) {
                                updates.forEach { row ->
                                    val listed = row.listed ?: return@forEach
                                    check(installer.install(listed.index, listed.entry) != AnimeLoadResult.Error)
                                }
                            }
                        },
                    )
                }
            }
            if (languages.size > 1) {
                item(key = "languages") {
                    TsuzukiExtensionLanguagePills(
                        languages = languages,
                        selected = language,
                        onSelect = { language = it },
                        labelFor = { LocaleHelper.getSourceDisplayName(it, activityContext) },
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            if (catalogue == null) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = tokens.accent)
                    }
                }
            }
            if (catalogue?.failed == true || operationFailed) {
                item(key = "error") {
                    val error = if (operationFailed) MR.strings.unknown_error else MR.strings.extension_api_error
                    Text(
                        tsuzukiMenuLabel(stringResource(error)),
                        color = tokens.mutedText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            if (catalogue != null && shown.isEmpty()) {
                item(key = "empty") {
                    Column {
                        Text(
                            tsuzukiMenuLabel(stringResource(MR.strings.no_results_found)),
                            color = tokens.mutedText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                        // AP-18: with no repository yet, point straight at adding one.
                        if (repos.isEmpty()) {
                            TextButton(onClick = { navigator.push(AnimeExtensionReposScreen) }) {
                                Text(tsuzukiMenuLabel(androidStringResource(R.string.tsz_anime_extension_repos)))
                            }
                        }
                    }
                }
            }
            listOf(
                Triple("update", labelUpdate, updates),
                Triple("installed", labelInstalled, installedRows),
                Triple("available", "$labelAvailable · ${available.size}", available),
            ).forEach { (key, label, group) ->
                if (group.isEmpty()) return@forEach
                item(key = "label-$key") { TsuzukiSectionLabel(label) }
                itemsIndexed(group, key = { _, row -> "$key-${row.pkgName}" }) { index, row ->
                    AnimeStoreExtensionRow(
                        row = row,
                        modifier = Modifier.then(
                            tsuzukiGroupedTile(first = index == 0, last = index == group.lastIndex),
                        ),
                        busy = busy == row.pkgName,
                        enabled = busy == null,
                        onAction = { onAction(row) },
                        onRemove = { remove = row },
                        onSettings = configurable[row.pkgName]?.let { sources -> { openSettings(sources) } },
                        onClick = { details = row }.takeIf { row.extension != null },
                    )
                }
            }
        }
        details?.let { row ->
            AnimeExtensionDetailsSheet(
                row = row,
                configurable = configurable[row.pkgName].orEmpty(),
                enabled = busy == null,
                onDismiss = { details = null },
                onAction = {
                    details = null
                    onAction(row)
                },
                onRemove = {
                    details = null
                    remove = row
                },
                onSourceSettings = { source ->
                    details = null
                    activityContext.startActivity(AnimeSourcePreferencesActivity.intent(activityContext, source.id))
                },
            )
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
                // Private installs can be removed here; a shared (system-installed) one is uninstalled in Android.
                dismissLabel = stringResource(
                    if (installer.isPrivate(extension.pkgName)) MR.strings.ext_uninstall else MR.strings.action_cancel,
                ),
                onClickConfirm = {
                    trust = null
                    mutate(extension.pkgName) { check(installer.trust(extension) != AnimeLoadResult.Error) }
                },
                onClickDismiss = {
                    trust = null
                    if (installer.isPrivate(extension.pkgName)) {
                        mutate(extension.pkgName) { installer.remove(extension.pkgName) }
                    }
                },
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
}

@Composable
private fun AnimeUpdatesSummary(count: Int, enabled: Boolean, onUpdateAll: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TsuzukiTile(Modifier.weight(1f).fillMaxHeight(), padding = 16.dp) {
            Text(
                tsuzukiMenuLabel(stringResource(MR.strings.ext_updates_pending)),
                color = tokens.mutedText,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(count.toString(), color = tokens.dotOn, style = MaterialTheme.typography.displaySmall)
        }
        Surface(
            onClick = onUpdateAll,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            enabled = enabled,
            shape = RoundedCornerShape(24.dp),
            color = tokens.accent,
            contentColor = tokens.onAccent,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Text(
                    tsuzukiMenuLabel(stringResource(MR.strings.ext_update_all)),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/** Repos name every anime extension "Aniyomi: Name"; the prefix says nothing here. */
internal fun cleanAnimeExtensionName(name: String): String {
    val trimmed = name.trim()
    val prefix = listOf("Aniyomi:", "Tachiyomi:").firstOrNull { trimmed.startsWith(it, ignoreCase = true) }
    return prefix?.let { trimmed.substring(it.length).trim() }?.takeIf(String::isNotEmpty) ?: trimmed
}

private fun AnimeStoreRow.language(): String = extension?.lang ?: listed?.entry?.lang.orEmpty()

/** Languages in the list, "all"/"multi" first, then by code. */
internal fun animeExtensionLanguages(rows: List<AnimeStoreRow>): List<String> = rows.map { it.language() }
    .filter(String::isNotBlank)
    .distinct()
    .sortedWith(compareBy<String>({ it != "all" && it != "multi" }, { it }))

/** The rows for one language (null: all) whose name or package name contains [query], sorted by clean name. */
internal fun filterAnimeStoreRows(rows: List<AnimeStoreRow>, language: String?, query: String?): List<AnimeStoreRow> {
    val needle = query?.trim().orEmpty()
    return rows.filter { row ->
        val matchesLanguage = language == null || row.language() == language
        val matchesQuery = needle.isEmpty() ||
            cleanAnimeExtensionName(row.name).contains(needle, ignoreCase = true) ||
            // Only the package's last segment: every one contains "animeextension".
            row.pkgName.substringAfterLast('.').contains(needle, ignoreCase = true)
        matchesLanguage && matchesQuery
    }.sortedBy { cleanAnimeExtensionName(it.name).lowercase() }
}

/** The extension's own icon: the installed APK's, else the repo's `icon/<pkg>.png`; its initial underneath. */
@Composable
private fun AnimeExtensionIcon(row: AnimeStoreRow, modifier: Modifier = Modifier) {
    val tokens = TsuzukiTheme.tokens
    val name = cleanAnimeExtensionName(row.name)
    val drawable = (row.extension as? AnimeExtension.Installed)?.icon
    val bitmap = remember(drawable) {
        drawable?.let { runCatching { it.toBitmap(96, 96).asImageBitmap() }.getOrNull() }
    }
    val url = remember(row.listed) {
        row.listed?.let { it.index.resolve("icon/${it.entry.packageName}.png")?.toString() }
    }
    Surface(
        modifier = modifier.size(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = tokens.track,
        contentColor = tokens.dotOn,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleSmall)
            if (bitmap != null) {
                Image(bitmap, contentDescription = null, modifier = Modifier.matchParentSize())
            } else if (url != null) {
                AsyncImage(model = url, contentDescription = null, modifier = Modifier.matchParentSize())
            }
        }
    }
}

@Composable
private fun AnimeStoreExtensionRow(
    row: AnimeStoreRow,
    busy: Boolean,
    enabled: Boolean,
    onAction: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    onSettings: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val tokens = TsuzukiTheme.tokens
    val context = LocalContext.current
    Row(
        modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .heightIn(min = 72.dp).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimeExtensionIcon(row)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                tsuzukiMenuLabel(cleanAnimeExtensionName(row.name)),
                color = tokens.dotOn,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val version = if (row.action == AnimeStoreAction.UPDATE) {
                "${row.version} → ${row.listed?.entry?.versionName.orEmpty()}"
            } else {
                row.version
            }
            val language = row.language().takeIf(String::isNotBlank)
                ?.let { LocaleHelper.getSourceDisplayName(it, context) }
                .orEmpty()
            val broken = if (row.action == AnimeStoreAction.BROKEN) stringResource(MR.strings.unknown_error) else ""
            val meta = listOf(version, language, broken).filter(String::isNotBlank).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(
                    tsuzukiMenuLabel(meta),
                    color = tokens.mutedText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (busy) {
            CircularProgressIndicator(Modifier.size(28.dp), color = tokens.accent, strokeWidth = 3.dp)
        } else {
            val label = when (row.action) {
                AnimeStoreAction.INSTALL -> MR.strings.ext_install
                AnimeStoreAction.UPDATE -> MR.strings.ext_update
                AnimeStoreAction.TRUST -> MR.strings.ext_trust
                AnimeStoreAction.INSTALLED, AnimeStoreAction.BROKEN -> null
            }
            if (label != null) {
                val filled = row.action != AnimeStoreAction.INSTALL
                Surface(
                    onClick = onAction,
                    enabled = enabled,
                    shape = RoundedCornerShape(50),
                    color = if (filled) tokens.accent else Color.Transparent,
                    contentColor = if (filled) tokens.onAccent else tokens.dotOn,
                    border = if (filled) null else BorderStroke(1.dp, tokens.mutedText),
                ) {
                    Text(
                        tsuzukiMenuLabel(stringResource(label)),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            if (onSettings != null) {
                IconButton(onClick = onSettings, enabled = enabled) {
                    Icon(Icons.Outlined.Settings, stringResource(MR.strings.source_settings), tint = tokens.mutedText)
                }
            }
            // A row with an action pill keeps its name readable: uninstall is in that action's dialog instead.
            if (row.removable && label == null) {
                IconButton(onClick = onRemove, enabled = enabled) {
                    Icon(Icons.Outlined.Delete, stringResource(MR.strings.ext_uninstall), tint = tokens.mutedText)
                }
            }
        }
    }
}
