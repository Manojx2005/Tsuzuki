package eu.kanade.tachiyomi.tsuzuki.anime.extension

import kotlinx.coroutines.CancellationException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import tachiyomi.data.Database
import tachiyomi.data.subscribeToList

internal data class AnimeExtensionRepo(val indexUrl: String, val name: String)

internal data class AnimeListedExtension(val index: HttpUrl, val entry: AnimeRepoEntry)

internal data class AnimeExtensionCatalogue(val entries: List<AnimeListedExtension>, val failed: Boolean)

internal enum class AnimeStoreAction {
    INSTALL,
    INSTALLED,
    UPDATE,
    TRUST,
    BROKEN,
}

internal data class AnimeStoreRow(
    val pkgName: String,
    val extension: AnimeExtension?,
    val listed: AnimeListedExtension?,
    val action: AnimeStoreAction,
    val removable: Boolean,
) {
    val name: String get() = extension?.name ?: listed?.entry?.name ?: pkgName
    val version: String get() = extension?.versionName ?: listed?.entry?.versionName.orEmpty()
}

internal fun animeStoreRows(
    available: List<AnimeListedExtension>,
    installed: Map<String, AnimeLoadResult>,
    isPrivate: (String) -> Boolean,
): List<AnimeStoreRow> {
    val listed = available.groupBy { it.entry.packageName }
        .mapValues { (_, entries) -> entries.maxBy { it.entry.versionCode } }
    return (listed.keys + installed.keys).map { pkg ->
        val remote = listed[pkg]
        val local = installed[pkg]
        val privateFile = isPrivate(pkg)
        val extension = when (local) {
            is AnimeLoadResult.Success -> local.extension
            is AnimeLoadResult.Untrusted -> local.extension
            else -> null
        }
        val action = when (local) {
            is AnimeLoadResult.Success -> {
                if (remote != null && remote.entry.versionCode > local.extension.versionCode) {
                    AnimeStoreAction.UPDATE
                } else {
                    AnimeStoreAction.INSTALLED
                }
            }
            is AnimeLoadResult.Untrusted -> AnimeStoreAction.TRUST
            else -> if (privateFile) AnimeStoreAction.BROKEN else AnimeStoreAction.INSTALL
        }
        AnimeStoreRow(pkg, extension, remote, action, privateFile)
    }.sortedBy { it.name.lowercase() }
}

internal class AnimeExtensionRepos(
    private val database: Database,
    private val installer: AnimeExtensionInstaller,
) {
    fun observe() = database.extension_storeQueries.getAnimeStores(::AnimeExtensionRepo).subscribeToList()

    suspend fun add(input: String) {
        val index = requireNotNull(AnimeRepoIndexParser.indexUrl(input))
        installer.loadIndex(index)
        val name = if (index.host == "raw.githubusercontent.com") {
            index.pathSegments.take(2).joinToString("/")
        } else {
            index.host
        }
        database.extension_storeQueries.insertAnimeStore(index.toString(), name)
    }

    suspend fun remove(index: String) {
        database.extension_storeQueries.deleteAnimeStore(index)
    }

    suspend fun catalogue(repos: List<AnimeExtensionRepo>): AnimeExtensionCatalogue {
        val entries = mutableListOf<AnimeListedExtension>()
        var failed = false
        for (repo in repos) {
            try {
                val index = repo.indexUrl.toHttpUrl()
                entries += installer.loadIndex(index).map { AnimeListedExtension(index, it) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = true
            }
        }
        return AnimeExtensionCatalogue(entries, failed)
    }
}
