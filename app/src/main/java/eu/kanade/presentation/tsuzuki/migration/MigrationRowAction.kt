package eu.kanade.presentation.tsuzuki.migration

import mihon.feature.migration.list.models.MigratingManga

/** Per-row actions of the migration list, in the order the row shows them. */
internal enum class MigrationRowAction { Skip, SearchManually, CopyNow, MigrateNow }

/**
 * Which actions a row offers for its match state. Mirrors the stock row: a search in progress can only be skipped;
 * no match or a match can be searched manually or skipped; only a match can be copied or migrated now.
 */
internal fun migrationRowActions(result: MigratingManga.SearchResult): List<MigrationRowAction> = when (result) {
    MigratingManga.SearchResult.Searching -> listOf(MigrationRowAction.Skip)
    MigratingManga.SearchResult.NotFound -> listOf(MigrationRowAction.Skip, MigrationRowAction.SearchManually)
    is MigratingManga.SearchResult.Success -> listOf(
        MigrationRowAction.Skip,
        MigrationRowAction.SearchManually,
        MigrationRowAction.CopyNow,
        MigrationRowAction.MigrateNow,
    )
}
