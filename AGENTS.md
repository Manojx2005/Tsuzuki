# AGENTS.md — working on Tsuzuki

Tsuzuki is an Android reader for light novels, web novels, manga and manhwa (Kotlin, Jetpack Compose).
It is a fork of Tsundoku (itself a Mihon fork). Read these before changing anything:

- `docs/PLAN.md`: product and engineering plan (the theme system is in §3).
- `docs/design/README.md`: design tokens, nav/rail specs, board index, and **implementation status**.
- `docs/GETTING_STARTED.md`: first-time setup.

## When the owner says "continue"

1. Open **Task queue** below and take the first unchecked task.
2. Find its design board in `docs/design/README.md` and build it for **Dot first**, then check Colour.
3. Build, install on the connected devices, look at a screenshot (see **Test on devices**), and fix what's off.
4. Tick the task here, update the status table in `docs/design/README.md`, then report what changed and what you saw on the devices.
   Don't commit unless the owner asks.

## Build

Windows machine, Git Bash or PowerShell.

```sh
# JDK 21 from Android Studio. The `java` on PATH is JDK 24, which this Gradle setup doesn't support.
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
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
- Android shows a "16 KB alignment" warning for upstream native libraries in debug builds. It's harmless; fix it before release.
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
  (already touched: `HomeScreen.kt`, `MainActivity.kt`, `App.kt`, `TachiyomiTheme.kt`, the two `Lnr*ColorScheme.kt`,
  `MangaCover.kt`, `MangaInfoHeader.kt`, `AppBar.kt`, `LibraryBadges.kt`, `HistoryItem.kt`, `UpdatesUiItem.kt`,
  `MoreScreen.kt`, `res/values/themes.xml`). Prefer a small dispatch at the top of an upstream composable that
  calls a `presentation/tsuzuki/...` version, as `HistoryItem` and `MoreScreen` do.
- **Sources.** Default repos are seeded once on first launch by `eu/kanade/tachiyomi/tsuzuki/DefaultSourceRepos.kt`:
  the LNReader plugin index (novels, via the QuickJS plugin host) and Keiyoushi (`repo.json`, which points at their
  v2 protobuf index; the legacy `index.min.json` only lists an "Outdated App" stub).
- **Commits:** only when asked. The owner is the sole author: no `Co-Authored-By` or "Generated with" trailers.
- **Licences:** Apache-2.0 app. Don't copy GPL code (NovelDokusha and similar); see PLAN.md §1.

## Task queue

Done:
- [x] Build runs on device; Colour and Dot themes, fonts
- [x] Theme tokens, progress bar and ring, tile, dot-grid background, cover palette (`BookPalette`)
- [x] Nav pill bar (phone) and rail (tablet), with Home replacing Library in the nav
- [x] Bento Home: phone, tablet portrait, tablet landscape (without the details pane), empty state
- [x] Default source repos (LNReader + Keiyoushi)
- [x] Startup animation (`MotionSplash`): `presentation/tsuzuki/startup/TsuzukiStartup.kt`, overlay in `MainActivity`; the system splash is blank black; the "…" loop waits for `StartupSignal.homeReady`; tap to skip; skipped when animations are off
- [x] Home verified with real books (Royal Road via LNReader plugins) on the tablet, both themes: Continue hero, Resume reopens the reader mid-chapter, real chapter names, "new" = chapters fetched since last read, shelf fills with library books
- [x] Cover palette: `rememberBookPalette` + animated `ProvideBookPalette` around the home tabs (`HomeScreen.kt`); Colour takes the last-read book's colours (dark-vibrant swatch, lifted to navy depth)
- [x] Tablet landscape details pane (`DetailsPane` in `TsuzukiHomeScreen.kt`): select a shelf book, Resume/Open, latest 5 chapters with New/Reading/Read
- [x] Shared components follow the theme: `MangaCover` is greyscale in Dot everywhere, `AppBarTitle` uses the display face (uppercase in Dot), library unread badges use the accent, dotted progress bars keep a compact pitch on wide rows
- [x] History rows (`presentation/tsuzuki/history/TsuzukiHistoryItem.kt`, dispatched from `HistoryItem`) with chapter progress; Updates rows get the tile treatment in `UpdatesUiItem` (**not yet seen on a device**: no updates existed when tested)
- [x] More as a bento (`presentation/tsuzuki/more/TsuzukiMoreScreen.kt`, dispatched from `MoreScreen`): Saved-only and Incognito toggle tiles, destination tiles, localised labels; checked on both devices
- [x] Novel details basics: display-face book title (`MangaInfoHeader`), and `primaryContainer` = accent in both Tsuzuki colour schemes so the Start/Resume FAB and other main actions use the accent
- [x] Upstream bug: a plain launcher start never set `MainActivity.ready`, so the splash waited the full 5 s; now set after `handleIntentAction` (checked with the stock theme)
- [x] Colour palette also re-colours `MaterialTheme` (background/surface/primary) inside `ProvideBookPalette`, so upstream tabs match the rail and Home
- [x] `primaryContainer` = accent reviewed: upstream uses it for selected/active states, which the boards paint in the accent
- [x] Japanese text matches the themes: DotGothic16 / M PLUS 1 Code (Dot), Noto Serif JP / Zen Kaku Gothic New (Colour) as per-glyph fallbacks; library toolbar title uses `AppBarTitle`
- [x] Anime placeholder in the app: Home has a Read / Watch switch; Watch shows a themed "coming soon" screen (`WatchPlaceholder` in `TsuzukiHomeScreen.kt`). The canvas boards on every page already include anime.
- [x] Library progress bar under covers in both grid modes (compact + comfortable)
- [x] Novel details: action buttons are tiles (`MangaActionButton`), chapter rows are tiles with swipe actions kept (`MangaChapterListItem`)
- [x] Novel details: Resume tile with progress + next chapter (`presentation/tsuzuki/details/TsuzukiResumeTile.kt`, phone + tablet layouts); action labels wrap to two lines in 72 dp tiles
- [x] Reader bars are solid in the Tsuzuki themes (`NovelReaderAppBars`: surfaceContainer instead of a 90% tinted surface)
- [x] Home, Watch placeholder, Resume tile and nav strings are `MR.strings.tsz_*` (English + Japanese); the phone date line uses the device locale
- [x] Literata is the default reading face (`res/font/lnr_literata.ttf`, Latin subset, OFL in `docs/licenses/OFL-Literata.txt`); font value `"literata"` works in both the TextView and WebView engines and is listed first in all three font pickers

Next, in order:
- [ ] Samsung cold start shows ~1 s of `#1F2125` grey (the system's dynamic dark background) before the startup animation. **Ruled out (2026-11-03):** it is not our splash. With `windowSplashScreenBackground` set to bright red (both `windowSplashScreenBackground` and `android:` variants, `res/values-v31/splash.xml`) the red never appeared, for both `monkey` and `am start --splashscreen-show-icon` launches; the original logo splash also sat on this grey. Next ideas: tap the real launcher icon, reboot to clear starting-window caches, test on a non-Samsung device, check One UI's app-launch animation settings. Low priority: it's the system's surface while the process starts.
- [ ] Library (`LibraryList`, `LibraryDisplay`, `LibrarySearch`): covers/badges already follow the theme. Done: display-face title, progress bar under each cover (both grids). Category pills done (`LibraryTabs`). Still to do: `LibraryMixed` (novel/manga/manhwa filter).
- [ ] Updates: stat tiles done (`presentation/tsuzuki/updates/TsuzukiUpdatesTiles.kt`: New count + Save all). Filter chips are pills (shared `theme/tsuzuki/TsuzukiPill.kt`, also used by Library categories). "Airing today" strip waits for anime.
- [ ] History: the three stat tiles from the board (Today / Chapters / Streak) once reading sessions are recorded.
- [ ] Novel details: chapter filter and download sheets (synopsis tile done).
- [ ] Reader chrome (`Reader`, `ReaderSettings`, `ReaderTOC`, `ReaderBars`): Literata on black in Dot, on the book tint in Colour.
- [ ] Settings (`Settings`, `SettingsAppearance`, `TabletSettings`). Add the Source health tile to More once health data exists.
- [ ] Browse (`Browse`, `Extensions`, `ExtensionRepos`, `BrowseManga`, `Catalogue`, `TabletBrowse`). Done: tab row is a pill row (`TabbedScreen` → `TsuzukiPillRow`). Global-search tile and grouped source tiles done (`presentation/tsuzuki/browse/TsuzukiBrowse.kt`). Extension sections are tiles too. Still to do: pinned sources as big two-up tiles (health dots wait for source health data).
- [ ] Motion (`MotionSpec`, then `MotionOpen`, `MotionReader`, `MotionStack`, `MotionTheme`, `MotionSheet`).
- [ ] Data-backed tiles: reading streak and daily goal (needs a reading-sessions table), Listen tile (TTS state), source health (`SourceHealth`).
- [ ] Translate the `tsz_*` strings into more languages (only base English + Japanese exist). Dot nav short labels (HOME/NEW/PAST/FIND) are intentionally English.
- [ ] Fix the 16 KB page alignment of native libraries, then release v0.1.

Later (after the queue above), anime integration from Aniyomi (Apache-2.0), designed on the canvas page "Final · Anime":
- [ ] Stage 1 spike on a branch: copy Aniyomi's `animesource` API, anime extension loader and mpv player (+ `aniyomi-mpv-lib`, `ffmpeg-kit`); play one episode from one extension. Clone Aniyomi with `git -c core.longpaths=true clone` on Windows.
- [ ] Stage 2: anime + episode tables (merge migrations with Tsundoku's), anime in the library with the Novels / Manga / Anime switch.
- [ ] Stage 3: details/episodes, history, updates, downloads, backups, trackers.
- [ ] Stage 4: restyle to the anime boards (Colour + Dot).
