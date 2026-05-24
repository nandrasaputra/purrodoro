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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.endiar.kuropomodoro.model.Category
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.model.Habit
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.ui.components.KuroPrimaryButton
import com.endiar.kuropomodoro.ui.designsystem.KuroBottomSheet
import com.endiar.kuropomodoro.ui.designsystem.KuroChip
import com.endiar.kuropomodoro.ui.designsystem.KuroChipVariant
import com.endiar.kuropomodoro.ui.designsystem.KuroCompletedTaskRow
import com.endiar.kuropomodoro.ui.designsystem.KuroInputField
import com.endiar.kuropomodoro.ui.designsystem.KuroPill
import com.endiar.kuropomodoro.ui.designsystem.KuroPillKind
import com.endiar.kuropomodoro.ui.designsystem.KuroScreenScaffold
import com.endiar.kuropomodoro.ui.designsystem.KuroSectionHeader
import com.endiar.kuropomodoro.ui.designsystem.KuroSegmentedControl
import com.endiar.kuropomodoro.ui.designsystem.KuroSpacing
import com.endiar.kuropomodoro.ui.designsystem.KuroStepper
import com.endiar.kuropomodoro.ui.designsystem.KuroTaskCard
import com.endiar.kuropomodoro.ui.designsystem.KuroTasksEmptyState
import com.endiar.kuropomodoro.ui.designsystem.KuroTasksOverviewCard
import com.endiar.kuropomodoro.ui.designsystem.KuroType
import com.endiar.kuropomodoro.ui.theme.kuroTokens

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
    onEditTimerSettings: () -> Unit = {},
    settings: TimerSettings = TimerSettings(),
    modifier: Modifier = Modifier,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    overBackdrop: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens

    var newTaskTitle by rememberSaveable { mutableStateOf("") }
    var addingTask by rememberSaveable { mutableStateOf(false) }
    var newTaskCategoryId by rememberSaveable { mutableStateOf(selectedCategoryId) }
    var focusSheetTask by remember { mutableStateOf<FocusTask?>(null) }
    var detailSheetTask by remember { mutableStateOf<FocusTask?>(null) }
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    var selectedTaskFilter by rememberSaveable { mutableStateOf(TaskUiFilter.All) }
    var deleteCategoryTarget by remember { mutableStateOf<Category?>(null) }
    var addCategorySheetVisible by rememberSaveable { mutableStateOf(false) }
    var renameCategoryTarget by remember { mutableStateOf<Category?>(null) }

    val categoryFilteredTasks = tasks.filter { task -> selectedCategoryId == null || task.categoryId == selectedCategoryId }
    val visibleTasks = filterTasksForUi(categoryFilteredTasks, selectedTaskFilter)
    val filteredActiveTasks = visibleTasks.filterNot { it.completed }
    val filteredCompletedTasks = visibleTasks.filter { it.completed }
    val overview = taskOverview(categoryFilteredTasks, settings)

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
            Box(headerMeasureModifier) {
                TasksScreenHeader(onAddClick = { addingTask = !addingTask })
            }

            KuroSegmentedControl(
                options = TaskUiFilter.entries.map { it.label },
                selectedIndex = TaskUiFilter.entries.indexOf(selectedTaskFilter),
                onSelect = { selectedTaskFilter = TaskUiFilter.entries[it] },
            )

            CategoryFilterRow(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                isEditMode = isEditMode,
                onCategorySelected = onCategorySelected,
                onToggleEditMode = { isEditMode = !isEditMode },
                onAddCategory = { addCategorySheetVisible = true },
                onRenameCategory = { renameCategoryTarget = it },
                onDeleteCategory = { deleteCategoryTarget = it },
            )

            if (addingTask) {
                InlineAddTaskCard(
                    newTaskTitle = newTaskTitle,
                    onTitleChange = { newTaskTitle = it },
                    categories = categories,
                    newTaskCategoryId = newTaskCategoryId,
                    onCategoryChange = { newTaskCategoryId = it },
                    onSubmit = submitTask,
                )
            }

            TasksOverviewCard(overview = overview)

            if (visibleTasks.isEmpty()) {
                TasksEmptyState(filter = selectedTaskFilter)
            }

            if (filteredActiveTasks.isNotEmpty()) {
                KuroSectionHeader(title = "In Progress")
                filteredActiveTasks.forEach { task ->
                    SwipeableTaskCard(
                        task = task,
                        categoryName = categories.firstOrNull { it.id == task.categoryId }?.name,
                        onToggleTask = onToggleTask,
                        onDeleteTask = onDeleteTask,
                        onTapRow = { detailSheetTask = it },
                        onFocusTask = { focusSheetTask = it },
                    )
                }
            }

            if (filteredCompletedTasks.isNotEmpty()) {
                KuroSectionHeader(title = "Completed")
                filteredCompletedTasks.forEach { task ->
                    SwipeToDeleteCompletedTaskRow(
                        task = task,
                        categoryName = categories.firstOrNull { it.id == task.categoryId }?.name,
                        onToggleTask = onToggleTask,
                        onDeleteTask = onDeleteTask,
                        onTapRow = { detailSheetTask = it },
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(KuroSpacing.radiusMd))
                        .border(1.dp, tokens.destructive.copy(alpha = 0.35f), RoundedCornerShape(KuroSpacing.radiusMd))
                        .clickable { onDeleteAllCompleted(selectedCategoryId) }
                        .padding(vertical = KuroSpacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Delete Completed",
                        color = tokens.destructive.copy(alpha = 0.82f),
                        style = KuroType.labelStrong,
                    )
                }
            }
        }
    }

    // ── TaskDetailSheet ──────────────────────────────────────────────────
    detailSheetTask?.let { task ->
        TaskDetailSheet(
            task = task,
            categories = categories,
            focusMinutes = settings.focusMinutes,
            onDismiss = { detailSheetTask = null },
            onUpdate = onUpdateTask,
            onFocusTask = { updatedTask ->
                detailSheetTask = null
                focusSheetTask = updatedTask
            }
        )
    }

    // ── StartFocusSheet ──────────────────────────────────────────────────
    focusSheetTask?.let { task ->
        key(task.id) {
            StartFocusSheet(
                settings = settings,
                tasks = tasks,
                habits = habits,
                today = java.time.LocalDate.now(),
                prefill = StartFocusPrefill.Task(task.id),
                onDismiss = { focusSheetTask = null },
                onEditConfig = {
                    focusSheetTask = null
                    onEditTimerSettings()
                },
                onStartFocus = { request ->
                    focusSheetTask = null
                    onFocusTask(request)
                },
            )
        }
    }

    // ── Delete Category confirmation dialog ──────────────────────────────
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

    // ── Add Category sheet ───────────────────────────────────────────────
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

    // ── Rename Category sheet ────────────────────────────────────────────
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

