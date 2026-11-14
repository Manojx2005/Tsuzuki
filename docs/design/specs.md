# Board specs for the remaining work

The design lives on the private "Tsuzuki App Screens" canvas. All 16 pages were saved from the
owner's Vivaldi session on 2026-11-03: see [the offline index](boards/INDEX.md) for PNGs and HTML.
Open the relevant screenshot before changing UI. This file supplies written measurements and
implementation constraints for the tasks in `AGENTS.md`. Sizes are dp, text is sp.

If a task needs a board that isn't here, check `docs/design/boards/` (the owner can export boards there,
see the end of this file). If it isn't there either, ask the owner instead of inventing a layout.

## Shared rules (all boards)

| Token | Colour | Dot |
|---|---|---|
| Screen background | book palette (default `#1C2B4A`) | `#000000` + dot grid (`#1C1C1C` 1 dp dots, 12 dp pitch) |
| Tile | white 8% | `#0D0D0D` with 1 dp `#262626` border |
| Muted text | white 72% | `#8A8A8A` |
| Accent / on-accent | `#E9B44C` / `#1C2B4A` | `#FF2D2D` / `#FFFFFF` |
| Sheet background | `#16223D` | `#0B0B0B`, top border `#2A2A2A` |
| Divider ("line") | white 14% | `#2A2A2A` |
| Text case | as written | UPPERCASE |
| Display face | Instrument Serif italic | Doto 900 |
| Body face | Manrope | Space Mono |
| Covers | full colour | greyscale |

In code, read all of these from `TsuzukiTheme.tokens` and `MaterialTheme.typography`. Never hardcode them.

Building blocks already in the app (reuse them):

- `TsuzukiPill` / `TsuzukiPillRow` (`theme/tsuzuki/TsuzukiPill.kt`): selectable pills, 38 dp, accent when selected.
- `TsuzukiSectionLabel`: 11 sp, ExtraBold, 1 sp letter spacing, uppercase, muted.
- `tsuzukiGroupedTile(first, last)` (`presentation/tsuzuki/browse/TsuzukiBrowse.kt`): makes consecutive list rows one tile.
- `TsuzukiProgressBar`, `TsuzukiTile`, `TsuzukiResumeTile`.

Other shared parts:

- **Sheets:** top corners 28, a 36×4 handle (line colour) at the top, padding 10 / 16 / 20, 14 between blocks.
- **Segmented control:** a tile with radius 22 and 4 padding, holding equal-width 40 dp buttons with radius 18. The selected button is white with black text, or the accent where noted.
- **Switch:** a 44×26 track. On: accent track with an on-accent knob. Off: white-18% track with a white knob.
- **Primary button:** 52–56 dp tall, fully rounded, accent background, on-accent text, 14–15 sp ExtraBold.
- **Secondary button:** the same shape, but a tile background with normal text.

Motion everywhere, from board `MotionSpec`:

- **Easing curves:**
  - Enter: `cubic-bezier(0.05, 0.7, 0.1, 1)`.
  - Exit: `(0.3, 0, 0.8, 0.15)`.
  - Standard: `(0.2, 0, 0, 1)`.
  - Spring: `(0.34, 1.4, 0.64, 1)`, for small pops.
- **Durations:**
  - 100 ms: press feedback (scale to 0.97).
  - 200 ms: toggles, chips and tab pills.
  - 300 ms: sheets, nav and tile expand.
  - 400 ms: stack shuffle and page turn.
  - 450 ms: shared cover into the reader and return navigation. Owner 2026-11-14:
    opening book details has a 300 ms total budget; its synchronized cover/fade uses
    200 ms, reserving 100 ms for navigation and composition. Device verification pending.
  - 600 ms: the theme switch reveal.
- **Colour vs Dot:**
  - Colour is smooth: backgrounds morph between books, covers fly as shared elements, and sheets use springs.
  - Dot keeps the same timings, but fills and fades advance in 8 steps (dot by dot). There's no colour morph, and the red dot pulses once on arrival.
- **Rules:**
  - Everything is interruptible.
  - Never animate text someone is reading.
  - With reduce-motion on, every transition becomes a 150 ms fade.
  - Nothing runs longer than 600 ms.

