package eu.kanade.tachiyomi.tsuzuki.anime.track.simkl

import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Every Simkl API call needs `client_id`, `app-name`, `app-version` and a User-Agent; calls on a user's data add
 * the Bearer token. Access tokens last 7 days; this interceptor is the single owner that refreshes them (a second
 * refresher would invalidate the first one's token, as Simkl's docs warn).
 */
internal class SimklInterceptor(
    private val tokens: SimklTokenStore,
    private val json: Json,
) : Interceptor {
    private val lock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = original.url.newBuilder()
            .setQueryParameter("client_id", SimklAuth.CLIENT_ID)
            .setQueryParameter("app-name", SimklAuth.APP_NAME)
            .setQueryParameter("app-version", SimklAuth.appVersion)
            .build()
        val builder = original.newBuilder().url(url).header("User-Agent", SimklAuth.userAgent)
        currentToken(chain)?.let { builder.header("Authorization", "Bearer ${it.accessToken}") }
        return chain.proceed(builder.build())
    }

    private fun currentToken(chain: Interceptor.Chain): SimklOAuth? = synchronized(lock) {
        val token = tokens.read() ?: return null
        if (!token.isExpired(System.currentTimeMillis())) return token
        val refresh = token.refreshToken ?: return token
        val request = Request.Builder()
            .url("${SimklAuth.API_URL}/oauth2/token")
            .header("User-Agent", SimklAuth.userAgent)
            .post(
                FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refresh)
                    .add("client_id", SimklAuth.CLIENT_ID)
                    .build(),
            )
            .build()
        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            return token
        }
        response.use {
            if (it.code == 400 || it.code == 401) {
                // The grant is gone (revoked or six months unused): sign the user out instead of retrying.
                tokens.clear()
                return null
            }
            if (!it.isSuccessful) return token
            val fresh = json.decodeFromString<SimklOAuth>(it.body.string()).stamped(token.refreshToken)
            tokens.write(fresh)
            fresh
        }
    }
}

/** Where the tokens live: the tracker's private preference, read fresh each time. */
internal interface SimklTokenStore {
    fun read(): SimklOAuth?
    fun write(token: SimklOAuth)
    fun clear()
}

/** Adds the absolute expiry; a refresh may omit the (non-rotating) refresh token, so keep the old one. */
internal fun SimklOAuth.stamped(previousRefresh: String? = null, now: Long = System.currentTimeMillis()) = copy(
    refreshToken = refreshToken ?: previousRefresh,
    expiresAt = if (expiresIn > 0) now + expiresIn * 1000 else 0,
)

internal fun simklClient(base: OkHttpClient, interceptor: SimklInterceptor): OkHttpClient =
    base.newBuilder().addInterceptor(interceptor).build()
