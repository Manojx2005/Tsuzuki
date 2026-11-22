package eu.kanade.tachiyomi.tsuzuki.anime.preview

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiCoverFilter
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.tachiyomi.R
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Fixed bounds supplied by the caller keep lazy rows stable while artwork loads or fails. */
@Composable
internal fun AnimeEpisodeThumbnail(
    sourceId: Long,
    episode: Episode,
    coverUrl: String?,
    modifier: Modifier,
    hero: Boolean = false,
) {
    val tokens = TsuzukiTheme.tokens
    val store = remember { Injekt.get<AnimeFrameStore>() }
    val versions by store.versions.collectAsState()
    val mode by store.mode.collectAsState()
    val network by store.network.collectAsState()
    val model = AnimeEpisodePreview(
        episode.animeId, episode.id, episode.previewUrl, coverUrl, versions[episode.id] ?: 0, sourceId,
        previewNetworkAllowed(mode, network.isWifi),
    )
    Box(
        modifier.clip(RoundedCornerShape(if (hero) 16.dp else 10.dp))
            .alpha(if (!hero && episode.seen) 0.5f else 1f)
            .background(Brush.linearGradient(listOf(tokens.tile, tokens.background, tokens.track))),
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            colorFilter = tsuzukiCoverFilter(),
        )
        val remaining = previewRemaining(episode.lastSecondSeen, episode.totalSeconds)
        val chip = if (hero) {
            remaining?.let {
                val number = episode.episodeNumber.takeIf { it.isFinite() && it >= 0 }?.let {
                    if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
                }
                number?.let { stringResource(R.string.tsz_anime_preview_remaining, it, DateUtils.formatElapsedTime(remaining)) }
            }
        } else {
            episode.totalSeconds.takeIf { it > 0 }?.let(DateUtils::formatElapsedTime)
        }
        if (chip != null) {
            Text(
                tsuzukiMenuLabel(chip),
                modifier = Modifier.align(if (hero) Alignment.TopStart else Alignment.BottomEnd)
                    .padding(if (hero) 10.dp else 4.dp).clip(RoundedCornerShape(8.dp))
                    .background(tokens.background.copy(alpha = 0.85f)).padding(horizontal = 8.dp, vertical = 4.dp),
                style = if (hero) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                color = tokens.dotOn,
                maxLines = 1,
            )
        }
        if (episode.totalSeconds > 0 && episode.lastSecondSeen > 0) {
            val fill = if (hero && !tokens.isDot) tokens.dotOn else tokens.accent
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(tokens.track))
            Box(
                Modifier.align(Alignment.BottomStart)
                    .fillMaxWidth((episode.lastSecondSeen.toDouble() / episode.totalSeconds).toFloat().coerceIn(0f, 1f))
                    .height(3.dp).background(fill),
            )
        }
    }
}
