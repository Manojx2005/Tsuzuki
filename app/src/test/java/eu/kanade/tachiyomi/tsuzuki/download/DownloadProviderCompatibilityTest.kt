package eu.kanade.tachiyomi.tsuzuki.download

import android.content.Context
import android.net.Uri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.download.DownloadProvider
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.storage.service.StorageManager

class DownloadProviderCompatibilityTest {
    private val preferences = mockk<LibraryPreferences>(relaxed = true)
    private val storage = mockk<StorageManager>()
    private val source = StubSource(123, "en", "Test source")
    private val root = mockk<UniFile>()
    private val sourceDir = mockk<UniFile>()
    private val mangaDir = mockk<UniFile>()
    private val manga = mockk<Manga>()
    private val provider = DownloadProvider(mockk<Context>(), storage, preferences)
    private var asciiOnly = false
    private val url = "https://test.invalid/chapter/1"

    @BeforeEach
    fun setUp() {
        every { preferences.disallowNonAsciiFilenames.get() } answers { asciiOnly }
        every { storage.getDownloadsDirectory() } returns root
        every { root.findFile("Test source (EN)") } returns sourceDir
        every { sourceDir.findFile("Test book") } returns mangaDir
        every { manga.title } returns "Test book"
        every { mangaDir.findFile(any()) } returns null
    }

    @Test
    fun `new downloads keep the existing combined name and url hash`() {
        provider.getChapterDirName("Chapter 1", "Group", url) shouldBe "Group_Chapter 1_719cf6"
        provider.getChapterDirName("   ", null, url) shouldBe "Chapter_719cf6"
        provider.getChapterDirName(". Chapter 1 ", "日本語", url) shouldBe "日本語_. Chapter 1_719cf6"
    }

    @Test
    fun `legacy split names survive ascii preference changes and retain zip support`() {
        for (ascii in listOf(false, true)) {
            asciiOnly = ascii
            val names = provider.getValidChapterDirNames(". Chapter 1 ", "日本語", url)
            for (base in listOf("日本語_Chapter 1_719cf6", "e697a5e69cace8aa9e_. Chapter 1_719cf6", "日本語_Chapter 1")) {
                for (suffix in listOf("", ".cbz", ".zip")) {
                    names shouldContain (base + suffix)
                }
            }
        }
    }

    @Test
    fun `scanlator slashes never become nested download paths`() {
        val names = provider.getValidChapterDirNames("Chapter 1", "Group/Team", url)
        names.contains("Group_Team_Chapter 1_719cf6.zip") shouldBe true
        names.any { '/' in it || '\\' in it } shouldBe false
    }

    @Test
    fun `long multibyte names fit the storage limit including cbz extension`() {
        val name = provider.getChapterDirName("字".repeat(100), "日本語", url)
        (name + ".cbz").toByteArray(Charsets.UTF_8).size.let { it <= 240 } shouldBe true
        name.endsWith("_719cf6") shouldBe true
        name.contains('\uFFFD') shouldBe false
    }

    @Test
    fun `single chapter lookup finds an older novel zip`() {
        val archive = mockk<UniFile>()
        every { mangaDir.findFile("日本語_Chapter 1_719cf6.zip") } returns archive
        provider.findChapterDir(". Chapter 1 ", "日本語", url, "Test book", source) shouldBe archive
    }

    @Test
    fun `large saf deletion lookup lists once and recognizes legacy zip ignoring case`() = runBlocking {
        val uri = mockk<Uri>()
        val archive = mockk<UniFile>()
        every { uri.scheme } returns "content"
        every { mangaDir.uri } returns uri
        every { archive.name } returns "cHaPtEr 1.ZIP"
        every { mangaDir.listFiles() } returns arrayOf(archive)
        val chapters = (1L..50L).map { Chapter.create().copy(id = it, name = "Chapter $it", url = "/chapter/$it") }
        provider.findChapterDirs(chapters, manga, source) shouldBe (mangaDir to listOf(archive))
        verify(exactly = 1) { mangaDir.listFiles() }
        verify(exactly = 0) { mangaDir.findFile(any()) }
    }

    @Test
    fun `plain file batch lookup still probes individual names`() = runBlocking {
        val uri = mockk<Uri>()
        val archive = mockk<UniFile>()
        every { uri.scheme } returns "file"
        every { mangaDir.uri } returns uri
        every { mangaDir.findFile("Chapter 1.zip") } returns archive
        val chapters = (1L..50L).map { Chapter.create().copy(id = it, name = "Chapter $it", url = "/chapter/$it") }
        provider.findChapterDirs(chapters, manga, source) shouldBe (mangaDir to listOf(archive))
        verify(exactly = 0) { mangaDir.listFiles() }
    }
}
