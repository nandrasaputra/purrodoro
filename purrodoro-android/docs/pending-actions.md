# Pending Actions

Items remaining before the offline-first sync feature is live end-to-end.
Branch: `feature/offline-first-sync` (22 commits, 253 unit tests green, Supabase schema applied).

---

## Done

### ✅ NAN-56 · Configure Google OAuth clients + Supabase Google provider

**Verified 2026-06-19** — debug build signs in with Google end-to-end.

- Web client ID in `SupabaseConfig.kt`; Supabase Google provider enabled.
- Android OAuth client registered for debug SHA-1 + `com.endiar.purrodoro`.
- **Still open for Play Store:** register release + Play App Signing SHA-1s (see
  `release-readiness.md` §2).

### ✅ NAN-57 · On-device auth verification

**Verified 2026-06-19** on emulator (`sdk_gphone64_arm64`) with two Google accounts
(`leftlucia@gmail.com`, `nandra.saputrass@gmail.com`).

| Scenario | Result |
|---|---|
| **Claim** | ✅ Guest task kept; `ownerId` tagged to account A |
| **Keep** | ✅ Guest edits kept; re-login as A — no switch dialog |
| **Drop** | ✅ Confirm switch → local data cleared; `ownerId` → B |
| **Drop cancelled** | ✅ Cancel → data and `ownerId` unchanged |
| **Session persistence** | ✅ `force-stop` + relaunch → still signed in |

---

## Blockers — remaining

*(none for debug sign-in; release OAuth SHA-1s tracked in `release-readiness.md`)*

---

## Verification — remaining

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
