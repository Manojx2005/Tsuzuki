package eu.kanade.tachiyomi.tsuzuki.anime.local

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocalAnimeFilesTest {
    @Test
    fun episodeNumberFromCommonFileNames() {
        assertEquals(3.0, localEpisodeNumber("Sample Show - 03.mkv"))
        assertEquals(12.0, localEpisodeNumber("Sample Show S01E12 [1080p].mp4"))
        assertEquals(7.0, localEpisodeNumber("[Group] Sample Show - 07 (x264) [ABCD1234].mkv"))
        assertEquals(5.0, localEpisodeNumber("Sample_Show_EP5.webm"))
        assertEquals(12.5, localEpisodeNumber("Sample Show - 12.5.mp4"))
        assertEquals(2.0, localEpisodeNumber("Sample Show 2019 - 02.mkv"))
    }

    @Test
    fun aNameWithoutANumberHasNone() {
        assertNull(localEpisodeNumber("Movie.mkv"))
        assertNull(localEpisodeNumber("Sample Show (2019) [1080p].mkv"))
    }

    @Test
    fun sidecarSubtitlesPairByBaseName() {
        val siblings = listOf(
            "Show - 01.mkv", "Show - 01.ass", "Show - 01.en.srt", "Show - 02.srt", "cover.jpg", "Show - 01.txt",
        )
        assertEquals(
            listOf("Show - 01.ass" to "", "Show - 01.en.srt" to "en"),
            localSubtitlesFor("Show - 01.mkv", siblings),
        )
        assertEquals(emptyList<Pair<String, String>>(), localSubtitlesFor("Show - 03.mkv", siblings))
    }

    @Test
    fun onlyIssuedContentAddressesAreLocal() {
        val uri = "content://com.android.externalstorage.documents/tree/x/document/x%2Flocalanime%2FShow%2F01.mkv"
        assertFalse(LocalAnimeVideos.isIssued(uri))
        LocalAnimeVideos.issue(uri)
        assertTrue(LocalAnimeVideos.isIssued(uri))
        assertFalse(LocalAnimeVideos.isIssued("content://other.provider/secret"))
        assertFalse(LocalAnimeVideos.isIssued("https://example.com/a.mp4"))
    }
}
