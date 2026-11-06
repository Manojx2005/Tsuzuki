package eu.kanade.presentation.tsuzuki.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Calendar

/** What the History board's stat tiles show, from the history table. */
internal data class HistoryStats(val todayMinutes: Int, val chaptersToday: Int)

/**
 * Today's reading from history rows. Each row holds a chapter's last read time and its total time read, so
 * "today" is chapters last read today and their time (the same measure as Home's Today tile). A streak would
 * need per-day sessions, which history overwrites, so it isn't shown.
 */
internal fun historyStats(history: List<HistoryWithRelations>, startOfToday: Long): HistoryStats {
    val today = history.filter { (it.readAt?.time ?: 0L) >= startOfToday }
    return HistoryStats(
        todayMinutes = (today.sumOf { it.readDuration } / 60_000L).toInt(),
        chaptersToday = today.distinctBy { it.chapterId }.size,
    )
}

internal fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Board "History": Today and Chapters tiles above the list. Subscribes on its own, independent of list paging. */
@Composable
fun TsuzukiHistoryStatsTiles(modifier: Modifier = Modifier) {
    val history by remember { Injekt.get<GetHistory>().subscribe(query = "", limit = 500) }
        .collectAsState(initial = emptyList())
    val stats = remember(history) { historyStats(history, startOfToday()) }
    val readingDays = remember { Injekt.get<eu.kanade.tachiyomi.tsuzuki.ReadingDays>() }
    val daysRaw by remember { readingDays.changes() }.collectAsState(initial = "")
    val streak = remember(daysRaw) {
        eu.kanade.tachiyomi.tsuzuki.ReadingDays.streak(readingDays.days(), java.time.LocalDate.now())
    }
    FlowRow(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        maxItemsInEachRow = if (LocalDensity.current.fontScale >= 1.5f) 1 else 3,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatTile(
            stringResource(MR.strings.tsz_today),
            stats.todayMinutes,
            stringResource(MR.strings.tsz_min),
            Modifier.weight(1f),
        )
        StatTile(
            stringResource(MR.strings.tsz_chapters),
            stats.chaptersToday,
            stringResource(MR.strings.tsz_ch),
            Modifier.weight(1f),
        )
        StatTile(
            stringResource(MR.strings.tsz_streak),
            streak,
            stringResource(MR.strings.tsz_days),
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(label: String, value: Int, unit: String, modifier: Modifier) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(18.dp)
    val up: (String) -> String = { if (t.isDot) it.uppercase() else it }
    Column(
        modifier.heightIn(min = 104.dp).clip(shape).background(t.tile)
            .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .padding(14.dp),
    ) {
        Text(
            up(label),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "$value",
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 40.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                up(unit),
                modifier = Modifier.align(Alignment.Bottom).padding(bottom = 3.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
