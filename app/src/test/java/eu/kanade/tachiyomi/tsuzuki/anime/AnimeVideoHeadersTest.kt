package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.tsuzuki.anime.player.rankAnimeVideos
import io.kotest.matchers.shouldBe
import okhttp3.Headers
import org.junit.jupiter.api.Test

class AnimeVideoHeadersTest {
    private val source = Headers.headersOf("User-Agent", "Source-UA")

    @Test
    fun `a video without headers gets the source headers`() {
        Video(videoUrl = "https://cdn.example/v.m3u8").withDefaultHeaders(source).headers shouldBe source
    }

    @Test
    fun `a video with its own headers keeps exactly those`() {
        val own = Headers.headersOf("Referer", "https://site.example/")
        Video(videoUrl = "https://cdn.example/v.m3u8", headers = own).withDefaultHeaders(source).headers shouldBe own
    }

    @Test
    fun `no source headers leaves the video unchanged`() {
        val video = Video(videoUrl = "https://cdn.example/v.m3u8")
        video.withDefaultHeaders(null) shouldBe video
    }

    @Test
    fun `at equal quality the sources preferred video goes first and source order is otherwise kept`() {
        val a = Video("a", resolution = 720)
        val b = Video("b", resolution = 720, preferred = true)
        val c = Video("c", resolution = 720)
        rankAnimeVideos(listOf(a, b, c), 0) shouldBe listOf(b, a, c)
        rankAnimeVideos(listOf(a, c), 0) shouldBe listOf(a, c)
    }
}
