package eu.kanade.tachiyomi.tsuzuki

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import eu.kanade.tachiyomi.data.download.DownloadProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.domain.source.model.StubSource
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Real storage lookup using a new unique folder, without adding a book or changing preferences. */
@RunWith(AndroidJUnit4::class)
class DownloadCompatibilityTest {
    @Test(timeout = 45_000)
    fun olderNovelZipIsFoundAndTestFolderIsRemoved() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".debug")) { "Use only the preview copy" }
        val provider = DownloadProvider(context)
        val source = StubSource(-7301, "en", "Tsuzuki compatibility " + UUID.randomUUID(), isNovelSource = true)
        assertNull("The unique test source folder must not exist", provider.findSourceDir(source))
        val mangaDir = provider.getMangaDir("Temporary compatibility test", source).getOrThrow()
        val sourceDir = provider.findSourceDir(source)!!
        val fileName = "日本語_Chapter 1_719cf6.zip"
        try {
            assertTrue(mangaDir.listFiles().isNullOrEmpty())
            val archive = mangaDir.createFile(fileName)!!
            ZipOutputStream(archive.openOutputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("index.html"))
                zip.write("<html><body>Temporary compatibility test</body></html>".toByteArray())
                zip.closeEntry()
            }
            val found = provider.findChapterDir(
                ". Chapter 1 ",
                "日本語",
                "https://test.invalid/chapter/1",
                "Temporary compatibility test",
                source,
            )
            assertNotNull("Recognize the older independently-sanitized chapter name and ZIP suffix", found)
            assertEquals(archive.uri, found!!.uri)
            ZipInputStream(found.openInputStream()).use { zip ->
                assertEquals("index.html", zip.nextEntry.name)
                assertTrue(zip.readBytes().toString(Charsets.UTF_8).contains("Temporary compatibility test"))
            }
        } finally {
            // Only this UUID source was created by the test. Refuse to delete unexpected contents.
            val files = mangaDir.listFiles().orEmpty()
            assertTrue(files.all { it.name == fileName })
            files.forEach { assertTrue(it.delete()) }
            assertTrue(mangaDir.delete())
            assertTrue(sourceDir.listFiles().isNullOrEmpty())
            assertTrue(sourceDir.delete())
            assertFalse(sourceDir.exists())
            assertNull(provider.findSourceDir(source))
        }
    }
}
