package eu.kanade.tachiyomi.tsuzuki.anime.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnimeScreenshotNameTest {
    @Test
    fun nameHasTitleEpisodeAndPosition() {
        assertEquals("Sample Show - EP 12 - 00-14-32", animeScreenshotName("Sample Show", 12.0, "Episode 12", 872.4))
        assertEquals("Sample Show - EP 12.5 - 01-02-03", animeScreenshotName("Sample Show", 12.5, "", 3723.0))
    }

    @Test
    fun anUnnumberedEpisodeUsesItsName() {
        assertEquals("Movie - Special - 00-00-00", animeScreenshotName("Movie", -1.0, " Special ", -5.0))
    }
}
