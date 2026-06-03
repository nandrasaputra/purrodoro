# Journey Screen Redesign Design

## Goal

Replace the current Records/History screen with a Journey screen that closely follows the provided Progress reference image while using Purrodoro's real persisted session data.

## Scope

- Rename visible history/records language to Journey.
- Replace `CalendarScreen` content with a reference-style layout: header, period segmented control, metric cards, weekly focus chart, focus calendar, selected-day summary, and today's timeline.
- Keep existing `SessionRecord`, `stamps`, detail sheet, and delete behavior.
- Remove timeline activity-type labels such as Pomodoro, Deep Work, Study, and Night Focus. Timeline rows show time, duration, optional task name, and status only.

## Data

The screen derives display data from:

- `SessionRecord.startedAt`, `endedAt`, `completedFocusCount`, `focusMinutes`, `status`, and optional `taskName`.
- `stamps` for focus-day markers and streak continuity where needed.

No new persistence model is introduced. Weekly/monthly/yearly tabs are visual navigation for this pass; the main reference layout stays week-focused.

## UI

The replacement uses a warm light reference palette independent of the timer backdrop glass treatment, matching the attached image more closely than the current translucent card style.

Sections:

- Header: `Journey`, subtitle `Your focus journey.`, and a circular cat avatar.
- Segmented control: `This week`, `This month`, `This year`.
- Four stat cards: weekly focus time, focus days, daily average, and weekly goal.
- Weekly bar chart: seven bars from real focus minutes.
- Focus calendar: current month grid with focused days marked and selected day highlighted.
- Selected-day strip: selected date, focus time, and completed focus session count.
- Today timeline: real records for today, with status chips and no type labels.

## Testing

Add pure Kotlin display-model tests before UI implementation for:

- Weekly total minutes and day bars.
- Focus days count.
- Daily average across focused days.
- Weekly goal percent.
- Selected-day summary.
- Timeline rows without activity-type labels.

