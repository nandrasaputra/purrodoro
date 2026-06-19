# Android Release Readiness

Production-readiness assessment and pre-launch checklist for the Purrodoro
Android app (`com.endiar.purrodoro`).

> Last assessed: **2026-06-19** against `main` (ahead of `origin/main` by 7
> commits at assessment time).
>
> Scope: Android only. Verified by building `assembleRelease` and running
> `testDebugUnitTest` locally.

---

## Verdict

**Functionally solid, not yet publishable.** The app compiles cleanly, the
release variant assembles, and the unit suite is green. The remaining work is
release engineering and operational readiness — not code quality.

| Signal | Result |
|---|---|
| `./gradlew assembleRelease` | ✅ builds (`app-release-**unsigned**.apk`, ~84 MB) |
| `./gradlew testDebugUnitTest` | ✅ **311 tests, 0 failures** |
| Compiler warnings | Benign only (Kotlin `KT-73255` annotation-target deprecation) |

---

## What's already in good shape

- **Builds + tests green** — release variant assembles; 311 unit tests pass.
- **Auth is real, not stubbed** — `AuthRepository` uses live Supabase
  ComposeAuth (Google via Credential Manager) and is fully wired through
  `LoginSheet → resolveSignedInUser → AccountManager.onSignedIn`.
  `SupabaseConfig.GOOGLE_WEB_CLIENT_ID` is populated.
- **Clean logging hygiene** — a single `Log.w` in the main source set; no stray
  `println` / `printStackTrace`.
- **Debug tooling isolated** — Dev Tools, sync debug, and `PremiumOverride` all
  live in `src/debug/`, so they cannot leak into release builds.
- **Session secret handled correctly** — Supabase refresh token persisted via
  `EncryptedSharedPreferences` with an Android Keystore master key
  (`EncryptedSessionManager`).
- **Current SDK targets** — `minSdk 28`, `targetSdk 36`.

> ⚠️ **Doc drift:** `docs/PRODUCT.md` and `purrodoro-android/docs/pending-actions.md`
> still describe a random-UUID auth *stub*. That is **stale** — auth is now a
> real Supabase implementation. Update those docs.

---

## Blockers — must be resolved before Play Store release

### 1. No release signing config  ·  high

`assembleRelease` emits `app-release-unsigned.apk`. There is no `signingConfig`,
`storeFile`, or keystore anywhere in the Gradle files. An unsigned artifact
cannot be uploaded.

**Action:** create a release/upload keystore, add a `signingConfig` (read
credentials from `local.properties` or environment variables — never commit
them), and enroll in **Play App Signing**.

### 2. Release Google OAuth client not registered  ·  high

The Google **Android** OAuth client is currently tied to the *debug* SHA-1
(see `pending-actions.md` · NAN-56). Production sign-in will fail until the
**release/upload key SHA-1** (and the Play App Signing SHA-1) are registered in
Google Cloud Console for package `com.endiar.purrodoro`.

**Action:** after creating the release key, run `./gradlew signingReport`, add
both release SHA-1s to the Google Cloud Android OAuth client, and confirm the
Supabase Google provider uses the Web client ID + secret.

### 3. On-device + multi-device QA not executed  ·  high

The auth Claim/Keep/Drop matrix (NAN-57) and the 9-scenario two-device sync
matrix (NAN-65) are documented in `pending-actions.md` but not yet run. Sync
correctness (LWW conflicts, tombstones, process-death mid-sync) should not ship
unverified.

**Action:** execute NAN-57 and NAN-65 on real devices before launch.

---

## Should-fix before release

### 4. Auto-backup can corrupt the encrypted session  ·  medium (correctness)

`AndroidManifest.xml` has `allowBackup="true"` and
`res/xml/data_extraction_rules.xml` enables `<cloud-backup/>`. This backs up the
`EncryptedSharedPreferences` file, but its decryption key lives in the
device-bound Android Keystore and is **not** restored on a new device. After a
device-transfer restore, reading that prefs file can throw and crash on launch.

**Action (pick one):**
- Exclude the encrypted-session prefs from backup (and likely the Room DB, since
  it is account-owned and re-synced from the server), **or**
- Wrap the session decrypt in a reset-on-failure fallback (clear + treat as
  signed-out), **or**
- Set `allowBackup="false"` if device-transfer of local data is not a goal.

### 5. No crash reporting / analytics  ·  medium (operability)

No Crashlytics / Sentry / Bugsnag is wired. Production crashes would be
invisible.

**Action:** integrate a crash reporter before launch.

### 6. R8 disabled; shipping APK instead of AAB  ·  medium

`isMinifyEnabled = false`, no `shrinkResources`, and the build produces an APK
(~84 MB unsigned; `res/raw` audio is only ~10 MB, so most size is native libs +
artwork).

**Action:** publish an **AAB** (`bundleRelease`) and enable R8 +
`shrinkResources`. If enabling R8, add ProGuard keep rules — `proguard-rules.pro`
is currently empty and the app uses libraries that need them:
- Supabase (`auth-kt`, `compose-auth`, `postgrest-kt`)
- Ktor (OkHttp engine)
- `kotlinx.serialization` (`@Serializable` models / DTOs)
- Room

---

## Minor / housekeeping

- `versionCode = 1` / `versionName = "1.0"` — fine for a first release; establish
  a bump process for subsequent uploads.
- Supabase `URL` + `PUBLISHABLE_KEY` are hardcoded in `SupabaseConfig.kt` —
  acceptable (anon role + RLS), but there is no staging/prod separation.
- Refresh the stale auth-stub references in `PRODUCT.md` and `pending-actions.md`.

---

## Pre-launch checklist

- [ ] Create release/upload keystore; add `signingConfig` (credentials out of VCS)
- [ ] Enroll in Play App Signing
- [ ] Register release + Play App Signing SHA-1s with Google OAuth Android client
- [ ] Verify production Google sign-in end-to-end
- [ ] Run NAN-57 (Claim/Keep/Drop) on device
- [ ] Run NAN-65 (two-device sync matrix)
- [ ] Fix auto-backup vs. encrypted-session restore (exclude or reset-on-failure)
- [ ] Integrate crash reporting
- [ ] Switch to AAB; enable R8 + `shrinkResources`; add ProGuard keep rules
- [ ] Confirm `app-release.aab` is signed and installs from a clean device
- [ ] Update stale auth-stub docs (`PRODUCT.md`, `pending-actions.md`)

---

## How to reproduce this assessment

```bash
cd purrodoro-android
./gradlew testDebugUnitTest assembleRelease --console=plain
# release artifact:
ls app/build/outputs/apk/release/        # -> app-release-unsigned.apk today
# test results:
ls app/build/test-results/testDebugUnitTest/
```

## Related docs

- Auth blockers / QA matrices: [`pending-actions.md`](pending-actions.md)
- Product reference: [`../../docs/PRODUCT.md`](../../docs/PRODUCT.md)
- Auth & account identity spec: [`../../docs/superpowers/specs/2026-06-09-auth-and-account-identity.md`](../../docs/superpowers/specs/2026-06-09-auth-and-account-identity.md)
