#!/usr/bin/env bash
# Normalize loudness and encode ambience loops for the Android APK.
#
# Quality notes:
# - Stereo + 192 kbps (single MP3 encode) — avoids the muffled mono 96k artifacts.
# - loudnorm runs on a lossless WAV intermediate; gain trim + MP3 encode happen once.
#
# Usage (from repo root):
#   assets/ambience/process.sh [source_dir]

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SRC="${1:-$ROOT/assets/ambience/source}"
OUT="$ROOT/assets/ambience/processed"
RAW="$ROOT/purrodoro-android/app/src/main/res/raw"

LOUDNESS_I=-16
LOUDNESS_TP=-1.5
LOUDNESS_LRA=11
TARGET_MEAN_DB=-18.0

# Rain: MP3 ~1 MB, seamless loop, static gain only (no loudnorm).
RAIN_TARGET_BYTES=1000000
RAIN_LOOP_CROSSFADE_SEC=1.0

mkdir -p "$OUT" "$RAW" "$SRC"

# Others: stereo MP3 @ 192k.
MP3_BITRATE=192k
SAMPLE_RATE=44100
CHANNELS=2

TRACKS=(
  "cafe:ambience_cafe.mp3"
  "fireplace:ambience_fireplace.mp3"
  "forest:ambience_forest.mp3"
  "sea-wave:ambience_sea_wave.mp3"
  "winter-wind:ambience_winter_wind.mp3"
)

resolve_input() {
  local stem="$1"
  for ext in wav WAV mp3 MP3; do
    if [[ -f "$SRC/${stem}.${ext}" ]]; then
      echo "$SRC/${stem}.${ext}"
      return 0
    fi
  done
  echo "missing source for $stem in $SRC" >&2
  return 1
}

measure_loudnorm_json() {
  local input="$1"
  ffmpeg -hide_banner -nostats -i "$input" \
    -af "loudnorm=I=${LOUDNESS_I}:TP=${LOUDNESS_TP}:LRA=${LOUDNESS_LRA}:print_format=json" \
    -f null - 2>&1 \
  | python3 -c "
import json, re, sys
blob = sys.stdin.read()
match = re.search(r'\{[^{}]*\"input_i\"[^{}]*\}', blob, re.DOTALL)
if not match:
    sys.exit('loudnorm stats not found')
print(json.dumps(json.loads(match.group(0))))
"
}

mean_volume_db() {
  local input="$1"
  ffmpeg -hide_banner -i "$input" -af volumedetect -f null - 2>&1 \
    | sed -n 's/.*mean_volume: //p' | sed 's/ dB//'
}

encode_rain() {
  local input out_name="ambience_rain.mp3"
  local looped="$OUT/.${out_name}.looped.wav"
  local trimmed="$OUT/.${out_name}.trimmed.wav"
  local final="$OUT/$out_name"
  input="$(resolve_input rain)"

  echo "→ rain ($input) [MP3 ~1 MB, seamless loop, no loudnorm]"

  ffmpeg -hide_banner -loglevel error -y -i "$input" -i "$input" -filter_complex \
    "[0:a][1:a]acrossfade=d=${RAIN_LOOP_CROSSFADE_SEC}:c1=tri:c2=tri[out]" \
    -map "[out]" "$looped"

  local mean_db gain_db
  mean_db="$(mean_volume_db "$looped")"
  gain_db="$(python3 -c "print(f'{$TARGET_MEAN_DB - float(\"$mean_db\"):.2f}')")"

  ffmpeg -hide_banner -loglevel error -y -i "$looped" \
    -af "volume=${gain_db}dB" \
    -c:a pcm_s16le -ar "$SAMPLE_RATE" -ac "$CHANNELS" \
    "$trimmed"

  local duration bitrate_k bytes attempt
  duration="$(ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 "$trimmed")"
  bitrate_k="$(python3 -c "
d = float('$duration')
target = $RAIN_TARGET_BYTES
print(max(128, int(target * 8 / d * 0.96 / 1000)))
")"

  for attempt in 1 2 3 4; do
    ffmpeg -hide_banner -loglevel error -y -i "$trimmed" \
      -codec:a libmp3lame -b:a "${bitrate_k}k" -ar "$SAMPLE_RATE" -ac "$CHANNELS" \
      -write_xing 0 \
      "$final"
    bytes="$(stat -f%z "$final" 2>/dev/null || stat -c%s "$final")"
    if [[ "$bytes" -le $(( RAIN_TARGET_BYTES * 105 / 100 )) ]]; then
      break
    fi
    bitrate_k="$(python3 -c "print(max(128, int($bitrate_k * $RAIN_TARGET_BYTES / $bytes * 0.95)))")"
  done

  rm -f "$looped" "$trimmed" \
    "$RAW/ambience_rain.wav" "$RAW/ambience_rain.ogg" \
    "$OUT/ambience_rain.wav" "$OUT/ambience_rain.ogg"
  cp "$final" "$RAW/$out_name"

  bytes="$(stat -f%z "$final" 2>/dev/null || stat -c%s "$final")"
  mean_db="$(mean_volume_db "$final")"
  printf "   %s  (%'d bytes, mean %s, MP3 %dk)\n" "$out_name" "$bytes" "$mean_db" "$bitrate_k"
}

