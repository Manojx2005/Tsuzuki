# Performance verification

Use JDK 21 and an optimized `preview` APK. Debug Compose timings are not representative.
Preview uses `io.github.manojx2005.tsuzuki.debug`; debug uses `io.github.manojx2005.tsuzuki.dev`.
Back up the owner's data before testing and use a separate preview library copy. An Activity-window time
from `am start -W` is not the same measurement as Home readiness or reader content readiness.

## Baseline and startup profiles

The collector in `baseline-profile` exercises Tsuzuki Home, Library scrolling, details, a cached novel reader,
Updates, History and Browse/extensions. It requires an English or Japanese test app with a restored library.
Pass `profileBookTitle` and a short `profileReaderText` that is visible in the cached chapter; collection fails
if the requested book or chapter does not load. It never seeds sample books in the production app.
The startup collector waits for the populated Home screen, beyond the first Activity window.

The default Gradle configuration retains the Pixel 6 API 34 managed device. Pass
`-PtsuzukiProfileConnected=true` to select connected-device collection instead. For collection against an
already prepared preview copy, the standalone test APK can also be run explicitly on one device:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr'
./gradlew.bat :baseline-profile:assembleNonMinifiedRelease
$adb = 'C:/Users/Manoj/AppData/Local/Android/Sdk/platform-tools/adb.exe'
& $adb -s R52Y90HA77L install -r baseline-profile/build/outputs/apk/nonMinifiedRelease/baseline-profile-nonMinifiedRelease.apk
```

Run `am instrument -w` with runner `mihon.baselineprofile/androidx.test.runner.AndroidJUnitRunner` and these
arguments: `targetAppId=io.github.manojx2005.tsuzuki.debug`,
`class=mihon.baselineprofile.BaselineProfileGenerator#generate`, `profileBookTitle=<test novel title>` and
`profileReaderText=<visible cached text>`. For startup, select
`mihon.baselineprofile.StartupProfileGenerator#generate` with the same target app.
Use the test's reported output paths under `Android/media/mihon.baselineprofile/` to pull the text profiles.
Do not operate the target device while collection is running.

Merge collected rules with the existing `app/src/main/baselineProfiles/` rules, combining H/S/P flags for
identical descriptors. Keep the existing legacy-theme coverage. Rebuild preview and verify that the APK
contains `assets/dexopt/baseline.prof` and `baseline.profm` before treating the profiles as shipped.

## 2026-11-04 collection evidence

The expanded baseline collector passed on the Tab S11 against a seeded optimized preview copy (397 seconds,
26,422 collected entries). The Home-ready startup collector passed separately (77 seconds, 16,476 entries).
The merged source profiles have 47,557 baseline entries and 34,382 startup entries. The collected baseline
includes Tsuzuki Home, the mixed Library grid, details, ReaderActivity and the native TextView novel renderer.
Renderer audit: 32 native NovelViewer rules and saved UI trees confirm native content. Two rules mentioning
NovelWebViewViewer are class/signature references and do not establish that WebView rendering was exercised.
The test library had four real books, so this run does not establish large-library scrolling performance.

Logs, collected profiles and coverage audit: `app/build/codex-preview-20261104/` (ignored local evidence).
The preview/debug build with the new profiles and all 630 app unit tests passed (2m26s). The rebuilt ARM64
preview APK is 36,767,472 bytes, packages baseline.prof/profm, and passes 16 KB ELF and APK alignment checks
for all seven ARM64 libraries. Validation log: `app/build/codex-profile-packaging-validation.log`.
One tablet cold Activity-window launch was 250 ms and one phone launch 594 ms; these are individual
observations, not comparative benchmark results or proof of Home-ready latency. Frame timing, repeatable
before/after startup measurements and the remaining reader flows are still required.

## Controlled cold-start measurements (2026-11-04)

`ColdStartupBenchmark` now captures frame timing through populated Home as well as the separate initial-window
metric. Ten cold launches per compilation mode passed on the Tab S11, optimized preview, Dot, four-book test
library. No thermal-throttle sleep was reported. APK SHA-256:
`e7c6c73f078705c4f4bdf057d140b4b50cb7f6afc84ef1c8708da9d1250cf17a`.

| Measurement | Shipped baseline profile | Compilation disabled |
|---|---:|---:|
| Initial-window median | 380.7 ms | 393.9 ms |
| Initial-window min–max | 337.4–399.7 ms | 384.7–443.5 ms |
| Frame CPU P95 | 19.7 ms | 21.8 ms |
| Frame overrun P95 | 17.4 ms | 21.8 ms |
| Frames beyond deadline | 267 / 1,450 (18.4%) | 292 / 1,429 (20.4%) |

The median initial-window difference is about 3.3%; the ranges overlap. This is a small single-device result,
not evidence of a large speed-up or a measurement of Home-ready latency. The frames include startup motion;
overruns remain and need trace-led investigation. Evidence: `cold-startup-benchmark.log`,
`cold-startup-benchmarkData.json`, `cold-startup-summary.json` and `startup-main-thread.csv` under the local
preview evidence directory above.

The first trace includes WebView initialization during application binding. Its user-agent warm-up deliberately
precedes dependency initialization to avoid a documented UI/background-thread deadlock in `App.kt`; do not
simply defer it to improve a startup number.

The splash now reads its exit value inside `graphicsLayer` and its Colour drop value inside the Canvas draw
callback. This avoids recomposing the surrounding layout for these frame updates and retains the board's
motion, geometry and timing. Optimized/debug builds and all 630 app tests pass. Dot/Colour startup recordings
were inspected on the tablet. A further ten-launch baseline-profile run passed (54 seconds): initial-window
median 365.8 ms, range 346.5–392.6 ms; frame CPU P95 20.3 ms, overrun P95 16.9 ms, 268/1,453 frames beyond
deadline (18.4%), no thermal sleep. The frame results are mixed and do not demonstrate an overall smoothness
gain. The initial-window change also precedes the animated state reads and cannot be attributed to them.
Evidence: `cold-startup-draw-benchmark*` and device `*-startup.mp4` files in the preview evidence directory.

## v0.1 cold start, S22 (2026-11-07)

Optimized preview from main `17ac99c42`, Galaxy S22, Colour theme, Japanese UI, four-book library. Ten launches with
`am force-stop` then `am start -W` (`TotalTime`, initial window): 447, 334, 338, 456, 372, 346, 360, 347, 355, 350 ms;
**median 352.5 ms**, min 334, max 456. The earlier single phone launch was 594 ms (2026-11-04); no regression after the
Batch R / v0.1 merges. Baseline profile re-collect (P2.4) still to do before the release tag.

