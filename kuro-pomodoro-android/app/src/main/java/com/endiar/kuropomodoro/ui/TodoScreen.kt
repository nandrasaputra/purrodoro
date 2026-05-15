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
import androidx.compose.runtime.getValue
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
import com.endiar.kuropomodoro.ui.components.GlassCard
import com.endiar.kuropomodoro.ui.components.GlassPill
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val filteredActiveTasks = tasks.filter { task ->
        !task.completed && (selectedCategoryId == null || task.categoryId == selectedCategoryId)
    }
    val filteredCompletedTasks = tasks.filter { task ->
        task.completed && (selectedCategoryId == null || task.categoryId == selectedCategoryId)
    }

    var newTaskTitle by rememberSaveable { mutableStateOf("") }
    var addingTask by rememberSaveable { mutableStateOf(false) }
    var newTaskCategoryId by rememberSaveable { mutableStateOf(selectedCategoryId) }
    var focusSheetTask by remember { mutableStateOf<FocusTask?>(null) }
    var detailSheetTask by remember { mutableStateOf<FocusTask?>(null) }
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    var completedExpanded by rememberSaveable { mutableStateOf(false) }
    var deleteCategoryTarget by remember { mutableStateOf<Category?>(null) }
    var addCategorySheetVisible by rememberSaveable { mutableStateOf(false) }
    var renameCategoryTarget by remember { mutableStateOf<Category?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val today = LocalDate.now()
    val dateLabel = today.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 100.dp)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header row ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "To-Do",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateLabel,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF8B6F))
                    .clickable { addingTask = !addingTask },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // ── Filter pill row ──────────────────────────────────────────────
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
                CategoryPill(
                    label = "All",
                    selected = selectedCategoryId == null,
                    isEditMode = false,
                    onClick = { onCategorySelected(null) },
                    glassTreatment = glassTreatment
                )
                categories.forEach { category ->
                    CategoryPill(
                        label = category.name,
                        selected = category.id == selectedCategoryId,
                        isEditMode = isEditMode,
                        onClick = {
                            if (isEditMode) {
                                renameCategoryTarget = category
                            } else {
                                onCategorySelected(category.id)
                            }
                        },
                        onDelete = { deleteCategoryTarget = category },
                        glassTreatment = glassTreatment
                    )
                }
                if (isEditMode) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0xFFFF8B6F).copy(alpha = 0.50f), RoundedCornerShape(20.dp))
                            .clickable { addCategorySheetVisible = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ New",
                            color = Color(0xFFFF8B6F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color.White.copy(alpha = 0.18f))
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (isEditMode) "Done" else "Edit",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { isEditMode = !isEditMode }
            )
        }

        // ── Inline add-task input ────────────────────────────────────────
        if (addingTask) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F0A1A).copy(alpha = 0.40f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "Add a focus task…",
                                color = Color.White.copy(alpha = 0.35f),
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitTask() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFFFF8B6F),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFF8B6F))
                            .clickable { submitTask() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(text = "Add", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (categories.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (newTaskCategoryId == null) Color(0xFFFF8B6F).copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .border(
                                    1.dp,
                                    if (newTaskCategoryId == null) Color(0xFFFF8B6F).copy(alpha = 0.60f)
                                    else Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { newTaskCategoryId = null }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "None",
                                color = if (newTaskCategoryId == null) Color(0xFFFF8B6F) else Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        categories.forEach { cat ->
                            val isSelected = newTaskCategoryId == cat.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) Color(0xFFFF8B6F).copy(alpha = 0.25f)
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFFFF8B6F).copy(alpha = 0.60f)
                                        else Color.White.copy(alpha = 0.15f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { newTaskCategoryId = cat.id }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = cat.name,
                                    color = if (isSelected) Color(0xFFFF8B6F) else Color.White.copy(alpha = 0.55f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Active tasks ────────────────────────────────────────────────
        Text(
            text = "UPCOMING",
            color = Color.White.copy(alpha = 0.30f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )

        if (filteredActiveTasks.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth(), glassTreatment = glassTreatment) {
                Text(
                    text = "No tasks yet",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tap + to add your first focus task.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp
                )
            }
        } else {
            val firstActiveTask = filteredActiveTasks.firstOrNull()
            filteredActiveTasks.forEach { task ->
                TaskGlassRow(
                    task = task,
                    active = task == firstActiveTask,
                    showCategoryLabel = selectedCategoryId == null,
                    categoryName = categories.firstOrNull { it.id == task.categoryId }?.name,
                    onToggleTask = onToggleTask,
                    onDeleteTask = onDeleteTask,
                    onTapRow = { detailSheetTask = it },
                    onFocusTask = { if (!task.completed) focusSheetTask = it },
                    glassTreatment = glassTreatment
                )
            }
        }

        // ── Completed section ───────────────────────────────────────────
        if (filteredCompletedTasks.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { completedExpanded = !completedExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMPLETED (${filteredCompletedTasks.size})",
                    color = Color.White.copy(alpha = 0.30f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = if (completedExpanded) "▲" else "▼",
                    color = Color.White.copy(alpha = 0.30f),
                    fontSize = 10.sp
                )
            }

            if (completedExpanded) {
                filteredCompletedTasks.forEach { task ->
                    TaskGlassRow(
                        task = task,
                        active = false,
                        showCategoryLabel = selectedCategoryId == null,
                        categoryName = categories.firstOrNull { it.id == task.categoryId }?.name,
                        dimmed = true,
                        onToggleTask = onToggleTask,
                        onDeleteTask = onDeleteTask,
                        onTapRow = { detailSheetTask = it },
                        onFocusTask = {},
                        glassTreatment = glassTreatment
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFE53935).copy(alpha = 0.40f), RoundedCornerShape(16.dp))
                        .clickable { onDeleteAllCompleted(selectedCategoryId) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Delete All Completed",
                        color = Color(0xFFE53935).copy(alpha = 0.80f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
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
        AlertDialog(
            onDismissRequest = { deleteCategoryTarget = null },
            title = {
                Text(
                    text = "Delete ${category.name}?",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Tasks in this category will become uncategorized.",
                    color = Color.White.copy(alpha = 0.75f)
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
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCategoryTarget = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.70f))
                }
            },
            containerColor = Color(0xFF2A2035),
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
private fun CategoryPill(
    label: String,
    selected: Boolean,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    glassTreatment: GlassTreatment = GlassTreatment.Clear
) {
    Box(contentAlignment = Alignment.TopStart) {
        GlassPill(
            text = label,
            selected = selected,
            onClick = onClick,
            glassTreatment = glassTreatment
        )
        if (isEditMode && onDelete != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "×",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskGlassRow(
    task: FocusTask,
    active: Boolean,
    showCategoryLabel: Boolean,
    categoryName: String?,
    dimmed: Boolean = false,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onTapRow: (FocusTask) -> Unit,
    onFocusTask: (FocusTask) -> Unit,
    glassTreatment: GlassTreatment = GlassTreatment.Clear
) {
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
            val direction = dismissState.dismissDirection
            val isLeading = direction == SwipeToDismissBoxValue.StartToEnd
            val isTrailing = direction == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        when {
                            isLeading -> Color(0xFFFF8B6F)
                            isTrailing -> Color(0xFFE53935)
                            else -> Color.Transparent
                        }
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = if (isLeading) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                when {
                    isLeading -> Text(
                        text = "▶  Focus with Kuro",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    isTrailing -> Text(
                        text = "✕",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        content = {
            val rowAlpha = if (dimmed) 0.50f else 1.0f
            GlassCard(modifier = Modifier.fillMaxWidth(), active = active, glassTreatment = glassTreatment) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTapRow(task) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val checkFill = if (task.completed) Color(0xFFFF8B6F).copy(alpha = rowAlpha) else Color.White.copy(alpha = 0.10f)
                    val checkBorder = if (task.completed) Color.Transparent else Color.White.copy(alpha = 0.25f)
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(checkFill)
                            .border(1.dp, checkBorder, CircleShape)
                            .clickable(
                                role = Role.Checkbox,
                                onClickLabel = if (task.completed) "Unmark ${task.title}" else "Complete ${task.title}"
                            ) { onToggleTask(task.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.completed) {
                            Text(text = "✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = task.title,
                            color = if (task.completed) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = rowAlpha),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                        )
                        if (showCategoryLabel && categoryName != null) {
                            Text(
                                text = categoryName,
                                color = Color.White.copy(alpha = 0.35f),
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = "${task.minutes} min",
                                color = Color.White.copy(alpha = 0.45f * rowAlpha),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
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
        containerColor = Color(0xFF1A1228),
        contentColor = Color.White,
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
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Title field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "TITLE",
                    color = Color.White.copy(alpha = 0.40f),
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
                        .background(Color.White.copy(alpha = 0.08f)),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFFF8B6F),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            // Category selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "CATEGORY",
                    color = Color.White.copy(alpha = 0.40f),
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
                            .background(if (noneSelected) Color(0xFFFF8B6F).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                            .border(1.dp, if (noneSelected) Color(0xFFFF8B6F).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .clickable { categoryId = null }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "None",
                            color = if (noneSelected) Color(0xFFFF8B6F) else Color.White.copy(alpha = 0.55f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    categories.forEach { cat ->
                        val isSelected = categoryId == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color(0xFFFF8B6F).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                                .border(1.dp, if (isSelected) Color(0xFFFF8B6F).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                .clickable { categoryId = cat.id }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat.name,
                                color = if (isSelected) Color(0xFFFF8B6F) else Color.White.copy(alpha = 0.55f),
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
                    color = Color.White.copy(alpha = 0.40f),
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
                    Text(text = minutes.toString(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(24.dp))
                    StepperButton(label = "+", enabled = minutes < 180, onClick = { if (minutes < 180) minutes++ })
                }
            }

            // Planned sessions stepper
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "PLANNED SESSIONS",
                    color = Color.White.copy(alpha = 0.40f),
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
                    Text(text = plannedSessions.toString(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(24.dp))
                    StepperButton(label = "+", enabled = plannedSessions < 8, onClick = { if (plannedSessions < 8) plannedSessions++ })
                }
            }

            // Completed sessions (read-only)
            Text(
                text = "${task.completedSessions} of ${task.plannedSessions} sessions done",
                color = Color(0xFFFF8B6F).copy(alpha = 0.75f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Focus with Kuro button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFF8B6F))
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
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (enabled) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.30f),
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable { mutableStateOf(initialName) }
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1228),
        contentColor = Color.White,
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
            Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                placeholder = { Text("Category name", color = Color.White.copy(alpha = 0.35f)) },
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
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFFF8B6F),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFF8B6F))
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
                Text(text = "Confirm", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sessionCount by rememberSaveable { mutableIntStateOf(1) }
    var showDetail by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1228),
        contentColor = Color.White,
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
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Start focus session",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (sessionCount == 1) "1 session · $focusMinutes min"
                       else "$sessionCount sessions · $focusMinutes min each",
                color = Color(0xFFFF8B6F).copy(alpha = 0.85f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { showDetail = !showDetail }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = if (showDetail) "Hide detail ▲" else "Show detail ▼",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (showDetail) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Sessions",
                        color = Color.White.copy(alpha = 0.45f),
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
                                    if (sessionCount <= 1) Color.White.copy(alpha = 0.05f)
                                    else Color.White.copy(alpha = 0.12f)
                                )
                                .clickable { if (sessionCount > 1) sessionCount-- }
                                .semantics { role = Role.Button; contentDescription = "Decrease sessions" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "−",
                                color = if (sessionCount <= 1) Color.White.copy(alpha = 0.30f) else Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(text = sessionCount.toString(), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (sessionCount >= 8) Color.White.copy(alpha = 0.05f)
                                    else Color.White.copy(alpha = 0.12f)
                                )
                                .clickable { if (sessionCount < 8) sessionCount++ }
                                .semantics { role = Role.Button; contentDescription = "Increase sessions" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                color = if (sessionCount >= 8) Color.White.copy(alpha = 0.30f) else Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Duration per session comes from your Focus preference ($focusMinutes min)",
                        color = Color.White.copy(alpha = 0.30f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFF8B6F))
                    .clickable {
                        onStartFocus(task.copy(plannedSessions = sessionCount, completedSessions = 0))
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶  Start Focus",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
