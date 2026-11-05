package eu.kanade.presentation.tsuzuki.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.reader.settings.RegexReplacement
import eu.kanade.presentation.theme.tsuzuki.TsuzukiPill
import eu.kanade.presentation.theme.tsuzuki.TsuzukiSectionLabel
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTile
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import eu.kanade.presentation.tsuzuki.settings.tsuzukiCase
import eu.kanade.presentation.tsuzuki.settings.tsuzukiDialogColor
import tachiyomi.core.common.preference.Preference
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/**
 * Find and replace rules in the Tsuzuki themes (board 06-reader "ReplaceRules"): one tile per rule showing
 * pattern → replacement as two mono chips, a type line and an enable switch; tapping a tile edits it; New rule
 * is the wide accent action. Reads and writes the same JSON preference as the stock section.
 *
 * Left out (no data): the board's chapter preview and per-rule scope / match counts; rules here apply everywhere.
 */
@Composable
internal fun TsuzukiReplaceRulesSection(rulesPref: Preference<String>) {
    val json by rulesPref.collectAsState()
    val rules = remember(json) { ReplaceRulesLogic.decode(json) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RegexReplacement?>(null) }

    fun save(updated: List<RegexReplacement>) = rulesPref.set(ReplaceRulesLogic.encode(updated))

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TsuzukiSectionLabel(
            text = stringResource(TDMR.strings.novel_regex_find_replace),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 4.dp, top = 4.dp, bottom = 0.dp),
        )
        rules.forEach { rule ->
            RuleTile(
                rule = rule,
                onToggle = { save(ReplaceRulesLogic.toggle(rules, rule.id)) },
                onEdit = { editing = rule },
            )
        }
        if (rules.isEmpty()) {
            Text(
                text = stringResource(TDMR.strings.novel_no_rules),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        NewRuleButton(onClick = { adding = true })
    }

    if (adding || editing != null) {
        val current = editing
        TsuzukiReplaceRuleDialog(
            initialRule = current,
            onDismiss = {
                adding = false
                editing = null
            },
            onConfirm = { rule ->
                save(ReplaceRulesLogic.upsert(rules, rule, current?.id))
                adding = false
                editing = null
            },
            onDelete = current?.let { target ->
                {
                    save(ReplaceRulesLogic.delete(rules, target.id))
                    adding = false
                    editing = null
                }
            },
        )
    }
}

@Composable
private fun RuleTile(rule: RegexReplacement, onToggle: () -> Unit, onEdit: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    val dim = if (rule.enabled) 1f else 0.5f
    TsuzukiTile(
        modifier = Modifier.fillMaxWidth().tsuzukiClickable(role = Role.Button, onClick = onEdit),
        padding = 14.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Chip(
                        text = if (rule.isRegex) "/${rule.pattern}/" else rule.pattern,
                        modifier = Modifier.weight(1f, fill = false),
                        alpha = dim,
                    )
                    Text("→", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f * dim))
                    Chip(
                        text = rule.replacement.ifEmpty { stringResource(TDMR.strings.novel_rule_remove) },
                        modifier = Modifier.weight(1f, fill = false),
                        alpha = dim,
                    )
                }
                Text(
                    text = tsuzukiCase(ruleMeta(rule)).orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * dim),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = rule.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedTrackColor = tokens.accent, checkedThumbColor = tokens.onAccent),
            )
        }
    }
}

@Composable
private fun ruleMeta(rule: RegexReplacement): String {
    val parts = buildList {
        add(rule.title)
        add(stringResource(if (rule.isRegex) TDMR.strings.novel_tag_regex else TDMR.strings.novel_tag_text))
        if (!rule.isRegex) {
            if (rule.matchWholeWord) add(stringResource(TDMR.strings.novel_match_whole_word))
            if (rule.caseSensitive) add(stringResource(TDMR.strings.novel_case_sensitive_matching))
        }
    }
    return parts.joinToString(" · ")
}

