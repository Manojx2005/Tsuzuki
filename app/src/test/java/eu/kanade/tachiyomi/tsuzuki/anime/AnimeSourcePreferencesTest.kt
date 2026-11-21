package eu.kanade.tachiyomi.tsuzuki.anime

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.PreferenceScreen
import eu.kanade.tachiyomi.animesource.preferenceKey
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.tsuzuki.anime.backup.animeSourcePreferenceEntries
import eu.kanade.tachiyomi.tsuzuki.anime.backup.animeSourcePreferencesForBackup
import eu.kanade.tachiyomi.tsuzuki.anime.ui.hasAnimeSourceSettings
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnimeSourcePreferencesTest {
    private abstract class Configurable : ConfigurableAnimeSource {
        override fun setupPreferenceScreen(screen: PreferenceScreen) = Unit
    }

    @Test
    fun `only a configurable source gets a settings entry`() {
        assertTrue(hasAnimeSourceSettings(mockk<Configurable>()))
        assertFalse(hasAnimeSourceSettings(mockk<AnimeSource>()))
        assertFalse(hasAnimeSourceSettings(null))
    }

    @Test
    fun `anime source settings use the same source_ key as manga and Aniyomi`() {
        val source = mockk<Configurable> { every { id } returns 7311000000000001L }
        assertEquals("source_7311000000000001", source.preferenceKey())
    }

    private val toBackup: (Map<String, *>) -> List<BackupPreference> = { map ->
        map.map { (key, value) -> BackupPreference(key, StringPreferenceValue(value.toString())) }
    }

    @Test
    fun `backup entries are keyed by source, skip empty files and repeated ids`() {
        val stored = mapOf(
            "source_1" to mapOf("preferred_quality" to "480"),
            "source_2" to emptyMap<String, Any>(),
        )
        val entries = animeSourcePreferenceEntries(listOf(1L, 2L, 1L), { stored[it].orEmpty() }, toBackup)
        assertEquals(listOf("source_1"), entries.map { it.sourceKey })
        assertEquals(
            listOf(BackupPreference("preferred_quality", StringPreferenceValue("480"))),
            entries.single().prefs,
        )
    }

    @Test
    fun `the creator's conversion decides which keys go in`() {
        val stored = mapOf("source_1" to mapOf("__PRIVATE_token" to "x", "preferred_quality" to "480"))
        val publicOnly: (Map<String, *>) -> List<BackupPreference> = { map ->
            toBackup(map.filterKeys { !it.startsWith("__PRIVATE_") })
        }
        val entries = animeSourcePreferenceEntries(listOf(1L), { stored[it].orEmpty() }, publicOnly)
        assertEquals(listOf("preferred_quality"), entries.single().prefs.map { it.key })
    }

    @Test
    fun `nothing is backed up while anime is off`() {
        assertEquals(emptyList<Any>(), animeSourcePreferencesForBackup(toBackup))
    }
}
