# Sounds Bottom Sheet Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the ambient Sounds feature from a dock tab into a `ModalBottomSheet` opened via a rotating music icon on the Focus screen.

**Architecture:** Remove `AppTab.Sounds` from the enum and its tab rendering branch; reuse `SoundsScreen` composable inside a `ModalBottomSheet` opened from a new `MusicButton` in `FocusDashboard`. All sound state (`playingSoundId`, `selectedSoundId`, callbacks) remains unchanged — only the presentation layer changes.

**Tech Stack:** Kotlin, Jetpack Compose, Material3 (`ModalBottomSheet`), Compose animation (`rememberInfiniteTransition`)

---

## File Map

| File | Change |
|------|--------|
| `app/src/main/java/com/endiar/purrodoro/model/KuroModels.kt` | Remove `AppTab.Sounds` entry |
| `app/src/main/java/com/endiar/purrodoro/ui/components/KuroComponents.kt` | Remove `isSoundPlaying` param and Sounds `DockTabItem` from `DashboardDock` |
| `app/src/main/java/com/endiar/purrodoro/KuroPomodoroApp.kt` | Remove Sounds tab branch, add sheet state + `ModalBottomSheet`, add `MusicButton` composable, update `FocusDashboard` signature, fix `AppTabSaver` |
| `app/src/test/java/com/endiar/purrodoro/model/KuroModelsTest.kt` | Add test: AppTab has 4 entries, Sounds is absent |

---

## Task 1: Remove `AppTab.Sounds` from the enum

