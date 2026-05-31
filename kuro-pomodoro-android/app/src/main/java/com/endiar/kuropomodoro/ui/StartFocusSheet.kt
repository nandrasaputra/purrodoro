package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.ChartColumn
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Coffee
import com.composables.icons.lucide.Leaf
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.NotebookPen
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Square
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.R
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.FocusType
import com.endiar.kuropomodoro.model.Habit
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.effectiveTarget
import com.endiar.kuropomodoro.model.isScheduledOn
import com.endiar.kuropomodoro.model.isTargetMetOn
import com.endiar.kuropomodoro.model.progressOn
import com.endiar.kuropomodoro.ui.components.FocusAlreadyCompleteDialog
import com.endiar.kuropomodoro.ui.components.KuroCard
import com.endiar.kuropomodoro.ui.designsystem.KuroAdaptiveBottomSheet
import com.endiar.kuropomodoro.ui.designsystem.KuroBottomSheetSubpageHeader
import com.endiar.kuropomodoro.ui.designsystem.KuroInputField
import com.endiar.kuropomodoro.ui.designsystem.KuroSelectionList
import com.endiar.kuropomodoro.ui.designsystem.KuroSpacing
import com.endiar.kuropomodoro.ui.designsystem.KuroStepper
import com.endiar.kuropomodoro.ui.designsystem.KuroType
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import java.time.LocalDate

enum class FocusIntent(val label: String) {
    Habit("Habit"),
    Task("Task"),
    SimpleNote("Simple note"),
    Empty("Empty"),
}

data class StartFocusRequest(
    val task: FocusTask?,
    val habitId: Int? = null,
    val settings: TimerSettings,
    val focusType: FocusType = FocusType.Empty,
)

private enum class StartFocusSheetPage {
    Confirm,
    EditConfig,
    PickTask,
    PickHabit,
}

sealed class StartFocusPrefill {
    /**
     * Optional one-shot override for this session's focus length, in minutes.
     * Used by the Home "continue interrupted focus" path to carry the leftover
     * time into the confirmation sheet, where the user can still adjust it.
     */
    abstract val focusMinutesOverride: Int?
    abstract val focusSessionsOverride: Int?

    data class Task(
        val taskId: Int,
        override val focusMinutesOverride: Int? = null,
        override val focusSessionsOverride: Int? = null,
    ) : StartFocusPrefill()

    data class Habit(
        val habitId: Int,
        override val focusMinutesOverride: Int? = null,
        override val focusSessionsOverride: Int? = null,
    ) : StartFocusPrefill()
}

private data class StartFocusPickerSnapshot(
    val intent: FocusIntent,
    val selectedTaskId: Int,
    val selectedHabitId: Int,
)

