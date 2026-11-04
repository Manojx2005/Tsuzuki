package eu.kanade.tachiyomi.tsuzuki

import android.content.res.ColorStateList
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.theme.tsuzuki.TsuzukiStyle
import eu.kanade.presentation.theme.tsuzuki.TsuzukiTokens
import eu.kanade.presentation.theme.tsuzuki.tsuzukiTokensFor
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Theme for the View-based manga download queue (board 07-system "Downloads"), which can't read Compose tokens:
 * cards become the theme's tile (radius, colour, Dot hairline border), titles are uppercase in Dot and progress bars
 * take the accent. Does nothing in stock themes. The Compose rewrite of that list is still open (AGENTS order 14.4).
 */
object TsuzukiViewStyle {

    private fun tokens(): TsuzukiTokens? {
        val theme = runCatching { Injekt.get<UiPreferences>().appTheme.get() }.getOrNull() ?: return null
        return tsuzukiTokensFor(theme).takeIf { it.style != TsuzukiStyle.NONE }
    }

    /** A download row: tile card with a little space around it. */
    fun downloadItem(card: MaterialCardView, title: TextView, progress: LinearProgressIndicator) {
        val t = tokens() ?: return
        val density = card.resources.displayMetrics.density
        card.setCardBackgroundColor(t.tile.toArgb())
        card.radius = 18f * density
        t.tileBorder?.let {
            card.strokeWidth = (1 * density).toInt()
            card.strokeColor = it.toArgb()
        }
        (card.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
            val h = (12 * density).toInt()
            val v = (3 * density).toInt()
            it.setMargins(h, v, h, v)
            card.layoutParams = it
        }
        if (t.isDot) title.isAllCaps = true
        progress.setIndicatorColor(t.accent.toArgb())
        progress.trackColor = t.tileBorder?.toArgb() ?: 0x33FFFFFF
    }

    /** A source header in the download queue: Dot uppercase, transparent card. */
    fun downloadHeader(card: MaterialCardView, title: TextView) {
        val t = tokens() ?: return
        card.setCardBackgroundColor(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT))
        if (t.isDot) title.isAllCaps = true
    }
}