@Composable
private fun TasksScreenHeader(
    onAddClick: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
        ) {
            Box(
                modifier = Modifier.size(KuroSpacing.xxxl + KuroSpacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .width(KuroSpacing.xl + KuroSpacing.xs)
                        .height(KuroSpacing.xxl)
                        .clip(RoundedCornerShape(KuroSpacing.sm))
                        .background(tokens.activeCardFill)
                        .border(KuroSpacing.hairline, tokens.accent.copy(alpha = 0.7f), RoundedCornerShape(KuroSpacing.sm))
                        .padding(top = KuroSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.width(KuroSpacing.md - KuroSpacing.xs).height(KuroSpacing.hairline).background(tokens.textSecondary.copy(alpha = 0.5f)))
                    Box(Modifier.width(KuroSpacing.md - KuroSpacing.xs).height(KuroSpacing.hairline).background(tokens.textSecondary.copy(alpha = 0.5f)))
                    Box(Modifier.width(KuroSpacing.md - KuroSpacing.sm).height(KuroSpacing.hairline).background(tokens.textSecondary.copy(alpha = 0.5f)))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = KuroSpacing.xs / 2)
                        .width(KuroSpacing.md - KuroSpacing.xs)
                        .height(KuroSpacing.sm - KuroSpacing.xs)
                        .clip(RoundedCornerShape(KuroSpacing.xs))
                        .background(tokens.accent),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs / 2)) {
                Text(
                    text = "Tasks",
                    color = tokens.textPrimary,
                    style = KuroType.headingDisplay,
                )
                Text(
                    text = "Organize your work your way.",
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
                .clickable(onClick = onAddClick)
                .semantics { role = Role.Button; contentDescription = "Add task" },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "+", color = tokens.accentOn, style = KuroType.fabGlyph)
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<Category>,
    selectedCategoryId: Int?,
    isEditMode: Boolean,
    onCategorySelected: (Int?) -> Unit,
    onToggleEditMode: () -> Unit,
    onAddCategory: () -> Unit,
    onRenameCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
        ) {
            KuroChip(
                text = "All",
                selected = selectedCategoryId == null,
                variant = KuroChipVariant.Inverted,
                onClick = { onCategorySelected(null) },
            )
            categories.forEach { category ->
                KuroChip(
                    text = category.name,
                    selected = category.id == selectedCategoryId,
                    variant = KuroChipVariant.Inverted,
                    onClick = {
                        if (isEditMode) onRenameCategory(category) else onCategorySelected(category.id)
                    },
                    onDelete = if (isEditMode) {{ onDeleteCategory(category) }} else null,
                )
            }
            if (isEditMode) {
                KuroChip(
                    text = "+ New",
                    selected = false,
                    variant = KuroChipVariant.Accent,
                    onClick = onAddCategory,
                )
            }
        }
        Spacer(Modifier.width(KuroSpacing.sm))
        Text(
            text = if (isEditMode) "Done" else "Edit",
            color = tokens.textSecondary,
            style = KuroType.labelStrong,
            modifier = Modifier.clickable(onClick = onToggleEditMode),
        )
    }
}

