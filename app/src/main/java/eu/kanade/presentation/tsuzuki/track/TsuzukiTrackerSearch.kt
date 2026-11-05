package eu.kanade.presentation.tsuzuki.track

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.tsuzuki.LocalTsuzukiTokens
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMenuItem
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.util.system.openInBrowser
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.runOnEnterKeyPressed

@Composable
internal fun TsuzukiTrackerSearch(
    state: TextFieldState,
    onDispatchQuery: () -> Unit,
    queryResult: Result<List<TrackSearch>>?,
    selected: TrackSearch?,
    onSelectedChange: (TrackSearch) -> Unit,
    onConfirmSelection: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    supportsPrivateTracking: Boolean,
    altTitles: List<String>,
) {
    val tokens = TsuzukiTheme.tokens
    val focusManager = LocalFocusManager.current
    val dispatchQuery: () -> Unit = {
        onDispatchQuery()
        focusManager.clearFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .tsuzukiBackground(tokens)
            .safeDrawingPadding(),
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, top = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(tokens.tile)
                    .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, CircleShape) } ?: Modifier),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(MR.strings.action_webview_back),
                    tint = tokens.dotOn,
                )
            }
            Text(
                text = tsuzukiCase(stringResource(MR.strings.add_tracking)).orEmpty(),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge,
                color = tokens.dotOn,
            )
        }
        TrackerSearchField(
            state = state,
            altTitles = altTitles,
            onSearch = dispatchQuery,
            modifier = Modifier.padding(horizontal = 12.dp).padding(top = 14.dp),
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val tracks = queryResult?.getOrNull()
            when {
                queryResult == null -> LoadingScreen()
                tracks == null -> EmptyScreen(
                    message = queryResult.exceptionOrNull()?.message ?: stringResource(MR.strings.unknown_error),
                )
                tracks.isEmpty() -> EmptyScreen(stringRes = MR.strings.no_results_found)
                else -> {
                    CompositionLocalProvider(LocalTsuzukiTokens provides tokens.copy(tileRadius = 20.dp)) {
                        ScrollbarLazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(tracks, key = {
                                "${it.tracker_id}-${it.remote_id}"
                            }, contentType = { "track_result" }) { track ->
                                TrackerSearchResult(track, selected = track == selected) {
                                    focusManager.clearFocus()
                                    onSelectedChange(track)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selected != null) {
            val colors = ButtonDefaults.buttonColors(containerColor = tokens.accent, contentColor = tokens.onAccent)
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { onConfirmSelection(false) },
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    colors = colors,
                ) {
                    Text(
                        text = tsuzukiCase(stringResource(MR.strings.action_track)).orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                if (supportsPrivateTracking) {
                    Button(
                        onClick = { onConfirmSelection(true) },
                        modifier = Modifier.heightIn(min = 56.dp),
                        colors = colors,
                    ) {
                        Icon(Icons.Filled.VisibilityOff, stringResource(MR.strings.action_toggle_private_on))
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerSearchField(
    state: TextFieldState,
    altTitles: List<String>,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = TsuzukiTheme.tokens
    val focusRequester = remember { FocusRequester() }
    val muted = tokens.dotOn.copy(alpha = 0.72f)
    var altMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(CircleShape)
            .background(tokens.tile)
            .border(2.dp, tokens.accent, CircleShape)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSearch) {
            Icon(Icons.Outlined.Search, stringResource(MR.strings.action_search), tint = muted)
        }
        BasicTextField(
            state = state,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp)
                .focusRequester(focusRequester)
                .runOnEnterKeyPressed(action = onSearch),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = tokens.dotOn),
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            onKeyboardAction = { onSearch() },
            cursorBrush = SolidColor(tokens.accent),
            decorator = { field ->
                Box {
                    if (state.text.isEmpty()) {
                        Text(
                            text = tsuzukiCase(stringResource(MR.strings.action_search_hint)).orEmpty(),
                            color = muted,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    field()
                }
            },
        )
        if (state.text.isNotEmpty()) {
            IconButton(
                onClick = {
                    state.clearText()
                    focusRequester.requestFocus()
                },
            ) {
                Icon(Icons.Filled.Close, stringResource(MR.strings.duplicate_category_clear), tint = muted)
            }
        }
        if (altTitles.isNotEmpty()) {
            Box {
                IconButton(onClick = { altMenuExpanded = true }) {
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        stringResource(MR.strings.tsz_tracker_search_pick_title),
                        tint = muted,
                    )
                }
                DropdownMenu(expanded = altMenuExpanded, onDismissRequest = { altMenuExpanded = false }) {
                    altTitles.forEach { title ->
                        TsuzukiMenuItem(
                            text = title,
                            onClick = {
                                state.edit { replace(0, length, title) }
                                altMenuExpanded = false
                                onSearch()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerSearchResult(track: TrackSearch, selected: Boolean, onClick: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val muted = tokens.dotOn.copy(alpha = 0.72f)
    val shape = RoundedCornerShape(tokens.tileRadius)
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    val type = track.publishing_type.toLowerCase(Locale.current).capitalize(Locale.current)
    val status = track.publishing_status.toLowerCase(Locale.current).capitalize(Locale.current)
    val details = listOfNotNull(
        type.takeIf { it.isNotBlank() },
        track.start_date.takeIf { it.isNotBlank() },
        if (track.total_chapters > 0) stringResource(MR.strings.duplicate_n_chapters, track.total_chapters) else null,
        status.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
    val credits = (track.authors + track.artists).distinct().joinToString()
    val summary = track.summary.trim()

    TsuzukiTile(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (selected) Modifier.border(2.dp, tokens.accent, shape) else Modifier)
            .semantics { this.selected = selected }
            .tsuzukiClickable(
                role = Role.RadioButton,
                onLongClick = { menuExpanded = true },
                onClick = onClick,
            ),
        padding = 10.dp,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MangaCover.Book(data = track.cover_url, modifier = Modifier.width(52.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = tsuzukiCase(track.title).orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.dotOn,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (details.isNotEmpty()) {
                    Text(
                        text = tsuzukiCase(details).orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                    )
                }
                if (credits.isNotBlank()) {
                    Text(
                        text = tsuzukiCase(credits).orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (track.score != -1.0) {
                    val label = stringResource(MR.strings.score)
                    Text(
                        text = tsuzukiCase("$label ${track.score}").orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                    )
                }
            }
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.size(24.dp),
                colors = RadioButtonDefaults.colors(selectedColor = tokens.accent, unselectedColor = muted),
            )
        }
        if (summary.isNotBlank()) {
            Text(
                text = tsuzukiCase(summary).orEmpty(),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            TsuzukiMenuItem(
                text = stringResource(MR.strings.action_copy_to_clipboard),
                onClick = {
                    scope.launch {
                        clipboard.setClipEntry(ClipData.newPlainText(track.title, track.title).toClipEntry())
                    }
                    menuExpanded = false
                },
            )
            TsuzukiMenuItem(
                text = stringResource(MR.strings.action_open_in_browser),
                onClick = {
                    if (track.tracking_url.isNotBlank()) {
                        context.openInBrowser(track.tracking_url)
                    }
                    menuExpanded = false
                },
            )
        }
    }
}
