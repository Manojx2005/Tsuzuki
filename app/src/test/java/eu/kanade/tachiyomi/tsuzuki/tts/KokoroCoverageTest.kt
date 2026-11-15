package eu.kanade.tachiyomi.tsuzuki.tts

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KokoroCoverageTest {
    private val words = setOf("the", "quiet", "morning", "began", "don't", "cafe")

    @Test
    fun quotedKnownWordsRemainCoveredAfterTheExactNativeInputNormalization() {
        assertTrue(isKokoroTextCovered("\"The quiet morning began.\"", words))
        assertTrue(isKokoroTextCovered("Don't!", words))
    }

    @Test
    fun unknownNamesNumbersAndWebAddressesGoToPhoneVoice() {
        assertFalse(isKokoroTextCovered("The Zylvarion morning began.", words))
        assertFalse(isKokoroTextCovered("The morning began at 12.", words))
        assertFalse(isKokoroTextCovered("https://the.quiet/morning", words))
        assertFalse(isKokoroTextCovered("the@quiet", words))
    }

    @Test
    fun unicodeAndEdgeApostrophesCannotBypassTheNativeTokenBoundaries() {
        assertFalse(isKokoroTextCovered("'the' morning", words))
        assertFalse(isKokoroTextCovered("cafe\u0301", words))
        assertFalse(isKokoroTextCovered("\u732b", words))
        assertFalse(isKokoroTextCovered("the/quiet", words))
        assertFalse(isKokoroTextCovered("quiet-morning", words))
        assertFalse(isKokoroTextCovered("...", words))
    }
}
