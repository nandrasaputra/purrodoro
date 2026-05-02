# Pomocat-Inspired Compose Clone Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a broad local-first Jetpack Compose clone of Pomocat with a working timer, cute cat UI, tabs, todos, calendar stamps, sound selection, and simulated premium/profile/settings flows.

**Architecture:** Keep the app in the existing single Android module. `MainActivity` owns the root Compose state and renders a `KuroPomodoroApp` app shell with focused model, component, and screen files. State is local Compose runtime state only; no backend, persistence, billing, ads, audio playback, or navigation library is introduced.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Android Gradle Plugin 9.0.1, Kotlin 2.0.21, JUnit 4.

---

## File Structure

- Modify `app/src/main/java/com/endiar/kuropomodoro/MainActivity.kt`: replace generated greeting with `KuroPomodoroApp`.
- Create `app/src/main/java/com/endiar/kuropomodoro/model/KuroModels.kt`: enums and data classes for tabs, timer modes, tasks, todo lists, sound options, settings, and calendar stamps.
- Create `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`: root local state, timer ticking, app shell, and routing between tabs.
- Create `app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt`: reusable cards, buttons, chips, bottom nav, timer ring, cat illustration, and helper UI.
- Create `app/src/main/java/com/endiar/kuropomodoro/ui/TimerScreen.kt`: Pomodoro focus screen.
- Create `app/src/main/java/com/endiar/kuropomodoro/ui/TodoScreen.kt`: local todo lists.
- Create `app/src/main/java/com/endiar/kuropomodoro/ui/CalendarScreen.kt`: stamp calendar.
- Create `app/src/main/java/com/endiar/kuropomodoro/ui/SoundsScreen.kt`: local sound picker and premium lock simulation.
- Create `app/src/main/java/com/endiar/kuropomodoro/ui/ProfileScreen.kt`: profile/settings/premium simulation.
- Modify `app/src/main/java/com/endiar/kuropomodoro/ui/theme/Color.kt`: replace generated colors with Pomocat-inspired palette.
- Modify `app/src/main/java/com/endiar/kuropomodoro/ui/theme/Theme.kt`: disable dynamic colors by default and use the app palette.
- Modify `app/src/main/java/com/endiar/kuropomodoro/ui/theme/Type.kt`: adjust typography for rounded, friendly weights.
- Create `app/src/test/java/com/endiar/kuropomodoro/model/KuroModelsTest.kt`: model behavior tests.

## Task 1: Model Types And Tests

**Files:**
- Create: `app/src/main/java/com/endiar/kuropomodoro/model/KuroModels.kt`
- Create: `app/src/test/java/com/endiar/kuropomodoro/model/KuroModelsTest.kt`

- [ ] **Step 1: Write model tests**

Create `app/src/test/java/com/endiar/kuropomodoro/model/KuroModelsTest.kt` with:

```kotlin
package com.endiar.kuropomodoro.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class KuroModelsTest {
    @Test
    fun timerModeFormatsRemainingTime() {
        assertEquals("25:00", TimerMode.Focus.formatDuration())
        assertEquals("05:00", TimerMode.ShortBreak.formatDuration())
        assertEquals("15:00", TimerMode.LongBreak.formatDuration())
        assertEquals("01:05", formatSeconds(65))
    }

    @Test
    fun stampCountIncrementsForSameDate() {
        val today = LocalDate.of(2026, 5, 2)
        val stamps = addFocusStamp(emptyMap(), today)
        val updated = addFocusStamp(stamps, today)

        assertEquals(2, updated[today])
    }

    @Test
    fun taskToggleChangesCompletionState() {
        val task = FocusTask(id = 1, title = "Read Kotlin docs", minutes = 25)

        assertFalse(task.completed)
        assertTrue(task.toggleCompleted().completed)
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

Run: `./gradlew testDebugUnitTest --tests com.endiar.kuropomodoro.model.KuroModelsTest`

Expected: FAIL because `TimerMode`, `formatSeconds`, `addFocusStamp`, and `FocusTask` do not exist.

- [ ] **Step 3: Add model implementation**

Create `app/src/main/java/com/endiar/kuropomodoro/model/KuroModels.kt` with:

```kotlin
package com.endiar.kuropomodoro.model

import java.time.LocalDate

enum class AppTab(val label: String, val symbol: String) {
    Timer("Focus", "Home"),
    Todo("Todo", "Todo"),
    Calendar("Calendar", "Stamp"),
    Sounds("Sounds", "Sound"),
    Profile("Me", "Me")
}

