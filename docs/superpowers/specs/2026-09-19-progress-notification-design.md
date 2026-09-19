# Progress-Centric Timer Notification — Design

Date: 2026-09-19 · Status: approved design, pre-plan · Scope: Android only

Replaces the running timer's compat progress bar with Android 16's
`Notification.ProgressStyle` on API 36+, keeping the current rendering as the
fallback on older OS versions. Foundation for a follow-up Live Update
promotion spec (promotion requires a qualifying style, which this provides).

## Goal

Give the ongoing Pomodoro cycle a system-grade progress surface: one bar for
the whole cycle, color-coded Focus/Break segments, milestone points at each
completed Focus, and a state-reflecting tracker icon — while changing nothing
about countdown text, actions, channels, or update plumbing.

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
- Segments built dynamically from `session.items` (2–8 items for N = 1..4):
  Focus = accent, ShortBreak = secondary, LongBreak = tertiary.
- Points at the cumulative end of each Focus item (focus completions).
- `setStyledByProgress(false)` — segments carry the color, brand chrome stays.
- Tracker icon reflects Playing / Paused / break state.
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

- `focusSessionsPerCycle` 1..4 → segments always derived from the live `items`
  list, which is fixed at cycle start (mid-cycle setting edits can't skew it).
- Paused freezes progress and swaps the tracker icon to the paused glyph.
- Process-death reconcile recomputes from `endsAtMillis`; no special casing.
- Very short (1-minute test) segments may cluster points — acceptable.
- Update cadence stays at the current 1s tick (battery parity; throttling is a
  future optimization, not this spec).

## Testing

- Unit: pure cycle → progress/segments/points mapping across N = 1..4,
  mid-cycle index, paused state, and boundary ticks.
- Robolectric: built notification smoke test (ongoing flag, channel, style
  present on 36, legacy style below 36).
- Manual: shade + lockscreen on API 36; fallback rendering on API 28 and 35;
  pause / skip / segment-boundary transitions.

## Follow-ups (out of scope)

- Live Update promotion spec (permission, promotion request, delete-intent
  handling, OEM criteria).
- Richer haptics, AppFunctions — independent specs in queue.
