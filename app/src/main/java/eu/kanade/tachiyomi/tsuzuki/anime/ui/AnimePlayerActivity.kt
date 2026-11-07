package eu.kanade.tachiyomi.tsuzuki.anime.ui

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
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimePlaybackProgress
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimePlayerActivity : Activity(), SurfaceHolder.Callback {
    private var player: AnimePlayerBackend? = null
    private var episodeId: Long? = null
    private val playbackProgress = AnimePlaybackProgress()

    @Volatile
    private var visible = false
    private var resumeAfterPause = false
    private var status: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!TsuzukiAnime.enabled) {
            finish()
            return
        }
        val request = AnimePlayback.take(intent)
        val backend = Injekt.get<AnimePlayerLauncher>().createBackend()
        if (request == null || backend == null) {
            Toast.makeText(this, R.string.tsz_anime_player_unavailable, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        episodeId = request.episode?.id
        try {
            backend.initialize(this, request, playbackListener())
            player = backend
        } catch (e: Exception) {
            backend.close()
            Log.e(TAG, "Could not initialize anime playback", e)
            Toast.makeText(this, MR.strings.tsz_anime_debug_playback_error.resourceId, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val surface = SurfaceView(this).also { it.holder.addCallback(this) }
        val label = TextView(this).apply {
            text = request.video.videoTitle.ifBlank { getString(MR.strings.tsz_widget_listening_play.resourceId) }
        }
        status = label
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(button("-10 s") { backend.seek(-10) })
            addView(button("Play / Pause") { backend.togglePause() })
            addView(button("+10 s") { backend.seek(10) })
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

    private fun playbackListener() = object : AnimePlayerBackend.Listener {
        override fun onPosition(seconds: Double) {
            if (visible && episodeId != null) playbackProgress.onPosition(seconds)
        }

        override fun onDuration(seconds: Double) {
            if (episodeId != null) playbackProgress.onDuration(seconds)
        }

        override fun onSeek() {
            playbackProgress.skipNextIncrement()
        }

        override fun onError() {
            runOnUiThread { status?.setText(MR.strings.tsz_anime_debug_playback_error.resourceId) }
        }
    }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        setOnClickListener { onClick() }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        player?.attach(holder.surface, playbackProgress.resumePosition())
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        player?.resize(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        player?.detach()
    }

    override fun onResume() {
        super.onResume()
        visible = true
        playbackProgress.skipNextIncrement()
        if (resumeAfterPause) player?.resume()
    }

    override fun onPause() {
        visible = false
        if (episodeId != null) resumeAfterPause = player?.pause() == true
        savePlaybackProgress()
        super.onPause()
    }

    private fun savePlaybackProgress() {
        val id = episodeId ?: return
        val checkpoint = playbackProgress.checkpoint() ?: return
        Injekt.get<CoroutineScope>().launch {
            try {
                Injekt.get<AnimeLibraryStore>().saveProgress(id, checkpoint)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Could not save episode progress", e)
            }
        }
    }

    override fun onDestroy() {
        player?.close()
        player = null
        super.onDestroy()
    }

    private companion object {
        const val TAG = "TsuzukiAnimePlayer"
    }
}
