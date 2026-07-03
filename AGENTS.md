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
