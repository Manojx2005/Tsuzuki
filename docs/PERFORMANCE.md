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


## PERF1 S22 speed budgets (2026-11-14)

Accepted RV5 main `88bcf6e4e`, optimized non-debuggable Preview, scratch package
`io.github.manojx2005.tsuzuki.rv5debugscratch`, versionCode 24. APK SHA-256
`6f519bc92772f28e31f7ca5db8796dac2be497feaf48d6b85554c61a34dcea96`.
Galaxy S22 SM-S901E (`RZCWA139X1X`), Android 16/API 36, Japanese UI, Dot, portrait,
120 Hz, connected over adb wireless. Restored Y4: 500 books (250 novels, 250 manga),
30 shelves, 50,000 chapters and 995 history entries. Only synthetic scratch data was changed.

| Measurement | Result | Budget / verdict |
|---|---:|---|
| Cold initial display, 10 runs | median 741.2 ms; 701.2–885.1 ms | 500 ms: missed, PERF-START |
| Local Native next chapter, 3 video samples | approximately 180–190 ms to first readable incoming text | 200 ms: met in this limited fixture; close to measurement uncertainty |
| Library scrolling | 25 / 1,397 janky frames, 1.79% | under 5%: met |
| Cached details, 3 video samples | first readable title 49–115 ms (median 58); transition settles about 502–511 ms (median 510) | 300 ms: full transition missed, PERF-DETAILS |

Cold method: existing `ColdStartupBenchmark#startupCompilationBaselineProfiles`,
`CompilationMode.Partial(BaselineProfileMode.Require)`, `StartupMode.COLD`, 10 iterations;
standalone helper targets the explicit scratch `targetAppId`. Run passed, no thermal-throttle
sleep. Initial display samples (ms): 723.0, 766.8, 722.9, 760.6, 735.2, 701.2, 774.2,
746.1, 736.2, 885.1. The test also waits for populated Home, but the reported timing is
`timeToInitialDisplayMs`, not a Home-ready metric. This is a different method and dataset
from the older four-book `am start -W` results; it does not establish their regression size.

Scroll method: reset `dumpsys gfxinfo`, then 24 automated 400 ms swipes, six down/six up
repeated twice, one-second final idle; 13.34 seconds total. Default shelf 01 contains 17
books while all 500 are in the database. Three-column grid, generated placeholder covers.
Legacy jank counter was 34/1,397 (2.43%), CPU frame P95 8 ms. This measures the stated
500-book database with the normal selected shelf, not 500 simultaneously visible covers
or real-cover decoding.

Video method: six-second 1080x2340 screen recordings, first pressed/input-feedback frame
as time zero, inspect original variable-rate presentation timestamps. Frame spacing around
the transition was 8–16 ms. These are visual estimates, not instrumented touch-dispatch
latencies; recorder overhead and the reference frame add uncertainty. Chapter samples use
the cached two-chapter RV2 local fixture, Native renderer, infinite scrolling disabled,
menus and Listen dock closed. First readable chapter-2 text is about 180–190 ms after
feedback while the outgoing frame is still cross-fading. Full settling takes about 0.5 s;
the third recording also includes a later non-transition frame, so its final frame is not
the settling timestamp. Network/source fetches, WebView and larger chapters are unmeasured.
Details samples open cached `[SYNTHETIC] NOVEL 481` from shelf 01: metadata plus the
missing-source message, with no network wait. First title is early, but the approximately
0.5 s visual transition exceeds 300 ms; do not count first content as a full-open pass.

Evidence (ignored local): `app/build/review-evidence/perf1/`, including benchmark JSON and
all ten Perfetto traces in `benchmark-output/`, `s22-cold-baseline.log`, the generated backup,
`measure-scroll.py`, raw gfxinfo, `record-tap.py`, six videos, before/after screenshots,
frame timestamp lists and contact sheets. `show_touches` was restored to its original 0.
No production tuning was performed; budget misses are separate cards.

## PERF-START trace investigation (2026-11-14, pending device review)

All ten archived PERF1 cold traces were queried locally with the official Perfetto
v59.1 Windows trace processor (SHA-256
`3acc5aca1c764541033a82ba3bb0a47524b5c3c0aa194eea8d9a7b0cdb346bdc`).
No app, device setting or release artifact was changed for this analysis.

