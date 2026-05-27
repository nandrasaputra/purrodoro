package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.R
import com.endiar.kuropomodoro.model.Category
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.model.Habit
import com.endiar.kuropomodoro.model.TaskPriority
import com.endiar.kuropomodoro.model.TimerSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.endiar.kuropomodoro.ui.components.KuroPrimaryButton
import com.endiar.kuropomodoro.ui.designsystem.KuroBottomSheet
import com.endiar.kuropomodoro.ui.designsystem.KuroCompletedTaskRow
import com.endiar.kuropomodoro.ui.designsystem.KuroInputField
import com.endiar.kuropomodoro.ui.designsystem.KuroPill
import com.endiar.kuropomodoro.ui.designsystem.KuroPillKind
import com.endiar.kuropomodoro.ui.designsystem.KuroScreenScaffold
import com.endiar.kuropomodoro.ui.designsystem.KuroSectionHeader
import com.endiar.kuropomodoro.ui.designsystem.KuroSegmentedControl
import com.endiar.kuropomodoro.ui.designsystem.KuroSpacing
import com.endiar.kuropomodoro.ui.designsystem.KuroTaskCard
import com.endiar.kuropomodoro.ui.designsystem.KuroTasksEmptyState
import com.endiar.kuropomodoro.ui.designsystem.KuroType
import com.endiar.kuropomodoro.ui.designsystem.categoryColorFor
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import kotlinx.coroutines.launch

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
    onAddCategory: (String) -> Unit,
    onRenameCategory: (Int, String) -> Unit,
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
    // Default to Active: the new tabbed filter makes "what's left to do" the
    // primary view; users opt into All or Done explicitly.
    var selectedTaskFilter by rememberSaveable { mutableStateOf(TaskUiFilter.Active) }
    var deleteCategoryTarget by remember { mutableStateOf<Category?>(null) }
    var addCategorySheetVisible by rememberSaveable { mutableStateOf(false) }
    var renameCategoryTarget by remember { mutableStateOf<Category?>(null) }
    var deleteAllCompletedVisible by rememberSaveable { mutableStateOf(false) }
    var categoryDropdownOpen by remember { mutableStateOf(false) }
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
    val filteredActiveTasks = visibleTasks.filterNot { it.completed }
    val filteredCompletedTasks = visibleTasks.filter { it.completed }
    val totalCount = categoryFilteredTasks.size
    val activeCount = categoryFilteredTasks.count { !it.completed }
    val doneCount = categoryFilteredTasks.count { it.completed }

    // Next-focus hero only appears on Active / All. Hide on Done so the hero
    // doesn't push the actual done list down for no reason.
    val nextFocus = remember(categoryFilteredTasks, today, selectedTaskFilter, liveTaskId) {
        if (selectedTaskFilter == TaskUiFilter.Done) return@remember null
        // If a real task is currently attached to a running cycle, use that as
        // the hero — that's the most accurate "what Kuro is focusing on" answer.
        liveTaskId?.let { id -> categoryFilteredTasks.firstOrNull { it.id == id && !it.completed } }
            ?: nextFocusTask(categoryFilteredTasks, today)
    }
    // Avoid double-rendering the hero task in the list below. It's still
    // selectable from the hero card itself (tap opens detail, Start opens focus).
    val heroTaskId = nextFocus?.id
    val activeRowsForList = filteredActiveTasks.filterNot { it.id == heroTaskId }

    val keyboardController = LocalSoftwareKeyboardController.current

    KuroScreenScaffold(
        stickyTitle = "Tasks",
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
            Box(headerMeasureModifier) { TasksScreenHeader() }

            FilterBar(
                statusOptions = listOf(
                    StatusFilterOption(TaskUiFilter.All, "All", totalCount),
                    StatusFilterOption(TaskUiFilter.Active, "Active", activeCount),
                    StatusFilterOption(TaskUiFilter.Done, "Done", doneCount),
                ),
                selectedStatus = selectedTaskFilter,
                onSelectStatus = { selectedTaskFilter = it },
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                dropdownOpen = categoryDropdownOpen,
                onOpenDropdown = { categoryDropdownOpen = true },
                onCloseDropdown = { categoryDropdownOpen = false },
                onSelectCategory = {
                    onCategorySelected(it)
                    categoryDropdownOpen = false
                },
                onManageCategories = {
                    categoryDropdownOpen = false
                    addCategorySheetVisible = true
                },
                onRenameCategoryFromDropdown = {
                    categoryDropdownOpen = false
                    renameCategoryTarget = it
                },
                onDeleteCategoryFromDropdown = {
                    categoryDropdownOpen = false
                    deleteCategoryTarget = it
                },
            )

            nextFocus?.let { hero ->
                val heroIsLive = hero.id == liveTaskId
                NextFocusHeroCard(
                    task = hero,
                    today = today,
                    categoryName = categories.firstOrNull { it.id == hero.categoryId }?.name,
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
                )
            }

            if (visibleTasks.isEmpty()) {
                TasksEmptyState(
                    filter = selectedTaskFilter,
                    hasAnyTask = totalCount > 0,
                    selectedCategoryName = categories.firstOrNull { it.id == selectedCategoryId }?.name,
                )
            }

            // Section header reads "More tasks" when a hero is shown above
            // (rows below can be a mix of dates / no-date / overdue, so we
            // avoid implying everything below is due today). Without a hero
            // we fall back to a flat "Tasks" header.
            if (activeRowsForList.isNotEmpty()) {
                if (selectedTaskFilter != TaskUiFilter.Done) {
                    KuroSectionHeader(
                        title = if (nextFocus != null) "More tasks" else "Tasks"
                    )
                }
                activeRowsForList.forEach { task ->
                    SwipeableTaskCard(
                        task = task,
                        today = today,
                        // When filtered to a single category, hide the per-row
                        // subtitle (it would be the same word on every row).
                        categoryName = categories
                            .firstOrNull { it.id == task.categoryId }
                            ?.name
                            ?.takeIf { selectedCategoryId == null },
                        isLive = task.id == liveTaskId,
                        onToggleTask = onToggleTask,
                        onDeleteTask = onDeleteTask,
                        onTapRow = { detailSheetTask = it },
                        onFocusTask = { attemptStartFocus(it) },
                    )
                }
            }

            if (filteredCompletedTasks.isNotEmpty()) {
                DoneSectionHeader(
                    showSectionLabel = selectedTaskFilter == TaskUiFilter.All,
                    count = filteredCompletedTasks.size,
                    onDeleteAll = { deleteAllCompletedVisible = true },
                )
                filteredCompletedTasks.forEach { task ->
                    SwipeToDeleteCompletedTaskRow(
                        task = task,
                        categoryName = categories
                            .firstOrNull { it.id == task.categoryId }
                            ?.name
                            ?.takeIf { selectedCategoryId == null },
                        onToggleTask = onToggleTask,
                        onDeleteTask = onDeleteTask,
                        onTapRow = { detailSheetTask = it },
                    )
                }
            }
        }

        TasksFab(
            onClick = {
                addTaskSheetCategoryId = selectedCategoryId
                addTaskSheetVisible = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = KuroSpacing.lg)
                // Sit above the bottom dock; matches the dockClearance used
                // elsewhere as the "safe area above the dock".
                .padding(bottom = KuroSpacing.dockClearance + KuroSpacing.sm)
                .padding(contentPadding),
        )
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
                    text = "Delete ${category.name}?",
                    color = dialogTokens.textPrimary,
                    style = KuroType.headingCard,
                )
            },
            text = {
                Text(
                    text = "Tasks in this category will become uncategorized.",
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
                        text = "Delete",
                        color = dialogTokens.destructive,
                        style = KuroType.labelStrong,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCategoryTarget = null }) {
                    Text("Cancel", color = dialogTokens.textSecondary)
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
                    text = "Delete $confirmCount completed task${if (confirmCount == 1) "" else "s"}?",
                    color = dialogTokens.textPrimary,
                    style = KuroType.headingCard,
                )
            },
            text = {
                Text(
                    text = "This can't be undone.",
                    color = dialogTokens.textSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAllCompleted(selectedCategoryId)
                    deleteAllCompletedVisible = false
                }) {
                    Text(
                        text = "Delete",
                        color = dialogTokens.destructive,
                        style = KuroType.labelStrong,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteAllCompletedVisible = false }) {
                    Text("Cancel", color = dialogTokens.textSecondary)
                }
            },
            containerColor = dialogTokens.cardFill,
            shape = RoundedCornerShape(KuroSpacing.radiusMd),
        )
    }

    if (addCategorySheetVisible) {
        CategoryNameSheet(
            title = "New Category",
            initialName = "",
            onDismiss = { addCategorySheetVisible = false },
            onConfirm = { name ->
                onAddCategory(name)
                addCategorySheetVisible = false
            }
        )
    }

    renameCategoryTarget?.let { category ->
        CategoryNameSheet(
            title = "Rename Category",
            initialName = category.name,
            onDismiss = { renameCategoryTarget = null },
            onConfirm = { name ->
                onRenameCategory(category.id, name)
                renameCategoryTarget = null
            }
        )
    }
}

