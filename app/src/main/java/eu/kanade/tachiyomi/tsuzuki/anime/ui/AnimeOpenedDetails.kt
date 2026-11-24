package eu.kanade.tachiyomi.tsuzuki.anime.ui

import androidx.compose.runtime.saveable.Saver
import eu.kanade.tachiyomi.animesource.model.SAnime

/**
 * AP-PHONE-F1: the catalogue anime whose details are open, kept across Activity recreation (rotation, coming back
 * from the landscape player). Only the identity and what the first frame shows are saved: an [SAnime] itself carries a
 * `memo` JsonObject that a Bundle cannot hold. The details model looks the anime up in the library by source and url
 * and fetches the rest from the source, as it does when the anime is first opened.
 */
internal data class AnimeOpenedDetails(val url: String, val title: String, val thumbnailUrl: String?) {
    fun toSAnime(): SAnime = SAnime.create().also {
        it.url = url
        it.title = title
        it.thumbnail_url = thumbnailUrl
        it.initialized = false
    }

    companion object {
        fun of(anime: SAnime) = AnimeOpenedDetails(anime.url, anime.title, anime.thumbnail_url)

        /** Saves to an ArrayList of strings, which a Bundle holds. */
        val Saver: Saver<AnimeOpenedDetails?, Any> = Saver(
            save = { opened -> opened?.let { arrayListOf(it.url, it.title, it.thumbnailUrl) } },
            restore = { saved -> restore(saved) },
        )

        internal fun restore(saved: Any): AnimeOpenedDetails? {
            val values = saved as? List<*> ?: return null
            val url = values.getOrNull(0) as? String ?: return null
            val title = values.getOrNull(1) as? String ?: return null
            return AnimeOpenedDetails(url, title, values.getOrNull(2) as? String)
        }
    }
}
