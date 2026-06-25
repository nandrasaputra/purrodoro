# Delete your Purrodoro account

**App:** Purrodoro (Android)  
**Developer:** Endiar  
**Last updated:** 26 June 2026

This page explains how to request deletion of your **Purrodoro account** and the data we store for you in the cloud.

---

## How to request account deletion

1. **Email us** from the Google address linked to your Purrodoro account:
   - **To:** endiar.dev@gmail.com
   - **Subject:** Purrodoro account deletion request
2. In the message, include:
   - The **email address** you use to sign in with Google
   - A clear statement that you want your **Purrodoro account and cloud data deleted**
3. We will verify the request and delete your account within **30 days** (usually sooner). We may reply once to confirm your identity if needed.

**Optional before you email:** Sign in to Purrodoro → **Profile** → sign out on devices you no longer use. This removes the encrypted session on those devices but does **not** delete cloud data by itself.

---

## What we delete

When your account is deleted, we remove your **Supabase auth account** and all synced app data tied to it, including:

| Data | Deleted |
|------|---------|
| Account identity (Supabase user ID) | Yes |
| Display name & timer settings (`profiles`) | Yes |
| Tasks & categories | Yes |
| Habits & habit logs | Yes |
| Focus / Pomodoro session history (`session_records`) | Yes |
| Daily stamps / journey stats | Yes |
| Google sign-in link to Purrodoro (Supabase Auth) | Yes |

Deletion is permanent. You can use Purrodoro again as a **guest** or sign in with Google to create a **new** account (empty cloud data).

---

## What we do not delete (or cannot delete)

| Data | What happens |
|------|----------------|
| **Data on your device** | Not removed by cloud deletion. Uninstall the app or clear **Purrodoro** storage in Android Settings to remove local data. |
| **Manual backup files (JSON)** | Files you exported stay wherever you saved them (Downloads, Drive, etc.). Delete those copies yourself. |
| **Crash & diagnostic logs** | May remain in **Firebase Crashlytics** for up to **~90 days** (Google’s default retention). Not linked to your account after deletion. |
| **Google Play subscription & purchase records** | Kept by **Google**. Deleting your Purrodoro account does **not** cancel Premium. Cancel in [Google Play → Subscriptions](https://play.google.com/store/account/subscriptions). |
| **Downloaded soundscape audio** | Cached files on your device; clear app storage or uninstall to remove. |

We do **not** sell your data. We do not keep a copy of your synced tasks, habits, or focus history after account deletion is completed.

---

## Guest users (no account)

If you never signed in with Google, there is **no cloud account** to delete. Remove local data by uninstalling Purrodoro or clearing app storage in Android Settings.

---

## Questions

**Email:** endiar.dev@gmail.com  
**Privacy policy:** https://nandrasaputra.dev/purrodoro/privacy-policy
