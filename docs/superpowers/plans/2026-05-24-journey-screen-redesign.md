# Journey Screen Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the existing Records/History screen with a reference-style Journey screen backed by real session data.

**Architecture:** Add a focused `JourneyModels.kt` file for pure display calculations, then replace the Compose content in `CalendarScreen.kt` while preserving the existing route and detail/delete behavior. Rename visible navigation labels to Journey without changing persistence or tab enum identity.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, JUnit 4, Android Gradle.

---

## File Structure

- Create `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/JourneyModels.kt`: pure display models and helpers for metrics, bars, calendar summaries, and timeline rows.
- Create `kuro-pomodoro-android/app/src/test/java/com/endiar/kuropomodoro/ui/JourneyModelsTest.kt`: unit tests for derived Journey data.
- Modify `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/CalendarScreen.kt`: full visual replacement using `JourneyModels`.
- Modify `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/model/KuroModels.kt`: visible tab label/symbol text from Records to Journey.
- Modify `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt`: bottom dock label from History to Journey.
- Modify `kuro-pomodoro-android/app/src/test/java/com/endiar/kuropomodoro/model/KuroModelsTest.kt`: assert Journey label is present.

### Task 1: Journey Display Models

**Files:**
- Create: `kuro-pomodoro-android/app/src/test/java/com/endiar/kuropomodoro/ui/JourneyModelsTest.kt`
- Create: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/JourneyModels.kt`

- [ ] **Step 1: Write failing tests**

Add tests that create session records for a known week and assert:

```kotlin
val summary = journeySummary(records, selectedDate, today, zone)
assertEquals(220, summary.weekFocusMinutes)
assertEquals(3, summary.focusDays)
assertEquals(73, summary.dailyAverageMinutes)
assertEquals(31, summary.weeklyGoalPercent)
assertEquals("2h 45m", summary.selectedDay.focusTimeLabel)
assertEquals(2, summary.todayRows.size)
assertEquals("9:00 AM", summary.todayRows.first().timeLabel)
assertEquals("25 min", summary.todayRows.first().durationLabel)
assertEquals("Completed", summary.todayRows.first().statusLabel)
```

- [ ] **Step 2: Run the failing test**

Run:

```bash
cd /Users/nandra.saputra/Projects/nandra/kuro-pomodoro/kuro-pomodoro-android
./gradlew testDebugUnitTest --tests com.endiar.kuropomodoro.ui.JourneyModelsTest
```

Expected: fail because `JourneyModels.kt` and `journeySummary` do not exist yet.

- [ ] **Step 3: Implement minimal display models**

Create data classes for `JourneySummary`, `JourneyDayBar`, `JourneyDaySummary`, and `JourneyTimelineRow`. Implement `journeySummary` using `SessionRecord` only.

- [ ] **Step 4: Run model tests**

Run the same Gradle test command. Expected: tests pass.

### Task 2: Full Journey UI Replacement

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/CalendarScreen.kt`

- [ ] **Step 1: Replace screen structure**

Replace the existing top-level layout with:

- Header `Journey` and subtitle.
- Segmented period control.
- Two-column metric card grid.
- Weekly chart card.
- Calendar card.
- Today timeline card.

- [ ] **Step 2: Preserve interactions**

Keep selected month/date state, previous/next month buttons, session detail sheet opening from timeline rows, and delete callback forwarding.

- [ ] **Step 3: Remove activity-type labels**

Ensure timeline rows do not render labels such as Pomodoro, Deep Work, Study, or Night Focus.

### Task 3: Visible Renames

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/model/KuroModels.kt`
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt`
- Modify: `kuro-pomodoro-android/app/src/test/java/com/endiar/kuropomodoro/model/KuroModelsTest.kt`

- [ ] **Step 1: Rename tab display text**

Change `AppTab.Calendar("Records", "Records")` to `AppTab.Calendar("Journey", "Journey")`.

- [ ] **Step 2: Rename dock label**

Change the dashboard dock label for `AppTab.Calendar` from `History` to `Journey`.

- [ ] **Step 3: Update test expectations**

Assert the Calendar tab label is `Journey`.

### Task 4: Verification

**Files:**
- Verify all modified Kotlin files.

- [ ] **Step 1: Run targeted unit tests**

```bash
cd /Users/nandra.saputra/Projects/nandra/kuro-pomodoro/kuro-pomodoro-android
./gradlew testDebugUnitTest --tests com.endiar.kuropomodoro.ui.JourneyModelsTest --tests com.endiar.kuropomodoro.model.KuroModelsTest
```

- [ ] **Step 2: Run full unit tests**

```bash
cd /Users/nandra.saputra/Projects/nandra/kuro-pomodoro/kuro-pomodoro-android
./gradlew testDebugUnitTest
```

- [ ] **Step 3: Build debug APK**

```bash
cd /Users/nandra.saputra/Projects/nandra/kuro-pomodoro/kuro-pomodoro-android
./gradlew assembleDebug
```

