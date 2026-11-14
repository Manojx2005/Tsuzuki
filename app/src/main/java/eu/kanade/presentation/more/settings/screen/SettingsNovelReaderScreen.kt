package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsNovelReaderScreen : SearchableSettings {

    override val supportsReset: Boolean get() = true

    @Composable
    override fun getAdditionalResetPreferences(): List<tachiyomi.core.common.preference.Preference<*>> {
        val readerPref = remember { Injekt.get<ReaderPreferences>() }
        return listOf(
            readerPref.novelFontSize,
            readerPref.novelLineHeight,
            readerPref.novelAutoScrollSpeed,
            readerPref.novelParagraphIndent,
            readerPref.novelParagraphSpacing,
            readerPref.novelMarginLeft,
            readerPref.novelMarginRight,
            readerPref.novelMarginTop,
            readerPref.novelMarginBottom,
            readerPref.novelAutoLoadNextChapterAt,
            readerPref.novelSourceCssPriority,
            readerPref.novelTtsSpeed,
            readerPref.novelTtsPitch,
            readerPref.novelStatusBarOrder,
        )
    }

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = TDMR.strings.pref_category_novel

    @Composable
    override fun getPreferences(): List<Preference> {
        val readerPref = remember { Injekt.get<ReaderPreferences>() }
        val navigator = LocalNavigator.currentOrThrow

        return listOf(
            getDisplayGroup(readerPref),
            getTextGroup(readerPref, navigator),
            getFormattingGroup(readerPref),
            getNavigationGroup(readerPref),
            getAutoScrollGroup(readerPref),
            getContentGroup(readerPref),
            getStatusBarGroup(readerPref, navigator),
            getTtsGroup(readerPref),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.tsz_dictionary_packs),
                onClick = { navigator.push(eu.kanade.presentation.tsuzuki.dictionary.DictionaryPacksScreen()) },
            ),
        )
    }

    @Composable
    private fun getStatusBarGroup(
        readerPreferences: ReaderPreferences,
        navigator: Navigator,
    ): Preference.PreferenceGroup {
        val enabled = readerPreferences.novelStatusBarEnabled.collectAsState().value

        val items = buildList {
            add(
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.novelStatusBarEnabled,
                    title = stringResource(TDMR.strings.pref_novel_status_bar),
                ),
            )
            if (enabled) {
                add(
                    Preference.PreferenceItem.ListPreference(
                        preference = readerPreferences.novelStatusBarPosition,
                        entries = mapOf(
                            "bottom" to stringResource(TDMR.strings.novel_status_bar_position_bottom),
                            "top" to stringResource(TDMR.strings.novel_status_bar_position_top),
                        ),
                        title = stringResource(TDMR.strings.pref_novel_status_bar_position),
                    ),
                )
                add(
                    Preference.PreferenceItem.ListPreference(
                        preference = readerPreferences.novelStatusBarSize,
                        entries = mapOf(
                            "small" to stringResource(TDMR.strings.novel_status_bar_size_small),
                            "medium" to stringResource(TDMR.strings.novel_status_bar_size_medium),
                        ),
                        title = stringResource(TDMR.strings.pref_novel_status_bar_size),
                    ),
                )
                add(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPreferences.novelStatusBarShowChapterNumber,
                        title = stringResource(TDMR.strings.pref_novel_status_bar_show_chapter_number),
                    ),
                )
                add(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPreferences.novelStatusBarShowChapterTitle,
                        title = stringResource(TDMR.strings.pref_novel_status_bar_show_chapter_title),
                    ),
                )
                add(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPreferences.novelStatusBarShowCharging,
                        title = stringResource(TDMR.strings.pref_novel_status_bar_show_charging),
                        subtitle = stringResource(TDMR.strings.pref_novel_status_bar_show_charging_summary),
                    ),
                )
                add(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(TDMR.strings.pref_novel_status_bar_customize),
                        subtitle = stringResource(TDMR.strings.pref_novel_status_bar_customize_summary),
                        onClick = { navigator.push(StatusBarElementsScreen()) },
                    ),
                )
            }
        }

        return Preference.PreferenceGroup(
            title = stringResource(TDMR.strings.pref_novel_status_bar),
            preferenceItems = items,
        )
    }

    @Composable
    private fun getDisplayGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_display),
            preferenceItems = listOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.tsz_rs_page_colour),
                ) {
                    eu.kanade.presentation.tsuzuki.novel.TsuzukiPageColourPicker(readerPreferences)
                },
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.novelRenderingMode,
                    entries = mapOf(
                        "default" to stringResource(TDMR.strings.pref_novel_native_renderer),
                        "webview" to stringResource(TDMR.strings.novel_render_webview),
                    ).toMap(),
                    title = stringResource(TDMR.strings.pref_novel_rendering_mode),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.fullscreen,
                    title = stringResource(MR.strings.pref_fullscreen),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.novelShowProgressSlider,
                    title = stringResource(TDMR.strings.pref_novel_progress_slider),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.novelKeepScreenOn,
                    title = stringResource(TDMR.strings.pref_novel_keep_screen_on),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.novelCustomBrightness,
                    title = stringResource(MR.strings.pref_custom_brightness),
                ),
            ),
        )
    }

    @Composable
    private fun getTextGroup(
        readerPreferences: ReaderPreferences,
        navigator: cafe.adriel.voyager.navigator.Navigator,
    ): Preference.PreferenceGroup {
        val fontSize = readerPreferences.novelFontSize.collectAsState().value
        val lineHeight = readerPreferences.novelLineHeight.collectAsState().value

        return Preference.PreferenceGroup(
            title = stringResource(TDMR.strings.pref_novel_text_group),
            preferenceItems = listOf(
                Preference.PreferenceItem.SliderPreference(
                    value = fontSize,
                    valueRange = 10..40,
                    title = stringResource(TDMR.strings.pref_font_size),
                    valueString = "${fontSize}px",
                    onValueChanged = {
                        readerPreferences.novelFontSize.set(it)
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.novelFontFamily,
                    entries = mapOf(
                        "literata" to "Literata",
                        "sans-serif" to stringResource(TDMR.strings.novel_font_sans_serif),
                        "serif" to stringResource(TDMR.strings.novel_font_serif),
                        "monospace" to stringResource(TDMR.strings.novel_font_monospace),
                        "Georgia, serif" to "Georgia",
                        "Times New Roman, serif" to "Times New Roman",
                        "Arial, sans-serif" to "Arial",
                    ).toMap(),
                    title = stringResource(TDMR.strings.pref_font_family),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(TDMR.strings.font_manager_title),
                    subtitle = stringResource(TDMR.strings.font_manager_summary),
                    onClick = { navigator.push(FontManagerScreen()) },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = (lineHeight * 10).toInt(),
                    valueRange = 10..30,
                    title = stringResource(TDMR.strings.pref_novel_line_height),
                    valueString = "${lineHeight}x",
                    onValueChanged = {
                        readerPreferences.novelLineHeight.set(it / 10f)
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = readerPreferences.novelTextAlign,
                    entries = mapOf(
                        "left" to stringResource(TDMR.strings.novel_align_left),
                        "center" to stringResource(TDMR.strings.novel_align_center),
                        "right" to stringResource(TDMR.strings.novel_align_right),
                        "justify" to stringResource(TDMR.strings.novel_align_justify),
                    ).toMap(),
                    title = stringResource(TDMR.strings.pref_novel_text_align),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.novelForceTextLowercase,
                    title = stringResource(TDMR.strings.novel_force_lowercase),
                    subtitle = stringResource(TDMR.strings.pref_novel_force_lowercase_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.novelUseOriginalFonts,
                    title = stringResource(TDMR.strings.pref_novel_original_fonts_webview),
                    subtitle = stringResource(TDMR.strings.pref_novel_original_fonts_summary),
                ),
            ),
        )
    }

    @Composable
    private fun getNavigationGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_reader_navigation),
            // This screen is a global default with no per-series rendering-mode context (see the
            // Content group below), and paged mode only ever applies to WebView-rendered novels -
            // so Swipe navigation stays visible regardless of the paged-mode toggle, since it
            // remains fully functional for TextView-rendered novels either way.
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.novelVolumeKeysScroll,
                    title = stringResource(TDMR.strings.pref_novel_volume_keys_scroll),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.novelTapToScroll,
                    title = stringResource(TDMR.strings.pref_novel_tap_to_scroll),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.novelSwipeNavigation,
                    title = stringResource(TDMR.strings.pref_novel_swipe_navigation),
                    subtitle = stringResource(TDMR.strings.pref_novel_swipe_navigation_summary),
                ),
            ),
        )
    }

    @Composable
    private fun getAutoScrollGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val autoScrollSpeed = readerPreferences.novelAutoScrollSpeed.collectAsState().value

        return Preference.PreferenceGroup(
            title = stringResource(TDMR.strings.pref_novel_auto_scroll),
            preferenceItems = listOf(
                Preference.PreferenceItem.SliderPreference(
                    value = autoScrollSpeed,
                    valueRange = 2..20,
                    title = stringResource(TDMR.strings.pref_novel_auto_scroll_speed),
                    valueString = "${autoScrollSpeed / 2f}",
                    onValueChanged = {
                        readerPreferences.novelAutoScrollSpeed.set(it)
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getFormattingGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val paragraphIndent = readerPreferences.novelParagraphIndent.collectAsState().value
        val paragraphSpacing = readerPreferences.novelParagraphSpacing.collectAsState().value
        val marginLeft = readerPreferences.novelMarginLeft.collectAsState().value
        val marginRight = readerPreferences.novelMarginRight.collectAsState().value
        val marginTop = readerPreferences.novelMarginTop.collectAsState().value
        val marginBottom = readerPreferences.novelMarginBottom.collectAsState().value

        return Preference.PreferenceGroup(
            title = stringResource(TDMR.strings.pref_novel_formatting_group),
            preferenceItems = listOf(
                Preference.PreferenceItem.SliderPreference(
                    value = (paragraphIndent * 10).toInt(),
                    valueRange = 0..50,
                    title = stringResource(TDMR.strings.pref_novel_paragraph_indent),
                    valueString = "${paragraphIndent}em",
                    onValueChanged = { readerPreferences.novelParagraphIndent.set(it / 10f) },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = (paragraphSpacing * 10).toInt(),
                    valueRange = 0..30,
                    title = stringResource(TDMR.strings.pref_novel_paragraph_spacing),
                    valueString = "${paragraphSpacing}em",
                    onValueChanged = { readerPreferences.novelParagraphSpacing.set(it / 10f) },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = marginLeft,
                    valueRange = 0..64,
                    title = stringResource(TDMR.strings.pref_novel_margin_left),
                    valueString = "${marginLeft}dp",
                    onValueChanged = { readerPreferences.novelMarginLeft.set(it) },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = marginRight,
                    valueRange = 0..64,
                    title = stringResource(TDMR.strings.pref_novel_margin_right),
                    valueString = "${marginRight}dp",
                    onValueChanged = { readerPreferences.novelMarginRight.set(it) },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = marginTop,
                    valueRange = 0..64,
                    title = stringResource(TDMR.strings.pref_novel_margin_top),
                    valueString = "${marginTop}dp",
                    onValueChanged = { readerPreferences.novelMarginTop.set(it) },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = marginBottom,
                    valueRange = 0..64,
                    title = stringResource(TDMR.strings.pref_novel_margin_bottom),
                    valueString = "${marginBottom}dp",
                    onValueChanged = { readerPreferences.novelMarginBottom.set(it) },
                ),
            ),
        )
    }

    @Composable
    private fun getContentGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val autoLoadNextAt = readerPreferences.novelAutoLoadNextChapterAt.collectAsState().value
        val markAsReadThreshold = readerPreferences.novelMarkAsReadThreshold.collectAsState().value
        val pagedModeEnabled = readerPreferences.novelPagedMode.collectAsState().value
        val pagedDragCommitPercent = readerPreferences.novelPagedDragCommitPercent.collectAsState().value

        return Preference.PreferenceGroup(
            title = stringResource(TDMR.strings.pref_novel_content_group),
            // This screen is a global default with no per-series rendering-mode context (unlike
            // the per-series reader settings, which correctly gate on renderingMode == "webview"),
            // and paged mode only ever applies to WebView-rendered novels - so Infinite scroll /
            // Auto-load next chapter stay visible here regardless of the paged-mode toggle, since
            // they remain fully functional for TextView-rendered novels either way.
            preferenceItems = buildList {
                add(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPreferences.novelChapterRecap,
                        title = stringResource(MR.strings.tsz_reader_recap_setting),
                        subtitle = stringResource(MR.strings.tsz_reader_recap_setting_summary),
                    ),
                )
                add(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPreferences.novelInfiniteScroll,
                        title = stringResource(TDMR.strings.pref_novel_infinite_scroll_label),
                        subtitle = stringResource(TDMR.strings.pref_novel_infinite_scroll_summary),
                    ),
                )
                add(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPreferences.novelPagedMode,
                        title = stringResource(TDMR.strings.pref_novel_paged_mode_webview),
                        subtitle = stringResource(TDMR.strings.pref_novel_paged_mode_summary),
                    ),
                )
                if (pagedModeEnabled) {
                    add(
                        Preference.PreferenceItem.SwitchPreference(
                            preference = readerPreferences.novelPagedSwipeEnabled,
                            title = stringResource(TDMR.strings.pref_novel_paged_swipe),
                            subtitle = stringResource(TDMR.strings.pref_novel_paged_swipe_summary),
                        ),
                    )
                    add(
                        Preference.PreferenceItem.SliderPreference(
                            value = pagedDragCommitPercent,
                            valueRange = 5..50,
                            title = stringResource(TDMR.strings.pref_novel_paged_drag_commit),
                            subtitle = stringResource(TDMR.strings.pref_novel_paged_drag_commit_summary),
                            valueString = "$pagedDragCommitPercent%",
                            onValueChanged = { readerPreferences.novelPagedDragCommitPercent.set(it) },
                        ),
                    )
                }
                add(
                    Preference.PreferenceItem.SliderPreference(
                        value = autoLoadNextAt,
                        valueRange = 50..100,
                        title = stringResource(TDMR.strings.pref_novel_auto_load_next_threshold),
                        valueString = "$autoLoadNextAt%",
                        onValueChanged = { readerPreferences.novelAutoLoadNextChapterAt.set(it) },
                    ),
                )
                add(
                    Preference.PreferenceItem.SliderPreference(
                        value = markAsReadThreshold,
                        valueRange = 50..100,
                        title = stringResource(TDMR.strings.pref_novel_mark_as_read_threshold),
                        valueString = "$markAsReadThreshold%",
                        onValueChanged = { readerPreferences.novelMarkAsReadThreshold.set(it) },
                    ),
                )
                add(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPreferences.novelMarkShortChapterAsRead,
                        title = stringResource(TDMR.strings.pref_novel_mark_short_read),
                        subtitle = stringResource(TDMR.strings.pref_novel_mark_short_read_summary),
                    ),
                )
                add(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPreferences.novelHideChapterTitle,
                        title = stringResource(TDMR.strings.pref_novel_hide_chapter_title),
                        subtitle = stringResource(TDMR.strings.pref_novel_hide_chapter_title_summary),
                    ),
                )
                add(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPreferences.novelBlockMedia,
                        title = stringResource(TDMR.strings.pref_novel_block_media_label),
                        subtitle = stringResource(TDMR.strings.pref_novel_block_media_summary),
                    ),
                )
                add(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPreferences.novelTextSelectable,
                        title = stringResource(TDMR.strings.pref_novel_text_selectable),
                        subtitle = stringResource(TDMR.strings.pref_novel_text_selectable_summary),
                    ),
                )
                add(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPreferences.novelShowRawHtml,
                        title = stringResource(TDMR.strings.pref_novel_show_raw_html),
                        subtitle = stringResource(TDMR.strings.pref_novel_show_raw_html_summary),
                    ),
                )
                add(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPreferences.novelSourceCssPriority,
                        title = stringResource(TDMR.strings.pref_novel_source_css_priority),
                        subtitle = stringResource(TDMR.strings.pref_novel_source_css_priority_summary),
                    ),
                )
            },
        )
    }

    @Composable
    private fun getTtsGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val ttsSpeed = readerPreferences.novelTtsSpeed.collectAsState().value
        val ttsPitch = readerPreferences.novelTtsPitch.collectAsState().value

        return Preference.PreferenceGroup(
            title = stringResource(TDMR.strings.pref_novel_tts_section),
            preferenceItems = listOf(
                Preference.PreferenceItem.SliderPreference(
                    value = (ttsSpeed * 10).toInt(),
                    valueRange = 1..30,
                    title = stringResource(TDMR.strings.pref_novel_tts_speed),
                    valueString = "${ttsSpeed}x",
                    onValueChanged = { readerPreferences.novelTtsSpeed.set(it / 10f) },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = (ttsPitch * 10).toInt(),
                    valueRange = 1..30,
                    title = stringResource(TDMR.strings.pref_novel_tts_pitch),
                    valueString = "${ttsPitch}x",
                    onValueChanged = { readerPreferences.novelTtsPitch.set(it / 10f) },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.novelTtsAutoNextChapter,
                    title = stringResource(TDMR.strings.pref_novel_tts_auto_next),
                    subtitle = stringResource(TDMR.strings.pref_novel_tts_auto_next_summary),
                ),
            ),
        )
    }
}
