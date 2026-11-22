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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtensionInstaller
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtensionRepo
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtensionRepos
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeRepoIndexParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.ui.res.stringResource as androidStringResource

internal object AnimeExtensionReposScreen : Screen() {
    @Composable
    override fun Content() {
        AnimeExtensionReposContent()
    }

    operator fun invoke(initialRepoUrl: String): Screen = LinkedAnimeExtensionReposScreen(initialRepoUrl)

    @Composable
    private fun AnimeExtensionReposContent(initialRepoUrl: String? = null) {
        if (!TsuzukiAnime.enabled && initialRepoUrl == null) return
        val context = LocalContext.current.applicationContext
        val repository = remember(context) { AnimeExtensionRepos(Injekt.get(), AnimeExtensionInstaller(context)) }
        val repos by remember(repository) { repository.observe() }.collectAsState(emptyList())
        val scope = rememberCoroutineScope()
        val navigator = LocalNavigator.currentOrThrow
        val tokens = TsuzukiTheme.tokens
        var input by rememberSaveable(initialRepoUrl) { mutableStateOf(initialRepoUrl.orEmpty()) }
        var showMissingLinkError by rememberSaveable(initialRepoUrl) {
            mutableStateOf(initialRepoUrl?.isBlank() == true)
        }
        var busy by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf(false) }
        var pendingAdd by rememberSaveable(initialRepoUrl) {
            mutableStateOf(initialRepoUrl?.let(AnimeRepoIndexParser::indexUrl)?.toString())
        }
        var pendingRemoval by remember { mutableStateOf<AnimeExtensionRepo?>(null) }
        val index = AnimeRepoIndexParser.indexUrl(input)
        val invalid = showMissingLinkError || (input.isNotBlank() && index == null)

        fun mutate(operation: suspend () -> Unit) {
            if (busy) return
            busy = true
            error = false
            scope.launch {
                try {
                    operation()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    error = true
                } finally {
                    busy = false
                }
            }
        }

        fun add(url: String) = mutate {
            repository.add(url)
            input = ""
        }

        Scaffold(
            modifier = Modifier.tsuzukiBackground(tokens),
            containerColor = Color.Transparent,
            topBar = {
                AppBar(
                    title = androidStringResource(R.string.tsz_anime_extension_repos),
                    navigateUp = navigator::pop,
                )
            },
        ) { padding ->
            LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    TsuzukiTile(Modifier.fillMaxWidth().padding(horizontal = 12.dp), padding = 14.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TsuzukiSectionLabel(stringResource(MR.strings.action_add_repo))
                            OutlinedTextField(
                                value = input,
                                onValueChange = {
                                    input = it
                                    error = false
                                    showMissingLinkError = false
                                },
                                label = { Text(stringResource(MR.strings.action_add_repo)) },
                                placeholder = { Text("https://…/index.min.json") },
                                supportingText = when {
                                    invalid -> {
                                        { Text(androidStringResource(R.string.tsz_anime_repo_invalid_link)) }
                                    }
                                    error -> {
                                        { Text(androidStringResource(R.string.tsz_anime_repo_load_error)) }
                                    }
                                    else -> null
                                },
                                isError = invalid || error,
                                enabled = !busy,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = {
                                    index?.toString()?.let { url ->
                                        if (initialRepoUrl == null) add(url) else pendingAdd = url
                                    }
                                },
                                enabled = index != null && !busy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                val label = if (busy) MR.strings.ext_pending else MR.strings.action_add_repo
                                Text(tsuzukiMenuLabel(stringResource(label)))
                            }
                        }
                    }
                }
                items(repos, key = AnimeExtensionRepo::indexUrl) { repo ->
                    TsuzukiTile(Modifier.fillMaxWidth().padding(horizontal = 12.dp), padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    tsuzukiMenuLabel(repo.name),
                                    color = tokens.dotOn,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    repo.indexUrl,
                                    color = tokens.mutedText,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    tsuzukiMenuLabel(
                                        stringResource(MR.strings.tsz_anime_media) +
                                            " · " + androidStringResource(R.string.tsz_anime_aniyomi_format),
                                    ),
                                    color = tokens.mutedText,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            IconButton(onClick = { pendingRemoval = repo }, enabled = !busy) {
                                Icon(Icons.Outlined.Delete, stringResource(MR.strings.ext_remove))
                            }
                        }
                    }
                }
            }
        }
        pendingAdd?.let { url ->
            AlertDialog(
                onDismissRequest = { pendingAdd = null },
                title = { Text(tsuzukiMenuLabel(stringResource(MR.strings.action_add_repo))) },
                text = { Text(url) },
                confirmButton = {
                    TextButton(onClick = {
                        pendingAdd = null
                        add(url)
                    }) { Text(stringResource(MR.strings.action_add_repo)) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingAdd = null }) { Text(stringResource(MR.strings.action_cancel)) }
                },
            )
        }
        pendingRemoval?.let { repo ->
            AlertDialog(
                onDismissRequest = { pendingRemoval = null },
                title = { Text(stringResource(MR.strings.ext_remove)) },
                text = { Text(repo.name) },
                confirmButton = {
                    TextButton(onClick = {
                        pendingRemoval = null
                        mutate { repository.remove(repo.indexUrl) }
                    }) { Text(stringResource(MR.strings.ext_remove)) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRemoval = null }) { Text(stringResource(MR.strings.action_cancel)) }
                },
            )
        }
    }

    private data class LinkedAnimeExtensionReposScreen(val initialRepoUrl: String) : Screen() {
        @Composable
        override fun Content() {
            AnimeExtensionReposContent(initialRepoUrl)
        }
    }
}
