package eu.kanade.tachiyomi.tsuzuki.update

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TsuzukiAppUpdateJobTest {
    @Test
    fun `a new version is announced once`() {
        TsuzukiAppUpdateJob.shouldNotify("v0.2.0", skippedVersion = "", lastNotifiedVersion = "") shouldBe true
        TsuzukiAppUpdateJob.shouldNotify("v0.2.0", skippedVersion = "", lastNotifiedVersion = "v0.2.0") shouldBe false
    }

    @Test
    fun `a newer version is announced after an older one`() {
        TsuzukiAppUpdateJob.shouldNotify("v0.3.0", skippedVersion = "", lastNotifiedVersion = "v0.2.0") shouldBe true
    }

    @Test
    fun `a skipped version is never announced`() {
        TsuzukiAppUpdateJob.shouldNotify("v0.2.0", skippedVersion = "v0.2.0", lastNotifiedVersion = "") shouldBe false
    }
}
