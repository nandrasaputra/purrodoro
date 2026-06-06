package com.endiar.purrodoro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Flag
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.SquarePen
import com.composables.icons.lucide.Tag
import com.composables.icons.lucide.Trash2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.purrodoro.R
import com.endiar.purrodoro.model.Category
import com.endiar.purrodoro.model.FocusTask
import com.endiar.purrodoro.model.GlassTreatment
import com.endiar.purrodoro.model.Habit
import com.endiar.purrodoro.model.TaskPriority
import com.endiar.purrodoro.model.TimerSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.endiar.purrodoro.ui.components.FocusAlreadyCompleteDialog
import com.endiar.purrodoro.ui.components.FocusBusyDialog
import com.endiar.purrodoro.ui.components.KuroPrimaryButton
import com.endiar.purrodoro.ui.designsystem.KuroBottomSheet
import com.endiar.purrodoro.ui.designsystem.KuroBottomSheetSubpageHeader
import com.endiar.purrodoro.ui.designsystem.KuroSheetHeader
import com.endiar.purrodoro.ui.designsystem.KuroCompletedTaskRow
import com.endiar.purrodoro.ui.designsystem.KuroInputField
import com.endiar.purrodoro.ui.designsystem.KuroScreenScaffold
import com.endiar.purrodoro.ui.designsystem.KuroSegmentedControl
import com.endiar.purrodoro.ui.designsystem.KuroSpacing
import com.endiar.purrodoro.ui.designsystem.StaggeredEntrance
import com.endiar.purrodoro.ui.designsystem.KuroTaskCard
import com.endiar.purrodoro.ui.designsystem.KuroTasksEmptyState
import com.endiar.purrodoro.ui.designsystem.KuroType
import com.endiar.purrodoro.ui.designsystem.categoryColorChoices
import com.endiar.purrodoro.ui.designsystem.categoryColorFor
import com.endiar.purrodoro.ui.theme.kuroTokens
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
/**
 * Tasks screen — decoupled from Pomodoro after the redesign.
 *
 * Tasks are pure todos here: title, category, done / not done. The Pomodoro
 * cycle still attaches to a task and auto-completes it on a successful finish
 * (see TimerStateMachine), but the row UI doesn't carry per-task pomodoro
 * targets, dot rows, or `X / Y` counters. Per-row primary actions are:
 *  - Toggle completion via the leading checkbox.
 *  - Tap the row body to open the detail sheet.
 *  - Swipe right to start a Pomodoro with this task attached.
 *  - Swipe left to delete (with snackbar undo wiring left to the parent host).
 *
 * [liveTaskId] is the id of the task currently attached to a running cycle,
 * threaded down so [KuroTaskCard] can paint a quiet accent ring around the
 * row's color dot. Any non-positive id (synthetic habit task) intentionally
 * never matches a real task, so the live indicator stays on real tasks only.
 */
@Composable
private fun taskPriorityLabel(priority: TaskPriority): String = when (priority) {
    TaskPriority.High -> stringResource(R.string.tasks_priority_high)
    TaskPriority.Normal -> stringResource(R.string.tasks_priority_normal)
    TaskPriority.Low -> stringResource(R.string.tasks_priority_low)
}

@Composable
private fun taskPriorityDescription(priority: TaskPriority): String = when (priority) {
    TaskPriority.High -> stringResource(R.string.tasks_priority_high_desc)
    TaskPriority.Normal -> stringResource(R.string.tasks_priority_normal_desc)
    TaskPriority.Low -> stringResource(R.string.tasks_priority_low_desc)
}

@Composable
private fun taskFilterLabel(filter: TaskUiFilter): String = when (filter) {
    TaskUiFilter.All -> stringResource(R.string.tasks_filter_all)
    TaskUiFilter.Active -> stringResource(R.string.tasks_filter_active)
    TaskUiFilter.Done -> stringResource(R.string.tasks_filter_done)
    TaskUiFilter.Archived -> stringResource(R.string.tasks_filter_archived)
}

/** Localized variant of [heroDueLabel] for the Next Focus hero chip. */
@Composable
private fun rememberHeroDueLabel(dueDate: LocalDate?, today: LocalDate): String? {
    dueDate ?: return null
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate).toInt()
    return when {
        days == 0 -> stringResource(R.string.tasks_due_today_hero)
        days == 1 -> stringResource(R.string.tasks_due_tomorrow_hero)
        days < 0 -> {
            val ago = -days
            if (ago == 1) stringResource(R.string.tasks_overdue_one)
            else stringResource(R.string.tasks_overdue_many, ago)
        }
        else -> stringResource(R.string.tasks_due_in_days, days)
    }
}

