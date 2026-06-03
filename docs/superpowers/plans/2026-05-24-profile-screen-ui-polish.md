# Profile Screen UI Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the flat list `ProfileScreen` with a reference-image-faithful design: large header, white rounded section cards, per-row leading icons, subtitles on toggle rows, chevrons on tappable rows, and a NOTIFICATIONS section with an "Enable notification sound" toggle.

**Architecture:** Single file replacement — `ProfileScreen.kt` is rewritten entirely. `PurrodoroApp.kt` needs two parameter additions to the `ProfileScreen` call: `notificationSoundEnabled` and `onUpdateNotificationSound`. No model changes needed; `notificationSoundEnabled` already exists in `AppState` and `AppRepository`.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, existing `AppRepository`/`AppState`

---

### Task 1: Add notification sound parameters to ProfileScreen signature and PurrodoroApp call site

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/ProfileScreen.kt`
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt`

Current `ProfileScreen` signature:
```kotlin
fun ProfileScreen(
    settings: TimerSettings,
    onBack: () -> Unit,
    onUpdateSettings: (TimerSettings) -> Unit,
)
```

Current call site in `PurrodoroApp.kt` (lines ~503–508):
```kotlin
ProfileScreen(
    settings = settings,
    onBack = { showProfile = false },
    onUpdateSettings = { persistSettings(it) },
)
```

`notificationSoundEnabled` is already available as a `var` at line 141 of `PurrodoroApp.kt`:
```kotlin
var notificationSoundEnabled by remember(appState.notificationSoundEnabled) { mutableStateOf(appState.notificationSoundEnabled) }
```

`setNotificationSoundEnabled` is called via the existing `setNotificationSound` lambda at line ~199.

- [ ] **Step 1: Update ProfileScreen signature**

Add two parameters after `onUpdateSettings`:
```kotlin
@Composable
fun ProfileScreen(
    settings: TimerSettings,
    onBack: () -> Unit,
    onUpdateSettings: (TimerSettings) -> Unit,
    notificationSoundEnabled: Boolean,
    onUpdateNotificationSound: (Boolean) -> Unit,
)
```

- [ ] **Step 2: Update PurrodoroApp call site**

Find `setNotificationSound` or `notificationSoundEnabled` usage in `PurrodoroApp.kt` to confirm the lambda name. Then update the `ProfileScreen(...)` call to pass:
```kotlin
ProfileScreen(
    settings = settings,
    onBack = { showProfile = false },
    onUpdateSettings = { persistSettings(it) },
    notificationSoundEnabled = notificationSoundEnabled,
    onUpdateNotificationSound = { setNotificationSound(it) },
)
```
(Verify the exact lambda name — it may be `setNotificationSoundEnabled` or similar; read the file first.)

- [ ] **Step 3: Build to confirm no compile errors**
```
cd purrodoro-android
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**
```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/ProfileScreen.kt \
        purrodoro-android/app/src/main/java/com/endiar/purrodoro/PurrodoroApp.kt
git commit -m "feat(profile): add notification sound params to ProfileScreen signature"
```

---

### Task 2: Rewrite ProfileScreen UI to reference design

**Files:**
- Modify: `purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/ProfileScreen.kt`

Read the file before editing. Replace the entire file contents with the implementation below.

- [ ] **Step 1: Write the new ProfileScreen.kt**

Replace the entire file with:

```kotlin
package com.endiar.purrodoro.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.purrodoro.model.TimerSettings

