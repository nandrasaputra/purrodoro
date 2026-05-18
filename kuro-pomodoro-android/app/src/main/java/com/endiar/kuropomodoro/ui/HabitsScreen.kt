package com.endiar.kuropomodoro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.Habit
import com.endiar.kuropomodoro.model.HabitOccurrence
import com.endiar.kuropomodoro.model.HabitTargetType
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.model.TimeBlock
import com.endiar.kuropomodoro.model.currentStreak
import com.endiar.kuropomodoro.model.effectiveTarget
import com.endiar.kuropomodoro.model.isScheduledOn
import com.endiar.kuropomodoro.model.isTargetMetOn
import com.endiar.kuropomodoro.model.progressOn
import com.endiar.kuropomodoro.model.streakLabel
import com.endiar.kuropomodoro.ui.components.GlassCard
import com.endiar.kuropomodoro.ui.components.ScreenStickyBar
import com.endiar.kuropomodoro.ui.theme.KuroSurfaceTokens
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ─── Data transfer object ─────────────────────────────────────────────────────

data class HabitDraft(
    val name: String,
    val emoji: String,
    val colorArgb: Long,
    val targetType: HabitTargetType,
    val targetValue: Int,
    val occurrence: HabitOccurrence,
    val timeBlock: TimeBlock,
)

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    habits: List<Habit>,
    today: LocalDate,
    onAddHabit: (HabitDraft) -> Unit,
    onUpdateHabit: (habitId: Int, draft: HabitDraft) -> Unit,
    onToggleBinary: (habitId: Int, date: LocalDate) -> Unit,
    onIncrementProgress: (habitId: Int, date: LocalDate, delta: Int) -> Unit,
    onSetSkipped: (habitId: Int, date: LocalDate, skipped: Boolean) -> Unit,
    onClearLog: (habitId: Int, date: LocalDate) -> Unit,
    onDeleteHabit: (habitId: Int) -> Unit,
    onStartPomodoroForHabit: (Habit) -> Unit,
    modifier: Modifier = Modifier,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    overBackdrop: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val scrollState = rememberScrollState()
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val topPaddingPx = with(density) { 8.dp.roundToPx() }
    val showStickyBar by remember { derivedStateOf { scrollState.value > headerHeightPx + topPaddingPx } }

    val todayHabits = habits.filter { it.isScheduledOn(today) }
    val upcomingHabits = habits.filter { !it.isScheduledOn(today) }
    val dateLabel = today.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))

    var showAddSheet by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 100.dp)
                .padding(contentPadding),
        ) {
            // ── Header ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { headerHeightPx = it.height },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Habits",
                        color = tokens.textPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(text = dateLabel, color = tokens.textSecondary, fontSize = 13.sp)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(tokens.accent)
                        .clickable { showAddSheet = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "+", color = tokens.accentOn, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(24.dp))

            if (habits.isEmpty()) {
                // ── Empty state ───────────────────────────────────────────
                HabitsEmptyState(tokens = tokens, onAdd = { showAddSheet = true })
            } else {
                // ── TODAY ─────────────────────────────────────────────────
                if (todayHabits.isNotEmpty()) {
                    SectionLabel(text = "TODAY", tokens = tokens)
                    Spacer(Modifier.height(8.dp))
                    todayHabits.forEach { habit ->
                        HabitRow(
                            habit = habit,
                            today = today,
                            isToday = true,
                            glassTreatment = glassTreatment,
                            overBackdrop = overBackdrop,
                            tokens = tokens,
                            onEdit = { editingHabit = habit },
                            onToggle = { onToggleBinary(habit.id, today) },
                            onIncrement = { onIncrementProgress(habit.id, today, 1) },
                            onStartPomodoro = { onStartPomodoroForHabit(habit) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // ── UPCOMING ──────────────────────────────────────────────
                if (upcomingHabits.isNotEmpty()) {
                    if (todayHabits.isNotEmpty()) Spacer(Modifier.height(8.dp))
                    SectionLabel(text = "UPCOMING", tokens = tokens)
                    Spacer(Modifier.height(8.dp))
                    upcomingHabits.forEach { habit ->
                        HabitRow(
                            habit = habit,
                            today = today,
                            isToday = false,
                            glassTreatment = glassTreatment,
                            overBackdrop = overBackdrop,
                            tokens = tokens,
                            onEdit = { editingHabit = habit },
                            onToggle = {},
                            onIncrement = {},
                            onStartPomodoro = {},
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // ── Sticky top bar ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showStickyBar,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it },
        ) {
            ScreenStickyBar(title = "Habits")
        }
    }

    // ── Add sheet ────────────────────────────────────────────────────────
    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
            containerColor = tokens.cardFill,
            contentColor = tokens.textPrimary,
            contentWindowInsets = { WindowInsets(0) },
        ) {
            HabitEditorSheet(
                initial = null,
                tokens = tokens,
                onConfirm = { draft -> onAddHabit(draft); showAddSheet = false },
                onDismiss = { showAddSheet = false },
            )
        }
    }

    // ── Edit / detail sheet ───────────────────────────────────────────────
    editingHabit?.let { habit ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { editingHabit = null },
            sheetState = sheetState,
            containerColor = tokens.cardFill,
            contentColor = tokens.textPrimary,
            contentWindowInsets = { WindowInsets(0) },
        ) {
            HabitEditorSheet(
                initial = habit,
                tokens = tokens,
                onConfirm = { draft -> onUpdateHabit(habit.id, draft); editingHabit = null },
                onDelete = { onDeleteHabit(habit.id); editingHabit = null },
                onDismiss = { editingHabit = null },
            )
        }
    }
}

// ─── Habit row ────────────────────────────────────────────────────────────────

@Composable
private fun HabitRow(
    habit: Habit,
    today: LocalDate,
    isToday: Boolean,
    glassTreatment: GlassTreatment,
    overBackdrop: Boolean,
    tokens: KuroSurfaceTokens,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onIncrement: () -> Unit,
    onStartPomodoro: () -> Unit,
) {
    val accent = Color(habit.colorArgb.toInt())
    val streak = habit.currentStreak(today)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        glassTreatment = glassTreatment,
        overBackdrop = overBackdrop,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Emoji bubble ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (isToday) 0.15f else 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = habit.emoji,
                    fontSize = 20.sp,
                    color = if (isToday) Color.Unspecified else LocalContentColorMuted,
                )
            }

            // ── Name + streak / schedule ──────────────────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = habit.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isToday) tokens.textPrimary else tokens.textMuted,
                )
                val sub = buildString {
                    if (streak > 0) append("🔥 ${streakLabel(streak)}")
                    if (!isToday) {
                        val schedule = nextScheduledLabel(habit, today)
                        if (streak > 0) append("  ·  $schedule")
                        else append(schedule)
                    }
                }
                if (sub.isNotEmpty()) {
                    Text(text = sub, fontSize = 11.sp, color = tokens.textSecondary)
                }
            }

            // ── Action (today only) — intercepts its own tap ──────────────
            if (isToday) {
                HabitRowAction(
                    habit = habit,
                    today = today,
                    accent = accent,
                    tokens = tokens,
                    onToggle = onToggle,
                    onIncrement = onIncrement,
                    onStartPomodoro = onStartPomodoro,
                )
            }
        }
    }
}

