# Bottom Nav Revamp Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Revamp `DashboardDock` so all five items have equal width, the Home button is navigation-only with a timer-state icon, the Sounds item shows a play-state icon, and both animated items share a consistent pulse/breathe animation.

**Architecture:** All changes are in-place edits to existing files — no new files created. `KuroComponents.kt` gets a private `PulseWrapper` composable and refactored `DockHomeButton` / `DockTabItem`. `KuroPomodoroApp.kt` removes `onStartPause` from the `DashboardDock` call-site and passes `isSoundPlaying`. `SoundsScreen.kt` gains `playingSoundId` and `onToggleSound` params (sound toggling stays in the screen, dock is navigation-only).

**Tech Stack:** Kotlin, Jetpack Compose, `androidx.compose.animation.core` (infiniteTransition, animateFloat, FastOutSlowInEasing)

---

## Files Modified

| File | What changes |
|------|-------------|
| `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt` | Add `PulseWrapper`; refactor `DockHomeButton` to nav-only + icon; add `isSoundPlaying` param to `DashboardDock` + `DockTabItem`; `weight(1f)` on all items; remove `onStartPause` param |
| `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt` | Remove `onStartPause` from `DashboardDock` call; add `isSoundPlaying` arg; add `playingSoundId` state; wire `onToggleSound`; pass `playingSoundId`+`onToggleSound` to `SoundsScreen` |
| `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/SoundsScreen.kt` | Add `playingSoundId: Int?` and `onToggleSound: (SoundOption) -> Unit` params; use `playingSoundId` for play-state; tint playing row background coral |

---