// ─── Header ────────────────────────────────────────────────────────────────

@Composable
private fun TasksScreenHeader() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            // Small mascot anchors Tasks to the Kuro brand: this is the same
            // visual cue used in Onboarding / Profile, scaled down to read as
            // a screen-title glyph rather than illustration.
            Image(
                painter = painterResource(id = R.drawable.cat_face),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
            )
            Text(
                text = "Tasks",
                color = tokens.textPrimary,
                style = KuroType.headingDisplay,
            )
        }
        // Reserved spot for future overflow actions (sort / search / archive).
        // We keep the affordance even though the menu isn't wired yet, so the
        // screen's chrome lands at its final shape.
        Spacer(Modifier.width(KuroSpacing.xxxl))
    }
}

// ─── Filter bar ────────────────────────────────────────────────────────────

private data class StatusFilterOption(
    val filter: TaskUiFilter,
    val label: String,
    val count: Int,
)

@Composable
private fun FilterBar(
    statusOptions: List<StatusFilterOption>,
    selectedStatus: TaskUiFilter,
    onSelectStatus: (TaskUiFilter) -> Unit,
    categories: List<Category>,
    selectedCategoryId: Int?,
    dropdownOpen: Boolean,
    onOpenDropdown: () -> Unit,
    onCloseDropdown: () -> Unit,
    onSelectCategory: (Int?) -> Unit,
    onManageCategories: () -> Unit,
    onRenameCategoryFromDropdown: (Category) -> Unit,
    onDeleteCategoryFromDropdown: (Category) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        KuroSegmentedControl(
            options = statusOptions.map { "${it.label} ${it.count}" },
            selectedIndex = statusOptions.indexOfFirst { it.filter == selectedStatus }
                .coerceAtLeast(0),
            onSelect = { idx -> onSelectStatus(statusOptions[idx].filter) },
            modifier = Modifier.weight(1f),
        )

        CategoryDropdownPill(
            selectedCategory = categories.firstOrNull { it.id == selectedCategoryId },
            categories = categories,
            isOpen = dropdownOpen,
            onOpen = onOpenDropdown,
            onClose = onCloseDropdown,
            onSelectCategory = onSelectCategory,
            onManageCategories = onManageCategories,
            onRenameCategory = onRenameCategoryFromDropdown,
            onDeleteCategory = onDeleteCategoryFromDropdown,
        )
    }
}

