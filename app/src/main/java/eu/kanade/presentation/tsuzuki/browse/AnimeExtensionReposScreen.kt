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
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.util.LocalBackPress
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
        if (!TsuzukiAnime.enabled) return
        val context = LocalContext.current.applicationContext
        val repository = remember(context) { AnimeExtensionRepos(Injekt.get(), AnimeExtensionInstaller(context)) }
        val repos by remember(repository) { repository.observe() }.collectAsState(emptyList())
        val scope = rememberCoroutineScope()
        val back = LocalBackPress.currentOrThrow
        val tokens = TsuzukiTheme.tokens
        var input by rememberSaveable { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf(false) }
        var pendingRemoval by remember { mutableStateOf<AnimeExtensionRepo?>(null) }
        val invalid = input.isNotBlank() && AnimeRepoIndexParser.indexUrl(input) == null

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

        Scaffold(
            modifier = Modifier.tsuzukiBackground(tokens),
            containerColor = Color.Transparent,
            topBar = {
                AppBar(
                    title = androidStringResource(R.string.tsz_anime_extension_repos),
                    navigateUp = back::invoke,
                )
            },
        ) { padding ->
            LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    TsuzukiTile(Modifier.fillMaxWidth().padding(horizontal = 12.dp), padding = 14.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                tsuzukiMenuLabel(stringResource(MR.strings.action_add_repo)),
                                color = tokens.mutedText,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it },
                                label = { Text(stringResource(MR.strings.action_add_repo)) },
                                placeholder = { Text("https://…/index.min.json") },
                                supportingText = if (invalid) {
                                    { Text(stringResource(MR.strings.invalid_repo_name)) }
                                } else {
                                    null
                                },
                                isError = invalid,
                                enabled = !busy,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = {
                                    mutate {
                                        repository.add(input)
                                        input = ""
                                    }
                                },
                                enabled = input.isNotBlank() && !invalid && !busy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                val label = if (busy) MR.strings.ext_pending else MR.strings.action_add_repo
                                Text(tsuzukiMenuLabel(stringResource(label)))
                            }
                            if (error) {
                                Text(stringResource(MR.strings.unknown_error), color = tokens.dotOn)
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
}
