package eu.kanade.tachiyomi.tsuzuki.anime.player

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog

/** What the player is doing, for the media session (AY4). No Android types, so it can be tested directly. */
internal enum class AnimeSessionPlayback { PLAYING, PAUSED, BUFFERING, ERROR, STOPPED }

internal data class AnimeSessionState(
    val playback: AnimeSessionPlayback,
    val positionMs: Long,
    val durationMs: Long,
    val canPrevious: Boolean,
    val canNext: Boolean,
    val canSeek: Boolean,
    val speed: Float = 1f,
)

internal fun animePlaybackState(state: AnimePlayerState): AnimeSessionState {
    val playback = when {
        state.failed -> AnimeSessionPlayback.ERROR
        state.ended -> AnimeSessionPlayback.STOPPED
        state.loading || state.buffering -> AnimeSessionPlayback.BUFFERING
        state.playing -> AnimeSessionPlayback.PLAYING
        else -> AnimeSessionPlayback.PAUSED
    }
    val ready = !state.loading && !state.failed && state.duration > 0
    return AnimeSessionState(
        playback = playback,
        positionMs = (state.position.coerceAtLeast(0.0) * 1000).toLong(),
        durationMs = (state.duration.coerceAtLeast(0.0) * 1000).toLong(),
        canPrevious = state.previous != null,
        canNext = state.next != null,
        canSeek = ready,
        speed = state.settings.speed,
    )
}

/** "Anime — Episode", the title mpv and the session show (PiP, the system media controls). */
internal fun animeMediaTitle(anime: String, episode: String?): String =
    listOf(anime, episode.orEmpty()).map(String::trim).filter(String::isNotEmpty).joinToString(" — ")

internal enum class AnimeKeyAction { TOGGLE, PLAY, PAUSE, BACK_10, FORWARD_10, NEXT, PREVIOUS }

/** Hardware and media keys the player handles itself (AY4); anything else goes to the system. */
internal fun animeKeyAction(keyCode: Int): AnimeKeyAction? = when (keyCode) {
    KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> AnimeKeyAction.TOGGLE
    KeyEvent.KEYCODE_MEDIA_PLAY -> AnimeKeyAction.PLAY
    KeyEvent.KEYCODE_MEDIA_PAUSE -> AnimeKeyAction.PAUSE
    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> AnimeKeyAction.BACK_10
    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> AnimeKeyAction.FORWARD_10
    KeyEvent.KEYCODE_MEDIA_NEXT -> AnimeKeyAction.NEXT
    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> AnimeKeyAction.PREVIOUS
    else -> null
}

internal fun AnimePlayerModel.perform(action: AnimeKeyAction) {
    when (action) {
        AnimeKeyAction.TOGGLE -> setPlaying(!state.value.playing)
        AnimeKeyAction.PLAY -> setPlaying(true)
        AnimeKeyAction.PAUSE -> setPlaying(false)
        AnimeKeyAction.BACK_10 -> seekBy(-10)
        AnimeKeyAction.FORWARD_10 -> seekBy(10)
        AnimeKeyAction.NEXT -> if (state.value.next != null) next()
        AnimeKeyAction.PREVIOUS -> if (state.value.previous != null) previous()
    }
}

/**
 * The player's `MediaSessionCompat` (AY4): system media controls, Bluetooth/headset buttons and the PiP window
 * reach the model through it. Active while the activity is started or in PiP; no notification or background
 * playback (later card). Media keys sent to the activity go through [animeKeyAction] instead, never both.
 */
internal class AnimeMediaSession(context: Context, private val model: () -> AnimePlayerModel?) {
    private val session = MediaSessionCompat(context, AnimeLog.TAG).apply {
        setCallback(
            object : MediaSessionCompat.Callback() {
                override fun onPlay() = act(AnimeKeyAction.PLAY)
                override fun onPause() = act(AnimeKeyAction.PAUSE)
                override fun onSkipToNext() = act(AnimeKeyAction.NEXT)
                override fun onSkipToPrevious() = act(AnimeKeyAction.PREVIOUS)
                override fun onFastForward() = act(AnimeKeyAction.FORWARD_10)
                override fun onRewind() = act(AnimeKeyAction.BACK_10)
                override fun onSeekTo(pos: Long) {
                    model()?.seekTo(pos / 1000.0)
                }
            },
        )
    }
    private var lastState: AnimeSessionState? = null
    private var lastTitle: String? = null
    private var lastDurationMs = -1L

    private fun act(action: AnimeKeyAction) {
        model()?.perform(action)
    }

    var active: Boolean
        get() = session.isActive
        set(value) {
            if (session.isActive != value) session.isActive = value
        }

    /** Pushes a new state; the position alone is pushed at most once a second (the session extrapolates). */
    fun update(state: AnimePlayerState) {
        val next = animePlaybackState(state)
        val previous = lastState
        val onlyPositionMoved = previous != null &&
            next.copy(positionMs = 0) == previous.copy(positionMs = 0) &&
            kotlin.math.abs(next.positionMs - previous.positionMs) < 1_000
        if (!onlyPositionMoved) {
            lastState = next
            session.setPlaybackState(next.toPlaybackState())
        }
        val title = animeMediaTitle(state.title, state.episode?.name)
        if (title != lastTitle || next.durationMs != lastDurationMs) {
            lastTitle = title
            lastDurationMs = next.durationMs
            session.setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, state.title)
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, state.episode?.name.orEmpty())
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, next.durationMs)
                    .build(),
            )
        }
    }

    fun release() {
        session.isActive = false
        session.release()
    }

    private fun AnimeSessionState.toPlaybackState(): PlaybackStateCompat {
        val code = when (playback) {
            AnimeSessionPlayback.PLAYING -> PlaybackStateCompat.STATE_PLAYING
            AnimeSessionPlayback.PAUSED -> PlaybackStateCompat.STATE_PAUSED
            AnimeSessionPlayback.BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            AnimeSessionPlayback.ERROR -> PlaybackStateCompat.STATE_ERROR
            AnimeSessionPlayback.STOPPED -> PlaybackStateCompat.STATE_STOPPED
        }
        var actions = PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE
        if (canSeek) {
            actions = actions or PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_FAST_FORWARD or
                PlaybackStateCompat.ACTION_REWIND
        }
        if (canNext) actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        if (canPrevious) actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        val rate = if (playback == AnimeSessionPlayback.PLAYING) speed else 0f
        return PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(code, positionMs, rate)
            .build()
    }
}
