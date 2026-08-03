//
//  HabitsView.swift
//  Purrodoro
//
//  The Habits tab: gentle daily rituals with a per-day target, a 7-day
//  completion strip, a streak, tap-to-log, and a "focus on this" action that
//  routes a session toward the habit.
//

import SwiftUI

struct HabitsView: View {
    @EnvironmentObject private var store: HabitStore
    @EnvironmentObject private var prefs: PreferencesStore
    @EnvironmentObject private var auth: AuthStore
    @EnvironmentObject private var ownerStore: AccountOwnerStore
    @EnvironmentObject private var syncRepo: SyncRepository
    @EnvironmentObject private var syncCoordinator: SyncCoordinator
    @EnvironmentObject private var engine: TimerEngine
    @EnvironmentObject private var tasks: TaskStore
    @EnvironmentObject private var history: SessionHistoryStore
    @EnvironmentObject private var loc: Localization
    @Environment(\.colorScheme) private var scheme
    @State private var editing: HabitItem?
    @State private var showAdd = false
    @State private var premiumUpsellTrigger: PremiumUpsellTrigger?
    @State private var showLoginSheet = false
    @State private var pendingUpsellAfterLogin: PremiumUpsellTrigger?
    @State private var showAccountSwitchAlert = false
    @State private var dropConfirmContinuation: CheckedContinuation<Bool, Never>?

    var onFocusHabit: (HabitItem) -> Void = { _ in }

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }
    private var habitCount: Int { store.habits.count }
    private var canAdd: Bool { PremiumGate.canAddHabit(habitCount: habitCount, isPremium: prefs.isPremium) }
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
                tasks: tasks,
                habits: store,
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
                    premiumUpsellTrigger = .habitCap
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
            .accessibilityLabel(loc.t("habits.add"))
        }
        .readableContentWidth()
        .environment(\.pColors, colors)
        .sheet(isPresented: $showAdd) {
            HabitEditor(habit: nil) { draft in
                store.add(
                    name: draft.name,
                    emoji: draft.emoji,
                    colorHex: draft.colorHex,
                    target: draft.target,
                    weekdays: draft.weekdays,
                    timeBlock: draft.timeBlock
                )
            }
        }
        .sheet(item: $editing) { habit in
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
        if store.habits.isEmpty {
            PlaceholderScreen(
                title: loc.t("habits.title"),
                subtitle: loc.t("habits.emptySubtitle"),
                image: "CatCelebrate"
            )
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(loc.t("habits.title"))
                            .font(.system(.largeTitle, design: .rounded).weight(.bold))
                        PremiumCapHeader(
                            visible: !prefs.isPremium,
                            current: habitCount,
                            limit: PremiumGate.freeHabitLimit,
                            labelKey: "free_tier.habits"
                        )
                    }
                        .foregroundStyle(colors.textPrimary)
                        .padding(.top, 12)
                        .padding(.bottom, 4)

                    ForEach(store.sortedHabits) { habit in
                        HabitCard(
                            habit: habit,
                            isActive: store.activeHabitID == habit.id,
                            colors: colors,
                            onToggle: { store.toggleToday(habit) },
                            onFocus: { onFocusHabit(habit) }
                        )
                        .contextMenu {
                            Button { editing = habit } label: { Label(loc.t("common.edit"), systemImage: "pencil") }
                            Button {
                                store.toggleRestDay(habit)
                            } label: {
                                Label(loc.t("habits.restToday"), systemImage: "moon.zzz")
                            }
                            Button(role: .destructive) { store.delete(habit) } label: {
                                Label(loc.t("common.delete"), systemImage: "trash")
                            }
                        }
                        .onTapGesture { editing = habit }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 170)
            }
        }
    }
}

// MARK: - Card

private struct HabitCard: View {
    @EnvironmentObject private var loc: Localization
    let habit: HabitItem
    let isActive: Bool
    let colors: PurrodoroColors
    let onToggle: () -> Void
    let onFocus: () -> Void

    private var streakText: String {
        let unit = habit.streak == 1 ? loc.t("habits.dayStreak", habit.streak) : loc.t("habits.daysStreak", habit.streak)
        return "\(unit) · \(habit.todayCount)/\(habit.targetPerDay)"
    }

