//
//  TasksView.swift
//  Purrodoro
//
//  The Tasks tab: a calm list of focus tasks grouped into Active and Done.
//  Tasks can be completed, edited, deleted, and made the active focus so a
//  session counts toward their planned sessions.
//

import SwiftUI

struct TasksView: View {
    @EnvironmentObject private var store: TaskStore
    @Environment(\.colorScheme) private var scheme
    @State private var editing: TaskItem?
    @State private var showAdd = false

    var onFocusTask: (TaskItem) -> Void = { _ in }

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            colors.page.ignoresSafeArea()

            content

            Button {
                showAdd = true
            } label: {
                Image(systemName: "plus")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 60, height: 60)
                    .background(Brand.coral, in: Circle())
                    .shadow(color: Brand.coral.opacity(0.4), radius: 12, y: 6)
            }
            .padding(.trailing, 24)
            .padding(.bottom, 96)
            .accessibilityLabel("Add task")
        }
        .environment(\.pColors, colors)
        .sheet(isPresented: $showAdd) {
            TaskEditor(task: nil) { store.add(title: $0, priority: $1, plannedSessions: $2) }
        }
        .sheet(item: $editing) { task in
            TaskEditor(task: task) { title, priority, planned in
                var updated = task
                updated.title = title
                updated.priority = priority
                updated.plannedSessions = max(planned, updated.completedSessions, 1)
                store.update(updated)
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        if store.tasks.isEmpty {
            PlaceholderScreen(
                title: "No tasks yet",
                subtitle: "Add a task and attach it to a focus session to track your progress.",
                image: "CatChecklist"
            )
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("Tasks")
                        .font(.system(.largeTitle, design: .rounded).weight(.bold))
                        .foregroundStyle(colors.textPrimary)
                        .padding(.top, 12)

                    if !store.activeTasks.isEmpty {
                        section(title: "Active") {
                            ForEach(store.activeTasks) { row(for: $0) }
                        }
                    }
                    if !store.completedTasks.isEmpty {
                        section(title: "Done") {
                            ForEach(store.completedTasks) { row(for: $0) }
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 170)
            }
        }
    }

    private func section<C: View>(title: String, @ViewBuilder _ rows: () -> C) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title.uppercased())
                .font(.system(.caption, design: .rounded).weight(.bold))
                .tracking(1)
                .foregroundStyle(colors.textFaint)
            rows()
        }
    }

    private func row(for task: TaskItem) -> some View {
        TaskRow(
            task: task,
            isActive: store.activeTaskID == task.id,
            colors: colors,
            onToggle: { store.toggleCompleted(task) },
            onFocus: { onFocusTask(task) },
            onEdit: { editing = task }
        )
        .contextMenu {
            Button { editing = task } label: { Label("Edit", systemImage: "pencil") }
            Button(role: .destructive) { store.delete(task) } label: {
                Label("Delete", systemImage: "trash")
            }
        }
    }
}

// MARK: - Row

private struct TaskRow: View {
    let task: TaskItem
    let isActive: Bool
    let colors: PurrodoroColors
    let onToggle: () -> Void
    let onFocus: () -> Void
    let onEdit: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Button(action: onToggle) {
                Image(systemName: task.completed ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 24))
                    .foregroundStyle(task.completed ? Brand.coral : colors.textFaint)
            }
            .buttonStyle(.plain)

            VStack(alignment: .leading, spacing: 5) {
                Text(task.title)
                    .font(.system(.body, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textPrimary)
                    .strikethrough(task.completed, color: colors.textFaint)
                    .lineLimit(2)

                HStack(spacing: 8) {
                    PriorityChip(priority: task.priority)
                    if task.plannedSessions > 0 {
                        Label("\(task.completedSessions)/\(task.plannedSessions)", systemImage: "target")
                            .font(.system(.caption2, design: .rounded).weight(.semibold))
                            .foregroundStyle(colors.textFaint)
                            .labelStyle(.titleAndIcon)
                    }
                }
            }

            Spacer()

            if !task.completed {
                Button(action: onFocus) {
                    Image(systemName: isActive ? "play.circle.fill" : "play.circle")
                        .font(.system(size: 28))
                        .foregroundStyle(isActive ? Brand.coral : colors.textSecondary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(isActive ? "Active focus task" : "Focus on this task")
            }
        }
        .padding(14)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(isActive ? Brand.coral : colors.border, lineWidth: isActive ? 2 : 1)
        )
        .contentShape(Rectangle())
        .onTapGesture(perform: onEdit)
    }
}

private struct PriorityChip: View {
    let priority: TaskPriority

    var body: some View {
        Text(priority.label)
            .font(.system(.caption2, design: .rounded).weight(.bold))
            .foregroundStyle(Brand.cocoa)
            .padding(.vertical, 3)
            .padding(.horizontal, 8)
            .background(priority.color, in: Capsule())
    }
}

// MARK: - Editor

private struct TaskEditor: View {
    let task: TaskItem?
    let onSave: (String, TaskPriority, Int) -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme
    @State private var title: String
    @State private var priority: TaskPriority
    @State private var planned: Int

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    init(task: TaskItem?, onSave: @escaping (String, TaskPriority, Int) -> Void) {
        self.task = task
        self.onSave = onSave
        _title = State(initialValue: task?.title ?? "")
        _priority = State(initialValue: task?.priority ?? .normal)
        _planned = State(initialValue: task?.plannedSessions ?? 1)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                colors.page.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 16) {
                        TextField("What do you want to focus on?", text: $title, axis: .vertical)
                            .font(.system(.body, design: .rounded))
                            .padding(16)
                            .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(colors.border, lineWidth: 1))

                        card {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("Priority")
                                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                                    .foregroundStyle(colors.textSecondary)
                                Picker("Priority", selection: $priority) {
                                    ForEach(TaskPriority.allCases) { Text($0.label).tag($0) }
                                }
                                .pickerStyle(.segmented)
                            }
                        }

                        card {
                            Stepper(value: $planned, in: 1...8) {
                                HStack {
                                    Text("Planned sessions")
                                        .font(.system(.body, design: .rounded).weight(.semibold))
                                        .foregroundStyle(colors.textPrimary)
                                    Spacer()
                                    Text("\(planned)")
                                        .font(.system(.body, design: .rounded).weight(.bold))
                                        .foregroundStyle(Brand.coral)
                                        .monospacedDigit()
                                }
                            }
                        }
                    }
                    .padding(20)
                }
            }
            .navigationTitle(task == nil ? "New task" : "Edit task")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        onSave(title, priority, planned)
                        dismiss()
                    }
                    .font(.system(.body, design: .rounded).weight(.bold))
                    .disabled(title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }

    private func card<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        content()
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(colors.border, lineWidth: 1))
    }
}

#Preview {
    TasksView().environmentObject(TaskStore())
}
