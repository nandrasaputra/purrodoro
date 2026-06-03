# Home/Timer UI Revamp Design

## Summary

Revamp only the Home/Timer screen to closely match the provided reference UI while keeping the existing Purrodoro timer engine, persistence, records, task progress, habit progress, notifications, and completion behavior intact.

This is a presentation-layer redesign. The target is a near-1:1 Compose implementation of the reference Home screen, adapted only where current app state or existing assets require it.

## Scope

In scope:

- Replace the current Timer tab dashboard presentation with the new Home layout.
- Support both idle and active/resume Home states from the reference.
- Keep existing timer controls wired to `TimerConnection.controller`.
- Reuse existing app state from `AppRepository`, `TimerRuntimeState`, `TimerSettings`, tasks, records, and stamps.
- Reuse existing time-based cat backdrop artwork where possible.
- Add small pure display helpers only if needed for metrics, greeting copy, or Pomodoro progress text.

Out of scope:

- Redesigning Tasks, Habits, History/Calendar, Profile, or Stats screens.
- Changing the timer state machine, foreground service, alarms, notification behavior, or DataStore schema.
- Changing Pomodoro cycle semantics.
- Adding a generated or remote image pipeline.
- Implementing the full bottom navigation redesign beyond what is necessary to make Home fit the current app shell.

## Visual Direction

The Home screen should read as a soft, warm, cozy productivity surface:

- Cream/off-white app background.
- Rounded cards with subtle borders and low, warm shadows.
- Coral primary action buttons.
- Large illustrated cat room hero as the first major visual element.
- Editorial greeting typography with the user name as the strongest text signal.
- Compact metric cards and task cards below the hero.
- Bottom dock remains available in the current app shell, but the Home content should visually align with the reference.

The implementation should avoid the current full-screen dark glass dashboard look for the Home/Timer tab. The timer experience should feel card-based and calm, matching the reference.

## Idle Home State

When no runtime timer state is active, Home shows:

1. Header
   - Time-aware greeting: `Good morning,`, `Good afternoon,`, or `Good evening,`.
   - User name: `Nandra`.
   - Small leaf accent beside the name.
   - Encouragement line such as `Let's make this evening meaningful.`.
   - Circular cat avatar with a small coral status dot.

2. Hero
   - Large rounded rectangle cat room image.
   - Use existing time-based cat artwork first.
   - Image corners and proportions should follow the reference.
   - A coral `Start Focus` button overlays the lower hero area with a play icon and elevated pill shape.

3. Continue Card
   - Section title: `Continue where you left off`.
   - One task row showing the active or most relevant task.
   - Code-style icon tile.
   - Task title.
   - Pomodoro progress copy, for example `2 / 5 Pomodoros completed`.
   - Chevron affordance.
   - If no task exists, show a calm empty row that still preserves the layout height.

4. Today Metrics
   - Card title: `Today's Focus`.
   - Optional `View all` affordance.
   - Three metrics: focus time, sessions, completed.
   - Values derive from existing records/stamps where practical. If an exact focus-time aggregate is not already available, derive it from completed/partial session records without changing persistence.

5. Quote Card
   - Text: `Little by little, a lot becomes a lot.`
   - Small heart accent.
   - Sleeping-cat illustration area.
   - If no sleeping-cat asset exists, draw a simple sleeping-cat Compose illustration for this pass.

## Active/Resume Home State

When a runtime timer state exists and the session is not cold idle:

1. The header and hero remain at the top.
2. A large frosted timer card overlaps the lower hero area.
3. The timer card contains:
   - Code-style task icon tile.
   - Task title, falling back to a generic focus label when no task is selected.
   - Category/subtitle line such as `Deep Work`.
   - Small `Edit` affordance, presentational for this pass unless an existing edit action is available.
   - Large remaining time, using the current runtime countdown display.
   - `remaining` caption.
   - Circular Pomodoro progress indicator with text like `Pomodoro 2 of 5`.
   - Cycle dots reflecting current cycle progress using existing cycle item/index state.
   - Coral CTA:
     - `Resume Focus` when paused.
     - `Pause Focus` when playing.
     - `Start Focus` if a warm idle state is displayed.

4. The existing stop and skip behavior should remain reachable if the current Home design needs secondary controls. The primary reference does not emphasize them, so secondary controls can be subtle as long as they are not lost for active sessions.

## Data Flow

The new Home UI reads the same inputs currently used by the Timer tab:

- `runtimeState` from `TimerConnection.state`.
- `settings` from `AppState.settings`.
- `tasks`, `sessionRecords`, `stamps`, and selected task state from `PurrodoroApp`.
- Timer actions from `TimerConnection.controller`.

Display-only derived values:

- Greeting copy from `AppClock.now()`.
- Remaining seconds from `runtimeState.endsAtMillis` while playing, otherwise `session.remainingSeconds`.
- Pomodoro count from focus segments in `runtimeState.session.items`, current index, and task progress where appropriate.
- Today metrics from existing records/stamps.

No new persistence fields are required.

## Components

Prefer small, focused composables rather than expanding the already large `PurrodoroApp.kt` further.

Candidate components:

- `HomeTimerScreen`
- `HomeHeader`
- `HomeHeroCard`
- `HomePrimaryTimerCard`
- `ContinueTaskCard`
- `TodayFocusCard`
- `HomeQuoteCard`
- `HomeCycleDots`

Shared visual tokens can live near existing UI components if they are Home-specific, or in theme files only if reused beyond Home.

## Behavior

- Start from idle calls the existing start flow:
  - request notification permission.
  - enter the current focus-mode state if still needed by the app shell.
  - call `timerConnection.controller.start(settings, activeTask)`.
- Playing CTA pauses via `timerConnection.controller.pause()`.
- Paused CTA resumes via `timerConnection.controller.resume()`.
- Stop, skip, completion acknowledgement, records, stamps, and habit/task increments keep their current behavior.
- The completion dialog remains unchanged for this pass.

## Testing

Add or update tests only where new pure helpers are introduced.

Suggested coverage:

- Greeting selection by time.
- Today metric aggregation, if implemented as a pure helper.
- Pomodoro progress label for idle, paused, playing, and warm-reset states, if implemented as a pure helper.

Run the existing JVM test suite after implementation:

```bash
./gradlew test
```

If Gradle cannot run because the sandbox blocks access to the local Gradle wrapper cache, report that explicitly.

## Acceptance Criteria

- Home/Timer visually matches the provided reference closely in structure, spacing, hierarchy, and warm card-based style.
- Idle state shows header, hero, Start Focus CTA, continue card, metrics card, and quote card.
- Active/resume state shows the overlapping timer card with task, remaining time, Pomodoro progress, cycle dots, and CTA.
- Existing timer lifecycle behavior is unchanged.
- No DataStore schema migration is introduced.
- Secondary tabs continue to work unchanged.
- The app compiles and relevant tests pass, or any environment blocker is documented.
