# Android Release Readiness

Production-readiness assessment and pre-launch checklist for the Purrodoro
Android app (`com.endiar.purrodoro`).

> Last assessed: **2026-06-19** against `main` (ahead of `origin/main` by 7
> commits at assessment time). Monetization status refreshed **2026-08-31**
> (Android premium live).
>
> Scope: Android only. Verified by building `assembleRelease` and running
> `testDebugUnitTest` locally.
>
> A full-codebase **security review** was run on **2026-06-19** (Android +
> Supabase + tools). Findings are folded into the sections below — see blockers
> §7 (premium bypass), should-fix §8 (public premium audio), and §4 (auto-backup,
> now also flagged for the privacy angle).

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

---

## Blockers — must be resolved before Play Store release

### 1. ~~No release signing config~~  ·  resolved

Release/upload keystore + `signingConfig` in Gradle (`keystore.properties`).
Play App Signing enrolled; first signed AAB uploaded to Play Console (**2026-06-25**).

### 2. ~~Release Google OAuth client not registered~~  ·  resolved 2026-06-25

Upload-key and **Play App Signing** SHA-1s registered on the Android OAuth client.
Release Google sign-in verified via `installRelease`.

### 3. Multi-device sync QA not executed  ·  high

NAN-57 (Claim/Keep/Drop auth matrix) **passed 2026-06-19** on emulator — see
`pending-actions.md`. The 9-scenario two-device sync matrix (NAN-65) is still
open. Sync correctness (LWW conflicts, tombstones, process-death mid-sync) should
not ship unverified.

**Action:** execute NAN-65 on two devices (or device + emulator) before launch.

---

## Should-fix before release

### 4. ~~Auto-backup can corrupt the encrypted session (and backs up sensitive data)~~  ·  resolved 2026-06-25

~~`AndroidManifest.xml` has `allowBackup="true"` and
`res/xml/data_extraction_rules.xml` enables `<cloud-backup/>` with no exclusions~~
**Updated 2026-10-19:** sign-in is gone. `purrodoro.db` (+ WAL/SHM) is included in cloud backup and device transfer so tasks, habits, and session history restore with the user's Google account. `files/backups/`, `files/soundscapes/`, and `files/soundscape-thumbs/` stay excluded. `PurrodoroBackupAgent` checkpoints the WAL before the copy.

### 5. No crash reporting / analytics  ·  medium (operability)

~~No Crashlytics / Sentry / Bugsnag is wired.~~ **Resolved 2026-06-22:** Firebase
Crashlytics is integrated (`PurrodoroApplication`, `google-services.json` committed).
Confirm release-symbol upload after R8 is enabled.

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

### 7. ~~Premium entitlement — Play Billing integrated (client-side)~~  ·  resolved — **live 2026-08-31**

> Updated **2026-06-23.** Play Billing Library 8.x is wired via
> [`BillingManager`](../app/src/main/java/com/endiar/purrodoro/billing/BillingManager.kt):
> subscribe/restore, acknowledgement, foreground + sign-in refresh, user-visible errors,
> suspended-subscription query. The paywall no longer calls `setIsPremium(true)` directly.

`purrodoro_premium` (monthly/yearly base plans + yearly-trial offer) is created in
Play Console and **live in production** as of **2026-08-31** — monetization go-live
is done.

**Optional follow-up:** Play Developer API / RTDN server-side entitlement check
(client-only verification is accepted for v1 — rooted/modded clients can still
cheat, but server entitlement was never a launch blocker).

### 8. Premium cloud audio is publicly downloadable (paywall is UI-only)  ·  medium (security/monetization)

> Source: security review, 2026-06-19 (updated 2026-06-20 for R2).

Cloud soundscape MP3s live in a **public-read Cloudflare R2** bucket
(`purrodoro-soundscapes`). Catalog rows in `soundscape_tracks` (including
`is_premium=true`) expose `file_path`; the client builds plain public URLs:

```109:113:purrodoro-android/app/src/main/java/com/endiar/purrodoro/audio/TrackDownloadManager.kt
    internal fun downloadUrl(filePath: String): String {
        val base = audioPublicBaseUrl.trimEnd('/')
        val path = filePath.trimStart('/')
        return "$base/$path"
    }
```

`audioPublicBaseUrl` is `SoundscapeConfig.R2_PUBLIC_URL`. So premium remote tracks
can be enumerated and downloaded anonymously without the app or a purchase — the
paywall is enforced only in the UI. (Bundled APK audio is always extractable;
this finding is specific to **cloud** premium content.)

**Action:** make the bucket private and serve short-lived signed URLs tied to an
authenticated user with a verified premium entitlement (Cloudflare Worker or
similar). Optionally omit `file_path` from anonymous catalog responses.

---

## Minor / housekeeping

- Supabase `URL` + `PUBLISHABLE_KEY` are hardcoded in `SupabaseConfig.kt` —
  acceptable (anon role + RLS), but there is no staging/prod separation.

---

## Pre-launch checklist

- [x] Create release/upload keystore; add `signingConfig` (credentials out of VCS) — **2026-06-25**
- [x] Enroll in Play App Signing — **2026-06-25**
- [x] Register Play App Signing SHA-1 with Google OAuth Android client — **2026-06-25**
- [x] Verify production Google sign-in end-to-end — **2026-06-25**, release APK
- [x] Run NAN-57 (Claim/Keep/Drop) on device — **2026-06-19**, emulator
- [ ] Run NAN-65 (two-device sync matrix)
- [x] Cold-start home hero: force-stop app, reopen with each saved `HomeScene` (Room, Café, Library, Park); no flash back to Room — **2026-06-25**
- [x] Fix auto-backup: exclude encrypted session + `files/backups/` (+ Room DB), add reset-on-failure decrypt — **2026-06-25**
- [x] Integrate crash reporting — **Firebase Crashlytics**, 2026-06-22
- [x] Switch to AAB; enable R8 + `shrinkResources`; add ProGuard keep rules — **2026-06-23**
- [x] Confirm `app-release.aab` is signed and uploaded to Play Console — **2026-06-25**
- [x] Update stale auth-stub docs (`PRODUCT.md`, `pending-actions.md`) — **2026-06-25**
- [x] Gate premium on Play Billing **subscriptions** (monthly + yearly) + restore — §7 **code shipped 2026-06-23**; **live in production 2026-08-31**
- [ ] Move premium cloud audio to a private bucket with signed URLs + entitlement checks — §8

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