@Composable
private fun CategoryDropdownPill(
    selectedCategory: Category?,
    categories: List<Category>,
    isOpen: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onSelectCategory: (Int?) -> Unit,
    onManageCategories: () -> Unit,
    onRenameCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(KuroSpacing.radiusPill))
                .background(tokens.cardFill)
                .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusPill))
                .clickable(onClick = onOpen)
                .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm)
                .semantics { contentDescription = "Filter by category" },
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
                        .background(categoryColorFor(selectedCategory.id, tokens.textMuted)),
                )
            }
            Text(
                text = selectedCategory?.name ?: "All",
                color = tokens.textPrimary,
                style = KuroType.labelStrong,
            )
            Text(
                text = "▾",
                color = tokens.textSecondary,
                style = KuroType.labelMeta,
            )
        }

        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = onClose,
        ) {
            DropdownMenuItem(
                text = { Text("All", style = KuroType.bodyPrimary) },
                onClick = { onSelectCategory(null) },
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(KuroSpacing.taskCategoryDot)
                                    .clip(CircleShape)
                                    .background(categoryColorFor(category.id, tokens.textMuted)),
                            )
                            Text(category.name, style = KuroType.bodyPrimary)
                        }
                    },
                    onClick = { onSelectCategory(category.id) },
                    trailingIcon = {
                        Row(horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                            Text(
                                "Rename",
                                color = tokens.textSecondary,
                                style = KuroType.labelMeta,
                                modifier = Modifier
                                    .clickable { onRenameCategory(category) }
                                    .padding(KuroSpacing.xs),
                            )
                            Text(
                                "Delete",
                                color = tokens.destructive,
                                style = KuroType.labelMeta,
                                modifier = Modifier
                                    .clickable { onDeleteCategory(category) }
                                    .padding(KuroSpacing.xs),
                            )
                        }
                    },
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        "+ New category",
                        color = tokens.accent,
                        style = KuroType.labelStrong,
                    )
                },
                onClick = onManageCategories,
            )
        }
    }
}

// ─── Section header ────────────────────────────────────────────────────────

