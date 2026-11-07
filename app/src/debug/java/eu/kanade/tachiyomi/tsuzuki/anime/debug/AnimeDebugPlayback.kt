package eu.kanade.tachiyomi.tsuzuki.anime.debug

import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.animesource.model.Video
import java.util.UUID

/** Keep extension URLs and headers out of intent extras handled by the exported N22 player. */
internal object AnimeDebugPlayback {
    private const val EXTRA_TOKEN = "anime_debug_playback_token"
    private var pending: Pair<String, Video>? = null

    fun launch(context: Context, video: Video) {
        val token = UUID.randomUUID().toString()
        pending = token to video
        try {
            context.startActivity(
                Intent(context, AnimeDebugPlayerActivity::class.java).putExtra(EXTRA_TOKEN, token),
            )
        } catch (e: Exception) {
            pending = null
            throw e
        }
    }

    fun take(intent: Intent): Video? {
        val request = pending ?: return null
        if (intent.getStringExtra(EXTRA_TOKEN) != request.first) return null
        pending = null
        return request.second
    }
}
