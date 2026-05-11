# Secondary Tabs — Glass over Cat Background Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the old light-card tab screens and `BottomTabBar` with a unified layout where every tab (Home, Todo, Records, Sounds, Me) shares the time-of-day cat photo background, soft scrims, and the existing glass `DashboardDock`.

**Architecture:** A shared `CatBackdrop` composable wraps the entire app UI — it always paints the time-of-day photo plus top/bottom scrims. The `DashboardDock` (already used on Home) is lifted into the root `KuroPomodoroApp` and shown on every tab, with a new `selectedTab` parameter to highlight the active item. Each secondary tab screen is restyled to use `GlassCard` / `GlassPill` / `StatusPill` instead of `KuroCard` and Material surface colours.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, no new dependencies.

---

## File map

| File | Change |
|---|---|
| `ui/components/KuroComponents.kt` | Add `CatBackdrop`, `GlassCard`, `GlassPill`, `StatusPill`, `MockBadge`; update `DashboardDock` signature |
| `KuroPomodoroApp.kt` | Wrap in `CatBackdrop`, share `DashboardDock` across all tabs, remove `BottomTabBar` |
| `ui/TodoScreen.kt` | Restyle: glass cards, collapsible add-task row, coral + button in header |
| `ui/CalendarScreen.kt` | Add stats row (Today / Week / Streak); restyle calendar cells with glass chips |
| `ui/SoundsScreen.kt` | Restyle: icon tiles, `StatusPill`, glass cards |
| `ui/ProfileScreen.kt` | Restyle: `Me` screen, `GlassCard` throughout, 2×2 mock feature grid |

No files are created. `BottomTabBar`, `DecorativeBackground`, `CatCompanion`, `TimerRing`, `ModeChip`, and `KuroPrimaryButton` are left in `KuroComponents.kt` but no longer used by the four restyled screens (they remain to avoid breaking any future references).

---

