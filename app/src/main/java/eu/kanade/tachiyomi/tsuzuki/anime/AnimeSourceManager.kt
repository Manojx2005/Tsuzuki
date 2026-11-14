package eu.kanade.tachiyomi.tsuzuki.anime

import android.content.Context
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtensionLoader
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeLoadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tachiyomi.domain.source.anime.model.StubAnimeSource
import tachiyomi.domain.source.anime.repository.AnimeSourceRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal data class RegisteredAnimeSource(
    val source: AnimeSource,
    val extensionPackage: String,
    val extensionName: String,
    val libVersion: Double,
)

internal class AnimeSourceManager(
    private val repository: AnimeSourceRepository,
    private val loadExtensions: suspend () -> List<AnimeLoadResult>,
) {
    private val refreshLock = Mutex()

    @Volatile
    private var installed = emptyMap<Long, RegisteredAnimeSource>()

    /** The [extensionChanges] value the current [installed] map was loaded at; null before the first load. */
    private var loadedAt: Long? = null

    /** Loads every installed extension again. Use when something outside the app may have changed them. */
    suspend fun refresh(): List<RegisteredAnimeSource> = refreshLock.withLock { load() }

    /**
     * Loads the extensions once and reuses the result until [extensionsChanged] is called. The player, catalogue and
     * downloads share one instance, so opening an episode no longer reloads every extension (and recreates every
     * source, losing whatever state the extension keeps in memory).
     */
    suspend fun ensureLoaded(): List<RegisteredAnimeSource> = refreshLock.withLock {
        if (loadedAt == extensionChanges.value) installed.values.toList() else load()
    }

    private suspend fun load(): List<RegisteredAnimeSource> {
        val version = extensionChanges.value
        val registered = loadExtensions()
            .filterIsInstance<AnimeLoadResult.Success>()
            .sortedBy { it.extension.pkgName }
            .flatMap { (extension) ->
                extension.sources.map { source ->
                    RegisteredAnimeSource(
                        source,
                        extension.pkgName,
                        extension.name,
                        extension.libVersion,
                    )
                }
            }
            .distinctBy { it.source.id }
        val saved = repository.awaitAll().associateBy { it.id }
        registered.forEach { entry ->
            val source = entry.source
            val metadata = StubAnimeSource(source.id, source.lang, source.name)
            if (saved[source.id] != metadata) repository.upsert(metadata)
        }
        installed = registered.associateBy { it.source.id }
        loadedAt = version
        return registered
    }

    fun get(id: Long): RegisteredAnimeSource? = installed[id]

    suspend fun stub(id: Long): StubAnimeSource? = repository.awaitById(id)

    companion object {
        private val mutableExtensionChanges = MutableStateFlow(0L)
        val extensionChanges = mutableExtensionChanges.asStateFlow()

        fun extensionsChanged() {
            mutableExtensionChanges.update { it + 1 }
        }

        fun create(context: Context): AnimeSourceManager {
            val appContext = context.applicationContext
            return AnimeSourceManager(Injekt.get()) {
                withContext(Dispatchers.IO) { AnimeExtensionLoader.loadExtensions(appContext) }
            }
        }
    }
}