@Composable
private fun DoneSectionHeader(
    showSectionLabel: Boolean,
    count: Int,
    onDeleteAll: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = KuroSpacing.xs / 2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showSectionLabel) {
            Text(
                text = "Done · $count",
                color = tokens.textSecondary,
                style = KuroType.labelStrong,
            )
        } else {
            // Keep the row layout balanced when label is hidden so the
            // trailing "Delete all" still sits at the right edge.
            Spacer(Modifier.width(KuroSpacing.xs))
        }
        Text(
            text = "Delete all",
            color = tokens.destructive,
            style = KuroType.labelMeta,
            modifier = Modifier
                .clickable(onClick = onDeleteAll)
                .padding(horizontal = KuroSpacing.xs, vertical = KuroSpacing.xs)
                .semantics { role = Role.Button; contentDescription = "Delete all completed tasks" },
        )
    }
}

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
private fun AddTaskSheet(
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

    val titleFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { titleFocusRequester.requestFocus() }

    val canSubmit = title.trim().isNotEmpty()
    val submit: () -> Unit = {
        val trimmed = title.trim()
        if (trimmed.isNotEmpty()) {
            onAdd(trimmed, categoryId, priority, dueDate)
        }
    }

    KuroBottomSheet(title = null, onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.lg)) {
            AddTaskSheetHeader(onClose = onDismiss)

            AddTaskTitleField(
                title = title,
                onTitleChange = { title = it },
                focusRequester = titleFocusRequester,
                onSubmitIme = { if (canSubmit) submit() },
            )

            AddTaskCategorySection(
                categories = categories,
                selectedId = categoryId,
                onSelect = { categoryId = it },
                onAddCategory = onAddCategory,
            )

            Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                PriorityListRow(selected = priority, onSelect = { priority = it })
                DueDateListRow(
                    dueDate = dueDate,
                    onPick = { showDatePicker = true },
                    onClear = { dueDate = null },
                )
            }

            AddTaskPrimaryButton(enabled = canSubmit, onClick = submit)
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
private fun AddTaskSheetHeader(onClose: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "New task",
            color = tokens.textPrimary,
            style = KuroType.headingCard,
            modifier = Modifier.weight(1f),
        )
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

@Composable
private fun AddTaskTitleField(
    title: String,
    onTitleChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onSubmitIme: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
        Text(
            text = "What will you focus on?",
            color = tokens.textSecondary,
            style = KuroType.labelMeta,
        )
        KuroInputField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = "Design onboarding flow",
            modifier = Modifier.focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmitIme() }),
        )
    }
}

@Composable
private fun AddTaskCategorySection(
    categories: List<Category>,
    selectedId: Int?,
    onSelect: (Int?) -> Unit,
    onAddCategory: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
        Text(
            text = "Category",
            color = tokens.textSecondary,
            style = KuroType.labelMeta,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            AddCategoryChoice(
                label = "None",
                selected = selectedId == null,
                onClick = { onSelect(null) },
            )
            categories.forEach { category ->
                AddCategoryChoice(
                    label = category.name,
                    selected = selectedId == category.id,
                    onClick = { onSelect(category.id) },
                )
            }
            AddCategoryChoice(
                label = "+ New",
                selected = false,
                onClick = onAddCategory,
                accent = true,
            )
        }
    }
}

/**
 * Coral pill primary button for the add-task sheet. Patterned after
 * [FocusWithKuroButton] so the two main calls-to-action across task surfaces
 * (Add task / Focus with Kuro) share the same shape, height, and motion.
 */
@Composable
private fun AddTaskPrimaryButton(enabled: Boolean, onClick: () -> Unit) {
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
                contentDescription = "Add task"
            }
            .padding(horizontal = KuroSpacing.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(KuroSpacing.sm))
        Text(
            text = "Add task",
            color = contentColor,
            style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.ExtraBold),
        )
    }
}

@Composable
private fun AddCategoryChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Boolean = false,
) {
    // The "+ New" entry uses the same accentSoft / accent treatment as Selected:
    // semantically it's an action chip, but styling matches Selected for visual
    // consistency with the rest of the strip.
    val kind = when {
        accent -> KuroPillKind.Selected
        selected -> KuroPillKind.Selected
        else -> KuroPillKind.Default
    }
    Box(modifier = Modifier.clickable(onClick = onClick)) {
        KuroPill(label = label, kind = kind)
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
            title = "No tasks yet"
            message = "Tap + to capture the first thing worth focusing on."
        }
        filter == TaskUiFilter.Active -> {
            title = "Nothing active"
            message = if (selectedCategoryName != null) {
                "No active tasks in $selectedCategoryName. Try All, or add a new one."
            } else {
                "All caught up. Add a new task or check what's done."
            }
        }
        filter == TaskUiFilter.Done -> {
            title = "Nothing done yet"
            message = "Finished tasks will land here."
        }
        filter == TaskUiFilter.Archived -> {
            title = "Nothing archived"
            message = "Archived tasks will appear here once archive support is available."
        }
        else -> {
            title = "No tasks here"
            message = "Tap + to capture the next thing worth focusing on."
        }
    }
    KuroTasksEmptyState(title = title, message = message)
}

