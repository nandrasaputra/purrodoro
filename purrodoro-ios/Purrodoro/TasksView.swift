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
    @EnvironmentObject private var prefs: PreferencesStore
    @EnvironmentObject private var auth: AuthStore
    @EnvironmentObject private var ownerStore: AccountOwnerStore
    @EnvironmentObject private var syncRepo: SyncRepository
    @EnvironmentObject private var syncCoordinator: SyncCoordinator
    @EnvironmentObject private var engine: TimerEngine
    @EnvironmentObject private var habits: HabitStore
    @EnvironmentObject private var history: SessionHistoryStore
    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme
    @State private var editing: TaskItem?
    @State private var showAdd = false
    @State private var premiumUpsellTrigger: PremiumUpsellTrigger?
    @State private var showLoginSheet = false
    @State private var pendingUpsellAfterLogin: PremiumUpsellTrigger?
    @State private var showAccountSwitchAlert = false
    @State private var dropConfirmContinuation: CheckedContinuation<Bool, Never>?

    var onFocusTask: (TaskItem) -> Void = { _ in }

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }
    private var activeCount: Int { PremiumGate.activeTaskCount(store.tasks) }
    private var canAdd: Bool { PremiumGate.canAddActiveTask(activeCount: activeCount, isPremium: prefs.isPremium) }
    private var isGuest: Bool {
        if case .guest = auth.state { return true }
        return false
    }

    private var accountManager: AccountManager {
        AccountManager(
            ownerStore: ownerStore,
            clearer: AccountDataClearer(
                syncRepo: syncRepo,
                engine: engine,
                tasks: store,
                habits: habits,
                history: history,
                prefs: prefs
            ),
            syncHooks: syncCoordinator.syncHooks
        )
    }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            colors.page.ignoresSafeArea()

            content

            Button {
                if canAdd {
                    showAdd = true
                } else {
                    premiumUpsellTrigger = .activeTaskCap
                }
            } label: {
                Image(systemName: "plus")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 60, height: 60)
                    .background(canAdd ? Brand.coral : Brand.premiumPurpleBottom, in: Circle())
                    .shadow(color: Brand.coral.opacity(0.4), radius: 12, y: 6)
            }
            .padding(.trailing, 24)
            .padding(.bottom, 96)
            .accessibilityLabel(loc.t("tasks.add"))
        }
        .environment(\.pColors, colors)
        .sheet(isPresented: $showAdd) {
            TaskEditor(task: nil, suggestions: store.categories) { draft in
                store.add(
                    title: draft.title,
                    priority: draft.priority,
                    plannedSessions: draft.planned,
                    category: draft.category,
                    dueDate: draft.dueDate
                )
            }
        }
        .sheet(item: $editing) { task in
            TaskEditor(task: task, suggestions: store.categories) { draft in
                var updated = task
                updated.title = draft.title
                updated.priority = draft.priority
                updated.plannedSessions = max(draft.planned, updated.completedSessions, 1)
                let cat = draft.category?.trimmingCharacters(in: .whitespacesAndNewlines)
                updated.category = (cat?.isEmpty ?? true) ? nil : cat
                updated.dueDate = draft.dueDate
                store.update(updated)
            }
        }
        .sheet(item: $premiumUpsellTrigger) { trigger in
            PremiumUpsellSheet(
                trigger: trigger,
                isGuest: isGuest,
                onDismiss: { premiumUpsellTrigger = nil },
                onSignIn: {
                    pendingUpsellAfterLogin = trigger
                    premiumUpsellTrigger = nil
                    showLoginSheet = true
                }
            )
        }
        .sheet(isPresented: $showLoginSheet) {
            LoginSheet(
                onSignedIn: { user in
                    showLoginSheet = false
                    Task {
                        await handleSignedIn(user)
                        if let pending = pendingUpsellAfterLogin {
                            pendingUpsellAfterLogin = nil
                            premiumUpsellTrigger = pending
                        }
                    }
                },
                onContinueAsGuest: { showLoginSheet = false }
            )
        }
        .alert(loc.t("account.switch.title"), isPresented: $showAccountSwitchAlert) {
            Button(loc.t("account.switch.confirm"), role: .destructive) {
                dropConfirmContinuation?.resume(returning: true)
                dropConfirmContinuation = nil
            }
            Button(loc.t("account.switch.cancel"), role: .cancel) {
                dropConfirmContinuation?.resume(returning: false)
                dropConfirmContinuation = nil
            }
        } message: {
            Text(loc.t("account.switch.message"))
        }
    }

    @MainActor
    private func handleSignedIn(_ user: AuthUser) async {
        let result = await accountManager.onSignedIn(
            incomingUserId: user.userId,
            confirmDrop: {
                await withCheckedContinuation { continuation in
                    dropConfirmContinuation = continuation
                    showAccountSwitchAlert = true
                }
            },
            onBeforeClear: {
                if engine.isStarted { engine.stop() }
            }
        )
        if result == .dropCancelled {
            try? await auth.signOut()
        }
    }

    @ViewBuilder
    private var content: some View {
        if store.tasks.isEmpty {
            PlaceholderScreen(
                title: loc.t("tasks.emptyTitle"),
                subtitle: loc.t("tasks.emptySubtitle"),
                image: "CatChecklist"
            )
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(loc.t("tasks.title"))
                            .font(.system(.largeTitle, design: .rounded).weight(.bold))
                            .foregroundStyle(colors.textPrimary)
                        PremiumCapHeader(
                            visible: !prefs.isPremium,
                            current: activeCount,
                            limit: PremiumGate.freeActiveTaskLimit,
                            labelKey: "free_tier.tasks"
                        )
                    }
                        .padding(.top, 12)

                    if !store.activeTasks.isEmpty {
                        section(title: loc.t("tasks.active")) {
                            ForEach(store.activeTasks) { row(for: $0) }
                        }
                    }
                    if !store.completedTasks.isEmpty {
                        section(title: loc.t("tasks.done")) {
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
            Button { editing = task } label: { Label(loc.t("common.edit"), systemImage: "pencil") }
            Button(role: .destructive) { store.delete(task) } label: {
                Label(loc.t("common.delete"), systemImage: "trash")
            }
        }
    }
}

