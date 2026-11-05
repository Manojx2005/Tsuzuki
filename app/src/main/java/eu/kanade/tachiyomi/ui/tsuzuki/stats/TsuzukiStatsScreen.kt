package eu.kanade.tachiyomi.ui.tsuzuki.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressRing
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSegments
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.tsuzuki.ReadingBar
import eu.kanade.tachiyomi.tsuzuki.ReadingChart
import eu.kanade.tachiyomi.tsuzuki.ReadingDays
import eu.kanade.tachiyomi.tsuzuki.ReadingPeriod
import eu.kanade.tachiyomi.ui.stats.StatsScreen
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * The "Stats & goals" board: daily goal ring, minutes chart (week / month / year) and chapter count, all from
 * [ReadingDays] and history. Tiles the app has no data for (words, speed, listening, anime) are left out.
 * "All statistics" opens Mihon's library statistics.
 */
class TsuzukiStatsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val readingDays = remember { Injekt.get<ReadingDays>() }
        val raw by readingDays.changes().collectAsState(initial = "")
        val days = remember(raw) { readingDays.days() }
        val goal by readingDays.dailyGoalMinutes.collectAsState()
        val history by remember { Injekt.get<GetHistory>().subscribe(query = "", limit = Long.MAX_VALUE) }
            .collectAsState(initial = emptyList())
        var period by rememberSaveable { mutableIntStateOf(0) }
        val today = LocalDate.now()
        val locale = Locale.getDefault()
        val bars = remember(days, period, today) {
            ReadingChart.bars(
                days,
                today,
                ReadingPeriod.entries[period],
                weekdayLabel = { it.getDisplayName(TextStyle.NARROW, locale) },
                monthLabel = { java.time.Month.of(it).getDisplayName(TextStyle.NARROW, locale) },
            )
        }
        val periodStart = when (ReadingPeriod.entries[period]) {
            ReadingPeriod.WEEK -> today.with(
                java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY),
            )
            ReadingPeriod.MONTH -> today.minusDays(29)
            ReadingPeriod.YEAR -> today.withDayOfMonth(1).minusMonths(11)
        }
        val startMs = periodStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val chapters = remember(history, startMs) {
            history.filter { (it.readAt?.time ?: 0L) >= startMs }.distinctBy { it.chapterId }.size
        }
        val todayMinutes = (days[today] ?: 0L) / 60_000L
        val tokens = TsuzukiTheme.tokens
        fun cased(s: String) = if (tokens.isDot) s.uppercase() else s

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.tsz_stats_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Goal ring.
                Tile {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TsuzukiProgressRing(
                            progress = if (goal > 0) (todayMinutes.toFloat() / goal).coerceIn(0f, 1f) else 0f,
                            diameter = 112.dp,
                            thickness = 10.dp,
                            dots = 36,
                        )
                        Column(Modifier.padding(start = 20.dp)) {
                            Text(
                                cased(stringResource(MR.strings.tsz_stats_today_goal)),
                                color = tokens.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    "$todayMinutes",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontSize = 44.sp,
                                        lineHeight = 44.sp,
                                    ),
                                )
                                Text(
                                    "/$goal ${stringResource(MR.strings.tsz_min)}",
                                    Modifier.padding(start = 4.dp, bottom = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                RoundButton(
                                    Icons.Outlined.Remove,
                                    stringResource(MR.strings.tsz_a11y_decrease),
                                    goal > 5,
                                ) {
                                    readingDays.dailyGoalMinutes.set((goal - 5).coerceIn(5, 240))
                                }
                                RoundButton(
                                    Icons.Outlined.Add,
                                    stringResource(MR.strings.tsz_a11y_increase),
                                    goal < 240,
                                ) {
                                    readingDays.dailyGoalMinutes.set((goal + 5).coerceIn(5, 240))
                                }
                            }
                        }
                    }
                }

                TsuzukiSegments(
                    labels = listOf(
                        stringResource(MR.strings.tsz_stats_week),
                        stringResource(MR.strings.tsz_stats_month),
                        stringResource(MR.strings.tsz_stats_year),
                    ),
                    selected = period,
                    onClick = { period = it },
                    modifier = Modifier,
                )

                // Minutes chart.
                Tile {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            cased(stringResource(MR.strings.tsz_stats_minutes_read)),
                            Modifier.weight(1f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            ReadingChart.formatTotal(bars.sumOf { it.minutes }),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Chart(bars)
                }

                // Only figures the app records.
                Tile {
                    Text("$chapters", style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp))
                    Text(
                        cased(stringResource(MR.strings.tsz_chapters)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }

                TextButton(onClick = { navigator.push(StatsScreen()) }, modifier = Modifier.align(Alignment.End)) {
                    Icon(Icons.Outlined.QueryStats, null, Modifier.padding(end = 8.dp).size(18.dp))
                    Text(cased(stringResource(MR.strings.tsz_stats_all)))
                }
            }
        }
    }
}

@Composable
private fun Tile(content: @Composable () -> Unit) {
    val t = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(t.tileRadius)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(t.tile)
            .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .padding(20.dp),
    ) { content() }
}

@Composable
private fun RoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier.minimumInteractiveComponentSize().size(44.dp).clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .tsuzukiClickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            description,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f),
        )
    }
}

/** Bars of dots (Dot) or rounded bars (Colour); the current bar is in the accent. */
@Composable
private fun Chart(bars: List<ReadingBar>) {
    val t = TsuzukiTheme.tokens
    val max = (bars.maxOfOrNull { it.minutes } ?: 0L).coerceAtLeast(1L)
    val showLabels = bars.size <= 12
    Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        bars.forEach { bar ->
            val color = if (bar.isCurrent) t.accent else t.dotOn
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                val fraction = bar.minutes.toFloat() / max
                if (t.isDot && bars.size <= 12) {
                    val total = 8
                    val lit = if (bar.minutes > 0) (fraction * total).toInt().coerceAtLeast(1) else 0
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(total) { i ->
                            Box(
                                Modifier.size(12.dp).clip(CircleShape)
                                    .background(if (total - i <= lit) color else t.dotOff),
                            )
                        }
                    }
                } else {
                    Box(
                        Modifier.width(if (bars.size <= 12) 14.dp else 5.dp)
                            .height((130 * fraction).dp.coerceAtLeast(3.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (bar.minutes > 0) color else Color.Transparent)
                            .background(if (bar.minutes > 0) color else t.dotOff),
                    )
                }
                if (showLabels) {
                    Text(
                        bar.label,
                        Modifier.padding(top = 8.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
