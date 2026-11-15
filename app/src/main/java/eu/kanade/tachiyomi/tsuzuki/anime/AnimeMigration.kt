package eu.kanade.tachiyomi.tsuzuki.anime

import tachiyomi.domain.items.episode.model.Episode

internal enum class AnimeMigrationMode { COPY, REPLACE }

internal fun matchAnimeEpisodes(original: List<Episode>, target: List<Episode>): List<Pair<Episode, Episode>> {
    val progressOrder = compareBy(Episode::seen).thenBy(Episode::lastSecondSeen).thenBy(Episode::totalSeconds)
    val originals = original.mapNotNull { episode ->
        episode.migrationNumber()?.let { it to episode }
    }.groupBy({ it.first }, { it.second }).mapValues { (_, releases) ->
        releases.maxWith(progressOrder)
    }
    return target.mapNotNull { episode ->
        originals[episode.migrationNumber()]?.let { it to episode }
    }
}

// Source episode numbers are floats. Backups can store the same decimal as a double.
private fun Episode.migrationNumber(): Float? = episodeNumber.toFloat()
    .takeIf { episodeNumber >= 0 && it.isFinite() }
    ?.let { if (it == 0f) 0f else it }

internal fun Episode.withMigratedProgress(original: Episode): Episode {
    val duration = totalSeconds.takeIf { it > 0 } ?: original.totalSeconds
    val inherited = if (duration > 0) minOf(original.lastSecondSeen, duration) else original.lastSecondSeen
    return copy(
        seen = seen || original.seen,
        lastSecondSeen = maxOf(lastSecondSeen, inherited),
        totalSeconds = duration,
    )
}
