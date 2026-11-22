# Anime parity audit

Baseline `5226188d3` from `main`, with AP3 present. The ARM64 Debug build uses
`io.github.manojx2005.tsuzuki.ap0scratch` on the Galaxy Tab S11 and wireless Galaxy S22.
The scratch libraries start with three AY0 titles and three local manga titles. The source provides
three corresponding episode lists and stores real resume progress. Manga comparisons use the same
device and theme. The anime boards take precedence for media-specific layouts such as the Watch hero,
episode artwork and player.

## Evidence

The [tablet](design/boards/evidence/anime-parity/tablet/) and [S22](design/boards/evidence/anime-parity/s22/)
directories use the same names for matching Dot and Colour views.

| Surface | Compare |
|---|---|
| Home | `home-read`, `home-watch`, `home-anime-off` |
| Library | `library-all`, `library-manga-selection`, `library-anime-longpress` |
| Details | `details-*-filter`, `details-*-selection`, anime tracking |
| Updates and History | `updates-*`, `history-*` |
| Browse | `catalogue-*`, `extensions-*`, `migration-*` |
| Reader and Player | `reader-manga-controls`, `player-anime-controls` |
| Downloads and Settings | `queue-*`, `settings-*`, `backup-options` |
| More, Stats and widgets | `more-anime-off`, `stats-watch`, `widget-*` |

Both devices fetched AnimeKai (Unoriginal) 16.13 through yuzono in the scratch app. Its One Piece
search returned 1,180 episodes on each. The live title stayed outside the paired libraries.
Widgets were rendered at 300 × 300 dp using their Glance content with scratch history.
The local manga fixtures exercise the reader and catalogue. The book migration screen omits local
titles by design, so the manga destination and write operations were outside the paired migration check.
Tracking was checked signed out. Download notification behavior, repository parsing and dictionary
entry points also use code inspection. The cards carry the device acceptance cases for those changes.
The debug build passed, along with all 1,560 module tests and nine AY0 server tests.

On both devices, the backup contained three manga entries and three anime entries with anime enabled. The preview
reported three entries and three history records. With anime disabled, the written archive contained zero anime
entries. Restoring the enabled backup brought back the manga and anime favorites that the probe had removed.
The scratch package did not resolve an `aniyomi://add-repo` intent on either device.

## Differences

