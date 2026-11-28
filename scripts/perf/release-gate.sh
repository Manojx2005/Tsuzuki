#!/usr/bin/env bash
# Release gate (Batch SM8, docs/SMOOTHNESS_PLAN.md). Run on the exact commit you will tag.
#
#   scripts/perf/release-gate.sh                 # build + tests + packaging checks
#   scripts/perf/release-gate.sh <serial>        # ... plus smoothness thresholds on a device (S22 recommended)
#
# Steps: all-module unit tests and the SQLDelight migration check; Preview and Release (-Penable-updater) builds;
# anime extension ABI audit on the Preview APK; 16 KB alignment of every APK; the bundled baseline profile
# (assets/dexopt/baseline.prof) in every Release APK and the source profile's Tsuzuki coverage; then, with a device,
# a fresh-install smoothness run (release build installed as the claudeqa scratch package, bundled profile only):
# book details open and Back x6 from Library > All and 6 flings of the details chapter list.
# Thresholds (SM8, SM9): median longest UI frame <= 33 ms to open details and for Back (Back measured 30 ms after
# SM9 on 2026-11-28 — little margin, so a FAIL just over 33 ms is worth one rerun before treating it as a regression);
# chapter-list flings <= 2 % of frames over 16.7 ms. A miss prints FAIL and the script exits 1: do not tag unless the owner waives it.
# The device run needs a library whose first book in Library > All has a long chapter list (S22 claudeqa has one).
set -uo pipefail
root=$(git rev-parse --show-toplevel); cd "$root"
export JAVA_HOME=${JAVA_HOME:-"/c/Program Files/Android/Android Studio/jbr"}
serial=${1:-}
fail=0
note() { printf '%s\n' "$*"; }
bad() { printf 'FAIL %s\n' "$*"; fail=1; }

note "== commit $(git rev-parse --short HEAD)"
if [ -z "${GATE_DEVICE_ONLY:-}" ]; then  # GATE_DEVICE_ONLY=1 reruns only the device thresholds
./gradlew --console=plain --max-workers=2 testDebugUnitTest :data:verifySqlDelightMigration > release-gate.log 2>&1 \
  || bad "unit tests / migrations (release-gate.log)"
./gradlew --console=plain --max-workers=2 :app:assemblePreview >> release-gate.log 2>&1 || bad "Preview build"
./gradlew --console=plain --max-workers=2 :app:assembleRelease -Penable-updater >> release-gate.log 2>&1 || bad "Release build"
python -I - <<'PY' || bad "test results"
import glob, xml.etree.ElementTree as ET
t = f = 0
for p in glob.glob('**/build/test-results/testDebugUnitTest/*.xml', recursive=True):
    r = ET.parse(p).getroot(); t += int(r.get('tests')); f += int(r.get('failures')) + int(r.get('errors'))
print(f'tests {t}, failures {f}')
raise SystemExit(1 if f or not t else 0)
PY

python -I scripts/anime-abi/check_apk.py app/build/outputs/apk/preview/app-arm64-v8a-preview.apk > release-gate-abi.log 2>&1 \
  && note "anime ABI ok" || bad "anime ABI audit (release-gate-abi.log)"

