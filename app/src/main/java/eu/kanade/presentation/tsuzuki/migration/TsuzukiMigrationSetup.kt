package eu.kanade.presentation.tsuzuki.migration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private const val BOOK_PREVIEW_LIMIT = 5

@Composable
internal fun TsuzukiMigrationSetupSummary(mangaIds: Collection<Long>, selectedSourceCount: Int) {
    val tokens = TsuzukiTheme.tokens
    val books by produceState<List<Manga>>(emptyList(), mangaIds) {
        value = withContext(Dispatchers.IO) {
            val getManga = Injekt.get<GetManga>()
            mangaIds.take(BOOK_PREVIEW_LIMIT).mapNotNull { getManga.await(it) }
        }
    }
    TsuzukiSectionLabel(stringResource(MR.strings.tsz_x4_books_to_move, mangaIds.size))
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        books.forEach { manga ->
            TsuzukiTile(Modifier.fillMaxWidth(), padding = 10.dp) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MangaCover.Book(data = manga, modifier = Modifier.width(42.dp))
                    Text(
                        tsuzukiMenuLabel(manga.title),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(Icons.Outlined.CheckCircle, null, Modifier.size(22.dp), tint = tokens.accent)
                }
            }
        }
        if (books.size == BOOK_PREVIEW_LIMIT && mangaIds.size > books.size) {
            Text(
                stringResource(MR.strings.tsz_x4_more_books, mangaIds.size - books.size),
                Modifier.padding(horizontal = 2.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    TsuzukiSectionLabel(stringResource(MR.strings.tsz_x4_search_sources, selectedSourceCount))
}

@Composable
internal fun TsuzukiMigrationPrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = TsuzukiTheme.tokens
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = tokens.accent, contentColor = tokens.onAccent),
    ) {
        Text(tsuzukiMenuLabel(text), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun TsuzukiMigrationSetupFooter(bookCount: Int, onContinue: () -> Unit) {
    TsuzukiMigrationPrimaryAction(
        stringResource(MR.strings.tsz_x4_find_matches, bookCount),
        onContinue,
        Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
internal fun TsuzukiMigrationToggle(title: String, subtitle: String?, checked: Boolean, onClick: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    TsuzukiTile(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)
            .toggleable(checked, role = Role.Switch) { onClick() },
        padding = 12.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tsuzukiMenuLabel(title), style = MaterialTheme.typography.bodyLarge)
                if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(checkedTrackColor = tokens.accent, checkedThumbColor = tokens.onAccent),
            )
        }
    }
}
