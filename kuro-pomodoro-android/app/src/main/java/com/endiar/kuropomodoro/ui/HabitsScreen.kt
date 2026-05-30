package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.R
import com.endiar.kuropomodoro.model.Habit
import com.endiar.kuropomodoro.model.HabitOccurrence
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.model.TimeBlock
import com.endiar.kuropomodoro.model.bestStreak
import com.endiar.kuropomodoro.model.currentStreak
import com.endiar.kuropomodoro.model.isScheduledOn
import com.endiar.kuropomodoro.model.isTargetMetOn
import com.endiar.kuropomodoro.ui.components.KuroPrimaryButton
import com.endiar.kuropomodoro.ui.designsystem.KuroBottomSheet
import com.endiar.kuropomodoro.ui.designsystem.KuroHabitCard
import com.endiar.kuropomodoro.ui.designsystem.KuroHabitStatusCard
import com.endiar.kuropomodoro.ui.designsystem.KuroHabitTodayStatus
import com.endiar.kuropomodoro.ui.designsystem.StaggeredEntrance
import com.endiar.kuropomodoro.ui.designsystem.KuroHabitMonthDotUi
import com.endiar.kuropomodoro.ui.designsystem.KuroHabitMotivationCard
import com.endiar.kuropomodoro.ui.designsystem.KuroHabitsEmptyState
import com.endiar.kuropomodoro.ui.designsystem.KuroHabitWeekDotUi
import com.endiar.kuropomodoro.ui.designsystem.KuroInputField
import com.endiar.kuropomodoro.ui.designsystem.KuroMonthlyHabitProgressCard
import com.endiar.kuropomodoro.ui.designsystem.KuroScreenScaffold
import com.endiar.kuropomodoro.ui.designsystem.KuroSpacing
import com.endiar.kuropomodoro.ui.designsystem.KuroType
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import com.endiar.kuropomodoro.ui.theme.Mint
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
    // Mirrors the Tasks detail focus gating. `isFocusActive` is true while any
    // Pomodoro is running or paused; `liveHabitId` is the habit currently
    // attached to that session (null when none / a task is focused).
    isFocusActive: Boolean = false,
    liveHabitId: Int? = null,
    onSwitchToHome: () -> Unit = {},
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var detailHabitId by rememberSaveable { mutableStateOf<Int?>(null) }

    // Resolve the open detail habit from the live list so its card, streak and
    // monthly grid stay in sync as logs change underneath it.
    val detailHabit = detailHabitId?.let { id -> habits.firstOrNull { it.id == id } }
    if (detailHabitId != null && detailHabit == null) {
        // Habit was deleted while its detail sheet was open — close the sheet.
        detailHabitId = null
    }

    KuroScreenScaffold(
        stickyTitle = "Habits",
        modifier = modifier,
        contentPadding = contentPadding,
    ) { scrollState, headerMeasureModifier ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = KuroSpacing.lg)
                .padding(top = KuroSpacing.lg, bottom = KuroSpacing.dockClearance)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
        ) {
            Box(headerMeasureModifier) {
                HabitsScreenHeader(onAdd = { showAddSheet = true })
            }

            if (habits.isEmpty()) {
                EmptyHabitCard(onAdd = { showAddSheet = true })
            } else {
                val grouped = groupHabitsByDueState(habits, today)
                // Single running index so the staggered entrance keeps a
                // smooth cascade across both sections.
                var entranceIndex = 0

                if (grouped.today.isNotEmpty()) {
                    val remaining = grouped.today.count { !it.isTargetMetOn(today) }
                    Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                        HabitSectionHeader(
                            title = "Today",
                            trailing = if (remaining == 0) "All done 🎉" else "$remaining to go",
                            trailingAccent = remaining == 0,
                        )
                        grouped.today.forEach { habit ->
                            StaggeredEntrance(index = entranceIndex++) {
                                HabitListCard(
                                    habit = habit,
                                    today = today,
                                    onClick = { detailHabitId = habit.id },
                                    onToggleDone = { onToggleBinary(habit.id, today) },
                                )
                            }
                        }
                    }
                }

                if (grouped.upcoming.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                        HabitSectionHeader(
                            title = "Coming up",
                            trailing = "${grouped.upcoming.size}",
                        )
                        grouped.upcoming.forEach { habit ->
                            StaggeredEntrance(index = entranceIndex++) {
                                HabitListCard(
                                    habit = habit,
                                    today = today,
                                    onClick = { detailHabitId = habit.id },
                                    onToggleDone = { onToggleBinary(habit.id, today) },
                                )
                            }
                        }
                    }
                }

                KuroHabitMotivationCard(
                    quote = "Consistency is the secret behind all progress.",
                    encouragement = "You're doing great! 🌟",
                )
            }
        }
    }

    // Detail + edit live in a single adaptive bottom sheet (same component as
    // the Home Pomodoro confirmation): tapping "Edit" swaps the sheet content
    // in place and the height animates to fit, instead of opening a new sheet.
    if (detailHabit != null) {
        HabitDetailSheet(
            habit = detailHabit,
            today = today,
            isFocusActive = isFocusActive,
            isLive = detailHabit.id == liveHabitId,
            onDismiss = { detailHabitId = null },
            onUpdate = { draft -> onUpdateHabit(detailHabit.id, draft) },
            onToggleToday = { onToggleBinary(detailHabit.id, today) },
            onStartFocus = {
                detailHabitId = null
                onStartPomodoroForHabit(detailHabit)
            },
            onOpenFocus = {
                detailHabitId = null
                onSwitchToHome()
            },
            onDelete = {
                onDeleteHabit(detailHabit.id)
                detailHabitId = null
            },
        )
    }

    if (showAddSheet) {
        KuroBottomSheet(title = "Add habit", onDismiss = { showAddSheet = false }) {
            HabitEditorSheet(
                initial = null,
                onConfirm = { draft -> onAddHabit(draft); showAddSheet = false },
                onDismiss = { showAddSheet = false },
            )
        }
    }
}

