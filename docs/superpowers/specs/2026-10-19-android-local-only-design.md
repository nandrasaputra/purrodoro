# Android local-only app — Design

_Date: 2026-10-19_

## Summary

Make the Android app local-only.

- Remove Google sign-in, account sync, and the Supabase client.
- Soundscapes play the tracks shipped in the APK. Remove the cloud catalog and Cloudflare R2 downloads.
- Let Android Auto Backup restore tasks, habits, and session history. Keep the existing JSON Backup & restore screen.
- Leave Firebase Crashlytics in place.
- Leave Play Billing subscriptions in place. The one-time purchase change is a later edit.
- iOS is out of scope.

## Why

Nobody is subscribed, and sign-in exists to sync data to Supabase. History should survive a reinstall without that account. The free platform path on Android is Auto Backup under the user's Google account, plus the JSON file the user already can export.

---

## Out of scope

- iOS (StoreKit, sign-in, iCloud, cloud catalog).
- Removing or replacing Firebase Crashlytics.
- Changing the subscription product, paywall prices, trial, or plan picker.
- A Room migration that drops sync columns or soundscape cache tables.
- Deleting the Supabase project, R2 bucket, or `docs/CLOUD_ASSET_MANAGEMENT.md` as an iOS/ops reference. Android stops calling them.

---

## 1. Sign-in and Supabase

Remove the Supabase Gradle dependencies: `auth-kt`, `compose-auth`, `postgrest-kt`, and the BOM.

Delete the auth and sync stacks and their tests:

- `auth/` (`AuthRepository`, `AccountManager`, `EncryptedSessionManager`, `SupabaseModule`, `SupabaseConfig`, `AccountAvatarStore`, `AuthDiagnostics`). `BillingManager` keeps its own Crashlytics log calls.
- `sync/` (`SyncEngine`, `SyncModule`, `SyncApi`, `SyncWorker`, `TableSyncs`, `StartupOwnership`, logout flush, sync status)
- `ui/auth/` (login sheet, Google sign-in block, account section, logout confirm, account-switch gate, premium login sheet)
- Debug sync UI (`SyncDebugActivity`, `SyncDebugController`, `SyncDebugNotification`)

`PurrodoroApp` stops collecting auth state, sync status, login, logout, and account-switch flows.

### Data that stays

Tasks, habits, habit logs, session records, daily stamps, and the local profile name stay in Room and DataStore. Existing installs open the same database.

Sync bookkeeping columns (`dirty`, owner ids, tombstones, and similar) stay in the schema. No migration. No code reads or writes them after this change.

`soundscape_track` and `soundscape_bundled` tables stay in the schema and are no longer read.

### JSON backup

`BackupCodec` / `BackupManager` stay. Restore is available whenever the user opens Backup & restore. The guest-only gate (`isGuest`, `backup_restore_blocked_signed_in`) goes away.

---

## 2. Soundscapes, local only

`SoundscapeCatalogBuilder` returns `BundledTrackRegistry.defaults` only.

| Kind | Tracks | Premium locks (unchanged) |
|---|---|---|
| Ambience | Rain, Cafe, Fire, Forest, Sea Wave, Winter Wind | Forest, Winter Wind |
| Lo-fi | Desk Lamp Rain, Mosswood Morning, Paper Skies, Quiet Window Code, Sore di Pendopo, Sudirman Rain Loop | Sore di Pendopo, Sudirman Rain Loop |

Delete:

- `SupabaseSoundscapeCatalogApi`, `SupabaseBundledCatalogApi`, and their repositories' network refresh
- `TrackDownloadManager` and `filesDir/soundscapes/` downloads
- `SoundscapeConfig.R2_PUBLIC_URL` and the DNS-over-HTTPS client in `SoundscapeHttp`
- Thumbnail fetches under `filesDir/soundscape-thumbs/`

On every cold start, delete `files/soundscapes/` and `files/soundscape-thumbs/` when those directories exist. Nothing recreates them.

### Saved selection

Resolve the saved ambience id and lo-fi id against the bundled list. If the id is missing or was a cloud track, fall back to Rain (`localId` 1, ambience) and Desk Lamp Rain (`localId` 1, music). Do not fall back onto a premium-locked track.

---

## 3. History backup

`android:allowBackup="true"` stays. Include the Room files by removing their `<exclude>` entries in both:

- `res/xml/backup_rules.xml` (API 28–30)
- `res/xml/data_extraction_rules.xml` (API 31+), under `<cloud-backup>` and `<device-transfer>`

Files included:

- `database/purrodoro.db`
- `database/purrodoro.db-wal`
- `database/purrodoro.db-shm`

Files still excluded:

- `file/backups/` (plaintext JSON safety copies)
- `file/soundscapes/`
- `file/soundscape-thumbs/`

Remove the `purrodoro_auth_session.xml` exclude. That file is no longer written.