// ─── Swipeable rows ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTaskCard(
    task: FocusTask,
    today: LocalDate,
    categoryName: String?,
    isLive: Boolean,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onTapRow: (FocusTask) -> Unit,
    onFocusTask: (FocusTask) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var resetDismiss: (suspend () -> Unit)? = null
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Focus is a non-destructive intent — settle the swipe before
                    // showing the start sheet so the row springs back if the user
                    // cancels.
                    scope.launch { resetDismiss?.invoke() }
                    onFocusTask(task)
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDeleteTask(task.id)
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.40f }
    )
    resetDismiss = { dismissState.reset() }

    val badge = taskUrgencyBadge(task, today)

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            SwipeBackground(direction = dismissState.dismissDirection)
        },
        content = {
            KuroTaskCard(
                task = task,
                subtitle = categoryName,
                onTap = { onTapRow(task) },
                onToggleComplete = { onToggleTask(task.id) },
                isLive = isLive,
                trailing = badge?.let { { UrgencyChip(badge = it) } },
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteCompletedTaskRow(
    task: FocusTask,
    categoryName: String?,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onTapRow: (FocusTask) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var resetDismiss: (suspend () -> Unit)? = null
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDeleteTask(task.id)
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Completed rows have no "focus" intent — settle.
                    scope.launch { resetDismiss?.invoke() }
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.40f }
    )
    resetDismiss = { dismissState.reset() }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            SwipeBackground(
                direction = dismissState.dismissDirection,
                showFocus = false,
            )
        },
        content = {
            KuroCompletedTaskRow(
                task = task,
                subtitle = categoryName,
                onTap = { onTapRow(task) },
                onToggleComplete = { onToggleTask(task.id) },
            )
        },
    )
}

@Composable
private fun SwipeBackground(
    direction: SwipeToDismissBoxValue,
    showFocus: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val isLeading = direction == SwipeToDismissBoxValue.StartToEnd && showFocus
    val isTrailing = direction == SwipeToDismissBoxValue.EndToStart
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(
                when {
                    isLeading -> tokens.accent
                    isTrailing -> tokens.destructive
                    else -> Color.Transparent
                },
            )
            .padding(horizontal = KuroSpacing.xl),
        contentAlignment = if (isLeading) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        when {
            isLeading -> Text(
                text = "Focus with Kuro",
                color = tokens.accentOn,
                style = KuroType.labelStrong,
            )
            isTrailing -> Text(
                text = "Delete",
                color = tokens.accentOn,
                style = KuroType.labelStrong,
            )
        }
    }
}

