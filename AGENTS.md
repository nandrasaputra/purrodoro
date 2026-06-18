# Purrodoro

Purrodoro is a Pomodoro / focus app with a cat mascot. The repo is multi-platform:

| Path | What it is | Buildable in Cursor Cloud (Linux)? |
|---|---|---|
| `purrodoro-android/` | The Android app (Kotlin, Jetpack Compose, Gradle). The primary product. | Yes |
| `purrodoro-ios/` | The iOS app (Swift / Xcode project). | No — requires macOS + Xcode |
| `supabase/` | Backend schema (`migrations/`) for the hosted Supabase project (sync, auth, soundscape catalog). | Yes, via local Supabase stack |
| `tools/soundscape-admin/` | A Vite + TypeScript SPA (debug, local-only) to manage the soundscape catalog tables. | Yes |

## Cursor Cloud specific instructions

The update script only runs `npm install` for `tools/soundscape-admin`. Everything
below (Android SDK, Docker, the Supabase CLI, and the local Supabase stack) is
installed/configured in the VM image — you generally do **not** need to reinstall it,
but daemons/services are **not** auto-started, so start them as noted.

### Android app (`purrodoro-android/`) — the main product

- Toolchain: JDK 21, Gradle 9.2.1 (wrapper), AGP 9.0.1, Kotlin 2.1.20, `compileSdk = 36.1`.
- `ANDROID_HOME` / `ANDROID_SDK_ROOT` are exported from `~/.bashrc` (→ `$HOME/android-sdk`),
  and `purrodoro-android/local.properties` (gitignored) points `sdk.dir` at the same path.
  If a build can't find the SDK, recreate `local.properties` with `sdk.dir=$HOME/android-sdk`.
- Run everything from the `purrodoro-android/` directory:
  - Unit tests (core timer/sync/soundscape logic, Robolectric): `./gradlew :app:testDebugUnitTest` (304 tests).
  - Build the debug APK: `./gradlew :app:assembleDebug` (output: `app/build/outputs/apk/debug/app-debug.apk`).
  - Lint: `./gradlew :app:lintDebug`.
- There is no Android emulator/device in this environment. Validate Android changes with the
  unit-test suite (it covers the Pomodoro state machine, sync engine, and soundscape catalog),
  not by launching the app GUI.

### Soundscape admin web tool (`tools/soundscape-admin/`)

- Dev-only tool. Standard scripts live in `package.json`: `npm run dev` (serves on
  `http://localhost:5173`), `npm run build` (`tsc && vite build` — typecheck + bundle).
- It needs `tools/soundscape-admin/.env.local` (gitignored) with `VITE_SUPABASE_URL` and
  `VITE_SUPABASE_SERVICE_ROLE_KEY`. For local dev, point these at the local Supabase stack
  (see below). Never run `npm run build`/serve `dist/` against a real key — Vite inlines
  `VITE_*` vars into client JS (see the tool's `README.md`).

### Local Supabase backend (for the admin tool / migrations)

The repo ships only `supabase/migrations/` (the real app uses a hosted project). To run a
local backend, a `supabase/config.toml` is generated locally via `supabase init` (gitignored,
not committed). Then:

- Docker is required and is **not** running on boot. Start it once per session:
  `sudo dockerd > /tmp/dockerd.log 2>&1 &` then `sudo chmod 666 /var/run/docker.sock`.
  Docker 29 is configured (`/etc/docker/daemon.json`) with the `fuse-overlayfs` storage driver
  and `containerd-snapshotter` disabled — both are required for Docker-in-Docker here.
- Start the stack from the repo root: `supabase start` (applies migrations, seeds the bundled
  soundscape catalog). `supabase status` prints the API URL and `SERVICE_ROLE_KEY` to put in
  the admin tool's `.env.local`.
- Non-obvious quirk: the local stack does **not** auto-grant table privileges to the Supabase
  roles the way the hosted project does, so REST queries return `42501 permission denied`.
  After `supabase start`, run:
  `docker exec supabase_db_workspace psql -U postgres -d postgres -c "GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role; GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated, service_role; GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated, service_role; ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO anon, authenticated, service_role;"`