| Main-thread interval | Median | Min–max |
|---|---:|---:|
| Application binding | 391.2 ms | 366.4–448.3 ms |
| Opening app dex files, within application binding | 104.3 ms | 99.5–114.6 ms |
| Synchronous Chromium startup, within application binding | 140.5 ms | 128.9–150.2 ms |
| MainActivity performCreate | 32.4 ms | 24.7–64.3 ms |
| First UI traversal | 166.4 ms | 112.8–206.4 ms |

These are inclusive wall intervals, including scheduling and waits. Nested rows
must not be added. The first traversal contains direct-child Compose recomposition
(median 46.1 ms), measure (9.0 ms) and layout (25.2 ms); those are also inclusive.
The traces do not identify individual application composables, so attributing the
whole traversal to Home queries, fonts or a specific tile would be speculative.
Every trace reports one `power_rail_empty_packet` import error; power is not used
by this analysis.

Perfetto's `android_startups` system interval has median 747.0 ms (705.0–887.9),
which has different boundaries from Macrobenchmark's initial-display metric.
The benchmark's 741.2 ms remains the budget result. Neither number establishes
a regression from the older four-book `am start -W` dataset.

Reproduce the analysis from the repository root (paths below assume the local
PERF1 archive and the verified processor binary):

```powershell
python scripts/analyze_startup_traces.py `
  --processor app/build/review-evidence/perf-start/trace_processor_shell.exe `
  --traces app/build/review-evidence/perf1/benchmark-output `
  --package io.github.manojx2005.tsuzuki.rv5debugscratch `
  --output app/build/perf-start/trace-analysis.json
```

The script requires exactly one cold startup for the requested package per trace,
retains the raw main-thread slices and trace health diagnostics, and reports missing
intervals with a zero sample count rather than treating them as zero cost.

