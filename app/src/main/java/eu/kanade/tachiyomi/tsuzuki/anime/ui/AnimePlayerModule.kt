package eu.kanade.tachiyomi.tsuzuki.anime.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeSourceManager
import eu.kanade.tachiyomi.tsuzuki.anime.player.MpvAnimePlayerLauncher
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal class AnimePlayerModule : InjektModule {
    override fun InjektRegistrar.registerInjectables() {
        // One process-wide instance: extensions are loaded once and shared by the player, catalogue and downloads.
        addSingletonFactory { AnimeSourceManager.create(get<Application>()) }
        addSingletonFactory<AnimePlayerLauncher> {
            selectAnimePlayerLauncher(Build.SUPPORTED_ABIS.firstOrNull(), Process.is64Bit())
        }
    }
}

internal fun supportsAnimePlayback(
    primaryAbi: String? = Build.SUPPORTED_ABIS.firstOrNull(),
    processIs64Bit: Boolean = Process.is64Bit(),
): Boolean = primaryAbi == "arm64-v8a" && processIs64Bit

internal fun selectAnimePlayerLauncher(primaryAbi: String?, processIs64Bit: Boolean): AnimePlayerLauncher =
    if (supportsAnimePlayback(primaryAbi, processIs64Bit)) MpvAnimePlayerLauncher else UnavailableAnimePlayer

private object UnavailableAnimePlayer : AnimePlayerLauncher {
    override val unavailableMessage = R.string.tsz_anime_player_unsupported_device

    override fun launch(context: Context, request: AnimePlayback.Request): Boolean = false

    override fun createBackend(): AnimePlayerBackend? = null
}
