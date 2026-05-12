package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.TodoListState
import com.endiar.kuropomodoro.ui.components.GlassCard
import com.endiar.kuropomodoro.ui.components.GlassPill
import com.endiar.kuropomodoro.model.GlassTreatment
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodoScreen(
    todoLists: List<TodoListState>,
    selectedListId: Int,
    onListSelected: (Int) -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onFocusTask: (FocusTask) -> Unit,
    modifier: Modifier = Modifier,
    glassTreatment: GlassTreatment = GlassTreatment.Clear
) {
    val selectedList = todoLists.firstOrNull { it.id == selectedListId } ?: todoLists.firstOrNull()
    var newTaskTitle by rememberSaveable { mutableStateOf("") }
    var addingTask by rememberSaveable { mutableStateOf(false) }
    var focusSheetTask by remember { mutableStateOf<FocusTask?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val today = LocalDate.now()
    val dateLabel = today.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))

    val submitTask = {
        val title = newTaskTitle.trim()
        if (title.isNotEmpty()) {
            onAddTask(title)
            newTaskTitle = ""
            addingTask = false
            keyboardController?.hide()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header row
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

        // List switcher
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            todoLists.forEach { list ->
                GlassPill(
                    text = list.name,
                    selected = list.id == selectedList?.id,
                    onClick = { onListSelected(list.id) },
                    glassTreatment = glassTreatment
                )
            }
        }

        // Inline add-task input (visible when + is tapped)
        if (addingTask) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F0A1A).copy(alpha = 0.40f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
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
        }

        // Section label
        Text(
            text = "UPCOMING",
            color = Color.White.copy(alpha = 0.30f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )

        // Task list
        if (selectedList?.tasks.isNullOrEmpty()) {
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
            val firstActiveTask = selectedList!!.tasks.firstOrNull { !it.completed }
            selectedList.tasks.forEach { task ->
                TaskGlassRow(
                    task = task,
                    active = task == firstActiveTask,
                    onToggleTask = onToggleTask,
                    onDeleteTask = onDeleteTask,
                    onFocusTask = { focusSheetTask = it },
                    glassTreatment = glassTreatment
                )
            }
        }
    }

    focusSheetTask?.let { task ->
        PreSessionSheet(
            task = task,
            onDismiss = { focusSheetTask = null },
            onStartFocus = { updatedTask ->
                focusSheetTask = null
                onFocusTask(updatedTask)
            }
        )
    }
}

@Composable
private fun TaskGlassRow(
    task: FocusTask,
    active: Boolean,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onFocusTask: (FocusTask) -> Unit,
    glassTreatment: GlassTreatment = GlassTreatment.Clear
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), active = active, glassTreatment = glassTreatment) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Checkbox,
                    onClickLabel = if (task.completed) "Unmark ${task.title}" else "Complete ${task.title}"
                ) { onToggleTask(task.id) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Circular checkbox
            val checkFill = if (task.completed) Color(0xFFFF8B6F) else Color.White.copy(alpha = 0.10f)
            val checkBorder = if (task.completed) Color.Transparent else Color.White.copy(alpha = 0.25f)
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(checkFill)
                    .border(1.dp, checkBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (task.completed) {
                    Text(text = "✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = task.title,
                    color = if (task.completed) Color.White.copy(alpha = 0.40f) else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = "${task.minutes} min",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp
                )
            }
            Text(
                text = "✕",
                color = Color.White.copy(alpha = 0.30f),
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Delete ${task.title}"
                    ) { onDeleteTask(task.id) }
                    .semantics { contentDescription = "Delete ${task.title}" }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreSessionSheet(
    task: FocusTask,
    onDismiss: () -> Unit,
    onStartFocus: (FocusTask) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sessionCount by remember { mutableIntStateOf(1) }

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
            // Task name subtitle
            Text(
                text = task.title,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            // Heading
            Text(
                text = "How many sessions?",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            // Stepper
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { if (sessionCount > 1) sessionCount-- },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "−", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = sessionCount.toString(),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { if (sessionCount < 8) sessionCount++ },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Start Focus button
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
