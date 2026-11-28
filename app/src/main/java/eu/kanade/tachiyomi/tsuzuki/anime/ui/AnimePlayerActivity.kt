package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.app.PictureInPictureUiState
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.presentation.tsuzuki.reader.applyTsuzukiReaderTransition
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimeMediaSession
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerContent
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerModel
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerPreferences
import eu.kanade.tachiyomi.tsuzuki.anime.player.animeKeyAction
import eu.kanade.tachiyomi.tsuzuki.anime.player.parseAnimeExternalResult
import eu.kanade.tachiyomi.tsuzuki.anime.player.perform
import eu.kanade.tachiyomi.tsuzuki.anime.player.runsOnDown
import eu.kanade.tachiyomi.tsuzuki.anime.player.toIntent
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.roundToInt

class AnimePlayerActivity : BaseActivity(), SurfaceHolder.Callback {
    private var model: AnimePlayerModel? = null
    private var pip: AnimePictureInPicture? = null
    private var inPip by mutableStateOf(false)
    private var enteringPip by mutableStateOf(false)
    private val pipPreference by lazy { AnimePlayerPreferences(Injekt.get()).pictureInPicture }
    private var orientationLocked by mutableStateOf(false)
    private var gestureStartLevel = 0f
    private val audio by lazy { getSystemService(AudioManager::class.java) }
    private var session: AnimeMediaSession? = null
    private var started = false

