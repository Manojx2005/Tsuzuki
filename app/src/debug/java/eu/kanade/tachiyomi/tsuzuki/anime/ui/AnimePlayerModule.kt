package eu.kanade.tachiyomi.tsuzuki.anime.ui

import eu.kanade.tachiyomi.tsuzuki.anime.debug.MpvAnimePlayerLauncher
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory

internal class AnimePlayerModule : InjektModule {
    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<AnimePlayerLauncher> { MpvAnimePlayerLauncher }
    }
}
