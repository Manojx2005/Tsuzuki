package eu.kanade.tachiyomi.tsuzuki.update

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** How long a background library update waits for installed extensions and JS plugins to load. */
internal val SOURCES_READY_TIMEOUT: Duration = 30.seconds

/**
 * Waits until [initialized] is true (the source manager has loaded extensions and JS plugins), for at most
 * [timeout]. Returns whether the sources are ready.
 *
 * A library update started by WorkManager in a cold process otherwise looks up sources while they are
 * still loading, gets `null`, and skips those books for the whole run (PERF-START-2-F). The time limit
 * keeps one broken extension from holding the update forever; the run then goes on as it did before.
 */
internal suspend fun awaitSourcesReady(
    initialized: StateFlow<Boolean>,
    timeout: Duration = SOURCES_READY_TIMEOUT,
): Boolean {
    if (initialized.value) return true
    return withTimeoutOrNull(timeout) { initialized.first { it } } ?: false
}
