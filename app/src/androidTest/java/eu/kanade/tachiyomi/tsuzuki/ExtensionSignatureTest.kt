package eu.kanade.tachiyomi.tsuzuki

import android.os.ParcelFileDescriptor
import android.os.Process
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mihon.domain.extension.model.ExtensionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Uses a copied real extension APK; never installs or uninstalls any source. */
@RunWith(AndroidJUnit4::class)
class ExtensionSignatureTest {
    @Test(timeout = 45_000)
    fun realSignedArchiveIsReadAndWrongStoreKeyIsRejected() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".debug")) { "Use only the separate preview copy" }
        val apk = File(context.externalCacheDir, "tsuzuki-signature-test.apk")
        assertTrue("Push the saved test source APK into preview external cache", apk.isFile)
        val info = ExtensionLoader.getArchivePackageInfo(context, apk)
        assertNotNull("The real APK must be recognized as an extension", info)
        val signatures = ExtensionLoader.getSignatures(info!!).orEmpty()
        assertTrue("Real extension signing certificates must be readable", signatures.isNotEmpty())
        assertTrue(signatures.all { it.length == 64 })
        val wrongKey = "0".repeat(64)
        assertFalse(signatures.contains(wrongKey))

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val executor = Executors.newSingleThreadExecutor()
        ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1")).use { server ->
            val served = executor.submit {
                server.accept().use { socket ->
                    socket.soTimeout = 10_000
                    val input = socket.getInputStream().bufferedReader()
                    while (!input.readLine().isNullOrEmpty()) { /* Consume the request headers. */ }
                    socket.getOutputStream().use { output ->
                        val headers = "HTTP/1.1 200 OK\r\nContent-Length: ${apk.length()}\r\n" +
                            "Connection: close\r\n\r\n"
                        output.write(headers.toByteArray())
                        apk.inputStream().use { it.copyTo(output) }
                    }
                }
            }
            val listing = Extension.Available(
                name = "Signature rejection test", pkgName = "tsuzuki.signature.test",
                versionName = "1", versionCode = 1, libVersion = 1.4, lang = "en",
                isNsfw = false, isNovel = false, sources = emptyList(),
                apkUrl = "http://127.0.0.1:${server.localPort}/extension.apk", iconUrl = "",
                store = ExtensionStore(
                    indexUrl = "https://test.invalid/repo",
                    name = "Wrong-key test store",
                    badgeLabel = "TEST",
                    signingKey = wrongKey,
                    contact = ExtensionStore.Contact("https://test.invalid", null),
                    isLegacy = false,
                    extensionListUrl = null,
                ),
            )
            try {
                val marker = "ExtensionSignature-" + System.nanoTime()
                Log.i("TsuzukiInstallerTest", marker)
                val result = runBlocking {
                    withTimeout(20_000) {
                        ExtensionInstaller(context, scope).downloadAndInstall(listing)
                            .first { it == InstallStep.Error || it == InstallStep.Installed }
                    }
                }
                assertEquals("A valid APK from the wrong signing key must never install", InstallStep.Error, result)
                served.get(10, TimeUnit.SECONDS)
                val logs = ParcelFileDescriptor.AutoCloseInputStream(
                    instrumentation.uiAutomation.executeShellCommand("logcat -d --pid=${Process.myPid()}"),
                ).bufferedReader().use { it.readText() }.substringAfter(marker)
                assertTrue(
                    "The actual store-key rejection must run, rather than an unrelated download failure",
                    logs.contains("Extension isn't signed with the key of Wrong-key test store"),
                )
                assertFalse(File(context.cacheDir, "extension_${listing.pkgName}.apk").exists())
                assertFalse(File(context.filesDir, "exts/${listing.pkgName}.ext").exists())
            } finally {
                scope.cancel()
                executor.shutdownNow()
                File(context.cacheDir, "extension_${listing.pkgName}.apk").delete()
            }
        }
    }
}
