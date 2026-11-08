package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerContent
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerModel
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.roundToInt

class AnimePlayerActivity : BaseActivity(), SurfaceHolder.Callback {
    private var model: AnimePlayerModel? = null
    private var orientationLocked by mutableStateOf(false)
    private var gestureStartLevel = 0f
    private val audio by lazy { getSystemService(AudioManager::class.java) }

    private val systemBars by lazy { WindowInsetsControllerCompat(window, window.decorView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!TsuzukiAnime.enabled) {
            finish()
            return
        }

        val player = try {
            val factory = viewModelFactory {
                initializer {
                    val original = checkNotNull(AnimePlayback.take(intent))
                    val request = if (original is AnimePlayback.Request.Episode) {
                        original.copy(id = savedInstanceState?.getLong(EPISODE, original.id) ?: original.id)
                    } else {
                        original
                    }
                    AnimePlayerModel(
                        context = applicationContext,
                        request = request,
                        backend = checkNotNull(Injekt.get<AnimePlayerLauncher>().createBackend()),
                        library = Injekt.get(),
                        preferences = Injekt.get(),
                        processScope = Injekt.get(),
                        restoredPosition = savedInstanceState?.takeIf { it.containsKey(POSITION) }?.getDouble(POSITION),
                        restoredPaused = savedInstanceState?.getBoolean(PAUSED) == true,
                    )
                }
            }
            ViewModelProvider(this, factory)[AnimePlayerModel::class.java]
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Anime player could not be opened" }
            Toast.makeText(this, R.string.tsz_anime_player_unavailable, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        model = player
        orientationLocked = savedInstanceState?.getBoolean(ORIENTATION_LOCKED) == true
        applyOrientation()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        systemBars.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        systemBars.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            TachiyomiTheme {
                val state by player.state.collectAsState()
                LaunchedEffect(state.playing) {
                    if (state.playing) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
                AnimePlayerContent(
                    state = state,
                    model = player,
                    onBack = ::finish,
                    onOrientationLock = { locked ->
                        orientationLocked = locked
                        applyOrientation()
                    },
                    orientationLocked = orientationLocked,
                    onLevelGesture = ::adjustLevel,
                    surface = { modifier ->
                        AndroidView(
                            modifier = modifier,
                            factory = { context ->
                                SurfaceView(context).also { it.holder.addCallback(this@AnimePlayerActivity) }
                            },
                        )
                    },
                )
            }
        }
    }

    private fun applyOrientation() {
        requestedOrientation = if (orientationLocked) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    private fun adjustLevel(volume: Boolean, delta: Float, start: Boolean): Float {
        if (volume) {
            val manager = audio ?: return 0f
            val stream = AudioManager.STREAM_MUSIC
            val maximum = manager.getStreamMaxVolume(stream).coerceAtLeast(1)
            if (start) gestureStartLevel = manager.getStreamVolume(stream).toFloat() / maximum
            val target = ((gestureStartLevel + delta).coerceIn(0f, 1f) * maximum).roundToInt()
            if (!manager.isVolumeFixed && target != manager.getStreamVolume(stream)) {
                manager.setStreamVolume(stream, target, 0)
            }
            return manager.getStreamVolume(stream).toFloat() / maximum
        }

        val attrs = window.attributes
        if (start) {
            gestureStartLevel = attrs.screenBrightness.takeIf { it >= 0f }
                ?: (Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f)
        }
        attrs.screenBrightness = (gestureStartLevel + delta).coerceIn(0.05f, 1f)
        window.attributes = attrs
        return attrs.screenBrightness
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            model?.setForeground(true)
            systemBars.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        model?.attach(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        model?.resize(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        model?.detach()
    }

    override fun onResume() {
        super.onResume()
        model?.setForeground(true)
    }

    override fun onPause() {
        model?.setForeground(false)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        model?.let { player ->
            val state = player.state.value
            player.targetEpisodeId?.let { outState.putLong(EPISODE, it) }
            if (state.episode != null) outState.putDouble(POSITION, state.position)
            outState.putBoolean(PAUSED, !player.shouldResume)
        }
        outState.putBoolean(ORIENTATION_LOCKED, orientationLocked)
    }

    override fun onDestroy() {
        model?.detach()
        model = null
        super.onDestroy()
    }

    private companion object {
        const val EPISODE = "anime_episode"
        const val POSITION = "anime_position"
        const val PAUSED = "anime_paused"
        const val ORIENTATION_LOCKED = "anime_orientation_locked"
    }
}
