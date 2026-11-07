package eu.kanade.tachiyomi.tsuzuki.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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
import coil3.executeBlocking
import coil3.imageLoader
import coil3.request.allowHardware
import coil3.toBitmap
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTokens
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.ReadingDays
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.tsuzuki.home.homeChapterNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import tachiyomi.core.common.Constants
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import androidx.glance.appwidget.action.actionStartActivity as actionStartActivityIntent

/** Continue reading with live history, chapter progress, and optional stats at larger widget sizes. */
class TsuzukiReadingWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val readingDays = Injekt.get<ReadingDays>()
        val library = Injekt.get<LibraryPreferences>()
        val history = Injekt.get<GetHistory>()
        val ui = Injekt.get<UiPreferences>()
        val tokens = widgetTheme(ui)
        val grey = tokens.isDot && !ui.tsuzukiDotColourCovers.get()
        val getNextChapters = Injekt.get<GetNextChapters>()
        val getChapters = Injekt.get<GetChaptersByMangaId>()
        val labels = Labels(
            continueLabel = context.stringResource(MR.strings.tsz_continue),
            resumeLabel = context.stringResource(MR.strings.action_resume),
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
            val book by remember {
                history.subscribe(query = "", limit = 1).mapLatest { rows ->
                    val item = rows.firstOrNull() ?: return@mapLatest null
                    withContext(Dispatchers.IO) {
                        val resume = getNextChapters.await(item.mangaId, item.chapterId, onlyUnread = false)
                            .firstOrNull()
                        val chapters = getChapters.await(item.mangaId)
                        val chapterNumber = resume?.chapterNumber ?: item.chapterNumber
                        ReadingWidgetBook(
                            title = item.title,
                            chapter = chapterNumber.takeIf { it >= 0 }?.let {
                                context.stringResource(MR.strings.display_mode_chapter, homeChapterNumber(it))
                            },
                            cover = loadWidgetCover(context, item.coverData, grey),
                            progress = readingWidgetProgress(chapters.count { it.read }, chapters.size),
                            canResume = resume != null,
                            destination = if (resume != null) {
                                ReaderActivity.newIntent(context, resume.mangaId, resume.id)
                            } else {
                                Intent(context, MainActivity::class.java).setAction(Constants.SHORTCUT_MANGA)
                                    .putExtra(Constants.MANGA_EXTRA, item.mangaId)
                            },
                        )
                    }
                }.catch { emit(null) }
            }.collectAsState(initial = null)

            val today = LocalDate.now()
            val todayMinutes = TimeUnit.MILLISECONDS.toMinutes(days[today] ?: 0L).toInt()
            ReadingWidgetContent(
                tokens = tokens,
                labels = labels,
                book = book,
                updates = actionStartActivityIntent(
                    Intent(context, MainActivity::class.java).setAction(Constants.SHORTCUT_UPDATES),
                ),
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
    val resumeLabel: String,
    val streak: String,
    val days: String,
    val today: String,
    val min: String,
    val new: String,
)

private data class ReadingWidgetBook(
    val title: String,
    val chapter: String?,
    val cover: Bitmap?,
    val progress: Float?,
    val canResume: Boolean,
    val destination: Intent,
)

internal fun readingWidgetProgress(read: Int, total: Int): Float? =
    if (total <= 0) null else read.coerceIn(0, total).toFloat() / total

@Composable
private fun ReadingWidgetContent(
    tokens: TsuzukiTokens,
    labels: Labels,
    book: ReadingWidgetBook?,
    updates: Action,
    streak: Int,
    todayMinutes: Int,
    goal: Int,
    newCount: Int,
) {
    val height = LocalSize.current.height
    val tall = height >= 244.dp
    val compact = height < 140.dp
    val coverHeight = if (compact) 72.dp else 100.dp
    val face = if (tokens.isDot) FontFamily.Monospace else FontFamily.SansSerif
    fun up(s: String) = if (tokens.isDot) s.uppercase() else s
    val open = book?.let { actionStartActivityIntent(it.destination) } ?: actionStartActivity<MainActivity>()

    Column(
        GlanceModifier.fillMaxSize().appWidgetBackground().cornerRadius(24.dp)
            .background(tokens.background).padding(10.dp),
    ) {
        Row(
            GlanceModifier.fillMaxWidth().cornerRadius(18.dp).background(tokens.tile)
                .padding(10.dp).clickable(open),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (book?.cover != null) {
                Image(
                    provider = ImageProvider(book.cover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.size(width = coverHeight * 0.67f, height = coverHeight)
                        .cornerRadius(8.dp),
                )
                Spacer(GlanceModifier.width(10.dp))
            }
            Column(GlanceModifier.defaultWeight()) {
                Text(up(labels.continueLabel), style = label(tokens.mutedText, face))
                Text(
                    up(book?.title ?: "—"),
                    maxLines = 2,
                    style = TextStyle(
                        color = ColorProvider(tokens.dotOn),
                        fontSize = if (compact) 16.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (tokens.isDot) face else FontFamily.Serif,
                    ),
                )
                book?.chapter?.let {
                    Text(up(it), maxLines = 1, style = label(tokens.mutedText, face))
                }
                book?.progress?.let {
                    Spacer(GlanceModifier.height(6.dp))
                    ReadingWidgetProgress(it, tokens)
                }
            }
            if (book?.canResume == true) {
                Spacer(GlanceModifier.width(8.dp))
                Box(
                    GlanceModifier.size(44.dp).cornerRadius(22.dp).background(tokens.accent).clickable(open),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_play_arrow_24dp),
                        contentDescription = labels.resumeLabel,
                        colorFilter = ColorFilter.tint(ColorProvider(tokens.onAccent)),
                        modifier = GlanceModifier.size(24.dp),
                    )
                }
            }
        }
        if (tall) {
            Spacer(GlanceModifier.height(8.dp))
            StatsRow(::up, labels, streak, todayMinutes, goal, newCount, tokens, face, updates)
        }
    }
}

