package eu.kanade.tachiyomi.tsuzuki.anime.player

import eu.kanade.tachiyomi.animesource.model.ChapterType
import eu.kanade.tachiyomi.animesource.model.TimeStamp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class AnimeChaptersTest {
    private val fixtureChapters = listOf(
        AnimeFileChapter("Opening", 0.0),
        AnimeFileChapter("Part A", 20.0),
        AnimeFileChapter("Ending", 70.0),
    )

    @Test
    fun fileChaptersRunToTheNextChapterAndTheEnd() {
        val merged = mergeAnimeChapters(fixtureChapters, emptyList(), emptyList(), 90.0)
        assertEquals(
            listOf(
                AnimeChapter(0.0, 20.0, "Opening"),
                AnimeChapter(20.0, 70.0, "Part A"),
                AnimeChapter(70.0, 90.0, "Ending"),
            ),
            merged,
        )
        // Plain file chapters are not skippable: only typed stamps are.
        assertTrue(merged.skipSegments().isEmpty())
        assertEquals(listOf(0f, 20f / 90f, 70f / 90f), merged.tickFractions(90.0))
        assertEquals("Part A", merged.chapterAt(45.0)?.name)
    }

    @Test
    fun sourceStampsDriveTheSkipButtonAndLeaveGapsUnnamed() {
        val merged = mergeAnimeChapters(
            emptyList(),
            listOf(
                TimeStamp(45.0, 55.0, "Ending", ChapterType.Ending),
                TimeStamp(5.0, 15.0, "Opening", ChapterType.Opening),
            ),
            emptyList(),
            60.0,
        )
        assertEquals(
            listOf(
                AnimeChapter(0.0, 5.0, null),
                AnimeChapter(5.0, 15.0, "Opening", AniSkipKind.OPENING),
                AnimeChapter(15.0, 45.0, null),
                AnimeChapter(45.0, 55.0, "Ending", AniSkipKind.ENDING),
                AnimeChapter(55.0, 60.0, null),
            ),
            merged,
        )
        assertEquals(
            listOf(AniSkipSegment(AniSkipKind.OPENING, 5.0, 15.0), AniSkipSegment(AniSkipKind.ENDING, 45.0, 55.0)),
            merged.skipSegments(),
        )
        assertEquals(AniSkipKind.OPENING, merged.skipSegments().atPosition(6.0)?.kind)
        assertNull(merged.skipSegments().atPosition(20.0))
        // No tick at 0 for the unnamed lead-in.
        assertEquals(listOf(5f, 15f, 45f, 55f).map { it / 60f }, merged.tickFractions(60.0))
    }

    @Test
    fun stampTypesMapToSkipKinds() {
        assertEquals(AniSkipKind.OPENING, ChapterType.Opening.skipKind())
        assertEquals(AniSkipKind.OPENING, ChapterType.MixedOp.skipKind())
        assertEquals(AniSkipKind.ENDING, ChapterType.Ending.skipKind())
        assertEquals(AniSkipKind.RECAP, ChapterType.Recap.skipKind())
        assertNull(ChapterType.Other.skipKind())
        val merged = mergeAnimeChapters(
            emptyList(),
            listOf(
                TimeStamp(0.0, 30.0, "Recap", ChapterType.Recap),
                TimeStamp(30.0, 40.0, "Cold open", ChapterType.Other),
                TimeStamp(40.0, 60.0, "", ChapterType.MixedOp),
            ),
            emptyList(),
            100.0,
        )
        assertEquals(
            listOf(
                AnimeChapter(0.0, 30.0, "Recap", AniSkipKind.RECAP),
                AnimeChapter(30.0, 40.0, "Cold open", null),
                AnimeChapter(40.0, 60.0, null, AniSkipKind.OPENING),
                AnimeChapter(60.0, 100.0, null),
            ),
            merged,
        )
    }

    @Test
    fun aniSkipInsideAFileChapterSplitsIt() {
        val merged = mergeAnimeChapters(
            listOf(AnimeFileChapter("Part A", 0.0), AnimeFileChapter("Part B", 600.0)),
            emptyList(),
            listOf(AniSkipSegment(AniSkipKind.OPENING, 90.0, 180.0)),
            1400.0,
        )
        assertEquals(
            listOf(
                AnimeChapter(0.0, 90.0, "Part A"),
                AnimeChapter(90.0, 180.0, null, AniSkipKind.OPENING),
                AnimeChapter(180.0, 600.0, "Part A"),
                AnimeChapter(600.0, 1400.0, "Part B"),
            ),
            merged,
        )
        assertEquals("Part A", merged.chapterAt(200.0)?.name)
        assertEquals(AniSkipKind.OPENING, merged.chapterAt(100.0)?.skip)
    }

    @Test
    fun sourceStampsWinOverOverlappingAniSkip() {
        val merged = mergeAnimeChapters(
            emptyList(),
            listOf(TimeStamp(60.0, 150.0, "OP", ChapterType.Opening)),
            listOf(
                AniSkipSegment(AniSkipKind.OPENING, 55.0, 145.0),
                AniSkipSegment(AniSkipKind.ENDING, 1300.0, 1390.0),
            ),
            1400.0,
        )
        assertEquals(
            listOf(
                AniSkipSegment(AniSkipKind.OPENING, 60.0, 150.0),
                AniSkipSegment(AniSkipKind.ENDING, 1300.0, 1390.0),
            ),
            merged.skipSegments(),
        )
        // AniSkip's opening overlapped the source's, so none of it is offered.
        assertNull(merged.chapterAt(57.0)?.skip)
        assertEquals("OP", merged.chapterAt(100.0)?.name)
    }

    @Test
    fun overlappingSourceStampsKeepTheEarlierOne() {
        val merged = mergeAnimeChapters(
            emptyList(),
            listOf(
                TimeStamp(10.0, 100.0, "Opening", ChapterType.Opening),
                TimeStamp(50.0, 120.0, "Recap", ChapterType.Recap),
            ),
            emptyList(),
            300.0,
        )
        assertEquals(
            listOf(
                AnimeChapter(0.0, 10.0, null),
                AnimeChapter(10.0, 100.0, "Opening", AniSkipKind.OPENING),
                AnimeChapter(100.0, 120.0, "Recap", AniSkipKind.RECAP),
                AnimeChapter(120.0, 300.0, null),
            ),
            merged,
        )
    }

    @Test
    fun outOfRangeStampsAreDroppedOrClamped() {
        val merged = mergeAnimeChapters(
            listOf(AnimeFileChapter("Late", 500.0), AnimeFileChapter("Bad", Double.NaN)),
            listOf(
                TimeStamp(-5.0, 10.0, "Negative", ChapterType.Opening),
                TimeStamp(20.0, 10.0, "Backwards", ChapterType.Opening),
                TimeStamp(Double.NaN, 30.0, "NaN", ChapterType.Opening),
                TimeStamp(120.0, 130.0, "After end", ChapterType.Ending),
                TimeStamp(80.0, 500.0, "Credits", ChapterType.Ending),
                TimeStamp(0.4, 15.0, "Recap", ChapterType.Recap),
            ),
            listOf(AniSkipSegment(AniSkipKind.OPENING, 100.0, 110.0)),
            100.0,
        )
        assertEquals(
            listOf(
                AnimeChapter(0.0, 15.0, "Recap", AniSkipKind.RECAP),
                AnimeChapter(15.0, 80.0, null),
                AnimeChapter(80.0, 100.0, "Credits", AniSkipKind.ENDING),
            ),
            merged,
        )
    }

    @Test
    fun unknownDurationKeepsStampsAndLeavesTheLastChapterOpen() {
        val stamps = listOf(TimeStamp(5.0, 15.0, "Opening", ChapterType.Opening))
        for (duration in listOf(0.0, -1.0, Double.NaN)) {
            val merged = mergeAnimeChapters(listOf(AnimeFileChapter("Part A", 0.0)), stamps, emptyList(), duration)
            assertEquals(
                listOf(
                    AnimeChapter(0.0, 5.0, "Part A"),
                    AnimeChapter(5.0, 15.0, "Opening", AniSkipKind.OPENING),
                    AnimeChapter(15.0, Double.POSITIVE_INFINITY, "Part A"),
                ),
                merged,
            )
            assertTrue(merged.tickFractions(duration).isEmpty())
            assertEquals(AniSkipKind.OPENING, merged.skipSegments().atPosition(10.0)?.kind)
        }
    }

    @Test
    fun nothingNamedGivesNoChapters() {
        assertTrue(mergeAnimeChapters(emptyList(), emptyList(), emptyList(), 90.0).isEmpty())
        assertTrue(mergeAnimeChapters(listOf(AnimeFileChapter(" ", 0.0)), emptyList(), emptyList(), 90.0).isEmpty())
    }

    @Test
    fun theSettingStopsAniSkipWhenTheVideoHasChaptersOrStamps() {
        val stamp = listOf(TimeStamp(5.0, 15.0, "Opening", ChapterType.Opening))
        assertFalse(shouldRequestAniSkip(AniSkipMode.OFF, false, emptyList(), emptyList()))
        assertTrue(shouldRequestAniSkip(AniSkipMode.BUTTON, true, emptyList(), emptyList()))
        assertFalse(shouldRequestAniSkip(AniSkipMode.BUTTON, true, fixtureChapters, emptyList()))
        assertFalse(shouldRequestAniSkip(AniSkipMode.AUTO, true, emptyList(), stamp))
        assertTrue(shouldRequestAniSkip(AniSkipMode.AUTO, false, fixtureChapters, stamp))
    }

    @Test
    fun theSettingDefaultsOnAndPersists() {
        val store = InMemoryPreferenceStore()
        assertTrue(AnimePlayerPreferences(store).get().aniSkipIgnoreWithChapters)
        AnimePlayerPreferences(store).set(AnimePlayerSettings(aniSkipIgnoreWithChapters = false))
        assertFalse(AnimePlayerPreferences(store).get().aniSkipIgnoreWithChapters)
    }

    @Test
    fun aSourceStampIsOfferedEvenWithAniSkipOff() {
        val state = AnimePlayerState(
            loading = false,
            position = 6.0,
            duration = 60.0,
            timestamps = listOf(TimeStamp(5.0, 15.0, "Opening", ChapterType.Opening)),
        ).withMergedChapters()
        assertEquals(AniSkipMode.OFF, state.settings.aniSkipMode)
        assertEquals(AniSkipSegment(AniSkipKind.OPENING, 5.0, 15.0), state.activeAniSkip)
        assertNull(state.copy(position = 20.0).activeAniSkip)
    }
}
