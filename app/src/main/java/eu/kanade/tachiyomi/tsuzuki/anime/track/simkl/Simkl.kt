package eu.kanade.tachiyomi.tsuzuki.anime.track.simkl

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.Request
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.track.model.Track as DomainTrack

/**
 * A5.4 (board `10-anime/AnimeTracking`, Simkl row): Simkl for anime only. It is a [BaseTracker] so sign-in, the
 * logo and the Settings row work like the other trackers, but it is registered only with [SIMKL_ID] in the anime
 * tracking (never in `TrackerManager.trackers`), so book screens never offer it. The book-tracking members are
 * therefore unused and refuse.
 */
internal class Simkl : BaseTracker(SIMKL_ID, "Simkl"), SimklTokenStore {

    private val json: Json by injectLazy()

    val authorizedClient by lazy { simklClient(client, SimklInterceptor(this, json)) }

    val anime by lazy { SimklAnimeApi(authorizedClient, json) }

    override fun getLogo(): Int = R.drawable.brand_simkl

    /** Signed in only when a token is stored (the username alone is not enough). */
    override val isLoggedIn: Boolean
        get() = super.isLoggedIn && read() != null

    /** Exchanges the consent code (PKCE) for tokens; any failure leaves the user signed out. */
    suspend fun login(code: String) = withIOContext {
        try {
            val request = Request.Builder()
                .url("${SimklAuth.API_URL}/oauth2/token")
                .header("User-Agent", SimklAuth.userAgent)
                .post(
                    FormBody.Builder()
                        .add("grant_type", "authorization_code")
                        .add("code", code)
                        .add("code_verifier", SimklAuth.takeVerifier())
                        .add("redirect_uri", SimklAuth.REDIRECT_URI)
                        .add("client_id", SimklAuth.CLIENT_ID)
                        .build(),
                )
                .build()
            val token = client.newCall(request).awaitSuccess().use {
                json.decodeFromString<SimklOAuth>(it.body.string())
            }.stamped()
            write(token)
            saveCredentials("user", token.accessToken)
            saveDisplayUsername(fetchUserName() ?: "Simkl")
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            logout()
        }
    }

    private suspend fun fetchUserName(): String? = runCatching {
        authorizedClient.newCall(GET("${SimklAuth.API_URL}/users/settings")).awaitSuccess().use {
            json.parseToJsonElement(it.body.string()).jsonObject["user"]?.jsonObject?.get("name")
                ?.jsonPrimitive?.contentOrNull
        }
    }.getOrNull()

    override fun logout() {
        super.logout()
        clear()
    }

    override fun read(): SimklOAuth? = trackPreferences.trackToken(this).get().takeIf(String::isNotBlank)?.let {
        runCatching { json.decodeFromString<SimklOAuth>(it) }.getOrNull()
    }

    override fun write(token: SimklOAuth) = trackPreferences.trackToken(this).set(json.encodeToString(token))

    override fun clear() = trackPreferences.trackToken(this).delete()

    // Book tracking: never offered (see the class note).
    override fun getStatusList(): List<Long> = emptyList()
    override fun getStatus(status: Long): StringResource? = null
    override fun getReadingStatus(): Long = 1
    override fun getRereadingStatus(): Long = 1
    override fun getCompletionStatus(): Long = 2
    override fun getScoreList(): ImmutableList<String> = persistentListOf()
    override fun displayScore(track: DomainTrack): String = track.score.toInt().toString()
    override suspend fun update(track: Track, didReadChapter: Boolean): Track = unsupported()
    override suspend fun bind(track: Track, hasReadChapters: Boolean): Track = unsupported()
    override suspend fun search(query: String): List<TrackSearch> = unsupported()
    override suspend fun refresh(track: Track): Track = unsupported()
    override suspend fun login(username: String, password: String) = login(password)

    private fun unsupported(): Nothing = throw UnsupportedOperationException("Simkl tracks anime only")

    companion object {
        /** Tsuzuki's own id: Aniyomi's Simkl id (101) is Tsundoku's NovelList here. */
        const val SIMKL_ID = 300L

        val instance: Simkl by lazy { Simkl() }
    }
}
