package eu.kanade.presentation.tsuzuki.browse

import eu.kanade.tachiyomi.extension.model.Extension

/**
 * How much one extension repository provides, taken from the available-extension lists already loaded.
 * [sources] is the sum of each extension's sources; some indexes list none, then only [extensions] is meaningful.
 */
data class TsuzukiRepoCounts(val extensions: Int, val sources: Int)

/** Available extensions grouped by their repository index URL. A repository with no loaded extension is absent. */
fun tsuzukiRepoCounts(available: List<Extension.Available>): Map<String, TsuzukiRepoCounts> {
    return available
        .groupBy { it.store.indexUrl }
        .mapValues { (_, list) -> TsuzukiRepoCounts(list.size, list.sumOf { it.sources.size }) }
}
