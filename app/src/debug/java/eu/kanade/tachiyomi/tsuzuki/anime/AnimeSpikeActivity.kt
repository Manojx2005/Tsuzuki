package eu.kanade.tachiyomi.tsuzuki.anime

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.tsuzuki.anime.data.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeExtensionLoader
import eu.kanade.tachiyomi.tsuzuki.anime.extension.AnimeLoadResult
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerActivity
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.core.common.Constants
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.data.Tsuzuki_anime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Debug source browser. Stage 2 writes only explicit anime favorites and episode metadata. */
class AnimeSpikeActivity : ComponentActivity() {
    private lateinit var rows: LinearLayout
    private var busy = false
    private val library by lazy { AnimeLibraryStore() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rows = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        setContentView(ScrollView(this).apply { addView(rows) })
        val animeId = intent.getLongExtra("anime_id", -1)
        if (animeId >= 0) openSavedAnime(animeId) else extensions()
    }

    private fun page(title: String) {
        rows.removeAllViews()
        rows.addView(
            TextView(this).apply {
                text = "Anime spike — $title"
                textSize = 22f
            },
        )
        button("Extensions") { extensions() }
    }

    private fun button(label: String, action: () -> Unit) {
        rows.addView(
            Button(this).apply {
                text = label
                setOnClickListener { if (!busy) action() }
            },
        )
    }

