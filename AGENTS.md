# AGENTS.md — working on Tsuzuki

Tsuzuki is an Android reader for light novels, web novels, manga and manhwa (Kotlin, Jetpack Compose).
It is a fork of Tsundoku (itself a Mihon fork). Read these before changing anything:

- `docs/PLAN.md`: product and engineering plan (the theme system is in §3).
- `docs/design/README.md`: design tokens, nav/rail specs, board index, and **implementation status**.
- `docs/design/specs.md`: the remaining boards written out.
- `docs/design/boards/INDEX.md`: all 16 canvas pages saved offline, with PNGs and per-board HTML.
- `docs/GETTING_STARTED.md`: first-time setup.
- `docs/UPSTREAM.md`: taking Mihon / Tsundoku changes (`scripts/upstream-report.sh`), pick log.

## When the owner says "continue"

1. Read `docs/HANDOFF.md` (the single shared handoff: current state, Codex queue, owner decisions, log), then
   `docs/WORKPLAN.md` (the plan and how Claude and Codex split work) and `docs/ARCHITECTURE.md` (where novel,
   manga and shared code live). Codex: take the first `ready` card in the Codex queue and follow the protocol at
   the top of `HANDOFF.md`. Claude: review `review` cards first, then continue the plan.
2. Find and look at its design board **before making UI changes**. Use the offline canvas snapshot first:
   - **Batch Q (2026-11-06) boards win over older boards where they overlap:** `docs/design/boards/fixes-2026-11/`
     (`README.md` maps each board to its card; PNGs in `png/`, Dot and Colour; bug screenshots in `evidence/`).
   - **Motion system (Batch SM, 2026-11-28): `docs/design/boards/motion-2026-11/README.md`** — every duration, curve and
     transition value; owner-approved 2026-11-28, wins over older motion boards (HANDOFF "Batch SM", SM4).
   - **Remaining cards (V8.2 empty states, A6.6, A6.7, A6.8, AY8): `docs/design/boards/remaining-2026-11/README.md`** — owner-approved 2026-11-28.
   - **`docs/design/boards/INDEX.md`** and `gallery.html`: 139 artboards and 99 individual Dot screens, captured from the owner's Vivaldi session on 2026-11-03. PNGs are in `png/<page>/`; individual Dot screens are in `png/<page>/dot/`. Per-board source is in `html/<page>/`, with original page exports in `exports/`. The index maps exact paths. Archives are reference only.
   - **`docs/design/specs.md`**: the boards for every open task, written out with sizes, colours, states and motion.
     Use these written measurements alongside the relevant screenshot and HTML.
   - `docs/design/boards/*.dc.html` (if present): raw board HTML exported by the owner. The layout is the markup;
     the numbers are in the `renderVals()` script at the bottom.
   - `docs/design/boards/png/*.png`: screenshots of the boards to compare against (see `docs/design/boards/README.md`).
   - `docs/design/README.md`: tokens, nav/rail specs, board index and implementation status.
   If a task's board is in none of these, ask the owner for it instead of inventing a layout.
   Build it for **Dot first**, then check Colour.
3. Build, install on the connected devices, look at a screenshot (see **Test on devices**), and fix what's off.
4. Record the result in `docs/HANDOFF.md` (card Result / log), update the status table in `docs/design/README.md`,
   then report what changed and what you saw on the devices. Codex commits only on its own card branch
   (`tsuzuki/<card>`, owner approved 2026-11-07); Claude reviews and merges into `main` (owner as sole author,
   no push unless asked). **Owner 2026-11-08:** while Claude is out of usage, the Codex *review lane* (HANDOFF
   "Batch T") does Claude's review: it merges passed cards into `main` locally (no push) and uses the tablet and S22.

## Build

Windows machine, Git Bash or PowerShell.

