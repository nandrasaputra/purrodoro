# Soundscape Admin + Unified Local/Remote Catalog — Design

**Date:** 2026-06-17

## Problem

Cloud soundscapes are managed manually via Supabase Dashboard + SQL. Bundled (APK) tracks are hardcoded in `PurrodoroApp.kt` with no remote visibility control. There is no unified view of local vs remote assets.

## Product model

Every track in the Sounds sheet is either **local** (bytes in APK) or **remote** (bytes in Supabase Storage, downloaded on demand).

| Concern | Local | Remote |
|---|---|---|
| Visibility | yes | yes |
| Premium lock | yes | yes |
| Download state | N/A (always ready) | NotDownloaded / Downloading / Downloaded |

**Local:** user taps → plays immediately (if visible and unlocked).

**Remote:** user taps when not downloaded → download; when downloaded → play; user can delete download later.

## Data architecture

### Remote — unchanged table

`public.soundscape_tracks` (ids ≥ 100) maps cloud MP3s + metadata:

- `file_path` → Storage object
- `is_active` → visibility
- `is_premium` → lock
- `sort_order` → section order

### Local — new table

`public.soundscape_bundled_catalog` keyed by `(kind, local_id)`:

- `raw_resource` → APK `R.raw.*` stem (e.g. `ambience_rain`)
- `is_active` → visibility
- `is_premium` → lock
- `sort_order` → section order
- `name`, `artist`, `symbol` → optional display overrides (empty = use `strings.xml` in app)

APK bytes are **not** in Supabase. Admin cannot delete bundled bytes without an app release.

### Repo manifest

`tools/soundscape-admin/bundled-manifest.json` documents the APK mapping (raw file, string resource keys, default premium). Used to seed the DB and validate `res/raw/` parity.

## Debug admin web app

Local-only SPA at `tools/soundscape-admin/`:

- **Remote panel:** list (incl. inactive), add (upload MP3 + insert row), edit metadata, toggle visibility/premium, soft-delete, preview/play
- **Local panel:** list APK-mapped tracks, edit visibility/premium/order (no upload)
- Auth: Supabase **service role** key in `.env.local` (gitignored)

Not deployed publicly; not shipped in the app.

## Android changes

1. Fetch `soundscape_bundled_catalog` on startup alongside remote catalog; cache in Room.
2. `SoundscapeCatalogBuilder` merges local registry + remote Room rows into `MusicOption` / `SoundOption` lists.
3. Remove hardcoded bundled lists from `PurrodoroApp.kt`.
4. Offline: use last cached bundled overrides; fall back to code defaults if never fetched.

## Out of scope

- IAP billing
- Localized names from admin (keep `strings.xml` for i18n)
- Removing bundled bytes from APK via admin
- Hosted/deployed admin UI
- Promote local → remote workflow (follow-up)

## References

- `docs/CLOUD_ASSET_MANAGEMENT.md`
- `supabase/migrations/0004_soundscape_catalog.sql`
- `purrodoro-android/.../soundscape/`, `audio/TrackDownloadManager.kt`