enum class TimerMode(val label: String, val defaultSeconds: Int, val accentName: String) {
    Focus("Focus", 25 * 60, "peach"),
    ShortBreak("Short Break", 5 * 60, "mint"),
    LongBreak("Long Break", 15 * 60, "lavender");

    fun formatDuration(): String = formatSeconds(defaultSeconds)
}

data class FocusTask(
    val id: Int,
    val title: String,
    val minutes: Int,
    val completed: Boolean = false
) {
    fun toggleCompleted(): FocusTask = copy(completed = !completed)
}

data class TodoListState(
    val id: Int,
    val name: String,
    val tasks: List<FocusTask>
)

data class SoundOption(
    val id: Int,
    val name: String,
    val description: String,
    val locked: Boolean,
    val symbol: String
)

data class TimerSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15
) {
    fun secondsFor(mode: TimerMode): Int = when (mode) {
        TimerMode.Focus -> focusMinutes * 60
        TimerMode.ShortBreak -> shortBreakMinutes * 60
        TimerMode.LongBreak -> longBreakMinutes * 60
    }

    fun update(mode: TimerMode, minutes: Int): TimerSettings {
        val safeMinutes = minutes.coerceAtLeast(1)
        return when (mode) {
            TimerMode.Focus -> copy(focusMinutes = safeMinutes)
            TimerMode.ShortBreak -> copy(shortBreakMinutes = safeMinutes)
            TimerMode.LongBreak -> copy(longBreakMinutes = safeMinutes)
        }
    }
}

fun formatSeconds(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

fun addFocusStamp(stamps: Map<LocalDate, Int>, date: LocalDate): Map<LocalDate, Int> {
    return stamps + (date to ((stamps[date] ?: 0) + 1))
}
```

- [ ] **Step 4: Run model tests and verify pass**

Run: `./gradlew testDebugUnitTest --tests com.endiar.kuropomodoro.model.KuroModelsTest`

Expected: PASS.

- [ ] **Step 5: Commit if repository exists**

Run: `git rev-parse --is-inside-work-tree`

Expected in this workspace: FAIL because the project is not a git repository. If the user initializes git before execution, run:

```bash
git add app/src/main/java/com/endiar/kuropomodoro/model/KuroModels.kt app/src/test/java/com/endiar/kuropomodoro/model/KuroModelsTest.kt
git commit -m "feat: add local pomodoro models"
```

## Task 2: Theme And Shared Components

**Files:**
- Modify: `app/src/main/java/com/endiar/kuropomodoro/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/endiar/kuropomodoro/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/endiar/kuropomodoro/ui/theme/Type.kt`
- Create: `app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt`

- [ ] **Step 1: Replace theme colors**

Replace `app/src/main/java/com/endiar/kuropomodoro/ui/theme/Color.kt` with:

```kotlin
package com.endiar.kuropomodoro.ui.theme

import androidx.compose.ui.graphics.Color

val Cream = Color(0xFFFFF8ED)
val WarmCream = Color(0xFFFFEFD8)
val Peach = Color(0xFFFFB59E)
val Coral = Color(0xFFFF7F73)
val Blush = Color(0xFFFFD8DF)
val Lavender = Color(0xFFD8C8FF)
val Mint = Color(0xFFCDEEDB)
val Sky = Color(0xFFCFEAF8)
val Cocoa = Color(0xFF5F4638)
val CocoaSoft = Color(0xFF8B6B5A)
val CardWhite = Color(0xFFFFFCF7)
val Night = Color(0xFF241E2B)
val NightCard = Color(0xFF342C3F)
```

- [ ] **Step 2: Replace theme setup**

Replace `app/src/main/java/com/endiar/kuropomodoro/ui/theme/Theme.kt` with:

```kotlin
package com.endiar.kuropomodoro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Peach,
    secondary = Lavender,
    tertiary = Mint,
    background = Night,
    surface = NightCard,
    onPrimary = Cocoa,
    onSecondary = Cream,
    onTertiary = Cocoa,
    onBackground = Cream,
    onSurface = Cream
)

private val LightColorScheme = lightColorScheme(
    primary = Coral,
    secondary = Peach,
    tertiary = Mint,
    background = Cream,
    surface = CardWhite,
    onPrimary = CardWhite,
    onSecondary = Cocoa,
    onTertiary = Cocoa,
    onBackground = Cocoa,
    onSurface = Cocoa
)

