package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import tachiyomi.domain.items.episode.model.Episode
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.UUID

/** Keep extension URLs and headers in memory and pass the private activity a one-use token. */
internal object AnimePlayback {
    data class Request(val video: Video, val episode: Episode?)

    private const val EXTRA_TOKEN = "anime_playback_token"
    private var pending: Pair<String, Request>? = null

    fun launch(context: Context, video: Video, episode: Episode? = null) {
        if (!TsuzukiAnime.enabled || !Injekt.get<AnimePlayerLauncher>().launch(context, video, episode)) {
            Toast.makeText(context, R.string.tsz_anime_player_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    fun startActivity(context: Context, video: Video, episode: Episode?) {
        val token = UUID.randomUUID().toString()
        pending = token to Request(video, episode)
        try {
            context.startActivity(Intent(context, AnimePlayerActivity::class.java).putExtra(EXTRA_TOKEN, token))
        } catch (e: Exception) {
            pending = null
            throw e
        }
    }

    fun take(intent: Intent): Request? {
        val request = pending ?: return null
        if (intent.getStringExtra(EXTRA_TOKEN) != request.first) return null
        pending = null
        return request.second
    }
}
