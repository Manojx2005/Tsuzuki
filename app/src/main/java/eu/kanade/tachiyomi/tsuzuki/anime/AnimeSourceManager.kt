package eu.kanade.tachiyomi.tsuzuki.anime

import android.content.Context
import eu.kanade.tachiyomi.BuildConfig
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
    /** Sources compiled into the app (only the debug fixture, AY0); installed extensions win on the same id. */
    private val builtIn: () -> List<RegisteredAnimeSource> = { emptyList() },
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
        val results = loadExtensions()
        animeLoadFailureMessage(results)?.let { AnimeLog.w(it) }
        val registered = results
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
            .plus(builtIn())
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
            return AnimeSourceManager(
                Injekt.get(),
                loadExtensions = { withContext(Dispatchers.IO) { AnimeExtensionLoader.loadExtensions(appContext) } },
                // A6.8: the local anime source is always registered; Browse lists it only while its folder exists.
                builtIn = { debugBuiltInAnimeSources() + localAnimeSource(appContext) },
            )
        }
    }
}

/** A log line for installed anime extensions that failed to load (the result carries no package name), or null. */
internal fun animeLoadFailureMessage(results: List<AnimeLoadResult>): String? {
    val errors = results.count { it == AnimeLoadResult.Error }
    return if (errors == 0) null else "$errors anime extension(s) failed to load (signature, trust or loader error)"
}

/** A6.8: the built-in local anime source (lib 15: plain video lists, episodes without the combined update API). */
internal fun localAnimeSource(context: Context): RegisteredAnimeSource = RegisteredAnimeSource(
    eu.kanade.tachiyomi.tsuzuki.anime.local.LocalAnimeSource(context, Injekt.get()),
    extensionPackage = eu.kanade.tachiyomi.tsuzuki.anime.local.LocalAnimeSource.PACKAGE,
    extensionName = context.getString(eu.kanade.tachiyomi.R.string.tsz_local_anime),
    libVersion = 15.0,
)

/** Class name of the debug-only fixture sources (`app/src/debug`); absent from preview and release builds. */
internal const val FIXTURE_SOURCES_CLASS = "eu.kanade.tachiyomi.tsuzuki.anime.debug.FixtureAnimeSources"

/**
 * The fixture sources in debug builds, found by name so main code never imports a debug class (the pattern
 * `DebugInfoScreen` uses for `AnimeDebugActivity`). Empty in other builds or if the class is missing.
 */
internal fun debugBuiltInAnimeSources(
    debug: Boolean = BuildConfig.DEBUG,
    className: String = FIXTURE_SOURCES_CLASS,
): List<RegisteredAnimeSource> {
    if (!debug) return emptyList()
    return try {
        val holder = Class.forName(className)
        val instance = holder.getField("INSTANCE").get(null)
        (holder.getMethod("all").invoke(instance) as List<*>).filterIsInstance<AnimeSource>().map {
            RegisteredAnimeSource(
                it,
                extensionPackage = "tsuzuki.fixture",
                extensionName = "Tsuzuki Fixture",
                libVersion = 16.0,
            )
        }
    } catch (e: ReflectiveOperationException) {
        AnimeLog.d("No built-in fixture sources: ${e.javaClass.simpleName}")
        emptyList()
    }
}
