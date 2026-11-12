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
    fun `rotation matches only scheduler-named files`() {
        val regex = backupFileRegex(release)
        regex.matches("Tsuzuki_2026-11-07_22-00-05-123.tachibk") shouldBe true
        regex.matches("io.github.manojx2005.tsuzuki_2026-11-07_22-00-05-123.tachibk") shouldBe true
        regex.matches("Tsuzuki-preview_2026-11-07_22-00-05-123.tachibk") shouldBe false
        regex.matches("my-own-backup.tachibk") shouldBe false
    }

    @Test
    fun `a manual export in the automatic folder is never pruned`() {
        val manual = "Tsuzuki_2026-01-01_09-30.tachibk"
        val newest = "Tsuzuki_2026-11-07_22-00-05-123.tachibk"
        val files = listOf(manual, "Tsuzuki_2026-11-06_22-00-05-123.tachibk", newest)
        backupsToPrune(files, backupFileRegex(release), keep = 1, newestName = newest) { it } shouldBe
            listOf("Tsuzuki_2026-11-06_22-00-05-123.tachibk")
    }

    @Test
    fun `a file with no backup field is not a backup`() {
        listOf(1, 2, 101, 104, 105, 106).all(::isBackupField) shouldBe true
        listOf(3, 100, 102, 103, 999).none(::isBackupField) shouldBe true
    }
}
