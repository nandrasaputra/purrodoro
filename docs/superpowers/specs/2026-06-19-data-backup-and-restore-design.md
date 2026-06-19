# Data Backup & Restore — Design

_Date: 2026-06-19_

## Summary

Add a user-facing **Backup & Restore** feature on Android: export all of a
user's on-device data to a single JSON file (via the system file picker), and
restore it later on the same or another device. The feature is **data-only**
and stays entirely out of the auth/identity layer — the canonical ownership
rule in [`AccountManager`](../../../purrodoro-android/app/src/main/java/com/endiar/purrodoro/auth/AccountManager.kt)
is untouched.

The name in the UI is **"Backup & Restore"** (not "Export/Import") to set the
right mental model: restore **replaces current state** and **merges history**.

> **Status: v1 implemented (2026-06-19).** Data layer (`BackupCodec`,
> `BackupManager`, `AppRepository.applyRestore`) + UI (Profile → Backup &
> Restore) are merged behind the **guest/signed-out-only restore** gate, with
> unit + Robolectric coverage. **Still gated for release** behind the launch
> blockers in
> [`purrodoro-android/docs/release-readiness.md`](../../../purrodoro-android/docs/release-readiness.md)
> and the two-device sync QA (NAN-65): the restore-then-sync convergence
> scenario must pass on real devices before this ships.

---

## Why (justification)

Cloud sync already backs up data for **signed-in** users, so this is not a
launch blocker. The real, distinct problems it solves:

- **Guests** (no account) have **no backup path** today — a wipe/reinstall loses
  everything.
- **Data portability / GDPR-style data-rights** — let users take their data out
  in an open, readable format.
- **Pre-sync safety net** — a local snapshot before risky operations (account
  switch, reinstall, device migration).
- **Leaving the ecosystem** — users can keep their history without an account.

If none of these justify the cost at the time of pickup, **don't build it.**

## Goals

- One-tap **Backup**: write a versioned JSON file to a user-chosen location (SAF).
- One-tap **Restore**: read a backup file, validate it, apply it safely.
- Treat the backup file as **fully untrusted input**.
- **Never** lose append-only history on restore.
- Keep the feature **free** and **out of the identity layer**.

## Non-Goals (v1)

- **Signed-in restore** — v1 restores only when **guest / signed-out** (see
  "Identity interaction"). Deterministic signed-in overwrite is deferred.
- **Encryption / password-protected backups** — v1 backups are plaintext; the UI
  warns about this. Deferred.
- **Merge UI / conflict picker** — restore semantics are fixed (replace
  current-state, union history), not user-tunable.
