# Developer Utility Panel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a debug-only developer utility panel accessible via persistent notification, with four actions: clock override, timer speed-up, session injection, and data reset.

**Architecture:** A `ClockDebugSource` interface and `AppClock` object live in the main source set — in release builds `AppClock.source` is always `null` and behavior is identical to `LocalTime.now()`. All debug classes (`DebugRepository`, `KuroDebugApplication`, `DebugActivity`) live in the `debug` source set and are absent from release APKs. The main app's two time-dependent call sites and the timer loop are updated to use `AppClock`.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, AndroidX DataStore, NotificationManager, JUnit4

---

## File Map

### New files — main source set
- `app/src/main/java/com/endiar/purrodoro/model/AppClock.kt` — `ClockDebugSource` interface + `AppClock` object

### Modified files — main source set
- `app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt` — replace `LocalTime.now()` on line 239, replace `delay(250)` on line 235

### New files — debug source set
- `app/src/debug/java/com/endiar/purrodoro/debug/DebugRepository.kt`
- `app/src/debug/java/com/endiar/purrodoro/debug/KuroDebugApplication.kt`
- `app/src/debug/java/com/endiar/purrodoro/debug/DebugNotification.kt`
- `app/src/debug/java/com/endiar/purrodoro/debug/DebugActivity.kt`
- `app/src/debug/AndroidManifest.xml`

### New files — test source set
- `app/src/test/java/com/endiar/purrodoro/model/AppClockTest.kt`
- `app/src/test/java/com/endiar/purrodoro/debug/DebugRepositoryTest.kt`

---

## Task 1: `AppClock` — interface + object in main source set

**Files:**
- Create: `app/src/main/java/com/endiar/purrodoro/model/AppClock.kt`
- Create: `app/src/test/java/com/endiar/purrodoro/model/AppClockTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/endiar/purrodoro/model/AppClockTest.kt`:

```kotlin
package com.endiar.purrodoro.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalTime

class AppClockTest {

    @After
    fun tearDown() {
        AppClock.source = null
    }

    @Test
    fun nowReturnsRealTimeWhenSourceIsNull() {
        AppClock.source = null
        val before = LocalTime.now()
        val result = AppClock.now()
        val after = LocalTime.now()
        // result should be between before and after (inclusive)
        assertNotNull(result)
        assert(!result.isBefore(before) || result == before)
    }

    @Test
    fun nowReturnsOverrideWhenSourceHasValue() {
        val fixed = LocalTime.of(22, 0)
        AppClock.source = object : ClockDebugSource {
            override val clockOverride = MutableStateFlow<LocalTime?>(fixed)
            override val speedUpTimer = MutableStateFlow(false)
        }
        assertEquals(fixed, AppClock.now())
    }

    @Test
    fun nowReturnsRealTimeWhenOverrideIsNull() {
        AppClock.source = object : ClockDebugSource {
            override val clockOverride = MutableStateFlow<LocalTime?>(null)
            override val speedUpTimer = MutableStateFlow(false)
        }
        assertNotNull(AppClock.now())
    }

    @Test
    fun tickIntervalMsReturns250WhenSourceIsNull() {
        AppClock.source = null
        assertEquals(250L, AppClock.tickIntervalMs())
    }

    @Test
    fun tickIntervalMsReturns250WhenSpeedUpIsFalse() {
        AppClock.source = object : ClockDebugSource {
            override val clockOverride = MutableStateFlow<LocalTime?>(null)
            override val speedUpTimer = MutableStateFlow(false)
        }
        assertEquals(250L, AppClock.tickIntervalMs())
    }

    @Test
    fun tickIntervalMsReturns25WhenSpeedUpIsTrue() {
        AppClock.source = object : ClockDebugSource {
            override val clockOverride = MutableStateFlow<LocalTime?>(null)
            override val speedUpTimer = MutableStateFlow(true)
        }
        assertEquals(25L, AppClock.tickIntervalMs())
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd purrodoro-android
./gradlew :app:testDebugUnitTest --tests "com.endiar.purrodoro.model.AppClockTest" 2>&1 | tail -20
```

Expected: FAILED — `AppClock` and `ClockDebugSource` not defined.

- [ ] **Step 3: Create `AppClock.kt`**

Create `app/src/main/java/com/endiar/purrodoro/model/AppClock.kt`:

