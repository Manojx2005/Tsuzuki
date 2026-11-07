package eu.kanade.presentation.tsuzuki.reader

import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.tsuzuki.download.isChapterOnDisk
import eu.kanade.tachiyomi.tsuzuki.reader.PendingAdjacentNavigation
import eu.kanade.tachiyomi.tsuzuki.reader.adjacentNovelChapter
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.text.textview.NovelViewer
import eu.kanade.tachiyomi.ui.reader.viewer.text.webview.NovelWebViewViewer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tachiyomi.domain.manga.model.Manga

internal class TsuzukiAdjacentNavigation {
    private val pending = PendingAdjacentNavigation()

    fun observe(viewModel: ReaderViewModel, scope: CoroutineScope, replay: (Boolean) -> Unit) {
        viewModel.state
            .map { it.isLoadingAdjacentChapter }
            .distinctUntilChanged()
            .onEach { isLoading -> pending.takeWhenIdle(isLoading)?.let(replay) }
            .launchIn(scope)
    }

    fun navigate(
        viewModel: ReaderViewModel,
        scope: CoroutineScope,
        forward: Boolean,
        beforeLoad: () -> Unit,
        afterLoad: () -> Unit,
    ) {
        scope.launch {
            if (pending.deferIfLoading(viewModel.state.value.isLoadingAdjacentChapter, forward)) return@launch
            beforeLoad()
            val loaded = if (forward) viewModel.loadNextChapter() else viewModel.loadPreviousChapter()
            if (!loaded) return@launch
            val viewer = viewModel.state.value.viewer
            (viewer as? NovelWebViewViewer)?.onChapterNavigate(if (forward) "next" else "prev")
            // Novel Previous positions the target in setChapters, when that renderer receives it.
            if (forward || (viewer !is NovelViewer && viewer !is NovelWebViewViewer)) afterLoad()
        }
    }
}

internal fun ReaderViewModel.State.resolveTsuzukiAdjacentChapter(
    forward: Boolean,
    ordered: () -> List<ReaderChapter>,
    infiniteScrollEnabled: () -> Boolean,
): ReaderChapter? {
    val chapters = viewerChapters ?: return null
    val isNovel = viewer is NovelViewer || viewer is NovelWebViewViewer
    return adjacentNovelChapter(
        active = chapters,
        ordered = ordered(),
        visibleId = novelVisibleChapter?.id,
        forward = forward,
        infiniteScroll = isNovel && infiniteScrollEnabled() && !isLoadingAdjacentChapter,
        paged = (viewer as? NovelWebViewViewer)?.isPagedModeActive() == true,
    )
}

internal suspend fun isTsuzukiChapterOnDisk(
    downloadManager: DownloadManager,
    manga: Manga?,
    readerChapter: ReaderChapter,
): Boolean {
    if (manga == null) return false
    val chapter = readerChapter.chapter
    return downloadManager.isChapterOnDisk(chapter.name, chapter.scanlator, chapter.url, manga.title, manga.source)
}

internal fun markTsuzukiChapterRead(chapter: ReaderChapter, saveProgress: (ReaderPage, Int) -> Unit) {
    chapter.pages?.firstOrNull()?.let { saveProgress(it, 100) }
}