@Composable
fun KuroPomodoroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme || dynamicColor) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

- [ ] **Step 3: Replace typography**

Replace `app/src/main/java/com/endiar/kuropomodoro/ui/theme/Type.kt` with:

```kotlin
package com.endiar.kuropomodoro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 40.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )
)
```

- [ ] **Step 4: Add shared components**

Create `app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt` with:

```kotlin
package com.endiar.kuropomodoro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.model.AppTab
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.ui.theme.Blush
import com.endiar.kuropomodoro.ui.theme.CardWhite
import com.endiar.kuropomodoro.ui.theme.Cocoa
import com.endiar.kuropomodoro.ui.theme.CocoaSoft
import com.endiar.kuropomodoro.ui.theme.Coral
import com.endiar.kuropomodoro.ui.theme.Cream
import com.endiar.kuropomodoro.ui.theme.Lavender
import com.endiar.kuropomodoro.ui.theme.Mint
import com.endiar.kuropomodoro.ui.theme.Peach
import com.endiar.kuropomodoro.ui.theme.Sky

@Composable
fun KuroCard(modifier: Modifier = Modifier, content: @Composable Column.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Cocoa)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = CocoaSoft)
        }
    }
}

@Composable
fun ModeChip(mode: TimerMode, selected: Boolean, onClick: () -> Unit) {
    val color = when (mode) {
        TimerMode.Focus -> Peach
        TimerMode.ShortBreak -> Mint
        TimerMode.LongBreak -> Lavender
    }
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) color else Cream
    ) {
        Text(
            text = mode.label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = Cocoa,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun KuroPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = CardWhite)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TimerRing(timeText: String, progress: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(230.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(230.dp)) {
            val stroke = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = Blush,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
                size = Size(size.width, size.height)
            )
            drawArc(
                color = Coral,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = stroke,
                size = Size(size.width, size.height)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(timeText, style = MaterialTheme.typography.displaySmall, color = Cocoa)
            Text("stay with kuro", style = MaterialTheme.typography.bodyMedium, color = CocoaSoft)
        }
    }
}

@Composable
fun CatCompanion(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(180.dp)) {
        drawCircle(color = Peach, radius = 62.dp.toPx(), center = Offset(size.width / 2, size.height / 2 + 12.dp.toPx()))
        drawCircle(color = Peach, radius = 44.dp.toPx(), center = Offset(size.width / 2, size.height / 2 - 34.dp.toPx()))
        drawCircle(color = Cocoa, radius = 5.dp.toPx(), center = Offset(size.width / 2 - 16.dp.toPx(), size.height / 2 - 42.dp.toPx()))
        drawCircle(color = Cocoa, radius = 5.dp.toPx(), center = Offset(size.width / 2 + 16.dp.toPx(), size.height / 2 - 42.dp.toPx()))
        drawCircle(color = Coral, radius = 5.dp.toPx(), center = Offset(size.width / 2, size.height / 2 - 28.dp.toPx()))
        drawCircle(color = Cocoa, radius = 12.dp.toPx(), center = Offset(size.width / 2 - 42.dp.toPx(), size.height / 2 + 28.dp.toPx()))
        drawCircle(color = Cocoa, radius = 12.dp.toPx(), center = Offset(size.width / 2 + 42.dp.toPx(), size.height / 2 + 28.dp.toPx()))
    }
}

@Composable
fun BottomTabBar(selectedTab: AppTab, onTabSelected: (AppTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(CardWhite)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AppTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onTabSelected(tab) }
                    .background(if (selected) Blush else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(tab.symbol, color = if (selected) Coral else CocoaSoft, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(tab.label, color = Cocoa, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun DecorativeBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.background(Cream)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(Sky, radius = 120.dp.toPx(), center = Offset(size.width * 0.12f, size.height * 0.16f))
            drawCircle(Mint, radius = 90.dp.toPx(), center = Offset(size.width * 0.92f, size.height * 0.28f))
            drawCircle(Blush, radius = 110.dp.toPx(), center = Offset(size.width * 0.75f, size.height * 0.82f))
        }
        content()
    }
}
```

- [ ] **Step 5: Run build and verify component compilation**

Run: `./gradlew assembleDebug`

Expected: PASS. If it fails for unused imports, remove only the unused imports named by the compiler.

- [ ] **Step 6: Commit if repository exists**

Run: `git rev-parse --is-inside-work-tree`

Expected in this workspace: FAIL. If git exists, run:

