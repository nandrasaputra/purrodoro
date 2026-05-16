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
import com.endiar.kuropomodoro.ui.theme.kuroTokens
import com.endiar.kuropomodoro.ui.theme.luminance
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
private fun tagPaletteFor(categoryId: Int?): Pair<Color, Color> {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val presets = if (isLight) listOf(
        Pair(Color(0xFFEFE6FF), Color(0xFF5B3F8C)),
        Pair(Color(0xFFDDF1E5), Color(0xFF3F7A4F)),
        Pair(Color(0xFFFFE2D5), Color(0xFF8C432F)),
        Pair(Color(0xFFDDE7F5), Color(0xFF3F567A)),
        Pair(Color(0xFFFFD8DF), Color(0xFF8C3F4F)),
    ) else listOf(
        Pair(Color(0xFF3F2A4A), Color(0xFFD8C8FF)),
        Pair(Color(0xFF1D3326), Color(0xFF9DD8B3)),
        Pair(Color(0xFF4A2218), Color(0xFFFFB59E)),
        Pair(Color(0xFF1F2A3F), Color(0xFFAFC6E0)),
        Pair(Color(0xFF3A1F25), Color(0xFFFFB6C1)),
    )
    val idx = ((categoryId ?: 0).hashCode() and Int.MAX_VALUE) % presets.size
    return presets[idx]
}


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
                    color = tokens.textPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateLabel,
                    color = tokens.textSecondary,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tokens.accent)
                    .clickable { addingTask = !addingTask },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", color = tokens.accentOn, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
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
                    glassTreatment = glassTreatment,
                    overBackdrop = overBackdrop,
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
                        glassTreatment = glassTreatment,
                        overBackdrop = overBackdrop,
                    )
                }
                if (isEditMode) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, tokens.accentSoft, RoundedCornerShape(20.dp))
                            .clickable { addCategorySheetVisible = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ New",
                            color = tokens.accent,
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
                    .background(tokens.borderSoft)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (isEditMode) "Done" else "Edit",
                color = tokens.textSecondary,
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
                    .background(tokens.cardFill)
                    .border(1.dp, tokens.borderSoft, RoundedCornerShape(18.dp))
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
                                color = tokens.textMuted,
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitTask() }),
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.accent)
                            .clickable { submitTask() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(text = "Add", color = tokens.accentOn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                                    if (newTaskCategoryId == null) tokens.accentSoft
                                    else tokens.segmentedTrack
                                )
                                .border(
                                    1.dp,
                                    if (newTaskCategoryId == null) tokens.accent.copy(alpha = 0.60f)
                                    else tokens.borderSoft,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { newTaskCategoryId = null }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "None",
                                color = if (newTaskCategoryId == null) tokens.accent else tokens.textSecondary,
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
                                        if (isSelected) tokens.accentSoft
                                        else tokens.segmentedTrack
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) tokens.accent.copy(alpha = 0.60f)
                                        else tokens.borderSoft,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { newTaskCategoryId = cat.id }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = cat.name,
                                    color = if (isSelected) tokens.accent else tokens.textSecondary,
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
            color = tokens.textFaint,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )

        if (filteredActiveTasks.isEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                glassTreatment = glassTreatment,
                overBackdrop = overBackdrop,
            ) {
                Text(
                    text = "No tasks yet",
                    color = tokens.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tap + to add your first focus task.",
                    color = tokens.textSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            filteredActiveTasks.forEach { task ->
                TaskGlassRow(
                    task = task,
                    active = false,
                    showCategoryLabel = selectedCategoryId == null,
                    categoryName = categories.firstOrNull { it.id == task.categoryId }?.name,
                    onToggleTask = onToggleTask,
                    onDeleteTask = onDeleteTask,
                    onTapRow = { detailSheetTask = it },
                    onFocusTask = { if (!task.completed) focusSheetTask = it },
                    glassTreatment = glassTreatment,
                    overBackdrop = overBackdrop,
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
                    color = tokens.textFaint,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = if (completedExpanded) "▲" else "▼",
                    color = tokens.textFaint,
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
                        glassTreatment = glassTreatment,
                        overBackdrop = overBackdrop,
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
                        color = Color(0xFFE53935),
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
private fun CategoryPill(
    label: String,
    selected: Boolean,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    overBackdrop: Boolean = true,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Box(contentAlignment = Alignment.TopStart) {
        GlassPill(
            text = label,
            selected = selected,
            onClick = onClick,
            glassTreatment = glassTreatment,
            overBackdrop = overBackdrop,
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
                    color = tokens.accentOn,
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
    glassTreatment: GlassTreatment = GlassTreatment.Clear,
    overBackdrop: Boolean = true,
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
            val direction = dismissState.dismissDirection
            val isLeading = direction == SwipeToDismissBoxValue.StartToEnd
            val isTrailing = direction == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        when {
                            isLeading -> tokens.accent
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
                        color = tokens.accentOn,
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
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                active = active,
                glassTreatment = glassTreatment,
                overBackdrop = overBackdrop,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTapRow(task) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val checkFill = if (task.completed) tokens.accent.copy(alpha = rowAlpha) else tokens.segmentedTrack
                    val checkBorder = if (task.completed) Color.Transparent else tokens.borderSoft
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
                            Text(text = "✓", color = tokens.accentOn, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = task.title,
                            color = if (task.completed) tokens.textMuted else tokens.textPrimary.copy(alpha = rowAlpha),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                        )
                        if (showCategoryLabel && categoryName != null) {
                            val (tagBg, tagText) = tagPaletteFor(task.categoryId)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(tagBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = categoryName,
                                    color = tagText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Text(
                                text = "${task.minutes} min",
                                color = tokens.textMuted.copy(alpha = rowAlpha),
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
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
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
            Text(text = title, color = tokens.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                Text(text = "Confirm", color = tokens.accentOn, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Start focus session",
                color = tokens.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
