# Pending Actions

Items remaining before the offline-first sync feature is live end-to-end.
Branch: `feature/offline-first-sync` (22 commits, 253 unit tests green, Supabase schema applied).

---

## Blockers — must be done before sign-in works

### NAN-56 · Configure Google OAuth clients + Supabase Google provider (~15 min, manual)

1. Run `./gradlew signingReport` in `purrodoro-android/` and note the **debug SHA-1**.
2. **Google Cloud Console** → Credentials → create two OAuth clients:
   - **Web** client ID (for the `signInWithIdToken` exchange with Supabase)
   - **Android** client ID — package `com.endiar.purrodoro` + debug SHA-1 (lets Credential Manager surface the account picker; not referenced in code)
3. **Supabase dashboard** → project `birjkyerhokqdzhdfgpt` → Authentication → Providers → Google:
   - Enable, paste the **Web client ID** and its secret
   - Keep **anonymous sign-in OFF**
4. Paste the Web client ID into `app/src/main/java/com/endiar/purrodoro/auth/SupabaseConfig.kt`:
   ```kotlin
   const val GOOGLE_WEB_CLIENT_ID = "<your-web-client-id>.apps.googleusercontent.com"
   ```
   `isGoogleConfigured` flips `true` automatically and the "not configured" error in `LoginSheet` goes away.

---

## Verification — once NAN-56 is done

### NAN-57 · On-device auth verification (two Google accounts needed)

| Scenario | Steps | Expected |
|---|---|---|
| **Claim** | Fresh install → create tasks/habits → sign in as account A | Data kept, `ownerId = A` |
| **Keep** | Sign out → edit as guest → sign back in as A | Edits kept, no confirm dialog |
| **Drop** | With A's data, sign in as B → confirm | A's data cleared (name, tasks, timer stopped), device prefs kept, `ownerId = B` |
| **Drop cancelled** | Same, but cancel the dialog | Data untouched, session reverted, `ownerId` still A |

Also verify: encrypted session survives process death (kill app → relaunch → still signed in).

### NAN-65 · Two-device QA matrix

Test on two physical devices (or device + emulator), same Google account unless noted:

| # | Scenario |
|---|---|
| 1 | **Fresh-device pull** — sign in on device B after using device A → all tasks/habits/records/settings appear |
| 2 | **Bidirectional edits** — create task on A, complete habit on B → both visible on both after sync |
| 3 | **LWW conflict** — edit same task title on both while one is offline → reconnect → later edit wins, no duplicates |
| 4 | **Offline cycle** — run a full Pomodoro offline on A → record + stamps sync to B's Journey/Calendar |
| 5 | **Tombstones** — delete task/category/habit on A → disappears on B, no resurrection |
| 6 | **Drop → restore** — account B signs in on A's device → confirm → B's cloud data appears; A's data intact in A's cloud (re-login as A to verify) |
| 7 | **Logout flush warning** — go offline, edit, logout → warning shown; online logout → no warning |
| 8 | **Upgrade path** — install current prod build, create data, install sync build → migration-free fresh Room, first sign-in uploads |
| 9 | **Process death mid-sync** — kill app during sync → relaunch → no data loss or duplicates |

---

## Merge

`feature/offline-first-sync` is unmerged, unpushed, sitting on top of `main` (2 commits ahead of origin/main at branch time). When QA passes, merge or open a PR.

---

## Minor leftover (cosmetic, non-blocking)

One KDoc paragraph in `sync/SyncEngine.kt` about checkpoint behavior is slightly stale after the universal pull-then-push ordering change — can be folded into any later commit.
