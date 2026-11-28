# Remaining boards (Claude, 2026-11-28) — awaiting owner approval

Live canvas (owner's Claude account, private): https://claude.ai/artifact/81kRWPKPFGGu6BAwyqYT1P
Drawn because these cards had no board (AGENTS.md: "ask the owner for it instead of inventing a layout"; owner
2026-11-28: "Design the ones not designed yet with claude design"). Same Dot / Colour tokens and motion values as
`../motion-2026-11/`. Each `.dc.html` has a Theme tweak (Dot, Colour); values are in the markup and `renderVals()`.

| File | Card | What it settles |
|---|---|---|
| `Main.dc.html` | V8.2 | Empty states: kaomoji upright in the **body** face (Space Mono / Manrope), 30 sp muted; one bold 15 sp line, 12 sp hint (≤ 2 lines), optional outlined accent pill (44 dp); fades in 150 ms after the data answered; legacy themes keep upstream `EmptyScreen` |
| `Airing.dc.html` | A6.6 | Details header Next tile shows `EP 13` over `in 2d 4h` (accent outline only when AniList has a time); Watch Home "Airing today" rows end with the real time (21:30) or a muted "today" for estimates; past `airingAt` dropped; hidden when unknown |
| `Screenshot.dc.html` | A6.7 | Options sheet row "Screenshot" (after Speed, before Open in another app); 150 ms white flash over the picture, snackbar "Saved to Pictures/Tsuzuki" + Share for 4 s; file `Title - EP 12 - 00-14-32.png`; not offered on the resume cover; reduced motion: no flash |
| `LocalAnime.dc.html` | A6.8 | Browse › Watch "On this device" group with a Local anime row (hidden without the folder); empty folder: V8.2 empty state plus the folder layout; rules for episode numbers, sidecar subtitles, cover / details.json |
| `AniyomiRestore.dc.html` | AY8 | Restore preview with an ANIYOMI origin badge and real counts (0 rows hidden); result lists anime waiting for an extension and settings not restored, with a button to Anime extensions |

Not drawn (no layout to design): AY7 (offline tracks keep their options — behaviour only), V8.6 (help link targets).
