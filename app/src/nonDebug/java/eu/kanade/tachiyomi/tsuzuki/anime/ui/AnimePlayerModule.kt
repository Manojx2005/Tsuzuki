package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.content.Context
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory

internal class AnimePlayerModule : InjektModule {
    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<AnimePlayerLauncher> { UnavailableAnimePlayer }
    }
}

private object UnavailableAnimePlayer : AnimePlayerLauncher {
    override fun launch(context: Context, request: AnimePlayback.Request): Boolean = false

    override fun createBackend(): AnimePlayerBackend? = null
}
