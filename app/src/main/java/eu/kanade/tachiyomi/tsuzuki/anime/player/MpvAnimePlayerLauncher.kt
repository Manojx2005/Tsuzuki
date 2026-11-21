package eu.kanade.tachiyomi.tsuzuki.anime.player

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import eu.kanade.tachiyomi.tsuzuki.anime.describeAnimeVideo
import eu.kanade.tachiyomi.tsuzuki.anime.formatMpvLog
import eu.kanade.tachiyomi.tsuzuki.anime.mpvLogPriority
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimePlayback
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimePlayerBackend
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimePlayerLauncher
import eu.kanade.tsuzuki.animeplayer.MpvPlayer
import `is`.xyz.mpv.MPVLib
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
    private var appliedSettings: AnimePlayerSettings? = null
    private var externalTracks: Video? = null
    private var eventObserver: MPVLib.EventObserver? = null
    private var logObserver: MPVLib.LogObserver? = null
    private var loadStartedAt = 0L
    private var listener: AnimePlayerBackend.Listener? = null

    /** AY3: where inline `data:` subtitles are written so mpv can open them as files. */
    private var subtitlesDir: File? = null

    /** Same order as BaseMPVView.initialize(): create, options, init, then post-init options and observers. */
    override fun initialize(context: Context, listener: AnimePlayerBackend.Listener) {
        val configDir = File(context.filesDir, "mpv").apply { mkdirs() }
        subtitlesDir = File(context.cacheDir, "anime-subs")
        // Aniyomi copies cacert.pem from assets into the mpv config dir and points tls-ca-file at it.
        val caFile = File(configDir, "cacert.pem")
        context.assets.open("cacert.pem").use { input ->
            if (!caFile.exists() || caFile.length() != input.available().toLong()) {
                caFile.outputStream().use { input.copyTo(it) }
            }
        }
        File(configDir, "mpv.conf").apply { if (!exists()) createNewFile() }
        val fontsDir = AnimeSubtitleFonts.install(context, configDir)

        this.listener = listener
        val events = newEventObserver(listener)
        eventObserver = events
        MpvPlayer.addObserver(events)
        // mpv's own diagnostics (failed opens, TLS and HTTP errors) were invisible before: route them to logcat.
        val logs = MPVLib.LogObserver { prefix, level, text ->
            mpvLogPriority(level, BuildConfig.DEBUG)?.let { priority ->
                Log.println(priority, AnimeLog.TAG, formatMpvLog(prefix, text))
            }
        }
        logObserver = logs
        MpvPlayer.addLogObserver(logs)

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
        AnimeSubtitleFonts.options(fontsDir).forEach { (name, value) -> setOpt(name, value) }
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
        MpvPlayer.observeProperty("paused-for-cache", MPVLib.mpvFormat.MPV_FORMAT_FLAG)
        MpvPlayer.observeProperty("seeking", MPVLib.mpvFormat.MPV_FORMAT_FLAG)
        MpvPlayer.observeProperty("track-list/count", MPVLib.mpvFormat.MPV_FORMAT_INT64)
        MpvPlayer.observeProperty("sid", MPVLib.mpvFormat.MPV_FORMAT_STRING)
        MpvPlayer.observeProperty("aid", MPVLib.mpvFormat.MPV_FORMAT_STRING)
    }

    private fun setOpt(name: String, value: String) {
        if (!MpvPlayer.optionOrFalse(name, value)) AnimeLog.w("mpv rejected option $name=$value")
    }

    private var pausedForCache = false
    private var seeking = false

    private fun reportBuffering(listener: AnimePlayerBackend.Listener) = listener.onBuffering(pausedForCache || seeking)

    override fun setMediaTitle(title: String) {
        MpvPlayer.property("force-media-title", title)
    }

    private fun newEventObserver(listener: AnimePlayerBackend.Listener) = object : MPVLib.EventObserver {
        override fun eventProperty(property: String) {
            if (property == "sid" || property == "aid") listener.onTracksChanged()
        }

        override fun eventProperty(property: String, value: Long) {
            if (property == "track-list/count") listener.onTracksChanged()
        }

        override fun eventProperty(property: String, value: String) {
            if (property == "sid" || property == "aid") listener.onTracksChanged()
        }

        override fun eventProperty(property: String, value: Boolean) {
            when (property) {
                "eof-reached" -> if (value) listener.onEnded()
                "paused-for-cache" -> {
                    pausedForCache = value
                    reportBuffering(listener)
                }
                "seeking" -> {
                    seeking = value
                    reportBuffering(listener)
                }
            }
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
                MPVLib.mpvEventId.MPV_EVENT_START_FILE -> AnimeLog.i("mpv START_FILE (${sinceLoad()})")
                MPVLib.mpvEventId.MPV_EVENT_END_FILE -> AnimeLog.i("mpv END_FILE (${sinceLoad()})")
                MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> {
                    AnimeLog.i("mpv FILE_LOADED (${sinceLoad()})")
                    listener.onLoaded()
                }
            }
        }

        override fun efEvent(err: String?) {
            if (err.isNullOrBlank()) {
                AnimeLog.w("mpv end-file reported an error with no detail (${sinceLoad()})")
                return
            }
            AnimeLog.e("mpv end-file error: $err (${sinceLoad()})")
            listener.onError(err)
        }
    }

    private fun sinceLoad() = "+${SystemClock.elapsedRealtime() - loadStartedAt} ms since loadfile"

    override fun load(video: Video, resumePosition: Double) {
        externalTracks = video
        loadStartedAt = SystemClock.elapsedRealtime()
        AnimeLog.i("mpv loadfile start=${"%.1f".format(resumePosition)}s ${describeAnimeVideo(video)}")
        val command = try {
            animeMpvLoadCommand(video, resumePosition)
        } catch (e: AnimeUnsupportedVideoUrlException) {
            AnimeLog.w("refused to load video: ${e.message}")
            listener?.onError(e.message)
            return
        }
        val headers = video.headers
        MpvPlayer.property("referrer", headers?.get("Referer").orEmpty())
        MpvPlayer.property("user-agent", headers?.get("User-Agent") ?: defaultUserAgent.orEmpty())
        MpvPlayer.command("change-list", "http-header-fields", "clr", "")
        headers?.forEach { (name, value) ->
            if (!name.equals("Referer", ignoreCase = true) && !name.equals("User-Agent", ignoreCase = true)) {
                MpvPlayer.command("change-list", "http-header-fields", "append", "$name: $value")
            }
        }
        // Sources can require demuxer options (e.g. force_mpegts for image-prefixed HLS segments).
        // Pass them to this file only, including the validated resume position.
        MpvPlayer.command(*command)
    }

    override fun attach(surface: Surface) {
        MpvPlayer.attachSurface(surface)
        MpvPlayer.property("vo", "gpu")
        setOpt("force-window", "yes")
    }

    override fun resize(width: Int, height: Int) {
        AnimeLog.i("surfaceChanged ${width}x$height")
        MpvPlayer.property("android-surface-size", "${width}x$height")
    }

    override fun detach() {
        AnimeLog.i("surfaceDestroyed: detaching surface")
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

    override fun applySettings(settings: AnimePlayerSettings) {
        val previous = appliedSettings
        if (settings.speed != previous?.speed) MPVLib.setPropertyDouble("speed", settings.speed.toDouble())
        if (settings.subtitleScale != previous?.subtitleScale) {
            MPVLib.setPropertyDouble("sub-scale", settings.subtitleScale.toDouble())
        }
        if (settings.subtitleDelay != previous?.subtitleDelay) {
            MPVLib.setPropertyDouble("sub-delay", settings.subtitleDelay.toDouble())
        }
        // mpv logs a rejected value itself (e.g. sub-border-style on an older libmpv).
        changedSubtitleStyleOptions(previous?.subtitleStyle, settings.subtitleStyle).forEach { (name, value) ->
            MpvPlayer.property(name, value)
        }
        if (settings.hardwareDecoding != previous?.hardwareDecoding) {
            MpvPlayer.property("hwdec", if (settings.hardwareDecoding) "auto" else "no")
        }
        appliedSettings = settings
    }

    override fun tracks(): List<AnimePlayerTrack> {
        externalTracks?.let { video ->
            externalTracks = null
            video.subtitleTracks.forEach { track ->
                val http = track.url.toHttpUrlOrNull()?.toString()
                val inline = if (http == null) decodeAnimeDataSubtitle(track.url) else null
                // AY3-F: a cache that can't be written skips this subtitle only; video and other tracks go on.
                val target = http ?: inline?.let { sub ->
                    subtitlesDir?.let { dir -> writeAnimeDataSubtitle(dir, sub)?.path }
                }
                when {
                    target != null -> MpvPlayer.command("sub-add", *externalTrackArgs(target, track.lang))
                    inline != null -> AnimeLog.w("skipped an inline subtitle: the subtitle cache could not be written")
                    track.url.startsWith("data:", ignoreCase = true) ->
                        AnimeLog.w("skipped an inline subtitle: the data: address is malformed")
                    else -> AnimeLog.w("skipped a subtitle track that is neither an http(s) nor a data: address")
                }
            }
            video.audioTracks.forEach { track ->
                track.url.toHttpUrlOrNull()?.let {
                    MpvPlayer.command("audio-add", *externalTrackArgs(it.toString(), track.lang))
                }
                    ?: AnimeLog.w("skipped an audio track that is not an http(s) address")
            }
        }
        val count = MPVLib.getPropertyInt("track-list/count") ?: 0
        return (0 until count).mapNotNull { index ->
            val path = "track-list/$index"
            val type = MPVLib.getPropertyString("$path/type")
            val kind = AnimeTrackKind.entries.firstOrNull { it.mpvType == type } ?: return@mapNotNull null
            val id = MPVLib.getPropertyInt("$path/id") ?: return@mapNotNull null
            AnimePlayerTrack(
                id = id,
                kind = kind,
                language = MPVLib.getPropertyString("$path/lang"),
                title = MPVLib.getPropertyString("$path/title"),
                codec = MPVLib.getPropertyString("$path/codec"),
                selected = MPVLib.getPropertyBoolean("$path/selected") == true,
                forced = MPVLib.getPropertyBoolean("$path/forced") == true,
            )
        }
    }

    override fun selectTrack(kind: AnimeTrackKind, id: Int?) {
        val property = if (kind == AnimeTrackKind.SUBTITLE) "sid" else "aid"
        MpvPlayer.property(property, id?.toString() ?: "no")
    }

    override fun close() {
        eventObserver?.let(MpvPlayer::removeObserver)
        eventObserver = null
        logObserver?.let(MpvPlayer::removeLogObserver)
        logObserver = null
        if (created) {
            created = false
            MpvPlayer.destroy()
        }
    }
}
