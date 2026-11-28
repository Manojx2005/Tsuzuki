# Performance kit (Batch SM, `docs/SMOOTHNESS_PLAN.md`)

Judge smoothness only on an **optimized release build**. Debug Compose is several times slower.
Devices and rules: `AGENTS.md` (Test on devices). Never install on the owner's phone.

| Script | Use |
|---|---|
| `build-qa-release.sh <serial> [--fresh]` | Release APK as the `claudeqa` scratch package (debug-key signed, so it installs over the debug `claudeqa` and keeps its test data). Default compiles `speed-profile`; `--fresh` resets compilation so only the bundled baseline profile applies (what a GitHub install gets). |
| `jank.sh <serial> <label> tap x y \| key code \| swipe x1 y1 x2 y2` | One action's frames: count, frames over 16.7 ms, longest UI-thread frame and when, UI ms in the first 150 ms. Stops unless `claudeqa` is in front. `SETTLE=2.5` for long actions. |
| `frames.py <framestats.txt> [--summary]` | The parser behind `jank.sh`; without `--summary` prints every frame `offset:total/ui` ms. |
| `strip.sh <video> <start> <len> <fps> <out.png> [cols]` | Contact sheet of a `screenrecord` file, for motion and pop-in. |
| `trace.sh <serial> start <s>` … `trace.sh <serial> pull <out>` | Perfetto with app tracing around an action. `long-slices.sql` lists main-thread slices over 8 ms. |

## Journeys (J1–J8)

J1 cold start → Home · J2 Home tabs round trip · J3 Library → book details → Back · J4 Library media switch
All → Novels → Manga → Anime → Novels · J5 Watch → anime details → Back · J6 Home resume → reader → pages → Back ·
J7 episode → playing → Back · J8 Library filter sheet, details ⋮ menu, Settings → Appearance → Back.

## Reporting

Per journey, ≥ 6 runs, before and after: median and worst of the longest UI frame, UI ms in the first 150 ms,
frames over 16.7 ms. Run-to-run spread is about ±50 %: a smaller change is not a result.
On a 120 Hz screen one frame is 8.3 ms.