/** Localized compact value for the due-date list row. */
@Composable
private fun dueDateRowValueLabel(dueDate: LocalDate?): String {
    if (dueDate == null) return stringResource(R.string.tasks_due_not_set)
    val today = LocalDate.now()
    return when (dueDate) {
        today -> stringResource(
            R.string.tasks_due_today,
            dueDate.format(DateTimeFormatter.ofPattern("MMM d")),
        )
        today.plusDays(1) -> stringResource(R.string.tasks_due_tomorrow)
        else -> dueDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    tasks: List<FocusTask>,
    categories: List<Category>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit,
    onAddTask: (String, Int?, TaskPriority, LocalDate?) -> Unit,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onUpdateTask: (FocusTask) -> Unit,
    onDeleteAllCompleted: (Int?) -> Unit,
    onAddCategory: (String, Long) -> Unit,
    onRenameCategory: (Int, String, Long) -> Unit,
    onDeleteCategory: (Int) -> Unit,
    onFocusTask: (StartFocusRequest) -> Unit,
    habits: List<Habit> = emptyList(),
    settings: TimerSettings = TimerSettings(),
    modifier: Modifier = Modifier,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    overBackdrop: Boolean = true,
    liveTaskId: Int? = null,
    // Fired when the user taps the hero's "Open" action while a cycle is
    // already running. The parent should switch the visible tab to Home so
    // the user can see / control the live timer, rather than re-opening the
    // StartFocusSheet which would imply configuring a new session.
    onSwitchToHome: () -> Unit = {},
    // True while any Pomodoro is running or paused (including habit-based
    // sessions where liveTaskId is null). When set, attempts to start a new
    // focus on a non-live task short-circuit into a "busy" dialog instead
    // of silently replacing the current session.
    isFocusActive: Boolean = false,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    // Driven by the FAB: when true, the AddTaskSheet bottom sheet is shown
    // over the Tasks list. The sheet owns its own input state so dismissing
    // it discards the in-progress draft.
    var addTaskSheetVisible by rememberSaveable { mutableStateOf(false) }
    var addTaskSheetCategoryId by rememberSaveable { mutableStateOf(selectedCategoryId) }
    var focusSheetTask by remember { mutableStateOf<FocusTask?>(null) }
    var detailSheetTask by remember { mutableStateOf<FocusTask?>(null) }
    // Default to All so opening Tasks shows the full picture (active + done)
    // at a glance; users opt into Active / Done explicitly.
    var selectedTaskFilter by rememberSaveable { mutableStateOf(TaskUiFilter.All) }
    var deleteCategoryTarget by remember { mutableStateOf<Category?>(null) }
    var addCategorySheetVisible by rememberSaveable { mutableStateOf(false) }
    var renameCategoryTarget by remember { mutableStateOf<Category?>(null) }
    var deleteAllCompletedVisible by rememberSaveable { mutableStateOf(false) }
    var categoryFilterSheetVisible by rememberSaveable { mutableStateOf(false) }
    // Shown when the user tries to start a focus on a non-live task while
    // another Pomodoro is already running or paused.
    var focusBusyDialogVisible by remember { mutableStateOf(false) }

    // Central gate: any "I want to start a focus on this task" intent
    // (hero pill, row swipe, detail sheet CTA) goes through here so we
    // never silently replace a running Pomodoro.
    //   - No active focus           → open the StartFocusSheet as usual.
    //   - Active focus, same task   → switch to Home so the user sees the
    //                                 timer (the task is already attached).
    //   - Active focus, other task  → show the "busy" dialog.
    val attemptStartFocus: (FocusTask) -> Unit = { task ->
        when {
            !isFocusActive -> focusSheetTask = task
            task.id == liveTaskId -> onSwitchToHome()
            else -> focusBusyDialogVisible = true
        }
    }

    val today = remember { LocalDate.now() }

    val categoryFilteredTasks = tasks.filter { task ->
        selectedCategoryId == null || task.categoryId == selectedCategoryId
    }
    val visibleTasks = filterTasksForUi(categoryFilteredTasks, selectedTaskFilter)
    // Active rows share the hero's ordering: due-date urgency → priority →
    // newest added first (see sortedActiveTasks).
    val filteredActiveTasks = sortedActiveTasks(visibleTasks, today)
    val filteredCompletedTasks = visibleTasks.filter { it.completed }
    val totalCount = categoryFilteredTasks.size
    val activeCount = categoryFilteredTasks.count { !it.completed }
    val doneCount = categoryFilteredTasks.count { it.completed }

    // Next-focus hero only appears on Active / All. Hide on Done so the hero
    // doesn't push the actual done list down for no reason.
    val nextFocus = remember(categoryFilteredTasks, today, selectedTaskFilter, liveTaskId) {
        if (selectedTaskFilter == TaskUiFilter.Done) return@remember null
        // If a real task is currently attached to a running cycle, use that as
        // the hero — that's the most accurate "what Purrodoro is focusing on" answer.
        liveTaskId?.let { id -> categoryFilteredTasks.firstOrNull { it.id == id && !it.completed } }
            ?: nextFocusTask(categoryFilteredTasks, today)
    }
    // The hero task is intentionally NOT removed from the list — it appears both
    // as the Next Focus card and as a normal row below, so it never feels like
    // it "went missing" from the list when promoted to the hero.
    val activeRowsForList = filteredActiveTasks

    val keyboardController = LocalSoftwareKeyboardController.current

    KuroScreenScaffold(
        stickyTitle = stringResource(R.string.tasks_title),
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
                TasksScreenHeader(
                    onAdd = {
                        addTaskSheetCategoryId = selectedCategoryId
                        addTaskSheetVisible = true
                    },
                )
            }

            FilterBar(
                statusOptions = listOf(
                    StatusFilterOption(TaskUiFilter.All, totalCount),
                    StatusFilterOption(TaskUiFilter.Active, activeCount),
                    StatusFilterOption(TaskUiFilter.Done, doneCount),
                ),
                selectedStatus = selectedTaskFilter,
                onSelectStatus = { selectedTaskFilter = it },
                selectedCategory = categories.firstOrNull { it.id == selectedCategoryId },
                onOpenCategorySheet = { categoryFilterSheetVisible = true },
            )

            nextFocus?.let { hero ->
                val heroIsLive = hero.id == liveTaskId
                val heroCategory = categories.firstOrNull { it.id == hero.categoryId }
                NextFocusHeroCard(
                    task = hero,
                    today = today,
                    categoryName = heroCategory?.name,
                    categoryColor = heroCategory?.let { categoryColorFor(it, tokens.textMuted) },
                    focusMinutes = settings.focusMinutes,
                    isLive = heroIsLive,
                    // Mute the Start pill when another focus is running on a
                    // different attachment (eg. a habit session). Tapping
                    // still works — it surfaces the busy dialog so the user
                    // understands why nothing is starting.
                    isStartBlocked = isFocusActive && !heroIsLive,
                    onOpenDetail = { detailSheetTask = hero },
                    onStartFocus = { attemptStartFocus(hero) },
                    onOpenFocus = onSwitchToHome,
                    onToggleComplete = { onToggleTask(hero.id) },
                )
            }

            if (visibleTasks.isEmpty()) {
                TasksEmptyState(
                    filter = selectedTaskFilter,
                    hasAnyTask = totalCount > 0,
                    selectedCategoryName = categories.firstOrNull { it.id == selectedCategoryId }?.name,
                )
            }

            // Active section. Grouped in its own column so the header hugs its
            // rows (tight 8dp rhythm within the group) while the outer column's
            // 16dp gap separates it from the hero / done section above and below.
            // Header reads "More tasks" when a hero is shown above (rows can be a
            // mix of dates / no-date / overdue), else a flat "Tasks".
            if (activeRowsForList.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                    if (selectedTaskFilter != TaskUiFilter.Done) {
                        TasksSectionHeader(
                            title = if (nextFocus != null) stringResource(R.string.tasks_section_more)
                            else stringResource(R.string.tasks_section_tasks)
                        )
                    }
                    activeRowsForList.forEachIndexed { index, task ->
                        // Key by task identity, not list position. These rows hold
                        // per-row animation state (the checkbox "stamp"), and this
                        // is a non-lazy Column — without a stable key, completing a
                        // task shifts the remaining rows up into the departed slot
                        // and they inherit its latched "completed" check state.
                        key(task.id) {
                            val rowCategory = categories.firstOrNull { it.id == task.categoryId }
                            StaggeredEntrance(index = index) {
                                SwipeableTaskCard(
                                    task = task,
                                    today = today,
                                    // When filtered to a single category, hide the per-row
                                    // subtitle (it would be the same word on every row).
                                    categoryName = rowCategory
                                        ?.name
                                        ?.takeIf { selectedCategoryId == null },
                                    categoryColor = rowCategory?.let {
                                        categoryColorFor(it, tokens.textMuted)
                                    },
                                    isLive = task.id == liveTaskId,
                                    onToggleTask = onToggleTask,
                                    onTapRow = { detailSheetTask = it },
                                )
                            }
                        }
                    }
                }
            }

            if (filteredCompletedTasks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                    TasksSectionHeader(
                        // Hide the "Done · N" label when the whole screen is
                        // already the Done filter; keep it only on All where it
                        // disambiguates from the active rows above.
                        title = stringResource(R.string.tasks_section_done, filteredCompletedTasks.size)
                            .takeIf { selectedTaskFilter == TaskUiFilter.All },
                        action = {
                            val deleteAllCd = stringResource(R.string.tasks_delete_all_cd)
                            Text(
                                text = stringResource(R.string.tasks_delete_all),
                                color = tokens.destructive,
                                style = KuroType.labelMeta,
                                modifier = Modifier
                                    .clickable { deleteAllCompletedVisible = true }
                                    .padding(horizontal = KuroSpacing.xs, vertical = KuroSpacing.xs)
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = deleteAllCd
                                    },
                            )
                        },
                    )
                    filteredCompletedTasks.forEach { task ->
                        key(task.id) {
                            val rowCategory = categories.firstOrNull { it.id == task.categoryId }
                            SwipeToDeleteCompletedTaskRow(
                                task = task,
                                categoryName = rowCategory
                                    ?.name
                                    ?.takeIf { selectedCategoryId == null },
                                categoryColor = rowCategory?.let {
                                    categoryColorFor(it, tokens.textMuted)
                                },
                                onToggleTask = onToggleTask,
                                onTapRow = { detailSheetTask = it },
                            )
                        }
                    }
                }
            }
        }

    }

    if (addTaskSheetVisible) {
        AddTaskSheet(
            categories = categories,
            defaultCategoryId = addTaskSheetCategoryId,
            onDismiss = {
                addTaskSheetVisible = false
                keyboardController?.hide()
            },
            onAdd = { title, categoryId, priority, dueDate ->
                onAddTask(title, categoryId, priority, dueDate)
                addTaskSheetVisible = false
                keyboardController?.hide()
            },
            onAddCategory = { addCategorySheetVisible = true },
        )
    }

    detailSheetTask?.let { task ->
        TaskDetailSheet(
            task = task,
            categories = categories,
            isFocusActive = isFocusActive,
            isLive = task.id == liveTaskId,
            onDismiss = { detailSheetTask = null },
            onUpdate = onUpdateTask,
            onDelete = {
                onDeleteTask(task.id)
                detailSheetTask = null
            },
            onFocusTask = { updatedTask ->
                detailSheetTask = null
                attemptStartFocus(updatedTask)
            },
            onAddCategory = { addCategorySheetVisible = true },
        )
    }

    if (focusBusyDialogVisible) {
        FocusBusyDialog(
            onOpenFocus = {
                focusBusyDialogVisible = false
                onSwitchToHome()
            },
            onDismiss = { focusBusyDialogVisible = false },
        )
    }

    focusSheetTask?.let { task ->
        key(task.id) {
            StartFocusSheet(
                settings = settings,
                tasks = tasks,
                habits = habits,
                today = java.time.LocalDate.now(),
                prefill = StartFocusPrefill.Task(task.id),
                onDismiss = { focusSheetTask = null },
                onStartFocus = { request ->
                    focusSheetTask = null
                    onFocusTask(request)
                },
            )
        }
    }

    deleteCategoryTarget?.let { category ->
        val dialogTokens = MaterialTheme.colorScheme.kuroTokens
        AlertDialog(
            onDismissRequest = { deleteCategoryTarget = null },
            title = {
                Text(
                    text = stringResource(R.string.tasks_delete_category_title, category.name),
                    color = dialogTokens.textPrimary,
                    style = KuroType.headingCard,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.tasks_delete_category_body),
                    color = dialogTokens.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCategory(category.id)
                    if (selectedCategoryId == category.id) onCategorySelected(null)
                    deleteCategoryTarget = null
                }) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = dialogTokens.destructive,
                        style = KuroType.labelStrong,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCategoryTarget = null }) {
                    Text(stringResource(R.string.common_cancel), color = dialogTokens.textSecondary)
                }
            },
            containerColor = dialogTokens.cardFill,
            shape = RoundedCornerShape(KuroSpacing.radiusMd)
        )
    }

    if (deleteAllCompletedVisible) {
        val dialogTokens = MaterialTheme.colorScheme.kuroTokens
        val confirmCount = filteredCompletedTasks.size
        AlertDialog(
            onDismissRequest = { deleteAllCompletedVisible = false },
            title = {
                Text(
                    text = if (confirmCount == 1) {
                        stringResource(R.string.tasks_delete_completed_title, confirmCount)
                    } else {
                        stringResource(R.string.tasks_delete_completed_title_plural, confirmCount)
                    },
                    color = dialogTokens.textPrimary,
                    style = KuroType.headingCard,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.tasks_delete_completed_body),
                    color = dialogTokens.textSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAllCompleted(selectedCategoryId)
                    deleteAllCompletedVisible = false
                }) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = dialogTokens.destructive,
                        style = KuroType.labelStrong,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteAllCompletedVisible = false }) {
                    Text(stringResource(R.string.common_cancel), color = dialogTokens.textSecondary)
                }
            },
            containerColor = dialogTokens.cardFill,
            shape = RoundedCornerShape(KuroSpacing.radiusMd),
        )
    }

    if (addCategorySheetVisible) {
        CategoryNameSheet(
            title = stringResource(R.string.tasks_category_new_title),
            initialName = "",
            initialColor = categoryColorChoices.first(),
            onDismiss = { addCategorySheetVisible = false },
            onConfirm = { name, color ->
                onAddCategory(name, color)
                addCategorySheetVisible = false
            }
        )
    }

    renameCategoryTarget?.let { category ->
        CategoryNameSheet(
            title = stringResource(R.string.tasks_category_rename_title),
            initialName = category.name,
            initialColor = category.colorArgb
                ?: (categoryColorFor(category, tokens.textMuted).toArgb().toLong() and 0xFFFFFFFFL),
            onDismiss = { renameCategoryTarget = null },
            onConfirm = { name, color ->
                onRenameCategory(category.id, name, color)
                renameCategoryTarget = null
            }
        )
    }

    if (categoryFilterSheetVisible) {
        CategoryFilterSheet(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            allCount = tasks.size,
            countFor = { id -> tasks.count { it.categoryId == id } },
            onSelectCategory = {
                onCategorySelected(it)
                categoryFilterSheetVisible = false
            },
            onAddCategory = {
                categoryFilterSheetVisible = false
                addCategorySheetVisible = true
            },
            onRenameCategory = {
                categoryFilterSheetVisible = false
                renameCategoryTarget = it
            },
            onDeleteCategory = {
                categoryFilterSheetVisible = false
                deleteCategoryTarget = it
            },
            onDismiss = { categoryFilterSheetVisible = false },
        )
    }
}

