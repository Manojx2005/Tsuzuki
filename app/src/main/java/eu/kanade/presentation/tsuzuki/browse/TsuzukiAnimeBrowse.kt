package eu.kanade.presentation.tsuzuki.browse

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeSourceManager
import eu.kanade.tachiyomi.tsuzuki.anime.RegisteredAnimeSource
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun rememberAnimeBrowseSources(): State<List<RegisteredAnimeSource>> {
    val appContext = LocalContext.current.applicationContext
    val owner = LocalLifecycleOwner.current
    val manager = remember(appContext) { AnimeSourceManager.create(appContext) }
    return produceState(emptyList(), manager, owner) {
        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
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

internal fun animeSourcesTab(
    sources: List<RegisteredAnimeSource>,
) = TabContent(MR.strings.label_sources) { padding, _ ->
    val context = LocalContext.current
    val tokens = TsuzukiTheme.tokens
    LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(sources, key = { it.source.id }) { entry ->
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
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = tokens.track,
                        contentColor = tokens.dotOn,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(source.name.trim().take(1).uppercase(), style = MaterialTheme.typography.titleSmall)
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
                                tsuzukiMenuLabel(source.lang),
                                color = tokens.mutedText,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                if (source.supportsLatest) {
                    TsuzukiPill(
                        text = stringResource(MR.strings.latest),
                        selected = false,
                        onClick = { openAnimeCatalogue(context, source.id, latest = true) },
                        role = Role.Button,
                    )
                }
            }
        }
    }
}

internal fun animeExtensionsTab(sources: List<RegisteredAnimeSource>) =
    TabContent(MR.strings.label_extensions) { padding, _ ->
        val context = LocalContext.current
        val tokens = TsuzukiTheme.tokens
        val installed = sources.distinctBy { it.extensionPackage }
        LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item(key = "anime-extension-setup") {
                Box(Modifier.padding(horizontal = 12.dp)) {
                    TsuzukiPill(
                        text = stringResource(MR.strings.tsz_anime_debug_title),
                        selected = false,
                        onClick = {
                            context.startActivity(
                                Intent().setClassName(
                                    context,
                                    "eu.kanade.tachiyomi.tsuzuki.anime.debug.AnimeDebugActivity",
                                ),
                            )
                        },
                        role = Role.Button,
                    )
                }
            }
            items(installed, key = { it.extensionPackage }) { entry ->
                Text(
                    text = tsuzukiMenuLabel(entry.extensionName),
                    color = tokens.dotOn,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(tsuzukiGroupedTile(first = true, last = true))
                        .padding(12.dp),
                )
            }
        }
    }

internal const val ANIME_SOURCE_ID = "anime_source_id"
internal const val ANIME_OPEN_LATEST = "anime_open_latest"

private fun openAnimeCatalogue(context: Context, sourceId: Long, latest: Boolean) {
    context.startActivity(
        Intent().setClassName(context, "eu.kanade.tachiyomi.tsuzuki.anime.debug.AnimeCatalogueActivity")
            .putExtra(ANIME_SOURCE_ID, sourceId)
            .putExtra(ANIME_OPEN_LATEST, latest),
    )
}