## Task 1 — Update `DashboardDock` to highlight the active tab

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt`

`DashboardDock` currently ignores which tab is active. Add a `selectedTab: AppTab?` parameter (null = Home, shows no item highlighted). Each of the four nav items checks whether it matches `selectedTab` and uses coral text + a rounded coral-tint background when it does.

- [ ] **Open `KuroComponents.kt` and find `DashboardDock`** (currently at the bottom of the file).

- [ ] **Update `DashboardDock` signature and body:**

```kotlin
@Composable
fun DashboardDock(
    isRunning: Boolean,
    selectedTab: AppTab? = null,
    onNavigate: (AppTab) -> Unit,
    onStartPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 21.dp)
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .height(62.dp)
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(36.dp))
            .clip(RoundedCornerShape(36.dp))
            .background(Color(0xFF15101F).copy(alpha = 0.65f))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockTabItem(
            icon = "\u2611", label = "TODO",
            active = selectedTab == AppTab.Todo
        ) { onNavigate(AppTab.Todo) }
        DockTabItem(
            icon = "\u229e", label = "RECORDS",
            active = selectedTab == AppTab.Calendar
        ) { onNavigate(AppTab.Calendar) }
        DockHomeButton(isRunning = isRunning, onClick = onStartPause)
        DockTabItem(
            icon = "\u266a", label = "SOUNDS",
            active = selectedTab == AppTab.Sounds
        ) { onNavigate(AppTab.Sounds) }
        DockTabItem(
            icon = "\u25cb", label = "ME",
            active = selectedTab == AppTab.Profile
        ) { onNavigate(AppTab.Profile) }
    }
}
```

- [ ] **Update `DockTabItem` to accept and apply an `active` flag:**

```kotlin
@Composable
private fun DockTabItem(icon: String, label: String, active: Boolean = false, onClick: () -> Unit) {
    val iconColor = if (active) Color(0xFFFF8B6F) else Color.White.copy(alpha = 0.7f)
    val bgColor = if (active) Color(0xFFFF8B6F).copy(alpha = 0.2f) else Color.Transparent
    Column(
        modifier = Modifier
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
```

- [ ] **Build the project** to confirm no compile errors:
  ```bash
  cd kuro-pomodoro-android && ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Commit:**
  ```bash
  git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt
  git commit -m "feat: add selectedTab param to DashboardDock for active tab highlight"
  ```

---

## Task 2 — Add `CatBackdrop` shared composable

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt`

`CatBackdrop` is a full-screen `Box` that:
1. Paints the time-of-day cat image (`backgroundImageForTime(LocalTime.now())`).
2. Overlays a top scrim (240dp, dark → transparent).
3. Overlays a bottom scrim (260dp, transparent → dark), anchored to the bottom edge.
4. Renders `content` on top.

- [ ] **Add the following imports** to `KuroComponents.kt` if not already present:

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import java.time.LocalTime
import com.endiar.kuropomodoro.model.backgroundImageForTime
```

- [ ] **Add `CatBackdrop` composable** (place after `DecorativeBackground`):

```kotlin
@Composable
fun CatBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val darkBase = Color(0xFF0A0816)
    Box(modifier = modifier.fillMaxSize().background(darkBase)) {
        Image(
            painter = painterResource(id = backgroundImageForTime(LocalTime.now())),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Top scrim — legibility for page title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(darkBase.copy(alpha = 0.80f), Color.Transparent)
                    )
                )
        )
        // Bottom scrim — legibility for dock
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, darkBase.copy(alpha = 0.80f))
                    )
                )
        )
        content()
    }
}
```

- [ ] **Build:**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Commit:**
  ```bash
  git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt
  git commit -m "feat: add CatBackdrop composable with time-of-day photo and scrims"
  ```

---

## Task 3 — Add `GlassCard`, `GlassPill`, `StatusPill`, `MockBadge`

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt`

These are the building-block primitives used by every restyled screen.

- [ ] **Add the following to `KuroComponents.kt`** (place after `CatBackdrop`):

```kotlin
// ── Glass primitives ─────────────────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    active: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val fill = if (active) Color(0xFFFF8B6F).copy(alpha = 0.10f) else Color(0xFF0F0A1A).copy(alpha = 0.40f)
    val border = if (active) Color(0xFFFF8B6F).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.10f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(fill)
            .border(1.dp, border, RoundedCornerShape(22.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun GlassPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val fill = if (selected) Color(0xFFFF8B6F) else Color.White.copy(alpha = 0.08f)
    val textColor = if (selected) Color.White else Color.White.copy(alpha = 0.55f)
    val borderColor = if (selected) Color.Transparent else Color.White.copy(alpha = 0.15f)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(fill)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

enum class StatusPillKind { Playing, Selected, Locked, Unlocked }

@Composable
fun StatusPill(label: String, kind: StatusPillKind) {
    val fill = when (kind) {
        StatusPillKind.Playing  -> Color(0xFFFF8B6F)
        StatusPillKind.Selected -> Color.White.copy(alpha = 0.10f)
        StatusPillKind.Locked   -> Color(0xFF7B4FBF).copy(alpha = 0.25f)
        StatusPillKind.Unlocked -> Color.White.copy(alpha = 0.10f)
    }
    val textColor = when (kind) {
        StatusPillKind.Playing  -> Color.White
        StatusPillKind.Selected -> Color.White.copy(alpha = 0.80f)
        StatusPillKind.Locked   -> Color(0xFFD8C8FF)
        StatusPillKind.Unlocked -> Color.White.copy(alpha = 0.55f)
    }
    val borderColor = when (kind) {
        StatusPillKind.Locked   -> Color(0xFFD8C8FF).copy(alpha = 0.40f)
        else                    -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = label, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MockBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF7B4FBF).copy(alpha = 0.25f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text = "Mock", color = Color(0xFFD8C8FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
```

- [ ] **Add missing imports** (only if not already present):
  ```kotlin
  import androidx.compose.foundation.BorderStroke
  import androidx.compose.ui.unit.sp
  ```
  `ColumnScope`, `RoundedCornerShape`, `Modifier`, `Color`, `Text`, `FontWeight`, `Box`, `Column`, `Row`, `clip`, `background`, `border`, `clickable`, `padding` are all already imported from previous composables in the file.

- [ ] **Build:**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Commit:**
  ```bash
  git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt
  git commit -m "feat: add GlassCard, GlassPill, StatusPill, MockBadge composables"
  ```

---

## Task 4 — Unify root layout in `KuroPomodoroApp`

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`

**What changes:**
- Replace the `if (selectedTab == AppTab.Timer) { FocusDashboard(…) } else { DecorativeBackground { … BottomTabBar … } }` split.
- Wrap everything in `CatBackdrop`.
- Always show `DashboardDock` at the bottom (passing `selectedTab` when not on Home, `null` on Home).
- `FocusDashboard` keeps its own content (the big timer, mode pill, play button) but no longer paints its own background or dock — those are now in the parent.
- Remove `BottomTabBar` callsite and the `premiumSheetMessage` overlay (keep the composable available, just move it into the content column above the dock).

- [ ] **Replace the entire rendering block starting at `if (selectedTab == AppTab.Timer)`** with:

```kotlin
CatBackdrop {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            when (selectedTab) {
                AppTab.Timer -> FocusDashboard(
                    timerMode = timerMode,
                    remainingSeconds = remainingSeconds,
                    isRunning = isRunning,
                    onModeSelected = { mode ->
                        timerMode = mode
                        isRunning = false
                        remainingSeconds = settings.secondsFor(mode)
                        activeTotalSeconds = settings.secondsFor(mode)
                    },
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
                    }
                )
                AppTab.Todo -> TodoScreen(
                    todoLists = todoLists,
                    selectedListId = selectedTodoListId,
                    onListSelected = { selectedTodoListId = it },
                    onAddTask = addTask,
                    onToggleTask = toggleTask,
                    onDeleteTask = deleteTask
                )
                AppTab.Calendar -> CalendarScreen(
                    stamps = stamps,
                    completedSessions = completedSessions
                )
                AppTab.Sounds -> SoundsScreen(
                    sounds = sounds,
                    selectedSoundId = selectedSoundId,
                    isPlaying = isSoundPlaying,
                    onSoundSelected = { sound ->
                        if (sound.locked) {
                            premiumSheetMessage = "${sound.name} is part of the simulated premium sound pack."
                        } else {
                            selectedSoundId = sound.id
                            isSoundPlaying = true
                            premiumSheetMessage = null
                        }
                    }
                )
                AppTab.Profile -> ProfileScreen(
                    settings = settings,
                    darkModePreview = darkModePreview,
                    onDarkModeChanged = { darkModePreview = it },
                    onDurationChanged = { mode, minutes ->
                        val updatedSettings = settings.update(mode, minutes)
                        settings = updatedSettings
                        if (mode == timerMode && !isRunning) {
                            remainingSeconds = updatedSettings.secondsFor(mode)
                            activeTotalSeconds = updatedSettings.secondsFor(mode)
                        }
                    },
                    onMockFeature = { premiumSheetMessage = it }
                )
            }
        }
        premiumSheetMessage?.let { message ->
            PremiumMessage(
                message = message,
                onDismiss = { premiumSheetMessage = null },
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }
        DashboardDock(
            isRunning = isRunning,
            selectedTab = if (selectedTab == AppTab.Timer) null else selectedTab,
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
            modifier = Modifier.navigationBarsPadding()
        )
    }
}
```

- [ ] **Update `FocusDashboard` signature** — remove `onNavigate` parameter since the dock is now in the parent and handles navigation:

```kotlin
@Composable
private fun FocusDashboard(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    onModeSelected: (TimerMode) -> Unit,
    onStartPause: () -> Unit,
) {
```

- [ ] **Inside `FocusDashboard`, remove the background image, scrims, and the `DashboardDock` block.** The function body becomes a `Column` containing only the timer content (mode menu + time + play button + spacer):

```kotlin
@Composable
private fun FocusDashboard(
    timerMode: TimerMode,
    remainingSeconds: Int,
    isRunning: Boolean,
    onModeSelected: (TimerMode) -> Unit,
    onStartPause: () -> Unit,
) {
    val buttonBg = if (isRunning) Color.White.copy(alpha = 0.92f) else Color(0xFFFF8B6F)
    val buttonContentColor = if (isRunning) Color(0xFF241E2B) else Color.White
    var showModeMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        // Timer
        Text(
            text = formatSeconds(remainingSeconds),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 72.sp,
            fontWeight = FontWeight.W200,
            letterSpacing = (-1).sp
        )
        Spacer(Modifier.height(24.dp))
        // Play/pause + mode pill row
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(buttonBg)
                    .clickable(onClick = onStartPause),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRunning) "\u23f8" else "\u25b6",
                    color = buttonContentColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(modifier = Modifier.wrapContentSize(Alignment.TopCenter)) {
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { showModeMenu = !showModeMenu }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timerMode.label,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "\u25be",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 13.sp
                        )
                    }
                }
                DropdownMenu(
                    expanded = showModeMenu,
                    onDismissRequest = { showModeMenu = false },
                    containerColor = Color(0xFF1E1628)
                ) {
                    TimerMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = mode.label,
                                    color = if (mode == timerMode) Color(0xFFFF8B6F) else Color.White.copy(alpha = 0.85f),
                                    fontWeight = if (mode == timerMode) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onModeSelected(mode)
                                showModeMenu = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = Color.White.copy(alpha = 0.85f)
                            )
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}
```

- [ ] **Remove unused imports** from `KuroPomodoroApp.kt`: `Image`, `ContentScale`, `painterResource`, `backgroundImageForTime` (they are now used in `KuroComponents.kt` only). Also remove the `BottomTabBar` import.

- [ ] **Build:**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Run the app on a device or emulator** and verify:
  - Home tab: cat photo visible, timer in the centre, dock at the bottom with no item highlighted.
  - Tapping Todo in the dock navigates to the (still old-styled) Todo screen; dock shows `TODO` highlighted in coral.

- [ ] **Commit:**
  ```bash
  git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt
  git commit -m "feat: unify root layout with CatBackdrop and shared DashboardDock"
  ```

---

## Task 5 — Restyle `TodoScreen`

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/TodoScreen.kt`

**What changes:** remove the `KuroCard`/`OutlinedTextField`/`KuroPrimaryButton` add-task block. Add a header row with title + coral `+` button. Replace `TodoListTab` with `GlassPill`. Replace `TaskRow` card with `GlassCard`. A collapsible inline input row appears when `+` is tapped.

- [ ] **Replace the entire file contents of `TodoScreen.kt`:**

```kotlin
package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.TodoListState
import com.endiar.kuropomodoro.ui.components.GlassCard
import com.endiar.kuropomodoro.ui.components.GlassPill
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodoScreen(
    todoLists: List<TodoListState>,
    selectedListId: Int,
    onListSelected: (Int) -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedList = todoLists.firstOrNull { it.id == selectedListId } ?: todoLists.firstOrNull()
    var newTaskTitle by rememberSaveable { mutableStateOf("") }
    var addingTask by rememberSaveable { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val today = LocalDate.now()
    val dateLabel = today.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))

    val submitTask = {
        val title = newTaskTitle.trim()
        if (title.isNotEmpty()) {
            onAddTask(title)
            newTaskTitle = ""
            addingTask = false
            keyboardController?.hide()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "To-Do",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateLabel,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF8B6F))
                    .clickable { addingTask = !addingTask },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // List switcher
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            todoLists.forEach { list ->
                GlassPill(
                    text = list.name,
                    selected = list.id == selectedList?.id,
                    onClick = { onListSelected(list.id) }
                )
            }
        }

        // Inline add-task input (visible when + is tapped)
        if (addingTask) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F0A1A).copy(alpha = 0.40f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = newTaskTitle,
                    onValueChange = { newTaskTitle = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Add a focus task…",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitTask() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFFF8B6F),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF8B6F))
                        .clickable { submitTask() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = "Add", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Section label
        Text(
            text = "UPCOMING",
            color = Color.White.copy(alpha = 0.30f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )

        // Task list
        if (selectedList?.tasks.isNullOrEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No tasks yet",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tap + to add your first focus task.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp
                )
            }
        } else {
            val isFirstActive = { task: FocusTask ->
                !task.completed && selectedList!!.tasks.firstOrNull { !it.completed } == task
            }
            selectedList!!.tasks.forEach { task ->
                TaskGlassRow(
                    task = task,
                    active = isFirstActive(task),
                    onToggleTask = onToggleTask,
                    onDeleteTask = onDeleteTask
                )
            }
        }
    }
}

@Composable
private fun TaskGlassRow(
    task: FocusTask,
    active: Boolean,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), active = active) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Checkbox,
                    onClickLabel = if (task.completed) "Unmark ${task.title}" else "Complete ${task.title}"
                ) { onToggleTask(task.id) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Circular checkbox
            val checkFill = if (task.completed) Color(0xFFFF8B6F) else Color.White.copy(alpha = 0.10f)
            val checkBorder = if (task.completed) Color.Transparent else Color.White.copy(alpha = 0.25f)
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(checkFill)
                    .border(1.dp, checkBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (task.completed) {
                    Text(text = "✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = task.title,
                    color = if (task.completed) Color.White.copy(alpha = 0.40f) else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = "${task.minutes} min",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp
                )
            }
            Text(
                text = "✕",
                color = Color.White.copy(alpha = 0.30f),
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Delete ${task.title}"
                    ) { onDeleteTask(task.id) }
                    .semantics { contentDescription = "Delete ${task.title}" }
            )
        }
    }
}
```

- [ ] **Build:**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Commit:**
  ```bash
  git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/TodoScreen.kt
  git commit -m "feat: restyle TodoScreen with glass cards and collapsible add-task"
  ```

---

## Task 6 — Restyle `CalendarScreen` (Records)

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/CalendarScreen.kt`

**What changes:** add a stats row (Today / Week / Streak); restyle the calendar card with `GlassCard`; restyle day cells as glass chips; use white-on-dark text.

- [ ] **Replace the entire file contents of `CalendarScreen.kt`:**

```kotlin
package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.ui.components.GlassCard
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun CalendarScreen(
    stamps: Map<LocalDate, Int>,
    completedSessions: Int,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val month = remember(today) { YearMonth.from(today) }
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val dates = remember(month, firstDayOfWeek) { monthCalendarCells(month, firstDayOfWeek) }
    val weekDays = remember(firstDayOfWeek) { localizedWeekDays(firstDayOfWeek) }
    val monthLabel = month.month.getDisplayName(TextStyle.FULL, locale)

    // Derive stats
    val todayCount = stamps[today] ?: 0
    val weekStart = today.with(WeekFields.of(locale).dayOfWeek(), 1)
    val weekCount = (0 until 7).sumOf { stamps[weekStart.plusDays(it.toLong())] ?: 0 }
    val streak = run {
        var d = today
        var count = 0
        while ((stamps[d] ?: 0) > 0) { count++; d = d.minusDays(1) }
        count
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "Records", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "$completedSessions focus sessions this month",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp
            )
        }

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMiniCard(label = "TODAY", value = todayCount.toString(), active = false, modifier = Modifier.weight(1f))
            StatMiniCard(label = "WEEK", value = weekCount.toString(), active = false, modifier = Modifier.weight(1f))
            StatMiniCard(label = "STREAK", value = "${streak}d", active = true, modifier = Modifier.weight(1f))
        }

        // Calendar card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            // Month nav row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "‹", color = Color.White.copy(alpha = 0.55f), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "$monthLabel ${month.year}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = "›", color = Color.White.copy(alpha = 0.55f), fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(12.dp))
            GlassCalendarGrid(
                dates = dates,
                stamps = stamps,
                today = today,
                locale = locale,
                weekDays = weekDays
            )
        }
    }
}

@Composable
private fun StatMiniCard(label: String, value: String, active: Boolean, modifier: Modifier = Modifier) {
    val fill = if (active) Color(0xFFFF8B6F).copy(alpha = 0.10f) else Color(0xFF0F0A1A).copy(alpha = 0.40f)
    val border = if (active) Color(0xFFFF8B6F).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.10f)
    val labelColor = if (active) Color(0xFFFF8B6F) else Color.White.copy(alpha = 0.40f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(fill)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = label, color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Text(text = value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GlassCalendarGrid(
    dates: List<LocalDate?>,
    stamps: Map<LocalDate, Int>,
    today: LocalDate,
    locale: Locale,
    weekDays: List<DayOfWeek>
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            weekDays.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
        dates.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    GlassDayCell(
                        date = date,
                        count = date?.let { stamps[it] } ?: 0,
                        isToday = date == today,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassDayCell(date: LocalDate?, count: Int, isToday: Boolean, modifier: Modifier = Modifier) {
    val hasStamp = count > 0
    val fill = when {
        hasStamp -> Color(0xFFFF8B6F).copy(alpha = 0.10f)
        isToday  -> Color.White.copy(alpha = 0.10f)
        else     -> Color.Transparent
    }
    val borderColor = when {
        hasStamp -> Color(0xFFFF8B6F).copy(alpha = 0.30f)
        isToday  -> Color.White.copy(alpha = 0.70f)
        else     -> Color.Transparent
    }
    val borderWidth = if (isToday) 1.5.dp else 1.dp
    val textColor = when {
        hasStamp -> Color.White
        isToday  -> Color.White
        date == null -> Color.Transparent
        else     -> Color.White.copy(alpha = 0.30f)
    }
    val label = when {
        date == null -> "Empty cell"
        hasStamp -> "${date.dayOfMonth}, $count sessions"
        else -> "${date.dayOfMonth}"
    }

    Box(
        modifier = modifier
            .aspectRatio(0.86f)
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Text(
                text = date.dayOfMonth.toString(),
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (hasStamp || isToday) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

private fun monthCalendarCells(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
    val firstDay = month.atDay(1)
    val leadingEmptyCells = Math.floorMod(firstDay.dayOfWeek.value - firstDayOfWeek.value, 7)
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val cells = List(leadingEmptyCells) { null } + days
    val trailingEmptyCells = (7 - cells.size % 7) % 7
    return cells + List(trailingEmptyCells) { null }
}

private fun localizedWeekDays(firstDayOfWeek: DayOfWeek): List<DayOfWeek> =
    (0 until 7).map { firstDayOfWeek.plus(it.toLong()) }
```

- [ ] **Build:**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Commit:**
  ```bash
  git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/CalendarScreen.kt
  git commit -m "feat: restyle CalendarScreen as Records with stats row and glass calendar"
  ```

---

## Task 7 — Restyle `SoundsScreen`

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/SoundsScreen.kt`

**What changes:** replace `KuroCard` rows with `GlassCard` (active style for currently-playing); replace icon `Surface` with a small coloured `Box`; replace status badge `Surface` with `StatusPill`.

The `SoundOption.symbol` field (e.g. `"Rain"`) becomes the display letter used in the icon tile until lucide icons are available.

- [ ] **Replace the entire file contents of `SoundsScreen.kt`:**

```kotlin
package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.SoundOption
import com.endiar.kuropomodoro.ui.components.GlassCard
import com.endiar.kuropomodoro.ui.components.StatusPill
import com.endiar.kuropomodoro.ui.components.StatusPillKind

@Composable
fun SoundsScreen(
    sounds: List<SoundOption>,
    selectedSoundId: Int,
    isPlaying: Boolean,
    onSoundSelected: (SoundOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "Sounds", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Pick a cozy loop for Kuro",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sounds.forEach { sound ->
                val playing = isPlaying && sound.id == selectedSoundId
                SoundGlassCard(
                    sound = sound,
                    playing = playing,
                    onClick = { onSoundSelected(sound) }
                )
            }
        }
    }
}

