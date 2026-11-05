package eu.kanade.tachiyomi.extension.model

import android.graphics.drawable.Drawable
import eu.kanade.tachiyomi.source.Source
import mihon.domain.extension.model.ContentWarning
import mihon.domain.extension.model.ExtensionStore
import tachiyomi.domain.source.model.StubSource

sealed class Extension {

    abstract val name: String
    abstract val pkgName: String
    abstract val versionName: String
    abstract val versionCode: Long
    abstract val libVersion: Double
    abstract val lang: String?
    abstract val contentWarning: ContentWarning
    abstract val isNovel: Boolean

    data class Installed(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String,
        override val contentWarning: ContentWarning,
        override val isNovel: Boolean,
        val pkgFactory: String?,
        val sources: List<Source>,
        val icon: Drawable?,
        val hasUpdate: Boolean = false,
        val isObsolete: Boolean = false,
        val isShared: Boolean,
        val store: ExtensionStore? = null,
    ) : Extension()

    data class Available(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String,
        override val contentWarning: ContentWarning,
        override val isNovel: Boolean,
        val sources: List<Source>,
        val apkUrl: String,
        val iconUrl: String,
        val store: ExtensionStore,
    ) : Extension() {

        data class Source(
            val id: Long,
            val lang: String,
            val name: String,
            val baseUrl: String,
        ) {
            fun toStubSource(): StubSource {
                return StubSource(
                    id = this.id,
                    lang = this.lang,
                    name = this.name,
                )
            }
        }
    }

    data class Untrusted(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        val signatureHash: String,
        override val lang: String? = null,
        override val contentWarning: ContentWarning = ContentWarning.SAFE,
        override val isNovel: Boolean = false,
    ) : Extension()

    /**
     * An installed extension that could not be loaded for a reason other than trust. Kept so the user
     * can see why and copy the report, or uninstall it (Mihon #3953).
     */
    data class Failed(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        val reason: Reason,
        /** Reason specific text: the unsupported lib version, or the class that could not be loaded. */
        val detail: String? = null,
        val stackTrace: String? = null,
        val icon: Drawable? = null,
        val isShared: Boolean = true,
        override val isNovel: Boolean = false,
        override val lang: String? = null,
        override val contentWarning: ContentWarning = ContentWarning.SAFE,
    ) : Extension() {

        enum class Reason {
            MISSING_VERSION,
            UNSUPPORTED_LIB_VERSION,
            NOT_SIGNED,
            CLASS_LOADER,
            MISSING_SOURCE_CLASS,
            SOURCE_CLASS_NOT_FOUND,
            SOURCE_LOAD_ERROR,
        }
    }

    data class JsPlugin(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String,
        override val contentWarning: ContentWarning,
        override val isNovel: Boolean,
        val sources: List<Available.Source>,
        val iconUrl: String,
        val repoUrl: String,
        val isInstalled: Boolean,
        val hasUpdate: Boolean,
    ) : Extension()
}
