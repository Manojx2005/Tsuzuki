package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerState
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import java.util.UUID

internal fun Context.supportsAnimePictureInPicture(): Boolean =
    packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

internal enum class AnimePipAction {
    REWIND,
    PLAY,
    PAUSE,
    FORWARD,
}

internal data class AnimePipControls(val canEnter: Boolean, val actions: List<AnimePipAction>)

internal fun AnimePlayerState.pipControls(enabled: Boolean, inPip: Boolean): AnimePipControls {
    if ((!enabled && !inPip) || loading || failed) return AnimePipControls(false, emptyList())
    val playback = if (playing) AnimePipAction.PAUSE else AnimePipAction.PLAY
    val actions = if (duration > 0) {
        listOf(AnimePipAction.REWIND, playback, AnimePipAction.FORWARD)
    } else {
        listOf(playback)
    }
    return AnimePipControls(enabled && playing, actions)
}

internal class AnimePictureInPicture(
    private val activity: Activity,
    onAction: (AnimePipAction) -> Unit,
) {
    // Commands from a previous activity instance must not control a newly opened video.
    private val actionName = "${activity.packageName}.anime.pip.${UUID.randomUUID()}"
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != actionName || !activity.isInPictureInPictureMode) return
            val action = AnimePipAction.entries.getOrNull(intent.getIntExtra(EXTRA_ACTION, -1)) ?: return
            onAction(action)
        }
    }

    init {
        ContextCompat.registerReceiver(
            activity,
            receiver,
            IntentFilter(actionName),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    fun update(controls: AnimePipControls) {
        activity.setPictureInPictureParams(params(controls))
    }

    fun enterOnUserLeave(controls: AnimePipControls) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || !controls.canEnter) return
        try {
            activity.enterPictureInPictureMode(params(controls))
        } catch (e: IllegalStateException) {
            logcat(LogPriority.WARN, e) { "Could not enter anime picture-in-picture" }
        }
    }

    fun close() {
        activity.unregisterReceiver(receiver)
    }

    private fun params(controls: AnimePipControls): PictureInPictureParams =
        PictureInPictureParams.Builder().apply {
            setActions(controls.actions.map(::remoteAction))
            val bounds = Rect()
            if (activity.window.decorView.getGlobalVisibleRect(bounds)) setSourceRectHint(bounds)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setAutoEnterEnabled(controls.canEnter)
            }
        }.build()

    private fun remoteAction(action: AnimePipAction): RemoteAction {
        val icon = when (action) {
            AnimePipAction.REWIND -> android.R.drawable.ic_media_rew
            AnimePipAction.PLAY -> android.R.drawable.ic_media_play
            AnimePipAction.PAUSE -> android.R.drawable.ic_media_pause
            AnimePipAction.FORWARD -> android.R.drawable.ic_media_ff
        }
        val title = when (action) {
            AnimePipAction.REWIND -> activity.getString(R.string.tsz_anime_player_rewind)
            AnimePipAction.PLAY -> activity.stringResource(MR.strings.tsz_widget_listening_play)
            AnimePipAction.PAUSE -> activity.stringResource(MR.strings.action_pause)
            AnimePipAction.FORWARD -> activity.getString(R.string.tsz_anime_player_forward)
        }
        val intent = Intent(actionName)
            .setPackage(activity.packageName)
            .putExtra(EXTRA_ACTION, action.ordinal)
        val pending = PendingIntent.getBroadcast(activity, action.ordinal, intent, PendingIntent.FLAG_IMMUTABLE)
        return RemoteAction(Icon.createWithResource(activity, icon), title, title, pending)
    }

    private companion object {
        const val EXTRA_ACTION = "anime_pip_action"
    }
}
