# Optimized preview runtime checks

Checkpoint: 2026-11-04. Test the optimized `preview` app (`…tsuzuki.debug`), using its authorized copy of the
four-book library. Original debug (`…tsuzuki.dev`) data is separate; its tablet baseline is Nano Machine
chapter 336. Keep test changes in preview. Both devices and temporary EPUB/Royal Road imports are authorized,
with backup/cleanup. No push or release-key/telemetry changes.

Local evidence: `app/build/codex-preview-20261104/`; filenames contain device, theme and flow. Earlier download
engine recovery checks used debug and are recorded separately in `docs/UPSTREAM.md` / `AGENTS.md`.

| Flow | Verified scope | Remaining |
|---|---|---|
| Profiles / packaging | Tablet full Home/Library/details/cached-reader/tabs collector and separate Home-ready collector pass; profiles packaged, seven ARM64 libraries and APK pass 16 KB checks | Large-library measurements |
| Cold startup | Tablet ten-launch compilation comparisons and frame metrics; Dot/Colour startup recordings | Phone repeated measurements; frame overruns remain. See `PERFORMANCE.md` |
| Home / combined Library / details | Both devices and themes used with four real books in optimized preview | Separate-library mode, search, large lists and empty/error cases |
| Novel WebView reader | Both devices/themes: cached content, compact toolbar title, progress semantics, reader settings fit and reversible +/- changes | Full renderer/rotation/process-death matrix |
| Contents / reader back chain | Phone contents, overflow, settings sheet and back chain checked | Repeat across remaining device/theme combinations |
| Manga reader | Tablet and phone online content; phone paged swipe 2 -> 3 and long-strip rendering. Original per-book default mode restored | Complete mode/rotation matrix |
| Reader progress | Dot dots / Colour thin track, both readers/devices/themes: taps, drag destination, current labels, return near the starting position (within two percentage points / one page), 48 dp themed touch height; phone RTL and tablet LTR | Vertical progress presentation remains stock |
| Legacy reader | Phone Default title fallback and stock progress presentation checked; RTL tap/drag seek returns exactly to page 3 | Broader stock-theme smoke test |
| TTS | Phone Dot reaches playing/highlight state, then pause/stop; copied controls-visibility preference restored to hidden | Other device/theme combinations, background playback. Highlight uses configured/default yellow even in Dot; assess explicit colour preferences before changing |
| Translation | Not yet exercised in optimized preview | Check configured provider and safe failure states; do not silently use paid credentials |
| Novel TextView renderer | Not yet exercised in optimized preview | Switch/revert renderer and check reading/settings/TTS |
| Downloads | Debug merge checks passed novel/manga concurrency, group pause/resume and force-stop recovery; interrupted split fixed | Repeat in optimized preview |
| Backup / restore | Debug tablet backup export; both preview copies restored successfully | Preview export/restore round trip |
| Sources | Phone Royal Road plugin copied and Asura upgraded in place from tablet; novel and manga open on both devices | In-app extension install/update and catalogue/global-search matrix |
| WebView / Cloudflare group | Three Mihon core files retained whole; safe-header tests pass. Both optimized devices pass forced renderer crash, page closure and fresh-page rendering. Both activity and navigator browser routes load real Asura/Royal Road pages and return on BACK (tablet Colour / phone Dot). Header lookup/getter runs on IO with readiness/failure tests. 634 app + 26 core tests pass | Live concurrent Cloudflare challenge not forced; download-provider work remains |
| Extension APK validation | Mihon installer port with retained Tsundoku adapters; 640 app tests pass. Both optimized devices read real APK certificates and reject a wrong repository key with the exact expected error; no real source installed/removed. Ordinary daily preview installed on both; Dot Home/screenshots/crash logs pass | Live successful system/private install/update matrix |
| EPUB / mass import | Owner approved temporary tests; prior debug probes did not queue imports | Optimized import/queue/retry/cleanup tests |
| Widget | Existing owner debug widget left intact | Optimized preview widget not placed or verified |
| Rotation / process death | Not yet systematically exercised in optimized preview | Both devices, readers, sheets and queued work |

Partial checks above do not mark the complete runtime sweep finished. Parked AppLock, Support and anime work
stay parked while daily-use stability, performance and usability take priority.
