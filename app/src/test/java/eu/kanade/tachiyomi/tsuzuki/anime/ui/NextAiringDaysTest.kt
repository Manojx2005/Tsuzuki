package eu.kanade.tachiyomi.tsuzuki.anime.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tachiyomi.domain.items.episode.model.Episode
import java.util.concurrent.TimeUnit

class NextAiringDaysTest {
    private val day = TimeUnit.DAYS.toMillis(1)
    private fun episode(upload: Long) = Episode(1, "/$upload", "Ep", dateUpload = upload)

    @Test
    fun earliestFutureEpisodeInWholeDays() {
        val now = 100 * day
        assertNull(nextAiringDays(listOf(episode(now - day), episode(now)), now))
        assertEquals(0, nextAiringDays(listOf(episode(now + day / 2)), now))
        assertEquals(2, nextAiringDays(listOf(episode(now + 5 * day), episode(now + 2 * day + 1)), now))
    }
}
