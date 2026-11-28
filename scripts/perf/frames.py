"""Per-frame timings from `dumpsys gfxinfo <pkg> framestats`.

Usage: python -I frames.py <framestats.txt> [--summary]
Prints offset ms : total ms / UI-thread ms per frame, or (with --summary) the frame count, frames over 16.7 ms,
the longest UI-thread frame and the UI-thread ms spent in the first 150 ms.
"""
import sys


def frames(path):
    lines = open(path, encoding="utf-8", errors="ignore").read().splitlines()
    marks = [i for i, line in enumerate(lines) if line.strip() == "---PROFILEDATA---"]
    rows = []
    for start in marks[::2]:
        header = lines[start + 1].split(",")
        for line in lines[start + 2:]:
            if line.startswith("---"):
                break
            values = line.split(",")
            if len(values) < len(header) - 1:
                continue
            row = dict(zip(header, values))
            if row.get("Flags") != "0":
                continue
            vsync = int(row["IntendedVsync"])
            total = (int(row["FrameCompleted"]) - vsync) / 1e6
            ui = (int(row["SyncStart"]) - int(row["HandleInputStart"])) / 1e6
            rows.append((vsync, total, ui))
    rows.sort()
    return rows


def main():
    rows = frames(sys.argv[1])
    if not rows:
        print("no frames")
        return
    t0 = rows[0][0]
    if "--summary" in sys.argv:
        longest = max(rows, key=lambda r: r[2])
        early = sum(r[2] for r in rows if (r[0] - t0) / 1e6 < 150)
        over = sum(1 for r in rows if r[1] > 16.7)
        print(f"frames={len(rows)} over16ms={over} maxUI={longest[2]:.0f}ms@{(longest[0] - t0) / 1e6:.0f} "
              f"UI_first150={early:.0f}ms")
    else:
        print(" ".join(f"{(v - t0) / 1e6:.0f}:{t:.0f}/{u:.0f}" for v, t, u in rows))


main()
