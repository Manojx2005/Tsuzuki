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

    suspend fun refresh(): List<RegisteredAnimeSource> = refreshLock.withLock {
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
        registered
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