```sh
# JDK 21 is required (the `java` on PATH is JDK 24/25, which this Gradle setup doesn't support).
# Desktop (this repo's usual machine): Android Studio's bundled jbr is 21.0.10.
# Second machine (Android Studio 2026.2 bundles JDK 25 there): Temurin 21.
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
[ -x "$JAVA_HOME/bin/java" ] && "$JAVA_HOME/bin/java" -version 2>&1 | grep -q '"21'   || export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot"
./gradlew :app:compileDebugKotlin     # fast check
./gradlew :app:assembleDebug          # APKs in app/build/outputs/apk/debug/
./gradlew :app:spotlessApply          # format (ktlint) before finishing a task
```

- `spotlessApply` also reformats untouched upstream files and stops on two old upstream lint errors
  (`SettingsNovelReaderScreen.kt`, `LNReaderBackupImporter.kt`). Keep its changes only in files you edited;
  restore any other file it touched with git, so Tsundoku merges stay clean.

- `local.properties` must use forward slashes: `sdk.dir=C:/Users/Manoj/AppData/Local/Android/Sdk`.
- The debug app id is `io.github.manojx2005.tsuzuki.dev` (not `.debug`).
- Kotlin files use LF line endings. When editing from Python on Windows, open files with `newline=''`.

## Test on devices

adb: `$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe` (not on PATH).

| Serial | Device | Screen |
|---|---|---|
| `R52Y90HA77L` | Galaxy Tab S11 (SM-X730) | 1600×2560 @ 340 dpi, about 753×1205 dp, so it gets the **rail** and tablet-portrait layout |
| `RZCWA139X1X` | Galaxy S22 (SM-S901E) | 1080×2340, phone layout with the **bottom pill nav** |
| `R5GL23ZYE1V` | Owner's own phone (SM-S948Q) | 1080×2340 @ 450 dpi ≈ 384×832 dp, 120 Hz; runs the **release** app `io.github.manojx2005.tsuzuki` with the owner's real data. Only connected to the second machine; never install debug builds on it without asking |

```sh
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
APK=app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
for d in R52Y90HA77L RZCWA139X1X; do
  "$ADB" -s $d shell svc power stayon usb          # keep the screen on while plugged in (owner's request)
  "$ADB" -s $d install -r $APK
  "$ADB" -s $d shell am force-stop io.github.manojx2005.tsuzuki.dev
  "$ADB" -s $d shell monkey -p io.github.manojx2005.tsuzuki.dev -c android.intent.category.LAUNCHER 1
done
"$ADB" -s R52Y90HA77L exec-out screencap -p > tablet.png   # then look at it
"$ADB" -s R52Y90HA77L logcat -d -b crash                   # check for crashes
```

- The devices are set to Japanese, so system strings and Tsundoku's own strings show in Japanese.
- Native libraries are 16 KB-aligned since quickjs-kt 1.0.15; re-check after dependency bumps (see the 16 KB task in the queue).
- Who may use adb: Claude, and the Codex review lane (HANDOFF "Batch T", owner 2026-11-08) on the tablet and S22
  only. Anime-lane implementer threads (Batch AY and the A6 cards) also test their own cards on those two devices
  (owner 2026-11-12), one thread at a time: claim the "Device lock" line in HANDOFF "Batch AY" first. Other
  implementer threads never use adb. Nobody installs on `R5GL23ZYE1V` without asking.
- The owner may be using a device while you work. If the screen isn't what you expect, don't tap through system pickers or permission dialogs; ask instead.

## Rules

- **Two themes, one layout.** Screens read colours from `TsuzukiTheme.tokens` (`app/.../presentation/theme/tsuzuki/`)
  and fonts from `MaterialTheme.typography` (display = Doto / Instrument Serif, body = Space Mono / Manrope).
  Don't hardcode theme colours in screens. In Dot, uppercase text with `tokens.case()`-style helpers, keep red
  as the only UI accent. Book covers keep their colours by default (owner 2026-11-15); respect the Appearance
  "Colour covers" preference so a saved grayscale choice still applies.
- **Japanese text** uses a partner font per theme face, chained behind the Latin font in `LnrTypography.kt`
  (Android 10+). Use `MaterialTheme.typography` styles so this applies; never set `fontFamily` directly.
  The Japanese fonts in `res/font/lnr_jp_*.ttf` are subsets (kana + JIS X 0208 kanji) made with the `subset-font`
  npm package; their licences are in `docs/licenses/`.
