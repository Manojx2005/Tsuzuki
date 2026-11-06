package eu.kanade.tachiyomi.tsuzuki

import eu.kanade.presentation.tsuzuki.library.distinctShelfLabels
import eu.kanade.tachiyomi.data.backup.restore.LNReaderBackupImporter.LNCategory
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.category.model.Category

class DefaultShelfMigrationTest {

    private val defaultNames = setOf("DEFAULT", "既定")

    @Test
    fun `in-memory migration moves memberships once and leaves other shelves in place`() = runTest {
        val categories = mutableListOf(
            shelf(0, "", Category.CONTENT_TYPE_ALL),
            shelf(1, " default ", Category.CONTENT_TYPE_NOVEL),
            shelf(2, "既定", Category.CONTENT_TYPE_ALL),
            shelf(3, "Default", Category.CONTENT_TYPE_MANGA),
            shelf(4, "Local", Category.CONTENT_TYPE_NOVEL),
        )
        val memberships = mutableListOf(10L to 1L, 10L to 4L, 20L to 2L, 20L to 0L, 30L to 3L, 40L to 4L)
        val migrated = InMemoryPreferenceStore().getBoolean("migrated")
        var runs = 0
        repeat(2) {
            migrateDefaultShelfOnce(migrated) {
                runs++
                val duplicateIds = legacyDefaultShelfIds(categories, defaultNames)
                memberships += booksToMoveToDefault(memberships, duplicateIds).map { it to Category.UNCATEGORIZED_ID }
                categories.removeAll { it.id in duplicateIds }
                memberships.removeAll { (_, shelfId) -> shelfId in duplicateIds }
            }
        }

        assertEquals(1, runs)
        assertTrue(migrated.get())
        assertEquals(listOf(0L, 3L, 4L), categories.map { it.id })
        assertEquals(setOf(10L to 0L, 10L to 4L, 20L to 0L, 30L to 3L, 40L to 4L), memberships.toSet())
        assertEquals(5, memberships.size)
    }

    @Test
    fun `the one-time flag is written after a successful transaction`() = runTest {
        val migrated = InMemoryPreferenceStore().getBoolean("migrated")
        val failure = runCatching {
            migrateDefaultShelfOnce(migrated) { error("transaction failed") }
        }
        assertTrue(failure.isFailure)
        assertFalse(migrated.get())

        migrateDefaultShelfOnce(migrated) {}
        assertTrue(migrated.get())
    }

    @Test
    fun `LNReader Default by id or localized name resolves to one system shelf`() {
        val shelves = mapLNReaderShelves(
            listOf(
                LNCategory(id = 1, name = "Renamed", sort = 0, novelIds = listOf(10)),
                LNCategory(id = 8, name = "既定", sort = 1, novelIds = listOf(10, 20)),
                LNCategory(id = 9, name = "Local", sort = 1, novelIds = listOf(20, 30)),
            ),
            defaultNames,
        )
        assertEquals(listOf("", "Local"), shelves.categories.map { it.name })
        assertEquals(
            listOf(Category.CONTENT_TYPE_ALL, Category.CONTENT_TYPE_NOVEL),
            shelves.categories.map {
                it.contentType
            },
        )
        assertEquals(mapOf(10 to listOf(0L), 20 to listOf(0L, 1L), 30 to listOf(1L)), shelves.ordersByNovelId)
    }

    @Test
    fun `pills preserve the system label and disambiguate same-type and generated collisions`() {
        val categories = listOf(
            shelf(0, "", Category.CONTENT_TYPE_ALL),
            shelf(1, "Default", Category.CONTENT_TYPE_NOVEL),
            shelf(2, "DEFAULT", Category.CONTENT_TYPE_NOVEL),
            shelf(3, "Default · Novels", Category.CONTENT_TYPE_MANGA),
        )
        val labels =
            distinctShelfLabels(categories, listOf("Default", "Default", "DEFAULT", "Default · Novels")) { "Novels" }

        assertEquals("Default", labels.first())
        assertEquals("Default · Novels", labels.last())
        assertEquals(labels.size, labels.map(::normalizedShelfName).toSet().size)
    }

    private fun shelf(id: Long, name: String, type: Int) = Category(id, name, id, 0, type)
}
