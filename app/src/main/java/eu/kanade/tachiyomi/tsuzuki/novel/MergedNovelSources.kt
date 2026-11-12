package eu.kanade.tachiyomi.tsuzuki.novel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import logcat.LogPriority
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

@Serializable
internal data class NovelEditionKey(val source: Long, val url: String)

internal val Manga.editionKey get() = NovelEditionKey(source, url)

internal data class NovelEdition(val manga: Manga, val chapters: List<Chapter>)

internal data class MergedNovelChapter(val copies: List<Chapter>) {
    val preferred get() = copies.firstOrNull { !it.locked } ?: copies.first()
    val read get() = copies.any { it.read }
}

private fun Chapter.mergeNumber(): Double? =
    chapterNumber.takeIf { isRecognizedNumber && it.isFinite() }?.let { if (it == 0.0) 0.0 else it }

internal fun mergeNovelChapters(editions: List<NovelEdition>): List<MergedNovelChapter> {
    val numbered = sortedMapOf<Double, MutableList<Chapter>>()
    val unnumbered = mutableListOf<MergedNovelChapter>()
    editions.forEach { edition ->
        edition.chapters.sortedBy(Chapter::sourceOrder).forEach { chapter ->
            val number = chapter.mergeNumber()
            if (number == null) {
                unnumbered += MergedNovelChapter(listOf(chapter))
            } else {
                numbered.getOrPut(number, ::mutableListOf).add(chapter)
            }
        }
    }
    return numbered.values.map(::MergedNovelChapter) + unnumbered
}

internal fun linkedNovelReadUpdates(
    changed: List<Chapter>,
    editions: List<NovelEdition>,
    read: Boolean,
): List<ChapterUpdate> {
    val changedIds = changed.mapTo(hashSetOf(), Chapter::id)
    val changedNumbers = changed.mapNotNull { chapter ->
        chapter.mergeNumber()?.let { it to chapter.mangaId }
    }.groupBy({ it.first }, { it.second })
    if (changedNumbers.isEmpty() || editions.size < 2) return emptyList()

    return editions.flatMap { edition ->
        edition.chapters.mapNotNull { chapter ->
            val hasOtherEdition = changedNumbers[chapter.mergeNumber()]?.any { it != chapter.mangaId } == true
            if (chapter.id in changedIds || !hasOtherEdition ||
                (chapter.read == read && (read || chapter.lastPageRead == 0L))
            ) {
                null
            } else {
                ChapterUpdate(chapter.id, read = read, lastPageRead = if (read) null else 0L)
            }
        }
    }
}

internal fun inheritNovelReadStatus(chapters: List<Chapter>, otherEditions: List<NovelEdition>): List<Chapter> {
    val readNumbers = otherEditions.flatMap(NovelEdition::chapters)
        .filter(Chapter::read).mapNotNullTo(hashSetOf()) { it.mergeNumber() }
    if (readNumbers.isEmpty()) return chapters
    return chapters.map { chapter ->
        if (!chapter.read && chapter.mergeNumber() in readNumbers) chapter.copy(read = true) else chapter
    }
}

