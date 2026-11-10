package eu.kanade.tachiyomi.tsuzuki.anime

import tachiyomi.domain.entries.anime.model.Anime
import java.text.Normalizer
import java.util.Locale

private val separators = Regex("[^\\p{L}\\p{M}\\p{N}]+")
private val seasonPrefix = Regex("(?:^| )(?:season|s) *(\\d+)$")
private val seasonSuffix = Regex("(?:^| )(\\d+)(?:st|nd|rd|th)? season$")
private val japaneseSeason = Regex(" *第 *(\\d+) *期$")

internal data class AnimeTitleKey(val title: String, val season: Int)

internal fun animeTitleKey(title: String): AnimeTitleKey? {
    val clean = Normalizer.normalize(title, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .replace(separators, " ")
        .trim()
    val match = seasonPrefix.find(clean) ?: seasonSuffix.find(clean) ?: japaneseSeason.find(clean)
    val season = match?.let { it.groupValues[1].toIntOrNull() ?: return null } ?: 1
    val name = if (match == null) clean else clean.substring(0, match.range.first).trimEnd()
    return name.takeIf(String::isNotEmpty)?.let { AnimeTitleKey(it.replace(" ", ""), season) }
}

internal fun findAnimeDuplicate(candidate: Anime, library: List<Anime>, trackedMatchId: Long?): Anime? {
    val otherSources = library.filter { it.favorite && it.id != candidate.id && it.source != candidate.source }
    otherSources.firstOrNull { it.id == trackedMatchId }?.let { return it }
    val title = candidate.duplicateTitleKey() ?: return null
    return otherSources.firstOrNull { it.duplicateTitleKey() == title }
}

private fun Anime.duplicateTitleKey(): AnimeTitleKey? = animeTitleKey(title)?.let { key ->
    val number = seasonNumber.toInt()
    if (number >= 0 && number.toDouble() == seasonNumber) key.copy(season = number) else key
}
