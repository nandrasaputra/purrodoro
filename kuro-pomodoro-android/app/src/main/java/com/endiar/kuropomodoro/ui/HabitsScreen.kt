package com.endiar.kuropomodoro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.endiar.kuropomodoro.ui.components.ScreenStickyBar
import com.endiar.kuropomodoro.ui.theme.KuroSurfaceTokens
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ReferenceCream = Color(0xFFF9F5F0)    // near-white BG
private val ReferenceCard = Color(0xFFFFFFFF)      // pure white cards
private val ReferenceCardWarm = Color(0xFFFFF8F2)  // very light warm inner surfaces
private val ReferenceBorder = Color(0xFFEDE0D4)
private val ReferenceTrack = Color(0xFFF5EEE8)     // near-invisible filter track
private val ReferenceInk = Color(0xFF201714)
private val ReferenceMuted = Color(0xFF786B64)
private val ReferenceFaint = Color(0xFFA99B92)
private val ReferenceCoral = Color(0xFFC96F51)
private val ReferenceLeaf = Color(0xFF8BA478)

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ReferenceCream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp)
                .padding(top = 10.dp, bottom = 100.dp)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ReferenceHabitHeader(
                modifier = Modifier.onSizeChanged { headerHeightPx = it.height },
                onAdd = { showAddSheet = true },
            )

            HabitFilterTabs(
                selectedFilter = selectedHabitFilter,
                onSelected = { selectedHabitFilter = it },
            )

            Text(
                text = sectionTitle,
                color = ReferenceInk,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )

            if (visibleHabits.isEmpty()) {
                ReferenceEmptyHabitCard(filter = selectedHabitFilter, onAdd = { showAddSheet = true })
            } else {
                visibleHabits.forEach { habit ->
                    val isScheduledToday = habit.isScheduledOn(today)
                    ReferenceHabitCard(
                        habit = habit,
                        today = today,
                        isScheduledToday = isScheduledToday,
                        onEdit = { editingHabit = habit },
                        onToggle = { onToggleBinary(habit.id, today) },
                        onIncrement = { onIncrementProgress(habit.id, today, 1) },
                        onStartPomodoro = { onStartPomodoroForHabit(habit) },
                    )
                }
            }

            MonthlyHabitProgressCard(habits = habits, today = today)
            HabitMotivationCard()
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

// ─── Reference-style habit screen ─────────────────────────────────────────────

@Composable
private fun ReferenceHabitHeader(
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LeafBadge()
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Habits",
                    color = ReferenceInk,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Small daily actions, big change.",
                    color = ReferenceMuted,
                    fontSize = 13.sp,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ReferenceCoral)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun LeafBadge() {
    // Flat leaf icon — no circle container, just the leaf shape on background
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(13.dp)
                .height(24.dp)
                .rotate(-32f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp))
                .background(ReferenceLeaf),
        )
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(21.dp)
                .padding(top = 5.dp)
                .rotate(34f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp))
                .background(ReferenceLeaf.copy(alpha = 0.74f)),
        )
    }
}

@Composable
private fun HabitFilterTabs(
    selectedFilter: HabitUiFilter,
    onSelected: (HabitUiFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(ReferenceTrack)
            .border(1.dp, ReferenceBorder, RoundedCornerShape(28.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HabitUiFilter.entries.forEach { filter ->
            val selected = filter == selectedFilter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (selected) ReferenceCard else Color.Transparent)
                    .clickable { onSelected(filter) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = filter.label,
                    color = if (selected) ReferenceCoral else ReferenceMuted,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ReferenceEmptyHabitCard(
    filter: HabitUiFilter,
    onAdd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(ReferenceCard)
            .border(1.dp, ReferenceBorder, RoundedCornerShape(30.dp))
            .clickable(onClick = onAdd)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LeafBadge()
        Text(
            text = if (filter == HabitUiFilter.All) "No habits yet" else "No ${filter.label.lowercase()} habits yet",
            color = ReferenceInk,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Add a small repeatable action and let Kuro keep you company.",
            color = ReferenceMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(ReferenceCoral)
                .padding(horizontal = 22.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Add habit", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReferenceHabitCard(
    habit: Habit,
    today: LocalDate,
    isScheduledToday: Boolean,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onIncrement: () -> Unit,
    onStartPomodoro: () -> Unit,
) {
    val accent = Color(habit.colorArgb.toInt())
    val streak = habit.currentStreak(today)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ReferenceCard)
            .border(1.dp, ReferenceBorder, RoundedCornerShape(24.dp))
            .clickable(onClick = onEdit)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── Top row: emoji + name/subtitle + streak ───────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = habit.emoji, fontSize = 24.sp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = habit.name,
                        color = ReferenceInk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = habitTargetScheduleLabel(habit, today),
                        color = ReferenceMuted,
                        fontSize = 12.sp,
                    )
                }
            }
            // Streak — right aligned: number + "day streak" / "week streak"
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = streak.toString(),
                    color = ReferenceCoral,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = habitStreakUnitLabel(habit),
                    color = ReferenceFaint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // ── Week dots row ─────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            habitWeekDots(habit, today).forEach { dot ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(dot.label, color = ReferenceFaint, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    dot.isCompleted -> ReferenceLeaf          // green for completed
                                    dot.isScheduled -> ReferenceLeaf.copy(alpha = 0.25f)
                                    else -> ReferenceBorder.copy(alpha = 0.6f)
                                },
                            )
                            .border(
                                1.dp,
                                if (dot.date == today) ReferenceInk.copy(alpha = 0.20f) else Color.Transparent,
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

/** Returns "day streak" or "week streak" depending on occurrence. */
private fun habitStreakUnitLabel(habit: Habit): String = when (habit.occurrence) {
    is HabitOccurrence.Daily -> "day streak"
    is HabitOccurrence.Weekdays -> "week streak"
}

@Composable
private fun MonthlyHabitProgressCard(habits: List<Habit>, today: LocalDate) {
    val dots = monthlyHabitProgressDots(habits, today)
    val monthLabel = today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    // Monday-first: DayOfWeek.MONDAY.value=1 → offset 0, SUNDAY.value=7 → offset 6
    val leadingBlanks = dots.firstOrNull()?.date?.dayOfWeek?.let { (it.value - 1) % 7 } ?: 0
    val gridSlots = List(leadingBlanks) { null } + dots
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ReferenceCard)
            .border(1.dp, ReferenceBorder, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("Monthly Progress", color = ReferenceInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(monthLabel, color = ReferenceMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        // Monday-first day headers
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    text = day,
                    color = ReferenceFaint,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
        gridSlots.chunked(7).forEach { rowDots ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                rowDots.forEach { dot ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        dot == null -> Color.Transparent
                                        dot.isCompleted -> ReferenceLeaf        // green for completed
                                        dot.date == today -> ReferenceLeaf.copy(alpha = 0.4f)
                                        else -> ReferenceBorder
                                    },
                                ),
                        )
                    }
                }
                repeat(7 - rowDots.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HabitMotivationCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ReferenceCardWarm)
            .border(1.dp, ReferenceBorder, RoundedCornerShape(24.dp))
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Real cat asset
        Image(
            painter = painterResource(id = com.endiar.kuropomodoro.R.drawable.cat_face),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            contentScale = ContentScale.Fit,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Consistency is the secret behind all progress.",
                color = ReferenceInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "You're doing great! 🌟",
                color = ReferenceCoral,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
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
