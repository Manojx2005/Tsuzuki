package eu.kanade.tachiyomi.tsuzuki.tts

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

class InstalledPhoneVoicesTest {
    @Test
    fun unavailableDataIsExcludedAndHigherQualityIsOfferedFirstWithinEachLocale() {
        val missing = voice("missing", 500, setOf(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))
        val normal = voice("normal", 200)
        val high = voice("high", 400)
        val online = voice("online", 300, network = true)
        assertEquals(listOf(high, online, normal), installedPhoneVoices(setOf(normal, missing, online, high)))
    }

    private fun voice(id: String, rank: Int, flags: Set<String> = emptySet(), network: Boolean = false) = mockk<Voice> {
        every { name } returns id
        every { locale } returns Locale.US
        every { quality } returns rank
        every { features } returns flags
        every { isNetworkConnectionRequired } returns network
    }
}