// ─── Header ────────────────────────────────────────────────────────────────

@Composable
private fun TasksScreenHeader(
    onAdd: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Title only — no leading mascot glyph, matching the shared top-bar
        // pattern (see HabitsScreen). The sticky bar shown on scroll already
        // echoes this title.
        Text(
            text = stringResource(R.string.tasks_title),
            color = tokens.textPrimary,
            style = KuroType.headingDisplay,
        )
        // Primary add action lives top-right (replacing the old bottom FAB),
        // consistent with the Habits header's add button.
        val addTaskCd = stringResource(R.string.tasks_add)
        Box(
            modifier = Modifier
                .size(KuroSpacing.xxxl + KuroSpacing.sm)
                .clip(CircleShape)
                .background(tokens.accent)
                .clickable(onClick = onAdd)
                .semantics { role = Role.Button; contentDescription = addTaskCd },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "+", color = tokens.accentOn, style = KuroType.fabGlyph)
        }
    }
}

// ─── Filter bar ────────────────────────────────────────────────────────────

private data class StatusFilterOption(
    val filter: TaskUiFilter,
    val count: Int,
)

@Composable
private fun FilterBar(
    statusOptions: List<StatusFilterOption>,
    selectedStatus: TaskUiFilter,
    onSelectStatus: (TaskUiFilter) -> Unit,
    selectedCategory: Category?,
    onOpenCategorySheet: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        KuroSegmentedControl(
            options = statusOptions.map {
                stringResource(R.string.tasks_filter_option, taskFilterLabel(it.filter), it.count)
            },
            selectedIndex = statusOptions.indexOfFirst { it.filter == selectedStatus }
                .coerceAtLeast(0),
            onSelect = { idx -> onSelectStatus(statusOptions[idx].filter) },
            modifier = Modifier.weight(1f),
        )

        CategoryFilterPill(
            selectedCategory = selectedCategory,
            onClick = onOpenCategorySheet,
        )
    }
}

/**
 * Pill that opens the category filter bottom sheet. Mirrors the previous
 * dropdown trigger (color dot + name/"All" + caret) but now routes to the
 * full-surface sheet picker instead of a cramped popover menu.
 */
@Composable
private fun CategoryFilterPill(
    selectedCategory: Category?,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val filterCategoryCd = stringResource(R.string.tasks_filter_category_cd)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusPill))
            .clickable(onClick = onClick)
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm)
            .semantics { role = Role.Button; contentDescription = filterCategoryCd },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
    ) {
        // Color preview only when a real category is selected; "All" stays
        // text-only so it's clearly the broadest scope.
        if (selectedCategory != null) {
            Box(
                modifier = Modifier
                    .size(KuroSpacing.taskCategoryDot)
                    .clip(CircleShape)
                    .background(categoryColorFor(selectedCategory, tokens.textMuted)),
            )
        }
        Text(
            text = selectedCategory?.name ?: stringResource(R.string.tasks_category_all),
            color = tokens.textPrimary,
            style = KuroType.labelStrong,
        )
        Text(
            text = "▾",
            color = tokens.textSecondary,
            style = KuroType.labelMeta,
        )
    }
}

// ─── Category filter sheet (Variation 1) ───────────────────────────────────

private enum class CategoryFilterPage { Filter, Manage }

/**
 * Bottom-sheet category filter.
 *
 * Replaces the old floating dropdown that crammed filtering, selecting,
 * renaming, deleting and adding into a tiny popover. Splits into two pages on
 * one branded surface:
 *  - Filter: pick "All categories" or a specific category (color dot + task
 *    count, selected row highlighted with a check). A "Manage categories"
 *    button routes to the second page.
 *  - Manage: rename / delete each category and add new ones. These actions
 *    open their own sheet/dialog, so the filter sheet dismisses first to avoid
 *    stacking surfaces.
 */
