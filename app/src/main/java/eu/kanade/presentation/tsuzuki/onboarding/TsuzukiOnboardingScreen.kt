package eu.kanade.presentation.tsuzuki.onboarding

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import eu.kanade.domain.extension.interactor.GetExtensionLanguages
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.more.onboarding.PermissionStep
import eu.kanade.presentation.more.onboarding.StorageStep
import eu.kanade.presentation.theme.tsuzuki.LocalTsuzukiTokens
import eu.kanade.presentation.theme.tsuzuki.TsuzukiMotion
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.rememberReduceMotion
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.theme.tsuzuki.tsuzukiTween
import eu.kanade.presentation.tsuzuki.settings.TsuzukiThemeCards
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun TsuzukiOnboardingScreen(
    onComplete: () -> Unit,
    onRestoreBackup: () -> Unit,
    onImportLNReader: () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    // Voyager's screen SaveableStateHolder restores this after Activity recreation/theme reveal.
    var step by rememberSaveable { mutableIntStateOf(0) }
    val pages = OnboardingPage.entries
    val storage = remember { StorageStep() }
    val permissions = remember { PermissionStep() }
    val storagePref = remember { Injekt.get<StoragePreferences>().baseStorageDirectory }
    val storageLocation by storagePref.changes().collectAsState(storagePref.get())
    val storageConfigured = storageLocation.isNotEmpty() && storagePref.isSet()
    val reducedMotion = rememberReduceMotion()
    val easing = if (tokens.isDot) TsuzukiMotion.stepped(TsuzukiMotion.Standard) else TsuzukiMotion.Standard

    BackHandler(enabled = step > 0) { step-- }

    Box(Modifier.fillMaxSize().tsuzukiBackground(tokens).safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(
                max = 480.dp,
            ).fillMaxSize().padding(horizontal = 22.dp).padding(top = 32.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(
                Modifier.semantics {
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(step.toFloat(), 0f..pages.lastIndex.toFloat(), pages.size - 2)
                },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                pages.forEachIndexed { index, _ ->
                    val width by animateDpAsState(
                        if (index == step) 28.dp else 6.dp,
                        animationSpec = tsuzukiTween(TsuzukiMotion.TOGGLE_MS),
                        label = "onboardingPip",
                    )
                    Box(
                        Modifier.size(width, 6.dp).clip(RoundedCornerShape(3.dp)).background(
                            if (index <=
                                step
                            ) {
                                tokens.accent
                            } else {
                                tokens.dotOn.copy(alpha = 0.2f)
                            },
                        ),
                    )
                }
            }
            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(tween(TsuzukiMotion.REDUCED_MS)) togetherWith fadeOut(tween(TsuzukiMotion.REDUCED_MS))
                    } else {
                        val direction = if (targetState > initialState) 1 else -1
                        slideInHorizontally(tween(TsuzukiMotion.SHEET_MS, easing = easing)) {
                            direction * it
                        } togetherWith
                            slideOutHorizontally(tween(TsuzukiMotion.SHEET_MS, easing = easing)) { -direction * it }
                    }
                },
                label = "onboardingPage",
            ) { index ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    when (pages[index]) {
                        OnboardingPage.WELCOME -> {
                            WelcomeMark()
                            Heading(stringResource(MR.strings.tsz_onboarding_welcome), welcome = true)
                            Muted(stringResource(MR.strings.tsz_onboarding_welcome_body))
                        }
                        OnboardingPage.LANGUAGES -> {
                            Heading(stringResource(MR.strings.tsz_onboarding_languages))
                            Muted(stringResource(MR.strings.tsz_onboarding_languages_body))
                            LanguagePills()
                        }
                        OnboardingPage.LOOK -> {
                            Heading(stringResource(MR.strings.tsz_onboarding_look))
                            Muted(stringResource(MR.strings.tsz_onboarding_look_body))
                            val context = LocalContext.current
                            val preference = remember { Injekt.get<UiPreferences>().appTheme }
                            val theme by preference.changes().collectAsState(preference.get())
                            TsuzukiThemeCards(theme, onPickTheme = { picked ->
                                if (picked != theme) {
                                    preference.set(picked)
                                    (context as? Activity)?.let(ActivityCompat::recreate)
                                }
                            })
                        }
                        OnboardingPage.STORAGE -> {
                            Heading(stringResource(MR.strings.tsz_onboarding_storage))
                            storage.Content()
                        }
                        OnboardingPage.PERMISSIONS -> {
                            Heading(stringResource(MR.strings.tsz_onboarding_permissions))
                            Muted(stringResource(MR.strings.tsz_onboarding_permissions_body))
                            permissions.Content()
                        }
                        OnboardingPage.IMPORT -> {
                            Heading(stringResource(MR.strings.tsz_onboarding_import))
                            Muted(stringResource(MR.strings.tsz_onboarding_import_body))
                            ImportTile(
                                stringResource(MR.strings.tsz_onboarding_lnreader),
                                Icons.Outlined.FileUpload,
                                onImportLNReader,
                            )
                            ImportTile(
                                stringResource(MR.strings.tsz_onboarding_mihon),
                                Icons.Outlined.LibraryBooks,
                                onRestoreBackup,
                            )
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        if (step > 0) {
                            step--
                        } else {
                            val target = onboardingSkipTarget(storageConfigured)
                            if (target == null) onComplete() else step = target.ordinal
                        }
                    },
                    modifier = Modifier.heightIn(min = 58.dp),
                ) {
                    Text(
                        tsuzukiCase(
                            stringResource(
                                if (step ==
                                    0
                                ) {
                                    MR.strings.tsz_onboarding_skip
                                } else {
                                    MR.strings.tsz_onboarding_back
                                },
                            ),
                        ).orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PrimaryButton(
                    text = stringResource(
                        when (pages[step]) {
                            OnboardingPage.WELCOME -> MR.strings.tsz_onboarding_start
                            OnboardingPage.IMPORT -> MR.strings.tsz_onboarding_finish
                            else -> MR.strings.tsz_onboarding_continue
                        },
                    ),
                    modifier = Modifier.weight(1f),
                    enabled = pages[step] != OnboardingPage.STORAGE || storageConfigured,
                    onClick = { if (step == pages.lastIndex) onComplete() else step++ },
                )
            }
        }
    }
}