@Composable
fun StartFocusSheet(
    settings: TimerSettings,
    tasks: List<FocusTask>,
    habits: List<Habit>,
    today: LocalDate,
    onDismiss: () -> Unit,
    onStartFocus: (StartFocusRequest) -> Unit,
    prefill: StartFocusPrefill? = null,
) {
    val initialIntent = when (prefill) {
        is StartFocusPrefill.Task -> FocusIntent.Task
        is StartFocusPrefill.Habit -> FocusIntent.Habit
        null -> FocusIntent.Empty
    }
    val initialTaskId = (prefill as? StartFocusPrefill.Task)?.taskId ?: -1
    val initialHabitId = (prefill as? StartFocusPrefill.Habit)?.habitId ?: -1

    // A prefill may carry a leftover focus length (continuing an interrupted
    // session). Seed the per-session settings with it so the confirmation card
    // shows the adjusted time up front; the user can still edit it from there.
    val initialSettings = remember(settings, prefill) {
        val withFocusMinutes = prefill?.focusMinutesOverride
            ?.coerceIn(1, 120)
            ?.let { settings.copy(focusMinutes = it) }
            ?: settings
        prefill?.focusSessionsOverride
            ?.let { withFocusMinutes.updateCycleSize(it) }
            ?: withFocusMinutes
    }

    var page by rememberSaveable { mutableStateOf(StartFocusSheetPage.Confirm) }
    var sessionSettings by remember(initialSettings) { mutableStateOf(initialSettings) }
    var sessionSettingsBeforeEdit by remember { mutableStateOf(initialSettings) }

    var intent by rememberSaveable { mutableStateOf(initialIntent) }
    var noteText by rememberSaveable { mutableStateOf("") }
    var selectedTaskId by rememberSaveable { mutableIntStateOf(initialTaskId) }
    var selectedHabitId by rememberSaveable { mutableIntStateOf(initialHabitId) }
    var pickerDraftId by rememberSaveable { mutableIntStateOf(-1) }
    var pickerSnapshot by remember { mutableStateOf<StartFocusPickerSnapshot?>(null) }
    // Holds a built request that's pending the "this is already complete" prompt.
    // Non-null while the confirmation dialog is on screen; confirming forwards it
    // to onStartFocus untouched, so the task/habit keeps its done state and only
    // the Pomodoro itself is recorded.
    var pendingCompletedRequest by remember { mutableStateOf<StartFocusRequest?>(null) }

    val incompleteTasks = tasks.filter { !it.completed }
    val todayHabits = habits.filter { it.isScheduledOn(today) }

    val sheetContentKey = page

    fun resetTaskAndHabitSelections() {
        selectedTaskId = -1
        selectedHabitId = -1
    }

    fun restorePickerSnapshot() {
        pickerSnapshot?.let { snapshot ->
            intent = snapshot.intent
            selectedTaskId = snapshot.selectedTaskId
            selectedHabitId = snapshot.selectedHabitId
        }
        pickerSnapshot = null
    }

    fun openTaskPicker() {
        pickerSnapshot = StartFocusPickerSnapshot(intent, selectedTaskId, selectedHabitId)
        selectedHabitId = -1
        if (intent != FocusIntent.Task) {
            selectedTaskId = -1
        }
        pickerDraftId = selectedTaskId
        page = StartFocusSheetPage.PickTask
    }

    fun openHabitPicker() {
        pickerSnapshot = StartFocusPickerSnapshot(intent, selectedTaskId, selectedHabitId)
        selectedTaskId = -1
        if (intent != FocusIntent.Habit) {
            selectedHabitId = -1
        }
        pickerDraftId = selectedHabitId
        page = StartFocusSheetPage.PickHabit
    }

    fun onIntentClick(option: FocusIntent) {
        when (option) {
            FocusIntent.Task -> openTaskPicker()
            FocusIntent.Habit -> openHabitPicker()
            FocusIntent.Empty, FocusIntent.SimpleNote -> {
                resetTaskAndHabitSelections()
                intent = option
            }
        }
    }

    // Completed tasks are allowed through (started via the task detail CTA); the
    // confirmation dialog below is what gates them, not a disabled button.
    fun canStart(): Boolean = when (intent) {
        FocusIntent.Empty -> true
        FocusIntent.SimpleNote -> noteText.trim().isNotEmpty()
        FocusIntent.Task -> selectedTaskId >= 0 && tasks.any { it.id == selectedTaskId }
        FocusIntent.Habit -> selectedHabitId >= 0 && todayHabits.any { it.id == selectedHabitId }
    }

    // Whether the currently-selected intent points at an item that's already done
    // (a completed task, or a habit whose target is met today). Used to show the
    // "focus on it anyway?" confirmation before starting.
    fun selectedAlreadyComplete(): Boolean = when (intent) {
        FocusIntent.Task -> tasks.firstOrNull { it.id == selectedTaskId }?.completed == true
        FocusIntent.Habit -> habits.firstOrNull { it.id == selectedHabitId }?.isTargetMetOn(today) == true
        FocusIntent.Empty, FocusIntent.SimpleNote -> false
    }

    KuroAdaptiveBottomSheet(
        onDismiss = onDismiss,
        contentKey = sheetContentKey,
        footer = if (page == StartFocusSheetPage.Confirm) {
            {
                StartFocusConfirmButton(enabled = canStart(), onClick = {
                    val request = buildStartFocusRequest(
                        intent = intent,
                        noteText = noteText,
                        selectedTaskId = selectedTaskId,
                        selectedHabitId = selectedHabitId,
                        tasks = tasks,
                        todayHabits = todayHabits,
                        today = today,
                        settings = sessionSettings,
                    )
                    if (selectedAlreadyComplete()) {
                        pendingCompletedRequest = request
                    } else {
                        onStartFocus(request)
                    }
                })
            }
        } else {
            null
        },
    ) {
        when (page) {
            StartFocusSheetPage.Confirm -> {
                StartFocusConfirmContent(
                    intent = intent,
                    noteText = noteText,
                    selectedTaskId = selectedTaskId,
                    selectedHabitId = selectedHabitId,
                    tasks = tasks,
                    todayHabits = todayHabits,
                    sessionSettings = sessionSettings,
                    onIntentClick = ::onIntentClick,
                    onNoteChange = { noteText = it },
                    onEditConfig = {
                        sessionSettingsBeforeEdit = sessionSettings
                        page = StartFocusSheetPage.EditConfig
                    },
                )
            }

            StartFocusSheetPage.PickTask -> {
                StartFocusPickerContent(
                    title = "Choose a task",
                    emptyMessage = "No open tasks yet. Add one from the Tasks tab.",
                    options = incompleteTasks.map { it.id to it.title },
                    draftSelectedId = pickerDraftId,
                    onDraftSelect = { pickerDraftId = it },
                    onBack = {
                        restorePickerSnapshot()
                        page = StartFocusSheetPage.Confirm
                    },
                    onConfirm = {
                        selectedTaskId = pickerDraftId
                        intent = FocusIntent.Task
                        pickerSnapshot = null
                        page = StartFocusSheetPage.Confirm
                    },
                )
            }

            StartFocusSheetPage.PickHabit -> {
                StartFocusPickerContent(
                    title = "Choose a habit",
                    emptyMessage = "No habits scheduled for today.",
                    options = todayHabits.map { it.id to "${it.emoji} ${it.name}" },
                    draftSelectedId = pickerDraftId,
                    onDraftSelect = { pickerDraftId = it },
                    onBack = {
                        restorePickerSnapshot()
                        page = StartFocusSheetPage.Confirm
                    },
                    onConfirm = {
                        selectedHabitId = pickerDraftId
                        intent = FocusIntent.Habit
                        pickerSnapshot = null
                        page = StartFocusSheetPage.Confirm
                    },
                )
            }

            StartFocusSheetPage.EditConfig -> {
                StartFocusConfigEditorContent(
                    settings = sessionSettings,
                    onSettingsChange = { sessionSettings = it },
                    onBack = {
                        sessionSettings = sessionSettingsBeforeEdit
                        page = StartFocusSheetPage.Confirm
                    },
                    onDone = { page = StartFocusSheetPage.Confirm },
                )
            }
        }
    }

    pendingCompletedRequest?.let { request ->
        FocusAlreadyCompleteDialog(
            isHabit = intent == FocusIntent.Habit,
            onConfirm = {
                pendingCompletedRequest = null
                onStartFocus(request)
            },
            onDismiss = { pendingCompletedRequest = null },
        )
    }
}

