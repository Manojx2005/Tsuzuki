package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.anime.player.animeMpvLoadCommand
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnimeMpvLoadTest {
    @Test
    fun `source demuxer options reach loadfile instead of opening disguised HLS as PNG`() {
        val video =
            Video("https://test.invalid/video.m3u8", "HD", mpvArgs = listOf("demuxer-lavf-o" to "force_mpegts=1"))
        assertArrayEquals(
            arrayOf("loadfile", video.videoUrl, "replace", "0", "demuxer-lavf-o=%14%force_mpegts=1,start=%4%42.0"),
            animeMpvLoadCommand(video, 42.0),
        )
    }

    @Test
    fun `nested options preserve commas quotes and multibyte text`() {
        val value = "a=1,b=\"\u65e5\u672c\""
        val video = Video("https://test.invalid/video", "HD", mpvArgs = listOf("demuxer-lavf-o" to value))
        assertEquals("demuxer-lavf-o=%14%$value,start=%3%0.0", animeMpvLoadCommand(video, Double.NaN).last())
    }

    @Test
    fun `the next file has its own start and no previous source options`() {
        val video = Video("https://test.invalid/video", "HD")
        assertEquals("start=%3%0.0", animeMpvLoadCommand(video, -10.0).last())
    }

    @Test
    fun `length quoting matches JNI modified UTF8 for surrogate pairs`() {
        val value = "\uD83D\uDE00"
        val video = Video("https://test.invalid/video", "HD", mpvArgs = listOf("title" to value))
        assertEquals("title=%6%$value,start=%3%0.0", animeMpvLoadCommand(video, 0.0).last())
    }
}
