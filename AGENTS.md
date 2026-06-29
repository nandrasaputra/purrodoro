# AGENTS

Quick orientation for agents working in this repo. Keep entries brief — link to
the source docs for detail.

- **Product reference:** [`docs/PRODUCT.md`](docs/PRODUCT.md)
- **Android release readiness:** [`purrodoro-android/docs/release-readiness.md`](purrodoro-android/docs/release-readiness.md)
- **Design specs:** [`docs/superpowers/specs/`](docs/superpowers/specs/)

## Testing (Android)

Run from `purrodoro-android/`:

```bash
./gradlew testDebugUnitTest      # unit + Robolectric
./gradlew assembleDebug          # compile/build the APK
./gradlew installDebug           # install on a connected device/emulator
```

### Backup & Restore

- **Automated:** `BackupCodecTest`, `AppRepositoryRestoreTest`, `BackupManagerTest`
  cover encode/decode, replace-vs-union restore semantics, the read size cap, and
  safety backups.
- **Manual import:** push a backup JSON to the device, then Profile → Backup &
  restore → Restore (guest/signed-out only). Full steps + sample file in the spec:
  [`docs/superpowers/specs/2026-06-19-data-backup-and-restore-design.md`](docs/superpowers/specs/2026-06-19-data-backup-and-restore-design.md).

  ```bash
  adb push purrodoro-android/build/purrodoro-sample-backup.json /sdcard/Download/
  ```

## Cursor Cloud specific instructions

The Cloud VM is Linux, so what is runnable differs per component:

- **Android (`purrodoro-android`) — primary product, build & test only.** The
  Android SDK is preinstalled at `$HOME/android-sdk` (Platform `android-36.1`,
  Build-Tools `36.1.0`, Platform-Tools) and `ANDROID_HOME`/`PATH` are exported in
  `~/.bashrc`, so login shells (the default) find it. Run the documented Gradle
  commands above from `purrodoro-android/` (the wrapper downloads Gradle 9.2.1 on
  first use). `./gradlew testDebugUnitTest` (340 JUnit/Robolectric tests) and
  `./gradlew assembleDebug` work headless. **No emulator / instrumented tests**:
  there is no `/dev/kvm`, so `connectedDebugAndroidTest` and running the app GUI
  are not possible here. `./gradlew lintDebug` runs but currently fails on a
  pre-existing `MissingPermission` error in `TimerAlarmReceiver.kt` (not an env
  issue; no lint baseline is configured). To preview the actual UI headlessly
  (no emulator), render it with Robolectric `@GraphicsMode(NATIVE)` and draw the
  view to a `Bitmap` — see `WidgetPreviewLayoutExportTest` (run with
  `EXPORT_WIDGET_PREVIEWS=1`) for the pattern.
- **iOS (`purrodoro-ios`) — not buildable on Linux.** Requires macOS + Xcode;
  skip on the Cloud VM.
- **Soundscape Admin (`tools/soundscape-admin`) — Vite dev-only web tool.** Needs
  a Supabase backend (URL + service-role key in `.env.local`; never `npm run
  build`/deploy — see its README). For a self-contained local backend on the VM,
  Docker and the Supabase CLI are preinstalled. Bring it up with:

  ```bash
  sudo dockerd            # if not already running (start once per VM)
  mkdir -p /tmp/sb && cd /tmp/sb && supabase init --force
  cp /workspace/supabase/migrations/*.sql /tmp/sb/supabase/migrations/
  supabase start          # applies migrations; prints API URL + SERVICE_ROLE_KEY
  ```

  Then set `tools/soundscape-admin/.env.local` to `VITE_SUPABASE_URL=http://127.0.0.1:54321`
  and `VITE_SUPABASE_SERVICE_ROLE_KEY=<SERVICE_ROLE_KEY from `supabase status`>`,
  and run `npm run dev` (serves `http://localhost:5173`).
  - **Gotcha:** the repo migrations create the `soundscape_*` tables but do not
    `GRANT` privileges to `service_role`. Hosted Supabase grants these via default
    privileges automatically; a fresh **local** DB does not, so PostgREST returns
    `permission denied`. After `supabase start`, grant once against the local DB:
    `grant all on all tables in schema public to anon, authenticated, service_role;`
    (run inside the `supabase_db_sb` container via `psql -U postgres`).
  - Migration `0005` seeds 9 bundled tracks, so the "Local tracks" table is
    populated immediately for testing toggles.
