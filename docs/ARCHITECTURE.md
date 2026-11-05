# Architecture map: novel-only, manga-only and shared code

Surveyed 2026-11-05 at `c26dfc13a` (read-only Sonnet research, written up by Claude). Use it to find where a bug
lives and which upstream a file follows. Update it when files move.

**Origin:** M = Mihon, T = Tsundoku, Z = Tsuzuki. (A file in `mihon/main` → M; else in `tsundoku/main` → T;
else Z. Mihon has renamed some old Tachiyomi files, so a few "T" files are really Mihon lineage with Tsundoku
edits: `mangas.sq`, `chapters.sq`, `categories.sq`, `sources.sq`, the old `ui/download/Download*` adapters,
`core/viewmodel`.)
**Class:** NOVEL = novel-only · MANGA = manga-only · SHARED-B = shared, branches on novel vs manga inside ·
SHARED-A = shared, media-agnostic.
**Path short forms:** `A/` = `app/src/main/java/eu/kanade/tachiyomi/`, `P/` = `app/src/main/java/eu/kanade/presentation/`,
`D/` = `app/src/main/java/eu/kanade/domain/`.

## How novel vs manga is decided

- Master switches: `Source.isNovelSource()` (`source-api/.../source/NovelSource.kt`) and `Manga.isNovel`
  (DB column `mangas.is_novel`). Novels and manga share the `mangas` / `chapters` tables.
- Reader: `ReaderViewModel.kt` ~L1306 returns `ReadingMode.NOVEL` for novel sources; `ReadingMode.toViewer`
  (`A/ui/reader/setting/ReadingMode.kt` ~L89) builds `NovelWebViewViewer` or `NovelViewer` from the
  `novelRenderingMode` preference; every other mode builds the Mihon pager or webtoon viewer.
- Tsuzuki UI: upstream composables dispatch to `P/tsuzuki/...` when `TsuzukiTheme.tokens.style != NONE`, often by
  fully-qualified name, so search by symbol name as well as imports.

## 1. Reader

