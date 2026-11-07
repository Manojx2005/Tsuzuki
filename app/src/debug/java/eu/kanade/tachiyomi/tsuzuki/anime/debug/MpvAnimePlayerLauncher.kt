package eu.kanade.tachiyomi.tsuzuki.anime.debug

import android.content.Context
import android.util.Log
import android.view.Surface
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimePlayback
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimePlayerBackend
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimePlayerLauncher
import eu.kanade.tsuzuki.animeplayer.MpvPlayer
import `is`.xyz.mpv.MPVLib
import tachiyomi.domain.items.episode.model.Episode
import java.io.File

internal object MpvAnimePlayerLauncher : AnimePlayerLauncher {
    override fun launch(context: Context, video: Video, episode: Episode?): Boolean {
        if (!MpvPlayer.isAvailable) return false
        AnimePlayback.startActivity(context, video, episode)
        return true
    }

    override fun createBackend(): AnimePlayerBackend? = if (MpvPlayer.isAvailable) MpvAnimePlayerBackend() else null
}

private class MpvAnimePlayerBackend : AnimePlayerBackend {
    private var created = false
    private lateinit var url: String
    private var eventObserver: MPVLib.EventObserver? = null

    /** Same order as BaseMPVView.initialize(): create, options, init, then post-init options and observers. */
    override fun initialize(context: Context, request: AnimePlayback.Request, listener: AnimePlayerBackend.Listener) {
        url = request.video.videoUrl
        val configDir = File(context.filesDir, "mpv").apply { mkdirs() }
        // Aniyomi copies cacert.pem from assets into the mpv config dir and points tls-ca-file at it.
        val caFile = File(configDir, "cacert.pem")
        context.assets.open("cacert.pem").use { input ->
            if (!caFile.exists() || caFile.length() != input.available().toLong()) {
                caFile.outputStream().use { input.copyTo(it) }
            }
        }
        File(configDir, "mpv.conf").apply { if (!exists()) createNewFile() }

        val events = newEventObserver(listener)
        eventObserver = events
        MpvPlayer.addObserver(events)

        MpvPlayer.create(context, "v")
        created = true
        setOpt("config", "yes")
        setOpt("config-dir", configDir.path)
        for (opt in arrayOf("gpu-shader-cache-dir", "icc-cache-dir")) setOpt(opt, context.cacheDir.path)
        setOpt("vo", "gpu")
        setOpt("profile", "fast")
        setOpt("hwdec", "auto")
        setOpt("msg-level", "all=v")
        setOpt("ytdl", "no")
        setOpt("tls-verify", "yes")
        setOpt("tls-ca-file", caFile.path)
        setOpt("demuxer-max-bytes", "${64 * 1024 * 1024}")
        setOpt("demuxer-max-back-bytes", "${64 * 1024 * 1024}")
        setOpt("input-default-bindings", "yes")
        setOpt("keep-open", "yes")
        request.episode?.takeUnless { it.seen }?.lastSecondSeen?.takeIf { it > 0 }?.let {
            setOpt("start", it.toString())
        }
        request.video.headers?.let { headers ->
            // Use the dedicated string options for the headers commonly needed by embed hosts.
            for ((header, option) in listOf("Referer" to "referrer", "User-Agent" to "user-agent")) {
                headers[header]?.let { MpvPlayer.optionOrFalse(option, it) }
            }
            headers["Origin"]?.takeUnless { ',' in it || '\\' in it }?.let {
                MpvPlayer.optionOrFalse("http-header-fields", "Origin: $it")
            }
        }
        MpvPlayer.init()

        // Post-init (BaseMPVView): force-window would crash before the surface is attached.
        setOpt("force-window", "no")
        // idle=once so the first loadfile works, as in Aniyomi.
        setOpt("idle", "once")

        MpvPlayer.observeProperty("time-pos", MPVLib.mpvFormat.MPV_FORMAT_DOUBLE)
        MpvPlayer.observeProperty("duration", MPVLib.mpvFormat.MPV_FORMAT_DOUBLE)
        MpvPlayer.observeProperty("pause", MPVLib.mpvFormat.MPV_FORMAT_FLAG)
        MpvPlayer.observeProperty("vo-configured", MPVLib.mpvFormat.MPV_FORMAT_FLAG)
        MpvPlayer.observeProperty("video-params/w", MPVLib.mpvFormat.MPV_FORMAT_INT64)
        MpvPlayer.observeProperty("hwdec-current", MPVLib.mpvFormat.MPV_FORMAT_STRING)
    }

