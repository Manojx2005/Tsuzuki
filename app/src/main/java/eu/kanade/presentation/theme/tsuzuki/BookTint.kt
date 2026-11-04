package eu.kanade.presentation.theme.tsuzuki

import android.app.Activity
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * The Colour theme's book tint, handed from Home (which already extracts the last-read book's
 * palette) to the novel reader, so reading that book happens on its own colour (board "Reader").
 * In-memory only: if Home hasn't computed a tint for the open book, the reader keeps its theme.
 */
object BookTint {

    /** Warm off-white that reads well on every lifted palette background. */
    const val INK: Int = 0xFFF3EBDD.toInt()

    @Volatile private var mangaId: Long? = null

    @Volatile private var background: Int = 0

    fun set(mangaId: Long, background: Int) {
        this.background = background
        this.mangaId = mangaId
    }

    /** Background and text colours for the reader's "app" theme, or null to keep the default. */
    fun forReader(activity: Activity): Pair<Int, Int>? {
        val id = mangaId ?: return null
        val readerManga = (activity as? ReaderActivity)?.viewModel?.manga ?: return null
        if (readerManga.id != id) return null
        val ui = Injekt.get<UiPreferences>()
        if (ui.appTheme.get() != AppTheme.COLOUR || !ui.tsuzukiFollowBook.get()) return null
        return background to INK
    }
}
