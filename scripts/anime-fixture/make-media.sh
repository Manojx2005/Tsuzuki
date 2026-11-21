#!/usr/bin/env bash
# Builds the anime fixture media into scripts/anime-fixture/media/ (git-ignored).
# Same inputs give the same files: fixed test patterns, single-threaded x264, bitexact muxing, no dates.
# Usage: bash scripts/anime-fixture/make-media.sh   (FIXTURE_FONT=/path/to/font.ttf to pick the label font)
set -euo pipefail

if ! ffmpeg -version >/dev/null 2>&1; then
  echo "ffmpeg not found on PATH. Install it (winget install Gyan.FFmpeg, apt install ffmpeg, brew install ffmpeg)." >&2
  echo "Card AY0 is blocked until it is: 'blocked: needs ffmpeg on the PC'." >&2
  exit 1
fi

here="$(cd "$(dirname "$0")" && pwd)"
out="$here/media"
mkdir -p "$out/hls"
cd "$out"

# A font for the burnt-in labels. drawtext needs a file; the first one found wins.
font="${FIXTURE_FONT:-}"
if [ -z "$font" ]; then
  for f in /c/Windows/Fonts/arial.ttf /usr/share/fonts/truetype/dejavu/DejaVuSans.ttf \
    /System/Library/Fonts/Supplemental/Arial.ttf /Library/Fonts/Arial.ttf; do
    if [ -f "$f" ]; then font="$f"; break; fi
  done
