package eu.kanade.presentation.tsuzuki.customsource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.tsuzuki.settings.isTsuzukiStyle
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.ui.customsource.SelectorWizardStep
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Top bar of the site picker. In the Tsuzuki themes: back circle, display title "Site picker", "Step n of m · name"
 * and the refresh action in a circle (08-tools/ElementSelector). Other themes get the stock [AppBar].
 */
@Composable
fun TsuzukiPickerAppBar(
    title: String,
    subtitle: String,
    navigateUp: () -> Unit,
    navigationIcon: ImageVector,
    actions: @Composable RowScope.() -> Unit,
    stepIndex: Int,
    stepCount: Int,
    stepTitle: StringResource,
) {
    if (!isTsuzukiStyle()) {
        AppBar(
            title = title,
            subtitle = subtitle,
            navigateUp = navigateUp,
            navigationIcon = navigationIcon,
            actions = actions,
        )
        return
    }
    val tokens = TsuzukiTheme.tokens
    TsuzukiToolHeader(
        title = stringResource(MR.strings.tsz_picker_title),
        subtitle = stringResource(MR.strings.tsz_picker_step, stepIndex + 1, stepCount, stringResource(stepTitle)),
        onBack = navigateUp,
        modifier = Modifier.statusBarsPadding(),
        trailing = {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(tokens.tile)
                    .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, CircleShape) } ?: Modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) { actions() }
        },
    )
}