private fun buildStartFocusRequest(
    intent: FocusIntent,
    noteText: String,
    selectedTaskId: Int,
    selectedHabitId: Int,
    tasks: List<FocusTask>,
    todayHabits: List<Habit>,
    today: LocalDate,
    settings: TimerSettings,
): StartFocusRequest = when (intent) {
    FocusIntent.Empty -> StartFocusRequest(
        task = null,
        settings = settings,
        focusType = FocusType.Empty,
    )
    FocusIntent.SimpleNote -> StartFocusRequest(
        task = FocusTask(id = -1, title = noteText.trim(), minutes = 0),
        settings = settings,
        focusType = FocusType.SimpleNote,
    )
    FocusIntent.Task -> StartFocusRequest(
        task = tasks.first { it.id == selectedTaskId },
        settings = settings,
        focusType = FocusType.Task,
    )
    FocusIntent.Habit -> {
        val habit = todayHabits.first { it.id == selectedHabitId }
        val remaining = habit.effectiveTarget() - habit.progressOn(today)
        val displaySessions = remaining.coerceAtLeast(1)
        StartFocusRequest(
            task = FocusTask(
                id = -habit.id,
                title = "${habit.emoji} ${habit.name}",
                minutes = 0,
                plannedSessions = displaySessions,
                completedSessions = habit.progressOn(today),
            ),
            habitId = habit.id,
            settings = settings,
            focusType = FocusType.Habit,
        )
    }
}

