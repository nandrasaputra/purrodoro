package com.endiar.kuropomodoro.ui

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.model.Category
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.model.Habit
import com.endiar.kuropomodoro.model.TimerSettings
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
    onAddTask: (String, Int?) -> Unit,
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
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    var newTaskTitle by rememberSaveable { mutableStateOf("") }
    var addingTask by rememberSaveable { mutableStateOf(false) }
    var newTaskCategoryId by rememberSaveable { mutableStateOf(selectedCategoryId) }
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

    val categoryFilteredTasks = tasks.filter { task ->
        selectedCategoryId == null || task.categoryId == selectedCategoryId
    }
    val visibleTasks = filterTasksForUi(categoryFilteredTasks, selectedTaskFilter)
    val filteredActiveTasks = visibleTasks.filterNot { it.completed }
    val filteredCompletedTasks = visibleTasks.filter { it.completed }
    val totalCount = categoryFilteredTasks.size
    val activeCount = categoryFilteredTasks.count { !it.completed }
    val doneCount = categoryFilteredTasks.count { it.completed }

    val keyboardController = LocalSoftwareKeyboardController.current
    val submitTask = {
        val title = newTaskTitle.trim()
        if (title.isNotEmpty()) {
            onAddTask(title, newTaskCategoryId)
            newTaskTitle = ""
            addingTask = false
            newTaskCategoryId = selectedCategoryId
            keyboardController?.hide()
        }
    }

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

            if (addingTask) {
                InlineAddTaskCard(
                    newTaskTitle = newTaskTitle,
                    onTitleChange = { newTaskTitle = it },
                    categories = categories,
                    newTaskCategoryId = newTaskCategoryId,
                    onCategoryChange = { newTaskCategoryId = it },
                    onSubmit = submitTask,
                    onAddCategory = { addCategorySheetVisible = true },
                )
            }

            if (visibleTasks.isEmpty()) {
                TasksEmptyState(
                    filter = selectedTaskFilter,
                    hasAnyTask = totalCount > 0,
                    selectedCategoryName = categories.firstOrNull { it.id == selectedCategoryId }?.name,
                )
            }

            // Section labels only render when the status filter is "All" — when the
            // user has explicitly picked "Active" or "Done", the section name would
            // just restate their selection.
            if (filteredActiveTasks.isNotEmpty()) {
                if (selectedTaskFilter == TaskUiFilter.All) {
                    KuroSectionHeader(title = "In Progress")
                }
                filteredActiveTasks.forEach { task ->
                    SwipeableTaskCard(
                        task = task,
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
                        onFocusTask = { focusSheetTask = it },
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
                addingTask = true
                newTaskCategoryId = selectedCategoryId
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

    detailSheetTask?.let { task ->
        TaskDetailSheet(
            task = task,
            categories = categories,
            onDismiss = { detailSheetTask = null },
            onUpdate = onUpdateTask,
            onDelete = {
                onDeleteTask(task.id)
                detailSheetTask = null
            },
            onFocusTask = { updatedTask ->
                detailSheetTask = null
                focusSheetTask = updatedTask
            }
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
        Text(
            text = "Tasks",
            color = tokens.textPrimary,
            style = KuroType.headingDisplay,
        )
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

// ─── Inline add card ───────────────────────────────────────────────────────

@Composable
private fun InlineAddTaskCard(
    newTaskTitle: String,
    onTitleChange: (String) -> Unit,
    categories: List<Category>,
    newTaskCategoryId: Int?,
    onCategoryChange: (Int?) -> Unit,
    onSubmit: () -> Unit,
    onAddCategory: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusLg))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusLg))
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            KuroInputField(
                value = newTaskTitle,
                onValueChange = onTitleChange,
                placeholder = "Add a focus task...",
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(KuroSpacing.radiusSm))
                    .background(tokens.accent)
                    .clickable(onClick = onSubmit)
                    .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Add", color = tokens.accentOn, style = KuroType.labelStrong)
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            AddCategoryChoice(
                label = "None",
                selected = newTaskCategoryId == null,
                onClick = { onCategoryChange(null) },
            )
            categories.forEach { category ->
                AddCategoryChoice(
                    label = category.name,
                    selected = newTaskCategoryId == category.id,
                    onClick = { onCategoryChange(category.id) },
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
    onDismiss: () -> Unit,
    onUpdate: (FocusTask) -> Unit,
    onDelete: () -> Unit,
    onFocusTask: (FocusTask) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var title by rememberSaveable { mutableStateOf(task.title) }
    var categoryId by rememberSaveable { mutableStateOf(task.categoryId) }
    var editingTitle by rememberSaveable { mutableStateOf(false) }

    // Auto-save on dismiss: empty title falls back to the previous value rather
    // than committing whitespace. Only persist when something actually changed
    // so we don't churn DataStore for a read-only open.
    val saveAndDismiss = {
        val trimmed = title.trim().ifEmpty { task.title }
        val changed = trimmed != task.title || categoryId != task.categoryId
        if (changed) {
            onUpdate(task.copy(title = trimmed, categoryId = categoryId))
        }
        onDismiss()
    }

    KuroBottomSheet(title = null, onDismiss = saveAndDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.lg)) {
            DetailSheetHeader(onClose = saveAndDismiss, onDelete = onDelete)

            EditableTitleField(
                title = title,
                onTitleChange = { title = it },
                editing = editingTitle,
                onToggleEditing = { editingTitle = !editingTitle },
            )

            Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                Text(text = "CATEGORY", color = tokens.textMuted, style = KuroType.labelCaption)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
                ) {
                    AddCategoryChoice(
                        label = "None",
                        selected = categoryId == null,
                        onClick = { categoryId = null },
                    )
                    categories.forEach { cat ->
                        AddCategoryChoice(
                            label = cat.name,
                            selected = categoryId == cat.id,
                            onClick = { categoryId = cat.id },
                        )
                    }
                }
            }

            MarkCompleteRow(
                completed = task.completed,
                onToggle = {
                    onUpdate(task.copy(completed = !task.completed))
                },
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
                modifier = Modifier.fillMaxWidth(),
            ) {
                KuroPrimaryButton(
                    text = "Focus with Kuro",
                    onClick = {
                        val updated = task.copy(
                            title = title.trim().ifEmpty { task.title },
                            categoryId = categoryId,
                        )
                        onFocusTask(updated)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Uses your default cycle. Edit it in Profile.",
                    color = tokens.textMuted,
                    style = KuroType.labelMeta,
                )
            }
        }
    }
}

