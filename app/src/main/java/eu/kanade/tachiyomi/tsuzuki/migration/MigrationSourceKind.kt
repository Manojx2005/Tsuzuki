package eu.kanade.tachiyomi.tsuzuki.migration

import tachiyomi.domain.manga.interactor.GetManga

// Migration targets must serve the same kind of book: a novel only migrates to novel sources and a manga only to
// manga sources. The saved target list (`migration_sources`) is shared by both kinds, so each screen filters it
// and saving one kind's selection keeps the other kind's picks.

/** The kinds of book (`true` = novel, `false` = manga) in a migration; empty when none could be read. */
internal suspend fun migrationBookKinds(ids: Collection<Long>, getManga: GetManga): Set<Boolean> =
    ids.mapNotNull { getManga.await(it)?.isNovel }.toSet()

/** Whether a source can be a target for these kinds of book. Unknown kinds keep every source, as before. */
internal fun isMigrationTarget(sourceIsNovel: Boolean, bookKinds: Set<Boolean>): Boolean =
    bookKinds.isEmpty() || sourceIsNovel in bookKinds

/**
 * The saved target list after this screen's choice: [selected] (in its order) replaces the ids this screen showed
 * ([shown]); saved ids it did not show, such as the other kind's sources, are kept after it.
 */
internal fun mergeMigrationSources(saved: List<Long>, shown: Set<Long>, selected: List<Long>): List<Long> =
    selected + saved.filter { it !in shown && it !in selected }
