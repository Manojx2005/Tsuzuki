package eu.kanade.tachiyomi.tsuzuki

import android.app.Activity
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Run with -PtsuzukiTestBuildType=preview against the separate optimized test-library copy. */
@RunWith(AndroidJUnit4::class)
class WebViewRendererRecoveryTest {
    @Test(timeout = 45_000)
    fun rendererDeathClosesOnlyThePageAndAnotherPageCanOpen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".debug")) { "Use the preview copy, not the owner's debug data" }
        val marker = "WebViewRecovery-" + UUID.randomUUID()
        Log.e("TsuzukiWebViewTest", marker)

        fun intent(url: String) = Intent().setClassName(
            context.packageName,
            "eu.kanade.tachiyomi.ui.webview.WebViewActivity",
        ).putExtra("url_key", url).putExtra("title_key", "WebView recovery test")

        ActivityScenario.launch<Activity>(intent("chrome://crash")).use { scenario ->
            val deadline = SystemClock.elapsedRealtime() + 15_000
            while (scenario.state != Lifecycle.State.DESTROYED && SystemClock.elapsedRealtime() < deadline) {
                SystemClock.sleep(100)
            }
            assertEquals("Only the failed page should close", Lifecycle.State.DESTROYED, scenario.state)
        }

        val logs = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("logcat -d --pid=${Process.myPid()}"),
        ).bufferedReader().use { it.readText() }.substringAfter(marker)
        assertTrue("The renderer-death callback must actually run", logs.contains("WebView renderer gone:"))
        assertTrue("The test must have crashed the renderer", logs.contains("crashed=true"))

        ActivityScenario.launch<Activity>(
            intent("data:text/html,<html><body>Recovered WebView</body></html>"),
        ).use { scenario ->
            val content = AtomicReference("")
            val deadline = SystemClock.elapsedRealtime() + 10_000
            while (!content.get().contains("Recovered WebView") && SystemClock.elapsedRealtime() < deadline) {
                val evaluated = CountDownLatch(1)
                scenario.onActivity { activity ->
                    findWebView(activity.window.decorView)?.evaluateJavascript(
                        "document.body ? document.body.textContent : ''",
                    ) {
                        content.set(it)
                        evaluated.countDown()
                    } ?: evaluated.countDown()
                }
                evaluated.await(1, TimeUnit.SECONDS)
                SystemClock.sleep(100)
            }
            assertTrue("A new renderer must display a fresh page", content.get().contains("Recovered WebView"))
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findWebView(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }
}