```bash
git add app/src/main/java/com/endiar/kuropomodoro/ui/theme app/src/main/java/com/endiar/kuropomodoro/ui/components/KuroComponents.kt
git commit -m "feat: add pomocat inspired compose theme"
```

## Task 3: Root App State And Main Activity

**Files:**
- Modify: `app/src/main/java/com/endiar/kuropomodoro/MainActivity.kt`
- Create: `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`

- [ ] **Step 1: Replace MainActivity**

Replace `app/src/main/java/com/endiar/kuropomodoro/MainActivity.kt` with:

```kotlin
package com.endiar.kuropomodoro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.endiar.kuropomodoro.ui.theme.KuroPomodoroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KuroPomodoroTheme {
                KuroPomodoroApp()
            }
        }
    }
}
```

- [ ] **Step 2: Add root app shell with temporary tab content**

Create `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt` with:

```kotlin
package com.endiar.kuropomodoro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.model.AppTab
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.SoundOption
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.TodoListState
import com.endiar.kuropomodoro.model.addFocusStamp
import com.endiar.kuropomodoro.ui.components.BottomTabBar
import com.endiar.kuropomodoro.ui.components.DecorativeBackground
import java.time.LocalDate
import kotlinx.coroutines.delay

@Composable
fun KuroPomodoroApp() {
    var selectedTab by remember { mutableStateOf(AppTab.Timer) }
    var timerMode by remember { mutableStateOf(TimerMode.Focus) }
    var settings by remember { mutableStateOf(TimerSettings()) }
    var remainingSeconds by remember { mutableIntStateOf(settings.secondsFor(timerMode)) }
    var isRunning by remember { mutableStateOf(false) }
    var completedSessions by remember { mutableIntStateOf(0) }
    var stamps by remember { mutableStateOf<Map<LocalDate, Int>>(emptyMap()) }
    var selectedSoundId by remember { mutableIntStateOf(1) }
    var isSoundPlaying by remember { mutableStateOf(false) }
    var premiumSheetMessage by remember { mutableStateOf<String?>(null) }
    var darkModePreview by remember { mutableStateOf(false) }
    var nextTaskId by remember { mutableIntStateOf(7) }
    var selectedTodoListId by remember { mutableIntStateOf(1) }
    var todoLists by remember {
        mutableStateOf(
            listOf(
                TodoListState(1, "Study", listOf(FocusTask(1, "Review Compose layout", 25), FocusTask(2, "Read chapter notes", 20))),
                TodoListState(2, "Home", listOf(FocusTask(3, "Clean desk", 10), FocusTask(4, "Water plants", 5))),
                TodoListState(3, "Ideas", listOf(FocusTask(5, "Sketch cat animation", 15), FocusTask(6, "Plan next sprint", 25)))
            )
        )
    }
    val sounds = remember {
        listOf(
            SoundOption(1, "Rainy Window", "Soft rain for deep reading", false, "Rain"),
            SoundOption(2, "Cafe Purr", "Warm cafe room ambience", false, "Cafe"),
            SoundOption(3, "Forest Nap", "Leaves, birds, and slow wind", true, "Forest"),
            SoundOption(4, "Fireplace", "Crackling fire with sleepy cat", true, "Fire"),
            SoundOption(5, "Night Keyboard", "Gentle keys in a quiet room", false, "Keys")
        )
    }

    LaunchedEffect(isRunning, remainingSeconds, timerMode) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
        if (isRunning && remainingSeconds == 0) {
            isRunning = false
            if (timerMode == TimerMode.Focus) {
                completedSessions += 1
                stamps = addFocusStamp(stamps, LocalDate.now())
                timerMode = TimerMode.ShortBreak
            }
            remainingSeconds = settings.secondsFor(timerMode)
        }
    }

    DecorativeBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).padding(18.dp), contentAlignment = Alignment.Center) {
                Text("${selectedTab.label} screen coming next")
            }
            BottomTabBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        }
    }
}
```

- [ ] **Step 3: Run build and verify app shell compiles**

Run: `./gradlew assembleDebug`

Expected: PASS with no generated greeting references.

- [ ] **Step 4: Commit if repository exists**

Run: `git rev-parse --is-inside-work-tree`

Expected in this workspace: FAIL. If git exists, run:

```bash
git add app/src/main/java/com/endiar/kuropomodoro/MainActivity.kt app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt
git commit -m "feat: add local compose app shell"
```

## Task 4: Timer Screen