@Composable
private fun CategoryFilterSheet(
    categories: List<Category>,
    selectedCategoryId: Int?,
    allCount: Int,
    countFor: (Int) -> Int,
    onSelectCategory: (Int?) -> Unit,
    onAddCategory: () -> Unit,
    onRenameCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var page by rememberSaveable { mutableStateOf(CategoryFilterPage.Filter) }
    val manageCategoriesCd = stringResource(R.string.tasks_manage_categories_cd)
    val newCategoryCd = stringResource(R.string.tasks_new_category)

    KuroBottomSheet(title = null, onDismiss = onDismiss, contentKey = page) {
        when (page) {
            CategoryFilterPage.Filter -> {
                Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.md)) {
                    KuroSheetHeader(title = stringResource(R.string.tasks_filter_by_category))

                    Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                        CategoryFilterRow(
                            selected = selectedCategoryId == null,
                            dotColor = tokens.textMuted,
                            hollow = true,
                            label = stringResource(R.string.tasks_all_categories),
                            count = allCount,
                            onClick = { onSelectCategory(null) },
                        )
                        categories.forEach { category ->
                            CategoryFilterRow(
                                selected = selectedCategoryId == category.id,
                                dotColor = categoryColorFor(category, tokens.textMuted),
                                hollow = false,
                                label = category.name,
                                count = countFor(category.id),
                                onClick = { onSelectCategory(category.id) },
                            )
                        }
                    }

                    // Secondary, navigational treatment (not coral): coral is
                    // reserved for primary forward actions (Start / Add), and
                    // this just routes to a management sub-page.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
                            .background(tokens.cardFill)
                            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusPill))
                            .clickable { page = CategoryFilterPage.Manage }
                            .semantics { role = Role.Button; contentDescription = manageCategoriesCd }
                            .padding(horizontal = KuroSpacing.lg, vertical = KuroSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.tasks_manage_categories),
                            color = tokens.textPrimary,
                            style = KuroType.labelStrong,
                        )
                        Spacer(Modifier.width(KuroSpacing.xs))
                        Icon(
                            imageVector = Lucide.ChevronRight,
                            contentDescription = null,
                            tint = tokens.textMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            CategoryFilterPage.Manage -> {
                KuroBottomSheetSubpageHeader(
                    title = stringResource(R.string.tasks_manage_categories),
                    onBack = { page = CategoryFilterPage.Filter },
                    actionLabel = stringResource(R.string.common_done),
                    onAction = { page = CategoryFilterPage.Filter },
                )
                Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                    if (categories.isEmpty()) {
                        Text(
                            text = stringResource(R.string.tasks_no_categories),
                            color = tokens.textSecondary,
                            style = KuroType.bodySecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = KuroSpacing.sm),
                        )
                    }
                    categories.forEach { category ->
                        ManageCategoryRow(
                            category = category,
                            onRename = { onRenameCategory(category) },
                            onDelete = { onDeleteCategory(category) },
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
                            .clickable(onClick = onAddCategory)
                            .semantics { role = Role.Button; contentDescription = newCategoryCd }
                            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
                    ) {
                        Icon(
                            imageVector = Lucide.Plus,
                            contentDescription = null,
                            tint = tokens.accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.tasks_new_category),
                            color = tokens.accent,
                            style = KuroType.labelStrong,
                        )
                    }
                }
            }
        }
    }
}

/** Selectable category row for the filter page: dot, name, task count, check. */
@Composable
private fun CategoryFilterRow(
    selected: Boolean,
    dotColor: Color,
    hollow: Boolean,
    label: String,
    count: Int,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(if (selected) tokens.accentSoft else tokens.cardFill)
            .border(
                1.dp,
                if (selected) tokens.accent.copy(alpha = 0.35f) else tokens.cardBorder,
                RoundedCornerShape(KuroSpacing.radiusMd),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        CategoryDot(color = dotColor, hollow = hollow)
        Text(
            text = label,
            style = KuroType.bodyPrimary,
            color = tokens.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = KuroType.labelMeta,
            color = tokens.textMuted,
        )
        if (selected) {
            Icon(
                imageVector = Lucide.Check,
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Manage-page row: color dot, name, and Rename / Delete text actions. */
@Composable
private fun ManageCategoryRow(
    category: Category,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusMd))
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        CategoryDot(
            color = categoryColorFor(category, tokens.textMuted),
            hollow = false,
        )
        Text(
            text = category.name,
            style = KuroType.bodyPrimary,
            color = tokens.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        CategoryActionIcon(
            icon = Lucide.SquarePen,
            tint = tokens.textSecondary,
            contentDescription = stringResource(R.string.tasks_rename_cd, category.name),
            onClick = onRename,
        )
        CategoryActionIcon(
            icon = Lucide.Trash2,
            tint = tokens.destructive,
            contentDescription = stringResource(R.string.tasks_delete_cd, category.name),
            onClick = onDelete,
        )
    }
}

/** Round, tappable icon button used for the per-category manage actions. */
@Composable
private fun CategoryActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(KuroSpacing.xxl)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ─── Section header ────────────────────────────────────────────────────────

/**
 * Single section-header treatment shared by every list section on the Tasks
 * screen ("More tasks" / "Tasks" / "Done · N"). Flush to the screen gutter —
 * the parent column already applies the horizontal padding, so this header adds
 * none of its own (the old `KuroSectionHeader` double-indented it). Title is
 * optional so a section can show only a trailing action and stay right-aligned.
 */
@Composable
private fun TasksSectionHeader(
    title: String?,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (title != null) {
            Text(text = title, style = KuroType.labelStrong, color = tokens.textMuted)
        } else {
            // Keep the action pinned right when there's no title.
            Spacer(Modifier.width(KuroSpacing.xs))
        }
        if (action != null) action()
    }
}

// ─── Task sheet pages ──────────────────────────────────────────────────────

/**
 * Sub-pages shared by the add-task and task-detail bottom sheets. The main
 * page hosts the task properties; tapping the Category or Priority row swaps
 * the sheet content to the matching picker (see [KuroAdaptiveBottomSheet]'s
 * `contentKey` animation), keeping everything on one bottom-sheet surface.
 */
private enum class TaskSheetPage { Main, CategoryPicker, PriorityPicker }

/** Max length for a task title, enforced on both the add and edit fields. */
private const val TaskTitleMaxLength = 50

// ─── Add task bottom sheet ─────────────────────────────────────────────────

/**
 * Bottom sheet for capturing a new task. Replaces the older inline add card
 * triggered by the FAB.
 *
 * Layout mirrors the Variation 1 reference mockup:
 *  - Title row: "New task" heading + circular close affordance.
 *  - Labelled title input that auto-focuses (keyboard opens immediately).
 *  - Category chips row (reuses [AddCategoryChoice]) with a trailing "+ New"
 *    accent chip routed back to the category-create sheet.
 *  - Priority + Due date rows that reuse the same components as the detail
 *    sheet, so the visual rhythm is consistent across "create" and "edit".
 *  - Coral primary "Add task" button at the bottom; disabled until the title
 *    has non-whitespace text.
 *
 * The sheet owns its own draft state and dismissing it (close X, scrim tap,
 * back press) discards the draft.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddTaskSheet(
    categories: List<Category>,
    defaultCategoryId: Int?,
    onDismiss: () -> Unit,
    onAdd: (title: String, categoryId: Int?, priority: TaskPriority, dueDate: LocalDate?) -> Unit,
    onAddCategory: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf(defaultCategoryId) }
    var priority by rememberSaveable { mutableStateOf(TaskPriority.Normal) }
    var dueDate by rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf(TaskSheetPage.Main) }

    val titleFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { titleFocusRequester.requestFocus() }

    val canSubmit = title.trim().isNotEmpty()
    val submit: () -> Unit = {
        val trimmed = title.trim()
        if (trimmed.isNotEmpty()) {
            onAdd(trimmed, categoryId, priority, dueDate)
        }
    }

    KuroBottomSheet(title = null, onDismiss = onDismiss, contentKey = page) {
        when (page) {
            TaskSheetPage.Main -> {
                Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.lg)) {
                    KuroSheetHeader(title = stringResource(R.string.tasks_new_task))

                    AddTaskTitleField(
                        title = title,
                        onTitleChange = { title = it.take(TaskTitleMaxLength) },
                        focusRequester = titleFocusRequester,
                        onSubmitIme = { if (canSubmit) submit() },
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                        CategoryListRow(
                            categories = categories,
                            selectedId = categoryId,
                            onClick = { page = TaskSheetPage.CategoryPicker },
                        )
                        PriorityListRow(
                            selected = priority,
                            onClick = { page = TaskSheetPage.PriorityPicker },
                        )
                        DueDateListRow(
                            dueDate = dueDate,
                            onPick = { showDatePicker = true },
                            onClear = { dueDate = null },
                        )
                    }

                    AddTaskPrimaryButton(enabled = canSubmit, onClick = submit)
                }
            }

            TaskSheetPage.CategoryPicker -> {
                CategoryPickerPage(
                    categories = categories,
                    selectedId = categoryId,
                    onSelect = {
                        categoryId = it
                        page = TaskSheetPage.Main
                    },
                    onAddCategory = onAddCategory,
                    onBack = { page = TaskSheetPage.Main },
                )
            }

            TaskSheetPage.PriorityPicker -> {
                PriorityPickerPage(
                    selected = priority,
                    onSelect = {
                        priority = it
                        page = TaskSheetPage.Main
                    },
                    onBack = { page = TaskSheetPage.Main },
                )
            }
        }
    }

    if (showDatePicker) {
        DueDatePickerDialog(
            initial = dueDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { picked ->
                dueDate = picked
                showDatePicker = false
            },
        )
    }
}

@Composable
internal fun AddTaskTitleField(
    title: String,
    onTitleChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onSubmitIme: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.tasks_what_focus),
                color = tokens.textSecondary,
                style = KuroType.labelMeta,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.tasks_char_count, title.length, TaskTitleMaxLength),
                color = if (title.length >= TaskTitleMaxLength) tokens.accent else tokens.textMuted,
                style = KuroType.labelMeta,
            )
        }
        KuroInputField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = stringResource(R.string.tasks_title_placeholder),
            modifier = Modifier.focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmitIme() }),
            // Let long titles wrap onto multiple lines instead of scrolling
            // horizontally. ImeAction.Done still submits (no newline inserted).
            singleLine = false,
        )
    }
}

/**
 * Coral pill primary button for the add-task sheet. Patterned after
 * [FocusWithKuroButton] so the two main calls-to-action across task surfaces
 * (Add task / Focus with Purrodoro) share the same shape, height, and motion.
 */
@Composable
internal fun AddTaskPrimaryButton(enabled: Boolean, onClick: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val fill = if (enabled) tokens.accent else tokens.accent.copy(alpha = 0.35f)
    val contentColor = if (enabled) tokens.accentOn else tokens.accentOn.copy(alpha = 0.7f)
    val addTaskLabel = stringResource(R.string.tasks_add)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(fill)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = addTaskLabel
            }
            .padding(horizontal = KuroSpacing.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Lucide.Plus,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(KuroSpacing.sm))
        Text(
            text = addTaskLabel,
            color = contentColor,
            style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.ExtraBold),
        )
    }
}

