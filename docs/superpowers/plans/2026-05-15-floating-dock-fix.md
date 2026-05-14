# Floating Dock Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `DashboardDock` truly float above screen content by overlaying it with a `Box`, and replace all hardcoded `bottom = 100.dp` workarounds with dynamic padding derived from the dock's measured height.

**Architecture:** Outer `Column` in `KuroPomodoroApp` becomes a `Box`. The content area fills full size. The dock is overlaid at `Alignment.BottomCenter`. The dock's pixel height is measured via `onSizeChanged`, converted to `Dp`, and passed as `contentPadding: PaddingValues` to each scrollable screen.

**Tech Stack:** Kotlin, Jetpack Compose — no new dependencies.

---

## File Map

| File | Change |
|------|--------|
| `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt` | Layout refactor: `Column` → `Box`, measure dock height, pass `contentPadding` to screens |
| `app/src/main/java/com/endiar/kuropomodoro/ui/CalendarScreen.kt` | Add `contentPadding: PaddingValues` param, remove hardcoded bottom padding |
| `app/src/main/java/com/endiar/kuropomodoro/ui/TodoScreen.kt` | Same |
| `app/src/main/java/com/endiar/kuropomodoro/ui/SoundsScreen.kt` | Same |
| `app/src/main/java/com/endiar/kuropomodoro/ui/ProfileScreen.kt` | Same |

---

## Task 1: Add `contentPadding` parameter to all four scrollable screens

Add a `contentPadding: PaddingValues` parameter to each screen and use it in place of the hardcoded `bottom = 100.dp`. Do all four screens in one commit.

**Files:**
- Modify: `app/src/main/java/com/endiar/kuropomodoro/ui/CalendarScreen.kt:45-51`
- Modify: `app/src/main/java/com/endiar/kuropomodoro/ui/TodoScreen.kt:69-79`
- Modify: `app/src/main/java/com/endiar/kuropomodoro/ui/SoundsScreen.kt:35-43`
- Modify: `app/src/main/java/com/endiar/kuropomodoro/ui/ProfileScreen.kt:43-54`

- [ ] **Step 1: Update `CalendarScreen` signature and scroll padding**

In `CalendarScreen.kt`, add `contentPadding` to the function signature and apply it:

```kotlin
fun CalendarScreen(
    stamps: Map<LocalDate, Int>,
    completedSessions: Int,
    sessionRecords: List<SessionRecord> = emptyList(),
    modifier: Modifier = Modifier,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    contentPadding: PaddingValues = PaddingValues(0.dp),  // ← add this
) {
```

Then find the scroll `Column` modifier (around line 71–74) and replace:
```kotlin
// Before
.padding(top = 8.dp, bottom = 100.dp),

// After
.padding(top = 8.dp)
.padding(contentPadding),
```

Add the import at the top of the file if not already present:
```kotlin
import androidx.compose.foundation.layout.PaddingValues
```

- [ ] **Step 2: Update `TodoScreen` signature and scroll padding**

In `TodoScreen.kt`, add `contentPadding` to the function signature:

```kotlin
fun TodoScreen(
    todoLists: List<TodoListState>,
    selectedListId: Int,
    onListSelected: (Int) -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onFocusTask: (FocusTask) -> Unit,
    settings: TimerSettings = TimerSettings(),
    modifier: Modifier = Modifier,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    contentPadding: PaddingValues = PaddingValues(0.dp),  // ← add this
) {
```

Then find the scroll `Column` modifier (around line 101–104) and replace:
```kotlin
// Before
.padding(top = 8.dp, bottom = 100.dp),

// After
.padding(top = 8.dp)
.padding(contentPadding),
```

Add import if missing:
```kotlin
import androidx.compose.foundation.layout.PaddingValues
```

- [ ] **Step 3: Update `SoundsScreen` signature and scroll padding**

In `SoundsScreen.kt`, add `contentPadding` to the function signature:

```kotlin
fun SoundsScreen(
    sounds: List<SoundOption>,
    selectedSoundId: Int,
    playingSoundId: Int?,
    onSoundSelected: (SoundOption) -> Unit,
    onToggleSound: (SoundOption) -> Unit,
    modifier: Modifier = Modifier,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    contentPadding: PaddingValues = PaddingValues(0.dp),  // ← add this
) {
```

Then find the scroll `Column` modifier (around line 47–50) and replace:
```kotlin
// Before
.padding(top = 8.dp, bottom = 100.dp),

// After
.padding(top = 8.dp)
.padding(contentPadding),
```

Add import if missing:
```kotlin
import androidx.compose.foundation.layout.PaddingValues
```

- [ ] **Step 4: Update `ProfileScreen` signature and scroll padding**

In `ProfileScreen.kt`, add `contentPadding` to the function signature:

```kotlin
fun ProfileScreen(
    settings: TimerSettings,
    darkModePreview: Boolean,
    glassReadabilityMode: GlassReadabilityMode,
    glassTreatment: GlassTreatment,
    onDarkModeChanged: (Boolean) -> Unit,
    onGlassReadabilityModeChanged: (GlassReadabilityMode) -> Unit,
    onDurationChanged: (TimerMode, Int) -> Unit,
    onCycleSizeChanged: (Int) -> Unit,
    onMockFeature: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),  // ← add this
) {
```

Then find the scroll `Column` modifier (around line 58–61) and replace:
```kotlin
// Before
.padding(top = 8.dp, bottom = 100.dp),

// After
.padding(top = 8.dp)
.padding(contentPadding),
```

Add import if missing:
```kotlin
import androidx.compose.foundation.layout.PaddingValues
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/endiar/kuropomodoro/ui/CalendarScreen.kt \
        app/src/main/java/com/endiar/kuropomodoro/ui/TodoScreen.kt \
        app/src/main/java/com/endiar/kuropomodoro/ui/SoundsScreen.kt \
        app/src/main/java/com/endiar/kuropomodoro/ui/ProfileScreen.kt
git commit -m "refactor: add contentPadding param to all scrollable screens"
```

---

## Task 2: Refactor `KuroPomodoroApp` layout to floating dock

Replace the outer `Column` + `Box(weight=1f)` structure with a `Box` overlay. Measure the dock height and pass it as `contentPadding` to each screen.

**Files:**
- Modify: `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`

- [ ] **Step 1: Add required imports to `KuroPomodoroApp.kt`**

Ensure these imports are present (add any that are missing):

```kotlin
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
```

`onSizeChanged` and `LocalDensity` are the new ones needed.

- [ ] **Step 2: Add dock height state variable inside `KuroPomodoroApp`**

Find the block of `var`/`val` state declarations near the top of `KuroPomodoroApp` (after the `appState` collection) and add:

```kotlin
var dockHeightPx by remember { mutableIntStateOf(0) }
```

- [ ] **Step 3: Replace the outer `Column` with `Box` + overlay layout**

Find this block near the bottom of `KuroPomodoroApp` (starts around the `CatBackdrop` call):

```kotlin
CatBackdrop(glassTreatment = glassTreatment, time = backdropTime) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            when (selectedTab) {
                // ... screen routing ...
            }
        }
        premiumSheetMessage?.let { message ->
            PremiumMessage(...)
        }
        if (!(isFocusMode && isRunning)) {
            DashboardDock(
                ...
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}
```

Replace it with:

```kotlin
CatBackdrop(glassTreatment = glassTreatment, time = backdropTime) {
    val density = LocalDensity.current
    val dockHeightDp = with(density) { dockHeightPx.toDp() }
    val screenContentPadding = PaddingValues(bottom = dockHeightDp)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            when (selectedTab) {
                AppTab.Timer -> FocusDashboard(
                    // ... all existing params unchanged ...
                )
                AppTab.Todo -> TodoScreen(
                    // ... all existing params ...
                    contentPadding = screenContentPadding,
                )
                AppTab.Calendar -> CalendarScreen(
                    // ... all existing params ...
                    contentPadding = screenContentPadding,
                )
                AppTab.Sounds -> SoundsScreen(
                    // ... all existing params ...
                    contentPadding = screenContentPadding,
                )
                AppTab.Profile -> ProfileScreen(
                    // ... all existing params ...
                    contentPadding = screenContentPadding,
                )
            }
        }
        premiumSheetMessage?.let { message ->
            PremiumMessage(
                message = message,
                onDismiss = { premiumSheetMessage = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp)
                    .padding(bottom = dockHeightDp + 8.dp)
            )
        }
        if (!(isFocusMode && isRunning)) {
            DashboardDock(
                isRunning = isRunning,
                isSoundPlaying = isSoundPlaying,
                onNavigate = { selectedTab = it },
                selectedTab = selectedTab,
                glassTreatment = glassTreatment,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .onSizeChanged { dockHeightPx = it.height }
            )
        }
    }
}
```

Key changes:
- Outer `Column` → `Box(fillMaxSize)`
- Inner content `Box` loses `weight(1f)`, gains `fillMaxSize()`
- `DashboardDock` gets `align(Alignment.BottomCenter)` + `onSizeChanged`
- `PremiumMessage` gets `align(Alignment.BottomCenter)` so it sits above the dock
- `contentPadding = screenContentPadding` passed to all 4 scrollable screens

- [ ] **Step 4: Verify the project compiles**

From the project root:

```bash
cd kuro-pomodoro-android && ./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`. If there are compile errors, fix them (likely missing imports or wrong parameter names) before committing.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt
git commit -m "feat: float DashboardDock above content using Box overlay and dynamic contentPadding"
```

---

## Done

At this point:
- The dock overlays all screen content and is never hidden behind it
- Scrollable screens clear the dock by exactly the dock's measured height (including system nav bar insets)
- No hardcoded `bottom = 100.dp` magic numbers remain
- The `PremiumMessage` banner sits above the dock when visible