@Composable
private fun SoundGlassCard(sound: SoundOption, playing: Boolean, onClick: () -> Unit) {
    val selected = !playing  // for non-playing selected state — handled by StatusPillKind logic below
    val kind = when {
        playing      -> StatusPillKind.Playing
        sound.locked -> StatusPillKind.Locked
        else         -> StatusPillKind.Unlocked
    }
    val statusLabel = when (kind) {
        StatusPillKind.Playing  -> "Playing"
        StatusPillKind.Locked   -> "Premium"
        else                    -> "Unlocked"
    }
    val iconTileFill = if (sound.locked) Color(0xFF7B4FBF).copy(alpha = 0.20f) else Color.White.copy(alpha = 0.10f)
    val iconColor = if (sound.locked) Color(0xFFD8C8FF) else Color.White.copy(alpha = 0.80f)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = statusLabel,
                onClick = onClick
            )
            .semantics { contentDescription = "${sound.name}, $statusLabel. ${sound.description}" },
        active = playing
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon tile
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconTileFill)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sound.symbol.take(2),
                    color = iconColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = sound.name,
                    color = if (sound.locked) Color.White.copy(alpha = 0.80f) else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = sound.description,
                    color = if (sound.locked) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )
            }
            StatusPill(label = statusLabel, kind = kind)
        }
    }
}
```

- [ ] **Build:**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Commit:**
  ```bash
  git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/SoundsScreen.kt
  git commit -m "feat: restyle SoundsScreen with glass cards and StatusPill"
  ```

---

## Task 8 — Restyle `ProfileScreen` (Me)

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/ProfileScreen.kt`

