# Pomocat-Inspired Compose Clone Design

## Context

The starter project is a minimal Jetpack Compose Android app in `purrodoro-android/app`. It currently contains the default `MainActivity` greeting screen, a generated Material3 theme, and no app-specific screens or state.

The target reference is Pomocat - Cute Pomodoro Timer (`com.enfpdev.pomocat`) on Google Play. Public listing information describes a cute cat Pomodoro timer with white noise, focus-together flows, stamp calendar progress tracking, dark mode, flexible timer settings, reminders, D-day tracking, alarm sounds, additional cat animations, and multiple todo lists. Public screenshot URLs and Play Store metadata are used for visual research only. The shipped app will use original Compose-drawn placeholder art rather than bundling Pomocat-owned images.

## Scope

Build a broad first-pass Android clone in Jetpack Compose with working local behavior and high visual similarity in layout, interaction model, and tone.

Included:

- Working Pomodoro timer with focus, short break, and long break modes.
- Start, pause, resume, reset, and session-complete behavior.
- Cat companion focus screen using original placeholder illustration.
- Bottom-tab app shell for timer, todo, calendar, sounds, and profile/settings.
- Local todo lists with add, complete, and delete interactions.
- Calendar stamp grid that records completed focus sessions locally during runtime.
- Sound selection UI with mock playback state and locked/premium badges.
- Local-only simulation for premium, account, reminders, D-day, friends, ads, and purchases.
- Light/dark visual handling controlled by app UI where practical.
- Responsive Compose layout for common phone sizes.

Excluded from the first pass:

- Backend services, real authentication, real friend rooms, real billing, ads, or purchases.
- Persisted storage across app restarts.
- Real bundled audio playback.
- Pomocat-owned image assets or animations.

## Architecture

The implementation will stay in the existing single Android app module. `MainActivity` will host a `KuroPomodoroApp` root composable.

Use a small in-memory state holder at the Compose root instead of introducing database, networking, or dependency injection. This keeps the first clone easy to inspect and avoids unnecessary infrastructure.

Suggested package structure:

- `com.endiar.purrodoro.MainActivity`: Android entry point.
- `com.endiar.purrodoro.KuroPomodoroApp`: top-level app shell and state ownership.
- `com.endiar.purrodoro.model`: simple data classes and enums for tabs, timer modes, tasks, sound options, calendar stamps, and mock premium state.
- `com.endiar.purrodoro.ui`: screen composables.
- `com.endiar.purrodoro.ui.components`: reusable buttons, cards, cat illustration, timer ring, bottom navigation, chips, and empty states.
- `com.endiar.purrodoro.ui.theme`: Pomocat-inspired original palette and typography refinements.

## Screens And Behavior

Timer screen:

- Pastel scene background with rounded cards and soft decorative shapes.
- Header with greeting, streak/session summary, and simulated profile icon.
- Mode chips for focus, short break, and long break.
- Large circular timer ring displaying remaining time.
- Original cat placeholder illustration placed as the emotional anchor of the screen.
- Current task card and quick task picker using local todo state.
- Primary start/pause/resume control plus reset.
- On focus completion, add a stamp for the current date and increment session count.

Todo screen:

- Multiple local list tabs such as Study, Home, and Ideas.
- Task rows with checkbox, title, optional focus minutes, and delete action.
- Inline add-task field or bottom add area.
- Premium simulation card for multiple todo lists if needed, but core local lists remain usable.

Calendar screen:

- Month header with completed-session summary.
- Seven-column stamp calendar.
- Dates with completed focus sessions show cute paw/star stamps.
- Empty days remain soft outlined cells.

Sounds screen:

- White-noise option cards such as Rain, Cafe, Forest, Fireplace, Night, and Keyboard.
- Selecting a sound updates local mock playback state.
- Some items show premium lock badges and open a local premium sheet instead of playing.

Profile/settings screen:

- Local-only profile card.
- Timer duration controls for focus, short break, and long break.
- Theme toggle simulation.
- Reminder, D-day, focus together, and premium cards that explain they are mocked locally.

## State And Data Flow

All state lives in Compose runtime state for the first pass:

- Selected tab.
- Timer mode, remaining seconds, running state, and configured durations.
- Session count and date stamps.
- Todo lists and task completion status.
- Selected sound and mock playing/paused state.
- Mock premium/profile/settings toggles.

Timer countdown can use `LaunchedEffect` keyed by running state and remaining seconds. When remaining seconds reaches zero, the app records completion for focus sessions and resets or advances to break mode.

## Visual System

The visual language should be cute, warm, and calm:

- Cream, peach, pink, muted lavender, mint, and chocolate-brown text colors.
- Rounded cards with soft shadows or tonal separation.
- Friendly typography using the available Compose default font stack with heavier rounded-feeling weights.
- Original vector-like Compose cat placeholder built from simple shapes.
- Icons may use text symbols or simple Compose shapes to avoid adding icon dependencies unless already available.

The UI should be inspired by Pomocat's product direction, not a direct asset copy.

## Error Handling And Edge Cases

- Timer duration controls must not allow zero or negative durations.
- Reset should restore the selected mode's configured duration.
- Completing a focus session multiple times on the same date increments the day's count rather than duplicating date cells.
- Locked sound and premium actions should show local explanatory UI rather than failing silently.
- Empty todo lists and empty calendar states should show friendly empty states.

## Testing And Verification

Verification for the first pass:

- Run Gradle unit tests with `./gradlew test`.
- Run Android build with `./gradlew assembleDebug`.
- Manually inspect Compose previews or emulator behavior if available.
- Confirm the app launches without the generated greeting screen.
- Confirm timer controls, tab navigation, todos, calendar stamps, sound selection, and mock premium flows work locally.

## Implementation Notes

Prefer the smallest correct implementation. Avoid adding navigation, persistence, DI, networking, billing, ads, or audio dependencies unless the first implementation proves they are necessary. Keep files focused, but do not over-split small composables prematurely.
