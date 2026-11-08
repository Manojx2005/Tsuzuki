package eu.kanade.presentation.tsuzuki.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Uses the existing novel filter so searches, categories, selection and DB paging agree. */
@Composable
fun TsuzukiMixedLibraryHeader(
    novelCount: Int,
    mangaCount: Int,
    showCounts: Boolean,
    onFilterChanged: () -> Unit,
    onOpenAnime: (() -> Unit)? = null,
) {
    val preferences = remember { Injekt.get<LibraryPreferences>() }
    val filter by preferences.filterNovel().collectAsState()
    val filters = listOf(TriState.DISABLED, TriState.ENABLED_IS, TriState.ENABLED_NOT)
    val hideMangaUi by remember { Injekt.get<BasePreferences>().hideMangaUi }.collectAsState()
    val tokens = TsuzukiTheme.tokens
    Column {
        if (showCounts && novelCount + mangaCount > 0) {
            val counts = listOfNotNull(
                stringResource(MR.strings.tsz_library_novel_count, novelCount).takeIf { novelCount > 0 },
                stringResource(MR.strings.tsz_library_manga_count, mangaCount).takeIf { mangaCount > 0 },
            ).joinToString(" · ")
            Text(
                text = if (tokens.isDot) counts.uppercase() else counts,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!hideMangaUi || onOpenAnime != null) {
            val labels = buildList {
                add(stringResource(MR.strings.tsz_library_all))
                if (!hideMangaUi) {
                    add(stringResource(TDMR.strings.label_novels))
                    add(stringResource(TDMR.strings.label_manga))
                }
                if (onOpenAnime != null) add(stringResource(MR.strings.tsz_anime_media))
            }
            TsuzukiPillRow(
                labels = labels,
                selected = if (hideMangaUi) 0 else filters.indexOf(filter),
                onClick = {
                    if (onOpenAnime != null && it == labels.lastIndex) {
                        onOpenAnime()
                    } else {
                        onFilterChanged()
                        preferences.filterNovel().set(filters[it])
                    }
                },
            )
        }
        TsuzukiLibraryQueryPills(onApplied = onFilterChanged)
    }
}
