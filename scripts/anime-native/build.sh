#!/usr/bin/env bash
set -euo pipefail

project_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
work_dir="$project_dir/build/anime-native-work"
output_dir="$project_dir/build/anime-native-output"
mkdir -p "$work_dir" "$output_dir/evidence" "$output_dir/licenses"
exec > >(tee "$output_dir/evidence/build.log") 2>&1

native_revision=da7c885c18ae117ca27239c562ea7572fce3d4f5
ndk_version=27.2.12479018
ndk_name=r27c
sdk_dir=${ANDROID_HOME:?GitHub runner Android SDK is required}
sdkmanager="$sdk_dir/cmdline-tools/latest/bin/sdkmanager"

# The work directory is isolated and disposable; never reuse a native build
# whose feature configuration or dependency revisions are unknown.
native_dir="$work_dir/mpv"
if [[ -e "$native_dir" ]]; then
    echo 'Native work directory already exists; use a fresh checkout.' >&2
    exit 1
fi
git clone --no-checkout https://github.com/aniyomiorg/aniyomi-mpv-lib.git "$native_dir"
git -C "$native_dir" checkout --detach "$native_revision"
cp "$native_dir/LICENSE" "$output_dir/licenses/aniyomi-mpv-wrapper-MIT.txt"

"$sdkmanager" "ndk;$ndk_version"
mkdir -p "$native_dir/buildscripts/sdk"
ln -s "$sdk_dir" "$native_dir/buildscripts/sdk/android-sdk-linux"
ln -s "$sdk_dir/ndk/$ndk_version" "$native_dir/buildscripts/sdk/android-ndk-$ndk_name"

export NATIVE_DIR="$native_dir"
python3 - <<'PY'
from pathlib import Path
import os

root = Path(os.environ['NATIVE_DIR'])
ffmpeg = root / 'buildscripts/scripts/ffmpeg.sh'
text = ffmpeg.read_text()
old = '--enable-{gpl,version3}'
assert text.count(old) == 1
# mbedTLS requires FFmpeg's version3 mode. With GPL disabled this selects
# LGPLv3, not GPLv3; audit the generated flags and licence string below.
text = text.replace(old, '--enable-version3 --disable-{gpl,nonfree,postproc}')
old = '--extra-ldflags="-L$prefix_dir/lib"'
assert text.count(old) == 1
text = text.replace(old, '--extra-ldflags="-L$prefix_dir/lib -Wl,-z,max-page-size=16384"')
ffmpeg.write_text(text)

mpv = root / 'buildscripts/scripts/mpv.sh'
text = mpv.read_text()
assert text.count('-Dlibmpv=true') == 1
mpv.write_text(text.replace('-Dlibmpv=true', '-Dgpl=false -Dlibmpv=true'))

# Postproc is a GPL library, absent in this FFmpeg configuration. Nothing in
# this wrapper's LOCAL_SHARED_LIBRARIES uses it.
makefile = root / 'app/src/main/jni/Android.mk'
text = makefile.read_text()
block = '''include $(CLEAR_VARS)
LOCAL_MODULE := libpostproc
LOCAL_SRC_FILES := $(PREFIX)/lib/$(LOCAL_MODULE).so
include $(PREBUILT_SHARED_LIBRARY)

'''
assert text.count(block) == 1
makefile.write_text(text.replace(block, ''))
PY

# Upstream leaves these three dependencies on moving default branches.
# Populate their directories first so download-deps.sh preserves these pins.
pin_dependency() {
    local name=$1 url=$2 revision=$3
    local target="$native_dir/buildscripts/deps/$name"
    git clone --no-checkout "$url" "$target"
    git -C "$target" checkout --detach "$revision"
    git -C "$target" submodule update --init --recursive --depth 1
}
mkdir -p "$native_dir/buildscripts/deps"
pin_dependency dav1d https://github.com/videolan/dav1d.git 32cf02af50f32af108a3b281c452788dccdac648
pin_dependency libass https://github.com/libass/libass.git e46aedea0a0d17da4c4ef49d84b94a7994664ab5
pin_dependency libplacebo https://github.com/haasn/libplacebo.git 1fd3c7bde7b943fe8985c893310b5269a09b46c5

cd "$native_dir/buildscripts"
IN_CI=1 bash include/download-deps.sh

# Archive the source versions, patches and licence documents even on failure.
git -C "$native_dir" diff > "$output_dir/evidence/native-build.patch"
cp include/depinfo.sh "$output_dir/evidence/dependency-versions.sh"
for dependency in deps/*; do
    [[ -d "$dependency" ]] || continue
    name=$(basename "$dependency")
    mkdir -p "$output_dir/licenses/$name"
    find "$dependency" -maxdepth 2 -type f \( -iname '*license*' -o -iname '*copying*' -o -name Copyright -o -name NOTICE \) \
        -exec cp --parents '{}' "$output_dir/licenses/$name/" \;
    if [[ -d "$dependency/.git" ]]; then
        printf '%s %s\n' "$name" "$(git -C "$dependency" rev-parse HEAD)" >> "$output_dir/evidence/source-revisions.txt"
        git -C "$dependency" submodule status --recursive >> "$output_dir/evidence/source-revisions.txt"
    fi
done

# The upstream script already sets the 16 KB linker page size. Check every
# resulting shared library independently instead of relying on that flag.
bash buildall.sh --arch arm64 mpv
prefix_dir="$native_dir/buildscripts/prefix/arm64"
ndk_dir="$sdk_dir/ndk/$ndk_version"
PREFIX64="$prefix_dir" "$ndk_dir/ndk-build" -C "$native_dir/app/src/main" -j2

mkdir -p "$output_dir/jniLibs/arm64-v8a" "$output_dir/include"
cp -L "$prefix_dir"/lib/*.so "$output_dir/jniLibs/arm64-v8a/"
cp -L "$native_dir"/app/src/main/libs/arm64-v8a/*.so "$output_dir/jniLibs/arm64-v8a/"
cp -R "$prefix_dir/include/mpv" "$output_dir/include/"
cp deps/ffmpeg/_build-arm64/config.h "$output_dir/evidence/ffmpeg-config.h"
cp deps/ffmpeg/_build-arm64/ffbuild/config.mak "$output_dir/evidence/ffmpeg-config.mak"
cp deps/mpv/_build-arm64/config.h "$output_dir/evidence/mpv-config.h"
cp deps/mpv/_build-arm64/meson-info/intro-buildoptions.json "$output_dir/evidence/mpv-buildoptions.json"

python3 "$project_dir/scripts/anime-native/audit.py" "$output_dir"