// ─── Habits screen sections ────────────────────────────────────────────────────

@Composable
private fun HabitsScreenHeader(
    onAdd: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Title only — no leading mascot glyph, matching the shared top-bar
        // pattern used by the Tasks screen. The sticky bar shown on scroll
        // already echoes this title.
        Text(
            text = "Habits",
            color = tokens.textPrimary,
            style = KuroType.headingDisplay,
        )
        Box(
            modifier = Modifier
                .size(KuroSpacing.xxxl + KuroSpacing.sm)
                .clip(CircleShape)
                .background(tokens.accent)
                .clickable(onClick = onAdd)
                .semantics { role = Role.Button; contentDescription = "Add habit" },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "+", color = tokens.accentOn, style = KuroType.fabGlyph)
        }
    }
}

@Composable
private fun LeafBadge() {
    Box(
        modifier = Modifier.size(KuroSpacing.habitLeafBadge),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(KuroSpacing.md - KuroSpacing.xs)
                .height(KuroSpacing.xl)
                .rotate(-32f)
                .clip(RoundedCornerShape(topStart = KuroSpacing.md, topEnd = KuroSpacing.md, bottomEnd = KuroSpacing.md))
                .background(Mint),
        )
        Box(
            modifier = Modifier
                .width(KuroSpacing.md - KuroSpacing.xs)
                .height(KuroSpacing.lg)
                .padding(top = KuroSpacing.xs + 1.dp)
                .rotate(34f)
                .clip(RoundedCornerShape(topStart = KuroSpacing.md, topEnd = KuroSpacing.md, bottomStart = KuroSpacing.md))
                .background(Mint.copy(alpha = 0.74f)),
        )
    }
}

@Composable
private fun EmptyHabitCard(
    onAdd: () -> Unit,
) {
    KuroHabitsEmptyState(
        title = "No habits yet",
        message = "Add a small repeatable action and let Kuro keep you company.",
        onAdd = onAdd,
        leading = { LeafBadge() },
    )
}

/** Section label for the grouped habit list ("Today" / "Coming up"). */
@Composable
private fun HabitSectionHeader(
    title: String,
    trailing: String? = null,
    trailingAccent: Boolean = false,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = KuroType.labelStrong, color = tokens.textMuted)
        if (trailing != null) {
            Text(
                text = trailing,
                style = KuroType.labelMeta,
                color = if (trailingAccent) tokens.accent else tokens.textFaint,
            )
        }
    }
}

