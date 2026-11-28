#!/usr/bin/env bash
# Build an optimized release APK as the claudeqa scratch package, install it on <serial> and compile it the way
# an installed app is compiled. Release is signed with the debug key, so it installs over the debug claudeqa and
# keeps its test library. Usage: scripts/perf/build-qa-release.sh <serial> [--fresh]
#   --fresh: reset compilation instead (only the bundled baseline profile applies, like a fresh GitHub install).
set -euo pipefail
serial=${1:?serial}; mode=${2:-}
root=$(git rev-parse --show-toplevel); cd "$root"
export JAVA_HOME=${JAVA_HOME:-"/c/Program Files/Android/Android Studio/jbr"}
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"; PKG=io.github.manojx2005.tsuzuki.claudeqa
cp app/build.gradle.kts /tmp/tsz-build.gradle.kts.bak
trap 'cp /tmp/tsz-build.gradle.kts.bak app/build.gradle.kts' EXIT
python - <<'PY'
p='app/build.gradle.kts'
s=open(p,encoding='utf-8',newline='').read()
old='''        val release = getByName("release") {
            isMinifyEnabled = true'''
assert old in s, 'release block changed; update build-qa-release.sh'
s=s.replace(old,'''        val release = getByName("release") {
            applicationIdSuffix = ".claudeqa"
            isMinifyEnabled = true''',1)
open(p,'w',encoding='utf-8',newline='').write(s)
PY
./gradlew :app:assembleRelease --max-workers=2 -q
apk=app/build/outputs/apk/release/app-arm64-v8a-release.apk
"$ADB" -s "$serial" install -r "$(cygpath -w "$apk")"
if [ "$mode" = "--fresh" ]; then
  "$ADB" -s "$serial" shell cmd package compile --reset "$PKG"
else
  "$ADB" -s "$serial" shell cmd package compile -m speed-profile -f "$PKG"
fi
echo "installed $(sha256sum "$apk" | cut -c1-16) on $serial ($PKG${mode:+, $mode})"
