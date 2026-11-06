package eu.kanade.presentation.tsuzuki.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.tachiyomi.tsuzuki.normalizedShelfName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun TsuzukiShelfPills(
    categories: List<Category>,
    selected: Int,
    getItemCountForCategory: (Category) -> Int?,
    onClick: (Int) -> Unit,
) {
    val novels = stringResource(MR.strings.tsz_storage_novels)
    val manga = stringResource(MR.strings.tsz_storage_manga)
    val all = stringResource(MR.strings.tsz_library_all)
    val names = categories.map { it.visualName }
    val labels = remember(categories, names, novels, manga, all) {
        distinctShelfLabels(categories, names) { type ->
            when (type) {
                Category.CONTENT_TYPE_NOVEL -> novels
                Category.CONTENT_TYPE_MANGA -> manga
                else -> all
            }
        }
    }
    TsuzukiPillRow(labels, selected, onClick, counts = categories.map(getItemCountForCategory))
}

/** Keep the localized system name and disambiguate user shelves even when they share a content type. */
internal fun distinctShelfLabels(
    categories: List<Category>,
    names: List<String>,
    typeLabel: (Int) -> String,
): List<String> {
    val keys = names.map(::normalizedShelfName)
    val counts = keys.groupingBy { it }.eachCount()
    val used = keys.toMutableSet()
    return categories.mapIndexed { index, category ->
        val name = names[index]
        if (category.isSystemCategory || counts.getValue(keys[index]) == 1) {
            name
        } else {
            val typedName = "$name · ${typeLabel(category.contentType)}"
            var label = typedName
            while (!used.add(normalizedShelfName(label))) {
                label = "$label · #${category.id}"
            }
            label
        }
    }
}

/** Paging only keeps a slice resident. In that mode show shelf totals from the database. */
@Composable
internal fun rememberPaginatedShelfCounts(): Map<Long, Int> {
    val counts by remember {
        val categories = Injekt.get<CategoryRepository>()
        val manga = Injekt.get<MangaRepository>()
        categories.getWithBookCountsAsFlow().map { shelves ->
            shelves.associate { (shelf, count) -> shelf.id to count.toInt() } +
                (Category.UNCATEGORIZED_ID to manga.getFavoriteIdsForCategory(Category.UNCATEGORIZED_ID).size)
        }.flowOn(Dispatchers.IO)
    }.collectAsState(emptyMap())
    return counts
}