@Composable
private fun StartFocusConfirmContent(
    intent: FocusIntent,
    noteText: String,
    selectedTaskId: Int,
    selectedHabitId: Int,
    tasks: List<FocusTask>,
    todayHabits: List<Habit>,
    sessionSettings: TimerSettings,
    onIntentClick: (FocusIntent) -> Unit,
    onNoteChange: (String) -> Unit,
    onEditConfig: () -> Unit,
) {
    Image(
        painter = painterResource(id = R.drawable.cat_home_sleeping),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .height(120.dp)
            .padding(top = KuroSpacing.sm),
    )
    Spacer(Modifier.height(KuroSpacing.md))
    Text(
        text = "Start Focus",
        style = KuroType.headingCard,
        color = MaterialTheme.colorScheme.kuroTokens.textPrimary,
    )
    Spacer(Modifier.height(KuroSpacing.xs))
    Text(
        text = "What are you working on?",
        style = KuroType.bodySecondary,
        color = MaterialTheme.colorScheme.kuroTokens.textSecondary,
    )
    Spacer(Modifier.height(KuroSpacing.xl))

    IntentSection(selected = intent, onSelect = onIntentClick)

    when (intent) {
        FocusIntent.SimpleNote -> {
            Spacer(Modifier.height(KuroSpacing.md))
            KuroInputField(
                value = noteText,
                onValueChange = onNoteChange,
                placeholder = "Write a quick note…",
            )
        }

        FocusIntent.Task -> {
            tasks.firstOrNull { it.id == selectedTaskId }?.let { task ->
                Spacer(Modifier.height(KuroSpacing.md))
                SelectedIntentSummary(
                    label = task.title,
                    onChange = { onIntentClick(FocusIntent.Task) },
                )
            }
        }

        FocusIntent.Habit -> {
            todayHabits.firstOrNull { it.id == selectedHabitId }?.let { habit ->
                Spacer(Modifier.height(KuroSpacing.md))
                SelectedIntentSummary(
                    label = "${habit.emoji} ${habit.name}",
                    onChange = { onIntentClick(FocusIntent.Habit) },
                )
            }
        }

        FocusIntent.Empty -> Unit
    }

    Spacer(Modifier.height(KuroSpacing.xl))
    DefaultConfigSection(
        settings = sessionSettings,
        onEdit = onEditConfig,
    )
}

@Composable
private fun StartFocusPickerContent(
    title: String,
    emptyMessage: String,
    options: List<Pair<Int, String>>,
    draftSelectedId: Int,
    onDraftSelect: (Int) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    val canConfirm = draftSelectedId >= 0 && options.any { it.first == draftSelectedId }

    KuroBottomSheetSubpageHeader(
        title = title,
        onBack = onBack,
        actionLabel = "OK",
        onAction = onConfirm,
        actionEnabled = canConfirm,
    )
    KuroSelectionList(
        emptyMessage = emptyMessage,
        options = options,
        selectedId = draftSelectedId,
        onSelect = onDraftSelect,
    )
}

@Composable
private fun SelectedIntentSummary(
    label: String,
    onChange: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(tokens.accentSoft)
            .border(1.dp, tokens.accent.copy(alpha = 0.35f), RoundedCornerShape(KuroSpacing.radiusMd))
            .clickable(onClick = onChange)
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = KuroType.bodyPrimary,
            color = tokens.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Change",
            style = KuroType.labelStrong,
            color = tokens.accent,
        )
    }
}