    /** A6.5: another app is playing the episode; kept across process death so it is not opened twice. */
    private var externalOpen = false
    private val externalPlayer = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        externalOpen = false
        val extras = result.data?.extras
        @Suppress("DEPRECATION")
        model?.onExternalResult(extras?.let { bundle -> parseAnimeExternalResult { bundle.get(it) } })
        finish()
    }

    /** AY4: headphones unplugged or a Bluetooth headset dropped: pause, as every player does. */
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) model?.setPlaying(false)
        }
    }

    private val systemBars by lazy { WindowInsetsControllerCompat(window, window.decorView) }

    override fun finish() {
        super.finish()
        applyTsuzukiReaderTransition(opening = false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Opens and closes like the book reader (owner 2026-11-23: anime motion matched to books).
        applyTsuzukiReaderTransition(opening = true)
        if (!TsuzukiAnime.enabled) {
            finish()
            return
        }

        val restartedProcess = savedInstanceState != null && !processSeen
        processSeen = true
        AnimeLog.i(
            "player restore: state=${savedInstanceState != null} newProcess=$restartedProcess " +
                "position=${savedInstanceState?.takeIf { it.containsKey(POSITION) }?.getDouble(POSITION)}",
        )
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
                        downloads = Injekt.get(),
                        preferences = Injekt.get(),
                        aniSkip = Injekt.get(),
                        processScope = Injekt.get(),
                        restoredPosition = savedInstanceState?.takeIf { it.containsKey(POSITION) }?.getDouble(POSITION),
                        // AY4: back from process death the episode comes back paused where it was.
                        restoredPaused = savedInstanceState?.getBoolean(PAUSED) == true || restartedProcess,
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
        inPip = isInPictureInPictureMode
        session = AnimeMediaSession(this) { model }
        if (supportsAnimePictureInPicture()) {
            pip = AnimePictureInPicture(this, ::handlePipAction)
            window.decorView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                currentPipControls()?.let { pip?.update(it) }
            }
        }
        orientationLocked = savedInstanceState?.getBoolean(ORIENTATION_LOCKED) == true
        externalOpen = savedInstanceState?.getBoolean(EXTERNAL_OPEN) == true
        // No enter animation runs after a recreation, and some launchers skip it: never wait longer than this.
        window.decorView.postDelayed(::enterDone, if (savedInstanceState != null) 0L else ENTER_FALLBACK_MS)
        applyOrientation()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        systemBars.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        systemBars.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            TachiyomiTheme {
                val state by player.state.collectAsState()
                val animeEnabled = TsuzukiAnime.enabled
                val preferPip by pipPreference.collectAsState()
                val pipControls = state.pipControls(animeEnabled && preferPip, animeEnabled && inPip)
                LaunchedEffect(pipControls) { pip?.update(pipControls) }
                LaunchedEffect(state) { session?.update(state) }
                LaunchedEffect(animeEnabled) { if (!animeEnabled) finish() }
                LaunchedEffect(state.external) {
                    val launch = state.external ?: return@LaunchedEffect
                    if (externalOpen) return@LaunchedEffect
                    externalOpen = true
                    try {
                        externalPlayer.launch(launch.toIntent(this@AnimePlayerActivity))
                    } catch (e: ActivityNotFoundException) {
                        AnimeLog.w("no app opens the video externally", e)
                        externalOpen = false
                        Toast.makeText(this@AnimePlayerActivity, R.string.tsz_anime_external_missing, Toast.LENGTH_LONG)
                            .show()
                        player.onExternalUnavailable()
                    }
                }
                LaunchedEffect(state.playing, inPip) {
                    if (state.playing && !inPip) {
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
                    pictureInPicture = inPip || enteringPip,
                    onPictureInPicture = pip?.takeIf { animeEnabled && preferPip }?.let { helper ->
                        {
                            val controls = state.pipControls(enabled = true, inPip = false)
                            if (!state.loading && !state.failed) helper.enter(controls)
                        }
                    },
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!TsuzukiAnime.enabled) {
            finish()
            return
        }
        // Reuse the Android task, but release the previous video and its ViewModel
        // before creating one for the new playback request.
        model?.setForeground(false)
        model?.detach(closing = true)
        model = null
        viewModelStore.clear()
        setIntent(intent)
        recreate()
    }

    private fun currentPipControls(): AnimePipControls? = model?.state?.value?.pipControls(
        enabled = TsuzukiAnime.enabled && pipPreference.get(),
        inPip = TsuzukiAnime.enabled && inPip,
    )

    private fun handlePipAction(action: AnimePipAction) {
        val player = model ?: return
        if (action !in currentPipControls()?.actions.orEmpty()) return
        when (action) {
            AnimePipAction.REWIND -> player.seekBy(-10)
            AnimePipAction.PLAY -> player.setPlaying(true)
            // Keep the existing focus request while paused so PiP can resume from the background.
            AnimePipAction.PAUSE -> player.setPlaying(false, retainAudioFocus = true)
            AnimePipAction.FORWARD -> player.seekBy(10)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        currentPipControls()?.let { pip?.enterOnUserLeave(it) }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip = isInPictureInPictureMode
        enteringPip = false
        updateSessionActive()
    }

    private fun updateSessionActive() {
        session?.active = started || inPip
    }

    /** AY4: keyboard and remote keys. Media keys reach here only when no session took them, so never twice. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val player = model
        val action = animeKeyAction(keyCode)
        if (player == null || action == null || !hasWindowFocus()) return super.onKeyDown(keyCode, event)
        // Held keys: consume every repeat so the system never sees them, but only seeking acts on them.
        if (action.runsOnDown(event.repeatCount)) player.perform(action)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onPictureInPictureUiStateChanged(pipState: PictureInPictureUiState) {
        super.onPictureInPictureUiStateChanged(pipState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enteringPip = pipState.isTransitioningToPip
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

    /*
     * Owner 2026-11-28: "stuck for 4-5 frames, then it starts". The window fades in on open, and the video layer sits
     * below it, so mpv's first (still) frame showed through the resume cover until the fade ended. The surface goes
     * to mpv only once the opening animation is over; the cover is up for that time anyway.
     */
    private var entered = false
    private var waitingSurface: SurfaceHolder? = null
    private var waitingSize: Pair<Int, Int>? = null

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        enterDone()
    }

    private fun enterDone() {
        if (entered) return
        entered = true
        val holder = waitingSurface ?: return
        waitingSurface = null
        model?.attach(holder.surface)
        waitingSize?.let { (width, height) -> model?.resize(width, height) }
        waitingSize = null
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (entered) model?.attach(holder.surface) else waitingSurface = holder
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (entered) model?.resize(width, height) else waitingSize = width to height
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (waitingSurface === holder) {
            waitingSurface = null
            waitingSize = null
            return
        }
        model?.detach(closing = isFinishing)
    }

    override fun onStart() {
        super.onStart()
        model?.setForeground(true)
        started = true
        updateSessionActive()
        ContextCompat.registerReceiver(
            this,
            noisy,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onResume() {
        super.onResume()
        model?.setForeground(true)
    }

    override fun onStop() {
        runCatching { unregisterReceiver(noisy) }
        started = false
        updateSessionActive()
        model?.setForeground(false)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onStop()
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
        outState.putBoolean(EXTERNAL_OPEN, externalOpen)
    }

    override fun onDestroy() {
        session?.release()
        session = null
        pip?.close()
        pip = null
        model?.detach(closing = isFinishing)
        model = null
        super.onDestroy()
    }

    private companion object {
        /** False until a player opens in this process: saved state while still false means the process died. */
        var processSeen = false
        const val EPISODE = "anime_episode"
        const val POSITION = "anime_position"
        const val PAUSED = "anime_paused"
        const val ORIENTATION_LOCKED = "anime_orientation_locked"
        const val EXTERNAL_OPEN = "anime_external_open"
        const val ENTER_FALLBACK_MS = 700L
    }
}
