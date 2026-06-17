# Storage Management — Design

**Date:** 2026-06-17

## Problem

Remote soundscape tracks download to internal storage on demand, but a user has
no single place to see what they've downloaded or reclaim the space. Today the
only remove affordance is a per-track trash button buried in the Sounds sheet.

## Decisions (locked)

- **Dedicated screen**, not an inline Profile section. Opened from a Profile row.
- **Generic "Storage"** naming/scope so it can absorb future downloadable assets
  (not soundscape-specific copy or structure), even though soundscapes are the
  only sizable downloads today.
- **Safety-first**: never delete bytes out from under playback or mid-download;
  destructive bulk actions confirm.

## User experience

Profile → **Storage** row (with current total used as the subtitle, e.g.
"12.4 MB used") → full screen:

- **Header summary**: total space used by downloaded content.
- **List** of downloaded items: icon, name, kind label, size, per-item remove.
- **In-progress** items shown as a disabled row with progress, **not** removable.
- **"Remove all downloads"** action with a confirm dialog.
- **Empty state**: cat art + "No downloads yet" + hint pointing to Sounds.

## Architecture

Reuse existing infra; add a thin pure layer + minimal manager helpers.

### TrackDownloadManager (additions)

- `fun fileSizeBytes(track): Long` — on-disk size of the versioned file, 0 if absent.

### Pure model + builder (unit-tested)

- `StorageItem(id, name, kind, sizeBytes, state)`.
- `StorageSummary(items, totalBytes)` where `totalBytes` is the sum of the
  itemized downloaded tracks.
- `buildStorageList(catalog, downloadStates, sizeFor)` → items for **Downloaded**
  rows (sorted by size desc, then name), plus the total. In-progress rows are
  included but flagged non-removable.
- `formatBytes(Long, locale)` → "KB"/"MB" human string; handles 0 and <1 MB.

> **Scope note:** we do **not** track "other"/orphaned on-disk bytes for now.
> The total reflects only itemized downloaded catalog tracks. Orphans/`.part`
> leftovers are still swept by the existing launch-time prune. The screen name
> stays generic ("Storage") so this can grow later without a rename.

### Navigation

Mirror the History overlay pattern: `showStorage` boolean in `PurrodoroApp`,
rendered as a full-screen overlay; `ProfileScreen` gains `onOpenStorage` +
`storageUsedLabel`. New `StorageScreen` composable.

## Edge cases & safety

| Case | Handling |
|---|---|
| **Removing a track that's currently playing** | Stop playback first — null `playingMusicId` / `playingAmbienceId` for that id (mirrors the Sounds-sheet delete path) before `delete()`. |
| **Removed track is in the saved mix** | Keep it in the mix (do **not** silently edit the user's mix). It simply reverts to "not downloaded" and needs re-download to play — consistent with today's Sounds behavior. |
| **Remove all while something is playing** | `pauseSoundscape()` if any removed track was the active ambience/music. Bundled (APK) playback is unaffected — bundled tracks are never in this list. |
| **Track is mid-download** | List it as a disabled "Downloading…" row with progress; **no** remove button. Avoids racing the writer coroutine (no cancel API exists; deleting the target mid-rename could be re-created). |
| **Premium track downloaded, then entitlement lost** | Still listed and removable — the list is keyed on **download state**, not entitlement. Disk usage is real regardless of locks. |
| **Downloaded file whose catalog row vanished** (server-deactivated, not yet pruned) | Not shown and not counted for now (out of scope). The launch-time prune removes these. The generic name leaves room to surface "other" bytes later. |
| **`.part` leftovers from an interrupted download** | Not counted/shown; swept by the existing launch-time prune. Never shown as a playable item. |
| **Delete returns false (I/O failure)** | Non-fatal; the next `states`/disk read reflects reality (item stays). No crash, no false "removed" toast. |
| **Account switch (Drop) clears account data** | Downloads are **not** account-owned data — public, re-downloadable cache in internal files dir. Leave them. Document this so it isn't mistaken for a leak. |
| **Open while offline** | Fully local; names come from the cached catalog, sizes from disk. Works offline. |
| **Empty / zero downloads** | Empty state; total shows "0 MB". |
| **Live updates** | Screen recomposes off the `downloadStates` flow + catalog; removed items disappear and the total drops immediately. |
| **Localization** | All copy via `strings.xml` (en + `values-in`); size unit strings localized. |

## Out of scope

- Tracking "other"/orphaned on-disk bytes and an "Other cached files" reclaim
  row (deferred; launch-time prune still cleans orphans).
- Cancelling an in-progress download (no manager API today).
- Non-soundscape downloads (none exist yet; structure is generic for later).
- Wi-Fi-only / metered-network download policies.
- A global "clear app cache" beyond soundscape downloads.

## References

- `audio/TrackDownloadManager.kt` (download/delete/states, version-stamped files)
- `ui/SoundsScreen.kt` (existing per-track download/remove UI + safe-delete path)
- `ui/HistoryScreen.kt` + `PurrodoroApp.kt` (`showHistory` overlay pattern to mirror)
- `soundscape/SoundscapeCatalogRepository.kt` (cached catalog, `fileSizeBytes`)
