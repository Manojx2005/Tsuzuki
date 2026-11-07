package eu.kanade.tachiyomi.tsuzuki.backup

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TsuzukiBackupNameTest {
    private val release = "io.github.manojx2005.tsuzuki"

    @Test
    fun `backups are named after Tsuzuki`() {
        backupFilePrefix(release) shouldBe "Tsuzuki"
        backupFilePrefix("$release.debug") shouldBe "Tsuzuki-preview"
        backupFilePrefix("$release.dev") shouldBe "Tsuzuki-dev"
    }

    @Test
    fun `rotation matches new and old names but nothing else`() {
        val regex = backupFileRegex(release)
        regex.matches("Tsuzuki_2026-11-07_22-00.tachibk") shouldBe true
        regex.matches("io.github.manojx2005.tsuzuki_2026-11-07_22-00.tachibk") shouldBe true
        regex.matches("Tsuzuki-preview_2026-11-07_22-00.tachibk") shouldBe false
        regex.matches("my-own-backup.tachibk") shouldBe false
    }
}