fi
[ -n "$font" ] || { echo "No label font found; set FIXTURE_FONT=/path/to/font.ttf" >&2; exit 1; }
# ffmpeg filter syntax: Windows drive colons must be escaped (C\:/...).
case "$font" in
  /[a-zA-Z]/*) font="${font:1:1}\\:${font:2}" ;;
esac

q=(-hide_banner -loglevel error -y)
bitexact=(-fflags +bitexact -flags:v +bitexact -flags:a +bitexact -map_metadata -1)
x264=(-c:v libx264 -preset veryfast -pix_fmt yuv420p -x264-params threads=1)
label() { # label <text expression> <size>
  echo "drawtext=fontfile='$font':text='$1':fontsize=$2:fontcolor=white:box=1:boxcolor=black@0.6:boxborderw=12:x=40:y=40"
}

echo "ok.mp4 (60 s, 1280x720, running clock)"
ffmpeg "${q[@]}" -f lavfi -i "testsrc2=size=1280x720:rate=24:duration=60" \
  -f lavfi -i "sine=frequency=440:sample_rate=44100:duration=60" \
  -vf "$(label '%{pts\:hms}' 72)" "${x264[@]}" -crf 36 -g 48 \
  -c:a aac -b:a 48k "${bitexact[@]}" -movflags +faststart ok.mp4

echo "hls/ (1080/720/480 variants, 4 s segments)"
master="#EXTM3U
#EXT-X-VERSION:3"
for v in "1080:1920:2400000" "720:1280:1200000" "480:854:600000"; do
  IFS=: read -r h w bw <<<"$v"
  mkdir -p "hls/$h"
  ffmpeg "${q[@]}" -f lavfi -i "testsrc2=size=${w}x${h}:rate=24:duration=24" \
    -f lavfi -i "sine=frequency=440:sample_rate=44100:duration=24" \
    -vf "$(label "${h}p  %{pts\\:hms}" 64)" "${x264[@]}" -crf 38 -g 96 -keyint_min 96 -sc_threshold 0 \
    -c:a aac -b:a 48k "${bitexact[@]}" \
    -f hls -hls_time 4 -hls_playlist_type vod -hls_segment_filename "hls/$h/seg%03d.ts" "hls/$h/index.m3u8"
  master="$master
#EXT-X-STREAM-INF:BANDWIDTH=$bw,RESOLUTION=${w}x$h
$h/index.m3u8"
done
printf '%s\n' "$master" >hls/master.m3u8

echo "subtitles (en.srt, en.vtt, ja.srt, tracks.ass)"
srt_time() { printf '00:00:%02d,000' "$1"; }
vtt_time() { printf '00:00:%02d.000' "$1"; }
: >en.srt; : >ja.srt; printf 'WEBVTT\n\n' >en.vtt
ja_lines=("これは字幕のテストです" "日本語の字幕が表示されています" "位置は動画の時計と一致します")
for i in $(seq 0 11); do
  s=$((i * 5)); e=$((s + 4)); n=$((i + 1))
  printf '%d\n%s --> %s\nEnglish subtitle %d (%d s)\n\n' "$n" "$(srt_time $s)" "$(srt_time $e)" "$n" "$s" >>en.srt
  printf '%s --> %s\nWebVTT subtitle %d (%d s)\n\n' "$(vtt_time $s)" "$(vtt_time $e)" "$n" "$s" >>en.vtt
  printf '%d\n%s --> %s\n%s（%d 秒）\n\n' "$n" "$(srt_time $s)" "$(srt_time $e)" "${ja_lines[$((i % 3))]}" "$s" >>ja.srt
done
cat >tracks.ass <<'ASS'
[Script Info]
ScriptType: v4.00+
PlayResX: 640
PlayResY: 360

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Default,Arial,28,&H0000FFFF,&H000000FF,&H00000000,&H64000000,-1,0,0,0,100,100,0,0,1,3,1,2,20,20,24,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
Dialogue: 0,0:00:01.00,0:00:19.00,Default,,0,0,0,,{\c&H00FF00&}Styled ASS: Opening
Dialogue: 0,0:00:21.00,0:01:09.00,Default,,0,0,0,,{\i1}Styled ASS: Part A{\i0}
Dialogue: 0,0:01:11.00,0:01:29.00,Default,,0,0,0,,{\b1}Styled ASS: Ending
ASS
cat >chapters.txt <<'META'
;FFMETADATA1
[CHAPTER]
TIMEBASE=1/1000
START=0
END=20000
title=Opening
[CHAPTER]
TIMEBASE=1/1000
START=20000
END=70000
title=Part A
[CHAPTER]
TIMEBASE=1/1000
START=70000
END=90000
title=Ending
META

echo "tracks.mkv (90 s, jpn + eng audio, ASS, three chapters)"
ffmpeg "${q[@]}" -f lavfi -i "testsrc2=size=640x360:rate=24:duration=90" \
  -f lavfi -i "sine=frequency=440:sample_rate=44100:duration=90" \
  -f lavfi -i "sine=frequency=880:sample_rate=44100:duration=90" \
  -i tracks.ass -i chapters.txt \
  -map 0:v -map 1:a -map 2:a -map 3:s -map_chapters 4 -map_metadata -1 \
  -vf "$(label '%{pts\:hms}' 40)" "${x264[@]}" -crf 38 -g 48 \
  -c:a aac -b:a 32k -c:s ass \
  -metadata:s:a:0 language=jpn -metadata:s:a:0 title="Japanese (440 Hz)" \
  -metadata:s:a:1 language=eng -metadata:s:a:1 title="English (880 Hz)" \
  -metadata:s:s:0 language=eng -metadata:s:s:0 title="Styled ASS" \
  -fflags +bitexact -flags:v +bitexact -flags:a +bitexact tracks.mkv
rm -f tracks.ass chapters.txt

echo "thumb-01..03.jpg (640x360 episode previews)"
for n in 1 2 3; do
  ffmpeg "${q[@]}" -f lavfi -i "testsrc2=size=640x360:rate=1:duration=1" \
    -vf "$(label "Fixture preview 0$n" 48)" -frames:v 1 -q:v 4 -fflags +bitexact "thumb-0$n.jpg"
done

total=$(du -sk "$out" | cut -f1)
echo "Done: $out (${total} KB)"
if [ "$total" -gt 20480 ]; then
  echo "Media is over 20 MB; lower the bitrates." >&2
  exit 1
fi