Add `PurrodoroBackupAgent` and set `android:backupAgent` on the application. Before `super.onFullBackup`, open the Room database and run `PRAGMA wal_checkpoint(FULL)` so the main database file contains the latest sessions.

DataStore (`kuro_app_prefs`) is already included. A restore brings back tasks, habits, habit logs, session history, stamps, the local name, timer settings, and the saved sound choice.

Auto Backup quota is about 25 MB. Excluding audio keeps history inside that quota. There is no in-app backup-status indicator. Android does not report last-backup time to the app.

---

## 4. UI

Copy changes ship in `values/strings.xml` and `values-in/strings.xml`.

### Onboarding

Welcome has one primary button, **Continue** (`onboarding_cta_continue`). It runs today's guest-finish path. Remove Sign in with Google, Continue as guest, the signed-in welcome card, the login sheet, and the auth busy overlay. The next step is still notifications.

### Home

The top-left chip always uses the mascot (`KuroGuestAvatarContent`). When `isPremium` is true, `KuroPremiumAvatarFrame` still wraps it. Google photos and `AccountAvatarStore` go away.

### Profile

Remove the Account section: sign-in row, Google name and email, sync panel, sign out.

The Profile section is always visible. The name row stays editable.

Add a Premium row on that profile card. Tap opens `PremiumScreen` directly. Delete `PremiumLoginRequiredSheet`. `requestPremiumAccess(Discovery)` no longer checks for a signed-in user. Upsell sheets (locked sound, task cap, habit cap, hero scene) already open without sign-in. They stay.

When `isPremium` is true, the profile card uses the existing aurora outline (`premiumAuroraOutline`). The Google identity banner does not come back.

### Sounds

The sheet lists the 12 bundled tracks. Remove download, downloading, progress, and remove-download controls from ambience tiles and lo-fi tiles. A locked track still shows the lock and opens `PremiumUpsellSheet`.

### Storage and backup

Remove the Manage storage row and `StorageScreen`. That screen only listed downloaded cloud audio.

Profile keeps one data row, **Backup & restore**, under its own section header (not "Storage").

On Backup & restore:

- Export stays.
- Restore is always enabled.
- Remove the signed-in blocked hint.
- Add one sentence to the summary: a new phone signed into the same Google account can restore tasks, habits, and history. Do not show a "Backed up" or "Last backed up" status.

### Unchanged screens

Timer, tasks, habits, journey, history list, focus shield, appearance, language, and the Premium subscription layout (plan picker, trial, manage subscription) stay.

---

## 5. Docs and privacy

Update the Android-facing claims in:

- `docs/PRIVACY_POLICY.md` — no Google sign-in, no Supabase sync, no R2 audio. Crash logs still go to Firebase Crashlytics. Play Billing still processes the subscription. Android backup stores tasks, habits, and focus history with the user's Google account.
- `docs/DELETE_ACCOUNT.md` — the in-app account no longer exists. State that uninstall plus the user's Google account backup controls the remaining copy.
- `docs/PRODUCT.md` — Android data is on-device. History restore is Auto Backup plus JSON. Soundscapes are the bundled set. Premium entitlement stays the Play subscription cache.
- `purrodoro-android/docs/release-readiness.md` — replace the "Room DB excluded from auto-backup" note with the new include rules.

Leave `docs/CLOUD_ASSET_MANAGEMENT.md` in the repo. Add a short note that the Android client no longer reads the catalog or the R2 bucket.

---

## 6. Testing

- Backup rules: a test reads `backup_rules.xml` and `data_extraction_rules.xml` and asserts the three database paths are not excluded, and that `backups/`, `soundscapes/`, and `soundscape-thumbs/` are excluded from cloud backup and device transfer.
- Catalog: `SoundscapeCatalogBuilder` returns only the 12 bundled tracks, with the four premium locks above, and zero remote rows.
- Selection fallback: an unknown ambience id resolves to Rain. An unknown music id resolves to Desk Lamp Rain.
- Profile: guest and signed-in auth parameters are gone. Premium discovery opens `PremiumScreen` without a login sheet.
- Backup restore: the restore action is enabled with no auth flag.
- Delete or rewrite unit tests that construct `AuthRepository`, `SyncEngine`, `AccountManager`, or the Supabase catalog APIs.
- `./gradlew testDebugUnitTest` from `purrodoro-android/`.

Manual, on a device with backup enabled: create a session, run `adb shell bmgr backupnow com.endiar.purrodoro`, uninstall, reinstall, confirm history is present and the Sounds sheet shows only bundled tracks.

---

## Implementation order

1. Local soundscape list, delete R2 download and thumbnail code, delete leftover audio files, strip download UI.
2. Remove auth, sync, and the Supabase dependencies. Rework onboarding, home avatar, and profile.
3. Auto Backup includes, backup agent checkpoint, backup-screen copy, privacy docs.

Each step should leave `testDebugUnitTest` green before the next starts.
