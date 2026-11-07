package eu.kanade.tachiyomi.tsuzuki.reader

import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat

// The stable system insets give the reader one viewport even when the menu shows the system bars.
internal fun WindowInsetsCompat.novelSafeInsets(): Insets =
    getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())

internal fun Insets.withNovelStatusBar(heightPx: Int, atBottom: Boolean): Insets =
    if (atBottom) {
        Insets.of(left, top, right, bottom + heightPx)
    } else {
        Insets.of(left, top + heightPx, right, bottom)
    }
