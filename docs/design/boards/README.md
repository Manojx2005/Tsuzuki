# Board screenshots

`png/<BoardName>.png` are exports of the Claude Design canvas, one per board (Colour). Each page's Dot
boards are one combined image (`DotManga.png` and so on). Compare your screen against these; exact numbers are in
`../specs.md`.

So far: the "Final · Manga & manhwa" page. To add more pages, in the canvas (Chrome) pick a page, then hover any
board's title, click the export icon ("Export this artboard"), choose **All artboards (.pdf)** and save. Then turn
the PDF into PNGs (for example with the `pdf-to-img` npm package) and name each page in canvas order (top to bottom,
then left to right). The board positions are in the canvas's `canvas.json` (Claude can read it).
