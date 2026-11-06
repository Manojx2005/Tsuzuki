"""Generate batch Q / anime boards (static HTML, 390x844) in Dot and Colour from one template."""
import os, sys

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)

THEMES = {
    'dot': dict(
        bg='#000', bgimg="background-image:radial-gradient(#1C1C1C 1px,transparent 1.2px);background-size:12px 12px;background-position:6px 6px;",
        tile='#111', border='#2A2A2A', muted='#8A8A8A', accent='#FF2D2D', on_accent='#fff', ink='#fff',
        sel_bg='#fff', sel_ink='#000', skel='#1A1A1A',
        head="font:900 40px/1 'Doto',monospace;letter-spacing:1px", body="'Space Mono',monospace",
        upper='text-transform:uppercase;', fonts='family=Doto:wght@900&family=Space+Mono:wght@400;700',
        cover=['#2B2B2B', '#3A3A3A', '#232323', '#333333', '#292929', '#3E3E3E'], cover_filter='filter:grayscale(1);',
    ),
    'colour': dict(
        bg='#1C2B4A', bgimg='', tile='rgba(255,255,255,.08)', border='rgba(255,255,255,.16)',
        muted='rgba(255,255,255,.72)', accent='#E9B44C', on_accent='#1C2B4A', ink='#fff',
        sel_bg='#fff', sel_ink='#1C2B4A', skel='rgba(255,255,255,.10)',
        head="font:italic 400 44px/1 'Instrument Serif',serif", body="'Manrope',sans-serif",
        upper='', fonts='family=Instrument+Serif:ital@1&family=Manrope:wght@500;700;800',
        cover=['#3B4F7A', '#5A4A6E', '#2F5A5A', '#6B4E3A', '#44507A', '#5E3B4A'], cover_filter='',
    ),
}

ICON = {
    'x': '<path d="M6 6l12 12M18 6L6 18"/>',
    'all': '<rect x="4" y="4" width="16" height="16" rx="3"/><path d="M8 12l3 3 5-6"/>',
    'shelf': '<path d="M4 6h16M4 12h16M4 18h10"/>',
    'read': '<path d="M5 12l4 4 10-10"/>',
    'save': '<path d="M12 4v11M7 10l5 5 5-5M5 20h14"/>',
    'remove': '<path d="M5 7h14M9 7V5h6v2M7 7l1 13h8l1-13"/>',
    'more': '<circle cx="5" cy="12" r="1.6"/><circle cx="12" cy="12" r="1.6"/><circle cx="19" cy="12" r="1.6"/>',
    'back': '<path d="M15 5l-7 7 7 7"/>',
    'search': '<circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/>',
    'filter': '<path d="M3 6h18M7 12h10M10 18h4"/>',
    'chev': '<path d="M9 6l6 6-6 6"/>',
    'down': '<path d="M6 9l6 6 6-6"/>',
    'drag': '<path d="M5 9h14M5 15h14"/>',
    'plus': '<path d="M12 5v14M5 12h14"/>',
    'play': '<path d="M8 5l11 7-11 7z"/>',
    'refresh': '<path d="M20 12a8 8 0 1 1-2.3-5.6M20 4v5h-5"/>',
}


def icon(name, color, size=22, fill=False):
    f = f'fill="{color}" stroke="none"' if fill else f'fill="none" stroke="{color}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"'
    return f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" {f} aria-hidden="true">{ICON[name]}</svg>'


