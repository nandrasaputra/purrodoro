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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.Category
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.GlassTreatment
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.ui.components.ScreenStickyBar
import com.endiar.kuropomodoro.ui.designsystem.KuroSpacing
import com.endiar.kuropomodoro.ui.designsystem.KuroType
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import com.endiar.kuropomodoro.ui.theme.Mint

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
    onFocusTask: (FocusTask) -> Unit,
    settings: TimerSettings = TimerSettings(),
    modifier: Modifier = Modifier,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    overBackdrop: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val scrollState = rememberScrollState()
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val topPaddingPx = with(density) { 8.dp.roundToPx() }
    val showStickyBar by remember { derivedStateOf { scrollState.value > headerHeightPx + topPaddingPx } }

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.pageBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = KuroSpacing.lg)
                .padding(top = 18.dp, bottom = 100.dp)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.md)
        ) {
            TasksReferenceHeader(
                modifier = Modifier.onSizeChanged { headerHeightPx = it.height },
                onAddClick = { addingTask = !addingTask },
            )

            TaskFilterSegmentedRow(
                selected = selectedTaskFilter,
                onSelected = { selectedTaskFilter = it },
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
                Text(
                    text = "In Progress",
                    color = tokens.textPrimary,
                    style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.Bold),
                )
                filteredActiveTasks.forEach { task ->
                    SwipeableReferenceTaskCard(
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
                Text(
                    text = "Completed",
                    color = tokens.textPrimary,
                    style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.Bold),
                )
                filteredCompletedTasks.forEach { task ->
                    SwipeToDeleteReferenceCompletedTaskRow(
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
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, tokens.destructive.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .clickable { onDeleteAllCompleted(selectedCategoryId) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Delete Completed",
                        color = tokens.destructive.copy(alpha = 0.82f),
                        style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }

    AnimatedVisibility(
        visible = showStickyBar,
        enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it },
        exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it },
    ) {
        ScreenStickyBar(title = "Tasks")
    }
    } // Box

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

    // ── PreSessionSheet ──────────────────────────────────────────────────
    focusSheetTask?.let { task ->
        PreSessionSheet(
            task = task,
            focusMinutes = settings.focusMinutes,
            onDismiss = { focusSheetTask = null },
            onStartFocus = { updatedTask ->
                focusSheetTask = null
                onFocusTask(updatedTask)
            }
        )
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
                    fontWeight = FontWeight.SemiBold
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
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCategoryTarget = null }) {
                    Text("Cancel", color = dialogTokens.textSecondary)
                }
            },
            containerColor = dialogTokens.cardFill,
            shape = RoundedCornerShape(20.dp)
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
private fun TasksReferenceHeader(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Flat clipboard icon — no card box behind it
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                // Clipboard body
                Column(
                    modifier = Modifier
                        .width(26.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(tokens.activeCardFill)
                        .border(1.5.dp, tokens.accent.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.width(14.dp).height(1.5.dp).background(tokens.textSecondary.copy(alpha = 0.5f)))
                    Box(Modifier.width(14.dp).height(1.5.dp).background(tokens.textSecondary.copy(alpha = 0.5f)))
                    Box(Modifier.width(10.dp).height(1.5.dp).background(tokens.textSecondary.copy(alpha = 0.5f)))
                }
                // Clipboard clip/tab at top
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                        .width(14.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(tokens.accent)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Tasks",
                    color = tokens.textPrimary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 38.sp
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
                .size(48.dp)
                .clip(CircleShape)
                .background(tokens.accent)
                .clickable(onClick = onAddClick)
                .semantics { role = Role.Button; contentDescription = "Add task" },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "+", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TaskFilterSegmentedRow(
    selected: TaskUiFilter,
    onSelected: (TaskUiFilter) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(tokens.segmentedTrack)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(26.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TaskUiFilter.entries.forEach { filter ->
            val isSelected = selected == filter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (isSelected) tokens.cardFill else Color.Transparent)
                    .clickable { onSelected(filter) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.label,
                    color = if (isSelected) tokens.accent else tokens.textSecondary,
                    style = if (isSelected) KuroType.labelStrong else KuroType.bodySecondary.copy(fontWeight = FontWeight.Medium)
                )
            }
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReferenceCategoryPill(
                label = "All",
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) }
            )
            categories.forEach { category ->
                ReferenceCategoryPill(
                    label = category.name,
                    selected = category.id == selectedCategoryId,
                    isEditMode = isEditMode,
                    onClick = {
                        if (isEditMode) onRenameCategory(category) else onCategorySelected(category.id)
                    },
                    onDelete = { onDeleteCategory(category) }
                )
            }
            if (isEditMode) {
                ReferenceCategoryPill(
                    label = "+ New",
                    selected = false,
                    onClick = onAddCategory,
                    accent = true,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (isEditMode) "Done" else "Edit",
            color = tokens.textSecondary,
            style = KuroType.labelStrong,
            modifier = Modifier.clickable(onClick = onToggleEditMode)
        )
    }
}

@Composable
private fun ReferenceCategoryPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isEditMode: Boolean = false,
    onDelete: (() -> Unit)? = null,
    accent: Boolean = false,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(contentAlignment = Alignment.TopStart) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (selected) tokens.textPrimary else tokens.cardFill)
                .border(1.dp, if (accent) tokens.accent.copy(alpha = 0.45f) else tokens.cardBorder, RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = when {
                    selected -> Color.White
                    accent -> tokens.accent
                    else -> tokens.textSecondary
                },
                style = KuroType.labelStrong
            )
        }
        if (isEditMode && onDelete != null) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(tokens.destructive)
                            .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "x", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
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
            .clip(RoundedCornerShape(24.dp))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextField(
                value = newTaskTitle,
                onValueChange = onTitleChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(text = "Add a focus task...", color = tokens.textFaint, fontSize = 14.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = tokens.textPrimary,
                    unfocusedTextColor = tokens.textPrimary,
                    cursorColor = tokens.accent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.accent)
                    .clickable(onClick = onSubmit)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Add", color = Color.White, style = KuroType.labelStrong)
            }
        }
        if (categories.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReferenceAddCategoryChoice(
                    label = "None",
                    selected = newTaskCategoryId == null,
                    onClick = { onCategoryChange(null) },
                )
                categories.forEach { category ->
                    ReferenceAddCategoryChoice(
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
private fun ReferenceAddCategoryChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) tokens.activeCardFill else Color.Transparent)
            .border(1.dp, if (selected) tokens.accent.copy(alpha = 0.45f) else tokens.cardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = if (selected) tokens.accent else tokens.textSecondary,
            style = KuroType.labelMeta.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun TasksOverviewCard(overview: TaskOverview) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Overview",
            color = tokens.textPrimary,
            style = KuroType.headingCard.copy(fontWeight = FontWeight.Bold)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OverviewMetric(
                icon = "📋",
                value = overview.total.toString(),
                label = "Total",
                modifier = Modifier.weight(1f)
            )
            OverviewMetric(
                icon = "🕐",
                value = overview.inProgress.toString(),
                label = "In Progress",
                modifier = Modifier.weight(1f)
            )
            OverviewMetric(
                icon = "✅",
                value = overview.completed.toString(),
                label = "Completed",
                modifier = Modifier.weight(1f)
            )
            OverviewMetric(
                icon = "🕐",
                value = overview.estimatedFocusTimeLabel,
                label = "Est. Focus Time",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OverviewMetric(icon: String, value: String, label: String, modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = icon, fontSize = 20.sp)
        Text(text = value, color = tokens.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            text = label,
            color = tokens.textSecondary,
            style = KuroType.labelMeta.copy(fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TasksEmptyState(filter: TaskUiFilter) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val message = if (filter == TaskUiFilter.Archived) {
        "Archived tasks will appear here once archive support is available."
    } else {
        "Tap + to capture the next thing worth focusing on."
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(26.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = if (filter == TaskUiFilter.Archived) "Nothing archived" else "No tasks here", color = tokens.textPrimary, style = KuroType.headingCard.copy(fontWeight = FontWeight.Bold))
        Text(text = message, color = tokens.textSecondary, style = KuroType.bodySecondary)
    }
}

@Composable
private fun ReferenceTaskCard(
    task: FocusTask,
    categoryName: String?,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onTapRow: (FocusTask) -> Unit,
    onFocusTask: (FocusTask) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(24.dp))
            .clickable { onTapRow(task) }
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Top row: icon tile + title/meta + ⋮ menu ──────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon tile — first character of task title
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tokens.activeCardFill)
                        .border(1.dp, tokens.cardBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = task.title.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                        color = tokens.accent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = task.title,
                        color = tokens.textPrimary,
                        style = KuroType.headingCard.copy(fontWeight = FontWeight.Bold),
                        lineHeight = 20.sp
                    )
                    Text(
                        text = taskMetadataLabel(task, categoryName),
                        color = tokens.textSecondary,
                        style = KuroType.bodySecondary
                    )
                }
            }
            // ⋮ opens detail sheet
            Text(
                text = "⋮",
                color = tokens.textFaint,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onTapRow(task) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // ── Bottom row: pomodoro dots left, N/M count right ───────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pomodoro dots — 14dp, empty = white with border
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(task.plannedSessions.coerceAtLeast(1)) { index ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (index < task.completedSessions) tokens.accent else Color.White)
                            .border(1.5.dp, if (index < task.completedSessions) tokens.accent else tokens.cardBorder, CircleShape)
                    )
                }
            }
            // N / M count in coral
            Text(
                text = "${task.completedSessions.coerceAtMost(task.plannedSessions)} / ${task.plannedSessions.coerceAtLeast(1)}",
                color = tokens.accent,
                style = KuroType.labelStrong
            )
        }
    }
}

