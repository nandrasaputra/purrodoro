# Soundscape Admin (debug, local-only)

A tiny Vite + TypeScript SPA for managing the soundscape catalogs without an app
release: the **remote** cloud catalog (`soundscape_tracks`) and the **local**
APK override table (`soundscape_bundled_catalog`).

> ⚠️ **Security — dev-server only.** This tool uses the Supabase **service role
> key** and R2 **write credentials**, which bypass normal access controls. Vite
> inlines any `VITE_*` env var into the client bundle at build time, so **never
> run `npm run build` / serve `dist/`** — that would ship secrets in plaintext
> JS. Use `npm run dev` locally only. Keep secrets in `.env` / `.env.local`
> (gitignored) and never commit them.

## Prerequisites

- Node 20+
- Supabase **service role** key (dashboard → Project Settings → API)
- Cloudflare R2 **read/write** API token scoped to `purrodoro-soundscapes`

## Setup

```bash
cd tools/soundscape-admin
cp .env.example .env.local   # paste Supabase + R2 credentials
npm install
npm run dev                  # http://localhost:5173
```

## Remote workflow (cloud tracks, ids ≥ 100)

- **Add:** pick kind, fill name/artist/symbol/sort, choose an MP3 → uploads to
  **Cloudflare R2** at `{lofi|ambience}/<filename>` and inserts a Postgres row.
- **Toggle visible / lock:** flips `is_active` / `is_premium` in Supabase.
- **Soft-delete:** set the track to hidden (the visible toggle). Don't reuse ids.
- **Preview:** inline `<audio>` plays the public R2 URL.

## Local workflow (APK-bundled tracks)

The audio ships inside the APK; this table only **overrides** metadata. You can
edit `is_active` (visibility), `is_premium` (lock), and `sort_order`. You cannot
upload or delete bytes here.

The source-of-truth mapping of APK raw files ↔ string keys is
[`bundled-manifest.json`](./bundled-manifest.json). See `docs/CLOUD_ASSET_MANAGEMENT.md`
for the `l_amb_*` / `l_lofi_*` (APK) vs `amb_*` / `lofi_*` (R2) naming table.
