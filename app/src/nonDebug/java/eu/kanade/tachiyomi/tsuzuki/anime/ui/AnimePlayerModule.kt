package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.content.Context
import eu.kanade.tachiyomi.animesource.model.Video
import tachiyomi.domain.items.episode.model.Episode
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory

internal class AnimePlayerModule : InjektModule {
    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<AnimePlayerLauncher> { UnavailableAnimePlayer }
    }
}

private object UnavailableAnimePlayer : AnimePlayerLauncher {
    override fun launch(context: Context, video: Video, episode: Episode?): Boolean = false

    override fun createBackend(): AnimePlayerBackend? = null
}
