package eu.kanade.presentation.tsuzuki.startup

/**
 * Raised when the first screen has its data, so the startup animation's "…" loading loop can end.
 * Home marks it after its first load; MainActivity's own `ready` flag covers other start screens.
 */
object StartupSignal {
    @Volatile
    var homeReady: Boolean = false
        private set

    fun markHomeReady() {
        homeReady = true
    }
}