@Composable
private fun HabitListCard(
    habit: Habit,
    today: LocalDate,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
) {
    KuroHabitStatusCard(
        name = habit.name,
        emoji = habit.emoji,
        accentColor = Color(habit.colorArgb.toInt()),
        streak = habit.currentStreak(today),
        statusLabel = habitTodayStatusLabel(habit, today),
        status = habitTodayStatus(habit, today),
        onClick = onClick,
        onToggleDone = onToggleDone,
    )
}

private fun habitTodayStatus(habit: Habit, today: LocalDate): KuroHabitTodayStatus = when {
    !habit.isScheduledOn(today) -> KuroHabitTodayStatus.RestDay
    habit.isTargetMetOn(today) -> KuroHabitTodayStatus.Done
    else -> KuroHabitTodayStatus.Pending
}

private fun habitTodayStatusLabel(habit: Habit, today: LocalDate): String = when {
    !habit.isScheduledOn(today) -> {
        val next = habit.nextScheduledDate(today)
        if (next == today.plusDays(1)) "Tomorrow"
        else "Next ${next.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}"
    }
    habit.isTargetMetOn(today) -> "Done today"
    else -> "Due today"
}

@Composable
private fun MonthlyHabitProgressCard(habits: List<Habit>, today: LocalDate) {
    val dots = monthlyHabitProgressDots(habits, today)
    val monthLabel = today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    val leadingBlanks = dots.firstOrNull()?.date?.dayOfWeek?.let { (it.value - 1) % 7 } ?: 0
    val gridSlots = List(leadingBlanks) {
        KuroHabitMonthDotUi(isCompleted = false, isToday = false, isPlaceholder = true)
    } + dots.map { dot ->
        KuroHabitMonthDotUi(
            isCompleted = dot.isCompleted,
            isToday = dot.date == today,
        )
    }
    KuroMonthlyHabitProgressCard(
        monthLabel = monthLabel,
        rows = gridSlots.chunked(7),
    )
}

private fun habitStreakUnitLabel(habit: Habit): String = when (habit.occurrence) {
    is HabitOccurrence.Daily -> "day streak"
    is HabitOccurrence.Weekdays -> "week streak"
}

