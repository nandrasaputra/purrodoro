# Cloud Soundscapes — Asset Management

How to add, replace, and remove downloadable soundscape tracks (music + ambience)
without shipping an app update. This is the permanent operations reference for the
**Cloud Soundscapes** feature.

> Source of truth for the schema: [`supabase/migrations/0004_soundscape_catalog.sql`](../supabase/migrations/0004_soundscape_catalog.sql).
> Client behavior: `purrodoro-android/.../soundscape/SoundscapeConfig.kt`,
> `audio/TrackDownloadManager.kt`, `tools/soundscape-admin/src/r2.ts`.
> If this doc and the code/migration disagree, the code wins — update this file.

---

## How it works (the model)

- A Postgres table, `public.soundscape_tracks`, is the **single source of truth** for the
  cloud catalog. One row = one downloadable track.
- The audio files live in a **public Cloudflare R2** bucket (`purrodoro-soundscapes`).
- The app reads the catalog (active rows only), caches it in Room, and downloads files
  **on demand** to internal storage. Users can delete a download to reclaim space.
- The catalog refresh is a **full snapshot**: every app start replaces the whole local
  cache with the server's active rows. So **adding** a row, **deactivating** it, or
  **replacing** its audio all propagate automatically on the next launch — no app release.

### Key rules (do not break these)

| Rule | Why |
|---|---|
| Track `id` must be **≥ 100** and **globally unique forever** | ids `< 100` are reserved for tracks bundled in the APK. Saved user mixes (`soundscape_mix_*` in DataStore) reference ids — **never reuse a retired id** for a different track. |
| `kind` is `'music'` or `'ambience'` | drives which UI section renders the track. |
| `file_path` must match the uploaded object path exactly | the client builds the download URL from it. |
| `file_size_bytes` should match the real file size | used for the download-size label and as a progress fallback. |
| Bumping `updated_at` invalidates the local file | the local filename is version-stamped, so a bump forces a re-download. |

---

## Backend reference

| Thing | Value |
|---|---|
| Supabase project | `purrodoro` (ref `birjkyerhokqdzhdfgpt`) |
| Project URL | `https://birjkyerhokqdzhdfgpt.supabase.co` |
| Catalog table | `public.soundscape_tracks` (Supabase Postgres) |
| Audio bucket | Cloudflare R2 `purrodoro-soundscapes` (public read) |
| Public object URL | `{R2_PUBLIC_URL}/{file_path}` — see `SoundscapeConfig.R2_PUBLIC_URL` in the Android app |
| Local file on device | `filesDir/soundscapes/{id}-v{updated_at_millis}.mp3` |

Catalog writes go through the **service role** (admin tool or SQL). Audio uploads go to
**R2** (admin tool or S3-compatible CLI). Reads are public — the catalog must be
readable before login.

> **Legacy:** migration `0004_soundscape_catalog.sql` also created a Supabase Storage
> bucket `soundscape-audio`. Audio bytes now live on R2 only; the Storage bucket is
> unused and may be removed from the project when convenient.

### Table columns

```
id              int        primary key, must be >= 100
kind            text       'music' | 'ambience'
name            text       track title
artist          text       music: artist name; ambience: short description under the name
symbol          text       ambience only: symbol label the UI renders on the tile
file_path       text       object key in R2, e.g. 'lofi/lofi_rainy_window.mp3'
thumbnail_path  text       R2 object key for tile art, e.g. 'lofi/lofi_rainy_window.webp'
file_size_bytes bigint     exact file size in bytes
is_premium      boolean    true = locked behind Purrodoro Premium
sort_order      int        ascending position within its section
is_active       boolean    false = hidden from clients (soft removal)
updated_at      timestamptz auto-maintained; bump to force re-download
```

---

## Preparing audio files

- Format: **mp3**, ideally **128–192 kbps** (focus loops don't need more; a ~3-min loop
  lands around 2–3 MB).
- Use **lowercase snake_case** filenames with kind prefixes (see table below):
  `lofi/lofi_rainy_window.mp3`, `ambience/amb_ocean_waves.mp3`.
- Pair each MP3 with a **384×384 WebP thumbnail** at the same path stem:
  `lofi/lofi_rainy_window.webp`. Set `thumbnail_path` in the catalog row (the
  admin tool derives it automatically from the MP3 filename).
- The track must **loop seamlessly** — trim so the end joins cleanly to the start.
- Note the exact byte size for `file_size_bytes`:

```bash
ls -l path/to/rainy_window.mp3        # bytes are the 5th column
# or
stat -f%z path/to/rainy_window.mp3    # macOS, bytes only
```

Source candidates live in the repo's `assets/lofi/`, `assets/ambience/`, and
`assets/soundscape/` folders.

---

## Add a track