| Surface | Item | Manga | Anime | Card |
|---|---|---|---|---|
| Home | Read / Watch switching | Read is always available | The entire switch disappears when anime is off | AP1 |
| Home | Activity tiles | Read presents today’s activity and recent updates. Tablet also shows library and history totals | Watch presents resume, airing or updates when available and a library strip, with no corresponding activity tiles | AP-14 |
| Settings | Discoverability | Manga reader settings are linked from Settings | The anime feature switch is nested under Browse | AP1 |
| More | Discoverability | Reading features are already reachable | There is no anime entry when the feature is off | AP1 |
| Onboarding | Feature choice | Reading starts enabled | There is no choice to enable watching | AP1 |
| Browse › Extension stores | Add-repo links | The book repository flow has an inbound link route | Aniyomi and Animetail links have no anime repository route | AP2 |
| Browse › Extension stores | URL input | Book repository submission reports invalid input | Anime rejects common folder and GitHub navigation URLs and shows a generic invalid-repository message | AP2 |
| Library | Selection entry | Long-press selects a cover or row | Long-press on an Anime grid cover opens details on both devices. Anime covers in All have no selection action | AP5 |
| Library | Selection toolbar | Count, cancel, select all and invert are available | Anime cover selection has no toolbar | AP5 |
| Library | Selection actions | Batch shelves, reading status, downloads, removal and migration | Anime offers no library batch actions | AP5 |
| Library | Mixed selection | Book types on All share selectable items | Anime cannot join a book selection on All | AP5 |
| Library | Display mode | Grid, list and compact configurations use library display preferences | Anime uses a fixed three-column phone grid or an adaptive tablet grid | AP8 |
| Library | Sorting | The library sheet has sort fields and direction | The Anime filter icon opens shelf management | AP8 |
| Library | Filtering | The library sheet exposes status and download filters | Anime has title search and global downloaded-only support, with no anime filter sheet | AP8 |
| Library | Badges | Book thumbnails support unread, download and source-language badges | Anime library covers show title and artwork without those badges | AP8 |
| Library | Shelf tools | The toolbar offers update category and random title | Anime has shelf pills and counts when multiple shelves exist, with no equivalent toolbar actions | AP8 |
| Library | Shelf organization | Book categories appear in the shared library pager | Anime uses its own shelf pager and separate filter-icon destination | AP8 |
| Updates | Row actions | Book rows support long-press selection | Anime rows open playback and do not register a selection action | AP9 |
| Updates | Batch actions | Selection toolbar and bottom bar expose status and download actions | Anime updates have no equivalent batch controls | AP9 |
| Updates | Swipe | Book update rows expose chapter swipe actions | Anime rows supply a play click only | AP9 |
| Updates | Row layout | The book row combines cover, chapter metadata, progress state and download indicator | Anime has its own title and play row | AP9 |
| Updates | New badge | Book rows show a New badge | Anime shows a page-level count only | AP9 |
| Updates | Save all | Book updates offer a header action to download every unread update | Anime shows the update count and library refresh without Save all | AP9 |
| Updates | View and filtering | Book updates expose list mode and filters | The Anime toolbar exposes only library refresh | AP9 |
| Updates | Tablet preview | Book updates show selected details and actions in a second pane | Anime updates remain a single list | AP9 |
| History | Row layout | Book rows use the shared cover, resume and removal controls | Anime renders its own row with elapsed episode time and remove control | AP9 |
| History | Clear filtered history | The Manga view exposes clear history | The Anime view hides clear history. The shared All view can clear anime as part of clearing everything | AP9 |
| History | List mode | Manga offers the List View action | Anime has no matching layout action | AP9 |
| History | Activity summary | Manga shows time today, chapter count and streak | The Anime filter shows the dated history list without activity tiles | AP9 |
| Browse › Catalogue | Display options | Manga supports display mode selection | Anime always uses an adaptive cover grid | AP10 |
| Browse › Catalogue | Add from results | Long-press can add to a shelf | Anime catalogue items handle a detail click only | AP10 |
| Browse › Catalogue | Library indicator | Manga can show library membership on a result | Anime catalogue results do not show the library badge | AP10 |
| Browse › Catalogue | WebView shortcut | Manga catalogue toolbar opens the source in WebView where supported | Anime catalogue toolbar has source settings where supported, without a WebView shortcut | AP10 |
| Browse › Catalogue | Filter placement | Manga exposes its filter action from catalogue chrome | Anime exposes its filter only in Search | AP10 |
| Browse › Sources | Search entry point | The Manga Browse toolbar launches global search | The Anime Browse toolbar searches source names. Anime can be searched from the app’s global search, but not directly from that toolbar | AP10 |
| Browse › Extensions | Details | Installed book extensions open a details page | Anime displays grouped rows with inline install/update/trust/uninstall actions but no details destination | AP-15 |
| Browse › Extensions | No repository configured | The book Extensions tab remains reachable before adding a repository | The Anime Extensions tab is hidden with no configured repository, even when a privately installed source is already present | AP-18 |
| Details | Header layout | Shared `MangaInfoHeader` aligns cover, metadata and title actions | Anime uses a separate header and cover sizes | AP7 |
| Details | Cover actions | Cover detail opens a larger image with save/share | Anime shows a static cover | AP7 |
| Details | Action row | In-library, Tracking, WebView and conditional next-update actions use shared layout | Anime uses its own action row with a dedicated Save action and no next-airing action | AP7 |
| Details | Notes | Book overflow links to title notes | Anime overflow has no notes destination | AP7 |
| Details | Edit metadata | Book details expose metadata and cover editing | Anime overflow does not expose either edit action | AP7 |
| Details | Download presets | Manga exposes a download menu with ranges and presets | Anime Save selects eligible episodes and opens the selection overflow | AP6 |
| Details › Episodes | Selection bar | Chapters use a toolbar with select all, invert and close | Episodes have a count and overflow with select all, but no invert action | AP6 |
| Details › Episodes | Range selection | A chapter range can be selected by long-press | Anime long-press toggles an individual row | AP6 |
| Details › Episodes | Bottom actions | Chapter selection exposes read/bookmark/download/delete actions in a bottom bar | Anime places selected actions in the overflow and lacks a batch delete-download action | AP6 |
| Details › Episodes | Swipe | Chapters follow the configured swipe actions | Anime episode rows do not handle swipes | AP6 |
| Details › Episodes | Filter states | Chapter settings use the shared filter controls | Anime has a separate filter sheet and simple checkbox states | AP6 |
| Details › Episodes | Downloaded filter | Manga offers a downloaded status filter | Anime lists unwatched and bookmarked only on both devices | AP6 |
| Details › Episodes | Sort modes | Chapters can sort by source, number and upload date | Anime sheet offers number/date and direction | AP6 |
| Details › Episodes | Display modes | Chapters can display titles and numbers using the display settings | Anime has no equivalent title/number display preference | AP6 |
| Details › Episodes | Mark until last opened | Book details can mark chapters through the last read chapter | Anime has a per-episode mark-previous action without a last-watched entry point | AP6 |
| Player | Settings layout | Reader settings use the app’s standard sheets, groups and controls | Anime has video-specific sheets and a separate card-based Settings screen | AP11 |
| Downloads | Progress notification | Manga shows current item and moving progress | Anime foreground notification is static while queue rows receive progress | AP4 |
| Downloads | Progress actions | Manga notification exposes queue actions | Anime notification lacks pause and cancel | AP4 |
| Downloads | Failure and completion | Manga provides separate failure/retry and completion feedback | Anime reports failure in queue rows and leaves the foreground notification generic | AP4 |
| Downloads | Wi-Fi wait | The book flow exposes the constraint in Settings | Anime defaults to Wi-Fi only and can wait with generic download messaging | AP4 |
| Downloads | Queue rows | Manga uses the shared queue row and progress layout | Anime uses separate grouped rows with progress and per-row retry/remove | AP11 |
| Downloads | Queue management | Book queue supports reorder and queue-wide controls | Anime has per-row controls but no matching reorder, pause/resume-all or clear-failed actions | AP11 |
| Settings › Downloads | Anime options | Books show Wi-Fi, cleanup and concurrency preferences | Anime Wi-Fi is in the queue, with no equivalent anime settings section for cleanup or concurrency | AP11 |
| Settings › Dictionaries | Import offline pack | A local backup or book can be selected from storage in the relevant entry point | Dictionaries lists packs without a local import action. This is a reader feature called out by the owner | AP12 |
| Stats | Period chart | Reading has a week, month and year activity chart | Anime has watch-time totals and episode counts without a period chart | AP-21 |
| Settings › Data and storage | Media counts | The overview shows favorite manga and chapter counts | The overview omits favorite anime and episode counts | AP-19 |
| Settings › Data and storage | Download usage | The overview measures book downloads | Anime saves to its own directory and exposes bytes in the queue, outside the storage overview | AP-19 |
| Widget | Larger sizes | Continue Reading can show activity and goal context at larger sizes | Continue Watching shows more resume rows, without equivalent activity information | AP-16 |
| Backup / restore | Media data while feature is hidden | Library export and restore can select book media independently of navigation visibility | Anime content export and restore check the current feature switch in addition to the backup options | AP-17 |
| Backup / restore | Preview counts | The restore summary counts manga entries, shelves and history | The validator reads anime backup fields for missing-source and tracker checks but omits anime entries, shelves and history from its preview counts | AP-20 |