- **Cross-format import** (other apps' exports), **scheduled/auto cloud backups**,
  **iOS** — separate, later.

---

## Identity interaction (the load-bearing rule)

**The backup file carries data only — never `ownerId`, UUIDs, sync checkpoints,
`dirty`/`locallyModifiedAt`/tombstone bookkeeping, the Supabase session, or
`isPremium`.**

Because the file has no identity, restored data simply **inherits the device's
current ownership context**. Restore never touches the `ownerId` marker; the
existing login flow ([`resolveLoginAction`](../../../purrodoro-android/app/src/main/java/com/endiar/purrodoro/model/AuthState.kt)
→ Claim/Keep/Drop) resolves identity *afterward*, exactly as it does today.

> If the file carried an `ownerId`, restoring would poison Claim/Keep/Drop — a
> later sign-in as that id would silently `Keep` (skipping the merge), and a
> different id would `Drop` (wiping the just-restored data). Stripping it is
> what keeps this feature decoupled.

### Why v1 restricts restore to guest / signed-out

Sync is **pull-then-push with last-write-wins**. If we allowed restore while
signed in, imported rows written with `locallyModifiedAt = now` are **not
guaranteed to win**: a cloud row with a newer `updated_at` (clock skew, or a
recent edit from another device) is pulled in and overwrites the restored row
**before** the push runs. The "restore replaces my data" promise would silently
break. Rather than ship a non-deterministic restore, v1 gates it:

| Auth state at restore | Allowed? | Behavior |
|---|---|---|
| **Guest** (`ownerId == null`) | ✅ | Restores locally; a later sign-in `Claim`s + merges it up |
| **Signed out, owned by A** | ✅ | Restores locally (dirty); next sign-in as A → `Keep` delta-pushes it |
| **Signed in** | ❌ (v1) | Blocked with a notice: "Sign out to restore a backup." |

Deferred (vNext): allow signed-in restore by resetting checkpoints + forcing the
imported rows to win, documented as "restore overwrites your cloud."

---

## Restore semantics — replace current-state, **union** history

A uniform "replace everything" is **wrong**: it would delete append-only history
that has no reason to be deleted (restoring a month-old backup onto an active
device would wipe weeks of focus records). Per-collection semantics:

| Collection | Semantic | Key | Rationale |
|---|---|---|---|
| Tasks | **Replace** | `legacyId` | Current state ("my task list") |
| Categories | **Replace** | `legacyId` | Current state |
| Habits | **Replace** | `legacyId` | Current state (definitions) |
| Habit logs | **Union** | `(habitId, date)` | History; keep both sides |
| Session records | **Union** | `legacyId` / `startedAt` | Append-only audit history |
| Daily stamps | **Union (max)** | `date` | History; take `max(count)` per date |
| Profile (name, settings) | **Replace** | singleton | Current state |
| Device prefs (appearance, language, glass, scene, sound, notif-sound) | **Replace** | — | Current state |

**Tombstone, don't hard-delete.** For the *replace* collections, rows absent
from the backup become **dirty tombstones** (soft-delete), so a future push
propagates the deletion to the cloud instead of the cloud **resurrecting** them
on the next pull. This mirrors what `setTasks` / `setCategories` / `setStamps`
already do.

**Pre-restore safety backup.** Before applying any restore, automatically write
`purrodoro-autobackup-<timestamp>.json` to app-private storage. Converts a
mis-tapped confirm from a catastrophe into an annoyance. Keep the last N (e.g. 3).

---

## File format

Versioned envelope, JSON, via `kotlinx.serialization` (already a dependency),
mirroring the defaulted/`ignoreUnknownKeys` pattern in
[`TimerSettingsJson`](../../../purrodoro-android/app/src/main/java/com/endiar/purrodoro/data/TimerSettingsJson.kt).

```jsonc
{
  "schema_version": 1,
  "app_version_name": "1.0",
  "exported_at": 1750300000000,        // epoch millis, informational
  "data": {
    "profile":     { "display_name": "...", "timer_settings": { /* TimerSettingsJson */ } },
    "prefs":       { "appearance": "Light", "language": "System", "glass": "Adaptive",
                     "home_scene": "Room", "selected_sound_id": 1, "notification_sound": true },
    "categories":  [ { "id": 1, "name": "...", "color_argb": 4283215696 } ],
    "tasks":       [ { "id": 1, "title": "...", "minutes": 25, "completed": false,
                       "planned_sessions": 1, "completed_sessions": 0, "category_id": 1,
                       "priority": "N", "due_date": "2026-06-20" } ],
    "habits":      [ { "id": 1, "name": "...", "emoji": "...", "color_argb": 4283215696,
                       "created_at": 1750000000000, "occurrence": "D", "occurrence_days": "",
                       "time_block": "X" } ],
    "habit_logs":  [ { "habit_id": 1, "date": "2026-06-18", "state": "C", "progress": 1 } ],
    "records":     [ { "id": 1, "started_at": 0, "ended_at": 0, "focus_minutes": 25,
                       "short_break_minutes": 5, "long_break_minutes": 15,
                       "completed_focus_count": 4, "total_focus_count": 4,
                       "completed_short_break_count": 3, "completed_long_break_count": 1,
                       "status": "Completed", "task_name": "...", "focus_type": "Task" } ],
    "stamps":      [ { "date": "2026-06-18", "count": 3 } ]
  }
}
```

- **Domain shape (legacy ids)** — the same vocabulary the public `AppRepository`
  setters speak. The sync layer re-mints UUIDs / dirty flags on write.
- **Excluded by design:** `is_premium`, `owner_id`, UUIDs, sync checkpoints,
  `dirty`/`locallyModifiedAt`/`deleted`, runtime snapshot, pending-cycle-ack,
  interrupted focus, onboarding/tutorial flags, Supabase session token.
- Compact enum tags reuse the existing storage tags (`priority` L/N/H,
  `occurrence` D/W, `time_block` M/A/E/X, habit-log state C/S/P).

---

## Security (file is untrusted input)

- **No entitlement in the file.** `isPremium` is never exported and **ignored on
  restore** — otherwise a hand-edited JSON is a one-line premium unlock and
  "unlocked" backups could be shared. Premium stays device-local.
- **No secrets.** The Supabase refresh token lives in `EncryptedSharedPreferences`
  (not in `AppState`); it is out of scope and must never be wired in.
- **Size cap** — reject files over a hard limit (~10 MB; realistic max is a few
  hundred KB for thousands of records) before fully reading, to avoid OOM/DoS.
- **Defensive parse** — all deserialization wrapped in `runCatching`; any failure
  → clean "couldn't read this backup" message, **never a partial write**.
- **Coerce / clamp everything**: minutes `1..180`, cycle `1..4`, counts `>= 0`,
  color longs valid ARGB, string lengths bounded (title/name caps), strip control
  chars. Unknown enum tags fall back to defaults (existing decoders already do).
- **No injection surface** — Room is parameterized; Compose `Text` renders no
  markup. Still length-clamp + sanitize as defense-in-depth.
- **PII** — backups are plaintext personal data (task/habit names, history,
  display name). User-initiated, stored where the user chooses via SAF, never
  auto-uploaded. UI shows a one-line "this file is not encrypted" notice.
- **SAF only** — `CreateDocument` / `OpenDocument` hand us a user-granted URI; we
  never construct paths, so there is no path-traversal / arbitrary-write risk.
  Works on `minSdk 28` with no storage permission.

---

## Edge cases

| Case | Handling |
|---|---|
| Empty data set | Valid; empty arrays |
| Malformed / truncated JSON, or non-JSON file picked | Graceful failure, **no mutation** |
| `schema_version` newer than app supports | Reject: "Update Purrodoro to restore this backup" |
| `schema_version` older | Accept; defaulted fields fill gaps |
| Duplicate ids within the file | Last-wins dedupe |
| Task → missing category | Null the `category_id` |
| Habit log → missing habit | Drop the log |
| Invalid `LocalDate` (stamps/logs/due_date) | Skip that row; keep the rest |
| Negative / overflow numbers | Coerce into valid range |
| Restore while a focus timer is running | Stop the timer first (reuse `AccountManager`'s `onBeforeClear` hook) so the runtime snapshot isn't left stale |
| Restore while signed in | Blocked in v1 (see Identity interaction) |
| Crash between Room txn and prefs counter write | Self-heal: derive `nextId` from `max(legacyId) + 1` on read |
| Very large history | Run on IO dispatcher inside one transaction; disable button + show progress to avoid ANR / re-entrancy |
| Re-entrancy (double tap) | Disable controls while in progress |

---

## Atomicity

- The full restore (clear/tombstone + upsert across all tables) runs in **one
  Room `withTransaction`** — all-or-nothing for domain data.
- The DataStore prefs/counter write is a **separate** step (DataStore reads/writes
  don't belong inside a Room transaction). A crash between the two is bounded and
  self-healing: counters are recomputed from `max(legacyId) + 1` on the next read,
  matching the existing `clearAccountOwnedData` accepted-edge pattern.

---

## Implementation sketch

**New pure module** (fully unit-testable, no Android types):
- `BackupModels.kt` — `@Serializable` envelope + per-collection DTOs (compact tags).
- `BackupCodec.kt` — `encode(AppState) : String`, `decode(String) : Result<Backup>`
  with validation + coercion. **All security clamping lives here.**

**Repository additions** (`AppRepository`) — replace/union setters that tombstone
missing rows and mark imported rows dirty, each transactional:
- `replaceHabits(...)`, `replaceHabitLogs(...)` (union), `replaceSessionRecords(...)`
  (union), `unionStamps(...)` (max per date). (`setTasks` / `setCategories` /
  `setStamps` already exist; reuse.)
- `applyRestore(backup)` — orchestrates all of the above in **one transaction**,
  then writes prefs/counters.

**UI** — a `BackupRestoreScreen` reachable from **Profile** (next to Storage),
using `rememberLauncherForActivityResult` with `CreateDocument("application/json")`
and `OpenDocument(arrayOf("application/json"))`; file IO via `ContentResolver` on
`Dispatchers.IO`. Auth-aware copy:
- Guest → "Restore replaces tasks/habits and merges history on this device."
- Signed in → restore disabled + "Sign out to restore a backup."

---

## Testing

- **Pure (unit):** round-trip encode→decode; every coercion/clamp; every edge-case
  row in the table above; schema-version gating; premium/owner-id exclusion.
- **Integration (Robolectric + in-memory Room):** `applyRestore` produces the
  expected replace-vs-union result; **replace collections leave tombstones** (no
  resurrection); history collections **union without loss**; counters self-heal.
- **Add to NAN-65** (two-device sync matrix): a "restore-then-sync converges — no
  resurrection, no history loss, no duplicates" scenario. Pure-function tests
  alone would give false confidence; the sync interaction is the risky part.

---

## Open questions / vNext

- Deterministic **signed-in restore** (checkpoint reset + force-win) — deferred.
- **Encrypted backups** (passphrase) — deferred; v1 is plaintext + warning.
- **Selective restore** (e.g. history only) — out of scope.
- Backup file **retention** policy for auto-safety backups (default keep 3?).

---

## Reference documents

- Auth & ownership: [`2026-06-09-auth-and-account-identity.md`](2026-06-09-auth-and-account-identity.md)
- Release blockers (gating): [`purrodoro-android/docs/release-readiness.md`](../../../purrodoro-android/docs/release-readiness.md)
- Product reference: [`docs/PRODUCT.md`](../../PRODUCT.md)
