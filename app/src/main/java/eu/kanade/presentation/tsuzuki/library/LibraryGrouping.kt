package eu.kanade.presentation.tsuzuki.library

import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.category.model.Category

internal fun List<LibraryItem>.groupLibraryItems(
    categories: List<Category>,
    showSystemCategory: Boolean,
): Map<Category, List<Long>> {
    val visibleCategories = ArrayList<Category>(categories.size)
    for (category in categories) {
        if (showSystemCategory || !category.isSystemCategory) {
            visibleCategories += category
        }
    }
    if (visibleCategories.isEmpty()) return emptyMap()

    val visibleCategoryById = HashMap<Long, Category>(visibleCategories.size * 2)
    val groupedItems = LinkedHashMap<Category, MutableList<Long>>(visibleCategories.size)
    for (category in visibleCategories) {
        visibleCategoryById[category.id] = category
        groupedItems[category] = mutableListOf()
    }

    for (item in this) {
        for (categoryId in item.libraryManga.categories) {
            val category = visibleCategoryById[categoryId] ?: continue
            groupedItems[category]?.add(item.id)
        }
    }

    return groupedItems
}