**Files:**
- Create: `app/src/main/java/com/endiar/kuropomodoro/ui/TimerScreen.kt`
- Modify: `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`

- [ ] **Step 1: Add timer screen**

Create `app/src/main/java/com/endiar/kuropomodoro/ui/TimerScreen.kt` with:

```kotlin
package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.formatSeconds
import com.endiar.kuropomodoro.ui.components.CatCompanion
import com.endiar.kuropomodoro.ui.components.KuroCard
import com.endiar.kuropomodoro.ui.components.KuroPrimaryButton
import com.endiar.kuropomodoro.ui.components.ModeChip
import com.endiar.kuropomodoro.ui.components.SectionTitle
import com.endiar.kuropomodoro.ui.components.TimerRing
import com.endiar.kuropomodoro.ui.theme.Cocoa
import com.endiar.kuropomodoro.ui.theme.CocoaSoft
import com.endiar.kuropomodoro.ui.theme.Coral

@Composable
fun TimerScreen(
    mode: TimerMode,
    remainingSeconds: Int,
    totalSeconds: Int,
    isRunning: Boolean,
    completedSessions: Int,
    currentTask: FocusTask?,
    onModeSelected: (TimerMode) -> Unit,
    onStartPause: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Focus with Kuro", "Cute companion timer for calm work")
            Text("$completedSessions stamps", color = Coral, fontWeight = FontWeight.Bold)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimerMode.entries.forEach { timerMode ->
                ModeChip(timerMode, selected = timerMode == mode, onClick = { onModeSelected(timerMode) })
            }
        }

        KuroCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                CatCompanion()
                TimerRing(
                    timeText = formatSeconds(remainingSeconds),
                    progress = if (totalSeconds == 0) 0f else remainingSeconds.toFloat() / totalSeconds.toFloat()
                )
                Text(mode.label, style = MaterialTheme.typography.titleMedium, color = Cocoa)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KuroPrimaryButton(if (isRunning) "Pause" else "Start", onClick = onStartPause, modifier = Modifier.weight(1f))
                    TextButton(onClick = onReset, modifier = Modifier.height(56.dp)) { Text("Reset", color = Cocoa) }
                }
            }
        }

        KuroCard(modifier = Modifier.fillMaxWidth()) {
            Text("Current task", style = MaterialTheme.typography.titleMedium, color = Cocoa)
            Spacer(Modifier.height(8.dp))
            Text(currentTask?.title ?: "Pick a tiny task and let Kuro keep you company.", color = CocoaSoft)
            if (currentTask != null) {
                Text("${currentTask.minutes} min planned", color = CocoaSoft, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
```

- [ ] **Step 2: Wire timer screen into root app**

In `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`, add imports:

```kotlin
import com.endiar.kuropomodoro.ui.TimerScreen
```

Replace the temporary `Text("${selectedTab.label} screen coming next")` inside the content `Box` with:

```kotlin
when (selectedTab) {
    AppTab.Timer -> TimerScreen(
        mode = timerMode,
        remainingSeconds = remainingSeconds,
        totalSeconds = settings.secondsFor(timerMode),
        isRunning = isRunning,
        completedSessions = completedSessions,
        currentTask = todoLists.flatMap { it.tasks }.firstOrNull { !it.completed },
        onModeSelected = {
            timerMode = it
            isRunning = false
            remainingSeconds = settings.secondsFor(it)
        },
        onStartPause = { isRunning = !isRunning },
        onReset = {
            isRunning = false
            remainingSeconds = settings.secondsFor(timerMode)
        }
    )
    else -> Text("${selectedTab.label} screen coming next")
}
```

- [ ] **Step 3: Run build**

Run: `./gradlew assembleDebug`

Expected: PASS.

- [ ] **Step 4: Commit if repository exists**

Run: `git rev-parse --is-inside-work-tree`

Expected in this workspace: FAIL. If git exists, run:

```bash
git add app/src/main/java/com/endiar/kuropomodoro/ui/TimerScreen.kt app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt
git commit -m "feat: add pomodoro timer screen"
```

## Task 5: Todo Screen

**Files:**
- Create: `app/src/main/java/com/endiar/kuropomodoro/ui/TodoScreen.kt`
- Modify: `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`

- [ ] **Step 1: Add todo screen**

Create `app/src/main/java/com/endiar/kuropomodoro/ui/TodoScreen.kt` with:

