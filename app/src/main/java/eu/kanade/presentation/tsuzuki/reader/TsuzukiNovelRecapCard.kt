package eu.kanade.presentation.tsuzuki.reader

import android.view.View
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiTokensFor
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.viewer.text.shared.ThemeUtils
import eu.kanade.tachiyomi.ui.reader.viewer.text.webview.NovelWebViewJsAssets
import org.json.JSONArray
import org.json.JSONObject
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
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
                val tokens = TsuzukiTheme.tokens
                val theme by preferences.novelTheme.collectAsState()
                val customPage by preferences.novelBackgroundColor.collectAsState()
                val customInk by preferences.novelFontColor.collectAsState()
                val (pageColor, textColor) = ThemeUtils.getThemeColors(this@createNovelRecapView, preferences, theme)
                val page = Color(if (theme == "custom" && customPage != 0) customPage else pageColor)
                val ink = Color(if (theme == "custom" && customInk != 0) customInk else textColor)
                val foreground = if (tokens.isDot) tokens.dotOn else ink
                val fill = if (tokens.isDot) tokens.tile else lerp(page, ink, 0.08f)
                val shape = RoundedCornerShape(tokens.tileRadius)
                val title = composeStringResource(MR.strings.tsz_reader_recap_previously)
                val dismiss = composeStringResource(MR.strings.tsz_reader_recap_dismiss)
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                        .background(fill, shape)
                        .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (tokens.isDot) title.uppercase() else title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (tokens.isDot) tokens.mutedText else ink,
                        )
                        TextButton(onClick = {
                            viewModel.dismissNovelRecap(recap.chapterId)
                            visibility = View.GONE
                        }) {
                            Text(
                                if (tokens.isDot) dismiss.uppercase() else dismiss,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (tokens.isDot) tokens.accent else ink,
                            )
                        }
                    }
                    recap.paragraphs.forEach { paragraph ->
                        Text(paragraph, style = MaterialTheme.typography.bodyMedium, color = foreground)
                    }
                }
            }
        }
    }
}

internal fun ReaderActivity.webNovelRecapScript(chapterId: Long): String {
    val recap = viewModel.novelRecapFor(chapterId) ?: return ""
    val tokens = tsuzukiTokensFor(Injekt.get<UiPreferences>().appTheme.get())
    fun String.cased() = if (tokens.isDot) uppercase() else this
    val payload = JSONObject()
        .put("chapterId", recap.chapterId.toString())
        .put("title", stringResource(MR.strings.tsz_reader_recap_previously).cased())
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
