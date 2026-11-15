package eu.kanade.tachiyomi.tsuzuki.anime.ui

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.player.MpvAnimePlayerLauncher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class AnimePlayerModuleTest {
    @Test
    fun arm64SelectsMpv() {
        assertSame(MpvAnimePlayerLauncher, selectAnimePlayerLauncher("arm64-v8a", processIs64Bit = true))
    }

    @Test
    fun unsupportedAbisSelectTheNoOpPlayer() {
        val unsupported = listOf(
            "arm64-v8a" to false,
            "armeabi-v7a" to false,
            "x86" to false,
            "x86_64" to true,
            "riscv64" to true,
            null to false,
        )
        for ((abi, processIs64Bit) in unsupported) {
            val launcher = selectAnimePlayerLauncher(abi, processIs64Bit)
            assertNotSame(MpvAnimePlayerLauncher, launcher)
            assertNull(launcher.createBackend())
            assertEquals(R.string.tsz_anime_player_unsupported_device, launcher.unavailableMessage)
        }
    }
}
