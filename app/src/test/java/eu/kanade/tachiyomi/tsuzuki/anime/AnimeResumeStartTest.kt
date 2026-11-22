package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayAfterEnd
import eu.kanade.tachiyomi.tsuzuki.anime.player.animePlayAfterEnd
import eu.kanade.tachiyomi.tsuzuki.anime.player.animeResumeStart
import eu.kanade.tachiyomi.tsuzuki.anime.player.animeStartIsAtEnd
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AnimeResumeStartTest {
    @Test
    fun `a position in the middle of the episode is kept`() {
        animeResumeStart(saved = 600.0, seen = false, knownDuration = 1420.0) shouldBe 600.0
    }

    @Test
    fun `a position within ten seconds of the end starts over`() {
        animeResumeStart(saved = 1415.0, seen = false, knownDuration = 1420.0) shouldBe 0.0
        animeResumeStart(saved = 1410.0, seen = false, knownDuration = 1420.0) shouldBe 0.0
        animeResumeStart(saved = 1200.0, seen = false, knownDuration = 1420.0) shouldBe 1200.0
    }

    @Test
    fun `a position past ninety percent starts over on a long file`() {
        animeResumeStart(saved = 3300.0, seen = false, knownDuration = 3600.0) shouldBe 0.0
        animeResumeStart(saved = 3200.0, seen = false, knownDuration = 3600.0) shouldBe 3200.0
    }

    @Test
    fun `a seen episode and nonsense positions start at zero`() {
        animeResumeStart(saved = 600.0, seen = true, knownDuration = 1420.0) shouldBe 0.0
        animeResumeStart(saved = -5.0, seen = false, knownDuration = 1420.0) shouldBe 0.0
        animeResumeStart(saved = Double.NaN, seen = false, knownDuration = 1420.0) shouldBe 0.0
        animeResumeStart(saved = Double.POSITIVE_INFINITY, seen = false, knownDuration = 1420.0) shouldBe 0.0
    }

    @Test
    fun `an unknown duration cannot be checked up front so the saved position is kept`() {
        animeResumeStart(saved = 1415.0, seen = false, knownDuration = 0.0) shouldBe 1415.0
        animeStartIsAtEnd(1415.0, 0.0) shouldBe false
    }

    @Test
    fun `the check once mpv reports the duration catches a start at the end`() {
        animeStartIsAtEnd(1415.0, 1420.0) shouldBe true
        animeStartIsAtEnd(0.0, 1420.0) shouldBe false
        animeStartIsAtEnd(1415.0, Double.NaN) shouldBe false
    }

    @Test
    fun `play after the end seeks when the duration is known and reloads when it is not`() {
        animePlayAfterEnd(1420.0) shouldBe AnimePlayAfterEnd.SEEK_TO_START
        animePlayAfterEnd(0.0) shouldBe AnimePlayAfterEnd.RELOAD
        animePlayAfterEnd(Double.NaN) shouldBe AnimePlayAfterEnd.RELOAD
    }

    @Test
    fun `a position restored after process death is kept near the end`() {
        animeResumeStart(saved = 50.6, seen = false, knownDuration = 60.0, restored = true) shouldBe 50.6
        animeResumeStart(saved = 59.5, seen = false, knownDuration = 60.0, restored = true) shouldBe 0.0
        animeResumeStart(saved = 50.6, seen = true, knownDuration = 60.0, restored = true) shouldBe 0.0
    }
}
