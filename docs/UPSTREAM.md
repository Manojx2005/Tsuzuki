# Taking changes from Mihon and Tsundoku

Tsuzuki = Tsundoku (novel features) + Tsuzuki design, and Tsundoku = Mihon + novels. All three share one
git history back to Tachiyomi's first commit, so upstream commits can be cherry-picked directly.

## Why new upstream features don't need design work

Tsuzuki's design is layered on top of upstream code instead of replacing it:

- **Dispatch at the top of upstream composables.** `HistoryItem`, `MoreScreen`, `ChapterSettingsDialog`, … call a
  `presentation/tsuzuki/...` version when `TsuzukiTheme.tokens.style != NONE`, and fall through to the stock code
  otherwise. Upstream can change the stock body freely; the dispatch line rarely conflicts.
- **Slots instead of rewrites.** `LibraryToolbar(below = …)`, `MangaToolbar(downloadSheet = …)`,
  `NovelReaderAppBars(onOpenContents = …)`: nullable parameters with a default, so upstream callers keep compiling.
- **Theme-level styling.** Screens Tsuzuki hasn't redesigned still pick up its colours and fonts through
  `MaterialTheme` (colour schemes + `LnrTypography`), `LocalActiveColor` (upstream's "active" tint),
  `PreferenceScreen` groups as tiles, `TabbedDialog` / `TabbedScreen` tabs as pills.

So a new Mihon feature arrives with **stock layout in Tsuzuki colours and fonts**: usable immediately, and it can
get a board and a Tsuzuki version later without blocking the merge.

## Release channel: upstream goes through Tsuzuki

```
Mihon / Tsundoku ──► weekly issue "Upstream changes to review" (.github/workflows/upstream-watch.yml)
                       └─► picked here (workflow below) ──► Tsuzuki main ──► Tsuzuki release ──► in-app updater
```

- Nothing upstream reaches users directly. The in-app updater (`data/updater/AppUpdateChecker.kt`) only checks
  `Manojx2005/Tsuzuki` releases.
- The updater calls GitHub's public releases API, so the repo (or a separate public releases repo, set in
  `GITHUB_REPO`) must be public before the first release.
- Tsundoku's release signing in `app/build.gradle.kts` only runs for the `tsundoku-otaku` owner; Tsuzuki needs its
  own keystore secrets before v0.1 (task queue: release).

## Remotes

Remotes are local git config; on a new machine `scripts/upstream-report.sh` adds them automatically:

```sh
git remote add tsundoku https://github.com/tsundoku-otaku/tsundoku.git
git remote add mihon    https://github.com/mihonapp/mihon.git
```

## Workflow

1. **Report:** `scripts/upstream-report.sh` (or `… mihon`) fetches and lists every upstream commit not yet in
   Tsuzuki (already-picked ones are hidden by patch id), grouped by area, with flags:
   - `.`: touches no file Tsuzuki or Tsundoku changed, usually picks cleanly;
   - `D`: touches a file Tsundoku changed from Mihon (likely conflict);
   - `T`: touches a file Tsuzuki changed (likely conflict, and **look at the screen afterwards**).
2. **Pick in a worktree** so uncommitted work is untouched:
   ```sh
   git worktree add ../Tsuzuki-upstream -b upstream/<topic> HEAD
   cp local.properties ../Tsuzuki-upstream/
   cd ../Tsuzuki-upstream
   git cherry-pick -x <sha>      # oldest first
   ```
   - Conflict only in `CHANGELOG.md`: keep ours (`git checkout --ours CHANGELOG.md && git add CHANGELOG.md &&
     git cherry-pick --continue`). Since v0.1 (2026-11-07) `CHANGELOG.md` is Tsuzuki's own; Tsundoku's history lives
     in `docs/CHANGELOG-tsundoku.md` and upstream changelog lines are never taken.
   - `app/google-services.json` was deleted for v0.1 (no telemetry, owner 2026-11-07). If a Tsundoku merge brings it
     back, delete it again (`git rm app/google-services.json`). Never build Tsuzuki releases with `-Pinclude-telemetry`.
   - Any other conflict: `git cherry-pick --abort` and log it below. Don't pull in a big refactor just to make one
     fix fit.
