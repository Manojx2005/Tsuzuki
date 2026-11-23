package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiAdaptiveSheet
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.tsuzuki.anime.openAnime
import eu.kanade.presentation.tsuzuki.browse.AnimeGlobalSearch
import eu.kanade.presentation.tsuzuki.browse.AnimeGlobalSearchResults
import eu.kanade.presentation.tsuzuki.browse.rememberAnimeBrowseSources
import eu.kanade.presentation.tsuzuki.browse.rememberAnimeSourceStubs
import eu.kanade.presentation.tsuzuki.migration.TsuzukiMigrationPreviewTile
import eu.kanade.presentation.tsuzuki.migration.TsuzukiMigrationPrimaryAction
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeMigrationMode
import eu.kanade.tachiyomi.tsuzuki.anime.RegisteredAnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.matchAnimeEpisodes
import eu.kanade.tachiyomi.tsuzuki.anime.toSourceAnime
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.ui.res.stringResource as androidStringResource

class AnimeMigrationActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!TsuzukiAnime.enabled) {
            finish()
            return
        }
        setContent { TachiyomiTheme { Content() } }
    }

    @Composable
    private fun Content() {
        val library = remember { Injekt.get<AnimeLibraryStore>() }
        val favorites by remember { library.observeLibrary() }.collectAsState(null)
        val sources by rememberAnimeBrowseSources()
        val stubs = rememberAnimeSourceStubs(sources)
        val sourceId = intent.getLongExtra(SOURCE, -1)
        val selectedIds = remember { intent.getLongArrayExtra(SELECTION)?.toSet().orEmpty() }
        val hasList = sourceId != -1L || selectedIds.size > 1
        var selected by rememberSaveable { mutableLongStateOf(intent.getLongExtra(ANIME, -1)) }
        var originalSnapshot by remember(selected) { mutableStateOf<Anime?>(null) }
        val original = originalSnapshot ?: favorites?.firstOrNull { it.anime.id == selected }?.anime
        // Replace invalidates the library flow before the activity can open the destination.
        LaunchedEffect(original) { originalSnapshot = original }
        val enabled = TsuzukiAnime.enabled
        LaunchedEffect(enabled) { if (!enabled) finish() }
        val back: () -> Unit = { if (hasList && selected != -1L) selected = -1 else finish() }
        BackHandler(enabled = selected != -1L && hasList, onBack = back)

        if (original != null) {
            key(original.id) {
                val from = sources.firstOrNull { it.source.id == original.source }
                MigrationSearch(
                    library = library,
                    original = original,
                    from = from,
                    fromName = from?.source?.name ?: stubs[original.source]?.name ?: original.source.toString(),
                    targets = sources.filterNot { it.source.id == original.source },
                    onBack = back,
                )
            }
        } else {
            val tokens = TsuzukiTheme.tokens
            val source = sources.firstOrNull { it.source.id == sourceId }?.source
            Scaffold(
                modifier = Modifier.tsuzukiBackground(tokens),
                containerColor = Color.Transparent,
                topBar = {
                    AppBar(
                        title = source?.name ?: stubs[sourceId]?.name ?: stringResource(MR.strings.label_migration),
                        navigateUp = back,
                    )
                },
            ) { padding ->
                LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (favorites == null) {
                        item { AnimeCatalogueStatus(null) }
                    } else {
                        val titles = favorites.orEmpty().map { it.anime }.filter {
                            it.id in selectedIds || it.source == sourceId
                        }
                        if (titles.isEmpty()) item { AnimeCatalogueStatus(stringResource(MR.strings.no_results_found)) }
                        items(titles, key = Anime::id) { anime ->
                            TsuzukiTile(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                    .tsuzukiClickable(role = Role.Button) { selected = anime.id },
                                padding = 10.dp,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val titleSource =
                                        source ?: sources.firstOrNull { it.source.id == anime.source }?.source
                                    AnimeCover(titleSource, anime.toSourceAnime(), Modifier.width(44.dp))
                                    Text(
                                        tsuzukiMenuLabel(anime.title),
                                        color = TsuzukiTheme.tokens.dotOn,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MigrationSearch(
        library: AnimeLibraryStore,
        original: Anime,
        from: RegisteredAnimeSource?,
        fromName: String,
        targets: List<RegisteredAnimeSource>,
        onBack: () -> Unit,
    ) {
        val model = remember { AnimeGlobalSearch().apply { submit(original.title) } }
        var query by rememberSaveable { mutableStateOf<String?>(null) }
        var candidate by remember { mutableStateOf<MigrationCandidate?>(null) }
        var preparing by remember { mutableStateOf(false) }
        var applying by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val snackbars = remember { SnackbarHostState() }
        val error = stringResource(MR.strings.unknown_error)
        val tokens = TsuzukiTheme.tokens
        val targetId = intent.getLongExtra(TARGET, -1)
        var suggestedTargetOpened by remember { mutableStateOf(targetId == -1L) }

        fun prepare(entry: RegisteredAnimeSource, result: SAnime) {
            if (preparing) return
            preparing = true
            scope.launch {
                try {
                    candidate = withContext(Dispatchers.IO) {
                        val target = checkNotNull(library.refresh(entry, result, fetchDetails = true))
                        val oldEpisodes = library.getEpisodes(original.id)
                        val newEpisodes = library.getEpisodes(target.id)
                        MigrationCandidate(entry, target, oldEpisodes, newEpisodes)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logcat(LogPriority.WARN, e) { "Could not prepare anime migration" }
                    snackbars.showSnackbar(error)
                } finally {
                    preparing = false
                }
            }
        }

        LaunchedEffect(targets) {
            if (!suggestedTargetOpened && targets.isNotEmpty()) {
                suggestedTargetOpened = true
                val target = withContext(Dispatchers.IO) { library.getAnime(targetId) }
                val entry = targets.firstOrNull { it.source.id == target?.source }
                if (entry != null && target != null) prepare(entry, target.toSourceAnime())
            }
        }
        LaunchedEffect(model.revision, targets) { model.search(targets, model.query) }
        BackHandler(enabled = applying) {}
        Scaffold(
            modifier = Modifier.tsuzukiBackground(tokens),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbars) },
            topBar = {
                SearchToolbar(
                    searchQuery = query,
                    onChangeSearchQuery = { query = it },
                    titleContent = { AppBarTitle(original.title) },
                    onSearch = model::submit,
                    navigateUp = { if (!applying) onBack() },
                )
            },
        ) { padding ->
            if (preparing || applying) {
                Column(Modifier.padding(padding)) { AnimeCatalogueStatus(null) }
            } else {
                AnimeGlobalSearchResults(model, padding, onSelect = ::prepare)
            }
        }
        candidate?.let { match ->
            if (!applying) {
                MigrationComparison(original, from, fromName, match, onDismiss = { candidate = null }) { mode ->
                    applying = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                library.migrateFavorite(original.id, match.target.id, mode, Injekt.get())
                            }
                            setResult(Activity.RESULT_OK)
                            openAnime(this@AnimeMigrationActivity, match.target.id)
                            finish()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logcat(LogPriority.WARN, e) { "Could not migrate anime" }
                            candidate = null
                            snackbars.showSnackbar(error)
                        } finally {
                            applying = false
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val ANIME = "migration_anime_id"
        private const val SOURCE = "migration_source_id"
        private const val TARGET = "migration_target_id"
        private const val SELECTION = "migration_selection"

        internal fun fromAnime(context: Context, id: Long, targetId: Long? = null) =
            Intent(context, AnimeMigrationActivity::class.java).putExtra(ANIME, id).apply {
                if (targetId != null) putExtra(TARGET, targetId)
            }

        internal fun fromSelection(context: Context, ids: Set<Long>) = if (ids.size == 1) {
            fromAnime(context, ids.single())
        } else {
            Intent(context, AnimeMigrationActivity::class.java).putExtra(SELECTION, ids.toLongArray())
        }

        internal fun fromSource(context: Context, id: Long) =
            Intent(context, AnimeMigrationActivity::class.java).putExtra(SOURCE, id)
    }
}

private data class MigrationCandidate(
    val entry: RegisteredAnimeSource,
    val target: Anime,
    val oldEpisodes: List<Episode>,
    val newEpisodes: List<Episode>,
) {
    val matchedCount = matchAnimeEpisodes(oldEpisodes, newEpisodes)
        .distinctBy { it.first.episodeNumber.toFloat() }.size
}

@Composable
private fun MigrationComparison(
    original: Anime,
    from: RegisteredAnimeSource?,
    fromName: String,
    match: MigrationCandidate,
    onDismiss: () -> Unit,
    onConfirm: (AnimeMigrationMode) -> Unit,
) {
    TsuzukiAdaptiveSheet(onDismiss) { close ->
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                tsuzukiMenuLabel(stringResource(MR.strings.label_migration)),
                color = TsuzukiTheme.tokens.dotOn,
                style = MaterialTheme.typography.displaySmall,
            )
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TsuzukiMigrationPreviewTile(
                        label = stringResource(MR.strings.tsz_migration_old, fromName),
                        title = original.title,
                        subtitle = stringResource(
                            MR.strings.tsz_anime_episode_progress,
                            match.oldEpisodes.count(Episode::seen),
                            match.oldEpisodes.size,
                        ),
                        highlighted = false,
                        modifier = Modifier.weight(1f),
                        cover = { AnimeCover(from?.source, original.toSourceAnime(), Modifier.fillMaxWidth()) },
                    )
                    TsuzukiMigrationPreviewTile(
                        label = stringResource(MR.strings.tsz_migration_new, match.entry.source.name),
                        title = match.target.title,
                        subtitle = stringResource(
                            MR.strings.tsz_anime_episode_progress,
                            match.newEpisodes.count(Episode::seen),
                            match.newEpisodes.size,
                        ),
                        highlighted = true,
                        modifier = Modifier.weight(1f),
                        cover = {
                            AnimeCover(match.entry.source, match.target.toSourceAnime(), Modifier.fillMaxWidth())
                        },
                    )
                }
                TsuzukiTile(Modifier.fillMaxWidth(), padding = 12.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            tsuzukiMenuLabel(
                                androidStringResource(R.string.tsz_anime_migration_matches, match.matchedCount),
                            ),
                            color = TsuzukiTheme.tokens.dotOn,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            androidStringResource(R.string.tsz_anime_migration_carry),
                            color = TsuzukiTheme.tokens.mutedText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { close { onConfirm(AnimeMigrationMode.COPY) } }) {
                    Text(tsuzukiMenuLabel(stringResource(MR.strings.copy)))
                }
                TsuzukiMigrationPrimaryAction(
                    text = androidStringResource(R.string.tsz_anime_migration_replace),
                    onClick = { close { onConfirm(AnimeMigrationMode.REPLACE) } },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