- **App themes (owner 2026-11-06, card Q12).** Colour and Dot are the only selectable app themes. Stored and
  restored legacy app themes resolve to Colour. Keep the legacy `AppTheme` entries, colour schemes and
  `TsuzukiStyle.NONE` for backup compatibility and Tsundoku merges (keep the `tokens.style != NONE` dispatches).
  The shared novel page choices are Book, Paper, Sepia, Sage, Night, Black and Custom; the saved custom text and
  background colours apply in Custom.
- **Real data only.** Never ship the design's sample numbers. If a tile has no backing data yet (streak, listen,
  source health), leave it out and note it in the task queue.
- **Keep Tsundoku mergeable.** Put new code in `tsuzuki` packages and keep edits to upstream files small
  (already touched: `Navigator.kt`, `CommonMangaItem.kt`, `BaseBrowseItem.kt`, `AppThemePreferenceWidget.kt`, `UpdatesUiItem.kt`, `ListPreferenceWidget.kt`, `SettingsMainScreen.kt`, `HistoryScreen.kt`, `MoreTab.kt`, `ReaderSettingsDialog.kt`, `SettingsAppearanceScreen.kt`, `UiPreferences.kt`, `PreferenceModule.kt`, `HomeScreen.kt`, `MainActivity.kt`, `App.kt`, `TachiyomiTheme.kt`, the two `Lnr*ColorScheme.kt`,
  `MangaCover.kt`, `MangaInfoHeader.kt`, `AppBar.kt`, `LibraryBadges.kt`, `HistoryItem.kt`, `UpdatesUiItem.kt`,
  `MoreScreen.kt`, `res/values/themes.xml`, `MangaScreen.kt` (both), `MangaToolbar.kt`, `ChapterSettingsDialog.kt`,
  `MangaViewModel.kt`, `LibraryContent.kt`, `LibraryPager.kt`, `LibraryViewModel.kt`, `NovelsTab.kt`,
  `presentation-core/.../theme/Color.kt` (`LocalActiveColor`), `NovelReaderAppBars.kt`, `ReaderActivity.kt`, `PreferenceScaffold.kt`, `BasePreferenceWidget.kt`, `TextPreferenceWidget.kt`, `AboutScreen.kt`, `CategoryListItem.kt`, `ExtensionsScreen.kt`, `ExtensionStoresContent.kt`, `ExtensionDetailsScreen.kt`, `GlobalSearchResultItems.kt`, `ui/.../BrowseSourceScreen.kt` (re-indented block, see order 14.9), `BrowseBadges.kt`, `Preference.kt`, `PreferenceItem.kt`, `SettingsLibraryScreen.kt`, `presentation/category/CategoryScreen.kt`, `CategoryFloatingActionButton.kt`, `SourcePriorityScreen.kt`, `ui/setting/SettingsScreen.kt`, `components/DropdownMenu.kt`, `LnrTypography.kt`, `AppLanguageScreen.kt`, `SettingsAdvancedScreen.kt`, `SettingsDataScreen.kt`, `res/xml/locales_config.xml`, `LibraryToolbar.kt`, `BrowseSourceToolbar.kt`, `WebViewScreenContent.kt`, `ReaderTopBar.kt`, `MassImportDialog.kt`, `DownloadHolder.kt`, `DownloadHeaderHolder.kt`, `MigrationConfigScreen.kt`, `JsPluginsTab.kt`, `NovelExtensionsTab.kt`, `ExtensionsTab.kt`, `ImportEpubScreen.kt`, `DownloadQueueScreen.kt`, `presentation/more/NewUpdateScreen.kt`, `presentation/manga/MangaNotesScreen.kt`, `presentation/more/onboarding/{OnboardingScreen,StorageStep,PermissionStep}.kt`, `ui/more/OnboardingScreen.kt`). Prefer a small dispatch at the top of an upstream composable that
  calls a `presentation/tsuzuki/...` version, as `HistoryItem` and `MoreScreen` do.