```kotlin
package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.model.TodoListState
import com.endiar.kuropomodoro.ui.components.KuroCard
import com.endiar.kuropomodoro.ui.components.KuroPrimaryButton
import com.endiar.kuropomodoro.ui.components.SectionTitle
import com.endiar.kuropomodoro.ui.theme.Cocoa
import com.endiar.kuropomodoro.ui.theme.CocoaSoft
import com.endiar.kuropomodoro.ui.theme.Coral

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
    var newTask by remember { mutableStateOf("") }
    val selectedList = todoLists.firstOrNull { it.id == selectedListId } ?: todoLists.first()

    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("Tiny Todo Lists", "Choose what Kuro should guard while you focus")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            todoLists.forEach { list ->
                Text(
                    text = list.name,
                    modifier = Modifier.clickable { onListSelected(list.id) }.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (list.id == selectedListId) Coral else Cocoa,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        KuroCard(modifier = Modifier.fillMaxWidth()) {
            selectedList.tasks.forEach { task ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = task.completed, onCheckedChange = { onToggleTask(task.id) })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            task.title,
                            color = Cocoa,
                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Text("${task.minutes} min", color = CocoaSoft, style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { onDeleteTask(task.id) }) { Text("Delete", color = CocoaSoft) }
                }
            }
            if (selectedList.tasks.isEmpty()) {
                Text("This list is empty. Add one calm, specific task.", color = CocoaSoft)
            }
        }
        KuroCard(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = newTask, onValueChange = { newTask = it }, label = { Text("New task") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            KuroPrimaryButton(
                text = "Add task",
                onClick = {
                    val title = newTask.trim()
                    if (title.isNotEmpty()) {
                        onAddTask(title)
                        newTask = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

- [ ] **Step 2: Wire todo actions into root app**

In `KuroPomodoroApp.kt`, add imports:

```kotlin
import com.endiar.kuropomodoro.ui.TodoScreen
```

Add helper lambdas before `DecorativeBackground`:

```kotlin
val addTask: (String) -> Unit = { title ->
    todoLists = todoLists.map { list ->
        if (list.id == selectedTodoListId) {
            list.copy(tasks = list.tasks + FocusTask(nextTaskId, title, 25))
        } else {
            list
        }
    }
    nextTaskId += 1
}
val toggleTask: (Int) -> Unit = { taskId ->
    todoLists = todoLists.map { list ->
        list.copy(tasks = list.tasks.map { task -> if (task.id == taskId) task.toggleCompleted() else task })
    }
}
val deleteTask: (Int) -> Unit = { taskId ->
    todoLists = todoLists.map { list -> list.copy(tasks = list.tasks.filterNot { it.id == taskId }) }
}
```

Add this branch to the `when (selectedTab)` expression:

```kotlin
AppTab.Todo -> TodoScreen(
    todoLists = todoLists,
    selectedListId = selectedTodoListId,
    onListSelected = { selectedTodoListId = it },
    onAddTask = addTask,
    onToggleTask = toggleTask,
    onDeleteTask = deleteTask
)
```

- [ ] **Step 3: Run build**

Run: `./gradlew assembleDebug`

Expected: PASS.

- [ ] **Step 4: Commit if repository exists**

Run: `git rev-parse --is-inside-work-tree`

Expected in this workspace: FAIL. If git exists, run:

```bash
git add app/src/main/java/com/endiar/kuropomodoro/ui/TodoScreen.kt app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt
git commit -m "feat: add local todo lists"
```

## Task 6: Calendar And Sounds Screens

**Files:**
- Create: `app/src/main/java/com/endiar/kuropomodoro/ui/CalendarScreen.kt`
- Create: `app/src/main/java/com/endiar/kuropomodoro/ui/SoundsScreen.kt`
- Modify: `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`

- [ ] **Step 1: Add calendar screen**

Create `app/src/main/java/com/endiar/kuropomodoro/ui/CalendarScreen.kt` with:

```kotlin
package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.ui.components.KuroCard
import com.endiar.kuropomodoro.ui.components.SectionTitle
import com.endiar.kuropomodoro.ui.theme.Blush
import com.endiar.kuropomodoro.ui.theme.Cocoa
import com.endiar.kuropomodoro.ui.theme.CocoaSoft
import com.endiar.kuropomodoro.ui.theme.Coral
import com.endiar.kuropomodoro.ui.theme.Cream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(stamps: Map<LocalDate, Int>, completedSessions: Int, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val month = YearMonth.from(today)
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("Stamp Calendar", "Every finished focus session leaves a tiny paw")
        KuroCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()), style = MaterialTheme.typography.titleMedium, color = Cocoa)
                Text("$completedSessions sessions", color = Coral, fontWeight = FontWeight.Bold)
            }
        }
        LazyVerticalGrid(columns = GridCells.Fixed(7), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(days) { date ->
                val count = stamps[date] ?: 0
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(if (count > 0) Blush else Cream, RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(date.dayOfMonth.toString(), color = Cocoa, style = MaterialTheme.typography.bodyMedium)
                        Text(if (count > 0) "Paw x$count" else "", color = CocoaSoft, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Add sounds screen**

Create `app/src/main/java/com/endiar/kuropomodoro/ui/SoundsScreen.kt` with:

```kotlin
package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.model.SoundOption
import com.endiar.kuropomodoro.ui.components.KuroCard
import com.endiar.kuropomodoro.ui.components.SectionTitle
import com.endiar.kuropomodoro.ui.theme.Cocoa
import com.endiar.kuropomodoro.ui.theme.CocoaSoft
import com.endiar.kuropomodoro.ui.theme.Coral

@Composable
fun SoundsScreen(
    sounds: List<SoundOption>,
    selectedSoundId: Int,
    isPlaying: Boolean,
    onSoundSelected: (SoundOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("White Noise", "Mock ambience choices for focused sessions")
        sounds.forEach { sound ->
            val selected = sound.id == selectedSoundId
            KuroCard(modifier = Modifier.fillMaxWidth().clickable { onSoundSelected(sound) }) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sound.name, style = MaterialTheme.typography.titleMedium, color = Cocoa)
                        Text(sound.description, color = CocoaSoft)
                    }
                    Text(
                        text = when {
                            sound.locked -> "Locked"
                            selected && isPlaying -> "Playing"
                            selected -> "Selected"
                            else -> sound.symbol
                        },
                        color = if (selected) Coral else CocoaSoft,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Wire calendar and sounds into root app**

In `KuroPomodoroApp.kt`, add imports:

```kotlin
import com.endiar.kuropomodoro.ui.CalendarScreen
import com.endiar.kuropomodoro.ui.SoundsScreen
```

Add these branches to the `when (selectedTab)` expression:

```kotlin
AppTab.Calendar -> CalendarScreen(stamps = stamps, completedSessions = completedSessions)
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
        }
    }
)
```

After `BottomTabBar(...)`, add a simple premium message:

```kotlin
premiumSheetMessage?.let { message ->
    Text(message, modifier = Modifier.padding(16.dp))
}
```

- [ ] **Step 4: Run build**

Run: `./gradlew assembleDebug`

Expected: PASS.

- [ ] **Step 5: Commit if repository exists**

Run: `git rev-parse --is-inside-work-tree`

Expected in this workspace: FAIL. If git exists, run:

```bash
git add app/src/main/java/com/endiar/kuropomodoro/ui/CalendarScreen.kt app/src/main/java/com/endiar/kuropomodoro/ui/SoundsScreen.kt app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt
git commit -m "feat: add calendar stamps and sound picker"
```

## Task 7: Profile And Settings Screen

**Files:**
- Create: `app/src/main/java/com/endiar/kuropomodoro/ui/ProfileScreen.kt`
- Modify: `app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`

- [ ] **Step 1: Add profile screen**

Create `app/src/main/java/com/endiar/kuropomodoro/ui/ProfileScreen.kt` with:

```kotlin
package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.model.TimerMode
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.ui.components.KuroCard
import com.endiar.kuropomodoro.ui.components.SectionTitle
import com.endiar.kuropomodoro.ui.theme.Cocoa
import com.endiar.kuropomodoro.ui.theme.CocoaSoft
import com.endiar.kuropomodoro.ui.theme.Coral

@Composable
fun ProfileScreen(
    settings: TimerSettings,
    darkModePreview: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
    onDurationChanged: (TimerMode, Int) -> Unit,
    onMockFeature: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("Kuro Profile", "Local-only settings and premium simulation")
        KuroCard(modifier = Modifier.fillMaxWidth()) {
            Text("Guest Cat", style = MaterialTheme.typography.titleMedium, color = Cocoa)
            Text("No account required. Friend rooms and purchases are mocked locally.", color = CocoaSoft)
        }
        KuroCard(modifier = Modifier.fillMaxWidth()) {
            Text("Timer lengths", style = MaterialTheme.typography.titleMedium, color = Cocoa)
            DurationRow("Focus", settings.focusMinutes, { onDurationChanged(TimerMode.Focus, it) })
            DurationRow("Short break", settings.shortBreakMinutes, { onDurationChanged(TimerMode.ShortBreak, it) })
            DurationRow("Long break", settings.longBreakMinutes, { onDurationChanged(TimerMode.LongBreak, it) })
        }
        KuroCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Dark mode preview", style = MaterialTheme.typography.titleMedium, color = Cocoa)
                    Text("Stored locally for this session", color = CocoaSoft)
                }
                Switch(checked = darkModePreview, onCheckedChange = onDarkModeChanged)
            }
        }
        listOf("Focus Together", "Reminders", "D-Day Tracker", "Premium Pass").forEach { title ->
            KuroCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, color = Cocoa)
                        Text("Simulated flow for this first Compose clone.", color = CocoaSoft)
                    }
                    TextButton(onClick = { onMockFeature("$title is simulated locally in this build.") }) { Text("Open", color = Coral) }
                }
            }
        }
    }
}

