package eu.kanade.tachiyomi.tsuzuki.tts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

class TtsVoiceLabelTest {
    @Test
    fun `voice label uses the locale in the app language`() {
        val label = ttsVoiceLabel("en-US-language", Locale.US, Locale.JAPANESE, "オフライン")
        assertEquals("${Locale.US.getDisplayName(Locale.JAPANESE)} · オフライン", label)
    }

    @Test
    fun `unknown voice locales preserve the engine label`() {
        for (voiceLocale in listOf(Locale.ROOT, Locale.forLanguageTag("qaa-US"))) {
            assertEquals("engine-voice", ttsVoiceLabel("engine-voice", voiceLocale, Locale.US, "Online"))
        }
    }
}