    var body: some View {
        VStack(spacing: 14) {
            HStack(spacing: 12) {
                Text(habit.emoji)
                    .font(.system(size: 26))
                    .frame(width: 48, height: 48)
                    .background(habit.color.opacity(0.35), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .opacity(habit.isScheduledToday ? 1 : 0.5)

                VStack(alignment: .leading, spacing: 3) {
                    Text(habit.name)
                        .font(.system(.body, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textPrimary)
                        .lineLimit(1)
                    HStack(spacing: 4) {
                        Image(systemName: "flame.fill")
                            .font(.system(size: 11))
                            .foregroundStyle(Brand.coral)
                        Text(streakText)
                            .font(.system(.caption2, design: .rounded))
                            .foregroundStyle(colors.textFaint)
                    }
                    scheduleLine
                }

                Spacer()

                Button(action: onFocus) {
                    Image(systemName: isActive ? "play.circle.fill" : "play.circle")
                        .font(.system(size: 26))
                        .foregroundStyle(isActive ? Brand.coral : colors.textSecondary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(isActive ? loc.t("habits.activeFocusA11y") : loc.t("habits.focusA11y"))

                Button(action: onToggle) {
                    Image(systemName: habit.isDoneToday ? "checkmark.circle.fill" : "circle")
                        .font(.system(size: 28))
                        .foregroundStyle(habit.isDoneToday ? habit.color : colors.textFaint)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(
                    habit.isDoneToday ? loc.t("habits.markNotDoneA11y") : loc.t("habits.logTodayA11y")
                )
            }

            weekStrip
        }
        .padding(14)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(isActive ? Brand.coral : colors.border, lineWidth: isActive ? 2 : 1)
        )
        .contentShape(Rectangle())
    }

    @ViewBuilder
    private var scheduleLine: some View {
        HStack(spacing: 6) {
            if !habit.isScheduledToday {
                Text(loc.t("habits.notToday"))
                    .font(.system(.caption2, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textFaint)
            } else {
                Image(systemName: habit.timeBlock.symbol)
                    .font(.system(size: 10))
                    .foregroundStyle(colors.textFaint)
                Text(occurrenceSummary)
                    .font(.system(.caption2, design: .rounded))
                    .foregroundStyle(colors.textFaint)
                    .lineLimit(1)
            }
        }
    }

    private var occurrenceSummary: String {
        if habit.scheduledWeekdays.isEmpty {
            return loc.t("habits.everyDay")
        }
        let symbols = Journey.calendar.veryShortWeekdaySymbols
        let labels = habit.scheduledWeekdays.sorted().compactMap { wd -> String? in
            let idx = wd - 1
            return symbols.indices.contains(idx) ? symbols[idx] : nil
        }
        return labels.joined(separator: " ")
    }

    private var weekStrip: some View {
        let cal = Journey.calendar
        let today = Date()
        let days: [Date] = (0..<7).reversed().compactMap {
            cal.date(byAdding: .day, value: -$0, to: today)
        }
        return HStack(spacing: 0) {
            ForEach(days, id: \.self) { date in
                let scheduled = habit.isScheduled(on: date)
                let done = habit.meetsTarget(on: date)
                let isToday = cal.isDateInToday(date)
                VStack(spacing: 5) {
                    Text(shortWeekday(date))
                        .font(.system(size: 10, design: .rounded).weight(.semibold))
                        .foregroundStyle(colors.textFaint)
                    ZStack {
                        Circle()
                            .fill(done ? habit.color : colors.page)
                            .frame(width: 22, height: 22)
                            .overlay(Circle().stroke(isToday ? Brand.coral : colors.border, lineWidth: isToday ? 2 : 1))
                            .opacity(scheduled ? 1 : 0.4)
                        if !scheduled && !done {
                            Circle()
                                .fill(colors.textFaint.opacity(0.4))
                                .frame(width: 4, height: 4)
                        }
                    }
                }
                .frame(maxWidth: .infinity)
            }
        }
    }

    private func shortWeekday(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "EEEEE"
        return f.string(from: date)
    }
}

// MARK: - Editor

struct HabitDraft {
    var name: String
    var emoji: String
    var colorHex: UInt32
    var target: Int
    var weekdays: Set<Int>
    var timeBlock: HabitTimeBlock
}

private struct HabitEditor: View {
    let habit: HabitItem?
    let onSave: (HabitDraft) -> Void

    @EnvironmentObject private var loc: Localization
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme
    @State private var name: String
    @State private var emoji: String
    @State private var colorHex: UInt32
    @State private var target: Int
    @State private var weekdays: Set<Int>
    @State private var timeBlock: HabitTimeBlock

    private var colors: PurrodoroColors { PurrodoroColors(scheme: scheme) }

    init(habit: HabitItem?, onSave: @escaping (HabitDraft) -> Void) {
        self.habit = habit
        self.onSave = onSave
        _name = State(initialValue: habit?.name ?? "")
        _emoji = State(initialValue: habit?.emoji ?? "🌱")
        _colorHex = State(initialValue: habit?.colorHex ?? HabitStore.palette[0])
        _target = State(initialValue: habit?.targetPerDay ?? 1)
        _weekdays = State(initialValue: habit?.scheduledWeekdays ?? [])
        _timeBlock = State(initialValue: habit?.timeBlock ?? .any)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                colors.page.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 16) {
                        TextField(loc.t("habits.name"), text: $name)
                            .font(.system(.body, design: .rounded))
                            .padding(16)
                            .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(colors.border, lineWidth: 1))

                        card(title: loc.t("habits.icon")) {
                            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 6), spacing: 12) {
                                ForEach(HabitStore.emojis, id: \.self) { e in
                                    Text(e)
                                        .font(.system(size: 24))
                                        .frame(width: 40, height: 40)
                                        .background(emoji == e ? Color(hex: colorHex).opacity(0.4) : colors.page,
                                                    in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                                        .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous)
                                            .stroke(emoji == e ? Brand.coral : colors.border, lineWidth: emoji == e ? 2 : 1))
                                        .onTapGesture { emoji = e }
                                }
                            }
                        }

                        card(title: loc.t("habits.color")) {
                            HStack(spacing: 12) {
                                ForEach(HabitStore.palette, id: \.self) { c in
                                    Circle()
                                        .fill(Color(hex: c))
                                        .frame(width: 34, height: 34)
                                        .overlay(Circle().stroke(colors.textPrimary, lineWidth: colorHex == c ? 2.5 : 0))
                                        .onTapGesture { colorHex = c }
                                }
                                Spacer()
                            }
                        }

                        occurrenceCard
                        timeBlockCard

                        card(title: nil) {
                            Stepper(value: $target, in: 1...8) {
                                HStack {
                                    Text(loc.t("habits.dailyTarget"))
                                        .font(.system(.body, design: .rounded).weight(.semibold))
                                        .foregroundStyle(colors.textPrimary)
                                    Spacer()
                                    Text(loc.t("habits.perDay", target))
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
            .navigationTitle(habit == nil ? loc.t("habits.new") : loc.t("habits.edit"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(loc.t("common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(loc.t("common.save")) {
                        onSave(HabitDraft(
                            name: name,
                            emoji: emoji,
                            colorHex: colorHex,
                            target: target,
                            weekdays: weekdays,
                            timeBlock: timeBlock
                        ))
                        dismiss()
                    }
                    .font(.system(.body, design: .rounded).weight(.bold))
                    .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }

    private var occurrenceCard: some View {
        card(title: loc.t("habits.occurrence")) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 6) {
                    ForEach(orderedWeekdays, id: \.self) { wd in
                        let selected = weekdays.contains(wd)
                        Button {
                            if selected { weekdays.remove(wd) } else { weekdays.insert(wd) }
                        } label: {
                            Text(weekdaySymbol(wd))
                                .font(.system(.caption, design: .rounded).weight(.bold))
                                .foregroundStyle(selected ? .white : colors.textSecondary)
                                .frame(maxWidth: .infinity)
                                .frame(height: 38)
                                .background(selected ? Brand.coral : colors.page,
                                            in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                                .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous)
                                    .stroke(selected ? Color.clear : colors.border, lineWidth: 1))
                        }
                        .buttonStyle(.plain)
                    }
                }
                Text(weekdays.isEmpty ? loc.t("habits.everyDay") : "")
                    .font(.system(.caption2, design: .rounded))
                    .foregroundStyle(colors.textFaint)
            }
        }
    }

    private var timeBlockCard: some View {
        card(title: loc.t("habits.timeBlock")) {
            HStack(spacing: 8) {
                ForEach(HabitTimeBlock.allCases) { block in
                    let selected = timeBlock == block
                    Button { timeBlock = block } label: {
                        VStack(spacing: 5) {
                            Image(systemName: block.symbol)
                                .font(.system(size: 15, weight: .semibold))
                            Text(loc.t(block.labelKey))
                                .font(.system(.caption2, design: .rounded).weight(.semibold))
                        }
                        .foregroundStyle(selected ? .white : colors.textSecondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(selected ? Brand.coral : colors.page,
                                    in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(selected ? Color.clear : colors.border, lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    /// Weekdays in the user's locale order (e.g. Sun-first or Mon-first).
    private var orderedWeekdays: [Int] {
        let first = Journey.calendar.firstWeekday
        return (0..<7).map { ((first - 1 + $0) % 7) + 1 }
    }

    private func weekdaySymbol(_ weekday: Int) -> String {
        let symbols = Journey.calendar.veryShortWeekdaySymbols
        let idx = weekday - 1
        return symbols.indices.contains(idx) ? symbols[idx] : "?"
    }

    private func card<Content: View>(title: String?, @ViewBuilder _ content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            if let title {
                Text(title)
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .foregroundStyle(colors.textSecondary)
            }
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(colors.border, lineWidth: 1))
    }
}

#Preview {
    HabitsView()
        .environmentObject(HabitStore())
        .environmentObject(Localization())
}
