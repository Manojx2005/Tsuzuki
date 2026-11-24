package eu.kanade.presentation.tsuzuki.browse

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeSourceManager
import eu.kanade.tachiyomi.tsuzuki.anime.RegisteredAnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtensionInstaller
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtensionRepo
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtensionRepos
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeLoadResult
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimeCatalogueActivity
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.ui.res.stringResource as androidStringResource

@Composable
internal fun rememberAnimeBrowseSources(): State<List<RegisteredAnimeSource>> {
    val appContext = LocalContext.current.applicationContext
    val owner = LocalLifecycleOwner.current
    val manager = remember { Injekt.get<AnimeSourceManager>() }
    return produceState(emptyList(), manager, owner) {
        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            AnimeSourceManager.extensionChanges.collect {
                try {
                    value = manager.refresh()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logcat(LogPriority.WARN, e) { "Could not refresh anime sources" }
                }
            }
        }
    }
}

@Composable
internal fun rememberAnimeBrowseRepos(): State<List<AnimeExtensionRepo>> {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { AnimeExtensionRepos(Injekt.get(), AnimeExtensionInstaller(context)) }
    return remember(repository) { repository.observe() }.collectAsState(emptyList())
}

@Composable
internal fun rememberAnimeSourcePins(): Pair<Preference<Set<String>>, Set<String>> {
    val preference = remember { Injekt.get<PreferenceStore>().getStringSet("tsuzuki_anime_pinned_sources") }
    val pins by remember(preference) { preference.changes() }.collectAsState(initial = preference.get())
    return preference to pins
}

/** Anime sources (board "BrowseAnime"): pinned first, each row with its extension's icon; [query] filters by name. */
@Composable
internal fun animeSourcesTab(
    sources: List<RegisteredAnimeSource>,
    query: String? = null,
    globalSearch: (() -> cafe.adriel.voyager.core.screen.Screen)? = null,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    return animeSourcesTabContent(sources, query, globalSearch?.let { screen -> { navigator.push(screen()) } })
}

@Composable
private fun animeSourcesTabContent(
    sources: List<RegisteredAnimeSource>,
    query: String?,
    onGlobalSearch: (() -> Unit)?,
) = TabContent(
    MR.strings.label_sources,
    searchEnabled = true,
    actions = listOfNotNull(
        onGlobalSearch?.let {
            AppBar.Action(
                title = stringResource(MR.strings.action_global_search),
                icon = Icons.Outlined.TravelExplore,
                onClick = {
                    AnimeGlobalSearch.openAnimeFirst()
                    it()
                },
            )
        },
    ),
) { padding, _ ->
    val context = LocalContext.current
    val tokens = TsuzukiTheme.tokens
    val (pinPreference, pins) = rememberAnimeSourcePins()
    val icons by produceState(emptyMap<String, ImageBitmap>(), sources) {
        value = withContext(Dispatchers.IO) {
            AnimeExtensionInstaller(context.applicationContext).installed().mapNotNull { (pkg, result) ->
                val icon = (result as? AnimeLoadResult.Success)?.extension?.icon ?: return@mapNotNull null
                runCatching { pkg to icon.toBitmap(96, 96).asImageBitmap() }.getOrNull()
            }.toMap()
        }
    }
    val needle = query?.trim().orEmpty()
    val ordered = remember(sources, pins, needle) {
        sources.filter { needle.isEmpty() || it.source.name.contains(needle, ignoreCase = true) }
            .sortedWith(compareBy({ it.source.id.toString() !in pins }, { it.source.name.lowercase() }))
    }
    val pinnedCount = ordered.count { it.source.id.toString() in pins }
    LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (sources.isEmpty()) {
            item {
                val navigator = LocalNavigator.currentOrThrow
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        tsuzukiMenuLabel(stringResource(MR.strings.no_results_found)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.mutedText,
                    )
                    TextButton(onClick = { navigator.push(AnimeExtensionReposScreen) }) {
                        Text(tsuzukiMenuLabel(androidStringResource(R.string.tsz_anime_extension_repos)))
                    }
                }
            }
        }
        itemsIndexed(ordered, key = { _, entry -> entry.source.id }) { index, entry ->
            if (index == 0 && pinnedCount > 0) {
                TsuzukiSectionLabel(
                    "${stringResource(MR.strings.pinned_sources)} · ${stringResource(MR.strings.tsz_anime_media)}",
                )
            } else if (index == pinnedCount) {
                TsuzukiSectionLabel(stringResource(MR.strings.label_sources))
            }
            val source = entry.source
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(tsuzukiGroupedTile(first = true, last = true))
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .tsuzukiClickable(role = Role.Button) { openAnimeCatalogue(context, source.id, latest = false) }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = tokens.track,
                        contentColor = tokens.dotOn,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val icon = icons[entry.extensionPackage]
                            if (icon != null) {
                                Image(icon, contentDescription = null, modifier = Modifier.matchParentSize())
                            } else {
                                Text(
                                    source.name.trim().take(1).uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                        }
                    }
                    Column {
                        Text(
                            text = tsuzukiMenuLabel(source.name),
                            color = tokens.dotOn,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (source.lang.isNotBlank()) {
                            Text(
                                tsuzukiMenuLabel(LocaleHelper.getSourceDisplayName(source.lang, context)),
                                color = tokens.mutedText,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                val id = source.id.toString()
                val pinned = id in pins
                IconButton(onClick = {
                    val current = pinPreference.get()
                    pinPreference.set(if (id in current) current - id else current + id)
                }) {
                    Icon(
                        Icons.Outlined.PushPin,
                        stringResource(if (pinned) MR.strings.action_unpin else MR.strings.action_pin),
                        tint = if (pinned) tokens.accent else tokens.mutedText,
                    )
                }
                if (source.supportsLatest) {
                    TextButton(
                        onClick = { openAnimeCatalogue(context, source.id, latest = true) },
                        colors = ButtonDefaults.textButtonColors(contentColor = tokens.accent),
                    ) {
                        Text(
                            tsuzukiMenuLabel(stringResource(MR.strings.latest)),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

internal const val ANIME_SOURCE_ID = "anime_source_id"
internal const val ANIME_OPEN_LATEST = "anime_open_latest"

internal const val ANIME_SEARCH_QUERY = "anime_search_query"

internal fun openAnimeCatalogue(
    context: Context,
    sourceId: Long,
    latest: Boolean = false,
    query: String? = null,
) {
    if (!TsuzukiAnime.enabled) return
    context.startActivity(
        Intent(context, AnimeCatalogueActivity::class.java)
            .putExtra(ANIME_SOURCE_ID, sourceId)
            .putExtra(ANIME_OPEN_LATEST, latest)
            .putExtra(ANIME_SEARCH_QUERY, query),
    )
}
