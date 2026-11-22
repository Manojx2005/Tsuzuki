package eu.kanade.presentation.tsuzuki.browse

import android.content.Intent
import eu.kanade.presentation.util.Screen

internal fun isAnimeRepoLink(scheme: String?, host: String?): Boolean = when (scheme) {
    "aniyomi", "animetail" -> host == "add-repo"
    "tachiyomi", "tsuzuki" -> host == "add-anime-repo"
    else -> false
}

internal fun animeRepoLinkScreen(intent: Intent): Screen? {
    if (intent.action != Intent.ACTION_VIEW) return null
    val uri = intent.data ?: return null
    if (!uri.isHierarchical || !isAnimeRepoLink(uri.scheme, uri.host)) return null
    return AnimeExtensionReposScreen(uri.getQueryParameter("url").orEmpty())
}
