package eu.kanade.tachiyomi.tsuzuki.extension

import android.content.Context
import android.content.pm.PackageInfo
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.extension.installer.Installer
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.network.NetworkHelper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ExtensionInstallerTest {
    @TempDir
    lateinit var cache: File

    private val context = mockk<Context>()
    private val preferences = mockk<BasePreferences>(relaxed = true)
    private val network = mockk<NetworkHelper>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val archive = mockk<PackageInfo>()
    private val listing = mockk<Extension.Available>(relaxed = true)
    private val closed = AtomicBoolean()
    private var code = 200
    private var body: ResponseBody? = null

    @BeforeEach
    fun setUp() {
        mockkObject(Installer.Companion, ExtensionLoader)
        every { Installer.cancelInstallQueue(context, any()) } returns Unit
        every { context.cacheDir } returns cache
        every { listing.pkgName } returns "test.extension"
        every { listing.apkUrl } returns "https://test.invalid/extension.apk"
        every { listing.store.signingKey } returns "expected-signature"
        every { listing.store.name } returns "Test store"
        every { preferences.extensionInstaller.get() } returns BasePreferences.ExtensionInstaller.PRIVATE
        every { ExtensionLoader.getArchivePackageInfo(context, any()) } returns archive
        every { ExtensionLoader.getSignatures(archive) } returns listOf("expected-signature")
        every { ExtensionLoader.installPrivateExtensionFile(context, any(), archive) } returns true
        every { network.client } returns OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(code).message("Test response").body(body ?: trackedBody()).build()
        }.build()
    }

    @AfterEach
    fun tearDown() {
        scope.cancel()
        unmockkAll()
    }

    private fun trackedBody(): ResponseBody = object : ResponseBody() {
        private val content = object : ForwardingSource(Buffer().writeUtf8("fake apk")) {
            override fun close() {
                closed.set(true)
                super.close()
            }
        }.buffer()
        override fun contentType() = null
        override fun contentLength() = -1L
        override fun source(): BufferedSource = content
    }

    private fun installer() = ExtensionInstaller(context, scope, preferences, network)

    private fun finish(installer: ExtensionInstaller): InstallStep = runBlocking {
        withTimeout(5000) {
            val result = installer.downloadAndInstall(listing)
                .first { it == InstallStep.Error || it == InstallStep.Installed }
            scope.coroutineContext[kotlinx.coroutines.Job]!!.children.toList().forEach { it.join() }
            result
        }
    }

    @Test
    fun `matching store signature reaches private install and closes response`() {
        finish(installer()) shouldBe InstallStep.Installed
        closed.get() shouldBe true
        verify(exactly = 1) { ExtensionLoader.installPrivateExtensionFile(context, any(), archive) }
    }

    @Test
    fun `private installer failure remains an error after signature verification`() {
        every { ExtensionLoader.installPrivateExtensionFile(context, any(), archive) } returns false
        finish(installer()) shouldBe InstallStep.Error
        File(cache, "extension_test.extension.apk").exists() shouldBe false
        verify(exactly = 1) { ExtensionLoader.installPrivateExtensionFile(context, any(), archive) }
    }

    @Test
    fun `wrong store signature rejects apk before installation`() {
        every { ExtensionLoader.getSignatures(archive) } returns listOf("different-signature")
        finish(installer()) shouldBe InstallStep.Error
        File(cache, "extension_test.extension.apk").exists() shouldBe false
        closed.get() shouldBe true
        verify(exactly = 0) { ExtensionLoader.installPrivateExtensionFile(any(), any(), any()) }
    }

    @Test
    fun `invalid archive is rejected before installation`() {
        every { ExtensionLoader.getArchivePackageInfo(context, any()) } returns null
        finish(installer()) shouldBe InstallStep.Error
        verify(exactly = 0) { ExtensionLoader.installPrivateExtensionFile(any(), any(), any()) }
    }

    @Test
    fun `http failure closes response without parsing an archive`() {
        code = 503
        finish(installer()) shouldBe InstallStep.Error
        closed.get() shouldBe true
        verify(exactly = 0) { ExtensionLoader.getArchivePackageInfo(any(), any()) }
    }

    @Test
    fun `cancelling while reading apk interrupts copy and never installs`() {
        val reading = CountDownLatch(1)
        val interrupted = CountDownLatch(1)
        body = object : ResponseBody() {
            private val content = object : ForwardingSource(Buffer()) {
                override fun read(sink: Buffer, byteCount: Long): Long {
                    reading.countDown()
                    try {
                        CountDownLatch(1).await()
                        return -1L
                    } catch (e: InterruptedException) {
                        interrupted.countDown()
                        throw e
                    }
                }
                override fun close() {
                    closed.set(true)
                    super.close()
                }
            }.buffer()
            override fun contentType() = null
            override fun contentLength() = -1L
            override fun source(): BufferedSource = content
        }
        val installer = installer()
        installer.downloadAndInstall(listing)
        try {
            reading.await(5, TimeUnit.SECONDS) shouldBe true
            installer.cancelInstall(listing.pkgName)
            interrupted.await(5, TimeUnit.SECONDS) shouldBe true
            runBlocking {
                withTimeout(5000) {
                    scope.coroutineContext[kotlinx.coroutines.Job]!!.children.toList().forEach { it.join() }
                }
            }
            closed.get() shouldBe true
            verify(exactly = 0) { ExtensionLoader.getArchivePackageInfo(any(), any()) }
            verify(exactly = 0) { ExtensionLoader.installPrivateExtensionFile(any(), any(), any()) }
        } finally {
            scope.cancel()
        }
    }
}
