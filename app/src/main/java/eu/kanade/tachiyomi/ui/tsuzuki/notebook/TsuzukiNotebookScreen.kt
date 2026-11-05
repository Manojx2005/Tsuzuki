package eu.kanade.tachiyomi.ui.tsuzuki.notebook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.tsuzuki.details.TsuzukiNotebook
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.notes.MangaNotesScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.quote.Quote
import eu.kanade.tachiyomi.ui.reader.quote.QuoteManager
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Per-book Notebook: the book's note plus the quotes saved for it. Reached from the book details overflow menu
 * in the Tsuzuki themes.
 */
class TsuzukiNotebookScreen(private val mangaId: Long) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val getManga = remember { Injekt.get<GetManga>() }
        val manga by produceState<Manga?>(null, mangaId) {
            getManga.subscribe(mangaId).collect { value = it }
        }
        val chapters by produceState(emptyList<Chapter>(), mangaId) {
            value = withIOContext { Injekt.get<GetChaptersByMangaId>().await(mangaId) }
        }
        val quoteManager = remember { QuoteManager(context.applicationContext) }
        val sourceManager = remember { Injekt.get<SourceManager>() }
        var reload by remember { mutableIntStateOf(0) }
        val quotes by produceState(emptyList<Quote>(), manga?.id, reload) {
            val current = manga ?: return@produceState
            value = withIOContext {
                quoteManager.getQuotes(sourceManager.getOrStub(current.source).toString(), current.title)
            }
        }
        val current = manga ?: return
        TsuzukiNotebook(
            title = current.title,
            notes = current.notes,
            quotes = quotes,
            chapters = chapters,
            navigateUp = navigator::pop,
            onEditNote = { navigator.push(MangaNotesScreen(manga = current)) },
            onOpenChapter = { chapterId ->
                context.startActivity(ReaderActivity.newIntent(context, current.id, chapterId))
            },
            onDeleteQuote = { quote ->
                scope.launch {
                    withIOContext {
                        quoteManager.removeQuote(
                            sourceManager.getOrStub(current.source).toString(),
                            current.title,
                            quote.id,
                        )
                    }
                    reload++
                }
            },
        )
    }
}