private fun habitScheduleLabel(habit: Habit, today: LocalDate): String =
    nextScheduledLabel(habit, today)

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
    onConfirm: (HabitDraft) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "✅") }
    var colorArgb by remember { mutableLongStateOf(initial?.colorArgb ?: PRESET_COLORS[0]) }
    // The schedule is just a set of selected weekdays. "Every day" = all 7
    // selected, which is the default for a new habit.
    var selectedDays by remember {
        mutableStateOf(initialSelectedDays(initial?.occurrence ?: HabitOccurrence.Daily))
    }
    val timeBlock = initial?.timeBlock ?: TimeBlock.Anytime

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.lg),
    ) {
        EditorLabel("NAME")
        Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(KuroSpacing.habitEditorEmojiButton)
                    .clip(RoundedCornerShape(KuroSpacing.radiusSm))
                    .background(tokens.segmentedTrack)
                    .clickable { showEmojiPicker = true },
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, style = KuroType.habitEmoji)
            }
            KuroInputField(
                value = name,
                onValueChange = { name = it },
                placeholder = "e.g. Meditate",
                modifier = Modifier.weight(1f),
            )
        }

        EditorLabel("COLOR")
        Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
            PRESET_COLORS.forEach { c ->
                Box(
                    modifier = Modifier
                        .size(KuroSpacing.habitColorSwatch)
                        .clip(CircleShape)
                        .background(Color(c.toInt()))
                        .border(
                            KuroSpacing.habitColorSwatchRing,
                            if (c == colorArgb) tokens.textPrimary else Color.Transparent,
                            CircleShape,
                        )
                        .clickable { colorArgb = c },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EditorLabel("REPEAT")
            Text(
                text = daysSummaryLabel(selectedDays),
                color = tokens.textSecondary,
                style = KuroType.labelMeta,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
        ) {
            DayOfWeek.entries.forEach { dow ->
                DayToggle(
                    label = dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    selected = dow in selectedDays,
                    accent = Color(colorArgb.toInt()),
                    modifier = Modifier.weight(1f),
                ) {
                    selectedDays =
                        if (dow in selectedDays) {
                            // Never allow an empty schedule — keep the last day on.
                            (selectedDays - dow).ifEmpty { selectedDays }
                        } else {
                            selectedDays + dow
                        }
                }
            }
        }

        KuroPrimaryButton(
            text = if (initial == null) "Add Habit" else "Save Changes",
            onClick = {
                val trimmed = name.trim()
                if (trimmed.isEmpty()) return@KuroPrimaryButton
                onConfirm(
                    HabitDraft(
                        name = trimmed,
                        emoji = emoji.ifBlank { "✅" },
                        colorArgb = colorArgb,
                        occurrence = occurrenceFromDays(selectedDays),
                        timeBlock = timeBlock,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (onDelete != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(KuroSpacing.md))
                    .border(1.dp, tokens.destructive.copy(alpha = 0.4f), RoundedCornerShape(KuroSpacing.md))
                    .clickable { showDeleteConfirm = true }
                    .padding(vertical = KuroSpacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Delete Habit",
                    color = tokens.destructive.copy(alpha = 0.85f),
                    style = KuroType.labelStrong,
                )
            }
        }
    }

    if (showEmojiPicker) {
        val accent = Color(colorArgb.toInt())
        AlertDialog(
            onDismissRequest = { showEmojiPicker = false },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2)) {
                    Text("Choose an icon", color = tokens.textPrimary, style = KuroType.headingCard)
                    Text(
                        "Pick something that fits your habit.",
                        color = tokens.textSecondary,
                        style = KuroType.bodySecondary,
                    )
                }
            },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = KuroSpacing.habitEmojiPickerMaxHeight),
                    verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
                ) {
                    items(HABIT_EMOJIS) { e ->
                        val isSelected = e == emoji
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(KuroSpacing.radiusMd))
                                .background(
                                    if (isSelected) accent.copy(alpha = 0.22f) else tokens.segmentedTrack,
                                )
                                .border(
                                    KuroSpacing.habitColorSwatchRing,
                                    if (isSelected) accent else Color.Transparent,
                                    RoundedCornerShape(KuroSpacing.radiusMd),
                                )
                                .clickable { emoji = e; showEmojiPicker = false },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(e, style = KuroType.habitEmoji, textAlign = TextAlign.Center)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEmojiPicker = false }) {
                    Text("Done", color = tokens.accent, style = KuroType.labelStrong)
                }
            },
            containerColor = tokens.cardFill,
            shape = RoundedCornerShape(KuroSpacing.radiusLg),
        )
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete habit?", color = tokens.textPrimary, style = KuroType.headingCard) },
            text = { Text("All logs and streak data will be permanently removed.", color = tokens.textSecondary) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Delete", color = tokens.destructive, style = KuroType.labelStrong)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = tokens.textSecondary)
                }
            },
            containerColor = tokens.cardFill,
            shape = RoundedCornerShape(KuroSpacing.radiusMd),
        )
    }
}

@Composable
private fun EditorLabel(text: String) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Text(text = text, color = tokens.textMuted, style = KuroType.labelCaption)
}

/** Single round day-of-week toggle used by the schedule picker. */
@Composable
private fun DayToggle(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = modifier
            .height(KuroSpacing.habitDayToggle)
            .clip(CircleShape)
            .background(if (selected) accent.copy(alpha = 0.20f) else tokens.segmentedTrack)
            .border(
                KuroSpacing.habitColorSwatchRing,
                if (selected) accent else Color.Transparent,
                CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) tokens.textPrimary else tokens.textSecondary,
            style = KuroType.labelStrong,
        )
    }
}

// ─── Schedule helpers ─────────────────────────────────────────────────────────

private val ALL_DAYS: Set<DayOfWeek> = DayOfWeek.entries.toSet()

private fun initialSelectedDays(occurrence: HabitOccurrence): Set<DayOfWeek> = when (occurrence) {
    HabitOccurrence.Daily -> ALL_DAYS
    is HabitOccurrence.Weekdays -> occurrence.days
}

private fun occurrenceFromDays(days: Set<DayOfWeek>): HabitOccurrence =
    if (days.isEmpty() || days.size == ALL_DAYS.size) HabitOccurrence.Daily
    else HabitOccurrence.Weekdays(days)