- **Sources.** Shipped builds (release, preview, foss) come with **no** repositories (owner 2026-11-10). Only the
  debug build seeds default repos, once on first launch, in `eu/kanade/tachiyomi/tsuzuki/DefaultSourceRepos.kt`:
  the LNReader plugin index (novels, via the QuickJS plugin host) and Keiyoushi (`repo.json`, which points at their
  v2 protobuf index; the legacy `index.min.json` only lists an "Outdated App" stub).
- **Reuse the Tsuzuki building blocks** before writing new UI: `TsuzukiPill` / `TsuzukiPillRow`,
  `TsuzukiSectionLabel` (`theme/tsuzuki/TsuzukiPill.kt`), `tsuzukiGroupedTile` (`presentation/tsuzuki/browse/`),
  `TsuzukiTile`, `TsuzukiProgressBar`. Dialog tabs (`TabbedDialog`), screen tabs (`TabbedScreen`) and preference
  groups (`PreferenceScreen`) already switch to these in the Tsuzuki themes.
- **Additional upstream file touched after the picks merge:** `data/download/Downloader.kt` (recover originals plus partial split images after process death; `de061e0ef`).
- **Small upstream diffs:** don't wrap and re-indent upstream code. Add a slot parameter or a thin wrapper instead
  (see `LibraryToolbar(below = ...)`).
- **Additional already touched file:** `presentation/reader/components/ChapterNavigator.kt` (exclude the invisible width-reserving label from accessibility; layout unchanged).
- **Additional already touched file:** `ui/reader/viewer/ReaderTransitionView.kt` (next-chapter download lookup passed as a lambda to `presentation/tsuzuki/reader/ReaderDownloadStatus.kt`, which runs it on IO).
- **Storage off the caller thread (N20):** `data/download/ChapterContentReader.kt` (public reads are `suspend`, on IO), `data/epub/EpubExportJob.kt` (`readOriginalImages` is `suspend`), `ui/reader/loader/DownloadPageLoader.kt` (`getPages` / `getPageDataStream` on IO), `ui/reader/loader/ChapterLoader.kt` and `ui/reader/ReaderViewModel.kt` (one-line swap to `DownloadManager.isChapterOnDisk`, `tsuzuki/download/ChapterOnDisk.kt`). The synchronous `DownloadProvider` / `DownloadCache` / `DownloadManager` lookups stay for Tsundoku-shared callers.
- **Additional upstream WebView files:** `core/common/.../network/NetworkHelper.kt`, `network/interceptor/CloudflareInterceptor.kt`, `network/interceptor/WebViewInterceptor.kt`, `util/system/WebViewUtil.kt`, `app/.../di/AppModule.kt`, `ui/webview/WebViewActivity.kt`, `ui/webview/WebViewViewModel.kt` (Mihon robustness group; existing rate limiting and Injekt retained).
- **Additional WebView screen adapter:** `ui/webview/WebViewScreen.kt` (header readiness and a separate ViewModel key for each browser route).
- **Restoring a file:** use `git show HEAD:<file> > <file>`.
- **AP batch 2 upstream touches (2026-11-24):** `presentation/updates/UpdatesScreen.kt`, `presentation/updates/UpdatesUiItem.kt`, `ui/updates/UpdatesTab.kt` (mixed book + anime selection in Updates › All via `presentation/tsuzuki/updates/TsuzukiMixedUpdatesSelection.kt`); `presentation/manga/components/MangaInfoHeader.kt` (`MangaSummary`, `MangaActionButton` made `internal` for the anime header).
- **Disk space (owner 2026-11-24):** once a card branch is merged into `main`, remove its worktree and its Gradle output (`build/`, `.gradle/`, `.kotlin/`); keep only the results (move `*/build/review-evidence` to `.worktrees/_evidence/<worktree>/` first; the branch ref stays). Clean throwaway QA/gate worktrees when done. Never remove a worktree with unsaved changes or one another thread is using.
- **Commits:** Claude only when asked; Codex on its own `tsuzuki/<card>` branch when a card is done (never `main` — except the review
  lane merging reviewed cards, Batch T —
  never another card's branch, never amend or rebase someone else's commit). The owner is the sole author: no
  `Co-Authored-By` or "Generated with" trailers.
