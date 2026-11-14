package eu.kanade.tachiyomi.tsuzuki.anime.player

import java.util.Locale

internal val animeLanguageCodes: List<String> by lazy {
    Locale.getISOLanguages().map { Locale.forLanguageTag(it).language }.distinct()
}

private val languageAliases by lazy {
    buildMap {
        for (code in animeLanguageCodes) {
            val locale = Locale.forLanguageTag(code)
            put(code, code)
            runCatching { locale.isO3Language }.getOrNull()?.let { put(it, code) }
            put(locale.getDisplayLanguage(Locale.ENGLISH).lowercase(Locale.ROOT), code)
        }
        putAll(mapOf("fre" to "fr", "ger" to "de", "chi" to "zh", "dut" to "nl", "cze" to "cs"))
    }
}

internal fun normalizeAnimeLanguage(value: String?): String? {
    val key = value?.trim()?.lowercase(Locale.ROOT) ?: return null
    return languageAliases[key] ?: languageAliases[key.substringBefore('-').substringBefore('_')]
}

private fun AnimeTrackKind.isOff(value: String) =
    this == AnimeTrackKind.SUBTITLE && value == AnimePlayerPreferences.SUBTITLES_OFF

internal fun animeLanguageOrder(value: String, kind: AnimeTrackKind): List<String> =
    value.split(',').mapNotNull { if (kind.isOff(it)) it else normalizeAnimeLanguage(it) }.distinct()

internal data class AnimeTrackChoice(val track: AnimePlayerTrack?)

private val partialSubtitle = Regex("\\b(?:forced|signs)\\b", RegexOption.IGNORE_CASE)

internal fun chooseAnimeTrack(
    kind: AnimeTrackKind,
    tracks: List<AnimePlayerTrack>,
    override: String?,
    languages: List<String>,
    legacy: String,
): AnimeTrackChoice? {
    val available = tracks.filter { it.kind == kind }
    fun byLanguage(language: String): AnimePlayerTrack? = available
        .filter { normalizeAnimeLanguage(it.language) == language }
        .minByOrNull { track ->
            kind == AnimeTrackKind.SUBTITLE && (track.forced || partialSubtitle.containsMatchIn(track.title.orEmpty()))
        }

    fun byKey(key: String): AnimeTrackChoice? {
        if (kind.isOff(key)) return AnimeTrackChoice(null)
        val language = normalizeAnimeLanguage(key.substringBefore('|'))
        val canonicalKey = if (language != null && '|' in key) "$language|${key.substringAfter('|')}" else key
        val selected = available.firstOrNull { it.preferenceKey == canonicalKey } ?: language?.let(::byLanguage)
        return selected?.let(::AnimeTrackChoice)
    }

    override?.takeIf(String::isNotBlank)?.let(::byKey)?.let { return it }
    for (language in languages) {
        if (kind.isOff(language)) return AnimeTrackChoice(null)
        normalizeAnimeLanguage(language)?.let(::byLanguage)?.let { return AnimeTrackChoice(it) }
    }
    return legacy.takeIf { languages.isEmpty() && it.isNotBlank() }?.let(::byKey)
}