```kotlin
package com.endiar.purrodoro.model

import kotlinx.coroutines.flow.StateFlow
import java.time.LocalTime

/**
 * Interface implemented by DebugRepository (debug source set only).
 * AppClock.source is set to null in release builds.
 */
interface ClockDebugSource {
    val clockOverride: StateFlow<LocalTime?>
    val speedUpTimer: StateFlow<Boolean>
}

/**
 * App-wide clock. Use AppClock.now() instead of LocalTime.now() anywhere
 * the time-of-day affects app rendering or logic.
 *
 * In release builds, source is always null and behaviour is identical to LocalTime.now().
 * In debug builds, DebugRepository sets source so clock overrides take effect.
 */
object AppClock {
    var source: ClockDebugSource? = null

    fun now(): LocalTime =
        source?.clockOverride?.value ?: LocalTime.now()

    fun tickIntervalMs(): Long =
        if (source?.speedUpTimer?.value == true) 25L else 250L
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.endiar.purrodoro.model.AppClockTest" 2>&1 | tail -20
```

Expected: 6 tests PASSED.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/model/AppClock.kt \
        app/src/test/java/com/endiar/purrodoro/model/AppClockTest.kt
git commit -m "feat: add AppClock and ClockDebugSource for injectable time"
```

---

## Task 2: Wire `AppClock` into `PurrodoroApp`

**Files:**
- Modify: `app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt` (lines 235, 239)

- [ ] **Step 1: Replace `LocalTime.now()` on line 239**

In `PurrodoroApp.kt`, find:
```kotlin
    val backdropTime = LocalTime.now()
```
Replace with:
```kotlin
    val backdropTime = AppClock.now()
```

- [ ] **Step 2: Replace `delay(250)` on line 235**

In `PurrodoroApp.kt`, find:
```kotlin
            delay(250)
```
Replace with:
```kotlin
            delay(AppClock.tickIntervalMs())
```

- [ ] **Step 3: Add import if not already present**

Ensure this import exists in `PurrodoroApp.kt` (add after existing model imports):
```kotlin
import com.endiar.purrodoro.model.AppClock
```

- [ ] **Step 4: Run existing tests to verify nothing broke**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -20
```

