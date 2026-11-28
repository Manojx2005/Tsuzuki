# Smoothness plan (Batch SM)

Owner, 2026-11-28: "Why does the app not feel as smooth as the Claude app, the Webtoon app or any other major
app?" — then: "Plan all of them in detail so Codex can help too, and start implementing."

This file is the plan. Card status lives in `docs/HANDOFF.md` ("Batch SM"). Read `AGENTS.md` first (build,
devices, upstream-file rules, commit rules).

## 1. What we know (measured, not guessed)

All numbers: Galaxy S22 (`RZCWA139X1X`, 120 Hz, 1080×2340), optimized release build installed as the
`claudeqa` scratch package (release signing = debug key, so it installs over the debug `claudeqa` and keeps its
test data), `cmd package compile -m speed-profile -f` before measuring. 120 Hz gives **8.3 ms per frame**.

| Finding | Evidence | Card |
|---|---|---|
| The shipped baseline profile is stale for Tsuzuki code: 47,557 rules, **347** mention `tsuzuki`, last collected 2026-11-04, one novel journey only. Anime, Watch, the player, the details redesign and most of the new Home are not covered. The app is sideloaded (GitHub), so there are no Play cloud profiles: this bundled profile is the only ahead-of-time compilation the owner gets. | `app/src/main/baselineProfiles/baseline-prof.txt`, `baseline-profile/.../BaselineProfileGenerator.kt` | SM1 |
| Opening book details builds the screen inside the opening animation: one 20–60 ms main-thread frame (Perfetto: `Recomposer:recompose` 19 ms, then `AndroidOwner:measureAndLayout` 39 ms on the first layout), i.e. 3–7 dropped frames as the motion starts. | `dumpsys gfxinfo framestats`, Perfetto trace 2026-11-28 | SM2, SM3 |
| Tab switches: 2–6 % janky frames. Book details open/back: 6–12 %. | `gfxinfo` per transition | SM2–SM5 |
| Background markdown parsing (`MarkdownRender immediate = false`) was A/B tested, 6 runs each: no measurable gain. Not shipped. | HANDOFF log 2026-11-28 | — |
| Data arrives after screens: anime episodes (fixed 2026-11-28, MEDIA-SLIDE/ANIME-POPIN), anime library "empty" flash (fixed). Others not audited yet. | S22 screen recordings | SM6 |
| Motion is not one system: navigator crossfade (now 300 ms, details open 180 ms), reader/player window animations (now 300 ms), tabs 200 ms fade-through, sheets 300/200 ms with their own curves; no predictive (gesture-driven) Back. | `TsuzukiMotion.kt`, `TsuzukiSharedCover.kt`, `res/anim/tsuzuki_reader_*`, `HomeScreen.kt` | SM4, SM5 |

## 2. How to measure (everyone uses the same tools) — card SM0

Scripts live in `scripts/perf/` (SM0). Never judge smoothness on a debug build.

- `scripts/perf/build-qa-release.sh` — builds the release APK with `applicationIdSuffix ".claudeqa"` (temporary
  edit, reverted by the script), installs on a serial, runs `cmd package compile -m speed-profile -f`.
- `scripts/perf/jank.sh <serial> <label> <action...>` — resets `gfxinfo`, performs one action (tap/key/swipe),
  prints frames, janky %, slow-UI count. Refuses to act unless Tsuzuki is in front (a stray Back once sent
  taps into system Settings — the guard is mandatory).
- `scripts/perf/frames.py <framestats.txt>` — per-frame list: offset, total ms, UI ms; prints the longest UI
  frame and UI ms in the first 150 ms.
- `scripts/perf/strip.sh <video> <start> <len> <fps> <out.png>` — contact sheet from a `screenrecord` file,
  for pop-in and motion review.
- Perfetto: `scripts/perf/trace.sh <serial> <seconds>` pushes a config (sched + atrace view/gfx/am/dalvik +
  app tracing), records while you act, pulls the trace; read it with Google's `trace_processor` launcher
  (`https://get.perfetto.dev/trace_processor`, fetches a pinned, SHA-checked binary; owner approved
  2026-11-28). Query: slices on the app main thread with `dur > 8e6`.

Report per journey: median and worst of **longest UI frame**, **UI ms in the first 150 ms**, **frames over
16.7 ms**, over ≥ 6 runs, before and after. Runs vary by ±50 %; a change smaller than that spread is not a
result (the markdown experiment).

Standard journeys (J1–J8), the same list SM1 profiles and SM8 gates:

