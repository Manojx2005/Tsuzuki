# Batch Q boards (2026-11-06) — phone fixes, reader settings, themes

Designs for the Codex cards **Q1–Q12** in `docs/HANDOFF.md`. Phone size 390×844 (the owner's phone is ≈ 384×832 dp).
Live canvas (owner only): https://claude.ai/artifact/Qs9pMeZhphy77fbPiE9cTH — pages **Dot** and **Colour**.

**Look at `png/<Board>.png` first.** `<Board>.dc.html` is the source: the layout is the markup, the numbers are in
`renderVals()` at the bottom (it needs the canvas runtime to run, so don't try to open it in a browser).
`evidence/` holds screenshots of the current app on the phone that show each bug.

| Board (Dot / Colour) | Card | What it shows |
|---|---|---|
| `Main` / `ColourMain` | Q2 | Reader menu: fixed ‹ › chapter buttons beside the progress, 4 labelled actions + MORE, ellipsised titles |
| `ChapterEnd` / `ColourChapterEnd` | Q2 | End-of-chapter card. In Colour it uses the **page** colours (ink on page); `pageColour` in `renderVals()` lists Sepia / Paper / Sage / Night / Black |
| `SaveChapters` / `ColourSaveChapters` | Q3 | Custom range by chapter **number**: From / To fields, −/+ steppers, shortcuts, validation, count |
| `Library` / `ColourLibrary` | Q4, Q5 | One DEFAULT shelf with counts; nav HOME · LIBRARY · UPDATES · BROWSE · MORE (Q5 waits for the owner) |
| `Details` / `ColourDetails` | Q6 | Toolbar back · download · filter · more; no N/A tile; one Resume; genres wrap |
| `Browse` / `ColourBrowse` | Q7 | NOVELS / MANGA pills + SOURCES / EXTENSIONS segment; full-width source names |
| `ReaderQuick` / `ColourReaderQuick` | Q11 | Quick reader sheet: TEXT · PAGE · CONTROLS, six named page colours, size and spacing with units |
| `ReaderAll` / `ColourReaderAll` | Q11 | Full-screen reader settings, TEXT tab: labelled tabs, labels above controls, FINE-TUNE expander, preview |
| `ReaderAllPage` / `ColourReaderAllPage` | Q11, Q12 | PAGE tab: one page-colour list + CUSTOM, side and top/bottom margins |
| `Themes` | Q12 | Appearance with only Colour and Dot, the eight real accent swatches, Dot covers switch |
| `Q9Selection` / `ColourQ9Selection` | Q9 (+Q3) | Long-press selection: count bar, selected covers, 5 labelled actions replacing the nav |
| `Q9Undo` / `ColourQ9Undo` | Q9 | Undo snackbar instead of a confirm dialog, 6 s time bar, above the nav |
| `Q9UpdatesLoading` / `ColourQ9UpdatesLoading` | Q9 | Updates first load: skeleton rows, no spinner |
| `Q9SettingsAdvanced` / `ColourQ9SettingsAdvanced` | Q9 | Rare options folded into one Advanced tile per settings screen |
| `AnimeShelves` / `ColourAnimeShelves` | A2.4 | Anime shelf management, separate from book shelves (owner decision) |
| `AnimeDebug` | A1.5 | Debug-only anime test screen: repo → extension → source → episode → play |

The Q9 / Anime boards (added 2026-11-06 by Claude) are **static HTML** (`<Board>.html`, open in any browser) with
the notes in a column beside the phone; `make_boards.py <dir>` regenerates them (Dot and Colour from one template).
Anime library / home / details / browse / player boards are in `../png/10-anime/` (use the new nav from Q5 on them).
Boards for X/Y cards are in `../INDEX.md`; cards with no UI (Q8 sweep, X1, Y1, Y4) need none.

Rules for reading the boards:
- Sample text and numbers are the owner's real data from the phone (Lord of the Mysteries, ch. 33, 1,432 chapters).
  Anything in `[brackets]` is a placeholder for real data. Never ship a number you can't back with data.
- Colour boards are drawn in uppercase because they were made from the Dot boards. **In the app, Colour labels use
  sentence case** with Manrope, and headings use Instrument Serif italic (as `ColourChapterEnd` shows).
- Colours come from `TsuzukiTheme.tokens`, never the hex values in the boards; Colour's navy/gold are the
  defaults that the book palette replaces.
