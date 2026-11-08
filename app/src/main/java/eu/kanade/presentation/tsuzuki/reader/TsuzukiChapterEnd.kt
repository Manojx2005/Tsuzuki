package eu.kanade.presentation.tsuzuki.reader

import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiTokensFor
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.viewer.text.shared.ThemeUtils
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import org.json.JSONObject
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private fun chapterEndTokens() = tsuzukiTokensFor(Injekt.get<UiPreferences>().appTheme.get())

private data class ChapterEndLabels(
    val end: String,
    val upNext: String,
    val number: String?,
    val title: String,
    val downloaded: String,
    val next: String,
    val markRead: String,
    val contents: String,
)

private fun ReaderActivity.chapterEndLabels(chapter: ReaderChapter, dot: Boolean): ChapterEndLabels {
    fun String.cased() = if (dot) uppercase() else this
    return ChapterEndLabels(
        end = stringResource(MR.strings.tsz_reader_chapter_end).cased(),
        upNext = stringResource(MR.strings.tsz_reader_up_next).cased(),
        number = chapter.chapter.chapter_number.takeIf { it >= 0 }?.let {
            stringResource(MR.strings.tsz_reader_chapter_position, formatChapterNumber(it.toDouble())).cased()
        },
        title = chapter.chapter.name.cased(),
        downloaded = stringResource(MR.strings.label_downloaded).cased(),
        next = stringResource(MR.strings.action_next_chapter).cased(),
        markRead = stringResource(MR.strings.action_mark_as_read).cased(),
        contents = stringResource(MR.strings.tsz_reader_contents).cased(),
    )
}

internal fun ReaderActivity.createChapterEndCard(
    chapters: ViewerChapters,
    preferences: ReaderPreferences,
    tag: String,
): ComposeView? {
    val next = chapters.nextChapter ?: return null
    if (chapterEndTokens().style == TsuzukiStyle.NONE) return null

    return ComposeView(this).apply {
        this.tag = tag
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        setContent {
            TachiyomiTheme {
                val tokens = TsuzukiTheme.tokens
                val labels = chapterEndLabels(next, tokens.isDot)
                val pageTheme by preferences.novelTheme.collectAsState()
                val customPage by preferences.novelBackgroundColor.collectAsState()
                val customInk by preferences.novelFontColor.collectAsState()
                val (pageColor, inkColor) = ThemeUtils.getThemeColors(this@createChapterEndCard, preferences, pageTheme)
                val page = Color(if (pageTheme == "custom" && customPage != 0) customPage else pageColor)
                val ink = Color(if (pageTheme == "custom" && customInk != 0) customInk else inkColor)
                val downloaded by produceState(false, next) { value = chapterEndDownloaded(next) }
                val foreground = if (tokens.isDot) tokens.dotOn else ink
                val fill = if (tokens.isDot) tokens.tile else lerp(page, ink, 0.08f)
                val primary = if (tokens.isDot) tokens.accent else ink
                val onPrimary = if (tokens.isDot) tokens.onAccent else page

                Column(Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
                    Text(
                        labels.end,
                        Modifier.align(Alignment.CenterHorizontally),
                        color = ink,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Column(
                        Modifier.padding(top = 20.dp)
                            .fillMaxWidth()
                            .background(fill, RoundedCornerShape(24.dp))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(labels.upNext, color = foreground, style = MaterialTheme.typography.labelMedium)
                        labels.number?.let {
                            Text(it, color = foreground, style = MaterialTheme.typography.headlineLarge)
                        }
                        Text(labels.title, color = foreground, style = MaterialTheme.typography.titleMedium)
                        if (downloaded) {
                            Text(labels.downloaded, color = foreground, style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = ::loadNextChapter,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = onPrimary),
                        ) {
                            Text(labels.next, style = MaterialTheme.typography.labelLarge)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.markNovelChapterRead(chapters.currChapter) },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = foreground),
                            ) {
                                Text(labels.markRead, style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = ::openNovelContentsFromPage,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = foreground),
                            ) {
                                Text(labels.contents, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal suspend fun ReaderActivity.chapterEndDownloaded(chapter: ReaderChapter): Boolean = try {
    viewModel.isNovelChapterDownloaded(chapter)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    logcat(LogPriority.WARN, e) { "Could not check the next chapter download" }
    false
}

internal fun ReaderActivity.webChapterEndPayload(chapter: ReaderChapter): JSONObject? {
    val tokens = chapterEndTokens()
    if (tokens.style == TsuzukiStyle.NONE) return null
    val labels = chapterEndLabels(chapter, tokens.isDot)
    return JSONObject()
        .put("end", labels.end)
        .put("upNext", labels.upNext)
        .put("number", labels.number)
        .put("title", labels.title)
        .put("downloaded", labels.downloaded)
        .put("next", labels.next)
        .put("markRead", labels.markRead)
        .put("contents", labels.contents)
        .apply {
            if (tokens.isDot) {
                put("tile", ThemeUtils.colorToHex(tokens.tile.toArgb()))
                put("ink", ThemeUtils.colorToHex(tokens.dotOn.toArgb()))
                put("button", ThemeUtils.colorToHex(tokens.accent.toArgb()))
                put("buttonInk", ThemeUtils.colorToHex(tokens.onAccent.toArgb()))
            }
        }
}