@Composable
private fun HabitRowAction(
    habit: Habit,
    today: LocalDate,
    accent: Color,
    tokens: KuroSurfaceTokens,
    onToggle: () -> Unit,
    onIncrement: () -> Unit,
    onStartPomodoro: () -> Unit,
) {
    val isDone = habit.isTargetMetOn(today)
    when (habit.targetType) {
        HabitTargetType.Binary -> {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isDone) accent else tokens.segmentedTrack)
                    .border(1.5.dp, if (isDone) Color.Transparent else tokens.borderSoft, CircleShape)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                if (isDone) {
                    Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        HabitTargetType.Quantity -> {
            val progress = habit.progressOn(today)
            val target = habit.effectiveTarget()
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDone) accent else tokens.segmentedTrack)
                    .border(1.5.dp, if (isDone) Color.Transparent else accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onIncrement)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when {
                        isDone -> "✓"
                        progress == 0 -> "0/$target"
                        else -> "$progress/$target"
                    },
                    color = if (isDone) Color.White else if (progress == 0) tokens.textMuted else tokens.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        HabitTargetType.Pomodoro -> {
            val progress = habit.progressOn(today)
            val target = habit.effectiveTarget()
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDone) accent else tokens.segmentedTrack)
                    .border(1.5.dp, if (isDone) Color.Transparent else accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onStartPomodoro)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isDone) {
                        Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("▶", color = accent, fontSize = 10.sp)
                        Text(
                            "$progress/$target",
                            color = if (progress == 0) tokens.textMuted else tokens.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Short human label for when/how often this habit runs. */
private fun nextScheduledLabel(habit: Habit, today: LocalDate): String {
    return when (val o = habit.occurrence) {
        is HabitOccurrence.Daily -> "Every day"
        is HabitOccurrence.Weekdays -> {
            val shortNames = o.days.sortedBy { it.value }
                .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
            shortNames
        }
    }
}

// Used to dim emoji text color for upcoming habits without a composable modifier stack
private val LocalContentColorMuted = Color(0xFF757575)

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun HabitsEmptyState(
    tokens: KuroSurfaceTokens,
    onAdd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("🌱", fontSize = 48.sp)
        Text(
            text = "No habits yet",
            color = tokens.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Build your first daily habit and pair it\nwith your Pomodoro sessions.",
            color = tokens.textSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(tokens.accent)
                .clickable(onClick = onAdd)
                .padding(horizontal = 28.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+ Add habit",
                color = tokens.accentOn,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─── Section label ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String, tokens: KuroSurfaceTokens) {
    Text(
        text = text,
        color = tokens.textFaint,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
    )
}

// ─── Habit editor sheet ───────────────────────────────────────────────────────

private val PRESET_COLORS: List<Long> = listOf(
    0xFFEF9A9AL, 0xFFFFCC80L, 0xFFFFF176L, 0xFFA5D6A7L,
    0xFF80DEEAL, 0xFF90CAF9L, 0xFFCE93D8L, 0xFFBCAAA4L,
)

private val HABIT_EMOJIS: List<String> = listOf(
    // fitness
    "🏃", "🚶", "🧘", "💪", "🏋️", "🤸", "🚴", "🧗",
    // health & food
    "🥤", "🍎", "🥗", "💊", "☕", "🫖", "🥦", "🥕",
    // mind & learning
    "📖", "📝", "🎯", "💡", "🧠", "🎨", "🎵", "🧩",
    // lifestyle
    "🛏️", "🧹", "🌱", "🌞", "🌙", "🪥", "💰", "🌿",
    // social & rewards
    "❤️", "😊", "🙏", "✅", "⭐", "🔥", "🎉", "🏆",
    // nature & misc
    "🌊", "⛰️", "🦋", "🌸", "🐕", "📸", "✍️", "🗓️",
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitEditorSheet(
    initial: Habit?,
    tokens: KuroSurfaceTokens,
    onConfirm: (HabitDraft) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "✅") }
    var colorArgb by remember { mutableLongStateOf(initial?.colorArgb ?: PRESET_COLORS[0]) }
    var targetType by remember { mutableStateOf(initial?.targetType ?: HabitTargetType.Binary) }
    var targetValue by remember { mutableIntStateOf(initial?.targetValue ?: 1) }
    var occurrence by remember { mutableStateOf(initial?.occurrence ?: HabitOccurrence.Daily) }

    val isDaily = occurrence == HabitOccurrence.Daily
    val weekdaySet = (occurrence as? HabitOccurrence.Weekdays)?.days ?: emptySet()
    val timeBlock = initial?.timeBlock ?: com.endiar.kuropomodoro.model.TimeBlock.Anytime

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = if (initial == null) "New Habit" else "Edit Habit",
            color = tokens.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        // ── Name + emoji ──────────────────────────────────────────────────
        EditorLabel("NAME", tokens)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            // Emoji picker button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.segmentedTrack)
                    .clickable { showEmojiPicker = true },
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 24.sp)
            }
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("e.g. Meditate", color = tokens.textMuted, fontSize = 14.sp) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.segmentedTrack),
                singleLine = true,
                colors = transparentFieldColors(tokens),
            )
        }

        // ── Color ─────────────────────────────────────────────────────────
        EditorLabel("COLOR", tokens)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PRESET_COLORS.forEach { c ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(c.toInt()))
                        .border(2.5.dp, if (c == colorArgb) tokens.textPrimary else Color.Transparent, CircleShape)
                        .clickable { colorArgb = c },
                )
            }
        }

        // ── Type ──────────────────────────────────────────────────────────
        EditorLabel("TYPE", tokens)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(HabitTargetType.Binary to "Check off", HabitTargetType.Quantity to "Count", HabitTargetType.Pomodoro to "Pomodoro")
                .forEach { (t, label) ->
                    PillChip(label = label, selected = targetType == t, tokens = tokens) { targetType = t }
                }
        }

        // ── Target value ──────────────────────────────────────────────────
        if (targetType != HabitTargetType.Binary) {
            val unit = if (targetType == HabitTargetType.Pomodoro) "sessions" else "times"
            EditorLabel("TARGET", tokens)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                StepBtn("−", targetValue > 1, tokens) { if (targetValue > 1) targetValue-- }
                Spacer(Modifier.width(20.dp))
                Text("$targetValue $unit", color = tokens.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(20.dp))
                StepBtn("+", targetValue < 20, tokens) { if (targetValue < 20) targetValue++ }
            }
        }

        // ── Schedule ──────────────────────────────────────────────────────
        EditorLabel("SCHEDULE", tokens)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillChip("Every day", isDaily, tokens) { occurrence = HabitOccurrence.Daily }
            PillChip("Custom days", !isDaily, tokens) {
                if (isDaily) occurrence = HabitOccurrence.Weekdays(setOf(DayOfWeek.MONDAY))
            }
        }
        if (!isDaily) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayOfWeek.values().forEach { dow ->
                    val sel = dow in weekdaySet
                    PillChip(
                        label = dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        selected = sel,
                        tokens = tokens,
                    ) {
                        val newSet = if (sel) weekdaySet - dow else weekdaySet + dow
                        occurrence = if (newSet.isEmpty()) HabitOccurrence.Daily
                                     else HabitOccurrence.Weekdays(newSet)
                    }
                }
            }
        }

        // ── Confirm ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (name.isBlank()) tokens.accent.copy(alpha = 0.4f) else tokens.accent)
                .clickable(enabled = name.isNotBlank()) {
                    onConfirm(
                        HabitDraft(
                            name = name.trim(),
                            emoji = emoji.ifBlank { "✅" },
                            colorArgb = colorArgb,
                            targetType = targetType,
                            targetValue = targetValue.coerceAtLeast(1),
                            occurrence = occurrence,
                            timeBlock = timeBlock,
                        )
                    )
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (initial == null) "Add Habit" else "Save Changes",
                color = tokens.accentOn,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // ── Delete (edit mode only) ───────────────────────────────────────
        if (onDelete != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFE53935).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .clickable { showDeleteConfirm = true }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Delete Habit",
                    color = Color(0xFFE53935).copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    // ── Emoji picker dialog ───────────────────────────────────────────────
    if (showEmojiPicker) {
        AlertDialog(
            onDismissRequest = { showEmojiPicker = false },
            title = {
                Text("Choose icon", color = tokens.textPrimary, fontWeight = FontWeight.SemiBold)
            },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(HABIT_EMOJIS) { e ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (e == emoji) tokens.accent.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (e == emoji) tokens.accent.copy(alpha = 0.5f) else Color.Transparent,
                                    RoundedCornerShape(8.dp),
                                )
                                .clickable { emoji = e; showEmojiPicker = false },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(e, fontSize = 22.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = tokens.cardFill,
            shape = RoundedCornerShape(20.dp),
        )
    }

    // ── Delete confirmation dialog ────────────────────────────────────────
    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "Delete habit?",
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Text(
                    text = "All logs and streak data will be permanently removed.",
                    color = tokens.textSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Delete", color = Color(0xFFE53935), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = tokens.textSecondary)
                }
            },
            containerColor = tokens.cardFill,
            shape = RoundedCornerShape(20.dp),
        )
    }
}

// ─── Small shared composables ─────────────────────────────────────────────────

@Composable
private fun EditorLabel(text: String, tokens: KuroSurfaceTokens) {
    Text(text = text, color = tokens.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
}

@Composable
private fun PillChip(label: String, selected: Boolean, tokens: KuroSurfaceTokens, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) tokens.accentSoft else tokens.segmentedTrack)
            .border(1.dp, if (selected) tokens.accent.copy(alpha = 0.6f) else tokens.borderSoft, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) tokens.accent else tokens.textSecondary,
        )
    }
}

@Composable
private fun StepBtn(label: String, enabled: Boolean, tokens: KuroSurfaceTokens, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (enabled) tokens.segmentedTrack else tokens.segmentedTrack.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (enabled) tokens.textPrimary else tokens.textMuted, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun transparentFieldColors(tokens: KuroSurfaceTokens) = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedTextColor = tokens.textPrimary,
    unfocusedTextColor = tokens.textPrimary,
    cursorColor = tokens.accent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)
