package eu.kanade.tachiyomi.tsuzuki.update

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

/**
 * [items] once [scanned] is true, else null (PERF-START-2-F2). Both inputs are the manager's own state, written
 * in that order (items first, then the flag). The two collectors can deliver notifications out of order, so
 * read the authoritative item state once the flag is visible instead of using the last collected snapshot.
 */
internal fun <T> afterScan(items: StateFlow<T>, scanned: StateFlow<Boolean>): Flow<T?> =
    combine(items, scanned) { _, done -> if (done) items.value else null }
