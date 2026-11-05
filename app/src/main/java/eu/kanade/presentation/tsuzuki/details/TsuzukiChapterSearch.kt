package eu.kanade.presentation.tsuzuki.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.UpIcon
import eu.kanade.presentation.theme.tsuzuki.LocalTsuzukiTokens
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiProgressBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.tsuzuki.reader.TsuzukiReadingTypography
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.ui.manga.search.ChapterSearchOptions
import eu.kanade.tachiyomi.ui.manga.search.ChapterSearchResult
import eu.kanade.tachiyomi.ui.manga.search.ChapterSearchScreen
import eu.kanade.tachiyomi.ui.manga.search.ChapterSearchSnippet
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.clearFocusOnSoftKeyboardHide
import tachiyomi.presentation.core.util.runOnEnterKeyPressed
import tachiyomi.presentation.core.util.showSoftKeyboard

@Composable
internal fun TsuzukiChapterSearch(
    state: ChapterSearchScreen.State,
    navigateUp: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOptionsChange: (ChapterSearchOptions) -> Unit,
    onResultClick: (Chapter) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .tsuzukiBackground(tokens)
            .safeDrawingPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChapterSearchHeading(state.manga?.title, navigateUp)
        ChapterSearchField(state.query, onQueryChange, onSearch)
        ChapterSearchOptionPills(state.options, onOptionsChange)

        val chapters = state.chapters
        when {
            chapters == null -> LoadingScreen()
            chapters.isEmpty() -> EmptyScreen(stringRes = TDMR.strings.chapter_search_no_chapters)
            state.regexError != null -> EmptyScreen(
                message = stringResource(TDMR.strings.novel_invalid_regex_format, state.regexError),
            )
            state.submittedQuery == null -> Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tsuzukiCase(stringResource(TDMR.strings.chapter_search_empty)).orEmpty(),
                    color = tokens.dotOn.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            else -> TsuzukiReadingTypography {
                CompositionLocalProvider(LocalTsuzukiTokens provides tokens.copy(tileRadius = 22.dp)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item(key = "summary") {
                            ChapterSearchSummary(state, chapters.size)
                        }
                        items(items = state.results, key = {
                            it.chapter.id
                        }, contentType = { "chapter_result" }) { result ->
                            ChapterSearchResultTile(result) { onResultClick(result.chapter) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterSearchHeading(bookTitle: String?, navigateUp: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, end = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(
            onClick = navigateUp,
            modifier = Modifier
                .size(44.dp)
                .background(tokens.tile, CircleShape)
                .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, CircleShape) } ?: Modifier),
        ) {
            UpIcon()
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = tsuzukiCase(stringResource(TDMR.strings.action_search_chapters)).orEmpty(),
                color = tokens.dotOn,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge,
            )
            if (!bookTitle.isNullOrBlank()) {
                Text(
                    text = tsuzukiCase(bookTitle).orEmpty(),
                    color = tokens.dotOn.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ChapterSearchField(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val submit: () -> Unit = {
        if (query.isNotBlank()) {
            onSearch()
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(CircleShape)
            .background(tokens.tile)
            .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, CircleShape) } ?: Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = submit, enabled = query.isNotBlank()) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = stringResource(MR.strings.action_search),
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .runOnEnterKeyPressed(action = submit)
                .showSoftKeyboard(remember { query.isEmpty() })
                .clearFocusOnSoftKeyboardHide(),
            textStyle = MaterialTheme.typography.titleMedium.copy(color = tokens.dotOn),
            cursorBrush = SolidColor(tokens.accent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = tsuzukiCase(stringResource(TDMR.strings.chapter_search_hint)).orEmpty(),
                            color = tokens.dotOn.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (query.isNotEmpty()) {
            IconButton(
                onClick = {
                    onQueryChange("")
                    focusRequester.requestFocus()
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(MR.strings.action_reset),
                    tint = tokens.dotOn.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
private fun ChapterSearchOptionPills(options: ChapterSearchOptions, onOptionsChange: (ChapterSearchOptions) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ChapterSearchOption(
            text = stringResource(TDMR.strings.novel_use_regex),
            selected = options.isRegex,
            onClick = { onOptionsChange(options.copy(isRegex = !options.isRegex)) },
        )
        ChapterSearchOption(
            text = stringResource(TDMR.strings.novel_case_sensitive_matching),
            selected = options.caseSensitive,
            onClick = { onOptionsChange(options.copy(caseSensitive = !options.caseSensitive)) },
        )
        ChapterSearchOption(
            text = stringResource(TDMR.strings.novel_match_whole_word),
            selected = options.wholeWord,
            onClick = { onOptionsChange(options.copy(wholeWord = !options.wholeWord)) },
        )
    }
}

@Composable
private fun ChapterSearchOption(text: String, selected: Boolean, onClick: () -> Unit) {
    TsuzukiPill(
        text = text,
        selected = selected,
        onClick = onClick,
        role = Role.Checkbox,
        modifier = Modifier.semantics { toggleableState = ToggleableState(selected) },
    )
}

@Composable
private fun ChapterSearchSummary(state: ChapterSearchScreen.State, totalChapters: Int) {
    val tokens = TsuzukiTheme.tokens
    val progress = stringResource(TDMR.strings.chapter_search_progress, state.searchedCount, totalChapters)
    val summary = when {
        state.isSearching -> progress
        state.results.isEmpty() -> stringResource(MR.strings.no_results_found)
        else -> {
            val matches = pluralStringResource(
                TDMR.plurals.chapter_search_matches,
                state.totalMatches,
                state.totalMatches,
            )
            val chapters = pluralStringResource(
                TDMR.plurals.chapter_search_matched_chapters,
                state.results.size,
                state.results.size,
            )
            "$progress · $matches $chapters"
        }
    }
    Column(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = tsuzukiCase(summary).orEmpty(),
            color = tokens.dotOn.copy(alpha = 0.72f),
            style = MaterialTheme.typography.labelMedium,
        )
        if (state.failedCount > 0) {
            val failures = pluralStringResource(
                TDMR.plurals.chapter_search_failed_chapters,
                state.failedCount,
                state.failedCount,
            )
            Text(
                text = tsuzukiCase(failures).orEmpty(),
                color = tokens.accent,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        TsuzukiProgressBar(progress = state.searchedCount / totalChapters.toFloat(), dots = 30, thickness = 4.dp)
    }
}

@Composable
private fun ChapterSearchResultTile(result: ChapterSearchResult, onClick: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val highlight = SpanStyle(
        color = tokens.onAccent,
        background = tokens.accent,
        fontWeight = FontWeight.Bold,
    )
    TsuzukiTile(
        modifier = Modifier.fillMaxWidth().tsuzukiClickable(role = Role.Button, onClick = onClick),
        padding = 14.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = tsuzukiCase(result.chapter.name).orEmpty(),
                    color = tokens.dotOn,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val matches = pluralStringResource(
                    TDMR.plurals.chapter_search_matches,
                    result.matchCount,
                    result.matchCount,
                )
                Text(
                    text = tsuzukiCase(matches).orEmpty(),
                    color = tokens.accent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            result.snippets.forEach { snippet ->
                Text(
                    text = chapterSearchSnippetText(snippet, highlight),
                    color = tokens.dotOn.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

internal fun chapterSearchSnippetText(
    snippet: ChapterSearchSnippet,
    highlight: SpanStyle,
): AnnotatedString =
    buildAnnotatedString {
        append(snippet.text)
        addStyle(highlight, snippet.matchStart, snippet.matchEnd)
        append(" (%.1f%%)".format(snippet.position * 100))
    }
