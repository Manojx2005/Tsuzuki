package eu.kanade.tachiyomi.tsuzuki.anime.debug

import androidx.preference.ListPreference
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.PreferenceScreen
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.Headers
import java.io.IOException

/**
 * Debug-only built-in anime sources backed by the fixture server (card AY0, `scripts/anime-fixture/`).
 * Main code finds this object by name (`debugBuiltInAnimeSources`); preview and release builds don't contain it.
 * Reach the server from a device with `adb reverse tcp:8765 tcp:8765`.
 */
@Suppress("unused") // Loaded by reflection from main code.
internal object FixtureAnimeSources {
    /** Fixed ids, also written in scripts/anime-fixture/README.md. */
    const val FIXTURE_ID = 7_311_000_000_000_001L
    const val NO_HOSTER_ID = 7_311_000_000_000_016L

    fun all(): List<AnimeSource> = listOf(FixtureAnimeSource(), FixtureNoHosterAnimeSource())
}

/**
 * Catalogue and episodes, shared by both fixture sources. It deliberately overrides **no** hoster
 * method, so [FixtureNoHosterAnimeSource] stays a pure lib16 "video list only" source.
 */
internal abstract class FixtureCatalogueSource : AnimeHttpSource() {
    final override val baseUrl = "http://127.0.0.1:8765"
    final override val lang = "all"
    final override val supportsLatest = true

    /** True for the lib16 source (case 16 only), false for the hoster source (cases 01–15). */
    protected abstract val noHosters: Boolean

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "TsuzukiFixture/1")
        .add("Referer", "$baseUrl/")

    protected suspend fun fetch(path: String): String =
        client.newCall(GET(baseUrl + path, headers)).awaitSuccess().use { it.body.string() }

    private suspend fun cases(): List<SAnime> = FixtureJson.animeList(fetch("/api/anime.json"), noHosters)

    override suspend fun getPopularAnime(page: Int) = AnimesPage(cases(), hasNextPage = false)

    override suspend fun getLatestUpdates(page: Int) = getPopularAnime(page)

    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList) =
        AnimesPage(cases().filter { it.title.contains(query, ignoreCase = true) }, hasNextPage = false)

    override suspend fun getAnimeDetails(anime: SAnime): SAnime =
        cases().firstOrNull { it.url == anime.url } ?: anime

    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> =
        FixtureJson.episodes(fetch("/api/episodes/${FixtureJson.caseOf(anime.url)}.json"))

    override fun getAnimeUrl(anime: SAnime): String = baseUrl + "/api/videos/${FixtureJson.caseOf(anime.url)}.json"

    override fun getEpisodeUrl(episode: SEpisode): String =
        baseUrl + "/api/videos/${FixtureJson.caseOf(episode.url)}.json"
}

/** Cases 01–15: hosters, lazy and failing hosters, headers, tracks, timestamps, mpvArgs. */
internal class FixtureAnimeSource : FixtureCatalogueSource(), ConfigurableAnimeSource {
    override val name = "Tsuzuki Fixture"
    override val id = FixtureAnimeSources.FIXTURE_ID
    override val noHosters = false

    override suspend fun getHosterList(episode: SEpisode): List<Hoster> =
        FixtureJson.hosters(fetch("/api/videos/${FixtureJson.caseOf(episode.url)}.json"))

    override suspend fun getVideoList(hoster: Hoster): List<Video> {
        if (hoster.internalData == FixtureJson.THROWS) throw IOException("Fixture hoster ${hoster.hosterName} failed")
        return FixtureJson.videos(Json.parseToJsonElement(hoster.internalData).jsonObject)
    }

    override fun List<Video>.sortVideos(): List<Video> = FixtureJson.preferQuality(this, preferredQuality())

    private fun preferredQuality(): String =
        getSourcePreferences().getString(PREF_QUALITY, PREF_QUALITY_DEFAULT) ?: PREF_QUALITY_DEFAULT

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        screen.addPreference(
            ListPreference(screen.context).apply {
                key = PREF_QUALITY
                title = "Preferred quality"
                entries = arrayOf("1080p", "720p", "480p")
                entryValues = arrayOf("1080", "720", "480")
                setDefaultValue(PREF_QUALITY_DEFAULT)
                summary = "%s"
            },
        )
    }

    companion object {
        const val PREF_QUALITY = "preferred_quality"
        const val PREF_QUALITY_DEFAULT = "720"
    }
}

/** Case 16: a lib16 source that only implements `getVideoList(episode)` (AY1's hoster-method detection). */
internal class FixtureNoHosterAnimeSource : FixtureCatalogueSource() {
    override val name = "Tsuzuki Fixture lib16 no hosters"
    override val id = FixtureAnimeSources.NO_HOSTER_ID
    override val noHosters = true

    override suspend fun getVideoList(episode: SEpisode): List<Video> =
        FixtureJson.firstHosterVideos(fetch("/api/videos/${FixtureJson.caseOf(episode.url)}.json"))
}