| | Journey |
|---|---|
| J1 | Cold start to populated Home (Read) |
| J2 | Home tabs: Home → Library → Updates → Browse → More → Home |
| J3 | Library: scroll grid; open book details; Back |
| J4 | Library media switch: All → Novels → Manga → Anime → Novels |
| J5 | Anime: Watch → anime details (episodes visible) → Back |
| J6 | Reader: Home resume → reader → turn pages → Back |
| J7 | Player: episode → playing (picture moving) → Back |
| J8 | Sheets: Library filter sheet open/close; details ⋮ menu; Settings → Appearance → Back |

## 3. Cards

Lane: **C** = Claude (devices, design, upstream-sensitive), **X** = Codex (implementer; device use only under
the Device lock rule in HANDOFF), **C/X** = either. Each card ends with: commit on `tsuzuki/<card>`, Result in
HANDOFF (files, numbers before/after, evidence path), status `review`.

### SM0 — Measurement kit in the repo (C, done with this plan)
Goal: §2's scripts committed under `scripts/perf/` with a README, so Codex and Claude measure the same way.
Acceptance: scripts run from Git Bash on Windows; README lists J1–J8 and the report format.

### SM1 — Baseline profile that covers Tsuzuki (C, devices) — **first, biggest win per hour**
Why: §1 row 1. Every Tsuzuki screen added since 2026-11-04 runs interpreted/JIT on the owner's phone until
Android's idle dexopt, days later.
Do:
1. Extend `baseline-profile/src/main/kotlin/mihon/baselineprofile/BaselineProfileGenerator.kt` into J1–J8
   (keep the existing novel journey). Each journey is its own `@Test` method using the same `BaselineProfileRule`
   so one failing journey does not lose the rest; selectors by text in English or Japanese (as today), never
   coordinates. Anime journeys need a profileable build **with a real anime source** (the fixture source is
   debug-only): pass `profileAnimeTitle` / skip with a logged warning when absent.
2. Extend `StartupProfileGenerator` to wait for populated Home in both Read and Watch.
3. Collect on the S22 against the release-signed `claudeqa` (profileable, keeps the test library):
   `:baseline-profile:assembleNonMinifiedRelease`, `am instrument -w` with `targetAppId=…claudeqa`.
4. Merge with the existing rules (H/S/P flags union per descriptor, keep legacy-theme coverage), commit
   `baseline-prof.txt` + `startup-prof.txt`.
5. Verify the APK has `assets/dexopt/baseline.prof(m)`; measure J1–J8 with `cmd package compile --reset`
   then **no** `speed-profile` step (simulates a fresh GitHub install: only the bundled profile applies).
6. Add "re-collect the baseline profile if Tsuzuki screens changed" to the release gate (`docs/HANDOFF.md`
   release steps and `rel_gate`-style script in `scripts/perf/`).
Acceptance: Tsuzuki rule count ≥ 10× today; J3/J5/J6 longest-UI-frame median improves on a fresh install;
release gate step documented.

### SM2 — Start navigation motion after the next screen's first frame (C/X)
Why: §1 row 2 — the build cost lands inside the animation. Hiding it before motion starts removes the dropped
frames at the start of every push.
Where: `presentation/theme/tsuzuki/TsuzukiSharedCover.kt` (`TsuzukiNavigatorTransition`), `presentation/util/
Navigator.kt` (`ScreenTransition`). Approach: keep the outgoing screen fully visible while the target composes
and lays out its first frame off-motion; begin the fade/slide on the next frame. Options to evaluate:
(a) `AnimatedContent` with `fadeIn(tween(delayMillis = 1 frame))` + matching hold on `fadeOut` (cheap, keeps
Voyager); (b) a custom `ScreenTransition` that composes the target with `Modifier.alpha(0f)`, waits for
`withFrameNanos` after its first layout, then animates. Shared covers must still have their target on the
first motion frame (MotionOpen board; details open stays 180 ms total motion, owner 2026-11-14).
Acceptance: J3 open: no frame > 16.7 ms after motion starts (screen recording + framestats), total
tap-to-settled ≤ 300 ms (MotionOpen budget), Back unaffected. Tests for any pure timing helpers.