def page(t, title, body, note):
    import re
    notes = re.findall(r'<div class="ann"[^>]*>(.*?)</div>', body)
    body = re.sub(r'<div class="ann"[^>]*>.*?</div>', '', body)
    side = ''.join(f'<li>{n}</li>' for n in notes)
    return f'''<!doctype html>
<html lang="en"><head><meta charset="utf-8"><title>{title}</title>
<!-- Board for docs/HANDOFF.md. Static HTML (no canvas runtime). Colours here are illustrations of
     TsuzukiTheme.tokens; build with tokens + MaterialTheme.typography, never these hex values.
     {note} -->
<link href="https://fonts.googleapis.com/css2?{t['fonts']}&display=swap" rel="stylesheet">
<style>
body{{margin:0;background:#2a2a2a;display:flex;width:640px}}
.side{{width:250px;box-sizing:border-box;padding:20px 16px;font:400 12px/1.45 'Space Mono',monospace;color:#cfe8ff}}
.side h2{{font:700 13px/1.3 'Space Mono',monospace;margin:0 0 10px;color:#fff}}
.side li{{margin-bottom:10px}}
.ph{{width:390px;height:844px;position:relative;overflow:hidden;background:{t['bg']};{t['bgimg']}font-family:{t['body']};color:{t['ink']};{t['upper']}}}
.h1{{{t['head']};margin:0}}
.muted{{color:{t['muted']};font:400 12px/1.4 {t['body']};letter-spacing:.4px}}
.lbl{{font:700 11px/1 {t['body']};letter-spacing:1px;color:{t['muted']}}}
.tile{{background:{t['tile']};border:1px solid {t['border']};border-radius:20px;box-sizing:border-box}}
.pill{{height:40px;padding:0 16px;border-radius:20px;border:1px solid {t['border']};background:{t['tile']};color:{t['ink']};font:700 12px/1 {t['body']};letter-spacing:1px;white-space:nowrap;display:inline-flex;align-items:center;gap:8px;box-sizing:border-box}}
.pill.on{{background:{t['accent']};border-color:{t['accent']};color:{t['on_accent']}}}
.ib{{width:48px;height:48px;display:flex;align-items:center;justify-content:center;border-radius:24px;flex:none}}
.row{{min-height:56px;display:flex;align-items:center;gap:14px;padding:0 16px;box-sizing:border-box}}
.row+.row{{border-top:1px solid {t['border']}}}
.t{{font:700 14px/1.3 {t['body']};letter-spacing:.4px}}
.s{{font:400 12px/1.35 {t['body']};color:{t['muted']};letter-spacing:.3px;text-transform:none}}
.btn{{height:56px;border-radius:28px;display:flex;align-items:center;justify-content:center;gap:10px;font:700 14px/1 {t['body']};letter-spacing:1px;box-sizing:border-box}}
.btn.p{{background:{t['accent']};color:{t['on_accent']}}}
.btn.o{{border:1px solid {t['border']};color:{t['ink']}}}
.nav{{position:absolute;left:16px;right:16px;bottom:20px;height:62px;border-radius:31px;background:{t['tile']};border:1px solid {t['border']};box-sizing:border-box;padding:0 6px;display:flex;align-items:center}}
.nav span{{flex:1;text-align:center;font:700 11px/1 {t['body']};letter-spacing:.6px;color:{t['muted']};position:relative}}
.ann{{position:absolute;right:8px;font:400 10px/1.3 'Space Mono',monospace;color:#7CF;text-transform:none;background:rgba(0,0,0,.7);padding:3px 6px;border-radius:4px;max-width:180px}}
</style></head><body>
<div class="ph">{body}</div>
<div class="side"><h2>{title}</h2><ul style="padding-left:16px;margin:0">{side}</ul><p style="color:#9ab">{note}</p></div>
</body></html>
'''


def nav(t, active):
    items = ['Home', 'Library', 'Updates', 'Browse', 'More']
    i = items.index(active)
    left = 6 + i * (356 - 12) / 5 + 0
    w = (356 - 12) / 5
    spans = ''.join(
        f'<span style="color:{t["sel_ink"]}">{x}</span>' if x == active else f'<span>{x}</span>' for x in items)
    return f'<div class="nav"><div style="position:absolute;left:{left:.0f}px;top:6px;width:{w:.0f}px;height:48px;border-radius:24px;background:{t["sel_bg"]}"></div>{spans}</div>'


BOOKS = ['Lord of the Mysteries', 'Shadow Slave', 'Reverend Insanity', 'Circle of Inevitability', 'Nano Machine',
         "The Academy's Genius Swordsman"]


def cover_grid(t, selected=(), top=0, books=None):
    cells = []
    for i, b in enumerate(books or BOOKS):
        sel = i in selected
        ring = f'outline:3px solid {t["accent"]};outline-offset:2px;' if sel else ''
        check = (f'<span style="position:absolute;top:6px;right:6px;width:26px;height:26px;border-radius:13px;'
                 f'background:{t["accent"]};display:flex;align-items:center;justify-content:center">'
                 f'{icon("read", t["on_accent"], 16)}</span>') if sel else ''
        dim = 'opacity:.55;' if selected and not sel else ''
        cells.append(f'''<div style="display:flex;flex-direction:column;gap:6px;min-width:0;{dim}">
  <div style="position:relative;aspect-ratio:2/3;border-radius:14px;background:{t["cover"][i]};{ring}">{check}</div>
  <div style="font:700 11px/1.35 {t["body"]};letter-spacing:.6px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden">{b}</div></div>''')
    return f'<div style="padding:{top}px 16px 0;display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px 10px">{"".join(cells)}</div>'


