package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.content.Context
import android.view.Surface
import eu.kanade.tachiyomi.animesource.model.Video
import tachiyomi.domain.items.episode.model.Episode

internal interface AnimePlayerLauncher {
    fun launch(context: Context, video: Video, episode: Episode?): Boolean
    fun createBackend(): AnimePlayerBackend?
}

internal interface AnimePlayerBackend : AutoCloseable {
    fun initialize(context: Context, request: AnimePlayback.Request, listener: Listener)
    fun attach(surface: Surface, resumePosition: Double?)
    fun resize(width: Int, height: Int)
    fun detach()
    fun seek(seconds: Int)
    fun togglePause()
    fun pause(): Boolean
    fun resume()

    interface Listener {
        fun onPosition(seconds: Double)
        fun onDuration(seconds: Double)
        fun onSeek()
        fun onError()
    }
}
