package eu.kanade.presentation.tsuzuki.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * "What do you read?" cards. Light novels are always on (no preference hides them); Manga & manhwa
 * is the inverse of [BasePreferences.hideMangaUi].
 */
@Composable
internal fun TsuzukiMediaTypeCards() {
    val preference = remember { Injekt.get<BasePreferences>().hideMangaUi }
    val hideMangaUi by preference.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        MediaTypeCard(
            title = stringResource(MR.strings.tsz_onboarding_media_novels),
            subtitle = stringResource(MR.strings.tsz_onboarding_media_novels_body),
            selected = true,
            art = MediaArt.NOVELS,
            onClick = null,
        )
        MediaTypeCard(
            title = stringResource(MR.strings.tsz_onboarding_media_manga),
            subtitle = stringResource(MR.strings.tsz_onboarding_media_manga_body),
            selected = !hideMangaUi,
            art = MediaArt.MANGA,
            onClick = { preference.set(!hideMangaUi) },
        )
    }
}

private enum class MediaArt { NOVELS, MANGA }

/** [onClick] == null means always selected: disabled-toggle semantics, no press effect. */
@Composable
private fun MediaTypeCard(title: String, subtitle: String, selected: Boolean, art: MediaArt, onClick: (() -> Unit)?) {
    val tokens = TsuzukiTheme.tokens
    val shape = RoundedCornerShape(22.dp)
    val borderColor = when {
        selected -> tokens.accent
        else -> tokens.tileBorder ?: Color.Transparent
    }
    val base = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(tokens.tile)
        .border(if (selected) 2.dp else 1.dp, borderColor, shape)
    val modifier = if (onClick != null) {
        base.tsuzukiClickable(role = Role.Checkbox, onClick = onClick)
    } else {
        base
    }
    Row(
        modifier
            .semantics(mergeDescendants = true) { toggleableState = ToggleableState(selected) }
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaArtBlock(art, Modifier.weight(1f).fillMaxHeight().heightIn(min = 88.dp))
        Column(
            Modifier.weight(3f).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                tsuzukiCase(title).orEmpty(),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, lineHeight = 26.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                tsuzukiCase(subtitle).orEmpty(),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CheckMark(selected, Modifier.padding(end = 16.dp))
    }
}

@Composable
private fun CheckMark(selected: Boolean, modifier: Modifier = Modifier) {
    val tokens = TsuzukiTheme.tokens
    val shape = if (tokens.isDot) RoundedCornerShape(6.dp) else CircleShape
    val box = modifier.size(28.dp).clip(shape)
    if (selected) {
        Box(box.background(tokens.accent), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Check, null, Modifier.size(18.dp), tint = tokens.onAccent)
        }
    } else {
        Box(box.border(2.dp, tokens.dotOn.copy(alpha = 0.25f), shape))
    }
}

@Composable
private fun MediaArtBlock(art: MediaArt, modifier: Modifier = Modifier) {
    val tokens = TsuzukiTheme.tokens
    Canvas(modifier.clipToBounds().clearAndSetSemantics { }) {
        when (art) {
            MediaArt.NOVELS -> {
                // Colour: navy depth. Dot: dark greys only.
                val top = if (tokens.isDot) Color(0xFF3A3A3A) else Color(0xFF34476E)
                val bottom = if (tokens.isDot) Color(0xFF0A0A0A) else Color(0xFF141E36)
                drawRect(Brush.verticalGradient(listOf(top, bottom)))
            }
            MediaArt.MANGA -> {
                // Light grey diagonal stripes on white, as on the board (grey keeps Dot's one-colour rule).
                drawRect(Color(0xFFF2F2F2))
                val step = 9.dp.toPx()
                val width = 3.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(
                        Color(0xFFD2D2D2),
                        Offset(x, size.height),
                        Offset(x + size.height, 0f),
                        strokeWidth = width,
                    )
                    x += step
                }
            }
        }
    }
}