@Composable
private fun DurationRow(label: String, value: Int, onValueChanged: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Cocoa)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onValueChanged(value - 1) }) { Text("-", color = Coral) }
            Text("$value min", color = Cocoa)
            TextButton(onClick = { onValueChanged(value + 1) }) { Text("+", color = Coral) }
        }
    }
}
```

- [ ] **Step 2: Wire profile into root app**

In `KuroPomodoroApp.kt`, add import:

```kotlin
import com.endiar.kuropomodoro.ui.ProfileScreen
```

Add this branch to the `when (selectedTab)` expression:

```kotlin
AppTab.Profile -> ProfileScreen(
    settings = settings,
    darkModePreview = darkModePreview,
    onDarkModeChanged = { darkModePreview = it },
    onDurationChanged = { mode, minutes ->
        settings = settings.update(mode, minutes)
        if (timerMode == mode && !isRunning) {
            remainingSeconds = settings.update(mode, minutes).secondsFor(mode)
        }
    },
    onMockFeature = { premiumSheetMessage = it }
)
```

- [ ] **Step 3: Run build**

Run: `./gradlew assembleDebug`

Expected: PASS.

- [ ] **Step 4: Commit if repository exists**

Run: `git rev-parse --is-inside-work-tree`

Expected in this workspace: FAIL. If git exists, run:

```bash
git add app/src/main/java/com/endiar/kuropomodoro/ui/ProfileScreen.kt app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt
git commit -m "feat: add local profile settings"
```

## Task 8: Polish, Verification, And Cleanup

**Files:**
- Modify only files created or changed in previous tasks if compiler or visual issues require it.

- [ ] **Step 1: Run unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

- [ ] **Step 2: Run debug build**

Run: `./gradlew assembleDebug`
Expected: PASS.

- [ ] **Step 3: Inspect for generated placeholder remnants**

Run: `grep -R "Greeting\|Hello Android" app/src/main/java app/src/test/java`
Expected: no matches.

- [ ] **Step 4: Manual behavior checklist**

Launch the debug app from Android Studio or an emulator and verify:

- Timer tab shows cat companion, mode chips, timer ring, current task, start/pause, and reset.
- Starting the timer decrements the displayed time.
- Reset restores the selected mode duration.
- Todo tab adds, completes, and deletes local tasks.
- Calendar tab shows the current month and displays stamps after a focus completion.
- Sounds tab selects unlocked sounds and shows a premium message for locked sounds.
- Profile tab changes timer durations and opens simulated premium/friend/reminder messages.

- [ ] **Step 5: Commit if repository exists**

Run: `git rev-parse --is-inside-work-tree`
Expected in this workspace: FAIL. If git exists and there are final polish changes, run:

```bash
git add app/src/main/java app/src/test/java
git commit -m "chore: polish pomodoro clone"
```

## Self-Review

- Spec coverage: timer, cat placeholder, tabs, todos, calendar stamps, sounds, profile/settings, local-only premium simulation, theme, and verification are covered by Tasks 1-8.
- Placeholder scan: no TBD/TODO/later placeholders remain; all implementation steps include concrete code or commands.
- Type consistency: `AppTab`, `TimerMode`, `FocusTask`, `TodoListState`, `SoundOption`, `TimerSettings`, `formatSeconds`, and `addFocusStamp` are defined in Task 1 before use in later tasks.
- Repository note: commit steps are included but guarded because this project is currently not inside a git repository.
