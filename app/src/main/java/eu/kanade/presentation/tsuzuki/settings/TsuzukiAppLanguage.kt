package eu.kanade.presentation.tsuzuki.settings

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTokens
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.tsuzuki.browse.tsuzukiGroupedTile
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The device's own language, named in itself ("日本語"). On Android 13+ the per-app language replaces the locale seen
 * through Resources, so read the system list from LocaleManager there.
 */
fun deviceLanguageName(context: android.content.Context): String {
    val locale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(android.app.LocaleManager::class.java)?.systemLocales?.get(0)
    } else {
        null
    } ?: android.content.res.Resources.getSystem().configuration.locales[0]
    return eu.kanade.tachiyomi.util.system.LocaleHelper.getLocalizedDisplayName(locale.toLanguageTag())
}

/** One app language: [tag] is empty for "follow the device". */
data class TsuzukiLanguage(val tag: String, val name: String, val nameHere: String?)

/**
 * App language in the Tsuzuki themes (board 17-drafts "AppLanguage", owner-approved 2026-11-04): display title and
 * subtitle, a search field over the shipped languages, "System default" in its own tile with the device language,
 * then every language in its own script with its name in the current language underneath. The chosen one has an
 * accent check.
 */
@Composable
fun TsuzukiAppLanguageScreen(
    languages: List<TsuzukiLanguage>,
    current: String,
    deviceLanguage: String,
    onPick: (String) -> Unit,
    onBack: () -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
    var query by rememberSaveable { mutableStateOf("") }
    val system = languages.firstOrNull { it.tag.isEmpty() }
    val all = languages.filter { it.tag.isNotEmpty() }
    val shown = if (query.isBlank()) {
        all
    } else {
        all.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.nameHere.orEmpty().contains(query, ignoreCase = true) ||
                it.tag.contains(query, ignoreCase = true)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().tsuzukiBackground(t),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "header") {
            Column(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .then(tileFill(t, CircleShape))
                        .clickable(role = Role.Button, onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text(
                    t.case(stringResource(MR.strings.pref_app_language)),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 44.sp, lineHeight = 48.sp),
                    color = Color.White,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Text(
                    t.case(stringResource(MR.strings.tsz_language_subtitle)),
                    style = body(13.sp, FontWeight.Medium),
                    color = muted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
            }
        }
        item(key = "search") {
            val shape = RoundedCornerShape(26.dp)
            Row(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(shape)
                    .then(tileFill(t, shape))
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = muted, modifier = Modifier.size(18.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            t.case(stringResource(MR.strings.tsz_language_search, all.size)),
                            style = body(14.sp, FontWeight.Medium),
                            color = muted,
                            maxLines = 1,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = body(14.sp, FontWeight.Medium).copy(color = Color.White),
                        cursorBrush = SolidColor(t.accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (system != null && query.isBlank()) {
            item(key = "system") {
                Box(Modifier.padding(top = 12.dp)) {
                    LanguageRow(
                        name = system.name,
                        sub = "$deviceLanguage · ${stringResource(MR.strings.tsz_language_from_device)}",
                        selected = current.isEmpty(),
                        first = true,
                        last = true,
                        onClick = { onPick("") },
                    )
                }
            }
        }
        item(key = "label") {
            Text(
                stringResource(MR.strings.tsz_language_all).uppercase(),
                style = body(11.sp, FontWeight.ExtraBold).copy(letterSpacing = 1.2.sp),
                color = muted,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp),
            )
        }
        itemsIndexed(shown, key = { _, it -> it.tag }) { index, lang ->
            LanguageRow(
                name = lang.name,
                sub = lang.nameHere,
                selected = current == lang.tag,
                first = index == 0,
                last = index == shown.lastIndex,
                onClick = { onPick(lang.tag) },
            )
        }
        item(key = "end") { Box(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun LanguageRow(
    name: String,
    sub: String?,
    selected: Boolean,
    first: Boolean,
    last: Boolean,
    onClick: () -> Unit,
) {
    val t = TsuzukiTheme.tokens
    val muted = if (t.isDot) Color(0xFF8A8A8A) else Color.White.copy(alpha = 0.72f)
    Column(tsuzukiGroupedTile(first = first, last = last).widthIn(max = 720.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.RadioButton, onClick = onClick)
                .heightIn(min = 64.dp)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    t.case(name),
                    style = body(16.sp, FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!sub.isNullOrBlank()) {
                    Text(
                        t.case(sub),
                        style = body(12.sp, FontWeight.Medium),
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (selected) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(t.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = t.onAccent,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        if (!last) HorizontalDivider(color = Color.White.copy(alpha = if (t.isDot) 0.1f else 0.06f))
    }
}

private fun tileFill(t: TsuzukiTokens, shape: androidx.compose.ui.graphics.Shape): Modifier = Modifier
    .background(t.tile)
    .then(t.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)

private fun TsuzukiTokens.case(s: String) = if (isDot) s.uppercase() else s

@Composable
private fun body(size: TextUnit, weight: FontWeight) =
    MaterialTheme.typography.bodyMedium.copy(fontSize = size, fontWeight = weight)
