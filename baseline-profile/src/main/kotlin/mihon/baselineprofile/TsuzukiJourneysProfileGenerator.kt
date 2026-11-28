package mihon.baselineprofile

import android.util.Log
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * Batch SM1 (docs/SMOOTHNESS_PLAN.md): profile the Tsuzuki journeys the 2026-11-04 profile never reached — Home
 * tabs, the Library media switch (All / Novels / Manga / Anime), Watch, anime details, sheets and Settings.
 * The book Home → Library → details → reader journey stays in [BaselineProfileGenerator].
 *
 * Every journey is its own test, so one that cannot run on the test library does not lose the others. Selectors are
 * English or Japanese text / content descriptions, never coordinates. Collect against a non-minified build
 * (rules must name real classes); see docs/PERFORMANCE.md.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TsuzukiJourneysProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    /** J2 + J8: every Home tab, then the Library filter sheet and Settings › Appearance. */
    @Test
    fun tabsSheetsAndSettings() = rule.collect(TARGET_PACKAGE_NAME) {
        pressHome()
        startActivityAndWait()
        repeat(2) {
            listOf(LIBRARY, UPDATES, BROWSE, MORE, HOME).forEach { tab ->
                device.tapNav(tab)
                device.waitForIdle()
            }
        }
        device.tapNav(LIBRARY)
        if (device.tapIfPresent(By.desc(FILTER))) {
            device.waitForIdle()
            device.pressBack()
            device.waitForIdle()
        }
        device.tapNav(MORE)
        if (device.scrollToAndTap(By.text(SETTINGS))) {
            device.tapIfPresent(By.text(APPEARANCE))
            device.waitForIdle()
            device.pressBack()
            device.pressBack()
        }
        device.tapNav(HOME)
    }

    /** J4: the Library media switch in both directions, with a fling on each page. */
    @Test
    fun libraryMediaSwitch() = rule.collect(TARGET_PACKAGE_NAME) {
        pressHome()
        startActivityAndWait()
        device.tapNav(LIBRARY)
        repeat(2) {
            listOf(ALL, NOVELS, MANGA, ANIME, NOVELS, ALL).forEach { media ->
                if (device.tapIfPresent(By.text(media))) {
                    device.waitForIdle()
                    device.fling()
                }
            }
        }
    }

    /** J5: Watch, an anime from the library, its episodes, Back; the anime Library grid. */
    @Test
    fun watchAndAnimeDetails() = rule.collect(TARGET_PACKAGE_NAME) {
        pressHome()
        startActivityAndWait()
        if (!device.tapIfPresent(By.text(WATCH))) {
            Log.w(TAG, "No Read / Watch switch: anime is off in the test app; Watch journey skipped")
            return@collect
        }
        device.waitForIdle()
        device.fling()
        device.tapNav(LIBRARY)
        if (device.tapIfPresent(By.text(ANIME))) {
            device.waitForIdle()
            device.fling()
            val title = arguments("profileAnimeTitle")
            val target = title?.let {
                device.wait(
                    Until.findObject(By.text(Pattern.compile(Pattern.quote(it), Pattern.CASE_INSENSITIVE))),
                    5_000,
                )
            }
            if (target != null) {
                target.click()
                device.waitForIdle()
                device.fling()
                device.pressBack()
                device.waitForIdle()
            } else {
                Log.w(TAG, "Anime details not profiled: pass profileAnimeTitle of an anime in the test library")
            }
            device.tapIfPresent(By.text(NOVELS))
        }
        device.tapNav(HOME)
        device.tapIfPresent(By.text(READ))
    }

    private fun arguments(name: String): String? =
        androidx.test.platform.app.InstrumentationRegistry.getArguments().getString(name)

    private companion object {
        const val TAG = "TsuzukiProfile"
        val HOME: Pattern = Pattern.compile("(?iu)home|ホーム")
        val LIBRARY: Pattern = Pattern.compile("(?iu)library|ライブラリ")
        val UPDATES: Pattern = Pattern.compile("(?iu)updates|新着")
        val BROWSE: Pattern = Pattern.compile("(?iu)browse|ブラウズ")
        val MORE: Pattern = Pattern.compile("(?iu)more|その他")
        val FILTER: Pattern = Pattern.compile("(?iu)filter|フィルター")
        val SETTINGS: Pattern = Pattern.compile("(?iu)settings|設定")
        val APPEARANCE: Pattern = Pattern.compile("(?iu)appearance|外観")
        val ALL: Pattern = Pattern.compile("(?iu)all|すべて")
        val NOVELS: Pattern = Pattern.compile("(?iu)novels|小説")
        val MANGA: Pattern = Pattern.compile("(?iu)manga|マンガ")
        val ANIME: Pattern = Pattern.compile("(?iu)anime|アニメ")
        val READ: Pattern = Pattern.compile("(?iu)read|読む")
        val WATCH: Pattern = Pattern.compile("(?iu)watch|観る")
    }
}

/**
 * Bottom bar (phone) / rail (tablet) items carry the label as their content description and as text. Compose can
 * replace a node between finding and clicking it (StaleObjectException), so a tap is retried with a fresh lookup.
 */
private fun UiDevice.tapNav(label: Pattern) {
    // Pages reuse nav labels (Browse has an "その他" / Other section): take the match nearest the bottom bar
    // (phones) or the left rail (tablets), and tap its centre: the labelled node itself is not clickable.
    repeat(3) {
        wait(Until.hasObject(By.desc(label)), 3_000)
        val matches = findObjects(By.desc(label)) + findObjects(By.text(label))
        val target = matches.maxByOrNull { node ->
            runCatching {
                val b = node.visibleBounds
                if (displayWidth >
                    displayHeight
                ) {
                    -b.left
                } else {
                    b.top
                }
            }.getOrDefault(Int.MIN_VALUE)
        }
        if (target != null) {
            try {
                val center = target.visibleCenter
                click(center.x, center.y)
                waitForIdle()
                return
            } catch (_: androidx.test.uiautomator.StaleObjectException) {
                waitForIdle()
            }
        }
    }
    error("Navigation item missing: $label")
}

private fun UiDevice.tapIfPresent(selector: BySelector): Boolean = tapFresh(selector)

private fun UiDevice.tapFresh(selector: BySelector): Boolean {
    repeat(3) {
        val found = wait(Until.findObject(selector), 3_000) ?: return false
        try {
            found.click()
            waitForIdle()
            return true
        } catch (_: androidx.test.uiautomator.StaleObjectException) {
            waitForIdle()
        }
    }
    return false
}

/** Scroll the current screen down until [selector] appears (More is longer than a phone screen), then tap it. */
private fun UiDevice.scrollToAndTap(selector: BySelector): Boolean {
    repeat(6) {
        findObject(selector)?.let {
            it.click()
            waitForIdle()
            return true
        }
        val x = displayWidth / 2
        swipe(x, displayHeight * 3 / 4, x, displayHeight / 4, 20)
        waitForIdle()
    }
    return false
}

private fun UiDevice.fling() {
    val x = displayWidth / 2
    swipe(x, displayHeight * 4 / 5, x, displayHeight / 3, 10)
    waitForIdle()
    swipe(x, displayHeight / 3, x, displayHeight * 4 / 5, 10)
    waitForIdle()
}
