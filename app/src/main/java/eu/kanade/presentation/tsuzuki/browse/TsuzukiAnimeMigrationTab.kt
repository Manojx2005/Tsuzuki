package eu.kanade.presentation.tsuzuki.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.RegisteredAnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimeMigrationActivity
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.domain.source.anime.model.StubAnimeSource
import tachiyomi.domain.source.anime.repository.AnimeSourceRepository
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun rememberHasAnimeMigration(): Boolean {
    val hasLibrary by remember { Injekt.get<AnimeLibraryStore>().observeHasFavorites() }.collectAsState(false)
    return hasLibrary
}

@Composable
internal fun rememberAnimeSourceStubs(sources: List<RegisteredAnimeSource>): Map<Long, StubAnimeSource> {
    val stubs by produceState(emptyMap<Long, StubAnimeSource>(), sources) {
        value = Injekt.get<AnimeSourceRepository>().awaitAll().associateBy(StubAnimeSource::id)
    }
    return stubs
}

@Composable
internal fun animeMigrateSourceTab(sources: List<RegisteredAnimeSource>): TabContent {
    val context = LocalContext.current
    val entries by remember { Injekt.get<AnimeLibraryStore>().observeLibrary() }.collectAsState(emptyList())
    val stubs = rememberAnimeSourceStubs(sources)
    val preferences = remember { Injekt.get<SourcePreferences>() }
    val mode by preferences.migrationSortingMode.collectAsState()
    val direction by preferences.migrationSortingDirection.collectAsState()
    val rows = remember(entries, sources, stubs, mode, direction) {
        val installed = sources.associateBy { it.source.id }
        val order = compareBy<Pair<Source, Long>, String>(String.CASE_INSENSITIVE_ORDER) { it.first.name }
            .thenBy { it.first.id }
        val comparator = if (mode ==
            SetMigrateSorting.Mode.TOTAL
        ) {
            compareBy<Pair<Source, Long>> { it.second }.then(order)
        } else {
            order
        }
        entries.groupingBy { it.anime.source }.eachCount().map { (id, count) ->
            val live = installed[id]?.source
            val stub = stubs[id]
            Source(
                id = id,
                lang = live?.lang ?: stub?.lang.orEmpty(),
                name = live?.name ?: stub?.name ?: id.toString(),
                supportsLatest = false,
                isStub = live == null,
            ) to count.toLong()
        }.sortedWith(if (direction == SetMigrateSorting.Direction.DESCENDING) comparator.reversed() else comparator)
    }

    return TabContent(
        titleRes = MR.strings.label_migration,
        content = { padding, _ ->
            TsuzukiMigrateSourceList(
                list = rows,
                contentPadding = padding,
                onClickItem = { context.startActivity(AnimeMigrationActivity.fromSource(context, it.id)) },
                onLongClickItem = { context.copyToClipboard(it.id.toString(), it.id.toString()) },
                sortingMode = mode,
                onToggleSortingMode = {
                    preferences.migrationSortingMode.set(
                        if (mode == SetMigrateSorting.Mode.TOTAL) {
                            SetMigrateSorting.Mode.ALPHABETICAL
                        } else {
                            SetMigrateSorting.Mode.TOTAL
                        },
                    )
                },
                sortingDirection = direction,
                onToggleSortingDirection = {
                    preferences.migrationSortingDirection.set(
                        if (direction == SetMigrateSorting.Direction.ASCENDING) {
                            SetMigrateSorting.Direction.DESCENDING
                        } else {
                            SetMigrateSorting.Direction.ASCENDING
                        },
                    )
                },
            )
        },
    )
}
