# Soundscape Admin + Unified Local/Remote Catalog — Design

**Date:** 2026-06-17

## Problem

Cloud soundscapes are managed manually via Supabase Dashboard + SQL. Bundled (APK) tracks are hardcoded in `PurrodoroApp.kt` with no remote visibility control. There is no unified view of local vs remote assets.

## Product model

Every track in the Sounds sheet is either **local** (bytes in APK) or **remote**
(bytes in Cloudflare R2, downloaded on demand). Catalog metadata for remote
tracks lives in Supabase Postgres (`soundscape_tracks`).

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

- `file_path` → R2 object key (e.g. `lofi/lofi_cedar_rain_study.mp3`)
- `is_active` → visibility
- `is_premium` → lock
- `sort_order` → section order

### Local — new table

`public.soundscape_bundled_catalog` keyed by `(kind, local_id)`:

- `raw_resource` → APK `R.raw.*` stem (e.g. `l_amb_rain`, `l_lofi_desk_lamp_rain`)
- `is_active` → visibility (see note below)
- `is_premium` → lock
- `sort_order` → section order
- `name`, `artist`, `symbol` → optional display overrides (empty = use `strings.xml` in app)

APK bytes are **not** in Supabase. Admin cannot delete bundled bytes without an app release.

**Existence vs override.** The on-device `BundledTrackRegistry` (the APK
defaults) is the source of *existence*; this table only *overrides*
visibility/premium/order/display. A bundled track with no matching row defaults
to **visible**, so shipping a new APK track is never silently swallowed by an
unseeded table. To *hide* a track, the admin sets `is_active = false`. Because
the app must tell "hidden" apart from "not yet seeded", it reads **all** bundled
rows (active and inactive) — the table's RLS allows public read of every row
(it exposes only flags/metadata, never audio bytes). The remote
`soundscape_tracks` RLS is unchanged: still active-only.

### Repo manifest

`tools/soundscape-admin/bundled-manifest.json` documents the APK mapping (raw file, string resource keys, default premium). Used to seed the DB and validate `res/raw/` parity. Filename prefixes: **`l_amb_*`** / **`l_lofi_*`** (local APK), **`amb_*`** / **`lofi_*`** (cloud R2) — see `docs/CLOUD_ASSET_MANAGEMENT.md`.

## Debug admin web app

Local-only SPA at `tools/soundscape-admin/`:

- **Remote panel:** list (incl. inactive), add (upload MP3 to R2 + insert Postgres row), edit metadata, toggle visibility/premium, soft-delete, preview/play
- **Local panel:** list APK-mapped tracks, edit visibility/premium/order (no upload)
- Auth: Supabase **service role** key + Cloudflare R2 **write** credentials in
  `.env` / `.env.local` (gitignored)

Not deployed publicly; not shipped in the app. **Dev-server only** — never
`npm run build`/serve `dist/`, since Vite inlines `VITE_*` env vars (the
RLS-bypassing service role key) into client JS.

## Android changes

1. Fetch `soundscape_bundled_catalog` on startup alongside remote catalog; cache in Room.
2. `SoundscapeCatalogBuilder` (pure) merges the bundled registry + cached
   overrides + remote Room rows into `MusicOption` / `SoundOption` lists.
   Delivery is carried by the existing `RemoteTrackInfo? == null` (no separate
   delivery type). The builder iterates the registry defaults and left-joins the
   cached row by `(kind, localId)`: missing row → visible default; inactive row
   → hidden; non-blank `name`/`artist`/`symbol` override the `strings.xml`
   defaults. Bundled tracks render before remote tracks.
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