@Composable
private fun LanguagePills() {
    val preference = remember { Injekt.get<SourcePreferences>().enabledLanguages }
    val selected by preference.changes().collectAsState(preference.get())
    val languagesFlow = remember { Injekt.get<GetExtensionLanguages>().subscribe() }
    val available by languagesFlow.collectAsState(emptyList())
    val context = LocalContext.current
    val languages = onboardingLanguageChoices(available, selected).sortedWith(LocaleHelper.comparator)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        languages.forEach { language ->
            TsuzukiPill(
                text = LocaleHelper.getSourceDisplayName(language, context),
                selected = language in selected,
                onClick = { preference.set(toggleOnboardingLanguage(preference.get(), language)) },
                role = Role.Checkbox,
                modifier = Modifier.semantics { toggleableState = ToggleableState(language in selected) },
            )
        }
    }
}

@Composable
private fun WelcomeMark() {
    val tokens = TsuzukiTheme.tokens
    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
        if (tokens.isDot) {
            Image(painterResource(R.drawable.ic_tsuzuki_dots), null, Modifier.size(144.dp))
        } else {
            Icon(painterResource(R.drawable.ic_mihon), null, Modifier.size(144.dp), tint = tokens.accent)
        }
    }
}

@Composable
private fun Heading(text: String, welcome: Boolean = false) {
    val size = if (welcome) 46.sp else 40.sp
    Text(
        tsuzukiCase(text).orEmpty(),
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = size,
            lineHeight =
            size * 0.98f,
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun Muted(text: String, preserveCase: Boolean = false) {
    Text(
        text = if (preserveCase) text else tsuzukiCase(text).orEmpty(),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.5.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val tokens = TsuzukiTheme.tokens
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 58.dp),
        enabled = enabled,
        shape = RoundedCornerShape(29.dp),
        colors = ButtonDefaults.buttonColors(containerColor = tokens.accent, contentColor = tokens.onAccent),
    ) {
        Text(
            tsuzukiCase(text).orEmpty(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun ImportTile(text: String, icon: ImageVector, onClick: () -> Unit) {
    OnboardingTile(Modifier.fillMaxWidth().tsuzukiClickable(role = Role.Button, onClick = onClick), padding = 16.dp) {
        Row(
            Modifier.heightIn(min = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, null, tint = TsuzukiTheme.tokens.accent)
            Text(
                tsuzukiCase(text).orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Presentation only: StorageStep owns the existing SAF picker, error handling and completion observation. */
@Composable
fun TsuzukiStorageContent(location: String, onSelect: () -> Unit, onHelp: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        OnboardingTile(Modifier.fillMaxWidth()) {
            Muted(
                stringResource(MR.strings.onboarding_storage_info, stringResource(MR.strings.app_name), location),
                preserveCase = true,
            )
        }
        PrimaryButton(stringResource(MR.strings.onboarding_storage_action_select), onSelect, Modifier.fillMaxWidth())
        Muted(stringResource(MR.strings.onboarding_storage_help_info, stringResource(MR.strings.app_name)))
        TextButton(onClick = onHelp) {
            Text(
                tsuzukiCase(stringResource(MR.strings.onboarding_storage_help_action)).orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = TsuzukiTheme.tokens.accent,
            )
        }
    }
}

/** PermissionStep retains lifecycle checks, permission launchers and platform-version guards. */
@Composable
fun TsuzukiPermissionTile(
    title: String,
    subtitle: String,
    granted: Boolean,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingTile(modifier.fillMaxWidth().padding(bottom = 12.dp), padding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                tsuzukiCase(title).orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Muted(subtitle)
            if (granted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.Check, null, tint = TsuzukiTheme.tokens.accent)
                    Text(
                        tsuzukiCase(stringResource(MR.strings.tsz_onboarding_granted)).orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = TsuzukiTheme.tokens.accent,
                    )
                }
            } else {
                TsuzukiPill(
                    stringResource(MR.strings.onboarding_permission_action_grant),
                    selected = true,
                    onClick = onGrant,
                    role = Role.Button,
                )
            }
        }
    }
}

/** The onboarding board uses 22 dp tiles in both skins, independent of other screens' tile radii. */
@Composable
private fun OnboardingTile(
    modifier: Modifier = Modifier,
    padding: androidx.compose.ui.unit.Dp = 20.dp,
    content: @Composable () -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    CompositionLocalProvider(LocalTsuzukiTokens provides tokens.copy(tileRadius = 22.dp)) {
        TsuzukiTile(modifier, padding, content)
    }
}
