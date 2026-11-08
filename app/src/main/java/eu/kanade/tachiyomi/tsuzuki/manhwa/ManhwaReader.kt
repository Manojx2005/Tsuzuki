package eu.kanade.tachiyomi.tsuzuki.manhwa

import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import okio.BufferedSource
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal fun manhwaDefaultReadingMode(manga: Manga?, language: String?, default: Int): Int {
    val isManhwa = manga != null && Injekt.get<ManhwaStore>().isManhwa(manga, language)
    return if (isManhwa) ReadingMode.WEBTOON.flagValue else default
}

/** Called on the reader's IO path with the original page already in memory. */
internal fun recordManhwaPage(page: ReaderPage, source: BufferedSource) {
    if (page is InsertPage) return
    val chapter = page.chapter
    val mangaId = chapter.chapter.manga_id ?: return
    val total = chapter.pages?.size ?: return
    val store = Injekt.get<ManhwaStore>()
    if (!store.needsPageEvidence(mangaId)) return
    val (width, height) = ImageUtil.getImageDimensions(source)
    val verdict = chapter.manhwaEvidence.record(page.index, total, width, height) ?: return
    store.recordPageEvidence(mangaId, verdict)
}
