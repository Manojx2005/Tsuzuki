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
   then report what changed and what you saw on the devices. Codex never commits; Claude commits after review
   (owner as sole author, no push unless asked).

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
- The owner may be using a device while you work. If the screen isn't what you expect, don't tap through system pickers or permission dialogs; ask instead.

## Rules

- **Two themes, one layout.** Screens read colours from `TsuzukiTheme.tokens` (`app/.../presentation/theme/tsuzuki/`)
  and fonts from `MaterialTheme.typography` (display = Doto / Instrument Serif, body = Space Mono / Manrope).
  Don't hardcode theme colours in screens. In Dot, uppercase text with `tokens.case()`-style helpers, keep red
  as the only colour, and show covers in greyscale.
- **Japanese text** uses a partner font per theme face, chained behind the Latin font in `LnrTypography.kt`
  (Android 10+). Use `MaterialTheme.typography` styles so this applies; never set `fontFamily` directly.
  The Japanese fonts in `res/font/lnr_jp_*.ttf` are subsets (kana + JIS X 0208 kanji) made with the `subset-font`
  npm package; their licences are in `docs/licenses/`.
- **Legacy themes must keep working.** Tsuzuki UI is used only when `tokens.style != TsuzukiStyle.NONE`; other
  themes get stock Mihon/Tsundoku UI.
- **Real data only.** Never ship the design's sample numbers. If a tile has no backing data yet (streak, listen,
  source health), leave it out and note it in the task queue.
- **Keep Tsundoku mergeable.** Put new code in `tsuzuki` packages and keep edits to upstream files small
  (already touched: `Navigator.kt`, `CommonMangaItem.kt`, `BaseBrowseItem.kt`, `AppThemePreferenceWidget.kt`, `UpdatesUiItem.kt`, `ListPreferenceWidget.kt`, `SettingsMainScreen.kt`, `HistoryScreen.kt`, `MoreTab.kt`, `ReaderSettingsDialog.kt`, `SettingsAppearanceScreen.kt`, `UiPreferences.kt`, `PreferenceModule.kt`, `HomeScreen.kt`, `MainActivity.kt`, `App.kt`, `TachiyomiTheme.kt`, the two `Lnr*ColorScheme.kt`,
  `MangaCover.kt`, `MangaInfoHeader.kt`, `AppBar.kt`, `LibraryBadges.kt`, `HistoryItem.kt`, `UpdatesUiItem.kt`,
  `MoreScreen.kt`, `res/values/themes.xml`, `MangaScreen.kt` (both), `MangaToolbar.kt`, `ChapterSettingsDialog.kt`,
  `MangaViewModel.kt`, `LibraryContent.kt`, `LibraryPager.kt`, `LibraryViewModel.kt`, `NovelsTab.kt`,
  `presentation-core/.../theme/Color.kt` (`LocalActiveColor`), `NovelReaderAppBars.kt`, `ReaderActivity.kt`, `PreferenceScaffold.kt`, `BasePreferenceWidget.kt`, `TextPreferenceWidget.kt`, `AboutScreen.kt`, `CategoryListItem.kt`, `ExtensionsScreen.kt`, `ExtensionStoresContent.kt`, `ExtensionDetailsScreen.kt`, `GlobalSearchResultItems.kt`, `ui/.../BrowseSourceScreen.kt` (re-indented block, see order 14.9), `BrowseBadges.kt`, `Preference.kt`, `PreferenceItem.kt`, `SettingsLibraryScreen.kt`, `presentation/category/CategoryScreen.kt`, `CategoryFloatingActionButton.kt`, `SourcePriorityScreen.kt`, `ui/setting/SettingsScreen.kt`, `components/DropdownMenu.kt`, `LnrTypography.kt`, `AppLanguageScreen.kt`, `SettingsAdvancedScreen.kt`, `SettingsDataScreen.kt`, `res/xml/locales_config.xml`, `LibraryToolbar.kt`, `BrowseSourceToolbar.kt`, `WebViewScreenContent.kt`, `ReaderTopBar.kt`, `MassImportDialog.kt`, `DownloadHolder.kt`, `DownloadHeaderHolder.kt`, `MigrationConfigScreen.kt`, `JsPluginsTab.kt`, `NovelExtensionsTab.kt`, `ExtensionsTab.kt`, `ImportEpubScreen.kt`, `DownloadQueueScreen.kt`, `presentation/more/NewUpdateScreen.kt`, `presentation/manga/MangaNotesScreen.kt`, `presentation/more/onboarding/{OnboardingScreen,StorageStep,PermissionStep}.kt`, `ui/more/OnboardingScreen.kt`). Prefer a small dispatch at the top of an upstream composable that
  calls a `presentation/tsuzuki/...` version, as `HistoryItem` and `MoreScreen` do.
- **Sources.** Default repos are seeded once on first launch by `eu/kanade/tachiyomi/tsuzuki/DefaultSourceRepos.kt`:
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
- **Commits:** only when asked. The owner is the sole author: no `Co-Authored-By` or "Generated with" trailers.
- **Licences:** Apache-2.0 app. Don't copy GPL code (NovelDokusha and similar); see PLAN.md §1.

- **Installer compatibility files also touched:** `extension/util/ExtensionInstaller.kt`, `ExtensionLoader.kt`, `extension/ExtensionManager.kt`; whole Mihon installer with documented adapters, novel loader features retained.

- **Download provider compatibility:** `data/download/DownloadProvider.kt` now follows Mihon with retained synchronous APIs and novel ZIP support; async caller migration remains open.

## Progress, plan and history

- Live state, Codex queue, owner decisions and log: **`docs/HANDOFF.md`** (the only handoff; don't create others).
- Plan, roles, model tiers, parallelism rules and the anime plan: **`docs/WORKPLAN.md`**.
- The old task queue and orders 1–14 (with per-order notes) are in git: `git show 8f7f12e3b:AGENTS.md`.

- **Overnight 2026-11-06 additional upstream touches (small hooks):** `SourcesScreen.kt`, `GlobalSearchScreen.kt`, `SearchViewModel.kt`, `ExtensionStoresViewModel.kt`, `ExtensionStoresScreen.kt`, `ExtensionsScreen.kt`, `ExtensionDetailsScreen.kt`, `SettingsBrowseScreen.kt`, `SourcePreferences.kt`, `GetExtensionsByType.kt`, `ExtensionLoader.kt`, `ExtensionManager.kt`, `ExtensionInstallReceiver.kt`, `LoadResult.kt`, domain `Extension.kt`, `NovelStatusBar.kt` (body moved into a themed content fn), `ReaderTopBar.kt` (actions lambda), `ReadingModePage.kt` (import aliases), `StatusBarElementsScreen.kt`, `ScanlatorFilterDialog.kt`, `DownloadQueueScreen.kt`/`ViewModel.kt`, `CategoryViewModel.kt`, `categories.sq`/`mangas.sq` (read-only queries), `MangaRepository(Impl)`, `ChapterRepository(Impl)`, `NotificationReceiver.kt`, `Notifications.kt`, `InfoScreen.kt`, `BasePreferences.kt`, `CrashLogUtil.kt`, `NewUpdateScreen(Model).kt`, `TtsPlaybackService.kt`, `NovelWebViewViewer.kt` (find API), `MangaCover.kt` + cover call sites, `app/build.gradle.kts` (debugImplementation anime-player). See docs/UPSTREAM.md and the HANDOFF log for each.
