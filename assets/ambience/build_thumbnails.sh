#!/usr/bin/env bash
# Build crisp ambience tiles for the Sounds sheet (3× density for 84dp tiles).
#
# Place source PNGs in assets/ambience/thumbnails/ named:
#   rain.png  cafe.png  fireplace.png  forest.png  sea_wave.png  winter_wind.png
#
# Usage (from repo root):
#   assets/ambience/build_thumbnails.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SRC="$ROOT/assets/ambience/thumbnails"
DRAWABLE="$ROOT/purrodoro-android/app/src/main/res/drawable"
# 84dp × 3 density ≈ 252px; 384 gives headroom for crop + sharp downscale.
EXPORT_SIZE=384
WEBP_QUALITY=92

mkdir -p "$SRC" "$DRAWABLE"

to_webp() {
  local input="$1"
  local output="$2"
  magick "$input" \
    -resize "${EXPORT_SIZE}x${EXPORT_SIZE}^" \
    -gravity center \
    -extent "${EXPORT_SIZE}x${EXPORT_SIZE}" \
    -strip \
    -define webp:method=6 \
    -quality "$WEBP_QUALITY" \
    "$output"
  local bytes
  bytes="$(stat -f%z "$output" 2>/dev/null || stat -c%s "$output")"
  printf "  %s  (%'d bytes)\n" "$(basename "$output")" "$bytes"
}

declare -a PAIRS=(
  "rain:soundscape_ambience_rain.webp"
  "cafe:soundscape_ambience_cafe.webp"
  "fireplace:soundscape_ambience_fireplace.webp"
  "forest:soundscape_ambience_forest.webp"
  "sea_wave:soundscape_ambience_sea_wave.webp"
  "winter_wind:soundscape_ambience_winter_wind.webp"
)

echo "Building ambience thumbnails (${EXPORT_SIZE}px → WebP)…"
for pair in "${PAIRS[@]}"; do
  stem="${pair%%:*}"
  out_name="${pair##*:}"
  input="$SRC/${stem}.png"
  if [[ ! -f "$input" ]]; then
    echo "missing $input" >&2
    exit 1
  fi
  to_webp "$input" "$DRAWABLE/$out_name"
done
echo "Done."