@Composable
private fun ReferenceCompletedTaskRow(
    task: FocusTask,
    categoryName: String?,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onTapRow: (FocusTask) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tokens.cardFill)
            .border(1.dp, tokens.cardBorder, RoundedCornerShape(20.dp))
            .clickable { onTapRow(task) }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon tile — same style as active card
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tokens.activeCardFill)
                .border(1.dp, tokens.cardBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = task.title.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                color = tokens.textSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = task.title,
                color = tokens.textSecondary,
                style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.SemiBold),
                textDecoration = TextDecoration.LineThrough
            )
            Text(text = taskMetadataLabel(task, categoryName), color = tokens.textFaint, style = KuroType.labelMeta)
        }
        // Green checkmark on the RIGHT
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Mint)
                .clickable(role = Role.Checkbox) { onToggleTask(task.id) },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
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
private fun SwipeableReferenceTaskCard(
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
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                when {
                                    isLeading -> tokens.accent
                                    isTrailing -> tokens.destructive
                                    else -> Color.Transparent
                                }
                            )
                    .padding(horizontal = 24.dp),
                contentAlignment = if (isLeading) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                when {
                    isLeading -> Text(
                        text = "Focus with Kuro",
                        color = Color.White,
                        style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.Bold)
                    )
                    isTrailing -> Text(
                        text = "Delete",
                        color = Color.White,
                        style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        content = {
            ReferenceTaskCard(
                task = task,
                categoryName = categoryName,
                onToggleTask = onToggleTask,
                onDeleteTask = onDeleteTask,
                onTapRow = onTapRow,
                onFocusTask = onFocusTask,
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteReferenceCompletedTaskRow(
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
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (isTrailing) tokens.destructive else Color.Transparent)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isTrailing) {
                    Text(
                        text = "Delete",
                        color = Color.White,
                        style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        content = {
            ReferenceCompletedTaskRow(
                task = task,
                categoryName = categoryName,
                onToggleTask = onToggleTask,
                onDeleteTask = onDeleteTask,
                onTapRow = onTapRow,
            )
        }
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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

    ModalBottomSheet(
        onDismissRequest = saveAndDismiss,
        sheetState = sheetState,
        containerColor = tokens.cardFill,
        contentColor = tokens.textPrimary,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Task Detail",
                color = tokens.textPrimary,
                style = KuroType.headingCard.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Title field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "TITLE",
                    color = tokens.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(tokens.segmentedTrack),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = tokens.textPrimary,
                        unfocusedTextColor = tokens.textPrimary,
                        cursorColor = tokens.accent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            // Category selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "CATEGORY",
                    color = tokens.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val noneSelected = categoryId == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (noneSelected) tokens.accentSoft else tokens.segmentedTrack)
                            .border(1.dp, if (noneSelected) tokens.accent.copy(alpha = 0.60f) else tokens.borderSoft, RoundedCornerShape(14.dp))
                            .clickable { categoryId = null }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "None",
                            color = if (noneSelected) tokens.accent else tokens.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    categories.forEach { cat ->
                        val isSelected = categoryId == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) tokens.accentSoft else tokens.segmentedTrack)
                                .border(1.dp, if (isSelected) tokens.accent.copy(alpha = 0.60f) else tokens.borderSoft, RoundedCornerShape(14.dp))
                                .clickable { categoryId = cat.id }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat.name,
                                color = if (isSelected) tokens.accent else tokens.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Minutes stepper
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "MINUTES PER SESSION",
                    color = tokens.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    StepperButton(label = "−", enabled = minutes > 1, onClick = { if (minutes > 1) minutes-- })
                    Spacer(Modifier.width(24.dp))
                    Text(text = minutes.toString(), color = tokens.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(24.dp))
                    StepperButton(label = "+", enabled = minutes < 180, onClick = { if (minutes < 180) minutes++ })
                }
            }

            // Planned sessions stepper
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "PLANNED SESSIONS",
                    color = tokens.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    StepperButton(label = "−", enabled = plannedSessions > 1, onClick = { if (plannedSessions > 1) plannedSessions-- })
                    Spacer(Modifier.width(24.dp))
                    Text(text = plannedSessions.toString(), color = tokens.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(24.dp))
                    StepperButton(label = "+", enabled = plannedSessions < 8, onClick = { if (plannedSessions < 8) plannedSessions++ })
                }
            }

            // Completed sessions (read-only)
            Text(
                text = "${task.completedSessions} of ${task.plannedSessions} sessions done",
                color = tokens.accentSoft.copy(alpha = 0.75f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Focus with Kuro button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.accent)
                    .clickable {
                        val updated = task.copy(
                            title = title.trim().ifEmpty { task.title },
                            minutes = minutes,
                            plannedSessions = plannedSessions,
                            categoryId = categoryId
                        )
                        onFocusTask(updated)
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶  Focus with Kuro",
                    color = tokens.accentOn,
                    style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (enabled) tokens.segmentedTrack else tokens.segmentedTrack.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) tokens.textPrimary else tokens.textMuted,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
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
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable { mutableStateOf(initialName) }
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.cardFill,
        contentColor = tokens.textPrimary,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, color = tokens.textPrimary, style = KuroType.headingCard.copy(fontWeight = FontWeight.Bold))
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.segmentedTrack),
                placeholder = { Text("Category name", color = tokens.textMuted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) {
                        keyboardController?.hide()
                        onConfirm(trimmed)
                    }
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = tokens.textPrimary,
                    unfocusedTextColor = tokens.textPrimary,
                    cursorColor = tokens.accent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.accent)
                    .clickable {
                        val trimmed = name.trim()
                        if (trimmed.isNotEmpty()) {
                            keyboardController?.hide()
                            onConfirm(trimmed)
                        }
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Confirm", color = tokens.accentOn, style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PreSessionSheet(
    task: FocusTask,
    focusMinutes: Int,
    onDismiss: () -> Unit,
    onStartFocus: (FocusTask) -> Unit
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sessionCount by rememberSaveable { mutableIntStateOf(1) }
    var showDetail by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.cardFill,
        contentColor = tokens.textPrimary,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = task.title,
                color = tokens.textSecondary,
                style = KuroType.bodySecondary.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = "Start focus session",
                color = tokens.textPrimary,
                style = KuroType.headingCard.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Task Detail",
                color = tokens.textPrimary,
                style = KuroType.headingCard.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = if (sessionCount == 1) "1 session · $focusMinutes min"
                       else "$sessionCount sessions · $focusMinutes min each",
                color = tokens.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.segmentedTrack)
                    .clickable { showDetail = !showDetail }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = if (showDetail) "Hide detail ▲" else "Show detail ▼",
                    color = tokens.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (showDetail) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(tokens.segmentedTrack)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Sessions",
                        color = tokens.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (sessionCount <= 1) tokens.segmentedTrack
                                    else tokens.borderSoft
                                )
                                .clickable { if (sessionCount > 1) sessionCount-- }
                                .semantics { role = Role.Button; contentDescription = "Decrease sessions" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "−",
                                color = if (sessionCount <= 1) tokens.textMuted else tokens.textPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(text = sessionCount.toString(), color = tokens.textPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (sessionCount >= 8) tokens.segmentedTrack
                                    else tokens.borderSoft
                                )
                                .clickable { if (sessionCount < 8) sessionCount++ }
                                .semantics { role = Role.Button; contentDescription = "Increase sessions" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                color = if (sessionCount >= 8) tokens.textMuted else tokens.textPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Duration per session comes from your Focus preference ($focusMinutes min)",
                        color = tokens.textFaint,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.accent)
                    .clickable {
                        onStartFocus(task.copy(plannedSessions = sessionCount, completedSessions = 0))
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶  Start Focus",
                    color = tokens.accentOn,
                    style = KuroType.bodyPrimary.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
