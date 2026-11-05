package eu.kanade.tachiyomi.data.backup.models

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BackupSourceNameTest {
    @Test
    fun `names survive but numeric and blank placeholders do not`() {
        "Example Source".isBackupSourceName() shouldBe true
        "123 Comics".isBackupSourceName() shouldBe true
        "  ".isBackupSourceName() shouldBe false
        " 1234 ".isBackupSourceName() shouldBe false
        "-9876".isBackupSourceName() shouldBe false
    }
}
