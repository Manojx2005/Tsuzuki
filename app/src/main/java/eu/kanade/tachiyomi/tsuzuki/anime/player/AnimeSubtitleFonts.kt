package eu.kanade.tachiyomi.tsuzuki.anime.player

import android.content.Context
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.AnimeLog
import java.io.File

/**
 * Fonts for mpv's subtitle renderer (AY3). libass on Android has no system font provider ("can't find selected
 * font provider"), so without fonts of our own no subtitle text renders at all, and it can't fall back per glyph
 * between files either. What it does have is a last resort: `<config-dir>/subfont.ttf`, used for any glyph the
 * selected font lacks (mpv-android ships one for this). So:
 * - [SUBTITLE_FONT] (the app's Latin face, Manrope) sits in `mpv/fonts` and is the subtitle/OSD font;
 * - the Japanese subset (kana + JIS X 0208 kanji) is `mpv/subfont.ttf`, the fallback for everything else.
 * Both are OFL (`docs/licenses/OFL-1.1.txt`, `OFL-zenkakugothicnew.txt`); no new font files.
 */
internal object AnimeSubtitleFonts {
    // libass matches the name-table family (ID 1); this variable font's default instance is ExtraLight.
    const val SUBTITLE_FONT = "Manrope ExtraLight"

    /** The files to copy, relative to the mpv config folder. */
    private val files = listOf(
        R.font.lnr_manrope to "fonts/manrope.ttf",
        R.font.lnr_jp_zenkaku_regular to "subfont.ttf",
    )

    /**
     * Copies the fonts when one is missing or the app was installed/updated since the last copy (font resources are
     * compressed in the APK, so their size can't be read without inflating them); returns the fonts folder.
     * The player initialises mpv synchronously, so this runs on the main thread; it copies only once per app
     * install/update (~2.3 MB) and logs how long that took.
     */
    fun install(context: Context, configDir: File): File {
        val dir = File(configDir, "fonts").apply { mkdirs() }
        val marker = File(dir, ".installed")

        @Suppress("DEPRECATION")
        val installed = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime.toString()
        val targets = files.map { (resource, name) -> resource to File(configDir, name) }
        val current = marker.takeIf(File::exists)?.readText()
        if (needsCopy(current, installed, targets.all { it.second.exists() })) {
            val started = System.currentTimeMillis()
            // Earlier AY3 builds put the Japanese face in fonts/, where both renderers would load it again.
            File(dir, "zen-kaku-gothic-new.ttf").delete()
            targets.forEach { (resource, target) ->
                context.resources.openRawResource(resource).use { input ->
                    target.outputStream().use { input.copyTo(it) }
                }
            }
            marker.writeText(installed)
            AnimeLog.i("subtitle fonts copied (+${System.currentTimeMillis() - started} ms)")
        }
        return dir
    }

    fun needsCopy(copiedFor: String?, installed: String, allPresent: Boolean): Boolean =
        !allPresent || copiedFor != installed

    /** The mpv options that point libass and the OSD at [fontsDir]. */
    fun options(fontsDir: File): List<Pair<String, String>> = listOf(
        "sub-fonts-dir" to fontsDir.path,
        "osd-fonts-dir" to fontsDir.path,
        "sub-font" to SUBTITLE_FONT,
        "osd-font" to SUBTITLE_FONT,
    )
}
