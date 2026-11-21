package eu.kanade.tachiyomi.tsuzuki.update

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

/**
 * [items] once [scanned] is true, else null (PERF-START-2-F2). Both inputs are the manager's own state, written
 * in that order (items first, then the flag), so the first non-null emission always carries the completed scan.
 * A list derived through a separate `stateIn` collector could still hold the pre-scan value when the flag flips,
 * which let the source manager declare itself ready with the extension sources missing.
 */
internal fun <T> afterScan(items: StateFlow<T>, scanned: StateFlow<Boolean>): Flow<T?> =
    combine(items, scanned) { value, done -> if (done) value else null }
