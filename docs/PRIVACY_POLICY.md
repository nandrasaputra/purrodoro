# Privacy Policy — Purrodoro

**Effective date:** 25 June 2026  
**App:** Purrodoro (Android)  
**Developer:** Endiar  
**Contact:** nandra.saputrass@gmail.com  
**Privacy policy URL:** https://nandrasaputra.dev/purrodoro/privacy-policy

---

## 1. Overview

Purrodoro is a Pomodoro focus timer. You can use the app **as a guest** (data stays on your device) or **sign in with Google** to sync tasks, habits, and focus history across devices.

This policy explains what information we collect, how we use it, and your choices. If you do not agree, please do not use the app.

---

## 2. Information we collect

### 2.1 Information you provide

| Data | Guest | Signed in | Purpose |
|------|-------|-----------|---------|
| Display name | Optional (local) | Yes (from Google or your edits) | Greeting & profile |
| Tasks, categories, habits | Yes (local) | Yes (synced) | Core app features |
| Pomodoro / focus session history | Yes (local) | Yes (synced) | Journey, calendar, stats |
| Timer & app settings | Yes (local) | Yes (synced) | Preferences |
| Manual backup file (JSON) | Optional, user-initiated export | Optional | Backup & restore (guest only) |

We do **not** ask for a password. Sign-in uses **Google** via Supabase Auth.

### 2.2 Information collected automatically

| Data | When | Purpose |
|------|------|---------|
| **Google account info** (email, name, profile photo URL) | When you sign in with Google | Authentication & profile display |
| **Supabase user ID** | When signed in | Account identity & sync |
| **Subscription / purchase status** | When you subscribe or restore via Google Play | Premium features |
| **Crash & diagnostic logs** | When the app crashes (release builds) | Fix bugs via Firebase Crashlytics |
| **Device & app metadata** (OS version, device model, app version) | With crash reports | Debugging |
| **Soundscape catalog metadata** | When you open Sounds | List available ambience / lo-fi tracks |
| **Downloaded audio files** | When you download cloud tracks | Offline playback |

We do **not** collect precise location, contacts, photos (except your Google profile image URL when signed in), microphone, or camera data.

### 2.3 Information we do not collect

- We do **not** sell your personal information.
- We do **not** use third-party advertising or ad tracking SDKs.
- We do **not** read your SMS, call log, or files outside the backup flow you start.

---

## 3. How we use information

We use data to:

- Run the Pomodoro timer, tasks, habits, and soundscapes
- Sync your data across devices when you are signed in
- Show your account name and avatar in Profile
- Verify Premium subscription status
- Send **local notifications** for timer completion (if you allow notifications)
- Diagnose crashes and improve stability
- Respond to support requests

---

## 4. Where data is stored

