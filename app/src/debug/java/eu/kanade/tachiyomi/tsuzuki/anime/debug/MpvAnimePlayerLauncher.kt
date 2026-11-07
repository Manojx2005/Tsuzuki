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
import java.io.File

internal object MpvAnimePlayerLauncher : AnimePlayerLauncher {
    override fun launch(context: Context, request: AnimePlayback.Request): Boolean {
        if (!MpvPlayer.isAvailable) return false
        AnimePlayback.startActivity(context, request)
        return true
    }

    override fun createBackend(): AnimePlayerBackend? = if (MpvPlayer.isAvailable) MpvAnimePlayerBackend() else null
}

private class MpvAnimePlayerBackend : AnimePlayerBackend {
    private var created = false
    private var defaultUserAgent: String? = null
    private var originalVolume: Double? = null
    private var eventObserver: MPVLib.EventObserver? = null

    /** Same order as BaseMPVView.initialize(): create, options, init, then post-init options and observers. */
    override fun initialize(context: Context, listener: AnimePlayerBackend.Listener) {
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
        setOpt("idle", "yes")
        MpvPlayer.init()

        // Post-init (BaseMPVView): force-window would crash before the surface is attached.
        setOpt("force-window", "no")
        defaultUserAgent = MPVLib.getPropertyString("user-agent")

        MpvPlayer.observeProperty("time-pos", MPVLib.mpvFormat.MPV_FORMAT_DOUBLE)
        MpvPlayer.observeProperty("duration", MPVLib.mpvFormat.MPV_FORMAT_DOUBLE)
        MpvPlayer.observeProperty("demuxer-cache-time", MPVLib.mpvFormat.MPV_FORMAT_DOUBLE)
        MpvPlayer.observeProperty("eof-reached", MPVLib.mpvFormat.MPV_FORMAT_FLAG)
    }

    private fun setOpt(name: String, value: String) {
        if (!MpvPlayer.optionOrFalse(name, value)) Log.w(TAG, "mpv rejected option $name=$value")
    }

    private fun newEventObserver(listener: AnimePlayerBackend.Listener) = object : MPVLib.EventObserver {
        override fun eventProperty(property: String) = Unit

        override fun eventProperty(property: String, value: Long) = Unit

        override fun eventProperty(property: String, value: String) = Unit

        override fun eventProperty(property: String, value: Boolean) {
            if (property == "eof-reached" && value) listener.onEnded()
        }

        override fun eventProperty(property: String, value: Double) {
            when (property) {
                "duration" -> listener.onDuration(value)
                "time-pos" -> listener.onPosition(value)
                "demuxer-cache-time" -> listener.onBuffered(value)
            }
        }

        override fun event(eventId: Int) {
            when (eventId) {
                MPVLib.mpvEventId.MPV_EVENT_SEEK -> listener.onSeek()
                MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> listener.onLoaded()
            }
        }

        override fun efEvent(err: String?) {
            if (!err.isNullOrBlank()) listener.onError()
        }
    }

    override fun load(video: Video, resumePosition: Double) {
        val headers = video.headers
        MpvPlayer.property("referrer", headers?.get("Referer").orEmpty())
        MpvPlayer.property("user-agent", headers?.get("User-Agent") ?: defaultUserAgent.orEmpty())
        MpvPlayer.command("change-list", "http-header-fields", "clr", "")
        headers?.forEach { (name, value) ->
            if (!name.equals("Referer", ignoreCase = true) && !name.equals("User-Agent", ignoreCase = true)) {
                MpvPlayer.command("change-list", "http-header-fields", "append", "$name: $value")
            }
        }
        setOpt("start", resumePosition.toString())
        MpvPlayer.command("loadfile", video.videoUrl, "replace")
    }

    override fun attach(surface: Surface) {
        MpvPlayer.attachSurface(surface)
        MpvPlayer.property("vo", "gpu")
        setOpt("force-window", "yes")
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

    override fun seekTo(seconds: Double) {
        MpvPlayer.command("seek", seconds.toString(), "absolute+exact")
    }

    override fun setPaused(paused: Boolean) {
        MPVLib.setPropertyBoolean("pause", paused)
    }

    override fun setDucked(ducked: Boolean) {
        if (ducked && originalVolume == null) {
            val volume = MPVLib.getPropertyDouble("volume") ?: return
            originalVolume = volume
            MPVLib.setPropertyDouble("volume", volume * 0.25)
        } else if (!ducked) {
            originalVolume?.let { MPVLib.setPropertyDouble("volume", it) }
            originalVolume = null
        }
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
