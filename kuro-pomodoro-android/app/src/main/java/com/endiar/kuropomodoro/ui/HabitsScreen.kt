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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.model.Habit
import com.endiar.kuropomodoro.model.HabitOccurrence
import com.endiar.kuropomodoro.model.HabitTargetType
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.model.TimeBlock
import com.endiar.kuropomodoro.model.currentStreak
import com.endiar.kuropomodoro.model.effectiveTarget
import com.endiar.kuropomodoro.model.isScheduledOn
import com.endiar.kuropomodoro.ui.components.KuroPrimaryButton
import com.endiar.kuropomodoro.ui.designsystem.KuroBottomSheet
import com.endiar.kuropomodoro.ui.designsystem.KuroHabitCard
import com.endiar.kuropomodoro.ui.designsystem.KuroHabitMonthDotUi
import com.endiar.kuropomodoro.ui.designsystem.KuroHabitMotivationCard
import com.endiar.kuropomodoro.ui.designsystem.KuroHabitsEmptyState
import com.endiar.kuropomodoro.ui.designsystem.KuroHabitWeekDotUi
import com.endiar.kuropomodoro.ui.designsystem.KuroInputField
import com.endiar.kuropomodoro.ui.designsystem.KuroMonthlyHabitProgressCard
import com.endiar.kuropomodoro.ui.designsystem.KuroPill
import com.endiar.kuropomodoro.ui.designsystem.KuroPillKind
import com.endiar.kuropomodoro.ui.designsystem.KuroScreenScaffold
import com.endiar.kuropomodoro.ui.designsystem.KuroSectionHeader
import com.endiar.kuropomodoro.ui.designsystem.KuroSegmentedControl
import com.endiar.kuropomodoro.ui.designsystem.KuroSpacing
import com.endiar.kuropomodoro.ui.designsystem.KuroStepper
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

    var selectedHabitFilter by rememberSaveable { mutableStateOf(HabitUiFilter.All) }
    val visibleHabits = filterHabitsForUi(habits, selectedHabitFilter)
    val sectionTitle = when (selectedHabitFilter) {
        HabitUiFilter.All -> "Daily Habits"
        HabitUiFilter.Daily -> "Daily Habits"
        HabitUiFilter.Weekly -> "Weekly Habits"
        HabitUiFilter.Custom -> "Custom Habits"
    }

    var showAddSheet by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }

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

            KuroSegmentedControl(
                options = HabitUiFilter.entries.map { it.label },
                selectedIndex = HabitUiFilter.entries.indexOf(selectedHabitFilter),
                onSelect = { selectedHabitFilter = HabitUiFilter.entries[it] },
            )

            KuroSectionHeader(title = sectionTitle)

            if (visibleHabits.isEmpty()) {
                EmptyHabitCard(filter = selectedHabitFilter, onAdd = { showAddSheet = true })
            } else {
                visibleHabits.forEach { habit ->
                    KuroHabitCard(
                        name = habit.name,
                        emoji = habit.emoji,
                        accentColor = Color(habit.colorArgb.toInt()),
                        scheduleLabel = habitTargetScheduleLabel(habit, today),
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
                        onClick = { editingHabit = habit },
                    )
                }
            }

            MonthlyHabitProgressCard(habits = habits, today = today)
            KuroHabitMotivationCard(
                quote = "Consistency is the secret behind all progress.",
                encouragement = "You're doing great! 🌟",
            )
        }
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

    editingHabit?.let { habit ->
        KuroBottomSheet(title = "Edit habit", onDismiss = { editingHabit = null }) {
            HabitEditorSheet(
                initial = habit,
                onConfirm = { draft -> onUpdateHabit(habit.id, draft); editingHabit = null },
                onDelete = { onDeleteHabit(habit.id); editingHabit = null },
                onDismiss = { editingHabit = null },
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LeafBadge()
            Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2)) {
                Text(
                    text = "Habits",
                    color = tokens.textPrimary,
                    style = KuroType.headingDisplay,
                )
                Text(
                    text = "Small daily actions, big change.",
                    color = tokens.textSecondary,
                    style = KuroType.bodySecondary,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(KuroSpacing.xxxl + KuroSpacing.sm)
                .clip(CircleShape)
                .background(tokens.accent)
                .clickable(onClick = onAdd),
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
    filter: HabitUiFilter,
    onAdd: () -> Unit,
) {
    KuroHabitsEmptyState(
        title = if (filter == HabitUiFilter.All) "No habits yet" else "No ${filter.label.lowercase()} habits yet",
        message = "Add a small repeatable action and let Kuro keep you company.",
        onAdd = onAdd,
        leading = { LeafBadge() },
    )
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

private fun habitTargetScheduleLabel(habit: Habit, today: LocalDate): String {
    val target = when (habit.targetType) {
        HabitTargetType.Binary -> "Check off"
        HabitTargetType.Quantity -> "${habit.effectiveTarget()} times"
        HabitTargetType.Pomodoro -> "${habit.effectiveTarget()} pomodoros"
    }
    return "$target - ${nextScheduledLabel(habit, today)}"
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
    var targetType by remember { mutableStateOf(initial?.targetType ?: HabitTargetType.Binary) }
    var targetValue by remember { mutableIntStateOf(initial?.targetValue ?: 1) }
    var occurrence by remember { mutableStateOf(initial?.occurrence ?: HabitOccurrence.Daily) }

    val isDaily = occurrence == HabitOccurrence.Daily
    val weekdaySet = (occurrence as? HabitOccurrence.Weekdays)?.days ?: emptySet()
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

        EditorLabel("TYPE")
        Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
            listOf(
                HabitTargetType.Binary to "Check off",
                HabitTargetType.Quantity to "Count",
                HabitTargetType.Pomodoro to "Pomodoro",
            ).forEach { (type, label) ->
                EditorPill(label = label, selected = targetType == type) { targetType = type }
            }
        }

        if (targetType != HabitTargetType.Binary) {
            val unit = if (targetType == HabitTargetType.Pomodoro) "sessions" else "times"
            EditorLabel("TARGET")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                KuroStepper(label = "−", enabled = targetValue > 1, large = true, onClick = { if (targetValue > 1) targetValue-- })
                Spacer(Modifier.width(KuroSpacing.lg))
                Text("$targetValue $unit", color = tokens.textPrimary, style = KuroType.stepperValue)
                Spacer(Modifier.width(KuroSpacing.lg))
                KuroStepper(label = "+", enabled = targetValue < 20, large = true, onClick = { if (targetValue < 20) targetValue++ })
            }
        }

        EditorLabel("SCHEDULE")
        Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
            EditorPill("Every day", isDaily) { occurrence = HabitOccurrence.Daily }
            EditorPill("Custom days", !isDaily) {
                if (isDaily) occurrence = HabitOccurrence.Weekdays(setOf(DayOfWeek.MONDAY))
            }
        }
        if (!isDaily) {
            Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                DayOfWeek.entries.forEach { dow ->
                    val selected = dow in weekdaySet
                    EditorPill(
                        label = dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        selected = selected,
                    ) {
                        val newSet = if (selected) weekdaySet - dow else weekdaySet + dow
                        occurrence = if (newSet.isEmpty()) HabitOccurrence.Daily
                        else HabitOccurrence.Weekdays(newSet)
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
                        targetType = targetType,
                        targetValue = targetValue.coerceAtLeast(1),
                        occurrence = occurrence,
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
        AlertDialog(
            onDismissRequest = { showEmojiPicker = false },
            title = { Text("Choose icon", color = tokens.textPrimary, style = KuroType.headingCard) },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
                ) {
                    items(HABIT_EMOJIS) { e ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(KuroSpacing.sm))
                                .background(if (e == emoji) tokens.accent.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (e == emoji) tokens.accent.copy(alpha = 0.5f) else Color.Transparent,
                                    RoundedCornerShape(KuroSpacing.sm),
                                )
                                .clickable { emoji = e; showEmojiPicker = false },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(e, style = KuroType.metricValue, textAlign = TextAlign.Center)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = tokens.cardFill,
            shape = RoundedCornerShape(KuroSpacing.radiusMd),
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

@Composable
private fun EditorPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.clickable(onClick = onClick)) {
        KuroPill(label = label, kind = if (selected) KuroPillKind.Selected else KuroPillKind.Default)
    }
}
