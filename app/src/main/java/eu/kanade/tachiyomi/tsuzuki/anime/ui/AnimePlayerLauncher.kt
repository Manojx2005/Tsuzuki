package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.content.Context
import android.view.Surface
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerSettings
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerTrack
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimeTrackKind

internal interface AnimePlayerLauncher {
    val unavailableMessage: Int get() = R.string.tsz_anime_player_unavailable

    fun launch(context: Context, request: AnimePlayback.Request): Boolean
    fun createBackend(): AnimePlayerBackend?
}

internal interface AnimePlayerBackend : AutoCloseable {
    fun initialize(context: Context, listener: Listener)
    fun load(video: Video, resumePosition: Double, subtitles: List<Track>)
    fun attach(surface: Surface)
    fun resize(width: Int, height: Int)
    fun detach()
    fun seekTo(seconds: Double)
    fun setPaused(paused: Boolean)
    fun setDucked(ducked: Boolean)
    fun applySettings(settings: AnimePlayerSettings)
    fun tracks(): List<AnimePlayerTrack>
    fun selectTrack(kind: AnimeTrackKind, id: Int?)

    /** Submit a video-only screenshot before native teardown; encoding is asynchronous. */
    fun capturePreview(): java.io.File? = null

    /** A6.7: write the current video frame (no subtitles, no controls) to [target]; false when not possible. */
    fun captureScreenshot(target: java.io.File): Boolean = false

    /** AY5: the file's chapters (mpv `chapter-list`), read after FILE_LOADED. */
    fun chapters(): List<eu.kanade.tachiyomi.tsuzuki.anime.player.AnimeFileChapter> = emptyList()

    /** AY4: the title mpv shows (PiP, its own OSD); "Anime — Episode". */
    fun setMediaTitle(title: String) = Unit

    interface Listener {
        fun onPosition(seconds: Double)
        fun onDuration(seconds: Double)
        fun onBuffered(seconds: Double)
        fun onSeek()
        fun onLoaded()
        fun onTracksChanged()
        fun onEnded()
        fun onError(detail: String?)

        /** AY4: waiting for data (mpv `paused-for-cache`) or for a seek to finish. */
        fun onBuffering(buffering: Boolean) = Unit
    }
}
