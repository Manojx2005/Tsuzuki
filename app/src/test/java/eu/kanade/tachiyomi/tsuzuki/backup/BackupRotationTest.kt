package eu.kanade.tachiyomi.tsuzuki.backup

import com.hippo.unifile.UniFile
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okio.Buffer
import okio.buffer
import okio.gzip
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import java.io.ByteArrayOutputStream
import java.io.IOException

class BackupRotationTest {
    @Test
    fun keepsNewestAcrossNamingSchemesAndIgnoresUnfinishedOrManualFiles() {
        val appId = "io.github.manojx2005.tsuzuki"
        val newest = "Tsuzuki_2026-11-08_12-00-00-123.tachibk"
        val old = "Tsuzuki_2026-11-06_12-00.tachibk"
        val files = listOf(
            "${appId}_2026-11-07_12-00.tachibk",
            newest,
            old,
            "Tsuzuki_2026-11-09_12-00-00-123.tachibk.part",
            "Tsuzuki-dev_2026-11-01_12-00.tachibk",
            "my-backup.tachibk",
        )
        backupsToPrune(files, backupFileRegex(appId), 2, newest) { it } shouldBe listOf(old)
    }

    @Test
    fun retentionDefaultsToExistingLimitAndRejectsUnsupportedValues() {
        val preferences = TsuzukiBackupPreferences(InMemoryPreferenceStore())
        preferences.retentionCount() shouldBe 4
        preferences.retention.set(8)
        preferences.retentionCount() shouldBe 8
        preferences.retention.set(0)
        preferences.retentionCount() shouldBe 4
    }

    @Test
    fun lastExportUsesSizeAndFallsBackToLegacyTimestamp() {
        BackupExport.fromPreference("20:125", 10) shouldBe BackupExport(20, 125)
        BackupExport.fromPreference("", 10) shouldBe BackupExport(10, null)
        BackupExport.fromPreference("20:125", 30) shouldBe BackupExport(30, null)
        BackupExport.fromPreference("broken", 0) shouldBe null
    }

    @Test
    fun sizeIsActualCompressedFileSize() {
        val destination = Buffer()
        val sizeSink = BackupSizeSink(destination)
        sizeSink.gzip().buffer().use { it.writeUtf8("library".repeat(100)) }
        sizeSink.bytesWritten shouldBe destination.size
    }

    @Test
    fun publishesStagedBytesWhenTheProviderCannotRename() {
        val bytes = byteArrayOf(1, 2, 3)
        val output = ByteArrayOutputStream()
        val staged = mockk<UniFile> {
            every { renameTo(any()) } returns false
            every { openInputStream() } returns bytes.inputStream()
            every { delete() } returns true
        }
        val destination = mockk<UniFile> {
            every { openOutputStream() } returns output
        }
        val directory = mockk<UniFile> {
            every { createFile(match { it.endsWith(".tachibk") }) } returns destination
        }

        AutomaticBackupFiles(directory).publish(staged) shouldBe destination
        output.toByteArray().toList() shouldBe bytes.toList()
        verify(exactly = 1) { staged.delete() }
    }

    @Test
    fun failedPublicationOnlyRemovesItsIncompleteDestination() {
        val staged = mockk<UniFile> {
            every { renameTo(any()) } returns false
            every { openInputStream() } returns byteArrayOf(1).inputStream()
        }
        val destination = mockk<UniFile> {
            every { openOutputStream() } throws IOException("Disk full")
            every { delete() } returns true
        }
        val directory = mockk<UniFile> {
            every { createFile(match { it.endsWith(".tachibk") }) } returns destination
        }

        assertThrows(IOException::class.java) { AutomaticBackupFiles(directory).publish(staged) }
        verify(exactly = 1) { destination.delete() }
        verify(exactly = 0) { directory.listFiles() }
        verify(exactly = 0) { staged.delete() }
    }
}
