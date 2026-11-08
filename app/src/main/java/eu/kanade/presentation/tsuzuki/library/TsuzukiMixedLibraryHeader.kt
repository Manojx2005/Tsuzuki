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
    manhwaCount: Int,
    showCounts: Boolean,
    manhwaOnly: Boolean,
    onManhwaOnlyChanged: (Boolean) -> Unit,
    onFilterChanged: () -> Unit,
) {
    val preferences = remember { Injekt.get<LibraryPreferences>() }
    val filter by preferences.filterNovel().collectAsState()
    val filters = listOf(TriState.DISABLED, TriState.ENABLED_IS, TriState.ENABLED_NOT)
    val hideMangaUi by remember { Injekt.get<BasePreferences>().hideMangaUi }.collectAsState()
    val tokens = TsuzukiTheme.tokens
    Column {
        if (showCounts && novelCount + mangaCount + manhwaCount > 0) {
            val counts = listOfNotNull(
                stringResource(MR.strings.tsz_library_novel_count, novelCount).takeIf { novelCount > 0 },
                stringResource(MR.strings.tsz_library_manga_count, mangaCount).takeIf { mangaCount > 0 },
                stringResource(MR.strings.tsz_library_manhwa_count, manhwaCount).takeIf { manhwaCount > 0 },
            ).joinToString(" · ")
            Text(
                text = if (tokens.isDot) counts.uppercase() else counts,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!hideMangaUi) {
            TsuzukiPillRow(
                labels = listOf(
                    stringResource(MR.strings.tsz_library_all),
                    stringResource(TDMR.strings.label_novels),
                    stringResource(TDMR.strings.label_manga),
                    stringResource(MR.strings.tsz_manhwa),
                ),
                selected = if (manhwaOnly && filter == TriState.ENABLED_NOT) filters.size else filters.indexOf(filter),
                onClick = {
                    onFilterChanged()
                    onManhwaOnlyChanged(it == filters.size)
                    preferences.filterNovel().set(filters.getOrElse(it) { TriState.ENABLED_NOT })
                },
            )
        }
        TsuzukiLibraryQueryPills(onApplied = onFilterChanged)
    }
}