**Files:**
- Modify: `app/src/main/java/com/endiar/purrodoro/model/KuroModels.kt:7-13`
- Test: `app/src/test/java/com/endiar/purrodoro/model/KuroModelsTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `KuroModelsTest` (after the existing `@Test` methods):

```kotlin
@Test
fun appTabHasFourEntriesAndNoSoundsTab() {
    assertEquals(4, AppTab.entries.size)
    assertFalse(AppTab.entries.any { it.name == "Sounds" })
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run from `purrodoro-android/`:
```bash
./gradlew :app:testDebugUnitTest --tests "com.endiar.purrodoro.model.KuroModelsTest.appTabHasFourEntriesAndNoSoundsTab"
```
Expected: FAIL — `expected:<4> but was:<5>`

- [ ] **Step 3: Remove `Sounds` from the `AppTab` enum**

In `app/src/main/java/com/endiar/purrodoro/model/KuroModels.kt`, change:

```kotlin
enum class AppTab(val label: String, val symbol: String) {
    Timer("Focus", "Home"),
    Todo("Todo", "Todo"),
    Calendar("Records", "Records"),
    Sounds("Sounds", "Sound"),
    Profile("Me", "Me")
}
```

to:

```kotlin
enum class AppTab(val label: String, val symbol: String) {
    Timer("Focus", "Home"),
    Todo("Todo", "Todo"),
    Calendar("Records", "Records"),
    Profile("Me", "Me")
}
```

- [ ] **Step 4: Run the test to confirm it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.endiar.purrodoro.model.KuroModelsTest.appTabHasFourEntriesAndNoSoundsTab"
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/model/KuroModels.kt \
        app/src/test/java/com/endiar/purrodoro/model/KuroModelsTest.kt
git commit -m "feat: remove AppTab.Sounds from enum"
```

---

## Task 2: Update `DashboardDock` — remove Sounds tab item and unused `isSoundPlaying` param

**Files:**
- Modify: `app/src/main/java/com/endiar/purrodoro/ui/components/KuroComponents.kt:315-370`
- Modify: `app/src/main/java/com/endiar/purrodoro/KuroPomodoroApp.kt:505-516`

After Task 1, the project won't compile because `DashboardDock` references `AppTab.Sounds` and `KuroPomodoroApp` passes `isSoundPlaying`. Fix both.

- [ ] **Step 1: Remove `isSoundPlaying` param and Sounds `DockTabItem` from `DashboardDock`**

In `KuroComponents.kt`, change the `DashboardDock` function from:

```kotlin
@Composable
fun DashboardDock(
    isRunning: Boolean,
    isSoundPlaying: Boolean,
    onNavigate: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: AppTab? = null,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    overBackdrop: Boolean = true,
) {
    val tokens = glassTokens(glassTreatment, overBackdrop)
    Row(
        modifier = modifier
            .padding(horizontal = 21.dp)
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .height(62.dp)
            .border(1.dp, tokens.dockBorder, RoundedCornerShape(36.dp))
            .clip(RoundedCornerShape(36.dp))
            .background(tokens.dockFill)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockTabItem(
            icon = Icons.Default.List,
            label = "TODO",
            active = selectedTab == AppTab.Todo,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Todo) }
        DockTabItem(
            icon = Icons.Default.DateRange,
            label = "RECORDS",
            active = selectedTab == AppTab.Calendar,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Calendar) }
        DockTabItem(
            icon = if (isRunning) Icons.Default.PlayArrow else Icons.Default.Home,
            label = "HOME",
            active = selectedTab == AppTab.Timer,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Timer) }
        DockTabItem(
            icon = Icons.Default.Notifications,
            label = "SOUNDS",
            active = selectedTab == AppTab.Sounds,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Sounds) }
        DockTabItem(
            icon = Icons.Default.Person,
            label = "Profile",
            active = selectedTab == AppTab.Profile,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Profile) }
    }
}
```

to:

```kotlin
@Composable
fun DashboardDock(
    isRunning: Boolean,
    onNavigate: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: AppTab? = null,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    overBackdrop: Boolean = true,
) {
    val tokens = glassTokens(glassTreatment, overBackdrop)
    Row(
        modifier = modifier
            .padding(horizontal = 21.dp)
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .height(62.dp)
            .border(1.dp, tokens.dockBorder, RoundedCornerShape(36.dp))
            .clip(RoundedCornerShape(36.dp))
            .background(tokens.dockFill)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockTabItem(
            icon = Icons.Default.List,
            label = "TODO",
            active = selectedTab == AppTab.Todo,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Todo) }
        DockTabItem(
            icon = Icons.Default.DateRange,
            label = "RECORDS",
            active = selectedTab == AppTab.Calendar,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Calendar) }
        DockTabItem(
            icon = if (isRunning) Icons.Default.PlayArrow else Icons.Default.Home,
            label = "HOME",
            active = selectedTab == AppTab.Timer,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Timer) }
        DockTabItem(
            icon = Icons.Default.Person,
            label = "Profile",
            active = selectedTab == AppTab.Profile,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Profile) }
    }
}
```

Also remove the `Notifications` import from KuroComponents.kt since it is no longer used:
```kotlin
// Remove this line:
import androidx.compose.material.icons.filled.Notifications
```

- [ ] **Step 2: Remove `isSoundPlaying` from the `DashboardDock` call in `KuroPomodoroApp.kt`**

Find the `DashboardDock(` call (around line 505). Change:

```kotlin
DashboardDock(
    isRunning = isRunning,
    isSoundPlaying = isSoundPlaying,
    onNavigate = { selectedTab = it },
    selectedTab = selectedTab,
    glassTreatment = glassTreatment,
    overBackdrop = true,
    modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .onSizeChanged { dockHeightPx = it.height }
)
```

to:

```kotlin
DashboardDock(
    isRunning = isRunning,
    onNavigate = { selectedTab = it },
    selectedTab = selectedTab,
    glassTreatment = glassTreatment,
    overBackdrop = true,
    modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .onSizeChanged { dockHeightPx = it.height }
)
```

- [ ] **Step 3: Verify the project still compiles (the Sounds tab branch will cause an error — that's expected)**

```bash
./gradlew :app:assembleDebug 2>&1 | head -40
```

Expected: compile error referencing `AppTab.Sounds` in the `when (selectedTab)` block. This is the signal to move to Task 3.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/ui/components/KuroComponents.kt \
        app/src/main/java/com/endiar/purrodoro/KuroPomodoroApp.kt
git commit -m "feat: remove DashboardDock sounds tab and isSoundPlaying param"
```

---

## Task 3: Remove `AppTab.Sounds` tab branch + fix `AppTabSaver`

**Files:**
- Modify: `app/src/main/java/com/endiar/purrodoro/KuroPomodoroApp.kt:414-434` (Sounds branch)
- Modify: `app/src/main/java/com/endiar/purrodoro/KuroPomodoroApp.kt:1115-1118` (AppTabSaver)

- [ ] **Step 1: Remove the `AppTab.Sounds` branch from the `when (selectedTab)` block**

In `KuroPomodoroApp.kt`, delete the entire `AppTab.Sounds -> SoundsScreen(...)` case. It looks like this:

```kotlin
AppTab.Sounds -> SoundsScreen(
    sounds = sounds,
    selectedSoundId = selectedSoundId,
    playingSoundId = playingSoundId,
    onSoundSelected = { sound ->
        if (sound.locked && !isPremium) {
            premiumSheetMessage =
                "${sound.name} is part of the simulated premium sound pack."
        } else {
            persistSelectedSoundId(sound.id)
            playingSoundId = sound.id
            premiumSheetMessage = null
        }
    },
    onToggleSound = { sound ->
        playingSoundId = if (playingSoundId == sound.id) null else sound.id
    },
    glassTreatment = glassTreatment,
    contentPadding = screenContentPadding,
    overBackdrop = overBackdrop,
)
```

Delete this block entirely (the `when` now has 4 branches: Timer, Todo, Calendar, Profile).

- [ ] **Step 2: Fix `AppTabSaver` to handle stale "Sounds" persisted state gracefully**

`AppTabSaver` currently uses `AppTab.valueOf(it)` which would throw `IllegalArgumentException` if "Sounds" was saved before this update. Change it to a safe lookup with a Timer fallback.

Find (around line 1115):

```kotlin
private val AppTabSaver = Saver<AppTab, String>(
    save = { it.name },
    restore = { AppTab.valueOf(it) }
)
```

Change to:

```kotlin
private val AppTabSaver = Saver<AppTab, String>(
    save = { it.name },
    restore = { name -> AppTab.entries.firstOrNull { it.name == name } ?: AppTab.Timer }
)
```

- [ ] **Step 3: Verify the project now compiles cleanly**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Run all unit tests to verify no regressions**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: all tests pass

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/KuroPomodoroApp.kt
git commit -m "feat: remove sounds tab branch and harden AppTabSaver fallback"
```

---

## Task 4: Add sounds bottom sheet state and `ModalBottomSheet`

**Files:**
- Modify: `app/src/main/java/com/endiar/purrodoro/KuroPomodoroApp.kt`

- [ ] **Step 1: Add new imports to `KuroPomodoroApp.kt`**

After the existing import block (after line ~107), add these imports:

```kotlin
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
```

- [ ] **Step 2: Add `showSoundsSheet` state variable**

In `KuroPomodoroApp`, after the existing `premiumSheetMessage` state declaration (around line 159):

```kotlin
var premiumSheetMessage by rememberSaveable { mutableStateOf<String?>(null) }
```

Add directly below:

```kotlin
var showSoundsSheet by rememberSaveable { mutableStateOf(false) }
```

- [ ] **Step 3: Add the `ModalBottomSheet` inside `surfaceContent`**

Inside the `surfaceContent` Box (inside the outer `Box(modifier = Modifier.fillMaxSize())`), after the cycle completion `AlertDialog` block (which ends around line 555), add the sounds sheet. The cycle completion dialog ends with:

```kotlin
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            ...
        )
    }
