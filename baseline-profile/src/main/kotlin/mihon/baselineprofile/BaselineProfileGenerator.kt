package mihon.baselineprofile

import android.content.Intent
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        val arguments = InstrumentationRegistry.getArguments()
        val bookTitle = requireNotNull(arguments.getString("profileBookTitle")) {
            "Restore a test library and pass profileBookTitle for a cached novel before collecting the profile"
        }
        val readerText = requireNotNull(arguments.getString("profileReaderText")) {
            "Pass profileReaderText: text visible in the cached chapter, to verify the reader actually loaded"
        }

        rule.collect(TARGET_PACKAGE_NAME) {
            pressHome()
            startActivityAndWait()

            // Profile the real Tsuzuki Home -> Library -> details -> reader path.
            // The test app must use English or Japanese and have a cached novel ready to open.
            device.waitAndClick(By.text(Pattern.compile("(?iu)(all books|すべての本)\\s*→?")))
            device.waitForIdle()

            val x = device.displayWidth * 3 / 4
            val top = device.displayHeight * 2 / 5
            val bottom = device.displayHeight * 4 / 5
            device.swipe(x, bottom, x, top, 20)
            device.waitForIdle()
            device.swipe(x, top, x, bottom, 20)
            device.waitForIdle()

            device.waitAndClick(By.text(Pattern.compile(Pattern.quote(bookTitle), Pattern.CASE_INSENSITIVE)))
            device.waitAndClick(By.text(Pattern.compile("(?iu)(start|resume|開始|再開)\\s*·.*")))
            check(device.wait(Until.hasObject(By.textContains(readerText)), 10_000)) {
                "The selected novel did not render the expected cached chapter text"
            }
            device.waitForIdle()
            device.pressBack()
            device.pressBack()

            listOf("SHOW_RECENTLY_UPDATED", "SHOW_RECENTLY_READ", "SHOW_CATALOGUES", "EXTENSIONS").forEach { action ->
                startActivityAndWait(
                    Intent("eu.kanade.tachiyomi.$action").apply {
                        setClassName(TARGET_PACKAGE_NAME, "eu.kanade.tachiyomi.ui.main.MainActivity")
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    },
                )
                device.waitForIdle()
            }
        }
    }
}

private fun UiDevice.waitAndClick(by: BySelector) {
    checkNotNull(wait(Until.findObject(by), 10_000)) { "Profile setup is missing UI element: $by" }.click()
}
