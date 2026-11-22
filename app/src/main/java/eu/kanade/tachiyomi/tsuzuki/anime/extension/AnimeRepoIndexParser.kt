package eu.kanade.tachiyomi.tsuzuki.anime.extension

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
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
    @Transient val downloadUrl: String? = null,
)

@Serializable
private data class AnimeRepoDocument(val extensionList: AnimeRepoExtensionList)

@Serializable
private data class AnimeRepoExtensionList(val extensions: List<AnimeRepoDocumentEntry>)

@Serializable
private data class AnimeRepoDocumentEntry(
    val name: String,
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val resources: AnimeRepoResources,
    val sources: List<AnimeRepoSource>,
)

@Serializable
private data class AnimeRepoResources(val apkUrl: String)

@Serializable
private data class AnimeRepoSource(val language: String)

internal object AnimeRepoIndexParser {
    private val json = Json { ignoreUnknownKeys = true }
    private val packageName = Regex("[a-zA-Z][a-zA-Z0-9_]*(?:[.][a-zA-Z][a-zA-Z0-9_]*)+")
    private val apkName = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]*[.]apk")
    private val githubSegment = Regex("[a-zA-Z0-9_-][a-zA-Z0-9._-]*")
    private val indexNames = setOf("index.min.json", "index.json")

    fun parse(body: String): List<AnimeRepoEntry> {
        val document = json.parseToJsonElement(body)
        val entries = if (document is JsonArray) {
            json.decodeFromJsonElement<List<AnimeRepoEntry>>(document)
        } else {
            json.decodeFromJsonElement<AnimeRepoDocument>(document).extensionList.extensions.map { entry ->
                val apk = requireNotNull(httpsUrl(entry.resources.apkUrl))
                AnimeRepoEntry(
                    name = entry.name,
                    packageName = entry.packageName,
                    apk = apk.pathSegments.last(),
                    lang = entry.sources.map(AnimeRepoSource::language).distinct().singleOrNull() ?: "all",
                    versionCode = entry.versionCode,
                    versionName = entry.versionName,
                    downloadUrl = apk.toString(),
                )
            }
        }
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
        val url = httpsUrl(if (trimmed.startsWith("github.com/", ignoreCase = true)) "https://$trimmed" else trimmed)
            ?: return null
        val segments = url.pathSegments.dropLastWhile(String::isEmpty)
        if (url.host == "github.com") return githubIndex(segments)
        val last = segments.lastOrNull() ?: return null
        if (last in indexNames) return url
        val folderPath = '.' !in last || url.encodedPath.endsWith('/')
        val folder = if (url.host == "raw.githubusercontent.com") {
            val branchOnly = segments.size == 3 ||
                (segments.size == 5 && segments[2] == "refs" && segments[3] == "heads")
            segments.size >= 3 && segments.take(2).all(githubSegment::matches) && (branchOnly || folderPath)
        } else {
            folderPath
        }
        if (!folder) return null
        return url.newBuilder().encodedPath(url.encodedPath.trimEnd('/') + "/index.min.json").build()
    }

    private fun githubIndex(segments: List<String>): HttpUrl? {
        if (segments.size < 2 || segments.take(2).any { !githubSegment.matches(it) }) return null
        val (owner, repository) = segments
        if (segments.size == 2) {
            val name = if (owner.equals("yuzono", true) && repository.equals("anime-extensions", true)) {
                "anime-repo"
            } else {
                repository
            }
            return "https://raw.githubusercontent.com/$owner/$name/repo/index.min.json".toHttpUrlOrNull()
        }
        val path = segments.drop(3)
        val indexPath = when (segments[2]) {
            "tree" -> path.takeIf { it.isNotEmpty() }?.plus("index.min.json")
            "blob", "raw" -> path.takeIf { it.size >= 2 && it.last() in indexNames }
            else -> null
        } ?: return null
        return HttpUrl.Builder()
            .scheme("https")
            .host("raw.githubusercontent.com")
            .apply { (listOf(owner, repository) + indexPath).forEach(::addPathSegment) }
            .build()
    }

    private fun httpsUrl(input: String): HttpUrl? = input.toHttpUrlOrNull()?.takeIf {
        it.isHttps && it.username.isEmpty() && it.password.isEmpty() && it.fragment == null
    }
}