1. **Upload the file** to R2 under `lofi/` or `ambience/` (use the soundscape-admin
   **Upload + add** form, or any S3-compatible client pointed at the R2 endpoint).
2. **Insert the catalog row** with a fresh id ≥ 100, the exact `file_path`, and the real
   byte size. SQL editor:

   ```sql
   insert into public.soundscape_tracks
     (id, kind, name, artist, symbol, file_path, file_size_bytes, is_premium, sort_order)
   values
     (100, 'music',    'Rainy Window', 'Artist Name', '',     'lofi/lofi_rainy_window.mp3',   2354176, false, 10),
     (101, 'music',    'Night Drive',  'Artist Name', '',     'lofi/lofi_night_drive.mp3',    2104320, true,  20),
     (102, 'ambience', 'Ocean Waves',  'Gentle surf', 'Wave', 'ambience/amb_ocean_waves.mp3', 812032, false, 10);
   ```

   - `is_premium = true` locks the track behind Premium.
   - `sort_order` positions it within its section (lower = earlier).
3. Done. Clients pick it up on the next app start — no release needed.

---

## Remove a track

Two options:

**Soft remove (recommended, reversible):**

```sql
update public.soundscape_tracks set is_active = false where id = 101;
```

**Hard remove:**

```sql
delete from public.soundscape_tracks where id = 101;
```

Either way, on the next refresh the client drops the track from the list **and
auto-deletes the local file** (the prune pass removes files that no longer match a
current catalog row). Optionally delete the R2 object afterwards (S3-compatible
delete or Cloudflare dashboard) to save storage.

> Reactivating a soft-removed track (`is_active = true`) brings it straight back. A
> hard-deleted row is gone, but its **id stays retired** — never reuse it.

---

## Replace a track's audio

1. Upload the new file — either over the same path, or to a new path and update
   `file_path`.
2. **Bump `updated_at`** so clients see the change. Any update fires the moddatetime
   trigger; update the size too (it changed):

   ```sql
   update public.soundscape_tracks
   set file_size_bytes = 2500000   -- the new file's size; this also bumps updated_at
   where id = 100;
   ```

3. The bumped `updated_at` changes the expected local filename
   (`{id}-v{updated_at_millis}.mp3`), so the client treats it as not-downloaded and
   re-downloads on the next refresh. The stale old file is pruned automatically.

---

## Edit metadata (no audio change)

Renaming, re-describing, re-ordering, or toggling premium is a plain update:

```sql
update public.soundscape_tracks
set name = 'Rainy Window (Night)',
    sort_order = 5,
    is_premium = true
where id = 100;
```

The `updated_at` bump is harmless here — the `file_path` is unchanged, so the file is
re-fetched only if it wasn't already downloaded. (If you want to avoid any re-download,
note that metadata-only edits still bump `updated_at`; the client keys the local file on
`updated_at`, so a metadata edit **will** trigger a re-download. Batch metadata edits
when you can.)

---

## Verifying changes

**Catalog read as the public (anon) role** — should return only `is_active = true` rows:

```bash
curl "$PROJECT_URL/rest/v1/soundscape_tracks?select=*" \
  -H "apikey: $SUPABASE_PUBLISHABLE_KEY"
```

**Public object serves correctly** (R2):

```bash
curl -I "https://pub-a2af288fd41b4193b1cd198233eb484d.r2.dev/lofi/lofi_cedar_rain_study.mp3"
# expect: HTTP/2 200, content-type: audio/mpeg
```

Use `SoundscapeConfig.R2_PUBLIC_URL` in the Android app or `VITE_R2_PUBLIC_URL`
in the admin tool — they must match the enabled public bucket URL.

**On a device:**

1. Relaunch the app → the track appears in the Sounds sheet after the bundled ones, with
   a cloud badge + size.
2. Tap → progress ring → badge clears → tap again plays it; it loops.
3. Airplane mode + relaunch → catalog still listed (Room cache); downloaded track still
   plays.
4. Expanded grid → trash icon removes the download; tile returns to the cloud badge.
5. After a soft-remove + relaunch, confirm the local file is gone:
   ```bash
   adb shell run-as com.endiar.purrodoro ls files/soundscapes
   ```

---

## Costs to watch

- Cloud audio is on **Cloudflare R2** (zero egress to the public bucket). Supabase still
  hosts the lightweight catalog rows only.

---

## Quick checklist

- [ ] mp3 is 128–192 kbps and loops cleanly
- [ ] uploaded to R2 `lofi/` or `ambience/` with a snake_case name
- [ ] row inserted with id ≥ 100 (never reused), exact `file_path`, real `file_size_bytes`
- [ ] `kind`, `is_premium`, `sort_order` set intentionally
- [ ] verified via anon catalog `curl` + R2 object `curl -I` + on-device relaunch

---

## Debug admin (recommended)

