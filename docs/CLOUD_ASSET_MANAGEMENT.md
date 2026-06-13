# Cloud Soundscapes — Asset Management

How to add, replace, and remove downloadable soundscape tracks (music + ambience)
without shipping an app update. This is the permanent operations reference for the
**Cloud Soundscapes** feature.

> Source of truth for the schema: [`supabase/migrations/0004_soundscape_catalog.sql`](../supabase/migrations/0004_soundscape_catalog.sql).
> Client behavior: `purrodoro-android/.../soundscape/` + `audio/TrackDownloadManager.kt`.
> If this doc and the code/migration disagree, the code wins — update this file.

---

## How it works (the model)

- A Postgres table, `public.soundscape_tracks`, is the **single source of truth** for the
  cloud catalog. One row = one downloadable track.
- The audio files live in a **public** Storage bucket, `soundscape-audio`.
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
| Catalog table | `public.soundscape_tracks` |
| Storage bucket | `soundscape-audio` (public read) |
| Public object URL | `{PROJECT_URL}/storage/v1/object/public/soundscape-audio/{file_path}` |
| Local file on device | `filesDir/soundscapes/{id}-v{updated_at_millis}.mp3` |

Writes to the table and bucket go through the **dashboard or the service role only**
(there are no insert/update/delete RLS policies). Reads are public — the catalog must be
readable before login.

### Table columns

```
id              int        primary key, must be >= 100
kind            text       'music' | 'ambience'
name            text       track title
artist          text       music: artist name; ambience: short description under the name
symbol          text       ambience only: symbol label the UI renders on the tile
file_path       text       object path inside the bucket, e.g. 'lofi/rainy_window.mp3'
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
- Use **lowercase snake_case** filenames and group by kind:
  `lofi/rainy_window.mp3`, `ambience/ocean_waves.mp3`.
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

1. **Upload the file.** Dashboard → Storage → `soundscape-audio` → into `lofi/` or
   `ambience/` (create the folder if needed) → upload the mp3.
2. **Insert the catalog row** with a fresh id ≥ 100, the exact `file_path`, and the real
   byte size. SQL editor:

   ```sql
   insert into public.soundscape_tracks
     (id, kind, name, artist, symbol, file_path, file_size_bytes, is_premium, sort_order)
   values
     (100, 'music',    'Rainy Window', 'Artist Name', '',     'lofi/rainy_window.mp3',   2354176, false, 10),
     (101, 'music',    'Night Drive',  'Artist Name', '',     'lofi/night_drive.mp3',    2104320, true,  20),
     (102, 'ambience', 'Ocean Waves',  'Gentle surf', 'Wave', 'ambience/ocean_waves.mp3', 812032, false, 10);
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
current catalog row). Optionally delete the Storage object afterwards to save storage:
Dashboard → Storage → `soundscape-audio` → delete the file.

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

**Public object serves correctly:**

```bash
curl -I "$PROJECT_URL/storage/v1/object/public/soundscape-audio/lofi/rainy_window.mp3"
# expect: HTTP/2 200, content-type: audio/mpeg
```

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

- Supabase egress is **5 GB/month** on the free tier. Each download of a ~2 MB track
  costs ~2 MB of egress.
- If the catalog scales, move the files to **Cloudflare R2** (zero egress); only the base
  URL in `TrackDownloadManager` changes.

---

## Quick checklist

- [ ] mp3 is 128–192 kbps and loops cleanly
- [ ] uploaded to `soundscape-audio/{lofi|ambience}/...` with a snake_case name
- [ ] row inserted with id ≥ 100 (never reused), exact `file_path`, real `file_size_bytes`
- [ ] `kind`, `is_premium`, `sort_order` set intentionally
- [ ] verified via anon `curl` + on-device relaunch
