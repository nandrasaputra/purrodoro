package com.endiar.kuropomodoro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.StackedBarChart
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.endiar.kuropomodoro.model.Habit
import com.endiar.kuropomodoro.model.TimerSettings
import com.endiar.kuropomodoro.model.effectiveTarget
import com.endiar.kuropomodoro.model.isScheduledOn
import com.endiar.kuropomodoro.model.progressOn
import com.endiar.kuropomodoro.ui.designsystem.KuroInputField
import com.endiar.kuropomodoro.ui.designsystem.KuroSpacing
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
)

sealed class StartFocusPrefill {
    data class Task(val taskId: Int) : StartFocusPrefill()
    data class Habit(val habitId: Int) : StartFocusPrefill()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartFocusSheet(
    settings: TimerSettings,
    tasks: List<FocusTask>,
    habits: List<Habit>,
    today: LocalDate,
    onDismiss: () -> Unit,
    onEditConfig: () -> Unit,
    onStartFocus: (StartFocusRequest) -> Unit,
    prefill: StartFocusPrefill? = null,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialIntent = when (prefill) {
        is StartFocusPrefill.Task -> FocusIntent.Task
        is StartFocusPrefill.Habit -> FocusIntent.Habit
        null -> FocusIntent.Empty
    }
    val initialTaskId = (prefill as? StartFocusPrefill.Task)?.taskId ?: -1
    val initialHabitId = (prefill as? StartFocusPrefill.Habit)?.habitId ?: -1

    var intent by rememberSaveable { mutableStateOf(initialIntent) }
    var noteText by rememberSaveable { mutableStateOf("") }
    var selectedTaskId by rememberSaveable { mutableIntStateOf(initialTaskId) }
    var selectedHabitId by rememberSaveable { mutableIntStateOf(initialHabitId) }

    val incompleteTasks = tasks.filter { !it.completed }
    val todayHabits = habits.filter { it.isScheduledOn(today) }

    fun canStart(): Boolean = when (intent) {
        FocusIntent.Empty -> true
        FocusIntent.SimpleNote -> noteText.trim().isNotEmpty()
        FocusIntent.Task -> selectedTaskId >= 0 && tasks.any { it.id == selectedTaskId && !it.completed }
        FocusIntent.Habit -> selectedHabitId >= 0 && todayHabits.any { it.id == selectedHabitId }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.cardFill,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = KuroSpacing.sm, bottom = KuroSpacing.xs)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(KuroSpacing.radiusPill))
                    .background(tokens.cardBorder),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KuroSpacing.xl)
                .padding(bottom = KuroSpacing.xxl)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
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
                color = tokens.textPrimary,
            )
            Spacer(Modifier.height(KuroSpacing.xs))
            Text(
                text = "What are you working on?",
                style = KuroType.bodySecondary,
                color = tokens.textSecondary,
            )
            Spacer(Modifier.height(KuroSpacing.xl))

            IntentSection(
                selected = intent,
                onSelect = { intent = it },
            )

            when (intent) {
                FocusIntent.SimpleNote -> {
                    Spacer(Modifier.height(KuroSpacing.md))
                    KuroInputField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = "Write a quick note…",
                    )
                }

                FocusIntent.Task -> {
                    Spacer(Modifier.height(KuroSpacing.md))
                    IntentPickerSection(
                        emptyMessage = "No open tasks yet. Add one from the Tasks tab.",
                        options = incompleteTasks.map { it.id to it.title },
                        selectedId = selectedTaskId,
                        onSelect = { selectedTaskId = it },
                    )
                }

                FocusIntent.Habit -> {
                    Spacer(Modifier.height(KuroSpacing.md))
                    IntentPickerSection(
                        emptyMessage = "No habits scheduled for today.",
                        options = todayHabits.map { it.id to "${it.emoji} ${it.name}" },
                        selectedId = selectedHabitId,
                        onSelect = { selectedHabitId = it },
                    )
                }

                FocusIntent.Empty -> Unit
            }

            Spacer(Modifier.height(KuroSpacing.xl))
            DefaultConfigSection(
                settings = settings,
                onEdit = onEditConfig,
            )
            Spacer(Modifier.height(KuroSpacing.xl))

            StartFocusConfirmButton(
                enabled = canStart(),
                onClick = {
                    val request = when (intent) {
                        FocusIntent.Empty -> StartFocusRequest(task = null)
                        FocusIntent.SimpleNote -> StartFocusRequest(
                            task = FocusTask(
                                id = -1,
                                title = noteText.trim(),
                                minutes = 0,
                            ),
                        )
                        FocusIntent.Task -> StartFocusRequest(
                            task = tasks.first { it.id == selectedTaskId },
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
                            )
                        }
                    }
                    onStartFocus(request)
                },
            )
        }
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
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp),
                )
            }
        }
    }
}

@Composable
private fun IntentPickerSection(
    emptyMessage: String,
    options: List<Pair<Int, String>>,
    selectedId: Int,
    onSelect: (Int) -> Unit,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    if (options.isEmpty()) {
        Text(
            text = emptyMessage,
            style = KuroType.bodySecondary,
            color = tokens.textSecondary,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.sm),
    ) {
        options.forEach { (id, label) ->
            val selected = id == selectedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(KuroSpacing.radiusMd))
                    .background(if (selected) tokens.accentSoft else tokens.pageBackground)
                    .border(
                        1.dp,
                        if (selected) tokens.accent.copy(alpha = 0.35f) else tokens.cardBorder,
                        RoundedCornerShape(KuroSpacing.radiusMd),
                    )
                    .clickable { onSelect(id) }
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
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = tokens.accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
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
            TextButton(onClick = onEdit) {
                Text(
                    text = "Edit",
                    style = KuroType.labelStrong,
                    color = tokens.accent,
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KuroSpacing.radiusMd))
                .background(tokens.pageBackground)
                .border(1.dp, tokens.cardBorder, RoundedCornerShape(KuroSpacing.radiusMd))
                .padding(KuroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(KuroSpacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
            ) {
                ConfigCell(
                    icon = Icons.Outlined.AccessTime,
                    label = "Focus",
                    value = "${settings.focusMinutes} min",
                    modifier = Modifier.weight(1f),
                )
                ConfigCell(
                    icon = Icons.Outlined.LocalCafe,
                    label = "Short break",
                    value = "${settings.shortBreakMinutes} min",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KuroSpacing.md),
            ) {
                ConfigCell(
                    icon = Icons.Outlined.LocalCafe,
                    label = "Long break",
                    value = "${settings.longBreakMinutes} min",
                    modifier = Modifier.weight(1f),
                )
                ConfigCell(
                    icon = Icons.Outlined.StackedBarChart,
                    label = "Sessions",
                    value = settings.focusSessionsPerCycle.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ConfigCell(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.colorScheme.kuroTokens
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(KuroSpacing.xs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tokens.textSecondary,
            modifier = Modifier.size(18.dp),
        )
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

@Composable
private fun ColumnScope.StartFocusConfirmButton(
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
            imageVector = Icons.Outlined.PlayArrow,
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
    FocusIntent.Habit -> Icons.Outlined.Spa
    FocusIntent.Task -> Icons.Outlined.TaskAlt
    FocusIntent.SimpleNote -> Icons.Outlined.EditNote
    FocusIntent.Empty -> Icons.Outlined.CropSquare
}
