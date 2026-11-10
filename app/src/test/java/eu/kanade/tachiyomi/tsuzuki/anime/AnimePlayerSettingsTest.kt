package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerPreferences
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerSettings
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerTrack
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimeTrackKind
import eu.kanade.tachiyomi.tsuzuki.anime.player.preferredAnimeTrack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class AnimePlayerSettingsTest {
    @Test
    fun pictureInPictureDefaultsOnAndPersistsAcrossPlayerSessions() {
        val store = InMemoryPreferenceStore()
        val preference = AnimePlayerPreferences(store).pictureInPicture
        assertTrue(preference.get())
        preference.set(false)
        assertFalse(AnimePlayerPreferences(store).pictureInPicture.get())
    }

    @Test
    fun settingsPersistAcrossSessionsAndClampInvalidValues() {
        val store = InMemoryPreferenceStore()
        val options = AnimePlayerPreferences(store)
        options.set(
            AnimePlayerSettings(
                speed = 3.5f,
                subtitleScale = 1.5f,
                subtitleDelay = Float.NaN,
                hardwareDecoding = false,
                seekGesture = false,
                brightnessGesture = false,
                volumeGesture = true,
            ),
        )
        options.preferredSubtitle.set(AnimePlayerPreferences.SUBTITLES_OFF)
        val reloaded = AnimePlayerPreferences(store)
        assertEquals(
            AnimePlayerSettings(
                speed = 3f,
                subtitleScale = 1.5f,
                subtitleDelay = 0f,
                hardwareDecoding = false,
                seekGesture = false,
                brightnessGesture = false,
                volumeGesture = true,
            ),
            reloaded.get(),
        )
        assertEquals(AnimePlayerPreferences.SUBTITLES_OFF, reloaded.preferredSubtitle.get())
    }

    @Test
    fun trackPreferenceFollowsMetadataWhenNativeIdsChange() {
        val english = AnimePlayerTrack(2, AnimeTrackKind.SUBTITLE, "eng", "Dialogue", "ass", false)
        val englishSigns = english.copy(id = 3, title = "Signs")
        val japanese = english.copy(id = 4, language = "jpn")
        val nextEpisode = listOf(japanese, englishSigns, english.copy(id = 8))

        assertEquals(8, preferredAnimeTrack(nextEpisode, english.preferenceKey)?.id)
        assertEquals(3, preferredAnimeTrack(nextEpisode.take(2), english.preferenceKey)?.id)
        assertNull(preferredAnimeTrack(nextEpisode, AnimePlayerPreferences.SUBTITLES_OFF))
    }

    @Test
    fun fasterPlaybackStoresElapsedWatchTimeAndMediaPositionSeparately() {
        val progress = AnimePlaybackProgress()
        progress.onDuration(100.0)
        progress.onPosition(10.0, playbackRate = 2f)
        progress.onPosition(14.0, playbackRate = 2f)
        assertEquals(AnimePlaybackCheckpoint(14.0, 100.0, 2), progress.checkpoint())
    }
}