// MARK: - Row

private struct TaskRow: View {
    @EnvironmentObject private var loc: Localization
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
                    if let category = task.trimmedCategory {
                        categoryChip(category)
                    }
                    dueBadge
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

    private func categoryChip(_ name: String) -> some View {
        Text(name)
            .font(.system(.caption2, design: .rounded).weight(.semibold))
            .foregroundStyle(colors.textSecondary)
            .padding(.vertical, 3)
            .padding(.horizontal, 8)
            .background(CategoryPalette.color(for: name).opacity(0.35), in: Capsule())
    }

    @ViewBuilder
    private var dueBadge: some View {
        if task.isOverdue {
            dueLabel(loc.t("tasks.overdue"), tint: Brand.deepCoral, filled: true)
        } else if task.isDueToday {
            dueLabel(loc.t("tasks.dueToday"), tint: Brand.coral, filled: false)
        } else if let due = task.dueDate {
            dueLabel(Self.shortDate(due), tint: colors.textFaint, filled: false)
        }
    }

    private func dueLabel(_ text: String, tint: Color, filled: Bool) -> some View {
        Label(text, systemImage: "calendar")
            .font(.system(.caption2, design: .rounded).weight(.semibold))
            .labelStyle(.titleAndIcon)
            .foregroundStyle(filled ? .white : tint)
            .padding(.vertical, 3)
            .padding(.horizontal, 8)
            .background(filled ? tint : tint.opacity(0.14), in: Capsule())
    }

    private static func shortDate(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "MMM d"
        return f.string(from: date)
    }
}

private struct PriorityChip: View {
    @EnvironmentObject private var loc: Localization
    let priority: TaskPriority

    var body: some View {
        Text(loc.t("priority.\(priority.rawValue)"))
            .font(.system(.caption2, design: .rounded).weight(.bold))
            .foregroundStyle(Brand.cocoa)
            .padding(.vertical, 3)
            .padding(.horizontal, 8)
            .background(priority.color, in: Capsule())
    }
}