**What changes:** replace all `KuroCard` and Material surface colours with `GlassCard`; restyle avatar, stepper buttons, toggle; collapse the four mock-feature cards into a 2×2 grid.

- [ ] **Replace the entire file contents of `ProfileScreen.kt`:**

```kotlin
package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.ui.components.GlassCard
import com.endiar.kuropomodoro.ui.components.MockBadge

@Composable
fun ProfileScreen(
    settings: TimerSettings,
    darkModePreview: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
    onDurationChanged: (TimerMode, Int) -> Unit,
    onMockFeature: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "Me", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Local · stays on this device",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp
            )
        }

        // Profile card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFF8B6F)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Ku", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "Pomodoro Keeper", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "No account, no cloud sync",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Timer lengths card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Timer lengths", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TimerLengthRow(
                    label = "Focus",
                    minutes = settings.focusMinutes,
                    onDecrease = { onDurationChanged(TimerMode.Focus, settings.focusMinutes - 1) },
                    onIncrease = { onDurationChanged(TimerMode.Focus, settings.focusMinutes + 1) }
                )
                TimerLengthRow(
                    label = "Short break",
                    minutes = settings.shortBreakMinutes,
                    onDecrease = { onDurationChanged(TimerMode.ShortBreak, settings.shortBreakMinutes - 1) },
                    onIncrease = { onDurationChanged(TimerMode.ShortBreak, settings.shortBreakMinutes + 1) }
                )
                TimerLengthRow(
                    label = "Long break",
                    minutes = settings.longBreakMinutes,
                    onDecrease = { onDurationChanged(TimerMode.LongBreak, settings.longBreakMinutes - 1) },
                    onIncrease = { onDurationChanged(TimerMode.LongBreak, settings.longBreakMinutes + 1) }
                )
            }
        }

        // Dark mode preview row
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "Dark mode preview", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Mock setting, no theme change yet",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = darkModePreview,
                    onCheckedChange = onDarkModeChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFFF8B6F),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.60f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                        uncheckedBorderColor = Color.White.copy(alpha = 0.20f)
                    ),
                    modifier = Modifier.semantics { contentDescription = "Dark mode preview" }
                )
            }
        }

        // Mock feature 2×2 grid — row 1
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MockFeatureSquare(
                icon = "👥",
                title = "Focus Together",
                subtitle = "Shared quiet rooms",
                onClick = { onMockFeature("Focus Together is a mock social focus room for this prototype.") },
                modifier = Modifier.weight(1f)
            )
            MockFeatureSquare(
                icon = "👑",
                title = "Premium Pass",
                subtitle = "Sounds, rooms, outfits",
                onClick = { onMockFeature("Premium Pass is simulated in this clone and does not start a purchase.") },
                active = true,
                modifier = Modifier.weight(1f)
            )
        }

        // Mock feature 2×2 grid — row 2
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MockFeatureSquare(
                icon = "🔔",
                title = "Reminders",
                subtitle = "Gentle nudges to focus",
                onClick = { onMockFeature("Reminders are mocked here; notifications are not scheduled yet.") },
                modifier = Modifier.weight(1f)
            )
            MockFeatureSquare(
                icon = "📅",
                title = "D-Day Tracker",
                subtitle = "Countdown to milestones",
                onClick = { onMockFeature("D-Day Tracker is a placeholder for future goal countdowns.") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TimerLengthRow(
    label: String,
    minutes: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(text = label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = "$minutes minutes", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
        }
        StepButton(
            label = "Decrease $label",
            text = "−",
            enabled = minutes > 1,
            active = false,
            onClick = onDecrease
        )
        StepButton(
            label = "Increase $label",
            text = "+",
            enabled = true,
            active = true,
            onClick = onIncrease
        )
    }
}

@Composable
private fun StepButton(label: String, text: String, enabled: Boolean, active: Boolean, onClick: () -> Unit) {
    val fill = when {
        !enabled -> Color.White.copy(alpha = 0.05f)
        active   -> Color(0xFFFF8B6F).copy(alpha = 0.20f)
        else     -> Color.White.copy(alpha = 0.10f)
    }
    val border = when {
        !enabled -> Color.Transparent
        active   -> Color(0xFFFF8B6F).copy(alpha = 0.40f)
        else     -> Color.White.copy(alpha = 0.15f)
    }
    val textColor = when {
        !enabled -> Color.White.copy(alpha = 0.20f)
        active   -> Color(0xFFFF8B6F)
        else     -> Color.White.copy(alpha = 0.80f)
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(fill)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .then(if (enabled) Modifier.clickable(
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick
            ) else Modifier)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MockFeatureSquare(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    active: Boolean = false,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .clickable(role = Role.Button, onClickLabel = "Preview $title", onClick = onClick),
        active = active
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 18.sp)
            MockBadge()
        }
        Spacer(Modifier.height(8.dp))
        Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(text = subtitle, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
    }
}
```

