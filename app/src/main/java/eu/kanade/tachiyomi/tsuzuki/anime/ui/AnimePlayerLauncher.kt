package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.content.Context
import android.view.Surface
import eu.kanade.tachiyomi.animesource.model.Video

internal interface AnimePlayerLauncher {
    fun launch(context: Context, request: AnimePlayback.Request): Boolean
    fun createBackend(): AnimePlayerBackend?
}

internal interface AnimePlayerBackend : AutoCloseable {
    fun initialize(context: Context, listener: Listener)
    fun load(video: Video, resumePosition: Double)
    fun attach(surface: Surface)
    fun resize(width: Int, height: Int)
    fun detach()
    fun seekTo(seconds: Double)
    fun setPaused(paused: Boolean)
    fun setDucked(ducked: Boolean)

    interface Listener {
        fun onPosition(seconds: Double)
        fun onDuration(seconds: Double)
        fun onBuffered(seconds: Double)
        fun onSeek()
        fun onLoaded()
        fun onEnded()
        fun onError()
    }
}
