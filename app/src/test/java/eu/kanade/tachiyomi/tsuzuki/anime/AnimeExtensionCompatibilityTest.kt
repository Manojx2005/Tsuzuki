package eu.kanade.tachiyomi.tsuzuki.anime

import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.coroutines.Continuation

class AnimeExtensionCompatibilityTest {
    @Test
    fun legacySuspendRequestsExposeTheDefaultArgumentBridges() {
        val requests = Class.forName(
            "eu.kanade.tachiyomi.network.RequestsKt",
            false,
            javaClass.classLoader,
        )
        for (url in listOf(String::class.java, HttpUrl::class.java)) {
            val method = requests.getMethod(
                "get\$default",
                OkHttpClient::class.java,
                url,
                Headers::class.java,
                CacheControl::class.java,
                Continuation::class.java,
                Integer.TYPE,
                Any::class.java,
            )
            assertEquals(Any::class.java, method.returnType)
        }
        val post = requests.getMethod(
            "post\$default",
            OkHttpClient::class.java,
            String::class.java,
            Headers::class.java,
            RequestBody::class.java,
            CacheControl::class.java,
            Continuation::class.java,
            Integer.TYPE,
            Any::class.java,
        )
        assertEquals(Any::class.java, post.returnType)
    }

    @Test
    fun linkageFailureReachesTheNormalSourceErrorPath() {
        val linkage = NoSuchMethodError("Missing extension API")
        val error = assertThrows(IllegalStateException::class.java) {
            callAnimeSource { throw linkage }
        }
        assertSame(linkage, error.cause)
    }
}
