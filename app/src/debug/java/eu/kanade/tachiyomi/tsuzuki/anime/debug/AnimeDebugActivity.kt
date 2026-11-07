package eu.kanade.tachiyomi.tsuzuki.anime.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtension
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeLoadResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

class AnimeDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = AnimeDebugRepository(applicationContext)
        setContent {
            TachiyomiTheme(appTheme = AppTheme.DOT) {
                val scope = rememberCoroutineScope()
                val flow = remember { AnimeDebugFlow(repository, scope) }
                LaunchedEffect(flow) { flow.open() }
                AnimeDebugScreen(flow, ::finish) { video -> AnimeDebugPlayback.launch(this, video) }
            }
        }
    }
}

private enum class AnimeDebugStep {
    Repo,
    Extension,
    Source,
    Episode,
    Play,
}

private data class DebugSource(val source: AnimeSource, val libVersion: Double)

private class AnimeDebugFlow(private val repository: AnimeDebugRepository, private val scope: CoroutineScope) {
    var input by mutableStateOf("")
    var step by mutableStateOf(AnimeDebugStep.Repo)
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var invalidRepo by mutableStateOf(false)
        private set
    var savedRepos by mutableStateOf(emptyList<String>())
        private set
    var index by mutableStateOf<HttpUrl?>(null)
        private set
    var entries by mutableStateOf(emptyList<AnimeRepoEntry>())
        private set
    var installed by mutableStateOf(emptyMap<String, AnimeLoadResult>())
        private set
    var pendingTrust by mutableStateOf<AnimeExtension.Untrusted?>(null)
    var catalogue by mutableStateOf<List<SAnime>?>(null)
        private set
    var episodes by mutableStateOf<List<SEpisode>?>(null)
        private set
    var videos by mutableStateOf<List<Video>?>(null)
        private set
    private var source: DebugSource? = null

    val sources: List<DebugSource>
        get() = installed.values.filterIsInstance<AnimeLoadResult.Success>().flatMap { result ->
            result.extension.sources.map { DebugSource(it, result.extension.libVersion) }
        }

    fun open() = run(
        work = { repository.savedRepos() to repository.installed() },
        success = { (repos, extensions) ->
            savedRepos = repos
            installed = extensions
        },
    )

    fun canOpen(step: AnimeDebugStep): Boolean = when (step) {
        AnimeDebugStep.Repo -> true
        AnimeDebugStep.Extension -> index != null
        AnimeDebugStep.Source -> installed.values.any { it != AnimeLoadResult.Error }
        AnimeDebugStep.Episode -> catalogue != null
        AnimeDebugStep.Play -> videos != null
    }

    fun load(raw: String) {
        val url = AnimeRepoIndexParser.indexUrl(raw)
        invalidRepo = url == null
        if (url == null) return
        input = raw
        run(
            work = { repository.loadIndex(url) to repository.savedRepos() },
            success = { (items, repos) ->
                index = url
                entries = items
                savedRepos = repos
                step = AnimeDebugStep.Extension
            },
        )
    }

    fun install(entry: AnimeRepoEntry) {
        val currentIndex = index ?: return
        run(
            work = { repository.install(currentIndex, entry) },
            success = { updateExtension(entry.packageName, it) },
        )
    }

    fun trust(extension: AnimeExtension.Untrusted) {
        pendingTrust = null
        run(
            work = { repository.trust(extension) },
            success = { updateExtension(extension.pkgName, it) },
        )
    }

    private fun updateExtension(pkg: String, result: AnimeLoadResult) {
        installed = installed + (pkg to result)
        if (result == AnimeLoadResult.Error) error = "Extension loader rejected the package"
        if (result is AnimeLoadResult.Success) step = AnimeDebugStep.Source
    }

    fun popular(selected: DebugSource) = run(
        work = { selected.source.getPopularAnime(1).animes },
        success = {
            source = selected
            catalogue = it
            episodes = null
            videos = null
            step = AnimeDebugStep.Episode
        },
    )

    fun openAnime(anime: SAnime) {
        val selected = source ?: return
        run(
            work = {
                repository.episodes(selected.source, selected.libVersion, anime)
                    .sortedBy { it.episode_number.takeIf { number -> number >= 0 } ?: Float.POSITIVE_INFINITY }
            },
            success = {
                episodes = it
                videos = null
            },
        )
    }

    fun openFirstEpisode() {
        val episode = episodes?.firstOrNull() ?: return
        val selected = source ?: return
        run(
            work = { repository.videos(selected.source, selected.libVersion, episode) },
            success = {
                videos = it
                step = AnimeDebugStep.Play
            },
        )
    }

    fun play(video: Video, launch: (Video) -> Unit) {
        val selected = source ?: return
        run(
            work = { repository.resolve(selected.source, selected.libVersion, video) },
            success = launch,
        )
    }

    private fun <T> run(work: suspend () -> T, success: (T) -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                success(withContext(Dispatchers.IO) { work() })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message?.replace(Regex("https?://[^ ]+"), "[URL]")?.take(300)
                    ?: e.javaClass.simpleName
            } catch (e: LinkageError) {
                error = e.javaClass.simpleName
            } finally {
                busy = false
            }
        }
    }
}