internal class MergedNovelSources(
    preferences: PreferenceStore,
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
) {
    private val links = preferences.getString(Preference.appStateKey("tsz_linked_novels"), "")
    private val mutex = Mutex()

    fun observe(book: Manga): Flow<List<NovelEdition>> = links.changes()
        .map { raw -> decode(raw).firstOrNull { book.editionKey in it } ?: listOf(book.editionKey) }
        .distinctUntilChanged()
        .flatMapLatest { keys -> combine(keys.map(::observeEdition)) { editions -> editions.filterNotNull() } }
        .flowOn(Dispatchers.IO)

    private fun observeEdition(key: NovelEditionKey): Flow<NovelEdition?> =
        mangaRepository.getMangaByUrlAndSourceIdAsFlow(key.url, key.source)
            .flatMapLatest { manga ->
                if (manga?.favorite == true && manga.isNovel) {
                    chapterRepository.getChapterByMangaIdAsFlow(manga.id)
                        .map { chapters -> NovelEdition(manga, chapters) }
                } else {
                    flowOf(null)
                }
            }

    suspend fun candidates(book: Manga, members: List<Manga>): List<Manga> = withContext(Dispatchers.IO) {
        val linkedKeys = decode(links.get()).flatten().toSet()
        val sources = members.mapTo(hashSetOf(book.source), Manga::source)
        mangaRepository.getFavorites().filter { manga ->
            manga.isNovel && manga.source !in sources && manga.editionKey !in linkedKeys
        }
    }

    suspend fun replace(book: Manga, previous: List<Manga>, members: List<Manga>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val groups = decode(links.get())
            val old = groups.firstOrNull { book.editionKey in it }.orEmpty()
            val current = old.ifEmpty { listOf(book.editionKey) }.mapNotNull { loadFavorite(it)?.editionKey }
            check(current == previous.map(Manga::editionKey))
            val keys = members.map(Manga::editionKey)
            require(book.editionKey in keys)
            require(keys.distinctBy(NovelEditionKey::source).size == keys.size)
            val otherGroups = groups - setOf(old)
            require(otherGroups.none { group -> group.any { it in keys } })
            val next = otherGroups + listOf(keys).filter { it.size >= 2 }

            val updates = if (keys.size >= 2 && keys.any { it !in old }) {
                val editions = loadEditions(keys)
                check(editions.size == keys.size)
                val read = editions.flatMap(NovelEdition::chapters).filter(Chapter::read)
                linkedNovelReadUpdates(read, editions, read = true)
            } else {
                emptyList()
            }
            withContext(NonCancellable) {
                if (updates.isNotEmpty()) chapterRepository.updateAll(updates)
                links.set(if (next.isEmpty()) "" else Json.encodeToString(next))
            }
        }
    }

    suspend fun insertChapters(book: Manga, chapters: List<Chapter>): List<Chapter> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val group = decode(links.get()).firstOrNull { book.editionKey in it }.orEmpty()
            val prepared = try {
                if (group.isEmpty()) {
                    chapters
                } else {
                    inheritNovelReadStatus(
                        chapters,
                        loadEditions(group - book.editionKey),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logcat(LogPriority.WARN, error) { "Could not read linked editions during chapter refresh" }
                chapters
            }
            chapterRepository.addAll(prepared)
        }
    }

    /** Keep the initiating write and its corresponding read flags in one chapter transaction. */
    suspend fun writeReadStatus(
        changed: List<Chapter>,
        read: Boolean,
        updates: List<ChapterUpdate>,
        write: suspend (List<ChapterUpdate>) -> Unit = chapterRepository::updateAll,
    ) = mutex.withLock {
        val groups = decode(links.get())
        if (groups.isEmpty()) {
            write(updates)
            return@withLock
        }
        val peers = try {
            val books = mangaRepository.getMangasByIds(changed.map(Chapter::mangaId).distinct())
                .filter { it.favorite && it.isNovel }
                .associateBy(Manga::id)
            val byBook = changed.groupBy { books[it.mangaId]?.editionKey }
            groups.flatMap { group ->
                val originals = group.flatMap { byBook[it].orEmpty() }
                if (originals.isEmpty()) emptyList() else linkedNovelReadUpdates(originals, loadEditions(group), read)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logcat(LogPriority.WARN, error) { "Could not resolve linked copies for a chapter read update" }
            emptyList()
        }
        write(updates + peers)
    }

    private suspend fun loadEditions(keys: List<NovelEditionKey>): List<NovelEdition> =
        keys.mapNotNull { key ->
            val manga = loadFavorite(key) ?: return@mapNotNull null
            NovelEdition(manga, chapterRepository.getChapterByMangaId(manga.id))
        }

    private suspend fun loadFavorite(key: NovelEditionKey): Manga? =
        mangaRepository.getMangaByUrlAndSourceId(key.url, key.source)?.takeIf { it.isNovel && it.favorite }

    private fun decode(raw: String): List<List<NovelEditionKey>> {
        if (raw.isEmpty()) return emptyList()
        return try {
            Json.decodeFromString(raw)
        } catch (error: SerializationException) {
            logcat(LogPriority.WARN, error) { "Could not read linked novel sources" }
            emptyList()
        }
    }
}