@Composable
private fun InlineAddTaskCard(
    newTaskTitle: String,
    onTitleChange: (String) -> Unit,
    categories: List<Category>,
    newTaskCategoryId: Int?,
    onCategoryChange: (Int?) -> Unit,
    onSubmit: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
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
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
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
        if (categories.isNotEmpty()) {
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
            }
        }
    }
}

@Composable
private fun AddCategoryChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.clickable(onClick = onClick)) {
        KuroPill(
            label = label,
            kind = if (selected) KuroPillKind.Selected else KuroPillKind.Default,
        )
    }
}

@Composable
private fun TasksOverviewCard(overview: TaskOverview) {
    KuroTasksOverviewCard(
        total = overview.total,
        inProgress = overview.inProgress,
        completed = overview.completed,
        estimatedFocusTimeLabel = overview.estimatedFocusTimeLabel,
    )
}

@Composable
private fun TasksEmptyState(filter: TaskUiFilter) {
    val message = if (filter == TaskUiFilter.Archived) {
        "Archived tasks will appear here once archive support is available."
    } else {
        "Tap + to capture the next thing worth focusing on."
    }
    KuroTasksEmptyState(
        title = if (filter == TaskUiFilter.Archived) "Nothing archived" else "No tasks here",
        message = message,
    )
}

