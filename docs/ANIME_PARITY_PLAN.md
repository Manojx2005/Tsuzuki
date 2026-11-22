# Anime parity plan (Batch AP, owner 2026-11-22)

Owner's report (2026-11-22, after v0.2.0 shipped), quoted in short:
- the anime switch is hard to find, and Read / Watch on Home is not there;
- the anime tabs look different from the novel and manga tabs and miss many features: **every feature the novel
  and manga tabs have must exist for anime too**;
- adding extensions from Aniyomi links does not work;
- downloaded episodes do not play; the download progress bar in the notification does not move;
- nothing can be done with selected anime in the Library (no delete or other actions);
- there is no way to add an offline dictionary;
- test every tab that has anime, compare it with the same tab for manga/manhwa, and fix every feature and design
  difference; check every screen for anime gaps and broken designs.

Goal: in **0.2.2** (0.2.1 is the Kokoro release already in progress) anime behaves and looks like the book tabs.
The reference for "the same" is always the **manga** screen in the same theme (Dot first, then Colour), then the
novel one where manga has nothing. Boards: `docs/design/boards/png/10-anime/` (anime-specific) win where they
exist; otherwise the matching book board (03-home, 04-browse, 05-library, 06-details …) applies as is.

## How the work is split

| Lane | Who | Device use |
|---|---|---|
| Audit and device bugs (AP0, AP3, AP4) | Claude | tablet + S22, Device lock |
| UI parity cards (AP5–AP11) | Codex app lane, or Claude | anime-lane rule: Codex may test its own card on the tablet/S22 under the Device lock |
| Small fixes (AP1, AP2, AP12) | either | yes |
| Review | the other lane; while Codex has no usage, Claude self-reviews (owner waiver 2026-11-22, mark it) | yes |

Rules for every card: branch `tsuzuki/<card>` from `main`; Dot first, then Colour; phone (S22) **and** tablet; the
AY0 fixture server (`scripts/anime-fixture/`) for deterministic data, a live yuzono extension only on a scratch
app; Japanese device strings; 8 locales for every new string; tests for logic; no change to the book screens'
behaviour unless the card says so. Prefer **reusing the book components** (`TsuzukiPill`, `tsuzukiGroupedTile`,
`TsuzukiSectionLabel`, library/updates/history row composables, `SelectionToolbar`, bottom action menu) over
writing anime copies; when a book screen is upstream code, add a small dispatch as AGENTS.md says.

## AP0 Parity audit (Claude, devices) — first

Build `main` as scratch Debug, AY0 fixture + one live yuzono extension, a manga library with the same number of
titles. For every surface below, put the anime screen next to the manga one (same theme, same device) and record
each difference as a row: *surface · feature or design item · manga behaviour · anime behaviour · card*.
Output: `docs/ANIME_PARITY_AUDIT.md` (the matrix + screenshots under `docs/design/boards/evidence/anime-parity/`).
Every row must map to a card below or to a new `AP-x` card. Surfaces:
Home (Read/Watch, continue, stats tiles) · Library (all/anime pills, grid/list/compact, sort, filter, badges,
search, categories tabs, selection + bottom bar, update category, random) · Updates (rows, selection, mark,
download, swipe, update button) · History (rows, delete one/all, resume, search) · Browse (sources, extensions,
catalogue grid/list, filters, long-press add, global search, migrate) · Details (header, action row, description,
genres, episode list, filter/sort/display sheet, selection + bottom bar, download menu, tracking, share, webview,
notes, edit/cover, migrate, duplicates) · Player (sheets, settings) · Downloads (queue screen, notification) ·
Settings (anime switch, player, downloads, tracking, backup) · More/Stats · widgets · backup/restore.

## AP1 Anime is easy to turn on; Read / Watch always on Home (T2)

- Settings top level gets an **Anime (beta)** row (switch + one-line description) near Browse; keep the existing
  Settings › Browse entry. More screen: an "Anime" row when off (opens that setting).