private fun daysSummaryLabel(days: Set<DayOfWeek>): String = when {
    days.isEmpty() || days.size == ALL_DAYS.size -> "Every day"
    days.size == 5 && days.none { it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY } -> "Weekdays"
    days.size == 2 && days.all { it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY } -> "Weekends"
    else -> days.sortedBy { it.value }
        .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

// ─── Habit detail sheet ───────────────────────────────────────────────────────

private enum class HabitDetailPage { Detail, Edit }

@Composable
private fun HabitDetailSheet(
    habit: Habit,
    today: LocalDate,
    isFocusActive: Boolean,
    isLive: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (HabitDraft) -> Unit,
    onToggleToday: () -> Unit,
    onStartFocus: () -> Unit,
    onOpenFocus: () -> Unit,
    onDelete: () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(HabitDetailPage.Detail) }

    KuroBottomSheet(
        title = null,
        onDismiss = onDismiss,
        contentKey = page,
    ) {
        when (page) {
            HabitDetailPage.Detail -> HabitDetailContent(
                habit = habit,
                today = today,
                isFocusActive = isFocusActive,
                isLive = isLive,
                onClose = onDismiss,
                onEdit = { page = HabitDetailPage.Edit },
                onToggleToday = onToggleToday,
                onStartFocus = onStartFocus,
                onOpenFocus = onOpenFocus,
                onDelete = onDelete,
            )

            HabitDetailPage.Edit -> Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(KuroSpacing.lg),
            ) {
                HabitEditSubpageHeader(onBack = { page = HabitDetailPage.Detail })
                HabitEditorSheet(
                    initial = habit,
                    onConfirm = { draft ->
                        onUpdate(draft)
                        page = HabitDetailPage.Detail
                    },
                    onDelete = onDelete,
                    onDismiss = { page = HabitDetailPage.Detail },
                )
            }
        }
    }
}

@Composable
private fun HabitDetailContent(
    habit: Habit,
    today: LocalDate,
    isFocusActive: Boolean,
    isLive: Boolean,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onToggleToday: () -> Unit,
    onStartFocus: () -> Unit,
    onOpenFocus: () -> Unit,
    onDelete: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val accent = Color(habit.colorArgb.toInt())
    val doneToday = habit.isTargetMetOn(today)
    val focusCta = when {
        isLive -> HabitFocusCtaState.OpenFocus
        isFocusActive -> HabitFocusCtaState.Blocked
        else -> HabitFocusCtaState.StartFocus
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.lg),
    ) {
        HabitDetailSheetHeader(onEdit = onEdit, onClose = onClose)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(KuroSpacing.habitDetailEmojiTile)
                    .clip(RoundedCornerShape(KuroSpacing.md))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = habit.emoji, style = KuroType.headingDisplay)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2),
            ) {
                Text(habit.name, color = tokens.textPrimary, style = KuroType.headingDisplay)
                Text(
                    habitScheduleLabel(habit, today),
                    color = tokens.textSecondary,
                    style = KuroType.bodySecondary,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md)) {
            HabitStatTile(
                value = habit.currentStreak(today).toString(),
                label = "Current streak",
                modifier = Modifier.weight(1f),
            )
            HabitStatTile(
                value = habit.bestStreak(today).toString(),
                label = "Best streak",
                modifier = Modifier.weight(1f),
            )
        }

        HabitSectionHeader(title = "This week")
        KuroHabitCard(
            name = habit.name,
            emoji = habit.emoji,
            accentColor = accent,
            scheduleLabel = habitScheduleLabel(habit, today),
            streak = habit.currentStreak(today),
            streakUnitLabel = habitStreakUnitLabel(habit),
            weekDots = habitWeekDots(habit, today).map { dot ->
                KuroHabitWeekDotUi(
                    label = dot.label,
                    isScheduled = dot.isScheduled,
                    isCompleted = dot.isCompleted,
                    isToday = dot.date == today,
                )
            },
            onClick = onEdit,
        )

        HabitSectionHeader(title = "Monthly progress")
        MonthlyHabitProgressCard(habits = listOf(habit), today = today)

        KuroPrimaryButton(
            text = if (doneToday) "Completed today ✓" else "Mark done today",
            onClick = onToggleToday,
            modifier = Modifier.fillMaxWidth(),
        )
        DetailSecondaryButton(
            label = if (focusCta == HabitFocusCtaState.OpenFocus) "Open focus" else "Focus with Kuro",
            enabled = focusCta != HabitFocusCtaState.Blocked,
            onClick = {
                when (focusCta) {
                    HabitFocusCtaState.StartFocus -> onStartFocus()
                    HabitFocusCtaState.OpenFocus -> onOpenFocus()
                    HabitFocusCtaState.Blocked -> Unit
                }
            },
        )
        HabitDetailHelperRow(
            message = when (focusCta) {
                HabitFocusCtaState.OpenFocus ->
                    "This Pomodoro is in progress — tap to jump back to it."

                HabitFocusCtaState.Blocked ->
                    "A Pomodoro is already running. Finish or stop it to focus on this habit."

                HabitFocusCtaState.StartFocus ->
                    "Focus a Pomodoro on this habit when you're ready."
            },
        )
        DetailDeleteButton(onDelete = onDelete)
    }
}

