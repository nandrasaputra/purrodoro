# Session History Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist per-cycle Pomodoro session records to DataStore and display them on the Records screen filtered by the calendar-selected date.

**Architecture:** Add `SessionRecord`/`SessionStatus` models, encode/decode them in `AppRepository` (same URL-encode pattern as todo lists), track an in-progress record in `KuroPomodoroApp` (open on first play, increment on Focus completion, close on Stop or cycle completion), and render session cards in `CalendarScreen` below the calendar heatmap.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX DataStore Preferences, `java.time`

---

### Task 1: Add `SessionStatus` and `SessionRecord` to the domain model

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/model/KuroModels.kt`

- [ ] **Step 1: Add the new types at the bottom of `KuroModels.kt` (before `backgroundImageForTime`)**

Add after line 183 (after `addUnfinishedStamp`):

```kotlin
enum class SessionStatus { Completed, Partial }

data class SessionRecord(
    val id: Long,
    val startedAt: Long,
    val endedAt: Long,
    val focusMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val completedFocusCount: Int,
    val totalFocusCount: Int,
    val status: SessionStatus,
    val taskName: String?
)
```

- [ ] **Step 2: Build the project to verify it compiles**

```bash
cd kuro-pomodoro-android && ./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/model/KuroModels.kt
git commit -m "feat: add SessionRecord and SessionStatus models"
```

---

### Task 2: Add session record persistence to `AppRepository`

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/data/AppRepository.kt`

- [ ] **Step 1: Add the DataStore key and update `AppState`**

In `AppRepository.kt`:

1. Add to the `companion object` keys block (after `KEY_IS_PREMIUM`):
```kotlin
private val KEY_SESSION_RECORDS = stringPreferencesKey("session_records_json")
```

2. Add field to `AppState` data class (after `isPremium`):
```kotlin
val sessionRecords: List<SessionRecord> = emptyList()
```

3. Add import at top of file:
```kotlin
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
```

- [ ] **Step 2: Read `sessionRecords` in the `appState` flow**

In the `appState` flow `map` block, add after `isPremium = ...`:
```kotlin
sessionRecords = decodeSessionRecords(prefs[KEY_SESSION_RECORDS] ?: ""),
```

- [ ] **Step 3: Add `appendSessionRecord` write function**

Add after `setIsPremium`:
```kotlin
suspend fun appendSessionRecord(record: SessionRecord) = dataStore.edit { prefs ->
    val existing = decodeSessionRecords(prefs[KEY_SESSION_RECORDS] ?: "")
    prefs[KEY_SESSION_RECORDS] = encodeSessionRecords(existing + record)
}
```

- [ ] **Step 4: Add encode/decode helpers at the bottom of the file**

```kotlin
internal fun encodeSessionRecords(records: List<SessionRecord>): String =
    records.joinToString("\n") { r ->
        val taskEncoded = if (r.taskName != null) URLEncoder.encode(r.taskName, "UTF-8") else ""
        listOf(
            r.id, r.startedAt, r.endedAt,
            r.focusMinutes, r.shortBreakMinutes, r.longBreakMinutes,
            r.completedFocusCount, r.totalFocusCount,
            r.status.name, taskEncoded
        ).joinToString("|")
    }

internal fun decodeSessionRecords(raw: String): List<SessionRecord> {
    if (raw.isBlank()) return emptyList()
    return runCatching {
        raw.split("\n").mapNotNull { line ->
            val p = line.split("|")
            if (p.size < 10) return@mapNotNull null
            SessionRecord(
                id = p[0].toLong(),
                startedAt = p[1].toLong(),
                endedAt = p[2].toLong(),
                focusMinutes = p[3].toInt(),
                shortBreakMinutes = p[4].toInt(),
                longBreakMinutes = p[5].toInt(),
                completedFocusCount = p[6].toInt(),
                totalFocusCount = p[7].toInt(),
                status = SessionStatus.valueOf(p[8]),
                taskName = if (p[9].isBlank()) null else URLDecoder.decode(p[9], "UTF-8")
            )
        }
    }.getOrElse { emptyList() }
}
```

- [ ] **Step 5: Build to verify**

```bash
cd kuro-pomodoro-android && ./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/data/AppRepository.kt
git commit -m "feat: persist session records in DataStore"
```

---

