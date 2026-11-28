package eu.kanade.tachiyomi.tsuzuki.anime.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnimeExternalPlayerTest {
    private fun launch(
        choice: AnimeExternalChoice,
        url: String = "https://cdn.example/ep1.mp4",
        position: Double = 61.5,
        headers: Map<String, String> = mapOf("Referer" to "https://site.example/", "User-Agent" to "UA/1"),
        subtitles: List<AnimeExternalLaunch.Subtitle> = emptyList(),
    ) = animeExternalLaunch(choice, url, "Show — Episode 1", position, headers, subtitles)

    @Test
    fun choiceRoundTripsThroughThePreference() {
        listOf(AnimeExternalChoice.Tsuzuki, AnimeExternalChoice.Ask, AnimeExternalChoice.App("org.videolan.vlc"))
            .forEach { assertEquals(it, AnimeExternalChoice.from(it.stored)) }
        assertEquals(AnimeExternalChoice.Tsuzuki, AnimeExternalChoice.from("  "))
    }

    @Test
    fun tsuzukiPlaysItself() {
        assertNull(launch(AnimeExternalChoice.Tsuzuki))
    }

    @Test
    fun knownPlayersGetTheirActivityAndPositionInMilliseconds() {
        val mpv = launch(AnimeExternalChoice.App("is.xyz.mpv"))!!
        assertEquals("is.xyz.mpv.MPVActivity", mpv.className)
        assertEquals(61_500L, mpv.positionMs)
        assertEquals(listOf("Referer", "https://site.example/", "User-Agent", "UA/1"), mpv.headers)
        assertEquals("video/mp4", mpv.mimeType)
        assertFalse(mpv.isVlc)

        val mx = launch(AnimeExternalChoice.App("com.mxtech.videoplayer.ad"))!!
        assertEquals("com.mxtech.videoplayer.ad.ActivityScreen", mx.className)

        val vlc = launch(AnimeExternalChoice.App("org.videolan.vlc"))!!
        assertTrue(vlc.isVlc)
    }

    @Test
    fun unknownAppAndChooserHaveNoActivity() {
        assertNull(launch(AnimeExternalChoice.App("com.example.player"))!!.className)
        val ask = launch(AnimeExternalChoice.Ask)!!
        assertNull(ask.packageName)
        assertNull(ask.className)
    }

    @Test
    fun startOfEpisodeAndBadPositionsSendZero() {
        assertEquals(0L, launch(AnimeExternalChoice.Ask, position = 0.0)!!.positionMs)
        assertEquals(0L, launch(AnimeExternalChoice.Ask, position = Double.NaN)!!.positionMs)
        assertEquals(0L, launch(AnimeExternalChoice.Ask, position = -3.0)!!.positionMs)
    }

    @Test
    fun onlyNetworkSubtitlesAreSent() {
        val subs = listOf(
            AnimeExternalLaunch.Subtitle("https://cdn.example/en.vtt", "English"),
            AnimeExternalLaunch.Subtitle("/data/user/0/app/cache/anime-subs/a.srt", "Japanese"),
            AnimeExternalLaunch.Subtitle("data:text/vtt;base64,AAAA", "German"),
        )
        assertEquals(listOf(subs.first()), launch(AnimeExternalChoice.Ask, subtitles = subs)!!.subtitles)
    }

    @Test
    fun downloadedFilesAreSharedButSavedHlsPlaysHere() {
        val file = launch(AnimeExternalChoice.Ask, url = "/storage/emulated/0/Tsuzuki/anime/ep1/video.mkv")!!
        assertTrue(file.isLocalFile)
        assertEquals("video/x-matroska", file.mimeType)
        assertNull(launch(AnimeExternalChoice.Ask, url = "/storage/emulated/0/Tsuzuki/anime/ep1/index.m3u8"))
        // A network playlist is fine: the other app fetches the segments itself.
        assertEquals(
            "application/x-mpegURL",
            launch(AnimeExternalChoice.Ask, url = "https://cdn.example/a.m3u8?t=1")!!.mimeType,
        )
    }

    @Test
    fun mimeTypeFallsBackToAnyVideo() {
        assertEquals("video/*", animeExternalMimeType("https://cdn.example/stream?id=4"))
        assertEquals("video/webm", animeExternalMimeType("https://cdn.example/a.WEBM#t=3"))
    }

    @Test
    fun mxPlayerCompletionMarksTheWholeEpisode() {
        val result = parseAnimeExternalResult(
            mapOf("end_by" to "playback_completion", "position" to 0, "duration" to 1_440_000)::get,
        )
        assertEquals(AnimeExternalResult(1440.0, 1440.0, completed = true), result)
    }

    @Test
    fun mxPlayerAndMpvStoppedByTheUser() {
        val mx =
            parseAnimeExternalResult(mapOf("end_by" to "user", "position" to 600_000, "duration" to 1_440_000)::get)
        assertEquals(AnimeExternalResult(600.0, 1440.0, completed = false), mx)
        val mpv = parseAnimeExternalResult(mapOf("position" to 90_000, "duration" to 1_440_000)::get)
        assertEquals(AnimeExternalResult(90.0, 1440.0, completed = false), mpv)
    }

    @Test
    fun vlcReportsLongExtras() {
        val vlc = parseAnimeExternalResult(mapOf("extra_position" to 1_300_000L, "extra_duration" to 1_440_000L)::get)
        assertEquals(AnimeExternalResult(1300.0, 1440.0, completed = false), vlc)
    }

    @Test
    fun nothingUsableSavesNothing() {
        assertNull(parseAnimeExternalResult(emptyMap<String, Any>()::get))
        assertNull(parseAnimeExternalResult(mapOf("position" to 5_000, "duration" to 0)::get))
        assertNull(parseAnimeExternalResult(mapOf("end_by" to "playback_completion", "duration" to 0)::get))
        assertNull(parseAnimeExternalResult(mapOf("position" to "12", "duration" to "40")::get))
    }

    @Test
    fun positionPastTheEndIsClamped() {
        val result = parseAnimeExternalResult(mapOf("position" to 2_000_000, "duration" to 1_440_000)::get)
        assertEquals(1440.0, result!!.position)
    }
}
