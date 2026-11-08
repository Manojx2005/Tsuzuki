package eu.kanade.tachiyomi.tsuzuki.anime

import android.content.Context
import eu.kanade.presentation.tsuzuki.updates.airingDay
import eu.kanade.tachiyomi.animesource.model.AnimeUpdateStrategy
import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.animesource.model.SAnime
import io.mockk.confirmVerified
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_OUTSIDE_RELEASE_PERIOD
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class AnimeLibraryUpdatePolicyTest {
    private val show = Anime(source = 1, url = "/show", title = "Show", favorite = true)
    private val now = TimeUnit.DAYS.toMillis(10)
    private val end = TimeUnit.DAYS.toMillis(11)

    @Test
    fun schedulingDoesNotTouchWorkManagerWhenAnimeIsDisabled() {
        val context = mockk<Context>()
        AnimeLibraryUpdateJob.setupTask(context, prefInterval = 24, enabled = false)
        confirmVerified(context)
    }

    @Test
    fun excludesOnlyShowsMatchingSourceAndLibraryRestrictions() {
        assertFalse(eligible(show.copy(favorite = false)))
        assertFalse(eligible(show.copy(fetchType = FetchType.Seasons)))
        val single = show.copy(updateStrategy = AnimeUpdateStrategy.ONLY_FETCH_ONCE)
        assertTrue(eligible(single))
        assertFalse(eligible(single.copy(totalCount = 1)))
        assertFalse(eligible(show.copy(status = SAnime.COMPLETED.toLong()), MANGA_NON_COMPLETED))
        assertFalse(eligible(show.copy(totalCount = 2, seenCount = 1), MANGA_HAS_UNREAD))
        assertFalse(eligible(show.copy(totalCount = 2), MANGA_NON_READ))
        assertTrue(eligible(show, MANGA_NON_READ))
        assertFalse(eligible(show.copy(nextUpdate = end + 1), MANGA_OUTSIDE_RELEASE_PERIOD))
        assertTrue(eligible(show, MANGA_OUTSIDE_RELEASE_PERIOD))
    }

    @Test
    fun skipsRecentRefreshesUntilTheConfiguredDaysPass() {
        assertFalse(eligible(show.copy(lastUpdate = now - TimeUnit.DAYS.toMillis(1)), skipDays = 2))
        assertTrue(eligible(show.copy(lastUpdate = now - TimeUnit.DAYS.toMillis(2)), skipDays = 2))
        assertTrue(eligible(show, skipDays = 2))
    }

    @Test
    fun airingUsesLocalMidnightEvenOnADaylightSavingTransition() {
        val day = airingDay(Instant.parse("2026-03-08T17:00:00Z"), ZoneId.of("America/New_York"))
        assertEquals(Instant.parse("2026-03-08T05:00:00Z").toEpochMilli(), day.first)
        assertEquals(Instant.parse("2026-03-09T04:00:00Z").toEpochMilli(), day.last + 1)
    }

    private fun eligible(anime: Anime, vararg restrictions: String, skipDays: Int = 0) =
        shouldUpdateAnime(anime, restrictions.toSet(), skipDays, now, end)
}
