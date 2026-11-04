package eu.kanade.tachiyomi.data.updater

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.util.system.isFossBuildType
import eu.kanade.tachiyomi.util.system.isNightlyBuildType
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.release.interactor.GetApplicationRelease
import uy.kohesive.injekt.injectLazy

class AppUpdateChecker {

    private val getApplicationRelease: GetApplicationRelease by injectLazy()

    suspend fun checkForUpdate(forceCheck: Boolean = false): GetApplicationRelease.Result {
        // Disable app update checks for older Android versions that we're going to drop support for
        // if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        //     return GetApplicationRelease.Result.OsTooOld
        // }

        return withIOContext {
            val result = getApplicationRelease.await(
                GetApplicationRelease.Arguments(
                    isFossBuildType,
                    isPreviewBuildType,
                    BuildConfig.COMMIT_COUNT.toInt(),
                    BuildConfig.VERSION_NAME,
                    GITHUB_REPO,
                    forceCheck,
                ),
            )

            result
        }
    }
}

// Tsuzuki ships its own releases; upstream (Mihon / Tsundoku) changes reach users only through
// a Tsuzuki release (docs/UPSTREAM.md). The repo must be public for this API call to see releases.
val GITHUB_REPO: String by lazy {
    if (isNightlyBuildType) {
        "Manojx2005/Tsuzuki"
    } else if (isPreviewBuildType) {
        "Manojx2005/Tsuzuki"
    } else {
        "Manojx2005/Tsuzuki"
    }
}

val RELEASE_TAG: String by lazy {
    if (isPreviewBuildType) {
        "r${BuildConfig.COMMIT_COUNT}"
    } else {
        "v${BuildConfig.VERSION_NAME}"
    }
}

val RELEASE_URL = "https://github.com/$GITHUB_REPO/releases/tag/$RELEASE_TAG"
