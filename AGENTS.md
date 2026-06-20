# AGENTS

Quick orientation for agents working in this repo. Keep entries brief — link to
the source docs for detail.

- **Product reference:** [`docs/PRODUCT.md`](docs/PRODUCT.md)
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
