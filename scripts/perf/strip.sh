#!/usr/bin/env bash
# Contact sheet from a screen recording: scripts/perf/strip.sh <video.mp4> <start s> <length s> <fps> <out.png> [cols]
# Record first: adb -s <serial> shell screenrecord --time-limit 8 --bit-rate 12000000 /sdcard/x.mp4 (MSYS_NO_PATHCONV=1)
set -euo pipefail
cols=${6:-6}
ffmpeg -hide_banner -loglevel error -y -ss "$2" -t "$3" -i "$1" -vf "fps=$4,scale=240:-1,tile=${cols}x$(( ( ${3%.*} * $4 + cols ) / cols + 1 ))" \
  -frames:v 1 "$5"
echo "$5"