def lib_header(t):
    return f'''<div style="padding:20px 8px 0 20px;display:flex;align-items:center">
  <h1 class="h1" style="flex:1">Library</h1><div class="ib">{icon("search", t["ink"])}</div><div class="ib">{icon("filter", t["ink"])}</div><div class="ib">{icon("more", t["ink"], 20, True)}</div></div>
<div style="padding:6px 20px 0" class="muted">4 novels · 141 manga</div>
<div style="padding:14px 16px 0;display:flex;gap:8px"><span class="pill on">All</span><span class="pill">Novels</span><span class="pill">Manga</span></div>'''


# ---------------- boards ----------------

def selection(t):
    acts = [('shelf', 'Shelf'), ('read', 'Mark read'), ('save', 'Save'), ('remove', 'Remove'), ('more', 'More')]
    bar = ''.join(f'''<div style="flex:1;display:flex;flex-direction:column;align-items:center;gap:6px;padding-top:10px">
  {icon(n, t["ink"], 22, n == "more")}<span style="font:700 10px/1 {t["body"]};letter-spacing:.6px">{l}</span></div>''' for n, l in acts)
    body = f'''<div style="height:72px;display:flex;align-items:center;padding:0 8px;gap:4px;background:{t["tile"]};border-bottom:1px solid {t["border"]}">
  <div class="ib">{icon("x", t["ink"])}</div>
  <div style="flex:1;font:700 18px/1 {t["body"]};letter-spacing:1px">3 selected</div>
  <div class="ib">{icon("all", t["ink"])}</div></div>
<div style="padding:12px 16px 0;display:flex;gap:8px"><span class="pill on">All</span><span class="pill">Novels</span><span class="pill">Manga</span></div>
{cover_grid(t, selected=(0, 2, 4), top=16)}
<div style="position:absolute;left:12px;right:12px;bottom:20px;height:72px;border-radius:24px;background:{t["tile"]};border:1px solid {t["border"]};display:flex;box-sizing:border-box">{bar}</div>
<div class="ann" style="top:80px">Long-press a cover → selection. Long-press a 2nd → selects the range between (Q3/Q9)</div>
<div class="ann" style="bottom:100px">Bar replaces the nav while selecting; 5 labelled actions max, the rest in More. Back = clear selection</div>'''
    return 'Q9Selection', 'Library — selection mode', body, 'Card Q9 (+ Q3 range select). Same bar in chapter list, Updates and History with their own actions.'


def undo(t):
    body = f'''{lib_header(t)}
<div style="padding:10px 16px 0;display:flex;gap:8px"><span class="pill" style="background:{t["sel_bg"]};color:{t["sel_ink"]};border-color:{t["sel_bg"]}">Default <span style="font-weight:400;opacity:.7">143</span></span><span class="pill">Local <span style="font-weight:400;opacity:.7">0</span></span></div>
{cover_grid(t, top=16, books=["Shadow Slave", "Circle of Inevitability", "Nano Machine", "The Academy's Genius Swordsman", "Overgeared", "Solo Leveling"])}
<div style="position:absolute;left:16px;right:16px;bottom:96px;min-height:56px;border-radius:16px;background:{t["sel_bg"]};color:{t["sel_ink"]};display:flex;align-items:center;padding:0 8px 0 18px;box-sizing:border-box;gap:8px;overflow:hidden">
  <div style="flex:1;font:700 13px/1.3 {t["body"]};letter-spacing:.4px;text-transform:none">2 books removed from library</div>
  <div style="height:48px;padding:0 14px;display:flex;align-items:center;font:700 13px/1 {t["body"]};letter-spacing:1px;color:{t["accent"] if t["accent"] != "#E9B44C" else "#1C2B4A"};border-radius:24px">UNDO</div>
  <div style="position:absolute;left:0;bottom:0;height:3px;width:62%;background:{t["accent"]}"></div></div>
{nav(t, "Library")}
<div class="ann" style="top:150px">After Remove / Delete downloads / Mark read: no confirm dialog. Action runs, snackbar offers UNDO for 6 s (bar = time left). TalkBack: announced, stays until dismissed</div>'''
    return 'Q9Undo', 'Library — undo instead of confirm', body, 'Card Q9. Snackbar sits above the nav pill (or above the selection bar).'