- **Licences:** Apache-2.0 app. Don't copy GPL code (NovelDokusha and similar); see PLAN.md §1.

- **Installer compatibility files also touched:** `extension/util/ExtensionInstaller.kt`, `ExtensionLoader.kt`, `extension/ExtensionManager.kt`; whole Mihon installer with documented adapters, novel loader features retained.

- **Download provider compatibility:** `data/download/DownloadProvider.kt` now follows Mihon with retained synchronous APIs and novel ZIP support; async caller migration remains open.

- **Anime integration upstream adapters (RV3-S):** `presentation/browse/components/GlobalSearchToolbar.kt`,
  `presentation/more/settings/screen/debug/DebugInfoScreen.kt`, `data/backup/create/BackupOptions.kt`,
  `data/backup/restore/{BackupRestorer,RestoreOptions}.kt`, and the anime API adapter methods in
  `data/track/{anilist/Anilist,anilist/AnilistApi,kitsu/Kitsu,kitsu/KitsuApi,myanimelist/MyAnimeList,myanimelist/MyAnimeListApi}.kt`.
  Existing shared screen/backup hooks listed above also dispatch anime behind `TsuzukiAnime.enabled`.
  `di/AppModule.kt` resolves download protection lazily when episode refresh runs, avoiding a constructor cycle.

## Progress, plan and history

