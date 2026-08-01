//
//  HabitDetailView.swift
//  Purrodoro
//
//  Habit detail sheet — Android `HabitDetailSheet` / `HabitDetailContent`:
//  streak tiles, mark-done, focus CTA + helpers, week strip, monthly progress
//  grid, inline edit, delete.
//

import SwiftUI

struct HabitDetailView: View {
    let habitID: UUID
    var onFocus: (HabitItem) -> Void = { _ in }
    var onOpenFocus: () -> Void = {}
    var onFocusBusy: () -> Void = {}

    @EnvironmentObject private var store: HabitStore
    @EnvironmentObject private var engine: TimerEngine
    @EnvironmentObject private var loc: Localization
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme

    @State private var showEditor = false
    @State private var confirmDelete = false

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    /// Live habit from the store so logs/streaks update while the sheet is open.
    private var habit: HabitItem? {
        store.habits.first { $0.id == habitID }
    }

    var body: some View {
        NavigationStack {
            Group {
                if let habit {
                    detailScroll(habit)
                } else {
                    Color.clear.onAppear { dismiss() }
                }
            }
            .background(colors.page.ignoresSafeArea())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(loc.t("common.done")) { dismiss() }
                }
            }
            .sheet(isPresented: $showEditor) {
                if let habit {
                    HabitEditor(habit: habit) { draft in
                        var updated = habit
                        updated.name = draft.name
                        updated.emoji = draft.emoji
                        updated.colorHex = draft.colorHex
                        updated.targetPerDay = max(draft.target, 1)
                        updated.scheduledWeekdays = draft.weekdays
                        updated.timeBlock = draft.timeBlock
                        store.update(updated)
                    }
                }
            }
            .alert(loc.t("habits.deleteTitle"), isPresented: $confirmDelete) {
                Button(loc.t("common.cancel"), role: .cancel) {}
                Button(loc.t("habits.deleteButton"), role: .destructive) {
                    if let habit {
                        store.delete(habit)
                        dismiss()
                    }
                }
            } message: {
                Text(loc.t("habits.deleteBody"))
            }
        }
    }

    @ViewBuilder
    private func detailScroll(_ habit: HabitItem) -> some View {
        let today = Date()
        let scheduledToday = habit.isScheduled(on: today)
        let doneToday = habit.meetsTarget(on: today)
        let isLive = store.activeHabitID == habit.id && engine.isStarted
        let focusCta: HabitFocusCtaState = {
            if isLive { return .openFocus }
            if engine.isStarted { return .blocked }
            return .startFocus
        }()
        let blockReason: HabitFocusBlockReason? = focusCta == .startFocus
            ? habit.focusBlockReason(on: today)
            : nil

        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                header(habit)
                streakTiles(habit, today: today)
                markDoneButton(habit, scheduled: scheduledToday, done: doneToday)
                if !scheduledToday {
                    helperRow(restDayMessage(habit, today: today))
                }
                focusButton(habit, cta: focusCta, blockReason: blockReason)
                helperRow(focusHelperMessage(cta: focusCta, blockReason: blockReason))

                sectionHeader(loc.t("habits.thisWeek"))
                HabitWeekStrip(habit: habit, colors: colors, today: today)
                    .padding(14)
                    .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(colors.border, lineWidth: 1)
                    )

                sectionHeader(loc.t("habits.monthlyProgress"))
                HabitMonthlyProgressCard(habit: habit, today: today, colors: colors)

                Button(role: .destructive) {
                    confirmDelete = true
                } label: {
                    Text(loc.t("habits.deleteButton"))
                        .font(.system(.body, design: .rounded).weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .buttonStyle(.plain)
                .foregroundStyle(Brand.coral)
                .padding(.top, 4)
            }
            .padding(20)
            .padding(.bottom, 24)
        }
    }

    private func header(_ habit: HabitItem) -> some View {
        HStack(spacing: 14) {
            Text(habit.emoji)
                .font(.system(size: 32))
                .frame(width: 64, height: 64)
                .background(habit.color.opacity(0.2), in: RoundedRectangle(cornerRadius: 16, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text(habit.name)
                    .font(.system(.title3, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                    .lineLimit(2)
                Text(scheduleLabel(habit))
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundStyle(colors.textSecondary)
            }

            Spacer(minLength: 8)

            Button {
                showEditor = true
            } label: {
                Text(loc.t("habits.editChip"))
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textPrimary)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(colors.card, in: Capsule())
                    .overlay(Capsule().stroke(colors.border, lineWidth: 1))
            }
            .buttonStyle(.plain)
            .accessibilityLabel(loc.t("habits.edit"))
        }
    }

    private func streakTiles(_ habit: HabitItem, today: Date) -> some View {
        HStack(spacing: 12) {
            statTile(value: "\(habit.currentStreak(asOf: today))", label: loc.t("habits.currentStreak"))
            statTile(value: "\(habit.bestStreak(asOf: today))", label: loc.t("habits.bestStreak"))
        }
    }

    private func statTile(value: String, label: String) -> some View {
        VStack(spacing: 6) {
            Text(value)
                .font(.system(.title2, design: .rounded).weight(.bold))
                .foregroundStyle(colors.textPrimary)
                .monospacedDigit()
            Text(label)
                .font(.system(.caption2, design: .rounded).weight(.semibold))
                .foregroundStyle(colors.textFaint)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(colors.border, lineWidth: 1))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityValue(value)
    }

    private func markDoneButton(_ habit: HabitItem, scheduled: Bool, done: Bool) -> some View {
        Button {
            store.toggleToday(habit)
        } label: {
            Text(done ? loc.t("habits.completedToday") : loc.t("habits.markDoneToday"))
                .font(.system(.headline, design: .rounded).weight(.bold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(
                    (scheduled ? Brand.coral : colors.textFaint).opacity(scheduled ? 1 : 0.45),
                    in: RoundedRectangle(cornerRadius: 16, style: .continuous)
                )
        }
        .buttonStyle(.plain)
        .disabled(!scheduled)
        .accessibilityLabel(done ? loc.t("habits.completedToday") : loc.t("habits.markDoneToday"))
    }

    private func focusButton(
        _ habit: HabitItem,
        cta: HabitFocusCtaState,
        blockReason: HabitFocusBlockReason?
    ) -> some View {
        let label = cta == .openFocus
            ? loc.t("habits.openFocus")
            : loc.t("habits.focusWithPurrodoro")
        let enabled = blockReason == nil
        return Button {
            switch cta {
            case .startFocus:
                onFocus(habit)
                dismiss()
            case .openFocus:
                onOpenFocus()
                dismiss()
            case .blocked:
                onFocusBusy()
                dismiss()
            }
        } label: {
            Text(label)
                .font(.system(.body, design: .rounded).weight(.semibold))
                .foregroundStyle(enabled ? Brand.coral : colors.textFaint)
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(enabled ? Brand.coral.opacity(0.35) : colors.border, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
        .disabled(!enabled && cta == .startFocus)
        .accessibilityLabel(label)
    }

    private func helperRow(_ message: String) -> some View {
        HStack(alignment: .center, spacing: 10) {
            Image("CatMascot")
                .resizable()
                .scaledToFill()
                .frame(width: 40, height: 40)
                .clipShape(Circle())
            Text(message)
                .font(.system(.subheadline, design: .rounded))
                .foregroundStyle(colors.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
        }
        .accessibilityElement(children: .combine)
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.system(.subheadline, design: .rounded).weight(.bold))
            .foregroundStyle(colors.textPrimary)
            .padding(.top, 4)
    }

    private func scheduleLabel(_ habit: HabitItem) -> String {
        if habit.scheduledWeekdays.isEmpty {
            return loc.t("habits.everyDay")
        }
        let symbols = Journey.calendar.shortWeekdaySymbols
        let labels = habit.scheduledWeekdays.sorted().compactMap { wd -> String? in
            let idx = wd - 1
            return symbols.indices.contains(idx) ? symbols[idx] : nil
        }
        return labels.joined(separator: ", ")
    }

    private func restDayMessage(_ habit: HabitItem, today: Date) -> String {
        let next = habit.nextScheduledDate(from: today)
        let cal = Journey.calendar
        let tomorrow = cal.date(byAdding: .day, value: 1, to: cal.startOfDay(for: today))
        let back: String
        if let tomorrow, cal.isDate(next, inSameDayAs: tomorrow) {
            back = loc.t("habits.backTomorrow")
        } else {
            let f = DateFormatter()
            f.dateFormat = "EEE"
            back = loc.t("habits.backDay", f.string(from: next))
        }
        return loc.t("habits.notScheduledDetail", back)
    }

    private func focusHelperMessage(
        cta: HabitFocusCtaState,
        blockReason: HabitFocusBlockReason?
    ) -> String {
        switch cta {
        case .openFocus:
            return loc.t("habits.helperOpen")
        case .blocked:
            return loc.t("habits.helperBlocked")
        case .startFocus:
            switch blockReason {
            case .notScheduled: return loc.t("habits.helperFocusNotScheduled")
            case .skipped: return loc.t("habits.helperSkipped")
            case .completed: return loc.t("habits.helperCompleted")
            case nil: return loc.t("habits.helperStart")
            }
        }
    }
}

private enum HabitFocusCtaState {
    case startFocus
    case openFocus
    case blocked
}

// MARK: - Week strip (shared with list cards)

struct HabitWeekStrip: View {
    let habit: HabitItem
    let colors: PurrodoroColors
    var today: Date = Date()

    var body: some View {
        let dots = HabitDisplay.weekDots(for: habit, today: today)
        HStack(spacing: 0) {
            ForEach(dots) { dot in
                VStack(spacing: 5) {
                    Text(dot.label)
                        .font(.system(size: 10, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textFaint)
                    ZStack {
                        Circle()
                            .fill(dot.isCompleted ? habit.color : colors.page)
                            .frame(width: 22, height: 22)
                            .overlay(
                                Circle().stroke(
                                    dot.isToday ? Brand.coral : colors.border,
                                    lineWidth: dot.isToday ? 2 : 1
                                )
                            )
                            .opacity(dot.isScheduled ? 1 : 0.4)
                        if !dot.isScheduled && !dot.isCompleted {
                            Circle()
                                .fill(colors.textFaint.opacity(0.4))
                                .frame(width: 4, height: 4)
                        }
                    }
                }
                .frame(maxWidth: .infinity)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(dot.label)
                .accessibilityValue(
                    dot.isCompleted ? "completed" : (dot.isScheduled ? "pending" : "rest")
                )
            }
        }
    }
}

// MARK: - Monthly progress (Android KuroMonthlyHabitProgressCard)

struct HabitMonthlyProgressCard: View {
    @EnvironmentObject private var loc: Localization
    let habit: HabitItem
    var today: Date = Date()
    let colors: PurrodoroColors

    private var monthTitle: String {
        let f = DateFormatter()
        f.dateFormat = "MMMM yyyy"
        return f.string(from: today)
    }

    private var locTitle: String { loc.t("habits.monthlyProgress") }

    var body: some View {
        let cells = gridCells()
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .bottom) {
                Text(locTitle)
                    .font(.system(.subheadline, design: .rounded).weight(.bold))
                    .foregroundStyle(colors.textPrimary)
                Spacer()
                Text(monthTitle)
                    .font(.system(.caption, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textSecondary)
            }

            HStack(spacing: 0) {
                ForEach(Array(HabitDisplay.mondayFirstWeekdayHeaders.enumerated()), id: \.offset) { _, day in
                    Text(day)
                        .font(.system(size: 10, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textFaint)
                        .frame(maxWidth: .infinity)
                }
            }

            let rows = chunk(cells, size: 7)
            VStack(spacing: 8) {
                ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                    HStack(spacing: 0) {
                        ForEach(Array(row.enumerated()), id: \.offset) { _, cell in
                            monthDot(completed: cell.completed, isToday: cell.isToday)
                                .frame(maxWidth: .infinity)
                        }
                        if row.count < 7 {
                            ForEach(0..<(7 - row.count), id: \.self) { _ in
                                Color.clear.frame(maxWidth: .infinity)
                            }
                        }
                    }
                }
            }
        }
        .padding(16)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(colors.border, lineWidth: 1))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(locTitle)
        .accessibilityValue(monthTitle)
    }

    private func gridCells() -> [(completed: Bool?, isToday: Bool)] {
        let dots = HabitDisplay.monthlyProgressDots(habits: [habit], monthDate: today)
        let lead = HabitDisplay.mondayFirstLeadingBlanks(for: today)
        let cal = Journey.calendar
        let todayStart = cal.startOfDay(for: today)
        var cells: [(completed: Bool?, isToday: Bool)] = (0..<lead).map { _ in (nil, false) }
        for dot in dots {
            cells.append((dot.isCompleted, cal.isDate(dot.date, inSameDayAs: todayStart)))
        }
        return cells
    }

    @ViewBuilder
    private func monthDot(completed: Bool?, isToday: Bool) -> some View {
        if let completed {
            Circle()
                .fill(
                    completed ? Brand.mint
                        : (isToday ? Brand.mint.opacity(0.4) : colors.border)
                )
                .frame(width: 12, height: 12)
        } else {
            Color.clear.frame(width: 12, height: 12)
        }
    }

    private func chunk<T>(_ items: [T], size: Int) -> [[T]] {
        stride(from: 0, to: items.count, by: size).map {
            Array(items[$0..<min($0 + size, items.count)])
        }
    }
}