@Composable
private fun AnimeDebugScreen(flow: AnimeDebugFlow, close: () -> Unit, play: (Video) -> Unit) {
    val labels = listOf(
        stringResource(MR.strings.label_extension_repos),
        stringResource(MR.strings.label_extensions),
        stringResource(MR.strings.label_sources),
        stringResource(MR.strings.tsz_anime_debug_episodes),
        stringResource(MR.strings.tsz_widget_listening_play),
    )
    val empty = stringResource(MR.strings.no_results_found)
    val tokens = TsuzukiTheme.tokens
    Scaffold(
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = close) { Text("â€¹", style = MaterialTheme.typography.titleLarge) }
                Text(
                    stringResource(MR.strings.tsz_anime_debug_title).uppercase(),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text("DEBUG", color = tokens.accent, style = MaterialTheme.typography.labelMedium)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AnimeDebugStep.entries.forEach { step ->
                        val label = "${step.ordinal + 1} ${labels[step.ordinal]}"
                        if (flow.canOpen(step)) {
                            TsuzukiPill(
                                text = label,
                                selected = flow.step == step,
                                onClick = { if (!flow.busy) flow.step = step },
                            )
                        } else {
                            Text(
                                label.uppercase(),
                                Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
            if (flow.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            flow.error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            when (flow.step) {
                AnimeDebugStep.Repo -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = flow.input,
                                onValueChange = { flow.input = it },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !flow.busy,
                                singleLine = true,
                                isError = flow.invalidRepo,
                                label = { Text(stringResource(MR.strings.tsz_anime_debug_repo_url)) },
                                supportingText = if (flow.invalidRepo) {
                                    { Text(stringResource(MR.strings.invalid_repo_name)) }
                                } else {
                                    null
                                },
                            )
                            Button(onClick = { flow.load(flow.input) }, enabled = !flow.busy) {
                                Text(stringResource(MR.strings.tsz_anime_debug_load).uppercase())
                            }
                        }
                    }
                    items(flow.savedRepos) { repo ->
                        DebugRow(repo, action = stringResource(MR.strings.tsz_anime_debug_load), enabled = !flow.busy) {
                            flow.load(repo)
                        }
                    }
                }
                AnimeDebugStep.Extension -> {
                    if (flow.entries.isEmpty()) item { Text(empty) }
                    items(flow.entries, key = { it.packageName }) { entry ->
                        val state = flow.installed[entry.packageName]
                        val action = when (animeDebugInstallAction(state)) {
                            AnimeDebugInstallAction.Install -> stringResource(MR.strings.ext_install)
                            AnimeDebugInstallAction.Trust -> stringResource(MR.strings.ext_trust)
                            AnimeDebugInstallAction.Installed -> stringResource(MR.strings.ext_installed)
                            AnimeDebugInstallAction.Retry -> stringResource(MR.strings.action_retry)
                        }
                        DebugRow(
                            title = entry.name,
                            detail = "${entry.lang} Â· ${entry.versionName}",
                            action = action,
                            enabled = !flow.busy && state !is AnimeLoadResult.Success,
                        ) {
                            if (state is AnimeLoadResult.Untrusted) {
                                flow.pendingTrust = state.extension
                            } else {
                                flow.install(entry)
                            }
                        }
                    }
                }
                AnimeDebugStep.Source -> {
                    val untrusted = flow.installed.values.filterIsInstance<AnimeLoadResult.Untrusted>()
                    if (flow.sources.isEmpty() && untrusted.isEmpty()) item { Text(empty) }
                    items(untrusted) { result ->
                        DebugRow(
                            result.extension.name,
                            stringResource(MR.strings.ext_untrusted),
                            stringResource(MR.strings.ext_trust),
                            !flow.busy,
                        ) { flow.pendingTrust = result.extension }
                    }
                    items(flow.sources) { source ->
                        DebugRow(
                            source.source.name,
                            source.source.lang,
                            stringResource(MR.strings.popular),
                            !flow.busy,
                        ) { flow.popular(source) }
                    }
                }
                AnimeDebugStep.Episode -> {
                    val episodes = flow.episodes
                    if (episodes != null) {
                        item {
                            TextButton(
                                onClick = flow::openFirstEpisode,
                                enabled = !flow.busy && episodes.isNotEmpty(),
                            ) {
                                Text(stringResource(MR.strings.tsz_anime_debug_first_episode).uppercase())
                            }
                        }
                        if (episodes.isEmpty()) item { Text(empty) }
                        items(episodes) { Text(it.name, style = MaterialTheme.typography.bodyMedium) }
                    }
                    val catalogue = flow.catalogue.orEmpty()
                    if (catalogue.isEmpty()) item { Text(empty) }
                    items(catalogue) { anime ->
                        DebugRow(
                            anime.title,
                            action = labels[AnimeDebugStep.Episode.ordinal],
                            enabled = !flow.busy,
                        ) { flow.openAnime(anime) }
                    }
                }
                AnimeDebugStep.Play -> {
                    val videos = flow.videos.orEmpty()
                    if (videos.isEmpty()) item { Text(empty) }
                    items(videos) { video ->
                        val playLabel = labels[AnimeDebugStep.Play.ordinal]
                        DebugRow(video.videoTitle.ifBlank { playLabel }, action = playLabel, enabled = !flow.busy) {
                            flow.play(video, play)
                        }
                    }
                }
            }
        }
    }
    flow.pendingTrust?.let { extension ->
        AlertDialog(
            onDismissRequest = { flow.pendingTrust = null },
            title = { Text(stringResource(MR.strings.untrusted_extension)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(MR.strings.untrusted_extension_message))
                    Text(extension.pkgName)
                    Text(extension.versionName)
                    Text(extension.signatureHash)
                }
            },
            confirmButton = {
                TextButton(onClick = { flow.trust(extension) }) { Text(stringResource(MR.strings.ext_trust)) }
            },
            dismissButton = {
                TextButton(onClick = { flow.pendingTrust = null }) {
                    Text(stringResource(MR.strings.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun DebugRow(
    title: String,
    detail: String? = null,
    action: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        TextButton(onClick = onClick, enabled = enabled) { Text(action.uppercase()) }
    }
}
