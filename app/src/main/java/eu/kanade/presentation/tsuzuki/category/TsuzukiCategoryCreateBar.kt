package eu.kanade.presentation.tsuzuki.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTheme
import eu.kanade.presentation.theme.tsuzuki.tsuzukiBackground
import eu.kanade.presentation.theme.tsuzuki.tsuzukiClickable
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun tsuzukiCategoryBackground(): Modifier {
    val tokens = TsuzukiTheme.tokens
    return if (tokens.style == TsuzukiStyle.NONE) Modifier else Modifier.tsuzukiBackground(tokens)
}

@Composable
fun tsuzukiCategoryContainerColor(): Color =
    if (TsuzukiTheme.tokens.style == TsuzukiStyle.NONE) MaterialTheme.colorScheme.background else Color.Transparent

/** Categories board: a wide bottom action that opens the existing category creation dialog. */
@Composable
fun TsuzukiCategoryCreateBar(onCreate: () -> Unit) {
    val tokens = TsuzukiTheme.tokens
    if (tokens.style == TsuzukiStyle.NONE) return
    val label = stringResource(MR.strings.tsz_category_new_shelf)
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(tokens.accent)
                .tsuzukiClickable(role = Role.Button, onClick = onCreate),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = tokens.onAccent)
            Text(
                text = if (tokens.isDot) label.uppercase() else label,
                color = tokens.onAccent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}