## AP13 follow-up cards

- **AP-14 — Watch activity tiles.** Add the AnimeHome board’s watched-today and new-episode tiles using stored
  watch activity and episode updates. Keep the anime-specific hero layout.
- **AP-15 — Anime extension details.** Open an installed extension details screen with source list, package and
  version metadata, and the extension’s current trust/update/uninstall actions.
- **AP-16 — Continue Watching widget.** Compare the large widget with Continue Reading and add real watch activity
  where the larger layout has room, while retaining the existing resume behavior.
- **AP-17 — Anime backup while hidden.** Verify the populated scratch round trip, including anime off at export
  and import. Preserve backed-up anime entries and make content inclusion an explicit choice when possible.
- **AP-18 — Anime Extensions with no repository.** Make Browse › Anime › Extensions reachable before repository
  setup, with a link to add a repository and any privately installed extensions already visible.
- **AP-19 — Anime in Data and storage.** Include saved-episode bytes and real anime and episode counts in the
  storage overview using the existing asynchronous measurement flow.
- **AP-20 — Anime restore preview counts.** Show anime entries, shelves and history stored in a backup, including
  an anime-only backup. Preserve the existing book counts and source and tracker checks.
- **AP-21 — Watch activity chart.** Add a week, month and year chart for the app’s recorded watch activity using
  the existing reading-chart design. Keep the existing watch totals.

## Existing behavior to preserve

- AP3 offline playback is already on this baseline. Its original fixture and live HLS results are in the handoff.
- Episode rows already have artwork, resume progress, individual download state and single-item status menus.
- Anime has history search, single-entry removal, clear-all integration, and resume. Anime also participates in
  global search, offers pinned sources and source settings where supported, and has a migration screen.
- Backup interfaces already offer an Anime option and the code includes anime library, episode, shelf, track,
  history and preference handling. Verify the scratch round trip before treating backup as a gap.
