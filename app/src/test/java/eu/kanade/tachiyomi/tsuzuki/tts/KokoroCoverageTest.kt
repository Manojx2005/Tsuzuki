package eu.kanade.tachiyomi.tsuzuki.tts

import org.junit.jupiter.api.Assertions.assertEquals
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
    fun typographyThatTheModelTextNormalizesIsCovered() {
        assertTrue(isKokoroTextCovered("The quiet… morning began…", words))
        assertTrue(isKokoroTextCovered("Don’t “began”", words))
        assertTrue(isKokoroTextCovered("The quiet — morning began", words))
        assertTrue(isKokoroTextCovered("The quiet–morning began", words))
        assertTrue(isKokoroTextCovered("the quiet - morning", words))
        assertTrue(isKokoroTextCovered("the quiet-morning began", words))
        assertTrue(isKokoroTextCovered("café", words))
        assertTrue(isKokoroTextCovered("café", words))
        assertTrue(isKokoroTextCovered("'the' morning", words))
    }

    @Test
    fun theCheckedTextIsExactlyWhatTheRuntimeReceives() {
        assertEquals("the quiet, morning began...", kokoroModelText("the quiet — morning began…"))
        assertEquals("don't began", kokoroModelText("Don’t “began”".lowercase()).trim().replace(Regex(" +"), " "))
        assertEquals("quiet morning", kokoroModelText("quiet-morning"))
        assertTrue(kokoroModelText("naïve “café” — ok…").all { it.code < 128 })
    }

    @Test
    fun unicodeThatCannotBeNormalizedAndUnknownPartsStayUncovered() {
        assertFalse(isKokoroTextCovered("猫", words))
        assertFalse(isKokoroTextCovered("the/quiet", words))
        assertFalse(isKokoroTextCovered("quiet-zylvarion", words))
        assertFalse(isKokoroTextCovered("the 'zylvarion' morning", words))
        assertFalse(isKokoroTextCovered("...", words))
        assertFalse(isKokoroTextCovered("…", words))
    }
}