| Path | Class | Origin | Note |
|---|---|---|---|
| A/ui/reader/ReaderActivity.kt | SHARED-B | M | Hosts both viewer families; Tsundoku's novel branches listed below. Tsuzuki adds ~44 lines (transition, contents drawer). |
| A/ui/reader/ReaderViewModel.kt | SHARED-B | M | Novel progress, last_read, completion. Tsuzuki adds `ReadingDays` recording (~L1069). |
| A/ui/reader/setting/ReaderPreferences.kt | SHARED-B | M | ~200 lines of `novel*` prefs (L176–474). |
| A/ui/reader/setting/ReadingMode.kt | SHARED-B | M | `NOVEL` enum, `toViewer`. |
| A/ui/reader/model/* | SHARED-A | M | `ReaderPage.text` carries novel text. |
| A/ui/reader/viewer/{Viewer,ViewerNavigation,MissingChapters,ReaderProgressIndicator,GestureDetectorWithLongTap}.kt | SHARED-A | M | |
| A/ui/reader/viewer/ViewerConfig.kt | SHARED-A | T | Base of Pager/Webtoon/NovelConfig. |
| A/ui/reader/viewer/{ReaderTransitionView,ReaderPageImageView}.kt, ReaderButton.kt | MANGA | M/T | Transition view used only by pager/webtoon transition holders. |
| A/ui/reader/viewer/navigation/{Disabled,Edge,Kindlish,L,RightAndLeft}Navigation.kt | MANGA | M | |
| A/ui/reader/viewer/navigation/{Bottom,Center}Navigation.kt | NOVEL | T | Used by `NovelConfig`. |
| A/ui/reader/viewer/pager/**, webtoon/** | MANGA | M (configs T) | |
| A/ui/reader/viewer/text/** (NovelConfig, textview/*, webview/*, shared/*) | NOVEL | T | TextView renderer (`NovelViewer`, `NovelTextRenderer`…), WebView renderer (`NovelWebViewViewer`, `…DocumentBuilder`, `…Styler`…), shared pipeline (`ContentPipeline`, `ChapterQueue`, `NovelProgress`, `TtsController`, `RegexReplacementsProcessor`, `HtmlUtils`, `ThemeUtils` → Tsuzuki `BookTint.forReader`). Tests under `app/src/test/.../reader/viewer/text/**`. |
| A/ui/reader/loader/ChapterLoader.kt | SHARED-B | M | L114–151 picks `LocalNovelPageLoader` for novel/JS/local-novel sources. |
| A/ui/reader/loader/HttpPageLoader.kt, DownloadPageLoader.kt | SHARED-B | M | Text vs image fetch. |
| A/ui/reader/loader/LocalNovelPageLoader.kt | NOVEL | T | |
| A/ui/reader/loader/{Archive,Directory,Epub}PageLoader.kt, PageLoader.kt | MANGA | M | |
| A/ui/reader/service/TtsPlaybackService.kt | NOVEL | T | |
| A/ui/reader/quote/** | NOVEL | T | Also used by details (`MangaViewModel`). |
| A/data/translation/** (+ 12 engines), data/domain translation repos | NOVEL | T | |
| P/reader/appbars/NovelReaderAppBars.kt, QuotesSheet, AppbarItemConfig, ReaderBottomBarEditorSheet | NOVEL | T | |
| P/reader/appbars/{ReaderAppBars,ReaderBottomBar,ReaderTopBar}.kt | MANGA | M | |
| P/reader/NovelStatusBar.kt, StatusBarItemConfig.kt, settings/NovelPage.kt | NOVEL | T | |
| P/reader/settings/ReaderSettingsDialog.kt | SHARED-B | M | `isNovelMode` → `TsuzukiReaderSettingsSheet` or Tsundoku's novel dialog. |
| P/reader/settings/ReadingModePage.kt | SHARED-B | M | |
| P/reader/components/ChapterNavigator.kt | SHARED-A | M | Uses the Tsuzuki slider. |
| P/reader/{ChapterTransition,ReaderPageActionsDialog,ReaderPageIndicator,ReadingModeSelectDialog,…}.kt | MANGA | M | |
| P/more/settings/screen/{SettingsNovelReaderScreen,StatusBarElementsScreen,FontManagerScreen}.kt | NOVEL | T | |

**Tsundoku's novel branches in ReaderActivity** (grep `NovelViewer|NovelWebViewViewer|isNovel`): status bar reserve
and page numbers L432–566; flush progress L689; finish L734; scroll/TTS L856–926; volume keys/auto-scroll
L1002–1100; bottom bar L1114–1230; edit mode L1245–1310 (Tsuzuki contents drawer at L1265); TTS controls
L1504–1578; `updateViewer` keeps the novel viewer for TTS L1595–1640; chapter navigate L1774–1827;
translation reload L1950; text selection/quotes L2044–2064; menu guards L2186–2318. Tsuzuki transition calls at
L246 and L765.

**…in ReaderViewModel:** saved scroll L198; restore position L322; `isNovelViewer` return L575; source checks
L623/L1223; `saveNovelProgress` L787–851 (95% completion); local novel return L873; completion rule L973; novel
`last_read` + Tsuzuki `ReadingDays.add` L1069–1075; `ReadingMode.NOVEL` L1306; novel compression L1763.

## 2. Sources and extensions

| Path | Class | Origin |
|---|---|---|
| source-api/.../source/{Source,CatalogueSource,online/HttpSource,…}.kt | SHARED-B | M |
| source-api/.../source/NovelSource.kt | NOVEL | T |
| source-local/.../LocalSource.kt, LocalCoverManager, io/* | MANGA | M |
| source-local/.../{LocalNovelSource,EpubChapterListBuilder,MultiVolumeChapterMerger,NovelAssetRewriter}.kt, LocalNovelCoverManager, LocalNovelSourceFileSystem | NOVEL | T |
| A/source/AndroidSourceManager.kt, SourceExtensions.kt | SHARED-B | M |
| A/source/custom/**, A/ui/customsource/** (custom CSS-selector sources) | NOVEL | T |
| A/jsplugin/** (LNReader plugin host), core/common/.../quickjs/QuickJs.kt | NOVEL | T |
| core/common/.../network/interceptor/*RateLimit* | SHARED-A | T |
| A/extension/ExtensionManager.kt, installer/*, util/ExtensionInstaller.kt | SHARED-A | M |
| A/extension/util/ExtensionLoader.kt, ExtensionDiscovery.kt | SHARED-B | M/T |
| D/source/interactor/{GetEnabledNovelSources,GetNovelSourcesWithFavoriteCount}.kt | NOVEL | T |
| D/source/interactor/{GetEnabledSources,GetSourcesWithFavoriteCount}.kt, D/extension/interactor/GetExtensionLanguages.kt | SHARED-B | M |
| A/ui/browse/extension/{ExtensionsTab,ExtensionsViewModel}.kt | MANGA | M |
| A/ui/browse/extension/NovelExtension*.kt, A/ui/browse/jsplugin/**, A/ui/browse/source/NovelSources*.kt, source/custom/** | NOVEL | T |
| A/ui/browse/BrowseTab.kt, source/SourcesTab.kt | SHARED-B | M |

`SourcePreferences.enabledLanguages` filters both manga extensions and novel plugins.

## 3. Downloads

| Path | Class | Origin | Note |
|---|---|---|---|
| A/data/download/Downloader.kt | SHARED-B | M | Novel branches L277–313, L418–453, L593–692 (text + `.zip` vs images + `.cbz`), L870–913. |
| A/data/download/NovelDownloadEngine.kt | NOVEL | Z | Paced text downloads, per-novel pause. |
| A/data/download/ChapterContentReader.kt | NOVEL | T | |
| A/data/download/{DownloadManager}.kt | SHARED-B | M | |
| A/data/download/{DownloadCache,DownloadProvider,DownloadStore,DownloadNotifier,DownloadPendingDeleter}.kt | SHARED-A | M | |
| A/ui/download/DownloadQueueScreen.kt, DownloadQueueViewModel.kt | SHARED-B | M | Novels / Manga / Translations tabs. |
| A/ui/download/Download{Adapter,Holder,HeaderHolder,…}.kt | SHARED-A | T | View-based manga queue (styled by `TsuzukiViewStyle`). |
| A/ui/download/TranslationQueueContent.kt, D/download/service/NovelDownloadPreferences.kt, P/more/settings/screen/SettingsNovelDownloadScreen.kt | NOVEL | T | |

## 4. Library, details, updates, history, browse, migration

| Path | Class | Origin | Note |
|---|---|---|---|
| A/ui/library/LibraryViewModel.kt | SHARED-B | M | `LibraryType {Manga, Novel, All}`. Most `isNovel` checks in the app (22). |
| A/ui/library/LibraryTab.kt | MANGA | M | |
| A/ui/library/NovelsTab.kt | SHARED-B | T | Novel or "all" (joined) library; Tsuzuki's default Library. |
| A/ui/library/ImportEpubScreen.kt, duplicate/**, P/library/components/{ImportEpubDialog,BatchExportEpubDialog,MassImportDialog}.kt, A/data/massimport/** | NOVEL | T | |
| A/data/library/LibraryUpdateJob.kt; D/manga/interactor/GetLibraryManga.kt | SHARED-B | T/M | |
| A/ui/manga/MangaViewModel.kt, MangaScreen.kt (ui + presentation) | SHARED-B | M | Novel-only actions: EPUB export, translate, word count, quotes, TTS. |
| A/ui/manga/track/TrackInfoDialog.kt | SHARED-B | M | Novel vs manga trackers. |
| A/ui/manga/search/**, P/manga/components/{ExportEpubDialog,WordCountDialog,TranslateMangaDetailsDialog}.kt | NOVEL | T | |
| P/manga/components/{MangaInfoHeader,MangaToolbar,MangaChapterListItem,…}.kt | SHARED-B | M | |
| A/ui/updates/UpdatesViewModel.kt, P/updates/** | SHARED-B | M | `UpdatesNovelGroup` groups novel updates. |
| A/ui/history/HistoryViewModel.kt, P/history/** | SHARED-B | M | Novel progress is a percent in `lastPageRead`. |
| A/ui/browse/source/browse/BrowseSource{Screen,ViewModel}.kt, globalsearch/{GlobalSearch,Search}ViewModel.kt | SHARED-B | M | |
| A/ui/browse/source/globalsearch/NovelGlobalSearch*.kt, migration/sources/NovelMigrateSource*.kt | NOVEL | T | |
| mihon/feature/migration/** | MANGA (SmartSourceSearchEngine SHARED-B) | M | |

## 5. Backup and trackers

| Path | Class | Origin |
|---|---|---|
| A/data/backup/{BackupCreator,BackupRestorer,MangaRestorer,models/*,…} | SHARED-B (`isNovel` serialized) | M |
| A/data/backup/BackupProto{Migration,Reader}.kt | SHARED-B | T |
| A/data/backup/restore/{LNReaderBackupImporter,LNReaderImportJob}.kt, A/data/storage/*PortableMigrator.kt | NOVEL | T |
| A/data/track/{TrackerManager,BaseTracker,…} | SHARED-B (`novelTrackers`, sync IDs 100/101) | M |
| A/data/track/{anilist,myanimelist,mangaupdates,kitsu,…} | SHARED-A | M |
| A/data/track/{novellist,novelupdates,ranobedb}/**, A/ui/webview/TrackerWebViewLoginActivity.kt | NOVEL | T |

## 6. Database (`data/src/main/sqldelight/tachiyomi/`)

- `mangas.sq` (SHARED-B): `is_novel` column + index; queries filter by `:isNovel` / tri-state `:filterNovel`.
- `sources.sq`: `is_novel`, `is_js` (stub sources). `extension_store.sq`: `is_novel`. `categories.sq`:
  `content_type` (0 all, 1 manga, 2 novel). `history.sq`: returns `isNovel`; accumulates duration without moving
  `last_read`.
- Novel-motivated migrations: 10, 11, 16, 21, 22, 27, 33, 34. Anime will need **new tables**, never columns on
  `mangas` (WORKPLAN §6.2).

## 7. Tsuzuki-owned code (origin Z)

**Media-specific (only these five):**

| Path | Class | Called from |
|---|---|---|
| P/tsuzuki/reader/TsuzukiReaderSettingsSheet.kt | NOVEL | P/reader/settings/ReaderSettingsDialog.kt (FQN) |
| P/tsuzuki/reader/TsuzukiReaderContents.kt | NOVEL | A/ui/reader/ReaderActivity.kt ~L1265 (FQN); test `TsuzukiTocFilterTest` |
| P/tsuzuki/updates/TsuzukiUpdatesGroupTile.kt | NOVEL | P/updates/UpdatesUiItem.kt |
| P/tsuzuki/settings/TsuzukiSourcePriority.kt | NOVEL | A/ui/library/duplicate/SourcePriorityScreen.kt; test `SourcePriorityStepTest` |
| P/tsuzuki/reader/ReaderDownloadStatus.kt | MANGA | A/ui/reader/viewer/ReaderTransitionView.kt; test `ReaderDownloadStatusTest` |

**Everything else is shared** — do not move it into novel/manga folders: `P/theme/tsuzuki/*` (tokens, pills,
menus, motion, sheets, shared cover, theme reveal, reader transition, palette/tint), `P/tsuzuki/{browse,category,
details,history,home,library,more,settings,startup}/*`, `P/tsuzuki/reader/{TsuzukiReaderSlider,TsuzukiReaderTitle}`,
`P/tsuzuki/updates/TsuzukiUpdatesTiles.kt`, `A/tsuzuki/*` (DefaultSourceRepos, ReadingDays, ReadingChart, Links,
ViewStyle, widget), `A/ui/tsuzuki/*` (home tab/view model, library media switch, stats, updates summary), and
`A/data/download/NovelDownloadEngine.kt` (novel, but lives with the downloader). Several shared files branch on
`isNovel` for labels/badges (mixed library grid/header, history item, extension store item, category items).

## 8. `isNovel` footprint

83 of 842 `.kt` files under `app/src/main/java` (358 occurrences): `eu.kanade.tachiyomi` 61 (ui 34 — browse 16,
reader 5, library 4, manga 3 …; data 20), `eu.kanade.presentation` 14, `eu.kanade.domain` 7, `mihon` 1. Other
modules: domain 16, data 14, source-api 3, source-local 1. Hottest: LibraryViewModel 22, ReaderActivity 18,
CustomSourcesScreen 17, Downloader 15, ReaderViewModel 12, UpdatesViewModel 10, CustomNovelSource 10,
ExtensionDiscovery 10.

## 9. Where to look first

**Novel bug:** `viewer/text/webview/*` · `viewer/text/textview/*` · `viewer/text/shared/*` (progress, queue,
TTS, regex) · `loader/ChapterLoader.kt` L114–151 + `LocalNovelPageLoader` + `HttpPageLoader` text path ·
`NovelDownloadEngine` + Downloader novel branches + `ChapterContentReader` · `jsplugin/**` + QuickJs ·
`ExtensionLoader`/`ExtensionDiscovery`/`AndroidSourceManager` (misclassified `is_novel`/`is_js`) ·
`source/custom/**` · translation / quotes / `TtsPlaybackService` · LNReader importer, EPUB import/export,
`LocalNovelSource`. Cross-cutting: `ReaderViewModel.saveNovelProgress`, ReaderActivity TTS/`updateViewer`,
`TsuzukiReaderSettingsSheet`, `TsuzukiReaderContents`.

**Manga bug:** `viewer/pager/**` · `viewer/webtoon/**` · `ReaderPageImageView`/`ReaderTransitionView`/tap
navigation · archive/directory/EPUB-image loaders + `HttpPageLoader` image path · manga app bars,
`ChapterNavigator`, page actions dialog · Downloader image path (`.cbz`), `DownloadCache`/`DownloadProvider` ·
`LibraryTab` + `LibraryType.Manga` · `ExtensionsTab`/`ExtensionsViewModel`, `mihon/feature/migration/**` ·
`LocalSource` · Mihon trackers, `ReaderDownloadStatus`.
