package eu.kanade.tachiyomi.tsuzuki.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
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
import coil3.executeBlocking
import coil3.imageLoader
import coil3.request.allowHardware
import coil3.toBitmap
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.tsuzuki.ReadingDays
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.coroutines.flow.map
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * The "Reading" home-screen widget (board 07-system "Widgets"): Continue (last book and chapter), Streak (days, and
 * today's minutes against the goal) and New (unseen chapters), all from data the app records. Tap opens the app.
 * Dot draws black with the red accent in a monospace face; Colour draws navy with the gold accent; other themes use
 * Colour's look. Glance can't use the app's bundled fonts, so Doto / Instrument Serif become system faces. Left out:
 * the cover (bitmap loading for widgets is a follow-up), Continue watching (anime) and Listening (TTS state).
 */
class TsuzukiReadingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val readingDays = Injekt.get<ReadingDays>()
        val library = Injekt.get<LibraryPreferences>()
        val history = Injekt.get<GetHistory>()
        val ui = Injekt.get<UiPreferences>()
        val dot = ui.appTheme.get() == AppTheme.DOT
        // Dot covers are greyscale unless the owner keeps their colours (Appearance → Colour covers).
        val grey = dot && !ui.tsuzukiDotColourCovers.get()
        val labels = Labels(
            continueLabel = context.stringResource(MR.strings.tsz_continue),
            streak = context.stringResource(MR.strings.tsz_streak),
            days = context.stringResource(MR.strings.tsz_days),
            today = context.stringResource(MR.strings.tsz_today),
            min = context.stringResource(MR.strings.tsz_min),
            new = context.stringResource(MR.strings.tsz_new_chapters),
        )

        provideContent {
            val days by remember { readingDays.changes().map { readingDays.days() } }
                .collectAsState(initial = readingDays.days())
            val goal by remember { readingDays.dailyGoalMinutes.changes() }
                .collectAsState(initial = readingDays.dailyGoalMinutes.get())
            val newCount by remember { library.newUpdatesCount.changes() }
                .collectAsState(initial = library.newUpdatesCount.get())
            val latest by remember { history.subscribe(query = "", limit = 1) }.collectAsState(initial = emptyList())
            val cover by remember {
                history.subscribe(query = "", limit = 1).map { list ->
                    list.firstOrNull()?.let { loadCover(context, it, grey) }
                }
            }.collectAsState(initial = null)

            val today = LocalDate.now()
            val todayMinutes = TimeUnit.MILLISECONDS.toMinutes(days[today] ?: 0L).toInt()
            ReadingWidgetContent(
                dot = dot,
                labels = labels,
                title = latest.firstOrNull()?.title,
                chapterNumber = latest.firstOrNull()?.chapterNumber,
                cover = cover,
                streak = ReadingDays.streak(days, today),
                todayMinutes = todayMinutes,
                goal = goal,
                newCount = newCount,
            )
        }
    }
}

class TsuzukiReadingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TsuzukiReadingWidget()
}

private data class Labels(
    val continueLabel: String,
    val streak: String,
    val days: String,
    val today: String,
    val min: String,
    val new: String,
)

