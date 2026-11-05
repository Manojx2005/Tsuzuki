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
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.tts.TtsNowPlaying
import eu.kanade.tachiyomi.tsuzuki.tts.TtsStatus
import eu.kanade.tachiyomi.ui.main.MainActivity
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

/**
 * The "Listening" home-screen widget (board 07-system "Widgets"): the book TTS is reading, its chapter and a
 * play/pause button. The button sends the service's own PAUSE / RESUME action, so it only appears while a session
 * is loaded; with nothing loaded the widget says so and a tap opens the app. Dot draws black / red monospace,
 * Colour navy / gold, as the Reading widget does. Left out: the replay button and sleep timer line (no data).
 */
class TsuzukiListeningWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ui = Injekt.get<UiPreferences>()
        val getManga = Injekt.get<GetManga>()
        val dot = ui.appTheme.get() == AppTheme.DOT
        val grey = dot && !ui.tsuzukiDotColourCovers.get()
        val labels = ListeningLabels(
            listening = context.stringResource(MR.strings.tsz_widget_listening),
            nothing = context.stringResource(MR.strings.tsz_widget_listening_nothing),
            play = context.stringResource(MR.strings.tsz_widget_listening_play),
            pause = context.stringResource(MR.strings.tsz_widget_listening_pause),
        )

        provideContent {
            val now by TtsNowPlaying.state.collectAsState()
            val cover by produceState<Bitmap?>(initialValue = null, now.mangaId, grey) {
                value = if (now.mangaId > 0) {
                    withContext(Dispatchers.IO) {
                        getManga.await(now.mangaId)?.let { loadWidgetCover(context, it.asMangaCover(), grey) }
                    }
                } else {
                    null
                }
            }
            ListeningWidgetContent(context, dot, labels, now.status, now.bookTitle, now.chapterName, cover)
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
    dot: Boolean,
    labels: ListeningLabels,
    status: TtsStatus,
    book: String,
    chapter: String,
    cover: Bitmap?,
) {
    val background = if (dot) Color(0xFF000000) else Color(0xFF1C2B4A)
    val tile = if (dot) Color(0xFF0D0D0D) else Color(0xFF2A3B60)
    val accent = if (dot) Color(0xFFFF2D2D) else Color(0xFFE9B44C)
    val onAccent = if (dot) Color.White else Color(0xFF1C2B4A)
    val muted = if (dot) Color(0xFF8A8A8A) else Color(0xB8FFFFFF)
    val face = if (dot) FontFamily.Monospace else FontFamily.SansSerif
    fun up(s: String) = if (dot) s.uppercase() else s
    val active = status != TtsStatus.STOPPED
    val playing = status == TtsStatus.PLAYING
    val titleStyle = TextStyle(
        color = ColorProvider(Color.White),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = face,
    )

    Row(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(background)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            GlanceModifier.fillMaxSize().cornerRadius(18.dp).background(tile).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (active && cover != null) {
                Image(
                    provider = ImageProvider(cover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.size(width = 48.dp, height = 72.dp).cornerRadius(8.dp),
                )
                Spacer(GlanceModifier.width(12.dp))
            }
            Column(GlanceModifier.defaultWeight()) {
                Text(up(labels.listening), style = small(muted, face))
                Text(up(if (active) book else labels.nothing), maxLines = 2, style = titleStyle)
                if (active && chapter.isNotBlank()) {
                    Text(
                        up(chapter),
                        maxLines = 1,
                        style = TextStyle(color = ColorProvider(muted), fontSize = 12.sp, fontFamily = face),
                    )
                }
            }
            if (active) {
                Spacer(GlanceModifier.width(8.dp))
                Box(
                    GlanceModifier
                        .size(48.dp)
                        .cornerRadius(24.dp)
                        .background(accent)
                        .clickable(actionStartService(transport(context, if (playing) "PAUSE" else "RESUME"))),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider = ImageProvider(
                            if (playing) R.drawable.ic_pause_24dp else R.drawable.ic_play_arrow_24dp,
                        ),
                        contentDescription = if (playing) labels.pause else labels.play,
                        colorFilter = ColorFilter.tint(ColorProvider(onAccent)),
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
