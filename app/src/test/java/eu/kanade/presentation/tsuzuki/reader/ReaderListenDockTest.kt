package eu.kanade.presentation.tsuzuki.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReaderListenDockTest {
    @Test
    fun countdownUsesRealSecondsIncludingSubMinuteAndExpiredTimers() {
        assertEquals("15:00", sleepCountdownLabel(900))
        assertEquals("14:32", sleepCountdownLabel(872))
        assertEquals("0:09", sleepCountdownLabel(9))
        assertEquals("60:00", sleepCountdownLabel(3600))
        assertEquals("0:00", sleepCountdownLabel(-1))
    }
}