## Task 1: Add `PulseWrapper` to `KuroComponents.kt`

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt`

- [ ] **Step 1: Add animation imports**

At the top of `KuroComponents.kt`, after the existing imports block, ensure these are present (add only the ones missing):

```kotlin
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
```

- [ ] **Step 2: Add `PulseWrapper` private composable**

Insert this block immediately before the `DashboardDock` composable (around line 267):

```kotlin
@Composable
private fun PulseWrapper(active: Boolean, content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (active) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    Box(
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
```

- [ ] **Step 3: Build to confirm no compile errors**

```bash
cd kuro-pomodoro-android && ./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt
git commit -m "feat: add PulseWrapper composable for dock animation"
```

---

## Task 2: Refactor `DashboardDock` — equal width + nav-only Home + Sounds play state

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt`

- [ ] **Step 1: Update `DashboardDock` signature and body**

Replace the entire `DashboardDock` composable (lines 267–312) with:

```kotlin
@Composable
fun DashboardDock(
    isRunning: Boolean,
    isSoundPlaying: Boolean,
    onNavigate: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: AppTab? = null,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
) {
    val tokens = glassTokens(glassTreatment)
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
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockTabItem(
            icon = "\u2611",
            label = "TODO",
            active = selectedTab == AppTab.Todo,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Todo) }
        DockTabItem(
            icon = "\u229e",
            label = "RECORDS",
            active = selectedTab == AppTab.Calendar,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Calendar) }
        DockHomeButton(
            isRunning = isRunning,
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(AppTab.Timer) }
        )
        DockTabItem(
            icon = if (isSoundPlaying) "\u266b" else "\u266a",
            label = "SOUNDS",
            active = selectedTab == AppTab.Sounds,
            isSoundPlaying = isSoundPlaying,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Sounds) }
        DockTabItem(
            icon = "\u25cb",
            label = "ME",
            active = selectedTab == AppTab.Profile,
            modifier = Modifier.weight(1f)
        ) { onNavigate(AppTab.Profile) }
    }
}
```

- [ ] **Step 2: Replace `DockTabItem` to accept `modifier` and `isSoundPlaying`**

Replace the entire `DockTabItem` composable (lines 314–330) with:

```kotlin
@Composable
private fun DockTabItem(
    icon: String,
    label: String,
    active: Boolean = false,
    isSoundPlaying: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val iconColor = if (active) DockCoral else Color.White.copy(alpha = 0.7f)
    val bgColor = if (active) DockCoral.copy(alpha = 0.2f) else Color.Transparent
    val shouldPulse = active && isSoundPlaying
    PulseWrapper(active = shouldPulse) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(24.dp))
                .background(bgColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(icon, color = iconColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(label, color = iconColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        }
    }
}
```

- [ ] **Step 3: Replace `DockHomeButton` to be nav-only with timer-state icon + pulse**

Replace the entire `DockHomeButton` composable (lines 332–359) with:

```kotlin
@Composable
private fun DockHomeButton(isRunning: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val icon = if (isRunning) "\u25b6" else "\u2302"
    val bgColor = DockCoral
    val contentColor = Color.White
    PulseWrapper(active = isRunning) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(24.dp))
                .background(bgColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = icon,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "HOME",
                color = contentColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
```

- [ ] **Step 4: Build**

```bash
cd kuro-pomodoro-android && ./gradlew assembleDebug 2>&1 | tail -20
```

Expected: compile errors about `onStartPause` at the call site in `KuroPomodoroApp.kt` — that is expected and will be fixed in Task 3.

- [ ] **Step 5: Commit (components only)**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt
git commit -m "feat: refactor DashboardDock — equal width, nav-only Home, Sounds play icon"
```

---

## Task 3: Update `KuroPomodoroApp.kt` call site

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`

- [ ] **Step 1: Replace the `DashboardDock` call**

Find this block (lines 304–321):

```kotlin
            DashboardDock(
                isRunning = isRunning,
                onNavigate = { selectedTab = it },
                onStartPause = {
                    if (!isRunning) {
                        activeTotalSeconds = timerStartTotalSeconds(
                            settings = settings,
                            mode = timerMode,
                            remainingSeconds = remainingSeconds,
                            activeTotalSeconds = activeTotalSeconds
                        )
                    }
                    isRunning = !isRunning
                },
                selectedTab = if (selectedTab == AppTab.Timer) null else selectedTab,
                glassTreatment = glassTreatment,
                modifier = Modifier.navigationBarsPadding()
            )
```

Replace with:

```kotlin
            DashboardDock(
                isRunning = isRunning,
                isSoundPlaying = isSoundPlaying,
                onNavigate = { selectedTab = it },
                selectedTab = if (selectedTab == AppTab.Timer) null else selectedTab,
                glassTreatment = glassTreatment,
                modifier = Modifier.navigationBarsPadding()
            )
```

- [ ] **Step 2: Build**

```bash
cd kuro-pomodoro-android && ./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt
git commit -m "feat: wire isSoundPlaying to DashboardDock, remove onStartPause from dock"
```

---

## Task 4: Update `SoundsScreen.kt` — `playingSoundId` param + coral tint on playing row

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/SoundsScreen.kt`

Context: The screen currently uses `isPlaying: Boolean` (true = something is playing) + `selectedSoundId: Int` to compute pill state. We replace `isPlaying` with `playingSoundId: Int?` (null = nothing playing) so each row knows whether *it* is the playing one. We also add `onToggleSound` so the playing row can be toggled off — `onSoundSelected` remains for selecting a new sound.

- [ ] **Step 1: Add `androidx.compose.foundation.background` import (for coral tint)**

The file already imports `clickable` from `foundation`. Ensure this import is present:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
```

Both are already present — no change needed, just verify.

- [ ] **Step 2: Replace `SoundsScreen` signature and update pill/tint logic**

Replace the entire function signature and the `sounds.forEach` block. The full new file content:

```kotlin
package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.SoundOption
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.ui.components.GlassCard
import com.endiar.kuropomodoro.ui.components.MockBadge
import com.endiar.kuropomodoro.ui.components.StatusPill
import com.endiar.kuropomodoro.ui.components.StatusPillKind

private val PlayingTint = Color(0xFFFF8B6F).copy(alpha = 0.15f)

@Composable
fun SoundsScreen(
    sounds: List<SoundOption>,
    selectedSoundId: Int,
    playingSoundId: Int?,
    onSoundSelected: (SoundOption) -> Unit,
    onToggleSound: (SoundOption) -> Unit,
    modifier: Modifier = Modifier,
    glassTreatment: GlassTreatment = GlassTreatment.Clear
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Sounds",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        sounds.forEach { sound ->
            val isSelected = sound.id == selectedSoundId
            val isPlaying = sound.id == playingSoundId
            val pillKind = when {
                isPlaying  -> StatusPillKind.Playing
                isSelected -> StatusPillKind.Selected
                sound.locked -> StatusPillKind.Locked
                else         -> StatusPillKind.Unlocked
            }
            val pillLabel = when {
                isPlaying  -> "Playing"
                isSelected -> "Selected"
                sound.locked -> "Premium"
                else         -> "Tap to play"
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isPlaying) PlayingTint else Color.Transparent)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = when {
                            sound.locked -> "Unlock ${sound.name}"
                            isPlaying    -> "Stop ${sound.name}"
                            else         -> "Play ${sound.name}"
                        }
                    ) {
                        if (isPlaying) onToggleSound(sound) else onSoundSelected(sound)
                    },
                active = isSelected,
                glassTreatment = glassTreatment
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = sound.symbol.take(2),
                            fontSize = 28.sp
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sound.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (sound.locked) MockBadge()
                            }
                            Text(
                                text = sound.description,
                                color = Color.White.copy(alpha = 0.50f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    StatusPill(label = pillLabel, kind = pillKind, glassTreatment = glassTreatment)
                }
            }
        }
    }
}
```

- [ ] **Step 3: Build — expect call-site errors in KuroPomodoroApp.kt**

```bash
cd kuro-pomodoro-android && ./gradlew assembleDebug 2>&1 | tail -20
```

Expected: errors at the `SoundsScreen(...)` call about missing `playingSoundId`/`onToggleSound` and unknown `isPlaying`. Fixed in Task 5.

- [ ] **Step 4: Commit (screen file only)**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/SoundsScreen.kt
git commit -m "feat: SoundsScreen uses playingSoundId, coral tint on playing row"
```

---

## Task 5: Update `KuroPomodoroApp.kt` — wire `playingSoundId` and `onToggleSound`

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`

- [ ] **Step 1: Replace `isSoundPlaying: Boolean` state with `playingSoundId: Int?`**

Find:

```kotlin
    var isSoundPlaying by rememberSaveable { mutableStateOf(false) }
```

Replace with:

```kotlin
    var playingSoundId by rememberSaveable { mutableStateOf<Int?>(null) }
    val isSoundPlaying = playingSoundId != null
```

- [ ] **Step 2: Update `SoundsScreen` call in the `when` block**

Find:

```kotlin
                    AppTab.Sounds -> SoundsScreen(
                        sounds = sounds,
                        selectedSoundId = selectedSoundId,
                        isPlaying = isSoundPlaying,
                        onSoundSelected = { sound ->
                            if (sound.locked) {
                                premiumSheetMessage = "${sound.name} is part of the simulated premium sound pack."
                            } else {
                                persistSelectedSoundId(sound.id)
                                isSoundPlaying = true
                                premiumSheetMessage = null
                            }
                        },
                        glassTreatment = glassTreatment
                    )
```

Replace with:

```kotlin
                    AppTab.Sounds -> SoundsScreen(
                        sounds = sounds,
                        selectedSoundId = selectedSoundId,
                        playingSoundId = playingSoundId,
                        onSoundSelected = { sound ->
                            if (sound.locked) {
                                premiumSheetMessage = "${sound.name} is part of the simulated premium sound pack."
                            } else {
                                persistSelectedSoundId(sound.id)
                                playingSoundId = sound.id
                                premiumSheetMessage = null
                            }
                        },
                        onToggleSound = { sound ->
                            playingSoundId = if (playingSoundId == sound.id) null else sound.id
                        },
                        glassTreatment = glassTreatment
                    )
```

- [ ] **Step 3: Build**

```bash
cd kuro-pomodoro-android && ./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Run unit tests**

```bash
cd kuro-pomodoro-android && ./gradlew test 2>&1 | tail -20
```

Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt
git commit -m "feat: wire playingSoundId state, connect SoundsScreen toggle"
```

---

## Self-Review Checklist

- [x] Equal-width dock items: `weight(1f)` on all 5 items — covered in Task 2
- [x] Home as nav-only, no `onStartPause` in dock: Tasks 2 + 3
- [x] Home icon: ⌂ idle (`\u2302`), ▶ running (`\u25b6`): Task 2 `DockHomeButton`
- [x] Sounds icon: ♪ idle (`\u266a`), ♬ playing (`\u266b`): Task 2 `DashboardDock`
- [x] Pulse animation on Home (timer running): Task 1 + 2 `DockHomeButton`
- [x] Pulse animation on Sounds (sound playing + active): Task 1 + 2 `DockTabItem` (`shouldPulse = active && isSoundPlaying`)
- [x] Coral tint on playing sound row: Task 4 `SoundsScreen`
- [x] `playingSoundId: Int?` in root state: Task 5
- [x] `onToggleSound` wired: Tasks 4 + 5
- [x] `isSoundPlaying = playingSoundId != null` derived: Task 5
- [x] All builds verified task by task
- [x] Type consistency: `playingSoundId: Int?` used consistently in Tasks 4 and 5; `isSoundPlaying: Boolean` passed to `DashboardDock` from Task 3 onward
