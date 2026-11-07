package eu.kanade.tachiyomi.tsuzuki.anime.debug

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtension
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtensionLoader
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeLoadResult
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import tachiyomi.core.common.preference.PreferenceStore
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

/** Called by the debug flow's single IO operation. */
internal class AnimeDebugRepository(private val context: Context) {
    private val repos by lazy { Injekt.get<PreferenceStore>().getStringSet("debug_anime_repos") }
    private val client by lazy {
        Injekt.get<NetworkHelper>().client.newBuilder().followSslRedirects(false).build()
    }

    fun savedRepos(): List<String> = repos.get().sorted()

    fun installed(): Map<String, AnimeLoadResult> = AnimeExtensionLoader.loadExtensions(context).mapNotNull {
        when (it) {
            is AnimeLoadResult.Success -> it.extension.pkgName to it
            is AnimeLoadResult.Untrusted -> it.extension.pkgName to it
            AnimeLoadResult.Error -> null
        }
    }.toMap()

    suspend fun loadIndex(index: HttpUrl): List<AnimeRepoEntry> {
        val entries = client.newCall(Request.Builder().url(index).build()).awaitSuccess().use { response ->
            val source = checkNotNull(response.body).source()
            require(!source.request(MAX_INDEX_BYTES + 1)) { "Index exceeds 8 MiB" }
            AnimeRepoIndexParser.parse(source.readUtf8())
        }
        repos.set(repos.get() + index.toString())
        return entries
    }

    @Suppress("DEPRECATION")
    suspend fun install(index: HttpUrl, entry: AnimeRepoEntry): AnimeLoadResult {
        val current = AnimeExtensionLoader.getAnimeExtensionPackageInfoFromPkgName(context, entry.packageName)
        check(current == null) { "This package is already installed" }
        val apk = File.createTempFile("anime-extension-", ".apk", context.cacheDir)
        try {
            val url = checkNotNull(index.resolve("apk/${entry.apk}"))
            client.newCall(Request.Builder().url(url).build()).awaitSuccess().use { response ->
                val source = checkNotNull(response.body).source()
                apk.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = source.read(buffer)
                        if (read == -1) break
                        total += read
                        require(total <= MAX_APK_BYTES) { "APK exceeds 64 MiB" }
                        output.write(buffer, 0, read)
                    }
                }
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
            val info = context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
            require(
                info != null && info.packageName == entry.packageName &&
                    PackageInfoCompat.getLongVersionCode(info) == entry.versionCode,
            ) { "APK does not match the index package and version" }
            val signed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                !info.signingInfo?.apkContentsSigners.isNullOrEmpty()
            } else {
                !info.signatures.isNullOrEmpty()
            }
            require(signed) { "APK is unsigned" }
            check(AnimeExtensionLoader.installPrivateExtensionFile(context, apk) { _, _ -> }) {
                "The extension loader rejected this APK"
            }
            return AnimeExtensionLoader.loadExtensionFromPkgName(context, entry.packageName).also {
                if (it ==
                    AnimeLoadResult.Error
                ) {
                    AnimeExtensionLoader.uninstallPrivateExtension(context, entry.packageName)
                }
            }
        } finally {
            apk.delete()
        }
    }

    suspend fun trust(extension: AnimeExtension.Untrusted): AnimeLoadResult {
        val current = AnimeExtensionLoader.loadExtensionFromPkgName(context, extension.pkgName)
        check(current is AnimeLoadResult.Untrusted && current.extension == extension) {
            "The extension changed. Reopen this screen and verify its signature again."
        }
        Injekt.get<TrustExtension>().trust(extension.pkgName, extension.versionCode, extension.signatureHash)
        return AnimeExtensionLoader.loadExtensionFromPkgName(context, extension.pkgName)
    }

    @Suppress("DEPRECATION")
    suspend fun episodes(source: AnimeSource, libVersion: Double, anime: SAnime): List<SEpisode> =
        if (libVersion >= 17) {
            source.getAnimeEpisodeUpdate(anime, emptyList(), fetchDetails = false, fetchEpisodes = true).episodes
        } else {
            source.getEpisodeList(anime)
        }

    @Suppress("DEPRECATION")
    suspend fun videos(source: AnimeSource, libVersion: Double, episode: SEpisode): List<Video> {
        if (libVersion < 16) return source.getVideoList(episode)
        return source.getHosterList(episode).flatMap { it.videoList ?: source.getVideoList(it) }
    }

    @Suppress("DEPRECATION")
    suspend fun resolve(source: AnimeSource, libVersion: Double, video: Video): Video {
        val result = when {
            source !is AnimeHttpSource -> video
            libVersion < 16 && video.videoUrl.toHttpUrlOrNull() == null -> video.copy(
                videoUrl = source.getVideoUrl(video),
            )
            libVersion >= 16 -> checkNotNull(source.resolveVideo(video)) { "The source could not resolve this video" }
            else -> video
        }
        require(result.videoUrl.toHttpUrlOrNull() != null) { "The video has no HTTP(S) stream URL" }
        return result
    }

    private companion object {
        const val MAX_INDEX_BYTES = 8L * 1024 * 1024
        const val MAX_APK_BYTES = 64L * 1024 * 1024
    }
}
