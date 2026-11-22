package eu.kanade.tachiyomi.tsuzuki.anime.extension

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeSourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

internal class AnimeExtensionInstaller(private val context: Context) {
    private val client by lazy {
        Injekt.get<NetworkHelper>().client.newBuilder().followSslRedirects(false).build()
    }

    suspend fun installed(): Map<String, AnimeLoadResult> = withContext(Dispatchers.IO) {
        AnimeExtensionLoader.loadExtensions(context).mapNotNull {
            when (it) {
                is AnimeLoadResult.Success -> it.extension.pkgName to it
                is AnimeLoadResult.Untrusted -> it.extension.pkgName to it
                AnimeLoadResult.Error -> null
            }
        }.toMap().toMutableMap().apply {
            AnimeExtensionLoader.privateExtensionPackages(context).forEach { putIfAbsent(it, AnimeLoadResult.Error) }
        }
    }

    suspend fun loadIndex(index: HttpUrl): List<AnimeRepoEntry> = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(index).build()).awaitSuccess().use { response ->
            val source = checkNotNull(response.body).source()
            require(!source.request(MAX_INDEX_BYTES + 1)) { "Index exceeds 8 MiB" }
            AnimeRepoIndexParser.parse(source.readUtf8())
        }
    }

    suspend fun install(index: HttpUrl, entry: AnimeRepoEntry): AnimeLoadResult = withContext(Dispatchers.IO) {
        mutationLock.withLock { installFromIndex(index, entry) }
    }

    @Suppress("DEPRECATION")
    private suspend fun installFromIndex(index: HttpUrl, entry: AnimeRepoEntry): AnimeLoadResult {
        val current = AnimeExtensionLoader.getAnimeExtensionPackageInfoFromPkgName(context, entry.packageName)
        require(current == null || entry.versionCode > PackageInfoCompat.getLongVersionCode(current))
        val apk = File.createTempFile("anime-extension-", ".apk", context.cacheDir)
        try {
            val url = entry.downloadUrl?.toHttpUrl() ?: checkNotNull(index.resolve("apk/${entry.apk}"))
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
            return AnimeExtensionLoader.installPrivateExtensionFile(context, apk).also { result ->
                if (result != AnimeLoadResult.Error) AnimeSourceManager.extensionsChanged()
            }
        } finally {
            apk.delete()
        }
    }

    suspend fun trust(extension: AnimeExtension.Untrusted): AnimeLoadResult = withContext(Dispatchers.IO) {
        mutationLock.withLock {
            val current = AnimeExtensionLoader.loadExtensionFromPkgName(context, extension.pkgName)
            check(current is AnimeLoadResult.Untrusted && current.extension == extension) {
                "The extension changed before it was trusted"
            }
            Injekt.get<TrustExtension>().trust(extension.pkgName, extension.versionCode, extension.signatureHash)
            AnimeExtensionLoader.loadExtensionFromPkgName(context, extension.pkgName).also {
                AnimeSourceManager.extensionsChanged()
            }
        }
    }

    fun isPrivate(pkgName: String): Boolean = AnimeExtensionLoader.isPrivateExtension(context, pkgName)

    suspend fun remove(pkgName: String) = withContext(Dispatchers.IO) {
        mutationLock.withLock {
            check(!isPrivate(pkgName) || AnimeExtensionLoader.uninstallPrivateExtension(context, pkgName))
            AnimeSourceManager.extensionsChanged()
        }
    }

    private companion object {
        val mutationLock = Mutex()
        const val MAX_INDEX_BYTES = 8L * 1024 * 1024
        const val MAX_APK_BYTES = 64L * 1024 * 1024
    }
}
