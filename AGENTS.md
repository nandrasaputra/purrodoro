# AGENTS

Quick orientation for agents working in this repo. Keep entries brief — link to
the source docs for detail.

- **Product reference:** [`docs/PRODUCT.md`](docs/PRODUCT.md)
- **iOS port status:** [`purrodoro-ios/STATUS.md`](purrodoro-ios/STATUS.md)
- **Android release readiness:** [`purrodoro-android/docs/release-readiness.md`](purrodoro-android/docs/release-readiness.md)
- **Design specs:** [`docs/superpowers/specs/`](docs/superpowers/specs/)
- **Soundscape assets:** Postgres catalog + **Cloudflare R2** audio (`l_amb_*` /
  `l_lofi_*` local APK, `amb_*` / `lofi_*` cloud); see
  [`docs/CLOUD_ASSET_MANAGEMENT.md`](docs/CLOUD_ASSET_MANAGEMENT.md)

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

## Testing (iOS)

Run from `purrodoro-ios/`:

```bash
xcodebuild -scheme Purrodoro -destination 'platform=iOS Simulator,name=iPhone 17' build
xcodebuild test -scheme Purrodoro -destination 'platform=iOS Simulator,name=iPhone 17' \
  -only-testing:PurrodoroTests
xcrun simctl install booted build/DerivedData/Build/Products/Debug-iphonesimulator/Purrodoro.app
```

Port progress + parity matrix: [`purrodoro-ios/STATUS.md`](purrodoro-ios/STATUS.md).

### Backup & Restore (iOS)

- **Automated:** `BackupCodecTests` in `PurrodoroTests/FeatureTests.swift` (round-trip, malformed, unsupported version).
- **Manual:** Profile → Backup & restore → Export / Restore. Guest-only restore; stop timer first. Cross-platform with Android schema v1 JSON.

### Auth (iOS)

- **Automated:** `ResolveLoginActionTests`, `AuthProviderTests`, `AccountManagerTests` in `PurrodoroTests/FeatureTests.swift`.
- **Manual:** Profile → Sign in → Apple (or Google once `SupabaseConfig.googleIOSClientID` is set). Claim/Keep/Drop flows — see STATUS.md.
- **Spec:** [`docs/superpowers/specs/2026-06-09-auth-and-account-identity.md`](docs/superpowers/specs/2026-06-09-auth-and-account-identity.md).

## Cursor Cloud specific instructions

The Cloud VM is **Linux**, so what is runnable differs per component. The
startup update script only refreshes dependencies — services are started
manually per the notes below.

### Android (`purrodoro-android`) — primary product, build & test only

- **SDK/JDK are already provisioned:** Android SDK lives at `$HOME/android-sdk`
  (Platform `android-36` + `android-36.1`, Build-Tools `36.0.0`, Platform-Tools).
  `ANDROID_HOME`/`ANDROID_SDK_ROOT`/`PATH` are exported in `~/.bashrc` (login
  shells only), and the update script writes `purrodoro-android/local.properties`
  (`sdk.dir=…`) so non-login shells resolve the SDK too. JDK 21 is preinstalled;
  the Gradle wrapper fetches Gradle 9.2.1 on first use.
- **Use the documented Gradle commands** in the *Testing (Android)* section above.
  `./gradlew testDebugUnitTest` (399 JUnit/Robolectric tests) and
  `./gradlew assembleDebug` work headless.
- **No emulator / GUI:** there is no `/dev/kvm`, so `connectedDebugAndroidTest`,
  `installDebug`, and running the app as a normal GUI are **not** possible here.
- **Lint runs but currently fails on pre-existing code errors** (41 errors; first
  is a `MissingPermission` in `timer/TimerAlarmReceiver.kt`). This is a code
  issue, not an environment issue — there is no lint baseline configured, so
  `./gradlew lintDebug` aborts. Treat lint as functional tooling; the failures
  are pre-existing.
- **To preview real UI headlessly (no emulator):** render with Robolectric
  `@GraphicsMode(NATIVE)` and draw the view to a `Bitmap`. See
  `widget/WidgetPreviewLayoutExportTest` — run it with `EXPORT_WIDGET_PREVIEWS=1`
  (it re-writes the committed `res/drawable/widget_preview_*.png`, so `git
  checkout` those afterward if you only wanted a preview).

### iOS (`purrodoro-ios`) — not buildable on Linux

Requires macOS + Xcode; skip on the Cloud VM.

### Soundscape Admin (`tools/soundscape-admin`) — Vite dev-only web tool

- Debug-only SPA; it **throws on load** unless `VITE_SUPABASE_URL` +
  `VITE_SUPABASE_SERVICE_ROLE_KEY` are set in `.env.local` (gitignored). Never
  `npm run build` / serve `dist/` — Vite would inline the secrets (see its
  README). Run `npm run dev` (serves `http://localhost:5173`).
- **Docker + the Supabase CLI are preinstalled** for a self-contained local
  backend, but `dockerd` is **not** auto-started. Bring the stack up with:

  ```bash
  sudo bash -c 'nohup dockerd >/var/log/dockerd.log 2>&1 &'   # start once per VM
  sudo chmod 666 /var/run/docker.sock                          # let non-root use docker
  cd ~/sb-local && supabase start                              # applies repo migrations, prints keys
  ```

  A ready-to-use local project already exists at `~/sb-local` (repo migrations
  copied into `~/sb-local/supabase/migrations`). Re-copy from
  `/workspace/supabase/migrations/*.sql` if migrations change.
- **Gotcha:** the repo migrations create the `soundscape_*` tables but do **not**
  `GRANT` privileges to `service_role`. Hosted Supabase grants these via default
  privileges; a fresh **local** DB does not, so PostgREST returns
  `permission denied`. After `supabase start`, grant once against the local DB:

  ```bash
  docker exec -i supabase_db_sb-local psql -U postgres -d postgres \
    -c "grant all on all tables in schema public to anon, authenticated, service_role;"
  ```

  Then point `tools/soundscape-admin/.env.local` at
  `VITE_SUPABASE_URL=http://127.0.0.1:54321` with the `SERVICE_ROLE_KEY` from
  `supabase status`. Migrations seed 22 remote + 12 bundled tracks, so both
  tables populate immediately. R2 (`VITE_R2_*`) creds are only needed for audio
  uploads — dummy values are fine for browsing/toggling.