// ─── Detail sheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDetailSheet(
    task: FocusTask,
    categories: List<Category>,
    // True while ANY Pomodoro is running or paused. Combined with [isLive]
    // it drives the three-way CTA state in the sheet:
    //   - no active focus            → "Focus with Kuro"  (enabled)
    //   - active focus, this task    → "Open focus"       (enabled, jumps to Home)
    //   - active focus, other task   → "Focus with Kuro"  (disabled, helper explains)
    isFocusActive: Boolean,
    isLive: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (FocusTask) -> Unit,
    onDelete: () -> Unit,
    onFocusTask: (FocusTask) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var title by rememberSaveable { mutableStateOf(task.title) }
    var categoryId by rememberSaveable { mutableStateOf(task.categoryId) }
    var editingTitle by rememberSaveable { mutableStateOf(false) }
    var priority by rememberSaveable(task.id) { mutableStateOf(task.priority) }
    var dueDate by rememberSaveable(task.id, stateSaver = LocalDateSaver) {
        mutableStateOf(task.dueDate)
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // Auto-save on dismiss: empty title falls back to the previous value rather
    // than committing whitespace. Only persist when something actually changed
    // so we don't churn DataStore for a read-only open.
    val saveAndDismiss = {
        val trimmed = title.trim().ifEmpty { task.title }
        val changed = trimmed != task.title ||
            categoryId != task.categoryId ||
            priority != task.priority ||
            dueDate != task.dueDate
        if (changed) {
            onUpdate(
                task.copy(
                    title = trimmed,
                    categoryId = categoryId,
                    priority = priority,
                    dueDate = dueDate,
                )
            )
        }
        onDismiss()
    }

    KuroBottomSheet(title = null, onDismiss = saveAndDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.lg)) {
            DetailSheetHeader(onClose = saveAndDismiss)

            DetailTitle(
                title = title,
                onTitleChange = { title = it },
                editing = editingTitle,
                onStartEdit = { editingTitle = true },
                onFinishEdit = { editingTitle = false },
            )

            DetailCategoryChips(
                categories = categories,
                selectedId = categoryId,
                onSelect = { categoryId = it },
            )

            Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                PriorityListRow(selected = priority, onSelect = { priority = it })
                DueDateListRow(
                    dueDate = dueDate,
                    onPick = { showDatePicker = true },
                    onClear = { dueDate = null },
                )
            }

            MarkDoneCheckboxRow(
                completed = task.completed,
                onToggle = { onUpdate(task.copy(completed = !task.completed)) },
            )

            val ctaState = when {
                isLive -> DetailCtaState.OpenFocus
                isFocusActive -> DetailCtaState.Blocked
                else -> DetailCtaState.StartFocus
            }
            FocusWithKuroButton(
                label = when (ctaState) {
                    DetailCtaState.OpenFocus -> "Open focus"
                    else -> "Focus with Kuro"
                },
                enabled = ctaState != DetailCtaState.Blocked,
                onClick = {
                    val updated = task.copy(
                        title = title.trim().ifEmpty { task.title },
                        categoryId = categoryId,
                        priority = priority,
                        dueDate = dueDate,
                    )
                    onFocusTask(updated)
                },
            )

            DetailHelperRow(
                message = when (ctaState) {
                    DetailCtaState.OpenFocus ->
                        "This Pomodoro is in progress — tap to jump back to it."

                    DetailCtaState.Blocked ->
                        "A Pomodoro is already running. Finish or stop it to focus on this task."

                    DetailCtaState.StartFocus ->
                        "We'll set up the focus together — start when you're ready."
                },
            )

            DeleteTaskLink(onClick = onDelete)
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

/**
 * Lighter detail-sheet header per the design refresh.
 *
 * The old version centered a "TASK" caption with close + delete on the
 * sides. The reference design treats the title itself as the heading, so
 * we drop the caption and reduce the header to just a close affordance on
 * the right. Delete moves to a small destructive link at the bottom of
 * the sheet so it's reachable but visually quiet.
 */
@Composable
private fun DetailSheetHeader(onClose: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
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

/**
 * Small destructive text link for deleting the task, rendered at the bottom
 * of the sheet. Avoids a high-contrast button so accidental taps are rare,
 * but keeps delete reachable from the detail surface.
 */
@Composable
private fun DeleteTaskLink(onClick: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; contentDescription = "Delete task" }
            .padding(vertical = KuroSpacing.sm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Delete task",
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            KuroInputField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = "Task name",
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onFinishEdit() }),
            )
            Text(
                text = "Done",
                color = tokens.accent,
                style = KuroType.labelStrong,
                modifier = Modifier
                    .clickable(onClick = onFinishEdit)
                    .padding(KuroSpacing.xs),
            )
        }
    } else {
        Text(
            text = title,
            color = tokens.textPrimary,
            style = KuroType.headingCard.copy(fontSize = 22.sp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onStartEdit)
                .semantics { role = Role.Button; contentDescription = "Edit title" },
        )
    }
}

/**
 * Inline category-pill strip used at the top of the detail sheet. Matches
 * the reference's single "Category" row look: lightweight pills, no
 * caption label needed because the task title above provides context.
 */
