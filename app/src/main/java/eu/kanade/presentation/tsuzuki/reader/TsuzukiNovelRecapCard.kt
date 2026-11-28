package eu.kanade.presentation.tsuzuki.reader

import android.app.Activity
import android.text.format.DateUtils
import android.view.View
import android.widget.LinearLayout
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiTokensFor
import eu.kanade.presentation.tsuzuki.novel.rememberNovelFontFamily
import eu.kanade.tachiyomi.tsuzuki.reader.NovelRecap
import eu.kanade.tachiyomi.tsuzuki.reader.novelRecapLabel
import eu.kanade.tachiyomi.tsuzuki.reader.novelRecapScrolledAway
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.viewer.text.shared.ThemeUtils
import eu.kanade.tachiyomi.ui.reader.viewer.text.webview.NovelWebViewJsAssets
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.abs
import tachiyomi.presentation.core.i18n.stringResource as composeStringResource

internal fun ReaderActivity.createNovelRecapView(chapterId: Long?, preferences: ReaderPreferences): View? {
    val recap = viewModel.novelRecapFor(chapterId) ?: return null
    return ComposeView(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        setContent {
            TachiyomiTheme {
                NovelRecapCard(
                    activity = this@createNovelRecapView,
                    recap = recap,
                    preferences = preferences,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    onDismiss = {
                        viewModel.dismissNovelRecap(recap.chapterId)
                        visibility = View.GONE
                    },
                )
            }
        }
    }
}

/**
 * The recap for a chapter that reopened mid-way, floated over the top of the reader so it is seen where the
 * reader is looking and never moves the restored position. Shared by the TextView and WebView renderers
 * (scroll and paged). It goes away on its button, once the reader has moved on, or while the menus show.
 */
@Composable
internal fun NovelResumeRecapOverlay(
    activity: ReaderActivity,
    viewModel: ReaderViewModel,
    state: ReaderViewModel.State,
    preferences: ReaderPreferences,
    insets: PaddingValues,
) {
    val recap by viewModel.novelResumeRecap.collectAsState()
    val current = recap ?: return
    val progress by rememberUpdatedState(state.novelProgressPercent to state.novelPageIndex)
    LaunchedEffect(current.chapterId) {
        // Restoring the position moves the progress by itself: wait for it to land, then watch for the reader.
        withTimeoutOrNull(NOVEL_RECAP_SETTLE_MS) {
            snapshotFlow { progress.first }.first { abs(it - current.resumePercent) <= 1 }
        }
        delay(NOVEL_RECAP_SETTLE_PAUSE_MS)
        val (basePercent, basePage) = progress
        snapshotFlow { progress }.first { (percent, page) ->
            novelRecapScrolledAway(basePercent, percent, basePage, page)
        }
        viewModel.dismissNovelRecap(current.chapterId)
    }
    if (state.menuVisible) return
    Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
        NovelRecapCard(
            activity = activity,
            recap = current,
            preferences = preferences,
            floating = true,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).widthIn(max = 640.dp).fillMaxWidth(),
            onDismiss = { viewModel.dismissNovelRecap(current.chapterId) },
        )
    }
}

private const val NOVEL_RECAP_SETTLE_MS = 4_000L
private const val NOVEL_RECAP_SETTLE_PAUSE_MS = 400L
private const val NOVEL_RECAP_CARD_MAX_LINES = 6

@Composable
private fun NovelRecapCard(
    activity: Activity,
    recap: NovelRecap,
    preferences: ReaderPreferences,
    modifier: Modifier = Modifier,
    floating: Boolean = false,
    onDismiss: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val theme by preferences.novelTheme.collectAsState()
    val customPage by preferences.novelBackgroundColor.collectAsState()
    val customInk by preferences.novelFontColor.collectAsState()
    val (pageColor, textColor) = ThemeUtils.getThemeColors(activity, preferences, theme)
    val page = Color(if (theme == "custom" && customPage != 0) customPage else pageColor)
    val ink = Color(if (theme == "custom" && customInk != 0) customInk else textColor)
    val foreground = if (tokens.isDot) tokens.dotOn else ink
    val fill = if (tokens.isDot) tokens.tile else lerp(page, ink, 0.08f)
    val shape = RoundedCornerShape(tokens.tileRadius)
    val title = novelRecapLabel(
        composeStringResource(MR.strings.tsz_reader_recap_previously),
        recap.previousChapter,
        recap.lastReadAt?.let(::novelRecapRelativeTime),
    )
    val dismiss = composeStringResource(MR.strings.tsz_reader_recap_dismiss)
    Column(
        modifier = modifier
            .then(if (floating && !tokens.isDot) Modifier.shadow(8.dp, shape) else Modifier)
            .background(fill, shape)
            .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (tokens.isDot) title.uppercase() else title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = if (tokens.isDot) tokens.mutedText else ink,
            )
            TextButton(onClick = onDismiss) {
                Text(
                    if (tokens.isDot) dismiss.uppercase() else dismiss,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (tokens.isDot) tokens.accent else ink,
                )
            }
        }
        val body = if (floating) {
            Modifier.heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.4f).dp)
                .verticalScroll(rememberScrollState())
        } else {
            Modifier
        }
        // Board: body 13 sp in the reader's own font, at most six lines on the card above the text.
        val face by preferences.novelFontFamily.collectAsState()
        val readerFont = rememberNovelFontFamily(face)
        val bodyStyle = MaterialTheme.typography.bodyMedium.let { base ->
            base.copy(fontSize = 13.sp, fontFamily = readerFont ?: base.fontFamily)
        }
        Column(body, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                recap.paragraphs.joinToString("\n\n"),
                style = bodyStyle,
                color = foreground,
                maxLines = if (floating) Int.MAX_VALUE else NOVEL_RECAP_CARD_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal fun ReaderActivity.webNovelRecapScript(chapterId: Long): String {
    val recap = viewModel.novelRecapFor(chapterId) ?: return ""
    val tokens = tsuzukiTokensFor(Injekt.get<UiPreferences>().appTheme.get())
    fun String.cased() = if (tokens.isDot) uppercase() else this
    val payload = JSONObject()
        .put("chapterId", recap.chapterId.toString())
        .put(
            "title",
            novelRecapLabel(
                stringResource(MR.strings.tsz_reader_recap_previously),
                recap.previousChapter,
                recap.lastReadAt?.let(::novelRecapRelativeTime),
            ).cased(),
        )
        .put("dismiss", stringResource(MR.strings.tsz_reader_recap_dismiss).cased())
        .put("paragraphs", JSONArray(recap.paragraphs))
        .apply {
            if (tokens.isDot) {
                put("tile", ThemeUtils.colorToHex(tokens.tile.toArgb()))
                put("ink", ThemeUtils.colorToHex(tokens.dotOn.toArgb()))
                put("accent", ThemeUtils.colorToHex(tokens.accent.toArgb()))
                tokens.tileBorder?.let { put("border", ThemeUtils.colorToHex(it.toArgb())) }
            }
        }
        .toString()
        .replace("<", "\\u003c")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029")
    return NovelWebViewJsAssets.loadWith(this, "chapter-recap-card.js", mapOf("RECAP" to payload))
}

/** "2 days ago" / "2 日前" in the device language (platform formatter, no app strings). */
private fun novelRecapRelativeTime(readAt: Long): String =
    DateUtils.getRelativeTimeSpanString(readAt, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString()
