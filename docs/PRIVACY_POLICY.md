# Privacy Policy — Purrodoro

**Last updated:** 8 November 2026
**App:** Purrodoro (Android)  
**Developer:** Endiar  
**Contact:** nandra.saputrass@gmail.com  
**Privacy policy URL:** https://nandrasaputra.dev/purrodoro/privacy-policy

---

## 1. Overview

Purrodoro is a Pomodoro focus timer. Tasks, habits, settings, and focus history stay on your device. There is no account sign-in. Android can include that data in your Google account's app backup, and you can also export a JSON file.

This policy explains what information we collect, how we use it, and your choices. If you do not agree, please do not use the app.

---

## 2. Information we collect

### 2.1 Information you provide

| Data | Where | Purpose |
|------|-------|---------|
| Display name | On device | Greeting and profile |
| Tasks, categories, habits | On device | Core app features |
| Pomodoro / focus session history | On device | Journey, calendar, stats |
| Timer and app settings | On device | Preferences |
| Focus Shield settings and selected apps | On device | Interrupt selected apps during focus sessions |
| Manual backup file (JSON) | A file you choose | Backup and restore |

The current Android app does not ask you to sign in or create an account. Accounts and cloud data from earlier versions may still exist until deleted.

### 2.2 Information collected automatically

| Data | When | Purpose |
|------|------|---------|
| **Purchase status** | When you purchase or restore Premium via Google Play | Premium features |
| **Crash & diagnostic logs** | When the app crashes (release builds) | Fix bugs via Firebase Crashlytics |
| **Device & app metadata** (OS version, device model, app version) | With crash reports | Debugging |
| **App-usage information** | If you enable Focus Shield and grant usage access | Detect the foreground app and show an interruption for selected apps |

Focus Shield checks recent app-usage events on the device while it is active. The current Android app uses this information locally to decide whether to show an overlay. Selected app settings are stored on the device and may be included in Android backup.

We do **not** collect precise location, contacts, photos, microphone, or camera data.

### 2.3 Information we do not collect

- We do **not** sell your personal information.
- We do **not** use third-party advertising or ad tracking SDKs.
- We do **not** read your SMS, call log, or files outside the backup flow you start.

---

## 3. How we use information

We use data to:

- Run the Pomodoro timer, tasks, habits, and soundscapes
- Verify Premium purchase status
- Send **local notifications** for timer completion (if you allow notifications)
- Diagnose crashes and improve stability
- Respond to support requests

---

## 4. Where data is stored

| Storage | What | Who can access |
|---------|------|----------------|
| **Your device** (Room database, preferences) | Tasks, habits, history, settings, display name | You. Included in Android Auto Backup and phone-to-phone transfer, except JSON safety copies |
| **Google** | Play Billing records; Android app backup | Google (per [Google Privacy Policy](https://policies.google.com/privacy)) |
| **Firebase Crashlytics** (Google) | Crash stack traces & diagnostic logs | Google / Firebase (per [Firebase Privacy](https://firebase.google.com/support/privacy)) |

The current Android app does not upload your tasks, habits, or focus history to a server we operate. Soundscape audio ships inside the app. Earlier versions supported accounts and cloud sync; see the deletion page below if you used those versions.

---

## 5. Third-party services

Purrodoro uses:

| Service | Provider | Role |
|---------|----------|------|
| **Google Play Billing** | Google LLC | Premium purchases |
| **Firebase Crashlytics** | Google LLC | Crash reporting (release builds only) |
| **Android backup** | Google LLC | Restores app data on a new phone with the same Google account |

Each provider has its own privacy policy. We share crash logs with Firebase, and purchase status with Google Play. Android backup is stored by Google with your account.

---

## 6. Permissions

The app may request:

| Permission | Why |
|------------|-----|
| **Internet** | Play Billing and crash reporting |
| **Notifications** | Timer completion alerts |
| **Foreground service** | Keep the focus timer running reliably |
| **Schedule exact alarms** | Fire segment-end alerts if the app is in the background *(optional; user can deny “Alarms & reminders”)* |
| **Wake lock** | Timer reliability while the screen is off |
| **Usage access** | Optional Focus Shield feature checks which app is in the foreground |
| **Display over other apps** | Optional Focus Shield interruption overlay |

You can deny notifications or alarm permission; core timing still works while the app is active.

---

## 7. Data retention & deletion

- **On-device data:** Stays on your device until you uninstall the app or clear app storage. Android backup may keep a copy with your Google account until you delete that backup.
- **Crash logs:** Retained per Firebase Crashlytics default retention (typically ~90 days).
- **Manual backups:** Files you export stay wherever you save them (Downloads, Drive, etc.).
- **Accounts from earlier versions:** Previously synced cloud data may remain until you request deletion using the page below.

**To delete your account and cloud data:** See our dedicated page:  
https://nandrasaputra.dev/purrodoro/delete-account

**To delete local data only:** Uninstall the app, or clear app storage in Android Settings.

---

## 8. Security

- Play Billing and crash reports use **HTTPS**.
- JSON safety copies and leftover downloaded audio are excluded from Android backup. The task and history database is included so a reinstall can restore it.

No method of transmission or storage is 100% secure; we work to protect data using industry-standard practices.

---

## 9. Children

Purrodoro is not directed at children under 13 (or the minimum age in your country). We do not knowingly collect personal information from children. Contact us if you believe a child has provided data and we will delete it.

---

## 10. International transfers

Google may process Play Billing records, crash logs, and Android backup in the United States and other countries. By using Purrodoro, you understand that information may be transferred to jurisdictions with different data-protection laws.

---

## 11. Your rights

Depending on where you live, you may have the right to:

- Access, correct, or delete your personal data
- Export your data from Profile → Backup & restore
- Withdraw consent where applicable (e.g. uninstall or deny notifications)
- Lodge a complaint with a data protection authority

**EU/UK (GDPR):** Legal bases include contract (providing the app), legitimate interests (security, crash fixes), and consent where applicable (such as notifications).
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
| Email address | No | — | — | — |
| Name | On device only | Not shared by us | App functionality | Yes |
| User IDs | Crash installs only | Firebase | Analytics | No (release builds) |
| App activity (tasks, habits, focus history) | On device; may be in Android backup | Google (backup) | App functionality | No |
| Crash logs | Yes | Firebase | Analytics | No (release builds) |
| App-usage information | Processed on device for optional Focus Shield | Not uploaded by the current Android app | App functionality | Yes |
| Purchase history | Yes | Google Play | App functionality | Yes (only if purchasing) |
| Photos | No | — | — | — |

**Encryption in transit:** Yes  
**Users can request deletion:** Yes (contact email)  
**Committed to Play Families / Designed for children:** No