3. **Build and test**: `./gradlew :app:assembleDebug :app:testDebugUnitTest`. A clean textual pick can still fail
   to compile when it relies on a Mihon refactor (e.g. `download.manga`, the new `Chapter` model); revert those
   picks (`git revert`) and log them.
4. **Check on a device**: the screens the picks touch, plus one novel and one manga download if download code moved.
5. Merge the branch into `main` once the owner has committed main's work.

When Tsundoku itself later merges Mihon, commits already picked here usually merge cleanly (same patch).

## Big refactors in Mihon not taken (as of 2026-11-03)

Mihon after v0.20.4 redid the reader (Compose-drawn, viewer out of the ViewModel), stores dates as `Instant`,
dropped the legacy chapter model, reworked database queries for a new schema, and moved icons to Material Symbols.
Tsundoku rewrote much of that code for novels, so these are not cherry-pickable; most later Mihon fixes that build on
them fail for the same reason.

## Downloads

Novels and manga share `data/download/Downloader.kt`, with novel branches inside it. Novels are paced on purpose
(`NovelDownloadPreferences`: 3 s + 0–1 s jitter per request, one chapter per novel at a time, configurable under
Settings → Novel Downloads) to avoid source rate limits; Mihon's speed work mainly helps manga.

Mihon's largest speed-up, `730e43fd1` ("download a source's next chapter while the current one finishes"),
redesigns the scheduler that Tsundoku also redesigned (rate-limit guards, novel progress, keep-going-on-error), so
it needs a hand port. First step done (branch `upstream/mihon-picks`): novel-only code moved to `NovelDownloadEngine.kt`, cutting
`Downloader.kt`'s distance from Mihon's from 470/102 to 334/97 lines. It doesn't make `730e43fd1` pick cleanly,
because its conflicts are on Tsundoku changes that apply to every download (rate-limit guard wrapper,
keep-going-on-error, the `mangaTitle`/`mangaId` model); expect to hand-port Mihon scheduler changes. Direction: **one shared queue / notification / service with separate
engines** (manga = kept close to Mihon's file so its download commits pick cleanly; novel = Tsundoku's paced fetcher;
anime = from Aniyomi later).

## Pick log

### 2026-11-04: picks merged into main

Owner authorized local commits and the merge through the CODEX_PROMPT handoff; no push.
Main's existing work was committed in `cf5d4326c`, `5bb7e6555`, `f76fc2811`, and `fa569add0`.
All 28 picks merged without textual conflicts in `c54b2f809`; Tsuzuki hooks were retained.
Changed-file Spotless, `:app:assembleDebug :app:testDebugUnitTest` (630 tests), and tablet checks passed.
Checked Dot Home/details/novel reader, Colour details/novel reader/manga long-strip reader, and Dot manga reader.
Novel group pause held its queue unchanged, Resume advanced it; novel and manga queues survived force-stop
and resumed. Two manga chapters and a novel batch downloaded alongside each other.

Restart testing found an inherited tall-image recovery bug: process death during splitting leaves the original
beside incomplete split files, and retry could choose a split first, producing "0 pages failed" while failing
the folder count. `de061e0ef` prefers the original and removes its interrupted split siblings before regenerating
them, including when splitting has since been disabled. It also passes the existing padded page filename to
the splitter. The same failed chapter completed with the fix; its CBZ had 43 entries, valid CRCs and no empty
files, and opened in the reader. Build/tests passed again. Crash buffer empty.

All 12 fresh owner DB/prefs/WorkManager files were restored with push + run-as cp and SHA-256 verified before
restart. Continue is now Nano Machine chapter 336 (owner state at test start). Only the two newly created test
download folders were removed, and the original download-directory set matches. Tablet left in Dot Home.
Evidence and backup: `app/build/codex-merge-20261104/`; validation logs in `app/build/codex-*-validation.log`.
Phone was connected but not modified. Git unregistered the merged picks worktree; Windows' long-path deletion
failure left files in its old folder, so finish cleanup with long-path support. Anime worktree untouched.