- Live state, Codex queue, owner decisions and log: **`docs/HANDOFF.md`** (the only handoff; don't create others).
- Plan, roles, model tiers, parallelism rules and the anime plan: **`docs/WORKPLAN.md`**.
- The old task queue and orders 1–14 (with per-order notes) are in git: `git show 8f7f12e3b:AGENTS.md`.

- **Overnight 2026-11-06 additional upstream touches (small hooks):** `SourcesScreen.kt`, `GlobalSearchScreen.kt`, `SearchViewModel.kt`, `ExtensionStoresViewModel.kt`, `ExtensionStoresScreen.kt`, `ExtensionsScreen.kt`, `ExtensionDetailsScreen.kt`, `SettingsBrowseScreen.kt`, `SourcePreferences.kt`, `GetExtensionsByType.kt`, `ExtensionLoader.kt`, `ExtensionManager.kt`, `ExtensionInstallReceiver.kt`, `LoadResult.kt`, domain `Extension.kt`, `NovelStatusBar.kt` (body moved into a themed content fn), `ReaderTopBar.kt` (actions lambda), `ReadingModePage.kt` (import aliases), `StatusBarElementsScreen.kt`, `ScanlatorFilterDialog.kt`, `DownloadQueueScreen.kt`/`ViewModel.kt`, `CategoryViewModel.kt`, `categories.sq`/`mangas.sq` (read-only queries), `MangaRepository(Impl)`, `ChapterRepository(Impl)`, `NotificationReceiver.kt`, `Notifications.kt`, `InfoScreen.kt`, `BasePreferences.kt`, `CrashLogUtil.kt`, `NewUpdateScreen(Model).kt`, `TtsPlaybackService.kt`, `NovelWebViewViewer.kt` (find API), `MangaCover.kt` + cover call sites, `app/build.gradle.kts` (debugImplementation anime-player). See docs/UPSTREAM.md and the HANDOFF log for each.

- **Batch Q upstream touches (merged 2026-11-07):** `NovelReaderAppBars.kt` (Tsuzuki row dispatch), `ReaderActivity.kt` / `ReaderViewModel.kt` (visible-chapter next/prev, pending replay), `NovelViewer.kt`, `NovelWebViewViewer.kt`, `NovelWebViewStyler.kt` (themed chapter-end card + `assets/novel-reader/chapter-end-card.js`), `ThemeUtils.kt`, `ReaderPreferences.kt`, `NovelPage.kt` (Tsundoku theme lists removed, Q12), `SettingsNovelReaderScreen.kt`, `SettingsAppearanceScreen.kt`, `AppThemePreferenceWidget.kt`, `UiPreferences.kt`, `PreferenceRestorer.kt`, `App.kt` (theme + shelf migrations), `LNReaderBackupImporter.kt` (LN Default → system shelf), `LibraryTabs.kt`, `LibraryContent.kt`, `LibraryTab.kt`, `LibraryViewModel.kt`, `NovelsTab.kt`, `MangaScreen.kt`, `MangaInfoHeader.kt` (hourglass tile wrapped in an `if` — small re-indent), `MangaToolbar.kt`, `MangaViewModel.kt`, `BrowseTab.kt`, `SourcesScreen.kt`, `TabbedScreen.kt`, `HomeScreen.kt`, `MoreTab.kt`, `MoreScreen.kt`.

- **v0.1 release upstream touches (2026-11-07):** `App.kt` (one line schedules `tsuzuki/update/TsuzukiAppUpdateJob`), `MainActivity.kt` (update-notification tap opens `NewUpdateScreen`; launch check skips if it is already open), `presentation/browse/MigrateSourceScreen.kt` (dispatch to `tsuzuki/browse/TsuzukiMigrateSourceList.kt`), `domain/.../download/service/NovelDownloadPreferences.kt` (faster pacing defaults: 1 s + 0.5 s jitter, 2 permits, 2 books at once; owner 2026-11-07), `domain/ui/model/ThemeMode.kt` (one-line hook: Dot and Colour always use night mode via `theme/tsuzuki/TsuzukiThemeMode.kt`; the System/Light/Dark switch is removed from Appearance, owner 2026-11-07), `data/.../release/ReleaseServiceImpl.kt` (preview builds read the release list, newest `r…` tag), `domain/.../release/interactor/GetApplicationRelease.kt` (ignore the other channel's tag), `.github/workflows/release.yml` (rewritten for Tsuzuki), `app/google-services.json` (deleted), `CHANGELOG.md` (Tsuzuki's own; Tsundoku's in `docs/CHANGELOG-tsundoku.md`).

- **Batch Q part 2 upstream touches (merged 2026-11-07):** Q9: `AppBar.kt`, `HistoryScreen.kt`, `HistoryItem.kt`, `HistoryTab.kt`, `CommonMangaItem.kt`, `LibraryToolbar.kt`, `MangaScreen.kt` (both), `MangaBottomActionMenu.kt`, `MangaChapterListItem.kt`, `MangaToolbar.kt`, `PreferenceScaffold.kt`, `PreferenceScreen.kt`, `UpdatesScreen.kt`, `UpdatesUiItem.kt`, `UpdatesTab.kt`, `UpdatesViewModel.kt`, `LibraryTab.kt`, `LibraryViewModel.kt`, `NovelsTab.kt`, `MangaViewModel.kt`; Q8: `ChapterHeader.kt`; Q11: `NovelPage.kt`, `ReaderSettingsDialog.kt`, presentation-core `SettingsItems.kt` (opt-in `LocalStackedSettingsLabel`); X1: `ReaderActivity.kt` / `ReaderViewModel.kt` (now one-line calls into `presentation/tsuzuki/reader/`); X3: `Preference.kt`, `PreferenceItem.kt`, `SettingsTranslationScreen.kt`, `EditTextPreferenceWidget.kt`; X4: `MigrationConfigScreen.kt`, `MigrationConfigScreenSheet.kt`; Claude fixes: `TabbedScreen.kt` (custom header → `TsuzukiPagerHeader`), `NovelTextRenderer.kt` (`removeBlankLines()`).

- **Batch T RV1 upstream touches:** `TtsController.kt` (voice label adapter), `GetUpcomingManga.kt` (eligible-book lookup), `NovelImageGetter.kt` (illustration sizing), `NovelTextRenderer.kt` (illustration span reflow), `NovelWebViewStyler.kt` (reader column CSS).

- **RV2 upstream touches (2026-11-13):** `data/backup/BackupFileValidator.kt`, `BackupProtoReader.kt`, `create/BackupCreateJob.kt`, `create/BackupCreator.kt`, `presentation/more/settings/screen/data/RestoreBackupScreen.kt`, and `ui/reader/viewer/text/webview/NovelWebViewDocumentBuilder.kt` (backup validation/counts and paged recap separation; other reader/settings hooks were already listed).
- **AY2 upstream touch (2026-11-21):** `data/backup/create/creators/PreferenceBackupCreator.kt` (one `.plus(...)` hook appending anime source settings from `tsuzuki/anime/backup/AnimeSourcePreferencesBackup.kt`; the restorer already restores `source_<id>` by name).
- **AY6 (2026-11-21):** `app/proguard-rules.pro` keeps `network.JavaScriptEngine` (R8 dropped the constructor extensions call); new `source-api` files `util/CoroutinesExtensions.kt` and `util/JsonExtensions.kt` (extensions-lib helpers). Re-run `scripts/anime-abi/` after dependency bumps (its README).
- **ERR1 upstream touches (2026-11-14):** `presentation/browse/BrowseSourceScreen.kt`, `presentation/browse/MigrateSearchScreen.kt`, `presentation/browse/GlobalSearchScreen.kt`, `ui/browse/source/globalsearch/{SearchViewModel,GlobalSearchScreen,NovelGlobalSearchScreen}.kt`, `ui/browse/migration/search/MigrateSearchScreen.kt`, `ui/reader/viewer/ReaderProgressIndicator.kt`, `viewer/pager/PagerPageHolder.kt`, `viewer/webtoon/WebtoonPageHolder.kt`, `viewer/text/textview/NovelTextViewInlineFeedback.kt`, `viewer/text/webview/NovelWebViewInlineFeedback.kt` (small dispatches to `presentation/tsuzuki/components/TsuzukiErrorState.kt` and `tsuzuki/error/ErrorCopy.kt`; per-source search retry).

- **V8.3 migration source kind (2026-11-19):** `mihon/feature/migration/config/MigrationConfigScreen.kt` (Model takes the book ids, filters targets by kind, merges saved sources), `mihon/feature/migration/list/MigrationListViewModel.kt` and `ui/browse/migration/search/MigrateSearchViewModel.kt` (one filter each); logic in `tsuzuki/migration/MigrationSourceKind.kt`.
- **V8.4 (2026-11-19):** `MigrationConfigScreen.kt` (source row: name + badge in one weighted row so language pills align) and `ui/browse/migration/manga/MigrateMangaScreen.kt` (Tsuzuki dispatch to `TsuzukiMigrateBookRow`).
- **READER-JP-FONT (2026-11-19):** `ui/reader/viewer/text/textview/NovelViewer.kt` (Literata branch calls `presentation/tsuzuki/reader/readingTypeface()` first: Literata + bundled Noto Serif JP, Android 10+).
- **STR3 (2026-11-19):** `SettingsTrackingScreen.kt` — 32 hardcoded English titles/subtitles/info/instructions/toast replaced by `tsz_track_*` resources (literal swaps only).
- **STR4:** `presentation/library/LibrarySettingsDialog.kt` — hardcoded English labels/content descriptions replaced by `tsz_libsettings_*` and existing resources (literal swaps only).
- **STR6 (2026-11-24):** hardcoded English replaced by `tsz_str6_*` / existing string resources (literal swaps only; two private helpers in `ImportEpubScreen.kt` gained `@Composable`): `ui/library/ImportEpubScreen.kt`, `presentation/manga/components/EditMangaDialog.kt`, `presentation/library/components/MassImportDialog.kt`, `SettingsLibraryScreen.kt`, `SettingsAdvancedScreen.kt`, `ui/browse/source/browse/{SourceFilterDialog,FilterPresetsDialog}.kt`, `ui/browse/migration/manga/QuickMigrateDialogs.kt`, `ui/webview/TrackerWebViewLoginActivity.kt`.
- **AY9 preview adapters:** `App.kt` (Coil model fetcher/keyer registration), `di/AppModule.kt` (frame-store factory and cleanup callback). All frame capture, storage, fetch and UI logic lives in Tsuzuki anime packages.
- **AY-INT (2026-11-19, in progress):** `SettingsMainScreen.kt` (anime player row filtered in composition, `isAnimeOnly`), `SettingsSearchScreen.kt` (anime player entries from `presentation/tsuzuki/settings/AnimeSettingsSearch.kt` while anime is on), `ui/download/DownloadQueueScreen.kt` (passes queued anime count to the Tsuzuki header), `presentation/category/CategoryScreen.kt` (top-bar `actions` slot → `tsuzuki/category/TsuzukiAnimeShelvesAction.kt`), `SettingsTrackingScreen.kt` (one spread line → `tsuzuki/settings/AnimeTrackingPreference.kt`). 2026-11-20: `presentation/history/HistoryScreen.kt` (`HistoryUiModel.Anime` + All merge via `presentation/tsuzuki/history/TsuzukiMixedHistory.kt`), `ui/history/HistoryTab.kt` and `presentation/history/components/HistoryDialogs.kt` (clear-all also clears anime; optional `note`), `ui/updates/UpdatesViewModel.kt` (one line: Update library also starts `AnimeLibraryUpdateJob`), data `anime_history.sq` (`clearAllHistory`, `refreshAllAnimeLastSeen`).
- **PERF-START-2-F2 (2026-11-21):** `source/AndroidSourceManager.kt` (combine the after-scan flows; `isInitialized` only with a map built from both completed scans), one property each in `extension/ExtensionManager.kt` and `jsplugin/JsPluginManager.kt` (`…AfterScan`, helper `tsuzuki/update/SourcesAfterScan.kt`). The synchronous WebView user-agent warm-up in `App.kt` stays (part 2 dropped).
- **TTS-SETTINGS upstream adapters (2026-11-15):** `SettingsMainScreen.kt`, `SettingsSearchScreen.kt`, `SettingsNovelReaderScreen.kt` add the dedicated Tsuzuki TTS settings entry; manifest queries installed TTS services. Listen dock/panel/settings use `presentation/tsuzuki/`; no neural runtime gate changes.

- **TTS-SETTINGS playback adapter:** `TtsController.kt` also closes an active queue when pausing during the silent gap before a pending handoff; queue/resume regression test included.

- **KOKORO-INTEGRATION upstream touches (2026-11-15, Debug-only Kokoro listening test):** `App.kt` (a worker process `:neural_tts` returns before Injekt/telemetry setup), `ui/reader/viewer/text/shared/TtsController.kt` (delegates neural playback, per-chunk phone fallback and engine-specific re-chunking to `tsuzuki/tts/NeuralSession.kt` / `TtsChunker.kt`; two optional constructor hooks), `ui/reader/viewer/text/shared/TtsTextUtils.kt` (`allowLongWords` flag so neural chunks never exceed 180 characters), `app/build.gradle.kts` (pinned `debugImplementation` of the source-built AAR; `verifyKokoroRuntime` hash check; release, preview and FOSS never depend on it), `app/src/debug/AndroidManifest.xml` (`:neural_tts` service), `TtsSettingsScreen.kt` (one call into `presentation/tsuzuki/settings/KokoroTestSettings.kt`), `.gitattributes` (AAR binary). `app/libs/*.aar` stays untracked pending an owner decision.

- **KOKORO-RELEASE (0.2.1, owner 2026-11-22):** the Kokoro voice ships in every arm64-v8a build. `KokoroTtsService` and its `:neural_tts` service moved to the main source set; `app/libs/sherpa-onnx-2.0.1-tsuzuki-kokoro-arm64.aar` is committed and `implementation` for all variants, hash-checked by `verifyKokoroRuntime` before every `pre*Build`; R8 keeps `com.k2fsa.sherpa.onnx.**` and `KokoroProgressCallback`. Pack manifest in `app/src/main/assets/kokoro-test-manifest.json` (name kept), pack built by `scripts/package-kokoro-cmu-test.py`, hosted as GitHub release `voices-v1`. Licences in About via `app/aboutlibraries/` and `docs/licenses/neural-tts/`.
