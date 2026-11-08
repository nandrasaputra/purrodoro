# Soundscape Admin (debug, local-only)

A tiny Vite + TypeScript SPA for managing the soundscape catalogs without an app
release: the **remote** cloud catalog (`soundscape_tracks`) and the **local**
APK override table (`soundscape_bundled_catalog`).

> **Security — local use only.** A loopback-only Node server holds the Supabase
> service-role key and R2 write credentials; the browser calls its scoped local
> API. These credentials bypass normal access controls. Do not expose the server
> through a proxy or tunnel, or use this tool on an untrusted computer. Malicious
> local software or browser extensions can still compromise it. Production builds
> and previews are disabled. Keep credentials in `.env.local` and never commit them.

## Prerequisites

- Node 20+
- Supabase **service role** key (dashboard → Project Settings → API)
- Cloudflare R2 **read/write** API token scoped to `purrodoro-soundscapes`

## Setup

```bash
cd tools/soundscape-admin
cp .env.example .env.local   # paste Supabase + R2 credentials
npm install
npm run dev                  # http://127.0.0.1:5173
```

### Migrating existing credentials

In your ignored `.env` or `.env.local`, rename `VITE_SUPABASE_URL` to `SUPABASE_URL`,
`VITE_SUPABASE_SERVICE_ROLE_KEY` to `SUPABASE_SERVICE_ROLE_KEY`, and all
`VITE_R2_*` entries to `R2_*`. See `.env.example`. The server refuses to start
while old privileged `VITE_` variables remain in either file or the shell. If credentials were ever exposed
through a published build, network-accessible dev server, or untrusted browser,
rotate them in Supabase and Cloudflare. The thumbnail upload script also reads
the new `R2_*` names from `.env.local`.

## Remote workflow (cloud tracks, ids ≥ 100)

- **Add:** pick kind, fill name/artist/symbol/sort, choose an MP3 (and optional
  WebP/PNG thumbnail) → uploads to **Cloudflare R2** and inserts a Postgres row.
  `thumbnail_path` defaults to the MP3 stem with `.webp`.
- **Thumbnails:** each remote row shows a preview, R2 object path, and a per-row
  **Upload thumb** control to replace the image without re-uploading audio.
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
