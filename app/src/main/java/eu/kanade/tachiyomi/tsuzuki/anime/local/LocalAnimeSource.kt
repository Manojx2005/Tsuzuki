package eu.kanade.tachiyomi.tsuzuki.anime.local

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tachiyomi.domain.storage.service.StorageManager

/**
 * A6.8 (board `docs/design/boards/remaining-2026-11/LocalAnime.dc.html`): videos on the device, like the local
 * manga source. `<storage>/localanime/<Title>/` holds a show's mp4/mkv/webm files, an optional `cover.jpg` and an
 * optional `details.json` (local manga's keys); sidecar .srt/.ass with the video's name become subtitle tracks.
 * Registered (Browse › Watch, "On this device") only while the folder exists; never creates it. All reads on IO.
 */
internal class LocalAnimeSource(
    private val context: Context,
    private val storage: StorageManager,
    private val storagePreferences: tachiyomi.domain.storage.service.StoragePreferences,
) : AnimeCatalogueSource {
    override val id: Long = LOCAL_ANIME_SOURCE_ID
    override val name: String get() = context.getString(R.string.tsz_local_anime)
    override val lang: String = "other"
    override val supportsLatest: Boolean = true

    private val json = Json { ignoreUnknownKeys = true }

    /** The folder beside the local manga folder, or null while it does not exist. */
    fun baseDirectory(): UniFile? {
        // A tree picked through the system picker has no parentFile, so start from the storage root itself.
        val root = UniFile.fromUri(context, android.net.Uri.parse(storagePreferences.baseStorageDirectory.get()))
            ?: storage.getLocalSourceDirectory()?.parentFile
        return root?.findFile(LOCAL_ANIME_FOLDER)?.takeIf { it.isDirectory }
    }

    private fun shows(): List<UniFile> =
        baseDirectory()?.listFiles().orEmpty().filter { it.isDirectory && !it.name.isNullOrBlank() }

    private fun show(name: String): UniFile? = baseDirectory()?.findFile(name)?.takeIf { it.isDirectory }

    override suspend fun getPopularAnime(page: Int): AnimesPage = withContext(Dispatchers.IO) {
        AnimesPage(shows().sortedBy { it.name.orEmpty().lowercase() }.map(::toAnime), hasNextPage = false)
    }

    override suspend fun getLatestUpdates(page: Int): AnimesPage = withContext(Dispatchers.IO) {
        AnimesPage(shows().sortedByDescending { it.lastModified() }.map(::toAnime), hasNextPage = false)
    }

    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage =
        withContext(Dispatchers.IO) {
            val needle = query.trim()
            AnimesPage(
                shows().filter { it.name.orEmpty().contains(needle, ignoreCase = true) }
                    .sortedBy { it.name.orEmpty().lowercase() }
                    .map(::toAnime),
                hasNextPage = false,
            )
        }

    override suspend fun getAnimeDetails(anime: SAnime): SAnime = withContext(Dispatchers.IO) {
        val folder = show(anime.url) ?: return@withContext anime
        val details = toAnime(folder)
        val meta = folder.findFile(DETAILS_FILE)?.let { file ->
            runCatching {
                file.openInputStream().use { json.decodeFromString<LocalAnimeDetails>(it.reader().readText()) }
            }.getOrNull()
        }
        if (meta != null) {
            meta.title?.takeIf(String::isNotBlank)?.let { details.title = it }
            details.author = meta.author
            details.artist = meta.artist
            details.description = meta.description
            details.genre = meta.genre?.joinToString()
            meta.status?.toIntOrNull()?.let { details.status = it }
        }
        details.initialized = true
        details
    }

    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> = withContext(Dispatchers.IO) {
        val folder = show(anime.url) ?: return@withContext emptyList()
        folder.listFiles().orEmpty()
            .filter { !it.isDirectory && it.name.orEmpty().fileExtension() in LOCAL_ANIME_VIDEO_EXTENSIONS }
            .sortedBy { it.name.orEmpty().lowercase() }
            .mapIndexed { index, file ->
                val fileName = file.name.orEmpty()
                SEpisode.create().apply {
                    url = "${anime.url}/$fileName"
                    name = fileName.fileBaseName()
                    // Without a number in the name, file order (1, 2, …) as the board says.
                    episode_number = (localEpisodeNumber(fileName) ?: (index + 1).toDouble()).toFloat()
                    date_upload = file.lastModified()
                }
            }
            .sortedByDescending { it.episode_number }
    }

    override suspend fun getVideoList(episode: SEpisode): List<Video> = withContext(Dispatchers.IO) {
        val folder = show(episode.url.substringBefore('/')) ?: return@withContext emptyList()
        val fileName = episode.url.substringAfter('/')
        val siblings = folder.listFiles().orEmpty().filter { !it.isDirectory }.associateBy { it.name.orEmpty() }
        val video = siblings[fileName] ?: return@withContext emptyList()
        val subtitles = localSubtitlesFor(fileName, siblings.keys.toList()).mapNotNull { (name, tag) ->
            siblings[name]?.let {
                Track(LocalAnimeVideos.issue(it.uri.toString()), tag.ifBlank { name.fileExtension() })
            }
        }
        listOf(
            Video(
                videoUrl = LocalAnimeVideos.issue(video.uri.toString()),
                videoTitle = fileName,
                subtitleTracks = subtitles,
                initialized = true,
            ),
        )
    }

    @Deprecated("Use the suspend API")
    override fun fetchPopularAnime(page: Int): rx.Observable<AnimesPage> = throw UnsupportedOperationException()

    @Deprecated("Use the suspend API")
    override fun fetchSearchAnime(page: Int, query: String, filters: AnimeFilterList): rx.Observable<AnimesPage> =
        throw UnsupportedOperationException()

    @Deprecated("Use the suspend API")
    override fun fetchLatestUpdates(page: Int): rx.Observable<AnimesPage> = throw UnsupportedOperationException()

    /** Plain video lists (registered with lib 15), never hosters. */
    override suspend fun getHosterList(episode: SEpisode): List<Hoster> = throw UnsupportedOperationException()

    override suspend fun getVideoList(hoster: Hoster): List<Video> = throw UnsupportedOperationException()

    private fun toAnime(folder: UniFile) = SAnime.create().apply {
        url = folder.name.orEmpty()
        title = folder.name.orEmpty()
        thumbnail_url = folder.findFile(COVER_FILE)?.uri?.toString()
    }

    @Serializable
    private data class LocalAnimeDetails(
        val title: String? = null,
        val author: String? = null,
        val artist: String? = null,
        val description: String? = null,
        val genre: List<String>? = null,
        val status: String? = null,
    )

    companion object {
        /** Aniyomi's local anime source id, so a restored Aniyomi library keeps pointing here (AY8). */
        const val LOCAL_ANIME_SOURCE_ID = 0L
        const val LOCAL_ANIME_FOLDER = "localanime"
        const val PACKAGE = "tsuzuki.localanime"
        private const val COVER_FILE = "cover.jpg"
        private const val DETAILS_FILE = "details.json"
    }
}
