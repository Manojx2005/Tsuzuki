package eu.kanade.tachiyomi.tsuzuki.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTokens
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.tts.TtsNowPlaying
import eu.kanade.tachiyomi.tsuzuki.tts.TtsNowPlayingState
import eu.kanade.tachiyomi.tsuzuki.tts.TtsStatus
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.service.TtsPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.atomic.AtomicBoolean
import androidx.glance.appwidget.action.actionStartActivity as actionStartActivityIntent

/**
 * The "Listening" home-screen widget (board 07-system "Widgets"): the book TTS is reading, its chapter and a
 * play/pause button. The button sends the service's own PAUSE / RESUME action, so it only appears while a session
 * is loaded; with nothing loaded the widget says so and a tap opens the app. Dot draws black / red monospace,
 * Colour navy / gold, as the Reading widget does. Left out: the replay button and sleep timer line (no data).
 */
class TsuzukiListeningWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ui = Injekt.get<UiPreferences>()
        val getManga = Injekt.get<GetManga>()
        val tokens = widgetTheme(ui)
        val grey = tokens.isDot && !ui.tsuzukiDotColourCovers.get()
        val labels = ListeningLabels(
            listening = context.stringResource(MR.strings.tsz_widget_listening),
            nothing = context.stringResource(MR.strings.tsz_widget_listening_nothing),
            play = context.stringResource(MR.strings.tsz_widget_listening_play),
            pause = context.stringResource(MR.strings.tsz_widget_listening_pause),
        )

        provideContent {
            val now by TtsNowPlaying.state.collectAsState()
            val cover by produceState<Bitmap?>(initialValue = null, now.mangaId, grey) {
                value = null
                value = if (now.mangaId > 0) {
                    withContext(Dispatchers.IO) {
                        getManga.await(now.mangaId)?.let { loadWidgetCover(context, it.asMangaCover(), grey) }
                    }
                } else {
                    null
                }
            }
            ListeningWidgetContent(context, tokens, labels, now, cover)
        }
    }
}

class TsuzukiListeningWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TsuzukiListeningWidget()
}

/** Pushes a widget refresh when what TTS is reading changes (book, chapter, play/pause), at most every 500 ms. */
object TsuzukiListeningWidgetUpdater {
    private val started = AtomicBoolean(false)

    @OptIn(FlowPreview::class)
    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            TtsNowPlaying.state.debounce(500).collect {
                runCatching { TsuzukiListeningWidget().updateAll(context) }
            }
        }
    }
}

private data class ListeningLabels(val listening: String, val nothing: String, val play: String, val pause: String)

@Composable
private fun ListeningWidgetContent(
    context: Context,
    tokens: TsuzukiTokens,
    labels: ListeningLabels,
    now: TtsNowPlayingState,
    cover: Bitmap?,
) {
    val compact = LocalSize.current.height < 100.dp
    val face = if (tokens.isDot) FontFamily.Monospace else FontFamily.SansSerif
    fun up(s: String) = if (tokens.isDot) s.uppercase() else s
    val active = now.isActive
    val playing = now.status == TtsStatus.PLAYING
    val open = if (active && now.mangaId > 0 && now.chapterId > 0) {
        actionStartActivityIntent(ReaderActivity.newIntent(context, now.mangaId, now.chapterId))
    } else {
        actionStartActivity<MainActivity>()
    }
    val titleStyle = TextStyle(
        color = ColorProvider(tokens.dotOn),
        fontSize = if (compact) 14.sp else 16.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = face,
    )

    Row(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(tokens.background)
            .padding(if (compact) 5.dp else 10.dp)
            .clickable(open),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            GlanceModifier.fillMaxSize().cornerRadius(18.dp).background(tokens.tile)
                .padding(if (compact) 5.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (active && cover != null) {
                Image(
                    provider = ImageProvider(cover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = if (compact) {
                        GlanceModifier.size(width = 32.dp, height = 44.dp).cornerRadius(6.dp)
                    } else {
                        GlanceModifier.size(width = 48.dp, height = 72.dp).cornerRadius(8.dp)
                    },
                )
                Spacer(GlanceModifier.width(12.dp))
            }
            Column(GlanceModifier.defaultWeight()) {
                Text(up(labels.listening), maxLines = 1, style = small(tokens.mutedText, face))
                Text(
                    up(if (active) now.bookTitle else labels.nothing),
                    maxLines = if (compact) 1 else 2,
                    style = titleStyle,
                )
                if (active && now.chapterName.isNotBlank()) {
                    Text(
                        up(now.chapterName),
                        maxLines = 1,
                        style = TextStyle(color = ColorProvider(tokens.mutedText), fontSize = 12.sp, fontFamily = face),
                    )
                }
            }
            if (active) {
                Spacer(GlanceModifier.width(8.dp))
                Box(
                    GlanceModifier
                        .size(44.dp)
                        .cornerRadius(22.dp)
                        .background(tokens.accent)
                        .clickable(actionStartService(transport(context, if (playing) "PAUSE" else "RESUME"))),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider = ImageProvider(
                            if (playing) R.drawable.ic_pause_24dp else R.drawable.ic_play_arrow_24dp,
                        ),
                        contentDescription = if (playing) labels.pause else labels.play,
                        colorFilter = ColorFilter.tint(ColorProvider(tokens.onAccent)),
                        modifier = GlanceModifier.size(28.dp),
                    )
                }
            }
        }
    }
}

/** An intent for the service's own PAUSE / RESUME actions (the same ones its notification buttons send). */
private fun transport(context: Context, action: String) =
    Intent(context, TtsPlaybackService::class.java).setAction("${TtsPlaybackService::class.java.name}.$action")

private fun small(color: Color, face: FontFamily) =
    TextStyle(color = ColorProvider(color), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = face)
