# Offline design reference

Start with [INDEX.md](INDEX.md) or [gallery.html](gallery.html). The 2026-11-03 snapshot covers
all 16 pages of the owner's Tsuzuki App Screens canvas: **139 artboards and 99 individual Dot
screens**, saved as **238 PNGs** at 2× the design dimensions. The two archive pages are included
for reference; build the Final pages. The previous nine top-level Manga PNGs remain for older links.

Look at the relevant PNG **before changing UI**. Use `../specs.md` and the per-board HTML for
measurements, states and motion. Screenshots preserve the actual canvas, including any design
layout quirks. Example books, counts, anime and health indicators are mockups, not implementation data.

- `png/<page>/<BoardName>.png`: each complete artboard, including composite Dot overviews.
- `png/<page>/dot/<BoardName>.png`: individual Dot screens extracted from the original component bounds.
- `html/<page>/<BoardName>.html`: decoded per-board source, fonts and original interactive runtime.
- `exports/<page>.html`: untouched original page exports from Vivaldi, including font licences.
- `manifest.json`: original headings, ordering, sizes, text, component crop bounds, source hashes and render checks.

PNGs need no login, fonts or network. HTML retains the original interactive source, but a few boards
reference public Google Fonts that the export did not bundle. The capture renderer caches these
under `build/reports/design-font-cache`; use `--fetch-public-fonts` when rebuilding on a fresh machine.
Motion PNGs show a static state; use their HTML and `../specs.md` for transitions.

The DotAnime overview has seven source screens. Its separate Colour tracking board has no Dot
counterpart in the original canvas; it is preserved as `AnimeTracking.png`.

## Refresh a page

In the owner's signed-in **Vivaldi**, choose a canvas page, open the top-level **Tsuzuki App Screens**
menu, then **Export → HTML**. Save its original download under `exports/<page>.html` without
editing it. This exports the current canvas page, not the whole project. Repeat for changed pages.

Run with Node.js, Playwright and a Chromium binary available:

```sh
node scripts/render_design_exports.mjs --input docs/design/boards/exports --output docs/design/boards --resume --fetch-public-fonts
python scripts/index_design_boards.py
```

The renderer accepts `--browser`, `--packages`, `--font-cache` and `--scale` for local paths.
It uses an isolated browser on downloaded local files, never the signed-in profile. Only explicitly
enabled public Google Fonts requests are allowed. `--resume` rerenders changed source pages and
preserves unchanged pages. Update the renderer's page-name mapping if boards were reordered or added.

As an alternative, each artboard's export menu offers **All artboards (.pdf)** for the current
page. `scripts/export_design_pdf.py` extracts original-resolution images from these PDFs and
can split the Manga Dot overview. HTML export is preferred because it also preserves source and fonts.