@Composable
private fun DetailSheetHeader(onClose: () -> Unit, onDelete: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(KuroSpacing.xxl)
                .clip(CircleShape)
                .border(1.dp, tokens.cardBorder, CircleShape)
                .clickable(onClick = onClose)
                .semantics { role = Role.Button; contentDescription = "Close" },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✕", color = tokens.textSecondary, style = KuroType.labelStrong)
        }
        Text(
            text = "TASK",
            color = tokens.textMuted,
            style = KuroType.labelCaption,
        )
        Box(
            modifier = Modifier
                .size(KuroSpacing.xxl)
                .clip(CircleShape)
                .border(1.dp, tokens.cardBorder, CircleShape)
                .clickable(onClick = onDelete)
                .semantics { role = Role.Button; contentDescription = "Delete task" },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🗑", color = tokens.destructive, style = KuroType.labelStrong)
        }
    }
}

@Composable
private fun EditableTitleField(
    title: String,
    onTitleChange: (String) -> Unit,
    editing: Boolean,
    onToggleEditing: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(editing) {
        if (editing) focusRequester.requestFocus()
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        if (editing) {
            KuroInputField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = "Task name",
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onToggleEditing() }),
            )
            Text(
                text = "Done",
                color = tokens.accent,
                style = KuroType.labelStrong,
                modifier = Modifier
                    .clickable(onClick = onToggleEditing)
                    .padding(KuroSpacing.xs),
            )
        } else {
            Text(
                text = title,
                color = tokens.textPrimary,
                style = KuroType.headingDisplay,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(KuroSpacing.xxl)
                    .clip(CircleShape)
                    .border(1.dp, tokens.cardBorder, CircleShape)
                    .clickable(onClick = onToggleEditing)
                    .semantics { role = Role.Button; contentDescription = "Edit title" },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "✎", color = tokens.textSecondary, style = KuroType.labelStrong)
            }
        }
    }
}

@Composable
private fun MarkCompleteRow(completed: Boolean, onToggle: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KuroSpacing.radiusMd))
            .background(tokens.activeCardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusMd))
            .clickable(onClick = onToggle)
            .padding(horizontal = KuroSpacing.md, vertical = KuroSpacing.md - KuroSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (completed) "Marked complete" else "Mark complete",
            color = tokens.textPrimary,
            style = KuroType.labelStrong,
        )
        // Pill-shaped switch styled with the same tokens used elsewhere.
        Box(
            modifier = Modifier
                .width(KuroSpacing.xxl + KuroSpacing.sm)
                .height(KuroSpacing.lg + KuroSpacing.xs / 2)
                .clip(RoundedCornerShape(KuroSpacing.radiusPill))
                .background(if (completed) tokens.accent else tokens.cardBorder)
                .padding(KuroSpacing.xs / 2),
            contentAlignment = if (completed) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .size(KuroSpacing.md - KuroSpacing.xs / 2)
                    .clip(CircleShape)
                    .background(tokens.cardFill),
            )
        }
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
