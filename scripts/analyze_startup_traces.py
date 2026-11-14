#!/usr/bin/env python3
"""Summarize existing cold-start traces without launching or modifying an app.

Requires the official Perfetto trace_processor_shell (v59.1 or newer).
Durations are inclusive wall time, not CPU time. Nested rows must not be added.
"""

import argparse
import csv
import io
import json
import statistics
import subprocess
from pathlib import Path


def query(processor, trace, sql):
    result = subprocess.run(
        [str(processor), "query", "--quiet", "--format", "csv", str(trace), sql],
        capture_output=True,
        text=True,
        encoding="utf-8",
        check=True,
    )
    return list(csv.DictReader(io.StringIO(result.stdout))), result.stderr


def analyze(processor, trace, package):
    # SQL literals are escaped; all filesystem arguments use subprocess's argument list.
    literal = "'" + package.replace("'", "''") + "'"
    startups, health = query(processor, trace, f"""
INCLUDE PERFETTO MODULE android.startup.startups;
SELECT ts, ts_end, dur FROM android_startups
WHERE package = {literal} AND startup_type = 'cold';
""")
    if len(startups) != 1:
        raise ValueError(f"{trace.name}: expected one cold startup, found {len(startups)}")
    start, end = (int(startups[0][key]) for key in ("ts", "ts_end"))
    rows, _ = query(processor, trace, f"""
SELECT s.id, s.parent_id, s.ts, s.dur, s.depth, s.name
FROM slice s JOIN thread_track tt ON s.track_id = tt.id
JOIN thread t USING (utid) JOIN process p USING (upid)
WHERE p.name = {literal} AND t.tid = p.pid
AND s.dur >= 0 AND s.ts < {end} AND s.ts + s.dur > {start}
ORDER BY s.ts, s.depth;
""")
    for row in rows:
        for key in ("id", "ts", "dur", "depth"):
            row[key] = int(row[key])
        row["offset_ms"] = (row["ts"] - start) / 1e6
        row["duration_ms"] = row["dur"] / 1e6
    def longest(predicate):
        values = [row["duration_ms"] for row in rows if predicate(row["name"])]
        return max(values) if values else None

    first_traversal = next((r for r in rows if r["name"] == "traversal"), None)
    traversal_children = [
        r for r in rows
        if first_traversal and r["parent_id"] == str(first_traversal["id"])
    ]
    metrics = {
        "system_startup_ms": int(startups[0]["dur"]) / 1e6,
        "bind_application_ms": longest(lambda n: n == "bindApplication"),
        "open_app_dex_ms": longest(lambda n: n.startswith("OpenDexFilesFromOat(") and package in n),
        "chromium_sync_ms": longest(lambda n: n == "WebViewChromiumAwInit.startChromiumLockedSync"),
        "activity_create_ms": longest(lambda n: n.startswith("performCreate:") and "MainActivity" in n),
        "first_traversal_ms": first_traversal["duration_ms"] if first_traversal else None,
    }
    return {
        "trace": trace.name,
        "metrics": metrics,
        "health": health.strip(),
        "first_traversal_direct_children": traversal_children,
        "main_thread_slices": rows,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--processor", required=True, type=Path)
    parser.add_argument("--traces", required=True, type=Path)
    parser.add_argument("--package", required=True)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    traces = sorted(args.traces.glob("*.perfetto-trace"))
    if not traces:
        parser.error("No .perfetto-trace files found")
    runs = [analyze(args.processor.resolve(), trace.resolve(), args.package) for trace in traces]
    summary = {}
    for key in runs[0]["metrics"]:
        values = [r["metrics"][key] for r in runs if r["metrics"][key] is not None]
        summary[key] = {
            "samples": len(values),
            "median": statistics.median(values),
            "min": min(values),
            "max": max(values),
        } if values else {"samples": 0}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps({"package": args.package, "summary": summary, "runs": runs}, indent=2), encoding="utf-8")
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
