package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.presentation.tsuzuki.anime.AnimeLibraryTab
import eu.kanade.presentation.tsuzuki.browse.BrowseMedia
import eu.kanade.presentation.tsuzuki.browse.browsePages
import eu.kanade.presentation.tsuzuki.home.showAnimeWatch
import eu.kanade.tachiyomi.ui.tsuzuki.navigation.isAnimeLibraryKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference

class TsuzukiAnimeGateTest {
    @Test
    fun disabledHidesBrowseWatchAndLibraryDestinations() {
        val browse = browsePages(hideManga = false, hasAnime = true, animeEnabled = false, hasAnimeRepos = true)
        assertFalse(browse.any { it.media == BrowseMedia.ANIME })
        assertFalse(showAnimeWatch(watching = true, animeEnabled = false))
        assertFalse(isAnimeLibraryKey(AnimeLibraryTab.key, animeEnabled = false))
    }

    @Test
    fun enabledRetainsAnimeDestinations() {
        val browse = browsePages(hideManga = false, hasAnime = true, animeEnabled = true)
        assertTrue(browse.any { it.media == BrowseMedia.ANIME })
        assertTrue(showAnimeWatch(watching = true, animeEnabled = true))
        assertTrue(isAnimeLibraryKey(AnimeLibraryTab.key, animeEnabled = true))
    }

    @Test
    fun settingChangesTakeEffectWithoutRecreatingTheGate() = runTest {
        val preference = TestPreference()
        val gate = AnimeFeatureGate(supported = true, preference)
        val updates = mutableListOf<Boolean>()
        backgroundScope.launch { gate.collectChanges { updates.add(it) } }
        runCurrent()
        assertFalse(gate.enabled)

        preference.set(true)
        runCurrent()
        assertTrue(gate.enabled)
        assertTrue(isAnimeLibraryKey(AnimeLibraryTab.key, animeEnabled = gate.enabled))

        preference.set(false)
        runCurrent()
        assertFalse(gate.enabled)
        assertFalse(isAnimeLibraryKey(AnimeLibraryTab.key, animeEnabled = gate.enabled))

        preference.set(true)
        runCurrent()
        assertTrue(gate.enabled)
        assertEquals(listOf(false, true, false, true), updates)
    }

    @Test
    fun unsupportedDevicesStayDisabledEvenWithARestoredPreference() = runTest {
        val preference = TestPreference(initial = true)
        val gate = AnimeFeatureGate(supported = false, preference)
        val updates = mutableListOf<Boolean>()
        backgroundScope.launch { gate.collectChanges { updates.add(it) } }
        runCurrent()

        preference.set(false)
        runCurrent()
        preference.set(true)
        runCurrent()

        assertFalse(gate.enabled)
        assertEquals(listOf(false), updates)
    }

    private class TestPreference(initial: Boolean = false) : Preference<Boolean> {
        private val state = MutableStateFlow(initial)

        override fun key(): String = "tsuzuki_anime_enabled"

        override fun get(): Boolean = state.value

        override fun set(value: Boolean) {
            state.value = value
        }

        override fun isSet(): Boolean = get()

        override fun delete() = set(defaultValue())

        override fun defaultValue(): Boolean = false

        override fun changes() = state

        override fun stateIn(scope: CoroutineScope) = state
    }
}
