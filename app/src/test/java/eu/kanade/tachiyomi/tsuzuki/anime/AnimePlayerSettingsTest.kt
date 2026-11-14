package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerPreferences
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerSettings
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimePlayerTrack
import eu.kanade.tachiyomi.tsuzuki.anime.player.AnimeTrackKind
import eu.kanade.tachiyomi.tsuzuki.anime.player.animeLanguageOrder
import eu.kanade.tachiyomi.tsuzuki.anime.player.chooseAnimeTrack
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

        fun pick(tracks: List<AnimePlayerTrack>, key: String) =
            chooseAnimeTrack(AnimeTrackKind.SUBTITLE, tracks, key, emptyList(), "")?.track?.id
        assertEquals(8, pick(nextEpisode, english.preferenceKey))
        assertEquals(3, pick(nextEpisode.take(2), english.preferenceKey))
        assertNull(pick(nextEpisode, AnimePlayerPreferences.SUBTITLES_OFF))
    }

    @Test
    fun orderedLanguagesAcceptTagsAndNamesAndPreferFullSubtitlesWithinALanguage() {
        val kind = AnimeTrackKind.SUBTITLE
        val forced = AnimePlayerTrack(1, kind, "ja", "Japanese", "ass", false, forced = true)
        val signs = forced.copy(id = 2, language = "jpn", title = "Signs and songs", forced = false)
        val dialogue = forced.copy(id = 3, language = "Japanese", title = "Dialogue", forced = false)
        val english = dialogue.copy(id = 4, language = "eng")
        val tracks = listOf(english, forced, signs, dialogue)
        assertEquals(listOf("ja", "en", "off"), animeLanguageOrder("Japanese,jpn,ja-JP,eng,off", kind))
        assertEquals(dialogue, chooseAnimeTrack(kind, tracks, null, listOf("jpn", "en"), "")?.track)
        assertEquals(english, chooseAnimeTrack(kind, tracks, null, listOf("en", "ja"), "")?.track)
        val off = checkNotNull(chooseAnimeTrack(kind, tracks, null, listOf("fr", "off", "en"), ""))
        assertNull(off.track)
    }

    @Test
    fun manualTrackChoiceIsScopedToTheAnimeAndOverridesTheLanguageOrder() {
        val options = AnimePlayerPreferences(InMemoryPreferenceStore())
        val kind = AnimeTrackKind.AUDIO
        val japanese = AnimePlayerTrack(1, kind, "jpn", "Original", "aac", false)
        val english = japanese.copy(id = 2, language = "English", title = "Dub")
        val tracks = listOf(japanese, english)
        options.languages(kind).set("ja,en")
        options.trackOverride(40, kind).set(english.preferenceKey)
        val order = animeLanguageOrder(options.languages(kind).get(), kind)
        fun pick(animeId: Long) = chooseAnimeTrack(kind, tracks, options.trackOverride(animeId, kind).get(), order, "")
        assertEquals(english, pick(40)?.track)
        assertEquals(japanese, pick(41)?.track)
        assertEquals("", options.preferredAudio.get())
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