def updates_loading(t):
    rows = ''
    for i in range(7):
        rows += f'''<div style="display:flex;gap:12px;align-items:center;padding:10px 0">
  <div style="width:48px;height:64px;border-radius:10px;background:{t["skel"]}"></div>
  <div style="flex:1;display:flex;flex-direction:column;gap:8px"><div style="height:12px;width:{70 - i * 5}%;border-radius:6px;background:{t["skel"]}"></div><div style="height:10px;width:{45 + (i % 3) * 10}%;border-radius:5px;background:{t["skel"]}"></div></div></div>'''
    body = f'''<div style="padding:20px 8px 0 20px;display:flex;align-items:center">
  <h1 class="h1" style="flex:1">Updates</h1><div class="ib">{icon("filter", t["ink"])}</div><div class="ib">{icon("refresh", t["ink"])}</div></div>
<div style="padding:14px 16px 0;display:flex;gap:8px"><span class="pill on">All</span><span class="pill">Manga</span><span class="pill">Novels</span></div>
<div style="padding:16px 20px 0" class="muted">Loading 1,871 updates…</div>
<div style="padding:6px 16px 0">{rows}</div>
{nav(t, "Updates")}
<div class="ann" style="top:150px">First frame within 100 ms: skeleton rows (no spinner). Mapping off the main thread; real rows replace skeletons in place, no jump. Static (no shimmer) when Reduce motion is on</div>'''
    return 'Q9UpdatesLoading', 'Updates — loading', body, 'Card Q9. Real count only if it is known before the rows (else just "Loading updates…").'


def settings_adv(t):
    def r(title, sub, end=''):
        return f'<div class="row"><div style="flex:1;min-width:0"><div class="t">{title}</div><div class="s">{sub}</div></div>{end}</div>'
    sw_on = f'<div style="width:52px;height:32px;border-radius:16px;background:{t["accent"]};position:relative;flex:none"><div style="position:absolute;right:4px;top:4px;width:24px;height:24px;border-radius:12px;background:{t["on_accent"]}"></div></div>'
    sw_off = f'<div style="width:52px;height:32px;border-radius:16px;border:2px solid {t["muted"]};box-sizing:border-box;position:relative;flex:none"><div style="position:absolute;left:5px;top:5px;width:18px;height:18px;border-radius:9px;background:{t["muted"]}"></div></div>'
    body = f'''<div style="padding:16px 8px 0 8px;display:flex;align-items:center;gap:4px"><div class="ib">{icon("back", t["ink"])}</div><h1 class="h1" style="font-size:34px">Library</h1></div>
<div style="padding:20px 20px 8px" class="lbl">Display</div>
<div class="tile" style="margin:0 16px">{r("Show unread count", "Badge on each cover", sw_on)}{r("Shelf book counts", "Number beside each shelf", sw_on)}</div>
<div style="padding:20px 20px 8px" class="lbl">Updates</div>
<div class="tile" style="margin:0 16px">{r("Automatic updates", "Every 12 hours · Wi-Fi only", icon("chev", t["muted"]))}{r("Only update ongoing", "Skip completed books", sw_off)}</div>
<div style="padding:20px 20px 8px" class="lbl">Advanced</div>
<div class="tile" style="margin:0 16px">
  <div class="row"><div style="flex:1"><div class="t">Advanced options</div><div class="s">9 more · update restrictions, mark-duplicate rules, swipe actions…</div></div>{icon("down", t["ink"])}</div></div>
<div class="ann" style="top:470px">Rare Tsundoku power options fold into one collapsed Advanced tile at the end of each settings screen. Nothing deleted; search still finds them and opens the tile expanded</div>'''
    return 'Q9SettingsAdvanced', 'Settings — advanced options folded', body, 'Card Q9. Which options are "advanced": decided per screen, listed in the card Result.'


