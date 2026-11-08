package eu.kanade.tachiyomi.tsuzuki.tts

import java.util.Locale

internal fun ttsVoiceLabel(
    engineLabel: String,
    voiceLocale: Locale,
    appLocale: Locale,
    connectionLabel: String,
): String {
    val language = voiceLocale.getDisplayLanguage(appLocale)
    if (language.isBlank() || language.equals(voiceLocale.language, ignoreCase = true)) return engineLabel
    return "${voiceLocale.getDisplayName(appLocale)} · $connectionLabel"
}
