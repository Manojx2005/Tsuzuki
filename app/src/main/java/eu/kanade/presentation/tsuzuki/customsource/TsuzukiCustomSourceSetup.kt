package eu.kanade.presentation.tsuzuki.customsource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.tachiyomi.ui.customsource.SelectorWizardStep
import eu.kanade.tachiyomi.ui.customsource.SourceFeatures
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource

private fun SourceFeatureChip.label(): StringResource = when (this) {
    SourceFeatureChip.POPULAR -> TDMR.strings.selector_feature_popular
    SourceFeatureChip.POPULAR_PAGES -> TDMR.strings.selector_feature_popular_pagination
    SourceFeatureChip.LATEST -> TDMR.strings.selector_feature_latest
    SourceFeatureChip.LATEST_PAGES -> TDMR.strings.selector_feature_latest_pagination
    SourceFeatureChip.SEARCH -> TDMR.strings.selector_feature_search
    SourceFeatureChip.SEARCH_PAGES -> TDMR.strings.selector_feature_search_pagination
    SourceFeatureChip.CHAPTER_PATTERN -> TDMR.strings.selector_feature_chapter_generate
    SourceFeatureChip.CHAPTER_PAGE -> TDMR.strings.selector_feature_chapter_separate_page
    SourceFeatureChip.CHAPTER_PAGES -> TDMR.strings.selector_feature_chapter_pagination
}

/** "Make a source" setup page (08-tools/CustomSource): address tile, feature pills, wizard steps, accent start button. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TsuzukiCustomSourceSetup(
    initialUrl: String,
    onBack: () -> Unit,
    onStart: (url: String, features: SourceFeatures) -> Unit,
) {
    val tokens = TsuzukiTheme.tokens
    var websiteUrl by remember { mutableStateOf(initialUrl) }
    var features by remember { mutableStateOf(SourceFeatures()) }
    val canStart = canStartSourceWizard(websiteUrl, features)
    val muted = tokens.dotOn.copy(alpha = 0.72f)
    val fieldShape = RoundedCornerShape(16.dp)

    Column(Modifier.fillMaxSize().tsuzukiBackground(tokens).safeDrawingPadding()) {
        TsuzukiToolHeader(
            title = stringResource(MR.strings.tsz_source_title),
            subtitle = stringResource(MR.strings.tsz_source_subtitle),
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TsuzukiTile(Modifier.fillMaxWidth(), padding = 16.dp) {
                Text(
                    text = tsuzukiCase(stringResource(MR.strings.tsz_source_address)).orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = muted,
                )
                Row(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(fieldShape)
                        .background(tokens.background)
                        .then(tokens.tileBorder?.let { Modifier.border(1.dp, it, fieldShape) } ?: Modifier)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Outlined.Link, contentDescription = null, tint = muted, modifier = Modifier.size(20.dp))
                    BasicTextField(
                        value = websiteUrl,
                        onValueChange = { websiteUrl = it },
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = tokens.dotOn),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        cursorBrush = SolidColor(tokens.accent),
                        decorationBox = { field ->
                            Box {
                                if (websiteUrl.isEmpty()) {
                                    Text(
                                        text = "https://example.com",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = muted,
                                    )
                                }
                                field()
                            }
                        },
                    )
                }
                Text(
                    text = stringResource(TDMR.strings.selector_setup_dynamic_notice),
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                )
            }

            Column {
                TsuzukiSectionLabel(
                    text = stringResource(TDMR.strings.selector_features_title),
                    contentPadding = PaddingValues(start = 4.dp, top = 6.dp, bottom = 2.dp),
                )
                Text(
                    text = stringResource(TDMR.strings.selector_features_desc),
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    visibleFeatureChips(features).forEach { chip ->
                        TsuzukiPill(
                            text = stringResource(chip.label()),
                            selected = chip.isOn(features),
                            onClick = { features = chip.toggled(features) },
                            role = Role.Checkbox,
                        )
                    }
                }
                if (!features.hasPopular && !features.hasLatest) {
                    Text(
                        text = stringResource(TDMR.strings.selector_features_need_listing),
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.accent,
                    )
                }
            }

            Column {
                TsuzukiSectionLabel(
                    text = stringResource(MR.strings.tsz_source_wizard),
                    contentPadding = PaddingValues(start = 4.dp, top = 6.dp, bottom = 6.dp),
                )
                TsuzukiTile(Modifier.fillMaxWidth(), padding = 16.dp) {
                    SelectorWizardStep.activeSteps(features).forEachIndexed { index, step ->
                        WizardStepRow(number = index + 1, step = step, first = index == 0)
                    }
                }
            }

            Button(
                onClick = { onStart(websiteUrl, features) },
                enabled = canStart,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = tokens.accent, contentColor = tokens.onAccent),
            ) {
                Text(
                    text = tsuzukiCase(stringResource(TDMR.strings.custom_source_start_wizard)).orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Box(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun WizardStepRow(number: Int, step: SelectorWizardStep, first: Boolean) {
    val tokens = TsuzukiTheme.tokens
    val current = number == 1
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = if (first) 0.dp else 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(2.dp, if (current) tokens.accent else tokens.track, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.labelLarge,
                color = if (current) tokens.accent else tokens.dotOn.copy(alpha = 0.72f),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = tsuzukiCase(stringResource(step.titleRes)).orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = tokens.dotOn,
            )
            Text(
                text = stringResource(step.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = tokens.dotOn.copy(alpha = 0.72f),
                maxLines = 2,
            )
        }
    }
}
