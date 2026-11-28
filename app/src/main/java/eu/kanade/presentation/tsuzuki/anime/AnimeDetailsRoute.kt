package eu.kanade.presentation.tsuzuki.anime

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLibraryStore
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeSourceManager
import eu.kanade.tachiyomi.tsuzuki.anime.RegisteredAnimeSource
import eu.kanade.tachiyomi.tsuzuki.anime.toSourceAnime
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimeDetailsScreen
import eu.kanade.tachiyomi.tsuzuki.anime.ui.AnimePlayback
import kotlinx.coroutines.CancellationException
import tachiyomi.domain.entries.anime.model.Anime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.ConcurrentHashMap

/**
 * Owner 2026-11-23: anime should open like books. From the anime Library, details open inside the main navigator (as
 * book details do), so the Library cover moves into the details cover with the book screens' shared-cover motion.
 * Everything else still opens AnimeCatalogueActivity, and so does this route when the anime or its source cannot be
 * loaded, so the missing-source notice stays in one place.
 */
internal data class AnimeDetailsRoute(val animeId: Long) : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val library = remember { Injekt.get<AnimeLibraryStore>() }
        val sources = remember { Injekt.get<AnimeSourceManager>() }
        // Drawn on the first frame when the Library handed over a loaded entry, so the cover has a target to move to.
        val primedRow = remember { primedAnime.remove(animeId) }
        val primed = remember { primedRow?.let { sources.readyDetails(it) } }
        // The stored row goes to the details screen too, so its episodes load on the first frame (no pop-in).
        var row by remember { mutableStateOf(primedRow) }
        val details by produceState(primed) {
            if (value != null) return@produceState
            value = try {
                sources.ensureLoaded()
                library.getAnime(animeId)?.also { row = it }?.let { sources.readyDetails(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AnimeLog.w("Could not open anime $animeId in place", e)
                null
            }
            if (value == null) {
                openAnime(context, animeId)
                navigator.pop()
            }
        }
        val ready = details
        if (ready == null) {
            Box(Modifier.fillMaxSize().tsuzukiBackground(TsuzukiTheme.tokens))
            return
        }
        AnimeDetailsScreen(
            entry = ready.first,
            anime = ready.second,
            library = library,
            resumeUrl = null,
            onBack = { navigator.pop() },
            sharedCoverKey = animeSharedCoverKey(animeId),
            savedAnime = row,
        ) { episode, track ->
            AnimePlayback.launchEpisode(context, episode.id, track)
        }
    }
}

private val primedAnime = ConcurrentHashMap<Long, Anime>()

/** Opens [anime] from the Library in the main navigator; [push] is the navigator's push. */
internal fun openAnimeInPlace(anime: Anime, push: (AnimeDetailsRoute) -> Unit) {
    primedAnime[anime.id] = anime
    push(AnimeDetailsRoute(anime.id))
}

internal fun animeSharedCoverKey(animeId: Long) = "tsuzuki-anime-cover-$animeId"

private fun AnimeSourceManager.readyDetails(anime: Anime): Pair<RegisteredAnimeSource, SAnime>? =
    get(anime.source)?.let { it to anime.toSourceAnime() }

/**
 * Opens an anime's details from a screen in the main navigator (Updates, Library, History) in place, like book details;
 * falls back to the details Activity where there is no navigator.
 */
@Composable
internal fun rememberAnimeDetailsOpener(): (Long) -> Unit {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    return remember(navigator, context) {
        { id -> navigator?.push(AnimeDetailsRoute(id)) ?: openAnime(context, id) }
    }
}