@Composable
private fun Chip(text: String, modifier: Modifier = Modifier, alpha: Float = 1f) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NewRuleButton(onClick: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    Row(
        Modifier
            .fillMaxWidth()
            .tsuzukiClickable(role = Role.Button, onClick = onClick)
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(tokens.accent),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = tokens.onAccent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(
            text = tsuzukiCase(stringResource(TDMR.strings.novel_add_rule)).orEmpty(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = tokens.onAccent,
        )
    }
}

/**
 * Add / edit sheet for one rule. Same fields and validation as the stock dialog (title, pattern, replacement,
 * regex or text with whole-word / case options). The test line runs the rule over a sample typed here, with the
 * reader's own semantics, and shows the result live. [onDelete] is given when editing an existing rule.
 */
@Composable
internal fun TsuzukiReplaceRuleDialog(
    initialRule: RegexReplacement?,
    onDismiss: () -> Unit,
    onConfirm: (RegexReplacement) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val tokens = TsuzukiTheme.tokens
    var title by remember { mutableStateOf(initialRule?.title ?: "") }
    var pattern by remember { mutableStateOf(initialRule?.pattern ?: "") }
    var replacement by remember { mutableStateOf(initialRule?.replacement ?: "") }
    var isRegex by remember { mutableStateOf(initialRule?.isRegex ?: true) }
    var wholeWord by remember { mutableStateOf(initialRule?.matchWholeWord ?: false) }
    var caseSensitive by remember { mutableStateOf(initialRule?.caseSensitive ?: false) }
    var sample by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val problem = ReplaceRulesLogic.validate(title, pattern, isRegex)
    val testResult = if (sample.isNotEmpty()) {
        ReplaceRulesLogic.test(sample, pattern, replacement, isRegex, wholeWord, caseSensitive)
    } else {
        null
    }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = tokens.accent,
        unfocusedBorderColor = tokens.track,
        focusedLabelColor = tokens.accent,
        cursorColor = tokens.accent,
    )
    val fieldStyle = MaterialTheme.typography.bodyMedium
    val problemText = when (problem) {
        ReplaceRuleProblem.PatternEmpty -> stringResource(TDMR.strings.novel_pattern_empty)
        ReplaceRuleProblem.InvalidRegex -> stringResource(
            TDMR.strings.novel_invalid_regex_format,
            regexMessage(pattern),
        )
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tsuzukiDialogColor() ?: MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(tokens.tileRadius),
        title = {
            Text(
                text = tsuzukiCase(
                    stringResource(
                        if (initialRule !=
                            null
                        ) {
                            TDMR.strings.novel_edit_rule
                        } else {
                            TDMR.strings.novel_add_rule_title
                        },
                    ),
                ).orEmpty(),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(tsuzukiCase(stringResource(TDMR.strings.novel_rule_title)).orEmpty()) },
                    singleLine = true,
                    isError = tried && problem == ReplaceRuleProblem.TitleEmpty,
                    textStyle = fieldStyle,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TsuzukiPill(
                        text = stringResource(TDMR.strings.novel_tag_regex),
                        selected = isRegex,
                        onClick = { isRegex = true },
                        role = Role.RadioButton,
                    )
                    TsuzukiPill(
                        text = stringResource(TDMR.strings.novel_tag_text),
                        selected = !isRegex,
                        onClick = { isRegex = false },
                        role = Role.RadioButton,
                    )
                }
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = {
                        Text(
                            tsuzukiCase(
                                stringResource(
                                    if (isRegex) TDMR.strings.novel_regex_pattern else TDMR.strings.novel_find_text,
                                ),
                            ).orEmpty(),
                        )
                    },
                    maxLines = 3,
                    isError = (pattern.isNotEmpty() && problem == ReplaceRuleProblem.InvalidRegex) ||
                        (tried && problem == ReplaceRuleProblem.PatternEmpty),
                    supportingText = problemText?.takeIf { pattern.isNotEmpty() || tried }?.let { { Text(it) } },
                    textStyle = fieldStyle,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = replacement,
                    onValueChange = { replacement = it },
                    label = { Text(tsuzukiCase(stringResource(TDMR.strings.novel_replace_with)).orEmpty()) },
                    maxLines = 3,
                    textStyle = fieldStyle,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!isRegex) {
                    OptionSwitch(stringResource(TDMR.strings.novel_match_whole_word), wholeWord) { wholeWord = it }
                    OptionSwitch(stringResource(TDMR.strings.novel_case_sensitive_matching), caseSensitive) {
                        caseSensitive = it
                    }
                }
                TsuzukiSectionLabel(
                    text = stringResource(TDMR.strings.novel_test),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 6.dp),
                )
                OutlinedTextField(
                    value = sample,
                    onValueChange = { sample = it },
                    label = { Text(tsuzukiCase(stringResource(TDMR.strings.novel_sample_input)).orEmpty()) },
                    minLines = 2,
                    maxLines = 4,
                    textStyle = fieldStyle,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                testResult?.let { result ->
                    result.fold(
                        onSuccess = {
                            Text(
                                text = stringResource(TDMR.strings.novel_output_format, it),
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.accent,
                            )
                        },
                        onFailure = {
                            Text(
                                text = stringResource(
                                    TDMR.strings.novel_error_format,
                                    it.message ?: stringResource(TDMR.strings.novel_invalid_regex),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    tried = true
                    if (problem == null) {
                        onConfirm(
                            ReplaceRulesLogic.build(
                                initialRule,
                                title,
                                pattern,
                                replacement,
                                isRegex,
                                wholeWord,
                                caseSensitive,
                            ),
                        )
                    }
                },
            ) {
                Text(
                    tsuzukiCase(stringResource(MR.strings.action_save)).orEmpty(),
                    color = tokens.accent,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(
                            tsuzukiCase(stringResource(MR.strings.action_delete)).orEmpty(),
                            color = tokens.accent,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        tsuzukiCase(stringResource(MR.strings.action_cancel)).orEmpty(),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
    )

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = tsuzukiDialogColor() ?: MaterialTheme.colorScheme.surfaceContainer,
            title = { Text(tsuzukiCase(stringResource(MR.strings.action_delete)).orEmpty()) },
            text = { Text(stringResource(TDMR.strings.novel_delete_rule_confirm)) },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text(
                        tsuzukiCase(stringResource(MR.strings.action_delete)).orEmpty(),
                        color = tokens.accent,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(tsuzukiCase(stringResource(MR.strings.action_cancel)).orEmpty())
                }
            },
        )
    }
}

private fun regexMessage(pattern: String): String =
    runCatching { Regex(pattern) }.exceptionOrNull()?.message?.lineSequence()?.firstOrNull().orEmpty()

@Composable
private fun OptionSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val tokens = TsuzukiTheme.tokens
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = tsuzukiCase(label).orEmpty(),
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = tokens.accent, checkedThumbColor = tokens.onAccent),
        )
    }
}