### Task 3: Track in-progress record in `KuroPomodoroApp` and save on close

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`

This task wires up three things:
1. **Open** an in-memory record when `isStarted` transitions `false → true`
2. **Increment** `completedFocusCount` each time a Focus block finishes naturally
3. **Close** the record (save to repo) on Stop or cycle completion

- [ ] **Step 1: Add `openRecord` state variable**

Add after the `isFocusMode` state declaration (around line 135):

```kotlin
// In-progress session record; null when no cycle is active
var openRecord by remember { mutableStateOf<SessionRecord?>(null) }
```

Add this import at the top:
```kotlin
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
```

- [ ] **Step 2: Add `persistSessionRecord` helper**

Add after `persistTodoLists`:
```kotlin
fun persistSessionRecord(record: SessionRecord) {
    scope.launch { repo.appendSessionRecord(record) }
}
```

- [ ] **Step 3: Open a record when the timer first starts**

In the `onStartPause` lambda (around line 362), inside the `if (!isRunning)` branch, after `isStarted = true`, add:

```kotlin
if (openRecord == null) {
    val totalFocus = cycleItems.count { it.type is SessionItemType.Focus }
    openRecord = SessionRecord(
        id = System.currentTimeMillis(),
        startedAt = System.currentTimeMillis(),
        endedAt = 0L,
        focusMinutes = settings.focusMinutes,
        shortBreakMinutes = settings.shortBreakMinutes,
        longBreakMinutes = settings.longBreakMinutes,
        completedFocusCount = 0,
        totalFocusCount = totalFocus,
        status = SessionStatus.Partial,
        taskName = activeTask?.title
    )
}
```

- [ ] **Step 4: Close the record on Stop**

In the `onStop` lambda (around line 377), before `isRunning = false`, add:

```kotlin
openRecord?.let { rec ->
    persistSessionRecord(
        rec.copy(
            endedAt = System.currentTimeMillis(),
            status = SessionStatus.Partial,
            taskName = activeTask?.title
        )
    )
    openRecord = null
}
```

- [ ] **Step 5: Increment `completedFocusCount` and close on natural cycle completion**

In the `LaunchedEffect` block, inside `if (updatedRemainingSeconds == 0)`, after the focus stamp logic (around line 295 after the `if (currentItem?.type is SessionItemType.Focus)` block), add:

```kotlin
// Increment completed focus count on natural Focus completion
if (currentItem?.type is SessionItemType.Focus) {
    openRecord = openRecord?.copy(
        completedFocusCount = (openRecord?.completedFocusCount ?: 0) + 1
    )
}
```

Then, after the cycle-advance block where `nextIndex >= cycleItems.size` (full cycle complete, around line 302), add:

```kotlin
// Full cycle complete — close record as Completed
openRecord?.let { rec ->
    persistSessionRecord(
        rec.copy(
            endedAt = System.currentTimeMillis(),
            status = SessionStatus.Completed,
            taskName = activeTask?.title
        )
    )
    openRecord = null
}
```

- [ ] **Step 6: Build to verify**

```bash
cd kuro-pomodoro-android && ./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt
git commit -m "feat: track and persist session records during cycle"
```

---

### Task 4: Thread `sessionRecords` down to `CalendarScreen`

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt`
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/CalendarScreen.kt`

- [ ] **Step 1: Expose `sessionRecords` from `appState` in `KuroPomodoroApp`**

After the existing `var todoLists by remember...` line (around line 120), add:

```kotlin
var sessionRecords by remember(appState.sessionRecords) { mutableStateOf(appState.sessionRecords) }
```

- [ ] **Step 2: Pass `sessionRecords` to `CalendarScreen`**

In the `AppTab.Calendar ->` branch (around line 407), update the call:

```kotlin
AppTab.Calendar -> CalendarScreen(
    stamps = stamps,
    completedSessions = completedSessions,
    sessionRecords = sessionRecords,
    glassTreatment = glassTreatment
)
```

- [ ] **Step 3: Update `CalendarScreen` signature to accept `sessionRecords`**

Change the function signature:

```kotlin
@Composable
fun CalendarScreen(
    stamps: Map<LocalDate, Int>,
    completedSessions: Int,
    sessionRecords: List<SessionRecord> = emptyList(),
    modifier: Modifier = Modifier,
    glassTreatment: GlassTreatment = GlassTreatment.Clear
)
```

Add import at top of `CalendarScreen.kt`:
```kotlin
import com.endiar.kuropomodoro.model.SessionRecord
import com.endiar.kuropomodoro.model.SessionStatus
```

- [ ] **Step 4: Build to verify**

```bash
cd kuro-pomodoro-android && ./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/KuroPomodoroApp.kt \
        kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/CalendarScreen.kt