@Composable
private fun DetailCategoryChips(
    categories: List<Category>,
    selectedId: Int?,
    onSelect: (Int?) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        AddCategoryChoice(
            label = "None",
            selected = selectedId == null,
            onClick = { onSelect(null) },
        )
        categories.forEach { cat ->
            AddCategoryChoice(
                label = cat.name,
                selected = selectedId == cat.id,
                onClick = { onSelect(cat.id) },
            )
        }
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
            text = if (completed) "Task marked done" else "Mark task done now",
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
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = tokens.textMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Priority row: list-row with a dropdown menu anchor. We anchor the menu to
 * the row itself so the dropdown drops down right beneath the trailing
 * chevron, matching the visual affordance.
 */
@Composable
private fun PriorityListRow(
    selected: TaskPriority,
    onSelect: (TaskPriority) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var menuOpen by remember { mutableStateOf(false) }
    val valueColor = if (selected == TaskPriority.High) tokens.accent else tokens.textSecondary
    Box {
        DetailListRow(
            icon = Icons.Outlined.Flag,
            iconTint = if (selected == TaskPriority.High) tokens.accent else tokens.textSecondary,
            label = "Priority",
            valueText = selected.label,
            valueColor = valueColor,
            onClick = { menuOpen = true },
        )
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
        ) {
            listOf(TaskPriority.High, TaskPriority.Normal, TaskPriority.Low).forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            style = KuroType.bodyPrimary.copy(
                                fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                            color = if (option == TaskPriority.High) tokens.accent else tokens.textPrimary,
                        )
                    },
                    onClick = {
                        onSelect(option)
                        menuOpen = false
                    },
                )
            }
        }
    }
}

/**
 * Due-date row: list-row that opens the system date picker. When a date is
 * set, the trailing chevron is swapped for a small "Clear" affordance so
 * users can drop the date without hunting through the picker.
 */
@Composable
private fun DueDateListRow(
    dueDate: LocalDate?,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val label = dueDateRowValue(dueDate)
    val valueColor = when {
        dueDate == null -> tokens.textMuted
        dueDate.isBefore(LocalDate.now()) -> tokens.destructive
        else -> tokens.accent
    }
    DetailListRow(
        icon = Icons.Outlined.CalendarMonth,
        iconTint = if (dueDate != null) tokens.accent else tokens.textSecondary,
        label = "Due date",
        valueText = label,
        valueColor = valueColor,
        onClick = onPick,
        trailing = if (dueDate == null) {
            null
        } else {
            {
                Text(
                    text = "Clear",
                    color = tokens.textSecondary,
                    style = KuroType.labelStrong,
                    modifier = Modifier
                        .clickable(onClick = onClear)
                        .padding(horizontal = KuroSpacing.xs, vertical = KuroSpacing.xs)
                        .semantics { role = Role.Button; contentDescription = "Clear due date" },
                )
            }
        },
    )
}

/**
 * Compact "value" string for the due-date row. Uses relative wording for
 * Today / Tomorrow so the most common cases read naturally, and falls back
 * to "EEE, MMM d" for further-out dates.
 */
private fun dueDateRowValue(dueDate: LocalDate?): String {
    if (dueDate == null) return "Not set"
    val today = LocalDate.now()
    return when (dueDate) {
        today -> "Today, ${dueDate.format(DateTimeFormatter.ofPattern("MMM d"))}"
        today.plusDays(1) -> "Tomorrow"
        else -> dueDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    }
}

/**
 * Bottom-of-sheet helper: small mascot beside short helper copy. Matches
 * the reference design's "Kuro will…" footer.
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
            imageVector = Icons.Filled.PlayArrow,
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

/**
 * Confirmation dialog shown when the user tries to start a Pomodoro for a
 * task other than the one currently in focus. We never silently replace a
 * running session — the user must either jump to the running focus to
 * finish/stop it, or back out of the action.
 */
@Composable
private fun FocusBusyDialog(
    onOpenFocus: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "A focus is in progress",
                color = tokens.textPrimary,
                style = KuroType.headingCard,
            )
        },
        text = {
            Text(
                text = "Finish or stop the current Pomodoro before starting a new one.",
                color = tokens.textSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenFocus) {
                Text(
                    text = "Open focus",
                    color = tokens.accent,
                    style = KuroType.labelStrong,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = tokens.textSecondary)
            }
        },
        containerColor = tokens.cardFill,
        shape = RoundedCornerShape(KuroSpacing.radiusMd),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueDatePickerDialog(
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
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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

@Composable
private fun UrgencyChip(badge: TaskUrgencyBadge) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val label: String
    val fill: Color
    val textColor: Color
    val showFlag: Boolean
    when (badge) {
        TaskUrgencyBadge.Overdue -> {
            label = "Overdue"
            fill = tokens.destructive.copy(alpha = 0.14f)
            textColor = tokens.destructive
            showFlag = false
        }
        TaskUrgencyBadge.Today -> {
            label = "Today"
            fill = tokens.accentSoft
            textColor = tokens.accent
            showFlag = false
        }
        TaskUrgencyBadge.Tomorrow -> {
            label = "Tomorrow"
            fill = tokens.segmentedTrack
            textColor = tokens.textSecondary
            showFlag = false
        }
        TaskUrgencyBadge.HighNoDate -> {
            label = "High"
            fill = tokens.accentSoft
            textColor = tokens.accent
            showFlag = true
        }
        is TaskUrgencyBadge.FutureDate -> {
            label = badge.label
            fill = Color.Transparent
            textColor = tokens.textMuted
            showFlag = false
        }
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(KuroSpacing.radiusSm))
            .background(fill)
            .padding(horizontal = KuroSpacing.sm, vertical = KuroSpacing.xs / 2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2),
    ) {
        if (showFlag) {
            Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp),
            )
        }
        Text(text = label, style = KuroType.labelMeta, color = textColor)
    }
}

