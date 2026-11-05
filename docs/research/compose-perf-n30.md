# N30 Tsuzuki lazy list audit

All lists already had keys on single-purpose items. Changes (behaviour and visuals unchanged):

| File | Fix |
|---|---|
| updates/TsuzukiUpdatesTwoPane.kt | `state.getUiModel()` (rebuilt each recomposition) moved into `remember(state.items, state.groupByNovel)`; header key `hashCode()` -> `header-${date}`; `contentType` header/update/novel_group |
| track/TsuzukiTrackerSearch.kt | key `hashCode()` of a mutable object -> `tracker_id-remote_id`; contentType |
| library/TsuzukiMixedLibraryGrid.kt | added `key = manga.id` |
| upcoming/TsuzukiUpcomingScreen.kt | key on spacer item; contentType on book rows |
| novel/TsuzukiDuplicateFinder.kt | action pills keyed by label; contentType on pills and groups |
| novel/TsuzukiReaderContents.kt, details/TsuzukiChapterSearch.kt, migration/TsuzukiMigrationList.kt, details/TsuzukiDuplicateMangaDialog.kt, details/TsuzukiNotebook.kt, reader/TsuzukiReaderPanel.kt, library/TsuzukiLibraryQueries.kt (x2), manga/TsuzukiMangaDetails.kt, browse/TsuzukiTabletBrowse.kt, browse/TsuzukiCatalogueFilters.kt, theme/tsuzuki/TsuzukiPill.kt | contentType added to `items` (these mix with header/summary items or share a LazyList with other row kinds) |

Not changed: cover requests go through upstream `MangaCover` (Coil sizes to the layout constraints), no Tsuzuki-side change needed. Catalogue filter items and tablet browse previews have no stable ids, so they keep positional keys.