@Composable
private fun ReadingWidgetProgress(progress: Float, tokens: TsuzukiTokens) {
    if (!tokens.isDot) {
        LinearProgressIndicator(
            progress = progress,
            modifier = GlanceModifier.fillMaxWidth().height(4.dp).cornerRadius(2.dp),
            color = ColorProvider(tokens.accent),
            backgroundColor = ColorProvider(tokens.track),
        )
        return
    }
    val dotCount = 10
    val current = (progress * dotCount).roundToInt()
    Row {
        repeat(dotCount) { index ->
            val color = when {
                index < current -> tokens.dotOn
                index == current -> tokens.dotHere
                else -> tokens.dotOff
            }
            Box(GlanceModifier.size(7.dp), contentAlignment = Alignment.Center) {
                Box(GlanceModifier.size(4.dp).cornerRadius(2.dp).background(color)) {}
            }
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
    tokens: TsuzukiTokens,
    face: FontFamily,
    updates: Action,
) {
    Row(GlanceModifier.fillMaxSize()) {
        Stat(
            label = up(labels.streak),
            value = "$streak",
            valueColor = tokens.accent,
            sub = up("${labels.days} · $todayMinutes/$goal ${labels.min} ${labels.today.lowercase()}"),
            tile = tokens.tile,
            muted = tokens.mutedText,
            face = face,
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(GlanceModifier.width(8.dp))
        Stat(
            label = up(labels.new),
            value = "$newCount",
            valueColor = Color.White,
            sub = null,
            tile = tokens.tile,
            muted = tokens.mutedText,
            face = face,
            modifier = GlanceModifier.defaultWeight().clickable(updates),
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
                fontSize = 36.sp,
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

/** Loads a widget-sized cover, greyscale in Dot unless the cover-colour setting is on. */
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