## ChapterFilter (book details → filter icon)

Today this is the stock tabbed dialog (`ChapterSettingsDialog`, tabs are already pills). The board makes it one page.

A bottom sheet about 610 dp tall, from top to bottom:

1. Handle.
2. Title "Chapters", display face, 30 sp.
3. One tile (padding 4 / 14) with four filter rows, each at least 50 dp tall:
   - A 26 dp square box with radius 8:
     - "only" = accent fill with a ✓.
     - "hide" = `#FF8A70` fill with a –.
     - "any" = 1.5 dp border in the muted colour.
   - The label, 14 sp Bold.
   - The state in label style ("only" / "hide" / "any") on the right.
   - Tapping cycles any → only → hide.
   - Rows: Unread, Saved (downloaded), Bookmarked, plus whatever extra tri-states upstream has. "Has notes" only if notes exist in the data.
4. Label "Sort" and a segmented control: Newest / Oldest / Upload date. Map these onto upstream's sort modes and direction.
5. Label "Show as" and a segmented control: Chapter titles / Numbers only.
6. Bottom row: secondary "Mark previous read" and primary "Done". Mark previous read uses upstream's existing action, if there is one; otherwise leave it out.

Keep "Set as default" (upstream overflow) reachable.

## DownloadSheet (book details → download)

A bottom sheet about 640 dp tall:

1. Handle.
2. Title "Save chapters", display, 30 sp.
3. A 3×2 grid of option tiles, each 84 dp tall with radius 20 and 12 padding. A big number (num face, 26 sp) sits top-left and a label (11 sp Bold) bottom-left. The selected tile is accent with on-accent text.
   - Options: Next 1, Next 5, Next 25, Unread (count), All (count), Custom (…).
   - These match upstream's `DownloadAction`s. Custom opens the range below.
4. A range tile:
   - Header: label "Range" on the left, "Ch. a → b" on the right.
   - A two-thumb slider: 4 dp track, accent between the thumbs, 24 dp white thumbs.
   - An estimate line under it: "N chapters · about X MB · Y min of reading". Show only figures you can compute; leave out the size if unknown.
5. A switches tile: "Only on Wi-Fi" (the existing download preference) and "Delete after reading" (the existing auto-delete preference).
6. A primary button at the bottom: download icon + "Save N chapters".

## LibraryMixed (combined library)

Used when "Combined library" is on. This is the one library for every type.

- **Header:**
  - Title "Library" (display, 32 sp).
  - Under it, counts per type in muted 11 sp: "1,284 novels · 312 manga · 96 manhwa", only types that exist.
  - Search and filter icon buttons on the right.
- **Type pills:** a scrolling row of pills: All / Novels / Manga / Manhwa (Anime later).
  - Use `TsuzukiPillRow`.
  - They filter the grid.
  - Manhwa = manga whose source or genre marks it as manhwa / webtoon. Check what data exists. If you can't tell reliably, ship All / Novels / Manga only.
- **Grid:** 3 columns with 10 gap; covers 160 tall, radius 14; title 12 sp under each.
  - **Type badge, top-left** (20 tall, 10 sp ExtraBold):
    - LN: accent background.
    - MANHWA: white background, black text.
    - MANGA: black 75% background, white text, 1 dp white-40% border.
  - **Unread count, top-right:** accent badge.
- Keep the category pills (already done in `LibraryTabs`) when categories exist.

## ReaderSettings (novel reader → settings sheet)

Upstream has a stock sheet (`NovelPage` and friends). The board is a sheet about 560 dp tall over the live page, which updates as you change things:

