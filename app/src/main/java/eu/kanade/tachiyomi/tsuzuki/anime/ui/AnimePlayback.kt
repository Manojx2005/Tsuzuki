package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.UUID

/** Keep extension URLs and headers in memory and pass the private activity a one-use token. */
internal object AnimePlayback {
    sealed interface Request {
        data class Direct(val video: Video) : Request
        data class Episode(val id: Long, val track: Boolean) : Request
    }

    private const val EXTRA_TOKEN = "anime_playback_token"
    private const val EXTRA_EPISODE = "anime_playback_episode"
    private const val EXTRA_TRACK = "anime_playback_track"
    private var pending: Pair<String, Request.Direct>? = null

    fun launch(context: Context, video: Video) = launch(context, Request.Direct(video))

    fun launchEpisode(context: Context, id: Long, track: Boolean) = launch(context, Request.Episode(id, track))

    private fun launch(context: Context, request: Request) {
        val launcher = Injekt.get<AnimePlayerLauncher>()
        if (!TsuzukiAnime.enabled || !launcher.launch(context, request)) {
            Toast.makeText(context, launcher.unavailableMessage, Toast.LENGTH_LONG).show()
        }
    }

    fun startActivity(context: Context, request: Request) {
        val intent = Intent(context, AnimePlayerActivity::class.java)
        when (request) {
            is Request.Direct -> {
                val token = UUID.randomUUID().toString()
                pending = token to request
                intent.putExtra(EXTRA_TOKEN, token)
            }
            is Request.Episode -> {
                intent.putExtra(EXTRA_EPISODE, request.id)
                intent.putExtra(EXTRA_TRACK, request.track)
            }
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            if (request is Request.Direct) pending = null
            throw e
        }
    }

    fun take(intent: Intent): Request? {
        val episode = intent.getLongExtra(EXTRA_EPISODE, -1)
        if (episode >= 0) return Request.Episode(episode, intent.getBooleanExtra(EXTRA_TRACK, false))
        val request = pending ?: return null
        if (intent.getStringExtra(EXTRA_TOKEN) != request.first) return null
        pending = null
        return request.second
    }
}
