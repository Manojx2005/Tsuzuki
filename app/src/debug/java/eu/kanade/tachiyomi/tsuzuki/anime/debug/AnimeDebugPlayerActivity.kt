package eu.kanade.tachiyomi.tsuzuki.anime.debug

import android.app.Activity
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import eu.kanade.tsuzuki.animeplayer.MpvPlayer

/**
 * Debug-only libmpv test player (N22). Plays the `url` intent extra with play/pause, seek +-10 s and back.
 * Not part of preview or release builds; `anime-player` is a debugImplementation dependency.
 */
class AnimeDebugPlayerActivity : Activity(), SurfaceHolder.Callback {

    private var started = false
    private var url: String? = null

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
            MpvPlayer.create(this)
            MpvPlayer.option("vo", "gpu")
            MpvPlayer.option("gpu-context", "android")
            MpvPlayer.option("opengl-es", "yes")
            MpvPlayer.option("hwdec", "mediacodec-copy")
            MpvPlayer.option("ao", "audiotrack,opensles")
            MpvPlayer.option("ytdl", "no")
            MpvPlayer.option("force-window", "no")
            MpvPlayer.option("idle", "yes")
            MpvPlayer.init()
            started = true
        } catch (e: Throwable) {
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

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        setOnClickListener { onClick() }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!started) return
        MpvPlayer.attachSurface(holder.surface)
        MpvPlayer.option("force-window", "yes")
        MpvPlayer.command("loadfile", url!!)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // mpv reads the surface size from the attached surface.
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (!started) return
        MpvPlayer.option("force-window", "no")
        MpvPlayer.detachSurface()
    }

    override fun onDestroy() {
        if (started) {
            started = false
            MpvPlayer.destroy()
        }
        super.onDestroy()
    }
}
