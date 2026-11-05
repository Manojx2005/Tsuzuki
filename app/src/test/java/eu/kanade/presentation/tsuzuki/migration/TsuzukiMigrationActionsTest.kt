package eu.kanade.presentation.tsuzuki.migration

import io.kotest.matchers.shouldBe
import mihon.feature.migration.list.models.MigratingManga
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga

class TsuzukiMigrationActionsTest {

    @Test
    fun `searching row can only be skipped`() {
        migrationRowActions(MigratingManga.SearchResult.Searching) shouldBe listOf(MigrationRowAction.Skip)
    }

    @Test
    fun `row without a match can skip or search manually`() {
        migrationRowActions(MigratingManga.SearchResult.NotFound) shouldBe
            listOf(MigrationRowAction.Skip, MigrationRowAction.SearchManually)
    }

    @Test
    fun `row with a match offers every action`() {
        val result = MigratingManga.SearchResult.Success(Manga.create(), 3, 2.0, "Source")
        migrationRowActions(result) shouldBe listOf(
            MigrationRowAction.Skip,
            MigrationRowAction.SearchManually,
            MigrationRowAction.CopyNow,
            MigrationRowAction.MigrateNow,
        )
    }
}
