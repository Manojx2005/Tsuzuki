#!/usr/bin/env bash
# Perfetto around one action:
#   scripts/perf/trace.sh <serial> start <seconds>   # starts a detached recording, returns at once
#   ... act (tap, jank.sh, a script) ...
#   scripts/perf/trace.sh <serial> pull <out.pftrace> # waits for it to finish, pulls it
# Read with Google's trace_processor (https://get.perfetto.dev/trace_processor):
#   python trace_processor -q scripts/perf/long-slices.sql out.pftrace
set -euo pipefail
export MSYS_NO_PATHCONV=1
serial=$1; cmd=$2
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"; PKG=${PKG:-io.github.manojx2005.tsuzuki.claudeqa}
remote=/data/misc/perfetto-traces/tsz.pftrace
case "$cmd" in
  start)
    seconds=${3:?seconds}
    cfg=$(mktemp)
    cat > "$cfg" <<CFG
buffers { size_kb: 65536 fill_policy: RING_BUFFER }
data_sources { config { name: "linux.ftrace" ftrace_config { ftrace_events: "sched/sched_switch" atrace_categories: "view" atrace_categories: "gfx" atrace_categories: "am" atrace_categories: "dalvik" atrace_categories: "binder_driver" atrace_apps: "$PKG" } } }
data_sources { config { name: "linux.process_stats" } }
duration_ms: $((seconds * 1000))
CFG
    "$ADB" -s "$serial" push "$(cygpath -w "$cfg")" /data/local/tmp/tsz-perf.cfg >/dev/null
    rm -f "$cfg"
    "$ADB" -s "$serial" shell "cat /data/local/tmp/tsz-perf.cfg | perfetto --background --txt -c - -o $remote" >/dev/null
    sleep 1 ;;
  pull)
    out=${3:?out}
    while "$ADB" -s "$serial" shell pidof perfetto >/dev/null 2>&1; do sleep 0.5; done
    "$ADB" -s "$serial" pull "$remote" "$(cygpath -w "$out")" >/dev/null
    echo "$out" ;;
  *) echo "usage: trace.sh <serial> start <seconds> | pull <out>"; exit 1 ;;
esac