Instead of hand-writing SQL in the Supabase dashboard, use the local debug admin
at [`tools/soundscape-admin/`](../tools/soundscape-admin/). It manages both
catalogs from one screen:

- **Remote tracks** (`soundscape_tracks`): upload MP3 to **R2** + insert a Postgres
  row, toggle visibility/premium, soft-delete, and preview playback.
- **Local tracks** (`soundscape_bundled_catalog`): edit visibility/premium/order
  for APK-bundled tracks (no byte upload — audio ships in `res/raw`).

See the tool's [README](../tools/soundscape-admin/README.md) for setup. It is
**dev-server only** — uses the Supabase service role key and R2 write credentials;
never build/deploy it (`npm run build` would inline secrets).

### Local (bundled) catalog table

`public.soundscape_bundled_catalog`, keyed by `(kind, local_id)`, controls
visibility / premium / order / display overrides for the tracks bundled in the
APK. Schema source of truth:
[`supabase/migrations/0005_soundscape_bundled_catalog.sql`](../supabase/migrations/0005_soundscape_bundled_catalog.sql).

| Rule | Why |
|---|---|
| `local_id` is **1–99**, unique per `kind` | mirrors the APK `BundledTrackRegistry` ids; remote ids stay ≥ 100. |
| `raw_resource` matches an APK `res/raw` stem | the app maps it to `R.raw.*`; bytes are never in Supabase. Bundled stems use **`l_amb_*`** or **`l_lofi_*`** (see naming table). |
| `is_active = false` **hides** a bundled track | the app reads all rows (RLS allows it) to tell hidden from unseeded. |
| A bundled track with **no row** stays **visible** | the on-device registry is the source of existence; a new APK track isn't swallowed by an unseeded table. |
| `name` / `artist` / `symbol` empty → use `strings.xml` | non-blank values override the localized defaults. |

### Soundscape filename prefixes

Filenames encode **kind** and **delivery** so local APK audio, cloud uploads, and
source folders are easy to tell apart at a glance.

| Delivery | Ambience | Lo-fi music |
|---|---|---|
| **Local** (APK `res/raw`) | `l_amb_*` — e.g. `l_amb_rain.mp3` | `l_lofi_*` — e.g. `l_lofi_desk_lamp_rain.mp3` |
| **Cloud** (R2 `ambience/` or `lofi/`) | `amb_*` — e.g. `ambience/amb_ocean_waves.mp3` | `lofi_*` — e.g. `lofi/lofi_cedar_rain_study.mp3` |

`l_` = *local* (bundled in the APK). Cloud objects drop the `l_` but keep the
kind prefix (`amb_` / `lofi_`).

**Bundled premium defaults (current):**

| Kind | Example local stem | Premium default |
|---|---|---|
| ambience | `l_amb_rain`, `l_amb_cafe`, `l_amb_fireplace`, `l_amb_forest`, `l_amb_sea_wave`, `l_amb_winter_wind` | Rain, Cafe, Fireplace, Ocean Shore free; Forest, Winter Wind premium |
| music | `l_lofi_desk_lamp_rain`, `l_lofi_mosswood_morning`, … | local ids 1–4 free, 5–6 premium |

Compressed cloud lo-fi masters for upload live outside the repo (e.g.
`~/Downloads/lofi-compressed/`) as `lofi_*.mp3`; the six APK tracks are
`l_lofi_*.mp3`. Do not rename lossless originals — only the compressed export folder.

Source of truth: [`BundledTrackRegistry.kt`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/soundscape/BundledTrackRegistry.kt), [`SoundscapeConfig.kt`](../purrodoro-android/app/src/main/java/com/endiar/purrodoro/soundscape/SoundscapeConfig.kt), and [`bundled-manifest.json`](../tools/soundscape-admin/bundled-manifest.json).
After renaming bundled stems, run migrations `0007`–`0009` on Supabase so
`soundscape_bundled_catalog.raw_resource` stays in sync.

### Cloud lo-fi seed (current)

Twenty cloud lo-fi tracks ship as R2 objects under `lofi/lofi_*.mp3` with
catalog rows **ids 100–119** (migration `0011_seed_cloud_lofi_catalog.sql`).
All are free (`is_premium = false`) with `sort_order` 10–200. The six APK-bundled
lo-fi tracks (ids 1–6) are separate and are **not** duplicated in the cloud catalog.

### Cloud ambience seed (current)

Two cloud ambience tracks ship as R2 objects under `ambience/amb_*.mp3` with
catalog rows **ids 120–121** (migration `0012_seed_cloud_ambience_catalog.sql`):

| id | name | symbol | R2 key |
|---|---|---|---|
| 120 | Storm | Storm | `ambience/amb_storm.mp3` |
| 121 | Night Crickets | Cricket | `ambience/amb_night_cricket.mp3` |

Both are free and appear after the six bundled ambience tiles on the Sounds sheet.