git commit -m "feat: thread sessionRecords to CalendarScreen"
```

---

### Task 5: Add selected-date state and session list UI to `CalendarScreen`

**Files:**
- Modify: `kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/CalendarScreen.kt`

- [ ] **Step 1: Add `selectedDate` state and make calendar cells tappable**

In `CalendarScreen`, after `val today = LocalDate.now()`, add:

```kotlin
var selectedDate by remember { mutableStateOf(today) }
```

Add these imports:
```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
```

In the calendar grid loop, change `DayCell(...)` call to:

```kotlin
DayCell(
    day = dayNum,
    isToday = isToday,
    isSelected = date == selectedDate,
    hasSessions = hasSessions,
    onClick = { selectedDate = date }
)
```

- [ ] **Step 2: Update `DayCell` to support `isSelected` and `onClick`**

Replace the existing `DayCell` composable:

```kotlin
@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    hasSessions: Boolean,
    onClick: () -> Unit = {}
) {
    val bg = when {
        isSelected  -> Color(0xFFFF8B6F)
        hasSessions -> Color.White.copy(alpha = 0.12f)
        else        -> Color.Transparent
    }
    val border = when {
        isSelected  -> Color.Transparent
        isToday     -> Color.White.copy(alpha = 0.60f)
        hasSessions -> Color.White.copy(alpha = 0.20f)
        else        -> Color.Transparent
    }
    val textColor = when {
        isSelected  -> Color.White
        hasSessions -> Color.White
        else        -> Color.White.copy(alpha = 0.50f)
    }
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
```

- [ ] **Step 3: Add session list section below the calendar**

After the existing `Text("Total sessions: ...")` line, add:

```kotlin
SessionSection(
    date = selectedDate,
    records = sessionRecords,
    glassTreatment = glassTreatment
)
```

- [ ] **Step 4: Implement `SessionSection` composable**

Add below `DayCell`:

```kotlin
@Composable
private fun SessionSection(
    date: LocalDate,
    records: List<SessionRecord>,
    glassTreatment: GlassTreatment
) {
    val dateRecords = records
        .filter { record ->
            val recordDate = java.time.Instant.ofEpochMilli(record.startedAt)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            recordDate == date
        }
        .sortedByDescending { it.startedAt }

    val dateLabel = if (date == LocalDate.now()) {
        val monthLabel = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        "Today · $monthLabel ${date.dayOfMonth}"
    } else {
        val monthLabel = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        "$monthLabel ${date.dayOfMonth}, ${date.year}"
    }

    Text(
        text = dateLabel,
        color = Color.White.copy(alpha = 0.60f),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )

    if (dateRecords.isEmpty()) {
        Text(
            text = "No sessions on this day",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    } else {
        dateRecords.forEach { record ->
            SessionCard(record = record, glassTreatment = glassTreatment)
        }
    }
}
```

- [ ] **Step 5: Implement `SessionCard` composable**

Add below `SessionSection`:

```kotlin
@Composable
private fun SessionCard(record: SessionRecord, glassTreatment: GlassTreatment) {
    val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
    val zone = java.time.ZoneId.systemDefault()
    val startTime = java.time.Instant.ofEpochMilli(record.startedAt).atZone(zone)
        .format(timeFormatter)
    val endTime = java.time.Instant.ofEpochMilli(record.endedAt).atZone(zone)
        .format(timeFormatter)

    val badgeText = if (record.status == SessionStatus.Completed) "Completed" else "Partial"
    val badgeColor = if (record.status == SessionStatus.Completed) Color(0xFF4CAF50) else Color(0xFFFF8B6F)
    val badgeBg = if (record.status == SessionStatus.Completed) Color(0x334CAF50) else Color(0x33FF8B6F)

    val dots = buildString {
        repeat(record.completedFocusCount) { append("● ") }
        repeat(record.totalFocusCount - record.completedFocusCount) { append("○ ") }
    }.trimEnd()

    GlassCard(modifier = Modifier.fillMaxWidth(), glassTreatment = glassTreatment) {
        // Header row: time range + badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$startTime – $endTime",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(text = badgeText, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(4.dp))
        // Focus dots + count
        Text(
            text = "$dots  ${record.completedFocusCount} / ${record.totalFocusCount} focus · ${record.focusMinutes} min each",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp
        )
        // Break line
        Text(
            text = "Break · ${record.shortBreakMinutes} min short + ${record.longBreakMinutes} min long",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp
        )
        // Optional task chip
        if (record.taskName != null) {
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "📌 ${record.taskName}",
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
```

Add missing imports to `CalendarScreen.kt`:
```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
```

- [ ] **Step 6: Build to verify**

```bash
cd kuro-pomodoro-android && ./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add kuro-pomodoro-android/app/src/main/java/com/endiar/kuropomodoro/ui/CalendarScreen.kt
git commit -m "feat: add session history list to Records screen"
```

---

## Self-Review Checklist

- [x] **Spec coverage:** Data model ✓, DataStore persist ✓, Open on first play ✓, Increment on Focus hit 0 ✓, Close on Stop (Partial) ✓, Close on cycle end (Completed) ✓, Calendar date picker ✓, Session cards ✓, Empty state ✓, Break combined into one line ✓, Task chip ✓
- [x] **No placeholders:** All steps have complete code
- [x] **Type consistency:** `SessionRecord`, `SessionStatus`, `openRecord` used consistently across all tasks