- **Tabs:** a segmented control with the accent on the selected one: **Look / Layout / Controls**.
- **Look tab:**
  - "Page colour": a row of 46 dp circles with "Aa" in Literata. The selected one has a 2 dp sheet ring and then a 2 dp white ring. The pages:
    - Book: Colour `#111A2E` / `#E6E1D6`; Dot `#000` / `#D9D9D9`. This is the "app" theme, which already uses `BookTint`.
    - Paper: `#FAFAF7` / `#1F2024`.
    - Sepia: `#F1E6D2` / `#3A3024`.
    - Sage: `#DDE5D6` / `#25301F`.
    - Night: `#15140F` / `#DED7C9`.
    - Black: `#000` / `#C9C9C9`.
    - Map them onto upstream's novel themes. Add Sage and Night through the "custom" theme colours, or as new theme ids handled in `ThemeUtils.getThemeColors`.
  - "Font": 4 tiles (44 tall, radius 14), each name set in its own face: Literata (bundled), Source Serif, Atkinson, System. Bundle extra faces only as Latin subsets with their OFL licence (see how `lnr_literata.ttf` was done). Otherwise show only the faces the app has.
  - Two steppers side by side, each a tile 56 tall with − and + buttons (40 dp round, white 8%) and the value in the accent:
    - Size: 13–28.
    - Lines: line height 1.2–2.2, steps of 0.1.
- **Layout tab:**
  - "Reading mode" segment: Scroll / Pages.
  - "Margins": three tiles, Narrow 14 / Medium 26 / Wide 44 (map these to upstream's margin preferences).
  - Switches tile: Justify text, Focus reading (if it exists upstream), Keep screen on.
- **Controls tab:**
  - "Tap zones" preview: three tiles in a 1 : 1.3 : 1 grid, 150 tall. Back / **Menu** (accent) / Next.
  - Switches tile: Volume keys turn pages, Auto-scroll, Swipe to next chapter. Use only the switches that have upstream preferences.

## ReaderTOC (novel reader → contents)

This is a left drawer, not a sheet:

- **Size and shape:** 336 dp wide, full height, right corners 28. Sheet background (Colour uses `#1C2B4A` here) with a right border in the line colour. Behind it, a black 55% scrim over the page.
- **Header:**
  - The book title in label style.
  - "Contents" (display, 34 sp).
- **Segmented tabs:** Chapters / Marks / Notes. White selected; Marks = bookmarks; leave out Notes unless notes exist.
- **Jump field:** a search tile 44 tall, "Jump to chapter…", that filters the list.
- **Volume label:** `TsuzukiSectionLabel` (e.g. "Book IV · The Index") when volume data exists.
- **Chapter rows** (46 tall, radius 14):
  - The number in the num face, 36 wide.
  - The title, 13 sp Bold, one line.
  - A state mark on the right:
    - Current: the whole row is accent, with ●.
    - Downloaded: ↓ in the accent.
    - Read: ✓ and the row at 50% opacity.

## Motion boards (MotionOpen, MotionReader, MotionStack, MotionTheme, MotionSheet)

Use the curves and durations above. The intent of each:

| Board | What moves |
|---|---|
| MotionOpen | The cover in a list flies to its place on book details. Owner 2026-11-14 overrides the archived board's 450 ms opening: fully settle within 300 ms total, with 200 ms synchronized cover/fade and 100 ms preparation allowance. Colour/Dot keep their existing appearance and smooth easing. This total budget is pending device verification. |
| MotionReader | Resume: the cover scales into the reader page (450 ms). The reader bars slide in after. |
| MotionStack | Home's shelf stack: tap shuffles the top book to the back (400 ms). |
| MotionTheme | Switching Colour ↔ Dot: a circular reveal from the tapped card (600 ms). |
| MotionSheet | Sheets enter with the Enter curve (300 ms) and leave with Exit (200 ms). Colour adds a slight spring; Dot steps. |

Compose has `SharedTransitionLayout` for shared elements. Check that Voyager's transitions allow it before you start, and keep it behind the theme check so legacy themes are untouched.

## Getting more boards into the repo

The owner can copy boards out of Claude Design (download or copy a board's HTML from the canvas, or ask Claude to
write it out) and save them as `docs/design/boards/<BoardName>.dc.html`. Each file is plain HTML: the layout is in the markup, and the
numbers (sizes, colours, which state is selected) are in the `<script type="text/x-dc">` block at the
bottom (`renderVals()`). Read both. The CSS variables in `V(t)` are the theme tokens in the table above.