// ─── Empty states ──────────────────────────────────────────────────────────

@Composable
private fun TasksEmptyState(
    filter: TaskUiFilter,
    hasAnyTask: Boolean,
    selectedCategoryName: String?,
) {
    val title: String
    val message: String
    when {
        !hasAnyTask -> {
            title = stringResource(R.string.tasks_empty_none_title)
            message = stringResource(R.string.tasks_empty_none_msg)
        }
        filter == TaskUiFilter.Active -> {
            title = stringResource(R.string.tasks_empty_active_title)
            message = if (selectedCategoryName != null) {
                stringResource(R.string.tasks_empty_active_msg_category, selectedCategoryName)
            } else {
                stringResource(R.string.tasks_empty_active_msg)
            }
        }
        filter == TaskUiFilter.Done -> {
            title = stringResource(R.string.tasks_empty_done_title)
            message = stringResource(R.string.tasks_empty_done_msg)
        }
        filter == TaskUiFilter.Archived -> {
            title = stringResource(R.string.tasks_empty_archived_title)
            message = stringResource(R.string.tasks_empty_archived_msg)
        }
        else -> {
            title = stringResource(R.string.tasks_empty_default_title)
            message = stringResource(R.string.tasks_empty_default_msg)
        }
    }
    KuroTasksEmptyState(title = title, message = message)
}

// ─── Task rows ─────────────────────────────────────────────────────────────

@Composable
private fun SwipeableTaskCard(
    task: FocusTask,
    today: LocalDate,
    categoryName: String?,
    categoryColor: Color?,
    isLive: Boolean,
    onToggleTask: (Int) -> Unit,
    onTapRow: (FocusTask) -> Unit,
) {
    KuroTaskCard(
        task = task,
        subtitle = categoryName,
        today = today,
        onTap = { onTapRow(task) },
        onToggleComplete = { onToggleTask(task.id) },
        isLive = isLive,
        categoryColor = categoryColor,
    )
}

@Composable
private fun SwipeToDeleteCompletedTaskRow(
    task: FocusTask,
    categoryName: String?,
    categoryColor: Color?,
    onToggleTask: (Int) -> Unit,
    onTapRow: (FocusTask) -> Unit,
) {
    KuroCompletedTaskRow(
        task = task,
        subtitle = categoryName,
        onTap = { onTapRow(task) },
        onToggleComplete = { onToggleTask(task.id) },
        categoryColor = categoryColor,
    )
}

// ─── Detail sheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDetailSheet(
    task: FocusTask,
    categories: List<Category>,
    // True while ANY Pomodoro is running or paused. Combined with [isLive]
    // it drives the three-way CTA state in the sheet:
    //   - no active focus            → "Focus with Purrodoro"  (enabled)
    //   - active focus, this task    → "Open focus"       (enabled, jumps to Home)
    //   - active focus, other task   → "Focus with Purrodoro"  (disabled, helper explains)
    isFocusActive: Boolean,
    isLive: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (FocusTask) -> Unit,
    onDelete: () -> Unit,
    onFocusTask: (FocusTask) -> Unit,
    onAddCategory: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var title by rememberSaveable { mutableStateOf(task.title) }
    var categoryId by rememberSaveable { mutableStateOf(task.categoryId) }
    var editingTitle by rememberSaveable { mutableStateOf(false) }
    var priority by rememberSaveable(task.id) { mutableStateOf(task.priority) }
    var completed by rememberSaveable(task.id) { mutableStateOf(task.completed) }
    var dueDate by rememberSaveable(task.id, stateSaver = LocalDateSaver) {
        mutableStateOf(task.dueDate)
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf(TaskSheetPage.Main) }
    // Shown when the user taps "Focus with Purrodoro" on an already-completed task.
    // Confirming proceeds to the normal start flow without un-completing it.
    var showAlreadyCompleteConfirm by remember { mutableStateOf(false) }

    // Auto-save on dismiss: empty title falls back to the previous value rather
    // than committing whitespace. Only persist when something actually changed
    // so we don't churn DataStore for a read-only open. `completed` is part of
    // the diff so the in-sheet "mark done" toggle is committed on close instead
    // of being clobbered by a stale copy of the task.
    val saveAndDismiss = {
        val trimmed = title.trim().ifEmpty { task.title }
        val changed = trimmed != task.title ||
            categoryId != task.categoryId ||
            priority != task.priority ||
            completed != task.completed ||
            dueDate != task.dueDate
        if (changed) {
            onUpdate(
                task.copy(
                    title = trimmed,
                    categoryId = categoryId,
                    priority = priority,
                    completed = completed,
                    dueDate = dueDate,
                )
            )
        }
        onDismiss()
    }

    // The task with any in-sheet edits applied, handed to the focus flow so the
    // user's latest title/category/etc. carry into the session.
    fun buildUpdatedTask(): FocusTask = task.copy(
        title = title.trim().ifEmpty { task.title },
        categoryId = categoryId,
        priority = priority,
        completed = completed,
        dueDate = dueDate,
    )

    KuroBottomSheet(title = null, onDismiss = saveAndDismiss, contentKey = page) {
        when (page) {
            TaskSheetPage.Main -> {
                Column(
                    modifier = Modifier.padding(top = KuroSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(KuroSpacing.lg),
                ) {
                    DetailTitle(
                        title = title,
                        onTitleChange = { title = it.take(TaskTitleMaxLength) },
                        editing = editingTitle,
                        onStartEdit = { editingTitle = true },
                        onFinishEdit = { editingTitle = false },
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                        CategoryListRow(
                            categories = categories,
                            selectedId = categoryId,
                            onClick = { page = TaskSheetPage.CategoryPicker },
                        )
                        PriorityListRow(
                            selected = priority,
                            onClick = { page = TaskSheetPage.PriorityPicker },
                        )
                        DueDateListRow(
                            dueDate = dueDate,
                            onPick = { showDatePicker = true },
                            onClear = { dueDate = null },
                        )
                    }

                    MarkDoneCheckboxRow(
                        completed = completed,
                        onToggle = { completed = !completed },
                    )

                    val ctaState = when {
                        isLive -> DetailCtaState.OpenFocus
                        isFocusActive -> DetailCtaState.Blocked
                        else -> DetailCtaState.StartFocus
                    }
                    FocusWithKuroButton(
                        label = when (ctaState) {
                            DetailCtaState.OpenFocus -> stringResource(R.string.tasks_open_focus)
                            else -> stringResource(R.string.tasks_focus_with_kuro)
                        },
                        // Stay tappable even when blocked: the tap routes through
                        // attemptStartFocus, which surfaces the "A focus is in
                        // progress" dialog. A dead, greyed-out button left users
                        // unsure why nothing happened / unaware a timer was live.
                        enabled = true,
                        onClick = {
                            // Only the "start a new focus" path needs the
                            // already-complete prompt; Open/Blocked are about a
                            // session that's already live.
                            if (ctaState == DetailCtaState.StartFocus && completed) {
                                showAlreadyCompleteConfirm = true
                            } else {
                                onFocusTask(buildUpdatedTask())
                            }
                        },
                    )

                    DetailHelperRow(
                        message = when (ctaState) {
                            DetailCtaState.OpenFocus ->
                                stringResource(R.string.tasks_helper_open)

                            DetailCtaState.Blocked ->
                                stringResource(R.string.tasks_helper_blocked)

                            DetailCtaState.StartFocus ->
                                stringResource(R.string.tasks_helper_start)
                        },
                    )

                    DeleteTaskLink(onClick = onDelete)
                }
            }

            TaskSheetPage.CategoryPicker -> {
                CategoryPickerPage(
                    categories = categories,
                    selectedId = categoryId,
                    onSelect = {
                        categoryId = it
                        page = TaskSheetPage.Main
                    },
                    onAddCategory = onAddCategory,
                    onBack = { page = TaskSheetPage.Main },
                )
            }

            TaskSheetPage.PriorityPicker -> {
                PriorityPickerPage(
                    selected = priority,
                    onSelect = {
                        priority = it
                        page = TaskSheetPage.Main
                    },
                    onBack = { page = TaskSheetPage.Main },
                )
            }
        }
    }

    if (showDatePicker) {
        DueDatePickerDialog(
            initial = dueDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { picked ->
                dueDate = picked
                showDatePicker = false
            },
        )
    }

    if (showAlreadyCompleteConfirm) {
        FocusAlreadyCompleteDialog(
            isHabit = false,
            onConfirm = {
                showAlreadyCompleteConfirm = false
                onFocusTask(buildUpdatedTask())
            },
            onDismiss = { showAlreadyCompleteConfirm = false },
        )
    }
}

