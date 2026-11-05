package eu.kanade.presentation.tsuzuki.onboarding

internal enum class OnboardingPage {
    WELCOME,
    LANGUAGES,
    LOOK,
    STORAGE,
    PERMISSIONS,
    IMPORT,
}

// TODO: 09-manga/OnboardingContent needs real content-type preferences before it can become a step.
internal fun onboardingSkipTarget(storageConfigured: Boolean): OnboardingPage? =
    if (storageConfigured) null else OnboardingPage.STORAGE

// Common languages in the bundled repository catalogues, available before the indexes load.
private val defaultSourceLanguages = listOf("en", "ru", "ar", "es", "fr", "tr", "pt", "id", "zh", "vi")

internal fun onboardingLanguageChoices(available: List<String>, selected: Set<String>): Set<String> =
    (available.ifEmpty { defaultSourceLanguages } + selected).toSet()

internal fun toggleOnboardingLanguage(selected: Set<String>, language: String): Set<String> = when {
    language !in selected -> selected + language
    selected.size > 1 -> selected - language
    else -> selected
}
