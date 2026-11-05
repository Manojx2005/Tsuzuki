package eu.kanade.tachiyomi.tsuzuki.extension

import eu.kanade.tachiyomi.extension.model.Extension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FailedExtensionReportTest {

    private fun failed(
        reason: Extension.Failed.Reason,
        detail: String? = null,
        trace: String? = null,
        shared: Boolean = true,
        novel: Boolean = false,
    ) = Extension.Failed(
        name = "Example",
        pkgName = "eu.kanade.tachiyomi.extension.en.example",
        versionName = "1.4.12",
        versionCode = 12,
        libVersion = 1.4,
        reason = reason,
        detail = detail,
        stackTrace = trace,
        isShared = shared,
        isNovel = novel,
    )

    @Test
    fun `report lists package version and reason`() {
        val report = failed(Extension.Failed.Reason.NOT_SIGNED).toReport()
        assertTrue(report.contains("Extension: Example (eu.kanade.tachiyomi.extension.en.example)"))
        assertTrue(report.contains("Version: 1.4.12 (code 12), lib 1.4"))
        assertTrue(report.contains("Installed: system"))
        assertTrue(report.contains("Reason: NOT_SIGNED"))
    }

    @Test
    fun `report includes detail and stack trace when present`() {
        val report = failed(
            Extension.Failed.Reason.SOURCE_LOAD_ERROR,
            detail = "a.b.Source",
            trace = "java.lang.Error: boom\n\tat a.b.Source.<init>\n",
            shared = false,
            novel = true,
        ).toReport()
        assertTrue(report.contains("Reason: SOURCE_LOAD_ERROR (a.b.Source)"))
        assertTrue(report.contains("Installed: private, novel"))
        assertTrue(report.trimEnd().endsWith("at a.b.Source.<init>"))
    }

    @Test
    fun `report without trace has no trailing blank section`() {
        val report = failed(Extension.Failed.Reason.MISSING_VERSION).toReport()
        assertFalse(report.contains("\n\n"))
        assertEquals("Reason: MISSING_VERSION", report.trimEnd().lines().last())
    }
}