    private fun <T> request(work: suspend () -> T, show: (T) -> Unit) {
        if (busy) return
        busy = true
        val status = TextView(this).apply { text = "Loading…" }
        rows.addView(status)
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { work() }
                busy = false
                rows.removeView(status)
                show(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status.text = "${e.javaClass.simpleName}: ${e.message}"
                if (status.parent == null) rows.addView(status)
            } finally {
                busy = false
            }
        }
    }

    private fun extensions() {
        page("Installed extensions")
        button("Anime library · Dot") { openLibrary(AppTheme.DOT) }
        button("Anime library · Colour") { openLibrary(AppTheme.COLOUR) }
        button("Library · Default") { openLibrary(AppTheme.DEFAULT) }
        request({ AnimeExtensionLoader.loadExtensions(this) }) { results ->
            if (results.isEmpty()) rows.addView(TextView(this).apply { text = "No anime extensions installed." })
            results.forEach { result ->
                when (result) {
                    is AnimeLoadResult.Success -> result.extension.sources.forEach { source ->
                        button("${source.name} (${source.lang})") { catalogue(source) }
                    }
                    is AnimeLoadResult.Untrusted -> button("Untrusted: ${result.extension.name}") {
                        val ext = result.extension
                        AlertDialog.Builder(this)
                            .setTitle("Trust this extension?")
                            .setMessage(
                                "${ext.pkgName}\n${ext.versionName}\nSHA-256: ${ext.signatureHash}\n" +
                                    "Extensions execute code inside this app.",
                            )
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Trust") { _, _ ->
                                Injekt.get<TrustExtension>().trust(ext.pkgName, ext.versionCode, ext.signatureHash)
                                extensions()
                            }.show()
                    }
                    AnimeLoadResult.Error -> rows.addView(
                        TextView(this).apply { text = "Extension failed to load. See logcat." },
                    )
                }
            }
        }
    }

    private fun catalogue(source: AnimeSource) {
        page(source.name)
        val query = EditText(this).apply { hint = "Search" }
        rows.addView(query)
        button("Search") {
            request({ source.getSearchAnime(1, query.text.toString(), source.getFilterList()).animes }) {
                animeRows(source, it)
            }
        }
        button("Popular") { request({ source.getPopularAnime(1).animes }) { animeRows(source, it) } }
    }

    private fun animeRows(source: AnimeSource, anime: List<SAnime>) {
        page(source.name)
        button("Search / Popular") { catalogue(source) }
        anime.forEach { entry -> button(entry.title) { episodes(source, entry) } }
    }

    private fun episodes(source: AnimeSource, anime: SAnime) {
        page(anime.title)
        request({
            val sourceUrl = anime.url
            val update = source.getAnimeEpisodeUpdate(anime, emptyList(), true, true)
            // Details parsers may return metadata without the original catalogue identity.
            update.anime.url = sourceUrl
            if (update.anime.thumbnail_url == null) update.anime.thumbnail_url = anime.thumbnail_url
            val id = library.id(source.id, sourceUrl)
            if (id != null) library.syncEpisodes(id, update.episodes)
            update to id?.let { library.get(it) }
        }) { (update, saved) ->
            episodeRows(source, update.anime, update.episodes, saved)
        }
    }

    private fun episodeRows(source: AnimeSource, anime: SAnime, episodes: List<SEpisode>, saved: Tsuzuki_anime?) {
        page(anime.title)
        val favorite = saved?.favorite == 1L
        button(if (favorite) "Remove from anime library" else "Add to anime library") {
            request({
                val id = if (favorite) {
                    library.remove(saved!!._id)
                    saved._id
                } else {
                    library.add(source.id, anime, episodes)
                }
                library.get(id)
            }) { episodeRows(source, anime, episodes, it) }
        }
        if (favorite) {
            listOf("Plan to watch", "Watching", "Completed").forEachIndexed { status, label ->
                button(if (saved!!.watch_status == status.toLong()) "$label ✓" else label) {
                    request({
                        library.setWatchStatus(saved!!._id, status)
                        library.get(saved._id)
                    }) { episodeRows(source, anime, episodes, it) }
                }
            }
        }
        episodes.forEach { episode -> button(episode.name) { videos(source, episode) } }
    }

    private fun openSavedAnime(id: Long) {
        page("Saved anime")
        request({
            val saved = library.get(id) ?: error("Saved anime not found")
            val source = AnimeExtensionLoader.loadExtensions(this)
                .filterIsInstance<AnimeLoadResult.Success>().flatMap { it.extension.sources }
                .firstOrNull { it.id == saved.source } ?: error("Install and trust this anime source to open episodes")
            source to SAnime.create().apply {
                url = saved.url
                title = saved.title
                thumbnail_url = saved.thumbnail_url
                description = saved.description
                genre = saved.genre
                status = saved.status.toInt()
            }
        }) { (source, anime) -> episodes(source, anime) }
    }

    private fun openLibrary(theme: AppTheme) {
        Injekt.get<UiPreferences>().appTheme.set(theme)
        Injekt.get<PreferenceStore>().getBoolean("tsuzuki_library_anime", false).set(true)
        startActivity(Intent(this, MainActivity::class.java).setAction(Constants.SHORTCUT_LIBRARY))
    }

    @Suppress("DEPRECATION")
    private fun videos(source: AnimeSource, episode: SEpisode) {
        page(episode.name)
        request({
            // Legacy extensions implement episode videos
            // Lib 16/17 use hosters.
            try {
                source.getVideoList(episode)
            } catch (_: UnsupportedOperationException) {
                source.getHosterList(episode).flatMap { it.videoList ?: source.getVideoList(it) }
            }
        }) { videos ->
            if (videos.isEmpty()) rows.addView(TextView(this).apply { text = "No videos returned." })
            videos.forEachIndexed { index, video ->
                button("${if (index == 0) "Play first: " else ""}${video.videoTitle}") {
                    request({
                        val resolved = if (source is AnimeHttpSource) {
                            source.resolveVideo(video) ?: error("Source could not resolve video")
                        } else {
                            video
                        }
                        val direct = resolved.videoUrl
                        val url = if (direct.isNotBlank() && direct != "null") {
                            direct
                        } else if (source is AnimeHttpSource) {
                            source.getVideoUrl(resolved)
                        } else {
                            error("Source returned no playable URL")
                        }
                        require(url.startsWith("https://") || url.startsWith("http://")) { "Unsupported stream URL" }
                        url to resolved
                    }) { (url, resolved) -> play(url, episode.name, resolved) }
                }
            }
        }
    }

    private fun play(url: String, title: String, video: Video) {
        if (!BuildConfig.ANIME_SPIKE) {
            rows.addView(TextView(this).apply { text = "Native playback is unavailable in this build." })
            return
        }
        startActivity(
            Intent(this, AnimePlayerActivity::class.java).apply {
                putExtra("url", url)
                putExtra("title", title)
                putStringArrayListExtra(
                    "headers",
                    ArrayList(video.headers?.map { "${it.first}: ${it.second}" }.orEmpty()),
                )
            },
        )
    }
}
