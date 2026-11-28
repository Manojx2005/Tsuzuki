package eu.kanade.tachiyomi.tsuzuki.anime.track.simkl

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.util.PkceUtil
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.security.SecureRandom
import java.util.Base64

/**
 * A5.4: Tsuzuki's own Simkl registration (owner, 2026-11-28; Simkl app #8276561, "Mobile, desktop & browser apps",
 * AUTH V2). The client id is public by design ("safe on GitHub"); this app type has no client secret, so nothing
 * here is a credential. User tokens never leave the app's private preferences.
 */
internal object SimklAuth {
    const val CLIENT_ID = "5e530858a6a8ec3d832ca2695c71433ce5be85e0311565a4c41de76d6182c8b9"
    const val REDIRECT_URI = "tsuzuki://simkl-auth"
    const val API_URL = "https://api.simkl.com"
    private const val AUTHORIZE_URL = "https://simkl.com/oauth2/authorize"
    private const val ISSUER = "https://simkl.com"
    const val APP_NAME = "tsuzuki"
    val appVersion: String get() = BuildConfig.VERSION_NAME
    val userAgent: String get() = "Tsuzuki/${BuildConfig.VERSION_NAME}"

    @Volatile private var verifier = ""

    @Volatile private var state = ""

    /** The consent page (simkl.com, not the API host); PKCE S256 and a CSRF state are mandatory in AUTH V2. */
    fun authUrl(): Uri {
        val codes = PkceUtil.generateS256Codes()
        verifier = codes.codeVerifier
        state = randomState()
        return AUTHORIZE_URL.toUri().buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "media:read media:write")
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", codes.codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
    }

    /** The callback is ours only when its state matches this sign-in and its issuer is Simkl. */
    fun acceptCallback(returnedState: String?, issuer: String?): Boolean =
        state.isNotEmpty() && returnedState == state && (issuer == null || issuer == ISSUER)

    /** The verifier for the code exchange, used once. */
    fun takeVerifier(): String = verifier.also {
        verifier = ""
        state = ""
    }

    private fun randomState(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}

/** Simkl's token response; [expiresAt] is filled in by the app when it is received. */
@Serializable
internal data class SimklOAuth(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 0,
    val expiresAt: Long = 0,
) {
    /** Refresh a minute early so a request never goes out with a token that dies on the way. */
    fun isExpired(now: Long): Boolean = expiresAt in 1..(now + 60_000)
}

/**
 * A5.4: `tsuzuki://simkl-auth?code=…&state=…&iss=…` from the consent page (TrackLoginActivity hook). A callback
 * whose state or issuer is not this sign-in's is ignored; "Don't allow" and other errors leave the user signed out.
 */
internal suspend fun handleSimklCallback(data: Map<String, String?>) {
    if (!SimklAuth.acceptCallback(data["state"], data["iss"])) return
    val code = data["code"]
    if (code != null && data["error"] == null) {
        Simkl.instance.login(code)
    } else {
        SimklAuth.takeVerifier()
        Simkl.instance.logout()
    }
}
