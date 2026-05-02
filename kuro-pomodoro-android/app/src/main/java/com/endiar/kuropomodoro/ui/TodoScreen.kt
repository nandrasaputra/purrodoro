package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.endiar.kuropomodoro.model.FocusTask
import com.endiar.kuropomodoro.model.TodoListState
import com.endiar.kuropomodoro.ui.components.KuroCard
import com.endiar.kuropomodoro.ui.components.KuroPrimaryButton
import com.endiar.kuropomodoro.ui.components.SectionTitle
import com.endiar.kuropomodoro.ui.theme.Cocoa

@Composable
fun TodoScreen(
    todoLists: List<TodoListState>,
    selectedListId: Int,
    onListSelected: (Int) -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedList = todoLists.firstOrNull { it.id == selectedListId } ?: todoLists.firstOrNull()
    var newTaskTitle by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val addTaskIfValid = {
        val title = newTaskTitle.trim()
        if (title.isNotEmpty()) {
            onAddTask(title)
            newTaskTitle = ""
            keyboardController?.hide()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(
            title = "Todo nest",
            subtitle = "Keep Kuro's next focus bites nearby"
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            todoLists.forEach { list ->
                TodoListTab(
                    name = list.name,
                    selected = list.id == selectedList?.id,
                    onClick = { onListSelected(list.id) }
                )
            }
        }

        KuroCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = selectedList?.name ?: "Tasks",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = newTaskTitle,
                onValueChange = { newTaskTitle = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Add a focus task") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { addTaskIfValid() })
            )
            Spacer(Modifier.height(12.dp))
            KuroPrimaryButton(
                text = "Add task",
                onClick = addTaskIfValid,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (selectedList?.tasks.isNullOrEmpty()) {
            EmptyTodoState()
        } else {
            selectedList.tasks.forEach { task ->
                TaskRow(
                    task = task,
                    onToggleTask = onToggleTask,
                    onDeleteTask = onDeleteTask
                )
            }
        }
    }
}

@Composable
private fun TodoListTab(name: String, selected: Boolean, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) colorScheme.secondary else colorScheme.surface
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = if (selected) Cocoa else colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun TaskRow(task: FocusTask, onToggleTask: (Int) -> Unit, onDeleteTask: (Int) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    KuroCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = task.completed,
                    role = Role.Checkbox,
                    onValueChange = { onToggleTask(task.id) }
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = null
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${task.minutes} min focus buddy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
            TextButton(
                onClick = { onDeleteTask(task.id) },
                modifier = Modifier.semantics {
                    contentDescription = "Delete ${task.title}"
                }
            ) {
                Text("Delete", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun EmptyTodoState() {
    KuroCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "No tasks yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Add one gentle task for Kuro to guard while you focus.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
        )
    }
}
