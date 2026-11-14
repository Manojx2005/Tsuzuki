package eu.kanade.tachiyomi.tsuzuki.tts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

class TtsLanguageTest {
    @Test
    fun englishChapterOnJapaneseDeviceFallsBackToEnglish() {
        val chapter = "The second chapter opens when the morning fog lifts from the harbor."
        assertEquals(Locale.ENGLISH, fallbackTtsLanguage(chapter, null, false, Locale.JAPAN))
        assertEquals(Locale.ENGLISH, fallbackTtsLanguage(chapter, Locale.JAPANESE, false, Locale.JAPAN))
    }

    @Test
    fun japaneseProseTakesPrecedenceOverAnEnglishChapterHeading() {
        val chapter = "Chapter 2\n夜が明けて、港に停泊していた船がゆっくりと動き出した。"
        assertEquals(Locale.JAPANESE, fallbackTtsLanguage(chapter, Locale.ENGLISH, true, Locale.US))
    }

    @Test
    fun latinSourceLanguageIsPreservedWithoutItsRegion() {
        val chapter = "Le brouillard se levait lentement sur le port."
        assertEquals(Locale.FRENCH, fallbackTtsLanguage(chapter, Locale.CANADA_FRENCH, true, Locale.JAPAN))
    }

    @Test
    fun automaticLocaleRetainsWritingSystemButDropsRegion() {
        assertEquals(Locale.ENGLISH, ttsLanguageOnly(Locale.US))
        assertEquals(Locale.ENGLISH, ttsLanguageOnly(Locale.UK))
        assertEquals(Locale.forLanguageTag("zh-Hant"), ttsLanguageOnly(Locale.forLanguageTag("zh-Hant-TW")))
    }

    @Test
    fun koreanAndChineseAreDetectedFromScript() {
        assertEquals(Locale.KOREAN, fallbackTtsLanguage("안개가 걷히자 항구가 보였다.", null, false, Locale.US))
        assertEquals(Locale.CHINESE, fallbackTtsLanguage("晨雾散去港口渐渐显现", null, false, Locale.JAPAN))
        assertEquals(Locale.CHINESE, fallbackTtsLanguage("晨雾散去港口渐渐显现", Locale.ENGLISH, true, Locale.JAPAN))
    }

    @Test
    fun hanOnlyTextKeepsAnEastAsianSourceLanguage() {
        assertEquals(Locale.JAPANESE, fallbackTtsLanguage("港口", Locale.JAPANESE, false, Locale.US))
    }

    @Test
    fun emptyAndSymbolOnlyTextUseSourceThenDevice() {
        assertEquals(Locale.JAPANESE, fallbackTtsLanguage("", null, false, Locale.JAPAN))
        assertEquals(Locale.FRENCH, fallbackTtsLanguage("123 ... !?", Locale.FRENCH, true, Locale.JAPAN))
    }

    @Test
    fun shortTextFollowsItsScript() {
        assertEquals(Locale.ENGLISH, fallbackTtsLanguage("Hi.", null, false, Locale.JAPAN))
        assertEquals(Locale.JAPANESE, fallbackTtsLanguage("はい。", null, false, Locale.US))
    }

    @Test
    fun latinTextWithNonLatinSourceIsEnglish() {
        assertEquals(
            Locale.ENGLISH,
            fallbackTtsLanguage("A short line of prose.", Locale.JAPANESE, false, Locale.JAPAN),
        )
    }
}
