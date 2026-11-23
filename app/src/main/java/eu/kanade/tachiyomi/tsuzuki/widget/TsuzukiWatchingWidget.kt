package eu.kanade.tachiyomi.tsuzuki.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
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
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
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
import eu.kanade.presentation.tsuzuki.anime.RESUME_ANIME_EPISODE_ID
import eu.kanade.presentation.tsuzuki.anime.SAVED_ANIME_ID
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimeCatalogueActivity
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.history.anime.model.AnimeHistoryDisplay
import tachiyomi.domain.history.anime.repository.AnimeHistoryRepository
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.NumberFormat
import java.time.LocalDate
import androidx.glance.appwidget.action.actionStartActivity as actionStartActivityIntent

class TsuzukiWatchingWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        if (!TsuzukiAnime.enabled) return
        val tokens = widgetTheme(Injekt.get<UiPreferences>())
        val history = Injekt.get<AnimeHistoryRepository>()
        val labels = WatchingLabels(
            heading = context.stringResource(MR.strings.tsz_anime_continue_watching),
            resume = context.stringResource(MR.strings.action_resume),
            empty = context.getString(R.string.tsz_watching_widget_empty),
        )
        val episodeNumbers = NumberFormat.getNumberInstance(context.resources.configuration.locales[0]).apply {
            maximumFractionDigits = 3
            isGroupingUsed = false
        }

        val weekLabel = context.getString(R.string.tsz_anime_watch_stats_week)
        val monthLabel = context.getString(R.string.tsz_anime_watch_stats_month)
        val duration = MeasureFormat.getInstance(
            context.resources.configuration.locales[0],
            MeasureFormat.FormatWidth.SHORT,
        )

        provideContent {
            // AP-16: a tall widget keeps room at the bottom for watch time, like Continue Reading's stats row.
            val height = LocalSize.current.height.value
            val tall = height >= WATCHING_WIDGET_TALL_DP
            val stats by remember(history, tall) {
                if (tall) {
                    history.subscribeWatchStats(LocalDate.now()).catch { error ->
                        logcat(LogPriority.WARN, error) { "Could not load watch time for the widget" }
                    }
                } else {
                    flowOf(null)
                }
            }.collectAsState(initial = null)
            val watch = stats?.takeIf { tall && it.hasWatchData }
            val resumeHeight = if (watch != null) height - WATCHING_STATS_ROW_DP else height
            val candidates by remember(history) {
                history.subscribeHistory()
                    .map { selectWatchingWidgetItems(it, MAX_WATCHING_ROWS) }
                    .catch { error ->
                        logcat(LogPriority.WARN, error) { "Could not load the Continue Watching widget" }
                        emit(emptyList())
                    }
            }.collectAsState(initial = emptyList())
            val items = candidates.take(watchingWidgetSlots(resumeHeight))
            val imageUrls = items.associate { it.episodeId to (it.previewUrl ?: it.thumbnailUrl) }
            val covers by produceState<Map<Long, Bitmap?>>(emptyMap(), imageUrls) {
                value = withContext(Dispatchers.IO) {
                    imageUrls.mapValues { (_, url) ->
                        url?.let { loadWidgetCover(context, it, tokens.isDot, width = 160, height = 90) }
                    }
                }
            }
            Column(
                GlanceModifier.fillMaxSize().appWidgetBackground().cornerRadius(24.dp)
                    .background(tokens.background).padding(8.dp),
            ) {
                if (items.isEmpty()) {
                    EmptyWatchingWidget(tokens, labels)
                }
                items.forEachIndexed { index, item ->
                    if (index > 0) Spacer(GlanceModifier.height(6.dp))
                    val episode = item.episodeNumber.takeIf { it.isFinite() && it >= 0 }?.let {
                        context.getString(R.string.tsz_anime_history_episode, episodeNumbers.format(it))
                    } ?: item.episodeName
                    WatchingWidgetRow(
                        context = context,
                        tokens = tokens,
                        labels = labels,
                        item = item,
                        episodeLabel = episode,
                        cover = covers[item.episodeId],
                        first = index == 0,
                    )
                }
                if (watch != null) {
                    Spacer(GlanceModifier.defaultWeight())
                    Row(GlanceModifier.fillMaxWidth().height(WATCHING_STATS_ROW_DP.dp - 8.dp)) {
                        WatchTimeTile(
                            tokens,
                            weekLabel,
                            duration.watchTime(watch.weekSeconds),
                            GlanceModifier.defaultWeight(),
                        )
                        Spacer(GlanceModifier.width(6.dp))
                        WatchTimeTile(
                            tokens,
                            monthLabel,
                            duration.watchTime(watch.monthSeconds),
                            GlanceModifier.defaultWeight(),
                        )
                    }
                }
            }
        }
    }
}

internal fun setWatchingWidgetAvailable(context: Context, available: Boolean) {
    val component = ComponentName(context, TsuzukiWatchingWidgetReceiver::class.java)
    val packageManager = context.packageManager
    val isEnabled =
        packageManager.getComponentEnabledSetting(component) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    if (available == isEnabled) return
    val state = if (available) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } else {
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    }
    packageManager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
}

class TsuzukiWatchingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TsuzukiWatchingWidget()
}

private const val WATCHING_WIDGET_TALL_DP = 244f
private const val WATCHING_STATS_ROW_DP = 84f
private const val MAX_WATCHING_ROWS = 4