@Composable
private fun StartFocusConfigEditorContent(
    settings: TimerSettings,
    onSettingsChange: (TimerSettings) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    KuroBottomSheetSubpageHeader(
        title = "Edit config",
        onBack = onBack,
        actionLabel = "Done",
        onAction = onDone,
    )

    Text(
        text = "Changes apply to this session only.",
        style = KuroType.bodySecondary,
        color = tokens.textSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = KuroSpacing.lg),
    )

    KuroCard(modifier = Modifier.fillMaxWidth()) {
        SessionConfigStepperRow(
            icon = Lucide.Clock,
            label = "Focus",
            value = settings.focusMinutes,
            unit = "min",
            min = 1,
            max = 120,
            onDecrement = {
                if (settings.focusMinutes > 1) {
                    onSettingsChange(settings.copy(focusMinutes = settings.focusMinutes - 1))
                }
            },
            onIncrement = {
                if (settings.focusMinutes < 120) {
                    onSettingsChange(settings.copy(focusMinutes = settings.focusMinutes + 1))
                }
            },
        )
        SessionConfigDivider()
        SessionConfigStepperRow(
            icon = Lucide.Coffee,
            label = "Short break",
            value = settings.shortBreakMinutes,
            unit = "min",
            min = 1,
            max = 120,
            onDecrement = {
                if (settings.shortBreakMinutes > 1) {
                    onSettingsChange(settings.copy(shortBreakMinutes = settings.shortBreakMinutes - 1))
                }
            },
            onIncrement = {
                if (settings.shortBreakMinutes < 120) {
                    onSettingsChange(settings.copy(shortBreakMinutes = settings.shortBreakMinutes + 1))
                }
            },
        )
        SessionConfigDivider()
        SessionConfigStepperRow(
            icon = Lucide.Coffee,
            label = "Long break",
            value = settings.longBreakMinutes,
            unit = "min",
            min = 1,
            max = 120,
            onDecrement = {
                if (settings.longBreakMinutes > 1) {
                    onSettingsChange(settings.copy(longBreakMinutes = settings.longBreakMinutes - 1))
                }
            },
            onIncrement = {
                if (settings.longBreakMinutes < 120) {
                    onSettingsChange(settings.copy(longBreakMinutes = settings.longBreakMinutes + 1))
                }
            },
        )
        SessionConfigDivider()
        SessionConfigStepperRow(
            icon = Lucide.ChartColumn,
            label = "Sessions",
            value = settings.focusSessionsPerCycle,
            unit = "",
            min = 1,
            max = TimerSettings.MAX_FOCUS_SESSIONS_PER_CYCLE,
            onDecrement = {
                if (settings.focusSessionsPerCycle > 1) {
                    onSettingsChange(settings.copy(focusSessionsPerCycle = settings.focusSessionsPerCycle - 1))
                }
            },
            onIncrement = {
                if (settings.focusSessionsPerCycle < TimerSettings.MAX_FOCUS_SESSIONS_PER_CYCLE) {
                    onSettingsChange(settings.copy(focusSessionsPerCycle = settings.focusSessionsPerCycle + 1))
                }
            },
        )
    }
}

@Composable
private fun SessionConfigDivider() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KuroSpacing.md)
            .height(1.dp)
            .background(tokens.divider),
    )
}

@Composable
private fun SessionConfigStepperRow(
    icon: ImageVector,
    label: String,
    value: Int,
    unit: String,
    min: Int,
    max: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val valueLabel = if (unit.isBlank()) value.toString() else "$value $unit"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tokens.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(KuroSpacing.sm))
        Text(
            text = label,
            style = KuroType.bodyPrimary,
            color = tokens.textPrimary,
            modifier = Modifier.weight(1f),
        )
        KuroStepper(label = "−", enabled = value > min, onClick = onDecrement)
        Text(
            text = valueLabel,
            style = KuroType.labelStrong,
            color = tokens.textPrimary,
            modifier = Modifier.padding(horizontal = KuroSpacing.sm),
        )
        KuroStepper(label = "+", enabled = value < max, onClick = onIncrement)
    }
}