`StartupBenchmarks.kt` now requests a separate `TsuzukiHomeObserved` section metric:
from immediately before `startActivityAndWait()` until automation finds the seeded
English/Japanese All books action and the localized startup overlay is absent.
The section is in the benchmark process, so `targetPackageOnly = false` is required
([AndroidX API](https://developer.android.com/reference/kotlin/androidx/benchmark/macro/TraceSectionMetric)).
It includes shell/accessibility observation overhead and is **not** an app-internal
fully-drawn timestamp. Existing initial-display and frame metrics remain separate.
The old wait could find Home behind the startup overlay; its frame sample ended
after an idle wait, with no numeric Home-readiness metric.

Independent review must run the helper on the same S22 synthetic fixture and
optimized APK, verify one nonzero Home-observed section per iteration, and retain
the ten raw initial-display and Home-observed samples. First recapture the baseline
with this helper before comparing a production change. Compare compilation modes
on the identical APK if investigating the dex/app-image cost; do not trim shipped
profiles based on these inclusive intervals alone. Keep the documented main-thread
WebView warm-up before any source graph construction. Any safe reduction of the
remaining cost needs instrumented attribution and like-for-like before/after
measurements; **the 500 ms miss is unresolved and no speed-up is claimed**.

## PERF-DETAILS timing change (2026-11-14, pending independent review)

The archived MotionOpen HTML explicitly uses 450 ms for the cover and Colour
background. `TsuzukiSharedCover.kt` used 450 ms for both the shared cover and
the full-screen crossfade, with smooth motion in both themes (the owner's
2026-11-06 override of stepped Dot movement). This explains why the approximately
510 ms PERF1 settling estimate cannot meet a 300 ms total-open budget.

**Owner decision, 2026-11-14:** change the design to finish the entire opening
within 300 ms. The original budget is retained; first readable text alone is not
a pass. The opening animation now uses 200 ms for both the shared cover and
crossfade, reserving 100 ms for navigation/composition. This allowance is a design
choice, not a measured guarantee. Return navigation and other screens retain
their existing 450 ms motion; reduced motion remains a 150 ms fade, without a
shared cover. The screen's data and layout are unchanged.

Readiness definition for review: from the first visible input/pressed feedback
frame until both the incoming cached details cover and screen have reached their
final position/opacity and the outgoing Library is absent. Verify against the
final settled frame, allowing for the recorder's 8–16 ms frame intervals; report
first readable title separately. Metadata/source requests that continue after
the cached screen is displayed are outside this fixture's timing and must not
be silently included as a cached-data speed pass.

Use the same optimized Preview package and S22 Y4 500-book fixture as PERF1,
opening cached `[SYNTHETIC] NOVEL 481` from shelf 01. Retain at least three
before/after videos per theme, original variable-rate PTS and the exact APK hash.
Dot first, then Colour; include a real-cover shared transition in addition to
the synthetic placeholder fixture. Check interrupted opening/Back and reduced
motion, and inspect the tablet rail layout as a visual regression check. Restore
test settings and hand back Dot Home using the normal device lock protocol.

**Measured 2026-11-14 (independent review, S22, optimized scratch Preview of `260691fba`, APK SHA-256
`5321203d47a4d0ef772d135d17aba719cd91e090f50fca542bad762b3418abd4`, Y4 fixture, same `record-tap.py` as PERF1).**
Times are from the first frame that differs from the pre-tap frame; each endpoint is ±8 ms (frame interval).

| Theme | Run | First title | Settled | Last differing frame |
|---|---|---:|---:|---:|
| Dot | 1 (first open after launch) | 108 ms | 284 ms | 292 ms |
| Dot | 2 | 44 ms | 235 ms | 249 ms |
| Dot | 3 | 46 ms | 235 ms | 244 ms |
| Colour | 1 | 59 ms | 251 ms | 263 ms |
| Colour | 2 | 50 ms | 243 ms | 252 ms |
| Colour | 3 | 58 ms | 251 ms | 260 ms |

**Verdict: the 300 ms total-open budget is met in both themes** (PERF1 baseline 502–511 ms). The margin is thin
on the first open after launch: 292 ms with one 25 ms frame gap, which is inside the measurement error of the
budget line. Return navigation measured about 440 ms and looked smooth. Not measured: a real (non-placeholder)
cover, reduced motion, the tablet (layout looked normal there; no timing). Evidence (ignored local):
`app/build/review-evidence/perf-details/`.

## PERF-START-V: S22 Home-observed baseline (2026-11-14)

Optimized scratch Preview from main `9f1b87ced`, APK SHA-256
`db7f5496f7225dd582bef404a86f68f8c5f7cbd31c2e99ef393fc8b548eb0566`;
package `io.github.manojx2005.tsuzuki.rv5debugscratch`. S22 hardware serial RZCWA139X1X,
Dot, Japanese UI, 500-book Y4-derived scratch database retained from the prior reviews
(current Home resume card is RV2 listening fixture). Both comparison builds must retain this state.
`ColdStartupBenchmark#startupCompilationBaselineProfiles`, COLD, Partial/Require, ten runs,
no thermal-throttle sleep. The updated helper is from the same commit.

**Helper pass:** each run contains exactly one nonzero `TsuzukiHomeObserved` section. This
includes shell/accessibility/startActivityAndWait observer overhead; it is not app fully-drawn
or a direct measure of how long a reader sees the splash. No helper changes made.

| Run | Initial display (ms) | Home observed (ms) | Section count |
|---|---:|---:|---:|
| 1 | 686.407 | 3238.022 | 1 |
| 2 | 772.369 | 3161.579 | 1 |
| 3 | 697.819 | 3280.642 | 1 |
| 4 | 647.388 | 3187.202 | 1 |
| 5 | 732.731 | 3284.658 | 1 |
| 6 | 699.409 | 3302.669 | 1 |
| 7 | 745.431 | 3325.115 | 1 |
| 8 | 685.652 | 3254.836 | 1 |
| 9 | 703.812 | 3327.479 | 1 |
| 10 | 673.366 | 3333.075 | 1 |

Initial display median **698.614 ms**, range 647.388–772.369 ms
(span 124.981 ms). Home observed median **3282.650 ms**,
range 3161.579–3333.075 ms (span 171.495 ms). Initial display still misses 500 ms.
Evidence: `app/build/review-evidence/perf-start-v/` (benchmark JSON/log, hashes and Dot Home screenshot).
