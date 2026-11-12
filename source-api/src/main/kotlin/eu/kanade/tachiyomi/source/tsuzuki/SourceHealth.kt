package eu.kanade.tachiyomi.source.tsuzuki

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import tachiyomi.core.common.tsuzuki.withSourceHealthRequest

/** Native extensions may build and execute requests directly from their suspend methods. */
suspend fun <T> Source.withSourceHealth(block: suspend () -> T): T =
    if (this is HttpSource) withSourceHealthRequest(id, block) else block()
