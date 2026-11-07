package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.SourceUiModel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.ui.browse.source.SourcesViewModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.isLocal

/** Put the real local catalogue alongside pinned manga without changing the stored pin state. */
internal fun mangaSourceTiles(items: List<SourceUiModel>): List<SourceUiModel> {
    val local = items.filterIsInstance<SourceUiModel.Item>().firstOrNull {
        it.sectionKey.isEmpty() && it.source.isLocal()
    } ?: return items

    val withoutLocal = items.filterNot { it === local }
    val filtered = withoutLocal.filterIndexed { index, item ->
        item !is SourceUiModel.Header || withoutLocal.getOrNull(index + 1) is SourceUiModel.Item
    }.toMutableList()

    val pinnedHeader = filtered.indexOfFirst {
        it is SourceUiModel.Header && !it.isGroup && it.language == SourcesViewModel.PINNED_KEY
    }
    if (pinnedHeader >= 0) {
        filtered.add(pinnedHeader + 1, local)
    } else {
        val afterLastUsed = filtered.indexOfFirst {
            it is SourceUiModel.Header && !it.isGroup && it.language == SourcesViewModel.LAST_USED_KEY
        }.takeIf { it >= 0 }?.let { lastUsed ->
            (lastUsed + 1 until filtered.size).firstOrNull { filtered[it] is SourceUiModel.Header } ?: filtered.size
        } ?: 0
        filtered.add(afterLastUsed, SourceUiModel.Header(SourcesViewModel.PINNED_KEY))
        filtered.add(afterLastUsed + 1, local)
    }
    return filtered
}

@Composable
internal fun TsuzukiMangaRepoTile(updates: Int, onOpenRepos: () -> Unit, onOpenExtensions: () -> Unit) {
    TsuzukiTile(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                tsuzukiMenuLabel(stringResource(MR.strings.tsz_manga_repos)),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                tsuzukiMenuLabel(stringResource(MR.strings.tsz_manga_repos_summary)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpenRepos) {
                    Text(tsuzukiMenuLabel(stringResource(MR.strings.label_extension_repos)))
                }
                TextButton(onClick = onOpenExtensions) {
                    val extensions = stringResource(MR.strings.label_extensions)
                    Text(tsuzukiMenuLabel(if (updates > 0) "$extensions ($updates)" else extensions))
                }
            }
        }
    }
}