    private fun setOpt(name: String, value: String) {
        if (!MpvPlayer.optionOrFalse(name, value)) Log.w(TAG, "mpv rejected option $name=$value")
    }

    private fun newEventObserver(listener: AnimePlayerBackend.Listener) = object : MPVLib.EventObserver {
        private var lastLoggedSecond = -1L

        override fun eventProperty(property: String) {
            Log.i(TAG, "prop $property (no value)")
        }

        override fun eventProperty(property: String, value: Long) {
            Log.i(TAG, "prop $property = $value")
        }

        override fun eventProperty(property: String, value: Boolean) {
            Log.i(TAG, "prop $property = $value")
        }

        override fun eventProperty(property: String, value: String) {
            Log.i(TAG, "prop $property = $value")
        }

        override fun eventProperty(property: String, value: Double) {
            when (property) {
                "duration" -> listener.onDuration(value)
                "time-pos" -> listener.onPosition(value)
            }
            // time-pos changes constantly; log it once per second.
            if (property == "time-pos") {
                val second = value.toLong()
                if (second == lastLoggedSecond) return
                lastLoggedSecond = second
            }
            Log.i(TAG, "prop $property = $value")
        }

        override fun event(eventId: Int) {
            if (eventId == MPVLib.mpvEventId.MPV_EVENT_SEEK) listener.onSeek()
            val name = when (eventId) {
                MPVLib.mpvEventId.MPV_EVENT_START_FILE -> "start-file"
                MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> "file-loaded"
                MPVLib.mpvEventId.MPV_EVENT_END_FILE -> "end-file"
                MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART -> "playback-restart"
                MPVLib.mpvEventId.MPV_EVENT_VIDEO_RECONFIG -> "video-reconfig"
                MPVLib.mpvEventId.MPV_EVENT_AUDIO_RECONFIG -> "audio-reconfig"
                MPVLib.mpvEventId.MPV_EVENT_SEEK -> "seek"
                MPVLib.mpvEventId.MPV_EVENT_SHUTDOWN -> "shutdown"
                else -> "event#$eventId"
            }
            Log.i(TAG, "event $name")
        }

        override fun efEvent(err: String?) {
            if (!err.isNullOrBlank()) listener.onError()
        }
    }

    override fun attach(surface: Surface, resumePosition: Double?) {
        Log.i(TAG, "surfaceCreated: attaching surface")
        MpvPlayer.attachSurface(surface)
        setOpt("force-window", "yes")
        resumePosition?.let { setOpt("start", it.toString()) }
        MpvPlayer.command("loadfile", url)
    }

    override fun resize(width: Int, height: Int) {
        Log.i(TAG, "surfaceChanged ${width}x$height")
        MpvPlayer.property("android-surface-size", "${width}x$height")
    }

    override fun detach() {
        Log.i(TAG, "surfaceDestroyed: detaching surface")
        MpvPlayer.property("vo", "null")
        setOpt("force-window", "no")
        MpvPlayer.detachSurface()
    }

    override fun seek(seconds: Int) {
        MpvPlayer.command("seek", seconds.toString(), "relative")
    }

    override fun togglePause() {
        MpvPlayer.command("cycle", "pause")
    }

    override fun pause(): Boolean {
        val wasPlaying = MPVLib.getPropertyBoolean("pause") == false
        MpvPlayer.property("pause", "yes")
        return wasPlaying
    }

    override fun resume() {
        MpvPlayer.property("pause", "no")
    }

    override fun close() {
        eventObserver?.let(MpvPlayer::removeObserver)
        eventObserver = null
        if (created) {
            created = false
            MpvPlayer.destroy()
        }
    }

    private companion object {
        const val TAG = "TsuzukiMpv"
    }
}
