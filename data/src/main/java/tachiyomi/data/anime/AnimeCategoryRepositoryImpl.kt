package tachiyomi.data.anime

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import tachiyomi.data.Database
import tachiyomi.data.subscribeToList
import tachiyomi.domain.category.anime.model.AnimeCategory
import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository

class AnimeCategoryRepositoryImpl(private val database: Database) : AnimeCategoryRepository {
    private val queries get() = database.anime_categoriesQueries
    private val links get() = database.animes_categoriesQueries

    override suspend fun awaitAll() = queries.getAll(::AnimeCategory).awaitAsList()

    override fun subscribeAll() = queries.getAll(::AnimeCategory).subscribeToList()

    override suspend fun awaitByAnimeId(animeId: Long) =
        queries.getForAnime(animeId, ::AnimeCategory).awaitAsList()

    override suspend fun insert(name: String, flags: Long): Long = database.transactionWithResult {
        require(name.isNotBlank())
        queries.insert(name, flags)
        queries.lastInsertedId().awaitAsOne()
    }

    override suspend fun update(category: AnimeCategory) {
        require(category.isSystem || category.name.isNotBlank())
        queries.update(category.name, category.flags, category.id)
    }

    override suspend fun delete(id: Long) {
        queries.delete(id)
    }

    override suspend fun reorder(ids: List<Long>) = database.transaction {
        val requested = ids.filterNot { it == 0L }
        val stored = awaitAll().filterNot(AnimeCategory::isSystem).map(AnimeCategory::id)
        require(requested.size == stored.size && requested.toSet() == stored.toSet())
        requested.forEachIndexed { index, id -> queries.setOrder(index.toLong(), id) }
    }

    override suspend fun setForAnime(animeId: Long, categoryIds: Set<Long>) = database.transaction {
        val current = awaitByAnimeId(animeId).mapTo(mutableSetOf(), AnimeCategory::id)
        val requested = categoryIds.ifEmpty { setOf(0L) }
        if (current == requested) return@transaction

        links.clearForAnime(animeId)
        // No explicit links is the default shelf.
        if (requested != setOf(0L)) requested.forEach { links.insert(animeId, it) }
    }
}