// ── Colour tokens ──────────────────────────────────────────────────────────────
private val CreamBg        = Color(0xFFFFF8F0)
private val CardWhite      = Color(0xFFFFFFFF)
private val SectionBrown   = Color(0xFFC07850)
private val LabelDark      = Color(0xFF3C2F1E)
private val ValueBrown     = Color(0xFF8B7355)
private val SubtitleGray   = Color(0xFF9E9083)
private val DividerColor   = Color(0xFFEDE5D8)
private val IconBrown      = Color(0xFFC07850)
private val SwitchThumb    = Color(0xFFFFFFFF)
private val SwitchTrackOn  = Color(0xFFC07850)
private val SwitchTrackOff = Color(0xFFD9CFC4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    settings: TimerSettings,
    onBack: () -> Unit,
    onUpdateSettings: (TimerSettings) -> Unit,
    notificationSoundEnabled: Boolean,
    onUpdateNotificationSound: (Boolean) -> Unit,
) {
    var editingField by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LabelDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CreamBg,
                    scrolledContainerColor = CreamBg
                )
            )
        },
        containerColor = CreamBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // ── Large header ────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Settings",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = LabelDark
            )
            Text(
                text = "Customize your focus experience.",
                fontSize = 14.sp,
                color = SubtitleGray,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // ── TIMER section ───────────────────────────────────────────────
            SectionLabel(title = "TIMER", icon = Icons.Outlined.Timer)
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                TappableRow(
                    icon = Icons.Outlined.AccessTime,
                    label = "Focus time",
                    value = "${settings.focusMinutes} min",
                    onClick = { editingField = "focus" }
                )
                CardDivider()
                TappableRow(
                    icon = Icons.Outlined.Coffee,
                    label = "Short break",
                    value = "${settings.shortBreakMinutes} min",
                    onClick = { editingField = "short" }
                )
                CardDivider()
                TappableRow(
                    icon = Icons.Outlined.LocalCafe,
                    label = "Long break",
                    value = "${settings.longBreakMinutes} min",
                    onClick = { editingField = "long" }
                )
                CardDivider()
                TappableRow(
                    icon = Icons.Outlined.Autorenew,
                    label = "Long break after",
                    value = "${settings.focusSessionsPerCycle} sessions",
                    onClick = { editingField = "cycle" }
                )
                CardDivider()
                ToggleRow(
                    icon = Icons.Outlined.PlayCircleOutline,
                    label = "Auto-start next session",
                    subtitle = "Automatically continue to the next session",
                    checked = settings.autoStartNextSession,
                    onCheckedChange = { onUpdateSettings(settings.copy(autoStartNextSession = it)) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── NOTIFICATIONS section ────────────────────────────────────────
            SectionLabel(title = "NOTIFICATIONS", icon = Icons.Outlined.Notifications)
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                ToggleRow(
                    icon = Icons.Outlined.Notifications,
                    label = "Enable notification sound",
                    subtitle = "Play sound when session ends",
                    checked = notificationSoundEnabled,
                    onCheckedChange = onUpdateNotificationSound
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── ABOUT section ───────────────────────────────────────────────
            SectionLabel(title = "ABOUT", icon = Icons.Outlined.Info)
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = IconBrown,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Purrodoro", fontSize = 15.sp, color = LabelDark)
                    }
                    Text(text = "v1.0", fontSize = 13.sp, color = ValueBrown)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ── Numeric edit dialogs ─────────────────────────────────────────────────
    when (editingField) {
        "focus" -> NumericEditDialog(
            title = "Focus time",
            value = settings.focusMinutes,
            min = 1, max = 120, unit = "min",
            onConfirm = { onUpdateSettings(settings.copy(focusMinutes = it)); editingField = null },
            onDismiss = { editingField = null }
        )
        "short" -> NumericEditDialog(
            title = "Short break",
            value = settings.shortBreakMinutes,
            min = 1, max = 120, unit = "min",
            onConfirm = { onUpdateSettings(settings.copy(shortBreakMinutes = it)); editingField = null },
            onDismiss = { editingField = null }
        )
        "long" -> NumericEditDialog(
            title = "Long break",
            value = settings.longBreakMinutes,
            min = 1, max = 120, unit = "min",
            onConfirm = { onUpdateSettings(settings.copy(longBreakMinutes = it)); editingField = null },
            onDismiss = { editingField = null }
        )
        "cycle" -> NumericEditDialog(
            title = "Long break after",
            value = settings.focusSessionsPerCycle,
            min = 1, max = 10, unit = "sessions",
            onConfirm = { onUpdateSettings(settings.copy(focusSessionsPerCycle = it)); editingField = null },
            onDismiss = { editingField = null }
        )
    }
}

// ── Section label ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SectionBrown,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            color = SectionBrown
        )
    }
}

// ── Section card container ───────────────────────────────────────────────────

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
private fun CardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 48.dp),
        color = DividerColor,
        thickness = 0.5.dp
    )
}

// ── Row types ────────────────────────────────────────────────────────────────

@Composable
private fun TappableRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = IconBrown,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = LabelDark,
            modifier = Modifier.weight(1f)
        )
        Text(text = value, fontSize = 13.sp, color = ValueBrown)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "›", fontSize = 18.sp, color = ValueBrown)
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = IconBrown,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 15.sp, color = LabelDark)
            Text(text = subtitle, fontSize = 12.sp, color = SubtitleGray, lineHeight = 16.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SwitchThumb,
                checkedTrackColor = SwitchTrackOn,
                uncheckedThumbColor = SwitchThumb,
                uncheckedTrackColor = SwitchTrackOff,
                uncheckedBorderColor = SwitchTrackOff
            )
        )
    }
}

// ── Numeric edit dialog ──────────────────────────────────────────────────────

@Composable
private fun NumericEditDialog(
    title: String,
    value: Int,
    min: Int,
    max: Int,
    unit: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember(value) { mutableIntStateOf(value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CreamBg,
        title = {
            Text(text = title, fontWeight = FontWeight.SemiBold, color = LabelDark)
        },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { if (current > min) current-- },
                    modifier = Modifier.size(40.dp)
                ) {
                    Text(
                        text = "−",
                        fontSize = 22.sp,
                        color = if (current > min) SwitchTrackOn else Color(0xFFCCBBAA)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "$current $unit",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LabelDark
                )
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = { if (current < max) current++ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Text(
                        text = "+",
                        fontSize = 22.sp,
                        color = if (current < max) SwitchTrackOn else Color(0xFFCCBBAA)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current) }) {
                Text("OK", color = SwitchTrackOn, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ValueBrown.copy(alpha = 0.6f))
            }
        }
    )
}
```

- [ ] **Step 2: Build to confirm no compile errors**
```
cd purrodoro-android
./gradlew assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

If there are missing icon imports (e.g. `Icons.Outlined.Coffee` doesn't exist in Material Icons Extended), substitute with the closest available icon — check by searching the existing codebase for icon import patterns:
```
grep -r "Icons\." purrodoro-android/app/src/main/java --include="*.kt" | grep -o "Icons\.[A-Za-z.]*" | sort -u | head -40
```

Common substitutions if needed:
- `Icons.Outlined.Coffee` → `Icons.Outlined.FreeBreakfast`
- `Icons.Outlined.LocalCafe` → `Icons.Outlined.EmojiFoodBeverage`
- `Icons.Outlined.PlayCircleOutline` → `Icons.Outlined.PlayCircle`

Fix any import issues and re-run until build is clean.

- [ ] **Step 3: Run unit tests**
```
./gradlew test 2>&1 | tail -20
```
Expected: all tests pass.

- [ ] **Step 4: Commit**
```bash
git add purrodoro-android/app/src/main/java/com/endiar/purrodoro/ui/ProfileScreen.kt
git commit -m "feat(profile): redesign UI — cards, icons, subtitles, notification sound toggle"
```