/**
 * Small destructive text link for deleting the task, rendered at the bottom
 * of the sheet. Avoids a high-contrast button so accidental taps are rare,
 * but keeps delete reachable from the detail surface.
 */
@Composable
private fun DeleteTaskLink(onClick: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val deleteTaskLabel = stringResource(R.string.tasks_delete_task)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; contentDescription = deleteTaskLabel }
            .padding(vertical = KuroSpacing.sm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = deleteTaskLabel,
            color = tokens.destructive,
            style = KuroType.labelStrong,
        )
    }
}

/**
 * Detail-sheet title with tap-to-edit affordance.
 *
 * Rendered as a large heading in read mode; tapping it swaps in an input
 * field, matching the reference design's clean "no pencil icon" look. The
 * keyboard's Done action commits the edit, and tapping outside dismisses
 * via auto-save in the parent's `saveAndDismiss`.
 */
@Composable
private fun DetailTitle(
    title: String,
    onTitleChange: (String) -> Unit,
    editing: Boolean,
    onStartEdit: () -> Unit,
    onFinishEdit: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(editing) {
        if (editing) focusRequester.requestFocus()
    }
    if (editing) {
        Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
            ) {
                KuroInputField(
                    value = title,
                    onValueChange = onTitleChange,
                    placeholder = stringResource(R.string.tasks_title_placeholder_short),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onFinishEdit() }),
                    singleLine = false,
                )
                Text(
                    text = stringResource(R.string.common_done),
                    color = tokens.accent,
                    style = KuroType.labelStrong,
                    modifier = Modifier
                        .clickable(onClick = onFinishEdit)
                        .padding(KuroSpacing.xs),
                )
            }
            Text(
                text = stringResource(R.string.tasks_char_count, title.length, TaskTitleMaxLength),
                color = if (title.length >= TaskTitleMaxLength) tokens.accent else tokens.textMuted,
                style = KuroType.labelMeta,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = KuroSpacing.xs),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
    } else {
        val editTitleCd = stringResource(R.string.tasks_edit_title_cd)
        Text(
            text = title,
            color = tokens.textPrimary,
            style = KuroType.headingCard.copy(fontSize = 22.sp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onStartEdit)
                .semantics { role = Role.Button; contentDescription = editTitleCd },
        )
    }
}

/**
 * Quiet checkbox-style row replacing the older switch toggle.
 *
 * We use a single checkbox instead of "label + switch" because there's now
 * only one user-driven completion path here (manual mark-done). The other
 * completion path is automatic — a finished Pomodoro cycle marks the task
 * done from `TimerStateMachine`, and that path doesn't need its own control
 * in the detail sheet. The auto-complete behaviour is announced on Home
 * once a task is attached.
 */
@Composable
private fun MarkDoneCheckboxRow(completed: Boolean, onToggle: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusMd))
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md - KuroSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(KuroSpacing.taskCheckbox)
                .clip(CircleShape)
                .background(if (completed) tokens.accent else tokens.cardFill)
                .border(
                    KuroSpacing.hairline + KuroSpacing.hairline / 5,
                    tokens.accent,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (completed) {
                Text(text = "✓", color = tokens.accentOn, style = KuroType.labelStrong)
            }
        }
        Text(
            text = if (completed) stringResource(R.string.tasks_marked_done)
            else stringResource(R.string.tasks_mark_done_now),
            color = tokens.textPrimary,
            style = KuroType.labelStrong,
            modifier = Modifier.weight(1f),
        )
    }
}

// ─── Detail sheet pickers ──────────────────────────────────────────────────

/**
 * Settings-list-row pattern shared by Priority and Due date. Mirrors common
 * iOS / Android settings rows so the detail sheet reads as "a small list of
 * properties" rather than three different controls stacked. The trailing
 * chevron is a discoverability hint for the dropdown / picker.
 */
@Composable
private fun DetailListRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    valueText: String,
    valueColor: Color,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusMd))
            .clickable(onClick = onClick)
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md - KuroSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            color = tokens.textPrimary,
            style = KuroType.bodyPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = valueText,
            color = valueColor,
            style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
        )
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = tokens.textMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Priority row: a settings-style list row whose chevron opens the priority
 * picker sub-page (see [PriorityPickerPage]). Replaces the older inline
 * dropdown so the choice lands in the same bottom-sheet surface as the rest
 * of the task properties.
 */
@Composable
internal fun PriorityListRow(
    selected: TaskPriority,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val valueColor = if (selected == TaskPriority.High) tokens.accent else tokens.textSecondary
    DetailListRow(
        icon = Lucide.Flag,
        iconTint = if (selected == TaskPriority.High) tokens.accent else tokens.textSecondary,
        label = stringResource(R.string.tasks_priority),
        valueText = taskPriorityLabel(selected),
        valueColor = valueColor,
        onClick = onClick,
    )
}

/**
 * Category row: a settings-style list row whose leading swatch reflects the
 * selected category's color. Tapping opens the category picker sub-page (see
 * [CategoryPickerPage]). Replaces the old horizontally-scrolling pill strip.
 */
@Composable
internal fun CategoryListRow(
    categories: List<Category>,
    selectedId: Int?,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val selectedCategory = categories.firstOrNull { it.id == selectedId }
    // Leading category icon reflects the selected category's own color so the
    // detail row matches the swatch shown on task chips and in the picker. Falls
    // back to a neutral muted tint when no category is selected ("None").
    val iconTint = categoryColorFor(selectedCategory, tokens.textMuted)
    val categoryCd = stringResource(R.string.tasks_category_cd)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusMd))
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; contentDescription = categoryCd }
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md - KuroSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Icon(
            imageVector = Lucide.Tag,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.tasks_category),
            color = tokens.textPrimary,
            style = KuroType.bodyPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = selectedCategory?.name ?: stringResource(R.string.tasks_category_none),
            color = tokens.textSecondary,
            style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
        )
        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = tokens.textMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Category color swatch. A filled dot for real categories; a hollow ring for
 * the "None" option so the absence of a category reads clearly.
 */