### 2026-11-03: branch `upstream/mihon-picks` (worktree `../Tsuzuki-upstream`)

Applied (18), builds, unit tests pass, novel download checked on the tablet:

| Mihon | Change |
|---|---|
| 7aacaa349 | Keep the paused download notification after the worker stops |
| 6490c41e6 | Wait for a canceled downloader job to finish before starting another |
| deeb9b258 | Fix downloads not being found for sources sharing a stored source's name |
| d1c5c12bd | Fix webtoon reader leaving a gap after zooming in a resized window |
| 76b146f3d | Show the updates widget's empty and locked text |
| ab7ee00c4 | Fix tap navigation staying off after a cancelled button press |
| 785f4fd1e | Keep downloads in excluded categories when marking them read |
| be44a81fb | Keep listing local chapters when one archive can't be read |
| adc9ee74e | Respect hidden missing chapter indicators in chapter headers |
| a82d27c3a | Keep the chapter selection when new chapters download after a refresh |
| a01867dc3 | Only send source headers to the source in WebView |
| 35bd0a4b3 | Long strip side padding no longer counted in zoom bounds |
| abb126ad2 | Fix startup crash on some Samsung Android 9–11 devices |
| 51b61a36e | Keep the pager on its page when chapters are added before layout |
| 3156b4a1c | Insert split pages only into the chapter they belong to |
| 2df48e19a | Fail the extension install when nothing can confirm it instead of crashing |
| c371d6331 | Ignore long strip taps confirmed after the reader is gone |
| bd2ada2e8 | Fix Sui not being detected as a Shizuku provider |

Picked then reverted (compile against Mihon refactors): 611578313 (index a finished download found on disk),
36ec043a5 (delete downloads of duplicates).

Skipped for conflicts with Tsundoku's code (candidates for hand ports, most valuable first):
730e43fd1 + f02bf5ba7 + 9a6dea804 + fb8e640d6 + ba16158b1 (download speed / less work), 88d4071cc, f0f1e4de5,
a5d584eeb, f28b2d887, f82a7a68f (download correctness), 649fc83ff + 5a5ff22c3 + 5f63c983d + 7ae858e60
(WebView / Cloudflare robustness), c279291e2 (FAB under taskbar on tablets), dd4e9f00f (confirm unfavourite),
a6ee4bc59 (library range selection crash), 1e471a414, 287cbbb98, 8457fb91e, 07b9fb695, ae749bb4f, cdc7d69c9,
c9e1b6e96, 4c88f0264, d3f10772f, e9234f205, b5856135c, a42428284, 0864ce0f4, 5910a51eb, 0c1be369b, 40f552981,
c0c090999, ee5939454; database migrations (6dffca159, a999e49f2, 9dbb7148c, ea2b73ad7) need merging with
Tsundoku's migration numbering.

### 2026-11-03: download engine hand-ports (order 8, same branch)

| Mihon | Picks commit | Notes |
|---|---|---|
| 730e43fd1 | 43d299129 | One job per manga source, shared page-slot semaphore; the next chapter starts once all pages of the current one have a slot. Kept Tsundoku's per-host rate-limit guard/bypass (both refcounted per host, so overlapping chapters stay guarded; a bypassed chapter overlapping a guarded one on the same host is paced for that overlap) and its keep-going-on-error. Novels keep `NovelDownloadEngine.pickActive` and run through the same page loop with one slot. `DownloadNotifier.onProgressChange` is `@Synchronized`. |
| 9a6dea804 | 7ebe29d00 | Applied around Tsundoku's id-based `Download` constructor. |
| f02bf5ba7 | 993a0e112 | `DownloadProvider` lookup ported. Also fixes a Tsundoku bug: `queueChapters` matched downloaded chapters by list position against a `mapNotNull` result, so the wrong chapters could be skipped. |
| fb8e640d6 | f35251118 | Queued/downloaded lookups once per chapter list (MangaViewModel, UpdatesViewModel). |
| ba16158b1 | skipped | Builds on Mihon's MangaViewModel state refactor (`subscriberGate`, `downloadStates` map) that Tsundoku never took, and removes `ChapterList.Item.downloadProgress`, which the chapter rows read. Take it with that refactor after the merge (order 9). |

