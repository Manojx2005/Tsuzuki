package eu.kanade.tachiyomi.tsuzuki.anime.debug

import android.content.Context
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtension
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtensionInstaller
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeRepoEntry
import okhttp3.HttpUrl
import tachiyomi.core.common.preference.PreferenceStore
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class AnimeDebugRepository(context: Context) {
    private val repos by lazy { Injekt.get<PreferenceStore>().getStringSet("debug_anime_repos") }
    private val installer = AnimeExtensionInstaller(context)

    fun savedRepos(): List<String> = repos.get().sorted()

    suspend fun installed() = installer.installed()

    suspend fun loadIndex(index: HttpUrl): List<AnimeRepoEntry> = installer.loadIndex(index).also {
        repos.set(repos.get() + index.toString())
    }

    suspend fun install(index: HttpUrl, entry: AnimeRepoEntry) = installer.install(index, entry)

    suspend fun trust(extension: AnimeExtension.Untrusted) = installer.trust(extension)
}