// MARK: - Editor

struct TaskDraft {
    var title: String
    var priority: TaskPriority
    var planned: Int
    var category: String?
    var dueDate: Date?
}

private struct TaskEditor: View {
    let task: TaskItem?
    let suggestions: [String]
    let onSave: (TaskDraft) -> Void

    @EnvironmentObject private var loc: Localization
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme
    @State private var title: String
    @State private var priority: TaskPriority
    @State private var planned: Int
    @State private var category: String
    @State private var hasDueDate: Bool
    @State private var dueDate: Date

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    init(task: TaskItem?, suggestions: [String], onSave: @escaping (TaskDraft) -> Void) {
        self.task = task
        self.suggestions = suggestions
        self.onSave = onSave
        _title = State(initialValue: task?.title ?? "")
        _priority = State(initialValue: task?.priority ?? .normal)
        _planned = State(initialValue: task?.plannedSessions ?? 1)
        _category = State(initialValue: task?.trimmedCategory ?? "")
        _hasDueDate = State(initialValue: task?.dueDate != nil)
        _dueDate = State(initialValue: task?.dueDate ?? Date())
    }

    var body: some View {
        NavigationStack {
            ZStack {
                colors.page.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 16) {
                        TextField(loc.t("tasks.whatFocus"), text: $title, axis: .vertical)
                            .font(.system(.body, design: .rounded))
                            .padding(16)
                            .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(colors.border, lineWidth: 1))

                        card {
                            VStack(alignment: .leading, spacing: 10) {
                                Text(loc.t("tasks.priority"))
                                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                                    .foregroundStyle(colors.textSecondary)
                                Picker(loc.t("tasks.priority"), selection: $priority) {
                                    ForEach(TaskPriority.allCases) { Text(loc.t("priority.\($0.rawValue)")).tag($0) }
                                }
                                .pickerStyle(.segmented)
                            }
                        }

                        categoryCard
                        dueDateCard

                        card {
                            Stepper(value: $planned, in: 1...8) {
                                HStack {
                                    Text(loc.t("tasks.plannedSessions"))
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
            .navigationTitle(task == nil ? loc.t("tasks.new") : loc.t("tasks.edit"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(loc.t("common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(loc.t("common.save")) {
                        onSave(TaskDraft(
                            title: title,
                            priority: priority,
                            planned: planned,
                            category: category,
                            dueDate: hasDueDate ? dueDate : nil
                        ))
                        dismiss()
                    }
                    .font(.system(.body, design: .rounded).weight(.bold))
                    .disabled(title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }

    private var categoryCard: some View {
        card {
            VStack(alignment: .leading, spacing: 10) {
                Text(loc.t("tasks.category"))
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textSecondary)
                TextField(loc.t("tasks.categoryPlaceholder"), text: $category)
                    .font(.system(.body, design: .rounded))
                    .textInputAutocapitalization(.words)
                if !suggestions.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(suggestions, id: \.self) { name in
                                Button { category = name } label: {
                                    Text(name)
                                        .font(.system(.caption, design: .rounded).weight(.semibold))
                                        .foregroundStyle(colors.textSecondary)
                                        .padding(.vertical, 5)
                                        .padding(.horizontal, 10)
                                        .background(CategoryPalette.color(for: name).opacity(0.3), in: Capsule())
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
            }
        }
    }

    private var dueDateCard: some View {
        card {
            VStack(alignment: .leading, spacing: 10) {
                Toggle(isOn: $hasDueDate.animation()) {
                    Text(loc.t("tasks.dueDate"))
                        .font(.system(.body, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textPrimary)
                }
                .tint(Brand.coral)
                if hasDueDate {
                    DatePicker("", selection: $dueDate, displayedComponents: .date)
                        .labelsHidden()
                        .datePickerStyle(.graphical)
                        .tint(Brand.coral)
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
    TasksView()
        .environmentObject(TaskStore())
        .environmentObject(Localization())
}