/** Step guidance tile with action pills (Test, Auto-detect, Selected n); same callbacks as the stock card. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TsuzukiStepInstructionCard(
    step: SelectorWizardStep,
    selectedCount: Int,
    searchStatus: String?,
    onViewSelected: () -> Unit,
    onTest: (() -> Unit)?,
    testEnabled: Boolean,
    onSetupSearch: (() -> Unit)?,
    onAutoDetect: (() -> Unit)?,
    paginationStatus: String?,
    onReusePagination: (() -> Unit)?,
    compact: Boolean,
) {
    val tokens = TsuzukiTheme.tokens
    var showHelp by remember { mutableStateOf(false) }
    val actions = listOfNotNull(
        onReusePagination?.let { TsuzukiPickerAction(TDMR.strings.selector_reuse_popular_pagination, true, it) },
        onSetupSearch?.let { TsuzukiPickerAction(TDMR.strings.selector_setup_search, true, it) },
        onAutoDetect?.let { TsuzukiPickerAction(TDMR.strings.selector_auto_detect, true, it) },
        onTest?.let { TsuzukiPickerAction(TDMR.strings.selector_test_section, testEnabled, it) },
    )
    val searchOk = searchStatus?.startsWith("ok:") == true
    TsuzukiTile(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = if (compact) 2.dp else 6.dp),
        padding = if (compact) 10.dp else 14.dp,
    ) {
        if (!compact || step.detailed) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!compact) {
                    Text(
                        text = stringResource(step.descriptionRes),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.dotOn,
                    )
                } else {
                    Box(Modifier.weight(1f))
                }
                if (step.detailed) {
                    IconButton(onClick = { showHelp = true }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(TDMR.strings.selector_step_help),
                            tint = tokens.accent,
                        )
                    }
                }
            }
        }
        if (showHelp) {
            AlertDialog(
                onDismissRequest = { showHelp = false },
                title = { Text(stringResource(step.titleRes)) },
                text = { Text(stringResource(step.detailedHelpRes)) },
                confirmButton = {
                    TextButton(onClick = { showHelp = false }) { Text(stringResource(MR.strings.action_ok)) }
                },
            )
        }
        if (step == SelectorWizardStep.SEARCH && searchStatus != null) {
            Text(
                text = if (searchOk) {
                    stringResource(TDMR.strings.selector_search_captured, searchStatus.removePrefix("ok:"))
                } else {
                    stringResource(TDMR.strings.selector_search_not_captured)
                },
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (searchOk) tokens.accent else tokens.dotOn.copy(alpha = 0.72f),
            )
        }
        if (paginationStatus != null) {
            Text(
                text = paginationStatus,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = tokens.accent,
            )
        }
        if (actions.isNotEmpty() || selectedCount > 0) {
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actions.forEach { action ->
                    TsuzukiPill(
                        text = stringResource(action.label),
                        selected = false,
                        onClick = { if (action.enabled) action.onClick() },
                        modifier = Modifier.alpha(if (action.enabled) 1f else 0.4f),
                        role = Role.Button,
                    )
                }
                if (selectedCount > 0) {
                    TsuzukiPill(
                        text = stringResource(TDMR.strings.selector_view_selected, selectedCount),
                        selected = true,
                        onClick = onViewSelected,
                        role = Role.Button,
                    )
                }
            }
        }
    }
}

internal class TsuzukiPickerAction(
    val label: StringResource,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

/** Back, select-element toggle and the accent next/save pill (board's bottom bar). */
@Composable
internal fun TsuzukiStepNavigationBar(
    isFirstStep: Boolean,
    isLastStep: Boolean,
    canProceed: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit,
    selectionModeEnabled: Boolean,
    onToggleSelection: (() -> Unit)?,
) {
    val tokens = TsuzukiTheme.tokens
    val selectLabel = if (selectionModeEnabled) {
        TDMR.strings.selector_selection_on
    } else {
        TDMR.strings.selector_select_element
    }
    val primaryLabel = if (isLastStep) {
        TDMR.strings.custom_selector_save_source
    } else {
        TDMR.strings.custom_selector_next_step
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .clip(CircleShape)
                .background(tokens.tile)
                .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, CircleShape) } ?: Modifier)
                .alpha(if (isFirstStep) 0.4f else 1f)
                .tsuzukiClickable(enabled = !isFirstStep, role = Role.Button, onClick = onPrevious)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                tint = tokens.dotOn,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = tsuzukiCase(stringResource(TDMR.strings.custom_selector_previous)).orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = tokens.dotOn,
                maxLines = 1,
            )
        }
        if (!isLastStep && onToggleSelection != null) {
            TsuzukiPill(
                text = stringResource(selectLabel),
                selected = selectionModeEnabled,
                onClick = onToggleSelection,
                modifier = Modifier.weight(1f),
                role = Role.Switch,
            )
        } else {
            Box(Modifier.weight(1f))
        }
        Button(
            onClick = if (isLastStep) onComplete else onNext,
            enabled = isLastStep || canProceed,
            modifier = Modifier.heightIn(min = 56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = tokens.accent, contentColor = tokens.onAccent),
        ) {
            Text(
                text = tsuzukiCase(stringResource(primaryLabel)).orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

/** Address strip above the page: back / forward circles and a pill URL field. */
@Composable
internal fun TsuzukiPickerUrlBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    currentUrl: String,
    onUrlSubmit: (String) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    var urlText by remember(currentUrl) { mutableStateOf(currentUrl) }
    val muted = tokens.dotOn.copy(alpha = 0.72f)
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onBack, enabled = canGoBack, modifier = Modifier.size(40.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(MR.strings.nav_zone_prev), tint = tokens.dotOn)
        }
        IconButton(onClick = onForward, enabled = canGoForward, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                stringResource(MR.strings.nav_zone_next),
                tint = tokens.dotOn,
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .clip(shape)
                .background(tokens.tile)
                .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = urlText,
                onValueChange = { urlText = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = tokens.dotOn),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onUrlSubmit(urlText) }),
                cursorBrush = SolidColor(tokens.accent),
                decorationBox = { field ->
                    Box {
                        if (urlText.isEmpty()) {
                            Text(
                                text = "https://",
                                style = MaterialTheme.typography.bodySmall,
                                color = muted,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        field()
                    }
                },
            )
            IconButton(onClick = { onUrlSubmit(urlText) }) {
                Icon(Icons.Outlined.Search, stringResource(MR.strings.action_search), tint = muted)
            }
        }
    }
}
