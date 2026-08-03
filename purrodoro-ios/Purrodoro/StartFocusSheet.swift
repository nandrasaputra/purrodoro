//
//  StartFocusSheet.swift
//  Purrodoro
//
//  A confirmation sheet shown before a focus cycle begins (ported from the
//  Android `StartFocusSheet`). Lets the user pick an intent (none / note /
//  task / habit) and adjust this session's durations before starting — without
//  touching the saved defaults. Also powers the "continue where you left off"
//  flow by accepting a leftover focus length to pre-fill.
//

import SwiftUI

/// Optional pre-fill for the sheet, used by the interrupted-focus resume path.
struct StartFocusPrefill: Equatable {
    var focusMinutesOverride: Int?
    var keepActiveIntent: Bool = true
}

private enum FocusIntentKind: String, CaseIterable, Identifiable {
    case none, note, task, habit
    var id: String { rawValue }
}

struct StartFocusSheet: View {
    @EnvironmentObject private var engine: TimerEngine
    @EnvironmentObject private var tasks: TaskStore
    @EnvironmentObject private var habits: HabitStore
    @EnvironmentObject private var prefs: PreferencesStore
    @EnvironmentObject private var loc: Localization
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme

    var prefill: StartFocusPrefill? = nil

    @State private var intent: FocusIntentKind = .none
    @State private var noteText: String = ""
    @State private var selectedTaskID: UUID?
    @State private var selectedHabitID: UUID?
    @State private var sessionSettings = TimerSettings()
    @State private var didSeed = false
    @State private var showFocusShieldSettings = false

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }
    private var today: Date { Date() }

    private var scheduledHabits: [HabitItem] {
        habits.habits.filter { $0.isScheduled(on: today) }
    }

    private var canStart: Bool {
        switch intent {
        case .none: return true
        case .note: return !noteText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        case .task: return selectedTaskID != nil
        case .habit: return selectedHabitID != nil
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                colors.page.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 18) {
                        mascot
                        intentSection
                        intentPicker
                        configCard
                        FocusShieldStartFocusRow {
                            showFocusShieldSettings = true
                        }
                    }
                    .padding(20)
                    .padding(.bottom, 90)
                }
                startBar
            }
            .navigationTitle(loc.t("start.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(loc.t("common.cancel")) { dismiss() }
                }
            }
            .navigationDestination(isPresented: $showFocusShieldSettings) {
                FocusShieldView()
            }
        }
        .environment(\.pColors, colors)
        .onAppear(perform: seedIfNeeded)
    }

    private func seedIfNeeded() {
        guard !didSeed else { return }
        didSeed = true
        var base = engine.settings
        if let override = prefill?.focusMinutesOverride {
            base = base.updating(.focus, minutes: override)
        }
        sessionSettings = base
        // Carry over any already-active intent so resuming keeps its context.
        if prefill?.keepActiveIntent ?? true {
            if let active = tasks.activeTask {
                intent = .task
                selectedTaskID = active.id
            } else if let active = habits.activeHabit {
                intent = .habit
                selectedHabitID = active.id
            } else if let note = prefs.activeSimpleNote, !note.isEmpty {
                intent = .note
                noteText = note
            }
        }
    }

    // MARK: Sections

    private var mascot: some View {
        VStack(spacing: 8) {
            Image("CatReady")
                .resizable()
                .scaledToFit()
                .frame(height: 110)
                .accessibilityHidden(true)
            Text(loc.t("start.whatWorkingOn"))
                .font(.system(.subheadline, design: .rounded))
                .foregroundStyle(colors.textSecondary)
        }
    }

    private var intentSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(loc.t("start.intentOptional").uppercased())
                .font(.system(.caption, design: .rounded).weight(.bold))
                .tracking(1)
                .foregroundStyle(colors.textFaint)
                .frame(maxWidth: .infinity, alignment: .leading)
            HStack(spacing: 8) {
                intentChip(.none, symbol: "timer", label: loc.t("start.none"))
                intentChip(.note, symbol: "pencil.line", label: loc.t("start.note"))
                intentChip(.task, symbol: "checkmark.circle", label: loc.t("start.task"))
                intentChip(.habit, symbol: "leaf", label: loc.t("start.habit"))
            }

            if intent == .note {
                TextField(loc.t("start.notePlaceholder"), text: $noteText)
                    .font(.system(.body, design: .rounded))
                    .padding(14)
                    .background(colors.card, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .stroke(colors.border, lineWidth: 1)
                    )
            }
        }
    }

    private func intentChip(_ kind: FocusIntentKind, symbol: String, label: String) -> some View {
        let selected = intent == kind
        return Button {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                intent = kind
                if kind == .none || kind == .note {
                    selectedTaskID = nil
                    selectedHabitID = nil
                }
            }
        } label: {
            VStack(spacing: 6) {
                Image(systemName: symbol)
                    .font(.system(size: 16, weight: .semibold))
                Text(label)
                    .font(.system(.caption2, design: .rounded).weight(.semibold))
                    .lineLimit(2)
                    .minimumScaleFactor(0.8)
                    .multilineTextAlignment(.center)
            }
            .foregroundStyle(selected ? Brand.coral : colors.textSecondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .padding(.horizontal, 4)
            .background {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(selected ? Brand.coral.opacity(0.14) : colors.card)
            }
            .overlay {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(selected ? Brand.coral.opacity(0.35) : colors.border, lineWidth: 1)
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(loc.t("a11y.intentKind", label))
        .accessibilityAddTraits(selected ? [.isButton, .isSelected] : .isButton)
    }

    @ViewBuilder
    private var intentPicker: some View {
        switch intent {
        case .none, .note:
            EmptyView()
        case .task:
            pickerCard(title: loc.t("start.chooseTask"), empty: loc.t("start.noTasks")) {
                ForEach(tasks.activeTasks) { task in
                    selectionRow(
                        label: task.title,
                        detail: task.category,
                        selected: selectedTaskID == task.id
                    ) { selectedTaskID = task.id }
                }
            } isEmpty: { tasks.activeTasks.isEmpty }
        case .habit:
            pickerCard(title: loc.t("start.chooseHabit"), empty: loc.t("start.noHabits")) {
                ForEach(scheduledHabits) { habit in
                    selectionRow(
                        label: "\(habit.emoji) \(habit.name)",
                        detail: nil,
                        selected: selectedHabitID == habit.id
                    ) { selectedHabitID = habit.id }
                }
            } isEmpty: { scheduledHabits.isEmpty }
        }
    }

    private func pickerCard<C: View>(
        title: String,
        empty: String,
        @ViewBuilder _ rows: () -> C,
        isEmpty: () -> Bool
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.system(.subheadline, design: .rounded).weight(.semibold))
                .foregroundStyle(colors.textSecondary)
            if isEmpty() {
                Text(empty)
                    .font(.system(.footnote, design: .rounded))
                    .foregroundStyle(colors.textFaint)
                    .padding(.vertical, 8)
            } else {
                VStack(spacing: 6) { rows() }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(colors.border, lineWidth: 1))
    }

    private func selectionRow(label: String, detail: String?, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: selected ? "largecircle.fill.circle" : "circle")
                    .foregroundStyle(selected ? Brand.coral : colors.textFaint)
                VStack(alignment: .leading, spacing: 1) {
                    Text(label)
                        .font(.system(.body, design: .rounded).weight(.medium))
                        .foregroundStyle(colors.textPrimary)
                        .lineLimit(1)
                    if let detail, !detail.isEmpty {
                        Text(detail)
                            .font(.system(.caption2, design: .rounded))
                            .foregroundStyle(colors.textFaint)
                    }
                }
                Spacer()
            }
            .padding(.vertical, 8)
            .padding(.horizontal, 12)
            .background(selected ? Brand.coral.opacity(0.10) : colors.page,
                        in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private var configCard: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(loc.t("start.config").uppercased())
                .font(.system(.caption, design: .rounded).weight(.bold))
                .tracking(1)
                .foregroundStyle(colors.textFaint)
                .padding(.bottom, 6)
            stepperRow(
                label: loc.t("start.focusLength"),
                value: sessionSettings.focusMinutes,
                unit: loc.t("start.minUnit", sessionSettings.focusMinutes),
                step: 5,
                range: TimerSettings.minMinutes...TimerSettings.maxMinutes
            ) { sessionSettings = sessionSettings.updating(.focus, minutes: $0) }
            divider
            stepperRow(
                label: loc.t("settings.shortBreak"),
                value: sessionSettings.shortBreakMinutes,
                unit: loc.t("start.minUnit", sessionSettings.shortBreakMinutes),
                step: 1,
                range: TimerSettings.minMinutes...TimerSettings.maxMinutes
            ) { sessionSettings = sessionSettings.updating(.shortBreak, minutes: $0) }
            divider
            stepperRow(
                label: loc.t("settings.longBreak"),
                value: sessionSettings.longBreakMinutes,
                unit: loc.t("start.minUnit", sessionSettings.longBreakMinutes),
                step: 5,
                range: TimerSettings.minMinutes...TimerSettings.maxMinutes
            ) { sessionSettings = sessionSettings.updating(.longBreak, minutes: $0) }
            divider
            stepperRow(
                label: loc.t("start.sessions"),
                value: sessionSettings.focusSessionsPerCycle,
                unit: "\(sessionSettings.focusSessionsPerCycle)",
                step: 1,
                range: 1...TimerSettings.maxFocusSessionsPerCycle
            ) { sessionSettings = sessionSettings.updatingCycleSize($0) }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(colors.border, lineWidth: 1))
    }

    private var divider: some View {
        Rectangle().fill(colors.border).frame(height: 1).padding(.vertical, 2)
    }

    private func stepperRow(
        label: String,
        value: Int,
        unit: String,
        step: Int,
        range: ClosedRange<Int>,
        set: @escaping (Int) -> Void
    ) -> some View {
        HStack {
            Text(label)
                .font(.system(.body, design: .rounded).weight(.medium))
                .foregroundStyle(colors.textPrimary)
            Spacer()
            Stepper(
                value: Binding(get: { value }, set: set),
                in: range,
                step: step
            ) { EmptyView() }
            .labelsHidden()
            Text(unit)
                .font(.system(.body, design: .rounded).weight(.semibold))
                .foregroundStyle(Brand.coral)
                .monospacedDigit()
                .frame(width: 64, alignment: .trailing)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(label)
        .accessibilityValue(unit)
    }

    private var startBar: some View {
        VStack {
            Spacer()
            PrimaryButton(title: loc.t("start.begin"), systemImage: "play.fill", tint: Brand.coral) {
                begin()
            }
            .opacity(canStart ? 1 : 0.5)
            .disabled(!canStart)
            .padding(.horizontal, 20)
            .padding(.bottom, 12)
            .background(
                LinearGradient(
                    colors: [colors.page.opacity(0), colors.page],
                    startPoint: .top, endPoint: .bottom
                )
                .frame(height: 120)
                .allowsHitTesting(false),
                alignment: .bottom
            )
        }
    }

    private func begin() {
        switch intent {
        case .none:
            tasks.clearActive(); habits.clearActive(); prefs.clearSimpleNote()
        case .note:
            tasks.clearActive(); habits.clearActive()
            prefs.setSimpleNote(noteText)
        case .task:
            prefs.clearSimpleNote()
            if let id = selectedTaskID, let task = tasks.tasks.first(where: { $0.id == id }) {
                habits.clearActive(); tasks.setActive(task)
            }
        case .habit:
            prefs.clearSimpleNote()
            if let id = selectedHabitID, let habit = habits.habits.first(where: { $0.id == id }) {
                tasks.clearActive(); habits.setActive(habit)
            }
        }
        engine.start(using: sessionSettings)
        dismiss()
    }
}