### SM3 — Make the heaviest first frames lighter (X, then C review on device)
Why: even hidden (SM2), a 60 ms build delays the response. Targets, trace-led (SM0 `trace.sh`), in order:
book details header (`presentation/manga/components/MangaInfoHeader.kt` dispatch → `presentation/tsuzuki/
details/*`: `TsuzukiGenreRows` composes every tag chip in a `SubcomposeLayout` each pass; blurred backdrop;
action tiles), anime details (`tsuzuki/anime/ui/AnimeDetailsScreen.kt`), reader settings sheets, Library grid
item. Techniques: defer below-the-fold parts one frame (`LaunchedEffect` + state), `remember` expensive
derivations, avoid `SubcomposeLayout` for chip rows (measure intrinsic widths once), image decode sizes.
Do not change layout or design; small upstream diffs only (AGENTS.md).
Acceptance: J3/J5 longest UI frame at the tap ≤ 16.7 ms median (release, S22), per-change before/after table.

### SM4 — Motion system board (C, owner approval required before SM5)
Why: §1 last row. One motion language is what makes major apps read as smooth.
Board (Claude Design, Dot + Colour, phone + tablet) covering: navigation push/pop as **shared-axis X**
(slide 30 dp + fade, 300 ms, emphasized-decelerate in / accelerate out), **predictive Back** (screen follows the
finger, scale 0.9 → release commits), tabs (keep 200 ms fade-through or shared-axis), sheets (single spring
spec), reader/player enter/exit matching navigation, reduced-motion fallbacks (150 ms fade), and where the
shared cover still flies. Must state exact durations/curves; update `docs/design/specs.md` §Motion.
Acceptance: owner approves the board; specs updated.

### SM5 — Implement the motion system (C/X after SM4)
Where: `TsuzukiMotion.kt` (tokens), `TsuzukiSharedCover.kt` / `Navigator.kt` (push/pop shared-axis),
predictive Back via Compose `PredictiveBackHandler` in the navigator host (`ui/main/MainActivity.kt` /
`HomeScreen.kt` tabs host — small hooks only) and `android:enableOnBackInvokedCallback="true"` (manifest),
`res/anim/tsuzuki_reader_*` (reader/player), sheets (`TsuzukiModalBottomSheet.kt`, `TsuzukiAdaptiveSheet.kt`).
Keep: MotionOpen shared cover, MotionTheme reveal, reduce-motion, Back key/gesture behaviour (BACK1 cases).
Acceptance: J2–J8 recordings match the board; BACK1 Back matrix passes on S22 + tablet in Dot and Colour;
gfxinfo no worse than before.

### SM6 — Pop-in audit and fixes (X; C reviews on device)
Why: "content pops in after" (owner). Fixed so far: anime details episodes, anime library empty flash.
Do: record J1–J8 (`strip.sh`, 20 fps) and list every element that appears or moves after the screen arrives
(covers, lists, counts, buttons, recap cards, badges). For each: load before show (pass already-loaded data, as
`savedAnime`), reserve the space (as the anime continue tile), or fade it in (≤ 150 ms). Never show "empty"
before the data source answered (null-initial `collectAsState`). Covers: check Coil crossfade/placeholder sizes
so the grid does not reflow.
Acceptance: a table screen → element → fix → before/after strip; no layout jump in J1–J8 strips.

### SM7 — Smooth scrolling of the big lists (X)
Library grid, Updates, History, episode lists, Browse results: `key` + `contentType` everywhere, stable item
classes (no lambdas/unstable lists recreated per item), image request sizes, no per-item `remember` of heavy
work. Measure with `jank.sh` on a 500-book fixture (PERF1's Y4 fixture) and a long episode list.
Acceptance: fling-scroll janky frames ≤ 2 % on S22 release for Library and episodes.

### SM8 — Keep it smooth: release gate (C)
Add to the release gate (`scripts/perf/`): fresh-install profile presence check, and J2/J3/J5 `jank.sh` medians
on the S22 with thresholds (initially: janky ≤ 8 %, longest UI frame ≤ 33 ms); a regression blocks the tag
unless the owner waives it. Document in HANDOFF release steps.

## 4. Order and parallelism

SM0 → **SM1** (Claude, now) → SM2 and SM3 in parallel (different files: navigator vs screens) → SM6/SM7 (Codex,
parallel, different screens) → SM4 board (Claude, can start any time; owner gate) → SM5 → SM8.
Ship SM1 (+ the merged 2026-11-28 fixes) as 0.2.4 as soon as it measures better; later cards ship as they pass.

## 5. Codex prompt (owner pastes)

`Read AGENTS.md, docs/HANDOFF.md ("Batch SM") and docs/SMOOTHNESS_PLAN.md. Take the first SM card marked ready
for your lane (X or C/X), measure with scripts/perf before and after on a release build, commit on
tsuzuki/<card>, fill the Result, set review. Device use only under the Device lock rule.`