def anime_shelves(t):
    shelves = [('Default', '[n]', True), ('Watching', '[n]', False), ('Plan to watch', '[n]', False), ('Completed', '[n]', False)]
    rows = ''
    for name, n, system in shelves:
        tag = f'<span style="font:700 10px/1 {t["body"]};letter-spacing:.8px;color:{t["accent"]};margin-left:8px">SYSTEM</span>' if system else ''
        end = '' if system else f'<div class="ib">{icon("more", t["ink"], 20, True)}</div>'
        rows += f'''<div class="row" style="min-height:64px">{icon("drag", t["muted"]) if not system else '<span style="width:22px"></span>'}
  <div style="flex:1;min-width:0"><div class="t">{name}{tag}</div><div class="s">{n} anime</div></div>{end}</div>'''
    body = f'''<div style="padding:16px 8px 0 8px;display:flex;align-items:center;gap:4px"><div class="ib">{icon("back", t["ink"])}</div><h1 class="h1" style="font-size:34px">Anime shelves</h1></div>
<div style="padding:10px 20px 0" class="muted" >Only for anime. Novel and manga shelves are separate and stay as they are.</div>
<div class="tile" style="margin:20px 16px 0">{rows}</div>
<div style="position:absolute;left:16px;right:16px;bottom:28px" class="btn p">{icon("plus", t["on_accent"])}New anime shelf</div>
<div class="ann" style="top:420px">Opened from the Anime library overflow (“Edit shelves”) and Settings › Library. Same components as book Shelves; data from anime_categories. Default (_id 0) can’t be renamed or deleted. [n] = real count</div>'''
    return 'AnimeShelves', 'Anime shelves — separate from book shelves', body, 'Card A2.4 (owner decision ANIME_SCHEMA 9 Q1: separate anime shelves). Library tabs: AnimeLibrary board.'


def anime_debug(t):
    steps = ['Repo', 'Extension', 'Source', 'Episode', 'Play']
    st = ''.join(f'<span class="pill{" on" if i == 1 else ""}" style="height:32px;padding:0 10px;font-size:10px">{i + 1} {s}</span>' for i, s in enumerate(steps))
    ext = [('[extension A]', 'v14 · EN', 'Installed'), ('[extension B]', 'v9 · JA', 'Install'), ('[extension C]', 'v22 · EN', 'Install')]
    erows = ''.join(f'''<div class="row"><div style="width:36px;height:36px;border-radius:10px;background:{t["skel"]}"></div>
  <div style="flex:1"><div class="t">{a}</div><div class="s">{b}</div></div>
  <span class="pill{" on" if c == "Install" else ""}" style="height:36px">{c}</span></div>''' for a, b, c in ext)
    body = f'''<div style="padding:16px 8px 0 8px;display:flex;align-items:center;gap:4px"><div class="ib">{icon("back", t["ink"])}</div><h1 class="h1" style="font-size:30px">Anime test</h1>
<span style="margin-left:8px;font:700 10px/1 {t["body"]};color:{t["accent"]};border:1px solid {t["accent"]};padding:4px 6px;border-radius:6px">DEBUG</span></div>
<div style="padding:12px 16px 0;display:flex;gap:6px;flex-wrap:wrap">{st}</div>
<div style="padding:18px 20px 8px" class="lbl">Repository</div>
<div class="tile" style="margin:0 16px;padding:12px 16px;display:flex;gap:10px;align-items:center">
  <div style="flex:1;font:400 12px/1.3 {t["body"]};text-transform:none;overflow:hidden;white-space:nowrap;text-overflow:ellipsis">github.com/yuzono/anime-extensions</div><span class="pill on" style="height:36px">Load</span></div>
<div style="padding:8px 20px 0" class="s">2 saved: yuzono · Secozzi</div>
<div style="padding:18px 20px 8px" class="lbl">Extensions · 3 of [n]</div>
<div class="tile" style="margin:0 16px">{erows}</div>
<div style="padding:16px 20px 0;font:400 12px/1.4 {t["body"]};color:{t["accent"]};text-transform:none">Signature not trusted — tap Trust to allow this extension (same check as manga extensions).</div>
<div style="position:absolute;left:16px;right:16px;bottom:28px" class="btn p">{icon("play", t["on_accent"], 20, True)}Next: pick a source</div>
<div class="ann" style="top:96px">Debug builds only (developer settings). One screen, 5 steps; each step lists real data from the repo/extension; errors shown as text, never swallowed</div>'''
    return 'AnimeDebug', 'Anime test screen (debug)', body, 'Card A1.5. Dot only; debug builds only, never in release.'


made = []
for fn in (selection, undo, updates_loading, settings_adv, anime_shelves, anime_debug):
    for theme in ('dot', 'colour'):
        if fn is anime_debug and theme == 'colour':
            continue
        t = THEMES[theme]
        name, title, body, note = fn(t)
        file = name + '.html' if theme == 'dot' else 'Colour' + name + '.html'
        open(os.path.join(OUT, file), 'w', encoding='utf-8', newline='\n').write(page(t, title, body, note))
        made.append(file)
print('\n'.join(made))
