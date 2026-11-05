package eu.kanade.tachiyomi.tsuzuki.anime.library

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.tsuzuki.anime.data.AnimeLibraryStore
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.library.LibraryViewModel
import eu.kanade.tachiyomi.ui.library.NovelsTab
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.data.GetAnimeLibrary
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Small library dispatch shared by the existing Voyager tabs; no new navigator. */
object AnimeLibraryUi {
    private fun selection() = Injekt.get<PreferenceStore>().getBoolean("tsuzuki_library_anime", false)

    @Composable
    fun readerType(joined: Boolean): LibraryViewModel.LibraryType =
        if (joined && TsuzukiTheme.tokens.style == TsuzukiStyle.NONE) {
            LibraryViewModel.LibraryType.All
        } else {
            LibraryViewModel.LibraryType.Novel
        }

    @Composable
    fun contentIfSelected(): Boolean {
        if (TsuzukiTheme.tokens.style == TsuzukiStyle.NONE) return false
        val preference = remember { selection() }
        val selected by preference.changes().collectAsState(preference.get())
        if (selected) Library()
        return selected
    }

    @Composable
    fun mediaSwitch(isManga: Boolean) {
        if (TsuzukiTheme.tokens.style == TsuzukiStyle.NONE) return
        val tokens = TsuzukiTheme.tokens
        val preference = remember { selection() }
        val animeSelected by preference.changes().collectAsState(preference.get())
        val hidePreference = remember { Injekt.get<BasePreferences>().hideMangaUi }
        val hideManga by hidePreference.changes().collectAsState(hidePreference.get())
        val counts by remember { AnimeLibraryStore().mediaCounts() }.collectAsState(initial = null)
        val navigator = LocalTabNavigator.current
        val labels = listOf(
            stringResource(TDMR.strings.label_novels),
            stringResource(TDMR.strings.label_manga),
            stringResource(MR.strings.tsz_anime),
        )
        val values = listOf(counts?.novels, counts?.manga, counts?.anime)
        val selected = if (animeSelected) {
            2
        } else if (isManga) {
            1
        } else {
            0
        }
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(tokens.tile).padding(4.dp),
        ) {
            (if (hideManga) listOf(0, 2) else listOf(0, 1, 2)).forEach { index ->
                val label = labels[index]
                val active = selected == index
                Row(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(28.dp))
                        .then(if (active) Modifier.background(MaterialTheme.colorScheme.onSurface) else Modifier)
                        .selectable(selected = active, role = Role.Tab, onClick = {
                            preference.set(index == 2)
                            if (index != 2) navigator.current = if (index == 1) LibraryTab else NovelsTab
                        }),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = (if (tokens.isDot) label.uppercase() else label) +
                            (values[index]?.let { " $it" } ?: ""),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (active) {
                            MaterialTheme.colorScheme.background
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    @Composable
    private fun Library() {
        val tokens = TsuzukiTheme.tokens
        val entries by remember { AnimeLibraryStore().library() }.collectAsState(initial = emptyList())
        var watchStatus by remember { mutableIntStateOf(0) }
        val statuses = listOf(1L, 0L, 2L)
        val labels = listOf(
            stringResource(MR.strings.tsz_anime_watching),
            stringResource(MR.strings.tsz_anime_plan),
            stringResource(MR.strings.completed),
        )
        Column(Modifier.fillMaxSize().tsuzukiBackground(tokens).statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val title = stringResource(MR.strings.tsz_library)
                Text(
                    if (tokens.isDot) title.uppercase() else title,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text("${entries.size}", style = MaterialTheme.typography.titleMedium)
            }
            mediaSwitch(isManga = false)
            TsuzukiPillRow(
                labels = labels,
                selected = statuses.indexOf(watchStatus.toLong()),
                counts = statuses.map { status -> entries.count { it.watch_status == status } },
                onClick = { watchStatus = statuses[it].toInt() },
            )
            val visible = entries.filter { it.watch_status == watchStatus.toLong() }
            if (visible.isEmpty()) {
                Text(
                    stringResource(MR.strings.tsz_anime_empty),
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(96.dp),
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(visible, key = { it.id }) { entry -> Cover(entry) }
            }
        }
    }

    @Composable
    private fun Cover(entry: GetAnimeLibrary) {
        val context = LocalContext.current
        val tokens = TsuzukiTheme.tokens
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MangaCover.Book(
                data = entry.thumbnail_url,
                modifier = Modifier.fillMaxWidth(),
                contentDescription = entry.title,
                shape = RoundedCornerShape(8.dp),
                onClick = if (BuildConfig.DEBUG) {
                    {
                        context.startActivity(
                            Intent().setClassName(
                                context.packageName,
                                "eu.kanade.tachiyomi.tsuzuki.anime.AnimeSpikeActivity",
                            ).putExtra("anime_id", entry.id),
                        )
                    }
                } else {
                    null
                },
            )
            Text(
                if (tokens.isDot) entry.title.uppercase() else entry.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (entry.episodes_fetched == 1L) {
                    stringResource(MR.strings.tsz_anime_episode_progress, entry.seen_count, entry.episode_count)
                } else {
                    stringResource(MR.strings.tsz_anime_episodes_unknown)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entry.episode_count > 0) {
                TsuzukiProgressBar(entry.seen_count.toFloat() / entry.episode_count, dots = 12)
            }
        }
    }
}
