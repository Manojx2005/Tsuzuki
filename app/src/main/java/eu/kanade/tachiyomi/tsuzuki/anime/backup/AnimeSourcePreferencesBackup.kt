package eu.kanade.tachiyomi.tsuzuki.anime.backup

import eu.kanade.tachiyomi.animesource.sourcePreferences
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.source.anime.repository.AnimeSourceRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Anime source settings for a backup (AY2). They live in `source_<id>` like manga source settings, and the
 * restorer already writes `BackupSourcePreferences` back by that name, so they only need to be **written**.
 * Every anime source the app has seen counts (the saved source list), not only the loaded ones, so a backup
 * taken before any extension loaded in this process still has them. Empty while anime is off.
 *
 * [convert] is the backup creator's own conversion (app-state keys dropped, private keys kept or dropped as the
 * backup options say).
 */
internal fun animeSourcePreferencesForBackup(
    convert: (Map<String, *>) -> List<BackupPreference>,
): List<BackupSourcePreferences> {
    if (!TsuzukiAnime.inBackups) return emptyList()
    val ids = try {
        // The creator's source-settings step isn't suspending; this is one small database read on its IO thread.
        runBlocking { Injekt.get<AnimeSourceRepository>().awaitAll().map { it.id } }
    } catch (e: Exception) {
        AnimeLog.w("Could not list anime sources for the backup", e)
        return emptyList()
    }
    return animeSourcePreferenceEntries(ids, { key -> sourcePreferences(key).all }, convert)
}

internal fun animeSourcePreferenceEntries(
    ids: List<Long>,
    read: (String) -> Map<String, *>,
    convert: (Map<String, *>) -> List<BackupPreference>,
): List<BackupSourcePreferences> = ids.distinct()
    .map { id -> "source_$id" }
    .map { key -> BackupSourcePreferences(key, convert(read(key))) }
    .filter { it.prefs.isNotEmpty() }