| Storage | What | Who can access |
|---------|------|----------------|
| **Your device** (Room database, preferences, encrypted session) | Guest & signed-in local cache | You; excluded from Android cloud backup: auth session, main database, safety backup folder |
| **Supabase** (hosted cloud database & auth) | Signed-in user data & account metadata | You (via your account); protected by row-level security |
| **Google** | Sign-in credentials; Play Billing records | Google (per [Google Privacy Policy](https://policies.google.com/privacy)) |
| **Firebase Crashlytics** (Google) | Crash stack traces & diagnostic logs | Google / Firebase (per [Firebase Privacy](https://firebase.google.com/support/privacy)) |
| **Cloudflare R2** | Public soundscape audio files & catalog | Anyone with the URL (audio files); catalog via our backend |

Guest data is **not uploaded** to our servers unless you sign in.

---

## 5. Third-party services

Purrodoro uses:

| Service | Provider | Role |
|---------|----------|------|
| **Supabase Auth & Database** | Supabase, Inc. | Google sign-in, cloud sync |
| **Google Sign-In** | Google LLC | Authentication |
| **Google Play Billing** | Google LLC | Premium subscriptions |
| **Firebase Crashlytics** | Google LLC | Crash reporting (release builds only) |
| **Cloudflare R2** | Cloudflare, Inc. | Soundscape audio hosting |

Each provider has its own privacy policy. We only share what is needed for these services to work (for example, your Supabase user ID and synced app data, or crash logs).

---

## 6. Permissions

The app may request:

| Permission | Why |
|------------|-----|
| **Internet** | Sign-in, sync, soundscape catalog & downloads |
| **Notifications** | Timer completion alerts |
| **Foreground service** | Keep the focus timer running reliably |
| **Schedule exact alarms** | Fire segment-end alerts if the app is in the background *(optional; user can deny “Alarms & reminders”)* |
| **Wake lock** | Timer reliability while the screen is off |

You can deny notifications or alarm permission; core timing still works while the app is active.

---

## 7. Data retention & deletion

- **Guest data:** Stays on your device until you uninstall the app or use in-app delete/reset features.
- **Signed-in data:** Kept in Supabase while your account exists. Sign out clears the on-device session; cloud data remains until you delete it or request account deletion.
- **Crash logs:** Retained per Firebase Crashlytics default retention (typically ~90 days).
- **Manual backups:** Files you export stay wherever you save them (Downloads, Drive, etc.).

**To delete cloud data:** Sign in, remove your tasks/habits in the app, then contact us to request full account deletion from Supabase if needed.

**To delete local data:** Uninstall the app, or clear app storage in Android Settings.

---

## 8. Security

- Supabase sessions use **encrypted storage** on device (Android Keystore).
- Cloud access uses **HTTPS** and **row-level security** so each user only sees their own rows.
- We exclude sensitive local files (auth session, database, automatic safety backups) from Android cloud backup.

No method of transmission or storage is 100% secure; we work to protect data using industry-standard practices.

---

## 9. Children

Purrodoro is not directed at children under 13 (or the minimum age in your country). We do not knowingly collect personal information from children. Contact us if you believe a child has provided data and we will delete it.

---

## 10. International transfers

Our service providers (Supabase, Google, Cloudflare) may process data in the United States and other countries. By using Purrodoro, you understand your information may be transferred to jurisdictions with different data-protection laws.

---

## 11. Your rights

Depending on where you live, you may have the right to:

- Access, correct, or delete your personal data
- Export your data (in-app backup for guests; synced data when signed in)
- Withdraw consent (e.g. sign out, uninstall, deny notifications)
- Lodge a complaint with a data protection authority

**EU/UK (GDPR):** Legal bases include contract (providing the app), legitimate interests (security, crash fixes), and consent (notifications, optional sign-in).  
**California (CCPA/CPRA):** We do not sell personal information. You may request access or deletion via the contact below.

---

## 12. Changes

We may update this policy. We will post the new version at the same URL and change the effective date. Continued use after changes means you accept the updated policy.

---

## 13. Contact

Questions or requests about privacy:

**Email:** nandra.saputrass@gmail.com  
**Developer:** Endiar — Purrodoro  
**Privacy policy URL:** https://nandrasaputra.dev/purrodoro/privacy-policy

---

## Appendix — Google Play Data safety (summary)

Use this as a guide when filling Play Console **Data safety**. Adjust if your live build differs.

| Data type | Collected | Shared | Purpose | Optional |
|-----------|-----------|--------|---------|----------|
| Email address | Yes (sign-in) | Supabase, Google | Account | Yes (guest mode) |
| Name | Yes (sign-in) | Supabase, Google | Account | Yes |
| User IDs | Yes | Supabase, Firebase | Account, analytics (crashes) | Partial |
| App activity (tasks, habits, focus history) | Yes | Supabase (if signed in) | App functionality | Yes (guest: local only) |
| Crash logs | Yes | Firebase | Analytics | No (release builds) |
| Purchase history | Yes | Google Play | App functionality | Yes (only if subscribing) |
| Photos | Profile image URL only | Google | Account | Yes |

**Encryption in transit:** Yes  
**Users can request deletion:** Yes (contact email)  
**Committed to Play Families / Designed for children:** No
