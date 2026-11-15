package eu.kanade.presentation.tsuzuki.anime

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import tachiyomi.domain.history.anime.repository.AnimeHistoryRepository
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.NumberFormat
import java.time.LocalDate
import tachiyomi.presentation.core.i18n.stringResource as mokoStringResource

@Composable
internal fun TsuzukiWatchStats(today: LocalDate) {
    if (!TsuzukiAnime.enabled) return
    val statsFlow = remember(today) { Injekt.get<AnimeHistoryRepository>().subscribeWatchStats(today) }
    val observed by statsFlow.collectAsState(initial = null)
    val stats = observed?.takeIf { it.hasWatchData } ?: return
    val tokens = TsuzukiTheme.tokens
    val locale = LocalConfiguration.current.locales[0]
    val numbers = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val duration = remember(locale) { MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            tsuzukiMenuLabel(mokoStringResource(MR.strings.tsz_watch)),
            color = tokens.mutedText,
            style = MaterialTheme.typography.labelLarge,
        )
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WatchStatTile(
                value = duration.formatWatchTime(stats.weekSeconds),
                label = stringResource(R.string.tsz_anime_watch_stats_week),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            WatchStatTile(
                value = duration.formatWatchTime(stats.monthSeconds),
                label = stringResource(R.string.tsz_anime_watch_stats_month),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        WatchStatTile(
            value = duration.formatWatchTime(stats.allTimeSeconds),
            label = stringResource(R.string.tsz_anime_watch_stats_all_time),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WatchStatTile(
                value = numbers.format(stats.episodesSeen),
                label = stringResource(R.string.tsz_anime_watch_stats_episodes),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            WatchStatTile(
                value = numbers.format(stats.completedAnime),
                label = stringResource(R.string.tsz_anime_watch_stats_completed),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun WatchStatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val tokens = TsuzukiTheme.tokens
    TsuzukiTile(modifier = modifier, padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                tsuzukiMenuLabel(value),
                color = tokens.dotOn,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                tsuzukiMenuLabel(label),
                color = tokens.mutedText,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

private fun MeasureFormat.formatWatchTime(seconds: Long): String {
    val minutes = seconds / 60
    return when {
        minutes < 60 -> formatMeasures(Measure(minutes, MeasureUnit.MINUTE))
        minutes % 60 == 0L -> formatMeasures(Measure(minutes / 60, MeasureUnit.HOUR))
        else -> formatMeasures(Measure(minutes / 60, MeasureUnit.HOUR), Measure(minutes % 60, MeasureUnit.MINUTE))
    }
}
