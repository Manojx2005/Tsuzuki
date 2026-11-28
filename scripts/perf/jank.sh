#!/usr/bin/env bash
# Reset frame stats, perform one action, print the frames of that action.
# Usage: scripts/perf/jank.sh <serial> <label> tap <x> <y> | key <code> | swipe <x1> <y1> <x2> <y2>
# Refuses to act unless Tsuzuki (claudeqa) is in front: a stray Back once sent taps into system Settings.
set -uo pipefail
export MSYS_NO_PATHCONV=1
serial=$1; label=$2; shift 2
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"; PKG=${PKG:-io.github.manojx2005.tsuzuki.claudeqa}
here=$(cd "$(dirname "$0")" && pwd)
focus=$("$ADB" -s "$serial" shell dumpsys window | grep -m1 mCurrentFocus)
echo "$focus" | grep -q "$PKG" || { echo "STOP $label: $PKG not in front ($focus)"; exit 2; }
"$ADB" -s "$serial" shell dumpsys gfxinfo "$PKG" reset >/dev/null
case "$1" in
  tap) "$ADB" -s "$serial" shell input tap "$2" "$3" ;;
  key) "$ADB" -s "$serial" shell input keyevent "$2" ;;
  swipe) "$ADB" -s "$serial" shell input swipe "$2" "$3" "$4" "$5" 300 ;;
  *) echo "unknown action $1"; exit 1 ;;
esac
sleep "${SETTLE:-1.6}"
out=$(mktemp)
"$ADB" -s "$serial" shell dumpsys gfxinfo "$PKG" framestats > "$out"
echo "$label: $(python -I "$(cygpath -w "$here/frames.py")" "$(cygpath -w "$out")" --summary)"
rm -f "$out"
