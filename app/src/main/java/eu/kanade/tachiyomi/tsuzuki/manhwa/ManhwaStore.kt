package eu.kanade.tachiyomi.tsuzuki.manhwa

import kotlinx.coroutines.flow.combine
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.manga.model.Manga

internal class ManhwaStore(preferences: PreferenceStore) {
    // Manga IDs are local to this database and can be reassigned on restore.
    private val measurements = preferences.getStringSet(Preference.appStateKey("tsz_manhwa_measurements"))
    private val overrides = preferences.getStringSet(Preference.appStateKey("tsz_manhwa_overrides"))

    fun observe() = combine(measurements.changes(), overrides.changes(), ::Snapshot)

    fun snapshot() = Snapshot(measurements.get(), overrides.get())

    fun isManhwa(manga: Manga, language: String?): Boolean = snapshot().isManhwa(manga, language)

    fun needsPageEvidence(mangaId: Long): Boolean =
        "$mangaId:true" !in measurements.get() && overrides.get().valueFor(mangaId) == null

    @Synchronized
    fun recordPageEvidence(mangaId: Long, longStrip: Boolean) {
        val current = measurements.get()
        if (current.valueFor(mangaId) == true) return
        val updated = current.withValue(mangaId, longStrip)
        if (current != updated) measurements.set(updated)
    }

    @Synchronized
    fun setOverride(mangaId: Long, manhwa: Boolean?) {
        overrides.set(overrides.get().withValue(mangaId, manhwa))
    }

    internal data class Snapshot(
        private val measurements: Set<String>,
        private val overrides: Set<String>,
    ) {
        fun overrideFor(mangaId: Long): Boolean? = overrides.valueFor(mangaId)

        fun isManhwa(manga: Manga, language: String?): Boolean = !manga.isNovel &&
            isManhwa(manga.genre, language, measurements.valueFor(manga.id), overrideFor(manga.id))
    }
}

private fun Set<String>.valueFor(id: Long): Boolean? = when {
    "$id:true" in this -> true
    "$id:false" in this -> false
    else -> null
}

private fun Set<String>.withValue(id: Long, value: Boolean?): Set<String> {
    val updated = this - "$id:true" - "$id:false"
    return if (value == null) updated else updated + "$id:$value"
}