- [ ] **Build:**
  ```bash
  ./gradlew :app:compileDebugKotlin
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Commit:**
  ```bash
  git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/ProfileScreen.kt
  git commit -m "feat: restyle ProfileScreen as Me with glass cards and 2x2 mock grid"
  ```

---

## Task 9 — Smoke-test full app and clean up unused imports

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt` (remove unused imports)
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt` (verify no new lint warnings)

- [ ] **Run full debug build:**
  ```bash
  ./gradlew :app:assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL` with no errors.

- [ ] **Check for unused-import warnings** using Android Studio's "Optimize Imports" or:
  ```bash
  ./gradlew :app:lintDebug 2>&1 | grep -i "unused import\|UnusedImport"
  ```
  Fix any reported.

- [ ] **Manual smoke-test on emulator / device.** Verify each tab in order:
  1. **Home** — cat photo, big timer, play/pause button, mode pill. Dock has no item highlighted.
  2. **Todo** — cat photo shows through. Title "To-Do" + date. List switcher chips. UPCOMING label. Task cards. Tap `+` → inline input appears. Add a task → task appears. Toggle a task → checkbox fills coral. Dock highlights TODO.
  3. **Records** — cat photo. "Records" title. TODAY / WEEK / STREAK stats. Calendar card with month header, weekday row, day grid. Today's cell has a white ring. Dock highlights RECORDS.
  4. **Sounds** — cat photo. "Sounds" title. Five sound cards with icon tiles and status pills. Tap unlocked sound → it becomes Playing. Tap locked sound → premium sheet appears. Dock highlights SOUNDS.
  5. **Me** — cat photo. "Me" title. Profile card (Ku avatar). Timer lengths card with +/− buttons. Dark mode toggle. 2×2 mock cards. Tapping a mock card shows premium sheet. Dock highlights ME.
  6. **Back to Home via dock home button** — timer state preserved.

- [ ] **Run existing unit tests:**
  ```bash
  ./gradlew :app:testDebugUnitTest
  ```
  Expected: all tests pass (`timerDisplayTotalSeconds`, `timerStartTotalSeconds`, model tests).

- [ ] **Commit:**
  ```bash
  git add -A
  git commit -m "chore: smoke-test cleanup and unused import removal"
  ```

---

## Self-review notes

**Spec coverage check:**

| Spec requirement | Covered by task |
|---|---|
| Cat photo on every tab | Task 2 (`CatBackdrop`), Task 4 (root layout) |
| Top + bottom scrims | Task 2 |
| `DashboardDock` shared, active tab highlighted | Task 1 |
| `BottomTabBar` removed | Task 4 |
| `GlassCard`, `GlassPill`, `StatusPill`, `MockBadge` | Task 3 |
| Todo: header row with `+`, date | Task 5 |
| Todo: glass chips list switcher | Task 5 |
| Todo: UPCOMING label | Task 5 |
| Todo: glass task cards, active task coral | Task 5 |
| Todo: collapsible inline add-task input | Task 5 |
| Records: stats row (Today / Week / Streak) | Task 6 |
| Records: month nav, weekday headers, day chips | Task 6 |
| Records: today cell with white ring, stamped cells coral | Task 6 |
| Sounds: icon tiles, status pills, active card | Task 7 |
| Me: profile card, timer steppers, dark mode toggle | Task 8 |
| Me: 2×2 mock grid | Task 8 |
| `FocusDashboard` no longer paints own background or dock | Task 4 |
| All existing callbacks/state unchanged | All tasks — signatures preserved |
