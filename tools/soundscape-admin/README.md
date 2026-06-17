# Soundscape Admin (debug, local-only)

A tiny Vite + TypeScript SPA for managing the soundscape catalogs without an app
release: the **remote** cloud catalog (`soundscape_tracks`) and the **local**
APK override table (`soundscape_bundled_catalog`).

> ⚠️ **Security — dev-server only.** This tool uses the Supabase **service role
> key**, which bypasses RLS (full read/write on every table). Vite inlines any
> `VITE_*` env var into the client bundle at build time, so **never run
> `npm run build` / serve `dist/`** — that would ship the key in plaintext JS.
> Use `npm run dev` locally only. Keep the key in `.env.local` (gitignored) and
> never commit it.

## Prerequisites

- Node 20+
- The Supabase **service role** key (Supabase dashboard → Project Settings → API)

## Setup

```bash
cd tools/soundscape-admin
cp .env.example .env.local      # then paste your service role key
npm install
npm run dev                     # http://localhost:5173
```

## Remote workflow (cloud tracks, ids ≥ 100)

- **Add:** pick kind, fill name/artist/symbol/sort, choose an MP3 → it uploads to
  `soundscape-audio/{lofi|ambience}/<filename>` and inserts a row with the next
  free id ≥ 100.
- **Toggle visible / lock:** flips `is_active` / `is_premium`.
- **Soft-delete:** set the track to hidden (the visible toggle). Don't reuse ids.
- **Preview:** the inline `<audio>` plays the public Storage URL.

## Local workflow (APK-bundled tracks)

The audio ships inside the APK; this table only **overrides** metadata. You can
edit `is_active` (visibility), `is_premium` (lock), and `sort_order`. You cannot
upload or delete bytes here.

- Hiding a bundled track: set `is_active = false`. The app reads inactive rows so
  it can tell "hidden" apart from "not yet seeded" (a brand-new APK track with no
  row stays visible by default).

The source-of-truth mapping of APK raw files ↔ string keys is
[`bundled-manifest.json`](./bundled-manifest.json), which also seeds the DB table.