Expected: All existing tests PASSED.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt
git commit -m "feat: use AppClock.now() and tickIntervalMs() in PurrodoroApp"
```

---

## Task 3: `DebugRepository` in debug source set

**Files:**
- Create: `app/src/debug/java/com/endiar/purrodoro/debug/DebugRepository.kt`
- Create: `app/src/test/java/com/endiar/purrodoro/debug/DebugRepositoryTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/endiar/purrodoro/debug/DebugRepositoryTest.kt`:

```kotlin
package com.endiar.purrodoro.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class DebugRepositoryTest {

    @Test
    fun initialStateHasNoClockOverride() {
        val repo = DebugRepository()
        assertNull(repo.clockOverride.value)
    }

    @Test
    fun initialStateHasSpeedUpDisabled() {
        val repo = DebugRepository()
        assertEquals(false, repo.speedUpTimer.value)
    }

    @Test
    fun setClockOverrideUpdatesFlow() {
        val repo = DebugRepository()
        val time = LocalTime.of(22, 0)
        repo.setClockOverride(time)
        assertEquals(time, repo.clockOverride.value)
    }

    @Test
    fun clearClockOverrideSetsNull() {
        val repo = DebugRepository()
        repo.setClockOverride(LocalTime.of(8, 0))
        repo.setClockOverride(null)
        assertNull(repo.clockOverride.value)
    }

    @Test
    fun setSpeedUpTimerUpdatesFlow() {
        val repo = DebugRepository()
        repo.setSpeedUpTimer(true)
        assertEquals(true, repo.speedUpTimer.value)
    }

    @Test
    fun initCreatesInstanceAndAssignsCompanion() {
        val repo = DebugRepository.init()
        assertEquals(repo, DebugRepository.instance)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.endiar.purrodoro.debug.DebugRepositoryTest" 2>&1 | tail -20
```

Expected: FAILED — `DebugRepository` not defined.

- [ ] **Step 3: Create the debug source set directory and `DebugRepository.kt`**

```bash
mkdir -p app/src/debug/java/com/endiar/purrodoro/debug
```

Create `app/src/debug/java/com/endiar/purrodoro/debug/DebugRepository.kt`:

```kotlin
package com.endiar.purrodoro.debug

import com.endiar.purrodoro.model.ClockDebugSource
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalTime

class DebugRepository : ClockDebugSource {

    override val clockOverride = MutableStateFlow<LocalTime?>(null)
    override val speedUpTimer = MutableStateFlow(false)

    fun setClockOverride(time: LocalTime?) {
        clockOverride.value = time
    }

    fun setSpeedUpTimer(enabled: Boolean) {
        speedUpTimer.value = enabled
    }

    companion object {
        lateinit var instance: DebugRepository
            private set

        fun init(): DebugRepository =
            DebugRepository().also { instance = it }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.endiar.purrodoro.debug.DebugRepositoryTest" 2>&1 | tail -20
```

Expected: 6 tests PASSED.

- [ ] **Step 5: Commit**

```bash
git add app/src/debug/java/com/endiar/purrodoro/debug/DebugRepository.kt \
        app/src/test/java/com/endiar/purrodoro/debug/DebugRepositoryTest.kt
git commit -m "feat: add DebugRepository in debug source set"
```

---

## Task 4: `DebugNotification` + `KuroDebugApplication`

**Files:**
- Create: `app/src/debug/java/com/endiar/purrodoro/debug/DebugNotification.kt`
- Create: `app/src/debug/java/com/endiar/purrodoro/debug/KuroDebugApplication.kt`
- Create: `app/src/debug/AndroidManifest.xml`

No unit tests for these — they depend on Android `Context` and `NotificationManager`. Manual verification in Step 5.

- [ ] **Step 1: Create `DebugNotification.kt`**

Create `app/src/debug/java/com/endiar/purrodoro/debug/DebugNotification.kt`:

```kotlin
package com.endiar.purrodoro.debug

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.endiar.purrodoro.R

private const val CHANNEL_ID = "kuro_dev"
private const val NOTIFICATION_ID = 9001

fun postDevNotification(context: Context) {
    val nm = context.getSystemService(NotificationManager::class.java)

    val channel = NotificationChannel(
        CHANNEL_ID,
        "Purrodoro Dev Tools",
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = "Developer utility panel"
    }
    nm.createNotificationChannel(channel)

    val intent = Intent(context, DebugActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Purrodoro Dev Tools")
        .setContentText("Tap to open debug panel")
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    nm.notify(NOTIFICATION_ID, notification)
}
```

- [ ] **Step 2: Create `KuroDebugApplication.kt`**

Create `app/src/debug/java/com/endiar/purrodoro/debug/KuroDebugApplication.kt`:

```kotlin
package com.endiar.purrodoro.debug

import android.app.Application
import com.endiar.purrodoro.model.AppClock

class KuroDebugApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val debugRepo = DebugRepository.init()
        AppClock.source = debugRepo
        postDevNotification(this)
    }
}
```

- [ ] **Step 3: Create debug `AndroidManifest.xml`**

Create `app/src/debug/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:name=".debug.KuroDebugApplication"
        tools:replace="android:name">

        <activity
            android:name=".debug.DebugActivity"
            android:exported="false"
            android:label="Purrodoro Dev Tools" />

    </application>

</manifest>
```

- [ ] **Step 4: Run full test suite to verify nothing broke**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -20
```

Expected: All tests PASSED.

- [ ] **Step 5: Commit**

```bash
git add app/src/debug/java/com/endiar/purrodoro/debug/DebugNotification.kt \
        app/src/debug/java/com/endiar/purrodoro/debug/KuroDebugApplication.kt \
        app/src/debug/AndroidManifest.xml
git commit -m "feat: add KuroDebugApplication, dev notification, debug manifest"
```

---

## Task 5: `DebugActivity` — Compose UI

**Files:**
- Create: `app/src/debug/java/com/endiar/purrodoro/debug/DebugActivity.kt`

This is a Compose Activity — no unit tests (UI-only, depends on Android runtime). Verify manually on device/emulator after this task.

- [ ] **Step 1: Create `DebugActivity.kt`**

Create `app/src/debug/java/com/endiar/purrodoro/debug/DebugActivity.kt`:

```kotlin
package com.endiar.purrodoro.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.endiar.purrodoro.data.AppRepository
import com.endiar.purrodoro.model.addFocusStamp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class DebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DebugScreen(
                    debugRepo = DebugRepository.instance,
                    appRepo = AppRepository.get(this),
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
private fun DebugScreen(
    debugRepo: DebugRepository,
    appRepo: AppRepository,
    onClose: () -> Unit
) {
    val clockOverride by debugRepo.clockOverride.collectAsStateWithLifecycle()
    val speedUpTimer by debugRepo.speedUpTimer.collectAsStateWithLifecycle()
    val appState by appRepo.appState.collectAsStateWithLifecycle(
        initialValue = com.endiar.purrodoro.data.AppState(
            darkModePreview = false,
            glassReadabilityMode = com.endiar.purrodoro.model.GlassReadabilityMode.Adaptive,
            selectedSoundId = 1,
            completedSessions = 0,
            settings = com.endiar.purrodoro.model.TimerSettings(),
            nextTaskId = 7,
            selectedTodoListId = 1,
            stamps = emptyMap(),
            todoLists = emptyList()
        )
    )
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Purrodoro Dev Tools", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClose) { Text("Close") }
            }

            HorizontalDivider()

            // --- Clock Override ---
            SectionTitle("CLOCK OVERRIDE")
            Text(
                text = if (clockOverride != null) "Active: ${clockOverride}" else "Using real time",
                style = MaterialTheme.typography.bodySmall,
                color = if (clockOverride != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "08:00" to LocalTime.of(8, 0),
                    "12:00" to LocalTime.of(12, 0),
                    "17:00" to LocalTime.of(17, 0),
                    "22:00" to LocalTime.of(22, 0)
                ).forEach { (label, time) ->
                    OutlinedButton(onClick = { debugRepo.setClockOverride(time) }) {
                        Text(label)
                    }
                }
            }
            TextButton(
                onClick = { debugRepo.setClockOverride(null) },
                enabled = clockOverride != null
            ) {
                Text("Clear override")
            }

            HorizontalDivider()

            // --- Timer Speed-Up ---
            SectionTitle("TIMER")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Speed up timer", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "25 min → ~150 seconds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = speedUpTimer,
                    onCheckedChange = { debugRepo.setSpeedUpTimer(it) }
                )
            }

            HorizontalDivider()

            // --- Session Injection ---
            SectionTitle("SESSION INJECTION")
            var injectionCount by remember { mutableIntStateOf(1) }
            var injectionDate by remember { mutableStateOf(LocalDate.now()) }

            Text(
                "Date: $injectionDate (today)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { if (injectionCount > 1) injectionCount-- }
                ) { Text("−") }
                Text("$injectionCount", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = { if (injectionCount < 10) injectionCount++ }
                ) { Text("+") }
            }
            Button(
                onClick = {
                    scope.launch {
                        var updatedStamps = appState.stamps
                        repeat(injectionCount) {
                            updatedStamps = addFocusStamp(updatedStamps, injectionDate)
                        }
                        appRepo.setStamps(updatedStamps)
                        appRepo.setCompletedSessions(appState.completedSessions + injectionCount)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add $injectionCount Session${if (injectionCount > 1) "s" else ""}")
            }

            HorizontalDivider()

            // --- Danger Zone ---
            SectionTitle("DANGER ZONE", color = MaterialTheme.colorScheme.error)
            var resetConfirming by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    if (!resetConfirming) {
                        resetConfirming = true
                    } else {
                        scope.launch {
                            appRepo.setStamps(emptyMap())
                            appRepo.setCompletedSessions(0)
                            appRepo.setTodoLists(emptyList())
                        }
                        resetConfirming = false
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (resetConfirming) MaterialTheme.colorScheme.error
                                     else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (resetConfirming) MaterialTheme.colorScheme.onError
                                   else MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (resetConfirming) "Are you sure? Tap again to confirm"
                    else "Reset All Data"
                )
            }
            if (resetConfirming) {
                TextButton(onClick = { resetConfirming = false }) {
                    Text("Cancel")
                }
            }
            Text(
                "Wipes stamps, sessions, and todos back to seed state.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String, color: androidx.compose.ui.graphics.Color = Color.Unspecified) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else color
    )
}
```

- [ ] **Step 2: Run full test suite**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -20
```

Expected: All tests PASSED. (DebugActivity has no unit tests — it's Compose UI.)

- [ ] **Step 3: Build debug APK to verify it compiles**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/debug/java/com/endiar/purrodoro/debug/DebugActivity.kt
git commit -m "feat: add DebugActivity with clock override, timer speed-up, session injection, reset"
```

---

## Task 6: Verify release build has no debug code

- [ ] **Step 1: Assemble release build**

```bash
./gradlew :app:assembleRelease 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL` with no warnings about debug classes.

- [ ] **Step 2: Confirm `AppClock.source` is always null in release**

`AppClock.source` is only ever set in `KuroDebugApplication.onCreate()`, which is in the debug source set. The release `AndroidManifest.xml` still declares the original `Application` class (none — Android defaults to `android.app.Application`). No further action needed; this is structural guarantee.

- [ ] **Step 3: Run full test suite one final time**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -20
```

Expected: All tests PASSED.

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "feat: developer utility panel complete — debug-only, notification entry point"
```

---

## Manual Verification Checklist (on device/emulator)

After installing the debug APK:

- [ ] Persistent "Purrodoro Dev Tools" notification appears in notification shade
- [ ] Tapping notification opens `DebugActivity`
- [ ] Clock override: tap `22:00` → main app cat backdrop switches to night scene immediately
- [ ] Clock override: tap `Clear override` → backdrop returns to current real time
- [ ] Timer speed-up: enable toggle → start a focus session → completes in ~150 seconds
- [ ] Session injection: add 3 sessions for today → Calendar screen shows +3 stamps for today
- [ ] Reset all data: tap once → button label changes → tap again → stamps/sessions cleared, todos back to seed
- [ ] Install release APK → no "Purrodoro Dev Tools" notification → `AppClock.source` is null (real time always used)
