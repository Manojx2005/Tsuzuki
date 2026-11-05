package eu.kanade.presentation.tsuzuki.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults.richTextEditorColors
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTokens
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.tachiyomi.ui.manga.notes.MangaNotesScreen
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.seconds

/**
 * Book notes in the Tsuzuki themes (board 17-drafts "BookNotes", owner-approved 2026-11-04): back circle, "Notes" in
 * the display face with the book title, the notes in one tile, and the editor's real tools (bold, italic, underline,
 * bulleted, numbered) in a floating pill bar, the active ones in the accent. Saving works as stock: Markdown, written
 * a quarter second after typing stops and again on leaving. The board's "Saved" marker is left out (no save state).
 */
@Composable
fun TsuzukiBookNotesScreen(
    state: MangaNotesScreen.State,
    navigateUp: () -> Unit,
    onUpdate: (String) -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
    val scope = rememberCoroutineScope()
    val rich = rememberRichTextState()

    DisposableEffect(scope, rich) {
        snapshotFlow { rich.annotatedString }
            .debounce(0.25.seconds)
            .distinctUntilChanged()
            .map { rich.toMarkdown() }
            .onEach { onUpdate(it) }
            .launchIn(scope)
        onDispose { onUpdate(rich.toMarkdown()) }
    }
    LaunchedEffect(Unit) {
        rich.setMarkdown(state.notes)
        rich.config.unorderedListIndent = 4
        rich.config.orderedListIndent = 20
    }
    LaunchedEffect(t.accent) { rich.config.linkColor = t.accent }
    val focus = remember { FocusRequester() }
    LaunchedEffect(focus) { focus.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .tsuzukiBackground(t)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(Modifier.padding(top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .minimumInteractiveComponentSize()
                    .size(44.dp)
                    .clip(CircleShape)
                    .then(tileFill(t, CircleShape))
                    .clickable(role = Role.Button, onClick = navigateUp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(MR.strings.tsz_a11y_back),
                    tint = Color.White,
                )
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    t.case(stringResource(MR.strings.action_notes)),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp, lineHeight = 40.sp),
                    color = Color.White,
                )
                Text(
                    t.case(state.manga.title),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        val shape = RoundedCornerShape(24.dp)
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .clip(shape)
                .then(tileFill(t, shape)),
        ) {
            RichTextEditor(
                state = rich,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 27.sp),
                placeholder = { Text(stringResource(MR.strings.notes_placeholder), color = muted) },
                colors = richTextEditorColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = t.accent,
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                modifier = Modifier.fillMaxSize().focusRequester(focus),
            )
        }

        val bar = RoundedCornerShape(32.dp)
        Row(
            Modifier
                .padding(vertical = 12.dp)
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .clip(bar)
                .then(tileFill(t, bar))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Tool(Icons.Outlined.FormatBold, rich.currentSpanStyle.fontWeight == FontWeight.Bold) {
                rich.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
            }
            Tool(Icons.Outlined.FormatItalic, rich.currentSpanStyle.fontStyle == FontStyle.Italic) {
                rich.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
            }
            Tool(
                Icons.Outlined.FormatUnderlined,
                rich.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) ?: false,
            ) {
                rich.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
            }
            Tool(Icons.AutoMirrored.Outlined.FormatListBulleted, rich.isUnorderedList) { rich.toggleUnorderedList() }
            Tool(Icons.Outlined.FormatListNumbered, rich.isOrderedList) { rich.toggleOrderedList() }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Tool(icon: ImageVector, on: Boolean, onClick: () -> Unit) {
    val t = TsuzukiTheme.tokens
    Box(
        Modifier
            .weight(1f)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (on) t.accent else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = icon.name,
            tint = if (on) t.onAccent else Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun tileFill(t: TsuzukiTokens, shape: androidx.compose.ui.graphics.Shape): Modifier = Modifier
    .background(t.tile)
    .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)

private fun TsuzukiTokens.case(s: String) = if (isDot) s.uppercase() else s
