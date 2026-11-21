package eu.kanade.tachiyomi.tsuzuki.anime.debug

import eu.kanade.tachiyomi.animesource.model.ChapterType
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.tsuzuki.anime.debugBuiltInAnimeSources
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** Canned JSON in the shape `scripts/anime-fixture/serve.py` serves; no network. */
class FixtureAnimeSourceTest {

    private val base = "http://127.0.0.1:8765"

    private val animeJson = """
        [
          {"case": "01", "title": "01 plain mp4", "url": "/anime/01", "thumbnail": "$base/media/thumb-01.jpg", "noHosters": false},
          {"case": "16", "title": "16 lib16 no hosters", "url": "/anime/16", "thumbnail": "$base/media/thumb-01.jpg", "noHosters": true}
        ]
    """.trimIndent()

    private fun videosJson(hosters: String) = """{"case": "xx", "noHosters": false, "hosters": [$hosters]}"""

    private fun video(path: String, quality: String = "720p", extra: String = "") =
        """{"url": "$base$path", "quality": "$quality"$extra}"""

    @Test
    fun `the case list splits between the two sources`() {
        FixtureJson.animeList(animeJson, noHosters = false).map { it.title } shouldBe listOf("01 plain mp4")
        FixtureJson.animeList(animeJson, noHosters = true).map { it.title } shouldBe listOf("16 lib16 no hosters")
        FixtureJson.animeList(animeJson, noHosters = false).single().thumbnail_url shouldBe "$base/media/thumb-01.jpg"
    }

    @Test
    fun `episodes carry number, date and preview`() {
        val json = """
            [
              {"url": "/episode/01/2", "name": "Episode 2", "number": 2.0, "dateUpload": 1767830400000, "previewUrl": "$base/media/thumb-01.jpg"},
              {"url": "/episode/01/1", "name": "Episode 1", "number": 1.0, "dateUpload": 1767225600000, "previewUrl": null}
            ]
        """.trimIndent()
        val episodes = FixtureJson.episodes(json)
        episodes.map { it.episode_number } shouldBe listOf(2f, 1f)
        episodes.map { it.date_upload } shouldBe listOf(1767830400000, 1767225600000)
        episodes.map { it.preview_url } shouldBe listOf("$base/media/thumb-01.jpg", null)
        FixtureJson.caseOf(episodes.first().url) shouldBe "01"
        FixtureJson.caseOf("/anime/16") shouldBe "16"
    }

    @Test
    fun `case 03 first hoster throws, second has videos`() {
        val hosters = FixtureJson.hosters(
            videosJson(
                """{"name": "A (throws)", "videos": [], "throws": true}, {"name": "B", "videos": [${video(
                    "/media/ok.mp4",
                )}]}""",
            ),
        )
        hosters.map { it.internalData == FixtureJson.THROWS } shouldBe listOf(true, false)
        hosters[0].videoList shouldBe null
        hosters[1].videoList!!.single().videoUrl shouldBe "$base/media/ok.mp4"
    }

    @Test
    fun `case 04 lazy hoster has no videos until picked`() {
        val hosters = FixtureJson.hosters(
            videosJson(
                """{"name": "A", "videos": [${video("/media/hls/480/index.m3u8", "480p")}], "lazy": true},""" +
                    """{"name": "B", "videos": [${video("/media/hls/720/index.m3u8")}]}""",
            ),
        )
        hosters.map { it.lazy } shouldBe listOf(true, false)
        hosters[0].videoList shouldBe null
        // Picking A parses its own JSON from internalData.
        FixtureJson.videos(
            kotlinx.serialization.json.Json.parseToJsonElement(
                hosters[0].internalData,
            ) as kotlinx.serialization.json.JsonObject,
        )
            .single().resolution shouldBe 480
    }

    @Test
    fun `cases 05, 07, 08, 09 and 11 map every video field`() {
        val json = videosJson(
            """{"name": "F", "videos": [
                ${video("/media/hls/1080/index.m3u8", "1080p")},
                ${video(
                "/header/ok.mp4",
                "720p",
                ""","preferred": true, "headers": {"X-Fixture": "video"},
                  "mpvArgs": [["demuxer-lavf-o", "probesize=5000000"], ["script", "x"]],
                  "subtitleTracks": [{"url": "$base/media/ja.srt", "lang": "ja", "title": "日本語 (SRT)"}],
                  "timestamps": [{"start": 5.0, "end": 15.0, "name": "Opening", "type": "Opening"}]""",
            )}
            ]}""",
        )
        val videos = FixtureJson.hosters(json).single().videoList!!
        videos.map { it.preferred } shouldBe listOf(false, true)
        val rich = videos[1]
        rich.headers!!["X-Fixture"] shouldBe "video"
        rich.mpvArgs shouldBe listOf("demuxer-lavf-o" to "probesize=5000000", "script" to "x")
        rich.subtitleTracks.single().url shouldBe "$base/media/ja.srt"
        rich.subtitleTracks.single().lang shouldBe "日本語 (SRT)"
        rich.timestamps.single().type shouldBe ChapterType.Opening
        rich.timestamps.single().start shouldBe 5.0
        videos[0].headers shouldBe null
    }

    @Test
    fun `case 15 preferred quality moves first, default 720`() {
        val videos = listOf("1080p", "720p", "480p").map { Video(videoUrl = it, videoTitle = it) }
        FixtureJson.preferQuality(videos, FixtureAnimeSource.PREF_QUALITY_DEFAULT).map { it.videoTitle } shouldBe
            listOf("720p", "1080p", "480p")
        FixtureJson.preferQuality(videos, "480").map { it.videoTitle } shouldBe listOf("480p", "1080p", "720p")
        FixtureAnimeSource.PREF_QUALITY_DEFAULT shouldBe "720"
    }

    @Test
    fun `case 16 source overrides none of the hoster methods`() {
        val hosterMethods = setOf("getHosterList", "hosterListRequest", "hosterListParse")
        var type: Class<*> = FixtureNoHosterAnimeSource::class.java
        while (type != AnimeHttpSource::class.java) {
            type.declaredMethods.map { it.name }.filter { it in hosterMethods } shouldBe emptyList()
            type.declaredMethods.any {
                it.name == "getVideoList" &&
                    it.parameterTypes.firstOrNull()?.simpleName == "Hoster"
            } shouldBe
                false
            type = type.superclass
        }
        FixtureAnimeSource::class.java.declaredMethods.any { it.name == "getHosterList" } shouldBe true
    }

    @Test
    fun `debug builds register both fixture sources by name`() {
        val sources = debugBuiltInAnimeSources(debug = true)
        sources.map { it.source.id } shouldBe listOf(FixtureAnimeSources.FIXTURE_ID, FixtureAnimeSources.NO_HOSTER_ID)
        sources.map { it.libVersion }.distinct() shouldBe listOf(16.0)
        sources.map { it.extensionPackage }.distinct() shouldBe listOf("tsuzuki.fixture")
        sources.map { it.source.lang }.distinct() shouldBe listOf("all")
    }
}
