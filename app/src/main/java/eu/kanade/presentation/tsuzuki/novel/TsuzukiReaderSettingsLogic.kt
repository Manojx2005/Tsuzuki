package eu.kanade.presentation.tsuzuki.novel

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import tachiyomi.core.common.preference.Preference
import tachiyomi.i18n.MR
import java.util.Locale
import kotlin.math.roundToInt

internal enum class ReaderPreferenceGroup {
    TYPOGRAPHY,
    PARAGRAPHS,
    COLOURS,
    PAGE_BASICS,
    PAGE_OPTIONS,
    NAVIGATION,
    TTS,
    ADVANCED,
}

internal enum class NovelSettingsTab(val label: StringResource, vararg val groups: ReaderPreferenceGroup) {
    TEXT(MR.strings.tsz_q11_text, ReaderPreferenceGroup.TYPOGRAPHY, ReaderPreferenceGroup.PARAGRAPHS),
    PAGE(
        MR.strings.tsz_q11_page,
        ReaderPreferenceGroup.COLOURS,
        ReaderPreferenceGroup.PAGE_BASICS,
        ReaderPreferenceGroup.PAGE_OPTIONS,
    ),
    CONTROLS(MR.strings.tsz_rs_controls, ReaderPreferenceGroup.NAVIGATION),
    LISTEN(MR.strings.tsz_q11_listen, ReaderPreferenceGroup.TTS),
    MORE(MR.strings.tsz_q11_more, ReaderPreferenceGroup.ADVANCED),
}

internal fun formatReaderSize(size: Int): String = "$size sp"

internal fun formatReaderSpacing(tenths: Int, locale: Locale): String =
    String.format(locale, "%.1f×", tenths / 10f)

internal fun formatReaderMargins(first: Int, second: Int): String =
    if (first == second) "$first dp" else "$first / $second dp"

internal fun linkedReaderMargin(first: Int, second: Int): Int = ((first.toDouble() + second) / 2).roundToInt()

/** Keep authored replacement rules and snippets when restoring the reader controls to their defaults. */
internal fun ReaderPreferences.resetNovelSettings() {
    listOf<Preference<*>>(
        enableEpubJs,
        enableEpubStyles,
        navigationModeNovel,
        novelAutoLoadNextChapterAt,
        novelAutoScrollSpeed,
        novelAutoSplitText,
        novelAutoSplitWordCount,
        novelBackgroundColor,
        novelBlockMedia,
        novelBottomZoneHeight,
        novelChapterTitleDisplay,
        novelCustomBrightness,
        novelCustomBrightnessValue,
        novelFontColor,
        novelFontFamily,
        novelFontSize,
        novelForceTextLowercase,
        novelHideChapterTitle,
        novelInfiniteScroll,
        novelKeepScreenOn,
        novelLineHeight,
        novelMarginBottom,
        novelMarginLeft,
        novelMarginRight,
        novelMarginTop,
        novelNavInverted,
        novelPagedMode,
        novelPagedSwipeEnabled,
        novelParagraphIndent,
        novelParagraphSpacing,
        novelRenderingMode,
        novelShowProgressSlider,
        novelShowRawHtml,
        novelSourceCssPriority,
        novelStatusBarEnabled,
        novelStatusBarPosition,
        novelStatusBarShowBattery,
        novelStatusBarShowChapterNumber,
        novelStatusBarShowChapterTitle,
        novelStatusBarShowProgress,
        novelStatusBarShowTime,
        novelStatusBarSize,
        novelSwipeNavigation,
        novelTextAlign,
        novelTextSelectable,
        novelTheme,
        novelTtsAutoNextChapter,
        novelTtsAutoStartOnPanelOpen,
        novelTtsBackgroundPlayback,
        novelTtsEnableHighlight,
        novelTtsHighlightColor,
        novelTtsHighlightStyle,
        novelTtsHighlightTextColor,
        novelTtsKeepHighlightInView,
        novelTtsPitch,
        novelTtsSpeed,
        novelTtsVoice,
        novelUseOriginalFonts,
        novelVerticalProgressSliderSize,
        novelVerticalScrollbar,
        novelVerticalScrollbarPosition,
        novelVolumeKeysScroll,
    ).forEach { it.delete() }
}