- Home: the Read / Watch switch is shown **even when anime is off**; tapping Watch while off shows a small sheet
  (board style, `TsuzukiAdaptiveSheet`): what anime is, "Turn on" (sets the preference) and "Not now". After
  turning on, Watch opens directly.
- Onboarding: one optional "Also watch anime?" toggle on the last step (default off).
- Files: `SettingsMainScreen.kt` (one row via existing Tsuzuki hook), `presentation/tsuzuki/home/TsuzukiHomeScreen.kt`
  (`MediaSwitch` gate), `presentation/tsuzuki/more/*`, onboarding step. Strings in 8 locales.
- Accept: fresh install, anime off → Home shows Read/Watch; Watch → sheet → Turn on → Watch home; Settings
  top-level row toggles it; book-only users see nothing else change.

## AP2 Aniyomi repo links work (T2)

- Manifest: `aniyomi://add-repo?url=…` (and `animetail://add-repo`, `tachiyomi`-style `?url=` on the anime host)
  → open `AnimeExtensionReposScreen` with the URL pre-filled and a confirm dialog (never add silently).
- `AnimeRepoIndexParser.indexUrl`: also accept a repo **folder** URL (`…/repo` or `…/repo/` → append
  `index.min.json`), `raw.githubusercontent.com/<o>/<r>/<branch>` without the file, GitHub `tree/<branch>` and
  `blob/<branch>/index.min.json` URLs, and `https://<host>/…/index.json` (Aniyomi's non-minified name). Reject
  everything non-HTTPS as today. Unit tests for each form, using real repo URLs as fixtures (yuzono, Secozzi,
  Kohi-den style).
- Repos screen: show the parse error under the field ("Not an Aniyomi repo link: …") instead of a silent no-op.
- Files: `AndroidManifest.xml`, `MainActivity.kt` (one dispatch), `tsuzuki/anime/extension/AnimeRepoIndexParser.kt`,
  `presentation/tsuzuki/browse/AnimeExtensionReposScreen.kt`.

## AP3 Downloaded episodes play (T3, Claude first: reproduce)

Code reading: `AnimeDownloads.offlineVideo` returns `Video(videoUrl = absolutePath)` from
`getExternalFilesDir(DOWNLOADS)/anime/<source>/<anime>/<episode>/{video,playlist}`; the player calls it in
`openEpisode` before the source. Reproduce on the tablet and S22 with the fixture (mp4 case 01 and HLS case 04):
download, airplane mode, play from details, from Updates, from History, from Library "continue". Suspects to check:
the HLS playlist's segment paths after download (`OfflineHls.kt`), `queueVideo` → subtitle preparation dropping a
non-HTTP URL, the AY1 watchdog, the source lookup failing first when the extension is missing/offline
(`ensureLoaded()` before `offlineVideo`), and R8 in Release. Fix, then a regression test per cause and a device
script that plays both formats offline in Dot and Colour.

## AP4 Download notification shows real progress (T2)

`AnimeDownloadJob.getForegroundInfo` posts a static "Downloading" notification; `AnimeDownloads.progress` is never
pushed to it. Match the manga downloader notification: title = anime + episode, determinate progress (bytes or HLS
segments), "n of m" queue count, Pause and Cancel actions, error notification with Retry, completion summary.
Throttle updates (≤ 2/s). Files: `tsuzuki/anime/download/AnimeDownloadJob.kt`, a new
`tsuzuki/anime/download/AnimeDownloadNotifier.kt`, `NotificationReceiver.kt` (actions, small hook). Accept: the bar
moves on both devices for mp4 and HLS; pause/cancel from the notification work; no notification spam.

## AP5 Library selection for anime (T3)

Long-press an anime in Library (Anime pill and the All page section) enters the **same selection mode as books**:
selection toolbar (count, select all, invert, close) and the bottom action bar with: change shelves, mark all
watched / unwatched, download (next 1/5/10/unwatched/all), delete downloads, remove from library, migrate.
Mixed selection (books + anime on All) shows only the actions both support. Reuse `LibraryContent` selection
state through a Tsuzuki dispatch; anime actions call `AnimeLibraryStore` / `AnimeDownloads`.
Accept: every action works on 1 and on several anime, Undo where books have it, Back clears selection.

## AP6 Episode list = chapter list (T3)

Details episode list matches the manga chapter list: long-press select, range select, select all/invert, bottom
bar (mark watched/unwatched, mark previous watched, bookmark, download, delete download), swipe actions with the
same settings as chapters, per-row download state, filter/sort/display sheet with the same tabs (Filter: unwatched,
downloaded, bookmarked; Sort: number/upload date/source; Display: title/number), "Mark until last watched".
Files: `tsuzuki/anime/ui/AnimeDetailsScreen.kt`, `AnimeEpisodeTiles.kt`, `AnimeDetailsScreenModel.kt`, store calls.

## AP7 Details header and actions = manga details (T3)

Header layout, cover tap (full cover, save/share), action row (In library, Soon/next airing, Tracking, WebView),
expandable description with genre pills, share, open in browser, notes, edit info/cover, migrate, duplicates,
"opens as" equivalent (player default per anime: none needed unless the audit finds a gap). Match
`MangaInfoHeader` Tsuzuki layout pixel for pixel in both themes (boards 06-details + 10-anime/AnimeDetails).

## AP8 Library tab = book library (T3)

Grid / list / compact list display modes and column count, sort (title, last watched, latest episode, unwatched
count, date added, total episodes) and filter (downloaded, unwatched, started, completed, tracked) in the same
sheet design, unwatched/downloaded/language badges, search, shelf tabs with counts, "update shelf", random pick,
empty states. Settings › Library options that books have apply to anime where they make sense.

## AP9 Updates and History = books (T2)

Updates: same row design, selection + bottom bar (mark watched, download, delete), swipe actions, update button,
"new" badges, date headers. History: same row, delete one / clear all with confirm, resume, search, date headers.

## AP10 Browse catalogue = book catalogue (T2)

Catalogue grid/list toggle (same as BrowseSource), long-press to add to library (shelf picker), "in library"
badge, global search over anime sources from the Browse search, open in WebView, source settings from the toolbar,
pinned sources and "latest" shortcuts (done in the redesign; verify), migrate entry point.

## AP11 Downloads queue and player settings (T2)

Download queue screen: anime rows look like manga rows (progress bar, reorder, cancel, pause/resume all, clear
failed). Settings › Downloads: anime download options (Wi-Fi only, delete after watched, parallel) next to the book
ones. Player settings page matches Settings design (sections, pills).

## AP12 Offline dictionary can be added (T2)

Today Settings › Reader › Dictionaries lists packs but none are downloadable on `main` (bundled defaults +29 MB are
an open owner decision; the Japanese/English bundled packs live on the unmerged app chain). Add **"Import
dictionary file"** (SAF picker, the existing pack format `dictionary-<lang>.pack` + `.json`, validated before
replacing, same code path as the bundled packs) so a user can add one now; and ask the owner again whether to
bundle the two packs (+29 MB) in 0.2.2. Files: `presentation/tsuzuki/dictionary/DictionaryPacksScreen.kt`,
`tsuzuki/dictionary/DictionaryPacks.kt`.

## AP13 Everything else the audit finds (T?)

Stats (watch time next to read time), widgets, backup/restore (anime shelves, history, settings), tracking
screens, extension details page for anime (like manga's), Settings search. One card per real gap.

## Order

AP0 (audit) → AP3, AP4 (device bugs, Claude) in parallel with AP1, AP2, AP12 (small, `parallel-ok`) → AP5, AP6,
AP7, AP8 (one at a time per file set; AP5+AP8 share library files, so sequential) → AP9, AP10, AP11 → AP13 → **RV-AP**
review + 0.2.2 release (version bump, changelog, all-module tests, Release build, 16 KB, both devices).
