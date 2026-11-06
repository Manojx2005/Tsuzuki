package eu.kanade.tachiyomi.tsuzuki

import android.content.Context
import android.content.res.Configuration
import app.cash.sqldelight.async.coroutines.awaitAsList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.data.Database
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

private const val MIGRATED_KEY = "tsuzuki_legacy_default_shelf_migrated"
private val defaultShelfMigrationMutex = Mutex()
private val appLocales = listOf("en", "de", "es", "fr", "ja", "ko", "pt-BR", "zh-CN")

internal fun normalizedShelfName(name: String): String = name.trim().uppercase()

/** Check the stored names from every supported locale, since the active locale can change after import. */
internal fun defaultShelfNames(context: Context): Set<String> = buildSet {
    add(normalizedShelfName("Default"))
    for (tag in appLocales) {
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
        val localized = context.createConfigurationContext(config)
        add(normalizedShelfName(localized.stringResource(MR.strings.label_default)))
        add(normalizedShelfName(localized.stringResource(MR.strings.default_category)))
    }
}

internal fun legacyDefaultShelfIds(categories: List<Category>, defaultNames: Set<String>): Set<Long> =
    categories.asSequence()
        .filter {
            it.id > Category.UNCATEGORIZED_ID &&
                (it.contentType == Category.CONTENT_TYPE_NOVEL || it.contentType == Category.CONTENT_TYPE_ALL) &&
                normalizedShelfName(it.name) in defaultNames
        }
        .map { it.id }
        .toSet()

internal fun booksToMoveToDefault(
    memberships: List<Pair<Long, Long>>,
    duplicateIds: Set<Long>,
): Set<Long> {
    val alreadyInDefault = memberships.filter { (_, shelfId) -> shelfId == Category.UNCATEGORIZED_ID }
        .mapTo(hashSetOf()) { (bookId) -> bookId }
    return memberships.asSequence()
        .filter { (bookId, shelfId) -> shelfId in duplicateIds && bookId !in alreadyInDefault }
        .map { (bookId) -> bookId }
        .toSet()
}

internal suspend fun migrateDefaultShelfOnce(migrated: Preference<Boolean>, migrate: suspend () -> Unit) {
    defaultShelfMigrationMutex.withLock {
        if (!migrated.get()) {
            migrate()
            migrated.set(true)
        }
    }
}

/** Fold LNReader's imported Default into the real shelf in one transaction, leaving other memberships in place. */
internal suspend fun migrateLegacyDefaultShelf(
    context: Context,
    database: Database = Injekt.get(),
    preferenceStore: PreferenceStore = Injekt.get(),
    libraryPreferences: LibraryPreferences = Injekt.get(),
) {
    migrateDefaultShelfOnce(preferenceStore.getBoolean(MIGRATED_KEY)) {
        val defaultNames = defaultShelfNames(context)
        val removed = database.transactionWithResult {
            val categories = database.categoriesQueries.getCategories { id, name, order, flags, contentType ->
                Category(id, name, order, flags, contentType.toInt())
            }.awaitAsList()
            val duplicateIds = legacyDefaultShelfIds(categories, defaultNames)
            if (duplicateIds.isNotEmpty()) {
                val memberships = database.mangas_categoriesQueries.getAllMangaCategoryPairs { bookId, shelfId ->
                    bookId to shelfId
                }.awaitAsList()
                for (bookId in booksToMoveToDefault(memberships, duplicateIds)) {
                    database.mangas_categoriesQueries.insert(bookId, Category.UNCATEGORIZED_ID)
                }
                // The database has foreign keys enabled; deleting each shelf cascades only its old memberships.
                duplicateIds.forEach { database.categoriesQueries.delete(it) }
            }
            duplicateIds
        }
        if (libraryPreferences.defaultCategory.get().toLong() in removed) {
            libraryPreferences.defaultCategory.set(Category.UNCATEGORIZED_ID.toInt())
        }
    }
}