```

After that closing brace, add:

```kotlin
        if (showSoundsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSoundsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                containerColor = tokens.cardFill,
                contentColor = tokens.textPrimary,
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
            ) {
                premiumSheetMessage?.let { message ->
                    PremiumMessage(
                        message = message,
                        onDismiss = { premiumSheetMessage = null },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                SoundsScreen(
                    sounds = sounds,
                    selectedSoundId = selectedSoundId,
                    playingSoundId = playingSoundId,
                    onSoundSelected = { sound ->
                        if (sound.locked && !isPremium) {
                            premiumSheetMessage =
                                "${sound.name} is part of the simulated premium sound pack."
                        } else {
                            persistSelectedSoundId(sound.id)
                            playingSoundId = sound.id
                            premiumSheetMessage = null
                        }
                    },
                    onToggleSound = { sound ->
                        playingSoundId = if (playingSoundId == sound.id) null else sound.id
                    },
                    glassTreatment = GlassTreatment.Clear,
                    overBackdrop = false,
                )
            }
        }
```

Note: `tokens` is already available inside `surfaceContent` (declared at line ~325 as `val tokens = ...`). Wait — actually `tokens` is used inside `surfaceContent` via the dock block. Confirm `tokens` is in scope; if not, use `glassTokens(GlassTreatment.Clear, false)` directly for `containerColor` and `contentColor`. In `surfaceContent`, look at line 325: `val tokens = glassTokens(glassTreatment, overBackdrop)` — this is inside a nested `Box` scope, and `glassTreatment` / `overBackdrop` are outer-scope vals from `KuroPomodoroApp`. So use `glassTokens(GlassTreatment.Clear, false)` directly inside the sheet:

```kotlin
        if (showSoundsSheet) {
            val sheetTokens = glassTokens(GlassTreatment.Clear, false)
            ModalBottomSheet(
                onDismissRequest = { showSoundsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                containerColor = sheetTokens.cardFill,
                contentColor = sheetTokens.textPrimary,
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
            ) {
                premiumSheetMessage?.let { message ->
                    PremiumMessage(
                        message = message,
                        onDismiss = { premiumSheetMessage = null },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                SoundsScreen(
                    sounds = sounds,
                    selectedSoundId = selectedSoundId,
                    playingSoundId = playingSoundId,
                    onSoundSelected = { sound ->
                        if (sound.locked && !isPremium) {
                            premiumSheetMessage =
                                "${sound.name} is part of the simulated premium sound pack."
                        } else {
                            persistSelectedSoundId(sound.id)
                            playingSoundId = sound.id
                            premiumSheetMessage = null
                        }
                    },
                    onToggleSound = { sound ->
                        playingSoundId = if (playingSoundId == sound.id) null else sound.id
                    },
                    glassTreatment = GlassTreatment.Clear,
                    overBackdrop = false,
                )
            }
        }
```

- [ ] **Step 4: Verify build is clean**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/KuroPomodoroApp.kt
git commit -m "feat: add sounds ModalBottomSheet with existing SoundsScreen content"
```

---

## Task 5: Add `MusicButton` composable and wire into `FocusDashboard`

**Files:**
- Modify: `app/src/main/java/com/endiar/purrodoro/KuroPomodoroApp.kt`

This task adds the music icon entry point on the home screen. It has two parts: (a) define the `MusicButton` composable, and (b) update `FocusDashboard`'s signature and body to use it.

- [ ] **Step 1: Add `MusicButton` private composable**

At the bottom of `KuroPomodoroApp.kt`, after the existing `PremiumMessage` composable (around line 1113) and before `AppTabSaver`, add:

```kotlin
@Composable
private fun MusicButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "musicRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .size(44.dp)
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = if (isPlaying) 0.50f else 0.20f),
                shape = RoundedCornerShape(22.dp)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(if (isPlaying) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .graphicsLayer { if (isPlaying) rotationZ = rotation },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\u266B",
            color = Color.White.copy(alpha = if (isPlaying) 0.90f else 0.40f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
```

`\u266B` is the ♫ (beamed musical notes) character. The whole circle rotates when `isPlaying` is true, creating the vinyl-disc spinning effect.

- [ ] **Step 2: Add `isSoundPlaying` and `onOpenSoundsSheet` params to `FocusDashboard`**

Find the `FocusDashboard` function signature (around line 571):

```kotlin
@Composable
private fun FocusDashboard(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    isStarted: Boolean,
    focusActive: Boolean,
    focusTransition: Transition<Boolean>,
    activeTask: FocusTask?,
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
    timerSettings: TimerSettings,
    onSkip: () -> Unit,
    onStartPause: () -> Unit,
    onStop: () -> Unit,
    onExitFocusMode: () -> Unit,
    onEnterFocusMode: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
```

Change to:

```kotlin
@Composable
private fun FocusDashboard(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    isStarted: Boolean,
    focusActive: Boolean,
    focusTransition: Transition<Boolean>,
    activeTask: FocusTask?,
    cycleItems: List<TimerSessionItem>,
    cycleIndex: Int,
    currentTimerState: TimerState,
    timerSettings: TimerSettings,
    isSoundPlaying: Boolean,
    onSkip: () -> Unit,
    onStartPause: () -> Unit,
    onStop: () -> Unit,
    onExitFocusMode: () -> Unit,
    onOpenSoundsSheet: () -> Unit,
    onEnterFocusMode: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
```

- [ ] **Step 3: Place `MusicButton` in the `FocusDashboard` body**

In the `FocusDashboard` Column, find the closing `}` of the controls row `focusTransition.AnimatedVisibility` block. This block starts with:

```kotlin
        focusTransition.AnimatedVisibility(
            visible = { active -> !active },
            enter = if (initialized) fadeIn(
```

and ends (after all the pause/play/stop/Focus Now logic) with:

```kotlin
            }
        }
```

After that closing brace (which ends the controls AnimatedVisibility), and **before** the `if (showStopConfirmDialog)` block, insert:

```kotlin
        Spacer(Modifier.height(16.dp))
        MusicButton(
            isPlaying = isSoundPlaying,
            onClick = onOpenSoundsSheet
        )
```

The resulting order in the Column becomes:
1. controls AnimatedVisibility (pause/play/stop/skip or Focus Now)
2. `Spacer(16.dp)` + `MusicButton` ← new
3. `if (showStopConfirmDialog)` AlertDialog (no layout impact)
4. `Spacer(Modifier.weight(1f))`
5. "enter focus mode" chip / "tap anywhere" text

- [ ] **Step 4: Pass the new params at the `FocusDashboard` call site**

Find the `AppTab.Timer -> FocusDashboard(` call (around line 330). Add `isSoundPlaying` and `onOpenSoundsSheet`:

```kotlin
AppTab.Timer -> FocusDashboard(
    timerMode = timerMode,
    remainingSeconds = remainingSeconds,
    isRunning = isRunning,
    isStarted = isStarted,
    focusActive = focusActive,
    focusTransition = focusTransition,
    activeTask = activeTask,
    cycleItems = cycleItems,
    cycleIndex = cycleIndex,
    currentTimerState = currentTimerState,
    timerSettings = settings,
    isSoundPlaying = isSoundPlaying,
    onSkip = { timerConnection.controller.skip() },
    onStartPause = {
        when {
            !isStarted -> {
                requestNotificationPermission()
                isFocusMode = true
                timerConnection.controller.start(settings, activeTask)
            }
            isRunning -> {
                isFocusMode = false
                timerConnection.controller.pause()
            }
            else -> {
                timerConnection.controller.resume()
            }
        }
    },
    onStop = {
        timerConnection.controller.stop()
        isFocusMode = false
        activeTask = null
    },
    onExitFocusMode = { isFocusMode = false },
    onOpenSoundsSheet = { showSoundsSheet = true },
    onEnterFocusMode = { isFocusMode = true },
    contentPadding = screenContentPadding
)
```

- [ ] **Step 5: Verify build is clean**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Run all unit tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: all tests pass

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/KuroPomodoroApp.kt
git commit -m "feat: add MusicButton to Focus screen opening sounds bottom sheet"
```

---

## Task 6: Remove unused `SoundsScreen` import (cleanup)

**Files:**
- Modify: `app/src/main/java/com/endiar/purrodoro/KuroPomodoroApp.kt:98`

`SoundsScreen` is still used (inside the bottom sheet), so this import is still needed. However, `isSoundPlaying` was a local val derived from `playingSoundId`. Verify it is still used in `FocusDashboard` call (`isSoundPlaying = isSoundPlaying`) — it is. No import cleanup needed.

Check for any other dead references introduced by the removal. Run:

```bash
./gradlew :app:assembleDebug 2>&1 | grep -i "warning\|unused"
```

Fix any unused imports that appear as warnings. Commit if any changes were made:

```bash
git add app/src/main/java/com/endiar/purrodoro/KuroPomodoroApp.kt \
        app/src/main/java/com/endiar/purrodoro/ui/components/KuroComponents.kt
git commit -m "chore: remove unused imports after sounds tab removal"
```

---

## Self-Review Checklist (already applied)

- [x] **Spec coverage:** All 3 spec sections covered: dock removal (Tasks 1–2), bottom sheet (Task 4), music icon + animation (Task 5).
- [x] **AppTabSaver crash fix** included in Task 3.
- [x] **PremiumMessage inside sheet** — rendered at the top of sheet content in Task 4, Step 3.
- [x] **isSoundPlaying removed from DashboardDock** (Task 2), added to FocusDashboard (Task 5).
- [x] **No placeholders** — all steps contain actual code.
- [x] **Type consistency** — `isSoundPlaying: Boolean` defined in Task 5 Step 2 matches usage in Step 3 and Step 4.