@Composable
private fun IntentSection(
    selected: FocusIntent,
    onSelect: (FocusIntent) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Text(
            text = "Intent (optional)",
            style = KuroType.labelStrong,
            color = tokens.textPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            FocusIntent.entries.forEach { option ->
                IntentChip(
                    label = option.label,
                    icon = intentIcon(option),
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun IntentChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val fill = if (selected) tokens.accentSoft else tokens.pageBackground
    val borderColor = if (selected) tokens.accent.copy(alpha = 0.35f) else tokens.cardBorder

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KuroSpacing.radiusMd))
                .background(fill)
                .border(1.dp, borderColor, RoundedCornerShape(KuroSpacing.radiusMd))
                .clickable(onClick = onClick)
                .padding(horizontal = KuroSpacing.xs, vertical = KuroSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) tokens.accent else tokens.textSecondary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = KuroType.labelMeta,
                color = if (selected) tokens.accent else tokens.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(tokens.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp),
                )
            }
        }
    }
}

@Composable
private fun DefaultConfigSection(
    settings: TimerSettings,
    onEdit: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Default config",
                style = KuroType.labelStrong,
                color = tokens.textPrimary,
            )
            Text(
                text = "Edit",
                style = KuroType.labelStrong,
                color = tokens.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(KuroSpacing.radiusSm))
                    .clickable(onClick = onEdit)
                    .padding(horizontal = KuroSpacing.xs, vertical = KuroSpacing.xs),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KuroSpacing.radiusMd))
                .background(tokens.pageBackground)
                .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusMd)),
        ) {
            ConfigGridRow {
                ConfigCell(
                    icon = Lucide.Clock,
                    label = "Focus",
                    value = "${settings.focusMinutes} min",
                    modifier = Modifier.weight(1f),
                )
                ConfigGridDividerVertical()
                ConfigCell(
                    icon = Lucide.Coffee,
                    label = "Short break",
                    value = "${settings.shortBreakMinutes} min",
                    modifier = Modifier.weight(1f),
                )
            }
            ConfigGridDividerHorizontal()
            ConfigGridRow {
                ConfigCell(
                    icon = Lucide.Coffee,
                    label = "Long break",
                    value = "${settings.longBreakMinutes} min",
                    modifier = Modifier.weight(1f),
                )
                ConfigGridDividerVertical()
                ConfigCell(
                    icon = Lucide.ChartColumn,
                    label = "Sessions",
                    value = settings.focusSessionsPerCycle.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ConfigGridRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun ConfigGridDividerVertical() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = KuroSpacing.md)
            .width(1.dp)
            .background(tokens.cardBorder.copy(alpha = 0.75f)),
    )
}

@Composable
private fun ConfigGridDividerHorizontal() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KuroSpacing.md)
            .height(1.dp)
            .background(tokens.cardBorder.copy(alpha = 0.75f)),
    )
}

@Composable
private fun ConfigCell(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier.padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tokens.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = KuroType.labelMeta,
                color = tokens.textSecondary,
            )
            Text(
                text = value,
                style = KuroType.labelStrong,
                color = tokens.textPrimary,
            )
        }
    }
}

@Composable
private fun StartFocusConfirmButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(if (enabled) tokens.accent else tokens.accent.copy(alpha = 0.45f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = KuroSpacing.xl),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Lucide.Play,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(KuroSpacing.sm))
        Text(
            text = "Start Focus",
            style = KuroType.bodyPrimary.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold),
            color = Color.White,
        )
    }
}

private fun intentIcon(intent: FocusIntent): ImageVector = when (intent) {
    FocusIntent.Habit -> Lucide.Leaf
    FocusIntent.Task -> Lucide.CircleCheck
    FocusIntent.SimpleNote -> Lucide.NotebookPen
    FocusIntent.Empty -> Lucide.Square
}