private fun taskMetadataLabel(task: FocusTask, categoryName: String?): String {
    val sessions = task.plannedSessions.coerceAtLeast(1)
    val pomodorosPart = if (sessions == 1) "1 Pomodoro" else "$sessions Pomodoros"
    val datePart = when {
        categoryName != null -> categoryName
        task.completed -> "Done"
        task.completedSessions == 0 -> "Today"
        else -> "Tomorrow"
    }
    return "$datePart · $pomodorosPart"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTaskCard(
    task: FocusTask,
    categoryName: String?,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onTapRow: (FocusTask) -> Unit,
    onFocusTask: (FocusTask) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val scope = rememberCoroutineScope()
    var resetDismiss: (suspend () -> Unit)? = null
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
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
                    val tokens = MaterialTheme.colorScheme.kuroTokens
                    val direction = dismissState.dismissDirection
                    val isLeading = direction == SwipeToDismissBoxValue.StartToEnd
                    val isTrailing = direction == SwipeToDismissBoxValue.EndToStart
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(KuroSpacing.radiusPill))
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
                                style = KuroType.bodyPrimary,
                            )
                            isTrailing -> Text(
                                text = "Delete",
                                color = tokens.accentOn,
                                style = KuroType.bodyPrimary,
                            )
                        }
                    }
                },
        content = {
            KuroTaskCard(
                task = task,
                subtitle = taskMetadataLabel(task, categoryName),
                onTap = { onTapRow(task) },
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
            val tokens = MaterialTheme.colorScheme.kuroTokens
            val isTrailing = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(KuroSpacing.radiusMd))
                    .background(if (isTrailing) tokens.destructive else Color.Transparent)
                    .padding(horizontal = KuroSpacing.xl),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (isTrailing) {
                    Text(
                        text = "Delete",
                        color = tokens.accentOn,
                        style = KuroType.bodyPrimary,
                    )
                }
            }
        },
        content = {
            KuroCompletedTaskRow(
                task = task,
                subtitle = taskMetadataLabel(task, categoryName),
                onTap = { onTapRow(task) },
                onToggleComplete = { onToggleTask(task.id) },
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDetailSheet(
    task: FocusTask,
    categories: List<Category>,
    focusMinutes: Int,
    onDismiss: () -> Unit,
    onUpdate: (FocusTask) -> Unit,
    onFocusTask: (FocusTask) -> Unit
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    var title by rememberSaveable { mutableStateOf(task.title) }
    var minutes by rememberSaveable { mutableIntStateOf(task.minutes) }
    var plannedSessions by rememberSaveable { mutableIntStateOf(task.plannedSessions) }
    var categoryId by rememberSaveable { mutableStateOf(task.categoryId) }

    val saveAndDismiss = {
        val trimmedTitle = title.trim().ifEmpty { task.title }
        onUpdate(task.copy(
            title = trimmedTitle,
            minutes = minutes,
            plannedSessions = plannedSessions,
            categoryId = categoryId
        ))
        onDismiss()
    }

    KuroBottomSheet(title = "Task Detail", onDismiss = saveAndDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.lg)) {
            Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                Text(text = "TITLE", color = tokens.textMuted, style = KuroType.labelCaption)
                KuroInputField(value = title, onValueChange = { title = it }, placeholder = "Task name")
            }

            Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                Text(text = "CATEGORY", color = tokens.textMuted, style = KuroType.labelCaption)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
                ) {
                    val noneSelected = categoryId == null
                    AddCategoryChoice(
                        label = "None",
                        selected = noneSelected,
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

            Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                Text(text = "MINUTES PER SESSION", color = tokens.textMuted, style = KuroType.labelCaption)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    KuroStepper(label = "−", enabled = minutes > 1, large = true, onClick = { if (minutes > 1) minutes-- })
                    Spacer(Modifier.width(KuroSpacing.xl))
                    Text(text = minutes.toString(), color = tokens.textPrimary, style = KuroType.stepperValue)
                    Spacer(Modifier.width(KuroSpacing.xl))
                    KuroStepper(label = "+", enabled = minutes < 180, large = true, onClick = { if (minutes < 180) minutes++ })
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm)) {
                Text(text = "PLANNED SESSIONS", color = tokens.textMuted, style = KuroType.labelCaption)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    KuroStepper(label = "−", enabled = plannedSessions > 1, large = true, onClick = { if (plannedSessions > 1) plannedSessions-- })
                    Spacer(Modifier.width(KuroSpacing.xl))
                    Text(text = plannedSessions.toString(), color = tokens.textPrimary, style = KuroType.stepperValue)
                    Spacer(Modifier.width(KuroSpacing.xl))
                    KuroStepper(label = "+", enabled = plannedSessions < 8, large = true, onClick = { if (plannedSessions < 8) plannedSessions++ })
                }
            }

            Text(
                text = "${task.completedSessions} of ${task.plannedSessions} sessions done",
                color = tokens.accentSoft.copy(alpha = 0.75f),
                style = KuroType.bodySecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            KuroPrimaryButton(
                text = "▶  Focus with Kuro",
                onClick = {
                    val updated = task.copy(
                        title = title.trim().ifEmpty { task.title },
                        minutes = minutes,
                        plannedSessions = plannedSessions,
                        categoryId = categoryId,
                    )
                    onFocusTask(updated)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryNameSheet(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
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

