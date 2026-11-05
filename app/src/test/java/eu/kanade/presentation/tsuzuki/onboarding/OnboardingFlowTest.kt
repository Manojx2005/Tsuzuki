package eu.kanade.presentation.tsuzuki.onboarding

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class OnboardingFlowTest {
    @Test
    fun `six real steps keep required setup before optional import`() {
        OnboardingPage.entries.toList() shouldBe listOf(
            OnboardingPage.WELCOME,
            OnboardingPage.LANGUAGES,
            OnboardingPage.LOOK,
            OnboardingPage.STORAGE,
            OnboardingPage.PERMISSIONS,
            OnboardingPage.IMPORT,
        )
    }

    @Test
    fun `skip requires storage on a fresh install`() {
        onboardingSkipTarget(storageConfigured = false) shouldBe OnboardingPage.STORAGE
    }

    @Test
    fun `skip finishes when storage is already configured`() {
        onboardingSkipTarget(storageConfigured = true) shouldBe null
    }

    @Test
    fun `offline language choices include common source languages and configured languages`() {
        val choices = onboardingLanguageChoices(emptyList(), setOf("ja", "en"))
        ("es" in choices) shouldBe true
        ("ja" in choices) shouldBe true
    }

    @Test
    fun `loaded catalogue languages preserve configured languages without duplication`() {
        onboardingLanguageChoices(listOf("en", "de", "en"), setOf("ja", "de")) shouldBe setOf("en", "de", "ja")
    }

    @Test
    fun `selecting a new language preserves current languages`() {
        toggleOnboardingLanguage(setOf("en", "ja"), "ko") shouldBe setOf("en", "ja", "ko")
    }

    @Test
    fun `a selected language can be removed when another remains`() {
        toggleOnboardingLanguage(setOf("en", "ja"), "en") shouldBe setOf("ja")
    }

    @Test
    fun `the final language cannot be deselected`() {
        toggleOnboardingLanguage(setOf("ja"), "ja") shouldBe setOf("ja")
    }

    @Test
    fun `an empty existing preference can recover by selecting a language`() {
        toggleOnboardingLanguage(emptySet(), "en") shouldBe setOf("en")
    }
}