za=$(ls -d "$LOCALAPPDATA"/Android/Sdk/build-tools/*/ | sort -V | tail -1)zipalign.exe
for apk in app/build/outputs/apk/release/*.apk app/build/outputs/apk/preview/*.apk; do
  "$za" -c -P 16 4 "$apk" >/dev/null 2>&1 || bad "16 KB alignment: $apk"
done
note "16 KB alignment checked"

for apk in app/build/outputs/apk/release/*.apk; do
  # Not grep -q: it stops reading early, unzip then dies of SIGPIPE and pipefail reports a false failure.
  unzip -l "$apk" | grep "assets/dexopt/baseline.prof" >/dev/null || bad "no baseline profile in $apk"
done
tsz=$(grep -c tsuzuki app/src/main/baselineProfiles/baseline-prof.txt)
note "baseline profile: $tsz Tsuzuki rules (SM1 collected 3,572 on 2026-11-28)"
[ "$tsz" -ge 3000 ] || bad "baseline profile lost Tsuzuki coverage ($tsz rules); re-collect (docs/PERFORMANCE.md)"
changed=$(git log -1 --format=%h -- app/src/main/baselineProfiles)
note "profile last changed in $changed; if Tsuzuki screens changed since, re-collect the five journeys before tagging"
fi

if [ -n "$serial" ]; then
  export MSYS_NO_PATHCONV=1
  ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"; PKG=io.github.manojx2005.tsuzuki.claudeqa
  bash scripts/perf/build-qa-release.sh "$serial" --fresh >> release-gate.log 2>&1 || bad "claudeqa release install"
  "$ADB" -s "$serial" shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 8
  "$ADB" -s "$serial" shell am force-stop $PKG; sleep 1
  "$ADB" -s "$serial" shell cmd package compile -m speed-profile -f $PKG >/dev/null
  "$ADB" -s "$serial" shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 6
  size=$("$ADB" -s "$serial" shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1); w=${size%x*}; h=${size#*x}
  nav=$((h * 92 / 100)); tap() { "$ADB" -s "$serial" shell input tap "$1" "$2"; sleep "${3:-1.5}"; }
  tap $((w * 33 / 100)) $nav 2        # Library (phone bottom bar, 2nd of 5)
  tap $((w * 17 / 100)) $((h * 15 / 100))   # All
  out=release-gate-device.txt; : > $out
  SETTLE=1.6 bash scripts/perf/jank.sh "$serial" warm tap $((w * 19 / 100)) $((h * 34 / 100)) >/dev/null
  "$ADB" -s "$serial" shell input keyevent 4; sleep 1.5
  for r in 1 2 3 4 5 6; do
    SETTLE=1.6 bash scripts/perf/jank.sh "$serial" "open" tap $((w * 19 / 100)) $((h * 34 / 100)) >> $out; sleep 0.8
    SETTLE=1.6 bash scripts/perf/jank.sh "$serial" "back" key 4 >> $out; sleep 1.2
  done
  tap $((w * 19 / 100)) $((h * 34 / 100)) 2.5
  for r in 1 2 3 4 5 6 7; do
    SETTLE=2.2 bash scripts/perf/jank.sh "$serial" "fling" swipe $((w / 2)) $((h * 81 / 100)) $((w / 2)) $((h * 21 / 100)) >> $out
  done
  "$ADB" -s "$serial" shell input keyevent 4
  python -I - "$out" <<'PY' || fail=1
import re, statistics, sys
rows = [l for l in open(sys.argv[1], encoding='utf-8') if ':' in l]
def ui(label): return [int(m) for l in rows if l.startswith(label) for m in re.findall(r'maxUI=(\d+)ms', l)]
bad = 0
for label, limit in (('open', 33), ('back', 33)):
    v = ui(label)
    if not v: print(f'FAIL no {label} samples'); bad = 1; continue
    med = statistics.median(v)
    print(f'{label}: median longest UI {med:.0f} ms (worst {max(v)})' + ('' if med <= limit else f'  FAIL > {limit} ms'))
    bad |= med > limit
flings = [l for l in rows if l.startswith('fling')][1:]  # the first fling scrolls the header away
frames = sum(int(re.search(r'frames=(\d+)', l).group(1)) for l in flings)
slow = sum(int(re.search(r'over16ms=(\d+)', l).group(1)) for l in flings)
share = 100 * slow / frames if frames else 100
print(f'chapter-list flings: {slow}/{frames} frames over 16.7 ms ({share:.1f} %)' + ('' if share <= 2 else '  FAIL > 2 %'))
bad |= share > 2
raise SystemExit(bad)
PY
fi

if [ $fail -eq 0 ]; then note "== GATE PASSED"; else note "== GATE FAILED"; fi
exit $fail
