package eu.kanade.tachiyomi.tsuzuki.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.backup.BackupFileValidator
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.Source
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.service.SourceManager
import java.io.FileNotFoundException
import java.io.IOException

class BackupRestorePreviewTest {
    private val uri = mockk<Uri>()
    private val fixture by lazy {
        checkNotNull(javaClass.getResourceAsStream("/synthetic-library-small.tachibk")).use { it.readBytes() }
    }

    @Test
    fun countsBooksShelvesAndHistoryFromTheCompressedFile() = runTest {
        val preview = validator(fixture).validate(uri)
        assertEquals(BackupFileValidator.Contents(4, 2, 8L), preview.contents)
    }

    @Test
    fun truncatedCompressedFileDoesNotProducePreview() {
        assertThrows(IOException::class.java) {
            runTest { validator(fixture.copyOf(fixture.size - 6)).validate(uri) }
        }
    }

    @Test
    fun unavailableProviderStreamHasSpecificFailure() {
        assertThrows(FileNotFoundException::class.java) {
            runTest { validator(null).validate(uri) }
        }
    }

    private fun validator(bytes: ByteArray?): BackupFileValidator {
        val resolver = mockk<ContentResolver> {
            every { openInputStream(uri) } answers { bytes?.inputStream() }
        }
        val context = mockk<Context> { every { contentResolver } returns resolver }
        val source = mockk<Source>()
        val sourceManager = mockk<SourceManager>()
        every { sourceManager.get(any()) } returns source
        return BackupFileValidator(context, sourceManager, mockk<TrackerManager>(), ProtoBuf)
    }
}
