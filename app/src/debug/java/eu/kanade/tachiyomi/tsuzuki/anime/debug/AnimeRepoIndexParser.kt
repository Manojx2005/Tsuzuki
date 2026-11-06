package eu.kanade.tachiyomi.tsuzuki.anime.debug

import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeLoadResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Serializable
internal data class AnimeRepoEntry(
    val name: String,
    @SerialName("pkg") val packageName: String,
    val apk: String,
    val lang: String,
    @SerialName("code") val versionCode: Long,
    @SerialName("version") val versionName: String,
)

internal object AnimeRepoIndexParser {
    private val json = Json { ignoreUnknownKeys = true }
    private val packageName = Regex("[a-zA-Z][a-zA-Z0-9_]*(?:[.][a-zA-Z][a-zA-Z0-9_]*)+")
    private val apkName = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]*[.]apk")
    private val githubSegment = Regex("[a-zA-Z0-9_-][a-zA-Z0-9._-]*")

    fun parse(body: String): List<AnimeRepoEntry> {
        val entries = json.decodeFromString<List<AnimeRepoEntry>>(body)
        require(
            entries.all {
                it.name.isNotBlank() && packageName.matches(it.packageName) &&
                    apkName.matches(it.apk) && it.versionCode > 0
            },
        )
        require(entries.distinctBy { it.packageName }.size == entries.size)
        return entries
    }

    fun indexUrl(input: String): HttpUrl? {
        val trimmed = input.trim()
        val url = (if (trimmed.startsWith("github.com/", ignoreCase = true)) "https://$trimmed" else trimmed)
            .toHttpUrlOrNull() ?: return null
        if (!url.isHttps || url.username.isNotEmpty() || url.password.isNotEmpty()) return null
        if (url.host != "github.com") {
            return url.takeIf { it.pathSegments.last() == "index.min.json" }
        }

        val segments = url.pathSegments.dropLastWhile(String::isEmpty)
        if (segments.size != 2 || segments.any { !githubSegment.matches(it) }) return null
        val (owner, repo) = segments
        val name = if (owner.equals("yuzono", true) && repo.equals("anime-extensions", true)) "anime-repo" else repo
        return "https://raw.githubusercontent.com/$owner/$name/repo/index.min.json".toHttpUrlOrNull()
    }
}

internal enum class AnimeDebugInstallAction {
    Install,
    Trust,
    Installed,
    Retry,
}

internal fun animeDebugInstallAction(extension: AnimeLoadResult?) =
    when (extension) {
        null -> AnimeDebugInstallAction.Install
        is AnimeLoadResult.Untrusted -> AnimeDebugInstallAction.Trust
        is AnimeLoadResult.Success -> AnimeDebugInstallAction.Installed
        AnimeLoadResult.Error -> AnimeDebugInstallAction.Retry
    }
