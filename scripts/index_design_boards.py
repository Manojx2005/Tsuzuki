"""Build a portable offline gallery and verify the captured design inventory."""

from pathlib import Path
import hashlib
import html
import json


ROOT = Path(__file__).resolve().parents[1] / "docs/design/boards"
manifest = json.loads((ROOT / "manifest.json").read_text(encoding="utf-8"))
pages = manifest["pages"]
board_count = sum(len(page["boards"]) for page in pages)
dot_count = sum(len(board["screens"]) for page in pages for board in page["boards"])
paths = []
markdown = [
    "# Offline design index",
    "",
    "Captured from the owner's Tsuzuki App Screens canvas in Vivaldi on 2026-11-03.",
    "",
    f"**{len(pages)} pages · {board_count} artboards · {dot_count} individual Dot screens.**",
    "",
    "Open [the visual gallery](gallery.html) to browse all PNGs. PNGs are 2× the design dimensions",
    "and need no fonts, browser session or network. The source exports and per-board HTML are also retained.",
    "Archive pages are reference material, not implementation targets. Motion PNGs capture one static state;",
    "their HTML retains the original interactions. Sample books and statistics are design examples only.",
    "",
    "| Page | Artboards | Individual Dot screens | Original export |",
    "|---|---:|---:|---|",
]
gallery = [
    '<!doctype html><html lang="en"><meta charset="utf-8">',
    '<meta name="viewport" content="width=device-width,initial-scale=1">',
    '<title>Tsuzuki offline design gallery</title>',
    '<style>body{margin:24px;background:#141414;color:#eee;font:16px system-ui}',
    'a{color:#a9d5ff}nav{display:flex;flex-wrap:wrap;gap:12px}section{margin-top:40px}',
    '.grid{display:flex;flex-wrap:wrap;align-items:flex-start;gap:20px}',
    'figure{margin:0;width:240px}img{width:100%;height:360px;object-fit:contain;',
    'object-position:top;background:#080808}figcaption{padding:8px 0;overflow-wrap:anywhere}',
    'small{color:#aaa}summary{cursor:pointer;padding:16px 0}h2{border-top:1px solid #444;padding-top:20px}</style>',
    '<h1>Tsuzuki design reference</h1>',
    f'<p>2026-11-03 · {len(pages)} pages · {board_count} artboards · {dot_count} individual Dot screens · 2× PNG.</p>',
    '<p>These images work offline. Archive pages are reference only. <a href="INDEX.md">Text index</a></p><nav>',
]
for page in pages:
    label = page["page"]
    dots = sum(len(board["screens"]) for board in page["boards"])
    markdown.append(f'| [{label}](#{label}) | {len(page["boards"])} | {dots} | [HTML]({page["source"]}) |')
    gallery.append(f'<a href="#{label}">{html.escape(label)}</a>')
gallery.append('</nav>')


def card(name, title, png, width, height):
    paths.append(png)
    return (
        f'<figure><a href="{html.escape(png)}"><img loading="lazy" src="{html.escape(png)}" '
        f'alt="{html.escape(title)}"></a><figcaption>{html.escape(name)}<br>'
        f'<small>{html.escape(title)} · {width}×{height} design px</small></figcaption></figure>'
    )


for page in pages:
    label = page["page"]
    markdown.extend(["", f"## {label}", "", "| Board | Design size | PNG | Source |", "|---|---|---|---|"])
    gallery.append(f'<section id="{label}"><h2>{html.escape(label)}</h2><div class="grid">')
    dot_cards = []
    dot_rows = []
    source = ROOT / page["source"]
    assert source.is_file(), source
    assert hashlib.sha256(source.read_bytes()).hexdigest() == page["sourceSha256"], source
    for board in page["boards"]:
        assert (ROOT / board["html"]).is_file(), board["html"]
        gallery.append(card(board["name"], board["title"], board["png"], board["width"], board["height"]))
        markdown.append(
            f'| {board["name"]} — {board["title"]} | {board["width"]}×{board["height"]} '
            f'| [PNG]({board["png"]}) | [HTML]({board["html"]}) |'
        )
        for screen in board["screens"]:
            dot_cards.append(card(screen["name"], "Dot · " + screen["name"], screen["png"], screen["width"], screen["height"]))
            dot_rows.append(f'- [{screen["name"]} — Dot]({screen["png"]})')
    gallery.append('</div>')
    if dot_cards:
        gallery.extend(['<details><summary>Individual Dot screens</summary><div class="grid">', *dot_cards, '</div></details>'])
        markdown.extend(["", "Individual Dot screens:", "", *dot_rows])
    gallery.append('</section>')

assert len(paths) == len(set(paths)), "Duplicate screenshot paths"
for relative in paths:
    assert (ROOT / relative).is_file(), relative
gallery.append('</html>')
(ROOT / "INDEX.md").write_text("\n".join(markdown) + "\n", encoding="utf-8", newline="")
(ROOT / "gallery.html").write_text("\n".join(gallery) + "\n", encoding="utf-8", newline="")
print(f"Indexed {len(pages)} pages, {board_count} artboards, {dot_count} Dot screens ({len(paths)} PNGs)")
