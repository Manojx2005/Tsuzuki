package eu.kanade.tachiyomi.tsuzuki.anime.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPillRow
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiMenuLabel
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.tsuzuki.anime.TsuzukiAnime
import eu.kanade.tachiyomi.tsuzuki.anime.ui.supportsAnimePictureInPicture
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.presentation.core.i18n.stringResource as mokoStringResource

internal object AnimePlayerSettingsScreen : Screen() {
    @Composable
    override fun Content() {
        if (!TsuzukiAnime.enabled) return
        val back = LocalBackPress.currentOrThrow
        val preferences = remember { AnimePlayerPreferences(Injekt.get()) }
        var settings by remember { mutableStateOf(preferences.get()) }
        val autoplay by preferences.autoplay.collectAsState()
        val pictureInPicture by preferences.pictureInPicture.collectAsState()
        val supportsPip = LocalContext.current.supportsAnimePictureInPicture()
        var showSpeed by remember { mutableStateOf(false) }
        var showAniSkip by remember { mutableStateOf(false) }
        var languageKind by remember { mutableStateOf<AnimeTrackKind?>(null) }
        val tokens = TsuzukiTheme.tokens

        fun update(settingsUpdate: AnimePlayerSettings) {
            preferences.set(settingsUpdate)
            settings = preferences.get()
        }

        Scaffold(
            modifier = Modifier.tsuzukiBackground(tokens),
            containerColor = Color.Transparent,
            topBar = {
                AppBar(
                    title = mokoStringResource(MR.strings.tsz_anime_player_settings),
                    navigateUp = back::invoke,
                )
            },
        ) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(if (LocalDensity.current.fontScale >= 1.5f) 220.dp else 158.dp),
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    PlayerSettingCard(
                        stringResource(R.string.tsz_anime_aniskip),
                        Icons.Outlined.SkipNext,
                        active = settings.aniSkipMode != AniSkipMode.OFF,
                    ) { color ->
                        TextButton(
                            onClick = { showAniSkip = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = color),
                        ) {
                            Text(tsuzukiMenuLabel(aniSkipModeLabel(settings.aniSkipMode)))
                        }
                        if (settings.aniSkipMode == AniSkipMode.AUTO) {
                            AnimePlayerStepper(
                                label = stringResource(R.string.tsz_anime_skip_intro_delay),
                                value = stringResource(R.string.tsz_anime_skip_seconds, settings.aniSkipIntroDelay),
                                contentColor = color,
                                decrease = {
                                    update(settings.copy(aniSkipIntroDelay = settings.aniSkipIntroDelay - 1))
                                },
                                increase = {
                                    update(settings.copy(aniSkipIntroDelay = settings.aniSkipIntroDelay + 1))
                                },
                            )
                        }
                    }
                }
                item {
                    PlayerSettingCard(
                        stringResource(R.string.tsz_anime_player_autoplay),
                        Icons.Outlined.PlayArrow,
                        active = autoplay,
                    ) {
                        Switch(
                            checked = autoplay,
                            onCheckedChange = preferences.autoplay::set,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = tokens.onAccent,
                                checkedTrackColor = tokens.onAccent.copy(alpha = 0.24f),
                            ),
                        )
                    }
                }
                item {
                    PlayerSettingCard(stringResource(R.string.tsz_anime_player_speed), Icons.Outlined.Speed) {
                        TextButton(onClick = { showSpeed = true }) { Text(formatAnimeSpeed(settings.speed)) }
                    }
                }
                // Full width: the subtitle look rows (AY3) are too many for one grid column.
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PlayerSettingCard(
                        stringResource(R.string.tsz_anime_player_subtitles),
                        Icons.Outlined.TextFields,
                    ) {
                        AnimePlayerStepper(
                            label = stringResource(R.string.tsz_anime_player_subtitle_size),
                            value = formatAnimePercent(settings.subtitleScale),
                            decrease = { update(settings.copy(subtitleScale = settings.subtitleScale - 0.1f)) },
                            increase = { update(settings.copy(subtitleScale = settings.subtitleScale + 0.1f)) },
                        )
                        TextButton(onClick = { languageKind = AnimeTrackKind.SUBTITLE }) {
                            Text(tsuzukiMenuLabel(stringResource(R.string.tsz_anime_player_preferred_languages)))
                        }
                        SubtitleStyleRows(settings.subtitleStyle) { update(settings.copy(subtitleStyle = it)) }
                    }
                }
                item {
                    PlayerSettingCard(stringResource(R.string.tsz_anime_player_audio), Icons.Outlined.Headphones) {
                        TextButton(onClick = { languageKind = AnimeTrackKind.AUDIO }) {
                            Text(tsuzukiMenuLabel(stringResource(R.string.tsz_anime_player_preferred_languages)))
                        }
                    }
                }
                item {
                    PlayerSettingCard(stringResource(R.string.tsz_anime_player_video), Icons.Outlined.Videocam) {
                        if (supportsPip) {
                            PlayerSettingSwitch(
                                label = stringResource(R.string.tsz_anime_player_picture_in_picture),
                                checked = pictureInPicture,
                                onChange = preferences.pictureInPicture::set,
                            )
                        }
                        PlayerSettingSwitch(
                            label = stringResource(R.string.tsz_anime_player_decoder_hardware),
                            checked = settings.hardwareDecoding,
                            onChange = { update(settings.copy(hardwareDecoding = it)) },
                        )
                    }
                }
                item {
                    PlayerSettingCard(stringResource(R.string.tsz_anime_player_gestures), Icons.Outlined.TouchApp) {
                        PlayerSettingSwitch(
                            stringResource(R.string.tsz_anime_player_seek_gesture),
                            settings.seekGesture,
                        ) { update(settings.copy(seekGesture = it)) }
                        PlayerSettingSwitch(
                            stringResource(R.string.tsz_anime_player_brightness),
                            settings.brightnessGesture,
                        ) { update(settings.copy(brightnessGesture = it)) }
                        PlayerSettingSwitch(
                            stringResource(R.string.tsz_anime_player_volume),
                            settings.volumeGesture,
                        ) { update(settings.copy(volumeGesture = it)) }
                    }
                }
            }
        }
        languageKind?.let { kind ->
            val preference = preferences.languages(kind)
            AnimeLanguageOrderDialog(
                kind = kind,
                initial = animeLanguageOrder(preference.get(), kind),
                onDismiss = { languageKind = null },
                onSave = {
                    preference.set(it.joinToString(","))
                    languageKind = null
                },
            )
        }
        if (showSpeed) {
            AlertDialog(
                onDismissRequest = { showSpeed = false },
                title = { Text(stringResource(R.string.tsz_anime_player_speed)) },
                text = {
                    LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(animePlaybackSpeeds) { speed ->
                            AnimePlayerOption(formatAnimeSpeed(speed), selected = speed == settings.speed) {
                                update(settings.copy(speed = speed))
                                showSpeed = false
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeed = false }) {
                        Text(mokoStringResource(MR.strings.action_cancel))
                    }
                },
            )
        }
        if (showAniSkip) {
            AlertDialog(
                onDismissRequest = { showAniSkip = false },
                title = { Text(tsuzukiMenuLabel(stringResource(R.string.tsz_anime_aniskip))) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AniSkipMode.entries.forEach { mode ->
                            AnimePlayerOption(aniSkipModeLabel(mode), selected = settings.aniSkipMode == mode) {
                                update(settings.copy(aniSkipMode = mode))
                                showAniSkip = false
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAniSkip = false }) {
                        Text(mokoStringResource(MR.strings.action_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun aniSkipModeLabel(mode: AniSkipMode): String = stringResource(
    when (mode) {
        AniSkipMode.OFF -> R.string.tsz_anime_player_track_off
        AniSkipMode.BUTTON -> R.string.tsz_anime_skip_button
        AniSkipMode.AUTO -> R.string.tsz_anime_skip_auto
    },
)

@Composable
private fun PlayerSettingCard(
    title: String,
    icon: ImageVector,
    active: Boolean = false,
    content: @Composable (Color) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    val ink = if (active) tokens.onAccent else tokens.dotOn
    TsuzukiTile(Modifier.fillMaxWidth(), padding = 0.dp) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 108.dp)
                .background(if (active) tokens.accent else Color.Transparent).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = ink)
            Text(tsuzukiMenuLabel(title), color = ink, style = MaterialTheme.typography.titleSmall)
            content(ink)
        }
    }
}

@Composable
private fun PlayerSettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            tsuzukiMenuLabel(label),
            modifier = Modifier.weight(1f),
            color = TsuzukiTheme.tokens.mutedText,
            style = MaterialTheme.typography.labelSmall,
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** The subtitle look (AY3): rows inside the Subtitles card; the player applies changes live. */
@Composable
private fun SubtitleStyleRows(style: AnimeSubtitleStyle, onChange: (AnimeSubtitleStyle) -> Unit) {
    fun set(next: AnimeSubtitleStyle) = onChange(next.bounded())
    AnimePlayerStepper(
        label = stringResource(R.string.tsz_anime_player_sub_font_size),
        value = style.fontSize.toString(),
        decrease = { set(style.copy(fontSize = style.fontSize - AnimeSubtitleStyle.FONT_SIZE_STEP)) },
        increase = { set(style.copy(fontSize = style.fontSize + AnimeSubtitleStyle.FONT_SIZE_STEP)) },
    )
    PlayerSettingSwitch(stringResource(R.string.tsz_anime_player_sub_bold), style.bold) {
        set(style.copy(bold = it))
    }
    PlayerSettingLabel(stringResource(R.string.tsz_anime_player_sub_color))
    TsuzukiPillRow(
        labels = AnimeSubtitleColor.entries.map { stringResource(it.label) },
        selected = style.color.ordinal,
        onClick = { set(style.copy(color = AnimeSubtitleColor.entries[it])) },
    )
    PlayerSettingLabel(stringResource(R.string.tsz_anime_player_sub_border))
    TsuzukiPillRow(
        labels = AnimeSubtitleBorder.entries.map { stringResource(it.label) },
        selected = style.border.ordinal,
        onClick = { set(style.copy(border = AnimeSubtitleBorder.entries[it])) },
    )
    if (style.border != AnimeSubtitleBorder.NONE) {
        AnimePlayerStepper(
            label = stringResource(R.string.tsz_anime_player_sub_border_size),
            value = style.borderSize.toString(),
            decrease = { set(style.copy(borderSize = style.borderSize - 1)) },
            increase = { set(style.copy(borderSize = style.borderSize + 1)) },
        )
    }
    PlayerSettingSwitch(stringResource(R.string.tsz_anime_player_sub_background), style.background) {
        set(style.copy(background = it))
    }
    AnimePlayerStepper(
        label = stringResource(R.string.tsz_anime_player_sub_position),
        value = "${style.position}%",
        decrease = { set(style.copy(position = style.position - AnimeSubtitleStyle.POSITION_STEP)) },
        increase = { set(style.copy(position = style.position + AnimeSubtitleStyle.POSITION_STEP)) },
    )
    PlayerSettingSwitch(stringResource(R.string.tsz_anime_player_sub_override_ass), style.overrideAss) {
        set(style.copy(overrideAss = it))
    }
}

@Composable
private fun PlayerSettingLabel(text: String) {
    Text(
        tsuzukiMenuLabel(text),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        color = TsuzukiTheme.tokens.mutedText,
        style = MaterialTheme.typography.labelMedium,
    )
}

private val AnimeSubtitleColor.label: Int
    get() = when (this) {
        AnimeSubtitleColor.WHITE -> R.string.tsz_anime_player_sub_white
        AnimeSubtitleColor.YELLOW -> R.string.tsz_anime_player_sub_yellow
        AnimeSubtitleColor.CYAN -> R.string.tsz_anime_player_sub_cyan
        AnimeSubtitleColor.GREEN -> R.string.tsz_anime_player_sub_green
    }

private val AnimeSubtitleBorder.label: Int
    get() = when (this) {
        AnimeSubtitleBorder.BLACK -> R.string.tsz_anime_player_sub_black
        AnimeSubtitleBorder.GREY -> R.string.tsz_anime_player_sub_grey
        AnimeSubtitleBorder.NONE -> R.string.tsz_anime_player_sub_none
    }
