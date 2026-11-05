package eu.kanade.tachiyomi.tsuzuki.anime.debug

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import eu.kanade.tsuzuki.animeplayer.MpvPlayer
import `is`.xyz.mpv.MPVLib
import java.io.File

/**
 * Debug-only libmpv test player (N22 / N22b). Plays the `url` intent extra with play/pause, seek +-10 s and back.
 * Not part of preview or release builds; `anime-player` is a debugImplementation dependency.
 *
 * The init and surface sequence follows aniyomi-mpv-lib's `BaseMPVView` and Aniyomi's `AniyomiMPVView` at the
 * pinned revisions (docs/ANIME_SPIKE.md). All mpv logs, events and key properties go to logcat with tag [TAG].
 */
class AnimeDebugPlayerActivity : Activity(), SurfaceHolder.Callback {

    private var started = false
    private var url: String? = null
    private var logObserver: MPVLib.LogObserver? = null
    private var eventObserver: MPVLib.EventObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        url = intent.getStringExtra("url")
        if (url.isNullOrBlank()) {
            Toast.makeText(this, "Missing intent extra: url", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if (!MpvPlayer.isAvailable) {
            Toast.makeText(this, "libmpv is not available on this device (arm64-v8a only)", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        try {
            initMpv()
            started = true
        } catch (e: Throwable) {
            Log.e(TAG, "mpv init failed", e)
            Toast.makeText(this, "mpv init failed: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val surface = SurfaceView(this).also { it.holder.addCallback(this) }
        val label = TextView(this).apply { text = url }
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(button("-10 s") { MpvPlayer.command("seek", "-10", "relative") })
            addView(button("Play / Pause") { MpvPlayer.command("cycle", "pause") })
            addView(button("+10 s") { MpvPlayer.command("seek", "10", "relative") })
            addView(button("Back") { finish() })
        }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(label)
                addView(surface, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
                addView(controls)
            },
        )
    }

    /** Same order as BaseMPVView.initialize(): create, options, init, then post-init options and observers. */
    private fun initMpv() {
        val configDir = File(filesDir, "mpv").apply { mkdirs() }
        // Aniyomi copies cacert.pem from assets into the mpv config dir and points tls-ca-file at it.
        val caFile = File(configDir, "cacert.pem")
        assets.open("cacert.pem").use { input ->
            if (!caFile.exists() || caFile.length() != input.available().toLong()) {
                caFile.outputStream().use { input.copyTo(it) }
            }
        }
        File(configDir, "mpv.conf").apply { if (!exists()) createNewFile() }

        val logs = MPVLib.LogObserver { prefix, level, text ->
            Log.println(logPriority(level), TAG, "[$prefix] ${text.trimEnd()}")
        }
        logObserver = logs
        MpvPlayer.addLogObserver(logs)
        val events = newEventObserver()
        eventObserver = events
        MpvPlayer.addObserver(events)

        MpvPlayer.create(this, "v")
        setOpt("config", "yes")
        setOpt("config-dir", configDir.path)
        for (opt in arrayOf("gpu-shader-cache-dir", "icc-cache-dir")) setOpt(opt, cacheDir.path)
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

    private fun logPriority(level: Int) = when {
        level <= MPVLib.mpvLogLevel.MPV_LOG_LEVEL_ERROR -> Log.ERROR
        level <= MPVLib.mpvLogLevel.MPV_LOG_LEVEL_WARN -> Log.WARN
        level <= MPVLib.mpvLogLevel.MPV_LOG_LEVEL_INFO -> Log.INFO
        else -> Log.DEBUG
    }

    private fun newEventObserver() = object : MPVLib.EventObserver {
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
            // time-pos changes constantly; log it once per second.
            if (property == "time-pos") {
                val second = value.toLong()
                if (second == lastLoggedSecond) return
                lastLoggedSecond = second
            }
            Log.i(TAG, "prop $property = $value")
        }

        override fun event(eventId: Int) {
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
            Log.i(TAG, "end-file error text: ${err ?: "none"}")
        }
    }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        setOnClickListener { onClick() }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!started) return
        Log.i(TAG, "surfaceCreated: attaching surface")
        MpvPlayer.attachSurface(holder.surface)
        // Forces mpv to render into the surface even if it would ordinarily not (BaseMPVView.surfaceCreated).
        setOpt("force-window", "yes")
        MpvPlayer.command("loadfile", url!!)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (!started) return
        Log.i(TAG, "surfaceChanged ${width}x$height")
        MpvPlayer.property("android-surface-size", "${width}x$height")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (!started) return
        Log.i(TAG, "surfaceDestroyed: detaching surface")
        MpvPlayer.property("vo", "null")
        setOpt("force-window", "no")
        MpvPlayer.detachSurface()
    }

    override fun onDestroy() {
        if (started) {
            started = false
            logObserver?.let { MpvPlayer.removeLogObserver(it) }
            eventObserver?.let { MpvPlayer.removeObserver(it) }
            MpvPlayer.destroy()
        }
        super.onDestroy()
    }

    private companion object {
        const val TAG = "TsuzukiMpv"
    }
}