@Composable
private fun ReadingWidgetContent(
    dot: Boolean,
    labels: Labels,
    title: String?,
    chapterNumber: Double?,
    cover: Bitmap?,
    streak: Int,
    todayMinutes: Int,
    goal: Int,
    newCount: Int,
) {
    val background = if (dot) Color(0xFF000000) else Color(0xFF1C2B4A)
    val tile = if (dot) Color(0xFF0D0D0D) else Color(0xFF2A3B60)
    val accent = if (dot) Color(0xFFFF2D2D) else Color(0xFFE9B44C)
    val muted = if (dot) Color(0xFF8A8A8A) else Color(0xB8FFFFFF)
    val face = if (dot) FontFamily.Monospace else FontFamily.SansSerif
    fun up(s: String) = if (dot) s.uppercase() else s

    Column(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(background)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(
            GlanceModifier.fillMaxWidth().cornerRadius(18.dp).background(tile).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (cover != null) {
                Image(
                    provider = ImageProvider(cover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.size(width = 56.dp, height = 84.dp).cornerRadius(10.dp),
                )
                Spacer(GlanceModifier.width(12.dp))
            }
            ContinueText(::up, labels, title, chapterNumber, muted, face)
        }
        Spacer(GlanceModifier.height(8.dp))
        StatsRow(::up, labels, streak, todayMinutes, goal, newCount, tile, accent, muted, face)
    }
}

@Composable
private fun ContinueText(
    up: (String) -> String,
    labels: Labels,
    title: String?,
    chapterNumber: Double?,
    muted: Color,
    face: FontFamily,
) {
    Column {
        Text(up(labels.continueLabel), style = label(muted, face))
        Text(
            up(title ?: "—"),
            maxLines = 2,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = face,
            ),
        )
        if (chapterNumber != null && chapterNumber >= 0) {
            val whole = chapterNumber % 1.0 == 0.0
            val number = if (whole) chapterNumber.toInt().toString() else chapterNumber.toString()
            Text(
                up("Ch. $number"),
                style = TextStyle(color = ColorProvider(muted), fontSize = 12.sp, fontFamily = face),
            )
        }
    }
}

@Composable
private fun StatsRow(
    up: (String) -> String,
    labels: Labels,
    streak: Int,
    todayMinutes: Int,
    goal: Int,
    newCount: Int,
    tile: Color,
    accent: Color,
    muted: Color,
    face: FontFamily,
) {
    Row(GlanceModifier.fillMaxSize()) {
        Stat(
            label = up(labels.streak),
            value = "$streak",
            valueColor = accent,
            sub = up("${labels.days} · $todayMinutes/$goal ${labels.min} ${labels.today.lowercase()}"),
            tile = tile,
            muted = muted,
            face = face,
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(GlanceModifier.width(8.dp))
        Stat(
            label = up(labels.new),
            value = "$newCount",
            valueColor = Color.White,
            sub = null,
            tile = tile,
            muted = muted,
            face = face,
            modifier = GlanceModifier.defaultWeight(),
        )
    }
}

@Composable
private fun Stat(
    label: String,
    value: String,
    valueColor: Color,
    sub: String?,
    tile: Color,
    muted: Color,
    face: FontFamily,
    modifier: GlanceModifier,
) {
    Column(
        modifier.fillMaxHeight().cornerRadius(18.dp).background(tile).padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, style = label(muted, face))
        Text(
            value,
            style = TextStyle(
                color = ColorProvider(valueColor),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = face,
            ),
        )
        if (sub != null) {
            Text(
                sub,
                maxLines = 2,
                style = TextStyle(color = ColorProvider(muted), fontSize = 11.sp, fontFamily = face),
            )
        }
    }
}

private fun label(color: Color, face: FontFamily) =
    TextStyle(color = ColorProvider(color), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = face)

/** The last book's cover, sized for the widget; greyscale in Dot unless covers keep their colours. */
private fun loadCover(context: Context, item: HistoryWithRelations, grey: Boolean): Bitmap? =
    loadWidgetCover(context, item.coverData, grey)

/** The cover, sized for a widget; greyscale in Dot unless covers keep their colours. Shared with the Listening widget. */
internal fun loadWidgetCover(context: Context, data: Any?, grey: Boolean): Bitmap? = runCatching {
    val request = coil3.request.ImageRequest.Builder(context)
        .data(data)
        .size(168, 252)
        // Widgets parcel their bitmaps to the launcher; hardware bitmaps can't be parcelled.
        .allowHardware(false)
        .memoryCachePolicy(coil3.request.CachePolicy.DISABLED)
        .build()
    val bitmap = context.imageLoader.executeBlocking(request).image?.toBitmap(168, 252) ?: return null
    if (!grey) return bitmap
    val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val paint = android.graphics.Paint().apply {
        colorFilter =
            android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(0f) })
    }
    android.graphics.Canvas(out).drawBitmap(bitmap, 0f, 0f, paint)
    out
}.getOrNull()
