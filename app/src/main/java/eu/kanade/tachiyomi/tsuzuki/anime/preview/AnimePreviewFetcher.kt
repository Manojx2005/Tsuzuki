package eu.kanade.tachiyomi.tsuzuki.anime.preview

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeSourceManager
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.callAnimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okio.Buffer
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class AnimePreviewKeyer : Keyer<AnimeEpisodePreview> {
    override fun key(data: AnimeEpisodePreview, options: Options) = data.cacheKey
}

internal class AnimePreviewFetcher(
    private val data: AnimeEpisodePreview,
    private val options: Options,
    private val loader: ImageLoader,
) : Fetcher {
    override suspend fun fetch(): ImageFetchResult? = withContext(Dispatchers.IO) {
        if (!TsuzukiAnime.enabled) return@withContext null
        val frames = Injekt.get<AnimeFrameStore>()
        val captured = frames.file(data.animeId, data.episodeId)
        try {
            // Only a published frame (version > 0): a shared-artwork key must never cache one episode's frame.
            if (data.frameVersion > 0 && captured.isFile) {
                load(Buffer().write(captured.readBytes()), DataSource.DISK)?.let { return@withContext it }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A concurrent cleanup may remove a frame after the existence check.
            logcat(LogPriority.WARN) { "Could not read captured anime frame (${e.javaClass.simpleName}); trying fallback" }
        }
        val network = frames.network.value
        val allowed = data.networkAllowed && previewNetworkAllowed(frames.mode.get(), network.isWifi)
        val urls = previewUrls(data.previewUrl, data.coverUrl, allowed)
        if (urls.isEmpty()) return@withContext null
        val sources = Injekt.get<AnimeSourceManager>()
        sources.ensureLoaded()
        val source = sources.get(data.sourceId)?.source as? AnimeHttpSource
        if (source == null) {
            val cover = data.coverUrl?.toHttpUrlOrNull() ?: return@withContext null
            if (!previewNetworkAllowed(frames.mode.get(), frames.network.value.isWifi)) return@withContext null
            return@withContext load(cover.toString())
        }
        for (url in urls) {
            try {
                val httpUrl = url.toHttpUrlOrNull() ?: continue
                // The extension's client carries its cookies, interceptors and source pacing, including CDN hosts.
                val (client, headers) = callAnimeSource { source.client to source.headers }
                if (!previewNetworkAllowed(frames.mode.get(), frames.network.value.isWifi)) break
                val bytes = coroutineScope {
                    val call = client.newCall(Request.Builder().url(httpUrl).headers(headers).build())
                    // await() covers cancellation until headers; keep cancellation attached while reading the body.
                    val cancelRead = launch(start = CoroutineStart.UNDISPATCHED) {
                        try { awaitCancellation() } finally { call.cancel() }
                    }
                    try {
                        call.await().use { response ->
                            if (!response.isSuccessful) return@use null
                            val input = response.body.source()
                            val buffer = Buffer()
                            val limit = 16L * 1024 * 1024
                            while (buffer.size <= limit) {
                                coroutineContext.ensureActive()
                                if (input.read(buffer, minOf(8192, limit + 1 - buffer.size)) == -1L) break
                            }
                            if (buffer.size <= limit) buffer.readByteArray() else null
                        }
                    } finally {
                        cancelRead.cancel()
                    }
                }
                bytes?.let { load(Buffer().write(it), DataSource.NETWORK) }?.let { return@withContext it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Never log image URLs, headers or response bodies.
                coroutineContext.ensureActive()
                logcat(LogPriority.WARN) { "Could not load anime episode artwork (${e.javaClass.simpleName}); trying fallback" }
            }
        }
        null
    }

    private suspend fun load(model: Any, source: DataSource? = null): ImageFetchResult? {
        val result = loader.execute(
            ImageRequest.Builder(options.context).data(model).size(options.size)
                .scale(options.scale).precision(options.precision).build(),
        )
        if (result is SuccessResult) return ImageFetchResult(result.image, result.isSampled, source ?: result.dataSource)
        logcat(LogPriority.WARN) { "Anime episode artwork could not be decoded; trying fallback" }
        return null
    }

    class Factory : Fetcher.Factory<AnimeEpisodePreview> {
        override fun create(data: AnimeEpisodePreview, options: Options, imageLoader: ImageLoader): Fetcher =
            AnimePreviewFetcher(data, options, imageLoader)
    }
}
