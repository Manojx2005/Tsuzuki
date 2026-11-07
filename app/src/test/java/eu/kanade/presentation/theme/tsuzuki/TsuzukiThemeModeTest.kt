package eu.kanade.presentation.theme.tsuzuki

import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.ThemeMode
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TsuzukiThemeModeTest {
    @Test
    fun `Dot and Colour are always dark`() {
        for (theme in listOf(AppTheme.DOT, AppTheme.COLOUR)) {
            for (mode in ThemeMode.entries) {
                effectiveThemeMode(theme, mode) shouldBe ThemeMode.DARK
            }
        }
    }

    @Test
    fun `legacy themes keep the stored light or dark choice`() {
        for (mode in ThemeMode.entries) {
            effectiveThemeMode(AppTheme.DEFAULT, mode) shouldBe mode
        }
    }
}