Tsuzuki fix found while testing, `6f8319531`: on a cold start `SourceManager.isInitialized` turns true before
installed extensions reach the source map (`installedExtensionsFlow` is derived on its own scope), so
`DownloadStore.restore` found no source, dropped every queued manga chapter and cleared the store. The downloader now
waits for the source manager, and restore gives a missing source up to 10 s (once per source), logging when it still
drops one. Mihon restores the same way; worth reporting upstream.

Tablet test (Galaxy Tab S11, Asura Scans + Royal Road): 10 manga chapters with consecutive chapters overlapping
(`Chapter 4_tmp` and `Chapter 5_tmp` at once), 3 novel chapters alongside with request starts 3.0 s and 3.6 s apart,
kill + cold start restores a seeded manga queue (2 → 2) and a novel queue (3 → 3, resumed and finished). Novel group
pause wasn't shown on the device: the 3 chapters finished before the pause landed. The code path is unchanged.
Build and unit tests pass. Test downloads were deleted and the owner's database/preferences restored byte-for-byte.

### 2026-11-04: WebView / Cloudflare robustness group

Fetched Mihon main `8794e89a8`. These three files are identical to that revision:
`core/common/.../network/interceptor/CloudflareInterceptor.kt`, `WebViewInterceptor.kt`, and
`util/system/WebViewUtil.kt`. They include per-host challenge serialization, non-GET challenges on the site's
root, early exit for interactive/failed challenges, renderer-death handling and unsafe-header filtering.
NetworkHelper uses the existing Injekt app coroutine scope; Tsundoku's rate limiting is retained. Both app
WebView entry points use the shared header filter. The existing themed browser/menus stay; its renderer-death
callback closes the failed page with the upstream message (eight locales). WebKit 1.18.0 matches Mihon and
the [official stable release](https://developer.android.com/jetpack/androidx/releases/webkit#1.18.0).

Normal preview/debug builds, changed-file formatting, 630 app tests and 26 core tests pass. Three new core
tests reject Origin/transport/proxy headers, retain source authentication/UA/Referer, and preserve the first
repeated header. Optimized renderer regression passes on Tab S11 (2.689 s) and S22 (4.442 s): actually crash
the renderer, require the callback and page closure, then render a fresh data page. Ordinary daily preview
then loaded real Asura (tablet Colour) and Royal Road (phone Dot) pages; BACK returned to the reader.
Seven ARM64 ELF/APK 16 KB checks and packaged baseline assets pass. Both devices left Dot Home; original
debug data was untouched. Preview copies contain test reading positions/history.

Reproduce the renderer regression with JDK 21 and the separate seeded preview copy:

```powershell
./gradlew.bat :app:assemblePreview :app:assemblePreviewAndroidTest -PtsuzukiTestBuildType=preview
# Install BOTH matching APKs on the selected device before running instrumentation:
# app/build/outputs/apk/preview/app-arm64-v8a-preview.apk
# app/build/outputs/apk/androidTest/preview/app-preview-androidTest.apk
# runner: io.github.manojx2005.tsuzuki.debug.test/androidx.test.runner.AndroidJUnitRunner
# class: eu.kanade.tachiyomi.tsuzuki.WebViewRendererRecoveryTest
```

The test refuses the original `.dev` app. AndroidJUnitRunner needs `androidx.tracing.Trace` retained in its
target; the conditional test-only rule applies solely with this property. Build the normal daily preview
without it. Selecting preview testBuildType removes the debug unit-test task, so run debug unit validation
separately. `assemblePreviewAndroidTest` alone does not package an updated target APK: build both as above.
Local evidence, matching test APK pair/SHA manifest, callbacks, screenshots and daily-process crash logs:
`app/build/codex-preview-20261104/`; final normal-build log `app/build/codex-webview-daily-validation.log`.
Two initial instrumentation-runner startup failures were harness/package issues, not renderer recovery; the
final tests pass. Daily main-process crash logs after cleanup are empty.

Still pending in priority 3: asynchronous source-header resolution, installer and download-provider ports.
The current installer differs in repo/signature models and DI; retain compatibility when adapting it.
No live concurrent Cloudflare challenge was forced in this check.

Follow-up: source lookup and header construction now run on IO in both the activity and navigator entry
points, following Mihon's asynchronous readiness pattern while retaining Injekt/manual factory integration.
The page is not constructed until headers are ready; missing sources and lookup/getter exceptions resolve
to an empty map, while cancellation propagates. Navigator browser routes have distinct ViewModel keys.
Four new tests cover blocked off-caller execution/readiness and the missing/getter/lookup failure cases.
All 634 app tests pass; core remains 26 passing tests. Debug/optimized builds pass. Renderer regression was
repeated successfully on both devices (2.579 / 4.163 s). Ordinary preview then checked both entry points:
tablet Colour Asura chapter/activity and Royal Road series/navigator, phone Dot Royal Road chapter/activity
and Asura series/navigator; all four pages load and BACK returns correctly. Seven-library/profile/16 KB
checks pass. Both devices returned Dot Home; test companions removed; original debug data untouched.
Logs: `app/build/codex-webview-headers-{debug,preview,daily}-validation.log`; new device logs use
`webview-headers-renderer-recovery.log`, with a separate matching instrumentation APK pair retained locally.
Installer/provider work remains; the previous synchronous-header item is now done.

### 2026-11-04: extension installer compatibility group

`ExtensionInstaller.kt` follows fetched Mihon main `8794e89a8` whole, with these retained adapters:
Injekt constructor defaults and app coroutine scope; explicit IO launch; rate-limit exemption for APKs;
Tsundoku's Boolean private-install result; context-bearing cancellation broadcast. The download uses cancellable
OkHttp await, response ownership, interruptible copy and an active-job check before installation. Job/step maps
are concurrent. Downloaded APKs must be recognized extensions signed with the listing store's key before any
installer is launched. Manager callers pass the available listing; no repository/model rewrite was needed.
The loader exposes archive/signature helpers, accepts the already-read package info, and handles missing signing
info safely; both manga and novel extension features and existing private update/downgrade checks remain.

Debug build and all 640 app tests pass (six new installer cases: matching key/private success, private failure,
wrong key, invalid archive, HTTP failure/response close, cancellation during body copy). Optimized instrumentation
passes on Tab S11 (0.119 s) and S22 (0.366 s): read the copied real Asura APK's signing certificates, serve it over
loopback, and require the exact wrong-store-key rejection plus removal of the downloaded temporary APK. No real
source is installed or uninstalled. Successful in-app system/private installation/update on a device remains open;
the success/failure adapter is covered by unit tests, not claimed as a completed live install matrix.

Instrumentation directly calls target methods/constructors which R8 may remove. The existing conditional
`src/androidTest/preview-target-rules.pro` now keeps those entry points and data constructors in addition to Trace.
Initial device runs failed with a removed-helper NoSuchMethodError before exercising behavior; both pass after
rebuilding/installing the matching target/test pair. These rules are excluded from the ordinary daily APK.
Matching pair and hashes: `app/build/codex-preview-20261104/installer-instrumented-pair.json`; test logs per device
are `installer-signature-test.log`. Initial harness failures are saved separately. Build logs:
`codex-installer-final-debug-validation.log`, `codex-installer-kept-instrumentation-build.log`.

Ordinary daily preview rebuilt without the conditional keep rules (configuration checked), installed on both.
Final Dot Home screenshots inspected; daily main-process crash logs empty. Test companions and copied APK
removed, original debug data untouched. Seven ARM64 ELF alignments, APK zip alignment and profile assets pass.
Final build `codex-installer-daily-validation.log`; all 640 app tests also rerun successfully after test cleanup
synchronization in `codex-installer-final-tests.log`. DownloadProvider and live successful install matrix remain.

### 2026-11-04: download provider compatibility group

`DownloadProvider.kt` now follows Mihon `8794e89a8` with Injekt defaults, retained synchronous get/find entry
points for existing Tsundoku reader/export/cache callers, always-enabled new-download hash default (no new
preference), and `.zip` alongside `.cbz`. The existing writer's combined scanlator/name and six-character URL
hash are unchanged. Upstream's cached naming parts recognize older independently-sanitized chapter/scanlator
forms and ASCII/hash variants. Batch `findChapterDirs` is now suspend and shares the optimized lookup already
used by downloaded-ID discovery: one case-insensitive listing for at least 50 SAF chapters, per-name probing
for plain files. The existing call site already suspends, so no caller edits were needed.

All 647 app tests and debug/optimized builds pass. Seven new tests cover stable write names, historical/ASCII
variants with ZIP support, slash safety, multibyte byte limits, old ZIP lookup, the single-list SAF path and
plain-file per-name behavior. Initial focused run exposed an incorrect test expectation (older split naming
left the scanlator Unicode); corrected to the actual historic combined ASCII name, with no production change.
Both optimized device tests pass: Tab S11 0.290 s / S22 0.533 s. Each creates a unique source folder, writes an
older-name novel ZIP, finds and reads its HTML via the real provider, then verifies file and folder cleanup.
No book added, preference changed or existing download modified. Matching test pair/SHA and device logs are
under `app/build/codex-preview-20261104/` (`provider-instrumented-pair.json`, `provider-compatibility-test.log`).
The direct provider methods and temporary StubSource constructor have conditional instrumentation-only keeps.

**Still open:** move synchronous reader/cache/export/translation callers together to asynchronous storage APIs.
This port preserves their existing API and does not claim to fix main-thread storage access. In particular,
`ReaderTransitionView` currently requests an uncached on-disk check while binding, and needs a measured follow-up.
Successful live extension installation and the broader optimized download/recovery matrix also remain open.
Logs: `codex-provider-debug-validation.log`, `codex-provider-preview-validation.log`; normal daily build passes.
Ordinary daily preview excludes instrumentation keeps (configuration checked), is installed on both, and leaves
both Dot Home with final screenshots inspected and current main-process crash logs empty. Companions removed;
original debug data untouched. Seven ARM64 ELF alignments, zipalign and baseline.prof/profm packaging pass.
Final build log `codex-provider-daily-validation.log`; no live build/device process remains.

### 2026-11-06: Mihon fixes group A (N1)

| Mihon | Disposition |
|---|---|
| `1e471a414` | Hand-port: ignore download events for entries no longer in the queue; the existing queue/cache refresh resolves the completed status. |
| `287cbbb98` | Phone already shows the last update on an empty list. Hand-port to the Tsuzuki tablet list. |
| `8457fb91e` | Inapplicable: Tsundoku's chapter recognizer parses ASCII matches directly and has no locale-dependent formatter. |
| `c9e1b6e96` | Hand-port the unknown tri-state fallback. The stock select widget already guards empty options and bounds its selection; Tsuzuki iterates the available options. |
| `4c88f0264` | Hand-port: update each result from the state inside the atomic update and retain the source filter. |
| `d3f10772f` | Hand-port: include local manga and novels independently of enabled languages, still respecting disabled sources and the selected source filter. |
| `e9234f205` | Hand-port: match, scroll to and highlight enabled group headings in both layouts. |
| `07b9fb695`, `ae749bb4f` | Hand-port into the existing service/broadcast installer. The earlier installer sync did not change its queue. |
| `cdc7d69c9` | Hand-port: stage the APK next to the target and rename only after the copy succeeds, preserving the old APK on failure. |

### 2026-11-06: Mihon fixes group B (N2)

| Mihon | Disposition |
|---|---|
| `b5856135c` | Skipped: the strip renderer relies on Mihon's native frame and pixel-buffer API. Tsundoku's decoder returns a whole Bitmap and also handles reader crop and display profiles. Porting the dependency and reader pipeline needs a separately tested card. |
| `a42428284` | Hand-port: keep ordinary cover keys stable across unfavourite and read custom covers only for favourites. |
| `0864ce0f4` | Hand-port: default-on redaction of Auth and Authorization in verbose network logs, with the upstream Advanced settings switch. |
| `5910a51eb` | Hand-port: restore real source names as missing stubs when restoring entries, preserving novel classification and existing stub metadata. |
| `0c1be369b` | Hand-port: show the existing add-to-library message in the cover dialog before opening the editor for remote books. |
| `40f552981` | Hand-port the blank-title guard to Tsundoku's exact and alternate title lookup. Alternate titles can still match for books without a title. |
| `c0c090999` | Already applied in Tsundoku's Algolia Kitsu search via the subtype filter. |
| `ee5939454` | Hand-port: use the reading history instant directly for the tracker start date. |
| `dd4e9f00f` | Hand-port: confirm unfavourite in both details layouts through their shared callback. The reused removal dialog follows the active theme. |

## N17: skip an app update version, download in the update screen (2026-11-06)

Hand-ported (Tsuzuki's updater still uses Injekt, not Mihon's Metro DI, so neither commit cherry-picks).

| Mihon commit | Disposition |
|---|---|
| `fc20410ce` Allow skipping an app update version | Hand-ported. `BasePreferences.skippedAppUpdate`, `NewUpdateScreenModel.skipUpdate`, `MainActivity` hides the exact skipped version (`isAppUpdateSkipped`, unit-tested), `CrashLogUtil` line, `InfoScreen.rejectAction` slot, confirm dialog + skip button in stock `NewUpdateScreen`, "Skip this version" link in `TsuzukiNewUpdateScreen` (dialog shared with stock). Strings `update_check_skip*` in base + 7 locales. Skipped: Mihon's `update_disabled.svg` MaterialSymbols icon (Tsuzuki uses `Icons.Outlined.UpdateDisabled`). |
| `aa25b3a14` Download app update without WorkManager (#3707) | Hand-ported. `NewUpdateScreenModel` downloads in `viewModelScope`; `AppUpdateDownloadJob` deleted, with the dead NotificationReceiver start/cancel actions, the `app_apk_update_channel` channel (now in the deprecated list) and strings `update_check_notification_download_in_progress` / `channel_app_updates` (base only, as Mihon). Tsundoku's `rateLimitExempt()` kept on the APK request. |

Repo is private, so the live update check 404s; check on a device by temporarily pointing `GITHUB_REPO` at a public repo with a newer release (or lowering `versionName`).

## N18: failed-to-load extensions with reason (2026-11-06)

| Mihon commit | Disposition |
|---|---|
| `e6d8efe6f` Expose installed extensions that failed to load (#3953) | Hand-ported to Tsundoku's loader without Mihon's `Extension.Loaded/NotLoaded` rewrite (that touches every extension consumer and Metro DI). New `Extension.Failed` (domain) with `Reason` enum, detail text, stack trace, icon, shared/private flag. `LoadResult.Failed` replaces the silent `LoadResult.Error` for: missing version, unsupported lib version, unsigned, class loader error, missing `*.class` meta, source class not found, source constructor/factory throwing. `Error` is kept for "package unreadable" and the hidden-NSFW case (a user setting, not a fault). Novel loading (`tachiyomi.novelextension`, `.novel` meta, namespace fallback) unchanged. `ExtensionManager.failedExtensionsFlow`, receiver `onExtensionFailed`; failed rows join the "Installed" section of both manga and novel extension tabs (filtered by `isNovel`), via `Extensions.failed` / `GetExtensionsByType`. Tapping the row or the info button opens `ExtensionFailedDialog`: reason, selectable stack trace, Copy (report from `FailedExtensionReport.toReport()`, unit-tested) and Uninstall. Rows use the existing grouped tiles in the Tsuzuki themes. Strings `tsz_ext_failed*` in 8 locales. |
| `fdc2d40a1` Reload extensions when relevant settings are updated (#3954) | Skipped: depends on Mihon's content-warning preferences, `Extension.Loaded/NotLoaded` and the Metro app graph (`appGraph.trustExtension`), none of which exist here. Tsundoku already loads an extension straight after it is trusted; the NSFW preference is still read once per process (`loadNsfwSource`), so a restart is needed there as before. |

## N19: content warnings instead of the NSFW toggle (2026-11-06)

No database migration and no backup-format change: the setting is two app preferences (`enabled_content_warnings`, `apply_content_warnings_to_installed`), and the index models already carried `contentWarning`.

| Mihon commit | Disposition |
|---|---|
| `0fbaa1ece` Replace extension NSFW flag with content warning app-wide (#3951) | Hand-ported (Mihon uses the Metro graph and a numbered `Migration`; Tsundoku uses Injekt). `ContentWarning` enum in domain; `Extension.isNsfw` is now `contentWarning` on every variant (the anime extension keeps its own boolean); `ContentWarningLabel`, Browse settings multi-select (authentication when allowing 18+/Mixed), extension list/details labels with the Mixed/18+ descriptions. Instead of Mihon's version-30 migration, `enabledContentWarnings` takes its default from the old `show_nsfw_source` key on first read (off = Safe only, otherwise all), so no `Migrations.kt` version number is spent; the old key is left in place. Pure logic is in `extension/util/ContentWarningPolicy.kt` (tested). The metadata parse keeps Tsundoku's `tachiyomix.nsfw` legacy flag. The installed-extension list is no longer filtered in `GetExtensionsByType` (the loader decides); the anime loader uses the same policy. |
| `0e0bc6972` Allow user to load installed extensions regardless of content warning (#3952) | Ported as `applyContentWarningsToInstalled` (default on); switching it off needs authentication, and then installed extensions load and update whatever their warning. Available extensions are always filtered by the allowed warnings. |
| `fdc2d40a1` (#3954, skipped in N18) | Still skipped: the setting needs an app restart, as the info text says. |

Strings: 11 new keys in base, ja, zh-rCN, ko, es, pt-rBR, fr, de. The old `pref_category_nsfw_content`, `pref_show_nsfw_source`, `parental_controls_info` and `ext_nsfw_warning` strings are now unused and left in place to keep merges with the other locales simple.

## V2b Mihon release picks (2026-11-07)

| Mihon commit | Disposition |
|---|---|
| `35bb84705` | Skipped. The fix releases a GPU texture in Mihon's `WebGpuViewer`. Tsuzuki uses its existing pager, Webtoon and novel viewers. |
| `a82baac3e` | Skipped. Tsuzuki's Kitsu tracker uses a fixed score list and has no server-provided score-type lookup during login. |
| `1e054ea14` | Already covered by Tsuzuki's MAL expiry check, which refreshes after the expiry time adjusted by one minute. |
| `40f552981` | Already applied by N2 in the Tsundoku title lookup. |
| `4b48a84ec` | Applied as `ed17f1112`. Adapted to Tsundoku's `MangaRestorer`: an existing tracker keeps its stored remote entry and takes only higher chapter progress from the backup. |
| `6d4ed6cb2` | Already covered by `ExtensionLoader` using `ChildFirstPathClassLoader` on all supported Android versions. |
| `b19c8abeb` | Applied as `2c7bac0b2`. Shared `TabText` measures the badge first and ellipsizes the label into the remaining width. |
| `0864ce0f4` | Already applied by N2. Verbose network logging redacts Auth and Authorization by default. |