/**
 * "Next focus" hero card at the top of the Tasks list.
 *
 * Surfaces the highest-priority active task (or the currently attached task,
 * if any). Two-column layout: mascot on the left signals the Kuro brand and
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
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val dueLabel = heroDueLabel(task.dueDate, today)
    val showHighFlag = task.priority == TaskPriority.High
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusLg))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusLg))
            .clickable(onClick = onOpenDetail)
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Text(
            text = if (isLive) "NOW FOCUSING" else "NEXT FOCUS",
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
                verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
            ) {
                Text(
                    text = task.title,
                    color = tokens.textPrimary,
                    style = KuroType.headingCard,
                )
                HeroMetadataRow(
                    categoryName = categoryName,
                    categoryId = task.categoryId,
                    dueDate = task.dueDate,
                    dueLabel = dueLabel,
                    isOverdue = task.dueDate?.isBefore(today) == true,
                    showHighFlag = showHighFlag,
                )
                Spacer(Modifier.height(KuroSpacing.xs))
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
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = tokens.textSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "$focusMinutes min focus",
                            color = tokens.textSecondary,
                            style = KuroType.labelMeta,
                        )
                    }
                    StartPill(
                        label = if (isLive) "Open" else "Start",
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
    categoryId: Int?,
    dueDate: LocalDate?,
    dueLabel: String?,
    isOverdue: Boolean,
    showHighFlag: Boolean,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    if (categoryName == null && dueLabel == null && !showHighFlag) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        if (categoryName != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
            ) {
                Box(
                    modifier = Modifier
                        .size(KuroSpacing.taskCategoryDot)
                        .clip(CircleShape)
                        .background(
                            com.endiar.kuropomodoro.ui.designsystem.categoryColorFor(
                                categoryId,
                                fallback = tokens.textMuted,
                            )
                        ),
                )
                Text(
                    text = categoryName,
                    color = tokens.textSecondary,
                    style = KuroType.labelMeta,
                )
            }
        }
        if (dueLabel != null) {
            // Inline dot separator — only shown when there's something on
            // either side of it, so we don't render a dangling "•".
            if (categoryName != null) HeroMetaDot()
            val dueText = if (isOverdue) tokens.destructive else tokens.accent
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = dueText,
                    modifier = Modifier.size(12.dp),
                )
                Text(text = dueLabel, color = dueText, style = KuroType.labelMeta)
            }
        }
        if (showHighFlag) {
            if (categoryName != null || dueLabel != null) HeroMetaDot()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Flag,
                    contentDescription = null,
                    tint = tokens.accent,
                    modifier = Modifier.size(12.dp),
                )
                Text(text = "High", color = tokens.accent, style = KuroType.labelMeta)
            }
        }
    }
}

@Composable
private fun HeroMetaDot() {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(tokens.textMuted),
    )
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
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val fill = if (muted) tokens.accent.copy(alpha = 0.35f) else tokens.accent
    val contentColor = if (muted) tokens.accentOn.copy(alpha = 0.7f) else tokens.accentOn
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
            .background(fill)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; contentDescription = "$label focus" }
            .padding(start = KuroSpacing.md, end = KuroSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
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

// ─── FAB ───────────────────────────────────────────────────────────────────

@Composable
private fun TasksFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = modifier
            .size(KuroSpacing.taskFab)
            .clip(CircleShape)
            .background(tokens.accent)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; contentDescription = "Add task" },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "+", color = tokens.accentOn, style = KuroType.fabGlyph)
    }
}

// ─── Category name sheet (add / rename) ────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryNameSheet(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val keyboardController = LocalSoftwareKeyboardController.current

    KuroBottomSheet(title = title, onDismiss = onDismiss) {
        KuroInputField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Category name",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        KuroPrimaryButton(
            text = "Confirm",
            onClick = {
                val trimmed = name.trim()
                if (trimmed.isNotEmpty()) {
                    keyboardController?.hide()
                    onConfirm(trimmed)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
