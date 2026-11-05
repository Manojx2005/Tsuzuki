package eu.kanade.tachiyomi.tsuzuki.anime.player

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import `is`.xyz.mpv.MPVLib
import java.io.File
import java.security.KeyStore
import java.util.Base64

/** Minimal SurfaceView host adapted from MIT BaseMPVView/MPVView
see MIT-Aniyomi-MPV.txt. */
class AnimePlayerActivity : ComponentActivity(), SurfaceHolder.Callback {
    private lateinit var surface: SurfaceView
    private lateinit var status: TextView
    private lateinit var seek: SeekBar
    private var initialized = false
    private var loaded = false
    private var dragging = false

    @Volatile
    private var lastMessage = ""
    private val playerLog = MPVLib.LogObserver { prefix, level, message ->
        if (level <= 30) {
            val safeMessage = message.replace(Regex("https?://\\S+"), "[stream URL]").trim().take(300)
            lastMessage = "$prefix: $safeMessage"
            Log.w("AnimeSpikeMPV", lastMessage)
        }
    }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var audio: AudioManager
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
        .setOnAudioFocusChangeListener { change ->
            if (change != AudioManager.AUDIOFOCUS_GAIN && initialized) MPVLib.setPropertyBoolean("pause", true)
        }.build()
    private val refresh = object : Runnable {
        override fun run() {
            if (!initialized) return
            val time = MPVLib.getPropertyDouble("time-pos") ?: 0.0
            val duration = MPVLib.getPropertyDouble("duration") ?: 0.0
            if (!dragging) seek.progress = if (duration > 0) (time / duration * 1000).toInt() else 0
            status.text = "${intent.getStringExtra("title")}\n${time.toInt()} / ${duration.toInt()} s | " +
                "video=${MPVLib.getPropertyString("video-codec")} audio=${MPVLib.getPropertyString("audio-codec")} | " +
                "${MPVLib.getPropertyInt("width")}×${MPVLib.getPropertyInt("height")} | " +
                "hwdec=${MPVLib.getPropertyString("hwdec-current")} " +
                "dropped=${MPVLib.getPropertyInt("frame-drop-count")}\n$lastMessage"
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        audio = getSystemService(AudioManager::class.java)
        volumeControlStream = AudioManager.STREAM_MUSIC
        val url = intent.getStringExtra("url") ?: return finish()
        require(url.startsWith("https://") || url.startsWith("http://"))
        MPVLib.addLogObserver(playerLog)
        MPVLib.create(applicationContext, "warn")
        option("config", "no")
        option("ytdl", "no")
        option("vo", "gpu")
        option("gpu-context", "android")
        option("opengl-es", "yes")
        option("hwdec", "mediacodec-copy")
        option("ao", "audiotrack,opensles")
        option("tls-verify", "yes")
        option("tls-ca-file", certificateBundle().absolutePath)
        option("demuxer-max-bytes", "67108864")
        option("force-window", "no")
        option("idle", "yes")
        val headers = intent.getStringArrayListExtra("headers").orEmpty()
        require(headers.none { '\r' in it || '\n' in it })
        // libmpv's option setter takes the base string-list option, with list escaping.
        if (headers.isNotEmpty()) {
            option("http-header-fields", headers.joinToString(",") { it.replace("\\", "\\\\").replace(",", "\\,") })
        }
        MPVLib.init()
        initialized = true
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 48, 16, 24)
        }
        status = TextView(this)
        column.addView(status)
        surface = SurfaceView(this)
        column.addView(surface, LinearLayout.LayoutParams(-1, 0, 1f))
        seek = SeekBar(this).apply {
            max = 1000
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar: SeekBar) {
                    dragging = true
                }
                override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) = Unit
                override fun onStopTrackingTouch(bar: SeekBar) {
                    MPVLib.command(arrayOf("seek", (bar.progress / 10.0).toString(), "absolute-percent"))
                    dragging = false
                }
            })
        }
        column.addView(seek)
        column.addView(
            Button(this).apply {
                text = "Play / Pause"
                setOnClickListener {
                    if (audio.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        MPVLib.command(arrayOf("cycle", "pause"))
                    }
                }
            },
        )
        column.addView(
            Button(this).apply {
                text = "Back"
                setOnClickListener { finish() }
            },
        )
        surface.holder.addCallback(this)
        setContentView(column)
        handler.post(refresh)
    }

    private fun option(name: String, value: String) {
        check(MPVLib.setOptionString(name, value) >= 0) { "mpv rejected option: $name" }
    }

    private fun certificateBundle(): File {
        val store = KeyStore.getInstance("AndroidCAStore").apply { load(null) }
        return File(cacheDir, "anime-ca.pem").apply {
            bufferedWriter().use { output ->
                store.aliases().toList().forEach { alias ->
                    output.appendLine("-----BEGIN CERTIFICATE-----")
                    val encoded = Base64.getMimeEncoder(64, byteArrayOf(10))
                        .encodeToString(store.getCertificate(alias).encoded)
                    output.appendLine(encoded)
                    output.appendLine("-----END CERTIFICATE-----")
                }
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!initialized) return
        MPVLib.attachSurface(holder.surface)
        option("force-window", "yes")
        if (!loaded) {
            loaded = true
            MPVLib.command(arrayOf("loadfile", intent.getStringExtra("url")!!))
            val hasFocus = audio.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            MPVLib.setPropertyBoolean("pause", !hasFocus)
        } else {
            MPVLib.setPropertyString("vo", "gpu")
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (initialized) MPVLib.setPropertyString("android-surface-size", "${width}x$height")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (!initialized) return
        MPVLib.setPropertyString("vo", "null")
        option("force-window", "no")
        MPVLib.detachSurface()
    }

    override fun onPause() {
        if (initialized) MPVLib.setPropertyBoolean("pause", true)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        if (initialized) {
            surface.holder.removeCallback(this)
            MPVLib.destroy()
            MPVLib.removeLogObserver(playerLog)
            initialized = false
        }
        audio.abandonAudioFocusRequest(focus)
        super.onDestroy()
    }
}