@Composable
internal fun CategoryDot(color: Color, hollow: Boolean) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .size(KuroSpacing.taskCategoryDot)
            .clip(CircleShape)
            .then(
                if (hollow) {
                    Modifier
                        .background(tokens.cardFill)
                        .border(1.dp, color, CircleShape)
                } else {
                    Modifier.background(color)
                },
            ),
    )
}

/**
 * Category picker sub-page rendered inside the task sheet. Lists "None" plus
 * every category with its color swatch, and (when [onAddCategory] is provided)
 * an accent "New category" row routed to the category-create sheet. Selecting
 * an option fires [onSelect] and the host returns to the main page.
 */
@Composable
internal fun CategoryPickerPage(
    categories: List<Category>,
    selectedId: Int?,
    onSelect: (Int?) -> Unit,
    onAddCategory: (() -> Unit)?,
    onBack: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val newCategoryCd = stringResource(R.string.tasks_new_category)
    KuroBottomSheetSubpageHeader(
        title = stringResource(R.string.tasks_category),
        onBack = onBack,
        actionLabel = stringResource(R.string.common_done),
        onAction = onBack,
    )
    Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
        PickerOptionRow(
            selected = selectedId == null,
            onClick = { onSelect(null) },
            leading = { CategoryDot(color = tokens.textMuted, hollow = true) },
            title = stringResource(R.string.tasks_category_none),
        )
        categories.forEach { category ->
            PickerOptionRow(
                selected = selectedId == category.id,
                onClick = { onSelect(category.id) },
                leading = {
                    CategoryDot(
                        color = categoryColorFor(category, tokens.textMuted),
                        hollow = false,
                    )
                },
                title = category.name,
            )
        }
        if (onAddCategory != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(KuroSpacing.radiusMd))
                    .clickable(onClick = onAddCategory)
                    .semantics { role = Role.Button; contentDescription = newCategoryCd }
                    .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
            ) {
                Icon(
                    imageVector = Lucide.Plus,
                    contentDescription = null,
                    tint = tokens.accent,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.tasks_new_category),
                    color = tokens.accent,
                    style = KuroType.labelStrong,
                )
            }
        }
    }
}

/**
 * Priority picker sub-page. Each option pairs a colored flag with a short
 * description so the relative weight of High / Normal / Low reads at a glance.
 */
@Composable
internal fun PriorityPickerPage(
    selected: TaskPriority,
    onSelect: (TaskPriority) -> Unit,
    onBack: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    KuroBottomSheetSubpageHeader(
        title = stringResource(R.string.tasks_priority),
        onBack = onBack,
        actionLabel = stringResource(R.string.common_done),
        onAction = onBack,
    )
    Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
        listOf(TaskPriority.High, TaskPriority.Normal, TaskPriority.Low).forEach { option ->
            val flagTint = when (option) {
                TaskPriority.High -> tokens.accent
                TaskPriority.Normal -> tokens.textSecondary
                TaskPriority.Low -> tokens.textMuted
            }
            PickerOptionRow(
                selected = option == selected,
                onClick = { onSelect(option) },
                leading = {
                    Icon(
                        imageVector = Lucide.Flag,
                        contentDescription = null,
                        tint = flagTint,
                        modifier = Modifier.size(18.dp),
                    )
                },
                title = taskPriorityLabel(option),
                subtitle = taskPriorityDescription(option),
            )
        }
    }
}

/**
 * Shared selectable row for the in-sheet pickers. Mirrors [KuroSelectionList]
 * styling (accentSoft fill + accent border + trailing check when selected)
 * but accepts a leading slot so callers can show a color swatch or icon, and
 * an optional subtitle.
 */
@Composable
internal fun PickerOptionRow(
    selected: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(if (selected) tokens.accentSoft else tokens.cardFill)
            .border(
                1.dp,
                if (selected) tokens.accent.copy(alpha = 0.35f) else tokens.cardBorder,
                RoundedCornerShape(KuroSpacing.radiusMd),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        leading()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = KuroType.bodyPrimary,
                color = tokens.textPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = KuroType.labelMeta,
                    color = tokens.textSecondary,
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Lucide.Check,
                contentDescription = null,
                tint = tokens.accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Due-date row: list-row that opens the system date picker. When a date is
 * set, the trailing chevron is swapped for a small "Clear" affordance so
 * users can drop the date without hunting through the picker.
 */
@Composable
internal fun DueDateListRow(
    dueDate: LocalDate?,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val label = dueDateRowValueLabel(dueDate)
    val clearDueCd = stringResource(R.string.tasks_clear_due_cd)
    val valueColor = when {
        dueDate == null -> tokens.textMuted
        dueDate.isBefore(LocalDate.now()) -> tokens.destructive
        else -> tokens.accent
    }
    DetailListRow(
        icon = Lucide.CalendarDays,
        iconTint = if (dueDate != null) tokens.accent else tokens.textSecondary,
        label = stringResource(R.string.tasks_due_date),
        valueText = label,
        valueColor = valueColor,
        onClick = onPick,
        trailing = if (dueDate == null) {
            null
        } else {
            {
                Text(
                    text = stringResource(R.string.tasks_clear),
                    color = tokens.textSecondary,
                    style = KuroType.labelStrong,
                    modifier = Modifier
                        .clickable(onClick = onClear)
                        .padding(horizontal = KuroSpacing.xs, vertical = KuroSpacing.xs)
                        .semantics { role = Role.Button; contentDescription = clearDueCd },
                )
            }
        },
    )
}

/**
 * Bottom-of-sheet helper: small mascot beside short helper copy. Matches
 * the reference design's "Purrodoro will…" footer.
 */
@Composable
private fun DetailHelperRow(message: String) {
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

/**
 * Three logical states the detail-sheet primary CTA can be in. Encapsulated
 * here so the sheet body, button label, helper copy, and click handling all
 * read from one source of truth.
 */
private enum class DetailCtaState {
    /** No active session — tapping opens the StartFocusSheet. */
    StartFocus,

    /** This very task is the live one — tapping jumps to Home. */
    OpenFocus,

    /** Another Pomodoro is running — CTA is disabled, helper explains. */
    Blocked,
}

/**
 * Full-width primary CTA used in the detail sheet. Mirrors the
 * `StartFocusConfirmButton` pill style (play icon + accent fill) but
 * scoped here because the design system's `KuroPrimaryButton` doesn't
 * carry icon affordance and we don't want to add an option to the design
 * system for a single screen.
 *
 * When [enabled] is false the pill is rendered with a muted fill, the
 * click handler is no-op, and the row is marked as a disabled button for
 * accessibility — the surrounding helper copy carries the "why" so this
 * stays purely visual.
 */
@Composable
private fun FocusWithKuroButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val fill = if (enabled) tokens.accent else tokens.accent.copy(alpha = 0.35f)
    val contentColor = if (enabled) tokens.accentOn else tokens.accentOn.copy(alpha = 0.7f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(fill)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .padding(horizontal = KuroSpacing.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Lucide.Play,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(KuroSpacing.sm))
        Text(
            text = label,
            color = contentColor,
            style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.ExtraBold),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DueDatePickerDialog(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate?) -> Unit,
) {
    val initialMillis = initial?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val picked = state.selectedDateMillis
                    ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate() }
                onConfirm(picked)
            }) {
                Text(stringResource(R.string.tasks_date_set))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}

private val LocalDateSaver = androidx.compose.runtime.saveable.Saver<LocalDate?, String>(
    save = { it?.toString() ?: "" },
    restore = { raw ->
        raw.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    },
)

// ─── Urgency chip + next-focus hero ────────────────────────────────────────

/**
 * Leading completion checkbox for the Next Focus hero. Mirrors the unchecked
 * state of the task-row checkbox (accent ring, hollow fill) so the hero borrows
 * the row's "this is a tickable task" affordance. Tapping completes the task;
 * the row press doesn't bubble up to the card's open-detail click.
 */
@Composable
private fun HeroCompletionCheckbox(onToggle: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val markDoneCd = stringResource(R.string.tasks_mark_done_cd)
    Box(
        modifier = Modifier
            .size(KuroSpacing.taskCheckbox)
            .clip(CircleShape)
            .background(tokens.cardFill)
            .border(
                KuroSpacing.hairline + KuroSpacing.hairline / 5,
                tokens.accent,
                CircleShape,
            )
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .semantics { role = Role.Checkbox; contentDescription = markDoneCd },
    )
}

/**
 * "Next focus" hero card at the top of the Tasks list.
 *
 * Surfaces the highest-priority active task (or the currently attached task,
 * if any). Two-column layout: mascot on the left signals the Purrodoro brand and
 * gives the card weight; content on the right carries title, lightweight
 * inline metadata, focus-duration hint, and a compact Start pill.
 *
 * The whole card body is tappable to open the detail sheet; the Start pill
 * has its own click handler so a tap directly on it skips the sheet and
 * jumps into the StartFocusSheet.
 *
 * Hidden when the Done filter is selected — see [TodoScreen].
 */
@Composable
private fun NextFocusHeroCard(
    task: FocusTask,
    today: LocalDate,
    categoryName: String?,
    categoryColor: Color?,
    focusMinutes: Int,
    isLive: Boolean,
    // True when another Pomodoro is running and this task isn't the one
    // attached to it. Renders the Start pill in a muted state; tapping
    // still fires onStartFocus so the parent can surface the busy dialog.
    isStartBlocked: Boolean,
    onOpenDetail: () -> Unit,
    onStartFocus: () -> Unit,
    // Tapped when the hero shows "NOW FOCUSING" — should jump to the live
    // timer (Home tab) rather than re-running the start-focus configure
    // flow, which would imply spinning up a second session.
    onOpenFocus: () -> Unit,
    // Toggles the hero task's completion. Surfaced as a leading checkbox so
    // the hero clearly reads as a task you can also tick off — not just a
    // promotional banner.
    onToggleComplete: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val dueLabel = rememberHeroDueLabel(task.dueDate, today)
    val showHighFlag = task.priority == TaskPriority.High
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusMd))
            .clickable(onClick = onOpenDetail)
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        Text(
            text = if (isLive) stringResource(R.string.tasks_now_focusing)
            else stringResource(R.string.tasks_next_focus),
            color = tokens.accent,
            style = KuroType.labelCaption,
        )
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(id = R.drawable.cat_checklist),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(92.dp)
                    .height(72.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
                ) {
                    HeroCompletionCheckbox(onToggle = onToggleComplete)
                    Text(
                        text = task.title,
                        color = tokens.textPrimary,
                        style = KuroType.headingCard,
                        modifier = Modifier.weight(1f),
                    )
                }
                HeroMetadataRow(
                    categoryName = categoryName,
                    categoryColor = categoryColor ?: tokens.textMuted,
                    dueDate = task.dueDate,
                    dueLabel = dueLabel,
                    isOverdue = task.dueDate?.isBefore(today) == true,
                    showHighFlag = showHighFlag,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Lucide.Clock,
                            contentDescription = null,
                            tint = tokens.textSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = stringResource(R.string.tasks_minutes_focus, focusMinutes),
                            color = tokens.textSecondary,
                            style = KuroType.labelMeta,
                        )
                    }
                    // Three CTA states: this task is live ("Open"), another
                    // focus is blocking a new start ("Finish first" — tapping
                    // surfaces the busy dialog), or a normal "Start".
                    val (pillLabel, pillIcon) = when {
                        isLive -> stringResource(R.string.tasks_pill_open) to Lucide.Play
                        isStartBlocked -> stringResource(R.string.tasks_pill_finish_first) to Lucide.Lock
                        else -> stringResource(R.string.tasks_pill_start) to Lucide.Play
                    }
                    StartPill(
                        label = pillLabel,
                        icon = pillIcon,
                        muted = isStartBlocked,
                        onClick = if (isLive) onOpenFocus else onStartFocus,
                    )
                }
            }
        }
    }
}

