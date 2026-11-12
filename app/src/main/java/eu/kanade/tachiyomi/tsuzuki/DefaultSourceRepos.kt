package eu.kanade.tachiyomi.tsuzuki

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.jsplugin.JsPluginManager
import logcat.LogPriority
import mihon.domain.extension.repository.ExtensionStoreRepository
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Source repositories added once on first launch of the **debug** build only (owner 2026-11-10: shipped builds
 * come with no repositories; the user adds their own in Browse).
 *
 * Each repo is seeded only once: if the user later removes it, it stays removed.
 * A failed seed (e.g. offline on first launch) is retried on the next launch.
 * Repositories an earlier shipped build already added are the user's data and are left alone.
 */
object DefaultSourceRepos {

    /** LNReader plugin index (novels), run by the bundled QuickJS plugin host. */
    const val LNREADER_NAME = "LNReader"
    const val LNREADER_URL =
        "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist/plugins.min.json"

    /** Keiyoushi extension repo (manga / manhwa). repo.json points at their v2 protobuf index. */
    const val KEIYOUSHI_URL = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/repo.json"

    private const val KEY_LNREADER = "tsuzuki_seeded_repo_lnreader"
    private const val KEY_KEIYOUSHI = "tsuzuki_seeded_repo_keiyoushi"

    suspend fun seed(
        preferenceStore: PreferenceStore = Injekt.get(),
        jsPluginManager: JsPluginManager = Injekt.get(),
        extensionStores: ExtensionStoreRepository = Injekt.get(),
        enabled: Boolean = BuildConfig.DEBUG,
    ) {
        if (!enabled) return

        val lnreaderSeeded = preferenceStore.getBoolean(KEY_LNREADER, false)
        if (!lnreaderSeeded.get()) {
            // addRepository de-duplicates by URL and refreshes the plugin list itself.
            jsPluginManager.addRepository(LNREADER_NAME, LNREADER_URL)
            lnreaderSeeded.set(true)
        }

        val keiyoushiSeeded = preferenceStore.getBoolean(KEY_KEIYOUSHI, false)
        if (!keiyoushiSeeded.get()) {
            extensionStores.insert(KEIYOUSHI_URL, isNovel = false)
                .onSuccess {
                    keiyoushiSeeded.set(true)
                    runCatching { Injekt.get<ExtensionManager>().findAvailableExtensions() }
                }
                .onFailure { logcat(LogPriority.WARN, it) { "Tsuzuki: could not add Keiyoushi repo, will retry" } }
        }
    }
}