encode_one() {
  local stem="$1"
  local out_name="$2"
  local bitrate="${3:-$MP3_BITRATE}"
  local input normalized="$OUT/.${out_name}.normalized.wav"
  local trimmed="$OUT/.${out_name}.trimmed.wav"
  local final="$OUT/$out_name"
  input="$(resolve_input "$stem")"

  echo "→ $stem ($input)"

  local stats
  stats="$(measure_loudnorm_json "$input")"
  local measured_i measured_tp measured_lra measured_thresh offset
  measured_i="$(python3 -c "import json,sys; print(json.load(sys.stdin)['input_i'])" <<<"$stats")"
  measured_tp="$(python3 -c "import json,sys; print(json.load(sys.stdin)['input_tp'])" <<<"$stats")"
  measured_lra="$(python3 -c "import json,sys; print(json.load(sys.stdin)['input_lra'])" <<<"$stats")"
  measured_thresh="$(python3 -c "import json,sys; print(json.load(sys.stdin)['input_thresh'])" <<<"$stats")"
  offset="$(python3 -c "import json,sys; print(json.load(sys.stdin)['target_offset'])" <<<"$stats")"

  ffmpeg -hide_banner -loglevel error -y -i "$input" \
    -af "loudnorm=I=${LOUDNESS_I}:TP=${LOUDNESS_TP}:LRA=${LOUDNESS_LRA}:measured_I=${measured_i}:measured_TP=${measured_tp}:measured_LRA=${measured_lra}:measured_thresh=${measured_thresh}:offset=${offset}:linear=true" \
    -ar "$SAMPLE_RATE" -ac "$CHANNELS" \
    "$normalized"

  local mean_db gain_db
  mean_db="$(mean_volume_db "$normalized")"
  gain_db="$(python3 -c "print(f'{$TARGET_MEAN_DB - float(\"$mean_db\"):.2f}')")"

  ffmpeg -hide_banner -loglevel error -y -i "$normalized" \
    -af "volume=${gain_db}dB" \
    -ar "$SAMPLE_RATE" -ac "$CHANNELS" \
    "$trimmed"

  ffmpeg -hide_banner -loglevel error -y -i "$trimmed" \
    -codec:a libmp3lame -b:a "$bitrate" -ar "$SAMPLE_RATE" -ac "$CHANNELS" \
    "$final"

  rm -f "$normalized" "$trimmed"
  cp "$final" "$RAW/$out_name"

  local bytes encode_label
  bytes="$(stat -f%z "$final" 2>/dev/null || stat -c%s "$final")"
  mean_db="$(mean_volume_db "$final")"
  encode_label="stereo ${bitrate}"
  printf "   %s  (%'d bytes, mean %s, %s)\n" "$out_name" "$bytes" "$mean_db" "$encode_label"
}

encode_rain

for entry in "${TRACKS[@]}"; do
  stem="${entry%%:*}"
  out_name="${entry##*:}"
  encode_one "$stem" "$out_name"
done

echo ""
echo "Done. Processed files:"
ls -lh "$OUT"