/**
 * Single-line metadata for the hero card. We keep this purposely quiet —
 * label-sized text with 12dp icons — so the hero reads as one composition
 * (mascot + title + tiny meta), not "a card stuffed with chips". Only the
 * elements that are actually present get rendered.
 */
@Composable
private fun HeroMetadataRow(
    categoryName: String?,
    categoryColor: Color,
    dueDate: LocalDate?,
    dueLabel: String?,
    isOverdue: Boolean,
    showHighFlag: Boolean,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    if (categoryName == null && dueLabel == null && !showHighFlag) return
    // Separator dots removed — groups are spaced out instead so the row reads
    // as distinct chips rather than a cramped "a • b • c" string.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
    ) {
        if (categoryName != null) {
            HeroMetaChip(
                icon = Lucide.Tag,
                iconTint = categoryColor,
                text = categoryName,
                textColor = tokens.textSecondary,
            )
        }
        if (dueLabel != null) {
            val dueText = if (isOverdue) tokens.destructive else tokens.accent
            HeroMetaChip(
                icon = Lucide.CalendarDays,
                iconTint = dueText,
                text = dueLabel,
                textColor = dueText,
            )
        }
        if (showHighFlag) {
            HeroMetaChip(
                icon = Lucide.Flag,
                iconTint = tokens.accent,
                text = stringResource(R.string.tasks_priority_high),
                textColor = tokens.accent,
            )
        }
    }
}

/** Icon + label metadata group for the Next Focus hero card. */
@Composable
private fun HeroMetaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    text: String,
    textColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(12.dp),
        )
        Text(text = text, color = textColor, style = KuroType.labelMeta)
    }
}

/**
 * Compact accent pill used as the hero's Start action.
 *
 * Replaces the previous full-width KuroPrimaryButton so the hero reads as
 * "task with a quick action" instead of "title with a giant button glued
 * underneath". Stays 40dp tall so it lines up with the focus-duration hint
 * on the left.
 */
@Composable
private fun StartPill(
    label: String,
    muted: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Lucide.Play,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val fill = if (muted) tokens.accent.copy(alpha = 0.35f) else tokens.accent
    val contentColor = if (muted) tokens.accentOn.copy(alpha = 0.7f) else tokens.accentOn
    val pillCd = stringResource(R.string.tasks_pill_focus_cd, label)
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(fill)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; contentDescription = pillCd }
            .padding(start = KuroSpacing.md, end = KuroSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            color = contentColor,
            style = KuroType.labelStrong,
        )
    }
}

// ─── Category name sheet (add / rename) ────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryNameSheet(
    title: String,
    initialName: String,
    initialColor: Long,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var name by rememberSaveable { mutableStateOf(initialName) }
    var colorArgb by rememberSaveable { mutableStateOf(initialColor) }
    val keyboardController = LocalSoftwareKeyboardController.current

    KuroBottomSheet(title = title, onDismiss = onDismiss) {
        KuroInputField(
            value = name,
            onValueChange = { name = it },
            placeholder = stringResource(R.string.tasks_category_name_placeholder),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        Text(
            text = stringResource(R.string.tasks_category_color),
            color = tokens.textMuted,
            style = KuroType.labelCaption,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = KuroSpacing.lg, bottom = KuroSpacing.xs),
        )
        CategoryColorPicker(
            selected = colorArgb,
            onSelect = { colorArgb = it },
        )
        KuroPrimaryButton(
            text = stringResource(R.string.tasks_confirm),
            onClick = {
                val trimmed = name.trim()
                if (trimmed.isNotEmpty()) {
                    keyboardController?.hide()
                    onConfirm(trimmed, colorArgb)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Simple swatch-based color picker for categories. Mirrors the habit editor's
 * color row: a wrapping grid of preset swatches; the selected one gets a ring.
 */
@Composable
private fun CategoryColorPicker(
    selected: Long,
    onSelect: (Long) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val colorCd = stringResource(R.string.tasks_category_color_cd)
    // Plain Row (not FlowRow) — mirrors the habit editor's color row and avoids a
    // FlowRow ABI mismatch with the bundled Compose foundation runtime. The fixed
    // 8-swatch palette fits on a single row.
    Row(
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = KuroSpacing.md),
    ) {
        categoryColorChoices.forEach { choice ->
            val isSelected = choice == selected
            Box(
                modifier = Modifier
                    .size(KuroSpacing.habitColorSwatch)
                    .clip(CircleShape)
                    .background(Color(choice.toInt()))
                    .border(
                        KuroSpacing.habitColorSwatchRing,
                        if (isSelected) tokens.textPrimary else Color.Transparent,
                        CircleShape,
                    )
                    .clickable(
                        role = Role.RadioButton,
                        onClick = { onSelect(choice) },
                    )
                    .semantics { contentDescription = colorCd },
            )
        }
    }
}
