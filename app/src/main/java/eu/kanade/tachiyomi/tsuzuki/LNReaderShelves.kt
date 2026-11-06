package eu.kanade.tachiyomi.tsuzuki

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.restore.LNReaderBackupImporter.LNCategory
import tachiyomi.domain.category.model.Category

internal data class LNReaderShelves(
    val categories: List<BackupCategory>,
    val ordersByNovelId: Map<Int, List<Long>>,
)

private const val LNREADER_DEFAULT_ID = 1

/** Use the system category's persisted empty name and all-content type to resolve it during restore. */
internal fun mapLNReaderShelves(categories: List<LNCategory>, defaultNames: Set<String>): LNReaderShelves {
    val backupCategories = mutableListOf<BackupCategory>()
    val ordersByNovelId = mutableMapOf<Int, MutableSet<Long>>()
    var defaultCategory: BackupCategory? = null
    for (category in categories.sortedBy { it.sort }) {
        val isDefault = category.id == LNREADER_DEFAULT_ID || normalizedShelfName(category.name) in defaultNames
        val backup = defaultCategory.takeIf { isDefault } ?: BackupCategory(
            name = if (isDefault) "" else category.name,
            order = backupCategories.size.toLong(),
            contentType = if (isDefault) Category.CONTENT_TYPE_ALL else Category.CONTENT_TYPE_NOVEL,
        ).also {
            backupCategories.add(it)
            if (isDefault) defaultCategory = it
        }
        for (novelId in category.novelIds) {
            ordersByNovelId.getOrPut(novelId) { linkedSetOf() }.add(backup.order)
        }
    }
    return LNReaderShelves(backupCategories, ordersByNovelId.mapValues { (_, orders) -> orders.toList() })
}
