# Progress-Centric Timer Notification — Design

Date: 2026-09-19 · Status: implemented (single-bar revision) · Scope: Android only

Replaces the running timer's compat progress bar with Android 16's
`Notification.ProgressStyle` on API 36+, keeping the current rendering as the
fallback on older OS versions. Foundation for a follow-up Live Update
promotion spec (promotion requires a qualifying style, which this provides).

## Goal

Give the ongoing Pomodoro cycle a calm, system-grade progress surface: one
coral bar for whole-cycle elapsed time with a dot tracker — matching the
brand instead of repeating cycle structure (which already lives in the app UI
and the notification text).

## Non-goals

- Live Update promotion (`POST_PROMOTED_NOTIFICATIONS`, `setRequestPromotedOngoing`) — separate spec.
- Changing notification update cadence, channels, completion notifications, or action behavior.
- Any `RemoteViews` custom layout (would disqualify future promotion).

## Architecture

New SDK-gated builder alongside `buildRunningNotification` in
`timer/TimerNotification.kt`:

- `if (Build.VERSION.SDK_INT >= 36)`: framework `Notification.Builder` +
  `Notification.ProgressStyle`. (`NotificationCompat` has no ProgressStyle
  backport, so the framework builder is required on 36+.)
- Else: today's compat `setProgress` path, unchanged.

Pure mapping function (unit-testable, no framework):

- Scale `0..totalCycleSeconds`, `progress = elapsedCycleSeconds`.
- Elapsed derived from existing state: sum of seconds of items before
  `currentIndex` + (`segmentTotal − segmentRemaining`), reusing the
  `endsAtMillis`-vs-`remainingSeconds` logic already in
  `currentRemainingSeconds`.
- One full-length coral segment (`DockCoral`) carries the bar color; a dot
  tracker (`notif_tracker_dot`: DeepCoral core, white halo) marks now.
  Deliberately no per-segment colors or milestone points — device testing
  showed them noisy against the calm brand; cycle structure stays in the
  notification text.
- `setStyledByProgress(false)` — brand colors hold.
- Title, countdown text, and pause/resume/stop/skip actions reuse `describe()`
  and the existing action builders verbatim. Ongoing, silent,
  only-alert-once, `CATEGORY_PROGRESS`, public visibility, and content intent
  are preserved.

## Data flow

No new plumbing. The existing tick re-notify in `TimerService`
(`NOTIF_ID_RUNNING`) rebuilds the style each tick. Pause/skip/segment-end
rebuild with frozen or advanced progress. Cycle completion keeps the current
completion notification. String resolution keeps using `localized()` so the
in-app language applies; segment/point colors are locale-independent.

## Edge cases

- `focusSessionsPerCycle` 1..4 → total derived from the live `items`
  list, which is fixed at cycle start (mid-cycle setting edits can't skew it).
- Paused freezes progress at the current elapsed value.
- Process-death reconcile recomputes from `endsAtMillis`; no special casing.
- Update cadence stays at the current 1s tick (battery parity; throttling is a
  future optimization, not this spec).

## Testing

- Unit: pure cycle → total/elapsed mapping across N = 1..4,
  mid-cycle index, paused state, and boundary ticks.
- Robolectric: built notification smoke test (ongoing flag, channel, style
  present on 36, legacy style below 36).
- Manual: shade + lockscreen on API 36; fallback rendering on API 28 and 35;
  pause / skip / segment-boundary transitions.

## Follow-ups (out of scope)

- Live Update promotion spec (permission, promotion request, delete-intent
  handling, OEM criteria).
- Richer haptics, AppFunctions — independent specs in queue.
