package eu.kanade.presentation.tsuzuki.anime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeUpdateEpisode
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import tachiyomi.domain.history.anime.repository.AnimeHistoryRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate
import kotlin.time.Clock

/** New episodes (the count Updates › Anime shows) and the number of shows they belong to. */
internal fun newEpisodeSummary(entries: List<AnimeUpdateEpisode>): Pair<Int, Int> =
    entries.size to entries.mapTo(hashSetOf()) { it.animeId }.size

/**
 * AP-14 (board AnimeHome): the "New episodes" and "Watched today" tiles of Watch, from the library's episode updates
 * (the last three months, as in Updates) and the watch time recorded today.
 */
@Composable
internal fun WatchActivityTiles(store: AnimeLibraryStore, modifier: Modifier = Modifier) {
    val since = remember {
        Clock.System.now().minus(3, DateTimeUnit.MONTH, TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }
    val updates by remember(store, since) { store.observeNewEpisodes(since) }.collectAsState(emptyList())
    val today = remember { LocalDate.now() }
    val watchDays by remember(today) { Injekt.get<AnimeHistoryRepository>().subscribeWatchDays(today) }
        .collectAsState(emptyMap())
    val (newEpisodes, shows) = newEpisodeSummary(updates)
    val minutes = (watchDays[today] ?: 0L) / 60
    val tokens = TsuzukiTheme.tokens
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ActivityTile(
            label = stringResource(R.string.tsz_anime_tile_new_episodes),
            value = newEpisodes.toString(),
            unit = pluralStringResource(R.plurals.tsz_anime_tile_in_shows, shows, shows),
            valueColor = tokens.accent,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        ActivityTile(
            label = stringResource(R.string.tsz_anime_tile_watched_today),
            value = minutes.toString(),
            unit = stringResource(R.string.tsz_anime_tile_min),
            valueColor = tokens.dotOn,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun ActivityTile(label: String, value: String, unit: String, valueColor: Color, modifier: Modifier) {
    val tokens = TsuzukiTheme.tokens
    TsuzukiTile(modifier, padding = 16.dp) {
        Column {
            Text(
                tsuzukiMenuLabel(label),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = tokens.mutedText,
            )
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 40.sp),
                    color = valueColor,
                )
                Text(
                    tsuzukiMenuLabel(unit),
                    modifier = Modifier.padding(start = 8.dp, bottom = 3.dp),
                    fontSize = 12.sp,
                    color = tokens.mutedText,
                )
            }
        }
    }
}
