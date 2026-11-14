package eu.kanade.presentation.theme.tsuzuki

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TsuzukiBackGateTest {

    @Test
    fun `a handler is active only while its screen is the top screen`() {
        backHandlerActive(true, "home", "home") shouldBe true
        // Details was just pushed and the tab host is still composed underneath it.
        backHandlerActive(true, "details", "home") shouldBe false
    }

    @Test
    fun `a handler that is not enabled stays inactive`() {
        backHandlerActive(false, "home", "home") shouldBe false
        backHandlerActive(false, "details", "home") shouldBe false
    }

    @Test
    fun `without a navigator the own state decides`() {
        backHandlerActive(true, null, "home") shouldBe true
        backHandlerActive(false, null, "home") shouldBe false
    }

    @Test
    fun `a restored singleton uses the key of its navigator instance`() {
        // The saved root and the newly constructed process singleton have different unique keys.
        backHandlerActive(true, "saved-home", "process-home", "saved-home") shouldBe true
        backHandlerActive(true, "details", "process-home", "saved-home") shouldBe false
        backHandlerActive(false, "saved-home", "process-home", "saved-home") shouldBe false
    }
}
