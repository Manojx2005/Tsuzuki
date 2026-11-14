package eu.kanade.tachiyomi.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tachiyomi.core.common.i18n.stringResource
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPOutputStream

class BackupFileValidatorTest {
    private val uri = mockk<Uri>()

    @Test
    fun rejectsGzipWithOnlyAnUnknownProtobufField() {
        // Field 150, wire type 2, one byte of unrelated payload.
        val bytes = gzip(byteArrayOf(0xb2.toByte(), 0x09, 0x01, 0x00))
        mockkStatic("tachiyomi.core.common.i18n.LocalizeKt")
        try {
            every { any<Context>().stringResource(any()) } returns "Invalid backup"
            val failure = assertThrows(IOException::class.java) {
                runTest { validator(bytes).validate(uri) }
            }
            assertEquals("Invalid backup", failure.message)
        } finally {
            unmockkStatic("tachiyomi.core.common.i18n.LocalizeKt")
        }
    }

    @Test
    fun acceptsSettingsOnlyBackupWithoutLibraryRecords() = runTest {
        val backup = Backup(
            backupManga = emptyList(),
            backupPreferences = listOf(BackupPreference("reader_setting", BooleanPreferenceValue(true))),
        )
        val bytes = gzip(ProtoBuf.encodeToByteArray(Backup.serializer(), backup))
        assertEquals(BackupFileValidator.Contents(0, 0, 0), validator(bytes).validate(uri).contents)
    }

    private fun validator(bytes: ByteArray): BackupFileValidator {
        val resolver = mockk<ContentResolver> {
            every { openInputStream(uri) } answers { bytes.inputStream() }
        }
        val context = mockk<Context> { every { contentResolver } returns resolver }
        return BackupFileValidator(context, mockk(), mockk(), ProtoBuf)
    }

    private fun gzip(bytes: ByteArray): ByteArray = ByteArrayOutputStream().also { output ->
        GZIPOutputStream(output).use { it.write(bytes) }
    }.toByteArray()
}
