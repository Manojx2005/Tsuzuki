# Motion system board (Batch SM, card SM4) — 2026-11-28

Live canvas (owner's Claude account, private): https://claude.ai/artifact/BsV5GpMxfMVQ4ku4KHEg2r
Status: **approved by the owner 2026-11-28.** `docs/design/specs.md` §Motion now carries these values.

The `.dc.html` files are the board sources (Claude Design canvas format): the layout is the markup, the colours and
copy per theme are in `renderVals()` at the bottom, and **every timing is a CSS `@keyframes` block in `<helmet>`**
(percentages of the cycle noted in each comment). They need the canvas runtime (`./support.js`) to render, so read
them as source; the values below are complete.

| File | Artboard |
|---|---|
| `Main.dc.html` | Tokens (curves, durations) and the three rules |
| `Nav.dc.html` | Navigation push/pop: shared axis X |
| `Back.dc.html` | Predictive Back: commit and cancel |
| `TabsSheets.dc.html` | Home tabs, Library media switch, sheets and menus |
| `ReaderPlayer.dc.html` | Reader and player: shared axis Z |
| `canvas.json` | Canvas index (frames, order) |

## Curves

| Token | Cubic bezier | Use |
|---|---|---|
| Enter (emphasized decelerate) | 0.05, 0.7, 0.1, 1 | Anything arriving: pushed screens, sheets, reader, player, Back cancel |
| Exit (emphasized accelerate) | 0.3, 0, 0.8, 0.15 | Anything leaving for good: popped screens, closed sheets, Back commit |
| Standard | 0.2, 0, 0, 1 | Things that move and stay: segments, toggles, nav pill, shared cover |
| Finger | linear to gesture progress | Predictive Back while the finger is down |

These are today's `TsuzukiMotion.Enter`, `Exit` and `Standard`; `TsuzukiMotion.Spring` is no longer used by sheets.

## Durations

| ms | What |
|---|---|
| 100 | Press (scale 0.97, unchanged) |
| 150 | Late-content fade-in; every move under reduced motion; menus (fade + scale 0.92 → 1 from the anchor) |
| 200 | Segments, toggles, nav pill, Home tab fade-through (out 70 + in 130), sheet exit |
| 300 | Push / pop (shared axis X), sheet enter, reader and player open (shared axis Z) |
| 180 | Book details open: cover flies (MotionOpen, owner 2026-11-14); the page around it uses the X slide shortened to 180 |
| 250 | Predictive Back release (commit or cancel); reader and player close |
| 600 | Theme reveal (unchanged) |

## Rules

1. **Ready, then move.** The next screen is composed while the old one still shows; motion starts on the next frame
   (SM2). No frame over 8.3 ms once motion starts on a 120 Hz screen.
2. **Nothing pushes.** Content that arrives late fades in over 150 ms into space kept for it; never show "empty"
   before the data source answered; lists, buttons and counts never shift the page (SM6).
3. **Reduced motion.** System animations off or the app's Reduce motion: every move is a 150 ms crossfade, no slide,
   scale or flying cover.

## Navigation — shared axis X (`Nav.dc.html`)

- Push: new screen translateX +30 dp → 0 over 300 ms (Enter curve) and alpha 0 → 1 from 90 ms to 300 ms (linear);
  old screen translateX 0 → −30 dp over 300 ms (Enter) and alpha 1 → 0 in the first 90 ms (linear).
- Pop (Back button/key): exact mirror, 300 ms.
- Book details: shared cover flies 180 ms (Standard); the page slides with the same values compressed to 180 ms.
- Replaces the full-screen crossfade in `TsuzukiSharedCover.kt` (`TsuzukiNavigatorTransition`).

## Predictive Back (`Back.dc.html`)

- Every pushed screen in the main navigator (details, settings pages, browse source, migration). Needs
  `android:enableOnBackInvokedCallback="true"` and Compose `PredictiveBackHandler` in the navigator host.
- While the finger is down (progress p 0 → 1): page scale 1 → 0.90, corner radius 0 → 28 dp, translateX up to 24 dp
  toward the finger's edge; screen behind starts at −30 dp and 55 % brightness and moves to −18 dp / 75 % with p.
- Release past 35 % width (or a flick): commit — page leaves sideways in 250 ms (Exit), behind settles at 0 dp,
  100 % brightness. Released earlier: cancel — page returns to full size in 250 ms (Enter).
- Reader and player keep the system's own back-to-app animation.

## Tabs, media switch, sheets (`TabsSheets.dc.html`)

- Home tabs: fade-through 200 ms (old out 70 ms, new in 130 ms after it), nav pill slides 200 ms (Standard).
  Unchanged from today.
- Library media (All / Novels / Manga / Anime): one control; highlight slides 200 ms (Standard), header never moves,
  grid crossfades 150 ms in place (implemented 2026-11-28, MEDIA-SLIDE).
- Sheets: enter translateY 100 % → 0 in 300 ms (Enter), leave in 200 ms (Exit), scrim 0 → 45 % with them. Same in
  Dot and Colour (Colour's overshoot spring removed — owner to confirm). Menus: 150 ms fade + scale 0.92 → 1 from
  the anchor.

## Reader and player — shared axis Z (`ReaderPlayer.dc.html`)

- Open: incoming scale 0.92 → 1 and alpha 0 → 1 over 300 ms (Enter); outgoing scale 1 → 1.05 and alpha out in
  90 ms. Pivot: screen centre. Close: mirror in 250 ms (Exit).
- Player opens into its "resuming" cover; the picture fades in (150 ms) only once the clock moves (PLAYER-RESUME).
- Replaces `res/anim/tsuzuki_reader_{enter,behind,return,exit}.xml` (fade + 0.94 scale pivoted at 85 % height).
- MotionReader's cover-into-page stays as a variant when opening from the Home hero.

## Changes from what the owner approved before

- Colour sheets lose their overshoot spring (MotionSheet, Q1).
- Reader/player open becomes centred Z instead of the bottom-pivot scale.
- Predictive Back is new.
