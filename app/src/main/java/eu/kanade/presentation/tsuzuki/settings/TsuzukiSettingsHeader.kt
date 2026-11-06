package eu.kanade.presentation.tsuzuki.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.presentation.more.settings.widget.AppThemeModePreferenceWidget
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiThemeReveal
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Top of Settings in the Tsuzuki themes (board "Settings"): a search tile and the Colour / Dot
 * theme cards. The cards' mini previews use each theme's own colours on purpose: they show the
 * theme you would get, not the current one.
 */
@Composable
fun TsuzukiSettingsHeader(
    appTheme: AppTheme,
    onSearch: () -> Unit,
    onPickTheme: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = TsuzukiTheme.tokens
    val up: (String) -> String = { if (t.isDot) it.uppercase() else it }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val searchShape = RoundedCornerShape(25.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(searchShape)
                .background(t.tile)
                .then(t.tileBorder?.let { Modifier.border(1.dp, it, searchShape) } ?: Modifier)
                .clickable(role = Role.Button, onClick = onSearch)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = muted, modifier = Modifier.size(18.dp))
            Text(up(stringResource(MR.strings.tsz_search_settings)), color = muted, fontSize = 14.sp)
        }

        val tileShape = RoundedCornerShape(24.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(tileShape)
                .background(t.tile)
                .then(t.tileBorder?.let { Modifier.border(1.dp, it, tileShape) } ?: Modifier)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(MR.strings.tsz_theme).uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = muted,
            )
            TsuzukiThemeCards(appTheme, onPickTheme)
        }
    }
}

/** The Colour / Dot cards (Settings and SettingsAppearance boards); picking one plays the circular reveal. */
@Composable
fun TsuzukiThemeCards(appTheme: AppTheme, onPickTheme: (AppTheme) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val colourClick = TsuzukiThemeReveal.click(appTheme, AppTheme.COLOUR) { onPickTheme(AppTheme.COLOUR) }
        val dotClick = TsuzukiThemeReveal.click(appTheme, AppTheme.DOT) { onPickTheme(AppTheme.DOT) }
        ThemeCard(
            selected = appTheme == AppTheme.COLOUR,
            name = stringResource(MR.strings.theme_lnr_colour),
            blurb = stringResource(MR.strings.tsz_theme_colour_sub),
            onClick = colourClick.onClick,
            modifier = Modifier.weight(1f).then(colourClick.modifier),
        ) { ColourPreview() }
        ThemeCard(
            selected = appTheme == AppTheme.DOT,
            name = stringResource(MR.strings.theme_lnr_dot).uppercase(),
            blurb = stringResource(MR.strings.tsz_theme_dot_sub),
            onClick = dotClick.onClick,
            modifier = Modifier.weight(1f).then(dotClick.modifier),
        ) { DotPreview() }
    }
}

/** Appearance offers the same two themes as onboarding and the Settings header. */
@Composable
fun TsuzukiAppearanceThemePicker(
    appTheme: AppTheme,
    themeMode: ThemeMode,
    onPickTheme: (AppTheme) -> Unit,
    onPickMode: (ThemeMode) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        TsuzukiThemeCards(appTheme, onPickTheme = {
            if (it != appTheme) {
                onPickTheme(it)
                (context as? android.app.Activity)?.let { a -> androidx.core.app.ActivityCompat.recreate(a) }
            }
        })
        AppThemeModePreferenceWidget(value = themeMode, onItemClick = onPickMode)
    }
}

@Composable
private fun ThemeCard(
    selected: Boolean,
    name: String,
    blurb: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val tokens = TsuzukiTheme.tokens
    Column(
        modifier
            .clip(shape)
            .background(tokens.tile)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) tokens.accent else tokens.tileBorder ?: tokens.track,
                shape,
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        preview()
        Column(Modifier.padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(blurb, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

@Composable
private fun ColourPreview() {
    Column(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1C2B4A))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .height(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE9B44C)),
            )
            Box(
                Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(2) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.12f)),
                )
            }
        }
    }
}

@Composable
private fun DotPreview() {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(shape)
            .background(Color.Black)
            .border(1.dp, Color(0xFF262626), shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .height(24.dp)
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp)),
            )
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF2D2D)),
            )
        }
        Row(Modifier.padding(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(12) { k ->
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(
                            when {
                                k < 7 -> Color.White
                                k == 7 -> Color(0xFFFF2D2D)
                                else -> Color(0xFF2E2E2E)
                            },
                        ),
                )
            }
        }
    }
}