/** Three logical states for the habit detail focus CTA, mirroring Tasks. */
private enum class HabitFocusCtaState { StartFocus, OpenFocus, Blocked }

/** Cat-face helper row that explains the focus CTA state (mirrors Tasks). */
@Composable
private fun HabitDetailHelperRow(message: String) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = KuroSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Image(
            painter = painterResource(id = R.drawable.cat_face),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
        )
        Text(
            text = message,
            color = tokens.textSecondary,
            style = KuroType.bodySecondary,
            modifier = Modifier.weight(1f),
        )
    }
}


@Composable
private fun HabitDetailSheetHeader(onEdit: () -> Unit, onClose: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(KuroSpacing.radiusPill))
                .background(tokens.segmentedTrack)
                .clickable(onClick = onEdit)
                .padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.sm)
                .semantics { role = Role.Button; contentDescription = "Edit habit" },
            contentAlignment = Alignment.Center,
        ) {
            Text("Edit", color = tokens.textPrimary, style = KuroType.labelStrong)
        }
        Box(
            modifier = Modifier
                .size(KuroSpacing.xxl)
                .clip(CircleShape)
                .background(tokens.segmentedTrack)
                .clickable(onClick = onClose)
                .semantics { role = Role.Button; contentDescription = "Close" },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✕", color = tokens.textSecondary, style = KuroType.labelStrong)
        }
    }
}

/** Back + title row shown above the inline editor when editing within the sheet. */
@Composable
private fun HabitEditSubpageHeader(onBack: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(KuroSpacing.xxl)
                .clip(CircleShape)
                .background(tokens.segmentedTrack)
                .clickable(onClick = onBack)
                .semantics { role = Role.Button; contentDescription = "Back" },
            contentAlignment = Alignment.Center,
        ) {
            Text("‹", color = tokens.textPrimary, style = KuroType.fabGlyph)
        }
        Text("Edit habit", color = tokens.textPrimary, style = KuroType.headingCard)
    }
}

@Composable
private fun HabitStatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(KuroSpacing.radiusLg))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusLg))
            .padding(KuroSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
    ) {
        Text(value, color = tokens.accent, style = KuroType.habitStreakValue)
        Text(label, color = tokens.textSecondary, style = KuroType.labelMeta)
    }
}

@Composable
private fun DetailSecondaryButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val borderColor = if (enabled) tokens.cardBorder else tokens.cardBorder.copy(alpha = 0.5f)
    val textColor = if (enabled) tokens.textPrimary else tokens.textFaint
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.md))
            .border(1.dp, borderColor, RoundedCornerShape(KuroSpacing.md))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = KuroSpacing.md)
            .semantics { role = Role.Button; contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = textColor, style = KuroType.labelStrong)
    }
}

@Composable
private fun DetailDeleteButton(onDelete: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var showConfirm by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.md))
            .border(1.dp, tokens.destructive.copy(alpha = 0.4f), RoundedCornerShape(KuroSpacing.md))
            .clickable { showConfirm = true }
            .padding(vertical = KuroSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Delete Habit",
            color = tokens.destructive.copy(alpha = 0.85f),
            style = KuroType.labelStrong,
        )
    }
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete habit?", color = tokens.textPrimary, style = KuroType.headingCard) },
            text = { Text("All logs and streak data will be permanently removed.", color = tokens.textSecondary) },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onDelete() }) {
                    Text("Delete", color = tokens.destructive, style = KuroType.labelStrong)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel", color = tokens.textSecondary)
                }
            },
            containerColor = tokens.cardFill,
            shape = RoundedCornerShape(KuroSpacing.radiusMd),
        )
    }
}