/** "3 h 20 min" style watch time, as Stats shows it. */
internal fun MeasureFormat.watchTime(seconds: Long): String {
    val minutes = seconds / 60
    return when {
        minutes < 60 -> formatMeasures(Measure(minutes, MeasureUnit.MINUTE))
        minutes % 60 == 0L -> formatMeasures(Measure(minutes / 60, MeasureUnit.HOUR))
        else -> formatMeasures(Measure(minutes / 60, MeasureUnit.HOUR), Measure(minutes % 60, MeasureUnit.MINUTE))
    }
}

@Composable
private fun WatchTimeTile(tokens: TsuzukiTokens, label: String, value: String, modifier: GlanceModifier) {
    Column(
        modifier.fillMaxHeight().cornerRadius(18.dp).background(tokens.tile).padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(tokens.widgetCase(label), style = tokens.watchingText(tokens.mutedText, 11, bold = true))
        Text(tokens.widgetCase(value), maxLines = 1, style = tokens.watchingText(tokens.dotOn, 18, bold = true))
    }
}

internal fun watchingWidgetSlots(heightDp: Float): Int = ((heightDp - 10) / 76).toInt().coerceIn(1, MAX_WATCHING_ROWS)

internal fun selectWatchingWidgetItems(history: List<AnimeHistoryDisplay>, limit: Int): List<AnimeHistoryDisplay> =
    history.asSequence()
        .filter { !it.seen && it.position > 0 && (it.duration <= 0 || it.position < it.duration) }
        .sortedByDescending { it.lastSeen.time }
        .distinctBy { it.animeId }
        .take(limit.coerceIn(1, MAX_WATCHING_ROWS))
        .toList()

internal suspend fun refreshWatchingWidget(context: Context) {
    if (TsuzukiAnime.enabled) TsuzukiWatchingWidget().updateAll(context)
}

private data class WatchingLabels(val heading: String, val resume: String, val empty: String)

@Composable
private fun EmptyWatchingWidget(tokens: TsuzukiTokens, labels: WatchingLabels) {
    Column(
        GlanceModifier.fillMaxSize().cornerRadius(18.dp).background(tokens.tile)
            .padding(12.dp).clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tokens.widgetCase(labels.heading), style = tokens.watchingText(tokens.dotOn, 14, bold = true))
        Text(tokens.widgetCase(labels.empty), style = tokens.watchingText(tokens.mutedText, 12))
    }
}

@Composable
private fun WatchingWidgetRow(
    context: Context,
    tokens: TsuzukiTokens,
    labels: WatchingLabels,
    item: AnimeHistoryDisplay,
    episodeLabel: String,
    cover: Bitmap?,
    first: Boolean,
) {
    val destination = Intent(context, AnimeCatalogueActivity::class.java)
        .setData("tsuzuki-widget://watch/${item.animeId}/${item.episodeId}".toUri())
        .putExtra(SAVED_ANIME_ID, item.animeId)
        .putExtra(RESUME_ANIME_EPISODE_ID, item.episodeId)
    val open = actionStartActivityIntent(destination)

    Row(
        GlanceModifier.fillMaxWidth().height(70.dp).cornerRadius(18.dp).background(tokens.tile)
            .padding(horizontal = 8.dp).clickable(open),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val thumbnailWidth = if (LocalSize.current.width < 280.dp) 76.dp else 96.dp
        Box(
            GlanceModifier.size(width = thumbnailWidth, height = 54.dp)
                .cornerRadius(8.dp).background(tokens.track),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (cover != null) {
                Image(
                    provider = ImageProvider(cover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.fillMaxSize(),
                )
            }
            if (item.duration > item.position) {
                WidgetProgress((item.position.toDouble() / item.duration).toFloat(), tokens)
            }
        }
        Spacer(GlanceModifier.width(8.dp))
        Column(GlanceModifier.defaultWeight()) {
            if (first) {
                Text(tokens.widgetCase(labels.heading), maxLines = 1, style = tokens.watchingText(tokens.mutedText, 10))
            }
            Text(
                tokens.widgetCase("${item.title} · $episodeLabel"),
                maxLines = 2,
                style = tokens.watchingText(tokens.dotOn, 13, bold = true),
            )
            if (item.duration > item.position) {
                val left = DateUtils.formatElapsedTime(item.duration - item.position)
                Text(
                    tokens.widgetCase(context.getString(R.string.tsz_anime_history_remaining, left)),
                    maxLines = 1,
                    style = tokens.watchingText(tokens.mutedText, 11),
                )
            }
        }
        Spacer(GlanceModifier.width(6.dp))
        Box(
            GlanceModifier.size(36.dp).cornerRadius(18.dp).background(tokens.accent).clickable(open),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_play_arrow_24dp),
                contentDescription = labels.resume,
                colorFilter = ColorFilter.tint(ColorProvider(tokens.onAccent)),
                modifier = GlanceModifier.size(22.dp),
            )
        }
    }
}

private fun TsuzukiTokens.widgetCase(text: String): String = if (isDot) text.uppercase() else text

private fun TsuzukiTokens.watchingText(color: Color, size: Int, bold: Boolean = false) = TextStyle(
    color = ColorProvider(color),
    fontSize = size.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    fontFamily = if (isDot) FontFamily.Monospace else FontFamily.SansSerif,
)
