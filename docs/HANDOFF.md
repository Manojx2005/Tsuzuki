# Tsuzuki handoff — shared by Claude and Codex

The **only** handoff file. Read order for any agent: `AGENTS.md` (rules, build, devices) → this file (state,
queue, log) → `docs/WORKPLAN.md` (the plan and how work is split) → `docs/ARCHITECTURE.md` (where code lives) →
design docs for UI work.

Old handoffs (`docs/CODEX_PROMPT.md`, `docs/CLAUDE_HANDOFF.md`, the AGENTS.md task queue) were folded in here on
2026-11-05. Their full text is in git: `git show 8f7f12e3b:docs/CODEX_PROMPT.md`,
`git show 8f7f12e3b:docs/CLAUDE_HANDOFF.md`, `git show 8f7f12e3b:AGENTS.md`.

## How to use this file

- **Owner → Codex app:** paste `Read AGENTS.md and docs/HANDOFF.md, then do the next task in the Codex queue.`
- **Codex:** take the first card with status `ready`. Set it to `codex-working` with your worktree path and the
  date. Work only in that worktree. When done, fill **Result** (files + one line each, decisions, left out and
  why, test counts, what Claude must check on a device), commit on your card branch
  `tsuzuki/<card>` (owner as author, no trailers; message `<card>: <what>`), put the hash in Result, set `review`,
  stop. Never commit to `main` or another card's branch; don't push or use adb.
  If blocked, set `blocked` and write the question.
- **Claude:** reviews `review` cards (checklist in WORKPLAN §0.5), tests on devices, merges into `main` as the owner, moves
  the card to the log, writes the next cards. Claude also owns upstream merges and everything on devices.
  **From 2026-11-08 (owner): the Codex review lane does this job** (Batch T): it reviews, uses the tablet and
  S22, and merges into `main` locally. Implementer threads still never touch `main` or adb.
  **From 2026-11-12 (owner): anime-lane implementers (Batch AY and the A6 cards) test their own cards with adb**
  on the tablet and the S22, one thread at a time (device lock line in "Batch AY"). App-lane implementers still
  don't use adb.
- Statuses: `ready` · `codex-working` · `claude-working` · `review` · `blocked` · `done`.
- **Several Codex threads at once (owner may open more than one):** give each thread a card explicitly, e.g.
  `Read AGENTS.md and docs/HANDOFF.md, then do card P0.4 only.` A thread must re-read this file and claim its card
  (status `codex-working` + worktree path) *before* any other work, and must skip any card already claimed.
  Only cards marked `parallel-ok` may run beside another card; never run two cards that list the same files.
  At most two threads build with Gradle at the same time.
- Log entries: newest first, one short paragraph, with commit hashes and evidence paths.

---

## Plan 0.1.1 — reading first, anime later (owner 2026-11-12)

Owner 2026-11-13 (this chat): review RV2 then RV4, then implement the app queue; another chat reviews cards implemented here. Use the tablet only for now; S22 device checks are deferred. Hourly resume reminder disabled after owner stop on 2026-11-13.

Owner decisions 2026-11-12: (1) the next release is **0.1.1 with no anime**; (2) **the anime lane is paused**
(A6.4 may be committed as it is; no AY card starts until 0.1.1 is tagged); (3) **Codex does the reviews**
(review lane, Batch T rules); (4) Claude drew the missing boards (`docs/design/boards/fixes-2026-11b/`); they
are **approved by the owner (2026-11-12)**.

| Step | What | Who | Done when |
|---|---|---|---|
| 1 | **RV2** finish: completed: fixes committed and accepted cards reviewed | Codex review lane | R15, S1, S2, S3, R13 each have a verdict; passed ones are in `main` |
| 2 | **RV4**: V7.7-F, R12-F, S3-F, R14, CHAPNUM, TTS-LANGUAGE, MAIN-REPOS | Codex review lane | same |
| 3 | Fix cards from RV2/RV4: **S2-F**, **RP1**, **STR1**, plus any `-F` card a review adds | Codex app lane (one thread) | each is `review`, then **RV5** |
| 4 | Boards 2026-11b approved | Owner | **done 2026-11-12** |
| 5 | **ERR1** errors and long waits say what happened (after step 4) | Codex app lane | `review`, in RV5 |
| 6 | **RV5**: everything from steps 3 and 5 | Codex review lane | merged |
| 6b | **Neural TTS** (owner 2026-11-12: ship it in 0.1.1): NT1 review S4 → NT2 licence + speed + owner listening test (**go / no-go**) → NT3 engine in release builds + Voices page (board 5, approved 2026-11-13) → NT4 voice pack published by the owner | review lane, app lane, owner | merged, or a recorded no-go |
| 7 | **PERF1** speed budgets measured on `main` | Codex review lane (needs devices) | numbers in `docs/PERFORMANCE.md` |
| 8 | **REL-0.1.1**: changelog, version, preview APK on the tablet and S22 | Codex review lane prepares; **owner tags and publishes** | owner has the release candidate |
| 9 | Owner uses the release candidate for a week; notes become cards | Owner | — |
| 10 | Anime resumes: RV3 (chain review), then AY-PLAY, AY9, AY0 … | Codex | after the 0.1.1 tag |

Not in 0.1.1: anime (all of it stays behind its switch, unmerged), R13 dictionary packs if RV2 does not pass them (they can ship in 0.1.2).

**Prompts for the owner to paste:**
- Review lane: `Read AGENTS.md and docs/HANDOFF.md. You are the review lane: follow "Plan 0.1.1", start with RV2, then RV4.`
- App lane (one thread): `Read AGENTS.md and docs/HANDOFF.md. You are the app lane: follow "Plan 0.1.1", take S2-F, then RP1, then STR1, then ERR1, then NT2 and NT3 when they are ready.`
- Anime threads are allowed again since the 0.1.1 tag (2026-11-15); see "Anime restart" in Batch AY.

Owner 2026-11-14 (this chat): Resume ERR1, then investigate a neural TTS binary that satisfies the literal NT1 native-string rule. The owner reports the tablet connected by USB and the S22 available over wireless debugging. Independent review may use both; this app implementer still does not run device QA. This is permission to investigate, not a waiver of NT1, lexicon licensing or the owner listening go/no-go.

## Batch AP — anime parity for 0.2.2 (owner 2026-11-22) — start here

Owner: every feature the novel and manga tabs have must exist for anime, with the same design; plus the bugs he
listed (switch hard to find, no Read/Watch on Home, Aniyomi links, downloaded episodes don't play, notification
progress, no Library selection actions, no offline dictionary). **Full plan, file lists and acceptance:
`docs/ANIME_PARITY_PLAN.md`.** 0.2.1 (Kokoro + the Browse redesign) is being released by its own session; AP
cards target **0.2.2**. Codex has no usage right now; Claude self-reviews under the owner waiver until it returns.

| Card | What | Lane | Status |
|---|---|---|---|
| AP0 | Parity audit, anime vs manga on every surface → `docs/ANIME_PARITY_AUDIT.md` | Codex (devices, lock) | **`done`** (Claude review, merged `bb5015414`) |
| AP1 | Anime switch at Settings top level + More; Read/Watch always on Home with a turn-on sheet | Codex | **`done`** (Claude review, merged `9a6319829`) |
| AP2 | Aniyomi repo links: `aniyomi://add-repo`, folder/tree/blob URLs, visible parse error | Codex | **`done`** (Claude review, merged `7d18475ba`) |
| AP3 | Downloaded episodes play offline (mp4 + HLS), all entry points | Claude (devices) | **`done`** (`61c5b8b71`..`538d8fe71`, on `main`) |
| AP4 | Download notification: real progress, n of m, pause/cancel, errors; "Waiting for Wi-Fi" + setting (see AP3 result) | Codex | **`done`** (Claude review, merged `11940cf3c`) |
| AP5 | Library selection for anime + bottom action bar (shelves, watched, download, delete, remove, migrate) | Codex | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `tsuzuki/ap5` (`9579ae1eb` + merge of main `96f9af1f1`), Codex implementation finished by Claude |
| AP6 | Episode list = chapter list (selection bar, swipe, filter/sort/display sheet) | Claude (selection bar part, owner bug report) | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `tsuzuki/ap6` (selection bar + swipe); filter/sort/display sheet parity not done |
| AP7 | Details header/actions = manga details | Claude (took over) | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` (notes, cover viewer, next airing) on `tsuzuki/ap7` (on top of `tsuzuki/ap6`); header pixel match, edit info/cover not done |
| AP8 | Library display modes, sort/filter sheet, badges, shelf tabs | Claude (took over) | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `tsuzuki/ap8` (3 commits, on top of `tsuzuki/ap5`); all card items in; not all device-checked |
| AP9 | Updates + History rows/selection/actions = books | Claude (took over) | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `tsuzuki/ap9` (2 commits), `.worktrees/ap9` — device check pending (lock); needs a reviewer that is not Claude's implementing thread |
| AP10 | Catalogue grid/list, long-press add, global search | Claude (took over) | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `598bc833e` on `tsuzuki/ap10`, `.worktrees/ap10` — device check pending (lock); needs a reviewer that is not Claude's implementing thread |
| AP11 | Download queue + anime download/player settings design | Claude (took over) | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `tsuzuki/ap11` (queue controls + delete after watching); player settings page not done |
| AP12 | Import dictionary files and bundle English and Japanese offline packs (owner approved +29 MB) | Codex | **`done`** (Claude review, merged `66057b59f`) |
| AP13 | Follow-up cards AP-14 through AP-21 from the AP0 audit | Codex | after AP11 |
| AP-14 | Watch Home activity tiles | Claude (took over) | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `62eff9e63` on `tsuzuki/ap-14` (based on `tsuzuki/ap-21`) |
| AP-15 | Anime extension details | Claude (took over) | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `39d7d9ab9` on `tsuzuki/ap-15` |
| AP-16 | Continue Watching widget activity at larger sizes | Claude (took over) | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `80979e63a` on `tsuzuki/ap-16`; full gate passed; widget not device-checked |
| AP-17 | Preserve anime in backups while the feature is hidden | Claude (took over) | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `1dec69b2c` on `tsuzuki/ap-17` |
| AP-18 | Keep Anime Extensions reachable with zero repositories | Claude (took over) | **`done`** (`7903f9dd5`, independently accepted and merged locally at `c444c20b6`) |
| AP-19 | Anime counts and download bytes in Data and storage | Claude (took over) | **`done`** (`3fbe28a8b`, independently accepted by Codex review lane) |
| AP-20 | Count anime records in the restore preview | Claude (took over) | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` on `tsuzuki/ap-17` (second commit) |
| AP-21 | Watch activity chart in Stats | Claude (took over) | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` on `tsuzuki/ap-21` |
| RV-AP | Review + 0.2.2 release | Codex review lane (a thread that did not implement the card) | last |

**AP line merged into `main` (Claude, 2026-11-24, `c02aecf32`, no push):** `tsuzuki/ap-integration` = every AP card branch, the RV-AP fix branches, AP5 rollback, anime History cover / in-place details, AP12 cache serialize and AP11 safe delete. One conflict (`AnimeDownloads.remove`): kept AP9's `remove` / `removeAndAwait` / `removeEpisode`, with `removeEpisode` = `editForRemoval { if (it.episodeId == id) it.removing() else it }`. Gate on the merged tree: `assembleDebug`, `:data:verifySqlDelightMigration`, all-module `testDebugUnitTest` **1,605 tests, 0 failures**. Tablet (Colour, claudeqa, existing data kept: schema 40, 2 anime / 4 episodes / 2 history rows): History › cover → details in place; episode sheet has Filter / Sort / Display; Display › 話数 shows "第2話" only, back to ソースタイトル restores titles; Sort lists ソースに基づいた順 / 話数 / 公開日; Mark until last watched shown on Filter (disabled: both episodes watched); crash buffer empty. Not checked on device: range select (fixture has 2 episodes), migrate entry (one anime source installed), S22 (not connected). Still open for parity: AP7 header pixel match; AP9 tablet preview pane; anime rows in Updates › All long-press.

**S22 QA of `main` (Claude QA agent, 2026-11-24, claudeqa, Dot and Colour, evidence `.worktrees/qa-ap/evidence/s22-*`):** all six checks pass in both themes, crash buffer empty: Watch home tiles; Library › アニメ grid, sheet tabs, selection bar (本棚 / 視聴済みに / 保存 / 削除 / その他); details header and tiles, episode long-press bar, range select (3 件選択), filter sheet フィルター / ソート (ソースに基づいた順) / 表示 (話数 ↔ ソースタイトル), mark-until disabled correctly; History › アニメ tiles + cover → details → Back; Updates › アニメ empty state; catalogue ↔ details crossfade with cover morph. **Found:** (1) anime details inside `AnimeCatalogueActivity` are lost when the activity is recreated by rotation (also Back from the landscape player after a rotation) → card AP-PHONE-F1 (agent, `tsuzuki/ap-phone-f1`); (2) Library 削除 removes at once (Undo snackbar only) while books confirm with `DeleteLibraryMangaDialog` → same card; (3) phone episode list while selecting: check column squeezes title ("Episo…") and wraps the date, last row sits partly under the bottom bar (Colour: progress line drawn over the bar), bar labels truncate ("ブックマークに…") → AP-PHONE-F2 after AP7-HEADER lands (same files); (4) the episode sheet changes height between tabs; (5) English text in the すべて library filter sheet (TAGS, EXTENSIONS, Search Options…; upstream strings) and content-desc "List View" / "Custom Sources". History 連続 0 日 after a 10 s watch is correct (a day needs ≥ 1 min).

**AP9 History tiles + Updates preview (Claude, 2026-11-24, merged into `main` `45d36e93d` and `c35bc6ea5`, self-review on owner word):** (1) `b6b6a9f30` History › アニメ shows the book History tiles: 今日 (minutes from `anime_watch_days` today), エピソード (episodes last watched today), 連続 (days in a row with ≥ 1 min watched; `ReadingDays.streak`). `AnimeHistoryStatsTest` (3). Tablet Colour: 0 分 / 0 話 / 1 日 (2 min watched yesterday). (2) `711870ef9` tablet Updates › アニメ: list left, preview right like book `ChapterPreview` (`AnimeUpdatesPreview.kt`: cover, "プレビュー · title", episode, relative time, other new episodes of that anime, 観る/続きから · 視聴済みにする/未視聴に戻す · ダウンロード · アニメを開く); tapped row outlined; phones keep tap-to-play; new string `tsz_anime_open_anime` in 8 locales; `AnimeUpdatesPreviewTest` (2). Tablet with the fixture server (`--new-episodes`, case 05 added from the catalogue long-press): Colour and Dot screenshots `.worktrees/qa-ap/evidence/preview-2.png`, `preview-dot.png`; Mark watched flips to 未視聴に戻す and persists across restart; アニメを開く opens details in place, Back returns; crash buffer empty. Media pills wrap to two rows in the narrow list column (same as the book pane with four pills). Parallel work started 2026-11-24 (owner: "use multiple agents … both lanes"): AP7-HEADER (`tsuzuki/ap7-header`), AP9-ALL-SELECT (`tsuzuki/ap9-all-select`), S22 QA of `main` (S22 on wireless adb `192.168.40.243:37553`).

**RV-AP independent review started (Codex, 2026-11-23).** Local trial `.worktrees/rv-ap` includes Claude's integration, the episode wording and changelog branches, and the newer AP6/AP7 filter-sheet changes. Three independent code reviews cover AP5/AP8, AP6/AP7/AP9/AP10, and AP11/AP-16/AP-17/AP-19/AP-20. Codex review lane covers AP12 cache and AP-14/AP-15/AP-18/AP-21, and coordinates build and device evidence. Card verdicts are recorded below as reviews finish.

**RV-AP episode and catalogue review (Codex agent, 2026-11-23, source review).** AP6 `deeff6ec2`: tri-state callback and legacy filter bits reviewed. Range selection, Mark until last watched, source order and Display remain open. AP7 `b4a943f43` changes requested: save notes with a notes-only write and surface failure, since the optimistic UI currently launches an uncaught write that can race a whole-row refresh. Header parity remains open. Cover editing was deferred by the owner. AP9 `6d2355435` changes requested: count and queue only unwatched rows in both Updates views, and handle selection write failures without losing selection. The All multi-select, tablet preview, and History activity summary are still open. AP10 `b2232d8ef` changes requested: add/remove by cached ID so thin catalogue results cannot overwrite saved details. Migration and final phone toolbar checks remain open. All four branch diffs passed `git diff --check`. This review was source-only.

**RV-AP library review (Codex agent, 2026-11-23, source review).** AP5 `96f9af1f1` changes requested: switching tabs during the Undo window cancels Mark watched, and mixed removal can be interrupted between book and anime writes. Keep mutations in a persistent scope and handle partial writes. AP8 `340839d86` changes requested: match book filter meanings (Started includes completed playback, Completed uses publishing status). Add preference-aware badges to the Anime and All list rows and refresh source language metadata after extension changes. Verify tab-switch persistence and the full filter/badge matrix on devices. Both diffs passed `git diff --check`. Prior Dot and Colour evidence was inspected.

**RV-AP storage review (Codex agent, 2026-11-23, source review).** AP11 `525e20864` changes requested: periodic playback checkpoints can delete unread HLS segments at 85%, changing subtitle-group visibility looks like a watch transition, and Clear failed can remove a retried episode. Protect in-use files and make queue cleanup conditional on current status. AP-16 `80979e63a` changes requested: keep the third resume row when watch stats are unavailable, and format the card's long lines. AP-17/AP-20 `58f8ae422` changes requested: recognize a decoded anime shelf so a shelves-only export validates and previews. Verify the populated hidden export and restore on devices. AP-19 `3fbe28a8b` passed source review. Populated tablet Colour evidence shows 2 shows, 6 episodes and 8.6 MB. Populated Dot and S22 checks remain open. All four branch diffs passed `git diff --check`.

| Review fix card | Scope | Status |
|---|---|---|
| AP7-NOTES / AP10-METADATA | Agent implemented notes-only persistence with retry feedback and catalogue favorite/shelf updates by cached ID. Both database regressions passed. | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `7bb886df0`, `.worktrees/rv-ap-episode-fixes`, device verification pending |
| AP5-ACTIONS / AP8-FILTERS | Persistent selection actions, mixed-write compensation, equivalent filter meanings and preference-aware reactive badges. Cancellation and failed-Undo regressions passed. S22 Dot Mark watched completed after leaving Library during the Undo window (2/2 episodes). | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `6e1cda637`, `.worktrees/rv-ap-library-fixes`, mixed Undo and filter/badge matrix pending |
| AP16-WIDGET / AP20-SHELVES | Agent implemented both. Gzipped shelves-only validator regression passed. | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `47ceed711`, `.worktrees/rv-ap-storage-fixes`, device verification pending |
| AP11-SAFE-DELETE | Retain saved media during the player session. Persist deferred removal, recheck watched status before deleting, and clear only entries currently failed. Tablet Dot synthetic local 24 s HLS retained the same files while cleanup was pending and removed them on Back. | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `7caeab929`, `.worktrees/rv-ap-download-fix`, forced-death and S22 checks pending |
| AP9-UNWATCHED | Unwatched New and Save all, plus queue acknowledgement and selection retention on write failure. Count and eligibility regressions passed. | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `0d01a9940`, `.worktrees/rv-ap-episode-fixes`, device verification pending |
| AP21-WATCH-DATES | Inclusive range for Stats and same-day Home query. Both boundaries and same-day aggregation verified. | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `cb6b39d34`, `.worktrees/rv-ap-storage-fixes`, device verification pending |
| AP6-RANGE | Agent implemented nearest-boundary range selection in visible episode order and reused the calculation for manga. Added three focused regressions. | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `073b5d4cb` on `tsuzuki/ap-library-review-fixes` (finished by Claude; Claude source review passed 2026-11-24) |
| AP6-DISPLAY | Agent implemented reversible source ordering, title/number display and compatible flags. Added legacy and new flag tests. One DetailsScreen hook remains before the integrated build. | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `c18ce01db` on `tsuzuki/ap-storage-review-fixes` (finished by Claude; source review passed 2026-11-24) |
| AP6-MARK-UNTIL | Agent implemented the Filter action in playback order, with eight locale labels and a resume/bookmark/excluded-release database regression. | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `6635ead41` on `tsuzuki/ap-episode-review-fixes` (finished by Claude; source review passed 2026-11-24) |
| AP10-MIGRATE | Agent added an entry point for saved results when another source is available. Existing duplicate-add migration remains. Added eligibility coverage. | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `a4ca11da4` on `tsuzuki/ap-library-review-fixes` (finished by Claude; source review passed 2026-11-24) |
| AP12-CACHE-SERIALIZE | Recheck the verified bundled dictionary under the mutation lock so concurrent first lookups verify only once. | **`done`** — merged into `main` `c02aecf32` (2026-11-24, Claude self-review on owner word); was: `review` `87bbb1e5d` on `tsuzuki/ap12-cache-serialize` (finished by Claude) |
| AP11-SETTINGS (Claude) | Owner answer 1: anime player settings rebuilt as a standard `SearchableSettings` page (Playback, AniSkip, Subtitles, Audio, Video, Gestures groups); switches bind stored prefs, sliders/lists go through `AnimePlayerPreferences` (re-read before write); settings search indexes the real rows. `assembleDebug` + all-module tests passed. Separate from AP11-SAFE-DELETE (no overlap: only `AnimePlayerSettingsScreen.kt`, `AnimePlayerSettings.kt`/`AnimeSubtitleStyle.kt` visibility, `AnimeSettingsSearch.kt`). | **`done`** — self-reviewed on owner's word and merged into `main` locally as cherry-pick `7634cb3bb` (branch `tsuzuki/ap11-settings` from `main`), so `main` does not take the AP11 queue/delete commits Codex sent back. Merging `tsuzuki/ap11` later brings the same change again (`bfe3ac219`); expect it to apply cleanly or as a no-op. Original `bfe3ac219` on `tsuzuki/ap11`. Device (tablet, claudeqa, combined `tsuzuki/claude-qa-motion`): Colour and Dot layouts as Settings tiles in the side pane; background switch on then Position slider → both kept (`sub_background=true`, position 70); AniSkip Auto shows the 3 s delay slider. Evidence `.worktrees/qa-ap/evidence/ap11-*` |
| ANIME-MOTION (Claude) | Owner 2026-11-23: "animations for anime library opening and other stuff are different from novels and manhwa". Cause: anime details/catalogue, migration and source settings are Activities (system window animation) while book pages crossfade in the main navigator; catalogue ↔ details swapped instantly. Fix: `presentation/tsuzuki/anime/AnimeScreenMotion.kt` gives those windows a 450 ms Standard-curve crossfade (`tsuzuki_screen_fade_in/out`; reader fade with reduced motion; shared-axis in legacy themes), the player opens/closes with the book reader's `applyTsuzukiReaderTransition`, and catalogue ↔ details fades in (`AnimeScreenEnter`). No shared-cover morph (it cannot cross windows; would need details in the main navigator). Gate passed. Overlaps Codex AP10-MIGRATE only in `AnimeCatalogueActivity.kt` (two wrapped calls). | **`done`** — self-reviewed on owner's word and merged into `main` locally (no push). `a6bc74d35`..`9a691f818` on `tsuzuki/anime-motion`. Device (tablet Dot, screenrecord contact sheets `.worktrees/qa-ap/evidence/motion-*.png`): Browse → catalogue crossfades (no system slide); catalogue ↔ details: the catalogue stays composed and the details layer fades over it (`a6a79f0bd`; opaque in every theme, takes all touches, catalogue hidden from accessibility while open; Back keeps page and scroll like book Browse). Found on the way: `AnimeFilterList.equals` is always false, so `remember(..., appliedFilters)` rebuilt the pager and reloaded the grid on every recomposition (e.g. opening an anime) — now keyed by identity (`9a691f818`; each Apply stores a freshly loaded list). Final recording `motion-overlay-final.png`: open and back are true crossfades with the grid intact. Pager probe (temporary log, removed): one pager on catalogue load, none on open anime + Back, a new one on the Search tab and on filter Apply. Re-indents the catalogue/details block in `AnimeCatalogueActivity.kt` — expect a conflict with AP10-MIGRATE; keep both |
| PILL-SCROLL (Claude) | Owner 2026-11-23 screenshot (phone, Dot): anime global search filter pills (Pinned / All / Has results) drawn empty. `TsuzukiPill` label has `weight(1f, fill = false)` (Q8); inside a `horizontalScroll` row the width is unbounded and Row gives weighted children 0. Fix: `.width(IntrinsicSize.Max)` on the pill; bounded parents still cap it (ellipsis kept), fill/weight callers keep their width. Also affects the compact player chip row (`AnimePlayerContent.kt`). Gate passed. | **`done`** — self-reviewed on owner's word ("You do the review instead", 2026-11-23) and merged into `main` locally (no push). `7acdbac82` on `tsuzuki/pill-scroll`. Device (S22 Dot, claudeqa): anime global search shows ピン留め済み / すべて / 結果あり labelled, すべて selected (`pill-s22-dot.png`) |

**Claude code review of the RV-AP review-fix commits (2026-11-23, source review; build/tests per Codex `rv-ap-fixes-v3.log`).** All six pass:
- AP21-WATCH-DATES `cb6b39d34`: inclusive `BETWEEN :start AND :end`, both callers bound to today, chart resubscribes on `start`/`today` via `key`; test covers both boundaries.
- AP9-UNWATCHED `0d01a9940`: Save all / New count only unwatched; `awaitCommand` propagates queue-edit failures and cancellation; per-anime failures don't stop the rest; selection kept and snackbar shown on failure.
- AP11-SAFE-DELETE `7caeab929`: player-held episodes deferred via `pendingAutoDelete` (serialized with a default, so old queue files load; survives process death — init re-runs `removeWatched`); watched state rechecked before deleting; Clear failed only touches entries failed at edit time; no-op edits skip the file write.
- AP16-WIDGET / AP20-SHELVES `47ceed711`: stats row space reserved only when shown; shelves-only backups recognised (test).
- AP7-NOTES / AP10-METADATA `7bb886df0`: notes-only query (trigger test proves no favorite write), save errors shown in the dialog; catalogue add/remove only flips membership + shelves on the cached row.
- AP5-ACTIONS / AP8-FILTERS `6e1cda637`: Undo and actions in an Activity-scoped ViewModel survive tab switches; Remove compensates book↔anime partial writes; shared badge helper used in all three anime grids; Completed = source status like books. Nit (not blocking): Mark watched runs books + anime in one non-cancellable block but does not compensate if the anime write fails after books succeed (Remove does).
Device verification still as Codex listed per row.
**Anime History cover → details (Claude, 2026-11-23, `review`, branch `tsuzuki/anime-history-cover` = Codex's `tsuzuki/ap-episode-review-fixes` + `main` merged + `e5df5e474`):** as in book History, the anime History row's cover opens the details (in the main navigator, `AnimeDetailsRoute`); the rest of the row still resumes. The `main` merge resolves the conflicts the AP integration will meet: `AnimeDetailsHeader` takes both `onCoverClick` and `sharedCoverKey`; `AnimeDetailsScreen` passes both and keeps AP7's notes tile; `TsuzukiAnimeLibrary` keeps AP5/AP8's grid with `openAnimeInPlace` and the shared-cover key; `AnimeCatalogueItem` gets a `coverModifier` slot carrying `animeCatalogueCover`. Gate passed on the combined tree. Tablet: cover → details, Back → History, row → `AnimePlayerActivity`, Back → History; crash buffer empty. Not merged into `main` (it carries the unmerged AP9 line); merge after the AP branches.
**Same branch, `893ed11f5` + `0f08197ac`:** anime Updates rows and groups (both layouts), the Library All page and Open random entry also open details in place (`rememberAnimeDetailsOpener`, Activity fallback without a navigator). Gate passed. Tablet: All page → details inside MainActivity, Back → All page, crash buffer empty. Updates not exercised on device (no anime updates in claudeqa). Entry points that stay on the Activity: resume hero (resumes playback), global search results, catalogue duplicate dialogs, seasons and migration inside details.
**Same branch, `d6e7c138e`:** anime global search results (main Global search), season switches and the duplicate prompt's "open" inside details also use `rememberAnimeDetailsOpener` (in place within the main navigator; the Activity inside migration / the catalogue Activity). Gate passed. Tablet: Global search "timestamps" › Anime › result → details inside MainActivity, Back → results with the query kept; crash buffer empty.
**AP5 Mark-watched rollback (Claude, 2026-11-23, `review` `7437ed99d` on `tsuzuki/ap5-mark-rollback`, from Codex's `tsuzuki/ap-library-review-fixes`):** closes the nit from my RV-AP review. Mixed book + anime Mark watched/unwatched (`markLibrarySeen`) writes anime first from a snapshot (`AnimeLibraryStore.episodeProgress` / `restoreEpisodeProgress`: watched flag and resume position) and restores it exactly if the book write fails; the book failure is rethrown with any rollback failure suppressed. Undoing books instead would mark every chapter unread. Tests: `AnimeLibraryMarkTest` (order, rollback, suppressed rollback failure, anime-only) and `AnimeDatabaseTest.restoringEpisodeProgressPutsBackWatchedStateAndPosition` (40 s resume point survives an unwatch + restore). Gate passed. No device check (needs a forced book-write failure). Merge after `tsuzuki/ap-library-review-fixes`.
**AP21-WATCH-DATES device check (Claude, 2026-11-23, tablet Colour, claudeqa from the combined fix trial `tsuzuki/claude-qa-rvfix`):** after ~80 s of real playback today, Watch home "今日の視聴" = 2 分 (`ap21-watch-home-after`), Stats › 観る chart shows a bar only on today (月, 11/23), 今週 / 今月 / 累計 = 2 分 (`ap21-stats-watch.png`). Passes. **Owner 2026-11-23: "You can take over codex tasks completely"** — Claude now finishes Codex's codex-working cards (AP12-CACHE-SERIALIZE committed `87bbb1e5d`; AP6-DISPLAY, AP6-MARK-UNTIL, AP6-RANGE, AP10-MIGRATE next) and merges the AP line.
**Device slip (Claude, 2026-11-23, tablet R52Y90HA77L):** while checking History, a mis-aimed tap hit the rail and a Back left the test app; later taps landed in the **release** app `io.github.manojx2005.tsuzuki` 0.1.1 installed on the tablet (146-book library). They opened its More page, its "Update ready" screen (closed with Back, nothing installed) and History. Afterwards its More page shows Downloaded only **off** and Incognito **off**; nothing else looked changed, but a toggle may have been flipped and flipped back. Owner: worth a glance at that app. From now on Claude checks the foreground package before each tap.

**Anime Library opens details in place (Claude, 2026-11-23, `5163bb478` on `tsuzuki/anime-details-route`, merged into `main` locally on the owner's self-review word):** the anime Library grid and the Watch home row now push `presentation/tsuzuki/anime/AnimeDetailsRoute` (Voyager screen, `animeId`) instead of starting `AnimeCatalogueActivity`; the Library cover and the details cover share `tsuzukiSharedCover(key)` (new String overload; anime key `tsuzuki-anime-cover-<id>`, book key unchanged), so the cover moves like book Library → details. The route draws on the first frame when the source is loaded (Library primes the entry), otherwise loads; if the anime/source is missing it opens the activity (notice) and pops. Other entry points (Updates, History, All page, search, widget, resume hero) still use the activity. Gate passed. Tablet Colour: `library-morph-open-colour.png` / `library-morph-back-colour.png`; play from in-place details opens `AnimePlayerActivity`, Back returns to details, Back again to Library; crash buffer empty. Merge notes: Codex AP5/AP8 (`TsuzukiAnimeLibrary.kt` grid/covers) and AP7 (`AnimeDetailsHeader` `onCoverClick`) touch the same lines — keep both (`AnimeDetailsHeader` gets both `sharedCoverKey` and `onCoverClick`).

**Anime cover morph (Claude, 2026-11-23, `ff2cb2411` on `tsuzuki/anime-cover-morph`, merged into `main` locally on the owner's self-review word):** the catalogue and its details share a `SharedTransitionLayout` (`AnimeSharedCovers`); the tapped catalogue cover (`sharedElementWithCallerManagedVisibility`, hidden while its details show) moves into the details cover (`sharedElement` in the overlay's `AnimatedVisibility` scope) and back, 450 ms Standard like book Browse → details. Off in legacy themes and with reduced motion; no-op where there is no catalogue (anime opened from Library, "source missing" notice). Gate passed. Tablet recordings `cover-morph-open-dot.png`, `cover-morph-back-dot.png`, `cover-morph-open-colour.png`; crash buffer empty. Library → details still crossfades without a morph (different Activity windows).

**Merged main on the S22 (Claude, 2026-11-23):** anime catalogue ↔ details crossfade on the phone layout, grid intact both ways (`s22-motion-main.png`). Found there: details flashed "エピソードが見つかりません" for a frame or two while episodes loaded (the episode flow started as an empty list and `refreshing` was already false). Fixed in `cbaba58b8` on `tsuzuki/anime-episodes-flash` (null = not loaded; empty line only after the database answers; a truly empty anime still shows it); gate passed; S22 recording `s22-episodes-no-flash.png` shows no flash; self-reviewed and merged into `main` locally. Codex AP6 fixes touching `AnimeDetailsScreen.kt` may need a small merge around `episodeState`.

**Claude merge into `main` (2026-11-23, owner: "You do the review instead"):** PILL-SCROLL, AP11-SETTINGS (cherry-pick) and ANIME-MOTION re-reviewed against `main` and merged: `8301ade73`. Trial of the same merges (`tsuzuki/claude-merge-trial`): `assembleDebug` + all-module tests passed. AP10-MIGRATE / Codex's catalogue fixes will conflict in `AnimeCatalogueActivity.kt` (catalogue now sits in a `Box` under `AnimeScreenOverlay`; `this` inside it is `this@AnimeCatalogueActivity`) — keep both.

**Merge note (Claude, 2026-11-23):** combining the four fix branches (`tsuzuki/ap-episode-review-fixes`, `-library-`, `ap11-safe-delete`, `-storage-`) conflicts once, in `AnimeDownloads.remove`: AP9 split it into `remove`/`removeAndAwait`/`removeEpisode`, AP11 changed its body to `editForRemoval`. Resolution: keep AP9's three functions with `removeEpisode` = `editForRemoval { entry -> if (entry.episodeId == id) entry.removing() else entry }`. Combined trial `tsuzuki/claude-qa-rvfix` (`cc51b1cb4`, throwaway): `assembleDebug` + all-module tests passed. Device (tablet Dot, claudeqa from that trial): AP7-NOTES — added fixture case 11 to Default, saved note "rvap-note-check" via ⋮ › ノートを編集; shown at once, still "ライブラリ内"; after force-stop + relaunch the note is still there when opened from Library (`rvap-notes-after-restart.png`); crash buffer empty.

**AP11-SAFE-DELETE follow-up review (Codex agent, 2026-11-23).** The proposed patch serializes Failed → Removing, uses raw watched state, and keeps local files until the player closes. Before marking a deferred episode for deletion, recheck watched status and make sure the download was already saved when the transition was observed. Forced process death currently loses pending automatic cleanup. Source review only.

**RV-AP Extensions verdicts (Codex, 2026-11-23).** AP-18 passed: with zero anime repositories, both Tab S11 and S22 show the Anime Extensions tab, existing installed APK rows and the toolbar's Add repository route in Dot and Colour. The inline no-results link was not needed because both devices have installed rows. AP-15: verified sheet opening, version, package and Trust affordance for shared untrusted extensions in both themes on both devices. Trusted extension sources/settings and update or uninstall actions remain unverified. Integrated baseline `d86ebdcf8`: `assembleDebug`, SQLDelight migration verification and 1,587 all-module tests passed. Evidence `.worktrees/rv-ap/app/build/review-evidence/rv-ap/`.

**RV-AP fixes build (Codex, 2026-11-23).** `rv-ap-fixes-v3.log`: `assembleDebug`, SQLDelight migration verification and 1,593 tests passed. AP11 tablet evidence `hls-preserved-while-pending.json`, `hls-player.log`, `dot-hls-removed-after-back.png` and the empty queue after Back. AP5 S22 evidence `dot-ap5-after-tab-switch.png`. Both used the separate `rvapscratch` package.

**AP-19 independent acceptance (Codex, 2026-11-23).** The scratch Data and storage page reports one favorited anime and two episodes in Dot and Colour on Tab S11 and S22. The saved fixture contributes 3.3 MB on the tablet and 5.3 MB on the S22. Measurements follow the existing on-entry behavior, so the tablet's open page refreshed after reopening. Screenshots `dot-ap19-storage-reopen.png`, `dot-ap19-storage.png` and `colour-ap19-storage.png` in the per-device RV-AP evidence folders.

**AP0 Result (Codex, 2026-11-22, `4e7d60608`):** `docs/ANIME_PARITY_AUDIT.md` maps 67 differences to existing cards or AP-14 through AP-21. The adjacent evidence directory contains 185 screenshots. Baseline `5226188d3` built as `…ap0scratch`; 1,560 all-module and 9 AY0 tests passed. Tablet and wireless S22 in Dot and Colour with three manga and three anime titles, private AnimeKai 16.13 (1,180 One Piece episodes), rendered widgets, and an export/restore probe. Both devices reproduced hidden anime data in export and missing anime counts in the restore preview. Reviewer: confirm the evidence and follow-up assignments. Local manga was excluded from the paired remote migration check, and tracking was signed out. Raw evidence and temporary probes live in `.worktrees/ap0/app/build/review-evidence/ap0/`. Both scratch packages and fixture mappings removed from both devices, no scratch crash entries, device lock released.

**AP1 Result (Codex, 2026-11-22, `ad83a64d6`):** Home always shows Read/Watch. Settings has an Anime switch using the shared row beside Browse, the More tile links to the Anime setting when off, and onboarding has an optional toggle. The Watch sheet uses the current adaptive sheet and theme buttons. Scoped Spotless and 1,561 all-module unit tests passed. Galaxy Tab S11 and wireless S22 passed in Dot then Colour, including screenshots of the final Settings header. Follow-up `073b9477b` adds a themed translucency fade to pinned Settings titles, preference pages and the phone Upcoming overlay. Scroll checks in both themes on both devices, scoped Spotless and all-module suite passed. Scratch apps removed. Evidence: `.worktrees/ap1/app/build/review-evidence/ap1/`.

**AP2 Result (Codex, 2026-11-22, `75679d3ce`):** Aniyomi and Animetail add-repo intents, and the Tsuzuki/Tachiyomi anime-repo hosts, open a prefilled confirmation even with Anime off. Repository folders and GitHub tree/blob/raw URLs resolve to their canonical HTTPS endpoints. Compact and expanded indexes work and bad links show inline errors. Scoped Spotless and 1,565 all-module tests passed. Tablet and wireless S22 checked in Dot then Colour with live Yuzono compact and Secozzi expanded indexes, cancel and bad-link validation. Scratch packages removed. Evidence: `.worktrees/ap2/app/build/review-evidence/ap2/`.

**AP3 result (Claude, 2026-11-22):** fixture mp4 and HLS downloads played offline fine; a **live AnimeKai** download (Frieren ep 1, 377 MB HLS, 341 segments) loaded and then failed with `error reading packet: Invalid argument`. Every saved segment began with a 1×1 PNG (`89 50 4E 47…`) in front of the MPEG-TS stream: an anti-hotlinking trick that the online stream survives through the source's mpv demuxer options, which a download does not keep. Fix: `tsuzuki/anime/download/HlsSegmentPrefix.kt` strips a PNG/JPEG/GIF prefix up to the first TS sync pattern (0x47 at three 188-byte steps) when each segment is saved, and `AnimeDownloads.offlineVideo` cleans episodes saved earlier once (marker `.segments-clean`). Tablet: the same 377 MB download now plays (PLAYING, position 13→15 s, no read errors). Tests: `HlsSegmentPrefixTest` (3) + `AnimeDownloadTest` updated; all-module suite passed otherwise. **Also found for AP4:** anime downloads default to **Wi-Fi only** (`AnimeDownloadQueue.wifiOnly = true`), so on mobile data they stay queued forever while the notification just says "Downloading" — AP4 must show "Waiting for Wi-Fi" and offer the setting. Evidence/probe `.worktrees/ap3/app/build/review-evidence/ap3/`.

**AP4 Result (Codex, 2026-11-22, `037e1f077`):** Anime notification reports MP4 bytes or HLS segments, position in the batch, Pause/Resume, Cancel, failure with Retry, and completion counts. Settings › Downloads adds the Anime Wi-Fi-only switch. Scoped Spotless and 1,562 all-module tests passed. AY0 fixture on the tablet and wireless S22, Dot then Colour, verified determinate progress, pause/resume, failure/retry, completion, and cancel preserving saved episodes. Both devices verified the switch next to the existing book option and retained its value across theme changes. Waiting for Wi-Fi covered in unit tests on synthetic network states. Notification permission granted only for AP4 scratch; both scratch packages and fixture mappings removed. Evidence: `.worktrees/ap4/app/build/review-evidence/ap4/`.

**AP12 Result (Codex, 2026-11-22, `382821c96`):** Settings › Novel Reader › Dictionary Packs accepts a pack and JSON manifest. Size, SHA-256, SQLite schema and version pass before replacing the installed dictionary. English WordNet and Japanese JMdict ship locally with the owner-approved +29 MB (28,963,435 compressed data bytes) and their notices. Both packs verified in the APK and in Git. Scoped Spotless and 1,562 all-module tests passed. Tablet and wireless S22 in Dot then Colour checked the Settings layout and device probes for English/Japanese lookup, valid import, bad checksum preserving the old pack, and deleting an override returning to the bundled pack. Android document selection was not tapped. Scratch apps removed. Evidence: `.worktrees/ap12/app/build/review-evidence/ap12/`.

**Claude review 2026-11-22 (AP0, AP1, AP2, AP4, AP12):** code read in full; all five merged into `main` (`bb5015414`..`66057b59f`, README row conflicts kept both rows). Checked: AP2 link screen opens with Anime off only through a link, expanded-index APK URLs must be HTTPS; AP4 PendingIntents differ by action, deferred notification follows `TsuzukiAnime.enabled` via `snapshotFlow`; AP1 Settings row toggles in place, More tile only while off; AP12 manifest size/SHA/schema/version checks before rename, gzip detected by magic bytes. Codex's device evidence accepted (devices held by AP5 during this review). Merged main: `assembleDebug` and all-module `testDebugUnitTest` passed (`build-review-ap.log`). Device recheck of the merged build waits for the AP5 Device lock.

**AP9 Result (Claude, 2026-11-22, `e54fc54ab` on `tsuzuki/ap9`; owner said take over Codex work, so committed on the card branch, not `main`):** Anime Updates: book-style 56 dp row (`AnimeUpdatesSelection.kt`: square cover, unwatched dot, bookmark, resume time, download indicator with queue/saved/retry), long-press selection with select all / invert, the shared `MangaBottomActionMenu` (bookmark, mark watched/unwatched with per-anime tracker sync, download/retry, delete download), and the New + Save all tiles (`TsuzukiUpdatesTiles` gained an `unit` slot: "ep"). Updates › All anime rows use the same tile. Query `getLibraryUpdates` returns seen/bookmark/progress/source/cover (no migration). Anime History: row matches the book row (poster cover, bold title, episode · time left · time, dotted progress, close button); the Anime pill now has Clear history with a confirmation (clears `last_seen` only; progress stays). Upstream touch: `HistoryScreen.kt` one dispatch. Follow-up `AP9: Last only toggle` adds the book History List view / Last only toggle to the Anime pill (same setting). Book Updates rows have no swipe actions either (swipe lives in the details chapter list, AP6), so none were added. Not done: anime activity tiles, tablet preview pane, and anime rows in Updates › All have no long-press (a mixed book+anime selection would need the upstream `UpdatesViewModel`). Gates on the branch: all-module `assembleDebug` + `testDebugUnitTest` passed (`.worktrees/ap9/ap9-full.log`), same for AP10 and the AP12 follow-up. Tests: `AnimeUpdatesSelectionTest` (3) + updates tests; compile, scoped Spotless, `assembleDebug` pass.

**AP10 Result (Claude, 2026-11-22, `598bc833e` on `tsuzuki/ap10`):** catalogue toolbar: Filter in every mode (the duplicate button in the Search row is gone), Display menu (comfortable / compact / list / cover-only, the shared `sourceDisplayMode` preference), WebView for `AnimeHttpSource.baseUrl`, source settings. Long-press: not saved → cache, duplicate check (`AnimeDuplicateDialog`), shelf picker, `setFavorite`; saved → confirm remove. Saved results show the In library badge and a dimmed cover. Thin catalogue results are safe to save: `saveAnime` keeps `initialized` and only takes `fetchType` from an initialized result. Browse › Anime sources toolbar has Global search, opening on the Anime pill (one-shot `AnimeGlobalSearch.openAnimeFirst()`). `AnimeShelfPicker` made `internal` with the same line as AP5 (clean merge). Upstream touch: `BrowseTab.kt` one argument. Follow-up `b2232d8ef`: WebView and source settings moved into the standard `AppBarActions` overflow (Filter and Display stay icons), so the title keeps room on phones; full gate passed. Not done: migrate entry point; pinned/latest shortcuts not re-verified; phone layout not device-checked (S22 offline). Tests: `AnimeCatalogueParityTest` (2).

**AP12 follow-up (Claude, 2026-11-22, `865bed4a0` on `tsuzuki/ap12-cache`):** `DictionaryPacks` remembers verified bundled packs per process, so a lookup no longer re-reads the manifest and reopens SQLite each time. Needs review.

**KOKORO-GAP (branch `tsuzuki/kokoro-gap`, `4befac288` + `e7bb16a4c`, from the Kokoro 0.2.1 session; not in this table before):** Claude read the diff 2026-11-22: Kokoro now plays every chunk sentence by sentence with a producer that keeps up to `NEURAL_LOOKAHEAD` = 2 finished pieces waiting (plus one in synthesis); a failed piece is delivered in order; the only timeouts in the path are `withTimeoutOrNull`, so no stray cancellation can leave the consumer waiting. Code looks right. **Not merged:** the branch also bumps the app to 0.2.2 (28) with a changelog entry, which collides with the Batch AP 0.2.2 release (RV-AP). Owner decides: ship KOKORO-GAP alone as 0.2.2 now, or merge only `4befac288` and let RV-AP do the version.

**AP11 Result (Claude, 2026-11-22, `2d376b683` on `tsuzuki/ap11`, queue part only):** anime queue gets Pause/Resume all, Retry failed and Clear failed pills (shown only when they apply) and a Move to top / Move to bottom menu on queued episodes (`AnimeDownloadQueue.move` / `retryFailed`; downloads run in list order). Settings › Downloads already has the anime Wi-Fi switch (AP4). Follow-up `571b7b36c`: Settings › Downloads › Delete chapters gets an anime **Delete after watching** switch (off by default, `tsuzuki_anime_delete_after_watched`). `AnimeDownloads` watches the seen state of saved anime; each anime's first look is a baseline, so episodes already watched (also ones downloaded later) are never deleted; only episodes that turn watched while saved are removed. Not device-checked. Not done: parallel-download setting, the player settings page redesign. Tests: `AnimeDownloadQueueOrderTest` (3).

**Device check AP9 + AP10 + AP11 (Claude, 2026-11-22, owner said use the devices; tablet only, Dot):** QA merge `tsuzuki/claude-qa-ap` (ap9+ap10+ap11) as `.dev` with the AY0 fixture (`serve.py --new-episodes`). Passed: Browse › Anime Global search opens on the Anime pill; catalogue toolbar Filter / Display / WebView / Settings; display menu (compact grid, list); long-press add → shelf picker → In library badge + dimmed cover; long-press saved title → remove confirmation; Updates › Anime: New "3 話" + Save all tiles, book-style rows with unwatched dot and download buttons, long-press selection (count, Select all, Invert, bar: mark read / save / bookmark), Select all → Save queued and downloaded all three; queue: Pause pill while queued, Retry failed / Clear failed after case 12 failed, Clear failed removed it. Not caught on device: Move to top (fixture downloads finished first; unit-tested), anime History rows (no watch history in the test app), Colour theme. **S22 dropped off wireless adb** (not discoverable), so the phone toolbar-crowding check is still open. Crash buffer clean. Evidence `.worktrees/qa-ap/evidence/`. **Incidents:** (1) after a force-stop, `monkey` failed silently and a few Updates taps (Anime pill, Update library) landed in Codex's `ap5scratch` app; harmless, but its anime library update ran. (2) Claude killed a lingering `serve.py` that turned out to be **Codex's AP5 fixture server** (it held port 8765); AP5 must restart it if it resumes. The shared selection bar shows the book labels ("mark as read") for episodes.

**AP-17 Result (Claude, 2026-11-22, `1dec69b2c` on `tsuzuki/ap-17`):** new `TsuzukiAnime.inBackups` = Anime on, or the device supports anime (an unknown ABI counts as unsupported, so JVM tests behave as before). Backup creation, restore, the validator, the Anime backup/restore option rows and anime source preferences now use it instead of `TsuzukiAnime.enabled`, so a backup made with Anime hidden keeps the anime library and restoring it with Anime hidden stores it (still hidden). The Anime checkbox in the options remains the explicit choice (default on, as before). One-condition edits in the already-listed backup adapters. Full gate passed. Not device-checked (AP0's export/restore probe is the acceptance test to re-run). No new unit test: `supported` reads `Build.SUPPORTED_ABIS`, which the JVM tests cannot set.

**AP-20 Result (Claude, 2026-11-22, second commit on `tsuzuki/ap-17`):** `BackupFileValidator.Contents` gains `anime: AnimeContents(entries, shelves, historyEntries)`, counted per anime backup format and reported for the format `AnimeBackupSummary` picked; `TsuzukiRestorePreview` adds a second row (Anime / Anime shelves / history entries) only when the backup contains anime. Book counts, missing-source and tracker checks unchanged. Full gate passed. The anime branch only runs when `TsuzukiAnime.inBackups`, which JVM tests can't enable, so it is not unit-tested; re-run AP0's export/restore probe on a device.

**AP-18 Result (Claude, 2026-11-22, `7903f9dd5` on `tsuzuki/ap-18`):** `animeBrowseSectionAvailable` always offers Extensions while Anime is on; with no repository the empty list adds a button to the anime repository screen. Tests updated (`BrowseHeaderSectionsTest`); full gate passed. Tablet (fresh `.dev`, zero repos, Dot): Browse › Anime shows Sources + Extensions; Extensions lists the shared installed extensions (AniDB 16.13, Anikage 14.10, …) with Trust (not tapped). The empty-state button was not seen on device because installed rows existed. `.dev` uninstalled, crash buffer clean.

**AP-19 Result (Claude, 2026-11-22, `3fbe28a8b` on `tsuzuki/ap-19`):** `StorageBreakdown` gains `anime` / `episodes`; while Anime is on, Settings › Data and storage shows Anime and Episodes tiles (library size and the sum of `totalCount`) under the book tiles, and Downloads adds the bytes of saved episodes from the anime queue (anime saves outside the book downloads folder). Full gate passed. Tablet (fresh `.dev`, Dot): the second row shows ANIME 0 / EPISODES 0 beside the book tiles; real counts and bytes not seen on device (empty library). `.dev` uninstalled, crash buffer clean.

**Owner report 2026-11-23: "no options on the bottom when I tap-hold and select an anime episode."** Reproduced on the S22: anime details selection showed only a top ⋮ menu. **AP6 (selection-bar part) `80e140781` on `tsuzuki/ap6`:** details now show the shared `MangaBottomActionMenu` (bookmark, mark watched/unwatched, mark previous watched, download/retry, delete download); the top bar has Select all and Invert; the header Save selects downloadable episodes. `AnimeShelfPicker` line untouched. S22 (scratch `…claudeqa`, built with a Gradle init script that sets the debug applicationId, QA merge of AP6+AP9+AP10+AP11): bar visible above the gesture bar, Mark as read → 1/2 watched, Save → episode downloaded; crash buffer clean. Labels are the book ones ("mark as read", "bookmark chapter"). Follow-up `332872726`: episode rows swipe like book chapters (`AnimeEpisodeSwipe.kt`, Library settings' swipe left/right: toggle watched, toggle bookmark, download / retry / delete), off while selecting. S22: swipe right bookmarked episode 2, swipe left marked it watched (1 / 2); crash buffer clean. Not done in AP6: filter/sort/display sheet parity. Also **AP9 `6d2355435`**: anime Updates hides the phone bottom nav while selecting (as book Updates), so its bar isn't covered — not re-checked on the phone (needs new episodes). **AP10 phone check:** catalogue toolbar fits (title truncated, Filter, Display, ⋮). Full gates on `tsuzuki/ap6` and `tsuzuki/ap9` passed.

**AP-21 Result (Claude, 2026-11-23, `tsuzuki/ap-21`):** Stats › Watch adds a chart tile above the watch totals: the reading screen's `Chart` (made `internal`) fed by `ReadingChart.bars` with watch seconds per day from the new read-only query `anime_watch_days.getWatchDays` (`AnimeHistoryRepository.subscribeWatchDays`, default `flowOf(emptyMap())` for fakes). It follows the Week / Month / Year selector of the reading chart and shows the period total. Watch totals unchanged. Full gate incl. `verifySqlDelightMigration` passed. Not device-checked: the Watch section only appears with recorded watch time (needs playback on a device).

**AP-15 Result (Claude, 2026-11-23, `39d7d9ab9` on `tsuzuki/ap-15`):** tapping an installed anime extension row opens `AnimeExtensionDetailsSheet` (adaptive sheet): name, version · language, package, its sources with a settings button where the source has settings, and the row's actions (Update / Trust, Uninstall when removable). Layout follows the book extension details content in the existing sheet components (no separate board). Full gate passed. Tablet (QA merge with AP-18, zero repos, Dot): tapping AniDB opened the sheet with name, 16.13, package and Trust (not tapped); untrusted shared extensions show no sources and no Uninstall, as expected. Trusted extension with sources/settings not seen on device. `.dev` uninstalled, crash buffer clean.

**AP-14 Result (Claude, 2026-11-23, `62eff9e63` on `tsuzuki/ap-14`, branched from `tsuzuki/ap-21` for `subscribeWatchDays`):** Watch home adds the AnimeHome board's two tiles under the airing strip while the anime library isn't empty: **New episodes** (new episodes of the last three months, the count Updates › Anime uses, in the accent, "in N shows") and **Watched today** (minutes recorded today). `WatchActivityTiles.kt`, strings in 8 locales, `WatchActivityTilesTest`. Full gate passed. Tablet (Dot): tiles render as on the board (0 / 0 shows, 0 min with fresh data). Non-zero values not seen on device. `.dev` uninstalled, crash buffer clean.

**AP5 finished (Claude, 2026-11-23; owner: "you can even finish half-done Codex cards"):** Codex's uncommitted AP5 in `.worktrees/ap5` (selection toolbar + bottom bar for anime library and mixed All selections: shelves, watched, download next 1/5/10/unwatched/all, delete downloads, remove with Undo, migrate; `AnimeLibraryActions.kt`, `AnimeLibrarySelection.kt`, `AnimeLibrarySelectionTest`) committed as `9579ae1eb` on `tsuzuki/ap5` (owner sole author), without the scratch `applicationIdSuffix` change, `Ap5Probe.kt` and `ap5_matrix.ps1` / `ap5_ui.py` (left untracked in the worktree). Main merged in (`96f9af1f1`, clean); full gate passed. Codex's own matrix: tablet Dot + Colour passed every step; S22 Dot stopped at "remove → Undo" because the wireless-adb harness tapped Undo after the timed undo window (the title stayed removed). Claude re-checked on the S22: Remove then Undo within ~1 s restores the title (Default back to 2); merged build shows the selection toolbar and bar (Shelf, Mark watched, Save, Remove, More), crash buffer clean. S22 Colour not run. Scratch apps uninstalled; Codex's fixture data on the devices is gone with them.

**AP-16 (Claude, 2026-11-23, `80979e63a` on `tsuzuki/ap-16`):** a Continue Watching widget at least 244 dp tall keeps a bottom row for This week / This month watch time (`subscribeWatchStats`), like Continue Reading's stats row; the resume rows use the rest of the height. Full gate passed; not device-checked (needs a placed widget with watch history).

**AP8 first part (Claude, 2026-11-23, `6a86ec6c8` on `tsuzuki/ap8`, branched from `tsuzuki/ap5`):** the anime library's filter icon opens `AnimeLibraryOptionsSheet` (the book library's `TabbedDialog` with the shared settings rows): tri-state Downloaded / Unwatched / Started / Completed, sort by title, last watched, latest episode, unwatched count, date added, total episodes (tap again to reverse), Display Grid / List (`AnimeLibraryListRow`); one preference each (`AnimeLibraryOptionPreferences`); Shelves moved to the sheet's overflow menu; icon tinted while a filter is set. Sorting runs before season grouping. `AnimeLibraryOptionsTest` (4); full gate passed. S22 (scratch `claudeqa`, Dot, landscape): sheet shows the three tabs and overflow, Sort lists the six fields, Display → List shows the row (cover, title, 0 / 2 話); crash buffer clean. Follow-ups `74712aa30` (unwatched / downloaded badges on covers, following Library settings' unread / download badge switches) and the toolbar overflow (Update category → `AnimeLibraryUpdateJob.start(context, shelfId)`, Open random entry from the shown titles); full gates passed; follow-ups not device-checked. Follow-up: Display offers compact grid (title on the cover), comfortable grid, cover only and list, plus the book library's per-orientation column count (shared preference); full gate passed, not device-checked. Follow-up: tri-state Tracked filter (read-only `anime_sync.getTrackedAnimeIds`, `AnimeTrackRepository.subscribeTrackedAnimeIds`); full gate incl. `verifySqlDelightMigration` passed. Follow-up: language badge (source language, Library settings' language-badge switch). AP8 card items all present; device check covered the sheet and List only.

**AP7 first part (Claude, 2026-11-23, `09ccb856d` + `407abd3dc` on `tsuzuki/ap7`, branched from `tsuzuki/ap6`):** anime details overflow has Edit notes (`AnimeNotes.kt` dialog → `AnimeLibraryStore.setNotes`, the existing `notes` column), saved notes show in a tile under the header; tapping the cover opens `AnimeCoverDialog` (full size, Share, Save to Pictures via `ImageSaver`, the source's image request at original size). Full gates passed. S22 (scratch `claudeqa`, Dot): notes typed and saved show under the header; cover viewer opens, Save wrote `Pictures/Tsuzuki/01 plain mp4.jpg` (test file and folder removed afterwards); crash buffer clean. Share not tapped. Follow-up `ce6b8343a`: the action row shows the book's next-update tile (hourglass, Soon / N days) for the earliest episode the source dates in the future, hidden otherwise (anime `nextUpdate` is never set, so it isn't used); `NextAiringDaysTest`; full gate passed, not device-checked (the fixture has no future episodes). Not done in AP7: pixel match of the header to `MangaInfoHeader`, edit info / cover.

**Integration check (Claude, 2026-11-23, throwaway branch `tsuzuki/claude-integration`, not for merging as is):** `main` + ap8 (incl. ap5) + ap7 (incl. ap6) + ap9 + ap10 + ap11 + ap12-cache + ap-14 (incl. ap-21) + ap-15 + ap-16 + ap-17 (incl. AP-20) + ap-18 + ap-19 merge cleanly (one `docs/design/README.md` row conflict, both rows kept). `assembleDebug`, all-module `testDebugUnitTest` (1,587 tests, 0 failures) and `verifySqlDelightMigration` pass on the combination. Tablet smoke test (Dot): Watch, Library, Updates, Browse, More and Settings open, crash buffer clean. Suggested merge order into `main`: the same as above. **Colour pass on the same build (tablet portrait, fixture):** catalogue (In library badges in the Colour accent, dimmed covers, Filter / Display / ⋮), long-press add, Library › Anime options sheet (tabs, four layouts, column slider), library selection bar, details episode selection bar; all in the Colour accent, crash buffer clean. Part 2 (same build, Colour, tablet landscape, fixture with 19 s of real playback and new episodes): Updates › Anime (New 2 話 + Save all tiles, book-style rows), Save all → queue (8.6 MB saved, pills hidden with nothing queued/failed), History › Anime (AP9 row: poster, "第1話 · 残り00:40 · 19:02", progress bar, close; toolbar Last-only + Clear history), Watch home (AP-14: New episodes 2 / 2 shows in the accent, Watched today 0 min for 19 s), Data and storage (AP-19: Downloads 8.6 MB, Anime 2, Episodes 6). Crash buffer clean.

**Follow-up branch `tsuzuki/anime-bar-labels` (`0c7b00bcc`, built on `tsuzuki/claude-integration`, merge after the 15):** the shared chapter selection bar reads `LocalSelectionEpisodes`; anime details and anime Updates set it, so their bars say Mark as watched / unwatched, Mark previous as watched, Bookmark / Remove bookmark (existing `tsz_anime_*` strings). Book screens unchanged. Full gate passed. Tablet: episode selection bar shows 視聴済みにする and ブックマークに追加; crash buffer clean.

**Changelog draft (Claude, 2026-11-23, `3fd4a3b60` on `tsuzuki/changelog-ap`, stacked on `tsuzuki/anime-bar-labels`):** `CHANGELOG.md` Unreleased lists the Batch AP work (AP1–AP21 user-facing changes) in the existing plain style, ready for RV-AP to turn into `[0.2.2]`. KOKORO-GAP is not in it (owner decision pending). Full merge order: the 15 card branches, then `tsuzuki/anime-bar-labels`, then `tsuzuki/changelog-ap`.

**Owner answers 2026-11-23:** (1) player settings page → **standard Settings layout**; (2) parallel anime downloads → **no**, keep one at a time; (3) episode filter sheet → **convert to the book design** (tabs, tri-state); (4) edit anime info / cover → **not now**. Claude is doing (1) and (3).

**Decisions for the owner (Claude, 2026-11-23):** (1) AP11 player settings: the audit wants the standard Settings layout, but the A4.1 anime boards drew the current card layout; which one wins? (2) AP11 parallel anime downloads would change the download engine (one episode at a time today); wanted? (3) AP6 episode sheet: it already has sort, Unwatched / Bookmarked and group exclusion; matching the book dialog's tabs and tri-state rows changes the saved episode-filter flags that backups carry. Keep as is, or convert? (4) AP7 edit info / cover for anime: a source refresh would overwrite edited fields unless edits are kept apart. Wanted?

**Owner 2026-11-22: Codex does all remaining AP cards** (Claude handed off; the Kokoro 0.2.1 session may hold the Device lock until it releases it). **Prompt for the owner to paste into Codex (when it has usage):** `Read AGENTS.md, docs/HANDOFF.md ("Batch AP")
and docs/ANIME_PARITY_PLAN.md. Take the first AP card marked ready that is in the Codex lane and not claimed;
claim it here first (codex-working + worktree), follow the card, commit on tsuzuki/<card>, set review. Use the
tablet/S22 only under the Device lock.`

**AP6 filter sheet Result (Claude, 2026-11-23, owner answer 3 "convert to the book design"):** `fcd769a6a` + fix `deeff6ec2` on `tsuzuki/ap6`, merged into `tsuzuki/ap7`. The episode Filter / Sort dialog is now a `TabbedDialog` like the book chapter dialog: Filter tab with tri-state Watched (stored as the mirrored unwatched flag), Downloaded and Bookmarked, plus subtitle-group exclusion; Sort tab with episode number / upload date (tap again to reverse). Changes apply at once, dialog stays open. Flags: new `*_NOT` and `DOWNLOADED*` bits 36–39 next to the old ones; old "only" bits still read as ENABLED_IS, so saved settings and backups need no migration. Device (tablet, claudeqa, fixture case 11): Dot and Colour screenshots in `.worktrees/qa-ap/evidence/ap6*`; Watched check → empty list (nothing watched), X → both episodes, clear → both; survives restart; sort reverses; crash buffer empty. First build double-advanced the state (`TriStateItem` already passes the next state) — caught on device and fixed. Gate on ap6 before the one-line fix: `assembleDebug` + all-module tests passed.

## Codex next steps (Claude, 2026-11-21) — start here

Goal: ship **v0.2** (first anime release). Two Codex threads can run at once: the **anime lane** (implements, may use the tablet/S22 under the Device lock) and the **review lane** (reviews and merges into `main` locally, Batch T rules; never reviews its own work). Claude implemented most of the waiting branches, so Claude must not review them.

**Anime lane, in this order (one card at a time; each branches from the previous card):**
1. **AY1** — **done by Claude 2026-11-21, committed `07bd59c7f` on `tsuzuki/ay1`, status `review`** (S22 supplement and tablet portrait pass; live AnimePahe blocked by the site's rate limit — see the card). The anime lane does not review it; the **review lane** does (add it after AY0 below). Start the anime lane at AY9.
2. **AY9** — implementation committed as `eff92bac9` + `7ba2fca41`. The two-device synthetic matrix passed. Live One Piece and tablet landscape layout acceptance remain open. The local integration branch `review/v02-anime-chain` combines AY9, accepted AY0 and AY1, retaining both sets of strings. AY1 still needs independent acceptance before any anime merge into `main`.
3. **AY2 → AY3 → AY4 → AY6** — plans in `docs/ANIME_ANIYOMI_PARITY.md` §9.3–§9.6 (written from the code, with tests and device scripts).
4. **REL-0.2** (review lane, after AY6) — §9.7: chain review, anime off by default, v0.1.1 → v0.2 upgrade test, all-module tests, 16 KB + ABI checks, changelog, versionCode 26; owner tags.
Not in v0.2: A6.5, AY5, A6.8, AY7, A6.6, A6.7, AY8.

**Review lane, in this order (each committed on its card branch, independent review needed):**

Review result (Codex, 2026-11-21): STR2, V8.1, V8.3, V8.4, V8.5, READER-JP-FONT and STR3 passed and are on local `main` at `5f428274f`. AY-INT is excluded with changes requested under AY-INT-F. Both builds and all 1,396 tests passed. Evidence `.worktrees/review-v02-main/app/build/review-evidence/v02-main/`. MARK-UNTIL-LAST-READ is excluded with changes requested. AY0 passed and is on local `main` at `e9d8bb9eb`. PERF-START-2-F missed its S22 run-range gate and has cold source-ordering findings under PERF-START-2-F2. RECAP-F, DICT-F and the Kokoro follow-ups have changes requested on their cards. AY1 needs a different reviewer because this chat contributed to its implementation. The Claude-authored AY2, AY3, AY4 and AY6 deltas have independent pre-review findings on their cards. The chain still awaits AY1 acceptance.

**Owner UI reports 2026-11-22 (Claude, merged on local `main`, for 0.2.1).** (1) **ANIME-BROWSE-REDESIGN** `b7771ca5a`: anime Extensions follow board 04-browse "Extensions" — each extension's own icon (installed APK icon, else the repo's `icon/<pkg>.png`, initial behind), the "Aniyomi:" prefix dropped, Browse search (name or package's last segment; every package contains "animeextension"), Extension stores and Refresh as toolbar icons, Updates-ready + Update all tiles, language pills with language names, Update / Installed / Available · N as grouped tiles, outlined Install and filled Update/Trust pills; names wrap to two lines, the delete icon hides while a pill shows and the trust dialog's second button uninstalls a private install. Anime Sources: extension icons, language names, search. S22 Dot and Colour screenshots in `.worktrees/anime-browse/app/build/review-evidence/browse-redesign/`. (2) Anime **Migrate** list and sheet titles were drawn black on dark tiles (`AnimeMigrationActivity.kt`, no colour on `Text` inside `TsuzukiTile`): now `tokens.dotOn`. (3) **UI-FIXES** `e892a631e`: manga/novel details no longer show a second spinner under the cover while refreshing (ERR1 loading row dropped; the error card stays); the TextView reader no longer pushes the page down when the menu opens (`updateChapterEndClearance` scrolls by the padding change, so only the chapter top moves clear of the bar). All-module tests 1,512 / 0 failures on the merge. **Not device-checked:** (2) and (3) (need a library title / a novel on a scratch). Self-checked (owner waiver).

**KOKORO-RELEASE result (Claude, 2026-11-22, self-checked under the owner waiver; branch `tsuzuki/kokoro-release`, ready to fast-forward `main`).** TTS-SETTINGS + the 9 Kokoro commits cherry-picked (no other app-chain work). `f2044d172`: engine in every arm64 build (service + `:neural_tts` in main, AAR committed and hash-checked for every variant, R8 keeps sherpa-onnx + `KokoroProgressCallback`, debug gates replaced by the arm64 check, no "test" wording, 23 strings in 7 locales). `ebb3155ba`: licence check (recorded in `docs/research/neural-tts.md`: only the Apache-2.0 model files + our CMUdict lexicon with its licence; no upstream lexicon, no espeak) and runtime licences in About. Version **0.2.1 / 27**, `[0.2.1]` changelog incl. the anime Browse redesign, UI fixes and AP3. **Owner answers (via the parent session):** publish `voices-v1` yes, +21.9 MB yes. **`voices-v1` published** (not "latest"; v0.2.0 stays latest): https://github.com/Manojx2005/Tsuzuki/releases/tag/voices-v1, `kokoro-cmu-v1.zip` 129,833,251 B, SHA-256 `9fc12e01…0e93` verified from the public URL. **Gates** (on the merge with main `b6281d9ac`): 1,557 all-module tests, `verifySqlDelightMigration`, R8 `assembleRelease -Penable-updater`, 5 APKs 16 KB-aligned; arm64 90.97 MB (v0.2.0: 69.06 MB). **Devices** (R8 Preview, scratch `…kokoroqa`, probe `KokoroReleaseProbe` with test-only keep rules for its entry points; worker path unchanged): tablet and S22 install the public pack, the `:neural_tts` worker loads `libsherpa-onnx-jni.so`, English spoken by Kokoro (tablet 7.0 s, S22 8.5 s from a cold worker), a Japanese sentence returns "uncovered" (phone voice); crash buffers empty; scratch uninstalled. Not exercised on a device: the Settings download dialog UI and a full chapter Listen (unit-tested; dialog checked on the tablet in KOKORO-TRANSFER-F). Evidence `.worktrees/kokoro-release/app/build/review-evidence/kokoro-release/`. **Waiting:** owner OK to push `main` + tag `v0.2.1`.

**Owner 2026-11-22 (later): "I want kokoro on 0.2".** Decisions: ship Kokoro as **0.2.1**; **override the speed rule** (S22 RTF 1.05–1.09; prefetch + per-chunk phone-voice fallback cover gaps); host the pack as a GitHub release **`voices-v1`** on the Tsuzuki repo; **research the lexicon licence first** (if it does not permit redistribution, come back to the owner). Claude does NT2 → NT3 → NT4 now.

**v0.2.0 published (Claude, 2026-11-22 ~14:28, owner: "Ship v0.2 now, Kokoro in v0.3").** `main` `7fe7e3fd9` and tag `v0.2.0` pushed (the stale 2016 upstream tag `v0.2.0` was deleted locally first, as for v0.1.1). Release workflow run `44989552138` **succeeded**: https://github.com/Manojx2005/Tsuzuki/releases/tag/v0.2.0 is the latest release (not a pre-release) with five signed APKs (arm64-v8a 69.1 MB, armeabi-v7a, x86, x86_64, universal 108.5 MB). Build & Test run `44989552094` (all-module tests) **succeeded**. **Owner decisions 2026-11-22:** Claude reviews its own cards while Codex has no usage (marked "owner waiver" on each card); Claude designs and chooses the tablet Watch layout; **Kokoro moves to v0.3** (NT2 licence + speed, NT3 engine in release builds + Voices page, NT4 the owner uploads the pack; S22 RTF 1.05–1.09 fails the speed rule unless the owner overrides it). Next: v0.3 = Kokoro (NT2 first), plus the app chain decision (+29 MB dictionaries) and A11Y1.

**v0.2 candidate on local `main` (Claude, 2026-11-22 evening).** Owner: Codex has no usage, so Claude reviewed the Claude-written anime cards (AY1, AY2, AY3, AY4, AY6, their fixes, AY9-ART; all **pass**, see each card) and was told to design the tablet Watch layout (AY9-WATCH-TABLET). `main` = **`89d7e9556`**: the whole anime chain, Watch on tablets, **versionCode 26 / 0.2.0** and the `[0.2.0]` changelog. Gates on that exact code: **1,509 all-module tests / 0 failures**, `verifySqlDelightMigration`, `:app:assembleRelease -Penable-updater` (5 APKs, all 16 KB-aligned), anime off by default, optimized v0.1.1 → v0.2 install-over start with no crash. **Not pushed, not tagged.** **Device checks done 2026-11-22 (owner approved):** live AY9 on the tablet (AnimeKai (Unoriginal) 16.13 from yuzono, private install into scratch `…clauderevscratch`): One Piece, **1,180 episodes**, every visible row shows artwork after ten fast flings, `gfxinfo` jank Dot 26/556 (4.7 %), Colour 44/546 (8.1 %); S22 (wireless, serial verified) RC Debug as scratch: fixture Watch home (phone vertical hero) and episode list with previews, progress and visible download icons in Dot and Colour; crash buffers empty; both scratch apps uninstalled. Evidence `.worktrees/v02-rc-trial/app/build/review-evidence/{ay9-live,s22-rc}/`. Kokoro is **not** in v0.2 (debug-only, waits on the owner's listening test). Still open: the owner's tag `v0.2.0` + push (CI signs), then the signed upgrade on a real install. Optional: app chain (+29 MB dictionaries) stays out unless the owner says otherwise.

**Claude review pass (2026-11-22, afternoon).** Local `main` is now **`eee06cf0f`**: AY-INT + AY-INT-F + AY-INT-F2 merged (`c7ea7da00`) and PERF-START-2-F3 merged without the unmeasured font warm-up (`e9449b0ca`). Code reviews passed (Codex-written, so independent): AY9 core, AY9-ART-F, AY9-TILE-CONTRAST, AY2-F2 (nit: filters reload after Back from details), AY2-FILTER-CONTRAST, AY3-F2. They stay on the anime chain because they sit on Claude-written cards. **v0.2 RC trial** `review/v02-rc-trial` `3baf1bdbe` (`.worktrees/v02-rc-trial`) = `review/v02-anime-full-trial` `260a9e1cd` + this `main`: **1,509 all-module tests / 0 failures**, `verifySqlDelightMigration` passed, `:app:assembleRelease -Penable-updater` (as `release.yml`) built 5 APKs, all pass `zipalign -c -P 16`; anime defaults off (`tsuzuki_anime_enabled` false). Still version 0.1.1 / 25. **What still blocks the v0.2 release (cannot be done by Claude):** an independent (Codex) review of the Claude-written anime cards — AY1 (co-written), AY2, AY3, AY4, AY6 and their -F fixes, AY9-ART; live One Piece and tablet-landscape acceptance for AY9; the S22 pass (not connected today); then REL-0.2 (versionCode 26, changelog from `docs/CHANGELOG-0.2-draft.md`, release-signed v0.1.1 → v0.2 upgrade by CI signing) and the owner's tag. App chain, dictionaries and Kokoro still wait on the owner.

Latest continuation (2026-11-22). Local `main` is at `7e333ca2a` after MARK-UNTIL-LAST-READ-F. PERF-START-2-F3 and DICT-HINT-F2 await independent review.

`review/v02-anime-full-trial` at `260a9e1cd` combines the anime playback and AY-INT chains and the AY9 artwork and icon fixes plus AY2-FILTER-CONTRAST. Debug, Preview and Release built. The same source tree passed 1,503 unit tests and all 15 APKs passed 16 KB ZIP and applicable ELF checks. Optimized variants passed the lib 14/16/17 ABI audit. Tablet checks covered artwork, contrast, subtitle, integrated History, the fresh anime gate, and a synthetic Debug upgrade from the exact v0.1.1 tag. AY1 independent review, S22, live-source acceptance, and the release-signed upgrade remain open.

1. **Main-based set**, merge order (trial integration passed 1,231 tests, see 2026-11-19): STR2 `82e3648bb` → V8.1 `d850b3a2f` → V8.3 `c512c23b2` → V8.4 `73850a4b7` → V8.5 `24d465e57` → READER-JP-FONT `d0773b3d9` → STR3 `ce240fa11` → AY-INT `641018f95`+`b0fdce1eb`. Expect append-only conflicts in the 8 `strings.xml` tails and AGENTS.md (keep both). Drop STR2's stale comment in `TsuzukiAdvancedPreferences.kt` when STR3 lands.
2. **MARK-UNTIL-LAST-READ** `7139eef26`, **AY0** `70974003a` (fixture server/sources; AY1 builds on it), then **AY1** `07bd59c7f` (branch `tsuzuki/ay1`, on AY0; PARITY §9.0 checklist; evidence in `.worktrees/ay1/app/build/review-evidence/ay1/`).
3. **PERF-START-2-F** `2c772793d` + gate `45cf42deb`: measure optimized Preview on the **S22**, 20 cold launches baseline vs candidate; keep the gate only with a gain beyond run ranges and Home-observed not worse; also a cold library update with the process dead. Tablet debug showed no gain.
4. **App chain** (`review/app-chain` + `d9b94f259`): first resume or restart the stalled **RECAP-F** review (`.worktrees/review-recap-f`; `animescratch` was left unrestored there), then TTS-POLISH, then DICT-F `900f081f1`. Merge only after the owner decides the +29 MB bundled dictionaries and whether the chain ships in v0.2.
5. **KOKORO-INTEGRATION** (debug only; crash fix `7c297e232`, prefetch `b9f2eca6f`, download screen `75fc7715b`): code review; it stays behind NT1/NT2 and the owner's listening test. `app/libs/*.aar` stays untracked until the owner decides.

**Waiting on the owner (don't guess):** +29 MB dictionaries; publishing dictionary packs; voice-pack host (default `releases/download/voices-v1/kokoro-cmu-v1.zip`); Kokoro listening test (S22 RTF 1.05–1.09, ~590 MB); V8.2 board; V8.6 help links; whether the app chain rides in v0.2.

**Prompts to paste:**
- Anime lane: `Read AGENTS.md, docs/HANDOFF.md ("Codex next steps") and docs/ANIME_ANIYOMI_PARITY.md (§3, §9). You are the anime lane: AY1 is done and in review; finish or reset AY9 first, then AY2, AY3, AY4, AY6 in order. Claim the Device lock before adb.`
- Review lane: `Read AGENTS.md and docs/HANDOFF.md ("Codex next steps"). You are the review lane (Batch T): review and merge the main-based set in the listed order, then MARK-UNTIL and AY0, then measure PERF-START-2-F on the S22. Don't merge the app chain or Kokoro until the owner decides.`

## Current state (2026-11-21, Claude)

Owner answers 2026-11-21: **commit everything on card branches**; **Library › All: anime sit on the All page in their own section** (the Anime pill stays); **Updates › All per-chapter list: tapping an episode keeps playing it**; **PERF-START-2-F: build the app-ready gate**.

- **Committed on card branches (no merge, no push):** STR2 `82e3648bb` (`tsuzuki/str2-main`), STR3 `ce240fa11`, V8.1 `d850b3a2f`, V8.3 `c512c23b2`, V8.4 `73850a4b7`, V8.5 `24d465e57`, READER-JP-FONT `d0773b3d9`, DICT-F `900f081f1` (on `review/app-chain`'s line), MARK-UNTIL-LAST-READ `7139eef26` (`tsuzuki/mark-until-last-read`), AY0 `70974003a` (`tsuzuki/ay0-fixture-server`), AY-INT `641018f95` + item 1 `b0fdce1eb` (`tsuzuki/ay-int`), PERF-START-2-F part 1 `2c772793d`, KOKORO-INTEGRATION crash fix `7c297e232`. All need an independent review (not Claude); merge order and trial-integration notes in the 2026-11-19 block.
- **AY-INT is complete → `review`.** Item 1 (`b0fdce1eb`): new `presentation/tsuzuki/anime/TsuzukiAllPageAnime.kt` (`allPageAnime`, `allPageAnimeSection`); `TsuzukiMixedLibraryGrid` draws an "アニメ" section after the books on the **first shelf page only, filter All, no search**, Downloaded only respected; upstream `LibraryPager.kt` passes `firstPage = page == 0` (one argument). `AnimeLibraryCover` made internal. Test `TsuzukiAllPageAnimeTest` (2); app suite **1,244, 0 failures**. Tablet (throwaway trial = AY-INT + item 1 + AY0, fixture case 01 favourited): すべて shows the two novels then 「アニメ」 with 「01 plain mp4 · 0 / 2 話」; 小説 hides it; tapping opens the anime; no crash; data restored byte-equal (9 files). Also wrapped three >120-char lines from the 2026-11-20 commit (Spotless was failing on them).
- **Kokoro:** crash fixed and playing on the tablet (see the 2026-11-15/21 Kokoro log entry): RTF ≈ 0.85, 2.6 s gap before each Kokoro chunk, worker ≈ 463 MB PSS.
- **PERF-START-2-F gate done → `review`** (`45cf42deb`): no gain measured on the tablet debug build (warm-up only ~45 ms there); the S22 optimized measurement decides whether it stays. Still open for the owner: V8.2 kaomoji board, V8.6 help links, DICTIONARY-DEFAULTS +29 MB APK, publishing the dictionary packs, the stalled Codex RECAP-F review on `animescratch`, the Kokoro AAR (commit or keep untracked).

## Current state (2026-11-20, Claude) — what waits on the owner

Everything below is uncommitted and waits for an owner answer; details are in each card.

1. **Commit the finished sets on their card branches** (then independent review): STR2, STR3, V8.1, V8.3, V8.4, V8.5 (incl. the search-count fix), READER-JP-FONT, DICT-F, MARK-UNTIL-LAST-READ (`.worktrees/mark-until`), AY0 (`.worktrees/ay0-server`, built on `main`), AY-INT (`.worktrees/ay-int`, items 2–5 done), PERF-START-2-F part 1 + font warm-up (`.worktrees/perf-f`). Merge order and trial-integration notes: 2026-11-19 block below.
2. **Stalled Codex RECAP-F review** holds `animescratch` (unrestored, `animescratch.test` installed) — resume it or restore. AY9 (Codex, untouched since 2026-11-15) and the whole v0.2 chain AY0 → AY6 wait on AY9.
3. **Library › All with anime (AY-INT item 1):** books page by book shelf, anime have separate shelves — anime on (a) every shelf page, (b) the first shelf page, (c) their own "Anime" pill in All, or (d) one unpaged list?
4. **Updates › All, per-chapter list:** tapping an episode plays it now; keep, or open the anime (as the grouped row does)?
5. **PERF-START-2-F:** moving the WebView warm-up needs one gate that network/source/Coil/JS/workers all await (audit in the card). Build the gate, or drop the move and look for the 500 ms elsewhere?
6. Older open questions: V8.2 kaomoji (needs a board), V8.6 help links (where should help point?), Kokoro model download, DICTIONARY-DEFAULTS +29 MB APK, publishing the dictionary packs.

## Current state (2026-11-19, Claude)

- **Owner 2026-11-19 (this chat): "you can do codex work too"** — Claude may take `ready` Codex-lane cards, not only reviews. Independent review still comes from another lane (the implementer never reviews or merges its own card).
- **Stalled Codex RECAP-F review** (`.worktrees/review-recap-f`, `review/recap-f`): stopped 2026-11-15 22:49 with uncommitted review fixes; its last tablet run failed (paged **plain** chapter at 90 % fell back to the previous chapter; likely cause and a suggested fix in the RECAP-F card). **`animescratch` is left unrestored with `animescratch.test` installed on both devices**; snapshots and `restore-scratch.py` are in that worktree's `app/build/review-evidence/recap-f/`. Owner: resume that review, or restore. Claude did not touch it (Claude implemented RECAP-F).
- **Verdicts given 2026-11-19 (Claude, independent of the implementer), none merged:** TTS-POLISH pass (tablet), DETAILS-REFRESH-TRACKING pass (UI), and from the 2026-11-15 device checks READER-DEFAULTS, TTS-SETTINGS; DICTIONARY-DEFAULTS now **changes needed (DICT-F)**. All sit on `review/app-chain`, which also carries Claude's RECAP-FIX/RECAP-F, so **the chain merges only after the RECAP review passes**; DICTIONARY-DEFAULTS also waits on the owner's +29 MB APK decision.
- **Done but uncommitted (owner OK needed to commit on the card branch, then independent review):** STR2 (`.worktrees/str2-main`, `tsuzuki/str2-main`; supersedes `.worktrees/str2`), V8.1 (`.worktrees/v8-1`), V8.3 (`.worktrees/v8-3`), READER-JP-FONT (`.worktrees/reader-jp-font`), DICT-F (`.worktrees/dict-f`, from `review/app-chain`), V8.5 (`.worktrees/v8-5`), V8.4 (`.worktrees/v8-4`), STR3 (`.worktrees/str3`). All from `main` `3ce5d2425`, 1,219–1,222 app tests pass, tablet self-checks pass.
- **Trial integration of the main-based sets (Claude 2026-11-19, nothing committed):** in a throwaway detached worktree from `main` `3ce5d2425`, the uncommitted changes were applied in this order with `git apply --3way`: **STR2 → V8.1 → V8.3 → V8.4 → V8.5 → READER-JP-FONT → STR3 → AY-INT**. Only append-only conflicts occurred (the 8 `strings.xml` tails and `AGENTS.md` notes), resolved by keeping both sides; the code overlaps (`TsuzukiAdvancedPreferences.kt` STR2+STR3, `SettingsTrackingScreen.kt` STR3+AY-INT) merged cleanly. No duplicate string keys added (the one duplicate, `migrationListScreen.copyNowActionLabel`, is already on `main`). Combined full app suite **1,231 tests, 0 failures/errors/skips** (incl. STR2's locale-completeness test over all new keys). Tablet smoke on the combined build: Home, Settings anime row, Tracking in Japanese, Browse settings Advanced fold, Library search with all four pills; no crash; data restored. **When merging for real, also drop STR2's comment "Upstream hardcodes these two titles in English…" in `TsuzukiAdvancedPreferences.kt`** (stale once STR3 lands). Leftover throwaway worktrees, both detached and safe to remove with `git worktree remove --force`: `.worktrees/trial-int` (first attempt; Windows would not delete it and resetting it was declined by the safety check) and `.worktrees/trial-int2` (the combined result).
- **Trial integration of the app-chain pair (Claude 2026-11-19, nothing committed):** DICT-F's uncommitted changes applied with `git apply --3way` on top of `tsuzuki/tts-polish` `d4878ac49` (which already contains `review/app-chain` `d9b94f259`) in the throwaway `.worktrees/trial-chain`: only append-only conflicts (8 `strings.xml` tails + `AGENTS.md`), kept both sides. Result: **0 missing `tsz_` keys in every locale and no duplicate keys** (TTS-POLISH covers the TTS strings, DICT-F the dictionary/app-update ones), full app suite **1,256 tests, 0 failures/errors/skips**. Merge order for the chain: app chain → TTS-POLISH → DICT-F (after the RECAP review passes). `.worktrees/trial-chain` is detached and safe to remove.
- **Merge note:** STR2, V8.1 and TTS-POLISH each append strings at the end of the same 8 `strings.xml` files — expect a trivial conflict per file when merging them in sequence.
- **New cards:** V8.2 (empty-state kaomoji in the display face) needs the owner; V8.4 (migration/About polish) and V8.5 (Library search hides All/Manga pills) ready for Codex; V8.6 (help links open Tsundoku's site) needs the owner. **DICT-F** (kanji words in Japanese chapters are offered the Chinese pack; 18–21 dictionary strings untranslated on the app chain) is ready and should land before the chain merges; READER-JP-FONT (TextView reader draws Japanese in the system sans) is ready. V8 itself: tablet pass done (incl. WebView); widget picker, extension details, phone Settings and S22 open.
- `docs/HANDOFF.md` is itself uncommitted on `main` and includes the stalled Codex thread's RECAP-F claim edit. Device lock: free (rv5debugscratch on the TTS-POLISH chain build, data restored).

## Current state (2026-11-15, Codex — existing app fixes first)

Owner latest steering: finish existing fixes before the queued anime cards. **TTS-POLISH is review** (`1da547881`) on `tsuzuki/tts-polish`, based on Claude app-chain `d9b94f259`; translated TTS/reset/list labels, inline information icon and wrapping preference values, Debug and 1,420 all-module tests pass. No device self-review or merge. Claude's recap/reader/dictionary changes remain on `review/app-chain`; the owner's voice/listening decisions are still pending for Kokoro.

**READER-NEXT-GUARD passed and merged locally** as `3ce5d2425`, including fix `e46df5e41` (Debug/Preview, 1,384 tests and four device/theme cases). Next: **RECAP-F independent component review** on `review/recap-f`, then resume AY9 acceptance and the explicitly requested AY0 → AY1 → AY2 → AY3 → AY4 → AY6 chain. AY9 is preserved uncommitted with 1,389 tests passing; pause captures/store checks pass on both, direct-close fix passes S22; tablet source changed and remaining UI/jank/native lifecycle acceptance is incomplete. **Do not mark AY9 review yet.** Scratch data restored, helpers removed, Dot Homes inspected, no crashes; **Device lock free**. No push/tag.

## Current state (2026-11-15, Codex — RV3-S accepted)

This block supersedes the afternoon checkpoint below. **RV3-S passed and merged locally as `304bd9aff`. No push or tag.**

- Final production code `03128b5ca` includes download-retention fix `a3740a40c` and the four-pill History sizing fix. Claude's code review and an independent second reviewer covered storage/backup/migrations and both fixes. Debug + optimized Preview rebuilt after all four pill modifiers: **1,215 app / 1,382 all-module tests, zero failures/errors/skips**; 686 locale/placeholder checks; all **10 APK ZIP / 100 included 64-bit ELF alignment checks** pass. Migration 36 unchanged; 37–39 additive.
- Final History device matrix passed on tablet and hardware-verified wireless S22: Debug, Dot/Colour, anime on/off (eight cases); optimized Preview, Dot/Colour (four cases). All labels render, Anime opens persisted One Piece history, and disabling anime restores the normal three book filters. Representative screenshots inspected; crash buffers empty. Core real playback/resume/EOF/native-track and anime-off gate coverage from the checkpoint remains valid; no player source changed afterward. Authenticated trackers and multilingual subtitle rendering were not exercised; those documented parity sweeps remain future work.
- Final ARM64 scratch Debug SHA-256 `14d3952dc91b0dce24c1d29afae44d4b32c5c5501a8862c7bb949ea99947815a`; Preview `c6ae55b72ab057010d438019958cbe04590515f4826d40c34dbcc29e1f695799`. Evidence archived in **`app/build/review-evidence/rv3-s-final/`**: accepted-history-audit.json, final History results/screenshots/XML, logs, helper source, restoration reports and both APKs. Temporary helper removed from module sources.
- Scratch data restored from the latest `*-history-final-before.tar` snapshots: **10 tablet / 11 S22 DB+preference files byte-equal before launch**, original files restored, helper uninstalled. Final Debug scratch installed, both left awake on Dot Home; handback screenshots inspected, crash logs empty. Daily apps and owner phone untouched. **Device lock free.**
- Owner 2026-11-15: **Dot book covers should be coloured by default.** Confirmed getter `tsuzukiDotColourCovers` already defaults true (also in v0.1.1) and shared book cover paths respect it. Corrected stale grayscale-default rules in AGENTS/PLAN/design docs; saved grayscale remains selectable. No new runtime migration required.
- **READER-DEFAULTS remains review**, `tsuzuki/reader-defaults`, implementation `9a483ac31`, docs `0e33d9f43`: shared reader column 600 → 720 dp, 1,092 app tests. This chat implemented it; a separate reviewer must device-check and merge it. Do not self-merge.
- **AY9 claimed** in `.worktrees/ay9`, `tsuzuki/ay9`, base `304bd9aff`; implementation from accepted current main (retain both RV3-S fixes), then AY0/AY1 in Batch AY order. STR2/A11Y1 remain parked; neural voice/licence/listening gates unchanged. Owner's continue-until-usage goal remains active.

## Earlier checkpoint (2026-11-15 afternoon, Codex — superseded by RV3-S acceptance above)

Owner asked for a handoff update before Codex usage runs out. **This block supersedes older next-action statements. No anime merge to main, push or tag. Owner resumed Codex with the active goal to continue until usage runs out; Codex is completing final RV3-S verification in the existing review worktree.**

- **READER-DEFAULTS is `review`**, `.worktrees/reader-defaults`, `tsuzuki/reader-defaults`: implementation **`9a483ac31`**, hash-recording doc commit **`0e33d9f43`**. Owner screenshot requests the same book-like serif/paragraph layout with less side whitespace as the first-install default. Shared Native/WebView column cap widened **600 → 720 dp**; on the 753 dp tablet portrait with default 16 dp margins, sides become **16/17 dp instead of 76/77 dp**. Tablet dispatch threshold remains 600 dp; phone layout and saved font/margin/line/paragraph choices unchanged. Scoped Spotless twice, Debug, **1,092 app tests**, diff check pass. Independent device review is still required; this chat implemented it and must not self-review/merge it. `docs/design/README.md` updated on its branch. APK/logs in that worktree under `app/build/reader-defaults/` and `app/build/outputs/apk/debug/`. The app was **not** installed on daily devices.
- **RV3-S remains `codex-working`**, `.worktrees/rv3-s`, branch **`review/rv3-s`**, from main `ed5fd0c41`, merged `tsuzuki/ay-play` `644bb44e4`. Claude's code review/owner acceptance of libmpv shipping were read. Independent second agent reviewed storage/backup/migrations, then the new fixes. **Do not merge the chain until the final History fix is device-verified.**
- **Download-retention review fix committed `a3740a40c`.** Found that production `AnimeLibraryStore.refresh()` never supplied protected download IDs to `syncEpisodes()`, so a source removing an unwatched downloaded/queued episode deleted its database row and orphaned playback. Store now takes a lazy suspend ID provider; `AppModule` resolves Downloads only when called, avoiding a constructor cycle. Downloads awaits restored queue state and protects all statuses (saved/queued/failed/removing). A real database regression through `refresh()` fails with original behavior (expected IDs `[1,2]`, got `[]`), then passes with the fix, including later removal after protection clears. Queue cross-anime/status test added. Five code/test files plus AGENTS upstream-adapter list. Second reviewer accepted the wiring. Snapshot/enqueue race is not broadened in this fix.
- **Verified candidate before the final History patch:** normal Debug + Preview and scratch Debug + Preview build; **1,215 app / 1,382 all-module unit tests**, no failures/errors/skips; 686 locale/placeholder checks; **10 APK ZIP alignment + 100 included 64-bit ELF entry checks** pass. Scratch ARM64 Debug SHA-256 `9402c95dfa94bb2bc54a6de411405b1ec501c8cb5da3c1697d0a4fa63d3ba52e`, Preview `b1940ac783624b6fc157b555cb4c31af2acf4bd02687b1d06d3f78d6f0162af9`. These are **before** the final History UI patch, not final shipping hashes. Migration 36 unchanged from v0.1.0; 37–39 additive. Evidence in `.worktrees/rv3-s/app/build/review-evidence/rv3-s/`.
- **Devices:** verified tablet `R52Y90HA77L`; mDNS reconnected S22 **`192.168.40.243:44823`**, verified hardware `RZCWA139X1X`. Only **`io.github.manojx2005.tsuzuki.animescratch`** and its `.test` helper used. Original scratch `shared_prefs`, `databases`, `files` snapshots: `tablet-scratch-before.tar` / `s22-scratch-before.tar` in evidence. Daily `.dev`, `.debug`, `rv5debugscratch`, release and owner phone untouched. No source trust changes, system extension installs, tracker logins or reverse/fixture server changes.
- **Device passes on that pre-History-patch candidate:** anime-off Home in **Dot and Colour, Debug and optimized Preview, both devices**; AnimeKai real episodes 1/2 start in 6.5–8.6 s, advance, resume 600 → ~604.5 s, seek EOF then Play restarts/advances, saved 1473/1478 s resets to ~1–2 s; both themes/devices. Anikage returns no videos and shows readable failure. AnimePahe catalogue loads in both themes/devices (no repeat of its episode playback this review). Details refresh and adding One Piece to the scratch library work; Library and Updates Anime filters work. Native audio Off then restored selection and subtitle Off report true on both devices; multi-language subtitle rendering not checked. Crash buffers empty.
- **Final History fix committed `03128b5ca`, not yet accepted:** after tracked playback, `anime_history` really contains episode 2360 (tablet 6 watched seconds; S22 existing+new 53 s), but History displays **four blank narrow pills** and the book-empty state. This is **layout, not lost history**: the new horizontal scroller gives infinite max width, while `TsuzukiPill`'s label uses `weight(1f, fill=false)`. The committed fix in **`presentation/history/HistoryScreen.kt`** gives **all four Tsuzuki pills** conditional `Modifier.width(IntrinsicSize.Max)` only when anime history makes the row scrollable. No-anime and stock layouts unchanged. Boards `03-home/History.png` and `03-home/dot/History.png` plus HTML viewed. Second reviewer approved; it caught the initially omitted Novels modifier, now corrected (four uses confirmed). Two scoped Spotless passes and diff check pass; second reviewer approved all four modifiers. **Retain commit `03128b5ca`.**
- **Build state / exact next action:** **No Gradle build is left running.** `history-final-build.log` completed successfully (3m48s), but may have compiled the intermediate three-pill version before the missing Novels modifier was added. Final four-pill code is committed as `03128b5ca` and scoped-formatted twice. Therefore **rerun scratch Debug + Preview + all-module tests from that commit**, then install final scratch Debug on both devices, create/retain a tracked playback history record, cold-open History, verify all four labels, tap Anime, and confirm the persisted One Piece episode row. Test Dot/Colour and anime-off behavior; record final APK hashes/alignment and verdict before merging. No separate History fix card: this small review fix is already committed.
- **Temporary QA source removed from source and archived:** `app/build/review-evidence/rv3-s/AnimeLivePlaybackProbe.kt` in rv3-s; copy it back to the AndroidTest path and build the test APK only if needed for the final checks. It asserts scratch package only. Modes: `gate=true` checks default/off and captures both themes; `suite=true` repeats live start/resume/EOF/near-end; `surfaces=true` tests details and native tracks (its repeated MainActivity shortcut hops race; do not claim those History/Updates screenshots succeeded); `tracked=true` starts episode 2360 with **`anime_playback_track=true`**, unlike the original probe's untracked playback. Scripts `install-and-gate.py`, `run-suite.py`, `check-preview.py`, `check-surfaces.py`, `check-tracked.py` and scratch init are in evidence. Cold launches in `check-tracked.py` correctly reach real Library/Updates/History; blank History labels are the remaining defect. Data-audit DB copies and before/after failure XML/logs are evidence only.
- **Device handback completed:** both original scratch snapshots restored; **10 tablet / 11 S22 database+preference files compared byte-for-byte before relaunch**. Original `files/` restored too. Scratch test helper uninstalled on both. Both scratch apps left on **Dot Home**, awake; handback screenshots inspected and crash logs empty. Daily apps untouched. **Device lock is free.** `restore-scratch.py`, per-device `*-restoration.txt`, `*-handback.png`, and empty `*-handback-crash.log` record this. No restoration should be repeated blindly. The installed APK remains the pre-History-patch Debug `9402c95…`; final `03128b5ca` APK is **not** installed/accepted.
- **After RV3-S passes:** merge locally per Batch T (no push), update card/status table, archive evidence before removing the review worktree. AY9 comes next; branch from the accepted main so `a3740a40c` and final History fix are retained. STR2/A11Y1 stay parked; neural TTS/licence/listening gates unchanged. **READER-DEFAULTS still needs a separate reviewer.**

## Current state (2026-11-15, Codex review lane — requested reviews and candidate refresh complete)

- **NT-LEX1 passed and merged**; **PERF-START-V passed** with the full S22 baseline in PERFORMANCE.md.
- **PERF-START-2 was measured d1950daa5 alone and rejected**: its gain did not exceed variability. The 500 ms engineering target remains (owner delegated judgment); PERF-START-2-F is ready for the app lane. Neither d1950daa5 nor diagnostic eb5e21fa7 is merged. Diagnostic code remains isolated.
- **BACK1 passed with review fixes** 3305865d0 (saved Home key) and 7e3a3d635 (explicit root navigator). Accepted main review 3606521fb; original f9d047d95 alone regressed normal Back. Full device/theme matrix: 48 timed cases + 16 edge gestures, plus normal and dead-process restoration checks.
- **0.1.1 candidate refreshed**, on `tsuzuki/rel-0.1.1`; final APK source **6119f2613**, versionCode 25 / Release 0.1.1 / Preview 0.1.1-8848. Includes BACK1 and faster PERF-DETAILS. A first Colour opening with the 200 ms animation landed on the 300 ms boundary, so the candidate review refinement uses **180 ms synchronized cover/fade**, reserving 120 ms for preparation. Main has the same refinement via 5c4b89421.
- Final S22 cached-opening upper bounds: **Dot 260 / 227 / 252 ms; Colour 226 / 253 / 243 ms**, original variable-rate video PTS, approximately 8–17 ms frame uncertainty. Synthetic placeholders only; real-cover and reduced-motion timing remain unmeasured. Final APK additionally passes 12 timed Library Back cases, four edge gestures and all four normal/restored Back combinations.
- Preview + Release + **1,091 tests pass**; all 15 normal/scratch APKs pass 16 KB ZIP alignment, included 64-bit ELF alignment and baseline profile checks. Release R8 initially ran out of heap with two workers; the single-worker retry passed. Exact tagged-v0.1.0 snapshot → final candidate direct upgrade preserves 500 books, 50,000 chapters, 995 history entries, 30 user shelves and all eight exported existing preferences, including Colour. No migration exception found.
- Both test devices are on the final optimized candidate scratch **Dot Home**, 500 books, awake; screenshots inspected and crash buffers empty. **Device lock: Devices: Claude subagent — TTS-SETTINGS/DETAILS-REFRESH device check**. Daily apps and owner phone untouched; STR2 still claude-working and untouched.
- Evidence and local ARM64 APKs: `app/build/review-evidence/rel-011-refresh-final/` (final-audit.json). Preview SHA-256 `51262ee4f416d69b2e7d2461baa9826417c0545458ca9ef729760a8fc17e0482`; Release `2f45a74d32b899be8f52cc7dd42b260361777eeddf088ca29f72e7212c27fc5f`; tested scratch `965e15c459bcde317b77320b061cde1d854c4c0c923b32737145229c6458d2f9`.

**Stop at the publishing handoff.** Local APKs use Android Debug signing. Owner handles official CI signing, owner-phone install, stale local v0.1.1 tag resolution, tagging and publishing. No tag/push performed. Neural runtime/voice and the anime chain remain outside this candidate. Earlier current-state blocks are historical; this block supersedes their next-action/candidate statements.

## Current state (2026-11-15 early morning, Claude — 0.1.1 tagged; anime is next)

Owner 2026-11-15 (before sleeping): (1) if everything is done with 0.1.1, Claude may push the build; (2) **anime
is the priority after 0.1.1**; (3) **STR2 is parked** to save usage (partial, uncommitted work in
`.worktrees/str2`: strings moved in 15 files plus a locale-coverage test, tests never run); A11Y1 waits with it.

- Claude's check of Codex's refreshed candidate before tagging: `main` and `tsuzuki/rel-0.1.1` differ only in
  docs; versionCode 25 / versionName 0.1.1; `:app:testDebugUnitTest` re-run on the release branch = **1,091
  tests, 0 failures**; Codex's review fixes read (`3305865d0`, `7e3a3d635`, `5c4b89421`); the 153 unpushed commits
  scanned for secrets, keystores and binaries (none); both test devices on the candidate, Dot Home, crash buffers
  empty. Not re-done by Claude: the Release build, 16 KB audit and the 0.1.0 → 0.1.1 upgrade comparison (Codex's
  `app/build/review-evidence/rel-011-refresh-final/final-audit.json`).
- The stale local tag `v0.1.1` (2016 upstream commit `0210fd882`) was deleted and `v0.1.1` now points at `main`.
  `main` and the tag were pushed to `origin`. **0.1.1 is published**: the Release workflow succeeded and
  https://github.com/Manojx2005/Tsuzuki/releases/tag/v0.1.1 has five signed APKs (arm64-v8a, armeabi-v7a, x86,
  x86_64, universal). The separate "Build & Test App" workflow failed on one stale `:domain` test
  (`RateLimitResolverTest` still expected 1 permit; the default became 2 on 2026-11-07); fixed on `main` after
  the tag. Local reviews only ever ran `:app` tests: **run `./gradlew testDebugUnitTest` (all modules) before a release.**
- **The anime pause is over** (Plan 0.1.1 step 10). **Read "Anime restart" at the top of Batch AY before any
  anime card**: the chain was merged with v0.1.1 on `tsuzuki/anime-sync`, and every anime card now branches from
  there, not from `tsuzuki/a6.4`.
- Still open and not anime: cold start 741 ms vs 500 ms (PERF-START-2-F); neural voice (lexicon decision, owner
  listening test); R12-F / R14 / S1 reviews (owner must approve trusting one live extension); R13 dictionary
  packs not published; owner-phone install of 0.1.1.

## Current state (2026-11-14 night, Claude — reviewing the app lane; budget subagents implement)

Owner 2026-11-14 night: Claude checks Codex's work, says what Codex does next, and uses cheaper subagents for
implementation, until Claude's usage runs out. Nothing is pushed or tagged.

**Verdicts on the four cards Codex left in `review`:**
- **PERF-START**: pass as an investigation, merged (`7defdb569`). The 500 ms miss is open.
- **R12-WARN**: pass; stays on `tsuzuki/r12-warn` (R12-F is still excluded from `main`).
- **NT1-BINARY + NT1-BINARY-F**: pass; scripts merged (`343a17309`). The runtime is not in the app. The neural
  voice is still blocked by the lexicon licence and the owner listening test.
- **PERF-DETAILS**: review fix `260691fba` on `tsuzuki/perf-details` (the duration local is now a dynamic
  `compositionLocalOf`; a static one recomposes both screens unskipped whenever the duration changes). Device
  timing on the S22: **pass**, 235–292 ms against 300 ms in Dot and Colour; merged (`c0a12f2c4`). New card BACK1
  from a Back-during-opening oddity seen in the test.

**Historical candidate state (superseded by the 2026-11-15 refresh above).** `tsuzuki/rel-0.1.1` (`987051988`) is the reviewed 0.1.1 candidate and the
tag point. `main` has moved past it with PERF-START and the neural build scripts (no app code changed by
either). Anything else merged into `main` from now on is for 0.1.2 unless the owner says to refresh the candidate.

Owner update (2026-11-14, review-lane chat): review NT-LEX1 first, then PERF-START-V when the Device lock is free, then PERF-START-2 (`d1950daa5` alone first; `eb5e21fa7` diagnostic only, never merged). **Refreshing the 0.1.1 candidate with BACK1 and the faster PERF-DETAILS opening is now authorized**; BACK1 must first leave `claude-working` and pass review. No tag/push; do not touch other `claude-working` cards.

**What Codex does next.**
*Review lane (a chat that did not write the card; Device lock first):*
1. **PERF-START-V** (ready): run the Home-observed helper on the S22, record the baseline.
2. Review **NT-LEX1** and **PERF-START-2** when Claude's subagents set them to `review` (Codex reviews Claude's
   subagents the same way it reviews Codex: read the diff, re-run the tests, don't trust the Result). PERF-START-2
   also needs the per-commit S22 measurements described in its card.
3. If the owner asks for PERF-DETAILS in 0.1.1: merge `main` into `tsuzuki/rel-0.1.1`, re-run REL-0.1.1 steps
   3–5 (build Preview + Release, tests, 16 KB check, upgrade check), and record new hashes. Not before the owner asks.
*App lane (one thread, no adb, own branch from `main`):* **A11Y1**, branched from `tsuzuki/str2` once STR2 is `review` (same files); BACK1 and STR2 are with Claude subagents (cards below, after NT-LEX1).
Don't take PERF-START-2 or NT-LEX1 while they say `claude-working`. If a `claude-working` card has no Result and
no log entry for a day, Claude has stopped: take it over from its worktree and say so in the card.
*Do not start:* NT2 steps 2–4, NT3, NT4, any anime card (paused until the 0.1.1 tag), tagging, pushing.

**Waiting for the owner:**
1. Publish 0.1.1: delete the stale local tag first (`git tag -d v0.1.1`, it points at a 2016 upstream commit),
   then tag `tsuzuki/rel-0.1.1` and let CI sign and publish. Local APKs are debug-signed.
2. Approve trusting one live Keiyoushi extension on a test device (unblocks the R12-F, R14 and S1 reviews).
3. Neural voice: wait for the misaki maintainer, or go with our own CMUdict lexicon (NT-LEX1 is the first step;
   the research estimates 3–4 weeks to a respectable US-only voice).
4. R13 dictionary packs are not published; the 0.1.1 changelog must not promise them until they are.

## Current state (2026-11-14, Codex review lane — Case B completed)

RV5, PERF1 and REL-0.1.1 candidate preparation are complete. Accepted review fixes:
`0a96ace3d`, `bf5055dcd`, `8570ff5d7`; RV5 close `88bcf6e4e`, PERF1 `5bd037c50`.
Release branch includes those fixes, versionCode 25 / versionName 0.1.1. Preview + Release
builds and 1,085 tests pass; 16 KB checks pass. Exact Y4 upgrade comparison retained
500 books, 50,000 chapters, 995 history entries, 30 shelves and exported settings.
Both test devices are on optimized candidate scratch Dot Home with 500 synthetic books;
final crash buffers empty, devices remain awake. Evidence: `app/build/review-evidence/rel-011/`.
PERF-START and PERF-DETAILS are ready follow-ups for measured budget misses; no tuning done.

**Stop at the owner publishing handoff.** Local artifacts use the Android Debug signing
certificate; official CI signing and publishing remain with the owner, along with the owner-phone
install and stale `v0.1.1` tag decision. No tag or push. Neural voice/anime remain blocked.
Daily apps were untouched: auto review rejected relaunching them, so scratch candidates
were used for the final handback. Local fixture server/reverse stopped and test Wi-Fi/files restored.

## Current state (2026-11-14 evening, Claude — took over from Codex, which is out of usage)

Owner 2026-11-14: Claude orchestrates and cheaper agents (Sonnet/Haiku subagents) do the work, until Claude's usage runs out. Nothing is pushed; `main` is still `0b1c8c889` until RV5 closes.

**For Codex, if it resumes before Claude finishes — read this block first.**
- Branch map (all local, nothing pushed, `main` untouched at `0b1c8c889`):
  `review/rv5` (`.worktrees/rv5`, tip `86e640358`) = S2-F + RP1 + STR1 + two review fixes + TTS-LANGUAGE →
  `review/rv5-err1` (`.worktrees/rv5e`, tip `28bd88a33`) = that + ERR1 with its review fixes; **1,084 tests, 0 failures** →
  `tsuzuki/rel-0.1.1` (`.worktrees/rel-011`, tip `6c0dea4a7`) = that + 0.1.1 changelog + versionCode 25 / versionName 0.1.1 (not built).
  When the device passes are clean: fast-forward `review/rv5` to `review/rv5-err1`, then `main` to it (Batch T step 7), then REL.
- **Do not take:** RV5, ERR1, TTS-LANGUAGE, NT1-BINARY review, REL-0.1.1 steps 1–2 (all done or in Claude's hands).
- **Still open, in order:** (1) device pass A–H on `review/rv5` (running; verdicts for S2-F, RP1, TTS-LANGUAGE come from
  it); (2) ERR1 device script with the APK already built at
  `.worktrees/rv5e/app/build/outputs/apk/debug/app-arm64-v8a-debug.apk` (package `…tsuzuki.rv5debugscratch`);
  (3) close RV5: verdicts in each card, log entry, status table in `docs/design/README.md`; (4) PERF1; (5) REL-0.1.1
  steps 3–5 on `tsuzuki/rel-0.1.1`.
- Scratch builds: `./gradlew -I app/build/review-evidence/rv5/scratch.init.gradle :app:assembleDebug` (init script
  sets the application id; no `build.gradle.kts` edit). Use `--no-parallel --max-workers=2 -Pkotlin.daemon.jvmargs=-Xmx6g`.
- **Owner must decide:** neural voice is out of 0.1.1 unless the lexicon licence is cleared (below and
  `docs/research/neural-tts-clean-lexicon.md`); a stale upstream **local tag `v0.1.1`** (2016 commit `0210fd882`)
  exists — `git tag -d v0.1.1` before tagging the release (the remote only has `v0.1.0`).

**What Codex does next (Claude, 2026-11-14 evening).**
*Case A — Claude is still working (the Device lock line in "Batch AY" names Claude):* app lane only, no adb, no
`main`, don't touch `.worktrees/rv5`, `rv5e` or `rel-011`. Take these in order, each on its own branch from
`review/rv5-err1` unless it says otherwise:
1. **NT1-BINARY-F** (branch `tsuzuki/nt1-binary`, same worktree; T2): add `-ffile-prefix-map` so the `.so` has no
   absolute build paths or user name; bundle the full BSD-3 text for kissfft; make `prepare-source.py` fail on
   extra files in `.native/src`; add a UTF-16 pass to `scripts/audit-neural-runtime.py`. Rebuild, re-run the 13
   tests, record the new hashes. Still no model, no app integration, no S4 merge.
2. **STR1-F** (T1): the CSV export dialog title shows 移行するデータを選択 ("select data to migrate"); give it
   its own `tsz_` string ("select columns to export") in 8 locales. One-line upstream edit.
3. **ERR1-F2** (T2), only what can be settled by reading code and unit tests: "Loading from the source" must not
   be the label for a downloaded chapter/page or for the WebView translation wait (use a neutral "Opening" label
   there; 8 locales); in themed mode the pager/webtoon loading tile should keep showing the real page progress
   percentage that `ReaderProgressIndicator` already receives. Leave the overlay-vs-chapter-end-card question to
   the device pass.
4. **R12-F** leftover (T2): add the missing decode warning RV4 noted; the live-source trust check stays deferred.
Set each to `review` with a Result as usual; Claude (or a different Codex chat) reviews them.
*Case B — Claude has stopped (owner says so, or the Device lock still names Claude with no log entry for a
day):* a **review-lane chat that did not implement S2-F, RP1, STR1, ERR1 or NT1-BINARY** takes over the "Still
open, in order" list below from item 1, following Batch T. Take the Device lock first. The app-lane chat that
wrote those cards must not give their verdicts.
*Either case, do not start:* NT2 steps 2–4, NT3, NT4 (blocked on the lexicon licence and the owner), any anime
card, tagging or pushing.

- **RV5 (`review/rv5`, `.worktrees/rv5`) is Claude's now.** On top of Codex's S2-F + RP1 + STR1 merges:
  `03cd237dd` review: RP1 Listen dock shows the *playing* chapter (was the viewport chapter; `ReaderListenLayout.kt`,
  new pure helpers in `NovelTtsPlayback.kt`); `498430910` review: native autoplay no longer replays the last
  chapter (hand-off anchored on the playing chapter id, not the viewport index; WebView never had the bug);
  `86e640358` merge **TTS-LANGUAGE** (`9fb91e4e3`, finished from the dirty worktree: script check first, classifier
  only for Latin/Han text of 24+ chars, engine without the detected language resets to the device locale).
  `TtsController.kt` conflict resolved keeping both the listening-run tracking and the language job.
  **1,044 tests, 0 failures.** Not yet device-verified: a device pass (checks A–H, evidence files `c-*` in
  `.worktrees/rv5/app/build/review-evidence/rv5/`) was started on the tablet, then the S22.
- **STR1 device check: pass** on tablet and S22 (Dot, Japanese): every Data and storage row and the CSV export
  dialog are Japanese, nothing clipped, no crash (`str1-*.png`). Not seen on a device: the LNReader options dialog
  (it opens only after a file is picked; no file was picked). Nit: the CSV export dialog title is
  移行するデータを選択 ("select data to migrate"), a reused migration string.
- **ERR1: independent review = pass with small fixes** (no blocker). Fixes applied on `tsuzuki/err1` as
  `5f5f49781` (1,051 tests), merged on `review/rv5-err1` as `5a7f1816a`, plus `1ac0b4524` (a late result after a
  per-source retry is ignored), `47e8438d0` (AGENTS touch list), `28bd88a33` (ktlint: parentheses in
  `ErrorCopy.kt`). What the fixes were: titles over-redacted ("Chapter #12" → "Chapter [redacted]");
  global-search Retry navigated instead of retrying (new per-source retry in `SearchViewModel`); `errorCopy`
  could throw `CancellationException` inside composition (non-throwing UI variant); bare HTTP 403 → challenge copy;
  redaction gaps (JSON-quoted secrets, file paths, `user:pass@host`); `NoChaptersException` lost its specific
  message; uncached `errorCopy` in download rows; hidden ComposeView left per pager retry; one vacuous test;
  `NovelViewer.showLoadingIndicator` re-indent. Still to check on a device: whether the opaque WebView overlay
  covers the chapter-end card during a chapter switch; "Loading from the source" shown for downloaded chapters;
  page-progress percentage dropped in themed mode. New upstream file for the AGENTS list besides the six in
  ERR1's Result: `presentation/browse/BrowseSourceScreen.kt`. Then: merge into `review/rv5`, build, device script.
- **NT1-BINARY: independent artifact review = pass.** AAR and .so hashes/sizes match; 0 `espeak`/`piper` bytes
  (ASCII and UTF-16) in the .so; 3 PT_LOAD at 0x4000; DT_NEEDED system libs only; 18 `Java_*` exports (10 TTS,
  8 VersionInfo); 13 Python tests pass. Notes: the patch removes code paths, not GPL code — official 2.0.1 has no
  espeak-ng/piper source, the gate's hits were deprecation text and identifiers; clean strings depend on
  `--gc-sections` (the audit script must stay in release checks); `classes.jar` is the unmodified official one and
  contains "eSpeak" inside `OfflineSpeaker*` class names (jar is not scanned by the gate); ONNX Runtime is a
  third-party prebuilt pinned by hash only; the .so embeds 25 absolute build paths with the Windows user name
  (add `-ffile-prefix-map` before shipping); kissfft's full BSD-3 text is missing; arm64 only (other ABIs must
  fall back to the phone voice; not implemented); never loaded a model. Not merged; S4 stays excluded.
- **NT2 step 1 (lexicon licence): NOT SETTLED → by the Plan 0.1.1 rule, 0.1.1 ships without the neural voice
  unless the owner clears it.** `lexicon-us-en.txt` / `lexicon-gb-en.txt` are a mechanical re-encoding of misaki's
  `{us,gb}_{gold,silver}.json` (sherpa-onnx `scripts/kokoro/v1.0/generate_lexicon_en.py`, `run.sh`). The misaki
  maintainer says (hexgrad/misaki#51) silver = output of espeak-ng 1.52 (GPL-3.0) over dwyl/english-words, gold =
  unnamed "local datasets". No data-specific licence exists; only the repo-level Apache-2.0. Issues
  hexgrad/misaki#107 and #113 ask exactly this and have no maintainer answer. Options: wait for / ask the
  maintainer; legal advice on espeak output; or build our own lexicon from a source with an explicit licence
  (e.g. CMUdict, licence not yet checked). `lexicon-zh.txt` (pypinyin + misaki zh) is unverified too.
- **REL-0.1.1 prep:** changelog draft (22 items for `main` + 5 pending lines) is in `docs/CHANGELOG-0.1.1-draft.md` (uncommitted),
  not yet moved into `CHANGELOG.md`. Its dictionary line promises packs that are not published (R13): reword or publish first.

## Current state (2026-11-14, Codex app lane)

- ERR1 code `ad3e6a871` / handoff `dd945ec72` is ready for independent review: Debug APK + 1,039 tests pass. S2-F, RP1 and STR1 are also review; RV5 is ready for another chat.
- NT1-BINARY `d1e360ff4` + `d580d500e` provides a 7.86 MB arm64 Kokoro-only source-built candidate satisfying the literal/native alignment checks; 13 audit/source tests pass. It has not been integrated or device-tested. NT1 remains pending independent review; NT2's licence/performance/listening gates remain.
- Main stays `0b1c8c889`; no merge or push. Tablet USB and S22 wireless are available per owner. This app lane performed no device QA or installs; device lock stays free. Hourly resume automation stays disabled.

## Current state (2026-11-13, Codex)

- RV2 base `424618de6`: RV2 accepted R15, S3/S3-F and R13; 1,006 tests green. S1 deferred pending explicit extension-trust approval; S2 failed and S2-F remains ready. RV2 evidence archived in `app/build/review-evidence/rv2/`; disposable app removed and the daily tablet Preview restored.
- RV4 accepted V7.7-F, CHAPNUM and MAIN-REPOS after the repository Add fix. Accepted suite: 1,011 tests green, Debug/Preview built, tablet scratch QA complete. This chat now switches to app work S2-F → RP1 → STR1 → ERR1; another chat reviews those cards. S22 deferred by owner; hourly resume reminder stays active.
- TTS-LANGUAGE still has uncommitted implementer work and was skipped. R12-F/R14/NT1 are deferred with explicit evidence/gates below. NT2 requires NT1 merged and NT3 requires the owner listening go; neither gate is waived. Anime remains paused.

## Current state (2026-11-12, Claude)

- **Owner bug 2026-11-12: extension videos don't play.** Reproduced by Claude on the tablet; card **AY-PLAY** (PARITY §7) is first in the anime lane, then **AY9** video previews (PARITY §8).
- **Anime lane: read `docs/ANIME_ANIYOMI_PARITY.md`, then "Batch AY" in the Codex queue.** Claude compared the
  anime code on `tsuzuki/a6.3` with Aniyomi (`97414446b`) and found 15 gaps; the biggest are stream loading (one
  failing hoster fails the episode, the source's own video order and default headers are ignored), no anime
  source settings screen, and subtitle fonts. New order: A6.4 → AY0 → AY1 → AY2 → AY3 → AY4 → AY6 → A6.5 → AY5 → A6.8 →
  AY7 → A6.6 → A6.7 → AY8. A6.9 is replaced by AY0 + AY1 + AY4.
- Unchanged since 2026-11-10: `main` has no anime chain yet (39 commits wait on `tsuzuki/a6.3`); RV2 is open with
  14 findings and no merge verdict; RV3 (anime chain review) has not started; A6.4 has uncommitted work in
  `.worktrees/a6.4`; `main` has uncommitted changes (no default repos in shipped builds, this file).
- S22 is on wireless debugging (owner 2026-11-12). On 2026-11-12 `adb devices` showed only the tablet, so the
  review lane needs the IP:port from the owner (`adb connect`), and must confirm the device with
  `getprop ro.serialno` before installing.

## Earlier state (2026-11-08, Claude)

- **Claude is out of usage (owner, 2026-11-08). Codex now reviews too:** open a third Codex thread with the
  review lane prompt in "Batch T". Waiting for review: app lane V7.6, V7.7, R12, R13, R15, R16, R17, S1, S2, S3
  (RV1, RV2) and the anime chain up to R-A5 on `tsuzuki/r-a5` (RV3). Ten new anime cards (A4.3a, A6.1–A6.9)
  are in the anime lane table.
- **v0.1.0 is released** (2026-11-08, https://github.com/Manojx2005/Tsuzuki/releases/tag/v0.1.0). **The freeze is
  lifted**: `main` takes reviewed cards again, anime included (still behind `TsuzukiAnime.enabled` until A4.3).
- **Codex runs two lanes** (prompts in "Batch S" at the top of the Codex queue): lane 1 = anime chain (A3.5 →
  R-A5 → A3.6 → ... → A4.2 → A5.x), lane 2 = app (V7.6 → V7.7 → R12 → ... → S4). Claude reviews,
  device-checks, merges.
- Claude next: merge `review/post-v01` (R11, HOME-SWIPE, R8, R10) after device checks; merge the anime chain into
  `main` once R-A5 is done; U1 after 0.1.1 is tagged.

## Earlier state (2026-11-07 evening, Claude)

- **Goal: v0.1 release (no anime).** Plan and every card: "v0.1 release — every card" in the Codex queue below.
- V1 merged (`3fa896b3e`): Y2, Y6, R1–R5, R9 are in main. Codex: V1.1 → R6 → R7 → V3–V6, then post-v0.1 cards.
- Claude's next session: V1 leftovers, review everything Codex finished, V2 upstream merge + freeze, V7–V12.

## Earlier state (2026-11-06 evening, Claude — desktop machine)

- Back on the desktop: JDK 21 = Android Studio `jbr` (21.0.10); the Temurin path in older notes is the laptop's.
  Tablet `R52Y90HA77L` + S22 `RZCWA139X1X` connected; owner's phone `R5GL23ZYE1V` is not (it's on the laptop).
- `main` = docs batch Q (`f832df96a`) + this plan. Q1 is being reviewed by Claude now, then merged into main;
  `tsuzuki/q5` gets rebased onto it. Codex runs **Overnight batch Q** below in one thread.
- Anime spike worktree + local branch deleted (owner, 2026-11-06); `origin/codex/anime-spike` kept as backup.

## Claude is working on (Codex: don't touch these)

- Owner 2026-11-07: Codex works on the anime lane only; Claude fixes owner-reported bugs.
- Overnight: every ~2 h Claude reviews cards that reached `review`, builds (only when Codex isn't building),
  device-checks on the tablet + S22 **with the preview build** (`.debug`; the owner's `.dev` data is never used for
  card builds; backup before data-changing cards Q4/Q12; A2.2 only in a temporary `.scratch` install), merges good
  cards into `main` locally and pushes main. Codex never waits on this.

## Codex queue

#### READER-DEFAULTS Narrower book-style reader gutters (owner 2026-11-15) — `review passed (device), not merged` (Claude device check 2026-11-15: tablet ≈17 dp gutters, serif, left-aligned in TextView and scrolling WebView; S22 keeps 16 dp. Paged WebView not checked. Merges with `review/app-chain`) (`tsuzuki/reader-defaults`, `.worktrees/reader-defaults`)

Owner screenshot `C:/Users/Manoj/Downloads/Screenshot_20261115_125306_Tsuzuki.jpg` is the layout reference: keep serif text, left alignment and paragraph spacing, reduce excessive side whitespace and make that the first-install default. Widen the shared tablet column from 600 to 720 dp (Tab S11 portrait default gutters ≈16/17 dp instead of 76/77 dp). Native and WebView, including paged WebView, must agree; preserve saved margins, font, line and paragraph choices. Phones keep their existing 16 dp defaults. No preference migration or forced style reset.

Result (Codex, 2026-11-15): shared `NovelReadingColumn.kt` cap is 720 dp; the tablet classification threshold stays 600 dp, so docked panes/illustrations keep their existing dispatch. Native/WebView scrolling and paging use the same width; saved preferences are untouched. `NovelReadingColumnTest.kt` adds the 753 dp portrait gutter/CSS regression. Scoped Spotless twice clean; Debug and **1,092 app tests pass**, no failures/skips; `git diff --check` clean. Evidence `app/build/reader-defaults/`. No device install by the app implementer; independent device review pending. Implementation commit **`9a483ac31`**.

Independent device review: fresh defaults on tablet portrait/landscape and phone, Native/WebView, scrolling/paged, Dot/Colour; confirm saved asymmetric margins still work.


#### TTS-LANGUAGE Select the automatic voice from chapter text (T3) — `done` (Codex RV5, 2026-11-14)

Owner report from RV2. English text spoken on a Japanese device used the Japanese voice. Detect the language of the text sent to speech in both novel renderers and ask the engine for that language. Keep an explicitly selected voice or legacy voice locale. Check a Japanese device with English and Japanese text and confirm the choice follows the chapter on handoff.

Review (Codex RV5, 2026-11-14): Pass. Retained explicit voice/legacy locale behavior and cancellation across handoff. In Japanese-system scratch apps, Native English selected `en-US` (S22 process trace 14:57:27), WebView Japanese selected `ja-JP` (tablet trace 14:59:22). Earlier explicit English playback and automatic/restore menu evidence inspected. Script/classifier/fallback tests included in the 1,085 passing combined tests. No neural runtime or model integrated.

### Batch T — Codex reviews Codex (owner 2026-11-08; Claude is out of usage) — read this first

Owner decisions (2026-11-08): **a Codex review thread does Claude's review job**: it may merge passed cards into
`main` locally (owner as sole author, no trailers, **no push**) and may use adb on the tablet `R52Y90HA77L` and
the S22 `RZCWA139X1X` (**never** the owner's phone `R5GL23ZYE1V`). The implementer lanes (Batch S lane 1 and 2)
are unchanged: they still commit only on `tsuzuki/<card>` and never touch `main` or adb.

**Review lane prompt:** `Read AGENTS.md and docs/HANDOFF.md. You are the review lane: do the first RV card that is
ready, then the next.` A reviewer never reviews a card it implemented in the same thread.

**How a review card runs (every RV card):**
1. Claim it (`codex-working`, worktree `.worktrees/<rv>`, date). Worktree from current `main` on branch
   `review/<rv>`; copy `local.properties`.
2. For each card in the RV card's list, in order: `git merge --no-ff tsuzuki/<card>`. Conflicts in
   `docs/HANDOFF.md`: keep `main`'s text and copy in only that card's status line + Result. Conflicts in string
   files: union (keep both keys; duplicates → keep one). Any other conflict you can't resolve in a few lines →
   `git merge --abort`, the card fails review ("needs a rebase on main"), go on.
3. Checklist per card (WORKPLAN §0.5 items 1–7): `git diff --stat main...tsuzuki/<card>` stays inside the card's
   files; upstream files have only a small hook (no re-indent, no stray Spotless; restore with
   `git show main:<file> > <file>`), and each new upstream file is added to the AGENTS.md "already touched" list;
   tokens + `MaterialTheme.typography` only; Dot uppercase + red only; real data only; no blocking on main,
   `CancellationException` rethrown, no swallowed errors without a log; strings in all 8 locales with the same
   placeholders. Read the whole diff, not only the Result.
4. Build the merged branch once: `:app:assembleDebug :app:assemblePreview :app:testDebugUnitTest`. Gradle limit:
   at most two builds at once across all threads; on a daemon OOM, wait and rerun.
5. Devices (AGENTS.md "Test on devices"): install the **preview** APK on the tablet and the S22; Dot first, then
   Colour; follow each card's "Claude device check" list; save screenshots to
   `app/build/review-evidence/<rv>/<card>-<device>-<what>.png` and look at them; `logcat -d -b crash` must be
   empty. Before a card that changes data (backup, migrations, library classification), export an in-app backup
   first and restore it after. Anime is hidden in preview: check anime with the **debug** APK on the S22 only, after
   an in-app backup of the `.dev` app (restore if anything changed). If the screen isn't what you expect (owner
   using the device, system picker, permission dialog), don't tap through it: skip that check and write it down.
   Leave the tablet on Dot Home; `svc power stayon usb` on both.
6. Verdict per card, written in that card's section as `Review (Codex RV, date):` with pass/fail, findings as
   `file:line — what`, test counts and evidence paths.
   - **Pass:** small fixes (stray formatting, a missing locale string, a missing AGENTS.md line, an upstream
     re-indent) go in a separate commit on the review branch, `review: <card> <fix>`. Card → `done`.
   - **Fail:** `git revert -m 1` that merge on the review branch (or never merge it); card → `blocked` with the
     findings; add a fix card `<card>-F` (`ready`, same lane, T2/T3) right under it, listing exactly what to change.
7. When every card in the list has a verdict: `git merge --ff-only review/<rv>` from the main worktree (if `main`
   moved, merge `main` into the review branch, rebuild, then fast-forward). Add one log entry (newest first:
   cards merged, cards failed, test counts, evidence folder), set the RV card `done`, update the status table in
   `docs/design/README.md`. No push. Delete the review worktree, keep the branches.



#### RV1 Review: app lane batch 1 (T3) — `done` (2026-11-08)
Cards, in order: **V7.6, V7.7, R12, R16, R17** (all `review`; each branch is 2 commits off `main`). R17 is
tablet-only: check line length on the tablet in portrait and landscape and that the S22 is unchanged.
Review: V7.6, R16 and R17 passed. V7.7 and R12 were reverted, with fix cards below. The full candidate built Debug and Preview and passed 981 tests. After the reverts, Preview built and 977 tests passed with no failures or skips. Both devices launched the accepted Preview. The tablet stayed on Dot Home. The S22 crash buffer contained a UI automation process failure during a dump, with no Tsuzuki crash. Evidence is in `app/build/review-evidence/rv1/`.

#### RV2 Review: app lane batch 2 (T3) — `done` (2026-11-13; R15, S3/S3-F, R13 accepted; S1 deferred, S2 failed)
**Final RV2 evidence:** accepted 1,006 tests and tablet results are recorded in the RV2 log and archived at `app/build/review-evidence/rv2/`. S2-F must revert exclusion `591429e49` to restore S2 before fixing it; merging its already-merged ancestor branch alone will not restore the excluded code. S1 remains deferred pending explicit extension-trust approval.

Cards, in order: **R15, S1, S2, S3, R13**. R15 changes backups and S1 classifies the library: back up first. R13
is T4 (offline dictionary packs): read every storage and download path line by line; the pack itself is not in the
repo, so check the "no pack installed" state and that nothing downloads without a tap.
**QA findings (Codex RV2, 2026-11-10)**

- **RV2-01 · R15 · Japanese backup actions truncate to the same label.** On the S22 in Colour, open Settings → Data and storage and find the backup actions. The earlier candidate showed `バックアップ…` on both tiles. `TsuzukiBackupTiles.kt` constrained these labels to a single ellipsized line. The wrapping change already in the RV2 worktree showed full Create and Restore labels in both Dot and Colour on the S22. See `.worktrees/rv2-batch-t/app/build/review-evidence/rv2/r15-phone-and-boards.png`, `s22-rv2-backup-labels-dot.png` and `s22-rv2-backup-labels-colour.png`. The status-card overlap is a separate issue (RV2-09).
- **RV2-02 · R15 · Scheduled retention can delete a manually saved backup (pre-existing).** Save a manual export with its default filename in the automatic backup directory, then let it age out of the retention window. `BackupCreator.getFilename()` uses the same legacy filename format that `TsuzukiBackupName.kt` accepts for automatic rotation. An executable check against the Preview classes put the manual filename in the deletion list at Keep 1. This check used synthetic filenames and did not delete any backup. See `qa-edge-cases.txt` in the RV2 evidence folder.
- **RV2-03 · S3 · Hard-wrapped plain-text paragraphs are split in the recap.** With two prose paragraphs separated by a blank line and two lines per paragraph, `NovelRecap.kt` returns only the last three lines as individual paragraphs. It drops the first line even though it belongs to the first of the two paragraphs. Reproduced against the Preview classes. See `qa-edge-cases.txt`.
- **RV2-04 · R13 · Incomplete SQLite schema passes pack inspection.** A schema-1 English database with valid metadata and one `term` row passes all of `DictionaryPacks.inspect()`'s queries while missing `exception`, `entry` and `sense`. Given matching manifest bytes, size and SHA-256, installation can replace a working pack with this file. The next English lookup hits a missing table, and `readPack()` reduces the error to an empty result. Reproduced the inspection/lookup SQL against an in-memory fixture. See `qa-dictionary-schema.txt`.
- **RV2-05 · R13 · Common Japanese adjective connective does not resolve to its headword.** A lookup for `高くて` should try `高い`. `DictionaryTerms.kt` currently generates the original form and `高くる`. In a pack with `高い` and no exact surface entry, lookup can miss the adjective or fall back to a shorter unrelated match. Reproduced the candidate generation against the Preview classes. See `qa-edge-cases.txt`.
- **RV2-06 · R15 · Restore offers to import an unrelated gzip file.** On the Tab S11 in Dot, opening `Tsuzuki-RV2-unrelated.tachibk` (gzip containing one unknown protobuf field and no backup records) showed 0 books, 0 shelves, 0 history entries and an enabled Restore button. `BackupFileValidator.kt` returns valid contents when it has recognized zero backup fields. Tested in the separate RV2 Preview package and stopped before restore. See `tablet-rv2-unrelated-restore.png` and the synthetic fixture in the RV2 evidence folder.
- **RV2-07 · S3 · A local novel's previous chapter is missed when there is no recap cache.** On the Tab S11, restore the synthetic local novel with a chapter 1 history entry 48 hours old and both chapter files already in `localnovels`. Open chapter 2 in Dot TextView before opening chapter 1. Chapter 2 starts at its own first sentence with no recap. `NovelRecapSession.prepare()` checks `LocalNovelSource.getPageList()` for a non-null `Page.text`. That source returns a URL-only `Page`, whose `text` defaults to null. The normal local reader calls `fetchPageText()` to open the local file. See `Tsuzuki-RV2-recap.tachibk`, both chapter fixture files, and `tablet-rv2-recap-cold-local-clean.png`.
- **RV2-08 · Reader · The toolbar covers the first highlighted line during TextView read-along.** On the tablet in Dot, open the short local chapter at its start, show reader controls and tap Listen → from the beginning. The highlight appears, but the title bar covers most of the text on the first line. With controls hidden, the line is readable. See `tablet-rv2-listening-playing.png` and `tablet-rv2-recap-cold-local-clean.png`. The same playback capture shows the duplicate `CH. 2: CHAPTER 2: RETURN` label tracked by CHAPNUM and the yellow default TTS highlight previously logged in `docs/RUNTIME_QA.md`.
- **RV2-09 · R15 · Last backup is painted behind the Create and Restore tiles.** On the S22 in Dot or Colour, open More → Data and storage → Backup and restore. The status card is underneath the actions. Dot exposes its border and part of the heading through the gap. Colour's translucent tiles allow the status text and icons to collide with the action labels. `TsuzukiBackupTiles.kt` emits `BackupStatusTile` and the action row as separate roots in a single custom preference item. See `s22-rv2-backup-labels-dot.png` and `s22-rv2-backup-labels-colour.png`.
- **RV2-10 · Data and storage · LNReader backup description is English in Japanese (pre-existing).** Both S22 theme captures show the Japanese row title with an English subtitle. `SettingsDataScreen.kt` hardcodes the fallback subtitle when no import is in progress. The literal is also in the main baseline. See `s22-rv2-backup-labels-dot.png` and `s22-rv2-backup-labels-colour.png`.
- **RV2-11 · Reader · WebView selection has low contrast in Dot.** Long-press a word in the tablet's Dot WebView reader. The selected glyphs become black against a dark blue selection fill. Sampling the captured word gives `#000000` on `#14485C`, about 2.11:1 contrast. See `tablet-rv2-webview-selection.png`.
- **RV2-12 · S3 · A recap changes the resumed text in paged WebView.** A Chromium fixture ran the candidate's recap script, paging script and multi-column CSS at 390 × 650. The same chapter and saved 60% position showed paragraphs 22–24 without a recap and paragraphs 21–23 with one. The card changed the layout from 14 to 15 pages. `paged-reader.js` derives the landing page from the total page count, which includes the inserted recap. The Dismiss button called the bridge once, and repeating the restore after dismissal returned to paragraphs 22–24. This isolates the layout change in Chromium. An Android paged-reader retest is pending. See `check-paged-recap.cjs` and `qa-paged-recap.txt` in the RV2 evidence folder.
- **RV2-13 · Reader · The compact Listen controls have no sleep timer (pre-existing).** Start speech in the tablet's 753 dp portrait reader. The compact dock has hide, previous paragraph, play/pause and next paragraph, with no timer action. The Dot and Colour ListenMini phone boards both show the timer. The only production control that calls `setSleepTimer()` is `TsuzukiReaderListenPanel`, which is mounted for a supported fold or a tablet window at least 900 dp wide. The main baseline has the same restriction. See `tablet-rv2-listening-pause-resume-comparison.png` and `docs/design/boards/png/06-reader/dot/ListenMini.png`.
- **RV2-14 · S2 · One autoplay run can count more chapter completions than the novel contains.** In a fresh tablet scratch install, restore `Tsuzuki-RV2-listening.tachibk` and its two local chapters. Start TextView Listen from chapter 1, pause and resume once, then leave auto-next and infinite scroll on. The later reader capture still showed chapter 1, and Stats reported 5 listened chapters. The fixture contains exactly 2 chapters and neither was explicitly replayed. The capture proves excess completions. The exact handoff sequence still needs a trace. See `tablet-rv2-listening-pause-resume-comparison.png`, `tablet-rv2-listening-followup-after-handoff.png` and `tablet-rv2-listening-followup-stats.png`.

**Verification so far**

The offline JDK 21 `:app:testDebugUnitTest` rerun passed 1,007 tests with zero failures, errors or skips. Both Python dictionary converter tests and both changed reader scripts' JavaScript syntax checks passed. RV2's earlier Debug and Preview builds passed. The tablet opened the already-installed A4.3a Preview to Dot Home without a new crash. RV2 ran on the Tab S11 and wireless S22 in a separate ARM64 Preview package (`io.github.manojx2005.tsuzuki.rv2scratch`). On the tablet, the valid synthetic backup preview showed 3 books, 1 shelf and 1 history entry. Restore populated the scratch Library, and Manhwa showed only the fixture tagged Webtoon, with the Manhwa badge. TextView and WebView selections both opened the no-pack dictionary sheet with system and web fallbacks. TTS finished the synthetic chapter and Stats recorded 1 chapter and 0 whole listening minutes. Replaying it in incognito left the listening count at 1. The S22 displayed both complete Japanese backup action labels in Dot and Colour. Create saved a 532-byte scratch backup and the Restore picker loaded its preview without applying it. The status text with the actual date and size still overlapped (`s22-rv2-backup-after-export.png`). Neither device logged a scratch-process crash. Evidence is in `.worktrees/rv2-batch-t/app/build/review-evidence/rv2/`.

Both temporary installs and fixture folders were removed. The tablet returned to its existing Preview on Dot Home. R15, S3 and R13 are held for the findings above. Their follow-up cards are R15-F, S3-F and R13-F. S1 still needs a live Keiyoushi title and the manual override check. S2 still needs background playback, timer, pause/resume and chapter handoff on device. Positive recap rendering, dismissal and paged resume also remain open. RV2 has no merge verdict yet.

#### RV2 fix plan (Claude, 2026-11-10) — read before taking R15-F, R13-F or S3-F

Claude read the code behind the RV2 findings in `.worktrees/rv2-batch-t` (branch `review/rv2`, all five cards
merged). Causes below are from reading the code. **Rows 1–7 are applied in the worktree but uncommitted and never built** (11 files, see "RV2 — how to finish"). Work on `review/rv2`
(one `review: <card> <fix>` commit per card), so RV2 can close without three more branches. Whoever takes a row
writes their name on it first. Each fix needs a unit test that fails before it.

| # | Finding | Cause (file) | Change | Owner |
|---|---|---|---|---|
| 1 | RV2-09 status card behind the tiles | `TsuzukiBackupTiles.kt`: status tile and action `Row` are two roots inside one `CustomPreference` slot, so they overlap | Wrap both in one `Column`. Keep the uncommitted label-wrap edit (RV2-01) | Claude |
| 2 | RV2-06 unrelated gzip accepted | `BackupFileValidator.validate`: unknown fields are skipped and nothing checks that any field was recognised | Count fields 1, 2, 101, 104, 105, 106; zero recognised → `IOException(invalid_backup_file_unknown)`. Settings-only backups stay valid | Claude |
| 3 | RV2-02 retention deletes manual exports | `TsuzukiBackupName.backupFileRegex` accepts the short `HH-mm` name that `BackupCreator.getFilename()` gives manual exports | Rotate only the scheduler's `HH-mm-ss-SSS` names. Old short-named automatic backups are then never rotated (safe side); update `TsuzukiBackupNameTest` | Claude |
| 4 | RV2-03 hard-wrapped text split per line | `NovelRecap.novelRecapParagraphs`: plain text uses `text.lines()` | Split on blank lines when the text has any; fall back to lines only when it has none | Claude |
| 5 | RV2-07 local novel has no recap without cache | `NovelRecapSession.prepare` reads `Page.text` from `LocalNovelSource.getPageList()`, which returns URL-only pages | Call `fetchPageText(page)` on the local source | Claude |
| 6 | RV2-05 `高くて` misses `高い` | `DictionaryTerms.japaneseEndings` has no `くて` rule; the generic `て` → `る` gives `高くる` | Add `"くて" to listOf("い")` before `"て"`; test in `DictionaryTermsTest` | Claude |
| 7 | RV2-04 incomplete pack passes inspection | `DictionaryPacks.inspect` queries only `metadata` and `term` | In `inspect`, run the real lookup join (`term` → `entry` → `sense`, with `ordinal`, `part`, `gloss`, `headword`, `reading`) and require one row; for English also `SELECT form, lemma FROM exception LIMIT 0`. A missing table or column throws `SQLiteException`, which `readPack` already turns into "invalid", so the working pack is kept | Claude |
| 8 | RV2-12 recap shifts the paged WebView resume | `paged-reader.js` derives the landing page from the total page count, which includes the recap card | Restore by anchor/offset of the chapter body, not by page fraction of the whole document; re-run `check-paged-recap.cjs`, then Android | Codex (S3-F) |
| 9 | RV2-14 listened chapters over-counted | Not traced. Suspect the chapter-finished callback firing per hand-off attempt with infinite scroll + auto-next | Trace `TtsController` hand-off with the RV2 fixture; count a chapter once per (chapter id, playback run) | Codex (new card S2-F) |
| 10 | RV2-08 toolbar covers the highlighted line | Not traced; TextView read-along scroll ignores the top bar height | Offset the scroll target by the visible top bar | Codex (reader polish card) |
| 11 | RV2-10 English LNReader strings | `SettingsDataScreen.kt` L269, L307, L385–387 hardcode English (Tsundoku's code) | New `tsz_` strings in 8 locales; smallest possible upstream edit | Codex (T1) |
| 12 | RV2-11 Dot WebView selection contrast | Not traced; `NovelWebViewStyler` sets no `::selection` colours for Dot | Token-based `::selection` background/text | Codex (reader polish card) |
| 13 | RV2-13 compact Listen dock has no sleep timer | `TsuzukiReaderListenPanel` (the only caller of `setSleepTimer()`) mounts only ≥ 900 dp or on a fold | Add the timer action to the compact dock per board `06-reader/dot/ListenMini` | Codex (reader polish card) |

After rows 1–7: `:app:testDebugUnitTest` and `:app:assemblePreview` on `review/rv2`, then the scratch-package device
checks from the RV2 card (S22 Dot + Colour backup screen; unrelated-gzip restore; local-novel recap on the tablet).
Still open for the RV2 verdict after that: S1 with a live Keiyoushi title and manual override; S2 background
playback, timer and hand-off (blocked by row 9); recap rendering and dismissal in both renderers (blocked by row 8
for paged WebView). S1 can merge alone if its device check passes; S2 and S3 wait for rows 8–9.

**Also uncommitted on `main` (Claude, owner request 2026-11-10): no default repos in shipped builds.**
`tsuzuki/DefaultSourceRepos.kt` seeds only when `BuildConfig.DEBUG` (release, preview and foss add nothing; debug
keeps seeding — owner confirmed). Repos an earlier build added are left alone. New test in
`DefaultSourceReposTest`; README step 4, CHANGELOG (Unreleased) and the AGENTS.md Sources rule updated. Not yet
compiled or tested. Device check needed: a fresh preview install shows an empty Browse whose empty state leads to
"add repository" on the novel and manga tabs. Review-lane scratch installs now start with no repos.

#### RV2 — how to finish (Claude, 2026-11-12; review lane)
The worktree `.worktrees/rv2-batch-t` (branch `review/rv2`, R15 + S1 + S2 + S3 + R13 merged) holds Claude's
uncommitted fixes for fix-plan rows 1–7: `TsuzukiBackupTiles.kt`, `BackupFileValidator.kt`, `TsuzukiBackupName.kt`,
`DictionaryPacks.kt`, `DictionaryTerms.kt`, `NovelRecap.kt`, `NovelRecapSession.kt` and four test files. They were
never compiled. Don't discard them.
1. Read the diff (`git diff`) against the fix-plan table. Each row needs a test that fails without the fix; rows
   2 (unrelated gzip), 5 (local-novel recap) and 7 (incomplete pack) have **no test yet** — write them
   (`BackupFileValidatorTest`, `NovelRecapSessionTest`, `DictionaryPacksTest`; follow the existing tests' style).
2. `git merge --no-ff tsuzuki/s3-f` (row 8, paged recap resume). Conflict in `NovelRecap*.kt` → keep both changes.
3. `./gradlew :app:testDebugUnitTest :app:assemblePreview`. Fix what your review finds; one commit per card:
   `review: R15 <fix>`, `review: S3 <fix>`, `review: R13 <fix>`.
4. Devices, in the scratch package `io.github.manojx2005.tsuzuki.rv2scratch` as before (never the owner's
   `.debug` or `.dev` data): re-run RV2-01, -02, -03, -05, -06, -07, -09 and -12; each must now pass. Then the
   checks RV2 never reached: S1 with a live Keiyoushi long-strip title + manual override; S2 background playback,
   timer, pause/resume; recap shown and dismissed in TextView, WebView scroll and WebView paged.
5. Verdicts. R15, S3, R13 pass if their findings are gone. **S2 fails on RV2-14** (over-counted chapters) unless
   you can show it is fixed: revert its merge, card S2-F below. S1 passes or fails on its device check alone.
   Findings RV2-08, -10, -11, -13 are pre-existing: they don't fail a card; they are RP1 and STR1 below.
6. Close RV2 as in Batch T step 7 (fast-forward `main`, log entry, status table, no push).
The S22 is on wireless adb: get the IP:port from the owner if `adb devices` doesn't show it, and confirm
`getprop ro.serialno` = `RZCWA139X1X` before installing.

#### RV4 Review: fix cards + owner reports (T3) — `done` (2026-11-13; V7.7-F, CHAPNUM, MAIN-REPOS accepted; other gates deferred)
**Final validation 2026-11-13:** Candidate 1,026 tests; accepted Debug + optimized scratch Preview built, **1,011 tests, 0 failures/errors/skips**, scoped Spotless twice. Tablet-only checks per owner; crash buffer empty; both scratch apps removed and daily Preview returned to Dot Home. Evidence archived and size-verified (225 files) in `app/build/review-evidence/rv4/`.

Cards, in order: **V7.7-F, R12-F, R14, CHAPNUM, TTS-LANGUAGE, NT1 (= S4), MAIN-REPOS**. (S3-F is merged in RV2.)
- V7.7-F and R12-F replace the cards RV1 reverted: merge `tsuzuki/v7.7` then `tsuzuki/v7.7-f`, `tsuzuki/r12` then
  `tsuzuki/r12-f`, and re-run the RV1 checks that failed (WebView chapter-end tap; source health with a native
  Keiyoushi source).
- R14 is T4 (merge sources for one book): back up first; read every DB write line by line.
- CHAPNUM and TTS-LANGUAGE are `codex-working` with commits on their branches. If the worktree has uncommitted
  work, the implementer thread is not finished: skip the card and say so. TTS-LANGUAGE device check: Japanese
  device, an English chapter is read with an English voice, a Japanese chapter with a Japanese voice, an
  explicitly chosen voice is kept.
- **MAIN-REPOS**: the uncommitted change in the **main checkout** (`DefaultSourceRepos.kt`, its test, README,
  CHANGELOG, AGENTS.md: no default repositories in shipped builds, owner 2026-11-10). Review it like a card: run
  `DefaultSourceReposTest`, fresh preview install shows an empty Browse whose empty state leads to "add
  repository" on the novel and manga tabs, debug still seeds. Then commit it on `main` as
  `Ship no default repositories outside debug builds`. `docs/HANDOFF.md`, `docs/ANIME_ANIYOMI_PARITY.md` and
  `docs/design/boards/fixes-2026-11b/` are also uncommitted on `main`: commit them as `docs: plan 0.1.1, boards 2026-11b`.
Review (Codex RV4, 2026-11-13): Accepted V7.7-F and CHAPNUM. MAIN-REPOS committed on main as `7f697fef9`; owner plan/boards as `3e610bd08`. Review fix `7f51d31da` restores the manga Add Repository action in Tsuzuki themes: fresh Preview has no repositories on either tab, both add flows open; Debug still seeds LNReader/Keiyoushi; 7 seed tests pass. R12-F deferred for native live-source trust approval (and missing decode warning), R14 deferred for two-source runtime checks, S4/NT1 deferred for literal native-string gate clarification; exclusions `89e1ac9ac`, `451168815`, `911a2222f`. TTS-LANGUAGE skipped because its worktree remains dirty. S3-F already passed RV2. Native bottom-card overlap is pre-existing and added to RP1. No push.

#### Neural TTS in 0.1.1 (owner 2026-11-12) — read before NT1–NT4
The owner wants the offline natural voice (Kokoro through sherpa-onnx, S4) **shipped in 0.1.1**. It is optional:
system TTS stays the default and the fallback, the voice pack is a download and never part of the APK, and the
conditions in `docs/research/neural-tts.md` stay hard rules (sherpa-onnx ≥ 2.0.1, no espeak-ng anywhere, lexicon
voices only, licences in About). **Three things can stop it, and each has a rule:**
- The licence of `lexicon-us-en.txt` / `lexicon-gb-en.txt` is UNVERIFIED in the research. We may not publish our
  own pack until NT2 settles it. Not settled → 0.1.1 ships without the neural voice (the code stays, the entry
  stays debug-only) and nobody decides otherwise but the owner.
- Speed has never been measured on a phone. NT2 measures it. Slower than real time at 1.5× on the S22 → same rule.
- Nobody has heard it. The owner's listening test (NT2 step 4) is the go / no-go.

#### NT1 Review S4 (review lane) — `blocked` (NT1-BINARY candidate awaits independent review, 2026-11-14)
Review `tsuzuki/s4` as a T4 card: the private process, the AAR's SHA-256 against the research doc, no
`espeak`/`piper` string in any packaged `.so` (`strings | grep -i -E 'espeak|piper'`), 16 KB alignment, licence
files present, system TTS untouched when the opt-in is off. Device: opt-in with no pack → system fallback.
Don't download a model. Pass → merge (still debug-only).
Review (Codex RV4, 2026-11-13): See S4 review below. Pinned AAR contains literal espeak/piper matches; owner clarification pending. This gate is not waived.

#### NT1-BINARY Investigate a runtime satisfying the literal native-string gate (T3) — `done` (independent review passed 2026-11-14, see NT1-BINARY-F; scripts in `main`; was: .worktrees/nt1-binary, 2026-11-14; d1e360ff4 + d580d500e; independent review required)

Inspect current official sherpa-onnx Android artifacts and source build options for a lexicon-only binary with no literal `espeak`/`piper` substring in packaged native libraries. Keep the raw gate unchanged, distinguish deprecated references/Speaker false matches from actual linked GPL code, and document reproducible source/dependency/string/16 KB checks for any candidate. Do not merge S4, publish a pack, download a model or waive NT2/owner listening gates. Another chat reviews any implementation produced here.
Result (Codex app lane, 2026-11-14): **review**, implementation `d1e360ff4` plus patch-whitespace rule `d580d500e` on `tsuzuki/nt1-binary`. Investigation produced a source-built candidate; **not an NT1 approval or app dependency change**.

- Audited all three official v2.0.1 Android AAR variants, SHA-256-verified against release metadata. Normal: 12/16 libraries fail the literal string gate; static-runtime: 4/5; RKNN: 6/10 have string matches and two additional libraries fail 16 KB alignment. The raw rule still rejects deprecated references and CreateSpeaker substring matches; no gate was relaxed.
- `scripts/neural-tts/kokoro-only-v2.0.1.patch` is a seven-file source profile: TTS/version JNI only, Kokoro factory/config only, a required lexicon, no legacy phonemizer directories, no external token/phoneme input routes, no unused legacy metadata reads. Removed APIs/paths are unavailable rather than renamed. Modified version/tag identity and Apache modification notices are retained. ONNX Runtime and C++ are statically linked; normal stripping removes debug information, with no post-link string edits.
- `scripts/build-kokoro-runtime.ps1` + preparation/packaging helpers pin source commit/archive and ten dependency hashes, preserve unknown edits, and rebuild under `.native/`. A cached **offline recipe run passes**. Official Java classes are byte-identical and **all 10 TTS/GeneratedAudio JNI exports match**. Candidate assets include modification/runtime/dependency notices. There are no app Gradle/source changes or voice models.
- Candidate `.worktrees/nt1-binary/.native/output/sherpa-onnx-2.0.1-tsuzuki-kokoro-arm64.aar`: **7,857,879 bytes**, SHA-256 `b2cee72b9131cf5190753c966e23e0c0e65ef7a0e9313e434d074d4aa3b5fa47`. JNI: **20,903,096 bytes**, SHA-256 `9aa11f94d53ad3bac95ae84e6349e3ff0486f63bba39154e10dbde5e6532ff6f`. Zero literal matches/runtime markers, three 0x4000-aligned PT_LOAD segments, DT_NEEDED only Android system libraries. Independent NDK LLVM strings/readelf agree; identical packaging inputs reproduce the AAR hash. Cross-path/toolchain native byte reproducibility is not established.
- **13 Python tests pass** (10 artifact checks, 3 source-integrity checks). Evidence `.worktrees/nt1-binary/app/build/nt1-binary-evidence/`; committed summary `docs/research/neural-tts-native-audit-20261114.json`; full recipe/limits `scripts/neural-tts/README.md`. No model downloaded, no synthesis or device test, no S4 exclusion revert, no app install, no merge/push.
- Independent reviewer: inspect patch/dependencies/notices; rebuild and compare the TTS API, raw string scan, ELF dependencies/alignment. Integrate only on a disposable review branch with S4 (excluded by `911a2222f`); this AAR is **arm64-only**, so unsupported ABIs must keep Android TTS. Verify worker/no-pack fallback and APK zipalign separately. Successful artifact checks do not settle the lexicon licence, performance, quality or owner listening decision. NT1/NT2/NT3/NT4 gates remain.


#### NT1-BINARY-F Native build hygiene follow-up — `done` (Claude review 2026-11-14: pass; build scripts merged into `main`, runtime not integrated)

Result (Codex, code `d904693d2`): completed the queued source/artifact follow-up, ready for independent review. This is not an NT1 acceptance verdict, inference result, lexicon licence waiver or S4 merge.
- `build-kokoro-runtime.ps1` maps source/SDK paths, defaults to two native jobs and uses source-built ONNX exclusively. `build-onnx-runtime.ps1` pins ONNX 1.28.2 source, portable CMake 3.31.6 and host protoc 21.12; verifies extracted files; builds default targets plus RE2; combines 81 Android archives and emits component hashes. Environment overrides for Sherpa's static ONNX input are restored after configure/build. No linked-library string editing or forbidden-symbol renaming.
- `prepare-source.py` rejects unknown edits/extra files before extraction; pinned legacy cache archives are hash-checked if present. Pristine mode validates ONNX/tool ZIP contents and rootless ZIPs. An old unlisted Eigen 3.4 archive was rejected and preserved at `.native/archives/unused-eigen-3.4.0.tar.gz`, not silently permitted.
- `audit-neural-runtime.py` checks ASCII plus UTF-16 LE/BE at both byte alignments. NT1's literal/alignment verdict is preserved; a separate build-hygiene result and `--require-build-hygiene` enforce no build paths. URL/runtime-path and random four-byte code fragments are regression cases. The intermediate candidate with 548 inherited ONNX CI paths is rejected by the strict mode.
- `package-runtime.py` requires the complete kissfft copyright/BSD-3 text, validates the pinned ONNX source/static hash manifest, includes that manifest in the AAR, and refuses retained build paths. Official `classes.jar` is byte-identical. Licences and ONNX third-party notices match the pinned upstream sources. Research summary and design status updated.
- **Final AAR:** `.native/output/sherpa-onnx-2.0.1-tsuzuki-kokoro-arm64.aar`, **7,853,259 bytes**, SHA-256 `1f70ec2b28f40dfe760a5fe1283e623a1163155c5c530b5a70ae06df1eae4ec1`. **JNI:** 20,887,080 bytes, `ef5ccbfc500fefb8a75100fd0f4fe0528d14669c0dad096781f7b7d13df71b24`. Combined ONNX archive SHA-256 `da1db20d213c68641a48e79b8a99e1c6557859b85186e87f5bf140848b463e50`.
- **Verification:** full ONNX source compilation and complete offline recipe passed; **21 Python tests, zero failures**. All 1,009 ONNX compiler commands and selected Sherpa commands carry prefix mapping. Strict audit has zero ASCII/UTF-16 literal or build-path hits. Independent NDK strings plus a broader source-file-path scan find none; username occurrences are zero in ASCII/UTF-16. NDK readelf: three PT_LOAD at 0x4000, DT_NEEDED only libandroid/liblog/libm/libdl/libc. 18 Java exports, ten TTS/GeneratedAudio JNI methods match official. Two packaging writes are byte-identical; no model entries in AAR.
- Evidence `app/build/nt1-binary-evidence/f-*` and `ort-*`: final independent JSON, readelf/exports/strings, repeat packaging, tests, full build/configure logs; intermediate prebuilt-ORT candidate and linker failures retained. Host protoc works outside the sandbox (the sandbox produced a DLL-relocation failure). Initial JNI link caught omitted RE2; recipe corrected and full retry passed. No stale failed log is treated as success.
- **Reviewer:** inspect changed scripts/notices and the source summary, independently rebuild with the pinned SDK/NDK and `-Offline` only after its caches exist; rerun strict artifact audit and compare API/classes/licences. Recheck tool/dependency provenance and path scans. Full native-byte reproducibility across machines/toolchains was not established; repeated packaging with identical inputs was. No adb, app dependency, voice pack, lexicon, Gradle edit or app integration; synthesis, non-arm64 fallback, APK packaging, performance and owner listening gates stay with NT1/NT2/NT3.

Review (Claude with an independent subagent, 2026-11-14): **pass.** Re-measured, not taken from the Result: AAR and `.so` sizes and SHA-256 match; 21 Python tests pass; strict audit exit 0; an independent scan of the `.so` finds 0 hits for `espeak`, `piper`, the Windows user name, `C:\`, `/Users/`, `/home/`, `.worktrees`, `Desktop` in ASCII and UTF-16 LE/BE; three PT_LOAD at 0x4000; DT_NEEDED = libandroid, liblog, libm, libdl, libc; 18 `Java_` exports; all 1,009 ONNX compile commands carry the prefix map; the three downloads are hash-pinned with no skip path; no built binary is edited. Notes, none blocking: (1) no single log shows one clean from-scratch build (full ONNX build log + RE2 link fix + final incremental link); (2) the audit's exit code covers build paths only with `--require-build-hygiene` (the recipe and the packager both enforce it); (3) `classes.jar` equality with the official AAR was confirmed indirectly (the pinned official AAR is not on disk; sibling official 2.0.1 AARs have the same hash). Not checked by anyone: inference, cross-machine reproducibility, app integration. `tsuzuki/nt1-binary` (scripts and docs only, no app or Gradle file) is merged into `main` so NT-LEX1 can build on it. **This does not unblock the neural voice**: the lexicon licence (NT2 step 1) and the owner listening test still gate it; S4 stays excluded.

#### NT2 Neural TTS: licence, speed and the listening test (T3) — `ready` (needs NT1 merged; app lane; **gate for NT3/NT4**)
1. Lexicon licence: find the origin and licence of the two English lexicon files in
   `kokoro-int8-multi-lang-v1_1` (sherpa-onnx `tts-models` release notes, the k2-fsa scripts that generate
   them, the upstream dictionary they derive from). Write the finding with links into
   `docs/research/neural-tts.md`. Allowed outcome: a licence that permits redistribution (then add its text to
   `docs/licenses/neural-tts/`). Otherwise `blocked` with the facts; do not guess.
2. If step 1 passes, build the test pack with `scripts/package-kokoro-test-pack.py` (downloading the upstream
   archive from the official sherpa-onnx release is allowed for this card; verify its SHA-256 against the
   release page and record it).
3. Measure on the S22 and the tablet with the debug build (review lane runs it if you have no device access):
   real-time factor for a 180-character chunk at 1×, 1.5× and 2×, time to first audio after tapping Listen, peak
   memory of the worker process, battery over 30 minutes of listening compared with system TTS. Table in the
   research doc. Budget: first audio ≤ 2 s, no gap between chunks at 1.5×, worker ≤ 400 MB.
4. Leave the pack side-loaded on the S22 debug build and tell the owner how to listen (which chapter, which
   toggle). **Owner go / no-go** is written under "Owner decisions in force".
Result: _pending_

#### NT3 Neural TTS for everyone: engine in release builds + Voices page (T4) — `blocked` (needs NT2 go; board 5 `Voices` approved by the owner 2026-11-13)
Board 5 in `docs/design/boards/fixes-2026-11b/`. Move the engine from the debug source set to main:
1. Packaging: the sherpa-onnx AAR for **arm64-v8a only** in release/preview/foss (other ABIs show only the phone
   voice tile, no download). Record the APK size before and after per ABI in Result; more than +20 MB on arm64 →
   stop and ask the owner. Keep rules for the JNI classes; 16 KB check; licences in About (V5's screen).
2. Pack manager `tsuzuki/tts/pack/`: manifest (URL, size, SHA-256, version) fetched from the Tsuzuki release
   asset NT4 publishes; download on Wi-Fi by default with pause/resume/cancel, free-space check (2× pack size),
   SHA-256 verified before install, atomic swap, delete, survives process death (WorkManager). Nothing downloads
   without a tap. Follow R13's pack code where it does the same job (and RV2's findings about validating before
   replacing).
3. Voices page per board 5: engine tiles, download states, voice rows with a one-sentence sample, "dialogue in
   a second voice" (quoted speech uses the second speaker; off by default), delete. Same entry from the Listen
   panel. 8 locales.
4. Routing: the neural voice is used only when the chapter's detected language (TTS-LANGUAGE's detector) is one
   the pack covers; otherwise the phone voice, with one line in the Listen panel saying which voice is speaking.
   Any synthesis failure falls back to the phone voice for that chunk, as S4 does.
5. Parity: sleep timer, read-along highlight, pause/resume, paragraph seek, chapter hand-off, listening stats
   (S2/S2-F) and the listening widget behave the same on both engines; one test per item with a fake engine.
6. Pronunciation: the per-book replace rules (existing ReplaceRules screen) are applied before synthesis on both
   engines; pauses: 250 ms after a paragraph, 600 ms at a scene-break line; footnote markers and URLs skipped.
Device script: fresh install → Voices → download on Wi-Fi, pause, resume, kill the app mid-download, finish →
pick a voice, hear the sample → Listen on an English chapter (neural) and a Japanese chapter (phone voice) →
airplane mode still works → delete the pack → Listen falls back. Dot and Colour, phone and tablet.
Result: _pending_

#### NT4 Publish the voice pack (review lane prepares; owner uploads) — `blocked` (needs NT2 go)
Build the final pack zip (no `espeak-ng-data/`, licence files inside), its SHA-256 and the manifest JSON, and
write the exact upload steps for the owner: a GitHub release on the Tsuzuki repo tagged `voices-v1` with the zip
and the manifest as assets. **Only the owner creates the release.** After the upload, verify the public URL and
hash from a device and put the URL in NT3's manifest constant.
Result: _pending_

#### S2-F Count a listened chapter once (T3) — `done` (Codex RV5, 2026-11-14)
RV2-14: one autoplay run counted 5 chapters for a 2-chapter book. Trace `TtsController`'s chapter hand-off with
infinite scroll + auto-next on (log each "chapter finished" with chapter id and a playback-run id). Count a
chapter at most once per (chapter id, playback run); a run ends on stop, on a manual chapter change, or when the
reader closes. Tests: hand-off fired twice → one count; replay after stop → second count; incognito → none.
Device script in Result (review lane runs it): the RV2 fixture `Tsuzuki-RV2-listening.tachibk`, expected Stats = 2.
Result (Codex app lane, 2026-11-13): `2f7b56a4c`; S2 restored on this branch by `252ff29b8` (do not separately revert its main exclusion when merging S2-F).

- `ListeningSession.kt` associates queues with chapter IDs and a playback-run ID. Repeated queues for a completed chapter count once until the run ends; real speaking duration still accumulates. Debug completion logs include chapter, run, queue and whether the completion was counted.
- `TtsController.kt` ignores duplicate/interrupted completion callbacks before scheduling auto-next. Pause/resume and seek preserve the run; Stop/manual navigation and viewer destruction end it. Incognito callbacks still hand off but do not write listening data. Unknown chapter IDs retain playback/time behavior without inventing a chapter count.
- `NovelWebViewViewer.kt` preserves the run for its explicit autoplay full-document handoff; user-driven navigation retains the normal Stop path.
- Tests cover repeated same-chapter queues, duplicate callback acknowledgement, two distinct chapters, pause/seek, replay after Stop, stale callbacks, incognito, mid-queue privacy changes, unknown chapter IDs, and the controller's stop/close/sleep-timer paths. Debug build + **1,024 unit tests, 0 failures/errors/skips**, two scoped Spotless passes; five restored labels present once in all eight locales. Evidence `.worktrees/s2-f/app/build/s2-f-evidence/`.
- No adb used in this app card. The exact device event sequence behind RV2-14 remains for independent review; the code fix and deterministic regression do not claim a runtime pass.

Independent reviewer device script (tablet for now; S22 deferred by owner):
1. Use a disposable install, restore `app/build/review-evidence/rv2/Tsuzuki-RV2-listening.tachibk`, and copy its `listen-1.txt` / `listen-2.txt` into the dedicated SAF folder's `localnovels/RV2 Listening Test/`. Enable infinite scroll and auto-next; choose an installed English system voice explicitly while TTS-LANGUAGE is pending.
2. Start chapter 1, pause/resume once, let both chapters finish. Capture `TTS chapter finished` logs: the run ID must stay the same across queue and chapter handoffs; only two distinct chapter IDs may have `counted=true`. Stats listened chapters must be **2**, in both renderers. Any automatic stop/restart that changes the run unexpectedly needs a follow-up trace.
3. Stop and explicitly replay one chapter: it starts a new run and adds one completion. Repeat in incognito: totals stay unchanged. Check timer stop and manual chapter navigation end the previous run. Restore/remove only the disposable app and leave daily data alone.


Review (Codex RV5, 2026-11-14): Pass. Reviewed restoration and queue/run accounting, retaining both timer constructor parameters and test sets. Prior tablet Native/WebView and S22 replay/incognito evidence plus this takeover’s S22 Native pause/resume trace show two distinct completions in one run, followed by Stop after the final chapter; no last-chapter replay. Combined suite: 1,085 tests, zero failures/errors/skips. Evidence: `app/build/review-evidence/rv5/` (`c-tablet-A-native-trace.txt`, `c-tablet-E-webview-trace.txt`, S22 Stats/replay/incognito captures and `s22-rv5-caseb-*`).

#### RP1 Reader polish from RV2 (T3) — `done` (Codex RV5, 2026-11-14)
1. RV2-08: during TextView read-along the top bar covers the highlighted line. Scroll target = line top −
   (visible top bar height + 16 dp). No board needed.
2. RV2-11: Dot WebView selection is black on dark blue (2.1:1). Set `::selection` and the TextView highlight
   colour from tokens (board 3, "Selection" row). The yellow TTS highlight becomes the token colours in board 4.
3. RV2-13: the compact Listen dock has no sleep timer. Add the timer chip per board 4 (`TabletListenDock`) and
   the phone board `06-reader/ListenMini`; it calls the same `setSleepTimer()` as `TsuzukiReaderListenPanel`.
4. RV4-01: the Native chapter-end card can remain under visible bottom reader bars at maximum scroll. Reserve the measured visible bottom chrome height plus 16 dp in scroll content so Next, Mark read and Contents stay tappable; remove the extra space when chrome hides. Keep story progress independent of the card. Existing Q2 ChapterEnd / TabletReader boards apply. Evidence `app/build/review-evidence/rv4/tablet-rv4-preview-native-secondary-actual-bounds-20261113.png`; hiding chrome exposes the card and Contents opens correctly. Card vertical layout is unchanged by V7.7-F, so this is a pre-existing polish issue.
Tests: scroll-offset maths; timer chip label formatting; selection CSS contains the token colours.
Result (Codex app lane, 2026-11-13): **review**, code `54810e270` on `tsuzuki/rp1`. Native read-along uses measured top chrome plus 16 dp; chapter-end scroll content reserves measured visible bottom chrome plus 16 dp and releases it when hidden. Shared token feedback drives Native selection/ink and both renderers' read-along; WebView selection CSS overrides source CSS. New board-based phone/tablet Listen dock shows the existing sleep choices and a live countdown from the controller deadline; actual chapter/paragraph/speed only. Thin AppBars slot preserves legacy UI. Three strings translated in all 8 locales. Debug APK and **1,017 tests** pass (zero failures/errors/skips), scoped Spotless twice; evidence `.worktrees/rp1/app/build/rp1-evidence/`. No adb or self-review.

Independent reviewer: check Dot/Colour Native highlighted line with bars opening during speech, selection fill/ink/handles in Native and WebView, final card Next/Mark read/Contents with bottom bars shown/hidden and unchanged story progress. Check timer Off/Chapter/15 minutes, actual decreasing countdown and pause/resume, phone dock fit and tablet controls. Read from here remains available in reader actions. S22 deferred by owner. On merge with S2-F, retain both ListeningSession and elapsed-clock constructor arguments and both timer-test additions; neither card subsumes the other.

Review (Codex RV5, 2026-11-14): Pass with review fixes `03cd237dd`, `498430910` and `0a96ace3d`. The dock follows the playing chapter; Native autoplay stops at the final chapter. The first line now has actual top clearance (the previous clamped scroll target still clipped it at chapter start), and measured floating Listen height is included in bottom clearance. Dot/Colour, tablet/S22, Native/WebView selection/read-along and timer evidence inspected. S22 Native card Next/Mark read/Contents exercised above both dock and bars; story progress remains 100%. Countdown visibly decreases (15:00 → 14:57; Colour 14:55). 1,085 tests pass. Evidence `app/build/review-evidence/rv5/`, notably `s22-rv5-caseb-first-line-fixed.png`, `s22-rv5-caseb-card-clears-dock.png`, `s22-rv5-caseb-preview-colour-readalong.png` and prior `c-tablet-F-*`.

#### STR1 Translate the LNReader import strings (T1) — `done` (Codex RV5, 2026-11-14)
RV2-10: `SettingsDataScreen.kt` L269, L307, L385–387 hardcode English. New `tsz_` strings in all 8 locales;
smallest possible edit to the upstream file. Check the same screen for any other literal.
Result (Codex app lane, 2026-11-13): `77ede839d` localizes the LNReader chooser, started status, import preference and entire options dialog. The same-screen audit also found CSV export labels; those now use resources too. 18 new `tsz_` keys in all 8 locales, existing Categories/Downloaded chapters reused. Upstream edit is 21 line replacements; no layout changes. `:app:compileDebugKotlin` and two scoped Spotless passes succeed. XML audit confirms 18 unique keys per locale and no remaining UI string literals in this screen. Evidence `.worktrees/str1/app/build/str1-evidence/`. No new tests for this copy-only change; no adb. Reviewer: Japanese Data screen chooser title, temporary import-started status, import-options dialog and CSV options, then another app language. No actual owner backup import required.

Review (Codex RV5, 2026-11-14): Pass with `bf5055dcd`: CSV now has its own “Select columns to export” title in all 8 locales. Japanese LNReader options/chooser/data evidence inspected; Japanese and English CSV dialog labels verified on S22. Eight-locale key uniqueness and XML audit pass; 1,085 combined tests pass. Evidence `app/build/review-evidence/rv5/` (`tablet-rv5-str1-japanese-options.png`, `s22-rv5-caseb-str1-ja-csv.png`, `s22-rv5-caseb-str1-en-csv.png`). No owner backup imported.

#### ERR1 Errors and long waits say what happened (T3) — `done` (Codex RV5, 2026-11-14)
Boards 1 `ErrorState` and 2 `LoadingStages`. One composable pair in `presentation/tsuzuki/components/`
(`TsuzukiErrorState`, `TsuzukiLoadingStages`) and one pure mapper `tsuzuki/error/ErrorCopy.kt`: Throwable →
(what failed, why in one sentence, technical line, primary action). Cases: no network, timeout, HTTP 403/503 with
a Cloudflare/DDoS-Guard challenge → "Open in WebView", HTTP 404, HTTP 5xx, parse error ("the site changed; update
the extension"), extension missing/untrusted, storage full/permission, anything else → the exception's message,
never "Unknown error". Technical line: status · message · host, no query string. Use it in: novel and manga
catalogue + global search, book details refresh, chapter open in both novel renderers and the manga reader,
downloads list rows. Dispatch from the existing upstream empty/error composables with a small hook
(`presentation/components/EmptyScreen` pattern), no re-indent. LoadingStages appears only after 2 s, and turns
into ErrorState after 30 s on one step. 8 locales. Tests: the mapper table; redaction; 2 s / 30 s thresholds.
Device script: airplane mode on a catalogue; a Cloudflare-protected source; a removed extension's book.
Result (Codex app lane, 2026-11-14): **review**, code `ad3e6a871` on `tsuzuki/err1`. Independent review required; no merge or push.

- `tsuzuki/error/ErrorCopy.kt` classifies wrapped network/timeouts, 403/503 browser challenges, 404/server/parse failures, unavailable or untrusted extensions, full storage and denied access. Display and clipboard diagnostics remove URL paths/queries, credentials, auth/cookie headers and stack frames; cause chains and message lengths are bounded. Normal cancellation propagates; the reader's timeout is classified as a timeout.
- `presentation/tsuzuki/components/TsuzukiErrorState.kt` implements approved boards 1/2 using shared tiles and pills, typography and tokens. Two-second delayed stages, per-step seconds and 30-second timeout; retry/new-operation/new-step resets the clock. The shared tile keeps its original positional API, with an additional overload for these boards' 24 dp corners.
- `TsuzukiOperationState.kt` adapts catalogue/global-search/details, with working extension/storage routes where hosted navigation is available. Details keeps cached data and shows the real refresh failure. Global-search Retry opens the same source with the current search query. Only observable steps appear; the adapters do not invent source/parse/layout completions.
- `TsuzukiReaderOperation.kt` adapts Native, WebView and manga holders. WebView loading/error UI sits outside the document so it cannot be spoken or counted as story progress. A full WebView failure stops speech/autoscroll; real document readiness removes the overlay. Native actual inline load failures reuse the card; WebView inline diagnostics reuse the mapper and token colours. Existing retry/WebView handlers remain wired. Search clocks reset on query change; manga clocks reset on reuse/hide.
- Novel/manga download rows show concise mapped causes and copy redacted diagnostics. **23 new keys in all 8 locales**, with identical placeholders. Debug APK and **1,039 tests, 0 failures/errors/skips**, including **28 mapper/redaction/threshold tests**, pass. Two scoped Spotless passes succeeded; unrelated upstream formatter edits restored, then final APK/tests rerun. Evidence `.worktrees/err1/app/build/err1-evidence/` (`build-tests-final.log`, `tests-final.txt`, `locale-audit.json`, formatter logs). All edited Kotlin uses UTF-8/LF.
- Independent device review: tablet + S22, Dot then Colour. Offline catalogue: cause and Retry; browser-challenge source: WebView then Retry; removed extension's cached book: Extensions route. Slow request: spinner before 2 s, real stage/seconds after, timeout at 30 s, retry/leave/reopen and new search reset it. Check Native/WebView chapter failure/retry and inline next-chapter failures; ensure error text is never spoken and story position is retained. Manga paged/strip: failed page retry, WebView, recycling/scroll back. Download rows: localized cause and copied diagnostic has no query/token. Check portrait/landscape and larger font sizing. No app-lane runtime verdict is claimed.
- Reviewer: add newly touched upstream adapters to AGENTS' touch list when merging: `presentation/browse/MigrateSearchScreen.kt`, `ui/reader/viewer/ReaderProgressIndicator.kt`, `viewer/pager/PagerPageHolder.kt`, `viewer/webtoon/WebtoonPageHolder.kt`, `viewer/text/textview/NovelTextViewInlineFeedback.kt`, `viewer/text/webview/NovelWebViewInlineFeedback.kt`. Other hooks are in already-listed files. Watch the Native loading-view expression formatting; no upstream body was wrapped/reindented as a new dispatch.

Review (Codex RV5, 2026-11-14): Pass with the prior independent review fixes and `8570ff5d7`. Reader waits now use neutral Opening copy, manga progress reaches the themed loading tile, and WebView keeps its chapter-end snapshot while target pages are being prepared (including repeated loaded-state callbacks). Controlled localhost fixture verified stages at 4s, timeout, HTTP 403 → WebView, HTTP 500 → reader error → Retry recovery. Tablet missing-source cached details → Extensions; S22 Native missing local chapter → Retry recovery after restoring the synthetic file. A synthetic failed download showed its Japanese cause, Copy action and successful Retry. Manga paged offline error and Retry exercised; DNS failure persisted briefly online, then images rendered in webtoon mode. Webtoon render/recycling smoke checked; a separate forced webtoon error, live external browser challenge, inline-append failure and full large-font/landscape matrix were not established by this sample. These remain broader runtime QA coverage, not claims of this verdict. Debug, optimized Preview and 1,085 tests pass; Preview 16 KB APK alignment passes, SHA-256 `6f519bc92772f28e31f7ca5db8796dac2be497feaf48d6b85554c61a34dcea96`. Evidence `app/build/review-evidence/rv5/`. Both final crash buffers empty; daily Preview/Debug and owner phone untouched.

#### RV5 Review app fixes for 0.1.1 (review lane) — `done` (Codex independent Case B takeover, 2026-11-14)

Plan 0.1.1 step 6: review **S2-F → RP1 → STR1 → ERR1** using Batch T. All four are now `review`.
Per-card independence: saved chat `01a1d049-7422-7173-a22c-a8a917b0b110` ("Follow Plan 0.1.1 review and app lanes") implemented S2-F/RP1/STR1. This distinct chat `01a1d800-cbe7-7272-91e5-d9c406331a99` may review those three. ERR1 and NT1-BINARY were implemented here and must be reviewed by a different chat. Do not merge either here. Use current `main` as base; no push. Tablet USB and S22 wireless
are available per owner 2026-11-14; confirm the wireless device serial before installing. Device lock applies.
S2-F already restores its excluded ancestor on its branch; do not independently revert the main exclusion.
When combining S2-F and RP1, retain both ListeningSession and elapsed-clock constructor parameters and both
timer-test additions. Merge eight-locale resources by key. Each card's Result has its exact runtime script.
Back up/use disposable data as Batch T requires; unrelated extension trust gates remain in force.
NT1-BINARY's candidate is a separate independent review; it does not silently enable S4 or waive NT2.
Result: accepted S2-F, RP1, STR1, ERR1 and TTS-LANGUAGE; final review fixes `0a96ace3d`, `bf5055dcd`, `8570ff5d7`. Debug + optimized Preview + 1,085 tests pass. Per-card verdicts below/above; evidence archived to `app/build/review-evidence/rv5/`. Batch T fast-forward complete at `88bcf6e4e`; no push. Daily-app relaunch was rejected by automatic approval review, so daily apps remain untouched; disposable Dot Home is used for handback.

Case B takeover (Codex independent review lane, 2026-11-14): owner explicitly assigned Case B; this chat implemented none of the review cards. Device lock claimed; resume A–H, ERR1 runtime checks, verdicts, then PERF1 and REL preparation. Main remains unchanged pending verdicts.
- Case B progress: baseline `review/rv5-err1` Debug + optimized Preview + 1,084 tests passed. S22 verified `RZCWA139X1X`; both scratch installs upgraded only. S22 Native pause/resume trace counted chapter 1 and 2 once in run 1 and stopped after chapter 2. Remaining first-line/bar clipping reproduced on S22 (default reader size 16px, system font scale 1.0); small review fix on rv5e reserves top scroll inset outside story coordinates, device capture now clears the bar. Review fixes also restore manga page percentage, use neutral reader loading copy, preserve WebView chapter-end transition ownership and give CSV columns their own title in 8 locales. Fix Debug + 1,085 tests pass; accepted in `0a96ace3d`, `bf5055dcd`, `8570ff5d7`. Scoped formatter twice, clean diff check.
- ERR1 runtime: tablet offline MangaFire shows Japanese connection copy; Wi-Fi restored and Retry loads catalogue. Synthetic localhost custom source exists only in scratch app (`files/custom_sources/rv5-caseb.json`, adb reverse 18765); controlled 40s request shows stages at 4s and timeout; HTTP 403 offers WebView and opens localhost correctly. Evidence `caseb-*` / `*-rv5-caseb-*` under `.worktrees/rv5/app/build/review-evidence/rv5/`; new build/formatter logs under rv5e equivalent. Main unchanged; no push. Synthetic fixture server must be stopped and reverse removed after QA.

RV5 progress (2026-11-14, this independent reviewer): review branch merges S2-F `0bf63eaec`, RP1 `85a7b5782`, STR1 `17ce3afaf` on claim `ff55d86d8`. Both timer constructor arguments and tests retained; locale conflicts unioned. Debug + optimized Preview + **1,030 tests, zero failures/errors/skips** pass. First parallel compiler run exhausted memory; retry with `--no-parallel --max-workers=2 -Pkotlin.daemon.jvmargs=-Xmx6g` passed. All 26 added keys match placeholders across eight locales; no new duplicate keys. Evidence `.worktrees/rv5/app/build/review-evidence/rv5/`. Daily Preview/Debug untouched: both devices use separate `io.github.manojx2005.tsuzuki.rv5debugscratch` installs with synthetic RV2 listening backup in `Documents/Tsuzuki-RV5-Scratch`. S22 endpoint `192.168.40.243:44823`, verified hardware serial `RZCWA139X1X`.
- Tablet native pause/resume, 15-minute countdown, two-chapter handoff: chapter 1/run 1/queue 2 and chapter 2/run 1/queue 3 counted=true; repeated chapter 2/queue 4 counted=false. Stats screenshot shows **2 listened chapters**. Baseline crash buffers empty. S22 native run also started, paused/resumed, English `en-us-x-iom-local` explicitly chosen on both. Remaining: capture S22 final Stats, WebView, replay/Stop and incognito, optimized Preview and STR1 flows. No final verdicts yet.
- Findings to disposition before verdicts: tablet native autoplay repeats chapter 2 after the last chapter. Trace `tablet-native-full-tts-trace.txt`: at 13:28:15 controller playback index=1/id=2, but `moveToLoadedNextChapterForTts anchor=0` selects loaded nextIndex=1 again. This is upstream native handoff behavior, beyond count deduplication, and needs a follow-up card. RP1 compact dock shows CHAPTER 1 while Chapter 2 paragraph 3 is highlighted/speaking (`tablet-rv5-native-after-handoff.png`); `ReaderListenLayout.kt:165` chooses viewport title instead of controller playback chapter. Do not claim either behavior passed.
- Owner explicitly said keep both devices awake: stayon true and screen_off_timeout=2147483647 on both. Tablet rotation was free, temporarily locked portrait with `wm user-rotation lock 0`; restore free when appropriate. Devices remain claimed for ongoing RV5 review.

#### PERF1 Speed budgets on `main` (review lane; needs devices) — `done` (2026-11-14; review/perf1, base main 88bcf6e4e)
Measure on the S22 with the preview APK and write the numbers, method and date into `docs/PERFORMANCE.md`:
cold start (10 runs, median; budget 500 ms), next novel chapter from the chapter-end card (budget 200 ms to
first text), Library scroll with the 500-book synthetic backup (Y4) — janky frames from `dumpsys gfxinfo`
(budget under 5 %), opening book details (budget 300 ms). A missed budget becomes a card; don't tune here.
Result: Optimized Preview, S22, Y4 500-book database. Cold Macrobenchmark 10-run median 741.2 ms (miss); local Native Next first readable text approximately 180–190 ms in 3 video samples (limited pass, close to timing uncertainty); Library 25/1,397 janky frames = 1.79% (pass; selected shelf 17 books); cached details first title median 58 ms but full transition about 510 ms (miss). Methods, scope and evidence in docs/PERFORMANCE.md. No tuning; follow-up cards below.

#### PERF-START Cold startup exceeds S22 budget — `done` as an investigation (Claude review 2026-11-14: pass, merged; the 500 ms miss is still open → **PERF-START-2**)
Trace-led investigation of PERF1: optimized Preview on accepted RV5 main, Y4 500-book
scratch database, baseline-profile-required Macrobenchmark 10 cold launches, median
initial display 741.2 ms versus 500 ms. Compare like-for-like before/after, retain the
WebView warm-up ordering documented in PERFORMANCE.md, and measure Home readiness
separately. Evidence: app/build/review-evidence/perf1/benchmark-output/. Do not infer
regression size from older four-book am-start measurements.


Result (Codex, 2026-11-14; `tsuzuki/perf-start` commit `b98a5e706`, `.worktrees/perf-start`): investigated all ten original PERF1 traces locally, no adb or production tuning. Main-thread medians: application bind 391.2 ms (includes dex open 104.3 ms and Chromium sync 140.5 ms), first traversal 166.4 ms, Activity create 32.4 ms. Inclusive wall intervals cannot be added across nesting. The benchmark's original 741.2 ms remains the budget verdict; Perfetto system interval boundaries differ.
- `scripts/analyze_startup_traces.py`: reproducible per-trace SQL, retained raw slices/health, missing-package rejection, summary ranges and sample counts. Ten-trace analysis + Python compilation + negative package check pass.
- `StartupBenchmarks.kt`: separate benchmark-process `TsuzukiHomeObserved` section, waits for seeded English/Japanese Home and startup overlay absence. Includes shell/accessibility overhead; not an app-internal fully-drawn time. Initial-display and frame metrics retained.
- `docs/PERFORMANCE.md`: attribution, reproduction, limitations and next measurement. `docs/design/README.md`: investigation status only. Main-thread WebView warm-up retained before source graph construction. No release artifact/profile/screen changed.
- JDK 21 helper compile and `:baseline-profile:assembleNonMinifiedRelease` pass; two scoped Spotless passes clean. Whole-module formatting encounters pre-existing `constants.kt` filename lint, so used absolute-path `spotlessIdeHook`. Helper APK SHA-256 `178bd7b251753c849f1e0e83651a637a65562845498c7cea6065ec541e42c96e`. Logs/JSON `.worktrees/perf-start/app/build/perf-start/`.
- **Independent review still required:** run helper on S22 same optimized PERF1 scratch APK/Y4 fixture, ensure ten nonzero Home-observed samples and compare initial display; if experimenting with compilation modes, use identical APK/data and retain all outputs. No new runtime measurement, before/after improvement or budget pass claimed. This card's speed target stays open; this review is for the investigation/helper, not a completed optimization.

Review (Claude, 2026-11-14): **pass, merged into `main`.** Read `StartupBenchmarks.kt` and `analyze_startup_traces.py`: the old metrics are kept, the new section only wraps the existing wait plus the overlay-gone wait, the script only reads traces (package name is SQL-escaped, arguments go through an argv list). Nothing in the app changed. Not run on a device by this review: the helper's ten non-zero `TsuzukiHomeObserved` samples are **PERF-START-V** for the review lane. What the numbers say: about 390 ms of the 741 ms is spent before the Activity exists (140 ms of it the WebView user-agent warm-up in `App.kt`), so the budget cannot be met by trimming Home alone.

#### PERF-DETAILS Details transition exceeds S22 budget — `done` (Claude review 2026-11-14: pass, 235–292 ms on the S22; merged `c0a12f2c4`; in 0.1.2 unless the owner asks for 0.1.1)
PERF1 cached details become readable in 49–115 ms, but the visual transition settles
in approximately 502–511 ms versus the 300 ms opening budget. Review the intended board
motion and record an explicit readiness definition before changing timing. Preserve
Dot/Colour behavior and compare optimized Preview on the same Y4 fixture. Evidence:
app/build/review-evidence/perf1/details-*.mp4. Video timings are estimates, not dispatch traces.

Owner decision (2026-11-14, this chat): change the design so the **entire opening finishes within 300 ms**. Preserve the total-open target; first readable content is not a substitute. Implementation in `.worktrees/perf-details` uses a 200 ms synchronized cover/fade, pending independent device measurement.


Result (Codex, 2026-11-14; `tsuzuki/perf-details` commit `3f15ad172`, `.worktrees/perf-details`): **review**, implementation complete; 300 ms total-open budget requires independent device proof.
- Opened MotionOpen/MotionSpec PNGs and decoded their offline HTML timing: shared cover/background 450 ms. Owner explicitly chose a new **300 ms complete-opening budget**, superseding the archived opening timing; `docs/design/specs.md` records that decision.
- `TsuzukiMotion.kt` and `TsuzukiSharedCover.kt`: details opening uses a 200 ms synchronized screen fade and shared-cover bounds animation, reserving 100 ms for preparation. One composition-local duration supplies both covers and the screen fade; back navigation/other screens retain 450 ms, reduced motion 150 ms. No upstream hooks, layout, colours, fonts, strings or data changed.
- `TsuzukiMotionTest.kt`: route/direction and reduced-motion regressions; **1,087 app tests, zero failures/errors/skips**, Debug assembly, optimized Preview assembly and two scoped Spotless passes pass. All Kotlin LF. Exact final test source precedes the result XML timestamp.
- Optimized ARM64 Preview package is `io.github.manojx2005.tsuzuki.rv5debugscratch`, versionCode 25; SHA-256 `a6fa1660d5385d9d4c70783a547a73067166df4e3cbe550f88fe01ab0d1c9b1a`. ZIP 16 KB alignment passes; baseline.prof/profm packaged. Artifact `.worktrees/perf-details/app/build/outputs/apk/preview/app-arm64-v8a-preview.apk`; logs/excerpts/test summary `app/build/perf-details/` in that worktree. Known non-fatal Compose mapping/XML service/optional window-extension warnings retained.
- **Independent device review:** S22, optimized baseline archive `app/build/review-evidence/rv5/rv5-arm64-preview.apk` SHA-256 `6f519bc92772f28e31f7ca5db8796dac2be497feaf48d6b85554c61a34dcea96`, then this candidate on the same scratch Y4 fixture. Dot then Colour, three original-PTS recordings each, `[SYNTHETIC] NOVEL 481` from shelf 01; first input feedback to final cover/screen position and opacity/outgoing Library absent is the total-open measurement. Report readable-title time separately. Also check real-cover sharing, interrupted opening/Back, reduced motion and tablet rail. Full protocol/uncertainty in PERFORMANCE.md. No adb/install/screenshot done by this implementer; no measured pass claimed, no main merge/push or release-candidate replacement.

Review (Claude with a device subagent, 2026-11-14): **pass, merged.** Review fix `260691fba`: `LocalCoverDuration` is a dynamic `compositionLocalOf` (a static local turns off skipping for both screens each time the value changes, at the start of the animation being measured). Rebuilt optimized scratch Preview (SHA-256 `5321203d…abd4`), 14 `TsuzukiMotionTest` tests pass. S22, three recordings per theme, first changed frame → settled: Dot 284 / 235 / 235 ms (last differing frame 292 / 249 / 244), Colour 251 / 243 / 251 ms (263 / 252 / 260); ±8 ms. Budget met; the first open after launch is within one frame of the line. Return about 440 ms, smooth. Tablet: two-pane details looked normal (2-book library, no timing). Crash buffers empty. Not tested: a real cover image, reduced motion. Table in `docs/PERFORMANCE.md`; evidence `app/build/review-evidence/perf-details/`. **Found while testing → BACK1:** Back pressed right after the tap does not close details, and the next Back shows Home instead of the Library (3 of 3). Not known whether this is older than the card.

#### PERF-START-V Run the Home-observed startup helper on the S22 (review lane, T2) — `done` (Codex 2026-11-14; .worktrees/review-perf-start-v; build preparation only until Device lock is free)
On `main` at or after `7defdb569`. Run the cold startup benchmark in `StartupBenchmarks.kt` on the S22 against an
optimized scratch Preview built from the same `main` commit, with the Y4 500-book fixture (PERF1's command is in
`app/build/review-evidence/perf1/s22-cold-baseline.log`). Pass = ten iterations, each with one non-zero
`TsuzukiHomeObserved` sample next to the initial-display value. Write all twenty numbers and the APK hash into
`docs/PERFORMANCE.md` as the baseline for PERF-START-2. If the section metric comes back empty, set `blocked`
with the benchmark output; don't patch the helper in this card.

Preparation (Codex review lane, 2026-11-14): main `9f1b87ced` built in `.worktrees/review-perf-start-v` (`review/perf-start-v`). Optimized scratch Preview and `:baseline-profile:assembleNonMinifiedRelease` both pass (3m43s). ARM64 Preview SHA-256 `db7f5496f7225dd582bef404a86f68f8c5f7cbd31c2e99ef393fc8b548eb0566`, target `io.github.manojx2005.tsuzuki.rv5debugscratch`; benchmark helper SHA-256 `178bd7b251753c849f1e0e83651a637a65562845498c7cea6065ec541e42c96e`. Paths in that worktree: `app/build/outputs/apk/preview/app-arm64-v8a-preview.apk`, `baseline-profile/build/outputs/apk/nonMinifiedRelease/baseline-profile-nonMinifiedRelease.apk`; build log and artifact manifest `app/build/review-evidence/perf-start-v/`. No adb run: Device lock still names Claude for BACK1. Once it says free, claim it, confirm S22 hardware serial and Y4 500 books, then install scratch/helper and run ten cold baseline-profile-required iterations. No measurements yet, no helper patch. PERF-START-2 measurements and candidate refresh remain pending behind this gate.

Review (Codex, 2026-11-14): **pass.** S22 confirmed RZCWA139X1X, optimized main `9f1b87ced` scratch Preview, Dot Home with 500 books inspected. All ten cold Partial/Require iterations have exactly one nonzero Home-observed section plus initial display. Initial display median 698.614 ms (647.388–772.369), Home-observed median 3282.650 ms (3161.579–3333.075); observer overhead is included, not app fully-drawn. All twenty values and APK hash recorded in PERFORMANCE.md. No thermal-throttle sleep, no helper patch, no crash observed. Evidence `app/build/review-evidence/perf-start-v/` (archived in root). Device lock retained for PERF-START-2.

#### PERF-START-2 Cold start: three measured changes (T4) — `blocked` (Claude subagent, 2026-11-14 night, `.worktrees/perf-start-2`, `tsuzuki/perf-start-2`; Codex independent review .worktrees/review-perf-start-2, 2026-11-14; for 0.1.2; review lane: read, then measure on the S22; **not measured, no speed-up claimed**)
PERF-START found 391 ms in application binding (140 ms of it the WebView user-agent warm-up), 166 ms in the
first traversal, 32 ms in Activity create; the budget is 500 ms and the benchmark says 741 ms. Make **each change
its own commit that builds alone**, so the review lane can measure them one at a time. No adb in this card.
1. **Warm-up after the first frame.** Today `App.onCreate` calls `WebSettings.getDefaultUserAgent` on the main
   thread before the modules load, because an extension calling it on a background thread while the main thread
   waits on the Injekt source graph deadlocks (comment in `App.kt`). Keep that guarantee a different way: post
   the warm-up to the main thread after the first frame (with a time limit so it always runs), and make the
   background block that first touches `SourceManager` / extension loading wait for it. Write down every place
   the main thread can build the source graph before the warm-up and why each is safe (on the main thread the
   call cannot deadlock, it is only slow). Add a unit test for the ordering gate. If any path can still build
   sources on a background thread before the warm-up, don't ship the change; say so in Result.
2. **Name the first traversal.** Add `androidx.tracing` sections (no behaviour change) around the startup
   overlay, the Home screen and its tiles, the nav bar/rail, and font loading, so the next trace says where the
   166 ms goes. No tuning in this commit.
3. **One change the new sections justify**, only if the existing ten traces already point at it; otherwise
   leave it out and say so.
Don't edit the baseline profile, the dex layout or the splash design. Result must list, per commit, what the
review lane should measure (PERF-START-V's method, same APK settings, same fixture) and what would count as a
regression (splash freeze, missing sources on Home, first search failing).
Review lane after it: measure baseline, then each commit; a change that doesn't move the median by more than
the run-to-run range is dropped. If all three still miss 500 ms, record the numbers and ask the owner whether
the S22 budget moves.

Result (Claude subagent, 2026-11-14): **review.** No adb; nothing measured. 1,093 tests, 0 failures; Debug builds.
- `d1950daa5` **warm-up after the first frame.** New `tsuzuki/startup/StartupWarmUp.kt` (pure gate: run once, main thread only, opens even if the warm-up throws) and `WebViewWarmUp.kt` (first resumed activity → one-shot pre-draw listener → post to the main handler; time limit 1,000 ms from `Application.onCreate`). `App.kt`: the inline call is replaced by `WebViewWarmUp.install(this)` (main process only). `ExtensionLoader.kt`: `runNowIfMainThread()` at the top of `loadExtensions`, `awaitBeforeSources()` at the top of the suspend `loadExtension`. 8 unit tests.
- `eb5e21fa7` **trace sections** (`presentation/tsuzuki/startup/TsuzukiTrace.kt`, `android.os.Trace`): startup overlay, Home and each tile, nav bar and rail, theme colours/typography/tokens, the two font family builders. Composition only. **It re-indents about 1,000 lines in `TsuzukiHomeScreen.kt`, `TsuzukiNav.kt`, `TsuzukiStartup.kt`** (`git diff -w` is small).
- `43cd71ad1` docs. Commit 3 skipped: the ten traces don't point at one composable.
- Why the subagent says the gate is enough: extension sources are constructed only in `ExtensionLoader.loadExtension`, whatever thread builds `SourceManager`/`ExtensionManager`; all four callers are gated; nothing on the main thread blocks on source initialisation; local, custom and JS-plugin sources never call `getDefaultUserAgent`.
- **Risks to check before any merge:** (1) `NetworkHelper` → `AndroidCookieJar` → `CookieManager.getInstance()` can now run on a background thread before the warm-up; whether that needs the UI thread was not verified. A splash freeze would point here first. (2) `LibraryUpdateJob` calls `sourceManager.get()` without waiting for initialisation; a job-started process now gets its sources up to 1 s later. (3) The 140 ms moves to just after the first frame; it may show as a hitch or make Home-observed worse. (4) Whether the pre-draw listener fires on the real first Compose frame with the Android 12 splash.

Claude's review notes (2026-11-14, before the review lane): **commit 2 is a diagnostic build only: measure with it, never merge it** (the re-indent would conflict with STR2 and A11Y1 on the same files; if the sections are wanted long-term, redo them without re-indenting). Review-lane order: PERF-START-V baseline on `main` → `d1950daa5` alone (initial display, Home-observed, and 20 cold starts watching for a splash freeze; also start the app from a library-update notification/job with the process dead, and run a first Browse search) → `eb5e21fa7` for the trace only. Merge `d1950daa5` only if the median moves by more than the run-to-run range and none of the four risks shows; otherwise leave the branch and record the numbers.

Review (Codex independent review, 2026-11-15): **not accepted; no app code merged.** `d1950daa5` alone built on current main as isolated `879959859`; optimized scratch Preview + 1,095 tests pass. S22 ten cold Partial/Require runs each: immediate baseline repeat initial display median **740.891 ms**, range 649.267–815.010; candidate **556.113 ms**, 418.006–674.533. Home-observed (observer overhead included) baseline **3337.493 ms**, 3206.650–3432.810; candidate **3176.401 ms**, 3019.051–3657.318. All observer counts one, thermal sleep zero. Initial gain 184.778 ms is below candidate run range 256.527; Home gain 161.092 below both baseline range 226.160 and candidate range 638.266. Strict merge rule fails; 500 ms still missed. Keep 500 ms target (owner delegated judgment), add follow-up rather than relaxing it.
- Twenty additional cold starts reached populated Dot Home; screenshots and sampled video inspected, no sustained freeze, empty crash buffer. Cold first Browse search returned the local Listening fixture. No usable remote source configured; no new extension trusted, remote constructor/search coverage remains open.
- Process-dead library-update job check: verified absent scratch PID, forced newly scheduled library-update job 9, process restarted and WebView warmed without freeze. WorkManager deferred the periodic worker body until due, so this does **not** verify cold source fetching. Original automatic-update Off restored and job 9 canceled; pre-job backup restored, BackupRestoreJob SUCCESS. Backup has 501 records / 50,002 chapters / 997 history / 30 shelves (500-book library plus extra reading fixture).
- Source findings: APK class construction is gated, but Custom/JS sources can initialize NetworkHelper/CookieManager before the gate; LibraryUpdateJob reads sources without waiting for initialization; pre-draw posting does not guarantee a presented frame. These coverage/ordering risks remain unresolved; no broad safety claim from the smoke test.
- `eb5e21fa7` only on disposable diagnostic branch as `52d0aa909`, built/measured **after** the production candidate. Never merged. Ten traces name the requested sections. First-composition medians: Dot fonts 37.546 ms, typography 37.924 ms (nested), Home phone 9.850 ms, shelf 1.105 ms, overlay 0.362 ms, nav 0.352 ms. Composition only, no summing nested/deferred groups; no third tuning change justified. Exact samples/hashes/limitations in PERFORMANCE.md; root-archived evidence `app/build/review-evidence/perf-start-2/` includes source audit, raw traces/parser warnings, logs, backup and 20-launch recording.

#### PERF-START-2-F Cold startup ordering and a stable measurable gain (T4) — `blocked` (independent changes requested on `2c772793d` and `45cf42deb`)
The reviewed experiment remains unmerged; main still has the synchronous main-thread UA warm-up. Keep the 500 ms target. Read PERF-START-2's verdict and PERFORMANCE.md. Rework the experiment only if all WebView-triggering source-graph paths are safe (including eager CookieManager initialization) and an actual presented-frame boundary can be established; cold background library updates must await usable sources before looking them up. Add meaningful ordering tests. Trace additions, if retained, must use small hooks without wholesale re-indentation and remain balanced around early returns. Do not merge `eb5e21fa7`. Reviewer repeats same-fixture optimized baseline/candidate runs: initial-display gain beyond observed run ranges, Home-observed not regressing, 20 cold launches, actual cold library-update worker-body execution and first Browse search. Existing live-extension trust gate still applies; do not bypass it. No profile/dex/splash design changes. Do not claim a gain from fonts/composition sections alone; measure full traversal and preserve real UI readiness.

Progress (Claude 2026-11-20, uncommitted): **part 1 — cold background updates await usable sources — done.** This is a real bug on `main` today, independent of the warm-up move: `LibraryUpdateJob` looked up sources (`sourceManager.get`, line 342) without waiting for `AndroidSourceManager.isInitialized` (extensions + JS plugins), so a WorkManager run in a cold process could skip books whose source was still loading. New `tsuzuki/update/SourcesReady.kt` `awaitSourcesReady(isInitialized, 30 s)` (returns at once when loaded; on timeout the run goes on as before, so one broken extension can't hold it), called once in `LibraryUpdateJob.doWork` after `setForegroundSafely()` (one line in an already-touched upstream file). `SourcesReadyTest` 3 pass (immediate, waits then continues, gives up at the limit; virtual time). Scoped Spotless clean; full app suite **1,220 tests, 0 failures/errors/skips**. Not run on a device (card: no devices in implementation) — reviewer: kill the process, run the library-update job cold, confirm every source's books update. **Part 2 — audit done (read-only, Claude 2026-11-20): moving the warm-up is NOT safe as designed; don't rework it without the gate below.** The deadlock class (App.kt:111–116) is: a background thread loads the WebView provider first (via `WebSettings.getDefaultUserAgent` *or* `CookieManager.getInstance()`) while holding Injekt's singleton lock, and the UI thread blocks on that lock. Paths that load the provider off the UI thread, found on `main` `3ce5d2425`: (a) **App.kt:136–137, `launchIO { Injekt.get<NetworkHelper>() }` at every cold start** → `NetworkHelper` → `AndroidCookieJar` (core/common `AndroidCookieJar.kt:10`, `private val manager = CookieManager.getInstance()` — eager, in the constructor). Today it runs *after* the synchronous warm-up; moved, it would run first, every launch. (b) App.kt:139 `Injekt.get<SourceManager>()` on the same IO block → extension sources whose constructors call `getDefaultUserAgent`. (c) Coil (App.kt:250–267) resolves `NetworkHelper().client` lazily on its own threads for the first cover fetch (Home shelf). (d) JS plugins: `JSLibraryProvider.kt:285/368` `CookieManager.getInstance()` on plugin threads. (e) Cold-process workers (library update, downloads, restore) that build `NetworkHelper`. (f) `CloudflareInterceptor` creates a WebView, but posts to the main thread (safe). **A safe version needs one gate that (a)–(e) all await before their first `NetworkHelper`/source/CookieManager touch**, with the warm-up on the UI thread after a real presented frame plus a time limit, and the UI thread must never block on `NetworkHelper`/`SourceManager` before that gate (it can't wait on a gate it hasn't opened). (c) is the hard one: gating Coil delays every first network cover. Recommendation: treat the 140 ms as fixed for 0.1.2 and look for the 500 ms elsewhere (the 166 ms first traversal), unless the owner wants the gate. **Part 3 (presented-frame boundary) not started**, since part 2 says the move shouldn't ship as is. **Part 4 — a different saving, font warm-up (Claude 2026-11-20, uncommitted, same worktree):** the 2026-11-15 trace put ~37.5 ms of first composition in building the theme's font families (`LnrTypography.kt` `TsuzukiTypographies.dot/colour`, built once per process behind a lock). `App.onCreate` (main process only) now launches one IO coroutine calling `lnrTypography(uiPreferences.appTheme.get(), resources)` before the existing async init, so the first composition finds the families cached; if it gets there first it waits on the same lock, which is never slower than today. Legacy theme values return null (no-op). No WebView/network touch. Compiles; scoped Spotless clean. **Not measured (card: no devices in implementation).** Reviewer: same-fixture optimized baseline vs candidate on the S22, initial display and the `Dot fonts` section; keep it only if the gain is beyond the run ranges. Earlier note: (2) every WebView/CookieManager-triggering source-graph path, incl. JS/custom sources creating `NetworkHelper` → `AndroidCookieJar` → `CookieManager.getInstance()` before the gate; (3) a real presented-frame boundary for moving the UA warm-up. Only then rework the warm-up move and measure.

Gate (Claude 2026-11-21, owner chose it; `45cf42deb`): Injekt keeps each singleton in its own synchronized `lazy` (per-type lock, checked in the `mihonapp/injekt` jar), and extensions load in `ExtensionManager`'s own `launchIO`, outside any factory lock, so the remaining risk was WebView loaded *inside* a lock the UI thread can wait on. Changes: (1) core/common `AndroidCookieJar` — `CookieManager` is a lock-free lazy (`PUBLICATION`), so building `NetworkHelper` no longer loads WebView inside its Injekt lock; (2) core/common `tsuzuki/startup/StartupGate.kt` (`CompletableDeferred`; only the UI thread opens it; waiters never time out); (3) `tsuzuki/startup/StartupWarmup.kt` replaces the `App.onCreate` warm-up: at a main-looper idle moment once Home has its data (`StartupSignal.homeReady`), or another first screen has drawn, or at once with no screen (WorkManager/receiver cold start), 2 s at most; runs `getDefaultUserAgent` + `CookieManager.getInstance()` on the UI thread, then opens the gate; other processes open it at once; (4) App's async init and `LibraryUpdateJob` await the gate before the network/source graph. Not gated: JS plugin cookie reads and Coil fetches — both run without holding an Injekt lock now that `NetworkHelper` construction is WebView-free. Tests `StartupWarmupTest` (6) + `SourcesReadyTest`; **1,393 all-module tests pass**. Tablet, debug build, 6 cold launches each (`am start -W` TotalTime): baseline 1,063–1,090 ms, candidate 1,082–1,107 ms — **no gain measured here**; candidate logs `trigger=HomeReady afterMs≈552 warmMs≈45` (the warm-up costs ~45 ms on the tablet vs ~140 ms measured on the S22). Home renders, no crash. **Reviewer:** optimized Preview on the S22, 20 cold launches each, initial display beyond run ranges and Home-observed not worse; a cold library update with the process dead; first Browse search; a launch from a notification; an extension whose constructor calls `getDefaultUserAgent` plus a JS plugin source. Drop the gate if the S22 shows no gain.

Review (Codex Batch T, 2026-11-21): Changes requested. S22 optimized Preview, 20 cold launches per build with the same 500-book fixture. Initial median 554.074 → 433.157 ms, a 120.917 ms improvement against a 203.305 ms candidate span. Home-observed median 3133.637 → 3077.355 ms. The agreed initial-display variability gate failed, so the candidate stays excluded. Both builds and 1,415 Android tests passed. The first cold Browse search found the local novel on the tablet Debug build. Both optimized Preview artifacts built with the same scratch ID and embedded baseline profiles. Part 1 source publication race reproduced under controlled coroutine scheduling: `AndroidSourceManager.kt:104–108` can publish readiness for an earlier combined map once the scan flags are true, while `ExtensionManager.kt:416` still has the installed-source map queued in a separate `stateIn` collector. The candidate’s exact `awaitSourcesReady` returned true while the synthetic extension source was unavailable. Part 2 source finding: `LibraryUpdateJob.kt:94` resolves the source graph in the constructor, including starting extension loading, before `StartupGate.await()` at line 131. `StartupWarmup.kt:37` uses Home data readiness and its non-Home path uses an OnDraw callback. Neither establishes that a frame was presented. Evidence `.worktrees/review-v02-perf/app/build/review-evidence/v02-perf/source-publication-review.txt`. Measurement evidence `.worktrees/review-v02-main/app/build/review-evidence/v02-perf/`.

Tablet cold update (Debug candidate): Scheduled the real `LibraryUpdateJob` for one synthetic novel. Verified PID 15372 was gone before the job started. JobScheduler started PID 15787, the local server returned `GET /novel` 200, WorkManager reported SUCCESS, and the fixture verifier found Chapter 3. The first attempt ran during cleanup after the server mapping was removed and was discarded. The successful fixture run does not resolve the independently reproduced readiness race. Notification launch and installed-extension constructor behavior remain unverified. Evidence `tablet-cold-retry-process.txt`, `tablet-cold-retry-before-verify.txt`, `tablet-cold-retry-worker-result.txt` and `tablet-cold-retry-verify.txt` in the review evidence folder above.

#### PERF-START-2-F2 Publish usable sources before declaring them ready (T3) — `blocked` (independent changes requested on `797c79710`; part 2 `45cf42deb` dropped)

Make readiness follow the published lookup map containing the completed extension and JS scans. Exercise the interleaving where the scan flag arrives before its derived flow. Verify the actual library worker fetches a chapter from a cold process. Keep the synchronous warm-up unless a revised experiment gates graph construction at its entry points, verifies a presented frame and passes the same S22 20-versus-20 criteria.

Independent review (Codex Batch T). The candidate can still publish readiness with the pre-scan map. `afterScan` combines two independently collected flows. Even though the manager writes the map first, the map collector can be preempted between reading and forwarding the new snapshot while the flag collector forwards `true`. A virtual-time probe using the actual candidate helper reproduced the first ready value as empty while the backing state already contained the installed source. Six focused tests passed, including the reproducer. Evidence and probe source are in `.worktrees/perf-f/app/build/review-evidence/perf-f2/`. Publish readiness and data from one state, or snapshot the authoritative current map after the flag is visible. Keep the unrelated plugin deduplication formatting outside the fix.

#### PERF-START-2-F3 Snapshot the source map after the completed scan (T2) — `done` (merged locally 2026-11-22 at `eee06cf0f`; font warm-up left out)

Review (Claude, 2026-11-22; Codex wrote F3): **passed and merged with one reviewer change.** `afterScan` reads `items.value` once the flag shows, so a stale collected snapshot can no longer be published as ready; correct, and the F2 wiring (both managers write the list before the flag; `AndroidSourceManager` sets `isInitialized` only with both scans) reads right. The `JsPluginManager` hunk only restores upstream formatting. **Reviewer change:** part 1's font warm-up in `App.kt` had no measured gain (tablet none, S22 gate failed) and no timing verdict, so it is **left out** (`App.kt` restored to `main`); `awaitSourcesReady` in `LibraryUpdateJob`, F2 and F3 are in. Review branch `review/perf-f3-main` = `main` + `tsuzuki/perf-start-2-f3` (part 2 `45cf42deb` not included): all-module tests **1,418 / 0 failures**; after merging onto `main` (with AY-INT) the app suite passed again. **Cold-process check** (tablet, new probe `ColdSourcesReadyProbe`, each run a fresh instrumentation process with the real managers): `isInitialized` false at start, true after **55 / 69 / 72 ms**, both scans done, no 30 s stall. Limit: the scratch had no installed manga extensions, and Codex's UI-built cold `LibraryUpdateJob` fixture was not repeated (its scratch package is gone). Evidence `.worktrees/review-perf-f3/app/build/review-evidence/perf-f3-cold/`.



Close the confirmed flag-before-flow interleaving and test the first ready value against controlled delivery. Keep the synchronous WebView warm-up. Independent review after implementation.

Result (Codex). `SourcesAfterScan.kt` now reads the current authoritative map once the scan flag is observed. `SourcesAfterScanTest.kt` controls delivery of the new map and checks that the first ready emission already includes the scanned source. Restored the unrelated deduplication lines in `JsPluginManager.kt` to their base formatting. Six focused tests pass, including the old interleavings and cold-worker wait/timeout tests. Evidence `.worktrees/perf-source-ready/app/build/review-evidence/perf-start-2-f3/`. The earlier F2 cold-work fixture pass predates F3. Reviewer should rerun the actual cold worker and give the retained font warm-up its own timing verdict.

Result (Claude, 2026-11-21): **Readiness now follows the published map.** `afterScan(items, flag)` (`tsuzuki/update/SourcesAfterScan.kt`) combines a manager's own state with its scan flag and gives null until the scan is done; both managers write the list before the flag, so a ready value always carries the scan. `ExtensionManager.installedExtensionsAfterScan` and `JsPluginManager.jsSourcesAfterScan` use it; `AndroidSourceManager` combines those (not the `stateIn`-derived `installedExtensionsFlow`) and sets `isInitialized` only in the collector that has just published a map built from both completed scans (the `first { it }` waits on the flags are gone). **Part 2 is dropped**: the synchronous WebView user-agent warm-up in `App.kt` stays as on `main`, so `StartupGate`/`StartupWarmup` and the worker-constructor finding no longer apply; part 1's `awaitSourcesReady` in `LibraryUpdateJob` and the off-thread font warm-up are kept. **Tests:** `SourcesAfterScanTest` 2/2, including the reproduction (flag before a lagging `stateIn` collector → ready with an empty list under the old shape; the new shape always has the scan); `SourcesReadyTest` 3/3; full app suite 1,222/0. **Tablet cold worker** (the review lane's `ColdLibraryUpdateReview` + `run-cold-worker-retry.py`, adapted paths; scratch `v02reviewscratch.dev`; probe copy also removes the chapter 3 an earlier run added): activity PID 1506 killed and absent, JobScheduler started PID 1752, `GET /novel` 200, worker SUCCESS, `verify` found Chapter 3; crash buffer empty; baseline APK restored, probe uninstalled, reverse removed. Evidence `.worktrees/perf-f/app/build/review-evidence/perf-f2/`. No S22 timing run: nothing on the start path changed beyond part 1. AGENTS.md lists the upstream touch.

#### NT-LEX1 Our own US-English lexicon from CMUdict, conversion only (T3) — `done` (Claude subagent, 2026-11-14 night, `.worktrees/nt-lex1`, `tsuzuki/nt-lex1` `243121aea`; Codex independent review 2026-11-14 in .worktrees/review-nt-lex1; for 0.1.2; review lane: independent review)
Read `docs/research/neural-tts-clean-lexicon.md` §2 first. The upstream Kokoro lexicon's licence is not settled,
so the neural voice is out of 0.1.1. This card builds the replacement data, nothing else.
- `scripts/neural-tts/build-lexicon-cmudict.py`: read CMUdict from a pinned commit of `cmusphinx/cmudict` (hash
  checked, like `prepare-source.py`), convert ARPAbet to the Kokoro symbols with the mapping in the research
  note, write a file in the format of `lexicon-us-en.txt` that the NT1-BINARY runtime reads. Deterministic
  output; print its SHA-256.
- Bundle CMU's full licence text and a line saying the data was converted (`scripts/neural-tts/licenses/`).
- Tests: every output symbol is in the model's `tokens.txt`; the mapping cases named in the research note
  (G → U+0261, the five diphthongs, ER0/ER1, AH0/AH1, flap T, stress mark position); alternate pronunciations
  `WORD(2)` handled; same input → same bytes.
- **Do not** copy from, or tune against, misaki's `gold`/`silver` data or the upstream lexicon files. You may
  report an agreement percentage as a diagnostic, as the research did.
- Left for later cards (say so in Result, don't start): function-word reduction, suffix rules, names and
  out-of-vocabulary words, numbers and dates, any British lexicon, the owner listening test.

Result (Claude subagent, 2026-11-14; `243121aea`): **review.** Scripts only; nothing in the app.
- `scripts/neural-tts/build-lexicon-cmudict.py`: CMUdict pinned at `cmusphinx/cmudict` commit `74790861f652`, `cmudict.dict` SHA-256 `81917843…3d22` (refused on mismatch; `--input` skips the check with a warning). Output `.native/lexicon/lexicon-us-en-cmudict.txt` (ignored, not committed): **126,052 entries**, SHA-256 `3f9d0234…fb50`, identical on two runs.
- Format confirmed in sherpa-onnx v2.0.1 source: `word p h o n e s`, word lowercased, one symbol per field (`scripts/kokoro/v1.0/generate_lexicon_en.py` lines 33, 49–53; reader `csrc/text-utils.cc` `ParseLexiconFile` 1778–1855). A repeated word overwrites the earlier one, so only the first CMUdict variant of each word is written.
- `licenses/cmudict-LICENSE.txt` (byte-identical to the pinned repo's) and `cmudict-NOTICE.txt`. Vocabulary fixture: the 114 symbols from `hexgrad/Kokoro-82M` `config.json` at `f3ff3571`.
- Tests: 26 new, 50 in `scripts/tests` in total, 0 failures (Claude re-ran them). Every one of the 126,052 entries uses only vocabulary symbols.
- **Decision to review: the flap symbol.** The research note said U+027E. misaki's own pipeline for Kokoro v1.0 rewrites U+027E to `T` before the model (`misaki/en.py` line 733 at `fba1236`), so the default output writes `T` ("water" → `w ˈ ɔ T ə ɹ`); `--flap ipa` writes U+027E, which is what the upstream sherpa lexicon feeds the model. Both are in the vocabulary. Nobody has listened to either; the owner listening test decides.
- Other choices: flap only for T after a vowel and before an unstressed vowel, inside a word; stress mark directly before the vowel; AH0 `ə`, AH1/2 `ʌ`, ER0 `əɹ`, ER1/2 `ɜɹ`; unknown phone or missing stress digit is an error.
- Diagnostic only, nothing copied: 58.6 % exact agreement with misaki gold on 36,973 shared words (66.0 % ignoring stress). The subagent read `us_gold.json` in a scratch directory outside the repo for this number and to count which flap symbol it uses.
- Not verified: the script's own HTTPS download (this machine's MSYS Python has no CA bundle; the pinned file was fetched with `curl` and passed the hash check); loading the file in the runtime; how it sounds.
- Reviewer: read the mapping table against the research note and a primary ARPAbet reference, re-run the tests, rebuild and compare the output hash, spot-check 30 common words, and confirm no misaki data or upstream lexicon text is in the commit.

Review (Codex independent review, 2026-11-14): **pass.** Full six-file diff reviewed, no copied gold/silver or upstream lexicon data. Pinned CMU input independently downloaded and hash-verified; two default builds give 126,052 entries and SHA-256 `3f9d023400a8b0c4730c095729363c9374c8e8efbf65ad62e2df67f0d18efb50`. All 50 Python tests pass (0 skips), full output vocabulary exactly matches the 114-symbol pinned config fixture, CMU licence byte-identical to pinned source, 30 common words checked against ARPAbet input. The default `T` flap is supported by pinned model normalization code; IPA option retained, listening still required. Explicit local `--input` bypass is documented and warns; production data must use default hash-enforced path. Python HTTPS CA limitation remains; curl verified TLS for independent downloads. JDK21 Debug + Preview builds and 1,087 Android unit tests pass (0 failures/errors/skips), diff check clean. No devices needed or used. Evidence `app/build/review-evidence/nt-lex1/` (archived in root); source checks and scope in research section 4. No app runtime integration or voice release approval.

#### BACK1 Back pressed while details is opening is swallowed (T3) — `done` (Codex independent review 2026-11-15; original f9d047d95 plus review fixes 3305865d0 and 7e3a3d635)
Seen on the S22 in the PERF-DETAILS review (3 of 3): Library → tap a book → system Back about 100–200 ms later.
Details stays open; the next Back shows **Home**, not the Library. Expected: the first Back returns to the
Library (or cancels the opening), and the Library tab is still selected. Guess, not confirmed: while the
transition runs, the Library tab's own back handler ("back goes to Home") is still the top handler, so the
early Back changes the selected tab underneath instead of popping `MangaScreen`. Find the cause by reading the
back handlers (`HomeScreen.kt`, the tab back handling, Voyager's `Navigator` back handling,
`TsuzukiNavigatorTransition`, predictive back if enabled). First check whether `main` before `c0a12f2c4`
(450 ms opening) has the same behaviour by reasoning from the code; say which. Fix so a Back during the opening
pops the details screen and leaves the tab alone; add a unit test for whatever decision function you extract.
Keep the upstream edit to a small dispatch. No adb. Reviewer script: S22 and tablet, Dot and Colour, Back at
about 50, 150 and 400 ms after the tap, from Library, Home shelf, Updates and History; then predictive-back swipe.


Review Result (Codex, owner-authorized takeover 2026-11-15): **pass with two review fixes**, not f9d047d95 alone. Seven-file diff reviewed. Covered tab Back callbacks could win while both routes stayed composed; the identical unconditional Home callback existed before c0a12f2c4 (450 ms), established by source reasoning, not an old-APK measurement. Original fix introduced a regression: ordinary Library Back exited the app because Home's handler sits outside its root-navigator provider and saw the nested tab navigator. Reproduced fresh and after process death. 7e3a3d635 passes the captured root navigator explicitly; 3305865d0 resolves the restored Home host key rather than the new process singleton key. Other screen instances retain their own keys. Small upstream hooks; no layout/data/motion changes.

Final JDK21 Debug + optimized Preview and 1,091 unit tests pass; four Back policy tests, scoped formatter/diff checks clean. On S22 and tablet, Dot and Colour, Library/Updates/History/Home shelf each pass Back at requested 50/150/400 ms (actual 51–56/151–153/401–406 ms), plus one edge Back swipe per origin: **48 timed cases + 16 gestures**. Each combination also passes ordinary Library-to-Home Back and verified-dead-process restoration followed by Library-to-Home Back. Warm open/pop precedes each timed series to exercise callback recreation. XML/text comparisons, recordings and all four final screenshot montages inspected; origins and selected phone pill/tablet rail preserved, crash buffers empty. S22 rv5debugscratch and tablet rel011upgradescratch both use 500-book synthetic fixtures; daily apps untouched. APK SHA-256: S22 `7ac99e682436d440bc29074f1d12cfd4341afafa8b393143c45a0a07379b8466`; tablet candidate-id `e97f80765c61800568b0b368b76e5d354a59059db8a7539cc40f09fc0510fa4b`. Evidence `app/build/review-evidence/back1-review/`, final-audit.json and source-audit.md. Timestamped input helper is evidence only and was removed from module sources. Final candidate refinement 6119f2613 (180 ms opening) also passes all four normal/restored Library Back combinations, 12 additional timed Library cases and four edge gestures; see rel-011-refresh-final/. No TalkBack/reduced-motion/real-cover claim. Device lock retained for candidate refresh.

#### STR2 Hardcoded and untranslated strings sweep (T2) — `done` (merged locally 2026-11-21)

Review (Codex Batch T, 2026-11-21): Passed and merged locally. Fixed Home fallback labels and counts to use the app language context (review commit `2812681bc`). Japanese tablet Home showed the chapter and library counts. The resource audit matched 506 Tsuzuki keys across all eight locales. Integrated Debug and optimized Preview built, with 1,396 tests passing. Evidence `.worktrees/review-v02-main/app/build/review-evidence/v02-main/`.
The test devices run in Japanese and RV5 still found English or reused strings (STR1, the CSV dialog title).
Sweep `app/src/main/java/eu/kanade/presentation/tsuzuki/` and `eu/kanade/tachiyomi/tsuzuki/` for user-visible
text that is a Kotlin string literal (including `contentDescription`, toasts, notification text, error copy) or
a `tsz_` key missing from any of the 8 locales Tsuzuki ships. Move literals to `tsz_` strings with all 8
locales; reuse an existing key only when the meaning is the same sentence, not just similar words. Add a unit
test that fails when a `tsz_` key is missing from a shipped locale (if one exists already, extend it). Don't
touch upstream strings or upstream screens. Result: a table of every string moved (file, old text, key), and a
list of anything left on purpose (debug-only text, log messages, format-only strings) with the reason.
Reviewer: Japanese device pass over the screens in the table.

Result (Claude 2026-11-19, uncommitted in `.worktrees/str2-main` on `main` `3ce5d2425`): sweep of `presentation/tsuzuki/`, `tachiyomi/tsuzuki/`, `ui/tsuzuki/`. **All 482 `tsz_` keys already exist in all 8 locales with matching placeholders**; new `TsuzukiLocaleCompletenessTest` keeps it that way (missing key or placeholder mismatch fails).

| File | Old text | Now |
|---|---|---|
| `TsuzukiStartup.kt` | "続き · read what happens next" | `tsz_startup_tagline` (Dot uppercases) |
| `TsuzukiHomeViewModel.kt` | "Ch. 12", "N ch" | `tsz_home_chapter_label`, `tsz_home_chapter_count` (ja 第12話 / 12話, same as `tsz_ch`) |
| `ReadingChart.kt` / `TsuzukiStatsScreen.kt` | "5 h 22 m", "22 m" | `tsz_duration_hours_minutes`, `tsz_duration_minutes` |
| `TsuzukiAdvancedPreferences.kt` | matched "Formatting", "Content", "Page load delay", "Skip cover loading", "Confirm back after pages" in English | the same string resources the screens render — **bug fix: on Japanese devices these never folded into Advanced** because the screens show 書式 / コンテンツ / ページ読み込みの間隔 … |

Left on purpose: "E-Ink", "Per-Extension Overrides" and the two "Minimum chapters before tracking" titles stay as literals because upstream screens hardcode them in English (translating them is an upstream-screen change, out of scope); `ErrorCopy` "Cancelled" and exception/`check` messages are the technical-detail text under the localized error title; animation labels, URL placeholders, clipboard label, licence names, preview sample data, log lines and the `animeLoadFailureMessage` log line. English upstream strings seen on devices (TTS, reset, list view, custom sources) are fixed by TTS-POLISH, not here.
Tests: focused 19/19 pass (locale 2, chart 4, home 13); full app suite **1,219 tests, 0 failures/errors/skips**. Scoped Spotless (unrelated reformatted files restored). Claude self-check (tablet, Japanese, Colour, STR2 build installed over `rv5debugscratch`, no schema difference from the chain): Settings › ブラウズ now folds ページ読み込みの間隔 / 表紙の読み込みを省略 / 戻る前に確認するページ数 under 詳細オプション (before: always in the main list); 小説リーダー shows コンテンツ and 自動スクロール under 詳細オプション (書式 not caught in the dump); no crash. Scratch data restored byte-equal and the TTS-POLISH chain APK (`37a770e6…`) reinstalled. A11Y1 can branch from `tsuzuki/str2-main` once this is committed. Reviewer: Japanese device pass over Home (chapter label/count), Stats chart total, startup splash tagline, and Settings › Browse / Novel reader showing those groups under the Advanced fold.

#### A11Y1 Screen-reader and touch-target pass on Tsuzuki screens (app lane, T3) — `review` (Claude 2026-11-21, `75b388588` + `da125dd58` + one more on `tsuzuki/a11y1` from `main` `e9d8bb9eb`, which has STR2; the per-screen audit continues)
Code-reading card, no adb. For every composable under `presentation/tsuzuki/` and `presentation/theme/tsuzuki/`:
(1) clickable things have a role and a spoken label (icon-only buttons need a `contentDescription` from a
`tsz_` string; decorative images use `null`); (2) touch targets are at least 48 dp (use
`minimumInteractiveComponentSize` or padding; don't change the drawn size on the boards); (3) rows that are one
action are merged (`semantics(mergeDescendants = true)`) so TalkBack reads them once; (4) state is spoken
(selected pill, toggles, progress bars with a value); (5) headings are marked on section labels;
(6) nothing conveys state by Dot's red alone. Fix what is a plain omission. For anything that would change a
board's look, don't change it: list it for the owner. Add Compose semantics unit tests only where a test rule
already exists in the module; otherwise list what a reviewer must check with TalkBack on the S22.
Result: a table per screen (what was missing, what was done), and the TalkBack script for the reviewer.

Found 2026-11-19 (Claude, tablet): Settings › 外観 accent swatches have English content descriptions ("Amber", "Coral", "Gold", "Mint", "Rose", "Sky", "Violet", "White"), so TalkBack reads English on a Japanese device — translate them (8 locales) as part of this pass.

Result (Claude, 2026-11-21) — shared building blocks fixed, then one code scan per criterion over `presentation/tsuzuki` and `theme/tsuzuki` (no per-screen TalkBack walk; that is the reviewer's script below):

| Where | Missing | Done |
|---|---|---|
| `TsuzukiPill` (every pill row) | Dot's capitals were what TalkBack read (short Latin labels can be spelled out) | the label's own text is the spoken description; `selectable` + role + 48 dp were already there, and `TsuzukiPillRow` already a `selectableGroup` |
| `TsuzukiSectionLabel` (Browse, Settings, History, Updates groups) | not a heading; read in capitals | `heading()` + label as written |
| Settings › 外観 accent swatches | English names ("Gold" … "White") on a Japanese device | `tsz_accent_swatch_*` in all 8 locales; the swatch row is a `selectableGroup` |
| Extension details debug row, download-queue error line, migration setup row, `TsuzukiSettingsPane` row | `clickable` with no role | `Role.Button` |
| Reader contents scrim and its tap-swallowing panel; `TsuzukiAdaptiveSheet` outside area and sheet surface | screen-sized `clickable`s TalkBack focuses as "double-tap to activate" | tap detectors (`pointerInput` + `detectTapGestures`), which add no accessibility node; Back still closes both |

Tablet (scratch `a11yqa`, then uninstalled): Appearance swatches read ゴールド, アンバー, コーラル, ローズ, バイオレット, スカイ, ミント, ホワイト; section label reads "Dotの表紙" while drawing "DOTの表紙"; the catalogue filter sheet stays open on an inside tap and closes on an outside tap; crash buffer empty. Compose semantics tests: the app module has no Compose UI test rule, so none were added.

Touch targets (scan of every clickable/IconButton in `presentation/tsuzuki` and `theme/tsuzuki`): drawn under 48 dp but still hit at 48 dp through Compose's minimum touch-target expansion, so left at their board sizes: Browse source unpin (32), custom-source picker help/back/forward (40; back and forward sit 4 dp apart, so their expanded areas meet and the nearer one wins), chapter-search back (44), Settings back circle (44), source-priority reset (44 wide). Reviewer: tap each edge on the S22. Already fine: `TsuzukiProgressBar` reports `progressBarRangeInfo`; the More screen toggles use `toggleable(role = Switch)`. Colour-only state (scan for accent colours chosen by a selected/current/active flag without matching semantics): fixed the Duplicate Finder filters toggle (`a11y1` second commit, now `selected` while open); and (third commit) the Home hero's page dots now say "page n of m" once for the row and the custom-source wizard's current step says "current step" (2 spoken-only strings in 8 locales; nothing drawn changes). Also clean: no icon-only clickable without a spoken label, no hard-coded English content description (only the app name on the rail logo); clickable rows already merge their children. Nothing found that would change a board's look. **TalkBack script for the reviewer (S22, Japanese):** (1) Library: swipe across the pills; each says its name, "選択済み" on the current one, and "タブ" as role. (2) Settings › 外観: swatches read in Japanese with the selected one marked; headings navigation (TalkBack "見出し") jumps between section labels. (3) Browse › a source › フィルター: focus moves into the sheet, never onto an empty full-screen button; Back closes it. (4) Reader › 目次: same for the contents panel. (5) Migration setup and a download-queue error line: announced as buttons. (6) Home with two or more books: the hero's dots read "page n of m". (7) Custom source setup › WebView element selection steps: the first step reads "現在のステップ" (uiautomator dumps don't show state descriptions, so this needs TalkBack; not checked on a device).

#### REL-0.1.1 Release candidate (review lane prepares; owner publishes) — `done` (candidate preparation only; Codex review lane, 2026-11-14)
1. `CHANGELOG.md`: move Unreleased to 0.1.1 with every merged card in plain words (what the reader notices).
2. Version bump as V6 did for 0.1.0 (see V6's Result for the files).
3. `:app:assemblePreview :app:assembleRelease :app:testDebugUnitTest`; 16 KB check on the APKs.
4. Upgrade test: install v0.1.0 (`git show v0.1.0`-built or the published APK) in a scratch package with the Y4
   backup restored, install the candidate over it, library/history/settings intact, no migration error.
5. Preview candidate on the tablet and S22, Dot Home. **Stop there.** The owner installs it on their phone, tags
   and publishes; nobody else tags, pushes or touches `R5GL23ZYE1V`.
Refresh Result (Codex, 2026-11-15): **complete, owner publishing handoff**. Final app source 6119f2613 includes BACK1 review fixes and a synchronized 180 ms opening/fade. The first Colour opening on the initial refreshed 200 ms candidate reached the 300 ms boundary (286–302 ms frame bracket); evidence retained in rel-011-refresh/. Final cached-opening conservative upper bounds are Dot 260/227/252 ms and Colour 226/253/243 ms (8–17 ms frame uncertainty), all below 300 ms. Six frame sheets inspected; Colour run 2 has late idle compressed-frame differences without further visible movement, so its conservative visual endpoint is recorded explicitly in details-timing.json rather than using the raw diff endpoint. No claim for remote fetch, real-cover images or reduced-motion timing.

Final Preview + Release and 1,091 tests pass (zero failures/errors/skips). Release R8 initially exhausted the 4 GB heap with two workers; retrying with --max-workers=1 passes. All ten normal Preview/Release APKs and five scratch Preview APKs pass 16 KB ZIP/64-bit ELF alignment and baseline.prof/profm checks. Final ARM64 Preview SHA-256 51262ee4f416d69b2e7d2461baa9826417c0545458ca9ef729760a8fc17e0482; Release 2f45a74d32b899be8f52cc7dd42b260361777eeddf088ca29f72e7212c27fc5f; optimized tested scratch 965e15c459bcde317b77320b061cde1d854c4c0c923b32737145229c6458d2f9. VersionCode 25, Release 0.1.1, Preview 0.1.1-8848; local Android Debug certificate remains 6c1e396d8d094a0b9bd98354cbd8997637acb2549d8ba79e84cc0212be5ae17a.

Direct upgrade repeated for the final APK: fresh exact v0.1.0 tag APK (5a65307c9), restored the archived v0.1.0 Y4 database/preferences snapshot via run-as (database byte equality verified before launch; this snapshot came from the original UI backup restore), re-granted only its dedicated fixture directory, then installed the final optimized candidate directly over it. Final backup exactly matches all titles/types, chapter read/bookmark/progress/timestamps/order, history dates/durations and category mappings: 500 books / 50,000 chapters / 995 history / 30 user shelves. Eight exported existing preferences match, including Colour; storage/onboarding retained in UI. Both devices additionally pass final-APK normal/dead-process-restored Library Back, 50/150/400 ms Back and edge swipe (12 timed cases + four gestures). Both final Dot Homes inspected, 500 books, correct phone pill/tablet rail, awake, empty crash buffers, no migration exception in candidate UID logs. Device lock released. Daily apps/owner phone/STR2 untouched. Evidence and final artifacts: app/build/review-evidence/rel-011-refresh-final/. No tag, push, official signing or owner-phone install.

Original preparation below (2026-11-14; hashes superseded by this refresh):

Result: Candidate prepared from release branch merge `fa9523f04` (accepted RV5 + PERF1), versionCode 25 / versionName 0.1.1. Changelog covers all accepted cards; automatic voice wording preserves explicit choices. `assemblePreview`, `assembleRelease`, `testDebugUnitTest` passed (7m43s; 1,085 tests, 0 failures/errors/skips). ARM64 Preview/Release and the optimized scratch candidate each contain seven aligned native libraries and baseline.prof/profm; 16 KB ELF and zipalign pass. All five Release APKs, five scratch Preview APKs and archived normal ARM64 Preview pass zipalign.

Upgrade: built exact tag `v0.1.0` (`5a65307c9`) as Debug in isolated package `io.github.manojx2005.tsuzuki.rel011upgradescratch`, restored Y4, chose Colour, then installed optimized non-debuggable 0.1.1 Preview over it on S22. Before database snapshot: 500 books / 50,000 chapters / 995 history rows / 30 synthetic shelves (+ system shelf). Candidate backup compared exactly against fixture: titles/types, all chapter read/bookmark/progress/timestamps/order, all history dates/durations and category mappings preserved. Seven exported existing settings matched with no changes, including Colour; internal app-state keys are excluded from exports, but storage and onboarding remained intact in the UI. No migration error. S22 switched back to Dot afterward. Tablet fresh scratch candidate restored the same Y4 data. Both final Dot Home screenshots inspected, 500 books, correct phone pill/tablet rail; both final crash logs 0 bytes. Owner phone and daily apps untouched. Existing Watch teaser from v0.1.0 remains; the blocked anime chain was not merged.

Artifacts and evidence: `app/build/review-evidence/rel-011/`. Normal ARM64 Preview `tsuzuki-0.1.1-arm64-preview.apk` SHA-256 `0a00fe4b7b824632694c0f6a9cbdef237c7bead01335c965110ad51ff7661738`; normal ARM64 Release `tsuzuki-0.1.1-arm64-release.apk` SHA-256 `e600b27a97853546ae894bde3b322e86faaca3c959f66f89b29c061477a83838`; tested optimized scratch SHA-256 `1473cc9606d691fc1645ea19c0c781d3a91e4d5cdbae35c34352dee6ce1a63ea`. Logs retain non-fatal Compose mapping, XML service and optional AndroidX window-extension warnings. These local APKs use the Android Debug certificate (SHA-256 `6c1e396d8d094a0b9bd98354cbd8997637acb2549d8ba79e84cc0212be5ae17a`); the owner's official CI signing/publish flow is still required for the distributed release. Do not uninstall an official app to install these local artifacts.

Stop point reached: owner handles official signing, owner-phone install, stale local tag resolution, tag and publish. No tag, push or official release was performed.

#### RV3-S Review: anime chain A2.3 → A6.4 + AY-PLAY (T4) — `done` (Codex independent review, 2026-11-15; review/rv3-s)
Reviewed the whole anime chain, merged with v0.1.1 on anime-sync and AY-PLAY, in `review/rv3-s`.
Checks completed: gate audit plus off-state Debug/Preview UI, upstream merge adapters, migration 36 unchanged
and additive 37–39, scratch real-source browse/library/details/playback/resume/native-track/History/Updates,
and 16 KB ZIP/ELF audit. Owner accepts native libraries and the arm64 toggle in release builds, off by default.
No daily-app backup/install or credential entry was needed: only the designated animescratch package was used.

Review verdict (Codex independent lane, 2026-11-15): **pass with review fixes `a3740a40c` and `03128b5ca`**. Whole chain on `review/rv3-s` includes v0.1.1 and AY-PLAY `644bb44e4`. The current acceptance block above is authoritative for updated gate policy (arm64 setting ships, off by default), migrations 37–39, scratch-only device scope, final hashes and coverage. 1,382 tests and final Debug/optimized Preview History matrix pass. Merged locally in `304bd9aff`; no push/tag. Remaining extended parity cases are recorded rather than claimed tested.

#### RV4+ Keep going
After RV3, each time 4–5 cards reach `review`, write the next RV card here (same format, same order the lanes
finished them) and run it. The anime chain stays one review per 3–4 cards.

#### CHAPNUM Novel chapter labels (owner 2026-11-08) — `done` (RV4, 2026-11-13)

Use a clearly stated chapter number in the source title when showing a novel chapter number. The Perfect Run
lists “2: Story Branching” while the stored number is 1. Apply the correction to the tablet details chapter
list, Home’s right-hand chapter list and the reader’s chapter labels in both themes. Keep stored numbers and chapter ordering intact.
Boards: TabletLibrary, TabletNovel, TabletReader and Q2 ChapterEnd. Test explicit decimal and volume/chapter headings,
unnumbered titles, and manga fallback behavior.

**Result (Codex, 2026-11-08):** `706affe00` and `512fd1972` on `tsuzuki/chapter-numbers`.

- `NovelChapterNumber.kt` recognises explicit source headings, including the Royal Road form
  `2: Story Branching`, decimals, volume/chapter headings and Japanese chapter markers.
- `TsuzukiHomeViewModel.kt` uses the source number in the sidebar’s number column and passes
  the title without its number to the title column. Volume labels stay with the title.
- `MangaScreen.kt` uses the recognised source number in the novel chapter list’s number-only
  mode and the original numbered title once in number-and-title mode.
- `ReaderActivity.kt` applies the same rule to the reader toolbar and optional status bar.
  `TsuzukiChapterEnd.kt` keeps the full numbered title and suppresses its redundant number label.
- `NovelChapterNumberTest.kt` covers the reported mismatch, duplicate suppression, both display
  modes, explicit decimals, fallback for titles with incidental numbers, and sidebar number/title splitting.

Stored chapter numbers still drive ordering and existing reading state. JDK 21, two scoped Spotless
passes, `:app:assembleDebug` and `:app:testDebugUnitTest` passed (972 tests, zero failures or skips).
Independent device review: open The Perfect Run on the tablet in Dot and Colour. Check the right-hand
Home list, details list, reader toolbar and chapter-end card, then check number-only mode.
Verify that a manga and a novel whose source title has no number keep their previous labels.

Review (Codex RV4, 2026-11-13): Pass for the requested details/Home/reader label surfaces. Synthetic source headings 129–131 with stored indexes 1–3 display once in details and reader in Dot/Colour; Home uses source Chapter 131. Pure tests cover explicit decimal, volume, Japanese headings, incidental-number fallback and duplicate suppression. Stored numbers/order are unchanged; manga remains on the existing guarded path. Four card tests and all 1,011 accepted tests pass. S22 deferred by owner. Evidence `app/build/review-evidence/rv4/tablet-rv4-chapter-numbers-*.png` and accepted-reader screenshot.

#### DETAILS-REFRESH-TRACKING Owner fixes: cover refresh indicator and tracker request branding — `review passed (UI), not merged` (2026-11-15, `.worktrees/details-refresh-tracking`, `tsuzuki/details-refresh-tracking`)
Owner screenshots: remove the loading indicator below novel/manga covers during refresh; preserve refresh and error/retry behavior. Owner expanded the branding request to **all trackers**, not just MAL. Boards: NovelDetails and MangaDetails (Dot, Colour), owner screenshot overrides the refresh state.
Result (Codex, 2026-11-15): `TsuzukiOperationState.kt` omits the extra details loading-stage block while refreshing and still shows source failures with Retry/WebView afterwards. Eight active tracker User-Agent identities/project links now say Tsuzuki; MAL's disabled header comment follows it. Kavita's login plugin name is Tsuzuki (string parameter confirmed in official API schema); compatibility API paths and stored IDs stay intact. MangaBaka no longer serializes the app-name resource object into its header. Small edits in ten tracker files, listed in AGENTS. `docs/TRACKER_BRANDING.md` audits all 14 trackers and documents the six separate provider registrations. Debug compile/assembly + **1,091 app tests, no failures/errors/skips**, two per-file format passes and diff checks pass. No low-value UI test added for removing an indicator. No app-lane adb; device review must refresh a cached novel and manga in Dot/Colour, confirm no below-cover spinner and preserved error actions. Authenticated tracker/consent-page checks are **not done**; TRACKER-CLIENTS below owns them. Implementation **`cb30680f0`**.

Independent review (Claude 2026-11-19): **UI part passes on the tablet; tracker sign-in not checked.** Code: during a refresh `TsuzukiDetailsSourceStatus` draws nothing (the old error is hidden) and shows the error with Retry/WebView after; tracker edits change only name/link strings (Kavita's `pluginName` is the one server-visible change, documented in `docs/TRACKER_BRANDING.md`). Device (tablet, `rv5debugscratch` = TTS-POLISH chain build, RV5 Network Fixture novel; a local PC server held each request 15 s via `adb reverse tcp:18765`, removed afterwards): Colour and Dot mid-refresh show **no block under the cover**, only the standard pull-to-refresh spinner (on the two-pane tablet it sits at the pane edge, slightly over the first chapter row — upstream placement, not a regression); refused connection and HTTP 503 both end in the error card with 再試行 and WebView (Dot: WEBVIEW). Not checked: a manga in progress (local manga refresh is instant; same shared composable), the S22, authenticated tracker requests (TRACKER-CLIENTS). Scratch data restored byte-equal (9 files), Colour Home, crash log empty. Evidence `app/build/review-evidence/details-refresh-claude/`. Not merged: part of `review/app-chain`, which waits for the RECAP review.

#### RECAP-FIX Novel recap never appears (owner 2026-11-15) — `review` (Claude subagent; `tsuzuki/recap-fix`, `92af378af`; in `review/app-chain`)
Owner: the recap never shows. Causes found: the previous chapter came from the reader's filtered list (skip-read / downloaded-only removed exactly the chapter just finished), nothing was recapped when the previous chapter was neither cached nor downloaded, and a chapter reopened mid-way restored past the card above the text. Fix: previous chapter from the full sorted list; one 3 s source fetch when nothing is offline; mid-chapter resume shows the recap as an overlay built from the prose just before the restored position (falls back to the previous chapter's excerpt). Debug only: `run-as … touch cache/force-recap` forces the 24 h rule. Commits `cbfe5d863`, `92af378af`.
Code review (Claude, 2026-11-15): **pass with notes, device check pending.** Logic and tests read (`NovelRecapTest`, `NovelRecapSessionTest`). Notes, not blocking: (1) when a recap is due, the source fetch runs before `loadChapter`, so a slow source can delay opening by up to 3 s (only after ≥ 24 h away, at most once per open); (2) the `ReaderViewModel` helper (~10 lines) and the `ReaderActivity` overlay block (~19 lines) are larger than the X1 one-line-hook rule — move them into `presentation/tsuzuki/reader/` at the next touch; (3) `writeExcerpt` failure is swallowed without a log line. Overlay is a new state not on the Recap board (`fixes-2026-11b` board 3); it reuses the card's content. Device script for the reviewer: novel read > 24 h ago (or `force-recap`), (a) open the next chapter from the top → card above text; (b) reopen a chapter at ~50 % → overlay with the paragraphs just before the position, dismiss on scroll ≥ 2 % / page turn; (c) skip-read filter on → recap still of the chapter just read; (d) online novel, previous chapter never opened → recap appears within 3 s or not at all. Dot and Colour, tablet and S22, Native and WebView.

#### READER-NEXT-GUARD Manga Next: loading indicator and no double skip on Next — `done` (independent Codex review passed 2026-11-15; original Claude `b53622e21`, review fix `e46df5e41`; local merge only)
Owner: in the manga reader, Next shows no loading screen, the page just changes later, and an accidental double tap skips two chapters. Causes: (1) upstream Tsundoku `3cbd993ba` ("webview paged reader") made the `isLoadingAdjacentChapter` observer in `ReaderActivity` return before `setProgressDialog` for every non-novel viewer, so manga lost Mihon's loading indicator; (2) `PendingAdjacentNavigation` (Q2) replays a tap made during a load, and a fast chapter can also finish loading before the second tap lands. Fix: `ReaderActivity` calls `setProgressDialog(isLoading)` for manga viewers (2-line hook; file already on the AGENTS list); `TsuzukiAdjacentNavigation.navigate` marks user-started switches, and `PendingAdjacentNavigation` drops taps while one is loading and for `ADJACENT_TAP_COOL_DOWN_MS` = 600 ms after it ends; taps during reader-started loads (infinite scroll, TTS handoff) are still replayed once (Q2 behaviour kept). Test `a second tap during a chapter switch the reader asked for is dropped, not replayed` + existing navigation tests pass; scoped Spotless clean.
Device (tablet, scratch `rv5debugscratch`, local manga "Next Button Test" 3×3 pages pushed to `Documents/Tsuzuki-RV5-Scratch/local/`): **before** (chain build) chapter 1 + double tap on 次の章 → Chapter 3; **after** → Chapter 2, and a later single tap → Chapter 3; crash buffer empty. **S22 (Dot, same branch):** chapter 1 + double tap on 次の章 → Chapter 2; crash buffer empty. Not checked: the loading indicator itself (local chapters load too fast to see it — check with an online manga source), novel infinite-scroll replay on device. Both scratch apps now run this branch. Note: the scratch app on the tablet now runs this branch (main-based, without the app chain).
Independent review verdict (Codex 2026-11-15): **pass with fix `e46df5e41`**. Found a Q2 regression: an automatic reader load beginning inside the old 600 ms user cooldown discarded the pending direction. New regression fails original code (expected false, actual null), then passes after preserving automatic-load queue/replay while keeping user-load double taps suppressed. Seven focused navigation tests pass; **Debug + optimized Preview + 1,384 all-module tests (1,217 app), no failures/errors/skips**, scoped formatter twice and diff check pass.

Independent device script/results, tablet + S22, Dot then Colour: temporary instrumentation-only `HttpSource` supplies three 3-page synthetic chapters, with a 3-second page-list delay on chapter 2; no source APK, external server, reverse or storage picker. Invoke the actual Next button handler twice 50 ms apart: `ReaderProgressIndicator` exists while loading and clears afterward; active and rendered chapter is **2**. After cooldown, one handler call reaches rendered chapter **3**. All four cases pass; loading/Chapter2/settled Chapter3 screenshots inspected. This is programmatic invocation of the actual button handler, not a claim of physical taps. Claude's earlier fast-local physical-tap checks remain in the original report. No repeated novel gesture UI sweep; Q2 automatic-load replay covered by the red/green unit regression.

The temporary source must override the test process's cached DI factory; initial fixture-setup failures were source-not-installed errors, not passing reader checks. Successful runs are `*-DOT-verified.txt` / `*-COLOUR-verified.txt`. Helper archived and removed from androidTest sources. Original scratch data restored byte-for-byte (**10 tablet / 11 S22 DB+preference files**), synthetic DB rows/report removed, helper uninstalled, both awake on Dot Home; screenshots inspected and crash logs empty. Daily apps and owner phone untouched; Device lock free.

ARM64 scratch Debug SHA-256 `8381a3d96332f60e40dfba8b4fac1ac8a648916562054bdc46a0f2f579660fd1`; optimized Preview `50303aaa3ed3a18d6eb88b6d5d843e05842c9fcc42fc178a2da5e2c4b83be721`. Evidence and both APKs archived at root **`app/build/review-evidence/reader-next-final/`**, including red-regression.xml, final-audit.json, scripts, helper source, logs/screenshots and original scratch snapshots. Accepted into local main through the review branch; no push/tag.


#### RECAP-F Recap card matches its board (T2) — `blocked` (changes requested after tablet pagination review. Trial fix in `.worktrees/review-v02-recap`.)

Review (Codex Batch T, 2026-11-21): The one-line trailing-page fallback fix passed the previous tablet failure in Dot and Colour. The 24-case tablet matrix of both themes, TextView, scroll WebView and paged WebView passed its original assertions. The actual excerpt still fails on a plain-text paged chapter. On the settled 90% page, paragraph 9 in the prior column exposes a roughly 3 CSS px fragment past the clip boundary. `NovelRecapViewport.kt:132` treats that fragment as visible and returns paragraphs 6–8. The actual column starts with a real continuation of paragraph 13, so paragraphs 10–12 should be the last completed excerpt. The same result occurred in both themes. Evidence `.worktrees/review-v02-recap/app/build/review-evidence/v02-recap/tablet-rects-DOT-plain.txt` and the corresponding screenshot. Tablet crash buffer empty, returned to its previous Dot Home. The separate `animescratch` state was untouched. The app chain remains unmerged.

Board `docs/design/boards/fixes-2026-11b/boards.html` §3 (Dot and Colour) and its spec row: label **"Previously · Ch. 4 · 2 days ago"** (previous chapter's short label + relative time since it was read), radius 24, padding 20, 18 below, **body 13 sp in the reader's own font** (today: heading 前回まで only, body in the UI sans `bodyMedium`), max 6 lines. Real data only: chapter label from the previous chapter (reuse `novelReaderChapterLabel`), time from the history `readAt` already read in `NovelRecapSession.prepare` (`lastRead`); hide the time part if unknown. Carry both in `NovelRecap` (new fields) and render them in **all three paths**: `NovelRecapCard` (TextView card + `NovelResumeRecapOverlay`) in `presentation/tsuzuki/reader/TsuzukiNovelRecapCard.kt`, and `webNovelRecapScript` (WebView JSON → the recap JS asset). Body uses the reader's font family/size settings (the WebView already does; match it in TextView). Dot uppercases the label via the existing `cased()`/tokens helper. New/changed strings in 8 locales with the same placeholders (relative time via the existing relative-date helper, not a new formatter). Tests: label text for chapter + "today/1 day/n days ago"; time hidden when `readAt` is null. Device review (review lane): card above text and resume overlay, TextView and WebView, Dot and Colour, tablet and S22, compared with the board.
Result (Claude, 2026-11-15, label only, uncommitted in `.worktrees/app-chain`): `NovelRecap` carries `previousChapter` (chapter name, only when the paragraphs come from that chapter) and `lastReadAt`; `novelRecapLabel()` joins the known parts; the time is Android's localized relative span (`DateUtils.getRelativeTimeSpanString`, so no new strings); same label in the Compose card, the resume overlay and the WebView script. New `NovelRecapTest.labelJoinsTheKnownPartsLikeTheBoard`; recap tests pass. Tablet WebView: "前回まで · Chapter 1: Listening fixture · 今日" above chapter 2. Then the rest: card padding 20 dp; body 13 sp in the reader's own face via a shared `rememberNovelFontFamily()` lifted out of the settings preview (`presentation/tsuzuki/novel/TsuzukiAllReaderSettings.kt`, same mapping as before); the card above the text is capped at 6 lines with an ellipsis (the resume overlay keeps its scrollable 40 % height). Corner radius stays the theme tile radius (the board's spec row says "same tile as above"). Recap tests pass; scoped Spotless clean (other files restored). Tablet TextView: label, Literata 13 sp, six lines + "…" (`app-chain/tab-recap-f.png`). WebView: `assets/novel-reader/chapter-recap-card.js` forced monospace (Dot) / sans-serif 14 px on the whole card; now the label keeps the UI face and the excerpt uses the page's own font at 13 px with a 6-line clamp. S22 Dot WebView checked (`app-chain/s22-recap-web.png`): label "前回まで · LISTEN-1 · 今日", Literata excerpt, clamp with "…". Note: the blank line between paragraphs counts toward the six lines, so on a phone only one paragraph shows before "…" — acceptable, reviewer may prefer a single line break. Not checked: Colour on S22, TextView on S22.

Implementer note for whoever resumes this review (Claude 2026-11-19, read-only; nothing in `.worktrees/review-recap-f` changed): the last tablet run (`tablet-paged-final-*`, `paged90=true`) passed Dot paged **resume** (HTML chapter) and failed Dot paged **plain** (one `pre` element) with the previous-chapter fallback; the S22 passed all four. Likely cause, not verified: on the tablet's wider paged layout a plain chapter has fewer pages, so 90 % can land on a page with no prose (end card / empty trailing column). Then every sample is before the viewport, the loop in `NOVEL_RECAP_VIEWPORT_SCRIPT` (`NovelRecapViewport.kt`) finishes without hitting a visible or future rect, and the script returns `[]` instead of `before.slice(-16)` → `onResumeViewport` falls back to the previous chapter. Check with the `diagnose` run on plain mode; if confirmed, returning `before.slice(-16)` at the end of the loop recaps this chapter's last paragraphs (the 0.99 finished guard still skips truly finished chapters).

Independent review scope (Codex 2026-11-15): verify Claude's recap card/overlay metadata, reader-font typography and finished-chapter guard on the app-chain snapshot. This chat did not implement RECAP-F/RECAP-FIX. Do **not** treat this as independent approval of our reader/dictionary/TTS ancestor cards or merge the whole app chain; those keep their own gates. Current device lock remains free until this review's first adb command. Owner priority: existing fixes before new anime cards.

#### RECAP-VIEWPORT-F Ignore off-column fragments in the resume excerpt (T2) — `review` (Claude 2026-11-21, `tsuzuki/recap-viewport-f` from `d9b94f259`: `3eeb6f866` the review lane's uncommitted rendered-viewport trial from `.worktrees/review-v02-recap`, unchanged; `b8e394c55` the fix)

Keep the trailing-page fallback correction in the review worktree. Select completed prose by the settled paged viewport and exclude artifacts from the hidden prior column. Retain a paragraph that genuinely continues onto the visible page as incomplete. Assert the exact excerpt at the tablet 90% plain-text position, and repeat with HTML, Dot/Colour, opening, middle and footer cases. Keep both the inline board card and the resume overlay behavior.

Result (Claude, 2026-11-21): the trial's `NOVEL_RECAP_VIEWPORT_SCRIPT` counted a paragraph as on the page when any fragment touched the clip; every line of a prior-column paragraph ends with a ~3 CSS px trailing-space rect that overhangs the column edge. Now a fragment must overlap the clip by **half the paragraph's font size** both ways (a space is ~0.25 em; any real continuation, even one CJK character, is wider). **Replay** of the recorded tablet geometry (`tablet-rects-{DOT,COLOUR}-plain.txt` from the review run, landscape, clip 242–962): old rule → Paragraphs 6–8, new rule → **10–12** in both themes; resume cases unchanged (12–14). **Tablet run** of the review lane's `RecapReviewProbe` against this branch (scratch `v02recapreview.dev`, portrait, clip 16–736): paged 90% plain → **8–10** in Dot and Colour (the page opens with paragraph 11's 536 px continuation; slivers of 1, 2, 5, 7 and 9 are 2–4 px and are ignored; the old rule would stop at paragraph 1 and fall back to the previous chapter), paged 90% resume → 10–12 (page starts at 13); the full 24-case matrix (Dot/Colour × TextView/scroll WebView/paged × above/resume/plain/finished) passes; crash buffer empty; card label "前回まで · CH. 1 REVIEW · 一昨日" on the overlay. `NovelRecap*` JVM tests 21/21. Evidence `.worktrees/recap-viewport-f/app/build/review-evidence/recap-viewport-f/` (paged90 + matrix reports, rect dumps, paged screenshots). The probe was copied in only for the run (not committed). **S22** (same probe, fresh scratch install, clip 16–344): paged 90% plain → 11–13 and resume → 12–14 in Dot and Colour, matching its geometry (the visible page opens with a full-width fragment, no slivers there); 24-case matrix 24/24; crash buffer empty; probe APK uninstalled. Not covered: an HTML chapter at 90% (the probe's paged90 uses the plain-text chapter). Reviewer note: the first commit is the review lane's own code, so neither Claude nor that review thread should review this branch alone.

#### KOKORO-INTEGRATION Clean-runtime Debug integration and listening-test pack — `codex-working` (2026-11-15, `.worktrees/kokoro-integration`, `tsuzuki/kokoro-integration`)

Follow-up code review (Codex Batch T, 2026-11-21, `7c297e232`, `b9f2eca6f`, `75fc7715b`): The named progress callback exposes the boxed `invoke(float[]): Integer` method that JNI expects. Changes requested for the download and prefetch follow-ups. The exact `KokoroPackDownload` code wrote another 64 KiB and emitted progress after Pause canceled the job in a controlled input stream test. `KokoroDownloadDialog.kt:69` cancels without waiting for the old transfer to release the shared partial file. Resume can overlap the old writer and stale progress can overwrite Paused. The twelve new labels exist only in the base locale. `KokoroTtsService.kt:165` stops playback without canceling prefetch. `NeuralTtsClient.kt:141` clears the active request on the phone-voice handoff, so a later stop sends no STOP for the running prefetch. Evidence `.worktrees/review-v02-recap/app/build/review-evidence/v02-kokoro/kokoro-review.txt`. These are JVM and source checks. NT1, NT2, owner listening and publishing gates still apply.

Owner: implement Kokoro first; leave a clear handoff for Claude. Prepare the approved source-built ARM64 runtime with our independently reviewed CMU lexicon, replacing S4's rejected official AAR/upstream lexicon. Implement private-worker Debug playback, safe system fallback, truthful settings and a reproducible checksum-pinned listening pack. Preserve existing queue identity, pause/resume, sleep timer, paragraph seek, highlights and listening stats. NT1 independent integration review, NT2 phone/tablet speed/memory/battery tests and owner listening go remain required before release/preview/FOSS or publishing a pack. No app-lane adb, main merge, push or public release. Base tts-settings; Result pending.

#### KOKORO-TRANSFER-F Serialize download controls and cancel abandoned prefetch (T2) — `review` (Claude 2026-11-21, `91f7e8b16` + `a3e6c779c` on `tsuzuki/kokoro-transfer-f`, on top of `75fc7715b`)

Make Pause close or cancel the active network read, then wait for the old writer before exposing Resume. Do the same before Cancel removes the partial file. Ignore progress from canceled transfers and validate the returned range before appending. Translate the twelve new labels in the other seven shipped locales. Keep prefetch available to the matching phone-to-neural continuation and cancel it on explicit stop, unrelated playback or unbind. Test a blocked read followed immediately by Pause and Resume, and stop while the phone voice is speaking an uncovered chunk. The runtime stays gated by NT1, NT2 and owner listening.

Result (Claude, 2026-11-21): **Download.** `KokoroPackDownload.run` registers a synchronous cancellation handler (`invokeOnCompletion(onCancelling = true)`) that closes the body on the cancelling thread, so a read blocked on the network ends at once (a coroutine dispatched to close it waited behind the blocked worker in the first try); after every read it checks the job before writing or reporting, so nothing is written or reported after cancel. A 206 must start at the partial file's length (`Content-Range` → `Opened.rangeStart`); another offset or a missing header deletes the partial file and fails. **Dialog.** Resume, Pause and Cancel `cancelAndJoin()` the old transfer before Resume opens the file, before Paused (and the Resume button) is shown, and before Cancel deletes the file; a generation counter drops progress, Failed and Installing from an older transfer. **Prefetch.** `NeuralPrefetchSlot` (service): kept for the matching continuation, cancelled on STOP, unbind, destroy, a lost client, a newer prefetch or a request for other text; a new PLAY no longer cancels it. `NeuralStopTarget` (client): after UNCOVERED the handed-off request id is kept, so `stop()` while the phone voice speaks still sends STOP for it; `play()` stops only an active request, so the continuation can use the prepared audio. **Strings:** the twelve download labels in ja, ko, zh-rCN, de, es, fr, pt-rBR. Tests: `KokoroPackDownloadTest` 9/9 (+ stalled read then Pause: ends within the join, file length and progress unchanged afterwards, Resume with a range request completes the same bytes; wrong/missing 206 offset; Content-Range parsing), `NeuralPrefetchTest` 4/4 (matching continuation, unrelated text, newer prefetch, stop; stop during the phone-voice hand-off names the worker's request), `NeuralSessionTest` 6/6. Not run on a device; the runtime stays gated by NT1, NT2 and the owner's listening test. `app/libs/*.aar` stays untracked.

#### TTS-SETTINGS Discoverable detailed speech settings — `review passed (device), not merged` (Claude device check 2026-11-15: gear pauses narration, speed change applies on Resume at the same paragraph, sample plays; string/icon/wrapping defects fixed by TTS-POLISH, passed 2026-11-19. 2026-11-19 tablet: settings search (rail 検索 inside Settings) finds 「エンジンとインストール済みの音声 — 読み上げ › 端末の音声」 for "Android"; the English word "TTS" finds nothing on a Japanese device (index is the translated text; fine); speed 1.0× → 2.4× survives force-stop + relaunch. Not checked: engine change, no-engine recovery, S22 after the fix. Merges with `review/app-chain`) (2026-11-15, `.worktrees/tts-settings`, `tsuzuki/tts-settings`)
Owner asks for better TTS, detailed controls, and settings that are easy to find. Add a top-level Text-to-speech Settings entry, searchable controls, and an entry directly from Listen. Reuse phone-voice portion of approved Voices board 5 and existing Settings/TTS board components. Expose installed system voice/language choices with offline/online labels and a sample, system engine/voice installation settings, speed, pitch, auto-next/background/highlight options. Keep existing preferences compatible; no simulated neural voice. Kokoro integration remains gated by NT1/NT2 and the owner listening test; this request does not waive those gates. Base dictionary-defaults; app-lane no adb.
Implementation: top-level Settings › Text-to-speech (summary includes TTS) and settings-search index; Novel Reader also links there. Both Listen layouts open the same preferences in an adaptive sheet, pausing narration and cancelling a pending autoplay request before opening. Voice picker contains installed phone voices only, language filter, online/offline indicators, better engine-reported quality first within a locale, selected sample, speed/pitch, background/auto-next/autostart, read-along/follow/style and reset. Voice/sample defaults use the actual engine locale; app settings search never initializes speech. Preview engine binds on resume, stops/shuts down on pause/disposal, ignores late init callbacks and refreshes after Android settings. Android TTS settings manages engine selection/downloaded voice data; **changing the system engine requires reopening the novel**. Existing voice/speed/pitch preferences reused; no controller/queue migration, new neural engine, voice download or cloud service.
Device review required: Dot/Colour on phone/tablet; top-level entry/search/Novel Reader links; Listen gear, narrow dock fit, adaptive sheet and Back returning to the same paragraph; narration stays paused during samples; preview stops on close/background; voice/speed/pitch applied on Resume; system engine/data changes refresh voice list, unavailable voice not silently sampled as another voice; offline/network flags; no installed engine/data recovery; reset; setting changes persist across restart. Kokoro runtime/pack/performance and owner listening gates remain as recorded under NT1/NT2. Validation: JDK21 Debug assembly + **1,099 app tests**, zero failures/errors/skips. New tests verify installed-voice filtering/quality order and pausing an active queue while the engine is silent, invalidating pending handoff and preserving the resume position. Scoped formatting/diff checks pass; final APK 16 KB alignment and inherited bundled-dictionary APK integrity audit pass. `TtsController.pause` now closes an active queue even if the engine is briefly silent, preventing auto-next during settings/sample playback. No app-lane adb or listening-quality claim. Logs/audits in `.worktrees/tts-settings/app/build/tts-settings-*.log` and `tts-settings-dictionary-apk-audit.json`. Implementation **`a039a7e5d`**; branch `tsuzuki/tts-settings`, needs its dictionary-defaults/tracker-clients ancestors. ARM64 Debug APK SHA-256 `fdceeaaa97bc3c4c68839e3babe4a5306bf1dae425a51854ee3a7eb04b54f7aa`, 135,728,108 bytes.

#### DICTIONARY-DEFAULTS Preinstalled local dictionaries and ready-to-use free API — `changes needed → DICT-F` (2026-11-19: kanji-only Japanese lookups miss JMdict; strings untranslated) (Claude device check 2026-11-15, see log: WordNet in both renderers on tablet + S22, JMdict and Online pill on tablet; 2026-11-19 tablet: **WebView Japanese fails → DICT-F** (kanji-only 図書館 opens the Chinese "not installed" sheet; 18 dictionary strings untranslated in ja). 2026-11-19 tablet: **selection toggled on after opening passes** (chapter opened with `pref_novel_text_selectable=false`: long-press gives no menu; switched on in 全設定 › 操作 without leaving the reader: long-press shows 引用として保存 / 辞書 / コピー / 翻訳 / すべて選択 / 共有; `dict-claude/`). Not checked: WebView Japanese, upgrade over an old pack. **Merge waits on the owner's APK-size decision (+29 MB)** and `review/app-chain`) (2026-11-15, `.worktrees/dictionary-defaults`, `tsuzuki/dictionary-defaults`)
#### KOKORO-INTEGRATION Clean-runtime Debug integration and listening-test pack — `codex-working` (2026-11-15, `.worktrees/kokoro-integration`, `tsuzuki/kokoro-integration`)
Owner: implement Kokoro first; leave a clear handoff for Claude. Prepare the approved source-built ARM64 runtime with our independently reviewed CMU lexicon, replacing S4's rejected official AAR/upstream lexicon. Implement private-worker Debug playback, safe system fallback, truthful settings and a reproducible checksum-pinned listening pack. Preserve existing queue identity, pause/resume, sleep timer, paragraph seek, highlights and listening stats. NT1 independent integration review, NT2 phone/tablet speed/memory/battery tests and owner listening go remain required before release/preview/FOSS or publishing a pack. No app-lane adb, main merge, push or public release. Base tts-settings; Result pending.
#### KOKORO-INTEGRATION Clean-runtime Debug integration and listening-test pack — `review` (code half; device half pending Claude) (2026-11-15, `.worktrees/kokoro-integration`, `tsuzuki/kokoro-integration`)
Owner: implement Kokoro first; leave a clear handoff for Claude. Prepare the approved source-built ARM64 runtime with our independently reviewed CMU lexicon, replacing S4's rejected official AAR/upstream lexicon. Implement private-worker Debug playback, safe system fallback, truthful settings and a reproducible checksum-pinned listening pack. Preserve existing queue identity, pause/resume, sleep timer, paragraph seek, highlights and listening stats. NT1 independent integration review, NT2 phone/tablet speed/memory/battery tests and owner listening go remain required before release/preview/FOSS or publishing a pack. No app-lane adb, main merge, push or public release. Base tts-settings.
Result (Claude, taking over from Codex, 2026-11-15; Codex's WIP is commit "Codex WIP (as left)", `tsuzuki/tts-settings` merged on top): **code half done, nothing run on a device.** An independent assessment found four defects; all fixed:
1. *Sticky fallback.* The worker now reports `UNCOVERED(index)` separately from `FAILED`. `NeuralSession` (new, testable with a fake player) makes the phone voice speak just that chunk under the same queue and utterance ids (highlights and listening stats keep working), then resumes neural at the next index. Real failures: retried once per chapter on a freshly killed worker process, then the rest of that chapter uses the phone voice; the next chapter starts neural again. `kokoroModelText` now normalises `…`, curly quotes, accents (NFKD), spaced and unspaced dashes, hyphenated words and edge apostrophes; the same string is checked and sent to the runtime. Not compared with the runtime source (it is not in the repository); the listening test is the check. Digits, `@`, URLs and non-Latin text stay uncovered.
2. *Watchdog.* `neuralWatchdogMs` scales with chunk length and speed (20 s + 200 ms per character / speed), plus a 120 s cold-start allowance until the worker has answered once; bind timeout 30 s. Retry kills the `:neural_tts` process so a hung native call cannot hold the runtime lock.
3. *First audio.* The first chunk of each request is synthesized as two pieces (first sentence, then the rest); highlights/stats still see one start and one done per chunk. One piece ahead is still prefetched.
4. *Install.* Debug Settings > Text-to-speech has Install / Remove test voice pack (document picker; `KokoroPackImporter` streams the zip, allowlist `kokoro-cmu-v1/<manifest file>` only, per-entry byte caps, staging dir, `KokoroPack.verify`, then rename). The switch can always be turned off and Remove turns it off. Toggling the voice mid-chapter re-chunks from the source text (`TtsChunker`, position mapped by character offset); chunks over 180 characters are never sent to the worker. The route via `adb run-as` and the archive URL/SHA are in `docs/research/neural-tts-clean-lexicon.md` section 5.
Worker logs `KokoroTiming` lines (`load verifyMs/modelMs`, `firstAudio afterRequestMs`, per-chunk `synthMs/audioMs/rtf`) for the device half. Device half (Claude): see the script in the commit message of the docs commit and below the table; NT1 integration review, NT2 speed/memory/battery and the owner listening go remain required. `app/libs/*.aar` is intentionally **untracked** (owner decision pending).

#### TTS-SETTINGS Discoverable detailed speech settings — `review` (2026-11-15, `.worktrees/tts-settings`, `tsuzuki/tts-settings`)
Owner asks for better TTS, detailed controls, and settings that are easy to find. Add a top-level Text-to-speech Settings entry, searchable controls, and an entry directly from Listen. Reuse phone-voice portion of approved Voices board 5 and existing Settings/TTS board components. Expose installed system voice/language choices with offline/online labels and a sample, system engine/voice installation settings, speed, pitch, auto-next/background/highlight options. Keep existing preferences compatible; no simulated neural voice. Kokoro integration remains gated by NT1/NT2 and the owner listening test; this request does not waive those gates. Base dictionary-defaults; app-lane no adb.
Implementation: top-level Settings › Text-to-speech (summary includes TTS) and settings-search index; Novel Reader also links there. Both Listen layouts open the same preferences in an adaptive sheet, pausing narration and cancelling a pending autoplay request before opening. Voice picker contains installed phone voices only, language filter, online/offline indicators, better engine-reported quality first within a locale, selected sample, speed/pitch, background/auto-next/autostart, read-along/follow/style and reset. Voice/sample defaults use the actual engine locale; app settings search never initializes speech. Preview engine binds on resume, stops/shuts down on pause/disposal, ignores late init callbacks and refreshes after Android settings. Android TTS settings manages engine selection/downloaded voice data; **changing the system engine requires reopening the novel**. Existing voice/speed/pitch preferences reused; no controller/queue migration, new neural engine, voice download or cloud service.
Device review required: Dot/Colour on phone/tablet; top-level entry/search/Novel Reader links; Listen gear, narrow dock fit, adaptive sheet and Back returning to the same paragraph; narration stays paused during samples; preview stops on close/background; voice/speed/pitch applied on Resume; system engine/data changes refresh voice list, unavailable voice not silently sampled as another voice; offline/network flags; no installed engine/data recovery; reset; setting changes persist across restart. Kokoro runtime/pack/performance and owner listening gates remain as recorded under NT1/NT2. Validation: JDK21 Debug assembly + **1,099 app tests**, zero failures/errors/skips. New tests verify installed-voice filtering/quality order and pausing an active queue while the engine is silent, invalidating pending handoff and preserving the resume position. Scoped formatting/diff checks pass; final APK 16 KB alignment and inherited bundled-dictionary APK integrity audit pass. `TtsController.pause` now closes an active queue even if the engine is briefly silent, preventing auto-next during settings/sample playback. No app-lane adb or listening-quality claim. Logs/audits in `.worktrees/tts-settings/app/build/tts-settings-*.log` and `tts-settings-dictionary-apk-audit.json`. Implementation **`a039a7e5d`**; branch `tsuzuki/tts-settings`, needs its dictionary-defaults/tracker-clients ancestors. ARM64 Debug APK SHA-256 `fdceeaaa97bc3c4c68839e3babe4a5306bf1dae425a51854ee3a7eb04b54f7aa`, 135,728,108 bytes.

#### DICTIONARY-DEFAULTS Preinstalled local dictionaries and ready-to-use free API — `review` (2026-11-15, `.worktrees/dictionary-defaults`, `tsuzuki/dictionary-defaults`)
Owner requests local dictionaries and free dictionary API ready in the app, and reports dictionary lookup failing in library novels. This supersedes R13's optional-only data packaging and no-source-download restriction for this card: build real licensed English/Japanese data from official sources, bundle independently licensed data with attribution, preserve updates and installed packs. Use a keyless public API instead of inventing or shipping private API credentials. Inspect both renderer selection paths, add meaningful pack/lookup/network tests, build and format; app-lane implementer does not use adb. Base is tracker-clients to retain the owner's previous fixes. Board: existing Dictionary Dot/Colour, reuses existing pills and definition sheet.
Result (Codex, 2026-11-15): owner clarified **Dictionary action missing**. TextView now installs the callback even when selection starts disabled; both renderers restore the idempotent action during preparation, ensure it is visible/enabled, and keep it on the toolbar. WebView handles both action-mode entry points. Full WordNet 3.0 (155,287 entries) and JMdict English-gloss snapshot (325,397 headword/reading entries) bundled as independently licensed gzip `.pack` assets, about 29 MB compressed / 87 MB extracted. Original optional release packs were never published; no-data lookup is separately resolved by bundling. Extract on IO at first use, verify checksum/size/schema, atomic rename, keep downloaded overrides and revert to built-in after deleting an override. Source attribution, complete licences and JMdict documentation included. Explicit English Online pill calls Free Dictionary API with no key/account; selected word only, 10 s timeout, 1 MiB reply cap, source/licence links, retry and existing web/system fallbacks. Japanese stays local plus web/system fallbacks. No private API keys.
Validation: final Debug build and **1,097 app tests** pass, zero failures/errors/skips; includes full bundled English/Japanese lookup with real SQLite and no network, inflections, malformed overrides, menu rebuild/visibility and keyless API/404/429/size limit. Two converter tests pass. Scoped formatting/diff checks pass; final APK pack-byte/checksum/schema/licence audit and 16 KB alignment pass. The APK audit caught Android transparently expanding `.gz` assets; `.pack` preserves bytes and the new `scripts/check_bundled_dictionaries.py --apk ...` guards against this packaging regression. Live API tested with public word book. No app-lane adb; **device selection regression remains unverified**, including actual library novels and selection toggled on after opening. Full checklist, source/licence details and rebuild commands in `docs/DICTIONARY_PACKS.md`; evidence in `.worktrees/dictionary-defaults/app/build/dictionary-*.log` / `dictionary-*-audit.json`. Implementation **`a6d8b27ea` + required binary correction `1d1b8e194`**, branch `tsuzuki/dictionary-defaults` (needs its tracker-clients/details-refresh-tracking ancestors). Fresh ARM64 Debug APK SHA-256 `236e28ac7116fc5a399a4680762072ab535f4e0754bc16ff1cbcb5ea99cb386c`, 134,910,132 bytes. Committed asset bytes pass the integrity audit (`--git`) **at `1d1b8e194` or later**; `.gitattributes` explicitly marks `.pack` binary and both assets were re-staged with `--renormalize`. Earlier implementation commits alone contain normalized/corrupt pack bytes: review the full branch including the correction. Working-tree and final APK bytes were already correct, so APK/test results remain valid.

#### TRACKER-CLIENTS Tsuzuki-owned consent-page registrations for all trackers — `blocked` (2026-11-15, `.worktrees/tracker-clients`, `tsuzuki/tracker-clients`; MAL stage ready for review, remaining providers await owner sign-in/access)
Owner requested MAL and all other trackers to use Tsuzuki. Name shown by each consent page belongs to its registered OAuth client. Register **MyAnimeList, AniList, Bangumi, Shikimori, MangaBaka and Hikka**, using the facts and callbacks in `docs/TRACKER_BRANDING.md`; do not invent IDs, secrets, provider accounts or change unrelated API paths. Kitsu uses its documented shared public password-grant client; the seven remaining trackers have direct/server/session authentication rather than upstream OAuth consent applications.
MAL: owner explicitly approved the prepared registration and agreement (2026-11-15). Submitted successfully in the owner's existing MAL account; registration is **PUBLISHED**, named Tsuzuki, Android/non-commercial/hobbyist, GitHub homepage, callback `tsuzuki://myanimelist-auth`. Live consent page visibly says Tsuzuki in its title, project link and permission text. No list-access grant submitted. The app now uses the owned client for authorization, exchange and refresh; manifest adds only the MAL Tsuzuki callback, retaining all six legacy callbacks. Local tracker records stay intact; existing sessions may need sign-in again. No client secret committed.
MAL validation: debug assembly and **1,091 app tests** pass (154 suites, zero failures/errors/skips); scoped formatter and diff checks pass. Merged ARM64 manifest resolves the exact new MAL callback plus six legacy callbacks. Only two production files changed in this stage. No app-lane adb. Build/format logs in `.worktrees/tracker-clients/app/build/tracker-clients-*.log`. MAL-stage implementation **`13c5a42c5`**, ready for review on `tsuzuki/tracker-clients` (based on `tsuzuki/details-refresh-tracking`).
Independent code review of the MAL stage (Claude 2026-11-19, `13c5a42c5`): **pass (code)**. `TrackLoginActivity` dispatches on `uri.host` only, so `tsuzuki://myanimelist-auth` reaches `handleMyAnimeList` without other changes; the dedicated `tsuzuki` scheme also avoids an app chooser when Tsundoku (which owns `tsundoku://`) is installed. Public PKCE client, no secret; existing MAL sessions were issued to the old client and will need one new sign-in (documented). The anime MAL adapter shares the same `MyAnimeListApi` constants. **Not checked:** an actual sign-in/refresh on a device (needs the owner's MAL login; Claude does not enter credentials). Merges with the app chain it is based on.
Remaining: AniList developer settings redirected to the signed-out homepage; owner sign-in requested. Bangumi, Shikimori, MangaBaka and Hikka need provider account access and required action-time registration/terms approvals. MangaBaka must be a public native PKCE client. Do not claim all external consent pages changed from request-header edits alone. App sign-in, token refresh, anime tracker and device callback checks remain for review; implementer does not use app-lane adb.
Kokoro answer supplied: the owner means the runtime built here, not a standalone Android TTS engine. The runtime is audited, **not integrated into released Listen controls**; no installed usable voice pack/enable switch is claimed. NT2 licensing/listening gate and NT3 integration remain open.

### Batch AY — Aniyomi parity (Claude, 2026-11-12) — lane 1 — **open again since the 0.1.1 tag (2026-11-15); owner: anime is the priority**

**Anime restart (Claude, 2026-11-15).**
- `tsuzuki/a6.4` got its unfinished work committed as it was (`028a0c06d`, owner allowed this on 2026-11-12):
  ordered preferred audio/subtitle languages, per-anime manual override, two new unit tests. Never seen on a device.
- **`tsuzuki/anime-sync` (`a7a6b3f9f`, `.worktrees/anime-sync`) = the whole anime chain (A2.3 … A6.4, 40 commits)
  merged with `main` at v0.1.1.** Debug builds; **1,187 unit tests, 0 failures**. Eight code conflicts resolved
  keeping both sides (global search retry + anime search; backup validation/counts + anime backup; download queue;
  stats). Migrations: `main` stops at 36, anime adds 37–39, `36.sqm` unchanged since v0.1.0. Done by a Claude
  subagent, read by nobody else yet.
- Known gaps from the merge: anime global-search results have no per-source Retry; the restore preview does not
  count anime; three anime activities are not gated by the setting (they are `exported="false"`); anime
  notification channels and the anime paths in `UpdatesViewModel`/`HistoryViewModel` were not checked for the gate.
- **Updated rule after RV3-S acceptance: every new anime card branches from accepted `main` (or its preceding card), retaining `a3740a40c` and `03128b5ca`; never
  from `tsuzuki/a6.4` or older.** Where a card says "needs `tsuzuki/a6.4`", read `tsuzuki/anime-sync`.
- **Order now:** AY-PLAY and RV3-S passed. Local main accepts the whole chain, anime off by default. **AY9** → AY0 → AY1 → AY2 → AY3 → AY4 → AY6 → A6.5 → AY5 → A6.8 → AY7 → A6.6 → A6.7 → AY8.
- Anime implementers test their own cards with adb on the tablet and S22 (owner 2026-11-12), **scratch package
  only** (`…tsuzuki.animescratch` via an init script like `app/build/perf-details/scratch.init.gradle`; never the
  `.dev`, daily or `rv5debugscratch` apps), Device lock first.
- Prompts for the owner: review lane `Read AGENTS.md and docs/HANDOFF.md. You are the review lane: do RV3-S as "Anime restart" in Batch AY describes.` Anime lane: `Read AGENTS.md, docs/HANDOFF.md and docs/ANIME_ANIYOMI_PARITY.md. You are the anime lane: do the device half of AY-PLAY on tsuzuki/ay-play (run its device script with three real sources, fix what the TsuzukiAnime log shows), then AY9.`

Detail for every card, the Aniyomi files to read, the code rules and the device-test procedure are in
**`docs/ANIME_ANIYOMI_PARITY.md`** (§1 reference checkout, §3 rules, §5 cards). Read §1–§3 once, then your card's
section; the card text here is only the summary. Each card: worktree from `main`, merge the Needs branch, follow
§3, write a **device script** (numbered steps + expected result, using the AY0 fixture), **run it yourself on the
tablet and the S22** (owner 2026-11-12: anime-lane implementers may use adb; PARITY §3 has the limits), and put the
script with what you saw at each step in Result. The review lane re-runs it on the merged branch. Aniyomi is
Apache-2.0: adapting its code is allowed with the header line from §1.

**Device lock** (one thread on the devices at a time; edit this line before and after): `Devices: Claude 2026-11-24 (tablet only, claudeqa = AP integration tree; S22 not connected)` **Still open from the stopped Codex RECAP-F review (2026-11-15 22:49):** its last tablet run failed ("Later paged position incorrectly fell back to the previous chapter", `.worktrees/review-recap-f/app/build/review-evidence/recap-f/tablet-paged-final-instrumentation.txt`); `animescratch` is **unrestored with `animescratch.test` installed on both devices**, and its review fixes are uncommitted in `.worktrees/review-recap-f`. Claude did not touch them; `tablet-before.tar` / `s22-before.tar` and `restore-scratch.py` are there. Owner decides: resume that review, or restore.

**Lane 1 prompt (anime):** `Read AGENTS.md, docs/HANDOFF.md and docs/ANIME_ANIYOMI_PARITY.md. You are the anime lane: finish A6.4 if it is still yours, then take the next ready card in the Batch AY order.`

**Owner 2026-11-15: the first anime release (v0.2) ships after seven cards (plus AY-INT, owner request the same day): AY9, AY0, AY1, AY2, AY3, AY4, AY6, then one chain review, release audit and upgrade test from 0.1.1. A6.5, AY5, A6.8, AY7, A6.6, A6.7 and AY8 move to a later update; do not start them before the release.**

**Order:** A6.4 → **AY-PLAY** → **AY9** → AY0 → AY1 → AY2 → AY3 → AY4 → AY6 → A6.5 → AY5 → A6.8 → AY7 → A6.6 → A6.7 → AY8. Each card's Needs is the
card before it in this order (`tsuzuki/ay0` needs `tsuzuki/a6.4`, `tsuzuki/a6.5` now needs `tsuzuki/ay6`, and so on).

#### AY-PLAY Anime: videos from extensions do not play (T4) — `done` (code + device review passed RV3-S, 2026-11-15; tsuzuki/ay-play 644bb44e4)
PARITY §7. Claude reproduced it on the tablet preview build: AnimeKai resolves after 25+ s, then sits at
23:35 / −00:00 with a black picture and a dead Play button; AnimePahe shows "unknown error"; the app logs nothing
from mpv. Evidence in `app/build/review-evidence/ay-play/` (main checkout). First make mpv and source errors
visible, reproduce and write down the real error, then fix: start-position clamp, Play after end, source header
fallback, one shared `AnimeSourceManager` (extensions are reloaded three times per episode open), readable
source errors. Implementer tests on the tablet and S22 with three real sources.
Result, code half (Claude subagent, 2026-11-15; **no device, nothing here is shown to fix playback**): five commits on `tsuzuki/anime-sync`; 1,206 unit tests, 0 failures (19 new); Debug builds; no upstream file touched.
- `e0f3eb669` + `73a61f538` **failures visible.** Before: mpv ran with verbose logging but no log observer was attached, resolve exceptions were swallowed, the UI said "Playback failed" / `unknown_error`. Now everything logs under tag **`TsuzukiAnime`** (`tsuzuki/anime/AnimeLog.kt`): open episode, extensions loaded (+ms), source returned N videos (+ms), resolved/failed video, `mpv loadfile`, `mpv FILE_LOADED` / `mpv end-file error: <reason>`, and mpv's own warn/error lines as `mpv/<prefix>:`. URLs are cut to the host; no tokens, cookies or paths. The player and catalogue error tiles show the ERR1 heading plus a redacted technical line.
- `5bd9798c2` **start and end.** Confirmed by reading: the saved position was passed to mpv with no check against the duration, Retry reused the current position, and Play after the end called a seek that returns early when the duration is unknown. Now a position within 10 s of the end (or ≥ 90 %) starts at 0, re-checked when mpv reports the duration; Play after the end seeks to 0 or reloads. `AnimePlayerPolicy.kt`, 7 tests. Whether this was the 23:35 black screen is not known.
- `f746985a0` **headers and order.** Confirmed against Aniyomi: a video without its own headers now gets the source's headers (before, mpv's default User-Agent); the source's `sortHosters()` / `sortVideos()` are applied. Left for AY1: `preferred` winning over resolution, parallel/lazy hosters, `mpvArgs`, per-hoster error isolation.
- `c7c241670` **one source manager.** Confirmed: a new manager loading every extension was built in the player, catalogue, downloads and browse; `AnimeLibraryUpdateJob` asked Injekt for one that nothing registered. Now one singleton with `ensureLoaded()`; reloads when the app's installer reports a change. Caveat: a system-level install/uninstall is only noticed when Browse resumes or the update job runs.
- **Device script for the anime lane** (debug build as `…tsuzuki.animescratch`, anime on, `adb logcat -s TsuzukiAnime:V`): (1) Browse → an anime source → Popular: a failing row shows heading + technical line; the `catalogue … failed` log line gives the cause (HTTP 403/Cloudflare, parse, or `Incompatible anime source` = AY6). (2) Open an unwatched episode: `extensions loaded` once, not per episode; `source returned … (+ms)` says whether a long wait is the source; `resolved video … headers=[…]`; then `mpv FILE_LOADED` or `mpv end-file error` with the `mpv/…` lines before it: **those lines are the real cause if it still doesn't play; fix from them.** (3) Resume mid-episode resumes; resume in the last 5 s starts at 0. (4) Play after the end restarts. (5) Retry on a failed episode does not reopen at the end. Crash buffer empty. Three real sources, tablet and S22.
- Not done: the 30 s "no FILE_LOADED" watchdog and per-hoster timeout (AY1); PARITY §7 items 3 and 4 (episode label, portrait).
Result, device half (Codex, 2026-11-15): **review** in `.worktrees/ay-play`, `tsuzuki/ay-play`; implementation **`5d9c530da`**.
- Reproduced the actual AnimeKai black screen at start 0: the source returned four videos, mpv reported HLS video codec `png` with no dimensions, repeated `Invalid argument`, then immediate EOF. APK inspection showed the extension supplies `demuxer-lavf-o=force_mpegts=1`; the pinned native build already includes that FFmpeg option. `AnimeMpvLoad.kt` now passes extension options and a validated start as file-scoped `loadfile` options. Modified-UTF-8 length quoting matches the JNI wrapper and preserves nested commas/quotes. No native library changed.
- `MpvAnimePlayerLauncher.kt`: use the command builder. Player-only manifest opt-out restores the requested landscape on Android 16 tablets; reviewed Dot/Colour Player boards first. `ExtensionSignatureTest.kt`: replace obsolete `isNsfw` argument with `ContentWarning.SAFE` so device-test compilation works. Four new command regressions; 1,210 app unit tests pass, Debug APK builds, two per-file Spotless passes and `git diff --check` pass; 16 KB ZIP alignment passes.
- Device script executed on the verified Tab S11 and S22, package **`io.github.manojx2005.tsuzuki.animescratch` only**: (1) load three installed real sources; trust only AnimeKai/Kotokai, AnimePahe and Anikage in scratch (S22 copies loaded privately, no system installs); (2) AnimeKai One Piece episodes 1 and 2, new and saved at 600 s, Dot then Colour: file-loaded 6.3–8.8 s, video and position advance in captures five seconds apart; (3) seek to EOF, press Play: restarts at 0 and advances; (4) saved 1473/1478 s: reopens at 0 and advances; (5) Anikage episodes 1 and 2 in both themes: source returns zero videos and the player shows the readable no-playable-video error; (6) AnimePahe initially rate-limited with readable copy, recovered on retry; two episodes subsequently play and advance on both devices in Colour. Mid-episode resume/Dot playback for AnimePahe were not separately run. Audio-focus requests observed. Correct episode 1/2 labels; tablet now landscape. Final formatted APK reinstalled on both and AnimeKai smoke passed again. Crash buffers empty; 44 matrix screenshots and final Dot Homes inspected.
- Evidence: `.worktrees/ay-play/app/build/review-evidence/ay-play/` (`final-audit.json`, original/fixed mpv logs, probe transcripts, archived temporary instrumentation driver, screenshots/montages). Final ARM64 scratch APK SHA-256 `b892ca50cd6be36d56218fd23e208c62371f3b0ca9d4f23ba5ec1a74af441628`. Temporary driver removed from source. No daily app or owner-phone install, no main merge/push.
- Left for AY1: per-hoster timeout, missing-FILE_LOADED watchdog, parallel/lazy hosters and the other documented parity gaps. `mpvArgs` has been pulled forward from AY1 because the real-source reproduction required it. No deterministic AY0 fixture or installed-Aniyomi comparison (typo corrected below); no authenticated trackers tested. Independent RV3-S review precedes AY9.

#### AY9-WATCH-TABLET Watch home on tablets (T2) — `passed` (Claude 2026-11-22, `bba2b96fe` on `tsuzuki/ay9-watch-tablet`; owner: "use Claude design to fix and choose it")

Watch home in tablet landscape had a full-width resume picture (about 650 dp tall) that pushed the title and the library below the fold, and there was no tablet board. Owner delegated the design. **Choice:** at 600 dp and wider the resume hero is horizontal, with the 16:9 preview on the left (55 %) and the label, title (3 lines), episode (2 lines) and play button on the right. Watch side padding is 28 dp, as on Home. Phones keep the boarded vertical hero. One file, `presentation/tsuzuki/anime/TsuzukiAnimeLibrary.kt`. **Tablet**, scratch, AY0 fixture case 01 with saved progress, probe-frozen rotation: Dot and Colour, landscape and portrait — the hero sits beside its text, the library row is visible below it, and the crash buffer is empty. Evidence and probe `.worktrees/ay9-watch-tablet/app/build/review-evidence/watch-tablet/`. Merged into `review/v02-rc-trial`. Self-checked (owner waiver).

#### AY9-ART Thumbnails load in long episode lists (T2) — `passed` (self-review under owner waiver 2026-11-22; with AY9-ART-F)

Self-review (Claude, 2026-11-22, **owner waiver**: "Codex doesn't have usage left, you can do the reviews instead"; Claude wrote this card): **PASS** together with Codex's AY9-ART-F (URL boundaries). Shared artwork is fetched and decoded once; captured frames stay per episode.


Found while measuring AY9 on the S22 (see the AY9 card): every episode row keyed its artwork request by episode, so a 1,000-episode show whose episodes share a preview image or fall back to the cover made one paced network request per row, and the visible rows stayed as empty placeholders while scrolling.

Result (Claude, 2026-11-22): `AnimeEpisodePreview.cacheKey` keeps the episode id only for a captured frame (`frameVersion > 0`); shared artwork (`frameVersion == 0`) is keyed by source, preview URL, cover URL and network policy, so Coil fetches and decodes it once. `AnimePreviewFetcher` reads a captured frame only when `frameVersion > 0`, so a shared key never caches one episode's frame. `AnimePreviewPolicyTest` 6/6. **S22, 1,000-episode fixture:** baseline rows stayed blank after ten flings, with one fixture GET per row (1–2/s); with the fix every row showed artwork after about 3 GETs (`measure-ay9/s22-shots/`, `measure-ay9/s22/fixture-ay9-measure.log`). Frame cost is unchanged (see the AY9 card: with real flings the list is smooth). Not checked: a source with a distinct preview per episode, where nothing is shared and each row still needs its own request.

#### AY9-ART-F Keep artwork cache keys distinct (T2) — `review` → code review passed (Claude 2026-11-22); lands with the anime chain

Code review (Claude, 2026-11-22; Codex wrote this card, so independent): **passes in code.** Length-prefixed URLs keep field boundaries and tell null from empty; the frame path stays keyed per episode. Not mergeable alone: it sits on `tsuzuki/ay9-art` (Claude) and AY9.



Result (Codex, 2026-11-22): `AnimePreviewPolicy.kt` length-prefixes the preview and cover URLs, preserving cross-episode artwork reuse and per-episode frame isolation. `AnimePreviewPolicyTest.kt` checks the original collision for both key paths and distinguishes absent URLs from empty or literal values. Scoped Spotless, Debug assembly and 1,390 all-module tests passed. Evidence: `.worktrees/ay9-art-f/app/build/review-evidence/ay9-art-f/build-tests.log`. Integration trial `012d6714f` passed Debug and all 1,473 unit tests. On the tablet in portrait, every visible episode row in a 1,000-episode fixture showed artwork in Dot and Colour. The scratch app and probe were removed, the crash buffer was empty, and the tablet returned to the launcher. The S22 had no verified adb transport. Independent review and live-source acceptance remain open. Evidence: `.worktrees/v02-anime-chain/app/build/review-evidence/ay9-art-integrated/`.

#### AY9-TILE-CONTRAST Keep episode download icons visible (T1) — `review` → code review passed (Claude 2026-11-22); lands with the anime chain

Code review (Claude, 2026-11-22; Codex wrote this card, so independent): **passes in code.** `tokens.dotOn` / `tokens.mutedText` match the tile's other icons; disabled logic unchanged.



The integrated tablet screenshots show black download icons on dark tiles in Dot and Colour. Match the AnimeDetails board with theme foreground colors, preserving disabled states and click behavior.

Result (Codex, 2026-11-22): `AnimeEpisodeTiles.kt` takes the enabled and disabled button colors from `TsuzukiTheme.tokens`. Scoped Spotless, Debug assembly and the all-module tests passed. The first scratch attempt lacked the fixture source. The integrated trial passed the fixture in both themes on the tablet, with screenshots confirming the enabled arrow changed from black to white. The S22 had no verified adb transport. Disabled styling was checked in code. The scratch packages were removed, the tablet returned to the launcher and its crash buffer was empty. Evidence: `.worktrees/v02-anime-chain/app/build/review-evidence/ay9-tile-contrast/`.

#### AY9 Anime: video previews on Watch home and episode rows (T3) — `blocked` (code review passed by Claude 2026-11-22; live One Piece and tablet landscape still open)

Code review (Claude, 2026-11-22; Codex implemented `eff92bac9` + `7ba2fca41`): **passes in code.** The frame store waits for a complete PNG (IEND) on IO, drops stale captures by generation and sequence, honours incognito, prunes on unfavourite/replace/refresh and caps at 100 MB. `AnimeFrameStore` is a lazy singleton reached only from anime code, so anime-off cold starts never build it. In the merged trial `detach(closing)`, `capturePreview(closing = true)` and `cancelPendingLoad()` all survive. Nit: several lines exceed 120 characters (CI does not run Spotless; fix with a formatting commit).

Tablet landscape (Claude, 2026-11-22, RC trial `3baf1bdbe` Debug as scratch `…clauderevscratch`, AY0 fixture case 01, progress saved at 20 s; rotation frozen by the probe through `UiAutomation`, no system setting changed): **anime details pass** in Dot and Colour — two panes, both episode rows show the fixture preview, episode 1 shows its progress, download icons are white on the tiles, Continue row reads 第1話の続きを再生 00:20. **Watch home works but needs an owner decision:** the resume hero fills the full landscape width (about 650 dp tall), so the title and the rest of Watch sit below the fold. The only board is the 390×844 phone `png/10-anime/AnimeHome.png`; there is no tablet or landscape Watch board, so Claude did not change it. **Owner:** draw or describe Watch on a tablet (cap the hero width, or put it beside the list as `TabletLandscapeHome` does for books), or accept it as is for v0.2. Crash buffer empty. Evidence and probe source `.worktrees/v02-rc-trial/app/build/review-evidence/ay9-landscape/`. Live One Piece remains open.


PARITY §8. Boards `10-anime/AnimeHome` (continue-watching card = 16:9 frame of where you stopped, "EP n · mm:ss
LEFT" chip, progress line) and `10-anime/AnimeDetails` (16:9 thumbnail per episode row with duration chip and
progress line). Frame = last-watched frame captured from mpv → `preview_url` through the source's client → cover
crop → gradient. The Library grid keeps covers, as on its board.
Progress (Codex 2026-11-15): **implementation and device QA in progress**, uncommitted in `.worktrees/ay9`. Shared private frame store, source-client Coil fetcher, fixed 104×58 episode thumbnails and 16:9 Watch hero, preview On/Wi-Fi/Off settings and 8 locales implemented. Debug scratch + **1,222 app tests** pass. Six new pure policy tests plus DB cleanup callback regression. Scoped formatting twice. Independent second reviewer found five edge cases; disk fallback race, response-body cancellation, class-only error logs, stale capture sequencing and stale temp sweep now fixed and reread without blockers.

Device findings: scratch snapshots captured for both devices under `app/build/review-evidence/ay9/`. Both pass actual 640-wide JPEG resize, black-frame preservation, disallowed capture, generation and cleanup checks. Both pass real 20-second pause capture. **Direct close initially failed on both:** SurfaceView destruction preceded onStop, skipping capture after `attached=false`. Fix requests capture before backend detach, with an explicit final-close flag. **S22 fixed-close rerun passes**, creates a new nonblack JPEG with a different SHA (`8a4e8114…`, previous `beff98a1…`); tablet rerun fails earlier because prior AnimeKai source 1571236147208399860 is now uninstalled (package list inspected; no trust/install changes). Thus tablet close fix remains unverified, not passed. Latest all-module run `allmodule4.log`: **1389 tests, 0 failures, 0 errors, 0 skipped**. Temporary helper archived as evidence and removed from androidTest sources. Original scratch data restored byte-equal, new AY9 frames removed, helpers uninstalled, Dot Homes/screenshots inspected, no crashes; **Device lock free**.

Resume (Codex anime lane, 2026-11-21, owner-directed): the existing AY9 source changes remain in `.worktrees/ay9`. Disposable `.worktrees/ay9-fixture-qa` combines those changes with accepted AY0 and uses package `io.github.manojx2005.tsuzuki.ay9final.dev`, per the owner's separate scratch approval. The local 1,000-episode probe and build logs are under `app/build/review-evidence/ay9-fixture/` in that worktree. Use the two debug fixture APKs after checking both package IDs, and claim this shared Device lock before running adb. The synthetic list can establish scroll performance for the fixture. The real One Piece gate still needs an available source. The older RECAP scratch and both prior review packages are outside this run.

Resume result (Codex). Code committed on `tsuzuki/ay9` as `eff92bac9` plus `7ba2fca41`. The missing-source image path now loads the show's cover without extension headers when thumbnail networking is enabled. The actual Coil loader passed that fixture on both devices. Both created a new nonblack 640-wide frame on pause and again on direct close. Incognito preserved the existing bytes. The four Watch screenshots show the captured frame, episode/remaining-time chip and progress. The Colour screenshots exposed a low-contrast fill against the tile, addressed by the second commit with the board's white progress. Tablet landscape shows the hero so tall that the play control is below the first viewport. No later tablet Watch board was found.

Synthetic 1,000-episode scroll in earlier fixture builds, `dumpsys gfxinfo` first summary: tablet Dot 28/1096 (2.55%), Colour 34/1142 (2.98%); S22 Dot 97/632 (15.35%), Colour 93/645 (14.42%). The fixture verifies stable episode tile sizing and actual scroll measurements. It does not settle the live One Piece gate. Debug and all 1,413 unit tests passed for the cover fallback build, with scoped Spotless and clean whitespace checks. Evidence `.worktrees/ay9-fixture-qa/app/build/review-evidence/ay9-fixture/`. The probe's first Watch failures were selector failures: a recursive accessibility-tree lookup and the already observed on-screen coordinates recovered the four cases. The S22 streamed APK transfer failed once; push install passed. Both scratch packages were removed on each device, the fixture port mapping removed and crash buffers empty.

Final contrast rerun (Codex). Debug and the temporary AndroidTest APK built with 1,413 passing Android unit tests. Both devices passed Colour Watch, direct-close capture and missing-source cover. The new white fill is visible in the S22 and tablet Colour screenshots. Final evidence is in `tablet-final-colour/` and `s22-final-colour/` under the fixture folder. Git integration `25e7ff4f2` combines AY0/AY9/AY1 in `.worktrees/v02-anime-chain` without changing the AY1 branch or local `main`.

The integrated Debug build passed all 1,422 Android unit tests and 16 KB APK alignment. On both devices, fixtures 04, 05, 15 and 16 reached playback in Dot, and 04 and 05 reached playback in Colour. The tablet used the Browse-to-player route for all six. On the S22 that route passed Dot 04, then the UI helper could not locate the source or Play for later cases. The remaining cases passed through the production player activity with the same fixture episodes. The first direct Colour 05 attempt reused a checkpoint from Dot and failed its assertion. The fresh-data rerun played at 720p and advanced beyond one second. Both scratch packages were uninstalled and crash buffers were empty. Evidence `.worktrees/v02-anime-chain/app/build/review-evidence/v02-anime-chain/`.

AY9 still needs before review: tablet close test with an available source/fixture; complete Dot/Colour details+Watch visual matrix, explicit missing-source cover fallback, real 1,000-episode scroll jank numbers, §9.0 native lifecycle acceptance and final scoped formatting/audit. Do not mark review prematurely. Existing source changed only on tablet; do not silently reinstall it.
Owner latest steering: **finish existing fixes before the later anime cards**. After safely closing/restoring the current AY9 device run, prioritize the outstanding app fixes Claude recorded (TTS labels/info/value wrapping and recap checks) before AY0; do not merge our own reader/AY9 implementation as its reviewer. Kokoro still needs the owner model/listening decisions. Preserve Claude's current main handoff updates when copying shared status.

Device script (write before adb; observations still **not checked**):
1. Claim lock, verify tablet/S22 hardware, snapshot only animescratch DB/preferences/files. Install AY9 scratch plus a scratch-only test helper if needed; no daily apps. Expected: original source/library state retained.
2. Dot, both devices: play a real episode for ≥20 s, pause/back. Expected: episode row and Watch hero use the same nonblack captured frame, real duration/progress and episode+remaining chip. Repeat after seeking to another scene; expected cache version/image updates.
3. Colour, both devices: repeat step 2; compare 16:9 sizing, radius, chip and progress against boards. Captured frame respects the cover-colour preference; Library stays covers.
4. Unwatched source preview then source without preview: expected source client/headers are used and cover crop fallback; bad images fall back to gradient. Off blocks thumbnail network but keeps captured frames; Wi-Fi only blocks network off Wi-Fi. Unknown duration hides duration/remaining. Incognito creates no frames.
5. Real One Piece ≥1,000 episode list: scroll 10 seconds per device, record gfxinfo frame/jank numbers; inspect stable row sizes and watched/filler metadata. Repeat startup/resume smoke and crash check.
6. Restore scratch snapshots and verify DB/preferences, remove helper, leave Dot Home awake, release lock. Record actual results/screenshots/logs; never claim unrun steps passed.

Lead for the S22 jank (Claude, 2026-11-21, code reading only, not measured; not a review): `AnimePreviewKeyer` keys every request by `anime-preview;animeId;episodeId;frameVersion;…;previewUrl;coverUrl`, so artwork shared by many episodes (the same `previewUrl`, or the cover fallback) is fetched through `AnimePreviewFetcher` and decoded once per episode and kept as separate memory-cache entries. **Correction (measured 23:29–23:40):** on the current AY9 build every row does go to the network: the fixture server logged a steady 1–2 artwork GETs per second during each scroll, one per episode, all for the same `thumb-01.jpg`, paced by the source's request pacing. Worth a check before the live gate: when an episode has no captured frame, key (or request) the shared artwork by its URL so Coil's memory cache serves it, then compare S22 gfxinfo against the 15.35% / 14.42% baseline. **Prototype ready (Claude, not measured):** `trial/ay9-shared-art` `3aae11196` on top of `tsuzuki/ay9` — `cacheKey` drops the episode id when `frameVersion == 0` (shared preview or cover), keeps it for captured frames, and the fetcher reads a captured frame only when `frameVersion > 0` so a shared key never caches one episode's frame; `AnimePreviewPolicyTest` 6/6. A same-session A/B kit is built: `measure-ay9/{base,cand}` = `main` + `tsuzuki/ay9` (`trial/ay9-measure`) without/with the change, scratch package `ay9final.dev`, plus Codex's `AnimePreviewProbe`; run Codex's `serve-ay9.py`, then the seed + `details -e scroll true` modes per build and compare `Janky frames`. **Measured on the S22 (portrait, Dot, Codex's `serve-ay9.py` + probe `seed` then `details -e scroll true`, two alternating rounds each):** baseline (`main` + AY9) 20.60% / 21.07% janky, 500 / 541 frames, 90th pct 28–29 ms; candidate (+ shared-artwork key) 68.16% / 67.04% janky, 179 / 179 frames, 90th pct 34 ms. **The numbers don't compare like for like.** After scrolling, the baseline's thumbnails were all still empty gradient placeholders, because each row waits for its own paced request; the candidate's rows all show artwork, from about 3 requests per run. So (1) the shared key fixes thumbnails not loading in long lists where many episodes share artwork or fall back to the cover (shots `measure-ay9/s22-shots/{base,cand}-scrolled.png`, server log `measure-ay9/s22/fixture-ay9-measure.log`); (2) once artwork is actually drawn, the S22 portrait list is slow (about 14 fps over 12.8 s of flings). **Bisect, same session:** a variant of the candidate that draws no image at all: 127 (69.0%) and 118 (68.6%) janky of 184 / 172 frames, the same as the candidate's 120 / 122 of 179 / 178. So drawing artwork is not the cost. Look at absolute janky frames, not percentages: all variants drop about 105–125 frames per ten flings. The baseline's 21% only looks better because its paced thumbnail loads add about 320 cheap extra frames per run. The shared-artwork key is jank-neutral and makes the thumbnails appear. The remaining S22 stutter is UI-thread row composition during flings (gfxinfo: most janky frames are "Slow UI thread"). **All these numbers, Codex's included, come from unminified debug builds**, which exaggerate Compose cost. The fixture source exists only in debug, so the meaningful number is the live 1,000-episode check on an optimized Preview build. Small per-row waste seen in passing: `AnimeEpisodeTile` calls `DateFormat.getDateInstance()` for every row on every recomposition. Not a review; for the anime lane. **Optimized build (Claude, 2026-11-22):** a temporary probe in the Preview test APK (`AnimeListPreviewProbe` with the fixture source copied into androidTest and test-only keep rules, `trial/ay9-measure2`, scratch `ay9prevqa`, candidate with the shared-artwork key; nothing committed) ran the same ten flings over the 1,000-episode list on the S22: 125 (56.6%) and 131 (61.5%) janky of 221 / 213 frames, 90th percentile 18–19 ms (debug 31–34 ms), 113 / 123 "Slow UI thread"; thumbnails loaded in every row (`measure-ay9/s22-preview/scrolled-1.png`). So R8 halves the frame time, but on the S22 each move frame that composes new rows still misses its deadline (a 120 Hz display gives an 8.3 ms budget). For comparison, Codex's tablet run was about 3%. **Resolved (Claude, 2026-11-22): the S22 "jank" is a measurement artifact of the probe's synthetic flings, not the list.** Framestats on the optimized build put slow frames' time mostly in render thread + GPU (about 6 ms median), not Compose (about 1.4 ms), and not the Dot grid (Dot 46% vs Colour 55–56% janky with the same probe). With ordinary `adb shell input swipe 540 1900 540 800 120` flings (10, 0.7 s apart) on the same optimized build and the same 1,000-episode list (the probe held open with `-e holdSeconds 90`): **10 janky of 1,197 frames (0.84%), 90th percentile 11 ms**. Samsung Settings with the same flings: 11 of 1,206 (0.91%), 7 ms. The probe injects each fling as moves 35 ms apart, which both gfxinfo and the input-latency counters treat as late frames; every S22 percentage above (Codex's 15% and the 46–69% runs) carries that artifact. Use real `input swipe` flings for the live gate. Evidence `measure-ay9/s22-theme/`, `measure-ay9/s22-preview/`. Claude also trial-merged AY1 + AY9 (`trial/ay1-ay9`, see the AY1 card).

Shared-artwork prototype source review (Codex Batch T). The URL fields are joined with a semicolon. The two distinct pairs `("https://art.example/a;https://art.example/b", "https://art.example/c")` and `("https://art.example/a", "https://art.example/b;https://art.example/c")` produced the same cache key under `3aae11196` with the same source and policy. Preserve field boundaries in the shared key so different artwork cannot reuse the same image. The reported portrait measurements leave render performance open.

#### TTS-POLISH Finish the app defects from Claude's device review (T2) — `review passed, not merged` (Claude independent review 2026-11-19; waits for the app chain `d9b94f259` it is based on) (owner priority 2026-11-15; `.worktrees/tts-polish`, `tsuzuki/tts-polish`, base `review/app-chain` d9b94f259)
Pulled forward from AY-INT item 8 per the owner's request to finish existing fixes first. Fix untranslated TTS/settings/list/source labels in all 8 locales, the TTS info icon's separate line, and truncated language/voice values on phones. Reuse Settings/TTS boards and typography. No player/source/stream work. Existing recap/reader/dictionary fixes remain on review/app-chain for independent review; do not reimplement or self-merge. This card is app implementation: no device self-review. After this, independently review Claude's READER-NEXT-GUARD (this chat did not implement it), then resume AY9 acceptance and the authorized anime queue. Kokoro model/listening gates remain with the owner.
Result (Codex 2026-11-15): **ready for independent review**, implementation **`1da547881`**. `TsuzukiSettingsPane.kt` measures the localized title/value in the actual typography and stacks long values below the title when they do not fit, with full wrapping; short values stay inline. New `TsuzukiPreferenceInfo.kt` puts the icon beside the note; `InfoWidget.kt` has a thin Tsuzuki-only dispatch. `SearchableSettings.kt`, History/Updates grouping actions and `NovelSourcesTab.kt` use localized reset/list/source titles. All 13 missing TTS labels translated in seven non-base locales; six reset/grouping labels added across eight locales. `AGENTS.md` records touched upstream hooks. Standard themes keep their original info layout.

Validation: Debug, **1,420 all-module tests (1,253 app), zero failures/errors/skips**, two scoped Spotless passes, diff check; 176 localized-key checks and no increase to pre-existing unrelated duplicate counts. AAPT resource dump from the actual APK confirms Japanese filter, automatic voice and reset labels. ARM64 Debug SHA-256 `773c12a30ce7e7173d6a232e29a901b8431b5678772ae786fc2ff301ae356883`. Evidence `app/build/review-evidence/tts-polish/` (`final-audit.json`, build-test.log, locale-audit.json, apk-resources.txt). No new dependencies. No device use by this app implementer; **not self-merged**.

Reviewer script: use an authorized scratch package and saved data, Dot then Colour on tablet/S22; open the top-level TTS page and reader Listen settings sheet. Japanese labels must be localized; info icon sits at the start of its text; language and automatic/long installed voice values show fully on the phone and at increased font scale; short values remain inline when they fit. Open language/voice dialogs and cancel without changing choices; confirm reset labels/dialog and Updates/History/Custom Sources action labels. Check stock theme info layout if applicable, screenshot and crash log, restore scratch state. Coordinate with Claude's app-chain review; this card branches from its `d9b94f259` and needs that chain. The TTS portion of AY-INT item 8 is now covered here; remaining anime integration work stays on AY-INT.

Independent review (Claude 2026-11-19): **pass on the tablet; phone width not verified.** Code read: `TsuzukiValuePreferenceRow` measures title and value in their real styles (font scale included via density) and stacks only when they don't fit; upstream hooks stay one-line swaps; Japanese strings read well. Nits, not blocking: fully-qualified `TsuzukiTheme`/`LocalDensity` names inline in `InfoWidget.kt`/`TsuzukiSettingsPane.kt`. Device (tablet R52Y90HA77L, scratch `rv5debugscratch` built from `d4878ac49`, ARM64 Debug, font scale 1.0, Colour then Dot): Settings list shows 読み上げ; TTS page all Japanese (端末の音声 / エンジンとインストール済みの音声 / 言語で音声を絞り込む / 音声サンプルを再生 / 再生 / 読み上げ表示), info icon inline at the start of its note, 「すべての言語」 and 「自動 · 章の言語に合わせる」 shown in full; reset icon → 設定をリセット / この画面の設定をすべて既定値に戻しますか？ / リセット / キャンセル, cancelled; language dialog opened and cancelled, choice unchanged; Updates and History action リスト表示, Browse カスタムソース; reader Listen sheet (Dot) same labels, icon inline, values in full. **Not verified:** the stacked layout itself (every value fit inline at tablet width, in the two-pane detail and the Listen sheet), S22/phone width and large font scale (S22 not on adb: wireless connect refused). Scratch data restored byte-equal (9 files), Colour Home, crash buffer empty. Evidence `app/build/review-evidence/tts-polish-claude/` (root checkout). **Not merged:** it sits on `review/app-chain` (Claude's own unreviewed recap/reader/dictionary chain), so it merges with that chain after the chain's independent review.

#### MARK-UNTIL-LAST-READ Library "Mark until last read" (T2) — `done` (merged with MARK-UNTIL-LAST-READ-F at `7e333ca2a`)

Review (Codex Batch T, 2026-11-21): Changes requested. `MarkUntilLastRead.kt:49` discards `SetReadStatus.Result`. `SetReadStatus.kt` returns `InternalError` if the database update fails, but the caller still reports those chapters as marked and increments the cached read count. The trial merge was aborted. This is a source finding.

Owner 2026-11-20: "add a mark until last read button in the library". Library selection bar › More (その他) gets **最後に読んだ所まで既読 / Mark until last read** as its first item (Tsuzuki themes; both Library and Novels tabs). For each selected book it marks every **unread chapter before the furthest chapter reached** as read: the anchor is the later (in reading order, `getChapterSort(manga, ascending)`) of the most recent history chapter and the last chapter that is read or has saved progress; the anchor itself is left alone so an in-progress chapter keeps its place. Undo snackbar first (same pattern as Mark read), then "N chapters marked as read" (plural) or "No unread chapters before your last read". Library unread counts update in place. Code: new `tsuzuki/library/MarkUntilLastRead.kt` (pure `chaptersBeforeLastRead` + runner), one VM function in `LibraryViewModel.kt`, one parameter in `MangaBottomActionMenu.kt` / `TsuzukiSelectionActions.kt`, one line each in `LibraryTab.kt` / `NovelsTab.kt` (all already-touched upstream files); 3 strings + 1 plural in 8 locales. Tests: `MarkUntilLastReadTest` (4) pass; full app suite on the branch **1,221 tests, 0 failures/errors/skips**. No board: reuses the existing selection More menu and the `ic_done_prev_24dp` icon from "Mark previous as read". Devices (scratch only, restored byte-equal): tablet Colour — RV2 with chapter 1 unread and chapter 2 last read → undo snackbar → "1件のチャプターを既読にしました", unread badge 2→1, DB shows chapter 1 read and chapter 2 untouched; S22 Dot (wireless) — menu item on one line, a book read in order → "最後に読んだチャプターより前に未読はありません"; crash buffers empty. Reviewer: run the full suite, check a manga (page-based progress) and a book sorted by source order, and Undo.

#### MARK-UNTIL-LAST-READ-F Report failed chapter updates accurately (T2) — `done` (Claude's `2448a3a33` passed independent review and merged locally at `7e333ca2a`)

Use the result from `SetReadStatus` when updating library counts and building the completion message. Count and update only successful writes. Surface a localized failure when one or more selected books fail, including partial success across books. Cover a failed write and a mixed success/failure batch. Keep the anchor, source-order and Undo checks from the original card.

Independent review passed. Debug and optimized Preview built from the integrated branch. All 1,412 Android unit tests passed, including the failure and mixed-result cases. The tablet and S22 scratch probe ran the actual action with a manga whose source order differed from its chapter numbers. Undo left the database unchanged. Dismissing the second Undo marked the one earlier chapter, kept page seven on the anchor and left the later source-order chapter unread. The probe exercised the production snackbar and repository programmatically. It did not repeat Claude's visual menu check. Scratch packages removed and crash buffers empty. Evidence `.worktrees/review-v02-mark-f/app/build/review-evidence/mark-f/`.

Result (Claude, 2026-11-21): `MarkUntilLastRead.await` now returns `MarkUntilLastReadOutcome(marked, failedBooks)` and checks each `SetReadStatus.Result`: `Success` counts, `NoChapters` counts zero (already read), `InternalError` counts the book as failed and the batch goes on. Library counts update only for `marked`. Snackbar: all ok → the existing plural; some failed → `tsz_mark_until_last_read_partial` ("2 chapters marked as read · Couldn't mark chapters in 1 book"); all failed → `tsz_mark_until_last_read_failed` plural; 1 string + 1 plural in all 8 locales. The lookups are constructor lambdas so the batch is unit-tested without mocks: 6/6 `MarkUntilLastReadTest` (anchor/source-order tests kept, plus a failed write and a mixed success/failure/already-read batch). Undo is unchanged (offered before any write). **Tablet** (temporary instrumentation probe, not committed; scratch `markfqa` = this branch, real DB and `SetReadStatus`): a library novel with chapters 1–5 and chapter 3 read → outcome `marked={book=2}, failedBooks=0`, chapters 1–2 now read and 4–5 untouched, message "2 件のチャプターを既読にしました"; a second run → nothing marked, "最後に読んだチャプターより前に未読はありません"; crash buffer empty; scratch and probe uninstalled. A failed DB write can't be produced on a device; that path is covered by the unit tests.

#### AY-INT-F Keep the anime section reachable in Library › All (T2) — `done` (merged 2026-11-22 with AY-INT-F2)




Keep the owner's first-page anime section and separate Anime pill. Use the eligible anime rows when deciding whether the first All page is empty. Filter those rows by title during All search and show an anime match even when the current book shelf has zero results. Cover an anime-only library, an empty first shelf with books elsewhere, an anime-only search hit, and the anime-off path. Return the whole AY-INT branch to independent review after the fix.

Independent review (Codex Batch T). The Library fix addresses the earlier empty-shelf path. The whole branch still drops watch history in History › All when the book query has no rows, including an anime-only history and a search matching only an anime title. `HistoryScreen.kt` checks the unmerged `state.list.isEmpty()` before calling `mergeAnimeIntoHistory`; the correct empty-state check is against the merged list. This is a source finding. Verify both zero-book cases and an anime-off case before merging the whole branch.

#### AY-INT-F2 Show watch history when the book query has no matches (T2) — `done` (merged locally 2026-11-22 with AY-INT and AY-INT-F, `c7ea7da00`)

Review (Claude, 2026-11-22; Codex wrote F2, so this is independent): **passed and merged.** `HistoryScreen.kt` now chooses rows vs the empty state from the merged list; correct. Review branch `review/ay-int-main` = `main` `7e333ca2a` + `tsuzuki/ay-int-f2` (AY-INT `641018f95`+`b0fdce1eb`, AY-INT-F `ed304d86a`, F2 `7f9d4805c`); only append-only conflicts (8 `strings.xml` tails + AGENTS.md, both kept; no new duplicate keys). Debug built, **all-module tests 1,442 / 0 failures**, `verifySqlDelightMigration` passed. Tablet (scratch `…clauderevscratch`, Codex's `AnimeHistoryReviewProbe` with only the package changed): Dot and Colour History › All with an anime-only history, the title hit and a search miss, and anime off — all **PASS**; screenshots inspected (hit shows the row under 今日, miss shows 結果が見つかりませんでした); crash buffer empty; scratch uninstalled. S22 was not connected (Codex's F2 run covered it). Evidence `.worktrees/review-ay-int/app/build/review-evidence/ay-int-merge/`.



Use the combined list to choose between History › All rows and its empty state. Keep the existing History board layout and anime gate. Independent review after implementation.

`HistoryScreen.kt` now decides whether to show the empty state after merging the watch and book rows. The new `TsuzukiMixedHistoryTest` case covers an anime-only search hit and a miss. The app compiled and ten focused History and Library tests passed. The tablet and S22 passed the actual History › All screen with an anime-only history, an anime-only title match and a search miss in Dot and Colour. Disabling anime hid the saved row on both devices. Scratch packages removed and crash buffers empty. Evidence `.worktrees/ay-int-history/app/build/review-evidence/ay-int-f2/`. The card still needs independent review.

Result (Claude, 2026-11-21): the All-page anime lookup moved from `TsuzukiMixedLibraryGrid` into `LibraryPager` (page 0 of a mixed library only), so the empty-shelf check counts the anime and an empty first shelf still renders the mixed grid with the section; `rememberAllPageAnime(searchQuery)` owns the All-pill check and filters by title (case-insensitive, trimmed); `NovelsTab` skips the empty-library screen when anime are present, and `allPageShelves()` gives an anime-only library (no visible shelf) the default shelf back. Tests 4/4 `TsuzukiAllPageAnimeTest` (+ search and shelf fallback) and 6/6 `TsuzukiMixedLibraryTest`. **Tablet Dot**, scratch `…ayintfqa` = `main` `e9d8bb9eb` + this branch, AY0 fixture: anime-only library shows the Anime section on All (`03`); committed search `PLAIN` shows the anime with zero books (`04`); `frieren` shows "no results" (`05`); anime off → stock empty-library screen, no Anime pill (`06`); crash buffer empty. **S22 Dot** (phone layout, same scratch build): anime-only library shows the Anime section under the All pill; `PLAIN` shows the anime with zero books; `frieren` shows no results; crash buffer empty; scratch uninstalled. Library search filters only on Enter (`commitSearch`). Not exercised on device: an empty first shelf with books on another shelf (same `items.isEmpty() && allPageAnime.isEmpty()` check). Evidence `.worktrees/ayintf-qa/app/build/review-evidence/ayintf/`; the merge on `qa/ayintf` is throwaway (append-only conflicts in the `strings.xml` tails and AGENTS.md, both kept). Scratch app uninstalled, port mapping removed.

#### AY-INT Anime everywhere the app shows books (T3) — `done` (merged 2026-11-22 with AY-INT-F and AY-INT-F2, `c7ea7da00`)




Review (Codex Batch T, 2026-11-21): **changes needed; excluded from the candidate.** `NovelsTab.kt:280` returns the book-only empty screen when the user owns anime but no books. Even with books elsewhere, `LibraryPager.kt:73` returns before the mixed grid on an empty first book shelf, where this card places the anime section. `TsuzukiMixedLibraryGrid.kt:85` suppresses the whole section for a nonempty query, so a Library › All search cannot match an anime title. These are source-path findings; runtime reproduction is pending.
Owner 2026-11-15: "a lot of tabs where anime is not implemented properly, like Settings or Updates and many other tabs", and **"the All tab in the Library doesn't show the anime"**. Claude's audit (code on `main` `304bd9aff` + tablet with anime on, 2026-11-15): anime was added as a **separate pill/screen on each surface** and never joins the shared views. Branch `tsuzuki/ay-int` from `tsuzuki/ay9` (or main if AY9 is not committed yet; say which). Boards: `10-anime/*` and the existing Library/Updates/History/Settings boards (Dot then Colour); where a board shows only books, extend the existing list with the same row/tile, don't invent a layout; anything with no board fit → `blocked: needs a board`.

Fix list (each: what's wrong now → expected):
1. **Library › All** (`ui/tsuzuki/library/TsuzukiLibraryMediaSwitch.kt`, `NovelsTab.kt`, `presentation/tsuzuki/anime/TsuzukiAnimeLibrary.kt`): All = novels + manga only; anime only under its own pill → All also lists anime entries (same grid tile with the anime badge, EP progress as on the AnimeLibrary board), sorted/filtered with the same All sort; the header count says books and anime separately ("2 books · 1 anime"). Search in All finds anime titles. Long-press/select on an anime tile opens the anime actions, not book actions.
2. **Updates › All** (`presentation/tsuzuki/updates/AnimeUpdatesSlots.kt`, `UpdatesScreen.kt`): All shows chapters only; the Anime pill appears only when anime favourites exist → All merges new episodes by date with chapters (episode rows from `TsuzukiAnimeUpdates`); "Update library" also runs `AnimeLibraryUpdateJob`; the last-update line counts both. Keep the Anime pill.
3. **History › All** (`presentation/history/HistoryScreen.kt` hook, `presentation/tsuzuki/history/TsuzukiAnimeHistory.kt`): All excludes anime → merged by last-watched/read time; delete/clear-all applies to anime rows too (confirm dialog says so).
4. **Settings**: only "Anime player" (main list) and "Anime shelves" (Library) exist, and the main list does not refresh when anime is switched on (`TsuzukiAnime.enabled` is read as a plain getter in `SettingsMainScreen.kt`; observe the gate state instead). Add, behind the gate: **Downloads** — anime download folder/quality/Wi-Fi-only/delete-after-watched rows (reuse the existing anime download prefs; list which exist); **Library** — anime update interval/restrictions if `AnimeLibraryUpdateJob` has them, else include anime in the existing library-update settings and say so; **Tracking** — show which trackers support anime (MAL/AniList/Kitsu) and anime sync options; **Notifications** — anime episode notifications toggle if a channel exists. Settings search finds the anime rows.
5. **More**: "Downloaded only" is not applied to the anime library → filter anime to downloaded episodes too; "Categories" edits only book shelves → show anime shelves in the same screen (section) or link to `AnimeShelvesScreen`; download queue already shows anime — check its header counts both.
6. **Global search** (rail search button / Browse global search): check whether anime sources are searched from the rail search; if not, add the anime results section that `presentation/tsuzuki/browse/TsuzukiAnimeGlobalSearch.kt` provides, behind the gate.
7. **Home** Read/Watch split is by design; check the Watch side's stat tiles use real anime data only (no book numbers).
8. Every new string in all 8 locales. While here, translate these English-only strings seen on the Japanese tablet: Settings "Text-to-speech", "Reset to defaults", Updates "List View", Browse "Custom Sources", and the TTS sheet ("Engine and installed voices", "Filter voices by language", "Play voice sample", "PHONE VOICE"/"PLAYBACK"/"READ-ALONG", the engine-change note, "Automatic · match the chapter language"). The TTS sheet's info icon sits alone on a line above its text → put it inline (Settings board). On phones the right-hand values of "Filter voices by language" and the voice row are truncated → let them wrap or stack under the title.

Progress (Claude 2026-11-19, uncommitted in `.worktrees/ay-int`): **item 4, main Settings list refresh — done.** `SettingsMainScreen` built its item list once (`private val items`), so the 「プレーヤー」 (anime player) row was frozen at screen creation; now the row is always in the list with `isAnimeOnly = true` and filtered in composition against the snapshot-state gate. Tablet: toggling Settings › ブラウズ › アニメ（ベータ版） off/on hides/shows the row live, no restart, no crash; data restored. **Item 4, search — done:** settings search indexes the anime player cards (AniSkip, autoplay, speed, subtitles, audio, video, gestures; new `AnimeSettingsSearch.kt`, one `getIndex` entry while anime is on); searching "AniSkip" shows 「AniSkip — プレーヤー」 and opens the player page. That exposed a **crash**: `AnimePlayerSettingsScreen` used `LocalBackPress.currentOrThrow`, and the search pane provides none — now nullable like `SearchableSettings` (back arrow only when a handler exists). Tablet: no crash after the fix (`v8-claude/ayint-search-open.png`); data restored. **Item 5, Downloaded only — done (code + tests, not seen on a device):** the anime library now also filters by More › ダウンロード済みのみ, keeping anime with at least one `Saved` episode download (`AnimeDownloadQueue.savedAnimeIds()`, new `AnimeSavedIdsTest`, 2 tests + 9 existing anime download tests pass). **Item 5, queue header — done (code, compiles; not seen on a device):** the Queued tile summed only novels + manga (`counts.take(2)`); it now adds anime episodes still `Queued` (not `Saved` ones, which the anime pill count includes) via a new `animeQueued` parameter on `TsuzukiDownloadQueueHeader`, passed from `DownloadQueueScreen.kt` (already-touched upstream file). **Item 5, Categories — done:** More › カテゴリー's overflow menu shows 「アニメの本棚」 while anime is on and opens `AnimeShelvesScreen` (new `TsuzukiAnimeShelvesAction`, one `actions` slot in `CategoryScreen.kt`); tablet: opens the anime shelves, no crash (`v8-claude/ayint-shelves.png`), data restored. **Item 5 is complete.** **Items 6 and 7 — already satisfied (checked 2026-11-19, no change needed):** (6) on the tablet the rail search is the Library search (すべて/小説/マンガ/アニメ pills) and its 「グローバルで…を探す」 opens global search, whose 小説/アニメ media pill comes from `rememberAnimeGlobalSearch` and is shown whenever anime is on and at least one anime source exists (`showAnimeGlobalSearch`); the scratch app has none, so the pill is correctly absent — confirm with an anime source. (7) Watch stats read `AnimeHistoryRepository.subscribeWatchStats` (real data); the Watch home shows only the real library card when empty. **Item 4, other settings — done/settled:** Tracking gets the anime 「エピソードごとに更新 — アニメ」 switch (the anime tracking sheet's own preference, new `AnimeTrackingPreference.kt`, one spread line in `SettingsTrackingScreen.kt`; tablet verified). Library: `AnimeLibraryUpdateJob` already reads the shared library-update interval/restrictions/throttle/metadata preferences, so Settings › Library already applies to anime (no rows added). Downloads: no anime download preferences exist (Wi-Fi only lives in the anime queue's own state and is toggled on the queue screen) — nothing to add without new preferences. Notifications: the anime update job posts no notifications, so there is no channel to expose. **Item 4 is complete.** Full app suite on the worktree: **1,219 tests, 0 failures/errors/skips**. **Items 2–3, merge logic (Claude 2026-11-20):** new `presentation/tsuzuki/anime/TsuzukiMixedTimeline.kt` — `mergeNewestFirst` (books + anime by time, books win ties, no anime = the book list unchanged) and `groupByDay` (local day, order kept); `TsuzukiMixedTimelineTest` 5 pass. Not wired into History/Updates yet: that needs anime rows on a device, which AY0's fixture source will give. **Item 2, Updates › All, per-chapter list — done (Claude 2026-11-20):** `UpdatesUiModel.Anime` (internal constructor; upstream `UpdatesScreen.kt` + key/contentType/render branches in `UpdatesUiItem.kt` and the tablet `TsuzukiUpdatesTwoPane.kt`); new `TsuzukiMixedUpdates.kt` `mergeAnimeIntoUpdates` (chapters by `dateFetch` + episodes by timestamp, headers rebuilt; `TsuzukiMixedUpdatesTest` 3 pass); `AnimeUpdatesSlots.newEpisodes` (favourites' new episodes, last 3 months) with `withAnimeUpdates` / `hasAnimeInAll` (All only; the empty state no longer hides episodes); the episode row and its play action are now `TsuzukiAnimeUpdateEntry`, shared with the Anime pill. **Tapping an episode plays it** (same as the Anime pill; owner asked: player or details — not yet answered). Tablet (combined trial build, fixture case 01 favourited, server restarted with the new `--new-episodes` flag, details refreshed): per-chapter 新着 › すべて shows 「今日 · 01 plain mp4 · Episode 3」, tap plays from the fixture; no crash (`review-evidence/ay-int/updates-all-*.png`); data restored. App suite **1,239, 0 failures**. Note: the anime library update job skipped the fixture anime (completed series with unwatched episodes, under the library update restrictions); refreshing the details page fetched it. **Item 2, grouped mode (the default) — done (Claude 2026-11-20):** in grouped 新着 › すべて each anime with new episodes is one group row among the book groups, ordered by latest time (`animeUpdateGroups` + `mergeUpdateGroups` in `TsuzukiMixedUpdates.kt`, 2 more tests, 5 total). Phone: `UpdatesScreen.kt` calls `tsuzukiUpdateGroups` (falls back to the stock `updatesNovelGroups` when there are no anime groups; anime rows use `TsuzukiAnimeUpdateGroupEntry`, no play icon). Tablet: `TsuzukiUpdatesTwoPane` renders anime groups with the same `UpdateRow` as books. **Tapping an anime group opens its details** (`openAnime`) — no right-pane preview for episodes (not on the board). Tablet (combined trial build, fixture case 01 favourited + `--new-episodes`): grouped すべて shows 「01 plain mp4 · 新着エピソード 1 件 · Episode 3 · 新着」; tap opens the anime (ライブラリ内, 3 episodes); no crash (`review-evidence/ay-int/updates-grouped-*.png`); data restored. App suite **1,241, 0 failures**; scoped Spotless clean. **Save all with episodes — done (Claude 2026-11-20):** in All the Updates tiles count new episodes too (新着 = unread chapters + new episodes; すべて保存 = unsaved chapters + episodes not in the anime download queue, `episodesToSave`, 1 more test) and Save all also queues those episodes, one `AnimeDownloads.enqueue` per anime with its source and title (`AnimeUpdatesSlots.saveEpisodes`; both phone `UpdatesScreen` and tablet two-pane; tiles now also show when All has only episodes). Tablet (combined trial build): tiles 「新着 1 話」「すべて保存 · 未読 1 話をダウンロード」 → tap → the episode downloads from the fixture (`/api/videos/01.json`, then `/media/ok.mp4` 200) and the tile turns 「保存済み」; no crash (`review-evidence/ay-int/updates-save-all-*.png`, `server-save-all.log`). App suite **1,242, 0 failures**. **Finding for AY1/AY7 (not this card):** the episode **download** request went out with a generic browser User-Agent and **no Referer** — the source's headers (`TsuzukiFixture/1`, Referer) were not used, unlike playback — so fixture cases 06/07 (and real hosts that check headers) will play but fail to download. **Item 2, "Update library" — done (Claude 2026-11-20):** `UpdatesViewModel.updateLibrary()` (already-touched upstream file, one line) also calls `AnimeLibraryUpdateJob.start`, which is a no-op while anime is off; the book job's result still drives the message. Tablet (anime on): one tap on 新着's refresh enqueued `LibraryUpdate-manual` and `tsuzuki-anime-library-manual` 14 ms apart, both SUCCEEDED (read from WorkManager's `workdb`); no crash; data restored. **Last-update line — done (code + tests, not seen on a device):** `AnimeLibraryUpdateJob` stamps the shared `lastUpdatedTimestamp` when a run has anime to update (as the book job does at its start), so 前回のライブラリ更新 shows the latest of either; it is display-only (its only reader is `UpdatesViewModel`). Needs anime in the library to see (AY0 fixture). Full app suite on `.worktrees/ay-int`: **1,230 tests, 0 failures/errors/skips**. **Item 3, History › All — done (Claude 2026-11-20):** `HistoryUiModel` gains one `Anime` case (upstream `HistoryScreen.kt`: the sealed model + key/contentType/render branches, and `history = mergeAnimeIntoHistory(...)` when anime history exists and the filter is All). New `presentation/tsuzuki/history/TsuzukiMixedHistory.kt` `mergeAnimeIntoHistory` (books + anime by time via `mergeNewestFirst`, day headers rebuilt over both, anime follow the search, and **while more book pages may follow, anime older than the oldest loaded book wait** so paging never puts old anime above unloaded newer books); `TsuzukiMixedHistoryTest` 5 pass. The anime row and its resume/remove dialog moved into `TsuzukiAnimeHistoryEntry` (shared by the Anime pill and All; a failed action now shows a toast instead of the list-level error line). **Tablet (combined throwaway build `.worktrees/trial-anime` = AY-INT + AY0, scratch, Colour):** after watching fixture case 01, 履歴 › すべて shows 「今日 · 01 plain mp4 · 第1話 · 残り00:43」 above the book entries under 「5 日前」; tapping it resumes the episode at ~17 s (clock 26.8 s ten seconds later); no crash (`review-evidence/ay-int/history-all-*.png`); data restored. Book multi-select stays books-only. **Item 3, clear-all — done (Claude 2026-11-20):** History's 「履歴を消去」 also clears anime watch history while anime is on: new `anime_history.sq` queries `clearAllHistory` (sets `last_seen` NULL, keeping watch time for stats, like the per-anime remove) + `refreshAllAnimeLastSeen`; `AnimeHistoryRepository.removeAllHistory()`; `HistoryTab.kt` (already-touched upstream) calls `clearAllAnimeHistory()` after the book clear; `HistoryDialogs.kt` `HistoryDeleteAllDialog` gains an optional `note` (upstream, one parameter) showing 「アニメの視聴履歴も削除されます。」 (new `tsz_history_clear_includes_anime`, 8 locales) only while anime is on. Test `clearingAllHistoryEmptiesTheListButKeepsWatchTime` (AnimeDatabaseTest 27 pass); app suite **1,235**. Tablet (combined trial build): dialog shows the note; OK empties both books and anime, the アニメ pill disappears, 「履歴が削除されました」; no crash (`review-evidence/ay-int/history-clear-all-*.png`); data restored. **Phone (S22) run of History/Updates All — stopped, not checked (Claude 2026-11-20):** the S22 sat in landscape; after playback, back presses left the app into the system's 開発者向けオプション screen that was open behind it (one scroll gesture landed there — nothing tapped or toggled), and text matching then opened the lib16 source instead. Stopped to avoid touching system screens; S22 data restored byte-equal. The phone code path (`UpdatesScreen` grouped/per-chapter + tiles, `HistoryScreen`) is compiled and unit-tested but **not seen on a phone**; next time hold the S22 in portrait and avoid extra back presses. **Dot (tablet, 2026-11-20):** grouped 新着 › すべて shows 「01 PLAIN MP4 · 新着エピソード 1 件 · EPISODE 3」 with the red 新着 badge and the tiles count the episode; 履歴 › すべて shows the anime row under 今日 above the books; no crash (`review-evidence/ay-int/dot-*.png`). Noticed (existing code, not changed here): in Dot the History anime thumbnail is grayscale (`AnimeHistoryRow` applies `DotCoverFilter` whenever Dot is on), while the owner rule (2026-11-15) keeps covers in colour by default unless the "Colour covers" preference is off — the Updates rows already keep colour. **Fixed (same day):** the four anime thumbnails (History row, Updates row, Watch library cover, anime global search) now use the shared `tsuzukiCoverFilter()` instead of `DotCoverFilter.takeIf { isDot }`, so Dot keeps their colours unless Colour covers is off (`TsuzukiAnimeLibrary.kt` is also changed by AY9 — one-line overlap). Not changed, on purpose: `TsuzukiTrackInfoDialogHome.kt` uses the same filter for **tracker logos** (not covers), which fits Dot's red-only accent rule. **Item 1, Library › All tiles — needs an owner decision (Claude 2026-11-20):** the place to add them is Tsuzuki's own `presentation/tsuzuki/library/TsuzukiMixedLibraryGrid.kt` (`LibraryPager` already dispatches All there), with `mergeMixedLibrary` for the order and the AnimeLibrary board's tile (cover, "Ep n / total", progress line, unseen badge). **Open question:** the book Library pages by **book shelf** (system/default shelf + user shelves, with pills), while anime live on separate **anime shelves** (`AnimeShelvesScreen`; the board's Watching / Plan to watch / Completed pills). No board shows anime inside the book Library (`10-anime/AnimeLibrary` has Novels / Manga / Anime segments only). Options: (a) anime appear in All on every book-shelf page (duplicated across shelves); (b) only on the first/default shelf page; (c) All gets one extra "Anime" shelf pill/page holding all anime; (d) All shows a single unpaged list when anime exist. Also: book paging (`paginationEnabled`) needs the same "hold older anime until the pages reach them" rule as History. **Item 1, merge logic (Claude 2026-11-20):** new `TsuzukiMixedLibrary.kt` — `mergeMixedLibrary` folds anime into the already-sorted book list without moving any book (same All sort key and direction; books win ties); sorts anime can't follow yet (Random, tracker mean, downloaded, source name) put anime after the books by title; `TsuzukiMixedLibraryTest` 6 pass. Scoped Spotless clean. Item 1's header count ("2 books · 1 anime") waits for the anime tiles so the line never counts anime the grid doesn't show. Remaining: items 1–3 (Library/Updates/History All merges; need anime in a scratch library to test); item 8 done by TTS-POLISH. Items 1–3 need anime in a scratch library to test (One Piece is only in `animescratch`, which is held by the stalled RECAP review).
Tests: All-list merge/sort with books + anime; updates/history merge by time; downloaded-only filter for anime; gate off → every surface identical to today (snapshot of the item lists).
Device script (scratch `animescratch` with One Piece in the anime library + the two RV books; tablet and S22; Dot then Colour): (1) Library All shows books and One Piece, counts right; Anime pill still works; (2) Updates All shows episodes and chapters by date; (3) History All shows the watched episode between book entries; (4) Settings shows the anime rows right after switching anime on, without restart; search finds them; (5) Downloaded-only hides undownloaded anime; (6) rail search finds One Piece; (7) anime off → no anime anywhere, lists as before; (8) Japanese device: no English strings on the screens above. Screenshots per step; crash buffer empty.
Result: _pending_

Early read (Claude, 2026-11-15, uncommitted AY9 worktree, not a review): `capturePreview()` uses mpv's `async screenshot-to-file` (off the caller thread, good); `AnimeFrameStore.accept` waits for the PNG IEND marker (≤ 5 s), downsamples, rejects dark frames, writes atomically under a lock with generation/sequence checks, caps at 100 MB. Notes for Codex: (1) `accept` lets `check()` failures escape (only `prune`/init use `guarded`) — make sure every caller catches and logs, or wrap it; (2) confirm `close()`/`MpvPlayer.destroy()` waits for or cancels a pending async screenshot (if mpv is still writing after the 5 s wait, the raw file is deleted under it and recreated — the 24 h stale sweep catches it, but say so); (3) fully-qualified `java.util.concurrent.*` names inline — import them; (4) `publish()` walks the whole store after every capture — fine at the cap, note it.

#### AY0 Anime fixture server + debug fixture source (T3) — `done` (merged locally 2026-11-21 at `e9d8bb9eb`)
Review (Codex Batch T, 2026-11-21): Passed and merged locally. Debug and optimized Preview built, 1,406 Android tests and nine fixture-server tests passed. Both fixtures appeared in the Debug catalogue. Case 01 played on tablet Dot (2.958 to 9.083 s) and S22 Colour (2.375 to 8.333 s), with actual HTTP 206 responses using the source headers. The optimized Preview DEX audit found none of the fixture class, source name or loopback endpoint. The S22 optimized Preview opened with anime off. Crash buffers empty. Restored anime-off and Dot on the scratch apps and removed the temporary port mappings. Review commit `e9d8bb9eb` scopes the README cleanup to that port. Evidence `.worktrees/review-v02-main/app/build/review-evidence/v02-ay0/`.

**Detailed plan: PARITY §9.1 (Claude 2026-11-15; replaces §5 AY0 where they differ).** PARITY §5 AY0. `scripts/anime-fixture/` (stdlib Python server on 127.0.0.1:8765, media made with ffmpeg,
git-ignored) and a debug-only `FixtureAnimeSource` with 15 named cases (failing hoster, lazy hoster, preferred
video, header checks, mpvArgs, external subtitles, chapters, timestamps, stall). Absent from preview/release.
Result: _pending_ — **server half done (Claude 2026-11-20, uncommitted, `.worktrees/ay0-server`, branch `tsuzuki/ay0-fixture-server` from `main`; scripts only, no app code, so it cannot clash with AY9).** `scripts/anime-fixture/make-media.sh` (ffmpeg 9 on this PC): `ok.mp4` 60 s 720p with burnt-in clock, `hls/` 1080/720/480 (24 s, 4 s segments), `tracks.mkv` 90 s (jpn 440 Hz + eng 880 Hz audio with titles, styled ASS, chapters Opening/Part A/Ending — confirmed with ffprobe), `en.srt`/`en.vtt`/`ja.srt`, `thumb-01..03.jpg`; **15 MB total, byte-identical on a second run** (hash compare). `serve.py` (stdlib, `127.0.0.1:8765` only, Range 206/416, HEAD, routes media/referer/ua/header/fail/slow/stall, `/api/anime.json`, `/api/episodes/<case>.json`, `/api/videos/<case>.json` for all 16 cases incl. `noHosters` for 16; one log line per request; dropped player connections don't print tracebacks). `README.md` with commands, media, routes, JSON fields and the case table. `.gitignore` ignores `media/`. Tests `scripts/tests/test_anime_fixture_server.py`: **8 pass** (loopback bind, Range incl. suffix and 416, header gates, fail/slow/stall, path traversal, case JSON, every case URL is made by the script). Live smoke on the PC: `curl -I` 200 + `Accept-Ranges`; ffprobe reads the HLS master (24 s) and `/referer/ok.mp4` only with the Referer (403 without); an ffmpeg seek to 40 s uses 206 ranges. **App half — done (Claude 2026-11-20, same worktree, uncommitted; built on `main`, NOT on `tsuzuki/ay9` as PARITY §9 asks — AY9 is still uncommitted and untouched since 2026-11-15; none of these files overlap AY9's, so moving them onto `tsuzuki/ay9` is a plain copy).** `app/src/debug/.../tsuzuki/anime/debug/FixtureJson.kt` (pure JSON → `SAnime`/`SEpisode`/`Hoster`/`Video`: lazy and throwing hosters carry no videos, headers, preferred, subtitle tracks, timestamps, mpvArgs, quality preference) and `FixtureAnimeSources.kt` (`FixtureAnimeSources.all()`; shared `FixtureCatalogueSource` with **no** hoster overrides; `FixtureAnimeSource` "Tsuzuki Fixture", id `7311000000000001`, cases 01–15, hoster methods + `ConfigurableAnimeSource` "Preferred quality" `preferred_quality` default 720 used by `sortVideos`; `FixtureNoHosterAnimeSource` "Tsuzuki Fixture lib16 no hosters", id `7311000000000016`, case 16, only `getVideoList(episode)`; headers `User-Agent: TsuzukiFixture/1` + `Referer`). Main: `AnimeSourceManager(repository, builtIn = { emptyList() }, loadExtensions)` merges built-ins before `distinctBy` (installed ids win; `builtIn` placed before `loadExtensions` so existing trailing-lambda callers are unchanged) and `create()` passes `::debugBuiltInAnimeSources` (by-name reflection, `BuildConfig.DEBUG` only, missing class → empty). Tests: `FixtureAnimeSourceTest` (testDebug) 8 + 2 new in `AnimeSourceManagerTest`; full app suite **1,227, 0 failures/errors/skips**; scoped Spotless clean. **Device (tablet, scratch, Colour, server + `adb reverse`):** Browse › アニメ › ソース lists both fixture sources (shown in the sources list; the extensions tab lists APKs only); the catalogue shows cases 01–15 with thumbnails; case 01 details show 2 episodes with preview images and dates; **case 01 plays: burnt-in clock 00:00:11.458 → 00:00:16.833 in two screenshots ~5 s apart**; server log shows `/api/videos/01.json` and `/media/ok.mp4` 206 with the source UA and Referer. **Preview APK: 0 fixture classes/strings in its 3 dex files** (debug APK: present). Evidence `.worktrees/ay0-server/app/build/review-evidence/ay0/` (screenshots + `server.log`). Data restored (389 files byte-equal), reverse removed, server stopped. **"Before" record for AY1 (tablet, Colour, landscape, screenshot 14 s after 第1話を再生; driven by on-screen text with `cases_run.py`; contact sheet `review-evidence/ay0/sheet.png`, server log `server-before-cases.log`):** 02 HLS plays (mpv picked the 1080p variant); **03 fails** — error 「再生できませんでした」 "Fixture hoster A (throws) failed", no fallback to hoster B; 04 plays 720p (the non-lazy hoster B); **05 plays 1080p, ignores `preferred`**; 06 plays (source Referer sent); 07 plays (video `X-Fixture` header sent); 08 plays (whether the disallowed `script` arg was dropped not checked); 09 plays (subtitle menu not opened); 10 plays (tracks/chapters not opened); 11 plays; 12 error with Retry, no crash (expected); 13 still loading at 14 s (expected, 20 s delay; play after not checked); **14 frozen at 00:00:01.167 with no buffering indicator** (AY4); **15 plays 1080p, the 720p preference not applied** (`sortVideos` not used); **16 fails with HTTP 404** — the app called the default hoster path `GET /episode/16/1` on a source that has no hosters (AY1's detection). No crash in any case. **S22 (wireless), Dot:** anime switched on in the scratch prefs for the run (restored after); Browse › アニメ lists both fixture sources (uppercased by Dot); **case 01 plays, clock 00:00:15.625 → 00:00:21.333 ~5 s apart** (`review-evidence/ay0/s22-c01-*.png`, `server-s22.log`); no crash. Not run: Colour on the S22, Dot on the tablet, cases 02–16 on the S22. Later (2026-11-20): `serve.py --new-episodes` gives every case an episode 3 so Updates can be tested (test added; 9 server tests pass).

#### AY1 Anime: load streams the way Aniyomi does (T4) — `passed` (self-review under owner waiver 2026-11-22; in the v0.2 RC)

Self-review (Claude, 2026-11-22, **owner waiver**: "Codex doesn't have usage left, you can do the reviews instead"; Claude wrote this card): **PASS.** Full diff read. Non-lazy hosters load at once with a 20 s limit each, so a dead or hanging mirror no longer fails the episode. On Auto the first `preferred` video that resolves starts before the rest finish. An already-initialized video is never sent back into the extension (`resolve` returns it; the queue's re-resolve is a no-op). Downloads use the same hosters, order and source headers. The 30 s FILE_LOADED watchdog is cancelled on load, error, reset and the next stream. `AnimeHosterLoaderTest` covers it. **Non-blocking:** a source whose hosters are all lazy gets no automatic video, neither in the player (it fails with the generic message until the user picks a server in the quality sheet) nor for downloads (`loadAll()` skips lazy hosters). Follow-up card if a real extension shows it.

**Add to AY1 (Claude 2026-11-20, found with the AY0 fixture):** episode **downloads** ignore the source's headers. `AnimeVideoTransfer` uses `video.headers ?: Headers.Builder().build()` (empty), and its caller `AnimeDownloads.transfer` passes only the source's client — so a video with no own headers is downloaded with a generic browser User-Agent and no Referer (server log `.worktrees/ay-int/app/build/review-evidence/ay-int/server-save-all.log`). Aniyomi falls back to the source's headers: use `video.headers ?: (source as? AnimeHttpSource)?.headers`. Check with fixture cases 06/07: download, not just play.
**Detailed plan: PARITY §9.2.** PARITY §5 AY1, findings 1–5. Parallel per-hoster loading with per-hoster errors and lazy hosters; hoster-method
detection for lib-16 extensions; the source's `sortHosters`/`sortVideos` and `Video.preferred` respected (the
player's quality choice wins only when the user set one); `video.headers ?: source.headers`; `mpvArgs`;
`file:`/`content:`/`data:` URLs; downloads use the same selection.
**Handoff for Codex (Claude 2026-11-21) — AY1 is mostly built, NOT committed; continue from here.** Worktree `.worktrees/ay1`, branch `tsuzuki/ay1` from `tsuzuki/ay0-fixture-server` `70974003a` (AY9 is uncommitted, so AY1 does not contain it; rebase/merge onto AY9 when that lands). Plan: PARITY §9.2.
Done (uncommitted, 13 files + 2 new):
- `tsuzuki/anime/AnimeStreamResolver.kt`: `animeSourceHasHosters()` (cached reflection, via `classOverridesHosterMethods(type, stopAt)`); `hosters()` — lib < 16 or no hoster methods → one `Hoster.NO_HOSTER_LIST` pseudo hoster of the `sortVideos()`-ed episode list, else `getHosterList()` → `sortHosters()`; `hosterVideos()`; `resolve()` skips `Video.initialized` and marks results initialized. Old `videos()` kept (debug activity uses it).
- New `tsuzuki/anime/player/AnimeHosterLoader.kt`: `AnimeHosterState` (Idle/Loading/Ready/Error), `AnimeHosterEntry`, `AnimeHosterLoader` (non-lazy hosters in parallel, each `withTimeout(20 s)`; a throwing/hanging hoster → Error, others continue; lazy ones only via `loadHoster(index)`; a hoster with its own `videoList` needs no request), `animePlaybackOrder()` (**Auto keeps the source order, `preferred` first; a picked quality re-sorts stably via `rankAnimeVideos`; blank URLs last**).
- `AnimePlayerPolicy.kt`: `AnimeStreamQueue` uses `animePlaybackOrder`; `ANIME_FILE_LOADED_WATCHDOG_MS = 30_000`.
- `AnimePlayerModel.kt`: `openEpisode` loads via `AnimeHosterLoader` (`hosterLoader`, reset per episode); if every hoster failed, the first hoster error becomes `lastFailure`; `AnimePlayerState.hosters`; `loadHoster(index)` (picked lazy/failed server → plays only that hoster's videos); **30 s FILE_LOADED watchdog** in `loadPending` (cancelled on load/error/new load) → same path as an mpv error.
- `AnimePlayerSheets.kt` quality sheet: after the qualities, rows for non-plain hosters that are not Ready (`Tap to load` / `Loading…` / `Couldn't load — tap to retry`) → `model.loadHoster`. Strings `tsz_anime_player_server_{load,loading,failed}` in all 8 `anime_player_strings.xml`.
- `download/AnimeDownloads.kt`: same hosters/loader/`animePlaybackOrder`, and **`withSourceHeaders` on the resolved video** (downloads previously went out without Referer/User-Agent).
- New test `test/.../anime/player/AnimeHosterLoaderTest.kt` (9): hoster-method detection (3 classes), throwing hoster isolated, hanging hoster cut at the limit (virtual time), lazy not asked until picked, own videoList, Auto order + preferred, picked quality stable, blank URLs last. These + `AnimePlayerPolicyTest`, `AnimeVideoHeadersTest`, `AnimeDownload*`, `FixtureAnimeSourceTest` pass. Full suite / Spotless **not yet run**.
Device script (tablet, scratch `rv5debugscratch`, fixture server from `.worktrees/ay0-server` + `adb reverse tcp:8765`; helper `ay1_case.sh` idea: open Browse › アニメ › Tsuzuki Fixture › case › Episode 1, then `logcat -s TsuzukiAnime` + server log; evidence `app/build/review-evidence/ay1/caseNN.{log,png}`):
- **03 first hoster throws — pass:** `hoster A (throws) failed`, `hoster B: 1 video`, FILE_LOADED +125 ms; server `/media/ok.mp4` with Referer + `TsuzukiFixture/1`; no crash.
- **04 lazy hoster — pass:** only `hoster B (720p)` loaded, no request for A; B played (FILE_LOADED +130 ms); no crash.
- **05 preferred video — pass:** `hoster Fixture: 3 video(s)`, resolved and played `720p (preferred)` (the middle one), FILE_LOADED +127 ms; no crash.
- **06 needs source headers — pass:** `video.headers` null, request `/referer/ok.mp4` carried `Referer: http://127.0.0.1:8765/` + `TsuzukiFixture/1` (the source's headers), FILE_LOADED +116 ms; no crash.
After the run: tablet data restored byte-equal (9 files), reverse removed, fixture server stopped, devices free. Device helpers for the remaining cases: `app/build/review-evidence/ay1/ay1_case.sh` (set `SRV` to the fixture server's stdout log) and `dev.sh`; the fixture server log of this run is `fixture-server.log` there.
**Result and handoff to Claude (Codex 2026-11-21, at the owner's request).** The 17 AY1 files are staged in `.worktrees/ay1` on `tsuzuki/ay1`, HEAD `70974003a`. The owner asked for handoff while supplemental device QA was running. AY1 has no commit yet. AY9 and the later cards remain in their previous state.

- **Implementation.** `AnimeHosterLoader.kt` now starts a resolved preferred stream on Auto and cancels unfinished hoster jobs after successful resolution. The player cancels an in-flight manual hoster choice when switching episodes. `AnimeStreamResolver.kt` sorts inline lists supplied by a hoster, skips re-resolving initialized videos, and encodes spaces in HTTP(S) stream URLs. Extension streams stay HTTP(S); the app uses its own paths for offline playback. `AnimePlayerSheets.kt` groups variants by hoster and shows lazy and failed hosts using the existing rows. A failed host offers Retry, following Claude's implementation handoff. The quality label says Auto in all eight locales. Downloads apply source headers when the video supplies none.
- **Build.** Final scratch Debug APK SHA-256 `7c3a2715570f1f1f67916e886aceb4d8709d36743acc419b9ca28314f7622cff`. Final scoped Spotless twice, `assembleDebug`, app and all-module `testDebugUnitTest`, and `git diff --cached --check` passed. **1,403 tests** (1,236 app, 106 domain, 35 source-local, 26 core/common), zero failures, errors, or skips. Evidence `ay1-auto-spot1.log` and `ay1-auto-build-tests.log`. A preceding build daemon exited during a test run and the low-worker retry passed.
- **Both devices, Dot core fixture matrix.** 03 fell back from throwing A to B; 04 played B before A was requested; 05 played the flagged 720p stream; 06 sent the source Referer and User-Agent; 07 sent `X-Fixture: video`; 08 logged the dropped `script` option and played; 12 exhausted both HTTP 500 streams and showed the playback error; 13 loaded after ~20 s with no watchdog; 15 on Auto played 720p; 16 played through the lib-16 source's plain list. Case 01 loadfile → FILE_LOADED samples (ms): tablet 94/51/45/49/44 (median 49), S22 246/133/126/124/142 (median 133). Evidence `tablet-final-dot.txt`, `s22-final-dot.txt` (03), and `s22-final-portrait-browse.txt` (04 onward); the S22 used portrait Browse and landscape playback. Both matrix crash logs are empty.
- **Tablet supplemental.** Case 04 lazy A played 480p on demand in Dot and Colour; the final APK was reinstalled and both quality sheets were captured again. Case 12 Retry reproduced the readable error. On the 40 s fixture, case 13 fired the watchdog at 30 s. Case 06 episode 1 downloaded with the Referer and played from a local file with the fixture port removed (the first probe had downloaded episode 2 and was corrected). Case 15 manual 1080p played. Evidence `tablet-*-quality*`, `tablet-retry.log`, `tablet-13-watchdog.*`, `tablet-06-offline.*`, and the server logs. Offline proof used removal of fixture forwarding.
- **S22 supplemental and live source still open.** S22 case 04 lazy 480p passed in Dot. Colour sheet, explicit Retry, 40 s watchdog, offline download playback and manual 1080p remain to verify on the S22. The supplement runner lost the fixture row when Browse returned to landscape, and was paused for handoff. Run portrait Browse again when resuming. Tablet live AnimePahe playback/resume and tablet portrait playback remain unverified. Automatic approval initially rejected the live smoke because trusting an extension changes its execution permissions. The owner then explicitly approved trust for only the existing `eu.kanade.tachiyomi.animeextension.en.animepahe` on the tablet's temporary `ayscratch` app. The attempted UI navigation tapped the rail's More label instead of the Advanced settings row and ended before any trust change. Use More → Settings → Advanced → Debug info → Anime test → Sources and verify the exact package in the in-app dialog. No source was trusted during this run.
- **Resume.** The owner approved the separate package `io.github.manojx2005.tsuzuki.ayscratch` on both devices. Evidence and helper are at `.worktrees/ay1/app/build/review-evidence/ay1/`; remove the `handoff-pause` marker there when resuming the helper. Start the normal local fixture on 8765. The separate 40 s server used host port 8766 and `adb reverse tcp:8765 tcp:8766`. Claim the Device lock first. The package contains only this run's fixture data; the old `tablet-before.tar` belongs to the prior `rv5debugscratch` run and must not be restored into this package. Finish the outstanding checks, record any limits, commit on `tsuzuki/ay1` and hand it to an independent reviewer. Then AY9, merge the two card branches and rerun the fixture, followed in order by AY2, AY3, AY4 and AY6.
- **Finished by Claude (2026-11-21) — committed `07bd59c7f` on `tsuzuki/ay1` (17 files, the staged set unchanged; installed APK built from the same source, Debug `70974003A` 12:36). Status `review`.** Fixture servers from `.worktrees/ay0-server` (8765 normal, 8766 `--slow-seconds 40`), `ayscratch` only. Helper fixes in `ay1_device.py`: one more look after the last scroll; `launch()` sets `user_rotation 0` when auto-rotate is off, because the landscape player leaves a locked S22 at 90° and Browse then shows only one source row (that was the "lost fixture row").
  - **S22 supplement — all pass:** Colour case 04 sheet shows 自動 / 720p (playing, highlighted) / `A (lazy, 480p)` タップして読み込む / hoster B's variants under a `B (720P)` section label; tapping A loaded it and played 480p from 11.6 s (FILE_LOADED +155 ms). Case 15 manual **1080p** from 11.7 s (+211 ms). Case 12 **Retry** reloads both hosters, tries the next stream (別のストリームを試しています toast), ends on the error card again; no crash (the toast briefly covers the card's buttons — cosmetic). Case 13 on the 40 s server: `mpv FILE_LOADED watchdog: nothing after 30 s` → `no playable video left`. Case 06 **offline**: downloaded, port removed, played `…/Download/anime/7311000000000001/4/8/video.media` (clock 00:00:12.958). Evidence `s22-COLOUR-{04,15}-quality*`, `s22-retry*`, `s22-13-watchdog*`, `s22-06-*`.
  - **Tablet portrait:** cases 05 and 02 started from portrait Browse; the player went landscape and played (FILE_LOADED +130/+237 ms). Case 05 played 1080p, not the preferred 720p, because both scratch apps keep `tsuzuki_anime_player_quality = 1080` from the manual picks — expected (a picked quality wins); a reviewer resetting it to Auto should see 720p. Evidence `tablet-portrait-0{2,5}*`.
  - **Live AnimePahe — not verified (site-side).** Trusted only `eu.kanade.tachiyomi.animeextension.en.animepahe` (v16.58) on the tablet `ayscratch`, after the in-app dialog named that exact package (owner-approved; `tablet-claude-trust-dialog.png`). The catalogue returned the source's own "You have been rate limited. Wait a few seconds and refresh" twice, 30 s apart; the app showed it with Retry, no crash. Not retried further. Live playback/resume on a real lib-16 hoster source stays open for the reviewer or AY6.
  - **Found, not AY1:** Debug info › アニメのテスト shows mojibake `â€¹` for its back arrow (debug-only screen; a `‹` string read with the wrong encoding). Small follow-up for the anime lane.
  - **Handback:** both on Dot Home, crash buffers empty, `adb reverse` removed, auto-rotate restored on both (S22 `accelerometer_rotation 1`; before-values in `*-claude-rotation-before.txt`), both fixture servers stopped. AnimePahe stays trusted in tablet `ayscratch`.
  - **Next:** independent review (PARITY §9.0 checklist); then merge with AY9 when that is committed and rerun 03–08, 12, 13, 15, 16.

Trial merge (Claude, 2026-11-21 ~23:05): `trial/ay1-ay9` (`.worktrees/ay1-ay9`, merge `5afa14c0a`) = `tsuzuki/ay1` `07bd59c7f` + `tsuzuki/ay9` `7ba2fca41`. Only conflicts: the append-only tails of the 8 `anime_player_strings.xml` files (both sides kept, no duplicate names). Full app suite 1,243 tests, 0 failures. AY1 fixture cases on the combined build (tablet, scratch `ay19qa`, 2026-11-22 ~01:30): **01, 03 (first hoster throws, B plays), 04 (lazy hoster, HLS), 05 (preferred 720p), 06 (source headers, `/referer/ok.mp4` 206) and 16 (LIB16, no hoster list, direct video) all reach mpv FILE_LOADED** with Referer and User-Agent on the media request; 0 crash lines. Evidence: `.worktrees/ay1-ay9/app/build/review-evidence/ay1-ay9/case*.png|log`. S22 not run on the combined build. Not a card branch; the anime lane or REL-0.2 decides the real merge order.

#### AY2 Anime: source settings screen (T3) — `passed` (self-review under owner waiver 2026-11-22; with AY2-F/F2)

Self-review (Claude, 2026-11-22, **owner waiver**: "Codex doesn't have usage left, you can do the reviews instead"; Claude wrote this card): **PASS.** The settings activity exits when anime is off or the id is missing, and catches a throwing `setupPreferenceScreen`. Anime `source_<id>` settings join backups through one `PreferenceBackupCreator` hook (`distinctBy` on the key; the restorer already writes them back). The earlier filter findings are fixed by AY2-F and AY2-F2 (Claude reviewed F2 above). Nit: the backup hook's `runBlocking` database read runs on the creator's IO thread.


Independent pre-review (Codex Batch T, 2026-11-21): Changes requested. Source review of `7043a84bd`. Settings reload the source instance and `AnimeExtensionLoader.kt:320` creates a fresh class loader. `AnimeCatalogueActivity.kt:144` retains the old extension-defined filter objects and passes them to the new source at lines 229–231. Searches can fail when the extension casts a filter to its own class, even when the filter has the same class name. Rebuild filters using the new source and reapply compatible values, or clear incompatible filters. Cover filtered search → change settings → back to results. Device acceptance and integration remain pending AY1’s independent review.

**Detailed plan: PARITY §9.3.** PARITY §5 AY2, finding 6. `ConfigurableAnimeSource` preferences from the extension details and the catalogue
overflow, stored under `source_<id>`, applied without a restart, included in backups.
Result (Claude 2026-11-21): **built, committed `7043a84bd` on `tsuzuki/ay2` (worktree `.worktrees/ay2`), status `review`.**
- **Code.** New `tsuzuki/anime/ui/AnimeSourcePreferencesActivity.kt` — a host activity (the catalogue is an Activity, so both entry points just start it; not a Voyager screen) with the Tsuzuki `AppBar`, a `FragmentContainerView` and `AnimeSourcePreferencesFragment` (the manga `SourcePreferencesFragment` styling copied, minus its manga-only "reverse chapters" row; `SharedPreferencesDataStore(source.sourcePreferences())`, so values live in `source_<id>`). Loading → `LoadingScreen`; a missing/non-configurable source or a throwing `setupPreferenceScreen` → ERR1 `TsuzukiErrorState`. An `OnSharedPreferenceChangeListener` marks a change; leaving (`isFinishing`) after a change calls `AnimeSourceManager.extensionsChanged()`. Restored fragments are dropped and re-added (no source before `ensureLoaded()`). `hasAnimeSourceSettings()` decides the buttons. Registered in the manifest.
- **Entry points.** Catalogue top bar: settings icon (`MR.strings.source_settings`, ソース設定) when the source is configurable; the catalogue's location now reloads when `extensionChanges` moves, so it gets the fresh instance and page 1. Extensions tab: settings icon before remove on installed rows with a configurable loaded source; several → a picker dialog (name · lang). The Extensions tab only appears with an anime repo, so it was **not seen on device** (scratch apps have none).
- **Backups.** `PreferenceRestorer.restoreSource` writes `source_<id>` by name without the manga manager, so only writing was needed: `tsuzuki/anime/backup/AnimeSourcePreferencesBackup.kt` (every anime source in the saved source list, empty files skipped, empty while anime is off) + a one-line `.plus(...)` + `distinctBy(sourceKey)` in upstream `PreferenceBackupCreator.createSource` (inside the existing "source settings" option and private-key filter). Added to AGENTS.md. Key prefix unchanged (`source_<id>`, as Aniyomi).
- **Tests.** `AnimeSourcePreferencesTest` (5): button decision, key name, backup entries keyed/deduped/empty skipped, the creator's private filter applies, nothing while anime is off. **Full app suite 1,241, 0 failures/errors/skips.** Scoped Spotless; 12 unrelated files it reformatted restored.
- **Device (ayscratch, fixture server, Device lock).** Tablet Dot: catalogue › ソース設定 shows "Preferred quality 720p" (`tablet-DOT-settings.png`); picked 480p → `source_7311000000000001.xml` `preferred_quality=480`; back, case 15 on Auto played **480p without restarting** (`resolved video … title='480p'`, FILE_LOADED +119 ms); after force-stop and relaunch still 480p (+126 ms). Tablet Colour (`tablet-COLOUR-settings.png`) and S22 Dot (`s22-DOT-settings.png`) render. **Real extension:** AnimePahe (catalogue still rate-limited) › ソース設定 opens its 7 settings (mirror, quality, language, site episode number, HLS, AV1, custom UA), no crash (`tablet-COLOUR-animepahe-settings.png`). Set back to 720; player quality reset to Auto on both; crash buffers empty. Evidence `.worktrees/ay2/app/build/review-evidence/ay2/`.
- **Not done / limits.** On-device backup → change → restore not run: the scratch apps have no backup folder and setting one goes through the system folder picker (not tapped). Covered by unit tests plus the restorer reading by key. Look: the fragment uses the platform preference theme, so AnimePahe's switches are cyan, not Tsuzuki amber — the same as the manga source settings page; no board exists, recorded not changed. No Colour S22 shot.


#### AY2-F Rebuild filters after a source settings change (T2) — `blocked` (independent changes requested on `b7611663d`)

Independent pre-review (Codex Batch T). The new source-reload path calls the extension's `getFilterList()` from `remember(source)` during composition. A blocking source freezes the UI and a thrown source error can abort composition. Obtain the new filters on IO through `callAnimeSource` and handle the failure before resuming the catalogue request. Also exercise a choice list that keeps the same size but reorders options: an in-range old index can silently select a different option. No device verdict yet.

When catalogue paging switches to a new source instance, replace extension-owned filter objects. Carry over compatible filter values and test an active filtered search across a settings change.

Result (Claude, 2026-11-21): `AnimeCatalogueActivity` keys the applied filters on the source instance: when a settings change brings a new instance, `carryAnimeFilters` builds them from the new `getFilterList()` and copies the compatible values (no filters stay no filters), so old extension-owned objects never reach the new source. `restoreAnimeFilterStates` now matches by class *name* + title (class identity differs across class loaders, so the old check dropped every value) and copies only source-api value types that still fit (select/sort index in range, Text String, CheckBox Boolean, TriState Int; groups recursively). Tests 3/3 `AnimeFilterCarryTest`, including the filters loaded again through a second class loader like a reloaded extension (values kept, objects from the new loader) and values that no longer fit; all 151 `tsuzuki.anime.*` tests pass. Not run on a device: the fixture sources have no filters, so a filtered search across a settings change needs a real extension with both filters and settings.

#### AY2-F2 Reload catalogue filters off the UI thread — `review` → code review passed with a nit (Claude 2026-11-22); lands with the anime chain

Code review (Claude, 2026-11-22; Codex wrote this card, so independent): **passes in code.** Filters load on IO through `callAnimeSource`, a failure shows a retry instead of searching, and choices are matched by unique label (ambiguous or removed keep the default). **Nit (not blocking):** `attempt`/`rebound` and the `LaunchedEffect` live inside `if (anime == null)`, so every Back from an anime's details reloads the filters (brief notice, one more `getFilterList()`, and the error screen if that load fails). Hoist them above the branch when this file is next touched. Not checked on a device by Claude.



Handle filter load errors before issuing a search with a reloaded source. Preserve choice labels and sort direction across reorderings where the match is unique.

`AnimeCatalogueActivity.kt` gates paging for a replacement source until rebuilt filters are ready, preserves the saved query and shows a retry action if loading fails. Opening and resetting the filter sheet use the same IO loader. `AnimeFilterDialog.kt` matches fields by unique class name and title and matches choices by unique displayed label. Removed or ambiguous choices keep the new source default. `AnimeFilterCarryTest.kt` covers reordered fields and choices, removed and duplicate choices, and filter objects from a replacement class loader. Scoped Spotless and all 1,413 Android unit tests passed. Tablet verification (Colour, `df1d501d8` plus a disposable debug fixture): the actual catalogue search kept query Needle and choice Beta when 720 changed to 480, the option order reversed, and a new source instance loaded. The fixture asserts that filter loading runs off main. The preference write and source-change signal were driven by the probe. The S22 and a third-party source remain unverified. Scratch packages removed and crash buffer empty. AY2-FILTER-CONTRAST fixes the black filter icon visible in the original Colour screenshot. Evidence `.worktrees/ay2-filter-qa/app/build/review-evidence/ay2-filter-qa/`.

#### AY2-FILTER-CONTRAST Keep the catalogue filter action visible (T1) — `review` → code review passed (Claude 2026-11-22); lands with the anime chain

Code review (Claude, 2026-11-22; Codex wrote this card, so independent): **passes in code.** Same token pair as the episode tiles; the button is disabled while filters load.



The filter icon rendered black against the dark catalogue search background. Use the same foreground and disabled tokens as the episode action, with the existing catalogue layout.

Result (Codex, 2026-11-22): `AnimeCatalogueActivity.kt` gives the filter button the theme foreground and disabled colours. The tablet fixture passed filtered search and preference-triggered source reload in both themes. The same 251 icon pixels that were black before the fix in Colour are white in Dot and Colour after it. Scoped Spotless, Debug assembly, and all 1,503 unit tests passed. Scratch packages removed and crash buffer empty. S22 unavailable. Evidence: `.worktrees/ay2-filter-qa/app/build/review-evidence/ay2-filter-contrast/`.

#### AY3 Anime: subtitles always render + subtitle style (T3) — `passed` (self-review under owner waiver 2026-11-22; with AY3-F/F2)

Self-review (Claude, 2026-11-22, **owner waiver**: "Codex doesn't have usage left, you can do the reviews instead"; Claude wrote this card): **PASS.** Bundled Manrope and the Japanese subset as libass `subfont.ttf`. They are copied once per install or update; the marker is written last, so an interrupted copy is redone. Style maps onto `sub-*` options, and only changed options are re-applied. Language labels such as "English (SRT)" and "日本語" are normalized. Inline `data:` subtitles: AY3-F (percent bytes, a failed cache skips one track) plus AY3-F2 (prepared on IO). Non-blocking: the 2.3 MB first copy runs on the main thread, like the existing cacert copy.


Independent pre-review (Codex Batch T, 2026-11-21): Changes requested. JVM and source review including the follow-up `09226361a` on AY6. `AnimeDataSubtitles.kt:22` turns percent-encoded bytes into UTF-8 text and back. The exact helper replaced ISO-8859-1 byte E9 with EF BF BD. The cache helper also threw FileNotFoundException when the cache directory could not be created. `MpvAnimePlayerLauncher.kt:266` lets the error escape the main-thread track refresh. Preserve percent-encoded bytes and handle cache failure per subtitle so video and other tracks remain usable. Evidence `.worktrees/review-v02-main/app/build/review-evidence/v02-ay-source-review/subtitle-result.txt`. Device acceptance and integration remain pending AY1’s independent review.

**Detailed plan: PARITY §9.4.** PARITY §5 AY3, findings 7–9. Review lane checks fixture case 09 (SRT, Japanese SRT) first; then fonts in the mpv
config dir if needed, any-scheme external tracks, and style settings (size, colours, border, position, override ASS).
Result (Claude 2026-11-21): **committed on `tsuzuki/ay3` (worktree `.worktrees/ay3`), status `review`.**
- **Step 1 finding (before):** no subtitle rendered at all, on either device. mpv logged `libass: can't find selected font provider` — Android has no system provider, so libass had no fonts. The three case 09 tracks were added but never selected, because a language preference never matched labels like "English (SRT)" / "日本語 (SRT)".
- **Fonts (`AnimeSubtitleFonts.kt`).** libass also can't fall back per glyph between files without a provider; its last resort is `<config-dir>/subfont.ttf` (mpv-android ships one). So `lnr_manrope.ttf` → `mpv/fonts/manrope.ttf` as `sub-font`/`osd-font` and the Japanese subset `lnr_jp_zenkaku_regular.ttf` → `mpv/subfont.ttf`. No new font files; licences `docs/licenses/OFL-1.1.txt` (Manrope) and `OFL-zenkakugothicnew.txt`. Copied once per install/update (marker = package `lastUpdateTime`; font resources are compressed, so size can't be read): **13 ms** on the tablet, on the main thread because mpv init is synchronous in the view model (moving it would reorder create/attach). The subset covers all 34 non-ASCII characters of ja.srt; it has no ASCII, which Manrope supplies.
- **Look limit for the owner:** Manrope is a variable font whose default instance is ExtraLight, and libass matches it only as "Manrope ExtraLight" — English subtitles render thin (`tablet-09-en-manrope.png`); the outline keeps them readable and the new Bold row helps. Not swapped unasked.
- **Track choice (`AnimeTrackLanguages.kt`).** Labels name their language: English and native names ("日本語", "español"), first word of 3+ letters ("English (SRT)", "[Eng] Full", "Japanese - Signs"); 1–2 letter words ("it", "no", "CC") are not codes. External tracks now pass the label as mpv title and its code as language (`externalTrackArgs`), and the forced/signs rule reads title + language, so "English (Signs)" loses to "English". Non-http(s) track URLs are logged and skipped (unchanged rule). `data:` subtitle URLs: **done later as a follow-up, `09226361a` on `tsuzuki/ay6`** (`AnimeDataSubtitles.kt`: base64/percent-encoded, written once to `cacheDir/anime-subs/<sha1>.<ext>`, `file:`/`content:` still refused; 3 tests; not device-checked, no fixture case uses one).
- **Style (`AnimeSubtitleStyle.kt`).** Font size 30–80 (55), bold, text colour pills (white/yellow/cyan/green), outline black/grey/none + width 0–5 (3), background box (`sub-border-style=background-box` + `#99000000`; accepted by the vendored mpv), position 50–100 % (95), "restyle styled (ASS)" (`sub-ass-override` force / mpv default `scale` when off — the plan said `no`; `scale` keeps the existing size setting working for ASS). Keys `tsuzuki_anime_player_sub_*`, clamped on read and write; applied live, only changed options. Rows in the player settings Subtitles card, which now spans the full width (Dot S22 `s22-DOT-subtitle-settings.png`; a gap remains next to the speed card). The board shows Subtitles as a tile opening its own section; this keeps the existing inline-card pattern. Strings in 8 locales.
- **Tests:** `AnimeSubtitleTracksTest` (6) and `AnimeSubtitleStyleTest` (5): label languages + negatives, signs/forced rule with three tracks, sub-add args, font copy rule, every style row → mpv value, defaults, clamping, changed-only. **Full app suite 1,252, 0 failures.** Scoped Spotless.
- **Device (ayscratch, fixture, Device lock):** tablet case 09 en.srt renders "English subtitle 1 (0 s)" (`tablet-09-en-manrope.png`), ja.srt "これは字幕のテストです（0 秒）" with the ASCII 0 from Manrope and kana/kanji/fullwidth brackets from subfont (`tablet-09-ja-after.png`); S22 ja (`s22-09-ja-final.png`); S22 style yellow/80/box/60 % visible (`s22-09-style.png`). No crashes. Prefs reset after.
- **Case 10 (later, tablet, `tsuzuki/ay6` build):** the embedded "Styled ASS" track renders; restyle off keeps its own look (green, drop shadow, higher), restyle on applies the user outline/position (`.worktrees/ay6/app/build/review-evidence/ay6/tablet-10-ass-{off,on}.png`); the green colour stays, presumably an inline `\c` tag that `force` doesn't touch (not confirmed from the file). **Not done:** `data:` subtitle URLs, Colour screenshots of the settings card, one screenshot per style row on the S22, Japanese "before" shot on the S22. Evidence `.worktrees/ay3/app/build/review-evidence/ay3/`.


#### AY3-F Preserve inline subtitle bytes and isolate cache failures (T2) — `blocked` (independent changes requested on `828d22965`)

Independent pre-review (Codex Batch T). Byte preservation and per-track cache-failure handling are present. The optimized route still calls inline decoding, hashing, directory creation and file writes synchronously from `AnimePlayerModel.onLoaded()` → main-thread `refreshTracks()` → `MpvAnimePlayerBackend.tracks()`. Prepare inline track files off the UI thread and discard results from a superseded load. Keep a test for a failed write leaving the other tracks playable. No device verdict yet.

Decode percent escapes to bytes. Keep a failed inline subtitle cache write from interrupting playback or other tracks. Cover non-UTF-8 input, cache failure, and successful retry. The inline subtitle follow-up is on the AY6 branch.

Result (Claude, 2026-11-21): `percentDecodeBytes` decodes RFC 2397 escapes straight to bytes (`%E9` stays E9; literal characters as UTF-8; `+` literal; a broken escape refuses the URL). `writeAnimeDataSubtitle` returns null instead of throwing when the cache directory or file can't be written, writes through a `.part` file and rename, and rewrites a short file left by an earlier failure. `MpvAnimePlayerLauncher.tracks()` skips only that subtitle with its own log line; video, audio and other subtitles are still added. Tests 6/6 `AnimeDataSubtitlesTest` (+ Latin-1 and Shift-JIS bytes, broken escapes, cache dir blocked by a file then a successful retry, truncated file rewritten). **Tablet** (scratch `ay3fqa` = this branch; an untracked copy of the AY0 server gave case 01 an inline WebVTT with Latin-1 escapes `Caf%E9 cr%E8me br%FBl%E9e`, a broken `data:text/vtt,bad%E` and the normal English WebVTT): video played throughout; the broken one was skipped with one log line and is not offered; the sheet lists Inline Latin-1 and English (WebVTT); `cache/anime-subs/<sha1>.vtt` holds bytes E9/E8/FB (not EF BF BD); the selected track renders "Café crème brûlée"; crash buffer empty. Scratch uninstalled, the server copy deleted. Follow-up commit on the same branch: a `data:` URL that fails to decode now logs "the data: address is malformed" instead of "neither an http(s) nor a data: address". Spotless on this branch stops on a pre-existing max-line-length lint in `AnimeExtensionAbiTest.kt:65` (AY6's file, fix with AY6-F).

#### AY3-F2 Prepare inline subtitles off the UI thread — `review` → code review passed (Claude 2026-11-22); lands with the anime chain

Code review (Claude, 2026-11-22; Codex wrote this card, so independent): **passes in code.** Inline subtitles are prepared on IO before `load`; `cancelPendingLoad()` runs on episode/stream change, retry, failure and `onCleared`; the temp file + `ATOMIC_MOVE` publish is safe (minSdk 26), and a losing writer reuses the published file. Merged trial still calls it everywhere it should.



Prepare subtitle URLs and cache files before handing the active video load to mpv. Cancel pending preparation when the request changes. Keep HTTP tracks usable when an inline subtitle fails.

`AnimePlayerModel.kt` cancels the pending preparation on episode and stream changes, failure, and close. `AnimeDataSubtitles.kt` decodes and writes inline subtitles on IO, retains valid HTTP subtitles when the cache fails, and refuses extension-provided file and content paths. `MpvAnimePlayerLauncher.kt` adds the already-prepared tracks once the file loads. Each writer uses a separate temporary file and publishes it atomically. The first full suite reproduced a race where a losing writer could skip a subtitle already published by the winner. `f81d3d2db` reuses the complete file in that case. Scoped Spotless and all 1,440 Android unit tests passed on the final code. Evidence `.worktrees/ay3/app/build/review-evidence/ay3-f2/`. On the integrated Debug trial `652cfd65e`, the tablet's real mpv playback exposed both the HTTP and inline SRT tracks in Colour and the probe verified the exact cached bytes. With a file blocking the cache directory, Dot playback exposed the HTTP track alone. The malformed data track was absent in both checks. The clean retry used a fresh episode and waited for playing state before checking tracks. Scratch packages were removed, crash buffer empty. The S22 was unavailable. Evidence: `.worktrees/v02-anime-chain/app/build/review-evidence/ay3-f2-device/`.

#### AY4 Anime: media session, headphones, buffering state (T3) — `passed` (self-review under owner waiver 2026-11-22; with AY4-F)

Self-review (Claude, 2026-11-22, **owner waiver**: "Codex doesn't have usage left, you can do the reviews instead"; Claude wrote this card): **PASS.** The `MediaSessionCompat` is active only while started or in PiP and is released in `onDestroy`. The playback state throttles position updates to one a second. The becoming-noisy receiver runs from `onStart` to `onStop` and stays during PiP. The buffering spinner comes from mpv `paused-for-cache`/`seeking` on the main thread. After process death the player restores paused at the saved position. AY4-F: held keys toggle once, only seeking repeats. Non-blocking: D-pad left and right are taken for seeking while the player window has focus.


Independent pre-review (Codex Batch T, 2026-11-21): Changes requested. Source review of `922c9866b`. `AnimePlayerActivity.kt:223–228` handles every ACTION_DOWN for toggle keys, including key repeats. Holding Space can pause and resume repeatedly, leaving playback in a state that depends on the last repeat. Ignore repeated down events for toggle actions while preserving the intended seek-key behavior. Test held Space and a repeated media toggle. Device acceptance and integration remain pending AY1’s independent review.

**Detailed plan: PARITY §9.5.** PARITY §5 AY4, findings 10–11. `MediaSession` with metadata and transport controls, pause on
`ACTION_AUDIO_BECOMING_NOISY`, `paused-for-cache` → buffering indicator, media title, hardware keys. Includes
A6.9's process-death case.
Result (Claude 2026-11-21): **committed `922c9866b` on `tsuzuki/ay4` (worktree `.worktrees/ay4`), status `review`.**
- **Code.** New `player/AnimeMediaSession.kt`: pure `animePlaybackState()` (playing/paused/buffering/error/stopped, position, duration, next/previous/seek availability, speed), `animeMediaTitle()` ("Anime — Episode"), `animeKeyAction()` key table and `AnimeMediaSession` (`MediaSessionCompat` tag `TsuzukiAnime`, callback → model; position pushed at most once a second plus on state changes; metadata title/subtitle/duration; no artwork). Activity: session created in `onCreate`, active while started or in PiP, released in `onDestroy`; `ACTION_AUDIO_BECOMING_NOISY` receiver `onStart`/`onStop` (`RECEIVER_NOT_EXPORTED`) → `setPlaying(false)`; `onKeyDown` (only with window focus, so not while a sheet dialog has it) handles space, media play/pause/next/previous, D-pad ±10 s; a handled media key never reaches the session too. Backend: `onBuffering` from mpv `paused-for-cache || seeking`; `setMediaTitle` → `force-media-title` before each load. Model: `AnimePlayerState.buffering`; the centre spinner shows while buffering (no new visuals).
- **Process death (A6.9 leftover) — fixed here.** Two bugs: (1) the restore resumed playing; now a saved state in a new process restores **paused**; (2) a restored position near the end (e.g. 50.6 s of 60 s) went through the "near the end → start over" rule and restarted at 0; `animeResumeStart(restored = true)` and the model's later `restartIfStartedAtEnd` now keep it unless within 1 s of the end. Log line `player restore: state=… newProcess=… position=…`.
- **Tests:** `AnimeMediaSessionTest` (4: state per case incl. buffering/ended/failed, actions, title, key table), `AnimeResumeStartTest` +1. **Full app suite 1,257, 0 failures.** Not added: a fake-backend buffering transition test and a noisy-receiver fake (no backend fake exists in the suite).
- **Device (ayscratch, fixture, Device lock).** Tablet + S22: `dumpsys media_session` — active, media-button session, `PLAYING` → `PAUSED` on `KEYCODE_MEDIA_PAUSE`, actions 878 (play/pause/seek/ff/rw/next), description "01 plain mp4 — Episode 1" (`tablet-session-*.txt`, `s22-session-*.txt`); `KEYCODE_MEDIA_PLAY`, `SPACE`, `DPAD_RIGHT` (adb, labelled), `KEYCODE_MEDIA_NEXT` → episode 2 loaded. Case 14 (tablet): spinner during the stall (`tablet-14-stall-3.png`, frozen at 1.167 s), mpv "End buffering (waited 28.7 secs)", then plays on by itself (`-6.png` at 9.8 s) — AY0 had it frozen with no indicator. Process death (PiP off for the test so the process can die; `am kill` confirmed, reopened from the launcher): tablet back at 11.2 s **paused**; S22 first run exposed bug (2) (50.6 s → 0), after the fix back at 13.9 s. Crash buffers empty; PiP pref restored.
- **PiP (later, tablet):** Home during playback → pinned window keeps playing (`.worktrees/ay6/.../tablet-pip.png`), session stays active; Samsung's PiP window shows no title text, so the title is only in the session/system controls. **Not checked:** noisy pause with real headphones (none connected; no broadcast simulated); case 02 far seek; the tablet keyboard cover. No background playback, notification or foreground service in v0.2 — a later card.


#### AY4-F Consume repeat events for playback toggles (T2) — `review` (Claude 2026-11-21, `eebba227b` on `tsuzuki/ay4-f`, on top of `02948285a`)

Keep one playback toggle per physical press. Verify held Space and repeated media toggle events, and preserve repeated seeking where intended.

Result (Claude, 2026-11-21): `AnimeKeyAction.runsOnDown(repeatCount)`: only BACK_10/FORWARD_10 act on repeats; toggle, play, pause, next and previous act once per press. `onKeyDown` still consumes every repeat (so the system never handles a held player key) but performs only when `runsOnDown`. The media-session path already ignores repeats (`MediaSessionCompat` handles play/pause only at `repeatCount == 0`). 6/6 `AnimeMediaSessionTest` (+ held toggle over two presses, held seek). **Tablet** (scratch `ay4fqa` = this branch, AY0 fixture case 01, media-session state from `dumpsys media_session`): playing → held Space (`input keyevent --longpress`) → PAUSED (once; the repeat no longer flips it back) → single Space → PLAYING; held ← from 60 s → 40 s (press + repeat both seek), single ← → 30 s; crash buffer empty. Scratch uninstalled.

#### AY6 Anime: extension ABI audit (T4) — `passed` (self-review under owner waiver 2026-11-22; with AY6-F)

Self-review (Claude, 2026-11-22, **owner waiver**: "Codex doesn't have usage left, you can do the reviews instead"; Claude wrote this card): **PASS.** The extensions-lib `CoroutinesExtensionsKt` / `JsonExtensionsKt` helpers have the same names and signatures. Plain `-keep` rules for them and `HttpException.getCode`, plus `JavaScriptEngine` kept, stop R8 from dropping or rewriting them; the optimized lib 14/16/17 audit passes (Codex trial evidence). `parallelCatchingFlatMap` swallows any `Throwable` per element, as Aniyomi's runCatching does.


Independent pre-review (Codex Batch T, 2026-11-21): Changes requested. APK and source review of `30145611d`. The original checker reported success on the existing optimized Preview APK for an intentionally absent `Video#reviewMissingMember ()V`. `check_apk.py:85` treats a member as present whenever an ancestor is outside the parsed APK. A path to java.lang.Object can therefore hide a missing compatibility method. Resolve inherited members using known API metadata or explicit verified allowlists and fail unproven lookups. Add positive and negative controls and rerun all three ABI baselines. Evidence `.worktrees/review-v02-main/app/build/review-evidence/v02-ay-source-review/abi-negative-result.txt` and `abi-negative-control.txt`. Device acceptance and integration remain pending AY1’s independent review.

**Detailed plan: PARITY §9.6.** PARITY §5 AY6, finding 13. Reflection table of every class/method the `extensions-lib` stubs (14, 16, 17) expose,
checked against the app and against the minified preview dex (`scripts/anime-abi-check.sh`); bridges for misses;
keep rules compared with Aniyomi's.
Result (Claude 2026-11-21): **committed on `tsuzuki/ay6`, status `review`.** Owner approved the download 2026-11-21.
- **Stubs (step 1):** aniyomiorg/extensions-lib tags `14` (`f33cc241…`), `v16` (= `16`, `a776b99d…`; JitPack has no build for plain `16`) and `v17` (`971ce96e…`), JitPack AARs, `classes.jar` read with `javap` only (nothing run). `scripts/anime-abi/dump_api.py` → `expected-lib14.txt` (429 lines), `-lib16.txt` (513), `-lib17.txt` (663); compiler-generated lambda/inline classes and `$annotations` skipped; mangled `Duration` names (`rateLimit-SxA4cEA`) kept.
- **JVM check (step 2):** `AnimeExtensionAbiTest` — every line must exist on the app test classpath with the same descriptor, public/protected, members found through supertypes; one message lists every miss; a broken-expectation test proves it. Passes for 14/16/17.
- **Fixed (step 3):** new `source-api/util/CoroutinesExtensions.kt` (`parallelMap`, `parallelMapNotNull`, `parallelFlatMap`, `parallelCatchingFlatMap` + `…Blocking`, same JVM class/descriptors as lib 16/17; extensions use them to load hosters in parallel — they would have failed with NoSuchMethodError) and `util/JsonExtensions.kt` (`parseAs` ×4). `AnimeExtensionHelpersTest` (3).
- **Not offered** (`not-offered.txt` + `NOT_OFFERED`, with reasons): `torrentutils.*` (lib 17 torrent API; no torrent in v0.2); stub-only no-arg constructors of `DialogPreference`/`Preference`/`PreferenceScreen`; `NetworkHelper(Context)` (the app builds it); lib-14 `Video.component3–6` and the old 6-arg `copy`/`copy$default` (Kotlin can't add same-name components with other return types, and an old `copy` overload would make every `copy()` ambiguous; lib-14 constructors were already bridged).
- **R8 (step 4):** `scripts/anime-abi/check_apk.py <apk>` (Python, not the planned `.sh`; build-tools `dexdump`, follows supertypes, exits 1 with every miss). Minified **preview** APK: first run found **`JavaScriptEngine(Context)` stripped by R8** — a crash for any extension that runs site JavaScript in shipped builds; fixed with a keep rule in `app/proguard-rules.pro` (AGENTS updated); rerun **exit 0** (`scripts/anime-abi/last-preview-check.txt`). Aniyomi's own proguard file was not compared (not downloaded).
- **Step 5:** fixture `fetch` uses `GET`/`awaitSuccess`/`asJsoup`/`parseAs` (earlier commit). **Step 6:** `scripts/anime-abi/README.md` (pinned commits, re-run commands, adding a lib).
- **Tests:** full app suite **1,267, 0 failures**. **Device step not done:** three real lib 14/16/17 extensions — the scratch apps only have AnimePahe (rate-limited); fixture 01/16 played on the tablet earlier with this branch's step-5 build.

Extended independent audit (Codex Batch T, 2026-11-21): The strict descriptor-only audit found 39 missing baseline entries, representing 13 distinct signatures across lib 14, 16 and 17. They are `HttpException.getCode()`, the eight `CoroutinesExtensionsKt` helpers, and the four `JsonExtensionsKt.parseAs` overloads. R8 usage confirms it removed the blocking helpers and `parseAs` methods. The mapping records a rewritten `parallelMap` descriptor. The original checker separately accepted `Video.initialized` even though dexdump marks the field PRIVATE FINAL. A fresh build of `30145611d` passed all four JVM ABI tests and reproduced all 39 minified misses. The stricter visibility test accepted the public `getInitialized()` getter and rejected the private field. Saved original APK SHA-256 `AAABAC91012FA0F770082F49B6981022A15FA823CB0E6ED6C02D3CB42BDA978F`. R8 report excerpts and controls are in `.worktrees/review-v02-main/app/build/review-evidence/v02-ay-source-review/`.

Review experiment (Codex Batch T, 2026-11-21): Three temporary class-specific rules in `ay6-review-keep.pro`, injected only into Preview by `ay6-review-keep.init.gradle`, made all three pinned ABI baselines pass with the stricter checker. The same checker accepted the known public getter and rejected both the existing private field and the nonexistent method. The five experimental Preview variants had byte-identical DEX files, so all share the checked ABI. The APKs passed 16 KB ZIP alignment, and the 25 distinct native libraries across the two 64-bit splits passed the ELF load-segment check. The universal APK contained all four expected ABIs. See `ay6-preview-variant-audit.json`. The named evidence APKs distinguish the exact branch rebuild (`ay6-exact-preview.apk`, SHA-256 `36B4FCADFBE3127E85ECD1ACF0D0F38219E07825D79BCF88C6FC204E4BB3EC5B`) from the keep-rule experiment (`ay6-review-keep-preview.apk`, SHA-256 `96ECFAC9B0C9024EFB641159C14A4C30B806476ECF0DE7F1B28EB8A174220813`). The ordinary rebuild restored the standard Preview output (ARM64 SHA-256 `F3B48DB4139775DA1EC2F4DDCA903DE3414896C617C472C5A1B7EC87C62D0F11`). Its APK hash differs from the archived exact rebuild. All five ordinary APKs passed the same packaging checks and contained no debug fixture markers (see `ay6-standard-variant-audit.json`). Tracked AY6 files are unchanged. Device acceptance and the independent AY1 review remain queued. Evidence `.worktrees/review-v02-main/app/build/review-evidence/v02-ay-source-review/`.

#### AY6-F Keep the extension ABI through R8 (T3) — `review` (Claude 2026-11-21, `3c1db54b0` on `tsuzuki/ay6-f`, on top of `30145611d`)

Independent artifact check (Codex Batch T). Reran the strict checker on the optimized ARM64 Preview with SHA-256 `e12c0e17ea40c6160adb1732ffb599e77217c66c5015bf5e82d20cf876d26718`. All three ABI lists pass (429 / 513 / 663 entries), including the positive and two negative controls. Every member owner also has a separate class check in its baseline. The APK passes 16 KB zipalign. Evidence `.worktrees/ay6/app/build/review-evidence/ay6-independent/`. Runtime compatibility with real lib 14, 16 and 17 sources and release-chain validation are still open.

Make the minified audit fail on missing members even when inheritance leaves the APK, and reject private members. Prove any supported external-supertype exception from explicit API evidence. Use targeted keep rules for the observed missing signatures and verify lib 14, 16 and 17 on a new optimized Preview. Check a known public getter, a known private field, and a nonexistent member. Keep the deliberate exclusions documented.

Result (Claude, 2026-11-21): **`check_apk.py` is strict now.** Members count only when the dex marks them PUBLIC/PROTECTED (class-only lines need a public class); inheritance is followed only through supertypes inside the APK, and a lookup that would continue into a framework/JDK class fails ("can't prove it through …"); the only external members accepted are `equals`/`hashCode`/`toString` (JVM spec). No other external-supertype exception is needed for lib 14/16/17. Every run first checks three controls on `Video` and exits 2 if one is misjudged: `getInitialized ()Z` accepted, the PRIVATE FINAL `initialized Z` and `reviewMissingMember ()V` rejected. Each miss prints its reason. **Keep rules** (`app/proguard-rules.pro`, plain `-keep`, no `allowoptimization`, because `-keep,allowoptimization class eu.kanade.**` keeps classes but lets R8 drop members and rewrite descriptors): `HttpException { public int getCode(); }`, `CoroutinesExtensionsKt { public static *; }`, `JsonExtensionsKt { public static *; }`. **Results:** on Codex's `ay6-exact-preview.apk` the new checker reports the same 39 misses (13 signatures: 36 via `java.lang.Object`, 3 via `IllegalStateException`); on Codex's keep-rule APK and on a **new optimized Preview from this branch** it passes for lib 14, 16 and 17 together and separately (`scripts/anime-abi/last-preview-check.txt`; arm64 SHA-256 `e12c0e17…6d26718`); arm64 and universal pass `zipalign -c -P 16`. JVM: `AnimeExtensionAbiTest` 4/4, `AnimeExtensionHelpersTest` 3/3. README documents the strict rules and the keep guidance; `not-offered.txt` unchanged. Also fixed two max-line-length lints in `AnimeExtensionAbiTest.kt` that stopped spotless on the AY branches. Still open from AY6: three real lib 14/16/17 extensions on a device.

#### REL-0.2 Anime release: chain review, audit, upgrade test from 0.1.1 — `ready` (needs `tsuzuki/ay6` in `review`; review lane)

Prep (Claude, 2026-11-22; mechanical only, not the chain review): on `review/v02-rc-trial` (`.worktrees/v02-rc-trial`), commit `b83c79068` sets **versionCode 26 / versionName 0.2.0** and moves `docs/CHANGELOG-0.2-draft.md` into `CHANGELOG.md` as `[0.2.0]`, without the optional app-chain block (owner decision pending) and without the A11Y1 screen-reader line (A11Y1 is not in the RC). Before tagging, drop changelog lines for any card that fails its review. **Optimized upgrade smoke (Claude, tablet):** R8 Preview of the exact `v0.1.1` tag (versionCode 25, `0.1.1-8853`) installed as scratch `…claudeupgrade` and launched (database created; onboarding stopped at the storage-folder step, which needs the system picker, so no library data), then the RC Preview `b83c79068` installed over it: versionCode 26, `0.2.0-9010`, starts with an empty crash buffer. This does not replace the release-signed upgrade with real data (CI keys, owner) or Codex's synthetic 500-book upgrade. Evidence `.worktrees/v02-rc-trial/app/build/review-evidence/upgrade/`. The RC before this commit (`3baf1bdbe`) passed 1,509 all-module tests, `verifySqlDelightMigration` and `assembleRelease -Penable-updater`, and 16 KB alignment on all 5 APKs.
**Detailed plan: PARITY §9.7.** One reviewer who implemented none of AY9–AY6: `review/v0.2` from main + `tsuzuki/ay6`; every card's device script re-run on the merged build; anime off by default on fresh and upgraded installs; v0.1.1 → v0.2 upgrade with the 500-book synthetic backup; all-module tests, Debug/Preview/Release, 16 KB and ABI checks; CHANGELOG v0.2, versionCode 26 / 0.2.0. Ask the owner whether `review/app-chain` rides along. Owner tags and publishes.
Pre-check (Claude, 2026-11-21, code only, not the release review): `tsuzuki_anime_enabled` defaults to false and its only writer is the Settings › Browse › Anime (beta) switch; the Aniyomi import and anime backup code never set it; a restored Tsuzuki backup carries it only if anime was on when the backup was made. So fresh and upgraded installs start with anime off unless the user restores such a backup; REL-0.2 still verifies it on devices. Database: since v0.1.1, `main` adds migrations 37 (extension_store.media), 38 (anime_sync.mal_id) and 39 (anime_watch_days), which cover every schema change; of the release branches only AY-INT touches a `.sq` file, and it adds queries only (`clearAllHistory`, `refreshAllAnimeLastSeen`); AY9's uncommitted work has no schema change. So no migration-number collisions when the chain merges; the 0.1.1 → 0.2 upgrade runs 37–39. Version: `app/build.gradle.kts` lines 36–37 still read `versionCode = 25`, `versionName = "0.1.1"` (unchanged since the tag); REL-0.2 sets 26 / "0.2.0". Changelog draft: `docs/CHANGELOG-0.2-draft.md` (novel-reader fixes in a separate block pending the owner's decision).

Result: _pending_

**After v0.2 (owner 2026-11-15; do not start before the release):** A6.5, AY5, A6.8, AY7, A6.6, A6.7, AY8.

Trial preparation (Codex, 2026-11-22): `review/v02-anime-full-trial` at `df1d501d8` combines the anime playback chain, AY9 artwork and icon fixes, and AY-INT-F2. Debug, optimized Preview, 1,503 all-module tests, pinned ABI, and five-APK packaging checks passed. The trial keeps versionCode 25. AY1 independent review, live-source and S22 acceptance, official version bump and the release-signed upgrade are pending. A tablet Debug preflight upgraded a local build of the exact v0.1.1 tag (`973307ed2`, version code 25) to the trial code with version code 26 in a disposable package. SQLite schema 37 advanced to 40. Full rows matched for 500 books, 50,000 chapters, 995 history entries, 31 category rows including the system shelf, and 500 shelf assignments. Eight preferences matched, including Colour, and anime stayed off. Tablet Home showed 500 books with the scratch app subsequently removed and the crash buffer empty. Evidence: `.worktrees/v02-upgrade-baseline/app/build/review-evidence/v02-upgrade/run-2a4c5c45fa/`. Evidence: `.worktrees/v02-anime-chain/app/build/review-evidence/ay-int-trial/`.

Packaging preflight (Codex, 2026-11-22): the local Release APKs at trial `260a9e1cd` passed the lib 14/16/17 ABI audit, 16 KB ZIP alignment, all 25 distinct 64-bit ELF library checks, and baseline profile presence. The Release DEX is identical across the five APKs and excludes the debug fixture. The local Release APK still uses version code 25 and the Android Debug certificate. The same trial Preview also passed ABI, ZIP, ELF and baseline profile checks. All five Debug scratch splits passed ZIP and ELF checks. Locale and placeholder validation covered 68 anime player keys and the History clear key across eight locales. Evidence: `.worktrees/v02-anime-chain/app/build/review-evidence/v02-release-preflight/`.

Fresh-install preflight (Codex, 2026-11-22): tablet Debug had a false anime preference before any probe writes. With onboarding bypassed in the scratch app, the actual Dot Home hid Watch by default. Changing the same preference used by Settings revealed Watch and turning it off hid it in the same Home activity. Settings navigation itself was not part of this probe. The app and helper were removed and the crash buffer was empty. Evidence: `.worktrees/ay2-filter-contrast/app/build/review-evidence/v02-fresh-gate/`.

#### AY5 Anime: chapters and source timestamps (T3) — `ready` (needs `tsuzuki/a6.5`; lane 1)
PARITY §5 AY5, finding 12. mpv `chapter-list` + `Video.timestamps` merged with AniSkip; seek-bar ticks; "no AniSkip
when the video has chapters".
Result: _pending_

#### AY7 Anime: offline episodes keep subtitles, audio and options (T3) — `ready` (needs `tsuzuki/a6.8`; lane 1)
PARITY §5 AY7, finding 14. Sidecar with external tracks, `mpvArgs` and quality; support table per container.
Result: _pending_

#### AY8 Anime: restore an Aniyomi backup (T3) — `ready` (needs `tsuzuki/a6.7`; lane 1)
PARITY §5 AY8, finding 15. Check first whether it already works; else read-only decoding of Aniyomi's anime fields.
Result: _pending_

**Notes on the A6 cards below (PARITY §6):** A6.4 uses Aniyomi's `TrackSelect` language matching. A6.5 ports
`ExternalIntents.kt` (extras and result parsing are already worked out there). A6.7 uses the same
`screenshot-to-file` call. A6.8 reads Aniyomi's `LocalAnimeSource` and needs AY1's `content:` support. **A6.9 is
replaced by AY0 + AY1 + AY4; don't take it.**

### Batch T anime cards (written 2026-11-08) — lane 1, after A5.3

All follow the anime lane rules (Needs merge, `TsuzukiAnime.enabled` gate, board first, tokens + typography,
8 locales, per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest`, Result + `review`). A card whose
screen has no part on its board: reuse the nearest existing Tsuzuki layout exactly (named in the card); if none
fits, set `blocked: needs a board` and go to the next card.

#### A4.3a Anime: user setting instead of the debug flag (T2) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (.worktrees/a4.3a, 2026-11-10; needs `tsuzuki/a4.2`; lane 1)
The code half of A4.3 (the device sweep and owner sign-off stay with the owner/RV). `TsuzukiAnime.enabled` reads a
new preference `tsuzuki_anime_enabled` (default **off**) instead of `BuildConfig.DEBUG`; Settings → Browse gets an
"Anime (beta)" switch (existing switch preference widget) with a one-line summary, shown only on arm64-v8a
(A4.2's ABI check); other ABIs show nothing. Turning it on shows the Watch pill/shelves without a restart; turning
it off hides every anime entry point and stops the anime update work, but never deletes anime data. Tests: gate
off → no anime entry point, no worker; on/off/on keeps the library; non-arm64 hides the switch.

**Result (Codex, 2026-11-10):** `195c35eca` adds the ARM64-only, default-off preference.
`b87145ef8` fixes Anime Browse discoverability, the repository screen's back navigation and
crash-process startup, the Library header shift, the History filter, and Aniyomi request linkage.

**Earlier candidate failures confirmed in the tablet crash logs**

Opening Anime repositories crashed in
`AnimeExtensionReposScreen.Content` because `LocalNavigator.currentOrThrow` found no navigator.
The `:error_handler` then repeatedly crashed while `App.onCreate` scheduled the update job without
WorkManager. Opening an AnimeKai episode list also crashed on missing `RequestsKt.get$default`.
These stacks predate the current install and are covered by `b87145ef8`. See `tablet-crashes.log`
and `tablet-extension-linkage-crash.log` in the anime-beta evidence folder. The 2026-11-10 follow-up
launch reached Dot Home without a new crash (`tablet-qa-home-20261110.png`).

- Final JDK 21 Debug, Preview and Release builds passed. 1,044 unit tests passed with zero
  failures or skips. Five Preview, five Release and ARM64 Debug APKs passed 16 KB ZIP alignment.
- Backed up Preview on Galaxy Tab S11 (USB) and Galaxy S22 (wireless ADB) before installing
  ARM64 Preview in place. Verified the Anime repository screen opens on both. On the S22,
  Browse → Anime remained visible without a repo and its empty-state button opened repository
  setup. The Anime Library tab held its position across the shelf switch on both.
- Verified the gate on/off/on in the same tablet process, with the widget provider disabled
  when off and saved anime visible again after re-enabling. Existing book resume survived the
  Preview upgrade. Verified fixture playback with resume, English audio and subtitle selection,
  S22 global search and Watch stats, tablet two-pane details and MP4 offline playback.
- Device evidence, exact APK hashes and pre-install backups are local under
  `.worktrees/a4.3a/app/build/review-evidence/anime-beta/`. Remaining device checks and owner
  sign-off are tracked in the review lane.

#### A6.1 Anime: Kitsu tracker (T3) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (.worktrees/a6.1, 2026-11-10; lane 1)
Board `10-anime/AnimeTracking`; if it has no Kitsu row, copy the AniList row's layout. Reuse the Kitsu client
already in the app (manga tracker: same public client id, login, token refresh); add the anime library
endpoints (search anime, library entry by anime id, update progress/status/score), stored in `anime_sync` like
A3.5. Progress syncs on episode seen. No new secrets. Tests: response parsing fixtures, status mapping, progress
never goes backwards.

**Result (Codex, 2026-11-10):** `9e17de25e` on `tsuzuki/a6.1`.

- `KitsuAnimeApi.kt` uses Kitsu's anime search and account library entries. Small hooks
  in `Kitsu.kt` and `KitsuApi.kt` supply the same authenticated client and account ID as
  manga tracking. No credentials or separate login were added.
- `AnimeTracking.kt` registers Kitsu with the existing `anime_sync` and episode-seen paths.
  It reads the current account entry before manual edits. The anime API only sends increasing
  episode progress, bounded by a known episode count. Status, rewatching, half-point score
  and dates use the same tracking row in `AnimeTrackingSheet.kt`.
- `AnimeTrackApiTest.kt` verifies search parsing, account lookup, create and edit payloads,
  and increasing progress. Public search returned Cowboy Bebop with 26 episodes from the
  existing Kitsu host. JDK 21, scoped Spotless, Debug assembly and all 1,063 unit tests passed.
- Device review: with an already logged-in Kitsu account, check the row in Dot and Colour,
  search and link a permitted fixture, verify its score and status, and mark an episode seen.
  Verify that replaying an earlier episode leaves remote progress at the higher value.
  Authenticated behavior awaits this check.

#### A6.2 Anime: migrate an anime to another source (T3) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (.worktrees/a6.2, 2026-11-10; lane 1)
Like manga migration, for anime: from details (overflow) and from Browse › Migrate (an ANIME pill only when ≥ 1
anime in the library). Layout: reuse `tsuzuki/browse/TsuzukiMigrateSourceList.kt` and the existing migration
search/config screens, no new layout. Carry over seen flags and positions matched by episode number, shelves,
tracker bindings, and the A5.3 group excludes; options "copy" or "replace" like manga. Tests: episode matching by
number (decimals, missing numbers), what each option carries.

**Result (Codex, 2026-11-10):** `152f8b944` on `tsuzuki/a6.2`.

- `AnimeMigrationActivity.kt` reuses anime search results and the migration comparison
  cards to select another source and confirm Copy or Replace. The details overflow opens
  it for a favorite. `TsuzukiAnimeMigrationTab.kt` reuses the source-list layout and sorting
  preferences. `BrowseTab.kt` and `TsuzukiBrowseHeader.kt` expose anime migration only with
  the beta enabled and at least one favorite. The original details reloads after the result.
- `AnimeMigration.kt` matches known episode numbers at source precision, including decimals.
  `AnimeLibraryStore.kt` commits the destination changes and then unfavorites the original
  for Replace in the same transaction. Both options preserve stronger destination progress,
  merge shelves and group exclusions, and copy tracker links when the destination has no
  binding for that provider. Replace keeps the original record, downloads and history.
- Three strings were added in eight locales. JDK 21, scoped Spotless, Debug assembly and all
  1,067 tests passed, including four regressions for matching, merge behavior, the Browse
  gate and the copy/replace transaction. `docs/design/README.md` records the implementation.
- Device review: on a scratch library, use the Details and Browse routes on both devices,
  check the match count and translated actions in Dot and Colour, then Copy and Replace
  across two sources. Confirm the destination resumes correctly and back returns to an
  original details screen whose favorite state reflects the chosen action.

#### A6.3 Anime: duplicate check when adding to the library (T2) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (.worktrees/a6.3, 2026-11-10; lane 1)
Adding an anime whose normalised title (or tracker id) matches one already in the library from another source
shows the existing duplicate dialog pattern used for manga (open existing · add anyway · migrate → A6.2). Tests:
title normalisation (case, punctuation, season suffixes), tracker-id match beats title.

**Result (Codex, 2026-11-10):** `6ef424d63` on `tsuzuki/a6.3`.

- `AnimeDuplicates.kt`, `AnimeLibraryStore.kt` and `anime_sync.sq` prefer a shared
  provider and media ID among favorites on other sources, then compare normalized
  titles. Numbered seasons and stored season numbers keep different seasons separate.
  The lookup uses local data and needs no schema migration.
- `AnimeDetailsScreen.kt` checks before opening the shelf picker. `AnimeDuplicateDialog.kt`
  reuses the themed manga dialog and migration cards. Open goes to the existing entry,
  Add anyway continues to the shelf picker, and Migrate preselects the exact destination
  in `AnimeMigrationActivity.kt`. Existing localized action labels are reused.
- JDK 21, scoped Spotless, Debug assembly and all 1,071 tests passed, including four
  duplicate checks for title normalization, season handling, source filtering and
  tracker precedence. `docs/design/README.md` records the implementation.
- Device review: use a scratch library to verify the warning in Dot and Colour and try
  all three actions. Open the same anime from its original source and a different season
  to verify neither triggers the warning. Check the existing manga duplicate dialog.

#### A6.4 Anime: preferred audio and subtitle languages (T2) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (.worktrees/a6.4, 2026-11-10; lane 1)
Board `10-anime/PlayerSettings` (tracks part; if absent, a row in R-A2's player settings page). Two ordered language
lists (audio, subtitles; subtitles also "off"). On load the player picks the first matching track; when the user
changes a track by hand, remember that choice for this anime only (preference store keyed by anime id, no
migration). Tests: track selection with tags like `ja`, `jpn`, `Japanese`, forced/signs subtitle handling,
per-anime override wins.
Result: _pending_

#### A6.5 Anime: open episodes in an external player (T2) — `claude-working` (Claude 2026-11-22, `.worktrees/a6-5`, `tsuzuki/a6.5` from `tsuzuki/ay6-f` `3c1db54b0`; lane 1; see Batch AY notes)
A PlayerSettings row "Play with": Tsuzuki · external app. External: `ACTION_VIEW` with the resolved stream URL,
type `video/*`, the hoster's headers and title as the extras mpv-android, VLC and MX Player read; resume position
passed in and, when the app returns a position (MX/mpv), saved as progress and seen at ≥ 90 %. No external app →
snackbar, play in Tsuzuki. Tests: intent extras per player, position result handling.
Result: _pending_

#### A6.6 Anime: next-episode airing times from AniList (T3) — `ready` (needs `tsuzuki/ay7`; lane 1)
For anime bound to AniList (A3.5), read `nextAiringEpisode { episode airingAt }` in the existing tracker refresh
(no extra polling: at most once per anime per library update). Show "EP n · in 2d" on the details header and use the
real time in R-A3's "Airing today", replacing the estimate only when AniList has a time. Unbound anime keep R-A3's
behaviour. Hide when unknown. Tests: parsing, "today" in the device time zone, estimate vs real precedence.
Result: _pending_

#### A6.7 Anime: player screenshot (T2) — `ready` (needs `tsuzuki/a6.6`; lane 1)
A camera action in the player options sheet (R-A2 `AnimePlayerSheets.kt`): mpv `screenshot-to-file` (video only,
no subtitles toggle) into the cache, then MediaStore `Pictures/Tsuzuki/`, snackbar with Share. No storage
permission on Android 10+. Tests: file naming (title, episode, timestamp), MediaStore insert on IO.
Result: _pending_

#### A6.8 Anime: local anime source (T3) — `ready` (needs `tsuzuki/ay5`; lane 1; see Batch AY notes)
Like the local manga source: `<storage>/localanime/<Title>/<episode files>` (mp4, mkv, webm), optional `cover.jpg`
and `details.json` (same keys as local manga). Episode number from the file name; sidecar `.srt`/`.ass` with the
same base name become subtitle tracks. Shows in Browse › Watch only when the folder exists. Reads on IO via the
existing storage-access helpers. Tests: file-name → episode number, subtitle pairing, empty folder.
Result: _pending_

#### A6.9 Anime: playback robustness pass (T3) — `done` (replaced by AY0 + AY1 + AY4, Claude 2026-11-12; do not take)
No new UI beyond existing snackbars/error states. Cover: hoster returns no video → next hoster, then the error state
with Retry and "Open in WebView"; network lost mid-stream → pause with a retry snackbar, keep the position; mpv
init failure → error state, never a crash; process death in the player → reopen the same episode and position
(A3.2 already restores; add the missing cases you find). Add a fake-source test harness for these paths. Report
every case as a table: case → behaviour → test.
Result: _pending_

### Batch S — after v0.1.0 (Claude, 2026-11-08)

The v0.1 freeze is over. Two Codex threads, one per lane; never two cards that list the same files. Each card:
worktree from **current `main`**, merge the branch in its Needs (if any), board first (Dot, then Colour), tokens +
`MaterialTheme.typography` only, real data only, strings in all 8 locales, tests named in the card, per-file Spotless
twice, `:app:assembleDebug :app:testDebugUnitTest`, Result + commit on `tsuzuki/<card>` + `review`, stop. Card
format: WORKPLAN §0.4. Upstream files: small dispatch hooks only (AGENTS.md "Keep Tsundoku mergeable").

**Lane 1 prompt (anime):** `Read AGENTS.md and docs/HANDOFF.md. You are the anime lane: finish A3.5 if it is still
yours, then take the next ready card in the "Anime lane order" table, in order.`

**Lane 2 prompt (app):** `Read AGENTS.md and docs/HANDOFF.md. You are the app lane: take the next ready card from
the Batch S app order, in order.`

**App lane order:** V7.6 → V7.7 → R12 → R16 → R17 → R15 → S1 → S2 → S3 → R13 → R14 → S4. (R13, R14 and S4 are T4;
take them only when the earlier ones are in `review`.)

#### R-A5 Anime chain: upstream-diff fixes from Claude's review (T3) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (needs `tsuzuki/a3.5`; lane 1)
Claude reviewed A2.4 → R-A3 on 2026-11-08: 985 tests pass and release builds hide anime, but these must change
before the chain merges into `main`. Start from `main`, merge `tsuzuki/a3.5` (it contains the whole chain).
1. **`data/library/LibraryUpdateJob.kt`** (chain diff +7/−25): Tsundoku's own update code was moved out of the file.
   Put upstream's code back exactly as on `main`, and add **one** hook line that calls a Tsuzuki function in
   `tsuzuki/anime/` for the anime part. Target: ≤ 5 changed lines.
2. **`presentation/updates/UpdatesScreen.kt`** (chain diff +93/−85): move the anime / "Airing today" UI into
   `presentation/tsuzuki/updates/` and dispatch at the top of the upstream composable (the `HistoryItem` pattern).
   Upstream body unchanged; target ≤ 15 changed lines, no re-indent. Same check for `UpdatesViewModel.kt`.
3. **Migration numbering:** `36.sqm` (anime tables) already shipped in v0.1.0. `37.sqm` (repo media flag, A3.3) has
   not. Add a short "Tsuzuki-owned migrations" table to `docs/UPSTREAM.md` (36 = anime tables, shipped 0.1.0;
   37 = extension repo media flag, unshipped) with the rule: before each Tsundoku merge, compare migration numbers;
   a clash on an **unshipped** Tsuzuki migration is fixed by renumbering ours above theirs. No schema change here.
4. Confirm no anime string or resource files are left in `app/src/debug/res/values*` (only `colors.xml` today).
Tests: existing anime update + Updates tests still pass; add one test that the hook is a no-op when
`TsuzukiAnime.enabled` is false. Result must list the final `git diff --stat main` for those upstream files.
Result: _pending_

#### S1 Manhwa: classify long-strip comics (T3) — `blocked` (note 2026-11-19: this card's branch commits are already in `main`; check whether the block still applies) (2026-11-08, `.worktrees/s1`; lane 2)
Boards `09-manga/LibraryMixed` (novels · manga · manhwa) and `09-manga/MangaDetails` ("auto webtoon"). Mark a
manga as manhwa/long-strip from real data only: source genre/tags (manhwa, manhua, webtoon, long strip), the
source's language hint, and, once chapters are read, the measured page aspect ratio (height ≥ 2.5 × width on most
pages). Store the result in a Tsuzuki preference-backed store keyed by manga id (no DB migration); a manual
override on the details screen wins. Use it for: a Manhwa pill on the Library Manga tab, the details "auto webtoon"
line, and the default reading mode (webtoon) for manhwa when the user hasn't set one. Pure classifier + tests (tags,
aspect ratios, override precedence). Claude device-checks with a known manhwa from Keiyoushi on the S22.
Result: `fc654bd76`. Two per-file Spotless passes (app and core/common) and offline JDK 21
`:app:assembleDebug :app:testDebugUnitTest` passed (974 tests, including 6 new).

- `ManhwaClassifier.kt` uses exact format tags, reader measurements and a weak Korean/Chinese source-language
  hint, in that order. A manual classification wins. A measurement verdict requires a majority of distinct
  chapter pages, at an inclusive 2.5:1 height-to-width threshold.
- `ManhwaStore.kt` persists verdicts and overrides keyed by local manga ID. The preferences stay local because
  backup restores can assign different IDs. `AppModule.kt` registers the shared store.
- `ManhwaReader.kt`, `ImageUtil.kt`, `ReaderChapter.kt` and both page holders measure bounds off the UI thread
  from bytes already loaded for the reader. `ReaderViewModel.kt` defaults classified comics to Webtoon when
  the title has no explicit reader mode.
- `LibraryViewModel.kt` and `LibraryItem.kt` apply classification before grouping and selection. `LibraryTab.kt`,
  `NovelsTab.kt`, `TsuzukiLibraryMediaSwitch.kt` and `TsuzukiMixedLibraryHeader.kt` expose a Manhwa-only subset.
  The existing Manga filter includes all comics. `TsuzukiMixedLibraryGrid.kt` adds badges.
- `TsuzukiMangaDetails.kt` shows auto Webtoon and offers format overrides alongside the reader-mode picker.
  Eight locale files provide labels. `ManhwaClassifierTest.kt` covers tags, language, page ratios, duplicate
  pages, and override precedence. `docs/design/README.md` records the board implementation.

Claude device review: on S22, favorite a known Keiyoushi manhwa and a paged manga, check the Manhwa filter,
badge, and reader default in Dot and Colour, then force Manga and return to Automatic. Verify an explicit
per-series reader mode still wins, and check the mixed-grid badge on the tablet.

**Review (Codex RV2, 2026-11-10)**

The tagged fixture filtered correctly on the tablet. The live Keiyoushi, manual override, and explicit reader-mode checks remain open.

**Review (Codex RV2, 2026-11-13):** Deferred, not a code failure. The required live Keiyoushi check could not run: automatic approval review rejected confirming trust for the existing Asura Scans extension because it can execute arbitrary code and access credentials. Explicit owner approval is pending; no trust was granted. S1 is excluded by `dab21422c`. After consent, reinstate by reverting that exclusion on a review branch, then check a live long-strip title and manual override. S22 is deferred by the owner; use the tablet for now.

#### S2 Listening stats (T2) — `blocked` (note 2026-11-19: this card's branch commits are already in `main`; check whether the block still applies) (2026-11-08, `.worktrees/s2`; lane 2)
Board `07-system/Stats` (Dot: `07-system/dot/Stats`). Owner Q4 keeps reading and watch time apart; listening counts
with reading. Count TTS minutes from real playback (`TtsController` start/stop/pause, chapter hand-offs) into the
existing reading-days store as a separate "listened" field (no DB migration), and show the board's listen row on
Stats (minutes today / this week, chapters listened). Hide the row until there is data. Tests: minute accounting
across pause, sleep timer, chapter hand-off, and a flush on pause (process death).
Result: `3a49d8ac7`. Two per-file Spotless passes and offline JDK 21
`:app:assembleDebug :app:testDebugUnitTest` passed (975 tests, including 7 new).

- `ListeningSession.kt` measures utterances with a monotonic clock, splits time at local midnight and flushes
  on stop, pause and chapter completion. It keeps speaking time and foreground visual time disjoint.
- `TtsController.kt` identifies each playback queue so callbacks from an interrupted queue cannot count
  toward a new one. `NovelViewer.kt` and `NovelWebViewViewer.kt` use the same session.
- `ReaderActivity.kt` flushes when the reader loses visibility, including during background TTS.
  `ReaderViewModel.kt` records visual time only while the reader is visible and preserves existing history
  duration and incognito behavior.
- `ReadingDays.kt` reads the original daily totals and stores new listening duration and chapter counts in
  the same preference. Goals, chart and streak count both forms of reading. A listened chapter is counted
  when its final utterance completes, including when playback started partway through the chapter.
- `TsuzukiStatsScreen.kt` shows listening minutes today and this week and completed chapters this week
  after playback is recorded. Eight locale files add labels and `docs/design/README.md` records the tile.
- `ListeningSessionTest.kt` and `ReadingDaysTest.kt` cover pause, sleep-stop accounting, handoffs, persistence,
  foreground/background separation, midnight, incognito, and older records. `TtsSleepTimerTest.kt` still
  exercises the controller's real timed stop with a test scheduler.

Claude device review: on tablet and S22, play both novel renderers, pause and resume, try the sleep timer and a
chapter handoff, then check the Stats tile in Dot and Colour. Check that background TTS adds listening time
once, and that incognito playback leaves the totals unchanged.

**Review (Codex RV2, 2026-11-10)**

The tablet recorded one completed listening chapter. An incognito replay left the total unchanged. Background playback, timer, pause/resume and chapter handoff still need device checks.

**Review (Codex RV2, 2026-11-13):** Fail: RV2-14 remains open. The current implementation still identifies queues rather than a chapter within a playback run; the earlier device fixture counted five chapters for a two-chapter book. Excluded by `591429e49`; S2-F stays ready. Build S2-F from the new main and revert `591429e49` there to restore the original S2 changes before fixing them; merging tsuzuki/s2 alone will not restore a reverted ancestor. This chat may implement S2-F, but another chat must review it.

#### S3 Chapter recap at the start of a chapter (T3) — `done` (2026-11-08, `.worktrees/s3`; lane 2)
Board `06-reader/ChapterEnd` (recap block). Offline and extractive only (no AI, no network): when a novel chapter is
opened after ≥ 24 h away from that book, show a dismissible "Previously" card above the text with the last 2–3
paragraphs of the previous chapter (from downloaded/cached text; hide when not available). Reader setting, default
on. Both renderers (TextView + WebView; WebView via the `chapter-end-card.js` asset pattern). Tests: paragraph
picking (skips author notes such as "A note from …", blank and very short paragraphs) and the 24 h rule.
Result: `ba61418a0`. Two per-file Spotless passes, JavaScript syntax checks and offline JDK 21
`:app:assembleDebug :app:testDebugUnitTest` passed (973 tests, including 5 new).

- `NovelRecap.kt` selects the last two or three prose paragraphs, strips markup, and skips short text and
  trailing author notes. `NovelRecapTest.kt` covers HTML, plain text, note filtering and the exact 24 h threshold.
- `NovelRecapSession.kt` reads existing history and local chapter text. It can reuse in-memory pages, downloaded
  chapters, a local EPUB or up to 64 cached excerpts. The cache stores only the extracted paragraphs in the app's
  cache directory. Recap preparation and cache writes skip incognito. Lookup makes no source requests.
- `ReaderViewModel.kt` prepares the first chapter's recap before the reader stamps its new history entry.
  `ReaderPreferences.kt` and `SettingsNovelReaderScreen.kt` add the default-on switch. Eight locales add labels.
- `TsuzukiNovelRecapCard.kt` renders the native card and supplies a safe WebView payload. `NovelViewer.kt`
  places and removes the card with its chapter block. `NovelWebViewViewer.kt`, `chapter-recap-card.js` and
  `scroll-tracking.js` handle insertion, dismissal, TTS exclusion and continuous-scroll position.
  `docs/design/README.md` records the recap scope.

Claude device review: reopen a novel after 24 h with a previous offline chapter and check the card and dismissal
in both renderers and themes. Check disabled, no-cache and first-chapter cases, and try WebView paged mode and
TextView infinite scroll. A mid-chapter resume keeps its reading position and the card remains at the chapter top.

**Review (Codex RV2, 2026-11-10)**

Hold for RV2-03, RV2-07 and RV2-12. The cold local case reproduced on the tablet, and the paged resume shift reproduced in Chromium.

**Review (Codex RV2, 2026-11-13):** Pass on the tablet. Fixes `ea748689e`, S3-F merge `cdaca0445`, and style correction `286d18b7b`: hard-wrapped local prose remains in its paragraphs; cold local recap appears in TextView, WebView scroll and WebView paged; dismissal works. Paged resume at 50% returns to the same paragraph with the recap (3/5 pages) and without it (2/4). Reader CSS no longer overrides Dot recap font or red dismiss ink. Browser fixtures pass all three sizes and fail without the style fix. Evidence: `app/build/review-evidence/rv2/*recap*20261113.png`, `paged-resume-before-close`, `paged-resume-final-settled`. S22 deferred by owner.

#### S3-F Preserve chapter text and position when showing a recap (T3) — `done` (.worktrees/s3-f, 2026-11-10; app lane)

Use the ChapterEnd board. Read the previous local chapter through the source's offline text path. Keep hard-wrapped lines within their paragraphs. Make paged WebView restore the same story text with or without the card. Add regression coverage for local data with no excerpt cache, wrapped paragraphs and resume after the recap appears or is dismissed. Verify the card and dismissal in both Android renderers.

**Result (Codex, 2026-11-10):** `c3029ea69` on `tsuzuki/s3-f` addresses RV2-12. Based on
`tsuzuki/s3` so the review lane can cherry-pick it onto `review/rv2` alongside Claude's
local-chapter and hard-wrap edits (RV2-07 and RV2-03).

- `NovelWebViewDocumentBuilder.kt` gives the recap its own opening column(s) in paged mode.
  `paged-reader.js` measures that offset and saves/restores progress using story pages.
  `chapter-recap-card.js` repaginates immediately on dismissal.
- `scripts/test-paged-recap.cjs` reproduces the old shift and verifies the visible lines at 60%,
  page turns, end-of-chapter landing and dismissal at phone, tablet and landscape sizes.
  `docs/design/README.md` records the change.
- JDK 21, two scoped Spotless passes, Debug assembly and all 973 unit tests passed. The
  Chromium fixture fails on S3 at the resume-lines assertion and passes all three sizes on S3-F.
- RV2 device check after integration: open the cold-cache local fixture in TextView and WebView,
  verify the excerpt preserves paragraph boundaries, and dismiss it. In paged WebView reopen
  from the same saved mid-chapter progress with the recap eligible and dismissed. Compare the
  visible story lines. Also open at 0%, turn through the recap and dismiss it at the start.
  Check Dot and Colour on the S22 and tablet. The local-source path still needs the Android retest.

#### S4 Neural TTS — step 1: engine + listening test, debug only (T4) — `blocked` (note 2026-11-19: this card's branch commits are already in `main`; check whether the block still applies) (RV4 deferred, 2026-11-13)
Research is done: `docs/research/neural-tts.md` (GO-WITH-CONDITIONS). The conditions are hard rules: sherpa-onnx
≥ v2.0.1 **without espeak-ng** (GPL); **lexicon voices only** (Kokoro int8 multi-lang v1.1), the pack re-zipped
**without `espeak-ng-data/`**; system TTS stays the default engine; 16 KB alignment check on the new `.so` files;
licence texts in `docs/licenses/`. This card: a second engine behind `TtsController` (system engine unchanged),
synthesis on a background thread one ~150–200-char chunk ahead (reuse `TtsTextUtils` chunking), and a
**debug-only** developer entry that loads a voice pack from app files and plays a chapter. No download UI and no
voice picker yet (no board; Claude draws them after the owner's listening test). Codex never downloads the model;
Claude side-loads it for the test. Tests: chunk queueing, stop/pause/seek, fallback to system TTS on load failure.
**Result (Codex, 2026-11-09):** `tsuzuki/s4` at `b68d437e1`.

- `TtsController.kt` and `TtsTextUtils.kt` let the Debug opt-in use the existing Listen, highlight, stop, pause, resume and seek path with a 180-character limit. Android TTS is the default and receives the interrupted chunk on failure.
- `NeuralTtsClient.kt`, `NeuralChunkQueue.kt` and `KokoroTtsService.kt` send chapter chunks to a private Debug process, prefetch one chunk on a worker, and play its PCM with AudioTrack. `App.kt` bypasses database and app setup in the worker process. `DebugInfoScreen.kt` adds the opt-in in eight locales.
- `app/build.gradle.kts` packages the official Sherpa ONNX 2.0.1 AAR and licence assets in Debug only. The published SHA-256 matches the working copy and committed blob. `.gitattributes` prevents Git newline conversion. `docs/licenses/neural-tts/` contains runtime and model licences and ONNX notices.
- `scripts/package-kokoro-test-pack.py` creates an allowlisted zip from an owner-supplied archive. No model was fetched. `docs/research/neural-tts.md` gives the side-load path and listening steps.

Verification: JDK 21, two scoped Spotless passes, `:app:assembleDebug :app:testDebugUnitTest` (981 tests, 0 failures, errors or skips). Queue tests cover one-ahead synthesis, stop/restart/seek, initial and prefetch failures and surrogate-safe chunking. The pack fixture excludes eSpeak data. All five Debug APKs pass `zipalign -c -P 16 -v 4`, and the packaged Sherpa/ONNX ELF load segments pass 16 KB alignment. The AAR scan found no checked eSpeak API or phontab markers. The Gradle audit confirms the AAR and service are absent from Preview and Release runtime dependencies and merged manifests.

Device review: with a backed-up S22 Debug install, first try the opt-in with no pack and confirm system fallback. Side-load the owner-supplied pack, reopen the reader, and try Listen in both renderers. Check pause/resume, paragraph seek, chapter advance, sleep timer and proper names. The prototype uses US English speaker 0 and the existing speech-rate preference. Real synthesis, sound quality and device timing await the listening test.

Review (Codex RV4, 2026-11-13): Deferred by NT1 gate. Exact pinned AAR SHA-256 matches research; all 16 native libraries have 0x4000 PT_LOAD alignment, Debug/Preview zipalign passes, Debug notices present and Preview has neither neural libraries nor service. Tablet opt-in default-off verified; opt-in with no pack logs missing model in private worker and falls back to Android TTS (highlight/Pause). No model downloaded. Literal espeak/piper grep has legacy token-conversion names, warning text/config names, and Speaker false matches; requested clarification rather than waiving the explicit rule. Excluded by `911a2222f`; revert exclusion to resume. Evidence `app/build/review-evidence/rv4/s4-native-audit-20261113.json` and TTS logs/screenshots after archive.

#### A5.1 Anime: picture-in-picture (T3) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (.worktrees/a5.1, 2026-11-10; lane 1; v0.3)
No board needed (system PiP window). The player enters PiP on Home/recents while playing (setting, default on),
keeps playing, offers play/pause and ±10 s remote actions, returns to the full player on tap, and pauses when the
PiP window is closed. Respect `TsuzukiAnime.enabled`. Tests for the action set and state restore.

**Result (Codex, 2026-11-10):** `098e584f0` on `tsuzuki/a5.1`, based on the committed A4.3a branch.

- `AndroidManifest.xml`, `AnimePlayerActivity.kt` and `AnimePictureInPicture.kt` enable the
  system window on supported devices. Android 12+ uses auto-entry, and older versions use the
  user-leave callback. The private actions offer play/pause and 10-second seeks. A new playback
  request reuses the activity's task and releases the previous player before opening the new one.
- `AnimePlayerModel.kt` holds playback while the window remains visible and pauses on stop.
  `AnimePlayerContent.kt` keeps the video surface mounted and hides the full controls in PiP.
  The existing episode, position and pause state still drive recreation.
- `AnimePlayerSettings.kt`, `AnimePlayerSettingsScreen.kt` and eight locale files add the
  default-on switch to the Video tile. A switch turned off while pinned prevents later automatic
  entry and keeps the current window's controls available.
- JDK 21, scoped Spotless and `:app:assembleDebug :app:testDebugUnitTest` passed. All 1,049
  tests passed, including five new preference and action-policy tests. The merged Debug manifest
  declares PiP on the private player activity. `docs/design/README.md` records the change.
- Device review: on an ARM64 device with Anime enabled, play a fixture and try both Home and
  Recents. Check play/pause, both seeks, expand to the same position, close while playing, and
  start another episode while pinned. Repeat with the switch off and with a paused video. Verify
  the switch fits in Dot and Colour, and check that turning Anime off closes an active player.

#### A5.2 Anime: seasons grouping (T3) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (.worktrees/a5.2, 2026-11-10; lane 1; v0.3)
Owner Q3 kept the four season columns and ignored them in v1. Board `10-anime/AnimeDetails` (seasons row). When a
source or an Aniyomi import fills `parent_id` / `season_number`, show seasons as a row on the parent's details and
group child entries under the parent in the anime library (setting "Group seasons", default on). No schema change
(the columns exist). Tests for grouping and orphaned children.

**Result (Codex, 2026-11-10):** `b9766a725` on `tsuzuki/a5.2`.

- `AnimeLibraryStore.kt` reads season lists through the existing v16/v17 source APIs and
  persists child links and order while retaining favorites. `animes.sq`, `AnimeRepository.kt`
  and `AnimeRepositoryImpl.kt` expose the existing relationships in season order, without
  a schema migration. Aniyomi imports use the same relationships.
- `AnimeDetailsScreen.kt` shows season pills using real numbers or source titles, with the
  current season selected among siblings. `TsuzukiAnimeLibrary.kt`, `AnimeSeasons.kt` and
  `AnimeShelvesScreen.kt` apply the default-on grouping switch to the current shelf. Search
  still shows individual titles. Orphans, cross-source children and malformed cycles stay visible.
- Eight locale files provide the labels. `AnimeDatabaseTest.kt` exercises discovery from a
  source details response, ordering and favorite retention. `AnimeSeasonsTest.kt` covers
  grouping and the preference. JDK 21, scoped Spotless, Debug assembly and all 1,054 unit
  tests passed. `docs/design/README.md` records the implementation.
- Device review: open a series with source-linked or imported seasons in Dot and Colour.
  Tap the season pills, check the selected sibling and its own episodes, and compare the
  library with the switch on and off. Check an orphan, a season on another shelf and search.

#### A5.3 Anime: fansub / group filter (T2) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (.worktrees/a5.3, 2026-11-10; lane 1; v0.3)
Owner Q5 left this out of v1. Board `10-anime/AnimeDetails` (groups). A per-anime exclude list of fansub groups
(`episodes.scanlator`), like manga's excluded scanlators: filter sheet on details; excluded groups hidden from the
episode list, Updates and next-episode logic. If this needs a new table, stop and set `blocked` (Claude writes the
migration). Tests for filtering and next-episode.

**Result (Codex, 2026-11-10):** `ea92e39eb` on `tsuzuki/a5.3`.

- `AnimeEpisodeGroups.kt` and `AnimeLibraryStore.kt` save per-anime exclusions in reserved
  metadata, protect that key from source writes, and keep unlabelled releases visible.
  `episodes.sq` adds read columns for both Updates lists. No migration or episode deletion.
- `AnimeDetailsScreen.kt`, `AnimeDetailsScreenModel.kt` and `AnimeEpisodeTiles.kt` add
  the group control and list to the adaptive episode filter sheet. Eight locales supply
  the label. `AnimePlayerModel.kt` applies exclusions to the episode picker and navigation
  and observes changes during playback.
- `AnimeEpisodeGroupsTest.kt` and `AnimeDatabaseTest.kt` cover matching, an excluded
  current episode, filter persistence and both Updates lists. JDK 21, scoped Spotless,
  Debug assembly and all 1,058 unit tests passed. `docs/design/README.md` records the change.
- Device review: on S22 and tablet, open an anime with multiple release groups, exclude one,
  and verify Details, Updates and Airing, then try Next/autoplay through the excluded release.
  Clear the filter and check that the same releases return. Check Dot and Colour and that
  a source with no group labels does not show the group control.

#### A5.4 Anime: Simkl tracker (T3) — `blocked` (owner: register a Simkl API client; then needs `tsuzuki/a5.3`)
Board `10-anime/AnimeTracking` (Simkl row). Needs a Simkl client id/secret registered by the owner and stored as
build secrets, never in the repo. Then: OAuth login, search/bind, episode progress sync on seen, stored in
`anime_sync` like AniList/MAL (A3.5). Torrent sources and torrent player settings stay out (owner has not asked).
Result: _pending_

#### U1 In-app updater end-to-end (Claude) — `blocked` until 0.1.1 is tagged
Install v0.1.0 from the release on the tablet (release app id), tag 0.1.1, then confirm the daily check and Settings
› About › Check for updates both find 0.1.1, the notification opens `NewUpdateScreen`, Download installs in place,
and Skip this version hides it. Record in V8/V12.

### Overnight batch Q — run rules (2026-11-06 night; overrides the batch Q "How to run" and prompt below)

**Prompt the owner pastes into ONE Codex thread:**

```text
You are working on Tsuzuki (Android, Kotlin, Jetpack Compose) overnight, alone. Read AGENTS.md, then
docs/HANDOFF.md section "Overnight batch Q — run rules", and follow it exactly: do the cards in the order of its
table, one at a time, until the table is finished or you run out of usage. Before each card re-read
docs/HANDOFF.md (Claude edits it during the night) and skip any card that is not `ready`. Never stop at a
blocked card: record it and move on to the next one.
```

**Design first:** every UI card in this table has a board (batch Q: `fixes-2026-11/README.md`; anime and
X/Y: named in the card or `docs/design/boards/INDEX.md`). If a card's screen has **no** board, don't invent one:
set it `blocked` ("needs a board") and move on — Claude draws it.

**Rules (every card):**
1. **Re-read this file** (root checkout `docs/HANDOFF.md`) before each card. Take the next `ready` row of the
   table. Set its card heading status to `codex-working (.worktrees/<id>, 2026-11-06)` — in the **root
   checkout's** `docs/HANDOFF.md`, never in the worktree copy. Only status + Result are yours to edit here.
2. **Worktree from the current `main`** (Claude merges reviewed cards into main during the night, so main
   moves): `git -c core.longpaths=true worktree add -b tsuzuki/<id> .worktrees/<id> main`, copy
   `local.properties`. Then for every card in the row's **Needs** column that is not yet in main
   (`git merge-base --is-ancestor tsuzuki/<need> main` fails), run `git merge --no-edit tsuzuki/<need>` in your
   worktree. A merge conflict there → `git merge --abort`, set the card `blocked` ("needs Claude to merge
   <need> first"), go to the next row.
3. **Before UI work** open the card's boards: `docs/design/boards/fixes-2026-11/README.md` (board → card),
   `png/<Board>.png` + `png/Colour<Board>.png`, numbers in `<Board>.dc.html`, bug shots in `evidence/`. Dot
   first, then Colour. Extra-wave cards name their boards in `docs/design/boards/INDEX.md`.
4. **Build:** JDK 21 exactly as AGENTS.md "Build" says (on this machine `/c/Program Files/Android/Android
   Studio/jbr`). At most **one** Gradle build of yours at a time. Follow AGENTS.md Rules + WORKPLAN appendix
   "standard rules for restyle cards": tokens + `MaterialTheme.typography` only, Dot uppercase + red, real data
   only, strings in all 8 locales, small dispatch hooks in upstream files (list every upstream file you touch
   in Result — Claude adds it to AGENTS.md), new code in `tsuzuki` packages, tests for logic you add.
5. **Done =** per-file `spotlessApply -PspotlessIdeHook=<abs path>` twice on every file you touched (never the
   whole project), `./gradlew :app:assembleDebug :app:testDebugUnitTest` green.
6. **Commit on the card's own branch** (owner approved 2026-11-06 for tonight):
   `git add -A -- . ':!docs/HANDOFF.md' ':!AGENTS.md' ':!docs/WORKPLAN.md'` then
   `git commit -m "<id>: <what and why>"`. Owner is the author; **no Co-Authored-By / Generated-with
   trailers**. Never commit to or merge into `main`, never push, never rebase another card's branch, never use
   adb or devices.
7. **Result** (under the card, root checkout): branch + commit hash, files + one line each, upstream files
   touched, decisions, what you left out and why, test count (`N tests, 0 failures`), exactly what Claude must
   check on the phone/tablet. Then set the card `review` and go straight on to the next row.
8. **Stuck > 45 min or build can't go green** → `blocked` + the exact question/error in Result, commit
   nothing, `git -C .worktrees/<id> reset --hard && git -C .worktrees/<id> clean -fd`, next row.
9. Never touch: release/signing/telemetry files, `app/google-services.json`, `.github/`, other cards'
   worktrees, `.worktrees/anime-spike`, DB schema (`*.sq` / `*.sqm` migrations). Q4's app-start migration is
   data-only (no schema change) and is the one allowed data migration.
10. AGENTS.md / WORKPLAN changes a card asks for (Q12's theme rule): write the exact new text into Result;
    Claude applies it.

**Order (one thread, top to bottom):**

| # | Card | Needs (merge first if not in main) | Why here |
|---|---|---|---|
| 1 | Q2 Novel reader next chapter | — | Only broken-function bug; doesn't overlap Q1, so it starts before Q1 is merged |
| 2 | Q5 Navigation Library tab | — (Q1 merged) | Continue on the existing `.worktrees/q5` (`tsuzuki/q5`, rebased by Claude); commit there on top of the wip commit |
| 3 | Q3 Save chapters number range | — | Details download sheet + chapter-list range select |
| 4 | Q6 Book details header | Q3 | Same details screen files as Q3 |
| 5 | Q4 Two Default shelves | — | Import fix + one-time data migration (Claude runs it on a device only after a backup) |
| 6 | Q7 Browse tabs + source rows | — | Browse header only |
| 7 | Q12 Remove other themes | — | Must land before Q11 (one page-colour list) |
| 8 | Q11 Reader settings redesign | Q12 | Uses Q12's page-colour list; also touches `NovelPage.kt` |
| 9 | Q9 Accessibility + QoL | Q3, Q5 | Range select (Q3) and nav labels (Q5) |
| 10 | Q8 Overflow + large-font sweep | Q2, Q3, Q5, Q6, Q7, Q11 | Touches every screen — last of batch Q |
| 11 | X1 Reader hook consolidation | Q2 | Extra wave |
| 12 | X3 Restyle check: ReplaceRules + SettingsTranslation | Q11 | Extra wave |
| 13 | X4 Restyle check: DuplicateFinder + Migration | — | Extra wave |
| 13a | Q13 Novel next chapter: instant, never a blank page | — | Owner report 2026-11-07 (lag before next chapter) |
| 13b | Q14 Design fixes from Claude's 2026-11-07 device pass | — | Owner asked for a bug pass of everything built overnight |
| 14 | A1.5 Anime: debug extension install → play | — | Anime lane (owner asked tonight); only new files + a debug entry |
| 15 | A2.2 Anime tables + data layer (migration 36) | — | T4; schema exception for this card only (rule 9) |
| 16 | A2.3 Anime sources + browse catalogue | A1.5, A2.2 | |
| 17 | A2.4 Anime library + Watch tab + anime shelves | A2.3 | |
| 18 | Y1 Mihon skipped fixes, top group | — | Wave Y |
| 19 | Y2 Tablet manga two-page spread | — | Wave Y |
| 20 | Y3 Manga details + Browse manga boards | Q6 | Wave Y |
| 21 | Y4 Synthetic 500-book backup generator | — | Wave Y, no app code |
| 22 | Y5 Restyle checks: Tracking, Quotes, ExportEpub, Upcoming, EditNovel | Q6 | Wave Y |
| 23 | Y6 Tablet TTS panel + foldable | Q11 | Wave Y |

X2 (tablet Updates two-pane) was dropped: Claude saw it already built on the tablet 2026-11-06.

**Optional second Codex thread (anime lane):** the anime rows (14–17) touch only new anime files, a debug entry
and the new migration, so the owner may run them in a **second** thread at the same time. Prompt: same as above
but "do only rows 14–17 of the table, in order". **If a second thread is open, thread 1 skips rows 14–17
entirely** (claimed or not). Both threads: one Gradle build each at most (with two threads, Claude's overnight
reviews mostly wait for the morning because of the two-build limit).

Q1 is Claude's (review/merge tonight). Q10 was done by Claude (AGENTS.md Build + device table).

### v0.1 release — every card (written 2026-11-07 by Claude; owner: v0.1 ships without anime)

**Critical path:** V0 (owner, start now) · V1 → R6 + R7 → V2 upstream merge → **freeze** → V3–V6 (Codex) →
V7 sweep + V8 never-seen screens → fixes → V9 perf → V10 signing check → V11 release candidate → V12 tag + publish.
**Freeze (from V2 until V12):** `main` takes only v0.1 cards and fixes found by V7/V8/V11. Everything else (anime lane,
R8/R10 reworks, R11–R17) keeps committing on its card branch and merges after the tag.
**Codex lane 2 order from now (updated 2026-11-08):** V7.6 (new, v0.1) → V7.5 → V7.4 → V7.3 → (done: V1.1, V2b, R6, R7, V3) → (V4–V6: Claude) → R8 rework →
R10 rework → R11 (resume `.worktrees/r11`) → R12 → … → R17. V3–V6 start from current `main` now (they don't wait
for V2; Claude reconciles them during V2). Everything after V6 is post-v0.1: commit on the card branch, `review`, done.
Lane 1 (anime) is unchanged and is v0.2; no anime entry point may appear in a release build.
**Not in v0.1 (v0.2 or later):** anime (A2.3 → R-A4 and stages 3–4), R8/R10 reworks unless they land before the freeze,
R11–R17, P1.11 storage async migration, Phase 5 features without a card.

| # | Card | Who | Needs | Tier |
|---|---|---|---|---|
| V0 | Owner tasks: repo public, secrets, signing key, permissions | owner | — | — |
| V1 | Review batch: device-check + merge Y2, Y6, R1–R5, R9 | Claude | — | — |
| — | R6 Colour parity, R7 Japanese layout (existing cards) | Codex | R3 | T2 |
| V2 | Tsundoku + Mihon upstream merge (C5), then freeze | Claude | V1, R6, R7 | — |
| V3 | Watch teaser check (switch stays, owner 2026-11-07) | Codex | V2 | T1 |
| V4 | Telemetry out + Tsuzuki `release.yml` | Codex | V2 | T2 |
| V5 | About, licences, privacy, links | Codex | V2 | T2 |
| V6 | CHANGELOG 0.1.0 + version | Codex | V2 | T1 |
| V7 | Release-build runtime sweep (RUNTIME_QA gaps) | Claude | V3–V6 | — |
| V8 | Built-but-never-seen screens (WORKPLAN 4A) | Claude | V3–V6 | — |
| V9 | Cold start + baseline profile re-collect | Claude | V7 fixes | — |
| V10 | Signing continuity check | Claude + owner | V0, V4 | — |
| V11 | Release candidate on all three devices | Claude + owner | V9, V10 | — |
| V12 | Tag v0.1.0, publish, verify updater | Claude + owner | V11 | — |

#### V0 Owner tasks (owner) — `done` (item 2 done by Claude at the owner's request, 2026-11-07)
**Item 2 result:** release keystore `C:/Users/Manoj/Documents/Tsuzuki-signing/tsuzuki-release.jks` (PKCS12, RSA 4096,
alias `tsuzuki`, `CN=Tsuzuki`, cert SHA-256 `FF:CD:C7:43:A6:69:78:AE:24:CD:76:0B:44:78:32:71:A3:CC:18:16:84:4C:6A:78:6C:C9:EF:71:8E:A1:F6:C7`);
owner confirmed 2026-11-07: folder backed up and password moved to a password manager.
Secrets `SIGNING_KEY_BASE64`, `KEY_STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` set on `Manojx2005/Tsuzuki`. Verified
by building `assembleRelease -Penable-updater` through the CI env-var signing path: APK signed `CN=Tsuzuki`, same
fingerprint. **Consequences:** the owner's phone (debug-signed install) needs backup → uninstall → install → restore at
V11; CI preview builds now use this key too, so the `.debug` preview installs on the tablet/S22 (debug key) must be
reinstalled once (test data only).
1. Make `Manojx2005/Tsuzuki` public, or name a separate public releases repo (then `GITHUB_REPO` in
   `AppUpdateChecker.kt` changes in V4). The updater 404s until the repo it reads is public.
2. **Not done yet (Claude checked 2026-11-07: no secrets, no releases).** Create a release keystore and keep it safe
   (losing it means users can never update in place). In Git Bash, outside the repo:
   `keytool -genkeypair -v -keystore tsuzuki-release.jks -alias tsuzuki -keyalg RSA -keysize 4096 -validity 36500`
   then `base64 -w0 tsuzuki-release.jks > key.b64` and add the secrets (repo → Settings → Secrets → Actions, or
   `gh secret set NAME -R Manojx2005/Tsuzuki`): `SIGNING_KEY_BASE64` (contents of key.b64), `KEY_STORE_PASSWORD`,
   `KEY_ALIAS` (`tsuzuki`), `KEY_PASSWORD`. Delete key.b64 afterwards. Both workflows use these names.
3. Tell Claude which key signed the release app on your phone (`io.github.manojx2005.tsuzuki`): the same keystore
   as the secrets, or a different one. V10 checks it; a different key means v0.1 can't install over your data.
4. Answer the permission questions (V3 Watch switch, device use, phone upgrade, test installs, language switch,
   pushes). Answers are recorded under "Owner permissions for v0.1" below.

#### V1 Review batch (Claude) — `done` except leftovers below (merged into main as `3fa896b3e` on 2026-11-07)
Merge Y2, Y6, R1, R2, R3, R4, R5, R9 into a `review/night5` branch, resolve overlaps, build, unit tests, install on
the tablet + S22. Per card: its "Claude device checks" list. Plus C4: restore R1's synthetic 500-book backup into a
**scratch** install and record `gfxinfo framestats` for Home, Library (each tab), Updates, History, Browse; and R2:
`adb shell am kill` on every screen in R2's Result. Merge into `main` only what passes; failures go back as fix cards.

**V1 progress (Claude, 2026-11-07):** `review/night5` = main + R3, R1, R2, R4, R5, R9, Y2, Y6 (string conflicts: both
sides kept; R2 + Y6 combined in `TsuzukiReaderHooks.kt`). `assembleDebug` + 936 unit tests green; `assemblePreview`
installed on tablet + S22, both launch, crash buffers empty.
- Pass: **Y6** (tablet landscape docks the LISTEN panel; playback survives rotation; portrait < 900 dp keeps the
  floating pill by design). **R4** (Library menu: icons, groups, red destructive row). **R5** (tablet Settings two-pane).
- Found: **V1.1** (selected tab lost after process death; not from this batch).
- S22: **R3** pass (word-boundary wraps on Library, Updates, Home); Library/Updates phone layouts fine.
- Merged into main as `3fa896b3e` with these **leftovers for Claude's next session** (code already in main):
- Pending: **Y2** (needs a *paged* manga: the only manga, Nano Machine, opens as a webtoon), **R1/C4** scratch 500-book
  frame timing, **R2** in-Library state (after V1.1), **R3/R9** on the S22, R9 widgets placed.
- Polish notes for V8 (existing, not regressions): Dot System/Light/Dark segment not uppercased and in body font;
  Settings TIME FORMAT label wraps while its value truncates; "GRAYSCALE" vs British "colour"; Listen panel voice
  label shows a raw locale ("en-US-language"); Updates "Library last updated: Never" is italic body text, not
  uppercased in Dot.

#### V1.1 Selected tab lost after process death (T3) — `done` (merged via `review/night6`, 2026-11-07) (found by Claude in V1, 2026-11-07; release-blocking)
Repro (preview build `.debug`, tablet, Dot): Library → MANGA tab → Home key → `adb shell am kill
io.github.manojx2005.tsuzuki.debug` → open the app from **recents** (not the launcher). Expected: Library, MANGA
selected. Actual: Home. No crash. Evidence: `r2-before.png` / `r2-after2.png` in the repo root (untracked, local).
`HomeScreen.kt` keeps `retainedTsuzukiTab` / `retainedLibraryKey` in `rememberSaveable` and feeds
`TsuzukiNavigation.restore(...)` into `TabNavigator(tab = initialTab)`, so find where the restored value is lost
(is `savedInstanceState` non-null in `MainActivity.onCreate`? is `isTsuzuki` false on the first frame so
`initialTab` is computed before the theme resolves? does the startup overlay or Voyager's root `Navigator` rebuild
Home?). Fix with the smallest upstream change; add a test for `TsuzukiNavigation.restore`. Then R2's in-Library
state (shelf, search, selection) can be checked; Claude re-tests both on the tablet + S22. Not caused by the V1
batch (none of its cards touch `HomeScreen.kt` or `MainActivity.kt`), but R2's Result claimed the root tab already
survived process death.

**Result (Codex, 2026-11-07):** `afee7c2e6` on `tsuzuki/v1.1` (`.worktrees/v1.1`).
- Upstream `presentation/util/Navigator.kt` keys the outer animation by Voyager's saved screen key. The previous default keyed by the screen object, whose identity can change on deserialization and change the saveable composition path under Home.
- `TsuzukiNavigationTest.kt` checks that a serialized Manga tab resolves to the separate Library tab or the combined Library according to the active setting. Per-file Spotless passed twice. `assembleDebug` and `testDebugUnitTest` passed with 934 tests and zero failures.
- Claude device check: on the tablet and S22, background Library → Manga, kill the background process, then return from Recents and verify both selections. Check another root tab and the R2 in-Library shelf, search and selection state. Android process recreation still needs this device check.

#### HOME-SWIPE Swipe the Home cover stack (T2) — `done` (merged via `review/post-v01`, 2026-11-08)
On Home layouts that show the MotionStack cover stack, let a horizontal swipe select the next or previous book. Keep the tap action and existing shuffle and reduced-motion behavior. Use the MotionStack and phone Home boards in Dot and Colour.

**Result (Codex, 2026-11-07):** `a3a8a4220` on `tsuzuki/home-swipe` (`.worktrees/home-swipe`).
- `TsuzukiCoverStack.kt` uses a horizontal drag detector. A left swipe advances and a right swipe goes back, with wraparound. Taps advance through the same callback. The existing shuffle plays in reverse for the previous book.
- `TsuzukiHomeScreen.kt` applies the selected index to the title, progress, Resume action and cover tint. The hint lists both inputs in all eight locales. `docs/design/README.md` records the MotionStack status.
- Per-file Spotless passed twice. All eight string files parsed. `assembleDebug` and `testDebugUnitTest` passed with 933 tests and zero failures.
- Claude device check: use compact Home with at least three books in Dot and Colour. Swipe left and right through both ends, tap, scroll vertically starting on the stack, and check reduced motion and Resume after a swipe. Tablet layouts that use a single hero have no stack.

**V1.1 device check (Claude, 2026-11-07):** tablet preview from `review/night6`: Library → MANGA → Home → `am kill` →
Recents returns to Library with MANGA selected (before the fix: Home). No crash. 934 tests green.
Still to check on devices in V7/V8: R2 in-Library state (shelf/search/selection) now that V1.1 is in, V2b tracker
restore + long tab badges, R6 Colour headings, R7 Japanese labels, V3 Watch teaser.

#### V2 Upstream merge + freeze (Claude) — Tsundoku part `done` 2026-11-07 (1 commit, f118703a2 TTS paragraph split, merged; 933 tests green; S22 preview from main `3da3254c7`: TTS plays, highlights the spoken paragraph, progress advances, no crash); Mihon part → V2b (Codex); **freeze started 2026-11-07** (V2b, R6, R7, V1.1 merged): `main` takes only v0.1 work until the tag
`scripts/upstream-report.sh`; merge `tsundoku/main`; pick the Mihon fixes listed by the report (log in
`docs/UPSTREAM.md`). Build, tests, quick device smoke. Then announce the freeze in this file.

#### V2b Mihon release picks (T3) — `done` (merged via `review/night6`, 2026-11-07) (written by Claude 2026-11-07, owner: leave the rest to Codex)
Claude ran `scripts/upstream-report.sh mihon`: 224 commits not taken, most already logged as picked or skipped in
`docs/UPSTREAM.md`. For v0.1 take only these, one `git cherry-pick -x` each on `tsuzuki/v2b` (skip any already in
main or logged in UPSTREAM.md; if one conflicts with a Tsuzuki hook, resolve keeping the Tsuzuki behaviour, or skip
it and say why):
- `35bb84705` reader: GPU texture leak when a decoded page is no longer usable (#3872)
- `a82baac3e` tracking: Kitsu score-type crash from login (#3887)
- `1e054ea14` tracking: inverted MAL OAuth expiry check (#3927)
- `40f552981` duplicates: a blank title matched every library entry (#3308)
- `4b48a84ec` backup: keep a stored track's remote entry when restoring
- `6d4ed6cb2` extensions: backport class loader before Android 17
- `b19c8abeb` tab badges stay visible when the label is too long (the final version; not 7a5632c01/e8977b4a6)
- `0864ce0f4` redact sensitive headers in logcat (#4156): privacy, wanted for v0.1 even though it is flagged T/D
Log each pick (or skip + reason) in `docs/UPSTREAM.md`. Build + unit tests. Result: list, test count, what Claude
should check on a device (tracker login only if the owner approves; reader paging; Library duplicates; tab badges).

**Result (Codex, 2026-11-07):** `4e214de47` on `tsuzuki/v2b` (`.worktrees/v2b`).
- `ed17f1112` adapts Mihon `4b48a84ec` to Tsundoku's upstream `MangaRestorer.kt`. A stored tracker entry keeps its linked remote IDs and metadata, and a backup can advance its progress.
- `2c7bac0b2` cherry-picks Mihon `b19c8abeb` into upstream `Tabs.kt` in presentation-core. Badges get measured before the text.
- `docs/UPSTREAM.md` records all eight decisions. N2 already covered blank-title matching and header redaction. The current MAL expiry check and extension class loader cover those picks. The GPU viewer and Kitsu score-type code are absent in this fork.
- Per-file Spotless passed twice. `assembleDebug` and `testDebugUnitTest` passed with 933 tests and zero failures.
- Claude device check: in a scratch install restore a tracked book over the same tracker with different remote IDs and higher chapter progress. Verify the old link and higher progress. Check long Library tab labels with counts, then smoke-test manga paging and duplicate lookup. Tracker login requires owner approval.

#### V3 Watch teaser check (T1) — `done` (merged via `review/night6`, 2026-11-07) (owner 2026-11-07: keep the Read / Watch switch as a teaser)
`presentation/tsuzuki/home/TsuzukiHomeScreen.kt` `WatchPlaceholder`: the copy must say anime is coming in a later
version, with no sample numbers, fake titles or controls that look usable; nothing on it may open anime code in a
release build (only Back). `TsuzukiLibraryMediaSwitch.kt`, Updates tiles and Stats: no anime option in release.
Fix only what fails; list the checks in Result. Claude checks: preview build, Home → Watch, both themes, both devices.

**Result (Codex, 2026-11-07):** `69aa2ad3e` on `tsuzuki/v3` (`.worktrees/v3`).
- `TsuzukiHomeScreen.kt` keeps the switch and shows a short later-version message with a Back to reading action. Android Back also returns to Read. The feature tiles and their strings were removed in all eight locales. `docs/design/README.md` records the teaser.
- The Library media switch, Updates tiles and Stats have reading options only. The debug control APK declares both anime activities. The four ABI release APKs and the universal release APK contain neither activity nor the mpv/player/FFmpeg libraries.
- Per-file Spotless passed twice and the eight teaser strings parsed. `assembleDebug`, `testDebugUnitTest` (933 tests, zero failures) and `assembleRelease` passed.
- Claude device check: in the preview build, open Home → Watch in Dot and Colour on the S22 and tablet. Check both back paths and verify that Library, Updates and Stats only offer reading features.

#### V4 Telemetry out + Tsuzuki release workflow (T2) — `done` (Claude 2026-11-07; see Result)
`PRIVACY.md` promises no telemetry. Keep `Config.includeTelemetry` and the Firebase code paths for Tsundoku merges,
but the Tsuzuki release must never pass `-Pinclude-telemetry`.
- Rewrite `.github/workflows/release.yml` for Tsuzuki: run on tag `v*` pushes, gate
  `github.repository == 'Manojx2005/Tsuzuki'`, JDK 21, `./gradlew assembleRelease -Penable-updater` (no telemetry),
  sign with the same secret names as `tsuzuki-preview.yml`, APK names `tsuzuki-<tag>.apk` (universal) and
  `tsuzuki-arm64-v8a-<tag>.apk` (+ the other ABIs the build makes), SHA-256 file, release notes = the matching
  `CHANGELOG.md` section. Copy the signing step style from `tsuzuki-preview.yml`. Remove Tsundoku-only jobs.
- Delete `app/google-services.json` (owner approved 2026-11-07) after confirming release builds without it; note it
  in `docs/UPSTREAM.md` (a Tsundoku merge that restores it gets resolved by deleting again).
- Add a Gradle check or unit test that release builds have `BuildConfig.TELEMETRY_INCLUDED == false`.
- Don't run the workflow and don't create tags. Claude checks: `assembleRelease -Penable-updater` locally, then the
  APK has no `com.google.firebase` classes.

**Result (Claude, 2026-11-07):**
- `.github/workflows/release.yml` rewritten for Tsuzuki: tag `v*` on `Manojx2005/Tsuzuki`, checks the tag equals
  `versionName`, `assembleRelease -Penable-updater` (never `-Pinclude-telemetry`), signs through Gradle with the same
  secrets as `tsuzuki-preview.yml`, fails if any `Lcom/google/firebase/` class is in the APK, renames to
  `tsuzuki[-<abi>]-<tag>.apk`, notes = the CHANGELOG section + SHA-256 list (checksums in the notes, not as an asset:
  the updater treats an asset without an ABI in its name as the universal APK), published `--latest`.
- `app/google-services.json` deleted (owner-approved). The telemetry module already compiles its no-op source set
  and no Firebase dependency without the flag; local `assembleRelease -Penable-updater`: 0 Firebase classes,
  `io.github.manojx2005.tsuzuki` 0.1.0 (24), label Tsuzuki.
- **Updater channel bug fixed (found while doing V4):** preview (`rNNNN`) and stable (`vX.Y.Z`) releases share one
  repo and both read `/releases/latest`, and the preview workflow published `--latest`. A stable 0.1.0 install seeing
  `r3000` would compare `3000 > 0` and offer the preview APK; a preview install seeing `v0.1.0` would hit
  `"0.1.0".toInt()`. Now: `tsuzuki-preview.yml` publishes `--prerelease`; `ReleaseServiceImpl` (data) makes preview
  builds read `/releases?per_page=30` and take the newest `r…` tag; `GetApplicationRelease.isNewVersion` (domain)
  ignores a tag from the other channel. 2 new tests in `GetApplicationReleaseTest` (5/5 pass). Note: preview builds
  installed before this change read `/releases/latest` and won't see new (pre-release) previews; reinstall them once.
- **Signing (feeds V0 / V10):** the repo has no releases and no Actions secrets yet (`gh release list` / `gh secret
  list` empty), and this machine has no `keystore.properties`, so local builds (preview and release) are signed with
  the per-machine **Android debug key** (`CN=Android Debug`, SHA-256 `6c1e396d…e17a`). Claude's recommendation: the
  owner creates a real release keystore and the four secrets (commands in V0), and the owner's phone does a one-time
  backup → uninstall → install → restore at V11 (its current install is almost certainly debug-signed on the laptop).

#### V5 About, licences, privacy, links (T2) — `done` (Claude 2026-11-07; see Result; device look at the About credits in V8)
About screen: app name, version, "Based on Tsundoku and Mihon" credit with links, GitHub link = the V0 releases
repo, open-source licences list includes the bundled fonts (`docs/licenses/`), QuickJS host and the LNReader plugin
index terms. `PRIVACY.md`: accurate for v0.1 (no telemetry; network calls: sources, trackers, translation provider if
configured, updater). `TsuzukiLinks.kt`: no link points at tsundoku-otaku. `NOTICE`/licence files present for
Apache-2.0. List each check in Result. Claude checks: About + licences on both devices, both themes.

**Result (Claude, 2026-11-07):** `PRIVACY.md` is already accurate for v0.1 (sources, repos, trackers,
translation, fonts, updates; no analytics, now enforced by V4). About (`TsuzukiAboutScreen.kt`) already credits Mihon,
Tsundoku, LNReader plugins and Keiyoushi with licences, links source code and privacy policy to
`Manojx2005/Tsuzuki`, and the Licences screen lists Gradle dependencies. Added: credits for the bundled fonts
(Doto, Instrument Serif, Space Mono, Manrope, Literata; DotGothic16, M PLUS 1 Code, Noto Serif JP, Zen Kaku Gothic
New: OFL-1.1, texts in `docs/licenses/`). No link points at tsundoku-otaku except the stock-theme Mihon credit. No
`NOTICE` file needed (Mihon/Tsundoku ship none). V8: look at the About credits rows (long names may wrap).

#### V6 CHANGELOG 0.1.0 + version (T1) — `done` (Claude 2026-11-07; see Result)
Move Tsundoku's changelog to `docs/CHANGELOG-tsundoku.md` (header: "Inherited from Tsundoku; Tsuzuki's own
history is in CHANGELOG.md"). New `CHANGELOG.md`: same Keep-a-Changelog format, `## [0.1.0]` written for users
from this file's log (Dot + Colour themes, Home, library shelves, novel reader + TTS, manga reader, tablet layouts,
downloads, backups, widgets, Japanese UI); no card IDs, no sample numbers. Leave `versionName = "0.1.0"`;
`versionCode` stays 24 unless Claude reports in V10 that the phone's install is ≥ 24 (then bump above it).

**Result (Claude, 2026-11-07):** Tsundoku's changelog moved to `docs/CHANGELOG-tsundoku.md`.
New `CHANGELOG.md` with `## [0.1.0]` (Added / Improved / Fixed / Other), written for users; `release.yml` publishes
this section as the release notes. `versionName` stays `0.1.0`, `versionCode` 24 (V10 checks the phone's install).
`docs/UPSTREAM.md`: keep ours for `CHANGELOG.md` conflicts; delete `google-services.json` again if a merge restores it.
**Before tagging (V11):** confirm the changelog lines that are not device-verified yet: process-death state (needs
V1.1), the foldable hinge layout (no foldable here; drop the clause if untested), and the two-page spread (Y2).

#### V7 Release-build runtime sweep (Claude) — `ready` after V3–V6
On an optimised (preview or release-type) build, tablet + S22, Dot + Colour, close every gap in
`docs/RUNTIME_QA.md`: P1.1 rotation + process death (readers, sheets, queued work), P1.2 downloads (novel + manga,
pause/resume, force-stop), P1.3 backup round trip, P1.4 EPUB + mass import (owner-approved temporary files), P1.5
translation safe failure without credentials, P1.6 TTS remaining combos + background + headset, P1.7 one
owner-approved extension install/update/uninstall, P1.8 widget, P1.10 separate-library mode, search, empty and
error states. Each bug → a `V7.n` fix card for Codex (logcat + screenshot + repro). Update RUNTIME_QA.

**V7 progress (Claude, 2026-11-07, preview from `review/night6`):**
- **R2 pass** (tablet): Library shelf + search text and applied search survive `am kill` → Recents. Library search
  filters on submit (Enter), not live; that is Tsundoku's existing draft/submitted split, not a regression.
- **V3 pass** (S22): Watch page says anime is coming in a later version; only a Back to reading button.
- **R6 pass** (S22, Colour): italic serif headings over Manrope body, palette follows the current book; Home + Library.
- **R7 mostly pass** (S22, app language Default = Japanese): dates, counts, tabs, tiles, badges localized. **Bug →
  V7.1:** the pill-nav label ライブラリ wraps mid-word ("ライブラ / リ") on the phone, selected and unselected.
- **Downloads (P1.2) pass** (S22): Updates → Save all queued 6 novel chapters; the tile turned "saved" and the queue
  emptied with no errors.
- **V7.2 fixed by Claude:** More tiles in Japanese cut off ダウンロードキュー / データとストレージ with "…"; tile titles now
  auto-size down to 75 % before ellipsizing (`TsuzukiMoreScreen.kt`). S22: both labels whole.
- **Light mode bug fixed (owner report 2026-11-07):** picking Light (or System on a light phone) mixed a light Material
  scheme into the dark-only Dot/Colour designs (white Settings pane, invisible labels, dark status icons on black).
  Owner: "just remove it". Dot and Colour now always use night mode and Appearance no longer shows the switch.
  `TsuzukiThemeModeTest` 2/2; tablet with Light stored renders correctly.
- **Novel reader process death pass** (tablet, TextView): scrolled to 92 %, `am kill`, Recents → same paragraph, 92 %.
- **Translation (P1.5) pass:** defaults are on-device ML Kit (no key, nothing paid) with target `en`; toggling
  Translate on an English chapter changes nothing and doesn't crash. Polish note: no hint when source = target.
- **Manga reader smoke pass** (tablet, webtoon): Nano Machine ch. 1 loads from the source, long-strip scroll to 11/24,
  page counter updates, no crash.
- Still open in V7: backup round trip (needs the system file picker; owner can do it on the release build), EPUB and
  mass import, one extension install/update/uninstall, widget placement, separate-library mode, Y2 spread (needs a
  paged manga), R1 500-book frame timing (scratch install).
- **Downloads slow (owner report 2026-11-07):** novel pacing defaults were 3 s + 0-1 s jitter per request, 1 permit,
  1 book at a time (Tsundoku). Now 1 s + 0-0.5 s, 2 permits, 2 books at once; per-source pacing and a source's
  declared minimum are unchanged. Devices that ever saved their own values keep them (Settings → Novel downloads).
- **Browse → Migrate bugged (owner report 2026-11-07):** Migrate is the pager's third page but the header had two
  segments, so nothing was selected and the indicator collapsed; the list was stock-styled. Now Migrate shows as a
  selected third segment while open, and the list uses the Sources rows (`TsuzukiMigrateSourceList`). Tablet: header,
  rows, tap → source's books, Sources segment back; `BrowseHeaderSectionsTest` 2/2.
  **Follow-up (owner 2026-11-07):** Migrate is now always the third segment (no overflow entry), and the Browse pager
  holds only the chosen media's three pages, so swipes move Sources ↔ Extensions ↔ Migrate and never cross Novels /
  Manga; the pills switch media and keep the section. Tablet: 4 swipes stop at Novels Migrate; Manga keeps Migrate;
  3 back-swipes stop at Manga Sources. `BrowseHeaderSectionsTest` 4/4.
- **App update notification (owner 2026-11-07):** `TsuzukiAppUpdateJob` checks GitHub daily (network required, release
  and preview builds with the updater only) and notifies once per new, unskipped version; tapping opens the existing
  update screen. Tablet: job scheduled (WorkManager job "TsuzukiAppUpdateJob"), simulated tap opens UPDATE READY with
  Download / Not now / Open on GitHub / Skip; 946 tests green. First real end-to-end check needs v0.1.0 → a later tag.
- V8 polish note: when More scrolls, tiles slide under the transparent status bar and the clock overlaps their text.
  `done` (merged via `review/night7`, 2026-11-08), was `review` (`tsuzuki/v8-more-insets` at `afd41c61f`). `TsuzukiMoreScreen.kt` applies the status-bar inset outside
  the scroll viewport so tiles stay below the clock and the themed background fills the inset. `docs/design/README.md`
  records the change. Per-file Spotless passed twice, the debug APK built, and 954 tests passed (0 failures or skips).
  Claude: scroll More to both ends on the S22 and tablet in Dot then Colour. Check that no tile crosses the clock
  and that the first title and final tile remain fully visible when scrolled to their respective ends.
  **Claude 2026-11-08:** S22 Colour, scrolled mid-list: tiles clip below the clock, last tile fully visible. Pass (in `review/night7`).
- Note for testers: after closing the preview app, a launcher tap opened the owner's `.dev` app by mistake (no
  changes made there). Always start the test app with `monkey -p io.github.manojx2005.tsuzuki.debug`.
- Owner 2026-11-07: Claude tests on tablet + S22 only; the owner checks the release build on their own phone
  (replaces the phone parts of V10/V11).
- Owner's phone `R5GL23ZYE1V` was connected but not visible to adb on this PC (not even "unauthorized"): needs USB
  debugging allowed for this computer before V10/V11.

#### V7.1 Japanese nav label wraps mid-word (T2) — `done` (Claude 2026-11-07: a label without spaces stays on one line and auto-sizes down to 75 %; labels with spaces still wrap between words; `TsuzukiNavLabelTest` 3/3; S22 Japanese: ライブラリ on one line, no crash)
S22 (1080 px, 384 dp wide), preview, Colour and Dot, app language Japanese: the bottom pill nav shows ライブラリ as
"ライブラ" / "リ" on two lines, selected and unselected (evidence: `r7-preview-home.png`, `r7-preview-lib.png` in the
repo root, untracked). Fix in the Tsuzuki pill-nav label composable (`theme/tsuzuki/TsuzukiNav.kt` or wherever the
pill item text is drawn): keep each label on one line (`maxLines = 1`, `softWrap = false`) and shrink to fit the
item width (auto-size between the current size and ~80 %, or measure and step down) instead of wrapping; never
break inside a word. Check the other locales' longest labels (de, fr, es, pt-BR) at 360 dp and with font scale
1.3. Test: a pure function for the label size choice. Claude re-checks on the S22.

#### V7.3 Japanese for the rest of the app (T2) — `done` (merged via `review/night7`, 2026-11-08)
Claude added 87 strings (main-file Settings strings + novel-module headings, commit `5a523840d`). Still missing in
Japanese: ~120 in `i18n/.../base/strings.xml` and ~860 in `i18n-novel/.../base/strings.xml` (only 6 of 882 novel
strings had Japanese). Add them to the matching `ja/strings.xml`, novel-reader and settings screens first. Rules: keep
every placeholder exactly (`%s`, `%d`, `%1$s`, `{TEXT}`…; write a check like Claude's `ja_settings.py`), escape `&`
and `'`, product names stay Latin (Tsuzuki, Dot, Colour, LNReader, Mihon, Shizuku), titles as short noun phrases,
descriptions in polite です/ます, match the existing ja file's terms (小説, マンガ, チャプター, ライブラリ, 本棚).
Claude checks the S22 in Japanese (Settings, novel reader settings, Downloads, Translation).

**Result:** `tsuzuki/v7.3` at `9d066cb97`. Added Japanese for the 129 missing translatable shared strings and
862 missing novel strings. Moved literal labels in Browse, Appearance, Font Manager, global novel settings,
and the in-reader settings into resources so they follow the app language.

- `i18n` and `i18n-novel` base and Japanese `strings.xml` files cover the existing and newly resource-backed text.
- Upstream `SettingsBrowseScreen.kt`, `SettingsAppearanceScreen.kt`, `FontManagerScreen.kt`,
  `SettingsNovelReaderScreen.kt`, and `NovelPage.kt` use the localized settings labels.
- `scripts/check-japanese-strings.py` verifies all 2,473 translatable keys for Japanese coverage and checks
  placeholders and markup. Per-file Spotless passed twice, debug built, and 954 tests passed without failures
  or skips. `docs/design/README.md` records status.
- Claude: check S22 in Japanese, including the named Settings pages, novel reader controls in both renderers,
  Downloads and Translation. Check wrapping at large font scale and confirm the labels also render in English.

**Claude review 2026-11-08 (`review/night7` = main + V7.5 + V7.4 + V7.3 + V8-more-insets, clean merges, not in main yet):** code reviewed, 959 unit tests green, `assemblePreview` installed on the tablet + S22, crash buffers empty. S22 Japanese: Settings main and Novel reader settings fully Japanese. Pass. New keys exist in English + Japanese only (other 6 locales fall back to English). Leftover truncated `ネイティブ（TextVie…` → V7.6 item 5. Merge into main waits for the owner (freeze: v0.1 fixes allowed).

#### V7.4 Novel reader: keep scrolling text out of the notch / gesture area (T3) — `done` (merged via `review/night7`, 2026-11-08)
Claude's `84d722dce` adds `novelSafeInsets()` to the top/bottom margins, so a chapter starts below the notch. While
scrolling, text still passes under the camera cutout and the gesture bar because the margin is content padding. Like
LNReader, reserve the safe area on the scroll viewport in both renderers: TextView (scroll container padding +
`clipToPadding = true`, padded band painted with the page colour) and WebView (fixed top/bottom bands or viewport
padding via CSS env(safe-area-inset-*) / JS-provided px). The user's margins stay content padding inside that.
Paged mode: page height excludes the safe area. Reader menu shown/hidden must not shift text. Test both renderers,
Dot + Colour, S22 (punch-hole) and tablet; screenshots at chapter start and mid-chapter.

**Result:** `tsuzuki/v7.4` at `f99094214`. Both renderers use the existing clipped native viewport, so scroll range
and WebView page height use the same physical safe area. Insets ignore system-bar visibility, and the reserve for
the app's optional reading status bar sits on its selected edge. The viewport bands use the page background.

- Upstream `ReaderActivity.kt` combines window insets and reading status bar height, anchors that bar in the safe
  area, and paints the viewport background. `NovelSafeInsets.kt` supplies the stable insets and bar reserve.
- Upstream `NovelViewer.kt` and `NovelWebViewStyler.kt` use the reader's selected margins as content spacing.
- `NovelSafeInsetsTest.kt` checks top and bottom placement and preserves rotated side cutouts. Per-file Spotless
  passed twice, the debug APK built, and all 954 app unit tests passed without failures or skips.
- Claude: S22 and tablet, Dot and Colour, TextView scroll and WebView scroll/paged. Take screenshots at chapter
  start and during scroll. Toggle the menu mid-chapter and check the same text stays in place. Check both reading
  status bar edges, page background continuity, rotation, and a manga fullscreen/cutout smoke check.

**Claude review 2026-11-08 (`review/night7` = main + V7.5 + V7.4 + V7.3 + V8-more-insets, clean merges, not in main yet):** code reviewed, 959 unit tests green, `assemblePreview` installed on the tablet + S22, crash buffers empty. S22: first line starts below the punch-hole; opening the menu mid-chapter leaves the text in place. Pass. Not yet run: WebView paged, reading status bar at the top edge, rotation. Merge into main waits for the owner (freeze: v0.1 fixes allowed).

#### V7.5 Novel reader: can't scroll after Previous chapter (T3) — `done` (merged via `review/night7`, 2026-11-08)
Owner: reading chapter 41 (Lord of the Mysteries, release build), tapped back to the previous chapter (40, already
read) and could not scroll any more. Reproduce on tablet + S22 in both renderers (TextView, WebView), infinite scroll
on and off: open chapter N > 1, tap ‹ Previous (bottom bar and chapter-end card), try to scroll up/down, tap, open
the menu. Suspects: the previous chapter loads at its saved 100 % position and the scroll restore or Q13's
next-chapter warm-up leaves the container locked/consuming gestures, or the TTS mini-controls overlay intercepts.
Fix the cause, add a regression test for the position/lock logic. Claude device-checks.

**Result:** `tsuzuki/v7.5` at `50319ea66` (includes `3f865b77e`). Both renderers restored a saved 100% position on Previous. Previous now
opens a completed chapter at its start. A chapter already loaded by infinite scroll becomes active and is positioned
by its renderer when it receives the chapter. Direct opens and full loads with valid partial progress still restore
that position. Each Previous request is consumed once so a subsequent renderer switch can restore normal progress.

- `NovelEntryProgress.kt` shares the entry rule, with three regressions in `NovelEntryProgressTest.kt`.
- Upstream `ReaderViewModel.kt` and `ViewerChapters.kt` carry a distinct request for each Previous tap.
  `NovelChapterNavigationTest.kt` checks repeat navigation to the same target and renderer redispatch.
- Upstream `NovelViewer.kt` cancels stale restores and positions a buffered chapter at its visible text block.
  `NovelWebViewViewer.kt`
  cancels the superseded content job and reuses a buffered chapter only when its document is ready.
- `TsuzukiAdjacentChapterHooks.kt` hands the backward jump to the renderer. `docs/design/README.md` records status.
- Per-file Spotless passed twice. Debug APK built and all 959 app unit tests passed, 0 failures or skips.
- Claude: on S22 and tablet, use Previous from chapter 41 to already-read chapter 40. Check upward/downward scroll,
  menu taps, and progress after moving backward twice. Scroll into chapter 41 again and tap Previous again while
  40 remains loaded. Cover TextView and WebView with infinite scroll on/off, then
  the WebView paged edge and any available Previous action at chapter end. The original touch symptom awaits device
  reproduction and verification. The code change addresses the confirmed end-position restore and buffered jump paths.

**Claude review 2026-11-08 (`review/night7` = main + V7.5 + V7.4 + V7.3 + V8-more-insets, clean merges, not in main yet):** code reviewed, 959 unit tests green, `assemblePreview` installed on the tablet + S22, crash buffers empty. S22 TextView, Colour: Ch. 108 → ‹ Previous → Ch. 107 opens at 0 % and scrolls; ‹ again → Ch. 106 scrolls down and up. Pass. Not yet run: WebView renderer, infinite scroll off, tablet. Merge into main waits for the owner (freeze: v0.1 fixes allowed).

#### V7.7 Smoother chapter-end Next chapter (T2) — `blocked` (note 2026-11-19: this card's branch commits are already in `main`; check whether the block still applies) (2026-11-08, `.worktrees/v7.7`) (owner 2026-11-08: 0.1.1; v0.1.0 is tagged)
Owner: RC4's fix works but the tap → next chapter switch should feel smoother. Board: `06-reader/ChapterEnd`. Text reader (`NovelViewer`) with
infinite scroll **off** (now non-default). Today the tap clears the page and the new chapter pops in at the top.
Look at the chapter-end card board, then: show press feedback at once, keep the card until the next chapter's
content is ready (no blank frame), and bring the new chapter in with the reader's existing transition
(`TsuzukiReaderTransition.kt`) rather than a hard cut. Don't regress the touch fix in
`tsuzuki/reader/NovelChapterEndTouch.kt` (fling into the end, then tap: must load on the first tap, no menu flash).
Check the WebView reader's card (`chapter-end-card.js`) for the same feel. Device check on the tablet + S22.

**Result (Codex, 2026-11-08):** `tsuzuki/v7.7` at `16f713ff8` (base `c0a0015a4`).

- `TsuzukiChapterEnd.kt` shows a pressed state and disables repeat taps while the chapter-end request is pending.
- `NovelChapterEndTransition.kt` keeps an in-memory copy of the drawn viewport above the reader and applies the existing enter and outgoing animations after the destination can draw. It releases the frame on errors, superseded loads and viewer teardown. If the window cannot be copied, navigation proceeds without the frame transition.
- `NovelViewer.kt` reveals after the next text layout and progress restore. The RC4 touch handling still owns the first tap after a fling.
- `chapter-end-card.js` and `NovelWebViewViewer.kt` share the pending feedback and wait for visual readiness before revealing the next page. The WebView keeps its old document while fetching and processing the next one.
- `ReaderActivity.kt` validates the outgoing chapter before forwarding the card request to the existing chapter loader.
- `docs/design/README.md` records the implementation status.

JDK 21, two per-file Spotless passes, `:app:assembleDebug` and `:app:testDebugUnitTest` passed
(968 tests, 0 skipped). A local JavaScript smoke test covered duplicate activation,
the chapter ID on all three actions and a fresh button after card reinjection.

Claude device review on the S22 and Tab S11 in Dot and Colour. Turn infinite scroll off, fling to
the chapter end and tap Next once. Check immediate feedback, no menu flash, and the reveal for
both cached and slow chapters in TextView and scrolling WebView. Check reduced motion, a failed
load and retry, and paged and infinite-scroll navigation.

**Review (Codex RV1, 2026-11-08):** Fail. Native Dot advances on the first tap and holds the outgoing frame. In scrolling WebView Colour, one Next chapter tap loads the right chapter but reveals the hidden reader chrome. The frame sheet shows the controls entering during the handoff. `NovelWebViewViewer.kt:302` handles the same tap as a native navigation gesture while the JS button handles it. Evidence: `v77-native-dot-frames.png` and `v77-web-colour-frames.png`. Reverted in `dc4b7d563`.

#### V7.7-F Keep the WebView chapter-end tap in the card (T3) — `done` (RV4, 2026-11-13)

Reapply V7.7 by reverting `dc4b7d563` on a fix branch from the reviewed main. A tap on an interactive
chapter-end control should reach the DOM action once and should preserve reader chrome visibility and scroll
position. Keep ordinary text taps and paged gestures working. Verify on the tablet and S22 with both renderers,
including a fling to the card and a slow or failed chapter load.

**Result (Codex, 2026-11-09):** `tsuzuki/v7.7-f` at `06224334e` (transition restored in `f8111f792`).

- `NovelWebViewViewer.kt` checks the DOM target at single-tap release before WebView handles the event. The native tap zone runs on confirmation only for a non-card hit. Paged drag, fling, and the native TextView tap path remain separate.
- `chapter-end-card.js` marks the card so the DOM keeps its own Next, Mark read and Contents clicks, including disabled Next while a request is pending.
- The revert-of-revert restores `NovelChapterEndTransition.kt`, `TsuzukiChapterEnd.kt`, and the existing `ReaderActivity.kt` / `NovelViewer.kt` handoff. `docs/design/README.md` records the current status.

Verification: JDK 21, two per-file Spotless passes, `:app:assembleDebug :app:testDebugUnitTest` (977 tests, 0 failures, errors or skips), and a Node DOM smoke check covering the three buttons, pending Next, nested targets, ordinary content and card reinjection.

Device review: on Tab S11 and S22, Dot then Colour, fling to the card and tap Next with chrome hidden and shown in TextView and scrolling WebView. Check one action, stable chrome and outgoing scroll position, plus slow/failed loads and retry. Check Mark read, Contents, ordinary text taps and paged swipes.

Review (Codex RV4, 2026-11-13): Pass for the chapter-end tap/transition change. Tablet Dot/Colour Native and scrolling WebView Next navigate once; the recorded hidden-chrome WebView tap keeps chrome hidden, and a normal text tap still shows it. Native missing local chapter displays Retry and recovers after the fixture is restored. Final accepted Preview smoke opens Chapter 131 at 0%. Native Contents works with chrome hidden; visible bottom bars overlap the card (pre-existing unchanged layout), tracked as RV4-01 in RP1, so no claim that covered-button taps passed. Candidate 1,026 tests; accepted 1,011 tests, both Debug/Preview builds and scoped Spotless twice pass. S22 deferred by owner. Evidence `app/build/review-evidence/rv4/`, including transition MP4s/contact sheets and accepted APKs.

#### V7.6 v0.1 polish from the V1 notes (T2) — `done` (2026-11-08, `.worktrees/v7.6`) (Claude 2026-11-08; v0.1 fix, allowed into main during the freeze)
Small, visible polish Claude found in V1 that is still open. Worktree from `main` (`tsuzuki/v7.6`), no Needs.
Boards: the screen's board in `docs/design/boards/INDEX.md` (Settings, Updates, TTS/ListenMini). Dot first, then Colour.
1. **Updates "Library last updated: Never/…" line** (`UpdatesUiItem.kt:68`, `TsuzukiUpdatesTwoPane.kt:260`): in the
   Tsuzuki themes it is italic body text. Use the board's caption style through `MaterialTheme.typography`, upright,
   and uppercase in Dot (`tsuzukiCase()`). Stock themes unchanged (dispatch, no re-indent).
2. **Settings TIME FORMAT row:** the label wraps while its value truncates on the S22 (360–384 dp). Give the label
   priority (value takes the remaining width, ellipsis on the value only), in the Tsuzuki preference row, not upstream.
3. **Listen panel voice label** shows a raw locale like `en-US-language`. Show the voice's display name
   (`Voice.locale.getDisplayName(appLocale)` + a short quality/network suffix only if the engine gives one); fall back to
   the engine's label. Same fix in the tablet LISTEN panel (Y6) and the mini player if they share the label.
4. **"Grayscale" vs British "colour":** the app's English uses British spelling (Colour theme). Change English
   `pref_grayscale` and any Tsuzuki-only English strings using "gray"/"color" in user-visible text to "greyscale"/
   "colour". Only `base/strings.xml` English values. Keys unchanged, other locales untouched; don't edit upstream
   strings that Tsundoku also ships unless they are Tsuzuki-only (list each changed key in Result).
5. **Novel reader settings, render-mode segment** (Japanese, S22): the left option shows `ネイティブ（TextVie…`. Use a
   shorter Japanese label (e.g. `ネイティブ`) or let the segment text wrap to two lines; check the other locales.
Tests: a pure function for the voice label (raw locale → display name; unknown → engine label). Verify per WORKPLAN §0.4
plus `:app:assemblePreview`. Claude checks on the S22 + tablet in Dot and Colour, English and Japanese.

**Result (Codex, 2026-11-08):** `tsuzuki/v7.6` at `2f482f4de` (base `c0a0015a4`).

- `UpdatesUiItem.kt`, `TsuzukiUpdatesTwoPane.kt`, `TsuzukiUpdatesLastUpdated.kt` share an upright, muted caption with Dot casing.
- `ListPreferenceWidget.kt` and `TsuzukiSettingsPane.kt` give the title first claim on width in themed rows that show a selected value. The value uses the remaining width with ellipsis.
- `TtsController.kt`, `TtsVoiceLabel.kt`, `ReaderListenLayout.kt` and `TsuzukiReaderListenPanel.kt` use the engine's voice locale and connection requirement for the docked Listen label. The engine label is the fallback. The phone mini player is controls-only.
- `TtsVoiceLabelTest.kt` covers app-locale display and unknown-locale fallback. `i18n` adds `tsz_tts_voice_offline` and `tsz_tts_voice_online` in all eight locales. `pref_grayscale` is the only existing English key changed in `i18n`.
- `i18n-novel` shortens `novel_render_default` in English and Japanese and `pref_novel_native_renderer` in Japanese. The six other shipped locales use the English `novel_render_default` fallback.
- `GuidesStep.kt` sends onboarding and empty Novel/Manga library links to the Tsuzuki README's Download section. Verified HTTP 200 and the published `#download` anchor.
- `docs/design/README.md` records the implementation status.

JDK 21, two per-file Spotless passes, `:app:assembleDebug`, `:app:testDebugUnitTest` (970 passed, 0 skipped)
and `:app:assemblePreview` passed. The serial build used `--no-parallel --max-workers=2` and a 6 GB Kotlin
daemon heap after the combined build exhausted the compiler heap.

Claude device review on the S22 and Tab S11 in Dot and Colour, English and Japanese. Verify the Updates
caption, Time format row and picker, the active voice label on tablet, the native renderer segment and
the Getting started guide from the empty library. R-A5 awaits the A3.5 commit.

**Review (Codex RV1, 2026-11-08):** Pass. S22 Time format kept its title on one line in Japanese and English, with only the English Dot value ellipsised. The Japanese Native chip fits. The Updates caption is upright on both displays, and tablet Listen shows the localised offline voice name. Earshot measured the 13.71 s local capture at −19.6 LUFS (AudioSet Speech 0.72; 92.55% digital silence). The unrelated GuidesStep URL change was dropped in `a8cfedf0f`. Evidence: `v76-*` in the RV1 evidence folder.

#### V8 Built-but-never-seen screens (Claude) — `claude-working` (tablet pass 2026-11-19; widget picker, extension details, WebView, phone Settings and S22 still open)
WORKPLAN 4A: widget picker preview, unopened menus (manga reader top bar, extension details, web view, migration
config, download queue Cancel all), mass import batch cards, phone Settings layout. NewUpdate screen moves to V12
(needs a public release). One English pass on the S22 if the owner approved it in V0. Bugs → `V8.n`.

**V8 tablet pass (Claude 2026-11-19, `rv5debugscratch` = TTS-POLISH chain build, Japanese, Colour; data restored byte-equal, crash log empty; evidence `app/build/review-evidence/v8-claude/`):**
- Manga reader bars (local manga): fine; top bar has only the bookmark (WebView/share appear for online sources only); RTL slider labels correct.
- Download queue: overflow 「すべてキャンセル」 works (queued a fixture download behind a slow local server via `adb reverse`, removed). Bugs → V8.1, V8.2.
- Migration: book list is the plain upstream row (no Tsuzuki tile); config screen follows the X4 board. Bugs → V8.3, V8.4.
- About credits: long names wrap; the licences tile title truncates to 「オープンソースライ…」 in the two-pane width and the last credits row's "OFL-1.1" touches "Noto" (no gap) → V8.4.
- WebView screen (fixture details › WebView, server down): bar, back/forward, menu 更新/共有/ブラウザーで開く/Cookie を削除 all fine in Japanese; the WebView starts below the bar (y=201 px). Polish: the display-serif title takes the bar's height so the URL line sits on the bar's bottom edge (`v8-webview.png`); the Chrome error page's icon is cut at the top — probably that page's own layout, recheck with a real page before filing.
- Not done: widget picker preview (needs the launcher, owner device action), extension details (needs an installed extension, P1.7 owner approval), phone Settings layout and the S22 pass (S22 not on adb).

#### V8.1 Novel download queue rows in English (T1) — `done` (merged locally 2026-11-21)

Review (Codex Batch T, 2026-11-21): Passed and merged locally. The tablet Dot queue showed the Japanese chapter count, downloading status, chapter label and overflow description. All five statuses have passing mapping tests. Screenshot `tablet-debug-download-queue-dot.png`. Integrated Debug and optimized Preview built, with 1,396 tests passing. Evidence `.worktrees/review-v02-main/app/build/review-evidence/v02-main/`.
On a Japanese device the novel queue row shows "0/1 chapters", "Chapter: …", status "Error/Downloading/Queued/Completed/Pending" and the overflow's label "More options". Use the existing translated keys (all present in ja): `TDMR.strings.novel_downloads_chapters_format` (`DownloadQueueScreen.kt:770`), `novel_downloads_more_options` (:783), `novel_downloads_chapter_format` (:903); make `DownloadQueueViewModel.statusText` return a `StringResource` (reuse existing MR keys where the meaning is the same; otherwise new `tsz_` keys in all 8 locales) and resolve it in the screen; `fullErrorReport`'s "Unknown error" can stay (clipboard detail). Both files are upstream and already touched; keep the diff to those lines. Test: status mapping per state. Reviewer: queue a download (slow local server, see V8 notes) on the Japanese tablet; screenshot `v8-claude/v8-queue-item.png` is the before.

Result (Claude 2026-11-19): `DownloadQueueScreen.kt` uses the existing translated `TDMR` keys for "N/M chapters", "Chapter: …" and the row's "More options"; `NovelDownloadItem.statusText: String` → `status: StringResource` (Error → `chapter_error` エラー, Downloading → `ext_downloading` ダウンロード中, Queued → `tsz_downloads_queued` 待機中; new `tsz_download_done` 完了 and `tsz_download_pending` 準備中 in 8 locales, because `completed` means a finished series (完結済み) and `ext_pending` means on hold (保留)). `errorDetails`' "Unknown error" fallback stays (Tsuzuki themes show the localized error heading from `errorCopyForUi`; stock theme only) and so does `fullErrorReport` (clipboard). New `NovelDownloadItemStatusTest` (5 states) passes; full app suite **1,222 tests, 0 failures/errors/skips**; scoped Spotless clean. Tablet (Japanese, Colour): row reads チャプター 0/1 · ダウンロード中 · チャプター：Chapter 1: Departure, then エラー after the HTTP 503; menu label その他のオプション; `v8-claude/v81-after.png`, `v81-error.png` vs `v8-queue-item.png`. Data restored, chain APK reinstalled, crash log empty.

#### V8.2 Empty-state kaomoji in the display face (T1) — `blocked` (needs the owner: no board for empty states)
Upstream `EmptyScreen` (presentation-core) draws a random kaomoji in `typography.displayMedium`; in Colour that is the italic display serif, so 「(；￣Д￣)」 renders slanted with detached strokes (`v8-claude/v8-queue-empty.png`). It shows on every empty list. Owner: keep the face in the body face, drop it in Tsuzuki themes, or draw a board for empty states? Do not invent a layout.

#### V8.3 Migration offers manga sources for a novel (and the reverse) (T2) — `done` (merged locally 2026-11-21)

Review (Codex Batch T, 2026-11-21): Passed and merged locally. The tablet standard migration setup listed the synthetic novel source. The tests cover novel, manga, mixed and unknown types, saved order and clearing choices. An online manga migration and cross-kind selection persistence were not exercised on-device. Screenshot `tablet-debug-migration-config-dot.png`. Integrated Debug and optimized Preview built, with 1,396 tests passing. Evidence `.worktrees/review-v02-main/app/build/review-evidence/v02-main/`.
`MigrationConfigScreen.Model.initSources` lists `sourceManager.getAll()` filtered only by language, so migrating a novel offers Asura Scans/MangaFire etc. as targets (`v8-claude/v8-migcfg2.png`). Filter target sources to the type of the books being migrated (novel sources for novels, manga for manga; mixed selections: say what happens — split, or offer both groups with headers). Keep saved `migrationSources` order; small dispatch in the upstream file (already touched, X4). Tests: novel-only, manga-only, mixed.

Result (Claude 2026-11-19): new `tsuzuki/migration/MigrationSourceKind.kt` — `migrationBookKinds` (isNovel of the books), `isMigrationTarget` (source kind matches; unknown kinds keep all, as before), `mergeMigrationSources` (saving one kind's selection keeps the other kind's saved ids, in their order). `MigrationConfigScreen`: `viewModel { Model(mangaIds) }`; the source list shows only the books' kind; initial selection uses the saved ids of that kind; save merges. `MigrationListViewModel` filters targets per book and `MigrateSearchViewModel` filters by the book's kind, so a saved mixed list (from before this fix) can no longer search the wrong kind. **Mixed selection** (novels and manga together): both kinds are listed, and each book still searches only its own kind. Tests: new `MigrationSourceKindTest` (5: novel, manga, mixed/unknown, merge order, clear) pass; full app suite **1,222 tests, 0 failures/errors/skips**; scoped Spotless clean. Tablet self-check (Japanese, Colour, scratch `rv5debugscratch`): RV5 novel's config lists only RV5 Network Fixture (before: 8 sources, 7 of them manga — `v8-claude/v8-migcfg2.png` vs `v83-after.png`); no crash; data restored, chain APK reinstalled. Manga side not checkable on device (only a local manga, which migration doesn't list). `Model` now depends on the ids: safe because `presentation/util/Screen` uses `uniqueScreenKey`, so each pushed config screen has its own Voyager ViewModel store. Reviewer: one novel and one online manga in a scratch library, check both lists, change one kind's selection, reopen the other and confirm its selection is kept; run a migration search for each.

#### V8.4 Migration/About polish (T1) — `done` (merged locally 2026-11-21)

Review (Codex Batch T, 2026-11-21): Passed and merged locally. Tablet Dot showed the migration cover, title and selected check. S22 Colour showed the full Japanese licences label and the licence gap in credits. The tablet used one migration target, so column alignment across multiple rows was covered by source review and Claude’s prior tablet evidence. Integrated Debug and optimized Preview built, with 1,396 tests passing. Evidence `.worktrees/review-v02-main/app/build/review-evidence/v02-main/`.
(1) Migration config: language pills (EN/JA/ALL) follow the name and sit at ragged x positions — align them in one column (X4 board row); (2) migration book list (Browse › 移行 › source) is the plain upstream row — use the Tsuzuki row/tile used on the config screen; (3) About: licences tile title truncates in the two-pane detail width — allow two lines; credits rows need a gap between a wrapped name and its licence ("…Noto" | "OFL-1.1"). Dot then Colour, tablet two-pane and phone.


Result items (1)(2) (Claude 2026-11-19): (1) `MigrationConfigScreen.SourceItem` puts name + type badge in one `weight(1f)` row (the name's and spacer's weights used to split the slack, so pills floated); EN/JA/ALL now line up at the end (`v8-claude/v84-migcfg.png`). (2) `MigrateMangaScreen.MigrateMangaItem` dispatches to new `TsuzukiMigrateBookRow` (same tile as the setup summary: cover, two-line title, accent check when selected; cover tap opens the book) in Tsuzuki themes (`v84-miglist2-sel.png`). Tablet Colour, no crash, data restored; unused `Spacer` import removed; scoped Spotless clean; AGENTS note in the worktree. Dot and phone width not seen. Full app suite **1,217 tests, 0 failures/errors/skips**.
Result item (3) (Claude 2026-11-19): `TsuzukiAboutScreen.kt` — link-tile labels allow two lines (オープンソースライ/センス now wraps instead of 「…」; the 112 dp tile still fits icon + two lines + subtitle), credits licence gets a 12 dp start gap. Tablet two-pane Colour: both fixed (`v8-claude/v84-about.png`); no crash; data restored. Scoped Spotless clean. Phone width not seen.

#### V8.5 Library search hides the All and Manga pills (T2) — `done` (merged locally 2026-11-21)

Review (Codex Batch T, 2026-11-21): Passed and merged locally. S22 optimized Preview kept All, Novels and Manga visible with an active search and hid library totals in both themes. Anime is disabled in this candidate. Screenshots `s22-preview-library-search.png` and `s22-preview-colour-library-search.png`. Integrated Debug and optimized Preview built, with 1,396 tests passing. Evidence `.worktrees/review-v02-main/app/build/review-evidence/v02-main/`.
Tablet, Japanese, Colour, `rv5debugscratch` (2 novels in the library): Library shows すべて / 小説 / マンガ / アニメ; opening the toolbar search and typing switches the pill row to **小説 / アニメ only**, so search cannot be scoped to All or Manga (`v7-claude/v7-lib-search-empty.png`). Find whether the search path builds the pills from a different source (types with results? the novel/anime tabs only?) and make it keep the same four pills as the normal Library (AY-INT item 1 also touches All). Also seen: results apply after the debounce/submit (fine), and the no-results kaomoji has the V8.2 italic-face problem. Tests: pills unchanged by an active query.


Result (Claude 2026-11-19): cause — `NovelsTab.kt` showed `TsuzukiMixedLibraryHeader` (すべて/小説/マンガ/アニメ) only while `toolbarQuery == null`; with search open it fell through to the older two-way `TsuzukiLibraryMediaSwitch` (小説/アニメ). Fix: one condition (`mixedLibrary && !selectionMode`) in the already-touched upstream file; the header's filter then also scopes the search. Tablet (Japanese, Colour): searching "RV2" keeps all four pills and filters to RV2 Listening Test (`v7-claude/v85-after.png`); no crash; data restored, chain APK reinstalled. Still open (polish): the count line under the pills keeps showing library totals ("小説 2冊") during a search, not the matches. **Fixed (Claude 2026-11-20, same worktree, uncommitted):** the count line is hidden while a search query is active (`showCounts` also needs `state.searchQuery.isNullOrBlank()`); totals come back when the search closes. Tablet: searching "RV2" filters to RV2 with no count line (`app/build/review-evidence/v85/search-no-count.png`), no crash, data restored. Selection mode still shows the old switch (unchanged). No unit test (Compose condition only); scoped Spotless clean. Full suite not re-run for this one-line change.

#### V8.6 Help links still open Tsundoku's site (T1) — `blocked` (needs the owner: where should help point?)
On the Japanese tablet, More › ヘルプ opens a browser tab on `tsundoku-otaku.github.io` ("Troubleshooting | Guides"). Nine user-facing links still point there (V5 only audited `TsuzukiLinks.kt`): `onboarding/GuidesStep.kt:59` (getting started), `about/AboutScreen.kt:218` (stock-theme credit, fine), `SettingsDataScreen.kt:85` (storage FAQ), `SettingsTrackingScreen.kt:90` (tracking guide), `WebViewScreenContent.kt:328` (Cloudflare help), `LibraryUpdateJob.kt:636` + `LibraryUpdateNotifier.kt:386` (error log / bulk-update help), `MigrateSourceTab.kt:34` + `NovelMigrateSourceTab.kt:35` (migration guide), plus the More › Help target. Owner: point them at Tsuzuki pages (which? the GitHub README/wiki), keep Tsundoku's guides (they describe the same features), or hide the help buttons. Then one constant in `TsuzukiLinks.kt` per page and one-line swaps in those upstream files.



#### STR3 Tracking settings are hardcoded English (T2) — `done` (merged locally 2026-11-21)

Review (Codex Batch T, 2026-11-21): Passed and merged locally. Japanese tracking labels verified on tablet Dot and S22 Colour. The Advanced fold uses the same localized threshold keys as the rows, verified in source. Screenshot `tablet-debug-tracking-dot.png`. Integrated Debug and optimized Preview built, with 1,396 tests passing. Evidence `.worktrees/review-v02-main/app/build/review-evidence/v02-main/`.
Found by Claude 2026-11-19 (tablet, Japanese): Settings › 同期 shows English for every novel-tracker option — "Mark chapters as read on NovelUpdates/NovelList/RanobeDB/MangaBaka", "Sync reading list", "Keep reading list status in sync with …", "Custom list mapping", "Configure list mapping", "Minimum chapters before tracking (Manga/Novels)" and their subtitles (`v8-claude/ayint-tracking.png`). `SettingsTrackingScreen.kt` (upstream, already touched) has **28 hardcoded `title`/`subtitle` literals**. Move them to `tsz_` keys with a `%1$s` tracker-name placeholder where the tracker name varies, in all 8 locales; keep the upstream diff to the literal swaps. Note: STR2's Advanced fold matches the two "Minimum chapters…" titles as English literals — if these become resources, switch that match to the same resources in the same change.

Result (Claude 2026-11-19): all user-visible English in `SettingsTrackingScreen.kt` moved to resources — 28 titles/subtitles (`tsz_track_mark_read_on` / `tsz_track_sync_list_summary` / `tsz_track_custom_mapping_summary` take the tracker name as `%1$s`; "Novel Trackers" reuses `tsz_tracker_novel_services`), plus the two info notes, the NovelList/NovelUpdates token instructions and the default token prompt, and the "Failed to fetch lists" toast: 22 new `tsz_track_*` keys in all 8 locales, placeholders checked. `TsuzukiAdvancedPreferences` now matches the two "Minimum chapters…" rows by the same resources (else they would stop folding into Advanced on Japanese devices). Tablet (Japanese): Settings › 同期 shows only tracker names and Japanese text; no crash; data restored. Full app suite **1,217 tests, 0 failures/errors/skips**. Merge notes: conflicts trivially with STR2 (adjacent lines in `TsuzukiAdvancedPreferences.kt`, strings.xml tails) and with AY-INT (adjacent line in `SettingsTrackingScreen.kt`).

#### DICT-F Japanese kanji words look up as Chinese; dictionary strings untranslated (T2) — `blocked` (changes requested on `900f081f1`, branch `tsuzuki/dict-f`)

Review (Codex Batch T, 2026-11-21): Changes requested on `900f081f1`. `ReaderViewModel.kt:1087–1091` keeps one last non-null script hint per reader session. `NovelWebViewViewer.kt:2660` overwrites it for appended/prepended chapters while the current chapter can remain on screen. The exact dictionary helpers confirm that a Korean neighbor changes the Japanese selection 図書館 to Korean for a neutral-language source. A Han-only chapter can also inherit an earlier hint. Key hints by chapter ID and route lookups using the chapter containing the selection in both renderers. Cover mixed-language neighboring chapters and a chapter without a hint. Evidence `.worktrees/review-v02-recap/app/build/review-evidence/v02-dict-f/dictionary-hint-review.txt`. Runtime reproduction pending.

Found by Claude 2026-11-19 on the tablet (chain build `d4878ac49` as `rv5debugscratch`, WebView renderer, a scratch local novel in Japanese; evidence `app/build/review-evidence/dict-claude/d-ja-*.png`):
1. **Kanji-only selection → "Chinese dictionary is not installed."** Selecting 図書館 in a Japanese chapter opens the sheet for Chinese. On the app chain only English (WordNet) is bundled — DICTIONARY-PACKS made Japanese (JMdict) an optional download — so neither pack is installed, and `ReaderDictionary` reports `missing = term.language`, which `dictionaryTerm()` (`tsuzuki/dictionary/DictionaryTerms.kt`) sets to **Chinese** for Han-only text when the book has no language tag (local novels and many sources have none). A Japanese reader is then offered the Chinese download. (Corrected 2026-11-19: the alternate *is* tried when either pack is installed; the earlier "alternate never tried" reading was wrong.) Fix: pass a script hint from the surrounding chapter (kana present → Japanese primary), which both renderers have; when neither pack is installed, offer the hinted language first. Same result in TextView and WebView, so it is not renderer-specific; the 2026-11-15 JMdict hit was on a build that still bundled JMdict.
2. **Untranslated keys on the app chain:** `review/app-chain` lacks 18 `tsz_` keys in ja and 21 in de/es/fr/ko/pt-rBR/zh-rCN — the dictionary strings (`tsz_dictionary_not_installed`, `_builtin`, `_builtin_note`, `_chinese`, `_form`, `_french`, `_german`, `_install`, `_korean`, `_lookup_error`, `_online`, `_online_notice`, …) and, outside ja, `tsz_app_update_available/_channel/_tap`. STR2's `TsuzukiLocaleCompletenessTest` fails on this chain once both are merged — translate first.
Context: bundling only English was the **owner decision of 2026-11-15** (`docs/DICTIONARY_PACKS.md` on `tsuzuki/dictionary-packs`); the optional packs need **publishing by the owner** ("Publishing (owner action)" in that doc) — until then the sheet's ダウンロード cannot fetch Japanese, so Japanese readers have no local dictionary at all.
3. Minor: the local source browse screen shows an English "Select" label (`d-local-browse.png`).
Tests: `dictionaryTerm` for Han-only + Japanese hint, missing primary pack falls back to the installed alternate; locale completeness passes on the chain.


Result (Claude 2026-11-19): (1) `DictionaryTerms.kt` adds `chapterScriptLanguage(content)` (kana in the first 4,000 characters → `ja`, else hangul → `ko`) and `dictionaryBookLanguage(source lang, hint)` (a source language that names a dictionary wins, else the hint). `ReaderViewModel.cacheNovelEstimateWords` — already called with each chapter's raw text by both renderers — records the hint (before the incognito return); `ReaderActivity.openReaderDictionary` passes `dictionaryBookLanguage(...)`. Without any hint, bare Han keeps the Chinese-first default. (2) The 18 ja / 21 de-es-fr-ko-pt-rBR-zh-rCN missing dictionary and app-update strings are translated (the `tsz_tts_*` ones are left to TTS-POLISH, which already has them). New tests in `DictionaryTermsTest` (3; 9 in the file pass); full app suite **1,256 tests, 0 failures/errors/skips** (app chain base); scoped Spotless clean; AGENTS note in the worktree. Tablet (WebView, same scratch Japanese book): 図書館 now shows 「日本語の辞書がインストールされていません。」 with EDRDG JMdict and the Japanese download (`dict-claude/dictf-after.png` vs `d-ja-dict.png`). (3) The browse "Select" / "N selected" chip (`ui/browse/source/browse/BrowseSourceScreen.kt`, two literals) uses new `tsz_action_select` / `tsz_selected_count` in 8 locales; tablet: 選択 → 1 件選択. Not done: installing JMdict needs the owner to publish the packs. Merge after TTS-POLISH: both append strings at the end of the same files (trivial conflict).

#### DICT-HINT-F Scope dictionary language to the selected chapter (T2) — `review` (fix `69f4d34d5` on `tsuzuki/dict-hint-f` for the changes requested on `9ec29c270`; app-chain decisions also pending)

Build on the DICT-F translations and routing helpers. Keep script hints keyed by chapter ID and resolve using the chapter containing the selection. A preceding or appended chapter must leave the active selection’s hint alone. A chapter with no script hint must reach the existing unknown-language fallback. Check a Japanese chapter with a Korean neighbor in scroll WebView, a Han-only chapter after a chapter with a script hint, and the TextView selection path.

Independent pre-review (Codex Batch T). In the TextView action, `getSelectedText()`, paragraph index and chapter ID all read the scroll cursor’s `currentChapterIndex`. The scroll cursor tracks the first chapter still intersecting the top of the viewport. Selecting text in an appended chapter visible farther down can therefore return no text from the earlier block. Resolve the selected block using the action’s TextView or the loaded block whose `hasSelection()` is true. In WebView, the script can identify the selected chapter but the dictionary action still passes the cursor chapter’s name. Keep the paragraph, chapter ID and chapter name from the same selection. These are source findings, with adjacent-chapter device checks still needed.

Result (Claude, 2026-11-21): `ChapterScriptHints` (in `DictionaryTerms.kt`) keeps one hint per chapter id; recording a chapter again replaces or clears its hint, and a chapter with no hint (or an unknown id) gives null, so the existing unknown-language fallback applies. `ReaderViewModel.novelScriptHints` replaces the single session `novelScriptLanguage`; `openReaderDictionary` takes the selection's chapter id. **TextView:** the current chapter's id (the selection lives in that chapter's block). **WebView:** the selection script returns `para|chapterId` from the enclosing `tsundoku-chapter[data-chapter-id]` (falls back to the current chapter when the selection is outside one); the Remember path still reads the paragraph index from the same header. Tests 12/12 `DictionaryTermsTest` (+ Japanese chapter with a later Korean neighbour keeps 図書館 Japanese; Han-only chapter after a hinted one gets no hint and goes Chinese-first; unknown/null chapter; reload replaces). **Tablet** (temporary instrumentation probe, not committed; scratch `dicthintqa` = this branch; a fixture source with language "other", a Japanese chapter then a Korean chapter, scroll WebView, the Korean one loaded through `loadNextChapter()` in the same reader session): hints ja/ko kept per chapter; `openReaderDictionary("図書館", chapter 1)` → Japanese, `("도서관", chapter 2)` → Korean, `("図書館", null)` → Chinese (the unknown-language fallback); crash buffer empty; scratch and probe uninstalled. **Real selection path** (second probe run): with both chapters in the DOM, the Japanese one current and the Korean one appended after it (the reported case), a JS range on 図書館 inside chapter 1's `tsundoku-chapter` element plus the viewer's own dictionary action (`onRememberSelectedText(forDictionary = true)`) → Japanese. Note: once the Korean chapter becomes current in scroll mode, the reader unloads chapter 1, so "select in an earlier chapter while a later one is current" doesn't arise there.

Fix for the review (Claude, 2026-11-22, `69f4d34d5`): **TextView** — `NovelViewer.selectionChapter()` is the loaded chapter whose block `hasSelection()` (the scroll cursor's chapter only when no block has one); the dictionary action takes text, paragraph, chapter name and id from that one block, `getSelectedText`/`getSelectedParagraphIndex`/new `getSelectedChapterName` use it, and `clearTextSelection` clears every block. Remember (TextView) now saves the quote under the selected chapter's name too (`ReaderActivity`, one-line swap). **WebView** — the chapter name is looked up from the selection's `data-chapter-id` among the loaded chapters (fallback: current chapter), so name, id and paragraph come from the same selection; the Remember path keeps it in `pendingChapterName` until the quote is saved. Compile, `DictionaryTermsTest`, full app suite **1,259 tests, 0 failures/errors**; Spotless clean (its edits outside the three files restored). **Tablet** (temporary probe, not committed; scratch `dicthintqa` = `69f4d34d5`; TextView, infinite scroll, Japanese chapter 1 then Korean chapter 2 appended by scrolling, back at the top): scroll cursor on chapter 1 (`cursor=0`), 도서관 selected in chapter 2's block, the TextView's own dictionary action → **Korean, chapter 'Ch. 2 ko', paragraph 0**; crash buffer empty; scratch uninstalled. Before the fix this read chapter 1's block, found no selection and opened nothing. Remember with an appended-chapter selection was not run on the device — reviewer: select in chapter 2 while chapter 1 is at the top, Remember, the quote is listed under chapter 2.

#### READER-JP-FONT Japanese text in the TextView reader ignores the serif partner font (T1) — `done` (merged locally 2026-11-21)

Review (Codex Batch T, 2026-11-21): Passed and merged locally. A Japanese chapter from the localhost test source visibly used the serif partner in the tablet Dot TextView reader. The synthetic EPUB did not load and the Colour/WebView comparison was not repeated. Screenshot `tablet-debug-reader-network-native-dot.png`. Integrated Debug and optimized Preview built, with 1,396 tests passing. Evidence `.worktrees/review-v02-main/app/build/review-evidence/v02-main/`.
Found by Claude 2026-11-19 (tablet, chain build): with Literata selected, a Japanese chapter renders in the system **sans** face in the TextView renderer and in Noto Serif JP in WebView (`dict-claude/d-ja-tv.png` vs `d-ja-reader.png`). `NovelViewer.kt:1941` loads `R.font.lnr_literata` with `ResourcesCompat.getFont` and no Japanese fallback; the AGENTS rule pairs every face with a Japanese partner, and `presentation/tsuzuki/reader/TsuzukiReadingTypography.kt` already builds Literata + `lnr_jp_notoserif` with `Typeface.CustomFallbackBuilder` (API 29+) for Compose. Fix: build the TextView typeface from the same pairing (one-line swap in the upstream file, helper in `tsuzuki/`), and the other reader faces: in `NovelViewer.kt` only `literata` loads a bundled font; 明朝 (`serif`) maps to `Typeface.SERIF`, and on the tablet Japanese renders in a proper serif there (`dict-claude/d-ja-tv-serif.png`), so **only Literata needs the partner**; ゴシック/等幅 map to system sans/monospace, which already render Japanese acceptably; custom font files get no partner (leave as is or add the serif/sans partner by face). Device check: same Japanese chapter in both renderers, Dot and Colour.

Result (Claude 2026-11-19): `TsuzukiReadingTypography.kt` now exposes `readingTypeface(resources)` — the existing Literata + `lnr_jp_notoserif` `CustomFallbackBuilder` pairing (system serif after it), cached, null below Android 10 — and its Compose family wraps the same typeface. `NovelViewer.kt` Literata branch tries it first, then the old plain Literata, then `SERIF`. Tablet (Japanese chapter, TextView, Literata): Japanese now renders in Noto Serif JP instead of the system sans (`dict-claude/rjf-ja.png` vs `d-ja-tv.png`); no crash; data restored, chain APK reinstalled, scratch book removed. **Note for the owner/reviewer:** the bundled `lnr_jp_notoserif.ttf` is a static **SemiBold (usWeightClass 600)** subset, so Japanese body text looks heavier than Literata Regular (also in the Compose quote/notebook tiles); a Regular (400) subset made the same way (`subset-font`, OFL) would match better — asset change, not done here. Full app suite **1,217 tests, 0 failures/errors/skips** (no new unit test: font loading needs a device); scoped Spotless clean; AGENTS note added in the worktree.

#### V9 Cold start + baseline profile (Claude) — cold start `done` 2026-11-07 (S22 median 352.5 ms over 10, see PERFORMANCE.md); baseline profile re-collect still to do
P2.1: phone cold start, `am start -W`, 10 runs, compiled vs not, before/after numbers in `docs/PERFORMANCE.md`.
P2.4: re-collect the baseline profile on the frozen main (`-PtsuzukiProfileConnected=true`), commit it.

#### V10 Signing continuity (Claude + owner) — `ready` after V0, V4
Pull the phone's installed release APK (`pm path io.github.manojx2005.tsuzuki`) and compare its certificate SHA-256
(`apksigner verify --print-certs`) with an APK signed by the release key; record the phone's installed `versionCode`.
Same cert and version code below the new one: in-place upgrade works. Different cert: **stop**, owner decides
(sign v0.1 with the phone's key, or backup → uninstall → install → restore on the phone).

#### V11 Release candidate (Claude + owner) — RC2 built 2026-11-07, owner testing on their phone
**RC6 (current, 2026-11-08):** RC4 + Dot as the default theme (onboarding in Dot) + infinite scroll on by
default. RC5 (Dot only, run 42431102954) was cancelled before it finished.
**RC4 (superseded, 2026-11-08):** commit `8be1ba942` (RC3 + chapter-end Next chapter fix), Actions run 42429219360,
0.1.0 (24), cert `ffcdc743…f6c7`. Installed on tablet + S22 (release app id): launches, crash buffers empty. Files:
`C:/Users/Manoj/Desktop/Tsuzuki-v0.1.0-rc4/tsuzuki-rc-8be1ba942/`. Tag only after the owner's OK.
**RC3 (superseded, 2026-11-08):** commit `000cb1ce6` (main + V7.3–V7.5 + V8 More insets), Actions run 42423861828,
0.1.0 (24), cert `ffcdc743…f6c7`. Installed on tablet + S22 (release app id): launches, Japanese onboarding renders,
crash buffers empty; reading not yet checked in this build (fresh install stops at the system folder picker). Owner
is testing it on their phone (debug-signed install → backup, uninstall, install, restore). Files:
`C:/Users/Manoj/Desktop/Tsuzuki-v0.1.0-rc3/tsuzuki-rc-000cb1ce6/`. V7.6 polish → 0.1.1. Tag only after the owner's OK.
**RC2 (superseded):** commit `2b9911b76` (adds the app update notification), Actions run 42375424770; same checks as RC1
pass (0.1.0 (24), cert `ffcdc743…`, 0 Firebase classes, `TsuzukiAppUpdateJob` kept by R8). Files + checklist:
`C:/Users/Manoj/Desktop/Tsuzuki-v0.1.0-rc2/tsuzuki-rc-2b9911b76/`. RC1 below is superseded.
**RC1:** commit `0025beca7`, built and signed by GitHub Actions (`release.yml` manual run 42373139062, no publish).
Verified: `io.github.manojx2005.tsuzuki` 0.1.0 (24), cert `CN=Tsuzuki` SHA-256 `ffcdc743…f6c7`, 0 Firebase classes,
baseline profile packaged. APKs + SHA256SUMS + owner checklist in `C:/Users/Manoj/Desktop/Tsuzuki-v0.1.0-rc1/`.
Local signed builds no longer work (the key password left this PC, owner 2026-11-07): build RCs with
`gh workflow run release.yml` and `gh run download`.
Build `assembleRelease -Penable-updater` locally with the release key. Tablet + S22: install the release app id,
smoke test Home, Library, both readers, TTS, downloads, backup + restore, Browse, extensions, settings, widgets.
Owner's phone (as approved in V0): fresh backup copied to the PC first, then upgrade in place over the real data;
check library, progress, shelves, history and prefs survived; crash buffer empty on all three.

#### V12 Tag + publish (Claude + owner) — `done` 2026-11-08
**Published:** https://github.com/Manojx2005/Tsuzuki/releases/tag/v0.1.0 — tag `v0.1.0` → `5a65307c9` (RC6 code +
changelog), run 42432617196. Not draft/pre-release; `/releases/latest` returns v0.1.0; 5 APKs with SHA-256 lines in the
notes; arm64 APK verified: cert `ffcdc743…f6c7`, 0.1.0 (24). Tachiyomi's 2016 local `v0.1.0` tag (`bba727ec8`, never on
origin) was deleted first. Still open: the in-app updater end-to-end check (needs a later tag), lifting the freeze.
Owner OK → `git tag v0.1.0` on frozen main → push **only that tag** (`git push origin v0.1.0`; never `--tags`: the
repo holds Tsundoku's `v0.8.4`–`v0.9.2`, which would look newer to the updater). Watch the workflow, check the
release assets and SHA-256 file. On the tablet, install a build reporting an older version and confirm the in-app
updater finds v0.1.0 and the NewUpdate screen renders (V8's deferred item). Then lift the freeze, merge the waiting
branches, and start v0.2 (anime).

#### Owner permissions for v0.1 (answers recorded 2026-11-07)
- **Repo:** `Manojx2005/Tsuzuki` is public (owner; Claude checked the API returns 200 without auth). Releases go there;
  `GITHUB_REPO` stays.
- **Signing:** owner: "do what is best". V10 signs v0.1 with whichever key signed the phone's installed release, so it
  upgrades in place; if that key isn't the Actions secret, Claude asks the owner to update the secrets before V12.
  Item 2 (secrets exist) is checked in V10.
- **Watch switch:** keep as a teaser (V3 rewritten).
- **Devices:** tablet + S22 free to use (any build type, `am kill`, scratch installs with the synthetic backup, widgets,
  temporary EPUB/mass-import files cleaned up after); one extension install/update/uninstall on tablet/S22; S22 to
  English once and back; owner's phone upgraded in place at V11 after a backup is copied to the PC.
- **Push:** Claude pushes `main` to origin after each verified merge, and pushes only the `v0.1.0` tag at V12 (never
  `--tags`).
- **Firebase:** delete `app/google-services.json` in V4.
- **Publish:** Claude stops after V11 and asks the owner before the V12 tag push.

### Batch R — the long list (written 2026-11-07 by Claude)

Same run rules as "Overnight batch Q — run rules" above (worktree per card from current `main`, Needs merged
first, commit on the card branch, Result + `review`, blocked → next row). Boards: `docs/design/boards/INDEX.md`
(open `png/<page>/<Board>.png` + `dot/` + `html/`) unless the card names another. Priority order from the owner:
stable → smooth/fast → design complete → Mihon parity → new features; anime has its own lane.

**Owner 2026-11-07: v0.1 ships without anime (anime = v0.2); checklist in WORKPLAN Phase 7.** Lane 2 cards R6 and R7 come first.
Anime lane rule until v0.2: no anime entry point may appear in a release build (gate with `BuildConfig.BUILD_TYPE == "debug"`, like the anime debug screen).

**Lane 1 — anime (Codex thread 1, in order):** superseded 2026-11-08 by the table "Anime lane order from 2026-11-08" in the Anime lane A section: A2.4 → A3.0 → A3.1 → A3.2 → R-A2 → A3.3 → A3.4 → R-A3 → A3.5 → A3.6 → R-A1 → R-A4 → A3.7 → A3.8 → A3.9 → A4.1 → A4.2.
**Lane 2 — app (Codex thread 2, in order):** R1 → R2 → … → R16 → R17. Lane 2 never touches anime files, and lane 1
never touches R-card files; both threads: one Gradle build at a time each.
**Lane 3 — Claude:** reviews, device checks and the C-list at the end of this section.

| # | Card | Lane | Needs | Tier |
|---|---|---|---|---|
| 1 | R1 Large-library performance pass | 2 | — | T3 |
| 2 | R2 Process death + rotation audit | 2 | — | T3 |
| 3 | R3 Mid-word wraps + tile labels sweep | 2 | — | T2 |
| 4 | R4 Menus: icons, groups, destructive row | 2 | — | T2 |
| 5 | R5 Tablet Library + Tablet Settings vs boards | 2 | — | T3 |
| 6 | R6 Colour theme parity pass | 2 | R3 | T2 |
| 7 | R7 Japanese layout pass | 2 | R3 | T2 |
| 8 | R8 Downloads screen + queue vs board | 2 | — | T2 |
| 9 | R9 Widgets: Continue reading + Listening vs board | 2 | — | T3 |
| 10 | R10 Tracker accounts + update schedule vs boards | 2 | — | T2 |
| 11 | R11 Reading estimates (time left, speed) | 2 | — | T3 |
| 12 | R12 Source health | 2 | — | T3 |
| 13 | R13 Dictionary lookup (system-provided) | 2 | — | T3 |
| 14 | R14 Merge sources for one book | 2 | — | T4 |
| 15 | R15 Backup: scheduled export + restore polish (Backup board) | 2 | — | T3 |
| 16 | R16 Upcoming: notify on expected release (optional) | 2 | — | T2 |
| 16b | R17 Tablet reading line length (cap ~600 dp, centred) | 2 | — | T3 |
| 17 | R-A1 Anime backups: Aniyomi import + our 8000-range fields | 1 | A2.4 | T4 |
| 18 | R-A2 Player settings + sheets (subtitles, audio, speed, decoder) | 1 | A2.4 | T3 |
| 19 | R-A3 Anime in Updates + "Airing today" strip | 1 | A2.4 | T3 |
| 20 | R-A4 Anime episode downloads (third engine) | 1 | R-A2 | T4 |

#### R1 Large-library performance pass (T3) — `done` (merged into main via `review/night5` → `3fa896b3e`, 2026-11-07; see V1)
Use Y4's `scripts/generate_synthetic_backup.py` (500 books, 30 shelves, 50 k chapters). Codex: no devices — profile
on the JVM side only: add Macrobenchmark-free unit/perf tests for the hot paths (library grouping/sorting/filtering,
shelf counts, Updates mapping, Home stack queries) with the synthetic data shape, find anything O(n²) or done on
the main thread, fix the top issues (stable keys/contentType already done in N30 — check for regressions),
document numbers before/after in Result. Claude then restores the backup into a **scratch** install and records
`gfxinfo framestats` for Home, Library (each tab), Updates, History, Browse.
**Result (Codex, 2026-11-07):** Verified working diff on `tsuzuki/r1` at `.worktrees/r1` (base `d5ff042a6`). Per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` passed (924 tests, 0 failures). Card-branch commit needs owner or Claude after automatic approval rejected that operation under AGENTS.md.
- `TsuzukiHomeViewModel.kt` separates badge emissions from stack loads and skips sorting fallback books when the stack is full. In the JVM flow test, 20 badge changes after first load cause 0 extra stack builds (previous pipeline: 20). With a full nine-book stack that avoids up to 180 chapter-list queries. `TsuzukiHomeLogicTest.kt` guards the load count.
- Upstream `UpdatesViewModel.kt` moves media filtering into the IO mapping flow, keeps the downloaded filter applied, discards stale media-filter results, and maps date headers in one pass. At 50,000 rows, date conversion calls fall from 100,000 to 50,000. `TsuzukiUpdatesTwoPaneTest.kt` checks row order, dates and grouping on that shape.
- `LibraryGrouping.kt` exposes the existing linear category grouping for JVM tests. Upstream `LibraryViewModel.kt` calls it; grouping, sorting and filters already run on IO. `LibraryGroupingTest.kt` verifies ordering of shelves, 500 books across 30 shelves, title filtering and shelf counts. N30 keys and content types remain in place.
- Ran Y4's generator. Fixture: `.worktrees/r1/build/r1-synthetic-500.tachibk`. JVM numbers are operation counts, not device frame timings. Claude: restore into a scratch install, compare Home stack and badge updates, Library shelves, Updates media and downloaded filters, History and Browse using `gfxinfo framestats` on phone/tablet.

#### R2 Process death + rotation audit (T3) — `done` (merged into main via `review/night5` → `3fa896b3e`, 2026-11-07; see V1)
WORKPLAN P1.1. For every Tsuzuki screen and sheet with user state (Home stack index, Library tab/shelf/filter/
selection, details sheets incl. Q3 save range, reader quick sheet tab, All reader settings tab, Browse media/
section, search queries, Q9 selection, Updates two-pane selection), make sure state survives rotation and
process death (`rememberSaveable` / ScreenModel saved state, no Parcelable leaks). List each screen + what was
missing in Result. Tests for any saver you write. Claude verifies with `adb shell am kill` on both devices.
**Result (Codex, 2026-11-07):** Verified working diff on `tsuzuki/r2` at `.worktrees/r2` (base `d5ff042a6`). Per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` passed (923 tests, 0 failures). Card-branch commit needs owner or Claude after automatic approval rejected that operation under AGENTS.md.
- Home stack index and selected tablet book were already saveable. Browse's media/section pager already saved its page. Upstream `BrowseTab.kt` now keeps both extension search drafts.
- Upstream `LibraryViewModel.kt`, `LibraryTab.kt`, and `NovelsTab.kt` retain the shelf index, draft and submitted search, filters sheet, and selected IDs. The root Library tab and persistent filter values already use saved state or preferences.
- Upstream `MangaViewModel.kt` and `MangaScreen.kt` retain chapter selection, chapter filter/tracking/cover sheets, and the scanlator filter sheet. Upstream `MangaToolbar.kt` retains the Save Chapters sheet, whose mode and range already use saveable state.
- Upstream `ReaderViewModel.kt` and `ReaderActivity.kt` retain the settings dialog and wait for the correct renderer before showing it. Quick and All settings tabs were already saveable. `TsuzukiReaderHooks.kt` retains Contents visibility. Reader page/chapter progress already uses the reader's saved state.
- Upstream `GlobalSearchScreen.kt`, `NovelGlobalSearchScreen.kt`, `GlobalSearchViewModel.kt`, `NovelGlobalSearchViewModel.kt`, and `SearchViewModel.kt` restore the draft and source filter and rerun the submitted query.
- Upstream `UpdatesTab.kt` and `UpdatesViewModel.kt` retain selected chapter IDs, media filter and loaded page limit. Upstream `UpdatesScreen.kt` holds the two-pane preview key outside the tablet-only branch and passes it to `TsuzukiUpdatesTwoPane.kt`.
- `TsuzukiSavedScreenState.kt` and `TsuzukiSavedScreenStateTest.kt` limit each saved selection to 8,192 IDs. Larger selections restore empty to avoid a partial bulk operation. Transient confirmations, in-flight work and reader find results are recreated on demand. R1 and R2 both change UpdatesViewModel; preserve both when merging.
- Claude device review: background and kill, reopen, and rotate on the phone and tablet with each named state active. Verify a custom chapter range, unsent draft vs submitted search, Q9 selection, both reader renderers, and a tablet preview selected beyond the first Updates page. JVM verification only.

#### R3 Mid-word wraps + tile labels sweep (T2) — `done` (merged into main via `review/night5` → `3fa896b3e`, 2026-11-07; see V1)
Found 2026-11-07: details action tiles wrap mid-word with 4 tiles ("TRACKIN / G", "IN / LIBRARY") on 360 dp.
Grep every Tsuzuki tile/pill/button label for `maxLines = 2` without word-safe handling; for equal-width tiles use
one line + ellipsis or a shorter label (8 locales), never break inside a word (`LineBreak.Simple` /
`Hyphens.None`, or `softWrap` off for single words). Add the details 4-tile case to `TsuzukiNarrowPreviews`.
**Result (Codex, 2026-11-07):** Verified working diff on `tsuzuki/r3` at `.worktrees/r3` (base `d5ff042a6`). Per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` passed (921 tests, 0 failures). R6 and R7 need the card-branch commit. Automatic approval rejected card commits under AGENTS.md.
- Upstream `MangaInfoHeader.kt` makes themed details action tiles single-line with ellipses and gives the labels more room by reducing the button's inner horizontal padding. Stock treatment and the full accessible labels are preserved.
- `TsuzukiPill.kt`, `TsuzukiSegments.kt`, and `TsuzukiNav.kt` apply single-line ellipses to shared control labels. `TsuzukiBrowse.kt`, `TsuzukiPageColours.kt`, `TsuzukiMoreScreen.kt`, `TsuzukiAboutScreen.kt`, and `TsuzukiBackupTiles.kt` apply the same to their action labels. Source names, book titles, and other body text keep their multiline treatment.
- `TsuzukiNarrowPreviews.kt` adds the real four-action details row, using the existing translated labels. The preview compiled. Claude: inspect at 360 dp in Dot and Colour, EN and JA, including 130% and 200% font sizes. On the phone open a library book with an expected update and a WebView action to check all four buttons and TalkBack labels.

#### R4 Menus: icons, groups, destructive row (T2) — `done` (merged into main via `review/night5` → `3fa896b3e`, 2026-11-07; see V1)
Board `17-drafts` Menus / MenuPick (status table: "Not yet: item icons, groups, accent destructive row, screens
using Material's menu directly"). Extend the shared Tsuzuki dropdown (`components/DropdownMenu.kt` hook +
`theme/tsuzuki/TsuzukiMenu.kt`) with optional leading icons, group dividers/labels and a destructive (accent) row;
then switch the remaining Material `DropdownMenu` call sites in Tsuzuki themes to it (list them).
**Result (Codex, 2026-11-07):** Verified working diff on `tsuzuki/r4` at `.worktrees/r4` (base `d5ff042a6`). Per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` passed (921 tests, 0 failures). Card-branch commit needs owner or Claude after automatic approval rejected that operation under AGENTS.md.
- `TsuzukiMenu.kt` supports icon rows, optional group captions and dividers, and accent destructive text/icons. Upstream `AppBar.kt` uses the shared row and groups overflow actions. Upstream `DropdownMenu.kt` styles anchored popups while preserving their stock position and sizing, and accepts icons for nested rows.
- Upstream `LibraryToolbar.kt` separates library operations and imports, and keeps clear-category last. Upstream `BrowseSourceToolbar.kt` labels the display-mode picker and styles the radio labels. Upstream `DownloadQueueScreen.kt` labels the sort picker and adds sort icons to nested rows.
- The remaining direct Material dropdown containers now use the shared anchored popup in upstream `ImportEpubDialog.kt`, `ImportEpubScreen.kt`, `MassImportDialog.kt`, `SettingsAdvancedScreen.kt`, `SettingsNovelDownloadScreen.kt`, `SettingsTrackingScreen.kt`, `DuplicateDetectionScreen.kt`, `MigrationListScreenContent.kt`, and `MigrateMangaScreen.kt`. Their row actions and custom content remain in their owners.
- Claude device review: compare Library overflow groups, Catalogue display mode, Downloads sorting and cancel row, then an import category and tracker list picker in Dot and Colour. Check edge anchoring and nested menu focus.

#### R5 Tablet Library + Tablet Settings vs boards (T3) — `done` (merged into main via `review/night5` → `3fa896b3e`, 2026-11-07; see V1)
Boards `11-tablet/TabletLibrary`, `TabletSettings` (+ dot). Compare with the current tablet layouts (rail + Library
grid; Settings two-pane) and fix only differences (list them). Real data only.
**Result (Codex, 2026-11-07):** Verified working diff on `tsuzuki/r5` at `.worktrees/r5` (base `d5ff042a6`). Per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` passed (921 tests, 0 failures). Card-branch commit needs owner or Claude after automatic approval rejected that operation under AGENTS.md.
- `TsuzukiHomeScreen.kt` shows six covers on the TabletLibrary reading shelf when its content has 670 dp, with five on narrower landscape panes. The named board is the Home reading grid with a details pane. The separate Library tab still uses the user's configured grid settings. The rail and portrait Home layout already matched the intended structure.
- `TsuzukiSettingsMain.kt` caps the section list at the board's 360 dp, gives rows the 52 dp minimum and fills the selected row's tile within the accent outline. The existing shared rail and appearance pane remain the entry points.
- Kept the available real-data tiles and settings. Device review: verify Dot and Colour on tablet portrait and landscape. In landscape confirm the six-cover row and details selection, then check Settings list width, selected outline/fill and preference navigation. Check the separate Library grid and rail at a large font scale.

#### R6 Colour theme parity pass (T2) — `done` (merged via `review/night6`, 2026-11-07) (R3 is in main)
Every Tsuzuki screen in **Colour** at 360 dp and tablet: sentence case, Instrument Serif headings, book-palette
accents, no Dot-only artefacts (uppercase, grey `#8A8A8A`, red). Use the Q8 narrow previews in Colour; fix
differences; list per screen.
**Result (Codex, 2026-11-07):** `c004ea917` on `tsuzuki/r6` (`.worktrees/r6`).
- Upstream `LnrTypography.kt` selects the italic Instrument Serif face in the paired Android typeface. Headings previously rendered upright even though Compose requested italic. The Noto Serif JP fallback stays in the family.
- `TsuzukiNarrowPreviews.kt` sets the board's night palette and mirrors the app's content locals. Added tablet portrait and landscape variants for Home and the Library, Settings and Updates samples. `docs/design/README.md` records the coverage.
- The headless 360 dp matrix rendered Home, Library, Browse, Details, More, novel and manga reader chrome, reader settings, Settings, Updates and History samples in both themes. Colour showed italic display text, sentence-case controls and gold default accents. Small uppercase section labels and cover media badges match their Colour boards. Tablet Home showed italic headings in both orientations. Evidence: `.worktrees/r6/app/build/r6-preview-after/`.
- Per-file Spotless passed twice. `assembleDebug` and `testDebugUnitTest` passed with 933 tests and zero failures.
- Claude device check: compare live cover accents and full Library, Settings and Updates panes on the tablet. The night preview uses the board palette. The initial day-mode sample showed low contrast where light Material ink met dark Tsuzuki tokens, so also check System, Light and Dark on the device. Full runtime coverage for screens outside the preview matrix is pending.

#### R7 Japanese layout pass (T2) — `done` (merged via `review/night6`, 2026-11-07) (R3 is in main)
Devices are Japanese. Status table: translated layouts never visually checked. Run the narrow previews with the
`ja` locale; fix truncation, mixed fonts (Japanese must use the partner font via `MaterialTheme.typography`) and
any hard-coded English in Tsuzuki screens (move to strings, 8 locales).
**Result (Codex, 2026-11-07):** `e1502c742` on `tsuzuki/r7` (`.worktrees/r7`).
- `TsuzukiNav.kt` wraps phone labels inside the existing five cells, up to three lines at large font scales. The selected pill grows with them. `TsuzukiMoreScreen.kt` allows two-line toggle labels.
- `TsuzukiHomeScreen.kt` formats the date using the app locale and localizes the new-chapter count. `TsuzukiHistoryItem.kt` uses the existing chapter-number translation and allows its timestamp line to wrap.
- `i18n/` adds the count in eight locales. `i18n-novel/` supplies translations for the existing Manga, Novels, Translate and three reading-status labels. `docs/design/README.md` records the Japanese preview coverage.
- Reviewed 360 dp Japanese previews at 100%, 130% and 200% in Dot and Colour with the R6 preview harness. Evidence: `.worktrees/r7/app/build/r7-preview/`. Long book titles and Details action tiles retain their designed ellipses. English shelf names in the Library preview are fixtures. Other upstream novel strings still use their existing fallback.
- Per-file Spotless passed twice and XML checks covered seven keys in eight locales. `assembleDebug` and `testDebugUnitTest` passed with 933 tests and zero failures.
- Claude device check: select Japanese on the S22 and tablet. At standard and large text scales, check the phone navigation, More toggles, Browse media choices, Home date/new chip, History timestamp and reader controls. Check the tablet rail and remaining runtime settings.

#### R8 Downloads screen + queue vs board (T2) — `done` (merged via `review/post-v01`, 2026-11-08)
**Claude review 2026-11-07 — rework:** the queue UI is good, but upstream `Downloader.kt` (+98/−48) rewrites the scheduler for every download: `collectLatest` → `collect` with `joinAll()` inside the collector (a cancelled job stuck in blocking IO now stalls all scheduling), the per-run job map became a class field, and `distinctUntilChanged` was dropped. Keep Mihon's `collectLatest` loop. Put source pause and targeted retry in `tsuzuki/download/` (e.g. filter paused sources inside `activeMangaDownloads`, and have retry reset statuses + bump a flow the existing `combine` already watches). Target: `Downloader.kt` under ~20 changed lines; drop the `joinAll` wait unless a test shows a real write race.
Board `Downloads` (+ `DownloadSheet`). Compare the Tsuzuki download queue (N10 Compose rows) with the board: group
headers, per-group pause/resume, progress, error rows with retry; fix differences.
**Result (Codex, 2026-11-07):** Verified working diff on `tsuzuki/r8` at `.worktrees/r8` (base `d5ff042a6`). Per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` passed (923 tests, 0 failures). Card-branch commit needs owner or Claude after automatic approval rejected that operation under AGENTS.md.
- `TsuzukiMangaDownloadQueue.kt` gives each manga source header a pause/resume button, shows paused status and provides a retry on failed rows beside the live progress and error reason. Upstream `DownloadQueueScreen.kt` wires the actions and adds a Retry button to the novel error section. Upstream `DownloadQueueViewModel.kt` snapshots novel statuses so a failure or retry updates the card even without a membership change.
- `DownloadQueueScheduling.kt` and `DownloadQueueSchedulingTest.kt` select one runnable manga download per available source and verify paused-source ordering and targeted worker startup. Upstream `Downloader.kt` re-evaluates when a group changes or a failure is retried, waits for old attempts to finish writing before restarting, and leaves paused groups out of scheduling. Upstream `DownloadManager.kt` exposes the controls. Upstream `DownloadJob.kt` distinguishes a targeted restart from global Start, so the former leaves other failed items alone.
- Manga groups retain source grouping, novel groups retain book grouping, and source pauses are session-scoped like existing novel pauses. The board's video rows, sample storage and retry countdown were not added. R4 and R8 both edit `DownloadQueueScreen.kt`; preserve both when merging.
- Claude device review: on Dot and Colour queue from two manga sources and two novels, pause one source and one novel while in flight, confirm others advance, resume, then trigger two errors and retry one. Check retry while the source or whole queue is paused, reordering, and removing items during progress.

**Rework result (Codex, 2026-11-07):** `32dff0c4d` on top of `917f06958` (`.worktrees/r8`).
- Upstream `Downloader.kt` restores `collectLatest`, distinct active selections and the per-run job map. Cancelled sources release the scheduler without a group join. Queue completion signals let a waiting source resume once the previous attempt exits.
- `DownloadQueueScheduling.kt` owns the paused-source and retry state, filters source queues and wakes the existing scheduler when the queue list itself stays the same. `DownloadQueueSchedulingTest.kt` adds checks for targeted retry while paused and resuming a source as its cancelled attempt exits.
- The cumulative `Downloader.kt` change is +36/−20 against the base, above the approximate 20-line target. The extra hooks keep retry and rapid resume from starting a second attempt while the same download is still finishing.
- Per-file Spotless passed twice. `assembleDebug` and `testDebugUnitTest` passed with 925 tests and zero failures. Repeat the device checklist above, including a rapid pause/resume while another source continues downloading.

#### R9 Widgets vs board (T3) — `done` (merged into main via `review/night5` → `3fa896b3e`, 2026-11-07; see V1)
Board `Widgets`. Continue-reading widget and N28 Listening widget: match sizes/styles, Dot + Colour (follow app
theme), real data only, tap targets open the right screen. Claude places them on the tablet and checks.
**Result (Codex, 2026-11-07):** Verified working diff on `tsuzuki/r9` at `.worktrees/r9` (base `d5ff042a6`). Per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` passed (923 tests, 0 failures). Card-branch commit needs owner or Claude after automatic approval rejected that operation under AGENTS.md.
- `TsuzukiReadingWidget.kt` and `tsz_reading_widget_info.xml` give Continue a 4×2 target, keep live stats when resized taller, and render chapter-count progress with Dot dots or a Colour bar. Continue uses the existing resume query, opens Details if the book has no resume target, and the taller New tile opens Updates. `TsuzukiReadingWidgetTest.kt` covers missing and inconsistent progress data.
- `TsuzukiListeningWidget.kt` fits the 4×1 target and opens the active reader chapter from live TTS IDs. Its transport still sends the existing service action.
- `TsuzukiWidgetTheme.kt` reuses theme tokens and watches app theme, accent and cover-colour changes. Upstream `App.kt` starts that watcher in the main process. Widget typography uses Glance system faces. Time estimates, anime content, and the launcher preview artwork are outside this diff.
- Claude device review: place both widgets on the tablet, resize Reading short/tall and Listening to one row, toggle Dot/Colour and cover colour, test an in-progress chapter, a completed book, the taller New tile, and Listening chapter/transport with TTS paused, playing and stopped. Verify the two widgets' distinct reader destinations when placed together.

#### R10 Tracker accounts + update schedule vs boards (T2) — `done` (merged via `review/post-v01`, 2026-11-08; Claude restored the upstream token dialog)
**Claude review 2026-11-07 — rework:** `SettingsLibraryScreen.kt` (+72/−41) and `SettingsTrackingScreen.kt` (+45/−24) wrap upstream preference rows in `if (!themed)` and re-indent them, which AGENTS.md rules out. Instead, put one dispatch at the top of each group builder (`if (isTsuzukiStyle()) return tsuzukiLibraryUpdateGroup(...)`) that builds the whole themed group in `presentation/tsuzuki/settings/`, and restore the upstream list byte-for-byte. Also check that the themed group keeps the smart-update manga restrictions (the current themed branch replaces them with Download new chapters).
Boards `TrackerAccounts`, `UpdateSchedule`. Settings › Tracking and Library update schedule screens: compare and
fix; never show tokens; no new network calls.
**Result (Codex, 2026-11-07):** Verified working diff on `tsuzuki/r10` at `.worktrees/r10` (base `d5ff042a6`). Per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` passed (921 tests, 0 failures). Card-branch commit needs owner or Claude after automatic approval rejected that operation under AGENTS.md.
- Novel tracker accounts come first and show locally linked library entries. Token entry is masked and opens empty. No additional network calls. The Update Schedule uses the existing interval, included shelves, download option, device and library restrictions, and scheduler. Detailed source throttling stays on its existing screen.
- Claude device review: compare Dot and Colour, change the schedule and restrictions, and check account state and link counts after login and logout. Coordinate the `SettingsTrackingScreen.kt` overlap with R4.

**Rework result (Codex, 2026-11-07):** `4b41d9712` on top of `7154179d3` (`.worktrees/r10`).
- Upstream `SettingsLibraryScreen.kt` and `SettingsTrackingScreen.kt` dispatch the themed groups at their entry points. Both stock preference bodies match the pre-R10 version byte for byte. The earlier empty, masked novel token entry stays in place.
- `TsuzukiUpdateSchedule.kt` reuses the interval and category dialog and exposes all four existing smart-update restrictions: skip unread, unstarted, completed and outside the release period. Download new chapters is a separate switch.
- `TsuzukiTrackingPreferences.kt` moves novel-only accounts to the top using tracker IDs and attaches local linked counts. `docs/design/README.md` records both groups.
- Per-file Spotless passed twice. `assembleDebug` and `testDebugUnitTest` passed with 921 tests and zero failures. Check all four smart-update flags independently of the download switch during the device pass above.

#### R11 Reading estimates (T3) — `done` (merged via `review/post-v01`, 2026-11-08)
Phase 5 "reading estimates (words, speed, time left)". Details Resume tile / reader chrome show **time left in
chapter** and **in book** only when backed by data: words from downloaded/cached chapter text (count once, cache in
a Tsuzuki table-free store e.g. prefs/JSON keyed by chapter id — no schema change), reading speed from
`ReadingDays` minutes ÷ words read (rolling median, needs ≥ 3 sessions). Hide when unknown. Pure helpers + tests.
Board: NovelDetails / TabletNovel estimate rows (status table says "estimates left out").
**Result:** `tsuzuki/r11` at `d6c8f5fae`, fast-forwarded to current main before applying the saved work. The Resume tile
and reader show chapter/book time left when local word counts and three valid reading samples are available.

- `NovelReadingEstimates.kt` caches counts and takes the median of up to nine usable samples.
  `TsuzukiReadingEstimates.kt` refreshes from real chapter, download and cache changes, subtracts each chapter's
  saved progress, and suppresses estimates in incognito mode.
- Upstream `ReaderActivity.kt`, `ReaderViewModel.kt`, `NovelViewer.kt`, `NovelWebViewViewer.kt`, and
  `TtsController.kt` supply reading windows, word counts and TTS/seek boundaries. The timer can start before
  initial novel loading finishes. Playback invalidates mixed sessions.
- Upstream `NovelReaderAppBars.kt` and `presentation/manga/MangaScreen.kt` use the estimate slots in
  `TsuzukiReaderTitle.kt` and `TsuzukiResumeTile.kt`. `PreferenceModule.kt` registers the local store.
- Eight locales preserve their placeholders. Per-file Spotless and the debug build passed. All 959 unit tests
  passed without failures or skips, including five estimate tests. The design status is updated. Pairwise
  merge checks with V7.3, V7.4 and V7.5 were clean.
- Claude: use a locally available novel on S22 and tablet in Dot and Colour. Check both renderers, three
  qualifying visual sessions including a cold entry, a mix of visual and TTS, a seek, incognito, and a
  missing chapter becoming available after download. Verify the Resume tile and reader use real progress.

#### R12 Source health (T3) — `blocked` (note 2026-11-19: this card's branch commits are already in `main`; check whether the block still applies) (2026-11-08, `.worktrees/r12`)
Board `SourceHealth`. Per installed source: last successful request time, average response time, last error
(HTTP code / Cloudflare / timeout), measured passively from real Browse/Updates traffic via an OkHttp interceptor
hook in `tsuzuki/` (no extra pings; optional manual "Check now" that does one Popular request). Store in a small
Tsuzuki store (no DB migration). Show on the Browse source rows (status dot) and a Source health screen.
**Result (Codex, 2026-11-08):** `tsuzuki/r12` at `b77043aee` (base `c0a0015a4`).

- `SourceHealthRequest.kt`, `HttpSource.kt`, `JsSource.kt`, `PluginRuntime.kt` and `JSLibraryProvider.kt` carry the source ID in an in-process request tag. The standard HTTP catalogue, details and chapter-list paths are covered. The LNReader runtime tags its plugin fetches, including reader fetches. A native extension that bypasses these request paths can remain unmeasured.
- `NetworkHelper.kt`, `CloudflareInterceptor.kt`, `AppModule.kt` and `SourceHealthInterceptor.kt` measure tagged network responses after pacing, classify failures, skip cache-only responses and ignore cancelled calls. No background probes or manual checks are scheduled.
- `SourceHealthStore.kt` persists seven daily buckets per source in app-state preferences. The latest failed observation marks a source Down, a successful-response average of at least 1.5 seconds marks it Slow, and dates without observations stay Unmeasured. Last success and last error survive the rolling history.
- `TsuzukiSourceHealthScreen.kt`, `TsuzukiSourceRow.kt`, `TsuzukiTokens.kt` and the Browse tab hooks show health dots, seven-day history and a real outage card with migration and WebView actions when available.
- The eight locale files add 16 health strings with matching format arguments. `docs/design/README.md` records the implementation status.

JDK 21, two targeted Spotless passes, `:app:assembleDebug` and `:app:testDebugUnitTest` passed
(972 passed, 0 failed). `SourceHealthStoreTest.kt` checks source isolation, persistence, recovery
and expiry, request tags, and failure classification.

Claude device review on the S22 and Tab S11 in Dot and Colour. Open a real LNReader and Keiyoushi
catalogue, then check the source row and Browse overflow screen. Verify the same source updates
from library traffic, a never-used source stays unmeasured, and recovery keeps its last error while
updating its status. Check outage actions only when a real failure is present.

**Review (Codex RV1, 2026-11-08):** Fail. Real Royal Road requests set a healthy dot, last success and latency. An Archive of Our Own request recorded HTTP 418 and showed the WebView outage action. On the S22, Asura Scans loaded both Popular and Latest but remained unmeasured. `HttpSource.kt:127` tags the legacy request builders. Modern Keiyoushi suspend methods can issue OkHttp requests directly, outside those builders. Evidence: `r12-health-observed-dot-tablet.png`, `r12-health-colour-tablet.png`, `r12-manga-latest-dot-s22.png` and `r12-health-manga-dot-s22.png`. Reverted in `1d6554c94`.

#### R12-F Measure current Keiyoushi sources (T3) — `blocked` (note 2026-11-19: this card's branch commits are already in `main`; check whether the block still applies) (RV4 deferred, 2026-11-13)

Reapply R12 by reverting `1d6554c94` on a fix branch from the reviewed main. Pass source identity through
native suspend catalogue and update paths that bypass HttpSource request builders. Keep cache-only requests,
cancellations and non-source calls out of the measurements. No probes or per-host source guesses. Verify one
real Keiyoushi source and one LNReader plugin on device, including unmeasured and observed states.

**Result (Codex, 2026-11-09):** `tsuzuki/r12-f` at `8246bccd7` (R12 restored in `57794a62f`).

- `SourceHealthRequest.kt` scopes the native source ID to the caller coroutine. `NetworkHelper.kt` attaches it to the OkHttp call at start, preserving explicit request tags and leaving HTTP headers alone. `SourceHealthInterceptor.kt` retains the same store and excludes cache-only, synthetic, cancelled and untagged calls.
- `source/tsuzuki/SourceHealth.kt` scopes native HTTP sources. `SourcePagingSource.kt`, `TsuzukiTabletBrowse.kt`, `SearchViewModel.kt`, `SmartSourceSearchEngine.kt` and `DeeplinkResolver.kt` use it for catalogue and search calls. `UpdateMangaFromRemote.kt`, `MassImport.kt` and `MassImportJob.kt` use it for native update calls. LNReader keeps its explicit request tags.
- Calls using the shared client or clients derived from it keep the tag factory. Calls made through an independent client or one that replaces the factory have no ambient measurement. No polling or source-host inference was added.
- `SourceHealthInterceptorTest.kt` covers concurrent sources on one host across dispatchers, explicit-tag precedence and cache/cancellation filtering. `docs/design/README.md` records the new coverage.

Verification: JDK 21, two scoped Spotless passes per fix file and `:app:assembleDebug :app:testDebugUnitTest` passed (984 tests, 0 failures, errors or skips).

Device review: on Tab S11 and S22, open Popular, Latest and Search for a live modern Keiyoushi source such as Asura Scans, then refresh one of its library entries. Check its observed status, latency and last success. Repeat with a live LNReader plugin in Dot and Colour, and verify an unused source remains unmeasured.

Review (Codex RV4, 2026-11-13): Deferred: the required live native Keiyoushi check cannot run until the owner explicitly approves trusting the untrusted extension. Automatic approval review rejected that trust action for arbitrary extension code/credential risk; it was not bypassed. Candidate unit tests passed, but this does not prove the live native path. Also fix `SourceHealthStore.kt` decode fallback before acceptance: malformed saved JSON is swallowed without the warning log required by the checklist. Excluded by `89e1ac9ac`; later review must revert that exclusion before testing. No code-fix card added for an authorization gate.

#### R12-WARN Saved source-health decode warning — `done` (Claude review 2026-11-14: pass; stays on `tsuzuki/r12-warn`, which is now the branch to review R12-F from)

Implements only the queued R12-F decode-warning leftover. Add a warning when saved source-health JSON cannot decode, retain the empty-record fallback and avoid putting preference contents in logs. No UI, source trust, live requests, main restoration or R12-F acceptance. Allowed code: `SourceHealthStore.kt`; run scoped formatting, Debug build and existing unit tests. Parent R12-F remains deferred for the owner-controlled live-source trust check. Review can cherry-pick this small fix onto that card before resuming its separate review.


Result (Codex, 2026-11-14; code `9cfc67ed7`): `SourceHealthStore.kt` adds a WARN diagnostic for malformed saved JSON while retaining the empty-record fallback. The parser exception/raw preference is deliberately omitted because its message can include JSON contents. Four added Kotlin lines (two imports, comment, warning), no other behavior, UI, strings or data changes. JDK 21 `:app:assembleDebug :app:testDebugUnitTest` passes: **984 tests, 0 failures/errors/skips** on the deferred feature branch; two scoped formatter passes clean, Kotlin LF and diff check pass. Logs `app/build/r12-warn/` in this worktree. No new tests for this logging-only change; existing behavior tests retained. No adb, trust action, main restoration, merge or push. Reviewer should apply this fix to R12-F's restored review branch; the parent feature's separate live-source trust/runtime gate remains deferred. This result does not accept R12-F.

Review (Claude, 2026-11-14): **pass.** `9cfc67ed7` is four lines in `SourceHealthStore.kt`: a WARN line on a decode failure, no exception text and no stored JSON in the log, fallback unchanged. Not merged into `main` because R12-F itself is still excluded there (`89e1ac9ac`); `tsuzuki/r12-warn` = R12-F + this fix, so a later R12-F review starts from this branch. R12-F still waits for the owner to approve trusting a live Keiyoushi extension on a test device.

#### R13 Dictionary lookup — offline packs (T4) — `done` (2026-11-08, `.worktrees/r13`)
Owner 2026-11-07: **offline dictionary**. Board `Dictionary`. Long-press a word in the novel reader (TextView +
WebView) → Tsuzuki sheet: headword, reading (JA), part of speech, senses, "Add to notebook" (N27 quote), and the
device's own handlers (`ACTION_PROCESS_TEXT`, web search) as a fallback row.
- **Data as optional downloadable packs, not in the APK** (same pattern as the neural-TTS plan): Settings › Reader ›
  Dictionaries lists packs with size, licence and Download/Delete; stored in app files.
- Packs: **English = Princeton WordNet 3.x** (WordNet licence, permissive, attribution) and **Japanese = JMdict**
  (EDRDG licence, CC BY-SA 4.0, attribution; data file kept separate from app code). Convert each to a compact
  SQLite (FTS/prefix index) with a script in `scripts/` (no network in tests; Codex writes the converter + a tiny
  fixture; Claude/owner run the real conversion and host the pack on our own GitHub release — Codex never
  downloads). Licence texts in `docs/licenses/` and About.
- Lookup: lemmatize English (simple suffix rules + WordNet exception lists) and Japanese (deinflection rules
  table, Yomichan-style, own implementation — no GPL code). Pure helpers + tests (edges of selection, punctuation,
  inflected forms).
- If no pack is installed, the sheet shows the system/web fallback and a "Get offline dictionary" link.
Result: `e9e315cfb`. Two per-file Spotless passes and offline JDK 21
`:app:assembleDebug :app:testDebugUnitTest` passed (971 tests, including 3 new). Two Python converter
fixture tests passed.

- `DictionaryTerms.kt` normalizes selections and generates independent English and Japanese lookup forms.
  `DictionaryTermsTest.kt` covers punctuation, width, phrases and common inflections.
- `DictionaryPacks.kt` uses indexed, read-only SQLite for lookup and WordNet exception forms. It validates size,
  SHA-256, schema and version before replacing a pack. `AppModule.kt` registers the shared pack service.
- `ReaderDictionary.kt` shows definitions, readings, parts of speech, device handlers and a web-search action.
  `NovelViewer.kt`, `NovelWebViewViewer.kt` and `ReaderActivity.kt` connect selection actions and the notebook.
- `DictionaryPacksScreen.kt` and `SettingsNovelReaderScreen.kt` manage optional downloads, updates and deletion.
  Eight locales add labels. `TsuzukiAboutScreen.kt` and the two `docs/licenses/` notices record data attribution.
- `build_dictionary_pack.py`, `jmdict-tiny.xml` and `test_build_dictionary_pack.py` implement and exercise the
  converter with synthetic data. `docs/DICTIONARY_PACKS.md` gives release filenames and publishing steps.
  The full source licence supplied to the converter is embedded in its pack and readable from Dictionaries.
- `docs/design/README.md` records the implemented board scope. The board's pronunciation, word frequency and
  flashcards are outside this card. Selection opens the sheet through the Dictionary menu action.

Claude device review: generate a fixture pack or publish owner-built packs to the `dictionaries-v1` release.
Check Download, Update, Delete, offline lookup, notebook and the fallbacks on S22 in both renderers and themes.
The real packs have not been built or published in this worktree.

**Review (Codex RV2, 2026-11-10)**

Hold for RV2-04 and RV2-05. The no-pack sheet passed in both tablet renderers. A synthetic SQLite fixture passed inspection while failing lookup.

**Review (Codex RV2, 2026-11-13):** Pass for the scope available without published packs. `bb82bd9f1` validates the actual lookup schema and English exceptions before replacement and resolves 高くて to 高い. Real SQLite tests cover missing tables, missing exception table, missing ordinal, valid schema, and a malformed update with matching checksum/size that preserves the old file and cleans staging. Tablet Dictionaries shows both packs unpublished; no pack downloaded. Earlier no-pack selection-sheet checks in both tablet renderers remain valid; no pack install/update/delete was claimed on a device. S22 deferred by owner.

#### R13-F Reject unusable dictionary packs before replacement (T4) — `done` (needs `tsuzuki/r13`; app lane)

Validate the lookup tables and required columns for each language before replacing an installed pack. Keep a working pack after a rejected update. Cover the malformed schema with a local fixture and add the missing Japanese adjective connective form. Verify the no-pack fallback in both renderers and fixture-backed lookup. Keep real packs unpublished during this check.

#### R14 Merge sources for one book (T4) — `blocked` (note 2026-11-19: this card's branch commits are already in `main`; check whether the block still applies) (RV4 deferred, 2026-11-13)
Board `MergeSources`. Start the Result with a short design note, then build it: link 2+ library entries of the same book from different sources into one "merged" view (chapter
list unified by chapter number, preferred source per chapter, read state synced across). Must not change the DB
schema in this card: store links in a Tsuzuki store; reading progress sync via existing interactors. Mihon has no
equivalent — keep it Tsuzuki-only. If it can't be done without schema changes, set `blocked` with the proposed
schema for Claude.
**Result (Codex, 2026-11-09):** `tsuzuki/r14` at `0ed0a6f18`. Design: link saved novel editions by source ID and source URL in local preferences. Project a single list from the existing chapters, using source priority for each recognised number and keeping unnumbered chapters separate.

- `MergeNovelSourcesScreen.kt` renders the Dot and Colour board, provides a saved-edition picker, drag and accessible reorder actions, removal, real coverage, and a combined list that opens each selected chapter through the normal reader.
- `MergedNovelSources.kt` validates distinct sources and rejects stale edits. It selects the first unlocked copy by priority, mirrors initial read flags on linking, and writes corresponding read/unread updates in one chapter transaction. New matching copies inherit read flags during refresh. Read progress offsets stay per edition, with explicit mark-unread resetting them on matching copies.
- `MangaScreen.kt` and `MangaToolbar.kt` add the novel-details overflow action. `DomainModule.kt`, `SetReadStatus.kt`, `SyncChaptersWithSource.kt` and `ReaderViewModel.kt` provide the data and read-state hooks.
- `MergedNovelSourcesTest.kt` covers priority, gaps, decimals, unnumbered rows, shared read state and late-arriving chapters. Seven new strings are present in all eight locales. `docs/design/README.md` records the implementation.

Links are local app state. The combined list routes to the preferred edition per row. Next/Previous inside the reader follow the open edition.

Verification: JDK 21, two clean per-file Spotless passes, `:app:assembleDebug :app:testDebugUnitTest` (981 tests, 0 failures, errors or skips), eight-locale placeholder check.

Device review: back up first. On Tab S11 and S22 in Dot and Colour, link two disposable saved editions with overlapping, missing and unnumbered chapters. Reorder and remove an edition, reopen the screen and check persistence, open a fallback row in the reader, mark a matching chapter read/unread in details, finish one in the reader and refresh for a late-arriving copy. Compare the read flags in both editions.

Review (Codex RV4, 2026-11-13): Deferred: line-by-line storage/read-flag review and four pure tests passed; tablet single-edition and empty picker states checked. A real second novel edition from a distinct trusted source was unavailable, so fallback reader opening, linked read/unread, reorder/removal and persistence are not device-proven. Excluded by `451168815`; revert that exclusion in the later review. No claim of a full data-safety pass.

#### R15 Backup: scheduled export + restore polish (T3) — `done` (2026-11-08, `.worktrees/r15`)
Board `Backup`. Data and storage › Backup: last backup time + size, scheduled frequency (existing pref), keep-N
rotation, restore preview (what's inside before restoring: counts of books/shelves/history from the file header),
clear errors. No cloud sync in this card (owner decision needed for Drive/WebDAV).
Result: `5e18bf6b4`. Two per-file Spotless passes and `:app:assembleDebug :app:testDebugUnitTest` passed
with JDK 21 offline (977 tests, including 9 new). Dot and Colour Backup boards checked offline.

- `SettingsDataScreen.kt` keeps the existing frequency preference and adds retention choices.
- `TsuzukiBackupTiles.kt` shows the last successful export time and compressed size. Older records show the
  saved automatic backup timestamp when it exists.
- `RestoreBackupScreen.kt` shows validation progress, readable errors and the contents tile before confirmation.
- `TsuzukiRestorePreview.kt` displays file-wide book, shelf and history counts independently of restore options.
- `BackupFileValidator.kt` computes those counts during the existing streaming decode.
- `BackupProtoReader.kt` reports an unavailable stream and rejects invalid tags or lengths.
- `BackupCreateJob.kt` surfaces export errors, including scheduled write failures.
- `BackupCreator.kt` measures the compressed export and validates it before publication and rotation.
- `AutomaticBackupFiles.kt` stages scheduled exports, supports providers without rename, and prunes after success.
- `TsuzukiBackupName.kt` sorts both current and legacy automatic names chronologically.
- `TsuzukiBackupPreferences.kt` stores export metadata and the retention count (2, 4, 8 or 12, default 4).
- `BackupRestorePreviewTest.kt` covers real fixture counts, truncation and an unavailable provider stream.
- `BackupRotationTest.kt` covers retention, naming, size, and successful or failed publication without rename.
- Eight shared locale files add labels and errors. `docs/design/README.md` records the implemented board scope.

Claude device review: on the tablet and S22, use a disposable backup directory to verify schedule and retention,
an older and a new file, valid and corrupt restore previews, and the last backup tile in Dot and Colour. Check that
a failed scheduled write keeps existing copies. Drive and WebDAV are outside this card.

**Review (Codex RV2, 2026-11-10)**

Hold for RV2-02, RV2-06 and RV2-09. The wrapped labels passed on S22 in both themes. The status card still overlaps after a successful export, and the unrelated gzip fixture reached an enabled Restore action.

**Review (Codex RV2, 2026-11-13):** Pass on the tablet. `005713576`: status and actions have one root, full Japanese labels wrap in both themes, unknown-only gzip is rejected with Restore disabled, and settings-only backup remains valid. Valid fixture counts and restore passed; actual manual export showed time and 784-byte size above the actions in Dot and Colour. Rotation tests preserve short manual/ambiguous legacy names and cover failed writes. Published backup survives status/rotation failure or cancellation. Evidence: `backup-after-export-{dot,colour}-20261113`, `unrelated-parse-rejected-20261113`, `settings-only-restore-preview-20261113`. S22 deferred by owner.

#### R15-F Separate backup actions, validate restores and protect manual exports (T3) — `done` (needs `tsuzuki/r15`; app lane)

Use the Backup board and the RV2 screenshots. Keep the wrapped labels and place the Last backup card above the action row. Reject the unrelated gzip fixture while accepting valid exports, including settings-only backups. Rotate files known to be scheduler-owned and preserve manual exports with indistinguishable legacy names. Verify both themes on the S22 and tablet, and exercise retention and failed writes with synthetic files in a disposable directory.

#### R16 Upcoming: notify on expected release (T2) — `done` (2026-11-08, `.worktrees/r16`)
Optional per-book toggle on Upcoming: a notification on the expected date from the existing interval estimate;
off by default; uses the existing notification channel; no new background polling.
**Result (Codex, 2026-11-08):** `tsuzuki/r16` at `6954716eb` (base `c0a0015a4`).

- `TsuzukiUpcomingScreen.kt` adds a trailing per-book bell with an accessible checked state, separate from the book navigation target. It requests notification permission only on opt-in and explains how to enable chapter notifications when Android blocks them.
- `UpcomingReleaseReminders.kt` stores opt-ins locally and follows changes from the existing Upcoming query. It enqueues one-time work per forecast and cancels superseded work it is tracking. Each worker rechecks the opt-in, eligible book, current estimate, local date and delivery marker before using the existing New chapters channel. Tapping the notification opens the book.
- `App.kt` starts the observer in the main process, and `GetUpcomingManga.kt` shares its eligible-status rule with the worker’s single-book lookup.
- Three resource keys ship in all eight locales. `docs/design/README.md` records the implementation status.

JDK 21, two per-file Spotless passes, `:app:assembleDebug` and `:app:testDebugUnitTest` passed
(971 passed, 0 failed). `UpcomingReleaseRemindersTest.kt` covers default-off persistence, changed
and stale estimates, Tokyo date boundaries and same-day deduplication.

Claude device review on the S22 and Tab S11 in Dot and Colour. Verify opt-in, opt-out, permission
denial and tapping through to the book with a real scheduled estimate. Test an estimate change
while the app process is running, then verify a pending reminder survives process exit. WorkManager
delivery is inexact. A reminder that runs after the expected date expires.

**Review (Codex RV1, 2026-11-08):** Pass. A real 11 November forecast on the S22 showed the bell off by default. Opt-in added a one-time JobScheduler entry approximately 2 days and 1 hour out. Opt-out removed it and restored the bell. The same forecast row rendered in Colour. The fresh tablet library showed the empty calendar state. Actual delivery, notification deep link and permission denial were not exercised. Evidence: `r16-*` in the RV1 evidence folder.

#### R17 Tablet reading line length (T3) — `done` (2026-11-08, `.worktrees/r17`)
Owner 2026-11-07: **cap the novel text column at ~600 dp, centred**, on wide windows (board `11-tablet/TabletReader`:
~512 dp column with ~64 dp margins). Phones unchanged; the user's own side margins remain the minimum. TextView:
measure the real content width (not `screenWidthDp` — the N25 side panel can be docked) and centre the chunk views
(e.g. `maxWidth` + centred gravity in the chapter block container) without changing Tsundoku's padding logic.
WebView: CSS `max-width` on the content wrapper with auto side margins, keeping the user margins as padding; check
paged mode (`paged-reader.js` column math) and the chapter-end card. Hook through `tsuzuki/reader/`; tests for the
width math. Claude checks tablet portrait/landscape, docked panel, both renderers, paged + scroll.
Result: `4642ed752` on `tsuzuki/r17`.

- `tsuzuki/reader/NovelReadingColumn.kt` and `NovelViewer.kt` measure the actual pane and split the excess width
  around the chapter blocks. Chunks retain their existing padding, including asymmetric margins. The cap applies
  in the 600 dp smallest-width class and above, which keeps landscape phones on their existing layout.
- `NovelIllustrations.kt`, `NovelImageGetter.kt` and `NovelTextRenderer.kt` size embedded images to the measured
  column and refit loaded spans when it resizes. Image-bearing chunks remeasure their span metrics during reflow.
- `NovelWebViewStyler.kt` and `NovelWebViewViewer.kt` apply the same cap using the WebView's viewport width.
  Scrolling uses auto margins and user padding. Paged gutters sit outside the clipped body, so the existing
  pager derives the column width and page stride from `body.clientWidth`.
- Scoped Spotless and `:app:assembleDebug :app:testDebugUnitTest` passed with 972 tests, including four new
  width tests. Headless Chromium checked phone portrait and landscape, tablet portrait and landscape, the
  exact threshold, asymmetric margins, dock resize, page clipping and stride, and the scrolling chapter-end
  card. The 550 px docked pane yielded 518 px of text with the default margins.
- Claude device review remains for Dot and Colour on the tablet in portrait and landscape, with the panel
  docked and undocked. Check TextView reflow on a chapter with an illustration, WebView paging and scrolling,
  the chapter-end card in scrolling mode, and a phone comparison.

**Review (Codex RV1, 2026-11-08):** Pass. Native tablet text spans measured 1343 px including two 16 dp margins at 340 dpi, in portrait and landscape. The dock re-centred the text inside the remaining pane. Colour WebView scrolling and paged layouts showed the same centred column, including a turn to page 2 of 16. The S22 native reader continued to occupy the full phone-width view. An illustration reflow fixture was unavailable. Evidence: `r17-*` and `v76-listen-dot-tablet.png` in the RV1 evidence folder.

#### R-A1 Anime backups (T4) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (`tsuzuki/r-a1`, `81bb813bd`)
ANIME_SCHEMA §4 + §9 Q2 (import only): read Aniyomi backups (fields 500–503) into the anime tables; write anime
into our own backups in the 8000 range; restore round-trip test with a synthetic file; never change existing
manga/novel backup fields. Claude: scratch-install restore test.
Result (Codex 2026-11-09): `tsuzuki/r-a1` at `81bb813bd`, based on A3.6.

- `AnimeBackupWire.kt` reads modern and legacy Aniyomi layouts and writes Tsuzuki anime fields 8000–8002. It
  converts Aniyomi's millisecond episode position and duration to seconds. The existing manga and novel field
  numbers are retained. The SQLDelight change adds a paged query.
- `AnimeBackupStore.kt` maps titles by source and URL, episodes within each title, shelves by name, and season
  parents when both entries are in the file. Restore keeps local shelf memberships, the furthest episode position,
  and the greater watch time. Repeating a restore does not double-count history.
- The debug create and restore dialogs have an Anime option, enabled by default. Validation checks source and
  tracker availability. Backups carry source names and tracker IDs. Install the relevant extensions and sign in
  separately to play or sync.
- JDK 21, scoped Spotless clean on pass 2, `:app:assembleDebug :app:testDebugUnitTest` passed all 1,022 tests.
  The synthetic gzip fixtures cover both Aniyomi layouts, the old reader's manga/novel fields, season remapping,
  and restore into existing progress. The Anime option reuses the label in all eight required locales.
- Scratch-install review on S22 and Tab S11 in Dot and Colour: create and restore a mixed Tsuzuki backup, import
  a sample Aniyomi file, and check Anime off, shelf placement, episode resume and missing-source messaging.

#### R-A2 Player settings + sheets (T3) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was )
Boards `10-anime/PlayerSettings`, `PlayerSheets`. Subtitles (track pick, size), audio track, speed, decoder
(hw/sw), gestures (seek/brightness/volume), skip ±85 s; persist per user; Dot + Colour.

Result (Codex 2026-11-08): `tsuzuki/r-a2` at `11ba0aba4`, includes A3.2.

- `AnimePlayerSettingsScreen.kt` adds a gated Player page for autoplay, speed, subtitle size, decoder and
  gesture toggles. `AnimePlayerSheets.kt` adds the landscape options panel for subtitles, audio, quality,
  speed, episodes and video. It shows tracks reported by mpv, including HTTP(S) tracks supplied by the source.
- `AnimePlayerSettings.kt` stores preferences and matches track metadata across files with different native
  IDs. The player applies subtitle size/delay, speed and decoder immediately. At non-default speeds,
  the progress tracker stores elapsed watch seconds separately from position in the video.
- Player controls include ±85-second seeks. Double-tap seeks and vertical brightness/media-volume gestures
  respect individual toggles and the controls lock. Upstream hook: `SettingsMainScreen.kt` adds the gated
  Player entry. The volume permission is declared in the debug manifest.
- JDK 21 per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest` green, 972 tests, zero failures.
  Verified placeholders in all eight required locales and the debug-only permission in the merged manifests.
- Claude device check on S22 and Tab S11 in Dot and Colour: use a show with multiple tracks, switch audio
  and subtitles, adjust subtitle size/delay and speed, switch decoder, and verify preferences after reopening
  or changing episodes. Test ±85 seconds and each swipe/tap toggle, including while locked.

#### R-A3 Anime in Updates + "Airing today" (T3) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (tsuzuki/r-a3, 029238bc8)
New episodes from library anime appear in Updates under the media pills (ANIME pill only when ≥ 1 anime in
library); "Airing today" strip from source upload dates (real data only). Anime library update job (separate
worker, respects existing update restrictions).
Result (Codex 2026-11-08): `tsuzuki/r-a3` at `029238bc8`, includes A3.4.

- `TsuzukiAnimeUpdates.kt` and `TsuzukiAnimeLibrary.kt` show Airing today only for a saved title with a
  source upload timestamp in the local day. The Anime media filter lists newly discovered episodes and
  opens the selected episode. The book filters retain their chapter feeds. Added strings in all eight locales.
- `AnimeLibraryStore.kt` shares episode refresh with `AnimeDetailsScreenModel.kt` and excludes baseline
  imports from Updates. Existing episode data retains progress and its new-episode marker across refreshes.
  `animes.sq` and `episodes.sq` use existing columns and add queries only.
- `AnimeLibraryUpdateJob.kt` is a separate WorkManager job. It shares the library schedule, device and
  title restrictions, metadata preference, and throttle. The Anime filter refresh action enqueues it manually.
- Upstream hooks: `App.kt`, `LibraryUpdateJob.kt`, `UpdatesTab.kt`, `UpdatesScreen.kt` and
  `TsuzukiUpdatesTwoPane.kt`. JDK 21 per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest`
  green. 985 tests, zero failures, errors or skips. No schema migration.
- Claude device check on S22 and Tab S11 in Dot and Colour: favorite one show, check the Anime pill and
  baseline exclusion, introduce a later episode through an approved extension, refresh and play the row.
  Verify Airing today appears only for a real upload date, and Wi-Fi and charging restrictions defer the job.

#### R-A4 Anime episode downloads (T4) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (.worktrees/r-a4, 2026-11-09)
WORKPLAN 6.3 third engine: queue in its own store, files under `<downloads>/<source>/<anime>/<episode>/`,
HLS/segments, pause/resume, Wi-Fi only pref, shown in the Downloads screen as an Anime group. Design note in
Result first; Claude reviews before merge (T4).

**Result (Codex, 2026-11-09):** `bc88b73e6` on `tsuzuki/r-a4`. Design: one atomically persisted
anime-only queue and a serial foreground WorkManager job. Video files use app-scoped Downloads
under `anime/<sourceId>/<animeId>/<episodeId>/` so the existing native player can open local HLS
segments by relative path. Manga and novel downloads keep their existing storage and scheduler.

- `AnimeDownloads.kt` and `AnimeDownloadJob.kt` persist queue order, pause and Wi-Fi policy. An
  episode can be retried or removed, and the worker can reopen the queue after process death.
- `AnimeVideoTransfer.kt` and `OfflineHls.kt` stage HTTP video and finite HLS. Direct files resume
  with a matching strong ETag and byte range. HLS reuses completed assets by URL hash and publishes
  a local playlist last. Supported fixtures cover MPEG-TS, fragmented MP4, byte ranges, and AES-128.
- `TsuzukiAnimeDownloadQueue.kt` and `DownloadQueueScreen.kt` add a gated Anime tab with grouped
  progress, downloaded bytes, pause/resume and Wi-Fi-only. `AnimeDetailsScreen.kt` and
  `AnimeEpisodeTiles.kt` queue one or several episodes and show their status. `AnimePlayerModel.kt`
  opens saved files through the existing episode playback and progress path.
- Live streams, DRM, separate HLS audio renditions and extension-provided external tracks are
  outside this implementation. App-scoped video storage is separate from the configurable
  manga/novel folder and is removed on uninstall.

Verification: JDK 21, two clean scoped Spotless passes, `:app:assembleDebug :app:testDebugUnitTest`
(1,030 tests, zero failures, errors or skips), and matching keys in all eight locales. Desktop
FFmpeg decoded synthetic MPEG-TS, fragmented MP4 and AES-128 HLS using only file/crypto protocols.

Device review: back up the S22 Debug install first. In Dot and Colour, queue a direct file and an
HLS episode. Pause and resume each, toggle Wi-Fi-only, interrupt and reopen the app, retry or cancel
a failed item, then disable networking and play the saved episodes. Confirm the existing resume
position survives, remove a saved episode, and check that the Anime tab is absent in Preview.

**Claude list (lane 3):** C1 review + device-check every card above as it lands · C2 A1.5 device test (needs a
non-owner debug install: build debug with a temporary `.scratch` suffix, install one yuzono extension, play an
episode, uninstall) · C3 Q1 frame timing + recording on the owner's phone when it is connected · C4 restore R1's
500-book backup into a scratch install and record jank · C5 Tsundoku + Mihon upstream merge (`scripts/upstream-
report.sh`) · C6 N28/R9 widgets placed and checked · C8 release v0.1
prep when the owner says go (signing, telemetry removal, changelog).

**Prompts for the owner:**
- Thread 1 (anime): `Read AGENTS.md and docs/HANDOFF.md, then work through "Batch R" lane 1 in order (A2.3, A2.4, Y2 rework, Y6 rework, R-A1…R-A4) under the batch Q run rules.`
- Thread 2 (app): `Read AGENTS.md and docs/HANDOFF.md, then work through "Batch R" lane 2 in order (R1…R16) under the batch Q run rules. Never touch anime files.`

### Batch Q — polish, layout overflow, smooth Dot motion, Mihon-style QoL (written 2026-11-06 by Claude)

**Why:** device pass on the owner's phone (Galaxy `R5GL23ZYE1V`, SM-S948Q, 1080×2340 @ 450 dpi ≈ **384×832 dp**,
120 Hz, release build `io.github.manojx2005.tsuzuki`, Dot theme). Evidence screenshots:
`docs/design/boards/fixes-2026-11/evidence/`. Boards for this batch: **`docs/design/boards/fixes-2026-11/README.md`**
(maps board → card). Look at `png/<Board>.png` (Dot) and `png/Colour<Board>.png` first; `<Board>.dc.html` is the
source with exact sizes. Same boards on the owner's canvas https://claude.ai/artifact/Qs9pMeZhphy77fbPiE9cTH (pages
**Dot** and **Colour**). In the app Colour labels are sentence case, not the uppercase drawn on most Colour boards.
Build Dot first, then match Colour. Stock themes go away in Q12; until then leave them unchanged.

**Prompt the owner pastes into Codex (one thread per card; change the card id):**

```text
You are working on Tsuzuki (Android, Kotlin, Jetpack Compose). Read AGENTS.md, then docs/HANDOFF.md, then do
card Q1 only (batch Q). Claim it first: set its status to `codex-working` with your worktree path and today's date.
Before changing UI, open the card's boards in docs/design/boards/fixes-2026-11/ (README.md says which; look at
png/<Board>.png and png/Colour<Board>.png, measurements in <Board>.dc.html) and the bug screenshots in evidence/.
Work only in your worktree (.worktrees/q1, branch tsuzuki/q1 from main). Build with
JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot". Follow AGENTS.md Rules: tokens and
MaterialTheme.typography only, Dot uppercase + red, real data only, strings in all 8 locales, small dispatch
hooks in upstream files, new code in tsuzuki packages. Write tests for the logic you add.
Done means: per-file spotlessApply twice on the files you touched,
./gradlew :app:assembleDebug :app:testDebugUnitTest green, then fill the card's Result (files + one line each,
what you left out and why, test counts, exactly what Claude must check on the phone) and set it to `review`.
Don't commit, push or use adb. If you are stuck for more than 45 minutes, set `blocked` with the question and stop.
```

**How to run:** one card per Codex thread, in order Q1 → Q12 unless a card says `parallel-ok`. Q5 builds on
Q1 (sliding nav indicator): start it after Q1 is merged. Worktree per card:
`git -c core.longpaths=true worktree add -b tsuzuki/<id> .worktrees/<id> main`, copy `local.properties`.
Follow `AGENTS.md` Rules and WORKPLAN "Appendix — standard rules for restyle cards" (early-return dispatch in upstream
files, tokens/typography only, Dot uppercase + red, real data only, strings in all 8 locales, per-file Spotless twice,
`./gradlew :app:assembleDebug :app:testDebugUnitTest` green). **Build JDK:** Android Studio now bundles JDK 25 — use
`JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot"`. Don't commit, push or use adb; fill
**Result** (files + one line each, what you left out and why, test counts, what Claude must check on the phone),
set `review`, stop. Stuck > 45 min → `blocked` with the question.

**Mihon rule of thumb for every card (owner: "don't overwhelm users"):** show the 3–5 actions people use every
time; put the rest in an overflow menu or a "More" sheet; never let a row scroll sideways to hide an action; hide
tiles/labels that have no data (no `N/A`); one primary action per screen; long text uses the body face in sentence
case, Doto only for short headings and numbers.

#### Q1 Smooth motion in Dot (T3) — `done` (Claude merged `tsuzuki/q1` @ 1d8fe9074 into main 2026-11-06; 862 tests green; tablet Dot rail + S22 Colour pill indicator slide checked, crash buffers empty; owner-phone gfxinfo/recording still to do on the laptop. Codex's full Result: `git show 1d8fe9074:docs/HANDOFF.md`)
Owner: "a lot of animations are laggy and not smooth on Dot." Measured: frames are fine (gfxinfo 1.2 % janky,
p50 5 ms at 120 Hz) — the lag is **designed in**:
1. `theme/tsuzuki/TsuzukiMotion.kt` `stepped()` quantises Dot motion into `DOT_STEPS = 8` jumps (≈ 13–27 visible
   fps). Used by `tsuzukiTween()` (all Dot callers), `TsuzukiModalBottomSheet.kt:41,47` and
   `TsuzukiOnboardingScreen.kt:107`. The owner already rejected this for the reader transition on 2026-11-06
   (`TsuzukiReaderTransition.kt:18`). **Use the smooth curve in Dot everywhere**; keep `stepped()` only for
   genuinely discrete fills (progress dots lighting one by one), and make those step on the dot count, not 8.
   Delete the now-unused `res/anim/tsuzuki_reader_*_dot.xml` if nothing references them.
2. Nav pill (`TsuzukiNav.kt` `NavBarItem`): `animateContentSize()` on every item + background toggled instantly →
   the white pill pops while the whole row re-lays out. Replace with ONE indicator behind fixed-width items that
   slides with `graphicsLayer { translationX }` (spring, no layout per frame). Same for the tablet rail.
3. Dot grid (`TsuzukiComponents.kt` `tsuzukiBackground`): ~2,000 `drawCircle` calls per draw, 22 call sites.
   Draw one 12 dp tile into an `ImageBitmap` once (`drawWithCache`) and fill with an `ImageShader`
   (`TileMode.Repeated`).
4. Audit remaining Dot-only animations (`TsuzukiStartup.kt`, `TsuzukiSharedCover.kt`, `TsuzukiCoverStack.kt`,
   `TsuzukiAdaptiveSheet.kt`, `TsuzukiSegments.kt`, `TsuzukiReaderContents.kt`): animate only
   transform/alpha in `graphicsLayer`, read animated values in draw/layout lambdas, not composition.
Keep Reduce motion behaviour. Tests: `stepped` / motion-spec selection unit tests updated.
Device check (Claude): record `dumpsys gfxinfo framestats` + screen recording for sheet open/close, tab switch,
home stack swipe, book open, before/after.

#### Q2 Novel reader: next chapter always reachable (T3) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night1` → `52ac6a536`)
Owner: "the continue button on the novel reader doesn't go to the next chapter." Found on the phone
(`evidence/reader-menu-overflow.png`): the bottom bar (`presentation/reader/appbars/NovelReaderAppBars.kt` ~l.504)
is a `horizontalScroll` row with `SpaceEvenly`; with 9–10 enabled items it is wider than 384 dp, **Settings is
clipped and Next chapter is off-screen** (uiautomator: Settings ends at x=1057/1080, no Next node). Board
`fixes-2026-11/Main.dc.html`:
- Row 1: fixed **‹ Previous** (48 dp, tile) | progress dots + `19%` / page | fixed **› Next** (48 dp, accent). Never
  scroll. Disabled state when there is no prev/next chapter.
- Row 2: the first **4** enabled actions from the existing configurable bottom-bar list (`BottomBarItem` order) with
  short labels + **MORE** (a Tsuzuki menu/sheet holding the rest: auto-scroll, rotation, scroll to top, edit, TTS
  paragraph controls). PREV/NEXT_CHAPTER items are taken out of row 2 because row 1 owns them.
- Top bar: title and chapter both `maxLines = 1` + ellipsis (now "SWITC" is hard-cut).
- End of chapter (infinite scroll off): replace the hardcoded light-blue `android.widget.Button` in
  `NovelViewer.addNextChapterButton()` (`#ADD8E6`, black stroke) with a themed end card — board
  `ChapterEnd.dc.html` (next chapter number + title from source, downloaded state, NEXT CHAPTER 56 dp, Mark read,
  Contents). The card sits on the reading page, so it takes the **page colours** (ink text, ink-filled button with
  page-coloured label, tile = ink at ~8 % over the page) in Colour, for every page colour incl. Night/Black — never
  the app tile/accent (white/gold on cream is unreadable; see `ColourChapterEnd.dc.html`, its `pageColour` switch).
  Do it through a hook/factory in `presentation/tsuzuki/reader/` so `NovelViewer` keeps a one-line call.
  WebView renderer: same card via its existing JS bridge / `onChapterNavigate` path.
- **Verify the action itself:** write tests around `ReaderActivity.loadNextChapterInternal` /
  `ReaderViewModel.loadNextChapter` for (a) infinite scroll on — `viewerChapters.nextChapter` must follow the
  *visible* chapter (`novelVisibleChapter`), not the first-loaded one; (b) a tap while `isLoadingAdjacentChapter`
  (pending direction replay); (c) WebView paged. If (a) is wrong, fix it.
Stock themes: keep the stock row but add the same fixed prev/next (it's a bug there too) only if the diff stays a
small slot; otherwise Tsuzuki-only and say so.

**Result (Codex, 2026-11-07):** Branch `tsuzuki/q2` at `50bfacf8e`.

- `NovelReaderAppBars.kt` dispatches to fixed chapter buttons, progress and the first four configured actions with an overflow for the rest. `TsuzukiNovelChrome.kt` owns those controls. `TsuzukiReaderTitle.kt` ellipsizes the novel subtitle.
- `TsuzukiReaderSlider.kt` and `TsuzukiComponents.kt` reuse the seek track and enable dots in both novel chrome themes.
- `ReaderActivity.kt` connects chapter buttons and the chapter-end Contents action. `ReaderViewModel.kt` resolves adjacent chapters from the visible chapter in infinite scroll, supports the existing read-progress path and checks download status on IO. `NovelChapterNavigation.kt` owns destination selection and queued taps.
- `NovelViewer.kt` mounts the Compose chapter-end card and scrolls to cached chapters on explicit navigation. `TsuzukiChapterEnd.kt` builds the native card and WebView payload with real next-chapter metadata and page colours.
- `NovelWebViewViewer.kt` connects the card to existing next navigation, handles the read and Contents actions, and scrolls to cached chapters. `NovelWebViewStyler.kt` injects `chapter-end-card.js` for continuous non-infinite reading. The JS uses page ink and background in Colour and the shared Dot palette in Dot.
- Eight `i18n` `strings.xml` files add localized reader labels. `NovelChapterNavigationTest.kt` covers visible-chapter routing, the paged path and one-time replay of the last queued direction.

Upstream files touched: `NovelReaderAppBars.kt`, `ReaderActivity.kt`, `ReaderViewModel.kt`, `NovelViewer.kt`, `NovelWebViewViewer.kt`, `NovelWebViewStyler.kt`, and the eight `i18n` resources.

Decisions and gaps: the fixed chrome is for Tsuzuki themes. Legacy toolbar configuration and its paged WebView page-turn arrows retain their existing behavior. Paged WebView uses the fixed chapter buttons and its existing edge gesture, with the in-page card in continuous mode only. The WebView card uses system font families, while the Compose card uses the Tsuzuki type ramp.

Verification: JDK 21 per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest` green (865 tests, 0 failures), JavaScript syntax and DOM/bridge smoke test green.

Claude device check: S22 and Tab S11, Dot then Colour. Open a novel with more than four enabled actions. Check the ellipses, both fixed arrows, overflow, and Settings at large font sizes. In TextView and WebView, scroll across appended chapters and then navigate in both directions, including back to a cached chapter and a second tap while loading. In paged WebView, verify that the fixed buttons cross chapters and tap/swipe still turns pages. With infinite scroll off, compare the card in Sepia, Night and Black, check the real downloaded marker and exercise Next chapter, Mark read and Contents. Check first and final chapter boundary states.

#### Q3 Save chapters: choose a range by chapter number (T3) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night1` → `52ac6a536`)
Owner: "I can't choose which chapter I want to download properly with entering numbers." Found
(`evidence/save-chapters-slider.png`, `presentation/tsuzuki/details/TsuzukiDownloadSheet.kt`): CUSTOM is a
`RangeSlider` over **list positions** (`rangeStart/rangeEnd` are indices); on 1,229 chapters a 25-chapter range puts
both thumbs on top of each other; no text entry. Board `SaveChapters.dc.html`:
- CUSTOM shows **From chapter / To chapter** numeric fields (`KeyboardType.Number`, IME Done), −/+ steppers (long-press
  repeats), shortcuts **FROM WHERE I AM** (first unread after last read) and **TO LAST**. Values are **chapter
  numbers** (`chapterNumber`, fallback to list order for chapters without a number); decimals (12.5) allowed.
- Inline validation (from > to, out of range, nothing to download) in the accent colour; the CTA reads
  `SAVE CH. 33–57` and is disabled when invalid. Show `N CHAPTERS · M ALREADY SAVED · SKIPPED`.
- Keep the slider out (remove it) — Mihon uses a number dialog for custom amounts; the slider is unusable on long books.
- Move the two app-wide switches (Only on Wi-Fi, Delete after reading) out of the per-book sheet into a
  `DOWNLOAD SETTINGS ›` row that opens the download settings screen.
- Explain/fix the count mismatch: header shows `1432 chapters`, sheet shows `ALL 1229`, `UNREAD 1198`. Label what
  ALL means (not yet saved?) or make it match; same numbers on details and sheet.
- Pure helper `chaptersInNumberRange(chapters, from, to)` + tests (decimals, missing numbers, reversed input,
  duplicates across scanlators).
Also confirm Mihon-style range selection still works in the chapter list (long-press one chapter, long-press
another → selects everything between → download from the action bar); if not wired in the Tsuzuki list, wire it.

**Result (Codex, 2026-11-07):** Branch `tsuzuki/q3` at `800f92ce4`.

- `TsuzukiDownloadSheet.kt` replaces the slider and global switches with preset pills, decimal From/To fields, repeating steppers, shortcuts, inline validation, a range CTA, and a link to Download settings. The sheet shows full-book totals alongside the actual eligible and already-saved counts.
- `TsuzukiDownloadSelection.kt` resolves inclusive source numbers, falls back to 1-based reading order for unnumbered chapters, and separates the selected range from eligible downloads. It also supplies the visible long-press range helper.
- `MangaViewModel.kt` applies the visible range by chapter ID against the full chapter list, so selection in a filtered list preserves hidden chapters and full-book counts.
- `TsuzukiDownloadSelectionTest.kt` covers decimal boundaries, duplicate scanlator numbers, unnumbered chapters, invalid input, and visible selection boundaries. Eight locale `strings.xml` files supply matching labels and format placeholders.

Upstream files touched: `MangaViewModel.kt` and the eight `i18n` resources. The input uses a decimal IME with Done so fractional numbers are available. Next 10 and Bookmarked stay in the sheet’s overflow.

Verification: per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` green (865 tests, 0 failures). Q3 locale keys and placeholders match in all eight locales.

Claude device check: Dot then Colour on S22 and Tab S11. Open Save chapters on a long book. Enter a decimal range, hold both steppers, exercise both shortcuts and check reversed, out-of-range and all-saved input. Compare totals with details, inspect the queued IDs and skipped count, and check the Download settings link and IME Done. Long-press two separated visible chapters with a filter active, download the selected range, clear the filter and confirm that the full list is available.

#### Q4 Two "Default" shelves in the library (T3) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night1` → `52ac6a536`)
Found (`evidence/library-two-defaults.png`): shelf pills `DEFAULT · DEFAULT · LOCAL`. The first is the system
category (id 0, manga); the second is a **real category named "Default"** holding the 4 novels, created by the
LNReader backup import (`LNReaderBackupImporter.kt` ~l.204 maps every LN category, including LNReader's own
"Default", to a new `BackupCategory`; `CategoriesRestorer` then creates it).
- Import: map LNReader's default category (its id 1 / name "Default", case-insensitive, also the localized
  `default_category` string) to the system category instead of creating one.
- One-time fix for existing installs (app-start migration in `tsuzuki/`, guarded by a preference flag): if a
  non-system category's trimmed name equals "Default" (or the localized default name) **and** it has content type
  novel or all, move its memberships to the system category and delete it. Unit/SQLite test with the in-memory fixture
  (memberships preserved, other categories untouched, runs once). Note it in the HANDOFF log; Claude will run it on
  the phone only after an in-app backup.
- Display: if two pills would still render the same label, never show duplicates (append the content type).
- Pills show their book count (N12 already has the query).

**Result (Codex, 2026-11-07):** Branch `tsuzuki/q4` at `e8f02e97b`.

- `LNReaderBackupImporter.kt` uses `LNReaderShelves.kt` to map LNReader’s Default ID and localized names to the system shelf, deduplicate Default memberships and preserve distinct category order. The importer’s existing dangling KDoc now attaches to its class.
- `DefaultShelfMigration.kt` moves matching novel and all-content memberships to category 0 in one database transaction, preserves other memberships and sets its one-time preference after success. `App.kt` calls it during background startup. A configured default pointing to a removed shelf follows the system shelf.
- `TsuzukiShelfPills.kt` disambiguates duplicate names with content type and, if needed, shelf ID. `LibraryTabs.kt` uses that implementation. `LibraryContent.kt`, `LibraryTab.kt`, `NovelsTab.kt` and `LibraryViewModel.kt` pass counts to the pills. The resident library uses its filtered group counts, while experimental pagination shows full shelf totals from the database.
- `DefaultShelfMigrationTest.kt` covers membership preservation, one-time execution, failure retry, LNReader mapping and label collisions.

Upstream files touched: `LNReaderBackupImporter.kt`, `App.kt`, `LibraryTabs.kt`, `LibraryContent.kt`, `LibraryTab.kt`, `NovelsTab.kt` and `LibraryViewModel.kt`. The migration uses existing SQLDelight queries. Shelf suffixes reuse translated strings.

Verification: per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` green (866 tests, 0 failures). An in-memory SQLite fixture using the checked-in table definitions and insert/delete statements verified category 0, existing memberships and foreign keys.

Claude device check: export an in-app backup first and use the preview test data. Start with a duplicate Default shelf containing novels, including a book also in Local. Verify a single system Default pill with the combined real count, Local and other shelves intact, and no extra memberships after a second start. Import an LNReader fixture with Default plus a custom shelf and verify the same mapping. Test a manga-only shelf sharing the Default label, then check Dot and Colour on phone and tablet.

#### Q5 Navigation: Library tab + real labels (T3) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night1` → `52ac6a536`) (Claude rebased onto main 2026-11-06: `tsuzuki/q5` @ 6612e1f1b, worktree **already exists** at `.worktrees/q5` — use it, don't create a new one. Code is nearly done; what's left: build + tests green, check the items in Codex's earlier Result (`git show 4345fc994:docs/HANDOFF.md`, card Q5), commit, fill Result) (owner approved 2026-11-06)
Mihon's five tabs are Library · Updates · History · Browse · More; in Tsuzuki the library is only reachable from
Home's "ALL BOOKS →" (no nav item) and Dot labels are cryptic (`NEW / PAST / FIND / •••`). Board `Library.dc.html` /
`Browse.dc.html`: **HOME · LIBRARY · UPDATES · BROWSE · MORE** (History moves into More + Home's recent row).
Uses Q1's sliding indicator. Tablet rail gets the same items. TalkBack labels unchanged words.
- History stays reachable: a HISTORY tile/row in More (top group) and Home's recent row opens it; deep links and
  shortcuts that targeted the History tab still land on History. Library tab opens the existing library screen
  (the one behind Home's ALL BOOKS →), keeps its state across tab switches, and Home's ALL BOOKS → selects the tab.
- Badges: Updates keeps its unread badge. Back from any tab returns to Home (as now).
- Tests: nav item list per layout, History routing. Device check (Claude): phone pill + tablet rail, Dot + Colour.

**Result (Codex, 2026-11-07):** Branch `tsuzuki/q5` at `b3165f66b`, on top of Claude’s rebased `6612e1f1b`.

- `TsuzukiNavigation.kt` supplies the five destinations and resolves hidden History, shortcuts and retained Library media. `HomeScreen.kt` wires that policy to the phone bar and tablet rail, keeps the Updates badge and preserves Back to Home.
- `TsuzukiHomeTab.kt` sends All Books through the retained Library route. The Recent row continues to open History.
- `MoreTab.kt` opens History from the tile dispatched through `MoreScreen.kt`. `TsuzukiMoreScreen.kt` places that tile below the top toggles.
- `TsuzukiSettingsMain.kt` presents the same five destinations in the tablet Settings rail. `TsuzukiNav.kt` documents the Dot label role.
- `TsuzukiNavigationTest.kt` covers shared order, History and Library routing, retained and restored media, and shortcut destinations.

Upstream files touched: `HomeScreen.kt`, `MoreTab.kt` and `MoreScreen.kt`. Labels reuse existing translations. The Library route uses existing Library tabs and saveable state.

Verification: per-file Spotless twice, JDK 21 `:app:assembleDebug :app:testDebugUnitTest` green (869 tests, 0 failures).

Claude device check: Dot then Colour on S22 and Tab S11. Verify the full Home, Library, Updates, Browse and More labels and the sliding indicator, including rapid reversals and reduce motion. In Library, change shelf and filters, open a book, switch tabs and return. In separate mode, select Manga, switch to Home and use All Books to reopen Manga. Open History from More, Home's recent row and the launcher shortcut. Verify the Updates badge, Back to Home, long localized labels at increased font scale, and leaving the tablet Settings rail for Library.

#### Q6 Book details header (T2) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night1` → `52ac6a536`)
`evidence/details-header.png`, board `Details.dc.html`:
- Toolbar: back · download · filter · overflow. Move scroll-to-top / next-unread / scroll-to-bottom into the overflow
  (Tsundoku added them; keep the actions, Mihon shows only download/filter/more).
- Hide tiles without data: the hourglass `N/A` tile; chapter rows show no `N/A` date line when the date is unknown.
- One Resume: the big Resume tile stays; the floating `Resume` FAB over the chapter list only appears after the tile
  scrolls off screen (it currently overlaps chapter 2 while the tile is visible).
- Genre chips wrap to max two lines (+N chip) instead of being clipped at the card edge ("Myster…").
- Dot case consistency: tile labels uppercase via tokens ("In library", "Tracking", "WebView" are mixed case);
  summary/author stay sentence case in the body face. Summary expand chevron must not overlap text.

**Result (Codex, 2026-11-07):** Branch `tsuzuki/q6` at `105f67354`, based on the Q3 commit `800f92ce4`.

- `MangaToolbar.kt` puts scroll-to-top, next unread and scroll-to-bottom in the Tsuzuki overflow while keeping Download and Filter in the toolbar.
- `MangaInfoHeader.kt` hides the empty update tile, uses themed action labels and supplies a summary layout with a separate chevron. The summary and author keep their source casing.
- `TsuzukiDetailsDescription.kt` measures genres for two rows, reserves a `+N` pill when needed, and keeps search and copy available for every tag. `TsuzukiGenreRowsTest.kt` covers fitting, overflow and wide chips.
- `MangaScreen.kt` omits unknown chapter dates, gates the floating Resume action on tile visibility in both layouts, and accounts for the themed header items in chapter scroll targets. `TsuzukiResumeTile.kt` uses the board’s neutral tile and accent play disc.

Upstream files touched: `MangaToolbar.kt`, `MangaInfoHeader.kt` and `MangaScreen.kt`. Existing translated labels and actual chapter and genre data are reused.

Verification: per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` green (868 tests, 0 failures).

Claude device check: Dot then Colour on S22 and Tab S11. Open a title with an unknown update date and chapter dates, verify the three primary toolbar actions and exercise all overflow jumps. Scroll the Resume tile fully out and back into view, including the tablet’s independent metadata pane. Open a book with many genres at larger font scale, use the `+N` pill and search/copy an overflowed genre. Expand the summary and check that its control clears the text.

#### Q7 Browse tabs + source rows (T2) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night1` → `52ac6a536`) `parallel-ok`
`evidence/tabs-updates-history-browse-more.png` (3rd panel), board `Browse.dc.html`: the 4 pills
(NOVEL SOURCES · MANGA SOURCES · NOVEL EXT… · …) run off screen. Use media pills (NOVELS | MANGA) + a 2-item
segmented control (SOURCES | EXTENSIONS) — the existing pager keeps its 4 pages, only the header changes. Source rows:
name gets the full width (no "LightNovelP…" at 384 dp), LATEST becomes a compact outline chip, pin stays 48 dp.

**Result (Codex, 2026-11-07):** Branch `tsuzuki/q7` at `1b7a84e78`.

- `TsuzukiBrowseHeader.kt` presents the media pills and Sources/Extensions segments over the existing pager, with the active media’s extension update count.
- `BrowseTab.kt` uses a typed page map for header, toolbar, search and shortcut routing. The live pager has four source/extension pages plus two migration pages. Each migration page stays available from its media’s overflow and from the existing pager.
- `TabbedScreen.kt` exposes a header slot and guards the current page while the tab count changes.
- `SourcesScreen.kt` dispatches themed rows to `TsuzukiSourceRow.kt`. The source name can wrap and has its own line, Latest has a compact outline, the pin target is 48 dp, and a preset remains available on its supporting row. Ordinary language sections use their existing language headings.
- `TsuzukiBrowsePagesTest.kt` covers the existing page order, hidden manga mode and the extension shortcut destination.

Upstream files touched: `BrowseTab.kt`, `TabbedScreen.kt` and `SourcesScreen.kt`. Existing localized labels are reused.

Verification: per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` green (864 tests, 0 failures).

Claude device check: Dot then Colour on S22 and Tab S11. Exercise both media pills, both segments, swipes, update counts, the extension shortcut and both migration routes. Switch to hidden manga mode and repeat the novel flow. Check long source names, Latest, preset, pin and long-press pin actions at phone width and in the tablet’s two-pane layout.

#### Q8 Overflow and large-font sweep (T3) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night2` → `6af172281`; follow-ups in Q14)
**From Claude's 2026-11-07 device pass (include these):** reader action row needs short labels (SCROLL TO TOP / START AUTO-SCROLL ellipsize now; board `Main` uses one word each); More tiles `DATA AND STOR…`, `DOWNLOADED ON…`; details `131 chapters` heading not uppercased in Dot.
Every Tsuzuki screen at **360 dp width** and **font scale 1.0 / 1.3 / 2.0**, Dot + Colour, EN + JA. Known on the
phone now: More "DATA AND STORA…", reader title hard-cut, genre chips, Browse pills, source names, Home hero title
(`LORD OF / THE / MYSTERIES` 3 lines pushes the stack), shelf pills. Add `@Preview` groups
(`widthDp = 360, fontScale = 1.3f/2f`) for each Tsuzuki screen and fix: no clipped text without ellipsis, rows that
wrap instead of scroll, min 48 dp targets, no fixed heights around text. List every fix in Result.

**Result (Codex, 2026-11-07):** `tsuzuki/q8` @ `e6a34735b`. **893 tests, 0 failures.** Per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green with JDK 21.
- `TsuzukiPill.kt` / `TsuzukiSegments.kt` — 48 dp controls, wrapping rows and labels, shelf/tab overflow with active choice retained, measured indicator height and a wrapped high-font-scale layout.
- `TsuzukiNav.kt` — taller bottom cells and two-line ellipsis for large text while retaining the sliding indicator and spoken labels.
- `TsuzukiHomeScreen.kt` — two-line ellipsized title, independent cover stack height, stacked hero and stat tiles at large font scales, a growing Resume row, and 48 dp Read/Watch controls.
- `TsuzukiMoreScreen.kt` — growing tile heights and multiline labels/subtitles. Phone tiles stack at large font scales.
- `TsuzukiBrowse.kt` / `TsuzukiSourceRow.kt` — multiline search and pinned labels, source names with ellipsis, wrapping metadata, and Latest under the title at large font scales.
- `TsuzukiNovelChrome.kt` / `NovelReaderAppBars.kt` — short labels for Top, Scroll/Stop, Rotate and paragraph actions, with full TalkBack/overflow descriptions. Fewer primary actions at larger scales, and progress labels can wrap. Q2 already ellipsizes novel titles and keeps fixed Previous and Next controls.
- `TsuzukiPageColours.kt` / `TsuzukiReaderSettingsSheet.kt` — quick swatches stay six across at the standard scale and wrap to three with large type. Font tiles grow and wrap. Full settings already has wrapping choices from Q11.
- `ChapterHeader.kt` — Dot chapter count uses uppercase, with a taller target. Existing Q6 genre rows fit two measured lines and keep extra tags in the overflow.
- `TsuzukiHistoryStats.kt` / `TsuzukiUpdatesTiles.kt` / `TsuzukiSettingsMain.kt` — growing stat tiles and wrapping units; settings titles and subtitles allow two lines.
- `TsuzukiNarrowPreviews.kt` — 360 dp English/Japanese previews at 1.0, 1.3 and 2.0 for Dot and Colour. Covers Home, Library/nav, Browse, Details, More, novel and manga reader chrome, reader settings, Settings, Updates and History. Previews use real composables with sample data. Separate previews for secondary screens were left out.
- `TsuzukiNarrowLayoutTest.kt` — checks shelf overflow and reader action capacity. Eight locale files add short reader labels. Upstream files touched: `ChapterHeader.kt` and `NovelReaderAppBars.kt`.
- Claude device checks: at 360 dp across the preview matrix, check More's Data and Storage and Downloaded only, Home long title and cover shuffle, Library selected shelf from More, Browse Latest and long source names, Details chapter count and genre overflow, reader short action labels and More, quick sheet swatches and font rows, and History/Updates stat tiles. Confirm long labels wrap or ellipsize and all actions stay reachable. Actual visual previews and device checks remain pending.

#### Q9 Accessibility + quality of life, Mihon parity (T3) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night2` → `6af172281`; follow-ups in Q14)
Boards (fixes-2026-11): `Q9Selection`, `Q9Undo`, `Q9UpdatesLoading`, `Q9SettingsAdvanced` + `Colour*` twins (static `.html` + `png/`).
- Nav and tab labels are spoken as real words (already true for content descriptions — keep it when Q5 lands).
- Contrast: check every muted/disabled colour on tile/background (e.g. `#8A8A8A` on `#0D0D0D`, dim read-chapter rows
  at `alpha .55`) ≥ 4.5:1 for text; fix tokens, not screens.
- Undo instead of confirm where Mihon does (remove from library, delete downloads, mark read): snackbar with UNDO.
- Long-press anywhere a list has selection (library, chapters, updates, history) and a bottom action bar with
  labelled actions; range select by long-press (see Q3).
- Settings: hide rarely used Tsundoku power options behind an "Advanced" group in each settings screen (no deletes).
- Updates: show a skeleton/last-update list instead of a bare spinner while 1,871 rows load (took > 2.5 s on the phone);
  profile the load and move mapping off the main thread if needed.
Each item is optional if it turns out already done — say so in Result.

**Result (Codex, 2026-11-07):** `tsuzuki/q9` @ `11efb24f4`. **892 tests, 0 failures.** Per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green with JDK 21.
- `TsuzukiSelectionBar.kt`, `TsuzukiSelectionActions.kt`, `TsuzukiSelectedCover.kt` — labeled, font-scale-aware selection actions with overflow, localized counts, and checked grid covers. Existing Q3 chapter, Library and Updates long-press ranges remain in use.
- `TsuzukiHistorySelection.kt`, `TsuzukiHistoryItem.kt` — History range selection over visible rows, accent selection, and bulk removal of the selected entries.
- `TsuzukiUndoSnackbar.kt` — six-second Undo window and progress bar, with explicit dismissal under TalkBack. Library removal updates favorites immediately and restores them on Undo. Mark read and file deletion are staged until the window closes because they also delete files or translations and may notify trackers. Existing advanced destructive Library operations remain in the explicit Delete menu.
- `TsuzukiUpdatesLoading.kt` — static first-load skeleton with a localized loading label and no invented count. `UpdatesViewModel.kt` already maps fetched rows on IO. Grouped mode now publishes the first page while it prefetches. The 100 ms board target still needs a device trace.
- `TsuzukiAdvancedPreferences.kt` — search expands and scrolls to rare options. Folded groups/options: Library behavior, update metadata and recent-update interval; Manga Reader E-Ink and actions; Novel Reader formatting, content and auto-scroll; Downloads deletion rules; Novel Downloads throttling, mass import and per-extension overrides; Browse delay, cover skipping and back confirmation; Tracking chapter thresholds; Data cleanup. Search and reset still see the original preference tree.
- `TsuzukiTokens.kt`, `TsuzukiNav.kt`, `TsuzukiHomeScreen.kt` — shared muted text ink for read labels, nav and the low-contrast Home label. Q5 already supplies spoken nav labels. `TsuzukiSelectionTest.kt` covers action capacity, History ranges, search reveal and 4.5:1 contrast on the default Dot and Colour surfaces.
- `i18n/src/commonMain/moko-resources/{base,ja,zh-rCN,ko,es,pt-rBR,fr,de}/strings.xml` — new selection, Undo, loading and settings copy.
- Upstream files: `AppBar.kt`, `LibraryToolbar.kt`, `MangaToolbar.kt` (selection count); `CommonMangaItem.kt`, `MangaBottomActionMenu.kt`, `MangaChapterListItem.kt`, `UpdatesUiItem.kt` (selection/contrast hooks); `HistoryScreen.kt`, `HistoryItem.kt`, `HistoryTab.kt` (selection wiring); `PreferenceScreen.kt`, `PreferenceScaffold.kt` (Advanced render hook); `LibraryTab.kt`, `NovelsTab.kt`, `LibraryViewModel.kt` (library undo); both `MangaScreen.kt` files and `MangaViewModel.kt` (chapter undo); `UpdatesScreen.kt`, `UpdatesTab.kt`, `UpdatesViewModel.kt` (loading, actions and undo).
- Claude device checks: on 360–384 dp phone and tablet in Dot and Colour, long-press two entries in Library, chapter list, Updates and History, then check the range, count, cover check, Back and all More actions. At 200% font scale confirm all actions remain reachable. For disposable entries, remove from Library and undo, and cancel then repeat mark-read and delete-download through expiry. Enable TalkBack and verify announcement, persistence and dismiss behavior. Open Settings Search to a folded option and verify it expands and scrolls. Capture first-frame timing on the large Updates dataset, with grouped mode and Reduce motion both on and off.

#### Q11 Reader settings redesign: quick sheet + All reader settings (T4) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night2` → `6af172281`; follow-ups in Q14)
Owner: "redesign the reader settings … the all settings panel too." Boards (Dot + Colour pages of the canvas;
offline `fixes-2026-11/ReaderQuick`, `ReaderAll`, `ReaderAllPage` and their `Colour*` twins). Found on the phone:
- Quick sheet (`presentation/tsuzuki/novel/TsuzukiReaderSettingsSheet.kt`): 6 page-colour circles overflow 384 dp
  (BLACK clipped) and have no visible names; tabs LOOK/LAYOUT/CONTROLS; size `13` and lines `1.1` without units.
- All settings (`presentation/reader/settings/NovelPage.kt`, 2,044 lines, Tsundoku's stock sheet opened from
  "ALL SETTINGS"): five **unlabelled icon tabs**, labels wrap mid-word ("Rende / ring / mode"), line height shows
  `11` here vs `1.1` in the quick sheet, four separate margin steppers, ~30 options, and **three overlapping colour
  lists** (Theme App/Light/Dark/Sepia/Black/Grey/Custom + Font colour ×9 + Background colour ×9) that fight the
  quick sheet's Book/Paper/Sepia/Sage/Night/Black.
Do:
1. Quick sheet → board `ReaderQuick`: tabs **TEXT · PAGE · CONTROLS**; page colours in one 6-column grid with names
   under each swatch (fits 360 dp); TEXT SIZE / LINE SPACING with the same unit formatting as the full screen
   (one formatter, e.g. `1.1×`); RESET (this reader's defaults) + `ALL READER SETTINGS ›`.
2. All reader settings → a **Tsuzuki full-screen page** (not a sheet) in `presentation/tsuzuki/novel/`, opened from
   the quick sheet in Tsuzuki themes; stock themes keep `NovelPage`. Labelled tabs
   **TEXT · PAGE · CONTROLS · LISTEN · MORE** mapping the existing tabs (Reading/Appearance/Controls/TTS/Advanced).
   Labels sit above controls (never beside a wrapping label); every row min 56 dp; rare options fold into a
   **FINE-TUNE** expander (paragraph indent/spacing, individual top/bottom margins, highlight style…).
   PAGE tab: one page-colour list (Q12) + CUSTOM swatch that opens the existing text/background colour pickers;
   margins as SIDE MARGINS + TOP / BOTTOM (writes left+right / top+bottom; separate values still editable in
   FINE-TUNE). Live preview strip on TEXT. Every control writes the **same `ReaderPreferences` keys** — no new prefs
   except what Q12 needs; no option is deleted, only regrouped.
3. Same treatment for the manga reader settings dialog tabs (`ReaderSettingsDialog.kt`, N4's Tsuzuki pages): labelled
   tabs, no mid-word wraps. Small.
Tests: pure mapping of tab → preference groups, unit formatter, margin linking. Strings in 8 locales.

**Result (Codex, 2026-11-07):** `tsuzuki/q11` @ `34ff66af1`. Per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green with JDK 21. **891 tests, 0 failures**.
- `TsuzukiReaderSettingsSheet.kt` / `TsuzukiPageColours.kt` — Text, Page and Controls tabs, six named swatches in the quick row, shared reader controls and reset/full-settings actions.
- `TsuzukiAllReaderSettings.kt` — full-screen dialog with five labeled tabs, live text preview, linked side/vertical margins, custom colours and Fine-tune sections for paragraph, page and highlight controls.
- `TsuzukiReaderSettingsLogic.kt` / `TsuzukiReaderSettingsLogicTest.kt` — tab-to-group mapping, locale-aware spacing, linked margin midpoint and reset to existing defaults, with three tests.
- `i18n/src/commonMain/moko-resources/{base,ja,zh-rCN,ko,es,pt-rBR,fr,de}/strings.xml` — translated new settings labels, sample text and reset prompt.
- Upstream files: `ReaderSettingsDialog.kt` dispatches the Tsuzuki page and gives the manga sheet compact labeled tabs; `NovelPage.kt` exposes the existing settings groups and highlight controls; `presentation-core/.../SettingsItems.kt` gives the Tsuzuki page stacked labels, unit displays and decimal entry while retaining the default layout for other callers.
- Reset is confirmed, novel-only and preserves authored CSS/JS snippets and replacement rules. When the two individual margins differ, both are shown and the first linked edit starts from the rounded midpoint. The preview uses sample text and the selected font, spacing and palette. Site-supplied WebView fonts and authored CSS are left to the actual chapter. Existing Tsundoku English-only advanced/TTS labels were carried forward.
- Claude device checks: at 360–384 dp in Dot and Colour, check six named swatches, both quick metric values and the full-screen open/back path. Test decimal entry in a comma locale, Text preview with custom font and Book/Night page colours, Page custom ink/background, independently set margins then relink, status bar position, native/WebView switching, highlight Fine-tune, and Reset after adding a disposable replacement rule. On tablet verify insets, landscape scrolling and all five tabs, then check the three labeled manga tabs in both themes.

#### Q12 Remove the themes we didn't make (T3) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night1` → `52ac6a536`)
Owner: "remove themes that we did not make" (2026-11-06). **This overrides the AGENTS.md rule "Legacy themes must
keep working"** — update that rule in the same card. Board `Themes.dc.html`.
- **App themes:** Settings › Appearance offers only **Colour** and **Dot** (+ light/dark/system mode, Colour accent
  swatches, Dot covers greyscale/in colour). Hide DEFAULT, MONET, CATPPUCCIN, GREEN_APPLE, LAVENDER,
  MIDNIGHT_DUSK, NORD, STRAWBERRY_DAIQUIRI, TAKO, TEALTURQUOISE, TIDAL_WAVE, YINYANG, YOTSUBA, TOKYONIGHT,
  MONOCHROME and the hidden DARK_BLUE/HOT_PINK/BLUE from `AppThemePreferenceWidget.kt` / `AppThemesList`. On
  start, a stored legacy value maps to **COLOUR** (one-time migration, also when restoring an old backup). Keep the
  enum entries and colour-scheme files for now (Tsundoku merges + backups stay readable) — just unreachable; list
  them for a later deletion card. Remove the stock-UI branches only where trivial; don't rip out `TsuzukiStyle.NONE`.
- **Novel reader themes (Tsundoku's):** `NovelPage.kt` `novelThemes` (app/light/dark/sepia/black/grey/custom) and
  the Font colour / Background colour lists disappear from Tsuzuki UI; the only list is ours (Book/Paper/Sepia/Sage/
  Night/Black + Custom). Map stored `pref_novel_theme`: light→paper, dark→night, grey→night, sepia→sepia,
  black→black, app→book, custom stays custom (font/background colour prefs kept for Custom).
- Tests: theme migration (every legacy value → COLOUR; COLOUR/DOT untouched), novel theme mapping, backup restore of
  a legacy theme value. Device check (Claude): Appearance shows two cards; switching works; reader colours intact.

**Result (Codex, 2026-11-07):** Branch `tsuzuki/q12` at `6deadf25d`.

- `TsuzukiThemePreferences.kt`, `UiPreferences.kt`, `ReaderPreferences.kt` and `App.kt` normalize stored themes and write canonical values at startup. `PreferenceRestorer.kt` applies the same mapping to incoming app backup preferences.
- `AppThemePreferenceWidget.kt` shows Colour and Dot only. `SettingsAppearanceScreen.kt`, `TsuzukiSettingsHeader.kt` and `TsuzukiAccentPicker.kt` expose the two cards, system/light/dark, Colour accent swatches and Dot cover colour.
- `TsuzukiPageColours.kt` supplies the seven page colours and the Custom editors to `NovelPage.kt`, `SettingsNovelReaderScreen.kt` and `TsuzukiReaderSettingsSheet.kt`. The pre-existing overlength subtitle in `SettingsNovelReaderScreen.kt` was wrapped for per-file Spotless.
- `ThemeUtils.kt`, `NovelViewer.kt`, `NovelWebViewStyler.kt` and `NovelWebViewViewer.kt` share palette resolution for TextView and WebView. The stored foreground and background overrides apply in Custom.
- `TsuzukiThemePreferencesTest.kt` covers every legacy enum value, the novel aliases and backup preference normalization. Eight locale `strings.xml` files provide the new labels.

Upstream files touched: `UiPreferences.kt`, `ReaderPreferences.kt`, `App.kt`, `PreferenceRestorer.kt`, `AppThemePreferenceWidget.kt`, `SettingsAppearanceScreen.kt`, `SettingsNovelReaderScreen.kt`, `NovelPage.kt`, `ThemeUtils.kt`, `NovelViewer.kt`, `NovelWebViewStyler.kt`, `NovelWebViewViewer.kt` and the eight `i18n` resources.

Retained for backup and upstream compatibility: DEFAULT, MONET, CATPPUCCIN, GREEN_APPLE, LAVENDER, MIDNIGHT_DUSK, NORD, STRAWBERRY_DAIQUIRI, TAKO, TEALTURQUOISE, TIDAL_WAVE, YINYANG, YOTSUBA, TOKYONIGHT, MONOCHROME, DARK_BLUE, HOT_PINK and BLUE enum entries and colour schemes. Deletion belongs to a later card.

Exact AGENTS.md replacement for the legacy theme bullet:

> - **App themes.** Colour and Dot are the only selectable app themes. Stored and restored legacy app themes resolve to Colour. Keep the legacy `AppTheme` entries, colour schemes and `TsuzukiStyle.NONE` for backup compatibility and Tsundoku merges. The shared novel page choices are Book, Paper, Sepia, Sage, Night, Black and Custom. The saved custom text and background colours apply in Custom.

Verification: per-file Spotless twice and JDK 21 `:app:assembleDebug :app:testDebugUnitTest` green (866 tests, 0 failures). All eight locales include the new labels.

Claude device check: export a backup first and use preview test data. Restore settings from a backup with a legacy app theme and novel `light`, `dark`, `grey` or `app`, and verify canonical choices. Check two theme cards, switching, system/light/dark, all eight accent swatches and both Dot cover states on phone and tablet. Open TextView and WebView and compare Book, Paper, Night, Black and a saved Custom pair. Switch away from Custom and return to verify the saved colours.

#### Q10 Docs: JDK 21 path (T1) — `done` (Claude 2026-11-06: AGENTS.md Build picks jbr 21 or Temurin 21; owner phone in device table)
`AGENTS.md` Build section still says JDK 21 is Android Studio's `jbr`; Android Studio 2026.2 ships JDK 25. Point to
Temurin 21 (`C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot`) and note the owner's phone serial
`R5GL23ZYE1V` (release app `io.github.manojx2005.tsuzuki`) in the device table. Docs only.


### Extra wave X (owner approved 2026-11-06; only after the batch Q rows above)

General: each X card first checks whether the work already exists (grep `presentation/tsuzuki/`, the status
table in `docs/design/README.md`). If it is already done, set `done` with a one-line Result saying where, and
move on.

#### X1 Reader hook consolidation (T3) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night2` → `6af172281`; follow-ups in Q14)
WORKPLAN P0.4. Start from `git diff $(git merge-base HEAD tsundoku/main) HEAD -- app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt`
(includes Q2's additions once merged). Move every Tsuzuki addition (reader transition animation selection,
`ReadingDays` recording, contents drawer relaunch, Tsuzuki chrome, Q2's next-chapter hooks) into
`presentation/tsuzuki/reader/TsuzukiReaderHooks.kt` (or a few small files there); the upstream files keep
one-line calls. **Behaviour identical**: no logic changes, no re-indent of Tsundoku code; Tsundoku's own novel
branches stay where they are. Tests for any pure helper you extract. Result lists each moved block
(upstream line → new function) so Claude can verify on devices: novel TextView + WebView open/close animation,
progress/history saved, `ReadingDays` minutes increase, contents drawer jump, manga paged + long strip.
Result: _pending_

**Result (Codex, 2026-11-07):** `tsuzuki/x1` @ `c54d8fba3`. **890 tests, 0 failures.** Per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green with JDK 21.
- `ReaderActivity.kt` pre-card lines 167, 936–946, 1238–1242 and 1626–1628 → `TsuzukiReaderHooks.kt`, `TsuzukiReaderChromeRequests.bind/openContents` and `TsuzukiReaderChrome.Overlay`. The activity owns the request lifetime, with the drawer and find state still scoped to the novel composition. The chrome binding provides contents, find and adjacent-button availability.
- `ReaderActivity.kt` pre-card lines 320–327 and 1734–1779 → `TsuzukiAdjacentChapterHooks.kt`, `TsuzukiAdjacentNavigation.observe/navigate`. Preserves the last pending direction, paged reset before load, WebView direction notification on success, and then page reset. Tsundoku manual versus TTS entry points keep their existing TTS behavior.
- `ReaderViewModel.kt` pre-card lines 873–881 and 1112–1125 → `isTsuzukiChapterOnDisk`, `markTsuzukiChapterRead`, and `State.resolveTsuzukiAdjacentChapter`. The last still checks for a viewer chapter before evaluating the lazy chapter list. The transition choice and ReadingDays write were already delegated through `applyTsuzukiReaderTransition` and `recordTsuzukiReadingTime`. The N20 disk helper and Q2 visible-chapter reset remain one-line hooks.
- `TsuzukiReaderHooksTest.kt` — adds lazy adjacency and chapter-end mark-read coverage. Existing transition, contents and Q2 pending/paged navigation tests still pass. Upstream touched: `ReaderActivity.kt` and `ReaderViewModel.kt` only.
- Claude device checks: on tablet and phone, open and close a novel in TextView and WebView, save progress, verify History and ReadingDays minutes, open Contents from the chrome and the end card, and jump to another chapter. Check find in WebView, infinite-scroll visible-chapter navigation, a second tap during a pending load, paged WebView edges, TTS handoff, and manga paged and long strip.

#### X2 Tablet Updates: list + chapter preview (T3) — `done` (already built: Claude saw list + preview pane on the tablet 2026-11-06; card kept for reference)
Board `docs/design/boards/png/11-tablet/TabletUpdates.png` (+ `dot/`), HTML `html/11-tablet/TabletUpdates.html`.
On windows ≥ 900 dp wide (same breakpoint helper as N9 Tablet Browse / N25 side panel — reuse it), Updates
becomes two panes: the existing Tsuzuki updates list on the left, the selected chapter's book card on the right
(cover, title, chapter number/title, date, downloaded state, actions READ / DOWNLOAD / OPEN BOOK). Phone layout
unchanged. Selection survives rotation (`rememberSaveable` chapter id). Real data only: no excerpt unless it is
cheap and real — otherwise leave it out and say so. Upstream `UpdatesScreen.kt` keeps one dispatch.
Tests: pane/selection logic.
Result: _pending_

#### X3 Restyle check: ReplaceRules + SettingsTranslation (T2) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night2` → `6af172281`; follow-ups in Q14)
Boards `png/06-reader/ReplaceRules.png`, `png/08-tools/SettingsTranslation.png` (+ `dot/`, HTML alongside).
Find the screens (Tsundoku text replacement rules, translation engine settings). If they are stock in Tsuzuki
themes, restyle with `PreferenceScreen` / `tsuzukiGroupedTile` / `TsuzukiPill` per the boards (dispatch at the
top of the upstream composable). Never show an API key in clear text; add no network calls. Leave out any board
stat without backing data.

**Result (Codex, 2026-11-07):** `tsuzuki/x3` @ `09c787c50`. **891 tests, 0 failures.** Per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green with JDK 21.
- Replacement Rules already dispatches to `TsuzukiReplaceRulesSection` from Q11 More. Verified its themed rule tiles, toggles, live test input, regex validation and add/delete actions against both boards. Chapter excerpts, match counts and per-book/per-source scopes have no backing data here and were left out.
- Translation already uses themed `PreferenceScreen` groups and wrapped engine pills. `SettingsTranslationScreen.kt` now routes nine API keys and Custom HTTP headers through a sensitive preference and displays localized pass/fail for the existing Test action. Provider responses and exception messages stay out of the UI. The existing Test request still runs only when explicitly tapped.
- `TsuzukiSensitivePreference.kt` — fixed mask on configured rows, blank replacement editor with fully obscured typing and a secure window. The saved value is never prefilled or copied into dialog state. Custom headers are replaced as a whole because they can contain credentials.
- Upstream `Preference.kt`, `PreferenceItem.kt` and `EditTextPreferenceWidget.kt` carry the sensitive flag and dispatch. `SettingsTranslationScreen.kt` opts in credential fields and uses the safe test copy. The eight locale files add two test status labels. No new pure logic warranted a unit test.
- Claude device checks: in Dot and Colour on phone and tablet, open Q11 More → Replacement Rules, toggle a rule, enter valid and invalid regex in a disposable draft and compare the live test result. In Translation switch engines, open a disposable saved credential and Custom HTTP headers, verify blank/masked entry, rotation and cancel, and check that Test shows only pass/fail or not configured. Use a fixture for any provider error that echoes credentials.

#### X4 Restyle check: DuplicateFinder + Migration (T2) — `done` (Claude reviewed + device-checked, merged into main 2026-11-07 via `review/night2` → `6af172281`; follow-ups in Q14)
Boards `png/08-tools/DuplicateFinder.png`, `png/04-browse/Migration.png`, `png/04-browse/MigrationCompare.png`
(+ `dot/`). `presentation/tsuzuki/migration/` exists: compare it to the boards first and fix only what differs
(list the differences in Result). DuplicateFinder: restyle if stock. No change to migration logic.

**Result (Codex, 2026-11-07):** `tsuzuki/x4` @ `7ce6588b0`. **888 tests, 0 failures.** Per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green with JDK 21.
- Duplicate Finder already dispatches to `TsuzukiDuplicateFinderScreen`. Verified its matching modes, keep-one pills, grouped tiles, selection counts and actions against Dot and Colour. No second layout was needed.
- `TsuzukiMigrationSetup.kt` adds the selected-book summary and a full-width Find matches action. It reads up to five real book titles/covers off the main thread and uses the actual selected book/source counts.
- `MigrationConfigScreen.kt` supplies the setup summary and footer, themed header/rows and Dot source casing. `MigrationConfigScreenSheet.kt` uses themed flag pills, switches and the primary button while retaining the existing options sheet and the same flag preference callbacks. These two are the upstream files touched.
- `TsuzukiMigrationList.kt` keeps OLD/NEW cards with an accent outline, adds available author metadata, omits unknown latest-chapter filler and uses a Back affordance routed to the existing exit flow. Skip and Migrate remain prominent on successful matches. Copy and Search manually move to More and still use the existing callbacks.
- The eight locale files add setup count labels. The board's confidence score, candidate list, retained highlights/download estimates, and per-book chapter progress were left out because those values are unavailable to these UI routes. The setup retains the reorderable source list and the separate options sheet. Migration flags, matching, transfer and skip logic stay in their existing implementation.
- Claude device checks: in Dot and Colour, open Duplicate Finder and exercise selection without deleting real entries. Select one and several disposable books for Migration, check setup book names and source count, source order, flags and the primary footer. Run a disposable search to see successful/unmatched rows, ensure Back still opens the exit dialog, and check Skip, Migrate, Copy and Search manually in their expected positions.

### Anime lane A (owner approved 2026-11-06) — read `docs/ANIME_SCHEMA.md` §9 decisions first

Common rules: new code under `app/.../tsuzuki/anime/`, `domain/.../anime/`, `data/.../anime/` (packages per
ANIME_SCHEMA §7); Apache-2.0 only — Aniyomi files may be ported only if their header is Apache-2.0, note each
ported file + upstream revision in Result; nothing visible to normal users in release builds until A2.4 (Watch
tab already exists as a placeholder). No GPL. Never download or install anything yourself — no network in tests.

#### A1.5 Anime: debug extension install → list sources → play (T3) — `done` (Claude reviewed, merged into main 2026-11-07 via `review/night4` → `c7c805cf2`)
Board: `docs/design/boards/fixes-2026-11/AnimeDebug.html` / `png/AnimeDebug.png` (Dot only).
Goal: from a **debug-only** developer screen, add an Aniyomi-format extension repo URL, list its extensions, install
one into the private dir (`files/anime-exts`, via the loader ported in `9b2a924de` with the host's trust/signature
check), list the installed sources, fetch Popular for one source, open one entry's episode list, resolve the video
list of the first episode and play it in the N22 debug player (`anime-player` module, debug only).
- Repo index: Aniyomi repos publish `index.min.json` (+ `apk/` and `icon/`); parse it with a pure parser
  (`AnimeRepoIndexParser`) + tests from a checked-in fixture file. Accept a GitHub repo URL and map it to the raw
  `repo` branch URL the way the manga repo code does (look at `DefaultSourceRepos.kt` / manga repo handling).
- Owner-approved test repos (Claude tests with these, Codex only writes code): `https://github.com/yuzono/anime-extensions`,
  `https://github.com/Secozzi/aniyomi-extensions`. Store added repos in a debug preference, not the DB (Q6 later).
- Network calls go through the existing `NetworkHelper` client; all work off the main thread; errors shown as text
  on the screen (not swallowed).
- Entry point: a row in the existing debug/developer settings screen (find where N22's debug player is launched).
- Tests: index parser, URL mapping, install-state model. Result: what Claude must tap to play an episode.

**Result (Codex, 2026-11-07):** `tsuzuki/a1.5` @ `03038f852`. **893 tests, 0 failures.** JDK 21, per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green.
- `AnimeRepoIndexParser.kt` validates JSON entries and maps GitHub URLs to their repo branch, including Yūzōnō's separate anime-repo alias. `AnimeRepoIndexParserTest.kt` and the synthetic `anime-repo-index.json` cover parsing, URL mapping, unsafe paths and the four install states.
- `AnimeDebugRepository.kt` keeps repo URLs in a debug preference, fetches via NetworkHelper, checks the APK package/version/signature and installs via the existing private loader. Existing packages are reused. Untrusted packages require a separate fingerprint confirmation before they load. No third-party source code was copied.
- `AnimeDebugActivity.kt` adds the five-step Dot flow through Popular, episodes and playable video choices. The debug manifest registers it privately. Upstream hook `DebugInfoScreen.kt` adds the row only to the debug build. Eight locale files add the new interface labels.
- `AnimeDebugPlayback.kt` and the N22 `AnimeDebugPlayerActivity.kt` hook hand the selected video to mpv in memory and support Referer, User-Agent and Origin. This debug path omits extension-specific mpv arguments, cookies, alternate tracks and local servers. Only the JSON index is in scope. Secozzi's current advertised protobuf URL has not been exercised.
- Claude device check on the scratch arm64 debug install: Settings > Developer/Debug info > Anime test. Enter the approved Yūzōnō GitHub URL, tap Load, Install one extension, then Trust after checking the package, version and fingerprint. Tap Popular on its source, Episodes on a title, Load first episode videos, then Play on a video. Confirm playback and seek, Back returns to the flow, and reopening lists the saved repo and trusted source. Repeat repo loading with the approved Secozzi URL and record any live index error. No repo/APK was fetched or installed by Codex.

#### A2.2 Anime tables + data layer — migration 36 (T4) — `done` (Claude reviewed, merged into main 2026-11-07 via `review/night4` → `c7c805cf2`)
Implement ANIME_SCHEMA §2–§3 and §7 **with the §9 decisions** (separate `anime_categories`; `anime_sync`; season
columns kept; no fansub exclusion; no `extension_store` column).
- `ls data/src/main/sqldelight/tachiyomi/migrations | sort -n | tail -1` → use latest + 1 (35 → `36.sqm`). One
  file, additive only: never alter `mangas`, `chapters`, `history`, `categories`, `mangas_categories`.
  `.sq` files for `animes`, `episodes`, `anime_history` (with the `ifnull` guard trigger), `anime_categories`,
  `animes_categories`, `anime_sync`, `anime_sources`. Seed `anime_categories` row `_id 0` in the migration.
- Domain models + repositories + interactors (get/insert/update anime, episodes by anime, history upsert, anime
  categories CRUD + reorder, set anime categories, tracks), Injekt registration next to the manga ones.
- **Tests (must all pass):** (1) in-memory SQLite: create the DB at version 35 with fixture rows in
  mangas/chapters/history/categories, run the migration, assert every old row unchanged and new tables empty
  except `anime_categories` `_id 0`; (2) fresh-install schema equals migrated schema (compare `sqlite_master`);
  (3) repository round-trips; (4) history trigger with NULL `last_seen`. Write the rollback note (ANIME_SCHEMA §3.4)
  into Result.
- Backup proto: **not in this card** (A2.5 later).
- Claude: line-by-line review + kotlin-reviewer agent, then scratch-install upgrade test on the tablet (a second
  install of an older build, never the owner's apps), then merge (owner allowed if all pass).

**Result (Codex, 2026-11-07):** `tsuzuki/a2.2` @ `6323ef6bb`. **910 tests, 0 failures.** JDK 21, two per-file Spotless passes and `:app:assembleDebug :app:testDebugUnitTest` green. No device actions.
- `36.sqm` creates the seven anime tables, matching the new `animes.sq`, `episodes.sq`, `anime_history.sq`, `anime_categories.sq`, `animes_categories.sq`, `anime_sync.sq` and `anime_sources.sq`. It seeds only the protected anime default shelf. Episode counters and guarded history timestamps are maintained separately from books. The four season columns are stored, and the source/URL index and category links are unique.
- `AnimeDatabaseAdapters.kt` registers enum, memo, list and date columns. `AnimeRepositoryImpl.kt` handles identity and library queries. `EpisodeRepositoryImpl.kt` handles episode reads, inserts and progress. `AnimeHistoryRepositoryImpl.kt` handles accumulated watch seconds and one latest watch per anime. `AnimeCategoryRepositoryImpl.kt` handles shelves, order and transactional links. `AnimeTrackRepositoryImpl.kt` upserts stable tracker identities. `AnimeSourceRepositoryImpl.kt` persists stub source metadata.
- Domain `Anime.kt`, `Episode.kt`, `AnimeHistory.kt`, `AnimeCategory.kt`, `AnimeTrack.kt`, `StubAnimeSource.kt` and `LibraryAnime.kt` define separate anime records. Their repository contracts and `AnimeInteractors.kt`, `EpisodeInteractors.kt`, `AnimeHistoryInteractors.kt`, `AnimeCategoryInteractors.kt`, `AnimeTracks.kt` and `GetLibraryAnime.kt` expose the A2 operations.
- Upstream `AppModule.kt` wires the adapters and `DomainModule.kt` registers the repositories and interactors. `app/build.gradle.kts` and `gradle/libs.versions.toml` add a test-only SQLDelight JDBC driver. `AnimeDatabaseTest.kt` tests unchanged book fixtures, fresh/upgrade schema equality, repository round-trips, cascades and NULL history. Since the repo has no pre-anime schema snapshot, the tests reconstruct schema version 36 by removing only the seven new tables before applying migration 36 to reach version 37. A separate in-memory SQLite run compared all 80 schema entries.
- No backup proto, season UI, fansub exclusion or extension-store schema changes. Watch time remains in anime history. No Aniyomi implementation files were ported.
- Claude review gate: review migration line by line. In a scratch tablet profile only, copy and hash the old build's 12-file DB/WAL/SHM, prefs and WorkManager backup, install the migration build, compare all old book rows and schema, confirm only anime shelf 0 is seeded and run integrity/foreign-key checks. Verify the old novel/manga Home, Library, History and Updates still open. Also check fresh startup. For rollback, reinstall the pre-migration APK, restore the 12 backed-up files and verify hashes. SQLite does not support reopening the upgraded DB with the older app. Never run the upgrade or rollback drill on the owner's live data.

#### A2.3 Anime sources + browse catalogue (T3) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (`tsuzuki/a2.3`)
**Claude 2026-11-07:** A1.5 + A2.2 are being merged into main by Claude (string conflicts resolved there); when the A2.2 card says `done`, start A2.3 from main (no prerequisite merge needed).
Register installed anime extensions as sources in an `AnimeSourceManager` (stub via `anime_sources` when the
extension is gone). Browse → a WATCH/ANIME media pill alongside novels/manga only when ≥ 1 anime extension is
installed (stays hidden otherwise; release users see nothing new). Catalogue screen: Popular / Latest / Search +
filters using the existing Tsuzuki browse grid components, cover → anime details stub (title, cover, description,
episode list from the source, PLAY first/next episode in the player). Boards: `docs/design/boards/png/10-anime/`
(BrowseAnime, AnimeDetails; Dot + Colour). Tests: source registration + stub fallback.

**Result:** `tsuzuki/a2.3` at `e5ab544`, based on current main with A1.5 and A2.2. Debug Browse shows the Watch
pill when trusted anime sources are installed. The catalogue supports Popular, Latest where supported, and
filtered Search. Its details stub shows source metadata and episodes, with first/next playback in the existing
debug player. Catalogue search and filters are retained when returning from details.

- `AnimeSourceManager.kt` registers installed sources and persists source stubs for use after uninstall.
- `TsuzukiAnimeBrowse.kt`, `TsuzukiBrowseHeader.kt`, and upstream `BrowseTab.kt` connect the debug Browse pages.
- `AnimeCatalogueActivity.kt`, `AnimeDetailsScreen.kt`, `AnimeFilterDialog.kt`, debug manifest and eight debug
  resource files implement the catalogue and details. Anime library and watch progress remain in A2.4.
- Source registration/uninstall tests and Browse gate/order tests pass. Per-file Spotless passed twice, debug
  built, and 957 app unit tests passed without failures or skips. The catalogue activity is registered in the
  debug manifest only. `docs/design/README.md` records status.
- Claude: on a debug device check the Watch pill before install, after trusting an extension, and after uninstall.
  Open its Popular/Latest/Search, apply a filter, open a result and return, then play the first and next episodes.
  Verify both Dot and Colour and spot-check both novel and manga Browse.

#### A2.4 Anime library + Watch tab + anime shelves (T3/T4) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was )
**From the A2.2 review (fix in A2.4):** `AnimeCategoryRepositoryImpl.setForAnime` must not insert a link row for the default shelf `0` when other shelves are chosen; `AnimeRepositoryImpl.insert` returns the existing id for a duplicate (source, url) without updating fields — call `update` after it when adding to the library.
Boards: `fixes-2026-11/AnimeShelves` + `ColourAnimeShelves` (shelf management), `png/10-anime/AnimeLibrary`, `AnimeHome` (with Q5's nav).
Add to library from anime details (writes `animes.favorite`, anime categories picker using `anime_categories`).
Home's Watch tab becomes real: Continue watching (from `anime_history`), shelf row; Library gets the anime media
switch (board `LibraryMixed`, `AnimeLibrary`, `AnimeHome`) showing anime shelves only. Anime shelf management
screen reusing the Tsuzuki category components with the anime repository. Episode progress saved from the
player (position, seen at ≥ 85 %). Watch minutes recorded separately (not in `ReadingDays`). Boards in
`png/10-anime/`. Tests: progress threshold, continue-watching ordering, shelf CRUD.

**Claude 2026-11-08:** the earlier "Needs A2.3 first" result is superseded: `tsuzuki/a2.3` (`e5ab544`) now has the catalogue, details stub and player hooks. Start from `main`, merge `tsuzuki/a2.3` into the worktree, then do this card. Episode progress here is saved from the existing player; A3.2 replaces the player later.

Result (Codex 2026-11-08): `tsuzuki/a2.4` at `c52c5cffc`, includes A2.3.

- `AnimeLibraryStore.kt` saves anime, favorite/shelf assignments, episode state and watch history transactionally.
  The existing source/URL row is reused and updated. The read-only membership query and category repository
  keep Default implicit and prevent renaming it. No schema migration.
- `TsuzukiAnimeLibrary.kt` adds the anime library, shelf tabs and Watch content. `AnimeShelvesScreen.kt` manages
  anime shelves separately. Continue Watching picks the most recently played eligible favorite and advances
  to the next unseen episode in episode order. Airing and updates stay with their respective later cards.
- `AnimeDetailsScreen.kt` and `AnimeCatalogueActivity.kt` connect favorite actions, shelf selection and exact
  episode resume. `AnimeDebugPlayback.kt`, `AnimeDebugPlayerActivity.kt` and `AnimePlaybackProgress.kt` restore
  position, mark seen at 85%, and count played seconds separately from reading days. Incognito skips progress.
- Upstream hooks: `SettingsLibraryScreen.kt` adds the debug anime shelf entry, `NovelsTab.kt` adds the anime
  selector and `HomeScreen.kt` restores that selection. `AppModule.kt` registers the store.
- JDK 21 per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest` green, 960 tests and zero failures.
  The merged manifests contain the catalogue in debug and omit it in preview/release.
- Claude device check on Tab S11 and S22: in Dot and Colour, create/reorder/rename/delete a custom anime shelf,
  verify Default and book shelves, add/remove a favorite, play and resume from Watch, then finish past 85% and
  verify the next episode. Verify preview/release show the Read/Watch teaser with no anime library or Browse pill.

#### Anime lane order from 2026-11-08 (Claude) — detailed plan in WORKPLAN §6.3–6.5

(2026-11-08: the v0.1 freeze is over; Claude merges the chain into `main` after R-A5.) **Don't wait for main**: each
card starts a worktree from `main` and merges the card branch in its **Needs** column (that branch already contains the earlier chain). Gate every entry point with
`TsuzukiAnime.enabled` (A3.0; until then `BuildConfig.DEBUG`). Release/preview builds must show nothing anime.
Every card: board first (Dot then Colour), tokens + typography only, real data only, strings in all 8 locales,
tests named in the card, per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest`, Result + `review`.
Card format and verify commands: WORKPLAN §0.4 and the appendix skeleton.

| # | Card | Needs (merge this branch) | Tier | Effort |
|---|---|---|---|---|
| 1 | A2.4 Library + Watch tab + shelves | `tsuzuki/a2.3` | T3/T4 | high |
| 2 | A3.0 Anime out of the debug source set, one flag | `tsuzuki/a2.4` | T2 | medium |
| 3 | A3.1 Anime details + episodes | `tsuzuki/a3.0` | T3 | high |
| 4 | A3.2 Production player | `tsuzuki/a3.1` | T4 | xhigh |
| 5 | R-A2 Player settings + sheets | `tsuzuki/a3.2` | T3 | high |
| 6 | A3.3 Anime extension store | `tsuzuki/r-a2` | T4 | high |
| 7 | A3.4 Anime history | `tsuzuki/a3.3` | T2 | medium |
| 8 | R-A3 Updates + Airing today | `tsuzuki/a3.4` | T3 | high |
| 9 | A3.5 Anime trackers (AniList, MAL) | `tsuzuki/r-a3` | T4 | high |
| 9b | R-A5 Anime chain upstream-diff fixes | `tsuzuki/a3.5` | T3 | medium |
| 10 | A3.6 AniSkip | `tsuzuki/r-a5` | T3 | medium |
| 11 | R-A1 Anime backups | `tsuzuki/a3.6` | T4 | high |
| 12 | R-A4 Episode downloads | `tsuzuki/r-a1` | T4 | xhigh |
| 13 | A3.7 Watch-time stats | `tsuzuki/r-a4` | T2 | medium |
| 14 | A3.8 Anime global search | `tsuzuki/a3.7` | T2 | medium |
| 15 | A3.9 Continue-watching widget | `tsuzuki/a3.8` | T3 | medium |
| 16 | A4.1 Anime restyle parity pass | `tsuzuki/a3.9` | T2 | medium |
| 17 | A4.2 Release packaging for anime | `tsuzuki/a4.1` | T3 | high |
| 17b | A4.3a Anime user setting instead of the debug flag (Batch T) | `tsuzuki/a4.2` | T2 | medium |
| — | A4.3 / A4.4 device sweep, owner sign-off, migration upgrade test | review lane + owner | — | — |
| 18 | A5.1 Picture-in-picture | `tsuzuki/a4.3a` | T3 | medium |
| 19 | A5.2 Seasons grouping | `tsuzuki/a5.1` | T3 | high |
| 20 | A5.3 Fansub / group filter | `tsuzuki/a5.2` | T2 | medium |
| 21 | A5.4 Simkl tracker | `tsuzuki/a5.3` | T3 | high (blocked on owner) |
| 22 | A6.1 Kitsu tracker | `tsuzuki/a5.3` | T3 | high |
| 23 | A6.2 Migrate anime to another source | `tsuzuki/a6.1` | T3 | high |
| 24 | A6.3 Duplicate check on add | `tsuzuki/a6.2` | T2 | medium |
| 25 | A6.4 Preferred audio/subtitle languages | `tsuzuki/a6.3` | T2 | medium |
| 26 | A6.5 External player | `tsuzuki/a6.4` | T2 | medium |
| 27 | A6.6 Airing times from AniList | `tsuzuki/a6.5` | T3 | medium |
| 28 | A6.7 Player screenshot | `tsuzuki/a6.6` | T2 | low |
| 29 | A6.8 Local anime source | `tsuzuki/a6.7` | T3 | high |
| 30 | A6.9 Playback robustness pass | `tsuzuki/a6.8` | T3 | high |

**From 2026-11-12 rows 26–30 are superseded by the Batch AY order** (A6.4 → AY0 → AY1 → AY2 → AY3 → AY4 → AY6 → A6.5 → AY5 → A6.8 → AY7 → A6.6 → A6.7 → AY8; A6.9 dropped).

A linear chain keeps merges simple. A merge conflict in the Needs step → `git merge --abort`, set `blocked`
("needs Claude to merge"), next card. If the Needs branch is not `review`/`done` yet, the card waits (skip it).
Branch names for R-A cards: `tsuzuki/r-a2`, etc.

#### A3.0 Anime out of the debug source set, one flag (T2) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was )
Goal: the anime catalogue, details and player stop being debug-only code so later cards build real features, while
normal users still see nothing. No UI change.
Files: move `app/src/debug/.../tsuzuki/anime/debug/{AnimeCatalogueActivity,AnimeDetailsScreen,AnimeFilterDialog,
AnimeDebugPlayerActivity,AnimeDebugPlayback}.kt` (and what they need) to `app/src/main/.../tsuzuki/anime/ui/`,
renamed without `Debug`; their manifest entries go to the main manifest, `exported=false`. Keep `AnimeDebugActivity`
(repo/APK install test screen) in `src/debug`. Add `tsuzuki/anime/TsuzukiAnime.kt` with `val enabled: Boolean`
(= `BuildConfig.DEBUG` for now; A4.3 turns it into a setting) and route every anime entry point through it: Browse
Watch pill, Home Watch tab content, Library anime switch, anime shelves (the Read/Watch switch stays a teaser, V3).
`anime-player` stays `debugImplementation`: put the player behind a small `AnimePlayerLauncher` interface in main,
the mpv implementation in `src/debug` (bound through a debug-only Injekt module), and a release no-op that reports
"unavailable". A4.2 changes this.
Tests: `TsuzukiAnimeGateTest` (flag false → Browse/Home/Library expose no anime destinations); a check that the
release merged manifest has no exported anime activity. `:app:assemblePreview` must also build.
Report: moved files, the gate points, preview APK size before/after (must stay within ±0.2 MB).

Result (Codex 2026-11-08): `tsuzuki/a3.0` at `55d5a5aee`, includes A2.4.

- Moved `AnimeCatalogueActivity.kt`, `AnimeDetailsScreen.kt`, `AnimeFilterDialog.kt`, playback request handling,
  and the player activity to `tsuzuki/anime/ui/` in main. `AnimeStreamResolver.kt` shares the extension API
  adapter with the debug installer. `AnimePlayerLauncher.kt` separates playback from the native backend.
  Debug injects mpv. Other variants inject an unavailable launcher with localized feedback.
- `TsuzukiAnime.enabled` gates the Browse Watch pill, real Home Watch content, the Library switch and saved
  routes, anime shelf settings, and both activities. The Watch teaser stays visible in preview and release.
  `AnimeDebugActivity` stays debug-only. Both shared activities are private, so playback starts from an
  in-app episode selection with a one-use token.
- Upstream hooks: `App.kt` imports the variant binding, `BrowseTab.kt`, `NovelsTab.kt` and
  `SettingsLibraryScreen.kt` read the flag, `app/build.gradle.kts` adds the fallback Kotlin source set,
  and the main/debug manifests declare the activities.
- JDK 21 per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest :app:assemblePreview` green.
  962 tests, zero failures. All seven anime activity declarations across the three merged manifests have
  `exported=false`. The five preview APKs contain neither mpv nor player native libraries.
- Preview APK size from A2.4 to A3.0: arm64 37,424,059 → 37,446,959 bytes and universal 77,257,922 →
  77,280,822 bytes. Every preview APK grew by 22,900 bytes, within the 0.2 MB budget.
- Claude device check on S22 and Tab S11: open a debug catalogue, filter it, play and resume a saved episode,
  then verify the preview Watch teaser and that Browse, Library and Settings omit anime destinations.

#### A3.1 Anime details + episodes (T3) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was )
Board: `docs/design/boards/png/10-anime/AnimeDetails.png` + `dot/AnimeDetails.png` + `html/10-anime/AnimeDetails.html`.
Goal: replace the A2.3 details stub with the real anime details page, modelled on Tsuzuki's book details (Q6 header,
`TsuzukiTile`, `TsuzukiPillRow`) but for episodes.
Steps: (1) `AnimeDetailsScreenModel` loads the anime from `animes` (or the source when not in the library), refreshes
details + episodes from the source on open and on pull-to-refresh, and upserts `episodes` (new episodes get
`date_fetch`; removed ones are kept if seen or downloaded, Mihon-style). (2) Header: cover, title, studio/author,
status, source, genre pills, synopsis expand. (3) Episode list: number, name, date, group, seen state, progress bar
from `last_second_seen/total_seconds`, bookmark, filler mark only if the source sets it. Sort (number/date, asc/desc)
and filters (unseen, bookmarked), saved per anime. (4) Actions: PLAY EP n / RESUME EP n (first unseen or last in
progress), mark seen/unseen (single, multi-select, "previous seen"), bookmark, add/remove library (A2.4 picker), open
in WebView, share. (5) Season columns ignored (owner Q3).
Real data: no fillers, groups or ratings unless the source returns them. Tests: episode upsert/removal rule,
resume-target choice, sort/filter persistence. Claude checks: a real extension's anime, refresh, mark seen, resume.

Result (Codex 2026-11-08): `tsuzuki/a3.1` at `9ef03a1e9`, includes A3.0.

- `AnimeDetailsScreenModel.kt` loads stored metadata and refreshes source details and episodes on open or pull.
  Cached episodes remain usable when refresh fails. `AnimeLibraryStore.kt` updates source fields while preserving
  episode IDs and user state. Removed rows with seen state, bookmarks, progress or a supplied offline ID are
  retained. R-A4 will supply offline IDs from the anime downloader. The SQLDelight changes add queries only.
- `AnimeDetailsHeader.kt`, `AnimeDetailsScreen.kt`, and `AnimeEpisodeTiles.kt` implement the themed header,
  synopsis, genres, library and shelf actions, WebView and share, episode art where supplied, group/date/filler
  metadata, progress, and single or multiple episode watch/bookmark actions. The resume tile picks the latest
  in-progress episode or the first unseen episode. Season columns and later-card download/track actions are omitted.
- `AnimeEpisodeDisplay.kt` persists sort and filters per anime in reserved high bits of the episode flags.
  Source and repository changes are confined to anime files. No schema or migration changes.
- Per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest` green, 965 tests and zero failures.
  The database tests exercise refresh/removal, preserving user state, recent resume, and per-anime filter persistence.
- Claude device check on S22 and Tab S11 in Dot and Colour: open a real extension's title, pull to refresh,
  check details and thumbnail data, mark seen/unseen and bookmark singly and in a selection, reopen to verify
  saved sort and filters, then start and resume a partial episode. Try WebView and share from the overflow.

#### A3.2 Production player (T4) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was )
Board: `png/10-anime/Player.png` + `dot/Player.png` + `html/10-anime/Player.html` (landscape 844×390).
Goal: one player that plays an episode from a real source the way Aniyomi users expect.
Steps: (1) Resolve videos through the anime source API in `source-api` (hosters → videos), pick the preferred quality
(pref, default highest), pass request headers + referer to mpv, fall back to the next video on load error with a
snackbar. (2) Controls overlay per board: title/episode, back, seek bar with buffered range, play/pause, ±10 s
double-tap, prev/next episode, lock, orientation lock, tap to hide (3 s). (3) Resume from
`episodes.last_second_seen`; save position every 10 s, on pause and on exit; mark seen at ≥ 85 % (reuse A2.4's
function); write `anime_history` and watch minutes. (4) Audio focus (pause on loss, duck on transient), pause in
background, keep screen on while playing, immersive mode, rotation without restarting playback (mpv in a retained
holder), process-death restore to the same episode + position. (5) Auto-play next episode pref (default on) with a
5 s countdown card at the end.
Files: `tsuzuki/anime/player/*` in main; the `anime-player` module only through `AnimePlayerLauncher` (A3.0). No GPL
code. Tests: video choice/fallback order, save cadence, seen threshold, next-episode choice. Claude checks: both
devices, rotation, background/foreground, audio focus with a notification sound, crash buffer, 16 KB.

Result (Codex 2026-11-08): `tsuzuki/a3.2` at `d41c05204`, includes A3.1.

- `AnimePlayerModel.kt` holds the native session across rotations and resolves each episode's stream queue.
  The player prefers the highest known resolution by default, remembers a chosen quality, and tries the
  remaining streams on failure. `MpvAnimePlayerLauncher.kt` passes headers through mpv and stays in debug.
- `AnimePlayerContent.kt` uses the Dot and Colour themes for the landscape overlay, buffered timeline, seeking,
  episode navigation, lock, orientation lock, stream fallback snackbar and five-second autoplay countdown.
  R-A2 owns the track, speed, decoder and gesture settings and sheets.
- The private player intent carries an episode ID so Android can restore the title and position after process
  death. The native surface reattaches without loading the current video again. Playback pauses in the
  background, observes media audio focus and saves progress at ten-second intervals and on pause/exit.
  The A2.4 85% threshold and separate watch history still apply. Incognito skips history writes.
- Changes stay in the anime packages and resources. Per-file Spotless twice,
  `:app:assembleDebug :app:testDebugUnitTest :app:compilePreviewKotlin` green, 969 tests and zero failures.
  The arm64 debug APK passed `zipalign -c -P 16 4` and all 11 packaged arm64 player libraries passed 16 KB ELF checks.
- Claude device check on S22 and Tab S11: use a real source with multiple streams, seek and reopen mid-episode,
  switch quality, rotate and background/foreground, test audio focus with a notification and another media app,
  restore an episode after process death, and check the countdown, controls lock and crash buffer in Dot and Colour.

#### A3.3 Anime extension store (T4) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was )
Boards: `png/10-anime/BrowseAnime.png` (extension stores part) + `dot/BrowseAnime.png`; ExtensionRepos
(`docs/design/boards/INDEX.md`). If a screen's anime part is missing from its board: `blocked` ("needs a board").
Goal: install anime extensions from a repo without the debug screen.
Steps: (1) Owner decision Q6: a small new SQLDelight migration after 36 adds a media flag to the extension-repos table
(`novel | manga | anime`, existing rows keep their type). (2) Adding a repo accepts Aniyomi-format anime repos
(`index.min.json`), shown with an ANIME tag. (3) Extensions gets an ANIME group (only when ≥ 1 anime repo):
available / installed / updates; install through the existing installer path into the private anime dir
(`files/anime-exts`, A1.5 loader), reuse the signature trust prompt, uninstall, update. (4) `AnimeSourceManager`
reloads on change. No network in tests. Tests: repo parse (fixture JSON), the media-flag migration from 36,
install-state mapping. Claude checks with one extension from an owner-approved test repo: install, update,
uninstall → stub.

Result (Codex 2026-11-08): `tsuzuki/a3.3` at `127d60577`, includes R-A2.

- Migration `37.sqm` adds the media flag and classifies existing stores as manga or novel. The existing
  reader queries and backup path see the same book-store rows. Anime stores use separate queries.
- `AnimeExtensionReposScreen.kt` accepts Aniyomi-format HTTPS indexes and shows the anime format tag.
  `AnimeExtensionsTab.kt` shows available, installed and update groups when an anime repo is configured.
  Existing installed anime sources remain accessible if the last anime repo is removed.
- `AnimeExtensionInstaller.kt` shares validation and installation with the debug tool. The private installer
  validates the archive package, version and signature, stages updates in the private directory and restores
  the previous APK if loading the replacement fails. The shared trust prompt shows the anime signature.
  Install, trust and removal refresh the anime sources and retain source stubs for saved shows.
- Upstream hooks: `SettingsBrowseScreen.kt` opens the anime repository page, `BrowseTab.kt` attaches the
  anime Extensions group, and `ExtensionsScreen.kt` exposes the existing trust prompt with optional details.
- JDK 21 per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest :app:compilePreviewKotlin` green.
  976 tests, zero failures. The repo fixture, install-state mapping, Browse gate and schema upgrade run offline.
- Claude device check on S22 and Tab S11: add an owner-approved test repo, install and verify the signature
  prompt, open a source, update from a higher version, uninstall, and open a saved show to confirm its stub.
  Verify novel and manga repo listings remain available after upgrading an existing installation.

#### A3.4 Anime history (T2) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was )
Board: History (`INDEX.md`, anime part; `10-anime/AnimeHome` continue-watching style). History gets an ANIME pill
beside novels/manga only when `anime_history` has rows. Row: cover, title, "EP n · 12:34 left", time; tap resumes in
the player; remove one / remove all for an anime; search. A separate repository query, no change to book history.
Tests: pill visibility, grouping by day, resume target.

Result (Codex 2026-11-08): `tsuzuki/a3.4` at `f399c7eeb`, includes A3.3.

- `TsuzukiAnimeHistory.kt` shows a dedicated anime filter when watch history has visible entries. It groups
  episodes by local day and filters by anime or episode name. Rows show the artwork, episode, time remaining
  or watched status, and the timestamp in Dot and Colour.
- Selecting a partial entry plays that episode. Selecting a finished entry starts the next unseen episode
  when present, otherwise it replays the selected episode. History removal hides one episode or all episodes
  of a show and keeps cumulative watch seconds and episode progress.
- Anime history uses separate SQLDelight queries. `HistoryScreen.kt` adds the pill and dispatches to the
  anime list. No database migration.
- JDK 21 per-file Spotless twice, `:app:assembleDebug :app:testDebugUnitTest` green, 980 tests and zero failures.
  Tests cover the gate, local-day grouping, search, resume, and history removal with watch-time preservation.
- Claude device check on S22 and Tab S11: play two episodes, open History and choose Anime, verify date groups
  and search, resume the partial entry, remove one record, then remove the rest for a show. Check Dot and Colour.

#### A3.5 Anime trackers: AniList + MyAnimeList (T4) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (.worktrees/a3.5, 2026-11-08)
Board: `png/10-anime/AnimeTracking.png` + `dot/` + html. Goal: the existing AniList and MAL logins also track anime.
Steps: anime search/bind/update for AniList (GraphQL `type: ANIME`, progress = episodes) and MAL (`/anime`,
`num_watched_episodes`), stored in `anime_sync`; a tracking sheet on anime details reusing the Tsuzuki tracking UI;
on episode seen, raise progress only if the tracker's count is lower (Mihon rule); status, score, dates. Simkl left
out (no client exists). Never log tokens. Tests: request bodies against fixtures, the progress-only-increases rule,
`anime_sync` mapping. Claude checks with a real account only if the owner logs in.

Result (Codex 2026-11-08, in progress): implementation in .worktrees/a3.5 on tsuzuki/a3.5.
Anime tracking uses existing AniList and MyAnimeList clients, stores links in anime_sync, and adds
an optional background progress sync and a tracking sheet. Migration 38 stores the MAL ID returned
by AniList for A3.6. Debug Kotlin compiled before the final formatting pass. Offline build and tests
are running; see spotless-a3.5.log and build-a3.5.log in the worktree.

#### A3.6 AniSkip (T3) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (.worktrees/a3.6, 2026-11-09; needs `tsuzuki/r-a5`)
Boards: `Player` (skip button) + `PlayerSettings` (AniSkip part). Call the AniSkip API
(`api.aniskip.com/v2/skip-times/<malId>/<ep>`) with the MAL id from an A3.5 binding (an AniList binding gives
`idMal`); no id → no button. A skip intro/ending/recap button for the segment's length; prefs: off / button /
auto-skip, intro offset. Cache per episode in memory. Tests: response parsing fixture, segment-at-position logic,
no id → disabled.

**Result (Codex, 2026-11-09):** `tsuzuki/a3.6` at `f4b6a0822` after clean Needs merge `90e708281`.

- `AniSkip.kt` looks up opening, ending, and recap intervals with the stored tracker MAL ID and a whole episode number. It validates and clips returned intervals, and caches successful or empty results in memory by MAL ID, episode and rounded playback duration.
- `AnimePlayerModel.kt` loads metadata only with AniSkip enabled and an eligible episode. Button mode seeks to the active segment's end. Auto mode honours the opening delay and skips each segment at most once per playback. Existing seek handling accounts for watch progress.
- `AnimePlayerContent.kt` adds the timeline markers and an active-segment pill that stays available with the chrome hidden. `AnimePlayerSettings.kt` and `AnimePlayerSettingsScreen.kt` provide Off by default, Show button, Auto-skip, and a 0–30 second opening delay. Eight labels are translated in all eight locales.
- `AppModule.kt` and `AnimePlayerActivity.kt` wire the lookup into the existing anime gate. `docs/design/README.md` records the status.

Verification: JDK 21, two per-file Spotless passes, `:app:assembleDebug :app:testDebugUnitTest` (1,019 tests, 0 failures, errors or skips), and locale placeholder validation. `AniSkipTest.kt` covers the response fixture, interval boundaries, opening delay, tracker IDs, no-ID network suppression, cache reuse and saved settings.

Device review: on the backed-up S22 Debug install, use an episode with a linked MAL or AniList binding and confirmed provider intervals. Check the pill in Dot and Colour with chrome shown and hidden, marker alignment, manual and automatic seeks, delay, replay into a skipped interval, and no button for an unlinked episode. Live provider playback awaits review.

RV3 checklist note: the R-A5 prerequisite already contains both unshipped migrations `37.sqm` and `38.sqm`. The latter provides tracker MAL IDs and is documented in `docs/UPSTREAM.md`. A3.6 adds zero schema changes.

#### A3.7 Watch-time stats (T2) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (.worktrees/a3.7, 2026-11-09)
Board: Stats (`INDEX.md`, anime part). Owner Q4: watch minutes stay separate from the reading streak and goal. Stats
gets a WATCH section only when there is watch data: minutes this week/month/all time, episodes seen, anime completed.
Data comes from the watch-minute store A2.4/A3.2 write. Tests: aggregation by day/week, empty state hidden.

**Result (Codex, 2026-11-09):** `4d7a7a28a` on `tsuzuki/a3.7`.

- `anime_watch_days.sq` and migration `39.sqm` keep local-day watch seconds per episode. New tracked
  playback updates the day and the existing lifetime history in the same transaction. Existing and
  restored episode history still contributes to lifetime totals. Its older seconds lack a reliable
  day breakdown, so they are excluded from week and month.
- `AnimeHistoryRepositoryImpl.kt` totals the calendar week (Monday start), calendar month, lifetime
  history and actual watched episodes. An anime counts as completed if its source status is
  Completed and all its known episodes are watched.
- `TsuzukiWatchStats.kt` adds a gated Watch section only when there is watch data. Duration units,
  counts and five labels are localized. The reading chart, goal and streak use their existing data.

Verification: JDK 21, two clean per-file Spotless passes, `:app:assembleDebug :app:testDebugUnitTest`
(1,032 tests, zero failures, errors or skips), and matching keys in all eight locales. The database
tests cover a week crossing a month boundary, repeat checkpoints, empty data, episode state, history
clearing and migration from schema 39 without losing lifetime totals.

Device review: on a backed-up S22 Debug install, check Stats in Dot and Colour before and after
watching an episode. Compare week, month, lifetime and episode counts, check a finished anime and
confirm the reading goal did not advance. Preview should have no Watch section.

#### A3.8 Anime global search (T2) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (.worktrees/a3.8, 2026-11-09)
Board: GlobalSearch (`INDEX.md`, anime part). An ANIME pill in global search when ≥ 1 anime source; searches
pinned/all anime sources in parallel (same limits as manga); results open A3.1. Tests: pill gate, source selection.

**Result (Codex, 2026-11-09):** `c65233360` on `tsuzuki/a3.8`.

- `TsuzukiAnimeGlobalSearch.kt` adds the Anime pill when the gate is enabled and at least one
  trusted anime source is installed. It searches all or pinned anime sources with at most five
  concurrent requests and keeps per-source results and elapsed time. Switching search inputs
  cancels the previous anime request.
- `TsuzukiAnimeBrowse.kt` adds persistent anime source pins. Selecting a result uses
  `AnimeLibraryStore.kt` to reuse or cache the entry, then opens the existing Anime Details
  route. Selecting the source opens its catalogue with the same query.
- The manga and novel search routes expose Anime alongside their existing media. They retain
  their separate result lists and extension-specific search behavior. The upstream UI changes
  are small slots in `GlobalSearchScreen.kt` and `GlobalSearchToolbar.kt`. Existing localized
  labels cover all eight required locales.

Verification: JDK 21, two clean scoped Spotless passes, and
`:app:assembleDebug :app:testDebugUnitTest` (1,036 tests, zero failures, errors or skips).
Fixtures cover the pill gate, pin selection and order, request concurrency and source failure
isolation, duplicate results, and preservation of saved details and extension metadata.

Device review: on a backed-up S22 Debug install, check Dot then Colour. With no trusted anime
extension, check the pill is absent. Install a fixture extension, pin and unpin its source in
Browse, try All, Pinned and With results, then open a title and a source catalogue. Check that
manga and novel searches still work and that Preview omits the Anime pill.

#### A3.9 Continue-watching widget (T3) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (.worktrees/a3.9, 2026-11-09)
Board: Widgets (`INDEX.md`, anime part; R9's widget code is the pattern). Glance widget: the last 1–4 anime in
progress; tap → resume in the player (or details if the source is gone). Hidden from the widget picker when
`TsuzukiAnime.enabled` is false. Tests: item selection and order.

**Result (Codex, 2026-11-09):** `484ae89bd` on `tsuzuki/a3.9`.

- `TsuzukiWatchingWidget.kt` shows the newest in-progress episode per anime, with one to four
  titles depending on widget height. It reuses the Reading widget's cover and progress styling,
  and shows remaining time only when the episode duration is known.
- `AnimePlayerModel.kt` refreshes the widget after playback progress is saved on leaving the
  player. `TsuzukiWidgetTheme.kt` refreshes it on theme changes.
- Widget taps open the existing episode resume route. If the source is removed,
  `AnimeCatalogueActivity.kt` shows cached title details and the source message.
- The provider is registered in the Debug manifest while the anime gate is Debug-only. The
  user setting card will need to move provider registration behind the runtime gate.

Verification: JDK 21, two clean per-file Spotless passes, `:app:assembleDebug :app:testDebugUnitTest`
(1,038 tests, zero failures, errors or skips), and matching keys in all eight locales. Selection
tests cover newest first, duplicate anime, watched and untouched episodes, unknown durations,
and one to four rows. The merged Debug manifest includes the provider and Preview omits it.

Device review: on the backed-up S22 Debug build, add the widget and resize it in Dot and Colour.
Watch part of an episode, leave the player, and check progress and remaining time. Tap two
distinct rows and verify each resumes its own episode. Uninstall the disposable fixture source
and check the cached detail fallback. Check the Preview widget picker separately.

#### A4.1 Anime restyle parity pass (T2) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (.worktrees/a4.1, 2026-11-09)
Follow the standard restyle rules (WORKPLAN appendix) for every screen on `png/10-anime/` (+ `dot/`, + `DotAnime.png`)
and `fixes-2026-11/AnimeShelves` / `ColourAnimeShelves`: spacing, type, case, pills, empty states; tablet layouts
(rail, two-pane details at ≥ 840 dp). Report a table: board → screen → what changed. No new features.

**Result (Codex, 2026-11-09):** `12ed776cd` on `tsuzuki/a4.1`. Scoped Spotless was clean,
`:app:assembleDebug` passed, and all 1,038 Debug unit tests passed.

| Board | Screen | Change |
|---|---|---|
| AnimeHome | Watch home | Landscape artwork, title and episode progress with remaining time when available. |
| AnimeLibrary | Watch shelf | Three columns on compact screens, responsive columns on wider screens, and an empty state for the selected shelf. |
| AnimeDetails | Anime detail | Overview and episode list side by side at 840 dp, with Favorite, Track, Save and External actions. Save uses existing episode selection. |
| BrowseAnime | Browse → Anime | Pinned and remaining source groups, with the existing Latest and pin actions. |
| PlayerSettings | Player → Settings | Icon cards for player preferences, with AniSkip and Autoplay highlighted. |
| Player | Episode player | Audio and subtitle pills reflect selected track languages, with scrolling on narrow layouts. |
| PlayerSheets | Player sheets | Bounded panel width on large displays, with responsive controls and clear selection states. |
| AnimeTracking | Tracking sheet | Tracker identity and status, with progress from the linked record. |
| AnimeShelves / ColourAnimeShelves | Manage anime shelves | Grouped rows, reorder handles, item counts, and a wider Add action. |

Watch, Library and Browse use the existing adaptive app navigation. The standalone detail route
keeps its own navigation. Board samples such as the New badge and unbacked schedule information
were omitted. Player sheets use the actions already supported by the player.

**Device review:** Compare the arm64 Debug APK in Dot and Colour on the S22 and tablet. Check the
tablet rail on Watch and Browse, and rotate to reach 840 dp for the detail split. Use a title with
artwork, multiple episodes and a linked tracker. Check Save eligibility, shelf reordering, and
selected tracks and player sheets in landscape and with larger text.

#### A4.2 Release packaging for anime (T3) — `done` (branch merged into `main`, checked 2026-11-19; earlier status was ) (.worktrees/a4.2, 2026-11-10)
Goal: anime can ship in a release build (still behind `TsuzukiAnime.enabled`). Steps: `anime-player` becomes
`implementation` with native libs only in the arm64-v8a split (other ABI splits get the no-op launcher and a "not
supported on this device" message); R8 keep rules for `is.xyz.mpv.MPVLib` JNI and the `animesource` API extensions
call; the checksum task still runs; About → licences lists the LGPL libraries with the source offer from
`docs/licenses/anime-native/README.md`. Verify `assembleRelease` (unsigned is fine) and `assemblePreview`; report each
APK's size before/after and the 16 KB check (`zipalign -c -P 16 -v 4`). Tests: launcher selection by ABI.

**Result (Codex, 2026-11-10):** `70ddd61c4` on `tsuzuki/a4.2`. Debug, Release and Preview
assembled, all 1,040 Debug unit tests passed, and scoped Spotless was clean. The first Preview
baseline ran out of disk space. Its retry from the unchanged A4.1 commit passed.

- `app/build.gradle.kts` includes the anime player in all build types. The JNI payload is in
  `lib/arm64-v8a` in the ARM64 and universal APKs. The other splits contain none of the ten
  player-specific libraries. The TLS trust bundle and audio permission come from the player module.
- `AnimePlayerModule.kt` selects mpv for a 64-bit ARM64 process and the no-op launcher otherwise.
  `AnimePlayerModuleTest.kt` checks both selections, including a 32-bit process on ARM64.
  The unsupported-device message is translated in all eight locales. This card leaves the
  Debug feature gate in place for A4.3a.
- R8 keeps the mpv JNI bridge and the anime extension API. Both optimized mappings retain their
  names, and `:anime-player:verifyNativeChecksums` ran in all three builds.
- About → Open source licences includes FFmpeg, mpv, FriBidi and libplacebo. The APK bundles the
  LGPL text and links to the pinned corresponding-source and build notes.

**APK sizes (bytes):** all ten APKs below passed `zipalign -c -P 16 -v 4`. Debug ARM64 passed too.
Logs and raw CSVs are under `.worktrees/a4.2/app/build/`.

| ABI | Release before | Release after | Preview before | Preview after |
|---|---:|---:|---:|---:|
| arm64-v8a | 37,748,915 | 69,267,749 | 37,755,031 | 69,273,861 |
| armeabi-v7a | 34,733,237 | 34,908,676 | 34,739,353 | 34,914,788 |
| x86 | 39,070,813 | 39,246,252 | 39,076,929 | 39,252,364 |
| x86_64 | 40,094,582 | 40,270,021 | 40,100,698 | 40,276,133 |
| Universal | 77,582,778 | 109,101,612 | 77,588,894 | 109,107,724 |

**Device review:** Check the licence sheet and, after A4.3a, play on ARM64 Preview to exercise
the optimized JNI and extension API. Other ABIs were verified by unit tests and APK inspection.

### Wave Y (owner approved 2026-11-06; after the anime lane)

Same general rule as wave X: check first whether it already exists; if done, set `done` with where.

#### Y1 Mihon skipped fixes, top group (T3) — `done`
`docs/UPSTREAM.md` "Skipped for conflicts": take the most valuable group (downloads, tablet FAB, range-selection
crash, WebView) that is still missing. Port each fix with the smallest diff, cite the Mihon commit hash per fix in
Result, skip any that conflicts with Tsuzuki hooks and say why. Tests where Mihon had them.

**Result (Codex, 2026-11-07):** Already on main. `7edc18c05` covers Mihon `88d4071cc` (startup cache rescan), `f28b2d887` (cancel removed-chapter queue entries), and `f82a7a68f` (fresh notification builders, with Tsuzuki's per-chapter error suppression retained). `DownloadProvider.kt` and its existing `DownloadProviderTest.kt` cover `f0f1e4de5` and `a5d584eeb`. The speed picks `730e43fd1`, `f02bf5ba7`, `9a6dea804`, and `fb8e640d6` are recorded in UPSTREAM's order 8 hand-port log. `2135d3852` already ports `c279291e2` (tablet FAB) and `a6ee4bc59` (range selection). The WebView group `649fc83ff`, `5a5ff22c3`, `5f63c983d`, `7ae858e60` is documented in the 2026-11-04 WebView entry. The remaining `ba16158b1` requires Mihon's subscriber gate and download-state refactor and removes row progress used by Tsuzuki. Kept that documented skip. No new code or build required for this audit.

#### Y2 Tablet manga two-page spread (T3) — `done` (merged into main via `review/night5` → `3fa896b3e`, 2026-11-07; see V1)
**Claude review 2026-11-07 — rework on `tsuzuki/y2` (keep your commit, add a fix commit):** the pairing logic is
good, but `PagerViewer.kt` (+99/−30) rewrites Mihon's `onPageChange` forward/preload block and long-tap handling for
every pager reader, and pulls `UiPreferences` via Injekt into the viewer. That changes behaviour for all manga
reading and makes every Mihon/Tsundoku merge of `PagerViewer` conflict. Move the spread logic behind a small
Tsuzuki helper (e.g. `tsuzuki/reader/MangaSpreadController` owning `updateSpreadLayout`, page-shape bookkeeping and
the `MangaSpread` → `ReaderPage` mapping); `PagerViewer` keeps the original `onPageChange` block untouched when the
spread is inactive and gets ≤ ~15 lines of one-line calls. Pass "themed" in from the activity instead of reading
`UiPreferences` in the viewer. Same for `PagerViewerAdapter` (keep its upstream code paths intact when inactive).
Board `png/11-tablet/TabletReader.png` / `TabletMangaSpread` (+ `dot/`). Paged manga reader on landscape windows
≥ 900 dp: optional two-page spread (setting in the manga reader settings, default off), first page single,
wide pages shown alone, RTL order follows reading direction. Hook in the viewer with a small Tsuzuki helper.
Tests: page-pairing logic (odd counts, wide pages, RTL).

**Result (Codex, 2026-11-07):** `tsuzuki/y2` @ `80173711f`. **910 tests, 0 failures.** JDK 21, per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green.
- `MangaSpread.kt` groups adjacent portrait pages in reading order, with first, wide and unpaired pages kept single and no cross-chapter pairs. `MangaSpreadHolder.kt` reuses two paged image holders and places the earlier page on the right for RTL. `MangaSpreadTest.kt` covers first page, odd tails, wide/unknown pages, chapter boundaries and RTL.
- Upstream `ReaderPreferences.kt`, `ReadingModePage.kt` and `PagerConfig.kt` wire the default-off setting. `PagerPageHolder.kt`, `PagerViewerAdapter.kt` and `PagerViewer.kt` classify loaded images, apply pairs when idle and retain the selected original page through layout changes. Long press uses the touched side.
- Upstream `ReaderActivity.kt`, `ReaderViewModel.kt` and `ReaderPageIndicator.kt` pass the original-page range to `TsuzukiMangaReaderChrome.kt`. Resume, seek and completion use real page indices. The eight locale files add the toggle label.
- Pairing starts once both dimensions are known. The mode applies to Dot and Colour on landscape paged manga at 900 dp or wider. In this mode, the old rotate/split treatment is suspended so wide scans stay intact. The board's spread is wired into the existing reader chrome.
- Claude device checks: on the tablet in both themes, confirm default off, enable in Reading mode and open a chapter with portrait pages, an odd tail and a wide scan. Check first/solo pages, RTL and LTR visual order, long-press each half, scrubbing/resume into either member, turn across chapters and completion when the last page is paired. Rotate or resize below 900 dp, return to landscape, and switch off while on a pair. On the phone verify the setting leaves single pages. Device verification pending.

**Rework Result (Codex, 2026-11-07):** Verified working diff in `.worktrees/y2` (`tsuzuki/y2`, base `80173711f`). Per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` passed (910 tests, 0 failures). Automatic approval rejected the card-branch commit because AGENTS.md says Codex never commits. Claude can review and commit the working diff.
- `MangaSpreadController.kt` owns layout, image shape tracking, pair mapping, touch-side lookup and holder dispatch.
- Upstream `ReaderActivity.kt` supplies the app theme gate. Upstream `PagerViewer.kt` retains its ordinary page-change block and delegates spread cases. Upstream `PagerViewerAdapter.kt` retains the ordinary grouping and lookup paths when the mode is off.
- Device review: repeat the checks above, including landscape to portrait while on a spread and stock long tap, splitting and chapter preloading with the setting off.


#### Y3 Manga details + Browse manga boards (T2/T3) — `done` (Claude reviewed, merged into main 2026-11-07 via `review/night4` → `c7c805cf2`) (needs Q6)
Boards `png/09-manga/MangaDetails.png`, `BrowseManga.png` (+ `dot/`): scanlator filter entry, auto-webtoon hint,
manga-specific tiles; compare with what N5 built and fix only differences (list them).

**Result (Codex, 2026-11-07):** `tsuzuki/y3` @ `8dc76777d`. **909 tests, 0 failures.** JDK 21, per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green.
- `TsuzukiMangaDetails.kt` adds the effective Webtoon hint and a tappable reading-mode tile that opens the existing themed grid. It uses the per-series flag or global default, without making an image-detection claim. The scanlator filter entry, count, and multi-select exclusions from N5 already match the requested behavior.
- Upstream details hooks in `presentation/manga/MangaScreen.kt`, `ui/manga/MangaScreen.kt`, and `MangaViewModel.kt` pass the choice to the existing `SetMangaViewerFlags` interactor. Novel and stock details still use their existing routes.
- `TsuzukiBrowseManga.kt` places Local manga alongside pinned sources without changing pin state and adds a manga repo tile. `TsuzukiBrowse.kt` renders the real local formats, while upstream `SourcesScreen.kt` exposes language under manga source names. `SourcesTab.kt` opens the existing repo manager and `BrowseTab.kt` switches to the existing Manga Extensions tab with its real update count.
- `MangaSourceTilesTest.kt` covers local promotion, ordering and an absent local source. Eight locale files add the labels and help copy. The board's fabricated genres and source totals are omitted, and the separate anime selector remains in the anime lane. The repo button opens the manager where the existing Add action lives.
- Claude device checks: in Dot and Colour on phone and tablet, use an entry with several scanlators, open/apply/cancel the exclusions, and switch the entry between Webtoon, RTL and Default from the tile. Open a chapter to confirm the saved mode. In Browse Manga, verify Local opens the local catalogue, real pins remain removable, remote rows show language and supported Latest actions, and both repo and extension shortcuts open the intended screens. Repeat Browse Novels to check its layout. Device verification pending.

#### Y4 Synthetic 500-book backup generator (T2) — `done` (Claude reviewed, merged into main 2026-11-07 via `review/night4` → `c7c805cf2`)
WORKPLAN P2.2: a script in `scripts/` (Kotlin script or Python, no app code) that writes a valid Tsuzuki
`.tachibk` with 500 books (novels + manga mix), 30 shelves, 50 k chapters, history, using the backup proto in
the repo. Fake titles clearly marked `[synthetic]`; no real source data. Claude restores it only into a scratch
install. Test: the script output parses with the app's backup decoder (unit test that decodes a small generated file).

**Result (Codex, 2026-11-07):** `tsuzuki/y4` @ `6b25f922b`. **907 tests, 0 failures.** JDK 21, per-file Spotless hooks twice and `:app:assembleDebug :app:testDebugUnitTest` green.
- `scripts/generate_synthetic_backup.py` writes the existing gzip/protobuf format with the Python standard library. Titles and stub names are marked `[synthetic]`, all identifiers are fabricated, and output requires a new file. Counts and the reference timestamp can be overridden for reproducible smaller fixtures.
- `synthetic-library-small.tachibk` is the deterministic fixture with four books, two shelves and 20 chapters. `SyntheticBackupTest.kt` decodes it through the app's `BackupDecoder` and checks media mix, source and shelf links, original chapter numbering, resume progress and history. No production app files changed.
- The default run wrote `.worktrees/y4/build/synthetic-library.tachibk` (generated, ignored). Independent wire counts confirmed 250 novels, 250 manga, 30 shelves, 50,000 chapters, 995 history records and two stub sources. Recreate from the card checkout with `python scripts/generate_synthetic_backup.py build/synthetic-library.tachibk` using a new destination if the file already exists.
- Claude scratch check: restore into an empty scratch preview profile, select both manga and novels plus history, confirm totals, shelf names, History and Continue entries, and exercise the Home and Library grids. The generator omits covers and real source content, so it measures the restored library data and layout. No restore or device action performed.

#### Y5 Restyle checks: Tracking, Quotes, ExportEpub, Upcoming, EditNovel (T2) — `done` (Claude reviewed, merged into main 2026-11-07 via `review/night4` → `c7c805cf2`) (needs Q6)
Boards in `docs/design/boards/INDEX.md` (05-novel, 08-tools). For each: Tsuzuki version exists? compare to board,
fix differences; stock → restyle with a dispatch hook. List per screen in Result.

**Result (Codex, 2026-11-07):** `tsuzuki/y5` @ `0a13db39f`. **908 tests, 0 failures.** JDK 21, per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green.
- Tracking: `TsuzukiTrackInfoDialogHome.kt` adds the chapter auto-sync hint when a tracker is linked and the real auto-update preference is enabled. Existing cards, metric edits, add/search, private and date controls already cover the board's backed actions.
- Quotes: `QuotesSheet.kt` uses themed heading typography and localized action descriptions. New `TsuzukiQuotesExport.kt` adds the actual count and saves the current novel's quotes to a UTF-8 text file through the document picker. `QuoteTileMetadataTest.kt` checks order, Unicode and chapter attribution. The existing quote tile covers per-quote share/delete. No cross-novel selector was added because this sheet is backed by the current novel.
- Export EPUB: upstream `BatchExportEpubDialog.kt` dispatches Dot/Colour to the form shared with single-book export in `TsuzukiExportEpubDialog.kt`. It shows the real selected count and titles and keeps existing options/defaults and the EPUB/ZIP output contract. `EpubExportContainerTest.kt` checks the filename follows the actual container. Multi-novel output stays one ZIP and the form omits unsupported separate-file and compression choices.
- Upcoming: `TsuzukiUpcomingScreen.kt` already has the board's month controls, actual marked dates and selected-day cover list. Verified by comparison. No predicted chapter number was invented.
- Edit Novel: `TsuzukiEditMangaDialog.kt` already has the cover picker/reset, themed fields, status pills and Save. Verified by comparison. The board's manual book palette has no saved per-book setting in this editor. Eight locale files add the three new labels.
- Claude device checks: in Dot and Colour, open Tracking with auto-sync on/off and exercise linked/unlinked trackers, then export several disposable quotes and inspect UTF-8 text and chapter order. Open one- and multi-selection EPUB forms and verify cancel, picker MIME and filename for original, Both and split volumes. Check Upcoming month/day/filter interactions and Edit Info cover, status and Save on a disposable book. Device verification pending.

#### Y6 Tablet TTS panel + foldable (T3) — `done` (merged into main via `review/night5` → `3fa896b3e`, 2026-11-07; see V1)
**Claude review 2026-11-07 — rework on `tsuzuki/y6`:** X1 just moved Tsuzuki code out of `ReaderActivity`; Y6 adds
~50 lines of listen-panel composition back into `ReaderActivity.setContent` (fold detection, listening state,
the whole `TsuzukiReaderListenPanel` lambda). Move that into one Tsuzuki composable / hook in
`presentation/tsuzuki/reader/` (e.g. `rememberTsuzukiListenPanel(state, viewer, readerPreferences, actions)` returning
`fold`, `listenContent`, `ttsSessionActive`, `ttsPaused`), with the activity passing the existing TTS functions as
lambdas. `ReaderActivity` should end up with a few one-line calls, as after X1. `TtsController.kt` (+40) is fine if
it only exposes state; keep it read-only.
Boards `png/11-tablet/TabletTTS.png`, `Foldable.png` (+ `dot/`). Tablet novel reader with TTS active: the N25
side panel shows the listen controls (play/pause, speed, voice, sleep timer, paragraph list) using existing
`TtsController` state; foldable half-open posture keeps text on top, controls on the bottom half. Tests: panel
state mapping.

**Result (Codex, 2026-11-07):** `tsuzuki/y6` @ `b0bc2463e`. **910 tests, 0 failures.** JDK 21, per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` green.
- `ReaderListenLayout.kt` reads Jetpack WindowManager posture and maps the existing controller's paragraph position to panel progress. `TsuzukiReaderPanel.kt` and `TsuzukiReaderHooks.kt` show Listen in the N25 dock, preserve Contents/Quotes/Notes, reserve reader space and move the panel below a horizontal half-open hinge. The measured hinge gap stays clear. Flat and vertical folds retain the normal reader layout.
- `TsuzukiReaderListenPanel.kt` shows the real cover, playing chapter when available, current voice, speed, paragraph progress, play/pause and paragraph transport. It lists the controller's actual spoken paragraphs for direct seek. Voice/speed selection reuses reader preferences and reapplies settings at the active paragraph when speaking.
- `TtsSleepTimer.kt` defines Off, end of chapter, and 15/30/60-minute choices. Upstream `TtsController.kt` owns the one-shot timer and short paragraph previews. A timed expiry stops playback and the chapter-end choice suppresses auto-advance. Timer state is scoped to the existing playback controller and clears on stop or destruction.
- Upstream `NovelViewer.kt` and `NovelWebViewViewer.kt` expose their existing controller internally. `ReaderActivity.kt` and `NovelReaderAppBars.kt` pass the active controls through the reader panel and reserve its measured space for the reader chrome. `app/build.gradle.kts` and `gradle/libs.versions.toml` declare AndroidX WindowManager 1.5.0. Eight locale files add timer and paragraph labels.
- `TsuzukiReaderPanelTest.kt` covers fold selection, hinge gap and clamped paragraph progress. `TtsSleepTimerTest.kt` tests replacing a timer and stopping once at expiry. No sample voice, offline badge or fake chapter data was added. The card's horizontal half-open instruction takes precedence over the board's vertical hinge illustration.
- Claude device checks: in Dot and Colour, start TTS on the tablet in each novel renderer, verify text and dock coexist, then pause/resume, change speed/voice and seek to a visible paragraph. Verify the close action keeps playback running and Listen reopens the panel. Check Contents/Quotes/Notes, status and menu bars with the dock open. Try timer replacement and end-of-chapter with auto-advance enabled. On a half-open foldable or emulator, verify text stays above the physical hinge, controls below, and flattening restores the ordinary layout without losing playback. Device verification pending.

**Rework Result (Codex, 2026-11-07):** Verified working diff in `.worktrees/y6` (`tsuzuki/y6`, base `b0bc2463e`). Per-file Spotless twice and `:app:assembleDebug :app:testDebugUnitTest` passed (910 tests, 0 failures). Card-branch commits require owner or Claude after automatic approval rejected the Y2 commit under AGENTS.md.
- `ReaderListenLayout.kt` owns renderer selection, fold detection, dock eligibility, listening state and panel composition. Upstream `ReaderActivity.kt` binds the hook and passes the existing pause, resume and paragraph actions. The rework keeps the layout and controller behavior from the original card.
- Device review: repeat the checks above, including phone mini-controls, both novel renderers and a horizontal half-open hinge.


#### Q13 Novel next chapter: instant, never a blank page (T3) — `done` (Claude `54717865f`: warm-up past 75 %, Next ~0.1 s instead of ~1.2 s blank on the S22; also fixed the stale 100 % on chapter entry)
Owner 2026-11-07: "sometimes the novel reader lags before going to the next chapter." Measured on the S22
(preview, Royal Road, TextView, infinite scroll off): tapping › Next shows a **blank black page for ~1.2 s**
(`app/build/night3-evidence/next.mp4`, `next-strip.png`) while the chapter text is fetched from the network.
`NovelViewer` already prefetches the next chapter, but only for infinite scroll and TTS
(`preFetchNextChapterForTts`, `prepareNextChapterForInfiniteScroll`); the button path loads on demand.
- Prefetch the next chapter's text (network or disk) when the reader passes ~75 % of the current chapter or the
  chapter-end card appears, for TextView and WebView, infinite scroll off as well as on. Reuse the existing
  prefetch helpers; one in-flight prefetch at a time; cancel on chapter change; never download (no files).
- Next uses the prefetched text when present (no network wait). If it isn't ready, keep the current page visible
  with a small themed loading state (Tsuzuki progress dots in the bottom bar / end card) instead of a blank page,
  and show a retry message on failure (no silent swallow).
- Respect data saver / Wi-Fi-only download prefs if they exist for reading; no prefetch while TTS already does it.
- Tests: prefetch trigger threshold + cancellation logic as pure helpers. Device check (Claude): Next is instant
  after scrolling past 75 %, loading dots when tapping early, airplane mode → retry message.
Hooks in `NovelViewer.kt` / `NovelWebViewViewer.kt` stay small (call into `tsuzuki/reader/`).
Result: _pending_

#### Q14 Design fixes from Claude's 2026-11-07 device pass (T2) — `done` (Claude `aa9d5544a`: items 1–5 fixed and device-checked; item 6 tablet margins deferred → owner question)
Boards: `fixes-2026-11/` (ReaderQuick, ReaderAll, Q9Selection) and `INDEX.md` SettingsTranslation. Evidence:
`app/build/night3-evidence/` (`reader-quick.png`, `reader-all.png`, `q9-selection.png`, `x3-translation.png`).
Fix each, Dot then Colour, at 360 dp and font scale 1.3:
1. Quick reader sheet: the **LITERATA** font chip text touches both chip edges (chip too narrow) — size chips to
   content or shrink the label; no clipping.
2. All reader settings: the five tabs wrap (**MORE** drops to a second line) — one row (equal-width segment or
   horizontally fitted pills per board `ReaderAll`); title "ALL READER SETTINGS" wraps to two lines — one line
   with ellipsis or a shorter title ("READER SETTINGS").
3. All reader settings › Fine-tune: **paragraph spacing shows `5`** (it is 0.5 em ×10) and indent `0` without a unit
   — show `0.5 em` / `0 em` with the shared unit formatter (Q11's `1.6×` style).
4. Library selection (board `Q9Selection`): selected covers get the **accent outline + check badge**, unselected
   covers dim to ~55 % (now: dark red tinted card, no dimming); action labels white (now grey); `MARK AS READ`
   wraps — use the short label `MARK READ` (8 locales) and one line.
5. Settings › Translation (Dot): slider rows ("Translation chunk size", "Delay between requests", "Request
   timeout", "Max parallel translations") are not uppercased like the other rows; the GENERAL tile's bottom
   corners are square (the slider row breaks the grouped-tile rounding).
6. Tablet novel reader: left text margin is only ~6 dp (looks cramped at the edge) — check the default margins for
   wide windows against board `TabletNovel` and use the board's side margin when the user hasn't changed it.
Result: _pending_

### Overnight batch N (2026-11-05 → 06:00 2026-11-06) — run in ONE Codex thread, in order

**How to run the batch (overrides the usual rules where they differ):**
- Do the cards **in order N1 → N32** (wave 1 N1–N8, wave 2 N9–N20, wave 3 N21–N32), one at a time. Before each card re-read this file; skip any card not `ready`.
- Worktree per card from the current `main`:
  `git -c core.longpaths=true worktree add -b tsuzuki/<id> .worktrees/<id> main` (lower-case id), copy
  `local.properties` from the repo root. If a card touches files an **earlier card of this batch** already changed,
  base it on that card's branch instead (`... .worktrees/<id> tsuzuki/<earlier-id>`) and say so in Result.
- **Overnight exception: commit your finished work on the card's own branch** (`git add -A app i18n` + a clear
  message; owner is the author, no co-author trailers). Never commit to `main`, never merge, never push.
- Follow `AGENTS.md` Rules and `docs/WORKPLAN.md` → "Appendix — standard rules for restyle cards" (early-return
  dispatch in upstream files, tokens/typography only, Dot uppercase + red, real data only, 8 locales, tests,
  per-file Spotless twice, `./gradlew :app:assembleDebug :app:testDebugUnitTest` green). No adb/devices.
- Card status: `codex-working` when you start, then `review` with **Result** filled (branch + commit hash, files,
  what you left out and why, test counts, what Claude must check on a device). If stuck > ~45 min or a build
  can't be made green, set `blocked` with the reason, `git checkout` the card's worktree clean, and move on.
- At most one Gradle build at a time. Don't touch devices, `main`, release/signing/telemetry files.

#### N1 Mihon fixes, group A (T3) — `done` (merged overnight)
Port from remote `mihon` (already fetched; `git show <sha>`): 1e471a414 (updates show finished chapter as still
downloading), 287cbbb98 (keep last update time on empty Updates), 8457fb91e (chapter refresh with non-ASCII
digits), c9e1b6e96 (invalid extension filters instead of crashing), 4c88f0264 (SearchViewModel race),
d3f10772f (local source in global search), e9234f205 (settings search matches group headings). Also check
07b9fb695, ae749bb4f, cdc7d69c9 (installer) — probably already in via `2c7bcae9a`; mark "already applied" if so.
Per commit: already applied / `git cherry-pick -x -n` / hand-port preserving Tsundoku novel paths and Tsuzuki
hooks / skip with reason. Add tests where the logic is pure. Log each in `docs/UPSTREAM.md` (new dated section).
Result: `tsuzuki/n1` at `96e8b11b0` (base `main`). Per-file Spotless twice, debug assembly and all 730 app unit tests passed. Logs: `.worktrees/n1/app/build/n1-verification.log` and `.log.build`.

- `UpdatesViewModel.kt` ignores stale events for downloads that left the queue. `TsuzukiUpdatesTwoPane.kt` keeps the last update timestamp when the tablet list is empty. The phone already does this.
- `SourceFilterDialog.kt` treats unknown tri-state values as disabled. The stock and Tsuzuki select widgets already handle invalid indices safely.
- `SearchViewModel.kt` atomically updates source results and includes enabled local manga and novel sources regardless of language filters.
- `SettingsSearchScreen.kt` matches group headings. `PreferenceScreen.kt` scrolls to and highlights the group in both layouts.
- `Installer.kt` synchronizes queue access and cancels only the matching entry. `ExtensionLoader.kt` stages replacements so copy or rename failure preserves the installed APK.
- `docs/UPSTREAM.md` records all ten dispositions. Skipped the chapter formatter fix because Tsundoku parses the numeric regex match directly. The three installer patches still needed ports after the earlier installer sync.

Device review: Finish a chapter download from Updates, clear Updates on tablet, search a local manga and novel with the Other language disabled, open an invalid source filter, and jump to a setting group from search. In an approved extension test, cancel one of two installs and verify a failed replacement leaves the original installed.

#### N2 Mihon fixes, group B (T3) — `done` (merged overnight)
Same method for: b5856135c (webtoon scroll stutter), a42428284 (covers reloading when removing from library),
0864ce0f4 (redact sensitive headers in logcat), 5910a51eb (keep uninstalled source names across restores),
0c1be369b (ask to add to library before editing cover), 40f552981 (blank title duplicates), c0c090999 (Kitsu
novels in search), ee5939454 (tracker start date UTC shift), dd4e9f00f (confirm unfavourite on manga screen —
must also work with the Tsuzuki details screen; keep the dialog themed via existing Tsuzuki dialog colours).
Result: `tsuzuki/n2` at `ae1c4a430`, based on `tsuzuki/n1` for the shared upstream log. Per-file Spotless twice, debug assembly and all 731 app unit tests passed. SQLite checks passed for blank, whitespace-only, exact and alternate title cases. Validated both redaction strings in all eight locales. Logs: `.worktrees/n2/app/build/overnight-verification.log` and `.build`.

- `MangaCoverFetcher.kt` and `MangaCoverKeyer.kt` use custom covers only for favourites. `MangaExtensions.kt`, `MangaViewModel.kt` and `BrowseSourceViewModel.kt` remove cached covers without bumping the cover key.
- `NetworkHelper.kt` and `NetworkPreferences.kt` redact Auth and Authorization by default. `SettingsAdvancedScreen.kt` adds the switch with two strings across eight `strings.xml` files.
- `BackupFileValidator.kt`, `BackupSource.kt` and `BackupRestorer.kt` preserve meaningful missing-source names and novel classification. `BackupSourceNameTest.kt` covers names and legacy placeholders.
- `MangaScreenConstants.kt`, `MangaCoverDialog.kt`, `MangaCoverViewModel.kt` and `MangaScreen.kt` reuse the existing add-to-library warning for cover editing.
- `mangas.sq` guards primary-title matching for blanks while preserving alternate title matches. `AddTracks.kt` keeps the history instant for the tracker start date.
- `MangaScreen.kt` and `MangaViewModel.kt` confirm removal through the shared details callback. `BrowseSourceDialogs.kt` applies Tsuzuki dialog colours.
- `docs/UPSTREAM.md` records all nine dispositions. Kitsu already filters out novels. Skipped Mihon's webtoon strip renderer because Tsundoku uses a different native decoder API with crop and display-profile hooks.

Device review: Check remove/cancel and cover editing on stock and Tsuzuki details, confirm ordinary covers stay stable when removing a favourite, round-trip a backup with an uninstalled novel source, check first-read tracker dates across a date boundary, and verify redaction with synthetic headers.

#### N3 Novel reader immersive + paged chrome (T3) — `done` (merged overnight)
Boards `06-reader/ReaderImmersive`, `06-reader/ReaderPaged` (PNG both themes + HTML). Restyle the existing
immersive (bars hidden, text selection toolbar if a composable exists) and WebView paged-mode chrome (page
indicator / page footer / any paged-mode controls) in `presentation/reader/**` via dispatches; new code in
`presentation/tsuzuki/novel/`. Don't touch viewer internals (`ui/reader/viewer/text/**`). Real data only.
Result: `tsuzuki/n3` at `6446abef0`, from current `main` (`159eafd73`). Per-file Spotless twice, debug assembly and all 731 app unit tests passed. Logs: `.worktrees/n3/app/build/overnight-verification.log` and `.build`.

- `NovelStatusBar.kt` dispatches to the themed wrapper while reusing the existing configurable clock, battery, chapter, progress, and collapse row. The Dot page label uses display typography and the themed toggle has a larger touch target.
- `TsuzukiNovelStatusBar.kt` draws real scroll or paged progress using the board's 26 dp insets and 3/4 dp track. Paged Colour uses the reader foreground for contrast against its configurable paper colour.
- `TsuzukiComponents.kt` lets this track spread dots across its width. `NovelPageNumberTest.kt` verifies the page bounds and single-page chapters.
- Left out the estimated minutes, menu hint, and separate running title header. Status-bar content and placement follow existing user preferences. The existing menu already provides the seek control and book title. Text selection is Android action mode in both renderers, outside this card's viewer boundary. No new strings.

Device review: In Dot and Colour open a novel, enable the status bar, then hide the menu. Check the footer in both continuous and WebView paged mode, rotate, change pages and chapters, move the status bar to the top, collapse and expand it, and check text selection still works. Smoke-test the stock status bar.

#### N4 Manga reader chrome (T3) — `done` (merged overnight)
Boards `09-manga/MangaReader`, `09-manga/MangaPaged`, `09-manga/MangaReaderSettings`. The manga reader top bar
already uses `TsuzukiReaderTitle` and the Tsuzuki slider; restyle the rest of the manga chrome in
`presentation/reader/appbars/{ReaderAppBars,ReaderBottomBar,ReaderTopBar}.kt`, page indicator, and the manga
reader settings dialog pages (reading mode / general / colour filter) via dispatches, new code in
`presentation/tsuzuki/manga/`. Keep every action. If N3 touched the same files, base on `tsuzuki/n3`.
Result: `tsuzuki/n4` at `26f678af0`, based on current `main` (`56fc27d2f`). N3 changed separate files. Per-file Spotless twice, debug assembly and all 731 app unit tests passed. Logs: `.worktrees/n4/app/build/overnight-verification.log` and `.build`.

- `ReaderAppBars.kt`, `ReaderTopBar.kt`, `ReaderBottomBar.kt` and `TsuzukiMangaReaderChrome.kt` render the themed header and combine the seek and action rows in a floating footer. Header actions, chapter navigation, mode, rotation, crop, and settings retain their callbacks.
- `ChapterNavigator.kt` styles the horizontal navigator in the footer and retains RTL seeking. `ReaderPageIndicator.kt` uses the themed counter when menus are hidden.
- `ReaderSettingsDialog.kt` supplies themed controls to the three manga settings pages. `ReadingModePage.kt` shows all six series modes as tiles. `GeneralSettingsPage.kt` and `ColorFilterPage.kt` share the controls in `TsuzukiMangaReaderSettings.kt`, preserving toggles, choices, sliders, and live preview.
- Reused all labels. Left the manga mode shortcuts in the existing mode dialog and tap-zone controls in settings. Page-level sharing remains in the existing page action menu. No series-type or scanlator label was added to the reader header.

Device review: On phone and tablet in Dot and Colour, open RTL, LTR and Webtoon readers. Check seeking, chapter changes, all header/footer actions, the hidden-menu indicator, switching settings tabs and live filter preview. Check the stock reader and novel settings for regressions.

#### N5 Manga details (T2) — `done` (merged overnight)
Board `09-manga/MangaDetails` (scanlators, auto webtoon). Details already has Tsuzuki header/tiles/resume; add
the board's manga-specific parts that have real data (scanlator filter entry → existing excluded-scanlators
dialog, reading-mode/webtoon indicator from the book's viewer flags) and restyle the scanlator dialog. Hooks in
`presentation/manga/**` only as dispatches.
Result: `tsuzuki/n5` at `bebf5b200`, based on `tsuzuki/n2` for the shared details screen. Per-file Spotless twice, debug assembly and all 731 app unit tests passed. Logs: `.worktrees/n5/app/build/overnight-verification.log` and `.build`.

- `presentation/manga/MangaScreen.kt` adds the mode tile and a direct scanlator entry to phone and tablet details without changing chapter list indices. `ui/manga/MangaScreen.kt` passes a callback to the existing dialog state.
- `ScanlatorFilterDialog.kt` dispatches Tsuzuki themes to the adaptive sheet. `TsuzukiMangaDetails.kt` renders the mode, exclusion count, and scanlator rows with the existing select all, reset, cancel and apply actions.
- The mode tile uses series flags and the configured reader default. It omits the board's art-detection claim and remains read-only. The chapter header uses a single filter entry to open the multi-select dialog. Reused existing localized strings.

Device review: On phone and tablet in Dot and Colour, open manga details with several scanlators. Check applying and clearing exclusions, cancel, the count, and the change in the chapter list. Verify Webtoon, an explicit paged mode and Default, then check stock and novel details.

#### N6 Vertical reader progress (T2) — `done` (merged overnight)
"Vertical progress presentation remains stock" (`docs/RUNTIME_QA.md`). The vertical chapter navigator
(`presentation/reader/components/ChapterNavigator.kt`, vertical variant) should use the Tsuzuki slider look
(Dot dots / Colour thin track, 48 dp touch area, same semantics) like the horizontal one already does
(`presentation/tsuzuki/reader/TsuzukiReaderSlider.kt`). If N3/N4 touched these files, base on that branch.
Result: `tsuzuki/n6` at `c26770738`, based on `tsuzuki/n4` for the shared chapter navigator. Per-file Spotless twice, debug assembly and all 731 app unit tests passed. Logs: `.worktrees/n6/app/build/overnight-verification.log` and `.build`.

- `ChapterNavigator.kt` connects the existing Material vertical slider to the themed track and reserves a 48 dp touch width. Chapter buttons, labels, seeking callbacks and accessibility stay on the existing path.
- `TsuzukiReaderSlider.kt` measures and rotates the existing Dot/Colour progress artwork for top-to-bottom movement independent of left/right placement. Stock themes still use the Material track and thumb. No new strings.

Device review: Enable the vertical navigator on the left and right in Dot and Colour. Tap and drag near both ends, check label and track alignment, use an accessibility increment, and verify horizontal/stock sliders still work.

#### N7 Settings screens vs boards (T2) — `done` (merged overnight)
Boards `07-system/SettingsReader`, `08-tools/ReaderBars`, `08-tools/ReaderEngine`, `08-tools/SettingsTranslation`.
These are `PreferenceScreen`-based and already get Tsuzuki tiles. Compare each board with the current screen
(`SettingsReaderScreen.kt`, `SettingsNovelReaderScreen.kt`, `StatusBarElementsScreen.kt`,
`SettingsTranslationScreen.kt`) and fix the differences that are presentation-only (grouping/section labels,
value labels in accent, segmented choices as pills via existing preference widgets/Tsuzuki widgets). List the
rest as left out. Note: `SettingsNovelReaderScreen.kt` has an old upstream lint error — don't "fix" unrelated code.
Result: `tsuzuki/n7` at `903b72b27`, based on current `main` (`e5ed33e07`). Per-file Spotless twice, debug assembly and all 732 app unit tests passed. Logs: `.worktrees/n7/app/build/overnight-verification.log` and `.build`.

- `SettingsReaderScreen.kt` groups the leading reader controls in a Reading mode tile. Existing list rows continue to show their selected values in accent.
- `Preference.kt`, `PreferenceItem.kt` and `TsuzukiSettingsPane.kt` support wrapping and segmented choices. The three novel reader choices are mapped by preference key in `TsuzukiReaderStatusBarSettings.kt` so `SettingsNovelReaderScreen.kt` and its pre-existing lint issue stay outside this diff.
- `StatusBarElementsScreen.kt` reuses its ordering and persistence with the themed header, grouped rows and switches in `TsuzukiReaderStatusBarSettings.kt`.
- `SettingsTranslationScreen.kt` uses wrapping pills for the real installed engine options when translation is enabled. Existing config and test actions stay in their own groups. Reused localized resources.
- Left out the status bar preview with sample chapter data, toolbar button placement controls, new reader thresholds and estimates, combined novel font/size, and inline translation credentials. Existing controls and preferences keep their behavior.

Device review: In Dot and Colour check reader settings, novel renderer and bar position/size, drag/reorder/reset of status-bar items, and translation engine selection without invoking a paid test. Check a legacy theme for stock controls.

#### N8 Time format setting (T2) — `done` (merged overnight)
Port Mihon #4118 (time format setting next to the existing Date format in Appearance; find the commit with
`git log mihon/main --grep "time format" -i`). Apply it where upstream applies it (relative times / timestamps),
keep Tsuzuki value labels (`tsuzukiValueLabel`) working, strings in 8 locales.
Result: `tsuzuki/n8` at `b22c05a3a`, based on `tsuzuki/n2` for the eight shared locale files. Hand-port of Mihon `970b12f1b` (#4118). Per-file Spotless twice, debug assembly and all 734 app unit tests passed. Logs: `.worktrees/n8/app/build/overnight-verification.log` and `.build`.

- `UiPreferences.kt` and `TimeFormat.kt` add Follow language (default), Follow system, 24-hour and 12-hour with locale-aware formatting.
- `SettingsAppearanceScreen.kt` adds Time format beside Date format with live examples and the existing accent value label. The five new keys are present in all eight requested `strings.xml` files.
- `TwentyFourHourSystem.kt` observes the system format on resume. `HistoryScreen.kt`, `HistoryItem.kt` and `TsuzukiHistoryItem.kt` share the selected formatter. `DateText.kt` and `TrackInfoDialog.kt` use the app locale for dates.
- `AboutScreen.kt`, `DebugInfoScreen.kt`, `WorkerInfoScreen.kt` and `DateExtensions.kt` apply the setting to absolute timestamps. The Tsuzuki About card has no visible build-time field. The novel status-bar clock is outside the upstream patch.
- `TimeFormatTest.kt` adds three tests for mode selection, locale defaults, and the combined timestamp.

Device review: In Appearance switch between all four formats in both Tsuzuki themes and a stock theme. Check History and Debug info, then change Android's 12/24-hour setting while the app is backgrounded and verify Follow system refreshes after resume.

#### Wave 2 — N9 to N20 (same batch rules; continue here after N8, in order)

#### N9 Tablet Browse two-pane (T3) — `done` (merged overnight)
Board `11-tablet/TabletBrowse`. On tablet UI (`isTabletUi()`, as D1/TsuzukiUpdatesTwoPane uses) in Tsuzuki themes,
Browse shows the sources list left and the selected source's catalogue right. Prefer hosting the existing catalogue
content composable in the pane over re-implementing it; if the Voyager screen model makes that unsafe, build a
read-only preview pane (popular list via the source's existing paging) with an "Open catalogue" action, and say so.
Phones unchanged. Don't touch `.worktrees/anime-a12` files.
Result: `tsuzuki/n9` at `cd94f45f8`, based on `tsuzuki/n8` for shared locale files. Per-file Spotless twice, debug assembly and all 734 app unit tests passed. Logs: `.worktrees/n9/app/build/overnight-verification.log` and `.build`.

- `SourcesScreen.kt` dispatches to the split pane only in Tsuzuki tablet UI for both media tabs. `TsuzukiBrowse.kt` highlights the selected pinned source. The same source list retains its pin, filter and latest actions.
- `TsuzukiTabletBrowse.kt` shows the selected source's real titles, covers, and language, with loading, retry, and empty states. The read-only preview uses page one of the existing popular catalogue API on IO and avoids the full pager's shared page tracker and navigation state. **Open catalogue** opens the full list with its existing controls and paging. Phones and stock themes keep their current routes.
- Two new labels in all eight `strings.xml` files. Left out sample counts, source health and filters in the preview. The first page loads when the user selects a source.

Device review: In portrait and landscape tablet Dot and Colour, select a novel source, switch sources while loading, use retry, and open the full catalogue. Repeat in manga Browse, test pin and latest actions, and verify a phone and a legacy theme still open the catalogue directly.

#### N10 Download queue manga rows in Compose (T3) — `done` (merged overnight)
`docs/design/boards` `07-system/Downloads`. The manga queue is View-based (`ui/download/DownloadAdapter`,
`DownloadHolder`, `DownloadHeaderHolder`, styled by `tsuzuki/TsuzukiViewStyle.kt`). In Tsuzuki themes render it in
Compose (source group header + rows with cover, title, chapter, progress via `TsuzukiProgressBar`, menu actions
move to top/bottom/cancel as stock, drag reorder if feasible with a maintained library already in the project —
else keep stock reorder via menu and say so). Stock themes keep the RecyclerView.
Result: `tsuzuki/n10` at `a63179f8b`, based on current `main` (`a567c86e5`). Per-file Spotless twice, debug assembly and all 737 app unit tests passed. Logs: `.worktrees/n10/app/build/overnight-verification.log` and `.build`.

- `DownloadQueueScreen.kt` dispatches the Manga tab to Compose in Tsuzuki themes. Legacy themes keep the RecyclerView. `TsuzukiMangaDownloadQueue.kt` renders source headers, real covers/titles, chapter rows, live progress/status, and the chapter and series menus. Error details can still be copied.
- `DownloadQueueViewModel.kt` loads covers on IO and sorts the existing groups without relying on the adapter. Reorder merges with the current queue so other media, new entries, and completed chapters are handled safely. `ReorderDownloadSubsetTest.kt` covers mixed queues and stale/duplicate drag entries.
- Drag reorder uses the existing Reorderable library within each source. Use the series menu or the global sort for larger moves. The Active and Errors views keep cancel actions and disable reorder. No new strings or sample storage/speed numbers.

Device review: Queue manga from multiple sources alongside novels. Check Dot/Colour progress, covers, drag within a source, chapter and series moves, toolbar sort, filters, error copy, and cancellation. Verify the stock RecyclerView and novel/translation tabs.

#### N11 Generated covers for books without art (T3) — `done` (merged overnight)
Board `14-components/Cover`. When a book has no cover URL / cover fails to load, draw a generated cover (title in
the display face, deterministic colours from the title hash; Dot = black/grey with one red mark; Colour = palette
like the board) instead of the empty placeholder, in `MangaCover` via a dispatch. Unit-test the deterministic
colour/seed logic.
Result: `tsuzuki/n11` at `a699ba338`, based on `tsuzuki/n9` for the tablet browse cover and its N1 ancestor for the updates pane. The shared `MangaCover` dispatch draws deterministic display-face covers for missing or failed art, with six motifs, the board’s ten Colour palettes and one red marker on greyscale Dot art. Passed real title metadata through browse, library, home, history and updates. URL-only callers without a supplied title use the existing placeholder, and no author is fabricated. Two deterministic tests cover palette/motif and the minimum signed hash. Both Spotless passes on 19 files, debug assemble and all 736 app unit tests passed. Device review: verify title sizing and clipping in grids, missing and broken URLs, and custom covers on books with no remote URL in Dot and Colour.

#### N12 Per-shelf book counts in Categories (T2) — `done` (merged overnight)
Board `07-system/Categories` shows a count per shelf. Add a read-only count (books per category, respecting the
category content type) to the Categories screen state and show it in `TsuzukiCategoryListItem`. Real data only.
Result: `tsuzuki/n12` at `47c2be4f2`, based on main `5f9c354df`. Categories now observes one grouped SQL query for live shelf counts. It counts saved library entries that match each shelf’s manga/novel/all type, including zero for empty shelves. The Tsuzuki row adds a localized numeric count alongside existing content type and default indicators. The automatic system shelf remains outside the editable shelf list. The in-memory SQLite fixture checked mixed and incompatible memberships, empty shelves and removal from the library. Both per-file Spotless passes, debug assemble and all 737 app unit tests passed. Device review: check count placement in both themes and updates after moving a book between typed shelves.

#### N13 Source counts per extension repository (T2) — `done` (Claude agent `13cfa2edc`, merged `57431ab55`)
Board `04-browse/ExtensionRepos` shows how many sources each repo provides. Compute from the available-extension
lists already loaded (manga + novel) grouped by repo, show in `TsuzukiExtensionStoreItem`. Hide when unknown.
Result: _pending_

#### N14 Global search result count + time (T2) — `done` (Claude agent `ec7f81ad0`, merged `6b7fa0b3f`)
Board `04-browse/GlobalSearch`: "N results · 0.6 s". Measure elapsed time per search in the global search
view models (manga + novel) and show results count + seconds in the Tsuzuki header/result tiles. Real measurements.
Result: _pending_

#### N15 Storage breakdown in Data and storage (T3) — `done` (Claude agent `ba3718e28`, merged `6bc2f7cdb`; tablet: 1.7 GB, 4 novels / 1 manga / 4,158 chapters)
Board `08-tools/DataTools` storage breakdown + stat tiles. Measure on IO: downloaded chapters size (download dir),
cover cache size, database size, plus counts (novels, chapters). Show in the Data and storage screen in Tsuzuki
themes; refresh on open; never block the main thread. Unit-test any formatting/aggregation helpers.
Result: _pending_

#### N16 Accent colour + Reduce motion settings (T3) — `done` (Claude agent `772656afa`, merged `2174782d3`)
Board `07-system/Settings` rows. Add `UiPreferences` for (a) Colour theme accent choice (the board's swatches; Dot
stays red) feeding `TsuzukiTokens`/`ProvideBookPalette` default accent, and (b) Reduce motion (app-level, OR-ed with
the system animator scale inside `rememberReduceMotion()`). Rows in Appearance (Tsuzuki themes only).
Result: _pending_

#### N17 Mihon: skip an update version + download from update screen (T2/T3) — `done` (Claude agent `5c66704ba`, merged `62896ccd9`; needs a public release to device-check)
Port Mihon fc20410 (skip this version) and #3707 (download the app update from the update screen) onto
`data/updater/**` and the Tsuzuki `TsuzukiNewUpdateScreen`. Find exact commits with `git log mihon/main --grep`.
Result: _pending_

#### N18 Mihon: failed-to-load extensions with reason (T3) — `done` (Claude agent `ef061ce14`, merged `3419ae572`; #3954 skipped, see UPSTREAM.md)
Port Mihon #3953 (list extensions that failed to load, with reason and stack trace) and, if cheap, #3954 (apply
trust/repo changes without restart). Keep Tsundoku's novel-extension loader paths. Restyle the new rows with
existing Tsuzuki extension tiles.
Result: _pending_

#### N19 Mihon: content warnings instead of the NSFW toggle (T3) — `done` (Claude agent `3911523fb`; no DB/backup change; old show_nsfw value seeds the new default)
Port Mihon #3951 (+ #3952 keep loading installed extensions regardless of warnings) if it applies cleanly to
Tsundoku's extension model (the repo index already carries `contentWarning`); if it needs a DB/backup change, stop
and mark `blocked` with the plan.
Result: _pending_

#### N20 Storage async migration (T4, last) — `done` (Claude agent `19d2b76a4`; all blocking storage probes moved to IO, sync wrappers kept for Downloader/cache; readers verified on tablet)
WORKPLAN P1.11: move synchronous `DownloadProvider`/`DownloadManager` storage callers (reader loaders, download
cache, EPUB export, translation) to the existing suspend APIs together, keeping thin synchronous wrappers only
where Tsundoku merges need the old signatures. Write tests for each moved path. If the change grows past ~600
lines or touches the novel viewer internals, stop at a clean intermediate commit and mark `blocked` with what's left.
Result: _pending_

#### N22b Anime: make the debug player render (T4) — `done` (Claude agent `755f1aa62`; tablet plays a local 720p H.264/AAC file with mediacodec hwdec + audiotrack; the old Google/Blender sample URLs return 403/404)
Tablet test 2026-11-06 05:51: `AnimeDebugPlayerActivity` (debug build) launches, libmpv loads (no
UnsatisfiedLinkError, no crash), but the SurfaceView stays black for both https and http Big Buck Bunny URLs and
mpv prints nothing. Steps: (1) wire mpv log messages (`MPVLib` event/log observer → logcat, `msg-level=all=v`);
(2) compare option order/values with Aniyomi's `PlayerActivity`/`MPVView` at the pinned revision (vo/gpu-context,
`android-surface-size`, hwdec, attach surface before/after `init`, `wid`); (3) for https provide a CA bundle
(`tls-ca-file`) like Aniyomi; (4) verify audio track via `ao=audiotrack`. Launch:
`adb shell am start -n io.github.manojx2005.tsuzuki.dev/eu.kanade.tachiyomi.tsuzuki.anime.debug.AnimeDebugPlayerActivity --es url <url>`.
Done when Big Buck Bunny plays with sound on the tablet.
Result: _pending_

#### Wave 3 — N21 to N32 (same batch rules; continue here after N20, in order)

#### N21 Anime A1.3: vendor the native player libraries (T3) — `done` (Claude `654e0dff6`; checksum tamper test passes; not an :app dependency yet)
WORKPLAN §6.1 A1.3. New Android library module `anime-player` with `src/main/jniLibs/arm64-v8a/*.so` and headers
copied from the archived artifact `app/build/anime-native-41645408336/anime-native-arm64-41645408336/` (verify every
file against its `SHA256SUMS.txt`; add a Gradle check task that re-verifies), licence texts from its `licenses/` into
`docs/licenses/` (LGPL FFmpeg/mpv notices + source offer note), module NOT yet a dependency of `:app` (no APK change).
Result: _pending_

#### N22 Anime A1.4: MPV wrapper + debug player (T4) — `done` (Claude agent `d71870d01`, merged `e49c11127`; see N22b)
Base on `tsuzuki/n21`. Port the MIT `MPVLib` wrapper (aniyomi-mpv-lib, pinned revision in docs/ANIME_SPIKE.md) into
`anime-player`, plus a debug-build-only `AnimeDebugPlayerActivity` (surface, play/pause, seek, back) that plays a URL
passed by intent. Wire `anime-player` into `:app` only for the debug build type (or behind a flag that is off in
preview/release). Re-run a 16 KB alignment check on the debug APK's anime libs and record sizes. No user-visible UI.
Result: _pending_

#### N23 Anime A2.1: schema design doc (T3, doc only) — `done` (`ec00d9a0b`, docs/ANIME_SCHEMA.md; owner questions Q1–Q8 inside)
Write `docs/ANIME_SCHEMA.md`: tables `animes`, `episodes`, `anime_history`, `anime_categories`, `anime_tracks` (Aniyomi
Apache-2.0 schema as reference), SQLDelight migration numbering after the current latest migration in
`data/src/main/sqldelight/tachiyomi/migrations/`, backup proto additions, how library/Home/Updates/History will read
them, rollback/upgrade test plan. No code changes.
Result: _pending_

#### N24 Neural TTS licence research (T3, doc only) — `done` (`52c7dae92`: GO-WITH-CONDITIONS)
WORKPLAN Phase 5 "Neural TTS voices". Determine from sherpa-onnx sources/build scripts whether its Android JNI
library links espeak-ng (GPL-3.0) for Kokoro/VITS, whether an espeak-free build option or lexicon-only voice packs
exist, and their licences/sizes. Write `docs/research/neural-tts.md` with a clear go/no-go and sources (URLs,
file paths, commit hashes). No code.
Result: _pending_

#### N25 Tablet reader side panel (T3) — `done` (Claude agent `502896e0e`; docked on windows ≥ 900 dp, reader_container end padding; not device-checked)
Board `11-tablet/TabletReaderPanel`. On tablet UI in Tsuzuki themes, the novel reader can show a side panel (contents
/ quotes / notes for this book) beside the text instead of the drawer, toggled from the existing contents action.
Reuse `TsuzukiReaderContents`, quotes data and book notes; don't touch viewer internals.
Result: done (`TsuzukiReaderPanel.kt`, hook in `TsuzukiReaderHooks.kt`; docked on tablet UI at >= 900 dp by padding `reader_container`; Contents / Quotes / Notes tabs; no Edit action since the notes screen is not reachable from the reader; not device-checked).

#### N26 Find in chapter (T3) — `done` (Claude agent `a9192df60`; WebView renderer only, hidden in TextView mode)
Board `06-reader/ReaderFind`. In the novel reader, a find bar that searches the current chapter text (query, n of m,
next/prev). Implement for the WebView renderer via its existing JS bridge / `findAllAsync`; for the TextView
renderer only if it can be done through a public hook of `NovelViewer` without changing its internals; otherwise
hide the action there and say so.
Result: _pending_

#### N27 Notebook: quotes + notes for a book (T3) — `done` (Claude agent `ecd4993c1`; details overflow, novels)
Board `06-reader/Notes`. A Tsuzuki "Notebook" screen per book combining existing quotes (highlights) and the
book note, reachable from details overflow and the reader. Real data only (no glossary — not stored).
Result: _pending_

#### N28 Listening widget + TTS state (T3) — `done` (Claude agent `0d7523a9c`; widget not yet placed/checked on a device)
Board `07-system/Widgets` (Listening). Expose TTS playback state from `TtsPlaybackService` (book, chapter, playing)
in a small observable store, and add a Glance "Listening" widget next to `TsuzukiReadingWidget` (play/pause via
the existing media session). Widget picker preview with neutral content.
Result: _pending_

#### N29 Library query builder (T3) — `done` (Claude agent `aa9d395a1`; saved filter/sort presets as pills; library overflow → Saved queries)
Board `08-tools/LibraryQuery`. A Tsuzuki screen that combines existing library filters/sorts (status, unread,
downloaded, source, category, tracker, content type) into saved "queries" shown as pills on the library header.
Store queries in a preference (JSON). Real filters only.
Result: _pending_

#### N30 Performance audit of Tsuzuki lazy lists (T2) — `done` (Claude agent `422bd3ec0`; ~24 key/contentType fixes, docs/research/compose-perf-n30.md)
Every Tsuzuki-owned `LazyColumn/LazyRow/LazyGrid` (presentation/tsuzuki/**, theme/tsuzuki/**): stable `key`s,
`contentType`, no heavy work in item composition (move to `remember`/view model), Coil image sizes for covers.
List each fix in Result. Behaviour unchanged; tests green.
Result: _pending_

#### N31 Accessibility audit of Tsuzuki UI (T2) — `done` (Claude agent `f986b6aeb`; 54 fixes, docs/research/a11y-n31.md)
All Tsuzuki composables: 48 dp touch targets, content descriptions for icon-only buttons (localised, 8 locales),
`Role`s/toggleable semantics, no hardcoded English. List each fix in Result.
Result: _pending_

#### N32 Unit tests for Tsuzuki logic (T2) — `done` (Claude agent `3c71b8dc1`; 56 new cases)
Add JVM tests for untested pure logic in `ui/tsuzuki/**`, `tachiyomi/tsuzuki/**` and `presentation/tsuzuki/**`
(e.g. home view-model mapping, updates summary, palette/tint math, value labels). Extract tiny pure helpers only
where needed (no behaviour change).
Result: _pending_

## Open questions for the owner


- Anime schema (docs/ANIME_SCHEMA.md Q1–Q8): share `categories` with content_type 3? Aniyomi-compatible backups (proto 500/501)? season columns? watch time in the reading streak? table naming; discard the spike's 36.sqm.
- Still parked: release v0.1 / signing / making the repo public; Firebase telemetry removal before release;
  Support us screen; in-app PIN lock; Import EPUB "off" switch.
- Upcoming (B6) now lists only the selected day's books (board behaviour) instead of every date with "Today /
  Tomorrow" headings; duplicate dialog (B2) taps select and Migrate is a separate button (board behaviour). Say
  if you prefer the stock behaviour.

## Owner decisions in force

- 2026-11-13: board 5 `Voices` (`docs/design/boards/fixes-2026-11b/`) is approved as drawn.
- 2026-11-12: the offline natural voice (neural TTS) ships **in 0.1.1** if NT2 passes (lexicon licence, speed, the owner's listening test); otherwise 0.1.1 ships without it.
- 2026-11-12: next release is 0.1.1 without anime; the anime lane is paused until its tag; Codex does the reviews; boards 2026-11b (`docs/design/boards/fixes-2026-11b/`) are approved as drawn; anime-lane implementers may use adb when the lane resumes.

- 2026-11-08: **v0.1 freeze lifted** after the v0.1.0 release. Codex: two lanes (anime, app), cards in Batch S.

- 2026-11-08: **Novel infinite scroll is on by default** (`ReaderPreferences.novelInfiniteScroll`; a saved
  choice is kept). Smoother chapter-end Next chapter → V7.7 in 0.1.1.
- 2026-11-08: **Dot is the default app theme** for new installs, so onboarding starts in Dot (`UiPreferences`
  default; a stored Colour choice and legacy themes → Colour are unchanged).

- 2026-11-07: run Batch R as **two Codex threads in parallel** (lane 1 anime, lane 2 app); tablet novel text
  **capped at ~600 dp, centred** (R17); **no cloud sync** for now (local backups only, R15); dictionary =
  **offline downloadable packs** (WordNet EN, JMdict JA; R13), not bundled in the APK.

- Nav (2026-11-06): HOME · LIBRARY · UPDATES · BROWSE · MORE with plain-word labels; History moves into More and
  Home's recent row (card Q5). Only Colour and Dot themes ship (card Q12). Reader settings follow the batch Q boards.

- Priority: usable, stable, smooth, fast first; then design completion; then Mihon features; then new features;
  anime last (but plan it now). (2026-11-04)
- No full novel/manga split; separate only Tsuzuki-owned code and hooks (2026-11-05, see WORKPLAN Phase 0).
- Owner runs Codex in the Codex app; Claude writes cards here and reviews (2026-11-05).
- Commits authored by the owner only, no co-author or "Generated with" trailers. Commit after review; **no push**
  unless asked.
- Both devices may be used for reversible testing; preview build is the daily/test app; owner's `.dev` data stays
  untouched. Temporary EPUB / Royal Road imports allowed with cleanup.
- Pushed screens follow the Settings boards, not the book palette. Dot "Colour covers" switch defaults on (owner reaffirmed 2026-11-15); grayscale remains selectable.
- Anime native builds via GitHub Actions only; must stay non-GPL (LGPL dynamic libs OK with licences).
- Owner 2026-11-05 (before leaving): push `main` after review — allowed; build the media-types onboarding step
  (novels + manga now); commit the anime spike WIP on its branch; Codex is out of usage, Claude continues alone.
- AppLock PIN, Support us: parked. Release/signing/telemetry: owner-gated (WORKPLAN Phase 7).

## Device and data notes (things that bit us)

- Preview app id `io.github.manojx2005.tsuzuki.debug`, not debuggable → no `run-as`; back up with the in-app
  backup export. Debug app `…tsuzuki.dev` is debuggable (binary restore: `adb push` to staging + `run-as cp`,
  verify SHA-256; never pipe binaries through stdin).
- Git Bash mangles `/data/local/tmp` → use `MSYS_NO_PATHCONV=1` or call adb from Python.
- Long strip: the transition card only appears with "Always show chapter transition" on (it is on in the preview
  copy) or while the neighbour chapter loads; reading to a chapter end marks it read.
- Update check logs HTTP 404 while the repo is private (expected).
- `spotlessApply` per file only (`-PspotlessIdeHook=<absolute path>`), twice.
- Long Windows paths: use `git -c core.longpaths=true` for worktrees.

## Log (newest first)

- **2026-11-22 Codex v0.2 preflight.** AY2-FILTER-CONTRAST `754ed5063` passed the tablet in Dot and Colour and went into trial `260a9e1cd`. The latest tree passed 1,503 unit tests, all 15 APK packaging checks and both optimized ABI audits. The tablet passed the fresh Watch gate and the disposable 25 to 26 upgrade from the exact v0.1.1 tag, preserving the synthetic library and eight settings. Scratch packages removed. Evidence is linked on REL-0.2 and AY2-FILTER-CONTRAST.

- **2026-11-22 Codex anime trial.** `652cfd65e` verified the episode download icons and mixed HTTP/inline subtitles on the tablet. Trial `df1d501d8` integrates AY-INT-F2 with the captured-frame Watch card. Debug and 1,503 tests passed. The integrated tablet passed anime-only History, title search, a search miss and anime off. Scratch packages removed. S22 did not have a verified transport. Evidence `.worktrees/v02-anime-chain/app/build/review-evidence/ay-int-trial/`.

- **2026-11-22 Codex anime lane.** AY9-ART-F `5c2094220` fixes delimiter collisions in Claude's shared-thumbnail cache key. Debug assembly and 1,390 all-module tests passed. Also regenerated the 68 app and 8 novel locale entries on Claude's upstream picks branch.

- **Codex anime and review lanes.** AY3-F2 `f9a072982` + `f81d3d2db` prepares inline tracks on IO and handles concurrent cache publication. Scoped formatting and 1,440 Android unit tests passed. The device check is open. Source review of the AY9 shared-artwork trial reproduced a cache-key collision when URL fields contain the delimiter.

- **Codex anime lane.** AY2-F2 `b0d41bc49` moves source filter work off the UI thread and preserves selections across compatible reorderings. Scoped Spotless and 1,413 Android unit tests passed. The card is in independent review with the filtered-search device check open.

- **2026-11-21 Codex anime and review lanes.** MARK-UNTIL-LAST-READ-F passed and merged locally at `7e333ca2a`. AY9 committed at `eff92bac9` + `7ba2fca41` and passed synthetic capture, cover, Watch and Details checks across both devices and themes. The AY0/AY9/AY1 trial at `25e7ff4f2` passed 1,422 tests, 16 KB APK alignment and the selected playback fixtures on both devices. AY-INT-F2 `7f9d4805c` passed ten focused tests and the actual History screen matrix. PERF-START-2-F3 `deda4c2e2` passed six focused tests. Both fixes are on their own branches for independent review. AY6-F's optimized APK audit passed independently. AY2-F, AY3-F and DICT-HINT-F have review findings on their cards. AY9 live source and tablet layout input, AY1 independent review, and the final release chain remain open. No push or tag.

- **2026-11-21 Claude — nine fix cards from the review lane, all to `review`.** Each on its own branch, no merge, no push: MARK-UNTIL-LAST-READ-F `2448a3a33`, AY4-F `eebba227b`, AY-INT-F `ed304d86a` (tablet: anime-only library, anime search hit/miss, anime off), AY3-F `828d22965`, AY2-F `b7611663d`, AY6-F `3c1db54b0` (strict checker reproduces the 39 misses on the old Preview and passes lib 14/16/17 on a new optimized Preview; 16 KB aligned), DICT-HINT-F `9ec29c270`, KOKORO-TRANSFER-F `91f7e8b16` + `a3e6c779c`, RECAP-VIEWPORT-F `3eeb6f866` + `b8e394c55` (tablet probe: 90% paged plain/resume correct in both themes, 24-case matrix passes). Details on each card. Claude implemented all of them, so none may be reviewed by Claude; RECAP-VIEWPORT-F's first commit is the review lane's trial, so it needs a third reviewer. AY9 left to the anime lane (fixture QA at 20:28–20:35 is not written up yet). PERF-START-2-F2 `797c79710` also to `review` (cold worker passed on the tablet; part 2 dropped). A11Y1 (3 commits on `tsuzuki/a11y1`) to `review` (shared semantics, Japanese swatch names, roles, scrims, spoken toggle/page/step state). Full `:app:testDebugUnitTest` on every branch, 0 failures: A11Y1 1,239, AY-INT-F 1,246, MARK-UNTIL-F 1,223, DICT-HINT-F 1,259, AY2-F 1,244, AY3-F 1,270, AY4-F 1,259, AY6-F 1,267, KOKORO-TRANSFER-F 1,142, RECAP-VIEWPORT-F 1,252, PERF-START-2-F2 1,222 (counts differ by base). AY4-F also device-checked (held Space pauses once; held arrows still repeat). Device lock free.
- **2026-11-21 Codex review lane — PERF-START-2-F and app-chain findings.** S22 20+20 launches: initial display median 554.074 → 433.157 ms, gain 120.917 ms within the candidate’s 203.305 ms run span. Home-observed median 3133.637 → 3077.355 ms. Changes requested for the timing gate and early source readiness. Tablet cold worker fetched and persisted the fixture chapter in its new process. RECAP-F, DICT-F and Kokoro follow-up findings and fix cards recorded. TTS-POLISH retains Claude’s existing independent review while the app chain is held. Both scratch apps on Dot Home, crash buffers empty and Device lock free. Local `main` remains `e9d8bb9eb`. Evidence `.worktrees/review-v02-main/app/build/review-evidence/v02-perf/` and `.worktrees/review-v02-recap/app/build/review-evidence/`.

- **2026-11-21 Codex review lane — AY0 merged.** Local `main` advanced to `e9d8bb9eb`. Android 1,406 tests and Python nine tests passed. Tablet Dot and S22 Colour fixture playback passed. Optimized Preview fixture exclusion verified. MARK-UNTIL-LAST-READ excluded and its storage-write finding recorded. Evidence `.worktrees/review-v02-main/app/build/review-evidence/v02-ay0/`. No push or tag.

- **2026-11-21 Codex review lane — v0.2 main-based set.** Seven cards fast-forwarded to local `main` at `5f428274f`. AY-INT changes requested under AY-INT-F. Debug and optimized Preview assembled, 1,396 tests passed, 506 Tsuzuki keys matched across eight locales, and tablet/S22 crash buffers were empty. Evidence `.worktrees/review-v02-main/app/build/review-evidence/v02-main/`. Next are MARK-UNTIL-LAST-READ and AY0. No push or tag.

- **2026-11-21 Codex anime lane — AY1 handed to Claude at owner request.** 17 staged files, 1,403 tests pass, core fixtures verified on both devices, tablet watchdog and offline evidence captured. Supplemental S22 checks and tablet live playback remain. Device lock free. See the AY1 Result for evidence and resume steps.

- **2026-11-20 Claude — AY0 app half:** debug fixture sources + `AnimeSourceManager` built-ins; case 01 plays on the tablet from the local server; preview APK has no fixture code; 1,227 tests. PERF-START-2-F: part 2 audit (warm-up move unsafe as designed), font warm-up candidate (not measured).
- **2026-11-20 Claude — PERF-START-2-F part 1:** library-update job waits (≤30 s) for sources to load before looking them up; 3 tests, 1,220 suite; `.worktrees/perf-f`, uncommitted. AY-INT: item 2 Update-library hook (tablet: both jobs enqueue) and last-update stamp; item 1 sort-merge logic (6 tests); 1,230 suite. V8.5 search count line hidden while searching (tablet).
- **2026-11-20 Claude — AY0 server half + AY-INT merge logic:** fixture media/server/README/tests (8 pass, deterministic media, PC smoke OK) in `.worktrees/ay0-server`; app half waits for AY9's commit. AY-INT `TsuzukiMixedTimeline` (5 tests). Nothing committed.
- **2026-11-20 Claude — MARK-UNTIL-LAST-READ:** Library selection More menu action added (owner request); 4 tests; tablet + S22 (wireless) checked; uncommitted in `.worktrees/mark-until`, card in `review`.
- **2026-11-19 Claude — app-chain trial:** TTS-POLISH + DICT-F apply together; every locale complete; 1,256 tests pass.
- **2026-11-19 Claude — trial integration:** all eight main-based fix sets apply together (append-only conflicts), 1,231 tests pass, tablet smoke clean. Merge order and notes in Current state.
- **2026-11-19 Claude — STR3 done, uncommitted:** Settings › 同期 fully localized (22 new keys × 8 locales); tablet shows no English sentences.
- **2026-11-19 Claude — AY-INT item 4 complete** (anime tracking switch in Settings › 同期; Library/Downloads/Notifications settled — see card). New card **STR3**: Settings › 同期 has 28 hardcoded English strings.
- **2026-11-19 Claude — AY-INT item 5 complete:** anime Downloaded-only filter, queued anime in the queue header, Categories links to anime shelves. Uncommitted in `.worktrees/ay-int`.
- **2026-11-19 Claude — AY-INT item 5 (Downloaded only for anime):** done in code with tests; device check needs anime downloads in a scratch library.
- **2026-11-19 Claude — AY-INT item 4 search:** anime player settings are searchable; fixed a crash when the player page opens from search (non-null back handler). Uncommitted in `.worktrees/ay-int`.
- **2026-11-19 Claude — AY-INT started (item 4, Settings anime row refreshes live):** uncommitted in `.worktrees/ay-int` from main; tablet toggle check passes.
- **2026-11-19 Claude — V8.4 done, uncommitted (owner: Claude may take Codex cards too):** migration language pills aligned, migration book list uses the Tsuzuki tile row, About tile/credits fixed; tablet checks pass.
- **2026-11-19 Claude — V8.4 About part done, uncommitted:** licences tile wraps to two lines, credits licence gets a gap; tablet check passes.
- **2026-11-19 Claude — V8.5 done, uncommitted (waits for owner OK to commit):** Library search keeps the すべて/小説/マンガ/アニメ pills (one condition in `NovelsTab.kt`); tablet check passes. DICT-F also localizes the browse Select chip.
- **2026-11-19 Claude — DICT-F done, uncommitted (waits for owner OK to commit):** chapter script hint routes kanji-only words in Japanese chapters to Japanese; 18–21 missing dictionary/app-update strings translated in 7 locales. Tablet shows the Japanese sheet in Japanese.
- **2026-11-19 Claude — READER-JP-FONT done, uncommitted (waits for owner OK to commit):** TextView reader uses the Literata + Noto Serif JP pairing; Japanese no longer falls to the system sans. Bundled JP serif is SemiBold — Regular subset suggested.
- **2026-11-19 Claude — anime empty-state sweep (tablet, Japanese):** Watch home, Library › アニメ and Browse › アニメ show no English (empty states only; no anime in the scratch library). READER-JP-FONT narrowed to Literata (明朝 renders Japanese serif).
- **2026-11-19 Claude — DICT-F narrowed + READER-JP-FONT filed:** kanji-only lookup fails in TextView too (language guess, not renderer); TextView reader loads Literata without the Noto Serif JP partner (`NovelViewer.kt:1941`).
- **2026-11-19 Claude — dictionary WebView Japanese check:** fails → new card DICT-F (kanji-only words go to the missing Chinese pack instead of bundled JMdict; the app chain lacks 18 ja / 21 other-locale `tsz_` strings).
- **2026-11-19 Claude — DICTIONARY-DEFAULTS gap closed:** turning text selection on inside an open chapter brings 辞書 back without reopening (TextView, tablet).
- **2026-11-19 Claude — TTS-SETTINGS gaps closed on the tablet:** settings search finds the TTS rows with their path; speed persists across force-stop.
- **2026-11-19 Claude — TRACKER-CLIENTS MAL stage code review:** pass (callback host dispatch, new scheme, public PKCE client); device sign-in needs the owner.
- **2026-11-19 Claude — card status hygiene:** 27 anime cards (A2.3–A6.4, R-A1–R-A5) whose branches are in `main` set to `done`; S1, S2, S4, V7.7, R12, R12-F, R14 are `blocked` but their branch commits are in `main` — noted on each heading for the owner/review lane to settle.
- **2026-11-19 Claude — English-leak sweep (tablet, Japanese):** Updates, History, Stats, Categories, More clean; accent swatch labels English (added to A11Y1); More › Help and eight other links open Tsundoku's docs site → V8.6 (owner decision).
- **2026-11-19 Claude — Library search check:** no-results state and global-search link work; new card V8.5 (search drops the All/Manga pills).
- **2026-11-19 Claude — V7 tablet checks (optimized preview of the chain):** novel reader survives process death (same chapter, Listen dock); download queue survives force-stop as Queued. `am kill` cannot kill during a download (foreground service). Rotation not tested (system setting). Recorded in `docs/RUNTIME_QA.md`.
- **2026-11-19 Claude — V8.1 done, uncommitted (waits for owner OK to commit):** novel download queue rows translated (existing keys + 2 new `tsz_` keys); 5 status tests; tablet shows the row fully in Japanese. `.worktrees/v8-1`.
- **2026-11-19 Claude — V8.3 done, uncommitted (waits for owner OK to commit):** migration now offers only sources of the book's kind (config list, list search and single-book search); saved selections of the other kind are kept. 5 new tests; tablet self-check passes. `.worktrees/v8-3`.
- **2026-11-19 Claude — V8 tablet pass:** manga reader bars, download queue (Cancel all), migration config, About credits checked on the Japanese tablet. New cards V8.1 (queue rows English, ready), V8.2 (empty-state kaomoji, needs owner), V8.3 (migration offers wrong-type sources, ready), V8.4 (migration/About polish, ready). Evidence `app/build/review-evidence/v8-claude/`.
- **2026-11-19 Claude — STR2 done, uncommitted (waits for owner OK to commit):** moved onto current main (`.worktrees/str2-main`, `tsuzuki/str2-main`); all 482 `tsz_` keys present in 8 locales, new `TsuzukiLocaleCompletenessTest`; four Kotlin literals moved; fixed Advanced-fold matching that used English titles (never folded on Japanese devices). 1,219 app tests pass; tablet self-check passes.
- **2026-11-19 Claude — DETAILS-REFRESH-TRACKING device check:** no below-cover block while refreshing (Colour + Dot, slow local server via adb reverse, removed); error card keeps Retry/WebView after refused and HTTP 503. Not merged (app chain). Also left a likely-cause note on RECAP-F for the paged plain-text fallback failure. Evidence `app/build/review-evidence/details-refresh-claude/`.
- **2026-11-19 Claude — TTS-POLISH independent review:** pass on the tablet (Colour + Dot, TTS page, Listen sheet, reset/language dialogs, Updates/History/Browse labels); stacked value layout, phone width and large font not verified (S22 offline). Not merged: based on the unreviewed app chain. Found the Codex RECAP-F review stopped since 2026-11-15 with a failing tablet run and `animescratch` unrestored on both devices; left as is for the owner. Evidence `app/build/review-evidence/tts-polish-claude/`.
- **2026-11-15 Codex independent READER-NEXT-GUARD review passed:** original Claude b53622e21 plus e46df5e41 preserves Q2 automatic-load replay through the manual-tap cooldown (red/green regression). Debug/Preview + 1,384 tests pass; delayed scratch source proves loading indicator, two handler requests → Chapter2 and later request → Chapter3, both themes/devices. Original scratch DB/prefs restored byte-equal, helper removed, Dot Homes inspected and crashes empty; lock free. Evidence/APKs `app/build/review-evidence/reader-next-final/`. Local merge only. TTS-POLISH d4878ac49 remains independent-review pending; AY9/queued anime cards not yet complete.

- **2026-11-15 Codex — TTS-POLISH review:** existing defects from Claude device QA fixed: 13 TTS labels translated, reset/grouping/source labels localized, adaptive value wrapping and inline notice icon. Debug + 1,420 all-module tests, scoped formatting twice, locale/APK resource audits pass. Based on app-chain d9b94f259; app implementer does not self-review/merge. AY9 safely preserved and devices restored/free per owner priority; manga Next guard independent review next.

- **2026-11-15/21 Claude — Kokoro device half (tablet):** pack rebuilt from the cached upstream archive (SHA-256 matches the pin) is byte-identical to `kokoro-cmu-v1-reviewed.zip` (`208f69b1…`); installed through the in-app picker (all files verified). **Crash found and fixed:** the worker aborted on the first covered chunk — sherpa-onnx's JNI looks up `invoke([F)Ljava/lang/Integer;` on the callback, but the Kotlin lambda compiled to a synthetic class with only `invoke(Object)` (`NoSuchMethodError` → SIGABRT in `:neural_tts`). `KokoroTtsService` now passes a named `KokoroProgressCallback : (FloatArray) -> Int` (javap confirms the typed method). **Uncommitted** in `.worktrees/kokoro-integration`. Retest 2026-11-21: no crash; load verify 464 ms + model 1,348 ms; covered 63-char chunks synth 2.6 s for 3.1 s audio (**RTF ≈ 0.85**, 2 threads), first audio 2.6 s after each request; chunks with digits ("Chapter 1, paragraph 1.") go to the phone voice and playback alternates correctly through the chapter. **Gap fixed 2026-11-21:** after a phone-voice hand-off the worker now prepares the next chunk's first piece (`nextNeuralPrefetch`, keyed by speed+text, cancelled on a different request); tablet: first audio after each hand-off **2.6 s → 3–8 ms**, prefetch hit every time, chapter to the end, no crash (`b9f2eca6f` on `tsuzuki/kokoro-integration`). **S22 (2026-11-21, same build, pack copied via the documented `run-as` route because the picker's index hadn't seen the pushed zip):** load verify 564 ms + model 1,641 ms; covered 63-char chunks synth 3.24–3.37 s for 3.09 s audio — **RTF 1.05–1.09 (slower than real time on the phone)**; prefetch hit on every phone-voice hand-off, first audio 6–9 ms; chapter to the end, no crash; `:neural_tts` **TOTAL PSS ≈ 592 MB** (RSS 670 MB). With RTF > 1, a chapter of consecutive covered chunks will still have ~0.2 s gaps per 3 s chunk (growing with chunk length); **4 threads tried (S22, 2026-11-21): RTF 1.01–1.06 vs 1.05–1.09, PSS unchanged ≈ 589 MB — not worth the extra CPU/battery; reverted to 2.** Remaining options for NT2: shorter chunks (less silence per gap, not faster), a smaller/faster model or quantisation, or accepting brief gaps. Tablet: worker memory while paused after playback: **TOTAL PSS ≈ 463 MB** (RSS 561 MB) for `:neural_tts` — large for phones; NT2 must measure the S22. Not done: S22, battery, the owner listening test. Owner also asked (2026-11-15) for a **downloading screen** for the voice pack → **built 2026-11-21 (`75fc7715b`, debug test section)**: Settings › Text-to-speech › *Download test voice pack* opens a dialog styled on board 5's downloading tile — theme progress bar, "46 of 124 MB · Wi-Fi", Pause/Resume (HTTP Range resume of the kept partial), Cancel (deletes it), free-space line ("52 GB · needs 318 MB while installing"); the zip installs through `KokoroPackImporter` (every file checked). Address = debug preference `tsz_kokoro_pack_url`, default `github.com/Manojx2005/Tsuzuki/releases/download/voices-v1/kokoro-cmu-v1.zip` (**owner: publish there, or tell me the address**); until then the dialog says the pack is not published yet. Tablet (local Range server via `adb reverse`): full download + install; pause at 16 MB kept 16,523,264 bytes and resumed with `Range: bytes=16523264-`; cancel deleted the partial; remove; unpublished message; no crash. `KokoroPackDownloadTest` (6). The release Voices page (NT3) still needs WorkManager so a download survives process death.

- **2026-11-15 Claude — app chain + uncommitted review fixes:** all-module `testDebugUnitTest` **1,420 tests, 0 failures/skips** (`.worktrees/app-chain/app/build/review-evidence/app-chain/allmodules-2.log`). Committed on `review/app-chain` as **`d9b94f259`** (7 files): dictionary error logging; recap skips finished chapters; RECAP-F label/font/padding/6-line cap in the Compose card and `chapter-recap-card.js`; shared `rememberNovelFontFamily`. **Waiting for the owner:** Kokoro model download + storage folder on the Kokoro scratch app, and whether the app chain rides in v0.2.

- **2026-11-15 Claude — Kokoro device half started (prep only):** `tsuzuki/kokoro-integration` (`9837be10d`, untracked runtime AAR `app/libs/sherpa-onnx-2.0.1-tsuzuki-kokoro-arm64.aar`) built as scratch package `kokorodebugscratch` (Debug; contains `libsherpa-onnx-jni.so` 20.9 MB and the runtime licences); APK archived `.worktrees/kokoro-integration/app/build/review-evidence/kokoro/`. Installed on the tablet only. Owner 2026-11-15: use the scratch app's folder → storage set to `Documents/Tsuzuki-RV5-Scratch` through the system picker (folder access allowed), optional permissions skipped, no backup imported. Settings › Text-to-speech shows the debug section "Kokoro · listening test" with **Install test voice pack** (asks for `kokoro-cmu-v1.zip`). **Not done, waiting for the owner:** download of the upstream model archive `kokoro-int8-multi-lang-v1_1.tar.bz2` (GitHub k2-fsa/sherpa-onnx, ~120–150 MB, SHA-256 pinned) to build the test pack; then pack install, `KokoroTiming` numbers, NT2 speed/memory, owner listening test.

- **2026-11-15 Claude — app chain on devices (owner: use the devices):** chain scratch APK `rv5debugscratch` (SHA-256 `4d325f73…`) on tablet + S22; launches in Colour (tablet) and Dot (S22), crash buffer empty. Tablet: **READER-DEFAULTS pass** (≈17 dp gutters, serif, left-aligned); **DICTIONARY pass** (selection shows 辞書; "lights" → WordNet "light", Built-in/Online pills); **TTS-SETTINGS** sheet opens from the Listen bar with all controls, but many new strings are English on the Japanese device and the info icon sits on its own line (→ AY-INT item 8); **DETAILS-REFRESH** details page has no under-cover strip (refresh state itself not triggered); **RECAP-FIX** overlay appears (with `force-recap`), **finding:** reopening a finished chapter (position at the end) recaps paragraphs 4–6 that are still on screen under the overlay — when the resume point is at/near the end, use the previous chapter's excerpt or skip. **Fixed (uncommitted, app-chain worktree):** `novelRecapMode` returns None at entry progress ≥ 0.99 (`NOVEL_RECAP_FINISHED_PROGRESS`), test added in `NovelRecapTest`; recap tests pass; rebuilt scratch APK on the tablet: reopening finished chapter 2 shows no overlay, crash buffer empty. S22 (Dot, same APK): details page has no under-cover strip; reader keeps the phone's 16 dp gutters (READER-DEFAULTS phone unchanged); finished chapter reopens with no overlay; crash buffer empty. Tablet **WebView renderer** (set `pref_novel_rendering_mode=webview` in the scratch prefs; confirmed `android.webkit.WebView` in the view tree): same ≈17 dp gutters and serif as TextView, no overlay on the finished chapter, selection shows 辞書 and opens WordNet "light"; crash buffer empty. Scratch app left on WebView rendering. **Recap card above text (WebView, chapter 2 marked unread):** pass — card 前回まで with chapter 1 paragraphs 4–6 above chapter 2. **Gap vs Recap board (`fixes-2026-11b` board 3):** the board shows the previous chapter's name and the time since it was read; the card shows only the heading and paragraphs → card **RECAP-F** (Codex). **TTS voice sample** (tablet): "Play voice sample" starts a Google TTS audio stream on the speaker (system audio log), no crash; device muted, so not heard. **TTS-SETTINGS on the tablet:** Listen playing at paragraph 2/6 → gear opens the sheet and pauses narration; speed raised to 2.4× → closing returns to paragraph 2 paused with 2.4× on the bar; Play resumes at paragraph 2 and reaches 3 in 5 s; speed reset to 1.0× afterwards. (A crash entry in the buffer at 20:10 is `uiautomator`'s own process, not Tsuzuki.) **S22 (Dot):** reader menu → 読み上げ → Text-to-speech opens the full-width sheet; same English strings and loose info icon as the tablet, plus the long values are cut off on the phone ("ALL …", "AUTOMATIC · MATCH THE CHAPTE…") — stack the value under the title when it doesn't fit (added to AY-INT item 8). Not done: S22 reader checks, WebView renderer, recap card-above-text case, TTS sample/voice change. Evidence `.worktrees/app-chain/app/build/review-evidence/app-chain/`. Owner also reported anime gaps across tabs (Library All, Updates, Settings…) → new card **AY-INT**.

- **2026-11-15 Claude — app chain (`review/app-chain`, `e5bc7ef6f`) verified without devices:** Debug builds; **1,419 all-module unit tests, 0 failures/skips** (1,252 in `:app`). Code-reviewed RECAP-FIX (card added, pass with notes), DICTIONARY-PACKS (availability/removal/restore/gzip download/lemma lookup) and the integration commit `e062867d9`. Uncommitted fix in the app-chain worktree: `ReaderDictionary.kt` now logs the two swallowed exceptions (lookup failure, built-in restore failure); dictionary tests pass. Note: a Japanese copy extracted by an earlier debug build is left orphaned on disk now that Japanese is optional (scratch installs only). **Still needed before merging the chain:** device review on the tablet and S22 (TTS-SETTINGS, READER-DEFAULTS, DETAILS-REFRESH-TRACKING, DICTIONARY-DEFAULTS/PACKS/DESIGN, RECAP-FIX scripts in their cards) — devices are held by Codex AY9; owner decides whether the chain rides in v0.2.

- **2026-11-15 Claude — v0.2 card plans:** detailed Codex plans for AY0, AY1, AY2, AY3, AY4, AY6 and the new REL-0.2 card written in `docs/ANIME_ANIYOMI_PARITY.md` §9, from `main` `304bd9aff` (plus a reviewer checklist for AY9, §9.0). Decisions recorded there: on Auto the source's order and `preferred` win; extension `file:`/`content:`/`data:`/magnet video addresses stay refused; subtitle colours as fixed pills; no background playback in v0.2; fixture registered through a debug class found by name. Found while reading: anime downloads never apply the source's headers (fixed in AY1); cleartext is already allowed, so AY0 needs no network config.

- **2026-11-15 Owner decision — anime release scope:** v0.2 = AY9, AY0, AY1, AY2, AY3, AY4, AY6 + review/audit/upgrade test. The other seven Batch AY cards (A6.5, AY5, A6.8, AY7, A6.6, A6.7, AY8) are after the release. Also: bundled dictionaries = English only (removable); other app languages are optional downloads (DICTIONARY-PACKS, `tsuzuki/dictionary-packs`; packs built locally, publishing to `dictionaries-v1` awaits the owner). Integration of the app chain on `review/app-chain` (Claude).

- **2026-11-15 Codex RV3-S final acceptance:** download protection `a3740a40c` and History pill sizing `03128b5ca` independently reviewed; final Debug/Preview builds, 1,382 tests, 686 locale checks, 10 APK/100 ELF alignment checks pass. History on/off/theme matrix in Debug plus both themes optimized Preview passed on tablet/S22. Scratch data restored byte-for-byte, helpers removed, screenshots inspected, crashes empty; devices free. Evidence/APKs archived `app/build/review-evidence/rv3-s-final/`. Dot book cover colour default confirmed; stale docs corrected. Reader defaults still separate-review pending. Local merge only, no push/tag.


- **2026-11-15 Codex handoff checkpoint:** READER-DEFAULTS `9a483ac31` / docs `0e33d9f43` ready for independent review (720 dp shared cap, 1,092 tests). RV3-S on `review/rv3-s`: download-retention regression reproduced and fixed in `a3740a40c`; pre-History-fix candidate passes Debug/Preview, 1,382 all-module tests, 10 APK/100 ELF alignment checks, locales and both-device playback/gate matrix. Final blank History pill fix `03128b5ca` committed, independently code-reviewed and formatted; final rebuild and device verification remain before merge. Original scratch snapshots restored and verified, helpers removed, devices free on Dot Home, no crashes. Main code untouched; no push/tag. Evidence `.worktrees/rv3-s/app/build/review-evidence/rv3-s/`; complete continuation instructions in the afternoon Current state block.

- **2026-11-15 Owner decision:** accepts that `anime-player` is a normal `implementation` dependency, so release/preview APKs ship libmpv/ffmpeg natives (LGPL notices via aboutlibraries) and the arm64 anime toggle in Settings › Browse, with anime off by default. No build-type gate needed for RV3-S.

- **2026-11-15 Claude — DICTIONARY-DEFAULTS device check:** old 0.1.1-equivalent build on the tablet reproduces the owner's bug (辞書 action present, sheet shows only the headword and "get offline dictionary" — no data on a fresh install). Fixed build (`92e05d833`) as `rv5debugscratch` (install -r): WordNet result in TextView and WebView on tablet and S22, JMdict 図書館 on tablet, Online pill (Free Dictionary API) on tablet. Not tested: selection toggled on after opening, WebView Japanese, upgrade over an old pack. Evidence `.worktrees/dictionary-defaults/app/build/review-evidence/dict-check/`. Owner 2026-11-15: the sheet does not match the Dictionary board → DICTIONARY-DESIGN (Claude subagent, `tsuzuki/dictionary-design`). Merge still waits on the owner's APK-size decision (+29 MB).

- **2026-11-15 Claude — RV3-S code review of `main...tsuzuki/ay-play`: pass with notes.** Gate sound (`TsuzukiAnime.enabled` = arm64 + pref, default off; Watch pill, slots, jobs, widget and the three activities all re-check it; the "activities not gated" note was stale). Migrations: 36 unchanged, 37–39 additive. No v0.1.1 behaviour lost in the eight merge resolutions. Codex `5d9c530da` read line by line: value quoting correct. Fixes in progress on `tsuzuki/ay-play` (Claude subagent): allowlist + name check for source `mpvArgs`, http(s)-only video URLs, log failed extension loads and blank mpv errors. **Owner decision:** `anime-player` is now `implementation` (not debug-only), so release/preview APKs ship libmpv/ffmpeg natives and the arm64 toggle in Settings › Browse even while off by default — accept, or gate the dependency by build type. Cleanups noted, not blocking: `BackupProtoReader.kt` rewritten instead of a small hook; `App.kt` moved upstream init lines under `isMainProcess`; validator does not run `migrateExtensionStore` on legacy Aniyomi field 106/108; restore preview still does not count anime. Also 2026-11-15: TTS-SETTINGS review bug fixed (`c18908cf5`, pause after a chapter ended replayed the last chunk); owner reports the novel recap never appears → RECAP-FIX (Claude subagent, `tsuzuki/recap-fix`); Codex out of usage → Claude subagent finishing KOKORO-INTEGRATION code half.

- **2026-11-15 TTS-SETTINGS:** `a039a7e5d` adds top-level/searchable speech settings and direct Listen shortcuts, installed voice/language filtering, samples, phone-engine settings, speed/pitch, playback and read-along controls. Pausing during silent gaps now cancels pending handoff. Debug and 1,099 tests pass, formatting/16 KB/dictionary packaging checks pass; phone/tablet review pending, no install. Kokoro remains outside Listen until NT1/NT2 gates pass.

- **2026-11-15 DICTIONARY-DEFAULTS:** `a6d8b27ea` + `1d1b8e194`; bundled full WordNet/JMdict, explicit keyless English Online lookup, and selection-menu restoration for both novel renderers. Debug + 1,097 tests + two converter tests pass; fresh APK asset/checksum/integrity/licence audit and 16 KB alignment pass. Device menu/lookup review pending; no app-lane adb. See card and `docs/DICTIONARY_PACKS.md`.

- **2026-11-15 TRACKER-CLIENTS MAL stage:** `13c5a42c5`; owner approved the prepared API registration/agreement. MAL registered and published Tsuzuki; live consent branding verified. New native client and callback wired, all legacy callbacks retained. Debug + 1,091 tests pass; device sign-in/refresh remain review work. AniList is signed out; remaining provider access requested. No list-access grant, push or device install.

- **2026-11-15 Codex app follow-up — DETAILS-REFRESH-TRACKING:** `cb30680f0` removes the extra below-cover refresh loader and changes tracker request/project identities plus Kavita plugin name to Tsuzuki. Debug and 1,091 tests pass; scoped formatting clean. All 14 trackers audited in `docs/TRACKER_BRANDING.md`; six provider consent applications need separate registrations (TRACKER-CLIENTS). MAL Android form prepared in the owner's signed-in Vivaldi, unsent and terms unchecked pending action-time approval. No app-lane adb or OAuth credential change; Kokoro remains an audited, unintegrated runtime.


- **2026-11-15 Codex anime lane — AY-PLAY device half:** `5d9c530da` passes source `mpvArgs` to per-file loadfile options, fixing reproduced AnimeKai image-prefixed HLS/PNG immediate EOF. Player-only Android 16 opt-out restores tablet landscape. 1,210 tests, Debug and 16 KB ZIP check pass. Tablet + S22 scratch-only Dot/Colour new/resume/EOF matrix; AnimePahe recovered and plays two episodes in Colour, Anikage returns zero videos with readable errors. Final APK b892ca50 reinstalled and smoked; Dot Homes inspected, crash buffers empty, Device lock free. Evidence `.worktrees/ay-play/app/build/review-evidence/ay-play/`. Independent RV3-S next; AY9 waits.


- **2026-11-15 Claude — 0.1.1 published; anime reopened.** Checked Codex's refreshed candidate (1,091 `:app` tests re-run, review fixes read, unpushed history scanned), replaced the stale local `v0.1.1` tag, pushed `main` and `v0.1.1` with the owner's permission. Release workflow passed: five signed APKs at the v0.1.1 release page. CI's Build & Test failed on one stale `:domain` test, fixed after the tag. Owner: anime first, STR2/A11Y1 parked. A6.4 committed as left (`028a0c06d`); anime chain merged with v0.1.1 on `tsuzuki/anime-sync` (`a7a6b3f9f`, 1,187 tests). AY-PLAY started.

- **2026-11-15 Codex review lane — 0.1.1 refresh complete.** Final source 6119f2613, BACK1 + 180 ms opening refinement (main cherry-pick 5c4b89421). Cached openings settle by 226–260 ms; final Back regression passes. Preview/Release + 1,091 tests, 15 APK alignment/profile audits and exact direct tagged-v0.1.0 Y4 upgrade comparison pass. Both final Dot Homes inspected, awake, crashes empty; lock free. Source-only startup warm-up and diagnostic commits remain unmerged; 500 ms startup target/follow-up stays open. Final hashes/artifacts in rel-011-refresh-final/final-audit.json. No tag/push; owner publishes.

- **2026-11-15 Codex review lane — BACK1 passes with review fixes.** Original f9d047d95 merged only with 3305865d0 (saved Home key) and 7e3a3d635 (explicit root navigator). Original normal Back regression reproduced and corrected. 1,091 tests, 48 timed Back cases, 16 edge gestures and all four ordinary/process-restoration checks pass; Dot/Colour phone/tablet screenshots inspected, crashes empty. Candidate refresh follows; no tag/push. Evidence back1-review/.

- **2026-11-15 Codex review lane — PERF-START-V passed; PERF-START-2 not merged.** Baseline helper validated (ten counts exactly one), full samples in PERFORMANCE.md. d1950daa5-only candidate built and 1,095 tests passed, but initial/Home median gains did not exceed observed variability; 500 ms target retained by delegated owner judgment. Twenty-launch/local search smoke passed with stated scope; periodic job starts a dead process but actual cold source fetching remains unverified. Diagnostic eb5e21fa7 measured on isolated branch only, never merged. PERF-START-2-F created. Owner authorizes taking over BACK1 independent review and refreshing 0.1.1 with BACK1 + faster details; STR2 remains untouched. Evidence perf-start-v/ and perf-start-2/.

- **2026-11-14 Codex review lane — NT-LEX1 pass.** Independently reviewed `243121aea` via `review/nt-lex1`: deterministic full-data hash, licence/vocabulary/source mapping and 30-word checks pass; 50 Python tests, Debug/Preview and 1,087 Android tests pass. Scripts only; voice/listening gates remain. No devices. Evidence `app/build/review-evidence/nt-lex1/`. Owner also authorized refreshing 0.1.1 with BACK1 and PERF-DETAILS; wait for BACK1 to leave claude-working and the Device lock to be free. No tag/push.

- **2026-11-14 night, Claude review of the app lane.** PERF-START pass, merged `7defdb569` (investigation + Home-observed helper; miss still open). R12-WARN pass, stays on `tsuzuki/r12-warn`. NT1-BINARY + NT1-BINARY-F pass after an independent artifact re-measure, scripts merged `343a17309` (runtime not integrated, voice still blocked). PERF-DETAILS pass after review fix `260691fba`: S22 235–292 ms vs 300 ms in Dot and Colour, merged `c0a12f2c4`. `tsuzuki/rel-0.1.1` untouched and still the 0.1.1 tag point. New cards: PERF-START-V, PERF-START-2 and NT-LEX1 (Claude subagents working), BACK1, STR2, A11Y1. Evidence `app/build/review-evidence/perf-details/`. No push, no tag.

- **2026-11-14 R12-WARN app lane:** `9cfc67ed7` adds the missing malformed-source-health decode warning without logging JSON/exception contents; fallback unchanged. Debug assembly and 984 existing tests pass on R12-F base, scoped formatter twice clean. Independent review pending; R12-F trust/runtime check remains deferred and no feature was restored on main. Evidence `.worktrees/r12-warn/app/build/r12-warn/`.



- **2026-11-14 NT1-BINARY-F app lane:** `d904693d2` removes local and inherited ONNX CI paths through source compilation, bundles full kissfft BSD-3 text and strengthens source/UTF-16 audits. Complete offline recipe, 21 tests, independent NDK checks and identical-input packaging pass. Final AAR SHA-256 `1f70ec2b28f40dfe760a5fe1283e623a1163155c5c530b5a70ae06df1eae4ec1`; independent review still required, no voice pack/app integration/S4 merge. Evidence `.worktrees/nt1-binary/app/build/nt1-binary-evidence/f-*`.



- **2026-11-14 PERF-DETAILS app lane:** `3f15ad172` synchronizes details cover and fade at 200 ms under the owner's new 300 ms total-opening budget. Debug, optimized scratch Preview, 1,087 tests, scoped formatting and APK 16 KB alignment pass. Independent S22/tablet runtime review pending; no measured budget pass or main merge. Evidence `.worktrees/perf-details/app/build/perf-details/`.



- **2026-11-14 Codex independent review lane — RV5 Case B completed.** Accepted S2-F, RP1, STR1, ERR1 and TTS-LANGUAGE; review fixes `0a96ace3d` (first-line/dock clearance), `bf5055dcd` (CSV title), `8570ff5d7` (reader loading/progress/held chapter-end state). Debug + optimized Preview + 1,085 tests pass; 16 KB APK alignment pass, both crash buffers empty. Tablet/S22 scratch-only Dot/Colour QA; archive `app/build/review-evidence/rv5/`. External/CDN and unforced matrix limits are explicit in ERR1 verdict. Main fast-forward next; no push.

- **2026-11-14 — NT1-BINARY candidate ready for independent review:** `d1e360ff4` + `d580d500e`; all official Android variants fail the unchanged literal gate, while an isolated arm64 Kokoro-only source profile passes raw strings, 16 KB ELF alignment and TTS JNI API checks. Candidate 7,857,879 B, `b2cee72b9131cf5190753c966e23e0c0e65ef7a0e9313e434d074d4aa3b5fa47`; 13 Python tests and offline recipe pass. Evidence `.worktrees/nt1-binary/app/build/nt1-binary-evidence/`. No model, app integration, install, merge or push. RV5 is now ready for an independent chat.


- **2026-11-14 — ERR1 implementation ready for independent review:** `ad3e6a871` adds actionable mapped errors, redacted diagnostics and delayed loading stages in catalogue/search/details/readers/download rows. Debug APK and 1,039 tests pass, 23 keys verified in eight locales; evidence `.worktrees/err1/app/build/err1-evidence/`. No device verdict or merge. Owner reports USB tablet + wireless S22 available and asks to investigate a binary satisfying NT1; gates remain in force.


- **2026-11-13 RV4 (Codex review lane).** Accepted V7.7-F and CHAPNUM; MAIN-REPOS owner policy committed `7f697fef9`, plan/boards `3e610bd08`; Add Repository review fix `7f51d31da`. Accepted Debug/Preview builds and **1,011 tests** pass; tablet Dot/Colour reader, mismatch-label and fresh-repository checks; crash buffer empty. R12-F/R14/S4 deferred and excluded (`89e1ac9ac`/`451168815`/`911a2222f`), TTS-LANGUAGE skipped dirty. No model downloaded or extension trusted. Evidence 225 files archived `app/build/review-evidence/rv4/`; scratch apps removed, daily tablet on Dot Home; S22 deferred. RV4-01 Native bottom-card overlap added to RP1. No push.

- **2026-11-13 RV2 (Codex review lane).** Accepted R15, S3/S3-F and R13 after fixes `005713576`, `ea748689e`, `bb82bd9f1`, `286d18b7b` plus the dictionary update regression. Candidate Debug and scratch Preview built; final suite **1,006 tests, 0 failures/errors/skips**, per-file Spotless twice; browser recap 3/3 sizes; converter 2/2. Tablet scratch QA passed backup export/restore and invalid/settings-only inputs, cold recap in all three reader modes, paged resume, and no-pack management; crash buffer empty. S2 excluded (`591429e49`) for the documented over-count; S1 excluded (`dab21422c`) pending explicit trust approval for a live source. S22 deferred by owner. Evidence archived at `app/build/review-evidence/rv2/`; daily Preview data untouched. No push. Next RV4, then this chat implements app cards for a different reviewer.

- **2026-11-10 A6.3 (Codex).** The anime duplicate check is in review (`6ef424d63`). Debug
  assembled and all 1,071 unit tests passed. Android dialog and migration checks remain.

- **2026-11-10 A6.2 (Codex).** Anime source migration is in review (`152f8b944`). Debug
  assembled and all 1,067 unit tests passed. Android navigation and two-source checks remain.

- **2026-11-10 A6.1 (Codex).** Kitsu anime tracking is in review (`9e17de25e`). Debug assembled,
  all 1,063 unit tests passed, and unauthenticated anime search worked against Kitsu. Authenticated
  verification remains in the device review lane.

- **2026-11-10 A5.3 (Codex).** Subtitle-group exclusions are in review (`ea92e39eb`). Debug
  assembled and all 1,058 unit tests passed. Android filter-sheet and playback checks remain.

- **2026-11-10 A5.2 (Codex).** Season pills and library grouping are in review (`b9766a725`).
  Debug assembled and all 1,054 unit tests passed. Source and import fixtures still need device review.

- **2026-11-10 A5.1 (Codex).** Anime PiP is in review (`098e584f0`). Debug assembled and
  all 1,049 unit tests passed. Home, Recents and window-close behavior await Android review.

- **2026-11-10 S3-F (Codex).** Paged WebView resume fix ready for RV2 (`c3029ea69`). Debug
  assembled, 973 unit tests passed, and Chromium verified the same story lines at three viewport
  sizes. Android verification stays with RV2 alongside Claude's local-source and hard-wrap changes.

- **2026-11-10 RV2 paged recap (Codex QA).** The browser fixture reproduced RV2-12. It also verified that the recap renders and Dismiss calls the bridge once. Synced the five implementation results from the RV2 worktree and queued follow-ups for R15, S3 and R13. S1 and S2 remain in device review.

- **2026-11-10 RV2 (Codex QA).** Logged RV2-01 through RV2-11 with reproduction steps and evidence. Tested the backup flows, Manhwa filter, recap, listening stats and no-pack dictionary across the isolated Preview installs on Tab S11 and wireless S22. All 1,007 Debug unit tests passed. Removed both scratch installs and left RV2 in review.

- **2026-11-10 A4.2 (Codex).** Release packaging (`70ddd61c4`) is in review. Debug, Release and Preview built, 1,040 tests passed, and all ten optimized APKs passed 16 KB ZIP alignment.

- **2026-11-09 A4.1 (Codex).** Anime restyle parity implementation (`12ed776cd`) is in review. Debug assembly and 1,038 tests passed. Dot and Colour device checks are pending.

- **2026-11-08 RV1 (Codex review).** Passed V7.6, R16 and R17. V7.7’s WebView tap exposed reader controls and R12 missed native Asura Scans traffic, so both were reverted and queued for fixes. Accepted Preview assembled and 977 unit tests passed. Dot tablet and Colour S22 launched the accepted Preview. Screenshots, frames and the local Earshot report are in `app/build/review-evidence/rv1/`.

- **2026-11-08 Claude — Batch T: Codex reviews Codex.** Owner: Claude is out of usage; a Codex review thread
  may merge into `main` locally (no push) and use the tablet + S22. Wrote RV1 (V7.6, V7.7, R12, R16, R17), RV2
  (R15, S1, S2, S3, R13), RV3 (anime chain on `tsuzuki/r-a5`) and anime cards A4.3a, A6.1–A6.9; A5.1 now
  needs A4.3a. AGENTS.md and WORKPLAN §0.3 updated for the review lane.

- **2026-11-08 10:35 Claude — post-v0.1 merges: R11, HOME-SWIPE, R8, R10 into `main` via `review/post-v01`.**
  String files union-merged. R10 had removed the tracker token dialog's show/hide toggle and its pre-filled saved
  token (not in the card); Claude restored upstream's dialog, leaving only the theme dispatch in
  `SettingsTrackingScreen.kt`. `assembleDebug` + **968 tests, 0 failures**; `assemblePreview` installed on the S22
  (tablet disconnected): Home cover stack swipes to the next book (HOME-SWIPE); "next 5 chapters" of Mother of
  Learning downloaded through R8's scheduler (5 complete zips, no `_tmp` left; R8's pause/resume-while-in-flight
  checklist still to do on the tablet); Library settings show the four smart-update switches separately (R10);
  crash buffer empty. Also: signing key alias moved out of secrets (logs no longer show `***`).

- **2026-11-08 09:10 Claude — RC3 bug: chapter-end NEXT CHAPTER did nothing (owner screen recording).**
  Text reader (`NovelViewer`) only. After a fling into the chapter end, `NestedScrollView` still had an
  unsettled overscroll stretch, so each new touch "caught" it and the scroll view took the gesture: the
  card's Compose buttons never clicked, and the tap fell through to the menu toggle. Fix: finish the edge
  effects on ACTION_DOWN before the intercept check, and skip the menu toggle for taps that start on the
  card (`tsuzuki/reader/NovelChapterEndTouch.kt`; 17-line hook in `NovelViewer.kt`). Tablet `.dev`,
  Mother of Learning: before the fix, fling-to-end then tap → 0/2 loads; after → 3/3 loads, no menu flash,
  text taps still toggle the menu, crash buffer empty. Uncommitted on `main`; needs an RC4 before tagging.

- **2026-11-07 11:35 Claude — Codex review: A1.5, A2.2, Y3, Y4, Y5 merged (`c7c805cf2`); Y2, Y6 sent back.**
  `review/night4` = main + a1.5, a2.2, y3, y4, y5 (string files union-merged). **921 tests, 0 failures.**
  A2.2 gate met: line-by-line read of `36.sqm` (additive only, `ifnull` guard, seeded anime shelf `_id 0`),
  kotlin-reviewer agent "safe to merge" (low items moved to A2.4), and a **scratch upgrade test** on the tablet:
  main build installed as `.scratch` (temporary uncommitted suffix), today's preview backup restored (4 novels,
  1 manga, updates list), then the A2.2 build installed over it → library counts and the Updates list identical,
  no crash, no SQLite errors; `.scratch` uninstalled, scratch worktrees removed. Merged build installed as preview on
  tablet + S22: Y3 webtoon tile + Browse › Manga (Local manga pinned, Extensions · 7) fine, no crashes. A1.5 is
  debug-build-only (no non-owner debug install available) → merged on review + tests, device test pending. Y2
  (PagerViewer rewrite) and Y6 (listen panel inlined in ReaderActivity) back to `ready` with rework notes. A2.3 /
  A2.4 unblocked. Follow-up: details action tiles wrap mid-word at 4 tiles ("TRACKIN / G").

- **2026-11-07 08:40 Claude — Q13 + Q14 done (owner: Codex on anime, Claude fixes).** `54717865f` Q13: new
  `tsuzuki/reader/NovelNextChapterWarmup.kt` called from `ReaderViewModel.updateNovelProgressPercent` (infinite
  scroll off): past 75 % it prepares the next chapter and loads its text; S22 recording: Next ~0.1 s (was ~1.2 s
  blank). Found + fixed: entering a chapter whose text is already loaded kept the previous chapter's 100 % in the
  bar (`NovelViewer.restoreProgress` now reports the start/restored progress). `aa9d5544a` Q14: font tiles keep the
  face's own name (Literata fits), All reader settings tabs in one row (labelSmall), one-line title, paragraph
  indent/spacing in em (`formatReaderEm`), mixed Library grid selection per board (ring + check, others dimmed,
  no tint — the All view uses `TsuzukiMixedLibraryGrid`, which Q9 missed), selection-bar labels white, one line
  (`MARK READ`), slider titles in Dot case (`PreferenceItem.kt`), grouped tiles round first/last *shown* rows
  (`PreferenceScreen.kt`; hidden disabled rows left Translation › General with square corners). 906 tests, 0
  failures; checked on S22 + tablet, no crashes. Q14 item 6 (wider tablet margins / max line length) deferred:
  needs owner decision + layout work in TextView and WebView.

- **2026-11-07 07:25 Claude — cut letters at the page edge fixed (`9ed457af5`).** Owner screenshot (Lord of the
  Mysteries ch. 40, Sepia, Literata, justify): the "f" of "patrol of" lost its tip. TextView clips text to its
  padded area; overhanging glyphs at the end of justified lines were cut. Fix: transparent zero-offset shadow
  (`tsuzuki/reader/NovelGlyphOverhang.kt`, one call in `NovelViewer.applyTextViewStyles`), which widens the clip.
  Tested by adding Lord of the Mysteries from **AllNovelFull** on the tablet (TextView, Sepia, justify): italic
  "off!" at the right edge now whole. Same test showed that the earlier blank-line removal left **no** paragraph
  gap when paragraph spacing is 0 (tablet's setting, `<br><br>` source) → blank lines are now removed only when
  spacing > 0. Tablet preview library now has LotM (AllNovelFull) opened at ch. 1 (test data, not favourited).

- **2026-11-07 06:52 Claude — morning report, overnight batch Q.** **Done + merged into main (pushed):**
  Q1 `7aad35150`; Q2, Q3, Q4, Q5, Q6, Q7, Q12 via `review/night1` `52ac6a536`; Q8, Q9, Q11, X1, X3, X4 via
  `review/night2` `6af172281`; Q10 docs; Claude fixes Browse pills `277130bd3`, paragraph gaps `8582f16a5`, Q2
  labels `86c2ee50d`. X2 dropped (already built). **In progress:** A1.5 anime test screen (Codex). **Queued
  next:** Q13 next-chapter prefetch, Q14 design fixes, then A2.2–A2.4, Y1–Y6. **Nothing blocked.** **Checked on
  devices:** preview build only (never `.dev`), backups first (`app/build/night1-evidence/`), tablet
  `R52Y90HA77L` (Dot rail, Colour Browse, Appearance, translation settings, reader) and S22 `RZCWA139X1X` (Dot:
  nav, Library, Browse + recording, details, save sheet, reader menu/quick/full settings, paragraphs, next-chapter
  timing, selection); crash buffers empty after every install. Owner phone `R5GL23ZYE1V` not connected (Q1
  gfxinfo/recording still owed there). Deleted origin drafts q1-motion, q1-nav, q1-nav-fix, q1-review, q5-more,
  q5-settings (all files covered by q1/q5 in main). **Open questions for the owner:** where do letters look cut
  at the page edge (device/book/renderer, screenshot)? Q4 duplicate-Default migration still unproven on real data
  (run on your phone after an in-app backup). Gradle user-file memory cap applies to all your Gradle projects.

- **2026-11-07 06:20–06:55 Claude — owner awake: memory fix, review/night2 merged, three reader/Browse fixes.**
  Builds were being killed because `~/.gradle/gradle.properties` set `org.gradle.jvmargs` without `-Xmx` (it
  overrides the project's `-Xmx4g`), so the Gradle and Kotlin daemons each took ~8 GB (15.4 GB together). Owner
  approved: user file now `-Xmx4g` + `kotlin.daemon.jvmargs=-Xmx3g` (backup `gradle.properties.bak-20261107`),
  daemons stopped (free RAM 3.6 → 19 GB; new daemons confirmed at 4 g / 3 g). Fixes on main: **Browse pills
  flicker** (header followed `currentPage` while the pager slid Novel sources → Manga sources → Novel extensions;
  now follows `targetPage` and skips jump, `277130bd3`, `TsuzukiPagerHeader.kt`, test); **paragraph gaps** (legacy
  `Html.fromHtml` adds an empty line per paragraph that also got the spacing span; blank lines removed after
  parsing, `8582f16a5`, `tsuzuki/reader/NovelBlankLines.kt`, one-line hook in `NovelTextRenderer.kt`, 4 tests).
  `review/night2` (Q8, Q9, Q11, X1, X3, X4 + fixes) built: **901 tests, 0 failures**; preview on tablet + S22,
  checked Browse (pills steady, recorded), reader paragraphs, reader row labels TOP/TRANSLATE/SCROLL/LISTEN/MORE,
  quick sheet, All reader settings, Q9 selection bar, `131 CHAPTERS`, X3 translation (API key hidden), crash
  buffers empty → main fast-forwarded to `6af172281`. Design bugs found → **Q14**; next-chapter lag measured
  (~1.2 s blank page, no prefetch on the button path) → **Q13**. "Letters half cut at the page edges" not
  reproduced on S22 (margins 48/55 px) or tablet (left margin ~6 dp, tight but not cut) — asked owner where.

- **2026-11-07 05:47 Claude — overnight tick 3: Q8, Q9, Q11, X1, X3, X4 merged on `review/night2`, not on main.**
  All six merged onto main in `.worktrees/rv2` (string files: union; real conflict Q8 × Q9 in `TsuzukiNav.kt`
  and `TsuzukiHomeScreen.kt` resolved by keeping Q8's narrow/large-font layout with Q9's `t.mutedText` contrast
  colour). Diff scan clean (only hex is the `TsuzukiTokens.mutedText` definition; no schema/telemetry files). X1
  read: faithful move of ReaderActivity/ViewModel blocks into `presentation/tsuzuki/reader/`. **Build not run:**
  the preview build + tests (`--max-workers=2`) were stopped by the system for low memory a second time while
  Codex works on A1.5. **Morning, with Codex idle:** build `review/night2` (`:app:assemblePreview
  :app:testDebugUnitTest`), device-check Q11 quick sheet + full settings, Q9 selection/undo/Updates loading/
  Advanced, Q8 at font scale 1.3/2.0 on the S22, X1 reader open/close + next/prev + contents jump, X3
  translation settings (keys hidden), X4 migration screens; then fast-forward main. Upstream files to add to
  AGENTS.md then: Q9's 21 (AppBar, HistoryScreen/Item/Tab, CommonMangaItem, LibraryToolbar, MangaScreen ×2,
  MangaBottomActionMenu, MangaChapterListItem, MangaToolbar, PreferenceScaffold, PreferenceScreen,
  UpdatesScreen/UiItem/Tab/ViewModel, LibraryTab/ViewModel, NovelsTab, MangaViewModel), Q8 `ChapterHeader.kt`,
  Q11 `NovelPage.kt`, `ReaderSettingsDialog.kt`, presentation-core `SettingsItems.kt`, X3 `Preference.kt`,
  `PreferenceItem.kt`, `SettingsTranslationScreen.kt`, `EditTextPreferenceWidget.kt`, X4
  `MigrationConfigScreen.kt`, `MigrationConfigScreenSheet.kt`.

- **2026-11-07 03:47 Claude — overnight tick 2: Q11 reviewed, not merged yet.** Diff review OK (tokens/typography;
  `fontFamily` only for the live preview of the reader's chosen font; `SettingsItems.kt` change is an opt-in
  `LocalStackedSettingsLabel`, default layout unchanged; `ReaderSettingsDialog.kt` dispatch). Merged cleanly onto
  main in `.worktrees/rv2` (`review/night2`), but the preview build + tests were stopped by the system for low
  memory (Codex building Q9 at the same time). Q11 stays `review`; build, device check (quick sheet + full page,
  Dot + Colour, phone + tablet) and merge at the next tick or in the morning.

- **2026-11-07 01:47–02:05 Claude — overnight tick 1: Q2, Q3, Q4, Q5, Q6, Q7, Q12 merged.** Merged the seven
  `review` branches in table order into `review/night1` (only string-file conflicts: each card appended strings;
  resolved as a union, XML valid). `:app:assemblePreview :app:testDebugUnitTest` green, **888 tests, 0 failures**.
  Backups first: preview `.tachibk` from both devices in `app/build/night1-evidence/`. Installed the preview build
  (never `.dev`) on tablet + S22; checked Dot: Q5 nav on phone pill + tablet rail, More has HISTORY tile; Q7 Browse
  NOVELS/MANGA + SOURCES/EXTENSIONS, full-width names; Q12 Appearance only Colour + Dot, accents, covers switch;
  Q6 details toolbar back/download/filter/more, no N/A tile, genres wrap `+7`; Q3 From/To chapter fields,
  shortcuts, `SAVE CH. 0–24`; Q2 fixed ‹ › around progress. **Fixed on review:** Q2 action labels wrapped into
  each other at 360 dp (SCROLL TO TOP over TRANSLATE) → one line + ellipsis (`86c2ee50d`); proper short labels
  are left to Q8. Q4: preview data had no duplicate Default left; migration ran, one shelf, no crash. Tablet
  Colour Browse checked, back to Dot. Crash buffers empty. AGENTS.md: theme rule replaced (Q12 text) + upstream
  touch list. Follow-ups for Q8: More tiles "DATA AND STOR…", "DOWNLOADED ON…"; details "131 chapters" heading
  not uppercased in Dot; reader action short labels.

- **2026-11-06 23:50 Claude — Q1 merged, Q5 rebased, anime + wave Y planned.** Q1 reviewed (diff, 862 tests,
  tablet Dot rail + S22 Colour pill: indicator slides, dot grid ok, no crashes) → merged `7aad35150`; also removed
  the unused `tsuzuki_standard_steps` interpolator. `tsuzuki/q5` rebased onto it (`6612e1f1b`, Q1 part dropped
  cleanly). Owner answered the anime schema questions (ANIME_SCHEMA §9: separate anime shelves, import-only
  Aniyomi, seasons kept inert, separate watch stats, no fansub filter; Claude may merge migration 36 if all tests
  pass; test repos yuzono + Secozzi). Added anime lane A1.5–A2.4 and wave Y1–Y6; X2 dropped (already built).

- **2026-11-06 evening Claude — overnight plan.** Back on the desktop (JDK 21 = AS jbr). Owner approved for
  tonight: Codex commits on card branches; Claude reviews + merges Q1 now, then reviews/merges cards about every
  2 h, device-tests on tablet + S22 (in-app backup first; Q4 migration allowed after a backup), pushes main +
  card branches, deletes the superseded draft branches; extra wave X1–X4 after batch Q. Q10 done by Claude.

- **2026-11-06 Claude — owner switching computers; everything pushed.** Codex finished Q1 (`review`) and started Q5
  (paused) in uncommitted worktrees; each was committed as a `wip(...)` commit on its own branch and pushed:
  `tsuzuki/q1` (final Q1), `tsuzuki/q5` (Q1 + Q5 so far), and Codex's partial pieces `tsuzuki/q1-motion`,
  `q1-nav` (older drafts, superseded by q1), `q1-nav-fix`, `q1-review` (already in q1), `q5-more`, `q5-settings`
  (already in q5). Nothing merged into main. **Resume on the new computer:** `git fetch`, then
  `git -c core.longpaths=true worktree add .worktrees/q1 tsuzuki/q1` (same for q5), copy `local.properties`;
  Claude reviews `tsuzuki/q1`, Codex continues Q5 from `tsuzuki/q5`. The draft branches can be deleted after review.

- **2026-11-06 15:20 Claude — new machine set up + device pass → batch Q.** Installed Android Studio 2026.2.2,
  Temurin JDK 21, SDK 37.0/36, build-tools 37/36, NDK 29.0.14206865, platform-tools; `:app:compileDebugKotlin`
  green in 5 m 38 s. Owner's phone `R5GL23ZYE1V` (release build, Dot): reader bottom bar hides Next chapter
  off-screen, save-chapters custom range is an index slider, library shows two DEFAULT shelves (LNReader import),
  details/Browse/More text clipped, Dot motion stepped by design (frames fine: 1.2 % janky at 120 Hz). Boards for
  the fixes: `docs/design/boards/fixes-2026-11/` + canvas https://claude.ai/artifact/Qs9pMeZhphy77fbPiE9cTH.
  Cards Q1–Q12 written (Q11 reader settings, Q12 remove non-Tsuzuki themes, both added on owner request; Colour boards added); Owner approved Q5 (nav) the same day.

- **2026-11-06 06:30 Claude — batch N complete.** N20 merged (blocking storage lookups → IO: chapter content
  reader, DownloadPageLoader, `isChapterOnDisk` for skip-cache probes; 5 off-thread tests; novel + manga readers
  open fine on the tablet). N22b merged: mpv logging (`TsuzukiMpv` tag) + Aniyomi init/surface sequence + CA
  bundle → **anime stage 1 playback works** (debug build, local test video, hardware decode, audio). Build 846
  tests green, pushed, preview on both devices, tablet on Dot Home. Next anime step: A1.5 (install one real anime
  extension in the debug build and play an episode from it) — needs the owner to pick an extension/repo.
- **2026-11-06 06:00 Claude — morning review.** Batch N status: N1–N19, N21–N32 done (N22 player loads but renders
  black → follow-up **N22b**); **N20** (storage async migration, T4) not started; **N22b** ready. Codex did N1–N12
  (00:00–01:37) and then stayed at its usage limit; Claude agents did N13–N19, N21, N23–N32. Final `main` =
  `aab2aff6a`: debug + preview build, **841 unit tests green**, pushed; preview installed on tablet + phone, tablet on
  Dot Home, both crash buffers empty, extensions still load. Merged worktrees removed (only `anime-spike` left).
  Owner decisions waiting: anime schema questions (docs/ANIME_SCHEMA.md Q1–Q8); B2/B6 behaviour changes (duplicate
  dialog select-then-migrate, Upcoming per-day list); repo now carries ~30 MB of anime `.so` files in
  `anime-player/` (fine without LFS, but say if you'd rather use releases/LFS).
- **2026-11-06 02:30–03:55 Claude — Codex paused at its limit since 01:37; Claude took over cards.** Merged N13
  (repo source counts), N14 (global search count + time), N15 (storage breakdown; tablet shows real 1.7 GB /
  4 novels / 1 manga / 4,158 chapters), N16 (accent swatches + Reduce motion; Dot Appearance checked), N17 (skip
  update version + in-screen download; needs a public release to see), N18 (failed-to-load extensions; extensions
  and sources still load after cold start), N24 research (`docs/research/neural-tts.md`: GO-WITH-CONDITIONS,
  sherpa-onnx >= 2.0.1 has no espeak-ng). Build 766 tests green, pushed, installed on both devices. Claude hit its
  5-hour limit at ~03:55 (resets 04:30); N21/N23 agents stopped mid-way and are back to `ready`. Remaining: N19-N23,
  N25-N32. The 06:00 Claude review is still scheduled.
- **2026-11-06 00:00–02:30 Claude — overnight batch N1–N12 reviewed and merged as they landed.** N1/N2 Mihon fix
  groups A/B (webtoon stutter b5856135c skipped: needs Mihon's pixel-buffer reader pipeline), N3 novel status bar
  (Tsundoku `NovelStatusBar.kt` body moved into a `themed`-flag content function: watch on Tsundoku merges), N4
  manga reader chrome + settings (`ReaderTopBar.kt` actions extracted to a lambda, `ReadingModePage.kt` import
  aliases: watch on Mihon merges), N5 manga details mode tile + scanlator sheet, N6 vertical Tsuzuki slider, N7
  reader/translation settings (`StatusBarElementsScreen.kt` 77-line change: watch), N8 Mihon time format setting,
  N9 tablet Browse preview pane (Claude: only on windows ≥ 900 dp — portrait squeezed source names), N10 Compose
  manga download queue (safer subset reorder + tests), N11 generated covers (title added to `MangaCover`), N12
  per-shelf counts (read-only query). Each merge built green (now 739 tests), pushed, installed on both devices;
  tablet checks: manga reader chrome, manga details mode tile, Browse portrait list — no crashes. Also: Dot book
  opening motion made smooth (`0e6fd1732`), NanoHTTPD licence added. N13 claimed by Codex ~01:40 but no worktree
  yet; Codex session idle since 01:37 (likely usage limit) — heartbeat will resume it.
- **2026-11-06 00:xx Claude — anime A1.2 merged (`bb18e65a7`).** 26 animesource API files, NanoHTTPD 2.3.1
  (BSD-3, credit still to add), keep rule, unregistered loader, native builder + licences, ANIME_SPIKE.md. Spike UI/
  schema/player left for A1.3+/A2. Build 730 tests green. E2 range selection checked on tablet (4 selected, no crash).
- **2026-11-05 Claude — E1 + E2 Mihon fixes merged (`8cafe3b13`, `32f4570c3`).** E1: 88d4071cc (disk index
  can't overwrite a startup rescan, atomic index file), f28b2d887 (cancel queued downloads of chapters the source
  removed), f82a7a68f (fresh error notifications); f0f1e4de5 + a5d584eeb already in via the DownloadProvider sync,
  Mihon's `DownloadProviderTest` added. E2: c279291e2 (tablet FAB above the taskbar), a6ee4bc59 (library range
  selection across categories; pure helper + 6 tests). Build 730 tests green; preview on both devices; pushed.
  Overnight Codex batch N1–N8 queued; Claude review scheduled 06:00 2026-11-06.
- **2026-11-05 Claude — D1 tablet Updates + device passes.** D1 (`2e0451bb4` merge): on tablet UI (sw ≥ 720 dp)
  Updates shows list + preview; tablet portrait Dot checked (row select switches preview without opening the
  reader). Added a confirm dialog before a group preview downloads > 25 chapters (Shadow Slave group had 1,911;
  `93f36e6cd`). Device-checked: C4 replacement rules section + add dialog (tablet Dot, cancelled), B1 filter sheet
  (Filter label was thin on red → bold, `onAccent`). Builds 708 tests green. Still unchecked on device: C1, C3,
  C5 (see entry below), B2–B6 screens.
- **2026-11-05 Claude — C3, C4, C5 merged (Sonnet agents, reviewed by Claude).** C3 custom source setup +
  site-picker chrome (WebView untouched); C4 replacement rules (same JSON pref, live test line, regex validation,
  accent delete); C5 TTS mini player (accent play, 48 dp targets; no timer/speed/progress — no data). Build 706
  tests green, preview installed on both devices. **Not yet seen on a device:** C1 migration list (needs a real
  migration run), C3 setup + picker (Browse → custom source), C4 (novel reader → All settings → Advanced), C5 (needs
  "TTS controls visible" on, which is off in the preview copy). Do these in the next device pass.
- **2026-11-05 Claude — P0.6 + C1 + C2 merged and pushed.** P0.6 media-types onboarding step (`b0c041128`,
  `10dec7ac3`): Light novels fixed on, Manga toggles `hideMangaUi`; mixed-library pills hide in novel-only mode;
  phone Dot check found the stripe art spilling over the card (fixed with clipToBounds) and a wrapping Japanese
  title (shortened). C1 migration list (`4e742d0bd`): old/new tiles side by side, all row actions; not device-run
  (needs a real migration). C2 duplicate finder (`e0b70b185` merge): pills, group tiles, bottom action bar;
  tablet Dot idle + empty states checked, no crashes. Build 692 tests green. `main` pushed to origin (CI only;
  preview release workflow is manual). Restored the restyle card rules into WORKPLAN (they had been dropped from
  this file when the Codex queue was emptied).
- **2026-11-05 Claude — Codex P0.3 + B1–B6 reviewed and merged.** P0.3 (`ae1ecc6bd`, merge `5f2ff2a03`): five
  media-specific files moved to `tsuzuki/novel/` and `tsuzuki/manga/`, package/import lines only. B1–B6
  (merges `b2f863a4f`…`757642986`): catalogue filters, change-category + duplicate dialogs, edit info,
  tracking + tracker search, quote tiles + chapter search, EPUB export + Upcoming — each behind an early-return
  dispatch. Three Sonnet reviewers: all merge-with-fixes, no blockers; fixes applied (category selection uses
  plain `remember`, duplicate summary restored, tracker search list keys, EPUB filename labels use the stock
  `epub_include_*` keys, explicit translation-mode list, preview remember keys, pill index guard, muted match
  suffix). Known exception: quotes use Literata via a direct `FontFamily` (commented; no shared reader-face helper).
  `strings.xml` conflicts unioned. Onboarding device pass (tablet): welcome, languages (real list, current
  selection), look (Colour ↔ Dot recreates and stays on the step), storage, permissions, import tiles, backup
  handoff — all OK; tablet back on Dot Home, no crashes. Known pre-existing quirk: re-opening onboarding from
  Settings on the tablet two-pane layout and using a backup tile pushes a second Settings into the detail pane
  (stock onboarding does the same). Anime spike WIP committed on `codex/anime-spike` (`ad6442e1f`).
- **2026-11-05 Claude — reviewed Codex P0.4 + P0.5, merged (`af8155b6c`, `c9e648a3b`).** P0.4 (`ff407d59c`):
  transitions, contents drawer and ReadingDays moved to `presentation/tsuzuki/reader/TsuzukiReaderHooks.kt`;
  ReaderActivity/ReaderViewModel keep one-line calls; behaviour identical on review. P0.5 (`b4532cf5b`): six-step
  Tsuzuki onboarding (welcome → languages → look → storage → permissions → import), stock flow for legacy
  themes, 19 strings × 8 locales, 9 tests. Claude change in review: `StorageStep.kt` hook made purely additive
  (early-return block with its own picker/effect) instead of restructuring upstream lines. Device check: see
  next entry when done.
- **2026-11-05 Claude:** merged Tsundoku main (`c26dfc13a`, clean: TTS headset buttons, scroll actions into the
  overflow, faster title marquee, source memo fix, Based on Mihon link — our Tsuzuki About already credits Mihon
  under "Built on"). Added icons to the three new overflow scroll actions. Archived anime native artifact (LGPLv3
  FFmpeg, 16 KB aligned) and backed up anime spike WIP. Wrote WORKPLAN.md, this file; deleted old handoffs.
- **2026-11-05 Claude:** `8f7f12e3b` reader transition next-chapter download check runs on IO (Codex wrote it on
  2026-11-04; Claude finished): tablet Dot long strip 335→336 card rendered and recycled, no errors, crash buffer
  empty; data restored as noted above. Phone check open (P1.9).
- **2026-11-04 Codex (summary of the day):** WebView/Cloudflare robustness port (`2510f09f6`), source headers
  off the UI thread (`5e68ec54a`), Mihon installer sync with signing-key check (`2c7bcae9a`), legacy download
  names + shared batch lookup (`7636bd698`), novel renderer coverage corrected (`1f85f4fb3`), reader progress
  bars/typography/accessibility (`02b963e81`, `9b105a87a`, `74f39d534`, `2c24c5027`), profiles + splash cost
  (`b8b3938de`, `b68b3fc2c`), picks merge + split-image recovery (`c54b2f809`, `de061e0ef`). Evidence under
  `app/build/codex-*`; QA matrix in `docs/RUNTIME_QA.md`; measurements in `docs/PERFORMANCE.md`.
- **2026-11-03/04 Claude + Codex:** orders 1–10, 12–14 of the old queue (Tsuzuki UI across Home, Library,
  details, reader chrome, settings, browse, motion, translations, widget, menus). Details in git history of
  `AGENTS.md` (see top of this file) and `docs/design/README.md` status table.

- **2026-11-14 Codex review lane — PERF1 completed.** All four speed budgets measured on optimized RV5 Preview/S22 with Y4 synthetic data. Cold startup and settled details missed; PERF-START/PERF-DETAILS created without tuning. Library passed; local Native chapter-first-text limited pass near measurement uncertainty. See PERFORMANCE.md and local perf1 evidence. REL preparation next.

- **2026-11-14 Codex review lane — REL-0.1.1 preparation completed.** Merged accepted main into existing release prep; Preview/Release + 1,085 tests passed, 16 KB APK/ELF/profile audits passed. Exact tagged-v0.1.0 Debug → optimized 0.1.1 scratch upgrade retained all Y4 book/chapter/history/shelf state and exported settings. Both candidate Dot Homes inspected, empty crash buffers. Local artifacts use Android Debug signing; official signing/tag/publish and owner-phone install remain owner-only. Device lock released, both kept awake. No push/tag.

- **2026-11-14 final Case B audit:** archived Preview/Release/scratch SHA-256 hashes rechecked, successful build log and empty final crash logs verified. Tablet still on scratch candidate; `wm user-rotation` already reports `free`, so no device setting changed. Required owner-publish stop point remains. Evidence `app/build/review-evidence/rel-011/final-audit.json`.
